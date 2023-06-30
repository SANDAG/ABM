from functools import reduce
from pathlib import Path

import numba as nb
import numpy as np
import pandas as pd

from activitysim.core import workflow

from .cvm_enum import BusinessTypes, CustomerTypes, RoutePurposes, VehicleTypes
from .cvm_enum_tools import as_int_enum
from .cvm_state import State


@nb.njit
def single_choice_maker(pr, rn):
    """

    Parameters
    ----------
    pr : vector of float
    rn : float

    Returns
    -------

    """
    n_alts = pr.size
    z = rn
    for col in range(n_alts):
        z = z - pr[col]
        if z <= 0:
            return col
    else:
        # rare condition, only if a random point is greater than 1 (a bug)
        # or if the sum of probabilities is less than 1 and a random point
        # is greater than that sum, which due to the limits of numerical
        # precision can technically happen
        max_pr = 0.0
        out = 0
        for col in range(n_alts):
            if pr[col] > max_pr:
                out = col
                max_pr = pr[col]
        return out


@nb.njit
def cross_choice_maker(pr, rn, out=None):
    """

    Parameters
    ----------
    pr : vector of float
    rn : array of float

    Returns
    -------

    """
    if out is None:
        out = np.empty(rn.shape[0], dtype=np.int32)
    n_alts = pr.shape[0]
    for row in range(rn.shape[0]):
        z = rn[row]
        for col in range(n_alts):
            z = z - pr[col]
            if z <= 0:
                out[row] = col
                break
        else:
            # rare condition, only if a random point is greater than 1 (a bug)
            # or if the sum of probabilities is less than 1 and a random point
            # is greater than that sum, which due to the limits of numerical
            # precision can technically happen
            max_pr = 0.0
            for col in range(n_alts):
                if pr[col] > max_pr:
                    out[row] = col
                    max_pr = pr[col]
    return out


@workflow.step
def route_purpose_and_vehicle(
    state: State,
    routes: pd.DataFrame,
    probability_file: Path = "cvm_route_purpose_and_vehicle_probs.csv",
) -> None:
    """
    Simulate purpose and vehicle type for routes.

    Parameters
    ----------
    state : State
    routes : pandas.DataFrame
    probability_file

    Returns
    -------

    """
    probability_file_ = state.filesystem.get_config_file_path(probability_file)
    probs = pd.read_csv(probability_file_, header=1)

    probs["route_purpose"] = as_int_enum(
        probs["route_purpose"], RoutePurposes, categorical=True
    )
    probs["vehicle_type"] = as_int_enum(
        probs["vehicle_type"], VehicleTypes, categorical=True
    )
    probs["customer_type"] = as_int_enum(
        probs["customer_type"], CustomerTypes, categorical=True
    )

    # rescale probs to ensure they total 1.0
    probs_float_cols = probs.select_dtypes(include=float).columns
    probs[probs_float_cols] = probs[probs_float_cols].div(
        probs[probs_float_cols].sum(), axis=1
    )

    purposes = []
    vehicles = []
    customers = []
    for c in probs_float_cols:
        routes_c = routes.query(f"business_type == '{c}'")

        # TODO: repro random
        r = np.random.default_rng().random(size=len(routes_c))

        routes_c_choices = cross_choice_maker(probs[c].values, r)
        purposes.append(
            pd.Series(
                probs["route_purpose"].values[routes_c_choices], index=routes_c.index
            )
        )
        vehicles.append(
            pd.Series(
                probs["vehicle_type"].values[routes_c_choices], index=routes_c.index
            )
        )
        customers.append(
            pd.Series(
                probs["customer_type"].values[routes_c_choices], index=routes_c.index
            )
        )

    routes = routes.assign(
        route_purpose=pd.concat(purposes),
        vehicle_type=pd.concat(vehicles),
        customer_type=pd.concat(customers),
    )
    state.add_table("routes", routes)
