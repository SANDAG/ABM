'''
Script to turn series 15 data into proper ActivitySim Inputs

How to run:
* check paths, file names, and options listed in the __init__(self): function
* python series15_data_prep.py
'''

import openmatrix as omx
import numpy as np
import pandas as pd
import os
import shutil


class Series15_Processor:
    def __init__(self):
        # ------------ Run Settings & Input Files --------------
        self.input_dir = r'C:\ABM3_dev\run_data\data_2z_series15_inputs'
        self.output_dir = r'C:\ABM3_dev\run_data\data_2z_series15'
        assert os.path.isdir(self.input_dir), f"Cannot find input directory {self.input_dir}"
        assert os.path.isdir(self.output_dir), f"Cannot find output directory {self.output_dir}"

        self.ext_data_file = os.path.join(self.input_dir, 'externalInternalControlTotalsByYear.csv')
        self.base_year = 2022
        self.landuse_file = os.path.join(self.input_dir, 'mgra15_based_input2019_v3.csv')
        self.trans_access_file = os.path.join(self.input_dir, 'transponderModelAccessibilities.csv')
        self.terminal_time_file = os.path.join(self.input_dir, 'zone_term.csv')

        self.maz_ext_taz_xwalk_file = os.path.join(self.input_dir, 'closest_maz_to_external_tazs.csv')
        self.maz_maz_walk_file = os.path.join(self.input_dir, 'maz_maz_walk.csv')
        self.maz_stop_walk_file = os.path.join(self.input_dir, 'maz_stop_walk.csv')
        self.maz_maz_bike_file = os.path.join(self.input_dir, 'bikeMgraLogsum.csv')
        self.taz_taz_bike_file = os.path.join(self.input_dir, 'bikeTazLogsum.csv')

        self.walk_speed = 3  # mph

        # skims are copied from input dir to output dir before operating on them
        self.traffic_skim_list = [
            'traffic_skims_EA.omx',
            'traffic_skims_AM.omx',
            'traffic_skims_MD.omx',
            'traffic_skims_PM.omx',
            'traffic_skims_EV.omx',
        ]
        self.transit_skim_list = [
            'transit_skims_EA.omx',
            'transit_skims_AM.omx',
            'transit_skims_MD.omx',
            'transit_skims_PM.omx',
            'transit_skims_EV.omx',
        ]
        # below omx file and core are used to create 'DIST' skim
        self.traffic_dist_omx_file = os.path.join(self.output_dir, 'traffic_skims_AM.omx')
        self.traffic_dist_omx_core = 'SOV_TR_H_DIST__AM'

        # bike logsums are the same for each time period,
        # but need to add by time period for activitysim odt_skim wrapper
        # (tour mode choice model does not have do_skims, only dot_skim)
        self.add_time_independent_bike_logsums = True
        self.add_time_dependent_bike_logsums = True

        self.time_periods = ['EA', 'AM', 'MD', 'PM', 'EV']

        # synthetic population files
        self.households_file = os.path.join(self.input_dir, 'households.csv')
        self.persons_file = os.path.join(self.input_dir, 'persons.csv')

        # series 15 names to previous ABM2+ column names
        self.households_rename_dict = {
            'household_id': 'hhid',
            'TAZ': 'taz',
            'HHADJINC': 'hinc',
            'numWorkers': 'hworkers',
            'VEH': 'veh',
            'NP': 'persons',
            'HHT': 'hht',
            # '', 'bldgsz', # FIXME: bldgsz not in synthetic households
        }
        
        self.persons_rename_dict = {
            # '': 'perid',  # FIXME: unique perid not in synthetic persons
            'household_id': 'hhid',
            'AGEP': 'age',
            # '': 'pnum',  # FIXME: generated here
            'SEX': 'sex',
            # FIXME: all below generated here
            # '': 'pemploy',
            # '': 'pstudent',
            # '': 'ptype',
            # '': 'educ',
        }

    def copy_skims_and_process_names(self):
        '''
        Copy the skims from the input_dir to the output_dir.
        skims will be operated on in the output_dir by subsequent functions.

        matrix core names need to have their time period listed at the end:
            AM_SOV_NT_L_DIST -> SOV_NT_L_DIST__AM
        # '''
        skim_list = self.traffic_skim_list + self.transit_skim_list
        for skim_file in skim_list:
            source = os.path.join(self.input_dir, skim_file)
            dest = os.path.join(self.output_dir, skim_file)
            print("copying", skim_file)
            shutil.copy(source, dest)

        # only need to rename traffic skims as transit skims already have correct format
        for skim_file in self.traffic_skim_list:
            output_skims = omx.open_file(os.path.join(self.output_dir, skim_file), 'a')
            print(f"renaming skims in {skim_file}")
            for skims_name in output_skims.list_matrices():
                name_elems = skims_name.split('_')
                if name_elems[-1] in self.time_periods:
                    # already formatted correctly
                    continue
                assert name_elems[0] in self.time_periods, f"{name_elems[0]} not in {self.time_periods}"
                new_name = '_'.join(name_elems[1:]) + '__' + name_elems[0]
                output_skims[skims_name].rename(new_name)
            output_skims.close()
            

    def pre_process_landuse(self):
        landuse = pd.read_csv(self.landuse_file)
        landuse['MAZ'] = landuse['mgra']
        landuse['TAZ'] = landuse['taz']

        # dropping crossborder columns
        cols_to_drop = [col for col in landuse.columns if '_wait_' in col]
        landuse.drop(columns=cols_to_drop, inplace=True)

        na_cols = landuse.columns[landuse.isna().any()].tolist()
        if len(na_cols) > 0:
            print(f"WARNING: {na_cols} have NA values. Filling with 0!")
            landuse.fillna(0, inplace=True)

        # setting MAZ as index
        landuse.set_index('MAZ', inplace=True)
        self.landuse = landuse

    def process_synthetic_population(self):
        '''
        Series15 synthetic population does not contain all of the same columns as ABM2+ syn pop.
        This function will tranform the Series 15 to match what is expected.
        
        If these column names are changed, then the corresponding names need to be changed in the configs.
        '''
        households = pd.read_csv(self.households_file)
        persons = pd.read_csv(self.persons_file)

        # households = households.rename(columns=self.households_rename_dict)
        # # FIXME: bldgsz currnetly only used in auto ownership model checking if its a detached single family home
        # # using HHT to estimate: if married couple, or family with only male or female
        # households['bldgsz'] = np.where(households.hht.isin([1,2,3]), 2, -1) # detacted single family home
        # # FIXME: households have missing auto ownership & hht entries!
        # households['veh'] = households['veh'].fillna(0).clip(upper=4).astype(int)
        # households['hht'] = households['hht'].fillna(1).astype(int) # family household

        # persons = persons.rename(columns=self.persons_rename_dict)
        # persons['perid'] = persons.index.values + 1
        # persons['pnum'] = persons.groupby('hhid').cumcount() + 1

        # # inserting person type logic copied from SEMCOG annotate persons:
        # PEMPLOY_FULL, PEMPLOY_PART, PEMPLOY_NOT, PEMPLOY_CHILD = 1, 2, 3, 4
        # persons['pemploy'] = np.zeros(len(persons))
        # persons['pemploy'] = np.where(persons.age < 16, PEMPLOY_CHILD, PEMPLOY_PART)
        # persons['pemploy'] = np.where((persons.age >= 16) & ((persons.ESR == 3) | (persons.ESR == 6)), PEMPLOY_NOT, persons['pemploy'])
        # persons['pemploy'] = np.where((persons.age>=16) & ((persons.ESR != 3) & (persons.ESR != 6)) & (persons.WKHP >= 35), PEMPLOY_FULL, persons['pemploy'])
        # persons['pemploy'] = persons['pemploy'].astype(int)

        # PSTUDENT_GRADE_OR_HIGH, PSTUDENT_UNIVERSITY, PSTUDENT_NOT = 1, 2, 3
        # persons['pstudent'] = np.zeros(len(persons))
        # persons['pstudent'] = np.where((persons.pemploy == 1) & (persons.age >= 16), PSTUDENT_NOT, persons.pstudent)
        # persons['pstudent'] = np.where((persons.pemploy == 1) & (persons.age < 16), PSTUDENT_GRADE_OR_HIGH, persons.pstudent)
        # persons['pstudent'] = np.where((persons.SCHG < 1) & (persons.age >= 16), PSTUDENT_NOT, persons.pstudent)
        # persons['pstudent'] = np.where((persons.SCHG < 1) & (persons.age < 16), PSTUDENT_GRADE_OR_HIGH, persons.pstudent)
        # persons['pstudent'] = np.where((persons.SCHG >= 15) & (persons.age >= 16) & (persons.pemploy != 1), PSTUDENT_UNIVERSITY, persons.pstudent)
        # persons['pstudent'] = np.where((persons.SCHG >= 15) & (persons.age < 16) & (persons.pemploy != 1), PSTUDENT_GRADE_OR_HIGH, persons.pstudent)
        # persons['pstudent'] = np.where((persons.age <= 19) & (persons.pemploy != 1) & (persons.SCHG >=1) & (persons.SCHG<=14), PSTUDENT_GRADE_OR_HIGH, persons.pstudent)
        # persons['pstudent'] = np.where((persons.age > 19) & (persons.pemploy != 1) & (persons.SCHG >=1) & (persons.SCHG<=14),  PSTUDENT_UNIVERSITY, persons.pstudent)
        # persons['pstudent'] = np.where(persons.pstudent == 0, 3, persons.pstudent)
        # persons['pstudent'] = persons['pstudent'].astype(int)

        # PTYPE_FULL, PTYPE_PART, PTYPE_UNIVERSITY, PTYPE_NONWORK, PTYPE_RETIRED, PTYPE_DRIVING, PTYPE_SCHOOL, PTYPE_PRESCHOOL = 1, 2, 3, 4, 5, 6, 7, 8
        # persons['ptype'] = np.zeros(len(persons))
        # persons['ptype'] = np.where((persons.pemploy == 1),  PTYPE_FULL, PTYPE_NONWORK)
        # persons['ptype'] = np.where((persons.pstudent == 3) & (persons.pemploy == 2), PTYPE_PART, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 3) & (persons.age >= 65) & ((persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_RETIRED, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 3) & (persons.age < 6) & ((persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_PRESCHOOL, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 3) & (persons.age >= 6) & (persons.age <= 64) & ((persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_NONWORK, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 2)  & ((persons.pemploy == 2)  | (persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_UNIVERSITY, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 1) & (persons.age < 6)  & ((persons.pemploy == 2)  | (persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_PRESCHOOL, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 1) & (persons.age >= 16)  & ((persons.pemploy == 2)  | (persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_DRIVING, persons.ptype)
        # persons['ptype'] = np.where((persons.pstudent == 1) & (persons.age >= 6) & (persons.age < 16)  & ((persons.pemploy == 2)  | (persons.pemploy == 3) | (persons.pemploy == 4)), PTYPE_SCHOOL, persons.ptype)
        # persons['ptype'] = persons['ptype'].astype(int)

        # # FIXME assuming everyone age 18+ has high school degree and 22+ has college
        # # these are the two checks that are made in resident configs
        # persons['educ'] = np.where(persons.age >= 18, 9, 0)
        # persons['educ'] = np.where(persons.age >= 22, 13, persons.educ)

        self.households = households
        self.persons = persons

    def add_external_counts_to_landuse(self):
        print("Adding external counts to landuse file.")
        ext_data = pd.read_csv(self.ext_data_file)
        ext_data = ext_data[ext_data.year == self.base_year].reset_index(drop=True)
        # FIXME:
        # Placeholder data is derived from this table of tour weights from the crossborder survey. (Provided by Hannah). 
        # The estimated values is 20% of the purpose total * 2 to convert from tours to trips.  
        # The other 80% of border crossings are assumed to be from Mexican residents.
        # External taz numbers are also hard-coded here
        str_base_year = str(self.base_year)
        ext_data.loc[len(ext_data)] = [str_base_year, 1, 12526 * 0.2 * 2, (2337+55317+1872+3657) * 0.2 * 2]
        ext_data.loc[len(ext_data)] = [str_base_year, 2, 6443 * 0.2 * 2, (260+18579+1993+4585) * 0.2 * 2]
        ext_data.loc[len(ext_data)] = [str_base_year, 4, 2181 * 0.2 * 2, (1148+1052+305+1501) * 0.2 * 2]
        # dummy for other external taz's that are not yet active
        # (all TAZs need to be listed in the landuse file or the output trip omx trip matrices aren't the right shape!)
        ext_data.loc[len(ext_data)] = [str_base_year, 3, 0, 0]
        ext_data.loc[len(ext_data)] = [str_base_year, 5, 0, 0]
        ext_data.loc[len(ext_data)] = [str_base_year, 11, 0, 0]
        
        ext_data.sort_values(by='taz')

        # FIXME: landuse does not have crossborder poe_ids

        # adding external data to landuse file
        ext_maz_nums = []
        for index, row in ext_data.iterrows():
            if (row['taz'] in self.landuse.TAZ.values):
                ext_maz_num = self.landuse[self.landuse.TAZ == row['taz']].index[0]
            else:
                ext_maz_num = self.landuse.index.max() + 1
                self.landuse.loc[ext_maz_num] = 0
                self.landuse.loc[ext_maz_num, 'poe_id'] = -1

            self.landuse.loc[ext_maz_num, 'TAZ'] = row['taz']
            self.landuse.loc[ext_maz_num, 'external_work'] = row['work']
            self.landuse.loc[ext_maz_num, 'external_nonwork'] = row['nonwork']
            self.landuse.loc[ext_maz_num, 'external_TAZ'] = 1
            self.landuse.loc[ext_maz_num, 'external_MAZ'] = 1
            ext_maz_nums.append(ext_maz_num)

        self.landuse['mgra'] = self.landuse.index.values
        # FIXME: maintaining two  TAZ cols here... 
        self.landuse['taz'] = self.landuse['TAZ']

        print("\tAdded external mazs: ", ext_maz_nums)
        
        self.landuse['external_work'] = self.landuse['external_work'].fillna(0)
        self.landuse['external_nonwork'] = self.landuse['external_nonwork'].fillna(0)


    def add_maz_stop_walk_to_landuse(self):
        maz_stop_walk = pd.read_csv(self.maz_stop_walk_file)
        maz_stop_walk.set_index('maz', inplace=True)

        self.landuse['walk_dist_local_bus'] = maz_stop_walk['walk_dist_local_bus'].reindex(self.landuse.index)
        self.landuse['walk_dist_premium_transit'] = maz_stop_walk['walk_dist_premium_transit'].reindex(self.landuse.index)

        self.landuse['walk_dist_local_bus'].fillna(999, inplace=True)
        self.landuse['walk_dist_premium_transit'].fillna(999, inplace=True)


    def add_transponder_accessibility_to_landuse(self):
        print("Adding transponder accessibility variables to landuse file.")
        transponder_data = pd.read_csv(self.trans_access_file)
        transponder_data.rename(columns={'DIST':'ML_DIST'}, inplace=True)

        self.landuse = pd.merge(self.landuse.reset_index(), transponder_data, how='left', on='TAZ').set_index('MAZ')

    def add_terminal_time_to_landuse(self):
        print("Adding transponder accessibility variables to landuse file.")
        tt_data = pd.read_csv(self.terminal_time_file, header=None)
        tt_data.columns = ['MAZ', 'terminal_time']

        assert self.landuse.index.name == 'MAZ'
        self.landuse['terminal_time'] = tt_data.set_index('MAZ')['terminal_time'].reindex(self.landuse.index).fillna(0)

    def add_external_stations_to_skim_df(self, skim_df, maz_ext_taz_xwalk, landuse, origin_col='OMAZ', dest_col='DMAZ'):
        # helper function to add external stations to an maz-maz level skim
        external_zones = landuse.loc[landuse.external_MAZ == 1, 'TAZ'].to_frame()
        assert external_zones.index.name == 'MAZ', 'landuse index not MAZ'
        assert maz_ext_taz_xwalk.index.name == 'external_taz', 'external zone crosswalk index not external_taz'
        skim_length = len(skim_df)
        
        new_connections = []

        for ext_taz, ext_maz in zip(external_zones['TAZ'].values, external_zones.index):
            if ext_maz not in skim_df[origin_col].values:
                print(f"missing external maz {ext_maz}")
                closest_maz = maz_ext_taz_xwalk.loc[ext_taz, 'closest_maz']
                print(f"\t closest internal maz {closest_maz}")
                od_connections = skim_df.loc[skim_df[origin_col] == closest_maz].copy()
                print(f"\t origins with this internal maz {len(od_connections)}")
                od_connections[origin_col] = ext_maz
                new_connections.append(od_connections)
                
                if dest_col is not None:
                    do_connections = skim_df.loc[skim_df[dest_col] == closest_maz].copy()
                    do_connections[dest_col] = ext_maz
                    print(f"\t destinations with this internal maz {len(do_connections)}")
                    new_connections.append(do_connections)

        new_connections = pd.concat(new_connections)
        updated_skim = pd.concat([skim_df, new_connections])
        print(f"Added {len(updated_skim) - skim_length} O-D pairs to skim file")
        return updated_skim

    def add_exernal_stations_to_maz_level_skims(self):
        maz_ext_taz_xwalk = pd.read_csv(self.maz_ext_taz_xwalk_file)
        maz_ext_taz_xwalk = maz_ext_taz_xwalk.set_index('external_taz')
        
        # maz-maz walk
        print("adding external stations to maz-maz walk")
        maz_maz_walk = pd.read_csv(self.maz_maz_walk_file)
        maz_maz_walk_updated = self.add_external_stations_to_skim_df(maz_maz_walk, maz_ext_taz_xwalk, self.landuse)
        maz_maz_walk_updated['walkTime'] = maz_maz_walk_updated['DISTWALK'] / self.walk_speed * 60
        self.maz_maz_walk = maz_maz_walk_updated

        # maz-maz bike -- created using logsum file
        maz_maz_bike = pd.read_csv(self.maz_maz_bike_file)

        rename_col_dict = {
            'i': 'OMAZ',
            'j': 'DMAZ',
            'logsum': 'BIKE_LOGSUM',
            'time': 'BIKE_TIME'
        }
        maz_maz_bike.rename(columns=rename_col_dict, inplace=True)

        print("adding external stations to maz-maz bike")
        maz_maz_bike_with_ext = self.add_external_stations_to_skim_df(
            maz_maz_bike, maz_ext_taz_xwalk, self.landuse, origin_col='OMAZ', dest_col='DMAZ')
        self.maz_maz_bike = maz_maz_bike_with_ext

    def add_TAZ_level_skims(self):
        '''
        Two steps:
        1. Add a DIST skim
        2. Add walkTime skim
        2. Add bike logsum skims for each time period

        '''
        # creating DIST & walkTime skims
        print("Creating DIST and walkTime skims")
        dist_file = omx.open_file(self.traffic_dist_omx_file, 'a')

        sov_tr_dist_AM = dist_file[self.traffic_dist_omx_core]
        skim_shape = sov_tr_dist_AM.shape
        if 'DIST' not in dist_file.list_matrices():
            dist_file['DIST'] = sov_tr_dist_AM
        if 'walkTime' not in dist_file.list_matrices():
            dist_file['walkTime'] = np.array(sov_tr_dist_AM) / self.walk_speed * 60

        
        # Adding TAZ to TAZ Bike Logsum
        print("Creating bikeLogsum skims")
        taz_taz_bike = pd.read_csv(self.taz_taz_bike_file)

        # making sure all zones are there so we get a full table when we pivot
        unique_otazs = taz_taz_bike.i.unique()
        unique_dtazs = taz_taz_bike.j.unique()
        assert skim_shape[0] == len(self.landuse.TAZ.unique()), f"Skim has {skim_shape[0]} TAZs, but landuse has {len(self.landuse.TAZ.unique())}"
        missing_tazs = [taz_num for taz_num in self.landuse.TAZ.unique() if ((taz_num not in unique_otazs) | (taz_num not in unique_dtazs))]

        for taz in missing_tazs:
            taz_taz_bike.loc[len(taz_taz_bike.index)] = [taz, taz, -999, 0]

        taz_taz_bike.sort_values(by=['i', 'j'])

        logsum_skim = taz_taz_bike.pivot_table(values='logsum', index='i', columns='j').fillna(-999)

        biketime_skim = taz_taz_bike.pivot_table(values='time', index='i', columns='j').fillna(0)

        if self.add_time_independent_bike_logsums:
            if 'BIKE_LOGSUM' not in dist_file.list_matrices():
                dist_file['BIKE_LOGSUM'] = logsum_skim.to_numpy()
                dist_file['BIKE_TIME'] = biketime_skim.to_numpy()

        dist_file.close()

        if self.add_time_dependent_bike_logsums:
            for skim_file in self.traffic_skim_list:
                # assumes skim file has time period as the last two characters in the name
                # e.g. traffic_skims_AM.omx
                time_period = skim_file.strip('.omx')[-2:].upper()
                assert time_period in self.time_periods, f'time period {time_period} not in {self.time_periods}'
                skim = omx.open_file(os.path.join(self.output_dir, skim_file), 'a')
                if f'BIKE_LOGSUM__{time_period}' not in skim.list_matrices():
                    skim[f'BIKE_LOGSUM__{time_period}'] = logsum_skim.to_numpy()
                    skim[f'BIKE_TIME__{time_period}'] = biketime_skim.to_numpy()
                skim.close()

    def process_landuse(self):
        self.pre_process_landuse()
        self.add_external_counts_to_landuse()
        self.add_maz_stop_walk_to_landuse()
        self.add_transponder_accessibility_to_landuse()
        self.add_terminal_time_to_landuse()


    def write_output(self):
        print("Writing final outputs")
        self.landuse.to_csv(os.path.join(self.output_dir, 'land_use.csv'), index=True)
        self.persons.to_csv(os.path.join(self.output_dir, 'persons.csv'), index=True)
        self.households.to_csv(os.path.join(self.output_dir, 'households.csv'), index=True)
        self.maz_maz_walk.to_csv(os.path.join(self.output_dir, 'maz_maz_walk.csv'), index=True)
        self.maz_maz_bike.to_csv(os.path.join(self.output_dir, 'maz_maz_bike.csv'), index=True)


if __name__ == '__main__':
    processor = Series15_Processor()

    # running the following processing steps:
    processor.copy_skims_and_process_names()
    processor.process_synthetic_population()
    processor.process_landuse()
    processor.add_exernal_stations_to_maz_level_skims()
    processor.add_TAZ_level_skims()
    processor.write_output()