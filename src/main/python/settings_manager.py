import sys
import os
import yaml

run_path = sys.argv[1]
OPENING_BRACKET_CODE = "[U+007B]"
CLOSING_BRACKET_CODE = "[U+007D]"
assert OPENING_BRACKET_CODE != CLOSING_BRACKET_CODE, "Opening and closing bracket codes must be different"

def store_settings(
        full_settings: dict,
        settings_to_store: dict,
        subset: list = [],
):
    """
    Stores a set of settings into an existing dictionary in place. If a list is specified in `subset` that list joined
    by hyphens will be the keys.
    
    Parameters
    ----------
    full_settings (dict):
        The full set of settings to add onto. This is edited in place.
    settings_to_store (dict):
        The group of settings to be added to `full_settings`
    subset (list):
        The subset of settings that is being added. This will be added to the front of each key in `full_settings`
        for every setting in `settings_to_store` that is added.
    """
    for setting in settings_to_store:
        if type(settings_to_store[setting]) == dict:
            store_settings(full_settings, settings_to_store[setting], subset + [setting])

        elif type(settings_to_store[setting]) == list:
            full_settings["-".join(subset + [setting])] = ",".join([str(s) for s in settings_to_store[setting]])

        else:
            full_settings["-".join(subset + [setting])] = settings_to_store[setting]

def read_settings(
        settings_file: str
):
    """
    Reads in the master settings file and stores in a single non-nested dictionary. Nesting is accounted for by adding
    the names of the upper levels joined by a hyphen. An example input and output is shown below:

    Input (yaml file):
    setting1: 1
    setting2: 2
    setting3:
      subset1: 3
      subset2:
        - 4
        - 5
    setting4: 6

    Output (dict):
    {
        "setting1": 1,
        "setting2": 2,
        "setting3-subset1": 3,
        "setting3-subset2": [4, 5],
        "setting4": 6
    }

    Parameters
    ----------
    settings_file (str):
        The name of the master settings file

    Returns
    -------
    settings (dict):
        All of the settings defined in `settings_file` stored in a single (non-nested) dictionary
    """
    with open(settings_file, "r") as f:
        master_settings = yaml.safe_load(f)
        f.close()

    settings = {}
    store_settings(settings, master_settings)

    return settings

def encode_curly_brackets(
        data: str,
        opening_bracket_code: str = OPENING_BRACKET_CODE,
        closing_bracket_code: str = CLOSING_BRACKET_CODE,
):
    """
    Replaces the presence of } in a string that isn't preceeded by : along with the corresponding opening bracket
    by the string defined in `code`

    Parameters
    ----------
    data (str):
        File data
    code (str):
        Code to replace } with

    Returns
    -------
    data (str):
        File data with } replaced by `code`
    """
    # Identify brackets of interest
    locs = []
    for i in range(1, len(data)):
        if data[i] == "}":
            if data[i-1] != ":":
                locs.append(i)
    locs.reverse() # The replacement will be done in reverse order to preserve index numbers

    # Replace brackets with codes
    for closing_index in locs:
        data = data[:closing_index] + closing_bracket_code + data[(closing_index+1):]
        opening_data = data.split(closing_bracket_code)[0]
        for i in range(len(opening_data)-1, -1, -1):
            if opening_data[i] == "{":
                opening_index = i
                break
        data = data[:opening_index] + opening_bracket_code + data[(opening_index+1):]

    return data

def decode_curly_brackets(
        data: str,
        opening_bracket_code: str = OPENING_BRACKET_CODE,
        closing_bracket_code: str = CLOSING_BRACKET_CODE,
):
    """
    Replaces the string defined in `code` with } in the input `data` string

    Parameters
    ----------
    data (str):
        File data
    code (str):
        Code to be replaced by }

    Returns
    -------
    data (str):
        File data with `code` replaced by }
    """
    return data.replace(opening_bracket_code, "{").replace(closing_bracket_code, "}")

def update_settings_file(
        filename: str,
        settings: dict,
):
    """
    Updates a settings file with a dictionary by replacing the text string {KEY:} with the value of `settings`[KEY].
    The updated settings file is then overwritten.
    An example is shown below:

    Input file:
    setting1: 1
    setting2: {setting2:}
    setting3: 3
    setting4: {setting4:}
    setting5: 5

    Input settings dictionary:
    {
        setting2: 2,
        setting2: 4,
    }

    Output file:
    setting1: 1
    setting2: 2
    setting3: 3
    setting4: 4
    setting5: 5

    Parameters
    ----------
    filename (str):
        Name of file to update
    settings (dict):
        Dictionary of settings to update the file specified by `filename` with
    """
    with open(filename, "r") as f:
        data = f.read()
        f.close()

    data = encode_curly_brackets(data)
    data = data.format(**settings)
    data = decode_curly_brackets(data)

    with open(filename, "w") as f:
        f.write(data)
        f.close()

def update_directory(
        dir: str,
        settings: dict,
):
    """
    Applies update_settings_file to every yaml file in a directory including all of its subdirectories.

    Parameters
    ----------
    dir (str):
        Directory containing yaml files to update
    settings (dict):
        Dictionary of settings to update with
    """
    for f in os.listdir(dir):
        if os.path.isdir(os.path.join(dir, f)):
            update_directory(
                os.path.join(dir, f),
                settings
            )

        elif f.endswith(".yaml"):
            update_settings_file(
                os.path.join(dir, f),
                settings
            )

# Read master settings file
settings = read_settings(
    os.path.join(run_path, "conf", "abm3_settings.yaml")
)

# Update property file
update_settings_file(
    os.path.join(run_path, "conf", "sandag_abm.properties"),
    settings
)

# Update passenger ActivitySim config files
update_directory(
    os.path.join(run_path, "src", "asim", "configs"),
    settings
)

# Update CVM config files
update_directory(
    os.path.join(run_path, "src", "asim-cvm", "configs"),
    settings
)