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

def connect_to_Azure():
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
        print("datalake exporter connected to Azure container")
        return True, container
    except KeyError as e:
        error_statment = f"{e}: datalake exporter could not find SAS_Token in environment\n"
        print(error_statement, "\n", file=sys.stderr)
        return False, None
    except ServiceRequestError as e:
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
    model_metadata_path = os.path.join(output_path, model, "model_metadata.yaml")
    try:
        with open(model_metadata_path, "r") as stream:
            metadata = yaml.safe_load(stream)
    except FileNotFoundError:
        return {
            "asim_branch_name": ["None"],
            "asim_commit_hash": ["None"],
            "abm_branch_name": ["None"],
            "abm_commit_hash": ["None"],
            "prefix": ["final_"]
        }
    return metadata

def create_metadata_df(model, ts, EMME_metadata, model_metadata):
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

    metadata = { 
        "scenario_name": [EMME_metadata["scenario_title"]],
        "scenario_yr": [EMME_metadata["scenario_year"]],
        "login_name": [EMME_metadata["username"]],
        "machine_name": [machine_name],
        "model": [model],
        "asim_branch_name": [model_metadata["asim_branch_name"]],
        "asim_commit_hash": [model_metadata["asim_commit_hash"]],
        "abm_branch_name": [model_metadata["abm_branch_name"]],
        "abm_commit_hash": [model_metadata["abm_commit_hash"]],
        "scenario_guid": [EMME_metadata["scenario_guid"]],
        "main_directory" : [EMME_metadata["main_directory"]],
        "select_link" : [EMME_metadata["select_link"]]
    }

    meta_df = pd.DataFrame(metadata)

    # add the timestamp as a new column to the DataFrame
    meta_df["scenario_ts"] = pd.to_datetime(ts)

    return meta_df

def export_data(table, name, model, EMME_metadata, timestamp_str, container):
    parent_dir_name = str(EMME_metadata["scenario_title"]) + "__" + str(EMME_metadata["username"]) + "__" + str(EMME_metadata["scenario_guid"][:5])
    model_output_file = "__".join([name, timestamp_str])+".parquet"
    lake_file_name = "/".join(["abm_15_0_0",parent_dir_name,model,model_output_file])

    parquet_file = BytesIO()
    table.to_parquet(parquet_file, engine="pyarrow")

    # change the stream position back to the beginning after writing
    parquet_file.seek(0)

    t0 = datetime.datetime.now()
    container.upload_blob(name=lake_file_name, data=parquet_file)
    print("Write to Data Lake: %s/%s took %s to write to Azure" % (model, name, str(datetime.datetime.now()-t0)))

def write_to_datalake(output_path, models):
    cloud_bool, container = connect_to_Azure()
    if not cloud_bool:
        return
    
    now = datetime.datetime.now()
    timestamp_str = now.strftime("%Y%m%d_%H%M%S")
    EMME_metadata = get_scenario_metadata(output_path)

    for model, is_asim in models:
        if is_asim:
            model_metadata = get_model_metadata(model, output_path)
            prefix = model_metadata["prefix"]
            metadata_df = create_metadata_df(model, now, EMME_metadata, model_metadata)
            export_data(metadata_df, 'scenario', model, EMME_metadata, timestamp_str, container)
        else:
            prefix = ""
        files = glob.glob(os.path.join(output_path, model, prefix + '*.csv'))
        for file in files:
            name = os.path.splitext(os.path.basename(file))[0]
            if prefix != '':
                name = name.split(prefix)[1]
            table = pd.read_csv(file)

            table["scenario_ts"] = pd.to_datetime(now)
            table["scenario_guid"] = EMME_metadata["scenario_guid"]
            if is_asim:
                table["model"] = model
            table.replace("", None, inplace=True) # replace empty strings with None - otherwise conversation error for boolean types

            export_data(table, name, model, EMME_metadata, timestamp_str, container)
        

output_path = sys.argv[1]
models = [
    ('resident', True),
    ('airport.CBX', True),
    ('airport.SAN', True),
    ('crossborder', True),
    ('visitor', True),
    ('report', False)
]
write_to_datalake(output_path, models)