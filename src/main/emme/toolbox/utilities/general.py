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
import inro.emme.core.exception as _except
from osgeo import ogr as _ogr
from contextlib import contextmanager as _context
from itertools import izip as _izip
import traceback as _traceback
import re as _re
import json as _json
import time as _time
import os
import numpy as _numpy

_omx = _m.Modeller().module("sandag.utilities.omxwrapper")


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
            # In case of transient file conflicts and lag in windows file handles over the network
            # attempt to delete file 10 times with increasing delays 0.05, 0.2, 0.45, 0.8 ... 5
            remove_matrix = lambda: emmebank.delete_matrix(matrix)
            retry(remove_matrix)


def retry(fcn, attempts=10, init_wait=0.05, error_types=(RuntimeError, WindowsError)):
    for attempt in range(1, attempts + 1):
        try:
            fcn()
            return
        except error_types:
            if attempt > attempts:
                raise
            _time.sleep(init_wait * (attempt**2))


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


class DataTableProc(object):

    def __init__(self, table_name, path=None, data=None, convert_numeric=False):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        project = desktop.project
        self._dt_db = dt_db = project.data_tables()
        self._convert_numeric = convert_numeric
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
        if self._convert_numeric:
            values = []
            for a in data.attributes():
                attr_values = _numpy.copy(a.values)
                attr_values[attr_values == ''] = 0
                try:
                    values.append(attr_values.astype("int"))
                except ValueError:
                    try:
                        values.append(attr_values.astype("float"))
                    except ValueError:
                        values.append(a.values)
            self._values = values
        else:
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


class ExportOMX(object):
    def __init__(self, file_path, scenario, omx_key="NAME"):
        self.file_path = file_path
        self.scenario = scenario
        self.emmebank = scenario.emmebank
        self.omx_key = omx_key

    @property
    def omx_key(self):
        return self._omx_key

    @omx_key.setter
    def omx_key(self, omx_key):
        self._omx_key = omx_key
        text_encoding = self.emmebank.text_encoding
        if omx_key == "ID_NAME":
            self.generate_key = lambda m: "%s_%s" % (
                m.id.encode(text_encoding), m.name.encode(text_encoding))
        elif omx_key == "NAME":
            self.generate_key = lambda m: m.name.encode(text_encoding)
        elif omx_key == "ID":
            self.generate_key = lambda m: m.id.encode(text_encoding)

    def __enter__(self):
        self.trace = _m.logbook_trace(name="Export matrices to OMX",
            attributes={
                "file_path": self.file_path, "omx_key": self.omx_key,
                "scenario": self.scenario, "emmebank": self.emmebank.path})
        self.trace.__enter__()
        self.omx_file = _omx.open_file(self.file_path, 'w')
        try:
            self.omx_file.create_mapping('zone_number', self.scenario.zone_numbers)
        except LookupError:
            pass
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.omx_file.close()
        self.trace.__exit__(exc_type, exc_val, exc_tb)

    def write_matrices(self, matrices):
        if isinstance(matrices, dict):
            for key, matrix in matrices.iteritems():
                self.write_matrix(matrix, key)
        else:
            for matrix in matrices:
                self.write_matrix(matrix)

    def write_matrix(self, matrix, key=None):
        text_encoding = self.emmebank.text_encoding
        matrix = self.emmebank.matrix(matrix)
        if key is None:
            key = self.generate_key(matrix)
        numpy_array = matrix.get_numpy_data(self.scenario.id)
        if matrix.type == "DESTINATION":
            n_zones = len(numpy_array)
            numpy_array = _numpy.resize(numpy_array, (1, n_zones))
        elif matrix.type == "ORIGIN":
            n_zones = len(numpy_array)
            numpy_array = _numpy.resize(numpy_array, (n_zones, 1))
        attrs = {"description": matrix.description.encode(text_encoding)}
        self.write_array(numpy_array, key, attrs)

    def write_clipped_array(self, numpy_array, key, a_min, a_max=None, attrs={}):
        if a_max is not None:
            numpy_array = numpy_array.clip(a_min, a_max)
        else:
            numpy_array = numpy_array.clip(a_min)
        self.write_array(numpy_array, key, attrs)

    def write_array(self, numpy_array, key, attrs={}):
        shape = numpy_array.shape
        if len(shape) == 2:
            chunkshape = (1, shape[0])
        else:
            chunkshape = None
        attrs["source"] = "Emme"
        numpy_array = numpy_array.astype(dtype="float64", copy=False)
        omx_matrix = self.omx_file.create_matrix(
            key, obj=numpy_array, chunkshape=chunkshape, attrs=attrs)


class OMXManager(object):
    def __init__(self, directory, name_tmplt):
        self._directory = directory
        self._name_tmplt = name_tmplt
        self._omx_files = {}

    def lookup(self, name_args, key):
        file_name = self._name_tmplt % name_args
        omx_file = self._omx_files.get(file_name)
        if omx_file is None:
            file_path = os.path.join(self._directory, file_name)
            omx_file = _omx.open_file(file_path, 'r')
            self._omx_files[file_name] = omx_file
        return omx_file[key].read()

    def file_exists(self, name_args):
        file_name = self._name_tmplt % name_args
        file_path = os.path.join(self._directory, file_name)
        return os.path.isfile(file_path)

    def zone_list(self, file_name):
        omx_file = self._omx_files[file_name]
        mapping_name = omx_file.list_mappings()[0]
        zone_mapping = omx_file.mapping(mapping_name).items()
        zone_mapping.sort(key=lambda x: x[1])
        omx_zones = [x[0] for x in zone_mapping]
        return omx_zones

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        for omx_file in self._omx_files.values():
            omx_file.close()
        self._omx_files = {}


class CSVReader(object):
    def __init__(self, path):
        self._path = path
        self._f = None
        self._fields = None

    def __enter__(self):
        self._f = open(self._path)
        header = self._f.next()
        self._fields = [h.strip().upper() for h in header.split(",")]
        return self

    def __exit__(self, exception_type, exception_value, traceback):
        self._f.close()
        self._f = None
        self._fields = None

    def __iter__(self):
        return self

    def next(self):
        line = self._f.next()
        tokens = [t.strip() for t in line.split(",")]
        return dict(zip(self._fields, tokens))
