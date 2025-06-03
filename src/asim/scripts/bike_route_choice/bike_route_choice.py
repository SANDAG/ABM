import os
import numpy as np
import pandas as pd
import random
import matplotlib.pyplot as plt
import networkx as nx
from scipy.sparse import csr_matrix
from scipy.sparse import csr_array, coo_array
from scipy.spatial import cKDTree
from scipy.sparse.csgraph import dijkstra
from multiprocessing import Pool
from numba import njit, types
from numba.typed import Dict
import time


# Global Variables for Network Size
NUM_PRCESSORS = 1
MAX_DISTANCE = (
    10  # Maximum distance for Dijkstra's algorithm to search for shortest paths
)

DATA_DIR = r"T:\ABM\user\aber\bike_route_choice\network"


INACCESSIBLE_COST_COEF = 999.0


# Global Variables for Network Size
NUM_NODES = 2000  # Changeable number of nodes
NUM_CENTROIDS = 50  # Number of centroids (randomly selected nodes)

# San Diego County Approximate Size (in miles)
SAN_DIEGO_LAT_MIN, SAN_DIEGO_LAT_MAX = 0, 10
SAN_DIEGO_LON_MIN, SAN_DIEGO_LON_MAX = 0, 10


def read_bike_network_data(num_centroids=0, zone_level="mgra"):
    """Read actual bike network data from CSV files."""
    print("Reading network data from ", DATA_DIR)
    nodes = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeNodes.csv"))
    edges = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeEdges.csv"))
    traversals = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeTraversals.csv"))

    # take the first n centroids for testing smaller samples
    if num_centroids > 0:
        new_centroids = (
            nodes[nodes.centroid & (nodes[zone_level] > 0)].sample(num_centroids).index
        )
        nodes.centroid = False
        nodes.loc[new_centroids, "centroid"] = True

    print(f"Nodes: {nodes.shape} Edges: {edges.shape} Traversals: {traversals.shape}")

    return nodes, edges, traversals


# def randomize_network_cost(edges, traversals, random_scale):
#     print("Randomizing network costs")
#     edges_rand = edges.copy()
#     traversals_rand = traversals.copy()
#     edges_rand.bikeCost = edges_rand.bikeCost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(edges_rand)))
#     traversals_rand.bikecost = traversals_rand.bikecost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(traversals_rand)))
#     return edges_rand, traversals_rand


def get_edge_cost(edges, coef_dict, random_scale_coef=None, random_scale_link=None):

    coefs = coef_dict.copy()

    if random_scale_coef is not None:
        for name, coef in coefs.items():
            rand_coef = coef * (
                1 + np.random.uniform(0 - random_scale_coef, random_scale_coef)
            )
            coefs[name] = rand_coef

    edge_cost = (
        (
            coefs["distcla0"]
            * edges.distance
            * ((edges.bikeClass < 1) | (edges.bikeClass > 3))
        )
        + (coefs["distcla1"] * edges.distance * ((edges.bikeClass == 1)))
        + (
            coefs["distcla2"]
            * edges.distance
            * ((edges.bikeClass == 2) & (~edges.cycleTrack))
        )
        + (
            coefs["distcla3"]
            * edges.distance
            * ((edges.bikeClass == 3) & (~edges.bikeBlvd))
        )
        + (
            coefs["dartne2"]
            * edges.distance
            * (
                (edges.bikeClass != 2)
                & (edges.bikeClass != 1)
                & (edges.functionalClass < 4)
                & (edges.functionalClass > 0)
            )
        )
        + (
            coefs["dwrongwy"]
            * edges.distance
            * ((edges.bikeClass != 1) & (edges.lanes == 0))
        )
        + (coefs["dcyctrac"] * edges.distance * ((edges.cycleTrack)))
        + (coefs["dbikblvd"] * edges.distance * ((edges.bikeBlvd)))
        + (coefs["gain"] * edges.gain)
    )

    if random_scale_link is not None:
        edge_cost = edge_cost * np.random.choice(
            [(1 - random_scale_link), (1 + random_scale_link)], edge_cost.size
        )

    edge_cost = edge_cost + (
        INACCESSIBLE_COST_COEF
        * ((edges.functionalClass < 3) & (edges.functionalClass > 0))
    )

    return edge_cost


def plot_shortest_path_with_results_buffered(
    nodes, edges, shortest_path_df, origin, destination, iteration, buffer_size=None
):
    """Plot the shortest path between two nodes with additional path information within a square buffer around the origin node."""
    print("Plotting the shortest path...")
    path_data = shortest_path_df[
        (shortest_path_df.origin == origin)
        & (shortest_path_df.destination == destination)
        & (shortest_path_df.iteration == iteration)
    ]
    if path_data.empty:
        print(
            f"No path found between {origin} and {destination} for iteration {iteration}"
        )
        return

    # Get the coordinates of the origin node
    origin_node = nodes[nodes["id"] == origin].iloc[0]
    origin_x, origin_y = origin_node["x"], origin_node["y"]

    if buffer_size:
        # Define the buffer boundaries
        min_x, max_x = origin_x - buffer_size, origin_x + buffer_size
        min_y, max_y = origin_y - buffer_size, origin_y + buffer_size

        # Filter nodes within the buffer
        filtered_nodes = nodes[
            (nodes["x"] >= min_x)
            & (nodes["x"] <= max_x)
            & (nodes["y"] >= min_y)
            & (nodes["y"] <= max_y)
        ]

        # Filter edges to include only those with both nodes within the buffer
        filtered_edges = edges[
            edges["fromNode"].isin(filtered_nodes["id"])
            & edges["toNode"].isin(filtered_nodes["id"])
        ]

        # check to make sure destination node is also in the buffer
        if destination not in filtered_nodes["id"].values:
            print(
                f"Destination node {destination} is not in the buffer size of {buffer_size}"
            )

    else:
        filtered_nodes = nodes
        filtered_edges = edges

    # Create a graph from the filtered nodes and edges
    G = nx.Graph()
    G.add_nodes_from(
        [
            (node["id"], {"pos": (node["x"], node["y"])})
            for _, node in filtered_nodes.iterrows()
        ]
    )
    G.add_edges_from(
        [
            (edge.fromNode, edge.toNode, {"weight": edge.distance})
            for _, edge in filtered_edges.iterrows()
        ]
    )

    # Extract positions for plotting
    pos = nx.get_node_attributes(G, "pos")

    # Plot the network
    plt.figure(figsize=(10, 10))
    nx.draw(
        G, pos, node_size=10, with_labels=False, edge_color="gray", alpha=0.5, width=0.5
    )

    # Highlight the shortest path
    path_edges = [
        (path_data.fromNode.iloc[i], path_data.toNode.iloc[i])
        for i in range(len(path_data))
    ]
    path_nodes = set(path_data.fromNode).union(set(path_data.toNode))
    nx.draw_networkx_nodes(
        G, pos, nodelist=path_nodes, node_color="red", node_size=50, label="Path Nodes"
    )
    nx.draw_networkx_edges(
        G, pos, edgelist=path_edges, edge_color="blue", width=2, label="Shortest Path"
    )

    # Highlight the origin and destination nodes
    nx.draw_networkx_nodes(
        G, pos, nodelist=[origin], node_color="green", node_size=100, label="Origin"
    )
    nx.draw_networkx_nodes(
        G,
        pos,
        nodelist=[destination],
        node_color="purple",
        node_size=100,
        label="Destination",
    )

    # Calculate path information
    num_edges = len(path_edges)
    total_distance = path_data["distance"].sum()

    total_cost = path_data["cost_total"].sum()
    turns = path_data[path_data.turnType > 0]["turnType"].count()
    path_size = path_data.iloc[0]["path_size"]
    # Add path information to the plot
    info_text = (
        f"Origin: {origin}\n"
        f"Destination: {destination}\n"
        f"Iteration: {iteration}\n"
        f"Number of Edges: {num_edges}\n"
        f"Total Distance: {total_distance:.2f} units\n"
        f"Turns: {turns}\n"
        f"Total Cost: {total_cost:.2f}\n"
        f"Path Size: {path_size:.2f}"
    )

    plt.text(
        0.05,
        0.95,
        info_text,
        transform=plt.gca().transAxes,
        fontsize=12,
        verticalalignment="top",
        bbox=dict(boxstyle="round,pad=0.3", edgecolor="black", facecolor="white"),
    )

    # Add a legend
    plt.legend(loc="upper right")
    plt.title(
        f"Shortest Path from Node {origin} to Node {destination} for iteration {iteration}"
    )
    plt.show(block=True)


def process_paths_new(centroids, predecessors):
    print("Processing paths without numba...")

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
    print("Calculating logsums...")

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
    # shortest_paths = {}
    # for centroid, distance_mat, predecessor_mat in zip(centroids, distances, predecessors):
    #     shortest_paths[centroid] = (distance_mat, predecessor_mat)

    return (distances, predecessors)


def perform_dijkstras_algorithm_batch_traversals(
    nodes, edges, traversals, origin_centroids, limit=3
):
    """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
    num_edges = len(edges)

    # # node mapping needs to start at 0 in order to create adjacency matrix
    edge_mapping = edges[["fromNode", "toNode", "edgeCost"]].reset_index()

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
    # Total bike cost is edge cost (after traversal) plus traversal cost
    # Note origin zone connector edge cost is not included with this approach, but this has no impact on shortest path selection
    traversals_mapped["bikeCostTotal"] = (
        traversals_mapped.edgeCostToEdge + traversals_mapped.bikecost
    )
    traversals_mapped.loc[
        traversals_mapped.indexFromEdge.isin(origin_centroids), "bikeCostTotal"
    ] += traversals_mapped.edgeCostFromEdge

    # Create a sparse adjacency matrix
    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.bikeCostTotal.to_numpy()
    adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_edges, num_edges))

    print(f"Need to calculate Dijkstra's on {len(origin_centroids)} centroids")

    # Perform Dijkstra's algorithm for all centroids
    shortest_paths = _perform_dijkstra(origin_centroids, adjacency_matrix, limit)
    return shortest_paths


def run_iterations_batch_traversals(
    nodes,
    edges,
    traversals,
    origin_centroids,
    dest_centroids,
    cost_limit,
    num_iterations,
    coef_dict,
    random_scale_coef,
    random_scale_link,
):

    all_paths = []

    for i in range(num_iterations):
        # randomize costs
        # edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)
        edges_rand = edges.copy()
        edges_rand["edgeCost"] = get_edge_cost(
            edges, coef_dict, random_scale_coef, random_scale_link
        )

        # run dijkstra's
        distances, predecessors = perform_dijkstras_algorithm_batch_traversals(
            nodes, edges_rand, traversals, origin_centroids, limit=cost_limit
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
    nodes,
    edges,
    traversals,
    origin_centroids,
    dest_centroids,
    cost_limit,
    num_iterations,
    coef_dict,
    random_scale_coef,
    random_scale_link,
    trace_origins=[],
    trace_dests=[],
):
    (
        all_paths_orig,
        all_paths_dest,
        all_paths_from_edge,
        all_paths_to_edge,
        all_paths_iteration,
    ) = run_iterations_batch_traversals(
        nodes,
        edges,
        traversals,
        origin_centroids,
        dest_centroids,
        cost_limit,
        num_iterations,
        coef_dict,
        random_scale_coef,
        random_scale_link,
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
