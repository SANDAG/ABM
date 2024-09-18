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

from math import ceil
import traceback as _traceback
import re as _re
import json as _json
import time as _time
import os
import numpy as _numpy
import datetime
import pandas as pd
import uuid
import yaml


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
            if isinstance(selections, str):
                selections = {"link": selections}
            spec["selections"] = selections
        else:
            spec["selections"] = {"link": "all"}
        return self._network_calc(spec, self._scenario)


class AvailableNodeIDTracker(object):
    def __init__(self, network, start=999999):
        self._network = network
        self._node_id = start

    def get_id(self):
        while self._network.node(self._node_id):
            self._node_id -= 1
        return self._node_id


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
    for elem_type, attributes in backup_attributes.items():
        backup[elem_type] = scenario.get_attribute_values(elem_type, attributes)
    try:
        yield
    finally:
        for elem_type, attributes in backup_attributes.items():
            scenario.set_attribute_values(elem_type, attributes, backup[elem_type])


class DataTableProc(object):

    def __init__(self, table_name, path=None, data=None, convert_numeric=False):
        modeller = _m.Modeller()
        desktop = modeller.desktop
        project = desktop.project
        self._dt_db = dt_db = project.data_tables()
        self._convert_numeric = convert_numeric
        if path:
            try:
                source = _dt.DataSource(path)
            except _dt.Error as error:
                raise Exception("Cannot open file at %s" % path)
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
                if geo_obj.GetGeometryName() == 'MULTILINESTRING':
                    coords = []
                    for line in geo_obj:
                        coords.extend(line.GetPoints())
                else:
                    coords = geo_obj.GetPoints()
                coords = [point[:2] for point in coords]
                geo_coords.append(coords)
            self._values.append(geo_coords)
            self._attr_names.append("geo_coordinates")

    def __iter__(self):
        values, attr_names = self._values, self._attr_names
        return (dict(zip(attr_names, record))
                for record in zip(*values))

    def save(self, name, overwrite=False):
        self._dt_db.create_table(name, self._data, overwrite=overwrite)

    def values(self, name):
        index = self._index[name]
        return self._values[index]


class E00FileProc:
    def __init__(self, table_name, file_path):
        self._file_path = file_path
        self._table_name = table_name
        self._values = []
        self._attr_names = []
        self._attr_widths = []
        self._attr_cast = []
        self._read_file()

    def _read_file(self):
        with open(self._file_path, 'r') as f:
            for line in f:
                if line.startswith(self._table_name):
                    break
            header = self._parse_header(line)
            for i in range(header["valid_attributes"]):
                attribute = self._parse_attribute(next(f))
                self._attr_names.append(attribute["name"])
                self._attr_widths.append(self._get_attr_width(attribute))
                self._attr_cast.append(self._get_attr_caster(attribute))
            self._parse_records(f, int(header["num_records"]))

    def _parse_header(self, text):
        file_name, external_flag, num_valid, num_total, record_length, num_records = \
            self._parse_fields(text, [32, 2, 4, 4, 4, 10])
        header = {
            "file_name": file_name,
            "external_flag": external_flag,
            "valid_attributes": int(num_valid),
            "total_attributes": int(num_total),
            "record_length": int(record_length),
            "num_records": int(num_records)
        }
        return header

    def _parse_attribute(self, text):
        name, size, _, start, _, _, fmt_width, fmt_precision, type_id, _, _, _, _, alt_name, index = \
            self._parse_fields(text, [16, 3, 2, 4, 1, 2, 4, 2, 3, 2, 4, 4, 2, 16, 5])
        attribute = {
            "name": name,
            "size": int(size),
            "start": int(start),
            "fmt_width": int(fmt_width),
            "fmt_precision": int(fmt_precision),
            "type": type_id,
            "alt_name": alt_name,
            "index": index
        }
        return attribute

    def _parse_fields(self, text, widths):
        tokens = []
        index = 0
        for width in widths:
            tokens.append(text[index:width+index].strip())
            index += width
        return tokens

    def _get_attr_width(self, attribute):
        a_type, size = attribute["type"], attribute["size"]
        if a_type == "10":    # "10": "date"
            return 8
        elif a_type == "20" or a_type == "30":
            # "20": "string" or "30": "fixed_integer"
            return size
        elif a_type == "40":  # "40": "single_float"
            return 14
        elif a_type == "50":  # "50": "integer"
            if size == 2:  # 2 bytes = 6 characters
                return 6
            elif size == 4:  # 4 bytes = 11 characters
                return 11
        elif a_type == "60":  # "60": "float"
            if size == 2:  # 2 bytes = 14 characters
                return 14
            elif size == 4:  # 4 bytes = 24 characters
                return 24

    def _get_attr_caster(self, attribute):
        str_strip = lambda x: x.strip()
        mapper = {
            "10": str_strip,  # "10": "date"
            "20": str_strip,  # "20": "string"
            "30": int,        # "30": "fixed_integer"
            "40": float,      # "40": "single_float"
            "50": int,        # "50": "integer"
            "60": float,      # "60": "float"
        }
        return mapper[attribute["type"]]

    def _parse_records(self, reader, num_records):
        num_lines = int(ceil(sum(self._attr_widths) / 80.0))
        indices = []
        index = 0
        for width, cast in zip(self._attr_widths, self._attr_cast):
            indices.append((index, width+index, cast))
            index += width
        for j in range(num_records):
            lines = []
            for j in range(num_lines):
                lines.append(next(reader).rstrip().ljust(80, " "))
            line = "".join(lines)
            self._values.append(
                [cast(line[start:stop]) for start, stop, cast in indices])

    def __iter__(self):
        values, attr_names = self._values, self._attr_names
        return (dict(zip(attr_names, record)) for record in values)

    def values(self, name):
        index = self._attr_names.index(name)
        return (v[index] for v in self._values)


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
                snapshot[name] = str(self[name])
            return _json.dumps(snapshot)
        except Exception:
            return "{}"

    def from_snapshot(self, snapshot):
        try:
            snapshot = _json.loads(snapshot)
            attributes = getattr(self, "attributes", [])
            for name in attributes:
                self[name] = snapshot[name]
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error), False)
        return self

    def get_state(self):
        attributes = getattr(self, "attributes", [])
        state = {}
        for name in attributes:
            try:
                state[name] = self[name]
            except _m.AttributeError as error:
                state[name] = str(error)
        return state


def log_snapshot(name, namespace, snapshot):
    try:
        _m.logbook_snapshot(name=name, comment="", namespace=namespace,
                            value=_json.dumps(snapshot))
    except Exception as error:
        print (error)


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
        if omx_key == "ID_NAME":
            self.generate_key = lambda m: "%s_%s" % (
                m.id, m.name)
        elif omx_key == "NAME":
            self.generate_key = lambda m: m.name
        elif omx_key == "ID":
            self.generate_key = lambda m: m.id

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
            for key, matrix in matrices.items():
                self.write_matrix(matrix, key)
        else:
            for matrix in matrices:
                self.write_matrix(matrix)

    def write_matrix(self, matrix, key=None):
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
        attrs = {"description": matrix.description}
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
        # with _m.logbook_trace("file_name: %s" % (file_name)):
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
        # with _m.logbook_trace("file_name and mapping name: %s and %s" % (file_name, mapping_name)):
        zone_mapping = list(omx_file.mapping(mapping_name).items())
        zone_mapping.sort(key=lambda x: x[1])
        omx_zones = [x[0] for x in zone_mapping]
        return omx_zones

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        for omx_file in list(self._omx_files.values()):
            omx_file.close()
        self._omx_files = {}


class CSVReader(object):
    def __init__(self, path):
        self._path = path
        self._f = None
        self._fields = None

    def __enter__(self):
        self._f = open(self._path)
        header = next(self._f)
        self._fields = [h.strip().upper() for h in header.split(",")]
        return self

    def __exit__(self, exception_type, exception_value, traceback):
        self._f.close()
        self._f = None
        self._fields = None

    def __iter__(self):
        return self

    @property
    def fields(self):
        return list(self._fields)

    def __next__(self):
        line = next(self._f)
        tokens = [t.strip() for t in line.split(",")]
        return dict(list(zip(self._fields, tokens)))

# class DataLakeExporter(object):
#     """
#     _________________


#     """
#     def __init__(self
#                  ,ScenarioPath = os.path.dirname(_m.Modeller().desktop.project.path)
#                  ,container = None
#                  ,timestamp = datetime.datetime.now()):
#         self.ScenarioPath = ScenarioPath
#         self.ReportPath = os.path.join(self.ScenarioPath, "report")
#         self.container = container
#         self.timestamp = timestamp

#     def get_datalake_connection(self):
#         """
#         ________


#         Check if Azure Storage SAS Token is properly configured in local machine's environment.
#             Return Azure ContainerClient and boolean indicating cloud connection was made successfully.
#             If it is not, pass argument back to write_to_datalake to only write outputs locally.
#         """
#         try:
#             from azure.storage.blob import ContainerClient
#             from azure.core.exceptions import ServiceRequestError
#             sas_url = os.environ["AZURE_STORAGE_SAS_TOKEN"]
#             container = ContainerClient.from_container_url(sas_url)
#             _m.logbook_write("Successfully created Azure container")
#             return container
#         except ImportError as e:
#             _m.logbook_write("Failed to create Azure container - unable to import azure module")
#             return None
#         except KeyError as e:
#             _m.logbook_write("Failed to create Azure container - No SAS_Token in environment")
#             return None
#         except ServiceRequestError as e:
#             _m.logbook_write("Failed to create Azure container - SAS_Token in environment likely malconfigured")
#             return None

#     def get_scenario_metadata(self):
#         """
#         get scenario's guid (globally unique identifier)
#         if guid not in scenario's output directory (generated by EMME's master_run.py),
#             then generate a model-specific guid
#         """
#         datalake_metadata_path = os.path.join(self.ScenarioPath,"output", "datalake_metadata.yaml")
#         with open(datalake_metadata_path, "r") as stream:
#             metadata = yaml.safe_load(stream)
#         self.guid = metadata["scenario_guid"]
#         self.username = metadata["username"]
#         self.scenario_title = metadata["scenario_title"]

#     def export_data(self, output_table_dict):
#         timestamp_str = self.timestamp.strftime("%Y%m%d_%H%M%S")
#         for tablename,table in output_table_dict.items():
#             parent_dir_name = str(self.scenario_title) + "__" + str(self.username) + "__" + str(self.guid[:5])
#             model_output_file = "_".join([tablename, timestamp_str])+".csv"
#             lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,"report",model_output_file])

#             if isinstance(table, str):
#                 _m.logbook_write("Read csv of %s" % (table))
#                 table = pd.read_csv(table)#os.path.join(self.ReportPath,tablename+'.csv'))
#             table["scenario_ts"] = pd.to_datetime(self.timestamp)
#             table["scenario_guid"] = self.guid
#             table.replace("", None, inplace=True) # replace empty strings with None - otherwise conversation error for boolean types

#             file = table.to_csv(index_label='idx', encoding = "utf-8")

#             t0 = datetime.datetime.now()
#             self.container.upload_blob(name=lake_file_name, data=file)
#             _m.logbook_write("Write to Data Lake: %s took %s to write to Azure" % (tablename, str(datetime.datetime.now()-t0)))

#     def write_to_datalake(self, output_table_dict):
#         #create Azure connection if one is not passed to DataLakeExporter object
#         if not self.container:
#             self.container = self.get_datalake_connection()
#         #export data if Azure container successfully built
#         self.get_scenario_metadata()
#         self.export_data(output_table_dict)