import pandas as pd
import numpy as np
import openmatrix as omx
import os
import sys
import yaml
import time

class TravelTimeReporter:
    """
    Class for creating report of travel time from origin MGRAs to destination MGRAs for different travel modes

    Parameters
    ----------
    model_run (str):
        Path of model run to analyze
    settings_file (str):
        Path of reporter configuration file
    """
    def __init__(self, model_run, settings_file):

        self.model_run = model_run

        print("Reading settings")
        with open(settings_file, "r") as f:
            self.settings = yaml.safe_load(f)
            f.close()

        print("Reading ActivitySim constants")
        with open(
            os.path.join(
                model_run,
                "src",
                "asim",
                "configs",
                "common",
                "constants.yaml"
            ),
            "r"
        ) as f:
            self.constants = yaml.safe_load(f)
            f.close()

        print("Reading land use")
        self.land_use = pd.read_csv(
            os.path.join(
                model_run,
                "input",
                "land_use.csv"
            )
        ).set_index("mgra")
        self.init_land_use()

        print("Reading skims")
        self.skims = {}
        self.read_skims("transit")
        self.read_skims("traffic")
        
        # Expand HOV3 skims now so they don't have to be expanded twice
        self.skims["HOV3_M_DIST__" + self.settings["time_period"]] = self.expand_skim(self.skims["HOV3_M_DIST__" + self.settings["time_period"]])
        self.skims["HOV3_M_TIME__" + self.settings["time_period"]] = self.expand_skim(self.skims["HOV3_M_TIME__" + self.settings["time_period"]])

        print("Reading walk and bike times")
        self.read_active_skims()

        self.results = None

    # # # # # # # # # # # # INPUT FUNCTIONS # # # # # # # # # # # #
    #=============================================================#
    def read_skims(self, mode = "transit"):
        """
        Reads skims into selected memory. Reads the omx file and then stores desired cores (specified in settings) into a dictionary
        """
        skim_file = os.path.join(
            self.model_run,
            "output",
            "skims",
            "{0}_skims_{1}.omx".format(
                mode,
                self.settings["time_period"]
            )
        )
        skims = omx.open_file(skim_file, "r")
        zones = skims.mapping("zone_number").keys()
        for core in self.settings["{}_skim_matrices".format(mode)]:

            skim_values = np.array(skims[core.format(self.settings["time_period"])])

            # Replace values of zero with self.settings["infinity"] if True
            if self.settings["{}_skim_matrices".format(mode)][core]:
                skim_values = np.where(
                    skim_values == 0,
                    self.settings["infinity"],
                    skim_values
                )

            self.skims[core.format(self.settings["time_period"])] = pd.DataFrame(
                skim_values,
                zones,
                zones,
                )
            
        skims.close()

    def read_active_skims(self):
        """
        Reads active skims into memory as data frames
        """
        for skim_name in self.settings["active_skim_files"]:
            self.skims[skim_name] = pd.read_csv(
                        os.path.join(
                        self.model_run,
                        "output",
                        "skims",
                        self.settings["active_skim_files"][skim_name]
                )
            ).rename(
                columns = {
                    "OMAZ": "i",
                    "DMAZ": "j",
                }
            ).set_index(
                ["i", "j"]
            )

    def init_land_use(self):
        """
        Adds fields to land use table to be used in calculations
        """
        self.land_use["walk_accegr_time"] = 60 * np.minimum(self.land_use["walk_dist_local_bus"], self.land_use["walk_dist_premium_transit"]) / self.constants["walkSpeed"]
        self.land_use["micro_accegr_dist"] = np.minimum(self.land_use["micro_dist_local_bus"], self.land_use["micro_dist_premium_transit"])
        self.land_use["microtransit_access_available"] = (self.land_use["microtransit"] > 0) & (self.land_use["micro_accegr_dist"] <= self.constants["microtransitMaxDist"])
        self.land_use["microtransit_egress_available"] = (self.land_use["microtransit"] > 0) & (self.land_use["micro_accegr_dist"] <= self.constants["microtransitMaxDist"]) & (self.land_use["micro_accegr_dist"] >= self.constants["maxWalkIfMTAccessAvailable"])
        self.land_use["microtransit_accegr_time"] = 60 * self.land_use["micro_accegr_dist"] / self.constants["microtransitSpeed"]
        self.land_use["nev_access_available"] = (self.land_use["nev"] > 0) & (self.land_use["micro_accegr_dist"] <= self.constants["nevMaxDist"])
        self.land_use["nev_egress_available"] = (self.land_use["nev"] > 0) & (self.land_use["micro_accegr_dist"] <= self.constants["nevMaxDist"]) & (self.land_use["micro_accegr_dist"] >= self.constants["maxWalkIfMTAccessAvailable"])
        self.land_use["nev_accegr_time"] = 60 * self.land_use["micro_accegr_dist"] / self.constants["nevSpeed"]

    # # # # # # # # # # # # UTILITY FUNCTIONS # # # # # # # # # # # #
    #===============================================================#
    def field2matrix(self, field, origin = True):
        """
        Creates a matrix out of a land use field

        Parameters
        ----------
        field (str):
            field in self.land_use to convert to matrix
        origin (bool, optional):
            If True, the values at the origin will be the matrix. If false, the values at the destination will be the matrix.

        Returns
        -------
        mtx (pandas.DataFrame):
            MGRA-level matrix
        """
        mtx = pd.DataFrame(
            np.vstack(len(self.land_use)*[self.land_use[field]]),
            self.land_use.index,
            self.land_use.index
        )
        if origin:
            return mtx.T
        else:
            return mtx

    def expand_skim(self, skim):
        """

        Expands a TAZ-TAZ skim into an MGRA-MGRA skim based on the correspondence in the land use table stored internally

        Parameters
        ----------
        skim (pandas.DataFrame):
            Skim matrix where the index and columns are the origins and destination TAZs, respectively, and the values are the impedance from each origin TAZ to the destination TAZ

        Returns
        -------
        expanded_skim (pandas.DataFrame):
            Skim matrix where the index and columns are the origins and destinations MGRAs, respectively, and the values are the impedance from each origin MGRA to the destination MGRA
        """
        expansion_matrix = pd.get_dummies(self.land_use["TAZ"]) # Indicates which MGRAs belong to which TAZ
        return pd.DataFrame(
            expansion_matrix.values.dot(skim.values).dot(expansion_matrix.T.values),
            self.land_use.index,
            self.land_use.index
        )

    def unpivot_skim(self, skim_name):
        """
        Unpivots a skim into a series with the origin and destination as the index

        Parameters
        ----------
        skim_name (str):
            Name of skim to unpivot

        Returns
        -------
        unpivoted_skim (pandas.Series):
            Series with the skim values and the origin and destination as a multi-level index
        """
        time_threshold = self.settings["time_threshold"]
        self.skims[skim_name].index.name = "i"

        return pd.melt(
            self.skims[skim_name].reset_index(),
            id_vars = ["i"],
            var_name = "j",
            value_name = "time"
        ).query(
            "time <= @time_threshold"
            ).sort_values(
                ["i", "j"]
            ).set_index(
                ["i", "j"]
            )["time"]

    # # # # # # # # # # # # CALCULATOR FUNCTIONS # # # # # # # # # # # #
    #==================================================================#
    def get_microtransit_direct_accegr_time(self, access = True, nev = False):
        """
        Returns a matrix with the microtransit access or egress times for each origin and destination MGRA

        Parameters
        ----------
        access (bool):
            Access time if True, egress time if False
        nev (bool):
            True if NEV, false if microtransit

        Returns
        -------
        direct_skim (pandas.DataFrame):
            The direct access or egress time for microtransit or NEV
        """
        direction = "acc" if access else "egr"
        flavor = "nev" if nev else "microtransit"
        print("Getting direct {0} {1} times".format(flavor, direction))

        accegr_time = self.field2matrix(
            flavor + "_accegr_time",
            access
        )

        return accegr_time # Congested time not needed as those matrices aren't actually congested time

    def get_total_microtransit_accegr_times(self, direct_times, nev = False):
        """
        Returns the total times by adding wait time and diversion to direct access or egress times by microtransit/NEV

        Parameters
        ----------
        direct_times (pandas.DataFrame):
            Skim of direct access/egress times from one MGRA to the other
        nev (bool):
            True if NEV, false if microtransit
        
        Returns
        -------
        total_times (pandas.DataFrame):
            Skim of total access/egress times from one MGRA to the other
        """
        flavor = "nev" if nev else "microtransit"
        return self.constants[flavor + "WaitTime"] + np.maximum(
            self.constants[flavor + "DiversionConstant"] + direct_times,
            self.constants[flavor + "DiversionFactor"] * direct_times
        )

    def get_accegr_times(self):
        """
        Returns transit access and egress times from every MGRA to every other MGRA
        """
        print("Calculating direct flexible fleet access and egress times")
        microtransit_direct_access_time = self.get_microtransit_direct_accegr_time(access = True, nev = False)
        microtransit_direct_egress_time = self.get_microtransit_direct_accegr_time(access = False, nev = False)
        nev_direct_access_time = self.get_microtransit_direct_accegr_time(access = True, nev = True)
        nev_direct_egress_time = self.get_microtransit_direct_accegr_time(access = False, nev = True)

        print("Calculating flexible fleet diverted access and egress times")
        microtransit_access_time = self.get_total_microtransit_accegr_times(microtransit_direct_access_time, nev = False)
        microtransit_egress_time = self.get_total_microtransit_accegr_times(microtransit_direct_egress_time, nev = False)
        nev_access_time = self.get_total_microtransit_accegr_times(nev_direct_access_time, nev = True)
        nev_egress_time = self.get_total_microtransit_accegr_times(nev_direct_egress_time, nev = True)

        print("Getting flexible fleet availability")
        microtransit_access_available = self.field2matrix("microtransit_access_available", origin = True)
        microtransit_egress_available = self.field2matrix("microtransit_egress_available", origin = False)
        nev_access_available = self.field2matrix("nev_access_available", origin = True)
        nev_egress_available = self.field2matrix("nev_egress_available", origin = False)

        print("Getting walk access and egress times")
        walk_access_time = self.field2matrix("walk_accegr_time", origin = True)
        walk_egress_time = self.field2matrix("walk_accegr_time", origin = False)

        print("Calculating access and egress times")
        self.skims["access_time"] = np.where(
            nev_access_available,
            nev_access_time,
            np.where(
                microtransit_access_available,
                microtransit_access_time,
                walk_access_time
            )
        )
        self.skims["egress_time"] = np.where(
            nev_egress_available,
            nev_egress_time,
            np.where(
                microtransit_egress_available,
                microtransit_egress_time,
                walk_egress_time
            )
        )

    def get_transit_time(self):
        """
        Obtains the total transit time, not including access or egress
        """
        print("Calculating transit times")

        _loc_time = (
            self.skims["WALK_LOC_XFERWALK__" + self.settings["time_period"]] +
            self.skims["WALK_LOC_XFERWAIT__" + self.settings["time_period"]] +
            self.skims["WALK_LOC_TOTALIVTT__" + self.settings["time_period"]]
        )

        _prm_time = (
            self.skims["WALK_PRM_XFERWALK__" + self.settings["time_period"]] +
            self.skims["WALK_PRM_XFERWAIT__" + self.settings["time_period"]] +
            self.skims["WALK_PRM_TOTALIVTT__" + self.settings["time_period"]]
        )

        _mix_time = (
            self.skims["WALK_MIX_XFERWALK__" + self.settings["time_period"]] +
            self.skims["WALK_MIX_XFERWAIT__" + self.settings["time_period"]] +
            self.skims["WALK_MIX_TOTALIVTT__" + self.settings["time_period"]]
        )

        self.skims["transit_time"] = self.expand_skim(
                np.minimum(
                _loc_time,
                _prm_time,
                _mix_time
            )
        )

    def get_total_transit_time(self):
        """
        Calculates the total time based on access, transit, and egress times
        """
        print("Calculating total times")
        self.skims["total_transit_time"] = self.skims["access_time"] + self.skims["transit_time"] + self.skims["egress_time"]

    def get_ff_time(self, nev = False):
        """
        Gets the flexible fleet travel time and appends to the skims

        Parameters
        ----------
        nev (bool):
            NEV if True. Microtransit if False
        """
        flavor = "nev" if nev else "microtransit"
        print("Getting " + flavor + " travel time")
        
        # Create matrices based on if flexible fleets are available at the origin or destination
        orig_service = self.field2matrix(flavor, True)
        dest_service = self.field2matrix(flavor, False)
        
        # Create Boolean matrix that is True if the flexible fleets service is available for the OD pair and False if it is not
        available = pd.DataFrame(
            (orig_service > 0) & (orig_service == dest_service) & (self.skims["HOV3_M_TIME__" + self.settings["time_period"]] < self.constants[flavor + "MaxDist"]),
            self.land_use.index,
            self.land_use.index
        )

        # Calculate the direct flexible fleet travel time
        direct_time = pd.DataFrame(
            np.maximum(
                60*self.skims["HOV3_M_DIST__" + self.settings["time_period"]]/self.constants[flavor + "Speed"],
                self.skims["HOV3_M_TIME__" + self.settings["time_period"]]
            ),
            self.land_use.index,
            self.land_use.index
        )

        # Calculate the diverted flexible fleet time for OD pairs where the service is available and add to dictionary of skim matrices
        self.skims[flavor + "_time"] = pd.DataFrame(
            np.where(
                available,
                np.maximum(
                    self.constants[flavor + "DiversionConstant"] + direct_time,
                    self.constants[flavor + "DiversionFactor"] * direct_time
                ) + self.constants[flavor + "WaitTime"],
                self.settings["infinity"]
            ),
            self.land_use.index,
            self.land_use.index
        )

    def get_drive_alone_time(self):
        """
        Obtains the drive alone time and appends to the skims
        """
        print("Getting drive alone time")
        orig_terminal_time = self.field2matrix("terminal_time", origin = True)
        dest_terminal_time = self.field2matrix("terminal_time", origin = False)
        self.skims["drive_alone_time"] = orig_terminal_time + self.expand_skim(self.skims["SOV_NT_L_TIME__" + self.settings["time_period"]]) + dest_terminal_time

    # # # # # # # # # # # # OUTPUT FUNCTIONS # # # # # # # # # # # #
    #==============================================================#
    def coalesce_results(self):
        """
        Unpivots skims into single data frame to be written out
        """
        print("Coalescing results")
        
        time_threshold = self.settings["time_threshold"]

        self.results = pd.DataFrame(
            {
                "transit": self.unpivot_skim("total_transit_time"),
                "walk": self.skims["walk_time"].query("walkTime <= @time_threshold")["walkTime"],
                "bike": self.skims["bike_time"].query("BIKE_TIME <= @time_threshold")["BIKE_TIME"],
                "microtransit": self.unpivot_skim("microtransit_time"),
                "nev": self.unpivot_skim("nev_time"),
#                "drive_alone": self.unpivot_skim("drive_alone_time")
            }
        ).reset_index().fillna(self.settings["infinity"]).sort_values(
            ["i", "j"]
        )

        _ebikeMaxTime = self.constants["ebikeMaxDist"] / self.constants["ebikeSpeed"] * 60
        _escooterMaxTime = self.constants["escooterMaxDist"] / self.constants["escooterSpeed"] * 60

        _ebikeTime = self.results["bike"] * self.constants["bikeSpeed"] / self.constants["ebikeSpeed"] + self.results["i"].map(self.land_use["MicroAccessTime"]) + self.constants["microRentTime"]
        _escooterTime = self.results["bike"] * self.constants["bikeSpeed"] / self.constants["escooterSpeed"] + self.results["i"].map(self.land_use["MicroAccessTime"]) + self.constants["microRentTime"]

        self.results["ebike"] = np.where(
            _ebikeTime > _ebikeMaxTime,
            self.settings["infinity"],
            _ebikeTime
        )

        self.results["escooter"] = np.where(
            _escooterTime > _escooterMaxTime,
            self.settings["infinity"],
            _escooterTime
        )

    def write_results(self):
        """
        Unpivots the total time skim, removes any travel time above the threshold, and writes the result to CSV
        """
        print("Writing results")

        self.results.to_csv(
                self.settings["outfile"],
                index = False,
            )

    # # # # # # # # # # # # MAIN FUNCTION # # # # # # # # # # # #
    #===========================================================#
    def run(self):
        """
        Runs the travel time calculator
        """
        self.get_accegr_times()
        self.get_transit_time()
        self.get_total_transit_time()
        self.get_ff_time(nev = True)
        self.get_ff_time(nev = False)
#        self.get_drive_alone_time()
        self.coalesce_results()
        self.write_results()

if __name__ == "__main__":

    start_time = time.time()
    model_run = sys.argv[1]
    settings_file = sys.argv[2]
    TravelTimeReporter(model_run, settings_file).run()
    end_time = time.time()

    print("Travel time reporter run in {} seconds".format(round(end_time - start_time, 1)))