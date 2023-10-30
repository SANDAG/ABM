#to be run in python3

import openmatrix as omx, numpy as np, tables, os, sys, pandas as pd

#get arguments
model_name = sys.argv[1]  
output_dir = sys.argv[2]

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    
    print("Working on setting zone mapping for cv trips_%s.omx" %(period))
    # check if the old file exists, if so, delete it
    if os.path.exists(output_dir + "/%s/trips_%s_.omx" % (model_name, period)):
        os.remove(output_dir + "/%s/trips_%s_.omx" % (model_name, period))
    # rename the file
    os.rename(output_dir + "/%s/trips_%s.omx" % (model_name, period), output_dir + "/%s/trips_%s_.omx" % (model_name, period))
    trip_table_old = omx.open_file(output_dir + "/%s/trips_%s_.omx" % (model_name, period), 'r')
    trip_table = omx.open_file(output_dir + "/%s/trips_%s.omx" % (model_name, period), 'w')

    for core in trip_table_old.list_matrices():

        mapping_name = trip_table_old.list_mappings()[0]
        zone_mapping = trip_table_old.mapping(mapping_name)
        zones = list(zone_mapping.keys())
        zones_sorted = sorted(zones)
        pos = [zones.index(zone) for zone in zones_sorted]

        data = trip_table_old[core]
        data = data[:][pos,:][:,pos]

        trip_table[core] = data

    trip_table_old.close()
    trip_table.close()