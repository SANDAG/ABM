import csv
from dbfread import DBF
from osgeo import ogr, osr
import sys


def dbf_to_csv(dbf_fn, fields, csv_fn):
    """
    Takes an input dbf file and writes the fields of interest to a csv file.

    :param dbf_fn: String path of dbf file to export to csv
    :param fields: List of field names within dbf to export to csv
    :param csv_fn: String path of csv file to create
    :return:
    """
    in_db = DBF(dbf_fn)
    out_csv = csv.writer(open(csv_fn, "wb"))  # add newline="" argument for python 3 and make wb into w

    # write out desired columns
    out_csv.writerow(fields)
    for dbf_record in in_db:
        out_csv.writerow([dbf_record[x] for x in fields])


def ogr_to_csv(fn, layer_nm, fields, csv_fn, in_srid = None, out_srid=None, where_filter=None):
    """
    Takes an input layer of a compatible OGR GIS file, writes the
    fields of interest and spatial wkt feature to a csv file applying a field
    based filter if specified.

    Modified from
    https://pcjericks.github.io/py-gdalogr-cookbook/layers.html
    #filter-and-select-input-shapefile-to-new-output-shapefile-like-ogr2ogr-cli
    https://gist.github.com/dkapitan/0ea7f170d5e961423dd3

    :param fn: String path of OGR GIS file to export to csv file
    :param layer_nm: String layer name in OGR GIS file to export to csv file
    :param fields: List of String field names within OGR GIS file layer to
                   export to csv file
    :param csv_fn: String path of csv file to create
    :param where_filter: String filter in SQL query WHERE format to apply
                         to the exported layer
    :return:
    """
    # get the input OGR GIS layer
    in_datasource = ogr.Open(fn, 0)  # open OGR GIS file as read only
    in_layer = in_datasource.GetLayer(layer_nm)

    # apply where filter if applicable
    if where_filter is None:
        pass
    else:
        in_layer.SetAttributeFilter(where_filter)  # set attribute filer

    # open output csv file to write output
    out_csv = csv.writer(open(csv_fn, "wb"))  # add newline="" argument for python 3 and make wb into w

    # write out header
    header = list(fields)  # header = [*fields, "geometry"] for python 3
    header.append("geometry")
    out_csv.writerow(header)

    # for each feature in the OGR GIS layer
    # write out the fields of interest and the geometry field as a wkt
    for feature in in_layer:
        # only take fields of interest
        row = [feature.GetField(field) for field in fields]
        geom = feature.GetGeometryRef()  # get geometry
        if geom is None:
            pass  # if no geometry then skip record
        else:
            wkt = geom.ExportToWkt()  # create wkt representation of geometry
            row.append(wkt)  # append wkt representation to the record
            out_csv.writerow(row)


# for testing
# scenario_folder = "//sandag.org/transdata/ABM/temp/2016_calibration"
scenario_folder = sys.argv[1]

# create highway link csv file with wkt field from e00 file
ogr_to_csv(fn=scenario_folder + "/input/hwycov.e00",
           layer_nm="ARC",
           fields=["HWYCOV-ID"],
           csv_fn=scenario_folder + "/input/hwy_link.csv")

# create transit link csv file with wkt field from e00 file
ogr_to_csv(fn=scenario_folder + "/input/trcov.e00",
           layer_nm="ARC",
           fields=["TRCOV-ID"],
           csv_fn=scenario_folder + "/input/transit_link.csv")

# create transit tap csv file with wkt field from shape file
ogr_to_csv(fn=scenario_folder + "/input/tapcov.shp",
           layer_nm="tapcov",
           fields=["TAP"],
           csv_fn=scenario_folder + "/input/transit_link_tap.csv",
           where_filter="TAP > 0 and ISTOP > 3")  # filter is unncecessary

# create transit stop csv file with wkt field from shape file
ogr_to_csv(fn=scenario_folder + "/stop.shp",
           layer_nm="stop",
           fields=["TRCOV_ID"],
           csv_fn=scenario_folder + "/input/transit_link_stop.csv",
           where_filter="ISTOP > 3")

# create transit stop csv file with wkt field from shape file
ogr_to_csv(fn=scenario_folder + "/route.shp",
           layer_nm="route",
           fields=["TRANSIT_"],
           csv_fn=scenario_folder + "/input/transit_link_route.csv")

# create bike network csv file with wkt field from shape file
ogr_to_csv(fn=scenario_folder + "/input/SANDAG_Bike_Net.shp",
           layer_nm="SANDAG_Bike_Net",
           fields=["ROADSEGID", "RD20FULL", "A", "B", "Distance", "AB_Gain",
                   "BA_Gain", "ABBikeClas", "BABikeClas", "AB_Lanes",
                   "BA_Lanes", "Func_Class", "Bike2Sep", "Bike3Blvd",
                   "SPEED", "ScenicIdx"],
           csv_fn=scenario_folder + "/input/SANDAG_Bike_Net.csv")

# Create csv from Bike_Node .dbf file
dbf_to_csv(dbf_fn=scenario_folder + "/input/SANDAG_Bike_Node.dbf",
           fields=["NodeLev_ID", "Signal"],
           csv_fn=scenario_folder + "/input/SANDAG_Bike_Node.csv")
