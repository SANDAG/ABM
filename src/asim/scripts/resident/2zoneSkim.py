import pandas as pd
import pandana as pdna
import numpy as np
import yaml
import os
import geopandas as gpd
from datetime import datetime
from typing import Tuple, Dict, List
from dataclasses import dataclass

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
    """Handles network construction and node assignment"""
    
    def __init__(self, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, config: dict):
        self.nodes = nodes
        self.links = links
        self.config = config
        self.network = self._build_network()
    
    @classmethod
    def from_files(cls, model_inputs: str, config: dict) -> 'NetworkBuilder':
        """
        Create NetworkBuilder instance from files.
        
        Args:
            model_inputs: Path to input directory
            config: Configuration dictionary
            
        Returns:
            NetworkBuilder instance
        """
        # Read and process nodes
        nodes = gpd.read_file(os.path.join(model_inputs, config['mmms']['shapefile_node_name']))
        nodes = cls._process_nodes(nodes)
        
        # Read and process links
        links = gpd.read_file(os.path.join(model_inputs, config['mmms']['shapefile_name']))
        links = cls._process_links(links)
        
        return cls(nodes, links, config)
    
    @staticmethod
    def _process_nodes(nodes: gpd.GeoDataFrame) -> gpd.GeoDataFrame:
        """
        Process raw nodes GeoDataFrame.
        
        Args:
            nodes: Raw nodes GeoDataFrame
            
        Returns:
            Processed nodes GeoDataFrame
        """
        nodes = nodes.to_crs(epsg=2230).set_index("NodeLev_ID")
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
    
    def _build_network(self) -> pdna.Network:
        """Build pandana network from nodes and links"""
        mmms = self.config['mmms']
        return pdna.Network(
            self.nodes.X,
            self.nodes.Y,
            self.links[mmms['mmms_link_ref_id']],
            self.links[mmms['mmms_link_nref_id']],
            self.links[[mmms['mmms_link_len']]] / 5280.0,
            twoway=True
        )
    
    @classmethod
    def process_centroids(cls, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, 
                         config: dict) -> pd.DataFrame:
        """
        Process centroids from nodes and links.
        
        Args:
            nodes: Processed nodes GeoDataFrame
            links: Processed links GeoDataFrame
            config: Configuration dictionary
            
        Returns:
            DataFrame containing centroid information
        """
        # Get closest network nodes for MAZs
        maz_closest_network_node_id = cls._get_closest_network_nodes(nodes, links, config)
        
        # Create centroids DataFrame
        centroids = cls._create_centroids_df(nodes, config)
        
        # Merge with closest network nodes
        return cls._merge_centroids_with_nodes(centroids, maz_closest_network_node_id, 
                                             nodes, config)
    
    @staticmethod
    def _get_closest_network_nodes(nodes: gpd.GeoDataFrame, 
                                 links: gpd.GeoDataFrame,
                                 config: dict) -> pd.DataFrame:
        """Get closest network nodes for MAZs"""
        mmms = config['mmms']
        maz_id = config['maz_shape_maz_id']
        
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
    def _merge_centroids_with_nodes(centroids: pd.DataFrame,
                                  closest_nodes: pd.DataFrame,
                                  nodes: gpd.GeoDataFrame,
                                  config: dict) -> pd.DataFrame:
        """Merge centroids with their closest network nodes"""
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
        
    def assign_network_nodes(self, df: pd.DataFrame, x_col: str, y_col: str) -> pd.Series:
        """
        Assign nearest network nodes to points.
        
        Args:
            df: DataFrame containing point coordinates
            x_col: Name of X coordinate column
            y_col: Name of Y coordinate column
            
        Returns:
            Series of nearest node IDs
        """
        return self.network.get_node_ids(df[x_col], df[y_col])

class SkimGenerator:
    """Main class for generating various types of skims"""
    
    def __init__(self, network_builder: NetworkBuilder, params: SkimParameters, output_path: str):
        self.network_builder = network_builder
        self.params = params
        self.output_path = output_path
        self.centroids = self._get_centroids(network_builder.nodes, network_builder.links, network_builder.config)
        
    def _get_centroids(self, nodes: gpd.GeoDataFrame, links: gpd.GeoDataFrame, config: dict) -> pd.DataFrame:
        """Get centroids DataFrame"""
        return NetworkBuilder.process_centroids(nodes, links, config)

    def _add_missing_mazs(self, centroids: pd.DataFrame, skim_table: pd.DataFrame, 
                         cost_table: pd.DataFrame, dist_col: str = 'DISTWALK') -> pd.DataFrame:
        """Add missing MAZs to skim table with their nearest neighbors"""
        missing_maz = centroids[~centroids['MAZ'].isin(skim_table['OMAZ'])][['MAZ']]
        missing_maz = missing_maz.rename(columns={'MAZ': 'OMAZ'})
        
        filtered_cost = cost_table[cost_table['OMAZ'] != cost_table['DMAZ']]
        sorted_cost = filtered_cost.sort_values(dist_col)
        grouped_cost = sorted_cost.groupby('OMAZ').agg({
            'DMAZ': 'first',
            dist_col: 'first'
        }).reset_index()
        
        return missing_maz.merge(grouped_cost, on='OMAZ', how='left')

    def generate_maz_maz_walk_skim(self) -> pd.DataFrame:
        """Generate MAZ to MAZ walk skims"""
        maz_pairs = self._create_maz_pairs(self.centroids)
        walk_skim = self._process_walk_distances(maz_pairs, self.params.max_maz_maz_walk_dist_feet)
        
        # Add intrazonal distances
        walk_skim = self._add_intrazonal_distances(walk_skim)
        return walk_skim
        
    def generate_maz_maz_bike_skim(self) -> pd.DataFrame:
        """Generate MAZ to MAZ bike skims"""
        maz_pairs = self._create_maz_pairs(self.centroids)
        bike_skim = self._process_bike_distances(maz_pairs, self.params.max_maz_maz_bike_dist_feet)
        return bike_skim
        
    def generate_maz_stop_walk_skim(self, stops: pd.DataFrame) -> pd.DataFrame:
        """Generate MAZ to stop walk skims"""
        maz_stop_pairs = self._create_maz_stop_pairs(self.centroids, stops)
        modes = {"L": "local_bus", "E": "premium_transit"}
        
        result = pd.DataFrame({'maz': self.centroids['MAZ']}).astype({'maz': 'int'}).sort_values('maz')
        
        for mode, output in modes.items():
            max_dist = getattr(self.params, f'max_maz_{output}_stop_walk_dist_feet') / 5280.0
            mode_skim = self._process_stop_distances(maz_stop_pairs, mode, max_dist)
            result = self._merge_stop_distances(result, mode_skim, output)
            
        return result.sort_values('maz')
    
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

    def _process_walk_distances(self, pairs: pd.DataFrame, max_dist: float) -> pd.DataFrame:
        """Process walking distances between MAZ pairs"""
        filtered = pairs[pairs["DISTWALK"] <= max_dist / 5280.0].copy()
        filtered["DISTWALK"] = self.network_builder.network.shortest_path_lengths(
            filtered["OMAZ_NODE"], filtered["DMAZ_NODE"])
        result = filtered[filtered["DISTWALK"] <= max_dist / 5280.0]
        
        # Add required fields for TNC routing
        result[['i', 'j']] = result[['OMAZ', 'DMAZ']]
        result['actual'] = result['DISTWALK'] / self.params.walk_speed_mph * 60.0
        return result

    def _add_intrazonal_distances(self, skim: pd.DataFrame) -> pd.DataFrame:
        """Add intrazonal distances based on nearest neighbors"""
        skim = skim.sort_values(['OMAZ', 'DISTWALK'])
        skim.set_index(['OMAZ', 'DMAZ'], inplace=True)
        unique_omaz = skim.index.get_level_values(0).unique()
        
        means = skim.loc[(unique_omaz, slice(None)), 'DISTWALK'].groupby(level=0).head(3).groupby(level=0).mean()
        intra_skims = pd.DataFrame({
            'OMAZ': unique_omaz,
            'DMAZ': unique_omaz,
            'DISTWALK': means.values/2,
            'i': unique_omaz,
            'j': unique_omaz,
            'actual': (means.values/self.params.walk_speed_mph * 60.0) / 2
        }).set_index(['OMAZ', 'DMAZ'])
        
        return pd.concat([skim, intra_skims], axis=0)


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
    walk_skim.to_csv(os.path.join(output_path, config['mmms']['maz_maz_walk_output']))
    
    print(f"{datetime.now().strftime('%H:%M:%S')} Generating MAZ-MAZ bike skims...")
    bike_skim = skim_generator.generate_maz_maz_bike_skim()
    bike_skim.to_csv(os.path.join(output_path, config['mmms']['maz_maz_bike_output']), index=False)
    
    print(f"{datetime.now().strftime('%H:%M:%S')} Generating MAZ-stop walk skims...")
    stop_skim = skim_generator.generate_maz_stop_walk_skim(stops)
    stop_skim.to_csv(os.path.join(output_path, "maz_stop_walk.csv"), index=False)

if __name__ == "__main__":
    import sys
    main(sys.argv[1])