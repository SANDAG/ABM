""" ABM Run Time Summary Tool

Generates a CSV file containing a run time summary for
a completed ABM run. Utilizes the Emme Modeler API to
query the Emme logbook for the run time information.

"""

# Importing libraries
import os
import pandas as pd
import traceback as _traceback
import inro.emme.desktop.app as _app
import inro.modeller as _m
from functools import reduce

_dir = os.path.dirname
_join = os.path.join

ATTR_SUFFIX = "_304A7365_C276_493A_AB3B_9B2D195E203F"

# Define unneeded entries
exclude = ()


class RunTime(_m.Tool()):

    def __init__(self):
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.path = _dir(project_dir)
        self.output_path = ''
        self.output_summary_path = ''
        self.begin = ''
        self.end = ''

    def run(self):
        """
        Executes Run Time Summary tool
        """
        self.tool_run_msg = ""
        try:
            self(path=self.path)
            run_msg = "Run Time Tool Complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg,
                                                           escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                                        error, _traceback.format_exc(error))
            raise

        return

    def __call__(self, path=""):
        """
        Calculates ABM run times and saves to CSV file

        :param path: Scenario file path
        """

        # Get element IDs for model runs
        run_ids = self.get_runs()

        # Define needed attributes (begin and end times)
        self.begin = "begin" + ATTR_SUFFIX
        self.end = "end" + ATTR_SUFFIX
        attrs = [self.begin, self.end]

        # DAG (Directed Acyclic Graph) for topological sort
        # An edge is created between each step and the next step in the run
        step_dict = {
            'Total Run Time': {
                'next': set(),
                'prev': set()
            }
        }
        prev_step = ''

        runtime_dfs = []
        for run_id in run_ids:

            name = 'Run ID: {}'.format(run_id)

            # Creating dummy total time
            total_entry = (run_id, 'Total Run Time', '0:0')

            prev_step = 'Total Run Time'

            # Get second level child entry run times if they exist
            child_runtimes = self.get_child_runtimes(run_id, attrs)

            # Get third (final) level child entry run times
            final_runtimes = [total_entry]
            for index, info in enumerate(child_runtimes):
                if info[1] == 'Final traffic assignments':
                    final_runtimes.append([0, 'Iteration 4', 0])

                    # add edge to DAG
                    if 'Iteration 4' in step_dict:
                        step_dict['Iteration 4']['prev'].add(prev_step)
                    else:
                        step_dict['Iteration 4'] = {
                            'next': set(),
                            'prev': {prev_step}
                        }
                    step_dict[prev_step]['next'].add('Iteration 4')
                    prev_step = 'Iteration 4'

                final_runtimes.append(info)

                # add edge to DAG
                if info[1] in step_dict:
                    step_dict[info[1]]['prev'].add(prev_step)
                else:
                    step_dict[info[1]] = {
                        'next': set(),
                        'prev': {prev_step}
                    }
                step_dict[prev_step]['next'].add(info[1])
                prev_step = info[1]

                if 'Iteration' in info[1]:
                    iter_str = '_{}'.format(info[1])

                    # Add iteration to children
                    iteration_children = self.get_child_runtimes(
                                                        info[0], attrs)
                    for index, child in enumerate(iteration_children):
                        step = child[1]
                        iteration_children[index][1] = step + iter_str

                        # add edge to DAG
                        if (step + iter_str) in step_dict:
                            step_dict[step + iter_str]['prev'].add(prev_step)
                        else:
                            step_dict[step + iter_str] = {
                                'next': set(),
                                'prev': {prev_step}
                            }
                        step_dict[prev_step]['next'].add(step + iter_str)
                        prev_step = step + iter_str

                    final_runtimes += iteration_children

            # Create run time summary table
            index = [x[1] for x in final_runtimes]
            values = [x[2] for x in final_runtimes]
            runtime_series = pd.Series(index=index, data=values)
            runtime_series.name = name
            runtime_df = runtime_series.to_frame()

            # Create intial time
            zero_time = pd.to_datetime('0:0', format='%H:%M')

            # Calculate iteration 4 run time if it exists
            iter_str = 'Iteration 4'
            if iter_str in runtime_df.index:
                iter_4_index = runtime_df.index.get_loc('Iteration 4')
                iter_4_df = runtime_df.iloc[iter_4_index+1:, :].copy()
                iter_4_df[name] = (pd.to_datetime(
                                            iter_4_df[name], format='%H:%M') -
                                   zero_time)
                iter_4_time = iter_4_df[name].sum()
                runtime_df.loc[iter_str, :] = self.format_runtime(iter_4_time)

            # Calculate total runtime
            is_iter_row = pd.Series(runtime_df.index).str.startswith('Iter')
            total_df = runtime_df[~is_iter_row.values].copy()
            total_df[name] = (pd.to_datetime(total_df[name], format='%H:%M') -
                              zero_time)
            total_time = total_df[name].sum()
            run_str = 'Total Run Time'
            runtime_df.loc[run_str, :] = self.format_runtime(total_time)

            runtime_dfs.append(runtime_df)

        # Merge all run time data frames if more than one exists and save
        file_name = 'runtime_summary.csv'
        self.output_path = _join(path, 'output', file_name)
        result = self.combine_dfs(runtime_dfs, step_dict)
        if result[1]:
            result[0].to_csv(self.output_path, header=True, index=False)
        else:
            result[0].to_csv(self.output_path, header=False)

        return

    def get_runs(self):
        """
        Queries the Emme logbook to retrieve the IDs of all
        model runs.

        :returns: List of IDs of model runs
        """

        # Emme logbook query
        query = """
        SELECT elements.element_id, elements.tag
            FROM elements
            JOIN attributes KEYVAL1 ON (elements.element_id=KEYVAL1.element_id)
                WHERE  (KEYVAL1.name=="self"
                    AND KEYVAL1.value LIKE "sandag.master_run")
        ORDER BY elements.element_id ASC
        """
        all_entries = _m.logbook_query(query)

        # Retrieves model run IDs
        run_ids = []
        for entry in all_entries:
            parent_id = entry[0]
            run_ids.append(parent_id)

        if len(run_ids) == 0:
            raise ValueError('A model run does not exist.')

        return run_ids

    def get_attributes(self, element_id):
        """
        Queries all the attributes of an Emme logbook element

        :param element_id: Integer ID of element
        :returns: List of tuples containing information for
                different attributes of an element.
        """

        # Emme logbook query
        query = """
        SELECT name, value FROM attributes
            WHERE attributes.element_id == %i
        """ % element_id

        return _m.logbook_query(query)

    def format_runtime(self, time):
        """
        Transforms a datetime object to a reformatted
        date string. Formatted as '{hours}:{'minutes'}'

        :param time Datetime object
        """

        hours = str(int(time.total_seconds() // 3600))
        minutes = str(int((time.total_seconds() % 3600) // 60)).zfill(2)
        formatted_runtime = hours + ":" + minutes

        return formatted_runtime

    def calc_runtime(self, begin, end):
        """
        Helper function for get_child_runtimes

        Converts beginning and end datetime strings into
        a formatted time delta. Formatted as '{hours}:{minutes}'

        :param begin: String representing beginning date
        :param end: String representating ending date
        :returns: String representing element runtime
        """

        # Calculate total run time
        total_runtime = pd.to_datetime(end) - pd.to_datetime(begin)

        # Format run time: '{hours}:{minutes}'
        formatted_runtime = self.format_runtime(total_runtime)

        # Defaulting zero second times to 1 second
        if formatted_runtime == '0:00':
            formatted_runtime = '0:01'

        return formatted_runtime

    def get_children(self, parent_id):
        """
        Retrieves all child elements for a parent element

        :param parent_id: Integer ID of parent element
        :returns: List of tuples containing child IDs and names
        """

        # Emme logbook query
        query = """
        SELECT elements.element_id, elements.tag
            FROM elements WHERE parent_id==%i
        ORDER BY elements.element_id ASC
        """ % parent_id
        child_entries = _m.logbook_query(query)

        return child_entries

    def get_child_runtimes(self, parent_id, attrs):
        """
        Calculates the run times for the child elements of
        a parent element

        :param parent_id: Integer ID of parent element
        :param attrs: List of strings representing attributes to query
        :returns: List of tuples containing information for child elements
        """

        # Get child elements
        all_child_entries = self.get_children(parent_id)

        # Calculates run times for each child element
        runtime_child_entries = []
        for element_id, name in all_child_entries:
            attributes = dict(self.get_attributes(element_id))

            # Gets element information if desired attribute is
            # available and it is not included in the excluded list
            if attrs[0] in attributes:
                begin = attributes[attrs[0]]

                # Handles cases where model fails mid iteration
                try:
                    end = attributes[attrs[1]]
                    runtime = self.calc_runtime(begin, end)
                except KeyError:
                    end = None
                    runtime = None
                runtime_child_entries.append([element_id, name, runtime])

        return runtime_child_entries

    def combine_dfs(self, df_list, step_dict):
        """
        Combines a list of Pandas DataFrames into a single
        summary DataFrame

        :param df_list: List of Pandas DataFrames
        :returns: Tuple contianing single run time summary DataFrame
                  and boolean whether it contains multiple runs
        """
        if len(df_list) > 1:
            # Compute final step order
            # Topological sort with Kahn's algorithm
            # "Total Run Time" is always the first step
            # Steps are added to the final order only after all parent steps have been added
            step_no_prev = {'Total Run Time'}
            step_order = []
            while step_no_prev:
                curr_step = step_no_prev.pop()
                step_order.append(curr_step)
                while step_dict[curr_step]['next']:
                    next_step = step_dict[curr_step]['next'].pop()
                    step_dict[next_step]['prev'].remove(curr_step)
                    if not step_dict[next_step]['prev']:
                        step_no_prev.add(next_step)

            # Drop tables with less than 2 entries
            final_dfs = []
            for df in df_list:
                if len(df.dropna()) > 1:
                    final_dfs.append(df.reset_index(drop=False))

            # Merge all data frames
            final_df = reduce(lambda left, right:
                              pd.merge(left, right, on=['index'], how='outer'),
                              final_dfs)
            
            # Sort data by step order
            final_df = final_df.set_index('index', drop= False)
            final_df = final_df.reindex(step_order)

            # Remove unneeded entries
            is_excluded = pd.Series(final_df.index).str.startswith(exclude)
            final_df = final_df[~(is_excluded.values)]

            # Remove appended iteration markers
            final_df['index'] = (final_df['index'].apply(
                                            lambda x: x.split('_')[0]))

            final_df = final_df.rename(columns={'index': 'Step'})
            result = (final_df, True)

        else:
            final_df = df_list[0]
            result = (final_df, False)

        return result
