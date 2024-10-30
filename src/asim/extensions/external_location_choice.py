# ActivitySim
# See full license in LICENSE.txt.
from __future__ import annotations

import logging

import numpy as np
import pandas as pd

from activitysim.abm.models.util import logsums as logsum
from activitysim.abm.models.util import tour_destination
from activitysim.abm.tables import shadow_pricing
from activitysim.core import (
    config,
    expressions,
    los,
    estimation,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.logit import (
    TourLocationComponentSettings,
    TourModeComponentSettings,
)
from activitysim.abm.models.location_choice import (
    write_estimation_specs,
    iterate_location_choice,
)
from activitysim.core.util import assign_in_place

logger = logging.getLogger(__name__)


@workflow.step
def external_school_location(
    state: workflow.State,
    persons_merged: pd.DataFrame,
    persons: pd.DataFrame,
    households: pd.DataFrame,
    network_los: los.Network_LOS,
    locutor: bool,
    model_settings: TourLocationComponentSettings | None = None,
    model_settings_file_name: str = "external_school_location.yaml",
    trace_label: str = "external_school_location",
):
    """
    External school location choice model

    iterate_location_choice adds location choice column and annotations to persons table
    """
    if model_settings is None:
        model_settings = TourLocationComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(state, "external_school_location")
    if estimator:
        write_estimation_specs(estimator, model_settings, model_settings_file_name)

    persons_df = iterate_location_choice(
        state=state,
        model_settings=model_settings,
        persons_merged=persons_merged,
        persons=persons,
        households=households,
        network_los=network_los,
        estimator=estimator,
        chunk_size=state.settings.chunk_size,
        locutor=locutor,
        trace_label=trace_label,
    )

    state.add_table("persons", persons_df)

    if estimator:
        estimator.end_estimation()


@workflow.step
def external_workplace_location(
    state: workflow.State,
    persons_merged: pd.DataFrame,
    persons: pd.DataFrame,
    households: pd.DataFrame,
    network_los: los.Network_LOS,
    locutor: bool,
    model_settings: TourLocationComponentSettings | None = None,
    model_settings_file_name: str = "external_workplace_location.yaml",
    trace_label: str = "external_workplace_location",
):
    """
    External workplace location choice model

    iterate_location_choice adds location choice column and annotations to persons table
    """

    if model_settings is None:
        model_settings = TourLocationComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    estimator = estimation.manager.begin_estimation(
        state, "external_workplace_location"
    )
    if estimator:
        write_estimation_specs(estimator, model_settings, model_settings_file_name)

    persons_df = iterate_location_choice(
        state=state,
        model_settings=model_settings,
        persons_merged=persons_merged,
        persons=persons,
        households=households,
        network_los=network_los,
        estimator=estimator,
        chunk_size=state.settings.chunk_size,
        locutor=locutor,
        trace_label=trace_label,
    )

    state.add_table("persons", persons_df)

    if estimator:
        estimator.end_estimation()


@workflow.step
def external_non_mandatory_destination(
    state: workflow.State,
    tours: pd.DataFrame,
    persons_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: TourLocationComponentSettings | None = None,
    model_settings_file_name: str = "external_non_mandatory_destination.yaml",
    trace_label: str = "external_non_mandatory_destination",
):
    """
    Given the tour generation from the above, each tour needs to have a
    destination, so in this case tours are the choosers (with the associated
    person that's making the tour)
    """
    if model_settings is None:
        model_settings = TourLocationComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    logsum_column_name = model_settings.DEST_CHOICE_LOGSUM_COLUMN_NAME
    want_logsums = logsum_column_name is not None

    sample_table_name = model_settings.DEST_CHOICE_SAMPLE_TABLE_NAME
    want_sample_table = (
        state.settings.want_dest_choice_sample_tables and sample_table_name is not None
    )

    # choosers are tours - in a sense tours are choosing their destination
    non_mandatory_ext_tours = tours[
        (tours.tour_category == "non_mandatory") & (tours.is_external_tour)
    ]

    # if no external non-mandatory tours
    if non_mandatory_ext_tours.shape[0] == 0:
        tracing.no_results(trace_label)
        return

    estimator = estimation.manager.begin_estimation(
        state, "external_non_mandatory_destination"
    )
    if estimator:
        estimator.write_coefficients(model_settings=model_settings)
        # estimator.write_spec(model_settings, tag='SAMPLE_SPEC')
        estimator.write_spec(model_settings, tag="SPEC")
        estimator.set_alt_id(model_settings.ALT_DEST_COL_NAME)
        estimator.write_table(
            state.get_injectable("size_terms"), "size_terms", append=False
        )
        estimator.write_table(state.get_table("land_use"), "landuse", append=False)
        estimator.write_model_settings(model_settings, model_settings_file_name)

    choices_df, save_sample_df = tour_destination.run_tour_destination(
        state,
        non_mandatory_ext_tours,
        persons_merged,
        want_logsums,
        want_sample_table,
        model_settings,
        network_los,
        estimator,
        trace_label,
    )

    if estimator:
        estimator.write_choices(choices_df.choice)
        choices_df.choice = estimator.get_survey_values(
            choices_df.choice, "tours", "destination"
        )
        estimator.write_override_choices(choices_df.choice)
        estimator.end_estimation()

    non_mandatory_ext_tours["destination"] = choices_df.choice

    assign_in_place(tours, non_mandatory_ext_tours[["destination"]])

    if want_logsums:
        non_mandatory_ext_tours[logsum_column_name] = choices_df["logsum"]
        assign_in_place(tours, non_mandatory_ext_tours[[logsum_column_name]])

    state.add_table("tours", tours)

    if want_sample_table:
        assert len(save_sample_df.index.get_level_values(0).unique()) == len(choices_df)
        # save_sample_df.set_index(model_settings['ALT_DEST_COL_NAME'], append=True, inplace=True)
        state.extend_table(sample_table_name, save_sample_df)

    if state.settings.trace_hh_id:
        state.tracing.trace_df(
            tours[tours.tour_category == "non_mandatory"],
            label="external_non_mandatory_destination",
            slicer="person_id",
            index_label="tour",
            columns=None,
            warn_if_empty=True,
        )


@workflow.step
def external_joint_tour_destination(
    state: workflow.State,
    tours: pd.DataFrame,
    persons_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: TourLocationComponentSettings | None = None,
    model_settings_file_name: str = "external_joint_tour_destination.yaml",
    trace_label: str = "external_joint_tour_destination",
):
    """
    Given the tour generation from the above, each tour needs to have a
    destination, so in this case tours are the choosers (with the associated
    person that's making the tour)
    """
    if model_settings is None:
        model_settings = TourLocationComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    logsum_column_name = model_settings.DEST_CHOICE_LOGSUM_COLUMN_NAME
    want_logsums = logsum_column_name is not None

    sample_table_name = model_settings.DEST_CHOICE_SAMPLE_TABLE_NAME
    want_sample_table = (
        state.settings.want_dest_choice_sample_tables and sample_table_name is not None
    )

    joint_ext_tours = tours[
        (tours.tour_category == "joint")
        & (tours.get("is_external_tour", False) == True)
    ]  # get needed incase no joint tours

    # if no external joint tours
    if joint_ext_tours.shape[0] == 0:
        tracing.no_results(trace_label)
        return

    estimator = estimation.manager.begin_estimation(
        state, "external_joint_tour_destination"
    )
    if estimator:
        estimator.write_coefficients(model_settings=model_settings)
        # estimator.write_spec(model_settings, tag='SAMPLE_SPEC')
        estimator.write_spec(model_settings, tag="SPEC")
        estimator.set_alt_id(model_settings.ALT_DEST_COL_NAME)
        estimator.write_table(
            state.get_injectable("size_terms"), "size_terms", append=False
        )
        estimator.write_table(state.get_table("land_use"), "landuse", append=False)
        estimator.write_model_settings(model_settings, model_settings_file_name)

    choices_df, save_sample_df = tour_destination.run_tour_destination(
        state,
        joint_ext_tours,
        persons_merged,
        want_logsums,
        want_sample_table,
        model_settings,
        network_los,
        estimator,
        trace_label,
    )

    if estimator:
        estimator.write_choices(choices_df.choice)
        choices_df.choice = estimator.get_survey_values(
            choices_df.choice, "tours", "destination"
        )
        estimator.write_override_choices(choices_df.choice)
        estimator.end_estimation()

    joint_ext_tours["destination"] = choices_df.choice

    assign_in_place(tours, joint_ext_tours[["destination"]])

    if want_logsums:
        joint_ext_tours[logsum_column_name] = choices_df["logsum"]
        assign_in_place(tours, joint_ext_tours[[logsum_column_name]])

    state.add_table("tours", tours)

    if want_sample_table:
        assert len(save_sample_df.index.get_level_values(0).unique()) == len(choices_df)
        # save_sample_df.set_index(model_settings['ALT_DEST_COL_NAME'], append=True, inplace=True)
        state.extend_table(sample_table_name, save_sample_df)

    if state.settings.trace_hh_id:
        state.tracing.trace_df(
            tours[tours.tour_category == "non_mandatory"],
            label="external_joint_tour_destination",
            slicer="person_id",
            index_label="tour",
            columns=None,
            warn_if_empty=True,
        )
