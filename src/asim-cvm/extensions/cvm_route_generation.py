# Route Generation
# -	Each (pseudo-)establishment generates N “Routes”
#   - A Route is the commercial work for a vehicle in a day
#   - Unique name helps disambiguate routes from “tours” in the passenger model
#   - A route is not a single closed tour in the usual modeling sense, it can
#     potentially return to depot multiple times, and/or end day not at depot
#   - Will need to segment by business types
#     - Industrial (agriculture, manufacture, construction)
#     - Wholesale
#     - Retail
#     - Service
#     - Transport handling (Fedex etc.)
#     - Utilities
#     - Gig workers
#   - Some kind of parameterized count model poisson, negative binomial, etc
#   - Each segment will have levers to attach generation rates to exogenous info.
from __future__ import annotations

import logging

import numpy as np
import pandas as pd

from activitysim.abm.tables.util import simple_table_join
from activitysim.core import workflow, tracing

from .cvm_enum import BusinessTypes
from .cvm_enum_tools import as_int_enum
from .cvm_state import State

logger = logging.getLogger(__name__)

_business_type_offset = int(10 ** np.ceil(np.log10(max(BusinessTypes))))


@workflow.step
def route_generation(
    state: State,
    establishments: pd.DataFrame,
    model_settings: None = None,
    model_settings_file_name: str = "route_generation.yaml",
    trace_label: str = "route_generation",
) -> None:
    """
    Generate routes from (pseudo-)establishments.

    Each (pseudo-)establishment generates N "routes". A route is the commercial
    work for a vehicle in a day.  A route is not a single closed tour in the
    usual modeling sense, as it can potentially return to originating depot
    multiple times, and/or end the day at a terminal location that is not the
    same as the originating depot.

    The result of running this component is the creation of the routes table.
    It contains very little information at creation, just the columns
    {'route_id', 'establishment_id', 'business_type'}.

    Parameters
    ----------
    state : State
    establishments : pandas.DataFrame
    """

    if model_settings is None:
        model_settings = state.filesystem.read_settings_file(model_settings_file_name)

    trace_label = "cvm_route_generation"

    logger.info("Running %s with synthetic establishments", trace_label)

    max_n_routes_per_business_type = model_settings.get(
        "MAX_N_ROUTES_PER_ESTABLISHMENT"
    )

    # TODO: interface with ActivitySim repro-random
    rng = np.random.default_rng(seed=42)

    establishments_df = establishments.copy()
    establishments_df["n_routes"] = 0

    # step 1 Binary logit model that predicts whether an establishment has at least one route.
    # Necessary because there are many which have none in the survey.
    logger.info("Running %s step 1 binary logit model", trace_label)
    # get the industry dictionary from model spec
    industry_dict = model_settings.get("industries")
    establishments_df["industry_group"] = establishments_df["industry_number"].map(
        industry_dict
    )
    establishments_df["industry_group"] = establishments_df["industry_group"].apply(
        lambda x: x.get("industry_group") if x is not None else 0
    )
    # get the industry group specific beta from model spec
    industry_group_dict = model_settings.get("industry_groups")
    _industry_group = establishments_df["industry_group"].map(industry_group_dict)
    establishments_df["beta_industry_group"] = _industry_group.apply(
        lambda x: x.get("beta_employment") if x is not None else 0
    )
    establishments_df["constant"] = _industry_group.apply(
        lambda x: x.get("constant") if x is not None else 0
    )

    # calculate the probability of generating at least one route
    establishments_df["has_generation_probability"] = 1 / (
        1
        + np.exp(
            -establishments_df["beta_industry_group"] * establishments_df["employees"]
            - establishments_df["constant"]
        )
    )
    # get random numbers for the binary logit model
    establishments_df["random"] = state.get_rn_generator().random_for_df(establishments)
    # calculate whether the establishment generates a route
    establishments_df["has_generation"] = (
        establishments_df["has_generation_probability"] > establishments_df["random"]
    )

    establishments["has_generation"] = establishments_df["has_generation"]

    tracing.print_summary(
        "has_generation",
        establishments["has_generation"],
        value_counts=True,
    )

    # step 2 preliminary estimated # routes for establishments generating at least one route
    # takes the form of sqrt()
    logger.info("Running %s step 2 preliminary estimated # routes", trace_label)
    beta_generation = model_settings.get("CONSTANTS").get("beta_employment")
    establishments_df["n_routes"] = np.where(
        establishments_df["has_generation"],
        np.sqrt(establishments_df["employees"]) * beta_generation,
        0,
    )

    # step 3 Industry factors.
    # These are estimated industry specific factors
    # which modify the predictions from the second step to produce the final set of outputs.
    logger.info("Running %s step 3 apply industry factors", trace_label)
    # get the industry dictionary from model spec
    industry_dict = model_settings.get("industries")
    industry_factor = {}
    for industry_number, industry_info in industry_dict.items():
        industry_factor[industry_number] = industry_info.get("industry_factor")
    # apply the industry effect to the number of routes
    establishments_df["n_routes"] *= establishments_df["industry_number"].map(
        industry_factor
    )
    establishments_df["n_routes"] = establishments_df["n_routes"].fillna(0)
    # round the number of routes to integer
    establishments_df["n_routes"] = establishments_df["n_routes"].round().astype(int)

    # cap the number of routes to the maximum number of routes per establishment
    establishments_df["n_routes"] = np.where(
        establishments_df["n_routes"] > max_n_routes_per_business_type,
        max_n_routes_per_business_type,
        establishments_df["n_routes"],
    )

    establishments["n_routes"] = establishments_df["n_routes"]

    # write establishments table back to state
    state.add_table("establishments", establishments)

    # generate routes table
    route_estab_id = []
    route_btype = []
    for b in BusinessTypes:
        establishments_sub_df = establishments_df[
            establishments_df["industry_name"] == b.name
        ].copy()
        n_routes_btype = establishments_sub_df["n_routes"].sum()
        route_estab_id.append(
            np.repeat(establishments_sub_df.index, establishments_sub_df["n_routes"])
        )
        route_btype.append(np.full(n_routes_btype, fill_value=b.value, dtype=np.int8))
    routes = pd.DataFrame(
        {
            "establishment_id": np.concatenate(route_estab_id),
            "business_type": as_int_enum(
                np.concatenate(route_btype), BusinessTypes, categorical=True
            ),
        }
    )
    route_id = routes.groupby(["establishment_id", "business_type"]).cumcount()
    route_id += (
        routes["establishment_id"] * _business_type_offset
        + routes["business_type"].cat.codes
    ) * max_n_routes_per_business_type
    routes = routes.set_index(pd.Index(route_id, name="route_id"))
    state.add_table("routes", routes)
    state.get_rn_generator().add_channel("routes", routes)


@workflow.temp_table
def routes_merged(
    state: workflow.State,  # noqa: F841
    routes: pd.DataFrame,
    establishments: pd.DataFrame,
) -> pd.DataFrame:
    """
    Join routes with establishment table.

    Parameters
    ----------
    state : State
    routes : DataFrame
    establishments : DataFrame

    Returns
    -------
    DataFrame
    """
    return simple_table_join(
        routes,
        establishments,
        left_on="establishment_id",
    )
