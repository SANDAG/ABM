# -*- coding: utf-8 -*-
""" ABM Scenario Skim Appender Module.

This module contains classes holding all utilities relating to appending
transportation skims to a completed SANDAG Activity-Based Model (ABM)
scenario. This module is used to append time, distance, cost, and other
related transportation skims to ABM trip lists.

Notes:
    docstring style guide - http://google.github.io/styleguide/pyguide.html
"""

import itertools
import os
import re
from functools import lru_cache  # caching decorator for modules
import numpy as np
import openmatrix as omx  # https://github.com/osPlanning/omx-python
import pandas as pd


class SkimAppender(object):
    """ This class holds all utilities relating to appending transportation
    skims to a completed SANDAG Activity-Based Model (ABM) scenario

    Args:
        scenario_path: String location of the completed ABM scenario folder

    Methods:
        _get_omx_auto_skim_dataset: Maps ABM trip list records to OMX files
            and OMX skim matrices
        auto_fare_cost: Appends auto fare cost to ABM trip list records
        auto_operating_cost: Appends auto operating cost to ABM trip list records
        auto_wait_time: Appends auto wait times to ABM trip list records
        bicycle_skims: Appends bicycle mode skims (time, distance) to ABM trip
            list records
        drive_transit_skims: Appends input file accessam.csv drive to
            transit auto mode skims (time, distance, no cost) to ABM trip
            list records
        omx_auto_skim_appender: Appends OMX auto mode skims (time, distance,
            cost) to ABM trip list records
        omx_transit_skims: Appends OMX transit mode skims (time, distance,
            cost) to transit mode trip list records

    Properties:
        mgra_xref: Pandas DataFrame geography cross-reference of MGRAs to
            TAZs and LUZs
        properties: Dictionary of ABM properties file token values
            (conf/sandag_abm.properties) """

    def __init__(self, scenario_path: str) -> None:
        self.scenario_path = scenario_path

    @property
    @lru_cache(maxsize=1)
    def mgra_xref(self) -> pd.DataFrame:
        """ Cross reference of Master Geographic Reference Area (MGRA) model
        geography to Transportation Analysis Zone (TAZ) and Land Use Zone
        (LUZ) model geographies. Cross reference is stored in each ABM
        scenario input MGRA file (input/mgra13_based_input<<year>>.csv).
        """

        # load the mgra based input file
        fn = "mgra13_based_input" + str(self.properties["year"]) + ".csv"

        mgra = pd.read_csv(os.path.join(self.scenario_path, "input", fn),
                           usecols=["mgra",  # MGRA geography
                                    "taz",  # TAZ geography
                                    "luz_id"],  # LUZ geography
                           dtype={"mgra": "int16",
                                  "taz": "int16",
                                  "luz_id": "int16"})

        # genericize column names
        mgra.rename(columns={"mgra": "MGRA",
                             "taz": "TAZ",
                             "luz_id": "LUZ"},
                    inplace=True)

        return mgra

    @property
    @lru_cache(maxsize=1)
    def properties(self) -> dict:
        """ Get the ABM scenario properties from the ABM scenario
        properties file (conf/sandag_abm.properties).

        The return dictionary contains the following ABM scenario properties:
            aocFuel - auto operating fuel cost in $/mile
            aocMaintenance - auto operating maintenance cost in $/mile
            baseFareNonPooledTNC - initial Non-Pooled TNC fare in $
            baseFarePooledTNC - initial Pooled TNC fare in $
            baseFareTaxi - initial taxi fare in $
            bicycleSpeed - bicycle mode speed (miles/hour)
            costMinimumNonPooledTNC - minimum Non-Pooled TNC fare cost in $
            costMinimumPooledTNC - minimum Pooled TNC fare cost in $
            costPerMileFactorAV - auto operating cost per mile factor to
                apply to AV trips
            costPerMileNonPooledTNC - Non-Pooled TNC fare cost per mile in $
            costPerMilePooledTNC - Pooled TNC fare cost per mile in $
            costPerMileTaxi - Taxi fare cost per mile in $
            costPerMinuteNonPooledTNC - Non-Pooled TNC fare cost per minute in $
            costPerMinutePooledTNC - Pooled TNC fare cost per minute in $
            costPerMinuteTaxi - Taxi fare cost per minute in $
            microMobilitySpeed - micro mobility walk mode speed (miles/hour)
            terminalTimeFactorAV - terminal time factor to apply to AV trips
            waitTimeNonPooledTNC - list of mean wait times in minutes for
                Non-Pooled TNC by PopEmpDenPerMi categories
                (see waitTimePopEmpDenPerMi)
            waitTimePooledTNC - list of mean wait times in minutes for Pooled
                TNC by PopEmpDenPerMi categories (see waitTimePopEmpDenPerMi)
            waitTimePopEmpDenPerMi - list of MGRA-Based input file
                PopEmpDenPerMi values defining the wait time categories for
                Taxi/TNC modes
            waitTimeTaxi - list of mean wait times in minutes for Taxi by
                PopEmpDenPerMi categories (see waitTimePopEmpDenPerMi)
            walkSpeed - walk mode speed (miles/hour)
            year - analysis year of the ABM scenario

        Returns:
            A dictionary defining the ABM scenario properties. """

        # create dictionary holding ABM properties file information
        # each property contains a dictionary {line, value} where the line
        # is the string to match in the properties file to
        # return the value of the property
        lookup = {
            "aocFuel": {
                "line": "aoc.fuel=",
                "type": "float",
                "value": None
            },
            "aocMaintenance": {
                "line": "aoc.maintenance=",
                "type": "float",
                "value": None
            },
            "baseFareNonPooledTNC": {
                "line": "TNC.single.baseFare=",
                "type": "float",
                "value": None
            },
            "baseFarePooledTNC": {
                "line": "TNC.shared.baseFare=",
                "type": "float",
                "value": None
            },
            "baseFareTaxi": {
                "line": "taxi.baseFare=",
                "type": "float",
                "value": None
            },
            "bicycleSpeed": {
                "line": "active.bike.minutes.per.mile=",
                "type": "float",
                "value": None},
            "costMinimumNonPooledTNC": {
                "line": "TNC.single.costMinimum=",
                "type": "float",
                "value": None
            },
            "costMinimumPooledTNC": {
                "line": "TNC.shared.costMinimum=",
                "type": "float",
                "value": None
            },
            "costPerMileFactorAV": {
                "line": "Mobility.AV.CostPerMileFactor=",
                "type": "float",
                "value": None
            },
            "costPerMileNonPooledTNC": {
                "line": "TNC.single.costPerMile=",
                "type": "float",
                "value": None
            },
            "costPerMilePooledTNC": {
                "line": "TNC.shared.costPerMile=",
                "type": "float",
                "value": None
            },
            "costPerMileTaxi": {
                "line": "taxi.costPerMile=",
                "type": "float",
                "value": None
            },
            "costPerMinuteNonPooledTNC": {
                "line": "TNC.single.costPerMinute=",
                "type": "float",
                "value": None
            },
            "costPerMinutePooledTNC": {
                "line": "TNC.shared.costPerMinute=",
                "type": "float",
                "value": None
            },
            "costPerMinuteTaxi": {
                "line": "taxi.costPerMinute=",
                "type": "float",
                "value": None
            },
            "microMobilitySpeed": {
                "line": "active.micromobility.speed=",
                "type": "float",
                "value": None},
            "terminalTimeFactorAV": {
                "line": "Mobility.AV.TerminalTimeFactor=",
                "type": "float",
                "value": None
            },
            "waitTimeNonPooledTNC": {
                "line": "TNC.single.waitTime.mean=",
                "type": "list",
                "value": None
            },
            "waitTimePooledTNC": {
                "line": "TNC.shared.waitTime.mean=",
                "type": "list",
                "value": None
            },
            "waitTimePopEmpDenPerMi": {
                "line": "WaitTimeDistribution.EndPopEmpPerSqMi=",
                "type": "list",
                "value": None
            },
            "waitTimeTaxi": {
                "line": "Taxi.waitTime.mean=",
                "type": "list",
                "value": None
            },
            "walkSpeed": {
                "line": "active.walk.minutes.per.mile=",
                "type": "float",
                "value": None},
            "year": {
                "line": "scenarioYear=",
                "type": "int",
                "value": None}
        }

        # open the ABM scenario properties file
        file = open(os.path.join(self.scenario_path, "conf", "sandag_abm.properties"), "r")

        # loop through each line of the properties file
        for line in file:
            # strip all white space from the line
            line = line.replace(" ", "")

            # for each element of the properties dictionary
            for name in lookup:
                item = lookup[name]

                if item["line"] is not None:
                    match = re.compile(item["line"]).match(line)
                    # if the properties file contains the matching line
                    if match:
                        # for waitTime properties create list from matching line
                        if "waitTime" in name:
                            value = line[match.end():].split(",")
                            value = list(map(float, value))
                        # otherwise take the final element of the line
                        else:
                            value = line[match.end():]

                        # update the dictionary value using the appropriate data type
                        if item["type"] == "float":
                            value = float(value)
                        elif item["type"] == "int":
                            value = int(value)
                        else:
                            pass

                        item["value"] = value
                        break

        file.close()

        # convert the property name and value to a non-nested dictionary
        results = {}
        for name in lookup:
            results[name] = lookup[name]["value"]

        # convert auto operating costs from cents per mile to dollars per mile
        results["aocFuel"] = results["aocFuel"] / 100
        results["aocMaintenance"] = results["aocMaintenance"] / 100

        # convert AT speeds from minutes per mile to miles per hour
        results["bicycleSpeed"] = (results["bicycleSpeed"] * 1/60)**-1
        results["microMobilitySpeed"] = (results["microMobilitySpeed"] * 1 / 60) ** -1
        results["walkSpeed"] = (results["walkSpeed"] * 1/60)**-1

        return results

    @staticmethod
    def _get_omx_auto_skim_dataset(df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame and returns a subset of trips
        with two columns indicating the proper auto-mode omx file name and
        skim matrix to use to append omx auto-mode transportation skims.

        If trips in the input DataFrame are not mapped to omx files
        and matrices (non-auto mode trips) they are not present in the return
        DataFrame. The return DataFrame is an interim DataFrame ready for
        input to the omx_auto_skim_appender

        The process uses the trip departure ABM 5 TOD period, trip mode,
        transponder availability, and the trip value of time (vot) category
        to select the correct auto skim-set file and matrix to use to get
        the auto-mode trip times, distances, and toll costs.

        Args:
            df: Input Pandas DataFrame containing the fields
                    [tripID] - unique identifier of a trip
                    [departTimeFiveTod] - trip departure ABM 5 TOD periods (1-5)
                    [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                    [transponderAvailable] - indicator if transponder available
                        on the trip (False, True)
                    [valueOfTimeCategory] - trip value of time categories
                        (Low, Medium, High)
                    [originTAZ] - trip origin TAZ (1-4996)
                    [destinationTAZ] - trip destination TAZ (1-4996)
                    [parkingTAZ] - trip parking TAZ ("", 1-4996)

        Returns:
            A Pandas DataFrame containing mapping of auto-mode trips to omx
            files and matrices containing auto-mode skims:
                [tripID] - unique identifier of a trip
                [omxFileName] - omx skim file name (e.g. traffic_skims_EA)
                [matrixName] - omx skim matrix name (e.g. EA_HOV2_M)
                [originTAZ] - trip origin TAZ (1-4996)
                [destinationTAZ] - trip destination TAZ (1-4996) """

        # set destinationTAZ equal to parkingTAZ is parkingTAZ is not missing
        # auto portion of trip ends at the parkingTAZ
        df.destinationTAZ = np.where(pd.notna(df.parkingTAZ),
                                     df.parkingTAZ,
                                     df.destinationTAZ).astype("int16")

        # create mappings from trip list to skim matrices
        # segmented by SOV and Non-SOV as process differs between the two
        # due to Transponder/Non-Transponder skimming in SOV mode
        # SOV - departTimeFiveTod + tripMode + transponderAvailable +
        #   valueOfTimeCategory
        # nonSOV - departTimeFiveTod + tripMode + valueOfTimeCategory
        skim_map = {"SOV": {"values": [[1, 2, 3, 4, 5],
                                       ["Drive Alone"],
                                       [False, True],
                                       ["Low", "Medium", "High"]],
                            "labels": [["EA", "AM", "MD", "PM", "EV"],
                                       ["SOV"],
                                       ["NT", "TR"],
                                       ["L", "M", "H"]],
                            "cols": ["departTimeFiveTod",
                                     "tripMode",
                                     "transponderAvailable",
                                     "valueOfTimeCategory",
                                     "matrixName"]},
                    "Non-SOV": {"values": [[1, 2, 3, 4, 5],
                                           ["Shared Ride 2",
                                            "Shared Ride 3+",
                                            "Light Heavy Duty Truck",
                                            "Medium Heavy Duty Truck",
                                            "Heavy Heavy Duty Truck",
                                            "Taxi",
                                            "Non-Pooled TNC",
                                            "Pooled TNC",
                                            "School Bus"],
                                           ["Low", "Medium", "High"]],
                                "labels": [["EA", "AM", "MD", "PM", "EV"],
                                           ["HOV2", "HOV3", "TRK", "TRK",
                                            "TRK", "HOV3", "HOV3", "HOV3",
                                            "HOV3"],
                                           ["L", "M", "H"]],
                                "cols": ["departTimeFiveTod",
                                         "tripMode",
                                         "valueOfTimeCategory",
                                         "matrixName"]}}

        # initialize empty auto trips DataFrame
        trips = pd.DataFrame()

        # map possible values of trip list columns to skim matrix names
        # filter input DataFrame to auto trips mapped to skim matrices
        for key in skim_map:
            values = skim_map[key]["values"]
            labels = skim_map[key]["labels"]
            cols = skim_map[key]["cols"]

            mapping = [list(i) + ["_".join(j)] for i, j in
                       zip(itertools.product(*values),
                           itertools.product(*labels))]

            # create Pandas DataFrame lookup table of column values
            # to skim matrix names
            lookup = pd.DataFrame(mapping, columns=cols)
            lookup["omxFileName"] = "traffic_skims_" + lookup["matrixName"].str[0:2]

            # set lookup DataFrame data types
            lookup = lookup.astype({
                "departTimeFiveTod": "int8",
                "tripMode": "category",
                "valueOfTimeCategory": "category",
                "omxFileName": "category",
                "matrixName": "category"})

            # merge lookup table to trip list and append to auto trips DataFrame
            trips = trips.append(df.merge(lookup, how="inner"), ignore_index=True)

        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        trips.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        return trips[["tripID",
                      "omxFileName",
                      "matrixName",
                      "originTAZ",
                      "destinationTAZ"]]

    def append_skims(self, df: pd.DataFrame, auto_only: bool, terminal_skims: bool) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame, runs all skimming class
        methods and appends all skims to the input Pandas DataFrame.
        See documentation of included class methods. Additionally creates and
        appends three fields to the input Pandas DataFrame:
            [timeTotal] - total trip time in minutes
            [distanceTotal] - total trip distance in miles
            [costTotal] - total trip cost in dollars

        Args:
            df: Input Pandas DataFrame containing fields required by the
                included class methods.
            terminal_skims: Boolean of whether to include auto-mode terminal
                skims, only apply to Resident model trip lists.
            auto_only: Boolean of whether to include only basic auto-mode
                skims, applies to Commercial Vehicle, Zombie AV, and Zombie
                TNC trip lists.

        Returns:
            A Pandas DataFrame containing all skims appended by the included
            class methods. The DataFrame contains all original fields
            of the input DataFrame along with fields appended by the
            aforementioned class methods. """

        # append omx auto skims
        df = self.omx_auto_skim_appender(df)

        # append auto operating cost
        df = self.auto_operating_cost(df)

        if auto_only:
            pass
        else:
            # append auto terminal skims if applicable
            if terminal_skims:
                df = self.auto_terminal_skims(df)

            # append TNC fare costs
            df = self.tnc_fare_cost(df)

            # append TNC wait times
            df = self.tnc_wait_time(df)

            # append omx transit skims
            df = self.omx_transit_skims(df)

            # append drive to transit skims
            # auto-operating cost not included
            # TNC fare costs and wait times not included
            df = self.drive_transit_skims(df)

            # append (micro-mobility/micro-transit/walk) to transit skims
            df = self.walk_transit_skims(df)

            # append micro-mobility/micro-transit/walk skims
            df = self.walk_skims(df)

            # append bicycle skims
            df = self.bicycle_skims(df)

        # append total trip time, distance, and cost skims
        # transit in vehicle times by line haul mode cannot be included
        transit_line_haul_cols = ["timeTier1TransitInVehicle",
                                  "timeFreewayRapidTransitInVehicle",
                                  "timeArterialRapidTransitInVehicle",
                                  "timeExpressBusTransitInVehicle",
                                  "timeLocalBusTransitInVehicle",
                                  "timeLightRailTransitInVehicle",
                                  "timeCommuterRailTransitInVehicle"]

        df["timeTotal"] = df[df.columns.difference(transit_line_haul_cols)].filter(regex="^time").sum(axis=1)

        df["distanceTotal"] = df.filter(regex="^distance").sum(axis=1)

        df["costTotal"] = df.filter(regex="^cost").sum(axis=1)

        return df

    def auto_operating_cost(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [avUsed]) and the fields appended by the
        SkimAppender class method omx_auto_skim_appender
        ([distanceDrive]) and returns the associated
        auto-mode operating cost.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [avUsed] - indicator if AV used on trip (True, False)
                [distanceDrive] - distance in miles for auto mode

        Returns:
            A Pandas DataFrame containing all associated auto-mode
            operating costs. The DataFrame contains all original fields
            of the input DataFrame along with the field:
                [costOperatingDrive] - auto operating cost in $ """

        # calculate auto operating cost as fuel+maintenance cents per mile
        # note this is scaled for AV trips by a factor
        aoc = self.properties["aocFuel"] + self.properties["aocMaintenance"]
        aoc_av = aoc * self.properties["costPerMileFactorAV"]

        # calculate auto operating cost for auto-mode trips
        # exclude Taxi/TNC as operating cost is passed to drivers
        # who are not considered part of the model universe
        auto_modes = ["Drive Alone",
                      "Shared Ride 2",
                      "Shared Ride 3+",
                      "Light Heavy Duty Truck",
                      "Medium Heavy Duty Truck",
                      "Heavy Heavy Duty Truck"]

        conditions = [
            np.array(df["tripMode"].isin(auto_modes) & ~df["avUsed"],
                     dtype="bool"),
            np.array(df["tripMode"].isin(auto_modes) & df["avUsed"],
                     dtype="bool")
        ]

        choices = [df["distanceDrive"] * aoc,
                   df["distanceDrive"] * aoc_av]

        df["costOperatingDrive"] = pd.Series(
            np.select(conditions, choices, default=0),
            dtype="float32")

        # return input DataFrame with appended auto operating cost column
        return df

    def auto_terminal_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [destinationTAZ]) and appends the auto-mode
        terminal walk time from the input/zone.term file. Distance is created
        from the walk speed specified in the properties file
        conf/sandag_abm.properties

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [destinationTAZ] - indicator if AV used on trip (True, False)

        Returns:
            A Pandas DataFrame containing auto-mode terminal walk time and
            distance. The DataFrame contains all original fields of the input
            DataFrame along with the fields:
                [timeAutoTerminalWalk] - auto terminal walk time in minutes
                [distanceAutoTerminalWalk] - auto terminal walk distance in
                    miles
        """
        # read in auto terminal time fixed width file
        skims = pd.read_fwf(
            os.path.join(self.scenario_path, "input", "zone.term"),
            widths=[5, 7],
            names=["destinationTAZ",
                   "timeAutoTerminalWalk"],
            dtype={"destinationTAZ": "int16",
                   "timeAutoTerminalWalk": "float32"}
        )

        # merge auto terminal times into input DataFrame
        # add 0s for TAZs with no terminal times
        df = df.merge(skims, on="destinationTAZ", how="left")
        df["timeAutoTerminalWalk"] = df["timeAutoTerminalWalk"].fillna(0)

        # set auto terminal times to 0 for non-auto modes
        # reduce auto terminal time if AV is used
        auto_modes = ["Drive Alone", "Shared Ride 2", "Shared Ride 3+"]
        av_factor = self.properties["terminalTimeFactorAV"]

        conditions = [
            np.array(df["tripMode"].isin(auto_modes) & ~df["avUsed"],
                     dtype="bool"),
            np.array(df["tripMode"].isin(auto_modes) & df["avUsed"],
                     dtype="bool")
        ]

        choices = [df["timeAutoTerminalWalk"],
                   df["timeAutoTerminalWalk"] * av_factor]

        df["timeAutoTerminalWalk"] = pd.Series(
            np.select(conditions, choices, default=0),
            dtype="float32")

        # create auto terminal distance from walk speed property
        df["distanceAutoTerminalWalk"] = pd.Series(
            df["timeAutoTerminalWalk"] * self.properties["walkSpeed"],
            dtype="float32")

        return df

    def bicycle_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [originMGRA], [destinationMGRA], [originTAZ],
        [destinationTAZ]) and returns the associated bicycle mode skims for
        time and distance.

        Bicycle mode skims are given at the MGRA-MGRA level by the
        output/bikeMgraLogsum.csv file and TAZ-TAZ level by the
        output/bikeTazLogsum.csv file. If a MGRA-MGRA o-d pair is not present
        in the MGRA-MGRA level then the TAZ-TAZ level skim is used. Note the
        skims only provide time in minutes so distance is created using the
        bicycle speed property set in the conf/sandag_abm.properties file.
        Non-bicycle modes have all skims set to 0.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [originMGRA] - trip origin MGRA (1-23002)
                [destinationMGRA] - trip destination MGRA (1-23002)
                [originTAZ] - trip origin TAZ (1-4996)
                [destinationTAZ] - trip destination TAZ (1-4996)

        Returns:
            A Pandas DataFrame containing all associated bicycle mode
            skims for time and distance. The DataFrame contains all the
            original fields of the input DataFrame along with the fields:
                [timeBike] - time in minutes for bicycle mode
                [distanceBike] - distance in miles for bicycle mode """

        # load the MGRA-MGRA bicycle skims
        mgra_skims = pd.read_csv(
            os.path.join(self.scenario_path, "output", "bikeMgraLogsum.csv"),
            usecols=["i",  # origin MGRA geography
                     "j",  # destination MGRA geography
                     "time"],  # time in minutes
            dtype={"i": "int16",
                   "j": "int16",
                   "time": "float32"}
        )

        # load the TAZ-TAZ bicycle skims
        taz_skims = pd.read_csv(
            os.path.join(self.scenario_path, "output", "bikeTazLogsum.csv"),
            usecols=["i",  # origin TAZ geography
                     "j",  # destination TAZ geography
                     "time"],  # time in minutes
            dtype={"i": "int16",
                   "j": "int16",
                   "time": "float32"}
        )

        # merge the skims with the input DataFrame bicycle mode records
        # use left outer joins to keep all bicycle mode records
        # if MGRA-MGRA skims do not exist
        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        records = df.loc[(df["tripMode"] == "Bike")].copy()
        records.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        records = records.merge(
            right=mgra_skims,
            how="left",
            left_on=["originMGRA", "destinationMGRA"],
            right_on=["i", "j"]
        )
        records = records.merge(
            right=taz_skims,
            how="left",
            left_on=["originTAZ", "destinationTAZ"],
            right_on=["i", "j"],
            suffixes=["MGRA", "TAZ"]
        )

        # if MGRA-MGRA skims do not exist use TAZ-TAZ skims
        records["timeBike"] = pd.Series(
            np.where(records["timeMGRA"].isna(),
                     records["timeTAZ"],
                     records["timeMGRA"]),
            dtype="float32")

        # calculate distance using bicycle speed
        records["distanceBike"] = pd.Series(
            records["timeBike"] * self.properties["bicycleSpeed"],
            dtype="float32")

        # merge result set DataFrame back into initial trip list
        records = records[["tripID", "timeBike", "distanceBike"]]
        df = df.merge(records, on="tripID", how="left")

        # set missing skim records to 0 as a missing skim means no bike trip
        skim_cols = ["timeBike", "distanceBike"]
        df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df

    def drive_transit_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [originTAZ], [destinationTAZ],
        [boardingTAP], [alightingTAP], and [inbound]) and returns the
        associated  drive to transit auto-mode skims for time and distance.

        The model uses an on-the-fly input file (input\accessam.csv) for
        drive to transit auto-mode skims and assumes no fare or toll costs.
        Non-drive to transit modes have all skims set to 0.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [originTAZ] - trip origin TAZ (1-4996)
                [destinationTAZ] - trip destination TAZ (1-4996)
                [boardingTAP] - trip boarding transit access point (TAP)
                [alightingTAP] - trip alighting transit access point (TAP)
                [inbound] - direction of trip on tour (False, True)

        Returns:
            A Pandas DataFrame containing all associated drive to transit
            auto-mode skims for time and distance (assumed no toll cost).
            The DataFrame contains all the original fields of the input
            DataFrame along with the fields:
                [timeDriveTransit] - time in minutes for auto mode
                [distanceDriveTransit] - distance in miles for auto mode """

        # read in the input accessam csv file
        # this file is used in place of auto skim matrices for drive to transit
        skims = pd.read_csv(self.scenario_path + "/input/accessam.csv",
                            names=["TAZ",  # TAZ geography
                                   "TAP",  # transit access point (TAP)
                                   "timeDriveTransit",  # time in minutes
                                   "distanceDriveTransit",  # distance in miles
                                   "mode"],
                            usecols=["TAZ",
                                     "TAP",
                                     "timeDriveTransit",
                                     "distanceDriveTransit"],
                            dtype={"TAZ": "int16",
                                   "TAP": "int16",
                                   "timeDriveTransit": "float32",
                                   "distanceDriveTransit": "float32"})

        # select drive to transit records
        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        modes = ["Park and Ride to Transit - Local Bus",
                 "Park and Ride to Transit - Premium Transit",
                 "Park and Ride to Transit - Local Bus and Premium Transit",
                 "Kiss and Ride to Transit - Local Bus",
                 "Kiss and Ride to Transit - Premium Transit",
                 "Kiss and Ride to Transit - Local Bus and Premium Transit",
                 "TNC to Transit - Local Bus",
                 "TNC to Transit - Premium Transit",
                 "TNC to Transit - Local Bus and Premium Transit"]
        records = df.loc[(df["tripMode"].isin(modes))].copy()
        records.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        # create MGRA-TAP origin-destinations based on inbound direction
        records["MGRA"] = np.where(records["inbound"],
                                   records["destinationMGRA"],
                                   records["originMGRA"])

        records["TAP"] = np.where(records["inbound"],
                                  records["alightingTAP"],
                                  records["boardingTAP"])

        # use the origin/destination MGRA to derive the origin/destination
        # TAZ, this accounts for issues where external TAZs (1-12) do not have
        # TAP-based skims, the skims are derived from the internal TAZ of the
        # internal MGRA of the trip origin/destination
        records = records.merge(self.mgra_xref, on="MGRA")

        # merge with the drive to transit access skims
        records = records[["tripID", "TAZ", "TAP"]]
        records = records.merge(skims, how="inner", on=["TAZ", "TAP"])
        records = records[["tripID", "timeDriveTransit", "distanceDriveTransit"]]

        # merge result set DataFrame back into initial trip list
        df = df.merge(records, on="tripID", how="left")

        # set missing skim records to 0 as a missing skim means no transit trip
        skim_cols = ["timeDriveTransit", "distanceDriveTransit"]
        df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df

    def omx_auto_skim_appender(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame and returns the DataFrame with
        associated auto-mode skims for time, distance, and toll cost appended.

        The process uses the trip departure ABM 5 TOD period, trip mode,
        transponder availability, and the trip value of time (vot) category
        to select the correct auto skim-set to get the trip time, distance,
        and toll costs. Non-auto modes have all skims set to 0.

        Args:
            df: Input Pandas DataFrame containing the fields
                    [tripID] - unique identifier of a trip
                    [departTimeFiveTod] - trip departure ABM 5 TOD periods (1-5)
                    [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                    [transponderAvailable] - indicator if transponder available
                        on the trip (False, True)
                    [valueOfTimeCategory] - trip value of time categories
                        (Low, Medium, High)
                    [originTAZ] - trip origin TAZ (1-4996)
                    [destinationTAZ] - trip destination TAZ (1-4996)
                    [parkingTAZ] - trip parking TAZ ("", 1-4996)

        Returns:
            The input Pandas DataFrame with auto-mode skims for time,
            distance, and toll cost appended. The DataFrame contains all the
            original fields of the input DataFrame along with the fields:
                [timeDrive] - time in minutes for auto mode
                [distanceDrive] - distance in miles for auto mode
                [costTollDrive] - toll cost in $ for auto mode """

        # prepare the input DataFrame for skim appending
        # selecting records that use the input omx file
        df_map = self._get_omx_auto_skim_dataset(df)

        # initialize output skim list
        output = []

        for omx_fn in df_map.omxFileName.unique():

            # open the input omx file
            fn = os.path.join(self.scenario_path, "output", omx_fn + ".omx")
            omx_file = omx.open_file(fn)

            # filter records mapped to omx file
            records_omx = df_map.loc[df_map.omxFileName == omx_fn]

            # for each skim matrix in the data-set
            for matrix in records_omx.matrixName.unique():
                # filter records mapped to skim matrix
                records = records_omx.loc[records_omx.matrixName == matrix]

                # create set of unique origin-destination pairs
                # get time, distance, cost associated with the o-d pairs
                od = set(zip(records.originTAZ, records.destinationTAZ))
                o, d = zip(*od)

                # auto omx matrices do not have zone to index mappings
                # assume 1-index to 0-index mappings
                o_idx = [number - 1 for number in o]
                d_idx = [number - 1 for number in d]

                skims = list(zip(
                    [omx_fn] * len(o),
                    [matrix] * len(o),
                    o, d,
                    omx_file[matrix + "_TIME"][o_idx, d_idx],
                    omx_file[matrix + "_DIST"][o_idx, d_idx],
                    omx_file[matrix + "_TOLLCOST"][o_idx, d_idx] / 100))

                output.extend(skims)

            omx_file.close()

        # create DataFrame from output skim list
        output = pd.DataFrame(data=output,
                              columns=["omxFileName",
                                       "matrixName",
                                       "originTAZ",
                                       "destinationTAZ",
                                       "timeDrive",
                                       "distanceDrive",
                                       "costTollDrive"])

        # set data types of output skims list
        output = output.astype({
            "omxFileName": "category",
            "matrixName": "category",
            "originTAZ": "int16",
            "destinationTAZ": "int16",
            "timeDrive": "float32",
            "distanceDrive": "float32",
            "costTollDrive": "float32"
        })

        # merge the output skim list back to the mapped input DataFrame
        # appending auto skims to tripIDs
        output = output.merge(right=df_map, how="inner")

        output = output[["tripID",
                         "timeDrive",
                         "distanceDrive",
                         "costTollDrive"]]

        # merge the output skim list back to the original input DataFrame
        df = df.merge(right=output, on="tripID", how="left")

        # set missing skim records to 0 as a missing skim means no auto trip
        skim_cols = ["timeDrive", "distanceDrive", "costTollDrive"]
        df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df

    def omx_transit_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the fields
        ([tripID], [departTimeFiveTod], [tripMode], [boardingTAP],
        [alightingTAP]) and returns the associated transit mode skims for time,
        distance, and fare cost.

        The process uses the trip departure ABM 5 TOD period and trip mode to
        select the correct transit skim-set to use to get the trip time,
        distance, and fare costs. Non-transit modes have all skims set to 0.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [departTimeFiveTod] - trip departure ABM 5 TOD periods (1-5)
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [boardingTAP] - trip boarding transit access point (TAP)
                [alightingTAP] - trip alighting transit access point (TAP)

        Returns:
            A Pandas DataFrame containing all associated transit-mode skims for
            time, distance, and fare cost. The DataFrame contains all the
            original fields of the input DataFrame along with the fields:
                [timeTransitInVehicle] - transit in-vehicle time in minutes
                [timeTier1TransitInVehicle] - tier 1 line haul mode transit
                    in-vehicle time in minutes
                [timeFreewayRapidTransitInVehicle] - freeway rapid line haul
                    mode transit in-vehicle time in minutes
                [timeArterialRapidTransitInVehicle] - arterial rapid line haul
                    mode transit in-vehicle time in minutes
                [timeExpressBusTransitInVehicle] - express bus line haul
                    mode transit in-vehicle time in minutes
                [timeLocalBusTransitInVehicle] - local bus line haul
                    mode transit in-vehicle time in minutes
                [timeLightRailTransitInVehicle] - light rail line haul
                    mode transit in-vehicle time in minutes
                [timeCommuterRailTransitInVehicle] - commuter rail line haul
                    mode transit in-vehicle time in minutes
                [timeTransitInitialWait] - initial transit wait time in minutes
                [timeTransitWait] - total transit wait time in minutes
                [timeTransitWalk] - total transit walk time in minutes
                [distanceTransitInVehicle] - transit in-vehicle distance in miles
                [distanceTransitWalk] - total transit walk distance in miles
                [costFareTransit] - fare cost in $ for transit mode
                [transfersTransit] - number of transfers """

        # create mappings from trip list to skim matrices
        # departTimeFiveTod + tripMode
        skim_map = {"values": [[1, 2, 3, 4, 5],
                               ["Walk to Transit - Local Bus",
                                "Walk to Transit - Premium Transit",
                                "Walk to Transit - Local Bus and Premium Transit",
                                "Park and Ride to Transit - Local Bus",
                                "Park and Ride to Transit - Premium Transit",
                                "Park and Ride to Transit - Local Bus and Premium Transit",
                                "Kiss and Ride to Transit - Local Bus",
                                "Kiss and Ride to Transit - Premium Transit",
                                "Kiss and Ride to Transit - Local Bus and Premium Transit",
                                "TNC to Transit - Local Bus",
                                "TNC to Transit - Premium Transit",
                                "TNC to Transit - Local Bus and Premium Transit"]],
                    "labels": [["EA", "AM", "MD", "PM", "EV"],
                               ["BUS", "PREM", "ALLPEN",
                                "BUS", "PREM", "ALLPEN",
                                "BUS", "PREM", "ALLPEN",
                                "BUS", "PREM", "ALLPEN"]],
                    "cols": ["departTimeFiveTod",
                             "tripMode",
                             "matrixName"]}

        # initialize empty result set DataFrame
        result = pd.DataFrame()

        # map possible values of trip list columns to skim matrix names
        mapping = [list(i) + ["_".join(j)] for i, j in
                   zip(itertools.product(*skim_map["values"]),
                       itertools.product(*skim_map["labels"]))]

        # create Pandas DataFrame lookup table of column values
        # to skim matrix names
        lookup = pd.DataFrame(mapping, columns=skim_map["cols"])

        # set data types of lookup table
        lookup = lookup.astype({
            "departTimeFiveTod": "int8",
            "tripMode": "category",
            "matrixName": "category"
        })

        # merge lookup table to trip list make DataFrame unique by tripID
        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        trips = df.merge(lookup, how="inner")
        trips.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        # open omx transit skim file and get TAP:element matrix mapping
        omx_file = omx.open_file(self.scenario_path + "/output/transit_skims.omx")
        omx_map = omx_file.mapping("zone_number")

        # for each skim matrix in the data-set
        for matrix in trips.matrixName.unique():
            # select records that use the skim matrix
            records = trips.loc[(trips["matrixName"] == matrix)].copy()

            # get lists of o-ds
            o = records.boardingTAP.astype("int16").tolist()
            d = records.alightingTAP.astype("int16").tolist()

            # map o-ds to omx matrix indices
            o_idx = [omx_map[number] for number in o]
            d_idx = [omx_map[number] for number in d]

            # append skims
            records["timeTransitInVehicle"] = omx_file[matrix + "_TOTALIVTT"][o_idx, d_idx]
            records["timeTier1TransitInVehicle"] = omx_file[matrix + "_TIER1IVTT"][o_idx, d_idx]
            records["timeFreewayRapidTransitInVehicle"] = omx_file[matrix + "_BRTYELIVTT"][o_idx, d_idx]
            records["timeArterialRapidTransitInVehicle"] = omx_file[matrix + "_BRTREDIVTT"][o_idx, d_idx]
            records["timeExpressBusTransitInVehicle"] = omx_file[matrix + "_EXPIVTT"][o_idx, d_idx]
            records["timeLocalBusTransitInVehicle"] = omx_file[matrix + "_BUSIVTT"][o_idx, d_idx]
            records["timeLightRailTransitInVehicle"] = omx_file[matrix + "_LRTIVTT"][o_idx, d_idx]
            records["timeCommuterRailTransitInVehicle"] = omx_file[matrix + "_CMRIVTT"][o_idx, d_idx]
            records["timeTransitInitialWait"] = omx_file[matrix + "_FIRSTWAIT"][o_idx, d_idx]
            records["timeTransitWait"] = omx_file[matrix + "_TOTALWAIT"][o_idx, d_idx]
            records["timeTransitWalk"] = omx_file[matrix + "_TOTALWALK"][o_idx, d_idx]
            records["distanceTransitInVehicle"] = omx_file[matrix + "_TOTDIST"][o_idx, d_idx]
            records["costFareTransit"] = omx_file[matrix + "_FARE"][o_idx, d_idx]
            records["transfersTransit"] = omx_file[matrix + "_XFERS"][o_idx, d_idx]
            records["distanceTransitWalk"] = records.timeTransitWalk * self.properties["walkSpeed"]

            # set skim data types
            records = records.astype({
                "timeTransitInVehicle": "float32",
                "timeTransitInitialWait": "float32",
                "timeTransitWait": "float32",
                "timeTransitWalk": "float32",
                "distanceTransitInVehicle": "float32",
                "costFareTransit": "float32",
                "transfersTransit": "float32",
                "distanceTransitWalk": "float32"})

            records = records[["tripID",
                               "timeTransitInVehicle",
                               "timeTier1TransitInVehicle",
                               "timeFreewayRapidTransitInVehicle",
                               "timeArterialRapidTransitInVehicle",
                               "timeExpressBusTransitInVehicle",
                               "timeLocalBusTransitInVehicle",
                               "timeLightRailTransitInVehicle",
                               "timeCommuterRailTransitInVehicle",
                               "timeTransitInitialWait",
                               "timeTransitWait",
                               "timeTransitWalk",
                               "distanceTransitInVehicle",
                               "distanceTransitWalk",
                               "costFareTransit",
                               "transfersTransit"]]

            result = result.append(records, ignore_index=True)

        omx_file.close()

        skim_cols = ["timeTransitInVehicle",
                     "timeTier1TransitInVehicle",
                     "timeFreewayRapidTransitInVehicle",
                     "timeArterialRapidTransitInVehicle",
                     "timeExpressBusTransitInVehicle",
                     "timeLocalBusTransitInVehicle",
                     "timeLightRailTransitInVehicle",
                     "timeCommuterRailTransitInVehicle",
                     "timeTransitInitialWait",
                     "timeTransitWait",
                     "timeTransitWalk",
                     "distanceTransitInVehicle",
                     "distanceTransitWalk",
                     "costFareTransit",
                     "transfersTransit"]

        # if there are no transit trip skim records
        if result.empty:
            # append skim columns to input DataFrame
            # set to 0 as a missing skim means no transit trip
            for col in skim_cols:
                df[col] = 0
        else:
            # merge result set DataFrame back into initial trip list
            df = df.merge(result, on="tripID", how="left")

            # set missing skim records to 0 as a missing skim means no transit trip
            df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df

    def tnc_fare_cost(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [avUsed]) and the fields appended by the
        SkimAppender class method omx_auto_skim_appender
        ([timeDrive], [distanceDrive]) and returns the associated
        fare costs.

        Note that drive to transit modes assume no fare costs and are not
        included here.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [timeDrive] - time in minutes for auto mode
                [distanceDrive] - distance in miles for auto mode

        Returns:
            A Pandas DataFrame containing all associated auto-mode fare
            costs. The DataFrame contains all original fields of the input
            DataFrame along with the field:
                [costFareDrive] - auto mode fare cost in $ """

        # calculate fare costs for non-pooled TNC, pooled TNC, and taxi modes
        conditions = [df["tripMode"] == "Non-Pooled TNC",
                      df["tripMode"] == "Pooled TNC",
                      df["tripMode"] == "Taxi"]

        # calculate non-pooled TNC fare incorporating minimum fare
        non_pooled_tnc_fare = pd.Series(
            self.properties["baseFareNonPooledTNC"] +
            self.properties["costPerMileNonPooledTNC"] * df["distanceDrive"] +
            self.properties["costPerMinuteNonPooledTNC"] * df["timeDrive"],
            dtype="float32")

        non_pooled_tnc_fare = np.where(
            non_pooled_tnc_fare < self.properties["costMinimumNonPooledTNC"],
            self.properties["costMinimumNonPooledTNC"],
            non_pooled_tnc_fare)

        # calculate pooled TNC fare incorporating minimum fare
        pooled_tnc_fare = pd.Series(
            self.properties["baseFarePooledTNC"] +
            self.properties["costPerMilePooledTNC"] * df["distanceDrive"] +
            self.properties["costPerMinutePooledTNC"] * df["timeDrive"],
            dtype="float32")

        pooled_tnc_fare = np.where(
            pooled_tnc_fare < self.properties["costMinimumPooledTNC"],
            self.properties["costMinimumPooledTNC"],
            pooled_tnc_fare)

        choices = [
            non_pooled_tnc_fare,
            pooled_tnc_fare,
            self.properties["baseFareTaxi"] +
            self.properties["costPerMileTaxi"] * df["distanceDrive"] +
            self.properties["costPerMinuteTaxi"] * df["timeDrive"]
        ]

        df["costFareDrive"] = pd.Series(
            np.select(conditions, choices, default=0),
            dtype="float32")

        # return input DataFrame with appended auto fare cost column
        return df

    def tnc_wait_time(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [originMGRA]) and returns the associated wait
        times for Taxi and TNC modes.

        Note that drive to transit modes assume no auto-mode wait time and
        are not included here.

        Note that the actual wait times experienced by trips are drawn from a
        distribution but not written out to the trip output files making it
        impossible to write out the actual wait time experienced. The mean is
        used here for all trips as an approximation.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [originMGRA] - trip origin MGRA geography

        Returns:
            A Pandas DataFrame containing all associated auto-mode wait times.
            The DataFrame contains all original fields of the input DataFrame
            along with the field:
                [timeWaitDrive] - auto mode wait time in minutes """

        # load the mgra based input file
        fn = "mgra13_based_input" + str(self.properties["year"]) + ".csv"

        mgra = pd.read_csv(os.path.join(self.scenario_path, "input", fn),
                           usecols=["mgra",  # MGRA geography
                                    "PopEmpDenPerMi"],  # density per mi
                           dtype={"mgra": "int16",
                                  "PopEmpDenPerMi": "float32"})

        # add PopEmpDenPerMi field to the input DataFrame
        df = df.merge(mgra, left_on="originMGRA", right_on="mgra")

        # select mean wait time for Taxi/TNC mode trips based on
        # the category the PopEmpDenPerMi value falls in
        # note the first true condition encountered is chosen
        conditions = [
            ((df["tripMode"] == "Non-Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][0])),
            ((df["tripMode"] == "Non-Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][1])),
            ((df["tripMode"] == "Non-Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][2])),
            ((df["tripMode"] == "Non-Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][3])),
            ((df["tripMode"] == "Non-Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][4])),
            ((df["tripMode"] == "Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][0])),
            ((df["tripMode"] == "Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][1])),
            ((df["tripMode"] == "Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][2])),
            ((df["tripMode"] == "Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][3])),
            ((df["tripMode"] == "Pooled TNC") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][4])),
            ((df["tripMode"] == "Taxi") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][0])),
            ((df["tripMode"] == "Taxi") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][1])),
            ((df["tripMode"] == "Taxi") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][2])),
            ((df["tripMode"] == "Taxi") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][3])),
            ((df["tripMode"] == "Taxi") & (
                    df["PopEmpDenPerMi"] < self.properties["waitTimePopEmpDenPerMi"][4]))
        ]

        choices = [
            *self.properties["waitTimeNonPooledTNC"],
            *self.properties["waitTimePooledTNC"],
            *self.properties["waitTimeTaxi"],
        ]

        df["timeWaitDrive"] = pd.Series(
            np.select(conditions, choices, default=0),
            dtype="float32")

        # remove mgra, PopEmpDenPerMi from the input DataFrame
        df.drop(columns=["mgra", "PopEmpDenPerMi"], inplace=True)

        # return input DataFrame with appended auto wait time column
        return df

    def walk_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [tripMode], [originMGRA], [destinationMGRA]) and returns
        the associated micro-mobility, micro-transit, and walk mode skims for
        time, distance, and fare cost. Non-micro-mobility/micro-transit/walk
        modes have all skims set to 0.

            Args:
                df: Input Pandas DataFrame containing the fields
                    [tripID] - unique identifier of a trip
                    [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                    [originMGRA] - trip origin MGRA (1-23002)
                    [destinationMGRA] - trip destination MGRA (1-23002)

            Returns:
                A Pandas DataFrame containing all associated micro-mobility,
                micro-transit, and walk mode skims for time, distance, and
                fare cost. The DataFrame contains all the original fields of
                the input DataFrame along with the fields:
                    [timeWalk] - time in minutes for walk mode
                    [distanceWalk] - distance in miles for walk mode
                    [timeMM] - time in minutes for micro-mobility mode
                    [distanceMM] - distance in miles for micro-mobility mode
                    [costFareMM] - fare cost in dollars for micro-mobility mode
                    [timeMT] - time in minutes for micro-transit mode
                    [distanceMT] - distance in miles for micro-transit mode
                    [costFareMT] - fare cost in dollars for micro-transit mode
            """
        # load the mgra-mgra walk/micro-mobility/micro-transit skim file
        skims = pd.read_csv(os.path.join(self.scenario_path,
                                         "output",
                                         "microMgraEquivMinutes.csv"),
                            usecols=["i",  # origin MGRA geography
                                     "j",  # destination MGRA geography
                                     "walkTime",  # walk time in minutes
                                     "dist",  # distance in miles
                                     "mmTime",  # micro-mobility time in minutes
                                     "mmCost",  # micro-mobility cost in dollars
                                     "mtTime",  # micro-transit time in minutes
                                     "mtCost"],    # micro-transit cost in dollars
                            dtype={"i": "int16",
                                   "j": "int16",
                                   "walkTime": "float32",
                                   "dist": "float32",
                                   "mmTime": "float32",
                                   "mmCost": "float32",
                                   "mtTime": "float32",
                                   "mtCost": "float32"})

        # merge the skims with the input DataFrame walk/mm/mt mode records
        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        records = df.loc[(df["tripMode"].isin(["Micro-Mobility",
                                               "Micro-Transit",
                                               "Walk"]))].copy()

        records.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        records = records.merge(
            right=skims,
            how="inner",
            left_on=["originMGRA", "destinationMGRA"],
            right_on=["i", "j"]
        )

        # set skims based on mode
        records["timeWalk"] = np.where(records["tripMode"] == "Walk",
                                       records["walkTime"],
                                       0)

        records["distanceWalk"] = np.where(records["tripMode"] == "Walk",
                                           records["dist"],
                                           0)

        records["timeMM"] = np.where(records["tripMode"] == "Micro-Mobility",
                                     records["mmTime"],
                                     0)

        records["distanceMM"] = np.where(records["tripMode"] == "Micro-Mobility",
                                         records["dist"],
                                         0)

        records["costFareMM"] = np.where(records["tripMode"] == "Micro-Mobility",
                                         records["mmCost"],
                                         0)

        records["timeMT"] = np.where(records["tripMode"] == "Micro-Transit",
                                     records["mtTime"],
                                     0)

        records["distanceMT"] = np.where(records["tripMode"] == "Micro-Transit",
                                         records["dist"],
                                         0)

        records["costFareMT"] = np.where(records["tripMode"] == "Micro-Transit",
                                         records["mtCost"],
                                         0)

        # merge result set DataFrame back into initial trip list
        records = records[["tripID",
                           "timeWalk",
                           "distanceWalk",
                           "timeMM",
                           "distanceMM",
                           "costFareMM",
                           "timeMT",
                           "distanceMT",
                           "costFareMT"]]

        skim_cols = ["timeWalk",
                     "distanceWalk",
                     "timeMM",
                     "distanceMM",
                     "costFareMM",
                     "timeMT",
                     "distanceMT",
                     "costFareMT"]

        # if there are no mm/mt/walk trip skim records
        if records.empty:
            # append skim columns to input DataFrame
            # set to 0 as a missing skim means no transit trip
            for col in skim_cols:
                df[col] = 0
        else:
            # merge result set DataFrame back into initial trip list
            df = df.merge(records, on="tripID", how="left")

            # set missing skim records to 0 as a missing skim means no transit trip
            df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df

    def walk_transit_skims(self, df: pd.DataFrame) -> pd.DataFrame:
        """ Takes an input Pandas DataFrame containing the ABM fields
        ([tripID], [inbound], [tripMode], [originMGRA], [destinationMGRA],
        [boardingTAP], [alightingTAP]) and the optional fields
        ([microMobilityTransitAccess], [microMobilityTransitEgress]) and
        returns the associated micro-mobility, micro-transit, and walk mode
        skims for transit access/egress for time, distance, and fare cost.
        Trips without micro-mobility, micro-transit, or walk mode
        access/egress to transit have all skims set to 0.

        Args:
            df: Input Pandas DataFrame containing the fields
                [tripID] - unique identifier of a trip
                [inbound] - boolean indicator of inbound/outbound direction
                [tripMode] - ABM trip modes (Drive Alone, Shared Ride 2, etc...)
                [originMGRA] - trip origin MGRA (1-23002)
                [destinationMGRA] - trip destination MGRA (1-23002)
                [boardingTAP] - transit boarding TAP
                [alightingTAP] - transit alighting TAP
                [microMobilityTransitAccess] - optional field indicating if
                    micro-mobility, micro-transit, or walk mode was used to
                    access transit
                [microMobilityTransitEgress] - optional field indicating if
                    micro-mobility, micro-transit, or walk mode was used to
                    egress transit

        Returns:
            A Pandas DataFrame containing all associated micro-mobility,
            micro-transit, and walk to transit mode access/egress skims for
            time, distance, and fare cost. The DataFrame contains all the
            original fields of the input DataFrame along with the fields:
                [timeTransitWalkAccessEgress] - time in minutes for walk
                    portion of transit access/egress
                [distanceTransitWalkAccessEgress] - distance in miles for
                    walk portion of transit access/egress
                [timeTransitMMAccessEgress] - time in minutes for
                    micro-mobility portion of transit access/egress
                [distanceTransitMMAccessEgress] - distance in miles for
                    micro-mobility portion of transit access/egress
                [costFareTransitMMAccessEgress] - fare cost in dollars for
                    micro-mobility portion of transit access/egress
                [timeTransitMTAccessEgress] - time in minutes for
                    micro-transit portion of transit access/egress
                [distanceTransitMTAccessEgress] - distance in miles for
                    micro-transit portion of transit access/egress
                [costFareTransitMTAccessEgress] - fare cost in dollars for
                    micro-transit portion of transit access/egress """

        # load the micro-mobility, micro-transit, and walk from MGRA-TAP
        # skim file containing time, distance, and cost
        skims = pd.read_csv(os.path.join(self.scenario_path,
                                         "output",
                                         "microMgraTapEquivMinutes.csv"),
                            usecols=["mgra",  # origin MGRA geography
                                     "tap",  # destination TAP
                                     "walkTime",  # walk time in minutes
                                     "dist",  # distance in miles
                                     "mmTime",  # micro-mobility time in minutes
                                     "mmCost",  # micro-mobility cost in dollars
                                     "mtTime",  # micro-transit time in minutes
                                     "mtCost"],  # micro-transit cost in dollars
                            dtype={"mgra": "int16",
                                   "tap": "int16",
                                   "walkTime": "float32",
                                   "dist": "float32",
                                   "mmTime": "float32",
                                   "mmCost": "float32",
                                   "mtTime": "float32",
                                   "mtCost": "float32"})

        # filter input DataFrame to records that have access/egress
        # micro-mobility, micro-transit, or walk segments
        records = df.loc[(df["tripMode"].isin(
            ["Walk to Transit - Local Bus",
             "Walk to Transit - Premium Transit",
             "Walk to Transit - Local Bus and Premium Transit",
             "Park and Ride to Transit - Local Bus",
             "Park and Ride to Transit - Premium Transit",
             "Park and Ride to Transit - Local Bus and Premium Transit",
             "Kiss and Ride to Transit - Local Bus",
             "Kiss and Ride to Transit - Premium Transit",
             "Kiss and Ride to Transit - Local Bus and Premium Transit",
             "TNC to Transit - Local Bus",
             "TNC to Transit - Premium Transit",
             "TNC to Transit - Local Bus and Premium Transit"]))].copy()

        # ABM Joint sub-model has multiple records per tripID
        # per person on trip records are identical otherwise
        records.drop_duplicates(subset="tripID", inplace=True, ignore_index=True)

        # merge micro-mobility, micro-transit, and walk access/egress skims
        # for both the access and egress portions
        records = records.merge(
            right=skims,
            how="left",
            left_on=["originMGRA", "boardingTAP"],
            right_on=["mgra", "tap"]
        )

        records = records.merge(
            right=skims,
            how="left",
            left_on=["destinationMGRA", "alightingTAP"],
            right_on=["mgra", "tap"],
            suffixes=["Access", "Egress"]
        )

        # conditionally set access/egress skim fields to 0 based on trip mode
        # and inbound direction of trip, note that walk to transit uses both
        records.loc[(records["tripMode"].isin(
            ["Park and Ride to Transit - Local Bus",
             "Park and Ride to Transit - Premium Transit",
             "Park and Ride to Transit - Local Bus and Premium Transit",
             "Kiss and Ride to Transit - Local Bus",
             "Kiss and Ride to Transit - Premium Transit",
             "Kiss and Ride to Transit - Local Bus and Premium Transit",
             "TNC to Transit - Local Bus",
             "TNC to Transit - Premium Transit",
             "TNC to Transit - Local Bus and Premium Transit"])) &
                    (records["inbound"] == False),
                    ["walkTimeAccess",
                     "distAccess",
                     "mmTimeAccess",
                     "mmCostAccess",
                     "mtTimeAccess",
                     "mtCostAccess"]] = 0

        records.loc[(records["tripMode"].isin([
            "Park and Ride to Transit - Local Bus",
            "Park and Ride to Transit - Premium Transit",
            "Park and Ride to Transit - Local Bus and Premium Transit",
            "Kiss and Ride to Transit - Local Bus",
            "Kiss and Ride to Transit - Premium Transit",
            "Kiss and Ride to Transit - Local Bus and Premium Transit",
            "TNC to Transit - Local Bus",
            "TNC to Transit - Premium Transit",
            "TNC to Transit - Local Bus and Premium Transit"])) &
                    (records["inbound"] == True),
                    ["walkTimeEgress",
                     "distEgress",
                     "mmTimeEgress",
                     "mmCostEgress",
                     "mtTimeEgress",
                     "mtCostEgress"]] = 0

        # if optional micro-mobility access/egress fields are not present
        # assume Walk modes only for access/egress skims
        if not {"microMobilityTransitAccess", "microMobilityTransitEgress"}.issubset(df.columns):
            records["timeTransitWalkAccessEgress"] = records["walkTimeAccess"] + records["walkTimeEgress"]
            records["distanceTransitWalkAccessEgress"] = records["distAccess"] + records["distEgress"]
            records["timeTransitMMAccessEgress"] = 0
            records["distanceTransitMMAccessEgress"] = 0
            records["costFareTransitMMAccessEgress"] = 0
            records["timeTransitMTAccessEgress"] = 0
            records["distanceTransitMTAccessEgress"] = 0
            records["costFareTransitMTAccessEgress"] = 0
        else:  # if optional micro-mobility access/egress fields are present
            # set skim fields based on micro-mobility access/egress fields
            records["timeTransitWalkAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Walk",
                         records["walkTimeAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Walk",
                         records["walkTimeEgress"], 0)

            records["distanceTransitWalkAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Walk",
                         records["distAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Walk",
                         records["distEgress"], 0)

            records["timeTransitMMAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Mobility",
                         records["mmTimeAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Mobility",
                         records["mmTimeEgress"], 0)

            records["distanceTransitMMAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Mobility",
                         records["distAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Mobility",
                         records["distEgress"], 0)

            records["costFareTransitMMAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Mobility",
                         records["mmCostAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Mobility",
                         records["mmCostEgress"], 0)

            records["timeTransitMTAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Transit",
                         records["mtTimeAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Transit",
                         records["mtTimeEgress"], 0)

            records["distanceTransitMTAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Transit",
                         records["distAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Transit",
                         records["distEgress"], 0)

            records["costFareTransitMTAccessEgress"] = \
                np.where(records["microMobilityTransitAccess"] == "Micro-Transit",
                         records["mtCostAccess"], 0) + \
                np.where(records["microMobilityTransitEgress"] == "Micro-Transit",
                         records["mtCostEgress"], 0)

        # merge result set DataFrame back into initial trip list
        records = records[["tripID",
                           "timeTransitWalkAccessEgress",
                           "distanceTransitWalkAccessEgress",
                           "timeTransitMMAccessEgress",
                           "distanceTransitMMAccessEgress",
                           "costFareTransitMMAccessEgress",
                           "timeTransitMTAccessEgress",
                           "distanceTransitMTAccessEgress",
                           "costFareTransitMTAccessEgress"]]

        # access/egress transit trip
        skim_cols = ["timeTransitWalkAccessEgress",
                     "distanceTransitWalkAccessEgress",
                     "timeTransitMMAccessEgress",
                     "distanceTransitMMAccessEgress",
                     "costFareTransitMMAccessEgress",
                     "timeTransitMTAccessEgress",
                     "distanceTransitMTAccessEgress",
                     "costFareTransitMTAccessEgress"]

        # if there are no mm/mt/walk access/egress skim records
        if records.empty:
            # append skim columns to input DataFrame
            # set to 0 as a missing skim means no transit trip
            for col in skim_cols:
                df[col] = 0
        else:
            # merge result set DataFrame back into initial trip list
            df = df.merge(records, on="tripID", how="left")

            # set missing skim records to 0 as a missing skim means no transit trip
            df[skim_cols] = df[skim_cols].fillna(0)

        # return input DataFrame with appended skim columns
        return df
