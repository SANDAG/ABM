"""
Created on March 2020

@author: cliu
"""

TOOLBOX_ORDER = 105

import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.core.exception as _except
from collections import OrderedDict
import os
import pdb


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

format = lambda x: ("%.6f" % x).rstrip('0').rstrip(".")
id_format = lambda x: str(int(x))

class validation(_m.Tool(), gen_utils.Snapshot):

    main_directory = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    traffic_emmebank = _m.Attribute(str)
    attributes = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.base_scenario_id = 100
        self.traffic_emmebank = os.path.join(project_dir, "Database", "emmebank")
        self.attributes = ["main_directory", "base_scenario_id", "traffic_emmebank"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Validation Step"
        pb.description = """
Export traffic flow to csv files for base year validation."""
        pb.branding_text = "- SANDAG - Validation"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory', 'directory',
                           title='Select main directory')

        #pb.add_text_box('base_scenario_id', title="Base scenario ID:", size=10)
        pb.add_select_file('traffic_emmebank', 'file',
                           title='Select traffic emmebank')
        #pb.add_select_file('transit_emmebank', 'file',
        #                   title='Select transit emmebank')

        #dem_utils.add_select_processors("num_processors", pb, self)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(self.main_directory, self.base_scenario_id, self.traffic_emmebank)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, main_directory, base_scenario_id, traffic_emmebank):
        attrs = {
            "traffic_emmebank": str(traffic_emmebank),
            "main_directory": main_directory,
            "base_scenario_id": base_scenario_id,
            "self": str(self)
        }

        gen_utils.log_snapshot("Validation step", str(self), attrs)


        traffic_emmebank = _eb.Emmebank(traffic_emmebank)
        export_path = os.path.join(main_directory, "analysis/validation")
        hwylink_atts_file = os.path.join(export_path, "flow_raw.csv")

        periods = ["EA", "AM", "MD", "PM", "EV"]
        period_scenario_ids = OrderedDict((v, i) for i, v in enumerate(periods, start=base_scenario_id + 1))
        print "period_scenario_ids", period_scenario_ids

        for p, scen_id in period_scenario_ids.iteritems():
            base_scenario = traffic_emmebank.scenario(scen_id)

            network = base_scenario.get_partial_network(["LINK"], include_attributes=True)

            self.export_traffic_to_csv(export_path, traffic_emmebank, scen_id, network, hwylink_atts_file)


    def export_traffic_to_csv(self, export_path, traffic_emmebank, scen_id, network, filename):
        auto_mode = network.mode("d")
        # only the original forward direction links and auto links only
        hwyload_attrs = [("ID1", "@tcov_id")]
        att_list = [("AB_Flow", "@non_pce_flow")]
        for key, attr in att_list:
            hwyload_attrs.append((key, attr))
            #hwyload_attrs.append((key.replace("AB_", "BA_"), attr))
            hwyload_attrs.append((key.replace("AB_", "BA_"), (attr, "")))

        print "scen_id", scen_id
        scenario = traffic_emmebank.scenario(scen_id)
        links = [l for l in network.links()
                if l["@tcov_id"] > 0 and
                (auto_mode in l.modes or (l.reverse_link and auto_mode in l.reverse_link.modes))
                ]
        links.sort(key=lambda l: l["@tcov_id"])
        with open(filename, 'w') as fout:
            fout.write(",".join(['"%s"' % x[0] for x in hwyload_attrs]))
            fout.write("\n")
            for link in links:
                key, att = hwyload_attrs[0]  # expected to be the link id
                values = [id_format(link[att])]
                reverse_link = link.reverse_link
                for key, att in hwyload_attrs[1:]:
                    #print "key", key, "att", att, "values", values, "hwyload_attrs[0]", hwyload_attrs[0], "hwyload_attrs[1:]", hwyload_attrs[1:]
                    #print ("link", link, link['@speed_adjusted'])
                    #print ("link", link, link['@non_pce_flow'])

                    if key == "AN":
                        values.append(link.i_node.id)
                    elif key == "BN":
                        values.append(link.j_node.id)
                    elif key.startswith("BA"):
                        print "key", key, att
                        name, default = att
                        if reverse_link and (abs(link["@tcov_id"]) == abs(reverse_link["@tcov_id"])):
                            values.append(format(reverse_link[name]))
                        else:
                            values.append(default)

                        #values.append(format(reverse_link[name]) if reverse_link else default)
                    elif att.startswith("#"):
                        values.append('"%s"' % link[att])
                    else:
                        print ("line 145 att", att)

                        values.append(format(link['@speed_adjusted']))
                        values.append(format(link['@non_pce_flow']))
                        #values.append(format(link[att]))

                fout.write(",".join(values))
                fout.write("\n")

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg