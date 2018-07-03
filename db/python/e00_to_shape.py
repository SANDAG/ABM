import arcpy
from arcpy import env
import sys
import os
import shutil


scenarioFolder = sys.argv[1]

# Remove already created coverages if they exist
# using shutil since arcpy delete doesn't remove info folder
if os.path.exists(scenarioFolder + "\\trcov"):
    shutil.rmtree(scenarioFolder + "\\trcov", ignore_errors=True)
if os.path.exists(scenarioFolder + "\\hwycov"):
    shutil.rmtree(scenarioFolder + "\\hwycov", ignore_errors=True)
if os.path.exists(scenarioFolder + "\\info"):
    shutil.rmtree(scenarioFolder + "\\info", ignore_errors=True)

# Move to scenario folder
env.workspace = scenarioFolder

# Create transit coverage from e00 files
arcpy.ImportFromE00_conversion("\\input\\trcov.e00",scenarioFolder,"trcov")

# Remove already created shape files if they exist
if arcpy.Exists("stop.shp"):
    arcpy.Delete_management("stop.shp")
if arcpy.Exists("route.shp"):
    arcpy.Delete_management("route.shp")

# Create necessary shape files from hwy and transit coverages
arcpy.FeatureClassToFeatureClass_conversion("trcov\\node", scenarioFolder, "stop.shp", '"ISTOP" > 3')
arcpy.FeatureClassToFeatureClass_conversion("trcov\\route.transit", scenarioFolder, "route.shp")
