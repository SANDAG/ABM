from __future__ import annotations

import logging

import pandas as pd
from pydantic import validator

from activitysim.core import (
    config,
    estimation,
    expressions,
    los,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.base import PreprocessorSettings, PydanticReadable
from activitysim.core.configuration.logit import (
    BaseLogitComponentSettings,
    LogitComponentSettings,
    PreprocessorSettings,
)
from activitysim.core.interaction_sample import interaction_sample
from activitysim.core.util import assign_in_place

from .cvm_enum import StopPurposes
from .cvm_enum_tools import as_int_enum
from .cvm_terminal_choice import SimpleLocationComponentSettings
from .cvm_route_terminal_type import route_endpoint_type, RouteStopTypeSettings

logger = logging.getLogger(__name__)


class StopLoopPurposeSettings(LogitComponentSettings, extra="forbid"):
    NEXT_PURP_COL: str = "next_stop_purpose"
    """The purpose for the next stop will be written to this column.
    
    This should be considered as a temporary variable, as it is expected to be
    available inside the stop loop but will be dropped from the route table when
    the stop loop exits.
    """

    PRIOR_PURP_COL: str = "prior_stop_purpose"
    """The purpose for the prior stop will be written to this column.
    
    This should be considered as a temporary variable, as it is expected to be
    available inside the stop loop but will be dropped from the route table when
    the stop loop exits.
    """


class StopLoopLocationSettings(SimpleLocationComponentSettings, extra="forbid"):
    TERMINAL_ZONE_COL_NAME: str = "terminal_zone"


class StopLoopComponentSettings(PydanticReadable, extra="forbid"):
    """
    Base configuration class for components that are non-logsum location choice models.
    """

    purpose_settings: StopLoopPurposeSettings
    location_type_settings: RouteStopTypeSettings
    location_settings: StopLoopLocationSettings

    # # Logsum-related settings
    # # CHOOSER_ORIG_COL_NAME: str
    # # ALT_DEST_COL_NAME: str
    # # IN_PERIOD: int | dict[str, int] | None = None
    # # OUT_PERIOD: int | dict[str, int] | None = None
    # RESULT_COL_NAME: str
    #
    # SEGMENTS: list[str] | None = None
    # SIZE_TERM_SELECTOR: str | None = None
    #
    # CHOOSER_FILTER_COLUMN_NAME: str | None = None
    # DEST_CHOICE_COLUMN_NAME: str | None = None
    # DEST_CHOICE_LOGSUM_COLUMN_NAME: str | None = None
    # DEST_CHOICE_SAMPLE_TABLE_NAME: str | None = None
    # CHOOSER_TABLE_NAME: str | None = None
    # CHOOSER_SEGMENT_COLUMN_NAME: str | None = None
    # SEGMENT_IDS: dict[str, int] | None = None
    # SHADOW_PRICE_TABLE: str | None = None
    # MODELED_SIZE_TABLE: str | None = None
    # SIMULATE_CHOOSER_COLUMNS: list[str] | None = None
    # LOGSUM_TOUR_PURPOSE: str | dict[str, str] | None = None
    # MODEL_SELECTOR: Literal["workplace", "school", None] = None
    # SAVED_SHADOW_PRICE_TABLE_NAME: str | None = None
    # CHOOSER_ID_COLUMN: str = "person_id"
    #
    # ORIG_ZONE_ID: str | None = None
    # """This setting appears to do nothing..."""
    #
    # annotate_routes: PreprocessorSettings | None = None
    # annotate_establishments: PreprocessorSettings | None = None


def _stop_purpose(
    state: workflow.State,
    nonterminated_routes: pd.DataFrame,
    model_settings: StopLoopPurposeSettings,
    trace_label: str = "cv_stop_purpose",
) -> pd.DataFrame:
    """
    Determine for each route whether its terminal and depot can be different.

    Parameters
    ----------
    state : workflow.State
    nonterminated_routes : DataFrame
        This represents the 'choosers' table for this subcomponent.
    model_settings : StopLoopPurposeSettings
        The settings used in this model subcomponent.
    trace_label : str, default "cv_stop_purpose"
        This label is used for various tracing purposes.
    """
    logger.info(
        "Running %s with %d nonterminated_routes",
        trace_label,
        len(nonterminated_routes),
    )

    # estimator = estimation.manager.begin_estimation(state, trace_label)

    constants = model_settings.CONSTANTS or {}

    # - preprocessor
    # preprocessor_settings = model_settings.preprocessor
    # if preprocessor_settings:
    #     locals_d = {}
    #     if constants is not None:
    #         locals_d.update(constants)
    #
    #     expressions.assign_columns(
    #         state,
    #         df=routes,
    #         model_settings=preprocessor_settings,
    #         locals_dict=locals_d,
    #         trace_label=trace_label,
    #     )

    model_spec = state.filesystem.read_model_spec(file_name=model_settings.SPEC)
    coefficients_df = state.filesystem.read_model_coefficients(model_settings)
    model_spec = simulate.eval_coefficients(state, model_spec, coefficients_df, None)

    nest_spec = config.get_logit_model_settings(model_settings)

    # if estimator:
    #     estimator.write_model_settings(model_settings, model_settings_file_name)
    #     estimator.write_spec(file_name=model_settings.SPEC)
    #     estimator.write_coefficients(
    #         coefficients_df, file_name=model_settings.COEFFICIENTS
    #     )
    #     estimator.write_choosers(routes)

    choices = simulate.simple_simulate(
        state,
        choosers=nonterminated_routes,
        spec=model_spec,
        nest_spec=nest_spec,
        locals_d=constants,
        trace_label=trace_label,
        trace_choice_name="route_terminal_type",
        estimator=None,
    )

    result_dtype = pd.CategoricalDtype(categories=model_spec.columns)
    choices = pd.Series(
        data=pd.Categorical.from_codes(choices, dtype=result_dtype), index=choices.index
    )

    # if estimator:
    #     estimator.write_choices(choices)
    #     choices = estimator.get_survey_values(
    #         choices, "routes", model_settings.RESULT_COL_NAME
    #     )
    #     estimator.write_override_choices(choices)
    #     estimator.end_estimation()

    nonterminated_routes[model_settings.PRIOR_PURP_COL] = nonterminated_routes[
        model_settings.NEXT_PURP_COL
    ]
    nonterminated_routes[model_settings.NEXT_PURP_COL] = (
        choices.reindex(nonterminated_routes.index)
        .fillna(model_spec.columns[0])
        .astype(result_dtype)
    )

    tracing.print_summary(
        model_settings.NEXT_PURP_COL,
        nonterminated_routes[model_settings.NEXT_PURP_COL],
        value_counts=True,
    )

    return nonterminated_routes


def _route_stop_location(
    state: workflow.State,
    nonterminated_routes: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: StopLoopLocationSettings,
    trace_label: str = "cv_stop_location",
) -> pd.DataFrame:
    all_choosers = nonterminated_routes.sort_index()

    if all_choosers.shape[0] == 0:
        tracing.no_results(trace_label)
        return nonterminated_routes

    # estimator = estimation.manager.begin_estimation(state, trace_label)
    # if estimator:
    #     estimator.write_coefficients(model_settings=model_settings)
    #     estimator.write_spec(model_settings, tag="SPEC")
    #     estimator.set_alt_id(model_settings.RESULT_COL_NAME)
    #     estimator.write_table(
    #         state.get_injectable("size_terms"), "size_terms", append=False
    #     )
    #     estimator.write_table(state.get_dataframe("land_use"), "landuse", append=False)
    #     estimator.write_model_settings(model_settings, model_settings_file_name)

    from activitysim.abm.models.util.tour_destination import SizeTermCalculator

    size_term_calculator = SizeTermCalculator(state, model_settings.SIZE_TERM_SELECTOR)

    # maps segment names to compact (integer) ids
    segments = model_settings.SEGMENTS

    chooser_segment_column = model_settings.CHOOSER_SEGMENT_COLUMN_NAME

    choices_list = []
    for segment_name in segments:
        segment_trace_label = tracing.extend_trace_label(trace_label, segment_name)

        if chooser_segment_column is not None:
            choosers = all_choosers[
                all_choosers[chooser_segment_column] == segment_name
            ]
        else:
            choosers = all_choosers

        if len(choosers) == 0:
            continue

        if segment_name == "base":
            # there is no terminal choice to make, the terminal location is establishment MAZ
            choices_list.append(
                pd.Series(
                    name=model_settings.RESULT_COL_NAME,
                    data=choosers["MAZ"],
                    index=choosers.index,
                )
            )
            continue

        # Note: size_term_calculator omits zones with impossible alternatives
        # (where dest size term is zero)
        segment_destination_size_terms = size_term_calculator.dest_size_terms_df(
            segment_name, segment_trace_label
        )
        spec = simulate.spec_for_segment(
            state,
            None,
            spec_id="SPEC",
            segment_name=segment_name,
            estimator=None,
            spec_file_name=model_settings.SPEC,
            coefficients_file_name=model_settings.COEFFICIENTS,
        )
        sample_size = 1  # not really sampling, we are choosing
        locals_dict = model_settings.CONSTANTS.copy()
        locals_dict.update(
            {
                # "size_terms": size_term_matrix,
                # "size_terms_array": size_term_matrix.df.to_numpy(),
                "timeframe": "timeless",
            }
        )  # FIXME use timed skims

        skim_dict = network_los.get_default_skim_dict()
        skims = skim_dict.wrap("zone_id", "MAZ")

        locals_dict.update({"skims": skims})

        choices = interaction_sample(
            state,
            choosers,
            segment_destination_size_terms,
            spec,
            sample_size,
            alt_col_name=model_settings.RESULT_COL_NAME,
            allow_zero_probs=False,
            log_alt_losers=False,
            skims=skims,
            locals_d=locals_dict,
            chunk_size=0,
            chunk_tag=None,
            trace_label=trace_label,
            zone_layer=None,
        )
        choices_list.append(choices[model_settings.RESULT_COL_NAME])

    if len(choices_list) > 0:
        choices_df = pd.concat(choices_list)
    else:
        # this will only happen with small samples (e.g. singleton) with no (e.g.) school segs
        logger.warning("%s no choices", trace_label)
        choices_df = pd.Series(name=model_settings.RESULT_COL_NAME)

    # if estimator:
    #     estimator.write_choices(choices_df)
    #     choices_df = estimator.get_survey_values(choices_df, "tours", "destination")
    #     estimator.write_override_choices(choices_df)
    #     estimator.end_estimation()

    assign_in_place(nonterminated_routes, choices_df.to_frame())

    assert all(
        ~nonterminated_routes[model_settings.RESULT_COL_NAME].isna()
    ), f"Routes are missing {model_settings.RESULT_COL_NAME}: {nonterminated_routes[nonterminated_routes[model_settings.RESULT_COL_NAME].isna()]}"

    return nonterminated_routes


@workflow.step
def route_stops(
    state: workflow.State,
    routes: pd.DataFrame,
    routes_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: StopLoopComponentSettings | None = None,
    model_settings_file_name: str = "route_stops.yaml",
    trace_label: str = "route_stops",
) -> None:
    if model_settings is None:
        model_settings = StopLoopComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    # Collect all non-terminated routes

    nonterminated_routes = routes_merged

    # initialize prior_stop_location to route origination location
    prior_stop_location = routes["origination_zone"]

    nonterminated_routes[model_settings.purpose_settings.NEXT_PURP_COL] = as_int_enum(
        pd.Series(StopPurposes.originate.name, index=nonterminated_routes.index),
        StopPurposes,
        categorical=True,
    )
    nonterminated_routes[model_settings.purpose_settings.PRIOR_PURP_COL] = as_int_enum(
        pd.Series(StopPurposes.originate.name, index=nonterminated_routes.index),
        StopPurposes,
        categorical=True,
    )

    nonterminated_routes.info(1)

    route_trip_num = 1

    cv_trips = []

    while len(nonterminated_routes) > 0:
        logger.debug(f"{trace_label} processing cv stop {route_trip_num}")

        # initialize next stop location as -1
        next_stop_location = pd.Series(-1, index=nonterminated_routes.index)

        # Choose next stop purpose
        nonterminated_routes = _stop_purpose(
            state, nonterminated_routes, model_settings.purpose_settings
        )

        routes_continuing = (
            nonterminated_routes[
                model_settings.purpose_settings.NEXT_PURP_COL
            ].cat.codes
            != StopPurposes.terminate
        )

        # when terminating, set stop location to terminal zone location
        next_stop_location[~routes_continuing] = nonterminated_routes[
            model_settings.location_settings.TERMINAL_ZONE_COL_NAME
        ][~routes_continuing]

        # when not terminating, choose next stop location type
        nonterminated_routes = route_endpoint_type(
            state,
            nonterminated_routes,
            model_settings=model_settings.location_type_settings,
            trace_label = "stop_location_type",
        )

        # Choose next stop location
        nonterminated_routes = _route_stop_location(
            state,
            nonterminated_routes,
            network_los,
            model_settings.location_settings,
        )
        next_stop_location[routes_continuing] = nonterminated_routes[
            model_settings.location_settings.RESULT_COL_NAME
        ][routes_continuing]

        # Choose dwell time
        pass  # time model

        # write to cv_trips table
        cv_trips.append(
            pd.DataFrame(
                {
                    "route_id": nonterminated_routes.index.to_numpy(),
                    "route_trip_num": route_trip_num,
                    "trip_origin": prior_stop_location.to_numpy(),
                    "trip_destination": next_stop_location.to_numpy(),
                    "trip_start_time": 9.9,
                },
                index=pd.Index(
                    nonterminated_routes.index * 1000 + route_trip_num,
                    name="cv_trip_id",
                ),
            )
        )

        route_trip_num += 1
        nonterminated_routes = nonterminated_routes[routes_continuing]
        prior_stop_location = next_stop_location[routes_continuing]

    logger.info(f"{trace_label}: all routes terminated")
    cv_trips_frame = pd.concat(cv_trips)
    cv_trips_frame.info()
    state.add_table("cv_trips", cv_trips_frame)
