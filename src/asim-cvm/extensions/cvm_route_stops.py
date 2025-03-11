from __future__ import annotations

import logging

import numpy as np
import pandas as pd
from pydantic import NonNegativeFloat, PositiveFloat, validator

from activitysim.core import (
    config,
    estimation,
    expressions,
    los,
    simulate,
    tracing,
    workflow,
)
from activitysim.core.configuration.base import PreprocessorSettings, PydanticReadable, ComputeSettings
from activitysim.core.configuration.logit import (
    BaseLogitComponentSettings,
    LogitComponentSettings,
    PreprocessorSettings,
)
from activitysim.core.interaction_sample import interaction_sample
from activitysim.core.util import assign_in_place
from activitysim.abm.models.util.tour_destination import (
    aggregate_size_terms,
    choose_MAZ_for_TAZ,
)
import scipy

from .cvm_enum import LocationTypes, StopPurposes, BusinessTypes
from .cvm_enum_tools import as_int_enum, int_enum_to_categorical_dtype
from .cvm_route_terminal_type import RouteStopTypeSettings, route_endpoint_type
from .cvm_terminal_choice import SimpleLocationComponentSettings

logger = logging.getLogger(__name__)

# temp column names for presampling
DEST_MAZ = "dest_MAZ"
DEST_TAZ = "dest_TAZ"
ORIG_TAZ = "TAZ"
ORIG_MAZ = "_prior_stop_location_"
TERMINAL_TAZ = "terminal_TAZ"

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

    preprocessor: PreprocessorSettings | None = None
    """Preprocessor settings for this model."""

    ACCESSIBILITY_TERMS: list[str] | None = None


class StopLoopLocationSettings(SimpleLocationComponentSettings, extra="forbid"):
    TERMINAL_ZONE_COL_NAME: str = "terminal_zone"


class GammaParameters(PydanticReadable, extra="forbid"):
    """
    Parameters for a gamma distributed random variable.
    """

    shape: PositiveFloat
    """The shape term for a gamma distribution.
    
    For the default gamma, this is the only term.  For adjusted gammas,
    all shape terms are combined multiplicatively.
    """

    scale: PositiveFloat = 1.0
    """The scale term for a gamma distribution.
    
    For the default gamma, this is the only term.  For adjusted gammas,
    all scale terms are combined multiplicatively.
    """


class GammaParameterAdjustments(GammaParameters, extra="forbid"):
    shape: PositiveFloat = 1.0

    shape_add: NonNegativeFloat = 0.0
    """An additive adjustment for shape term for a gamma distribution.

    Additions are applied cumulatively, and are all applied after the 
    base term is computed multiplicatively.  So if the base is 1, there are two 
    `shape` adjustments of 2 and 3, and two `shape_add` values of 4 and 5, the 
    final shape is `(1 * 2 * 3) + 4 + 5 = 15`.
    """

    scale_add: NonNegativeFloat = 0.0
    """An additive adjustment for scale term for a gamma distribution.

    Additions are applied cumulatively, and are all applied after the 
    base term is computed multiplicatively.  So if the base is 1, there are two 
    `scale` adjustments of 2 and 3, and two `scale_add` values of 4 and 5, the 
    final shape is `(1 * 2 * 3) + 4 + 5 = 15`.
    """


class DwellTimeSettings(PydanticReadable, extra="forbid"):
    RESULT_COL_NAME: str = "dwell_time"
    """The column in the cv_trips table that has the resulting dwell time."""

    purpose_column: str = "next_stop_purpose"
    location_type_column: str = "next_stop_location_type"
    vehicle_type_column: str = "vehicle_type"
    before_noon_column: str = "before_noon"
    min_dwell_time: float = 1
    """Minimum dwell time for any stop.

    If the random number generator selects a time less than this, it is 
    truncated to this value.
    """

    max_dwell_time: float = 360
    """Maximum dwell time for any stop.
    
    If the random number generator selects a time greater than this, it is 
    truncated to this value.
    """


class StopLoopComponentSettings(PydanticReadable, extra="forbid"):
    """
    Base configuration class for components that are non-logsum location choice models.
    """

    purpose_settings: StopLoopPurposeSettings
    location_type_settings: RouteStopTypeSettings
    location_settings_estb: StopLoopLocationSettings
    location_settings_tnc: StopLoopLocationSettings
    dwell_settings: DwellTimeSettings
    travel_time_skim: str
    """Name of the skim that contains the travel times.
    
    These are the values, along with dwell time, used to accumulate elapsed time."""

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

    # this needs to happen before we choose the next purpose
    nonterminated_routes[model_settings.PRIOR_PURP_COL] = nonterminated_routes[
        model_settings.NEXT_PURP_COL
    ]

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
        trace_choice_name="route_next_stop_purpose",
        estimator=None,
    )

    result_dtype = pd.CategoricalDtype(categories=model_spec.columns)
    desired_result_dtype = int_enum_to_categorical_dtype(StopPurposes)
    choices = pd.Series(
        data=pd.Categorical.from_codes(choices, dtype=result_dtype), index=choices.index
    ).astype(desired_result_dtype)

    # if estimator:
    #     estimator.write_choices(choices)
    #     choices = estimator.get_survey_values(
    #         choices, "routes", model_settings.RESULT_COL_NAME
    #     )
    #     estimator.write_override_choices(choices)
    #     estimator.end_estimation()

    nonterminated_routes[model_settings.NEXT_PURP_COL] = (
        choices.reindex(nonterminated_routes.index)
        .fillna(model_spec.columns[0])
        .astype(desired_result_dtype)
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

    from activitysim.abm.models.util.tour_destination import SizeTermCalculator

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

        if len(choosers) == 0:
            continue

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

        if TERMINAL_TAZ not in choosers:
            choosers[TERMINAL_TAZ] = network_los.map_maz_to_taz(choosers[model_settings.TERMINAL_ZONE_COL_NAME])

        # create wrapper with keys for this lookup - in this case there is a TAZ in the choosers
        # and a DEST_TAZ in the alternatives which get merged during interaction
        # the skims will be available under the name "skims" for any @ expressions
        skim_dict = network_los.get_skim_dict("taz")
        skims = {
            "skims": skim_dict.wrap(ORIG_TAZ, DEST_TAZ),
            "leg1_skims": skim_dict.wrap_3d(
                orig_key=ORIG_TAZ,
                dest_key=DEST_TAZ,
                dim3_key="_time_period_",
            ),
            "leg2_skims": skim_dict.wrap_3d(
                orig_key=DEST_TAZ,
                dest_key=TERMINAL_TAZ,
                dim3_key="_time_period_",
            )
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

        # keep the choosers columns specified in the model settings
        if model_settings.SIMULATE_CHOOSER_COLUMNS:
            choosers = choosers[model_settings.SIMULATE_CHOOSER_COLUMNS]

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
            compute_settings=ComputeSettings(drop_unused_columns=False),
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

    assign_in_place(nonterminated_routes, choices_df.to_frame())

    assert all(
        ~nonterminated_routes[model_settings.RESULT_COL_NAME].isna()
    ), f"Routes are missing {model_settings.RESULT_COL_NAME}: {nonterminated_routes[nonterminated_routes[model_settings.RESULT_COL_NAME].isna()]}"

    return nonterminated_routes


def _dwell_time(
    state: workflow.State,
    nonterminated_routes: pd.DataFrame,
    model_settings: DwellTimeSettings,
    trace_label: str = "cv_stop_purpose",
    distribution_file: str = "route_stop_duration.csv",
) -> pd.DataFrame:
    # TODO: interface with ActivitySim repro-random
    # rng = np.random.default_rng(seed=42)
    distribution_dict = {}

    distribution_file_ = state.filesystem.get_config_file_path(distribution_file)
    distributions_df = pd.read_csv(distribution_file_)
    distribution_dict = distributions_df.set_index(
        ["purpose", "vehicle_type", "before_noon"]
    ).to_dict(orient="index")

    result_list = []

    # 12:00 pm is the 18th time period in the skim time periods
    nonterminated_routes["before_noon"] = nonterminated_routes["trip_start_time"] < 18

    for (purpose, vehicle_type, before_noon), df in nonterminated_routes.groupby(
        [
            model_settings.purpose_column,
            model_settings.vehicle_type_column,
            model_settings.before_noon_column,
        ]
    ):
        try:
            purpose_ = StopPurposes[purpose]
        except KeyError:
            purpose_ = purpose
        if purpose_ == StopPurposes.terminate:
            # no need to compute dwell times on terminating stops
            result_list.append(
                pd.Series(0, index=df.index, name=model_settings.RESULT_COL_NAME)
            )
            continue

        alpha = distribution_dict[(purpose, vehicle_type, before_noon)]["alpha"]
        beta = distribution_dict[(purpose, vehicle_type, before_noon)]["beta"]
        max_duration = distribution_dict[(purpose, vehicle_type, before_noon)][
            "max_duration"
        ]

        random_dwell_times = scipy.stats.beta.rvs(
            a=alpha,
            b=beta,
            loc=model_settings.min_dwell_time,
            scale=max_duration,
            size=len(df),
        )

        result_list.append(
            pd.Series(
                random_dwell_times, index=df.index, name=model_settings.RESULT_COL_NAME
            )
        )

    dwell_times = pd.concat(result_list)
    assign_in_place(nonterminated_routes, dwell_times.to_frame())
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

    # create is_tnc column
    nonterminated_routes["is_tnc"] = nonterminated_routes["business_type"].isin(
        [
            BusinessTypes.TNCNRR.name,
            BusinessTypes.TNCRES.name,
            BusinessTypes.TNCRET.name,
        ]
    )

    if "route_elapsed_time" not in nonterminated_routes:
        nonterminated_routes["route_elapsed_time"] = 0.0

    # initialize prior_stop_location to route origination location
    nonterminated_routes[ORIG_MAZ] = prior_stop_location = routes[
        "origination_zone"
    ]

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

    # calculate additional accessibility variables - move these to preprocessor?
    accessibility_df = state.get_table("commercial_accessibility")
    accessibility_df['acc_hh_goods'] = np.log(
        np.exp(accessibility_df['estab_acc_hh_food']) +
        np.exp(accessibility_df['estab_acc_hh_package'])
    )

    route_trip_num = 1

    cv_trips = []

    while len(nonterminated_routes) > 0:
        logger.debug(f"{trace_label} processing cv stop {route_trip_num}")

        # use existing elapsed time to compute trip start time
        nonterminated_routes["trip_start_time"] = nonterminated_routes["start_time"] + (
            nonterminated_routes["route_elapsed_time"]
            // state.network_settings.skim_time_periods.period_minutes
        ).astype(np.int16)

        # override trip start time if it would be after the modeled day
        too_late = (
            nonterminated_routes["trip_start_time"]
            >= state.network_settings.skim_time_periods.periods[-1]
        )
        if too_late.any():
            nonterminated_routes.loc[too_late, "trip_start_time"] = (
                state.network_settings.skim_time_periods.periods[-1] - 1
            )

        nonterminated_routes["_time_period_"] = network_los.skim_time_period_label(
            nonterminated_routes["trip_start_time"]
        )

        # initialize next stop location as -1
        next_stop_location = pd.Series(-1, index=nonterminated_routes.index)

        # initialize next dwell time as 0
        dwell_time = pd.Series(0, index=nonterminated_routes.index, dtype=np.float32)

        # get the accessibility at prior stop location
        if "acc_hh_goods" in nonterminated_routes:
            nonterminated_routes.drop(["acc_hh_goods"], axis=1, inplace=True)
        
        join_df = nonterminated_routes.merge(
            accessibility_df[['acc_hh_goods']],
            left_on=ORIG_MAZ,
            right_index=True,
        )

        assert len(join_df) == len(nonterminated_routes)

        nonterminated_routes["acc_hh_goods"] = join_df["acc_hh_goods"]

        # Choose next stop purpose
        nonterminated_routes = _stop_purpose(
            state, nonterminated_routes, model_settings.purpose_settings
        )

        # override purpose to be terminate if it would be after the modeled day
        if too_late.any():
            nonterminated_routes.loc[
                too_late, model_settings.purpose_settings.NEXT_PURP_COL
            ] = StopPurposes.terminate.name

        routes_continuing = (
            nonterminated_routes[
                model_settings.purpose_settings.NEXT_PURP_COL
            ].cat.codes
            != StopPurposes.terminate
        )

        # when terminating, set stop location to terminal zone location
        next_stop_location[~routes_continuing] = nonterminated_routes[
            model_settings.location_settings_estb.TERMINAL_ZONE_COL_NAME
        ][~routes_continuing]

        # when not terminating, choose next stop location type
        nonterminated_routes = route_endpoint_type(
            state,
            nonterminated_routes,
            model_settings=model_settings.location_type_settings,
            trace_label="stop_location_type",
        )

        # when terminating, the next stop location type is terminal type
        nonterminated_routes[model_settings.location_type_settings.RESULT_COL_NAME] = np.where(
            ~routes_continuing,
            nonterminated_routes["terminal_stop_type"],
            nonterminated_routes[model_settings.location_type_settings.RESULT_COL_NAME],
        )

        # if next stop purpose is base, then the location type is base
        nonterminated_routes[model_settings.location_type_settings.RESULT_COL_NAME] = np.where(
            nonterminated_routes[model_settings.purpose_settings.NEXT_PURP_COL] == 'base',
            "base",
            nonterminated_routes[model_settings.location_type_settings.RESULT_COL_NAME]
        )

        tracing.print_summary(
            model_settings.location_type_settings.RESULT_COL_NAME,
            nonterminated_routes[model_settings.location_type_settings.RESULT_COL_NAME],
            value_counts=True,
        )

        # Choose next stop location
        # part 1 establishment routes
        nonterminated_routes_is_estb = (nonterminated_routes["is_tnc"] == False)
        nonterminated_routes_estb = _route_stop_location(
            state,
            nonterminated_routes[nonterminated_routes["is_tnc"] == False],
            network_los,
            model_settings.location_settings_estb,
        )
        # part 2 tnc routes
        nonterminated_routes_is_tnc = (nonterminated_routes["is_tnc"] == True)
        nonterminated_routes_tnc = _route_stop_location(
            state,
            nonterminated_routes[nonterminated_routes["is_tnc"] == True],
            network_los,
            model_settings.location_settings_tnc,
        )
        # combine the two parts
        nonterminated_routes[model_settings.location_settings_estb.RESULT_COL_NAME] = (
            pd.concat([nonterminated_routes_estb, nonterminated_routes_tnc])[model_settings.location_settings_estb.RESULT_COL_NAME]
        )

        next_stop_location[routes_continuing] = nonterminated_routes[
            model_settings.location_settings_estb.RESULT_COL_NAME
        ][routes_continuing]

        nonterminated_routes[model_settings.location_settings_estb.RESULT_COL_NAME] = next_stop_location

        np.random.seed(seed=42)

        # Choose dwell time
        nonterminated_routes = _dwell_time(
            state,
            nonterminated_routes,
            model_settings.dwell_settings,
        )
        dwell_time[routes_continuing] = nonterminated_routes[
            model_settings.dwell_settings.RESULT_COL_NAME
        ][routes_continuing]

        skim_dict = network_los.get_default_skim_dict()
        dskim = skim_dict.wrap_3d(
            orig_key=ORIG_MAZ,
            dest_key=model_settings.location_settings_estb.RESULT_COL_NAME,
            dim3_key="_time_period_",
        )
        dskim.set_df(nonterminated_routes)
        trip_travel_time = dskim[model_settings.travel_time_skim]

        # update total elapsed time
        nonterminated_routes["route_elapsed_time"] = (
            nonterminated_routes["route_elapsed_time"] + trip_travel_time + dwell_time
        )

        # write to cv_trips table
        cv_trips.append(
            pd.DataFrame(
                {
                    "route_id": nonterminated_routes.index.to_numpy(),
                    "route_trip_num": route_trip_num,
                    "trip_origin": prior_stop_location.values,
                    "trip_destination": next_stop_location.values,
                    "trip_origin_purpose": nonterminated_routes[
                        "prior_stop_purpose"
                    ].values,
                    "trip_destination_purpose": nonterminated_routes[
                        "next_stop_purpose"
                    ].values,
                    "trip_destination_type": nonterminated_routes[
                        "next_stop_location_type"
                    ].values,
                    "trip_start_time": nonterminated_routes["trip_start_time"].values,
                    "trip_travel_time": trip_travel_time.values,
                    "dwell_time": dwell_time.values,
                    "route_elapsed_time": nonterminated_routes[
                        "route_elapsed_time"
                    ].values,
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
        nonterminated_routes[ORIG_MAZ] = prior_stop_location

    logger.info(f"{trace_label}: all routes terminated")
    cv_trips_frame = pd.concat(cv_trips)
    cv_trips_frame.info()
    state.add_table("cv_trips", cv_trips_frame)
