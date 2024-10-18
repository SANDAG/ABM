##//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#////  utilities/demand.py                                                  ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 101


import inro.emme.datatable as _dt
import inro.modeller as _m
from collections import OrderedDict
from contextlib import contextmanager as _context
from copy import deepcopy as _copy
import multiprocessing as _multiprocessing
import re as _re
import pandas as _pandas
import numpy as _numpy
import os


class Utils(_m.Tool()):

    def page(self):
        pb = _m.ToolPageBuilder(self, runnable=False)
        pb.title = 'Demand utility'
        pb.description = """Utility tool / module for common code. Not runnable."""
        pb.branding_text = ' - SANDAG - Utilities'
        return pb.render()


# Read a CSV file, store it as a DataTable and return a representative DataFrame
def csv_to_data_table(path, overwrite=False):
    layer_name = os.path.splitext(os.path.basename(path))[0]
    data_source = _dt.DataSource(path)
    data = data_source.layer(layer_name).get_data()
    desktop = _m.Modeller().desktop
    dt_db = desktop.project.data_tables()
    table = dt_db.create_table(layer_name, data, overwrite=overwrite)
    return table_to_dataframe(table)


# Convert a DataTable into a DataFrame
def table_to_dataframe(table):
    if type(table) == str:
        desktop = _m.Modeller().desktop
        dt_db = desktop.project.data_tables()
        table_name = table
        table = dt_db.table(table)
        if not table:
            raise Exception('%s is not a valid table name.' %table_name)

    df = _pandas.DataFrame()
    for attribute in table.get_data().attributes():
        try:
            df[attribute.name] = attribute.values.astype(float)
        except Exception as e:
            df[attribute.name] = attribute.values

    return df


# Convert a dataframe to a datatable
def dataframe_to_table(df, name):
    desktop = _m.Modeller().desktop
    dt_db = desktop.project.data_tables()
    data = _dt.Data()
    for key in df.columns:
        found_dtype = False
        dtypes = [
            (bool, True, 'BOOLEAN'),
            (int, 0, 'INTEGER32'),
            (int, 0, 'INTEGER'),
            (float, 0, 'REAL')
        ]
        for caster, default, name in dtypes:
            try:
                df[[key]] = df[[key]].fillna(default)
                values = df[key].astype(caster)
                attribute = _dt.Attribute(key, values, name)
                found_dtype = True
                break
            except ValueError:
                pass

        if not found_dtype:
            df[[key]] = df[[key]].fillna(0)
            values = df[key].astype(str)
            attribute = _dt.Attribute(key, values, 'STRING')

        data.add_attribute(attribute)

    table = dt_db.create_table(name, data, overwrite=True)
    return table

# Add missing (usually external zones 1 to 12) zones to the DataFrame 
# and populate with zeros
def add_missing_zones(df, scenario):
    all_zones = scenario.zone_numbers
    existing_zones = df['taz'].values
    missing_zones = set(all_zones) - set(existing_zones)
    num_missing = len(missing_zones)
    if num_missing == 0:
        return df

    ext_df = _pandas.DataFrame()
    for c in df.columns:
        ext_df[c] = _numpy.zeros(num_missing)
    ext_df['taz'] = _numpy.array(list(missing_zones))
    df = _pandas.concat([df, ext_df])
    df = df.sort_values('taz', ascending=True)      # sort method was deprecated in version 0.20.0,yma,2/12/2019 
    return df


def add_select_processors(tool_attr_name, pb, tool):
    max_processors = min(_multiprocessing.cpu_count(), int(os.environ.get("NUMBER_OF_PROCESSORS", 999)))
    tool._max_processors = max_processors
    options = [("MAX-1", "Maximum available - 1"), ("MAX", "Maximum available")]
    options.extend([(n, "%s processors" % n) for n in range(1, max_processors + 1) ])
    pb.add_select(tool_attr_name, options, title="Number of processors:")


def parse_num_processors(value):
    max_processors = _multiprocessing.cpu_count()
    if isinstance(value, int):
        return value
    if isinstance(value, str):
        if value == "MAX":
            return max_processors
        if _re.match("^[0-9]+$", value):
            return int(value)
        result = _re.split("^MAX[\s]*-[\s]*", value)
        if len(result) == 2:
            return max(max_processors - int(result[1]), 1)
    if value:
        return int(value)
    return value

class MatrixCalculator(object):
    def __init__(self, scenario, num_processors=0):
        self._scenario = scenario
        self._matrix_calc = _m.Modeller().tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        self._specs = []
        self._last_report = None
        self.num_processors = num_processors

    @property
    def num_processors(self):
        return self._num_processors

    @num_processors.setter
    def num_processors(self, value):
        self._num_processors = parse_num_processors(value)

    @property
    def last_report(self):
        return _copy(self._last_report)

    @_context
    def trace_run(self, name):
        with _m.logbook_trace(name):
            yield
            self.run()

    def add(self, result, expression, constraint=None, aggregation=None):
        spec = self._format_spec(result, expression, constraint, aggregation)
        self._specs.append(spec)

    def _format_spec(self, result, expression, constraint, aggregation):
        spec = {
            "result": result,
            "expression": expression,
            "type": "MATRIX_CALCULATION"
        }
        if constraint is not None:
            if isinstance(constraint, (list, tuple)):
                # specified as list of by_value inputs
                constraint = {
                    "by_value": {
                        "od_values": constraint[0],
                        "interval_min": constraint[1],
                        "interval_max": constraint[2],
                        "condition": constraint[3]
                    }
                }
            elif "od_values" in constraint:
                # specified as the by_value sub-dictionary only
                constraint = {"by_value": constraint}
            # By zone constraints
            elif ("destinations" in constraint or "origins" in constraint):
                # specified as the by_zone sub-dictionary only
                constraint = {"by_zone": constraint}
            # otherwise, specified as a regular full constraint dictionary
            if "by_value" in constraint:
                # cast the inputs to the correct values
                constraint["by_value"]["od_values"] = \
                    str(constraint["by_value"]["od_values"])
                constraint["by_value"]["condition"] = \
                    constraint["by_value"]["condition"].upper()
            spec["constraint"] = constraint

            #Add None for missing key values if needed
            if "by_value" not in constraint:
                constraint["by_value"] = None
            if "by_zone" not in constraint:
                constraint["by_zone"] = None

        else:
            spec["constraint"] = None

        if aggregation is not None:
            if isinstance(aggregation, str):
                aggregation = {"origins": aggregation}
            spec["aggregation"] = aggregation
        else:
            spec["aggregation"] = None
        return spec

    def add_spec(self, spec):
        self._specs.append(spec)

    def run(self):
        specs, self._specs = self._specs, []
        report = self._matrix_calc(specs, scenario=self._scenario,
                                   num_processors=self._num_processors)
        self._last_report = report
        return report

    def run_single(self, result, expression, constraint=None, aggregation=None):
        spec = self._format_spec(result, expression, constraint, aggregation)
        return self._matrix_calc(spec, scenario=self._scenario,
                                 num_processors=self._num_processors)


def reduce_matrix_precision(matrices, precision, num_processors, scenario):
    emmebank = scenario.emmebank
    calc = MatrixCalculator(scenario, num_processors)
    gen_utils = _m.Modeller().module('sandag.utilities.general')
    with gen_utils.temp_matrices(emmebank, "SCALAR", 2) as (sum1, sum2):
        sum1.name = "ORIGINAL_SUM"
        sum2.name = "ROUNDED_SUM"
        for mat in matrices:
            mat = emmebank.matrix(mat).named_id
            with calc.trace_run('Reduce precision for matrix %s' % mat):
                calc.add(sum1.named_id, mat, aggregation={"destinations": "+", "origins": "+"})
                calc.add(mat, "{mat} * ({mat} >= {precision})".format(
                    mat=mat, precision=precision))
                calc.add(sum2.named_id, mat, aggregation={"destinations": "+", "origins": "+"})
                calc.add(sum2.named_id, "({sum2} + ({sum2} == 0))".format(sum2=sum2.named_id))
                calc.add(mat, "{mat} * ({sum1} / {sum2})".format(
                    mat=mat, sum2=sum2.named_id, sum1=sum1.named_id))


def create_full_matrix(name, desc, scenario):
    create_matrix = _m.Modeller().tool(
        "inro.emme.data.matrix.create_matrix")
    emmebank = scenario.emmebank
    matrix = emmebank.matrix(name)
    if matrix:
        ident = matrix.id
    else:
        used_ids = set([])
        for m in emmebank.matrices():
            if m.prefix == "mf":
                used_ids.add(int(m.id[2:]))
        for i in range(900, emmebank.dimensions["full_matrices"]):
            if i not in used_ids:
                ident = "mf" + str(i)
                break
        else:
            raise Exception("Not enough available matrix IDs for selected demand. Change database dimensions to increase full matrices.")
    return create_matrix(ident, name, desc, scenario=scenario, overwrite=True)


def demand_report(matrices, label, scenario, report=None):
    text = ['<div class="preformat">']
    text.append("%-28s %13s" % ("name", "sum"))
    for name, data in matrices:
        stats = (name, data.sum())
        text.append("%-28s %13.7g" % stats)
    text.append("</div>")
    title = "Demand summary"
    if report is None:
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(label, report.render())
    else:
        report.wrap_html(label, "<br>".join(text))
