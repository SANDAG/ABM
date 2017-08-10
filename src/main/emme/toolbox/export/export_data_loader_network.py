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

TOOLBOX_ORDER = 73


import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.core.exception as _except
from contextlib import contextmanager as _context
from collections import OrderedDict
import os

gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class ExportDataLoaderNetwork(_m.Tool(), gen_utils.Snapshot):

    main_directory = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    traffic_emmebank = _m.Attribute(str)
    transit_emmebank = _m.Attribute(str)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.base_scenario_id = 100
        self.traffic_emmebank = os.path.join(project_dir, "Database", "emmebank")
        self.transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")
        self.num_processors = "MAX-1"
        self.attributes = ["main_directory", "base_scenario_id", "traffic_emmebank", "transit_emmebank", "num_processors"]

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
        pb.add_select_file('transit_emmebank', 'file',
                           title='Select transit emmebank')

        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(self.main_directory, self.base_scenario_id,
                           self.traffic_emmebank, self.transit_emmebank,
                           self.num_processors)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export network results for Data Loader", save_arguments=True)
    def __call__(self, main_directory, base_scenario_id, traffic_emmebank, transit_emmebank, num_processors):
        attrs = {
            "traffic_emmebank": str(traffic_emmebank),
            "transit_emmebank": str(transit_emmebank),
            "main_directory": main_directory,
            "base_scenario_id": base_scenario_id,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export network results", str(self), attrs)

        traffic_emmebank = _eb.Emmebank(traffic_emmebank)
        transit_emmebank = _eb.Emmebank(transit_emmebank)
        export_path = os.path.join(main_directory, "report")
        num_processors = dem_utils.parse_num_processors(num_processors)

        periods = ["EA", "AM", "MD", "PM", "EV"]
        period_scenario_id = OrderedDict((v, i) for i, v in enumerate(periods, start=base_scenario_id + 1))

        base_scenario = traffic_emmebank.scenario(base_scenario_id)
        
        self.export_traffic_attribute(base_scenario, export_path, traffic_emmebank, period_scenario_id)
        self.export_traffic_load_by_period(export_path, traffic_emmebank, period_scenario_id)
        self.export_transit_results(export_path, transit_emmebank, period_scenario_id, num_processors)

    @_m.logbook_trace("Export traffic attribute data")
    def export_traffic_attribute(self, base_scenario, export_path, traffic_emmebank, period_scenario_id):
        copy_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.copy_scenario")
        create_attribute = _m.Modeller().tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        copy_attribute= _m.Modeller().tool(
            "inro.emme.data.network.copy_attribute")
        delete_scenario = _m.Modeller().tool(
            "inro.emme.data.scenario.delete_scenario")

        auto_mode_id = "d"
        result_scenario_id = base_scenario.number + 6
        hwylink_atts = [
            ("ID", "@tcov_id"),
            ("Length", "length"), ("SPHERE", "@sphere"),
            ("NM", "#name"),
            ("FXNM", "#name_from"), ("TXNM", "#name_to"),
            ("AN", "i"), ("BN", "j"),
            ("ASPD", "@speed_adjusted"), ("IYR", "@year_open_traffic"),
            ("IPROJ", "@project_code"), ("IJUR", "@jurisdiction_type"),
            ("IFC", "type"), ("IHOV", "@lane_restriction"),
            ("ITRUCK", "@truck_restriction"), ("ISPD", "@speed_posted"),
            ("IWAY", "1/2 way"), ("IMED", "@median"),
            ("ABAU", "@lane_auxiliary"), ("ABCNT", "@traffic_control"),
            ("BAAU", "@lane_auxiliary"), ("BACNT", "@traffic_control"),
            ("ITOLL2_EA", "@toll_ea"),
            ("ITOLL2_AM", "@toll_am"),
            ("ITOLL2_MD", "@toll_md"),
            ("ITOLL2_PM", "@toll_pm"),
            ("ITOLL2_EV", "@toll_ev"),
            ("ITOLL3_EA", "@cost_auto_ea"),
            ("ITOLL3_AM", "@cost_auto_am"),
            ("ITOLL3_MD", "@cost_auto_md"),
            ("ITOLL3_PM", "@cost_auto_pm"),
            ("ITOLL3_EV", "@cost_auto_ev"),
            ("ITOLL4_EA", "@cost_med_truck_ea"),
            ("ITOLL4_AM", "@cost_med_truck_am"),
            ("ITOLL4_MD", "@cost_med_truck_md"),
            ("ITOLL4_PM", "@cost_med_truck_pm"),
            ("ITOLL4_EV", "@cost_med_truck_ev"),
            ("ITOLL5_EA", "@cost_hvy_truck_ea"),
            ("ITOLL5_AM", "@cost_hvy_truck_am"),
            ("ITOLL5_MD", "@cost_hvy_truck_md"),
            ("ITOLL5_PM", "@cost_hvy_truck_pm"),
            ("ITOLL5_EV", "@cost_hvy_truck_ev"),
            ("ABCP_EA", "@capacity_link_ea"),   ("BACP_EA", "@capacity_link_ea"),
            ("ABCP_AM", "@capacity_link_am"),   ("BACP_AM", "@capacity_link_am"),
            ("ABCP_MD", "@capacity_link_md"),   ("BACP_MD", "@capacity_link_md"),
            ("ABCP_PM", "@capacity_link_pm"),   ("BACP_PM", "@capacity_link_pm"),
            ("ABCP_EV", "@capacity_link_ev"),   ("BACP_EV", "@capacity_link_ev"),
            ("ABCX_EA", "@capacity_inter_ea"),  ("BACX_EA", "@capacity_inter_ea"),
            ("ABCX_AM", "@capacity_inter_am"),  ("BACX_AM", "@capacity_inter_am"),
            ("ABCX_MD", "@capacity_inter_md"),  ("BACX_MD", "@capacity_inter_md"),
            ("ABCX_PM", "@capacity_inter_pm"),  ("BACX_PM", "@capacity_inter_pm"),
            ("ABCX_EV", "@capacity_inter_ev"),  ("BACX_EV", "@capacity_inter_ev"),
            ("ABTM_EA", "@time_link_ea"),       ("BATM_EA", "@time_link_ea"),
            ("ABTM_AM", "@time_link_am"),       ("BATM_AM", "@time_link_am"),
            ("ABTM_MD", "@time_link_md"),       ("BATM_MD", "@time_link_md"),
            ("ABTM_PM", "@time_link_pm"),       ("BATM_PM", "@time_link_pm"),
            ("ABTM_EV", "@time_link_ev"),       ("BATM_EV", "@time_link_ev"),
            ("ABTX_EA", "@time_inter_ea"),      ("BATX_EA", "@time_inter_ea"),
            ("ABTX_AM", "@time_inter_am"),      ("BATX_AM", "@time_inter_am"),
            ("ABTX_MD", "@time_inter_md"),      ("BATX_MD", "@time_inter_md"),
            ("ABTX_PM", "@time_inter_pm"),      ("BATX_PM", "@time_inter_pm"),
            ("ABTX_EV", "@time_inter_ev"),      ("BATX_EV", "@time_inter_ev"),
            ("ABLN_EA", "@lane_ea"),            ("BALN_EA", "@lane_ea"),
            ("ABLN_AM", "@lane_am"),            ("BALN_AM", "@lane_am"),
            ("ABLN_MD", "@lane_md"),            ("BALN_MD", "@lane_md"),
            ("ABLN_PM", "@lane_pm"),            ("BALN_PM", "@lane_pm"),
            ("ABLN_EV", "@lane_ev"),            ("BALN_EV", "@lane_ev"),
            ("ABSTM_EA", "@auto_time_ea"),      ("BASTM_EA", "@auto_time_ea"),
            ("ABSTM_AM", "@auto_time_am"),      ("BASTM_AM", "@auto_time_am"),
            ("ABSTM_MD", "@auto_time_md"),      ("BASTM_MD", "@auto_time_md"),
            ("ABSTM_PM", "@auto_time_pm"),      ("BASTM_PM", "@auto_time_pm"),
            ("ABSTM_EV", "@auto_time_ev"),      ("BASTM_EV", "@auto_time_ev"),
            ("ABHTM_EA", "@auto_time_ea"),      ("BAHTM_EA", "@auto_time_ea"),
            ("ABHTM_AM", "@auto_time_am"),      ("BAHTM_AM", "@auto_time_am"),
            ("ABHTM_MD", "@auto_time_md"),      ("BAHTM_MD", "@auto_time_md"),
            ("ABHTM_PM", "@auto_time_pm"),      ("BAHTM_PM", "@auto_time_pm"),
            ("ABHTM_EV", "@auto_time_ev"),      ("BAHTM_EV", "@auto_time_ev"),
            ("ABPRELOAD_EA", "@volad_ea"),      ("BAPRELOAD_EA", "@volad_ea"),
            ("ABPRELOAD_AM", "@volad_am"),      ("BAPRELOAD_AM", "@volad_am"),
            ("ABPRELOAD_MD", "@volad_md"),      ("BAPRELOAD_MD", "@volad_md"),
            ("ABPRELOAD_PM", "@volad_pm"),      ("BAPRELOAD_PM", "@volad_pm"),
            ("ABPRELOAD_EV", "@volad_ev"),      ("BAPRELOAD_EV", "@volad_ev")
        ]
        hwylink_atts_scenario = [
            ("ABSTM_EA", "@auto_time_ea"),      ("BASTM_EA", "@auto_time_ea"),
            ("ABSTM_AM", "@auto_time_am"),      ("BASTM_AM", "@auto_time_am"),
            ("ABSTM_MD", "@auto_time_md"),      ("BASTM_MD", "@auto_time_md"),
            ("ABSTM_PM", "@auto_time_pm"),      ("BASTM_PM", "@auto_time_pm"),
            ("ABSTM_EV", "@auto_time_ev"),      ("BASTM_EV", "@auto_time_ev"),
            ("ABPRELOAD_EA", "@volad_ea"),      ("BAPRELOAD_EA", "@volad_ea"),
            ("ABPRELOAD_AM", "@volad_am"),      ("BAPRELOAD_AM", "@volad_am"),
            ("ABPRELOAD_MD", "@volad_md"),      ("BAPRELOAD_MD", "@volad_md"),
            ("ABPRELOAD_PM", "@volad_pm"),      ("BAPRELOAD_PM", "@volad_pm"),
            ("ABPRELOAD_EV", "@volad_ev"),      ("BAPRELOAD_EV", "@volad_ev")
        ]

        result_scenario = copy_scenario(
            base_scenario, result_scenario_id,
            "%s - result" % base_scenario.title,
            overwrite=True, set_as_primary=False)
        #copy assignment from period scenarios
        for key, dst_att in hwylink_atts_scenario:
            if key.startswith("BA"):
                continue
            p = key[-2:]
            create_attribute("LINK", dst_att, dst_att[1:], 0,
                             overwrite=True, scenario=result_scenario)
            from_scenario = traffic_emmebank.scenario(period_scenario_id[p])
            if "auto_time" in dst_att:
                src_att = "@auto_time"
            elif "volad" in dst_att:
                src_att = "volad"
            copy_attribute(from_attribute_name=src_att, to_attribute_name=dst_att,
                           from_scenario=from_scenario, to_scenario=result_scenario)
        net = result_scenario.get_partial_network(["LINK"], include_attributes=True)
        auto_mode = net.mode(auto_mode_id)
        # Get all the original forward direction links
        links = [link for link in net.links() if link["@tcov_id"] > 0]

        hwylink_atts_file = os.path.join(export_path, "hwy_tcad.csv")
        self.export_traffic_to_csv(hwylink_atts_file, hwylink_atts, links, auto_mode)
        # delete result_scenario
        delete_scenario(result_scenario)

    @_m.logbook_trace("Export traffic load data by period")
    def export_traffic_load_by_period(self, export_path, traffic_emmebank, period_scenario_id):
        create_attribute = _m.Modeller().tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        net_calculator = _m.Modeller().tool(
            "inro.emme.network_calculation.network_calculator")

        auto_mode_id = "d"
        hwyload_atts = [
            ("ID1", "@tcov_id"),
            ("AB_Flow_PCE", "@pce_flow"),   ("BA_Flow_PCE", "@pce_flow"), # sum of pce flow
            ("AB_Time", "@auto_time"),      ("BA_Time", "@auto_time"),  #computed vdf based on pce flow
            ("AB_VOC", "@voc"),             ("BA_VOC", "@voc"),
            ("AB_V_Dist_T", "length"),      ("BA_V_Dist_T", "length"),
            ("AB_VHT", "@vht"),             ("BA_VHT", "@vht"),
            ("AB_Speed", "@speed"),         ("BA_Speed", "@speed"),
            ("AB_VDF", "volume_delay_func"),("BA_VDF", "volume_delay_func"),
            ("AB_MSA_Flow", "@msa_flow"),   ("BA_MSA_Flow", "@msa_flow"),   #
            ("AB_MSA_Time", "@msa_time"),   ("BA_MSA_Time", "@msa_time"),   #
            ("AB_Flow_SOV_GP", "@sovgp"),   ("BA_Flow_SOV_GP", "@sovgp"),
            ("AB_Flow_SOV_PAY", "@sovtoll"),("BA_Flow_SOV_PAY", "@sovtoll"),
            ("AB_Flow_SR2_GP", "@hov2gp"),  ("BA_Flow_SR2_GP", "@hov2gp"),
            ("AB_Flow_SR2_HOV", "@hov2hov"),("BA_Flow_SR2_HOV", "@hov2hov"),
            ("AB_Flow_SR2_PAY", "@hov2toll"), ("BA_Flow_SR2_PAY", "@hov2toll"),
            ("AB_Flow_SR3_GP", "@hov3gp"),  ("BA_Flow_SR3_GP", "@hov3gp"),
            ("AB_Flow_SR3_HOV", "@hov3hov"),("BA_Flow_SR3_HOV", "@hov3hov"),
            ("AB_Flow_SR3_PAY", "@hov3toll"), ("BA_Flow_SR3_PAY", "@hov3toll"),
            ("AB_Flow_lhdn", "@trklgp"),    ("BA_Flow_lhdn", "@trklgp"),
            ("AB_Flow_mhdn", "@trkmgp"),    ("BA_Flow_mhdn", "@trkmgp"),
            ("AB_Flow_hhdn", "@trkhgp"),    ("BA_Flow_hhdn", "@trkhgp"),
            ("AB_Flow_lhdt", "@trkltoll"),  ("BA_Flow_lhdt", "@trkltoll"),
            ("AB_Flow_mhdt", "@trkmtoll"),  ("BA_Flow_mhdt", "@trkmtoll"),
            ("AB_Flow_hhdt", "@trkhtoll"),  ("BA_Flow_hhdt", "@trkhtoll"),
            ("AB_Flow", "@non_pce_flow"),   ("BA_Flow", "@non_pce_flow")
        ]
        for p, scen_id in period_scenario_id.iteritems():
            scenario = traffic_emmebank.scenario(scen_id)
            new_atts = [("@msa_flow", "MSA flow", "@auto_volume"), #updated with vdf on msa flow
                        ("@msa_time", "MSA time", "timau"),  #skim assignment time on msa flow
                        ("@voc", "volume over capacity", "@auto_volume/ul3"),
                        ("@vht", "vehicle hour traveled", "@auto_volume*timau/60"),
                        ("@speed", "link travel speed", "length*60/timau"),
                        ("@pce_flow", "total number of vehicles in Pce",
                                 "@sovgp+@sovtoll+ \
                                  @hov2gp+@hov2hov+@hov2toll+ \
                                  @hov3gp+@hov3hov+@hov3toll+ \
                                  (@trklgp+@trkltoll) + (@trkmgp+@trkltoll) + \
                                  (@trkhgp+@trkhtoll)" ),
                        ("@non_pce_flow", "total number of vehicles in non-Pce",
                                 "@sovgp+@sovtoll+ \
                                  @hov2gp+@hov2hov+@hov2toll+ \
                                  @hov3gp+@hov3hov+@hov3toll+ \
                                  (@trklgp+@trkltoll)/1.3 + (@trkmgp+@trkltoll)/1.5 + \
                                  (@trkhgp+@trkhtoll)/2.5" )
                        ]
            for name, des, formula in new_atts:
                att = scenario.extra_attribute(name)
                if not att:
                    att = create_attribute("LINK", name, des, 0, overwrite=True, scenario=scenario)
                cal_spec = {"result": att.id,
                            "expression": formula,
                            "aggregation": None,
                            "selections": {
                                "link": "mode=d"
                            },
                            "type": "NETWORK_CALCULATION"
                        }
                net_calculator(cal_spec, scenario=scenario)

            net = scenario.get_partial_network(["LINK"], include_attributes=True)
            auto_mode = net.mode(auto_mode_id)
            links = [link for link in net.links() if link["@tcov_id"] > 0]
            # highway link load
            self.export_traffic_to_csv(os.path.join(export_path, "hwyload_%s.csv" % p), hwyload_atts, links, auto_mode)

    def export_traffic_to_csv(self, filename, att_list, links, auto_mode):
        fout = open(filename, 'w')
        fout.write(",".join([x[0] for x in att_list]))
        fout.write("\n")
        for link in links:
            #auto only
            reverse_link = link.reverse_link
            if not auto_mode in link.modes or (reverse_link and not auto_mode in reverse_link.modes):
                continue
            key, att = att_list[0]     #link id
            link_id = int(link[att])
            if link_id == 0:
                link_id = int(link.i_node.number * 100000 + link.j_node.number)
            values = [link_id]
            for key, att in att_list[1:]:
                # special for highway atts
                if key == "AN":
                    values.append(link.i_node.id)
                elif key == "BN":
                    values.append(link.j_node.id)
                elif key == "IWAY":
                    values.append(2 if reverse_link else 1)
                # special for highway load
                elif key.startswith("AB"):
                    values.append(link[att])
                elif key.startswith("BA"):
                    values.append(reverse_link[att] if reverse_link else 0)
                else:
                    values.append(link[att])
            fout.write(",".join([str(x) for x in values]))
            fout.write("\n")
        fout.close()

    @_m.logbook_trace("Export transit results")
    def export_transit_results(self, export_path, transit_emmebank, period_scenario_id, num_processors):
        # Note: Node analysis for transfers is VERY time consuming
        #       this implementation will be replaced when new Emme version is available
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
            "COST"
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
        fout_seg = open(transit_flow_file, 'w')
        fout_seg.write(",".join(transit_flow_atts))
        fout_seg.write("\n")

        transit_aggregate_flow_file = os.path.join(export_path, "transit_aggflow.csv")
        fout_link = open(transit_aggregate_flow_file, 'w')
        fout_link.write(",".join(transit_aggregate_flow_atts))
        fout_link.write("\n")

        transit_onoff_file = os.path.join(export_path, "transit_onoff.csv")
        fout_stop = open(transit_onoff_file, 'w')
        fout_stop.write(",".join(transit_onoff_atts))
        fout_stop.write("\n")

        access_modes = ["WLK", "PNR", "KNR"]
        main_modes = ["BUS", "LRT", "CMR", "BRT", "EXP"]
        all_modes = ["b", "c", "e", "l", "r", "p", "y", "a", "w", "x"]
        local_bus_modes = ["b", "a", "w", "x"]
        for tod, scen_id in period_scenario_id.iteritems():
            with _m.logbook_trace("Processing period %s" % tod):
                scenario = transit_emmebank.scenario(scen_id)
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
                    mode = "LOC" if main_mode == "BUS" else main_mode
                    mode_list = local_bus_modes if main_mode == "BUS" else all_modes
                    for access_type in access_modes:
                        with _m.logbook_trace("Main mode %s access mode %s" % (main_mode, access_type)):
                            class_name = "%s_%s%s" % (tod, access_type, main_mode)
                            segment_results = {
                                "transit_volumes": segment_flow.id,
                                "initial_boardings": initial_boardings.id,
                                "total_boardings": total_boardings.id,
                                "final_alightings": final_alightings.id,
                                "total_alightings": total_alightings.id,
                                "transfer_boardings": xfer_boardings.id,
                                "transfer_alightings": xfer_alightings.id
                            }
                            self.calc_additional_results(
                                scenario, class_name, num_processors,
                                total_walk_flow, segment_results, link_transit_flow,
                                access_walk_flow, xfer_walk_flow, egress_walk_flow)

                            net = scenario.get_network()
                            # ===============================================
                            # analysis for nodes with/without walk option
                            if use_node_analysis_to_get_transit_transfers:
                                stop_on, stop_off = self.transfer_analysis(scenario, class_name, num_processors)
                            else:
                                stop_on, stop_off = {}, {}
                            # ===============================================
                            links = [link for link in net.links() if link["@tcov_id"] > 0]
                            lines = [line for line in net.transit_lines() if line.mode.id in mode_list]
                            
                            self.output_transit_flow(mode, access_type, tod, lines, segment_flow, fout_seg)
                            self.output_transit_aggregate_flow(
                                mode, access_type, tod, links,
                                link_transit_flow, total_walk_flow, access_walk_flow,
                                xfer_walk_flow, egress_walk_flow, fout_link)
                            self.output_transit_onoff(
                                mode, access_type, tod, lines, 
                                total_boardings, total_alightings, initial_boardings,
                                xfer_boardings, xfer_alightings, final_alightings,
                                stop_on, stop_off, fout_stop)
        fout_stop.close()
        fout_link.close()
        fout_seg.close()
        return

    def output_transit_flow(self, mode, access_type, tod, lines, segment_flow, fout_seg):
        # output segment data (transit_flow)
        for line in lines:
            total_length = 0
            for seg in line.segments():
                total_length += seg.link.length
            last_link_length = frommp = 0.0
            for seg in line.segments():
                from_stop, to_stop, centroid = seg.i_node.id, seg.j_node.id, "0"
                frommp += last_link_length
                last_link_length = seg.link.length
                tomp = total_length - frommp
                transit_flow = seg[segment_flow.id]
                cost = baseivtt = seg.transit_time
                fout_seg.write(",".join([str(x) for x in [
                                mode, access_type, tod, line["@route_id"], from_stop, to_stop,
                                centroid, frommp, tomp, transit_flow, baseivtt, cost]]))
                fout_seg.write("\n")

    def output_transit_aggregate_flow(self, mode, access_type, tod, links,
                                      link_transit_flow, total_walk_flow, access_walk_flow,
                                      xfer_walk_flow, egress_walk_flow, fout_link):
        # output link data (transit_aggregate_flow)
        for link in links:
            link_id = int(link["@tcov_id"])
            ab_transit_flow = link[link_transit_flow.id]
            ab_non_transit_flow = link[total_walk_flow.id]
            ab_total_flow = ab_transit_flow + ab_non_transit_flow
            ab_access_walk_flow = link[access_walk_flow.id]
            ab_xfer_walk_flow = link[xfer_walk_flow.id]
            ab_egress_walk_flow = link[egress_walk_flow.id]
            if link.reverse_link:
                ba_transit_flow = link.reverse_link[link_transit_flow.id]
                ba_non_transit_flow = link.reverse_link[total_walk_flow.id]
                ba_total_flow = ba_transit_flow + ba_non_transit_flow
                ba_access_walk_flow = link.reverse_link[access_walk_flow.id]
                ba_xfer_walk_flow = link.reverse_link[xfer_walk_flow.id]
                ba_egress_walk_flow = link.reverse_link[egress_walk_flow.id]
            else:
                ba_transit_flow = 0.0
                ba_non_transit_flow = 0.0
                ba_total_flow = 0.0
                ba_access_walk_flow = 0.0
                ba_xfer_walk_flow = 0.0
                ba_egress_walk_flow = 0.0

            fout_link.write(",".join([str(x) for x in [
                         mode, access_type, tod, link_id,
                         ab_transit_flow, ba_transit_flow,
                         ab_non_transit_flow, ba_non_transit_flow,
                         ab_total_flow, ba_total_flow,
                         ab_access_walk_flow, ba_access_walk_flow,
                         ab_xfer_walk_flow, ba_xfer_walk_flow,
                         ab_egress_walk_flow, ba_egress_walk_flow
                        ]]))
            fout_link.write("\n")

    def output_transit_onoff(self, mode, access_type, tod, lines, 
                             total_boardings, total_alightings, initial_boardings,
                             xfer_boardings, xfer_alightings, final_alightings,
                             stop_on, stop_off, fout_stop):
        # output stop data (transit_onoff)
        for line in lines:
            for seg in line.segments(True):
                if not (seg.allow_alightings or seg.allow_boardings):
                    continue
                stop = seg.i_node.id
                boardings = seg[total_boardings.id]
                alightings = seg[total_alightings.id]
                walk_access_on = seg[initial_boardings.id]
                direct_xfer_on = seg[xfer_boardings.id]
                walk_xfer_on = 0.0
                direct_xfer_off = seg[xfer_alightings.id]
                walk_xfer_off = 0.0
                if stop_on.has_key(stop):
                    if stop_on[stop].has_key(line.id):
                        if direct_xfer_on>0:
                            walk_xfer_on = direct_xfer_on - stop_on[stop][line.id]
                            direct_xfer_on = stop_on[stop][line.id]
                if stop_off.has_key(stop):
                    if stop_off[stop].has_key(line.id):
                        if direct_xfer_off>0:
                            walk_xfer_off = direct_xfer_off - stop_off[stop][line.id]
                            direct_xfer_off = stop_off[stop][line.id]

                egress_off = seg[final_alightings.id]
                fout_stop.write(",".join([str(x) for x in [
                                mode, access_type, tod, line["@route_id"], stop,
                                boardings, alightings, walk_access_on,
                                direct_xfer_on, walk_xfer_on, direct_xfer_off, walk_xfer_off,
                                egress_off]]))
                fout_stop.write("\n")

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
                    if not stop_on[stop].has_key(on_line):
                        stop_on[stop][on_line] = 0.0
                    stop_on[stop][on_line] += trip
        return stop_off, stop_on

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
