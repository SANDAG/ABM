import sys
import geopandas
import numpy as np
import os
import pandas as pd
from shapely import wkt


def export_highway_shape(scenario_path: str) -> geopandas.GeoDataFrame:
    """ Takes an input path to a completed ABM scenario model run, reads the
    input and loaded highway networks from the report folder, and outputs a
    geography shape file to the report folder of the loaded highway network.

    Args:
        scenario_path: String location of the completed ABM scenario folder

    Returns:
        A GeoPandas GeoDataFrame of the loaded highway network """
        
    # temporary so that the sensitivity summary on data lake works
    # the sensitivity summary on data lake uses IFC (from TCOV) rather than FC (from TNED)
    hwy_tcad = pd.read_csv(os.path.join(scenario_path, "report", "hwyTcad.csv"))
    hwy_tcad['IFC'] = hwy_tcad['FC']
    hwy_tcad.to_csv(os.path.join(scenario_path, "report", "hwyTcad.csv"), index=False)    
    
    # read in input highway network
    hwy_tcad = pd.read_csv(os.path.join(scenario_path, "report", "hwyTcad.csv"),
                           usecols=["ID",  # highway coverage id
                                    "NM",  # link name
                                    "Length",  # link length in miles
                                    "COJUR",  # count jurisdiction code
                                    "COSTAT",  # count station number
                                    "COLOC",  # count location code
                                    "FC",  # initial functional class
                                    "HOV",  # link operation type
                                    "EATRUCK",  # truck restriction code - Early AM
                                    "AMTRUCK",  # truck restriction code - AM Peak
                                    "MDTRUCK",  # truck restriction code - Midday
                                    "PMTRUCK",  # truck restriction code - PM Peak
                                    "EVTRUCK",  # truck restriction code - Evening
                                    "SPD",  # posted speed limit
                                    "WAY",  # one or two way operations
                                    "MED",  # median type
                                    "AN",  # A node number
                                    "FXNM",  # cross street name at from end of link
                                    "BN",  # B node number
                                    "TXNM",  # cross street name at to end of link
                                    "ABLN_EA",  # lanes - from-to - Early AM
                                    "ABLN_AM",  # lanes - from-to - AM Peak
                                    "ABLN_MD",  # lanes - from-to - Midday
                                    "ABLN_PM",  # lanes - from-to - PM Peak
                                    "ABLN_EV",  # lanes - from-to - Evening
                                    "BALN_EA",  # lanes - to-from - Early AM
                                    "BALN_AM",  # lanes - to-from - AM Peak
                                    "BALN_MD",  # lanes - to-from - Midday
                                    "BALN_PM",  # lanes - to-from - PM Peak
                                    "BALN_EV",  # lanes - to-from - Evening
                                    "ABPRELOAD_EA",  # preloaded bus flow - to-from - Early AM
                                    "BAPRELOAD_EA",  # preloaded bus flow - from-to - Early AM
                                    "ABPRELOAD_AM",  # preloaded bus flow - to-from - AM Peak
                                    "BAPRELOAD_AM",  # preloaded bus flow - from-to - AM Peak
                                    "ABPRELOAD_MD",  # preloaded bus flow - to-from - Midday
                                    "BAPRELOAD_MD",  # preloaded bus flow - from-to - Midday
                                    "ABPRELOAD_PM",  # preloaded bus flow - to-from - PM Peak
                                    "BAPRELOAD_PM",  # preloaded bus flow - from-to - PM Peak
                                    "ABPRELOAD_EV",  # preloaded bus flow - to-from - Evening
                                    "BAPRELOAD_EV",  # preloaded bus flow - from-to - Evening
                                    "geometry"])  # WKT geometry

    # read in loaded highway network for each time period
    for tod in ["EA", "AM", "MD", "PM", "EV"]:
        fn = "hwyload_" + tod + ".csv"

        file = pd.read_csv(os.path.join(scenario_path, "report", fn),
                           usecols=["ID1",  # highway coverage id
                                    "AB_Time",  # a-b loaded travel time
                                    "BA_Time",  # b-a loaded travel time
                                    "AB_Speed",  # a-b loaded speed
                                    "BA_Speed",  # b-a loaded speed
                                    "AB_VOC",  # a-b volume to capacity
                                    "BA_VOC",  # b-a volume to capacity
                                    "AB_Flow_SOV_NTPL",
                                    "BA_Flow_SOV_NTPL",
                                    "AB_Flow_SOV_TPL",
                                    "BA_Flow_SOV_TPL",
                                    "AB_Flow_SR2L",
                                    "BA_Flow_SR2L",
                                    "AB_Flow_SR3L",
                                    "BA_Flow_SR3L",
                                    "AB_Flow_SOV_NTPM",
                                    "BA_Flow_SOV_NTPM",
                                    "AB_Flow_SOV_TPM",
                                    "BA_Flow_SOV_TPM",
                                    "AB_Flow_SR2M",
                                    "BA_Flow_SR2M",
                                    "AB_Flow_SR3M",
                                    "BA_Flow_SR3M",
                                    "AB_Flow_SOV_NTPH",
                                    "BA_Flow_SOV_NTPH",
                                    "AB_Flow_SOV_TPH",
                                    "BA_Flow_SOV_TPH",
                                    "AB_Flow_SR2H",
                                    "BA_Flow_SR2H",
                                    "AB_Flow_SR3H",
                                    "BA_Flow_SR3H",
                                    "AB_Flow_lhd",
                                    "BA_Flow_lhd",
                                    "AB_Flow_mhd",
                                    "BA_Flow_mhd",
                                    "AB_Flow_hhd",
                                    "BA_Flow_hhd"])

        # match input highway network to loaded highway network
        # to get preload bus flows
        file = file.merge(right=hwy_tcad,
                          how="inner",
                          left_on="ID1",
                          right_on="ID")

        # calculate aggregated flows
        file["AB_Flow_SOV"] = file[["AB_Flow_SOV_NTPL",
                                    "AB_Flow_SOV_TPL",
                                    "AB_Flow_SOV_NTPM",
                                    "AB_Flow_SOV_TPM",
                                    "AB_Flow_SOV_NTPH",
                                    "AB_Flow_SOV_TPH"]].sum(axis=1)

        file["BA_Flow_SOV"] = file[["BA_Flow_SOV_NTPL",
                                    "BA_Flow_SOV_TPL",
                                    "BA_Flow_SOV_NTPM",
                                    "BA_Flow_SOV_TPM",
                                    "BA_Flow_SOV_NTPH",
                                    "BA_Flow_SOV_TPH"]].sum(axis=1)

        file["AB_Flow_SR2"] = file[["AB_Flow_SR2L",
                                    "AB_Flow_SR2M",
                                    "AB_Flow_SR2H"]].sum(axis=1)

        file["BA_Flow_SR2"] = file[["BA_Flow_SR2L",
                                    "BA_Flow_SR2M",
                                    "BA_Flow_SR2H"]].sum(axis=1)

        file["AB_Flow_SR3"] = file[["AB_Flow_SR3L",
                                    "AB_Flow_SR3M",
                                    "AB_Flow_SR3H"]].sum(axis=1)

        file["BA_Flow_SR3"] = file[["BA_Flow_SR3L",
                                    "BA_Flow_SR3M",
                                    "BA_Flow_SR3H"]].sum(axis=1)

        file["AB_Flow_Truck"] = file[["AB_Flow_lhd",
                                      "AB_Flow_mhd",
                                      "AB_Flow_hhd"]].sum(axis=1)

        file["BA_Flow_Truck"] = file[["BA_Flow_lhd",
                                      "BA_Flow_mhd",
                                      "BA_Flow_hhd"]].sum(axis=1)

        file["AB_Flow_Bus"] = file["ABPRELOAD_" + tod]

        file["BA_Flow_Bus"] = file["BAPRELOAD_" + tod]

        file["AB_Flow"] = file[["AB_Flow_SOV",
                                "AB_Flow_SR2",
                                "AB_Flow_SR3",
                                "AB_Flow_Truck",
                                "AB_Flow_Bus"]].sum(axis=1)

        file["BA_Flow"] = file[["BA_Flow_SOV",
                                "BA_Flow_SR2",
                                "BA_Flow_SR3",
                                "BA_Flow_Truck",
                                "BA_Flow_Bus"]].sum(axis=1)

        # fill NAs with 0s
        na_vars = ["AB_Time",
                   "BA_Time",
                   "AB_Speed",
                   "BA_Speed",
                   "AB_VOC",
                   "BA_VOC"]

        file[na_vars] = file[na_vars].fillna(0)

        # select columns of interest
        file = file[["ID1",
                     "AB_Time",
                     "BA_Time",
                     "AB_Speed",
                     "BA_Speed",
                     "AB_VOC",
                     "BA_VOC",
                     "AB_Flow_SOV",
                     "BA_Flow_SOV",
                     "AB_Flow_SR2",
                     "BA_Flow_SR2",
                     "AB_Flow_SR3",
                     "BA_Flow_SR3",
                     "AB_Flow_lhd",
                     "AB_Flow_mhd",
                     "AB_Flow_hhd",
                     "AB_Flow_Truck",
                     "BA_Flow_lhd",
                     "BA_Flow_mhd",
                     "BA_Flow_hhd",
                     "BA_Flow_Truck",
                     "AB_Flow_Bus",
                     "BA_Flow_Bus",
                     "AB_Flow",
                     "BA_Flow"]]

        # add time of day suffix to column names
        file = file.add_suffix("_" + tod)

        # merge loaded highway network into input highway network
        hwy_tcad = hwy_tcad.merge(right=file,
                                  how="inner",
                                  left_on="ID",
                                  right_on="ID1_" + tod)

    # create string description of [FC] field
    conditions = [hwy_tcad["FC"] == 1,
                  hwy_tcad["FC"] == 2,
                  hwy_tcad["FC"] == 3,
                  hwy_tcad["FC"] == 4,
                  hwy_tcad["FC"] == 5,
                  hwy_tcad["FC"] == 6,
                  hwy_tcad["FC"] == 7,
                  hwy_tcad["FC"] == 8,
                  hwy_tcad["FC"] == 9,
                  hwy_tcad["FC"] == 10]

    choices = ["Freeway",
               "Prime Arterial",
               "Major Arterial",
               "Collector",
               "Local Collector",
               "Rural Collector",
               "Local (non-circulation element) Road",
               "Freeway Connector Ramp",
               "Local Ramp",
               "Zone Connector"]

    hwy_tcad["FC_Desc"] = np.select(conditions, choices, default="")

    # calculate aggregate flows
    hwy_tcad["AB_Flow_SOV"] = hwy_tcad[["AB_Flow_SOV_EA",
                                        "AB_Flow_SOV_AM",
                                        "AB_Flow_SOV_MD",
                                        "AB_Flow_SOV_PM",
                                        "AB_Flow_SOV_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_SOV"] = hwy_tcad[["BA_Flow_SOV_EA",
                                        "BA_Flow_SOV_AM",
                                        "BA_Flow_SOV_MD",
                                        "BA_Flow_SOV_PM",
                                        "BA_Flow_SOV_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_SR2"] = hwy_tcad[["AB_Flow_SR2_EA",
                                        "AB_Flow_SR2_AM",
                                        "AB_Flow_SR2_MD",
                                        "AB_Flow_SR2_PM",
                                        "AB_Flow_SR2_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_SR2"] = hwy_tcad[["BA_Flow_SR2_EA",
                                        "BA_Flow_SR2_AM",
                                        "BA_Flow_SR2_MD",
                                        "BA_Flow_SR2_PM",
                                        "BA_Flow_SR2_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_SR3"] = hwy_tcad[["AB_Flow_SR3_EA",
                                        "AB_Flow_SR3_AM",
                                        "AB_Flow_SR3_MD",
                                        "AB_Flow_SR3_PM",
                                        "AB_Flow_SR3_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_SR3"] = hwy_tcad[["BA_Flow_SR3_EA",
                                        "BA_Flow_SR3_AM",
                                        "BA_Flow_SR3_MD",
                                        "BA_Flow_SR3_PM",
                                        "BA_Flow_SR3_EV"]].sum(axis=1)
    
    hwy_tcad["AB_Flow_lhd"] = hwy_tcad[["AB_Flow_lhd_EA",
                                        "AB_Flow_lhd_AM",
                                        "AB_Flow_lhd_MD",
                                        "AB_Flow_lhd_PM",
                                        "AB_Flow_lhd_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_mhd"] = hwy_tcad[["AB_Flow_mhd_EA",
                                        "AB_Flow_mhd_AM",
                                        "AB_Flow_mhd_MD",
                                        "AB_Flow_mhd_PM",
                                        "AB_Flow_mhd_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_hhd"] = hwy_tcad[["AB_Flow_hhd_EA",
                                        "AB_Flow_hhd_AM",
                                        "AB_Flow_hhd_MD",
                                        "AB_Flow_hhd_PM",
                                        "AB_Flow_hhd_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_Truck"] = hwy_tcad[["AB_Flow_Truck_EA",
                                          "AB_Flow_Truck_AM",
                                          "AB_Flow_Truck_MD",
                                          "AB_Flow_Truck_PM",
                                          "AB_Flow_Truck_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_lhd"] = hwy_tcad[["BA_Flow_lhd_EA",
                                        "BA_Flow_lhd_AM",
                                        "BA_Flow_lhd_MD",
                                        "BA_Flow_lhd_PM",
                                        "BA_Flow_lhd_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_mhd"] = hwy_tcad[["BA_Flow_mhd_EA",
                                        "BA_Flow_mhd_AM",
                                        "BA_Flow_mhd_MD",
                                        "BA_Flow_mhd_PM",
                                        "BA_Flow_mhd_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_hhd"] = hwy_tcad[["BA_Flow_hhd_EA",
                                        "BA_Flow_hhd_AM",
                                        "BA_Flow_hhd_MD",
                                        "BA_Flow_hhd_PM",
                                        "BA_Flow_hhd_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_Truck"] = hwy_tcad[["BA_Flow_Truck_EA",
                                          "BA_Flow_Truck_AM",
                                          "BA_Flow_Truck_MD",
                                          "BA_Flow_Truck_PM",
                                          "BA_Flow_Truck_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_Bus"] = hwy_tcad[["AB_Flow_Bus_EA",
                                        "AB_Flow_Bus_AM",
                                        "AB_Flow_Bus_MD",
                                        "AB_Flow_Bus_PM",
                                        "AB_Flow_Bus_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow_Bus"] = hwy_tcad[["BA_Flow_Bus_EA",
                                        "BA_Flow_Bus_AM",
                                        "BA_Flow_Bus_MD",
                                        "BA_Flow_Bus_PM",
                                        "BA_Flow_Bus_EV"]].sum(axis=1)

    hwy_tcad["AB_Flow_Auto"] = hwy_tcad[["AB_Flow_SOV",
                                         "AB_Flow_SR2",
                                         "AB_Flow_SR3"]].sum(axis=1)

    hwy_tcad["BA_Flow_Auto"] = hwy_tcad[["BA_Flow_SOV",
                                         "BA_Flow_SR2",
                                         "BA_Flow_SR3"]].sum(axis=1)

    hwy_tcad["AB_Flow"] = hwy_tcad[["AB_Flow_EA",
                                    "AB_Flow_AM",
                                    "AB_Flow_MD",
                                    "AB_Flow_PM",
                                    "AB_Flow_EV"]].sum(axis=1)

    hwy_tcad["BA_Flow"] = hwy_tcad[["BA_Flow_EA",
                                    "BA_Flow_AM",
                                    "BA_Flow_MD",
                                    "BA_Flow_PM",
                                    "BA_Flow_EV"]].sum(axis=1)

    hwy_tcad["Flow"] = hwy_tcad[["AB_Flow",
                                 "BA_Flow"]].sum(axis=1)

    # calculate vehicle miles travelled (vmt)
    hwy_tcad["AB_VMT"] = hwy_tcad["AB_Flow"] * hwy_tcad["Length"]
    hwy_tcad["BA_VMT"] = hwy_tcad["BA_Flow"] * hwy_tcad["Length"]
    hwy_tcad["VMT"] = hwy_tcad["Flow"] * hwy_tcad["Length"]

    # calculate vehicle hours travelled (vht)
    hwy_tcad["AB_VHT"] = 1/60 * (hwy_tcad["AB_Time_EA"] * hwy_tcad["AB_Flow_EA"] + \
        hwy_tcad["AB_Time_AM"] * hwy_tcad["AB_Flow_AM"] + \
        hwy_tcad["AB_Time_MD"] * hwy_tcad["AB_Flow_MD"] + \
        hwy_tcad["AB_Time_PM"] * hwy_tcad["AB_Flow_PM"] + \
        hwy_tcad["AB_Time_EV"] * hwy_tcad["AB_Flow_EV"])

    hwy_tcad["BA_VHT"] = 1/60 * (hwy_tcad["BA_Time_EA"] * hwy_tcad["BA_Flow_EA"] + \
        hwy_tcad["BA_Time_AM"] * hwy_tcad["BA_Flow_AM"] + \
        hwy_tcad["BA_Time_MD"] * hwy_tcad["BA_Flow_MD"] + \
        hwy_tcad["BA_Time_PM"] * hwy_tcad["BA_Flow_PM"] + \
        hwy_tcad["BA_Time_EV"] * hwy_tcad["BA_Flow_EV"])

    hwy_tcad["VHT"] = hwy_tcad["AB_VHT"] + hwy_tcad["BA_VHT"]

    # select columns of interest
    hwy_tcad = hwy_tcad[["ID",
                         "NM",
                         "Length",
                         "COJUR",
                         "COSTAT",
                         "COLOC",
                         "FC",
                         "FC_Desc",
                         "HOV",
                         "EATRUCK",
                         "AMTRUCK",
                         "MDTRUCK",
                         "PMTRUCK",
                         "EVTRUCK",
                         "SPD",
                         "WAY",
                         "MED",
                         "AN",
                         "FXNM",
                         "BN",
                         "TXNM",
                         "Flow",
                         "AB_Flow",
                         "BA_Flow",
                         "AB_VMT",
                         "BA_VMT",
                         "VMT",
                         "AB_VHT",
                         "BA_VHT",
                         "VHT",
                         "AB_Flow_EA",
                         "BA_Flow_EA",
                         "AB_Flow_AM",
                         "BA_Flow_AM",
                         "AB_Flow_MD",
                         "BA_Flow_MD",
                         "AB_Flow_PM",
                         "BA_Flow_PM",
                         "AB_Flow_EV",
                         "BA_Flow_EV",
                         "AB_Flow_Auto",
                         "BA_Flow_Auto",
                         "AB_Flow_SOV",
                         "BA_Flow_SOV",
                         "AB_Flow_SR2",
                         "BA_Flow_SR2",
                         "AB_Flow_SR3",
                         "BA_Flow_SR3",
                         "AB_Flow_lhd",
                         "AB_Flow_mhd",
                         "AB_Flow_hhd",
                         "AB_Flow_Truck",
                         "BA_Flow_lhd",
                         "BA_Flow_mhd",
                         "BA_Flow_hhd",
                         "BA_Flow_Truck",
                         "AB_Flow_Bus",
                         "BA_Flow_Bus",
                         "AB_Speed_EA",
                         "BA_Speed_EA",
                         "AB_Speed_AM",
                         "BA_Speed_AM",
                         "AB_Speed_MD",
                         "BA_Speed_MD",
                         "AB_Speed_PM",
                         "BA_Speed_PM",
                         "AB_Speed_EV",
                         "BA_Speed_EV",
                         "AB_Time_EA",
                         "BA_Time_EA",
                         "AB_Time_AM",
                         "BA_Time_AM",
                         "AB_Time_MD",
                         "BA_Time_MD",
                         "AB_Time_PM",
                         "BA_Time_PM",
                         "AB_Time_EV",
                         "BA_Time_EV",
                         "ABLN_EA",
                         "BALN_EA",
                         "ABLN_AM",
                         "BALN_AM",
                         "ABLN_MD",
                         "BALN_MD",
                         "ABLN_PM",
                         "BALN_PM",
                         "ABLN_EV",
                         "BALN_EV",
                         "AB_VOC_EA",
                         "BA_VOC_EA",
                         "AB_VOC_AM",
                         "BA_VOC_AM",
                         "AB_VOC_MD",
                         "BA_VOC_MD",
                         "AB_VOC_PM",
                         "BA_VOC_PM",
                         "AB_VOC_EV",
                         "BA_VOC_EV",
                         "geometry"]]

    # rename fields to match old process field names
    hwy_tcad.rename(columns={"ID": "hwycov_id",
                             "NM": "link_name",
                             "Length": "len_mile",
                             "COJUR": "count_jur",
                             "COSTAT": "count_stat",
                             "COLOC": "count_loc",
                             "FC": "fc",
                             "FC_Desc": "fc_desc",
                             "HOV": "hov",
                             "EATRUCK": "truck_ea",
                             "AMTRUCK": "truck_am",
                             "MDTRUCK": "truck_md",
                             "PMTRUCK": "truck_pm",
                             "EVTRUCK": "truck_ev",
                             "SPD": "post_speed",
                             "WAY": "way",
                             "MED": "med",
                             "AN": "from_node",
                             "FXNM": "from_nm",
                             "BN": "to_node",
                             "TXNM": "to_nm",
                             "Flow": "total_flow",
                             "AB_Flow": "abTotFlow",
                             "BA_Flow": "baTotFlow",
                             "AB_VMT": "ab_vmt",
                             "BA_VMT": "ba_vmt",
                             "VMT": "vmt",
                             "AB_VHT": "ab_vht",
                             "BA_VHT": "ba_vht",
                             "VHT": "vht",
                             "AB_Flow_EA": "ab_ea_flow",
                             "BA_Flow_EA": "ba_ea_flow",
                             "AB_Flow_AM": "ab_am_flow",
                             "BA_Flow_AM": "ba_am_flow",
                             "AB_Flow_MD": "ab_md_flow",
                             "BA_Flow_MD": "ba_md_flow",
                             "AB_Flow_PM": "ab_pm_flow",
                             "BA_Flow_PM": "ba_pm_flow",
                             "AB_Flow_EV": "ab_ev_flow",
                             "BA_Flow_EV": "ba_ev_flow",
                             "AB_Flow_Auto": "abAutoFlow",
                             "BA_Flow_Auto": "baAutoFlow",
                             "AB_Flow_SOV": "abSovFlow",
                             "BA_Flow_SOV": "baSovFlow",
                             "AB_Flow_SR2": "abHov2Flow",
                             "BA_Flow_SR2": "baHov2Flow",
                             "AB_Flow_SR3": "abHov3Flow",
                             "BA_Flow_SR3": "baHov3Flow",
                             "AB_Flow_lhd": "abLhdFlow",
                             "AB_Flow_mhd": "abMhdFlow",
                             "AB_Flow_hhd": "abHhdFlow",
                             "AB_Flow_Truck": "abTrucFlow",
                             "BA_Flow_lhd": "baLhdFlow",
                             "BA_Flow_mhd": "baMhdFlow",
                             "BA_Flow_hhd": "baHhdFlow",
                             "BA_Flow_Truck": "baTrucFlow",
                             "AB_Flow_Bus": "abBusFlow",
                             "BA_Flow_Bus": "baBusFlow",
                             "AB_Speed_EA": "ab_ea_mph",
                             "BA_Speed_EA": "ba_ea_mph",
                             "AB_Speed_AM": "ab_am_mph",
                             "BA_Speed_AM": "ba_am_mph",
                             "AB_Speed_MD": "ab_md_mph",
                             "BA_Speed_MD": "ba_md_mph",
                             "AB_Speed_PM": "ab_pm_mph",
                             "BA_Speed_PM": "ba_pm_mph",
                             "AB_Speed_EV": "ab_ev_mph",
                             "BA_Speed_EV": "ba_ev_mph",
                             "AB_Time_EA": "ab_ea_min",
                             "BA_Time_EA": "ba_ea_min",
                             "AB_Time_AM": "ab_am_min",
                             "BA_Time_AM": "ba_am_min",
                             "AB_Time_MD": "ab_md_min",
                             "BA_Time_MD": "ba_md_min",
                             "AB_Time_PM": "ab_pm_min",
                             "BA_Time_PM": "ba_pm_min",
                             "AB_Time_EV": "ab_ev_min",
                             "BA_Time_EV": "ba_ev_min",
                             "ABLN_EA": "ab_ea_lane",
                             "BALN_EA": "ba_ea_lane",
                             "ABLN_AM": "ab_am_lane",
                             "BALN_AM": "ba_am_lane",
                             "ABLN_MD": "ab_md_lane",
                             "BALN_MD": "ba_md_lane",
                             "ABLN_PM": "ab_pm_lane",
                             "BALN_PM": "ba_pm_lane",
                             "ABLN_EV": "ab_ev_lane",
                             "BALN_EV": "ba_ev_lane",
                             "AB_VOC_EA": "ab_ea_voc",
                             "BA_VOC_EA": "ba_ea_voc",
                             "AB_VOC_AM": "ab_am_voc",
                             "BA_VOC_AM": "ba_am_voc",
                             "AB_VOC_MD": "ab_md_voc",
                             "BA_VOC_MD": "ba_md_voc",
                             "AB_VOC_PM": "ab_pm_voc",
                             "BA_VOC_PM": "ba_pm_voc",
                             "AB_VOC_EV": "ab_ev_voc",
                             "BA_VOC_EV": "ba_ev_voc"},
                    inplace=True)

    # create geometry from WKT geometry field
    hwy_tcad["geometry"] = hwy_tcad["geometry"].apply(wkt.loads)

    # create GeoPandas DataFrame
    hwy_tcad = geopandas.GeoDataFrame(
        hwy_tcad,
        geometry="geometry",
        crs=2230)

    return hwy_tcad

scenario_path = sys.argv[1]
export_highway_shape(scenario_path).to_file(os.path.join(scenario_path, "report", "hwyLoad.shp"))