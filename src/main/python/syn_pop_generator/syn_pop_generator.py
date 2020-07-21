import pandas as pd 
import collections, os
import yaml

# paths and names
os.chdir('<<SET_WORKING_DIRECTORY>>')

mgra_data_path = 'input/mgra13_based_input2016.csv'
origin_mgra_input_path = 'input/origin_mgra_input.csv'
destination_mgra_input_path = 'input/destination_mgra_input.csv'
syn_pop_attributes_path = r'input/syn_pop_attributes.yaml'

out_prefix = 'output/dummy_syn_pop'
csv_out = True

# fixed household attributes
household = collections.OrderedDict([
    ('hworkers',   [1]),     # number of hh workers: one worker per household 
    ('persons',    [2]),     # number of hh persons: two persons per household
    ('version',    [0]),     # synthetic population version
])

# fixed person attributes
persons = collections.OrderedDict([
    ('pnum',                [1, 2]),                    # person number: two per household
    ('pemploy',             [1, 3]),                    # employment status: full-time employee and unemployed
    ('ptype',               [1, 4]),                    # person type: full-time worker and non-working adult
    ('occen5',              [0, 0]),                    # occupation
    ('occsoc5',             ['11-1021', '00-0000']),    # occupation code
    ('indcen',              [0, 0]),                    # industry code
    ('weeks',               [1, 0]),                    # weeks worked
    ('hours',               [35, 0]),                   # hours worked
    ('race1p',              [9, 9]),                    # race
    ('hisp',                [1, 1]),                    # hispanic flag
    ('version',             [9, 9]),                    # synthetic population run version: 9 is new for disaggregate population
    ('timeFactorWork',      [1, 1]),                    # work travel time factor: 1 is the mean
    ('timeFactorNonWork',   [1, 1]),                    # non work travel time factor: 2 is the mean
    ('DAP',                 ['M', 'N'])                 # daily activity pattern: M (Mandatory), N (Non-Mandatory)
])

def replicate_df_for_variable(hh_df, var_name, var_values):
    new_var_df = pd.DataFrame({var_name: var_values})
    new_var_df['join_key'] = 1
    hh_df['join_key'] = 1
    
    ret_hh_df = pd.merge(left = hh_df, right = new_var_df, how = 'outer').drop(columns=['join_key'])
    return ret_hh_df

def maybe_list(values):
    if (type(values) is not list) and (type(values) is not int):
        raise Exception('Attribute values may only be of type list or int.')
    if type(values) is not list:
        return [values]
    else:
        return values
    
def hinccat(hh_row):
    if hh_row['hinc'] < 30000:
        return 1
    if hh_row['hinc'] >= 30000 and hh_row['hinc'] < 60000:
        return 2
    if hh_row['hinc'] >= 60000 and hh_row['hinc'] < 100000:
        return 3
    if hh_row['hinc'] >= 100000 and hh_row['hinc'] < 150000:
        return 4
    if hh_row['hinc'] >= 150000:
        return 5
    
def pstudent(person_row):
    if person_row['grade'] == 0:
        return 3
    if person_row['grade'] == 1:
        return 1
    if person_row['grade'] == 2:
        return 1
    if person_row['grade'] == 3:
        return 1
    if person_row['grade'] == 4:
        return 1
    if person_row['grade'] == 5:
        return 1
    if person_row['grade'] == 6:
        return 2
    if person_row['grade'] == 7:
        return 2
    
def p_type(person_row):
    if person_row['ptype'] == 1:
        return 'Full-time worker'
    if person_row['ptype'] == 2:
        return 'Part-time worker'
    if person_row['ptype'] == 3:
        return 'University student'
    if person_row['ptype'] == 4:
        return 'Non-worker'
    if person_row['ptype'] == 5:
        return 'Retired'
    if person_row['ptype'] == 6:
        return 'Student of driving age'
    if person_row['ptype'] == 7:
        return 'Student of non-driving age'
    if person_row['ptype'] == 8:
        return 'Child too young for school'

if __name__ == '__main__':

    # read inputs
    mgra_data = pd.read_csv(mgra_data_path)
    origin_mgra_data = pd.read_csv(origin_mgra_input_path)
    origin_mgra_data = set(origin_mgra_data['MGRA'])
    origin_mgra_data = list(origin_mgra_data)
    mgra_data = pd.read_csv(mgra_data_path)
    mgra_data = mgra_data[['mgra', 'taz']]
    destination_mgra_data = pd.read_csv(destination_mgra_input_path)
    destination_mgra_data = set(destination_mgra_data['MGRA'])
    destination_mgra_data = list(origin_mgra_data)
    
    # read in synthetic population attributes
    with open (syn_pop_attributes_path) as file:
        syn_pop_attributes = yaml.load(file, Loader = yaml.FullLoader)
        
    household_attributes = syn_pop_attributes['household']
    person_attributes = syn_pop_attributes['person']
    tour_attributes = syn_pop_attributes['tour']
        
    #############################################################################################################
    # input household file
    #############################################################################################################
    household_df = pd.DataFrame.from_dict(household)
    household_df = replicate_df_for_variable(household_df, 'mgra', origin_mgra_data)
    for key, values in household_attributes.items():
        household_df = replicate_df_for_variable(household_df, key, maybe_list(values))
    household_df['hinccat1'] = household_df.apply(lambda hh_row: hinccat(hh_row), axis = 1)
    household_df = replicate_df_for_variable(household_df, 'poverty', [1])
    household_df = pd.merge(left = household_df, right = mgra_data, on = 'mgra')
    household_df = household_df.reset_index(drop = True)
    household_df['hhid'] = household_df.index + 1
    household_df['household_serial_no'] = 0
    
    # reorder columns
    household_df = household_df[['hhid', 'household_serial_no', 'taz', 'mgra', 'hinccat1', 'hinc', 'hworkers', 
                                 'veh','persons', 'hht', 'bldgsz', 'unittype', 'version', 'poverty']]
    
    # output file
    if csv_out:
        out_file = '{}_households.csv'.format(out_prefix)
        household_df.to_csv(out_file, index = False)

    #############################################################################################################
    # output household file
    #############################################################################################################
    household_out_df = household_df.copy()
    household_out_df = household_out_df[['hhid', 'mgra', 'hinc', 'veh']]
    household_out_df['transponder'] = 1
    household_out_df['cdap_pattern'] = 'MNj'
    household_out_df['out_escort_choice'] = 0
    household_out_df['inb_escort_choice'] = 0
    household_out_df['jtf_choice'] = 0
    if tour_attributes['av_avail']:
        household_out_df['AVs'] = household_out_df['veh']
        household_out_df['veh'] = 0
    else:
        household_out_df['AVs'] = 0
    
    # rename columns
    household_out_df.rename(columns = {'hhid':'hh_id', 'mgra':'home_mgra', 'hinc':'income', 'veh':'HVs'}, 
                            inplace = True)
    
    # output file
    if csv_out:
        out_file = '{}_output_households.csv'.format(out_prefix)
        household_out_df.to_csv(out_file, index = False)
    
    #############################################################################################################
    # input person file
    #############################################################################################################
    persons.update(person_attributes)
    person_df = pd.DataFrame.from_dict(persons)
    person_df['join_key'] = 1
    household_df['join_key'] = 1
    person_df = pd.merge(left = person_df, right = household_df[['hhid','household_serial_no', 'join_key']]).\
        drop(columns = ['join_key'])
    person_df['pstudent'] = person_df.apply(lambda person_row: pstudent(person_row), axis = 1)
    person_df = person_df.sort_values(by = 'hhid')
    person_df = person_df.reset_index(drop = True)
    person_df['perid'] = person_df.index + 1
    
    # reorder columns
    person_df = person_df[['hhid', 'perid', 'household_serial_no', 'pnum', 'age', 'sex', 'miltary', 'pemploy',
                           'pstudent', 'ptype', 'educ', 'grade', 'occen5', 'occsoc5', 'indcen', 'weeks', 'hours',
                           'race1p', 'hisp', 'version', 'timeFactorWork', 'timeFactorNonWork', 'DAP']]
    
    # output file
    if csv_out:
        out_file = '{}_persons.csv'.format(out_prefix)
        person_df.to_csv(out_file, index = False)
    
    #############################################################################################################
    # output person file
    #############################################################################################################
    person_out_df = person_df.copy()
    person_out_df = person_out_df[['hhid', 'perid', 'pnum', 'age', 'sex', 'ptype', 'DAP',
                                   'timeFactorWork', 'timeFactorNonWork']]
    person_out_df['gender'] = person_out_df['sex'].apply(lambda x: 'male' if x == 1 else 'female')
    person_out_df['type'] = person_out_df.apply(lambda person_row: p_type(person_row), axis = 1)
    person_out_df['value_of_time'] = 0
    person_out_df['imf_choice'] = person_out_df['pnum'].apply(lambda x: 1 if x == 1 else 0)
    person_out_df['inmf_choice'] = person_out_df['pnum'].apply(lambda x: 0 if x == 1 else 36)
    person_out_df['fp_choice'] = person_out_df['pnum'].apply(lambda x: 2 if x == 1 else -1)
    person_out_df['reimb_pct'] = 0
    person_out_df['tele_choice'] = person_out_df['pnum'].apply(lambda x: 1 if x == 1 else -1)
    person_out_df['ie_choice'] = 1
    
    # drop columns not required
    person_out_df.drop(columns = ['sex', 'ptype'], inplace = True)
    
    # rename columns
    person_out_df.rename(columns = {'hhid':'hh_id', 'perid':'person_id', 
                                    'pnum':'person_num', 'DAP':'activity_pattern'},
                         inplace = True)
    
    # reorder columns
    person_out_df = person_out_df[['hh_id', 'person_id', 'person_num', 'age', 'gender', 'type', 'value_of_time',
                                   'activity_pattern', 'imf_choice', 'inmf_choice', 'fp_choice', 'reimb_pct',
                                   'tele_choice', 'ie_choice', 'timeFactorWork', 'timeFactorNonWork']]
    
    # output file
    if csv_out:    
        out_file = '{}_output_persons.csv'.format(out_prefix)
        person_out_df.to_csv(out_file, index = False)
    
    #############################################################################################################
    # output work location file
    #############################################################################################################
    household_subset_df = household_df.copy()
    household_subset_df = household_subset_df[['hhid', 'mgra', 'hinc']]
    person_subset_df = person_df.copy()
    person_subset_df = person_subset_df[['hhid', 'perid', 'pnum', 'ptype', 'age', 'pemploy', 'pstudent']]    
    work_location_df = pd.merge(left = household_subset_df, right = person_subset_df, on = 'hhid')
    work_location_df['WorkSegment'] = work_location_df['pnum'].apply(lambda x: 0 if x == 1 else -1)
    work_location_df['SchoolSegment'] = -1
    work_location_df = replicate_df_for_variable(work_location_df, 'WorkLocation', maybe_list(destination_mgra_data))
    work_location_df['WorkLocationDistance'] = 0
    work_location_df['WorkLocationLogsum'] = 0
    work_location_df['SchoolLocation'] = -1
    work_location_df['SchoolLocationDistance'] = 0
    work_location_df['SchoolLocationLogsum'] = 0    
    
    # rename columns
    work_location_df.rename(columns = {'hhid':'HHID', 'mgra':'homeMGRA', 'hinc':'income', 'perid':'personID',
                                       'pnum':'personNum', 'ptype':'personType', 'age':'personAge', 
                                       'pemploy':'Employment Category', 'pstudent':'StudentCategory'},
                            inplace = True)
    
    # reorder columns
    work_location_df = work_location_df[['HHID', 'homeMGRA', 'income', 'personID', 'personNum', 'personType',
                                         'personAge', 'Employment Category', 'StudentCategory', 'WorkSegment',
                                         'SchoolSegment', 'WorkLocation', 'WorkLocationDistance', 'WorkLocationLogsum',
                                         'SchoolLocation', 'SchoolLocationDistance', 'SchoolLocationLogsum']]
    
    # output file
    if csv_out:
        out_file = '{}_work_location.csv'.format(out_prefix)
        work_location_df.to_csv(out_file, index = False)
    
    #############################################################################################################
    # output individual tour file
    #############################################################################################################
    tour_file_df = work_location_df.copy()
    tour_file_df = tour_file_df[['HHID', 'personID', 'personNum', 'personType', 'homeMGRA', 'WorkLocation']]
    tour_file_df = tour_file_df.sort_values(by = list(tour_file_df.columns), ascending = True)
    tour_file_df['tour_id'] = tour_file_df.groupby(['HHID', 'personID']).cumcount()
    tour_file_df['tour_category'] = tour_file_df['personNum'].\
        apply(lambda x: 'INDIVIDUAL_MANDATORY' if x == 1 else 'INDIVIDUAL_NON_MANDATORY')
    tour_file_df['tour_purpose'] = tour_file_df['personNum'].apply(lambda x: 'Work' if x == 1 else 'Shop')
    tour_file_df['start_period'] = tour_file_df['personNum'].apply(lambda x: tour_attributes['start_period'][0] if x == 1 else tour_attributes['start_period'][1])
    tour_file_df['end_period'] = tour_file_df['personNum'].apply(lambda x: tour_attributes['end_period'][0] if x == 1 else tour_attributes['end_period'][1])
    tour_file_df['tour_mode'] = 0
    if tour_attributes['av_avail']:
        tour_file_df['av_avail'] = 1
    else:
        tour_file_df['av_avail'] = 0
    tour_file_df['tour_distance'] = 0
    tour_file_df['atwork_freq'] = tour_file_df['personNum'].apply(lambda x: 1 if x == 1 else 0)
    tour_file_df['num_ob_stops'] = 0
    tour_file_df['num_ib_stops'] = 0
    tour_file_df['valueOfTime'] = 0
    tour_file_df['escort_type_out'] = 0
    tour_file_df['escort_type_in'] = 0
    tour_file_df['driver_num_out'] = 0
    tour_file_df['driver_num_in'] = 0
    
    # utilities 1 through 13
    util_cols = []
    for x in range(1, 14, 1):
        col_name = 'util_{}'.format(x)
        tour_file_df[col_name] = 0
        util_cols.append(col_name)
        
    # probabilities 1 through 13
    prob_cols = []
    for x in range(1, 14, 1):
        col_name = 'prob_{}'.format(x)
        tour_file_df[col_name] = 0
        prob_cols.append(col_name)
        
    # rename columns
    tour_file_df.rename(columns = {'HHID':'hh_id', 'personID':'person_id', 'personNum':'person_num',
                                   'personType':'person_type', 'homeMGRA':'orig_mgra', 'WorkLocation':'dest_mgra'},
                        inplace = True)
    
    # reorder columns
    tour_file_df = tour_file_df[['hh_id', 'person_id', 'person_num', 'person_type', 'tour_id', 'tour_category', 
                                 'tour_purpose', 'orig_mgra', 'dest_mgra', 'start_period', 'end_period',
                                 'tour_mode', 'av_avail', 'tour_distance', 'atwork_freq', 'num_ob_stops',
                                 'num_ib_stops', 'valueOfTime', 'escort_type_out', 'escort_type_in', 
                                 'driver_num_out', 'driver_num_in'] + util_cols + prob_cols]
    
    # output file
    if csv_out:
        out_file = '{}_tour_file.csv'.format(out_prefix)
        tour_file_df.to_csv(out_file, index = False)
    