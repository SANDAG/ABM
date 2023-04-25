import openmatrix as omx, numpy as np, tables, os, sys

#get arguments
model_name = sys.argv[1]  
output_dir = sys.argv[2]
  
#Convert auto tables
if len(model_name.split('.')) > 1:
    mode = 'auto' + model_name.split('.')[0] + 'Trips.' + model_name.split('.')[1]
elif model_name == 'resident':
    mode = 'auto' + 'Trips'
else:
    mode = 'auto' + model_name + 'Trips'

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    
    for vot in ['low', 'med', 'high']:
        print("Working on auto %s_%s_%s" %(mode, period, vot))
        #rename the file
        # os.rename(output_dir + "/%s_%s_%s.omx" % (mode, period, vot), output_dir + "/%s_%s_%s_.omx" % (mode, period, vot))
        skim = omx.open_file(output_dir + "/" + model_name + "/%s_%s_%s.omx" % (mode, period, vot), 'r')
        new_skim = omx.open_file(output_dir + "/%s_%s_%s.omx" % (mode, period, vot), 'w')

        #transfer zone mapping from original skim
        mapping_name = skim.list_mappings()[0]
        zone_mapping = skim.mapping(mapping_name).items()
        sorted(zone_mapping, key=lambda x: x[1])
        omx_zones = [x[0] for x in zone_mapping]
        new_skim.create_mapping(mapping_name, omx_zones)
        
        for table in skim.list_matrices():
            new_skim.create_matrix(name = table, obj=np.array(skim[table]), shape = skim[table].shape, atom=tables.Atom.from_dtype(np.dtype('float64')))
            
        skim.close()
        new_skim.close()

#convert transit tables
if len(model_name.split('.')) > 1:
    mode = model_name.split('.')[0] + 'Trips.' + model_name.split('.')[1]
elif model_name == 'resident':
    mode = 'Trips'
else:
    mode = model_name + 'Trips'

for period in ['EA', 'AM', 'MD', 'PM', 'EV']:
    print("Working on transit %s_%s" %(mode, period))
    #rename the file
    # os.rename(r"C:\abm_runs\abm3_dev\emme_setup\output\Tran%s_%s.omx" % (mode, period), r"C:\abm_runs\abm3_dev\emme_setup\output\Tran%s_%s_.omx" % (mode, period))
    skim = omx.open_file(output_dir + "/" + model_name + "/Tran%s_%s.omx" % (mode, period), 'r')
    new_skim = omx.open_file(output_dir + "/Tran%s_%s.omx" % (mode, period), 'w')
    
    #transfer zone mapping from original skim
    mapping_name = skim.list_mappings()[0]
    zone_mapping = skim.mapping(mapping_name).items()
    sorted(zone_mapping, key=lambda x: x[1])
    omx_zones = [x[0] for x in zone_mapping]
    new_skim.create_mapping(mapping_name, omx_zones)
    
    for table in skim.list_matrices():
        new_skim.create_matrix(name = table, obj=np.array(skim[table]), shape = skim[table].shape, atom=tables.Atom.from_dtype(np.dtype('float64')))

    skim.close()
    new_skim.close()



