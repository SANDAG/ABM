#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export_to_csv.py                                                      ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import inro.emme.database.emmebank as _emmebank
import traceback as _traceback
# from copy import deepcopy as _copy
# from collections import defaultdict as _defaultdict
# import contextlib as _context
import math
import os
import sys


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class ExportToCSV(_m.Tool(), gen_utils.Snapshot):

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.attributes = []
        

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export to CSV"
        pb.description = """"""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self()
            run_msg = "Transit assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export to CSV", save_arguments=True)
    def __call__(self, main_emmebank, transit_emmebank, export_path, period_scenario_ids):
        modeller = _m.Modeller()

        # TODO: rename uses of these variables
        bank_traffic = main_emmebank
        bank_traffic_path = main_emmebank.path
        bank_transit = transit_emmebank
        bank_transit_path = transit_emmebank.path

        attrs = {
            "main_emmebank": main_emmebank, 
            "transit_emmebank": transit_emmebank, 
            "export_path": export_path,
            "period_scenario_ids": period_scenario_ids,
            "self": str(self)
        }
        gen_utils.log_snapshot("Export to CSV", str(self), attrs)

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

        hwylink_attrs = self.hwylink_attrs
        hwylink_attrs_scenario = self.hwylink_attrs_scenario
        hwyload_attrs = self.hwyload_attrs

        periods = ["EA", "AM", "MD", "PM", "EV"]
        scenario_id = period_scenario_ids
        base_scenario = bank_traffic.scenario(100)
        net = base_scenario.get_network()
        auto_mode = net.mode('d')


    def export_to_csv(self, filename, att_list, links, auto_mode):
        with open(filename, 'w') as fout:
            no_of_fields = len(att_list)
            #print "total number of fields:", no_of_fields
            for i in range(no_of_fields):
                key, att = att_list[i]
                fout.write("%s" % key)
                if i<no_of_fields-1:
                    fout.write(",")
                else:
                    fout.write("\n")
            lno = 0
            for link in links:        
                ##lno += 1
                ##if lno>=10:
                ##    break
                #auto only
                if not auto_mode in link.modes:
                    if link.reverse_link:
                        if not auto_mode in link.reverse_link.modes:
                            continue
                    else:
                        continue
                #
                key, att = att_list[0]     #link id
                link_id = link[att]
                if link_id==0:
                    link_id = link.i_node.number*100000 + link.j_node.number
                fout.write("%.0f" % link_id)
                for i in range(1, no_of_fields):
                    key, att = att_list[i]
                    # specil for highway atts
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
                    # speical for highway load
                    elif key=="AB_V_Dist_T":
                        value = link.length
                    elif key[:2]=="AB":
                        if att=="vdf":
                            value = link.volume_delay_func
                        else:
                            value = link[att]
                    elif key[:2]=="BA":
                        if link.reverse_link:
                            if att=="vdf":
                                value = link.reverse_link.volume_delay_func
                            else:
                                value = link.reverse_link[att]
                        else:
                            value = 0               #if no reverse link, value=0
                    else:
                        value = link[att]
                    fout.write(",%s" % value)
                fout.write("\n")

    @property
    def hwylink_attrs(self):
        hwylink_attrs = [
            ("ID", "@tcov_id"), 
            ("Length", "length"), 
            ("SPHERE", "@sphere"),
            ("NM", "#name"),
            ("FXNM", "#name_from"), 
            ("TXNM", "#name_to"),
            ("AN", "i"), 
            ("BN", "j"),
            ("ASPD", "@speed_adjusted"), 
            ("IYR", "@year_open_traffic"),
            ("IPROJ", "@project_code"), 
            ("IJUR", "@jurisdiction_type"),
            ("IFC", "type"), 
            ("IHOV", "@lane_restriction"),
            ("ITRUCK", "@truck_restriction"), 
            ("ISPD", "@speed_posted"),
            ("IWAY", "1/2 way"), 
            ("IMED", "@median"),
            ("ABAU", "@lane_auxiliary"), 
            ("ABCNT", "@traffic_control"),
            ("BAAU", "@lane_auxiliary"), 
            ("BACNT", "@traffic_control"),
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
            ("ABSTM_EA", "@timau_ea"),
            ("ABSTM_AM", "@timau_am"),
            ("ABSTM_MD", "@timau_md"),
            ("ABSTM_PM", "@timau_pm"),
            ("ABSTM_EV", "@timau_ev"),
            ("BASTM_EA", "@timau_ea"),
            ("BASTM_AM", "@timau_am"),
            ("BASTM_MD", "@timau_md"),
            ("BASTM_PM", "@timau_pm"),
            ("BASTM_EV", "@timau_ev"),
            ("ABHTM_EA", "@timau_ea"),
            ("ABHTM_AM", "@timau_am"),
            ("ABHTM_MD", "@timau_md"),
            ("ABHTM_PM", "@timau_pm"),
            ("ABHTM_EV", "@timau_ev"),
            ("BAHTM_EA", "@timau_ea"),
            ("BAHTM_AM", "@timau_am"),
            ("BAHTM_MD", "@timau_md"),
            ("BAHTM_PM", "@timau_pm"),
            ("BAHTM_EV", "@timau_ev"),
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
        return hwylink_attrs

    @property
    def hwylink_attrs_scenario(self):
        hwylink_attrs_scenario = [
            ("ABSTM_EA", "@timau_ea"),
            ("ABSTM_AM", "@timau_am"),
            ("ABSTM_MD", "@timau_md"),
            ("ABSTM_PM", "@timau_pm"),
            ("ABSTM_EV", "@timau_ev"),
            ("BASTM_EA", "@timau_ea"),
            ("BASTM_AM", "@timau_am"),
            ("BASTM_MD", "@timau_md"),
            ("BASTM_PM", "@timau_pm"),
            ("BASTM_EV", "@timau_ev"),
            #("ABHTM_EA", "@timau_ea"),
            #("ABHTM_AM", "@timau_am"),
            #("ABHTM_MD", "@timau_md"),
            #("ABHTM_PM", "@timau_pm"),
            #("ABHTM_EV", "@timau_ev"),
            #("BAHTM_EA", "@timau_ea"),
            #("BAHTM_AM", "@timau_am"),
            #("BAHTM_MD", "@timau_md"),
            #("BAHTM_PM", "@timau_pm"),
            #("BAHTM_EV", "@timau_ev"),
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
        return hwylink_attrs_scenario

    @property
    def hwyload_attrs(self):
        hwyload_attrs = [ 
            ("ID1", "@tcov_id"), 
            ("AB_Flow_PCE", "@volau"), 
            ("BA_Flow_PCE", "@volau"),
            ("AB_Time", "@timau"), 
            ("BA_Time", "@timau"),
            ("AB_VOC", "@voc"), 
            ("BA_VOC", "@voc"),
            ("AB_V_Dist_T", "length"), 
            ("BA_V_Dist_T", "length"),
            ("AB_VHT", "@vht"), 
            ("BA_VHT", "@vht"), 
            ("AB_Speed", "@speed"), 
            ("BA_Speed", "@speed"),
            ("AB_VDF", "vdf"), 
            ("BA_VDF", "vdf"),
            ("AB_MSA_Flow", "@msa_flow"), 
            ("BA_MSA_Flow", "@msa_flow"), 
            ("AB_MSA_Time", "@msa_time"), 
            ("BA_MSA_Time", "@msa_time"),
            ("AB_Flow_SOV_GP", "@sovgp"), 
            ("BA_Flow_SOV_GP", "@sovgp"),
            ("AB_Flow_SOV_PAY", "@sovtoll"), 
            ("BA_Flow_SOV_PAY", "@sovtoll"),
            ("AB_Flow_SR2_GP", "@hov2gp"), 
            ("BA_Flow_SR2_GP", "@hov2gp"),
            ("AB_Flow_SR2_HOV", "@hov2hov"), 
            ("BA_Flow_SR2_HOV", "@hov2hov"),
            ("AB_Flow_SR2_PAY", "@hov2toll"), 
            ("BA_Flow_SR2_PAY", "@hov2toll"),
            ("AB_Flow_SR3_GP", "@hov3gp"), 
            ("BA_Flow_SR3_GP", "@hov3gp"),
            ("AB_Flow_SR3_HOV", "@hov3hov"), 
            ("BA_Flow_SR3_HOV", "@hov3hov"),
            ("AB_Flow_SR3_PAY", "@hov3toll"),
            ("BA_Flow_SR3_PAY", "@hov3toll"),
            ("AB_Flow_lhdn", "@trklgp"), 
            ("BA_Flow_lhdn", "@trklgp"),
            ("AB_Flow_mhdn", "@trkmgp"), 
            ("BA_Flow_mhdn", "@trkmgp"),
            ("AB_Flow_hhdn", "@trkhgp"), 
            ("BA_Flow_hhdn", "@trkhgp"),
            ("AB_Flow_lhdt", "@trkltoll"), 
            ("BA_Flow_lhdt", "@trkltoll"),
            ("AB_Flow_mhdt", "@trkmtoll"), 
            ("BA_Flow_mhdt", "@trkmtoll"),
            ("AB_Flow_hhdt", "@trkhtoll"), 
            ("BA_Flow_hhdt", "@trkhtoll"),
            ("AB_Flow", "@non_pce_flow"),
            ("BA_Flow", "@non_pce_flow")
        ]
        return hwyload_attrs