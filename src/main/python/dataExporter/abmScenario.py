# -*- coding: utf-8 -*-
""" ABM Scenario Output Module.

This module contains classes holding all information and utilities relating to
a completed SANDAG Activity-Based Model (ABM) scenario. This module is used to
create data-sets from the output of an ABM model run that are suitable for
reporting purposes.

Notes:
    docstring style guide - http://google.github.io/styleguide/pyguide.html
"""

from datetime import time, timedelta, date, datetime
from functools import lru_cache  # caching decorator for modules
import itertools
import numpy as np
import os
import pandas as pd
import re


class ScenarioData(object):
    """ This is the parent class for all information and utilities relating
    to a completed SANDAG Activity-Based Model (ABM) scenario.

    Args:
        scenario_path: String location of the completed ABM scenario folder

    Methods:
        _map_time_periods: maps ABM half hour periods to ABM five time of
            day periods
        _map_vot_categories: maps continuous value of time (vot) values to
            vot categories ("Low", "Medium", "High") defined in the ABM
            scenario properties file (see vot_categories property)

    Properties:
        mgra_xref: Pandas DataFrame geography cross-reference of MGRAs to
            TAZs and LUZs
        pnr_taps: Pandas DataFrame of transit TAP park and ride data placed
            here until it can be properly incorporated into the EMME data
            exporter process
        properties: Dictionary of ABM properties file token values
            (conf/sandag_abm.properties)
        time_periods: Dictionary of ABM model time resolution periods


    Subclasses:
        SyntheticPopulation: Holds all synthetic population data for a
            completed ABM scenario model run
        TourLists: Holds all tour list data for a completed ABM scenario
            model run
        TripLists: Holds all trip list data for a completed ABM scenario
            model run """

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
        fn = "mgra15_based_input" + str(self.properties["year"]) + ".csv"

        mgra = pd.read_csv(os.path.join(self.scenario_path, "input", fn),
                           usecols=["mgra",  # MGRA geography
                                    "taz",  # TAZ geography
                                    "luz_id"],
                           dtype={"mgra": "int16",
                                  "taz": "int16",
                                  "luz_id": "int16"})  # LUZ geography

        # genericize column names
        mgra.rename(columns={"mgra": "MGRA",
                             "taz": "TAZ",
                             "luz_id": "LUZ"},
                    inplace=True)

        return mgra

    @property
    @lru_cache(maxsize=1)
    def pnr_taps(self) -> pd.DataFrame:
        """ Create the transit TAP park and ride lot data-set.

        Read in and combine the transit TAP parking lot type information and
        parking lot vehicles by time of day information.

        Returns:
            A Pandas DataFrame of the transit TAP park and ride lot data-set """
        # load parking lot type data-set
        lots = pd.read_fwf(
            os.path.join(self.scenario_path, "input", "tap.ptype"),
            names=["TAP",
                   "lotID",
                   "parkingType",
                   "lotTAZ",
                   "capacity",
                   "distance",
                   "mode"],
            header=None,
            widths=[5, 6, 6, 5, 5, 5, 3])

        # replicate parking lot data by ABM five time of day
        five_tod = pd.DataFrame(
            {"key": [0]*5,
             "timeFiveTod": ["EA", "AM", "MD", "PM", "EV"]})

        lots["key"] = 0
        lots = lots.merge(five_tod)

        # load parking lot vehicles by time of day
        vehicles = pd.read_csv(
            os.path.join(self.scenario_path, "output", "PNRByTAP_Vehicles.csv"),
            usecols=["TAP",
                     "EA",
                     "AM",
                     "MD",
                     "PM",
                     "EV"])

        # restructure vehicle data from wide to long by ABM five time of day
        vehicles = pd.melt(
            vehicles,
            id_vars=["TAP"],
            value_vars=["EA", "AM", "MD", "PM", "EV"],
            var_name="timeFiveTod",
            value_name="vehicles")

        # merge parking lot and vehicle data
        lots = lots.merge(vehicles, how="left")

        # set missing vehicle fields to 0
        lots["vehicles"] = lots["vehicles"].fillna(0)

        # convert distance field from feet to miles
        lots["distance"] = lots["distance"] / 5280

        # apply exhaustive field mappings where applicable
        mappings = {
            "timeFiveTod": {"EA": 1,
                            "AM": 2,
                            "MD": 3,
                            "PM": 4,
                            "EV": 5},
            "parkingType": {1: "Formal Parking",
                             2: "Other Parking",
                             3: "Other Light Rail Trolley Parking",
                             4: "Non-formal parking area based on the on-board survey",
                             5: "Non-formal parking area based on the on-board survey"}
        }

        for field in mappings:
            lots[field] = lots[field].map(mappings[field])

        # rename columns to standard/generic ABM naming conventions
        lots.rename(columns={"TAP": "tapID"}, inplace=True)

        return lots[["tapID",
                     "lotID",
                     "lotTAZ",
                     "timeFiveTod",
                     "parkingType",
                     "capacity",
                     "distance",
                     "vehicles"]]

    @property
    @lru_cache(maxsize=1)
    def properties(self) -> dict:
        """ Get the ABM scenario properties from the ABM scenario
        properties file (conf/sandag_abm.properties).

        The return dictionary contains the following ABM scenario properties:
            cvmScaleLight - commercial vehicle model trip scaling factor for
                light vehicles for each ABM five time of day
            cvmScaleMedium - commercial vehicle model trip scaling factor for
                intermediate and medium vehicles for each ABM five time of day
            cvmScaleHeavy - commercial vehicle model trip scaling factor for
                heavy vehicles for each ABM five time of day
            cvmShareLight - commercial vehicle model trip intermediate vehicle
                share factor for light vehicles
            cvmShareMedium - commercial vehicle model trip intermediate vehicle
                share factor for medium vehicles
            cvmShareHeavy - commercial vehicle model trip intermediate vehicle
                share factor for heavy vehicles
            iterations - number of model iteration
            nonPooledTNCPassengers - average number of passengers to assume for
                Non-Pooled TNC mode in models without party size specifications
            pooledTNCPassengers - average number of passengers to assume for
                Pooled TNC mode in models without party size specifications
            sr2Passengers - average number of passengers to assume for Shared
                Ride 2 mode in models without party size specifications
            sr3Passengers - average number of passengers to assume for Shared
                Ride 3+ mode in models without party size specifications
            taxiPassengers - average number of passengers to assume for Taxi
                mode in models without party size specifications
            timePeriodWidthTNC - time period width (in minutes) for custom
                fixed-width time periods used in TNC routing model, note that
                this is not currently restricted to nest within ABM model time
                periods
            sampleRate - sample rate of final iteration
            valueOfTimeLow - upper limit of 'Low' value of time category
            valueOfTimeMedium - upper limit of 'Medium' value of time category
            year - analysis year of the ABM scenario

        Returns:
            A dictionary defining the ABM scenario properties. """

        # create dictionary holding ABM properties file information
        # each property contains a dictionary {line, value} where the line
        # is the string to match in the properties file to
        # return the value of the property
        lookup = {
            "cvmScaleLight": {
                "line": "cvm.scale_light=",
                "type": "list",
                "value": None},
            "cvmScaleMedium": {
                "line": "cvm.scale_medium=",
                "type": "list",
                "value": None},
            "cvmScaleHeavy": {
                "line": "cvm.scale_heavy=",
                "type": "list",
                "value": None},
            "cvmShareLight": {
                "line": "cvm.share.light=",
                "type": "float",
                "value": None},
            "cvmShareMedium": {
                "line": "cvm.share.medium=",
                "type": "float",
                "value": None},
            "cvmShareHeavy": {
                "line": "cvm.share.heavy=",
                "type": "float",
                "value": None},
            "iterations": {
                "line": None,
                "type": "int",
                "value": None},
            "nonPooledTNCPassengers": {
                "line": "TNC.single.passengersPerVehicle=",
                "type": "float",
                "value": None},
            "pooledTNCPassengers": {
                "line": "TNC.shared.passengersPerVehicle=",
                "type": "float",
                "value": None},
            "sr2Passengers": {
                "line": None,
                "type": "int",
                "value": 2},
            "sr3Passengers": {
                "line": None,
                "type": "float",
                "value": 3.34},
            "taxiPassengers": {
                "line": "Taxi.passengersPerVehicle=",
                "type": "float",
                "value": None},
            "timePeriodWidthTNC": {
                "line": "Maas.RoutingModel.minutesPerSimulationPeriod=",
                "type": "int",
                "value": None},
            "sampleRate": {
                "line": "sample_rates=",
                "type": "float",
                "value": None},
            "valueOfTimeLow": {
                "line": "valueOfTime.threshold.low=",
                "type": "float",
                "value": None},
            "valueOfTimeMedium": {
                "line": "valueOfTime.threshold.med=",
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

                # if the properties file contains the matching line
                if item["line"] is not None:
                    match = re.compile(item["line"]).match(line)
                else:
                    match = False

                if match:
                    # if the match is for the sample rate element
                    # then take the portion of the line after the matching string
                    # and split by the comma character
                    if name == "sampleRate":
                        line = line[match.end():].split(",")

                        # set number of iterations to number of sample rates
                        # that are specified
                        lookup["iterations"]["value"] = len(line)

                        # if the split line contains a single element
                        # return that element otherwise return the final element
                        if len(line) == 1:
                            value = line[0]
                        else:
                            value = line[-1]
                    # if the match is for a cvm scale element then take the
                    # portion of the line after the matching string and split
                    # by the comma character into a list of floats
                    elif "cvmScale" in name:
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

        return results

    @property
    def time_periods(self) -> dict:
        """ Dictionary of ABM model time resolution periods with start and
        end times where the start time is inclusive and the end time is
        exclusive. Dictionary is of the form:
            {"period": "startTime": "endTime":}

        Returns:
            A Dictionary of the ABM model time resolution periods.
        """
        periods = {
            "abmHalfHour": [
                {"period": 1,
                 "startTime": time(3, 0),
                 "endTime": time(5, 0)},
                {"period": 2,
                 "startTime": time(5, 0),
                 "endTime": time(5, 30)},
                {"period": 3,
                 "startTime": time(5, 30),
                 "endTime": time(6, 0)},
                {"period": 4,
                 "startTime": time(6, 0),
                 "endTime": time(6, 30)},
                {"period": 5,
                 "startTime": time(6, 30),
                 "endTime": time(7, 0)},
                {"period": 6,
                 "startTime": time(7, 0),
                 "endTime": time(7, 30)},
                {"period": 7,
                 "startTime": time(7, 30),
                 "endTime": time(8, 0)},
                {"period": 8,
                 "startTime": time(8, 0),
                 "endTime": time(8, 30)},
                {"period": 9,
                 "startTime": time(8, 30),
                 "endTime": time(9, 0)},
                {"period": 10,
                 "startTime": time(9, 0),
                 "endTime": time(9, 30)},
                {"period": 11,
                 "startTime": time(9, 30),
                 "endTime": time(10, 0)},
                {"period": 12,
                 "startTime": time(10, 0),
                 "endTime": time(10, 30)},
                {"period": 13,
                 "startTime": time(10, 30),
                 "endTime": time(11, 0)},
                {"period": 14,
                 "startTime": time(11, 0),
                 "endTime": time(11, 30)},
                {"period": 15,
                 "startTime": time(11, 30),
                 "endTime": time(12, 0)},
                {"period": 16,
                 "startTime": time(12, 0),
                 "endTime": time(12, 30)},
                {"period": 17,
                 "startTime": time(12, 30),
                 "endTime": time(13, 0)},
                {"period": 18,
                 "startTime": time(13, 0),
                 "endTime": time(13, 30)},
                {"period": 19,
                 "startTime": time(13, 30),
                 "endTime": time(14, 0)},
                {"period": 20,
                 "startTime": time(14, 0),
                 "endTime": time(14, 30)},
                {"period": 21,
                 "startTime": time(14, 30),
                 "endTime": time(15, 0)},
                {"period": 22,
                 "startTime": time(15, 0),
                 "endTime": time(15, 30)},
                {"period": 23,
                 "startTime": time(15, 30),
                 "endTime": time(16, 0)},
                {"period": 24,
                 "startTime": time(16, 0),
                 "endTime": time(16, 30)},
                {"period": 25,
                 "startTime": time(16, 30),
                 "endTime": time(17, 0)},
                {"period": 26,
                 "startTime": time(17, 0),
                 "endTime": time(17, 30)},
                {"period": 27,
                 "startTime": time(17, 30),
                 "endTime": time(18, 0)},
                {"period": 28,
                 "startTime": time(18, 0),
                 "endTime": time(18, 30)},
                {"period": 29,
                 "startTime": time(18, 30),
                 "endTime": time(19, 0)},
                {"period": 30,
                 "startTime": time(19, 0),
                 "endTime": time(19, 30)},
                {"period": 31,
                 "startTime": time(19, 30),
                 "endTime": time(20, 0)},
                {"period": 32,
                 "startTime": time(20, 0),
                 "endTime": time(20, 30)},
                {"period": 33,
                 "startTime": time(20, 30),
                 "endTime": time(21, 0)},
                {"period": 34,
                 "startTime": time(21, 0),
                 "endTime": time(21, 30)},
                {"period": 35,
                 "startTime": time(21, 30),
                 "endTime": time(22, 0)},
                {"period": 36,
                 "startTime": time(22, 0),
                 "endTime": time(22, 30)},
                {"period": 37,
                 "startTime": time(22, 30),
                 "endTime": time(23, 0)},
                {"period": 38,
                 "startTime": time(23, 0),
                 "endTime": time(23, 30)},
                {"period": 39,
                 "startTime": time(23, 30),
                 "endTime": time.max},
                {"period": 40,
                 "startTime": time.min,
                 "endTime": time(3, 0)}
            ],
            "abm5Tod": [
                {"period": 1,
                 "startTime": time(3, 0),
                 "endTime": time(6, 0)},
                {"period": 2,
                 "startTime": time(6, 0),
                 "endTime": time(9, 0)},
                {"period": 3,
                 "startTime": time(9, 0, 0),
                 "endTime": time(15, 30)},
                {"period": 4,
                 "startTime": time(15, 30),
                 "endTime": time(19, 0)},
                {"period": 5,
                 "startTime": time(19, 0),
                 "endTime": time.max},
                {"period": 5,
                 "startTime": time.min,
                 "endTime": time(3, 0)}
            ]
        }

        return periods

    @staticmethod
    def _map_time_periods(abm_half_hour: pd.Series) -> pd.Series:
        """ Map ABM half hour time periods to ABM five time of day periods

        Returns:
            A Pandas Series of ABM five time of day periods """

        conditions = [abm_half_hour.between(1, 6),
                      abm_half_hour.between(7, 12),
                      abm_half_hour.between(13, 25),
                      abm_half_hour.between(26, 32),
                      abm_half_hour.between(33, 48)]

        choices = [1, 2, 3, 4, 5]

        abm_5_tod = np.select(conditions, choices, default=np.NaN)

        return pd.Series(abm_5_tod).astype("float")

    def _map_vot_categories(self, vot: pd.Series) -> pd.Series:
        """ Map Pandas Series of continuous ABM value of time (vot) values to
        vot categories ("Low", "Medium", "High") defined in the ABM scenario
        properties file.

        Returns:
            A Pandas Series of value of time categories. """

        # get vot thresholds
        low = self.properties["valueOfTimeLow"]
        med = self.properties["valueOfTimeMedium"]

        # map continuous values of time to categories
        conditions = [vot < low,
                      (low <= vot) & (vot < med),
                      vot >= med]

        choices = ["Low", "Medium", "High"]

        vot_category = np.select(conditions, choices, default=np.NaN)

        return pd.Series(vot_category).astype("category")


class LandUse(ScenarioData):
    """ A subclass of the ScenarioData class. Holds all land use
    data for a completed ABM scenario model run. As of now, this includes only
    the MGRA-based input file. This is held as a class property.

    Properties:
        mgra_input: MGRA-based input file
    """
    @property
    @lru_cache(maxsize=1)
    def mgra_input(self) -> pd.DataFrame:
        """ Create the MGRA-based input file data-set. """
        # load the MGRA-based input file
        fn = "mgra15_based_input" + str(self.properties["year"]) + ".csv"

        mgra = pd.read_csv(
            os.path.join(self.scenario_path, "input", fn),
            usecols=["mgra",
                     "taz",
                     "hs",
                     "hs_sf",
                     "hs_mf",
                     "hs_mh",
                     "hh",
                     "hh_sf",
                     "hh_mf",
                     "hh_mh",
                     "gq_civ",
                     "gq_mil",
                     "i1",
                     "i2",
                     "i3",
                     "i4",
                     "i5",
                     "i6",
                     "i7",
                     "i8",
                     "i9",
                     "i10",
                     "hhs",
                     "pop",
                     "hhp",
                     "emp_ag",
                     "emp_const_non_bldg_prod",
                     "emp_const_non_bldg_office",
                     "emp_utilities_prod",
                     "emp_utilities_office",
                     "emp_const_bldg_prod",
                     "emp_const_bldg_office",
                     "emp_mfg_prod",
                     "emp_mfg_office",
                     "emp_whsle_whs",
                     "emp_trans",
                     "emp_retail",
                     "emp_prof_bus_svcs",
                     "emp_prof_bus_svcs_bldg_maint",
                     "emp_pvt_ed_k12",
                     "emp_pvt_ed_post_k12_oth",
                     "emp_health",
                     "emp_personal_svcs_office",
                     "emp_amusement",
                     "emp_hotel",
                     "emp_restaurant_bar",
                     "emp_personal_svcs_retail",
                     "emp_religious",
                     "emp_pvt_hh",
                     "emp_state_local_gov_ent",
                     "emp_fed_non_mil",
                     "emp_fed_mil",
                     "emp_state_local_gov_blue",
                     "emp_state_local_gov_white",
                     "emp_public_ed",
                     "emp_own_occ_dwell_mgmt",
                     "emp_fed_gov_accts",
                     "emp_st_lcl_gov_accts",
                     "emp_cap_accts",
                     "emp_total",
                     "enrollgradekto8",
                     "enrollgrade9to12",
                     "collegeenroll",
                     "othercollegeenroll",
                     "adultschenrl",
                     "ech_dist",
                     "hch_dist",
                     "pseudomsa",
                     "parkarea",
                     "hstallsoth",
                     "hstallssam",
                     "hparkcost",
                     "numfreehrs",
                     "dstallsoth",
                     "dstallssam",
                     "dparkcost",
                     "mstallsoth",
                     "mstallssam",
                     "mparkcost",
                     "zip09",
                     "parkactive",
                     "openspaceparkpreserve",
                     "beachactive",
                     "hotelroomtotal",
                     "truckregiontype",
                     "district27",
                     "milestocoast",
                     "acres",
                     "effective_acres",
                     "land_acres",
                     "MicroAccessTime",
                     "remoteAVParking",
                     "refueling_stations",
                     "totint",
                     "duden",
                     "empden",
                     "popden",
                     "retempden",
                     "totintbin",
                     "empdenbin",
                     "dudenbin",
                     "PopEmpDenPerMi"])

        return mgra


class SyntheticPopulation(ScenarioData):
    """ A subclass of the ScenarioData class. Holds all synthetic population
    data for a completed ABM scenario model run. This includes the input
    synthetic persons and households sampled in the ABM model run as well as
    model results pertaining to person and household attributes (e.g. work
    location, parking reimbursement, etc...). The synthetic population persons
    and households are held as class properties and include:
        Synthetic Households
        Synthetic Persons

    Properties:
        households: Synthetic households sampled
        persons:  Synthetic persons sampled
    """
    @property
    @lru_cache(maxsize=1)
    def households(self) -> pd.DataFrame:
        """ Create the synthetic households data-set.

        Read in the input synthetic household list and the sampled synthetic
        household list, combine the lists taking only sampled households,
        map field values, and genericize field names.

        Returns:
            A Pandas DataFrame of the synthetic households """
        # load input synthetic household list into Pandas DataFrame
        input_households = pd.read_csv(
            os.path.join(self.scenario_path, "input", "households.csv"),
            usecols=["hhid",
                     "taz",
                     "mgra",
                     "hinccat1",
                     "hinc",
                     "hworkers",
                     "persons",
                     "bldgsz",
                     "unittype",
                     "poverty"],
            dtype={"hhid": "int32",
                   "taz": "int16",
                   "mgra": "int16",
                   "hinccat1": "int8",
                   "hinc": "int32",
                   "hworkers": "int8",
                   "persons": "int8",
                   "bldgsz": "int8",
                   "unittype": "int8",
                   "poverty": "float32"})

        # load output sampled synthetic household list
        fn = "householdData_" + str(self.properties["iterations"]) + ".csv"
        output_households = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn),
            usecols=["hh_id",
                     "autos",
                     "HVs",
                     "AVs",
                     "transponder"],
            dtype={"hh_id": "int32",
                   "autos": "int8",
                   "HVs": "int8",
                   "AVs": "int8",
                   "transponder": "bool"})

        # merge output sampled households with input sampled households
        # keep only households present in the sampled households
        households = output_households.merge(
            input_households,
            how="inner",
            left_on="hh_id",
            right_on="hhid"
        )

        # apply exhaustive field mappings where applicable
        mappings = {
            "hinccat1": {1: "Less than 30k",
                         2: "30k-60k",
                         3: "60k-100k",
                         4: "100k-150k",
                         5: "150k+"},
            "bldgsz": {1: "Mobile Home or Trailer",
                       2: "Single Family Home - Detached",
                       3: "Single Family Home - Attached",
                       8: "Multi-Family Home",
                       9: "Other (includes Group Quarters)"},
            "unittype": {0: "Non-Group Quarters",
                         1: "Group Quarters"}
        }

        for field in mappings:
            households[field] = households[field].map(mappings[field]).astype("category")

        # rename columns to standard/generic ABM naming conventions
        households.rename(columns={"hh_id": "hhId",
                                   "HVs": "autosHumanVehicles",
                                   "AVs": "autosAutonomousVehicles",
                                   "transponder": "transponderAvailable",
                                   "mgra": "homeMGRA",
                                   "taz": "homeTAZ",
                                   "hinccat1": "hhIncomeCategory",
                                   "hinc": "hhIncome",
                                   "hworkers": "hhWorkers",
                                   "persons": "hhPersons",
                                   "bldgsz": "buildingCategory",
                                   "unittype": "unitType"},
                          inplace=True)

        return households[["hhId",
                           "autos",
                           "autosHumanVehicles",
                           "autosAutonomousVehicles",
                           "transponderAvailable",
                           "homeMGRA",
                           "homeTAZ",
                           "hhIncomeCategory",
                           "hhIncome",
                           "hhWorkers",
                           "hhPersons",
                           "buildingCategory",
                           "unitType",
                           "poverty"]]

    @property
    @lru_cache(maxsize=1)
    def persons(self) -> pd.DataFrame:
        """ Create the synthetic persons data-set.

        Read in the input synthetic person list and the sampled synthetic
        person list, combine the lists taking only sampled persons,
        map field values, and genericize field names.

        Returns:
            A Pandas DataFrame of the synthetic persons """
        # load input synthetic person list into Pandas DataFrame
        input_persons = pd.read_csv(
            os.path.join(self.scenario_path, "input", "persons.csv"),
            usecols=["hhid",
                     "perid",
                     "pnum",
                     "age",
                     "sex",
                     "miltary",
                     "pemploy",
                     "pstudent",
                     "ptype",
                     "educ",
                     "grade",
                     "weeks",
                     "hours",
                     "rac1p",
                     "hisp"],
            dtype={"hhid": "int32",
                   "perid": "int32",
                   "pnum": "int8",
                   "age": "int8",
                   "sex": "int8",
                   "miltary": "int8",
                   "pemploy": "int8",
                   "pstudent": "int8",
                   "ptype": "int8",
                   "educ": "int8",
                   "grade": "int8",
                   "weeks": "int8",
                   "hours": "int8",
                   "rac1p": "int8",
                   "hisp": "int8"})

        # load output sampled synthetic person list
        fn_person_data = "personData_" + str(self.properties["iterations"]) + ".csv"
        output_persons = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn_person_data),
            usecols=["person_id",
                     "activity_pattern",
                     "fp_choice",
                     "reimb_pct",
                     "tele_choice"],
            dtype={"person_id": "int32",
                   "activity_pattern": "string",
                   "fp_choice": "int8",
                   "reimb_pct": "float32",
                   "tele_choice": "int8"})

        # load work-school location model results
        fn_ws_loc_results = "wsLocResults_" + str(self.properties["iterations"]) + ".csv"
        ws_loc_results = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn_ws_loc_results),
            usecols=["PersonID",
                     "HomeMGRA",
                     "WorkSegment",
                     "SchoolSegment",
                     "WorkLocation",
                     "SchoolLocation"],
            dtype={"PersonID": "int32",
                   "HomeMGRA": "int16",
                   "WorkSegment": "int32",
                   "SchoolSegment": "int32",
                   "WorkLocation": "int32",
                   "SchoolLocation": "int32"})

        # merge output sampled persons with input sampled persons
        # keep only persons present in the sampled persons
        persons = output_persons.merge(
            input_persons,
            how="inner",
            left_on="person_id",
            right_on="perid"
        )

        # merge in work-school location model results
        persons = persons.merge(
            ws_loc_results,
            how="inner",
            left_on="person_id",
            right_on="PersonID"
        )

        # if person works at home set work location to home MGRA
        # if person is home-school set school location to home MGRA
        persons["WorkLocation"] = np.where(
            persons["WorkSegment"] == 99999,
            persons["HomeMGRA"],
            persons["WorkLocation"])

        persons["SchoolLocation"] = np.where(
            persons["SchoolSegment"] == 88888,
            persons["HomeMGRA"],
            persons["SchoolLocation"])

        # apply exhaustive field mappings where applicable
        mappings = {
            "sex": {1: "Male",
                    2: "Female"},
            "miltary": {0: "Not Active Military",
                        1: "Active Military"},
            "pemploy": {1: "Employed Full-Time",
                        2: "Employed Part-Time",
                        3: "Unemployed or Not in Labor Force",
                        4: "Less than 16 Years Old"},
            "pstudent": {1: "Pre K-12",
                         2: "College Undergrad+Grad and Prof. School",
                         3: "Not Attending School"},
            "ptype": {1: "Full-Time Worker",
                      2: "Part-Time Worker",
                      3: "College Student",
                      4: "Non-Working Adult",
                      5: "Non-Working Senior",
                      6: "Driving Age Student",
                      7: "Non-Driving Age Student",
                      8: "Pre K or Child too Young for School"},
            "educ": {1: "Not a High School Graduate",
                     9: "High School Graduate or Associates Degree",
                     13: "Bachelors Degree or Higher"},
            "grade": {0: "Preschool or Not Attending School",
                      2: "Kindergarten - Grade 8",
                      5: "Grade 9 to Grade 12",
                      6: "College Undergraduate or Higher"},
            "weeks": {1: "27 or More Weeks Worked per Year",
                      5: "Less than 27 Weeks Worked per Year"},
            "hours": {0: "Less than 35 Hours Worked or Not Working",
                      35: "35 or More Hours Worked"},
            "rac1p": {1: "White Alone",
                      2: "Black or African American Alone",
                      3: "American Indian Alone",
                      4: "Alaska Native Alone",
                      5: "American Indian and Alaska Native Tribes specified; or American Indian or Alaska Native not specified and no other races",
                      6: "Asian Alone",
                      7: "Native Hawaiian and Other Pacific Islander Alone",
                      8: "Some Other Race Alone",
                      9: "Two or More Major Race Groups"},
            "hisp": {1: "Non-Hispanic",
                     2: "Hispanic"},
            "activity_pattern": {"H": "Home",
                                 "M": "Mandatory",
                                 "N": "Non-Mandatory"},
            "fp_choice": {1: "Has Free Parking",
                          2: "Employer Pays for Parking",
                          3: "Employer Reimburses for Parking"},
            "tele_choice": {0: "No telecommute",
                            1: "One Day a Week",
                            2: "Two-Three Days a Week",
                            3: "Four or More Days a Week",
                            9: "Telecommuter Only"},
            "WorkSegment": {0: "Management Business Science and Labor",
                            1: "Services Labor",
                            2: "Sales and Office Labor",
                            3: "Natural Resources Construction and Maintenance Labor",
                            4: "Production Transportation and Material Moving Labor",
                            5: "Military Labor",
                            99999: "Work from Home"},
            "SchoolSegment": {**{88888: "Home Schooled"},
                              **{key: value for (key, value) in zip(list(range(0, 57)),
                                                                    ["Unknown"] * 57)}
                              },
            "WorkLocation": {key: value for (key, value) in zip(list(range(1, 23003)),
                                                                list(range(1, 23003)))},
            "SchoolLocation": {key: value for (key, value) in zip(list(range(1, 23003)),
                                                                  list(range(1, 23003)))}
        }

        for field in mappings:
            if field in ["WorkLocation", "SchoolLocation"]:
                persons[field] = persons[field].map(mappings[field]).astype("float32")
            else:
                persons[field] = persons[field].map(mappings[field]).astype("category")

        # if employer does not reimburse for parking
        # set parking reimbursement percentage to missing
        persons.loc[persons["fp_choice"] != "Employer Reimburses for Parking", "reimb_pct"] = np.nan
        persons.loc[persons["fp_choice"].isna(), "reimb_pct"] = np.nan

        # rename columns to standard/generic ABM naming conventions
        persons.rename(columns={"perid": "personId",
                                "hhid": "hhId",
                                "pnum": "personNumber",
                                "miltary": "militaryStatus",
                                "pemploy": "employmentStatus",
                                "pstudent": "studentStatus",
                                "ptype": "abmPersonType",
                                "educ": "education",
                                "rac1p": "race",
                                "hisp": "hispanic",
                                "activity_pattern": "abmActivityPattern",
                                "fp_choice": "freeParkingChoice",
                                "reimb_pct": "parkingReimbursementPercentage",
                                "tele_choice": "telecommuteChoice",
                                "WorkSegment": "workSegment",
                                "SchoolSegment": "schoolSegment",
                                "WorkLocation": "workLocation",
                                "SchoolLocation": "schoolLocation"},
                       inplace=True)

        return persons[["personId",
                        "hhId",
                        "personNumber",
                        "age",
                        "sex",
                        "militaryStatus",
                        "employmentStatus",
                        "studentStatus",
                        "abmPersonType",
                        "education",
                        "grade",
                        "weeks",
                        "hours",
                        "race",
                        "hispanic",
                        "abmActivityPattern",
                        "freeParkingChoice",
                        "parkingReimbursementPercentage",
                        "telecommuteChoice",
                        "workSegment",
                        "schoolSegment",
                        "workLocation",
                        "schoolLocation"]]


class TourLists(ScenarioData):
    """ A subclass of the ScenarioData class. Holds all tour list data for a
    completed ABM scenario model run. This includes all data from the ABM
    sub-models with tours. These are held as class properties and include:
        Cross Border Model
        Commercial Vehicle Model
        Internal-External Model
        Individual Model
        Joint Model
        Visitor Model

    The tour list data is loaded from raw ABM output files in the scenario
    output folder and transformed where applicable.

    Properties:
        cross_border: Mexican Resident Cross Border model tour list
        cvm: Commercial Vehicle model tour list
        ie: San Diego Resident Internal-External model tour list
        individual: San Diego Resident Individual travel model tour list
        joint: San Diego Resident Joint travel model tour list
        Visitor: Visitor model tour list
    """
    @property
    @lru_cache(maxsize=1)
    def cross_border(self) -> pd.DataFrame:
        """ Create the Cross-border Model tour list.

        Read in the Cross-border tour list, map field values, and genericize
        field names.

        Returns:
            A Pandas DataFrame of the Cross-border tour list """

        # load tour list into Pandas DataFrame
        fn = "mgra15_based_input" + str(self.properties["year"]) + ".csv"
        maz_taz_mapping = self.mgra_xref[["MGRA", "TAZ"]]
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", "crossborder", "final_tours.csv"),
            usecols=["tour_id",
                     "purpose_id",
                     "pass_type",
                     "poe_id",
                     "start",
                     "end",
                     "origin",
                     "destination",
                     "tour_mode"],
            dtype={"id": "int32",
                   "purpose": "int8",
                   "pass_type": "str",
                   "poe_id": "int8",
                   "start": "int8",
                   "end": "int8",
                   "origin": "int16",
                   "destination": "int16",
                   "tour_mode": "category"})
        
        tours = pd.merge(tours, maz_taz_mapping, left_on='origin', right_on='MGRA', how='left').rename(columns={'taz': 'originTAZ', 'origin':'originMGRA'}).drop(columns=['MGRA'])
        tours = pd.merge(tours, maz_taz_mapping, left_on='destination', right_on='MGRA', how='left').rename(columns={'taz': 'destinationTAZ', 'destination':'destinationMGRA'}).drop(columns=['MGRA'])

        # apply exhaustive field mappings where applicable
        mappings = {
            "purpose_id": {0: "Work",
                        1: "School",
                        2: "Cargo",
                        3: "Shop",
                        4: "Visit",
                        5: "Other"},
            "poe_id": {0: "San Ysidro",
                    1: "Otay Mesa",
                    2: "Tecate",
                    3: "Otay Mesa East",
                    4: "Jacumba"},
        }

        for field in mappings:
            tours[field] = tours[field].map(mappings[field]).astype("category")

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.start)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.end)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"tour_id": "tourID",
                              "purpose_id": "tourPurpose",
                              "poe_id": "pointOfEntry",
                              "start": "departTimeAbmHalfHour",
                              "end": "arriveTimeAbmHalfHour",
                              "tour_mode": "tourMode"},
                     inplace=True)

        return tours[["tourID",
                      "tourPurpose",
                      "pass_type",
                      "pointOfEntry",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "tourMode"]]

    @property
    @lru_cache(maxsize=1)
    def cvm(self) -> pd.DataFrame:
        """ Create the Commercial Vehicle Model tour list.

        Read in the Commercial Vehicle trip lists, apply share allocation, map
        field values, genericize field names, and create the tour list from the
        trip list.

        Returns:
            A Pandas DataFrame of the Commercial Vehicle tour list """
        # create list of all Commercial Vehicle model trip list files
        # files are of the form Trip_<<ActorType>>_<<OriginalTimePeriod>>
        files = ["Trip" + "_" + i + "_" + j + ".csv" for i, j in
                 itertools.product(["FA", "GO", "IN", "RE", "SV", "TH", "WH"],
                                   ["OE", "AM", "MD", "PM", "OL"])]

        # read all trip list files into a Pandas DataFrame
        trips = pd.concat((
            pd.read_csv(os.path.join(self.scenario_path, "output", file),
                        usecols=["SerialNo",
                                 "Trip",
                                 "ActorType",
                                 "HomeZone",
                                 "Mode",
                                 "StartTime",
                                 "EndTime",
                                 "TourType",
                                 "OriginalTimePeriod"],
                        dtype={"SerialNo": "int32",
                               "Trip": "int8",
                               "ActorType": "string",
                               "HomeZone": "int16",
                               "Mode": "string",
                               "StartTime": "float32",
                               "EndTime": "float32",
                               "TourType": "string",
                               "OriginalTimePeriod": "string"})
            for file in files))

        # apply re-allocation originally implemented in
        # Java by Nagendra Dhakar + Joel Freedman at RSG

        # create lookup table of mode-tod-share using scenario properties
        lookup = pd.DataFrame(
            {"Mode": ["L"] * 5 + ["I"] * 5 + ["M"] * 5 + ["H"] * 5,
             "OriginalTimePeriod": ["OE", "AM", "MD", "PM", "OL"] * 4,
             "cvmShare": [self.properties["cvmShareLight"]] * 5 +
                         [0] * 5 +
                         [self.properties["cvmShareMedium"]] * 5 +
                         [self.properties["cvmShareHeavy"]] * 5})

        # merge trip list and lookup table
        trips = trips.merge(lookup)

        # within each mode, the properties file designates a percentage of the
        # trip weight to be removed from the original trip and given to a new
        # identical trip with the "I" (light-heavy duty truck) mode
        new_trips = trips.loc[trips["cvmShare"] > 0].copy()
        new_trips.reset_index(drop=True, inplace=True)
        new_trips["Mode"] = "I"
        trips = pd.concat([trips, new_trips], ignore_index=True)

        # create tour surrogate key
        # unique tour is defined by (SerialNo, Mode)
        trips["tourID"] = trips.groupby(["SerialNo", "Mode"]).ngroup().astype("int32") + 1

        # create tour list using the first and last trip within each tour
        # all tour data constant across trips excepting start/end times
        # first trip provides start time, last trip provides end time
        tours = trips.sort_values(by=["tourID", "Trip"]).groupby(["tourID"])
        tours = tours.head(1).merge(tours.tail(1)[["tourID", "EndTime"]],
                                    on="tourID",
                                    suffixes=("_start", ""))

        # apply exhaustive field mappings where applicable
        mappings = {
            "ActorType": {"FA": "Fleet Allocator",
                          "GO": "Government\\Office",
                          "IN": "Industry",
                          "RE": "Retail",
                          "SV": "Service",
                          "TH": "Transport",
                          "WH": "Wholesale"},
            "Mode": {"L": "Drive Alone",
                     "I": "Light Heavy Duty Truck",
                     "M": "Medium Heavy Duty Truck",
                     "H": "Heavy Heavy Duty Truck"},
            "TourType": {"G": "Goods",
                         "S": "Service",
                         "O": "Other"}
        }

        for field in mappings:
            tours[field] = tours[field].map(mappings[field]).astype("category")

        # map continuous start and end times to ABM half hour time periods
        # times are in continuous hours of the day (0-24) and can wrap into
        # the following day or even multiple following days (>24) with no
        # upper limit

        # create times from continuous hour start and end times
        # taking into account their wrapping into subsequent days
        tours["StartTime"] = tours["StartTime"].apply(
            lambda x: (datetime.combine(date.today(), time.min) +
                       timedelta(hours=(x % 24))).time())
        tours["EndTime"] = tours["EndTime"].apply(
            lambda x: (datetime.combine(date.today(), time.min) +
                       timedelta(hours=(x % 24))).time())

        # map continuous times to abm half hour periods
        depart_half_hour = [
            [p["period"] for p in self.time_periods["abmHalfHour"]
             if p["startTime"] <= x < p["endTime"]]
            for x in tours["StartTime"]]
        depart_half_hour = [val for sublist in depart_half_hour for val in sublist]
        tours = tours.assign(departTimeAbmHalfHour=depart_half_hour)
        tours["departTimeAbmHalfHour"] = tours["departTimeAbmHalfHour"].astype("int8")

        arrive_half_hour = [
            [p["period"] for p in self.time_periods["abmHalfHour"]
             if p["startTime"] <= x < p["endTime"]]
            for x in tours["EndTime"]]
        arrive_half_hour = [val for sublist in arrive_half_hour for val in sublist]
        tours = tours.assign(arriveTimeAbmHalfHour=arrive_half_hour)
        tours["arriveTimeAbmHalfHour"] = tours["arriveTimeAbmHalfHour"].astype("int8")

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.departTimeAbmHalfHour)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.arriveTimeAbmHalfHour)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"ActorType": "actorType",
                              "TourType": "tourPurpose",
                              "HomeZone": "originTAZ",
                              "Mode": "tourMode"},
                     inplace=True)

        return tours[["tourID",
                      "actorType",
                      "tourPurpose",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originTAZ",
                      "tourMode"]]

    @property
    @lru_cache(maxsize=1)
    def ie(self) -> pd.DataFrame:
        """ Create the Internal-External Model tour list.

        Read in the Internal-External trip list, map field values, genericize
        field names, and create the tour list from the trip list.

        Returns:
            A Pandas DataFrame of the Internal-External tour list """

        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "internalExternalTrips.csv"),
            usecols=["personID",
                     "tourID",
                     "outbound",
                     "period",
                     "originMGRA",
                     "destinationMGRA",
                     "originTAZ",
                     "destinationTAZ",
                     "tripMode"],
            dtype={"personID": "int32",
                   "tourID": "int32",
                   "outbound": "boolean",
                   "period": "int8",
                   "originMGRA": "int16",
                   "destinationMGRA": "int16",
                   "originTAZ": "int16",
                   "destinationTAZ": "int16",
                   "tripMode": "int8"})

        # create tour list using the first and last trip within each tour
        # all tour data constant across trips excepting start/end times
        # first trip provides start time, last trip provides end time
        # first trip also provides the tour destination
        tours = trips.sort_values(by=["tourID", "outbound"]).groupby(["tourID"])
        tours = tours.head(1).merge(tours.tail(1)[["tourID", "period"]],
                                    on="tourID",
                                    suffixes=("Start", "End"))

        # apply exhaustive field mappings where applicable
        mappings = {
            "tripMode": {1: "Drive Alone",
                         2: "Shared Ride 2",
                         3: "Shared Ride 3+",
                         4: "Walk",
                         5: "Bike",
                         6: "Walk to Transit",
                         7: "Park and Ride to Transit",
                         8: "Kiss and Ride to Transit",
                         9: "TNC to Transit",
                         10: "Taxi",
                         11: "Non-Pooled TNC",
                         12: "Pooled TNC"},
        }

        for field in mappings:
            tours[field] = tours[field].map(mappings[field]).astype("category")

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.periodStart)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.periodEnd)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"periodStart": "departTimeAbmHalfHour",
                              "periodEnd": "arriveTimeAbmHalfHour",
                              "tripMode": "tourMode"},
                     inplace=True)

        return tours[["tourID",
                      "personID",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "tourMode"]]

    @property
    @lru_cache(maxsize=1)
    def individual(self) -> pd.DataFrame:
        """ Create the Individual Model tour list.

        Read in the Individual tour list, map field values and genericize
        field names.

        Returns:
            A Pandas DataFrame of the Individual tour list """

        # load tour list into Pandas DataFrame
        fn = "final_tours.csv"
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", "resident", fn),
            usecols=["person_id",
                     "tour_id",
                     "tour_category",
                     "primary_purpose",
                     "origin",
                     "destination",
                     "start",
                     "end",
                     "tour_mode"],
            dtype={"person_id": "int32",
                   "tour_id": "int8",
                   "tour_category": "string",
                   "primary_purpose": "string",
                   "origin": "int16",
                   "destination": "int16",
                   "start": "int8",
                   "end": "int8",
                   "tour_mode": "category"})
        
        #filter for non joint tours
        tours = tours[tours.tour_category != 'joint']

        # apply exhaustive field mappings where applicable
        mappings = {
            "tour_category": {"atwork": "At-Work",
                              "non_mandatory": "Individual Non-Mandatory",
                              "mandatory": "Mandatory"},
            # "tour_mode": {1: "Drive Alone",
            #               2: "Shared Ride 2",
            #               3: "Shared Ride 3+",
            #               4: "Walk",
            #               5: "Bike",
            #               6: "Walk to Transit",
            #               7: "Park and Ride to Transit",
            #               8: "Kiss and Ride to Transit",
            #               9: "TNC to Transit",
            #               10: "Taxi",
            #               11: "Non-Pooled TNC",
            #               12: "Pooled TNC",
            #               13: "School Bus"}
        }

        for field in mappings:
            tours[field] = tours[field].map(mappings[field]).astype("category")

        # create tour surrogate key (person_id, tour_id, primary_purpose)
        tour_key = ["person_id", "tour_id", "primary_purpose"]
        tours["tourID"] = tours.groupby(tour_key).ngroup().astype("int32") + 1

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        tours = tours.merge(taz_info, left_on="origin", right_on="MGRA")
        tours.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        tours = tours.merge(taz_info, left_on="destination", right_on="MGRA")
        tours.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.start)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.end)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"person_id": "personID",
                              "tour_category": "tourCategory",
                              "primary_purpose": "tourPurpose",
                              "start": "departTimeAbmHalfHour",
                              "end": "arriveTimeAbmHalfHour",
                              "origin": "originMGRA",
                              "destination": "destinationMGRA",
                              "tour_mode": "tourMode"},
                     inplace=True)

        return tours[["tourID",
                      "personID",
                      "tourCategory",
                      "tourPurpose",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "tourMode"]]

    @property
    @lru_cache(maxsize=1)
    def joint(self) -> pd.DataFrame:
        """ Create the Joint Model tour list.

        Read in the Joint tour list, map field values and genericize field
        names.

        Returns:
            A Pandas DataFrame of the Joint tour list """

        # load tour list into Pandas DataFrame
        fn = "final_tours.csv"
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", "resident", fn),
            usecols=["person_id",
                     "household_id",
                     "tour_id",
                     "tour_category",
                     "primary_purpose",
                     "origin",
                     "destination",
                     "start",
                     "end",
                     "tour_mode",
                     "number_of_participants"
                     ],
            dtype={"person_id": "int32",
                   "household_id": "int32",
                   "tour_id": "int8",
                   "tour_category": "string",
                   "primary_purpose": "string",
                   "origin": "int16",
                   "destination": "int16",
                   "start": "int8",
                   "end": "int8",
                   "tour_mode": "category",
                   "number_of_participants": "int8"})
        
        #filter for non joint tours
        tours = tours[tours.tour_category != 'joint']

        # apply exhaustive field mappings where applicable
        # mappings = {
        #     "tour_category": {"JOINT_NON_MANDATORY": "Joint Non-Mandatory"},
        #     "tour_mode": {2: "Shared Ride 2",
        #                   3: "Shared Ride 3+",
        #                   4: "Walk",
        #                   5: "Bike",
        #                   6: "Walk to Transit",
        #                   7: "Park and Ride to Transit",
        #                   8: "Kiss and Ride to Transit",
        #                   9: "TNC to Transit",
        #                   10: "Taxi",
        #                   11: "Non-Pooled TNC",
        #                   12: "Pooled TNC"}
        # }

        # for field in mappings:
        #     tours[field] = tours[field].map(mappings[field]).astype("category")

        # create tour surrogate key (hh_id, tour_id)
        tour_key = ["household_id", "tour_id"]
        tours["tourID"] = tours.groupby(tour_key).ngroup().astype("int32") + 1

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        tours = tours.merge(taz_info, left_on="origin", right_on="MGRA")
        tours.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        tours = tours.merge(taz_info, left_on="destination", right_on="MGRA")
        tours.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.start)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.end)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"household_id": "hhID",
                              "tour_category": "tourCategory",
                              "primary_purpose": "tourPurpose",
                              "number_of_participants": "NumberofParticipants",
                              "start": "departTimeAbmHalfHour",
                              "end": "arriveTimeAbmHalfHour",
                              "origin": "originMGRA",
                              "destination": "destinationMGRA",
                              "tour_mode": "tourMode"},
                     inplace=True)

        return tours[["tourID",
                      "hhID",
                      "NumberofParticipants",
                      "tourCategory",
                      "tourPurpose",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "tourMode"]]

    @property
    @lru_cache(maxsize=1)
    def visitor(self) -> pd.DataFrame:
        """ Create the Visitor Model tour list.

        Read in the Visitor tour list, map field values and genericize field
        names.

        Returns:
            A Pandas DataFrame of the Visitor tour list """

        # load tour list into Pandas DataFrame
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", "visitor", "final_Tours.csv"),
            usecols=["tour_id",
                     "visitor_travel_type",
                     "purpose_id",
                     "number_of_participants",
                     "income",
                     "start",
                     "end",
                     "origin",
                     "destination",
                     "tour_mode"],
            dtype={"tour_id": "int32",
                   "visitor_travel_type": "category",
                   "purpose_id": "int8",
                   "number_of_participants": "int8",
                   "income": "int8",
                   "start": "int8",
                   "end": "int8",
                   "origin": "int16",
                   "destination": "int16",
                   "tour_mode": "category"})

        # apply exhaustive field mappings where applicable
        mappings = {
            "visitor_travel_type": {0: "Business",
                        1: "Personal"},
            "purpose_id": {0: "Work",
                        1: "Recreation",
                        2: "Dining"},
            "income": {0: "Less than 30k",
                       1: "30k-60k",
                       2: "60k-100k",
                       3: "100k-150k",
                       4: "150k+"},
            # "tourMode": {1: "Drive Alone",
            #              2: "Shared Ride 2",
            #              3: "Shared Ride 3+",
            #              4: "Walk",
            #              5: "Bike",
            #              6: "Walk to Transit",
            #              7: "Park and Ride to Transit",
            #              8: "Kiss and Ride to Transit",
            #              9: "TNC to Transit",
            #              10: "Taxi",
            #              11: "Non-Pooled TNC",
            #              12: "Pooled TNC"}
        }

        for field in mappings:
            tours[field] = tours[field].map(mappings[field]).astype("category")

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        tours = tours.merge(taz_info, left_on="origin", right_on="MGRA")
        tours.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        tours = tours.merge(taz_info, left_on="destination", right_on="MGRA")
        tours.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        # map abm half hours to abm five time of day
        tours["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.start)
        tours["arriveTimeFiveTod"] = self._map_time_periods(abm_half_hour=tours.end)

        # rename columns to standard/generic ABM naming conventions
        tours.rename(columns={"tour_id": "tourID",
                              "tour_mode": "tourMode",
                              "purpose_id": "tourPurpose",
                              "start": "departTimeAbmHalfHour",
                              "end": "arriveTimeAbmHalfHour",
                              "visitor_travel_type": "segment",
                              "origin": "originMGRA",
                              "destination": "destinationMGRA",},
                     inplace=True)

        return tours[["tourID",
                      "segment",
                      "tourPurpose",
                      "number_of_participants",
                      "income",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "tourMode"]]


class TripLists(ScenarioData):
    """ A subclass of the ScenarioData class. Holds all trip list data for a
    completed ABM scenario model run. This includes all data from the ten ABM
    sub-models. These are held as class properties and include:
        Airport (CBX) Model
        Airport (SAN) Model
        Cross Border Model
        Commercial Vehicle Model
        External-External Model
        External-Internal Model
        Internal-External Model
        Individual Model
        Joint Model
        Truck Model
        Visitor Model
        Zombie AV Trips
        Zombie TNC Trips

    The trip list data is loaded from raw ABM output files in the scenario
    output folder and given consistent fields and field values.

    Methods:
        _combine_mode_set: Combines ABM mode field with transit skim set
            field
        _combine_mode_walk: Recodes ABM mode field using the ABM walk mode
            field for walk mode trips

    Properties:
        airport_cbx: Cross Border Express (CBX) model trip list
        airport_san: San Diego Airport (SAN) model trip list
        cross_border: Mexican Resident Cross Border model trip list
        cvm: Commercial Vehicle model trip list
        ee: External-External model trip list, placed here until it can be
            properly incorporated into the EMME data exporter process
        ei: External-Internal model trip list, placed here until it can be
            properly incorporated into the EMME data exporter process
        ie: San Diego Resident Internal-External model trip list
        individual: San Diego Resident Individual travel model trip list
        joint: San Diego Resident Joint travel model trip list
        truck: Truck model trip list, placed here until it can be
            properly incorporated into the EMME data exporter process
        visitor: Visitor model trip list
        zombie_av: 0-passenger Autonomous Vehicle trip list
        zombie_tnc: 0-passenger TNC Vehicle trip list
    """

    @staticmethod
    def _combine_mode_set(mode: pd.Series, transit_set: pd.Series) -> pd.Series:
        """ Combine Pandas Series of ABM mode field values with Pandas Series
        of ABM transit skim set field values.

        Returns:
            A Pandas Series of the combined mode and transit skim set field
            values
        """

        # ensure series are string data type
        mode = mode.astype("string")
        transit_set = transit_set.astype("string")

        # if ABM mode field value contains the string Transit
        # append the transit skim set field value
        mode = np.where(mode.str.contains("Transit"),
                        mode + " - " + transit_set,
                        mode)

        return pd.Series(mode).astype("category")

    @staticmethod
    def _combine_mode_walk(mode: pd.Series, walk_mode: pd.Series) -> pd.Series:
        """ Combine Pandas Series of ABM mode field values with Pandas Series
        of ABM walk mode (micro_walkMode) field values. Update the ABM mode
        field value to the indicated ABM walk mode where appropriate.

        Returns:
            A Pandas Series of the recoded ABM mode field values.
        """

        # ensure series are string data type
        mode = mode.astype("string")
        walk_mode = walk_mode.astype("string")

        # if ABM mode field value is Walk then use the ABM walk mode field
        # value as the ABM mode, otherwise use the ABM mode field value
        mode = np.where(mode == "Walk",
                        walk_mode,
                        mode)

        return pd.Series(mode).astype("category")

    @property
    @lru_cache(maxsize=1)
    def airport_cbx(self) -> pd.DataFrame:
        """ Create the Cross Border Express (CBX) Airport Model trip list.

        Read in the CBX trip list, map field values, and genericize field
        names.

        Returns:
            A Pandas DataFrame of the CBX trip list """

        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "airport.CBX", "final_cbxtrips.csv"),
            usecols=["trip_id",
                     "tour_id",
                     "outbound",
                     "primary_purpose",
                     "depart",
                     "origin",
                     "destination",
                    #  "originTAZ",
                    #  "destinationTAZ",
                     "trip_mode",
                    #  "arrivalMode",
                    #  "set",
                     "vot1",
                     "vot2",
                     "vot3"],
            dtype={"trip_id": "int32",
                   "outbound": "bool",
                   "primary_purpose": "category",
                   "depart": "int8",
                   "origin": "int16",
                   "destination": "int16",
                #    "originTAZ": "int16",
                #    "destinationTAZ": "int16",
                   "trip_mode": "category",
                #    "arrivalMode": "int8",
                #    "set": "int8",
                   "vot1": "bool",
                   "vot2": "bool",
                   "vot3": "bool"})
        
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", "airport.CBX", "final_cbxtours.csv"),
            usecols=['tour_id','party_size', 'income', 'nights'])
        
        trips = trips.merge(tours, on='tour_id', how='left').rename(columns={'party_size': 'size'})

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        trips = trips.merge(taz_info, left_on="origin", right_on="MGRA").drop(columns=['MGRA'])
        trips.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        trips = trips.merge(taz_info, left_on="destination", right_on="MGRA").drop(columns=['MGRA'])
        trips.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        # apply exhaustive field mappings where applicable
        mappings = {
            # "purpose": {0: "Resident Business",
            #             1: "Resident Personal",
            #             2: "Visitor Business",
            #             3: "Visitor Personal",
            #             4: "External"},
            "income": {0: "Less than 25k",
                       1: "25k-50k",
                       2: "50k-75k",
                       3: "75k-100k",
                       4: "100k-125k",
                       5: "125k-150k",
                       6: "150k-200k",
                       7: "200k+"},
            # "tripMode": {1: "Drive Alone",
            #              2: "Shared Ride 2",
            #              3: "Shared Ride 3+",
            #              4: "Walk",
            #              5: "Bike",
            #              6: "Walk to Transit",
            #              7: "Park and Ride to Transit",
            #              8: "Kiss and Ride to Transit",
            #              9: "TNC to Transit",
            #              10: "Taxi",
            #              11: "Non-Pooled TNC",
            #              12: "Pooled TNC"},
            # "arrivalMode": {1: "Parking lot terminal",
            #                 2: "Parking lot off-site San Diego airport area",
            #                 3: "Parking lot off-site private",
            #                 4: "Pickup/Drop-off escort",
            #                 5: "Pickup/Drop-off curbside",
            #                 6: "Rental car",
            #                 7: "Taxi",
            #                 8: "Non-Pooled TNC",
            #                 9: "Pooled TNC",
            #                 10: "Shuttle/van/courtesy vehicle",
            #                 11: "Transit"},

            # "set": {-1: "",
            #         0: "Local Bus",
            #         1: "Premium Transit",
            #         2: "Local Bus and Premium Transit"}
        }

        for field in mappings:
            data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=trips.depart)

        # concatenate mode and transit skim set for transit trips
        # trips["tripMode"] = self._combine_mode_set(mode=trips.trip_mode, transit_set=trips.set)

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = np.where(trips.vot1, 'Low', np.where(trips.vot2, 'Medium', 'High'))

        # no airport trips use transponders or AVs
        # no airport trips are allowed to park in another MGRA
        # there is no destination purpose
        trips["transponderAvailable"] = False
        trips["avUsed"] = False
        trips["parkingMGRA"] = pd.Series(np.NaN, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.NaN, dtype="float32")

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        trips["weightTrip"] = 1 / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")
        trips["weightPersonTrip"] = pd.Series(trips["size"] / self.properties["sampleRate"], dtype="float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"trip_id": "tripID",
                              "primary_purpose": "tripPurpose",
                              "trip_mode": "tripMode",
                              "income": "incomeCategory",
                              "nights": "nightsStayed",
                              "depart": "departTimeAbmHalfHour",
                              "origin": "originMGRA", 
                              "destination": "destinationMGRA"},
                     inplace=True)

        return trips[["tripID",
                      "outbound",
                      "tripPurpose",
                      "incomeCategory",
                      "nightsStayed",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                    #   "arrivalMode",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip"]]

    @property
    @lru_cache(maxsize=1)
    def airport_san(self) -> pd.DataFrame:
        """ Create the San Diego (SAN) Airport Model trip list.

        Read in the SAN trip list, map field values, and genericize field
        names.

        Returns:
            A Pandas DataFrame of the SAN trip list """

        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "airport_out.SAN.csv"),
            usecols=["id",
                     "direction",
                     "purpose",
                     "size",
                     "income",
                     "nights",
                     "departTime",
                     "originMGRA",
                     "destinationMGRA",
                     "originTAZ",
                     "destinationTAZ",
                     "tripMode",
                     "arrivalMode",
                     "boardingTAP",
                     "alightingTAP",
                     "set",
                     "valueOfTime"],
            dtype={"id": "int32",
                   "direction": "bool",
                   "purpose": "int8",
                   "size": "int8",
                   "income": "int8",
                   "nights": "int8",
                   "departTime": "int8",
                   "originMGRA": "int16",
                   "destinationMGRA": "int16",
                   "originTAZ": "int16",
                   "destinationTAZ": "int16",
                   "tripMode": "int8",
                   "arrivalMode": "int8",
                   "boardingTAP": "int16",
                   "alightingTAP": "int16",
                   "set": "int8",
                   "valueOfTime": "float32"})

        # apply exhaustive field mappings where applicable
        mappings = {
            "purpose": {0: "Resident Business",
                        1: "Resident Personal",
                        2: "Visitor Business",
                        3: "Visitor Personal",
                        4: "External"},
            "income": {0: "Less than 25k",
                       1: "25k-50k",
                       2: "50k-75k",
                       3: "75k-100k",
                       4: "100k-125k",
                       5: "125k-150k",
                       6: "150k-200k",
                       7: "200k+"},
            "tripMode": {1: "Drive Alone",
                         2: "Shared Ride 2",
                         3: "Shared Ride 3+",
                         4: "Walk",
                         5: "Bike",
                         6: "Walk to Transit",
                         7: "Park and Ride to Transit",
                         8: "Kiss and Ride to Transit",
                         9: "TNC to Transit",
                         10: "Taxi",
                         11: "Non-Pooled TNC",
                         12: "Pooled TNC"},
            "arrivalMode": {1: "Parking lot terminal",
                            2: "Parking lot off-site San Diego airport area",
                            3: "Parking lot off-site private",
                            4: "Pickup/Drop-off escort",
                            5: "Pickup/Drop-off curbside",
                            6: "Rental car",
                            7: "Taxi",
                            8: "Non-Pooled TNC",
                            9: "Pooled TNC",
                            10: "Shuttle/van/courtesy vehicle",
                            11: "Transit"},
            "boardingTAP": {key: value for (key, value) in
                            zip(list(range(1, 99999)),
                                list(range(1, 99999)))},
            "alightingTAP": {key: value for (key, value) in
                             zip(list(range(1, 99999)),
                                 list(range(1, 99999)))},
            "set": {-1: "",
                    0: "Local Bus",
                    1: "Premium Transit",
                    2: "Local Bus and Premium Transit"}
        }

        for field in mappings:
            if field in ["boardingTAP", "alightingTAP"]:
                data_type = "float32"
            else:
                data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.departTime)

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(mode=trips.tripMode, transit_set=trips.set)

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(vot=trips.valueOfTime)

        # no airport trips use transponders or AVs
        # no airport trips are allowed to park in another MGRA
        # there is no destination purpose
        trips["transponderAvailable"] = False
        trips["avUsed"] = False
        trips["parkingMGRA"] = pd.Series(np.NaN, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.NaN, dtype="float32")

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        trips["weightTrip"] = 1 / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")
        trips["weightPersonTrip"] = pd.Series(trips["size"] / self.properties["sampleRate"], dtype="float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"id": "tripID",
                              "direction": "outbound",
                              "purpose": "tripPurpose",
                              "income": "incomeCategory",
                              "nights": "nightsStayed",
                              "departTime": "departTimeAbmHalfHour"},
                     inplace=True)

        return trips[["tripID",
                      "outbound",
                      "tripPurpose",
                      "incomeCategory",
                      "nightsStayed",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "arrivalMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip"]]

    @property
    @lru_cache(maxsize=1)
    def cross_border(self) -> pd.DataFrame:
        """ Create the Cross-border Model trip list.

        Read in the Cross-border trip list, map field values, and genericize
        field names.

        Returns:
            A Pandas DataFrame of the Cross-border trip list """

        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "crossBorderTrips.csv"),
            usecols=["tourID",
                     "tripID",
                     "outbound",
                     "period",
                     "originPurp",
                     "destPurp",
                     "originMGRA",
                     "destinationMGRA",
                     "originTAZ",
                     "destinationTAZ",
                     "tripMode",
                     "boardingTap",
                     "alightingTap",
                     "set",
                     "valueOfTime",
                     "parkingCost"],
            dtype={"tourID": "int32",
                   "tripID": "int8",
                   "outbound": "boolean",
                   "period": "int8",
                   "originPurp": "int8",
                   "destPurp": "int8",
                   "originMGRA": "int16",
                   "destinationMGRA": "int16",
                   "originTAZ": "int16",
                   "destinationTAZ": "int16",
                   "tripMode": "int8",
                   "boardingTap": "int16",
                   "alightingTap": "int16",
                   "set": "int8",
                   "valueOfTime": "float32",
                   "parkingCost": "float32"})

        # use the tripID column from the data-set as stopID within the tour
        # and create actual tripID field
        trips.rename(columns={"tripID": "stopID"}, inplace=True)
        trips = trips.sort_values(by=["tourID", "stopID"]).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # apply exhaustive field mappings where applicable
        mappings = {
            "originPurp": {-1: "Unknown",
                           0: "Work",
                           1: "School",
                           2: "Cargo",
                           3: "Shop",
                           4: "Visit",
                           5: "Other"},
            "destPurp": {-1: "Unknown",
                         0: "Work",
                         1: "School",
                         2: "Cargo",
                         3: "Shop",
                         4: "Visit",
                         5: "Other"},
            "tripMode": {1: "Drive Alone",
                         2: "Shared Ride 2",
                         3: "Shared Ride 3+",
                         4: "Walk",
                         5: "Bike",
                         6: "Walk to Transit",
                         7: "Park and Ride to Transit",
                         8: "Kiss and Ride to Transit",
                         9: "TNC to Transit",
                         10: "Taxi",
                         11: "Non-Pooled TNC",
                         12: "Pooled TNC"},
            "boardingTap": {key: value for (key, value) in
                            zip(list(range(1, 99999)),
                                list(range(1, 99999)))},
            "alightingTap": {key: value for (key, value) in
                             zip(list(range(1, 99999)),
                                 list(range(1, 99999)))},
            "set": {-1: "",
                    0: "Local Bus",
                    1: "Premium Transit",
                    2: "Local Bus and Premium Transit"}
        }

        for field in mappings:
            if field in ["boardingTap", "alightingTap"]:
                trips[field] = trips[field].map(mappings[field]).astype("float32")
            else:
                trips[field] = trips[field].map(mappings[field]).astype("category")

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.period)

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(mode=trips.tripMode,
                                                   transit_set=trips.set)

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(
            vot=trips.valueOfTime)

        # transform parking cost from cents to dollars
        trips["parkingCost"] = round(trips.parkingCost / 100, 2)

        # no cross-border trips use transponders or AVs
        # no cross-border trips are allowed to park in another MGRA
        trips["transponderAvailable"] = False
        trips["avUsed"] = False
        trips["parkingMGRA"] = pd.Series(np.NaN, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.NaN, dtype="float32")

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        conditions = [(trips["tripMode"] == "Shared Ride 2"),
                      (trips["tripMode"] == "Shared Ride 3+"),
                      (trips["tripMode"] == "Taxi"),
                      (trips["tripMode"] == "Non-Pooled TNC"),
                      (trips["tripMode"] == "Pooled TNC")]
        choices = [1 / self.properties["sr2Passengers"],
                   1 / self.properties["sr3Passengers"],
                   1 / self.properties["taxiPassengers"],
                   1 / self.properties["nonPooledTNCPassengers"],
                   1 / self.properties["pooledTNCPassengers"]]

        trips["weightTrip"] = pd.Series(np.select(conditions, choices, default=1) / self.properties["sampleRate"], dtype="float32")
        trips["weightPersonTrip"] = 1 / self.properties["sampleRate"]
        trips["weightPersonTrip"] = trips["weightPersonTrip"].astype("float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"id": "tripID",
                              "period": "departTimeAbmHalfHour",
                              "originPurp": "tripPurposeOrigin",
                              "destPurp": "tripPurposeDestination",
                              "parkingCost": "costParking",
                              "boardingTap": "boardingTAP",
                              "alightingTap": "alightingTAP"},
                     inplace=True)

        return trips[["tripID",
                      "tourID",
                      "stopID",
                      "outbound",
                      "tripPurposeOrigin",
                      "tripPurposeDestination",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip",
                      "costParking"]]

    @property
    @lru_cache(maxsize=1)
    def cvm(self) -> pd.DataFrame:
        """ Create the Commercial Vehicle Model trip list.

        Read in the Commercial Vehicle trip list, apply trip scaling and share
        allocation, map field values, and genericize field names.

        Returns:
            A Pandas DataFrame of the Commercial Vehicle trip list """

        # create list of all Commercial Vehicle model trip list files
        # files are of the form Trip_<<ActorType>>_<<OriginalTimePeriod>>
        files = ["Trip" + "_" + i + "_" + j + ".csv" for i, j in
                 itertools.product(["FA", "GO", "IN", "RE", "SV", "TH", "WH"],
                                   ["OE", "AM", "MD", "PM", "OL"])]

        # read all trip list files into a Pandas DataFrame
        trips = pd.concat((
            pd.read_csv(os.path.join(self.scenario_path, "output", file),
                        usecols=["SerialNo",
                                 "Trip",
                                 "HomeZone",
                                 "ActorType",
                                 "OPurp",
                                 "DPurp",
                                 "I",
                                 "J",
                                 "Mode",
                                 "StartTime",
                                 "EndTime",
                                 "StopDuration",
                                 "TourType",
                                 "OriginalTimePeriod"],
                        dtype={"SerialNo": "int32",
                               "Trip": "int8",
                               "ActorType": "string",
                               "HomeZone": "int16",
                               "OPurp": "string",
                               "DPurp": "string",
                               "I": "int16",
                               "J": "int16",
                               "Mode": "string",
                               "StartTime": "float32",
                               "EndTime": "float32",
                               "StopDuration": "float32",
                               "TourType": "string",
                               "OriginalTimePeriod": "string"}
                        )
            for file in files))

        # apply weighting and share re-allocation originally implemented in
        # Java by Nagendra Dhakar + Joel Freedman at RSG

        # create lookup table of mode-tod-scale-share using scenario properties
        lookup = pd.DataFrame(
            {"Mode": ["L"] * 5 + ["I"] * 5 + ["M"] * 5 + ["H"] * 5,
             "OriginalTimePeriod": ["OE", "AM", "MD", "PM", "OL"] * 4,
             "cvmScale": self.properties["cvmScaleLight"] +
                         self.properties["cvmScaleMedium"] +
                         self.properties["cvmScaleMedium"] +
                         self.properties["cvmScaleHeavy"],
             "cvmShare": [self.properties["cvmShareLight"]] * 5 +
                         [0] * 5 +
                         [self.properties["cvmShareMedium"]] * 5 +
                         [self.properties["cvmShareHeavy"]] * 5})

        # merge trip list and lookup table
        trips = trips.merge(lookup)

        # within each mode, the properties file designates a percentage of the
        # trip weight to be removed from the original trip and given to a new
        # identical trip with the "I" (light-heavy duty truck) mode
        new_trips = trips.loc[trips["cvmShare"] > 0].copy()
        new_trips.reset_index(drop=True, inplace=True)
        new_trips["Mode"] = "I"

        # within each mode and tour start abm five time of day period, the
        # properties file designates a scaling factor to apply to the trip
        # weight taking into account the share factor
        new_trips["weightTrip"] = new_trips["cvmScale"] * new_trips["cvmShare"]
        trips["weightTrip"] = trips["cvmScale"] * (1 - trips["cvmShare"])
        trips = pd.concat([trips, new_trips], ignore_index=True)

        # apply exhaustive field mappings where applicable
        mappings = {
            "OPurp": {"Est": "Return to Establishment",
                      "Gds": "Goods",
                      "Srv": "Service",
                      "Oth": "Other"},
            "DPurp": {"Est": "Return to Establishment",
                      "Gds": "Goods",
                      "Srv": "Service",
                      "Oth": "Other"},
            "Mode": {"L": "Drive Alone",
                     "I": "Light Heavy Duty Truck",
                     "M": "Medium Heavy Duty Truck",
                     "H": "Heavy Heavy Duty Truck"},
        }

        for field in mappings:
            trips[field] = trips[field].map(mappings[field]).astype("category")

        # create tour and trip surrogate keys
        # unique tour is defined by (SerialNo, Mode)
        # unique trip is defined by (SerialNo, Mode, Trip)
        trips["tourID"] = trips.groupby(["SerialNo", "Mode"]).ngroup().astype("int32") + 1
        trips = trips.sort_values(by=["SerialNo", "Mode", "Trip"]).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # map continuous start and end times to ABM half hour time periods
        # times are in continuous hours of the day (0-24) and can wrap into
        # the following day or even multiple following days (>24) with no
        # upper limit

        # create times from continuous hour start and end times
        # taking into account their wrapping into subsequent days
        trips["StartTime"] = trips["StartTime"].apply(
            lambda x: (datetime.combine(date.today(), time.min) +
                       timedelta(hours=(x % 24))).time())
        trips["EndTime"] = trips["EndTime"].apply(
            lambda x: (datetime.combine(date.today(), time.min) +
                       timedelta(hours=(x % 24))).time())

        # map continuous times to abm half hour periods
        depart_half_hour = [
            [p["period"] for p in self.time_periods["abmHalfHour"]
             if p["startTime"] <= x < p["endTime"]]
            for x in trips["StartTime"]]
        depart_half_hour = [val for sublist in depart_half_hour for val in sublist]
        trips = trips.assign(departTimeAbmHalfHour=depart_half_hour)
        trips["departTimeAbmHalfHour"] = trips["departTimeAbmHalfHour"].astype("int8")

        arrive_half_hour = [
            [p["period"] for p in self.time_periods["abmHalfHour"]
             if p["startTime"] <= x < p["endTime"]]
            for x in trips["EndTime"]]
        arrive_half_hour = [val for sublist in arrive_half_hour for val in sublist]
        trips = trips.assign(arriveTimeAbmHalfHour=arrive_half_hour)
        trips["arriveTimeAbmHalfHour"] = trips["arriveTimeAbmHalfHour"].astype("int8")

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.departTimeAbmHalfHour)

        trips["arriveTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.arriveTimeAbmHalfHour)

        # all cvm trips are high value of time
        # only Drive Alone cvm trips use transponders
        # no cvm trips use AVs
        # no cvm trips are allowed to park in another MGRA
        trips["valueOfTimeCategory"] = "High"
        trips["valueOfTimeCategory"] = trips["valueOfTimeCategory"].astype("category")
        trips["transponderAvailable"] = np.where(trips["Mode"] == "Drive Alone", True, False)
        trips["avUsed"] = False
        trips["parkingTAZ"] = pd.Series(np.NaN, dtype="float32")

        # add person-based weight and adjust weights
        # by the ABM scenario final iteration sample rate
        trips["weightTrip"] = pd.Series(trips["weightTrip"] / self.properties["sampleRate"], dtype="float32")
        trips["weightPersonTrip"] = trips["weightTrip"]

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"Trip": "stopID",
                              "OPurp": "tripPurposeOrigin",
                              "DPurp": "tripPurposeDestination",
                              "I": "originTAZ",
                              "J": "destinationTAZ",
                              "Mode": "tripMode",
                              "StopDuration": "stopDuration"},
                     inplace=True)

        return trips[["tripID",
                      "tourID",
                      "stopID",
                      "tripPurposeOrigin",
                      "tripPurposeDestination",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "stopDuration",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip"]]

    @property
    @lru_cache(maxsize=1)
    def ee(self) -> pd.DataFrame:
        """ Create the External-External Model trip list.

        Read in the External-External trip last, map field values, and
        genericize field names.

        Returns:
            A Pandas DataFrame of the External-External trips list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "report", "eetrip.csv"),
            usecols=["OTAZ",
                     "DTAZ",
                     "TOD",
                     "MODE",
                     "TRIPS",
                     "TIME",
                     "DIST",
                     "AOC",
                     "TOLLCOST"],
            dtype={"OTAZ": "int16",
                   "DTAZ": "int16",
                   "TOD": "string",
                   "MODE": "string",
                   "TRIPS": "float32",
                   "TIME": "float32",
                   "DIST": "float32",
                   "AOC": "float32",
                   "TOLLCOST": "float32"})

        # expand trip list by 3x
        # divide [TRIPS] field by 3
        # assign each copy of trip list to each value of time category
        trips_low = trips.copy()
        trips_low["valueOfTimeCategory"] = "Low"

        trips_med = trips.copy()
        trips_med["valueOfTimeCategory"] = "Medium"

        trips_high = trips.copy()
        trips_high["valueOfTimeCategory"] = "High"

        trips = pd.concat([trips_low, trips_med, trips_high], ignore_index=True)

        trips["TRIPS"] = trips["TRIPS"] / 3

        # create trip surrogate key
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # apply exhaustive field mappings where applicable
        mappings = {
            "TOD": {"EA": 1,
                    "AM": 2,
                    "MD": 3,
                    "PM": 4,
                    "EV": 5},
            "MODE": {"DA": "Drive Alone",
                     "S2": "Shared Ride 2",
                     "S3": "Shared Ride 3+"}
        }

        for field in mappings:
            if field == "TOD":
                trips[field] = trips[field].map(mappings[field]).astype("int8")
            else:
                trips[field] = trips[field].map(mappings[field]).astype("category")

        # convert cents-based cost fields to dollars
        trips["AOC"] = trips["AOC"] / 100
        trips["TOLLCOST"] = trips["TOLLCOST"] / 100

        # no trips use transponders or autonomous vehicles
        trips["transponderAvailable"] = False
        trips["avUsed"] = False

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        conditions = [(trips["MODE"] == "Shared Ride 2"),
                      (trips["MODE"] == "Shared Ride 3+")]
        choices = [self.properties["sr2Passengers"],
                   self.properties["sr3Passengers"]]

        trips["weightPersonTrip"] = pd.Series(
            trips["TRIPS"] * np.select(conditions, choices, default=1) / self.properties["sampleRate"],
            dtype="float32")
        trips["weightTrip"] = trips["TRIPS"] / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"OTAZ": "originTAZ",
                              "DTAZ": "destinationTAZ",
                              "TOD": "departTimeFiveTod",
                              "MODE": "tripMode",
                              "TIME": "timeDrive",
                              "DIST": "distanceDrive",
                              "AOC": "costOperatingDrive",
                              "TOLLCOST": "costTollDrive"},
                     inplace=True)

        # create total time/distance/cost columns
        trips["timeTotal"] = trips["timeDrive"]
        trips["distanceTotal"] = trips["distanceDrive"]
        trips["costTotal"] = trips["costTollDrive"] + trips["costOperatingDrive"]

        return trips[["tripID",
                      "departTimeFiveTod",
                      "originTAZ",
                      "destinationTAZ",
                      "tripMode",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip",
                      "timeDrive",
                      "distanceDrive",
                      "costTollDrive",
                      "costOperatingDrive",
                      "timeTotal",
                      "distanceTotal",
                      "costTotal"]]

    @property
    @lru_cache(maxsize=1)
    def ei(self) -> pd.DataFrame:
        """ Create the External-Internal Model trip list.

        Read in the External-Internal trip last, map field values, and
        genericize field names.

        Returns:
            A Pandas DataFrame of the External-Internal trips list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "report", "eitrip.csv"),
            usecols=["OTAZ",
                     "DTAZ",
                     "TOD",
                     "MODE",
                     "PURPOSE",
                     "TRIPS",
                     "TIME",
                     "DIST",
                     "AOC",
                     "TOLLCOST"],
            dtype={"OTAZ": "int16",
                   "DTAZ": "int16",
                   "TOD": "string",
                   "MODE": "string",
                   "PURPOSE": "string",
                   "TRIPS": "float32",
                   "TIME": "float32",
                   "DIST": "float32",
                   "AOC": "float32",
                   "TOLLCOST": "float32"})

        # expand trip list by 3x
        # divide [TRIPS] field by 3
        # assign each copy of trip list to each value of time category
        trips_low = trips.copy()
        trips_low["valueOfTimeCategory"] = "Low"

        trips_med = trips.copy()
        trips_med["valueOfTimeCategory"] = "Medium"

        trips_high = trips.copy()
        trips_high["valueOfTimeCategory"] = "High"

        trips = pd.concat([trips_low, trips_med, trips_high], ignore_index=True)

        trips["TRIPS"] = trips["TRIPS"] / 3

        # create trip surrogate key
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # apply exhaustive field mappings where applicable
        mappings = {
            "TOD": {"EA": 1,
                    "AM": 2,
                    "MD": 3,
                    "PM": 4,
                    "EV": 5},
            "MODE": {"DAN": "Drive Alone",
                     "DAT": "Drive Alone",
                     "S2N": "Shared Ride 2",
                     "S2T": "Shared Ride 2",
                     "S3N": "Shared Ride 3+",
                     "S3T": "Shared Ride 3+"},
            "PURPOSE": {"NONWORK": "Non-Work",
                        "WORK": "Work"}
        }

        for field in mappings:
            if field == "TOD":
                trips[field] = trips[field].map(mappings[field]).astype("int8")
            else:
                trips[field] = trips[field].map(mappings[field]).astype("category")

        # convert cents-based cost fields to dollars
        trips["AOC"] = trips["AOC"] / 100
        trips["TOLLCOST"] = trips["TOLLCOST"] / 100

        # no trips use transponders or autonomous vehicles
        trips["transponderAvailable"] = False
        trips["avUsed"] = False

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        conditions = [(trips["MODE"] == "Shared Ride 2"),
                      (trips["MODE"] == "Shared Ride 3+")]
        choices = [self.properties["sr2Passengers"],
                   self.properties["sr3Passengers"]]

        trips["weightPersonTrip"] = pd.Series(
            trips["TRIPS"] * np.select(conditions, choices, default=1) / self.properties["sampleRate"],
            dtype="float32")
        trips["weightTrip"] = trips["TRIPS"] / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"OTAZ": "originTAZ",
                              "DTAZ": "destinationTAZ",
                              "TOD": "departTimeFiveTod",
                              "MODE": "tripMode",
                              "PURPOSE": "tripPurpose",
                              "TIME": "timeDrive",
                              "DIST": "distanceDrive",
                              "AOC": "costOperatingDrive",
                              "TOLLCOST": "costTollDrive"},
                     inplace=True)

        # create total time/distance/cost columns
        trips["timeTotal"] = trips["timeDrive"]
        trips["distanceTotal"] = trips["distanceDrive"]
        trips["costTotal"] = trips["costTollDrive"] + trips["costOperatingDrive"]

        return trips[["tripID",
                      "departTimeFiveTod",
                      "originTAZ",
                      "destinationTAZ",
                      "tripMode",
                      "tripPurpose",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip",
                      "timeDrive",
                      "distanceDrive",
                      "costTollDrive",
                      "costOperatingDrive",
                      "timeTotal",
                      "distanceTotal",
                      "costTotal"]]

    @property
    @lru_cache(maxsize=1)
    def ie(self) -> pd.DataFrame:
        """ Create the Internal-External Model trip list.

        Read in the Internal-External trip list, map field values,
        and genericize field names.

        Returns:
            A Pandas DataFrame of the Internal-External trip list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "internalExternalTrips.csv"),
            usecols=["hhID",
                     "personID",
                     "tourID",
                     "outbound",
                     "period",
                     "originMGRA",
                     "destinationMGRA",
                     "originTAZ",
                     "destinationTAZ",
                     "tripMode",
                     "av_avail",
                     "boardingTap",
                     "alightingTap",
                     "set",
                     "valueOfTime"],
            dtype={"hhID": "int32",
                   "personID": "int32",
                   "tourID": "int32",
                   "outbound": "boolean",
                   "period": "int8",
                   "originMGRA": "int16",
                   "destinationMGRA": "int16",
                   "originTAZ": "int16",
                   "destinationTAZ": "int16",
                   "tripMode": "int8",
                   "av_avail": "bool",
                   "boardingTap": "int16",
                   "alightingTap": "int16",
                   "set": "int8",
                   "valueOfTime": "float32"})

        # load output household transponder ownership data
        hh_fn = "householdData_" + str(self.properties["iterations"]) + ".csv"
        hh = pd.read_csv(
            os.path.join(self.scenario_path, "output", hh_fn),
            usecols=["hh_id",
                     "transponder"],
            dtype={"hh_id": "int32",
                   "transponder": "bool"})

        # if household has a transponder then all trips can use it
        trips = trips.merge(hh, left_on="hhID", right_on="hh_id")

        # apply exhaustive field mappings where applicable
        mappings = {
            "tripMode": {1: "Drive Alone",
                         2: "Shared Ride 2",
                         3: "Shared Ride 3+",
                         4: "Walk",
                         5: "Bike",
                         6: "Walk to Transit",
                         7: "Park and Ride to Transit",
                         8: "Kiss and Ride to Transit",
                         9: "TNC to Transit",
                         10: "Taxi",
                         11: "Non-Pooled TNC",
                         12: "Pooled TNC"},
            "boardingTap": {key: value for (key, value) in
                            zip(list(range(1, 99999)),
                                list(range(1, 99999)))},
            "alightingTap": {key: value for (key, value) in
                             zip(list(range(1, 99999)),
                                 list(range(1, 99999)))},
            "set": {-1: "",
                    0: "Local Bus",
                    1: "Premium Transit",
                    2: "Local Bus and Premium Transit"}
        }

        for field in mappings:
            if field in ["boardingTap", "alightingTap"]:
                data_type = "float32"
            else:
                data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # create trip surrogate key
        # create stop surrogate key
        # every tourID contains only two trips (outbound and outbound)
        trips["stopID"] = trips.sort_values(by=["tourID", "outbound"]).groupby(["tourID"]).cumcount().astype("int8") + 1
        trips = trips.sort_values(by=["tourID", "stopID"]).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.period
        )

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(
            mode=trips.tripMode,
            transit_set=trips.set
        )

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(
            vot=trips.valueOfTime
        )

        # no internal-external trips are allowed to park in another MGRA
        trips["parkingMGRA"] = pd.Series(np.nan, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.nan, dtype="float32")

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        conditions = [(trips["tripMode"] == "Shared Ride 2"),
                      (trips["tripMode"] == "Shared Ride 3+"),
                      (trips["tripMode"] == "Taxi"),
                      (trips["tripMode"] == "Non-Pooled TNC"),
                      (trips["tripMode"] == "Pooled TNC")]
        choices = [1 / self.properties["sr2Passengers"],
                   1 / self.properties["sr3Passengers"],
                   1 / self.properties["taxiPassengers"],
                   1 / self.properties["nonPooledTNCPassengers"],
                   1 / self.properties["pooledTNCPassengers"]]

        trips["weightTrip"] = pd.Series(
            np.select(conditions, choices, default=1) / self.properties["sampleRate"],
            dtype="float32")
        trips["weightPersonTrip"] = 1 / self.properties["sampleRate"]
        trips["weightPersonTrip"] = trips["weightPersonTrip"].astype("float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"period": "departTimeAbmHalfHour",
                              "av_avail": "avUsed",
                              "boardingTap": "boardingTAP",
                              "alightingTap": "alightingTAP",
                              "transponder": "transponderAvailable"},
                     inplace=True)

        return trips[["tripID",
                      "personID",
                      "tourID",
                      "stopID",
                      "outbound",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip"]]

    @property
    @lru_cache(maxsize=1)
    def individual(self) -> pd.DataFrame:
        """ Create the Individual Model trip list.

        Read in the Individual trip list, map field values, and genericize
        field names.

        Returns:
            A Pandas DataFrame of the Individual trip list """
        # load trip list into Pandas DataFrame
        fn = "final_trips.csv"
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "resident", fn),
            usecols=["person_id",
                     "tour_id",
                     "trip_id",
                     "outbound",
                     "primary_purpose",
                     "orig_purpose",
                     "dest_purpose",
                     "orig_mgra",
                     "dest_mgra",
                     "parking_mgra",
                     "stop_period",
                     "trip_mode",
                     "av_avail",
                     "trip_board_tap",
                     "trip_alight_tap",
                     "set",
                     "valueOfTime",
                     "transponder_avail",
                     "micro_walkMode",
                     "micro_trnAcc",
                     "micro_trnEgr",
                     "parkingCost"],
            dtype={"person_id": "int32",
                   "tour_id": "int8",
                   "trip_id": "int8",
                   "outbound": "bool",
                   "primary_purpose": "string",
                   "orig_purpose": "string",
                   "dest_purpose": "string",
                   "orig_mgra": "int16",
                   "dest_mgra": "int16",
                   "parking_mgra": "int16",
                   "stop_period": "int8",
                   "trip_mode": "int8",
                   "av_avail": "bool",
                   "trip_board_tap": "int16",
                   "trip_alight_tap": "int16",
                   "set": "int8",
                   "valueOfTime": "float32",
                   "transponder_avail": "bool",
                   "micro_walkMode": "int8",
                   "micro_trnAcc": "int8",
                   "micro_trnEgr": "int8",
                   "parkingCost": "float32"})

        # apply exhaustive field mappings where applicable
        mappings = {
            "parking_mgra": {key: value for (key, value) in
                             zip(list(range(1, len(self.mgra_xref["MGRA"]) + 1)),
                                 list(range(1, len(self.mgra_xref["MGRA"]) + 1)))},
            # "trip_mode": {1: "Drive Alone",
            #               2: "Shared Ride 2",
            #               3: "Shared Ride 3+",
            #               4: "Walk",
            #               5: "Bike",
            #               6: "Walk to Transit",
            #               7: "Park and Ride to Transit",
            #               8: "Kiss and Ride to Transit",
            #               9: "TNC to Transit",
            #               10: "Taxi",
            #               11: "Non-Pooled TNC",
            #               12: "Pooled TNC",
            #               13: "School Bus"},

            # "set": {0: "Local Bus",
            #         1: "Premium Transit",
            #         2: "Local Bus and Premium Transit"},
            # "micro_walkMode": {1: "Walk",
            #                    2: "Micro-Mobility",
            #                    3: "Micro-Transit"},
            # "micro_trnAcc": {1: "Walk",
            #                  2: "Micro-Mobility",
            #                  3: "Micro-Transit"},
            # "micro_trnEgr": {1: "Walk",
            #                  2: "Micro-Mobility",
            #                  3: "Micro-Transit"}
        }

        for field in mappings:
            if field in ["parking_mgra"]:
                data_type = "float32"
            else:
                data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # create tour surrogate key (person_id, tour_id, primary_purpose)
        tour_key = ["person_id", "tour_id", "primary_purpose"]
        trips["tourID"] = pd.Series(trips.groupby(tour_key).ngroup() + 1, dtype="int32")

        # create tour stop surrogate key (outbound, trip_id)
        stop_key = ["outbound", "trip_id"]
        trips["stopID"] = pd.Series(trips.sort_values(by=stop_key).groupby(tour_key).cumcount() + 1, dtype="int8")

        # create unique trip surrogate key
        trips = trips.sort_values(by=tour_key + stop_key).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        trips = trips.merge(taz_info, left_on="orig_mgra", right_on="MGRA")
        trips.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        trips = trips.merge(taz_info, left_on="dest_mgra", right_on="MGRA")
        trips.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        trips = trips.merge(taz_info, how="left", left_on="parking_mgra", right_on="MGRA")
        trips.rename(columns={"TAZ": "parkingTAZ"}, inplace=True)
        trips["parkingTAZ"] = trips["parkingTAZ"].astype("float32")

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.stop_period
        )

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(
            mode=trips.trip_mode,
            transit_set=trips.set
        )

        # set appropriate walk mode for walk trips
        trips["tripMode"] = self._combine_mode_walk(
            mode=trips.tripMode,
            walk_mode=trips.micro_walkMode
        )

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(
            vot=trips.valueOfTime
        )

        # transform parking cost from cents to dollars
        trips["parkingCost"] = round(trips.parkingCost / 100, 2)

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        conditions = [(trips["tripMode"] == "Shared Ride 2"),
                      (trips["tripMode"] == "Shared Ride 3+"),
                      (trips["tripMode"] == "Taxi"),
                      (trips["tripMode"] == "Non-Pooled TNC"),
                      (trips["tripMode"] == "Pooled TNC")]
        choices = [1 / self.properties["sr2Passengers"],
                   1 / self.properties["sr3Passengers"],
                   1 / self.properties["taxiPassengers"],
                   1 / self.properties["nonPooledTNCPassengers"],
                   1 / self.properties["pooledTNCPassengers"]]

        trips["weightTrip"] = pd.Series(np.select(conditions, choices, default=1) / self.properties["sampleRate"], dtype="float32")
        trips["weightPersonTrip"] = 1 / self.properties["sampleRate"]
        trips["weightPersonTrip"] = trips["weightPersonTrip"].astype("float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"person_id": "personID",
                              "orig_purpose": "tripPurposeOrigin",
                              "dest_purpose": "tripPurposeDestination",
                              "stop_period": "departTimeAbmHalfHour",
                              "orig_mgra": "originMGRA",
                              "dest_mgra": "destinationMGRA",
                              "parking_mgra": "parkingMGRA",
                              "parkingCost": "costParking",
                              "av_avail": "avUsed",
                              "trip_board_tap": "boardingTAP",
                              "trip_alight_tap": "alightingTAP",
                              "transponder_avail": "transponderAvailable",
                              "micro_trnAcc": "microMobilityTransitAccess",
                              "micro_trnEgr": "microMobilityTransitEgress"},
                     inplace=True)

        return trips[["tripID",
                      "personID",
                      "tourID",
                      "stopID",
                      "outbound",
                      "tripPurposeOrigin",
                      "tripPurposeDestination",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "microMobilityTransitAccess",
                      "microMobilityTransitEgress",
                      "weightTrip",
                      "weightPersonTrip",
                      "costParking"]]

    @property
    @lru_cache(maxsize=1)
    def joint(self) -> pd.DataFrame:
        """ Create the Joint Model trip list.

        Read in the Joint trip list, map field values, genericize field
        names, append skim values, replicate data-set records for each trip
        participant creating data-set format of one record per participant,
        and assign trip weights accounting for replicated records.

        Returns:
            A Pandas DataFrame of the Joint trip list """
        # load trip list into Pandas DataFrame
        fn_trips = "jointTripData_" + str(self.properties["iterations"]) + ".csv"
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn_trips),
            usecols=["hh_id",
                     "tour_id",
                     "trip_id",
                     "outbound",
                     "orig_purpose",
                     "dest_purpose",
                     "orig_mgra",
                     "dest_mgra",
                     "parking_mgra",
                     "stop_period",
                     "trip_mode",
                     "av_avail",
                     "num_participants",
                     "trip_board_tap",
                     "trip_alight_tap",
                     "set",
                     "valueOfTime",
                     "transponder_avail",
                     "parkingCost"],
            dtype={"hh_id": "int32",
                   "tour_id": "int8",
                   "trip_id": "int8",
                   "outbound": "bool",
                   "orig_purpose": "string",
                   "dest_purpose": "string",
                   "orig_mgra": "int16",
                   "dest_mgra": "int16",
                   "parking_mgra": "int16",
                   "stop_period": "int8",
                   "trip_mode": "int8",
                   "av_avail": "bool",
                   "num_participants": "int8",
                   "trip_board_tap": "int16",
                   "trip_alight_tap": "int16",
                   "set": "int8",
                   "valueOfTime": "float32",
                   "transponder_avail": "bool",
                   "parkingCost": "float32"})

        # apply exhaustive field mappings where applicable
        mappings = {
            "parking_mgra": {key: value for (key, value) in
                             zip(list(range(1, 23003)),
                                 list(range(1, 23003)))},
            "trip_mode": {2: "Shared Ride 2",
                          3: "Shared Ride 3+",
                          4: "Walk",
                          5: "Bike",
                          6: "Walk to Transit",
                          7: "Park and Ride to Transit",
                          8: "Kiss and Ride to Transit",
                          9: "TNC to Transit",
                          10: "Taxi",
                          11: "Non-Pooled TNC",
                          12: "Pooled TNC"},
            "trip_board_tap": {key: value for (key, value) in
                               zip(list(range(1, 99999)),
                                   list(range(1, 99999)))},
            "trip_alight_tap": {key: value for (key, value) in
                                zip(list(range(1, 99999)),
                                    list(range(1, 99999)))},
            "set": {0: "Local Bus",
                    1: "Premium Transit",
                    2: "Local Bus and Premium Transit"},
        }

        for field in mappings:
            if field in ["parking_mgra", "trip_board_tap", "trip_alight_tap"]:
                data_type = "float32"
            else:
                data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # create tour surrogate key (hh_id, tour_id)
        tour_key = ["hh_id", "tour_id"]
        trips["tourID"] = pd.Series(
            trips.groupby(tour_key).ngroup() + 1,
            dtype="int32")

        # create tour stop surrogate key (outbound, trip_id)
        stop_key = ["outbound", "trip_id"]
        trips["stopID"] = pd.Series(
            trips.sort_values(by=stop_key).groupby(tour_key).cumcount() + 1,
            dtype="int8")

        # create unique trip surrogate key
        trips = trips.sort_values(by=tour_key + stop_key).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        trips = trips.merge(taz_info, left_on="orig_mgra", right_on="MGRA")
        trips.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        trips = trips.merge(taz_info, left_on="dest_mgra", right_on="MGRA")
        trips.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        trips = trips.merge(taz_info, how="left", left_on="parking_mgra",
                            right_on="MGRA")
        trips.rename(columns={"TAZ": "parkingTAZ"}, inplace=True)

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.stop_period
        )

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(
            mode=trips.trip_mode,
            transit_set=trips.set
        )

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(
            vot=trips.valueOfTime
        )

        # transform parking cost from cents to dollars
        trips["parkingCost"] = round(trips.parkingCost / 100, 2)

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"orig_purpose": "tripPurposeOrigin",
                              "dest_purpose": "tripPurposeDestination",
                              "stop_period": "departTimeAbmHalfHour",
                              "orig_mgra": "originMGRA",
                              "dest_mgra": "destinationMGRA",
                              "parking_mgra": "parkingMGRA",
                              "parkingCost": "costParking",
                              "av_avail": "avUsed",
                              "trip_board_tap": "boardingTAP",
                              "trip_alight_tap": "alightingTAP",
                              "transponder_avail": "transponderAvailable"},
                     inplace=True)

        # load tour list into Pandas DataFrame
        fn_tours = "jointTourData_" + str(
            self.properties["iterations"]) + ".csv"
        tours = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn_tours),
            usecols=["hh_id",
                     "tour_id",
                     "tour_participants"],
            dtype={"hh_id": "int32",
                   "tour_id": "int8",
                   "tour_participants": "string"})

        # split the tour participants column by " " and append in wide-format
        # to each record
        tours = pd.concat(
            [tours[["hh_id", "tour_id"]],
             tours["tour_participants"].str.split(" ", expand=True)],
            axis=1
        )

        # melt the wide-format tour participants to long-format
        tours = pd.melt(tours, id_vars=["hh_id", "tour_id"],
                        value_name="person_num")
        tours = tours[tours["person_num"].notnull()]
        tours["person_num"] = tours["person_num"].astype("int8")

        # load output person data into Pandas DataFrame
        fn_persons = "personData_" + str(self.properties["iterations"]) + ".csv"
        persons = pd.read_csv(
            os.path.join(self.scenario_path, "output", fn_persons),
            usecols=["hh_id",
                     "person_num",
                     "person_id"],
            dtype={"hh_id": "int32",
                   "person_num": "int8",
                   "person_id": "int32"})
        persons.rename(columns={"person_id": "personID"}, inplace=True)

        # merge persons with the long-format tour participants to get the person id
        tours = tours.merge(persons, on=["hh_id", "person_num"])

        # merge long-format tour participants with the trip list
        # this many-to-one merge replicates trip records for each participant
        # as well as appending the person id to each replicated record
        trips = trips.merge(tours, on=["hh_id", "tour_id"])

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        # each record is per-person on trip
        # data-set has single person record with multiple trip records
        trips["weightTrip"] = pd.Series(
            1 / (trips["num_participants"] * self.properties["sampleRate"]),
            dtype="float32")
        trips["weightPersonTrip"] = 1 / self.properties["sampleRate"]
        trips["weightPersonTrip"] = trips["weightPersonTrip"].astype("float32")

        return trips[["tripID",
                      "personID",
                      "tourID",
                      "stopID",
                      "outbound",
                      "tripPurposeOrigin",
                      "tripPurposeDestination",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip",
                      "costParking"]]

    @property
    @lru_cache(maxsize=1)
    def truck(self) -> pd.DataFrame:
        """ Create the Truck Model trip list.

        Read in the External-External trip last, map field values, and
        genericize field names.

        Returns:
            A Pandas DataFrame of the External-External trips list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "report", "trucktrip.csv"),
            usecols=["OTAZ",
                     "DTAZ",
                     "TOD",
                     "MODE",
                     "TRIPS",
                     "TIME",
                     "DIST",
                     "AOC",
                     "TOLLCOST"],
            dtype={"OTAZ": "int16",
                   "DTAZ": "int16",
                   "TOD": "string",
                   "MODE": "string",
                   "TRIPS": "float32",
                   "TIME": "float32",
                   "DIST": "float32",
                   "AOC": "float32",
                   "TOLLCOST": "float32"})

        # create trip surrogate key
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # apply exhaustive field mappings where applicable
        mappings = {
            "TOD": {"EA": 1,
                    "AM": 2,
                    "MD": 3,
                    "PM": 4,
                    "EV": 5},
            "MODE": {"lhdn": "Light Heavy Duty Truck",
                     "lhdt": "Light Heavy Duty Truck",
                     "mhdn": "Medium Heavy Duty Truck",
                     "mhdt": "Medium Heavy Duty Truck",
                     "hhdn": "Heavy Heavy Duty Truck",
                     "hhdt": "Heavy Heavy Duty Truck"}
        }

        for field in mappings:
            if field == "TOD":
                trips[field] = trips[field].map(mappings[field]).astype("int8")
            else:
                trips[field] = trips[field].map(mappings[field]).astype("category")

        # convert cents-based cost fields to dollars
        trips["AOC"] = trips["AOC"] / 100
        trips["TOLLCOST"] = trips["TOLLCOST"] / 100

        # all trips are High value of time
        # no trips use transponders or autonomous vehicles
        trips["valueOfTimeCategory"] = "High"
        trips["transponderAvailable"] = False
        trips["avUsed"] = False

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        trips["weightTrip"] = trips["TRIPS"] / self.properties["sampleRate"]
        trips["weightPersonTrip"] = trips["TRIPS"] / self.properties["sampleRate"]

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"OTAZ": "originTAZ",
                              "DTAZ": "destinationTAZ",
                              "TOD": "departTimeFiveTod",
                              "MODE": "tripMode",
                              "TIME": "timeDrive",
                              "DIST": "distanceDrive",
                              "AOC": "costOperatingDrive",
                              "TOLLCOST": "costTollDrive"},
                     inplace=True)

        # create total time/distance/cost columns
        trips["timeTotal"] = trips["timeDrive"]
        trips["distanceTotal"] = trips["distanceDrive"]
        trips["costTotal"] = trips["costTollDrive"] + trips["costOperatingDrive"]

        return trips[["tripID",
                      "departTimeFiveTod",
                      "originTAZ",
                      "destinationTAZ",
                      "tripMode",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip",
                      "timeDrive",
                      "distanceDrive",
                      "costTollDrive",
                      "costOperatingDrive",
                      "timeTotal",
                      "distanceTotal",
                      "costTotal"]]

    @property
    @lru_cache(maxsize=1)
    def visitor(self) -> pd.DataFrame:
        """ Create the Visitor Model trip list.

        Read in the Visitor trip list, map field values, and genericize
        field names.

        Returns:
            A Pandas DataFrame of the Visitor trip list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "visitorTrips.csv"),
            usecols=["tourID",
                     "tripID",
                     "originPurp",
                     "destPurp",
                     "originMGRA",
                     "destinationMGRA",
                     "outbound",
                     "period",
                     "tripMode",
                     "avAvailable",
                     "boardingTap",
                     "alightingTap",
                     "set",
                     "valueOfTime",
                     "partySize",
                     "micro_walkMode",
                     "micro_trnAcc",
                     "micro_trnEgr",
                     "parkingCost"],
            dtype={"tourID": "int32",
                   "tripID": "int8",
                   "originPurp": "int8",
                   "destPurp": "int8",
                   "originMGRA": "int16",
                   "destinationMGRA": "int16",
                   "outbound": "boolean",
                   "period": "int8",
                   "tripMode": "int8",
                   "avAvailable": "bool",
                   "boardingTap": "int16",
                   "alightingTap": "int16",
                   "set": "int8",
                   "valueOfTime": "float32",
                   "partySize": "int8",
                   "micro_walkMode": "int8",
                   "micro_trnAcc": "int8",
                   "micro_trnEgr": "int8",
                   "parkingCost": "float32"})

        # apply exhaustive field mappings where applicable
        mappings = {
            "originPurp": {-1: "Unknown",
                           0: "Work",
                           1: "Recreation",
                           2: "Dining"},
            "destPurp": {-1: "Unknown",
                         0: "Work",
                         1: "Recreation",
                         2: "Dining"},
            "tripMode": {1: "Drive Alone",
                         2: "Shared Ride 2",
                         3: "Shared Ride 3+",
                         4: "Walk",
                         5: "Bike",
                         6: "Walk to Transit",
                         7: "Park and Ride to Transit",
                         8: "Kiss and Ride to Transit",
                         9: "TNC to Transit",
                         10: "Taxi",
                         11: "Non-Pooled TNC",
                         12: "Pooled TNC"},
            "boardingTap": {key: value for (key, value) in
                            zip(list(range(1, 99999)),
                                list(range(1, 99999)))},
            "alightingTap": {key: value for (key, value) in
                             zip(list(range(1, 99999)),
                                 list(range(1, 99999)))},
            "set": {0: "Local Bus",
                    1: "Premium Transit",
                    2: "Local Bus and Premium Transit"},
            "micro_walkMode": {1: "Walk",
                               2: "Micro-Mobility",
                               3: "Micro-Transit"},
            "micro_trnAcc": {1: "Walk",
                             2: "Micro-Mobility",
                             3: "Micro-Transit"},
            "micro_trnEgr": {1: "Walk",
                             2: "Micro-Mobility",
                             3: "Micro-Transit"}
        }

        for field in mappings:
            if field in ["boardingTap", "alightingTap"]:
                data_type = "float32"
            else:
                data_type = "category"
            trips[field] = trips[field].map(mappings[field]).astype(data_type)

        # create unique trip surrogate key
        # the tripID field included in the data-set is a stopID
        trips.rename(columns={"tripID": "stopID"}, inplace=True)
        trips = trips.sort_values(by=["tourID", "stopID"]).reset_index(drop=True)
        trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

        # add TAZ information in addition to MGRA information
        taz_info = self.mgra_xref[["MGRA", "TAZ"]]

        trips = trips.merge(taz_info, left_on="originMGRA", right_on="MGRA")
        trips.rename(columns={"TAZ": "originTAZ"}, inplace=True)

        trips = trips.merge(taz_info, left_on="destinationMGRA", right_on="MGRA")
        trips.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

        # map abm half hours to abm five time of day
        trips["departTimeFiveTod"] = self._map_time_periods(
            abm_half_hour=trips.period
        )

        # concatenate mode and transit skim set for transit trips
        trips["tripMode"] = self._combine_mode_set(
            mode=trips.tripMode,
            transit_set=trips.set
        )

        # set appropriate walk mode for walk trips
        trips["tripMode"] = self._combine_mode_walk(
            mode=trips.tripMode,
            walk_mode=trips.micro_walkMode
        )

        # calculate value of time category auto skim set used
        trips["valueOfTimeCategory"] = self._map_vot_categories(
            vot=trips.valueOfTime
        )

        # transform parking cost from cents to dollars
        trips["parkingCost"] = round(trips.parkingCost / 100, 2)

        # no visitor trips use transponders
        # no visitor trips are allowed to park in another MGRA
        trips["transponderAvailable"] = False
        trips["parkingMGRA"] = pd.Series(np.nan, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.nan, dtype="float32")

        # add vehicle/trip-based weight and person-based weight
        # adjust by the ABM scenario final iteration sample rate
        trips["weightTrip"] = 1 / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")
        trips["weightPersonTrip"] = pd.Series(
            trips["partySize"] / self.properties["sampleRate"],
            dtype="float32")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"originPurp": "tripPurposeOrigin",
                              "destPurp": "tripPurposeDestination",
                              "period": "departTimeAbmHalfHour",
                              "parkingCost": "costParking",
                              "avAvailable": "avUsed",
                              "boardingTap": "boardingTAP",
                              "alightingTap": "alightingTAP",
                              "micro_trnAcc": "microMobilityTransitAccess",
                              "micro_trnEgr": "microMobilityTransitEgress"},
                     inplace=True)

        return trips[["tripID",
                      "tourID",
                      "stopID",
                      "outbound",
                      "tripPurposeOrigin",
                      "tripPurposeDestination",
                      "departTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "boardingTAP",
                      "alightingTAP",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "microMobilityTransitAccess",
                      "microMobilityTransitEgress",
                      "weightTrip",
                      "weightPersonTrip",
                      "costParking"]]

    @property
    @lru_cache(maxsize=1)
    def zombie_av(self) -> pd.DataFrame:
        """ Create the 0-Passenger Autonomous Vehicle trip list.

        Read in the Autonomous Vehicle trip list, select only 0-passenger
        trips, map field values, and genericize field names.

        Returns:
            A Pandas DataFrame of the 0-Passenger Autonomous Vehicle trip
            list """
        fn = os.path.join(self.scenario_path, "output", "householdAVTrips.csv")
        # file does not exist if AV-component of model is turned off
        if os.path.isfile(fn):
            # load trip list into Pandas DataFrame
            trips = pd.read_csv(
                fn,
                usecols=["hh_id",
                         "veh_id",
                         "vehicleTrip_id",
                         "orig_mgra",
                         "dest_gra",
                         "period",
                         "occupants",
                         "originIsHome",
                         "destinationIsHome",
                         "originIsRemoteParking",
                         "destinationIsRemoteParking",
                         "remoteParkingCostAtDest"],
                dtype={"hh_id": "int32",
                       "veh_id": "int32",
                       "vehicleTrip_id": "int32",
                       "orig_mgra": "int32",
                       "dest_gra": "int32",
                       "period": "int32",
                       "occupants": "int32",
                       "originIsHome": "bool",
                       "destinationIsHome": "bool",
                       "originIsRemoteParking": "bool",
                       "destinationIsRemoteParking": "bool",
                       "remoteParkingCostAtDest": "float32"}
            )

            # filter trip list to empty/zombie av trips
            trips = trips.loc[trips["occupants"] == 0].copy()

            # create unique trip surrogate key
            trip_key = ["hh_id", "veh_id", "vehicleTrip_id"]
            trips = trips.sort_values(by=trip_key).reset_index(drop=True)
            trips["tripID"] = pd.Series(trips.index + 1, dtype="int32")

            # load output household transponder ownership data
            hh_fn = "householdData_" + str(self.properties["iterations"]) + ".csv"
            hh = pd.read_csv(
                os.path.join(self.scenario_path, "output", hh_fn),
                usecols=["hh_id", "transponder"],
                dtype={"hh_id": "int32",
                       "transponder": "bool"})

            # if household has a transponder then all trips can use it
            trips = trips.merge(hh, on="hh_id")

            # add TAZ information in addition to MGRA information
            taz_info = self.mgra_xref[["MGRA", "TAZ"]]

            trips = trips.merge(taz_info, left_on="orig_mgra", right_on="MGRA")
            trips.rename(columns={"TAZ": "originTAZ"}, inplace=True)

            trips = trips.merge(taz_info, left_on="dest_gra", right_on="MGRA")
            trips.rename(columns={"TAZ": "destinationTAZ"}, inplace=True)

            # map abm half hours to abm five time of day
            trips["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=trips.period)

            # map abm half hours to abm five time of day
            trips["departTimeFiveTod"] = self._map_time_periods(abm_half_hour=trips.period)

            # all zombie AV trips are Drive Alone and High vot
            trips["tripMode"] = "Drive Alone"
            trips["valueOfTimeCategory"] = "High"
            trips["avUsed"] = True
            trips["parkingMGRA"] = np.nan
            trips["parkingTAZ"] = np.nan

            # add person-based weight and adjust weights
            # by the ABM scenario final iteration sample rate
            # no people are in zombie AV trips
            trips["weightTrip"] = 1 / self.properties["sampleRate"]
            trips["weightPersonTrip"] = 0

            # rename columns to standard/generic ABM naming conventions
            trips.rename(columns={"hh_id": "hhID",
                                  "veh_id": "vehID",
                                  "vehicleTrip_id": "vehicleTripID",
                                  "orig_mgra": "originMGRA",
                                  "dest_gra": "destinationMGRA",
                                  "period": "departTimeAbmHalfHour",
                                  "transponder": "transponderAvailable",
                                  "remoteParkingCostAtDest": "costParking"},
                         inplace=True)

            return trips[["tripID",
                          "hhID",
                          "vehID",
                          "vehicleTripID",
                          "originIsHome",
                          "destinationIsHome",
                          "originIsRemoteParking",
                          "destinationIsRemoteParking",
                          "departTimeAbmHalfHour",
                          "departTimeFiveTod",
                          "originMGRA",
                          "destinationMGRA",
                          "parkingMGRA",
                          "originTAZ",
                          "destinationTAZ",
                          "parkingTAZ",
                          "tripMode",
                          "valueOfTimeCategory",
                          "transponderAvailable",
                          "avUsed",
                          "weightTrip",
                          "weightPersonTrip",
                          "costParking"]]
        else:  # return empty DataFrame if file does not exist
            return(pd.DataFrame(
                columns=["tripID",
                         "hhID",
                         "vehID",
                         "vehicleTripID",
                         "originIsHome",
                         "destinationIsHome",
                         "originIsRemoteParking",
                         "destinationIsRemoteParking",
                         "parkingChoiceAtDestination",
                         "departTimeAbmHalfHour",
                         "departTimeFiveTod",
                         "originMGRA",
                         "destinationMGRA",
                         "parkingMGRA",
                         "originTAZ",
                         "destinationTAZ",
                         "parkingTAZ",
                         "tripMode",
                         "valueOfTimeCategory",
                         "transponderAvailable",
                         "avUsed",
                         "weightTrip",
                         "weightPersonTrip"]))

    @property
    @lru_cache(maxsize=1)
    def zombie_tnc(self) -> pd.DataFrame:
        """ Create the 0-Passenger TNC trip list.

        Read in the TNC Vehicle trip list, select only 0-passenger
        trips, map field values, and genericize field names.

        Returns:
            A Pandas DataFrame of the 0-Passenger TNC Vehicle trip list """
        # load trip list into Pandas DataFrame
        trips = pd.read_csv(
            os.path.join(self.scenario_path, "output", "TNCTrips.csv"),
            usecols=["trip_ID",
                     "originMgra",
                     "destinationMgra",
                     "originTaz",
                     "destinationTaz",
                     "totalPassengers",
                     "startPeriod",
                     "endPeriod",
                     " originPurpose",
                     " destinationPurpose"],
            dtype={"trip_ID": "int32",
                   "originMgra": "int16",
                   "destinationMgra": "int16",
                   "originTaz": "int16",
                   "destinationTaz": "int16",
                   "totalPassengers": "int8",
                   "startPeriod": "int16",
                   "endPeriod": "int16",
                   " originPurpose": "int8",
                   " destinationPurpose": "int8"})

        # filter trip list to empty/zombie tnc trips
        trips = trips.loc[trips["totalPassengers"] == 0].copy()
        trips.reset_index(drop=True, inplace=True)

        # apply exhaustive field mappings where applicable
        mappings = {
            " originPurpose": {0: "Home",
                               1: "Pickup Only",
                               2: "Drop-off Only",
                               3: "Pickup and Drop-off",
                               4: "Refuel"},
            " destinationPurpose": {0: "Home",
                                    1: "Pickup Only",
                                    2: "Drop-off Only",
                                    3: "Pickup and Drop-off",
                                    4: "Refuel"}
        }

        for field in mappings:
            trips[field] = trips[field].map(mappings[field]).astype("category")

        # only map TNC time periods to ABM time periods if they nest
        period_width = self.properties["timePeriodWidthTNC"]
        if 30 % period_width == 0:
            # map TNC time periods to actual period start times
            # take the defined width of the time periods multiplied
            # by the time period number as the minutes after 3am allowing
            # the time periods to wrap around 12am and set the time value
            trips["StartTime"] = trips["startPeriod"].apply(
                lambda x: (datetime.combine(date.today(), time(3, 0)) +
                           timedelta(minutes=(x-1) * period_width)).time())
            trips["EndTime"] = trips["endPeriod"].apply(
                lambda x: (datetime.combine(date.today(), time(3, 0)) +
                           timedelta(minutes=(x-1) * period_width)).time())

            # map continuous times to abm half hour periods
            depart_half_hour = [
                [p["period"] for p in self.time_periods["abmHalfHour"]
                 if p["startTime"] <= x < p["endTime"]]
                for x in trips["StartTime"]]
            depart_half_hour = [val for sublist in depart_half_hour for val in sublist]
            trips = trips.assign(departTimeAbmHalfHour=depart_half_hour)
            trips["departTimeAbmHalfHour"] = trips["departTimeAbmHalfHour"].astype("int8")

            arrive_half_hour = [
                [p["period"] for p in self.time_periods["abmHalfHour"]
                 if p["startTime"] <= x < p["endTime"]]
                for x in trips["EndTime"]]
            arrive_half_hour = [val for sublist in arrive_half_hour for val in sublist]
            trips = trips.assign(arriveTimeAbmHalfHour=arrive_half_hour)
            trips["arriveTimeAbmHalfHour"] = trips["arriveTimeAbmHalfHour"].astype("int8")

            # map abm half hours to abm five time of day
            trips["departTimeFiveTod"] = self._map_time_periods(
                abm_half_hour=trips.departTimeAbmHalfHour)

            trips["arriveTimeFiveTod"] = self._map_time_periods(
                abm_half_hour=trips.arriveTimeAbmHalfHour)
        else:
            # if time periods are not able to nest within ABM model time periods
            # set the ABM model time period fields to NaN
            # RSG has been made aware of this issue
            trips["departTimeAbmHalfHour"] = pd.Series(np.nan, dtype="float32")
            trips["departTimeFiveTod"] = pd.Series(np.nan, dtype="float32")
            trips["arriveTimeAbmHalfHour"] = pd.Series(np.nan, dtype="float32")
            trips["arriveTimeFiveTod"] = pd.Series(np.nan, dtype="float32")

        # all zombie TNC trips are Drive Alone and High vot
        # assumed Transponder ownership for Drive Alone TNC
        trips["tripMode"] = "Drive Alone"
        trips["tripMode"] = trips["tripMode"].astype("category")
        trips["valueOfTimeCategory"] = "High"
        trips["valueOfTimeCategory"] = trips["valueOfTimeCategory"].astype("category")
        trips["transponderAvailable"] = True
        trips["avUsed"] = False
        trips["parkingMGRA"] = pd.Series(np.nan, dtype="float32")
        trips["parkingTAZ"] = pd.Series(np.nan, dtype="float32")

        # add person-based weight and adjust weights
        # by the ABM scenario final iteration sample rate
        # no people are in zombie AV trips
        trips["weightTrip"] = 1 / self.properties["sampleRate"]
        trips["weightTrip"] = trips["weightTrip"].astype("float32")
        trips["weightPersonTrip"] = 0
        trips["weightPersonTrip"] = trips["weightPersonTrip"].astype("int8")

        # rename columns to standard/generic ABM naming conventions
        trips.rename(columns={"trip_ID": "tripID",
                              "originMgra": "originMGRA",
                              "destinationMgra": "destinationMGRA",
                              "originTaz": "originTAZ",
                              "destinationTaz": "destinationTAZ",
                              " originPurpose": "originPurpose",
                              " destinationPurpose": "destinationPurpose"},
                     inplace=True)

        return trips[["tripID",
                      "originPurpose",
                      "destinationPurpose",
                      "departTimeAbmHalfHour",
                      "arriveTimeAbmHalfHour",
                      "departTimeFiveTod",
                      "arriveTimeFiveTod",
                      "originMGRA",
                      "destinationMGRA",
                      "parkingMGRA",
                      "originTAZ",
                      "destinationTAZ",
                      "parkingTAZ",
                      "tripMode",
                      "valueOfTimeCategory",
                      "transponderAvailable",
                      "avUsed",
                      "weightTrip",
                      "weightPersonTrip"]]
