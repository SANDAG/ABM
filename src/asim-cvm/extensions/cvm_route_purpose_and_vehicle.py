from functools import reduce
import logging
from pathlib import Path

import numba as nb
import numpy as np
import pandas as pd

from activitysim.core import config, estimation, simulate, workflow, logit, tracing
from activitysim.core.configuration.logit import LogitComponentSettings

from .cvm_enum import (
    BusinessTypes,
    CustomerTypes,
    RoutePurposes,
    VehicleTypes,
    VehicleTypes_ABM3,
)
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


class RouteGenerationSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `route_generation` component.
    """

    ALTS: str
    """The name of the file containing the alternatives."""

    CVM_ABM3_VEHICLE_DISTRIBUTION_FILE: str
    """The name of the file containing the vehicle distribution."""


logger = logging.getLogger(__name__)


@workflow.step
def route_purpose_and_vehicle(
    state: State,
    routes: pd.DataFrame,
    model_settings: RouteGenerationSettings | None = None,
    model_settings_file_name: str = "route_vehicle_purpose_customer.yaml",
    trace_label: str = "route_vehicle_purpose_customer",
) -> None:
    """
    Simulate purpose, customer, and vehicle type for routes.

    Parameters
    ----------
    state : State
    routes : pandas.DataFrame
    model_settings : default None
    model_settings_file_name : str, default "route_vehicle_purpose_customer.yaml"
    trace_label : str, default "route_vehicle_purpose_customer"

    Returns
    -------

    """

    if model_settings is None:
        model_settings = RouteGenerationSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    trace_label = "route_vehicle_purpose_customer"

    logger.info("Running %s", trace_label)

    estimator = estimation.manager.begin_estimation(state, trace_label)

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    model_spec = simulate.eval_coefficients(
        state, model_spec, coefficients_df, estimator
    )

    nest_spec = config.get_logit_model_settings(model_settings)

    choices = simulate.simple_simulate(
        state,
        choosers=routes,
        spec=model_spec,
        nest_spec=nest_spec,
        trace_label=trace_label,
        estimator=estimator,
    )

    # convert indexes to alternative names
    choices = pd.Series(model_spec.columns[choices.values], index=choices.index)

    tracing.print_summary(
        "purpuse_customer_vehicle choice",
        choices,
        value_counts=True,
    )

    # get the alternatives file
    alternatives_df = simulate.read_model_alts(
        state, model_settings.ALTS, set_index="alt"
    )
    alternatives_df["route_purpose"] = as_int_enum(
        alternatives_df["route_purpose"], RoutePurposes, categorical=True
    )
    alternatives_df["customer_type"] = as_int_enum(
        alternatives_df["customer_type"], CustomerTypes, categorical=True
    )
    alternatives_df["vehicle_type"] = as_int_enum(
        alternatives_df["vehicle_type"], VehicleTypes, categorical=True
    )
    # join the choices with alternatives
    choices = choices.to_frame("choice").merge(
        alternatives_df, left_on="choice", right_index=True
    )

    # assign the choices to the routes
    routes["route_purpose"] = choices["route_purpose"]
    routes["customer_type"] = choices["customer_type"]
    routes["vehicle_type"] = choices["vehicle_type"]

    # use pre-determined vehicle distribution to ABM3
    # read the vehicle distribution
    distribution_file_ = state.filesystem.get_config_file_path(
        model_settings.CVM_ABM3_VEHICLE_DISTRIBUTION_FILE
    )
    vehicle_distribution_df = pd.read_csv(distribution_file_)
    vehicle_distribution_df["vehicle_type"] = as_int_enum(
        vehicle_distribution_df["vehicle_type"], VehicleTypes, categorical=True
    )
    vehicle_distribution_df = vehicle_distribution_df.set_index(
        ["is_tnc", "vehicle_type"]
    )
    # create is_tnc column
    routes["is_tnc"] = routes["business_type"].isin(
        [
            BusinessTypes.TNCNRR.name,
            BusinessTypes.TNCRES.name,
            BusinessTypes.TNCRET.name,
        ]
    )
    # join the vehicle type distribution to the routes on is_tnc and vehicle_type
    _routes = routes.merge(
        vehicle_distribution_df, 
        how="left",
        left_on=["is_tnc", "vehicle_type"], 
        right_index=True
    )
    # this is the probability of each vehicle type for each route
    _probs = _routes[vehicle_distribution_df.columns]
    # make choices
    _choices, _rands = logit.make_choices(state, _probs, trace_label=trace_label)

    # assign the choices to the routes
    _choices = pd.Series(
        vehicle_distribution_df.columns[_choices.values], index=_choices.index
    )
    _choices = as_int_enum(_choices, VehicleTypes_ABM3, categorical=True)
    routes["vehicle_type_abm3"] = _choices

    state.add_table("routes", routes)
