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
MAX_DISTANCE = 10  # Maximum distance for Dijkstra's algorithm to search for shortest paths

DATA_DIR = r"T:\ABM\user\aber\bike_route_choice\network"


INACCESSIBLE_COST_COEF = 999.0


# Global Variables for Network Size
NUM_NODES = 2000  # Changeable number of nodes
NUM_CENTROIDS = 50  # Number of centroids (randomly selected nodes)

# San Diego County Approximate Size (in miles)
SAN_DIEGO_LAT_MIN, SAN_DIEGO_LAT_MAX = 0, 10  
SAN_DIEGO_LON_MIN, SAN_DIEGO_LON_MAX = 0, 10


def read_bike_network_data(num_centroids=0, zone_level='mgra'):
    """Read actual bike network data from CSV files."""
    print("Reading network data from ", DATA_DIR)
    nodes = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeNodes.csv"))
    edges = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeEdges.csv"))
    traversals = pd.read_csv(os.path.join(DATA_DIR, "derivedBikeTraversals.csv"))

    # take the first n centroids for testing smaller samples
    if num_centroids > 0:
        new_centroids = nodes[nodes.centroid & (nodes[zone_level] > 0)].sample(num_centroids).index
        nodes.centroid = False
        nodes.loc[new_centroids, 'centroid'] = True

    print(f"Nodes: {nodes.shape} Edges: {edges.shape} Traversals: {traversals.shape}")

    return nodes, edges, traversals

# def randomize_network_cost(edges, traversals, random_scale):
#     print("Randomizing network costs")
#     edges_rand = edges.copy()
#     traversals_rand = traversals.copy()
#     edges_rand.bikeCost = edges_rand.bikeCost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(edges_rand)))
#     traversals_rand.bikecost = traversals_rand.bikecost * (1 + random_scale * np.random.uniform(-1.0,1.0,len(traversals_rand)))
#     return edges_rand, traversals_rand


def get_edge_cost(edges, coef_dict, random_scale_coef = None, random_scale_link = None):
    
    coefs = coef_dict.copy()

    if random_scale_coef is not None:
        for name, coef in coefs.items():
            rand_coef = coef * (1 + np.random.uniform(0-random_scale_coef, random_scale_coef))
            coefs[name] = rand_coef

    edge_cost = (
        coefs['distcla0'] * edges.distance * (
            (edges.bikeClass < 1)
            | (edges.bikeClass > 3)
        )
    ) + (
        coefs['distcla1'] * edges.distance * (
            (edges.bikeClass == 1)
        )
    ) + (
        coefs['distcla2'] * edges.distance * (
            (edges.bikeClass == 2) 
            & (~edges.cycleTrack)
        )
    ) + (
        coefs['distcla3'] * edges.distance * (
            (edges.bikeClass == 3)
            & (~edges.bikeBlvd)
        )
    ) + (
        coefs['dartne2'] * edges.distance * (
            (edges.bikeClass != 2)
            & (edges.bikeClass != 1)
            & (edges.functionalClass < 4)
            & (edges.functionalClass > 0)
        )
    ) + (
        coefs['dwrongwy'] * edges.distance * (
            (edges.bikeClass != 1)
            & (edges.lanes == 0)
        )
    ) + (
        coefs['dcyctrac'] * edges.distance * (
            (edges.cycleTrack)
        )
    ) + (
        coefs['dbikblvd'] * edges.distance * (
            (edges.bikeBlvd)
        )
    ) + (
        coefs['gain'] * edges.gain
    )

    if random_scale_link is not None:
        edge_cost = edge_cost * np.random.choice([(1-random_scale_link),(1+random_scale_link)],edge_cost.size)

    edge_cost = edge_cost + (
        INACCESSIBLE_COST_COEF * (
            (edges.functionalClass < 3)
            & (edges.functionalClass > 0)
        )
    )
    
    return edge_cost


# def remove_cost_differences(nodes, edges, traversals, zone_level):
#     # Need to remove origin zone connector cost and traversal cost to test with identical costs with and without traversals
#     print("Removing differences in cost for testing with and without traversals")
#     traversals.bikecost = 0
#     # edges.loc[edges.fromNode.isin(nodes[nodes['centroid'] & (nodes[zone_level] > 0)].id), 'bikeCost'] = 0
#     return nodes, edges, traversals


# # Generate Random Node Positions within San Diego County
# def generate_random_nodes(num_nodes):
#     latitudes = np.random.uniform(SAN_DIEGO_LAT_MIN, SAN_DIEGO_LAT_MAX, num_nodes)
#     longitudes = np.random.uniform(SAN_DIEGO_LON_MIN, SAN_DIEGO_LON_MAX, num_nodes)
#     return np.column_stack((latitudes, longitudes))


# def create_edges(nodes, max_neighbors):
#     node_positions = nodes[['x', 'y']].values
#     node_ids = nodes['id'].to_numpy()
#     tree = cKDTree(node_positions)
#     edges = []

#     neighbors = np.random.randint(1, max_neighbors, len(nodes))  # Random number of neighbors for each node

#     # Find the nearest neighbors for all nodes
#     distances, indices = tree.query(node_positions, k=max_neighbors)

#     # Create edges by iterating over the nodes and their neighbors
#     # for i in range(len(nodes)):
#     #     from_node = nodes.iloc[i]['id']
#     #     for j, dist in zip(indices[i][1:], distances[i][1:]):  # Skip the node itself (index 0)
#     #         to_node = nodes.iloc[j]['id']
#     #         edges.append((from_node, to_node, dist))  # (fromNode, toNode, distance)
#     edges = [
#         (node_ids[i], node_ids[j], dist)
#         for i in range(len(nodes))
#         for j, dist in zip(indices[i][1:], distances[i][1:])  # Skip the node itself (index 0)
#     ]
#     return edges


# def create_dummy_network():
#     """
#     Create a dummy network with the same format as the actual data.
#     Generates nodes with random coordinates and edges between them.
#     """
#     print("Creating a dummy network...")
#     # Generate Random Node Positions within San Diego County
#     num_nodes = NUM_NODES
#     nodes = generate_random_nodes(num_nodes)

#     # Create a DataFrame for nodes similar to the actual data format
#     nodes_df = pd.DataFrame({
#         'id': np.arange(num_nodes),
#         'x': nodes[:, 0],
#         'y': nodes[:, 1],
#         'mgra': np.zeros(num_nodes, dtype=int),
#         'taz': np.zeros(num_nodes, dtype=int),
#         'signalized': False,
#         'centroid': False
#     })
#     centroids = nodes_df.sample(NUM_CENTROIDS).index  # Randomly select centroids
#     nodes_df.loc[centroids, 'centroid'] = True
#     nodes_df.loc[centroids, 'mgra'] = np.arange(1, NUM_CENTROIDS + 1)  # Assign unique MGRA to centroids
#     nodes_df.loc[centroids, 'taz'] = np.arange(1, NUM_CENTROIDS + 1)  # Assign unique TAZ to centroids

#     # Create random edges between nodes
#     edges = create_edges(nodes_df, max_neighbors=5)

#     # Create a DataFrame for edges similar to the actual data format
#     edges_df = pd.DataFrame(edges, columns=['fromNode', 'toNode', 'distance'])
#     edges_df['bikeClass'] = np.random.randint(1, 4, size=len(edges))  # Random bike class
#     edges_df['lanes'] = np.random.randint(1, 3, size=len(edges))  # Random number of lanes
#     edges_df['functionalClass'] = np.random.randint(1, 6, size=len(edges))  # Random functional class
#     edges_df['autosPermitted'] = np.random.choice([True, False], size=len(edges))  # Random autos permitted flag
#     edges_df['cycleTrack'] = np.random.choice([True, False], size=len(edges))  # Random cycle track flag
#     edges_df['bikeBlvd'] = np.random.choice([True, False], size=len(edges))  # Random bike boulevard flag
#     edges_df['gain'] = np.random.uniform(-10, 10, size=len(edges))  # Random gain (elevation)
#     edges_df['bikeCost'] = np.random.uniform(0, 5, size=len(edges))  # Random bike cost
#     edges_df['walkCost'] = np.random.uniform(0, 5, size=len(edges))  # Random walk cost
#     edges_df['centroidConnector'] = np.where(edges_df['fromNode'].isin(centroids) | edges_df['toNode'].isin(centroids), True, False)

#     return nodes_df, edges_df


# def plot_network(nodes, edges):
#     """Plot the network using NetworkX and Matplotlib."""
#     G = nx.Graph()
    
#     # Add nodes and edges to the graph
#     for _, node in nodes.iterrows():
#         G.add_node(node['id'], pos=(node['x'], node['y']))
    
#     for edge in edges:
#         G.add_edge(edge[0], edge[1], weight=edge[2])
    
#     # Extract positions for plotting
#     pos = nx.get_node_attributes(G, 'pos')
    
#     # Plot the network
#     plt.figure(figsize=(10, 10))
#     nx.draw(G, pos, node_size=10, with_labels=False, edge_color='gray', alpha=0.5, width=0.5)
#     plt.title("Bike Network")
#     plt.show(block=True)


# def plot_shortest_path_with_results(nodes, edges, shortest_path_df, origin, destination):
#     """Plot the shortest path between two nodes with additional path information."""
#     print("Plotting the shortest path...")
#     path_data = shortest_path_df[(shortest_path_df.origin == origin) & (shortest_path_df.destination == destination)]
#     if path_data.empty:
#         print(f"No path found between {origin} and {destination}")
#         return

#     # Create a graph from the edges
#     G = nx.Graph()
#     G.add_nodes_from([(node['id'], {'pos': (node['x'], node['y'])}) for _, node in nodes.iterrows()])
#     G.add_edges_from([(edge.fromNode, edge.toNode, {'weight': edge.distance}) for _, edge in edges.iterrows()])

#     # Extract positions for plotting
#     pos = nx.get_node_attributes(G, 'pos')

#     # Plot the network
#     plt.figure(figsize=(10, 10))
#     nx.draw(G, pos, node_size=10, with_labels=False, edge_color='gray', alpha=0.5, width=0.5)

#     # Highlight the shortest path
#     path_edges = [(path_data.fromNode.iloc[i], path_data.toNode.iloc[i]) for i in range(len(path_data))]
#     nx.draw_networkx_edges(G, pos, edgelist=path_edges, edge_color='blue', width=2, label="Shortest Path")

#     # Highlight the origin and destination nodes
#     nx.draw_networkx_nodes(G, pos, nodelist=[origin], node_color='green', node_size=100, label="Origin")
#     nx.draw_networkx_nodes(G, pos, nodelist=[destination], node_color='purple', node_size=100, label="Destination")

#     # Calculate path information
#     num_edges = len(path_edges)
#     total_distance = path_data['distance'].sum()

#     # Add path information to the plot
#     info_text = (f"Origin: {origin}\n"
#                  f"Destination: {destination}\n"
#                  f"Number of Edges: {num_edges}\n"
#                  f"Total Distance: {total_distance:.2f} miles")
#     plt.text(0.05, 0.95, info_text, transform=plt.gca().transAxes, fontsize=12,
#              verticalalignment='top', bbox=dict(boxstyle="round,pad=0.3", edgecolor='black', facecolor='white'))

#     # Add a legend
#     plt.legend(loc="upper right")
#     plt.title(f"Shortest Path from Node {origin} to Node {destination}")
#     plt.show(block=True)


def plot_shortest_path_with_results_buffered(nodes, edges, shortest_path_df, origin, destination, iteration, buffer_size=None):
    """Plot the shortest path between two nodes with additional path information within a square buffer around the origin node."""
    print("Plotting the shortest path...")
    path_data = shortest_path_df[(shortest_path_df.origin == origin) & (shortest_path_df.destination == destination) & (shortest_path_df.iteration == iteration)]
    if path_data.empty:
        print(f"No path found between {origin} and {destination} for iteration {iteration}")
        return

    # Get the coordinates of the origin node
    origin_node = nodes[nodes['id'] == origin].iloc[0]
    origin_x, origin_y = origin_node['x'], origin_node['y']

    if buffer_size:
        # Define the buffer boundaries
        min_x, max_x = origin_x - buffer_size, origin_x + buffer_size
        min_y, max_y = origin_y - buffer_size, origin_y + buffer_size

        # Filter nodes within the buffer
        filtered_nodes = nodes[(nodes['x'] >= min_x) & (nodes['x'] <= max_x) & (nodes['y'] >= min_y) & (nodes['y'] <= max_y)]

        # Filter edges to include only those with both nodes within the buffer
        filtered_edges = edges[edges['fromNode'].isin(filtered_nodes['id']) & edges['toNode'].isin(filtered_nodes['id'])]

        # check to make sure destination node is also in the buffer
        if destination not in filtered_nodes['id'].values:
            print(f"Destination node {destination} is not in the buffer size of {buffer_size}")

    else:
        filtered_nodes = nodes
        filtered_edges = edges

    # Create a graph from the filtered nodes and edges
    G = nx.Graph()
    G.add_nodes_from([(node['id'], {'pos': (node['x'], node['y'])}) for _, node in filtered_nodes.iterrows()])
    G.add_edges_from([(edge.fromNode, edge.toNode, {'weight': edge.distance}) for _, edge in filtered_edges.iterrows()])

    # Extract positions for plotting
    pos = nx.get_node_attributes(G, 'pos')

    # Plot the network
    plt.figure(figsize=(10, 10))
    nx.draw(G, pos, node_size=10, with_labels=False, edge_color='gray', alpha=0.5, width=0.5)

    # Highlight the shortest path
    path_edges = [(path_data.fromNode.iloc[i], path_data.toNode.iloc[i]) for i in range(len(path_data))]
    path_nodes = set(path_data.fromNode).union(set(path_data.toNode))
    nx.draw_networkx_nodes(G, pos, nodelist=path_nodes, node_color='red', node_size=50, label="Path Nodes")
    nx.draw_networkx_edges(G, pos, edgelist=path_edges, edge_color='blue', width=2, label="Shortest Path")

    # Highlight the origin and destination nodes
    nx.draw_networkx_nodes(G, pos, nodelist=[origin], node_color='green', node_size=100, label="Origin")
    nx.draw_networkx_nodes(G, pos, nodelist=[destination], node_color='purple', node_size=100, label="Destination")

    # Calculate path information
    num_edges = len(path_edges)
    total_distance = path_data['distance'].sum()
    
    total_cost = path_data['cost_total'].sum()
    turns = path_data[path_data.turnType > 0]['turnType'].count()
    path_size = path_data.iloc[0]['path_size']
    # Add path information to the plot
    info_text = (f"Origin: {origin}\n"
                f"Destination: {destination}\n"
                f"Iteration: {iteration}\n"
                f"Number of Edges: {num_edges}\n"
                f"Total Distance: {total_distance:.2f} units\n"
                f"Turns: {turns}\n"
                f"Total Cost: {total_cost:.2f}\n"
                f"Path Size: {path_size:.2f}")
    
    plt.text(0.05, 0.95, info_text, transform=plt.gca().transAxes, fontsize=12,
             verticalalignment='top', bbox=dict(boxstyle="round,pad=0.3", edgecolor='black', facecolor='white'))

    # Add a legend
    plt.legend(loc="upper right")
    plt.title(f"Shortest Path from Node {origin} to Node {destination} for iteration {iteration}")
    plt.show(block=True)


# @njit
# def reconstruct_path(predecessors, destination):
#     path = []
#     current = destination
#     while current != -9999:  # -9999 indicates no predecessor
#         path.append(current)
#         current = predecessors[current]
#     return path[::-1]  # Reverse the path to start from the origin
    

# # @njit
# def process_paths(shortest_paths, dest_centroids):
#     rows = []
#     for orig, _, o_predecessors in shortest_paths:
#         reachable = (o_predecessors >= 0).nonzero()[0]
#         for dest in reachable[np.isin(reachable,dest_centroids)]:
#             # Reconstruct the path from the predecessors array
#             path = reconstruct_path(o_predecessors, dest)

#             # Add rows for each node in the path
#             for path_node_num, path_node in enumerate(path):
#                 rows.append((orig, dest, path_node_num, path_node)) # FIXME: path_node_num is 0-indexed when using traversals

#     return rows


def process_paths_new(centroids, predecessors):
    print("Processing paths without numba...")

    # Add self-referential column to predecessor table to indicate end of path
    predecessors_null = np.hstack((predecessors,np.full((predecessors.shape[0],1),-1)))
    predecessors_null[predecessors_null == -9999] = -1
    # Get starting indices for OD pairs with path found
    notnull = (predecessors_null >= 0).nonzero()
    notnull = tuple(i.astype(np.int32) for i in notnull) # force int32 to save memory (defaults to int64)
    notnull_dest = np.isin(notnull[1], centroids).nonzero()
    origin_indices, dest_indices = (notnull[0][notnull_dest],notnull[1][notnull_dest])

    # Iterate through predecessors
    node_indices = dest_indices
    paths = [node_indices]
    while(np.any(node_indices >= 0)):
        node_indices = predecessors_null[origin_indices,node_indices]
        paths.append(node_indices)

    stack_paths = np.vstack(paths).T
    stack_paths = stack_paths[:,::-1] # Reverse order to get origin -> destination
    stack_paths_from = stack_paths[:,:-1]
    stack_paths_to = stack_paths[:,1:] # Offset by 1 to get to-node

    # Remove null edges
    od_index, path_num = (stack_paths_from >= 0).nonzero()
    # path_num_actual = path_num - np.argmax(stack_paths_from >= 0, axis=1)[od_index] # 0-index
    paths_from_node = stack_paths_from[od_index, path_num]
    paths_to_node = stack_paths_to[od_index, path_num]

    paths_orig = origin_indices[od_index] # centroids index
    paths_dest = dest_indices[od_index] # mapped node id

    return(paths_orig, paths_dest, paths_from_node, paths_to_node)


# def calculate_final_logsums(centroids, nodes, edges, node_mapping, all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration, num_iterations):
#     print("Calculating logsums...")
    
#     # Mapped node id to centroids index
#     centroids_rev_map = np.zeros(max(centroids)+1,dtype=np.int32)
#     centroids_rev_map[centroids] = range(len(centroids))
#     all_paths_dest_rev = centroids_rev_map[all_paths_dest]

#     # SciPy Sparse arrays only surrport 2d arrays, so flatten OD and link indices
#     paths_od_ravel = np.ravel_multi_index((all_paths_orig,all_paths_dest_rev),(len(centroids),len(centroids)))
#     paths_link_ravel = np.ravel_multi_index((all_paths_from_node,all_paths_to_node),(len(nodes),len(nodes)))

#     # SciPy COO array will add duplicates together upon conversion to CSR array
#     # Insert ones for each path link to count number of paths for each OD/link
#     # Likely not an optimal solution, but np.unique took far longer, should consider other alternatives 
#     ones_arr = np.ones(len(paths_od_ravel),dtype=np.uint8)
#     link_num_paths = coo_array((ones_arr,(paths_od_ravel,paths_link_ravel)),shape=(len(centroids)**2,len(nodes)**2))
#     link_num_paths = csr_array(link_num_paths)
    
#     paths_num_paths = link_num_paths[paths_od_ravel,paths_link_ravel]
    
#     row = edges.fromNode.map(node_mapping).to_numpy()
#     col = edges.toNode.map(node_mapping).to_numpy()

#     # Apply link lengths and costs to path links
#     data = edges.distance.to_numpy()
#     link_lengths = csr_array((data, (row, col)), shape=(len(nodes),len(nodes)))
#     all_paths_length = link_lengths[all_paths_from_node,all_paths_to_node]
#     data = edges.bikeCost.to_numpy()
#     link_costs = csr_array((data, (row, col)), shape=(len(nodes),len(nodes)))
#     all_paths_cost = link_costs[all_paths_from_node,all_paths_to_node]

#     # path size = sum( ( la / Li ) * ( 1 / Man ) ) = sum( la / Man ) / Li
#     all_paths_size_component = all_paths_length / paths_num_paths   # la / Man
#     # Flatten OD and iteration indices to sum cost, length, path size
#     # Should check if COO array is faster than bincount
#     paths_od_iter_ravel = np.ravel_multi_index((all_paths_orig,all_paths_dest_rev,all_paths_iteration),(len(centroids),len(centroids),num_iterations))
#     od_iter_length_total = np.bincount(paths_od_iter_ravel,all_paths_length)    # Li
#     od_iter_path_size = np.bincount(paths_od_iter_ravel,all_paths_size_component) / od_iter_length_total    # sum( la / Man ) / Li
#     od_iter_cost = np.bincount(paths_od_iter_ravel,all_paths_cost)

#     # Unflatten OD and iteration indices, no longer need individual links
#     od_iter_indices = (od_iter_length_total > 0).nonzero()[0]
#     paths_od_iter_orig, paths_od_iter_dest, paths_od_iter_iter = np.unravel_index(od_iter_indices, (len(centroids),len(centroids),num_iterations))
#     paths_od_iter_cost = od_iter_cost[od_iter_indices]
#     paths_od_iter_path_size = od_iter_path_size[od_iter_indices]

#     # Normalize path size, need to sum path size by OD
#     paths_od_ravel = np.ravel_multi_index((paths_od_iter_orig,paths_od_iter_dest),(len(centroids),len(centroids)))
#     od_path_size_sum = np.bincount(paths_od_ravel,paths_od_iter_path_size)
#     paths_od_iter_path_size_sum = od_path_size_sum[paths_od_ravel]
#     paths_od_iter_path_size_normalized = paths_od_iter_path_size / paths_od_iter_path_size_sum

#     # Add path cost to utility function. Log or no log?
#     paths_od_iter_utility = (-1 * paths_od_iter_cost) + np.log(paths_od_iter_path_size_normalized)
#     # paths_od_iter_utility = (-1 * paths_od_iter_cost) + paths_od_iter_path_size_normalized

#     # Unflatten OD indices, no longer need iterations
#     od_indices = (od_path_size_sum > 0).nonzero()[0]
#     paths_od_orig, paths_od_dest = np.unravel_index(od_indices, (len(centroids),len(centroids)))

#     # Logsum calculation
#     od_logsum = np.bincount(paths_od_ravel,np.exp(paths_od_iter_utility))
#     paths_od_logsum = np.log(od_logsum[od_indices])

#     # Centroids index to mapped node id
#     centroids_np = np.array(centroids)
#     paths_od_orig_mapped = centroids_np[paths_od_orig]
#     paths_od_dest_mapped = centroids_np[paths_od_dest]

#     print("Converting to pandas dataframe...")
#     paths_df = pd.DataFrame({
#         'origin': paths_od_orig_mapped,
#         'destination': paths_od_dest_mapped,
#         'logsum': paths_od_logsum,
#     }, copy=False)

#     # Mapped node id to original node id
#     reverse_map = {v: k for k, v in node_mapping.items()}
#     paths_df['origin'] = paths_df['origin'].map(reverse_map)
#     paths_df['destination'] = paths_df['destination'].map(reverse_map)

#     return paths_df


# def calculate_final_logsums_batch(centroids, centroid_batch, nodes, edges, node_mapping, all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration, num_iterations):
#     print("Calculating logsums...")
    
#     # Mapped node id to centroids index
#     centroids_rev_map = np.zeros(max(centroids)+1,dtype=np.int32)
#     centroids_rev_map[centroids] = range(len(centroids))
#     all_paths_dest_rev = centroids_rev_map[all_paths_dest]

#     # SciPy Sparse arrays only surrport 2d arrays, so flatten OD and link indices
#     paths_od_ravel = np.ravel_multi_index((all_paths_orig,all_paths_dest_rev),(len(centroid_batch),len(centroids)))
#     paths_link_ravel = np.ravel_multi_index((all_paths_from_node,all_paths_to_node),(len(nodes),len(nodes)))

#     # SciPy COO array will add duplicates together upon conversion to CSR array
#     # Insert ones for each path link to count number of paths for each OD/link
#     # Likely not an optimal solution, but np.unique took far longer, should consider other alternatives 
#     ones_arr = np.ones(len(paths_od_ravel),dtype=np.uint8)
#     link_num_paths = coo_array((ones_arr,(paths_od_ravel,paths_link_ravel)),shape=(len(centroid_batch)*len(centroids),len(nodes)**2))
#     link_num_paths = csr_array(link_num_paths)
    
#     paths_num_paths = link_num_paths[paths_od_ravel,paths_link_ravel]
    
#     row = edges.fromNode.map(node_mapping).to_numpy()
#     col = edges.toNode.map(node_mapping).to_numpy()

#     # Apply link lengths and costs to path links
#     data = edges.distance.to_numpy()
#     link_lengths = csr_array((data, (row, col)), shape=(len(nodes),len(nodes)))
#     all_paths_length = link_lengths[all_paths_from_node,all_paths_to_node]
#     data = edges.bikeCost.to_numpy()
#     link_costs = csr_array((data, (row, col)), shape=(len(nodes),len(nodes)))
#     all_paths_cost = link_costs[all_paths_from_node,all_paths_to_node]

#     # path size = sum( ( la / Li ) * ( 1 / Man ) ) = sum( la / Man ) / Li
#     all_paths_size_component = all_paths_length / paths_num_paths   # la / Man
#     # Flatten OD and iteration indices to sum cost, length, path size
#     # Should check if COO array is faster than bincount
#     paths_od_iter_ravel = np.ravel_multi_index((all_paths_orig,all_paths_dest_rev,all_paths_iteration),(len(centroid_batch),len(centroids),num_iterations))
#     od_iter_length_total = np.bincount(paths_od_iter_ravel,all_paths_length)    # Li
#     od_iter_path_size = np.bincount(paths_od_iter_ravel,all_paths_size_component) / od_iter_length_total    # sum( la / Man ) / Li
#     od_iter_cost = np.bincount(paths_od_iter_ravel,all_paths_cost)

#     # Unflatten OD and iteration indices, no longer need individual links
#     od_iter_indices = (od_iter_length_total > 0).nonzero()[0]
#     paths_od_iter_orig, paths_od_iter_dest, paths_od_iter_iter = np.unravel_index(od_iter_indices, (len(centroid_batch),len(centroids),num_iterations))
#     paths_od_iter_cost = od_iter_cost[od_iter_indices]
#     paths_od_iter_path_size = od_iter_path_size[od_iter_indices]

#     # Normalize path size, need to sum path size by OD
#     paths_od_ravel = np.ravel_multi_index((paths_od_iter_orig,paths_od_iter_dest),(len(centroid_batch),len(centroids)))
#     od_path_size_sum = np.bincount(paths_od_ravel,paths_od_iter_path_size)
#     paths_od_iter_path_size_sum = od_path_size_sum[paths_od_ravel]
#     paths_od_iter_path_size_normalized = paths_od_iter_path_size / paths_od_iter_path_size_sum

#     # Add path cost to utility function
#     paths_od_iter_utility = (-1 * paths_od_iter_cost) + np.log(paths_od_iter_path_size_normalized)

#     # Unflatten OD indices, no longer need iterations
#     od_indices = (od_path_size_sum > 0).nonzero()[0]
#     paths_od_orig, paths_od_dest = np.unravel_index(od_indices, (len(centroid_batch),len(centroids)))

#     # Logsum calculation
#     od_logsum = np.bincount(paths_od_ravel,np.exp(paths_od_iter_utility))
#     paths_od_logsum = np.log(od_logsum[od_indices])

#     # Centroids index to mapped node id
#     centroid_batch_np = np.array(centroid_batch)
#     paths_od_orig_mapped = centroid_batch_np[paths_od_orig]
#     centroids_np = np.array(centroids)
#     paths_od_dest_mapped = centroids_np[paths_od_dest]

#     return paths_od_orig_mapped, paths_od_dest_mapped, paths_od_logsum


def calculate_final_logsums_batch_traversals(nodes, edges, traversals, origin_centroids, dest_centroids, all_paths_orig, all_paths_dest, all_paths_from_edge, all_paths_to_edge, all_paths_iteration, num_iterations, trace_origins = [], trace_dests = []):
    print("Calculating logsums...")
    
    # Mapped node id to centroids index
    dest_centroids_rev_map = np.zeros(max(dest_centroids)+1,dtype=np.int32)
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

    edge_mapping = edges[['fromNode','toNode']].reset_index()

    traversals_mapped = traversals.merge(
        edge_mapping,
        how='left',
        left_on=['start','thru'],
        right_on=['fromNode','toNode']
    ).merge(
        edge_mapping,
        how='left',
        left_on=['thru','end'],
        right_on=['fromNode','toNode'],
        suffixes=('FromEdge','ToEdge')
    )

    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.bikecost.to_numpy()
    trav_costs = csr_array((data, (row, col)), shape=(num_edges, num_edges))

    all_paths_trav_cost = trav_costs[all_paths_from_edge,all_paths_to_edge]

    # Add origin connectors
    orig_connectors_indices = np.isin(all_paths_from_edge, origin_centroids).nonzero()[0]
    all_paths_from_node = np.concatenate((all_paths_from_node, edge_from_node[all_paths_from_edge][orig_connectors_indices]))
    all_paths_to_node = np.concatenate((all_paths_to_node, edge_to_node[all_paths_from_edge][orig_connectors_indices]))
    all_paths_edge_cost = np.concatenate((all_paths_edge_cost, edge_cost[all_paths_from_edge][orig_connectors_indices]))
    all_paths_edge_length = np.concatenate((all_paths_edge_length, edge_length[all_paths_from_edge][orig_connectors_indices]))
    all_paths_trav_cost = np.concatenate((all_paths_trav_cost, np.zeros_like(orig_connectors_indices)))
    all_paths_orig_new = np.concatenate((all_paths_orig, all_paths_orig[orig_connectors_indices]))
    all_paths_dest_rev_new = np.concatenate((all_paths_dest_rev, all_paths_dest_rev[orig_connectors_indices]))
    all_paths_iteration_new = np.concatenate((all_paths_iteration, all_paths_iteration[orig_connectors_indices]))
    if trace_origins:
        all_paths_prev_node = np.concatenate((all_paths_prev_node, np.full_like(orig_connectors_indices,-1)))

    # all_paths_from_node = all_paths_thru_node 
    # all_paths_to_node = all_paths_end_node
    all_paths_cost = all_paths_edge_cost + all_paths_trav_cost

    # SciPy Sparse arrays only surrport 2d arrays, so flatten OD and link indices
    paths_od_ravel = np.ravel_multi_index((all_paths_orig_new,all_paths_dest_rev_new),(len(origin_centroids),len(dest_centroids)))
    paths_link_ravel = np.ravel_multi_index((all_paths_from_node,all_paths_to_node),(len(nodes),len(nodes)))

    if trace_origins:
        # trace_indices = (np.isin(all_paths_orig_new, trace_origins) & np.isin(all_paths_dest_rev_new, trace_dests)).nonzero()[0]
        trace_od_ravel = np.ravel_multi_index((trace_origins,trace_dests),(len(origin_centroids),len(dest_centroids)))
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
    ones_arr = np.ones(len(paths_od_ravel),dtype=np.uint8)
    link_num_paths = coo_array((ones_arr,(paths_od_ravel,paths_link_ravel)),shape=(len(origin_centroids)*len(dest_centroids),len(nodes)**2))
    link_num_paths = csr_array(link_num_paths)
    
    paths_num_paths = link_num_paths[paths_od_ravel,paths_link_ravel]

    # path size = sum( ( la / Li ) * ( 1 / Man ) ) = sum( la / Man ) / Li
    all_paths_size_component = all_paths_edge_length / paths_num_paths   # la / Man
    # Flatten OD and iteration indices to sum cost, length, path size
    # Should check if COO array is faster than bincount
    paths_od_iter_ravel = np.ravel_multi_index((all_paths_orig_new,all_paths_dest_rev_new,all_paths_iteration_new),(len(origin_centroids),len(dest_centroids),num_iterations))
    od_iter_length_total = np.bincount(paths_od_iter_ravel,all_paths_edge_length)    # Li
    od_iter_path_size = np.bincount(paths_od_iter_ravel,all_paths_size_component) / od_iter_length_total    # sum( la / Man ) / Li
    od_iter_cost = np.bincount(paths_od_iter_ravel,all_paths_cost)

    if trace_origins:
        trace_paths_od_iter_ravel = np.ravel_multi_index((trace_paths_orig,trace_paths_dest_rev,trace_paths_iteration),(len(origin_centroids),len(dest_centroids),num_iterations))
        trace_paths_path_size = od_iter_path_size[trace_paths_od_iter_ravel]


    # Unflatten OD and iteration indices, no longer need individual links
    od_iter_indices = (od_iter_length_total > 0).nonzero()[0]
    paths_od_iter_orig, paths_od_iter_dest, paths_od_iter_iter = np.unravel_index(od_iter_indices, (len(origin_centroids),len(dest_centroids),num_iterations))
    paths_od_iter_cost = od_iter_cost[od_iter_indices]
    paths_od_iter_path_size = od_iter_path_size[od_iter_indices]

    # Normalize path size, need to sum path size by OD
    paths_od_ravel = np.ravel_multi_index((paths_od_iter_orig,paths_od_iter_dest),(len(origin_centroids),len(dest_centroids)))
    od_path_size_sum = np.bincount(paths_od_ravel,paths_od_iter_path_size)
    paths_od_iter_path_size_sum = od_path_size_sum[paths_od_ravel]
    paths_od_iter_path_size_normalized = paths_od_iter_path_size / paths_od_iter_path_size_sum

    # Add path cost to utility function
    paths_od_iter_utility = (-1 * paths_od_iter_cost) + np.log(paths_od_iter_path_size_normalized)
    # paths_od_iter_utility = (-1 * paths_od_iter_cost) + (paths_od_iter_path_size_normalized)

    # Unflatten OD indices, no longer need iterations
    od_indices = (od_path_size_sum > 0).nonzero()[0]
    paths_od_orig, paths_od_dest = np.unravel_index(od_indices, (len(origin_centroids),len(dest_centroids)))

    # Logsum calculation
    od_logsum = np.bincount(paths_od_ravel,np.exp(paths_od_iter_utility))
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
        return paths_od_orig_mapped, paths_od_dest_mapped, paths_od_logsum, trace_paths_orig_mapped, trace_paths_dest_mapped, trace_paths_iteration, trace_paths_prev_node, trace_paths_from_node, trace_paths_to_node, trace_paths_path_size
    else:
        return paths_od_orig_mapped, paths_od_dest_mapped, paths_od_logsum, np.empty((0)), np.empty((0)), np.empty((0)), np.empty((0)), np.empty((0)), np.empty((0)), np.empty((0))


# def calculate_final_logsums_spark(centroids, nodes, edges, node_mapping, all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration, num_iterations):



# def convert_shortest_paths_to_long_df_numba(shortest_paths, edges, node_mapping):
#     """
#     Convert the output of perform_dijkstra_scipy into a pandas DataFrame in long format.

#     Args:
#         shortest_paths (dict): A dictionary with centroids as keys and tuples of shortest path lengths
#                                and predecessors as values.

#     Returns:
#         pd.DataFrame: A DataFrame with shortest path information where each row is an edge in the path
#     """
#     print("Converting shortest paths to pandas dataframe...")

#     print("Processing paths with numba...")
#     centroids = list(zip(*shortest_paths))[0]
#     rows = process_paths(shortest_paths, centroids)


#     print("Creating dataframe...")
#     rows = np.array(rows)
#     sp_df = pd.DataFrame(rows, columns=['origin', 'destination', 'path_node_num', 'toNode'])
#     # convert the 0 index node id back to the actual node id
#     reverse_map = {v: k for k, v in node_mapping.items()}
#     sp_df['origin'] = sp_df['origin'].map(reverse_map)
#     sp_df['destination'] = sp_df['destination'].map(reverse_map)
#     sp_df['toNode'] = sp_df['toNode'].map(reverse_map)

#     sp_df['path_id'] = sp_df.groupby(['origin', 'destination']).ngroup()
#     sp_df['fromNode'] = sp_df.groupby('path_id')['toNode'].shift(1)
#     sp_df = sp_df[sp_df['toNode'] != sp_df['origin']]  # Remove the dummy start rows
#     sp_df['fromNode'] = sp_df['fromNode'].astype(int)

#     # merging edge attributes onto sp_df
#     sp_df = sp_df.merge(
#         edges[['fromNode', 'toNode', 'distance', 'bikeCost']], 
#         how='left',
#         on=['fromNode', 'toNode'],
#     )

#     return sp_df

# def convert_shortest_paths_to_long_df_numba_traversals(shortest_paths, nodes, edges, traversals, edge_mapping, zone_level='mgra'):
#     """
#     Convert the output of perform_dijkstra_scipy into a pandas DataFrame in long format.

#     Args:
#         shortest_paths (dict): A dictionary with centroids as keys and tuples of shortest path lengths
#                                and predecessors as values.

#     Returns:
#         pd.DataFrame: A DataFrame with shortest path information where each row is an edge in the path
#     """
#     print("Converting shortest paths to pandas dataframe...")

#     print("Processing paths with numba...")
#     # origin_centroids = list(shortest_paths.keys())
#     dest_centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].merge(
#         edge_mapping,
#         how='left',
#         left_on='id',
#         right_on='toNode'
#     )['index'].tolist()
#     rows = process_paths(shortest_paths, dest_centroids)


#     print("Creating dataframe...")
#     rows = np.array(rows)
#     sp_df = pd.DataFrame(rows, columns=['origin', 'destination', 'path_node_num', 'toEdge'])

#     # convert the 0 index node id back to the actual node id

#     sp_df = sp_df.merge(edge_mapping[['fromNode','index']],how='left',left_on='origin',right_on='index')
#     sp_df['origin'] = sp_df['fromNode']
#     sp_df = sp_df.drop(columns=['index', 'fromNode'])

#     sp_df = sp_df.merge(edge_mapping[['toNode','index']],how='left',left_on='destination',right_on='index')
#     sp_df['destination'] = sp_df['toNode']
#     sp_df = sp_df.drop(columns=['index', 'toNode'])

#     sp_df = sp_df.merge(edge_mapping[['fromNode','toNode','index']],how='left',left_on='toEdge',right_on='index')
#     sp_df = sp_df.drop(columns=['index', 'toEdge'])

#     sp_df['path_id'] = sp_df.groupby(['origin', 'destination']).ngroup()
#     sp_df['prevNode'] = sp_df.groupby('path_id')['fromNode'].shift(1)

#     # reverse_map = {v: k for k, v in node_mapping.items()}
#     # sp_df['origin'] = sp_df['origin'].map(reverse_map)
#     # sp_df['destination'] = sp_df['destination'].map(reverse_map)
#     # sp_df['toNode'] = sp_df['toNode'].map(reverse_map)

#     # sp_df['path_id'] = sp_df.groupby(['origin', 'destination']).ngroup()
#     # sp_df['fromNode'] = sp_df.groupby('path_id')['toNode'].shift(1)
#     # sp_df = sp_df[sp_df['toNode'] != sp_df['origin']]  # Remove the dummy start rows
#     sp_df['prevNode'] = sp_df['prevNode'].fillna(-1)
#     # sp_df[sp_df['fromNode'] == sp_df['origin']]['prevNode'] = -1
#     sp_df['prevNode'] = sp_df['prevNode'].astype(int)

#     # merging edge attributes onto sp_df
#     sp_df = sp_df.merge(
#         edges[['fromNode', 'toNode', 'distance', 'bikeCost']], 
#         how='left',
#         on=['fromNode', 'toNode'],
#     ).rename(columns={'bikeCost': 'bikeCostEdge'})

#     # merging traversal attributes onto sp_df
#     sp_df = sp_df.merge(
#         traversals[['start', 'thru', 'end', 'turnType', 'bikecost']],
#         how='left',
#         left_on=['prevNode','fromNode','toNode'],
#         right_on=['start','thru','end']
#     ).rename(columns={'bikecost': 'bikeCostTraversal'}).drop(columns=['start','thru','end'])

#     sp_df['bikeCostTraversal'] = sp_df['bikeCostTraversal'].fillna(0)
#     sp_df['turnType'] = sp_df['turnType'].fillna(0)
#     sp_df['bikeCostTotal'] = sp_df['bikeCostTraversal'] + sp_df['bikeCostEdge']

#     return sp_df


def _perform_dijkstra(centroids, adjacency_matrix, limit=3):
    """Perform Dijkstra's algorithm for a batch of centroids."""
    print(f"Processing Dijkstra's on {len(centroids)} centroids with limit={limit}...")
    distances, predecessors = dijkstra(
        adjacency_matrix, directed=True, indices=centroids, return_predecessors=True, limit=limit
    )
    # shortest_paths = {}
    # for centroid, distance_mat, predecessor_mat in zip(centroids, distances, predecessors):
    #     shortest_paths[centroid] = (distance_mat, predecessor_mat)

    return (distances, predecessors)


# def perform_dijkstras_algorithm(nodes, edges, limit=3, zone_level='mgra', num_processors=4):
#     """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
#     num_nodes = len(nodes)

#     # node mapping needs to start at 0 in order to create adjacency matrix
#     node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.id)}

#     # Create a sparse adjacency matrix
#     row = edges.fromNode.map(node_mapping).to_numpy()
#     col = edges.toNode.map(node_mapping).to_numpy()
#     data = edges.bikeCost.to_numpy()
#     adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_nodes, num_nodes))

#     # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
#     centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].id.map(node_mapping).tolist()

#     print(f"Need to calculate Dijkstra's on {len(centroids)} centroids with {num_processors} processors")

#     if num_processors > 1:
#         # Split centroids into batches
#         centroid_batches = np.array_split(centroids, num_processors)

#         shortest_paths = {}
#         with Pool(processes=num_processors) as pool:
#             results = pool.map(_perform_dijkstra, [(batch, adjacency_matrix, limit) for batch in centroid_batches])
#             for distances, predecessors in results:
#                 shortest_paths.update((centroids, distances, predecessors, node_mapping))
#             return shortest_paths

#     else:
#         # Perform Dijkstra's algorithm for all centroids
#         distances, predecessors = _perform_dijkstra(centroids, adjacency_matrix, limit)
#         return (centroids, distances, predecessors, node_mapping)

# def perform_dijkstras_algorithm_batched(nodes, edges, node_mapping, centroids, centroid_batches, limit=3):
#     """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
#     num_nodes = len(nodes)

#     # # node mapping needs to start at 0 in order to create adjacency matrix
#     # node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.id)}

#     # Create a sparse adjacency matrix
#     row = edges.fromNode.map(node_mapping).to_numpy()
#     col = edges.toNode.map(node_mapping).to_numpy()
#     data = edges.bikeCost.to_numpy()
#     adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_nodes, num_nodes))

#     # # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
#     # centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].id.map(node_mapping).tolist()

#     print(f"Need to calculate Dijkstra's on {len(centroids)} centroids with {len(centroid_batches)} processors")

#     if len(centroid_batches) > 1:
#         # # Split centroids into batches
#         # centroid_batches = np.array_split(centroids, num_processors)

#         shortest_paths = {}
#         with Pool(processes=len(centroid_batches)) as pool:
#             results = pool.map(_perform_dijkstra, [(batch, adjacency_matrix, limit) for batch in centroid_batches])
#             # for distances, predecessors in results:
#             #     shortest_paths.update((distances, predecessors))
#             for batch_result in results:
#                 shortest_paths.update(batch_result)
#             return shortest_paths

#     else:
#         # Perform Dijkstra's algorithm for all centroids
#         shortest_paths = _perform_dijkstra(centroids, adjacency_matrix, limit)
#         return shortest_paths
    
# def perform_dijkstras_algorithm_batch(nodes, edges, node_mapping, centroid_batch, limit=3):
#     """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
#     num_nodes = len(nodes)

#     # # node mapping needs to start at 0 in order to create adjacency matrix
#     # node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.id)}

#     # Create a sparse adjacency matrix
#     row = edges.fromNode.map(node_mapping).to_numpy()
#     col = edges.toNode.map(node_mapping).to_numpy()
#     data = edges.bikeCost.to_numpy()
#     adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_nodes, num_nodes))

#     # # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
#     # centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].id.map(node_mapping).tolist()

#     print(f"Need to calculate Dijkstra's on {len(centroid_batch)} centroids")

#     # Perform Dijkstra's algorithm for all centroids
#     shortest_paths = _perform_dijkstra(centroid_batch, adjacency_matrix, limit)
#     return shortest_paths

def perform_dijkstras_algorithm_batch_traversals(nodes, edges, traversals, origin_centroids, limit=3):
    """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
    num_edges = len(edges)

    # # node mapping needs to start at 0 in order to create adjacency matrix
    edge_mapping = edges[['fromNode','toNode','edgeCost']].reset_index()

    traversals_mapped = traversals.merge(
        edge_mapping,
        how='left',
        left_on=['start','thru'],
        right_on=['fromNode','toNode']
    ).merge(
        edge_mapping,
        how='left',
        left_on=['thru','end'],
        right_on=['fromNode','toNode'],
        suffixes=('FromEdge','ToEdge')
    )
    # Total bike cost is edge cost (after traversal) plus traversal cost
    # Note origin zone connector edge cost is not included with this approach, but this has no impact on shortest path selection
    traversals_mapped['bikeCostTotal'] = traversals_mapped.edgeCostToEdge + traversals_mapped.bikecost
    traversals_mapped.loc[traversals_mapped.indexFromEdge.isin(origin_centroids), 'bikeCostTotal'] += traversals_mapped.edgeCostFromEdge

    # Create a sparse adjacency matrix
    row = traversals_mapped.indexFromEdge.to_numpy()
    col = traversals_mapped.indexToEdge.to_numpy()
    data = traversals_mapped.bikeCostTotal.to_numpy()
    adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_edges, num_edges))

    print(f"Need to calculate Dijkstra's on {len(origin_centroids)} centroids")

    # Perform Dijkstra's algorithm for all centroids
    shortest_paths = _perform_dijkstra(origin_centroids, adjacency_matrix, limit)
    return shortest_paths

# def perform_dijkstras_algorithm_traversals(nodes, edges, traversals, limit=3, zone_level='mgra', num_processors=4):
#     """Perform Dijkstra's algorithm for centroids using SciPy's sparse graph solver with batched parallel processing."""
#     num_edges = len(edges)

#     # # node mapping needs to start at 0 in order to create adjacency matrix
#     edge_mapping = edges[['fromNode','toNode','bikeCost']].reset_index()

#     # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
#     # Centroid connectors
#     origin_centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].merge(
#         edge_mapping,
#         how='left',
#         left_on='id',
#         right_on='fromNode'
#     )

#     null_cols = origin_centroids[origin_centroids.isnull().any(axis=1)]
#     if not null_cols.empty:
#         print("WARNING: Null columns found in centroids dataframe! Dropping")
#         print(null_cols)
#         origin_centroids = origin_centroids.dropna()

#     origin_centroids = origin_centroids['index'].tolist()

#     traversals_mapped = traversals.merge(
#         edge_mapping,
#         how='left',
#         left_on=['start','thru'],
#         right_on=['fromNode','toNode']
#     ).merge(
#         edge_mapping,
#         how='left',
#         left_on=['thru','end'],
#         right_on=['fromNode','toNode'],
#         suffixes=('FromEdge','ToEdge')
#     )
#     # Total bike cost is edge cost (after traversal) plus traversal cost
#     # Note origin zone connector edge cost is not included with this approach, but this has no impact on shortest path selection
#     traversals_mapped['bikeCostTotal'] = traversals_mapped.bikeCostToEdge + traversals_mapped.bikecost
#     traversals_mapped.loc[traversals_mapped.indexFromEdge.isin(origin_centroids), 'bikeCostTotal'] += traversals_mapped.bikeCostFromEdge

#     # Create a sparse adjacency matrix
#     row = traversals_mapped.indexFromEdge.to_numpy()
#     col = traversals_mapped.indexToEdge.to_numpy()
#     data = traversals_mapped.bikeCostTotal.to_numpy()
#     adjacency_matrix = csr_matrix((data, (row, col)), shape=(num_edges, num_edges))

#     print(f"Need to calculate Dijkstra's on {len(origin_centroids)} centroids with {num_processors} processors")

#     if num_processors > 1:
#         # Split centroids into batches
#         centroid_batches = np.array_split(origin_centroids, num_processors)

#         shortest_paths = {}
#         with Pool(processes=num_processors) as pool:
#             results = pool.map(_perform_dijkstra, [(batch, adjacency_matrix, limit) for batch in centroid_batches])
#             for batch_result in results:
#                 shortest_paths.update(batch_result)

#     else:
#         # Perform Dijkstra's algorithm for all centroids
#         shortest_paths = _perform_dijkstra(origin_centroids, adjacency_matrix, limit)

#     return shortest_paths, edge_mapping


# def summarize_shortest_paths(sp_df):
#     summary = sp_df.groupby(['origin', 'destination']).agg(
#         {'distance': 'sum', 'bikeCost': 'sum', 'path_node_num': 'max'}).reset_index().rename(
#         columns={'path_node_num': 'num_edges_per_path'}).describe()
#     print(summary)
#     return

# def summarize_shortest_paths_traversals(sp_df):
#     summary = sp_df.groupby(['origin', 'destination']).agg(
#         {'distance': 'sum', 
#          'turnType': lambda x: (x.ne(0)).sum(),
#          'bikeCostTotal': 'sum', 
#          'bikeCostEdge': 'sum',
#          'bikeCostTraversal': 'sum',
#          'path_node_num': 'max'}).reset_index().rename(
#         columns={'path_node_num': 'num_edges_per_path','turnType': 'turns'}).describe()
#     print(summary)
#     return


# def run_test_example():
#     nodes, edges = create_dummy_network()

#     # Perform Dijkstra's algorithm
#     shortest_paths, node_mapping = perform_dijkstras_algorithm(nodes, edges, limit=MAX_DISTANCE, num_processors=NUM_PRCESSORS)

#     sp_df = convert_shortest_paths_to_long_df_numba(shortest_paths, edges, node_mapping)

#     summarize_shortest_paths(sp_df)

#     # Plot shortest path between origin and destination
#     origin = sp_df.loc[0, 'origin']  # Use the first centroid
#     destination = sp_df.loc[0, 'destination']  # Use the second centroid
#     plot_shortest_path_with_results(nodes, edges, sp_df, origin, destination)


# def run_actual_data():
#     nodes, edges, traversals = read_bike_network_data(num_centroids=5000)

#     # Perform Dijkstra's algorithm
#     shortest_paths, node_mapping = perform_dijkstras_algorithm(nodes, edges, limit=MAX_DISTANCE, num_processors=NUM_PRCESSORS)

#     sp_df = convert_shortest_paths_to_long_df_numba(shortest_paths, edges, node_mapping)

#     summarize_shortest_paths(sp_df)

#     # Plot shortest path between origin and destination
#     # origin = sp_df.loc[0, 'origin']  # Use the first centroid
#     # destination = sp_df.loc[0, 'destination']  # Use the second centroid
#     # plot_shortest_path_with_results(nodes, edges, sp_df, origin, destination)


# def run_iterations(nodes, edges, traversals, zone_level, cost_limit, random_scale, num_iterations):
#     all_paths = []

#     for i in range(num_iterations):
#         # randomize costs
#         edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)

#         # run dijkstra's
#         centroids, distances, predecessors, node_mapping = perform_dijkstras_algorithm(nodes, edges_rand, limit=cost_limit, zone_level=zone_level, num_processors=NUM_PRCESSORS)

#         # process paths
#         paths = process_paths_new(centroids, predecessors)
#         all_paths.append(paths + (np.full_like(paths[0],i,dtype=np.uint8),))

#     all_paths_concat = map(np.concatenate,zip(*all_paths))
#     all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration = all_paths_concat
#     return all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration, centroids, node_mapping

# def run_iterations_batched(nodes, edges, traversals, zone_level, cost_limit, random_scale, num_iterations, num_batches):

#     # node mapping needs to start at 0 in order to create adjacency matrix
#     node_mapping = {node_id: idx for idx, node_id in enumerate(nodes.id)}

#     # Get mgra centroids (nodes with 'centroid' flag True and 'mgra' greater than zero)
#     centroids = nodes[nodes['centroid'] & (nodes[zone_level] > 0)].id.map(node_mapping).tolist()

#      # Split centroids into batches
#     centroid_batches = np.array_split(centroids, num_batches)

#     all_paths = [[]] * num_batches

#     for i in range(num_iterations):
#         # randomize costs
#         edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)

#         # run dijkstra's
#         # centroids, distances, predecessors, node_mapping = perform_dijkstras_algorithm(nodes, edges_rand, limit=cost_limit, zone_level=zone_level, num_processors=NUM_PRCESSORS)
#         # shortest_paths = perform_dijkstras_algorithm(nodes, edges_rand, limit=cost_limit, zone_level=zone_level, num_processors=num_batches)
        
#         shortest_paths = perform_dijkstras_algorithm_batched(nodes, edges_rand, node_mapping, centroids, centroid_batches, limit=cost_limit)

#         # process paths
#         for i in range(num_batches):
#             distances, predecessors = shortest_paths[i]
#             paths = process_paths_new(centroids, predecessors)
#             all_paths[i].append(paths + (np.full_like(paths[0],i,dtype=np.uint8),))

#     all_paths_concat = [None] * num_batches
#     for i in range(num_batches):
#         all_paths_concat[i] = map(np.concatenate,zip(*all_paths[i]))
#         # all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration = all_paths_concat
#     return all_paths_concat, centroids, node_mapping, centroid_batches

# def run_iterations_batch(nodes, edges, traversals, node_mapping, centroids, centroid_batch, cost_limit, random_scale, num_iterations):

#     all_paths = []

#     for i in range(num_iterations):
#         # randomize costs
#         edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)

#         # run dijkstra's
#         distances, predecessors = perform_dijkstras_algorithm_batch(nodes, edges_rand, node_mapping, centroid_batch, limit=cost_limit)

#         # process paths
#         paths = process_paths_new(centroids, predecessors)
#         all_paths.append(paths + (np.full_like(paths[0],i,dtype=np.uint8),))

#     all_paths_concat = map(np.concatenate,zip(*all_paths))
#     all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration = all_paths_concat
#     return all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration


def run_iterations_batch_traversals(nodes, edges, traversals, origin_centroids, dest_centroids, cost_limit, num_iterations, coef_dict, random_scale_coef, random_scale_link):

    all_paths = []

    for i in range(num_iterations):
        # randomize costs
        # edges_rand, traversals_rand = randomize_network_cost(edges, traversals, random_scale)
        edges_rand = edges.copy()
        edges_rand['edgeCost'] = get_edge_cost(edges, coef_dict, random_scale_coef, random_scale_link)

        # run dijkstra's
        distances, predecessors = perform_dijkstras_algorithm_batch_traversals(nodes, edges_rand, traversals, origin_centroids, limit=cost_limit)

        # process paths
        paths = process_paths_new(dest_centroids, predecessors)
        all_paths.append(paths + (np.full_like(paths[0],i,dtype=np.uint8),))

    all_paths_concat = map(np.concatenate,zip(*all_paths))
    all_paths_orig, all_paths_dest, all_paths_from_edge, all_paths_to_edge, all_paths_iteration = all_paths_concat
    return all_paths_orig, all_paths_dest, all_paths_from_edge, all_paths_to_edge, all_paths_iteration


# def run_batch(nodes, edges, traversals, node_mapping, centroids, centroid_batch, cost_limit, random_scale, num_iterations):
#     all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration = run_iterations_batch(nodes, edges, traversals, node_mapping, centroids, centroid_batch, cost_limit, random_scale, num_iterations)
#     final_paths =  calculate_final_logsums_batch(centroids, centroid_batch, nodes, edges, node_mapping, all_paths_orig, all_paths_dest, all_paths_from_node, all_paths_to_node, all_paths_iteration, num_iterations)
#     return final_paths


def run_batch_traversals(nodes, edges, traversals, origin_centroids, dest_centroids, cost_limit, num_iterations, coef_dict, random_scale_coef, random_scale_link, trace_origins = [], trace_dests = []):
    all_paths_orig, all_paths_dest, all_paths_from_edge, all_paths_to_edge, all_paths_iteration = run_iterations_batch_traversals(nodes, edges, traversals, origin_centroids, dest_centroids, cost_limit, num_iterations, coef_dict, random_scale_coef, random_scale_link)
    trace_origins_rev = []
    trace_dests_rev = []
    if trace_origins:
        trace_origins_np = np.array(trace_origins)
        origin_centroids_rev_map = np.zeros(max(origin_centroids)+1,dtype=np.int32)
        origin_centroids_rev_map[origin_centroids] = range(len(origin_centroids))
        trace_origins_rev = origin_centroids_rev_map[trace_origins_np[np.isin(trace_origins,origin_centroids)]]

        trace_dests_np = np.array(trace_dests)
        dest_centroids_rev_map = np.zeros(max(dest_centroids)+1,dtype=np.int32)
        dest_centroids_rev_map[dest_centroids] = range(len(dest_centroids))
        trace_dests_rev = dest_centroids_rev_map[trace_dests_np[np.isin(trace_origins,origin_centroids)]]
    final_paths =  calculate_final_logsums_batch_traversals(nodes, edges, traversals, origin_centroids, dest_centroids, all_paths_orig, all_paths_dest, all_paths_from_edge, all_paths_to_edge, all_paths_iteration, num_iterations, trace_origins_rev, trace_dests_rev)
    return final_paths

# def measure_performance(num_centroids_list):
#     times = []

#     for num_centroids in num_centroids_list:
#         start_time = time.time()
        
#         # Run the actual data processing with the specified number of centroids
#         nodes, edges, traversals = read_bike_network_data(num_centroids=num_centroids)
#         shortest_paths, node_mapping = perform_dijkstras_algorithm(nodes, edges, limit=MAX_DISTANCE, num_processors=NUM_PRCESSORS)
#         sp_df = convert_shortest_paths_to_long_df_numba(shortest_paths, edges, node_mapping)
#         summarize_shortest_paths(sp_df)
        
#         end_time = time.time()
#         elapsed_time = end_time - start_time
#         times.append(elapsed_time)
#         print(f"Number of centroids: {num_centroids}, Time taken: {elapsed_time:.2f} seconds")

#     # Plot the results
#     plt.figure(figsize=(10, 6))
#     plt.plot(num_centroids_list, times, marker='o')
#     plt.xlabel('Number of Centroids')
#     plt.ylabel('Time (seconds)')
#     plt.title('Computation Time as a Function of Number of Centroids')
#     plt.grid(True)
#     plt.show(block=True)


# if __name__ == "__main__":

#     start_time = time.time()

#     # run_test_example()
#     # run_actual_data()
#     num_centroids_list = [100, 500, 1000, 2000, 4000, 5000]
#     measure_performance(num_centroids_list)

#     end_time = time.time()
#     elapsed_time = end_time - start_time
#     print()