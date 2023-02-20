# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np
import pandas as pd

from activitysim.core import tracing
from activitysim.core import config
from activitysim.core import pipeline
from activitysim.core import simulate
from activitysim.core import inject
from activitysim.core import expressions

from activitysim.abm.models.util import estimation

logger = logging.getLogger(__name__)


def determine_closest_external_station(choosers, skim_dict, origin_col="home_zone_id"):

    unique_origin_zones = choosers[origin_col].unique()
    landuse = inject.get_table("land_use").to_frame()
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
    model_settings, estimator, choosers, network_los, chunk_size, trace_label
):

    constants = config.get_model_constants(model_settings)

    locals_d = {}
    if constants is not None:
        locals_d.update(constants)
    locals_d.update({"land_use": inject.get_table("land_use").to_frame()})

    skim_dict = network_los.get_default_skim_dict()
    # print('skim_dict', skim_dict)
    choosers = determine_closest_external_station(choosers, skim_dict)

    # - preprocessor
    preprocessor_settings = model_settings.get("preprocessor", None)
    if preprocessor_settings:
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
        estimator.write_model_settings(model_settings, model_settings['_yaml_file_name'])
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df, model_settings)
        estimator.write_choosers(choosers)

    choices = simulate.simple_simulate(
        choosers=choosers,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=locals_d,
        chunk_size=chunk_size,
        trace_label=trace_label,
        trace_choice_name=trace_label,
        estimator=estimator,
    )

    return choices


@inject.step()
def external_worker_identification(
    persons_merged, persons, network_los, chunk_size, trace_hh_id
):
    """
    This model predicts the whether a worker has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external worker model is the external_worker_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = "external_worker_identification"
    model_settings_file_name = "external_worker_identification.yaml"
    model_settings = config.read_model_settings(model_settings_file_name)
    model_settings['_yaml_file_name'] = model_settings_file_name

    estimator = estimation.manager.begin_estimation(trace_label)

    choosers = persons_merged.to_frame()
    filter_col = model_settings.get("CHOOSER_FILTER_COLUMN_NAME")
    choosers = choosers[choosers[filter_col]]
    logger.info("Running %s with %d persons", trace_label, len(choosers))

    choices = external_identification(
        model_settings, estimator, choosers, network_los, chunk_size, trace_label
    )

    external_col_name = model_settings["EXTERNAL_COL_NAME"]
    internal_col_name = model_settings["INTERNAL_COL_NAME"]

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "persons", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    persons = persons.to_frame()
    persons[external_col_name] = (
        (choices == 0).reindex(persons.index).fillna(False).astype(bool)
    )
    persons[internal_col_name] = persons[filter_col] & ~persons[external_col_name]

    pipeline.replace_table("persons", persons)

    tracing.print_summary(
        external_col_name, persons[external_col_name], value_counts=True
    )

    if trace_hh_id:
        tracing.trace_df(persons, label=trace_label, warn_if_empty=True)


@inject.step()
def external_student_identification(
    persons_merged, persons, network_los, chunk_size, trace_hh_id
):
    """
    This model predicts the whether a student has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_student_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = "external_student_identification"
    model_settings_file_name = "external_student_identification.yaml"
    model_settings = config.read_model_settings(model_settings_file_name)
    model_settings['_yaml_file_name'] = model_settings_file_name

    estimator = estimation.manager.begin_estimation(trace_label)

    choosers = persons_merged.to_frame()
    filter_col = model_settings.get("CHOOSER_FILTER_COLUMN_NAME")
    choosers = choosers[choosers[filter_col]]

    choices = external_identification(
        model_settings, estimator, choosers, network_los, chunk_size, trace_label
    )

    external_col_name = model_settings["EXTERNAL_COL_NAME"]
    internal_col_name = model_settings["INTERNAL_COL_NAME"]

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "persons", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    persons = persons.to_frame()
    persons[external_col_name] = (
        (choices == 0).reindex(persons.index).fillna(False).astype(bool)
    )
    persons[internal_col_name] = persons[filter_col] & ~persons[external_col_name]

    pipeline.replace_table("persons", persons)

    tracing.print_summary(
        external_col_name, persons[external_col_name], value_counts=True
    )

    if trace_hh_id:
        tracing.trace_df(persons, label=trace_label, warn_if_empty=True)


def set_external_tour_variables(tours, choices, model_settings, trace_label):
    """
    Set the internal and external tour indicator columns in the tours file
    """
    external_col_name = model_settings["EXTERNAL_COL_NAME"]
    internal_col_name = model_settings["INTERNAL_COL_NAME"]

    tours = tours.to_frame()

    tours.loc[choices.index, external_col_name] = (
        (choices == 0).reindex(tours.index).fillna(False).astype(bool)
    )
    tours.loc[choices.index, internal_col_name] = np.where(
        tours.loc[choices.index, external_col_name], False, True
    )

    # - annotate tours table
    if "annotate_tours" in model_settings:
        expressions.assign_columns(
            df=tours,
            model_settings=model_settings.get("annotate_tours"),
            trace_label=tracing.extend_trace_label(trace_label, "annotate_tours"),
        )

    return tours


@inject.step()
def external_non_mandatory_identification(
    tours_merged, tours, network_los, chunk_size, trace_hh_id
):
    """
    This model predicts the whether a non-mandatory tour is external.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_nonmandatory_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = "external_non_mandatory_identification"
    model_settings_file_name = "external_non_mandatory_identification.yaml"
    model_settings = config.read_model_settings(model_settings_file_name)
    model_settings['_yaml_file_name'] = model_settings_file_name

    estimator = estimation.manager.begin_estimation(trace_label)

    choosers = tours_merged.to_frame()
    choosers = choosers[choosers["tour_category"] == "non_mandatory"]

    choices = external_identification(
        model_settings, estimator, choosers, network_los, chunk_size, trace_label
    )

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, "tours", trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    tours = set_external_tour_variables(tours, choices, model_settings, trace_label)

    pipeline.replace_table("tours", tours)

    external_col_name = model_settings["EXTERNAL_COL_NAME"]
    tracing.print_summary(
        external_col_name, tours[external_col_name], value_counts=True
    )

    if trace_hh_id:
        tracing.trace_df(tours, label=trace_label, warn_if_empty=True)


@inject.step()
def external_joint_tour_identification(
    tours_merged, tours, network_los, chunk_size, trace_hh_id
):
    """
    This model predicts the whether a joint tour is external.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_nonmandatory_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = "external_joint_tour_identification"
    model_settings_file_name = "external_joint_tour_identification.yaml"
    model_settings = config.read_model_settings(model_settings_file_name)
    model_settings['_yaml_file_name'] = model_settings_file_name

    estimator = estimation.manager.begin_estimation(trace_label)

    choosers = tours_merged.to_frame()
    choosers = choosers[choosers["tour_category"] == "joint"]

    # - if no choosers
    if choosers.shape[0] > 0:
        choices = external_identification(
            model_settings, estimator, choosers, network_los, chunk_size, trace_label
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

    tours = set_external_tour_variables(tours, choices, model_settings, trace_label)

    pipeline.replace_table("tours", tours)

    external_col_name = model_settings["EXTERNAL_COL_NAME"]
    tracing.print_summary(
        external_col_name, tours[external_col_name], value_counts=True
    )

    if trace_hh_id:
        tracing.trace_df(tours, label=trace_label, warn_if_empty=True)
