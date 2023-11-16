# ActivitySim
# See full license in LICENSE.txt.
import datetime
import git
import logging
import os
import socket
import uuid
import yaml

import numpy as np
import pandas as pd

from activitysim.core import config, inject, pipeline, tracing
from activitysim.core.config import setting
from azure.storage.blob import ContainerClient
from azure.core.exceptions import ServiceRequestError
from io import BytesIO
from io import StringIO

# from io import StringIO

logger = logging.getLogger("activitysim")


def find_git_folder(code_folder, path_level):
    """
    Returns the path to the .git folder

    Parameters
    ----------
    code folder: str
    path_level: str

    Returns
    -------
    git_dir: str
        The path to the .git folder.
    """

    git_dir = os.path.abspath(os.path.join(code_folder, path_level))
    git_folder = os.path.join(git_dir, ".git")
    return git_folder


def get_commit_info(repo_path):
    """
    Returns a dictionary containing the short commit hash and branch name of the Git repository
    at the specified path.

    Parameters
    ----------
    repo_path (str): The path to the Git repository.

    Returns
    -------
    dict: A dictionary with the following keys:
            - short_commit_hash (str): The first 7 characters of the current commit hash.
            - branch_name (str): The name of the current active branch.
            If the repository path is not a Git repository, both values will be empty strings.
    """

    commit_hash = ""
    branch_name = ""

    if os.path.isdir(repo_path):
        try:
            repo = git.Repo(repo_path)
            if repo.head.is_valid():
                commit_hash = repo.head.commit.hexsha[:7]
                if not repo.head.is_detached:
                    branch_name = repo.active_branch.name
            else:
                branch_name = repo.active_branch.name
                branch_file = open(repo_path + "\\refs\\heads\\" + branch_name, "r")
                commit_hash = branch_file.read()[:7]
                # commit_hash = branch_file.read(7)
                branch_file.close()
        except (git.InvalidGitRepositoryError, AttributeError, FileNotFoundError):
            pass

    return {"short_commit_hash": commit_hash, "branch_name": branch_name}


def calc_execute_time(resume_after):
    """
    Calculates the total time from timing log file.

    Args:
        resume_after (string): model step to resume after or None.

    Returns:
        float: The total time in minutes.

    """
    with config.open_log_file("timing_log.csv", "r") as log_file:
        time_in_min = 0
        model_step_found = False
        time_trace = {}
        for line in log_file:
            timing_log_row = line.strip().split(",")
            if resume_after is not None and timing_log_row[1] == resume_after:
                model_step_found = True
                continue  # skip this line and move on to the next one
            if resume_after is not None and not model_step_found:
                continue  # skip all lines until the keyword is found
            try:
                model_step = timing_log_row[1]
                time_in_min = float(timing_log_row[3])
            except ValueError:
                continue  # skip this line if last value is not a float
            time_trace[model_step] = time_in_min

    # Sum the values in the dictionary
    total_time = sum(mins for mins in time_trace.values())

    return total_time


def drop_duplicate_column(table_df, table_name, column_name):
    """
    Drops a column from table if it exists.

    Args:
        table_df (pandas.DataFrame): The DataFrame from which the column should be dropped.
        table_name (str): The name of the table or DataFrame.
        column_name (str): The name of the column to be dropped.

    Returns:
        None

    """
    if column_name in table_df.columns:
        table_df.drop(column_name, axis=1, inplace=True)
        logger.info(f"Dropped duplicate column {column_name} in {table_name}")


def create_metadata_df(input_dir, ts, time_to_write, EMME_metadata):
    """
    create a metadata dataframe containing the git commit, branch, model run timestamp, guid,
    and input data dir.

    Parameters
    ----------
    input_dir: str
       path to the input directory (crop or full)
    guid: str
        unique identifier for model run
    now: str
       timestamp

    Returns
    -------
    metadata_df : pandas.DataFrame
        description of the model run and columns (guid and model run
        timestamp) to join tables (e.g. persons and trips) in a given model run.
    """

    # repo branch name and commit hash: activitysim
    asim_git_folder = find_git_folder(pipeline.__file__, "../../..")
    asim_commit_info = get_commit_info(asim_git_folder)

    # repo branch name and commit hash: abm3
    abm_configs_dir = inject.get_injectable("configs_dir")[0]
    abm_path_level = ".."
    # check one to four levels up for git folder
    for i in range(4):
        abm_git_folder = find_git_folder(abm_configs_dir, abm_path_level)
        if os.path.exists(abm_git_folder):
            break
        else:
            abm_path_level += "/.."
    abm_commit_info = get_commit_info(abm_git_folder)

    machine_name = socket.gethostname()

    settings = inject.get_injectable("settings")
    settings_to_write = ["model_name"]
    default_value = "None"
    metadata_settings = {}
    for key in settings_to_write:
        try:
            value = settings[key]
        except KeyError:
            value = default_value  # None, if not in settings
        metadata_settings[key] = value

    total_time = calc_execute_time(settings["resume_after"]) + time_to_write

    metadata = {
        "scenario_name": [EMME_metadata["scenario_title"]],
        "scenario_yr": [EMME_metadata["scenario_year"]],
        "login_name": [EMME_metadata["username"]],
        "machine_name": [machine_name],
        "model": [metadata_settings["model_name"]],
        "asim_branch_name": [asim_commit_info["branch_name"]],
        "asim_commit_hash": [asim_commit_info["short_commit_hash"]],
        "abm_branch_name": [abm_commit_info["branch_name"]],
        "abm_commit_hash": [abm_commit_info["short_commit_hash"]],
        "time_to_execute": [total_time],
        "scenario_guid": [EMME_metadata["scenario_guid"]],
        "main_directory" : [EMME_metadata["main_directory"]],
        "select_link" : [EMME_metadata["select_link"]]
    }

    meta_df = pd.DataFrame(metadata)

    # add the timestamp as a new column to the DataFrame
    meta_df["scenario_ts"] = pd.to_datetime(ts)

    return meta_df


def column_filter(df, setting):
    # remove columns from a final table
    output_settings_file_name = "..\common\outputs.yaml"
    output_settings = config.read_model_settings(output_settings_file_name)
    remove_cols = output_settings[setting]

    remove_filter = df.filter(remove_cols)
    df_removed = df.drop(columns=remove_filter)
    df_removed.name = df.name

    return df_removed


def write_summarize_files(
    summarize_dir, timestamp_str, guid, year_folder, month_folder, day_folder, container, current_ts
):
    """
    Write output from the summarize model to the Azure Storage container.

    Parameters:
        summarize_dir (str): Directory path containing the output summarize files.
        timestamp_str (str): Timestamp string used for generating output filenames.
        guid (str): Unique identifier for scenario.
        year_folder (str): Year folder used in the Azure Blob Storage path.
        month_folder (str): Month folder used in the Azure Blob Storage path.
        container (azure.storage.blob.ContainerClient): Azure Blob Storage container client.

    Returns:
        None
    """
    for summarize_file in os.listdir(summarize_dir):
        if summarize_file.endswith(".csv"):
            # Extract the base filename and extension from the input file.
            base_summarize_filename, ext = os.path.splitext(
                os.path.basename(summarize_file)
            )

            # Generate the output filename using the given timestamp and guid.
            summarize_out_file = f"{base_summarize_filename}_{timestamp_str}_{guid}.csv"

            # Create the Blob Storage path for the output file.
            summarize_path = f"summarize/{base_summarize_filename}/{year_folder}/{month_folder}/{day_folder}/{summarize_out_file}"

            # Read the CSV data from the summarize folder.
            summarize_df = pd.read_csv(os.path.join(summarize_dir, summarize_file))

            # Add scenario info
            summarize_df["scenario_ts"] = pd.to_datetime(current_ts)
            summarize_df["scenario_guid"] = guid

            # Prepare the CSV data to be written to data lake.
            output = StringIO()
            output = summarize_df.to_csv(
                date_format="%Y-%m-%d %H:%M:%S", index=False, encoding="utf-8"
            )

            # Upload the data to Azure Blob Storage.
            blob_client = container.upload_blob(
                name=summarize_path, data=output, encoding="utf-8", overwrite=True
            )


def get_output_table(table_name, output_tables_settings):
    """
    Get outputs as specified by output_tables list in settings file.

    'output_tables' can specify either a list of output tables to include or to skip
    if no output_tables list is specified, then all checkpointed tables will be written

    To write all output tables EXCEPT the households and persons tables:

    ::

      output_tables:
        action: skip
        tables:
          - households
          - persons

    To write ONLY the households table:

    ::

      output_tables:
        action: include
        tables:
           - households

    To write tables into a single HDF5 store instead of individual CSVs, use the h5_store flag:

    ::

      output_tables:
        h5_store: True
        action: include
        tables:
           - households

    Parameters
    ----------
    output_dir: str

    """
    sort = output_tables_settings.get("sort", False)

    if not isinstance(table_name, str):
        table_decode_cols = table_name.get("decode_columns", {})
        table_name = table_name["tablename"]
    else:
        table_decode_cols = {}

    if table_name == "checkpoints":
        output_table = pipeline.get_checkpoints()
    else:
        # if table_name not in registered_tables:
        #     logger.warning("Skipping '%s': Table not found." % table_name)
        #     continue
        output_table = pipeline.get_table(table_name)
        if sort:
            traceable_table_indexes = inject.get_injectable(
                "traceable_table_indexes", {}
            )

            if output_table.index.name in traceable_table_indexes:
                output_table = output_table.sort_index()
                logger.debug(
                    f"write_to_datalake sorting {table_name} on index {output_table.index.name}"
                )
            else:
                # find all registered columns we can use to sort this table
                # (they are ordered appropriately in traceable_table_indexes)
                sort_columns = [
                    c for c in traceable_table_indexes if c in output_table.columns
                ]
                if len(sort_columns) > 0:
                    output_table = output_table.sort_values(by=sort_columns)
                    logger.debug(
                        f"write_tables sorting {table_name} on columns {sort_columns}"
                    )
                else:
                    logger.debug(
                        f"write_tables sorting {table_name} on unrecognized index {output_table.index.name}"
                    )
                    output_table = output_table.sort_index()

    if config.setting("recode_pipeline_columns", True):
        for colname, decode_instruction in table_decode_cols.items():
            if "|" in decode_instruction:
                decode_filter, decode_instruction = decode_instruction.split("|")
                decode_filter = decode_filter.strip()
                decode_instruction = decode_instruction.strip()
            else:
                decode_filter = None
            if "." not in decode_instruction:
                lookup_col = decode_instruction
                source_table = table_name
                parent_table = output_table
            else:
                source_table, lookup_col = decode_instruction.split(".")
                parent_table = inject.get_table(source_table)
            try:
                map_col = parent_table[f"_original_{lookup_col}"]
            except KeyError:
                map_col = parent_table[lookup_col]
            map_col = np.asarray(map_col)
            map_func = map_col.__getitem__
            if decode_filter:
                if decode_filter == "nonnegative":

                    def map_func(x):
                        return x if x < 0 else map_col[x]

                else:
                    raise ValueError(f"unknown decode_filter {decode_filter}")
            if colname in output_table.columns:
                output_table[colname] = output_table[colname].astype(int).map(map_func)
            elif colname == output_table.index.name:
                output_table.index = output_table.index.astype(int).map(map_func)
            # drop _original_x from table if it is duplicative
            if source_table == table_name and f"_original_{lookup_col}" in output_table:
                output_table = output_table.drop(columns=[f"_original_{lookup_col}"])

    # set sample rate to float
    if table_name == "households":
        output_table["sample_rate"] = output_table["sample_rate"].astype(float)

    # # drop write_trip_matrice skim columns
    # if table_name == "trips":
    #     output_table = final_trips_column_filter(output_table)
    #     output_table.reset_index(drop=False, inplace=True)

    # split vehicle_type column
    if table_name == "vehicles":
        output_table[["vehicle_category", "num_occupants", "fuel_type"]] = output_table[
            "vehicle_type"
        ].str.split(pat="_", expand=True)
        # output_table.drop(columns={'vehicle_type'}, inplace=True) ## TODO decide whether to drop column here or in bronze -> silver filter

    output_table.name = table_name
    return output_table


def write_model_output_to_local(output_table: pd.DataFrame(), output_tables_settings):
    """
    Write pipeline tables as csv files to local drive.
    """
    prefix = output_tables_settings.get("prefix", "final_")
    h5_store = output_tables_settings.get("h5_store", False)

    if h5_store:
        file_path = config.output_file_path("%soutput_tables.h5" % prefix)
        output_table.to_hdf(file_path, key=output_table.name, mode="a", format="fixed")
    else:
        file_name = "%s%s.csv" % (prefix, output_table.name)
        file_path = config.output_file_path(file_name)

        # include the index if it has a name or is a MultiIndex
        write_index = output_table.index.name is not None or isinstance(
            output_table.index, pd.MultiIndex
        )
        output_table.to_csv(file_path, index=write_index)


def connect_to_Azure(path_override=None):
    """
    Check if Azure Storage SAS Token is properly configured in local machine's environment.
        Return Azure ContainerClient and boolean indicating cloud connection was made successfully.
        If it is not, pass argument back to write_to_datalake to only write outputs locally.

    Include stand-in path_override parameter for future config options
    """
    try:
        sas_url = os.environ["AZURE_STORAGE_SAS_TOKEN"]
        container = ContainerClient.from_container_url(sas_url)
        container.get_account_information()
        logger.info("write_to_datalake step connected to Azure container")
        return True, container
    except KeyError as e:
        error_statment = f"{e}: write_to_datalake could not find SAS_Token in environment, only writing tables locally\n"
        print(error_statement, "\n")
        logger.debug(error_statement)
        return False, None
    except ServiceRequestError as e:
        error_statement = f"""
            {e}: write_to_datalake had issue connecting to Azure container using SAS_Token in environment,
            token likely malconfigured, only writing tables locally"""
        print(error_statement,"\n")
        logger.debug(error_statement)
        return False, None


def write_model_output_to_datalake(
    output_table: pd.DataFrame(), prefix, container, EMME_metadata, now
):
    """
    Write pipeline tables as csv files to Azure Data Lake Storage as
    specified by output_tables list in settings file.

    Azure environment variable must exist on server for Azure storage.

    """
    guid = EMME_metadata["scenario_guid"]

    # format the timestamp as a string in a consistent format
    timestamp_str = now.strftime("%Y%m%d_%H%M%S")

    # create folder structure for hierarchical organization of files
    year_folder = now.strftime("%Y")
    month_folder = now.strftime("%m")
    day_folder = now.strftime("%d")

    # # Split the prefix in settings.yaml (final_, final_san, final_cbx, etc.)
    # split_prefix = prefix.split("_")
    # file_name = f"{split_prefix[0]}_{output_table.name}.csv"

    # # extract base filename and extension
    # base_filename, ext = os.path.splitext(os.path.basename(file_name))

    # remove duplicate column
    drop_duplicate_column(output_table, output_table.name, "taz")

    # add model name: resident, visitor, etc.
    settings = inject.get_injectable("settings")
    try:
        model_name = settings["model_name"]
    except KeyError:
        model_name = "None"

    output_table["model"] = model_name

    # extract table name from base filename, e.g. households, trips, persons, etc.
    tablename = output_table.name
    
    # Construct the model output filename w guid
    short_guid = str(guid)[:5]
    scenario_title = EMME_metadata["scenario_title"]
    username  = EMME_metadata['username']
    model_output_file = f"{tablename}__{timestamp_str}"
    lake_file = f"abm_15_0_0/{scenario_title}__{username}__{short_guid}/{model_name}/{model_output_file}.parquet"

    # replace empty strings with None
    # otherwise conversation error for boolean types
    output_table.replace("", None, inplace=True)

    parquet_file = BytesIO()
    output_table.to_parquet(parquet_file, engine="pyarrow")

    # change the stream position back to the beginning after writing
    parquet_file.seek(0)

    container.upload_blob(name=lake_file, data=parquet_file)


def write_summaries_to_datalake(output_dir, container, guid, now):
    """optional function that implements the write_summarize_files() function"""

    # format the timestamp as a string in a consistent format
    timestamp_str = now.strftime("%Y-%m-%d_%H-%M-%S")

    summarize_output_dir = os.path.join(output_dir, "summarize")
    if os.path.exists(summarize_output_dir):
        write_summarize_files(
            summarize_output_dir,
            timestamp_str,
            guid,
            now.strftime("%Y"),  # year_folder
            now.strftime("%m"),  # month_folder
            now.strftime("%d"),  # day_folder
            container,
            now,
        )


def write_metadata_to_datalake(data_dir, t0, EMME_metadata, container, now):
    """
    write scenario and write_to_datalake metadata to the datalake
        after model outputs are exported to the datalake
    """

    t1 = tracing.print_elapsed_time()
    write_time = round((t1 - t0) / 60.0, 1)
    scenario_df = create_metadata_df(data_dir[0], now, write_time, EMME_metadata)

    # format the timestamp as a string in a consistent format
    timestamp_str = now.strftime("%Y%m%d_%H%M%S")

    # add model name: resident, visitor, etc.
    settings = inject.get_injectable("settings")
    try:
        model_name = settings["model_name"]
    except KeyError:
        model_name = "None"

    # generate metadata filename and path
    scenario_title = EMME_metadata["scenario_title"]
    username = EMME_metadata["username"]
    short_guid = EMME_metadata['scenario_guid'][:5]

    metadata_file = f"scenario__{timestamp_str}.parquet"
    metadata_path = f"abm_15_0_0/{scenario_title}__{username}__{short_guid}/{model_name}/{metadata_file}"

    # write metadata to data lake
    parquet_file = BytesIO()
    scenario_df.to_parquet(parquet_file, engine="pyarrow")
    # change the stream position back to the beginning after writing
    parquet_file.seek(0)
    container.upload_blob(name=metadata_path, data=parquet_file)


def get_scenario_metadata(output_dir):
    """
    get scenario's guid (globally unique identifier)
    if guid not in scenario's output directory (generated by EMME's master_run.py),
        then generate a model-specific guid
    """
    # not_iteration = 999
    datalake_metadata_path = os.path.abspath(os.path.join(output_dir, '../', "datalake_metadata.yaml"))
    if os.path.isfile(datalake_metadata_path):
        with open(datalake_metadata_path, "r") as stream:
            model_metadata_dict = yaml.safe_load(stream)
            logger.info(f"datalake_metadata.yaml file found at: {datalake_metadata_path}")
        # overwrite existing guid if running model from cmd so that model runs do not share guid
        # if model_metadata_dict["end_iteration"] == not_iteration:
        #     model_metadata_dict['scenario_guid'] = uuid.uuid4().hex
        #     model_metadata_dict['scenario_guid_created_at'] = datetime.datetime.now()
        #     with open(datalake_metadata_path, "w") as file:
        #         yaml.dump(model_metadata_dict, file, default_flow_style=False)
        #     logger.info(f"overwriting guid in datalake_metadata.yaml file found at: {datalake_metadata_path}")
    else:
        logger.info(f"datalake_metadata.yaml file NOT found at: {datalake_metadata_path}, using new guid")
        model_metadata_dict = {"main_directory" : None
                                ,"scenario_guid" : uuid.uuid4().hex
                                ,'scenario_guid_created_at' : datetime.datetime.now()
                                ,"scenario_title" : "abm2022" #TODO change this to None when development ends
                                ,"scenario_year": "2022" #TODO change this to None when development ends
                                ,"select_link" : None
                                ,"username" : os.getenv("USERNAME")}
        # write guid to file for DataExporter
        # with open(datalake_metadata_path, "w") as file:
        #     yaml.dump(model_metadata_dict, file, default_flow_style=False)
    return model_metadata_dict


def get_output_table_names(output_tables_settings, output_tables_settings_name):
    """ """
    action = output_tables_settings.get("action")
    tables = output_tables_settings.get("tables")
    registered_tables = pipeline.registered_tables()
    if action == "include":
        # interpret empty or missing tables setting to mean include all registered tables
        output_tables_list = tables if tables is not None else registered_tables
    elif action == "skip":
        output_tables_list = [t for t in registered_tables if t not in tables]
    else:
        raise "expected %s action '%s' to be either 'include' or 'skip'" % (
            output_tables_settings_name,
            action,
        )
    return output_tables_list


@inject.step()
def write_to_datalake(
    data_dir, output_dir
):  # TODO rename function? it's not only writing to datalake now, it writes locally as well
    """
    Write pipeline tables as csv files to Azure Data Lake Storage and/or local drive.

    Parameters
    ----------
    output_dir: str

    """
    t0 = tracing.print_elapsed_time()

    # get list of model outputs to write out
    output_tables_settings_name = "output_tables"
    output_tables_settings = setting(output_tables_settings_name)
    if output_tables_settings is None:
        logger.info("No output_tables specified in settings file. Nothing to write.")
        return
    output_tables_list = get_output_table_names(
        output_tables_settings, output_tables_settings_name
    )

    # determine if writing to cloud is permitted
    # if permitted, get Azure cloudcontainer
    cloud_bool, container = connect_to_Azure()

    # get parameters for writing to datalake (if permitted)
    if cloud_bool:
        now = datetime.datetime.now()
        prefix = output_tables_settings.get("prefix", "final_")
        EMME_metadata = get_scenario_metadata(output_dir)

    # write out model outputs to local and datalake (if permitted)
    for table_name in output_tables_list:
        output_table = get_output_table(table_name, output_tables_settings)

        # drop write_trip_matrice skim columns
        if output_table.name == "trips":
            output_table = column_filter(output_table, "REMOVE_COLUMNS_TRIPS")

        if output_table.name == "households":
            output_table = column_filter(output_table, "REMOVE_COLUMNS_HOUSEHOLDS")

        if cloud_bool:
            # add unique identifier
            output_table["scenario_guid"] = EMME_metadata["scenario_guid"]
            # add the timestamp as a new column to the DataFrame
            output_table["scenario_ts"] = pd.to_datetime(now)

        write_model_output_to_local(output_table, output_tables_settings)
        if cloud_bool:
            write_model_output_to_datalake(output_table, prefix, container, EMME_metadata, now)
            logger.info(f"write_to_datalake writing {table_name} to cloud")

    # write out summary and metadata tables to datalake (if permitted)
    if cloud_bool:
        write_summaries_to_datalake(output_dir, container, EMME_metadata["scenario_guid"], now)
        write_metadata_to_datalake(data_dir, t0, EMME_metadata, container, now)
