# ABM3 Model Inputs

The main inputs to ABM3 include the transportation network, land-use data, synthetic population data, parameters files, and model specifications. Outputs include a set of files that describe travel decisions made by all travel markets considered by the model (residents, overnight visitors, airport ground access trips, commercial vehicles and trucks, Mexico residents traveling in San Diego County, and travel made by all other non-residents into and through San Diego County).

### File Types

There are several file types used for model inputs and outputs. They are denoted by their extension, as listed in the table below.

| **Extension** | **Format** | 
| --- | --- |
| .log, .txt | Text files created during a model run containing logging results. | 
| .yaml | Text files used for setting properties that control ActivitySim or some other process. | 
| .csv | Comma-separated value files used to store model parameters, input or output data. | 
| .omx | Open matrix format files used to store input or output trip tables or skims | 
| .h5 | HDF5 files, used to store pipeline for restarting ActivitySim | 
| .shp (along with other files - .cpg, .dbf, .prj, .shx) | ArcGIS shapefiles and associated files | 
| .html | Hypertext markup language files, open in web browser | 
| .png | Portable network graphics file, open in web browser, Microsoft photos, or third-party graphics editor | 

## Model Inputs

The table below contains brief descriptions of the input files required to execute the SANDAG ABM3.

| **File Name** | **Purpose** | **File Type** | **Prepared By** | 
| --- | --- | --- | --- |
| **Land Use** |  |  |  | 
| [mgra_based_input{SCENARIO_YEAR}.csv](#lu) | Land use forecast of the size and structure of the region’s economy and corresponding demographic forecast | CSV | Land Use Modelers, Transportation Modelers, and GIS | 
| **Synthetic Population** |  |  |  | 
| [households.csv](#population_synth_households) | Synthetic households | CSV | Transportation Modelers | 
| [persons.csv](#population_synth_persons) | Synthetic persons | CSV | Transportation Modelers | 
| **Network** |  |  |  | 
| EmmeOutputs.gdb | Network Input Files | GDB | Transportation Modelers |
| transit_connectors | Input Files | | |
| [vehicle_class_toll_factors.csv](#vehicle_class_toll) | Relative toll values by six vehicle classes by Facility name. Used to identify "free for HOV" type managed lane facilities. | CSV | Transportation Modelers | 
| [trlink.csv](#tr_link) | Transit route with a list of links file | CSV | Transportation Modelers | 
| trrt.csv | Transit route attribute file | CSV | Transportation Modelers | 
| [trstop.csv](#transit_binary_stop) | Transit stop attribute file | TCSV | Transportation Modelers | 
| mode5tod.csv | Transit mode parameters table | CSV | Transportation Modelers |
| [timexfer_XX.csv](#transit_transfer_proh) | Transit timed transfers table between COASTER and feeder buses; XX is the TOD (EA, AM, MD, PM, and EV) | CSV | Transportation Modelers | 
| special_fares.txt | Fares to coaster | Text File | Transportation Modelers | 
| [mobilityHubMGRA.csv](#mobility_mgra) |  | CSV | Transportation Modelers | 
| [SANDAG_Bike_Net.dbf](#bike_net_link) | Bike network links | DBF | GIS | 
| [SANDAG_Bike_Node.dbf](#bike_net_node) | Bike network nodes | DBF | GIS | 
| [bikeTazLogsum.csv](#bike_taz_logsum) <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike TAZ logsum | CSV | Transportation Modelers | 
| [bikeMgraLogsum.csv](#bike_mgra_logsum) <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike MGRA logsum | CSV | Transportation Modelers | 
| mgra15.sbn | | SBN | |
| taz15.shp | | SHP | |
| **Cross-Border Model (Derived from cross-border survey)** |  |  |  | 
| crossBorder_tourPurpose_control.csv |  | CSV |  | 
| crossBorder_tourPurpose_nonSENTRI.csv | Cross Border Model tour purpose distribution for Non-SENTRI tours | CSV | Transportation Modelers | 
| crossBorder_tourPurpose_SENTRI.csv | Cross Border Model tour purpose distribution for SENTRI tours | CSV | Transportation Modelers | 
| [crossBorder_tourEntryAndReturn.csv](#cross_border_entry_return) | Cross Border Model tour entry and return time-of-day distribution | CSV | Transportation Modelers | 
| [crossBorder_supercolonia.csv](#cross_border_supercolonia) | Cross Border Model distance from Colonias to border crossing locations | CSV | Transportation Modelers | 
| [crossBorder_pointOfEntryWaitTime.csv](#cross_border_wait_time) | Cross Border Model wait times at border crossing locations table | CSV | GIS - Pat L vtsql | 
| [crossBorder_stopFrequency.csv](#cross_border_stops) | Cross Border Model stop frequency data | CSV | Transportation Modelers | 
| [crossBorder_stopPurpose.csv](#cross_border_stop_purpose) | Cross Border Model stop purpose distribution | CSV | Transportation Modelers | 
| [crossBorder_outboundStopDuration.csv](#cross_border_out_stop) | Cross Border Model time-of-day offsets for outbound stops | CSV | Transportation Modelers | 
| [crossBorder_inboundStopDuration.csv](#cross_border_in_stop) | Cross Border Model time-of-day offsets for inbound stops | CSV | Transportation Modelers | 
| [closest_maz_to_external_tazs.csv](#closest_maz_to_external_tazs) | | CSV | Transportation Modelers |
| [mazs_xborder.csv](#mazs_xborder) | | CSV | Transportation Modelers | 
| **External Models (Derived from SCAG survey)** |  |  |  | 
| [externalExternalTripsByYear.csv](#external_trip) <i> (raw inputs have these by year) <i> | External origin-destination station trip matrix | CSV | Transportation Modelers | |  |  |  | 
| [externalInternalControlTotalsByYear.csv](#external_internal) <i> (raw inputs have these by year) <i> | External Internal station control totals read by GISDK | CSV | Transportation Modelers | |  |  |  | 
| [internalExternal_tourTOD.csv](#internal_external_tod) | Internal-External Model tour time-of-day frequency distribution | CSV | Transportation Modelers | 
| [resident_ie_size_term.csv](#resident_ie_size_term) | | CSV | Transportation Modelers
| **Commercial Vehicle Model** |  |  |  | 
| land_use(output from preprocessing step) | MGRA based land use file | CSV | |
| percent_of_establishments_by_luz_size_emp_cat.xlsx | Percent of establishments in LUZ that belong in each size category by industry sector | Excel Workbook | |
| CVM\SynthEstablishments.csv | Output from CVM establishment synthesis, similar description as previous part | CSV | |
| CVM\MGRAEmpByEstSize.csv | MGRA Based synthetically generated establishments. Used for disgnostic purposes, not for simulation | CSV | |
| CVM\SummaryEstablishments.csv | Contains information about synthetically generated establishments to be used as inputs to the commercial vehicle model | CSV | |
| **Heavy Truck Model ( HTM )** |
| HTM\inputs_sandag_htm_<Scenario_Year>.xlsx | Contains all the required inputs ( in different sheets) for the Heavy Truck Model | Excel Workbook | |
| HTM\FAF5_BaseAndFutureYears_Oct27_2023.csv | FAF5 Data (filtered) containing FAF flows for required years | CSV | |
| **Other** |  |  |  | 
| [parametersByYears.csv](#parametersbyyearscsv) | Parameters by scenario years. Includes AOC, aiport enplanements, cross-border tours, cross-border sentri share. | CSV | Transportation Modelers | 
| [filesByYears.csv](#filesbyyearscsv) | File names by scenario years. | CSV | Transportation Modelers | 
| trip_XX.omx | Warm start trip table; XX is the TOD (EA, AM, MD, PM, and EV) | OMX | Transportation Modelers |
| zone.txt | TAZ terminal times | Space Delimited Text File | Transportation Modelers | 
| all_vol_dfs.csv [to be updated] | | | |
| all_wait_times.csv [to be updated] | | | |
| specialEvents_() [to be updated] | | | |
<a id="land_use"></a>

## Land Use
### MGRA_BASED_INPUT_SCENARIO_YEAR.CSV
<a name="lu"></a>

| Column name | Description | 
| --- | --- |
| mgra | MGRANumber | 
| taz | TAZ Number | 
| luz_id |  | 
| pop | total population | 
| hhp | total household population (exclude gq pop) | 
| hs | housing structures | 
| hs_sf | single family structures | 
| hs_mf | multi family structures | 
| hs_mh | mobile homes | 
| hh | total number of households | 
| hh_sf | number of households - single family | 
| hh_mf | number of households - multi family | 
| hh_mh | number of mobile homes | 
| hhs | household size | 
| gq_civ | GQ civilian | 
| gq_mil | GQ military | 
| i1 | Number of households with income less than $15,000 ($2010) | 
| i2 | Number of households with income $15,000-$29,999 ($2010) | 
| i3 | Number of households with income $30,000-$44,999 ($2010) | 
| i4 | Number of households with income $45,000-$59,999 ($2010) | 
| i5 | Number of households with income $60,000-$74,999 ($2010) | 
| i6 | Number of households with income $75,000-$99,999 ($2010) | 
| i7 | Number of households with income $100,000-$124,999 ($2010) | 
| i8 | Number of households with income $125,000-$149,999 ($2010) | 
| i9 | Number of households with income $150,000-$199,999 ($2010) | 
| i10 | Number of households with income $200,000 or more ($2010) | 
| emp_gov | Government employment | 
| emp_mil | military employment | 
| emp_ag_min | Agriculture and mining employment | 
| emp_bus_svcs | Professional and Business Services employment | 
| emp_fin_res_mgm | Financial and resource management employment | 
| emp_educ | Education services employment | 
| emp_hlth | Health services employment | 
| emp_ret | Retail services employment | 
| emp_trn_wrh | Transportation and Warehousing employment | 
| emp_con | Construction employment | 
| emp_utl | Utilities office support employment | 
| emp_mnf | Manufacturing employment | 
| emp_whl | Wholesale employment | 
| emp_ent | Entertainment services employment | 
| emp_accm | Hotel and accomodation services | 
| emp_food | Food services employment | 
| emp_oth | Other employment | 
| emp_non_ws_wfh | Non-wage and salary work from home employments | 
| emp_non_ws_oth | Non-wage and salary other employments | 
| emp_total | Total employment | 
| pseudomsa | Pseudo MSA<br>1: Downtown<br>2: Central<br>3: North City<br>4: South Suburban<br>5: East Suburban<br>6: North County West<br>7: North County East<br>8: East County | 
| zip09 | 2009 Zip Code | 
| enrollgradekto8 | Grade School K-8 enrollment | 
| enrollgrade9to12 | Grade School 9-12 enrollment | 
| collegeenroll | Major College enrollment | 
| othercollegeenroll | Other College enrollment | 
| hotelroomtotal | Total number of hotel rooms | 
| parkactive | Acres of Active Park | 
| openspaceparkpreserve | Acres of Open Park or Preserve | 
| beachactive | Acres of Active Beach | 
| district27 | Special layer reg employer shuttle service around Sorrento Valley | 
| milestocoast | Distance (miles) to the nearest coast | 
| acres | Total acres in the mgra (used in CTM) | 
| land_acres | Acres of land in the mgra (used in CTM) | 
| effective_acres | Effective acres in the mgra (used in CTM) | 
| exp_hourly | Expected hourly prking cost | 
| exp_daily | Expected daily prking cost | 
| exp_monthly | Expected monthly prking cost | 
| parking_type | MGRA parking type | 
| parking_spaces | MGRA estimated parking spaces | 
| ech_dist | Elementary school district | 
| hch_dist | High school district | 
| remoteAVParking | Remote AV parking available at MGRA: 0 = Not available, 1 = Available | 
| refueling_stations | Number of refueling stations at MGRA | 
| MicroAccessTime | Micro-mobility access time (mins) | 
| microtransit | microtransit access time (mins) | 
| nev | Neighborhood Electric Vehicle access time (mins) | 
| totint | Total intersections | 
| duden | Dwelling unit density | 
| empden | Employment density | 
| popden | Population density | 
| retempden | Retail employment density | 
| totintbin | Total intersection bin | 
| empdenbin | Employment density bin | 
| dudenbin | Dwelling unit density bin | 
| PopEmpDenPerMi | Population and employment density per mile |


## Synthetic Population
<a id="population_synth_households"></a>

### Population Synthesizer Household Data
#### `HOUSEHOLDS.CSV`

| Column Name | Description |
| ----------- | ----------- |
| hhid | Unique Household ID |
| household_serial_no | Household serial number |
| taz | TAZ of household |
| mgra | MGRA of household |
| hinccat1 | Household income category:<br>1 = <$30k<br>2 = $30-60k<br>3 = $60-100k<br>4 = $100-150k<br>5 = $150k+ |
| hinc | Household income |
| num_workers | Number of workers in household |
| veh | Number of vehicles in household |
| persons | Number of persons in household |
| hht | Household/family type:<br>0 = Not in universe (vacant or GQ)<br>1 = Family household: married-couple<br>2 = Family household: male householder, no wife present<br>3 = Family household: female householder, no husband present<br>4 = Nonfamily household: male householder, living alone<br>5 = Nonfamily household: male householder, not living alone<br>6 = Nonfamily household: female householder, living alone<br>7 = Nonfamily household: female householder, not living alone |
| bldgsz | Building size - Number of Units in Structure & Quality:<br>1 = Mobile home or trailer<br>2 = One-family house detached<br>3 = One-family house attached<br>8 = 20-49 Apartments<br>9 = 50 or more apartments |
| unittype | Household unit type:<br>0 = Non-GQ Household<br>1 = GQ Household |
| version | Synthetic population run version. Presently set to 0. |
| poverty | Poverty indicator utilized for social equity reports. Percentage value where value <= 2 (200% of the [Federal Poverty Level](https://aspe.hhs.gov/2020-poverty-guidelines)) indicates household is classified under poverty. |


<a id="population_synth_persons"></a>

### Population Synthesizer Person Data
#### `PERSONS.CSV`

| Column Name          | Description                                                                                   |
|----------------------|-----------------------------------------------------------------------------------------------|
| hhid                 | Household ID                                                                                  |
| perid                | Person ID                                                                                     |
| Household_serial_no  | Household serial number                                                                        |
| pnum                 | Person Number                                                                                 |
| age                  | Age of person                                                                                 |
| sex                  | Gender of person<br>1 = Male<br>2 = Female                                                     |
| miltary             | Military status of person:<br>0 = N/A Less than 17 Years Old<br>1 = Yes, Now on Active Duty    |
| pemploy              | Employment status of person:<br>1 = Employed Full-Time<br>2 = Employed Part-Time<br>3 = Unemployed or Not in Labor Force<br>4 = Less than 16 Years Old |
| pstudent             | Student status of person:<br>1 = Pre K-12<br>2 = College Undergrad+Grad and Prof. School<br>3 = Not Attending School |
| ptype                | Person type:<br>1 = Full-time Worker<br>2 = Part-time Worker<br>3 = College Student<br>4 = Non-working Adult<br>5 = Non-working Senior<br>6 = Driving Age Student<br>7 = Non-driving Student<br>8 = Pre-school |
| educ                 | Educational attainment:<br>1 = No schooling completed<br>9 = High school graduate<br>13 = Bachelor's degree |
| grade                | School grade of person:<br>0 = N/A (not attending school)<br>2 = K to grade 8<br>5 = Grade 9 to grade 12<br>6 = College undergraduate |
| occen5               | Occupation:<br>0 = Not in universe (Under 16 years or LAST-WRK = 2)<br>1..997 = Legal census occupation code |
| occsoc5              | Detailed occupation codes defined by the Bureau of Labor Statistics |
| indcen              | Industry code.<br>0 = default<br>9970 = NAICS2 is MIL |
| weeks              | Weeks worked during past 12 months<br>0 .N/A (less than 16 years old/did not work during the past 12 .months)<br>1 .50 to 52 weeks worked during past 12 months<br>2 .48 to 49 weeks worked during past 12 months<br>3 .40 to 47 weeks worked during past 12 months<br>4 .27 to 39 weeks worked during past 12 month<br>5 .14 to 26 weeks worked during past 12 months<br>6 .13 weeks or less worked during past 12 months|
| hours              | Hours worked per week past 12 months<br>0 .N/A (less than 16 years old/did not work during the past .12 months)<br>1..98 .1 to 98 usual hours<br>99 .99 or more usual hours |
| rac1p              | Race:<br>1 = White alone<br>2 = Black or African American alone<br>3 = American Indian alone<br>4 = Alaska Native alone<br>5 = American Indian and Alaska Native tribes specified; or .American Indian or Alaska Native, not specified and no other .races<br>6 = Asian alone<br>7 = Native Hawaiian and Other Pacific Islander alone<br>8 = Some Other Race alone<br>9 =Two or More Races |
| hisp              | Hispanic origin:<br>1 = Not Hispanic<br>2 = Hispanic |
| version              | Synthetic population run version. Presently set to 0. |
| naics2_original_code              | 2 digit North American Industry Classification System (NAICS) |
| soc2              | 2 digit Standard Occupational Classification |


## Network
<a id="vehicle_class_toll"></a>

### Highway Network Vehicle Class Toll Factors File
#### `vehicle_class_toll_factors.csv`

Required file. Used to specify the relative toll values by six vehicle classes by Facility name, scenario year and time of day. Can be used, for example, to identify "free for HOV" type managed lane facilties. Used by the Import network Modeller tool.

Example:

| Facility_name | Year | Time_of_Day | DA_Factor | S2_Factor | S3_Factor | TRK_L_Factor | TRK_M_Factor | TRK_H_Factor |
| ------------- | ---- | ----------- | --------- | --------- | --------- | ------------ | ------------ | ------------ |
| I-15          | 2016 | EA          | 1.0       | 0.0       | 0.0       | 1.0          | 1.03         | 2.33         |
| SR-125        | 2016 | ALL         | 1.0       | 1.0       | 1.0       | 1.0          | 1.03         | 2.33         |
| I-5           | 2035 | ALL         | 1.0       | 1.0       | 0.0       | 1.0          | 1.03         | 2.33         |

The network links are matched to a record in this file based on the NM, FXNM or TXNM values (in that order). A simple substring matching is used, so the record with Facility_name "I-15" matches any link with name "I-15 SB", "I-15 NB", "I-15/DEL LAGO DAR NB" etc. The records should not be overlapping: if there are two records which match a given link it will be an arbitrary choice as to which one is used.

Note that if a link does not match to a record in this file, the default factors (specified in the table below) will be applied to said link. It is OK if there are records for which there are no link tolls.

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Facility_name</td>
        <td>Name of the facility, used in the substring matching with links by NM, FXNM or TXNM fields</td>
    </tr>
    <tr>
        <td>Year</td>
        <td>Scenario year</td>
    </tr>
    <tr>
        <td>Time_of_Day</td>
        <td>
            Time of day period:<br>
            EA = Early morning (3am - 5:59am)<br>
            AM = AM peak (6am to 8:59am)<br>
            MD = Mid-day (9am to 3:29pm)<br>
            PM = PM peak (3:30pm to 6:59pm)<br>
            EV = Evening (7pm to 2:59am)<br>
            ALL = All time of day periods
        </td>
    </tr>
    <tr>
        <td>DA_Factor</td>
        <td>Positive toll factor for Drive Alone (SOV) vehicle classes. The default value is 1.0</td>
    </tr>
    <tr>
        <td>S2_Factor</td>
        <td>Positive toll factor for Shared 2 person (HOV2) vehicle classes. The default value is 1.0</td>
    </tr>
    <tr>
        <td>S3_Factor</td>
        <td>Positive toll factor for Shared 3+ person (HOV3) vehicle classes. The default value is 1.0</td>
    </tr>
    <tr>
        <td>TRK_L_Factor</td>
        <td>Positive toll factor for Light Truck (TRKL) vehicle classes. The default value is 1.0</td>
    </tr>
    <tr>
        <td>TRK_M_Factor</td>
        <td>Positive toll factor for Medium Truck (TRKM) vehicle classes. The default value is 1.03</td>
    </tr>
    <tr>
        <td>TRK_H_Factor</td>
        <td>Positive toll factor for Heavy Truck (TRKH) vehicle classes. The default value is 2.03</td>
    </tr>
</table>


### `special_fares.txt`

```
boarding_cost:
   base: 
       - {line: "398104", cost: 3.63}
       - {line: "398204", cost: 3.63}
   stop_increment:
       - {line: "398104", stop: "SORRENTO VALLEY", cost: 0.46}
       - {line: "398204", stop: "SORRENTO VALLEY", cost: 0.46}
in_vehicle_cost: 
   - {line: "398104", from: "SOLANA BEACH", cost: 0.45}
   - {line: "398104", from: "SORRENTO VALLEY", cost: 0.45}
   - {line: "398204", from: "OLD TOWN",  cost: 0.45}
   - {line: "398204", from: "SORRENTO VALLEY", cost: 0.45}
day_pass: 4.54
regional_pass: 10.90
```
### MGRAs at Mobility Hubs
#### `MOBILITYHUBMGRA.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <td>Decription</td>
    </tr>
    <tr>
        <td>MGRA</td>
        <td>MGRA ID</td>
    </tr>
    <tr>
        <td>MoHubName</td>
        <td>Mobility Hub name</td>
    </tr>
    <tr>
        <td>MoHubType</td>
        <td>
            Mobility Hub type:<br>
            Suburban<br>
            Coastal<br>
            Gateway<br>
            Major Employment Center<br>
            Urban
        </td>
    </tr>
</table>


<a id="transit_binary_stop"></a>

### Transit Binary Stop Table
#### `TRSTOP.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Stop_id</td>
        <td>Unique stop ID</td>
    </tr>
    <tr>
        <td>Route_id</td>
        <td>Sequential route number</td>
    </tr>
    <tr>
        <td>Link_id</td>
        <td>Link id associated with route</td>
    </tr>
    <tr>
        <td>Pass_count</td>
        <td>Number of times the route passes this stop. Most of value is one, some value is 2</td>
    </tr>
    <tr>
        <td>Milepost</td>
        <td>Stop mile post</td>
    </tr>
    <tr>
        <td>Longitude</td>
        <td>Stop Longitude</td>
    </tr>
    <tr>
        <td>Latitude</td>
        <td>Stop Latitude</td>
    </tr>
    <tr>
        <td>NearNode</td>
        <td>Node number that stop is nearest to</td>
    </tr>
    <tr>
        <td>FareZone</td>
        <td>Zones defined in Fare System</td>
    </tr>
    <tr>
        <td>StopName</td>
        <td>Name of Stop</td>
    </tr>
    <tr>
        <td>MODE_NAME</td>
        <td>
            Line haul mode name:<br>
            Transfer<br>
            Center City Walk<br>
            Walk Access<br>
            Commuter Rail<br>
            Light Rail<br>
            Regional BRT (Yellow)<br>
            Regional BRT (Red)<br>
            Limited Express<br>
            Express<br>
            Local
        </td>
    </tr>
    <tr>
        <td>MODE_ID</td>
        <td>
            Mode ID<br>
            1 = Transfer<br>
            2 = Center City Walk<br>
            3 = Walk Access<br>
            4 = Commuter Rail<br>
            5 = Light Rail<br>
            6 = Regional BRT (Yellow)<br>
            7 = Regional BRT (Red)<br>
            8 = Limited Express<br>
            9 = Express<br>
            10 = Local
        </td>
    </tr>
    <tr>
        <td>PREMODE</td>
        <td>
            Premium Transit mode<br>
            0 = No<br>
            1 = Yes
        </td>
    </tr>
    <tr>
        <td>EXPBSMODE</td>
        <td>
            Express bus mode<br>
            0 = No<br>
            1 = Yes
        </td>
    </tr>
    <tr>
        <td>LOCMODE</td>
        <td>
            Local bus mode<br>
            0 = No<br>
            1 = Yes
        </td>
    </tr>
    <tr>
        <td>OP_TRNTIME</td>
        <td>
            Off peak transcad matrix used by mode:<br>
            *oploctime<br>
            *oppretime
        </td>
    </tr>
    <tr>
        <td>AM_TRNTIME</td>
        <td>
            AM peak transcad matrix used by mode:<br>
            *amloctime<br>
            *ampretime
        </td>
    </tr>
    <tr>
        <td>PM_TRNTIME</td>
        <td>
            PM peak transcad matrix used by mode:<br>
            *pmloctime<br>
            *pmpretime
        </td>
    </tr>
    <tr>
        <td>MODE_ACCES</td>
        <td>Mode of access (1)</td>
    </tr>
    <tr>
        <td>MODE_EGRES</td>
        <td>Mode of egress (1)</td>
    </tr>
    <tr>
        <td>WT_IVTPK</td>
        <td>Weight for peak in-vehicle time: 1, 1.5, or 1.8</td>
    </tr>
    <tr>
        <td>WT_FWTPK</td>
        <td>Weight for peak first wait time: 1, 1.5</td>
    </tr>
    <tr>
        <td>WT_XWTPK</td>
        <td>Weight for peak transfer wait time: 1, 3</td>
    </tr>
    <tr>
        <td>WT_FAREPK</td>
        <td>Weight for peak fare: 0.46, 0.60, 0.63, 0.67, 1</td>
    </tr>
    <tr>
        <td>WT_IVTOP</td>
        <td>Weight for off-peak in-vehicle time: 1, 1.5, or 1.6</td>
    </tr>
    <tr>
        <td>WT_FWTOP</td>
        <td>Weight for off-peak first wait time: 1, 1.5</td>
    </tr>
    <tr>
        <td>WT_XWTOP</td>
        <td>Weight for off-peak transfer wait time: 1, 3</td>
    </tr>
    <tr>
        <td>WT_FAREOP</td>
        <td>Weight for off-peak fare: 0.23, 0.51, 0.52, 0.54, 0.58, 1</td>
    </tr>
    <tr>
        <td>FARE</td>
        <td>Transit fare: $0, $1.25, $1.50, $2.50, $3.00, $3.50</td>
    </tr>
    <tr>
        <td>DWELLTIME</td>
        <td>Dwell time: 0, 0.3, 0.5</td>
    </tr>
    <tr>
        <td>FARETYPE</td>
        <td>
            Fare Type:<br>
            1 = Bus<br>
            2 = Rail
        </td>
    </tr>
    <tr>
        <td>FAREFIELD</td>
        <td>
            Fare Field:<br>
            coaster fare<br>
            lightrail fare
        </td>
    </tr>
    <tr>
        <td>CRMODE</td>
        <td>Boolean if Commuter rail available</td>
    </tr>
    <tr>
        <td>LRMODE</td>
        <td>Boolean if light rail available</td>
    </tr>
    <tr>
        <td>XFERPENTM</td>
        <td>Transfer Penalty time: 5 minutes</td>
    </tr>
    <tr>
        <td>WTXFERTM</td>
        <td>Transfer Wait time: 1 minute</td>
    </tr>
    <tr>
        <td>TRNTIME_EA</td>
        <td>Early AM transit time impedance</td>
    </tr>
    <tr>
        <td>TRNTIME_AM</td>
        <td>AM transit time impedance</td>
    </tr>
    <tr>
        <td>TRNTIME_MD</td>
        <td>Midday transit time impedance</td>
    </tr>
    <tr>
        <td>TRNTIME_PM</td>
        <td>PM transit time impedance</td>
    </tr>
    <tr>
        <td>TRNTIME_EV</td>
        <td>Evening transit time impedance</td>
    </tr>
</table>

<a id="transit_transfer_proh"></a>

### Transit Timed Transfers Between COASTER and Feeder Buses
#### `TIMEXFER_XX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>FROM_LINE</td>
        <td>From Route Number</td>
    </tr>
    <tr>
        <td>TO_LINE</td>
        <td>To Route Number</td>
    </tr>
    <tr>
        <td>WAIT_TIME</td>
        <td>Wait time in minutes</td>
    </tr>
</table>

<a id="transit_fares"></a>

### Transit Stop Table
#### `TRSTOP.CSV`
| Column Name  | Description |
| ------------ | ----------- |
| Stop_id      | Unique stop ID |
| Route_id     | Sequential route number |
| Link_id      | Link id associated with route |
| Pass_count   | Number of times the route passes this stop. Most of value is one, some value is 2 |
| Milepost     | Stop mile post |
| Longitude    | Stop Longitude |
| Latitude     | Stop Latitude |
| NearNode     | Node number that stop is nearest to |
| FareZone     | Zones defined in Fare System |
| StopName     | Name of Stop |
| MODE_NAME    | Line haul mode name:<br>Transfer<br>Center City Walk<br>Walk Access<br>Commuter Rail<br>Light Rail<br>Regional BRT (Yellow)<br>Regional BRT (Red)<br>Limited Express<br>Express<br>Local |
| MODE_ID      | Mode ID<br>1 = Transfer<br>2 = Center City Walk<br>3 = Walk Access<br>4 = Commuter Rail<br>5 = Light Rail<br>6 = Regional BRT (Yellow)<br>7 = Regional BRT (Red)<br>8 = Limited Express<br>9 = Express<br>10 = Local |
| PREMODE      | Premium Transit mode<br>0 = No<br>1 = Yes |
| EXPBSMODE    | Express bus mode<br>0 = No<br>1 = Yes |
| LOCMODE      | Local bus mode<br>0 = No<br>1 = Yes |
| OP_TRNTIME   | Off peak transcad matrix used by mode:<br>*oploctime<br>*oppretime |
| AM_TRNTIME   | AM peak transcad matrix used by mode:<br>*amloctime<br>*ampretime |
| PM_TRNTIME   | PM peak transcad matrix used by mode:<br>*pmloctime<br>*pmpretime |
| MODE_ACCES   | Mode of access (1) |
| MODE_EGRES   | Mode of egress (1) |
| WT_IVTPK     | Weight for peak in-vehicle time: 1, 1.5, or 1.8 |
| WT_FWTPK     | Weight for peak first wait time: 1, 1.5 |
| WT_XWTPK     | Weight for peak transfer wait time: 1, 3 |
| WT_FAREPK    | Weight for peak fare: 0.46, 0.60, 0.63, 0.67, 1 |
| WT_IVTOP     | Weight for off-peak in-vehicle time: 1, 1.5, or 1.6 |
| WT_FWTOP     | Weight for off-peak first wait time: 1, 1.5 |
| WT_XWTOP     | Weight for off-peak transfer wait time: 1, 3 |
| WT_FAREOP    | Weight for off-peak fare: 0.23, 0.51, 0.52, 0.54, 0.58, 1 |
| FARE         | Transit fare: $0, $1.25, $1.50, $2.50, $3.00, $3.50 |
| DWELLTIME    | Dwell time: 0, 0.3, 0.5 |
| FARETYPE     | Fare Type:<br>1 = Bus<br>2 = Rail |
| FAREFIELD    | Fare Field:<br>coaster fare<br>lightrail fare |
| CRMODE       | Boolean if Commuter rail available |
| LRMODE       | Boolean if light rail available |
| XFERPENTM    | Transfer Penalty time: 5 minutes |
| WTXFERTM     | Transfer Wait time: 1 minute |
| TRNTIME_EA   | Early AM transit time impedance |
| TRNTIME_AM   | AM transit time impedance |
| TRNTIME_MD   | Midday transit time impedance |
| TRNTIME_PM   | PM transit time impedance |
| TRNTIME_EV   | Evening transit time impedance |

<!-- <a href="#top">Go To Top</a> -->
<a id="tr_link"></a>

### Transit Link File
#### `TRLINK.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Route_id:</td>
        <td>Sequential route number</td>
    </tr>
    <tr>
        <td>Link_id</td>
        <td>Link id associated with route</td>
    </tr>
    <tr>
        <td>Direction</td>
        <td>+ or -</td>
    </tr>
</table>


<a id="bike_net_link"></a>

### Bike Network Link Field List
#### `SANDAG_BIKE_NET.DBF`

| Column Name | Description |
| ----------- | ----------- |
| ROADSEGID   | Road Segment ID |
| RD20FULL    | Road/Street Name |
| A           | Foreign key of first node |
| B           | Foreign key of second node |
| A_LEVEL     | Level of first node |
| B_LEVEL     | Level of second node |
| Distance    | Arc length of link (ft) |
| AB_Gain     | Cumulative non-negative increase in elevation from A to B nodes (ft) |
| BA_Gain     | Cumulative non-negative increase in elevation from B to A nodes (ft) |
| ABBikeClas  | Type of Bike Classification in AB direction where:<br>1 = Multi-Use Path<br>2 = Bike Lane<br>3 = Bike Route |
| BABikeClas  | Type of Bike Classification in BA direction where:<br>1 = Multi-Use Path<br>2 = Bike Lane<br>3 = Bike Route |
| AB_Lanes    | Number of Lanes in AB direction |
| BA_Lanes    | Number of Lanes in BA direction |
| Func_Class  | Type of Road Functional Class where:<br>1 = Freeway to Freeway Ramp<br>2 = Light (2-lane) Collector Street<br>3 = Rural Collector Road<br>4 = Major Road/4-lane Major Road<br>5 = Rural Light Collector/Local Road<br>6 = Prime Arterial<br>7 = Private Street<br>8 = Recreational Parkway<br>9 = Rural Mountain Road<br>A = Alley<br>B = Class I Bicycle Path<br>C = Collector/4-lane Collector Street<br>D = Two-lane Major Street<br>E = Expressway<br>F = Freeway<br>L = Local Street/Cul-de-sac<br>M = Military Street within Base<br>P = Paper Street<br>Q = Undocumented<br>R = Freeway/Expressway On/Off Ramp<br>S = Six-lane Major Street<br>T = Transitway<br>U = Unpaved Road<br>W = Pedestrian Way/Bikeway |
| Bike2Sep    | Separated Bike Lane Flag where:<br>0 = No<br>1 = Yes |
| Bike3Blvd   | Bike Boulevard Lane Flag where:<br>0 = No<br>1 = Yes |
| SPEED       | Road Speed |
| A_Elev      | A Node Elevation |
| B_Elev      | B Node Elevation |
| ProjectID   | Project ID in the regional bike network |
| Year        | Year built/opened to the public |
| Scenicldx   | Scenic index represents the closeness to the ocean and parks |
| Path        | Null |
| Shape_Leng  | length of the link (ft) |

<a id="bike_net_node"></a>

### Bike Network Node Field List
#### `SANDAG_BIKE_NODE.DBF`
<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>NodeLev_ID</td>
        <td>Node Unique Identifier</td>
    </tr>
    <tr>
        <td>MGRA</td>
        <td>MGRA ID for Centroids</td>
    </tr>
    <tr>
        <td>TAZ</td>
        <td>TAZ ID for Centroids</td>
    </tr>
    <tr>
        <td>TAP</td>
        <td>TAP ID</td>
    </tr>
    <tr>
        <td>XCOORD</td>
        <td>X Coordinate of Node in NAD 1983 State Plane California Region VI FIPS: 0406 (ft)</td>
    </tr>
    <tr>
        <td>YCOORD</td>
        <td>Y Coordinate of Node in NAD 1983 State Plane California Region VI FIPS: 0406(ft)</td>
    </tr>
    <tr>
        <td>ZCOORD</td>
        <td>Elevation (ft)</td>
    </tr>
    <tr>
        <td>Signal</td>
        <td>
            Traffic Signal Presence where:<br>
            0 = Absence<br>
            1 = Presence
        </td>
    </tr>
</table>



<a id="bike_taz_logsum"></a>

### Bike TAZ Logsum
#### `BIKETAZLOGSUM.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>i</td>
        <td>Origin TAZ</td>
    </tr>
    <tr>
        <td>j</td>
        <td>Destination TAZ</td>
    </tr>
    <tr>
        <td>Logsum</td>
        <td>Logsum - a measure of the closeness of the origin and the destination of the trip</td>
    </tr>
    <tr>
        <td>time</td>
        <td>Time (In minutes) </td>
    </tr>
</table>

<a id="bike_mgra_logsum"></a>

### Bike MGRA Logsum
#### `BIKEMGRALOGSUM.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>i</td>
        <td>Origin of MGRA</td>
    </tr>
    <tr>
        <td>j</td>
        <td>Destination of MGRA</td>
    </tr>
    <tr>
        <td>Logsum</td>
        <td>Logsum - a measure of the closeness of the origin and the destination of the trip</td>
    </tr>
    <tr>
        <td>time</td>
        <td>Time (in minutes) </td>
    </tr>
</table>

## Airport
<a id="airport_nights"></a>

### Airport Number of Nights by Purpose Distribution
#### `AIRPORT_NIGHTS.SAN.CSV AND AIRPORT_NIGHTS.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Nights</td>
        <td>Number of Nights stayed (0 through 14+)</td>
    </tr>
    <tr>
        <td>purp1_perc</td>
        <td>Distribution for Resident Personal purpose</td>
    </tr>
    <tr>
        <td>purp2_perc</td>
        <td>Distribution for Visitor Business purpose</td>
    </tr>
    <tr>
        <td>purp3_perc</td>
        <td>Distribution for Visitor Personal purpose</td>
    </tr>
    <tr>
        <td>purp4_perc</td>
        <td>Distribution for External purpose</td>
    </tr>
</table>

<a id="airport_income"></a>

### Airport Income by Purpose Distribution
#### `AIRPORT_INCOME.SAN.CSV AND AIRPORT_INCOME.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Income group</td>
        <td>
            Household income:<br>
            0 = Less than $25K<br>
            1 = $25K – $50K<br>
            2 = $50K – $75K<br>
            3 = $75K – $100K<br>
            4 = $100K – $125K<br>
            5 = $125K – $150K<br>
            6 = $150K – $200K<br>
            7 = $200K plus
        </td>
    </tr>
    <tr>
        <td>purp1_perc</td>
        <td>Distribution for Resident Personal purpose</td>
    </tr>
    <tr>
        <td>purp2_perc</td>
        <td>Distribution for Visitor Business purpose</td>
    </tr>
    <tr>
        <td>purp3_perc</td>
        <td>Distribution for Visitor Personal purpose</td>
    </tr>
    <tr>
        <td>purp4_perc</td>
        <td>Distribution for External purpose</td>
    </tr>
</table>

<a id="airport_departure"></a>

### Airport Departure Time by Purpose Distribution
#### `AIRPORT_DEPARTURE.SAN.CSV` and `AIRPORT_DEPARTURE.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Period</td>
        <td>
            Departure Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>purp1_perc</td>
        <td>Distribution for Resident Personal purpose</td>
    </tr>
    <tr>
        <td>purp2_perc</td>
        <td>Distribution for Visitor Business purpose</td>
    </tr>
    <tr>
        <td>purp3_perc</td>
        <td>Distribution for Visitor Personal purpose</td>
    </tr>
    <tr>
        <td>purp4_perc</td>
        <td>Distribution for External purpose</td>
    </tr>
</table>


<a id="airport_arrival"></a>

### Airport Arrival Time by Purpose Distribution
#### `AIRPORT_ARRIVAL.SAN.CSV` and `AIRPORT_ARRIVAL.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Period</td>
        <td>
            Arrival Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>purp1_perc</td>
        <td>Distribution for Resident Personal purpose</td>
    </tr>
    <tr>
        <td>purp2_perc</td>
        <td>Distribution for Visitor Business purpose</td>
    </tr>
    <tr>
        <td>purp3_perc</td>
        <td>Distribution for Visitor Personal purpose</td>
    </tr>
    <tr>
        <td>purp4_perc</td>
        <td>Distribution for External purpose</td>
    </tr>
</table>

<a id="cvm_establishment_synthesis"></a>

## Commercial Vehicle Model
### `PERCENT_OF_ESTABLISHMENTS_BY_LUZ_SIZE_EMP_CAT.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Info</td>
        <td>
            Basic information of the data, and the size class definitions included
        </td>
    </tr>
    <tr>
        <td>Data</td>
        <td>Detailed industry sector breakdown by size class and LUZ</td>
    </tr>
    
</table>

<a id ="cvm"></a>

### `SYNTHESTABLISHMENTS.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Establishment_ID</td>
        <td>
            Unique establishment id ( index ) for each establishment
        </td>
    </tr>
    <tr>
        <td>Industry_No</td>
        <td>The category of industry that the establishment belongs to (range: 1 - 12 )</td>
    </tr>
     <tr>
        <td>Industry_Name</td>
        <td>The name of industry that the establishment belongs to ( EPO, AGM, CON etc.)/td>
    </tr>
     <tr>
        <td>LUZ</td>
        <td>Land Use Zone ( Or the TAZ) to which the establishment belongs to</td>
    </tr>
     <tr>
        <td>MGRA</td>
        <td>The MGRA number to which establishment belongs to </td>
    </tr>
     <tr>
        <td>Employees</td>
        <td>The number of employees in the establishment</td>
    </tr>
     <tr>
        <td>Size_Class</td>
        <td>The categorized size of the establishment based on the number of employees</td>
    </tr>
    
</table>

##### `MGRAEmpByEstSize.csv`
<table>
  <tr>
    <td>Field 
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>mgra
    </td>
    <td>MGRA id
    </td>
  </tr>
  <tr>
    <td>taz
    </td>
    <td>TAZ id
    </td>
  </tr>
  <tr>
    <td>luz
    </td>
    <td>Land use zone

  </tr>
  <tr>
    <td>emp_Sector_Size_Class
    </td>
    <td>Employment numbers, where Sector is gov, mil, ag_min, bus_svcs, 
fin_res_mgm, educ, hlth, ret, trn_wrh, con, utl, mnf, whl, ent, accm, food, 
oth; where Size_Class ranges 1-7.

  </tr>
  

</table>

#### CVM Establishment Synthesis File ( SummarySynthEstabs.csv )
##### `SummarySynthEstabs.csv`

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>Industry_No
    </td>
    <td>Industry No
    </td>
  </tr>
  <tr>
    <td>Industry_Name
    </td>
    <td>Industry name
    </td>
  </tr>
  <tr>
    <td>Size_Class
    </td>
    <td>Size class
</td>
  </tr>
  <tr>
    <td>Count
    </td>
    <td>Total number
  </td>
  </tr>
</table>



<a id="cvm_industry"></a>

<table>
    <tr>
        <th>Industry No</th>
        <th>Industry Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>1</td>
        <td>
            AGM
        </td>
        <td>Agriculture, Forestry, Fishing, and Hunting, and Mining</td>
    </tr>
    <tr>
        <td>2</td>
        <td>
            MFG
        </td>
        <td>Manufacturing</td>
    </tr>
    <tr>
        <td>3</td>
        <td>
            IUT
        </td>
        <td>Industrial Utilities</td>
    </tr>
    <tr>
        <td>4</td>
        <td>
            RET
        </td>
        <td>Retail Trade</td>
    </tr>
    <tr>
        <td>5</td>
        <td>
            WHL
        </td>
        <td>Wholesale Trade</td>
    </tr>
    <tr>
        <td>6</td>
        <td>
            CON
        </td>
        <td>Construction</td>
    </tr>
    <tr>
        <td>7</td>
        <td>
            TRN
        </td>
        <td>Transportation and Warehousing</td>
    </tr>
    <tr>
        <td>8</td>
        <td>
            IFR
        </td>
        <td>Information, Financial, Insurance, Real Estate, and Professional Services</td>
    </tr>
    <tr>
        <td>9</td>
        <td>
            EPO
        </td>
        <td>Education, Public, and Other Services</td>
    </tr>
    <tr>
        <td>10</td>
        <td>
            MHS
        </td>
        <td>Medical and Health Services</td>
    </tr>
    <tr>
        <td>11</td>
        <td>
            LAF
        </td>
        <td>Leisure, Accomodations, and Food</td>
    </tr>
    <tr>
        <td>12</td>
        <td>
            MIL
        </td>
        <td>Military</td>
    </tr>
    
</table>

## Crossborder

<a id="cross_border_entry_return"></a>

### Cross Border Model Tour Entry and Return Distribution
#### `CROSSBORDER_TOURENTRYANDRETURN.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Purpose</td>
        <td>
            Tour Purpose:<br>
            0 = Work<br>
            1 = School<br>
            2 = Cargo<br>
            3 = Shop<br>
            4 = Visit<br>
            5 = Other
        </td>
    </tr>
    <tr>
        <td>EntryPeriod</td>
        <td>
            Entry Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>Return Period</td>
        <td>
            Return Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>Percent</td>
        <td>Distribution of tours in entry and return period time slots</td>
    </tr>
</table>

<a id="cross_border_supercolonia"></a>

### Cross Border Model Supercolonia
#### `CROSSBORDER_SUPERCOLONIA.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Supercolonia_ID</td>
        <td>Super colonia ID</td>
    </tr>
    <tr>
        <td>Population</td>
        <td>Population of the super colonia</td>
    </tr>
    <tr>
        <td>Distance_poe0</td>
        <td>Distance from colonia to point of entry 0 (San Ysidro)</td>
    </tr>
    <tr>
        <td>Distance_poe1</td>
        <td>Distance from colonia to point of entry 1 (Otay Mesa)</td>
    </tr>
    <tr>
        <td>Distance_poe2</td>
        <td>Distance from colonia to point of entry 2 (Tecate)</td>
    </tr>
</table>

<a id="cross_border_wait_time"></a>

### Cross Border Model Point of Entry Wait Time
#### `CROSSBORDER_POINTOFENTRYWAITTIME.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>poe</td>
        <td>
            Point of Entry number:<br>
            0 = San Ysidro<br>
            1 = Otay Mesa<br>
            2 = Tecate<br>
            3 = Otay Mesa East<br>
            4 = Jacumba
        </td>
    </tr>
    <tr>
        <td>StartHour</td>
        <td>Start Hour (1 through 12)</td>
    </tr>
    <tr>
        <td>EndHour</td>
        <td>End Hour (1 through 12)</td>
    </tr>
    <tr>
        <td>StartPeriod</td>
        <td>
            Start Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>EndPeriod</td>
        <td>
            End Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>StandardWait</td>
        <td>Standard wait time</td>
    </tr>
    <tr>
        <td>SENTRIWait</td>
        <td>SENTRI users wait time</td>
    </tr>
    <tr>
        <td>PedestrianWait</td>
        <td>Pedestrian wait time</td>
    </tr>
</table>

<a id="cross_border_stops"></a>

### Cross Border Model Stop Frequency
#### `CROSSBORDER_STOPFREQUENCY.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Purpose</td>
        <td>
            Tour Purpose:<br>
            0 = Work<br>
            1 = School<br>
            2 = Cargo<br>
            3 = Shop<br>
            4 = Visit<br>
            5 = Other
        </td>
    </tr>
    <tr>
        <td>DurationLo</td>
        <td>Lower bound of tour duration (0, 4, or 8)</td>
    </tr>
    <tr>
        <td>DurationHi</td>
        <td>Upper bound of tour duration (4, 8, or 24)</td>
    </tr>
    <tr>
        <td>Outbound</td>
        <td>Number of stops on the outbound (0, 1, 2, 3+)</td>
    </tr>
    <tr>
        <td>Inbound</td>
        <td>Number of stops on the inbound (0, 1, 2, 3+)</td>
    </tr>
    <tr>
        <td>Percent</td>
        <td>Distribution of tours by purpose, duration, number of outbound/inbound stops</td>
    </tr>
</table>

<a id="cross_border_stop_purpose"></a>

### Cross Border Model Stop Purpose Distribution
#### `CROSSBORDER_STOPPURPOSE.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>TourPurp</td>
        <td>
            Tour Purpose:<br>
            0 = Work<br>
            1 = School<br>
            2 = Cargo<br>
            3 = Shop<br>
            4 = Visit<br>
            5 = Other
        </td>
    </tr>
    <tr>
        <td>Inbound</td>
        <td>Boolean for whether stop is inbound (0=No, 1=Yes)</td>
    </tr>
    <tr>
        <td>StopNum</td>
        <td>Stop number on tour (1, 2, or 3)</td>
    </tr>
    <tr>
        <td>Multiple</td>
        <td>Boolean for whether there are multiple stops on tour (0=No, 1=Yes)</td>
    </tr>
    <tr>
        <td>StopPurp0</td>
        <td>Distribution of Work stops</td>
    </tr>
    <tr>
        <td>StopPurp1</td>
        <td>Distribution of School stops</td>
    </tr>
    <tr>
        <td>StopPurp2</td>
        <td>Distribution of Cargo stops</td>
    </tr>
    <tr>
        <td>StopPurp3</td>
        <td>Distribution of Shopping stops</td>
    </tr>
    <tr>
        <td>StopPurp4</td>
        <td>Distribution of Visiting stops</td>
    </tr>
    <tr>
        <td>StopPurp5</td>
        <td>Distribution of Other stops</td>
    </tr>
</table>

<a id="cross_border_out_stop"></a>

### Cross Border Model Outbound Stop Duration Distribution
#### `CROSSBORDER_OUTBOUNDSTOPDURATION.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>RemainingLow</td>
        <td>
            Lower bound of remaining half hour periods after last scheduled trip:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>RemainingHigh</td>
        <td>
            Upper bound of remaining half hour periods after last scheduled trip:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>Stop</td>
        <td>Stop number on tour (1, 2, or 3)</td>
    </tr>
    <tr>
        <td>0</td>
        <td>Probability that stop departure is in same period as last outbound trip</td>
    </tr>
    <tr>
        <td>1</td>
        <td>Probability that stop departure is in last outbound trip period + 1</td>
    </tr>
    <tr>
        <td>2</td>
        <td>Probability that stop departure is in last outbound trip period + 2</td>
    </tr>
    <tr>
        <td>3</td>
        <td>Probability that stop departure is in last outbound trip period + 3</td>
    </tr>
    <tr>
        <td>4</td>
        <td>Probability that stop departure is in last outbound trip period + 4</td>
    </tr>
    <tr>
        <td>5</td>
        <td>Probability that stop departure is in last outbound trip period + 5</td>
    </tr>
    <tr>
        <td>6</td>
        <td>Probability that stop departure is in last outbound trip period + 6</td>
    </tr>
    <tr>
        <td>7</td>
        <td>Probability that stop departure is in last outbound trip period + 7</td>
    </tr>
    <tr>
        <td>8</td>
        <td>Probability that stop departure is in last outbound trip period + 8</td>
    </tr>
    <tr>
        <td>9</td>
        <td>Probability that stop departure is in last outbound trip period + 9</td>
    </tr>
    <tr>
        <td>10</td>
        <td>Probability that stop departure is in last outbound trip period + 10</td>
    </tr>
    <tr>
        <td>11</td>
        <td>Probability that stop departure is in last outbound trip period + 11</td>
    </tr>
</table>

<a id="cross_border_in_stop"></a>

### Cross Border Model Inbound Stop Duration Distribution
#### `CROSSBORDER_INBOUNDSTOPDURATION.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>RemainingLow</td>
        <td>
            Lower bound of remaining half hour periods after last scheduled trip:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>RemainingHigh</td>
        <td>
            Upper bound of remaining half hour periods after last scheduled trip:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>Stop</td>
        <td>Stop number on tour (1, 2, or 3)</td>
    </tr>
    <tr>
        <td>0</td>
        <td>Probability that stop departure period is same as tour arrival period</td>
    </tr>
    <tr>
        <td>-1</td>
        <td>Probability that stop departure period is tour arrival period - 1</td>
    </tr>
    <tr>
        <td>-2</td>
        <td>Probability that stop departure period is tour arrival period – 2</td>
    </tr>
    <tr>
        <td>-3</td>
        <td>Probability that stop departure period is tour arrival period – 3</td>
    </tr>
    <tr>
        <td>-4</td>
        <td>Probability that stop departure period is tour arrival period – 4</td>
    </tr>
    <tr>
        <td>-5</td>
        <td>Probability that stop departure period is tour arrival period – 5</td>
    </tr>
    <tr>
        <td>-6</td>
        <td>Probability that stop departure period is tour arrival period – 6</td>
    </tr>
    <tr>
        <td>-7</td>
        <td>Probability that stop departure period is tour arrival period - 7</td>
    </tr>
</table>

<a id="external_trip"></a>

## External Models
### `EXTERNALEXTERNALTRIPSByYEAR.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>originTAZ</td>
        <td>External origin TAZ</td>
    </tr>
    <tr>
        <td>destinationTAZ</td>
        <td>External destination TAZ</td>
    </tr>
    <tr>
        <td>Trips</td>
        <td>Number of trips between external TAZs</td>
    </tr>
</table>

<a id="external_internal"></a>

### External Internal Control Totals
#### `EXTERNALINTERNALCONTROLTOTALSByYEAR.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Taz</td>
        <td>External TAZ station</td>
    </tr>
    <tr>
        <td>Work</td>
        <td>Number of work vehicle trips</td>
    </tr>
    <tr>
        <td>Nonwork</td>
        <td>Number of non-work vehicle trips</td>
    </tr>
</table>

<a id="internal_external_tod"></a>

### Internal External Tours Time of Day Distribution
#### `INTERNALEXTERNAL_TOURTOD.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Purpose</td>
        <td>
            Tour Purpose:<br>
            0 = All Purposes
        </td>
    </tr>
    <tr>
        <td>EntryPeriod</td>
        <td>
            Entry Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>ReturnPeriod</td>
        <td>
            Return Period:<br>
            1 = Before 5:00AM<br>
            2 = 5:00AM-5:30AM<br>
            3 through 39 is every half hour time slots<br>
            40 = After 12:00AM
        </td>
    </tr>
    <tr>
        <td>Percent</td>
        <td>Distribution of tours by entry and return periods</td>
    </tr>
</table>

## Heavy Truck Model (HTM)
### Inputs SANDAG HTM
#### `INPUTS_SANDAG_HTM_<SCENARIO_YEAR>.XLSX`

<table>
    <tr>
        <th>Sheet Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>FAZ_County</td>
        <td>County-FAZ Mapping</td>
    </tr>
    <tr>
        <td>TAZ_FAZ</td>
        <td>Identifies in which FAZ, each TAZ is located</td>
    </tr>
    <tr>
        <td>OtherMode_Truck</td>
        <td>Determines what percentage of each mode belongs to trucks</td>
    </tr>
    <tr>
        <td>SD_Flows</td>
        <td>Identifies all OD pairs that have at least one end in SANDAG region or passes through the SANDAG region.</td>
    </tr>
    <tr>
        <td>FAZ_Gateway</td>
        <td>A look up table that corresponds FAF FAZ that are outside the SANDAG region to one/many SANDAG Gateways. This table also includes area code of each FAZ that is outside SANDAG region.
</td>
    </tr>
    <tr>
        <td>Commodity_Group</td>
        <td>Determines the commodity group (15 categories) to which each of the 43 commodities from the FAF data belongs.</td>
    </tr>
    <tr>
        <td>EMP_Calc</td>
        <td>Calculate the share of each of the 3 digits NAICS employee categories withing the 19 categories SANDAG ABM employee for each of the 5 FAZs in San Diego County</td>
    </tr>
    <tr>
        <td>EMP_Converter</td>
        <td>Provides a table that correlates SANDAG model employee categories with corresponding NAICS employee categories.
</td>
    </tr>
    <tr>
        <td>CG_Emp_P</td>
        <td>Establishes the relationship between each commodity group and the NAICS employee category for the production end.</td>
    </tr>
    <tr>
        <td>CG_Emp_A</td>
        <td>Establishes the relationship between each commodity group and the NAICS employee category for the attraction end.</td>
    </tr>
    <tr>
        <td>Annual_Factor</td>
        <td>Number of business days in a year.</td>
    </tr>
    <tr>
        <td>Truck_Dist</td>
        <td>Provides the percent distribution of truck type based on OD distance. </td>
    </tr>
    <tr>
        <td>Payload</td>
        <td>Average pounds of load that each truck type can carry based on commodity groups.
</td>
    </tr>
    <tr>
        <td>Time_of_Day</td>
        <td>Provides distribution of trucks throughout the day.</td>
    </tr>
    <tr>
        <td>External_Count</td>
        <td>The Inbound and outbound truck counts by type at each of the 12 SANDAG gateways. For base year, this is the daily truck counts at the gateways.
</td>
    </tr>
    <tr>
        <td>SRA_Dist</td>
        <td>The overall distribution of trucks when they cross each of the gateway from/to each of the 63 SRAs. For base year this information is calculated from truck GPS data</td>
    </tr>
    <tr>
        <td>SRA_TAZ</td>
        <td>SRA-TAZ Mapping</td>
    </tr>
    
</table>

### FAF5_BaseAndFutureYears_Oct27_2023 (Possible values for <year> : 2017, 2025,2030,2035,2040,2045,2050)
#### `FAF5_BaseAndFutureYears_Oct27_2023.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>dms_orig</td>
        <td>FAF region or state where a freight movement begins the domestic portion of shipment. For imports, this is the US entry region where an import enters United States.
</td>
    </tr>
    <tr>
        <td>dms_dest</td>
        <td>FAF region or state where a freight movement ends the domestic portion of shipment. For exports, this is the US exit region where an export leaves United States.
</td>
    </tr>
    <tr>
        <td>Mode</td>
        <td>Mode used between domestic origins and destinations</td>
    </tr>
    <tr>
        <td>Commodity</td>
        <td>2-digit level of the Standard Classification of Transported Goods ( SCTG) </td>
    </tr>
    <tr>
        <td>Direction</td>
        <td>Trade Direction : II or XI
</td>
    </tr>
    <tr>
        <td>Trade</td>
        <td>Trade Type : Domestic or Foreign</td>
    </tr>
    <tr>
        <td>fr_orig</td>
        <td>Foreign region of shipment origin</td>
    </tr>
    <tr>
        <td>fr_dest</td>
        <td>Foreign region of shipment destination
</td>
    </tr>
    <tr>
        <td>fr_inmode</td>
        <td>Mode used between a foreign region and the US entry region for the imported goods</td>
    </tr>
    <tr>
        <td>fr_outmode</td>
        <td>Mode used between the US exit region and foreign region for the exported goods</td>
    </tr>
    <tr>
        <td>distons_year</td>
        <td>Total weight of commodities shipped (unit: Thousand Tons) in year</td>
    </tr>
    <tr>
        <td>disvalue_year</td>
        <td>Total value (in 2017 constant dollar) of commodities shipped (unit: Million Dollars) in year </td>
    </tr>
    
    
</table>

### Mode Dictionary
<table>

<tr>
        <th>Numeric Label</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>1</td>
        <td>Truck
</td>
    </tr>
    <tr>
        <td>2</td>
        <td>Rail
</td>
    </tr>
    <tr>
        <td>3</td>
        <td>Water</td>
    </tr>
    <tr>
        <td>4</td>
        <td>Air ( include truck-air) </td>
    </tr>
    <tr>
        <td>5</td>
        <td>Multiple modes & mail
</td>
    </tr>
    <tr>
        <td>6</td>
        <td>Pipeline</td>
    </tr>
    <tr>
        <td>7</td>
        <td>Other and unknown</td>
    </tr>
    <tr>
        <td>8</td>
        <td>No domestic mode
</td>
    </tr>
       
    
</table>

### Commodity Groups Dictionary
<table>

<tr>
        <th>Numeric Label</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>01</td>
        <td>Live animals/fish
</td>
    </tr>
    <tr>
        <td>02</td>
        <td>Cereal grains
</td>
    </tr>
    <tr>
        <td>03</td>
        <td>Other ag prods.</td>
    </tr>
    <tr>
        <td>04</td>
        <td>Animal feed</td>
    </tr>
    <tr>
        <td>05</td>
        <td>Meat/seafood
</td>
    </tr>
    <tr>
        <td>06</td>
        <td>Milled grain prods.</td>
    </tr>
    <tr>
        <td>07</td>
        <td>Other foodstuffs</td>
    </tr>
    <tr>
        <td>08</td>
        <td>Alcoholic Beverages
</td>
    </tr>
    <tr>
        <td>09</td>
        <td>Tobacco prods
</td>
    </tr>
    <tr>
        <td>10</td>
        <td>Building stone
</td>
    </tr>
    <tr>
        <td>11</td>
        <td>Natural sands
</td>
    </tr>
    <tr>
        <td>12</td>
        <td>Gravel
</td>
    </tr>
    <tr>
        <td>13</td>
        <td>Nonmetallic minerals
</td>
    </tr>
    <tr>
        <td>14</td>
        <td>Metallic ores
</td>
    </tr>
    <tr>
        <td>15</td>
        <td>Coal
</td>
    </tr>
    <tr>
        <td>16</td>
        <td>Crude Petroleum
</td>
    </tr>
    <tr>
        <td>17</td>
        <td>Gasoline
</td>
    </tr>
    <tr>
        <td>18</td>
        <td>Fuel oils
</td>
    </tr>
    <tr>
        <td>19</td>
        <td>Natural gas and other fossil products
</td>
    </tr>
    <tr>
        <td>20</td>
        <td>Basic chemicals
</td>
    </tr>
    <tr>
        <td>21</td>
        <td>Pharmaceuticals
</td>
    </tr>
    <tr>
        <td>22</td>
        <td>Fertilizers
</td>
    </tr>
    <tr>
        <td>23</td>
        <td>Chemical prods.
</td>
    </tr>
    <tr>
        <td>24</td>
        <td>Plastics/rubber
</td>
    </tr>
    <tr>
        <td>25</td>
        <td>Logs
</td>
    </tr>
    <tr>
        <td>26</td>
        <td>Wood prods
</td>
    </tr>
    <tr>
        <td>27</td>
        <td>Newsprint/paper
</td>
    </tr>
    <tr>
        <td>28</td>
        <td>Paper articles
</td>
    </tr>
    <tr>
        <td>29</td>
        <td>Printed prods.
</td>
    </tr>
    <tr>
        <td>30</td>
        <td>Textiles/leather
</td>
    </tr>
    <tr>
        <td>31</td>
        <td>Nonmetal min. prods.
</td>
    </tr>
    <tr>
        <td>32</td>
        <td>Base metals
</td>
    </tr>
    <tr>
        <td>33</td>
        <td>Articles-base metal
</td>
    </tr>
    <tr>
        <td>34</td>
        <td>Machinery
</td>
    </tr>
    <tr>
        <td>35</td>
        <td>Electronics
</td>
    </tr>
    <tr>
        <td>36</td>
        <td>Motorized Vehicles
</td>
    </tr>
    <tr>
        <td>37</td>
        <td>Transport equip.
</td>
    </tr>
    <tr>
        <td>38</td>
        <td>Precision instruments
</td>
    </tr>
    <tr>
        <td>39</td>
        <td>Furniture
</td>
    </tr>
    <tr>
        <td>40</td>
        <td>Misc. mfg. prods.
</td>
    </tr>
    <tr>
        <td>41</td>
        <td>Waste/scrap
</td>
    </tr>
    <tr>
        <td>43</td>
        <td>Mixed freight
</td>
    </tr>
    
</table>

## Others
### Parameters by Scenario Years
#### `PARAMETERSBYYEARS.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>year</td>
        <td>Scenario build year</td>
    </tr>
    <tr>
        <td>aoc.fuel</td>
        <td>Auto operating fuel cost</td>
    </tr>
    <tr>
        <td>aoc.maintenance</td>
        <td>Auto operating maitenance cost</td>
    </tr>
    <tr>
        <td>airport.SAN.enplanements</td>
        <td>San Diego International Airport enplanements</td>
    </tr>
    <tr>
        <td>airport.SAN.connecting</td>
        <td>San Diego International Airport connecting passengers</td>
    </tr>
    <tr>
        <td>airport.SAN.airportMgra</td>
        <td>MGRA San Diego International Airport is located in</td>
    </tr>
    <tr>
        <td>airport.CBX.enplanements</td>
        <td>Cross Border Express Terminal (Tijuana International Airport) enplanements</td>
    </tr>
    <tr>
        <td>airport.CBX.connecting</td>
        <td>Cross Border Express Terminal (Tijuana International Airport) connecting passengers</td>
    </tr>
    <tr>
        <td>airport.CBX.airportMgra</td>
        <td>MGRA Cross Border Express Terminal is located in</td>
    </tr>
    <tr>
        <td>crossBorder.tours</td>
        <td>Number of cross border tours</td>
    </tr>
    <tr>
        <td>crossBorders.sentriShare</td>
        <td>Share of cross border tours that are SENTRI</td>
    </tr>
    <tr>
        <td>taxi.baseFare</td>
        <td>Initial taxi fare</td>
    </tr>
    <tr>
        <td>taxi.costPerMile</td>
        <td>Taxi cost per mile</td>
    </tr>
    <tr>
        <td>taxi.cosPerMinute</td>
        <td>Taxi cost per minute</td>
    </tr>
    <tr>
        <td>TNC.single.baseFare</td>
        <td>Initial TNC fare for single ride</td>
    </tr>
    <tr>
        <td>TNC.single.costPerMile</td>
        <td>TNC cost per mile for single ride</td>
    </tr>
    <tr>
        <td>TNC.single.costPerMinute</td>
        <td>TNC cost per minute for single ride</td>
    </tr>
    <tr>
        <td>TNC.single.costMinimum</td>
        <td>TNC minimum cost for single ride</td>
    </tr>
    <tr>
        <td>TNC.shared.baseFare</td>
        <td>Initial TNC fare for shared ride</td>
    </tr>
    <tr>
        <td>TNC.shared.costPerMile</td>
        <td>TNC cost per mile for shared ride</td>
    </tr>
    <tr>
        <td>TNC.shared.costPerMinute</td>
        <td>TNC cost per minute for shared ride</td>
    </tr>
    <tr>
        <td>TNC.shared.costMinimum</td>
        <td>TNC minimum cost for shared ride</td>
    </tr>
    <tr>
        <td>Mobility.AV.RemoteParkingCostPerHour</td>
        <td>Remote parking cost per hour for autonomous vehicles</td>
    </tr>
    <tr>
        <td>active.micromobility.variableCost</td>
        <td>Variable cost for micromobility</td>
    </tr>
    <tr>
        <td>active.micromobility.fixedCost</td>
        <td>Fixed cost for micromobility</td>
    </tr>
    <tr>
        <td>active.microtransit.fixedCost</td>
        <td>Fixed cost for microtransit</td>
    </tr>
    <tr>
        <td>Mobility.AV.Share</td>
        <td>The share of vehicles assumed to be autonomous vehicles in the vehicle fleet</td>
    </tr>
    <tr>
        <td>smartSignal.factor.LC</td>
        <td></td>
    </tr>
    <tr>
        <td>smartSignal.factor.MA</td>
        <td></td>
    </tr>
    <tr>
        <td>smartSignal.factor.PA</td>
        <td></td>
    </tr>
    <tr>
        <td>atdm.factor</td>
        <td></td>
    </tr>
</table>

### Files by Scenario Years
#### `FILESBYYEARS.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>year</td>
        <td>Scenario build year</td>
    </tr>
    <tr>
        <td>crossborder.dc.soa.alts.file</td>
        <td>Crossborder model destination choice alternatives file</td>
    </tr>
    <tr>
        <td>crossBorder.dc.uec.file</td>
        <td>Crossborder model destination choice UEC file</td>
    </tr>
    <tr>
        <td>uwsl.dc.uec.file</td>
        <td>Tour destination choice UEC file</td>
    </tr>
    <tr>
        <td>nmdc.uec.file</td>
        <td>Non-mandatory tour destination choice UEC file</td>
    </tr>
    <tr>
        <td>crossBorder.tour.mc.uec.file</td>
        <td>Crossborder model tour mode choice UEC file</td>
    </tr>
    <tr>
        <td>visualizer.reference.path</td>
        <td>Path to reference scenario for SANDAG ABM visualizer</td>
    </tr>
</table>

<a id="mobility_mgra"></a>

### Zone Terminal Time
#### `ZONE.txt`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Zone</td>
        <td>TAZ number</td>
    </tr>
    <tr>
        <td>Terminal time</td>
        <td>Terminal time (3, 4, 5, 7, 10 minutes)<td>
    </tr>
</table>

<a href="#top">Go To Top</a>
