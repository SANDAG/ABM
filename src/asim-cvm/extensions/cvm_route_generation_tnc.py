from __future__ import annotations

import logging

import numpy as np
import pandas as pd

from activitysim.core import workflow
from activitysim.core.input import read_input_table

from .cvm_enum import BusinessTypes
from .cvm_enum_tools import as_int_enum
from .cvm_state import State

logger = logging.getLogger(__name__)

_business_type_offset = int(10 ** np.ceil(np.log10(max(BusinessTypes))))


@workflow.step
def route_generation_tnc(
    state: State,
    land_use: pd.DataFrame,
    establishments: pd.DataFrame,
    model_settings: None = None,
    model_settings_file_name: str = "route_generation_tnc.yaml",
    trace_label: str = "route_generation_tnc",
) -> None:
    """
    Generate tnc routes from employments in land use zones.

    group the employments of each land use zones into three business types: tnc resturant, tnc retail, tnc non-rr.
    for each business type, generate routes from the total employments in the land use zone by applying a routes per job rate.

    The result of running this component is the creation of the tnc routes table, appended to the routes table.
    It contains very little information at creation, just the columns
    {'route_id', 'establishment_id', 'business_type'}.

    need to create fake establishment IDs for the tnc routes, since the tnc routes are not associated with any establishments.

    should use the sample rate of the establishments table for the tnc routes table.

    Parameters
    ----------
    state : State
    land_use : pandas.DataFrame
    establishments : pandas.DataFrame
    """

    if model_settings is None:
        model_settings = state.filesystem.read_settings_file(model_settings_file_name)

    trace_label = "cvm_route_generation_tnc"

    logger.info("Running %s with land use data", trace_label)

    land_use_df = land_use.copy()
    all_establishments_df = read_input_table(state, "establishments")
    establishments_df = establishments.copy()

    # calculates total employments for each business type in each land use zone
    tnc_industry = model_settings.get("industries")
    for industry_id, industry in tnc_industry.items():
        # sum the employments
        land_use_df[industry.get("name")] = land_use_df[
            industry.get("employment_categories")
        ].sum(axis=1)

    # create required columns for fake establishments
    # industry_number, industry_name, zone_id, employees, establishment_id, sample_rate
    tnc_establishments_df = land_use_df[
        [industry.get("name") for industry_id, industry in tnc_industry.items()]
    ].copy()
    tnc_establishments_df = tnc_establishments_df.melt(
        var_name="industry_name", value_name="employees", ignore_index=False
    ).reset_index()

    # the establishment id starts from the maximum establishment id in the establishments table
    tnc_establishments_df["establishment_id"] = (
        tnc_establishments_df.index
        + all_establishments_df["establishment_id"].max()
        + 1
    )
    # set index
    tnc_establishments_df.set_index("establishment_id", inplace=True)

    # add industry number
    tnc_establishments_df["industry_number"] = tnc_establishments_df[
        "industry_name"
    ].map(
        {
            industry.get("name"): industry_id
            for industry_id, industry in tnc_industry.items()
        }
    )

    # add sample rate column
    tnc_establishments_df["sample_rate"] = establishments_df["sample_rate"].max()
    # scale down the employees by the sample rate
    tnc_establishments_df["employees"] = (
        tnc_establishments_df["employees"] * tnc_establishments_df["sample_rate"]
    )

    # generate tnc routes from the tnc establishments
    # step 1: calculate the number of routes for each business type
    tnc_establishments_df["n_routes"] = 0
    rate_map = {
        industry_id: industry.get("tnc_rate")
        for industry_id, industry in tnc_industry.items()
    }
    tnc_establishments_df["n_routes"] = tnc_establishments_df[
        "employees"
    ] * tnc_establishments_df["industry_number"].map(rate_map)
    tnc_establishments_df["n_routes"] = tnc_establishments_df["n_routes"].fillna(0)

    # round the number of routes to integer
    tnc_establishments_df["n_routes"] = (
        tnc_establishments_df["n_routes"].round().astype(int)
    )

    # cap the number of routes to the maximum number of routes per establishment
    max_n_routes_per_lu_zone = model_settings.get("MAX_N_ROUTES_PER_LU_ZONE")
    tnc_establishments_df["n_routes"] = np.where(
        tnc_establishments_df["n_routes"] > max_n_routes_per_lu_zone,
        max_n_routes_per_lu_zone,
        establishments_df["n_routes"],
    )

    # append the tnc establishments to the establishments table
    establishments_df = establishments_df.append(tnc_establishments_df)

    # register the new establishments table
    state.add_table("establishments", establishments_df)

    # step 2: generate routes from the tnc establishments
    # generate routes table
    route_estab_id = []
    route_btype = []
    for b in BusinessTypes:
        establishments_sub_df = tnc_establishments_df[
            tnc_establishments_df["industry_name"] == b.name
        ].copy()
        n_routes_btype = establishments_sub_df["n_routes"].sum()
        route_estab_id.append(
            np.repeat(establishments_sub_df.index, establishments_sub_df["n_routes"])
        )
        route_btype.append(np.full(n_routes_btype, fill_value=b.value, dtype=np.int8))
    tnc_routes = pd.DataFrame(
        {
            "establishment_id": np.concatenate(route_estab_id),
            "business_type": as_int_enum(
                np.concatenate(route_btype), BusinessTypes, categorical=True
            ),
        }
    )
    route_id = tnc_routes.groupby(["establishment_id", "business_type"]).cumcount()
    route_id += (
        tnc_routes["establishment_id"] * _business_type_offset
        + tnc_routes["business_type"].cat.codes
    ) * max_n_routes_per_lu_zone
    tnc_routes = tnc_routes.set_index(pd.Index(route_id, name="route_id"))

    # append the tnc routes to the routes table
    routes = state.get_table("routes")
    routes = routes.append(tnc_routes)

    # register the new routes table
    state.add_table("routes", routes)
    state.get_rn_generator().add_channel("routes", routes)
