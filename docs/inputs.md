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
| [activity_code_indcen_acs.csv](#activity_mapping) | PECAS activity code categories mapping to Census industry codes; This is used for military occupation mapping. | CSV | Land Use Modelers | 
| [pecas_occ_occsoc_acs.csv](#pecas_occ) | PECAS activity code categories mapping to Census industry codes | CSV | Lande Use Modelers | 
| [mobilityHubMGRA.csv](#mobility_mgra) |  | CSV | Transportation Modelers | 
| **Synthetic Population** |  |  |  | 
| [households.csv](#population_synth_households) | Synthetic households | CSV | Transportation Modelers | 
| [persons.csv](#population_synth_persons) | Synthetic persons | CSV | Transportation Modelers | 
| **Network: Highway (to be updated with TNED)** |  |  |  | 
| hwycov.e00 | Highway network nodes from GIS | ESRI input exchange | Transportation Modelers | 
| hwycov.e00 | Highway network links from GIS | ESRI input exchange | Transportation Modelers | 
| turns.csv | Highway network turns file | CSV | Transportation Modelers | 
| LINKTYPETURNS.dbf | Highway network link type turns table | DBF | Transportation Modelers | 
| LINKTYPETURNSCST.DBF |  | DBF | Transportation Modelers | 
| [vehicle_class_toll_factors.csv](#vehicle_class_toll) | Relative toll values by six vehicle classes by Facility name. Used to identify "free for HOV" type managed lane facilities. | CSV | Transportation Modelers | 
| [off_peak_toll_factors.csv](#off_peak_toll) | Relative toll values for the three off-peak times-of-day (EA, MD, EV) by Facility name. Multiplied together with the values from vehicle_class_toll_factors.csv to get the final toll. | CSV | Transportation Modelers | 
| [vehicle_class_availability.csv](#hwy_link_vehicle_class_availability) | The availability / unavailability of six vehicle classes for five times-of-day by facility name. | CSV | Transportation Modelers | 
| **Network: Transit (To be updated with TNED)** |  |  |  | 
| trcov.e00 | Transit network arc data from GIS | ESRI input exchange | Transportation Modelers | 
| trcov.e00 | Transit network node data from GIS | ESRI input exchange | Transportation Modelers | 
| [trlink.csv](#tr_link) | Transit route with a list of links file | CSV | Transportation Modelers | 
| trrt.csv | Transit route attribute file | CSV | Transportation Modelers | 
| [trstop.csv](#transit_binary_stop) | Transit stop attribute file | TCSV | Transportation Modelers | 
| mode5tod.csv | Transit mode parameters table | CSV | Transportation Modelers |
| [timexfer_XX.csv](#transit_transfer_proh) | Transit timed transfers table between COASTER and feeder buses; XX is the TOD (EA, AM, MD, PM, and EV) | CSV | Transportation Modelers | 
| special_fares.txt | Fares to coaster | Text File | Transportation Modelers | 
| **Network: Active Transportation** |  |  |  | 
| [SANDAG_Bike_Net.dbf](#bike_net_link) | Bike network links | DBF | GIS | 
| [SANDAG_Bike_Node.dbf](#bike_net_node) | Bike network nodes | DBF | GIS | 
| [bikeTazLogsum.csv](#bike_taz_logsum) <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike TAZ logsum | CSV | Transportation Modelers | 
| [bikeMgraLogsum.csv](#bike_mgra_logsum) <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike MGRA logsum | CSV | Transportation Modelers | 
| [walkMgraEquivMinutes.csv](#walk_mgra_equiv) <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Walk, in minutes, between MGRAs | CSV |  | |  |  |  |
| **Visitor Model (Derived from visitor survey)** |  |  |  | 
| visitor_businessFrequency.csv | Visitor model tour frequency distribution for business travelers | CSV | Transportation Modelers | 
| visitor_personalFrequency.csv | Visitor model tour frequency distribution for personal travelers | CSV | Transportation Modelers | 
| visitor_partySize.csv | Visitor model party size distribution | CSV | Transportation Modelers | 
| visitor_autoAvailable.csv | Visitor model auto availability distribution | CSV | Transportation Modelers | 
| visitor_income.csv | Visitor model income distribution | CSV | Transportation Modelers | 
| visitor_tourTOD.csv | Visitor model tour time-of-day distribution | CSV | Transportation Modelers | 
| visitor_stopFrequency.csv | Visitor model stop frequency distribution | CSV | Transportation Modelers | 
| visitor_stopPurpose.csv | Visitor model stop purpose distribution | CSV | Transportation Modelers | 
| visitor_outboundStopDuration.csv | Visitor model time-of-day offsets for outbound stops | CSV | Transportation Modelers | 
| visitor_inboundStopDuration.csv | Visitor model time-of-day offsets for inbound stops | CSV | Transportation Modelers | 
| **Airport Model (Derived from airport survey)** |  |  |  | 
| [airport_purpose.csv](#airport_trip_purpose) | Airport model tour purpose frequency table | CSV | Transportation Modelers | 
| [airport_party.csv](#airport_party_purpose) | Airport model party type frequency table | CSV | Transportation Modelers | 
| [airport_nights.csv](#airport_nights) | Airport model trip duration frequency table | CSV | Transportation Modelers | 
| [airport_income.csv](#airport_income) | Airport model trip income distribution table | CSV | Transportation Modelers | 
| [airport_departure.csv](#airport_departure) | Airport model time-of-day distribution for departing trips | CSV | Transportation Modelers | 
| [airport_arrival.csv](#airport_arrival) | Airport model time-of-day distribution for arriving trips | CSV | Transportation Modelers | 
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
| **External Models (Derived from SCAG survey)** |  |  |  | 
| [externalExternalTripsByYear.csv](#external_trip) <i> (raw inputs have these by year) <i> | External origin-destination station trip matrix | CSV | Transportation Modelers | |  |  |  | 
| [externalInternalControlTotalsByYear.csv](#external_internal) <i> (raw inputs have these by year) <i> | External Internal station control totals read by GISDK | CSV | Transportation Modelers | |  |  |  | 
| [internalExternal_tourTOD.csv](#internal_external_tod) | Internal-External Model tour time-of-day frequency distribution | CSV | Transportation Modelers | 
| **Commercial Vehicle Model** (TO BE UPDATED) |  |  |  | 
| tazcentroids_cvm.csv | Zone centroid coordinates in state plane feet and albers | CSV | Transportation Modelers | 
| commVehFF.csv | Commercial Vehicle Model friction factors | CSV | Transportation Modelers | 
| OE.csv | Commercial vehicle model parameters file for off-peak early (OE) period | CSV | Transportation Modelers | 
| AM.csv | Commercial vehicle model parameters file for AM period | CSV | Transportation Modelers | 
| MD.csv | Commercial vehicle model parameters file for mid-day (MD) period | CSV | Transportation Modelers | 
| PM.csv | Commercial vehicle model parameters file for PM period | CSV | Transportation Modelers | 
| OL.csv | Commercial vehicle model parameters file for off-peak late (OL) period | CSV | Transportation Modelers | 
| FA.csv | Commercial vehicle model parameters file for fleet allocator (FA) industry | CSV | Transportation Modelers | 
| GO.csv | Commercial vehicle model parameters file for government/ office (GO) industry | CSV | Transportation Modelers | 
| IN.csv | Commercial vehicle model parameters file for industrial (IN) industry | CSV | Transportation Modelers | 
| FA.csv | Commercial vehicle model parameters file for fleet allocator (FA) industry | CSV | Transportation Modelers | 
| RE.csv | Commercial vehicle model parameters file for retail (RE) industry | CSV | Transportation Modeler | 
| SV.csv | Commercial vehicle model parameters file for service (SV) industry | CSV | Transportation Modelers | 
| TH.csv | Commercial vehicle model parameters file transport and handling (TH) industry | CSV | Transportation Modelers | 
| WH.csv | Commercial vehicle model parameters file wholesale (WH) industry | CSV | Transportation Modelers | 
| **Truck Model** |  |  |  | 
| TruckTripRates.csv | Truck model data: Truck trip rates | CSV | Transportation Modelers | 
| regionalEItrips<year>.csv | Truck model data: Truck external to internal data | CSV | Transportation Modelers | 
| regionalIEtrips<year>.csv | Truck model data: Truck internal to external data | CSV | Transportation Modelers | 
| regionalEEtrips<year>.csv | Truck model data: Truck external to external data | CSV | Transportation Modelers | 
| specialGenerators.csv | Truck model data: Truck special generator data | CSV | Transportation Modelers | 
| **Other** |  |  |  | 
| [parametersByYears.csv](#parametersbyyearscsv) | Parameters by scenario years. Includes AOC, aiport enplanements, cross-border tours, cross-border sentri share. | CSV | Transportation Modelers | 
| [filesByYears.csv](#filesbyyearscsv) | File names by scenario years. | CSV | Transportation Modelers | 
| trip_XX.omx | Warm start trip table; XX is the TOD (EA, AM, MD, PM, and EV) | OMX | Transportation Modelers |
| zone.term | TAZ terminal times | Space Delimited Text File | Transportation Modelers | 

```MGRA_BASED_INPUT<<SCENARIO_YEAR>>.CSV```<a name="lu"></a>

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
| pseudomsa | Pseudo MSA - | 
|  | 1: Downtown | 
|  | 2: Central | 
|  | 3: North City | 
|  | 4: South Suburban | 
|  | 5: East Suburban | 
|  | 6: North County West | 
|  | 7: North County East | 
|  | 8: East County | 
| zip09 | 2009 Zip Code | 
| enrollgradekto8 | Grade School K-8 enrollment | 
| enrollgrade9to12 | Grade School 9-12 enrollment | 
| collegeenroll | Major College enrollment | 
| othercollegeenroll | Other College enrollment | 
| hotelroomtotal | Total number of hotel rooms | 
| parkactive | Acres of Active Park | 
| openspaceparkpreserve | Acres of Open Park or Preserve | 
| beachactive | Acres of Active Beach | 
| district27 |  | 
| milestocoast | Distance (miles) to the nearest coast | 
| acres | Total acres in the mgra (used in CTM) | 
| land_acres | Acres of land in the mgra (used in CTM) | 
| effective_acres | Effective acres in the mgra (used in CTM) | 
| truckregiontype |  | 
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

<a id="activity_mapping"></a>

### Activity Mapping to Industry Codes
#### `ACTIVITY_CODE_INDCEN_ACS.CSV`

| Column Name | Description |
| ----------- | ----------- |
| indcen | Industry code defined in PECAS: They are about 270 industry categories grouped by 6-digit NAICS code (North American Industrial Classification System) |
| activity_code | Activity code defined in PECAS. They are about 30 types of activities grouped by the industry categories:<br>1 = Agriculture<br>3 = Construction Non-Building office support (including mining)<br>5 = Utilities office support<br>9 = Manufacturing office support<br>10 = Wholesale and Warehousing<br>11 = Transportation Activity<br>12 = Retail Activity<br>13 = Professional and Business Services<br>14 = Professional and Business Services (Building Maintenance)<br>16 = Private Education Post-Secondary (Post K-12) and Other<br>17 = Health Services<br>18 = Personal Services Office Based<br>19 = Amusement Services<br>20 = Hotels and Motels<br>21 = Restaurants and Bars<br>22 = Personal Services Retail Based<br>23 = Religious Activity<br>24 = Private Households<br>25 = State and Local Government Enterprises Activity<br>27 = Federal Non-Military Activity<br>28 = Federal Military Activity<br>30 = State and Local Government Non-Education Activity office support<br>31 = Public Education |


<a id="pecas_occ"></a>

### PECAS SOC - Defined Occupational Codes
#### `PECAS_OCC_OCCSOC_ACS.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>occsoc5</td>
        <td>Detailed occupation codes defined by the <a href="https://www.bls.gov/soc/">Standard Occupational Classification (SOC)</a> system</td>
    </tr>
    <tr>
        <td>commodity_id</td>
        <td>
            Commodity code defined in PECAS. The detailed SOC occupations are grouped into 6 types of laborers, which are included as part of commodity: <br>
            51 = Services Labor<br>
            52 = Work at Home Labor<br>
            53 = Sales and Office Labor<br>
            54 = Natural Resources Construction and Maintenance Labor<br>
            55 = Production Transportation and Material Moving Labor<br>
            56 = Military Labor
        </td>
    </tr>
</table>

<a id="external_zones"></a>

### Listing of External Zones Attributes
#### `EXTERNALZONES.XLS`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Internal Cordon LUZ</td>
        <td>Internal Cordon Land use zone</td>
    </tr>
    <tr>
        <td>External LUZ</td>
        <td>External land use zone</td>
    </tr>
    <tr>
        <td>Cordon Point</td>
        <td>Cordon Point description</td>
    </tr>
    <tr>
        <td>Destination Approximation</td>
        <td>Name of approximate city destination</td>
    </tr>
    <tr>
        <td>Miles to be Added to Cordon Point</td>
        <td>Miles to be added to cordon point</td>
    </tr>
    <tr>
        <td>Travel Time</td>
        <td>Travel time to external zone</td>
    </tr>
    <tr>
        <td>Border Delay</td>
        <td>Border delay time</td>
    </tr>
    <tr>
        <td>Minutes to be Added to Cordon Point</td>
        <td>Minutes to be added to cordon point</td>
    </tr>
    <tr>
        <td>MPH</td>
        <td>Average miles per hour based on miles and minutes to be added to cordon point</td>
    </tr>
</table>

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
| military             | Military status of person:<br>0 = N/A Less than 17 Years Old<br>1 = Yes, Now on Active Duty    |
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

The toll values for each class on each link are calculated by multiplying the input toll value from hwycov.e00 (ITOLLA, ITOLLP, ITOLLO) by this factor, matched by the Facility name (together with the toll factors from <a href="#hwy_link_off_peak_toll_factors">off_peak_toll_factors.csv</a> in converting ITOLLO to the off-peak times-of-day). 

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

<a id="off_peak_toll"></a>

### Highway Network Off-Peak Toll Factors File
#### `off_peak_toll_factors.csv`

Optional file. Used to specify different tolls in the off-peak time-of-day scenarios based on the single link ITOLLO field, together with the tolls by vehicle class from <a href="#hwy_link_vehicle_class_toll_factors">vehicle_class_toll_factors.csv</a>.
Used by the Import network Modeller tool.

Example:
```
Facility_name, OP_EA_factor, OP_MD_factor, OP_EV_factor
I-15,                  0.75,          1.0,         0.75
SR-125,                1.0 ,          1.0,         1.0
SR-52,                 0.8 ,          1.0,         0.8
```

See note re: network link matching under <a href="#hwy_link_vehicle_class_toll_factors">vehicle_class_toll_factors.csv</a>. Note that all facilities need not be specified, links not matched will use a factor of 1.0.

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
        <td>OP_EA_FACTOR</td>
        <td>Positive toll factor for Early AM period tolls</td>
    </tr>
    <tr>
        <td>OP_MD_FACTOR</td>
        <td>Positive toll factor for Midday period tolls</td>
    </tr>
    <tr>
        <td>OP_EV_FACTOR</td>
        <td>Positive toll factor for Evening period tolls</td>
    </tr>
</table>

<a id="hwy_link_vehicle_class_availability"></a>

### Highway Network Vehicle Class Toll Factors File
#### `vehicle_class_availability.csv`

Optional file. Specifies the availability / unavailability of six vehicle classes for five times-of-day by Facility name. This will override any mode / vehicle class availability specified directly on the network (hwycov.e00), via ITRUCK and IHOV fields. Used in the generation of time-of-day Emme scenarios in the Master run Modeller tool.

Example:

| Facility_name | vehicle_class | EA_Avail | AM_Avail | MD_Avail | PM_Avail | EV_Avail |
| ------------- | ------------- | -------- | -------- | -------- | -------- | -------- |
| I-15          | DA            | 1        | 1        | 1        | 1        | 1        |
| I-15          | S2            | 1        | 1        | 1        | 1        | 1        |
| I-15          | S3            | 1        | 0        | 1        | 0        | 1        |
| I-15          | TRK_L         | 1        | 1        | 1        | 1        | 1        |
| I-15          | TRK_M         | 1        | 0        | 0        | 0        | 1        |
| I-15          | TRK_H         | 1        | 0        | 0        | 0        | 1        |

See note re: network link matching under <a href="#hwy_link_vehicle_class_toll_factors">vehicle_class_toll_factors.csv</a>. Note that all facilities need not be specified, links not matched will use the availability as indicated by the link fields in hwycov.e00.

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
        <td>Vehicle_class</td>
        <td>Name of the vehicle class, one of DA, S2, S3, TRK_L, TRK_M, or TRK_H</td>
    </tr>
    <tr>
        <td>EA_Avail</td>
        <td>For this facility and vehicle class, is available for Early AM period (0 or 1)</td>
    </tr>
    <tr>
        <td>AM_Avail</td>
        <td>For this facility and vehicle class, is available for AM Peak period (0 or 1)</td>
    </tr>
    <tr>
        <td>MD_Avail</td>
        <td>For this facility and vehicle class, is available for Midday period (0 or 1)</td>
    </tr>
    <tr>
        <td>PM_Avail</td>
        <td>For this facility and vehicle class, is available for PM Peak period (0 or 1)</td>
    </tr>
    <tr>
        <td>EV_Avail</td>
        <td>For this facility and vehicle class, is available for Evening period (0 or 1)</td>
    </tr>
</table>

#### `special_fares.txt`

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

### Zone Terminal Time
#### `ZONE.TERM`

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

<a id="walk_mgra_equiv"></a>

### Walk MGRA Equivalent Minutes
#### `WALKMGRAEQUIVMINUTES.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>i</td>
        <td>Origin (MGRA)</td>
    </tr>
    <tr>
        <td>j</td>
        <td>Destination (MGRA)</td>
    </tr>
    <tr>
        <td>percieved</td>
        <td>Percieved time to walk</td>
    </tr>
    <tr>
        <td>actual</td>
        <td>Actual time to walk (minutes)</td>
    </tr>
    <tr>
        <td>gain</td>
        <td>Gain in elevation</td>
    </tr>
</table>

<a id="airport_trip_purpose"></a>

### Airport Trip Purpose Distribution
#### `AIRPORT_PURPOSE.SAN.CSV AND AIRPORT_PURPOSE.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Purpose</td>
        <td>
            Trip Purpose:<br>
            0 = Resident Business<br>
            1 = Resident Personal<br>
            2 = Visitor Business<br>
            3 = Visitor Personal<br>
            4 = External
        </td>
    </tr>
    <tr>
        <td>Percent</td>
        <td>Distribution of Trips in trip purpose</td>
    </tr>
</table>

<a id="airport_party_purpose"></a>

### Airport Party Size by Purpose Distribution
#### `AIRPORT_PARTY.SAN.CSV AND AIRPORT_PARTY.CBX.CSV`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>Party</td>
        <td>Party size (0 through 5+)</td>
    </tr>
    <tr>
        <td>purp0_perc</td>
        <td>Distribution for Resident Business purpose</td>
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

#### `EXTERNALEXTERNALTRIPSByYEAR.CSV`

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

<a href="#top">Go To Top</a>
