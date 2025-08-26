'''
Runs the settings manager outside of EMME

To be run from root directory of a clone of the ABM repo

Instructions to run from Anaconda prompt:

python src\main\python\run_settings_manager.py
'''
from src.main.emme.toolbox.utilities.settings_manager import SettingsManager

manage_settings = SettingsManager(r"src\main\resources\abm3_settings.yaml")
manage_settings(r"src\main\resources\sandag_abm.properties")
manage_settings(r"src\asim\configs")
manage_settings(r"src\asim-cvm\configs")