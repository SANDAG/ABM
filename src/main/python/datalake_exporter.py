import sys
import datetime
import os
import socket
import yaml
import glob

import pandas as pd

from azure.storage.blob import ContainerClient
from azure.core.exceptions import ServiceRequestError
from io import BytesIO

def connect_to_Azure(env):
    """
    Check if Azure Storage SAS Token is properly configured in local machine's environment.
        Return Azure ContainerClient and boolean indicating cloud connection was made successfully.
        If it is not, pass argument back to write_to_datalake to only write outputs locally.

    Include stand-in path_override parameter for future config options
    """
    try:
        if env == "dev":
            sas_url = os.environ["AZURE_STORAGE_SAS_TOKEN_DEV"]
        else:
            sas_url = os.environ["AZURE_STORAGE_SAS_TOKEN_PROD"]
        container = ContainerClient.from_container_url(sas_url)
        container.get_account_information()
        print("datalake exporter connected to Azure container")
        return True, container
    except KeyError as e:
        error_statement = f"{e}: datalake exporter could not find SAS_Token in environment\n"
        print(error_statement, "\n", file=sys.stderr)
        return False, None
    except Exception as e:
        error_statement = f"""
            {e}: datalake exporter had issue connecting to Azure container using SAS_Token in environment,
            token likely malconfigured"""
        print(error_statement,"\n", file=sys.stderr)
        return False, None

def get_scenario_metadata(output_path):
    """
    get scenario's guid (globally unique identifier) and other metadata
    """
    datalake_metadata_path = os.path.join(output_path, "datalake_metadata.yaml")
    with open(datalake_metadata_path, "r") as stream:
        metadata = yaml.safe_load(stream)
    return metadata

def get_model_metadata(model, output_path):
    metadata = {
        "asim_branch_name": "",
        "asim_commit_hash": "",
        "abm_branch_name": "",
        "abm_commit_hash": "",
        "constants": {},
        "prefix": "final_"
    }
    model_metadata_path = os.path.join(output_path, model, "model_metadata.yaml")
    try:
        with open(model_metadata_path, "r") as stream:
            new_metadata = yaml.safe_load(stream)
            metadata.update(new_metadata)
    except FileNotFoundError:
        pass
    return metadata

def create_scenario_df(ts, EMME_metadata, parent_dir_name, output_path):
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

    machine_name = socket.gethostname()

    # repo branch name and commit hash: abm3
    abm_git_path = os.path.abspath(os.path.join(output_path, '..', 'git_info.yaml'))
    if os.path.isfile(abm_git_path):
        with open(abm_git_path, "r") as stream:
            abm_git_info = yaml.safe_load(stream)
            abm_git_info["commit"] = abm_git_info["commit"][:7]
    else:
        abm_git_info = {
            "branch": "",
            "commit": ""
        }

    metadata = {
        "scenario_name": [EMME_metadata["scenario_title"]],
        "scenario_yr": [EMME_metadata["scenario_year"]],
        "login_name": [EMME_metadata["username"]],
        "machine_name": [machine_name],
        "abm_branch_name": [abm_git_info["branch"]],
        "abm_commit_hash": [abm_git_info["commit"]],
        "scenario_id": [EMME_metadata["scenario_id"]],
        "scenario_guid": [EMME_metadata["scenario_guid"]],
        "main_directory" : [EMME_metadata["main_directory"]],
        "datalake_path" : ["/".join(["bronze/abm3dev/abm_15_0_0",parent_dir_name])],
        "select_link" : [EMME_metadata["select_link"]],
        "sample_rate" : [EMME_metadata["sample_rate"]]
    }

    meta_df = pd.DataFrame(metadata)

    # add the timestamp as a new column to the DataFrame
    meta_df["scenario_ts"] = pd.to_datetime(ts)

    return meta_df

def create_model_metadata_df(model, model_metadata):
    metadata = {
        "model": [model],
        "asim_branch_name": [model_metadata["asim_branch_name"]],
        "asim_commit_hash": [model_metadata["asim_commit_hash"]],
        "abm_branch_name": [model_metadata["abm_branch_name"]],
        "abm_commit_hash": [model_metadata["abm_commit_hash"]]
    }

    meta_df = pd.DataFrame(metadata)

    return meta_df

def export_table(table, name, model, parent_dir_name, container):
    model_output_file = name+".parquet"
    if model == '':
        lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,model_output_file])
    else:
        lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,model,model_output_file])

    parquet_file = BytesIO()
    table.to_parquet(parquet_file, engine="pyarrow")

    # change the stream position back to the beginning after writing
    parquet_file.seek(0)

    t0 = datetime.datetime.now()
    container.upload_blob(name=lake_file_name, data=parquet_file)
    print("Write to Data Lake: %s/%s took %s to write to Azure" % (model, name, str(datetime.datetime.now()-t0)))

def write_to_datalake(output_path, models, exclude, env):
    cloud_bool, container = connect_to_Azure(env)
    if not cloud_bool:
        return

    for model, relpath, is_asim in models:
        if is_asim:
            model_metadata = get_model_metadata(model, output_path)
            prefix = model_metadata["prefix"]
        elif model == "CVM":
            prefix = "final_"
        elif model == "HTM":
            prefix = "final_"
        else:
            prefix = ""
        files = glob.glob(os.path.join(output_path, relpath, model, prefix + '*'))
        if not files:
            print(("%s has no output files" % model), file=sys.stderr)


    now = datetime.datetime.now()
    EMME_metadata = get_scenario_metadata(output_path)
    if "scenario_id" not in EMME_metadata:
        print("No scenario id found in metadata file", file=sys.stderr)
        return
    parent_dir_name = str(EMME_metadata["scenario_title"]) + "__" + str(EMME_metadata["username"]) + "__" + str(EMME_metadata["scenario_id"])

    scenario_df = create_scenario_df(now, EMME_metadata, parent_dir_name, output_path)
    export_table(scenario_df, 'scenario', '', parent_dir_name, container)

    for model, relpath, is_asim in models:
        if is_asim:
            model_metadata = get_model_metadata(model, output_path)
            prefix = model_metadata["prefix"]
            model_metadata_df = create_model_metadata_df(model, model_metadata)
            export_table(model_metadata_df, 'model_metadata', model, parent_dir_name, container)
            constants_df = pd.json_normalize(model_metadata["constants"], sep='__')
            export_table(constants_df, 'constants', model, parent_dir_name, container)
        elif model == "CVM":
            prefix = "final_"
        elif model == "HTM":
            prefix = "final_"
        else:
            prefix = ""

        files = glob.glob(os.path.join(output_path, relpath, model, prefix + '*'))
        for file in files:
            if os.path.basename(file) in exclude:
                continue
            name, ext = os.path.splitext(os.path.basename(file))
            if prefix != '':
                name = name.split(prefix)[1]

            if ext == '.csv':
                table = pd.read_csv(file, low_memory=False)

                table["scenario_ts"] = pd.to_datetime(now)
                table["scenario_id"] = EMME_metadata["scenario_id"]
                if is_asim or model == "CVM" or model == "HTM":
                    table["model"] = model
                table.replace("", None, inplace=True) # replace empty strings with None - otherwise conversation error for boolean types

                export_table(table, name, model, parent_dir_name, container)
            else:
                with open(file, "rb") as data:
                    if model == '':
                        lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,name+ext])
                    else:
                        lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,model,name+ext])
                    container.upload_blob(name=lake_file_name, data=data)

    report_path = os.path.join(os.path.split(output_path)[0], 'report')

    other_files = [
        EMME_metadata["properties_path"],
        os.path.join(output_path, 'skims', 'traffic_skims_MD.omx'),
        os.path.abspath(os.path.join(output_path, '..', 'input', 'zone_term.csv')),
        os.path.abspath(os.path.join(output_path, '..', 'input', 'trlink.csv')),
        os.path.join(output_path, 'bikeMgraLogsum.csv'),
        os.path.join(output_path, 'microMgraEquivMinutes.csv'),
        os.path.join(report_path, 'walkMgrasWithin45Min_AM.csv'),
        os.path.join(report_path, 'walkMgrasWithin45Min_MD.csv')
    ]
    for file in other_files:
        try:
            with open(file, "rb") as data:
                lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,os.path.basename(file)])
                container.upload_blob(name=lake_file_name, data=data)
        except (FileNotFoundError, KeyError):
            print(("%s not found" % file), file=sys.stderr)
            pass


output_path = sys.argv[1]
env = sys.argv[2]
models = [
    ('resident', '', True),
    ('airport.CBX', '', True),
    ('airport.SAN', '', True),
    ('crossborder', '', True),
    ('visitor', '', True),
    ('CVM', '', False),
    ('HTM', '', False),
    ('report', '..', False)
]
exclude = [
    'final_pipeline.h5'
]
write_to_datalake(output_path, models, exclude, env)
