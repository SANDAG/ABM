#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright RSG, 2019-2020.                                             ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/input_checker.py                                               ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Reviews all inputs to SANDAG ABM for possible issues that will result in model errors
# 
#
# Files referenced:
#	input_checker\config\inputs_checks.csv
# 	input_checker\config\inputs_list.csv	
#
# Script example:
# python C:\ABM_runs\maint_2020_RSG\Tasks\input_checker\emme_toolbox\emme\toolbox\import\input_checker.py


import os, shutil, sys, time, csv, logging
import win32com.client as com
import numpy as np
import pandas as pd
import traceback as _traceback
import datetime
import warnings
from simpledbf import Dbf5
import inro.modeller as _m

warnings.filterwarnings("ignore")

_join = os.path.join
_dir = os.path.dirname

#gen_utils = _m.Modeller().module("sandag.utilities.general")

class input_checker(_m.Tool()):

	path = _m.Attribute(unicode)

	tool_run_msg = ""

	@_m.method(return_type=_m.UnicodeType)
	def tool_run_msg_status(self):
		return self.tool_run_msg

	def __init__(self):
		project_dir = _dir(_m.Modeller().desktop.project.path)
		self.path = _dir(project_dir)
		self.input_checker_path = ''
		self.inputs_list_path = ''
		self.inputs_checks_path = ''
		self.prop_input_paths = {}
		self.inputs_list = pd.DataFrame()
		self.inputs_checks = pd.DataFrame()
		self.inputs = {}
		self.results = {}
		self.result_list = {}
		self.problem_ids = {}
		self.report_stat = {}
		self.num_fatal = int()
		self.num_warning = int()
		self.num_logical = int()

	def page(self):
		pb = _m.ToolPageBuilder(self)
		pb.title = "Input Checker"
		pb.description = """
		Reviews all inputs to SANDAG ABM for possible issues that could result
		in model errors. List of inputs and checks are read from two CSV files:
		<br>
		<div style="text-align:left">
			<ul>
	            <li>input_checker\config\inputs_checks.csv</li>
	            <li>input_checker\config\inputs_list.csv</li>
	        </ul>
	    </div>
		The input checker goes through the list of checks and evaluates each
		one as True or False. A log file is produced at the end with results
		for each check and additionally, a summary of all checks:
		<br>
		<div style="text-align:left">
			<ul>
	            <li>input_checker\logs\inputCheckerLog [YEAR-M-D].LOG</li>
	            <li>input_checker\logs\inputCheckerSummary.txt</li>
	        </ul>
	    </div>
		"""
		pb.branding_text = "SANDAG - Input Checker"

		if self.tool_run_msg != "":
			pb.tool_run_status(self.tool_run_msg_status)

		return pb.render()

	def run(self):
		self.tool_run_msg = ""
		try:
			self()
			run_msg = "Input Checker Complete"
			self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
		except Exception as error:
			self.tool_run_msg = _m.PageBuilder.format_exception(
				error, _traceback.format_exc(error))
			raise

	def __call__(self):
		_m.logbook_write("Started running input checker...")

		self.input_checker_path = _join(self.path, 'input_checker')
		self.inputs_list_path = _join(self.input_checker_path, 'config', 'inputs_list.csv')
		self.inputs_checks_path = _join(self.input_checker_path, 'config', 'inputs_checks.csv')

		#attributes = {"path": self.path}
		#gen_utils.log_snapshot("Run Input Checker", str(self), attributes)

		file_paths = [self.inputs_list_path, self.inputs_checks_path]
		for path in file_paths:
			if not os.path.exists(path):
				raise Exception("missing file '%s'" % (path))

		_m.logbook_write("Reading inputs...")
		self.read_inputs()

		_m.logbook_write("Conducting checks...")
		self.checks()

		_m.logbook_write("Writing logs...")
		self.write_log()

		_m.logbook_write("Checking for fatal errors...")
		self.check_num_fatal()

		_m.logbook_write("Finisehd running input checker")

	def read_inputs(self):
		# read list of inputs from CSV file
		self.inputs_list = pd.read_csv(self.inputs_list_path)

		# remove all commented inputs from the inputs list
		self.inputs_list = self.inputs_list.loc[[not i for i in (self.inputs_list['Input_Table'].str.startswith('#'))]]

		# obtain file paths from the sandag_abm.properties
		self.prop_file_paths()

		# load emme network
		network = _m.Modeller().emmebank.scenario(100).get_network()

		def get_emme_object(emme_network, emme_network_object, fields_to_export):
			# Emme network attribute and object names
			net_attr = {
			'NODE':'nodes', 
			'LINK':'links', 
			'TRANSIT_SEGMENT':'transit_segments', 
			'TRANSIT_LINE':'transit_lines'
			}

			# read-in entire emme network object as a list
			get_objs = 'list(emme_network.' + net_attr[emme_network_object] + '())'
			uda = eval(get_objs)

			# get list of network object attributes
			obj_attr = []
			if fields_to_export[0] in ['all','All','ALL']:
				obj_attr = emme_network.attributes(emme_network_object)
			else:
				obj_attr = fields_to_export
				
			# instantiate list of network objects
			net_objs = []
			for i in range(len(uda)):
				obj_fields = []
				get_id = 'uda[i].id'
				obj_fields.append(eval(get_id))
				for attr in obj_attr:
					get_field = 'uda[i]["' + attr + '"]'
					obj_fields.append(eval(get_field))
				net_objs.append(obj_fields)
			net_obj_df = pd.DataFrame(net_objs, columns = ['id'] + obj_attr)

			return(net_obj_df)

		for item, row in self.inputs_list.iterrows():

			print('Adding Input: ' + row['Input_Table'])

			table_name = row['Input_Table']
			emme_network_object = row['Emme_Object']
			column_map = row['Column_Map']
			fields_to_export = row['Fields'].split(',')

			# obtain emme network object, csv or dbf input
			if not (pd.isnull(emme_network_object)):
				df = get_emme_object(network, emme_network_object, fields_to_export)
				self.inputs[table_name] = df
				print(' - ' + table_name + ' added')
			else:
				input_path = self.prop_input_paths[table_name]
				input_ext = os.path.splitext(input_path)[1]
				if input_ext == '.csv':
					df = pd.read_csv(_join(self.path, input_path))
					self.inputs[table_name] = df
					print(' - ' + table_name + ' added')
				else:
					dbf = Dbf5(_join(_dir(self.path), input_path))
					df = dbf.to_dataframe()
					self.inputs[table_name] = df
					print(' - ' + table_name + ' added')

	def checks(self):
		# read all input DFs into memory
		for key, df in self.inputs.items():
			expr = key + ' = df'
			exec(expr)

		# copy of locals(), a dictionary of all local variables
		calc_dict = locals()

		# read list of checks from CSV file
		self.inputs_checks = pd.read_csv(self.inputs_checks_path)

		# remove all commented checks from the checks list
		self.inputs_checks = self.inputs_checks.loc[[not i for i in (self.inputs_checks['Test'].str.startswith('#'))]]

		# loop through list of checks and conduct all checks
		# checks must evaluate to True if inputs are correct
		for item, row in self.inputs_checks.iterrows():

			test = row['Test']
			table = row['Input_Table']
			id_col = row['Input_ID_Column']
			expr = row['Expression']
			test_vals = row['Test_Vals']
			if not (pd.isnull(row['Test_Vals'])):
				test_vals = test_vals.split(',')
				test_vals = [txt.strip() for txt in test_vals]
			test_type = row['Type']
			Severity = row['Severity']
			stat_expr = row['Report_Statistic']

			if test_type == 'Test':

				print ('Performing Check: ' +  row['Test'])

				if (pd.isnull(row['Test_Vals'])):

					# perform test
					out = eval(expr, calc_dict)

					# check if test result is a series
					if str(type(out)) == "<class 'pandas.core.series.Series'>":
						# for series, the test must be evaluated across all items
						# result is False if a single False is found
						self.results[test] = not (False in out.values)
						
						# reverse results list since we need all False IDs
						reverse_results = [not i for i in out.values]
						error_expr = table + '.' + id_col + '[reverse_results]'
						error_id_list = eval(error_expr)

						# report first 25 problem IDs in the log
						if error_id_list.size > 25:
							self.problem_ids[test] = error_id_list.iloc[range(25)]
						else:
							self.problem_ids[test] = error_id_list if error_id_list.size > 0 else []

						# compute report statistics
						if (pd.isnull(stat_expr)):
							self.report_stat[test] = ''
						else:
							stat_list = eval(stat_expr)
							self.report_stat[test] = stat_list[reverse_results]
					else:
						self.results[test] = out
						self.problem_ids[test] = []
						if (pd.isnull(stat_expr)):
							self.report_stat[test] = ''
						else:
							self.report_stat[test] = eval(stat_expr)
				else:
					# loop through test_vals and perform test for each item
					self.result_list[test] = []
					for test_val in test_vals:
						# perform test (test result must not be of type Series)
						out = eval(expr)

						# compute report statistic
						if (pd.isnull(stat_expr)):
							self.report_stat[test] = ''
						else:
							self.report_stat[test] = eval(stat_expr)

						# append to list
						self.result_list[test].append(out)
					self.results[test] = not (False in self.result_list[test])
					self.problem_ids[test] = []

				print (' - Check Complete')

			else:
				# perform calculation
				print ('Performing Calculation: ' + row['Test'])
				calc_expr = test + ' = ' + expr
				exec(calc_expr, {}, calc_dict)
				print (' - Calculation Complete')

	def prop_file_paths(self):
		prop_files = self.inputs_list[['Input_Table','Property_Token']].dropna()

		load_properties = _m.Modeller().tool('sandag.utilities.properties')
		props = load_properties(_join(self.path, 'conf', 'sandag_abm.properties'))

		for item, row in prop_files.iterrows():
			input_table = row['Input_Table']
			input_path = props[row['Property_Token']]
			self.prop_input_paths[input_table] = input_path

	def write_log(self):
		# function to write out the input checker log file
		# there are three blocks:
		#   - Introduction
		#   - Action Required: FATAL, LOGICAL, WARNINGS
		#   - List of passed checks
		
		# create log file
		now = datetime.datetime.now()

		# create log directory if it doesn't already exist
		log_path = _join(self.input_checker_path,'logs')
		if not os.path.exists(log_path):
			os.makedirs(log_path)

		f = open(_join(self.input_checker_path,'logs', ('inputCheckerLog ' + now.strftime("[%Y-%m-%d]") + '.LOG')), 'wb')
		
		# define re-usable elements
		seperator1 = '###########################################################'
		seperator2 = '***********************************************************'
		
		# write out Header
		f.write(seperator1 + seperator1 + "\r\n")
		f.write(seperator1 + seperator1 + "\r\n\r\n")
		f.write("\t SANDAG ABM Input Checker Log File \r\n")
		f.write("\t ____________________________ \r\n\r\n\r\n")
		f.write("\t Log created on: " + now.strftime("%Y-%m-%d %H:%M") + "\r\n\r\n")
		f.write("\t Notes:-\r\n")
		f.write("\t The SANDAG ABM Input Checker performs various QA/QC checks on SANDAG ABM inputs as specified by the user.\r\n")
		f.write("\t The Input Checker allows the user to specify three severity levels for each QA/QC check:\r\n\r\n")
		f.write("\t 1) FATAL  2) LOGICAL  3) WARNING\r\n\r\n")
		f.write("\t FATAL Checks:   The failure of these checks would result in a FATAL errors in the SANDAG ABM run.\r\n")
		f.write("\t                 In case of FATAL failure, the Input Checker returns a return code of 1 to the\r\n")
		f.write("\t                 main SANDAG ABM model, cauing the model run to halt.\r\n")
		f.write("\t LOGICAL Checks: The failure of these checks indicate logical inconsistencies in the inputs.\r\n")
		f.write("\t                 With logical errors in inputs, the SANDAG ABM outputs may not be meaningful.\r\n")
		f.write("\t WARNING Checks: The failure of Warning checks would indicate problems in data that would not.\r\n")
		f.write("\t                 halt the run or affect model outputs but might indicate an issue with inputs.\r\n\r\n\r\n")
		f.write("\t The results of all the checks are organized as follows: \r\n\r\n")
		f.write("\t IMMEDIATE ACTION REQUIRED:\r\n")
		f.write("\t -------------------------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a FATAL check\r\n\r\n")
		f.write("\t ACTION REQUIRED:\r\n")
		f.write("\t ---------------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a LOGICAL check\r\n\r\n")
		f.write("\t WARNINGS:\r\n")
		f.write("\t ---------------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a WARNING check\r\n\r\n")
		f.write("\t LOG OF ALL PASSED CHECKS:\r\n")
		f.write("\t -----------\r\n")
		f.write("\t A complete listing of results of all passed checks\r\n\r\n")
		f.write(seperator1 + seperator1 + "\r\n")
		f.write(seperator1 + seperator1 + "\r\n\r\n\r\n\r\n")
		
		# combine results, inputs_checks and inputs_list
		self.inputs_checks['result'] = self.inputs_checks['Test'].map(self.results)
		checks_df = pd.merge(self.inputs_checks, self.inputs_list, on='Input_Table')
		checks_df = checks_df[checks_df.Type=='Test']
		checks_df['reverse_result'] = [not i for i in checks_df.result]
		
		# get all FATAL failures
		self.num_fatal = checks_df.result[(checks_df.Severity=='Fatal') & (checks_df.reverse_result)].count()
		
		# get all LOGICAL failures
		self.num_logical = checks_df.result[(checks_df.Severity=='Logical') & (checks_df.reverse_result)].count()
		
		# get all WARNING failures
		self.num_warning = checks_df.result[(checks_df.Severity=='Warning') & (checks_df.reverse_result)].count()

		def write_check_log(self, fh, row):
			# define constants
			seperator2 = '-----------------------------------------------------------'

			# integerize problem ID list
			problem_ids = self.problem_ids[row['Test']]
			problem_ids = [int(x) for x in problem_ids]

			# write check summary
			fh.write('\r\n\r\n' + seperator2 + seperator2)
			fh.write("\r\n\t Input File Name: " + ('NA' if not pd.isnull(row['Emme_Object']) else 
				(self.prop_input_paths[row['Input_Table']].rsplit('/', 1)[-1])))
			fh.write("\r\n\t Input File Location: " + ('NA' if not pd.isnull(row['Emme_Object']) else 
				(_join(self.input_checker_path, self.prop_input_paths[row['Input_Table']].replace('/','\\')))))
			fh.write("\r\n\t Emme Object: " + (row['Emme_Object'] if not pd.isnull(row['Emme_Object']) else 'NA'))
			fh.write("\r\n\t Input Description: " + (row['Input_Description'] if not pd.isnull(row['Input_Description']) else ""))
			fh.write("\r\n\t Test Name: " + row['Test'])
			fh.write("\r\n\t Test_Description: " + (row['Test_Description'] if not pd.isnull(row['Test_Description']) else ""))
			fh.write("\r\n\t Test Severity: " + row['Severity'])
			fh.write("\r\n\r\n\t TEST RESULT: " + ('PASSED' if row['result'] else 'FAILED'))

			# display problem IDs for failed column checks
			if (not row['result']) & (len(problem_ids)>0) :
				fh.write("\r\n\t TEST failed for following values of ID Column: " + row['Input_ID_Column'] + " (only upto 25 IDs displayed)")
				fh.write("\r\n\t " + row['Input_ID_Column'] + ": " + ','.join(map(str, problem_ids[0:25])))
				if not (pd.isnull(row['Report_Statistic'])):
					this_report_stat = self.report_stat[row['Test']]
					fh.write("\r\n\t Test Statistics: " + ','.join(map(str, this_report_stat[0:25])))
				fh.write("\r\n\t Total number of failures: " + str(len(problem_ids)))
			else:
				if not (pd.isnull(row['Report_Statistic'])):
					fh.write("\r\n\t Test Statistic: " + str(self.report_stat[row['Test']]))

			# display result for each test val if it was specified
			if not (pd.isnull(row['Test_Vals'])):
				fh.write("\r\n\t TEST results for each test val")
				result_tuples = zip(row['Test_Vals'].split(","), self.result_list[row['Test']])
				fh.write("\r\n\t ")
				fh.write(','.join('[{} - {}]'.format(x[0],x[1]) for x in result_tuples))
				
			fh.write("\r\n" + seperator2 + seperator2 + "\r\n\r\n")
		
		# write out IMMEDIATE ACTION REQUIRED section if needed
		if self.num_fatal > 0:
			fatal_checks = checks_df[(checks_df.Severity=='Fatal') & (checks_df.reverse_result)]
			f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n\r\n")
			f.write('\t' + "IMMEDIATE ACTION REQUIRED \r\n")
			f.write('\t' + "------------------------- \r\n\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			
			# write out log for each check
			for item, row in fatal_checks.iterrows():
				#self.write_check_log(f, row, self.problem_ids[row['Test']])
				#write_check_log(self, f, row, self.problem_ids[row['Test']])
				write_check_log(self, f, row)
		
		# write out ACTION REQUIRED section if needed
		if self.num_logical > 0:
			logical_checks = checks_df[(checks_df.Severity=='Logical') & (checks_df.reverse_result)]
			f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n\r\n")
			f.write('\t' + "ACTION REQUIRED \r\n")
			f.write('\t' + "--------------- \r\n\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			
			#write out log for each check
			for item, row in logical_checks.iterrows():
				write_check_log(self, f, row)
		
		# write out WARNINGS section if needed
		if self.num_warning > 0:
			warning_checks = checks_df[(checks_df.Severity=='Warning') & (checks_df.reverse_result)]
			f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n\r\n")
			f.write('\t' + "WARNINGS \r\n")
			f.write('\t' + "-------- \r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			
			# write out log for each check
			for item, row in warning_checks.iterrows():
				write_check_log(self, f, row)
				
		# write out the complete listing of all checks that passed
		passed_checks = checks_df[(checks_df.result)]
		f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
		f.write(seperator2 + seperator2 + "\r\n\r\n")
		f.write('\t' + "LOG OF ALL PASSED CHECKS \r\n")
		f.write('\t' + "------------------------ \r\n")
		f.write(seperator2 + seperator2 + "\r\n")
		f.write(seperator2 + seperator2 + "\r\n")
		
		# write out log for each check
		for item, row in passed_checks.iterrows():
			write_check_log(self, f, row)
			
		f.close()
		# write out a summary of results from input checker for main model
		f = open(_join(self.input_checker_path,'logs', ('inputCheckerSummary' + '.txt')), 'wb')
		f.write('\r\n' + seperator2 + '\r\n')
		f.write('\t Summary of Input Checker Fails \r\n')
		f.write(seperator2 + '\r\n\r\n')
		f.write(' Number of Fatal Errors: ' + str(self.num_fatal))
		f.write('\r\n\r\n Number of Logical Errors: ' + str(self.num_logical))
		f.write('\r\n\r\n Number of Warnings: ' + str(self.num_warning) + '\r\n\r\n')
		f.close()

	def check_num_fatal(self):
		# return code to the main model based on input checks and results
		if self.num_fatal > 0:
			_m.logbook_write('At least one fatal error in the inputs.')
			_m.logbook_write('Input Checker Failed')
			sys.exit(2)