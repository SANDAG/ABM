import os
import pandas as pd
import geopandas as gpd
import yaml


class Base:
    
    reduced_parking_df = None
    imputed_parking_df = None
    districts_df = None    
    estimated_spaces_df = None
    districts_dict = None
    combined_df = None
    
    def __init__(self):
        with open("settings.yaml", "r") as stream:
            try:
                self.settings = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                print(exc)

        assert self.settings.get("models")

        # Set defaults
        default_settings = {
            "space_estimation_method": "lm",
            "cache_dir": "./cache",
            "input_dir": "./data",
            "output_dir": "./output",
            "walk_dist": 0.5,
            "walk_coef": -0.3,
            "plot": True,
        }

        # Add default parameters if missing
        for key, value in default_settings.items():
            self.settings.setdefault(key, value)

        # Create cach directory if not already there
        if not os.path.exists(self.settings.get("cache_dir")):
            os.mkdir(self.settings.get("cache_dir"))

        if not os.path.exists(self.settings.get("plots_dir")):
            os.makedirs(self.settings.get("plots_dir"))

        # Initialize empty vars
        self.estimated_spaces = None
        self.full_graph = None
        self.street_data = None
        self.mgra_gdf = None
        
        # Input data
        inputs = self.settings.get('inputs')        
        self.raw_path = inputs.get("raw_parking_inventory")
        self.lu_path = inputs.get("land_use")
        self.geometry = inputs.get("geometry")
        
        self.raw_parking_df = pd.read_csv(self.raw_path).set_index("mgra")
        self.lu_df = pd.read_csv(self.lu_path).set_index("mgra")
        if set(['hparkcost', 'dparkcost', 'mparkcost', 'parkarea']).issubset(set(self.lu_df.columns)):
            self.lu_df.drop(columns=['hparkcost', 'dparkcost', 'mparkcost', 'parkarea'], inplace=True)
            
        if set(['exp_hourly', 'exp_daily', 'exp_monthly', 'parking_type', 'parking_spaces']).issubset(set(self.lu_df.columns)):
            self.lu_df.drop(columns=['exp_hourly', 'exp_daily', 'exp_monthly', 'parking_type', 'parking_spaces'], inplace=True)

    def mgra_data(self):
        if self.mgra_gdf is None:
            print("Reading MGRA shapefile data")
            path = self.geometry
            cached_path = os.path.join(
                self.settings.get("cache_dir"), "cached_mgra.shp"
            )

            if not os.path.isfile(cached_path):
                self.mgra_gdf = gpd.read_file(path).set_index("MGRA")[
                    ["TAZ", "geometry"]
                ]
                self.mgra_gdf.to_file(cached_path)
            else:
                self.mgra_gdf = gpd.read_file(cached_path).set_index("MGRA")

        return self.mgra_gdf
       
    
    def write_output(self):
                
        output_cols = self.settings.get('output_columns')
        for df_name, out_path in self.settings.get('outputs').items():
            df = getattr(self, df_name).reset_index()
            
            if df_name in output_cols.keys():                                
                # Format column names
                renaming = {k: k if v is None else v for k, v in output_cols[df_name].items()}
                df = df.rename(columns=renaming)[renaming.values()]
                df.fillna(0, inplace=True)
            
            df.to_csv(out_path, index=False)
            #also write to land use file
            self.lu_df.merge(df, left_index=True, right_on='mgra').to_csv(self.lu_path)
       
        return