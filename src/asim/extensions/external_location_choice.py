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
from activitysim.abm.models.location_choice import iterate_location_choice

logger = logging.getLogger(__name__)


@inject.step()
def external_school_location(
        persons_merged, persons, households,
        network_los,
        chunk_size, trace_hh_id, locutor
        ):
    """
    External school location choice model

    iterate_location_choice adds location choice column and annotations to persons table
    """

    trace_label = 'external_school_location'
    model_settings = config.read_model_settings('external_school_location.yaml')

    estimator = estimation.manager.begin_estimation('external_school_location')
    if estimator:
        write_estimation_specs(estimator, model_settings, 'external_school_location.yaml')

    persons_df = iterate_location_choice(
        model_settings,
        persons_merged, persons, households,
        network_los,
        estimator,
        chunk_size, trace_hh_id, locutor, trace_label
    )

    pipeline.replace_table("persons", persons_df)

    if estimator:
        estimator.end_estimation()


@inject.step()
def external_workplace_location(
        persons_merged, persons, households,
        network_los,
        chunk_size, trace_hh_id, locutor
        ):
    """
    External workplace location choice model

    iterate_location_choice adds location choice column and annotations to persons table
    """

    trace_label = 'external_workplace_location'
    model_settings = config.read_model_settings('external_workplace_location.yaml')

    estimator = estimation.manager.begin_estimation('external_workplace_location')
    if estimator:
        write_estimation_specs(estimator, model_settings, 'external_workplace_location.yaml')

    persons_df = iterate_location_choice(
        model_settings,
        persons_merged, persons, households,
        network_los,
        estimator,
        chunk_size, trace_hh_id, locutor, trace_label
    )

    pipeline.replace_table("persons", persons_df)

    if estimator:
        estimator.end_estimation()
