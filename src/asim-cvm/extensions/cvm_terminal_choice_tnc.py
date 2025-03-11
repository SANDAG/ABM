from __future__ import annotations

import logging
from pathlib import Path
from typing import Literal

import numpy as np
import pandas as pd

from activitysim.abm.models.util import annotate, tour_destination
from activitysim.core.interaction_sample import interaction_sample
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


def route_endpoint_tnc(
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
                all_choosers[chooser_segment_column].isin(
                    [
                        "TNCNRR",
                        "TNCRES",
                        "TNCRET"
                    ]
                )
            ]
        else:
            choosers = all_choosers

        # there are multiple size terms for each segment, defined in the setting
        segment_destination_size_terms = []
        segment_size_terms = None
        eligibility_term = None

        if isinstance(segment, dict):
            eligibility_term = segment.get("eligibility_term", None)
            # eligibility_term should be a string or None
            assert eligibility_term is None or isinstance(
                eligibility_term, str
            ), f"eligibility_term should be a string or None, not {eligibility_term}"

        for size_term in size_term_calculator.destination_size_terms.columns:
            # Note: size_term_calculator omits zones with impossible alternatives
            # (where dest size term is zero)
            size_term_df = size_term_calculator.dest_size_terms_df(
                size_term, segment_trace_label
            )
            size_term_df.columns = [size_term]
            segment_destination_size_terms.append(size_term_df)
        segment_destination_size_terms = pd.concat(segment_destination_size_terms, axis=1)
       
        # drop the alternatives that do not have non-zero size term in the eligibility term
        if eligibility_term is not None:
            segment_destination_size_terms = segment_destination_size_terms[
                segment_destination_size_terms[eligibility_term] > 0
            ]

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
        skims = {
            "skims": skim_dict.wrap_3d(
                orig_key="zone_id_chooser",
                dest_key="zone_id",
                dim3_key="_route_start_time_period_",
            ),
        }

        locals_dict.update(skims)

        constants = model_settings.CONSTANTS or {}
        locals_dict.update(constants)

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
            compute_settings=ComputeSettings(drop_unused_columns=False),
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
        choices_df = estimator.get_survey_values(choices_df, "tours", "destination")
        estimator.write_override_choices(choices_df)
        estimator.end_estimation()

    # replace the column in the routes with the new choices for tnc routes
    if model_settings.RESULT_COL_NAME in routes.columns:
        routes.loc[choices_df.index,model_settings.RESULT_COL_NAME] = choices_df

    # for tnc routes, make their destination the same as their origin
    if "terminal_zone" in routes.columns:
        routes["terminal_zone"] = np.where(
            routes["business_type"].isin(["TNCNRR", "TNCRES", "TNCRET"]),
            routes["origination_zone"],
            routes["terminal_zone"]
        )

    assert all(
        ~routes[model_settings.RESULT_COL_NAME].isna()
    ), f"Routes are missing {model_settings.RESULT_COL_NAME}: {routes[routes[model_settings.RESULT_COL_NAME].isna()]}"

    state.add_table("routes", routes)

    if model_settings.annotate_routes:
        annotate.annotate_tours(state, model_settings, trace_label)


@workflow.step
def route_origin_destination_tnc(
    state: workflow.State,
    routes: pd.DataFrame,
    routes_merged: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: SimpleLocationComponentSettings | None = None,
    model_settings_file_name: str = "route_od_tnc.yaml",
    trace_label: str = "route_od_tnc",
) -> None:
    return route_endpoint_tnc(
        state=state,
        routes=routes,
        routes_merged=routes_merged,
        network_los=network_los,
        model_settings=model_settings,
        model_settings_file_name=model_settings_file_name,
        trace_label=trace_label,
    )
