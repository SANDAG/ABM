import sys
import numpy as np
import pandas as pd
import geopandas as gpd
import logging
import warnings
from scipy.sparse import csr_matrix
from scipy.sparse import csr_array, coo_array
from scipy.sparse.csgraph import dijkstra
from multiprocessing import Pool

import bike_net_reader
import bike_route_calculator
from bike_route_utilities import BikeRouteChoiceSettings, load_settings

# Set up logging
logger = logging.getLogger(__name__)


def process_paths_new(centroids, predecessors):
    """
    Converts the predecessors array from Dijkstra's algorithm into paths.

    Args:
        centroids (list): List of centroid indices.
        predecessors (np.ndarray): Predecessors array from Dijkstra's algorithm.

    Returns:
        tuple: A tuple containing:
            - paths_orig (np.ndarray): Origin indices for paths.
            - paths_dest (np.ndarray): Destination indices for paths.
            - paths_from_node (np.ndarray): From-node indices for paths.
            - paths_to_node (np.ndarray): To-node indices for paths.
    """
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
    settings,
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
    trace_origins=[],
    trace_dests=[],
):
    """
    Calculate the final logsums for the bike network using pre-computed paths and traversals.
    Includes path size calculation.

    Args:
        settings: BikeRouteChoiceSettings
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
            - trace_paths_edge_cost (np.ndarray): Edge cost for traced paths.
            - trace_paths_trav_cost (np.ndarray): Traversal cost for traced paths.
    """
    logger.info("Calculating logsums...")

    # Mapped node id to centroids index
    dest_centroids_rev_map = np.zeros(int(max(dest_centroids)) + 1, dtype=np.int32)
    dest_centroids_rev_map[dest_centroids] = range(len(dest_centroids))
    all_paths_dest_rev = dest_centroids_rev_map[all_paths_dest]

    node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.index)}

    edges = edges.reset_index()
    edge_from_node = edges.fromNode.map(node_mapping).to_numpy()
    edge_to_node = edges.toNode.map(node_mapping).to_numpy()
    edge_cost = edges.edge_utility.to_numpy()
    edge_length = edges.distance.to_numpy()
    edge_ids = edges.edgeID.to_numpy()

    all_paths_from_node = edge_from_node[all_paths_to_edge]
    all_paths_to_node = edge_to_node[all_paths_to_edge]
    all_paths_edge_cost = edge_cost[all_paths_to_edge]
    all_paths_edge_length = edge_length[all_paths_to_edge]
    all_paths_edge_id = edge_ids[all_paths_to_edge]

    if len(trace_origins) > 0:
        all_paths_prev_node = edge_from_node[all_paths_from_edge]

    num_edges = len(edges)

    # node mapping needs to start at 0 in order to create adjacency matrix
    # using edges instead of nodes since the edges are treated as nodes in the Dijkstra's algorithm
    # constructing edge_mapping with columns [index, fromNode, toNode]
    edge_mapping = edges[["fromNode", "toNode"]].reset_index()

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

    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.traversal_utility.to_numpy()
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
    all_paths_edge_id = np.concatenate(
        (all_paths_edge_id, edge_ids[all_paths_from_edge][orig_connectors_indices])
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
    all_paths_cost = all_paths_edge_cost + all_paths_trav_cost

    # SciPy Sparse arrays only surrport 2d arrays, so flatten OD and link indices
    paths_od_ravel = np.ravel_multi_index(
        (all_paths_orig_new, all_paths_dest_rev_new),
        (len(origin_centroids), len(dest_centroids)),
    )
    paths_link_ravel = np.ravel_multi_index(
        (all_paths_from_node, all_paths_to_node), (len(nodes), len(nodes))
    )

    # extract paths for OD pairs that are being traced
    if len(trace_origins) > 0:
        # trace_indices = (np.isin(all_paths_orig_new, trace_origins) & np.isin(all_paths_dest_rev_new, trace_dests)).nonzero()[0]
        all_paths_prev_node = np.concatenate(
            (all_paths_prev_node, np.full_like(orig_connectors_indices, -1))
        )
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
        trace_paths_edge_cost = all_paths_edge_cost[trace_indices]
        trace_paths_trav_cost = all_paths_trav_cost[trace_indices]
        trace_paths_edge_id = all_paths_edge_id[trace_indices]

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
        (len(origin_centroids), len(dest_centroids), settings.number_of_iterations),
    )
    od_iter_length_total = np.bincount(paths_od_iter_ravel, all_paths_edge_length)  # Li

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        od_iter_path_size = (
            np.bincount(paths_od_iter_ravel, all_paths_size_component)
            / od_iter_length_total
        )  # sum( la / Man ) / Li
    
    od_iter_cost = np.bincount(paths_od_iter_ravel, all_paths_cost)

    # extract path sizes for OD pairs that are being traced
    if len(trace_origins) > 0:
        trace_paths_od_iter_ravel = np.ravel_multi_index(
            (trace_paths_orig, trace_paths_dest_rev, trace_paths_iteration),
            (len(origin_centroids), len(dest_centroids), settings.number_of_iterations),
        )
        trace_paths_path_size = od_iter_path_size[trace_paths_od_iter_ravel]

    # Unflatten OD and iteration indices, no longer need individual links
    od_iter_indices = (od_iter_length_total > 0).nonzero()[0]
    paths_od_iter_orig, paths_od_iter_dest, paths_od_iter_iter = np.unravel_index(
        od_iter_indices,
        (len(origin_centroids), len(dest_centroids), settings.number_of_iterations),
    )
    paths_od_iter_cost = od_iter_cost[od_iter_indices]
    paths_od_iter_path_size = od_iter_path_size[od_iter_indices]
    paths_od_iter_length_total = od_iter_length_total[od_iter_indices]

    # Normalize path size, need to sum path size by OD
    paths_od_ravel = np.ravel_multi_index(
        (paths_od_iter_orig, paths_od_iter_dest),
        (len(origin_centroids), len(dest_centroids)),
    )
    od_count_iter = np.bincount(paths_od_ravel)
    od_path_size_sum = np.bincount(paths_od_ravel, paths_od_iter_path_size)
    paths_od_iter_path_size_sum = od_path_size_sum[paths_od_ravel]
    paths_od_iter_path_size_normalized = (
        paths_od_iter_path_size / paths_od_iter_path_size_sum
    )

    # Add path cost to utility function
    paths_od_iter_utility = paths_od_iter_cost + np.log(
        paths_od_iter_path_size_normalized
    )
    paths_od_iter_utility_exp = np.exp(paths_od_iter_utility)

    od_utility_exp_sum = np.bincount(paths_od_ravel, paths_od_iter_utility_exp)
    paths_od_iter_utility_exp_sum = od_utility_exp_sum[paths_od_ravel]
    paths_od_iter_prob = paths_od_iter_utility_exp / paths_od_iter_utility_exp_sum

    paths_od_iter_length_weighted = paths_od_iter_length_total * paths_od_iter_prob

    # Unflatten OD indices, no longer need iterations
    od_indices = (od_path_size_sum > 0).nonzero()[0]
    paths_od_orig, paths_od_dest = np.unravel_index(
        od_indices, (len(origin_centroids), len(dest_centroids))
    )

    paths_od_count_iter = od_count_iter[od_indices]
    paths_od_path_size_sum = od_path_size_sum[od_indices]

    # Average path length
    od_dist = np.bincount(paths_od_ravel, paths_od_iter_length_weighted)
    paths_od_dist = od_dist[od_indices]

    # Logsum calculation
    od_logsum = np.bincount(paths_od_ravel, paths_od_iter_utility_exp)
    paths_od_logsum = np.log(od_logsum[od_indices])

    # Centroids index to mapped node id
    origin_centroids_np = np.array(origin_centroids)
    paths_od_orig_mapped = origin_centroids_np[paths_od_orig]
    dest_centroids_np = np.array(dest_centroids)
    paths_od_dest_mapped = dest_centroids_np[paths_od_dest]

    # edge mapping is used to map origins and destinations back to their original node ids
    # this awkward mapping is necessary because the edges are treated as nodes in the dijkstra's algorithm
    fromNode_map = edge_mapping.set_index("index")["fromNode"].to_dict()
    toNode_map = edge_mapping.set_index("index")["toNode"].to_dict()

    paths_od_orig_mapped = np.array([fromNode_map[i] for i in paths_od_orig_mapped])
    paths_od_dest_mapped = np.array([toNode_map[i] for i in paths_od_dest_mapped])

    if len(trace_origins) > 0:
        # remapping traced origins and destinations from 0 index to edge index, then edge index to node id
        trace_paths_orig_mapped = origin_centroids_np[trace_paths_orig]
        trace_paths_dest_mapped = dest_centroids_np[trace_paths_dest_rev]
        trace_paths_orig_mapped = np.array(
            [fromNode_map[i] for i in trace_paths_orig_mapped]
        )
        trace_paths_dest_mapped = np.array(
            [toNode_map[i] for i in trace_paths_dest_mapped]
        )

        # paths themselves just need to be mapped back from 0-index to node id
        rev_node_mapping = {v: k for k, v in node_mapping.items()}
        rev_node_mapping[-1] = -1  # map no path -1 to itself
        trace_paths_prev_node = np.array(
            [rev_node_mapping[i] for i in trace_paths_prev_node]
        )
        trace_paths_from_node = np.array(
            [rev_node_mapping[i] for i in trace_paths_from_node]
        )
        trace_paths_to_node = np.array(
            [rev_node_mapping[i] for i in trace_paths_to_node]
        )

        return (
            paths_od_orig_mapped,
            paths_od_dest_mapped,
            paths_od_dist,
            paths_od_logsum,
            paths_od_path_size_sum,
            paths_od_count_iter,
            trace_paths_orig_mapped,
            trace_paths_dest_mapped,
            trace_paths_iteration,
            trace_paths_prev_node,
            trace_paths_from_node,
            trace_paths_to_node,
            trace_paths_path_size,
            trace_paths_edge_cost,
            trace_paths_trav_cost,
            trace_paths_edge_id,
        )
    else:
        return (
            paths_od_orig_mapped,
            paths_od_dest_mapped,
            paths_od_dist,
            paths_od_logsum,
            paths_od_path_size_sum,
            paths_od_count_iter,
            np.empty((0)),
            np.empty((0)),
            np.empty((0)),
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
    logger.info(
        f"Processing Dijkstra's on {len(centroids)} centroids with limit={limit}..."
    )
    distances, predecessors = dijkstra(
        adjacency_matrix,
        directed=True,
        indices=centroids,
        return_predecessors=True,
        limit=limit,
    )

    return (distances, predecessors)


def perform_dijkstras_algorithm_batch_traversals(
    edges, traversals, origin_centroids, limit
):
    """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
    num_edges = len(edges)

    # node mapping needs to start at 0 in order to create adjacency matrix
    # fromNode and toNode index is saved as columns in edges
    # then reindex again to get index of the edge
    edge_mapping = edges["edge_utility"]

    traversals_mapped = (
        traversals.reset_index()
        .merge(
            edge_mapping,
            how="left",
            left_on="edgeID_fromEdge",
            right_index=True
        )
        .merge(
            edge_mapping,
            how="left",
            left_on="edgeID_toEdge",
            right_index=True,
            suffixes=("FromEdge", "ToEdge"),
        )
    )
    # Total bike cost is edge cost (after traversal) plus traversal cost
    # Note origin zone connector edge cost is not included with this approach, but this has no impact on shortest path selection
    traversals_mapped["total_utility"] = (
        traversals_mapped.edge_utilityToEdge + traversals_mapped.traversal_utility
    )
    traversals_mapped.loc[
        traversals_mapped.edgeID_fromEdge.isin(origin_centroids), "total_utility"
    ] += traversals_mapped.edge_utilityFromEdge

    # Convert from negative utility to positive cost for Dijkstra's
    traversals_mapped.total_utility *= -1

    # Create a sparse adjacency matrix
    row = traversals_mapped.edgeID_fromEdge.to_numpy()
    col = traversals_mapped.edgeID_toEdge.to_numpy()
    data = traversals_mapped.total_utility.to_numpy()
    adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_edges, num_edges))

    logger.info(f"Need to calculate Dijkstra's on {len(origin_centroids)} centroids")

    # Perform Dijkstra's algorithm for all centroids
    shortest_paths = _perform_dijkstra(origin_centroids, adjacency_matrix, limit)
    return shortest_paths


def run_iterations_batch_traversals(
    settings: BikeRouteChoiceSettings,
    edges: pd.DataFrame,
    traversals: pd.DataFrame,
    origin_centroids: list,
    dest_centroids: list,
):
    """
    Run multiple iterations of Dijkstra's algorithm using traversals as "links" and edges as "vertices".
    For each iteration, it calculates utilities for edges and traversals, then performs Dijkstra's algorithm to find paths.

    Args:
        settings: BikeRouteChoiceSettings
        edges: pd.DataFrame
        traversals: pd.DataFrame
        origin_centroids: list
        dest_centroids: list

    Returns:
        tuple: A tuple containing arrays of path information.
    """

    all_paths = []

    for i in range(settings.number_of_iterations):
        logger.info(f"Running iteration {i + 1} of {settings.number_of_iterations}")
        # calculating utilties with randomness
        edges["edge_utility"] = bike_route_calculator.calculate_utilities_from_spec(
            settings,
            choosers=edges,
            spec_file=settings.edge_util_file,
            trace_label=f"bike_edge_utilities_iteration_{i}",
            randomize=True,
        )
        traversals[
            "traversal_utility"
        ] = bike_route_calculator.calculate_utilities_from_spec(
            settings,
            choosers=traversals,
            spec_file=settings.traversal_util_file,
            trace_label=f"bike_traversal_utilities_iteration_{i}",
            randomize=False,
        )
        # convert edge utility to distance
        avg_utility_df = edges.loc[(edges.edge_utility > -999) & (edges.distance > 0)]
        avg_utility_per_mi = (avg_utility_df["edge_utility"] 
                              / avg_utility_df["distance"]
                              ).mean() + traversals.traversal_utility.mean()
        
        utility_limit = -1 * settings.max_dijkstra_distance * avg_utility_per_mi 

        # run dijkstra's
        distances, predecessors = perform_dijkstras_algorithm_batch_traversals(
            edges, traversals, origin_centroids, limit=utility_limit
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
    trace_origin_edgepos,
    trace_dest_edgepos,
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
        edges,
        traversals,
        origin_centroids,
        dest_centroids,
    )
    trace_origins_rev = []
    trace_dests_rev = []

    if len(settings.trace_origins) > 0:

        filtered_trace_origs = trace_origin_edgepos[np.isin(trace_origin_edgepos,origin_centroids)]

        trace_origins_rev = pd.DataFrame(
            enumerate(origin_centroids),
            columns=['subset_pos','edge_pos']
            ).set_index(
                'edge_pos'
            ).loc[
                filtered_trace_origs
            ].subset_pos.values

        filtered_trace_dests = trace_dest_edgepos[np.isin(trace_dest_edgepos,dest_centroids)]

        trace_dests_rev = pd.DataFrame(
            enumerate(dest_centroids),
            columns=['subset_pos','edge_pos']
            ).set_index(
                'edge_pos'
            ).loc[
                filtered_trace_dests
            ].subset_pos.values

    # calculate non-randomized utilities for edges and traversals to use in final logsum calculation
    edges["edge_utility"] = bike_route_calculator.calculate_utilities_from_spec(
        settings,
        choosers=edges,
        spec_file=settings.edge_util_file,
        trace_label="bike_edge_utilities_final",
        randomize=False,
    )
    traversals[
        "traversal_utility"
    ] = bike_route_calculator.calculate_utilities_from_spec(
        settings,
        choosers=traversals,
        spec_file=settings.traversal_util_file,
        trace_label="bike_traversal_utilities_final",
        randomize=False,
    )

    final_paths = calculate_final_logsums_batch_traversals(
        settings,
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
        trace_origins_rev,
        trace_dests_rev,
    )
    return final_paths


def get_centroid_connectors(
    settings: BikeRouteChoiceSettings, nodes: pd.DataFrame, edges: pd.DataFrame
):
    """
    Generate centroids for the bike route choice model.
    This function is a placeholder and should be replaced with a more intelligent centroid selection method.

    Returns:
        Index of edge IDs of corresponding connectors
    """
    # node mapping needs to start at 0 in order to create adjacency matrix
    # constructing edge_mapping with columns [index, fromNode, toNode]
    edge_mapping = edges[["fromNode", "toNode"]]

    # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
    # Centroid connectors
    origin_centroid_connectors = nodes[
        nodes["centroid"] & (nodes[settings.zone_level] > 0)
    ].merge(edge_mapping, how="left", left_index=True, right_on="fromNode")

    dest_centroid_connectors = nodes[nodes["centroid"] & (nodes[settings.zone_level] > 0)].merge(
        edge_mapping, how="left", left_index=True, right_on="toNode"
    )

    if isinstance(settings.zone_subset, list):
        # filter centroids based on zone_subset if it is a list
        origin_centroid_connectors = origin_centroid_connectors[
            origin_centroid_connectors[settings.zone_level].isin(settings.zone_subset)
        ]
        dest_centroid_connectors = dest_centroid_connectors[dest_centroid_connectors[settings.zone_level].isin(settings.zone_subset)]
    elif isinstance(settings.zone_subset, int):
        # take the first N centroids if zone_subset is an integer
        origin_centroid_connectors = origin_centroid_connectors[: settings.zone_subset]
        dest_centroid_connectors = dest_centroid_connectors[: settings.zone_subset]

    def _clean_centroid_connectors(df, label, edge_mapping):
        null_rows = df[df.isnull().any(axis=1)]
        if not null_rows.empty:
            logger.warning(
                f"Null columns found in {label} centroids dataframe! Dropping:\n {null_rows}"
            )
            df = df.dropna()
            df.index = df.index.astype(np.int32)
        return df

    origin_centroid_connectors = _clean_centroid_connectors(origin_centroid_connectors, "origin", edge_mapping)
    dest_centroid_connectors = _clean_centroid_connectors(dest_centroid_connectors, "destination", edge_mapping)

    origin_centroid_connectors = origin_centroid_connectors[settings.zone_level]
    dest_centroid_connectors = dest_centroid_connectors[settings.zone_level]

    return origin_centroid_connectors, dest_centroid_connectors


def run_bike_route_choice(settings):
    """Main function to run the bike route choice model."""

    # create bike network
    nodes, edges, traversals = bike_net_reader.create_bike_net(settings)

    # Define centroids
    origin_centroid_connectors, dest_centroid_connectors = get_centroid_connectors(settings, nodes, edges)

    trace_origins_edgepos = np.array(origin_centroid_connectors[origin_centroid_connectors.isin(settings.trace_origins)].index)
    trace_dests_edgepos = np.array(dest_centroid_connectors[dest_centroid_connectors.isin(settings.trace_destinations)].index)

    # drop zone IDs
    origin_centroid_connectors = origin_centroid_connectors.index
    dest_centroid_connectors = dest_centroid_connectors.index


    logger.info(
        f"Splitting {len(origin_centroid_connectors)} origins into {settings.number_of_batches} batches"
    )
    origin_centroid_batches = np.array_split(
        origin_centroid_connectors, settings.number_of_batches
    )

    # run the bike route choice model in either single or multi-process mode
    if settings.number_of_processors > 1:
        # Split origin centroids into batche
        final_paths = []
        for origin_centroid_batch in origin_centroid_batches:
            logger.info(
                f"Splitting batch of {len(origin_centroid_batch)} origins into {settings.number_of_processors} processes"
            )
            origin_centroid_sub_batches = np.array_split(
                origin_centroid_batch, settings.number_of_processors
            )
            with Pool(processes=settings.number_of_processors) as pool:
                results = pool.starmap(
                    run_batch_traversals,
                    [
                        (
                            settings,
                            nodes,
                            edges.drop(columns='geometry'),
                            traversals,
                            origin_centroid_sub_batch,
                            dest_centroid_connectors,
                            trace_origins_edgepos,
                            trace_dests_edgepos,
                        )
                        for origin_centroid_sub_batch in origin_centroid_sub_batches
                    ],
                )
                final_paths.extend(results)

    else:
        final_paths = []
        for i, origin_centroid_batch in enumerate(origin_centroid_batches):
            logger.info(
                f"Processing batch {i+1} of {len(origin_centroid_batch)} origins"
            )
            results = run_batch_traversals(
                settings=settings,
                nodes=nodes,
                edges=edges.drop(columns='geometry'),
                traversals=traversals,
                origin_centroids=origin_centroid_batch,
                dest_centroids=dest_centroid_connectors,
                trace_origin_edgepos=trace_origins_edgepos,
                trace_dest_edgepos=trace_dests_edgepos,
            )
            final_paths.append(results)

    final_paths_concat = tuple(map(np.concatenate,zip(*final_paths)))

    logsums = pd.DataFrame(
        {
            "origin": final_paths_concat[0],
            "destination": final_paths_concat[1],
            "distance": final_paths_concat[2],
            "logsum": final_paths_concat[3],
            "path_size_sum": final_paths_concat[4],
            "iterations": final_paths_concat[5]
        }
    )

    # calculate replacement intrazonal logsum values
    diag_logsums = logsums[logsums.origin!=logsums.destination].groupby('origin').logsum.max()
    diag_logsums[diag_logsums < 0] *= 0.5
    diag_logsums[diag_logsums >= 0] *= 2

    diags = diag_logsums.to_frame()

    # calculate replacement intrazonal distance values
    diags['distance'] = logsums[logsums.origin!=logsums.destination].groupby('origin').distance.min() * 0.5

    # indexing work
    diags = diags.reset_index()
    diags['destination'] = diags.origin
    diags = diags.set_index(['origin','destination'])

    diags['path_size_sum'] = 0
    diags['iterations'] = 0

    # replace the values in the logsums table
    logsums = logsums.set_index(['origin','destination'])
    logsums = diags.combine_first(logsums)
    # logsums.loc[diags.index,'distance'] = diags.distance
    # logsums.loc[diags.index,'logsum'] = diags.logsum
    # logsums.loc[diags.index,'path_size_sum'] = np.nan

    logsums.to_csv(f"{settings.output_path}/bike_route_choice_logsums.csv")

    logsums['time'] = (logsums['distance'] / settings.bike_speed) * 60

    logsums = logsums.merge(nodes[[settings.zone_level]], how='left', left_on='origin', right_index=True)
    logsums = logsums.rename(columns={settings.zone_level: 'i'})
    logsums = logsums.merge(nodes[[settings.zone_level]], how='left', left_on='destination', right_index=True)
    logsums = logsums.rename(columns={settings.zone_level: 'j'})

    if settings.zone_level == 'mgra':
        logsums = logsums[(logsums.iterations == 0) | (logsums.iterations >= settings.min_iterations)]

    logsums = logsums[['i','j','logsum','time']]

    logsums.to_csv(settings.output_file_path,index=False)

    # Save the final paths to a CSV file
    if len(settings.trace_origins) > 0:
        trace_paths = pd.DataFrame(
            {
                "origin": final_paths_concat[6],
                "destination": final_paths_concat[7],
                "iteration": final_paths_concat[8],
                "prev_node": final_paths_concat[9],
                "from_node": final_paths_concat[10],
                "to_node": final_paths_concat[11],
                "path_size": final_paths_concat[12],
                "edge_cost": final_paths_concat[13],
                "traversal_cost": final_paths_concat[14],
                "edgeID":final_paths_concat[15],
            }
        )
        trace_paths.to_csv(
            f"{settings.output_path}/bike_route_choice_trace.csv", index=False
        )

        if settings.generate_shapefile:
            logger.info("Generating shapefile...")

            
            # # attach edges to the paths
            trace_paths = gpd.GeoDataFrame(
                trace_paths.merge(
                    edges['geometry'],
                    left_on=['edgeID'],
                    right_index=True,
                    how='left'),
                crs=settings.crs
                )
            
            logger.info("Writing shapefile...")
            trace_paths.to_file(f"{settings.output_path}/bike_route_choice_trace.shp")


    print("Bike route choice model completed.")
    return final_paths_concat


if __name__ == "__main__":
    # can pass settings file as command line argument
    if len(sys.argv) > 1:
        settings_file = sys.argv[1]
    else:
        settings_file = "bike_route_choice_settings.yaml"
    # load settings
    settings = load_settings(settings_file)

    run_bike_route_choice(settings)
