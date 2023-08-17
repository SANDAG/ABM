import numpy as np
import pandas as pd
import seaborn as sns
import scipy
import matplotlib.pyplot as plt
from sklearn.experimental import enable_iterative_imputer
from sklearn.impute import IterativeImputer
from . import base

class ImputeParkingCosts(base.Base):
    
    def run_imputation(self):
        print("Impute missing parking costs")   

        assert isinstance(self.reduced_parking_df, pd.DataFrame), "Must run create_reduced_parking_df() first"
        
        # Estimate model fit
        lm_res = self.linear_model(self.reduced_parking_df)

        # Impute missing costs
        imputed_parking_df = self.MICE_imputation(self.reduced_parking_df, self.lu_df)
        self.imputed_parking_df = self.label_imputations(imputed_parking_df, self.reduced_parking_df)

        # Plotting
        self.plot_imputation(self.reduced_parking_df, self.imputed_parking_df, lm_res)
        
        # append combined        
        new_cols = list(set(self.imputed_parking_df.columns) - set(self.reduced_parking_df.columns))
        self.combined_df = self.combined_df.join(self.imputed_parking_df[new_cols])


    # Estimate cost conversion model
    def linear_model(self, reduced_df):
        # Drop NAs
        hourday_df = reduced_df[["hourly", "daily"]].dropna()
        monthday_df = reduced_df[["monthly", "daily"]].dropna()
        monthhour_df = reduced_df[["monthly", "hourly"]].dropna()

        # Linear model
        lm_dayfromhour = scipy.stats.linregress(
            y=hourday_df.daily.values, x=hourday_df.hourly.values
        )
        lm_dayfrommonth = scipy.stats.linregress(
            y=monthday_df.daily.values, x=monthday_df.monthly.values
        )
        lm_monthfromhour = scipy.stats.linregress(
            y=monthhour_df.monthly.values, x=monthhour_df.hourly.values
        )

        lm_hourfromday = scipy.stats.linregress(
            x=hourday_df.daily.values, y=hourday_df.hourly.values
        )
        lm_monthfromday = scipy.stats.linregress(
            x=monthday_df.daily.values, y=monthday_df.monthly.values
        )
        lm_hourfrommonth = scipy.stats.linregress(
            x=monthhour_df.monthly.values, y=monthhour_df.hourly.values
        )

        # (from, to)
        lm_res = {
            ("hourly", "daily"): lm_dayfromhour,
            ("monthly", "daily"): lm_dayfrommonth,
            ("monthly", "hourly"): lm_monthfromhour,
            ("daily", "hourly"): lm_hourfromday,
            ("daily", "monthly"): lm_monthfromday,
            ("hourly", "monthly"): lm_hourfrommonth,
        }

        self.lm_res = lm_res

        return lm_res

    # Calculate daily from hourly and monthly costs, averaging the two if both exist.
    def manual_imputation(self, lm_models, reduced_df):
        # Loop through target columns to impute
        cols = ["hourly", "daily", "monthly"]
        for to_y in cols:
            # Loop through the from cols, average the difference if any
            res = pd.Series(0, index=reduced_df.index)
            count = pd.Series(0, index=reduced_df.index)

            for from_x in [x for x in cols if x is not to_y]:
                lm = lm_models[(from_x, to_y)]
                res += (lm.intercept + reduced_df[from_x] * lm.slope).fillna(0)
                count += np.where(reduced_df[from_x].isnull(), 0, 1)
            res /= count

            # fill in values we don't need to impute
            res[~reduced_df[to_y].isnull()] = reduced_df.loc[
                ~reduced_df[to_y].isnull(), to_y
            ]

            reduced_df[to_y + "_imputed"] = res

        return reduced_df

    # This basically does the same as the above, except for all cost columns and makes use of land use data
    def MICE_imputation(self, reduced_df, lu_df):
        # Step 2: Imputation
        all(lu_df.acres == lu_df.effective_acres)

        # Join landuse to data
        # model_df = reduced_df.join(
        #     lu_df[[
        #         # 'pop', 'emp_total',
        #         'hh_sf', 'hh_mf', 'hh_mh',
        #         # 'acres',
        #         'duden', 'empden', 'popden', 'retempden',
        #         # 'walk_dist_local_bus', 'walk_dist_premium_transit', #'luz_id'
        #         ]],
        #     # how='right'
        # )
        model_df = reduced_df.copy()

        # Remove 999s
        model_df[model_df > 999] = None
        model_df = model_df.drop(
            columns=[x for x in model_df.columns if "imputed" in x]
        )

        # Define imputer
        # imputer = SimpleImputer(missing_values=np.nan, strategy='mean')
        imputer = IterativeImputer(random_state=100, max_iter=100, min_value=0)
        # imputer = KNNImputer(n_neighbors=5, weights='distance')
        imputer.fit(model_df)

        # Impute and format the results
        imputed_df = pd.DataFrame(
            data=imputer.transform(model_df),
            index=model_df.index,
            columns=model_df.columns,
        )
        imputed_df = imputed_df.rename(
            columns={k: k + "_imputed" for k in ["hourly", "daily", "monthly"]}
        )
        imputed_df = imputed_df[[x for x in imputed_df.columns if "imputed" in x]]

        return model_df.join(imputed_df)

    def label_imputations(self, imputed_df, reduced_df):
        imputed_labels = pd.DataFrame(index=reduced_df.index)
        imputed_labels.loc[:, "imputed"] = False
        imputed_labels.loc[:, "imputed_types"] = ""
        imputed_labels.loc[:, "cost_types"] = ""
        cost_cols = ["hourly", "daily", "monthly"]

        for cost in cost_cols:
            imputed_labels.loc[reduced_df[cost].isnull(), "imputed"] = True
            imputed = imputed_labels.loc[reduced_df[cost].isnull(), "imputed_types"]
            not_imputed = imputed_labels.loc[~reduced_df[cost].isnull(), "cost_types"]
            imputed_labels.loc[reduced_df[cost].isnull(), "imputed_types"] += np.where(
                imputed == "", cost, " & " + cost
            )
            imputed_labels.loc[~reduced_df[cost].isnull(), "cost_types"] += np.where(
                not_imputed == "", cost, " & " + cost
            )

        imputed_labels.loc[
            imputed_labels.imputed.isnull(), "imputed_types"
        ] = "Not imputed"
        imputed_labels.loc[imputed_labels.cost_types == "", "cost_types"] = "None"
        imputed_df = imputed_df.join(imputed_labels)

        return imputed_df

    def plot_imputation(self, reduced_df, imputed_df, models):
        # Make some simple models
        # models.keys()
        xy_list = {
            ("daily", "monthly"): (5, 10),
            ("daily", "hourly"): (5, 10),
            ("hourly", "monthly"): (5, 0),
        }

        fig, axes = plt.subplots(2, 2, figsize=(10, 5))
        fig.subplots_adjust(hspace=0.25)

        for c, ((x, y), (txtx, txty)) in enumerate(xy_list.items()):
            i = c % 2
            j = c // 2
            ax = axes[i][j]
            mod = models[(x, y)]
            sns.regplot(
                data=reduced_df,
                x=x,
                y=y,
                scatter=True,
                ax=ax,
                scatter_kws={"s": 2},
            )
            sns.scatterplot(
                data=imputed_df,
                x=f"{x}_imputed",
                y=f"{y}_imputed",
                hue="imputed",
                ax=ax,
                alpha=0.5,
                s=5,
            )
            txt = f"y = {str(round(mod.intercept, 3))} + {str(round(mod.slope, 3))}x"
            ax.text(txtx, txty, txt)

        axes[1][1].axison = False
        for k, ((from_x, to_y), mod) in enumerate(models.items()):
            k /= 10
            txt = f"(x={from_x}, y={to_y}), y = {str(round(mod.intercept, 3))} + {str(round(mod.slope, 3))}x"
            axes[1][1].text(0, k, txt)
