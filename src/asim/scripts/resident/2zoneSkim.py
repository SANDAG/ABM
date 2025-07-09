import pandas as pd
import pandana as pdna
import numpy as np
import yaml
import os
import geopandas as gpd
from datetime import datetime
from typing import Tuple, Dict, List
from dataclasses import dataclass
import logging

@dataclass
class SkimParameters:
    """Configuration parameters for skim generation read from YAML file"""
    max_maz_maz_walk_dist_feet: int
    max_maz_maz_bike_dist_feet: int
    max_maz_local_bus_stop_walk_dist_feet: int
    max_maz_premium_transit_stop_walk_dist_feet: int
    walk_speed_mph: float
    drive_speed_mph: float
    
    @classmethod
    def from_yaml(cls, yaml_data: dict) -> 'SkimParameters':
        """Create parameters from YAML configuration"""
        mmms = yaml_data['mmms']
        return cls(
            max_maz_maz_walk_dist_feet=int(mmms['max_maz_maz_walk_dist_feet']),
            max_maz_maz_bike_dist_feet=int(mmms['max_maz_maz_bike_dist_feet']),
            max_maz_local_bus_stop_walk_dist_feet=int(mmms['max_maz_local_bus_stop_walk_dist_feet']),
            max_maz_premium_transit_stop_walk_dist_feet=int(mmms['max_maz_premium_transit_stop_walk_dist_feet']),
            walk_speed_mph=float(mmms['walk_speed_mph']),
            drive_speed_mph=float(mmms['drive_speed_mph'])
        )

class NetworkBuilder:
    """
    Handles network construction, and node assignment, and transit stop processing for transportation networks.
    
    This class processes raw node and link data to create a network and processes transit stops. It handles coordinate projections, network topology,
    and centroid connections.
    """
    
    def __init__(self, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, 
                 stops: pd.DataFrame, routes: pd.DataFrame, config: dict):
        """
        Initialize the NetworkBuilder with nodes, links, and configuration.

        Args:
            nodes (gpd.GeoDataFrame): GeoDataFrame containing node geometries and attributes.
                Must include columns for node ID and coordinates.
            links (gpd.GeoDataFrame): GeoDataFrame containing link geometries and attributes.
                Must include columns for from/to nodes and length.
            config (dict): Configuration dictionary containing:
                - mmms (dict): Network construction parameters
                - mmms_link_ref_id (str): Column name for link start node
                - mmms_link_nref_id (str): Column name for link end node
                - mmms_link_len (str): Column name for link length

        Note:
            The network is built immediately upon initialization using _build_network().
        """
        self.nodes = nodes
        self.links = links
        self.stops = stops
        self.routes = routes
        self.config = config
        self.network = self._build_network(nodes, links, config)
    
    @classmethod
    def from_files(cls, model_inputs: str, config: dict) -> 'NetworkBuilder':
        """
        Create a NetworkBuilder instance by reading nodes and links from shapefiles.

        This factory method handles reading and processing the raw input files to create
        a properly configured NetworkBuilder instance.

        Args:
            model_inputs (str): Path to directory containing input shapefiles
            config (dict): Configuration dictionary containing:
                - mmms (dict): File and processing parameters
                - shapefile_node_name (str): Filename for nodes shapefile
                - shapefile_name (str): Filename for links shapefile

        Returns:
            NetworkBuilder: A fully initialized NetworkBuilder instance with processed
                nodes and links.
        """
        # Read and process nodes
        nodes = gpd.read_file(os.path.join(model_inputs, config['mmms']['shapefile_node_name']))
        nodes = cls._process_nodes(nodes)
        
        # Read and process links
        links = gpd.read_file(os.path.join(model_inputs, config['mmms']['shapefile_name']))
        links = cls._process_links(links)
        links = links.reset_index(drop=True)

        # Read and process stops and routes
        stops = pd.read_csv(os.path.join(model_inputs, config['stop_attributes']['file']))
        routes = pd.read_csv(os.path.join(model_inputs, config['route_attributes']['file']))
        routes = routes.filter(['Route_ID', 'Route_Name', 'Mode'])
        stops = stops.merge(routes,  left_on='Route_ID', right_on='Route_ID')
        network = cls._build_network(nodes, links, config)
        stops = cls.process_transit_stops(stops, network, nodes) 
        
        return cls(nodes, links, stops, routes, config)
    
    @staticmethod
    def _process_nodes(nodes: gpd.GeoDataFrame) -> gpd.GeoDataFrame:
        """
        Process raw nodes GeoDataFrame by projecting and adding coordinates.
        Ensures NodeLev_ID is integer for Pandana compatibility.
        """
        nodes = nodes.to_crs(epsg=2230)
        nodes["NodeLev_ID"] = nodes["NodeLev_ID"].astype(np.int64)
        nodes = nodes.set_index("NodeLev_ID")
        nodes['X'] = nodes.geometry.x
        nodes['Y'] = nodes.geometry.y
        return nodes
    
    @staticmethod
    def _process_links(links: gpd.GeoDataFrame) -> gpd.GeoDataFrame:
        """
        Process raw links GeoDataFrame.
        
        Args:
            links: Raw links GeoDataFrame
            
        Returns:
            Processed links GeoDataFrame
        """
        return links.to_crs(epsg=2230)
    
    @classmethod
    def _build_network(cls, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, config: dict) -> pdna.Network:
        """Build pandana network from nodes and links (Pandana 0.7 compatible)"""
        mmms = config['mmms']
        # Ensure links DataFrame is fully reset and arrays are aligned
        links_reset = links.reset_index(drop=True)
        edge_attr = (links_reset[[mmms['mmms_link_len']]] / 5280.0)
        # Ensure X and Y are in the order of node IDs (sorted by index)
        nodes_sorted = nodes.sort_index()
        net = pdna.Network(
            nodes_sorted.index.values.astype(np.int64),  # node IDs
            nodes_sorted.X.values,                       # X coordinates
            nodes_sorted.Y.values,                       # Y coordinates
            links_reset[mmms['mmms_link_ref_id']].astype(np.int64).values,
            links_reset[mmms['mmms_link_nref_id']].astype(np.int64).values,
            edge_attr
        )
        net.set_twoway(True)
        return net
    
    @classmethod
    def get_closest_net_node_to_MGRA(cls, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, 
                         config: dict) -> pd.DataFrame:
        """
        Gets closest network nodes to MAZ centroids. This is used to assign network nodes to MAZ centroids. This assigned nodes is then used to calculate skims.
        
        Args:
            nodes: Processed nodes GeoDataFrame
            links: Processed links GeoDataFrame
            config: Configuration dictionary
            
        Returns:
            DataFrame containing centroid information
        """
        # Get closest network nodes for MAZ centroids, e.g. 'centroid connector' start and end nodes
        maz_closest_network_node_id = cls._get_closest_network_nodes(nodes, links, config)
        
        # Create centroids DataFrame
        centroids = cls._create_centroids_df(nodes, config)
        
        # Merge with closest network nodes. This will add the end node of the connector as the associated network node for the MGRA
        return cls._merge_centroids_with_connector_end_nodes(centroids, maz_closest_network_node_id, 
                                             nodes, config)
    
    @staticmethod
    def _get_closest_network_nodes(nodes: gpd.GeoDataFrame, 
                                 links: gpd.GeoDataFrame,
                                 config: dict) -> pd.DataFrame:
        """Get closest network nodes for MAZs"""
        mmms = config['mmms']
        maz_id = config['maz_shape_maz_id']
        
        # Return the 'centroid connector' start and end nodes, reads as : links[links['A'].isin(list(nodes[nodes['MGRA']!=0].index))][['A','B']] with A and B being the start and end node of a link
        return links[links[mmms["mmms_link_ref_id"]].isin(
            list(nodes[nodes[maz_id]!=0].index)
        )][[mmms["mmms_link_ref_id"], mmms["mmms_link_nref_id"]]]
    
    @staticmethod
    def _create_centroids_df(nodes: gpd.GeoDataFrame, config: dict) -> pd.DataFrame:
        """Create initial centroids DataFrame"""
        maz_nodes = nodes[nodes[config['maz_shape_maz_id']]!=0]
        return pd.DataFrame({
            'X': maz_nodes.X,
            'Y': maz_nodes.Y,
            'MAZ': maz_nodes.MGRA,
            'MAZ_centroid_id': maz_nodes.index
        })
    
    @staticmethod
    def _merge_centroids_with_connector_end_nodes(centroids: pd.DataFrame,
                                  closest_nodes: pd.DataFrame,
                                  nodes: gpd.GeoDataFrame,
                                  config: dict) -> pd.DataFrame:
        """Merge centroids with their closest network nodes at the nd of the connector"""
        mmms = config['mmms']
        
        centroids = pd.merge(
            centroids, 
            closest_nodes, 
            left_on='MAZ_centroid_id',
            right_on=mmms["mmms_link_ref_id"],
            how='left'
        )
        
        centroids = centroids.rename(columns={mmms["mmms_link_nref_id"]: 'network_node_id'})
        
        # Add network node coordinates
        centroids["network_node_x"] = nodes["X"].loc[centroids["network_node_id"]].tolist()
        centroids["network_node_y"] = nodes["Y"].loc[centroids["network_node_id"]].tolist()
        
        return centroids
    
    @staticmethod    
    def _process_stop_geometry(stops: pd.DataFrame) -> gpd.GeoDataFrame:
        """
        Convert stops to GeoDataFrame and project coordinates.

        Args:
            stops (pd.DataFrame): Stops DataFrame with Longitude/Latitude

        Returns:
            gpd.GeoDataFrame: Projected stops with updated coordinates
        """
        pd.set_option('display.float_format', lambda x: '%.9f' % x)
        
        gpd_stops = gpd.GeoDataFrame(
            stops, 
            geometry=gpd.points_from_xy(stops.Longitude, stops.Latitude, crs='epsg:4326')
        )
        gpd_stops = gpd_stops.to_crs('epsg:2230')
        
        gpd_stops['Longitude'] = gpd_stops['geometry'].x
        gpd_stops['Latitude'] = gpd_stops['geometry'].y
        
        return gpd_stops

    @staticmethod
    def _assign_network_nodes_to_stops(
        stops: pd.DataFrame, 
        gpd_stops: gpd.GeoDataFrame,
        net: pdna.Network,
        nodes: pd.DataFrame
    ) -> pd.DataFrame:
        """
        Assign network nodes to stops (Pandana 0.7 compatible).
        """
        # Pandana 0.7 requires return_distances argument
        stops["network_node_id"] = net.get_node_ids(gpd_stops['Longitude'].values, gpd_stops['Latitude'].values, return_distances=False)
        stops["network_node_x"] = nodes["X"].loc[stops["network_node_id"]].tolist()
        stops["network_node_y"] = nodes["Y"].loc[stops["network_node_id"]].tolist()
        return stops
    
    @staticmethod
    def _assign_transit_modes(stops: pd.DataFrame) -> pd.DataFrame:
        """
        Assign simplified transit modes to stops.
        
        Modes:
        - L: Local bus (Mode 10)
        - E: Premium (Modes 4-9)
        - N: None (should not occur)

        Args:
            stops (pd.DataFrame): Stops DataFrame with Mode column

        Returns:
            pd.DataFrame: Stops with assigned simplified modes
        """
        prm_modes = [4, 5, 6, 7, 8, 9]
        stops['mode'] = np.where(
            stops['Mode'] == 10, 'L',
            np.where(stops['Mode'].isin(prm_modes), 'E', 'N')
        )
        return stops

    @classmethod
    def process_transit_stops(cls, stops: pd.DataFrame, network: pdna.Network, nodes: pd.DataFrame) -> pd.DataFrame:
        """Process transit stops using provided network."""
        gpd_stops = cls._process_stop_geometry(stops)
        stops = cls._assign_network_nodes_to_stops(stops, gpd_stops, network, nodes)
        stops = cls._assign_transit_modes(stops)
        return stops

class SkimGenerator:
    """Main class for generating walk, bike, and stop skims"""
    
    def __init__(self, network_builder: NetworkBuilder, params: SkimParameters, output_path: str):
        self.network_builder = network_builder
        self.params = params
        self.output_path = output_path
        self.net_centroids = self._get_closest_net_node(network_builder.nodes, network_builder.links, network_builder.config)
        self.stops = self.network_builder.stops
        
    def _get_closest_net_node(self, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, config: dict) -> pd.DataFrame:
        """Get centroids DataFrame"""
        return self.network_builder.get_closest_net_node_to_MGRA(nodes, links, config)

    def generate_maz_maz_walk_skim(self) -> pd.DataFrame:
        """Generate MAZ to MAZ walk skims"""
        maz_pairs = self._create_maz_pairs(self.net_centroids)
        walk_skim = self._get_walk_distances(maz_pairs, self.params.max_maz_maz_walk_dist_feet)
        # Add intrazonal distances
        walk_skim = self._add_intrazonal_distances(walk_skim)
        walk_skim = self._convert_columns_to_type(walk_skim, {'OMAZ': 'uint16', 'DMAZ': 'uint16', 'i': 'uint16', 'j': 'uint16'})
        return walk_skim
        
    def generate_maz_maz_bike_skim(self) -> pd.DataFrame:
        """Generate MAZ to MAZ bike skims"""
        maz_pairs = self._create_maz_pairs(self.net_centroids)
        bike_skim = self._get_bike_distances(maz_pairs, self.params.max_maz_maz_bike_dist_feet)
        bike_skim = self._convert_columns_to_type(bike_skim, {'OMAZ': 'uint16', 'DMAZ': 'uint16'})
        return bike_skim
        
    def generate_maz_stop_walk_skim(self) -> pd.DataFrame:
        """Generate MAZ to stop walk skims"""
        maz_stop_pairs, maz_stop_output = self._create_maz_stop_pairs(self.net_centroids, self.stops)
        stop_skim = self._get_stop_distances(maz_stop_pairs)
        stop_skim = self._process_stop_skims_by_mode(stop_skim, maz_stop_output)
                    
        return stop_skim.sort_values('maz')
    
    def _create_maz_pairs(self, centroids: pd.DataFrame) -> pd.DataFrame:
        """Create all possible MAZ to MAZ pairs with their network nodes"""
        o_m = np.repeat(centroids['MAZ'].tolist(), len(centroids))
        d_m = np.tile(centroids['MAZ'].tolist(), len(centroids))
        
        pairs = pd.DataFrame({
            "OMAZ": o_m,
            "DMAZ": d_m,
            "OMAZ_NODE": np.repeat(centroids['network_node_id'].tolist(), len(centroids)),
            "DMAZ_NODE": np.tile(centroids['network_node_id'].tolist(), len(centroids)),
            "OMAZ_NODE_X": np.repeat(centroids['network_node_x'].tolist(), len(centroids)),
            "OMAZ_NODE_Y": np.repeat(centroids['network_node_y'].tolist(), len(centroids)),
            "DMAZ_NODE_X": np.tile(centroids['network_node_x'].tolist(), len(centroids)),
            "DMAZ_NODE_Y": np.tile(centroids['network_node_y'].tolist(), len(centroids))
        })
        
        pairs["DISTWALK"] = pairs.eval("(((OMAZ_NODE_X-DMAZ_NODE_X)**2 + (OMAZ_NODE_Y-DMAZ_NODE_Y)**2)**0.5) / 5280.0")
        return pairs[pairs["OMAZ"] != pairs["DMAZ"]]

    def _create_maz_stop_pairs(self, centroids: pd.DataFrame, stops: pd.DataFrame, ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        Build a table of MAZ to transit stop connections with initial distances.
        
        Creates a cross-join between MAZs and stops, calculating the straight-line
        distance between each MAZ-stop pair. Distances are converted to miles.

  
        """
        # Create cross product of MAZs and stops
        o_m = np.repeat(centroids['MAZ'].tolist(), len(stops))
        o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(stops))
        d_t = np.tile(stops['Stop_ID'].tolist(), len(centroids))
        d_t_nn = np.tile(stops['network_node_id'].tolist(), len(centroids))
        o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(stops))
        o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(stops))
        d_t_x = np.tile(stops['network_node_x'].tolist(), len(centroids))
        d_t_y = np.tile(stops['network_node_y'].tolist(), len(centroids))
        mode = np.tile(stops['mode'].tolist(), len(centroids))

        # Create DataFrame
        pairs = pd.DataFrame({
            "MAZ": o_m,
            "stop": d_t,
            "OMAZ_NODE": o_m_nn,
            "DSTOP_NODE": d_t_nn,
            "OMAZ_NODE_X": o_m_x,
            "OMAZ_NODE_Y": o_m_y,
            "DSTOP_NODE_X": d_t_x,
            "DSTOP_NODE_Y": d_t_y,
            "MODE": mode
        })

        # Calculate distances in miles
        pairs["DISTANCE"] = pairs.eval(
            "(((OMAZ_NODE_X-DSTOP_NODE_X)**2 + (OMAZ_NODE_Y-DSTOP_NODE_Y)**2)**0.5) / 5280.0"
        )

        #create stop distances output file
        maz_stop_output = pd.DataFrame(centroids['MAZ'])
        maz_stop_output = maz_stop_output.rename(columns = {'MAZ': 'maz'})
        maz_stop_output['maz'] = maz_stop_output['maz'].astype('int')
        maz_stop_output.sort_values(by=['maz'], inplace=True)

        return pairs, maz_stop_output

    def _get_walk_distances(self, pairs: pd.DataFrame, max_dist: float) -> pd.DataFrame:
        """Process walking distances between MAZ pairs (Pandana 0.7 compatible)"""
        filtered = pairs[pairs["DISTWALK"] <= max_dist / 5280.0].copy()
        # Pandana 0.7: shortest_path_lengths requires impedance argument and numpy arrays
        filtered["DISTWALK"] = self.network_builder.network.shortest_path_lengths(
            filtered["OMAZ_NODE"].values, filtered["DMAZ_NODE"].values, impedance=None
        )
        result = filtered[filtered["DISTWALK"] <= max_dist / 5280.0]

        # Add missing MAZs
        result = self._add_missing_mazs(self.net_centroids, result, pairs, 'DISTWALK')
        # Add required fields for TNC routing
        result[['i', 'j']] = result[['OMAZ', 'DMAZ']]
        result['actual'] = result['DISTWALK'] / self.params.walk_speed_mph * 60.0
        return result
    
    def _get_bike_distances(self, pairs: pd.DataFrame, max_dist: float) -> pd.DataFrame:
        """Process bike distances between MAZ pairs (Pandana 0.7 compatible)"""
        filtered = pairs[pairs["DISTWALK"] <= max_dist / 5280.0].copy()
        filtered["DISTBIKE"] = self.network_builder.network.shortest_path_lengths(
            filtered["OMAZ_NODE"].values, filtered["DMAZ_NODE"].values, impedance=None
        )
        result = filtered[filtered["DISTBIKE"] <= max_dist / 5280.0]
        # Add missing MAZs
        result = self._add_missing_mazs(self.net_centroids, result, pairs, 'DISTBIKE')
        return result
    
    def _get_stop_distances(self, pairs: pd.DataFrame) -> pd.DataFrame:
        """Process stop distances between MAZ pairs (Pandana 0.7 compatible)"""
        filtered = pairs[(pairs["DISTANCE"] <= self.params.max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (pairs['MODE'] == 'L') |
                         (pairs["DISTANCE"] <= self.params.max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (pairs['MODE'] == 'E')].copy()
        filtered["DISTWALK"] = self.network_builder.network.shortest_path_lengths(
            filtered["OMAZ_NODE"].values, filtered["DSTOP_NODE"].values, impedance=None
        )
        result = filtered[(filtered["DISTWALK"] <= self.params.max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (filtered['MODE'] == 'L') |
                         (filtered["DISTWALK"] <= self.params.max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (filtered['MODE'] == 'E')]
        return result
    
    def _process_stop_skims_by_mode(self, stop_skims: pd.DataFrame, maz_stop_output: pd.DataFrame) -> pd.DataFrame:
        """
        Process stop skims by transit mode and merge with output DataFrame.

        Args:
            stop_skims (pd.DataFrame): DataFrame containing stop skim data with columns:
                - MODE: Transit mode ('L' or 'E')
                - MAZ: MAZ ID
                - DISTWALK: Walk distance
            maz_stop_output (pd.DataFrame): Base DataFrame to merge results into

        Returns:
            pd.DataFrame: Processed DataFrame with walk distances by mode:
                - maz: MAZ ID
                - walk_dist_local_bus: Walk distance to nearest local bus stop
                - walk_dist_premium_transit: Walk distance to nearest premium transit stop
        """
        modes = {"L": "local_bus", "E": "premium_transit"}
        
        for mode, mode_descr in modes.items():
            stop_skims_by_mode = (
                stop_skims[stop_skims.MODE == mode]
                .groupby("MAZ")["DISTWALK"]
                .min()
                .reset_index()
            )

            stop_skims_by_mode = stop_skims_by_mode.rename(
                {
                    "MAZ": "maz",
                    "DISTWALK": f"walk_dist_{mode_descr}",
                },
                axis="columns",
            )
            
            maz_stop_output = maz_stop_output.merge(
                stop_skims_by_mode, 
                on="maz", 
                how="outer"
            )
            maz_stop_output[f"walk_dist_{mode_descr}"].fillna(999999, inplace=True)

        return maz_stop_output

    def _add_intrazonal_distances(self, skim: pd.DataFrame) -> pd.DataFrame:
        """Add intrazonal distances based on 3 nearest neighbors"""
        skim = skim.sort_values(['OMAZ', 'DISTWALK'])
        skim.set_index(['OMAZ', 'DMAZ'], inplace=True)
        unique_omaz = skim.index.get_level_values(0).unique()
        # find the average of the closest 3 zones
        means = skim.loc[(unique_omaz, slice(None)), 'DISTWALK'].groupby(level=0).head(3).groupby(level=0).mean()
        intra_skims = pd.DataFrame({
            'OMAZ': unique_omaz,
            'DMAZ': unique_omaz,
            'DISTWALK': means.values/2,
            'i': unique_omaz,
            'j': unique_omaz,
            'actual': (means.values/self.params.walk_speed_mph * 60.0) / 2
        }).set_index(['OMAZ', 'DMAZ'])
        
        return pd.concat([skim, intra_skims], axis=0).reset_index()

    def _add_missing_mazs(self, centroids: pd.DataFrame, skim_table: pd.DataFrame, 
                         cost_table: pd.DataFrame, dist_col: str = 'DISTWALK') -> pd.DataFrame:
        """Add missing MAZs to skim table since some MAZs will not be within distance of a stop or each other.
        This will make sure we will have skims for all MAZs in the region."""
        missing_maz = centroids[~centroids['MAZ'].isin(skim_table['OMAZ'])][['MAZ']]
        missing_maz = missing_maz.rename(columns={'MAZ': 'OMAZ'})
        
        filtered_cost = cost_table[cost_table['OMAZ'] != cost_table['DMAZ']]
        if dist_col != 'DISTWALK':
            filtered_cost = filtered_cost.rename(columns={'DISTWALK': dist_col})
        sorted_cost = filtered_cost.sort_values(dist_col)
        grouped_cost = sorted_cost.groupby('OMAZ').agg({
            'DMAZ': 'first',
            dist_col: 'first'
        }).reset_index()

        missing_maz = missing_maz.merge(grouped_cost, on='OMAZ', how='left')   
        skim_table = pd.concat([skim_table[["OMAZ","DMAZ", dist_col]], missing_maz]).sort_values(['OMAZ', 'DMAZ'])

        return skim_table
    
    def _convert_columns_to_type(self, df: pd.DataFrame, columns: Dict[str, str]) -> pd.DataFrame:
        """
        Convert specified columns in a DataFrame to a given data type.

        Args:
            df (pd.DataFrame): The DataFrame to convert.
            columns (Dict[str, str]): A dictionary where keys are column names and values are the target data types.

        Returns:
            pd.DataFrame: The DataFrame with converted columns.
        """
        return df.astype(columns)

def main(path: str):
    """Main execution function"""
    # Load configuration
    with open(os.path.join(path, "src/asim/scripts/resident/2zoneSkim_params.yaml"), 'r') as f:
        config = yaml.safe_load(f)
    
    params = SkimParameters.from_yaml(config)
    model_inputs = os.path.join(path, "input")
    output_path = os.path.join(path, "output/skims")
    
    # Create network builder using class method
    network_builder = NetworkBuilder.from_files(model_inputs, config)
    
    # Initialize skim generator
    skim_generator = SkimGenerator(network_builder, params, output_path)
    
    # Generate and save skims
    print(f"{datetime.now().strftime('%H:%M:%S')} Generating MAZ-MAZ walk skims...")
    walk_skim = skim_generator.generate_maz_maz_walk_skim()
    walk_skim.to_csv(os.path.join(output_path, config['mmms']['maz_maz_walk_output']), index=False)
    
    print(f"{datetime.now().strftime('%H:%M:%S')} Generating MAZ-MAZ bike skims...")
    bike_skim = skim_generator.generate_maz_maz_bike_skim()
    bike_skim.to_csv(os.path.join(output_path, config['mmms']['maz_maz_bike_output']), index=False)
    
    print(f"{datetime.now().strftime('%H:%M:%S')} Generating MAZ-stop walk skims...")
    stop_skim = skim_generator.generate_maz_stop_walk_skim()
    stop_skim.to_csv(os.path.join(output_path, "maz_stop_walk.csv"), index=False)

if __name__ == "__main__":
    import sys
    main(sys.argv[1])