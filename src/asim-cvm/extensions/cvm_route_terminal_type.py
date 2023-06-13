# ActivitySim
# See full license in LICENSE.txt.
from __future__ import annotations

import logging

import pandas as pd
from pydantic import validator

from activitysim.core import (
    config,
    estimation,
    expressions,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.base import PreprocessorSettings, PydanticReadable
from activitysim.core.configuration.logit import LogitComponentSettings

logger = logging.getLogger(__name__)


class RouteStopTypeSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `route_terminal_type` component.
    """

    preprocessor: PreprocessorSettings | None = None
    """Setting for the preprocessor."""

    RESULT_COL_NAME: str
    """The name of the column in the route table that is created by this component."""


def route_endpoint_type(
    state: workflow.State,
    routes: pd.DataFrame,
    model_settings: RouteStopTypeSettings | None = None,
    model_settings_file_name: str = "route_terminal_type.yaml",
    trace_label: str = "route_terminal_type",
) -> None:
    """
    Determine for each route whether its terminal and depot can be different.

    Parameters
    ----------
    state : workflow.State
    routes : DataFrame
        This represents the 'choosers' table for this component.
    model_settings : RouteStopTypeSettings, optional
        The settings used in this model component.  If not provided, they are
        loaded out of the configs directory YAML file referenced by
        the `model_settings_file_name` argument.
    model_settings_file_name : str, default "route_terminal_type.yaml"
        This is where model setting are found if `model_settings` is not given
        explicitly.  The same filename is also used to write settings files to
        the estimation data bundle in estimation mode.
    trace_label : str, default "route_terminal_type"
        This label is used for various tracing purposes.
    """
    if model_settings is None:
        model_settings = RouteStopTypeSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    logger.info("Running %s with %d persons", trace_label, len(routes))

    estimator = estimation.manager.begin_estimation(state, trace_label)

    constants = model_settings.CONSTANTS or {}

    # - preprocessor
    preprocessor_settings = model_settings.preprocessor
    if preprocessor_settings:
        locals_d = {}
        if constants is not None:
            locals_d.update(constants)

        expressions.assign_columns(
            state,
            df=routes,
            model_settings=preprocessor_settings,
            locals_dict=locals_d,
            trace_label=trace_label,
        )

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    model_spec = simulate.eval_coefficients(
        state, model_spec, coefficients_df, estimator
    )

    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(file_name=model_settings.SPEC)
        estimator.write_coefficients(
            coefficients_df, file_name=model_settings.COEFFICIENTS
        )
        estimator.write_choosers(routes)

    choices = simulate.simple_simulate(
        state,
        choosers=routes,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=constants,
        trace_label=trace_label,
        trace_choice_name="route_terminal_type",
        estimator=estimator,
    )

    result_dtype = pd.CategoricalDtype(categories=model_spec.columns)
    choices = pd.Series(
        data=pd.Categorical.from_codes(choices, dtype=result_dtype), index=choices.index
    )

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(
            choices, "routes", model_settings.RESULT_COL_NAME
        )
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    routes[model_settings.RESULT_COL_NAME] = (
        choices.reindex(routes.index).fillna(model_spec.columns[0]).astype(result_dtype)
    )

    state.add_table("routes", routes)

    tracing.print_summary(
        model_settings.RESULT_COL_NAME,
        routes[model_settings.RESULT_COL_NAME],
        value_counts=True,
    )


@workflow.step
def route_terminal_type(
    state: workflow.State,
    routes: pd.DataFrame,
    model_settings: RouteStopTypeSettings | None = None,
    model_settings_file_name: str = "route_terminal_type.yaml",
    trace_label: str = "route_terminal_type",
) -> None:
    return route_endpoint_type(
        state,
        routes,
        model_settings,
        model_settings_file_name,
        trace_label,
    )



@workflow.step
def route_origination_type(
    state: workflow.State,
    routes: pd.DataFrame,
    model_settings: RouteStopTypeSettings | None = None,
    model_settings_file_name: str = "route_origination_type.yaml",
    trace_label: str = "route_origination_type",
) -> None:
    return route_endpoint_type(
        state,
        routes,
        model_settings,
        model_settings_file_name,
        trace_label,
    )
