#to be run in python3

import openmatrix as omx, numpy as np, tables, os, sys, pandas as pd

#get arguments
model_name = sys.argv[1]  
output_dir = sys.argv[2]

if len(model_name.split('.')) > 1:
    mode = 'auto' + model_name.split('.')[0] + 'Trips.' + model_name.split('.')[1]
elif model_name == 'resident':
    mode = 'auto' + 'Trips'
else:
    mode = 'auto' + model_name + 'Trips'

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    for vot in ['low', 'med', 'high']:

        print("Working on setting zone mapping for %s_%s_%s" %(mode, period, vot))
        #rename the file
        os.rename(output_dir + f"/{model_name}/%s_%s_%s.omx" % (mode, period, vot), output_dir + f"/{model_name}/%s_%s_%s_.omx" % (mode, period, vot))
        trip_table_old = omx.open_file(output_dir + f"/{model_name}/%s_%s_%s_.omx" % (mode, period, vot), 'r')
        trip_table = omx.open_file(output_dir + f"/{model_name}/%s_%s_%s.omx" % (mode, period, vot), 'w')

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

#convert transit tables
if len(model_name.split('.')) > 1:
    mode = model_name.split('.')[0] + 'Trips.' + model_name.split('.')[1]
elif model_name == 'resident':
    mode = 'Trips'
else:
    mode = model_name + 'Trips'

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    
        print("Working on setting zone mapping for Tran%s_%s" %(mode, period))
        #rename the file
        # os.rename(output_dir + "/%s_%s_%s.omx" % (mode, period, vot), output_dir + "/%s_%s_%s_.omx" % (mode, period, vot))
        os.rename(output_dir + f"/{model_name}/Tran%s_%s.omx" % (mode, period), output_dir + f"/{model_name}//Tran%s_%s_.omx" % (mode, period))
        trip_table_old = omx.open_file(output_dir + f"/{model_name}/Tran%s_%s_.omx" % (mode, period), 'r')
        trip_table = omx.open_file(output_dir + f"/{model_name}/Tran%s_%s.omx" % (mode, period), 'w')

        for core in trip_table_old.list_matrices():

            mapping_name = trip_table_old.list_mappings()[0]
            zone_mapping = trip_table_old.mapping(mapping_name)
            zones = list(zone_mapping.keys())
            zones_sorted = sorted(list(zone_mapping.keys()))
            pos = [zones.index(zone) for zone in zones_sorted]

            data = trip_table_old[core]
            data = data[:][pos,:][:,pos]

            trip_table[core] = data

        trip_table_old.close()
        trip_table.close()