#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// traffic_assignment.py                                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 20


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TrafficAssignment(_m.Tool()):

    period = _m.Attribute(unicode)
    relative_gap = _m.Attribute(float)
    max_iterations = _m.Attribute(int)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        self.relative_gap = 0.0005
        self.max_iterations = 100
        self.num_processors = "MAX-1"
        version = os.environ.get("EMMEPATH", "")
        self._version = version[-5:] if version else ""
        self._skim_classes_separately = False  # Used for debugging only
        
    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Traffic assignment"
        pb.description = """
Assign traffic demand for the selected time period."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        options = [("EA","Early AM"),
                   ("AM","AM peak"),
                   ("MD","Mid-day"), 
                   ("PM","PM peak"),
                   ("EV","Evening")]
        pb.add_select("period", options, title="Period:")

        pb.add_text_box("relative_gap", title="Relative gap:")
        pb.add_text_box("max_iterations", title="Max iterations:")
        dem_utils.add_select_processors("num_processors", pb, self)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            results = self(self.period, self.relative_gap, self.max_iterations, self.num_processors, scenario)
            run_msg = "Traffic assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, relative_gap, max_iterations, num_processors, scenario):
        attrs = {
            "period": period, 
            "relative_gap": relative_gap, 
            "max_iterations": max_iterations, 
            "num_processors": num_processors, 
            "scenario": scenario,
            "self": self
        }
        with _m.logbook_trace("Traffic assignment for period %s" % period, attributes=attrs):
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise _except.ArgumentError(
                    'period: unknown value - specify one of %s' % periods)
            num_processors = dem_utils.parse_num_processors(num_processors)
            classes = [
                {   # 0
                    "name": 'SOVGP', "mode": 's', "PCE": 1, "VOT": 67., "cost": '',
                    "skims": ["GENCOST", "TIME", "DIST"]
                },                      
                {   # 1
                    "name": 'SOVTOLL', "mode": 'S', "PCE": 1, "VOT": 67., "cost": '@cost_auto',      
                    "skims": ["GENCOST", "TIME", "DIST", "MLCOST", "TOLLCOST", "TOLLDIST"]
                },                  
                {   # 2
                    "name": 'HOV2GP', "mode": 's', "PCE": 1, "VOT": 67., "cost": '',                
                    "skims": []  # same as SOV_gp
                },                    
                {   # 3
                    "name": 'HOV2HOV', "mode": 'h', "PCE": 1, "VOT": 67., "cost": '',                
                    "skims": ["GENCOST", "TIME", "DIST", "HOVDIST"]
                },                  
                {   # 4
                    "name": 'HOV2TOLL', "mode": 'H', "PCE": 1, "VOT": 67., "cost": '@cost_hov',      
                    "skims": ["GENCOST", "TIME", "DIST", "MLCOST", "TOLLCOST", "TOLLDIST"]
                },                
                {   # 5 
                    "name": 'HOV3GP', "mode": 's', "PCE": 1, "VOT": 67., "cost": '',                
                    "skims": []  # same as SOV_gp
                },                    
                {   # 6
                    "name": 'HOV3HOV', "mode": 'i', "PCE": 1, "VOT": 67., "cost": '',                
                    "skims": ["GENCOST", "TIME", "DIST", "HOVDIST"]
                },                  
                {   # 7
                    "name": 'HOV3TOLL', "mode": 'I', "PCE": 1, "VOT": 67., "cost": '@cost_hov',      
                    "skims": ["GENCOST", "TIME", "DIST", "MLCOST", "TOLLCOST", "TOLLDIST"]
                },                
                {   # 8
                    "name": 'TRKHGP', "mode": 'v', "PCE": 2.5, "VOT": 89., "cost": '',                
                    "skims": ["GENCOST", "TIME", "DIST"]
                },                    
                {   # 9
                    "name": 'TRKHTOLL',  "mode": 'V', "PCE": 2.5, "VOT": 89., "cost": '@cost_hvy_truck', 
                    "skims": ["GENCOST", "TIME", "DIST", "TOLLCOST"]
                },                
                {   # 10
                    "name": 'TRKLGP',    "mode": 't', "PCE": 1.3, "VOT": 67., "cost": '',                
                    "skims": ["GENCOST", "TIME", "DIST"]
                },                    
                {   # 11
                    "name": 'TRKLTOLL',  "mode": 'T', "PCE": 1.3, "VOT": 67., "cost": '@cost_auto',      
                    "skims": ["GENCOST", "TIME", "DIST", "TOLLCOST"]
                },                
                {   # 12
                    "name": 'TRKMGP',   "mode": 'm', "PCE": 1.5, "VOT": 68., "cost": '',                
                    "skims": ["GENCOST", "TIME", "DIST"]
                },                    
                {   # 13
                    "name": 'TRKMTOLL', "mode": 'M', "PCE": 1.5, "VOT": 68., "cost": '@cost_med_truck', 
                    "skims": ["GENCOST", "TIME", "DIST",  "TOLLCOST"]
                }                
            ]
            self.run_assignment(period, relative_gap, max_iterations, num_processors, scenario, classes)
            self.run_skims(period, num_processors, scenario, classes)

    def run_assignment(self, period, relative_gap, max_iterations, num_processors, scenario, classes):     
        emmebank = scenario.emmebank

        modeller = _m.Modeller()
        set_extra_function_para = modeller.tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        create_matrix = modeller.tool(
            "inro.emme.data.matrix.create_matrix")
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")    
        traffic_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        net_calc = gen_utils.NetworkCalculator(scenario)
        
        p = period.lower()
        assign_spec = self.base_assignment_spec(relative_gap, max_iterations, num_processors)
        with _m.logbook_trace("Prepare traffic data for period %s" % period):
            with _m.logbook_trace("Input link attributes"):
                # set extra attributes for the period for VDF
                # ul1 = @time_link
                # ul2 = transig flow -> volad
                # ul3 = @capacity_link
                el1 = "@green_to_cycle"
                el2 = "@volau"              # for skim only
                el3 = "@capacity_inter"       
                set_extra_function_para(el1, el2, el3, emmebank=emmebank)

                # set green to cycle to el1=@green_to_cycle for VDF
                att_name = "@green_to_cycle_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@green_to_cycle"
                create_attribute("LINK", new_att_name, att.description, 
                                  0, overwrite=True, scenario=scenario)
                net_calc(new_att_name, att_name, "modes=d")
                # set capacity_inter to el3=@capacity_inter for VDF
                att_name = "@capacity_inter_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@capacity_inter"
                create_attribute("LINK", new_att_name, att.description, 
                                  0, overwrite=True, scenario=scenario)
                net_calc(new_att_name, att_name, "modes=d")
                # set link time
                net_calc("ul1", "@time_link_%s" % p, "modes=d")
                net_calc("ul3", "@capacity_link_%s" % p, "modes=d")            
                # set number of lanes (not used in VDF, just for reference)
                net_calc("lanes", "@lane_%s" % p, "modes=d") 

            with _m.logbook_trace("Transit line headway and background traffic"):
                # set headway for the period
                hdw = {"ea": "@headway_op", 
                       "am": "@headway_am",
                       "md": "@headway_op", 
                       "pm": "@headway_pm",
                       "ev": "@headway_op"}
                net_calc("hdw", hdw[p], {"transit_line": "all"})

                # transit vehicle as background flow with periods
                period_hours = {'ea': 3, 'am': 3, 'md': 6.5, 'pm': 3.5, 'ev': 5}
                expression = "60 / (hdw) * vauteq * %s" % (period_hours[p])
                net_calc("ul2", expression, 
                    selections={"link": "all", "transit_line": "hdw=0.01,9999"},
                    aggregation="+")  

            with _m.logbook_trace("Per-class flow attributes"):
                for traffic_class in classes:
                    demand = 'mf"%s_%s"' % (period, traffic_class["name"])
                    #demand = demand + "_PCE" if 'TRK' in traffic_class["name"] else demand            
                    link_cost = "%s_%s" % (traffic_class["cost"], p) if traffic_class["cost"] else "@cost_operating"

                    att_name = "@%s" % (traffic_class["name"].lower())
                    att_des = "%s, %s, link volume"% (traffic_class["name"], period)
                    link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                    att_name = "@p%s" % (traffic_class["name"].lower())
                    att_des = "%s, %s, turn volume"% (traffic_class["name"], period)
                    turn_flow = create_attribute("TURN", att_name, att_des, 0, overwrite=True, scenario=scenario)

                    class_spec = {
                        "mode": traffic_class["mode"],
                        "demand": demand,
                        "generalized_cost": {
                            "link_costs": link_cost, "perception_factor": 1.0 / traffic_class["VOT"]
                        },
                        "results": {
                            "link_volumes": link_flow.id, "turn_volumes": turn_flow.id,
                            "od_travel_times": None 
                        }
                    }
                    assign_spec["classes"].append(class_spec)

        # Run assignment
        traffic_assign(assign_spec, scenario)
        return

    def run_skims(self, period, num_processors, scenario, classes):
        modeller = _m.Modeller()
        create_matrix = modeller.tool(
            "inro.emme.data.matrix.create_matrix")
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")    
        traffic_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        net_calc = gen_utils.NetworkCalculator(scenario)
        emmebank = scenario.emmebank
        p = period.lower()

        with self.setup_skims(period, scenario):
            if period == "MD":
                gen_truck_mode = 'D'
                classes.append({ 
                    "name": 'TRK', "mode": gen_truck_mode, "PCE": 1, "VOT": 67., "cost": '',
                    "skims": ["GENCOST", "TIME", "DIST", "MLCOST", "TOLLCOST"]
                })
            analysis_link = {
                "TIME":     "@timau", 
                "DIST":     "length", 
                "HOVDIST":  "@hovdist", 
                "TOLLCOST": "@tollcost",
                "MLCOST":   "@mlcost",
                "TOLLDIST": "@tolldist"
            }
            analysis_turn = {"TIME": "@ptimau"}
            with _m.logbook_trace("Link attributes for skims"):
                create_attribute("LINK", "@hovdist", "distance for HOV", 0, overwrite=True, scenario=scenario)
                create_attribute("LINK", "@tollcost", "Toll cost in cents", 0, overwrite=True, scenario=scenario)
                create_attribute("LINK", "@mlcost", "Manage lane cost in cents", 0, overwrite=True, scenario=scenario)
                create_attribute("LINK", "@tolldist", "Toll distance", 0, overwrite=True, scenario=scenario)

                net_calc("@hovdist", "length", {"link": "@lane_restriction=2,3"})
                net_calc("@tollcost", "@toll_%s" % p, {"link": "modes=d"})
                net_calc("@mlcost", "@toll_%s" % p, 
                    {"link": "not @toll_%s=0.0 and not @lane_restriction=4" % p})
                net_calc("@tolldist", "length", {"link": "not @toll_%s=0.0" % p})
                # TODO (optional): use temporary link attributes ?
                # link volume in @volau
                create_attribute("LINK", "@volau", "traffic link volume (volau)", 
                                  0, overwrite=True, scenario=scenario)
                create_attribute("LINK", "@timau", "traffic link time (timau)", 
                                  0, overwrite=True, scenario=scenario)
                create_attribute("TURN", "@ptimau", "traffic turn time (ptimau)", 
                                  0, overwrite=True, scenario=scenario)
                net_calc("@volau", "volau", {"link": "modes=d"})
                net_calc("@timau", "timau", {"link": "modes=d"})
                net_calc("@ptimau", "ptimau*(ptimau.gt.0)",
                         {"incoming_link": "all", "outgoing_link": "all"})

            skim_spec = self.base_assignment_spec(0, 0, num_processors)        
            for traffic_class in classes:
                if not traffic_class["skims"]:
                    continue
                class_analysis = []
                if "GENCOST" in traffic_class["skims"]:
                    od_travel_times = 'mf"%s_%s_%s"' % (period, traffic_class["name"], "GENCOST")
                    traffic_class["skims"].remove("GENCOST")
                else:
                    od_travel_times = None
                for skim_type in traffic_class["skims"]:
                    class_analysis.append({
                        "link_component": analysis_link.get(skim_type),
                        "turn_component": analysis_turn.get(skim_type),
                        "operator": "+",
                        "selection_threshold": {"lower": None, "upper": None},
                        "path_to_od_composition": {
                            "considered_paths": "ALL",
                            "multiply_path_proportions_by": 
                                {"analyzed_demand": False, "path_value": True}
                        },
                        "results": {
                            "od_values": 'mf"%s_%s_%s"' % (period, traffic_class["name"], skim_type),
                            "selected_link_volumes": None,
                            "selected_turn_volumes": None
                        }
                    })
                if traffic_class["cost"]:
                    link_cost = "%s_%s" % (traffic_class["cost"], p)
                else:
                    link_cost = "@cost_operating"
                skim_spec["classes"].append({
                    "mode": traffic_class["mode"],
                    # 0 demand for skim with 0 iteration and fix flow in vdf
                    "demand": 'ms"zero"',
                    "generalized_cost": {
                        "link_costs": link_cost, "perception_factor": 1.0 / traffic_class["VOT"]
                    },
                    "results": {
                        "link_volumes": None, "turn_volumes": None,
                        "od_travel_times": {"shortest_paths": od_travel_times}
                    },
                    "path_analyses": class_analysis,
                })

            # skim assignment
            if self._skim_classes_separately:
                # Debugging check
                skim_classes = skim_spec["classes"][:]
                for kls in skim_classes:
                    skim_spec["classes"] = [kls]
                    traffic_assign(skim_spec, scenario)   
            else:
                traffic_assign(skim_spec, scenario)   
        
            # compute diagnal value for TIME and DIST
            with _m.logbook_trace("Compute diagnal values for period %s" % period):
                with gen_utils.temp_matrices(emmebank, "ORIGIN") as (mo_intra,):
                    mo_intra.name = "temp"
                    for traffic_class in classes:
                        class_name = traffic_class["name"]
                        skims = traffic_class["skims"]
                        with _m.logbook_trace("Class %s" % class_name):
                            for skim_type in skims:
                                name = 'mf"%s_%s_%s"' % (period, class_name, skim_type)
                                if skim_type == "TIME" or skim_type == "DIST":
                                    mo_intra.description = "temp intra zonal, %s" % name
                                    mo_intra.initialize(0)
                                    mat_spec = {
                                        "expression": name,
                                        "result": mo_intra.id,
                                        "constraint": {
                                            "by_value": {
                                                "od_values": name, "interval_min": 0, "interval_max": 0, "condition": "EXCLUDE"
                                            },
                                        },
                                        "aggregation": {"destinations": ".min."},
                                        "type": "MATRIX_CALCULATION"
                                    }
                                    matrix_calc(mat_spec, scenario, num_processors=num_processors)
                                    expression = "(p.eq.q) * 0.5 * %s + (p.ne.q) * %s" % (mo_intra.id, name)
                                else:
                                    expression = "(p.eq.q) * (-99999999.0) + (p.ne.q) * %s" % (name)
                                mat_spec = {
                                    "expression": expression, 
                                    "result": name,
                                    "constraint": {"by_value": None, "by_zone": None},
                                    "aggregation": {"origins": None, "destinations": None},
                                    "type": "MATRIX_CALCULATION"
                                }
                                matrix_calc(mat_spec, scenario, num_processors=num_processors)
        return

    def base_assignment_spec(self, relative_gap, max_iterations, num_processors):
        base_spec = {
            "type": "SOLA_TRAFFIC_ASSIGNMENT",
            "background_traffic": {
                "link_component": "ul2",     # ul2 = transit flow of the period
                "turn_component": None,
                "add_transit_vehicles": False
            },                
            "classes": [],
            "stopping_criteria": {
                "max_iterations": max_iterations, "best_relative_gap": 0.0,
                "relative_gap": relative_gap, "normalized_gap": 0.0
            },
            "performance_settings": {"number_of_processors": num_processors},
        }
        return base_spec

    @_context
    def setup_skims(self, period, scenario):
        modeller = _m.Modeller()
        emmebank = scenario.emmebank
        with _m.logbook_trace("Extract skims for period %s" % period):
            if period == "MD":
                gen_truck_mode = 'D'
                create_mode = modeller.tool(
                    "inro.emme.data.network.mode.create_mode")
                delete_mode = modeller.tool(
                    "inro.emme.data.network.mode.delete_mode")
                change_link_modes = modeller.tool(
                    "inro.emme.data.network.base.change_link_modes")

                with _m.logbook_trace("Preparation for generic truck skim"):
                    truck_mode = scenario.mode(gen_truck_mode)
                    if not truck_mode:
                        truck_mode = create_mode(mode_type="AUX_AUTO", mode_id=gen_truck_mode,
                                         mode_description="all trucks", scenario=scenario)
                    change_link_modes(modes=[truck_mode], action="ADD",
                                      selection="modes=vVmMtT", scenario=scenario)
            try:
                attrs = {
                    "LINK": ["auto_volume", "additional_volume", "auto_time"],
                    "TURN": ["auto_volume", "additional_volume", "auto_time"],
                }
                with gen_utils.backup_and_restore(scenario, attrs):
                    # temp_functions converts to skim-type VDFs
                    with gen_utils.temp_functions(emmebank):
                        yield
            finally:
                if period == "MD":
                    with _m.logbook_trace("Cleanup for generic truck skim"):
                        change_link_modes(modes=[truck_mode], action="DELETE",
                                          selection="all", scenario=scenario)
                        delete_mode(mode=truck_mode, scenario=scenario)
     
    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
