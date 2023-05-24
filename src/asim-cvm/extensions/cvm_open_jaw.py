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


class OpenJawSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `cvm_open_jaw` component.
    """

    preprocessor: PreprocessorSettings | None = None
    """Setting for the preprocessor."""


@workflow.step
def open_jaw_route(
    state: workflow.State,
    routes: pd.DataFrame,
    model_settings: OpenJawSettings | None = None,
    model_settings_file_name: str = "open_jaw.yaml",
    trace_label: str = "open_jaw",
) -> None:
    """
    Determine for each route whether its terminal and depot can be different.

    Parameters
    ----------
    state : workflow.State
    routes : DataFrame
        This represents the 'choosers' table for this component.
    model_settings : OpenJawSettings, optional
        The settings used in this model component.  If not provided, they are
        loaded out of the configs directory YAML file referenced by
        the `model_settings_file_name` argument.
    model_settings_file_name : str, default "open_jaw.yaml"
        This is where model setting are found if `model_settings` is not given
        explicitly.  The same filename is also used to write settings files to
        the estimation data bundle in estimation mode.
    trace_label : str, default "open_jaw"
        This label is used for various tracing purposes.
    """
    if model_settings is None:
        model_settings = OpenJawSettings.read_settings_file(
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
        trace_choice_name="open_jaw",
        estimator=estimator,
    )

    choices = choices.astype(bool)

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(
            choices, "routes", "open_jaw"
        )
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    routes["open_jaw"] = (
        choices.reindex(routes.index).fillna(0).astype(bool)
    )

    state.add_table("routes", routes)

    tracing.print_summary(
        "open_jaw", routes.open_jaw, value_counts=True
    )

