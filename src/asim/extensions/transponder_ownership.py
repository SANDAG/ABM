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
from activitysim.core.configuration.base import PreprocessorSettings
from activitysim.core.configuration.logit import LogitComponentSettings

logger = logging.getLogger(__name__)


class TransponderOwnershipSettings(LogitComponentSettings):
    """
    Settings for the `external_identification` component.
    """

    TRANSPONDER_OWNERSHIP_ALT: int = 1
    """Zero-based index of the column for owning a transponder in the model spec."""

    preprocessor: PreprocessorSettings | None = None


@workflow.step
def transponder_ownership(
    state: workflow.State,
    households: pd.DataFrame,
    households_merged: pd.DataFrame,
    model_settings: TransponderOwnershipSettings | None = None,
    model_settings_file_name: str = "transponder_ownership.yaml",
    trace_label: str = "transponder_ownership",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts whether the household owns a transponder.
    The output from this model is TRUE (if yes) or FALSE (if no) and is stored
    in the transponder_ownership column in the households table

    The main interface to the Transponder Ownership model is the transponder_ownership() function.
    This function is registered as an orca step in the example Pipeline.
    """
    if model_settings is None:
        model_settings = TransponderOwnershipSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )
    transponder_own_alt = model_settings.TRANSPONDER_OWNERSHIP_ALT

    estimator = estimation.manager.begin_estimation(state, "transponder_ownership")
    constants = config.get_model_constants(model_settings)

    choosers = households_merged
    logger.info("Running %s with %d households", trace_label, len(choosers))

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
    model_spec = simulate.eval_coefficients(
        state, model_spec, coefficients_df, estimator
    )

    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df, model_settings)
        estimator.write_choosers(choosers)

    choices = simulate.simple_simulate(
        state,
        choosers=choosers,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=constants,
        trace_label=trace_label,
        trace_choice_name="transponder_ownership",
        estimator=estimator,
        compute_settings=model_settings.compute_settings,
    )
    choices = choices == transponder_own_alt

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(
            choices, "households", "transponder_ownership"
        )
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    households["transponder_ownership"] = (
        choices.reindex(households.index).fillna(0).astype(bool)
    )
    state.add_table("households", households)

    tracing.print_summary(
        "transponder_ownership", households["transponder_ownership"], value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(households, label=trace_label, warn_if_empty=True)
