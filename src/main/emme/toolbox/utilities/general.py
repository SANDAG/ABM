#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// transit_assignment.py                                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

TOOLBOX_ORDER = 102


import inro.modeller as _m
import inro.emme.datatable as _dt
import omx as _omx
from osgeo import ogr as _ogr
from contextlib import contextmanager as _context
from collections import OrderedDict
from itertools import izip as _izip
import traceback as _traceback
import re as _re
import json as _json
import os


class UtilityTool(_m.Tool()):

    tool_run_msg = ""

    def page(self):
        pb = _m.ToolPageBuilder(self, runnable=False)
        pb.title = "General utility"
        pb.description = """Utility tool / module for common code. Not runnable."""
        pb.branding_text = "- SANDAG"
        if self.tool_run_msg:
            pb.add_html(self.tool_run_msg)

        return pb.render()

    def run(self):
        pass


class NetworkCalculator(object):
    def __init__(self, scenario):
        self._scenario = scenario
        self._network_calc = _m.Modeller().tool(
            "inro.emme.network_calculation.network_calculator")

    def __call__(self, result, expression, selections=None, aggregation=None):
        spec = {
            "result": result,
            "expression": expression,
            "aggregation": aggregation,
            "type": "NETWORK_CALCULATION"
        }
        if selections is not None:
            if isinstance(selections, basestring):
                selections = {"link": selections}
            spec["selections"] = selections
        else:
            spec["selections"] = {"link": "all"}
        return self._network_calc(spec, self._scenario)


@_context
def temp_matrices(emmebank, mat_type, total=1, default_value=0.0):
    matrices = []
    try:
        while len(matrices) != int(total):
            try:
                ident = emmebank.available_matrix_identifier(mat_type)
            except _except.CapacityError:
                raise _except.CapacityError(
                    "Insufficient room for %s required temp matrices." % total)
            matrices.append(emmebank.create_matrix(ident, default_value))
        yield matrices[:]
    finally:
        for matrix in matrices:
            emmebank.delete_matrix(matrix)

@_context
def temp_attrs(scenario, attr_type, idents, default_value=0.0):
    attrs = []
    try:
        for ident in idents:
            attrs.append(scenario.create_extra_attribute(attr_type, ident, default_value))
        yield attrs[:]
    finally:
        for attr in attrs:
            scenario.delete_extra_attribute(attr)


@_context
def backup_and_restore(scenario, backup_attributes):
    backup = {}
    for elem_type, attributes in backup_attributes.iteritems():
        backup[elem_type] = scenario.get_attribute_values(elem_type, attributes)
    try:
        yield
    finally:
        for elem_type, attributes in backup_attributes.iteritems():
            scenario.set_attribute_values(elem_type, attributes, backup[elem_type])


@_context
def temp_functions(emmebank):    
    change_function = _m.Modeller().tool(
        "inro.emme.data.function.change_function")
    with _m.logbook_trace("Set functions to skim parameter"):
        for func in emmebank.functions():
            if func.prefix=="fd":
                exp = func.expression
                if "volau" in exp:
                    exp = exp.replace("volau", "el2")
                    change_function(func, exp, emmebank)
    try:
        yield
    finally:
        with _m.logbook_trace("Reset functions to assignment parameter"):
            for func in emmebank.functions():
                if func.prefix=="fd":
                    exp = func.expression
                    if "el2" in exp:
                        exp = exp.replace("el2", "volau")
                        change_function(func, exp, emmebank)

                        
class DataTableProc(object):

    def __init__(self, table_name, path=None, data=None):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        project = desktop.project
        self._dt_db = dt_db = project.data_tables()
        if path:
            #try:
            source = _dt.DataSource(path)
            #except:
            #    raise Exception("Cannot open file at %s" % path)
            layer = source.layer(table_name)
            self._data = layer.get_data()
        elif data:
            table = dt_db.create_table(table_name, data, overwrite=True)
            self._data = data
        else:
            table = dt_db.table(table_name)
            self._data = table.get_data()
        self._load_data()

    def _load_data(self):
        data = self._data
        self._values = [a.values for a in data.attributes()]
        self._attr_names = [a.name for a in data.attributes()]
        self._index = dict((k, i) for i,k in enumerate(self._attr_names))
        if "geometry" in self._attr_names:
            geo_coords = []
            attr = data.attribute("geometry")
            for record in attr.values:
                geo_obj = _ogr.CreateGeometryFromWkt(record.text)
                geo_coords.append(geo_obj.GetPoints())
            self._values.append(geo_coords)
            self._attr_names.append("geo_coordinates")

    def __iter__(self):
        values, attr_names = self._values, self._attr_names
        return (dict(_izip(attr_names, record))
                for record in _izip(*values))

    def save(self, name, overwrite=False):
        self._dt_db.create_table(name, self._data, overwrite=overwrite)

    def values(self, name):
        index = self._index[name]
        return self._values[index]


class Snapshot(object):
    def __getitem__(self, key):
        return getattr(self, key)

    def __setitem__(self, key, value):
        setattr(self, key, value)

    def to_snapshot(self):
        try:
            attributes = getattr(self, "attributes", [])
            snapshot = {}
            for name in attributes:
                snapshot[name] = unicode(self[name])
            return _json.dumps(snapshot)
        except Exception:
            return "{}"

    def from_snapshot(self, snapshot):
        try:
            snapshot = _json.loads(snapshot)
            attributes = getattr(self, "attributes", [])
            for name in attributes:
                self[name] = snapshot[name]
        except Exception, error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error), False)
        return self

    def get_state(self):
        attributes = getattr(self, "attributes", [])
        state = {}
        for name in attributes:
            try:
                state[name] = self[name]
            except _m.AttributeError, error:
                state[name] = unicode(error)
        return state


def log_snapshot(name, namespace, snapshot):
    try:
        _m.logbook_snapshot(name=name, comment="", namespace=namespace,
                            value=_json.dumps(snapshot))
    except Exception as error:
        print error
        
        
@_m.logbook_trace(name="Export matrices to OMX", save_arguments=True)
def export_to_omx(matrices, export_file, scenario, omx_key="ID_NAME"):
    emmebank = scenario.emmebank
    n_zones = len(scenario.zone_numbers)

    text_encoding = emmebank.text_encoding
    if omx_key == "ID_NAME":
        generate_key = lambda m: "%s_%s" % (
            m.id.encode(text_encoding), m.name.encode(text_encoding))
    elif omx_key == "NAME":
        generate_key = lambda m: m.name.encode(text_encoding)
    elif omx_key == "ID":
        generate_key = lambda m: m.id.encode(text_encoding)
    if isinstance(matrices, list):
        matrices = OrderedDict([(generate_key(emmebank.matrix(m)), m) for m in matrices])

    omx_file = _omx.openFile(export_file, 'w')
    try:
        n_matrices = len(matrices)
        for key, matrix_name in matrices.iteritems():
            if isinstance(matrix_name, list):
                matrix = emmebank.matrix(matrix_name[0])
                numpy_array = matrix.get_numpy_data(scenario.id)
                for name in matrix_name[1:]:
                    matrix = emmebank.matrix(name)
                    numpy_array = numpy_array + matrix.get_numpy_data(scenario.id)
            else:
                matrix = emmebank.matrix(matrix_name)
                numpy_array = matrix.get_numpy_data(scenario.id)
            numpy_array = numpy_array.astype(dtype="float64", copy=False)

            if matrix.type == "DESTINATION":
                numpy_array = _numpy.resize(numpy_array, (1, n_zones))
                chunkshape = None
            elif matrix.type == "ORIGIN":
                numpy_array = _numpy.resize(numpy_array, (n_zones, 1))
                chunkshape = None
            else:
                chunkshape = (1, numpy_array.shape[0])
            attrs = {
                "description": matrix.description.encode(text_encoding),
                "source": "Emme"}
            omx_matrix = omx_file.createMatrix(
                key, obj=numpy_array, chunkshape=chunkshape, attrs=attrs)
        try:
            omx_file.createMapping('zone_number', scenario.zone_numbers)
        except LookupError:
            pass

    finally:
        omx_file.close()
    return
