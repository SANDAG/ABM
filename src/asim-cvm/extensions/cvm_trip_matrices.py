# ActivitySim
# See full license in LICENSE.txt.
from __future__ import annotations

import logging

import numpy as np
import openmatrix as omx
import pandas as pd

from activitysim.core import config, expressions, los, workflow

logger = logging.getLogger(__name__)


@workflow.step(copy_tables=["cv_trips"])
def write_cvm_trip_matrices(
    state: workflow.State,
    network_los: los.Network_LOS,
    cv_trips: pd.DataFrame,
    establishments: pd.DataFrame,
) -> None:
    """
    Write CVM trip matrices step.

    Adds boolean columns to local trips table via annotation expressions,
    then aggregates trip counts and writes OD matrices to OMX.  Save annotated
    trips table to pipeline if desired.

    Writes taz trip tables for one and two zone system.

    For one zone system, uses the land use table for the set of possible tazs.
    For two zone system, uses the taz skim zone names for the set of possible tazs.

    """

    if cv_trips is None:
        # this step is a NOP if there is no trips table
        # this might legitimately happen if they comment out some steps to debug but still want write_tables
        # this saves them the hassle of remembering to comment out this step
        logger.warning(
            "write_cv_trip_matrices returning empty-handed because there is no cv_trips table"
        )
        return

    model_settings = state.filesystem.read_model_settings("write_trip_matrices.yaml")
    # add establishment sample size to cv_trips
    est_weight_col = model_settings.get("EST_EXPANSION_WEIGHT_COL")
    cv_trips[est_weight_col] = establishments[est_weight_col].iloc[0]
    cv_trips_df = annotate_trips(state, cv_trips, network_los, model_settings)

    if bool(model_settings.get("SAVE_TRIPS_TABLE")):
        state.add_table("cv_trips", cv_trips_df)

    # write matrices by zone system type
    if network_los.zone_system == los.ONE_ZONE:  # taz trips written to taz matrices
        logger.info("aggregating trips one zone...")
        aggregate_trips = cv_trips_df.groupby(
            ["trip_origin", "trip_destination"], sort=False
        ).sum(numeric_only=True)

        # use the average household weight for all trips in the origin destination pair
        est_weight_col = model_settings.get("EST_EXPANSION_WEIGHT_COL")
        aggregate_weight = (
            cv_trips_df[["trip_origin", "trip_destination", est_weight_col]]
            .groupby(["trip_origin", "trip_destination"], sort=False)
            .mean()
        )
        aggregate_trips[est_weight_col] = aggregate_weight[est_weight_col]

        orig_vals = aggregate_trips.index.get_level_values("trip_origin")
        dest_vals = aggregate_trips.index.get_level_values("trip_destination")

        # use the land use table for the set of possible tazs
        land_use = state.get_dataframe("land_use")
        zone_index = land_use.index
        assert all(zone in zone_index for zone in orig_vals)
        assert all(zone in zone_index for zone in dest_vals)

        _, orig_index = zone_index.reindex(orig_vals)
        _, dest_index = zone_index.reindex(dest_vals)

        try:
            zone_labels = land_use[f"_original_{land_use.index.name}"]
        except KeyError:
            zone_labels = land_use.index

        write_matrices(
            state, aggregate_trips, zone_labels, orig_index, dest_index, model_settings
        )

    elif network_los.zone_system == los.TWO_ZONE:  # maz trips written to taz matrices
        logger.info("aggregating trips two zone...")
        cv_trips_df["otaz"] = (
            state.get_dataframe("land_use")
            .reindex(cv_trips_df["trip_origin"])
            .TAZ.tolist()
        )
        cv_trips_df["dtaz"] = (
            state.get_dataframe("land_use")
            .reindex(cv_trips_df["trip_destination"])
            .TAZ.tolist()
        )
        aggregate_trips = cv_trips_df.groupby(["otaz", "dtaz"], sort=False).sum(
            numeric_only=True
        )

        # use the average household weight for all trips in the origin destination pair
        est_weight_col = model_settings.get("EST_EXPANSION_WEIGHT_COL")
        aggregate_weight = (
            cv_trips_df[["otaz", "dtaz", est_weight_col]]
            .groupby(["otaz", "dtaz"], sort=False)
            .mean()
        )
        aggregate_trips[est_weight_col] = aggregate_weight[est_weight_col]

        orig_vals = aggregate_trips.index.get_level_values("otaz")
        dest_vals = aggregate_trips.index.get_level_values("dtaz")

        try:
            land_use_taz = state.get_dataframe("land_use_taz")
        except (KeyError, RuntimeError):
            pass  # table missing, ignore
        else:
            if "_original_TAZ" in land_use_taz.columns:
                orig_vals = orig_vals.map(land_use_taz["_original_TAZ"])
                dest_vals = dest_vals.map(land_use_taz["_original_TAZ"])

        zone_index = pd.Index(network_los.get_tazs(state), name="TAZ")
        assert all(zone in zone_index for zone in orig_vals)
        assert all(zone in zone_index for zone in dest_vals)

        _, orig_index = zone_index.reindex(orig_vals)
        _, dest_index = zone_index.reindex(dest_vals)

        write_matrices(
            state, aggregate_trips, zone_index, orig_index, dest_index, model_settings
        )
    else:
        None


@workflow.func
def annotate_trips(
    state: workflow.State, cv_trips: pd.DataFrame, network_los, model_settings
):
    """
    Add columns to local trips table. The annotator has
    access to the origin/destination skims and everything
    defined in the model settings CONSTANTS.

    Pipeline tables can also be accessed by listing them under
    TABLES in the preprocessor settings.
    """

    cv_trips_df = cv_trips

    trace_label = "trip_matrices"

    skim_dict = network_los.get_default_skim_dict()

    # setup skim keys
    if "trip_period" not in cv_trips_df:
        cv_trips_df["trip_period"] = network_los.skim_time_period_label(
            cv_trips_df.trip_start_time
        )
    od_skim_wrapper = skim_dict.wrap("trip_origin", "trip_destination")
    odt_skim_stack_wrapper = skim_dict.wrap_3d(
        orig_key="trip_origin", dest_key="trip_destination", dim3_key="trip_period"
    )
    skims = {"od_skims": od_skim_wrapper, "odt_skims": odt_skim_stack_wrapper}

    locals_dict = {}
    constants = config.get_model_constants(model_settings)
    if constants is not None:
        locals_dict.update(constants)

    expressions.annotate_preprocessors(
        state, cv_trips_df, locals_dict, skims, model_settings, trace_label
    )

    if not np.issubdtype(cv_trips_df["trip_period"].dtype, np.integer):
        if hasattr(skim_dict, "map_time_periods_from_series"):
            trip_period_idx = skim_dict.map_time_periods_from_series(
                cv_trips_df["trip_period"]
            )
            if trip_period_idx is not None:
                cv_trips_df["trip_period"] = trip_period_idx

    # Data will be expanded by an expansion weight column from
    # the households pipeline table, if specified in the model settings.
    hh_weight_col = model_settings.get("HH_EXPANSION_WEIGHT_COL")

    return cv_trips_df


def write_matrices(
    state: workflow.State,
    aggregate_trips,
    zone_index,
    orig_index,
    dest_index,
    model_settings,
    is_tap=False,
):
    """
    Write aggregated trips to OMX format.

    The MATRICES setting lists the new OMX files to write.
    Each file can contain any number of 'tables', each specified by a
    table key ('name') and a trips table column ('data_field') to use
    for aggregated counts.

    Any data type may be used for columns added in the annotation phase,
    but the table 'data_field's must be summable types: ints, floats, bools.
    """

    matrix_settings = model_settings.get("MATRICES")

    if not matrix_settings:
        logger.error("Missing MATRICES setting in write_trip_matrices.yaml")

    for matrix in matrix_settings:
        matrix_is_tap = matrix.get("is_tap", False)

        if matrix_is_tap == is_tap:  # only write tap matrices to tap matrix files
            filename = matrix.get("file_name")
            filepath = state.get_output_file_path(filename)
            logger.info("opening %s" % filepath)
            file = omx.open_file(str(filepath), "w")  # possibly overwrite existing file
            table_settings = matrix.get("tables")

            for table in table_settings:
                table_name = table.get("name")
                col = table.get("data_field")

                if col not in aggregate_trips:
                    logger.error(f"missing {col} column in aggregate_trips DataFrame")
                    return

                est_weight_col = model_settings.get("EST_EXPANSION_WEIGHT_COL")
                if est_weight_col:
                    aggregate_trips[col] = (
                        aggregate_trips[col] / aggregate_trips[est_weight_col]
                    )

                data = np.zeros((len(zone_index), len(zone_index)))
                data[orig_index, dest_index] = aggregate_trips[col]
                logger.debug(
                    "writing %s sum %0.2f" % (table_name, aggregate_trips[col].sum())
                )
                file[table_name] = data  # write to file

            # include the index-to-zone map in the file
            logger.info(
                "adding %s mapping for %s zones to %s"
                % (zone_index.name, zone_index.size, filename)
            )
            file.create_mapping(zone_index.name, zone_index.to_numpy())

            logger.info("closing %s" % filepath)
            file.close()
