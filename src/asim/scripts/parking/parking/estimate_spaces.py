import os
import seaborn as sns
import osmnx as ox
import geonetworkx as gnx
import networkx as nx
import geopandas as gpd
import pandas as pd
import statsmodels.formula.api as smf
import matplotlib.pyplot as plt
from tqdm import tqdm
from . import base


class EstimateStreetParking(base.Base):
    
    def run_space_estimation(self):
        method = self.settings.get("space_estimation_method")
        cache_dir = self.settings.get("cache_dir")

        # Read input
        mgra_gdf = self.mgra_data()

        assert isinstance(mgra_gdf, gpd.GeoDataFrame)
        assert isinstance(self.imputed_parking_df, pd.DataFrame)
        assert isinstance(self.lu_df, pd.DataFrame)

        parking_df = self.aggregate_spaces_data(self.imputed_parking_df)
        street_data = self.get_streetdata(mgra_gdf)
        estimated_spaces = self.estimate_spaces(
            street_data, mgra_gdf, parking_df, self.lu_df, method
        )

        # estimated_spaces.to_csv(out_path)
        self.estimated_spaces_df = estimated_spaces
        
        # append combined
        self.combined_df = self.combined_df.join(self.estimated_spaces_df)


    def aggregate_spaces_data(self, raw_parking_df):
        is_raw = any([x for x in raw_parking_df.columns if "on_street" in x])

        if "spaces" not in raw_parking_df.columns and is_raw:
            # Aggregate on-street parking and join
            spaces_df = (
                raw_parking_df.filter(regex="on_street")
                .filter(regex="spaces")
                .sum(axis=1)
                .to_frame("spaces")
            )
        else:
            spaces_df = raw_parking_df[["spaces"]]

        return spaces_df

    def estimate_spaces(self, street_data, mgra_gdf, parking_df, land_use, method="lm"):
        spaces_df = parking_df.join(street_data[["length", "intcount"]])
        spaces_df = spaces_df.join((mgra_gdf.geometry.area / 43560).to_frame("acres"))
        spaces_df = spaces_df[(spaces_df.spaces > 0) & (spaces_df.length > 0)]

        # Impute parking for only missing zones
        street_data["estimated_spaces"] = self.predict_spaces(
            street_data, parking_df, mgra_gdf, land_use
        )

        if method != "lm":
            street_data["estimated_spaces"] = self.calculate_spaces(
                street_data.length, street_data.intcount
            ).astype(int)

        street_data.loc[spaces_df.index, "estimated_spaces"] = spaces_df.spaces.astype(
            int
        )

        return street_data

    # Fetch network data from web or local
    def get_network(self, mgra_gdf, cache_path):
        path = os.path.join(cache_path, "network.graphml")

        if not os.path.isfile(path):
            print("Fetching latest OSM network data")
            region = mgra_gdf.geometry.to_crs(epsg=4326).unary_union
            G = ox.graph_from_polygon(
                region, network_type="drive", simplify=True, truncate_by_edge=True
            )
            print("Saving OSM network in cache")
            ox.save_graphml(G, path)
        else:
            print("Loading cached network data")
            G = ox.load_graphml(path)

        self.full_graph = G

        return G

    # Query link types, remove unlikely to have parking (highways, etc.)
    def filter_network(self, G, parking_streets=None):
        if parking_streets is None:
            parking_streets = [
                # 'unclassified',
                "residential",
                "living_street",
                "road",
                # 'service',
                "tertiary",
                "secondary",
            ]

        def isparkingstreet(highway):
            if not isinstance(highway, list):
                highway = [highway]
            return len(set(highway).intersection(parking_streets)) > 0

        selected_edges = [
            (u, v, e) for u, v, e in G.edges(data=True) if isparkingstreet(e["highway"])
        ]

        # Create new graph from selected edges and add node attributes
        H = nx.Graph(selected_edges)
        nx.set_node_attributes(
            H, {n: e for n, e in G.nodes(data=True) if n in H.nodes()}
        )

        # Update crs
        H.graph["crs"] = G.graph["crs"] # type: ignore

        self.cleaned_graph = H

        return H

    # This could probably be parallelized, but was messy to do in jupyter
    def aggregate_streetdata(self, cleaned_graph, mgra_gdf):
        edges_gdf = gpd.GeoDataFrame(gnx.graph_edges_to_gdf(cleaned_graph))
        nodes_gdf = gpd.GeoDataFrame(gnx.graph_nodes_to_gdf(cleaned_graph))

        # Current crs
        graph_crs = cleaned_graph.graph["crs"]
        
        edges_gdf = edges_gdf.set_crs(graph_crs).to_crs(mgra_gdf.crs.to_epsg()) # type: ignore
        nodes_gdf = nodes_gdf.set_crs(graph_crs).to_crs(mgra_gdf.crs.to_epsg()) # type: ignore

        # Intersections have >2 segments
        assert isinstance(nodes_gdf, gpd.GeoDataFrame)
        intnodes_gdf = nodes_gdf[nodes_gdf.street_count > 2] 

        def intersect_zones(geo):
            # First clip search space
            edges_clipped = gpd.clip(edges_gdf, geo)
            nodes_clipped = gpd.clip(intnodes_gdf, geo)

            # Get detailed intersection
            e = edges_clipped.geometry.intersection(geo)
            n = nodes_clipped.geometry.intersection(geo)

            # Remove empty
            e = e[~e.is_empty]

            res = {
                "length": e.length.sum(),
                "intcount": n.count(),
                "edges": e,
                "nodes": n,
            }
            return res

        # Intersect graph with zones
        print("Aggregating network data into zones")
        street_data = [intersect_zones(x) for x in tqdm(list(mgra_gdf.geometry.values))]
        streets_gdf = gpd.GeoDataFrame.from_dict(street_data)
        streets_gdf.index = mgra_gdf.index
        return streets_gdf

    def get_streetdata(self, mgra_gdf):        
        out_dir = self.settings.get("output_dir")
        cache_dir = self.settings.get("cache_dir")
        data_path = os.path.join(out_dir, 'aggregated_street_data.csv')
        
        if not os.path.isfile(data_path):
            print("Aggregated street data")
            full_graph = self.get_network(mgra_gdf, cache_dir)
            cleaned_graph = self.filter_network(full_graph)

            # Aggregate length and number of intersections per zone
            street_data = self.aggregate_streetdata(cleaned_graph, mgra_gdf)
                        
            df = street_data[["length", "intcount"]]
            assert isinstance(df, pd.DataFrame)
            df.to_csv(data_path)
        else:
            street_data = pd.read_csv(data_path).set_index("MGRA")

        self.street_data = street_data

        return street_data

    # Calculate number of parking spaces per length and intersections
    def calculate_spaces(self, length, intcount):
        return 2 * (length / 10) - 2 * intcount

    def predict_spaces(self, street_data, parking_df, mgra_gdf, land_use):
        acres = (mgra_gdf.geometry.area / 43560).to_frame("acres")
        lu = land_use[["hh", "hh_sf", "hh_mf", "emp_total"]]

        model_df = parking_df.join(street_data[["length", "intcount"]])
        model_df = model_df.join(acres)
        model_df = model_df[(model_df.spaces > 0) & (model_df.length > 0)]
        model_df["length_per_acre"] = model_df.length / model_df.acres
        model_df["int_per_acre"] = model_df.intcount / model_df.acres
        model_df["avg_block_length"] = model_df.length / model_df.intcount.clip(1)
        model_df = model_df.join(land_use[["hh", "hh_sf", "hh_mf", "emp_total"]])

        # Formula
        f = "spaces ~ 0 + length + intcount + acres + hh_sf + hh_mf + emp_total"

        # Estimate model
        mod_lm = smf.ols(formula=f, data=model_df).fit()
        print(mod_lm.summary())

        # Predict model
        full_streetdata_df = street_data[["length", "intcount"]].join(acres).join(lu)
        result = mod_lm.predict(full_streetdata_df).clip(0)

        # plot distributions
        plots_dir = self.settings.get("plots_dir")
        self.plot_distributions(model_df, plots_dir)
        self.plot_reg(model_df, plots_dir)
        self.plot_predictions(mod_lm, model_df, plots_dir)

        return result

    def plot_distributions(self, model_df, plot_dir):
        fig, axes = plt.subplots(3, 3, figsize=(12, 8))
        fig.subplots_adjust(hspace=0.3, wspace=0.25)
        plot_vars = [
            ["length", "intcount", "acres"],
            ["length_per_acre", "int_per_acre", "avg_block_length"],
            ["spaces"],
        ]

        for i, row in enumerate(plot_vars):
            for j, col in enumerate(row):
                sns.histplot(data=model_df, x=col, ax=axes[i][j])

        fig.savefig(f"{plot_dir}/parkingspace_distributions.png")

    def plot_reg(self, model_df, plot_dir):
        i, cols = 0, [
            "length",
            "intcount",
            "avg_block_length",
            "acres",
        ]  # {'spaces': ['length', 'intcount'], 'log-spaces': ['length_per_acre', 'int_per_acre']}
        fig, axes = plt.subplots(2, 2, figsize=(9, 6))
        for axx in axes:
            for ax in axx:
                sns.regplot(
                    data=model_df,
                    y="spaces",
                    x=cols[i],
                    ax=ax,
                    scatter=True,
                    scatter_kws={"s": 5, "alpha": 0.25},
                )  # .set(xscale='log')
                i += 1

        fig.savefig(f"{plot_dir}/parkingspace_regplot.png")

    def plot_predictions(self, model, model_df, plot_dir):
        yactual = model_df.spaces.values
        ypred_lm = model.predict(model_df)
        ypred_calc = self.calculate_spaces(model_df.length, model_df.intcount)

        # fit model
        df = pd.DataFrame({"x": yactual, "y_lm": ypred_lm, "y_calc": ypred_calc})
        r_lm = smf.ols("ypred_lm ~ x + 0", data=df).fit()
        r_calc = smf.ols("ypred_calc ~ x + 0", data=df).fit()

        fig, axes = plt.subplots(1, 2, figsize=(12, 6))
        for ax in axes:
            ax.set_ylim(0, 1000)
            ax.set_xlim(0, 1000)
        sns.scatterplot(
            x=yactual,
            y=ypred_lm,
            ax=axes[0],
            s=5,
            alpha=0.25,
            # scatter=True,
            # scatter_kws={"s": 5, "alpha": 0.25},
        ).set(
            title="Regression model of parking spaces",
            xlabel="Actual",
            ylabel="Predicted",
        )
        axes[0].plot(yactual, r_lm.fittedvalues)
        axes[0].text(
            3 + 0.2,
            4.5,
            f"R^2: {round(r_lm.rsquared, 2)}, y = 0 + {round(r_lm.params[0],2)}x",
            horizontalalignment="left",
            size="medium",
            color="black",
        )

        sns.scatterplot(
            x=yactual,
            y=ypred_calc,
            ax=axes[1],
            s=5,
            alpha=0.25,
            # scatter=True,
            # scatter_kws={"s": 5, "alpha": 0.25},
        ).set(
            title="Formulaic model of parking spaces",
            xlabel="Actual",
            ylabel="Predicted",
        )
        axes[1].plot(yactual, r_calc.fittedvalues)
        axes[1].text(
            3 + 0.2,
            4.5,
            f"R^2: {round(r_calc.rsquared, 2)}, y = 0 + {round(r_calc.params[0],2)}x",
            horizontalalignment="left",
            size="medium",
            color="black",
        )

        fig.savefig(f"{plot_dir}/parkingspace_prediction_plot.png")


# # Debugging
# if __name__ == "__main__":
#     print("load data")
#     raw_parking_df = pd.read_csv("data/mgra_parking_inventory.csv").set_index("mgra")
#     mgra_gdf = gpd.read_file("data/mgra15/mgra15.shp").set_index("MGRA")

#     print("prepare network data")
#     EP = EstimateStreetParking()
#     # G = EP.get_network(mgra_gdf)
#     # H = EP.filter_network(G)
#     # print('aggregate network')
#     # street_data = EP.aggregate_streetdata(H, mgra_gdf)
#     street_data = EP.get_streetdata(mgra_gdf, "./data/agg_streetdata.csv")
#     print("estimate spaces")
#     estimated_spaces = EP.estimate_spaces(street_data, mgra_gdf, raw_parking_df)
