#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2018.                                                 ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// utilities/file_manager.py                                             ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
TOOLBOX_ORDER = 104


import inro.modeller as _m
import inro.emme.database.emmebank as _eb
import inro.director.logging as _log

import traceback as _traceback
import shutil as _shutil
import time as _time
import os
# from fnmatch import fnmatch as _fnmatch
from math import log10
import subprocess as _subprocess

_join = os.path.join
_dir = os.path.dirname
_norm = os.path.normpath

gen_utils = _m.Modeller().module("sandag.utilities.general")


class FileManagerTool(_m.Tool(), gen_utils.Snapshot):

    operation = _m.Attribute(str)
    remote_dir = _m.Attribute(str)
    local_dir = _m.Attribute(str)
    user_folder = _m.Attribute(str)
    scenario_id = _m.Attribute(str)
    initialize = _m.Attribute(bool)
    delete_local_files = _m.Attribute(bool)

    tool_run_msg = ""
    LOCAL_ROOT = "C:\\abm_runs"

    def __init__(self):
        self.operation = "UPLOAD"
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.remote_dir = _dir(project_dir)
        folder_name = os.path.basename(self.remote_dir)
        self.user_folder = os.environ.get("USERNAME")
        self.scenario_id = 100
        self.initialize = True
        self.delete_local_files = True
        self.attributes = [
            "operation", "remote_dir", "local_dir", "user_folder",
            "scenario_id", "initialize", "delete_local_files"
        ]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "File run manager utility"
        pb.description = """
    <p align="left">
        Utility tool to manually manage the use of the local drive for subsequent model run.
        The remote data can be downloaded (copied) to the local drive;
        or the local data can be uploaded to the remote drive.
        In normal operation this tool does not need to run manually, but in case of an
        error it may be necessary to upload the project data in order to run on
        a different machine, or operate directly on the server.
    </p>
    <p align="left">
    Note that file masks are used from config/sandag_abm.properties to identify which
    files to copy. See RunModel.FileMask.Upload and RunModel.FileMask.Download for
    upload and download respectively.
    </p>"""
        pb.branding_text = "- SANDAG"
        if self.tool_run_msg:
            pb.add_html(self.tool_run_msg)

        pb.add_radio_group('operation', title="File copy operation",
            keyvalues=[("UPLOAD", "Upload from local directory to remote directory"),
                       ("DOWNLOAD", "Download from remote directory to local directory")], )
        pb.add_select_file('remote_dir','directory',
                           title='Select remote ABM directory (e.g. on T drive)', note='')
        pb.add_text_box('user_folder', title="User folder (for local drive):")
        pb.add_text_box('scenario_id', title="Base scenario ID:")
        pb.add_checkbox_group(
            [{"attribute": "delete_local_files", "label": "Delete all local files on completion (upload only)"},
             {"attribute": "initialize", "label": "Initialize all local files; if false only download files which are different (download only)"}])
        pb.add_html("""
<script>
    $(document).ready( function ()
    {
        $("input:radio[name='operation']").bind('change', function()    {
            var value = $("input:radio[name='operation']:checked").val();
            $("#delete_local_files").prop('disabled', value == "DOWNLOAD");
            $("#initialize").prop('disabled', value == "UPLOAD");
        }).trigger('change');
   });
</script>""")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            self(self.operation, self.remote_dir, self.user_folder, self.scenario_id,
                 self.initialize, self.delete_local_files)
            run_msg = "File copying complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, operation, remote_dir, user_folder, scenario_id, initialize=True, delete_local_files=True):
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(remote_dir, "conf", "sandag_abm.properties"))
        if operation == "DOWNLOAD":
            file_masks = props.get("RunModel.FileMask.Download")
            return self.download(remote_dir, user_folder, scenario_id, initialize, file_masks)
        elif operation == "UPLOAD":
            file_masks = props.get("RunModel.FileMask.Upload")
            self.upload(remote_dir, user_folder, scenario_id, delete_local_files, file_masks)
        else:
            raise Exception("operation must be one of UPLOAD or DOWNLOAD")

    @_m.logbook_trace("Copy project data to local drive", save_arguments=True)
    def download(self, remote_dir, user_folder, scenario_id, initialize, file_masks):
        folder_name = os.path.basename(remote_dir)
        user_folder = user_folder or os.environ.get("USERNAME")
        if not user_folder:
            raise Exception("Username must be specified for local drive operation "
                            "(or define USERNAME environment variable)")
        if not os.path.exists(self.LOCAL_ROOT):
            os.mkdir(self.LOCAL_ROOT)
        user_directory = _join(self.LOCAL_ROOT, user_folder)
        if not os.path.exists(user_directory):
            os.mkdir(user_directory)
        local_dir = _join(user_directory, folder_name)
        if not os.path.exists(local_dir):
            os.mkdir(local_dir)

        self._report = ["Copy"]
        self._stats = {"size": 0, "count": 0}
        if not file_masks:
            # suggested default: "output", "report", "sql", "logFiles"
            file_masks = []
        file_masks = [_join(remote_dir, p) for p in file_masks]
        file_masks.append(_join(remote_dir, "emme_project"))
        if initialize:
            # make sure that all of the root directories are created
            root_dirs = [
                "application", "bin", "conf", "emme_project", "input",
                "logFiles", "output", "python", "report", "sql", "uec"
            ]
            for name in root_dirs:
                if not os.path.exists(_join(local_dir, name)):
                    os.mkdir(_join(local_dir, name))
            # create new Emmebanks with scenario and matrix data
            title_fcn = lambda t: "(local) " + t[:50]
            emmebank_paths = self._copy_emme_data(
                src=remote_dir, dst=local_dir, initialize=True,
                title_fcn=title_fcn, scenario_id=scenario_id)
            # add new emmebanks to the open project
            # db_paths = set([db.core_emmebank.path for db in data_explorer.databases()])
            # for path in emmebank_paths:
                # if path not in db_paths:
                    # _m.Modeller().desktop.data_explorer().add_database(path)

        # copy all files (except Emme project, and other file_masks)
        self._copy_dir(src=remote_dir, dst=local_dir,
                       file_masks=file_masks, check_metadata=not initialize)
        self.log_report()
        return local_dir

    @_m.logbook_trace("Copy project data to remote drive", save_arguments=True)
    def upload(self, remote_dir, user_folder, scenario_id, delete_local_files, file_masks):
        folder_name = os.path.basename(remote_dir)
        user_folder = user_folder or os.environ.get("USERNAME")
        user_directory = _join(self.LOCAL_ROOT, user_folder)
        local_dir = _join(user_directory, folder_name)

        self._report = []
        self._stats = {"size": 0, "count": 0}
        if not file_masks:
            file_masks = []
        # prepend the src dir to the project masks
        file_masks = [_join(local_dir, p) for p in file_masks]
        # add to mask the emme_project folder
        file_masks.append(_join(local_dir, "emme_project"))

        title_fcn = lambda t: t[8:] if t.startswith("(local)") else t
        # copy all files (except Emme project, and other file_masks)
        self._copy_dir(src=local_dir, dst=remote_dir, file_masks=file_masks)
        emmebank_paths = self._copy_emme_data(
            src=local_dir, dst=remote_dir, title_fcn=title_fcn, scenario_id=scenario_id)
        
        
        self.log_report()

        # data_explorer = _m.Modeller().desktop.data_explorer()
        # for path in emmebank_paths:
            # for db in data_explorer.databases():
                # if db.core_emmebank.path == path:
                    # db.close()
                    # data_explorer.remove_database(db)
        # data_explorer.databases()[0].open()

        if delete_local_files:
            # small pause for file handles to close
            _time.sleep(2)
            for name in os.listdir(local_dir):
                path = os.path.join(local_dir, name)
                if os.path.isfile(path):
                    try:  # no raise, local files can be left behind
                        os.remove(path)
                    except:
                        pass
                elif os.path.isdir(path):
                    _shutil.rmtree(path, ignore_errors=True)

    def _copy_emme_data(self, src, dst, title_fcn, scenario_id, initialize=False):
        # copy data from Database and Database_transit using API and import tool
        # create new emmebanks and copy emmebank data to local drive
        import_from_db = _m.Modeller().tool("inro.emme.data.database.import_from_database")
        emmebank_paths = []
        periods = ["EA", "AM", "MD", "PM", "EV"]
        dbs = ["Database"]
        for period in periods:
            dbs.append("Database_transit_" + period)
        for db_dir in dbs:
            src_db_path = _join(src, "emme_project", db_dir, "emmebank")
            if not os.path.exists(src_db_path):
                # skip if the database does not exist (will be created later)
                continue
            src_db = _eb.Emmebank(src_db_path)
            dst_db_dir = _join(dst, "emme_project", db_dir)
            dst_db_path = _join(dst_db_dir, "emmebank")
            emmebank_paths.append(dst_db_path)
            self._report.append("Copying Emme data <br>from %s<br> to %s" % (src_db_path, dst_db_path))
            self._report.append("Start: %s" % _time.strftime("%c"))
            if initialize:
                # remove any existing database (overwrite)
                if os.path.exists(dst_db_path):
                    self._report.append("Warning: overwritting existing Emme database %s" % dst_db_path)
                    dst_db = _eb.Emmebank(dst_db_path)
                    dst_db.dispose()
                if os.path.exists(dst_db_dir):
                    gen_utils.retry(lambda: _shutil.rmtree(dst_db_dir))
                gen_utils.retry(lambda: os.mkdir(dst_db_dir))
                dst_db = _eb.create(dst_db_path, src_db.dimensions)
            else:
                if not os.path.exists(dst_db_dir):
                    os.mkdir(dst_db_dir)
                if os.path.exists(dst_db_path):
                    dst_db = _eb.Emmebank(dst_db_path)
                else:
                    dst_db = _eb.create(dst_db_path, src_db.dimensions)

            dst_db.title = title_fcn(src_db.title)
            for prop in ["coord_unit_length", "unit_of_length", "unit_of_cost",
                         "unit_of_energy", "use_engineering_notation", "node_number_digits"]:
                setattr(dst_db, prop, getattr(src_db, prop))

            if initialize:
                src_db.dispose()
                continue

            exfpars = [p for p in dir(src_db.extra_function_parameters) if p.startswith("e")]
            for exfpar in exfpars:
                value = getattr(src_db.extra_function_parameters, exfpar)
                setattr(dst_db.extra_function_parameters, exfpar, value)

            for s in src_db.scenarios():
                if dst_db.scenario(s.id):
                    dst_db.delete_scenario(s)
            for f in src_db.functions():
                if dst_db.function(f.id):
                    dst_db.delete_function(f)
            for m in src_db.matrices():
                if dst_db.matrix(m.id):
                    dst_db.delete_matrix(m)
            for p in dst_db.partitions():
                p.description = ""
                p.initialize(0)
            ref_scen = dst_db.scenario(999)
            if not ref_scen:
                ref_scen = dst_db.create_scenario(999)
            import_from_db(
                src_database=src_db,
                src_scenario_ids=[s.id for s in src_db.scenarios()],
                src_function_ids=[f.id for f in src_db.functions()],
                copy_path_strat_files=True,
                dst_database=dst_db,
                dst_zone_system_scenario=ref_scen)
            dst_db.delete_scenario(999)
            src_matrices = [m.id for m in src_db.matrices()]
            src_partitions = [p.id for p in src_db.partitions()
                if not(p.description == '' and not (sum(p.raw_data)))]
            if src_matrices or src_partitions:
                import_from_db(
                    src_database=src_db,
                    src_zone_system_scenario=src_db.scenario(scenario_id),
                    src_matrix_ids=src_matrices,
                    src_partition_ids=src_partitions,
                    dst_database=dst_db,
                    dst_zone_system_scenario=dst_db.scenario(scenario_id))
            src_db.dispose()
            self._report.append("End: %s" % _time.strftime("%c"))
        return emmebank_paths

    def _copy_dir(self, src, dst, file_masks, check_metadata=False):
        
        # windows xcopy is much faster than shutil
        # upgrading xcopy to a faster robocopy
        self._report.append(_time.strftime("%c"))
        exclude_filename = "TEMP_file_manager_exclude.txt"
        exclude_file = open(exclude_filename, "a+")
        for x in file_masks + [exclude_filename]:
            exclude_file.write(x + '\n')
        exclude_file.close()
        # if check_metadata:
        #     flags = '/Y/S/E/D'
        # else:
        #     flags = '/Y/S/E'
        try:
            exclude_file_list = file_masks + [exclude_filename]
            flags = ['/E', '/Z', '/MT:8']
            output = _subprocess.check_output(['robocopy', src, dst] + flags + ["/XF"] + exclude_file_list + ["/XD"] + exclude_file_list)
            self._report.append(output)
        except _subprocess.CalledProcessError as error:
            self._report.append(error.output)
            self.log_report()
            if (error.returncode < 16) and (error.returncode > 0):
                self._report.append(error.output)
            else:
                raise
        os.remove(exclude_filename)
        self._report.append(_time.strftime("%c"))

        # for name in os.listdir(src):
        #     src_path = _join(src, name)
        #     skip_file = bool([1 for mask in file_masks if _fnmatch(src_path, mask)])
        #     if skip_file:
        #         continue
        #     dst_path = _join(dst, name)
        #     if os.path.isfile(src_path):
        #         size = os.path.getsize(src_path)
        #         if check_metadata and os.path.exists(dst_path):
        #             same_size = os.path.getsize(dst_path) == size
        #             same_time = os.path.getmtime(dst_path) == os.path.getmtime(src_path)
        #             if same_size and same_time:
        #                 continue
        #         self._report.append(_time.strftime("%c"))
        #         self._report.append(dst_path + file_size(size))
        #         self._stats["size"] += size
        #         self._stats["count"] += 1
        #         # shutil.copy2 performs 5-10 times faster on download, and ~20% faster on upload
        #         # than os.system copy calls
        #         src_time = os.path.getmtime(src_path)
        #         if name == 'persons.csv':
        #             src_time = os.path.getmtime(src_path)
        #             if os.path.exists(dst_path):
        #                 dest_time = os.path.getmtime(dst_path)
        #                 if dest_time <= src_time:
        #                     _shutil.copy2(src_path, dst_path)
        #                     print "dest_time <= ori_time, copied, dst_path", dst_path
        #                 else:
        #                     print "dest_time > ori_time, not copied, dst_path", dst_path
        #             else:
        #                 _shutil.copy2(src_path, dst_path)
        #                 print "dest file not exist, copied"
        #         else:
        #             _shutil.copy2(src_path, dst_path)
        #         self._report.append(_time.strftime("%c"))
        #     elif os.path.isdir(src_path):
        #         if not os.path.exists(dst_path):
        #             os.mkdir(dst_path)
        #         self._report.append(dst_path)
        #         self._copy_dir(src_path, dst_path, file_masks, check_metadata)

    def log_report(self):
        # size, count = file_size(self._stats["size"]), self._stats["count"]
        # name = "File copy report: copied {count} files {size}".format(count=count, size=size)
        name = "File copy report"
        report = _m.PageBuilder(title=name)
        def clean(item):
            if isinstance(item, bytes):
                item = item.decode(errors="replace")
            return item.replace("\r\n", "<br>").replace("\n", "<br>")
        report.add_html("<br>".join(clean(item) for item in self._report))
        _m.logbook_write(name, report.render())


_suffixes = ['bytes', 'KiB', 'MiB', 'GiB', 'TiB']

def file_size(size):
    order = int(log10(size) / 3) if size else 0
    return ' {} {}'.format(round(float(size) / (10**(order*3)), 1), _suffixes[order])
