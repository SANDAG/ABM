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
from activitysim.core import workflow

from .cvm_enum import BusinessTypes
from .cvm_enum_tools import as_int_enum
from .cvm_state import State

logger = logging.getLogger(__name__)

_business_type_offset = int(10 ** np.ceil(np.log10(max(BusinessTypes))))


@workflow.step
def route_generation(
    state: State,
    establishments: pd.DataFrame,
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
    max_n_routes_per_business_type = 10000

    # TODO: interface with ActivitySim repro-random
    rng = np.random.default_rng(seed=42)

    ## WHOLESALE ## Some kind of parameterization p and r based on data....
    x_mean = np.clip(establishments["employees"], 1, 200)
    x_variance = x_mean * 1.2
    p = np.clip(x_mean / x_variance, 0.05, 0.95)
    r = np.square(x_mean) / np.clip(x_variance - x_mean, 0.05, np.inf)
    x = rng.negative_binomial(r, p)
    establishments["n_routes_wholesale"] = x
    n_routes_total = x

    ## GIG WORKERS ## Some kind of parameterization p and r based on data....
    x_mean = np.clip(establishments["employees"], 1, 200)
    x_variance = x_mean * 1.2
    p = np.clip(x_mean / x_variance, 0.05, 0.95)
    r = np.square(x_mean) / np.clip(x_variance - x_mean, 0.05, np.inf)
    x = rng.negative_binomial(r, p)
    establishments["n_routes_gigwork"] = x
    n_routes_total += x

    # write establishments table back to state
    state.add_table("establishments", establishments)

    # generate routes table
    route_estab_id = []
    route_btype = []
    for b in BusinessTypes:
        n_routes_btype = establishments[f"n_routes_{b.name}"].sum()
        route_estab_id.append(
            np.repeat(establishments.index, establishments[f"n_routes_{b.name}"])
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
