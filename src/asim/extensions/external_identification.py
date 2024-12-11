# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np
import pandas as pd

from pydantic import validator

from activitysim.core import (
    config,
    expressions,
    los,
    estimation,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.logit import LogitComponentSettings
from activitysim.core.configuration.base import PreprocessorSettings

logger = logging.getLogger(__name__)


class ExternalIdentificationSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `external_identification` component.
    """

    CHOOSER_FILTER_COLUMN_NAME: str | None = None
    """Column name which selects choosers."""

    EXTERNAL_COL_NAME: str | None = None
    """Adds this column and set to True if model selects external"""

    INTERNAL_COL_NAME: str | None = None
    """Column name set to True if not external but CHOOSER_FILTER_COLUMN_NAME is True"""

    preprocessor: PreprocessorSettings | None = None


def determine_closest_external_station(
    state, choosers, skim_dict, origin_col="home_zone_id"
):

    unique_origin_zones = choosers[origin_col].unique()
    landuse = state.get_table("land_use")
    ext_zones = landuse[landuse.external_MAZ > 0].index.to_numpy()

    choosers["closest_external_zone"] = -1
    choosers["dist_to_external_zone"] = 0.0

    for origin_zone in unique_origin_zones:
        # FIXME in_skim check in skim_dictionary.py requires orig and dests to be the same shape in lookup
        orig_zones = np.full(shape=len(ext_zones), fill_value=origin_zone, dtype=int)
        zone_distances = skim_dict.lookup(orig_zones, ext_zones, "DIST")

        closest_zone_idx = np.argmin(zone_distances)
        closest_zone = int(ext_zones[closest_zone_idx])
        dist_to_closest_zone = zone_distances[closest_zone_idx]

        choosers.loc[
            choosers[origin_col] == origin_zone, "closest_external_zone"
        ] = closest_zone
        choosers.loc[
            choosers[origin_col] == origin_zone, "dist_to_external_zone"
        ] = dist_to_closest_zone

    return choosers


def external_identification(
    state,
    model_settings,
    estimator,
    choosers,
    network_los,
    model_settings_file_name,
    trace_label,
):

    constants = config.get_model_constants(model_settings)

    locals_d = {}
    if constants is not None:
        locals_d.update(constants)
    locals_d.update({"land_use": state.get_table("land_use")})

    skim_dict = network_los.get_default_skim_dict()
    choosers = determine_closest_external_station(state, choosers, skim_dict)

    # - preprocessor
    preprocessor_settings = model_settings.preprocessor
    if preprocessor_settings:
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
        locals_d=locals_d,
        trace_label=trace_label,
        trace_choice_name=trace_label,
        estimator=estimator,
        compute_settings=model_settings.compute_settings,
    )

    return choices


@workflow.step
def external_worker_identification(
    state: workflow.State,
    persons: pd.DataFrame,
    persons_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: ExternalIdentificationSettings | None = None,
    model_settings_file_name: str = "external_worker_identification.yaml",
    trace_label: str = "external_worker_identification",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts the whether a worker has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external worker model is the external_worker_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """
    if model_settings is None:
        model_settings = ExternalIdentificationSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(state, trace_label)

    filter_col = model_settings.CHOOSER_FILTER_COLUMN_NAME
    if filter_col is None:
        choosers = persons_merged
    else:
        choosers = persons_merged[persons_merged[filter_col]]
    logger.info("Running %s with %d persons", trace_label, len(choosers))

    choices = external_identification(
        state,
        model_settings,
        estimator,
        choosers,
        network_los,
        model_settings_file_name,
        trace_label,
    )

    external_col_name = model_settings.EXTERNAL_COL_NAME
    internal_col_name = model_settings.INTERNAL_COL_NAME

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "persons", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    if external_col_name is not None:
        persons[external_col_name] = (
            (choices == 0).reindex(persons.index).fillna(False).astype(bool)
        )
    if internal_col_name is not None:
        persons[internal_col_name] = persons[filter_col] & ~persons[external_col_name]

    state.add_table("persons", persons)

    tracing.print_summary(
        external_col_name, persons[external_col_name], value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(persons, label=trace_label, warn_if_empty=True)


@workflow.step
def external_student_identification(
    state: workflow.State,
    persons: pd.DataFrame,
    persons_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: ExternalIdentificationSettings | None = None,
    model_settings_file_name: str = "external_student_identification.yaml",
    trace_label: str = "external_student_identification",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts the whether a student has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_student_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    if model_settings is None:
        model_settings = ExternalIdentificationSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(state, trace_label)

    filter_col = model_settings.CHOOSER_FILTER_COLUMN_NAME
    if filter_col is None:
        choosers = persons_merged
    else:
        choosers = persons_merged[persons_merged[filter_col]]
    logger.info("Running %s with %d persons", trace_label, len(choosers))

    choices = external_identification(
        state,
        model_settings,
        estimator,
        choosers,
        network_los,
        model_settings_file_name,
        trace_label,
    )

    external_col_name = model_settings.EXTERNAL_COL_NAME
    internal_col_name = model_settings.INTERNAL_COL_NAME

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "persons", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    if external_col_name is not None:
        persons[external_col_name] = (
            (choices == 0).reindex(persons.index).fillna(False).astype(bool)
        )
    if internal_col_name is not None:
        persons[internal_col_name] = persons[filter_col] & ~persons[external_col_name]

    state.add_table("persons", persons)

    tracing.print_summary(
        external_col_name, persons[external_col_name], value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(persons, label=trace_label, warn_if_empty=True)


def set_external_tour_variables(state, tours, choices, model_settings, trace_label):
    """
    Set the internal and external tour indicator columns in the tours file
    """
    external_col_name = model_settings.EXTERNAL_COL_NAME
    internal_col_name = model_settings.INTERNAL_COL_NAME

    if external_col_name is not None:
        tours[external_col_name] = (
            (choices == 0).reindex(tours.index).fillna(False).astype(bool)
        )
    if internal_col_name is not None:
        tours[internal_col_name] = (
            (choices == 1).reindex(tours.index).fillna(True).astype(bool)
        )

    # - annotate tours table
    if "annotate_tours" in model_settings:
        expressions.assign_columns(
            state,
            df=tours,
            model_settings=model_settings.get("annotate_tours"),
            trace_label=tracing.extend_trace_label(trace_label, "annotate_tours"),
        )

    return tours


@workflow.step
def external_non_mandatory_identification(
    state: workflow.State,
    tours: pd.DataFrame,
    tours_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: ExternalIdentificationSettings | None = None,
    model_settings_file_name: str = "external_non_mandatory_identification.yaml",
    trace_label: str = "external_non_mandatory_identification",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts the whether a non-mandatory tour is external.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_nonmandatory_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """
    if model_settings is None:
        model_settings = ExternalIdentificationSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(state, trace_label)

    choosers = tours_merged[tours_merged["tour_category"] == "non_mandatory"]

    choices = external_identification(
        state,
        model_settings,
        estimator,
        choosers,
        network_los,
        model_settings_file_name,
        trace_label,
    )

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "tours", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    tours = set_external_tour_variables(
        state, tours, choices, model_settings, trace_label
    )

    state.add_table("tours", tours)

    external_col_name = model_settings.EXTERNAL_COL_NAME
    tracing.print_summary(
        external_col_name, tours[external_col_name], value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(tours, label=trace_label, warn_if_empty=True)


@workflow.step
def external_joint_tour_identification(
    state: workflow.State,
    tours: pd.DataFrame,
    tours_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: ExternalIdentificationSettings | None = None,
    model_settings_file_name: str = "external_joint_tour_identification.yaml",
    trace_label: str = "external_joint_tour_identification",
    trace_hh_id: bool = False,
) -> None:
    """
    This model predicts the whether a joint tour is external.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_nonmandatory_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """
    if model_settings is None:
        model_settings = ExternalIdentificationSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(state, trace_label)

    choosers = tours_merged[tours_merged["tour_category"] == "joint"]

    # - if no choosers
    if choosers.shape[0] > 0:
        choices = external_identification(
            state,
            model_settings,
            estimator,
            choosers,
            network_los,
            model_settings_file_name,
            trace_label,
        )
    else:
        # everything is internal, still want to set internal or external columns in df
        choices = pd.Series(1, index=choosers.index)
        tracing.no_results(trace_label)

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "tours", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    tours = set_external_tour_variables(
        state, tours, choices, model_settings, trace_label
    )

    state.add_table("tours", tours)

    external_col_name = model_settings.EXTERNAL_COL_NAME
    tracing.print_summary(
        external_col_name, tours[external_col_name], value_counts=True
    )

    if trace_hh_id:
        state.tracing.trace_df(tours, label=trace_label, warn_if_empty=True)
