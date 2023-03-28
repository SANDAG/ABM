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
#////     For running assignments on a remote server using PsExec,          ///
#////     via batch file which configures for Emme python, starts           ///
#////     or restarts the ISM and and maps T drive.                         ///
#////                                                                       ///
#////     The input arguments for the traffic assignment is read from       ///
#////     start_*.args file in the database directory (database_dir).       ///
#////     The "*" is one of the five time period abbreviations.             ///
#////                                                                       ///
#////     Usage: remote_run_traffic.py database_dir                         ///
#////                                                                       ///
#////         database_dir: The path to the directory with the period       ///
#////              specific traffic assignment data (scenarios and          ///
#////              matrices).                                               ///
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


class LogFile(object):
    def __init__(self, log_path):
        self._log_path = log_path
    def write(self, text):
        with open(self._log_path, 'a') as f:
            f.write(text)
    def write_timestamp(self, text):
        text = "%s - %s\n" % (_time.strftime("%Y-%m-%d %H:%M:%S"), text)
        self.write(text)
    def write_dict(self, value):
        with open(self._log_path, 'a') as f:
            _json.dump(value, f, indent=4)
            f.write("\n")


def run_assignment(modeller, database_path, period, msa_iteration,
                   relative_gap, max_assign_iterations, num_processors, 
                   period_scenario, select_link, logger):
    logger.write_timestamp("start for period %s" % period)
    traffic_assign  = modeller.tool("sandag.assignment.traffic_assignment")
    export_traffic_skims = modeller.tool("sandag.export.export_traffic_skims")
    with _emmebank.Emmebank(_join(database_path, 'emmebank')) as eb:
        period_scenario = eb.scenario(period_scenario)
        logger.write_timestamp("start traffic assignment")
        traffic_assign(
            period, msa_iteration, relative_gap, max_assign_iterations, 
            num_processors, period_scenario, select_link)
        logger.write_timestamp("traffic assignment finished, start export to OMX")
        output_dir = _join(_dir(_dir(database_path)), "output")
        omx_file = _join(output_dir, "traffic_skims_%s.omx" % period)  
        logger.write_timestamp("start export to OMX %s" % omx_file)
        if msa_iteration < 4:
            export_traffic_skims(period, omx_file, period_scenario)
        logger.write_timestamp("export to OMX finished")
        logger.write_timestamp("period %s completed successfully" % period)


if __name__ == "__main__":
    python_file, database_dir = sys.argv
    file_ref = os.path.split(database_dir)[1].lower()
    log_path = _join(_dir(_dir(database_dir)), "logFiles", "traffic_assign_%s.log" % file_ref)
    logger = LogFile(log_path)
    try:
        logger.write_timestamp("remote process started")
        # Test out licence by using the API
        eb = _emmebank.Emmebank(_join(database_dir, 'emmebank'))
        eb.close()
        logger.write_timestamp("starting Emme Desktop application")
        project_path = _join(_dir(database_dir), "emme_project.emp")
        desktop = _app.start_dedicated(True, "abc", project_path)
        try:
            logger.write_timestamp("Emme Desktop open")
            proc_logbook = _join("%<$ProjectPath>%", "Logbook", "project_%s_temp.mlbk" % file_ref)
            desktop.project.par("ModellerLogbook").set(proc_logbook)
            modeller = _m.Modeller(desktop)
            
            from_file = _join(database_dir, "start_*.args")
            all_files = _glob.glob(from_file)
            for path in all_files:
                input_args_file = _join(database_dir, path)  # communication file
                logger.write_timestamp("input args read from %s" % input_args_file)
                with open(input_args_file, 'r') as f:
                    assign_args = _json.load(f)
                logger.write_dict(assign_args)
                assign_args["logger"] = logger
                assign_args["modeller"] = modeller
                run_assignment(**assign_args)
        finally:
            desktop.close()
    except Exception as error:
        with open(_join(database_dir, "finish"), 'w', newline='') as f:
            f.write("FATAL ERROR\n")
        logger.write_timestamp("FATAL error execution stopped:")
        logger.write(str(error) + "\n")
        logger.write(_traceback.format_exc(error))
    finally:
        _time.sleep(1)
        with open(_join(database_dir, "finish"), 'a') as f:
            f.write("finish\n")
    sys.exit(0)
