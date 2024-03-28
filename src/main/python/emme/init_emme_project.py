#///////////////////////////////////////////////////////////////////////////////
#////                                                                        ///
#//// Copyright INRO, 2016-2019.                                             ///
#//// Rights to use and modify are granted to the                            ///
#//// San Diego Association of Governments and partner agencies.             ///
#//// This copyright notice must be preserved.                               ///
#////                                                                        ///
#//// init_emme_project.py                                                   ///
#////                                                                        ///
#////     Usage: init_emme_project.py [-r root] [-t title]                   ///
#////                                                                        ///
#////         [-r root]: Specifies the root directory in which to create     ///
#////              the Emme project.                                         ///
#////              If omitted, defaults to the current working directory     ///
#////         [-t title]: The title of the Emme project and Emme database.   ///
#////              If omitted, defaults to SANDAG empty database.            ///
#////         [-v emmeversion]: Emme version to use to create the project.   ///
#////              If omitted, defaults to 4.3.7.                            ///
#////                                                                        ///
#////                                                                        ///
#////                                                                        ///
#////                                                                        ///
#///////////////////////////////////////////////////////////////////////////////

import inro.emme.desktop.app as _app
import inro.emme.desktop.types as _ws_types
import inro.emme.database.emmebank as _eb
import argparse
import os

from collections import OrderedDict

WKT_PROJECTION = 'PROJCS["NAD_1983_NSRS2007_StatePlane_California_VI_FIPS_0406_Ft_US",GEOGCS["GCS_NAD_1983_NSRS2007",DATUM["D_NAD_1983_NSRS2007",SPHEROID["GRS_1980",6378137.0,298.257222101]],PRIMEM["Greenwich",0.0],UNIT["Degree",0.0174532925199433]],PROJECTION["Lambert_Conformal_Conic"],PARAMETER["False_Easting",6561666.666666666],PARAMETER["False_Northing",1640416.666666667],PARAMETER["Central_Meridian",-116.25],PARAMETER["Standard_Parallel_1",32.78333333333333],PARAMETER["Standard_Parallel_2",33.88333333333333],PARAMETER["Latitude_Of_Origin",32.16666666666666],UNIT["Foot_US",0.3048006096012192]];-118608900 -91259500 3048.00609601219;-100000 10000;-100000 10000;3.28083333333333E-03;0.001;0.001;IsHighPrecision'

def init_emme_project(root, title, emmeversion):
    project_path = _app.create_project(root, "emme_project")
    project_root = os.path.dirname(project_path)
    desktop = _app.start_dedicated(project=project_path, user_initials="WS", visible=False)
    project = desktop.project
    project.name = "SANDAG Emme project"
    prj_file_path = os.path.join(project_root, 'NAD 1983 NSRS2007 StatePlane California VI FIPS 0406 (US Feet).prj')
    with open(prj_file_path, 'w') as f:
        f.write(WKT_PROJECTION)
    project.spatial_reference_file = prj_file_path
    project.initial_view = _ws_types.Box(6.18187e+06, 1.75917e+06, 6.42519e+06, 1.89371e+06)

    property_path = os.path.join(root, "conf", "sandag_abm.properties")
    properties = load_properties(property_path)
    sla_limit = properties.get('traffic.sla_limit', 3)

    num_links = 160000
    num_turn_entries = 13000
    num_traffic_classes = 15
    if sla_limit > 3:
        # extra_attribute_values = 18000000 + 90000 * (sla_limit - 3)
        extra_attribute_values = 30000000 + (sla_limit - 3) * ((num_links + 1) * (num_traffic_classes + 1) + (num_turn_entries + 1) * (num_traffic_classes))
    else:
        extra_attribute_values = 30000000
    
    dimensions = {
        'scalar_matrices': 9999,
        'destination_matrices': 999,
        'origin_matrices': 999,
        'full_matrices': 9999,

        'scenarios': 10,
        'centroids': 5000,
        'regular_nodes': 29999,
        'links': num_links,
        'turn_entries': num_turn_entries,
        'transit_vehicles': 200,
        'transit_lines': 450,
        'transit_segments': 40000,
        'extra_attribute_values': extra_attribute_values,

        'functions': 99,
        'operators': 5000
    }

    # for Emme version > 4.3.7, add the sola_analyses dimension
    if emmeversion != '4.3.7':
        dimensions['sola_analyses'] = 240

    os.mkdir(os.path.join(project_root, "Database"))
    emmebank = _eb.create(os.path.join(project_root, "Database", "emmebank"), dimensions)
    emmebank.title = title
    emmebank.coord_unit_length = 0.000189394  # feet to miles
    emmebank.unit_of_length = "mi"
    emmebank.unit_of_cost = "$"
    emmebank.unit_of_energy = "MJ"
    emmebank.node_number_digits = 6
    emmebank.use_engineering_notation = True
    scenario = emmebank.create_scenario(100)
    scenario.title = "Empty scenario"
    emmebank.dispose()

    desktop.data_explorer().add_database(emmebank.path)
    desktop.add_modeller_toolbox("%<$ProjectPath>%/scripts/sandag_toolbox.mtbx")
    # desktop.add_modeller_toolbox("%<$ProjectPath>%/scripts/solutions.mtbx")
    project.save()

def load_properties(path):
    prop = OrderedDict()
    comments = {}
    with open(path, 'r') as properties:
        comment = []
        for line in properties:
            line = line.strip()
            if not line or line.startswith('#'):
                comment.append(line)
                continue
            key, value = line.split('=')
            key = key.strip()
            tokens = value.split(',')
            if len(tokens) > 1:
                value = _parse_list(tokens)
            else:
                value = _parse(value)
            prop[key] = value
            comments[key], comment = comment, []
    return prop

def _parse_list(values):
    converted_values = []
    for v in values:
        converted_values.append(_parse(v))
    return converted_values

def _parse(value):
    value = str(value).strip()
    if value == 'true':
        return True
    elif value == 'false':
        return False
    for caster in int, float:
        try:
            return caster(value)
        except ValueError:
            pass
    return value

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a new empty Emme project and database with Sandag defaults.")
    parser.add_argument('-r', '--root', help="path to the root ABM folder, default is the working folder",
                        default=os.path.abspath(os.getcwd()))
    parser.add_argument('-t', '--title', help="the Emmebank title",
                        default="SANDAG empty database")
    parser.add_argument('-v', '--emmeversion', help='the Emme version', default='4.3.7')
    args = parser.parse_args()

    init_emme_project(args.root, args.title, args.emmeversion)
