import sys
import numpy as np
import logging
from scipy.sparse import csr_matrix
from scipy.sparse import csr_array, coo_array
from scipy.sparse.csgraph import dijkstra

import bike_net_reader
import bike_route_calculator
from bike_route_utilities import BikeRouteChoiceSettings, load_settings

# Set up logging
logger = logging.getLogger(__name__)

# def randomize_network_cost(edges, traversals, random_scale):
#     print("Randomizing network costs")
#     edges_rand = edges.copy()
#     traversals_rand = traversals.copy()
#     edges_rand.bikeCost = edges_rand.bikeCost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(edges_rand)))
#     traversals_rand.bikecost = traversals_rand.bikecost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(traversals_rand)))
#     return edges_rand, traversals_rand


def process_paths_new(centroids, predecessors):
    logger.info("Processing paths without numba...")

    # Add self-referential column to predecessor table to indicate end of path
    predecessors_null = np.hstack(
        (predecessors, np.full((predecessors.shape[0], 1), -1))
    )
    predecessors_null[predecessors_null == -9999] = -1
    # Get starting indices for OD pairs with path found
    notnull = (predecessors_null >= 0).nonzero()
    notnull = tuple(
        i.astype(np.int32) for i in notnull
    )  # force int32 to save memory (defaults to int64)
    notnull_dest = np.isin(notnull[1], centroids).nonzero()
    origin_indices, dest_indices = (notnull[0][notnull_dest], notnull[1][notnull_dest])

    # Iterate through predecessors
    node_indices = dest_indices
    paths = [node_indices]
    while np.any(node_indices >= 0):
        node_indices = predecessors_null[origin_indices, node_indices]
        paths.append(node_indices)

    stack_paths = np.vstack(paths).T
    stack_paths = stack_paths[:, ::-1]  # Reverse order to get origin -> destination
    stack_paths_from = stack_paths[:, :-1]
    stack_paths_to = stack_paths[:, 1:]  # Offset by 1 to get to-node

    # Remove null edges
    od_index, path_num = (stack_paths_from >= 0).nonzero()
    # path_num_actual = path_num - np.argmax(stack_paths_from >= 0, axis=1)[od_index] # 0-index
    paths_from_node = stack_paths_from[od_index, path_num]
    paths_to_node = stack_paths_to[od_index, path_num]

    paths_orig = origin_indices[od_index]  # centroids index
    paths_dest = dest_indices[od_index]  # mapped node id

    return (paths_orig, paths_dest, paths_from_node, paths_to_node)


def calculate_final_logsums_batch_traversals(
    nodes,
    edges,
    traversals,
    origin_centroids,
    dest_centroids,
    all_paths_orig,
    all_paths_dest,
    all_paths_from_edge,
    all_paths_to_edge,
    all_paths_iteration,
    num_iterations,
    trace_origins=[],
    trace_dests=[],
):
    """
    Calculate the final logsums for the bike network using pre-computed paths and traversals.
    Includes path size calculation.

    Args:
        nodes (pd.DataFrame): DataFrame containing node information.
        edges (pd.DataFrame): DataFrame containing edge information.
        traversals (pd.DataFrame): DataFrame containing traversal information.
        origin_centroids (list): List of origin centroids.
        dest_centroids (list): List of destination centroids.
        all_paths_orig (np.ndarray): Array of origin indices for all paths.
        all_paths_dest (np.ndarray): Array of destination indices for all paths.
        all_paths_from_edge (np.ndarray): Array of from-edge indices for all paths.
        all_paths_to_edge (np.ndarray): Array of to-edge indices for all paths.
        all_paths_iteration (np.ndarray): Array of iteration indices for all paths.
        num_iterations (int): Number of iterations in the simulation.
        trace_origins (list, optional): List of origins to trace. Defaults to [].
        trace_dests (list, optional): List of destinations to trace. Defaults to [].

    Returns:
        tuple: A tuple containing:
            - paths_od_orig_mapped (np.ndarray): Mapped origin indices for paths.
            - paths_od_dest_mapped (np.ndarray): Mapped destination indices for paths.
            - paths_od_logsum (np.ndarray): Logsum values for each OD pair.
            - trace_paths_orig_mapped (np.ndarray): Mapped origins for traced paths.
            - trace_paths_dest_mapped (np.ndarray): Mapped destinations for traced paths.
            - trace_paths_iteration (np.ndarray): Iteration indices for traced paths.
            - trace_paths_prev_node (np.ndarray): Previous node indices for traced paths.
            - trace_paths_from_node (np.ndarray): From-node indices for traced paths.
            - trace_paths_to_node (np.ndarray): To-node indices for traced paths.
            - trace_paths_path_size (np.ndarray): Path size for traced paths.
    """
    logger.info("Calculating logsums...")

    # Mapped node id to centroids index
    dest_centroids_rev_map = np.zeros(max(dest_centroids) + 1, dtype=np.int32)
    dest_centroids_rev_map[dest_centroids] = range(len(dest_centroids))
    all_paths_dest_rev = dest_centroids_rev_map[all_paths_dest]

    node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.id)}

    edge_from_node = edges.fromNode.map(node_mapping).to_numpy()
    edge_to_node = edges.toNode.map(node_mapping).to_numpy()
    edge_cost = edges.edgeCost.to_numpy()
    edge_length = edges.distance.to_numpy()

    all_paths_from_node = edge_from_node[all_paths_to_edge]
    all_paths_to_node = edge_to_node[all_paths_to_edge]
    all_paths_edge_cost = edge_cost[all_paths_to_edge]
    all_paths_edge_length = edge_length[all_paths_to_edge]
    if trace_origins:
        all_paths_prev_node = edge_from_node[all_paths_from_edge]

    num_edges = len(edges)

    edge_mapping = edges[["fromNode", "toNode"]].reset_index()

    traversals_mapped = traversals.merge(
        edge_mapping,
        how="left",
        left_on=["start", "thru"],
        right_on=["fromNode", "toNode"],
    ).merge(
        edge_mapping,
        how="left",
        left_on=["thru", "end"],
        right_on=["fromNode", "toNode"],
        suffixes=("FromEdge", "ToEdge"),
    )

    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.bikecost.to_numpy()
    trav_costs = csr_array((data, (row, col)), shape=(num_edges, num_edges))

    all_paths_trav_cost = trav_costs[all_paths_from_edge, all_paths_to_edge]

    # Add origin connectors
    orig_connectors_indices = np.isin(all_paths_from_edge, origin_centroids).nonzero()[
        0
    ]
    all_paths_from_node = np.concatenate(
        (
            all_paths_from_node,
            edge_from_node[all_paths_from_edge][orig_connectors_indices],
        )
    )
    all_paths_to_node = np.concatenate(
        (all_paths_to_node, edge_to_node[all_paths_from_edge][orig_connectors_indices])
    )
    all_paths_edge_cost = np.concatenate(
        (all_paths_edge_cost, edge_cost[all_paths_from_edge][orig_connectors_indices])
    )
    all_paths_edge_length = np.concatenate(
        (
            all_paths_edge_length,
            edge_length[all_paths_from_edge][orig_connectors_indices],
        )
    )
    all_paths_trav_cost = np.concatenate(
        (all_paths_trav_cost, np.zeros_like(orig_connectors_indices))
    )
    all_paths_orig_new = np.concatenate(
        (all_paths_orig, all_paths_orig[orig_connectors_indices])
    )
    all_paths_dest_rev_new = np.concatenate(
        (all_paths_dest_rev, all_paths_dest_rev[orig_connectors_indices])
    )
    all_paths_iteration_new = np.concatenate(
        (all_paths_iteration, all_paths_iteration[orig_connectors_indices])
    )
    if trace_origins:
        all_paths_prev_node = np.concatenate(
            (all_paths_prev_node, np.full_like(orig_connectors_indices, -1))
        )

    # all_paths_from_node = all_paths_thru_node
    # all_paths_to_node = all_paths_end_node
    all_paths_cost = all_paths_edge_cost + all_paths_trav_cost

    # SciPy Sparse arrays only surrport 2d arrays, so flatten OD and link indices
    paths_od_ravel = np.ravel_multi_index(
        (all_paths_orig_new, all_paths_dest_rev_new),
        (len(origin_centroids), len(dest_centroids)),
    )
    paths_link_ravel = np.ravel_multi_index(
        (all_paths_from_node, all_paths_to_node), (len(nodes), len(nodes))
    )

    if trace_origins:
        # trace_indices = (np.isin(all_paths_orig_new, trace_origins) & np.isin(all_paths_dest_rev_new, trace_dests)).nonzero()[0]
        trace_od_ravel = np.ravel_multi_index(
            (trace_origins, trace_dests), (len(origin_centroids), len(dest_centroids))
        )
        trace_indices = (np.isin(paths_od_ravel, trace_od_ravel)).nonzero()[0]
        trace_paths_orig = all_paths_orig_new[trace_indices]
        trace_paths_dest_rev = all_paths_dest_rev_new[trace_indices]
        trace_paths_iteration = all_paths_iteration_new[trace_indices]
        trace_paths_prev_node = all_paths_prev_node[trace_indices]
        trace_paths_from_node = all_paths_from_node[trace_indices]
        trace_paths_to_node = all_paths_to_node[trace_indices]

    # SciPy COO array will add duplicates together upon conversion to CSR array
    # Insert ones for each path link to count number of paths for each OD/link
    # Likely not an optimal solution, but np.unique took far longer, should consider other alternatives
    ones_arr = np.ones(len(paths_od_ravel), dtype=np.uint8)
    link_num_paths = coo_array(
        (ones_arr, (paths_od_ravel, paths_link_ravel)),
        shape=(len(origin_centroids) * len(dest_centroids), len(nodes) ** 2),
    )
    link_num_paths = csr_array(link_num_paths)

    paths_num_paths = link_num_paths[paths_od_ravel, paths_link_ravel]

    # path size = sum( ( la / Li ) * ( 1 / Man ) ) = sum( la / Man ) / Li
    all_paths_size_component = all_paths_edge_length / paths_num_paths  # la / Man
    # Flatten OD and iteration indices to sum cost, length, path size
    # Should check if COO array is faster than bincount
    paths_od_iter_ravel = np.ravel_multi_index(
        (all_paths_orig_new, all_paths_dest_rev_new, all_paths_iteration_new),
        (len(origin_centroids), len(dest_centroids), num_iterations),
    )
    od_iter_length_total = np.bincount(paths_od_iter_ravel, all_paths_edge_length)  # Li
    od_iter_path_size = (
        np.bincount(paths_od_iter_ravel, all_paths_size_component)
        / od_iter_length_total
    )  # sum( la / Man ) / Li
    od_iter_cost = np.bincount(paths_od_iter_ravel, all_paths_cost)

    if trace_origins:
        trace_paths_od_iter_ravel = np.ravel_multi_index(
            (trace_paths_orig, trace_paths_dest_rev, trace_paths_iteration),
            (len(origin_centroids), len(dest_centroids), num_iterations),
        )
        trace_paths_path_size = od_iter_path_size[trace_paths_od_iter_ravel]

    # Unflatten OD and iteration indices, no longer need individual links
    od_iter_indices = (od_iter_length_total > 0).nonzero()[0]
    paths_od_iter_orig, paths_od_iter_dest, paths_od_iter_iter = np.unravel_index(
        od_iter_indices, (len(origin_centroids), len(dest_centroids), num_iterations)
    )
    paths_od_iter_cost = od_iter_cost[od_iter_indices]
    paths_od_iter_path_size = od_iter_path_size[od_iter_indices]

    # Normalize path size, need to sum path size by OD
    paths_od_ravel = np.ravel_multi_index(
        (paths_od_iter_orig, paths_od_iter_dest),
        (len(origin_centroids), len(dest_centroids)),
    )
    od_path_size_sum = np.bincount(paths_od_ravel, paths_od_iter_path_size)
    paths_od_iter_path_size_sum = od_path_size_sum[paths_od_ravel]
    paths_od_iter_path_size_normalized = (
        paths_od_iter_path_size / paths_od_iter_path_size_sum
    )

    # Add path cost to utility function
    paths_od_iter_utility = (-1 * paths_od_iter_cost) + np.log(
        paths_od_iter_path_size_normalized
    )
    # paths_od_iter_utility = (-1 * paths_od_iter_cost) + (paths_od_iter_path_size_normalized)

    # Unflatten OD indices, no longer need iterations
    od_indices = (od_path_size_sum > 0).nonzero()[0]
    paths_od_orig, paths_od_dest = np.unravel_index(
        od_indices, (len(origin_centroids), len(dest_centroids))
    )

    # Logsum calculation
    od_logsum = np.bincount(paths_od_ravel, np.exp(paths_od_iter_utility))
    paths_od_logsum = np.log(od_logsum[od_indices])

    # Centroids index to mapped node id
    origin_centroids_np = np.array(origin_centroids)
    paths_od_orig_mapped = origin_centroids_np[paths_od_orig]
    dest_centroids_np = np.array(dest_centroids)
    paths_od_dest_mapped = dest_centroids_np[paths_od_dest]

    if trace_origins:
        trace_paths_orig_mapped = origin_centroids_np[trace_paths_orig]
        trace_paths_dest_mapped = dest_centroids_np[trace_paths_dest_rev]

    if trace_origins:
        return (
            paths_od_orig_mapped,
            paths_od_dest_mapped,
            paths_od_logsum,
            trace_paths_orig_mapped,
            trace_paths_dest_mapped,
            trace_paths_iteration,
            trace_paths_prev_node,
            trace_paths_from_node,
            trace_paths_to_node,
            trace_paths_path_size,
        )
    else:
        return (
            paths_od_orig_mapped,
            paths_od_dest_mapped,
            paths_od_logsum,
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
        )


def _perform_dijkstra(centroids, adjacency_matrix, limit=3):
    """Perform Dijkstra's algorithm for a batch of centroids."""
    print(f"Processing Dijkstra's on {len(centroids)} centroids with limit={limit}...")
    distances, predecessors = dijkstra(
        adjacency_matrix,
        directed=True,
        indices=centroids,
        return_predecessors=True,
        limit=limit,
    )

    return (distances, predecessors)


def perform_dijkstras_algorithm_batch_traversals(
    nodes, edges, traversals, origin_centroids, limit
):
    """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
    num_edges = len(edges)

    # # node mapping needs to start at 0 in order to create adjacency matrix
    # fromNode and toNode index is saved as columns in edges
    edge_mapping = edges["edge_utility"].reset_index()

    traversals_mapped = (
        traversals.reset_index()
        .merge(
            edge_mapping,
            how="left",
            left_on=["start", "thru"],
            right_on=["fromNode", "toNode"],
        )
        .merge(
            edge_mapping,
            how="left",
            left_on=["thru", "end"],
            right_on=["fromNode", "toNode"],
            suffixes=("FromEdge", "ToEdge"),
        )
    )
    # Total bike cost is edge cost (after traversal) plus traversal cost
    # Note origin zone connector edge cost is not included with this approach, but this has no impact on shortest path selection
    traversals_mapped["total_utility"] = (
        traversals_mapped.edge_utilityToEdge + traversals_mapped.traversal_utility
    )
    traversals_mapped.loc[
        traversals_mapped.indexFromEdge.isin(origin_centroids), "total_utility"
    ] += traversals_mapped.edge_utilityFromEdge

    # Create a sparse adjacency matrix
    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.total_utility.to_numpy()
    adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_edges, num_edges))

    logger.info(f"Need to calculate Dijkstra's on {len(origin_centroids)} centroids")

    # Perform Dijkstra's algorithm for all centroids
    shortest_paths = _perform_dijkstra(origin_centroids, adjacency_matrix, limit)
    return shortest_paths


def run_iterations_batch_traversals(
    settings,
    nodes,
    edges,
    traversals,
    origin_centroids,
    dest_centroids,
    num_iterations,
):

    all_paths = []

    for i in range(num_iterations):
        # randomize costs
        # edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)
        # FIXME - calculate utilities with randomized costs
        edges["edge_utility"] = bike_route_calculator.calculate_utilities_from_spec(
            settings,
            choosers=edges,
            spec_file=settings.edge_util_file,
            trace_label="bike_edge_utilities",
        )
        traversals[
            "traversal_utility"
        ] = bike_route_calculator.calculate_utilities_from_spec(
            settings,
            choosers=traversals,
            spec_file=settings.traversal_util_file,
            trace_label="bike_traversal_utilities",
        )
        # convert edge utility to distance
        avg_utility_per_mi = (edges["edge_utility"] / edges["distance"]).mean()
        utility_limit = -1 * settings.max_dijkstra_distance * avg_utility_per_mi

        # run dijkstra's
        distances, predecessors = perform_dijkstras_algorithm_batch_traversals(
            nodes, edges, traversals, origin_centroids, limit=utility_limit
        )

        # process paths
        paths = process_paths_new(dest_centroids, predecessors)
        all_paths.append(paths + (np.full_like(paths[0], i, dtype=np.uint8),))

    all_paths_concat = map(np.concatenate, zip(*all_paths))
    (
        all_paths_orig,
        all_paths_dest,
        all_paths_from_edge,
        all_paths_to_edge,
        all_paths_iteration,
    ) = all_paths_concat
    return (
        all_paths_orig,
        all_paths_dest,
        all_paths_from_edge,
        all_paths_to_edge,
        all_paths_iteration,
    )


def run_batch_traversals(
    settings,
    nodes,
    edges,
    traversals,
    origin_centroids,
    dest_centroids,
    num_iterations,
    trace_origins=[],
    trace_dests=[],
):
    """
    Run batch traversals for the bike route choice model.

    """
    (
        all_paths_orig,
        all_paths_dest,
        all_paths_from_edge,
        all_paths_to_edge,
        all_paths_iteration,
    ) = run_iterations_batch_traversals(
        settings,
        nodes,
        edges,
        traversals,
        origin_centroids,
        dest_centroids,
        num_iterations,
    )
    trace_origins_rev = []
    trace_dests_rev = []
    if trace_origins:
        trace_origins_np = np.array(trace_origins)
        origin_centroids_rev_map = np.zeros(max(origin_centroids) + 1, dtype=np.int32)
        origin_centroids_rev_map[origin_centroids] = range(len(origin_centroids))
        trace_origins_rev = origin_centroids_rev_map[
            trace_origins_np[np.isin(trace_origins, origin_centroids)]
        ]

        trace_dests_np = np.array(trace_dests)
        dest_centroids_rev_map = np.zeros(max(dest_centroids) + 1, dtype=np.int32)
        dest_centroids_rev_map[dest_centroids] = range(len(dest_centroids))
        trace_dests_rev = dest_centroids_rev_map[
            trace_dests_np[np.isin(trace_origins, origin_centroids)]
        ]
    final_paths = calculate_final_logsums_batch_traversals(
        nodes,
        edges,
        traversals,
        origin_centroids,
        dest_centroids,
        all_paths_orig,
        all_paths_dest,
        all_paths_from_edge,
        all_paths_to_edge,
        all_paths_iteration,
        num_iterations,
        trace_origins_rev,
        trace_dests_rev,
    )
    return final_paths


def run_bike_route_choice(settings):
    """Main function to run the bike route choice model."""

    # create bike network
    nodes, edges, traversals = bike_net_reader.create_bike_net(settings)

    # Define centroids
    # FIXME - centroids need to be selected intelligently
    centroids = nodes[nodes.taz > 0].index.to_numpy()[
        :100
    ]  # Use first 100 centroids for testing

    # Run the bike route choice model
    final_paths = run_batch_traversals(
        settings=settings,
        nodes=nodes,
        edges=edges,
        traversals=traversals,
        origin_centroids=centroids,
        dest_centroids=centroids,  # For testing, use same centroids for origins and destinations
        num_iterations=2,  # FIXME need setting Number of iterations for randomization
    )

    print("Bike route choice model completed.")
    return final_paths


if __name__ == "__main__":
    # can pass settings file as command line argument
    if len(sys.argv) > 1:
        settings_file = sys.argv[1]
    else:
        settings_file = "bike_route_choice_settings.yaml"
    # load settings
    settings = load_settings(settings_file)

    run_bike_route_choice(settings)
