# -*- coding: utf-8 -*-
"""
Created on Mon Jan 17 12:06:45 2022
Updated on Wed June 8 17:32:15 2022

author: Andrew Rohne, Ted Lin, Kyeongsu Kim
"""

# import sys
import pandas as pd
import pandana as pdna
import numpy as np
import time
import yaml
import os
import geopandas as gpd
import time


def getNodes(sf_node):
    #         output_node = gpd.read_file(sf_node)
    output_node = sf_node.copy()
    output_node_df = pd.DataFrame(output_node)
    output_node.set_index("NodeLev_ID", inplace=True)
    output_node['X'] = output_node['XCOORD'] #.astype(str).apply(lambda x: ','.join(x.fillna('')), axis=1)
    output_node['Y'] = output_node['YCOORD'] #.astype(str).apply(lambda x: ','.join(x.fillna('')), axis=1)
    # nodes=output_node
    return(output_node)

def getLinks(sf, parms):
    linksDict = {}
    outputD = []
    # gpd_sf = gpd.read_file(sf)
    gpd_sf = sf.copy()
    for idx, record in gpd_sf.iterrows(): # idx,
        referenceNode = record[parms['mmms']["mmms_link_ref_id"]]
        nreferenceNode = record[parms['mmms']["mmms_link_nref_id"]]
        linkid = record[parms['mmms']["mmms_link_id"]]
        length = record[parms['mmms']["mmms_link_len"]] / 5280.0
        outputD.append({'FNODE': referenceNode, 'TNODE': nreferenceNode, 'LENGTH': length})
        outputD.append({'FNODE': nreferenceNode, 'TNODE': referenceNode, 'LENGTH': length})
    output = pd.DataFrame(outputD)
    # links = output
    return(output)

def getCentroids(sf_maz, parms):
    output = pd.DataFrame(columns=['MAZ', 'X','Y'])
    # centr_sf = gpd.read_file(sf_maz)
    centr_sf = sf_maz.copy()
    centr_X = centr_sf.centroid.x
    centr_Y = centr_sf.centroid.y

    centr_df = pd.DataFrame(centr_sf)
    output['MAZ'] = centr_df['MGRA']
    output['X'] = centr_X
    output['Y'] = centr_Y
    # centroids = output
    return(output)

# class CMapMazStop():
#     __MODELLER_NAMESPACE__ = "sandag"
#     tool_run_msg = ""

#     def __init__(self):
#         print("Preparing MAZ-MAZ and MAZ-Stop Connectors...")

#     def __call__(self, parms):
# path = "E:\\Projects\\Clients\\SANDAG\\MAZ_WalkTime"
path = "C:\\Users\\kyeongsu.kim\\OneDrive - Resource Systems Group, Inc\\Documents\\GitHub\\cmap_abm"

yml_path = os.path.join(path, "sandag_maz_stop_params.yaml")
with open(yml_path, 'r') as f:
    parms = yaml.safe_load(f)

print(f"{time.localtime()} Preparing MAZ-MAZ and MAZ-Stop Connectors...")
startTime = time.time()
asim_inputs = os.path.join(path, "ASIM_INPUTS")
model_inputs = os.path.join(path, "INPUT_FOLDER")
sf = os.path.join(model_inputs, parms['mmms']['shapefile_name'])
sf_node = os.path.join(model_inputs, parms['mmms']['shapefile_node_name'])
sf_maz = os.path.join(model_inputs, parms['mazfile_name'])

max_maz_maz_walk_dist_feet = int(parms['mmms']['max_maz_maz_walk_dist_feet'])
max_maz_maz_bike_dist_feet = int(parms['mmms']['max_maz_maz_bike_dist_feet'])
max_maz_local_bus_stop_walk_dist_feet = int(parms['mmms']['max_maz_local_bus_stop_walk_dist_feet'])
max_maz_premium_transit_stop_walk_dist_feet = int(parms['mmms']['max_maz_premium_transit_stop_walk_dist_feet'])
max_maz_future_BRT_stop_walk_dist_feet = int(parms['mmms']['max_maz_future_BRT_stop_walk_dist_feet'])

walk_speed_mph  = float(parms['mmms']["walk_speed_mph"])
drive_speed_mph = float(parms['mmms']["drive_speed_mph"])
print(f"{time.localtime()} Getting Nodes...")
# nodes = getNodes(gpd.read_file(sf_node), parms) # changed from sf
nodes = getNodes(gpd.read_file(sf_node)) # changed from sf                
print(f"{time.localtime()} Getting Links...")
links = getLinks(gpd.read_file(sf), parms)
print(f"{time.localtime()} Building MAZ Centroids...")
centroids = getCentroids(gpd.read_file(sf_maz), parms)
print(f"{time.localtime()} Building Network...")

net = pdna.Network(nodes["X"], nodes["Y"], links["FNODE"], links["TNODE"], links[["LENGTH"]], twoway=True)
print(f"{time.localtime()} Assign Nearest Network Node to MAZs and stops")
centroids["network_node_id"] = net.get_node_ids(centroids["X"], centroids["Y"])
centroids["network_node_x"] = nodes["X"].loc[centroids["network_node_id"]].tolist()
centroids["network_node_y"] = nodes["Y"].loc[centroids["network_node_id"]].tolist()

## read transit stop and route file (KK) ============
stops = pd.read_csv(os.path.join(model_inputs, parms['stop_attributes']['file']))
routes = pd.read_csv(os.path.join(model_inputs, parms['route_attributes']['file']))
routes = routes.filter(['Route_ID','Route_Name', 'Mode'])

# add mode from route file & convert lat.long to stateplane(KK)=====
stops = stops.merge(routes,  left_on='Route_ID', right_on='Route_ID') # 

stops["Longitude1"] = stops["Longitude"]/1000000
stops["Latitude1"] = stops["Latitude"]/1000000

gpd_stops = gpd.GeoDataFrame(stops, geometry = gpd.points_from_xy(stops.Longitude1, stops.Latitude1))
gpd_stops = gpd_stops.set_crs('epsg:4326')
gpd_stops['geometry'] = gpd_stops['geometry'].to_crs(epsg=6426)

pd.set_option('display.float_format', lambda x: '%.9f' % x)

gpd_stops['Longitude'] = gpd_stops.centroid.x 
gpd_stops['Latitude'] = gpd_stops.centroid.y 

stops["network_node_id"] = net.get_node_ids(stops[parms['stop_attributes']['x_field']], stops[parms['stop_attributes']['y_field']])
stops["network_node_x"] = nodes["X"].loc[stops["network_node_id"]].tolist()
stops["network_node_y"] = nodes["Y"].loc[stops["network_node_id"]].tolist()
# B: Future BRT, E: regular express, premium express, sprinter\trolley, and coaster, L: Local bus, N: None. There should be no Ns
stops['mode'] = np.where(stops['Mode']==10,'L',
                np.where((stops['Mode']==4) | (stops['Mode']==5) | (stops['Mode']==8) | (stops['Mode']==9), 'E',
                np.where((stops['Mode']==6) | (stops['Mode']==7),'B','N')))
print(f"{time.localtime()} Build MAZ to MAZ Walk Table...")
o_m = np.repeat(centroids['MAZ'].tolist(), len(centroids))
d_m = np.tile(centroids['MAZ'].tolist(), len(centroids))
o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(centroids))
d_m_nn = np.tile(centroids['network_node_id'].tolist(), len(centroids))
o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(centroids))
o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(centroids))
d_m_x = np.tile(centroids['network_node_x'].tolist(), len(centroids))
d_m_y = np.tile(centroids['network_node_y'].tolist(), len(centroids))
#
# MAZ-to-MAZ Walk
#
maz_to_maz_cost = pd.DataFrame({"OMAZ":o_m, "DMAZ":d_m, "OMAZ_NODE":o_m_nn, "DMAZ_NODE":d_m_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DMAZ_NODE_X":d_m_x, "DMAZ_NODE_Y":d_m_y})
maz_to_maz_cost["DISTWALK"] = maz_to_maz_cost.eval("(((OMAZ_NODE_X-DMAZ_NODE_X)**2 + (OMAZ_NODE_Y-DMAZ_NODE_Y)**2)**0.5) / 5280.0")
maz_to_maz_cost = maz_to_maz_cost[maz_to_maz_cost["OMAZ"] != maz_to_maz_cost["DMAZ"]]

print(f"{time.localtime()} Remove MAZ to MAZ Pairs Beyond Max Walk Distance...")
maz_to_maz_walk_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0].copy()
print(f"{time.localtime()} Get Shortest Path Length...")
maz_to_maz_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_maz_walk_cost["OMAZ_NODE"], maz_to_maz_walk_cost["DMAZ_NODE"])
maz_to_maz_walk_cost_out = maz_to_maz_walk_cost[maz_to_maz_walk_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0]
missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_walk_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index(), on = 'OMAZ', how = 'left')
print(f"{time.localtime()} Write Results...")
maz_to_maz_walk_cost_out[["OMAZ","DMAZ","DISTWALK"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmms']["maz_maz_walk_output"]), index=False)
del(missing_maz)
#
# MAZ-to-MAZ Bike
#
print(f"{time.localtime()} Build Maz To Maz Bike Table...") # same table above
maz_to_maz_bike_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_bike_dist_feet / 5280.0].copy()
print(f"{time.localtime()} Get Shortest Path Length...")
maz_to_maz_bike_cost["DISTBIKE"] = net.shortest_path_lengths(maz_to_maz_bike_cost["OMAZ_NODE"], maz_to_maz_bike_cost["DMAZ_NODE"])
maz_to_maz_bike_cost_out = maz_to_maz_bike_cost[maz_to_maz_bike_cost["DISTBIKE"] <= max_maz_maz_bike_dist_feet / 5280.0]
missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_bike_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index().rename(columns = {'DISTWALK': 'DISTBIKE'}), on = 'OMAZ', how = 'left')
print(f"{time.localtime()} Write Results...")
maz_to_maz_bike_cost_out[["OMAZ","DMAZ","DISTBIKE"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmms']["maz_maz_bike_output"]), index=False)
del(missing_maz)

# MAZ-to-stop Walk
#
print(f"{time.localtime()} Build Maz To stop Walk Table...")
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
                                            (maz_to_stop_cost["DISTANCE"] <= max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'E') | 
                                            (maz_to_stop_cost["DISTANCE"] <= max_maz_future_BRT_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'B')].copy()
print(f"{time.localtime()} Get Shortest Path Length...")
maz_to_stop_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_stop_walk_cost["OMAZ_NODE"], maz_to_stop_walk_cost["DSTOP_NODE"])
print(f"{time.localtime()} Remove Maz Stop Pairs Beyond Max Walk Distance...")

maz_to_stop_walk_cost_out = maz_to_stop_walk_cost[(maz_to_stop_walk_cost["DISTANCE"] <= max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'L') | 
                                                    (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_premium_transit_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'E') | 
                                                    (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_future_BRT_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'B')].copy()

maz_stop_walk0 = pd.DataFrame(gpd.read_file(sf_maz))
maz_stop_walk0 = maz_stop_walk0.drop(columns =['geometry'])
maz_stop_walk0 = maz_stop_walk0.rename(columns = {'MGRA': 'maz'})
maz_stop_walk0.sort_values(by=['maz'])

modes = {"L": "local_bus", "E": "premium_transit"} #,, "B": "future_BRT"
for mode, output in modes.items():
    max_walk_dist = parms['mmms']['max_maz_' + output + '_stop_walk_dist_feet'] / 5280.0
    maz_to_stop_walk_cost_out_mode = maz_to_stop_walk_cost_out[maz_to_stop_walk_cost_out['MODE'].str.contains(mode)].copy()
    maz_to_stop_walk_cost_out_mode.loc[:, 'MODE'] = mode
    # in case straight line distance is less than max and actual distance is greater than max (e.g., street net), set actual distance to max
    maz_to_stop_walk_cost_out_mode['DISTWALK'] = maz_to_stop_walk_cost_out_mode['DISTWALK'].clip(upper=max_walk_dist)
    missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_stop_walk_cost_out_mode['MAZ'])]['MAZ']).merge(maz_to_stop_cost.sort_values('DISTANCE').groupby(['MAZ', 'MODE']).agg({'stop': 'first', 'DISTANCE': 'first'}).reset_index(), on = 'MAZ', how = 'left')
    maz_to_stop_walk_cost = maz_to_stop_walk_cost_out_mode.append(missing_maz.rename(columns = {'DISTANCE': 'DISTWALK'})).sort_values(['MAZ', 'stop'])
    del(maz_to_stop_walk_cost_out_mode)
    del(missing_maz)
    maz_stop_walk = maz_to_stop_walk_cost[maz_to_stop_walk_cost.MODE==mode].groupby('MAZ')['DISTWALK'].min().reset_index()
    maz_stop_walk.loc[maz_stop_walk['DISTWALK'] > max_walk_dist, 'DISTWALK'] = np.nan
    #maz_stop_walk["walk_time"] = maz_stop_walk["DISTWALK"].apply(lambda x: x / parms['mmms']['walk_speed_mph'] * 60.0)
    maz_stop_walk['DISTWALK'].fillna(9999, inplace = True)
    maz_stop_walk.rename({'MAZ': 'maz', 'DISTWALK': 'walk_dist_' + output}, axis='columns', inplace=True)
    maz_stop_walk0 = maz_stop_walk0.merge(maz_stop_walk, left_on='maz', right_on='maz')
    
maz_stop_walk0.sort_values(by=['maz'])
print(f"{time.localtime()} Write Results...")
maz_stop_walk0.to_csv(os.path.join(asim_inputs, "maz_stop_walk_output.csv"), index=False)
print(f"{time.localtime()} Completed MAZ-MAZ and MAZ-stop Connectors in {time.strftime('%H:%M:%S', time.gmtime(time.time() - startTime))}")



