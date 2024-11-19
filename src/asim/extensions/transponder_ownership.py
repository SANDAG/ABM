# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np

from activitysim.core import tracing
from activitysim.core import config
from activitysim.core import pipeline
from activitysim.core import simulate
from activitysim.core import inject
from activitysim.core import expressions

from activitysim.abm.models.util import estimation

logger = logging.getLogger(__name__)


@inject.step()
def transponder_ownership(
    households_merged, households, network_los, chunk_size, trace_hh_id
):
    """
    This model predicts whether the household owns a transponder.
    The output from this model is TRUE (if yes) or FALSE (if no) and is stored
    in the transponder_ownership column in the households table

    The main interface to the Transponder Ownership model is the transponder_ownership() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = "transponder_ownership"
    model_settings_file_name = "transponder_ownership.yaml"
    model_settings = config.read_model_settings(model_settings_file_name)
    transponder_own_alt = model_settings['TRANSPONDER_OWNERSHIP_ALT']

    estimator = estimation.manager.begin_estimation("transponder_ownership")
    constants = config.get_model_constants(model_settings)

    choosers = households_merged.to_frame()
    logger.info("Running %s with %d households", trace_label, len(choosers))

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
    model_spec = simulate.eval_coefficients(model_spec, coefficients_df, estimator)

    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df, model_settings)
        estimator.write_choosers(choosers)

    choices = simulate.simple_simulate(
        choosers=choosers,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=constants,
        chunk_size=chunk_size,
        trace_label=trace_label,
        trace_choice_name="transponder_ownership",
        estimator=estimator,
    )
    choices = choices == transponder_own_alt

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(
            choices, "households", "transponder_ownership"
        )
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    households = households.to_frame()
    households["transponder_ownership"] = (
        choices.reindex(households.index).fillna(0).astype(bool)
    )
    pipeline.replace_table("households", households)

    tracing.print_summary(
        "transponder_ownership", households["transponder_ownership"], value_counts=True
    )

    if trace_hh_id:
        tracing.trace_df(households, label=trace_label, warn_if_empty=True)
