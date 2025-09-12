#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// build_transit_scenario.py                                             ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# The build transit scenario tool generates a new scenario in the Transit
# database (under the Database_transit directory) as a copy of a scenario in
# the base (traffic assignment) database. The base traffic scenario should have
# valid results from a traffic assignment for the travel times on links to be
# available for transit lines in mixed traffic operation.
#
#
# Inputs:
#   period: the corresponding period for the scenario
#   base_scenario_id: the base traffic assignment scenario in the main Emme database
#   scenario_id: the ID to use for the new scenario in the Transit Emme database
#   scenario_title: the title for the new scenario
#   data_table_name: the root name for the source data table for the timed transfer
#                    line pairs and the day and regional pass costs.
#                    Usually the ScenarioYear
#   overwrite: overwrite the scenario if it already exists.
#
#
# Script example:
"""
import inro.modeller as _m
import os
modeller = _m.Modeller()
desktop = modeller.desktop

build_transit_scen  = modeller.tool("sandag.assignment.build_transit_scenario")
transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
load_properties = modeller.tool('sandag.utilities.properties')

project_dir = os.path.dirname(desktop.project_path())
main_directory = os.path.dirname(project_dir)
props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
main_emmebank = os.path.join(project_dir, "Database", "emmebank")
scenario_id = 100
base_scenario = main_emmebank.scenario(scenario_id)

transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")

periods = ["EA", "AM", "MD", "PM", "EV"]
period_ids = list(enumerate(periods, start=int(scenario_id) + 1))
num_processors = "MAX-1"
scenarioYear = str(props["scenarioYear"])

for number, period in period_ids:
    src_period_scenario = main_emmebank.scenario(number)
    transit_assign_scen = build_transit_scen(
        period=period, base_scenario=src_period_scenario,
        transit_emmebank=transit_emmebank,
        scenario_id=src_period_scenario.id,
        scenario_title="%s %s transit assign" % (base_scenario.title, period),
        data_table_name=scenarioYear, overwrite=True)
    transit_assign(period, transit_assign_scen, data_table_name=scenarioYear,
                   skims_only=True, num_processors=num_processors)
"""



TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import inro.emme.database.emmebank as _eb
import inro.emme.matrix as _matrix

import traceback as _traceback
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict
import contextlib as _context

import os
import sys
import math


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")
desktop = _m.Modeller().desktop


class BuildTransitNetwork(_m.Tool(), gen_utils.Snapshot):

    period = _m.Attribute(str)
    scenario_id = _m.Attribute(int)
    base_scenario_id =  _m.Attribute(str)

    data_table_name = _m.Attribute(str)
    scenario_title = _m.Attribute(str)
    overwrite = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.data_table_name = None
        self.base_scenario = _m.Modeller().scenario
        self.scenario_id = 100
        self.scenario_title = ""
        self.overwrite = False
        self.attributes = [
            "period", "scenario_id", "base_scenario_id",
            "data_table_name", "scenario_title", "overwrite"]
        self._node_id_tracker = None

    def page(self):
        if not self.data_table_name:
            load_properties = _m.Modeller().tool('sandag.utilities.properties')
            project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            main_directory = os.path.dirname(project_dir)
            props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
            self.data_table_name = props["scenarioYear"]

        pb = _m.ToolPageBuilder(self)
        pb.title = "Build transit network"
        pb.description = """
            Builds the transit network for the specified period based
            on existing base (traffic + transit) scenario."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        options = [("EA", "Early AM"),
                   ("AM", "AM peak"),
                   ("MD", "Mid-day"),
                   ("PM", "PM peak"),
                   ("EV", "Evening")]
        pb.add_select("period", options, title="Period:")

        root_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        main_emmebank = _eb.Emmebank(os.path.join(root_dir, "Database", "emmebank"))
        options = [(scen.id, "%s - %s" % (scen.id, scen.title)) for scen in main_emmebank.scenarios()]
        pb.add_select("base_scenario_id", options,
            title="Base scenario (with traffic and transit data):",
            note="With period traffic results from main (traffic assignment) database at:<br>%s" % main_emmebank.path)

        pb.add_text_box("scenario_id", title="ID for transit assignment scenario:")
        pb.add_text_box("scenario_title", title="Scenario title:", size=80)
        pb.add_text_box("data_table_name", title="Data table prefix name:", note="Default is the ScenarioYear")
        pb.add_checkbox("overwrite", title=" ", label="Overwrite existing scenario")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            root_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            main_emmebank = _eb.Emmebank(os.path.join(root_dir, "Database", "emmebank"))
            base_scenario = main_emmebank.scenario(self.base_scenario_id)
            transit_emmebank = _eb.Emmebank(os.path.join(root_dir, "Database_transit_" + self.period, "emmebank"))
            results = self(
                self.period, base_scenario, transit_emmebank,
                self.scenario_id, self.scenario_title,
                self.data_table_name, self.overwrite)
            run_msg = "Transit scenario created"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, base_scenario, transit_emmebank, scenario_id, scenario_title,
                 data_table_name, overwrite=False):
        modeller = _m.Modeller()
        attrs = {
            "period": period,
            "base_scenario_id": base_scenario.id,
            "transit_emmebank": transit_emmebank.path,
            "scenario_id": scenario_id,
            "scenario_title": scenario_title,
            "data_table_name": data_table_name,
            "overwrite": overwrite,
            "self": str(self)
        }
        with _m.logbook_trace("Build transit network for period %s" % period, attributes=attrs):
            gen_utils.log_snapshot("Build transit network", str(self), attrs)
            copy_scenario = modeller.tool(
                "inro.emme.data.scenario.copy_scenario")
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise Exception(
                    'period: unknown value - specify one of %s' % periods)

            transit_assignment = modeller.tool(
                "sandag.assignment.transit_assignment")
            if transit_emmebank.scenario(scenario_id):
                if overwrite:
                    transit_emmebank.delete_scenario(scenario_id)
                else:
                    raise Exception("scenario_id: scenario %s already exists" % scenario_id)
            
            copy_att = _m.Modeller().tool("inro.emme.data.network.copy_attribute")
            netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")

            scenario = transit_emmebank.create_scenario(scenario_id)
            scenario.title = scenario_title[:80]
            scenario.has_traffic_results = base_scenario.has_traffic_results
            scenario.has_transit_results = base_scenario.has_transit_results
            for attr in sorted(base_scenario.extra_attributes(), key=lambda x: x._id):
                dst_attr = scenario.create_extra_attribute(attr.type, attr.name, attr.default_value)
                dst_attr.description = attr.description
            for field in base_scenario.network_fields():
                scenario.create_network_field(field.type, field.name, field.atype, field.description)
            network = base_scenario.get_network()
            self._node_id_tracker = gen_utils.AvailableNodeIDTracker(network)
            new_attrs = [
                ("TRANSIT_LINE", "@xfer_from_day", "Fare for xfer from daypass/trolley"),
                ("TRANSIT_LINE", "@xfer_from_premium", "Fare for first xfer from premium"),
                ("TRANSIT_LINE", "@xfer_from_coaster", "Fare for first xfer from coaster"),
                ("TRANSIT_LINE", "@xfer_regional_pass", "0-fare for regional pass"),
                ("TRANSIT_SEGMENT", "@xfer_from_bus", "Fare for first xfer from bus"),
                ("TRANSIT_SEGMENT", "@headway_seg", "Headway adj for special xfers"),
                ("TRANSIT_SEGMENT", "@transfer_penalty_s", "Xfer pen adj for special xfers"),
                ("TRANSIT_SEGMENT", "@layover_board", "Boarding cost adj for special xfers"),
                ("NODE", "@network_adj", "Model: 1=TAP adj, 2=circle, 3=timedxfer"),
                ("NODE", "@network_adj_src", "Orig src node for timedxfer splits"),
            ]
            for elem, name, desc in new_attrs:
                attr = scenario.create_extra_attribute(elem, name)
                attr.description = desc
                network.create_attribute(elem, name)
            network.create_attribute("TRANSIT_LINE", "xfer_from_bus")

            transit_passes = gen_utils.DataTableProc("%s_transit_passes" % data_table_name)
            transit_passes = {row["pass_type"]: row["cost"] for row in transit_passes}
            day_pass = float(transit_passes["day_pass"]) / 2.0
            regional_pass = float(transit_passes["regional_pass"]) / 2.0
            params = transit_assignment.get_perception_parameters(period)
            mode_groups = transit_assignment.group_modes_by_fare(network, day_pass)

            bus_fares = {}
            for mode_id, fares in mode_groups["bus"]:
                for fare, count in fares.items():
                    bus_fares[fare] = bus_fares.get(fare, 0) + count
            # set nominal bus fare as unweighted average of two most frequent fares
            bus_fares = sorted(bus_fares.items(), key=lambda x: x[1], reverse=True)

            if len(bus_fares) >= 2:
                bus_fare = (bus_fares[0][0] + bus_fares[1][0]) / 2
            elif len(bus_fares) == 1:  # unless there is only one fare value, in which case use that one
                bus_fare = bus_fares[0][0]
            else:
                bus_fare = 0
            # find max premium mode fare
            premium_fare = 0
            for mode_id, fares in mode_groups["premium"]:
                for fare in fares.keys():
                    premium_fare = max(premium_fare, fare)
            # find max coaster_fare by checking the cumulative fare along each line
            coaster_fare = 0
            for line in network.transit_lines():
                if line.mode.id != "c":
                    continue
                segments = line.segments()
                first = next(segments)
                fare = first["@coaster_fare_board"]
                for seg in segments:
                    fare += seg["@coaster_fare_inveh"]
                coaster_fare = max(coaster_fare, fare)

            bus_fare_modes = [x[0] for x in mode_groups["bus"]]  # have a bus fare, less than the day pass
            day_pass_modes = [x[0] for x in mode_groups["day_pass"]]  # boarding fare is the same as the day pass
            premium_fare_modes = ["c"] + [x[0] for x in mode_groups["premium"]] # special premium services not covered by day pass

            for line in list(network.transit_lines()):
                # remove the "unavailable" lines in this period
                if line[params["xfer_headway"]] == 0:
                    network.delete_transit_line(line)
                    continue
                # Adjust fare perception by VOT
                line[params["fare"]] = line[params["fare"]] / params["vot"]
                # set the fare increments for transfer combinations with day pass / regional pass
                if line.mode.id in bus_fare_modes:
                    line["xfer_from_bus"] = max(min(day_pass - line["@fare"], line["@fare"]), 0)
                    line["@xfer_from_day"] = 0.0
                    line["@xfer_from_premium"] = max(min(regional_pass - premium_fare, line["@fare"]), 0)
                    line["@xfer_from_coaster"] = max(min(regional_pass - coaster_fare, line["@fare"]), 0)
                elif line.mode.id in day_pass_modes:
                    line["xfer_from_bus"] = max(day_pass - bus_fare, 0.0)
                    line["@xfer_from_day"] = 0.0
                    line["@xfer_from_premium"] = max(min(regional_pass - premium_fare, line["@fare"]), 0)
                    line["@xfer_from_coaster"] = max(min(regional_pass - coaster_fare, line["@fare"]), 0)
                elif line.mode.id in premium_fare_modes:
                    if line["@fare"] > day_pass or line.mode.id == "c":
                        # increment from bus to regional
                        line["xfer_from_bus"] = max(regional_pass - bus_fare, 0)
                        line["@xfer_from_day"] = max(regional_pass - day_pass, 0)
                    else:
                        # some "premium" modes lines are really regular fare
                        # increment from bus to day pass
                        line["xfer_from_bus"] = max(day_pass - bus_fare, 0)
                        line["@xfer_from_day"] = 0.0
                    line["@xfer_from_premium"] = max(regional_pass - premium_fare, 0)
                    line["@xfer_from_coaster"] = max(min(regional_pass - coaster_fare, line["@fare"]), 0)

            for segment in network.transit_segments():
                line = segment.line
                segment["@headway_seg"] = line[params["xfer_headway"]]
                segment["@transfer_penalty_s"] = line["@transfer_penalty"]
                segment["@xfer_from_bus"] = line["xfer_from_bus"]
            network.delete_attribute("TRANSIT_LINE", "xfer_from_bus")

            #self.taps_to_centroids(network)
            # changed to allow timed xfers for different periods
            timed_transfers_with_walk = list(gen_utils.DataTableProc("%s_timed_xfer_%s" % (data_table_name,period)))
            self.timed_transfers(network, timed_transfers_with_walk, period)
            #self.connect_circle_lines(network)
            #self.duplicate_tap_adajcent_stops(network)
            # The fixed guideway travel times are stored in "@trtime"
            # and copied to data2 (ul2) for the ttf
            # The congested auto times for mixed traffic are in "@auto_time"
            # (output from traffic assignment) which needs to be copied to auto_time (a.k.a. timau)
            # (The auto_time attribute is generated from the VDF values which include reliability factor)
            ## also copying auto_time to ul1, so it does not get wiped when transit connectors are created. 
            
            for link in network.links():
                if scenario.has_traffic_results and "@auto_time" in scenario.attributes("LINK"):
                    link["auto_time"]=link["@auto_time"]
                    link["data1"]=link["@auto_time"]
                rail_modes = set(network.mode(m) for m in "lco")
                if link.modes & rail_modes:
                    link["data2"]=link[params["fixed_rail_link_time"]]
                else:
                    link["data2"]=link[params["fixed_bus_link_time"]]

            scenario.publish_network(network)
            self._node_id_tracker = None
            return scenario 

    @_m.logbook_trace("Add timed-transfer links", save_arguments=True)
    def timed_transfers(self, network, timed_transfers_with_walk, period):
        no_walk_link_error = "no walk link from line %s to %s"
        node_not_found_error = "node %s not found in itinerary for line %s; "\
            "the to_line may end at the transfer stop"

        def find_walk_link(from_line, to_line):
            to_nodes = set([s.i_node for s in to_line.segments(True)
                            if s.allow_boardings])
            link_candidates = []
            for seg in from_line.segments(True):
                if not seg.allow_alightings:
                    continue
                for link in seg.i_node.outgoing_links():
                    if link.j_node in to_nodes:
                        link_candidates.append(link)
            if not link_candidates:
                raise Exception(no_walk_link_error % (from_line, to_line))
            # if there are multiple connecting links take the shortest one
            return sorted(link_candidates, key=lambda x: x.length)[0]

        def link_on_line(line, node, near_side_stop):
            node = network.node(node)
            if near_side_stop:
                for seg in line.segments():
                    if seg.j_node == node:
                        return seg.link
            else:
                for seg in line.segments():
                    if seg.i_node == node:
                        return seg.link
            raise Exception(node_not_found_error % (node, line))

        # Group parallel transfers together (same pair of alighting-boarding nodes from the same line)
        walk_transfers = _defaultdict(lambda: [])
        for i, transfer in enumerate(timed_transfers_with_walk, start=1):
            try:
                from_line = network.transit_line(transfer["from_line"])
                if not from_line:
                    raise Exception("from_line %s does not exist" % transfer["from_line"])
                to_line = network.transit_line(transfer["to_line"])
                if not to_line:
                    raise Exception("to_line %s does not exist" % transfer["to_line"])
                walk_link = find_walk_link(from_line, to_line)
                from_link = link_on_line(from_line, walk_link.i_node, near_side_stop=True)
                to_link = link_on_line(to_line, walk_link.j_node, near_side_stop=False)
                walk_transfers[(from_link, to_link)].append({
                    "to_line": to_line,
                    "from_line": from_line,
                    "walk_link": walk_link,
                    "wait": transfer["wait_time"],
                })
            except Exception as error:
                new_message = "Timed transfer[%s]: %s" % (i, error)
                raise type(error)(new_message).with_traceback(sys.exc_info()[2])

        # If there is only one transfer at the location (redundant case)
        # OR all transfers are from the same line (can have different waits)
        # OR all transfers are to the same line and have the same wait
        # Merge all transfers onto the same transfer node
        network_transfers = []
        for (from_link, to_link), transfers in walk_transfers.items():
            walk_links = set([t["walk_link"] for t in transfers])
            from_lines = set([t["from_line"] for t in transfers])
            to_lines = set([t["to_line"] for t in transfers])
            waits = set(t["wait"] for t in transfers)
            if len(transfers) == 1 or len(from_lines) == 1 or (len(to_lines) == 1 and len(waits) == 1):
                network_transfers.append({
                    "from_link": from_link,
                    "to_link": to_link,
                    "to_lines": list(to_lines),
                    "from_lines": list(from_lines),
                    "walk_link": walk_links.pop(),
                    "wait": dict((t["to_line"], t["wait"]) for t in transfers)})
            else:
                for transfer in transfers:
                    network_transfers.append({
                        "from_link": from_link,
                        "to_link": to_link,
                        "to_lines": [transfer["to_line"]],
                        "from_lines": [transfer["from_line"]],
                        "walk_link": transfer["walk_link"],
                        "wait": {transfer["to_line"]: transfer["wait"]}})

        def split_link(link, node_id, lines, split_links, stop_attr, waits=None):
            near_side_stop = (stop_attr == "allow_alightings")
            orig_link = link
            if link in split_links:
                link = split_links[link]
            i_node, j_node = link.i_node, link.j_node
            length = link.length
            proportion = min(0.006 / length, 0.2)
            if near_side_stop:
                proportion = 1 - proportion
            new_node = network.split_link(i_node, j_node, node_id, False, proportion)
            new_node["@network_adj"] = 3
            new_node["@network_adj_src"] = orig_link.j_node.number if near_side_stop else orig_link.i_node.number
            in_link = network.link(i_node, new_node)
            out_link = network.link(new_node, j_node)
            split_links[orig_link] = in_link if near_side_stop else out_link
            if near_side_stop:
                in_link.length = length
                out_link.length = 0
                out_link["@trtime"] = 0
            else:
                out_link.length = length
                in_link.length = 0
                in_link["@trtime"] = 0

            for seg in in_link.segments():
                if not near_side_stop:
                    seg.transit_time_func = 3
                seg["@coaster_fare_inveh"] = 0
            for seg in out_link.segments():
                if near_side_stop:
                    seg.transit_time_func = 3
                seg.allow_alightings = seg.allow_boardings = False
                seg.dwell_time = 0
                if seg.line in lines:
                    seg[stop_attr] = True
                    if stop_attr == "allow_boardings":
                        seg["@headway_seg"] = float(waits[seg.line]) * 2
            return new_node

        # process the transfer points, split links and set attributes
        split_links = {}
        for transfer in network_transfers:
            new_alight_node = split_link(
                transfer["from_link"], self._node_id_tracker.get_id(), transfer["from_lines"],
                split_links, "allow_alightings")
            new_board_node = split_link(
                transfer["to_link"], self._node_id_tracker.get_id(), transfer["to_lines"],
                split_links, "allow_boardings", waits=transfer["wait"])
            walk_link = transfer["walk_link"]
            transfer_link = network.create_link(
                new_alight_node, new_board_node, [network.mode("x")])
            for attr in network.attributes("LINK"):
                transfer_link[attr] = walk_link[attr]

    @_m.logbook_trace("Add circle line free layover transfers")
    def connect_circle_lines(self, network):
        network.create_attribute("NODE", "circle_lines")
        line_attributes = network.attributes("TRANSIT_LINE")
        seg_attributes = network.attributes("TRANSIT_SEGMENT")

        def offset_coords(node):
            rho = math.sqrt(5000)
            phi = 3 * math.pi / 4 + node.circle_lines * math.pi / 12
            x = node.x + rho * math.cos(phi)
            y = node.y + rho * math.sin(phi)
            node.circle_lines += 1
            return(x, y)

        transit_lines = list(network.transit_lines())
        for line in transit_lines:
            first_seg = line.segment(0)
            last_seg = line.segment(-1)
            if first_seg.i_node == last_seg.i_node:
                # Add new node, offset from existing node
                start_node = line.segment(0).i_node
                xfer_node = network.create_node(self._node_id_tracker.get_id(), False)
                xfer_node["@network_adj"] = 2
                xfer_node.x, xfer_node.y = offset_coords(start_node)
                network.create_link(start_node, xfer_node, [line.vehicle.mode])
                network.create_link(xfer_node, start_node, [line.vehicle.mode])

                # copy transit line data, re-route itinerary to and from new node
                line_data = dict((k, line[k]) for k in line_attributes)
                line_data["id"] = line.id
                line_data["vehicle"] = line.vehicle
                first_seg.allow_boardings = True
                first_seg.allow_alightings = False
                first_seg_data = dict((k, first_seg[k]) for k in seg_attributes)
                first_seg_data.update({
                    "@headway_seg": 0.01, "dwell_time": 0, "transit_time_func": 3,
                    "@transfer_penalty_s": 0, "@xfer_from_bus": 0, "@layover_board": 1
                })
                last_seg.allow_boardings = False
                last_seg.allow_alightings = True
                last_seg_data = dict((k, last_seg[k]) for k in seg_attributes)
                last_seg_data.update({
                    "@headway_seg": 0.01, "dwell_time": 5.0, "transit_time_func": 3
                    # incremental dwell time for layover of 5 min
                    # Note: some lines seem to have a layover of 0, most of 5 mins
                })
                seg_data = {
                    (xfer_node, start_node, 1): first_seg_data,
                    (xfer_node, None, 1): last_seg_data}
                itinerary = [xfer_node.number]
                for seg in line.segments():
                    seg_data[(seg.i_node, seg.j_node, seg.loop_index)] = dict((k, seg[k]) for k in seg_attributes)
                    itinerary.append(seg.i_node.number)
                last_seg = line.segment(-1)
                seg_data[(last_seg.i_node, xfer_node, 1)] = dict((k, last_seg[k]) for k in seg_attributes)
                seg_data[(last_seg.i_node, xfer_node, 1)]["transit_time_func"] = 3
                itinerary.extend([last_seg.i_node.number, xfer_node.number])

                network.delete_transit_line(line)
                new_line = network.create_transit_line(
                    line_data.pop("id"), line_data.pop("vehicle"), itinerary)
                for k, v in line_data.items():
                    new_line[k] = v
                for seg in new_line.segments(include_hidden=True):
                    data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                    for k, v in data.items():
                        seg[k] = v

        network.delete_attribute("NODE", "circle_lines")
