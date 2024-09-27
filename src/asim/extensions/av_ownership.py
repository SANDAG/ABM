# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np
import pandas as pd

from activitysim.core import (
    config,
    expressions,
    estimation,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.base import PreprocessorSettings, PydanticReadable
from activitysim.core.configuration.logit import LogitComponentSettings

logger = logging.getLogger("activitysim")


class AVOwnershipSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `transit_pass_subsidy` component.
    """

    preprocessor: PreprocessorSettings | None = None
    """Setting for the preprocessor."""

    AV_OWNERSHIP_ALT: int = 0
    """The column index number of the spec file for owning an autonomous vehicle."""

    # iterative what-if analysis example
    # omit these settings to not iterate
    AV_OWNERSHIP_ITERATIONS: int | None = 1
    """Maximum number of auto-calibration iterations to run."""
    AV_OWNERSHIP_TARGET_PERCENT: float | None = 0.0
    """Target percent of households owning an autonomous vehicle."""
    AV_OWNERSHIP_TARGET_PERCENT_TOLERANCE: float | None = 0.01
    """
    Tolerance for the target percent of households owning an autonomous vehicle.  
    Auto-calibration iterations will stop after achieving tolerance or hitting the max number.
    """
    AV_OWNERSHIP_COEFFICIENT_CONSTANT: str | None = "coef_av_target_share"
    """Name of the coefficient to adjust in each auto-calibration iteration."""


@workflow.step
def av_ownership(
    state: workflow.State,
    households_merged: pd.DataFrame,
    households: pd.DataFrame,
    model_settings: AVOwnershipSettings | None = None,
    model_settings_file_name: str = "av_ownership.yaml",
    trace_label: str = "av_ownership",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts whether a household owns an autonomous vehicle.
    The output from this model is TRUE or FALSE.
    """

    if model_settings is None:
        model_settings = AVOwnershipSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    choosers = households_merged
    logger.info("Running %s with %d households", trace_label, len(choosers))

    estimator = estimation.manager.begin_estimation(state, "av_ownership")

    constants = config.get_model_constants(model_settings)
    av_ownership_alt = model_settings.AV_OWNERSHIP_ALT

    # - preprocessor
    preprocessor_settings = model_settings.preprocessor
    if preprocessor_settings:

        locals_d = {}
        if constants is not None:
            locals_d.update(constants)

        expressions.assign_columns(
            state,
            df=choosers,
            model_settings=preprocessor_settings,
            locals_dict=locals_d,
            trace_label=trace_label,
        )

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df)
        estimator.write_choosers(choosers)

    # - iterative single process what-if adjustment if specified
    iterations = model_settings.AV_OWNERSHIP_ITERATIONS
    iterations_coefficient_constant = model_settings.AV_OWNERSHIP_COEFFICIENT_CONSTANT
    iterations_target_percent = model_settings.AV_OWNERSHIP_TARGET_PERCENT
    iterations_target_percent_tolerance = (
        model_settings.AV_OWNERSHIP_TARGET_PERCENT_TOLERANCE
    )

    # check to make sure all required settings are specified
    assert (
        iterations_coefficient_constant is not None if (iterations > 0) else True
    ), "AV_OWNERSHIP_COEFFICIENT_CONSTANT required if AV_OWNERSHIP_ITERATIONS is specified"
    assert (
        iterations_target_percent is not None if (iterations > 0) else True
    ), "AV_OWNERSHIP_TARGET_PERCENT required if AV_OWNERSHIP_ITERATIONS is specified"
    assert (
        iterations_target_percent_tolerance is not None if (iterations > 0) else True
    ), "AV_OWNERSHIP_TARGET_PERCENT_TOLERANCE required if AV_OWNERSHIP_ITERATIONS is specified"

    for iteration in range(iterations):

        logger.info(
            "Running %s with %d households iteration %d",
            trace_label,
            len(choosers),
            iteration,
        )

        # re-read spec to reset substitution
        model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
        model_spec = simulate.eval_coefficients(
            state, model_spec, coefficients_df, estimator
        )

        choices = simulate.simple_simulate(
            state,
            choosers=choosers,
            spec=model_spec,
            nest_spec=nest_spec,
            locals_d=constants,
            trace_label=trace_label,
            trace_choice_name="av_ownership",
            estimator=estimator,
            compute_settings=model_settings.compute_settings,
        )

        if iterations_target_percent is not None:

            current_percent = (choices == av_ownership_alt).sum() / len(choosers)
            logger.info(
                "Running %s iteration %i choosers %i current percent %f target percent %f",
                trace_label,
                iteration,
                len(choosers),
                current_percent,
                iterations_target_percent,
            )

            if current_percent <= (
                iterations_target_percent + iterations_target_percent_tolerance
            ) and current_percent >= (
                iterations_target_percent - iterations_target_percent_tolerance
            ):
                logger.info(
                    "Running %s iteration %i converged with coefficient %f",
                    trace_label,
                    iteration,
                    coefficients_df.value[iterations_coefficient_constant],
                )
                break

            else:
                new_value = (
                    np.log(
                        iterations_target_percent / np.maximum(current_percent, 0.0001)
                    )
                    + coefficients_df.value[iterations_coefficient_constant]
                )
                coefficients_df.value[iterations_coefficient_constant] = new_value
                logger.info(
                    "Running %s iteration %i new coefficient for next iteration %f",
                    trace_label,
                    iteration,
                    new_value,
                )
                iteration = iteration + 1

    choices = choices == av_ownership_alt

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "households", "av_ownership")
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    households["av_ownership"] = (
        choices.reindex(households.index).fillna(0).astype(bool)
    )

    state.add_table("households", households)

    tracing.print_summary("av_ownership", households.av_ownership, value_counts=True)

    if trace_hh_id:
        state.tracing.trace_df(households, label=trace_label, warn_if_empty=True)
