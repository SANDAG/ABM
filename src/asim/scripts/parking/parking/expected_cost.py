import os
import folium
import numpy as np
import pandas as pd
import geopandas as gpd
from tqdm import tqdm
import matplotlib.pyplot as plt
from . import base

class ExpectedParkingCost(base.Base):
    
    def run_expected_parking_cost(self):
        # Inputs
        out_dir = self.settings.get("output_dir")
        plots_dir = self.settings.get("plots_dir")
                
        assert isinstance(self.districts_dict, dict), "Must run create_districts() first"
        districts_df = self.districts_dict['districts']
        mgra_gdf = self.mgra_data()
        
        # Prepare data
        costs_df = self.prepare_cost_table(self.imputed_parking_df, self.estimated_spaces_df, districts_df)
        district_ids = districts_df[districts_df.is_prkdistrict].index.unique()
        geos = mgra_gdf.loc[district_ids].geometry
        max_dist = self.settings.get("walk_dist")
        walk_coef = self.settings.get("walk_coef")

        print("pre-calculate walk distance matrix")
        dist_df = self.pre_calc_dist(geos, max_dist, out_dir)

        print("Calculate expected costs")
        # Calculate expected cost for all zones in districts, all else are 0 cost
        exp_prkcosts = [
            self.expected_parking_cost(x, costs_df, dist_df, walk_coef)
            for x in tqdm(geos.index)
        ]
        # exp_prkcosts = pd.DataFrame(exp_prkcosts, index=geos.index, columns=['exp_prkcost'])
        exp_prkcosts_df = pd.DataFrame(exp_prkcosts).set_index("index")

        exp_prkcosts_df = exp_prkcosts_df.rename(
            columns={x: "exp_" + x for x in exp_prkcosts_df.columns}
        )
        exp_prkcosts_df = exp_prkcosts_df.reindex(mgra_gdf.index)
        exp_prkcosts_gdf = exp_prkcosts_df.join(mgra_gdf[["geometry"]])
        exp_prkcosts_df = exp_prkcosts_df.fillna(0)
        
        # Map it
        self.map_costs_pngs(exp_prkcosts_gdf, plots_dir)
        self.map_costs(exp_prkcosts_gdf, plots_dir)

        # exp_prkcosts_df.to_csv(f"./{output_dir}/expected_parking_costs.csv")
        self.expected_parking_df = exp_prkcosts_df
        
        # append combined
        self.combined_df = self.combined_df.join(self.expected_parking_df)

        # return exp_prkcosts_gdf

    def pre_calc_dist(self, geos, max_dist, save_dir):
        dist_path = os.path.join(save_dir, "distances.csv")

        if not os.path.isfile(dist_path):
            dist_matrix = np.zeros([len(geos.index)] * 2)
            for i, zone in enumerate(tqdm(geos.index)):
                dist_matrix[i, :] = geos.distance(geos.loc[zone]) / 5280
            dist_df = pd.DataFrame(dist_matrix, index=geos.index, columns=geos.index)
            dist_df.index.name = "OZONE"
            dist_df = dist_df.reset_index().melt(
                id_vars="OZONE", var_name="DZONE", value_name="dist"
            )
            dist_df = dist_df[dist_df.dist <= max_dist]
            dist_df = dist_df.reset_index()

            # Store cached distances
            dist_df.to_csv(dist_path)
        else:
            dist_df = pd.read_csv(dist_path)

        dist_df = dist_df.set_index("DZONE")

        return dist_df

    def prepare_cost_table(self, parking_df, spaces_df, districts_df, use_imputed=True):
        if use_imputed:
            imputed_names = {k + "_imputed": k for k in ["hourly", "daily", "monthly"]}
            parking_df = parking_df.drop(columns=["hourly", "daily", "monthly"])
            parking_df = parking_df.rename(columns=imputed_names)

        # Estimated spaces
        parking_df = parking_df.drop(columns="spaces")
        spaces_df = spaces_df.rename(columns={"estimated_spaces": "spaces"})

        # Remove zones outside district or no supply
        # _districts = districts_df.loc[
        #     districts_df.is_prkdistrict & ~districts_df.is_noprkspace
        # ]
        noprk_zones = districts_df.loc[
            districts_df.is_prkdistrict & districts_df.is_noprkspace
        ]
        park_districts = districts_df.loc[districts_df.is_prkdistrict]

        # Keep only the zones we want to calculate expected costs for
        spaces_df = spaces_df.loc[park_districts.index]

        # Final costs dataframe
        # costs_df = _districts.join(spaces_df).join(costs_df)
        costs_df = spaces_df.join(parking_df)
        costs_df.index.name = "mgra"
        costs_df = costs_df[["spaces", "hourly", "daily", "monthly"]]

        # Fill in 0 for non paid zones
        costs_df = costs_df.fillna(0)
        # Set no parking zones to 0 supply
        costs_df.loc[noprk_zones.index, "spaces"] = 0

        assert not costs_df.index.duplicated().any()

        return costs_df

    def expected_parking_cost(
        self,
        dest_id,
        costs_df,
        dist_df,
        walk_coef,
        cost_type=["hourly", "daily", "monthly"],
    ):
        # If dest_id not in parking costs at all, default to 0
        if dest_id in costs_df.index:
            # If no other zone within walking distance, default to parking cost of zone
            if dest_id in dist_df.index:
                # Find all zones within walking distance
                dest_df = dist_df.loc[dest_id]

                # Swap the indices and join costs to the aternative zones to be averaged
                dest_df = dest_df.reset_index().set_index("OZONE").join(costs_df)

                # Natural exponent -- compute once
                expo = np.exp(dest_df.dist.values * walk_coef) * dest_df.spaces.values

                # numerator = sum(e^{dist * \beta_{dist}} * spaces * cost)
                # denominator = sum(e^{dist * \beta_{dist}} * spaces)
                numer = np.nansum(expo * dest_df[cost_type].values.T, axis=1)
                denom = np.nansum(expo)

                if denom > 0:
                    expected_cost = dict(zip(cost_type, numer / denom))
                else:
                    expected_cost = {x: 0 for x in cost_type}

            else:
                expected_cost = costs_df.loc[dest_id, cost_type].to_dict()

        else:
            expected_cost = {x: 0 for x in cost_type}

        expected_cost["index"] = dest_id

        return expected_cost

    def map_costs(self, exp_prkcost_gdf, plots_dir):
        for cost_type in ["exp_hourly", "exp_daily", "exp_monthly"]:
            gdf = exp_prkcost_gdf[["geometry", cost_type]].dropna().reset_index()
            gdf = gpd.GeoDataFrame(gdf)

            # Plot paid parking zones
            map = folium.Map(
                location=[32.7521765494396, -117.11514883606573],
                tiles="Stamen Toner",
                zoom_start=9,
            )
            folium.Choropleth(
                data=gdf,
                geo_data=gdf,  # data
                columns=["MGRA", cost_type],  # [key, value]
                key_on="feature.properties.MGRA",
                fill_column=cost_type,
                fill_color="YlOrRd",  # cmap
                line_weight=0.1,  # line wight (of the border) # type: ignore
                line_opacity=0.5,  # line opacity (of the border) # type: ignore
                legend_name=f"Expected {cost_type} parking costs",
            ).add_to(
                map
            )  # name on the legend color bar
            map.save(f"{plots_dir}/parking_costs_{cost_type}.html")
            
    def map_costs_pngs(self, exp_prkcost_gdf, plots_dir):        
                
        for cost_type in ["exp_hourly", "exp_daily", "exp_monthly"]:
            fig, ax = plt.subplots(1, 1, figsize=(8, 8))
            lab = cost_type[4:].capitalize()
            gdf = exp_prkcost_gdf[["geometry", cost_type]].dropna().reset_index()
            gdf = gpd.GeoDataFrame(gdf)
            # ax.axis('off')
            ax.set_xlim(np.array([6.26, 6.31]) * 1e6)  # type: ignore
            ax.set_ylim(np.array([1.82, 1.86]) * 1e6)  # type: ignore
            gdf.plot(column=cost_type, alpha=0.5, ax=ax, legend=True).set_title(f'{lab} Expected Parking Costs')
            fig.savefig(f"{plots_dir}/parking_costs_{cost_type}.png")