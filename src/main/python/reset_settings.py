# jflo 5/9/2025
import os
import sys
from shutil import copy2, copytree, rmtree

def get_property(
        data: str,
        property: str
):
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

def set_property(
        data: str,
        property: str,
        value: str,
        str_replace: bool
):
    """
    Sets a property in a text string that will be written as a property file

    Parameters
    ----------
    data (str):
        Text of property file
    property (str):
        If `str_replace` is False, this property will be updated with `value`. Otherwise all instances of that text string
        will be replaced with `value`.
    value (str):
        Value to set `property` as
    str_replace (bool):
        If true, str.replace() will be used to replace all instances of `property` with `value`. Otherwise the single value of
        `property` will be replaced by `value`.
    Returns
    -------
    data (str):
        Updated text of property file
    """
    if str_replace:
        return data.replace(property, value)
    else:
        text_to_replace = data.split(property + " = ")[1].split("\n")[0]
        start = data.index(text_to_replace)
        end = start + len(text_to_replace)
        return data[:start] + value + data[end:]

def set_properties(
        fp: str,
        groups: list
):
    """
    Updates a property file with a list of length-2 tuples. For each tuple the first value will be replaced in the file with
    the second.

    Parameters
    ----------
    fp (str):
        Filepath of property file
    group (list):
        List of length-3 tuples indicating what needs to be replaced, what to replace it by, and what method will be used
    """
    with open(fp, "r") as f:
        data = f.read()
        f.close()

    for group in groups:
        assert len(group) == 3, "Inputs must be length-3 tuples"
        data = set_property(data, group[0], group[1], group[2])

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
    scenario_year = get_property(data, "scenarioYear")
    scenario_year_suffix = get_property(data, "scenarioYearSuffix")

    # Copy property file
    copy2(
        os.path.join(release_directory, "common", "conf", "sandag_abm.properties"),
        os.path.join(run_directory, "conf", "sandag_abm.properties")
    )

    # Update property file with release, landuse, and network directories
    set_properties(
        os.path.join(run_directory, "conf", "sandag_abm.properties"),
        [
            ("release", release_directory, False),
            ("landuse", landuse_directory, False),
            ("network", network_directory, False),
            ("${year}", scenario_year, True),
            ("${suffix}", scenario_year_suffix, True),
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
        os.path.join(run_directory, "src", "asim-cvm", "configs")
    )
    copytree(
        os.path.join(release_directory, "common", "src", "asim-cvm", "configs"),
        os.path.join(run_directory, "src", "asim-cvm", "configs")
    )

    print("Settings have been reset!")