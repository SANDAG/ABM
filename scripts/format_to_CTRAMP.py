# -*- coding: utf-8 -*-
"""
Created on Fri Aug 06 14:24:15 2021
script to format survey to CT-ramp inputs to be converted for ASIM
@author: hannah.carson
"""
import pandas as pd
import numpy as np
import os
wd = r'E:\projects\clients\sandag\XBORDER\Tasks\T03 Data_Processing\survey_data_processing'

#read formatted survey files
tours = pd.read_csv(os.path.join(wd,'output','tours_wt.csv'))
trips = pd.read_csv(os.path.join(wd,'output','trips_wt.csv'))
#Exapanded weight column name: 'EXP_WEIGHT_TOTTR'

#CROSSBORDER_STOPFREQUENCY.CSV
"""
Column Name	Description
Purpose	Tour Purpose:
0 = Work
1 = School
2 = Cargo
3 = Shop
4 = Visit
5 = Other
DurationLo	Lower bound of tour duration (0, 4, or 8)
DurationHi	Upper bound of tour duration (4, 8, or 24)
Outbound	Number of stops on the outbound (0, 1, 2, 3+)
Inbound	Number of stops on the inbound (0, 1, 2, 3+)
Percent	Distribution of tours by purpose, duration, number of outbound/inbound stops
"""
tours['Purpose'] = tours['TOUR_PURP_CBM2'] - 1 #index from 0
tours['tourdur'] = tours['TOUR_DUR_HR'] + tours['TOUR_DUR_MIN']/60

def tourdurbound(df):
    x = df['tourdur']
    if x < 4:
        return 0,4
    elif x < 8:
        return 4,8
    else:
        return 8,24
tours[['DurationLo','DurationHi']] = tours.apply(  tourdurbound, axis=1, result_type = 'expand')
tourdur = tours.dropna(subset = ['DurationLo','DurationHi','OUT_STOPS','IN_STOPS'])
tourdur['Outbound'] = tourdur['OUT_STOPS'].astype(np.int64)
tourdur['Inbound'] = tourdur['IN_STOPS'].astype(np.int64)
xbord_stop = tourdur.groupby(['Purpose','DurationLo','DurationHi','Outbound','Inbound'],as_index = False)[['EXP_WEIGHT_TOTTR']].sum()
for purp in xbord_stop.Purpose.unique():
    for durbin in [0,4,8]:
        tot = xbord_stop[(xbord_stop.Purpose == purp) & (xbord_stop.DurationLo == durbin)].EXP_WEIGHT_TOTTR.sum()
        xbord_stop.loc[(xbord_stop.Purpose == purp) & (xbord_stop.DurationLo == durbin),'Percent'] = xbord_stop.loc[(xbord_stop.Purpose == purp) & (xbord_stop.DurationLo == durbin)]['EXP_WEIGHT_TOTTR'] / tot
xbord_stop = xbord_stop[['Purpose','DurationLo','DurationHi','Outbound','Inbound','Percent']]

#format all rows
xbord_stopfr = pd.DataFrame()
purp = [i for i in range(0,6)]*3*4*4
purp.sort()

xbord_stopfr['Purpose'] = pd.Series(purp)
durlo = [0,4,8]*16
durlo.sort()
durlo = durlo*6
xbord_stopfr['DurationLo'] = pd.Series(durlo)

durhi = [4,8,24]*16
durhi.sort()
durhi = durhi*6
xbord_stopfr['DurationHi'] = pd.Series(durhi)

outstop = [i for i in range(4)]*4
outstop.sort()
outstop = outstop*3*6
xbord_stopfr['Outbound'] = pd.Series(outstop)
xbord_stopfr['Inbound'] = pd.Series([i for i in range(4)]*4*3*6)
xbord_stopfr = xbord_stopfr.merge(xbord_stop, how = 'left', on = ['Purpose','DurationLo','DurationHi','Outbound','Inbound'])
xbord_stopfr.fillna(0).to_csv(os.path.join(wd,'output','CTRAMP','crossborder_stopfrequency.csv'),index = False)


"""
CROSSBORDER_STOPPURPOSE.CSV
Column Name	Description
TourPurp	Tour Purpose:
0 = Work
1 = School
2 = Cargo
3 = Shop
4 = Visit
5 = Other
Inbound	Boolean for whether stop is inbound (0=No, 1=Yes)
StopNum	Stop number on tour (1, 2, or 3)
Multiple	Boolean for whether there are multiple τ
StopPurp0	Distribution of Work stops
StopPurp1	Distribution of School stops
StopPurp2	Distribution of Cargo stops
StopPurp3	Distribution of Shopping stops
StopPurp4	Distribution of Visiting stops
StopPurp5	Distribution of Other stops
"""

trippurp = trips.copy()
trippurp = trippurp[trippurp.STOP_DIRECTION.isin(['Inbound stop','Outbound stop'])]
trippurp['TourPurp'] = trippurp['TOUR_PURP_CBM2'] -1 #zero index purpose
trippurp.loc[trippurp.STOP_DIRECTION.isin(["Inbound stop","Border return"]),"Inbound"] = int(1)
trippurp.loc[trippurp.STOP_DIRECTION.isin(["Outbound stop","Primary stop"]),"Inbound"] = int(0)
trippurp = trippurp.dropna(subset = ['Inbound'])
trippurp['outbound_flag'] = np.where(trippurp.STOP_DIRECTION.isin(['Outbound stop','Primary stop']), 1, 0)
trippurp_dir = trippurp.groupby('HH_ID',as_index = False)[['outbound_flag']].sum()
trippurp = trippurp.drop('outbound_flag',axis = 1).merge(trippurp_dir, how = 'inner', on = 'HH_ID')

#get stop num for each leg
trippurp['outbound'] = np.where(trippurp['TRIP_ID'] <= trippurp['outbound_flag'], True, False)
trippurp['StopNum'] = np.where(trippurp['outbound'] == True, trippurp['TRIP_ID'], trippurp['TRIP_ID'] - trippurp['outbound_flag'])
trippurp.loc[trippurp.outbound == False,'StopNum'] = trippurp.loc[trippurp.outbound == False]['StopNum'] -1
trippurp = trippurp.merge(trippurp.rename(columns = {'StopNum':'leg_trips'}).groupby(['HH_ID','Inbound'],as_index = False).leg_trips.max(), how = 'left', on = ['HH_ID','Inbound'])
trippurp['Multiple'] = 0

trippurp.loc[trippurp.leg_trips > 1, 'Multiple'] = 1
purpdict = {'Work':0,
            'School':1,
            'Cargo':2,
            'Shop':3,
            'Visit':4,
            'Other':5,
            'Border':6}
trippurp['TRIPPURP'] = trippurp['DEST_PURP_CBM_STR'].map(purpdict)
trippurp['StopPurp'] = trippurp['TRIPPURP'].map(lambda n: 'StopPurp{}'.format(n))
trippurp = trippurp.merge(tours[['HH_ID','EXP_WEIGHT_TOTTR']], how = 'left', on = 'HH_ID')
for i in range(6):
    trippurp['StopPurp{}'.format(i)] = np.where(trippurp['TRIPPURP'] == i, trippurp['EXP_WEIGHT_TOTTR'], 0)
trippurp2 = trippurp.groupby(['TourPurp','Inbound','StopNum','Multiple'])[['StopPurp{}'.format(i) for i in range(6)]].sum()
trippurp2['Tot'] = trippurp2.sum(axis = 1)
for i in range(6):
    trippurp2['StopPurp{}'.format(i)] = trippurp2['StopPurp{}'.format(i)]/trippurp2['Tot']

trippurp2 = trippurp2.dropna().drop('Tot',axis = 1).reset_index()
trippurp2[trippurp2.StopNum < 4].to_csv(os.path.join(wd,'output','CTRAMP','CROSSBORDER_STOPPURPOSE.CSV'.lower()),index = False)

"""
CROSSBORDER_OUTBOUNDSTOPDURATION.CSV
Column Name	Description
RemainingLow	Lower bound of remaining half hour periods after last scheduled trip:
1 = Before 5:00AM
2 = 5:00AM-5:30AM
3 through 39 is every half hour time slots
40 = After 12:00AM
RemainingHigh	Upper bound of remaining half hour periods after last scheduled trip:
1 = Before 5:00AM
2 = 5:00AM-5:30AM
3 through 39 is every half hour time slots
40 = After 12:00AM
Stop	Stop number on tour (1, 2, or 3)
0	Probability that stop departure is in same period as last outbound trip
1	Probability that stop departure is in last outbound trip period + 1
2	Probability that stop departure is in last outbound trip period + 2
3	Probability that stop departure is in last outbound trip period + 3
4	Probability that stop departure is in last outbound trip period + 4
5	Probability that stop departure is in last outbound trip period + 5
6	Probability that stop departure is in last outbound trip period + 6
7	Probability that stop departure is in last outbound trip period + 7
8	Probability that stop departure is in last outbound trip period + 8
9	Probability that stop departure is in last outbound trip period + 9
10	Probability that stop departure is in last outbound trip period + 10
11	Probability that stop departure is in last outbound trip period + 11"""
bin_dict = {1:1,2:1,3:1,4:1,5:2,6:3,7:4,8:5,9:6,10:7,11:8,12:9,13:10,14:11,15:12,16:13,
            17:14,18:15,19:16,20:17,21:18,22:19,23:20,24:21,25:22,26:23,27:24,28:25,29:26,
            30:27,31:28,32:29,33:30,34:31,35:32,36:33,37:34,38:35,39:36,40:37,41:38,42:39,43:40,
            44:40,45:40,46:40,47:40,48:40}
tripdur = trippurp.copy()
tripdur = tripdur.sort_values(['HH_ID','TRIP_ID'])
tripdur['DEST_DEP_BIN'] = tripdur['ORIG_DEP_BIN'].shift(-1)
tripdur['DEST_DEP_BIN_HHID'] = tripdur['HH_ID'].shift(-1)
tripdur['DEST_DEP_BIN'] = np.where(tripdur['DEST_DEP_BIN_HHID'] != tripdur['HH_ID'], np.nan, tripdur['DEST_DEP_BIN'])
tripdur['TRIPDURBIN'] = tripdur['DEST_DEP_BIN'] - tripdur['DEST_ARR_BIN']
tripdur['rampbin'] = tripdur['ORIG_DEP_BIN'].map(bin_dict)
tripdur['rampbinIn'] = tripdur['ORIG_DEP_BIN'].map(bin_dict)
tripdur['rampbinBorder'] = tripdur['DEST_ARR_BIN'].map(bin_dict)

get_inbound_bin = tripdur[tripdur.Inbound ==1].sort_values(['HH_ID','StopNum']).drop_duplicates(subset = ['HH_ID'])[['HH_ID','rampbinIn']]
get_border_bin = tripdur[tripdur.Inbound == 1].sort_values(['HH_ID','StopNum'], ascending = False).drop_duplicates(subset = ['HH_ID'])[['HH_ID','rampbinBorder']]

tripdur = tripdur.drop(['rampbinIn','rampbinBorder'],axis = 1).merge(get_inbound_bin, how = 'left', on = 'HH_ID')
tripdur = tripdur.merge(get_border_bin, how = 'left', on = 'HH_ID')

#outbound
outtripdur = tripdur.copy()
outtripdur = outtripdur[outtripdur.Inbound ==0]
outtripdur['remaining'] = outtripdur['rampbinIn'] - outtripdur['rampbin']
outtripdur.loc[outtripdur['remaining'] >= 11,'remaining'] = 11
outtripdur['RemainingLow'] = outtripdur['remaining']
outtripdur['RemainingHigh'] = np.where(outtripdur['remaining'] >= 11, 39, outtripdur['remaining'])
outtripdur['Stop'] = outtripdur['StopNum']
outtripdur['TRIPDURBIN'] = np.where(outtripdur['TRIPDURBIN'] >= 11, 11, outtripdur['TRIPDURBIN'])
for i in range(12):
    outtripdur[i] = np.where(outtripdur['TRIPDURBIN'] == i, outtripdur['EXP_WEIGHT_TOTTR'], 0)
    outtripdur['cnt_{}'.format(i)] = np.where(outtripdur['TRIPDURBIN'] == i, 1, 0)
outtripdur2 = outtripdur[outtripdur.Stop <=3].groupby(['RemainingLow','RemainingHigh','Stop'])[[i for i in range(12)] ].sum()
outtripdur2['tot'] = outtripdur2.sum(axis = 1)
for i in range(12):
    outtripdur2[i] = outtripdur2[i]/outtripdur2['tot']

outtripdur2.drop(['tot'],axis = 1).to_csv(os.path.join(wd,'output','CTRAMP','crossborder_outboundstopduration.csv'))

"""
CROSSBORDER_INBOUNDSTOPDURATION.CSV
Column Name	Description
RemainingLow	Lower bound of remaining half hour periods after last scheduled trip:
1 = Before 5:00AM
2 = 5:00AM-5:30AM
3 through 39 is every half hour time slots
40 = After 12:00AM
RemainingHigh	Upper bound of remaining half hour periods after last scheduled trip:
1 = Before 5:00AM
2 = 5:00AM-5:30AM
3 through 39 is every half hour time slots
40 = After 12:00AM
Stop	Stop number on tour (1, 2, or 3)
0	Probability that stop departure period is same as tour arrival period
-1	Probability that stop departure period is tour arrival period - 1
-2	Probability that stop departure period is tour arrival period – 2
-3	Probability that stop departure period is tour arrival period – 3
-4	Probability that stop departure period is tour arrival period – 4
-5	Probability that stop departure period is tour arrival period – 5
-6	Probability that stop departure period is tour arrival period – 6
-7	Probability that stop departure period is tour arrival period - 7
"""
intripdur = tripdur.copy()
intripdur = intripdur[intripdur.Inbound ==1]
intripdur['remaining'] = intripdur['rampbinBorder'] - intripdur['rampbin']
intripdur.loc[intripdur['remaining'] >= 11,'remaining'] = 11
intripdur['RemainingLow'] = intripdur['remaining']
intripdur['RemainingHigh'] = np.where(intripdur['remaining'] >= 11, 39, intripdur['remaining'])
intripdur['Stop'] = intripdur['StopNum']
intripdur['TRIPDURBIN'] = np.where(intripdur['TRIPDURBIN'] >= 11, 11, intripdur['TRIPDURBIN'])
intripdur = intripdur[intripdur['remaining'] >=0]# = np.where(intripdur['TRIPDURBIN'] >= 11, 11, intripdur['TRIPDURBIN'])
for i in range(8):
    intripdur[i] = np.where(intripdur['TRIPDURBIN'] == i, intripdur['EXP_WEIGHT_TOTTR'], 0)
    intripdur['cnt_{}'.format(i)] = np.where(intripdur['TRIPDURBIN'] == i, 1, 0)
intripdur2 = intripdur[intripdur.Stop <=3].groupby(['RemainingLow','RemainingHigh','Stop'])[[i for i in range(8)] ].sum()
intripdur2['tot'] = intripdur2.sum(axis = 1)
for i in range(8):
    intripdur2[i] = intripdur2[i]/intripdur2['tot']

intripdur2.drop(['tot'],axis = 1).to_csv(os.path.join(wd,'output','CTRAMP','crossborder_inboundstopduration.csv'))


"""
CROSSBORDER_TOURPURPOSE_NONSENTRI.CSV
Column Name	Description
Purpose	Tour Purpose:
0 = Work
1 = School
2 = Cargo
3 = Shop
4 = Visit
5 = Other
Percent	Distribution of Tours by tour purpose for non-sentri users
"""

ns_tour = tours.copy()
ns_tour = ns_tour[ns_tour.EXP_CAT.isin([2,3,4,6,7,8,9,10])]
ns_tour = ns_tour[ns_tour.TOUR_PURP_CBM_STR != 'Cargo']

ns_tour = ns_tour.groupby(['Purpose','TOUR_PURP_CBM_STR'])[['EXP_WEIGHT_TOTTR']].sum()
ns_tour['Non-SENTRI'] = ns_tour['EXP_WEIGHT_TOTTR']/ns_tour.EXP_WEIGHT_TOTTR.sum()
ns_tour[['Non-SENTRI']].to_csv(os.path.join(wd,'output','CTRAMP','crossborder_tourpurpose_nonSENTRI.csv'))
"""
CROSSBORDER_TOURPURPOSE_SENTRI.CSV
Column Name	Description
Purpose	Tour Purpose:
0 = Work
1 = School
2 = Cargo
3 = Shop
4 = Visit
5 = Other
Percent	Distribution of Tours by tour purpose for sentri users
"""
s_tour = tours.copy()
s_tour = s_tour[~s_tour.EXP_CAT.isin([2,3,4,6,7,8,9,10])]
s_tour = s_tour[s_tour.TOUR_PURP_CBM_STR != 'Cargo']

s_tour = s_tour.groupby(['Purpose','TOUR_PURP_CBM_STR'])[['EXP_WEIGHT_TOTTR']].sum()
s_tour['SENTRI'] = s_tour['EXP_WEIGHT_TOTTR']/s_tour.EXP_WEIGHT_TOTTR.sum()
s_tour[['SENTRI']].to_csv(os.path.join(wd,'output','CTRAMP','crossborder_tourpurpose_SENTRI.csv'))



"""Tour Purpose Control"""
poe_dict = {1:0,
             2:0,
             3:0,
             4:0,
             5:1,
             6:1,
             7:1,
             8:1,
             9:2,
             10:2}
lane_share = tours.copy()
lane_share['poe'] = lane_share['EXP_CAT'].map(poe_dict)
lane_share = lane_share.groupby(['TOUR_PURP_CBM_STR','poe'],as_index = False)[['EXP_WEIGHT_TOTTR']].sum()
lane_share = lane_share.pivot( index = 'TOUR_PURP_CBM_STR', columns = 'poe', values = 'EXP_WEIGHT_TOTTR')
lane_share = lane_share.drop('Cargo',axis = 0)
for col in [0,1,2]:
    lane_share[col] = lane_share[col]/lane_share[col].sum()
lane_share.to_csv(os.path.join(wd,'output','CTRAMP','crossBorder_tourPurpose_control.csv'))

