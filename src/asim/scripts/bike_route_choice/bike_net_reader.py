import geopandas as gpd
import pandas as pd
import numpy as np
import math
import yaml
from pydantic import BaseModel, ValidationError
from typing import Optional, List
import os
import sys
import logging
import warnings
import tqdm

# Set up logging
logger = logging.getLogger(__name__)

# turn type encodings
turn_none = 0
turn_left = 1
turn_right = 2
turn_reverse = 3


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

    # path to bike route choice model output
    output_path: str = "output"

    # whether to trace edge and traversal utility calculations
    trace_bike_utilities: bool = False

    # whether to recreate Java attributes -- not needed, but here for backwards compatibility tests
    recreate_java_attributes: bool = False


def load_settings(
    yaml_path: str = "bike_route_choice_settings.yaml",
) -> BikeRouteChoiceSettings:
    with open(yaml_path, "r") as f:
        data = yaml.safe_load(f)
    try:
        settings = BikeRouteChoiceSettings(**data)
    except ValidationError as e:
        print("Settings validation error:", e)
        raise

    # ensure output path exists
    if not os.path.exists(settings.output_path):
        os.makedirs(settings.output_path)
        logger.info(f"Created output directory: {settings.output_path}")

    # setup logger
    log_file_location = os.path.join(settings.output_path, "bike_model.log")
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(levelname)s - %(message)s",
        handlers=[logging.FileHandler(log_file_location), logging.StreamHandler()],
    )

    return settings


def calc_edge_angle(
    start_x: float, start_y: float, end_x: float, end_y: float
) -> float:
    """
    Calculate the heading of an edge from its start x and y coordinates
    """

    deltax = end_x - start_x
    deltay = end_y - start_y

    return math.atan2(deltay, deltax)


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


def create_and_attribute_edges(node: pd.DataFrame, link: pd.DataFrame) -> pd.DataFrame:
    """
    Create and attribute edges from the provided node and link dataframes.
    This function creates a directional edge dataframe from the links,
    creates reverse direction edges, combines them, and attributes them.
    It also manipulates the node dataframe to the output format and calculates
    edge headings and arterial status.

    Parameters:
        node: pd.DataFrame - Node dataframe
        link: pd.DataFrame - Link dataframe

    Returns:
        edges: pd.DataFrame - Edge dataframe with derived attributes
        nodes: pd.DataFrame - Node dataframe with derived attributes

    """

    # create directional edge dataframe from links
    logger.info("Creating directional edges from links")
    abEdges = (
        link.rename(
            columns={
                "A": "fromNode",
                "B": "toNode",
                "AB_Gain": "gain",
                "ABBikeClas": "bikeClass",
                "AB_Lanes": "lanes",
                "Func_Class": "functionalClass",
                "Bike2Sep": "cycleTrack",
                "Bike3Blvd": "bikeBlvd",
            }
        )
        .copy()
        .assign(
            distance=link.Shape_Leng / 5280.0, # convert feet to miles
            autosPermitted=link.Func_Class.isin(range(1, 8)),
            centroidConnector=link.Func_Class == 10,
        )[
            [
                "fromNode",
                "toNode",
                "bikeClass",
                "lanes",
                "functionalClass",
                "centroidConnector",
                "autosPermitted",
                "cycleTrack",
                "bikeBlvd",
                "distance",
                "gain",
            ]
        ]
    )

    # create reverse direction edges
    baEdges = (
        link.rename(
            columns={
                "B": "fromNode",
                "A": "toNode",
                "BA_Gain": "gain",
                "BABikeClas": "bikeClass",
                "BA_Lanes": "lanes",
                "Func_Class": "functionalClass",
                "Bike2Sep": "cycleTrack",
                "Bike3Blvd": "bikeBlvd",
            }
        )
        .copy()
        .assign(
            distance=link.Shape_Leng / 5280.0, # convert feet to miles
            autosPermitted=link.Func_Class.isin(range(1, 8)),
            centroidConnector=link.Func_Class == 10,
        )[
            [
                "fromNode",
                "toNode",
                "bikeClass",
                "lanes",
                "functionalClass",
                "centroidConnector",
                "autosPermitted",
                "cycleTrack",
                "bikeBlvd",
                "distance",
                "gain",
            ]
        ]
    )

    # combine bidirectional edges
    edges = pd.concat([abEdges, baEdges]).set_index(["fromNode", "toNode"])

    # manipulate node dataframe to output format
    nodes = (
        node.assign(centroid=(node.MGRA > 0) | (node.TAZ > 0))
        .rename(
            columns={
                "NodeLev_ID": "id",
                "XCOORD": "x",
                "YCOORD": "y",
                "MGRA": "mgra",
                "TAZ": "taz",
                "Signal": "signalized",
            }
        )
        .drop(columns=["ZCOORD", "geometry"])
        .set_index("id")
    )

    # calculate edge headings
    # Merge coordinates for fromNode and toNode
    edges_with_coords = (
        edges.reset_index()
        .merge(
            nodes[["x", "y"]],
            how="left",
            left_on="fromNode",
            right_index=True,
            suffixes=("", "_from"),
        )
        .merge(
            nodes[["x", "y"]],
            how="left",
            left_on="toNode",
            right_index=True,
            suffixes=("_from", "_to"),
        )
    )

    # Calculate angle in radians
    edges_with_coords["angle"] = np.arctan2(
        edges_with_coords["y_to"] - edges_with_coords["y_from"],
        edges_with_coords["x_to"] - edges_with_coords["x_from"],
    )

    # Set index and assign angle back to edges
    edges_with_coords = edges_with_coords.set_index(["fromNode", "toNode"]).reindex(
        edges.index
    )
    assert edges_with_coords.index.equals(
        edges.index
    ), "Index mismatch between edges and coordinates"
    edges["angle"] = edges_with_coords["angle"]

    # attribute (major) arterial status
    edges["majorArterial"] = (
        (edges.functionalClass <= 3)
        & (edges.functionalClass > 0)
        & (edges.bikeClass != 1)
    )
    edges["arterial"] = (
        (edges.functionalClass <= 4)
        & (edges.functionalClass > 0)
        & (edges.bikeClass != 1)
    )

    # keep track of how many duplicate edges are (major) arterials
    dupMajArts = edges.groupby(edges.index).majorArterial.sum()
    dupMajArts.index = pd.MultiIndex.from_tuples(dupMajArts.index)
    edges["dupMajArts"] = dupMajArts.reindex(edges.index)

    dupArts = edges.groupby(edges.index).arterial.sum()
    dupArts.index = pd.MultiIndex.from_tuples(dupArts.index)
    edges["dupArts"] = dupArts.reindex(edges.index)

    # keep track of how many edges emanating from a node cross (major) arterials
    nodes["majorArtXings"] = (
        edges.groupby(edges.index.get_level_values(0))
        .majorArterial.sum()
        .reindex(nodes.index)
    )
    nodes["artXings"] = (
        edges.groupby(edges.index.get_level_values(0))
        .arterial.sum()
        .reindex(nodes.index)
    )
    return edges, nodes


def recreate_java_attributes(
       traversals: pd.DataFrame,
):
    """
    Recreate Java attributes for traversals.
    WARNING: This function contains bugs that we think exist in the Java implementation.
    This is merely an optional function used for potential backwards compatibility.\
    
    Loop and vectorized outputs should be the same.  Loop code closely mimic the Java implementation,
    while vectorized code is more efficient for python implementation.

    Parameters:
        traversals: pd.DataFrame - Traversal dataframe with derived attributes

    Returns:
        pd.DataFrame - Traversal dataframe with recreated Java attributes

    """
    ##########################################################################
    # BUG IMPLEMENTATIONS BELOW

    def isThruJunc(trav, turn_type, last_all):
        # this method is buggy because it only considers the last traversal
        # instead of checking for any right turn
        if trav.centroid_start or trav.centroid_thru or trav.centroid_end:
            return False
        if trav.turnType != turn_none:
            return False
        if (
            len(
                traversals[
                    (traversals.start == trav.start)
                    & (traversals.thru == trav.thru)
                    & (traversals.end != trav.start)
                    & (traversals.end != trav.end)
                ]
            )
            == 0
        ):
            return True

        if last_all == "last":
            if (
                traversals[
                    (traversals.start == trav.start)
                    & (traversals.thru == trav.thru)
                    & (traversals.end != trav.start)
                    & (traversals.end != trav.end)
                ]
                .iloc[-1]
                .turnType
                == turn_type
            ):
                return False
            else:
                return True

        elif last_all == "any":
            return not (
                traversals[
                    (traversals.start == trav.start)
                    & (traversals.thru == trav.thru)
                    & (traversals.end != trav.start)
                    & (traversals.end != trav.end)
                ].turnType
                == turn_type
            ).any()
        
    # keep track of whether traversal is "thru junction"
    traversals["ThruJunction_anynonevec"] = (
        ~(
            traversals.centroid_start
            | traversals.centroid_thru
            | traversals.centroid_end
        )
        & (traversals.turnType == turn_none)
        & (traversals.none_turns - traversals.dupNoneTurns_toEdge == 0)
    )

    traversals["ThruJunction_lastnoneloop"] = False
    traversals["ThruJunction_lastrtloop"] = False
    traversals["ThruJunction_anynoneloop"] = False
    traversals["ThruJunction_anyrtloop"] = False

    logger.info("Beginning slow loop recreating Java attributes")

    for idx, trav in tqdm.tqdm(traversals.iterrows(), total=len(traversals)):

        traversals.loc[idx, "ThruJunction_lastnoneloop"] = isThruJunc(
            trav, turn_none, "last"
        )
        traversals.loc[idx, "ThruJunction_lastrtloop"] = isThruJunc(
            trav, turn_right, "last"
        )
        traversals.loc[idx, "ThruJunction_anynoneloop"] = isThruJunc(
            trav, turn_none, "any"
        )
        traversals.loc[idx, "ThruJunction_anyrtloop"] = isThruJunc(
            trav, turn_right, "any"
        )

    # the below is buggy because it counts none-turns instead of right turns

    # index: all last traversals of all input groups
    # values: whether the index traversal is a none turn
    is_none = (
        traversals.groupby(["start", "thru"])
        .last()
        .reset_index()
        .set_index(["start", "thru", "end"])
        .turnType
        == turn_none
    )

    last_travs = is_none.index

    # index: all last and penultimate traversals
    # values: whether the index traversal is a none turn
    is_none = pd.concat([is_none,
        traversals[
            # don't allow index of penultimates to match last - we want two different candidates
            ~traversals.set_index(["start", "thru", "end"]).index.isin(last_travs)
        ]
        .groupby(["start", "thru"])
        .last()
        .reset_index()
        .set_index(["start", "thru", "end"])
        .turnType
        == turn_none
    ])

    penult_travs = is_none.index[~is_none.index.isin(last_travs)]
    penult_is_none = is_none[penult_travs]
    last_is_none = is_none[last_travs]

    # tack on the two new columns
    traversals = traversals.merge(
        last_is_none, left_on=["start", "thru"], right_index=True, how="left"
    ).merge(
        penult_is_none, left_on=["start", "thru"], right_index=True, how="left"
    )

    # for all traversals that match the last traversal, use the penultimate value
    traversals.loc[traversals.index.isin(last_is_none.index), "buggyRTE_none"] = (
        traversals.loc[traversals.index.isin(last_is_none.index), "penultimate_is_none"]
    )
    # for all other traversals, use the last value
    traversals.loc[~traversals.index.isin(last_is_none.index), "buggyRTE_none"] = (
        traversals.loc[~traversals.index.isin(last_is_none.index), "last_is_none"]
    )

    # index: all last traversals of all input groups
    # values: whether the index traversal is a right turn
    last_is_rt = (
        traversals.groupby(["start", "thru"])
        .last()
        .reset_index()
        .set_index(["start", "thru", "end"])
        .turnType
        == turn_right
    )
    # index: all penultimate traversals of input groups w/ >1 out link
    # values: whether the index traversal is a right turn
    penultimate_is_rt = (
        traversals[
            ~traversals.set_index(["start", "thru", "end"]).index.isin(last_is_rt.index)
        ]
        .groupby(["start", "thru"])
        .last()
        .reset_index()
        .set_index(["start", "thru", "end"])
        .turnType
        == turn_right
    )

    # drop the end column 
    last_is_rt = (
        last_is_rt.reset_index()
        .set_index(["start", "thru"])
        .turnType.rename("last_is_rt")
    )
    # drop the end column and assume false for input groups w/ 1 out link
    penultimate_is_rt = (
        penultimate_is_rt.reset_index()
        .set_index(["start", "thru"])
        .turnType.reindex(last_is_rt.index, fill_value=False)
        .rename("penultimate_is_rt")
    )

    # tack on two new columns
    traversals = traversals.merge(
        last_is_rt, left_on=["start", "thru"], right_index=True, how="left"
    ).merge(
        penultimate_is_rt, left_on=["start", "thru"], right_index=True, how="left"
    )

    traversals.loc[traversals.index.isin(last_is_rt.index), "buggyRTE_rt"] = traversals.loc[
        traversals.index.isin(last_is_rt.index), "penultimate_is_rt"
    ]
    traversals.loc[~traversals.index.isin(last_is_rt.index), "buggyRTE_rt"] = traversals.loc[
        ~traversals.index.isin(last_is_rt.index), "last_is_rt"
    ]

    traversals["ThruJunction_lastnonevec"] = (
        ~(
            traversals.centroid_start
            | traversals.centroid_thru
            | traversals.centroid_end
        )
        & (traversals.turnType == turn_none)
        & ~(traversals.buggyRTE_none)
    )
    traversals["ThruJunction_lastrtvec"] = (
        ~(
            traversals.centroid_start
            | traversals.centroid_thru
            | traversals.centroid_end
        )
        & (traversals.turnType == turn_none)
        & ~(traversals.buggyRTE_rt)
    )
    # END BUG CODE
    #######################################
    logger.info("Finished calculating java attributes")

    java_cols = [
        "ThruJunction_anynonevec",
        "ThruJunction_lastnoneloop",
        "ThruJunction_lastrtloop",
        "ThruJunction_anynoneloop",
        "ThruJunction_anyrtloop",
        "ThruJunction_anyrtvec",
        "ThruJunction_lastnonevec",
        "ThruJunction_lastrtvec",
    ]

    return traversals, java_cols


def calculate_signalExclRight_alternatives(traversals: pd.DataFrame) -> pd.DataFrame:
    """
    Calculate alternative signalized right turn exclusion columns.
    This was originally used to test backwards compatibility with the Java implementation.
    It is not used in the current implementation, but is left here for reference.

        "signalExclRight_lastnoneloop",  # current Java implementation
        "signalExclRight_lastrtloop",  # fixes rt-vs-none
        "signalExclRight_lastnonevec",  # vector implementation of current Java
        "signalExclRight_lastrtvec",  # same as above but also fixes rt-vs-none
        "signalExclRight_anynoneloop",  # fixes any-vs-last check
        "signalExclRight_anyrtloop",  # fixes any-vs-last and rt-vs-none (allegedly correct)
        "signalExclRight_anynonevec",  # vector implementation of any-vs-last fix
        "signalExclRight_anyrtvec",  # allegedly correct vector implementation

    Parameters:
        traversals: pd.DataFrame - Traversal dataframe with attributes

    Returns:
        pd.DataFrame - Traversal dataframe with alternative signalized right turn exclusion columns
    """
    # the rest can be ditched (ALLEGEDLY)
    traversals = traversals.assign(
        signalExclRight_anynonevec=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_anynonevec)
        ),
        signalExclRight_anyrtloop=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_anyrtloop)
        ),
        signalExclRight_anynoneloop=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_anynoneloop)
        ),
        signalExclRight_lastnonevec=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_lastnonevec)
        ),
        signalExclRight_lastrtvec=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_lastrtvec)
        ),
        signalExclRight_lastnoneloop=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_lastnoneloop)
        ),
        signalExclRight_lastrtloop=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_lastrtloop)
        )
    )

    java_attributes = [
        "signalExclRight_lastnoneloop",
        "signalExclRight_lastrtloop",
        "signalExclRight_lastnonevec",
        "signalExclRight_lastrtvec",
        "signalExclRight_anynoneloop",
        "signalExclRight_anyrtloop",
        "signalExclRight_anynonevec",
        "signalExclRight_anyrtvec",
    ]

    return traversals, java_attributes



def create_and_attribute_traversals(
    edges: pd.DataFrame, nodes: pd.DataFrame
) -> pd.DataFrame:
    """
    Create and attribute traversals from edges and nodes.

    This function creates a traversal table from the edges, calculates traversal attributes,
    and merges node information to provide a comprehensive traversal dataset.
    It calculates angles, turn types, and various derived attributes for each traversal.

    Parameters:
        edges: pd.DataFrame - Edge dataframe with attributes
        nodes: pd.DataFrame - Node dataframe with attributes

    Returns:
        pd.DataFrame - Traversal dataframe with derived attributes
    """

    # create initial traversal table from edges
    logger.info("Creating traversal table from edges")
    traversals = (
        edges.reset_index()
        .merge(
            edges.reset_index(),
            left_on="toNode",
            right_on="fromNode",
            suffixes=["_fromEdge", "_toEdge"],
        )
        .rename(
            columns={
                "fromNode_fromEdge": "start",
                "toNode_fromEdge": "thru",
                "toNode_toEdge": "end",
            }
        )
        .merge(nodes, left_on="thru", right_index=True)
    )

    # drop U-turns
    traversals = traversals[traversals.start != traversals.end]

    # calculate traversal angles
    traversals["angle"] = traversals.angle_toEdge - traversals.angle_fromEdge
    traversals.loc[traversals.angle > math.pi, "angle"] = (
        traversals.loc[traversals.angle > math.pi, "angle"] - 2 * math.pi
    )
    traversals.loc[traversals.angle < -math.pi, "angle"] = (
        traversals.loc[traversals.angle < -math.pi, "angle"] + 2 * math.pi
    )
    traversals.set_index(["start", "thru", "end"])

    # keep track of the absolute value of the traversal angle
    traversals["absAngle"] = traversals.angle.abs()

    # attach component nodes' centroid statuses
    traversals = (
        traversals.merge(
            nodes.centroid,
            left_on="start",
            right_index=True,
            suffixes=("_thru", "_start"),
        )
        .merge(
            nodes.centroid,
            left_on="end",
            right_index=True,
        )
        .rename(columns={"centroid": "centroid_end"})
    )

    logger.info("Calculating derived traversal attributes")

    # calculate traversal angle attributes for thru node
    max_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .angle.max()
        .fillna(-math.pi)
        .rename("max_angle")
    )

    min_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .angle.min()
        .fillna(math.pi)
        .rename("min_angle")
    )

    min_abs_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .absAngle.min()
        .fillna(math.pi)
        .rename("min_abs_angle")
    )

    leg_count = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .size()
        .rename("leg_count")
        + 1
    )

    # consolidate into a dataframe for merging and reindex to full traversal table
    turn_atts = (
        pd.concat(
            [
                max_angles,
                min_angles,
                min_abs_angles,
                leg_count,
            ],
            axis=1,
        )
        .reindex([traversals.start, traversals.thru])
        .fillna(
            {
                "max_angle": -math.pi,
                "min_angle": math.pi,
                "min_abs_angle": math.pi,
                "leg_count": 1,
            }
        )
    )

    # attach to full table
    traversals = pd.concat(
        [traversals.set_index(["start", "thru"]), turn_atts], axis=1
    ).reset_index()

    # calculate turn type
    # turn type is determined by the angle and leg count
    # the options are no turns, left, right, and reverse
    traversals.loc[~traversals.leg_count.isna(), "turnType"] = turn_none

    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle <= traversals.min_angle)
        & (traversals.angle.abs() > math.pi / 6),
        "turnType",
    ] = turn_right

    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle >= traversals.max_angle)
        & (traversals.angle.abs() > math.pi / 6),
        "turnType",
    ] = turn_left

    traversals.loc[
        (traversals.leg_count > 3)
        & (traversals.angle.abs() > traversals.min_abs_angle)
        & (
            (traversals.angle.abs() >= math.pi / 6)
            | (traversals.angle <= traversals.min_angle)
            | (traversals.angle >= traversals.max_angle)
        )
        & (traversals.angle < 0),
        "turnType",
    ] = turn_right

    traversals.loc[
        (traversals.leg_count > 3)
        & (traversals.angle.abs() > traversals.min_abs_angle)
        & (
            (traversals.angle.abs() >= math.pi / 6)
            | (traversals.angle <= traversals.min_angle)
            | (traversals.angle >= traversals.max_angle)
        )
        & (traversals.angle >= 0),
        "turnType",
    ] = turn_left

    traversals.loc[
        (traversals.angle < -math.pi * 5 / 6) | (traversals.angle > math.pi * 5 / 6),
        "turnType",
    ] = turn_reverse

    traversals.loc[traversals.start == traversals.end, "turnType"] = turn_reverse

    traversals.loc[
        traversals.centroid_start | traversals.centroid_thru | traversals.centroid_end,
        "turnType",
    ] = turn_none

    traversals.turnType = traversals.turnType.astype(int)

    # keep track of the number of outgoing turns w/ turn type == none
    # FIXME this should almost certainly get removed
    traversals = traversals.merge(
        (traversals.set_index(["start", "thru"]).turnType == turn_none)
        .reset_index()
        .groupby(["start", "thru"])
        .sum()
        .rename(columns={"turnType": "none_turns"}),
        left_on=["start", "thru"],
        right_index=True,
        how="left",
    )

    # do the same but with right turns
    # FIXME this should be the actual usage, not the above, but we're
    # copying from the java implementation
    traversals = traversals.merge(
        (traversals.set_index(["start", "thru"]).turnType == turn_right)
        .reset_index()
        .groupby(["start", "thru"])
        .sum()
        .rename(columns={"turnType": "rt_turns"}),
        left_on=["start", "thru"],
        right_index=True,
        how="left",
    )

    # keep track of how many duplicate traversals have turn type == none
    traversals = traversals.merge(
        (traversals.set_index(["start", "thru", "end"]).turnType == turn_none)
        .reset_index()
        .groupby(["start", "thru", "end"])
        .sum()
        .rename(columns={"turnType": "dupNoneTurns_toEdge"}),
        left_on=["start", "thru", "end"],
        right_index=True,
        how="left",
    )

    traversals = traversals.merge(
        (traversals.set_index(["start", "thru", "end"]).turnType == turn_right)
        .reset_index()
        .groupby(["start", "thru", "end"])
        .sum()
        .rename(columns={"turnType": "dupRtTurns_toEdge"}),
        left_on=["start", "thru", "end"],
        right_index=True,
        how="left",
    )

    if settings.recreate_java_attributes:
        traversals, java_cols = recreate_java_attributes(traversals)
        traversals, java_attributes = calculate_signalExclRight_alternatives(traversals)
        java_cols += java_attributes

    traversals["ThruJunction_anyrtvec"] = (
        ~(
            traversals.centroid_start
            | traversals.centroid_thru
            | traversals.centroid_end
        )
        & (traversals.turnType == turn_none)
        & (traversals.rt_turns - traversals.dupRtTurns_toEdge == 0)
    )

    # populate derived traversal attributes
    
    traversals = traversals.assign(
        thruCentroid=traversals.centroidConnector_fromEdge
        & traversals.centroidConnector_toEdge,
        # this one is allegedly the one to keep
        # taken from signalExclRight_anyrtvec
        signalExclRight=(
            traversals.signalized
            & (traversals.turnType != turn_right)
            & (~traversals.ThruJunction_anyrtvec)
        ),
        # unlfrma: unsignalized left from major arterial
        unlfrma=(
            (~traversals.signalized)
            & (traversals.functionalClass_fromEdge <= 3)
            & (traversals.functionalClass_fromEdge > 0)
            & (traversals.bikeClass_fromEdge != 1)
            & (traversals.turnType == turn_left)
        ),
        # unlfrmi: unsignalized left from minor arterial
        unlfrmi=(
            (~traversals.signalized)
            & (traversals.functionalClass_fromEdge == 4)
            & (traversals.bikeClass_fromEdge != 1)
            & (traversals.turnType == turn_left)
        ),
        # unxma: unsignalized cross major arterial
        unxma=(
            ~(
                traversals.centroid_start
                | traversals.centroid_thru
                | traversals.centroid_end
            )
            & (
                (
                    (traversals.turnType == turn_none)
                    & (
                        traversals.majorArtXings
                        - traversals.dupMajArts_fromEdge
                        - traversals.dupMajArts_toEdge
                    )
                    >= 2
                )
                | (
                    (traversals.turnType == turn_left)
                    & (traversals.functionalClass_toEdge <= 3)
                    & (traversals.functionalClass_toEdge > 0)
                    & (traversals.bikeClass_toEdge != 1)
                )
            )
        ),
        # unxmi: unsignalized cross minor arterial
        unxmi=(
            ~(
                traversals.centroid_start
                | traversals.centroid_thru
                | traversals.centroid_end
            )
            & (
                (
                    (traversals.turnType == turn_none)
                    & (
                        traversals.artXings
                        - traversals.dupArts_fromEdge
                        - traversals.dupArts_toEdge
                    )
                    >= 2
                )
                | (
                    (traversals.turnType == turn_left)
                    & (traversals.functionalClass_toEdge == 4)
                    & (traversals.functionalClass_toEdge > 0)
                    & (traversals.bikeClass_toEdge != 1)
                )
            )
        ),
    )


    output_cols = [
        "turnType",
        "thruCentroid",
        "signalExclRight",
        "unlfrma",
        "unlfrmi",
        "unxma",
        "unxmi",
    ]
    if settings.recreate_java_attributes:
        # include the java attributes if they were recreated
        output_cols += java_cols

    # keep only the relevant columns
    traversals = traversals.set_index(["start", "thru", "end"])[output_cols]
    
    logger.info("Finished calculating derived traversal attributes")

    return traversals


def read_bike_net(
    settings: BikeRouteChoiceSettings,
) -> tuple[
    pd.DataFrame,  # node dataframe
    pd.DataFrame,  # edge dataframe
    pd.DataFrame,  # traversal dataframe
]:
    """
    Read bike network from supplied shapefiles and derive attributes

    This method reads in two shapefiles detailing the nodes and edges
    of a bike network, manipulates the data tables to match the expected
    output format, and derives additonal attributes, some of which are
    solely used internally while others are appended to the edge and node
    tables. Additionally, a new table is developed of all traversals, that
    is, all possible combinations of edges leading to and from a node which
    are not reversals.

    Parameters:
        node_file: str  -   path to the node shapefile
        edge_file: str  -   path to the edge shapefile

    Returns:
        nodes: pd.DataFrame   -     node dataframe with derived attributes in expected format
        edges: pd.DataFrame   -     edge dataframe with derived attributes in expected format
        traversals: pd.DataFrame -  traversal dataframe with derived attributes in expected format
    """

    # read shapefiles
    logger.info("Reading link and node shapefiles")
    node = read_file(settings, settings.node_file)
    link = read_file(settings, settings.link_file)

    # create and attribute edges
    edges, nodes = create_and_attribute_edges(node, link)

    traversals = create_and_attribute_traversals(edges, nodes)


    return nodes, edges, traversals


def calculate_utilities(
    settings: BikeRouteChoiceSettings,
    choosers: pd.DataFrame,
    spec: pd.DataFrame,
    trace_label: str,
) -> pd.DataFrame:
    """
    Calculate utilities for choosers using the provided specifications.
    Modeled after ActivitySim's core.simulate.eval_utilities.

    Parameters:
        settings: BikeRouteChoiceSettings - settings for the bike route choice model
        choosers: pd.DataFrame - DataFrame of choosers (edges or traversals)
        spec: pd.Series - DataFrame with index as utility expressions and values as coefficients
        trace_label: str - label for tracing
    Returns:
        pd.DataFrame - DataFrame of calculated utilities with same index as choosers
    """

    assert isinstance(
        spec, pd.Series
    ), "Spec must be a pandas Series with utility expressions as index and coefficients as values"

    globals_dict = {}
    locals_dict = {
        "np": np,
        "pd": pd,
        "df": choosers,
    }

    expression_values = np.empty((spec.shape[0], choosers.shape[0]))

    for i, expr in enumerate(spec.index):
        try:
            with warnings.catch_warnings(record=True) as w:
                # Cause all warnings to always be triggered.
                warnings.simplefilter("always")
                if expr.startswith("@"):
                    expression_value = eval(expr[1:], globals_dict, locals_dict)
                else:
                    expression_value = choosers.eval(expr)

                if len(w) > 0:
                    for wrn in w:
                        logger.warning(
                            f"{trace_label} - {type(wrn).__name__} ({wrn.message}) evaluating: {str(expr)}"
                        )

        except Exception as err:
            logger.exception(
                f"{trace_label} - {type(err).__name__} ({str(err)}) evaluating: {str(expr)}"
            )
            raise err

        expression_values[i] = expression_value

    # - compute_utilities
    utilities = np.dot(expression_values.transpose(), spec.astype(np.float64).values)
    utilities = pd.DataFrame(utilities, index=choosers.index, columns=["utility"])

    if settings.trace_bike_utilities:
        # trace entire utility calculation
        logger.info(f"Tracing {trace_label} utilities")
        trace_targets = pd.Series(True, index=choosers.index)
        offsets = np.nonzero(list(trace_targets))[0]

        if expression_values is not None:
            data = expression_values[:, offsets]
            # index is utility expressions (and optional label if MultiIndex)
            expression_values_df = pd.DataFrame(data=data, index=spec.index)
            expression_values_df.to_csv(
                os.path.join(settings.output_path, f"{trace_label}_utilities.csv"),
            )
        else:
            logger.info(f"No expression values to trace for {trace_label} utilities")

    assert utilities.index.equals(
        choosers.index
    ), "Index mismatch between utilities and choosers"

    return utilities


def calculate_edge_utilities(
    settings: BikeRouteChoiceSettings,
    edges: pd.DataFrame,
    randomize_coeffs: bool = True,
) -> pd.DataFrame:
    """
    Calculate edge utilities from the edge utility specification file
    """

    # read edge utility specification file
    edge_spec = read_file(settings, settings.edge_util_file)
    trace_label = "bike_edge_utilities"

    # TODO Randomize edge coefficients
    if randomize_coeffs:
        logger.info("Randomizing edge coefficients")
        # edge_spec["Coefficient"] *= np.random.uniform(0, 1, size=edge_spec.shape[0])

    # calculate edge utilities
    edge_utilities = calculate_utilities(
        settings=settings,
        choosers=edges,
        spec=edge_spec.set_index("Expression")["Coefficient"],
        trace_label=trace_label,
    )

    # check that all edge utilities are less than or equal to zero
    # positive utilities will cause issues in Dijkstra's algorithm
    assert (
        edge_utilities.utility <= 0
    ).all(), "Edge utilities should all be less than or equal to zero"

    return edge_utilities


def calculate_traversal_utilities(
    settings: BikeRouteChoiceSettings,
    traversals: pd.DataFrame,
    randomize_coeffs: bool = True,
) -> pd.DataFrame:
    """
    Calculate traversal utilities from the traversal utility specification file
    """

    # read traversal utility specification file
    trav_util = read_file(settings, settings.traversal_util_file)
    trace_label = "bike_traversal_utilities"

    # TODO Randomize traversal coefficients
    if randomize_coeffs:
        logger.info("Randomizing traversal coefficients")
        # trav_util["Coefficient"] *= np.random.uniform(0, 1, size=trav_util.shape[0])

    # calculate traversal utilities
    traversal_utilities = calculate_utilities(
        settings=settings,
        choosers=traversals,
        spec=trav_util.set_index("Expression")["Coefficient"],
        trace_label=trace_label,
    )

    # check that all traversal utilities are less than or equal to zero
    assert (
        traversal_utilities.utility <= 0
    ).all(), "Traversal utilities should all be less than or equal to zero"

    return traversal_utilities


if __name__ == "__main__":
    # can pass settings file as command line argument
    if len(sys.argv) > 1:
        settings_file = sys.argv[1]
    else:
        settings_file = "bike_route_choice_settings.yaml"
    # load settings
    settings = load_settings(settings_file)

    # create bike network
    nodes, edges, traversals = read_bike_net(settings)

    # calculate utilities
    edges["utility"] = calculate_edge_utilities(settings, edges)
    traversals["utility"] = calculate_traversal_utilities(settings, traversals)
