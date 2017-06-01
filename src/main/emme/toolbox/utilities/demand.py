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
from contextlib import contextmanager
from collections import OrderedDict
import pandas as pd
import numpy as np
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

    df = pd.DataFrame()
    for attribute in table.get_data().attributes():
        try:
            df[attribute.name] = attribute.values.astype(float)
        except Exception, e:
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

    ext_df = pd.DataFrame()
    for c in df.columns:
        ext_df[c] = np.zeros(num_missing)
    ext_df['taz'] = np.array(list(missing_zones))
    df = pd.concat([df, ext_df])
    df = df.sort('taz', ascending=True)
    return df


_properties_lookup = {}


class Properties(object):

    def __new__(cls, path="./sandag_abm.properties", *args, **kwargs):
        path = os.path.normpath(os.path.abspath(unicode(path)))
        if os.path.isdir(path):
            path = os.path.join(path, "sandag_abm.properties")
        properties = _properties_lookup.get(os.path.normcase(path), None)
        return properties or object.__new__(cls)

    def __init__(self, path="./sandag_abm.properties"):
        if os.path.isdir(path):
            path = os.path.join(path, "sandag_abm.properties")
        if hasattr(self, "_created"):
            # TODO: timestamp check untested
            if self._timestamp == os.path.getmtime(self._path):
                return
        self._path = os.path.normpath(os.path.abspath(path))
        self._load_properties()
        self._created = True
        self._timestamp = os.path.getmtime(self._path)
        _properties_lookup[os.path.normcase(self._path)] = self        

    def _load_properties(self):
        # TODO: could generate UI based on properties file contents?
        self._prop = prop = OrderedDict()
        self._comments = comments = {}
        with open(self._path, 'r') as properties:
            comment = []
            for line in properties:
                line = line.strip()
                if not line or line.startswith('#'):
                    comment.append(line)
                    continue
                key, value = line.split('=')
                key = key.strip()
                tokens = value.split(',')
                if len(tokens) > 1:
                    value = self._convert_list(tokens)
                else:
                    value = self._convert(value)
                prop[key] = value
                comments[key], comment = comment, []

    def _convert_list(self, values):
        converted_values = []
        for v in values:
            converted_values.append(self._convert(v))
        return converted_values

    def _convert(self, value):
        value = str(value).strip()
        if value == 'true':
            return True
        elif value == 'false':
            return False
        for caster in int, float:
            try:
                return caster(value)
            except ValueError:
                pass
        return value

    def _write_properties(self, path):
        # TODO: untested
        with open(path, 'w') as f:
            for key, value in self._prop.iteritems():
                if isinstance(value, bool):
                    value = "true" if value else "false"
                elif isinstance(value, list):
                    value = ",".join(value)
                comment = self._comments.get(key)
                if comment:
                    for line in comment:
                        f.write(comment)
                        f.write("\n")
                f.write("%s = %s\n" % (key, value))

    def __setitem__(self, key, item): 
        self._prop[key] = item

    def __getitem__(self, key): 
        return self._prop[key]

    def __repr__(self): 
        return repr(self._prop)

    def __len__(self): 
        return len(self._prop)

    def __delitem__(self, key): 
        del self._prop[key]

    def clear(self):
        return self._prop.clear()

    def has_key(self, k):
        return self._prop.has_key(k)

    def pop(self, k, d=None):
        return self._prop.pop(k, d)

    def update(self, *args, **kwargs):
        return self._prop.update(*args, **kwargs)

    def keys(self):
        return self._prop.keys()

    def values(self):
        return self._prop.values()

    def items(self):
        return self._prop.items()

    def pop(self, *args):
        return self._prop.pop(*args)

    def __cmp__(self, dict):
        return cmp(self._prop, dict)

    def __contains__(self, item):
        return item in self._prop

    def __iter__(self):
        return iter(self._prop)

    def __unicode__(self):
        return unicode(repr(self._prop))


def reduce_matrix_precision(matrices, precision):
    eb = _m.Modeller().emmebank
    m_calc = _m.Modeller().tool(
        'inro.emme.matrix_calculation.matrix_calculator')

    for m in matrices:
        with _m.logbook_trace('Reduce precision for matrix %s' % m):
            m = eb.matrix(m)
            spec = {
                "expression": "%s" % m.named_id,
                "aggregation": {
                    "destinations": "+",
                    "origins": "+"
                },
                "type": "MATRIX_CALCULATION"
            }
            mtx_sum = m_calc(spec)['result']

            spec = {
                "expression": "0",
                "result": "%s" % m.named_id,
                "constraint": {
                    "by_value": {
                        "interval_min": 0,
                        "interval_max": precision,
                        "condition": "INCLUDE",
                        "od_values": "%s" % m.named_id
                    }
                },
                "type": "MATRIX_CALCULATION"
            }
            m_calc(spec)

            spec = {
                "expression": "%s" % m.named_id,
                "aggregation": {
                    "destinations": "+",
                    'origins': '+'
                },
                "type": "MATRIX_CALCULATION"
            }
            rounded_mtx_sum = m_calc(spec)['result']

            spec = {
                "expression": "%s * %s / %s" % (
                m.named_id, mtx_sum, rounded_mtx_sum),
                "result": "%s" % m.named_id,
                "type": "MATRIX_CALCULATION"
            }
            m_calc(spec)
