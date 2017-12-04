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
#   timed_xfers_table: the source data table for the timed transfer line pairs
#   overwrite: overwrite the scenario if it already exists.
#
#


TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import inro.emme.database.emmebank as _eb
import traceback as _traceback
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict
import contextlib as _context

import os
import sys
import math


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class BuildTransitNetwork(_m.Tool(), gen_utils.Snapshot):

    period = _m.Attribute(unicode)
    scenario_id = _m.Attribute(int)
    base_scenario_id =  _m.Attribute(str)

    timed_xfers_table = _m.Attribute(unicode)
    scenario_title = _m.Attribute(unicode)
    overwrite = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        self.timed_xfers_table = None
        self.base_scenario = _m.Modeller().scenario
        self.scenario_id = 100
        self.scenario_title = ""
        self.overwrite = False
        self.attributes = [
            "period", "scenario_id", "base_scenario_id", 
            "timed_xfers_table", "scenario_title", "overwrite"]
        self._dt_db = _m.Modeller().desktop.project.data_tables()

    def page(self):
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
        
        options = [(None, "")] + [(table.name, table.name) for table in self._dt_db.tables()]
        pb.add_select("timed_xfers_table", options, title="Timed transfer data table:",
            note="Normally used only with AM peak period assignment.")

        pb.add_checkbox("overwrite", title=" ", label="Overwrite existing scenario")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            root_dir = os.path.dirname(_m.Modeller().desktop.project.path)
            main_emmebank = _eb.Emmebank(os.path.join(root_dir, "Database", "emmebank"))
            base_scenario = main_emmebank.scenario(self.base_scenario_id)
            transit_emmebank = _eb.Emmebank(os.path.join(root_dir, "Database_transit", "emmebank"))
            results = self(
                self.period, base_scenario, transit_emmebank,
                self.scenario_id, self.scenario_title, 
                self.timed_xfers_table, self.overwrite)
            run_msg = "Transit scenario created"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, base_scenario, transit_emmebank, scenario_id, scenario_title="", 
                 timed_xfers_table=None, overwrite=False):
        modeller = _m.Modeller()
        attrs = {
            "period": period, 
            "scenario_id": scenario_id, 
            "transit_emmebank": transit_emmebank.path, 
            "base_scenario_id": base_scenario.id, 
            "timed_xfers_table": timed_xfers_table,
            "scenario_title": scenario_title, 
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

            if timed_xfers_table:
                timed_transfers_with_walk = list(gen_utils.DataTableProc(timed_xfers_table))

            perception_parameters = {
                "EA": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_ea"
                },
                "AM": {
                    "vot": 0.10, "init_wait": 1.5, "xfer_wait": 3.0, "walk": 1.8, 
                    "headway": "@headway_rev_am", "fare": "@fare_per_pk", "in_vehicle": "@vehicle_per_pk",
                    "fixed_link_time": "@trtime_link_am"
                },
                "MD": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_md"
                },
                "PM": {
                    "vot": 0.10, "init_wait": 1.5, "xfer_wait": 3.0, "walk": 1.8, 
                    "headway": "@headway_rev_pm", "fare": "@fare_per_pk", "in_vehicle": "@vehicle_per_pk",
                    "fixed_link_time": "@trtime_link_pm"
                },
                "EV": {
                    "vot": 0.05, "init_wait": 1.6, "xfer_wait": 2.5, "walk": 1.6, 
                    "headway": "@headway_rev_op", "fare": "@fare_per_op", "in_vehicle": "@vehicle_per_op",
                    "fixed_link_time": "@trtime_link_ev"
                },
            }        
            params = perception_parameters[period]
            
            if transit_emmebank.scenario(scenario_id):
                if overwrite:
                    transit_emmebank.delete_scenario(scenario_id)
                else:
                    raise Exception("scenario_id: scenario %s already exists" % scenario_id)

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
            new_attrs = [
                ("TRANSIT_LINE", "@xfer_from_bus", "Fare for first xfer from bus"), 
                ("TRANSIT_LINE", "@xfer_from_day", "Fare for xfer from daypass/trolley"), 
                ("TRANSIT_LINE", "@xfer_from_premium", "Fare for first xfer from premium"), 
                ("TRANSIT_LINE", "@xfer_from_coaster", "Fare for first xfer from coaster"), 
                ("TRANSIT_LINE", "@xfer_regional_pass", "0-fare for regional pass"),  
                ("TRANSIT_SEGMENT", "@headway_seg", "Headway adj for special xfers"), 
                ("TRANSIT_SEGMENT", "@transfer_penalty_s", "Xfer pen adj for special xfers"),
                ("TRANSIT_SEGMENT", "@layover_board", "Boarding cost adj for special xfers"),
                ("NODE", "@coaster_fare_node", "Coaster fare boarding costs at nodes"),
                ("NODE", "@network_adj", "Model: 1=TAP adj, 2=circle, 3=timedxfer")
            ]
            for elem, name, desc in new_attrs:
                attr = scenario.create_extra_attribute(elem, name)
                attr.description = desc
                network.create_attribute(elem, name)

            self._init_node_id(network)
            coaster_mode = network.mode("c")
            bus_mode = network.mode("b")
            prem_mode = network.mode("p")
            lrt_mode = network.mode("l")
            for line in list(network.transit_lines()):
                # remove the "unavailable" lines in this period
                if line[params["headway"]] == 0:
                    network.delete_transit_line(line)
                    continue
                # Adjust fare perception by VOT
                line[params["fare"]] = line[params["fare"]] / params["vot"]

                # set the fare increments for transfer combinations with day pass / regional pass
                if line.mode == bus_mode and line["@fare"] > 1.00:
                    line["@xfer_from_bus"] = min(max(2.50 - line["@fare"], 0), 0.75)
                elif line.mode in [ prem_mode, coaster_mode ]:
                    line["@xfer_from_bus"] = 4.0    # increment from bus to regional (either 1.75 and 2.25 bus fare)
                    line["@xfer_from_day"] = 3.5    # increment from trolley / half day pass to half regional pass
                elif line.id.startswith("399"):
                    line["@xfer_from_bus"] = 0.75
                else:  # lrt, express and brt (red and yellow)
                    line["@xfer_from_bus"] = 0.25
                if line["@fare"] > 0.0:
                    line["@xfer_from_premium"] = 1.0
                    line["@xfer_from_coaster"] = 1.25
            for segment in network.transit_segments():
                segment["@headway_seg"] = segment.line[params["headway"]]
                segment["@transfer_penalty_s"] = segment.line["@transfer_penalty"]

            self.taps_to_centroids(network)
            if timed_xfers_table:
                self.timed_transfers(network, timed_transfers_with_walk, period)
            self.connect_circle_lines(network)
            self.duplicate_tap_adajcent_stops(network)
            # The fixed guideway travel times are stored in "@trtime_link_xx"
            # and copied to data2 (ul2) for the ttf 
            # The congested auto times for mixed traffic are in "timau" 
            # (output from traffic assignment)
            values = network.get_attribute_values("LINK", [params["fixed_link_time"]])
            network.set_attribute_values("LINK", ["data2"], values)
            scenario.publish_network(network)

            network_calc = _m.Modeller().tool(
                "inro.emme.network_calculation.network_calculator")
            network_calc_spec = {
                "result": "@coaster_fare_node",
                "expression": "@coaster_fare_board",
                "selections": {"transit_line": "all", "link": "all"},
                "aggregation": ".max.",
                "type": "NETWORK_CALCULATION"
            }
            network_calc(network_calc_spec, scenario=scenario)
            
            return scenario

    @_m.logbook_trace("Convert TAP nodes to centroids")
    def taps_to_centroids(self, network):
        # delete existing traffic centroids
        for centroid in list(network.centroids()):
            network.delete_node(centroid, cascade=True)

        node_attrs = network.attributes("NODE")
        link_attrs = network.attributes("LINK")
        for node in list(network.nodes()):
            if node["@tap_id"] > 0:
                centroid = network.create_node(node["@tap_id"], is_centroid=True)
                for attr in node_attrs:
                    centroid[attr] = node[attr]
                for link in node.outgoing_links():
                    connector = network.create_link(centroid, link.j_node, link.modes)
                    connector.vertices = link.vertices
                    for attr in link_attrs:
                        connector[attr] = link[attr]
                for link in node.incoming_links():
                    connector = network.create_link(link.i_node, centroid, link.modes)
                    connector.vertices = link.vertices
                    for attr in link_attrs:
                        connector[attr] = link[attr]
                network.delete_node(node, cascade=True)

    @_m.logbook_trace("Duplicate TAP access and transfer access stops")
    def duplicate_tap_adajcent_stops(self, network):
        # Expand network by duplicating TAP adjacent stops
        network.create_attribute("NODE", "tap_stop", False)
        all_transit_modes = set([network.mode(m) for m in ["b", "e", "p", "r", "y", "l", "c"]])
        access_mode = set([network.mode("a")])
        transfer_mode =  network.mode("x")
        walk_mode =  network.mode("w")

        # Mark TAP adjacent stops and split TAP connectors
        for centroid in network.centroids():
            out_links = list(centroid.outgoing_links())
            in_links = list(centroid.incoming_links())
            for link in out_links + in_links:
                link.length = 0.0005  # setting length so that connector access time = 0.01
            for link in out_links:
                real_stop = link.j_node
                has_adjacent_transfer_links = False
                has_adjacent_walk_links = False
                for stop_link in real_stop.outgoing_links():
                    if stop_link == link.reverse_link:
                        continue
                    if transfer_mode in stop_link.modes :
                        has_adjacent_transfer_links = True
                    if walk_mode in stop_link.modes :
                        has_adjacent_walk_links = True
                        
                if has_adjacent_transfer_links and not has_adjacent_walk_links:
                    length = link.length
                    tap_stop = network.split_link(centroid, real_stop, self._get_node_id(), include_reverse=True)
                    tap_stop["@network_adj"] = 1
                    real_stop.tap_stop = tap_stop
                    transit_access_link = network.link(real_stop, tap_stop)
                    for link in transit_access_link, transit_access_link.reverse_link:
                        link.modes = all_transit_modes
                        link.length = 0
                        for p in ["ea", "am", "md", "pm", "ev"]:
                            link["@time_link_" + p] = 0
                    access_link = network.link(tap_stop, centroid)
                    access_link.modes = access_mode
                    access_link.reverse_link.modes = access_mode
                    access_link.length = length
                    access_link.reverse_link.length = length
            
        line_attributes = network.attributes("TRANSIT_LINE")
        seg_attributes = network.attributes("TRANSIT_SEGMENT")

        # re-route the transit lines through the new TAP-stops
        for line in network.transit_lines():
            # store line and segment data for re-routing
            line_data = dict((k, line[k]) for k in line_attributes)
            line_data["id"] = line.id
            line_data["vehicle"] = line.vehicle
            
            seg_data = {}
            itinerary = []
            tap_adjacent_stops = []

            for seg in line.segments(include_hidden=True):
                seg_data[(seg.i_node, seg.j_node, seg.loop_index)] = \
                    dict((k, seg[k]) for k in seg_attributes)
                itinerary.append(seg.i_node.number)
                if seg.i_node.tap_stop and seg.allow_boardings:
                    # insert tap_stop, real_stop loop after tap_stop
                    real_stop = seg.i_node
                    tap_stop = real_stop.tap_stop
                    itinerary.extend([tap_stop.number, real_stop.number])
                    tap_adjacent_stops.append(len(itinerary) - 1)  # index of "real" stop in itinerary
            
            if tap_adjacent_stops:
                network.delete_transit_line(line)
                new_line = network.create_transit_line(
                    line_data.pop("id"), 
                    line_data.pop("vehicle"), 
                    itinerary)
                for k, v in line_data.iteritems():
                    new_line[k] = v

                for seg in new_line.segments(include_hidden=True):
                    data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                    for k, v in data.iteritems():
                        seg[k] = v
                for index in tap_adjacent_stops:
                    access_seg = new_line.segment(index - 2)
                    egress_seg = new_line.segment(index - 1)
                    real_seg = new_line.segment(index)
                    for k in seg_attributes:
                        access_seg[k] = egress_seg[k] = real_seg[k]
                    access_seg.allow_boardings = False
                    access_seg.allow_alightings = True
                    access_seg.transit_time_func = 3
                    access_seg.dwell_time = real_seg.dwell_time
                    egress_seg.allow_boardings = True
                    egress_seg.allow_alightings = True
                    egress_seg.transit_time_func = 3
                    egress_seg.dwell_time = 0
                    real_seg.allow_boardings = True
                    real_seg.allow_alightings = False
                    real_seg.dwell_time = 0

        network.delete_attribute("NODE", "tap_stop")

    @_m.logbook_trace("Add timed-transfer links", save_arguments=True)
    def timed_transfers(self, network, timed_transfers_with_walk, period):
        no_walk_link_error = "no walk link from line %s to %s"
        node_not_found_error = "node %s not found in itinerary for line %s"

        def find_walk_link(from_line, to_line):
            to_nodes = set([s.i_node for s in to_line.segments(True)
                            if s.allow_boardings])
            for seg in from_line.segments(True):
                if not s.allow_alightings:
                    continue
                for link in seg.i_node.outgoing_links():
                    if link.j_node in to_nodes:
                        return link
            raise Exception(no_walk_link_error % (from_line, to_line))
            
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

        # Group parallel transfers together (same pair of alighting-boarding nodes)
        walk_transfers = _defaultdict(lambda: {"from_lines": [], "to_lines": [], "walk_link": None})
        waits = {}
        for i, transfer in enumerate(timed_transfers_with_walk):
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
                walk_transfers[(from_link, to_link)]["from_lines"].append(from_line)
                walk_transfers[(from_link, to_link)]["to_lines"].append(to_line)
                walk_transfers[(from_link, to_link)]["walk_link"] = walk_link
                waits[to_line] = transfer["wait_time"]
            except Exception as error:
                new_message = "Timed transfer[%s]: %s" % (i, error.message)
                raise type(error), type(error)(new_message), sys.exc_info()[2]

        def split_link(link, node_id, lines, stop_attr, split_links, waits=None, near_side_stop=True):
            i_node, j_node = link.i_node, link.j_node
            if link in split_links:
                new_node = split_links[link]
                in_link = network.link(i_node, new_node)
                out_link = network.link(new_node, j_node)
            else:
                length = link.length
                proportion = min(0.006 / length, 0.2)
                if near_side_stop:
                    proportion = 1 - proportion
                new_node = network.split_link(i_node, j_node, node_id, False, proportion)
                new_node["@network_adj"] = 3
                split_links[link] = new_node
                in_link = network.link(i_node, new_node)
                out_link = network.link(new_node, j_node)
                if near_side_stop:
                    out_link.length = 0
                    in_link.length = length
                    for p in ["ea", "am", "md", "pm", "ev"]:
                        out_link["@time_link_" + p] = 0
                else:
                    out_link.length = length
                    in_link.length = 0
                    for p in ["ea", "am", "md", "pm", "ev"]:
                        in_link["@time_link_" + p] = 0
                
            for seg in in_link.segments():
                seg.transit_time_func = 3
                seg["@coaster_fare_inveh"] = 0
            for seg in out_link.segments():
                seg.allow_alightings = seg.allow_boardings = False            
                seg.dwell_time = 0
                if seg.line in lines:
                    seg[stop_attr] = True
                    if stop_attr == "allow_boardings":
                        seg["@headway_seg"] = waits[seg.line] * 2
            return new_node

        # process the transfer points, split links and set attributes
        split_links = {}
        for (from_link, to_link), transfer in walk_transfers.iteritems():
            new_alight_node = split_link(
                from_link, self._get_node_id(), transfer["from_lines"], 
                "allow_alightings", split_links, near_side_stop=True)
            new_board_node = split_link(
                to_link, self._get_node_id(), transfer["to_lines"], 
                "allow_boardings", split_links, waits, near_side_stop=False)
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
            if line.segment(0).i_node == line.segment(-1).i_node:
                # Add new node, offset from existing node
                start_node = line.segment(0).i_node
                xfer_node = network.create_node(self._get_node_id(), False)
                xfer_node["@network_adj"] = 2
                xfer_node.x, xfer_node.y = offset_coords(start_node)        
                network.create_link(start_node, xfer_node, [line.vehicle.mode])
                network.create_link(xfer_node, start_node, [line.vehicle.mode])
                
                # copy transit line data, re-route itinerary to and from new node
                line_data = dict((k, line[k]) for k in line_attributes)
                line_data["id"] = line.id
                line_data["vehicle"] = line.vehicle
                seg_data = {
                    (xfer_node, start_node, 1): {
                        "allow_boardings": True, "allow_alightings": True, 
                        "@headway_seg": 0.01, "dwell_time": 0, "transit_time_func": 3,
                        "@transfer_penalty_s": 0, #"@xfer_from_bus": 0,
                        "@layover_board": 1
                    },
                    (xfer_node, None, 1): {
                        "allow_boardings": True, "allow_alightings": True, 
                        "@headway_seg": 0.01, 
                        "dwell_time": 4.7,  
                        # incremental dwell time for layover of 5 min (0.3 already included)
                        # Note: some lines seem to have a layover of 0, most of 5 mins
                        "transit_time_func": 3
                    },
                }
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
                for k, v in line_data.iteritems():
                    new_line[k] = v
                for seg in new_line.segments(include_hidden=True):
                    data = seg_data.get((seg.i_node, seg.j_node, seg.loop_index), {})
                    for k, v in data.iteritems():
                        seg[k] = v
                
        network.delete_attribute("NODE", "circle_lines")

    def _init_node_id(self, network):
        new_node_id = max(n.number for n in network.nodes())
        self._new_node_id = math.ceil(new_node_id / 10000.0) * 10000

    def _get_node_id(self):
        self._new_node_id += 1
        return self._new_node_id
