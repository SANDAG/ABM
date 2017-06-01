#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/truck/generation.py                                             ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 42


import inro.modeller as _m
import traceback as _traceback
import numpy as np
import pandas as pd
import os


class TruckGeneration(_m.Tool()):

    input_directory = _m.Attribute(str)
    input_truck_directory = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.input_truck_directory = os.path.join(os.path.dirname(project_dir), "input_truck")
        self._properties = None

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Truck generation"
        pb.description = """
<div style="text-align:left">
    Generates standard truck trip and special (military) truck trips as well as
    regional truck trips, IE trips, EI trips and EE trips and balances truck trips 
    productions / attractions.
</div> """
        pb.branding_text = "- SANDAG - Model - Truck"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')
        pb.add_select_file('input_truck_directory', 'directory',
                           title='Select truck input directory')

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.input_directory, self.input_truck_directory, scenario)
            run_msg = "Tool complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Truck generation')
    def __call__(self, input_directory, input_truck_directory, scenario):
        self.input_directory = input_directory
        self.input_truck_directory = input_truck_directory
        self.scenario = scenario
        utils = _m.Modeller().module('sandag.utilities.demand')

        self._properties = utils.Properties(
            os.path.join(input_directory, "sandag_abm.properties"))
        base_trucks_PA = self.truck_standard_generation()
        special_trucks_PA = self.special_truck_generation(base_trucks_PA)
        trucks_PA = self.balance_truck_PA(special_trucks_PA)
        self.store_PA_to_matrices(trucks_PA)
        self.read_external_external_demand()
        return trucks_PA

    # this assume that demographic information was loaded into datatable
    # using utils.csv_to_data_table
    def truck_standard_generation(self):
        year = self._properties['truck.FFyear']

        is_interim_year, prev_year, next_year = self.interim_year_check(year)
        if is_interim_year:
            # TODO - interpolation for interim year is untested
            raise Exception("tuck_model.generation: interim year interpolation not tested.")
        #     households_taz = self.interpolate_table(
        #         prev_year, year, next_year,
        #         'hhByTaz_%s' % prev_year,
        #         'hhByTaz_%s' % year,
        #         'hhByTaz_%s' % next_year)
        #     employments_taz = self.interpolate_table(
        #         prev_year, year, next_year,
        #         'empByTaz_%s' % prev_year,
        #         'empByTaz_%s' % year,
        #         'empByTaz_%s' % next_year)
            
        taz = self.create_demographics_by_taz()

        trip_rates = pd.read_csv(
            os.path.join(self.input_directory, 'TruckTripRates.csv'))

        taz = pd.merge(
            taz, trip_rates,
            left_on='truckregiontype', right_on='RegionType',
            how='left')
        taz.fillna(0, inplace=True)

        # Compute lhd truck productions "AGREMPN", "CONEMPN", "RETEMPN", "GOVEMPN", "MANEMPN", "UTLEMPN", "WHSEMPN", "OTHEMPN"}
        taz['LHD_Productions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TG_L_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TG_L_Retail'] \
            + taz['emp_gov'] * taz['TG_L_Government'] \
            + taz['emp_mfg'] * taz['TG_L_Manufacturing'] \
            + taz['emp_twu'] * taz['TG_L_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TG_L_Wholesale'] \
            + taz['emp_other'] * taz['TG_L_Other'] \
            + taz['hh'] * taz['TG_L_Households']

        taz['LHD_Attractions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TA_L_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TA_L_Retail'] \
            + taz['emp_gov'] * taz['TA_L_Government'] \
            + taz['emp_mfg'] * taz['TA_L_Manufacturing'] \
            + taz['emp_twu'] * taz['TA_L_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TA_L_Wholesale'] \
            + taz['emp_other'] * taz['TA_L_Other'] \
            + taz['hh'] * taz['TA_L_Households']

        taz['MHD_Productions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TG_M_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TG_M_Retail'] \
            + taz['emp_gov'] * taz['TG_M_Government'] \
            + taz['emp_mfg'] * taz['TG_M_Manufacturing'] \
            + taz['emp_twu'] * taz['TG_M_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TG_M_Wholesale'] \
            + taz['emp_other'] * taz['TG_M_Other'] \
            + taz['hh'] * taz['TG_M_Households']

        taz['MHD_Attractions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TA_M_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TA_M_Retail'] \
            + taz['emp_gov'] * taz['TA_M_Government'] \
            + taz['emp_mfg'] * taz['TA_M_Manufacturing'] \
            + taz['emp_twu'] * taz['TA_M_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TA_M_Wholesale'] \
            + taz['emp_other'] * taz['TA_M_Other'] \
            + taz['hh'] * taz['TA_M_Households']

        taz['HHD_Productions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TG_H_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TG_H_Retail'] \
            + taz['emp_gov'] * taz['TG_H_Government'] \
            + taz['emp_mfg'] * taz['TG_H_Manufacturing'] \
            + taz['emp_twu'] * taz['TG_H_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TG_H_Wholesale'] \
            + taz['emp_other'] * taz['TG_H_Other'] \
            + taz['hh'] * taz['TG_H_Households']

        taz['HHD_Attractions'] = \
            (taz['emp_agmin'] + taz['emp_cons']) * taz['TA_H_Ag/Min/Constr'] \
            + taz['emp_retrade'] * taz['TA_H_Retail'] \
            + taz['emp_gov'] * taz['TA_H_Government'] \
            + taz['emp_mfg'] * taz['TA_H_Manufacturing'] \
            + taz['emp_twu'] * taz['TA_H_Transp/Utilities'] \
            + taz['emp_whtrade'] * taz['TA_H_Wholesale'] \
            + taz['emp_other'] * taz['TA_H_Other'] \
            + taz['hh'] * taz['TA_H_Households']

        taz.reset_index(inplace=True)
        taz = taz[['taz',
                   'LHD_Productions', 'LHD_Attractions',
                   'MHD_Productions', 'MHD_Attractions',
                   'HHD_Productions', 'HHD_Attractions']]
        return taz


    # Creates households and employments by TAZ.
    # Specific to the truck trip generation model.
    # Inputs:
    #     - sandag.properties
    #     - input/mgra13_based_input20XX.csv
    def create_demographics_by_taz(self):
        utils = _m.Modeller().module('sandag.utilities.demand')
        dt = _m.Modeller().desktop.project.data_tables()
        year = self._properties['truck.FFyear']
        mgra = pd.read_csv(os.path.join(
            self.input_directory, 'mgra13_based_input%s.csv' % year))

        # Combine employment fields that match to the truck trip rate classification
        mgra['TOTEMP'] = mgra.emp_total
        mgra['emp_agmin'] = mgra.emp_ag
        mgra['emp_cons'] = mgra.emp_const_bldg_prod + mgra.emp_const_bldg_office
        mgra['emp_retrade'] = mgra.emp_retail + mgra.emp_personal_svcs_retail
        mgra['emp_gov']= mgra.emp_state_local_gov_ent \
                        + mgra.emp_state_local_gov_blue \
                        + mgra.emp_state_local_gov_white \
                        + mgra.emp_fed_non_mil \
                        + mgra.emp_fed_mil
        mgra['emp_mfg'] = mgra.emp_mfg_prod \
                         + mgra.emp_mfg_office
        mgra['emp_twu'] = mgra.emp_trans \
                      + mgra.emp_utilities_office \
                      + mgra.emp_utilities_prod
        mgra['emp_whtrade'] = mgra.emp_whsle_whs
        mgra['emp_other'] = mgra.TOTEMP \
                          - mgra.emp_agmin \
                          - mgra.emp_cons \
                          - mgra.emp_retrade \
                          - mgra.emp_gov \
                          - mgra.emp_mfg \
                          - mgra.emp_twu \
                          - mgra.emp_whtrade

        f = {
            'truckregiontype':['mean'],
            'emp_agmin':['sum'],
            'emp_cons': ['sum'],
            'emp_retrade': ['sum'],
            'emp_gov': ['sum'],
            'emp_mfg': ['sum'],
            'emp_twu': ['sum'],
            'emp_whtrade': ['sum'],
            'emp_other': ['sum'],
            'hh': ['sum']
        }

        mgra = mgra[['truckregiontype', 'emp_agmin', 'emp_cons',
                           'emp_retrade', 'emp_gov', 'emp_mfg', 'emp_twu',
                           'emp_whtrade', 'emp_other', 'taz', 'hh']]
        taz = mgra.groupby('taz').agg(f)
        taz.reset_index(inplace=True)
        taz.columns = taz.columns.droplevel(-1)
        # Add external zones
        taz = utils.add_missing_zones(taz, self.scenario)
        return taz

    # Add trucks generated by special generators, such as military sites,
    # mail to//from airport, cruise ships etc
    # Inputs:
    # - input/specialGenerators.csv
    # - dataframe: base_trucks (gmTruckDataII.csv)
    def special_truck_generation(self, base_trucks):
        year = self._properties['truck.FFyear']

        # is_interim_year, prev_year, next_year = self.interim_year_check(year)
        # if is_interim_year:
        #     prev_year = prev_year
        #     next_year = next_year
        #     spec_gen_prev = self.add_special_truck_PA(prev_year, base_trucks)
        #     spec_gen_next = self.add_special_truck_PA(next_year, base_trucks)
        #     special_trucks = self.interpolate_df(prev_year, year, next_year,
        #                                          spec_gen_prev, spec_gen_next)

        spec_gen = pd.read_csv(os.path.join(self.input_directory, 'specialGenerators.csv'))
        spec_gen = pd.merge(spec_gen, base_trucks,
                            left_on=['TAZ'], right_on=['taz'],
                            how='outer')
        spec_gen.fillna(0, inplace=True)

        for t in ['L', 'M', 'H']:
            spec_gen['%sHD_Attr' % t] = spec_gen['%sHD_Attractions' % t] + \
                                   (spec_gen['Y%s' % year] *
                                    spec_gen['trkAttraction'] *
                                    spec_gen['%shdShare' % t.lower()])
            spec_gen['%sHD_Prod' % t] = spec_gen['%sHD_Productions' % t] + \
                                   (spec_gen['Y%s' % year] *
                                    spec_gen['trkProduction'] *
                                    spec_gen['%shdShare' % t.lower()])

        special_trucks = spec_gen[
            ['taz', 'LHD_Prod', 'LHD_Attr', 'MHD_Prod', 'MHD_Attr', 'HHD_Prod', 'HHD_Attr']]
        return special_trucks

    # Balance truck Productions and Attractions
    def balance_truck_PA(self, truck_pa):
        truck_pa = self.balance_internal_truck_PA(truck_pa)
        regional_truck_pa = self.get_regional_truck_PA()
        truck_pa = self.add_balanced_regional_PA(regional_truck_pa, truck_pa)
        truck_pa.fillna(0, inplace=True)

        truck_pa['TRKL_Prod'] = truck_pa['LHD_Prod']
        truck_pa['TRKM_Prod'] = truck_pa['MHD_Prod']
        truck_pa['TRKH_Prod'] = truck_pa['HHD_Prod']
        truck_pa['TRKIE_Prod'] = truck_pa['IE_Prod']
        truck_pa['TRKEI_Prod'] = truck_pa['EI_Prod']

        truck_pa['TRKL_Attr'] = truck_pa['LHD_Attr']
        truck_pa['TRKM_Attr'] = truck_pa['MHD_Attr']
        truck_pa['TRKH_Attr'] = truck_pa['HHD_Attr']
        truck_pa['TRKIE_Attr'] = truck_pa['IE_Attr']
        truck_pa['TRKEI_Attr'] = truck_pa['EI_Attr']
        return truck_pa

    def get_regional_truck_PA(self):
        year = self._properties['truck.FFyear']
        trips = {}
        regional_trip_types = ['IE', 'EI', 'EE']
        # is_interim_year, prev_year, next_year = self.interim_year_check(year)
        # if is_interim_year:
        #     for t in regional_trip_types:
        #         prev_trips = pd.read_csv(os.path.join(
        #             self.input_truck_directory,
        #             'regional%strips%s.csv' % (t, prev_year)
        #         ))
        #         next_trips = pd.read_csv(os.path.join(
        #             self.input_truck_directory,
        #             'regional%strips%s.csv' % (t, next_year)
        #         ))
        #         trips_df = self.interpolate_df(
        #             prev_year, year, next_year,
        #             prev_trips, next_trips
        #         )
        #         trips[t] = trips_df

        for t in regional_trip_types:
            trips[t] = pd.read_csv(os.path.join(
                self.input_truck_directory,
                'regional%strips%s.csv' % (t, year)
            ))
        return trips

    def balance_internal_truck_PA(self, truck_pa):
        truck_types = ['LHD', 'MHD', 'HHD']
        for t in truck_types:
            s1 = truck_pa['%s_Prod' % t].sum()
            s2 = truck_pa['%s_Attr' % t].sum()
            avg = (s1 + s2)/2.0
            w1 = avg / s1
            w2 = avg / s2

            truck_pa['%s_Prod_unbalanced' % t] = truck_pa['%s_Prod' % t]
            truck_pa['%s_Attr_unbalanced' % t] = truck_pa['%s_Attr' % t]
            truck_pa['%s_Prod' % t] = truck_pa['%s_Prod' % t] * w1
            truck_pa['%s_Attr' % t] = truck_pa['%s_Attr' % t] * w2
        return truck_pa

    # Balance only EI and IE. EE truck trips are already balanced and can be
    # directly imported as a matrix
    def add_balanced_regional_PA(self, regional_trips, truck_pa):
        ei_trips = regional_trips['EI']
        ei_trips = ei_trips.groupby('fromZone').sum()
        ei_trips.reset_index(inplace=True)

        truck_pa = pd.merge(truck_pa,
                            ei_trips[['fromZone', 'EITrucks']],
                            left_on='taz', right_on='fromZone',
                            how='outer')

        sum_ei = ei_trips['EITrucks'].sum()
        sum_hhd_attr = truck_pa['HHD_Attr'].sum()
        truck_pa['EI_Attr'] = truck_pa['HHD_Attr'] * sum_ei / sum_hhd_attr
        truck_pa['EI_Prod'] = truck_pa['EITrucks']

        ie_trips = regional_trips['IE']
        ie_trips = ie_trips.groupby('toZone').sum()
        ie_trips.reset_index(inplace=True)
        truck_pa = pd.merge(truck_pa,
                            ie_trips[['toZone', 'IETrucks']],
                            left_on='taz', right_on='toZone',
                            how='outer')

        sum_ie = ie_trips['IETrucks'].sum()
        sum_hhd_prod = truck_pa['HHD_Prod'].sum()
        truck_pa['IE_Prod'] = truck_pa['HHD_Prod'] * sum_ie / sum_hhd_prod
        truck_pa['IE_Attr'] = truck_pa['IETrucks']
        truck_pa.fillna(0, inplace=True)

        return truck_pa

    def store_PA_to_matrices(self, truck_pa):
        emmebank = self.scenario.emmebank
        truck_pa.sort('taz', inplace=True)
        control_to_store = ['L', 'M', 'H', 'EI', 'IE']
        for t in control_to_store:
            prod = emmebank.matrix('moTRK%s_PROD' % t)
            prod.set_numpy_data(truck_pa['TRK%s_Prod' % t].values, self.scenario)
            attr = emmebank.matrix('mdTRK%s_ATTR' % t)
            attr.set_numpy_data(truck_pa['TRK%s_Attr' % t].values, self.scenario)

    @_m.logbook_trace('External - external truck matrix')
    def read_external_external_demand(self):
        utils = _m.Modeller().module('sandag.utilities.demand')
        # TODO: reads the regional truck table a second time
        emmebank = self.scenario.emmebank
        regional_trips = self.get_regional_truck_PA()
        ee = regional_trips['EE']
        m_ee = emmebank.matrix('mfTRKEE_DEMAND')
        m_ee_data = m_ee.get_data(self.scenario)
        for i, row in ee.iterrows():
            m_ee_data.set(row['fromZone'], row['toZone'], row['EETrucks'])
        m_ee.set_data(m_ee_data, self.scenario)

    # Generates data for the forecast years from years where data is
    # is available. Interpolates data from the closest previous/ next years.
    def interpolate_table(self, prev_year, new_year, next_year,
                          prev_table, new_table_name, next_table):
        utils = _m.Modeller().module('sandag.utilities.demand')
        prev_table = utils.table_to_dataframe(prev_table)
        next_table = utils.table_to_dataframe(next_table)
        new_df = self.interpolate_df(prev_year, new_year, next_year,
                                     prev_table, next_table)
        utils.dataframe_to_table(new_df, new_table_name)
        return new_df

    def interpolate_df(self, prev_year, new_year, next_year,
                       prev_year_df, next_year_df):
        current_year_df = pd.DataFrame()
        for key in prev_year_df.columns:
            current_year_df[key] = prev_year_df[key] \
                             + (new_year - prev_year) / (next_year-prev_year) \
                            * (next_year_df[key] - prev_year_df[key])
        return current_year_df

    def interim_year_check(self, year):
        year_with_data = self._properties['truck.DFyear']

        if year in year_with_data:
            return [False, year, year]

        else:
            next_year_idx = np.searchsorted(year_with_data, year)
            if next_year_idx == 0 or next_year_idx > len(year_with_data):
                raise Exception('Cannot interpolate data for year %s' % year)

            prev_year = year_with_data[next_year_idx - 1]
            next_year = year_with_data[next_year_idx]
            return [True, prev_year, next_year]
