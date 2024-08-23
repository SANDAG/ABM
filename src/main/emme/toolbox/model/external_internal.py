#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/external_internal.py                                            ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
# 
# 
# Runs the external USA to internal demand model. 
#    1) Work and non-work trip gateway total trips are read from control totals
#    2) Generates internal trip ends based on relative attractiveness from employment (by category) and households
#    3) Applies time-of-day and occupancy factors
#    4) Applies toll diversion model with toll and non-toll skims
# Control totals are read from externalInternalControlTotalsByYear.csv for
# the specified year in sandag_abm.properties. If this file does not exist
# externalInternalControlTotals.csv will be used instead.
#
# Inputs:
#    input_directory: source directory for most input files, including demographics and trip rates
#    scenario: traffic scenario to use for reference zone system
#
# Files referenced:
#    Note: YEAR is replaced by scenarioYear in the conf/sandag_abm.properties file 
#    input/mgra13_based_inputYEAR.csv
#    input/externalInternalControlTotalsByYear.csv
#    input/externalInternalControlTotals.csv 
#        (if externalInternalControlTotalsByYear.csv is unavailable)
#
# Matrix results:
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    input_dir = os.path.join(main_directory, "input")
    base_scenario = modeller.scenario
    external_internal = modeller.tool("sandag.model.external_internal")
    external_internal(input_dir, input_truck_dir, base_scenario)
"""

TOOLBOX_ORDER = 61


import inro.modeller as _m
import numpy as np
import pandas as pd
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


class ExternalInternal(_m.Tool(), gen_utils.Snapshot):
    input_directory = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["input_directory"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "External internal model"
        pb.description = """
            Runs the external USA to internal demand model.
            Control totals are read from externalInternalControlTotalsByYear.csv for
            the specified year in sandag_abm.properties. If this file does not exist
            externalInternalControlTotals.csv will be used instead."""
        pb.branding_text = "- SANDAG - Model"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.input_directory, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('External-internal model', save_arguments=True)
    def __call__(self, input_directory, scenario):
        attributes = {"input_directory": input_directory}
        gen_utils.log_snapshot("External-internal model", str(self), attributes)
        np.seterr(divide='ignore', invalid='ignore')

        emmebank = scenario.emmebank
        zones = scenario.zone_numbers
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(
            os.path.join(os.path.dirname(input_directory), "conf", "sandag_abm.properties"))

        year = int(props['scenarioYear'])
        mgra = pd.read_csv(
            os.path.join(input_directory, 'mgra15_based_input%s.csv' % year))

        # Load data
        file_path = os.path.join(
            input_directory, "externalInternalControlTotalsByYear.csv")
        if os.path.isfile(file_path):
            control_totals = pd.read_csv(file_path)
            control_totals = control_totals[control_totals.year==year]
            control_totals = control_totals.drop("year", axis=1)
        else:
            file_path = os.path.join(
                input_directory, 'externalInternalControlTotals.csv')
            if not os.path.isfile(file_path):
                raise Exception(
                    "External-internal model: no file 'externalInternalControlTotals.csv' "
                    "or 'externalInternalControlTotalsByYear.csv'")
            control_totals = pd.read_csv(file_path)
        _m.logbook_write("Control totals read from %s" % file_path)
        
        # Aggregate purposes
        mgra['emp_blu'] = (mgra.emp_con
                           + mgra.emp_utl
                           + mgra.emp_mnf
                           + mgra.emp_fin_res_mgm
                           + mgra.emp_whl
                           + mgra.emp_trn_wrh)

        mgra['emp_svc'] = mgra.emp_bus_svcs

        mgra['emp_edu'] = mgra.emp_educ

        mgra['work_size'] = (mgra.emp_blu +
                             1.364 * mgra.emp_ret +
                             4.264 * mgra.emp_ent +
                             0.781 * mgra.emp_svc +
                             1.403 * mgra.emp_edu +
                             1.779 * mgra.emp_hlth +
                             0.819 * mgra.emp_gov +
                             0.708 * mgra.emp_oth)

        mgra['non_work_size'] = (mgra.hh +
                                 1.069 * mgra.emp_blu +
                                 4.001 * mgra.emp_ret +
                                 6.274 * mgra.emp_ent +
                                 0.901 * mgra.emp_svc +
                                 1.129 * mgra.emp_edu +
                                 2.754 * mgra.emp_hlth +
                                 1.407 * mgra.emp_gov +
                                 0.304 * mgra.emp_oth)

        # aggregate to TAZ
        taz = mgra[['taz', 'work_size', 'non_work_size']].groupby('taz').sum()
        taz.reset_index(inplace=True)
        taz = dem_utils.add_missing_zones(taz, scenario)
        taz.sort_values('taz', ascending=True, inplace=True)   # method sort was deprecated since pandas version 0.20.0, yma, 2/12/2019
        taz.reset_index(inplace=True, drop=True)
        control_totals = pd.merge(control_totals, taz[['taz']], how='outer')
        control_totals.sort_values('taz', inplace=True)        # method sort was deprecated since pandas version 0.20.0, yma, 2/12/2019

        length_skim = emmebank.matrix('mf"SOV_TR_M_DIST__MD"').get_numpy_data(scenario)

        # Compute probabilities for work purpose
        wrk_dist_coef = -0.029
        wrk_prob = taz.work_size.values * np.exp(wrk_dist_coef * length_skim)
        wrk_sum = np.sum(wrk_prob, 1)
        wrk_prob = wrk_prob / wrk_sum[:, np.newaxis]
        wrk_prob = np.nan_to_num(wrk_prob)
        # Apply probabilities to control totals
        wrk_pa_mtx = wrk_prob * control_totals.work.values[:, np.newaxis]
        wrk_pa_mtx = np.nan_to_num(wrk_pa_mtx)
        wrk_pa_mtx = wrk_pa_mtx.astype("float32")
        
        # compute probabilities for non work purpose
        non_wrk_dist_coef = -0.006
        nwrk_prob = taz.non_work_size.values * np.exp(non_wrk_dist_coef * length_skim)
        non_wrk_sum = np.sum(nwrk_prob, 1)
        nwrk_prob = nwrk_prob / non_wrk_sum[:, np.newaxis]
        nwrk_prob = np.nan_to_num(nwrk_prob)
        # Apply probabilities to control totals
        nwrk_pa_mtx = nwrk_prob * control_totals.nonwork.values[:, np.newaxis]
        nwrk_pa_mtx = np.nan_to_num(nwrk_pa_mtx)
        nwrk_pa_mtx = nwrk_pa_mtx.astype("float32")

        # Convert PA to OD and apply Diurnal Facotrs
        wrk_ap_mtx = 0.5 * np.transpose(wrk_pa_mtx)
        wrk_pa_mtx = 0.5 * wrk_pa_mtx
        nwrk_ap_mtx = 0.5 * np.transpose(nwrk_pa_mtx)
        nwrk_pa_mtx = 0.5 * nwrk_pa_mtx

        # Apply occupancy and diurnal factors
        work_time_PA_factors = [0.26, 0.26, 0.41, 0.06, 0.02]
        work_time_AP_factors = [0.08, 0.07, 0.41, 0.42, 0.02]

        nonwork_time_PA_factors = [0.25, 0.39, 0.30, 0.04, 0.02]
        nonwork_time_AP_factors = [0.12, 0.11, 0.37, 0.38, 0.02]

        work_occupancy_factors = [0.58, 0.31, 0.11]
        nonwork_occupancy_factors = [0.55, 0.29, 0.15]

        # value of time is in cents per minute (toll cost is in cents)
        vot_work = 15.00  # $9.00/hr
        vot_non_work = 22.86  # $13.70/hr
        ivt_coef = -0.03

        gp_modes = ["SOVGP", "HOV2HOV", "HOV3HOV"]
        toll_modes = ["SOVTOLL", "HOV2TOLL", "HOV3TOLL"]
        # TODO: the GP vs. TOLL distinction should be collapsed 
        #       (all demand added to transponder demand in import_auto_demand)
        skim_lookup = {
            "SOVGP":    "SOV_NT_M", 
            "HOV2HOV":  "HOV2_M", 
            "HOV3HOV":  "HOV3_M",
            "SOVTOLL":  "SOV_TR_M", 
            "HOV2TOLL": "HOV2_M", 
            "HOV3TOLL": "HOV3_M"
        }
        periods = ["EA", "AM", "MD", "PM", "EV"]
        for p, w_d_pa, w_d_ap, nw_d_pa, nw_d_ap in zip(
                periods, work_time_PA_factors, work_time_AP_factors,
                nonwork_time_PA_factors, nonwork_time_AP_factors):
            for gp_mode, toll_mode, w_o, nw_o in zip(
                    gp_modes, toll_modes, work_occupancy_factors, nonwork_occupancy_factors):
                wrk_mtx = w_o * (w_d_pa * wrk_pa_mtx + w_d_ap * wrk_ap_mtx)
                nwrk_mtx = nw_o * (nw_d_pa * nwrk_pa_mtx + nw_d_ap * nwrk_ap_mtx)

                # Toll choice split
                f_tm_imp = emmebank.matrix('mf%s_TIME__%s' % (skim_lookup[gp_mode], p)).get_numpy_data(scenario)
                t_tm_imp = emmebank.matrix('mf%s_TIME__%s' % (skim_lookup[toll_mode], p)).get_numpy_data(scenario)
                t_cst_imp = emmebank.matrix('mf%s_TOLLCOST__%s' % (skim_lookup[toll_mode], p)).get_numpy_data(scenario)

                # Toll diversion for work purpose
                # TODO: .mod no longer needed, to confirm
                wrk_toll_prb = np.exp(
                    ivt_coef * (t_tm_imp - f_tm_imp + np.mod(t_cst_imp, 10000) / vot_work) - 3.39
                )
                wrk_toll_prb[t_cst_imp <= 0] = 0
                wrk_toll_prb = wrk_toll_prb / (1 + wrk_toll_prb)
                work_matrix_toll = wrk_mtx * wrk_toll_prb
                work_matrix_non_toll = wrk_mtx * (1 - wrk_toll_prb)

                toll_eiwork = emmebank.matrix('%s_%s_EIWORK' % (p, toll_mode))
                gp_ei_work = emmebank.matrix('%s_%s_EIWORK' % (p, gp_mode))
                toll_eiwork.set_numpy_data(work_matrix_toll, scenario)
                gp_ei_work.set_numpy_data(work_matrix_non_toll, scenario)

                # Toll diversion for non work purpose
                nwrk_toll_prb = np.exp(
                    ivt_coef * (t_tm_imp - f_tm_imp + np.mod(t_cst_imp, 10000) / vot_non_work) - 3.39
                )

                nwrk_toll_prb[t_cst_imp <= 0] = 0
                nwrk_toll_prb = nwrk_toll_prb / (1 + nwrk_toll_prb)

                non_work_toll_matrix = nwrk_mtx * nwrk_toll_prb
                non_work_gp_matrix = nwrk_mtx * (1 - nwrk_toll_prb)

                toll_einonwork =  emmebank.matrix('%s_%s_EINONWORK' % (p, toll_mode))
                gp_einonwork = emmebank.matrix('%s_%s_EINONWORK' % (p, gp_mode))
                toll_einonwork.set_numpy_data(non_work_toll_matrix, scenario)
                gp_einonwork.set_numpy_data(non_work_gp_matrix, scenario)

        precision = float(props['RunModel.MatrixPrecision'])
        self.matrix_rounding(scenario, precision)

    @_m.logbook_trace('Controlled rounding of demand')
    def matrix_rounding(self, scenario, precision):
        round_matrix = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_controlled_rounding")
        emmebank = scenario.emmebank
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        modes = ["SOVGP", "HOV2HOV", "HOV3HOV", "SOVTOLL", "HOV2TOLL", "HOV3TOLL"]
        purpose_types = ["EIWORK", "EINONWORK"]
        for period in periods:
            for mode in modes:
                for purpose in purpose_types:
                    matrix = emmebank.matrix("mf%s_%s_%s" % (period, mode, purpose))
                    try:
                        report = round_matrix(demand_to_round=matrix,
                                              rounded_demand=matrix,
                                              min_demand=precision,
                                              values_to_round="SMALLER_THAN_MIN",
                                              scenario=scenario)
                    except:
                        max_val = matrix.get_numpy_data(scenario.id).max()
                        if max_val == 0: 
                            # if max_val is 0 the error is that the matrix is 0, log a warning
                            _m.logbook_write('Warning: matrix %s is all 0s' % matrix.named_id)
                        else:
                            raise
