#to be run in python2
import openmatrix as omx, numpy as np, tables, os, sys

#get arguments
model_name = sys.argv[1]  
output_dir = sys.argv[2]
  
#Convert trip tables

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    
    
    print("Working on auto %s_%s" %(model_name, period))
    #rename the file
    # os.rename(output_dir + "/%s_%s_%s.omx" % (mode, period, vot), output_dir + "/%s_%s_%s_.omx" % (mode, period, vot))
    trip = omx.open_file(output_dir + "/" + model_name + "//%strips_%s.omx" % (model_name, period), 'r')
    new_trips = omx.open_file(output_dir + "/assignment/%strips_%s.omx" % (model_name, period), 'w')
    
    for table in trip.list_matrices():
        new_trips.create_matrix(name = table, obj=np.array(trip[table]), shape = trip[table].shape, atom=tables.Atom.from_dtype(np.dtype('float64')))

    trip.close()
    new_trips.close()