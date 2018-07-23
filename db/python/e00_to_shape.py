import arcpy
from arcpy import env
import sys
import os
import shutil

# abm scenario folder
scenarioFolder = sys.argv[1]

# write coverage created from ImportFromE00_conversion function
# to a designated output folder to avoid undocumented
# issue with the functions Output_folder parameter being unable
# to handle strings over a certain size
outputPath = sys.argv[2]

# remove already created coverages if they exist
# use shutil since arcpy delete does not remove info folder
if os.path.exists(outputPath + "\\trcov"):
    shutil.rmtree(outputPath + "\\trcov", ignore_errors=True)
if os.path.exists(outputPath + "\\info"):
    shutil.rmtree(outputPath + "\\info", ignore_errors=True)

# move arcpy workspace to abm scenario folder
env.workspace = scenarioFolder

# create transit coverage from e00 files
arcpy.ImportFromE00_conversion("input\\trcov.e00",outputPath,"trcov")

# remove already created shape files if they exist
if arcpy.Exists("stop.shp"):
    arcpy.Delete_management("stop.shp")
if arcpy.Exists("route.shp"):
    arcpy.Delete_management("route.shp")

# create necessary shape files from transit coverage in the abm scenario folder
arcpy.FeatureClassToFeatureClass_conversion(outputPath + "\\trcov\\node",
                                            scenarioFolder,
                                            "stop.shp", '"ISTOP" > 3')

arcpy.FeatureClassToFeatureClass_conversion(outputPath + "\\trcov\\route.transit",
                                            scenarioFolder,
                                            "route.shp")

# clean up created coverages if they exist
# use shutil since arcpy delete does not remove info folder
if os.path.exists(outputPath + "\\trcov"):
    shutil.rmtree(outputPath + "\\trcov", ignore_errors=True)
if os.path.exists(outputPath + "\\info"):
    shutil.rmtree(outputPath + "\\info", ignore_errors=True)
