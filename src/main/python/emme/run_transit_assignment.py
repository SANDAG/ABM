
import inro.modeller as _m
import inro.emme.desktop.app as _app
import inro.emme.database.emmebank as _eb
import os
import argparse
import subprocess
import shutil
import time

_dir = os.path.dirname
_join= os.path.join


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-r", "--root_dir")
    parser.add_argument("-p", "--project_path")
    parser.add_argument("--number")
    parser.add_argument("--period")
    parser.add_argument("--proc")
    parser.add_argument("-o", "--output_dir")
    parser.add_argument("--create_connector_flag", action='store_true')
    args = parser.parse_args()
    print(args)
    main_directory = args.root_dir.strip('"')
    project_path = args.project_path.strip('"')
    number = args.number.strip('"')
    period = args.period.strip('"')
    num_processors = str(args.proc.strip('"'))
    output_dir = args.output_dir.strip('"')
    create_connector_flag = args.create_connector_flag
    print(main_directory, project_path)
    desktop = _app.start_dedicated(visible=True, user_initials="SD", project=project_path)
    modeller = _m.Modeller(desktop)
    
    with _m.logbook_trace("Running transit assignment for period " + period):
        transit_assign  = modeller.tool("sandag.assignment.transit_assignment")
        load_properties = modeller.tool('sandag.utilities.properties')
        export_transit_skims = modeller.tool("sandag.export.export_transit_skims")
        create_transit_connector = modeller.tool("sandag.assignment.create_transit_connector")

        props = load_properties(os.path.join(main_directory, "conf", "sandag_abm.properties"))
        # scenario_id = 100

        transit_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database_transit_" + period, "emmebank"))

        # periods = ["EA", "AM", "MD", "PM", "EV"]
        # period_ids = list(enumerate(periods, start=int(scenario_id) + 1))
        # num_processors = "9"
        scenarioYear = str(props["scenarioYear"])

        transit_assign_scen = transit_emmebank.scenario(number)

        create_transit_connector(period, transit_assign_scen, create_connector_flag, main_directory)

        # for number, period in period_ids:
        transit_assign(period, transit_assign_scen, data_table_name=scenarioYear,
                       skims_only=True, num_processors=num_processors)
        
        omx_file = _join(output_dir, "skims", "transit_skims_" + period + ".omx")
        export_transit_skims(omx_file, [period], transit_assign_scen, big_to_zero=False)

        transit_emmebank.dispose()
    desktop.close()

time.sleep(60)    
"""
python "C:\abm_runs\abm3_dev\emme_setup\python\emme\run_transit_assignment.py" -r "C:\\abm_runs\\abm3_dev\\emme_setup" -p "C:\\abm_runs\\abm3_dev\\emme_setup\\transit_assign_dummy_project\\dummy\\dummy.emp"
"""