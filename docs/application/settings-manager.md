# Settings Manager

ActivitySim has several configuration files. ABM3 includes multiple ActivitySim-based models (resident, crossborder, airport access, visitor, commercial vehicle), many of which have settings that are in common but specified in their own configuration files. In order to ensure internal consistency between the models (such as having the same ridehail wait time distributions between the resident, crossborder, and visitor models or the same walk speed across all models), the settings manager was created. This reads a master settings file (abm3_settings.yaml) and populates the various configuration files with the appropriate values.

## Master settings file

The master settings file is a yaml file that is read into memory as a dictionary in which settings can be grouped as the user sees appropriate. Rather than read it into memory as a nested dictionary, there is only one level with the key being all of the nests joined by hyphens. For example, the following yaml data...
```
mode:
  walk:
    speed: 3
    maxDist: 2
  bike:
    speed: 10
    maxDist: 20
```
... will be read in as the following dictionary:
```
{
    "mode-walk-speed": 3,
    "mode-walk-maxDist": 2,
    "mode-bike-speed": 10,
    "mode-bike-maxDist": 20
}
```
This is done to ease global replacement. In any configuration yaml file (or the property file), a value from the master settings file can be replaced by putting the dictionary key folowed by a colon within curly brackets. Using the above example, any instance of `{mode-walk-speed:}` in any configuration file would be replaced by 3 when the settings manager is run, even if those references are in different files.

## Resetting settings

If a user realizes that they set a setting to the wrong value after they started a model run, then they need to reset the settings in order to get all of the tags back to what they were before the settings manager was run. This can be accomplished by running reset_settings.cmd within the "bin" folder in the model directory.