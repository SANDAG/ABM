# %%
import pandas as pd
import pandana as pdna
import numpy as np
import time
import yaml
import os
import geopandas as gpd
import time
import sys
from datetime import datetime

# %%
path = sys.argv[1]
yml_path = path + "/src/asim/scripts/resident/2zoneSkim_params.yaml"

with open(yml_path, 'r') as f:
    parms = yaml.safe_load(f)

def add_missing_mazs_to_skim_table(centroids, maz_to_maz_walk_cost_out, maz_to_maz_cost):
    """
    Considering that we are using a maximum walk threshold for our skims here, it is possible that some MGRAs, 
    especially in non-urban areas, might not be within threshold distance of other MGRAs. This means that the skim table 
    may not have skims for all MGRAs. 
    
    This function allows identifying those MGRAs that may be missing from 
    the skim table, finds the shortest distance 
    for them (not to themselves), and therefore creating a missig_maz df that now contains  skims for those MGRAs that 
    otherwise did not have skim at all.

    """
    # Identify missing MAZs
    missing_maz = centroids[~centroids['MAZ'].isin(maz_to_maz_walk_cost_out['OMAZ'])][['MAZ']]
    missing_maz = missing_maz.rename(columns={'MAZ': 'OMAZ'})

    # Filter and sort maz_to_maz_cost DataFrame so we have the shortest distance for each OMAZ first
    filtered_maz_to_maz_cost = maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']]
    sorted_maz_to_maz_cost = filtered_maz_to_maz_cost.sort_values('DISTWALK')

    # Group by 'OMAZ' and select the first (shortest dist) 'DMAZ' and 'DISTWALK' for each group
    grouped_maz_to_maz_cost = sorted_maz_to_maz_cost.groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index()

    # Merge the missing MAZs with the grouped maz_to_maz_cost DataFrame
    result = missing_maz.merge(grouped_maz_to_maz_cost, on='OMAZ', how='left')

    return result

print(f"{datetime.now().strftime('%H:%M:%S')} Preparing MAZ-MAZ and MAZ-Stop Connectors...")
startTime = time.time()
#asim_inputs = os.path.join(path, "ASIM_INPUTS")
model_inputs = os.path.join(path, "input")
sf = os.path.join(model_inputs, parms['mmms']['shapefile_name'])
sf_node = os.path.join(model_inputs, parms['mmms']['shapefile_node_name'])
sf_maz = os.path.join(model_inputs, parms['mazfile_name'])

max_maz_maz_walk_dist_feet = int(parms['mmms']['max_maz_maz_walk_dist_feet'])
max_maz_maz_bike_dist_feet = int(parms['mmms']['max_maz_maz_bike_dist_feet'])
max_maz_local_bus_stop_walk_dist_feet = int(parms['mmms']['max_maz_local_bus_stop_walk_dist_feet'])
max_maz_premium_transit_stop_walk_dist_feet = int(parms['mmms']['max_maz_premium_transit_stop_walk_dist_feet'])

# %%
walk_speed_mph  = float(parms['mmms']["walk_speed_mph"])
drive_speed_mph = float(parms['mmms']["drive_speed_mph"])

print(f"{datetime.now().strftime('%H:%M:%S')} Reading Allstreet Network...")
nodes = gpd.read_file(sf_node)
nodes = nodes.to_crs(epsg=2230)
nodes= nodes.set_index("NodeLev_ID")
nodes['X'] = nodes.geometry.x
nodes['Y'] = nodes.geometry.y

links = gpd.read_file(sf)
links = links.to_crs(epsg=2230)

# %%
print(f"{datetime.now().strftime('%H:%M:%S')} Building Network...")

net = pdna.Network(nodes.X, nodes.Y, links[parms['mmms']['mmms_link_ref_id']], links[parms['mmms']["mmms_link_nref_id"]], links[[parms['mmms']["mmms_link_len"]]]/5280.0, twoway=True)

print(f"{datetime.now().strftime('%H:%M:%S')} Assign Nearest Network Node to MAZs and stops")
maz_closest_network_node_id = links[links[parms['mmms']["mmms_link_ref_id"]].isin(list(nodes[nodes[parms['maz_shape_maz_id']]!=0].index))][[parms['mmms']["mmms_link_ref_id"],parms['mmms']["mmms_link_nref_id"]]]

#assign the node at the other end of centroid connector as closest network node
centroids = pd.DataFrame()
centroids['X'] = nodes[nodes[parms['maz_shape_maz_id']]!=0].X
centroids['Y'] = nodes[nodes[parms['maz_shape_maz_id']]!=0].Y
centroids['MAZ'] = nodes[nodes[parms['maz_shape_maz_id']]!=0].MGRA
centroids['MAZ_centroid_id'] = nodes[nodes[parms['maz_shape_maz_id']]!=0].index

centroids = pd.merge(centroids, maz_closest_network_node_id, left_on='MAZ_centroid_id', right_on=parms['mmms']["mmms_link_ref_id"], how='left')
centroids = centroids.rename(columns={parms['mmms']["mmms_link_nref_id"]:'network_node_id'})

centroids["network_node_x"] = nodes["X"].loc[centroids["network_node_id"]].tolist()
centroids["network_node_y"] = nodes["Y"].loc[centroids["network_node_id"]].tolist()

# %%
## read transit stop and route file (KK) ============
stops = pd.read_csv(os.path.join(model_inputs, parms['stop_attributes']['file']))
routes = pd.read_csv(os.path.join(model_inputs, parms['route_attributes']['file']))
routes = routes.filter(['Route_ID','Route_Name', 'Mode'])

# add mode from route file & convert lat.long to stateplane(KK)=====
stops = stops.merge(routes,  left_on='Route_ID', right_on='Route_ID') #

gpd_stops = gpd.GeoDataFrame(stops, geometry = gpd.points_from_xy(stops.Longitude, stops.Latitude, crs='epsg:4326'))
gpd_stops = gpd_stops.to_crs('epsg:2230')

pd.set_option('display.float_format', lambda x: '%.9f' % x)

gpd_stops['Longitude'] = gpd_stops['geometry'].x
gpd_stops['Latitude'] = gpd_stops['geometry'].y

stops["network_node_id"] = net.get_node_ids(gpd_stops['Longitude'], gpd_stops['Latitude'])
stops["network_node_x"] = nodes["X"].loc[stops["network_node_id"]].tolist()
stops["network_node_y"] = nodes["Y"].loc[stops["network_node_id"]].tolist()
# B: Future BRT, E: regular express, premium express, sprinter\trolley, and coaster bus, L: Local bus, N: None. There should be no Ns
stops['mode'] = np.where(stops['Mode']==10,'L',
                np.where((stops['Mode']==4) | (stops['Mode']==5) | (stops['Mode']==8) | (stops['Mode']==9) | (stops['Mode']==6) | (stops['Mode']==7), 'E',
                'N'))

# %%
# MAZ-to-MAZ Walk
print(f"{datetime.now().strftime('%H:%M:%S')} Build MAZ to MAZ Walk Table...")
o_m = np.repeat(centroids['MAZ'].tolist(), len(centroids))
d_m = np.tile(centroids['MAZ'].tolist(), len(centroids))
o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(centroids))
d_m_nn = np.tile(centroids['network_node_id'].tolist(), len(centroids))
o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(centroids))
o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(centroids))
d_m_x = np.tile(centroids['network_node_x'].tolist(), len(centroids))
d_m_y = np.tile(centroids['network_node_y'].tolist(), len(centroids))

maz_to_maz_cost = pd.DataFrame({"OMAZ":o_m, "DMAZ":d_m, "OMAZ_NODE":o_m_nn, "DMAZ_NODE":d_m_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DMAZ_NODE_X":d_m_x, "DMAZ_NODE_Y":d_m_y})
maz_to_maz_cost["DISTWALK"] = maz_to_maz_cost.eval("(((OMAZ_NODE_X-DMAZ_NODE_X)**2 + (OMAZ_NODE_Y-DMAZ_NODE_Y)**2)**0.5) / 5280.0")
maz_to_maz_cost = maz_to_maz_cost[maz_to_maz_cost["OMAZ"] != maz_to_maz_cost["DMAZ"]]

print(f"{datetime.now().strftime('%H:%M:%S')} Remove MAZ to MAZ Pairs Beyond Max Walk Distance...")
maz_to_maz_walk_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0].copy()
print(f"{datetime.now().strftime('%H:%M:%S')} Get Shortest Path Length...")
maz_to_maz_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_maz_walk_cost["OMAZ_NODE"], maz_to_maz_walk_cost["DMAZ_NODE"])
maz_to_maz_walk_cost_out = maz_to_maz_walk_cost[maz_to_maz_walk_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0]
missing_maz = add_missing_mazs_to_skim_table(centroids, maz_to_maz_walk_cost_out, maz_to_maz_cost)
maz_maz_walk_output = maz_to_maz_walk_cost_out[["OMAZ","DMAZ","DISTWALK"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ'])
#creating fields as required by the TNC routing Java model. "actual" is walk time in minutes
maz_maz_walk_output[['i', 'j']] = maz_maz_walk_output[['OMAZ', 'DMAZ']]
maz_maz_walk_output['actual'] = maz_maz_walk_output['DISTWALK'] / walk_speed_mph * 60.0

# find intrazonal distance by averaging the closest 3 zones and then half it
maz_maz_walk_output = maz_maz_walk_output.sort_values(['OMAZ', 'DISTWALK'])
maz_maz_walk_output.set_index(['OMAZ', 'DMAZ'], inplace=True)
unique_omaz = maz_maz_walk_output.index.get_level_values(0).unique()
# find the average of the closest 3 zones
means = maz_maz_walk_output.loc[(unique_omaz, slice(None)), 'DISTWALK'].groupby(level=0).head(3).groupby(level=0).mean()
intra_skims = pd.DataFrame({
    'OMAZ': unique_omaz,
    'DMAZ': unique_omaz,
    'DISTWALK': means.values/2,
    'i': unique_omaz,
    'j': unique_omaz,
    'actual': (means.values/walk_speed_mph * 60.0) / 2
}).set_index(['OMAZ', 'DMAZ'])
maz_maz_walk_output = pd.concat([maz_maz_walk_output, intra_skims], axis=0)
# write output
print(f"{datetime.now().strftime('%H:%M:%S')} Write Results...")
maz_maz_walk_output.to_csv(path + '/output/skims/' + parms['mmms']["maz_maz_walk_output"])
del(missing_maz)

# %%
# MAZ-to-MAZ Bike
print(f"{datetime.now().strftime('%H:%M:%S')} Build Maz To Maz Bike Table...") # same table above
maz_to_maz_bike_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_bike_dist_feet / 5280.0].copy()
print(f"{datetime.now().strftime('%H:%M:%S')} Get Shortest Path Length...")
maz_to_maz_bike_cost["DISTBIKE"] = net.shortest_path_lengths(maz_to_maz_bike_cost["OMAZ_NODE"], maz_to_maz_bike_cost["DMAZ_NODE"])
maz_to_maz_bike_cost_out = maz_to_maz_bike_cost[maz_to_maz_bike_cost["DISTBIKE"] <= max_maz_maz_bike_dist_feet / 5280.0]
_missing_maz = add_missing_mazs_to_skim_table(centroids, maz_to_maz_bike_cost_out, maz_to_maz_cost)
missing_maz = _missing_maz.rename(columns = {'DISTWALK': 'DISTBIKE'})
print(f"{datetime.now().strftime('%H:%M:%S')} Write Results...")
maz_to_maz_bike_cost_out[["OMAZ","DMAZ","DISTBIKE"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(path + '/output/skims/' + parms['mmms']["maz_maz_bike_output"], index=False)
del(missing_maz)

# %%
#MAZ-to-stop Walk
print(f"{datetime.now().strftime('%H:%M:%S')} Build Maz To stop Walk Table...")
o_m = np.repeat(centroids['MAZ'].tolist(), len(stops))
o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(stops))
d_t = np.tile(stops[parms['stop_attributes']['id_field']].tolist(), len(centroids))
d_t_nn = np.tile(stops['network_node_id'].tolist(), len(centroids))
o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(stops))
o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(stops))
d_t_x = np.tile(stops['network_node_x'].tolist(), len(centroids))
d_t_y = np.tile(stops['network_node_y'].tolist(), len(centroids))
mode = np.tile(stops['mode'].tolist(), len(centroids))

maz_to_stop_cost = pd.DataFrame({"MAZ":o_m, "stop":d_t, "OMAZ_NODE":o_m_nn, "DSTOP_NODE":d_t_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y,
                                "DSTOP_NODE_X":d_t_x, "DSTOP_NODE_Y":d_t_y, "MODE": mode}) # "DSTOP_CANPNR":d_t_canpnr,


maz_to_stop_cost["DISTANCE"] = maz_to_stop_cost.eval("(((OMAZ_NODE_X-DSTOP_NODE_X)**2 + (OMAZ_NODE_Y-DSTOP_NODE_Y)**2)**0.5) / 5280.0")

# B: Future BRT, E: regular express, premium express, sprinter\trolley, and coaster bus, L: Local bus, N: None. There should be no Ns
maz_to_stop_walk_cost = maz_to_stop_cost[(maz_to_stop_cost["DISTANCE"] <= max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'L') | 
                                            (maz_to_stop_cost["DISTANCE"] <= max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'E')].copy()

print(f"{datetime.now().strftime('%H:%M:%S')} Get Shortest Path Length...")

maz_to_stop_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_stop_walk_cost["OMAZ_NODE"], maz_to_stop_walk_cost["DSTOP_NODE"])


print(f"{datetime.now().strftime('%H:%M:%S')} Remove Maz Stop Pairs Beyond Max Walk Distance...")
maz_to_stop_walk_cost_out = maz_to_stop_walk_cost[(maz_to_stop_walk_cost["DISTWALK"] <= max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'L') | 
                                                    (maz_to_stop_walk_cost["DISTWALK"] <= max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'E')].copy()


maz_stop_walk0 = pd.DataFrame(centroids['MAZ'])
maz_stop_walk0 = maz_stop_walk0.rename(columns = {'MAZ': 'maz'})
maz_stop_walk0['maz'] = maz_stop_walk0['maz'].astype('int')
maz_stop_walk0.sort_values(by=['maz'], inplace=True)

modes = {"L": "local_bus", "E": "premium_transit"}
for mode, output in modes.items():
    max_walk_dist = parms['mmms']['max_maz_' + output + '_stop_walk_dist_feet'] / 5280.0
    maz_to_stop_walk_cost_out_mode = maz_to_stop_walk_cost_out[maz_to_stop_walk_cost_out['MODE'].str.contains(mode)].copy()
    maz_to_stop_walk_cost_out_mode.loc[:, 'MODE'] = mode
    maz_to_stop_walk_cost = maz_to_stop_walk_cost_out_mode.sort_values(['MAZ', 'stop'])
    del(maz_to_stop_walk_cost_out_mode)
    maz_stop_walk = maz_to_stop_walk_cost[maz_to_stop_walk_cost.MODE==mode].groupby('MAZ')['DISTWALK'].min().reset_index()
    maz_stop_walk.loc[maz_stop_walk['DISTWALK'] > max_walk_dist, 'DISTWALK'] = np.nan
    #maz_stop_walk["walk_time"] = maz_stop_walk["DISTWALK"].apply(lambda x: x / parms['mmms']['walk_speed_mph'] * 60.0)
    maz_stop_walk.rename({'MAZ': 'maz', 'DISTWALK': 'walk_dist_' + output}, axis='columns', inplace=True)
    maz_stop_walk0 = maz_stop_walk0.merge(maz_stop_walk, left_on='maz', right_on='maz', how='outer')
    maz_stop_walk0['walk_dist_' + output].fillna(999999, inplace = True)

maz_stop_walk0.sort_values(by=['maz'], inplace=True)
print(f"{datetime.now().strftime('%H:%M:%S')} Write Results...")
maz_stop_walk0.to_csv(path + '/output/skims/' + "maz_stop_walk.csv", index=False)

# %%



