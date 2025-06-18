"""
ABM Base Year Validation - 2022
- create a master worksheet for PowerBI Visualizations of ABM base year validation results"
    - [all class flow]
    - [freeway flow]
    - [freeway speed]
    - [transit ridership]
    - [truck flow]
"""

import os
import sys
import numpy as np
import pandas as pd
import geopandas as gpd

import warnings

warnings.filterwarnings("ignore")

# Set Parameters
scenario_path = str(sys.argv[1])
scenYear = int(sys.argv[2])
output_path = scenario_path + "\\analysis\\validation\\"
abmTOD = ["day", "ea", "am", "md", "pm", "ev"]


# Load Input Files
"""
observed traffic data
"""
# all roadway classes
allCounts = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\source_Counts.xlsx",
    sheet_name="all_classes",
)
allCounts["rdClassID"] = (
    allCounts["rdClass"]
    .copy()
    .replace({"Freeway": 1, "Ramp": 2, "Arterial": 3, "Collector": 4})
)

# freeway
# count
fwyCounts = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\source_Counts.xlsx",
    sheet_name="freeway",
)
fwyCounts["traffic_metric"] = "fwyCount"

# speed (Replica 2022)
fwySpeed = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\source_Speed.xlsx"
)
fwySpeed["traffic_metric"] = "fwySpeed"

# transit ridership
obRidership = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\source_Transit Ridership.xlsx"
)
obRidership = obRidership.rename(
    columns={"total_board": "board_day", "total_psgrmile": "psgrmile_day"}
)

# Truck (for CVM)
truckAADT = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\source_Counts.xlsx",
    sheet_name="truck",
)
truckAADT["rdClassID"] = (
    truckAADT["rdClass"]
    .copy()
    .replace({"Freeway": 1, "Ramp": 2, "Arterial": 3, "Collector": 4, "Zone Connectors": 5})
)

"""
model estimated traffic data
"""
# model traffic estimates
hwyload = gpd.read_file(rf"{scenario_path}\report\hwyLoad.shp")

# mapping TNED hwyload schema with TCOV
schema_mapping = {'way':'iway',
                  'spd':'ispd',
                  'hov':'ihov',
                  'med':'imed',
                  'fc':'ifc',
                  'fc_desc':'ifc_desc'}

if any(k in hwyload.columns for k in schema_mapping.keys()):
    hwyload = hwyload.rename(columns=schema_mapping)

    '''
    hwycovid for TNED zone connectors were recalculated. The id starts from 100000. 
    This temporary step is to replace hwycovid of a particular TCOV zone connector (29395),
    which has a match truck count, with its corresponding TNED hwycovid (106765). 
    '''
    truckAADT["hwycovid_2"] = truckAADT["hwycovid_2"].replace(29395, 106765)

# calculate model flow, speed, and vmt by TOD
for tod in abmTOD:
    if tod != "day":
        # create patterns for searching
        flowCol = tod + "_flow"
        speedTodCol2 = tod + "_mph"
        speedTodCol1 = "ab_" + tod + "_mph"

        # sum flow by AB & BA directions
        flowColOut = "flow_" + tod
        hwyload[flowColOut] = hwyload.filter(like=flowCol).sum(axis=1).round(0).astype("int64")

        # average speed by AB & BA directions
        speedColOut = "speed_" + tod
        abmSpdTodCol = hwyload.filter(like=speedTodCol2).columns
        hwyload[speedColOut] = hwyload.apply(
            lambda x: x[abmSpdTodCol].mean() if x.iway == 2 else x[speedTodCol1], axis=1
        )
        hwyload[speedColOut] = hwyload[speedColOut].round(0).astype("int64")

        # sum model vmt by TOD
        vmtColOut = "vmt_" + tod
        hwyload[vmtColOut] = (hwyload[flowColOut] * hwyload["len_mile"]).round(0).astype("int64")

    else:
        pass

# calculate daily flow, speed, and vmt
hwyload["flow_day"] = hwyload[
    hwyload.columns[hwyload.columns.str.contains("flow_")]
].sum(axis=1)
hwyload["speed_day"] = hwyload[
    hwyload.columns[hwyload.columns.str.contains("speed_")]
].mean(axis=1)
hwyload = hwyload.drop("vmt", axis=1)  # drop vmt(daily) derived from data exporter
hwyload["vmt_day"] = hwyload[hwyload.columns[hwyload.columns.str.contains("vmt_")]].sum(
    axis=1
)

# calculate truck flow
hwyload['TruckFlow'] = hwyload[['abTrucFlow', 'baTrucFlow']].sum(axis=1)
hwyload['lhdTruckFlow'] = hwyload[['abLhdFlow','baLhdFlow']].sum(axis=1)
hwyload['mhdTruckFlow'] = hwyload[['abMhdFlow','baMhdFlow']].sum(axis=1)
hwyload['hhdTruckFlow'] = hwyload[['abHhdFlow','baHhdFlow']].sum(axis=1)

# reshape hwyload
hwyCols = hwyload.columns[
    hwyload.columns.str.contains("hwycov_id|flow_|speed_|vmt_|len_mile|TruckFlow")
]
hwyload = hwyload[hwyCols]
hwyload = hwyload.rename(
    columns={
        "hwycov_id": "hwycovid",
        "flow_day": "DAY_Flow",
        "flow_ea": "EA_Flow",
        "flow_am": "AM_Flow",
        "flow_md": "MD_Flow",
        "flow_pm": "PM_Flow",
        "flow_ev": "EV_Flow",
        "speed_day": "DAY_Speed",
        "speed_ea": "EA_Speed",
        "speed_am": "AM_Speed",
        "speed_md": "MD_Speed",
        "speed_pm": "PM_Speed",
        "speed_ev": "EV_Speed",
        "vmt_day": "DAY_Vmt",
        "vmt_ea": "EA_Vmt",
        "vmt_am": "AM_Vmt",
        "vmt_md": "MD_Vmt",
        "vmt_pm": "PM_Vmt",
        "vmt_ev": "EV_Vmt",
    }
)

# model regional daily vmt (includes all hwycov segments)
regionalVMT = pd.DataFrame([hwyload.DAY_Vmt.sum()], columns=["regional_vmt"])

# model estimated transit ridership
""" psgrmile by routes"""
# load transit route
"""
Route_Name: route number (first three digits), direction(1/2), and configuration (last two digits)
"""
transitRoute = pd.read_csv(os.path.join(scenario_path, "report", "transitRoute.csv"))
transitRoute["Route_Name"] = transitRoute["Route_Name"].astype("str")
transitRoute["Route"] = transitRoute["Route_Name"].str[:-3].astype("int64")

# load transit flow
transitFlow = pd.read_csv(os.path.join(scenario_path, "report", "transit_flow.csv"))
transitFlow["StopDist"] = transitFlow.TOMP - transitFlow.FROMMP
transitFlow["Psgrmiles"] = transitFlow.TRANSITFLOW * transitFlow.StopDist
transitFlow = transitFlow.rename(columns={"ROUTE": "Route_ID", "TOD": "Tod"})

# merge transit flow with route table
cols = ["Route_ID", "Route", "Mode", "Tod", "Psgrmiles"]
transitFlow = transitFlow.merge(transitRoute, on="Route_ID")[cols].sort_values("Route")
psgrmileDF = transitFlow.groupby(["Route", "Tod"])["Psgrmiles"].sum().reset_index()
psgrmile_route_pivt = pd.pivot(
    psgrmileDF, values=["Psgrmiles"], index=["Route"], columns="Tod"
).reset_index()

# Format pivoted psgrmileDF
psgrmile_route_pivt = pd.DataFrame(psgrmile_route_pivt.to_records()).drop(
    "index", axis=1
)
psgrmile_route_pivt = psgrmile_route_pivt[["('Route', '')", "('Psgrmiles', 'EA')", "('Psgrmiles', 'AM')",
                                        "('Psgrmiles', 'MD')", "('Psgrmiles', 'PM')", "('Psgrmiles', 'EV')"]]
psgrmile_route_pivt.columns = [
    "route",
    "EA_PsgrMile",
    "AM_PsgrMile",
    "MD_PsgrMile",
    "PM_PsgrMile",
    "EV_PsgrMile",
]
psgrmile_route_pivt["DAY_PsgrMile"] = psgrmile_route_pivt[
    ["EA_PsgrMile", "AM_PsgrMile", "MD_PsgrMile", "PM_PsgrMile", "EV_PsgrMile"]
].sum(axis=1)

""" boardings by routes"""
# Load transit boardings
transitBoard = pd.read_csv(os.path.join(scenario_path, "report", "transit_onoff.csv"))
transitBoard = transitBoard.rename(
    columns={"ROUTE": "Route_ID", "TOD": "Tod", "BOARDINGS": "board"}
)

# Merge transit flow with route table
cols = ["Route_ID", "Route", "Mode", "Tod", "board"]
transitBoard = transitBoard.merge(transitRoute, on="Route_ID")[cols].sort_values(
    "Route"
)

# Aggregate boardings by routes and TOD
boardDF = transitBoard.groupby(["Route", "Tod"])["board"].sum().reset_index()

# Pivot aggregated table
board_route_pivt = pd.pivot(
    boardDF, values=["board"], index=["Route"], columns="Tod"
).reset_index()

# Format pivoted psgrmileDF
board_route_pivt = pd.DataFrame(board_route_pivt.to_records()).drop("index", axis=1)
board_route_pivt = board_route_pivt[["('Route', '')", "('board', 'EA')", "('board', 'AM')",
                                    "('board', 'MD')", "('board', 'PM')", "('board', 'EV')"]]
board_route_pivt.columns = [
    "route",
    "EA_Board",
    "AM_Board",
    "MD_Board",
    "PM_Board",
    "EV_Board",
]
board_route_pivt["DAY_Board"] = board_route_pivt[
    ["EA_Board", "AM_Board", "MD_Board", "PM_Board", "EV_Board"]
].sum(axis=1)

# Combine model ridership
abmRidership = board_route_pivt.merge(psgrmile_route_pivt, on="route")
abmRidership["route"] = abmRidership["route"].astype("str")

"""
FHWA guideline
"""
fhwaDF = pd.read_excel(
    rf"{scenario_path}\analysis\validation\observed_data\FHWA Guideline.xlsx",
    sheet_name="FHWA",
)
header = fhwaDF.columns
exclusions = ["volume", 2000, 8000, 12000, 18000, 30000, 40000, 50000]
for h in header:
    if h in exclusions:
        header = header.drop(h)


# Functions
"""
labels for validations
"""
# freeway
fwyLabel = pd.DataFrame(
    {
        "Class": "freeway",
        "Gap Range": [
            ">=40%",
            "30%~40%",
            "20%~30%",
            "10%~20%",
            "0%~10%",
            "0%~-10%",
            "-10%~-20%",
            "-20%~-30%",
            "-30%~-40%",
            ">-40%",
            "-10%~10%",
            "-20%~20%",
            "-30%~30%",
            "links w/ positive gap",
            "links w/ negative gap",
            "total links",
            "avg positive gap(%)",
            "avg negative gap(%)",
            "overall avg(%)",
        ],
    }
)

# all-class
allClassLabel = pd.DataFrame(
    {
        "Class": "all-class",
        "Gap Range": [
            ">=100%",
            "50%~100%",
            "30%~50%",
            "20%~30%",
            "10%~20%",
            "0%~10%",
            "0%~-10%",
            "-10%~-20%",
            "-20%~-30%",
            "-30%~-50%",
            ">-50%",
            "-10%~10%",
            "-20%~20%",
            "-30%~30%",
            "links w/ positive gap",
            "links w/ negative gap",
            "total links",
            "avg positive gap(%)",
            "avg negative gap(%)",
            "overall avg(%)",
        ],
    }
)

# transit ridership
transitLabel = allClassLabel.copy()
transitLabel["Class"] = "transit"

# truck volumes
truckLabel = allClassLabel.copy()
truckLabel["Class"] = "truck"

"""
Compare with FHWA Guideline
"""


def compare2FHWA(worksheet, fhwaDF, aggregator):
    # SANDAG RMSE% by volume category
    sandagDF = (
        worksheet.groupby([aggregator])
        .apply(
            lambda grp: ((((grp.DAY_Flow - grp.count_day) ** 2).mean()) ** 0.5)
            / grp.count_day.mean()
        )
        .reset_index(name="RMSE%")
    )

    sandagDF = (
        pd.concat([sandagDF.iloc[0:3], sandagDF.iloc[3:11]])
        .reset_index(drop=True)
        .transpose()
    )
    sandagDF = sandagDF.rename(columns=sandagDF.iloc[0]).drop(sandagDF.index[0])
    sandagDF.columns = header
    sandagDF["volume"] = "SANDAG"

    # merge FHWA RMSE% with SANDAG RMSE%
    resultDF = pd.concat([fhwaDF, sandagDF], axis=0).transpose().reset_index()
    resultDF.columns = resultDF.iloc[0]
    resultDF = resultDF[1:]

    return resultDF


"""
Create Gap Summary
"""


def create_gap_range(gapTod, aggregator, worksheet, classLabel):
    # freeway gap summary
    if classLabel.Class.unique()[0] in ["freeway", "truck"]:
        gap40 = (
            worksheet.query(f"{gapTod} >= 40").groupby([aggregator])[aggregator].count()
        )
        gap3040 = (
            worksheet.query(f"30 <= {gapTod} < 40")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap2030 = (
            worksheet.query(f"20 <= {gapTod} < 30")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap1020 = (
            worksheet.query(f"10 <= {gapTod} < 20")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap10 = (
            worksheet.query(f"0 <= {gapTod} < 10")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap10N = (
            worksheet.query(f"-10 <= {gapTod} < 0")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap1020N = (
            worksheet.query(f"-20 <= {gapTod} < -10")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap2030N = (
            worksheet.query(f"-30 <= {gapTod} < -20")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap3040N = (
            worksheet.query(f"-40 <= {gapTod} < -30")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap40N = (
            worksheet.query(f"-40 <= {gapTod}")
            .groupby([aggregator])[aggregator]
            .count()
        )

        gap_range = [
            gap40,
            gap3040,
            gap2030,
            gap1020,
            gap10,
            gap10N,
            gap1020N,
            gap2030N,
            gap3040N,
            gap40N,
        ]

    # all-class and transit gap summary
    elif classLabel.Class.unique()[0] in ["all-class", "transit"]:
        gap100 = (
            worksheet.query(f"{gapTod} >= 100")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap50100 = (
            worksheet.query(f"50 <= {gapTod} < 100")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap3050 = (
            worksheet.query(f"30 <= {gapTod} < 50")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap2030 = (
            worksheet.query(f"20 <= {gapTod} < 30")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap1020 = (
            worksheet.query(f"10 <= {gapTod} < 20")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap10 = (
            worksheet.query(f"0 <= {gapTod} < 10")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap10N = (
            worksheet.query(f"-10 <= {gapTod} < 0")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap1020N = (
            worksheet.query(f"-20 <= {gapTod} < -10")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap2030N = (
            worksheet.query(f"-30 <= {gapTod} < -20")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap3050N = (
            worksheet.query(f"-50 <= {gapTod} < -30")
            .groupby([aggregator])[aggregator]
            .count()
        )
        gap50N = (
            worksheet.query(f"-50 <= {gapTod}")
            .groupby([aggregator])[aggregator]
            .count()
        )

        gap_range = [
            gap100,
            gap50100,
            gap3050,
            gap2030,
            gap1020,
            gap10,
            gap10N,
            gap1020N,
            gap2030N,
            gap3050N,
            gap50N,
        ]

    # convert a list of gap range to dataframe
    gap_rangeDF = pd.DataFrame(gap_range).fillna(0).astype("int64")

    # create gap range summary
    gap_summary = []
    for n in range(-30, 0, 10):
        summary = (
            worksheet.query(rf"{n} <= {gapTod} < {abs(n)}")
            .groupby(aggregator)[aggregator]
            .count()
        )
        gap_summary.append(summary)

        # convert a list of summary to dataframe and reverse rows
        gap_summaryDF = pd.DataFrame(gap_summary).fillna(0).astype("int64")
        gap_summaryDF = gap_summaryDF.iloc[::-1]

    # summarize links with positive / negative gap
    pLinkDF = pd.DataFrame(
        worksheet.query(f"0 <= {gapTod}").groupby(aggregator)[aggregator].count()
    ).transpose()
    nLinkDF = pd.DataFrame(
        worksheet.query(f"0 > {gapTod}").groupby(aggregator)[aggregator].count()
    ).transpose()
    totLinkDF = pd.DataFrame(
        worksheet.groupby(aggregator)[aggregator].count()
    ).transpose()
    linkDF = pd.concat([pLinkDF, nLinkDF, totLinkDF], axis=0, sort=False)

    # average positive / negative gaps %
    avg_p_gap = (
        worksheet[[gapTod, aggregator]]
        .query(f"{gapTod} >= 0")
        .groupby(aggregator)
        .mean()
        .transpose()
    )
    avg_n_gap = (
        worksheet[[gapTod, aggregator]]
        .query(f"{gapTod} < 0")
        .groupby(aggregator)
        .mean()
        .transpose()
    )
    avg_total = worksheet[[gapTod, aggregator]].groupby(aggregator).mean().transpose()
    avg_gapDF = pd.concat([avg_p_gap, avg_n_gap, avg_total], axis=0, sort=False).round(
        2
    )

    # merge all gap-related dataframes
    gap_statDF = (
        pd.concat(
            [gap_rangeDF, gap_summaryDF, linkDF, avg_gapDF],
            axis=0,
            ignore_index=True,
            sort=False,
        )
        .fillna(0)
        .round(0)
        .astype("int64")
    )
    gap_statDF = pd.concat([classLabel, gap_statDF], axis=1)
    gap_statDF = gap_statDF.reset_index(names="gap_vis_order")

    return gap_statDF


"""
Freeway Validation
"""


def freewayVal(modelData, observeData, label):
    # join datasets & calculate statistics
    worksheet = observeData.merge(modelData, on="hwycovid")

    # sort hwycov links in correct sequence and create visualization index
    worksheet = worksheet.sort_values(["nm", "rtno", "lkno"])
    worksheet["vis_order"] = worksheet.index

    # craete output list
    outputList = []

    # loop through each gap of time and create the gap-related attributes
    aggregator = "dir_nm"

    for idx, tod in enumerate(abmTOD):
        # generate observed metrics by TOD
        observeCountTod = "count_" + tod
        observeSpdTod = "speed_" + tod
        observeVmtTod = "vmt_" + tod

        # calculate gap by TOD
        metric = worksheet["traffic_metric"].unique()[0]
        gapCol = "gap_" + tod

        # flow and vmt gap calculation
        if metric == "fwyCount":
            # calculate observed vmt by TOD
            worksheet[observeVmtTod] = (
                worksheet[observeCountTod] * worksheet["len_mile"]
            ).round(0).astype("int64")

            # model volume vs. observed count
            modelFlowTod = tod.upper() + "_Flow"
            worksheet[gapCol] = (
                ((worksheet[modelFlowTod] / worksheet[observeCountTod]) - 1) * 100
            ).round(1)

            # model vmt vs. observed vmt
            modelVmtTod = tod.upper() + "_Vmt"
            vmtGapCol = "vmt_gap_" + tod
            worksheet[vmtGapCol] = (
                ((worksheet[modelVmtTod] / worksheet[observeVmtTod]) - 1) * 100
            ).round(1)

            # get resulting gap summary and append to output list (excludes vmt)
            gapFlowOut = create_gap_range(gapCol, aggregator, worksheet, fwyLabel)
            outputList.append(gapFlowOut)

        # speed gap calculation
        elif metric == "fwySpeed":
            modelSpdTod = tod.upper() + "_Speed"
            observeSpdTod = "speed_" + tod
            worksheet[gapCol] = (
                ((worksheet[modelSpdTod] / worksheet[observeSpdTod]) - 1) * 100
            ).round(1)

            # get resulting gap summary and append to output list
            gapSpdOut = create_gap_range(gapCol, aggregator, worksheet, label)
            outputList.append(gapSpdOut)
        else:
            pass  # need raise error components
    else:
        pass  # need raise error components

    #
    outputList.append(worksheet)
    keys = abmTOD.copy()
    keys.append("input_sheet")
    outputDict = dict(zip(keys, outputList))

    return outputDict


"""
All-Class Validation
"""


def allClassVal(modelData, observeData, label):
    # join datasets & calculate statistics
    worksheet = observeData.merge(modelData, on="hwycovid")
    worksheet = worksheet[
        list(observeData.columns) + ["DAY_Flow", "len_mile", "DAY_Vmt"]
    ]

    # compute observed daily VMT
    worksheet["vmt_day"] = (worksheet["count_day"] * worksheet["len_mile"]).round(0).astype("int64")

    # create visualization index to sort hwycov links in correct sequence
    worksheet["vis_order"] = worksheet.index

    # craete output list
    outputList = []

    # label model volumes by range
    volRange = [
        "0~10,000",
        "10,000~20,000",
        "20,000~30,000",
        "30,000~40,000",
        "40,000~50,000",
        "50,000~60,000",
        "60,000~70,000",
        "70,000~80,000",
        "80,000~90,000",
        "90,000~100,000",
        "> 100,000",
    ]

    for d in range(0, 101, 10):
        if d == 100:
            worksheet.loc[(worksheet["DAY_Flow"] / 1000 >= d), "vcategory"] = volRange[
                int(d / 10)
            ]
        else:
            worksheet.loc[
                (worksheet["DAY_Flow"] / 1000 >= d)
                & (worksheet["DAY_Flow"] / 1000 < d + 10),
                "vcategory",
            ] = volRange[int(d / 10)]

    # create volume range id starting from 1
    for vcat in volRange:
        worksheet.loc[worksheet["vcategory"] == vcat, "vcategory_id"] = (
            volRange.index(vcat) + 1
        )

    # volume vs. count
    gapCol = "gap_day"
    worksheet[gapCol] = (
        ((worksheet["DAY_Flow"] / worksheet["count_day"]) - 1) * 100
    ).round(1)

    # model vmt vs. observed vmt
    worksheet["vmt_gap_day"] = (
        ((worksheet["DAY_Vmt"] / worksheet["vmt_day"]) - 1) * 100
    ).round(1)

    # create several aggregators: roadwayClass, volCategory, city, and pmsa
    aggregators = ["rdClass", "vcategory", "city_nm", "pmsa_nm"]

    for agtr in aggregators:
        resultDF = create_gap_range(gapCol, agtr, worksheet, label)
        outputList.append(resultDF)

        # Compare SANDAG daily volume with FHWA guideline
        if agtr == "vcategory":
            fhwaCP = compare2FHWA(worksheet, fhwaDF, agtr)

    #
    outputList.append(worksheet)
    outputList.append(fhwaCP)
    keys = aggregators.copy()
    keys.append("input_sheet")
    keys.append("FHWA")
    outputDict = dict(zip(keys, outputList))

    return outputDict


"""
Transit Ridership Validation
"""


def transitVal(modelData, observeData, label):
    # join datasets & calculate statistics
    worksheet = observeData.merge(modelData, on="route")
    worksheet["vis_order"] = worksheet.index

    #
    outputList = []
    metrics = ["board", "psgrmile"]
    aggregator = "mode_name"

    for m in metrics:
        # create working dataframes
        metricCols = worksheet.columns[worksheet.columns.str.contains(m, case=False)]
        dfByRoute = worksheet[
            ["route", "mode", "mode_name", "source", "year"] + list(metricCols)
        ]
        dfByMode = (
            worksheet.groupby(["mode", "mode_name"])[metricCols]
            .sum()
            .round(0)
            .reset_index()
        )
        dfByMode.loc["Total"] = dfByMode[metricCols].sum(numeric_only=True, axis=0)
        dfByMode.loc["Total", ["mode", "mode_name"]] = [6, "Total"]

        #
        for tod in abmTOD:
            diffCol = "diff_" + tod
            gapCol = "gap_" + tod

            # set observed & model ridership columns by TOD
            observeRidershipTod = m + "_" + tod
            if m == "board":
                modelRidershipTod = tod.upper() + "_" + m.capitalize()
            elif m == "psgrmile":
                modelRidershipTod = tod.upper() + "_PsgrMile"

            # gap calculations
            """
            gap by route is for day only
            gap by mode is for day + ABM five TODs
            """

            # diff by mode
            dfByMode[diffCol] = (dfByMode[modelRidershipTod] - dfByMode[observeRidershipTod]).round(1)

            # gaps by mode
            dfByMode[gapCol] = (
                ((dfByMode[modelRidershipTod] / dfByMode[observeRidershipTod]) - 1)
                * 100
            ).round(1)

            if tod == "day":
                # gaps by route
                dfByRoute[gapCol] = (
                    (
                        (dfByRoute[modelRidershipTod] / dfByRoute[observeRidershipTod])
                        - 1
                    )
                    * 100
                ).round(1)
                gapSummaryByRouteTod = create_gap_range(
                    gapCol, aggregator, dfByRoute, label
                )
                outputList.append(dfByRoute)
                outputList.append(gapSummaryByRouteTod)

        # format diffs by mode dataframe
        diffOutCols = ["mode", "mode_name"] + list(
            dfByMode.columns[dfByMode.columns.str.contains("diff")]
        )
        dfByModeDiff = dfByMode.reset_index(drop=True)[diffOutCols]
        dfByModeDiff.columns = ["mode", "mode_name", "DAY", "EA", "AM", "MD", "PM", "EV"]
        dfByModeDiff["vis_order"] = dfByModeDiff.index
        outputList.append(dfByModeDiff)

        # format gaps by mode dataframe
        gapOutCols = ["mode", "mode_name"] + list(
            dfByMode.columns[dfByMode.columns.str.contains("gap")]
        )
        dfByModeGap = dfByMode.reset_index(drop=True)[gapOutCols]
        dfByModeGap.columns = ["mode", "mode_name", "DAY", "EA", "AM", "MD", "PM", "EV"]
        dfByModeGap["vis_order"] = dfByModeGap.index
        outputList.append(dfByModeGap)

    # save all outputs in a dictionary
    keys = [
        "board_input",
        "gs_boardByRoute",
        "diff_boardByMode",
        "gs_boardByMode",
        "psgrmile_input",
        "gs_psgrmileByRoute",
        "diff_psgrmileByMode",
        "gs_psgrmileByMode",
    ]
    outputDict = dict(zip(keys, outputList))

    return outputDict


"""
Truck Validation
"""


def truckVal(modelData, observeData, label):
    # join one-way truck aadt with model estimates
    one2one = observeData.query("hwycovid_2.isna()")
    one2one = one2one.merge(modelData[['hwycovid','TruckFlow','lhdTruckFlow','mhdTruckFlow','hhdTruckFlow','len_mile']], on='hwycovid')
    one2one['TruckVmt'] = one2one.len_mile * one2one.TruckFlow

    # join two-way truck aadt with model estimates
    modelCols = ['hwycovid','TruckFlow','lhdTruckFlow','mhdTruckFlow','hhdTruckFlow','len_mile']
    one2many = observeData.query("~hwycovid_2.isna()")
    one2many = one2many.merge(modelData[modelCols], on="hwycovid")
    one2many = one2many.merge(modelData[modelCols], left_on="hwycovid_2", right_on='hwycovid').rename(columns={'hwycovid_x':'hwycovid'})
    one2many['len_mile'] = one2many.len_mile_x + one2many.len_mile_y
    one2many['TruckFlow'] = one2many.TruckFlow_x+ one2many.TruckFlow_y
    one2many['lhdTruckFlow'] = one2many.lhdTruckFlow_x + one2many.lhdTruckFlow_y
    one2many['mhdTruckFlow'] = one2many.mhdTruckFlow_x + one2many.mhdTruckFlow_y
    one2many['hhdTruckFlow'] = one2many.hhdTruckFlow_x + one2many.hhdTruckFlow_y
    one2many['TruckVmt'] = one2many.len_mile * one2many.TruckFlow
    one2many['lhdTruckVmt'] = one2many.len_mile * one2many.lhdTruckFlow
    one2many['mhdTruckVmt'] = one2many.len_mile * one2many.mhdTruckFlow
    one2many['hhdTruckVmt'] = one2many.len_mile * one2many.hhdTruckFlow
    one2many = one2many[one2one.columns]

    # merge one-way and two-way dataframes
    worksheet = pd.concat([one2one, one2many]).sort_values('source').reset_index(drop=True)

    # label truck volumes by range
    worksheet["range"] = np.select(condlist=[worksheet.TruckFlow <= 2500,
                                            (worksheet.TruckFlow > 2500) & (worksheet.TruckFlow <= 5000),
                                            (worksheet.TruckFlow > 5000) & (worksheet.TruckFlow <= 10000),
                                            (worksheet.TruckFlow > 10000)],
                                    choicelist = [  "1:0~2,500",
                                                    "2:2,500~5,000",
                                                    "3:5,000~10,000",
                                                    "4:> 10,000"
                                                ],
                                    default = np.nan
    )
    worksheet["vcategory_id"] = worksheet["range"].str.split(":", expand=True)[0]
    worksheet["vcategory"] = worksheet["range"].str.split(":", expand=True)[1]
    worksheet = worksheet.drop("range", axis=1).reset_index(drop=True)

    # compute observed daily VMT
    worksheet["obTruckVmt"] = (worksheet["truckAADT"] * worksheet["len_mile"]).round(0).astype("int64")

    # gap calculation
    truck_class2gap = {'total':'gap_day',
                       'lhdTruck':'gap_lhd',
                       'mhdTruck':'gap_mhd',
                       'hhdTruck':'gap_hhd'
    }
    
    for trk, gapCol in truck_class2gap.items():
        if trk == 'total':
            # volume vs. aadt
            worksheet["gap_day"] = (
                ((worksheet['TruckFlow'] / worksheet["truckAADT"]) - 1) * 100
            ).round(1)

            # model vmt vs. observed vmt
            worksheet["vmt_gap_day"] = (
                ((worksheet['TruckVmt'] / worksheet["obTruckVmt"]) - 1) * 100
            ).round(1)
        else:
            modelFlow = trk+'Flow'
            obAADT = trk+'AADT'
            gap = 'gap_'+trk
            # volume vs. aadt
            worksheet[gap] = (
                ((worksheet[modelFlow] / worksheet[obAADT]) - 1) * 100
            ).round(1)

    # craete output list
    outputList = []
    outputList.append(worksheet)

    # set aggregator for gap summary
    aggregators = ["rdClass", "vcategory", "city_nm", "pmsa_nm"]

    for agtr in aggregators:
        resultDF = create_gap_range(truck_class2gap['total'], agtr, worksheet, label)
        outputList.append(resultDF)

    # create visualization index
    worksheet["vis_order"] = worksheet.index

    # create unique identifier
    worksheet["link_pair"] = worksheet.apply(lambda x: str(x.hwycovid) + ', ' + str(int(x.hwycovid_2)) if not np.isnan(x.hwycovid_2) else str(x.hwycovid), axis=1)

    #
    keys = ["input_sheet"] + aggregators.copy()
    outputDict = dict(zip(keys, outputList))

    return outputDict



# Execute Validations
fwyFlowDict = freewayVal(hwyload, fwyCounts, fwyLabel)
fwySpdDict = freewayVal(hwyload, fwySpeed, fwyLabel)
allClassDict = allClassVal(hwyload, allCounts, allClassLabel)
transitDict = transitVal(abmRidership, obRidership, transitLabel)
truckDict = truckVal(hwyload, truckAADT, truckLabel)

# Export Results to Master Worksheet
outputSummary = {
    "regional_vmt": regionalVMT,
    "fwy_worksheet": fwyFlowDict["input_sheet"],
    "gap_stat_day": fwyFlowDict["day"],
    "gap_stat_am": fwyFlowDict["am"],
    "gap_stat_pm": fwyFlowDict["pm"],
    "fwy_spd_worksheet": fwySpdDict["input_sheet"],
    "spd_gap_stat_day": fwySpdDict["day"],
    "spd_gap_stat_am": fwySpdDict["am"],
    "spd_gap_stat_pm": fwySpdDict["pm"],
    "spd_gap_stat_md": fwySpdDict["md"],
    "allclass_worksheet": allClassDict["input_sheet"],
    "gap_stat_road_type": allClassDict["rdClass"],
    "gap_stat_vcategory": allClassDict["vcategory"],
    "gap_stat_jur": allClassDict["city_nm"],
    "gap_stat_psma": allClassDict["pmsa_nm"],
    "Compare2FWHA": allClassDict["FHWA"],
    "board_worksheet": transitDict["board_input"],
    "gap_stat_board_day": transitDict["gs_boardByRoute"],
    "DiffByMode_bd_day": transitDict["diff_boardByMode"],
    "GapByMode_bd_day": transitDict["gs_boardByMode"],
    "psgrmile_worksheet": transitDict["psgrmile_input"],
    "gap_stat_psgrmile_day": transitDict["gs_psgrmileByRoute"],
    "DiffByMode_pr_day": transitDict["diff_psgrmileByMode"],
    "GapByMode_pr_day": transitDict["gs_psgrmileByMode"],
    "truck_worksheet": truckDict['input_sheet'],
    "truck_gap_stat_rdClass": truckDict["rdClass"],
    "truck_gap_stat_vcategory": truckDict["vcategory"],
    "truck_gap_stat_jur": truckDict["city_nm"],
    "truck_gap_stat_pmsa": truckDict["pmsa_nm"],
}

with pd.ExcelWriter(
    os.path.join(output_path, "vis_worksheet.xlsx"), engine="openpyxl"
) as writer:
    for sheet_name, table in outputSummary.items():
        table.to_excel(writer, sheet_name=sheet_name, index=False)

print("#####vis_worksheet.xlsx was successfully created.#####")

# Export each table (selected) to a separate CSV file
outputCSV = {
    "allclass_worksheet": allClassDict["input_sheet"],
    "fwy_worksheet": fwyFlowDict["input_sheet"],
    "board_worksheet": transitDict["board_input"],
    "truck_worksheet": truckDict['input_sheet']
}
for name, table in outputCSV.items():
    output_file = os.path.join(output_path, "vis_worksheet - " + f"{name}.csv")
    table.to_csv(output_file, index=False)

print("##### All CSV files were successfully created. #####")