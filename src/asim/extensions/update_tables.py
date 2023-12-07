# ActivitySim
# See full license in LICENSE.txt.
import git
import logging
import os
import yaml

import numpy as np

from activitysim.core import config, inject, pipeline, tracing
from activitysim.core.config import setting

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


def write_metadata(prefix):

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

    trip_settings = config.read_model_settings("write_trip_matrices.yaml")
    constants = trip_settings.get("CONSTANTS")

    model_metadata_dict = {
        "asim_branch_name": asim_commit_info["branch_name"],
        "asim_commit_hash": asim_commit_info["short_commit_hash"],
        "abm_branch_name": abm_commit_info["branch_name"],
        "abm_commit_hash": abm_commit_info["short_commit_hash"],
        "constants": constants,
        "prefix": prefix
    }
    output_dir = inject.get_injectable("output_dir")
    model_metadata_path = os.path.join(output_dir,'model_metadata.yaml')
    with open(model_metadata_path, 'w') as file:
        yaml.dump(model_metadata_dict, file, default_flow_style=False)

def remove_columns(table_settings, df):
    # remove columns from a final table
    setting = "remove_columns"
    if setting not in table_settings:
        return df
    remove_cols = table_settings[setting]

    remove_filter = df.filter(remove_cols)
    df_removed = df.drop(columns=remove_filter)
    # df_removed.name = df.name

    return df_removed

def reorder_columns(table_settings, df):
    # reorder columns in a final table
    setting = "reorder_columns"
    if setting not in table_settings:
        return df
    reorder_cols = table_settings[setting]

    # index will not get reordered
    if df.index.name in reorder_cols:
        reorder_cols.remove(df.index.name)

    existing_cols = df.columns.values.tolist()
    for col in existing_cols:
        if col not in reorder_cols:
            reorder_cols.append(col)
    
    for col in reorder_cols:
        if col not in existing_cols:
            df[col] = np.nan

    df_reorder = df[reorder_cols]
    # df_reorder.name = df.name
    
    return df_reorder

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
def update_tables():
    
    # get list of model outputs to update
    output_tables_settings_name = "output_tables"
    output_tables_settings = setting(output_tables_settings_name)
    if output_tables_settings is None:
        logger.info("No output_tables specified in settings file. Nothing to update.")
        return
    output_tables_list = get_output_table_names(
        output_tables_settings, output_tables_settings_name
    )

    common_settings_file_name = "..\common\outputs.yaml"
    common_settings = config.read_model_settings(common_settings_file_name)

    for table_name in output_tables_list:
        if not isinstance(table_name, str):
            table_name = table_name["tablename"]

        if not (table_name in common_settings
                or table_name == "households"
                or table_name == "vehicles"):
            continue
        
        output_table = pipeline.get_table(table_name)
        
        # set sample rate to float
        if table_name == "households":
            output_table["sample_rate"] = output_table["sample_rate"].astype(float)

        # split vehicle_type column
        if table_name == "vehicles":
            output_table[["vehicle_category", "num_occupants", "fuel_type"]] = output_table[
                "vehicle_type"
            ].str.split(pat="_", expand=True)
            # output_table.drop(columns={'vehicle_type'}, inplace=True) ## TODO decide whether to drop column here or in bronze -> silver filter
        
        if table_name in common_settings:
            table_settings = common_settings[table_name]
            output_table = remove_columns(table_settings, output_table)
            output_table = reorder_columns(table_settings, output_table)
        
        pipeline.replace_table(table_name, output_table)

    prefix = output_tables_settings.get("prefix", "final_")
    write_metadata(prefix)