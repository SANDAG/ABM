from __future__ import annotations

import logging
from pathlib import Path
from typing import Literal

import pandas as pd
from activitysim.core.configuration import PydanticBase
from activitysim.core.util import reindex

from activitysim.abm.models.util import annotate, tour_destination
from activitysim.core import estimation, los, tracing, workflow, expressions, simulate, interaction_sample_simulate
from activitysim.core.configuration.logit import (
    PreprocessorSettings,
    TourLocationComponentSettings,
BaseLogitComponentSettings,
)
from activitysim.core.util import assign_in_place

logger = logging.getLogger(__name__)


class SimpleLocationComponentSettings(BaseLogitComponentSettings, extra="forbid"):
    """
    Base configuration class for components that are non-logsum location choice models.
    """

    # Logsum-related settings
    # CHOOSER_ORIG_COL_NAME: str
    # ALT_DEST_COL_NAME: str
    # IN_PERIOD: int | dict[str, int] | None = None
    # OUT_PERIOD: int | dict[str, int] | None = None
    RESULT_COL_NAME: str

    SEGMENTS: list[str] | None = None
    SIZE_TERM_SELECTOR: str | None = None

    CHOOSER_FILTER_COLUMN_NAME: str | None = None
    DEST_CHOICE_COLUMN_NAME: str | None = None
    DEST_CHOICE_LOGSUM_COLUMN_NAME: str | None = None
    DEST_CHOICE_SAMPLE_TABLE_NAME: str | None = None
    CHOOSER_TABLE_NAME: str | None = None
    CHOOSER_SEGMENT_COLUMN_NAME: str | None = None
    SEGMENT_IDS: dict[str, int] | None = None
    SHADOW_PRICE_TABLE: str | None = None
    MODELED_SIZE_TABLE: str | None = None
    SIMULATE_CHOOSER_COLUMNS: list[str] | None = None
    LOGSUM_TOUR_PURPOSE: str | dict[str, str] | None = None
    MODEL_SELECTOR: Literal["workplace", "school", None] = None
    SAVED_SHADOW_PRICE_TABLE_NAME: str | None = None
    CHOOSER_ID_COLUMN: str = "person_id"

    ORIG_ZONE_ID: str | None = None
    """This setting appears to do nothing..."""

    annotate_routes: PreprocessorSettings | None = None
    annotate_establishments: PreprocessorSettings | None = None


def annotate_routes(
    state: workflow.State,
    model_settings: dict | PydanticBase,
    trace_label: str,
    locals_dict: dict | None = None,
):
    """
    Add columns to the tours table in the pipeline according to spec.

    Parameters
    ----------
    state : workflow.State
    model_settings : dict or PydanticBase
    trace_label : str
    locals_dict : dict, optional
    """
    if isinstance(model_settings, PydanticBase):
        model_settings = model_settings.dict()
    if locals_dict is None:
        locals_dict = {}
    routes = state.get_dataframe("routes")
    expressions.assign_columns(
        state,
        df=routes,
        model_settings=model_settings.get("annotate_routes"),
        locals_dict=locals_dict,
        trace_label=tracing.extend_trace_label(trace_label, "annotate_routes"),
    )
    state.add_table("routes", routes)

#
# def destination_simulate(
#     state: workflow.State,
#     spec_segment_name: str,
#     choosers: pd.DataFrame,
#     destination_sample,
#     want_logsums: bool,
#     model_settings: TourLocationComponentSettings,
#     network_los: los.Network_LOS,
#     destination_size_terms,
#     estimator,
#     chunk_size,
#     trace_label,
#     skip_choice:bool=False,
# ):
#     chunk_tag = "tour_destination.simulate"
#
#     model_spec = simulate.spec_for_segment(
#         state,
#         None,
#         spec_id="SPEC",
#         segment_name=spec_segment_name,
#         estimator=estimator,
#         spec_file_name=model_settings.SPEC,
#         coefficients_file_name=model_settings.COEFFICIENTS,
#     )
#
#     # FIXME - MEMORY HACK - only include columns actually used in spec (omit them pre-merge)
#     chooser_columns = model_settings.SIMULATE_CHOOSER_COLUMNS
#
#     if chooser_columns:
#         choosers = choosers[
#             [c for c in choosers.columns if c in chooser_columns]
#         ]
#
#     # interaction_sample requires that choosers.index.is_monotonic_increasing
#     if not choosers.index.is_monotonic_increasing:
#         logger.debug(
#             f"destination_simulate {trace_label} sorting choosers because not monotonic_increasing"
#         )
#         choosers = choosers.sort_index()
#
#     if estimator:
#         estimator.write_choosers(choosers)
#
#     alt_dest_col_name = model_settings.ALT_DEST_COL_NAME
#     origin_col_name = model_settings.CHOOSER_ORIG_COL_NAME
#
#     # alternatives are pre-sampled and annotated with logsums and pick_count
#     # but we have to merge size_terms column into alt sample list
#     destination_sample["size_term"] = reindex(
#         destination_size_terms.size_term, destination_sample[alt_dest_col_name]
#     )
#
#     # state.tracing.dump_df(False, destination_sample, trace_label, "alternatives")
#
#     constants = model_settings.CONSTANTS
#
#     logger.info("Running destination_simulate with %d persons", len(choosers))
#
#     # create wrapper with keys for this lookup - in this case there is a home_zone_id in the choosers
#     # and a zone_id in the alternatives which get merged during interaction
#     # the skims will be available under the name "skims" for any @ expressions
#     skim_dict = network_los.get_default_skim_dict()
#     skims = skim_dict.wrap(origin_col_name, alt_dest_col_name)
#
#     locals_d = {
#         "skims": skims,
#         "orig_col_name": skims.orig_key,  # added for sharrow flows
#         "dest_col_name": skims.dest_key,  # added for sharrow flows
#         "timeframe": "timeless",
#     }
#     if constants is not None:
#         locals_d.update(constants)
#
#     # state.tracing.dump_df(DUMP, choosers, trace_label, "choosers")
#
#     log_alt_losers = state.settings.log_alt_losers
#
#     choices = interaction_sample_simulate.interaction_sample_simulate(
#         state,
#         choosers,
#         destination_sample,
#         spec=model_spec,
#         choice_column=alt_dest_col_name,
#         log_alt_losers=log_alt_losers,
#         want_logsums=want_logsums,
#         skims=skims,
#         locals_d=locals_d,
#         chunk_size=chunk_size,
#         chunk_tag=chunk_tag,
#         trace_label=trace_label,
#         trace_choice_name="destination",
#         estimator=estimator,
#         skip_choice=skip_choice,
#     )
#
#     if not want_logsums:
#         # for consistency, always return a dataframe with canonical column name
#         assert isinstance(choices, pd.Series)
#         choices = choices.to_frame("choice")
#
#     return choices

from activitysim.core.interaction_sample import interaction_sample

# def interaction_sample(
#     state,
#     choosers,
#     alternatives,
#     spec,
#     sample_size,
#     alt_col_name,
#     allow_zero_probs=False,
#     log_alt_losers=False,
#     skims=None,
#     locals_d=None,
#     chunk_size=0,
#     chunk_tag=None,
#     trace_label=None,
#     zone_layer=None,
# ):


def route_endpoint(
    state: workflow.State,
    routes: pd.DataFrame,
    routes_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: SimpleLocationComponentSettings | None = None,
    model_settings_file_name: str = "route_terminal.yaml",
    trace_label: str = "route_terminal",
) -> None:
    if model_settings is None:
        model_settings = SimpleLocationComponentSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )

    trace_hh_id = state.settings.trace_hh_id

    logsum_column_name = model_settings.DEST_CHOICE_LOGSUM_COLUMN_NAME
    want_logsums = logsum_column_name is not None

    sample_table_name = model_settings.DEST_CHOICE_SAMPLE_TABLE_NAME
    want_sample_table = (
        state.settings.want_dest_choice_sample_tables and sample_table_name is not None
    )

    # choosers are routes with non-base terminal types
    all_choosers = routes_merged.sort_index() #[routes_merged[model_settings.CHOOSER_SEGMENT_COLUMN_NAME] != "base"]

    if all_choosers.shape[0] == 0:
        tracing.no_results(trace_label)
        return

    estimator = estimation.manager.begin_estimation(state, trace_label)
    if estimator:
        estimator.write_coefficients(model_settings=model_settings)
        estimator.write_spec(model_settings, tag="SPEC")
        estimator.set_alt_id(model_settings.RESULT_COL_NAME)
        estimator.write_table(
            state.get_injectable("size_terms"), "size_terms", append=False
        )
        estimator.write_table(state.get_dataframe("land_use"), "landuse", append=False)
        estimator.write_model_settings(model_settings, model_settings_file_name)

    from activitysim.abm.models.util.tour_destination import SizeTermCalculator
    size_term_calculator = SizeTermCalculator(state, model_settings.SIZE_TERM_SELECTOR)

    # maps segment names to compact (integer) ids
    segments = model_settings.SEGMENTS

    chooser_segment_column = model_settings.CHOOSER_SEGMENT_COLUMN_NAME

    choices_list = []
    for segment_name in segments:
        segment_trace_label = tracing.extend_trace_label(trace_label, segment_name)

        if chooser_segment_column is not None:
            choosers = all_choosers[all_choosers[chooser_segment_column] == segment_name]
        else:
            choosers = all_choosers

        if segment_name == "base":
            # there is no terminal choice to make, the terminal location is establishment MAZ
            choices_list.append(
                pd.Series(name=model_settings.RESULT_COL_NAME, data=choosers["MAZ"], index=choosers.index)
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
            estimator=estimator,
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
        )

        skim_dict = network_los.get_default_skim_dict()
        skims = skim_dict.wrap("zone_id", "MAZ")

        locals_dict.update({"skims":skims})
        log_alt_losers = state.settings.log_alt_losers

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

    if estimator:
        estimator.write_choices(choices_df)
        choices_df = estimator.get_survey_values(
            choices_df, "tours", "destination"
        )
        estimator.write_override_choices(choices_df)
        estimator.end_estimation()

    assign_in_place(routes, choices_df.to_frame())

    # if want_logsums:
    #     choosers[logsum_column_name] = choices_df["logsum"]
    #     assign_in_place(routes, choosers[[logsum_column_name]])

    assert all(
        ~routes[model_settings.RESULT_COL_NAME].isna()
    ), f"Routes are missing {model_settings.RESULT_COL_NAME}: {routes[routes[model_settings.RESULT_COL_NAME].isna()]}"

    state.add_table("routes", routes)

    if model_settings.annotate_routes:
        annotate.annotate_tours(state, model_settings, trace_label)

    # if want_sample_table:
    #     assert len(save_sample_df.index.get_level_values(0).unique()) == len(choices_df)
    #     state.extend_table(sample_table_name, save_sample_df)

    # if trace_hh_id:
    #     state.tracing.trace_df(
    #         routes[routes.open_jaw],
    #         label=trace_label,
    #         slicer="establishment_id",
    #         index_label="route",
    #         columns=None,
    #         warn_if_empty=True,
    #     )


@workflow.step
def route_terminal(
    state: workflow.State,
    routes: pd.DataFrame,
    routes_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: SimpleLocationComponentSettings | None = None,
    model_settings_file_name: str = "route_terminal.yaml",
    trace_label: str = "route_terminal",
) -> None:
    return route_endpoint(
        state=state,
        routes=routes,
        routes_merged=routes_merged,
        network_los=network_los,
        model_settings= model_settings,
        model_settings_file_name=model_settings_file_name,
        trace_label=trace_label,
    )

@workflow.step
def route_origination(
    state: workflow.State,
    routes: pd.DataFrame,
    routes_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: SimpleLocationComponentSettings | None = None,
    model_settings_file_name: str = "route_origination.yaml",
    trace_label: str = "route_origination",
) -> None:
    return route_endpoint(
        state=state,
        routes=routes,
        routes_merged=routes_merged,
        network_los=network_los,
        model_settings= model_settings,
        model_settings_file_name=model_settings_file_name,
        trace_label=trace_label,
    )