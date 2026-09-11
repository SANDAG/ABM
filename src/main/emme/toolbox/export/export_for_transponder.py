#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2019-2020.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// export_for_transponder.py                                             ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#


TOOLBOX_ORDER = 57


import inro.modeller as _m

import numpy as _np
import pandas as _pd
import string as _string
import traceback as _traceback
import math
import os
_dir, _join = os.path.dirname, os.path.join

from shapely.geometry import MultiLineString, Point, LineString
from contextlib import contextmanager as _context


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module('sandag.utilities.demand')


class ExportForTransponder(_m.Tool(), gen_utils.Snapshot):

    scenario = _m.Attribute(object)
    output_directory = _m.Attribute(str)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = _dir(_m.Modeller().desktop.project.path)
        modeller = _m.Modeller()
        if modeller.emmebank.path == _join(project_dir, "Database", "emmebank"):
            self.scenario = modeller.emmebank.scenario(102)
        self.num_processors = "max-1"
        self.output_directory = _join(_dir(project_dir), "output")
        self.attributes = ["scenario", "output_directory", "num_processors"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Export for transponder ownership model"
        pb.description = """
<p>Calculates and exports the following results for each origin zone:</p>
<ul align="left">
<li>"MLDIST" - Managed lane distance - straight-line distance to the 
    nearest managed lane facility
<li>"AVGTTS" - Average travel time savings - average travel time savings 
    across all possible destinations.
<li>"PCTDETOUR" - Percent detour - The percent difference between the AM 
    transponder travel time and the AM non-transponder travel time 
    to sample zones when the general purpose lanes parallel to all toll 
    lanes using transponders are not available.
</ul>
 ."""
        pb.branding_text = "- SANDAG - Export"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_scenario("scenario", 
            title="Representative scenario")
        pb.add_select_file('output_directory', 'directory',
                           title='Select output directory')

        dem_utils.add_select_processors("num_processors", pb, self)
        return pb.render()


    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.output_directory, self.num_processors, self.scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Export results for transponder ownership model", save_arguments=True)
    def __call__(self, output_directory, num_processors, scenario):
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(
            _join(_dir(output_directory), "conf", "sandag_abm.properties"))
        input_directory = _join(_dir(output_directory), "input")
        num_processors = dem_utils.parse_num_processors(num_processors)
        network = scenario.get_network()
        distances = self.ml_facility_dist(network)
        savings = self.avg_travel_time_savings(scenario, input_directory, props, num_processors)
        detour = self.percent_detour(scenario, network, props, num_processors)
        self.export_results(output_directory, scenario, distances, savings, detour)

    @_m.logbook_trace("Calculate distance to nearest managed lane facility")
    def ml_facility_dist(self, network):
        # DIST: Straight line distance to the nearest ML facility (nearest link with a ML Cost)
        # managed lane is :
        #   HOV2+ only (carpool lane): "IFC" = 1 and "IHOV" = 2 and "ITOLLO" = 0 and "ITOLLA" = 0 and "ITOLLP" = 0
        #   HOV3+ only (carpool lane): "IFC" = 1 and "IHOV" = 3 and "ITOLLO" = 0 and "ITOLLA" = 0 and "ITOLLP" = 0
        #   HOV2+ & HOT (managed lane. HOV 2+ free. SOV pay toll): ): "IFC" = 1 and "IHOV" = 2 and "ITOLLO" > 0 and "ITOLLA" > 0 and "ITOLLP" > 0
        #   HOV2+ & HOT (managed lane. HOV 3+ free. HOV2 & SOV pay toll): ): "IFC" = 1 and "IHOV" = 3 and "ITOLLO" > 0 and "ITOLLA" > 0 and "ITOLLP" > 0
        #   Tollway (all vehicles tolled): "IFC" = 1 and "IHOV" = 4
        #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        # NOTE: NOT ALL MANAGED LANE LINKS HAVE A TOLL COST, 
        #       SOME COSTS ARE JUST SPECIFIED ON THE ENTRANCE / EXIT LINKS
        #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        ml_link_coords = []
        ml_links = []
        for link in network.links():
            if link["type"] == 1 and link["@hov"] in (2,3) and (
                    link["@toll_am"] + link["@toll_md"] + link["@toll_pm"]) > 0:
                ml_link_coords.append(LineString(link.shape))
                ml_links.append(link)
        ml_link_collection = MultiLineString(ml_link_coords)
        distances = []
        for zone in network.centroids():
            zone_point = Point((zone.x, zone.y))
            distances.append(zone_point.distance(ml_link_collection) / 5280)

        # distances is a Python list of the distance from each zone to nearest ML link 
        # in same order as centroids
        return distances

    @_m.logbook_trace("Calculate average travel time savings")
    def avg_travel_time_savings(self, scenario, input_directory, props, num_processors):
        # AVGTTS: The average travel savings of all households in each zone over all possible 
        # work destinations d.  
        # This average was calculated using an expected value with probabilities taken 
        # from a simplified destination 
        # choice model.  The expected travel time savings of households in a zone z is
        # SUM[d](NTTime[zd] - TRTime[zd]) * Employment[d] * exp(-0.01*NTTime[zd]) / 
        #     SUM[d]Employmentd * exp(-0.01*NTTime[zd])
        #
        # NTTime[zd] = AM_NTTime[zd] + PM_NTTime[dz]
        # TRTime[zd] = AM_TRTime[zd] + PM_TRTime[dz]

        emmebank = scenario.emmebank
        year = int(props['scenarioYear'])
        mgra = _pd.read_csv(
            _join(input_directory, 'mgra15_based_input%s.csv' % year))
        try:
            taz = mgra[['taz', 'emp_total']].groupby('taz').sum()
        except KeyError:
            taz = mgra[['taz', 'emp_tot']].groupby('taz').sum()
        taz.reset_index(inplace=True)
        taz = dem_utils.add_missing_zones(taz, scenario)
        taz.reset_index(inplace=True)

        with setup_for_tt_savings_calc(emmebank):
            employment_matrix = emmebank.matrix("mdemployment")
            try:
                employment_matrix.set_numpy_data(taz["emp_total"].values, scenario.id)
            except KeyError:
                employment_matrix.set_numpy_data(taz["emp_tot"].values, scenario.id)
            matrix_calc = dem_utils.MatrixCalculator(scenario, num_processors)
            matrix_calc.add("NTTime",    "SOV_NT_M_TIME__AM + SOV_NT_M_TIME__PM'")
            matrix_calc.add("TRTime",    "SOV_TR_M_TIME__AM + SOV_TR_M_TIME__PM'")
            matrix_calc.add("numerator",   "((NTTime - TRTime).max.0) * employment * exp(-0.01 * NTTime)", 
                            aggregation={"destinations": "+"})
            matrix_calc.add("denominator", "employment * exp(-0.01 * NTTime)", 
                            aggregation={"destinations": "+"})
            matrix_calc.add("AVGTTS", "numerator / denominator")
            matrix_calc.run()
            avg_tts = emmebank.matrix("AVGTTS").get_numpy_data(scenario.id)
        return avg_tts

    @_m.logbook_trace("Calculate percent detour without managed lane facilities")
    def percent_detour(self, scenario, network, props, num_processors):
        # PCTDETOUR: The percent difference between the AM non-toll travel time 
        # to a sample downtown zone and the AM non-toll travel time to downtown 
        # when the general purpose lanes parallel to all toll lanes requiring 
        # transponders are not available.  This variable 
        # is calculated as
        # 100*(TimeWithoutFacility - NonTransponderTime) / NonTransponderTime

        destinations = props["transponder.destinations"]

        network.create_attribute("NODE", "@root")
        network.create_attribute("NODE", "@leaf")

        mode_id = get_available_mode_id(network)
        new_mode = network.create_mode("AUX_AUTO", mode_id)
        sov_non_toll_mode = network.mode("s")

        # Find special managed links and potential parallel GP facilities
        ml_link_coords = []
        freeway_links = []
        for link in network.links():
            if link["@hov"] in [2, 3] and link["type"] == 1 and (
                    link["@toll_am"] + link["@toll_md"] + link["@toll_pm"]) > 0:
                ml_link_coords.append(LineString(link.shape))
            if sov_non_toll_mode in link.modes:
                link.modes |= set([new_mode])
                if link["type"] == 1:
                    freeway_links.append(link)

        # remove mode from nearby GP links to special managed lanes
        ml_link_collection = MultiLineString(ml_link_coords)
        for link in freeway_links:
            link_shape = LineString(link.shape)
            distance = link_shape.distance(ml_link_collection)
            if distance < 100:
                for ml_shape in ml_link_collection.geoms:
                    if ml_shape.distance(link_shape) and close_bearing(link_shape, ml_shape):
                        link.modes -= set([new_mode])
                        break

        for node in network.centroids():
            node["@root"] = 1
        for dst in destinations:
            network.node(dst)["@leaf"] = 1

        reverse_auto_network(network, "@auto_time")
        detour_impedances = shortest_paths_impedances(
            network, new_mode, "@auto_time", destinations)
        direct_impedances = shortest_paths_impedances(
            network, sov_non_toll_mode, "@auto_time", destinations)

        percent_detour = (detour_impedances - direct_impedances) / direct_impedances
        avg_percent_detour = _np.sum(percent_detour, axis=1) / len(destinations)
        avg_percent_detour = _np.nan_to_num(avg_percent_detour)
        return avg_percent_detour

    @_m.logbook_trace("Export results to transponderModelAccessibilities.csv file")
    def export_results(self, output_directory, scenario, distances, savings, detour):
        zones = scenario.zone_numbers
        output_file = _join(output_directory, "transponderModelAccessibilities.csv")
        with open(output_file, 'w', newline='') as f:
            f.write("TAZ,DIST,AVGTTS,PCTDETOUR\n")
            for row in zip(zones, distances, savings, detour):
                f.write("%d, %.4f, %.5f, %.5f\n" % row)

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg


def reverse_auto_network(network, link_cost):
    # swap directionality of modes and specified link costs, as well as turn prohibitions
    # delete all transit lines
    for line in network.transit_lines():
        network.delete_transit_line(line)

    # backup modes so that turns can be swapped (auto mode remains avialable)
    network.create_attribute("LINK", "backup_modes")
    for link in network.links():
        link.backup_modes = link.modes
    # add new reverse links (where needed) and get the one-way links to be deleted
    auto_mode = network.mode("d")
    links_to_delete = []
    for link in network.links():
        reverse_link = network.link(link.j_node.id, link.i_node.id)
        if reverse_link is None:
            reverse_link = network.create_link(link.j_node.id, link.i_node.id, link.modes)
            reverse_link.backup_modes = reverse_link.modes
            links_to_delete.append(link)
        reverse_link.modes |= link.modes

    # reverse the turn data
    visited = set([])
    for turn in network.turns():
        if turn in visited:
            continue
        reverse_turn = network.turn(turn.k_node, turn.j_node, turn.i_node)
        time, reverse_time = turn["data1"], turn["data1"]
        turn["data1"], turn["data1"] = time, reverse_time
        tpf, reverse_tpf = turn.penalty_func, reverse_turn.penalty_func
        reverse_turn.penalty_func, turn.penalty_func = tpf, reverse_tpf
        visited.add(turn)
        visited.add(reverse_turn)

    # reverse the link data
    visited = set([])
    for link in network.links():
        if link in visited:
            continue
        reverse_link = network.link(link.j_node.id, link.i_node.id)
        time, reverse_time = link[link_cost], reverse_link[link_cost]
        reverse_link[link_cost], link[link_cost] = time, reverse_time
        reverse_link.modes, link.modes = link.backup_modes, reverse_link.backup_modes
        visited.add(link)
        visited.add(reverse_link)
        
    # delete the one-way links
    for link in links_to_delete:
        network.delete_link(link.i_node, link.j_node)
        

def shortest_paths_impedances(network, mode, link_cost, destinations):
    excluded_links = []
    for link in network.links():
        if mode not in link.modes:
            excluded_links.append(link)

    impedances = []
    for dest_id in destinations:
        tree = network.shortest_path_tree(
            dest_id, link_cost, excluded_links=excluded_links, consider_turns=True)
        costs = []
        for node in network.centroids():
            if node.number == dest_id: 
                costs.append(0)
            else:
                try:
                    path_cost = tree.cost_to_node(node.id)
                except KeyError:
                    path_cost = 600
                costs.append(path_cost)
        impedances.append(costs)
    return _np.array(impedances).T


@_context
def setup_for_tt_savings_calc(emmebank):
    with gen_utils.temp_matrices(emmebank, "FULL", 2) as mf:
        mf[0].name = "NTTime"
        mf[0].description = "Temp AM + PM' Auto non-transponder time"
        mf[1].name = "TRTime"
        mf[1].description = "Temp AM + PM' Auto transponder time"
        with gen_utils.temp_matrices(emmebank, "ORIGIN", 3) as mo:
            mo[0].name = "numerator"
            mo[1].name = "denominator"
            mo[2].name = "AVGTTS"
            mo[2].description = "Temp average travel time savings"
            with gen_utils.temp_matrices(emmebank, "DESTINATION", 1) as md:
                md[0].name = "employment"
                md[0].description = "Temp employment per zone"
                yield

@_context
def get_temp_scenario(src_scenario):
    delete_scenario = _m.Modeller().tool(
        "inro.emme.data.scenario.delete_scenario")
    emmebank = src_scenario.emmebank
    scenario_id = get_available_scenario_id(emmebank)
    temp_scenario = emmebank.copy_scenario(src_scenario, scenario_id)
    try:
        yield temp_scenario
    finally:
        delete_scenario(temp_scenario)

def get_available_mode_id(network):
    for mode_id in _string.ascii_letters:
        if network.mode(mode_id) is None:
            return mode_id

def get_available_scenario_id(emmebank):
    for i in range(1,10000):
        if not emmebank.scenario(i): 
            return i

def bearing(shape):
    pt1 = shape.coords[0]
    pt2 = shape.coords[-1]
    x_diff = pt2[0] - pt1[0]
    y_diff = pt2[1] - pt1[1]
    return math.degrees(math.atan2(y_diff, x_diff))

def close_bearing(shape1, shape2, tol=25):
    b1 = bearing(shape1)
    b2 = bearing(shape2)
    diff = (b1 - b2) % 360
    if diff >= 180:
        diff -= 360
    return abs(diff) < tol
