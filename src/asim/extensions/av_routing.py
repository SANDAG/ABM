# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np
import pandas as pd
import itertools

from activitysim.core import (
    config,
    expressions,
    logit,
    simulate,
    tracing,
    workflow,
    interaction_simulate,
    util,
)
from activitysim.core.configuration.base import PreprocessorSettings
from activitysim.core.configuration.logit import LogitComponentSettings

logger = logging.getLogger(__name__)


class AVRoutingSettings(LogitComponentSettings):
    """
    Settings for the `AVRouting` component.
    """

    AV_TRIP_MATCHING_SPEC: str
    AV_TRIP_MATCHING_COEFFICIENTS: str

    av_trip_matching_preprocessor: PreprocessorSettings | None = None

    AV_REPOSITIONING_SPEC: str
    AV_REPOSITIONING_COEFFICIENTS: str

    av_repositioning_preprocessor: PreprocessorSettings | None = None

    DRIVING_MODES: list[str]
    """List of modes that are eligible for routing with a household AV"""


def setup_model_settings(state, model_settings):
    """
    Reading the coefficients and spec files so we aren't reading every time period
    """
    model_settings.AV_TRIP_MATCHING_SPEC = state.filesystem.read_model_spec(
        file_name=model_settings.AV_TRIP_MATCHING_SPEC
    )
    model_settings.AV_TRIP_MATCHING_COEFFICIENTS = (
        state.filesystem.read_model_coefficients(
            file_name=model_settings.AV_TRIP_MATCHING_COEFFICIENTS
        )
    )

    model_settings.AV_REPOSITIONING_SPEC = state.filesystem.read_model_spec(
        file_name=model_settings.AV_REPOSITIONING_SPEC
    )
    model_settings.AV_REPOSITIONING_COEFFICIENTS = (
        state.filesystem.read_model_coefficients(
            file_name=model_settings.AV_REPOSITIONING_COEFFICIENTS
        )
    )

    return model_settings


def setup_skims(network_los):
    """
    Setup skims for AV routing.

    """

    skim_dict = network_los.get_default_skim_dict()

    # setup skim keys
    out_time_col_name = "start"
    in_time_col_name = "end"
    orig_col_name = "origin"
    dest_col_name = "destination"

    # creating skim wrappers
    odt_skim_stack_wrapper = skim_dict.wrap_3d(
        orig_key=orig_col_name, dest_key=dest_col_name, dim3_key="out_period"
    )

    dot_skim_stack_wrapper = skim_dict.wrap_3d(
        orig_key=dest_col_name, dest_key=orig_col_name, dim3_key="in_period"
    )
    odr_skim_stack_wrapper = skim_dict.wrap_3d(
        orig_key=orig_col_name, dest_key=dest_col_name, dim3_key="in_period"
    )
    dor_skim_stack_wrapper = skim_dict.wrap_3d(
        orig_key=dest_col_name, dest_key=orig_col_name, dim3_key="out_period"
    )
    od_skim_stack_wrapper = skim_dict.wrap(orig_col_name, dest_col_name)
    do_skim_stack_wrapper = skim_dict.wrap(dest_col_name, orig_col_name)

    skims = {
        "odt_skims": odt_skim_stack_wrapper,
        "dot_skims": dot_skim_stack_wrapper,
        "odr_skims": odr_skim_stack_wrapper,  # dot return skims for e.g. TNC bridge return fare
        "dor_skims": dor_skim_stack_wrapper,  # odt return skims for e.g. TNC bridge return fare
        "od_skims": od_skim_stack_wrapper,
        "do_skims": do_skim_stack_wrapper,
        "orig_col_name": orig_col_name,
        "dest_col_name": dest_col_name,
        "out_time_col_name": out_time_col_name,
        "in_time_col_name": in_time_col_name,
    }

    return skims


def construct_av_to_trip_alternatives(num_avs, num_trips):
    """
    Construct a DataFrame of all possible AV-trips assignments.

    Each row represents a unique assignment of trips to AVs.
    The first trip (trip_0) is a placeholder for no trip assigned to an AV.
    The remaining trips (trip_1, trip_2, ..., trip_n) represent actual trips.

    Example for 2 AVs and 2 trips:
         av_1    av_2
    0   trip_0  trip_0
    1   trip_0  trip_1
    2   trip_0  trip_2
    3   trip_1  trip_0
    4   trip_1  trip_2
    5   trip_2  trip_0
    6   trip_2  trip_1
    """
    av_columns = [f"av_{i+1}" for i in range(num_avs)]
    trip_labels = [f"trip_{i}" for i in range(1, num_trips + 1)]
    trip_labels_with_zero = ["trip_0"] + trip_labels

    # All possible assignments (with replacement) of trips (including trip_0) to AVs
    all_assignments = itertools.product(trip_labels_with_zero, repeat=num_avs)

    # Filter so that each trip (except trip_0) is assigned at most once
    valid_assignments = [
        assignment
        for assignment in all_assignments
        if len(set([t for t in assignment if t != "trip_0"]))
        == len([t for t in assignment if t != "trip_0"])
    ]

    alts = pd.DataFrame(valid_assignments, columns=av_columns)
    alts.index.name = "alt"
    return alts


def build_av_to_trip_interaction_df(
    vehicles: pd.DataFrame,
    trips: pd.DataFrame,
    alts: pd.DataFrame,
    choosers: pd.DataFrame,
    trace_label: str,
):
    """
    Creating a table that matches a single av trip with a single trip.
    This allows us to calculate the partial alternative utilities for each trip-AV combination individually.

    Example for 2 AVs and 1 trip:
     household_id | vehicle_id | trip_id | alt | vehicle_info | trip_info
     1            | 1          | 0       | 0   | ...          | ...
     1            | 2          | 0       | 0   | ...          | ...
     1            | 1          | 1       | 1   | ...          | ...
     1            | 2          | 0       | 1   | ...          | ...
     1            | 1          | 0       | 2   | ...          | ...
     1            | 2          | 1       | 2   | ...          | ...


    Parameters:
        vehicles: DataFrame containing vehicle information.
        trips: DataFrame containing trip information.
        alts: DataFrame containing alternatives (AV-trip combinations).
        choosers: DataFrame containing choosers (households).
        trace_label: Label for tracing the interaction DataFrame.

    Returns:
        interaction_df: DataFrame containing the interaction between AVs and trips.
    """

    interaction_dfs = []
    # looping through alternatives to build custom interaction_df
    for row in alts.iterrows():
        for col in alts.columns:
            av_number = int(
                col.split("_")[1]
            )  # Extract the AV number from the column name
            trip_number = int(
                row[1][col].split("_")[1]
            )  # Extract the trip number from row

            av_choosers = vehicles[
                vehicles.household_id.isin(trips.household_id.unique())
                & (vehicles.av_number == av_number)
            ]
            trip_choosers = trips[trips.trip_number == trip_number]

            assert (
                av_choosers.household_id.is_unique
            ), "There should be only one AV chooser per household at this stage"
            assert (
                trip_choosers.household_id.is_unique
            ), "There should be only one trip chooser per household at this stage"

            # want a complete set of household_ids for both choosers
            # reindex to save trip / vehicle ID and then reindex to households which are making the choices
            av_choosers = (
                av_choosers.reset_index()
                .set_index("household_id")
                .reindex(choosers.index, fill_value=np.nan)
            )
            trip_choosers = (
                trip_choosers.reset_index()
                .set_index("household_id")
                .reindex(choosers.index, fill_value=np.nan)
            )

            # setting alt trip and av numbers so we can use them in spec availability conditions
            av_choosers["av_number"] = av_number
            trip_choosers["trip_number"] = trip_number

            # merge the tables together to create a table with columns describing the AV and the trip
            interaction_df = pd.merge(
                av_choosers,
                trip_choosers,
                on="household_id",
                suffixes=("", "_trip"),
            )
            interaction_df["alt"] = row[0]

            interaction_dfs.append(
                interaction_df
            )  # Assign the alternative index to the interaction_df

    # Concatenate all interaction DataFrames into a single DataFrame
    # and sort by household_id and alt
    interaction_df = pd.concat(interaction_dfs, ignore_index=False).reset_index()
    interaction_df.sort_values(by=["household_id", "alt"], inplace=True)

    assert (
        interaction_df.groupby("household_id").size() == (alts.shape[0] * alts.shape[1])
    ).all(), "There should be one row per AV and trip combination per household"

    return interaction_df


def execute_av_trip_matches(
    state,
    model_settings: AVRoutingSettings,
    choices: pd.DataFrame,
    vehicle_trips: pd.DataFrame | None,
    trips_in_period: pd.DataFrame,
    av_vehicles: pd.DataFrame,
):
    """
    Execute the AV trip matches by updating the vehicles DataFrame with the chosen trips.
    This function updates the vehicles DataFrame based on the choices made by the AV routing model.

    Parameters:
        state: The current state of the simulation.
        model_settings: The settings for the AV routing model.
        choices: The choices made by the AV routing model.
        vehicles: The vehicles DataFrame to be updated.

    Returns:
        vehicles: The updated vehicles DataFrame.
    """

    if choices.empty:
        # no trips to make
        return vehicle_trips

    # looping through avs in the household
    av_cols = choices.columns[choices.columns.str.startswith("av_")]

    all_veh_trips = []

    for av_col in av_cols:
        av_number = int(av_col.split("_")[1])

        # drop trip_0 or na from choices since they represent the AV not doing anything
        vehicle_choices = (
            choices.loc[choices[av_col] != "trip_0", av_col]
            .dropna()
            .to_frame(name="av_choice")
            .reset_index()
        )
        vehicle_choices["trip_number"] = (
            vehicle_choices.av_choice.str.split("_").str[1].astype(int)
        )
        vehicle_choices["av_number"] = av_number

        # merge vehicle id so we know which vehicle we are tracking
        current_veh_trips = vehicle_choices.merge(
            av_vehicles.reset_index()[["household_id", "vehicle_id", "av_number"]],
            on=["household_id", "av_number"],
            how="left",
            validate="1:1",
        )
        # merge trip information so we know where the vehicle is going
        current_veh_trips = current_veh_trips.merge(
            trips_in_period.reset_index()[
                [
                    "household_id",
                    "trip_id",
                    "depart",
                    "origin",
                    "destination",
                    "trip_number",
                ]
            ],
            on=["household_id", "trip_number"],
            how="left",
            validate="1:1",
        )

        all_veh_trips.append(current_veh_trips)

    all_veh_trips = pd.concat(all_veh_trips, ignore_index=True)
    if vehicle_trips is not None:
        vehicle_trips = pd.concat([vehicle_trips, all_veh_trips], ignore_index=True)
    else:
        vehicle_trips = all_veh_trips

    assert (
        all_veh_trips.vehicle_id.notna().all()
    ), "There should be a vehicle_id for each trip made by an AV"

    return vehicle_trips


def update_vehicle_positions(vehicle_trips, av_vehicles):
    """
    Update the vehicle positions based on the trips assigned to each AV.

    Parameters:
        vehicle_trips: DataFrame containing trips made by household AVs.
        av_vehicles: DataFrame containing the all AV vehicles.

    Returns:
        av_vehicles: The updated AV vehicles DataFrame with vehicle_location
    """
    # get the rows with the latest depart time for each trip
    latest_trips = vehicle_trips.loc[
        vehicle_trips.groupby(["household_id", "vehicle_id"])["depart"].idxmax()
    ].set_index("vehicle_id")

    # update the vehicle_location in av_vehicles with the latest trip's destination
    av_vehicles.loc[latest_trips.index, "veh_location"] = latest_trips["destination"]

    assert (
        av_vehicles.veh_location.notna().all()
    ), "There should be a vehicle_location for each AV"

    return av_vehicles


def av_trip_matching(
    state,
    model_settings: AVRoutingSettings,
    trips: pd.DataFrame,
    vehicles: pd.DataFrame,
    trace_label: str,
) -> pd.DataFrame:
    """
    Match trips to AVs
    First, construct all possible trip-AV combinations within the household for this time period.
    Then, use a utility function to select the best AV for each trip.

    Much of the utilty and choice logic is taken from activitysim.core.interaction_simulate
    (Couldn't just call it because of the extra step here where we combine across alternatives)
    """

    # get the maximum number of AVs and Trips during this time period
    max_number_of_avs = vehicles.groupby("household_id").size().max()
    max_number_of_trips = trips.groupby("household_id").size().max()

    # the real choosers here are the households with an av that have trips in this time period
    choosers = pd.DataFrame(index=trips.household_id.unique())
    choosers.index.name = "household_id"

    alts = construct_av_to_trip_alternatives(max_number_of_avs, max_number_of_trips)

    interaction_df = build_av_to_trip_interaction_df(
        vehicles=vehicles,
        trips=trips,
        alts=alts,
        choosers=choosers,
        trace_label=trace_label,
    )
    have_trace_targets = state.tracing.has_trace_targets(
        interaction_df, slicer="household_id"
    )

    model_spec = simulate.eval_coefficients(
        state,
        model_settings.AV_TRIP_MATCHING_SPEC,
        model_settings.AV_TRIP_MATCHING_COEFFICIENTS,
        estimator=None,
    )
    constants = config.get_model_constants(model_settings)

    # setup skim wrappers
    # skims = setup_skims(network_los=state.get_injectable("network_los"))
    # simulate.set_skim_wrapper_targets(interaction_df, skims)

    locals_d = {
        # "skims": skims,
    }
    if constants is not None:
        locals_d.update(constants)

    # evaluate expressions from the spec multiply by coefficients and sum
    # spec is df with one row per spec expression and one col with utility coefficient
    # column names of model_design match spec index values
    # utilities has utility value for elements in the cross product of choosers and alternatives
    # interaction_utilities is a df with one utility column and one row per row in model_design
    if have_trace_targets:
        trace_rows, trace_ids = state.tracing.interaction_trace_rows(
            interaction_df, choosers, sample_size=None
        )
        state.tracing.trace_df(
            interaction_df,
            tracing.extend_trace_label(trace_label, "interaction_df"),
            transpose=False,
        )
        # write alternatives to trace folder as well
        state.tracing.trace_df(
            alts,
            tracing.extend_trace_label(trace_label, "alts"),
            slicer="NONE",
            transpose=False,
        )
    else:
        trace_rows = trace_ids = None

    (
        interaction_df["utility"],
        trace_eval_results,
    ) = interaction_simulate.eval_interaction_utilities(
        state,
        model_spec,
        interaction_df,
        locals_d,
        trace_label,
        trace_rows,
        estimator=None,
        log_alt_losers=False,
        compute_settings=model_settings.compute_settings,
    )

    # sum utilities across the alternatives
    interaction_utilities = (
        interaction_df.groupby(["household_id", "alt"])["utility"]
        .sum()
        .reset_index()
        .set_index("alt")
    )

    # make choices based on the summed utilities
    # reshape utilities (one utility column and one row per row in model_design)
    # to a dataframe with one row per chooser and one column per alternative
    utilities = pd.DataFrame(
        interaction_utilities.utility.values.reshape(len(choosers), len(alts)),
        index=choosers.index,
    )

    if have_trace_targets:
        state.tracing.trace_df(
            utilities,
            tracing.extend_trace_label(trace_label, "utils"),
            column_labels=["alternative", "utility"],
        )

    # convert to probabilities (utilities exponentiated and normalized to probs)
    # probs is same shape as utilities, one row per chooser and one column for alternative
    probs = logit.utils_to_probs(
        state, utilities, trace_label=trace_label, trace_choosers=choosers
    )

    # make choices
    # positions is series with the chosen alternative represented as a column index in probs
    # which is an integer between zero and num alternatives in the alternative sample
    positions, rands = logit.make_choices(
        state, probs, trace_label=trace_label, trace_choosers=choosers
    )

    # need to get from an integer offset into the alternative sample to the alternative index
    # that is, we want the index value of the row that is offset by <position> rows into the
    # tranche of this choosers alternatives created by cross join of alternatives and choosers
    # offsets is the offset into model_design df of first row of chooser alternatives
    offsets = np.arange(len(positions)) * len(alts)
    # resulting Int64Index has one element per chooser row and is in same order as choosers
    choices = interaction_utilities.index.take(positions + offsets)

    # create a series with index from choosers and the index of the chosen alternative
    choices = pd.Series(choices, index=choosers.index)

    if have_trace_targets:
        state.tracing.trace_df(
            choices,
            tracing.extend_trace_label(trace_label, "choices"),
            columns=[None, trace_label],
        )
        state.tracing.trace_df(
            rands,
            tracing.extend_trace_label(trace_label, "rands"),
            columns=[None, "rand"],
        )

    choices = (
        choices.to_frame(name="alt")
        .reset_index()
        .merge(alts.reset_index(), how="left", on="alt")
        .set_index("household_id")
    )

    return choices


def get_next_household_trips_to_service(
    state,
    choosers: pd.DataFrame,
    av_eligible_trips: pd.DataFrame,
    av_vehicles: pd.DataFrame,
) -> pd.DataFrame:
    """
    Get the next household trips to service for each AV vehicle.
    Eligible trips are those that satisfy the following conditions:
    - the trip origin is not at home
    - the trip does not have an AV at their current location
    - exists in households that had an AV service a trip in this time period
    - is the next trip

    Parameters:
        state: The current state of the simulation.
        choosers: DataFrame containing the choosers (AV vehicles).
        av_eligible_trips: DataFrame containing the eligible trips for AV routing.
        av_vehicles: DataFrame containing all AV vehicles.

    Returns:
        choosers with selected next trip info attached as new columns, e.g.
        next_trip_id_1, next_trip_depart_1, next_trip_origin_1, next_trip_destination_1, next_trip_id_2,...
    """

    # get the next trip for each person in the household that are not at home
    next_hh_trips = (
        av_eligible_trips[
            av_eligible_trips.household_id.isin(choosers.household_id)
            & (av_eligible_trips.depart > choosers["depart"].max())
            & (av_eligible_trips.origin != av_eligible_trips.home_zone_id)
        ]
        .reset_index()
        .sort_values(by=["depart", "trip_num"])
        .groupby(["household_id", "person_id"])
        .first()
        .reset_index()
        .set_index("trip_id")
    )

    # remove the trip if there is already an AV at the trip origin
    # this also takes care of the AV trying to reposition to its current location
    next_hh_trips_x_av = next_hh_trips.reset_index().merge(
        av_vehicles[["household_id", "veh_location"]],
        on="household_id",
        how="inner",
    )
    trips_with_av_at_origin = next_hh_trips_x_av[
        next_hh_trips_x_av.origin == next_hh_trips_x_av.veh_location
    ]
    next_hh_trips = next_hh_trips[
        ~next_hh_trips.index.isin(trips_with_av_at_origin.trip_id)
    ]

    # select only the first 3 possible trips to reroute to
    next_hh_trips = next_hh_trips.groupby("household_id").head(3).reset_index()
    # number the trips
    next_hh_trips["next_trip_number"] = (
        next_hh_trips.groupby("household_id").cumcount() + 1
    )

    trip_columns_to_keep = [
        "trip_id",
        "person_id",
        "household_id",
        "depart",
        "origin",
        "destination",
    ]

    # loop through next_trip_number and add the trip info to choosers
    for next_trip_num in range(1, 4):
        next_trips = next_hh_trips[next_hh_trips.next_trip_number == next_trip_num][
            trip_columns_to_keep
        ]
        next_trips.columns = [
            f"next_{col}_{next_trip_num}" for col in trip_columns_to_keep
        ]
        next_trips.rename(
            columns={f"next_household_id_{next_trip_num}": "household_id"}, inplace=True
        )
        choosers = choosers.merge(next_trips, on="household_id", how="left")

    # fill NaN values with -1 so skims and stuff works with utility expressions
    choosers.fillna(-1, inplace=True)

    return choosers


def av_repositioning(
    state,
    model_settings: AVRoutingSettings,
    veh_trips_this_period: pd.DataFrame,
    av_eligible_trips: pd.DataFrame,
    av_vehicles: pd.DataFrame,
    trace_label: str,
) -> None:
    """
    Reposition AVs based on household needs.

    Alternatives are:
    1. Stay with person
    2. Go to remote parking
    3. Go home
    4. Service another household trip

    Choosers of this model are av vehicles who serviced a trip during this time period
    as determined by the av_trip_matching model.  We are now deciding where to go after
    they have dropped off their rider.

    Parameters:
        state: The current state of the simulation.
        model_settings: The settings for the AV routing model.
        veh_trips_this_period: DataFrame containing trips made by household AVs in this time period.
        av_eligible_trips: DataFrame containing trips eligible for AV routing.
        av_vehicles: DataFrame containing all AV vehicles.
        trace_label: Label for tracing the repositioning choices.

    Returns:
        veh_trips_this_period: input dataframe with additional repositioning trips appended
    """

    choosers = veh_trips_this_period.copy()

    # choosers['nearest_av_parking_zone_id'] = get_nearest_parking_zone_id(state, choosers)

    choosers = get_next_household_trips_to_service(
        state, choosers, av_eligible_trips, av_vehicles
    )

    model_spec = simulate.eval_coefficients(
        state,
        model_settings.AV_REPOSITIONING_SPEC,
        model_settings.AV_REPOSITIONING_COEFFICIENTS,
        estimator=None,
    )
    constants = config.get_model_constants(model_settings)

    state.get_rn_generator().add_channel("av_repositioning", choosers)

    choices = simulate.simple_simulate(
        state,
        choosers=choosers,
        spec=model_spec,
        nest_spec=None,
        locals_d=constants,
        trace_label=trace_label,
        trace_choice_name="transponder_ownership",
        estimator=None,
        compute_settings=model_settings.compute_settings,
    )

    # convert indexes to alternative names
    choices = pd.Series(model_spec.columns[choices.values], index=choices.index)

    state.get_rn_generator().drop_channel("av_repositioning")

    # veh_trips_this_period = reposition_avs(choosers, choices)

    return veh_trips_this_period


@workflow.step
def av_routing(
    state: workflow.State,
    households: pd.DataFrame,
    trips: pd.DataFrame,
    vehicles: pd.DataFrame,
    model_settings: AVRoutingSettings | None = None,
    model_settings_file_name: str = "av_routing.yaml",
    trace_label: str = "av_routing",
    trace_hh_id: bool = False,
) -> None:
    """
    This model performs intra-household autonomous vehicle routing.
    """
    if model_settings is None:
        model_settings = AVRoutingSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name,
        )
    model_settings = setup_model_settings(state, model_settings)

    av_vehicles = vehicles[vehicles.vehicle_type.str.contains("-AV")].copy()
    av_eligible_trips = trips[
        trips.trip_mode.isin(model_settings.DRIVING_MODES)
        & trips.household_id.isin(av_vehicles.household_id)
    ].copy()
    time_periods = av_eligible_trips.depart.unique()
    time_periods.sort()

    if av_vehicles.empty or av_eligible_trips.empty:
        logger.info("No AVs or trips to process, skipping AV routing.")
        return

    # prepping table for models: labeling vehicles and adding init location to home
    av_eligible_trips["home_zone_id"] = av_eligible_trips.household_id.map(
        households.home_zone_id.to_dict()
    )
    av_vehicles["av_number"] = av_vehicles.groupby("household_id").cumcount() + 1
    av_vehicles["veh_location"] = households.home_zone_id.reindex(
        av_vehicles.household_id
    ).values
    vehicle_trips = None

    av_trip_matching_choices = []
    all_veh_trips = []

    # looping through time periods
    for time_period in time_periods:
        period_trace_label = tracing.extend_trace_label(trace_label, str(time_period))

        # trips in the time period
        trips_in_period = av_eligible_trips[av_eligible_trips.depart == time_period]
        if trips_in_period.empty:
            continue

        trips_in_period["trip_number"] = (
            trips_in_period.groupby("household_id").cumcount() + 1
        )

        choices = av_trip_matching(
            state,
            model_settings,
            trips=trips_in_period,
            vehicles=av_vehicles,
            trace_label=tracing.extend_trace_label(
                period_trace_label, "av_trip_matching"
            ),
        )

        choices["time_period"] = time_period
        av_trip_matching_choices.append(choices)

        veh_trips_this_period = execute_av_trip_matches(
            state, model_settings, choices, vehicle_trips, trips_in_period, av_vehicles
        )
        av_vehicles = update_vehicle_positions(veh_trips_this_period, av_vehicles)
        veh_trips_this_period = av_repositioning(
            state,
            model_settings,
            veh_trips_this_period,
            av_eligible_trips,
            av_vehicles,
            trace_label=tracing.extend_trace_label(
                period_trace_label, "av_repositioning"
            ),
        )
        # again update vehicle positions after av_repositioning model
        av_vehicles = update_vehicle_positions(veh_trips_this_period, av_vehicles)
        all_veh_trips.append(veh_trips_this_period)

    av_trip_matching_choices = pd.concat(av_trip_matching_choices, ignore_index=False)

    # create one table of all vehicle trips and add to pipeline
    vehicle_trips = pd.concat(all_veh_trips, ignore_index=False)
    state.add_table("vehicle_trips", vehicle_trips)
