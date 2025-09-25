import os
import logging
import yaml
from pydantic import BaseModel, ValidationError
import geopandas as gpd
import pandas as pd
import numpy as np
import networkx as nx
import matplotlib.pyplot as plt
from typing import Literal


class BikeRouteChoiceSettings(BaseModel):
    """
    Bike route choice settings
    """

    # path to bike network shapefiles
    node_file: str = "SANDAG_Bike_Node.shp"
    link_file: str = "SANDAG_Bike_Net.shp"

    # data directory, optional additional place to look for data
    data_dir: str = ""

    # edge utility specifcation file
    edge_util_file: str = "bike_edge_utils.csv"

    # traversal utility specifcation file
    traversal_util_file: str = "bike_traversal_utils.csv"

    # coefficient to multiply intrazonal distance by to get logsum
    intrazonal_coefficient: str = "util_distance"

    bike_speed: float = 0

    # path to bike route choice model output
    output_path: str = "output"
    output_file_path: str = "output\bikeTazLogsum.csv"

    # whether to trace edge and traversal utility calculations
    trace_bike_utilities: bool = False

    # runtime settings
    number_of_batches: int = 1
    number_of_processors: int = 1

    # randomization settings
    random_seed: int = 42
    random_scale_coef: float = 0.5
    random_scale_link: float = 0.7

    # trace origin and destination pairs
    trace_origins: list = []
    trace_destinations: list = []

    # whether to recreate Java attributes -- not needed, but here for backwards compatibility tests
    recreate_java_attributes: bool = False

    # maximum distance in miles for Dijkstra's algorithm to search for shortest paths
    max_dijkstra_utility: float = 10

    # caching options for bike network creation to save time
    read_cached_bike_net: bool = False  # will crash if network does not exist
    save_bike_net: bool = True

    # shapefile generation properties
    generate_shapefile: bool = False
    crs: str = None

    # can define a subset of zones to use for the model
    # this is useful for testing or if you only want to run the model for a specific area
    zone_subset: int | list | None = None

    # whether to treat mazs or tazs as the centroid zones
    zone_level: Literal["taz", "mgra"] = "taz"

    # how many different paths to build for each origin-destination pair
    # this is the number of times dijkstra's algorithm will be run
    number_of_iterations: int = 10

    # minimum number of iterations/paths. Zone pairs with fewer paths will be discarded
    min_iterations: int = 0


def load_settings(
    logger: logging.Logger, yaml_path: str = "bike_route_choice_settings.yaml", 
) -> BikeRouteChoiceSettings:
    with open(yaml_path, "r") as f:
        data = yaml.safe_load(f)
    try:
        settings = BikeRouteChoiceSettings(**data)
    except ValidationError as e:
        print("Settings validation error:", e)
        raise

    # ensure output path exists
    if not os.path.exists(os.path.expanduser(settings.output_path)):
        os.makedirs(os.path.expanduser(settings.output_path))
        logger.info(f"Created output directory: {settings.output_path}")

    # setup logger
    log_file_location = os.path.join(os.path.expanduser(settings.output_path), "bike_model.log")
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
        handlers=[logging.FileHandler(log_file_location), logging.StreamHandler()],
    )

    # set random seed for reproducibility
    np.random.seed(settings.random_seed)

    return settings


def read_file(settings, file_path: str) -> gpd.GeoDataFrame:
    """
    Read a shapefile and return a GeoDataFrame

    Looks for the shapefile in a few places:
    1. The current working directory
    2. The directory of the script
    3. The data directory specified in the settings file
    """

    def return_file(path: str) -> gpd.GeoDataFrame | pd.DataFrame:
        if path.endswith(".shp"):
            return gpd.read_file(path)
        elif path.endswith(".csv"):
            return pd.read_csv(path, comment="#")
        else:
            raise ValueError(f"Unsupported file type: {path}")

    # 1. Try current working directory
    if os.path.exists(file_path):
        return return_file(file_path)

    # 2. Try directory of the script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    script_path = os.path.join(script_dir, file_path)
    if os.path.exists(script_path):
        return return_file(script_path)

    # 3. Try data directory
    data_path = os.path.join(os.path.expanduser(settings.data_dir), file_path)
    if os.path.exists(data_path):
        return return_file(data_path)

    raise FileNotFoundError(
        f"Shapefile '{file_path}' not found in current directory, script directory, or provided path."
    )


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
        (path_data.from_node.iloc[i], path_data.to_node.iloc[i])
        for i in range(len(path_data))
    ]
    path_nodes = set(path_data.from_node).union(set(path_data.to_node))
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

    # total_cost = path_data["cost_total"].sum()
    # turns = path_data[path_data.turnType > 0]["turnType"].count()
    path_size = path_data.iloc[0]["path_size"]
    # Add path information to the plot
    info_text = (
        f"Origin: {origin}\n"
        f"Destination: {destination}\n"
        f"Iteration: {iteration}\n"
        f"Number of Edges: {num_edges}\n"
        f"Total Distance: {total_distance:.2f} units\n"
        # f"Turns: {turns}\n"
        # f"Total Cost: {total_cost:.2f}\n"
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
