import pandas as pd
import numpy as np
import os
import math
import logging

# import tqdm
from bike_route_utilities import BikeRouteChoiceSettings, read_file

# turn type encodings
TURN_NONE = 0
TURN_LEFT = 1
TURN_RIGHT = 2
TURN_REVERSE = 3

BLUE_STEEL = TURN_RIGHT
LE_TIGRE = TURN_RIGHT
FERRARI = TURN_RIGHT
MAGNUM = TURN_LEFT


def create_and_attribute_edges(
    settings: BikeRouteChoiceSettings,
    node: pd.DataFrame,
    link: pd.DataFrame,
    logger: logging.Logger,
) -> pd.DataFrame:
    """
    Create and attribute edges from the provided node and link dataframes.
    This function creates a directional edge dataframe from the links,
    creates reverse direction edges, combines them, and attributes them.
    It also manipulates the node dataframe to the output format and calculates
    edge headings and arterial status.

    Parameters:
        settings: BikeRouteChoiceSettings - Settings for the bike route choice model
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
                "SPEED": "speedLimit",
            }
        )
        .copy()
        .assign(
            distance=link.Shape_Leng / 5280.0,  # convert feet to miles
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
                "speedLimit",
                "geometry",
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
                "SPEED": "speedLimit",
            }
        )
        .copy()
        .assign(
            distance=link.Shape_Leng / 5280.0,  # convert feet to miles
            autosPermitted=link.Func_Class.isin(range(1, 8)),
            centroidConnector=link.Func_Class == 10,
            geometry=link.geometry.reverse(),
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
                "speedLimit",
                "geometry",
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
                "NodeLev_ID": "nodeID",
                "XCOORD": "x",
                "YCOORD": "y",
                "MGRA": "mgra",
                "TAZ": "taz",
                "Signal": "signalized",
            }
        )
        .drop(columns=["ZCOORD", "geometry"])
        .set_index("nodeID")
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


# def recreate_java_attributes(
#     traversals: pd.DataFrame,
# ):
#     """
#     Recreate Java attributes for traversals.
#     WARNING: This function contains bugs that we think exist in the Java implementation.
#     This is merely an optional function used for potential backwards compatibility.\

#     Loop and vectorized outputs should be the same.  Loop code closely mimic the Java implementation,
#     while vectorized code is more efficient for python implementation.

#     Parameters:
#         traversals: pd.DataFrame - Traversal dataframe with derived attributes

#     Returns:
#         pd.DataFrame - Traversal dataframe with recreated Java attributes

#     """
#     def isThruJunc(trav, turn_type, last_all):
#         # this method is buggy because it only considers the last traversal
#         # instead of checking for any right turn
#         if trav.centroid_start or trav.centroid_thru or trav.centroid_end:
#             return False

#         if trav.turnType != TURN_NONE:
#             return False

#         if (
#             # if there are no sibling traversals
#             len(
#                 traversals[
#                     (traversals.start == trav.start)
#                     & (traversals.thru == trav.thru)
#                     & (traversals.end != trav.start)
#                     & (traversals.end != trav.end)
#                 ]
#             )
#             == 0
#         ):
#             return True


#         if last_all == "last":
#             # if the last sibling traversal is of the target turn type
#             if (
#                 traversals[
#                     (traversals.start == trav.start)
#                     & (traversals.thru == trav.thru)
#                     & (traversals.end != trav.start)
#                     & (traversals.end != trav.end)
#                 ]
#                 .iloc[-1]
#                 .turnType
#                 == turn_type
#             ):
#                 return False
#             else:
#                 return True

#         elif last_all == "any":
#             # if any sibling traversal is of the target turn type
#             return not (
#                 traversals[
#                     (traversals.start == trav.start)
#                     & (traversals.thru == trav.thru)
#                     & (traversals.end != trav.start)
#                     & (traversals.end != trav.end)
#                 ].turnType
#                 == turn_type
#             ).any()

#     # keep track of whether traversal is "thru junction"
#     traversals["ThruJunction_anynonevec"] = (
#         ~(
#             traversals.centroid_start
#             | traversals.centroid_thru
#             | traversals.centroid_end
#         )
#         & (traversals.turnType == TURN_NONE)
#         & (traversals.none_turns - traversals.dupNoneTurns_toEdge == 0)
#     )

#     # create columns to populate in the slow loop
#     traversals["ThruJunction_lastnoneloop"] = False
#     traversals["ThruJunction_lastrtloop"] = False
#     traversals["ThruJunction_anynoneloop"] = False
#     traversals["ThruJunction_anyrtloop"] = False

#     logger.info("Beginning slow loop recreating Java attributes")

#     # the slow loop through all the traversals (could this be parallelized?)
#     for idx, trav in tqdm.tqdm(traversals.iterrows(), total=len(traversals)):

#         # check all four possible parameter combos
#         traversals.loc[idx, "ThruJunction_lastnoneloop"] = isThruJunc(
#             trav, TURN_NONE, "last"
#         )
#         traversals.loc[idx, "ThruJunction_lastrtloop"] = isThruJunc(
#             trav, TURN_RIGHT, "last"
#         )
#         traversals.loc[idx, "ThruJunction_anynoneloop"] = isThruJunc(
#             trav, TURN_NONE, "any"
#         )
#         traversals.loc[idx, "ThruJunction_anyrtloop"] = isThruJunc(
#             trav, TURN_RIGHT, "any"
#         )

#     # the below is buggy because it counts none-turns instead of right turns

#     # index: all last traversals of all input groups
#     # values: whether the index traversal is a none turn
#     is_none = (
#         traversals.groupby(["start", "thru"])
#         .last()
#         .reset_index()
#         .set_index(["start", "thru", "end"])
#         .turnType
#         == TURN_NONE
#     )

#     last_travs = is_none.index

#     # index: all last and penultimate traversals
#     # values: whether the index traversal is a none turn
#     is_none = pd.concat(
#         [
#             is_none,
#             traversals[
#                 # don't allow index of penultimates to match last - we want two different candidates
#                 ~traversals.set_index(["start", "thru", "end"]).index.isin(last_travs)
#             ]
#             .groupby(["start", "thru"])
#             .last()
#             .reset_index()
#             .set_index(["start", "thru", "end"])
#             .turnType
#             == TURN_NONE,
#         ]
#     )

#     # create an index of all penultimate sibling turn movements
#     penult_travs = is_none.index[~is_none.index.isin(last_travs)]

#     # get the penultimate sibling turn movments whose turn type is none
#     penult_is_none = is_none[penult_travs].reset_index(2).turnType.rename("penultimate_is_none")

#     # get the last sibling turn movements whose turn type is none
#     last_is_none = is_none[last_travs].reset_index(2).turnType.rename("last_is_none")

#     # tack on the two new columns
#     traversals = traversals.merge(
#         last_is_none, left_on=["start", "thru"], right_index=True, how="left"
#     ).merge(penult_is_none, left_on=["start", "thru"], right_index=True, how="left")

#     # for all traversals that match the last traversal, use the penultimate value
#     traversals.loc[
#         traversals.index.isin(last_is_none.index), "buggyRTE_none"
#     ] = traversals.loc[traversals.index.isin(last_is_none.index), "penultimate_is_none"]
#     # for all other traversals, use the last value
#     traversals.loc[
#         ~traversals.index.isin(last_is_none.index), "buggyRTE_none"
#     ] = traversals.loc[~traversals.index.isin(last_is_none.index), "last_is_none"]

#     # index: all last traversals of all input groups
#     # values: whether the index traversal is a right turn
#     last_is_rt = (
#         traversals.groupby(["start", "thru"])
#         .last()
#         .reset_index()
#         .set_index(["start", "thru", "end"])
#         .turnType
#         == TURN_RIGHT
#     )
#     # index: all penultimate traversals of input groups w/ >1 out link
#     # values: whether the index traversal is a right turn
#     penultimate_is_rt = (
#         traversals[
#             ~traversals.set_index(["start", "thru", "end"]).index.isin(last_is_rt.index)
#         ]
#         .groupby(["start", "thru"])
#         .last()
#         .reset_index()
#         .set_index(["start", "thru", "end"])
#         .turnType
#         == TURN_RIGHT
#     )

#     # drop the end column
#     last_is_rt = (
#         last_is_rt.reset_index()
#         .set_index(["start", "thru"])
#         .turnType.rename("last_is_rt")
#     )
#     # drop the end column and assume false for input groups w/ 1 out link
#     penultimate_is_rt = (
#         penultimate_is_rt.reset_index()
#         .set_index(["start", "thru"])
#         .turnType.reindex(last_is_rt.index, fill_value=False)
#         .rename("penultimate_is_rt")
#     )

#     # tack on two new columns (last_is_rt and penultimate_is_rt)
#     traversals = traversals.merge(
#         last_is_rt, left_on=["start", "thru"], right_index=True, how="left"
#     ).merge(penultimate_is_rt, left_on=["start", "thru"], right_index=True, how="left")

#     # define the right turn exists parameter for all positions,
#     # starting with those which are last-siblings
#     traversals.loc[
#         traversals.index.isin(last_is_rt.index), "buggyRTE_rt"
#     ] = traversals.loc[traversals.index.isin(last_is_rt.index), "penultimate_is_rt"]

#     # then moving on to all others
#     traversals.loc[
#         ~traversals.index.isin(last_is_rt.index), "buggyRTE_rt"
#     ] = traversals.loc[~traversals.index.isin(last_is_rt.index), "last_is_rt"]

#     # define whether each entry is a thru-junction based on its underlying
#     # right-turn-exists variables
#     traversals["ThruJunction_lastnonevec"] = (
#         ~(
#             traversals.centroid_start
#             | traversals.centroid_thru
#             | traversals.centroid_end
#         )
#         & (traversals.turnType == TURN_NONE)
#         & ~(traversals.buggyRTE_none)
#     )
#     traversals["ThruJunction_lastrtvec"] = (
#         ~(
#             traversals.centroid_start
#             | traversals.centroid_thru
#             | traversals.centroid_end
#         )
#         & (traversals.turnType == TURN_NONE)
#         & ~(traversals.buggyRTE_rt)
#     )

#     logger.info("Finished calculating java attributes")

#     java_cols = [
#         "ThruJunction_anynonevec",
#         "ThruJunction_lastnoneloop",
#         "ThruJunction_lastrtloop",
#         "ThruJunction_anynoneloop",
#         "ThruJunction_anyrtloop",
#         "ThruJunction_anyrtvec",
#         "ThruJunction_lastnonevec",
#         "ThruJunction_lastrtvec",
#     ]

#     return traversals, java_cols


def calculate_signalExclRight_alternatives(
    traversals: pd.DataFrame, logger: logging.Logger
) -> pd.DataFrame:
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
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_anynonevec)
        ),
        signalExclRight_anyrtloop=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_anyrtloop)
        ),
        signalExclRight_anynoneloop=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_anynoneloop)
        ),
        signalExclRight_lastnonevec=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_lastnonevec)
        ),
        signalExclRight_lastrtvec=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_lastrtvec)
        ),
        signalExclRight_lastnoneloop=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_lastnoneloop)
        ),
        signalExclRight_lastrtloop=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_lastrtloop)
        ),
    )

    java_attributes = [
        "signalExclRight_lastnoneloop",
        "signalExclRight_lastrtloop",
        "signalExclRight_lastnonevec",
        "signalExclRight_lastrtvec",
        "signalExclRight_anynoneloop",
        "signalExclRight_anyrtloop",
        "signalExclRight_anynonevec",
        # "signalExclRight_anyrtvec",
    ]

    return traversals, java_attributes


def create_and_attribute_traversals(
    settings: BikeRouteChoiceSettings,
    edges: pd.DataFrame,
    nodes: pd.DataFrame,
    logger: logging.Logger,
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
            how="outer",
        )
        .rename(
            columns={
                "fromNode_fromEdge": "start",
                "toNode_fromEdge": "thru",
                "toNode_toEdge": "end",
            }
        )
        .merge(nodes, left_on="thru", right_index=True, how="outer")
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
    # maximum turning angle
    max_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .angle.max()
        .fillna(-math.pi)
        .rename("max_angle")
    )

    # minimum turning angle (most negative)
    min_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .angle.min()
        .fillna(math.pi)
        .rename("min_angle")
    )

    # minimum absolute turning angle
    min_abs_angles = (
        traversals[
            traversals.autosPermitted_toEdge & (traversals.start != traversals.end)
        ]
        .groupby(["start", "thru"])
        .absAngle.min()
        .fillna(math.pi)
        .rename("min_abs_angle")
    )

    # number of outbound legs
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
    traversals.loc[~traversals.leg_count.isna(), "turnType"] = TURN_NONE

    # label right turns
    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle <= traversals.min_angle)
        & (traversals.angle.abs() > math.pi / 6),
        "turnType",
    ] = TURN_RIGHT

    # label left turns
    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle >= traversals.max_angle)
        & (traversals.angle.abs() > math.pi / 6),
        "turnType",
    ] = TURN_LEFT

    # more right turns
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
    ] = TURN_RIGHT

    # more left turns
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
    ] = TURN_LEFT

    # reversals by angle
    traversals.loc[
        (traversals.angle < -math.pi * 5 / 6) | (traversals.angle > math.pi * 5 / 6),
        "turnType",
    ] = TURN_REVERSE

    # reversals by start/end node
    traversals.loc[traversals.start == traversals.end, "turnType"] = TURN_REVERSE

    # all centroid connector traversals are NONE type
    traversals.loc[
        traversals.centroid_start | traversals.centroid_thru | traversals.centroid_end,
        "turnType",
    ] = TURN_NONE

    traversals.turnType = traversals.turnType.astype(int)

    # keep track of the number of outgoing turns w/ turn type == none
    # FIXME this should almost certainly get removed
    traversals = traversals.merge(
        (traversals.set_index(["start", "thru"]).turnType == TURN_NONE)
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
        (traversals.set_index(["start", "thru"]).turnType == TURN_RIGHT)
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
        (traversals.set_index(["start", "thru", "end"]).turnType == TURN_NONE)
        .reset_index()
        .groupby(["start", "thru", "end"])
        .sum()
        .rename(columns={"turnType": "dupNoneTurns_toEdge"}),
        left_on=["start", "thru", "end"],
        right_index=True,
        how="left",
    )

    # keep track of how many duplicate traversals have turn type == right
    traversals = traversals.merge(
        (traversals.set_index(["start", "thru", "end"]).turnType == TURN_RIGHT)
        .reset_index()
        .groupby(["start", "thru", "end"])
        .sum()
        .rename(columns={"turnType": "dupRtTurns_toEdge"}),
        left_on=["start", "thru", "end"],
        right_index=True,
        how="left",
    )

    # for replicating the buggy Java implementation
    # if settings.recreate_java_attributes:
    #     traversals, java_cols = recreate_java_attributes(traversals)
    #     traversals, java_attributes = calculate_signalExclRight_alternatives(traversals)
    #     java_cols += java_attributes

    # this is the correct implementation
    traversals["ThruJunction_anyrtvec"] = (
        ~(
            traversals.centroid_start
            | traversals.centroid_thru
            | traversals.centroid_end
        )
        & (traversals.turnType == TURN_NONE)
        & (traversals.rt_turns - traversals.dupRtTurns_toEdge == 0)
    )

    # populate derived traversal attributes

    traversals = traversals.assign(
        thruCentroid=(
            traversals.centroidConnector_fromEdge & traversals.centroidConnector_toEdge
        ),
        # this one is allegedly the one to keep
        # taken from signalExclRight_anyrtvec
        signalExclRight=(
            traversals.signalized
            & (traversals.turnType != TURN_RIGHT)
            & (~traversals.ThruJunction_anyrtvec)
        ),
        # unlfrma: unsignalized left from major arterial
        unlfrma=(
            (~traversals.signalized)
            & (traversals.functionalClass_fromEdge <= 3)
            & (traversals.functionalClass_fromEdge > 0)
            & (traversals.bikeClass_fromEdge != 1)
            & (traversals.turnType == TURN_LEFT)
        ),
        # unlfrmi: unsignalized left from minor arterial
        unlfrmi=(
            (~traversals.signalized)
            & (traversals.functionalClass_fromEdge == 4)
            & (traversals.bikeClass_fromEdge != 1)
            & (traversals.turnType == TURN_LEFT)
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
                    (traversals.turnType == TURN_NONE)
                    & (
                        traversals.majorArtXings
                        - traversals.dupMajArts_fromEdge
                        - traversals.dupMajArts_toEdge
                    )
                    >= 2
                )
                | (
                    (traversals.turnType == TURN_LEFT)
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
                    (traversals.turnType == TURN_NONE)
                    & (
                        traversals.artXings
                        - traversals.dupArts_fromEdge
                        - traversals.dupArts_toEdge
                    )
                    >= 2
                )
                | (
                    (traversals.turnType == TURN_LEFT)
                    & (traversals.functionalClass_toEdge == 4)
                    & (traversals.functionalClass_toEdge > 0)
                    & (traversals.bikeClass_toEdge != 1)
                )
            )
        ),
        roadDowngrade=(
            traversals.functionalClass_fromEdge.isin([3, 4, 5, 6])
            & traversals.functionalClass_toEdge.isin([4, 5, 6, 7])
            & (traversals.functionalClass_fromEdge < traversals.functionalClass_toEdge)
        ),
    )

    output_cols = [
        "start",
        "thru",
        "end",
        "turnType",
        "thruCentroid",
        "signalExclRight",
        "unlfrma",
        "unlfrmi",
        "unxma",
        "unxmi",
        "roadDowngrade",
    ]
    # if settings.recreate_java_attributes:
    #     # include the java attributes if they were recreated
    #     output_cols += java_attributes

    # keep only the relevant columns
    traversals = traversals.set_index(["edgeID_fromEdge", "edgeID_toEdge"])[output_cols]

    logger.info("Finished calculating derived traversal attributes")

    return traversals


def create_bike_net(settings: BikeRouteChoiceSettings, logger: logging.Logger) -> tuple[
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
    if settings.read_cached_bike_net:
        logger.info("Reading cached bike network from CSV files")
        edges = pd.read_csv(
            os.path.join(os.path.expanduser(settings.output_path), "edges.csv"),
            index_col=[0],
        )
        nodes = pd.read_csv(
            os.path.join(os.path.expanduser(settings.output_path), "nodes.csv"),
            index_col=0,
        )
        traversals = pd.read_csv(
            os.path.join(
                os.path.join(os.path.expanduser(settings.output_path), "traversals.csv")
            ),
            index_col=[0, 1],
        )
        return nodes, edges, traversals

    # read shapefiles
    logger.info("Reading link and node shapefiles")
    node = read_file(settings, settings.node_file)
    link = read_file(settings, settings.link_file)

    # create and attribute edges
    edges, nodes = create_and_attribute_edges(settings, node, link, logger)

    # generate authoritiative positional index
    edges = edges.reset_index()
    edges.index.name = "edgeID"

    traversals = create_and_attribute_traversals(settings, edges, nodes, logger)

    # save edges, nodes, and traversals to csv files if specified
    if settings.save_bike_net:
        logger.info("Saving bike network to CSV files")
        edges.to_csv(
            os.path.join(os.path.expanduser(settings.output_path), "edges.csv")
        )
        nodes.to_csv(
            os.path.join(os.path.expanduser(settings.output_path), "nodes.csv")
        )
        traversals.to_csv(
            os.path.join(os.path.expanduser(settings.output_path), "traversals.csv")
        )

    return nodes, edges, traversals
