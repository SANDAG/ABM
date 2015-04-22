import arcpy
from arcpy import env
import sys
import os
import shutil

# For testing
# scenarioFolder = "T:\\data\\ABM_AT\\aging\\2012_base"

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

# Remove already created shape files if they exist
if arcpy.Exists("tap.shp"):
    arcpy.Delete_management("tap.shp")
if arcpy.Exists("stop.shp"):
    arcpy.Delete_management("stop.shp")
if arcpy.Exists("route.shp"):
    arcpy.Delete_management("route.shp")
if arcpy.Exists("trlink.shp"):
    arcpy.Delete_management("trlink.shp")
if arcpy.Exists("hwylink.shp"):
    arcpy.Delete_management("hwylink.shp")

# Delete csv's created from dbf files if they exist
if os.path.exists(scenarioFolder + "\\input\\SANDAG_Bike_Net.csv"):
    os.remove(scenarioFolder + "\\input\\SANDAG_Bike_Net.csv")
if os.path.exists(scenarioFolder + "\\input\\SANDAG_Bike_Node.csv"):
    os.remove(scenarioFolder + "\\input\\SANDAG_Bike_Node.csv")
