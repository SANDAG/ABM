# %%
# -*- coding: utf-8 -*-
"""
Created on Tue Nov  9 15:33:52 2021

@author: hannah.carson
@edited by: ali.etezady
"""

import pandas as pd
import numpy as np
import os
import openmatrix as omx
import sys

# %%
path = sys.argv[1]
scenario_year = sys.argv[2]

"""
print('Creating myfile.omx')
myfile = omx.open_file('myfile.omx','w')   # use 'a' to append/edit an existing file

# Write to the file.
myfile['m1'] = ones
myfile['m2'] = twos
myfile['m3'] = ones + twos           # numpy array math is fast
myfile.close()

# Open an OMX file for reading only
print('Reading myfile.omx')
myfile = omx.open_file('myfile.omx')

print ('Shape:', myfile.shape())                 # (100,100)
print ('Number of tables:', len(myfile))         # 3
print ('Table names:', myfile.list_matrices())   # ['m1','m2',',m3']
"""


# %%
xwalk = pd.read_csv(os.path.join(path,'input','mgra15_based_input{}.csv'.format(scenario_year)), usecols = ['taz','zip09'])
xwalk['destination'] = 0
xwalk.loc[xwalk['zip09'] == 92037, 'destination'] = 1
xwalk.loc[xwalk['zip09'] == 92101, 'destination'] = 2
xwalk.loc[xwalk['zip09'] == 92108, 'destination'] = 3
xwalk = xwalk.sort_values('taz').drop_duplicates(subset = 'taz')

# %%
with omx.open_file(os.path.join(path,'output','skims','dest_poi.omx'),'w') as pmsa_omx:

# pmsa_omx = omx.open_file(os.path.join(wd,'output','dest_poi.omx'),'w')

    filler = [0 for i in range(12)] + xwalk['destination'].tolist()
    print(len(filler))
    print(np.array([filler]*4947).shape)
    pd.DataFrame(np.array([filler]*4947), columns = [i for i in range(1,4948)]).to_csv(os.path.join(path,'output','skims','dest_poi.omx.csv'),index = False)
    pmsa_omx['poi_dest'] = np.array([filler]*4947)
    pmsa_omx.create_mapping('taz',np.arange(1,4948))
    # pmsa_omx['pmsa_dest'].attrs
    # pmsa_omx.close()