#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright RSG, 2019-2020.                                             ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/mode_choice_diagnostic.py                                      ///                  
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Diagnostic tool for the SANDAG activity-based travel model mode choice results.
# This script first generates synthetic population files for target markets. 
# Users may input target market parameters via the "syn_pop_attributes.yaml" file.
# Users must additionally input origin and destination MAZs (i.e. MGRAs) via the
# "origin_mgra.csv" and "destination_mgra.csv" files. 
# 
# Once all synthetic population files have been created, the script creates a copy of
# the "sandag_abm.properties" file and modifies specific property parameters so that
# it is compatible with a the mode choice diagnostic tool. The modified properties
# file is renamed as "sandag_abm_mcd.properties"
#
# Finally, the mode choice diagnostic tool is run via "runSandagAbm_MCDiagnostic.cmd"
# The mode choice diagnostic tool uses the synthetic population files as inputs and
# outputs a tour file with utilities and probabilities for each tour mode. 
#
# Files referenced:
#	input\mcd\destination_mgra.csv
#	input\mcd\origin_mgra.csv
#	input\mcd\syn_pop_attributes.yaml
#	output\mcd\mcd_households.csv
#	output\mcd\mcd_persons.csv
# 	output\mcd\mcd_output_households.csv
#	output\mcd\mcd_output_persons.csv
#	output\mcd\mcd_work_location.csv
#	output\mcd\mcd_tour_file.csv
#	conf\sandag_abm.properties
#	bin\runSandagAbm_MCDiagnostic.cmd

import inro.modeller as _m

import pandas as pd 
import collections, os
import shutil as _shutil
import yaml
import warnings
import traceback as _traceback
import tempfile as _tempfile
import subprocess as _subprocess

warnings.filterwarnings("ignore")

_join = os.path.join
_dir = os.path.dirname

class mode_choice_diagnostic(_m.Tool()):

	tool_run_msg = ""

	@_m.method(return_type=_m.UnicodeType)
	def tool_run_msg_status(self):
		return self.tool_run_msg

	def __init__(self):
		project_dir = _dir(_m.Modeller().desktop.project.path)
		self.main_directory = _dir(project_dir)
		self.properties_path = _join(_dir(project_dir), "conf")
		self.mcd_out_path = _join(_dir(project_dir), "output", "mcd")
		self.syn_pop_attributes_path = _join(_dir(project_dir), "input", "mcd", "syn_pop_attributes.yaml")
		self.origin_mgra_path = _join(_dir(project_dir), "input", "mcd", "origin_mgra.csv")
		self.destination_mgra_path = _join(_dir(project_dir), "input", "mcd", "destination_mgra.csv")
		self.household_df = pd.DataFrame()
		self.household_out_df = pd.DataFrame()
		self.person_df = pd.DataFrame()
		self.person_out_df = pd.DataFrame()
		self.work_location_df = pd.DataFrame()
		self.tour_df = pd.DataFrame()
		self.household_attributes = {}
		self.person_attributes = {}
		self.tour_attributes = {}
		self._log_level = "ENABLED"

	def page(self):
		pb = _m.ToolPageBuilder(self)
		pb.title = "Mode Choice Diagnostic Tool"
		pb.description = """
		Diagnostic tool for the activity-based travel model mode choice results.<br>
		<br>
		<div style="text-align:left">
		This tool first generates synthetic population files for specified target markets. 
		Users may edit target market attributes via a configuration file.
		Users may additionally select origin and destination MAZs (i.e. MGRAs) of interest via
		input CSV files.<br><br>
		The configuration file and MAZ selection CSV files are read from the following locations:<br>
			<ul>
				<li>input\mcd\syn_pop_attributes.yaml</li>
				<li>input\mcd\origin_mgra.csv</li>
				<li>input\mcd\destination_mgra.csv</li>
			</ul>
		The synthetic population generator outputs the following files:<br>
			<ul>
				<li>output\mcd\mcd_households.csv</li>
				<li>output\mcd\mcd_persons.csv</li>
				<li>output\mcd\mcd_output_households.csv</li>
				<li>output\mcd\mcd_output_persons.csv</li>
				<li>output\mcd\mcd_work_location.csv</li>
				<li>output\mcd\mcd_tour_file.csv</li>
			</ul>
		Once all synthetic population files have been created, the script creates a copy of
		the "sandag_abm.properties" file and modifies specific property parameters so that
		it is compatible with the mode choice diagnostic tool. The modified properties
		file is renamed and output as "conf\sandag_abm_mcd.properties"<br>
		<br>
		Finally, the mode choice diagnostic tool is run via <code>runSandagAbm_MCDiagnostic.cmd</code>
		The mode choice diagnostic tool uses the synthetic population files as inputs and
		outputs a tour file with utilities and probabilities for each tour mode. The tour file
		is output as "output\mcd\indivTourData_5.csv"
		</div>
		"""
		pb.branding_text = "SANDAG - Mode Choice Diagnostic Tool"

		if self.tool_run_msg != "":
			pb.tool_run_status(self.tool_run_msg_status)

		return pb.render()

	def run(self):
		self.tool_run_msg = ""
		try:
			self()
			run_msg = "Mode Choice Diagnostic Complete"
			self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
		except Exception as error:
			self.tool_run_msg = _m.PageBuilder.format_exception(
				error, _traceback.format_exc(error))
			raise

	def __call__(self):
		_m.logbook_write("Started running mode choice diagnostic...")

		# check if transit shapefiles are present in mcd input directory
		# if present, will move to mcd output directory
		_m.logbook_write("Checking for transit shapefiles...")
		self.check_shp()

		# run synthetic population generator
		_m.logbook_write("Creating synthetic population...")
		self.syn_pop()

		# copy and edit properties file
		_m.logbook_write("Copying and editing properties file...")
		mcd_props = self.copy_edit_props()

		self.set_global_logbook_level(mcd_props)

		drive, path_no_drive = os.path.splitdrive(self.main_directory)

		# run matrix manager
		_m.logbook_write("Running matrix manager...")
		self.run_proc("runMtxMgr.cmd", [drive, drive + path_no_drive], "Start matrix manager")

		# run driver
		_m.logbook_write("Running JPPF driver...")
		self.run_proc("runDriver.cmd", [drive, drive + path_no_drive], "Start JPPF driver")

		# run household manager
		_m.logbook_write("Running household manager, JPPF driver, and nodes...")
		self.run_proc("StartHHAndNodes.cmd", [drive, path_no_drive], "Start household manager, JPPF driver, and nodes")

		# run diagnostic tool
		_m.logbook_write("Running mode choice diagnostic tool...")
		path_forward_slash = path_no_drive.replace("\\", "/")
		self.run_proc(
			"runSandagAbm_MCDiagnostic.cmd",
			[drive, drive + path_forward_slash, 1.0, 5],
			"Java-Run Mode Choice Diagnostic Tool", capture_output=True)

		# move final output mcd files to the mcd output directory
		self.move_mcd_files()

	def syn_pop(self):
		# Creates sample synthetic population files for desired target market. Files will in turn
		# be used as inputs to the mode choice diagnostic tool

		load_properties = _m.Modeller().tool("sandag.utilities.properties")
		props = load_properties(self.properties_path)

		mgra_data_path = _join(self.main_directory, props["mgra.socec.file"])

		file_paths = [self.syn_pop_attributes_path, self.origin_mgra_path, self.destination_mgra_path, mgra_data_path]

		for path in file_paths:
			if not os.path.exists(path):
				raise Exception("missing file '%s'" % (path))

		# create output directory if it donesn't already exist
		if not os.path.exists(self.mcd_out_path):
			os.makedirs(self.mcd_out_path)

		# read inputs
		mgra_data = pd.read_csv(mgra_data_path)[['mgra', 'taz']]
		origin_mgra_data = list(set(pd.read_csv(self.origin_mgra_path)['MGRA']))
		destination_mgra_data = list(set(pd.read_csv(self.destination_mgra_path)['MGRA']))

		# read synthetic population attributes
		with open (self.syn_pop_attributes_path) as file:
			syn_pop_attributes = yaml.load(file, Loader = yaml.Loader)
			
		self.household_attributes = syn_pop_attributes["household"]
		self.person_attributes = syn_pop_attributes["person"]
		self.tour_attributes = syn_pop_attributes["tour"]

		# create households input file
		self.household_in(origin_mgra_data, mgra_data)

		# create households output file
		self.household_out()

		# create persons input file
		self.person_in()

		# create persons output file
		self.person_out()
		
		# create output work location file
		self.work_location(destination_mgra_data)

		# create individual tour file
		self.tour()

	def household_in(self, origin_mgra_data, mgra_data):
		# Creates the input household file

		# fixed household attributes
		household = collections.OrderedDict([
			('hworkers',   [1]),     # number of hh workers: one worker per household 
			('persons',    [2]),     # number of hh persons: two persons per household
			('version',    [0]),     # synthetic population version
		])

		household_df = pd.DataFrame.from_dict(household)
		household_df = self.replicate_df_for_variable(household_df, 'mgra', origin_mgra_data)
		for key, values in self.household_attributes.items():
			household_df = self.replicate_df_for_variable(household_df, key, self.maybe_list(values))
		household_df['hinccat1'] = household_df.apply(lambda hh_row: self.hinccat(hh_row), axis = 1)
		household_df = self.replicate_df_for_variable(household_df, 'poverty', [1])
		household_df = pd.merge(left = household_df, right = mgra_data, on = 'mgra')
		household_df = household_df.reset_index(drop = True)
		household_df['hhid'] = household_df.index + 1
		household_df['household_serial_no'] = 0

		# reorder columns
		household_df = household_df[['hhid', 'household_serial_no', 'taz', 'mgra', 'hinccat1', 'hinc', 'hworkers', 
									 'veh','persons', 'hht', 'bldgsz', 'unittype', 'version', 'poverty']]

		self.household_df = household_df

		# print 
		household_df.to_csv(_join(self.mcd_out_path, 'mcd_households.csv'), index = False)

	def household_out(self):
		# Creates the output household file

		household_out_df = self.household_df.copy()
		household_out_df = household_out_df[['hhid', 'mgra', 'hinc', 'veh']]
		household_out_df['transponder'] = 1
		household_out_df['cdap_pattern'] = 'MNj'
		household_out_df['out_escort_choice'] = 0
		household_out_df['inb_escort_choice'] = 0
		household_out_df['jtf_choice'] = 0
		if self.tour_attributes['av_avail']:
			household_out_df['AVs'] = household_out_df['veh']
			household_out_df['veh'] = 0
		else:
			household_out_df['AVs'] = 0

		# rename columns
		household_out_df.rename(columns = {'hhid':'hh_id', 'mgra':'home_mgra', 'hinc':'income', 'veh':'HVs'}, 
								inplace = True)

		self.household_out_df = household_out_df

		# print
		household_out_df.to_csv(_join(self.mcd_out_path, 'mcd_output_households.csv'), index = False)

	def person_in(self):
		# Creates the input person file

		# fixed person attributes
		persons = collections.OrderedDict([
			('pnum',                [1, 2]),                    # person number: two per household
			('pemploy',             [1, 3]),                    # employment status: full-time employee and unemployed
			('ptype',               [1, 4]),                    # person type: full-time worker and non-working adult
			('occen5',              [0, 0]),                    # occupation
			('occsoc5',             ['11-1021', '00-0000']),    # occupation code#
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

		persons.update(self.person_attributes)
		person_df = pd.DataFrame.from_dict(persons)
		person_df['join_key'] = 1
		self.household_df['join_key'] = 1
		person_df = pd.merge(left = person_df, right = self.household_df[['hhid','household_serial_no', 'join_key']]).\
			drop(columns = ['join_key'])
		person_df['pstudent'] = person_df.apply(lambda person_row: self.pstudent(person_row), axis = 1)
		person_df = person_df.sort_values(by = 'hhid')
		person_df = person_df.reset_index(drop = True)
		person_df['perid'] = person_df.index + 1

		# reorder columns
		person_df = person_df[['hhid', 'perid', 'household_serial_no', 'pnum', 'age', 'sex', 'miltary', 'pemploy',
							   'pstudent', 'ptype', 'educ', 'grade', 'occen5', 'occsoc5', 'indcen', 'weeks', 'hours',
							   'race1p', 'hisp', 'version', 'timeFactorWork', 'timeFactorNonWork', 'DAP']]

		self.person_df = person_df

		# print
		person_df.to_csv(_join(self.mcd_out_path, 'mcd_persons.csv'), index = False)

	def person_out(self):
		# Creates the output person file

		person_out_df = self.person_df.copy()
		person_out_df = person_out_df[['hhid', 'perid', 'pnum', 'age', 'sex', 'ptype', 'DAP',
									   'timeFactorWork', 'timeFactorNonWork']]
		person_out_df['gender'] = person_out_df['sex'].apply(lambda x: 'male' if x == 1 else 'female')
		person_out_df['type'] = person_out_df.apply(lambda person_row: self.p_type(person_row), axis = 1)
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

		self.person_out_df = person_out_df

		# print
		person_out_df.to_csv(_join(self.mcd_out_path, 'mcd_output_persons.csv'), index = False)

	def work_location(self, destination_mgra_data):
		# Creates the output work location file

		# create copies and subset household and person dataframes 
		household_subset_df = self.household_df.copy()
		person_subset_df = self.person_df.copy()
		household_subset_df = household_subset_df[['hhid', 'mgra', 'hinc']]
		person_subset_df = person_subset_df[['hhid', 'perid', 'pnum', 'ptype', 'age', 'pemploy', 'pstudent']]

		# merge to create work location dataframe
		work_location_df = pd.merge(left = household_subset_df, right = person_subset_df, on = 'hhid')
		work_location_df['WorkSegment'] = work_location_df['pnum'].apply(lambda x: 0 if x == 1 else -1)
		work_location_df['SchoolSegment'] = -1
		work_location_df = self.replicate_df_for_variable(work_location_df, 'WorkLocation', self.maybe_list(destination_mgra_data))
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

		self.work_location_df = work_location_df

		# print
		work_location_df.to_csv(_join(self.mcd_out_path, 'mcd_work_location.csv'), index = False)

	def tour(self):
		# Creates the individual tour file

		tour_df = self.work_location_df.copy()
		tour_df = tour_df[['HHID', 'personID', 'personNum', 'personType', 'homeMGRA', 'WorkLocation']]
		tour_df = tour_df.sort_values(by = list(tour_df.columns), ascending = True)
		tour_df['tour_id'] = tour_df.groupby(['HHID', 'personID']).cumcount()
		tour_df['tour_category'] = tour_df['personNum'].\
			apply(lambda x: 'INDIVIDUAL_MANDATORY' if x == 1 else 'INDIVIDUAL_NON_MANDATORY')
		tour_df['tour_purpose'] = tour_df['personNum'].apply(lambda x: 'Work' if x == 1 else 'Shop')
		tour_df['start_period'] = tour_df['personNum'].apply(lambda x: self.tour_attributes['start_period'][0] if x == 1 else self.tour_attributes['start_period'][1])
		tour_df['end_period'] = tour_df['personNum'].apply(lambda x: self.tour_attributes['end_period'][0] if x == 1 else self.tour_attributes['end_period'][1])
		tour_df['tour_mode'] = 0
		if self.tour_attributes['av_avail']:
			tour_df['av_avail'] = 1
		else:
			tour_df['av_avail'] = 0
		tour_df['tour_distance'] = 0
		tour_df['atwork_freq'] = tour_df['personNum'].apply(lambda x: 1 if x == 1 else 0)
		tour_df['num_ob_stops'] = 0
		tour_df['num_ib_stops'] = 0
		tour_df['valueOfTime'] = 0
		tour_df['escort_type_out'] = 0
		tour_df['escort_type_in'] = 0
		tour_df['driver_num_out'] = 0
		tour_df['driver_num_in'] = 0

		# utilities 1 through 13
		util_cols = []
		for x in range(1, 14, 1):
			col_name = 'util_{}'.format(x)
			tour_df[col_name] = 0
			util_cols.append(col_name)
			
		# probabilities 1 through 13
		prob_cols = []
		for x in range(1, 14, 1):
			col_name = 'prob_{}'.format(x)
			tour_df[col_name] = 0
			prob_cols.append(col_name)
			
		# rename columns
		tour_df.rename(columns = {'HHID':'hh_id', 'personID':'person_id', 'personNum':'person_num',
									   'personType':'person_type', 'homeMGRA':'orig_mgra', 'WorkLocation':'dest_mgra'},
							inplace = True)
		
		# reorder columns
		tour_df = tour_df[['hh_id', 'person_id', 'person_num', 'person_type', 'tour_id', 'tour_category', 
									 'tour_purpose', 'orig_mgra', 'dest_mgra', 'start_period', 'end_period',
									 'tour_mode', 'av_avail', 'tour_distance', 'atwork_freq', 'num_ob_stops',
									 'num_ib_stops', 'valueOfTime', 'escort_type_out', 'escort_type_in', 
									 'driver_num_out', 'driver_num_in'] + util_cols + prob_cols]

		self.tour_df = tour_df

		# print
		tour_df.to_csv(_join(self.mcd_out_path, 'mcd_tour_file.csv'), index = False)

	def replicate_df_for_variable(self, df, var_name, var_values):
		new_var_df = pd.DataFrame({var_name: var_values})
		new_var_df['join_key'] = 1
		df['join_key'] = 1
	
		ret_df = pd.merge(left = df, right = new_var_df, how = 'outer').drop(columns=['join_key'])
		return ret_df

	def maybe_list(self, values):
		if (type(values) is not list) and (type(values) is not int):
			raise Exception('Attribute values may only be of type list or int.')
		if type(values) is not list:
			return [values]
		else:
			return values

	def hinccat(self, hh_row):
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

	def pstudent(self, person_row):
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

	def p_type(self, person_row):
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

	def copy_edit_props(self):
		# Copy and edit properties file tokens to be compatible with the mode choice diagnostic tool

		load_properties = _m.Modeller().tool("sandag.utilities.properties")
		mcd_props = load_properties(_join(self.properties_path, "sandag_abm.properties"))

		# update properties

		# PopSyn inputs
		mcd_props["RunModel.MandatoryTourModeChoice"] = "true"
		mcd_props["RunModel.IndividualNonMandatoryTourModeChoice"] =  "true"

		# data file paths
		mcd_props["PopulationSynthesizer.InputToCTRAMP.HouseholdFile"] = "output/mcd/mcd_households.csv"
		mcd_props["PopulationSynthesizer.InputToCTRAMP.PersonFile"] = "output/mcd/mcd_persons.csv"
		mcd_props["Accessibilities.HouseholdDataFile"] = "output/mcd/mcd_output_households.csv"
		mcd_props["Accessibilities.PersonDataFile"] = "output/mcd/mcd_output_persons.csv"
		mcd_props["Accessibilities.IndivTourDataFile"] = "output/mcd/mcd_tour_file.csv"
		mcd_props["Accessibilities.JointTourDataFile"] = ""
		mcd_props["Accessibilities.IndivTripDataFile"] = ""
		mcd_props["Accessibilities.JointTripDataFile"] = ""

		# model component run flags
		mcd_props["RunModel.PreAutoOwnership"] = "false"
		mcd_props["RunModel.UsualWorkAndSchoolLocationChoice"] = "false"
		mcd_props["RunModel.AutoOwnership"] = "false"
		mcd_props["RunModel.TransponderChoice"] = "false"
		mcd_props["RunModel.FreeParking"] = "false"
		mcd_props["RunModel.CoordinatedDailyActivityPattern"] = "false"
		mcd_props["RunModel.IndividualMandatoryTourFrequency"] = "false"
		mcd_props["RunModel.MandatoryTourModeChoice"] = "true"
		mcd_props["RunModel.MandatoryTourDepartureTimeAndDuration"] = "false"
		mcd_props["RunModel.SchoolEscortModel"] = "false"
		mcd_props["RunModel.JointTourFrequency"] = "false"
		mcd_props["RunModel.JointTourLocationChoice"] = "false"
		mcd_props["RunModel.JointTourDepartureTimeAndDuration"] = "false"
		mcd_props["RunModel.JointTourModeChoice"] = "true"
		mcd_props["RunModel.IndividualNonMandatoryTourFrequency"] = "false"
		mcd_props["RunModel.IndividualNonMandatoryTourLocationChoice"] = "false"
		mcd_props["RunModel.IndividualNonMandatoryTourDepartureTimeAndDuration"] = "false"
		mcd_props["RunModel.IndividualNonMandatoryTourModeChoice"] = "true"
		mcd_props["RunModel.AtWorkSubTourFrequency"] = "false"
		mcd_props["RunModel.AtWorkSubTourLocationChoice"] = "false"
		mcd_props["RunModel.AtWorkSubTourDepartureTimeAndDuration"] = "false"
		mcd_props["RunModel.AtWorkSubTourModeChoice"] = "true"
		mcd_props["RunModel.StopFrequency"] = "false"
		mcd_props["RunModel.StopLocation"] = "false"

		mcd_props.save(_join(self.properties_path, "sandag_abm_mcd.properties"))

		return(mcd_props)

	def run_proc(self, name, arguments, log_message, capture_output=False):
		path = _join(self.main_directory, "bin", name)
		if not os.path.exists(path):
			raise Exception("No command / batch file '%s'" % path)
		command = path + " " + " ".join([str(x) for x in arguments])
		attrs = {"command": command, "name": name, "arguments": arguments}
		with _m.logbook_trace(log_message, attributes=attrs):
			if capture_output and self._log_level != "NO_EXTERNAL_REPORTS":
				report = _m.PageBuilder(title="Process run %s" % name)
				report.add_html('Command:<br><br><div class="preformat">%s</div><br>' % command)
				# temporary file to capture output error messages generated by Java
				err_file_ref, err_file_path = _tempfile.mkstemp(suffix='.log')
				err_file = os.fdopen(err_file_ref, "w")
				try:
					output = _subprocess.check_output(command, stderr=err_file, cwd=self.main_directory, shell=True)
					report.add_html('Output:<br><br><div class="preformat">%s</div>' % output)
				except _subprocess.CalledProcessError as error:
					report.add_html('Output:<br><br><div class="preformat">%s</div>' % error.output)
					raise
				finally:
					err_file.close()
					with open(err_file_path, 'r') as f:
						error_msg = f.read()
					os.remove(err_file_path)
					if error_msg:
						report.add_html('Error message(s):<br><br><div class="preformat">%s</div>' % error_msg)
					try:
						# No raise on writing report error
						# due to observed issue with runs generating reports which cause
						# errors when logged
						_m.logbook_write("Process run %s report" % name, report.render())
					except Exception as error:
						print _time.strftime("%Y-%M-%d %H:%m:%S")
						print "Error writing report '%s' to logbook" % name
						print error
						print _traceback.format_exc(error)
						if self._log_level == "DISABLE_ON_ERROR":
							_m.logbook_level(_m.LogbookLevel.NONE)
			else:
				_subprocess.check_call(command, cwd=self.main_directory, shell=True)

	def set_global_logbook_level(self, props):
		self._log_level = props.get("RunModel.LogbookLevel", "ENABLED")
		log_all = _m.LogbookLevel.ATTRIBUTE | _m.LogbookLevel.VALUE | _m.LogbookLevel.COOKIE | _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG
		log_states = {
			"ENABLED": log_all,
			"DISABLE_ON_ERROR": log_all,
			"NO_EXTERNAL_REPORTS": log_all,
			"NO_REPORTS": _m.LogbookLevel.ATTRIBUTE | _m.LogbookLevel.COOKIE | _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG,
			"TITLES_ONLY": _m.LogbookLevel.TRACE | _m.LogbookLevel.LOG,
			"DISABLED": _m.LogbookLevel.NONE,
		}
		_m.logbook_write("Setting logbook level to %s" % self._log_level)
		try:
			_m.logbook_level(log_states[self._log_level])
		except KeyError:
			raise Exception("properties.RunModel.LogLevel: value must be one of %s" % ",".join(log_states.keys()))

	def move_mcd_files(self):

		out_directory = _join(self.main_directory, "output")

		hh_data = "householdData_5.csv"
		ind_tour = "indivTourData_5.csv"
		ind_trip = "indivTripData_5.csv"
		joint_tour = "jointTourData_5.csv"
		joint_trip = "jointTripData_5.csv"
		per_data = "personData_5.csv"
		mgra_park = "mgraParkingCost.csv"

		files = [hh_data, ind_tour, ind_trip, joint_tour, joint_trip, per_data, mgra_park]

		for file in files:
			src = _join(out_directory, file)
			if not os.path.exists(src):
				raise Exception("missing output file '%s'" % (src))
			dst = _join(self.mcd_out_path, file)
			_shutil.move(src, dst)

	def check_shp(self):

	in_directory = _join(self.main_directory, "input", "mcd")
	out_directory = self.mcd_out_path

	tapcov = "tapcov.shp"
	rtcov = "rtcov.shp"

	files = [tapcov, rtcov]

	for file in files:
		src = _join(in_directory, file)
		dst = _join(out_directory, file)
		if not os.path.exists(src):
			raise Exception("missing shapefile '%s'" % (src))
		_shutil.move(src, dst)