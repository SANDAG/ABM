# %%
import pandas as pd
import numpy as np
import geopandas as gpd
import openmatrix as omx
import os
import sys

path = sys.argv[1]

# %%
pmsa = gpd.read_file(os.path.join(path,'input', 'pmsa_geoms.shp'))
taz = gpd.read_file(os.path.join(path,'input','TAZ15.shp'))

# %%
taz['geometry'] = taz['geometry'].representative_point()

xwalk = gpd.sjoin(taz,pmsa, op = 'within')
xwalk[['TAZ','pseudomsa']].to_csv(os.path.join(path,'output', 'skims','taz_pmsa_xwalk.csv'), index = False)

# %%
xwalk = pd.read_csv(os.path.join(path,'output', 'skims','taz_pmsa_xwalk.csv'))#, usecols = ['taz','pseudomsa'])
xwalk = xwalk.sort_values('TAZ').drop_duplicates()

with omx.open_file(os.path.join(path,'output','skims','dest_pmsa.omx'), 'w') as pmsa_omx:

    filler = [0 for i in range(12)] + xwalk['pseudomsa'].tolist()
    pmsa_omx['pmsa_dest'] = np.array([filler]*4947)
    pmsa_omx.create_mapping('taz',np.arange(1,4948))



