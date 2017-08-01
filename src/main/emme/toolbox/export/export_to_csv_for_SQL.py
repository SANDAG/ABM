#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export_to_csv_for_SQL.py                                              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////



import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.core.exception as _except
from contextlib import contextmanager as _context
import os

gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

modeller = _m.Modeller()


class ExportToCSV(_m.Tool(), gen_utils.Snapshot):

    main_directory = _m.Attribute(str)
    traffic_emmebank = _m.Attribute(str)
    transit_emmebank = _m.Attribute(str)
    num_processors = _m.Attribute(str)
    
    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.traffic_emmebank = os.path.join(project_dir, "Database", "emmebank")
        self.transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")        
        self.num_processors = "MAX-1"
        self.attributes = ["main_directory", "traffic_emmebank", "transit_emmebank", "num_processors"]

        # TODO: refactor these settings
        self.transit_skims_only = True
        #
        self.auto_mode_id = "d"
        self.periods = ["EA", "AM", "MD", "PM", "EV"]
        self.scenario_id = {"EA": 101, "AM": 102, "MD": 103, "PM": 104, "EV": 105}

        self.result_scenario_id = 106        #create/delete
        self.use_node_analysis_to_get_transit_transfers = False
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export to csv for SQL"
        pb.description = """
Export model results to csv files for SQL data loader."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
            
        pb.add_select_file('main_directory', 'directory',
                           title='Select main directory')
        pb.add_select_file('traffic_emmebank', 'file',
                           title='Select traffic emmebank')
        pb.add_select_file('transit_emmebank', 'file',
                           title='Select transit emmebank')
                           
        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            results = self(self.main_directory, self.traffic_emmebank, self.transit_emmebank,
                self.num_processors)
            run_msg = "Export to csv completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export network results to CSV")
    def __call__(self, main_directory, traffic_emmebank, transit_emmebank, num_processors):
        #
        copy_scenario = modeller.tool(
            "inro.emme.data.scenario.copy_scenario")
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        net_calculator = modeller.tool(
            "inro.emme.network_calculation.network_calculator")
        copy_attribute= modeller.tool(
            "inro.emme.data.network.copy_attribute")
        delete_scenario = modeller.tool(
            "inro.emme.data.scenario.delete_scenario")
        network_results = modeller.tool(
            "inro.emme.transit_assignment.extended.network_results")
        path_based_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.path_based_analysis")
        transfers_at_stops = modeller.tool(
            "inro.emme.transit_assignment.extended.apps.transfers_at_stops")    

        attrs = {
            "traffic_emmebank": str(traffic_emmebank), 
            "transit_emmebank": str(transit_emmebank), 
            "main_directory": main_directory,
            #"period_scenario_ids": period_scenario_ids,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export to CSV", str(self), attrs)
        
        
        bank_traffic = _eb.Emmebank(traffic_emmebank)
        bank_transit = _eb.Emmebank(transit_emmebank)
        export_path = os.path.join(main_directory, "output")
        num_processors = dem_utils.parse_num_processors(num_processors)
        
        # TODO refactor these ...
        transit_skims_only = self.transit_skims_only
        
        auto_mode_id = self.auto_mode_id
        periods = self.periods
        scenario_id = self.scenario_id
        base_scenario = bank_traffic.scenario(100)
        result_scenario_id = self.result_scenario_id
        
        use_node_analysis_to_get_transit_transfers = self.use_node_analysis_to_get_transit_transfers
        #
        net = base_scenario.get_network()
        auto_mode = net.mode(auto_mode_id)        
        #
        with _m.logbook_trace("Export traffic attribute data"):
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
                ("ABCP_EA", "@capacity_link_ea"), 
                ("ABCP_AM", "@capacity_link_am"),
                ("ABCP_MD", "@capacity_link_md"),
                ("ABCP_PM", "@capacity_link_pm"),
                ("ABCP_EV", "@capacity_link_ev"),
                ("BACP_EA", "@capacity_link_ea"),
                ("BACP_AM", "@capacity_link_am"),
                ("BACP_MD", "@capacity_link_md"),
                ("BACP_PM", "@capacity_link_pm"),
                ("BACP_EV", "@capacity_link_ev"),
                ("ABCX_EA", "@capacity_inter_ea"),
                ("ABCX_AM", "@capacity_inter_am"),
                ("ABCX_MD", "@capacity_inter_md"),
                ("ABCX_PM", "@capacity_inter_pm"),
                ("ABCX_EV", "@capacity_inter_ev"),
                ("BACX_EA", "@capacity_inter_ea"),
                ("BACX_AM", "@capacity_inter_am"),
                ("BACX_MD", "@capacity_inter_md"),
                ("BACX_PM", "@capacity_inter_pm"),
                ("BACX_EV", "@capacity_inter_ev"),
                ("ABTM_EA", "@time_link_ea"),
                ("ABTM_AM", "@time_link_am"),
                ("ABTM_MD", "@time_link_md"),
                ("ABTM_PM", "@time_link_pm"),
                ("ABTM_EV", "@time_link_ev"),
                ("BATM_EA", "@time_link_ea"),
                ("BATM_AM", "@time_link_am"),
                ("BATM_MD", "@time_link_md"),
                ("BATM_PM", "@time_link_pm"),
                ("BATM_EV", "@time_link_ev"),
                ("ABTX_EA", "@time_inter_ea"),
                ("ABTX_AM", "@time_inter_am"),
                ("ABTX_MD", "@time_inter_md"),
                ("ABTX_PM", "@time_inter_pm"),
                ("ABTX_EV", "@time_inter_ev"),
                ("BATX_EA", "@time_inter_ea"),
                ("BATX_AM", "@time_inter_am"),
                ("BATX_MD", "@time_inter_md"),
                ("BATX_PM", "@time_inter_pm"),
                ("BATX_EV", "@time_inter_ev"),
                ("ABLN_EA", "@lane_ea"),
                ("ABLN_AM", "@lane_am"),
                ("ABLN_MD", "@lane_md"),
                ("ABLN_PM", "@lane_pm"),
                ("ABLN_EV", "@lane_ev"),
                ("BALN_EA", "@lane_ea"),
                ("BALN_AM", "@lane_am"),
                ("BALN_MD", "@lane_md"),
                ("BALN_PM", "@lane_pm"),
                ("BALN_EV", "@lane_ev"),
                ("ABSTM_EA", "@auto_time_ea"),
                ("ABSTM_AM", "@auto_time_am"),
                ("ABSTM_MD", "@auto_time_md"),
                ("ABSTM_PM", "@auto_time_pm"),
                ("ABSTM_EV", "@auto_time_ev"),
                ("BASTM_EA", "@auto_time_ea"),
                ("BASTM_AM", "@auto_time_am"),
                ("BASTM_MD", "@auto_time_md"),
                ("BASTM_PM", "@auto_time_pm"),
                ("BASTM_EV", "@auto_time_ev"),
                ("ABHTM_EA", "@auto_time_ea"),
                ("ABHTM_AM", "@auto_time_am"),
                ("ABHTM_MD", "@auto_time_md"),
                ("ABHTM_PM", "@auto_time_pm"),
                ("ABHTM_EV", "@auto_time_ev"),
                ("BAHTM_EA", "@auto_time_ea"),
                ("BAHTM_AM", "@auto_time_am"),
                ("BAHTM_MD", "@auto_time_md"),
                ("BAHTM_PM", "@auto_time_pm"),
                ("BAHTM_EV", "@auto_time_ev"),
                ("ABPRELOAD_EA", "@volad_ea"),
                ("BAPRELOAD_EA", "@volad_ea"),
                ("ABPRELOAD_AM", "@volad_am"),
                ("BAPRELOAD_AM", "@volad_am"),
                ("ABPRELOAD_MD", "@volad_md"),
                ("BAPRELOAD_MD", "@volad_md"),
                ("ABPRELOAD_PM", "@volad_pm"),
                ("BAPRELOAD_PM", "@volad_pm"),
                ("ABPRELOAD_EV", "@volad_ev"),
                ("BAPRELOAD_EV", "@volad_ev")
            ]
            hwylink_atts_scenario = [
                ("ABSTM_EA", "@auto_time_ea"),
                ("ABSTM_AM", "@auto_time_am"),
                ("ABSTM_MD", "@auto_time_md"),
                ("ABSTM_PM", "@auto_time_pm"),
                ("ABSTM_EV", "@auto_time_ev"),
                ("BASTM_EA", "@auto_time_ea"),
                ("BASTM_AM", "@auto_time_am"),
                ("BASTM_MD", "@auto_time_md"),
                ("BASTM_PM", "@auto_time_pm"),
                ("BASTM_EV", "@auto_time_ev"),
                ("ABPRELOAD_EA", "@volad_ea"),
                ("BAPRELOAD_EA", "@volad_ea"),
                ("ABPRELOAD_AM", "@volad_am"),
                ("BAPRELOAD_AM", "@volad_am"),
                ("ABPRELOAD_MD", "@volad_md"),
                ("BAPRELOAD_MD", "@volad_md"),
                ("ABPRELOAD_PM", "@volad_pm"),
                ("BAPRELOAD_PM", "@volad_pm"),
                ("ABPRELOAD_EV", "@volad_ev"),
                ("BAPRELOAD_EV", "@volad_ev")
            ]        

            # 
            scenario_exist = False
            for p in periods:
                if result_scenario_id==scenario_id[p]:
                    scenario_exist = True
                    raise _except.ArgumentError(
                        "Scenario %s is used by assignments!!!" % result_scenario_id)
            if not scenario_exist:
                result_scenario = copy_scenario(base_scenario, result_scenario_id, 
                                    "%s - result" % base_scenario.title,
                                    overwrite=True, set_as_primary=False)
            #copy assignment from period scenarios
            for key, att in hwylink_atts_scenario:
                if key[:2]=="BA":
                    continue
                p = key[-2:]
                create_attribute("LINK", att, att[1:], 
                            0, overwrite=True, scenario=result_scenario)        
                from_scenario = bank_traffic.scenario(scenario_id[p]) 
                if "auto_time" in att:
                    emme_att = att[:-3]   #@auto_time
                elif "volad" in att:
                    emme_att = att[1:-3]  #volad
                #    
                copy_attribute(from_attribute_name=emme_att, to_attribute_name=att, selection={},
                               from_scenario=from_scenario, to_scenario=result_scenario)
            net = result_scenario.get_network()
            auto_mode = net.mode(auto_mode_id)
            links = []
            for link in net.links():
                link_id = link["@tcov_id"]
                if link_id>0:
                    links.append(link)        
            # highway link attributes
            # TODO: find example of this file ... 
            hwylink_atts_file = os.path.join(export_path, "hwylink_attr.csv")
            self.export_traffic_to_csv(hwylink_atts_file, hwylink_atts, links, auto_mode)
            # delete result_scenario
            delete_scenario(result_scenario)            
            #
        with _m.logbook_trace("Export traffic load data by period"):
            hwyload_atts = [ ("ID1", "@tcov_id"), 
                        ("AB_Flow_PCE", "@pce_flow"), ("BA_Flow_PCE", "@pce_flow"), # sum of pce flow
                        ("AB_Time", "@auto_time"), ("BA_Time", "@auto_time"),  #computed vdf based on pce flow
                        ("AB_VOC", "@voc"), ("BA_VOC", "@voc"),
                        ("AB_V_Dist_T", "length"), ("BA_V_Dist_T", "length"),
                        ("AB_VHT", "@vht"), ("BA_VHT", "@vht"), 
                        ("AB_Speed", "@speed"), ("BA_Speed", "@speed"),
                        ("AB_VDF", "vdf"), ("BA_VDF", "vdf"),
                        ("AB_MSA_Flow", "@msa_flow"), ("BA_MSA_Flow", "@msa_flow"),   #
                        ("AB_MSA_Time", "@msa_time"), ("BA_MSA_Time", "@msa_time"),   #
                        ("AB_Flow_SOV_GP", "@sovgp"), ("BA_Flow_SOV_GP", "@sovgp"),
                        ("AB_Flow_SOV_PAY", "@sovtoll"), ("BA_Flow_SOV_PAY", "@sovtoll"),
                        ("AB_Flow_SR2_GP", "@hov2gp"), ("BA_Flow_SR2_GP", "@hov2gp"),
                        ("AB_Flow_SR2_HOV", "@hov2hov"), ("BA_Flow_SR2_HOV", "@hov2hov"),
                        ("AB_Flow_SR2_PAY", "@hov2toll"), ("BA_Flow_SR2_PAY", "@hov2toll"),
                        ("AB_Flow_SR3_GP", "@hov3gp"), ("BA_Flow_SR3_GP", "@hov3gp"),
                        ("AB_Flow_SR3_HOV", "@hov3hov"), ("BA_Flow_SR3_HOV", "@hov3hov"),
                        ("AB_Flow_SR3_PAY", "@hov3toll"), ("BA_Flow_SR3_PAY", "@hov3toll"),
                        ("AB_Flow_lhdn", "@trklgp"), ("BA_Flow_lhdn", "@trklgp"),
                        ("AB_Flow_mhdn", "@trkmgp"), ("BA_Flow_mhdn", "@trkmgp"),
                        ("AB_Flow_hhdn", "@trkhgp"), ("BA_Flow_hhdn", "@trkhgp"),
                        ("AB_Flow_lhdt", "@trkltoll"), ("BA_Flow_lhdt", "@trkltoll"),
                        ("AB_Flow_mhdt", "@trkmtoll"), ("BA_Flow_mhdt", "@trkmtoll"),
                        ("AB_Flow_hhdt", "@trkhtoll"), ("BA_Flow_hhdt", "@trkhtoll"),
                        ("AB_Flow", "@non_pce_flow"),
                        ("BA_Flow", "@non_pce_flow")
                       ]
            for p in periods:
                cur_scen = bank_traffic.scenario(scenario_id[p]) 
                #
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
                    att = cur_scen.extra_attribute(name)
                    if not att:
                        att = create_attribute("LINK", name, des, 
                                    0, overwrite=True, scenario=cur_scen) 
                    cal_spec = {"result": att.id,
                                "expression": formula,
                                "aggregation": None,
                                "selections": {
                                    "link": "mode=%s" % auto_mode.id
                                },
                                "type": "NETWORK_CALCULATION"
                            }
                    net_calculator(cal_spec, scenario=cur_scen)    

                    #
                net = cur_scen.get_network()
                auto_mode = net.mode(auto_mode_id)
                links = []
                for link in net.links():
                    link_id = link["@tcov_id"]
                    if link_id>0:
                        links.append(link)        
                # highway link load
                self.export_traffic_to_csv(os.path.join(export_path, "hwyload_%s_attr.csv" % p), hwyload_atts, links, auto_mode)

        with _m.logbook_trace("Export transit results"):
            transit_flow_atts = [ 
                #"MODE",
                #"ACCESSMODE",
                #"TOD",
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
                #"MODE",
                #"ACCESSMODE",
                #"TOD",
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
                #"MODE",
                #"ACCESSMODE",
                #"TOD",
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

            for p in periods:
                cur_scen = bank_transit.scenario(scenario_id[p]) 
                tod = p
                # find stop with/without walk transfer option
                stop_walk_list = []  # stop (id) with walk option
                att = "@stop_flag"
                stop_flag = create_attribute("NODE", att, "1=stop without walk option, 2=otherwise", 
                            0, overwrite=True, scenario=cur_scen)            
                stop_nline_att = "@stop_nline"
                stop_lines = create_attribute("NODE", stop_nline_att, "number of lines on the stop", 
                            0, overwrite=True, scenario=cur_scen)            
                net = cur_scen.get_network()
                for line in net.transit_lines():
                    for seg in line.segments(True):
                        node = seg.i_node
                        if seg.allow_alightings or seg.allow_boardings:
                            node[stop_nline_att] += 1
                        if node[att]>0:  #node checked
                            continue
                        if seg.allow_alightings or seg.allow_boardings:
                            node[att] = 1
                            for ilink in node.incoming_links():
                                # skip connector
                                if ilink.i_node.is_centroid:
                                    continue
                                for m in ilink.modes:
                                    if m.type=="AUX_TRANSIT":
                                        node[att] = 2
                                        stop_walk_list.append(node.id)
                                        break
                                if node[att]>=2:
                                    break
                            if node[att]>=2:
                                continue                    
                            for olink in node.outgoing_links():
                                # skip connector
                                if olink.j_node.is_centroid:
                                    continue
                                for m in olink.modes:
                                    if m.type=="AUX_TRANSIT":
                                        node[att] = 2
                                        stop_walk_list.append(node.id)
                                        break
                                if node[att]>=2:
                                    break                
                cur_scen.publish_network(net)
                #
                # attributes
                total_walk_flow = create_attribute("LINK", "@volax", "total walk flow on links", 
                            0, overwrite=True, scenario=cur_scen)        
                segment_flow = create_attribute("TRANSIT_SEGMENT", "@voltr", "transit segment flow", 
                            0, overwrite=True, scenario=cur_scen)  
                link_transit_flow = create_attribute("LINK", "@link_voltr", "total transit flow on link", 
                            0, overwrite=True, scenario=cur_scen)  
                initial_boardings = create_attribute("TRANSIT_SEGMENT", 
                            "@init_boardings", "transit initial boardings", 
                            0, overwrite=True, scenario=cur_scen)  
                xfer_boardings = create_attribute("TRANSIT_SEGMENT", 
                            "@xfer_boardings", "transit transfer boardings", 
                            0, overwrite=True, scenario=cur_scen)  
                total_boardings = create_attribute("TRANSIT_SEGMENT", 
                            "@total_boardings", "transit total boardings", 
                            0, overwrite=True, scenario=cur_scen)  
                final_alightings = create_attribute("TRANSIT_SEGMENT", 
                            "@final_alightings", "transit final alightings", 
                            0, overwrite=True, scenario=cur_scen)  
                xfer_alightings = create_attribute("TRANSIT_SEGMENT", 
                            "@xfer_alightings", "transit transfer alightings", 
                            0, overwrite=True, scenario=cur_scen)  
                total_alightings = create_attribute("TRANSIT_SEGMENT", 
                            "@total_alightings", "transit total alightings", 
                            0, overwrite=True, scenario=cur_scen)  
                #
                access_walk_flow = create_attribute("LINK", 
                            "@access_walk_flow", "access walks (orig to init board)", 
                            0, overwrite=True, scenario=cur_scen)  
                xfer_walk_flow = create_attribute("LINK", 
                            "@xfer_walk_flow", "xfer walks (init board to final aligt)", 
                            0, overwrite=True, scenario=cur_scen)  
                egress_walk_flow = create_attribute("LINK", 
                            "@egress_walk_flow", "egress walks (final aligt to dest)", 
                            0, overwrite=True, scenario=cur_scen)  
                #
                skims_only = transit_skims_only
                access_modes = ["WLK"] if skims_only else ["WLK", "PNR", "KNR"]
                main_modes = ["BUS", "LRT"] if skims_only else ["BUS", "LRT", "CMR", "BRT", "EXP"]
                all_modes = ["b", "c", "e", "l", "r", "p", "y", "a", "w", "x"]
                local_bus_modes = ["b", "a", "w", "x"]
                for main_mode in main_modes: 
                    mode = main_mode
                    mode_list = all_modes
                    if mode=="BUS":
                        mode = "LOC"
                        mode_list = local_bus_modes
                    for access_type in access_modes:
                        legacy_class_name = "%s_%s_%s" % (access_type, mode, p)
                        transit_flow_file = os.path.join(export_path, "flow%s.csv" % legacy_class_name)
                        fout_seg = open(transit_flow_file, 'w')
                        fout_seg.write(",".join(['"%s"' % x for x in transit_flow_atts]))
                        fout_seg.write("\n")
                        
                        transit_aggregate_flow_file = os.path.join(export_path, "agg%s.csv" % legacy_class_name)
                        fout_link = open(transit_aggregate_flow_file, 'w')
                        fout_link.write(",".join(['"%s"' % x for x in transit_aggregate_flow_atts]))
                        fout_link.write("\n")

                        transit_onoff_file = os.path.join(export_path, "ono%s.csv" % legacy_class_name)
                        fout_stop = open(transit_onoff_file, 'w')
                        fout_stop.write(",".join(['"%s"' % x for x in transit_onoff_atts]))
                        fout_stop.write("\n")

                        class_name = "%s_%s%s" % (p, access_type, main_mode)
                        # extend transit analysis
                        # network results
                        spec = {
                                "on_links": {
                                    "aux_transit_volumes": "%s" % total_walk_flow.id
                                },
                                "on_segments": {
                                    "transit_volumes": "%s" % segment_flow.id,
                                    "initial_boardings": "%s" % initial_boardings.id,
                                    "total_boardings": "%s" % total_boardings.id,
                                    "final_alightings": "%s" % final_alightings.id,
                                    "total_alightings": "%s" % total_alightings.id,
                                    "transfer_boardings": "%s" % xfer_boardings.id,
                                    "transfer_alightings": "%s" % xfer_alightings.id
                                },
                                "aggregated_from_segments": None,
                                "analyzed_demand": None,
                                "constraint": None,
                                "type": "EXTENDED_TRANSIT_NETWORK_RESULTS"
                            }
                        network_results(specification=spec, scenario=cur_scen, 
                                        class_name=class_name, num_processors=num_processors)
                        cal_spec = {
                                    "result": "%s" % link_transit_flow.id,
                                    "expression": "%s" % segment_flow.id,
                                    "aggregation": "+",
                                    "selections": {
                                        "link": "all",
                                        "transit_line": "all"
                                    },
                                    "type": "NETWORK_CALCULATION"
                                    }
                        net_calculator(cal_spec, scenario=cur_scen)    
                        #
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
                            path_based_analysis(specification=spec, scenario=cur_scen, 
                                            class_name=class_name, num_processors=num_processors)
                        #
                        # analysis for nodes with/without walk option ==
                        stop_off = {}
                        stop_on = {}
                        # if do node analysis
                        node_list = []
                        if use_node_analysis_to_get_transit_transfers:
                            node_list = stop_walk_list
                        for stop in node_list:
                            stop_off[stop] = {}
                            stop_on[stop] = {}
                            selection = "i=%s" % stop
                            results = transfers_at_stops(selection, scenario=cur_scen, 
                                    class_name=class_name, num_processors=num_processors)
                            for off_line in results:
                                stop_off[stop][off_line] = 0.0
                                for on_line in results[off_line]:
                                    trip = float(results[off_line][on_line])
                                    stop_off[stop][off_line] += trip
                                    #
                                    if not stop_on[stop].has_key(on_line):
                                        stop_on[stop][on_line] = 0.0
                                    stop_on[stop][on_line] += trip
                        # ===============================================
                        net = cur_scen.get_network()
                        links = []
                        for link in net.links():
                            link_id = link["@tcov_id"]
                            if link_id>0:
                                links.append(link)        
                        # output segment data (transit_flow)
                        for line in net.transit_lines():
                            if not line.mode.id in mode_list:
                                continue
                            total_length = 0
                            for seg in line.segments():
                                total_length += seg.link.length
                            frommp = 0.0
                            last_link_length = 0.0
                            for seg in line.segments():
                                from_stop = seg.i_node.id
                                to_stop = seg.j_node.id
                                centroid = "0"
                                frommp += last_link_length 
                                last_link_length = seg.link.length
                                tomp = total_length - frommp
                                att = "%s" % segment_flow.id
                                transit_flow = seg[att]
                                baseivtt = seg.transit_time      # suppose the same for all classes
                                cost = baseivtt
                                fout_seg.write(",".join([str(x) for x in [
                                                line.id, from_stop, to_stop, centroid, frommp, tomp, 
                                                transit_flow, baseivtt, cost]]))
                                fout_seg.write("\n")
                        # output link data (transit_aggregate_flow)
                        for link in links:
                            link_id = int(link["@tcov_id"])
                            ab_transit_flow = link[link_transit_flow.id]
                            ba_transit_flow = 0.0
                            ab_non_transit_flow = link[total_walk_flow.id]
                            ba_non_transit_flow = 0.0
                            ab_total_flow = ab_transit_flow + ab_non_transit_flow
                            ba_total_flow = 0.0
                            #
                            ab_access_walk_flow = link[access_walk_flow.id]
                            ba_access_walk_flow = 0.0
                            ab_xfer_walk_flow = link[xfer_walk_flow.id]
                            ba_xfer_walk_flow = 0.0
                            ab_egress_walk_flow = link[egress_walk_flow.id]
                            ba_egress_walk_flow = 0.0
                            #
                            if link.reverse_link:
                                ba_transit_flow = link.reverse_link[link_transit_flow.id]
                                ba_non_transit_flow = link.reverse_link[total_walk_flow.id]
                                ba_total_flow = ba_transit_flow + ba_non_transit_flow
                                #
                                ba_access_walk_flow = link.reverse_link[access_walk_flow.id]
                                ba_xfer_walk_flow = link.reverse_link[xfer_walk_flow.id]
                                ba_egress_walk_flow = link.reverse_link[egress_walk_flow.id]
                                
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
                        # output stop data (transit_onoff)
                        for line in net.transit_lines():
                            if not line.mode.id in mode_list:
                                continue
                            for seg in line.segments(True):
                                if not (seg.allow_alightings or seg.allow_boardings):
                                    continue
                                stop = seg.i_node.id
                                boardings = seg[total_boardings.id]
                                alightings = seg[total_alightings.id]
                                walk_access_on = seg[initial_boardings.id]
                                #
                                # use node based analysis ==
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
                                # =============================
                                egress_off = seg[final_alightings.id]
                                fout_stop.write(",".join([str(x) for x in [
                                                mode, access_type, tod, line.id, stop, 
                                                boardings, alightings, 
                                                walk_access_on,
                                                direct_xfer_on, walk_xfer_on,
                                                direct_xfer_off, walk_xfer_off,
                                                egress_off]]))
                                fout_stop.write("\n")
                        fout_stop.close()
                        fout_link.close()
                        fout_seg.close()
        return

    def export_traffic_to_csv(self, filename, att_list, links, auto_mode):
        fout = open(filename, 'w')
        no_of_fields = len(att_list)
        fout.write(",".join([x[0] for x in att_list]))
        fout.write("\n")
        lno = 0
        for link in links:
            #auto only
            if not auto_mode in link.modes:
                if link.reverse_link:
                    if not auto_mode in link.reverse_link.modes:
                        continue
                else:
                    continue

            key, att = att_list[0]     #link id
            link_id = link[att]
            if link_id == 0:
                link_id = link.i_node.number * 100000 + link.j_node.number
            fout.write("%.0f" % link_id)
            for i in range(1, no_of_fields):
                key, att = att_list[i]
                # special for highway atts
                if key=="Length":
                    value = link.length
                elif key=="AN":
                    value = link.i_node.id
                elif key=="BN":
                    value = link.j_node.id
                elif key=="IWAY":
                    value = 1
                    if link.reverse_link:
                        value = 2
                # special for highway load
                elif key[:2]=="AB":
                    if att=="vdf":
                        value = link.volume_delay_func
                    elif key=="AB_V_Dist_T":
                        value = link.length
                    else:
                        value = link[att]
                elif key[:2]=="BA":
                    if link.reverse_link:
                        if att=="vdf":
                            value = link.reverse_link.volume_delay_func
                        elif key=="BA_V_Dist_T":
                            value = link.reverse_link.length
                        else:
                            value = link.reverse_link[att]
                    else:
                        value = 0               #if no reverse link, value=0
                else:
                    value = link[att]
                fout.write(",%s" % value)

            fout.write("\n")
        fout.close()
            
        
    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
    