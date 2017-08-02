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

TOOLBOX_ORDER = 61


import inro.modeller as _m
import numpy as np
import pandas as pd
import traceback as _traceback
import os


gen_utils = _m.Modeller().module("sandag.utilities.general")


class ExternalInternal(_m.Tool(), gen_utils.Snapshot):
    input_directory = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
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
        load_properties = modeller.tool('sandag.utilities.properties')
        props = load_properties(
            os.path.join(os.path.dirname(input_directory), "conf", "sandag_abm.properties"))


        year = int(props['scenarioYear'])
        mgra = pd.read_csv(
            os.path.join(input_directory, 'mgra13_based_input%s.csv' % year))

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
                raise Exception("External-internal model: no file 'externalInternalControlTotals.csv' or 'externalInternalControlTotalsByYear.csv'")
            control_totals = pd.read_csv(file_path)
        _m.logbook_write("Control totals read from %s" % file_path)
        
        # Aggregate purposes
        mgra['emp_blu'] = (mgra.emp_const_non_bldg_prod
                           + mgra.emp_const_non_bldg_office
                           + mgra.emp_utilities_prod
                           + mgra.emp_utilities_office
                           + mgra.emp_const_bldg_prod
                           + mgra.emp_const_bldg_office
                           + mgra.emp_mfg_prod
                           + mgra.emp_mfg_office
                           + mgra.emp_whsle_whs
                           + mgra.emp_trans)

        mgra['emp_svc'] = (mgra.emp_prof_bus_svcs
                           + mgra.emp_prof_bus_svcs_bldg_maint
                           + mgra.emp_personal_svcs_office
                           + mgra.emp_personal_svcs_retail)

        mgra['emp_edu'] = (mgra.emp_pvt_ed_k12
                           + mgra.emp_pvt_ed_post_k12_oth
                           + mgra.emp_public_ed)

        mgra['emp_gov'] = (mgra.emp_state_local_gov_ent
                           + mgra.emp_fed_non_mil
                           + mgra.emp_fed_non_mil
                           + mgra.emp_state_local_gov_blue
                           + mgra.emp_state_local_gov_white)

        mgra['emp_ent'] = (mgra.emp_amusement
                           + mgra.emp_hotel
                           + mgra.emp_restaurant_bar)

        mgra['emp_oth'] = (mgra.emp_religious
                           + mgra.emp_pvt_hh
                           + mgra.emp_fed_mil)

        mgra['work_size'] = (mgra.emp_blu +
                             1.364 * mgra.emp_retail +
                             4.264 * mgra.emp_ent +
                             0.781 * mgra.emp_svc +
                             1.403 * mgra.emp_edu +
                             1.779 * mgra.emp_health +
                             0.819 * mgra.emp_gov +
                             0.708 * mgra.emp_oth)

        mgra['non_work_size'] = (mgra.hh +
                                 1.069 * mgra.emp_blu +
                                 4.001 * mgra.emp_retail +
                                 6.274 * mgra.emp_ent +
                                 0.901 * mgra.emp_svc +
                                 1.129 * mgra.emp_edu +
                                 2.754 * mgra.emp_health +
                                 1.407 * mgra.emp_gov +
                                 0.304 * mgra.emp_oth)

        # aggregate to TAZ
        taz = mgra[['taz', 'work_size', 'non_work_size']].groupby('taz').sum()
        taz.reset_index(inplace=True)
        taz = utils.add_missing_zones(taz, scenario)
        taz.sort('taz', ascending=True, inplace=True)
        taz.reset_index(inplace=True, drop=True)
        control_totals = pd.merge(control_totals, taz[['taz']], how='outer')
        control_totals.sort('taz', inplace=True)

        length_skim = emmebank.matrix('mf"MD_SOVTOLL_DIST"').get_numpy_data(scenario)

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
        periods = ["EA", "AM", "MD", "PM", "EV"]
        for p, w_d_pa, w_d_ap, nw_d_pa, nw_d_ap in zip(
                periods, work_time_PA_factors, work_time_AP_factors,
                nonwork_time_PA_factors, nonwork_time_AP_factors):
            for gp_mode, toll_mode, w_o, nw_o in zip(
                    gp_modes, toll_modes, 
                    work_occupancy_factors, nonwork_occupancy_factors):
                wrk_mtx = w_o * (w_d_pa * wrk_pa_mtx + w_d_ap * wrk_ap_mtx)
                nwrk_mtx = nw_o * (nw_d_pa * nwrk_pa_mtx + nw_d_ap * nwrk_ap_mtx)

                # Toll choice split
                f_tm_imp = emmebank.matrix('mf%s_%s_TIME' % (p, gp_mode)).get_numpy_data(scenario)
                t_tm_imp = emmebank.matrix('mf%s_%s_TIME' % (p, toll_mode)).get_numpy_data(scenario)
                t_cst_imp = emmebank.matrix('mf%s_%s_TOLLCOST' % (p, toll_mode)).get_numpy_data(scenario)

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
                toll_einonwork.set_numpy_data(non_work_gp_matrix, scenario)
                gp_einonwork.set_numpy_data(non_work_toll_matrix, scenario)
