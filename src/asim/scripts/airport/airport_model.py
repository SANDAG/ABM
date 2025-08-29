import pandas as pd
import numpy as np
import os
import sys
import yaml
from collections import OrderedDict
# import openmatrix as omx
import argparse
import subprocess
import itertools

def find_root_level(target):
    # Finds the target path if it is in a parent directory
    OS_ROOT = os.path.abspath('.').split(os.path.sep)[0] + os.path.sep
    pardir = ''
    while not os.path.exists(os.path.join(pardir, target)):
        pardir = os.path.join(os.pardir, pardir)
        if os.path.abspath(pardir) == OS_ROOT:
            print("Could not find directory specified in settings.yaml!")
            raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), target)
    return pardir

def create_tours(settings):
    """ Create tours from airport model settings and probability distributions"""
    print('Creating tours.')
    # employee_park = pd.read_csv(os.path.join(config_dir, settings['employee_park_fname']))
    # arrival_sched = pd.read_csv(os.path.join(config_dir, settings['arrival_sched_probs_fname']))
    # departure_sched = pd.read_csv(os.path.join(config_dir, settings['departure_sched_probs_fname']))
    purp_probs = pd.read_csv(os.path.join(config_dir, settings['purpose_probs_input_fname']))
    party_size_probs = pd.read_csv(os.path.join(config_dir, settings['party_size_probs_input_fname']))
    nights_probs_df = pd.read_csv(os.path.join(config_dir, settings['nights_probs_input_fname']))
    income_probs_df = pd.read_csv(os.path.join(config_dir, settings['income_probs_input_fname']))
    ext_station_probs_df = pd.read_csv(os.path.join(config_dir, settings['ext_station_probs_input_fname']))
    tour_settings = settings['tours']
    
    enplanements = tour_settings['num_enplanements']
    annualization = tour_settings['annualization_factor']
    connecting = tour_settings['connecting']
    avg_party_size = tour_settings['avg_party_size']
    airport_mgra = tour_settings['airport_mgra']
    
    num_tours = int((enplanements - connecting)/annualization/avg_party_size *2) 
    departing_tours = int(num_tours /2)
    arriving_tours = num_tours - departing_tours
    if settings['airport_code'] == 'CBX':
        employee_park = pd.read_csv(os.path.join(config_dir, settings['employee_park_fname']))
        employee_tours = int(sum(employee_park['Employee Stalls']*employee_park['Share to Terminal']))
    arr_tours = pd.DataFrame(
        index=range(arriving_tours), columns=[
            'direction', 'purpose','party_size','nights', 'income'])
    arr_tours.index.name = 'id'
    arr_tours['direction'] = 'inbound'
    dep_tours = pd.DataFrame(
        index=range(departing_tours), columns=[
            'direction', 'purpose','party_size','nights', 'income'])
    dep_tours.index.name = 'id'
    dep_tours['direction'] = 'outbound'

    if settings['airport_code'] == 'CBX':
        emp_tours = pd.DataFrame(
            index=range(employee_tours*2), columns=[
                'direction', 'purpose','party_size','nights', 'income'])
        emp_tours.index.name = 'id'
        emp_tours.loc[0:int(len(emp_tours)/2),'direction'] = 'inbound'
        emp_tours.loc[len(emp_tours)/2:len(emp_tours),'direction'] = 'outbound'
    
    # assign purpose
    purp_probs_sum = sum(purp_probs.Percent)
    purp_probs = {k: v / purp_probs_sum for k, v in zip(purp_probs['Purpose'],purp_probs['Percent'])}
    id_to_purp = {0:'purp0_perc',
                  1:'purp1_perc',
                  2:'purp2_perc',
                  3:'purp3_perc',
                  4:'purp4_perc',
                  5:'purp5_perc'}
    purp_cum_probs = np.array(list(purp_probs.values())).cumsum()
    
    for tour_table in [dep_tours, arr_tours]:
        purp_scaled_probs = np.subtract(
           purp_cum_probs, np.random.rand(len(tour_table), 1))
        purp_type_ids = np.argmax((purp_scaled_probs + 1.0).astype('i4'), axis=1)
        tour_table['purpose_id'] = purp_type_ids
        tour_table['purpose'] = tour_table['purpose_id'].map(id_to_purp)
    
    # time_probs_list = [departure_sched, arrival_sched]
    # time_col = ['start','end']
    for i,df in enumerate([dep_tours,arr_tours]):
        for purp_type, group in df.groupby('purpose'):
            num_purp_tours = len(group)
           
            #assign size
            size_probs = OrderedDict(party_size_probs[ purp_type])
            # scale probs to so they sum to 1
            size_sum = sum(size_probs.values())
            size_probs = {k: v / size_sum for k,v in size_probs.items()}
            size_cum_probs = np.array(list(size_probs.values())).cumsum()
            size_scaled_probs = np.subtract(
                size_cum_probs, np.random.rand(num_purp_tours, 1))
            size = np.argmax((size_scaled_probs + 1.0).astype('i4'), axis=1)
            group['party_size'] = size
            df.loc[group.index, 'party_size'] = size

            #assign nights
            nights_probs = OrderedDict(nights_probs_df[ purp_type])
            # scale probs to so they sum to 1
            nights_sum = sum(nights_probs.values())
            nights_probs = {k: v / nights_sum for k,v in nights_probs.items()}
            nights_cum_probs = np.array(list(nights_probs.values())).cumsum()
            nights_scaled_probs = np.subtract(
                nights_cum_probs, np.random.rand(num_purp_tours, 1))
            nights = np.argmax((nights_scaled_probs + 1.0).astype('i4'), axis=1)
            group['nights'] = nights
            df.loc[group.index, 'nights'] = nights

            #assign income
            income_probs = OrderedDict(income_probs_df[ purp_type])
            # scale probs to so they sum to 1
            income_sum = sum(income_probs.values())
            income_probs = {k: v / income_sum for k,v in income_probs.items()}
            income_cum_probs = np.array(list(income_probs.values())).cumsum()
            income_scaled_probs = np.subtract(
                income_cum_probs, np.random.rand(num_purp_tours, 1))
            income = np.argmax((income_scaled_probs + 1.0).astype('i4'), axis=1)
            group['income'] = income
            df.loc[group.index, 'income'] = income
    
    if settings['airport_code'] == 'CBX':
        #enumerate employee tours
        emp_tours['purpose'] = 'purp5_perc'
        emp_tours['purpose_id'] = 5
        emp_tours['party_size'] = 1
        emp_tours['nights'] = -99
        emp_tours['income'] = -99
        #choose employee park destination
        park_probs_sum = sum(employee_park['Employee Stalls']*employee_park['Share to Terminal'])
        employee_park = employee_park[employee_park['Share to Terminal'] > 0]
        if park_probs_sum > 0:
            park_probs = {k: v / park_probs_sum for k, v in zip(employee_park['MGRA'],employee_park['Employee Stalls']*employee_park['Share to Terminal'])}
        else:
            park_probs = {k: v for k,v in zip(employee_park['MGRA'],employee_park['Employee Stalls']*employee_park['Share to Terminal'])}
        park_cum_probs = np.array(list(park_probs.values())).cumsum()
        id_to_park = {k:v for k,v in zip(employee_park['Name']-1,employee_park['MGRA'])}

        for tour_table in [emp_tours]:
            park_scaled_probs = np.subtract(
               park_cum_probs, np.random.rand(len(tour_table), 1))
            park_type_ids = np.argmax((park_scaled_probs + 1.0).astype('i4'), axis=1)
            tour_table['parkinglot'] = park_type_ids
            tour_table['parkinglot'] = tour_table['parkinglot'].map(id_to_park)
        if len(emp_tours) > 0:
            emp_tours.loc[emp_tours['direction'] == 'inbound', 'destination'] = emp_tours[emp_tours.direction == 'inbound']['parkinglot']
            emp_tours.loc[emp_tours['direction'] == 'inbound', 'origin'] = airport_mgra
            emp_tours.loc[emp_tours['direction'] == 'outbound', 'origin'] = emp_tours[emp_tours.direction == 'outbound']['parkinglot']
            emp_tours.loc[emp_tours['direction'] == 'outbound', 'destination'] = airport_mgra
        #choose employee mode
        # employee_park = employee_park[employee_park['Public Transit Share to Terminal']>0]
            employee_mode = employee_park.copy()
            employee_mode['PT_terminal'] = employee_mode['Public Transit Share to Terminal']
            employee_mode['Mode'] = 'WALK_PRM'
            employee_mode_2 = employee_park.copy()
            employee_mode_2['PT_terminal'] = 1- employee_mode_2['Public Transit Share to Terminal']
            employee_mode_2['Mode'] = 'WALK'
            employee_mode = pd.concat([employee_mode, employee_mode_2])
            employee_mode = employee_mode.pivot(index = 'Mode', columns = 'MGRA', values = 'PT_terminal' ).reset_index().fillna(0)
            employee_mode['Name'] = pd.Series([0,1])
            final_employee = pd.DataFrame()
            for mgra in employee_park.MGRA.unique():
                mode_probs = {k: v  for k, v in zip(employee_mode['Mode'],employee_mode[mgra])}
                mode_cum_probs = np.array(list(mode_probs.values())).cumsum()
                id_to_mode = {k:v for k,v in zip(employee_mode['Name'],employee_mode['Mode'])}
            
                for tour_table in [emp_tours[emp_tours.parkinglot == mgra]]: #TODO remove this 'for' loop
                    mode_scaled_probs = np.subtract(
                       mode_cum_probs, np.random.rand(len(tour_table), 1))
                    mode_type_ids = np.argmax((mode_scaled_probs + 1.0).astype('i4'), axis=1)
                    tour_table['emp_trip_mode'] = mode_type_ids
                    tour_table['emp_trip_mode'] = tour_table['emp_trip_mode'].map(id_to_mode)
                    final_employee = pd.concat([final_employee, tour_table])
            final_employee = final_employee.drop('parkinglot',axis = 1)
        else:
            final_employee = emp_tours.drop('parkinglot',axis = 1).copy()
            final_employee['emp_trip_mode'] = None
            # # schedule tours
            # time_probs_list = [departure_sched, arrival_sched]
            # time_col = ['start','end']
            # for i, time in enumerate(time_probs_list):
            #     for purp_type, group in emp_tours.groupby('purpose'):
            #         num_purp_tours = len(group)
            #         time_probs = OrderedDict(time_probs_list[i][ purp_type])
            #         # scale probs to so they sum to 1
            #         time_sum = sum(time_probs.values())
            #         time_probs = {k: v / time_sum for k,v in time_probs.items()}
            #         time_cum_probs = np.array(list(time_probs.values())).cumsum()
            #         time_scaled_probs = np.subtract(
            #             time_cum_probs, np.random.rand(num_purp_tours, 1))
            #         time = np.argmax((time_scaled_probs + 1.0).astype('i4'), axis=1)
            #         group[time_col[i]] = time
            #         emp_tours.loc[group.index, time_col[i]] = time
    
        
    # pick external tour destination
    ext_probs_sum = sum(ext_station_probs_df['{}.Pct'.format(settings['airport_code'])])
    ext_probs_dep = {k: v / ext_probs_sum for k, v in zip(ext_station_probs_df['mgraRet'],ext_station_probs_df['{}.Pct'.format(settings['airport_code'])])}
    ext_probs_arr = {k: v / ext_probs_sum for k, v in zip(ext_station_probs_df['mgraOut'],ext_station_probs_df['{}.Pct'.format(settings['airport_code'])])}
    ext_cum_probs_dep = np.array(list(ext_probs_dep.values())).cumsum()
    ext_cum_probs_arr = np.array(list(ext_probs_arr.values())).cumsum()
    id_to_ext_dep = OrderedDict(ext_station_probs_df['mgraRet'])
    id_to_ext_arr = OrderedDict(ext_station_probs_df['mgraOut'])
    
    ext_cum_probs = [ext_cum_probs_dep, ext_cum_probs_arr]
    id_to_ext = [id_to_ext_dep,id_to_ext_arr]
    dep_tours_ext = dep_tours[dep_tours.purpose == 'purp4_perc']
    arr_tours_ext = arr_tours[arr_tours.purpose == 'purp4_perc']
    dep_tours = dep_tours[dep_tours.purpose != 'purp4_perc']
    arr_tours = arr_tours[arr_tours.purpose != 'purp4_perc']
    for i,tour_table in enumerate([dep_tours_ext, arr_tours_ext]):
        ext_scaled_probs = np.subtract(
           ext_cum_probs[i], np.random.rand(len(tour_table), 1))
        ext_type_ids = np.argmax((ext_scaled_probs + 1.0).astype('i4'), axis=1)
        tour_table['destination'] = ext_type_ids
        tour_table['destination'] = tour_table['destination'].map(id_to_ext[i])
    
        # time_probs = OrderedDict([ park_type])
    #         # scale probs to so they sum to 1
    #         time_sum = sum(time_probs.values())
    #         time_probs = {k: v / time_sum for k,v in time_probs.items()}
    #         time_cum_probs = np.array(list(time_probs.values())).cumsum()
    #         time_scaled_probs = np.subtract(
    #             time_cum_probs, np.random.rand(num_purp_tours, 1))
    #         time = np.argmax((time_scaled_probs + 1.0).astype('i4'), axis=1)
    #         group['departtime'] = time
    #         df.loc[group.index, 'departtime'] = time


    # for xborder model, only 1 person per tour and 1 tour per person
    # tours['number_of_participants'] = 1
    # tours['tour_num'] = 1
    # tours['tour_count'] = 1

    if settings['airport_code'] == 'SAN':
        tours = pd.concat([dep_tours,arr_tours,dep_tours_ext,arr_tours_ext],ignore_index = True).fillna(0)
        tours['tour_id'] = np.arange(1, len(tours) +1)
        tours = tours.set_index('tour_id')
        tours['tour_category'] = 'non_mandatory'
        tours['origin'] = airport_mgra

        segment_dict = {0:'res_bus', 1:'res_per', 2:'vis_bus', 3:'vis_per'}#, 4:'ext', 5:'emp'}
        tours['tour_type'] = tours['purpose_id'].map(segment_dict)
        tours['mode_segment'] = tours['tour_type'].copy()
        return tours
    
    elif settings['airport_code'] == 'CBX':
        tours = pd.concat([dep_tours,arr_tours,dep_tours_ext,arr_tours_ext,final_employee],ignore_index = True).fillna(0)
        tours['tour_id'] = np.arange(1, len(tours) +1)
        tours = tours.set_index('tour_id')
        tours['tour_category'] = 'non_mandatory'
        tours['origin'] = airport_mgra
        for i,purp in enumerate(['bus','per']):
            for income in range(8):
                tours.loc[(tours.purpose_id ==i) & (tours.income ==income), 'tour_type'] = 'res_{}{}'.format(purp,income+1)
                if i == 0:
                    tours.loc[(tours.purpose_id.isin([0,2])) & (tours.income == income), 'mode_segment'] = '{}{}'.format(purp,income+1)
                else:
                    tours.loc[(tours.purpose_id.isin([1,3])) & (tours.income == income), 'mode_segment'] = '{}{}'.format(purp,income+1)
        tours.loc[(tours.purpose_id ==2) , 'tour_type'] = 'vis_bus'
        tours.loc[(tours.purpose_id ==3) , 'tour_type'] = 'vis_per'
        tours.loc[(tours.purpose_id ==4) , 'tour_type'] = 'external'
        tours.loc[(tours.purpose_id ==5) , 'tour_type'] = 'emp'
        tours.loc[(tours.purpose_id.isin([5])), 'mode_segment'] = 'emp'
        for income in range(8):
            tours.loc[(tours.purpose_id == 4) & (tours.income ==income), 'mode_segment'] = 'ext{}'.format(income+1)
        return tours
    else:
        raise ValueError("preprocessing.yaml 'airport_code' invalid")

def create_sched_probs(settings):
    """ Create tours from airport model settings and probability distributions"""
    print('Creating tour scheduling probabilities.')
    arrival_sched = pd.read_csv(os.path.join(config_dir, settings['arrival_sched_probs_fname']))
    departure_sched = pd.read_csv(os.path.join(config_dir, settings['departure_sched_probs_fname']))
    asim_sched = [pd.DataFrame(columns = arrival_sched.columns[1:]),pd.DataFrame(columns = departure_sched.columns[1:])]
    for m,distribution in enumerate([ departure_sched,arrival_sched]):
        distribution = distribution.rename(columns = {'period':'Period'}).set_index('Period')
        for i in range(1,49):
            if i <= 4:
                asim_sched[m].loc[i] = distribution.loc[1]/4
            elif i < 43:
                asim_sched[m].loc[i] = distribution.loc[i-3]
            else:
                asim_sched[m].loc[i] = distribution.iloc[-1]/6
        # asim_sched[m] = pd.DataFrame(asim_sched[m].stack()).reset_index()#.rename(columns = {'index':'purpose'})
        asim_sched[m] = pd.DataFrame(asim_sched[m].T).reset_index().rename(columns = {'index':'purpose'})
    
        # asim_sched[m].columns = ['period','purpose','prob']
        asim_sched[m]['outbound'] = m==0
        asim_sched[m] = asim_sched[m][['purpose','outbound'] + [i for i in range(1,49)]]
        if m ==0:
            asim_sched[m].columns = [['purpose','outbound'] + ["1_{}".format(i) for i in range(1,49)]]
        else:
            asim_sched[m].columns = [['purpose','outbound'] + ["{}_48".format(i) for i in range(1,49)]]

        
    return pd.concat([asim_sched[0], asim_sched[1]]).fillna(0)
    
def create_households(tours):
    print("Creating households.")
    num_tours = len(tours)

    # one household per tour
    households = pd.DataFrame({'household_id': np.arange(1,num_tours +1)})
    households['home_zone_id'] = 3692
    return households


def create_persons(settings, num_households):

    print("Creating persons")
    # one person per household
    persons = pd.DataFrame({'person_id': np.arange(num_households)+1})
    persons['household_id'] = np.random.choice(
        num_households , num_households, replace=False)
    persons['household_id'] = persons['household_id'] +1
    return persons



def assign_hh_p_to_tours(tours, persons):

    num_tours = len(tours)

    # assign persons and households to tours
    tours['household_id'] = np.random.choice(
        num_tours, num_tours, replace=False)
    tours['household_id'] = tours['household_id'] +1
    tours['person_id'] = persons.set_index('household_id').reindex(
        tours['household_id'])['person_id'].values

    return tours

def create_landuse(settings):

    print("Creating land use")
    # one person per household
    input_lu = pd.read_csv(os.path.join(data_dir, settings['maz_input_fname']))
    synthetic_hh = pd.read_csv(os.path.join(data_dir, settings['hh_input_fname']))

    if 'MAZ' not in input_lu.columns:
        output_lu = input_lu.copy().rename(columns = {'mgra':'MAZ','taz':'TAZ'})
    else:   
        output_lu = input_lu.copy()

    synthetic_hh['airport_income_bin'] = pd.cut(synthetic_hh['hinc'], bins = [-99999999,25000,50000,75000,100000,125000,150000,200000,9999999999], labels = ['a1','a2','a3','a4','a5','a6','a7','a8'])
    synthetic_hh = synthetic_hh.groupby(['mgra','airport_income_bin'],as_index = False)[['hhid']].count() # TODO: change to sample rate
    synthetic_hh = pd.pivot(synthetic_hh, index = 'mgra', columns = 'airport_income_bin', values= 'hhid')
    output_lu = output_lu.set_index('MAZ').merge(synthetic_hh, how = 'left', left_index = True, right_index = True).fillna(0)    
    return output_lu


if __name__ == '__main__':

    # runtime args
    parser = argparse.ArgumentParser(prog='preprocessor')
    parser.add_argument(
        '-p', '--preprocess',
        action='store_true', help='Run preprocessor.')
    parser.add_argument(
        '-a', '--asim',
        action='store_true', help='Run activitysim.')
    parser.add_argument(
         '-c', '--configs',
         help = 'Config Directory')
    parser.add_argument(
         '-d', '--data',
         help = 'Input Directory')
    parser.add_argument(
         '-o', '--output',
         help = 'Output Directory')
    
    args = parser.parse_args()
    run_preprocessor = args.preprocess
    run_asim = args.asim
    config_dir = args.configs
    data_dir = args.data
    output_dir = args.output

    # load settings



    if run_preprocessor:
        print('RUNNING PREPROCESSOR!')
        with open(os.path.join(config_dir,'preprocessing.yaml')) as f:
            settings = yaml.load(f, Loader=yaml.FullLoader)
            # data_dir = settings['data_dir']
            # config_dir = settings['config_dir']
        # create input data
        tours = create_tours(settings)
        lu = create_landuse(settings)
        households = create_households(tours)  # 1 per tour
        persons = create_persons(settings, num_households=len(households))
        tours = assign_hh_p_to_tours(tours, persons)
        sched_probs = create_sched_probs(settings)
        # # store input files to disk
        tours.to_csv(os.path.join(
            data_dir, settings['tours_output_fname']))
        households.to_csv(os.path.join(
            data_dir, settings['households_output_fname']), index=False)
        persons.to_csv(os.path.join(
            data_dir, settings['persons_output_fname']), index=False)
        lu.to_csv(os.path.join(
            data_dir,settings['maz_output_fname']))
        sched_probs.to_csv(os.path.join(config_dir, settings['tour_scheduling_probs_output_fname']), index = False)
    if args.asim:
        print('RUNNING ACTIVITYSIM!')
        with open(os.path.join(config_dir,'settings.yaml')) as f:
            settings = yaml.load(f, Loader=yaml.FullLoader)

        process = subprocess.Popen(
            [sys.executable, '-u', 'src/asim/simulation.py' ,'-c', config_dir, '-c', 'src/asim/configs/common_airport', '-o', output_dir, '-d', data_dir, '-d', 'output/skims'],
            stdout=sys.stdout, stderr=subprocess.PIPE)
        _, stderr = process.communicate()
        if process.returncode != 0:
            raise subprocess.SubprocessError(stderr.decode())
        
        print('RECODING TRIP MODES')
        settings_file = os.path.join(config_dir, "settings.yaml")
        with open(settings_file) as f:
            settings = yaml.safe_load(f)
            f.close()
        prefix = settings["output_tables"]["prefix"]
        
        trips_file = os.path.join(output_dir, prefix + "trips.csv")
        trips = pd.read_csv(trips_file)
        del trips["trip_mode"]
        trips.rename(columns = {"trip_mode_assign": "trip_mode"}).to_csv(trips_file, index = False)