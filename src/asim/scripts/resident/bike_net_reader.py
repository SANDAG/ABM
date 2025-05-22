import geopandas as gpd
import pandas as pd
import math

# default shapefile sources
node_file = "SANDAG_Bike_Node.shp"
link_file = "SANDAG_Bike_Net.shp"

# turn type encodings
turn_none = 0
turn_left = 1
turn_right = 2
turn_reverse = 3


def calc_edge_angle(start_x:float,start_y:float,end_x:float,end_y:float) -> float:
    """
    Calculate the heading of an edge from its start x and y coordinates
    """

    deltax = end_x - start_x
    deltay = end_y - start_y

    return math.atan2(deltay, deltax)

def read_bike_net(
        node_file: str, # path to node shapefile
        link_file: str, # path to edge shapefile
    ) -> tuple[
        pd.DataFrame,   # node dataframe
        pd.DataFrame,   # edge dataframe
        pd.DataFrame    # traversal dataframe
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
    node = gpd.read_file(node_file)
    link = gpd.read_file(link_file)

    # create directional edge dataframe from links
    abEdges = link.rename(columns={
            "A":"fromNode",
            "B":"toNode",
            "AB_Gain":"gain",
            "ABBikeClas":"bikeClass",
            "AB_Lanes":"lanes",
            "Func_Class":"functionalClass",
            "Bike2Sep":"cycleTrack",
            "Bike3Blvd":"bikeBlvd",
        }).copy().assign(
            distance=link.Shape_Leng/5280.0,
            autosPermitted=link.Func_Class.isin(range(1,8)),
            centroidConnector=link.Func_Class==10,
        )[[
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
            "gain"
        ]]
    
    # create reverse direction edges
    baEdges = link.rename(columns={
            "B":"fromNode",
            "A":"toNode",
            "BA_Gain":"gain",
            "BABikeClas":"bikeClass",
            "BA_Lanes":"lanes",
            "Func_Class":"functionalClass",
            "Bike2Sep":"cycleTrack",
            "Bike3Blvd":"bikeBlvd",
        }).copy().assign(
            distance=link.Shape_Leng/5280.0,
            autosPermitted=link.Func_Class.isin(range(1,8)),
            centroidConnector=link.Func_Class==10,
        )[[
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
            "gain"
        ]]
    
    # combine bidirectional edges
    edges = pd.concat([abEdges,baEdges]).set_index(['fromNode','toNode'])
    
    # manipulate node dataframe to output format
    nodes = node.assign(
        centroid=(node.MGRA>0)|(node.TAZ>0)
    ).rename(columns={
        "NodeLev_ID":"id",
        "XCOORD":"x",
        "YCOORD":"y",
        "MGRA":"mgra",
        "TAZ":"taz",
        "Signal":"signalized"
    }).drop(columns=["ZCOORD","geometry"]).set_index('id')
    
    # calculate edge headings
    edges = edges.assign(angle=edges.reset_index().merge(nodes,
                left_on='fromNode',
                right_index=True
        ).merge(nodes,
                left_on='toNode',
                right_index=True,
                suffixes=('_from','_to')
            ).set_index(
                    ['fromNode','toNode']
            ).apply(lambda edge:
                    calc_edge_angle(edge.x_from,edge.y_from, edge.x_to, edge.y_to),
                    axis='columns'
            )
    )

    # attribute (major) arterial status
    edges["majorArterial"] = (edges.functionalClass <= 3) & (edges.functionalClass > 0) & (edges.bikeClass != 1)
    edges["arterial"] = (edges.functionalClass <= 4) & (edges.functionalClass > 0) & (edges.bikeClass != 1)
    
    # keep track of how many duplicate edges are (major) arterials
    dupMajArts = edges.groupby(edges.index).majorArterial.sum()
    dupMajArts.index = pd.MultiIndex.from_tuples(dupMajArts.index)
    edges['dupMajArts'] = dupMajArts.reindex(edges.index)

    dupArts = edges.groupby(edges.index).arterial.sum()
    dupArts.index = pd.MultiIndex.from_tuples(dupArts.index)
    edges['dupArts'] = dupArts.reindex(edges.index)

    # keep track of how many edges emanating from a node cross (major) arterials
    nodes['majorArtXings'] = edges.groupby(edges.index.get_level_values(0)).majorArterial.sum().reindex(nodes.index)
    nodes['artXings'] = edges.groupby(edges.index.get_level_values(0)).arterial.sum().reindex(nodes.index)

    # create initial traversal table from edges
    traversals = edges.reset_index().merge(edges.reset_index(),
                left_on="toNode",
                right_on="fromNode",
                suffixes=["_fromEdge","_toEdge"]
        ).rename(columns={
            "fromNode_fromEdge": "start",
            "toNode_fromEdge": "thru",
            "toNode_toEdge": "end",
        }).merge(nodes,
                left_on="thru",
                right_index=True)
    
    # drop U-turns
    traversals = traversals[traversals.start != traversals.end]
    
    # calculate traversal angles
    traversals['angle'] = (traversals.angle_toEdge - traversals.angle_fromEdge)
    traversals.loc[traversals.angle > math.pi, 'angle'] = traversals.loc[traversals.angle > math.pi, 'angle'] - 2*math.pi
    traversals.loc[traversals.angle < -math.pi, 'angle'] = traversals.loc[traversals.angle > math.pi, 'angle'] + 2*math.pi
    traversals.set_index(['start','thru','end'])

    # keep track of the absolute value of the traversal angle
    traversals['absAngle'] = traversals.angle.abs()
    
    # attach component nodes' centroid statuses
    traversals = traversals.merge(
            nodes.centroid,
            left_on='start',
            right_index=True,
            suffixes=("_thru","_start")
        ).merge(nodes.centroid,
            left_on='end',
            right_index=True,
        ).rename(columns={"centroid":"centroid_end"})
    
    # calculate traversal angle attributes for thru node
    max_angles = traversals[
            traversals.autosPermitted_toEdge 
            & (traversals.start != traversals.end)
        ].groupby(
            ['start','thru']
        ).angle.max().fillna(
            -math.pi
        ).rename('max_angle')
    
    min_angles = traversals[
            traversals.autosPermitted_toEdge 
            & (traversals.start != traversals.end)
        ].groupby(
            ['start','thru']
        ).angle.min().fillna(
            math.pi
        ).rename('min_angle')
    
    min_abs_angles = traversals[
            traversals.autosPermitted_toEdge 
            & (traversals.start != traversals.end)
        ].groupby(
            ['start','thru']
        ).absAngle.min().fillna(
            math.pi
        ).rename('min_abs_angle')
    
    leg_count = traversals[
            traversals.autosPermitted_toEdge 
            & (traversals.start != traversals.end)
        ].groupby(
            ['start','thru']
        ).size().rename('leg_count') + 1
    
    # consolidate into a dataframe for merging and reindex to full traversal table
    turn_atts = pd.concat(
            [
                max_angles, 
                min_angles,
                min_abs_angles, 
                leg_count,
            ],
            axis=1
        ).reindex(
            [traversals.start,traversals.thru]
        ).fillna(
            {
                "max_angle" : -math.pi,
                "min_angle": math.pi,
                "min_abs_angle": math.pi,
                "leg_count" : 1
            }
        )
    
    # attach to full table
    traversals = pd.concat(
            [
                traversals.set_index(['start','thru']),
                turn_atts
            ],
            axis=1
        ).reset_index()
    
    # calculate turn type
    traversals.loc[~traversals.leg_count.isna(),'turnType'] = turn_none

    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle <= traversals.min_angle)
        & (traversals.angle.abs() > math.pi/6),
        "turnType"
        ] = turn_right

    traversals.loc[
        (traversals.leg_count == 3)
        & (traversals.angle >= traversals.max_angle)
        & (traversals.angle.abs() > math.pi/6),
        "turnType"
        ] = turn_left

    traversals.loc[
        (traversals.leg_count > 3)
        & (traversals.angle.abs() > traversals.min_abs_angle)
        & (
            (traversals.angle.abs() >= math.pi/6)
            | (traversals.angle <= traversals.min_angle)
            | (traversals.angle >= traversals.max_angle)
        )
        & (traversals.angle < 0),
        "turnType"
    ] = turn_right

    traversals.loc[
        (traversals.leg_count > 3)
        & (traversals.angle.abs() > traversals.min_abs_angle)
        & (
            (traversals.angle.abs() >= math.pi/6)
            | (traversals.angle <= traversals.min_angle)
            | (traversals.angle >= traversals.max_angle)
        )
        & (traversals.angle >= 0),
        "turnType"
    ] = turn_left
    
    traversals.loc[
        (traversals.angle < -math.pi*5/6) 
        | (traversals.angle > math.pi*5/6),
        "turnType"
    ] = turn_reverse

    traversals.loc[
        traversals.start == traversals.end,
        "turnType"
    ] = turn_reverse

    traversals.loc[
        traversals.centroid_start | traversals.centroid_thru | traversals.centroid_end,
        "turnType"
    ] = turn_none

    # keep track of the number of outgoing turns w/ turn type == none
    traversals = traversals.merge(
        (
            traversals.set_index(['start','thru']).turnType == turn_none
        ).reset_index().groupby(
            [
                'start','thru'
        ]).sum().rename(
            columns={'turnType':'none_turns'}
            ),
        left_on=['start','thru'],
        right_index=True,
        how='left') 
    
    # keep track of how many duplicate traversals have turn type == none
    traversals = traversals.merge(
        (
            traversals.set_index(['start','thru','end']).turnType == turn_none
        ).reset_index().groupby(
            ['start','thru','end']
        ).sum().rename(
            columns={'turnType':'dupNoneTurns_toEdge'}
        ),
        left_on=['start','thru','end'],
        right_index=True,
        how='left')

    # populate derived traversal attributes
    traversals = traversals.assign(
        thruCentroid = traversals.centroidConnector_fromEdge & traversals.centroidConnector_toEdge,
        signalExclRight = (
            traversals.signalized 
            & (traversals.turnType != turn_right)
            & (
                traversals.centroid_start 
                | traversals.centroid_thru 
                | traversals.centroid_end 
                | (traversals.turnType != turn_none)
            )
            # FIXME: this doesn't include isThruJunction method yet
        ),

        # unlfrma: unsignalized left from major arterial
        unlfrma = (
            (~traversals.signalized) 
            & (traversals.functionalClass_fromEdge <= 3)
            & (traversals.functionalClass_fromEdge > 0)
            & (traversals.bikeClass_fromEdge !=1)
            & (traversals.turnType == turn_left)
        ),

        # unlfrmi: unsignalized left from minor arterial
        unlfrmi = (
            (~traversals.signalized)
            & (traversals.functionalClass_fromEdge == 4)
            & (traversals.bikeClass_fromEdge != 1)
            & (traversals.turnType == turn_left)
        ),

        # unxma: unsignalized cross major arterial
        unxma = (
            ~(traversals.centroid_start | traversals.centroid_thru | traversals.centroid_end)
            & (
                (   
                    (traversals.turnType == turn_none)
                    & ( traversals.majorArtXings 
                        - traversals.dupMajArts_fromEdge 
                        - traversals.dupMajArts_toEdge
                        ) >= 2
                ) 
                | (
                    (traversals.turnType==turn_left)
                    & (traversals.functionalClass_toEdge <= 3)
                    & (traversals.functionalClass_toEdge > 0)
                    & (traversals.bikeClass_toEdge != 1)
                )
            )
        ),

        # unxmi: unsignalized cross minor arterial
        unxmi = (
            ~(traversals.centroid_start | traversals.centroid_thru | traversals.centroid_end)
            & (
                ( 
                    (traversals.turnType == turn_none)
                    & ( traversals.artXings 
                        - traversals.dupArts_fromEdge 
                        - traversals.dupArts_toEdge
                        ) >= 2
                ) 
                | (
                    (traversals.turnType==turn_left)
                    & (traversals.functionalClass_toEdge == 4)
                    & (traversals.functionalClass_toEdge > 0)
                    & (traversals.bikeClass_toEdge != 1)
                )
            )
        )

        
    ).set_index(['start','thru','end'])[
        [
            'turnType',
            'thruCentroid',
            'signalExclRight',
            'unlfrma',
            'unlfrmi',
            'unxma',
            'unxmi'
        ]
    ].astype({'turnType':int})

    return nodes, edges, traversals

if __name__ == "__main__":
    nodes, edges, traversals = read_bike_net(node_file, link_file)