# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np

from activitysim.abm.models.util import estimation
from activitysim.core import config, expressions, inject, pipeline, simulate, tracing

logger = logging.getLogger("activitysim")


@inject.step()
def av_ownership(households_merged, households, chunk_size, trace_hh_id):
    """
    This model predicts whether a household owns an autonomous vehicle.
    The output from this model is TRUE or FALSE.
    """

    trace_label = "av_ownership"
    model_settings_file_name = "av_ownership.yaml"

    choosers = households_merged.to_frame()
    model_settings = config.read_model_settings(model_settings_file_name)

    logger.info("Running %s with %d households", trace_label, len(choosers))

    estimator = estimation.manager.begin_estimation("av_ownership")

    constants = config.get_model_constants(model_settings)
    av_ownership_alt = model_settings.get("AV_OWNERSHIP_ALT", 0)

    # - preprocessor
    preprocessor_settings = model_settings.get("preprocessor", None)
    if preprocessor_settings:

        locals_d = {}
        if constants is not None:
            locals_d.update(constants)

        expressions.assign_columns(
            df=choosers,
            model_settings=preprocessor_settings,
            locals_dict=locals_d,
            trace_label=trace_label,
        )

    model_spec = simulate.read_model_spec(file_name=model_settings["SPEC"])
    coefficients_df = simulate.read_model_coefficients(model_settings)
    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df)
        estimator.write_choosers(choosers)

    # - iterative single process what-if adjustment if specified
    iterations = model_settings.get("AV_OWNERSHIP_ITERATIONS", 1)
    iterations_coefficient_constant = model_settings.get(
        "AV_OWNERSHIP_COEFFICIENT_CONSTANT", None
    )
    iterations_target_percent = model_settings.get("AV_OWNERSHIP_TARGET_PERCENT", None)
    iterations_target_percent_tolerance = model_settings.get(
        "AV_OWNERSHIP_TARGET_PERCENT_TOLERANCE", 0.01
    )

    for iteration in range(iterations):

        logger.info(
            "Running %s with %d households iteration %d",
            trace_label,
            len(choosers),
            iteration,
        )

        # re-read spec to reset substitution
        model_spec = simulate.read_model_spec(file_name=model_settings["SPEC"])
        model_spec = simulate.eval_coefficients(model_spec, coefficients_df, estimator)

        choices = simulate.simple_simulate(
            choosers=choosers,
            spec=model_spec,
            nest_spec=nest_spec,
            locals_d=constants,
            chunk_size=chunk_size,
            trace_label=trace_label,
            trace_choice_name="av_ownership",
            estimator=estimator,
        )

        if iterations_target_percent is not None:
            # choices_for_filter = choices[choosers[iterations_chooser_filter]]

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

    households = households.to_frame()
    households["av_ownership"] = (
        choices.reindex(households.index).fillna(0).astype(bool)
    )

    pipeline.replace_table("households", households)

    tracing.print_summary("av_ownership", households.av_ownership, value_counts=True)

    if trace_hh_id:
        tracing.trace_df(households, label=trace_label, warn_if_empty=True)
