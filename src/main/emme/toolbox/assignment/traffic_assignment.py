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
#
#
# The Traffic assignment tool runs the traffic assignment and skims per 
# period on the current primary scenario.
#
# The traffic assignment is a 15-class assignment with generalized cost on 
# links and BPR-type volume-delay functions which include capacities on links 
# and at intersection approaches. The assignment is run using the 
# fast-converging Second-Order Linear Approximation (SOLA) method in Emme to 
# a relative gap of 5x10-4. The per-link fixed costs include toll values and 
# operating costs which vary by class of demand. 
# Assignment matrices and resulting network flows are always in PCE.
#
# Inputs:
#   period: the time-of-day period, one of EA, AM, MD, PM, EV.
#   msa_iteration: global iteration number. If greater than 1, existing flow 
#       values must be present and the resulting flows on links and turns will 
#       be the weighted average of this assignment and the existing values.
#   relative_gap: minimum relative stopping criteria.
#   max_iterations: maximum iterations stopping criteria.
#   num_processors: number of processors to use for the traffic assignments.
#   select_link: specify one or more select link analysis setups as a list of 
#       specifications with three keys:
#           "expression": selection expression to identify the link(s) of interest. 
#           "suffix": the suffix to use in the naming of per-class result 
#               attributes and matrices, up to 6 characters.
#           "threshold": the minimum number of links which must be encountered 
#               for the path selection.
#        Example:
#   select_link = [
#       {"expression": "@tov_id=4578 or @tcov_id=9203", "suffix": "fwy", "threshold": "1"}
#   ]
#   raise_zero_dist: if checked, the distance skim for the SOVGP is checked for 
#       any zero values, which would indicate a disconnected zone, in which case 
#       an error is raised and the model run is halted.
#
# Matrices:
#   All traffic demand and skim matrices.
#   See list of classes under __call__ method, or matrix list under report method.
#
# Script example:
"""
import inro.modeller as _m
import os
import inro.emme.database.emmebank as _eb

modeller = _m.Modeller()
desktop = modeller.desktop
traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
load_properties = modeller.tool('sandag.utilities.properties')
project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
main_directory = os.path.dirname(project_dir)
output_dir = os.path.join(main_directory, "output")
props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))


main_emmebank = os.path.join(project_dir, "Database", "emmebank")
scenario_id = 100
base_scenario = main_emmebank.scenario(scenario_id)

periods = ["EA", "AM", "MD", "PM", "EV"]
period_ids = list(enumerate(periods, start=int(scenario_id) + 1))

msa_iteration = 1
relative_gap = 0.0005
max_assign_iterations = 100
num_processors = "MAX-1"
select_link = None  # Optional select link specification

for number, period in period_ids:
    period_scenario = main_emmebank.scenario(number)
    traffic_assign(period, msa_iteration, relative_gap, max_assign_iterations,
                   num_processors, period_scenario, select_link)
    omx_file = _join(output_dir, "traffic_skims_%s.omx" % period)
    if msa_iteration < 4:
        export_traffic_skims(period, omx_file, base_scenario)
"""


TOOLBOX_ORDER = 20


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class TrafficAssignment(_m.Tool(), gen_utils.Snapshot):

    period = _m.Attribute(str)
    msa_iteration = _m.Attribute(int)
    relative_gap = _m.Attribute(float)
    max_iterations = _m.Attribute(int)
    num_processors = _m.Attribute(str)
    select_link = _m.Attribute(str)
    raise_zero_dist = _m.Attribute(bool)
    stochastic = _m.Attribute(bool)
    input_directory = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        self.msa_iteration = 1
        self.relative_gap = 0.0005
        self.max_iterations = 100
        self.num_processors = "MAX-1"
        self.raise_zero_dist = True
        self.select_link = '[]'
        self.stochastic = False
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["period", "msa_iteration", "relative_gap", "max_iterations",
                           "num_processors", "select_link", "raise_zero_dist", "stochastic", "input_directory"]
        version = os.environ.get("EMMEPATH", "")
        self._version = version[-5:] if version else ""
        self._skim_classes_separately = True  # Used for debugging only
        self._stats = {}

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Traffic assignment"
        pb.description = """
The Traffic assignment tool runs the traffic assignment and skims per 
period on the current primary scenario.
<br>
The traffic assignment is a 15-class assignment with generalized cost on 
links and BPR-type volume-delay functions which include capacities on links 
and at intersection approaches. The assignment is run using the 
fast-converging Second-Order Linear Approximation (SOLA) method in Emme to 
a relative gap of 5x10-4. The per-link fixed costs include toll values and 
operating costs which vary by class of demand. 
Assignment matrices and resulting network flows are always in PCE.
"""
        pb.branding_text = "- SANDAG - Assignment"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        options = [("EA","Early AM"),
                   ("AM","AM peak"),
                   ("MD","Mid-day"),
                   ("PM","PM peak"),
                   ("EV","Evening")]
        pb.add_select("period", options, title="Period:")
        pb.add_text_box("msa_iteration", title="MSA iteration:", note="If >1 will apply MSA to flows.")
        pb.add_text_box("relative_gap", title="Relative gap:")
        pb.add_text_box("max_iterations", title="Max iterations:")
        dem_utils.add_select_processors("num_processors", pb, self)
        pb.add_checkbox("raise_zero_dist", title=" ", label="Raise on zero distance value",
            note="Check for and raise an exception if a zero value is found in the SOVGP_DIST matrix.")
        pb.add_checkbox(
            'stochastic',
            title=" ",
            label="Run as a stochastic assignment", 
            note="If the current MSA iteration is the last (4th) one, the SOLA traffic assignment is replaced with a stochastic traffic assignment."
        )
        pb.add_select_file('input_directory', 'directory', title='Select input directory')
        self._add_select_link_interface(pb)
        return pb.render()


    def _add_select_link_interface(self, pb):
        pb.add_html("""
<style type="text/css">
    table.select_link      {
        border-style: none;
        border-collapse:collapse;
        margin: 10px 10px 10px 10px;
    }
    table.select_link td   {
        padding: 2px 5px 0px 5px;
        font-size: 12px;
        vertical-align: top;
    }
    table.select_link th   {
        padding: 2px 5px 0px 5px;
        font-size: 12px;
        text-align: left;
        vertical-align: text-top;
    }
</style>""")
        pb.add_text_box("select_link", multi_line=True)
        pb.wrap_html(title="Select link(s):",
            body="""
<table id="ref_select_link" class="select_link">
    <tr><th width="20px"></th><th width="200px">Expression</th><th width="90px">Result suffix</th><th width="80px">Threshold</th></tr>
</table>
<div style="margin-left:20px;"><button type="button" id="add_select_link">Add select link</button></div>

<div class="t_block t_element -inro-util-disclosure">
    <div class="-inro-util-disclosure-header">
        Click for help
    </div>
    <div>
        <p>
            <strong>Expression:</strong> Emme selection expression to identify the link(s) of interest. 
            Examples and available attributes below.
        </p>
        <p>
            <strong>Result suffix:</strong> the suffix to use in the naming of per-class result 
            attributes and matrices, up to 6 characters. 
            Should be unique (existing attributes / matrices will be overwritten).
        </p>
        <p>
            <strong>Threshold:</strong> the minimum number of links which must be encountered 
            for the path selection. 
            The default value of 1 indicates an "any" link selection.
        </p>
        <p>
            Expression selection help: use one (or more) selection criteria of the form 
            <tt>attribute=value</tt> or <tt>attribute=min,max</tt>.
            Multiple criteria may be combined with 'and' ('&'), 'or' ('|'), and
            'xor' ('^'). Use 'not' ('!') in front or a criteria to negate it. <br>
            <a class="-inro-modeller-help-link"
                data-ref="qthelp://com.inro.emme.modeller_man/doc/html/modeller_man/network_selectors.html">
                More help on selection expressions
            </a>
            <ul>
                <li>Select by attribute: <tt>@selected_link=1</tt></li>
                <li>Select link by ID (i node, j node): <tt>link=1066,1422</tt></li>
                <li>Select TCOVED ID (two links): <tt>@tov_id=4578 or @tcov_id=9203</tt></li>
                <li>Outgoing connector: <tt>ci=1</tt></li>
                <li>Incoming connector: <tt>cj=1</tt></li>
                <li>Links of type 6 and 7: <tt>type=6,7</tt></li>
            </ul>
        </p>
        <p>
            Result link and turn flows will be saved in extra attributes
            <tt>@sel_XX_NAME_SUFFIX</tt>, where XX is the period, NAME is 
            the class name, and SUFFIX is the specified result suffix. 
            The selected O-D demand will be saved in SELDEM_XX_NAME_SUFFIX.
        </p>
    </div>
</div>
<div class="t_block t_element -inro-util-disclosure">
    <div class="-inro-util-disclosure-header">
        Click for available attributes
    </div>
    <div>
        <div id="link_attributes" class="link_attributes attr"></div>
    </div>
</div>""")
        pb.add_html("""
<script>
    $(document).ready( function ()
    {
        var tool = new inro.modeller.util.Proxy(%(tool_proxy_tag)s) ;

        window.select_link_label_tmpl = {
            value: inro.modeller.util.unserialize(tool.select_link),
            slave_text_field: "#select_link",
            num_select_link: 0,
            ready: true,
            update_text_field: function( ) {
                if( this.ready)
                {
                    var value = inro.modeller.util.serialize(this.value);
                    $(this.slave_text_field).val(value).trigger('change');
                }
                return(true);
            },
            add_select_row_ui: function( ) {
                var num = this.num_select_link;
                this.num_select_link += 1;
                var text = '<tr><th>' + num + ':</th>';
                text += '<td><textarea rows="1" style="width: 200px; max-width: 350px;" id="select_link_expression_' + num + '"' ;
                text += ' index="' + num + '" name="expression"></textarea></td>';
                text += '<td><input type="text" size="6" maxlength="6" id="select_link_suffix_' + num + '"' ;
                text += ' index="' + num + '" name="suffix"></input></td>';
                text += '<td><input type="text" size="4" id="select_link_threshold_' + num + '"' ;
                text += ' index="' + num + '" name="threshold"></input></td>';
                text += '</tr>';
                $("#ref_select_link").append(text);
                var select_items = ["expression", "suffix", "threshold"]
                for (var i = 0; i < select_items.length; i++) { 
                    var jq_obj = $('#select_link_' + select_items[i] + "_" + num);
                    jq_obj.val(this.value[num][select_items[i]]);
                    jq_obj.bind('change', function (){
                        var index = $(this).attr("index");
                        var name = $(this).attr("name");
                        window.select_link_label_tmpl.value[index][name] = $(this).val();
                        window.select_link_label_tmpl.update_text_field();
                    });
                }
            },
            init_current_select_row: function( ) {
                this.value[this.num_select_link] = {"expression": "", "suffix": "", "threshold": 1};
                this.update_text_field();
            },
            preload: function( ) {
                for (var i = 0; i < this.value.length; i++) { 
                    this.add_select_row_ui()
                }
            }
        };
        $("#select_link").parent().parent().hide();
        window.select_link_label_tmpl.preload();
        $("#add_select_link").bind('click', function()    {
            window.select_link_label_tmpl.init_current_select_row();
            window.select_link_label_tmpl.add_select_row_ui();
        });

        $("#link_attributes").html(tool.get_link_attributes());
   });
</script>""" % {"tool_proxy_tag": pb.tool_proxy_tag, })


    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            results = self(self.period, self.msa_iteration, self.relative_gap, self.max_iterations,
                           self.num_processors, scenario, self.select_link, self.raise_zero_dist,
                           self.stochastic, self.input_directory)
            run_msg = "Traffic assignment completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, msa_iteration, relative_gap, max_iterations, num_processors, scenario,
                 select_link=[], raise_zero_dist=True, stochastic=False, input_directory=None):
        select_link = _json.loads(select_link) if isinstance(select_link, str) else select_link
        attrs = {
            "period": period,
            "msa_iteration": msa_iteration,
            "relative_gap": relative_gap,
            "max_iterations": max_iterations,
            "num_processors": num_processors,
            "scenario": scenario.id,
            "select_link": _json.dumps(select_link),
            "raise_zero_dist": raise_zero_dist,
            "stochastic": stochastic,
            "input_directory": input_directory,
            "self": str(self)
        }
        self._stats = {}
        with _m.logbook_trace("Traffic assignment for period %s" % period, attributes=attrs):
            gen_utils.log_snapshot("Traffic assignment", str(self), attrs)
            periods = ["EA", "AM", "MD", "PM", "EV"]
            if not period in periods:
                raise _except.ArgumentError(
                    'period: unknown value - specify one of %s' % periods)
            num_processors = dem_utils.parse_num_processors(num_processors)
            # Main list of assignment classes
            classes = [
                {   # 0
                    "name": 'SOV_NT_L', "mode": 's', "PCE": 1, "VOT": 8.81, "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 1
                    "name": 'SOV_TR_L', "mode": 'S', "PCE": 1, "VOT": 8.81, "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 2
                    "name": 'HOV2_L', "mode": 'H', "PCE": 1, "VOT": 8.81, "cost": '@cost_hov2',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV2", "TOLLDIST.HOV2", "HOVDIST"]
                },
                {   # 3
                    "name": 'HOV3_L', "mode": 'I', "PCE": 1, "VOT": 8.81, "cost": '@cost_hov3',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV3", "TOLLDIST.HOV3", "HOVDIST"]
                },
                {   # 4
                    "name": 'SOV_NT_M', "mode": 's', "PCE": 1, "VOT": 18.0, "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 5
                    "name": 'SOV_TR_M', "mode": 'S', "PCE": 1, "VOT": 18.0, "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 6
                    "name": 'HOV2_M', "mode": 'H', "PCE": 1, "VOT": 18.0, "cost": '@cost_hov2',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV2", "TOLLDIST.HOV2", "HOVDIST"]
                },
                {   # 7
                    "name": 'HOV3_M', "mode": 'I', "PCE": 1, "VOT": 18.0, "cost": '@cost_hov3',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV3", "TOLLDIST.HOV3", "HOVDIST"]
                },
                {   # 8
                    "name": 'SOV_NT_H', "mode": 's', "PCE": 1, "VOT": 85., "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 9
                    "name": 'SOV_TR_H', "mode": 'S', "PCE": 1, "VOT": 85., "cost": '@cost_auto',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 10
                    "name": 'HOV2_H', "mode": 'H', "PCE": 1, "VOT": 85., "cost": '@cost_hov2',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV2", "TOLLDIST.HOV2", "HOVDIST"]
                },
                {   # 11
                    "name": 'HOV3_H', "mode": 'I', "PCE": 1, "VOT": 85., "cost": '@cost_hov3',
                    "skims": ["TIME", "DIST", "REL", "TOLLCOST.HOV3", "TOLLDIST.HOV3", "HOVDIST"]
                },
                {   # 12
                    "name": 'TRK_L', "mode": 'T', "PCE": 1.3, "VOT": 67., "cost": '@cost_lgt_truck',
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_L"]
                },
                {   # 13
                    "name": 'TRK_M', "mode": 'M', "PCE": 1.5, "VOT": 68., "cost": '@cost_med_truck',
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_M"]
                },
                {   # 14
                    "name": 'TRK_H', "mode": 'V', "PCE": 2.5, "VOT": 89., "cost": '@cost_hvy_truck',
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_H"]
                },
            ]
            
            # change mode to allow sovntp on SR125
            # TODO: incorporate this into import_network instead
            #       also, consider updating mode definitions
            self.change_mode_sovntp(scenario)
            
            if period == "MD" and (msa_iteration == 1 or not scenario.mode('D')):
                self.prepare_midday_generic_truck(scenario)

            if 1 < msa_iteration < 4:
                # Link and turn flows
                link_attrs = ["auto_volume"]
                turn_attrs = ["auto_volume"]
                for traffic_class in classes:
                    link_attrs.append("@%s" % (traffic_class["name"].lower()))
                    turn_attrs.append("@p%s" % (traffic_class["name"].lower()))
                msa_link_flows = scenario.get_attribute_values("LINK", link_attrs)[1:]
                msa_turn_flows = scenario.get_attribute_values("TURN", turn_attrs)[1:]

            if stochastic:
                self.run_stochastic_assignment(
                    period,
                    relative_gap, 
                    max_iterations, 
                    num_processors, 
                    scenario, 
                    classes,
                    input_directory 
                )
            else:
                self.run_assignment(period, relative_gap, max_iterations, num_processors, scenario, classes, select_link)


            if 1 < msa_iteration < 4:
                link_flows = scenario.get_attribute_values("LINK", link_attrs)
                values = [link_flows.pop(0)]
                for msa_array, flow_array in zip(msa_link_flows, link_flows):
                    msa_vals = numpy.frombuffer(msa_array, dtype='float32')
                    flow_vals = numpy.frombuffer(flow_array, dtype='float32')
                    result = msa_vals + (1.0 / msa_iteration) * (flow_vals - msa_vals)
                    result_array = array.array('f', result.tobytes())
                    values.append(result_array)
                scenario.set_attribute_values("LINK", link_attrs, values)

                turn_flows = scenario.get_attribute_values("TURN", turn_attrs)
                values = [turn_flows.pop(0)]
                for msa_array, flow_array in zip(msa_turn_flows, turn_flows):
                    msa_vals = numpy.frombuffer(msa_array, dtype='float32')
                    flow_vals = numpy.frombuffer(flow_array, dtype='float32')
                    result = msa_vals + (1.0 / msa_iteration) * (flow_vals - msa_vals)
                    result_array = array.array('f', result.tobytes())
                    values.append(result_array)
                scenario.set_attribute_values("TURN", turn_attrs, values)

            self.calc_network_results(period, num_processors, scenario)

            if msa_iteration <= 4:
                self.run_skims(period, num_processors, scenario, classes)
                self.report(period, scenario, classes)
                # Check that the distance matrix is valid (no disconnected zones)
                # Using SOVGPL class as representative
                if raise_zero_dist:
                    name = "SOV_TR_H_DIST__%s" % period
                    dist_stats = self._stats[name]
                    if dist_stats[1] == 0:
                        zones = scenario.zone_numbers
                        matrix = scenario.emmebank.matrix(name)
                        data = matrix.get_numpy_data(scenario)
                        row, col = numpy.unravel_index(data.argmin(), data.shape)
                        row, col = zones[row], zones[col]
                        raise Exception("Disconnected zone error: 0 value found in matrix %s from zone %s to %s" % (name, row, col))

    def run_assignment(self, period, relative_gap, max_iterations, num_processors, scenario, classes, select_link):
        emmebank = scenario.emmebank

        modeller = _m.Modeller()
        set_extra_function_para = modeller.tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        traffic_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        net_calc = gen_utils.NetworkCalculator(scenario)

        if period in ["AM", "PM"]:
            # For freeway links in AM and PM periods, convert VDF to type 25
            net_calc("vdf", "25", "vdf=10")

        p = period.lower()
        assign_spec = self.base_assignment_spec(
            relative_gap, max_iterations, num_processors)
        with _m.logbook_trace("Prepare traffic data for period %s" % period):
            with _m.logbook_trace("Input link attributes"):
                # set extra attributes for the period for VDF
                # ul1 = @time_link (period)
                # ul2 = transit flow -> volad (for assignment only)
                # ul3 = @capacity_link (period)
                el1 = "@green_to_cycle"
                el2 = "@sta_reliability"
                el3 = "@capacity_inter"
                set_extra_function_para(el1, el2, el3, emmebank=emmebank)

                # set green to cycle to el1=@green_to_cycle for VDF
                att_name = "@green_to_cycle_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@green_to_cycle"
                create_attribute("LINK", new_att_name, att.description,
                                  0, overwrite=True, scenario=scenario)
                net_calc(new_att_name, att_name, "modes=d")
                # set static reliability to el2=@sta_reliability for VDF
                att_name = "@sta_reliability_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@sta_reliability"
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
                if period in ["EA", "MD", "EV"]:
                    # For links with signals inactive in the off-peak periods, convert VDF to type 11
                    net_calc("vdf", "11", "modes=d and @green_to_cycle=0 and @traffic_control=4,5 and vdf=24")
                # # Set HOV2 cost attribute
                # create_attribute("LINK", "@cost_hov2_%s" % p, "toll (non-mngd) + cost for HOV2",
                #                  0, overwrite=True, scenario=scenario)
                # net_calc("@cost_hov2_%s" % p, "@cost_hov_%s" % p, "modes=d")
                # net_calc("@cost_hov2_%s" % p, "@cost_auto_%s" % p, "@hov=3")

            with _m.logbook_trace("Transit line headway and background traffic"):
                # set headway for the period
                hdw = {"ea": "@headway_ea",
                       "am": "@headway_am",
                       "md": "@headway_md",
                       "pm": "@headway_pm",
                       "ev": "@headway_ev"}
                net_calc("hdw", hdw[p], {"transit_line": "all"})

                # transit vehicle as background flow with periods
                period_hours = {'ea': 3, 'am': 3, 'md': 6.5, 'pm': 3.5, 'ev': 5}
                expression = "(60 / hdw) * vauteq * %s" % (period_hours[p])
                net_calc("ul2", "0", "modes=d")
                net_calc("ul2", expression,
                    selections={"link": "modes=d", "transit_line": "hdw=0.02,9999"},
                    aggregation="+")

            with _m.logbook_trace("Per-class flow attributes"):
                for traffic_class in classes:
                    demand = 'mf"%s_%s"' % (period, traffic_class["name"])
                    link_cost = "%s_%s" % (traffic_class["cost"], p) if traffic_class["cost"] else "@cost_operating"

                    att_name = "@%s" % (traffic_class["name"].lower())
                    att_des = "%s %s link volume" % (period, traffic_class["name"])
                    link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                    att_name = "@p%s" % (traffic_class["name"].lower())
                    att_des = "%s %s turn volume" % (period, traffic_class["name"])
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
            if select_link:
                for class_spec in assign_spec["classes"]:
                    class_spec["path_analyses"] = []
                for sub_spec in select_link:
                    expr = sub_spec["expression"]
                    suffix = sub_spec["suffix"]
                    threshold = sub_spec["threshold"]
                    if not expr and not suffix:
                        continue
                    with _m.logbook_trace("Prepare for select link analysis '%s' - %s" % (expr, suffix)):
                        slink = create_attribute("LINK", "@slink_%s" % suffix, "selected link for %s" % suffix, 0,
                                                 overwrite=True, scenario=scenario)
                        net_calc(slink.id, "1", expr)
                        with _m.logbook_trace("Initialize result matrices and extra attributes"):
                            for traffic_class, class_spec in zip(classes, assign_spec["classes"]):
                                att_name = "@sl_%s_%s" % (traffic_class["name"].lower(), suffix)
                                att_des = "%s %s '%s' sel link flow"% (period, traffic_class["name"], suffix)
                                link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                                att_name = "@psl_%s_%s" % (traffic_class["name"].lower(), suffix)
                                att_des = "%s %s '%s' sel turn flow" % (period, traffic_class["name"], suffix)
                                turn_flow = create_attribute("TURN", att_name, att_des, 0, overwrite=True, scenario=scenario)

                                name = "SELDEM_%s_%s_%s" % (period, traffic_class["name"], suffix)
                                desc = "Selected demand for %s %s %s" % (period, traffic_class["name"], suffix)
                                seldem = dem_utils.create_full_matrix(name, desc, scenario=scenario)

                                # add select link analysis
                                class_spec["path_analyses"].append({
                                    "link_component": slink.id,
                                    "turn_component": None,
                                    "operator": "+",
                                    "selection_threshold": { "lower": threshold, "upper": 999999},
                                    "path_to_od_composition": {
                                        "considered_paths": "SELECTED",
                                        "multiply_path_proportions_by": {"analyzed_demand": True, "path_value": False}
                                    },
                                    "analyzed_demand": None,
                                    "results": {
                                        "selected_link_volumes": link_flow.id,
                                        "selected_turn_volumes": turn_flow.id,
                                        "od_values": seldem.named_id
                                    }
                                })
        # Run assignment
        traffic_assign(assign_spec, scenario, chart_log_interval=2)
        return

    def run_stochastic_assignment(
            self, period, relative_gap, max_iterations, num_processors, scenario,
            classes, input_directory):
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        main_directory = os.path.dirname(input_directory)
        props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
        distribution_type = props['stochasticHighwayAssignment.distributionType']
        replications = props['stochasticHighwayAssignment.replications']
        a_parameter = props['stochasticHighwayAssignment.aParameter']
        b_parameter = props['stochasticHighwayAssignment.bParameter']
        seed = props['stochasticHighwayAssignment.seed']

        emmebank = scenario.emmebank

        modeller = _m.Modeller()
        set_extra_function_para = modeller.tool(
            "inro.emme.traffic_assignment.set_extra_function_parameters")
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        traffic_assign = modeller.tool(
            "solutions.stochastic_traffic_assignment")
        net_calc = gen_utils.NetworkCalculator(scenario)

        if period in ["AM", "PM"]:
            # For freeway links in AM and PM periods, convert VDF to type 25 
            net_calc("vdf", "25", "vdf=10")
            
        p = period.lower()
        assign_spec = self.base_assignment_spec(
            relative_gap, max_iterations, num_processors)
        assign_spec['background_traffic'] = {
            "link_component": None,
            "turn_component": None,
            "add_transit_vehicles": True
        }
        with _m.logbook_trace("Prepare traffic data for period %s" % period):
            with _m.logbook_trace("Input link attributes"):
                # set extra attributes for the period for VDF
                # ul1 = @time_link (period)
                # ul2 = transit flow -> volad (for assignment only)
                # ul3 = @capacity_link (period)
                el1 = "@green_to_cycle"
                el2 = "@sta_reliability"
                el3 = "@capacity_inter"
                set_extra_function_para(el1, el2, el3, emmebank=emmebank)

                # set green to cycle to el1=@green_to_cycle for VDF
                att_name = "@green_to_cycle_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@green_to_cycle"
                create_attribute("LINK", new_att_name, att.description,
                                  0, overwrite=True, scenario=scenario)
                net_calc(new_att_name, att_name, "modes=d")
                # set static reliability to el2=@sta_reliability for VDF
                att_name = "@sta_reliability_%s" % p
                att = scenario.extra_attribute(att_name)
                new_att_name = "@sta_reliability"
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
                if period in ["EA", "MD", "EV"]:
                    # For links with signals inactive in the off-peak periods, convert VDF to type 11
                    net_calc("vdf", "11", "modes=d and @green_to_cycle=0 and @traffic_control=4,5 and vdf=24")
                # # Set HOV2 cost attribute
                # create_attribute("LINK", "@cost_hov2_%s" % p, "toll (non-mngd) + cost for HOV2",
                #                  0, overwrite=True, scenario=scenario)
                # net_calc("@cost_hov2_%s" % p, "@cost_hov_%s" % p, "modes=d")
                # net_calc("@cost_hov2_%s" % p, "@cost_auto_%s" % p, "@hov=3")

            with _m.logbook_trace("Transit line headway and background traffic"):
                # set headway for the period: format is (attribute_name, period duration in hours) 
                hdw = {"ea": ("@headway_ea", 3),
                       "am": ("@headway_am", 3),
                       "md": ("@headway_md", 6.5),
                       "pm": ("@headway_pm", 3.5),
                       "ev": ("@headway_ev", 5)}
                net_calc('ul2', '0', {'link': 'all'})
                net_calc('hdw', '9999.99', {'transit_line': 'all'})
                net_calc(
                    'hdw', "{hdw} / {p} ".format(hdw=hdw[p][0], p=hdw[p][1]),
                    {"transit_line": "%s=0.02,9999" % hdw[p][0]}
                )

            with _m.logbook_trace("Per-class flow attributes"):
                for traffic_class in classes:
                    demand = 'mf"%s_%s"' % (period, traffic_class["name"])
                    link_cost = "%s_%s" % (traffic_class["cost"], p) if traffic_class["cost"] else "@cost_operating"

                    att_name = "@%s" % (traffic_class["name"].lower())
                    att_des = "%s %s link volume" % (period, traffic_class["name"])
                    link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                    att_name = "@p%s" % (traffic_class["name"].lower())
                    att_des = "%s %s turn volume" % (period, traffic_class["name"])
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
        traffic_assign(
            assign_spec,
            dist_par={'type': distribution_type, 'A': a_parameter, 'B': b_parameter},
            replications=replications,
            seed=seed,
            orig_func=False,
            random_term='ul2',
            compute_travel_times=False,
            scenario=scenario
        )
        
        with _m.logbook_trace("Reset transit line headways"):
                # set headway for the period
                hdw = {"ea": "@headway_ea",
                       "am": "@headway_am",
                       "md": "@headway_md",
                       "pm": "@headway_pm",
                       "ev": "@headway_ev"}
                net_calc("hdw", hdw[p], {"transit_line": "all"})
        return

    def calc_network_results(self, period, num_processors, scenario):
        modeller = _m.Modeller()
        create_attribute = modeller.tool(
            "inro.emme.data.extra_attribute.create_extra_attribute")
        net_calc = gen_utils.NetworkCalculator(scenario)
        emmebank = scenario.emmebank
        p = period.lower()
        # ul2 is the total flow (volau + volad) in the skim assignment
        with _m.logbook_trace("Calculation of attributes for skims"):
            link_attributes = [
                ("@hovdist", "distance for HOV"),
                ("@tollcost", "Toll cost for SOV autos"),
                ("@h2tollcost", "Toll cost for hov2"),
                ("@h3tollcost", "Toll cost for hov3"),
                ("@trk_ltollcost", "Toll cost for light trucks"),
                ("@trk_mtollcost", "Toll cost for medium trucks"),
                ("@trk_htollcost", "Toll cost for heavy trucks"),
                ("@mlcost", "Manage lane cost in cents"),
                ("@tolldist", "Toll distance"),
                ("@h2tolldist", "Toll distance for hov2"),
                ("@h3tolldist", "Toll distance for hov3"),
                ("@reliability", "Reliability factor"),
                ("@reliability_sq", "Reliability factor squared"),
                ("@auto_volume", "traffic link volume (volau)"),
                ("@auto_time", "traffic link time (timau)"),
            ]
            for name, description in link_attributes:
                create_attribute("LINK", name, description,
                                 0, overwrite=True, scenario=scenario)
            create_attribute("TURN", "@auto_time_turn", "traffic turn time (ptimau)", 
                             overwrite=True, scenario=scenario)

            net_calc("@hovdist", "length", {"link": "@hov=2,3"})
            net_calc("@tollcost", "@cost_auto_%s - @cost_operating" % p)
            net_calc("@h2tollcost", "@cost_hov2_%s - @cost_operating" % p, {"link": "@hov=3,4"})
            net_calc("@h3tollcost", "@cost_hov3_%s - @cost_operating" % p, {"link": "@hov=4"})
            net_calc("@trk_ltollcost", "@cost_lgt_truck_%s - @cost_operating" % p)
            net_calc("@trk_mtollcost", "@cost_med_truck_%s - @cost_operating" % p)
            net_calc("@trk_htollcost", "@cost_hvy_truck_%s - @cost_operating" % p)
            net_calc("@mlcost", "@toll_%s" % p, {"link": "not @hov=4"})
            net_calc("@tolldist", "length", {"link": "@hov=2,4"})
            net_calc("@h2tolldist", "length", {"link": "@hov=3,4"})
            net_calc("@h3tolldist", "length", {"link": "@hov=4"})
            net_calc("@auto_volume", "volau", {"link": "modes=d"})
            net_calc("ul2", "volau+volad", {"link": "modes=d"})
            vdfs = [f for f in emmebank.functions() if f.type == "VOLUME_DELAY"]
            exf_pars = emmebank.extra_function_parameters
            for function in vdfs:
                expression = function.expression
                for exf_par in ["el1", "el2", "el3"]:
                    expression = expression.replace(exf_par, getattr(exf_pars, exf_par))
                # split function into time component and reliability component
                time_expr, reliability_expr = expression.split("*(1+@sta_reliability+")
                net_calc("@auto_time", time_expr, {"link": "vdf=%s" % function.id[2:]})
                net_calc("@reliability", "(@sta_reliability+" + reliability_expr, 
                         {"link": "vdf=%s" % function.id[2:]})

            net_calc("@reliability_sq", "@reliability**2", {"link": "modes=d"})
            net_calc("@auto_time_turn", "ptimau*(ptimau.gt.0)",
                     {"incoming_link": "all", "outgoing_link": "all"})

    def run_skims(self, period, num_processors, scenario, classes):
        modeller = _m.Modeller()
        traffic_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        emmebank = scenario.emmebank
        p = period.lower()
        analysis_link = {
            "TIME":           "@auto_time",
            "DIST":           "length",
            "HOVDIST":        "@hovdist",
            "TOLLCOST.SOV":   "@tollcost",
            "TOLLCOST.HOV2":  "@h2tollcost",
            "TOLLCOST.HOV3":  "@h3tollcost",
            "TOLLCOST.TRK_L": "@trk_ltollcost",
            "TOLLCOST.TRK_M": "@trk_mtollcost",
            "TOLLCOST.TRK_H": "@trk_htollcost",
            "MLCOST":         "@mlcost",
            "TOLLDIST":       "@tolldist",
            "TOLLDIST.HOV2":  "@h2tolldist",
            "TOLLDIST.HOV3":  "@h3tolldist",
            "REL":            "@reliability_sq"
        }
        analysis_turn = {"TIME": "@auto_time_turn"}
        with self.setup_skims(period, scenario):
            if period == "MD":
                gen_truck_mode = 'D'
                classes.append({
                    "name": 'TRK', "mode": gen_truck_mode, "PCE": 1, "VOT": 67., "cost": '',
                    "skims": ["TIME"]
                })
            skim_spec = self.base_assignment_spec(0, 0, num_processors, background_traffic=False)
            for traffic_class in classes:
                if not traffic_class["skims"]:
                    continue
                class_analysis = []
                if "GENCOST" in traffic_class["skims"]:
                    od_travel_times = 'mf"%s_%s__%s"' % (traffic_class["name"], "GENCOST", period)
                    traffic_class["skims"].remove("GENCOST")
                else:
                    od_travel_times = None
                for skim_type in traffic_class["skims"]:
                    skim_name = skim_type.split(".")[0]
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
                            "od_values": 'mf"%s_%s__%s"' % (traffic_class["name"], skim_name, period),
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
                    "demand": 'ms"zero"',  # 0 demand for skim with 0 iteration and fix flow in ul2 in vdf
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

            # compute diagonal value for TIME and DIST
            with _m.logbook_trace("Compute diagonal values for period %s" % period):
                num_cells = len(scenario.zone_numbers) ** 2
                for traffic_class in classes:
                    class_name = traffic_class["name"]
                    skims = traffic_class["skims"]
                    with _m.logbook_trace("Class %s" % class_name):
                        for skim_type in skims:
                            skim_name = skim_type.split(".")[0]
                            name = '%s_%s__%s' % (class_name, skim_name, period)
                            matrix = emmebank.matrix(name)
                            data = matrix.get_numpy_data(scenario)
                            if skim_name == "TIME" or skim_name == "DIST":
                                numpy.fill_diagonal(data, 999999999.0)
                                data[numpy.diag_indices_from(data)] = 0.5 * numpy.nanmin(data[::,12::], 1)
                                internal_data = data[12::, 12::]  # Exclude the first 12 zones, external zones
                                self._stats[name] = (name, internal_data.min(), internal_data.max(), internal_data.mean(), internal_data.sum(), 0)
                            elif skim_name == "REL":
                                data = numpy.sqrt(data)
                            else:
                                self._stats[name] = (name, data.min(), data.max(), data.mean(), data.sum(), 0)
                                numpy.fill_diagonal(data, 0.0)
                            matrix.set_numpy_data(data, scenario)
        return

    def base_assignment_spec(self, relative_gap, max_iterations, num_processors, background_traffic=True):
        base_spec = {
            "type": "SOLA_TRAFFIC_ASSIGNMENT",
            "background_traffic": None,
            "classes": [],
            "stopping_criteria": {
                "max_iterations": int(max_iterations), "best_relative_gap": 0.0,
                "relative_gap": float(relative_gap), "normalized_gap": 0.0
            },
            "performance_settings": {"number_of_processors": num_processors},
        }
        if background_traffic:
            base_spec["background_traffic"] = {
                "link_component": "ul2",     # ul2 = transit flow of the period
                "turn_component": None,
                "add_transit_vehicles": False
            }
        return base_spec

    @_context
    def setup_skims(self, period, scenario):
        emmebank = scenario.emmebank
        with _m.logbook_trace("Extract skims for period %s" % period):
            # temp_functions converts to skim-type VDFs
            with temp_functions(emmebank):
                backup_attributes = {"LINK": ["data2", "auto_volume", "auto_time", "additional_volume"]}
                with gen_utils.backup_and_restore(scenario, backup_attributes):
                    yield

    def prepare_midday_generic_truck(self, scenario):
        modeller = _m.Modeller()
        create_mode = modeller.tool(
            "inro.emme.data.network.mode.create_mode")
        delete_mode = modeller.tool(
            "inro.emme.data.network.mode.delete_mode")
        change_link_modes = modeller.tool(
            "inro.emme.data.network.base.change_link_modes")
        with _m.logbook_trace("Preparation for generic truck skim"):
            gen_truck_mode = 'D'
            truck_mode = scenario.mode(gen_truck_mode)
            if not truck_mode:
                truck_mode = create_mode(
                    mode_type="AUX_AUTO", mode_id=gen_truck_mode,
                    mode_description="all trucks", scenario=scenario)
            change_link_modes(modes=[truck_mode], action="ADD",
                              selection="modes=vVmMtT", scenario=scenario)
            
    #added by RSG (nagendra.dhakar@rsginc.com) for collapsed assignment classes testing
    #this adds non-transponder SOV mode to SR-125 links
    # TODO: move this to the network_import step for consistency and foward-compatibility
    def change_mode_sovntp(self, scenario):
        modeller = _m.Modeller()
        change_link_modes = modeller.tool(
            "inro.emme.data.network.base.change_link_modes")
        with _m.logbook_trace("Preparation for sov ntp assignment"):
            gen_sov_mode = 's'
            sov_mode = scenario.mode(gen_sov_mode)
            change_link_modes(modes=[sov_mode], action="ADD",
                              selection="@hov=4", scenario=scenario)            

    def report(self, period, scenario, classes):
        emmebank = scenario.emmebank
        text = ['<div class="preformat">']
        matrices = []
        for traffic_class in classes:
            matrices.extend(["%s_%s" % (traffic_class["name"], s.split(".")[0]) for s in traffic_class["skims"]])
        num_zones = len(scenario.zone_numbers)
        num_cells = num_zones ** 2
        text.append("""
            Number of zones: %s. Number of O-D pairs: %s. 
            Values outside -9999999, 9999999 are masked in summaries.<br>""" % (num_zones, num_cells))
        text.append("%-25s %9s %9s %9s %13s %9s" % ("name", "min", "max", "mean", "sum", "mask num"))
        for name in matrices:
            name = name + "__" + period
            matrix = emmebank.matrix(name)
            stats = self._stats.get(name)
            if stats is None:
                data = matrix.get_numpy_data(scenario)
                data = numpy.ma.masked_outside(data, -9999999, 9999999, copy=False)
                stats = (name, data.min(), data.max(), data.mean(), data.sum(), num_cells-data.count())
            text.append("%-25s %9.4g %9.4g %9.4g %13.7g %9d" % stats)
        text.append("</div>")
        title = 'Traffic impedance summary for period %s' % period
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(title, report.render())

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    @_m.method(return_type=str)
    def get_link_attributes(self):
        export_utils = _m.Modeller().module("inro.emme.utility.export_utilities")
        return export_utils.get_link_attributes(_m.Modeller().scenario)


@_context
def temp_functions(emmebank):
    change_function = _m.Modeller().tool(
        "inro.emme.data.function.change_function")
    orig_expression = {}
    with _m.logbook_trace("Set functions to skim parameter"):
        for func in emmebank.functions():
            if func.prefix=="fd":
                exp = func.expression
                orig_expression[func] = exp
                if "volau+volad" in exp:
                    exp = exp.replace("volau+volad", "ul2")
                    change_function(func, exp, emmebank)
    try:
        yield
    finally:
        with _m.logbook_trace("Reset functions to assignment parameters"):
            for func, expression in orig_expression.items():
                change_function(func, expression, emmebank)

