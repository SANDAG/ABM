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

import enum

import numpy as np
import pandas as pd

from activitysim.core import workflow

from .cvm_state import State


class BusinessTypes(enum.IntEnum):
    wholesale = 1
    gigwork = 2


_business_type_offset = int(10 ** np.ceil(np.log10(max(BusinessTypes))))


@workflow.step
def route_generation(
    state: State,
    establishments: pd.DataFrame,
) -> None:
    max_n_routes_per_business_type = 10000

    # TODO: interface with ActivitySim repro-random
    rng = np.random.default_rng(seed=42)

    ## WHOLESALE ## Some kind of parameterization p and r based on data....
    x_mean = np.clip(establishments["n_employees"], 1, 200)
    x_variance = x_mean * 1.2
    p = np.clip(x_mean / x_variance, 0.05, 0.95)
    r = np.square(x_mean) / np.clip(x_variance - x_mean, 0.05, np.inf)
    x = rng.negative_binomial(r, p)
    establishments["n_routes_wholesale"] = x
    n_routes_total = x

    ## GIG WORKERS ## Some kind of parameterization p and r based on data....
    x_mean = np.clip(establishments["n_small_ests"], 1, 200)
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
            "business_type": np.concatenate(route_btype),
        }
    )
    route_id = routes.groupby(["establishment_id", "business_type"]).cumcount()
    route_id += (
        routes["establishment_id"] * _business_type_offset + routes["business_type"]
    ) * max_n_routes_per_business_type
    routes = routes.set_index(route_id)
    state.add_table("routes", routes)

