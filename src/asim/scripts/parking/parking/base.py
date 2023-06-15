import os
import geopandas as gpd
import yaml


class Base:
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

    def mgra_data(self):
        if self.mgra_gdf is None:
            print("Reading MGRA shapefile data")
            path = self.settings.get("geometry")
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
