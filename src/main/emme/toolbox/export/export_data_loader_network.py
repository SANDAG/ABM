#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export_data_loader_network.py                                         ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Exports the network results to csv file for use by the Java Data export process
# and the Data loader to the reporting database.
#
#
# Inputs:
#    main_directory: main ABM directory
#    base_scenario_id: scenario ID for the base scenario (same used in the Import network tool)
#    traffic_emmebank: the base, traffic, Emme database
#    transit_emmebank: the transit database
#    num_processors: number of processors to use in the transit analysis calculations
#
# Files created:
#    report/hwyload_pp.csv
#    report/hwy_tcad.csv rename to hwyTcad.csv
#    report/transit_aggflow.csv
#    report/transit_flow.csv
#    report/transit_onoff.csv
#     report/trrt.csv rename to transitRoute.csv
#    report/trstop.csv renmae to transitStop.csv
#    report/transitTap.csv
#    report/transitLink.csv
#
# Script example:
"""
    import os
    import inro.emme.database.emmebank as _eb
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    main_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    transit_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database_transit", "emmebank"))
    num_processors = "MAX-1"
    export_data_loader_network = modeller.tool(
        "sandag.export.export_data_loader_network")
    export_data_loader_network(main_directory, 100, main_emmebank, transit_emmebank, num_processors)
"""

TOOLBOX_ORDER = 73


import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.desktop.worksheet as _ws
import inro.emme.datatable as _dt
import inro.emme.core.exception as _except
from contextlib import contextmanager as _context
from collections import OrderedDict
from itertools import chain as _chain
import math
import os
import pandas as pd
import numpy as _np


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

format = lambda x: ("%.6f" % x).rstrip('0').rstrip(".")
id_format = lambda x: str(int(x))

class ExportDataLoaderNetwork(_m.Tool(), gen_utils.Snapshot):

    main_directory = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    traffic_emmebank = _m.Attribute(str)
    # transit_emmebank = _m.Attribute(str)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.base_scenario_id = 100
        self.traffic_emmebank = os.path.join(project_dir, "Database", "emmebank")
        # self.transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")
        self.num_processors = "MAX-1"
        self.attributes = ["main_directory", "base_scenario_id", "traffic_emmebank", "num_processors"]

        # self.container = gen_utils.DataLakeExporter().get_datalake_connection()
        # self.util_DataLakeExporter = gen_utils.DataLakeExporter(ScenarioPath=self.main_directory
        #                                                         ,container = self.container)


    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export network for Data Loader"
        pb.description = """
Export network results to csv files for SQL data loader."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory', 'directory',
                           title='Select main directory')

        pb.add_text_box('base_scenario_id', title="Base scenario ID:", size=10)
        pb.add_select_file('traffic_emmebank', 'file',
                           title='Select traffic emmebank')
        # pb.add_select_file('transit_emmebank', 'file',
        #                    title='Select transit emmebank')

        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            transit_emmebank_dict = {}
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            for period in ["EA", "AM", "MD", "PM", "EV"]:
                transit_emmebank_dict[period] = _eb.Emmebank(os.path.join(project_dir, "Database_transit_" + period, "emmebank"))
            results = self(self.main_directory, self.base_scenario_id,
                           self.traffic_emmebank, transit_emmebank_dict,
                           self.num_processors)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export network results for Data Loader", save_arguments=True)
    def __call__(self, main_directory, base_scenario_id, traffic_emmebank, transit_emmebank_dict, num_processors):
        attrs = {
            "traffic_emmebank": str(traffic_emmebank),
            "main_directory": main_directory,
            "base_scenario_id": base_scenario_id,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export network results", str(self), attrs)
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))

        traffic_emmebank = _eb.Emmebank(traffic_emmebank)
        # transit_emmebank = _eb.Emmebank(transit_emmebank)
        if not os.path.exists(os.path.join(main_directory, "report")):
            os.mkdir(os.path.join(main_directory, "report"))
        export_path = os.path.join(main_directory, "report")
        input_path = os.path.join(main_directory,"input")
        num_processors = dem_utils.parse_num_processors(num_processors)

        periods = ["EA", "AM", "MD", "PM", "EV"]
        period_scenario_ids = OrderedDict((v, i) for i, v in enumerate(periods, start=base_scenario_id + 1))

        base_scenario = traffic_emmebank.scenario(base_scenario_id)

        self.export_traffic_attribute(base_scenario, export_path, traffic_emmebank, period_scenario_ids, props)
        self.export_traffic_load_by_period(export_path, traffic_emmebank, period_scenario_ids)
        self.export_transit_results(export_path, input_path, transit_emmebank_dict, period_scenario_ids, num_processors)
        self.export_geometry(export_path, traffic_emmebank)

    @_m.logbook_trace("Export traffic attribute data")
    def export_traffic_attribute(self, base_scenario, export_path, traffic_emmebank, period_scenario_ids, props):
        # Several column names are legacy from the original network files
        # and data loader process, and are populated with zeros.
        # items are ("column name", "attribute name") or ("column name", ("attribute name", default))
        hwylink_attrs = [
            ("ID", "@tcov_id"),
            ("HWYSegGUID", "#hwyseg_guid"),
            ("Length", "length"),
            ("Dir", "is_one_way"),
            ("hwycov-id:1", "@tcov_id"),
            ("ID:1", "@tcov_id"),
            ("Length:1", "length_feet"),
            ("QID", "zero"),
            ("CCSTYLE", "zero"),
            ("UVOL", "zero"),
            ("AVOL", "zero"),
            ("TMP1", "zero"),
            ("TMP2", "zero"),
            ("PLOT", "zero"),
            ("SPHERE", "@sphere"),
            ("RTNO", "zero"),
            ("LKNO", "zero"),
            ("NM", "#name"),
            ("FXNM", "#name_from"),
            ("TXNM", "#name_to"),
            ("AN", "i"),
            ("BN", "j"),
            ("COJUR", "zero"),
            ("COSTAT", "zero"),
            ("COLOC", "zero"),
            ("RLOOP", "zero"),
            ("ADTLK", "zero"),
            ("ADTVL", "zero"),
            ("PKPCT", "zero"),
            ("TRPCT", "zero"),
            ("SECNO", "zero"),
            ("DIR:1", "zero"),
            ("FFC", "type"),
            ("CLASS", "zero"),
            ("ASPD", "@speed_adjusted"),
            ("YR", "@year_open_traffic"),
            ("PROJ", "@project_code"),
            ("FC", "type"),
            ("FFC", "@fed_type"),
            ("HOV", "@hov"),
            ("EATRUCK", "@truck_ea"),
            ("AMTRUCK", "@truck_am"),
            ("MDTRUCK", "@truck_md"),
            ("PMTRUCK", "@truck_pm"),
            ("EVTRUCK", "@truck_ev"),
            ("SPD", "@speed_posted"),
            ("TSPD", "zero"),
            ("WAY", "way"),
            ("MED", "@median"),
            ("COST", "@cost_operating"),
        ]
        directional_attrs = [
            ("ABLNO", "@lane_md", "0"),
            ("ABLNA", "@lane_am", "0"),
            ("ABLNP", "@lane_pm", "0"),
            ("ABAU", "@lane_auxiliary", "0"),
            ("ABPCT", "zero", "0"),
            ("ABPHF", "zero", "0"),
            ("ABCNT", "@traffic_control", "0"),
            ("ABTL", "@turn_thru", "0"),
            ("ABRL", "@turn_right", "0"),
            ("ABLL", "@turn_left", "0"),
            ("ABTLB", "zero", "0"),
            ("ABRLB", "zero", "0"),
            ("ABLLB", "zero", "0"),
            ("ABGC", "@green_to_cycle_init", "0"),
            ("ABPLC", "per_lane_capacity", "1900"),
            ("ABTMO", "@time_link_md", "999"),
            ("ABTMA", "@time_link_am", "999"),
            ("ABTMP", "@time_link_pm", "999"),
            ("ABTXO", "@time_inter_md", "0"),
            ("ABTXA", "@time_inter_am", "0"),
            ("ABTXP", "@time_inter_pm", "0"),
            ("ABCST", "zero", "999.999"),
            ("ABVLA", "zero", "0"),
            ("ABVLP", "zero", "0"),
            ("ABLOS", "zero", "0"),
        ]
        for key, name, default in directional_attrs:
            hwylink_attrs.append((key, name))
        for key, name, default in directional_attrs:
            hwylink_attrs.append(("BA" + key[2:], (name, default)))
        hwylink_attrs.append(("relifac", "relifac"))

        time_period_atts = [
            ("TOLL2",     "@toll"),
            ("TOLL3",     "@cost_auto"),
            ("TOLL4",     "@cost_med_truck"),
            ("TOLL5",     "@cost_hvy_truck"),
            ("TOLL",      "toll_hov"),
            ("ABCP",      "@capacity_link", "999999"),
            ("ABCX",      "@capacity_inter", "999999"),
            ("ABCH",      "@capacity_hourly", "0"),
            ("ABTM",      "@time_link", "999"),
            ("ABTX",      "@time_inter", "0"),
            ("ABLN",      "@lane", "0"),
            ("ABSCST",    "sov_total_gencost", ""),
            ("ABH2CST",   "hov2_total_gencost", ""),
            ("ABH3CST",   "hov3_total_gencost", ""),
            ("ABSTM",     "auto_time", ""),
            ("ABHTM",     "auto_time", ""),
        ]
        periods = ["_ea", "_am", "_md", "_pm", "_ev"]
        for column in time_period_atts:
            for period in periods:
                key = column[0] + period.upper()
                name = column[1] + period
                hwylink_attrs.append((key, name))
            if key.startswith("AB"):
                for period in periods:
                    key = column[0] + period.upper()
                    name = column[1] + period
                    default = column[2]
                    hwylink_attrs.append(("BA" + key[2:], (name, default)))
        for period in periods:
            key = "ABPRELOAD" + period.upper()
            name = "additional_volume" + period
            default = "0"
            hwylink_attrs.append((key, name))
            hwylink_attrs.append(("BA" + key[2:], (name, default)))

        vdf_attrs = [
            ("AB_GCRatio", "@green_to_cycle", ""),
            ("AB_Cycle", "@cycle", ""),
            ("AB_PF", "progression_factor", ""),
            ("ALPHA1", "alpha1", "0.8"),
            ("BETA1", "beta1", "4"),
            ("ALPHA2", "alpha2", "4.5"),
            ("BETA2", "beta2", "2"),
        ]
        for key, name, default in vdf_attrs:
            name = name + "_am" if name.startswith("@") else name
            hwylink_attrs.append((key, name))
            if key.startswith("AB"):
                hwylink_attrs.append(("BA" + key[2:], (name, default)))
        for period in periods:
            for key, name, default in vdf_attrs:
                name = name + period if name.startswith("@") else name
                default = default or "0"
                hwylink_attrs.append((key + period.upper(), name))
                if key.startswith("AB"):
                    hwylink_attrs.append(("BA" + key[2:] + period.upper(), (name, default)))

        network = base_scenario.get_partial_network(["LINK"], include_attributes=True)

        #copy assignment from period scenarios
        for period, scenario_id in period_scenario_ids.items():
            from_scenario = traffic_emmebank.scenario(scenario_id)
            src_attrs = ["@auto_time", "additional_volume"]
            dst_attrs = ["auto_time_" + period.lower(),
                         "additional_volume_" + period.lower()]
            for dst_attr in dst_attrs:
                network.create_attribute("LINK", dst_attr)
            values = from_scenario.get_attribute_values("LINK", src_attrs)
            network.set_attribute_values("LINK", dst_attrs, values)
        # add in and calculate additional columns
        new_attrs = [
            ("zero", 0), ("is_one_way", 0), ("way", 2), ("length_feet", 0),
            ("toll_hov", 0), ("per_lane_capacity", 1900),
            ("progression_factor", 1.0), ("alpha1", 0.8), ("beta1", 4.0),
            ("alpha2", 4.5), ("beta2", 2.0), ("relifac", 1.0),
        ]
        for name, default in new_attrs:
            network.create_attribute("LINK", name, default)
        for period in periods:
            network.create_attribute("LINK", "toll_hov" + period, 0)
            network.create_attribute("LINK", "sov_total_gencost" + period, 0)
            network.create_attribute("LINK", "hov2_total_gencost" + period, 0)
            network.create_attribute("LINK", "hov3_total_gencost" + period, 0)
        for link in network.links():
            link.is_one_way = 1 if link.reverse_link else 0
            link.way = 2 if link.reverse_link else 1
            link.length_feet = link.length * 5280
            for period in periods:
                link["toll_hov"  + period] = link["@cost_hov2" + period] - link["@cost_operating"]
                link["sov_total_gencost" + period] = link["auto_time" + period] + link["@cost_auto" + period]
                link["hov2_total_gencost" + period] = link["auto_time" + period] + link["@cost_hov2" + period]
                link["hov3_total_gencost" + period] = link["auto_time" + period] + link["@cost_hov3" + period]
            if link.volume_delay_func == 24:
                link.alpha2 = 6.0
            link.per_lane_capacity = max([(link["@capacity_link" + p] / link["@lane" + p])
                                          for p in periods if link["@lane" + p] > 0] + [0])

        hwylink_atts_file = os.path.join(export_path, "hwy_tcad.csv")
        busPCE = props["transit.bus.pceveh"]
        self.export_traffic_to_csv(hwylink_atts_file, hwylink_attrs, network, busPCE)

    @_m.logbook_trace("Export traffic load data by period")
    def export_traffic_load_by_period(self, export_path, traffic_emmebank, period_scenario_ids):
        create_attribute = _m.Modeller().tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        net_calculator = _m.Modeller().tool(
            "inro.emme.network_calculation.network_calculator")
        hwyload_attrs = [("ID1", "@tcov_id")]

        dir_atts = [
            ("AB_Flow_PCE", "@pce_flow"),   # sum of pce flow
            ("AB_Time", "@auto_time"),      # computed vdf based on pce flow
            ("AB_VOC", "@voc"),
            ("AB_V_Dist_T", "length"),
            ("AB_VHT", "@vht"),
            ("AB_Speed", "@speed"),
            ("AB_VDF", "@msa_time"),
            ("AB_MSA_Flow", "@msa_flow"),
            ("AB_MSA_Time", "@msa_time"),
            ("AB_Flow_SOV_NTPL", "@sov_nt_l"),
            ("AB_Flow_SOV_TPL", "@sov_tr_l"),
            ("AB_Flow_SR2L", "@hov2_l"),
            ("AB_Flow_SR3L", "@hov3_l"),
            ("AB_Flow_SOV_NTPM", "@sov_nt_m"),
            ("AB_Flow_SOV_TPM", "@sov_tr_m"),
            ("AB_Flow_SR2M", "@hov2_m"),
            ("AB_Flow_SR3M", "@hov3_m"),
            ("AB_Flow_SOV_NTPH", "@sov_nt_h"),
            ("AB_Flow_SOV_TPH", "@sov_tr_h"),
            ("AB_Flow_SR2H", "@hov2_h"),
            ("AB_Flow_SR3H", "@hov3_h"),
            ("AB_Flow_lhd", "@trk_l_non_pce"),
            ("AB_Flow_mhd", "@trk_m_non_pce"),
            ("AB_Flow_hhd", "@trk_h_non_pce"),
            ("AB_Flow", "@non_pce_flow"),
        ]

        for key, attr in dir_atts:
            hwyload_attrs.append((key, attr))
            hwyload_attrs.append((key.replace("AB_", "BA_"), (attr, "")))  # default for BA on one-way links is blank
        for p, scen_id in period_scenario_ids.items():
            scenario = traffic_emmebank.scenario(scen_id)
            new_atts = [
                ("@speed", "link travel speed", "length*60/@auto_time"),
                ("@sov_nt_all", "total number of SOV GP vehicles",
                         "@sov_nt_l+@sov_nt_m+@sov_nt_h" ),
                ("@sov_tr_all", "total number of SOV TOLL vehicles",
                         "@sov_tr_l+@sov_tr_m+@sov_tr_h" ),
                ("@hov2_all", "total number of HOV2 HOV vehicles",
                         "@hov2_l+@hov2_m+@hov2_h" ),
                ("@hov3_all", "total number of HOV3 HOV vehicles",
                         "@hov3_l+@hov3_m+@hov3_h" ),
                ("@trk_l_non_pce", "total number of light trucks in non-Pce",
                         "(@trk_l)/1.3" ),
                ("@trk_m_non_pce", "total medium trucks in non-Pce",
                         "(@trk_m)/1.5" ),
                ("@trk_h_non_pce", "total heavy trucks in non-Pce",
                         "(@trk_h)/2.5" ),
                ("@pce_flow", "total number of vehicles in Pce",
                         "@sov_nt_all+@sov_tr_all+ \
                          @hov2_all+ \
                          @hov3_all+ \
                          (@trk_l) + (@trk_m) + \
                          (@trk_h) + volad" ),
                ("@non_pce_flow", "total number of vehicles in non-Pce",
                         "@sov_nt_all+@sov_tr_all+ \
                          @hov2_all+ \
                          @hov3_all+ \
                          (@trk_l)/1.3 + (@trk_m)/1.5 + \
                          (@trk_h)/2.5 + volad/3" ), #volad includes bus flow - pce factor is 3
                ("@msa_flow", "MSA flow", "@non_pce_flow"), #flow from final assignment
                ("@msa_time", "MSA time", "timau"),  #skim assignment time on msa flow
                ("@voc", "volume over capacity", "@pce_flow/ul3"), #pce flow over road capacity
                ("@vht", "vehicle hours travelled", "@non_pce_flow*@auto_time/60") #vehicle flow (non-pce)*time
            ]

            for name, des, formula in new_atts:
                att = scenario.extra_attribute(name)
                if not att:
                    att = create_attribute("LINK", name, des, 0, overwrite=True, scenario=scenario)
                cal_spec = {"result": att.id,
                            "expression": formula,
                            "aggregation": None,
                            "selections": {"link": "mode=d"},
                            "type": "NETWORK_CALCULATION"
                        }
                net_calculator(cal_spec, scenario=scenario)
            file_path = os.path.join(export_path, "hwyload_%s.csv" % p)
            network = self.get_partial_network(scenario, {"LINK": ["@tcov_id"] + [a[1] for a in dir_atts]})
            self.export_traffic_to_csv(file_path, hwyload_attrs, network)

    def export_traffic_to_csv(self, filename, att_list, network, busPCE = None):
        auto_mode = network.mode("d")
        # only the original forward direction links and auto links only
        links = [l for l in network.links()
                 if l["@tcov_id"] > 0 and
                 (auto_mode in l.modes or (l.reverse_link and auto_mode in l.reverse_link.modes))
                ]
        links.sort(key=lambda l: l["@tcov_id"])
        with open(filename, 'w', newline='') as fout:
            fout.write(",".join(['"%s"' % x[0] for x in att_list]))
            fout.write("\n")
            for link in links:
                key, att = att_list[0]  # expected to be the link id
                values = [id_format(link[att])]
                reverse_link = link.reverse_link
                for key, att in att_list[1:]:
                    if key == "AN":
                        values.append(link.i_node.id)
                    elif key == "BN":
                        values.append(link.j_node.id)
                    elif key.startswith("BA"):
                        name, default = att
                        if reverse_link and (abs(link["@tcov_id"]) == abs(reverse_link["@tcov_id"])):
                            if "additional_volume" in name:
                                values.append(format(float(reverse_link[name]) / busPCE))
                            else:
                                values.append(format(reverse_link[name]))
                        else:
                            values.append(default)

                        #values.append(format(reverse_link[name]) if reverse_link else default)
                    elif att.startswith("#"):
                        values.append('"%s"' % link[att])
                    else:
                        if "additional_volume" in att:
                            values.append(format(float(link[att]) / busPCE))
                        else:
                            values.append(format(link[att]))
                fout.write(",".join(values))
                fout.write("\n")
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({os.path.basename(filename)[:-4]:str(filename)})

    @_m.logbook_trace("Export transit results")
    def export_transit_results(self, export_path, input_path, transit_emmebank_dict, period_scenario_ids, num_processors):
        # Note: Node analysis for transfers is VERY time consuming
        #       this implementation will be replaced when new Emme version is available

        trrt_atts = ["Route_ID","Route_Name","Mode","AM_Headway","PM_Headway","Midday_Headway","Evening_Headway","EarlyAM_Headway",
                     "Evening_Hours", "EarlyAM_Hours", "Config","Fare"]
        trstop_atts = ["Stop_ID","Route_ID","Link_ID","Link_GUID","Pass_Count","Milepost","Longitude","Latitude","NearNode","StopName"]

        #transit route file
        trrt_infile = os.path.join(input_path, "trrt.csv")
        trrt = pd.read_csv(trrt_infile)
        trrt = trrt.rename(columns=lambda x:x.strip())
        trrt_out = trrt[trrt_atts]
        trrt_outfile = os.path.join(export_path, "trrt.csv")
        trrt_out.to_csv(trrt_outfile, index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'trrt':trrt_out})

        #transit stop file
        trstop_infile = os.path.join(input_path, "trstop.csv")
        trstop = pd.read_csv(trstop_infile)
        trstop = trstop.rename(columns={"Node":"NearNode"})
        trstop = trstop.rename(columns=lambda x:x.strip())
        trstop_out = trstop[trstop_atts]
        trstop_outfile = os.path.join(export_path, "trstop.csv")
        trstop_out.to_csv(trstop_outfile, index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'trstop':trstop_out})

        use_node_analysis_to_get_transit_transfers = False

        copy_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.copy_scenario")
        create_attribute = _m.Modeller().tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        net_calculator = _m.Modeller().tool(
            "inro.emme.network_calculation.network_calculator")
        copy_attribute= _m.Modeller().tool(
            "inro.emme.data.network.copy_attribute")
        delete_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.delete_scenario")
        transit_flow_atts = [
            "MODE",
            "ACCESSMODE",
            "TOD",
            "ROUTE",
            "FROM_STOP",
            "TO_STOP",
            "CENTROID",
            "FROMMP",
            "TOMP",
            "TRANSITFLOW",
            "BASEIVTT",
            "COST",
            "VOC",
        ]
        transit_aggregate_flow_atts = [
            "MODE",
            "ACCESSMODE",
            "TOD",
            "LINK_ID",
            "AB_TransitFlow",
            "BA_TransitFlow",
            "AB_NonTransit",
            "BA_NonTransit",
            "AB_TotalFlow",
            "BA_TotalFlow",
            "AB_Access_Walk_Flow",
            "BA_Access_Walk_Flow",
            "AB_Xfer_Walk_Flow",
            "BA_Xfer_Walk_Flow",
            "AB_Egress_Walk_Flow",
            "BA_Egress_Walk_Flow"
        ]
        transit_onoff_atts = [
            "MODE",
            "ACCESSMODE",
            "TOD",
            "ROUTE",
            "STOP",
            "BOARDINGS",
            "ALIGHTINGS",
            "WALKACCESSON",
            "DIRECTTRANSFERON",
            "WALKTRANSFERON",
            "DIRECTTRANSFEROFF",
            "WALKTRANSFEROFF",
            "EGRESSOFF"
        ]

        transit_flow_file = os.path.join(export_path, "transit_flow.csv")
        fout_seg = open(transit_flow_file, 'w', newline='')
        fout_seg.write(",".join(['"%s"' % x for x in transit_flow_atts]))
        fout_seg.write("\n")

        transit_aggregate_flow_file = os.path.join(export_path, "transit_aggflow.csv")
        fout_link = open(transit_aggregate_flow_file, 'w', newline='')
        fout_link.write(",".join(['"%s"' % x for x in transit_aggregate_flow_atts]))
        fout_link.write("\n")

        transit_onoff_file = os.path.join(export_path, "transit_onoff.csv")
        fout_stop = open(transit_onoff_file, 'w', newline='')
        fout_stop.write(",".join(['"%s"' % x for x in transit_onoff_atts]))
        fout_stop.write("\n")
        try:
            access_modes = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN", "TNCOUT", "TNCIN"]
            main_modes = ["LOC", "PRM","MIX"]
            all_modes = ["b", "c", "e", "l", "r", "p", "y", "o", "w", "x", "k", "u", "f", "g", "q", "j", "Q", "J"]
            local_bus_modes = ["b", "w", "x", "k", "u", "f", "g", "q", "j", "Q", "J"]
            premium_modes = ["c", "l", "e", "p", "r", "y", "o", "w", "x", "k", "u", "f", "g", "q", "j", "Q", "J"]
            for tod, scen_id in period_scenario_ids.items():
                with _m.logbook_trace("Processing period %s" % tod):
                    scenario = transit_emmebank_dict[tod].scenario(scen_id)
                    with _m.logbook_trace("Scen %s" % (scenario)):
                    # attributes
                        total_walk_flow = create_attribute("LINK", "@volax", "total walk flow on links",
                                    0, overwrite=True, scenario=scenario)
                        segment_flow = create_attribute("TRANSIT_SEGMENT", "@voltr", "transit segment flow",
                                    0, overwrite=True, scenario=scenario)
                        link_transit_flow = create_attribute("LINK", "@link_voltr", "total transit flow on link",
                                    0, overwrite=True, scenario=scenario)
                        initial_boardings = create_attribute("TRANSIT_SEGMENT",
                                    "@init_boardings", "transit initial boardings",
                                    0, overwrite=True, scenario=scenario)
                        xfer_boardings = create_attribute("TRANSIT_SEGMENT",
                                    "@xfer_boardings", "transit transfer boardings",
                                    0, overwrite=True, scenario=scenario)
                        total_boardings = create_attribute("TRANSIT_SEGMENT",
                                    "@total_boardings", "transit total boardings",
                                    0, overwrite=True, scenario=scenario)
                        final_alightings = create_attribute("TRANSIT_SEGMENT",
                                    "@final_alightings", "transit final alightings",
                                    0, overwrite=True, scenario=scenario)
                        xfer_alightings = create_attribute("TRANSIT_SEGMENT",
                                    "@xfer_alightings", "transit transfer alightings",
                                    0, overwrite=True, scenario=scenario)
                        total_alightings = create_attribute("TRANSIT_SEGMENT",
                                    "@total_alightings", "transit total alightings",
                                    0, overwrite=True, scenario=scenario)

                        access_walk_flow = create_attribute("LINK",
                                    "@access_walk_flow", "access walks (orig to init board)",
                                    0, overwrite=True, scenario=scenario)
                        xfer_walk_flow = create_attribute("LINK",
                                    "@xfer_walk_flow", "xfer walks (init board to final alight)",
                                    0, overwrite=True, scenario=scenario)
                        egress_walk_flow = create_attribute("LINK",
                                    "@egress_walk_flow", "egress walks (final alight to dest)",
                                    0, overwrite=True, scenario=scenario)

                        for main_mode in main_modes:
                            mode = main_mode
                            if main_mode == "LOC":
                                mode_list = local_bus_modes
                            elif main_mode == "PRM":
                                mode_list = premium_modes
                            else:
                                mode_list = all_modes

                            for access_type in access_modes:
                                with _m.logbook_trace("Main mode %s access mode %s" % (main_mode, access_type)):
                                    class_name = "%s_%s__%s" % (access_type, main_mode, tod)
                                    segment_results = {
                                        "transit_volumes": segment_flow.id,
                                        "initial_boardings": initial_boardings.id,
                                        "total_boardings": total_boardings.id,
                                        "final_alightings": final_alightings.id,
                                        "total_alightings": total_alightings.id,
                                        "transfer_boardings": xfer_boardings.id,
                                        "transfer_alightings": xfer_alightings.id
                                    }
                                    link_results = {
                                        "total_walk_flow": total_walk_flow.id,
                                        "link_transit_flow": link_transit_flow.id,
                                        "access_walk_flow": access_walk_flow.id,
                                        "xfer_walk_flow": xfer_walk_flow.id,
                                        "egress_walk_flow": egress_walk_flow.id
                                    }

                                    self.calc_additional_results(
                                        scenario, class_name, num_processors,
                                        total_walk_flow, segment_results, link_transit_flow,
                                        access_walk_flow, xfer_walk_flow, egress_walk_flow)
                                    attributes = {
                                        "NODE": ["@network_adj", "@network_adj_src"],#, "initial_boardings", "final_alightings"],
                                        "LINK": list(link_results.values()) + ["@tcov_id", "length"],
                                        "TRANSIT_LINE": ["@route_id"],
                                        "TRANSIT_SEGMENT": list(segment_results.values()) + [
                                            "transit_time", "dwell_time", "@stop_id", "allow_boardings", "allow_alightings"],
                                    }
                                    network = self.get_partial_network(scenario, attributes)
                                    self.collapse_network_adjustments(network, segment_results, link_results)
                                    # ===============================================
                                    # analysis for nodes with/without walk option
                                    if use_node_analysis_to_get_transit_transfers:
                                        stop_on, stop_off = self.transfer_analysis(scenario, class_name, num_processors)
                                    else:
                                        stop_on, stop_off = {}, {}
                                    # ===============================================
                                    transit_modes = [m for m in network.modes() if m.type in ("TRANSIT", "AUX_TRANSIT")]
                                    links = [link for link in network.links()
                                            if link["@tcov_id"] > 0 and (link.modes.union(transit_modes))]
                                    links.sort(key=lambda l: l["@tcov_id"])
                                    lines = [line for line in network.transit_lines() if line.mode.id in mode_list]
                                    lines.sort(key=lambda l: l["@route_id"])

                                    label = ",".join([mode, access_type, tod])
                                    self.output_transit_flow(label, lines, segment_flow.id, fout_seg)
                                    self.output_transit_aggregate_flow(
                                    label, links, link_transit_flow.id, total_walk_flow.id, access_walk_flow.id,
                                        xfer_walk_flow.id, egress_walk_flow.id, fout_link)
                                    self.output_transit_onoff(
                                        label, lines, total_boardings.id, total_alightings.id, initial_boardings.id,
                                        xfer_boardings.id, xfer_alightings.id, final_alightings.id,
                                        stop_on, stop_off, fout_stop)
        finally:
            fout_stop.close()
            fout_link.close()
            fout_seg.close()

            # if self.container:
            #     self.util_DataLakeExporter.write_to_datalake({'transit_flow':str(transit_flow_file)
            #                                                     ,'transit_aggregate_flow':str(transit_aggregate_flow_file)
            #                                                     ,'transit_onoff':str(transit_onoff_file)})
        return

    @_m.logbook_trace("Export geometries")
    def export_geometry(self, export_path, traffic_emmebank):
        # --------------------------Export Transit Nework Geometory-----------------------------
        # domain: NODE, LINK, TURN, TRANSIT_LINE, TRANSIT_VEHICLE, TRANSIT_SEGMENT
        def export_as_csv(domain, attributes, scenario = None):
            if scenario is None:
                scenario = _m.Modeller().scenario
            initial_scenario = _m.Modeller().scenario
            #if initial_scenario.number != scenario.number:
                #data_explorer.replace_primary_scenario(scenario)
            # Create the network table
            network_table = project.new_network_table(domain)
            for k, a in enumerate(attributes):
                column = _ws.Column()
                column.name = column.expression = a
                network_table.add_column(k, column)
            # Extract data
            data = network_table.get_data()
            f = _np.vectorize(lambda x: x.text)  # required to get the WKT representation of the geometry column
            data_dict = {}
            for a in data.attributes():
                if isinstance(a, _dt.GeometryAttribute):
                    data_dict[a.name] = f(a.values)
                else:
                    data_dict[a.name] = a.values
            df = pd.DataFrame(data_dict)

            network_table.close()
            #if initial_scenario.number != scenario.number:
            #    data_explorer.replace_primary_scenario(initial_scenario)
            return df

        desktop = _m.Modeller().desktop
        desktop.refresh_data()
        data_explorer = desktop.data_explorer()
        previous_active_database = data_explorer.active_database()
        try:
            desktop_traffic_database = data_explorer.add_database(traffic_emmebank.path)
            desktop_traffic_database.open()
        except Exception as error:
            import traceback
            print (traceback.format_exc())
        project = desktop.project
        scenario = _m.Modeller().emmebank.scenario(101)
        data_explorer.replace_primary_scenario(scenario)
        node_attributes = ['i','@tap_id']
        link_attributes = ['i', 'j', '@tcov_id', 'modes']
        transit_line_attributes = ['line', 'routeID']
        transit_segment_attributes = ['line', 'i', 'j', 'loop_index','@tcov_id','@stop_id']
        mode_talbe = ['mode', 'type']
        network_table = project.new_network_table('MODE')
        for k, a in enumerate(mode_talbe):
            column = _ws.Column()
            column.name = column.expression = a
            network_table.add_column(k, column)
        data = network_table.get_data()
        data_dict = {}
        for a in data.attributes():
            data_dict[a.name] = a.values
        df = pd.DataFrame(data_dict)
        mode_list = df[df['type'].isin([2.0, 3.0])]['mode'].tolist()

        # df = export_as_csv('NODE', node_attributes, scenario)
        # df = df[['@tap_id', 'geometry']]
        # is_tap =  df['@tap_id'] > 0
        # df = df[is_tap]
        # df.columns = ['tapID', 'geometry']
        # df.to_csv(os.path.join(export_path, 'transitTap.csv'), index=False)
        df = export_as_csv('TRANSIT_LINE', transit_line_attributes)
        df = df[['line', 'geometry']]
        df.columns = ['Route_Name', 'geometry']
        df['Route_Name'] = df['Route_Name'].astype(int)
        df_routeFull = pd.read_csv(os.path.join(export_path, 'trrt.csv'))
        result = pd.merge(df_routeFull, df, how='left', on=['Route_Name'])
        mode5tod = pd.read_csv(os.path.join(self.main_directory, 'input', 'MODE5TOD.csv')).set_index('MODE_ID')
        result['Mode_Name'] = result['Mode'].map(mode5tod['MODE_NAME'])
        result.to_csv(os.path.join(export_path, 'transitRoute.csv'), index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'transitRoute':result})
        os.remove(os.path.join(export_path, 'trrt.csv'))

        df = export_as_csv('TRANSIT_SEGMENT', transit_segment_attributes, None)
        df_seg = df[['@tcov_id', 'geometry']]
        df_seg.columns = ['trcovID', 'geometry']
        df_seg = df_seg.drop_duplicates()
        #df_seg.to_csv(os.path.join(export_path, 'transitLink.csv'), index=False)
        #df_stop = df[(df['@stop_id'] > 0) & (df['@tcov_id'] > 0)]
        df_stop = df[(df['@stop_id'] > 0)]
        df_stop = df_stop[['@stop_id', 'geometry']]
        df_stop = df_stop.drop_duplicates()
        df_stop.columns = ['Stop_ID', 'geometry']
        temp=[]
        for value in df_stop['geometry']:
            value=value.split(',')
            value[0]=value[0]+')'
            value[0]=value[0].replace("LINESTRING", "POINT")
            temp.append(value[0])
        df_stop['geometry'] = temp
        df_stopFull = pd.read_csv(os.path.join(export_path, 'trstop.csv'))
        result = pd.merge(df_stopFull, df_stop, how='left', on=['Stop_ID'])
        result.to_csv(os.path.join(export_path, 'transitStop.csv'), index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'transitStop':result})
        os.remove(os.path.join(export_path, 'trstop.csv'))

        df = export_as_csv('LINK', link_attributes, None)
        df_link = df[['@tcov_id', 'geometry']]
        df_link.columns = ['hwycov-id:1', 'geometry']
        df_linkFull = pd.read_csv(os.path.join(export_path, 'hwy_tcad.csv'))
        result = pd.merge(df_linkFull, df_link, how='left', on=['hwycov-id:1'])
        result.to_csv(os.path.join(export_path, 'hwyTcad.csv'), index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'hwyTcad':result})
        os.remove(os.path.join(export_path, 'hwy_tcad.csv'))
        ##mode_list = ['Y','b','c','e','l','p','r','y','a','x','w']##
        df_transit_link = df[df.modes.str.contains('|'.join(mode_list))]
        df_transit_link = df_transit_link[['@tcov_id', 'geometry']]
        df_transit_link.columns = ['trcovID', 'geometry']
        df_transit_link = df_transit_link[df_transit_link['trcovID'] != 0]
        df_transit_link['AB'] = df_transit_link['trcovID'].apply(lambda x: 1 if x > 0 else 0)
        df_transit_link['trcovID'] = abs(df_transit_link['trcovID'])
        df_transit_link = df_transit_link[['trcovID', 'AB', 'geometry']]
        df_transit_link.to_csv(os.path.join(export_path, 'transitLink.csv'), index=False)
        # if self.container:
        #     self.util_DataLakeExporter.write_to_datalake({'transitLink':df_transit_link})
        network_table.close()
        try:
            previous_active_database.open()
            data_explorer.remove_database(desktop_traffic_database)
        except:
            pass

    def get_partial_network(self, scenario, attributes):
        domains = attributes.keys()
        network = scenario.get_partial_network(domains, include_attributes=False)
        for domain, attrs in attributes.items():
            if attrs:
                values = scenario.get_attribute_values(domain, attrs)
                network.set_attribute_values(domain, attrs, values)
        return network

    def output_transit_flow(self, label, lines, segment_flow, fout_seg):
        # output segment data (transit_flow)
        centroid = "0"  # always 0
        voc = ""  # volume/capacity, not actually used,
        for line in lines:
            line_id = id_format(line["@route_id"])
            ivtt = from_mp = to_mp = 0
            segments = iter(line.segments(include_hidden=True))
            seg = next(segments)
            from_stop = id_format(seg["@stop_id"])
            for next_seg in segments:
                to_mp += seg.link.length
                ivtt += seg.transit_time - next_seg.dwell_time
                transit_flow = seg[segment_flow]
                seg = next_seg
                if not next_seg.allow_alightings:
                    continue
                to_stop = id_format(next_seg["@stop_id"])
                formatted_ivtt = format(ivtt)
                fout_seg.write(",".join([
                    label, line_id, from_stop, to_stop, centroid, format(from_mp), format(to_mp),
                    format(transit_flow), formatted_ivtt, formatted_ivtt, voc]))
                fout_seg.write("\n")
                from_stop = to_stop
                from_mp = to_mp
                ivtt = 0

    def output_transit_aggregate_flow(self, label, links,
                                      link_transit_flow, total_walk_flow, access_walk_flow,
                                      xfer_walk_flow, egress_walk_flow, fout_link):
        # output link data (transit_aggregate_flow)
        for link in links:
            link_id = id_format(link["@tcov_id"])
            ab_transit_flow = link[link_transit_flow]
            ab_non_transit_flow = link[total_walk_flow]
            ab_total_flow = ab_transit_flow + ab_non_transit_flow
            ab_access_walk_flow = link[access_walk_flow]
            ab_xfer_walk_flow = link[xfer_walk_flow]
            ab_egress_walk_flow = link[egress_walk_flow]
            if link.reverse_link:
                ba_transit_flow = link.reverse_link[link_transit_flow]
                ba_non_transit_flow = link.reverse_link[total_walk_flow]
                ba_total_flow = ba_transit_flow + ba_non_transit_flow
                ba_access_walk_flow = link.reverse_link[access_walk_flow]
                ba_xfer_walk_flow = link.reverse_link[xfer_walk_flow]
                ba_egress_walk_flow = link.reverse_link[egress_walk_flow]
            else:
                ba_transit_flow = 0.0
                ba_non_transit_flow = 0.0
                ba_total_flow = 0.0
                ba_access_walk_flow = 0.0
                ba_xfer_walk_flow = 0.0
                ba_egress_walk_flow = 0.0

            fout_link.write(",".join(
                [label, link_id,
                 format(ab_transit_flow), format(ba_transit_flow),
                 format(ab_non_transit_flow), format(ba_non_transit_flow),
                 format(ab_total_flow), format(ba_total_flow),
                 format(ab_access_walk_flow), format(ba_access_walk_flow),
                 format(ab_xfer_walk_flow), format(ba_xfer_walk_flow),
                 format(ab_egress_walk_flow), format(ba_egress_walk_flow)]))
            fout_link.write("\n")

    def output_transit_onoff(self, label, lines,
                             total_boardings, total_alightings, initial_boardings,
                             xfer_boardings, xfer_alightings, final_alightings,
                             stop_on, stop_off, fout_stop):
        # output stop data (transit_onoff)
        for line in lines:
            line_id = id_format(line["@route_id"])
            for seg in line.segments(True):
                if not (seg.allow_alightings or seg.allow_boardings):
                    continue
                i_node = seg.i_node.id
                boardings = seg[total_boardings]
                alightings = seg[total_alightings]
                walk_access_on = seg[initial_boardings]
                direct_xfer_on = seg[xfer_boardings]
                walk_xfer_on = 0.0
                direct_xfer_off = seg[xfer_alightings]
                walk_xfer_off = 0.0
                if i_node in stop_on:
                    if line.id in stop_on[i_node]:
                        if direct_xfer_on > 0:
                            walk_xfer_on = direct_xfer_on - stop_on[i_node][line.id]
                            direct_xfer_on = stop_on[i_node][line.id]
                if i_node in stop_off:
                    if line.id in stop_off[i_node]:
                        if direct_xfer_off > 0:
                            walk_xfer_off = direct_xfer_off - stop_off[i_node][line.id]
                            direct_xfer_off = stop_off[i_node][line.id]

                egress_off = seg[final_alightings]
                fout_stop.write(",".join([
                    label, line_id, id_format(seg["@stop_id"]),
                    format(boardings), format(alightings), format(walk_access_on),
                    format(direct_xfer_on), format(walk_xfer_on), format(direct_xfer_off),
                    format(walk_xfer_off), format(egress_off)]))
                fout_stop.write("\n")

    def collapse_network_adjustments(self, network, segment_results, link_results):
        segment_alights = [v for k, v in list(segment_results.items()) if "alightings" in k]
        segment_boards = [v for k, v in list(segment_results.items()) if "boardings" in k] + ["transit_boardings"]
        segment_result_attrs = segment_alights + segment_boards
        link_result_attrs = list(link_results.values()) + ["aux_transit_volume"]
        link_attrs = network.attributes("LINK")
        link_modified_attrs = [
            "length", "@trtime", link_results["link_transit_flow"]]
        seg_attrs = network.attributes("TRANSIT_SEGMENT")
        line_attrs = network.attributes("TRANSIT_LINE")

        transit_modes = set([network.mode(m) for m in "blryepc"])
        aux_modes = set([network.mode(m) for m in "wxa"])
        xfer_mode = network.mode('x')

        def copy_seg_attrs(src_seg, dst_seg):
            for attr in segment_result_attrs:
                dst_seg[attr] += src_seg[attr]
            dst_seg["allow_alightings"] |= src_seg["allow_alightings"]
            dst_seg["allow_boardings"] |= src_seg["allow_boardings"]

        def get_xfer_link(node, timed_xfer_link, is_outgoing=True):
            links = node.outgoing_links() if is_outgoing else node.incoming_links()
            for link in links:
                if xfer_mode in link.modes and link.length == timed_xfer_link.length:
                    return link
            return None

        lines_to_update = set([])
        nodes_to_merge = []
        nodes_to_delete = []

        for node in network.regular_nodes():
            if node["@network_adj"] == 1:
                nodes_to_merge.append(node)
                # copy boarding / alighting attributes for the segments to the original segment / stop
                for seg in node.incoming_segments():
                    lines_to_update.add(seg.line)
                    copy_seg_attrs(seg, seg.line.segment(seg.number+2))
                for seg in node.outgoing_segments():
                    lines_to_update.add(seg.line)
                    copy_seg_attrs(seg, seg.line.segment(seg.number+1))
            elif node["@network_adj"] == 2:
                nodes_to_delete.append(node)
                # copy boarding / alighting attributes for the segments to the original segment / stop
                for seg in node.outgoing_segments(True):
                    lines_to_update.add(seg.line)
                    if seg.j_node:
                        copy_seg_attrs(seg, seg.line.segment(seg.number+1))
                    else:
                        copy_seg_attrs(seg, seg.line.segment(seg.number-1))
            elif node["@network_adj"] == 3:
                orig_node = network.node(node["@network_adj_src"])
                # Remove transfer walk links and copy data to source walk link
                for link in _chain(node.incoming_links(), node.outgoing_links()):
                    if xfer_mode in link.modes and link.j_node["@network_adj"] == 3 and link.i_node["@network_adj"] == 3:
                        orig_xfer_link = get_xfer_link(orig_node, link)
                        for attr in link_result_attrs:
                            orig_xfer_link[attr] += link[attr]
                        network.delete_link(link.i_node, link.j_node)
                # Sum link and segment results and merge links
                mapping = network.merge_links_mapping(node)
                for (link1, link2), attr_map in mapping['links'].items():
                    for attr in link_modified_attrs:
                        attr_map[attr] = max(link1[attr], link2[attr])

                for (seg1, seg2), attr_map in mapping['transit_segments'].items():
                    if seg2.allow_alightings:
                        for attr in seg_attrs:
                            attr_map[attr] = seg1[attr]
                    else:  # if it is a boarding stop or non-stop
                        for attr in seg_attrs:
                            attr_map[attr] = max(seg1[attr], seg2[attr])
                    attr_map["transit_time_func"] = min(seg1["transit_time_func"], seg2["transit_time_func"])
                    for attr in segment_boards:
                        attr_map[attr] = seg1[attr] + seg2[attr]
                    next_seg = seg2.line.segment(seg2.number+1)
                    for attr in segment_alights:
                        next_seg[attr] += seg2[attr]
                    attr_map["transit_time"] = seg1["transit_time"] + seg2["transit_time"]
                network.merge_links(node, mapping)

        # Backup transit lines with altered routes and remove from network
        lines = []
        for line in lines_to_update:
            seg_data = {}
            itinerary = []
            for seg in line.segments(include_hidden=True):
                if seg.i_node["@network_adj"] in [1,2] or (seg.j_node and seg.j_node["@network_adj"] == 1):
                    continue
                # for circle line transfers, j_node is now None for new "hidden" segment
                j_node = seg.j_node
                if (seg.j_node and seg.j_node["@network_adj"] == 2):
                    j_node = None
                seg_data[(seg.i_node, j_node, seg.loop_index)] = dict((k, seg[k]) for k in seg_attrs)
                itinerary.append(seg.i_node.number)

            lines.append({
                "id": line.id,
                "vehicle": line.vehicle,
                "itinerary": itinerary,
                "attributes": dict((k, line[k]) for k in line_attrs),
                "seg_attributes": seg_data})
            network.delete_transit_line(line)
        # Remove duplicate network elements (undo network adjustments)
        for node in nodes_to_delete:
            for link in _chain(node.incoming_links(), node.outgoing_links()):
                network.delete_link(link.i_node, link.j_node)
            network.delete_node(node)
        for node in nodes_to_merge:
            mapping = network.merge_links_mapping(node)
            for (link1, link2), attr_map in mapping["links"].items():
                if link2.j_node.is_centroid:
                    link1, link2 = link2, link1
                for attr in link_attrs:
                    attr_map[attr] = link1[attr]
            network.merge_links(node, mapping)
        # Re-create transit lines on new itineraries
        for line_data in lines:
            new_line = network.create_transit_line(
                line_data["id"], line_data["vehicle"], line_data["itinerary"])
            for k, v in line_data["attributes"].items():
                new_line[k] = v
            seg_data = line_data["seg_attributes"]
            for seg in new_line.segments(include_hidden=True):
                data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                for k, v in data.items():
                    seg[k] = v

    def calc_additional_results(self, scenario, class_name, num_processors,
                                total_walk_flow, segment_results, link_transit_flow,
                                access_walk_flow, xfer_walk_flow, egress_walk_flow):
        network_results = _m.Modeller().tool(
            "inro.emme.transit_assignment.extended.network_results")
        path_based_analysis = _m.Modeller().tool(
            "inro.emme.transit_assignment.extended.path_based_analysis")
        net_calculator = _m.Modeller().tool(
            "inro.emme.network_calculation.network_calculator")

        spec = {
            "on_links": {
                "aux_transit_volumes": total_walk_flow.id
            },
            "on_segments": segment_results,
            "aggregated_from_segments": None,
            "analyzed_demand": None,
            "constraint": None,
            "type": "EXTENDED_TRANSIT_NETWORK_RESULTS"
        }
        network_results(specification=spec, scenario=scenario,
                        class_name=class_name, num_processors=num_processors)
        cal_spec = {
            "result": "%s" % link_transit_flow.id,
            "expression": "%s" % segment_results["transit_volumes"],
            "aggregation": "+",
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        net_calculator(cal_spec, scenario=scenario)

        walk_flows = [("INITIAL_BOARDING_TO_FINAL_ALIGHTING", access_walk_flow.id),
                      ("INITIAL_BOARDING_TO_FINAL_ALIGHTING", xfer_walk_flow.id),
                      ("FINAL_ALIGHTING_TO_DESTINATION", egress_walk_flow.id)]
        for portion_of_path, aux_transit_volumes in walk_flows:
            spec = {
                    "portion_of_path": portion_of_path,
                    "trip_components": {
                        "in_vehicle": None,
                        "aux_transit": "length",
                        "initial_boarding": None,
                        "transfer_boarding": None,
                        "transfer_alighting": None,
                        "final_alighting": None
                    },
                    "path_operator": ".max.",
                    "path_selection_threshold": {
                        "lower": -1.0,
                        "upper": 999999.0
                    },
                    "path_to_od_aggregation": None,
                    "constraint": None,
                    "analyzed_demand": None,
                    "results_from_retained_paths": {
                        "paths_to_retain": "SELECTED",
                        "aux_transit_volumes": aux_transit_volumes
                    },
                    "path_to_od_statistics": None,
                    "path_details": None,
                    "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
                }
            path_based_analysis(
                specification=spec, scenario=scenario,
                class_name=class_name, num_processors=num_processors)

    def transfer_analysis(self, scenario, net, class_name, num_processors):
        create_attribute = _m.Modeller().tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        transfers_at_stops = _m.Modeller().tool(
            "inro.emme.transit_assignment.extended.apps.transfers_at_stops")

        # find stop with/without walk transfer option
        stop_walk_list = []  # stop (id) with walk option
        stop_flag = "@stop_flag"
        create_attribute("NODE", att, "1=stop without walk option, 2=otherwise",
                                     0, overwrite=True, scenario=scenario)
        stop_nline = "@stop_nline"
        create_attribute("NODE", stop_nline, "number of lines on the stop",
                                      0, overwrite=True, scenario=scenario)

        for line in net.transit_lines():
            for seg in line.segments(True):
                node = seg.i_node
                if seg.allow_alightings or seg.allow_boardings:
                    node[stop_nline] += 1
                if node[stop_flag] > 0 :  #node checked
                    continue
                if seg.allow_alightings or seg.allow_boardings:
                    node[stop_flag] = 1
                    for ilink in node.incoming_links():
                        # skip connector
                        if ilink.i_node.is_centroid:
                            continue
                        for m in ilink.modes:
                            if m.type=="AUX_TRANSIT":
                                node[stop_flag] = 2
                                stop_walk_list.append(node.id)
                                break
                        if node[stop_flag]>=2:
                            break
                    if node[stop_flag]>=2:
                        continue
                    for olink in node.outgoing_links():
                        # skip connector
                        if olink.j_node.is_centroid:
                            continue
                        for m in olink.modes:
                            if m.type=="AUX_TRANSIT":
                                node[stop_flag] = 2
                                stop_walk_list.append(node.id)
                                break
                        if node[stop_flag]>=2:
                            break
        #scenario.publish_network(net)
        stop_off = {}
        stop_on = {}
        for stop in stop_walk_list:
            stop_off[stop] = {}
            stop_on[stop] = {}
            selection = "i=%s" % stop
            results = transfers_at_stops(
                selection, scenario=scenario,
                class_name=class_name, num_processors=num_processors)
            for off_line in results:
                stop_off[stop][off_line] = 0.0
                for on_line in results[off_line]:
                    trip = float(results[off_line][on_line])
                    stop_off[stop][off_line] += trip
                    if on_line not in stop_on[stop]:
                        stop_on[stop][on_line] = 0.0
                    stop_on[stop][on_line] += trip
        return stop_off, stop_on

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg