# jflo 5/9/2025
import os
import sys
from shutil import copy2, copytree, rmtree

def get_property(data, property):
    """
    Reads a property from a text string of the property file

    Parameters
    ----------
    data (str):
        Text of property file
    property (str):
        Specific property to get

    Returns
    -------
    value (str):
        Value of property in file
    """
    return data.split(property + " = ")[1].split("\n")[0]

def set_property(data, property, value):
    """
    Sets a property in a text string that will be written as a property file

    Parameters
    ----------
    data (str):
        Text of property file
    property (str):
        Name of property to be set
    value (str):
        Value to set `property` as

    Returns
    -------
    data (str):
        Updated text of property file
    """
    text_to_replace = data.split(property + " = ")[1].split("\n")[0]
    return data.replace(text_to_replace, value)

def set_properties(fp, pairs):
    """
    Updates a property file with a list of length-2 tuples. For each tuple the first value will be replaced in the file with
    the second.

    Parameters
    ----------
    fp (str):
        Filepath of property file
    pairs (list):
        List of length-2 tuples indicating what needs to be replaced and what to replace it by
    """
    with open(fp, "r") as f:
        data = f.read()
        f.close()

    for pair in pairs:
        data = set_property(data, pair[0], pair[1])

    with open(fp, "w") as f:
        f.write(data)
        f.close()

if __name__ == "__main__":
    run_directory = sys.argv[1]
    property_file = os.path.join(run_directory, "conf", "sandag_abm.properties")

    with open(property_file, "r") as f:
        data = f.read()
        f.close()

    release_directory = get_property(data, "release")
    landuse_directory = get_property(data, "landuse")
    network_directory = get_property(data, "network")

    # Copy property file
    copy2(
        os.path.join(release_directory, "common", "conf", "sandag_abm.properties"),
        os.path.join(run_directory, "conf", "sandag_abm.properties")
    )

    # Update property file with release, landuse, and network directories
    set_properties(
        os.path.join(run_directory, "conf", "sandag_abm.properties"),
        [
            ("release", release_directory),
            ("landuse", landuse_directory),
            ("network", network_directory),
        ]
    )

    # Copy passenger model settings
    rmtree(
        os.path.join(run_directory, "src", "asim", "configs")
    )
    copytree(
        os.path.join(release_directory, "common", "src", "asim", "configs"),
        os.path.join(run_directory, "src", "asim", "configs")
    )

    # Copy CVM settings
    rmtree(
        os.path.join(run_directory, "src", "asim", "configs")
    )
    copytree(
        os.path.join(release_directory, "common", "src", "asim-cvm", "configs"),
        os.path.join(run_directory, "src", "asim-cvm", "configs")
    )

    print("Settings have been reset!")