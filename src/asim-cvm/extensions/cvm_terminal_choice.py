from __future__ import annotations

import logging
from pathlib import Path
from typing import Literal

import numpy as np
import pandas as pd

from activitysim.abm.models.util import annotate, tour_destination
from activitysim.core import (
    estimation,
    expressions,
    interaction_sample_simulate,
    los,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration import PydanticBase
from activitysim.core.configuration.base import ComputeSettings
from activitysim.core.configuration.logit import (
    BaseLogitComponentSettings,
    PreprocessorSettings,
    TourLocationComponentSettings,
)
from activitysim.core.util import assign_in_place, reindex
from activitysim.abm.models.util.tour_destination import (
    aggregate_size_terms,
    choose_MAZ_for_TAZ,
)

logger = logging.getLogger(__name__)

# temp column names for presampling
DEST_MAZ = "dest_MAZ"
DEST_TAZ = "dest_TAZ"
ORIG_TAZ = "TAZ"
ORIG_MAZ = "zone_id"

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

    SEGMENTS: list[str] | list[dict] | None = None
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

    REQUIRE_ACCESSIBILITY: bool = False
    """If True, require that the accessibility table is present in the pipeline."""
    ACCESSIBILITY_TERMS: list[str] | None = None
    """List of accessibility terms to be used in the model."""

    port_taz: list[int] | None = None

    CONSTANTS: dict = {}

    SAMPLE_SPEC: str
    """spec file for the pre-sampling step"""

    SAMPLE_SIZE: int = 1
    """number of alternatives to sample for each chooser"""

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

    all_choosers = routes_merged.sort_index()

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

    # check if need to join accessibility terms to the land use table
    if model_settings.REQUIRE_ACCESSIBILITY:
        if model_settings.ACCESSIBILITY_TERMS is None:
            raise RuntimeError(
                "REQUIRE_ACCESSIBILITY is True, but ACCESSIBILITY_TERMS is not set"
            )
        if "commercial_accessibility" not in state._LOADABLE_TABLES:
            raise RuntimeError(
                "REQUIRE_ACCESSIBILITY is True, but accessibility table not found in pipeline"
            )
        accessibility_terms = model_settings.ACCESSIBILITY_TERMS
        assert isinstance(accessibility_terms, list)
        land_use = state.get_table("land_use")
        # add accessibility terms that are not in land use table
        add_accessibility_terms = list(
            set(accessibility_terms) - set(land_use.columns)
        )
        if len(add_accessibility_terms) > 0:
            accessibility = state.get_table("commercial_accessibility")
            land_use = land_use.join(accessibility[add_accessibility_terms], how="left")
            state.add_table("land_use", land_use)
    
    land_use = state.get_table("land_use")
    assert "TAZ" in land_use.columns
    land_use["is_port"] = 0
    if model_settings.port_taz:
        land_use["is_port"] = land_use["TAZ"].isin(model_settings.port_taz)
    
    state.add_table("land_use", land_use)

    size_term_calculator = SizeTermCalculator(state, model_settings.SIZE_TERM_SELECTOR)

    # maps segment names to compact (integer) ids
    segments = model_settings.SEGMENTS

    chooser_segment_column = model_settings.CHOOSER_SEGMENT_COLUMN_NAME

    choices_list = []
    for segment in segments:
        if isinstance(segment, dict):
            segment_name = segment["name"]
        else:
            segment_name = segment
        segment_trace_label = tracing.extend_trace_label(trace_label, segment_name)

        if chooser_segment_column is not None:
            choosers = all_choosers[
                all_choosers[chooser_segment_column] == segment_name
            ]
        else:
            choosers = all_choosers

        if segment_name == "base":
            # there is no terminal choice to make, the terminal location is establishment MAZ
            choices_list.append(
                pd.Series(
                    name=model_settings.RESULT_COL_NAME,
                    data=choosers["zone_id"],
                    index=choosers.index,
                )
            )
            continue

        # there are multiple size terms for each segment, defined in the setting
        MAZ_size_terms = []
        TAZ_size_terms = []
        eligibility_term = None

        if isinstance(segment, dict):
            eligibility_term = segment.get("eligibility_term", None)
            # eligibility_term should be a string or None
            assert eligibility_term is None or isinstance(
                eligibility_term, str
            ), f"eligibility_term should be a string or None, not {eligibility_term}"

        for size_term in size_term_calculator.destination_size_terms.columns:
            if size_term not in segment.get("size_terms", []):
                continue
            # Note: size_term_calculator omits zones with impossible alternatives
            # (where dest size term is zero)
            MAZ_single_size_term_df = size_term_calculator.dest_size_terms_df(
                size_term, segment_trace_label
            )
            # aggregate size terms to TAZ level
            MAZ_single_size_term, TAZ_single_size_term = aggregate_size_terms(
                MAZ_single_size_term_df, network_los
            )
            # CVM needs more than one single size term
            # becuase there are size terms that are applied based on route/trip attributes
            MAZ_single_size_term.rename(
                columns={"size_term": size_term}, inplace=True
            )
            # index MAZ size terms by zone_id
            MAZ_single_size_term.set_index("zone_id", inplace=True)
            MAZ_single_size_term.drop("dest_TAZ", axis=1, inplace=True)

            TAZ_single_size_term.rename(
                columns={"size_term": size_term}, inplace=True
            )
            TAZ_single_size_term.drop("dest_TAZ", axis=1, inplace=True)

            MAZ_size_terms.append(MAZ_single_size_term)
            TAZ_size_terms.append(TAZ_single_size_term)

        MAZ_size_terms = pd.concat(MAZ_size_terms, axis=1)
        TAZ_size_terms = pd.concat(TAZ_size_terms, axis=1)

        # size term fillna with 0
        MAZ_size_terms.fillna(0, inplace=True)
        TAZ_size_terms.fillna(0, inplace=True)
       
        # drop the alternatives that do not have non-zero size term in the eligibility term
        if eligibility_term is not None:
            TAZ_size_terms = TAZ_size_terms[
                TAZ_size_terms[eligibility_term] > 0
            ]
            MAZ_size_terms = MAZ_size_terms[
                MAZ_size_terms[eligibility_term] > 0
            ]

        logger.info(f"{trace_label} location_presample")

        alt_dest_col_name = model_settings.RESULT_COL_NAME
        assert DEST_TAZ != alt_dest_col_name

        assert ORIG_MAZ in choosers
        choosers[ORIG_TAZ] = network_los.map_maz_to_taz(choosers[ORIG_MAZ])

        skim_dict = network_los.get_skim_dict("taz")
        skims = {
            "skims": skim_dict.wrap_3d(
                orig_key=ORIG_TAZ,
                dest_key=DEST_TAZ,
                dim3_key="_route_start_time_period_",
            ),
        }

        locals_dict = model_settings.CONSTANTS.copy()
        locals_dict.update(skims)

        # keep the choosers columns specified in the model settings
        if model_settings.SIMULATE_CHOOSER_COLUMNS:
            choosers = choosers[model_settings.SIMULATE_CHOOSER_COLUMNS]

        spec = simulate.spec_for_segment(
            state,
            None,
            spec_id="SAMPLE_SPEC",
            segment_name=segment_name,
            estimator=None,
            spec_file_name=model_settings.SAMPLE_SPEC,
            coefficients_file_name=model_settings.COEFFICIENTS,
        )
        
        logger.info("running %s with %d tours", trace_label, len(choosers))

        sample_size = model_settings.SAMPLE_SIZE

        taz_sample = interaction_sample(
            state,
            choosers,
            TAZ_size_terms,
            spec,
            sample_size,
            alt_col_name=DEST_TAZ,
            allow_zero_probs=False,
            log_alt_losers=False,
            skims=skims,
            locals_d=locals_dict,
            chunk_size=0,
            chunk_tag=None,
            trace_label=trace_label,
            zone_layer=None,
            compute_settings=model_settings.compute_settings.subcomponent_settings(
                "sample"
            ),
        )

        # choose a MAZ for each DEST_TAZ choice, choice probability based on MAZ size_term fraction of TAZ total
        if DEST_TAZ not in MAZ_size_terms:
            MAZ_size_terms[DEST_TAZ] = network_los.map_maz_to_taz(MAZ_size_terms.index)
        
        if "zone_id" not in MAZ_size_terms:
            MAZ_size_terms["zone_id"] = MAZ_size_terms.index
        
        # calculate explicit size term per MAZ per route
        # join choosers with MAZ size terms, based on dest_TAZ
        MAZ_size_terms['size_term'] = MAZ_size_terms[
            eligibility_term
        ]

        maz_choices = choose_MAZ_for_TAZ(state, taz_sample, MAZ_size_terms, trace_label)

        assert DEST_MAZ in maz_choices
        maz_choices = maz_choices.rename(columns={DEST_MAZ: alt_dest_col_name})

        choices_list.append(maz_choices[alt_dest_col_name])

    if len(choices_list) > 0:
        choices_df = pd.concat(choices_list)
    else:
        # this will only happen with small samples (e.g. singleton) with no (e.g.) school segs
        logger.warning("%s no choices", trace_label)
        choices_df = pd.Series(name=model_settings.RESULT_COL_NAME)

    if estimator:
        estimator.write_choices(choices_df)
        choices_df = estimator.get_survey_values(choices_df, "tours", "destination")
        estimator.write_override_choices(choices_df)
        estimator.end_estimation()

    assign_in_place(routes, choices_df.to_frame())

    assert all(
        ~routes[model_settings.RESULT_COL_NAME].isna()
    ), f"Routes are missing {model_settings.RESULT_COL_NAME}: {routes[routes[model_settings.RESULT_COL_NAME].isna()]}"

    state.add_table("routes", routes)

    if model_settings.annotate_routes:
        annotate.annotate_tours(state, model_settings, trace_label)


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
        model_settings=model_settings,
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
        model_settings=model_settings,
        model_settings_file_name=model_settings_file_name,
        trace_label=trace_label,
    )
