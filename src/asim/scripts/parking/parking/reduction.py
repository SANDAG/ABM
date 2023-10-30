import numpy as np
import pandas as pd
from . import base


class ReduceRawParkingData(base.Base):
    def run_reduction(self):
        print("Reducing raw parking data")
        self.reduced_parking_df = self.parking_reduction(self.raw_parking_df)
        
        self.combined_df = self.lu_df.join(self.reduced_parking_df)

    def parking_reduction(self, raw_parking_df):
        # Free parking spaces
        df = (
            raw_parking_df[
                [
                    "on_street_free_spaces",
                    "off_street_free_spaces",
                    "off_street_residential_spaces",
                ]
            ]
            .sum(axis=1)
            .to_frame("free_spaces")
        )

        # Paid parking spaces
        df["paid_spaces"] = raw_parking_df[
            ["on_street_paid_spaces", "off_street_paid_private_spaces"]
        ].sum(axis=1)

        # Total spaces
        df["spaces"] = df[["paid_spaces", "free_spaces"]].sum(axis=1)

        # Drop zones with zero spaces
        df = df[df.spaces > 0]

        # Hourly cost
        hourly_costs = pd.concat(
            [
                raw_parking_df[
                    [
                        "on_street_hourly_cost_during_business",
                        "on_street_hourly_cost_after_business",
                    ]
                ].max(axis=1),
                raw_parking_df[
                    [
                        "off_street_paid_public_hourly_cost_during_business",
                        "off_street_paid_public_hourly_cost_after_business",
                    ]
                ].max(axis=1),
                raw_parking_df[
                    [
                        "off_street_paid_private_hourly_cost_during_business",
                        "off_street_paid_private_hourly_cost_after_business",
                    ]
                ].max(axis=1),
            ],
            axis=1,
        )

        dummy = ~hourly_costs.isnull().values
        spaces = raw_parking_df[
            [
                "on_street_paid_spaces",
                "off_street_paid_public_spaces",
                "off_street_paid_private_spaces",
            ]
        ]
        # Average weighted hourly cost, skipping NAs
        df["hourly"] = (hourly_costs * spaces.values).sum(axis=1) / (
            spaces * dummy
        ).sum(axis=1)
        df["hourly"] = hourly_costs.mean(axis=1)

        # Daily costs
        daily_costs = raw_parking_df[
            ["off_street_paid_public_daily_cost", "off_street_paid_private_daily_cost"]
        ]
        dummy = ~daily_costs.isnull().values
        spaces = raw_parking_df[
            ["off_street_paid_public_spaces", "off_street_paid_private_spaces"]
        ]
        # Average weighted daily cost, skipping NAs
        df["daily"] = (daily_costs * spaces.values).sum(axis=1) / (spaces * dummy).sum(
            axis=1
        )
        df["daily"] = daily_costs.mean(axis=1)

        # Monthly costs
        monthly_costs = raw_parking_df[
            [
                "off_street_paid_public_monthly_cost",
                "off_street_paid_private_monthly_cost",
            ]
        ]
        dummy = ~monthly_costs.isnull().values
        # Average weighted monthly cost, skipping NAs
        df["monthly"] = (monthly_costs * spaces.values).sum(axis=1) / (
            spaces * dummy
        ).sum(axis=1)
        df["monthly"] = monthly_costs.mean(axis=1)

        # Can't have $0 costs, replace with NA
        for cost in ["hourly", "daily", "monthly"]:
            df[cost] = df[cost].replace(0, np.NaN)

        return df
