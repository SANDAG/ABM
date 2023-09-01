# ActivitySim
# See full license in LICENSE.txt.
from __future__ import annotations

import logging

import pandas as pd
from pydantic import validator

from activitysim.core import (
    los,
    config,
    estimation,
    expressions,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.input import read_input_table
from activitysim.core.configuration.base import PreprocessorSettings, PydanticReadable
from activitysim.core.configuration.logit import LogitComponentSettings

class HouseholdAttractorSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `household_attractor` component.
    """

    preprocessor: PreprocessorSettings | None = None
    """Setting for the preprocessor."""

    RESULT_COL_NAME: str
    """The name of the column in the route table that is created by this component."""

    HAS_ATTRACTION_ALT: int
    """The alternative number that indicates that the household has attraction."""

    annotate_land_use: PreprocessorSettings | None = None
    """Setting for annotation."""


logger = logging.getLogger(__name__)

@workflow.step
def household_attractor(
    state: workflow.State,
    households: pd.DataFrame,
    households_merged: pd.DataFrame,
    land_use: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: HouseholdAttractorSettings | None = None,
    model_settings_file_name: str = "household_attractor.yaml",
    trace_label: str = "household_attractor",
) -> None:
    """
    Calculate household attractor for commercial vehicle model.

    Parameters
    ----------
    state : workflow.State
    households : DataFrame
    households_merged : DataFrame
    network_los : los.Network_LOS
    model_settings : default None
    model_settings_file_name : str, default "cvm_household_attractor.yaml"
    trace_label : str, default "cvm_household_attractor"
    """

    # read the tours table from ABM and store in the state
    tours = read_input_table(state, "tours")
    state.add_table("tours", tours)

    if model_settings is None:
        model_settings = HouseholdAttractorSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    trace_label = "cvm_household_attractor"
    trace_hh_id = state.settings.trace_hh_id

    logger.info("Running %s with %d households", trace_label, len(households))

    estimator = estimation.manager.begin_estimation(state, trace_label)

    constants = model_settings.CONSTANTS or {}

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    model_spec = simulate.eval_coefficients(
        state, model_spec, coefficients_df, estimator
    )

    nest_spec = config.get_logit_model_settings(model_settings)

    # - preprocessor
    preprocessor_settings = model_settings.preprocessor
    if preprocessor_settings:
        locals_d = {}
        if constants is not None:
            locals_d.update(constants)
        
        expressions.assign_columns(
            state,
            df=households_merged,
            model_settings=preprocessor_settings,
            trace_label=trace_label,
        )

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(file_name=model_settings.SPEC)
        estimator.write_coefficients(
            coefficients_df, file_name=model_settings.COEFFICIENTS
        )
        estimator.write_choosers(households)
    
    for segment in ["package", "food", "service"]:
        households_merged["segment"] = segment

        choices = simulate.simple_simulate(
            state,
            choosers=households_merged,
            spec=model_spec,
            nest_spec=nest_spec,
            locals_d=constants,
            trace_label=trace_label,
            trace_choice_name=f"household_attractor.{segment}",
            estimator=estimator,
        )

        has_attraction_alt = model_settings.HAS_ATTRACTION_ALT

        choices = choices == has_attraction_alt

        if estimator:
            estimator.write_choices(choices)
            choices = estimator.get_survey_values(
                choices, "households", model_settings.RESULT_COL_NAME + "_" + segment
            )
            estimator.write_override_choices(choices)
            estimator.end_estimation()

        households[model_settings.RESULT_COL_NAME + "_" + segment] = (
            choices.reindex(households.index).fillna(0).astype(bool)
        )

        tracing.print_summary(
            model_settings.RESULT_COL_NAME + "_" + segment,
            households[model_settings.RESULT_COL_NAME + "_" + segment],
            value_counts=True,
        )

        if trace_hh_id:
            state.tracing.trace_df(households, label=f"household_attactor_{segment}", warn_if_empty=True)
    
    state.add_table("households", households)
    
    expressions.assign_columns(
        state,
        df=land_use,
        model_settings=model_settings.annotate_land_use,
        trace_label=tracing.extend_trace_label(trace_label, "annotate_land_use"),
    )

    state.add_table("land_use", land_use)
