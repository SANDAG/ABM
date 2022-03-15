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


def determine_closest_external_station(choosers, skim_dict):

    unique_home_zones = choosers.home_zone_id.unique()
    landuse = inject.get_table('land_use').to_frame()
    print(landuse)
    ext_zones = landuse[landuse.external_MAZ > 0].index.to_numpy()
    print(ext_zones)

    choosers['closest_external_zone'] = -1
    choosers['dist_to_external_zone'] = 0

    for home_zone in unique_home_zones:
        # for ext_zone in ext_zones:
        #     print('home: ', home_zone, '\t external: ', ext_zone)
        #     print('home type: ', type(home_zone), '\t external type: ', type(ext_zone))
        #     print(skim_dict.lookup([home_zone], [ext_zone], 'DIST'))
        print(unique_home_zones)
        zone_distances = skim_dict.lookup([home_zone], ext_zones, 'DIST')
        # zone_distances = [skim_dict.lookup([home_zone], [ext_zone], 'DIST') for ext_zone in ext_zones]
        # closest_zone_idx = np.argmin(skim_dict.lookup([home_zone], ext_zones, 'DIST'))
        closest_zone = ext_zones[closest_zone_idx]
        dist_to_closest_zone = zone_distances[closest_zone_idx]
        print("closest_zone: ", closest_zone, '\t Distance:', dist_to_closest_zone)
        choosers.loc[choosers.home_zone_id == home_zone,
            'closest_external_zone'] = closest_zone
        choosers.loc[choosers.home_zone_id == home_zone,
            'dist_to_external_zone'] = dist_to_closest_zone

    return choosers



def external_identification(model_settings, choosers, network_los, chunk_size, trace_label):

    estimator = estimation.manager.begin_estimation(trace_label)

    constants = config.get_model_constants(model_settings)

    locals_d = {}
    if constants is not None:
        locals_d.update(constants)
    skim_dict = network_los.get_default_skim_dict()
    # print('skim_dict', skim_dict)
    # locals_d.update
    # choosers = determine_closest_external_station(choosers, skim_dict)

    # - preprocessor
    preprocessor_settings = model_settings.get('preprocessor', None)
    if preprocessor_settings:
        expressions.assign_columns(
            df=choosers,
            model_settings=preprocessor_settings,
            locals_dict=locals_d,
            trace_label=trace_label)

    model_spec = simulate.read_model_spec(file_name=model_settings['SPEC'])
    coefficients_df = simulate.read_model_coefficients(model_settings)
    model_spec = simulate.eval_coefficients(model_spec, coefficients_df, estimator)

    nest_spec = config.get_logit_model_settings(model_settings)

    if estimator:
        estimator.write_model_settings(model_settings, model_settings_file_name)
        estimator.write_spec(model_settings)
        estimator.write_coefficients(coefficients_df)
        estimator.write_choosers(choosers)

    choices = simulate.simple_simulate(
        choosers=choosers,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=locals_d,
        chunk_size=chunk_size,
        trace_label=trace_label,
        trace_choice_name=trace_label,
        estimator=estimator)

    if estimator:
        estimator.write_choices(choices)
        choices = estimator.get_survey_values(choices, 'persons', trace_label)
        estimator.write_override_choices(choices)
        estimator.end_estimation()

    return choices


@inject.step()
def external_worker_identification(
        persons_merged, persons, network_los,
        chunk_size, trace_hh_id):
    """
    This model predicts the whether a worker has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external worker model is the external_worker_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = 'external_worker_identification'
    model_settings_file_name = 'external_worker_identification.yaml'
    model_settings = config.read_model_settings(model_settings_file_name)

    choosers = persons_merged.to_frame()
    filter_col = model_settings.get('CHOOSER_FILTER_COLUMN_NAME')
    choosers = choosers[choosers[filter_col]]
    logger.info("Running %s with %d persons", trace_label, len(choosers))

    choices = external_identification(model_settings, choosers, network_los, chunk_size, trace_label)

    external_col_name = model_settings['EXTERNAL_COL_NAME']
    internal_col_name = model_settings['INTERNAL_COL_NAME']

    persons = persons.to_frame()
    persons[external_col_name] = choices.reindex(persons.index).fillna(0).astype(bool)
    persons[internal_col_name] = (persons[filter_col] & ~persons[external_col_name])

    pipeline.replace_table("persons", persons)

    tracing.print_summary(external_col_name, persons[external_col_name], value_counts=True)

    if trace_hh_id:
        tracing.trace_df(persons,
                         label=trace_label,
                         warn_if_empty=True)


@inject.step()
def external_student_identification(
        persons_merged, persons, network_los,
        chunk_size, trace_hh_id):
    """
    This model predicts the whether a student has an external work location.
    The output from this model is TRUE (if external) or FALSE (if internal).

    The main interface to the external student model is the external_student_identification() function.
    This function is registered as an orca step in the example Pipeline.
    """

    trace_label = 'external_student_identification'
    model_settings_file_name = 'external_student_identification.yaml'
    model_settings = config.read_model_settings(model_settings_file_name)

    choosers = persons_merged.to_frame()
    filter_col = model_settings.get('CHOOSER_FILTER_COLUMN_NAME')
    choosers = choosers[choosers[filter_col]]

    choices = external_identification(model_settings, choosers, network_los, chunk_size, trace_label)

    external_col_name = model_settings['EXTERNAL_COL_NAME']
    internal_col_name = model_settings['INTERNAL_COL_NAME']

    persons = persons.to_frame()
    persons[external_col_name] = choices.reindex(persons.index).fillna(0).astype(bool)
    persons[internal_col_name] = (persons[filter_col] & ~persons[external_col_name])

    pipeline.replace_table("persons", persons)

    tracing.print_summary(external_col_name, persons[external_col_name], value_counts=True)

    if trace_hh_id:
        tracing.trace_df(persons,
                         label=trace_label,
                         warn_if_empty=True)
