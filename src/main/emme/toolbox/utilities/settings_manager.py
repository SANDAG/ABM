import os
import yaml
import inro.modeller as _m

class SettingsManager(object):

    def __init__(self, settings_file):
        self.OPENING_BRACKET_CODE = "[U+007B]"
        self.CLOSING_BRACKET_CODE = "[U+007D]"
        assert self.OPENING_BRACKET_CODE != self.CLOSING_BRACKET_CODE, "Opening and closing bracket codes must be different"

        self.settings = {}
        self.read_settings(settings_file)

    def store_settings(self, full_settings, settings_to_store, subset):
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
                self.store_settings(full_settings, settings_to_store[setting], subset + [setting])

            elif type(settings_to_store[setting]) == list:
                full_settings["-".join(subset + [setting])] = ",".join([str(s) for s in settings_to_store[setting]])

            else:
                full_settings["-".join(subset + [setting])] = settings_to_store[setting]

    def read_settings(self, settings_file):
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

        self.store_settings(self.settings, master_settings)

    def encode_curly_brackets(self, data):
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
            data = data[:closing_index] + self.CLOSING_BRACKET_CODE + data[(closing_index+1):]
            opening_data = data.split(self.CLOSING_BRACKET_CODE)[0]
            for i in range(len(opening_data)-1, -1, -1):
                if opening_data[i] == "{":
                    opening_index = i
                    break
            data = data[:opening_index] + self.OPENING_BRACKET_CODE + data[(opening_index+1):]

        return data

    def decode_curly_brackets(self, data):
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
        return data.replace(self.OPENING_BRACKET_CODE, "{").replace(self.CLOSING_BRACKET_CODE, "}")

    def update_settings_file(self, filename):
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
            setting4: 4,
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

        if ":}" not in data: # Check if there's nothing in the file to update and end if there's not
            return

        try:
            data = self.encode_curly_brackets(data)
            data = data.format(**self.settings)
            data = self.decode_curly_brackets(data)
        except Exception as e:
            print("Error in updating " + filename)
            raise e

        with open(filename, "w") as f:
            f.write(data)
            f.close()

    def update_directory(self, dir):
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
            if f == "logging.yaml": # This file won't be updated and trying to edit it may cause a crash
                continue

            if os.path.isdir(os.path.join(dir, f)):
                self.update_directory(
                    os.path.join(dir, f),
                    self.settings
                )

            elif f.endswith(".yaml"):
                self.update_settings_file(
                    os.path.join(dir, f),
                    self.settings
                )

    def __call__(self, target):
        if os.path.isdir(target):
            self.update_directory(target)

        elif os.path.isfile(target):
            self.update_settings_file(target)