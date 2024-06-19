# Inputs and Outputs

The main inputs to ABM3 include the transportation network, land-use data, synthetic population data, parameters files, and model specifications. Outputs include a set of files that describe travel decisions made by all travel markets considered by the model (residents, overnight visitors, airport ground access trips, commercial vehicles and trucks, Mexico residents traveling in San Diego County, and travel made by all other non-residents into and through San Diego County.

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
| [mgra_based_input<<SCENARIO_YEAR>>.csv](#lu) | Land use forecast of the size and structure of the region’s economy and corresponding demographic forecast | CSV | Land Use Modelers, Transportation Modelers, and GIS | 
| activity_code_indcen_acs.csv | PECAS activity code categories mapping to Census industry codes; This is used for military occupation mapping. | CSV | Land Use Modelers | 
| pecas_occ_occsoc_acs.csv | PECAS activity code categories mapping to Census industry codes | CSV | Lande Use Modelers | 
| mobilityHubMGRA.csv |  | CSV | Transportation Modelers | 
| **Synthetic Population** |  |  |  | 
| households.csv | Synthetic households | CSV | Transportation Modelers | 
| persons.csv | Synthetic persons | CSV | Transportation Modelers | 
| **Network: Highway (to be updated with TNED)** |  |  |  | 
| hwycov.e00 | Highway network nodes from GIS | ESRI input exchange | Transportation Modelers | 
| hwycov.e00 | Highway network links from GIS | ESRI input exchange | Transportation Modelers | 
| turns.csv | Highway network turns file | CSV | Transportation Modelers | 
| LINKTYPETURNS.dbf | Highway network link type turns table | DBF | Transportation Modelers | 
| LINKTYPETURNSCST.DBF |  | DBF | Transportation Modelers | 
| vehicle_class_toll_factors.csv | Relative toll values by six vehicle classes by Facility name. Used to identify "free for HOV" type managed lane facilities. | CSV | Transportation Modelers | 
| off_peak_toll_factors.csv | Relative toll values for the three off-peak times-of-day (EA, MD, EV) by Facility name. Multiplied together with the values from vehicle_class_toll_factors.csv to get the final toll. | CSV | Transportation Modelers | 
| vehicle_class_availability.csv | The availability / unavailability of six vehicle classes for five times-of-day by facility name. | CSV | Transportation Modelers | 
| **Network: Transit (To be updated with TNED)** |  |  |  | 
| trcov.e00 | Transit network arc data from GIS | ESRI input exchange | Transportation Modelers | 
| trcov.e00 | Transit network node data from GIS | ESRI input exchange | Transportation Modelers | 
| trlink.csv | Transit route with a list of links file | CSV | Transportation Modelers | 
| trrt.csv | Transit route attribute file | CSV | Transportation Modelers | 
| trstop.csv | Transit stop attribute file | TCSV | Transportation Modelers | 
| mode5tod.csv | Transit mode parameters table | CSV | Transportation Modelers |
| timexfer_XX.csv | Transit timed transfers table between COASTER and feeder buses; XX is the TOD (EA, AM, MD, PM, and EV) | CSV | Transportation Modelers | 
| special_fares.txt | Fares to coaster | Text File | Transportation Modelers | 
| **Network: Active Transportation** |  |  |  | 
| SANDAG_Bike_Net.dbf | Bike network links | DBF | GIS | 
| SANDAG_Bike_Node.dbf | Bike network nodes | DBF | GIS | 
| bikeTazLogsum.csv <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike TAZ logsum | CSV | Transportation Modelers | 
| bikeMgraLogsum.csv <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Bike MGRA logsum | CSV | Transportation Modelers | 
| walkMgraEquivMinutes.csv <i>(not saved in inputs, instead, run at the beginning of a model run)<i> | Walk, in minutes, between MGRAs | CSV |  | |  |  |  |
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
| airport_purpose.csv | Airport model tour purpose frequency table | CSV | Transportation Modelers | 
| airport_party.csv | Airport model party type frequency table | CSV | Transportation Modelers | 
| airport_nights.csv | Airport model trip duration frequency table | CSV | Transportation Modelers | 
| airport_income.csv | Airport model trip income distribution table | CSV | Transportation Modelers | 
| airport_departure.csv | Airport model time-of-day distribution for departing trips | CSV | Transportation Modelers | 
| airport_arrival.csv | Airport model time-of-day distribution for arriving trips | CSV | Transportation Modelers | 
| **Cross-Border Model (Derived from cross-border survey)** |  |  |  | 
| crossBorder_tourPurpose_control.csv |  | CSV |  | 
| crossBorder_tourPurpose_nonSENTRI.csv | Cross Border Model tour purpose distribution for Non-SENTRI tours | CSV | Transportation Modelers | 
| crossBorder_tourPurpose_SENTRI.csv | Cross Border Model tour purpose distribution for SENTRI tours | CSV | Transportation Modelers | 
| crossBorder_tourEntryAndReturn.csv | Cross Border Model tour entry and return time-of-day distribution | CSV | Transportation Modelers | 
| crossBorder_supercolonia.csv | Cross Border Model distance from Colonias to border crossing locations | CSV | Transportation Modelers | 
| crossBorder_pointOfEntryWaitTime.csv | Cross Border Model wait times at border crossing locations table | CSV | GIS - Pat L vtsql | 
| crossBorder_stopFrequency.csv | Cross Border Model stop frequency data | CSV | Transportation Modelers | 
| crossBorder_stopPurpose.csv | Cross Border Model stop purpose distribution | CSV | Transportation Modelers | 
| crossBorder_outboundStopDuration.csv | Cross Border Model time-of-day offsets for outbound stops | CSV | Transportation Modelers | 
| crossBorder_inboundStopDuration.csv | Cross Border Model time-of-day offsets for inbound stops | CSV | Transportation Modelers | 
| **External Models (Derived from SCAG survey)** |  |  |  | 
| externalExternalTripsByYear.csv <i> (raw inputs have these by year) <i> | External origin-destination station trip matrix | CSV | Transportation Modelers | |  |  |  | 
| externalInternalControlTotalsByYear.csv <i> (raw inputs have these by year) <i> | External Internal station control totals read by GISDK | CSV | Transportation Modelers | |  |  |  | 
| internalExternal_tourTOD.csv | Internal-External Model tour time-of-day frequency distribution | CSV | Transportation Modelers | 
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
| parametersByYears.csv | Parameters by scenario years. Includes AOC, aiport enplanements, cross-border tours, cross-border sentri share. | CSV | Transportation Modelers | 
| filesByYears.csv | File names by scenario years. | CSV | Transportation Modelers | 
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
### `ACTIVITY_CODE_INDCEN_ACS.CSV`

| Column Name | Description |
| ----------- | ----------- |
| indcen | Industry code defined in PECAS: They are about 270 industry categories grouped by 6-digit NAICS code (North American Industrial Classification System) |
| activity_code | Activity code defined in PECAS. They are about 30 types of activities grouped by the industry categories:<br>1 = Agriculture<br>3 = Construction Non-Building office support (including mining)<br>5 = Utilities office support<br>9 = Manufacturing office support<br>10 = Wholesale and Warehousing<br>11 = Transportation Activity<br>12 = Retail Activity<br>13 = Professional and Business Services<br>14 = Professional and Business Services (Building Maintenance)<br>16 = Private Education Post-Secondary (Post K-12) and Other<br>17 = Health Services<br>18 = Personal Services Office Based<br>19 = Amusement Services<br>20 = Hotels and Motels<br>21 = Restaurants and Bars<br>22 = Personal Services Retail Based<br>23 = Religious Activity<br>24 = Private Households<br>25 = State and Local Government Enterprises Activity<br>27 = Federal Non-Military Activity<br>28 = Federal Military Activity<br>30 = State and Local Government Non-Education Activity office support<br>31 = Public Education |

<a href="#top">Go To Top</a>

<a id="pecas_occ"></a>
### PECAS SOC-Defined Occupational Codes
### `PECAS_OCC_OCCSOC_ACS.CSV`

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

<a href="#top">Go To Top</a>

<a id="external_zones"></a>
### Listing of External Zones Attributes
### `EXTERNALZONES.XLS`

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

<a href="#top">Go To Top</a>

<a id="population_synth_households"></a>
### Population Synthesizer Household Data
### `HOUSEHOLDS.CSV`

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

[Go To Top](#top)


<a id="population_synth_persons"></a>
### POPULATION SYNTHESIZER PERSON DATA
### `PERSONS.CSV`

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


<a href="#top">Go To Top</a>

<a id="esri_hwy_node"></a>

### Highway Network Vehicle Class Toll Factors File
### `vehicle_class_toll_factors.csv`

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

<a href="#top">Go To Top</a>

### Highway Network Off-Peak Toll Factors File
### `off_peak_toll_factors.csv`

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

<a href="#top">Go To Top</a>

<a id="hwy_link_vehicle_class_availability"></a>
### Highway Network Vehicle Class Toll Factors File
### `vehicle_class_availability.csv`

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

<a href="#top">Go To Top</a>

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

<a href="#top">Go To Top</a>

<a id="transit_binary_stop"></a>
### Transit Binary Stop Table
### `TRSTOP.CSV`

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
            Premium Transit mode?<br>
            0 = No<br>
            1 = Yes
        </td>
    </tr>
    <tr>
        <td>EXPBSMODE</td>
        <td>
            Express bus mode?<br>
            0 = No<br>
            1 = Yes
        </td>
    </tr>
    <tr>
        <td>LOCMODE</td>
        <td>
            Local bus mode?<br>
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

<a href="#top">Go To Top</a>

<a id="transit_transfer_proh"></a>

### Transit Timed Transfers Between COASTER and Feeder Buses
### `TIMEXFER_XX.CSV`

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

<a href="#top">Go To Top</a>

<a id="transit_fares"></a>

### Transit Stop Table
### `TRSTOP.CSV`
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
| PREMODE      | Premium Transit mode?<br>0 = No<br>1 = Yes |
| EXPBSMODE    | Express bus mode?<br>0 = No<br>1 = Yes |
| LOCMODE      | Local bus mode?<br>0 = No<br>1 = Yes |
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

<a href="#top">Go To Top</a>

### Transit Link File
### `TRLINK.CSV`

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

<a href="#top">Go To Top</a>


<a id="bike_net_link"></a>
### Bike Network Link Field List
### `SANDAG_BIKE_NET.DBF`

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

<a href="#top">Go To Top</a>

<a id="bike_net_node"></a>
### Bike Network Node Field List
### `SANDAG_BIKE_NODE.DBF`
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

<a href="#top">Go To Top</a>

### Zone Terminal Time
### `ZONE.TERM`

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

<a id="bike_taz_logsum"></a>
### Bike TAZ Logsum
### `BIKETAZLOGSUM.CSV`

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

<a href="#top">Go To Top</a>

<a id="bike_mgra_logsum"></a>
### Bike MGRA Logsum
### `BIKEMGRALOGSUM.CSV`

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

<a href="#top">Go To Top</a>

<a id="walk_mgra_equiv"></a>
### Walk MGRA Equivalent Minutes
### `WALKMGRAEQUIVMINUTES.CSV`

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

<a href="#top">Go To Top</a>

### Airport Trip Purpose Distribution
### `AIRPORT_PURPOSE.SAN.CSV AND AIRPORT_PURPOSE.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="airport_party_purpose"></a>
### Airport Party Size by Purpose Distribution
### `AIRPORT_PARTY.SAN.CSV AND AIRPORT_PARTY.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="airport_nights"></a>
### Airport Number of Nights by Purpose Distribution
### `AIRPORT_NIGHTS.SAN.CSV AND AIRPORT_NIGHTS.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="airport_income"></a>
### Airport Income by Purpose Distribution
### `AIRPORT_INCOME.SAN.CSV AND AIRPORT_INCOME.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="airport_departure"></a>
### Airport Departure Time by Purpose Distribution
### `AIRPORT_DEPARTURE.SAN.CSV` and `AIRPORT_DEPARTURE.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="airport_arrival"></a>
### Airport Arrival Time by Purpose Distribution
### `AIRPORT_ARRIVAL.SAN.CSV` and `AIRPORT_ARRIVAL.CBX.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_entry_return"></a>
### Cross Border Model Tour Entry and Return Distribution
### `CROSSBORDER_TOURENTRYANDRETURN.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_supercolonia"></a>
### Cross Border Model Supercolonia
### `CROSSBORDER_SUPERCOLONIA.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_wait_time"></a>
### Cross Border Model Point of Entry Wait Time
### `CROSSBORDER_POINTOFENTRYWAITTIME.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_stops"></a>
### Cross Border Model Stop Frequency
### `CROSSBORDER_STOPFREQUENCY.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_stop_purpose"></a>
### Cross Border Model Stop Purpose Distribution
### `CROSSBORDER_STOPPURPOSE.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_out_stop"></a>
### Cross Border Model Outbound Stop Duration Distribution
### `CROSSBORDER_OUTBOUNDSTOPDURATION.CSV`

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

<a href="#top">Go To Top</a>

<a id="cross_border_in_stop"></a>
### Cross Border Model Inbound Stop Duration Distribution
### `CROSSBORDER_INBOUNDSTOPDURATION.CSV`

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

<a href="#top">Go To Top</a>

<a id="external_trip"></a>

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

<a href="#top">Go To Top</a>

<a id="external_internal"></a>
### External Internal Control Totals
### `EXTERNALINTERNALCONTROLTOTALSByYEAR.CSV`

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

<a href="#top">Go To Top</a>

<a id="internal_external_tod"></a>
### Internal External Tours Time of Day Distribution
### `INTERNALEXTERNAL_TOURTOD.CSV`

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

<a href="#top">Go To Top</a>

### Parameters by Scenario Years
### `PARAMETERSBYYEARS.CSV`

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

<a href="#top">Go To Top</a>

### Files by Scenario Years
### `FILESBYYEARS.CSV`

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

<a href="#top">Go To Top</a>

### MGRAs at Mobility Hubs
### `MOBILITYHUBMGRA.CSV`

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

## Model Outputs

Model outputs are stored in the .\outputs directory. The contents of the directory are listed in the table below.

### Output Directory (.\output)

| **Directory\File Name** | **Description** | 
| --- | --- |
| airport.CBX (directory) | Outputs for Cross-Border Express Airport Ground Access Model | 
| airport.SAN (directory) | Outputs for San Diego International Airport Ground Access Model | 
| assignment (directory) | Assignment outputs | 
| crossborder (directory) | Crossborder Travel Model outputs | 
| cvm (directory) | Commercial Vehicle Model outputs | 
| parking (directory) | Parking model outputs | 
| resident (directory) | Resident model outputs | 
| skims (directory) | Skim outputs | 
| visitor (directory) | Visitor Model outputs | 
| bikeMgraLogsum.csv | Bike logsum file for close-together MGRAs | 
| bikeTazLogsum.csv | Bike logsum file for TAZs | 
| datalake_metadata.yaml | Metadata file for datalake reporting system | 
| derivedBikeEdges.csv | Derived bike network edge file | 
| derivedBikeNodes.csv | Derived bike network node file | 
| derivedBikeTraversals.csv | Derived bike network traversals file | 
| microMgraEquivMinutes.csv | Equivalent minutes for using micromobility between close-together MGRAs (not used) | 
| runtime_summary.csv | Summary of model runtime | 
| temp_tazdata_cvm.csv | TAZ data for commercial vehicle model | 
| transponderModelAccessibilities.csv | Transponder model accessibilities (not used) | 
| trip_(period).omx | Trips for each time period, for assignment | 
| walkMgraEquivMinutes.csv | Equivalent minutes for walking between close-together MGRAs |

### ActivitySim log files

ActivitySim writes out various log files when it runs; these have standard names for each model component. Therefore we list them separately, but copies of these files may be in each model’s output directory depending upon the settings used to run ActivitySim for that model component.

| **File ** | **Description** | 
| --- | --- |
| activitysim.log | ActivitySim log file for model | 
| breadcrumbs.yaml | Breadcrumbs provides a record of steps that have been run for use when resuming a model run | 
| final_checkpoints.csv | ActivitySim checkpoint file | 
| final_pipeline.h5 | ActivitySim pipeline file | 
| mem.csv | ActivitySim memory use log file | 
| mem_mp_households.csv | Memory logs for ActivitySim model steps running with the same num_processes (all except accessibility, initialize, and summarize) | 
| mem_mp_initialize.csv | Memory logs for ActivitySim model step initialize | 
| mem_mp_summarize.csv | Memory logs for ActivitySim model step summarize | 
| mp_households_(processnumber)-activitysim.log | ActivitySim log file for processnumber. This logfile is created if model is run in multiprocess mode | 
| mp_households_(processnumber)-mem.csv | Memory log file for processnumber | 
| mp_households_apportion-activitysim.log | ActivitySim log file for apportioning data between multiple processes | 
| mp_households_coalesce-activitysim.log | ActivitySIm logfile for coalesing output from multiple processes into one | 
| mp_initialize-activitysim.log | ActivitySim log file for the initialization steps | 
| mp_initialize-mem.csv | Memory logs for ActivitySim model step summarize (similar to mp_initialize-mem.csv) | 
| mp_setup_skims-activitysim.log | ActivitySim logfile for reading in skims | 
| mp_summarize-activitysim.log | ActivitySim log file for summarizing model output (omx and csv trip table) | 
| mp_summarize-mem.csv | Memory logs for ActivitySim model step summarize (similar to mem_mp_initialize.csv) | 
| mp_tasks_log.txt | Log files of multiprocessed steps | 
| omnibus_mem.csv | Memory log file of all model steps (similar to mem.csv) | 
| run_list.txt | List of models that have been run | 
| timing_log.csv | Model run time by steps | 

### Airport model outputs (.\airport.CBX, .\airport.SAN)

There are two subdirectories containing outputs for each of the two airport models. airport.CBX contains output for the Cross-Border Express model, and airport.SAN contains output for the San Diego International Airport model. Each directory has identical files so we provide one generic output table below.

| **Filename** |  | **Description** | 
| --- | --- | --- |
| final_(airport)accessibility.csv |  | Accessibility file for airport (cbx, san) (not used, created by default) | 
| [final_(airport)households.csv](#### Airport Model household file (final_(airport)households.csv)) |  | Household file for airport (cbx, san) | 
| final_(airport)land_use.csv |  | Land-use file for airport (cbx, san) | 
| [final_(airport)persons.csv](#### Airport Model person file (final_(airport)persons.csv)) |  | Persons file for airport (cbx, san) | 
| [final_(airport)tours.csv](#### Airport Model tour file (final_(airport)tours.csv)) |  | Tour file for airport (cbx, san) | 
| [final_(airport)trips.csv](#### Airport Model trip file (final_(airport)trips.csv)) |  | Trip file for airport (cbx, san) | 
| model_metadata.yaml |  | Datalake metadata file | 
| nmotairporttrips.(airport)_(period).omx |  | Non-motorized trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV) | 

#### Airport Model household file (final_(airport)households.csv)

| Field | Description | 
| --- | --- |
| home_zone_id | Airport MGRA | 
| sample_rate | Sample rate | 
| household_id | Household ID | 
| poverty | Poverty indicator utilized for social equity reports. Percentage value where value <= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty. | 

#### Airport Model person file (final_(airport)persons.csv)

| Field | Description | 
| --- | --- |
| household_id | Household ID | 
| person_id | Person ID | 

#### Airport Model tour file (final_(airport)tours.csv)

| Field | Description | 
| --- | --- |
| tour_id | Tour ID | 
| purpose_id | ? | 
| party_size | Number of persons in airport travel party | 
| nights | Number of nights away | 
| income | Income group 0-7,  -99 if employee | 
| direction | Direction of trip. String. outbound: airport to non-airport, inbound: non-airport to airport | 
| household_id | Household ID | 
| person_id | Person ID | 
| tour_category | Tour category. String "non_mandatory" | 
| tour_type | Type of tour. String. "Emp": Employee, "ext": External, "res_busn": Resident business where n is?, "res_pern": Resident personal where n is?, "vis_bus": Visitor business, "vis_per": Visitor personal | 
| origin | Origin MGRA | 
| destination | Destination MGRA | 
| number_of_participants | Same as party_size | 
| outbound | TRUE if outbound, else FALSE | 
| start | Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| end | Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| duration | Duration of the tour in number of half-hour periods, including all activity episodes and travel | 
| destination_logsum | Logsum from destination choice model? | 
| stop_frequency | out_0in, 0out_in | 
| primary_purpose | "busn", "emp", "extn", "pern" | 

#### Airport Model trip file (final_(airport)trips.csv)

| Field | Description | 
| --- | --- |
| trip_id | Trip ID | 
| person_id | Person ID | 
| household_id | Household ID | 
| primary_purpose | Primary purpose of trip: "busn": Business, where n is..., "emp": Employee, "extn": External, where n is..., "pern": Personal, where n is... | 
| trip_num | 1 | 
| outbound | TRUE if outbound, else FALSE | 
| trip_count | 1 | 
| destination | Destination MGRA | 
| origin | Origin MGRA | 
| tour_id | Tour ID | 
| depart | Departure time period (1…48) | 
| trip_mode | Trip mode (see trip mode table) | 
| mode_choice_logsum | Mode choice logsum for trip | 
| vot | Value of time in dollars per hour ($2023) | 
| arrival_mode | Arrival mode from airport trip mode choice model | 
| cost_parking | Cost of parking ($2023) | 
| cost_fare_drive | Ridehail/Taxi fare on a trip | 
| distance_walk | Distance walked on a trip (including access/egress for transit modes) | 
| time_mm | Micromobility time | 
| distance_mm | Micromobility distance | 
| cost_fare_mm | Micromobility cost ($2023) | 
| distance_bike | Bike distance | 
| time_wait_drive | Ridehail/Taxi wait times for a trip | 
| trip_period | A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”) | 
| party | Party size | 
| tour_participants | Number of joint tour participants if joint tour, else 1 | 
| distance_total | Trip distance | 
| add_driver | TRUE if trip requires a driver based on airport mode (for example, TNC, or pickup), else FALSE | 
| weight_trip | 1 | 
| weight_person_trip | weight_trip * tour_participants | 
| cost_operating_drive | Auto operating cost ($2023)? | 
| inbound | TRUE if trip is from (origin) airport to (destination) non-airport zone, else FALSE | 
| time_drive | Auto time | 
| distance_drive | Auto distance | 
| cost_toll_drive | Auto toll cost ($2023)? | 
| time_transit_in_vehicle | Transit in-vehicle time | 
| time_rapid_transit_in_vehicle | Rapid transit in-vehicle time | 
| time_express_bus_transit_in_vehicle | Express bus in-vehicle time | 
| time_local_bus_transit_in_vehicle | Local bus in-vehicle time | 
| time_light_rail_transit_in_vehicle | Light rail transit in-vehicle time | 
| time_commuter_rail_transit_in_vehicle | Commuter rail in-vehicle time | 
| time_transit_initial_wait | Transit initial-wait time | 
| cost_fare_transit | Transit fare ($2023) | 
| transfers_transit | Number of transfers | 
| time_bike | Bike time | 
| time_walk | Walk mode time | 
| cost_total | Sum of all costs a trip might incur (auto operating, toll, transit fare) | 
| time_total | Total time (sum of ?) | 
| value_of_time_category_id | Value of time bin. 1: Low, 2: Medium, 3: High | 
| sample_rate | Sample rate | 
| otaz | Origin TAZ | 
| dtaz | Destination TAZ | 

### Assignment model trip tables (.\assignment)

This directory contains trip tables from auto and transit assignments.

| **File ** | **Description** | 
| --- | --- |
| autoairportTrips.(airport)_(period_(vot).omx | Auto trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV) and value of time (low, medium, high) | 
| autocrossborderTrips_(period)_(vot).omx | Auto trip table for cross border model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high) | 
| autoTrips_(period)_(vot).omx | Auto trip table for resident model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high) | 
| autovisitorTrips_(period)_(vot).omx | Auto trip table for visitor model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high) | 
| emptyAVTrips.omx | Empty private autonomous vehicle trips | 
| householdAVTrips.csv | All private autonomous vehicle trips | 
| TNCTrips.csv | All TNC trips | 
| TNCVehicleTrips_(period).omx | TNC vehicle trip table by period (EA, AM, MD, PM, EV) | 
| TranairportTrips.(airport)_(period).omx | Transit trip tables for airport (CBX, SAN) by period (EA, AM, MD, PM, EV) | 
| TrancrossborderTrips_(period).omx | Transit trip tables for cross-border model by period (EA, AM, MD, PM, EV) | 
| TranTrips_(period).omx | Transit trip tables for resident model by period (EA, AM, MD, PM, EV) | 
| TranvisitorTrips_(period).omx | Transit trip tables for visitor model by period (EA, AM, MD, PM, EV) | 
| TripMatrices.csv | Disaggregate commercial vehicle trips | 

### Crossborder model outputs (.\crossborder)

This directory contains outputs from the Crossborder model, which represents all travel made by Mexico residents in San Diego County.

| **File ** | **Description** | 
| --- | --- |
| final_accessibility.csv | Accessibility file for Crossborder Model (not used, created by default) | 
| [final_households.csv](#### Crossborder Model household file (final_households.csv)) | Household file for Crossborder Model | 
| final_land_use.csv | Land-use file for Crossborder Model | 
| [final_persons.csv](#### Crossborder Model person file (final_persons.csv)) | Persons file for Crossborder Model | 
| [final_tours.csv](#### Crossborder Model tour file (final_tours.csv)) | Tour file for Crossborder Model | 
| [final_trips.csv](#### Crossborder Model trip file (final_trips.csv)) | Tour file for Crossborder Model | 
| model_metadata.yaml | Model run meta data for use in Datalake storage and reporting | 
| nmCrossborderTrips_AM.omx | Non-motorized trip table for Crossborder Model by period (EA, AM, MD, PM, EV) | 
| othrCrossborderTrips_AM.omx | Other trip table for Crossborder Model by period (EA, AM, MD, PM, EV) | 

#### Crossborder Model household file (final_households.csv)

| Field | Description | 
| --- | --- |
| sample_rate | Sample Rate | 
| num_persons | Number of persons in travel party | 
| origin | Origin MGRA (Border crossing station) | 
| home_zone_id | Home MGRA (Border crossing station) | 
| household_id | Household ID | 
| poverty | Poverty indicator utilized for social equity reports. Percentage value where value <= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty. | 

#### Crossborder Model person file (final_persons.csv)

| Field | Description | 
| --- | --- |
| household_id | Household ID | 
| work_time_factor | Travel time sensitivity factor for work tours | 
| non_work_time_factor | Travel time sensitivity factor for non-work tours (Sampled in person preprocessor) | 
| origin | Origin MGRA (Border crossing station) | 
| home_zone_id | Home MGRA (Border crossing station) | 
| person_id | Person ID | 

#### Crossborder Model tour file (final_tours.csv)

| Field | Description | 
| --- | --- |
| tour_id | Tour ID | 
| pass_type | Type of border crossing pass. String. "no_pass": Does not own a pass, "sentri": SENTRI pass, or "ready": READY pass | 
| tour_type | Tour purpose. String. "other", "school", "shop", "visit", or "work" | 
| purpose_id | Tour purpose ID. 0: work, 1: school, 2: shop, 3: visit, 4: other | 
| tour_category | Tour category. String. Mandatory: Work or school, Non-Mandatory: Shop, visit, other | 
| number_of_participants | Number of participants in tour | 
| household_id | Household ID | 
| person_id | Person ID | 
| start | Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| end | Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| duration | Duration of the tour in number of half-hour periods, including all activity episodes and travel | 
| origin | Tour origin (Border Crossing) MGRA | 
| destination | Tour primary destination MGRA | 
| tour_od_logsum | Tour origin-crossing-destination logsum | 
| poe_id | Number of border crossing station | 
| tour_mode | Tour mode | 
| mode_choice_logsum | Tour mode choice logsum | 
| stop_frequency | Number of stops on tour by direction. String. xout_yin where x is number of stops in the outbound direction and y is the number of stops in the inbound direction | 
| primary_purpose | will drop | 

#### Crossborder Model trip file (final_trips.csv)

| Field | Description | 
| --- | --- |
| trip_id | Trip ID | 
| person_id | Person ID | 
| household_id | Household ID | 
| primary_purpose | Purpose at primary destination. String. "other", "school", "shop", "visit", or "work" | 
| trip_num | Sequential number of trip on half-tour from 1 to 4 | 
| outbound | TRUE if outbound, else FALSE | 
| trip_count | number of trips per tour. Will drop | 
| destination | Destination MGRA | 
| origin | Origin MGRA | 
| tour_id | Tour ID | 
| purpose | Purpose at trip destination. String. "other", "school", "shop", "visit", or "work" | 
| depart | Departure time period (1…48) | 
| trip_mode | Trip mode (see trip mode table) | 
| trip_mode_choice_logsum | Mode choice logsum for trip | 
| parking_cost | Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered. | 
| tnc_single_wait_time | Wait time for single pay TNC | 
| tnc_shared_wait_time | Wait time for shared\pooled TNC | 
| taxi_wait_time | Wait time for taxi | 
| cost_parking | Cost of parking ($2023) | 
| cost_fare_drive | ? | 
| distance_walk | Walk mode distance? | 
| time_mm | Micromobility time | 
| distance_mm | Micromobility distance | 
| cost_fare_mm | Micromobility cost ($2023) | 
| distance_bike | Bike distance | 
| time_wait_drive | ? | 
| trip_period | A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”) | 
| tour_participants | Number of joint tour participants if joint tour, else 1 | 
| distance_total | ? | 
| cost_operating_drive | Auto operating cost ($2023)? | 
| weight_trip | Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3) | 
| weight_person_trip | Person trip weight defined as the ratio of the number of participants on a trip to sample rate of the model run | 
| inbound | TRUE if trip is in outbound direction, else FALSE | 
| time_drive | Auto time | 
| distance_drive | Auto distance | 
| cost_toll_drive | Auto toll cost ($2023)? | 
| time_transit_in_vehicle | Transit in-vehicle time | 
| time_rapid_transit_in_vehicle | Rapid transit in-vehicle time | 
| time_express_bus_transit_in_vehicle | Express bus in-vehicle time | 
| time_local_bus_transit_in_vehicle | Local bus in-vehicle time | 
| time_light_rail_transit_in_vehicle | Light rail transit in-vehicle time | 
| time_commuter_rail_transit_in_vehicle | Commuter rail in-vehicle time | 
| time_transit_initial_wait | Transit initial-wait time | 
| cost_fare_transit | Transit fare ($2023) | 
| transfers_transit | Number of transfers | 
| time_bike | Bike time | 
| time_walk | Walk mode time | 
| cost_total | Sum of all costs a trip might incur (auto operating, toll, transit fare) | 
| time_total | Total time (sum of ?) | 
| value_of_time_category_id | Value of time bin. 1: Low, 2: Medium, 3: High | 
| origin_micro_prm_dist | Distance from trip origin MGRA to closest premium transit stop by microtransit | 
| dest_micro_prm_dist | Distance from trip destination MGRA to closest premium transit stop by microtransit | 
| microtransit_orig | Distance from trip origin MGRA to closest local transit stop by microtransit? | 
| microtransit_dest | Distance from trip destination MGRA to closest local transit stop by microtransit? | 
| microtransit_available | TRUE if microtransit is available for trip, else FALSE | 
| nev_orig | True if Neighborhood Electric Vehicle is available at origin | 
| nev_dest | True if Neighborhood Electric Vehicle is available at destination | 
| nev_available | TRUE if Neighborhood Electric Vehicle is available, else FALSE | 
| microtransit_access_available_out | TRUE if microtransit is available from the origin, else FALSE | 
| nev_access_available_out | TRUE if neighborhood electric vehicle is available from the origin, else FALSE | 
| microtransit_egress_available_out | Availability of microtransit egress in the outbound direction | 
| nev_egress_available_out | Availability of NEV egress in the outbound direction | 
| microtransit_access_available_in | Availability of microtransit access in the inbound direction | 
| nev_access_available_in | Availability of NEV egress in the inbound direction | 
| microtransit_egress_available_in | Availability of microtransit egress in the inbound direction | 
| nev_egress_available_in | Availability of microtransit egress in the outbound direction | 
| sample_rate | Sample rate | 
| otaz | Origin TAZ | 
| dtaz | Destination TAZ | 

### Commercial Vehicle Model (.\cvm)

//TODO
Update with CVM results once model is updated

### Parking cost calculations (.\parking)

This directory contains intermediate files and final expected parking costs calculated from input parking supply data and walk distances between MGRAs.

| **File ** | **Description** | 
| --- | --- |
| aggregated_street_data.csv | Street length and intersections aggregated to MGRA level, used to estimate free on-street parking spaces | 
| cache (directory) | Directory containing intermediate calculations for expected parking costs | 
| distances.csv | MGRA-MGRA distances used for expected parking cost calculations | 
| districts.csv | Calculated parking districts at MGRA level used for expected parking cost calculations | 
| final_parking_data.csv | Expected hourly, daily, and monthly parking costs, total spaces, and parking district at the MGRA level for use in travel models | 
| plots | Directory containing plots of the parking model results | 
| shapefiles | Directory containing shapefiles for parking model calculations | 

### Resident model outputs (.\resident)

This directory contains San Diego resident travel model outputs.

| File | Description | 
| --- | --- |
| cdap_joint_spec_(persons).csv | Model specification file for coordinated daily activity pattern model joint tour alternative for (persons)-way interaction terms | 
| cdap_spec_(persons).csv | Model specification file for coordinated daily activity pattern model for (persons)-way interaction terms. | 
| data_dict.csv | Data dictionary for resident model, csv format | 
| data_dict.txt | Data dictionary for resident model, text format | 
| final_accessibility.csv | Resident model aggregate accessibility file | 
| final_disaggregate_accessibility.csv | Resident model disaggregate accessibility file at MGRA level | 
| [final_households.csv](#### Resident Model household file (final_households.csv)) | Resident model household file | 
| [final_joint_tour_participants.csv](#### Resident Model joint tour participants file (final_joint_tour_participants.csv)) | Resident model joint tour participants file | 
| final_land_use.csv | Resident model land-use file | 
| [final_persons.csv](#### Resident Model vehicle file (final_vehicles.csv)) | Resident model persons file | 
| final_proto_disaggregate_accessibility.csv | Resident model disaggregate accessibility file at person level | 
| [final_tours.csv](#### Resident Model tour file (final_tours.csv)) | Resident model tour file | 
| [final_trips.csv](#### Resident Model trips file (final_trips.csv)) | Resident model trip file | 
| [final_vehicles.csv](#### Resident Model vehicle table (final_vehicles.csv)) | Resident model vehicle file | 
| log (directory) | Directory for resident model logging output | 
| model_metadata.yaml | Resident model Datalake metadata file | 
| nmottrips_period.omx | Resident model non-motorized trip tables by period (EA, AM, MD, PM, EV) | 
| skim_usage.txt | Skim usage file | 
| trace (directory) | Directory for resident model trace output | 

#### Resident Model household file (final_households.csv)

| **Field** | **Description** | 
| --- | --- |
| home_zone_id | Household MGRA - same as mgra | 
| income | Household income in dollars ($2023) | 
| hhsize | Number of persons in household | 
| HHT | Household dwelling unit type. 0: N/A (GQ/vacant), 1: Married couple household, 2: Other family household: Male householder no spouse present, 3: Other family household: Female householder no spouse present, 4: Nonfamily household: Male householder living alone, 5: Nonfamily household: Male householder: Not living alone, 6: Nonfamily household: Female householder: Living alone, 7: Nonfamily household: Female householder: Not living alone | 
| auto_ownership | (Model output) Auto ownership | 
| num_workers | Number of workers in household | 
| building_category | Units in structure. 0: N/A (GQ), 1: Mobile home or trailer, 2: One-family house detached, 3: One-family house attached, 4: 2 Apartments, 5: 3-4 Apartments, 6: 5-9 Apartments, 7: 10-19 Apartments, 8: 20-49 Apartments, 9: 50 or more apartments, 10: Boat, RV, van, etc. | 
| unittype | Household unit type. 0: Non-GQ Household, 1: GQ Household (used in Visualizer) | 
| sample_rate | Sample rate for household | 
| income_in_thousands | Household income in thousands of dollars ($2023) | 
| income_segment | Household income segment (1-4) | 
| num_non_workers | Number of non-workers in household | 
| num_drivers | Number of persons age 16+ | 
| num_adults | Number of persons age 18+ | 
| ebike_owner | TRUE if household owns an e-bike, else FALSE (output from e-bike owership simulation) | 
| av_ownership | TRUE if household owns an autonomous vehicle, else FALSE (output from AV Ownership Model) | 
| workplace_location_accessibility | Work location choice logsum (output from Disaggregate Accessibility Model) | 
| shopping_accessibility | Shopping primary destination choice logsum (output from Disaggregate Accessibility Model) | 
| othdiscr_accessibility | Other Discretionary primary destination choice logsum (output from Disaggregate Accessibility Model) | 
| numAVowned | Number of autonomous vehicles owned by household (output from Vehicle Type Choice Model) | 
| transponder_ownership | TRUE if household owns a transponder, else FALSE (output from Transponder Ownership Model) | 
| has_joint_tour | 1 if household has at least one fully joint tour, else false (output from Coordinated Daily Activity Pattern Model) | 
| num_under16_not_at_school | Number of persons age less than 16 who do not attend school (output from Coordinated Daily Activity Pattern Model) | 
| num_travel_active | Number of persons in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model) | 
| num_travel_active_adults | Number of adults in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model) | 
| num_travel_active_preschoolers | Number of preschool children in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model) | 
| num_travel_active_children | Number of children in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model) | 
| num_travel_active_non_preschoolers | Number of non-preschoolers household in who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model) | 
| participates_in_jtf_model | TRUE if household has a joint tour frequency model, else FALSE (output from Coordinated Daily Activity Pattern Model) | 
| school_escorting_outbound | Alternative number for school escort model in the outbound direction (initial output from School Escort Model) | 
| school_escorting_inbound | Alternative number for school escort model in the inbound direction (output from School Escort Model) | 
| school_escorting_outbound_cond | Alternative number for school escort model in the outbound direction (final output from School Escort Model) | 
| auPkRetail | Auto peak access to retail employment from household TAZ (aggregate accessibility output) | 
| auPkTotal | Auto peak access to total employment from household TAZ (aggregate accessibility output) | 
| auOpRetail | Auto offpeak access to retail employment from household TAZ (aggregate accessibility output) | 
| auOpTotal | Auto offpeak access to total employment from household TAZ (aggregate accessibility output) | 
| trPkRetail | Transit peak access to retail employment from household TAZ (aggregate accessibility output) | 
| trPkTotal | Transit peak access to total employment from household TAZ (aggregate accessibility output) | 
| trPkHH | Transit peak access to total employment from household (aggregate accessibility output) | 
| trOpRetail | Transit offpeak access to retail employment from household TAZ (aggregate accessibility output) | 
| trOpTotal | Transit offpeak access to total employment from household TAZ (aggregate accessibility output) | 
| nmRetail | Walk access to retail employment from household TAZ (aggregate accessibility output) | 
| nmTotal | Walk access to total employment from household TAZ (aggregate accessibility output) | 
| microtransit | Microtransit access time in household MGRA | 
| nev | Neighborhood electric vehicle access time in household MGRA | 
| mgra | Household MGRA - same as home_zone_id | 
| TAZ | Household TAZ | 
| micro_dist_local_bus | Distance to closest local bus stop from household MGRA by microtransit, if available. 999999 if not available. | 
| micro_dist_premium_transit | Distance to closest premium transit stop from household MGRA by microtransit, if available. 999999 if not available. | 
| joint_tour_frequency_composition | Joint tour frequency and composition model choice (output from Joint Tour Frequency\Composition Model) | 
| num_hh_joint_tours | Number of fully joint tours at the household level (0, 1 or 2) (output from Coordinated Daily Activity Pattern Model and Joint Tour Frequency\Composition Models) | 
| household_id | Household ID | 
| poverty | Poverty indicator utilized for social equity reports. Percentage value where value <= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty. | 

#### Resident Model person file (final_persons.csv)

| **Field** | **Description** | 
| --- | --- |
| household_id | Household ID | 
| age | Person age in years | 
| PNUM | Person number in household (1…n where n is number of persons in household) | 
| sex | 1: Male, 2: Female | 
| pemploy | Employment status of person. 1: Employed Full-Time, 2: Employed Part-Time, 3: Unemployed or Not in Labor Force, 4: Less than 16 Years Old | 
| pstudent | Student status of person. 1: Pre K-12, 2: College Undergrad+Grad and Prof. School, 3: Not Attending School | 
| ptype | Person type  1: Full-time worker 2: Part-time worker 3: College\University Student 4: Non-Working Adult 5: Retired 6: Driving-age student 7: Non-driving age student 8: Pre-school\Age <=5 | 
| educ | Educational attainment. 1: No schooling completed, 9: High school graduate, 13: Bacehlor's degree | 
| soc2 | Two-digit Standard Occupational Classification (SOC) codes (https://www.bls.gov/oes/current/oes_stru.htm) | 
| is_student | Person is a K12 or college student | 
| school_segment | School location choice model's segment a student belongs to (preschool, grade school, high school, university) | 
| is_worker | Person is a full-time or part-time worker | 
| is_internal_worker | TRUE if worker works internal to region, else FALSE (output from Internal-External Worker Identification Model) | 
| is_external_worker | TRUE if worker works external to region, else FALSE (output from Internal-External Worker Identification Model) | 
| home_zone_id | Household MGRA | 
| time_factor_work | Travel time sensitivity factor for work tours | 
| time_factor_nonwork | Travel time sensitivity factor for non-work tours (Sampled in person preprocessor) | 
| naics_code | Two-digit NAICS code (https://www.census.gov/naics/) | 
| occupation | Occupation String | 
| work_from_home | TRUE if worker and works from home, else FALSE (output from Work From Home Model) | 
| is_out_of_home_worker | TRUE if worker has a usual out of home work location, else FALSE (output from Work From Home Model) | 
| external_workplace_zone_id | MGRA number of external workplace if external worker, else -1 (output from External Workplace Location Choice Model) | 
| external_workplace_location_logsum | Location choice logsum for external workplace location choice model (output from External Workplace Location Choice Model) | 
| external_workplace_modechoice_logsum | Mode choice logsum for mode choice from external workplace location choice model (output from External Workplace Location Choice Model) | 
| school_zone_id | MGRA number of school location, else -9  (output from School Location Choice Model) | 
| school_location_logsum | Location choice logsum for school location choice model, else -9 (output from School Location Choice Model) | 
| school_modechoice_logsum | Mode choice logsum for mode choice from school location choice model, else -9 (output from School Location Choice Model) | 
| distance_to_school | Distance to school if student, else -9  (output from School Location Choice Model) | 
| roundtrip_auto_time_to_school | Round trip offpeak auto time to school, else -9  (output from School Location Choice Model) | 
| workplace_zone_id | MGRA number of internal work location, else -9  (output from Internal Work Location Choice Model) | 
| workplace_location_logsum | Location choice logsum for work location choice model, else -9 (output from Internal Work Location Choice Model) | 
| workplace_modechoice_logsum | Mode choice logsum for mode choice from work location choice model, else -9 (output from Internal Work Location Choice Model) | 
| distance_to_work | Distance to work if internal worker with work location, else -9  (output from Internal Work Location Choice Model) | 
| work_zone_area_type | Area type of work zone for worker if internal worker with work location, else -9 (output from Internal Work Location Choice Model) | 
| auto_time_home_to_work | Peak auto time from home to work if internal worker with work location, else -9  (output from Internal Work Location Choice Model) | 
| roundtrip_auto_time_to_work | Round trip auto travel time to and from work | 
| work_auto_savings | Travel time savings as a result of  using auto vs. walk-transit mode | 
| exp_daily_work | Expected daily cost of parking at work if internal worker with work location, else -9 (output from Internal Work Location Choice Model) | 
| non_toll_time_work | Time from home to work for path without I-15, if worker with internal workplace, else -9 | 
| toll_time_work | Time from home to work for path with I-15, if worker with internal workplace, else -9 | 
| toll_dist_work | Travel distance for work using a tolled route | 
| toll_cost_work | Toll cost for going to work | 
| toll_travel_time_savings_work | Work travel time savings for using tolled vs. non-tolled routes | 
| transit_pass_subsidy | 1 if person has subsidized transit from their employer or school, else 0 (Output from Transit Subsidy Model) | 
| transit_pass_ownership | 1 if person owns a transit pass, else 0 (Output from Transit Pass Ownership Model) | 
| free_parking_at_work | TRUE if person has free parking at work, else FALSE (Output from Free Parking Model) | 
| telecommute_frequency | Telecommute frequency if worker who does not work from hom, else null (Output from Telecommute Frequency Model) String "No_Telecommute", "1_day_week", "2_3_days_week", "4_days_week" | 
| cdap_activity | Coordinated daily activity pattern type (Output from Coordinated Daily Activity Pattern Model) String "M": Mandatory pattern, "N": Non-mandatory pattern, "H": Home or out of region pattern | 
| travel_active | TRUE if activity pattern is "M" or "N", else FALSE  (Output from Coordinated Daily Activity Pattern Model) | 
| num_joint_tours | Total number of fully joint tours (Output from Fully Joint Tour Participation Model) | 
| non_mandatory_tour_frequency | Non-Mandatory Tour Frequency Model Choice (Output from Non-Mandatory Tour Frequency Chopice Model) | 
| num_non_mand | Total number of non-mandatory tours (Output from School Escort Model, Non-Mandatory Tour Frequency Model, and At-Work Subtour Model) | 
| num_escort_tours | Total number of escorting tours (Output from School Escort Model and Non-Mandatory Tour Frequency Model) | 
| num_eatout_tours | Total number of eating out tours (Output from Non-Mandatory Tour Frequency Model) | 
| num_shop_tours | Total number of shopping tours (Output from Non-Mandatory Tour Frequency Model) | 
| num_maint_tours | Total number of other maintenance tours (Output from Non-Mandatory Tour Frequency Model) | 
| num_discr_tours | Total number of discretionary tours (Output from Non-Mandatory Tour Frequency Model) | 
| num_social_tours | Total number of social\visiting tours (Output from Non-Mandatory Tour Frequency Model) | 
| num_add_shop_maint_tours | Total number of additional shopping and maintenance tours (Output from Non-Mandatory Tour Frequency Extension Model) | 
| num_add_soc_discr_tours | Total number of additional social\visiting and other discretionary tours (Output from Non-Mandatory Tour Frequency Extension Model) | 
| person_id | Person ID | 
| miltary | 1 if serves in the military, else 0 | 
| grade | School grade of person: 0 = N/A (not attending school), 2 = K to grade 8, 5 = Grade 9 to grade 12, 6 = College undergraduate | 
| weeks | Weeks worked during past 12 months 0: N/A (less than 16 years old/did not work during the past 12 .months) 1: 50 to 52 weeks worked during past 12 months 2: 48 to 49 weeks worked during past 12 months 3: 40 to 47 weeks worked during past 12 months 4: 27 to 39 weeks worked during past 12 month 5: 14 to 26 weeks worked during past 12 months 6: 13 weeks or less worked during past 12 months | 
| hours | Usual hours worked per week past 12 months
0: .N/A (less than 16 years old/did not work during the past .12 months), 1..98 .1 to 98 usual hours, 99 .99 or more usual hours | 
| race | Recoded detailed race code 1: .White alone, 2: Black or African American alone, 3: American Indian alone, 4: Alaska Native alone, 5: American Indian and Alaska Native tribes specified; or .American Indian or Alaska Native, not specified and no other races, 6: Asian alone, 7: Native Hawaiian and Other Pacific Islander alone, 8: Some Other Race alone, 9: Two or More Races | 
| hispanic | Hispanic flag: 1: Non-Hispanic, 2: Hispanic | 

#### Resident Model vehicle file (final_vehicles.csv)

| **Field** | **Description** | 
| --- | --- |
| vehicle_id | Vehicle ID | 
| household_id | Household ID | 
| vehicle_num | Vehicle number in household from 1…n where n is total vehicles owned by household | 
| vehicle_type | String bodytype_age_fueltype | 
| auto_operating_cost | Auto operating cost for vehicle ($2023 cents/mile) | 
| Range | Range if electric vehicle, else 0 | 
| MPG | Miles per gallen for vehicle | 
| vehicle_year | Year of vehicle | 
| vehicle_category | String, Body type (Car, Motorcycle, Pickup, SUV, Van. Autonomous vehicles have _AV extension on body type) | 
| num_occupants | Number of occupants in the vehicle | 
| fuel_type | String. BEV: Battery electric vehicle, Diesel, Gas, Hybrid: Gas\Electric non plug-in vehicle, PEV: Plug-in hybrid electric vehicle | 

#### Resident Model joint tour participants file (final_joint_tour_participants.csv)

| **Field** | **Description** | 
| --- | --- |
| participant_id | Participant ID | 
| tour_id | Tour ID | 
| household_id | Household ID | 
| person_id | Person ID | 
| participant_num | Sequent number of participant 1…n where n is total number of participants in joint tour | 

#### Resident Model tour file (final_tours.csv)

| Field | Description | 
| --- | --- |
| tour_id | Tour ID | 
| person_id | Person ID | 
| tour_type | Purpose string of the primary activity on the tour: For home-based tours, the purposes are: “work”, “school”, “escort”, “shopping”, “othmaint”, “eatout”, “social”, and “othdiscr”. For work-based subtours, the purposes are “business”, “eat”, and “maint”. | 
| tour_type_count | The total number of tours within the tour_type | 
| tour_type_num | The sequential number of the tour within the tour_category. In other words if a person has 3 tours; 1 work tour and 2 non-mandatory tours, the tour_type_num would be 1 for the work tour, 1 for the first non-mandatory tour and 2 for the second non-mandatory tour. | 
| tour_num | ? | 
| tour_count | ? | 
| tour_category | The category string of the primary activity on the tour. “mandatory”, “joint”, “non_mandatory”, “atwork” | 
| number_of_participants | Number of participants on the tour for fully joint tours, else 1 | 
| destination | MGRA number of primary destination | 
| origin | MGRA number of tour origin | 
| household_id | Household ID | 
| start | Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| end | Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| duration | Duration of the tour in number of half-hour periods, including all activity episodes and travel | 
| school_esc_outbound | For school tours where the child is being escorted according to the school pickup/dropoff model, this string field indicates the type of escorting in the outbound direction: “pure_escort” or “rideshare” | 
| school_esc_inbound | For school tours where the child is being escorted according to the school pickup/dropoff model, this string field indicates the type of escorting in the inbound direction: “pure_escort” or “rideshare” | 
| num_escortees | Number of children being escorted on this tour (max of outbound and inbound direction) | 
| tdd | Tour departure and duration. Index of the tour departure and durarion alterntive configs | 
| composition | Composition of tour if joint “adults”, “children” | 
| is_external_tour | TRUE if primary destination activity is external to region, else FALSE | 
| is_internal_tour | Whether tour is internal | 
| destination_logsum | Logsum from tour destination choice model | 
| vehicle_occup_1 | Tour vehicle with occupancy of 1 | 
| vehicle_occup_2 | Tour vehicle with occupancy of 2 | 
| vehicle_occup_3_5 | Tour vehicle with occupancy of 3+ | 
| tour_mode | Tour mode string | 
| mode_choice_logsum | Logsum from tour mode choice model | 
| selected_vehicle | Selected vehicle from vehicle type choice model; a string field consisting of [Body type][age][fuel type] and an optional extension “_AV” if the vehicle is an autonomous vehicle | 
| atwork_subtour_frequency | At-work subtour frequency choice model result; a string field with the following values: “no_subtours”, “business1”, “business2”, “eat”, “eat_business”, “maint”, or blank for non-work tours. | 
| parent_tour_id | Parent tour ID if this is a work-based subtour, else 0 | 
| stop_frequency | Stop frequency choice model result; a string value of the form [0…n]out_[0…n]in where the first number is the number of outbound stops and the second number is the number of inbound stops | 
| primary_purpose | Recoding of tour_type where all atwork subtours are identified as “atwork” regardless of destination purpose | 

#### Resident Model trip file (final_trips.csv)

| Field | Description | 
| --- | --- |
| trip_id | Trip ID | 
| person_id | Person ID | 
| household_id | Household ID | 
| primary_purpose | Primary purpose of tour (see tour table) | 
| trip_num | Sequential number of trip by direction (1…n where n is maximum trips on half-tour, e.g. max stops + 1) | 
| outbound | TRUE if trip is in the outbound direction, else FALSE | 
| destination | MGRA of trip destination | 
| origin | MGRA of trip origin | 
| tour_id | Tour ID | 
| escort_participants | Space delimited string field listing person IDs of other children escorted on this trip, else null | 
| school_escort_direction | String field indicating whether child is being dropped off at school (“outbound”) or picked up from school (“inbound”). “null” if not a child being picked up or dropped off. | 
| purpose | Purpose at destination? | 
| destination_logsum | Logsum from trip destination choice model. -9 if destination is tour origin or primary destination. | 
| depart | Departure time period (1…48) | 
| trip_mode | Trip mode string | 
| mode_choice_logsum | Logsum from trip mode choice model | 
| vot | Value of time for trip in dollars per hour ($2023) | 
| owns_transponder | True if household owns transponder. Same as ownTrp | 
| totalWaitSingleTNC | Wait time for single pay TNC | 
| totalWaitSharedTNC | Wait time for shared\pooled TNC | 
| s2_time_skims | HOV2 travel time | 
| s2_dist_skims | HOV3 travel distance | 
| s2_cost_skims | HOV2 travel toll cost | 
| cost_parking | Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered. | 
| cost_fare_drive | Taxi/TNC fare for any trip or trip portion taken on these modes | 
| distance_walk | Walk mode distance? | 
| time_mm | Micromobility time | 
| distance_mm | Micromobility distance | 
| cost_fare_mm | Micromobility cost ($2023) | 
| distance_bike | Bike distance | 
| time_wait_drive | Waiting time for a TNC/ Taxi modes | 
| parking_zone | MGRA from parking location choice model at destination, else -1 | 
| trip_period | A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”) | 
| tour_participants | Number of joint tour participants if joint tour, else 1 | 
| distance_total | Trip distance in miles | 
| cost_operating_drive | Auto operating cost ($2023)? | 
| weight_trip | Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3) | 
| weight_person_trip | Person trip weigth defined as the ratio of the number of particpants on a trip to sample rate of the model run | 
| inbound | TRUE if trip is in the inbound direction, else FALSE | 
| time_drive | Auto time | 
| distance_drive | Auto distance | 
| cost_toll_drive | Auto toll cost ($2023)? | 
| time_transit_in_vehicle | Transit in-vehicle time | 
| time_rapid_transit_in_vehicle | Rapid transit in-vehicle time | 
| time_express_bus_transit_in_vehicle | Express bus in-vehicle time | 
| time_local_bus_transit_in_vehicle | Local bus in-vehicle time | 
| time_light_rail_transit_in_vehicle | Light rail transit in-vehicle time | 
| time_commuter_rail_transit_in_vehicle | Commuter rail in-vehicle time | 
| time_transit_initial_wait | Transit initial-wait time | 
| cost_fare_transit | Transit fare before subsidy ($2023) | 
| transfers_transit | Number of transfers | 
| time_bike | Bike time | 
| time_walk | Walk mode time | 
| cost_total | total cost of a trip (sum of auto operating, toll, transit fare) | 
| time_total | Total time (sum of drive, bike, walk, initial transit wait, transit time, transit transfer)) | 
| time_transit_wait | Transit total wait time? | 
| value_of_time_category_id | Value of time bin. 1: Low, 2: Medium, 3: High | 
| origin_micro_prm_dist | Distance from trip origin MGRA to closest premium transit stop by microtransit | 
| dest_micro_prm_dist | Distance from trip destination MGRA to closest premium transit stop by microtransit | 
| microtransit_orig | Distance from trip origin MGRA to closest local transit stop by microtransit? | 
| microtransit_dest | Distance from trip destination MGRA to closest local transit stop by microtransit? | 
| microtransit_available | TRUE if microtransit is available for trip, else FALSE | 
| nev_orig | Availability of Neighborhood Electric vehicle at origin | 
| nev_dest | Availability of Neighborhood Electric vehicle at destination | 
| nev_available | TRUE if neighborhood electric vehicle is available, else FALSE | 
| microtransit_access_available_out | TRUE if microtransit is available from the origin, else FALSE | 
| nev_access_available_out | TRUE if neighborhood electric vehicle is available from the origin, else FALSE? | 
| microtransit_egress_available_out | Availability of microtransit egress in the outbound direction | 
| nev_egress_available_out | Availability of NEV egress in the outbound direction | 
| microtransit_access_available_in | Availability of microtransit access in the inbound direction | 
| nev_access_available_in | Availability of NEV egress in the inbound direction | 
| microtransit_egress_available_in | Availability of microtransit egress in the inbound direction | 
| nev_egress_available_in | Availability of microtransit egress in the outbound direction | 
| trip_veh_body | Body type of vehicle used for trip, else “null” | 
| trip_veh_age | Age of vehicle used for trip, else “null” | 
| trip_veh_fueltype | Fuel type of vehicle used for trip, else “null” | 
| origin_purpose | Purpose at origin | 
| sample_rate | Sample rate | 
| origin_parking_zone | MGRA from parking location choice model at trip origin, else -1 | 
| otaz | Origin TAZ | 
| dtaz | Destination TAZ | 

### Skims (.\skims)

This directory contains auto, transit, and non-motorized level-of-service matrices, also known as skims. Each file is a collection of origin destination tables of times and costs, at the TAZ level.

| File | Description | 
| --- | --- |
| dest_pmsa.omx | A matrix containing pseudo - metropolitan statistical area code for each destination TAZ | 
| dest_poi.omx | A matrix containing point of interest code for each destination TAZ (currently zeros) | 
| dest_poi.omx.csv | A csv file containing point of interest code for each destination TAZ (currently zeros) | 
| impm(truck type)(toll type)_(period)_(matrixtype).txt | Truck impedance matrix for truck type (ld = Light duty, lhd = light heavy duty, mhd = medium heavy duty, hhd = heavy heavy duty), toll type (n = non-toll, t = toll) and matrixtype (DU = utility, dist = distance, time = time) | 
| maz_maz_bike.csv | Bike logsums between close together MGRAs | 
| maz_maz_walk.csv | Walk times between close together MGRAs | 
| maz_stop_walk.csv | Walk times between MGRAs and transit stops | 
| taz_pmsa_xwalk.csv | Crosswalk file between pseudo-metropolitan statistical areas and TAZs | 
| traffic_skims_(period).omx | Auto skims by period (EA, AM, MD, PM, EV) | 
| transit_skims_(period).omx | Transit skims by period (EA, AM, MD, PM, EV) | 

### Visitor model outputs (.\visitor)

This directory contains outputs from the overnight visitor model.

| File | Description | 
| --- | --- |
| [final_households.csv](#### Visitor Model household file (final_households.csv)) | Visitor model household file | 
| final_land_use.csv | Visitor model land-use file | 
| [final_persons.csv](#### Visitor Model person file (final_persons.csv)) | Visitor model person file | 
| [final_tours.csv](#### Visitor Model tour file (final_tours.csv)) | Visitor model tour file | 
| [final_trips.csv](#### Visitor Model trip file (final_trips.csv)) | Visitor model trip file | 
| model_metadata.yaml | Visitor model Datalake metadata file | 
| nmotVisitortrips_(period).omx | Visitor model non-motorized trips by period (EA, AM, MD, PM, EV) | 

#### Visitor Model household file (final_households.csv)

| Field | Description | 
| --- | --- |
| home_zone_id | Home MGRA | 
| sample_rate | Sample rate | 
| household_id | Household ID | 
| poverty | Poverty indicator utilized for social equity reports. Percentage value where value <= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty. | 

#### Visitor Model person file (final_persons.csv)

| Field | Description | 
| --- | --- |
| household_id | Household ID | 
| home_zone_id | Home MGRA | 
| person_id | Person ID | 

#### Visitor Model tour file (final_tours.csv)

| Field | Description | 
| --- | --- |
| tour_id | Tour ID | 
| tour_type | Type of tour. String. "dining", "recreation", or "work" | 
| purpose_id | Type of tour. 0: work, 1: "dining, 2: "recreation" | 
| visitor_travel_type | Visitor purpose. String. "business" or "personal" | 
| tour_category | Tour category. All tour categories in the visitor model are "non-mandatory" | 
| number_of_participants | Number of participants on tour | 
| auto_available | Auto availability indicator 0: not available, 1: available | 
| income | Income 0 - 4 | 
| origin | Tour origin MGRA | 
| tour_num | Sequential number of tour 1 to n where n is total number of tours | 
| tour_count | Number of tours per person | 
| household_id | Household ID | 
| person_id | Person ID | 
| start | Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| end | Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM. | 
| duration | Duration of the tour in number of half-hour periods, including all activity episodes and travel | 
| destination | Tour primary destination MGRA | 
| destination_logsum | Tour destination choice logsum | 
| tour_mode | Tour mode | 
| mode_choice_logsum | Tour mode choice logsum | 
| stop_frequency | Number of stops on tour by direction. String. xout_yin where x is number of stops in the outbound direction and y is the number of stops in the inbound direction | 
| primary_purpose | Primary purpose of a tour. String (recreation, dining, work) | 

#### Visitor Model trip file (final_trips.csv)

| Field | Description | 
| --- | --- |
| trip_id | Trip ID | 
| person_id | Person ID | 
| household_id | Household ID | 
| primary_purpose | Purpose at primary destination of tour. String. "dining", "recreation", or "work" | 
| trip_num | Sequential number of trip on half-tour from 1 to 4 | 
| outbound | TRUE if outbound, else FALSE | 
| trip_count | Number of trips in a tour | 
| destination | Destination MGRA | 
| origin | Origin MGRA | 
| tour_id | Tour ID | 
| purpose | Destination purpose. String. "dining", "recreation", or "work" | 
| destination_logsum | Destination choice logsum | 
| depart | Departure time period (1…48) | 
| trip_mode | Trip mode (see trip mode table) | 
| trip_mode_choice_logsum | Mode choice logsum for trip | 
| vot_da | will drop | 
| vot_s2 | will drop | 
| vot_s3 | will drop | 
| parking_cost | Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered. | 
| tnc_single_wait_time | Wait time for single pay TNC | 
| tnc_shared_wait_time | Wait time for shared\pooled TNC | 
| taxi_wait_time | Wait time for taxi | 
| cost_parking | Cost of parking ($2023) | 
| cost_fare_drive | Taxi/TNC fare for any trip or trip portion taken on these modes | 
| distance_walk | Walk mode distance? | 
| time_mm | Micromobility time | 
| distance_mm | Micromobility distance | 
| cost_fare_mm | Micromobility cost ($2023) | 
| distance_bike | Bike distance | 
| time_wait_drive | Ridehail/Taxi wait times for a trip | 
| trip_period | A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”) | 
| tour_participants | Number of tour participants | 
| distance_total | Trip distance | 
| cost_operating_drive | Auto operating cost ($2023)? | 
| weight_trip | Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3) | 
| weight_person_trip | Person trip weigth defined as the ratio of the number of particpants on a trip to sample rate of the model run | 
| vot | Value of time in dollars per hour ($2023) | 
| inbound | TRUE if trip is in outbound direction, else FALSE | 
| time_drive | Auto time | 
| distance_drive | Auto distance | 
| cost_toll_drive | Auto toll cost ($2023)? | 
| time_transit_in_vehicle | Transit in-vehicle time | 
| time_rapid_transit_in_vehicle | Rapid transit in-vehicle time | 
| time_express_bus_transit_in_vehicle | Express bus in-vehicle time | 
| time_local_bus_transit_in_vehicle | Local bus in-vehicle time | 
| time_light_rail_transit_in_vehicle | Light rail transit in-vehicle time | 
| time_commuter_rail_transit_in_vehicle | Commuter rail in-vehicle time | 
| time_transit_initial_wait | Transit initial-wait time | 
| cost_fare_transit | Transit fare ($2023) | 
| transfers_transit | Number of transfers | 
| time_bike | Bike time | 
| time_walk | Walk mode time | 
| cost_total | total cost of a trip (sum of auto operating, toll, transit fare) | 
| time_total | Total time (sum of ?) | 
| value_of_time_category_id | Value of time bin. 1: Low, 2: Medium, 3: High | 
| origin_micro_prm_dist | Distance from trip origin MGRA to closest premium transit stop by microtransit | 
| dest_micro_prm_dist | Distance from trip destination MGRA to closest premium transit stop by microtransit | 
| microtransit_orig | Distance from trip origin MGRA to closest local transit stop by microtransit? | 
| microtransit_dest | Distance from trip destination MGRA to closest local transit stop by microtransit? | 
| microtransit_available | TRUE if microtransit is available for trip, else FALSE | 
| nev_orig | True if Neghoborhood Electric Vehicle is available at origin | 
| nev_dest | True if Neghoborhood Electric Vehicle is available at destination | 
| nev_available | TRUE if neighborhood electric vehicle is available, else FALSE | 
| microtransit_access_available_out | TRUE if microtransit is available from the origin, else FALSE | 
| nev_access_available_out | TRUE if neighborhood electric vehicle is available from the origin, else FALSE | 
| microtransit_egress_available_out | Availability of microtransit egress in the outbound direction | 
| nev_egress_available_out | Availability of NEV egress in the outbound direction | 
| microtransit_access_available_in | Availability of microtransit access in the inbound direction | 
| nev_access_available_in | Availability of NEV egress in the inbound direction | 
| microtransit_egress_available_in | Availability of microtransit egress in the inbound direction | 
| nev_egress_available_in | Availability of microtransit egress in the outbound direction | 
| sample_rate | Sample rate | 
| otaz | Origin TAZ | 
| dtaz | Destination TAZ |