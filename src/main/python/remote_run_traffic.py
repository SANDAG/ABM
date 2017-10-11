#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// remote_run_traffic.py                                                 ///
#////                                                                       ///
#////     Runs the traffic assignment(s) for the specified periods.         ///  
#////     For running assignments on a remote server using psexec,          ///
#////     via batch file which configures for Emme python and maps T        ///
#////     drive.                                                            ///
#////                                                                       ///
#////     Usage: remote_run_traffic.py database_dir periods                 ///
#////                                                                       ///
#////         database_dir: The path to the directory with the period       ///
#////              specific traffic assignment data (scenarios and          ///
#////              matrices).                                               ///
#////         periods: list of comma-separated periods e.g. AM,MD           ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////


import inro.emme.desktop.app as _app
import inro.modeller as _m
import inro.emme.database.emmebank as _emmebank
import json as _json
import traceback as _traceback
import glob as _glob
import time as _time
import sys
import os

_join = os.path.join
_dir = os.path.dirname


def run_assignment(database_path, period, msa_iteration, relative_gap, max_assign_iterations, 
                   num_processors, period_scenario, select_link, log_path):
    project_path = _join(_dir(database_path), "emme_project.emp")
    desktop = _app.start_dedicated(True, "abc", project_path)
    proc_logbook = _join("%<$ProjectPath>%", "Logbook", "project_%s_temp.mlbk" % period)
    desktop.project.par("ModellerLogbook").set(proc_logbook)
    modeller = _m.Modeller(desktop)
    traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
    export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")

    with _emmebank.Emmebank(_join(database_path, 'emmebank')) as eb:
        period_scenario = eb.scenario(period_scenario)
        with open(log_path, 'w') as f:
            f.write(_time.strftime("%Y-%m-%d %H:%M:%S"))
            f.write(" - start traffic assignment\n")
        traffic_assign(period, 
                       msa_iteration, 
                       relative_gap, 
                       max_assign_iterations, 
                       num_processors, 
                       period_scenario,
                       select_link)
        with open(log_path, 'w') as f:
            f.write(_time.strftime("%Y-%m-%d %H:%M:%S"))
            f.write(" - traffic assignment finished, start export to OMX\n")
        omx_file = _join(output_dir, "traffic_skims_%s.omx" % period)   
        export_traffic_skims(period, omx_file, base_scenario)


if __name__ == "__main__":
    python_file, database_path = sys.argv
    error = False
    try:
        from_file = _join(database_path, "start_*.args")
        all_files = _glob.glob(from_file)
        for path in all_files:
            # communication file
            with open(_join(database_path, path), 'r') as f:
                input_args = _json.loads(f.read())
            period = input_args["period"]
            log_path = _join(_dir(_dir(database_path)), "logFile", "traffic_assign_%s_remote_log.txt" % period)
            with open(log_path, 'w') as f:
                f.write(_time.strftime("%Y-%m-%d %H:%M:%S"))
                f.write(" - remote process started and input args read\n")
                _json.dump(input_args, f, indent=4)
                f.write("\n")
            input_args["log_path"] = log_path
            _time.sleep(1)
            try:
                run_assignment(**input_args)
            except Exception as error:
                with open(log_path, 'a') as f:
                    f.write(_time.strftime("%Y-%m-%d %H:%M:%S\n"))
                    f.write("FATAL error execution stopped:\n")
                    f.write(unicode(error) + "\n")
                    f.write(_traceback.format_exc(error))
                raise
            with open(log_path, 'w') as f:
                f.write(_time.strftime("%Y-%m-%d %H:%M:%S"))
                f.write(" - assignment and export to OMX completed successfully\n")
    except:
        error = True
        raise
    finally:
        _time.sleep(1)
        with open(_join(database_path, "finish"), 'w') as f:
            if error:
                f.write("FATAL ERROR\n")
            else:
                f.write("finish\n")
    sys.exit(0)
