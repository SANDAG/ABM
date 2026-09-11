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


class RentalCarChoiceSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `rental_car_choice` component.
    """

    preprocessor: PreprocessorSettings | None = None
    """Setting for the preprocessor."""

    RENTAL_CAR_CHOICE_ALT: int = 1
    """The column index number of the spec file for renting a car."""

    CHOOSER_FILTER_COLUMN_NAME: str | None = None
    """Column name to filter choosers (e.g., 'visitor' to apply only to visitors)."""


@workflow.step
def rental_car_choice(
    state: workflow.State,
    tours_merged: pd.DataFrame,
    tours: pd.DataFrame,
    model_settings: RentalCarChoiceSettings | None = None,
    model_settings_file_name: str = "rental_car_choice.yaml",
    trace_label: str = "rental_car_choice",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts whether a tour will rent a car.
    The output from this model is TRUE (1) or FALSE (0).
    
    Parameters
    ----------
    state : workflow.State
    tours_merged : DataFrame
        Merged tours table with household and person attributes
    tours : DataFrame
        Tours table to update with rental car choice results
    model_settings : RentalCarChoiceSettings, optional
        The settings used in this model component.  If not provided, they are
        loaded out of the configs directory YAML file referenced by
        the `model_settings_file_name` argument.
    model_settings_file_name : str, default "rental_car_choice.yaml"
        This is where model settings are found if `model_settings` is not given
    trace_label : str
        Label for tracing
    trace_hh_id : bool
        Whether to trace household
    """

    if model_settings is None:
        model_settings = RentalCarChoiceSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    choosers = tours_merged
    logger.info("Running %s with %d tours", trace_label, len(choosers))

    estimator = estimation.manager.begin_estimation(state, "rental_car_choice")

    constants = config.get_model_constants(model_settings)
    rental_car_choice_alt = model_settings.RENTAL_CAR_CHOICE_ALT

    # - preprocessor (run this FIRST to create filter columns if needed)
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

    # Filter choosers if specified (e.g., only visitors)
    # This must come AFTER preprocessor so filter columns are created
    if model_settings.CHOOSER_FILTER_COLUMN_NAME:
        filter_column = model_settings.CHOOSER_FILTER_COLUMN_NAME
        if filter_column in choosers.columns:
            choosers = choosers[choosers[filter_column]]
            logger.info(
                "Filtered to %d tours where %s is True",
                len(choosers),
                filter_column,
            )
        else:
            logger.warning(
                "Filter column '%s' not found in choosers, proceeding without filtering",
                filter_column,
            )

    if len(choosers) == 0:
        logger.warning("No choosers for %s", trace_label)
        # Add rental_car_choice column with all zeros if no choosers
        tours["rental_car_choice"] = 0
        state.add_table("tours", tours)
        return

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df)
        estimator.write_choosers(choosers)

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
        trace_choice_name="rental_car_choice",
        estimator=estimator,
        compute_settings=model_settings.compute_settings,
    )

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(
            choices, "tours", "rental_car_choice"
        )  # override choices with survey values
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    # Update tours table with rental car choice results
    # Initialize all tours with 0 (no rental car)
    tours["rental_car_choice"] = 0
    
    # Set rental car choice for tours that were in the choice set
    # Use Series assignment to ensure proper index alignment
    tours.loc[choices.index, "rental_car_choice"] = choices

    state.add_table("tours", tours)

    tracing.print_summary(
        "rental_car_choice", tours.rental_car_choice, value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(
            tours, label=trace_label, slicer="NONE", warn_if_empty=True
        )
