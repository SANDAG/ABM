#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// transit_select_analysis.py                                            ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# This tool runs select type network analysis on the results of one or more 
# transit assignments. It is run as a post-process tool after the assignment
# tools are complete, using the saved transit strategies. Any number of 
# analyses can be run without needing to rerun the assignments.  
#
#
# Inputs:
#   Trip components for selection: pick one or more extra attributes which 
#       identify the network elements of interest by trip component: 
#           in_vehicle
#           aux_transit
#           initial_boarding
#           transfer_boarding 
#           transfer_alighting
#           final_alighting
#   Result suffix: the suffix to use in the naming of per-class result 
#       attributes and matrices, up to 6 characters.
#   Threshold: the minimum number of elements which must be encountered 
#       for the path selection. 
#   Scenario: the scenario to analyse.
#
#
# Script example:
"""
import inro.modeller as _m
import os
modeller = _m.Modeller()
desktop = modeller.desktop

select_link = modeller.tool("sandag.assignment.transit_select_link")

project_dir = os.path.dirname(desktop.project_path())
main_directory = os.path.dirname(project_dir)

transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")

periods = ["EA", "AM", "MD", "PM", "EV"]
period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

suffix = "LRT"
threshold = 1
num_processors = "MAX-1"
selection = {
    "in_vehicle": None,
    "aux_transit": None,
    "initial_boarding": "@selected_line",
    "transfer_boarding": None,
    "transfer_alighting": None,
    "final_alighting": None,
}

for number, period in period_ids:
    scenario = transit_emmebank.scenario(number)
    select_link(selection, suffix, threshold, scenario, num_processors)
"""

TOOLBOX_ORDER = 25


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TransitSelectAnalysis(_m.Tool(), gen_utils.Snapshot):

    in_vehicle = _m.Attribute(object)
    aux_transit = _m.Attribute(object)
    initial_boarding = _m.Attribute(object)
    transfer_boarding = _m.Attribute(object)
    transfer_alighting = _m.Attribute(object)
    final_alighting = _m.Attribute(object)

    suffix = _m.Attribute(str)
    threshold = _m.Attribute(int)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        self.threshold = 1
        self.num_processors = "MAX-1"
        self.attributes = [
            "in_vehicle", "aux_transit", "initial_boarding", "transfer_boarding", 
            "transfer_alighting", "final_alighting", "suffix", "threshold", 
            "num_processors"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Transit select analysis"
        pb.description = """
        Run select type of analysis (select link, select node, select line ...) on
        the results of the transit assignment(s) using a path-based analysis. 
        Can be used after a transit assignment has been completed."""
        pb.branding_text = "- SANDAG - Assignment"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        with pb.section("Trip components for selection:"):
            domains = ["LINK", "NODE", "TRANSIT_SEGMENT", "TRANSIT_LINE"]
            pb.add_select_extra_attribute("in_vehicle", title="In-vehicle", filter=domains, allow_none=True)
            pb.add_select_extra_attribute("aux_transit", title="Auxilary transit", filter=domains, allow_none=True)
            pb.add_select_extra_attribute("initial_boarding", title="Initial boarding", filter=domains, allow_none=True)
            pb.add_select_extra_attribute("transfer_boarding", title="Transfer boarding", filter=domains, allow_none=True)
            pb.add_select_extra_attribute("transfer_alighting", title="Transfer alighting", filter=domains, allow_none=True)
            pb.add_select_extra_attribute("final_alighting", title="Final alighting", filter=domains, allow_none=True)

        pb.add_text_box("suffix", title="Suffix for results (matrices and attributes):", size=6,
            note="The suffix to use in the naming of per-class result attributes and matrices, up to 6 characters. "
                 "Should be unique (existing attributes / matrices will be overwritten).")
        pb.add_text_box("threshold", title="Threshold for selection:", 
            note="The minimum number of links which must be encountered for the path selection. "
                 "The default value of 1 indicates an 'any' link selection.")
        dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            selection = {
                "in_vehicle": self.in_vehicle,
                "aux_transit": self.aux_transit,
                "initial_boarding": self.initial_boarding,
                "transfer_boarding": self.transfer_boarding,
                "transfer_alighting": self.transfer_alighting,
                "final_alighting": self.final_alighting,
            }
            scenario = _m.Modeller().scenario
            results = self(selection, self.suffix, self.threshold, scenario, self.num_processors)
            run_msg = "Traffic assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, selection, suffix, threshold, scenario, num_processors):
        attrs = {
            "selection": selection, 
            "suffix": suffix, 
            "threshold": threshold, 
            "scenario": scenario.id,
            "num_processors": num_processors
        }
        with _m.logbook_trace("Transit select analysis %s" % suffix, attributes=attrs):
            attrs.update(dict((k,v) for k,v in attrs["selection"].items()))
            gen_utils.log_snapshot("Transit select analysis", str(self), attrs)

            path_analysis = _m.Modeller().tool(
                "inro.emme.transit_assignment.extended.path_based_analysis")
            create_attribute = _m.Modeller().tool(
                "inro.emme.data.extra_attribute.create_extra_attribute")

            spec = {
                "portion_of_path": "COMPLETE",
                "trip_components": selection,
                "path_operator": "+",
                "path_selection_threshold": {"lower": threshold, "upper": 999999},
                "path_to_od_aggregation": None,
                "constraint": None,
                "analyzed_demand": None,
                "results_from_retained_paths": None,
                "path_to_od_statistics": None,
                "path_details": None,
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            strategies = scenario.transit_strategies
            classes = [x.name for x in strategies.strat_files()]
            if not classes:
                raise Exception("Results for multi-class transit assignment not available")
                
            for class_name in classes:
                with _m.logbook_trace("Analysis for class %s" % class_name):
                    seldem_name = "SELDEM_%s_%s" % (class_name, suffix)
                    desc = "Selected demand for %s %s" % (class_name, suffix)
                    seldem = dem_utils.create_full_matrix(seldem_name, desc, scenario=scenario)
                    results_from_retained_paths = {
                        "paths_to_retain": "SELECTED",
                        "demand": seldem.named_id,
                    }
                    attributes = [
                        ("transit_volumes",     "TRANSIT_SEGMENT", "@seltr_%s_%s",  "%s '%s' sel segment flow"),
                        ("aux_transit_volumes", "LINK",            "@selax_%s_%s",  "%s '%s' sel aux transit flow"),
                        ("total_boardings",     "TRANSIT_SEGMENT", "@selbr_%s_%s", "%s '%s' sel boardings"),
                        ("total_alightings",    "TRANSIT_SEGMENT", "@selal_%s_%s", "%s '%s' sel alightings"),
                    ]
                    mode_name = class_name.lower()[3:]
                    for key, domain, name, desc in attributes:
                        attr = create_attribute(domain, name % (mode_name, suffix), desc % (class_name, suffix), 
                                                0, overwrite=True, scenario=scenario)
                        results_from_retained_paths[key] = attr.id
                    spec["results_from_retained_paths"] = results_from_retained_paths
                    path_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg
