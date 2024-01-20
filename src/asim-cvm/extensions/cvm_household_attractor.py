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

    segments: list[str]
    """The segments to run the model for."""

    HOUSEHOLD_SAMPLE_RATE_COLUMN: str
    """The name of the column in the input household table that contains the sample rate."""


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

    # - segments
    segments = model_settings.segments
    segment_dtype = pd.CategoricalDtype(categories=segments, ordered=False)

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

    for segment in model_settings.segments:
        households_merged["segment"] = pd.Series(
            segment, index=households_merged.index, dtype=segment_dtype
        )

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
            state.tracing.trace_df(
                households, label=f"household_attactor_{segment}", warn_if_empty=True
            )

    state.add_table("households", households)

    # summarize total household attractions by zone
    land_use["num_hh_food_delivery"] = households.groupby("home_zone_id")[
        model_settings.RESULT_COL_NAME + "_food"
    ].sum()
    land_use["num_hh_food_delivery"] = land_use["num_hh_food_delivery"].fillna(0)
    land_use["num_hh_package_delivery"] = households.groupby("home_zone_id")[
        model_settings.RESULT_COL_NAME + "_package"
    ].sum()
    land_use["num_hh_package_delivery"] = land_use["num_hh_package_delivery"].fillna(0)
    land_use["num_hh_service"] = households.groupby("home_zone_id")[
        model_settings.RESULT_COL_NAME + "_service"
    ].sum()
    land_use["num_hh_service"] = land_use["num_hh_service"].fillna(0)

    # scale household attraction by input household sample rate
    land_use["num_hh_food_delivery"] = (
        land_use["num_hh_food_delivery"]
        / households[model_settings.HOUSEHOLD_SAMPLE_RATE_COLUMN].iloc[0]
    )
    land_use["num_hh_package_delivery"] = (
        land_use["num_hh_package_delivery"]
        / households[model_settings.HOUSEHOLD_SAMPLE_RATE_COLUMN].iloc[0]
    )
    land_use["num_hh_service"] = (
        land_use["num_hh_service"]
        / households[model_settings.HOUSEHOLD_SAMPLE_RATE_COLUMN].iloc[0]
    )

    state.add_table("land_use", land_use)
