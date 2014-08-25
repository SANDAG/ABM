import arcpy
from arcpy import env
import sys
import os
import shutil
import csv
from dbfpy import dbf


def dbf_to_csv(dbf_fn, csv_fn, cols_wanted):
    in_db = dbf.Dbf(dbf_fn)
    out_csv = csv.writer(open(csv_fn, 'wb'))

    names = []
    position = []

    # Grab positions of wanted columns
    for field in in_db.header.fields:
        names.append(field.name)
    for col in cols_wanted:
        position.append(names.index(col))

    # Write out desired columns
    out_csv.writerow(cols_wanted)
    for rec in in_db:
        out_rec = []
        for pos in position:
            out_rec.append(rec.fieldData[pos])
        out_csv.writerow(out_rec)

        
# For testing
# scenarioFolder = "T:\\projects\\sr13\\sdf_scenevl_r3\\2012_mustang"
scenarioFolder = str.replace(sys.argv[1], "\\", "\\\\")


# Remove already created coverages if they exist
# using shutil since arcpy delete doesn't remove info folder
if os.path.exists(scenarioFolder + "\\trcov"):
    shutil.rmtree(scenarioFolder + "\\trcov", ignore_errors=True)
if os.path.exists(scenarioFolder + "\\hwycov"):
    shutil.rmtree(scenarioFolder + "\\hwycov", ignore_errors=True)
if os.path.exists(scenarioFolder + "\\info"):
    shutil.rmtree(scenarioFolder + "\\info", ignore_errors=True)


# Move to scenario folder
env.workspace= scenarioFolder


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


# Create hwy and transit coverages from e00 files
arcpy.ImportFromE00_conversion("\\input\\trcov.e00",scenarioFolder,"trcov")
arcpy.ImportFromE00_conversion("\\input\\hwycov.e00",scenarioFolder,"hwycov")


# Create necessary shape files from hwy and transit coverages
arcpy.FeatureClassToFeatureClass_conversion("trcov\\node", scenarioFolder, "tap.shp", '"TAP">0 and "ISTOP" > 3', )
arcpy.FeatureClassToFeatureClass_conversion("trcov\\node", scenarioFolder, "stop.shp", '"ISTOP" > 3')
arcpy.FeatureClassToFeatureClass_conversion("trcov\\route.transit", scenarioFolder, "route.shp")
arcpy.FeatureClassToFeatureClass_conversion("trcov\\arc", scenarioFolder, "trlink.shp")
arcpy.FeatureClassToFeatureClass_conversion("hwycov\\arc", scenarioFolder, "hwylink.shp")


# Create csv from Bike_Net .dbf file
in_bike_net = scenarioFolder + "\\input\\SANDAG_Bike_Net.dbf"
out_bike_net = scenarioFolder + "\\input\\SANDAG_Bike_Net.csv"
col_bike_net = ["ROADSEGID", "RD20FULL", "A", "B", "DISTANCE", "AB_GAIN",
                "BA_GAIN", "ABBIKECLAS", "BABIKECLAS", "AB_LANES",
                "BA_LANES", "FUNC_CLASS", "BIKE2SEP", "BIKE3BLVD",
                "SPEED", "PARKAREA", "SCENICIDX"]
dbf_to_csv(in_bike_net, out_bike_net, col_bike_net)



# Create csv from Bike_Node .dbf file
in_bike_node = scenarioFolder + "\\input\\SANDAG_Bike_Node.dbf"
out_bike_node = scenarioFolder + "\\input\\SANDAG_Bike_Node.csv"
col_bike_node = ["NODELEV_ID", "SIGNAL"]
dbf_to_csv(in_bike_node, out_bike_node, col_bike_node)
