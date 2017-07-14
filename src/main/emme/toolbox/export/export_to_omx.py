#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// INRO.Modeller.emme                                                    ///
#//// export_to_omx                                                         ///
#////                                                                       ///
#////                                                                       ///
#//// Copyright 2017 INRO                                                   ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////

import os
import traceback as _traceback
import inro.modeller as _m
import inro.emme.core.exception as _except
import numpy as _numpy
import omx as _omx

_NAME = _m.translate("ExportToOMX", "Export matrices to OMX")

validate = _m.Modeller().module("inro.emme.utility.validate").Validate()
log_utils = _m.Modeller().module("inro.emme.utility.logbook_utilities")
_s = _m.Modeller().module("inro.emme.utility.snapshot_support")

_DEFAULT_MATRIX_TYPE = "FULL"
_DEFAULT_APPEND_TO_FILE = False
_DEFAULT_OMX_KEY = "ID_NAME"


class ExportMatricesToOMX(_m.Tool(), _s.SnapshotTool):
    matrix_type = _m.Attribute(unicode)

    matrices_full = _m.Attribute(unicode)
    matrices_dest = _m.Attribute(unicode)
    matrices_orig = _m.Attribute(unicode)

    export_file = _s.String()
    append_to_file = _s.Boolean(_DEFAULT_APPEND_TO_FILE)
    omx_key = _s.String(default=_DEFAULT_OMX_KEY)

    percent = 0
    tool_run_msg = ""

    def __init__(self):
        self.append_to_file = _DEFAULT_APPEND_TO_FILE
        self.matrix_type = _DEFAULT_MATRIX_TYPE
        self.omx_key = _DEFAULT_OMX_KEY
        self._snapshot_load_order = [
            "matrix_type", "matrices", "export_file", 
            "append_to_file", "omx_key"]

    @_s.CustomSerializer
    def matrices(self):
        serializer = _s.MatrixIDList()
        serializer.name = "matrices"
        if self.matrix_type == "FULL":
            return serializer.serialize(self.matrices_full)
        elif self.matrix_type == "ORIGIN":
            return serializer.serialize(self.matrices_orig)
        elif self.matrix_type == "DESTINATION":
            return serializer.serialize(self.matrices_dest)
        return []

    @matrices.setter
    def matrices(self, value):
        scenario = self.current_scenario
        try:
            first_matrix = scenario.emmebank.matrix(value[0])
            if first_matrix:
                self.matrix_type = first_matrix.type
        except IndexError:
            pass
        serializer = _s.MatrixIDList(allowed=[self.matrix_type])
        serializer.name = "matrices"
        if self.matrix_type == "FULL":
            self.matrices_full = serializer.deserialize(value, scenario)
        elif self.matrix_type == "ORIGIN":
            self.matrices_orig = serializer.deserialize(value, scenario)
        elif self.matrix_type == "DESTINATION":
            self.matrices_dest = serializer.deserialize(value, scenario)

    def page(self):
        try:
            if self.export_file is None:
                self.export_file = self.get_default_export_path()
        except _m.AttributeError:
            pass
        pb = _m.ToolPageBuilder(
            self,
            title=_NAME,
            help_path="qthelp://com.inro.emme.modeller_man/doc/html"
                      "/modeller_man/export_matrices_to_omx_tool.html",
            description=_m.translate(
                "ExportToOMX",
                "Export selected matrices to open matrix file format (OMX) "
                "using the zone system from the current scenario."),
            branding_text=_m.translate(
                "ExportToOMX", "- Emme Standard - Data management - Matrix"))
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_radio_group(
            "matrix_type",
            keyvalues=[
                ("FULL",
                 _m.translate("ExportToOMX", "Full")),
                ("ORIGIN",
                 _m.translate("ExportToOMX", "Origin")),
                ("DESTINATION",
                 _m.translate("ExportToOMX", "Destination"))],
            title=_m.translate("ExportToOMX", "Matrix type:"))

        pb.add_select_matrix(
            "matrices_full",
            title=_m.translate("ExportToOMX",
                               "Full matrices to export:"),
            filter=["FULL"],
            id=True,
            multiple=True)

        pb.add_select_matrix(
            "matrices_orig",
            title=_m.translate("ExportToOMX",
                               "Origin matrices to export:"),
            filter=["ORIGIN"],
            id=True,
            multiple=True)

        pb.add_select_matrix(
            "matrices_dest",
            title=_m.translate("ExportToOMX",
                               "Destination matrices to export:"),
            filter=["DESTINATION"],
            id=True,
            multiple=True)

        start_path = self.get_default_export_path()
        try:
            if not self.export_file:
                self.export_file = start_path
            else:
                start_path = self.export_file
        except _m.AttributeError:
            pass

        pb.add_select_file(
            "export_file",
            window_type="save_file",
            file_filter="*.omx",
            start_path=start_path,
            title=_m.translate("ExportToOMX", "Export file:"),
            note=_m.translate("ExportToOMX",
                              "Default is &lt;database_directory&gt;/matrices.omx."))

        pb.add_checkbox(
            "append_to_file",
            title=" ",
            label=_m.translate("ExportToOMX",
                               "Append to the end of the file"),
            note=_m.translate("ExportToOMX",
                              "If False, contents will be overwritten "
                              "if the file already exists."))
        pb.add_radio_group(
            "omx_key",
            keyvalues=[
                ("ID_NAME", _m.translate("ExportToOMX", "As ID_NAME (e.g. 'mf1_auto_demand')")),
                ("NAME", _m.translate("ExportToOMX", "As NAME (e.g. 'auto_demand')")),
                ("ID", _m.translate("ExportToOMX", "As ID (e.g. 'mf1')"))],
            title=_m.translate("ExportToOMX", "Matrix key in OMX file:"))

        pb.add_html("""
<script>
    $(document).ready( function ()
    {
        var tool = new inro.modeller.util.Proxy(%s) ;

        show_hide = function()
        {
            $("#matrices_full").closest("div.t_element")
                .toggle(tool.matrix_type == "FULL");
            $("#matrices_orig").closest("div.t_element")
                .toggle(tool.matrix_type == "ORIGIN");
            $("#matrices_dest").closest("div.t_element")
                .toggle(tool.matrix_type == "DESTINATION");
        };

        $("input:radio[name='matrix_type']").on("change", function() {
            $(this).commit();
            show_hide();
        });

        $(this).commit();
        show_hide();
    });
</script>""" % pb.tool_proxy_tag)

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.matrices, self.export_file, self.append_to_file, 
                 omx_key=self.omx_key)
            matrices = _s.json_loads_no_raise(self.matrices)
            if len(matrices) == 1:
                msg = _m.translate("ExportToOMX",
                                   "Matrix %s exported.") % matrices[0]
            else:
                msg = _m.translate(
                    "ExportToOMX",
                    "Matrices %s exported.") % ", ".join(matrices)
            self.tool_run_msg = _m.PageBuilder.format_info(msg, escape=False)
        except Exception, e:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                e, _traceback.format_exc(e))
            raise

    @_m.logbook_trace(name=_NAME, save_arguments=True)
    def __call__(self, matrices, export_file="", append_to_file=False,
                 scenario=None, omx_key="ID_NAME"):

        if not scenario:
            scenario = self.current_scenario

        self.log_call_snapshot(_NAME, locals())

        scenario = validate.scenario(scenario, writable=False)
        validate.scenario_has(scenario, 'centroids')
        emmebank = scenario.emmebank

        matrices = validate.list_of_matrices(
            matrices, emmebank, types=["FULL", "ORIGIN", "DESTINATION"])
        self._validate_matrix_types(matrices)

        self.__call__.logbook_cursor.write(
            attributes={"emmebank": emmebank.path})

        if not export_file:
            export_path = os.path.dirname(emmebank.path)
            file_name = "matrices.omx"
            export_file = os.path.join(export_path, file_name)

        export_file = validate.file_path(export_file, "export_file")
        append_to_file = validate.Boolean(append_to_file, "append_to_file")

        try:
            if append_to_file:
                omx_file = _omx.openFile(export_file, 'a')
            else:
                omx_file = _omx.openFile(export_file, 'w')
        except:
            message = _m.translate("ExportToOMX",
                                   "Could not open file: %s" % export_file)
            raise _except.ArgumentError(message)

        omx_key = validate.string_constant(
            omx_key, "omx_key", ("ID_NAME", "NAME", "ID"))
        text_encoding = emmebank.text_encoding
        if omx_key == "ID_NAME":
            generate_key = lambda m: "%s_%s" % (
                m.id.encode(text_encoding), m.name.encode(text_encoding))
        elif omx_key == "NAME":
            generate_key = lambda m: m.name.encode(text_encoding)
        elif omx_key == "ID":
            generate_key = lambda m: m.id.encode(text_encoding)

        try:
            if append_to_file and len(omx_file) > 0:
                self._validate_for_append(
                    matrices, scenario, omx_file, emmebank)
            n_matrices = len(matrices)
            for matrix in matrices:
                numpy_array = matrix.get_numpy_data(scenario.id)
                if matrix.type == "DESTINATION":  # (1,n_zones)
                    n_zones = len(scenario.zone_numbers)
                    numpy_array = _numpy.resize(numpy_array, (1, n_zones))
                if matrix.type == "ORIGIN":
                    n_zones = len(scenario.zone_numbers)
                    numpy_array = _numpy.resize(numpy_array, (n_zones, 1))
                key = generate_key(matrix)
                omx_file[key] = numpy_array.astype(dtype="float64")
                omx_file[key].attrs.description = \
                    matrix.description.encode(text_encoding)
                omx_file[key].attrs.source = "Emme"
                self.percent += 100 / n_matrices

            # Now create the zone mappings, if they don't currently exist
            try:
                omx_file.createMapping('zone_number', scenario.zone_numbers)
            except LookupError:
                pass

        finally:
            omx_file.close()
            log_utils.log_link_report(_NAME, export_file)
        return

    @staticmethod
    def _validate_matrix_types(matrices):
        # Check that all matrices are the same type
        m = matrices[0]
        matrix_type = m.type
        for m in matrices:
            if m.type != matrix_type:
                message = _m.translate(
                    "ExportToOMX",
                    "All matrices must have the same type.")
                raise _except.ArgumentError(message)

    @staticmethod
    def _validate_for_append(
            matrices, scenario, omx_file, emmebank):
        n_zones = len(scenario.zone_numbers)
        m = matrices[0]
        matrix_type = m.type
        if matrix_type == "FULL":
            mshape = (n_zones, n_zones)
        elif matrix_type == "ORIGIN":
            mshape = (n_zones, 1)
        else:  # destinations
            mshape = (1, n_zones)

        # Validate that matrix shape matches that of OMX file
        shape = omx_file.shape()
        if shape[0] != mshape[0] or shape[1] != mshape[1]:
            message1 = _m.translate(
                "ExportToOMX",
                "Could not append matrix to file. "
                "Incompatible matrix shapes.")
            message2 = _m.translate(
                "ExportToOMX",
                " File requires shape (%d, %d). " % (shape[0], shape[1]))
            message3 = _m.translate(
                "ExportToOMX",
                "Matrix has shape (%d, %d)." % (mshape[0], mshape[1]))
            message = message1 + message2 + message3
            raise _except.Error(message)

        # add a validation to see if matrix with the same name is in the file
        text_encoding = emmebank.text_encoding
        for matrix in matrices:
            my_name_id = "%s_%s" % (matrix.id.encode(text_encoding),
                                    matrix.name.encode(text_encoding))
            for omx_matrix in omx_file.listMatrices():
                if omx_matrix == my_name_id:
                    message = _m.translate(
                        "ExportToOMX",
                        "A matrix named '%s' already exists in the OMX file." %
                        my_name_id)
                    raise _except.Error(message)

    def get_default_export_path(self):
        return os.path.join(
            os.path.dirname(self.current_scenario.emmebank.path),
            'matrices.omx')

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
