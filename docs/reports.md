## Report Files Directory

The report files are produced by the SANDAG travel model. 


### External-External Model Trip List (.\eetrip.csv)

<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tripID
   </td>
   <td>unique identifier of trip
   </td>
  </tr>
  <tr>
   <td>departTimeFiveTod
   </td>
   <td>trip departure time ABM five time of day period
   </td>
  </tr>
  <tr>
   <td>originTAZ
   </td>
   <td>trip origin TAZ
   </td>
  </tr>
  <tr>
   <td>destination TAZ
   </td>
   <td>trip destination TAZ
   </td>
  </tr>
  <tr>
   <td>tripMode
   </td>
   <td>trip mode
   </td>
  </tr>
  <tr>
   <td>valueOfTimeCategory
   </td>
   <td>trip value of time category
   </td>
  </tr>
  <tr>
   <td>transponderAvailable
   </td>
   <td>boolean indicator of toll transponder availability on trip
   </td>
  </tr>
  <tr>
   <td>avUsed
   </td>
   <td>boolean indicator of autonomous vehicle usage on trip
   </td>
  </tr>
  <tr>
   <td>weightTrip
   </td>
   <td>weight of record to use for trip metrics
   </td>
  </tr>
  <tr>
   <td>weightPersonTrip
   </td>
   <td>weight of record to use for person trip metrics
   </td>
  </tr>
  <tr>
   <td>timeDrive
   </td>
   <td>time (minutes) travelled using auto mode(s) on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>distanceDrive
   </td>
   <td>distance (miles) travelled using auto mode(s) on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>costTollDrive
   </td>
   <td>cost ($) spent on auto mode(s) tolls on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>costOperatingDrive
   </td>
   <td>cost ($) spent on auto mode(s) operation (gas/maintenance/fees) on trip, if applicable
   </td>
  </tr>
  <tr>
   <td>timeTotal
   </td>
   <td>time (minutes) travelled on trip
   </td>
  </tr>
  <tr>
   <td>distanceTotal
   </td>
   <td>distance (miles) travelled on trip
   </td>
  </tr>
  <tr>
   <td>costTotal
   </td>
   <td>cost ($) of trip
   </td>
  </tr>
  
</table>


### External-Internal Model Trip List (.\eitrip.csv)

<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tripID
   </td>
   <td>unique identifier of trip
   </td>
  </tr>
  <tr>
   <td>departTimeFiveTod
   </td>
   <td>trip departure time ABM five time of day period
   </td>
  </tr>
  <tr>
   <td>originTAZ
   </td>
   <td>trip origin TAZ
   </td>
  </tr>
  <tr>
   <td>destination TAZ
   </td>
   <td>trip destination TAZ
   </td>
  </tr>
  <tr>
   <td>tripMode
   </td>
   <td>trip mode
   </td>
  </tr>
  <tr>
   <td>valueOfTimeCategory
   </td>
   <td>trip value of time category
   </td>
  </tr>
  <tr>
   <td>transponderAvailable
   </td>
   <td>boolean indicator of toll transponder availability on trip
   </td>
  </tr>
  <tr>
   <td>avUsed
   </td>
   <td>boolean indicator of autonomous vehicle usage on trip
   </td>
  </tr>
  <tr>
   <td>weightTrip
   </td>
   <td>weight of record to use for trip metrics
   </td>
  </tr>
  <tr>
   <td>weightPersonTrip
   </td>
   <td>weight of record to use for person trip metrics
   </td>
  </tr>
  <tr>
   <td>timeDrive
   </td>
   <td>time (minutes) travelled using auto mode(s) on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>distanceDrive
   </td>
   <td>distance (miles) travelled using auto mode(s) on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>costTollDrive
   </td>
   <td>cost ($) spent on auto mode(s) tolls on trip (excluding transit access/egress), if applicable
   </td>
  </tr>
  <tr>
   <td>costOperatingDrive
   </td>
   <td>cost ($) spent on auto mode(s) operation (gas/maintenance/fees) on trip, if applicable
   </td>
  </tr>
  <tr>
   <td>timeTotal
   </td>
   <td>time (minutes) travelled on trip
   </td>
  </tr>
  <tr>
   <td>distanceTotal
   </td>
   <td>distance (miles) travelled on trip
   </td>
  </tr>
  <tr>
   <td>costTotal
   </td>
   <td>cost ($) of trip
   </td>
  </tr>
  
</table>


### Loaded Highway Network Shape File (hwyLoad.shp (.\hwyload.shp))

<table>
  <tr>
    <td>Field</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>hwycovid</td>
    <td>unique identifier of highway link</td>
  </tr>
  <tr>
    <td>link_name</td>
    <td>link name</td>
  </tr>
  <tr>
    <td>len_mile</td>
    <td>link length (miles)</td>
  </tr>
  <tr>
    <td>count_jur</td>
    <td>count jurisdiction code</td>
  </tr>
  <tr>
    <td>count_stat</td>
    <td>count station number</td>
  </tr>
  <tr>
    <td>count_loc</td>
    <td>count location code</td>
  </tr>
  <tr>
    <td>ifc</td>
    <td>initial functional class</td>
  </tr>
  <tr>
    <td>ifc_desc</td>
    <td>initial functional class description</td>
  </tr>
  <tr>
    <td>ihov</td>
    <td>link operation type</td>
  </tr>
  <tr>
    <td>itruck</td>
    <td>truck restriction code</td>
  </tr>
  <tr>
    <td>post_speed</td>
    <td>posted speed limit</td>
  </tr>
  <tr>
    <td>iway</td>
    <td>one or two way operations</td>
  </tr>
  <tr>
    <td>imed</td>
    <td>median type</td>
  </tr>
  <tr>
    <td>from_node</td>
    <td>A node number</td>
  </tr>
  <tr>
    <td>from_nm</td>
    <td>cross street name at from end of link</td>
  </tr>
  <tr>
    <td>to_node</td>
    <td>B node number</td>
  </tr>
  <tr>
    <td>to_nm</td>
    <td>cross street name at to end of link</td>
  </tr>
  <tr>
    <td>total_flow</td>
    <td>total vehicle flow</td>
  </tr>
  <tr>
    <td>abTotFlow</td>
    <td>AB directional vehicle flow</td>
  </tr>
  <tr>
    <td>baTotFlow</td>
    <td>BA directional vehicle flow</td>
  </tr>
  <tr>
    <td>ab_vmt</td>
    <td>AB directional vehicle miles travelled</td>
  </tr>
  <tr>
    <td>ba_vmt</td>
    <td>BA directional vehicle miles travelled</td>
  </tr>
  <tr>
    <td>vmt</td>
    <td>total vehicle miles travelled</td>
  </tr>
  <tr>
    <td>ab_vht</td>
    <td>AB directional vehicle hours travelled</td>
  </tr>
  <tr>
    <td>ba_vht</td>
    <td>BA directional vehicle hours travelled</td>
  </tr>
  <tr>
    <td>vht</td>
    <td>total vehicle hours travelled</td>
  </tr>
  <tr>
    <td>ab_ea_flow</td>
    <td>AB directional vehicle flow for ABM early am time period</td>
  </tr>
  <tr>
    <td>ba_ea_flow</td>
    <td>BA directional vehicle flow for ABM early am time period</td>
  </tr>
  <tr>
    <td>ab_am_flow</td>
    <td>AB directional vehicle flow for ABM am peak time period</td>
  </tr>
  <tr>
    <td>ba_am_flow</td>
    <td>BA directional vehicle flow for ABM am peak time period</td>
  </tr>
  <tr>
    <td>ab_md_flow</td>
    <td>AB directional vehicle flow for ABM midday time period</td>
  </tr>
  <tr>
    <td>ba_md_flow</td>
    <td>BA directional vehicle flow for ABM midday time period</td>
  </tr>
  <tr>
    <td>ab_pm_flow</td>
    <td>AB directional vehicle flow for ABM pm peak time period</td>
  </tr>
  <tr>
    <td>ba_pm_flow</td>
    <td>BA directional vehicle flow for ABM pm peak time period</td>
  </tr>
  <tr>
    <td>ab_ev_flow</td>
    <td>AB directional vehicle flow for ABM evening time period</td>
  </tr>
  <tr>
    <td>ba_ev_flow</td>
    <td>BA directional vehicle flow for ABM evening time period</td>
  </tr>
  <tr>
    <td>abAutoFlow</td>
    <td>AB directional vehicle flow for autos (sov, sr2, sr3)</td>
  </tr>
  <tr>
    <td>baAutoFlow</td>
    <td>BA directional vehicle flow for autos (sov, sr2, sr3)</td>
  </tr>
  <tr>
    <td>abSovFlow</td>
    <td>AB directional vehicle flow for single occupancy vehicles</td>
  </tr>
  <tr>
    <td>baSovFlow</td>
    <td>BA directional vehicle flow for single occupancy vehicles</td>
  </tr>
  <tr>
    <td>abHov2Flow</td>
    <td>AB directional vehicle flow for high occupancy (2 persons) vehicles</td>
  </tr>
  <tr>
    <td>baHov2Flow</td>
    <td>BA directional vehicle flow for high occupancy (2 persons) vehicles</td>
  </tr>
  <tr>
    <td>abHov3Flow</td>
    <td>AB directional vehicle flow for high occupancy (3+ persons) vehicles</td>
  </tr>
  <tr>
    <td>baHov3Flow</td>
    <td>BA directional vehicle flow for high occupancy (3+ persons) vehicles</td>
  </tr>
  <tr>
    <td>abTrucFlow</td>
    <td>AB directional vehicle flow for trucks</td>
  </tr>
  <tr>
    <td>baTrucFlow</td>
    <td>BA directional vehicle flow for trucks</td>
  </tr>
  <tr>
    <td>abBusFlow</td>
    <td>AB directional vehicle flow for preloaded bus</td>
  </tr>
  <tr>
    <td>baBusFlow</td>
    <td>BA directional vehicle flow for preloaded bus</td>
  </tr>
  <tr>
    <td>ab_ea_mph</td>
    <td>AB directional loaded speed (miles/hour) for ABM early am time period</td>
  </tr>
  <tr>
    <td>ba_ea_mph</td>
    <td>BA directional loaded speed (miles/hour) for ABM early am time period</td>
  </tr>
  <tr>
    <td>ab_am_mph</td>
    <td>AB directional loaded speed (miles/hour) for ABM am peak time period</td>
  </tr>
  <tr>
    <td>ba_am_mph</td>
    <td>BA directional loaded speed (miles/hour) for ABM am peak time period</td>
  </tr>
  <tr>
    <td>ab_md_mph</td>
    <td>AB directional loaded speed (miles/hour) for ABM midday time period</td>
  </tr>
  <tr>
    <td>ba_md_mph</td>
    <td>BA directional loaded speed (miles/hour) for ABM midday time period</td>
  </tr>
  <tr>
    <td>ab_pm_mph</td>
    <td>AB directional loaded speed (miles/hour) for ABM pm peak time period</td>
  </tr>
  <tr>
    <td>ba_pm_mph</td>
    <td>BA directional loaded speed (miles/hour) for ABM pm peak time period</td>
  </tr>
  <tr>
    <td>ab_ev_mph</td>
    <td>AB directional loaded speed (miles/hour) for ABM evening time period</td>
  </tr>
  <tr>
    <td>ba_ev_mph</td>
    <td>BA directional loaded speed (miles/hour) for ABM evening time period</td>
  </tr>
  <tr>
    <td>geometry</td>
    <td>linestring geometry of highway link</td>
  </tr>
</table>


### Loaded Highway Network by ABM Five Time of Day (hwyload_<<TOD>>.csv) (.\hwyload.csv)

<table>
  <tr>
    <td>Field</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>ID1</td>
    <td>unique identifier of highway link</td>
  </tr>
  <tr>
    <td>AB_Flow_PCE</td>
    <td>AB directional flow passenger car equivalents</td>
  </tr>
  <tr>
    <td>BA_Flow_PCE</td>
    <td>BA directional flow passenger car equivalents</td>
  </tr>
  <tr>
    <td>AB_Time</td>
    <td>AB directional loaded travel time (minutes)</td>
  </tr>
  <tr>
    <td>BA_Time</td>
    <td>BA directional loaded travel time (minutes)</td>
  </tr>
  <tr>
    <td>AB_VOC</td>
    <td>AB directional volume to capacity</td>
  </tr>
  <tr>
    <td>BA_VOC</td>
    <td>BA directional volume to capacity</td>
  </tr>
  <tr>
    <td>AB_V_Dist_T</td>
    <td>AB directional length</td>
  </tr>
  <tr>
    <td>BA_V_Dist_T</td>
    <td>BA directional length</td>
  </tr>
  <tr>
    <td>AB_VHT</td>
    <td>AB directional vehicle hours of travel</td>
  </tr>
  <tr>
    <td>BA_VHT</td>
    <td>BA directional vehicle hours of travel</td>
  </tr>
  <tr>
    <td>AB_Speed</td>
    <td>AB directional loaded speed</td>
  </tr>
  <tr>
    <td>BA_Speed</td>
    <td>BA directional loaded speed</td>
  </tr>
  <tr>
    <td>AB_VDF</td>
    <td>AB directional volume delay function</td>
  </tr>
  <tr>
    <td>BA_VDF</td>
    <td>BA directional volume delay function</td>
  </tr>
  <tr>
    <td>AB_MSA_Flow</td>
    <td>AB directional average of iterations flow</td>
  </tr>
  <tr>
    <td>BA_MSA_Flow</td>
    <td>BA directional average of iterations flow</td>
  </tr>
  <tr>
    <td>AB_MSA_Time</td>
    <td>AB directional average of iterations loaded travel time</td>
  </tr>
  <tr>
    <td>BA_MSA_Time</td>
    <td>BA directional average of iterations loaded travel time</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_NTPL</td>
    <td>AB directional flow for drive alone non-transponder low value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_NTPL</td>
    <td>BA directional flow for drive alone non-transponder low value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_TPL</td>
    <td>AB directional flow for drive alone transponder low value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_TPL</td>
    <td>BA directional flow for drive alone transponder low value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR2L</td>
    <td>AB directional flow for shared ride 2 low value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR2L</td>
    <td>BA directional flow for shared ride 2 low value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR3L</td>
    <td>AB directional flow for shared ride 3+ low value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR3L</td>
    <td>BA directional flow for shared ride 3+ low value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_NTPM</td>
    <td>AB directional flow for drive alone non-transponder medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_NTPM</td>
    <td>BA directional flow for drive alone non-transponder medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_TPM</td>
    <td>AB directional flow for drive alone transponder medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_TPM</td>
    <td>BA directional flow for drive alone transponder medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR2M</td>
    <td>AB directional flow for shared ride 2 medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR2M</td>
    <td>BA directional flow for shared ride 2 medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR3M</td>
    <td>AB directional flow for shared ride 3+ medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR3M</td>
    <td>BA directional flow for shared ride 3+ medium value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_NTPH</td>
    <td>AB directional flow for drive alone non-transponder high value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_NTPH</td>
    <td>BA directional flow for drive alone non-transponder high value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SOV_TPH</td>
    <td>AB directional flow for drive alone transponder high value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SOV_TPH</td>
    <td>BA directional flow for drive alone transponder high value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR2H</td>
    <td>AB directional flow for shared ride 2 high value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR2H</td>
    <td>BA directional flow for shared ride 2 high value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_SR3H</td>
    <td>AB directional flow for shared ride 3+ high value of time vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_SR3H</td>
    <td>BA directional flow for shared ride 3+ high value of time vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_lhd</td>
    <td>AB directional flow for light heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_lhd</td>
    <td>BA directional flow for light heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_mhd</td>
    <td>AB directional flow for medium heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_mhd</td>
    <td>BA directional flow for medium heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow_hhd</td>
    <td>AB directional flow for heavy heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>BA_Flow_hhd</td>
    <td>BA directional flow for heavy heavy duty truck vehicle class</td>
  </tr>
  <tr>
    <td>AB_Flow</td>
    <td>AB directional flow</td>
  </tr>
  <tr>
    <td>BA_Flow</td>
    <td>BA directional flow</td>
  </tr>
</table>

### Loaded Highway Network File (hwytcad.csv)

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>ID</td>
    <td>Unique identifier assigned to each model run</td>
  </tr>
  <tr>
    <td>HWYSegGUID</td>
    <td>Highway segment globally unique identifier (GUID)</td>
  </tr>
  <tr>
    <td>Length</td>
    <td>Length of link in miles</td>
  </tr>
  <tr>
    <td>Dir</td>
    <td>Link direction where: 0 = Center City Walk Links, 1 = Northbound, 2 = Westbound, 3 = Southbound, 4 = Eastbound</td>
  </tr>
  <tr>
    <td>hwycov-id:1</td>
    <td>SANDAG-assigned link ID</td>
  </tr>
  <tr>
    <td>SPHERE</td>
    <td>Jurisdiction sphere of influence</td>
  </tr>
  <tr>
    <td>NM</td>
    <td>Street name</td>
  </tr>
  <tr>
    <td>FXNM</td>
    <td>Program assigned cross street name at the FROM end of the link</td>
  </tr>
  <tr>
    <td>TXNM</td>
    <td>Program assigned cross street name at the TO end of the link</td>
  </tr>
  <tr>
    <td>AN</td>
    <td>A node number</td>
  </tr>
  <tr>
    <td>BN</td>
    <td>B node number</td>
  </tr>
  <tr>
    <td>ASPD</td>
    <td>Adjusted link speed in mph</td>
  </tr>
  <tr>
    <td>YR</td>
    <td>The year the link opened to traffic</td>
  </tr>
  <tr>
    <td>PROJ</td>
    <td>Project ID in the regional roadway network</td>
  </tr>
  <tr>
    <td>FC</td>
    <td>Roadway functional class where: 1 = Freeway, 2 = Prime Arterial, 3 = Major Arterial, 4 = Collector, 5 = Local Collector, 6 = Rural Collector, 7 = Local (non-circulation element) Road, 8 = Freeway Connector Ramp, 9 = Local Ramp, 10 = TAZ Connector, 11 = Rail Line, 12 = Bus Street, 99 = Walk Links, Transfer Links or Center City Walk Links</td>
  </tr>
  <tr>
    <td>FFC</td>
    <td>Federal functional class where: 0 = everything else other than 1, 2, and 9; 1 = Interstate Freeway; 2 = Other Freeway or Expressway; 3 = Principal Arterial; 4 = Minor Arterial; 5 = Major Collector; 6 = Minor Collector; 9 = Not Classified</td>
  </tr>
  <tr>
    <td>HOV</td>
    <td>Roadway Operation Restriction where: 1 = General Purpose; 2 = 2+ HOV (Managed lanes if toll >1); 3 = 3+ HOV (Managed lanes if toll > 1); 4 = Toll Lane</td>
  </tr>
  <tr>
    <td>EATRUCK</td>
    <td>Early AM Truck Restriction where: 1 = All Vehicle Classes, 2 = HHDT Excluded, 3 = MHDT & HHDT Excluded, 4 = LHDT, MHDT & HHDT Excluded (All Trucks), 5 = HHDT Only, 6 = MHDT & HHDT Only, 7 = LHDT, MHDT & HHDT Only (Truck Only)</td>
  </tr>
  <tr>
    <td>AMTRUCK</td>
    <td>AM Truck Restriction where: 1 = All Vehicle Classes, 2 = HHDT Excluded, 3 = MHDT & HHDT Excluded, 4 = LHDT, MHDT & HHDT Excluded (All Trucks), 5 = HHDT Only, 6 = MHDT & HHDT Only, 7 = LHDT, MHDT & HHDT Only (Truck Only)</td>
  </tr>
  <tr>
    <td>MDTRUCK</td>
    <td>Midday Truck Restriction where: 1 = All Vehicle Classes, 2 = HHDT Excluded, 3 = MHDT & HHDT Excluded, 4 = LHDT, MHDT & HHDT Excluded (All Trucks), 5 = HHDT Only, 6 = MHDT & HHDT Only, 7 = LHDT, MHDT & HHDT Only (Truck Only)</td>
  </tr>
  <tr>
    <td>PMTRUCK</td>
    <td>PM Truck Restriction where: 1 = All Vehicle Classes, 2 = HHDT Excluded, 3 = MHDT & HHDT Excluded, 4 = LHDT, MHDT & HHDT Excluded (All Trucks), 5 = HHDT Only, 6 = MHDT & HHDT Only, 7 = LHDT, MHDT & HHDT Only (Truck Only)</td>
  </tr>
  <tr>
    <td>EVTRUCK</td>
    <td>Evening Truck Restriction where: 1 = All Vehicle Classes, 2 = HHDT Excluded, 3 = MHDT & HHDT Excluded, 4 = LHDT, MHDT & HHDT Excluded (All Trucks), 5 = HHDT Only, 6 = MHDT & HHDT Only, 7 = LHDT, MHDT & HHDT Only (Truck Only)</td>
  </tr>
  <tr>
    <td>SPD</td>
    <td>Link speed in mph</td>
  </tr>
  <tr>
    <td>WAY</td>
    <td>One or two way link indicator where: 1 = One-way link, 2 = Two-way link</td>
  </tr>
  <tr>
    <td>MED</td>
    <td>Median type where: 1 = No median, 2 = Raised or fixed median, 3 = Continuous left turn center lane</td>
  </tr>
  <tr>
    <td>COST</td>
    <td>Cost associated with the link</td>
  </tr>
  <tr>
    <td>ABAU</td>
    <td>Number of auxiliary lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABCNT</td>
    <td>Intersection control type at the TO end of the link: 0 = No Control, 1 = Traffic Signal, 2 = All-Way Stop Sign, 3 = Two-Way Stop Sign, 4 = Ramp Meter, 5 = Ramp Meter with HOV lane meter, 6 = Light Rail Crossing, 7 = Toll Booth, 9 = Prevent Control</td>
  </tr>
  <tr>
    <td>ABTL</td>
    <td>Intersection approach through lanes at the TO end of the link: 0~4 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>ABRL</td>
    <td>Intersection approach right-turn lanes at the TO end of the link: 0~2 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>ABLL</td>
    <td>Intersection approach left-turn lanes at the TO end of the link: 0~2 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>ABGC</td>
    <td>Intersection green-to-cycle ratio at the TO end of the link (%)</td>
  </tr>
  <tr>
    <td>ABPLC</td>
    <td>Per-lane capacity per hour in the TO direction of the link</td>
  </tr>
  <tr>
    <td>BAAU</td>
    <td>Number of auxiliary lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BACNT</td>
    <td>Intersection control type at the FROM end of the link: 0 = No Control, 1 = Traffic Signal, 2 = All-Way Stop Sign, 3 = Two-Way Stop Sign, 4 = Ramp Meter, 5 = Ramp Meter with HOV lane meter, 6 = Light Rail Crossing, 7 = Toll Booth, 9 = Prevent Control</td>
  </tr>
  <tr>
    <td>BATL</td>
    <td>Intersection approach through lanes at the FROM end of the link: 0~4 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>BARL</td>
    <td>Intersection approach right-turn lanes at the FROM end of the link: 0~2 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>BALL</td>
    <td>Intersection approach left-turn lanes at the FROM end of the link: 0~2 = number of through lanes, 7 = Free, 8 = Prohibited, 9 = No dedicated lane for the movement</td>
  </tr>
  <tr>
    <td>BAGC</td>
    <td>Intersection green-to-cycle ratio at the FROM end of the link (%)</td>
  </tr>
  <tr>
    <td>BAPLC</td>
    <td>Per-lane capacity per hour in the FROM direction of the link</td>
  </tr>
  <tr>
    <td>relifac</td>
    <td>Reliability factor for the link</td>
  </tr>
  <tr>
    <td>TOLL2_EA</td>
    <td>Early AM Toll cost for Toll 2 vehicles</td>
  </tr>
  <tr>
    <td>TOLL2_AM</td>
    <td>AM Toll cost for Toll 2 vehicles</td>
  </tr>
  <tr>
    <td>TOLL2_MD</td>
    <td>Midday Toll cost for Toll 2 vehicles</td>
  </tr>
  <tr>
    <td>TOLL2_PM</td>
    <td>PM Toll cost for Toll 2 vehicles</td>
  </tr>
  <tr>
    <td>TOLL2_EV</td>
    <td>Evening Toll cost for Toll 2 vehicles</td>
  </tr>
  <tr>
    <td>TOLL3_EA</td>
    <td>Early AM Toll cost for Toll 3 vehicles</td>
  </tr>
  <tr>
    <td>TOLL3_AM</td>
    <td>AM Toll cost for Toll 3 vehicles</td>
  </tr>
  <tr>
    <td>TOLL3_MD</td>
    <td>Midday Toll cost for Toll 3 vehicles</td>
  </tr>
  <tr>
    <td>TOLL3_PM</td>
    <td>PM Toll cost for Toll 3 vehicles</td>
  </tr>
  <tr>
    <td>TOLL3_EV</td>
    <td>Evening Toll cost for Toll 3 vehicles</td>
  </tr>
  <tr>
    <td>TOLL4_EA</td>
    <td>Early AM Toll cost for Toll 4 vehicles</td>
  </tr>
  <tr>
    <td>TOLL4_AM</td>
    <td>AM Toll cost for Toll 4 vehicles</td>
  </tr>
  <tr>
    <td>TOLL4_MD</td>
    <td>Midday Toll cost for Toll 4 vehicles</td>
  </tr>
  <tr>
    <td>TOLL4_PM</td>
    <td>PM Toll cost for Toll 4 vehicles</td>
  </tr>
  <tr>
    <td>TOLL4_EV</td>
    <td>Evening Toll cost for Toll 4 vehicles</td>
  </tr>
  <tr>
    <td>TOLL5_EA</td>
    <td>Early AM Toll cost for Toll 5 vehicles</td>
  </tr>
  <tr>
    <td>TOLL5_AM</td>
    <td>AM Toll cost for Toll 5 vehicles</td>
  </tr>
  <tr>
    <td>TOLL5_MD</td>
    <td>Midday Toll cost for Toll 5 vehicles</td>
  </tr>
  <tr>
    <td>TOLL5_PM</td>
    <td>PM Toll cost for Toll 5 vehicles</td>
  </tr>
  <tr>
    <td>TOLL5_EV</td>
    <td>Evening Toll cost for Toll 5 vehicles</td>
  </tr>
  <tr>
    <td>TOLL_EA</td>
    <td>Early AM Toll Cost (in cents per mile)</td>
  </tr>
  <tr>
    <td>TOLL_AM</td>
    <td>AM Toll Cost (in cents per mile)</td>
  </tr>
  <tr>
    <td>TOLL_MD</td>
    <td>Midday Toll Cost (in cents per mile)</td>
  </tr>
  <tr>
    <td>TOLL_PM</td>
    <td>PM Toll Cost (in cents per mile)</td>
  </tr>
  <tr>
    <td>TOLL_EV</td>
    <td>Evening Toll Cost (in cents per mile)</td>
  </tr>
  <tr>
    <td>ABCP_EA</td>
    <td>Early AM mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCP_AM</td>
    <td>AM mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCP_MD</td>
    <td>Midday mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCP_PM</td>
    <td>PM mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCP_EV</td>
    <td>Evening mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>BACP_EA</td>
    <td>Early AM mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACP_AM</td>
    <td>AM mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACP_MD</td>
    <td>Midday mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACP_PM</td>
    <td>PM mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACP_EV</td>
    <td>Evening mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>ABCX_EA</td>
    <td>Early AM intersection approach capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCX_AM</td>
    <td>AM intersection approach capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCX_MD</td>
    <td>Midday intersection approach capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCX_PM</td>
    <td>PM intersection approach capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCX_EV</td>
    <td>Evening intersection approach capacity in the TO direction</td>
  </tr>
  <tr>
    <td>BACX_EA</td>
    <td>Early AM intersection approach capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACX_AM</td>
    <td>AM intersection approach capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACX_MD</td>
    <td>Midday intersection approach capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACX_PM</td>
    <td>PM intersection approach capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACX_EV</td>
    <td>Evening intersection approach capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>ABCH_EA</td>
    <td>Early AM hourly mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCH_AM</td>
    <td>AM hourly mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCH_MD</td>
    <td>Midday hourly mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCH_PM</td>
    <td>PM hourly mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>ABCH_EV</td>
    <td>Evening hourly mid-link capacity in the TO direction</td>
  </tr>
  <tr>
    <td>BACH_EA</td>
    <td>Early AM hourly mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACH_AM</td>
    <td>AM hourly mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACH_MD</td>
    <td>Midday hourly mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACH_PM</td>
    <td>PM hourly mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>BACH_EV</td>
    <td>Evening hourly mid-link capacity in the FROM direction</td>
  </tr>
  <tr>
    <td>ABTM_EA</td>
    <td>Early AM link travel time (minutes) in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABTM_AM</td>
    <td>AM link travel time (minutes) in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABTM_MD</td>
    <td>Midday link travel time (minutes) in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABTM_PM</td>
    <td>PM link travel time (minutes) in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABTM_EV</td>
    <td>Evening link travel time (minutes) in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>BATM_EA</td>
    <td>Early AM link travel time (minutes) in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BATM_AM</td>
    <td>AM link travel time (minutes) in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BATM_MD</td>
    <td>Midday link travel time (minutes) in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BATM_PM</td>
    <td>PM link travel time (minutes) in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BATM_EV</td>
    <td>Evening link travel time (minutes) in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>ABTX_EA</td>
    <td>Early AM intersection delay time (seconds) in the TO end</td>
  </tr>
  <tr>
    <td>ABTX_AM</td>
    <td>AM intersection delay time (seconds) in the TO end</td>
  </tr>
  <tr>
    <td>ABTX_MD</td>
    <td>Midday intersection delay time (seconds) in the TO end</td>
  </tr>
  <tr>
    <td>ABTX_PM</td>
    <td>PM intersection delay time (seconds) in the TO end</td>
  </tr>
  <tr>
    <td>ABTX_EV</td>
    <td>Evening intersection delay time (seconds) in the TO end</td>
  </tr>
  <tr>
    <td>BATX_EA</td>
    <td>Early AM intersection delay time (seconds) in the FROM end</td>
  </tr>
  <tr>
    <td>BATX_AM</td>
    <td>AM intersection delay time (seconds) in the FROM end</td>
  </tr>
  <tr>
    <td>BATX_MD</td>
    <td>Midday intersection delay time (seconds) in the FROM end</td>
  </tr>
  <tr>
    <td>BATX_PM</td>
    <td>PM intersection delay time (seconds) in the FROM end</td>
  </tr>
  <tr>
    <td>BATX_EV</td>
    <td>Evening intersection delay time (seconds) in the FROM end</td>
  </tr>
  <tr>
    <td>ABLN_EA</td>
    <td>Early AM number of lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABLN_AM</td>
    <td>AM number of lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABLN_MD</td>
    <td>Midday number of lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABLN_PM</td>
    <td>PM number of lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>ABLN_EV</td>
    <td>Evening number of lanes in the FROM-TO direction</td>
  </tr>
  <tr>
    <td>BALN_EA</td>
    <td>Early AM number of lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BALN_AM</td>
    <td>AM number of lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BALN_MD</td>
    <td>Midday number of lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BALN_PM</td>
    <td>PM number of lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>BALN_EV</td>
    <td>Evening number of lanes in the TO-FROM direction</td>
  </tr>
  <tr>
    <td>ABSCST_EA</td>
    <td>Early AM period AB directional travel cost</td>
  </tr>
  <tr>
    <td>ABSCST_AM</td>
    <td>AM period AB directional travel cost</td>
  </tr>
  <tr>
    <td>ABSCST_MD</td>
    <td>Midday period AB directional travel cost</td>
  </tr>
  <tr>
    <td>ABSCST_PM</td>
    <td>PM period AB directional travel cost</td>
  </tr>
  <tr>
    <td>ABSCST_EV</td>
    <td>Evening period AB directional travel cost</td>
  </tr>
  <tr>
    <td>BAH2CST_EA</td>
    <td>Early AM period BA directional HOV2 travel cost</td>
  </tr>
  <tr>
    <td>BAH2CST_AM</td>
    <td>AM period BA directional HOV2 travel cost</td>
  </tr>
  <tr>
    <td>BAH2CST_MD</td>
    <td>Midday period BA directional HOV2 travel cost</td>
  </tr>
  <tr>
    <td>BAH2CST_PM</td>
    <td>PM period BA directional HOV2 travel cost</td>
  </tr>
  <tr>
    <td>BAH2CST_EV</td>
    <td>Evening period BA directional HOV2 travel cost</td>
  </tr>
  <tr>
    <td>ABH3CST_EA</td>
    <td>Early AM period AB directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>ABH3CST_AM</td>
    <td>AM period AB directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>ABH3CST_MD</td>
    <td>Midday period AB directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>ABH3CST_PM</td>
    <td>PM period AB directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>ABH3CST_EV</td>
    <td>Evening period AB directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>BAH3CST_EA</td>
    <td>Early AM period BA directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>BAH3CST_AM</td>
    <td>AM period BA directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>BAH3CST_MD</td>
    <td>Midday period BA directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>BAH3CST_PM</td>
    <td>PM period BA directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>BAH3CST_EV</td>
    <td>Evening period BA directional HOV3 travel cost</td>
  </tr>
  <tr>
    <td>ABSTM_EA</td>
    <td>Early AM period AB directional transit flow</td>
  </tr>
  <tr>
    <td>ABSTM_AM</td>
    <td>AM period AB directional transit flow</td>
  </tr>
  <tr>
    <td>ABSTM_MD</td>
    <td>Midday period AB directional transit flow</td>
  </tr>
  <tr>
    <td>ABSTM_PM</td>
    <td>PM period AB directional transit flow</td>
  </tr>
  <tr>
    <td>ABSTM_EV</td>
    <td>Evening period AB directional transit flow</td>
  </tr>
  <tr>
    <td>BASTM_EA</td>
    <td>Early AM period BA directional transit flow</td>
  </tr>
  <tr>
    <td>BASTM_AM</td>
    <td>AM period BA directional transit flow</td>
  </tr>
  <tr>
    <td>BASTM_MD</td>
    <td>Midday period BA directional transit flow</td>
  </tr>
  <tr>
    <td>BASTM_PM</td>
    <td>PM period BA directional transit flow</td>
  </tr>
  <tr>
    <td>BASTM_EV</td>
    <td>Evening period BA directional transit flow</td>
  </tr>
  <tr>
    <td>ABPRELOAD_EA</td>
    <td>Early AM period AB directional preload flow</td>
  </tr>
  <tr>
    <td>ABPRELOAD_AM</td>
    <td>AM period AB directional preload flow</td>
  </tr>
  <tr>
    <td>ABPRELOAD_MD</td>
    <td>Midday period AB directional preload flow</td>
  </tr>
  <tr>
    <td>ABPRELOAD_PM</td>
    <td>PM period AB directional preload flow</td>
  </tr>
  <tr>
    <td>ABPRELOAD_EV</td>
    <td>Evening period AB directional preload flow</td>
  </tr>
  <tr>
    <td>BAPRELOAD_EA</td>
    <td>Early AM period BA directional preload flow</td>
  </tr>
  <tr>
    <td>BAPRELOAD_AM</td>
    <td>AM period BA directional preload flow</td>
  </tr>
  <tr>
    <td>BAPRELOAD_MD</td>
    <td>Midday period BA directional preload flow</td>
  </tr>
  <tr>
    <td>BAPRELOAD_PM</td>
    <td>PM period BA directional preload flow</td>
  </tr>
  <tr>
    <td>BAPRELOAD_EV</td>
    <td>Evening period BA directional preload flow</td>
  </tr>
  <tr>
    <td>AB_GCRatio</td>
    <td>Intersection green-to-cycle ratio at the TO end of the link (%)</td>
  </tr>
  <tr>
    <td>BA_GCRatio</td>
    <td>Intersection green-to-cycle ratio at the FROM end of the link (%)</td>
  </tr>
  <tr>
    <td>AB_Cycle</td>
    <td>Cycle time at the AB end of the link</td>
  </tr>
  <tr>
    <td>BA_Cycle</td>
    <td>Cycle time at the BA end of the link</td>
  </tr>
  <tr>
    <td>AB_PF</td>
    <td>Performance factor in the AB direction</td>
  </tr>
  <tr>
    <td>BA_PF</td>
    <td>Performance factor in the BA direction</td>
  </tr>
  <tr>
    <td>ALPHA1</td>
    <td>Volume delay function (VDF) parameter Alpha1</td>
  </tr>
  <tr>
    <td>BETA1</td>
    <td>Volume delay function (VDF) parameter Beta1</td>
  </tr>
  <tr>
    <td>ALPHA2</td>
    <td>Volume delay function (VDF) parameter Alpha2</td>
  </tr>
  <tr>
    <td>BETA2</td>
    <td>Volume delay function (VDF) parameter Beta2</td>
  </tr>
  <tr>
    <td>geometry</td>
    <td>Geospatial data representation of the link</td>
  </tr>
</table>




### Aggregated Transit Flow Table (transit_aggflow.csv) 

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>MODE</td>
    <td>Transit mode (BUS, PREM, ALLPEN)</td>
  </tr>
  <tr>
    <td>ACCESSMODE</td>
    <td>Access mode (WLK, PNR, KNR)</td>
  </tr>
  <tr>
    <td>TOD</td>
    <td>Time of day (EA, AM, MD, PM, EV)</td>
  </tr>
  <tr>
    <td>LINK_ID</td>
    <td>Link ID</td>
  </tr>
  <tr>
    <td>AB_TransitFlow</td>
    <td>A-B Direction Transit Flow</td>
  </tr>
  <tr>
    <td>BA_TransitFlow</td>
    <td>B-A Direction Transit Flow</td>
  </tr>
  <tr>
    <td>AB_NonTransit</td>
    <td>A-B Direction Non Transit Flow</td>
  </tr>
  <tr>
    <td>BA_NonTransit</td>
    <td>B-A Direction Non Transit Flow</td>
  </tr>
  <tr>
    <td>AB_TotalFlow</td>
    <td>A-B Direction Total Flow</td>
  </tr>
  <tr>
    <td>BA_TotalFlow</td>
    <td>B-A Direction Total Flow</td>
  </tr>
  <tr>
    <td>AB_Access_Walk_Flow</td>
    <td>A-B Direction Access Walk Flow</td>
  </tr>
  <tr>
    <td>BA_Access_Walk_Flow</td>
    <td>B-A Direction Access Walk Flow</td>
  </tr>
  <tr>
    <td>AB_Xfer_Walk_Flow</td>
    <td>A-B Direction Transfer Walk Flow</td>
  </tr>
  <tr>
    <td>BA_Xfer_Walk_Flow</td>
    <td>B-A Direction Transfer Walk Flow</td>
  </tr>
  <tr>
    <td>AB_Egress_Walk_Flow</td>
    <td>A-B Direction Egress Walk Flow</td>
  </tr>
  <tr>
    <td>BA_Egress_Walk_Flow</td>
    <td>B-A Direction Egress Walk Flow</td>
  </tr>
</table>



### Transit Flow Table (transit_flow.csv)

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>MODE</td>
    <td>Transit mode (BUS, PREM, ALLPEN)</td>
  </tr>
  <tr>
    <td>ACCESSMODE</td>
    <td>Access mode (WLK, PNR, KNR)</td>
  </tr>
  <tr>
    <td>TOD</td>
    <td>Time of day (EA, AM, MD, PM, EV)</td>
  </tr>
  <tr>
    <td>ROUTE</td>
    <td>Sequential Route Number</td>
  </tr>
  <tr>
    <td>FROM_STOP</td>
    <td>From Stop ID</td>
  </tr>
  <tr>
    <td>TO_STOP</td>
    <td>To Stop ID</td>
  </tr>
  <tr>
    <td>CENTROID</td>
    <td>Centroid</td>
  </tr>
  <tr>
    <td>FROMMP</td>
    <td>From milepost</td>
  </tr>
  <tr>
    <td>TOMP</td>
    <td>To milepost</td>
  </tr>
  <tr>
    <td>TRANSITFLOW</td>
    <td>Transit flow</td>
  </tr>
  <tr>
    <td>BASEIVTT</td>
    <td>Base in-vehicle time</td>
  </tr>
  <tr>
    <td>COST</td>
    <td>Cost</td>
  </tr>
  <tr>
    <td>VOC</td>
    <td>Volume to Capacity</td>
  </tr>
</table>


### On and Off Transit File (transit_onoff.csv)

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>MODE</td>
    <td>Transit mode (BUS, PREM, ALLPEN)</td>
  </tr>
  <tr>
    <td>ACCESSMODE</td>
    <td>Access mode (WLK, PNR, KNR)</td>
  </tr>
  <tr>
    <td>TOD</td>
    <td>Time of day (EA, AM, MD, PM, EV)</td>
  </tr>
  <tr>
    <td>ROUTE</td>
    <td>Sequential Route Number</td>
  </tr>
  <tr>
    <td>STOP</td>
    <td>Stop ID</td>
  </tr>
  <tr>
    <td>BOARDINGS</td>
    <td>Number of boardings</td>
  </tr>
  <tr>
    <td>ALIGHTINGS</td>
    <td>Number of alightings</td>
  </tr>
  <tr>
    <td>WALKACCESSON</td>
    <td>Number of walk access boardings</td>
  </tr>
  <tr>
    <td>DIRECTTRANSFERON</td>
    <td>Number of transfer boardings</td>
  </tr>
  <tr>
    <td>WALKTRANSFERON</td>
    <td>Number of walk transfer boardings</td>
  </tr>
  <tr>
    <td>DIRECTTRANSFEROFF</td>
    <td>Number of transfer alightings</td>
  </tr>
  <tr>
    <td>WALKTRANSFEROFF</td>
    <td>Number of walk transfer alightings</td>
  </tr>
  <tr>
    <td>EGRESSOFF</td>
    <td>Number of walk egress alightings</td>
  </tr>
</table>


### Transit Link File (transitLink.csv)


<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>trcovID</td>
    <td>Transit link ID, unique with AB direction field</td>
  </tr>
  <tr>
    <td>AB</td>
    <td>1/0 indicator of AB directionality</td>
  </tr>
  <tr>
    <td>geometry</td>
    <td>Transit link linestring geometry</td>
  </tr>
</table>



### Transit Route File (transitRoute.csv)

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>Route_ID</td>
    <td>Transit route ID</td>
  </tr>
  <tr>
    <td>Route_Name</td>
    <td>Route number (first three digits), direction (1/2), and configuration (last two digits)</td>
  </tr>
  <tr>
    <td>Mode</td>
    <td>Line haul mode of route:<br>
        4 = coaster<br>
        5 = sprinter/trolley<br>
        8 = prem express<br>
        9 = regular express<br>
        10 = local bus
    </td>
  </tr>
  <tr>
    <td>AM_Headway</td>
    <td>ABM five time of day am peak time period headway</td>
  </tr>
  <tr>
    <td>PM_Headway</td>
    <td>ABM five time of day pm peak time period headway</td>
  </tr>
  <tr>
    <td>OP_Headway</td>
    <td>ABM five time of day midday time period headway</td>
  </tr>
  <tr>
    <td>Night_Headway</td>
    <td>ABM five time of day early am and evening time period headway</td>
  </tr>
  <tr>
    <td>Night_Hours</td>
    <td>Hours of transit route operation during ABM five time of day early am and evening time periods</td>
  </tr>
  <tr>
    <td>Config</td>
    <td>Route number (first three digits), direction (1/2), and configuration (last two digits)</td>
  </tr>
  <tr>
    <td>Fare</td>
    <td>Transit fare cost ($) of route</td>
  </tr>
  <tr>
    <td>geometry</td>
    <td>Transit route linestring geometry</td>
  </tr>
</table>



### Transit Stop File (transitStop.csv)

<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>Stop_ID</td>
    <td>Transit stop ID</td>
  </tr>
  <tr>
    <td>Route_ID</td>
    <td>Transit route ID</td>
  </tr>
  <tr>
    <td>Link_ID</td>
    <td>Transit link ID</td>
  </tr>
  <tr>
    <td>Pass_Count</td>
    <td>Passenger count</td>
  </tr>
  <tr>
    <td>Milepost</td>
    <td>Mile post</td>
  </tr>
  <tr>
    <td>Longitude</td>
    <td>Longitude of stop location</td>
  </tr>
  <tr>
    <td>Latitude</td>
    <td>Latitude of stop location</td>
  </tr>
  <tr>
    <td>NearNode</td>
    <td>Transit node ID</td>
  </tr>
  <tr>
    <td>FareZone</td>
    <td>Fare zone</td>
  </tr>
  <tr>
    <td>StopName</td>
    <td>Name of transit stop</td>
  </tr>
  <tr>
    <td>geometry</td>
    <td>Transit stop point geometry</td>
  </tr>
</table>



### MGRA Travel Times - AM Period (walkMgrasWithin45Min_AM.csv)</h2>
<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>i</td>
    <td>Origin MGRA</td>
  </tr>
  <tr>
    <td>j</td>
    <td>Destination MGRA</td>
  </tr>
  <tr>
    <td>transit</td>
    <td>Travel time (in minutes) from i to j by transit mode; 999 if not accessible</td>
  </tr>
  <tr>
    <td>walk</td>
    <td>Travel time (in minutes) from i to j by walking; 999 if not accessible</td>
  </tr>
  <tr>
    <td>bike</td>
    <td>Travel time (in minutes) from i to j by biking; 999 if not accessible</td>
  </tr>
  <tr>
    <td>microtransit</td>
    <td>Travel time (in minutes) from i to j by microtransit; 999 if not accessible</td>
  </tr>
  <tr>
    <td>nev</td>
    <td>Travel time (in minutes) from i to j by neighborhood electric vehicle (NEV); 999 if not accessible</td>
  </tr>
  <tr>
    <td>ebike</td>
    <td>Travel time (in minutes) from i to j by electric bike (e-bike); 999 if not accessible</td>
  </tr>
  <tr>
    <td>escooter</td>
    <td>Travel time (in minutes) from i to j by electric scooter (e-scooter); 999 if not accessible</td>
  </tr>
</table>

### MGRA Travel Times - MD Period (walkMgrasWithin45Min_MD.csv)</h2>
<table>
  <tr>
    <td>Column Name</td>
    <td>Description</td>
  </tr>
  <tr>
    <td>i</td>
    <td>Origin MGRA</td>
  </tr>
  <tr>
    <td>j</td>
    <td>Destination MGRA</td>
  </tr>
  <tr>
    <td>transit</td>
    <td>Travel time (in minutes) from i to j by transit mode; 999 if not accessible</td>
  </tr>
  <tr>
    <td>walk</td>
    <td>Travel time (in minutes) from i to j by walking; 999 if not accessible</td>
  </tr>
  <tr>
    <td>bike</td>
    <td>Travel time (in minutes) from i to j by biking; 999 if not accessible</td>
  </tr>
  <tr>
    <td>microtransit</td>
    <td>Travel time (in minutes) from i to j by microtransit; 999 if not accessible</td>
  </tr>
  <tr>
    <td>nev</td>
    <td>Travel time (in minutes) from i to j by neighborhood electric vehicle (NEV); 999 if not accessible</td>
  </tr>
  <tr>
    <td>ebike</td>
    <td>Travel time (in minutes) from i to j by electric bike (e-bike); 999 if not accessible</td>
  </tr>
  <tr>
    <td>escooter</td>
    <td>Travel time (in minutes) from i to j by electric scooter (e-scooter); 999 if not accessible</td>
  </tr>
</table>