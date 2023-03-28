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
# Files referenced:
#	input_checker\config\inputs_checks.csv
# 	input_checker\config\inputs_list.csv	

import os, shutil, sys, time, csv, logging
import win32com.client as com
import numpy as np
import pandas as pd
import traceback as _traceback
import datetime
import warnings
from simpledbf import Dbf5
import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import inro.director.util.qtdialog as dialog
import textwrap

warnings.filterwarnings("ignore")

_join = os.path.join
_dir = os.path.dirname

class input_checker(_m.Tool()):

	path = _m.Attribute(str)

	tool_run_msg = ""

	@_m.method(return_type=str)
	def tool_run_msg_status(self):
		return self.tool_run_msg

	def __init__(self):
		project_dir = _dir(_m.Modeller().desktop.project.path)
		self.path = _dir(project_dir)
		self.input_checker_path = ''
		self.inputs_list_path = ''
		self.inputs_checks_path = ''
		self.log_path = ''
		self.logical_log_path = ''
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
		self.logical_fails = pd.DataFrame()
		self.scenario_df = pd.DataFrame()

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
		one as True or False. A summary file is produced at the end with results
		for each check. The input checker additionally outputs a report for
		failed checks of severity type Logical with more than 25 failed records.
		The additional summary report lists every failed record.
		The following reports are output:
		<br>
		<div style="text-align:left">
			<ul>
				<li>input_checker\inputCheckerSummary_[YEAR-MM-DD].txt</li>
				<li>completeLogicalFails_[YEAR-MM-DD].txt</li>
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
			self(path = self.path)
			run_msg = "Input Checker Complete"
			self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
		except Exception as error:
			self.tool_run_msg = _m.PageBuilder.format_exception(
				error, _traceback.format_exc(error))
			raise

	def __call__(self, path = ""):
		_m.logbook_write("Started running input checker...")

		self.path = path

		self.input_checker_path = _join(self.path, 'input_checker')
		self.inputs_list_path = _join(self.input_checker_path, 'config', 'inputs_list.csv')
		self.inputs_checks_path = _join(self.input_checker_path, 'config', 'inputs_checks.csv')

		file_paths = [self.inputs_list_path, self.inputs_checks_path]
		for path in file_paths:
			if not os.path.exists(path):
				raise Exception("missing file '%s'" % (path))

		_m.logbook_write("Reading inputs...")
		self.read_inputs()

		_m.logbook_write("Conducting checks...")
		self.checks()

		_m.logbook_write("Writing logical fail logs...")
		self.write_logical_log()

		_m.logbook_write("Writing logs...")
		self.write_log()

		_m.logbook_write("Checking for logical errors...")
		self.check_logical()

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

		# load emmebank
		eb_path = _join(self.path, "emme_project", "Database", "emmebank")
		eb = _eb.Emmebank(eb_path)

		# load emme network
		network = eb.scenario(100).get_network()

		# create extra network attributes (maybe temporary)

		# link modes_str attribute
		network.create_attribute("LINK", "mode_str")
		for link in network.links():
			link.mode_str = "".join([m.id for m in link.modes])

		# link isTransit flag attribute
		network.create_attribute("LINK", "isTransit")
		transit_modes = set([m for m in network.modes() if m.type == "TRANSIT"])
		for link in network.links():
			link.isTransit = bool(link.modes.intersection(transit_modes))

		# transit segment isFirst and isLast flags attributes
		network.create_attribute("TRANSIT_SEGMENT", "isFirst", False)
		network.create_attribute("TRANSIT_SEGMENT", "isLast", False)
		for line in network.transit_lines():
			first_seg = line.segment(0)
			last_seg = line.segment(-2)
			first_seg.isFirst = True
			last_seg.isLast = True

		# node isCentroid flag attribute
		network.create_attribute("NODE", "isCentroid", False)
		centroids = [c for c in network.nodes() if c.is_centroid]
		for node in network.nodes():
			node.isCentroid = bool(node in centroids)

		# node numInLinks and numOutLinks attributes
		network.create_attribute("NODE", "numInLinks")
		network.create_attribute("NODE", "numOutLinks")
		for node in network.nodes():
			node.numInLinks = len(list(node.incoming_links()))
			node.numOutLinks = len(list(node.outgoing_links()))

		# node hasLocalConnection flag attribute
		class BreakLoop (Exception):
			pass

		network.create_attribute("NODE", "hasLocalConnection", False)
		for node in network.centroids():
			try:
				for zone_connector in node.incoming_links():
					for local_link in zone_connector.i_node.incoming_links():
						if local_link["@lane_restriction"] == 1.0:
							node.hasLocalConnection = True
							raise BreakLoop("")
			except:
				pass

		# transit line hasTAP flag attribute
		network.create_attribute("TRANSIT_LINE", "hasTAP", False)
		for line in network.transit_lines():
			has_first_tap = False
			has_last_tap = False
			for link in line.segment(0).i_node.outgoing_links():
				if link.j_node["@tap_id"] > 0:
					has_first_tap = True
					break
			for link in line.segment(-2).j_node.outgoing_links():
				if link.j_node["@tap_id"] > 0:
					has_last_tap = True
					break
			line.hasTAP = has_first_tap and has_last_tap

		# link names attribute
		network.create_attribute("LINK", "linkNames")
		for link in network.links():
			link.linkNames = str(link['#name'] + "," + link['#name_from'] + "," + link['#name_to'])

		def get_emme_object(emme_network, emme_network_object, fields_to_export):
			# Emme network attribute and object names
			net_attr = {
			'NODE':'nodes', 
			'LINK':'links', 
			'TRANSIT_SEGMENT':'transit_segments', 
			'TRANSIT_LINE':'transit_lines',
			'CENTROID':'centroids'
			}

			# read-in entire emme network object as a list
			get_objs = 'list(emme_network.' + net_attr[emme_network_object] + '())'
			uda = eval(get_objs)

			# get list of network object attributes
			obj_attr = []
			if fields_to_export[0] in ['all','All','ALL']:
				if emme_network_object == 'CENTROID':
					obj_attr = emme_network.attributes('NODE')
				else:
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

			table_name = row['Input_Table']
			emme_network_object = row['Emme_Object']
			column_map = row['Column_Map']
			fields_to_export = row['Fields'].split(',')

			# obtain emme network object, csv or dbf input
			if not (pd.isnull(emme_network_object)):
				df = get_emme_object(network, emme_network_object, fields_to_export)
				self.inputs[table_name] = df
			else:
				input_path = self.prop_input_paths[table_name]
				input_ext = os.path.splitext(input_path)[1]
				if input_ext == '.csv':
					df = pd.read_csv(_join(self.path, input_path))
					self.inputs[table_name] = df
				else:
					dbf_path = input_path
					if '%project.folder%' in dbf_path:
						dbf_path = dbf_path.replace('%project.folder%/', '')
					dbf = Dbf5(_join(self.path, dbf_path))
					df = dbf.to_dataframe()
					self.inputs[table_name] = df

		# add scenario table to input dictionary
		self.inputs['scenario'] = self.scenario_df

	def checks(self):
		# read all input DFs into memory
		for key, df in list(self.inputs.items()):
			expr = key + ' = df'
			exec(expr)

		# copy of locals(), a dictionary of all local variables
		calc_dict = locals()

		# read list of checks from CSV file
		self.inputs_checks = pd.read_csv(self.inputs_checks_path)

		# remove all commented checks from the checks list
		self.inputs_checks = self.inputs_checks.loc[[not i for i in (self.inputs_checks['Test'].str.startswith('#'))]]

		# perform calculations and add user-defined data frame subsets
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

			if test_type == 'Calculation':

				try: 					
					calc_expr = test + ' = ' + expr
					exec(calc_expr, {}, calc_dict)
					calc_out = eval(expr, calc_dict)
				except Exception as error:
					print(('An error occurred with the calculation: {}'.format(test)))
					raise

				if str(type(calc_out)) == "<class 'pandas.core.frame.DataFrame'>":
					print(('added '+ row['Test'] + ' as new DataFrame input'))
					self.inputs[row['Test']] = calc_out
					self.inputs_list = self.inputs_list.append({'Input_Table': row['Test'],'Property_Token':'NA','Emme_Object':'NA', \
						'Fields':'NA','Column_Map':'NA','Input_Description':'NA'}, ignore_index = True)
					self.inputs_checks = self.inputs_checks.append({'Test':test, 'Input_Table': table, 'Input_ID_Column':id_col, 'Severity':Severity, \
						'Type':test_type, 'Expression': expr, 'Test_Vals':test_vals, 'Report_Statistic':stat_expr, 'Test_Description': row['Test_Description']}, \
						ignore_index = True)

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

				if (pd.isnull(row['Test_Vals'])):

					# perform test
					try:
						out = eval(expr, calc_dict)
					except Exception as error:
						print(('An error occurred with the check: {}'.format(test)))
						raise

					# check if test result is a series
					if str(type(out)) == "<class 'pandas.core.series.Series'>":
						# for series, the test must be evaluated across all items
						# result is False if a single False is found
						self.results[test] = not (False in out.values)
						
						# reverse results list since we need all False IDs
						reverse_results = [not i for i in out.values]
						error_expr = table + "['" + id_col + "']" + "[reverse_results]"
						error_id_list = eval(error_expr)

						# report first 25 problem IDs in the log
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
						try:
							out = eval(expr)
						except Exception as error:
							print(('An error occurred with the check: {}'.format(test)))
							raise 

						# compute report statistic
						if (pd.isnull(stat_expr)):
							self.report_stat[test] = ''
						else:
							self.report_stat[test] = eval(stat_expr)

						# append to list
						self.result_list[test].append(out)
					self.results[test] = not (False in self.result_list[test])
					self.problem_ids[test] = []
			else:
				# perform calculation
				try: 
					calc_expr = test + ' = ' + expr
					exec(calc_expr, {}, calc_dict)
				except Exception as error:
					print(('An error occurred with the calculation: {}'.format(test)))
					raise

	def prop_file_paths(self):
		prop_files = self.inputs_list[['Input_Table','Property_Token']].dropna()

		load_properties = _m.Modeller().tool('sandag.utilities.properties')
		props = load_properties(_join(self.path, 'conf', 'sandag_abm.properties'))

		for item, row in prop_files.iterrows():
			input_table = row['Input_Table']
			input_path = props[row['Property_Token']]
			self.prop_input_paths[input_table] = input_path

		# obtain scenario year and number of zones
		self.scenario_df['Year'] = [props['scenarioYear']]
		self.scenario_df['zoneCount'] = [props['zones.count']]

	def write_log(self):
		# function to write out the input checker log file
		# there are four blocks
		#   - Introduction
		#	- Summary of checks
		#   - Action Required: FATAL, LOGICAL, WARNINGS
		#   - List of passed checks
		
		# create log file
		now = datetime.datetime.now()

		self.log_path = _join(self.input_checker_path, ('inputCheckerSummary_' + now.strftime("[%Y-%m-%d]") + '.txt'))
		f = open(self.log_path, 'w', newline='')
		
		# define re-usable elements
		seperator1 = '###########################################################'
		seperator2 = '***********************************************************'
		
		# write out Header
		f.write(seperator1 + seperator1 + "\r\n")
		f.write(seperator1 + seperator1 + "\r\n\r\n")
		f.write("\t SANDAG ABM Input Checker Summary File \r\n")
		f.write("\t _____________________________________ \r\n\r\n\r\n")
		f.write("\t Created on: " + now.strftime("%Y-%m-%d %H:%M") + "\r\n\r\n")
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
		f.write("\t The contents of this summary file are organized as follows: \r\n\r\n")
		f.write("\t TALLY OF FAILED CHECKS:\r\n")
		f.write("\t -----------------------\r\n")
		f.write("\t A tally of all failed checks per severity level\r\n\r\n")
		f.write("\t IMMEDIATE ACTION REQUIRED:\r\n")
		f.write("\t -------------------------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a FATAL check\r\n\r\n")
		f.write("\t ACTION REQUIRED:\r\n")
		f.write("\t ---------------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a LOGICAL check\r\n\r\n")
		f.write("\t WARNINGS:\r\n")
		f.write("\t ---------\r\n")
		f.write("\t A log under this heading will be generated in case of failure of a WARNING check\r\n\r\n")
		f.write("\t SUMMARY OF ALL PASSED CHECKS:\r\n")
		f.write("\t ----------------------------\r\n")
		f.write("\t A complete listing of results of all passed checks\r\n\r\n")
		f.write(seperator1 + seperator1 + "\r\n")
		f.write(seperator1 + seperator1 + "\r\n\r\n\r\n\r\n")
		
		# combine results, inputs_checks and inputs_list
		self.inputs_checks['result'] = self.inputs_checks['Test'].map(self.results)
		checks_df = pd.merge(self.inputs_checks, self.inputs_list, on='Input_Table')
		checks_df = checks_df[checks_df.Type=='Test']
		checks_df['reverse_result'] = [not i for i in checks_df.result]
		
		# get count of all FATAL failures
		self.num_fatal = checks_df.result[(checks_df.Severity=='Fatal') & (checks_df.reverse_result)].count()
		
		# get count of all LOGICAL failures
		self.num_logical = checks_df.result[(checks_df.Severity=='Logical') & (checks_df.reverse_result)].count()
		self.logical_fails = checks_df[(checks_df.Severity=='Logical') & (checks_df.reverse_result)]
		
		# get count of all WARNING failures
		self.num_warning = checks_df.result[(checks_df.Severity=='Warning') & (checks_df.reverse_result)].count()

		# write summary of failed checks
		f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
		f.write(seperator2 + seperator2 + "\r\n\r\n")
		f.write('\t' + "TALLY OF FAILED CHECKS \r\n")
		f.write('\t' + "---------------------- \r\n\r\n")
		f.write(seperator2 + seperator2 + "\r\n")
		f.write(seperator2 + seperator2 + "\r\n\r\n\t")
		f.write(' Number of Fatal Errors: ' + str(self.num_fatal))
		f.write('\r\n\t Number of Logical Errors: ' + str(self.num_logical))
		f.write('\r\n\t Number of Warnings: ' + str(self.num_warning))

		def write_check_log(self, fh, row):
			# define constants
			seperator2 = '-----------------------------------------------------------'

			# integerize problem ID list
			problem_ids = self.problem_ids[row['Test']]
			#problem_ids = [int(x) for x in problem_ids]

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
			wrapper = textwrap.TextWrapper(width = 70)
			if (not row['result']) & (len(problem_ids)>0) :
				fh.write("\r\n\t TEST failed for following values of ID Column: " + row['Input_ID_Column'] + " (only up to 25 IDs displayed)")
				fh.write("\r\n\t " + row['Input_ID_Column'] + ": " + "\r\n\t " + "\r\n\t ".join(wrapper.wrap(text = ", ".join(map(str, problem_ids[0:25])))))
				if not (pd.isnull(row['Report_Statistic'])):
					this_report_stat = self.report_stat[row['Test']]
					fh.write("\r\n\t Test Statistics: " + "\r\n\t " + "\r\n\t ".join(wrapper.wrap(text = ", ".join(map(str, this_report_stat[0:25])))))
				fh.write("\r\n\t Total number of failures: " + str(len(self.problem_ids[row['Test']])))
				if ((len(self.problem_ids[row['Test']])) > 25) and (row['Severity'] == 'Logical'):
					fh.write("\r\n\t Open {} for complete list of failed Logical failures.".format(self.logical_log_path))
			else:
				if not (pd.isnull(row['Report_Statistic'])):
					fh.write("\r\n\t Test Statistic: " + str(self.report_stat[row['Test']]))

			# display result for each test val if it was specified
			if not (pd.isnull(row['Test_Vals'])):
				fh.write("\r\n\t TEST results for each test val")
				result_tuples = list(zip(row['Test_Vals'].split(","), self.result_list[row['Test']]))
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
			f.write('\t' + "-------- \r\n\r\n")
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
		f.write('\t' + "------------------------ \r\n\r\n")
		f.write(seperator2 + seperator2 + "\r\n")
		f.write(seperator2 + seperator2 + "\r\n")
		
		# write out log for each check
		for item, row in passed_checks.iterrows():
			write_check_log(self, f, row)
			
		f.close()

	def write_logical_log(self):
		# function to write out the complete list of Logical failures

		# combine results, inputs_checks and inputs_list
		self.inputs_checks['result'] = self.inputs_checks['Test'].map(self.results)
		checks_df = pd.merge(self.inputs_checks, self.inputs_list, on='Input_Table')
		checks_df = checks_df[checks_df.Type=='Test']
		checks_df['reverse_result'] = [not i for i in checks_df.result]
		
		# get count of all LOGICAL failures
		self.num_logical = checks_df.result[(checks_df.Severity=='Logical') & (checks_df.reverse_result)].count()
		self.logical_fails = checks_df[(checks_df.Severity=='Logical') & (checks_df.reverse_result)]

		log_fail_id_tally = 0
		if self.num_logical > 0:
			for item, row in self.logical_fails.iterrows():
				problem_ids = self.problem_ids[row['Test']]
				if len(problem_ids) > 0:
					log_fail_id_tally += 1
		
		if log_fail_id_tally > 0:

			# create log file
			now = datetime.datetime.now()

			self.logical_log_path = _join(self.input_checker_path, ('completeLogicalFails_' + now.strftime("[%Y-%m-%d]") + '.txt'))
			f = open(self.logical_log_path, 'w', newline='')
			
			# define re-usable elements
			seperator1 = '###########################################################'
			seperator2 = '***********************************************************'
			
			# write out Header
			f.write(seperator1 + seperator1 + "\r\n")
			f.write(seperator1 + seperator1 + "\r\n\r\n")
			f.write("\t SANDAG ABM Input Checker Logical Failures Complete List \r\n")
			f.write("\t _______________________________________________________ \r\n\r\n\r\n")
			f.write("\t Created on: " + now.strftime("%Y-%m-%d %H:%M") + "\r\n\r\n")
			f.write("\t Notes:-\r\n")		
			f.write("\t The SANDAG ABM Input Checker performs various QA/QC checks on SANDAG ABM inputs as specified by the user.\r\n")
			f.write("\t The Input Checker allows the user to specify three severity levels for each QA/QC check:\r\n\r\n")
			f.write("\t 1) FATAL  2) LOGICAL  3) WARNING\r\n\r\n")
			f.write("\t This file provides the complete list of failed checks for checks of severity type Logical. \r\n")
			f.write(seperator1 + seperator1 + "\r\n")
			f.write(seperator1 + seperator1 + "\r\n\r\n\r\n\r\n")

			# write total number of failed logical checks
			f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n\r\n")
			f.write('\t' + "TALLY OF FAILED CHECKS \r\n")
			f.write('\t' + "---------------------- \r\n\r\n")
			f.write(seperator2 + seperator2 + "\r\n")
			f.write(seperator2 + seperator2 + "\r\n\r\n\t")
			f.write('\r\n\t Number of Logical Errors: ' + str(self.num_logical))

			def write_logical_check_log(self, fh, row):
				# define constants
				seperator2 = '-----------------------------------------------------------'

				# integerize problem ID list
				problem_ids = self.problem_ids[row['Test']]
				#problem_ids = [int(x) for x in problem_ids]

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
				wrapper = textwrap.TextWrapper(width = 70)
				if (not row['result']) & (len(problem_ids)>0) :
					fh.write("\r\n\t TEST failed for following values of ID Column: " + row['Input_ID_Column'])
					fh.write("\r\n\t " + row['Input_ID_Column'] + ": " + "\r\n\t " + "\r\n\t ".join(wrapper.wrap(text = ", ".join(map(str, problem_ids)))))
					if not (pd.isnull(row['Report_Statistic'])):
						this_report_stat = self.report_stat[row['Test']]
						fh.write("\r\n\t Test Statistics: " + "\r\n\t " + "\r\n\t ".join(wrapper.wrap(text = ", ".join(map(str, this_report_stat)))))
					fh.write("\r\n\t Total number of failures: " + str(len(self.problem_ids[row['Test']])))
				else:
					if not (pd.isnull(row['Report_Statistic'])):
						fh.write("\r\n\t Test Statistic: " + str(self.report_stat[row['Test']]))

				# display result for each test val if it was specified
				if not (pd.isnull(row['Test_Vals'])):
					fh.write("\r\n\t TEST results for each test val")
					result_tuples = list(zip(row['Test_Vals'].split(","), self.result_list[row['Test']]))
					fh.write("\r\n\t ")
					fh.write(','.join('[{} - {}]'.format(x[0],x[1]) for x in result_tuples))
					
				fh.write("\r\n" + seperator2 + seperator2 + "\r\n\r\n")
			
			# write out ACTION REQUIRED section if needed
			if self.num_logical > 0:
				logical_checks = checks_df[(checks_df.Severity=='Logical') & (checks_df.reverse_result)]
				f.write('\r\n\r\n' + seperator2 + seperator2 + "\r\n")
				f.write(seperator2 + seperator2 + "\r\n\r\n")
				f.write('\t' + "LOG OF ALL FAILED LOGICAL CHECKS \r\n")
				f.write('\t' + "-------------------------------- \r\n\r\n")
				f.write(seperator2 + seperator2 + "\r\n")
				f.write(seperator2 + seperator2 + "\r\n")
				
				#write out log for each check
				for item, row in logical_checks.iterrows():
					if len(self.problem_ids[row['Test']]) > 25:
						write_logical_check_log(self, f, row)

			f.close()

	def check_logical(self):
		if self.num_logical > 0:
			# raise exception for each logical check fail
			for item, row in self.logical_fails.iterrows():
				answer = dialog.alert_question(
					message = "The following Logical check resulted in at least 1 error: {} \n Open {} for details. \
					 \n\n Click OK to continue or Cancel to stop run.".format(row['Test'], self.log_path),
					title = "Logical Check Error",
					answers = [("OK", dialog.YES_ROLE), ("Cancel", dialog.REJECT_ROLE)]
					)

				if answer == 1:
					raise Exception("Input checker was cancelled")

	def check_num_fatal(self):
		# return code to the main model based on input checks and results
		if self.num_fatal > 0:
			raise Exception("Input checker failed, {} fatal errors found. Open {} for details.".format(self.num_fatal, self.log_path))