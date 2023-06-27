import os
import folium
import numpy as np
import pandas as pd
import geopandas as gpd
import alphashape
from tqdm import tqdm
import matplotlib.pyplot as plt
from sklearn.cluster import AgglomerativeClustering
from . import base

class CreateDistricts(base.Base):
    def create_districts(self):
        print("Creating parking districts")

        # Read input
        in_path = self.settings.get("imputed_parking_costs")
        out_path = self.settings.get("district_data")
        plots_dir = self.settings.get("plots_dir")

        parking_costs_df = pd.read_csv(in_path).set_index("mgra")
        mgra_gdf = self.mgra_data()

        # Create districts
        self.districts_dict = self.parking_districts(
            parking_costs_df, mgra_gdf, self.settings.get("walk_dist")
        )

        # Create cach directory if not already there
        for geo in self.districts_dict.keys():
            self.save_districts(geo)

        # Save district data to csv
        self.map_districts_pngs(self.districts_dict, mgra_gdf, plots_dir)
        self.map_districts(self.districts_dict, mgra_gdf, plots_dir)        
        
        self.districts_dict["districts"].drop(columns="geometry").to_csv(out_path)

        return self.districts_dict

    def parking_districts(self, imputed_df, mgra_gdf, max_dist):
        # 1. Spatially cluster zones with paid parking
        paid_zones = mgra_gdf.loc[
            imputed_df[imputed_df.paid_spaces > 0].index, ["TAZ", "geometry"]
        ]

        # Calculate similarity matrix of ever zone to zone pair on their geometries not centroid
        print("Calculating similarity matrix")
        data_matrix = np.zeros([len(paid_zones)] * 2)
        for i, z in enumerate(tqdm(paid_zones.index)):
            data_matrix[i, :] = (
                paid_zones.geometry.distance(paid_zones.loc[z].geometry) / 5280
            )

        # Run clustering model -- kind of excessive but whatever
        print("Clustering zones")
        model = AgglomerativeClustering(
            metric="precomputed",
            compute_full_tree=True,
            linkage="single",
            distance_threshold=max_dist,
            n_clusters=None,
        ).fit(data_matrix)

        # Create cluster label dataframe to join from
        parking_clusters_labels = pd.DataFrame(
            model.labels_, columns=["cluster_id"], index=paid_zones.index
        )
        parking_clusters = paid_zones.join(parking_clusters_labels)

        # 2. Create a concave hull for each cluster & add buffer
        print("Creating concave hulls")

        def concave_hull(geo):
            alpha = 2.5 / (max_dist * 5280)
            flat_coords = [xy for geo in geo.exterior for xy in geo.coords]
            return alphashape.alphashape(flat_coords, alpha)

        hull_geoms = (
            parking_clusters.groupby("cluster_id")
            .geometry.apply(concave_hull)
            .set_crs(mgra_gdf.crs.to_epsg())
            .to_frame("geometry")
        )
        hull_geoms.index.name = "hull_id"
        buffer_geoms = hull_geoms.geometry.buffer(max_dist * 5280).to_frame("geometry")

        # Consolidate overlapping geometries
        parents = {}
        for i, geom in enumerate(buffer_geoms.geometry):
            connected = geom.intersects(buffer_geoms.geometry)
            edges = buffer_geoms.index[connected].to_list()
            for x in edges:
                if x not in parents:
                    parents[x] = i

        district_ids = pd.DataFrame.from_dict(
            parents, orient="index", columns=["district_id"]
        )
        buffer_geoms = buffer_geoms.join(district_ids).dissolve("district_id")

        # 3. Spatial Join all zones within hulls
        print("Performing spatial joins")
        # Add cluster id
        parking_districts = mgra_gdf[["geometry"]].join(
            parking_clusters[["cluster_id"]]
        )

        # Add hull id
        parking_districts = parking_districts.sjoin(
            hull_geoms.geometry.reset_index(), how="left", predicate="within"
        ).drop(columns="index_right")

        # Add district
        parking_districts = parking_districts.sjoin(
            buffer_geoms.geometry.reset_index(), how="left", predicate="within"
        ).drop(columns="index_right")

        # Determine parking_zone_type
        # is_prkdistrict    = zone within parking district
        # is_noprkspace     = zone within parking district but has no parking spaces

        # filters
        is_district = ~parking_districts.district_id.isnull()
        is_nodata = ~parking_districts.index.isin(paid_zones.index)
        is_hull = ~parking_districts.hull_id.isnull()

        # Assign
        parking_districts["is_prkdistrict"] = False
        parking_districts["is_noprkspace"] = False
        parking_districts.loc[is_district, "is_prkdistrict"] = True
        parking_districts.loc[is_hull & is_nodata, "is_noprkspace"] = True

        output = {
            "districts": parking_districts,
            "hulls": hull_geoms,
            "buffered_hulls": buffer_geoms,
            "clusters": parking_clusters,
        }

        return output

    def map_districts(self, district_dict, mgra_gdf, plots_dir):
        parking_districts = district_dict["districts"]
        parking_hulls = district_dict["hulls"]
        parking_buffered_hulls = district_dict["buffered_hulls"]
        parking_clusters = district_dict["clusters"]

        # Plot paid parking zones
        map = folium.Map(
            location=[32.7521765494396, -117.11514883606573],
            tiles="Stamen Toner",
            zoom_start=9,
        )
        folium.Choropleth(
            data=parking_clusters.reset_index(),
            geo_data=parking_clusters.reset_index(),  # data
            columns=["mgra", "cluster_id"],  # [key, value]
            key_on="feature.properties.mgra",
            fill_column="cluster_id",
            fill_color="Set3",  # cmap
            line_weight=0.1,  # line wight (of the border)
            line_opacity=0.5,  # line opacity (of the border)
            legend_name="Parking clusters",
        ).add_to(
            map
        )  # name on the legend color bar
        map.save(f"{plots_dir}/1_paid_zones.html")

        # Plot concave hull areas
        folium.GeoJson(
            data=parking_hulls.geometry,
            style_function=lambda x: {
                "fillColor": "#e41a1c",
                "color": "#e41a1c",
                "weight": 0.5,
            },
        ).add_to(map)
        map.save(f"{plots_dir}/2_concave_hull.html")

        # Plot buffered concave hulls
        buffered_hulls = gpd.GeoSeries(
            parking_buffered_hulls.geometry.unary_union
        ).set_crs(mgra_gdf.crs.to_epsg())
        folium.GeoJson(
            data=buffered_hulls,
            style_function=lambda x: {
                "fillColor": "#000000",
                "color": "#000000",
                "weight": 0.5,
            },
        ).add_to(map)
        map.save(f"{plots_dir}/3_buffered_hull.html")

        # Plot parking district zones
        gdf_districts = parking_districts[
            ~parking_districts.district_id.isnull()
        ].geometry.unary_union
        gdf_districts = gpd.GeoSeries(gdf_districts).set_crs(mgra_gdf.crs.to_epsg())
        folium.GeoJson(
            data=gdf_districts,
            style_function=lambda x: {"fillColor": "#000000", "weight": 0},
        ).add_to(map)
        map.save(f"{plots_dir}/4_parking_district.html")
        
    def map_districts_pngs(self, district_dict, mgra_gdf, plots_dir):
        parking_districts = district_dict["districts"]
        parking_hulls = district_dict["hulls"]
        parking_buffered_hulls = district_dict["buffered_hulls"]
        parking_clusters = district_dict["clusters"]
        
        fig, axes = plt.subplots(2, 2, figsize=(8, 7))
        for axrow in axes:
            for ax in axrow:
                ax.axis('off')
                mgra_gdf.geometry.plot(color='white', edgecolor='k', linewidth=0.125, ax=ax)
                ax.set_xlim(np.array([6.26, 6.31]) * 1e6)
                ax.set_ylim(np.array([1.82, 1.86])*1e6)
        parking_clusters.plot(column='cluster_id', alpha=0.5, ax=axes[0][0], legend=False).set_title('Parking zone clusters')
        parking_hulls.geometry.reset_index().plot(column='hull_id', alpha=0.5, ax=axes[0][1], legend=False).set_title('Concave hulls')
        parking_buffered_hulls.geometry.reset_index().plot(column='district_id', alpha=0.5, ax=axes[1][0], legend=False).set_title('Buffered hulls')
        parking_districts[~parking_districts.district_id.isnull()].plot(column='district_id', alpha=0.5, ax=axes[1][1], legend=False).set_title('Parking districts')

        fig.savefig(f'{plots_dir}/clustermethod.png', dpi=800)

    def save_districts(self, geo):
        # Create cach directory if not already there
        out_dir = os.path.join(self.settings.get("output_dir"), "shapefiles")
        if not os.path.exists(out_dir):
            os.mkdir(out_dir)

        out_path = os.path.join(out_dir, geo + ".shp")
        self.districts_dict[geo].to_file(out_path)
