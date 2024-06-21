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


<table>
  <tr>
   <td><strong>Directory\File Name</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>airport.CBX (directory)
   </td>
   <td>Outputs for Cross-Border Express Airport Ground Access Model
   </td>
  </tr>
  <tr>
   <td>airport.SAN (directory)
   </td>
   <td>Outputs for San Diego International Airport Ground Access Model
   </td>
  </tr>
  <tr>
   <td>assignment (directory)
   </td>
   <td>Assignment outputs
   </td>
  </tr>
  <tr>
   <td>crossborder (directory)
   </td>
   <td>Crossborder Travel Model outputs
   </td>
  </tr>
  <tr>
   <td>cvm (directory)
   </td>
   <td>Commercial Vehicle Model outputs
   </td>
  </tr>
  <tr>
   <td>parking (directory)
   </td>
   <td>Parking model outputs
   </td>
  </tr>
  <tr>
   <td>resident (directory)
   </td>
   <td>Resident model outputs
   </td>
  </tr>
  <tr>
   <td>skims (directory)
   </td>
   <td>Skim outputs
   </td>
  </tr>
  <tr>
   <td>visitor (directory)
   </td>
   <td>Visitor Model outputs
   </td>
  </tr>
  <tr>
   <td>bikeMgraLogsum.csv
   </td>
   <td>Bike logsum file for close-together MGRAs
   </td>
  </tr>
  <tr>
   <td>bikeTazLogsum.csv
   </td>
   <td>Bike logsum file for TAZs
   </td>
  </tr>
  <tr>
   <td>datalake_metadata.yaml
   </td>
   <td>Metadata file for datalake reporting system
   </td>
  </tr>
  <tr>
   <td>derivedBikeEdges.csv
   </td>
   <td>Derived bike network edge file
   </td>
  </tr>
  <tr>
   <td>derivedBikeNodes.csv
   </td>
   <td>Derived bike network node file
   </td>
  </tr>
  <tr>
   <td>derivedBikeTraversals.csv
   </td>
   <td>Derived bike network traversals file
   </td>
  </tr>
  <tr>
   <td>microMgraEquivMinutes.csv
   </td>
   <td>Equivalent minutes for using micromobility between close-together MGRAs (not used)
   </td>
  </tr>
  <tr>
   <td>runtime_summary.csv
   </td>
   <td>Summary of model runtime
   </td>
  </tr>
  <tr>
   <td>temp_tazdata_cvm.csv
   </td>
   <td>TAZ data for commercial vehicle model
   </td>
  </tr>
  <tr>
   <td>transponderModelAccessibilities.csv
   </td>
   <td>Transponder model accessibilities (not used)
   </td>
  </tr>
  <tr>
   <td>trip_(period).omx
   </td>
   <td>Trips for each time period, for assignment
   </td>
  </tr>
  <tr>
   <td>walkMgraEquivMinutes.csv
   </td>
   <td>Equivalent minutes for walking between close-together MGRAs
   </td>
  </tr>
</table>



### Skims (.\skims)

This directory contains auto, transit, and non-motorized level-of-service matrices, also known as skims. Each file is a collection of origin destination tables of times and costs, at the TAZ level.


<table>
  <tr>
   <td>File
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>dest_pmsa.omx
   </td>
   <td>A matrix containing pseudo - metropolitan statistical area code for each destination TAZ
   </td>
  </tr>
  <tr>
   <td>dest_poi.omx
   </td>
   <td>A matrix containing point of interest code for each destination TAZ (currently zeros)
   </td>
  </tr>
  <tr>
   <td>dest_poi.omx.csv
   </td>
   <td>A csv file containing point of interest code for each destination TAZ (currently zeros)
   </td>
  </tr>
  <tr>
   <td>impm(truck type)(toll type)_(period)_(matrixtype).txt
   </td>
   <td>Truck impedance matrix for truck type (ld = Light duty, lhd = light heavy duty, mhd = medium heavy duty, hhd = heavy heavy duty), toll type (n = non-toll, t = toll) and matrixtype (DU = utility, dist = distance, time = time)
   </td>
  </tr>
  <tr>
   <td>maz_maz_bike.csv
   </td>
   <td>Bike logsums between close together MGRAs
   </td>
  </tr>
  <tr>
   <td>maz_maz_walk.csv
   </td>
   <td>Walk times between close together MGRAs
   </td>
  </tr>
  <tr>
   <td>maz_stop_walk.csv
   </td>
   <td>Walk times between MGRAs and transit stops
   </td>
  </tr>
  <tr>
   <td>taz_pmsa_xwalk.csv
   </td>
   <td>Crosswalk file between pseudo-metropolitan statistical areas and TAZs
   </td>
  </tr>
  <tr>
   <td><a href="#auto-skims">traffic_skims_(period).omx</a>
</td>
   <td>Auto skims by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td><a href="#transit-skims">transit_skims_(period).omx</a>
   </td>
   <td>Transit skims by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
</table>



#### Auto skims by period
<h4 id="auto-skims">Auto skims by period</h4>

TRAFFIC_SKIMS_<TIME_PERIOD>.OMX


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_TIME
   </td>
   <td>Travel time for evaluation of volume-delay functions
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_DIST
   </td>
   <td>Travel distance
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_REL
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_TOLLCOST
   </td>
   <td>Total toll cost (only for TOLL traffic classes)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_TOLLDIST
   </td>
   <td>Total distance on toll facilities (only for TOLL traffic classes)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_MLCOST
   </td>
   <td>Total cost for managed lane facilities (only for TOLL traffic classes)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_HOVDIST
   </td>
   <td>Distance on HOV facilities (only for HOV traffic classes)
   </td>
  </tr>
  <tr>
   <td>*traffic class = SOV_TR, SOV_NT, HOV2, HOV3, TRK
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>vot = L (low), M (medium), and H (high)
   </td>
   <td>
   </td>
  </tr>
</table>



<h4 id="transit-skims">Transit skims by period</h4>

TRANSIT_SKIMS<time period>.OMX


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_GENCOST
   </td>
   <td>Total generalized cost which includes perception factors from assignment
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_FIRSTWAIT
   </td>
   <td>actual wait time at initial boarding point
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_XFERWAIT
   </td>
   <td>actual wait time at all transfer boarding points
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALWAIT
   </td>
   <td>total actual wait time
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_FARE
   </td>
   <td>fare paid
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_XFERS
   </td>
   <td>number of transfers
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_ACCWALK
   </td>
   <td>access actual walk time prior to initial boarding
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_EGRWALK
   </td>
   <td>egress actual walk time after final alighting
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALWALK
   </td>
   <td>total actual walk time
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALIVTT
   </td>
   <td>Total actual in-vehicle travel time
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_DWELLTIME
   </td>
   <td>Total dwell time at stops
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_BUSIVTT
   </td>
   <td>actual in-vehicle travel time on local bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_LRTIVTT
   </td>
   <td>actual in-vehicle travel time on LRT mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class> _CMRIVTT
   </td>
   <td>actual in-vehicle travel time on commuter rail mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class> _EXPIVTT
   </td>
   <td>actual in-vehicle travel time on express bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_LTDEXPIVTT
   </td>
   <td>actual in-vehicle travel time on premium bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_BRTIVTT
   </td>
   <td>actual in-vehicle travel time on  BRT mode
   </td>
  </tr>
  <tr>
   <td>*time period = EA, AM, MD, PM, EV
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>transit_class = LOC, PRM, MIX
   </td>
   <td>
   </td>
  </tr>
</table>



### ActivitySim log files

ActivitySim writes out various log files when it runs; these have standard names for each model component. Therefore we list them separately, but copies of these files may be in each model’s output directory depending upon the settings used to run ActivitySim for that model component.


<table>
  <tr>
   <td><strong>File </strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>activitysim.log
   </td>
   <td>ActivitySim log file for model
   </td>
  </tr>
  <tr>
   <td>breadcrumbs.yaml
   </td>
   <td>Breadcrumbs provides a record of steps that have been run for use when resuming a model run
   </td>
  </tr>
  <tr>
   <td>final_checkpoints.csv
   </td>
   <td>ActivitySim checkpoint file 
   </td>
  </tr>
  <tr>
   <td>final_pipeline.h5
   </td>
   <td>ActivitySim pipeline file
   </td>
  </tr>
  <tr>
   <td>mem.csv
   </td>
   <td>ActivitySim memory use log file
   </td>
  </tr>
  <tr>
   <td>mem_mp_households.csv
   </td>
   <td>Memory logs for ActivitySim model steps running with the same num_processes (all except accessibility, initialize, and summarize)
   </td>
  </tr>
  <tr>
   <td>mem_mp_initialize.csv
   </td>
   <td>Memory logs for ActivitySim model step initialize
   </td>
  </tr>
  <tr>
   <td>mem_mp_summarize.csv
   </td>
   <td>Memory logs for ActivitySim model step summarize
   </td>
  </tr>
  <tr>
   <td>mp_households_(processnumber)-activitysim.log
   </td>
   <td>ActivitySim log file for processnumber. This logfile is created if model is run in multiprocess mode
   </td>
  </tr>
  <tr>
   <td>mp_households_(processnumber)-mem.csv
   </td>
   <td>Memory log file for processnumber
   </td>
  </tr>
  <tr>
   <td>mp_households_apportion-activitysim.log
   </td>
   <td>ActivitySim log file for apportioning data between multiple processes
   </td>
  </tr>
  <tr>
   <td>mp_households_coalesce-activitysim.log
   </td>
   <td>ActivitySIm logfile for coalesing output from multiple processes into one
   </td>
  </tr>
  <tr>
   <td>mp_initialize-activitysim.log
   </td>
   <td>ActivitySim log file for the initialization steps
   </td>
  </tr>
  <tr>
   <td>mp_initialize-mem.csv
   </td>
   <td>Memory logs for ActivitySim model step summarize (similar to mp_initialize-mem.csv)
   </td>
  </tr>
  <tr>
   <td>mp_setup_skims-activitysim.log
   </td>
   <td>ActivitySim logfile for reading in skims
   </td>
  </tr>
  <tr>
   <td>mp_summarize-activitysim.log
   </td>
   <td>ActivitySim log file for summarizing model output (omx and csv trip table)
   </td>
  </tr>
  <tr>
   <td>mp_summarize-mem.csv
   </td>
   <td>Memory logs for ActivitySim model step summarize (similar to mem_mp_initialize.csv)
   </td>
  </tr>
  <tr>
   <td>mp_tasks_log.txt
   </td>
   <td>Log files of multiprocessed steps
   </td>
  </tr>
  <tr>
   <td>omnibus_mem.csv
   </td>
   <td>Memory log file of all model steps (similar to mem.csv)
   </td>
  </tr>
  <tr>
   <td>run_list.txt
   </td>
   <td>List of models that have been run
   </td>
  </tr>
  <tr>
   <td>timing_log.csv
   </td>
   <td>Model run time by steps
   </td>
  </tr>
</table>



### Airport model outputs (.\airport.CBX, .\airport.SAN)

There are two subdirectories containing outputs for each of the two airport models. airport.CBX contains output for the Cross-Border Express model, and airport.SAN contains output for the San Diego International Airport model. Each directory has identical files so we provide one generic output table below.


<table>
  <tr>
   <td colspan="2" ><strong>Filename</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td colspan="2" >final_(airport)accessibility.csv
   </td>
   <td>Accessibility file for airport (cbx, san) (not used, created by default)
   </td>
  </tr>
  <tr>
   <td colspan="2" >[final_(airport)households.csv](#### Airport Model household file (final_(airport)households.csv))
   </td>
   <td>Household file for airport (cbx, san)
   </td>
  </tr>
  <tr>
   <td colspan="2" >final_(airport)land_use.csv
   </td>
   <td>Land-use file for airport (cbx, san)
   </td>
  </tr>
  <tr>
   <td colspan="2" >[final_(airport)persons.csv](#### Airport Model person file (final_(airport)persons.csv))
   </td>
   <td>Persons file for airport (cbx, san)
   </td>
  </tr>
  <tr>
   <td colspan="2" >[final_(airport)tours.csv](#### Airport Model tour file (final_(airport)tours.csv))
   </td>
   <td>Tour file for airport (cbx, san)
   </td>
  </tr>
  <tr>
   <td colspan="2" >[final_(airport)trips.csv](#### Airport Model trip file (final_(airport)trips.csv))
   </td>
   <td>Trip file for airport (cbx, san)
   </td>
  </tr>
  <tr>
   <td colspan="2" >model_metadata.yaml
   </td>
   <td>Datalake metadata file
   </td>
  </tr>
  <tr>
   <td colspan="2" >autoairporttrips.(airport)_(period).omx
   </td>
   <td>Auto trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td colspan="2" >tranairporttrips.(airport)_(period).omx
   </td>
   <td>Transit trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td colspan="2" >nmotairporttrips.(airport)_(period).omx
   </td>
   <td>Non-motorized trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
</table>



#### Airport Model household file (final_(airport)households.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Airport MGRA
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>poverty
   </td>
   <td>Poverty indicator utilized for social equity reports. Percentage value where value &lt;= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty.
   </td>
  </tr>
</table>



#### Airport Model person file (final_(airport)persons.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
</table>



#### Airport Model tour file (final_(airport)tours.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>purpose_id
   </td>
   <td>ID for tour type:
<p>
1 = resident business
<p>
2 = resident personal
<p>
3= visitor business
<p>
4 = visitor personal
<p>
5 = external
   </td>
  </tr>
  <tr>
   <td>party_size
   </td>
   <td>Number of persons in airport travel party
   </td>
  </tr>
  <tr>
   <td>nights
   </td>
   <td>Number of nights away
   </td>
  </tr>
  <tr>
   <td>income
   </td>
   <td>Income group 0-7,  -99 if employee
   </td>
  </tr>
  <tr>
   <td>direction
   </td>
   <td>Direction of trip. String. outbound: airport to non-airport, inbound: non-airport to airport
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>tour_category
   </td>
   <td>Tour category. String "non_mandatory"
   </td>
  </tr>
  <tr>
   <td>tour_type
   </td>
   <td>Type of tour. String. "Emp": Employee, "ext": External, "res_busn": Resident business where <em>n</em> is the ID for the income bracket (1&lt;25K, 2: between 25K & 50K, 3: between 50K & 75K, 4: between 75K & 100K, 5: between 100K & 125K, 6: between 125K & 150K, 7: between 150K & 200K, 8: 200k+
<p>
, "res_pern": Resident personal where n is the ID for the income bracket as defined above, "vis_bus": Visitor business, "vis_per": Visitor personal  
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Destination MGRA
   </td>
  </tr>
  <tr>
   <td>number_of_participants
   </td>
   <td>Same as party_size
   </td>
  </tr>
  <tr>
   <td>outbound
   </td>
   <td>TRUE if outbound, else FALSE
   </td>
  </tr>
  <tr>
   <td>start
   </td>
   <td>Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>end
   </td>
   <td>Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>duration
   </td>
   <td>Duration of the tour in number of half-hour periods, including all activity episodes and travel
   </td>
  </tr>
  <tr>
   <td>destination_logsum
   </td>
   <td>Logsum from destination choice model
   </td>
  </tr>
  <tr>
   <td>stop_frequency
   </td>
   <td>out_0in, 0out_in
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>"busn", "emp", "extn", "pern"
   </td>
  </tr>
</table>



#### Airport Model trip file (final_(airport)trips.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>trip_id
   </td>
   <td>Trip ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Primary purpose of trip: "busn": Business, where n is..., "emp": Employee, "extn": External, where n is..., "pern": Personal, where n is...
   </td>
  </tr>
  <tr>
   <td>trip_num
   </td>
   <td>1
   </td>
  </tr>
  <tr>
   <td>outbound
   </td>
   <td>TRUE if outbound, else FALSE
   </td>
  </tr>
  <tr>
   <td>trip_count
   </td>
   <td>1
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Destination MGRA
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>depart
   </td>
   <td>Departure time period (1…48)
   </td>
  </tr>
  <tr>
   <td>trip_mode
   </td>
   <td>Trip mode (see trip mode table)
   </td>
  </tr>
  <tr>
   <td>mode_choice_logsum
   </td>
   <td>Mode choice logsum for trip
   </td>
  </tr>
  <tr>
   <td>vot
   </td>
   <td>Value of time in dollars per hour ($2023)
   </td>
  </tr>
  <tr>
   <td>arrival_mode
   </td>
   <td>Arrival mode from airport trip mode choice model
   </td>
  </tr>
  <tr>
   <td>cost_parking
   </td>
   <td>Cost of parking ($2023)
   </td>
  </tr>
  <tr>
   <td>cost_fare_drive
   </td>
   <td>Ridehail/Taxi fare on a trip
   </td>
  </tr>
  <tr>
   <td>distance_walk
   </td>
   <td>Distance walked on a trip (including access/egress for transit modes)
   </td>
  </tr>
  <tr>
   <td>time_mm
   </td>
   <td>Micromobility time
   </td>
  </tr>
  <tr>
   <td>distance_mm
   </td>
   <td>Micromobility distance
   </td>
  </tr>
  <tr>
   <td>cost_fare_mm
   </td>
   <td>Micromobility cost ($2023)
   </td>
  </tr>
  <tr>
   <td>distance_bike
   </td>
   <td>Bike distance
   </td>
  </tr>
  <tr>
   <td>time_wait_drive
   </td>
   <td>Ridehail/Taxi wait times for a trip
   </td>
  </tr>
  <tr>
   <td>trip_period
   </td>
   <td>A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”)
   </td>
  </tr>
  <tr>
   <td>party
   </td>
   <td>Party size
   </td>
  </tr>
  <tr>
   <td>tour_participants
   </td>
   <td>Number of joint tour participants if joint tour, else 1
   </td>
  </tr>
  <tr>
   <td>distance_total
   </td>
   <td>Trip distance
   </td>
  </tr>
  <tr>
   <td>add_driver
   </td>
   <td>TRUE if trip requires a driver based on airport mode (for example, TNC, or pickup), else FALSE
   </td>
  </tr>
  <tr>
   <td>weight_trip
   </td>
   <td>1
   </td>
  </tr>
  <tr>
   <td>weight_person_trip
   </td>
   <td>weight_trip * tour_participants
   </td>
  </tr>
  <tr>
   <td>cost_operating_drive
   </td>
   <td>Auto operating cost ($2023)
   </td>
  </tr>
  <tr>
   <td>inbound
   </td>
   <td>TRUE if trip is from (origin) airport to (destination) non-airport zone, else FALSE
   </td>
  </tr>
  <tr>
   <td>time_drive
   </td>
   <td>Auto time
   </td>
  </tr>
  <tr>
   <td>distance_drive
   </td>
   <td>Auto distance
   </td>
  </tr>
  <tr>
   <td>cost_toll_drive
   </td>
   <td>Auto toll cost ($2023)
   </td>
  </tr>
  <tr>
   <td>time_transit_in_vehicle
   </td>
   <td>Transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_rapid_transit_in_vehicle
   </td>
   <td>Rapid transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_express_bus_transit_in_vehicle
   </td>
   <td>Express bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_local_bus_transit_in_vehicle
   </td>
   <td>Local bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_light_rail_transit_in_vehicle
   </td>
   <td>Light rail transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_commuter_rail_transit_in_vehicle
   </td>
   <td>Commuter rail in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_transit_initial_wait
   </td>
   <td>Transit initial-wait time
   </td>
  </tr>
  <tr>
   <td>cost_fare_transit
   </td>
   <td>Transit fare ($2023)
   </td>
  </tr>
  <tr>
   <td>transfers_transit
   </td>
   <td>Number of transfers
   </td>
  </tr>
  <tr>
   <td>time_bike
   </td>
   <td>Bike time
   </td>
  </tr>
  <tr>
   <td>time_walk
   </td>
   <td>Walk mode time
   </td>
  </tr>
  <tr>
   <td>cost_total
   </td>
   <td>Sum of all costs a trip might incur (auto operating, toll, transit fare)
   </td>
  </tr>
  <tr>
   <td>time_total
   </td>
   <td>Total travel time (including iIVT and access/egress and wait times for all modes)
   </td>
  </tr>
  <tr>
   <td>value_of_time_category_id
   </td>
   <td>Value of time bin. 1: Low, 2: Medium, 3: High
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>otaz
   </td>
   <td>Origin TAZ
   </td>
  </tr>
  <tr>
   <td>dtaz
   </td>
   <td>Destination TAZ
   </td>
  </tr>
</table>



#### Arrival Mode Table for Airport Models


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>Curb_LOCn
   </td>
   <td>Pickup/Dropoff curbside (n=1,5, with 1 = terminal, and 2,5 = other locations)
   </td>
  </tr>
  <tr>
   <td>TAXI_LOC<em>n</em>
   </td>
   <td>Taxi to airport (n =1,2 with 1= terminal mgra and 2=other)
   </td>
  </tr>
  <tr>
   <td>RIDEHAIL_LOC<em>n</em>
   </td>
   <td>Ridehail to airport (n =1,2 with 1= terminal mgra and 2=other)
   </td>
  </tr>
  <tr>
   <td>PARK_LOC<em>n</em>
   </td>
   <td>Parking lot (n=1,5, with 1 = terminal mgra and 2,5= other locations)
   </td>
  </tr>
  <tr>
   <td>PARK_ESCORT
   </td>
   <td>Parking escort
   </td>
  </tr>
  <tr>
   <td>SHUTTLEVAN
   </td>
   <td>Shuttle Vehicle
   </td>
  </tr>
  <tr>
   <td>RENTAL
   </td>
   <td>Rental car
   </td>
  </tr>
  <tr>
   <td>HOTEL_COURTESY
   </td>
   <td>Hotel transportation
   </td>
  </tr>
  <tr>
   <td>WALK
   </td>
   <td>Walk
   </td>
  </tr>
  <tr>
   <td>WALK_LOC, WALK_PRM, WALK_MIX
   </td>
   <td>Walk transit modes
   </td>
  </tr>
  <tr>
   <td>KNR_LOC, KNR_PRM, KNR_MIX
   </td>
   <td>KNR transit modes
   </td>
  </tr>
  <tr>
   <td>TNC_LOC, TNC_PRM, TNC_MIX
   </td>
   <td>TNC transit modes
   </td>
  </tr>
</table>



### Assignment model trip tables (.\assignment)

This directory contains trip tables from auto and transit assignments.


#### Demand Matrices


<table>
  <tr>
   <td><strong>File </strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>autoairportTrips.(airport)_(period_(vot).omx
   </td>
   <td>Auto trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
   </td>
  </tr>
  <tr>
   <td>autocrossborderTrips_(period)_(vot).omx
   </td>
   <td>Auto trip table for cross border model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
   </td>
  </tr>
  <tr>
   <td>autoTrips_(period)_(vot).omx
   </td>
   <td>Auto trip table for resident model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
   </td>
  </tr>
  <tr>
   <td>autovisitorTrips_(period)_(vot).omx
   </td>
   <td>Auto trip table for visitor model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
   </td>
  </tr>
  <tr>
   <td>emptyAVTrips.omx
   </td>
   <td>Empty private autonomous vehicle trips 
   </td>
  </tr>
  <tr>
   <td>householdAVTrips.csv
   </td>
   <td>All private autonomous vehicle trips
   </td>
  </tr>
  <tr>
   <td>TNCTrips.csv
   </td>
   <td>All TNC trips
   </td>
  </tr>
  <tr>
   <td>TNCVehicleTrips_(period).omx
   </td>
   <td>TNC vehicle trip table by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>TranairportTrips.(airport)_(period).omx
   </td>
   <td>Transit trip tables for airport (CBX, SAN) by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>TrancrossborderTrips_(period).omx
   </td>
   <td>Transit trip tables for cross-border model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>TranTrips_(period).omx
   </td>
   <td>Transit trip tables for resident model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>TranvisitorTrips_(period).omx
   </td>
   <td>Transit trip tables for visitor model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>TripMatrices.csv
   </td>
   <td>Disaggregate commercial vehicle trips
   </td>
  </tr>
</table>



#### Airport Model auto demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>SR2_&lt;<time period>>
   </td>
   <td>Shared Ride 2 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR3_&lt;<time period>>
   </td>
   <td>Shared Ride 3 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SOV_&lt;<time period>>
   </td>
   <td>Drive Alone for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Airport Model transit demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Airport Model non-motorized demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WALK_&lt;<time period>>
   </td>
   <td>Walk for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>BIKE_&lt;<time period>>
   </td>
   <td>Bike for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Crossborder Model auto demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>SR2_&lt;<time period>>
   </td>
   <td>Shared Ride 2 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR3_&lt;<time period>>
   </td>
   <td>Shared Ride 3 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SOV_&lt;<time period>>
   </td>
   <td>Drive Alone for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Crossborder Model transit demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Crossborder Model non-motorized demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WALK_&lt;<time period>>
   </td>
   <td>Walk for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>BIKE_&lt;<time period>>
   </td>
   <td>Bike for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Visitor Model auto demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>SR2_&lt;<time period>>
   </td>
   <td>Shared Ride 2 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR3_&lt;<time period>>
   </td>
   <td>Shared Ride 3 for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SOV_&lt;<time period>>
   </td>
   <td>Drive Alone for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Visitor Model transit demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set1_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set2_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>WLK_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>PNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>KNR_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>TNC_SET_set3_&lt;<time period>>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### Visitor Model non-motorized demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WALK_&lt;<time period>>
   </td>
   <td>Walk for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>BIKE_&lt;<time period>>
   </td>
   <td>Bike for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td colspan="2" >where TIME PERIOD = EA, AM, MD, PM, EV
   </td>
  </tr>
</table>



#### TNC Vehicle trip demand table


<table>
  <tr>
   <td>Column Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>trip_ID
   </td>
   <td>Trip ID
   </td>
  </tr>
  <tr>
   <td>vehicle_ID
   </td>
   <td>Vehicle ID
   </td>
  </tr>
  <tr>
   <td>originTaz
   </td>
   <td>Origin TAZ
   </td>
  </tr>
  <tr>
   <td>destinationTaz
   </td>
   <td>Destination TAZ
   </td>
  </tr>
  <tr>
   <td>originMgra
   </td>
   <td>Origin MGRA
   </td>
  </tr>
  <tr>
   <td>destinationMgra
   </td>
   <td>Destination MGRA
   </td>
  </tr>
  <tr>
   <td>totalPassengers
   </td>
   <td>Number of passengers in the vehicle
   </td>
  </tr>
  <tr>
   <td>startPeriod
   </td>
   <td>Trip starting period
   </td>
  </tr>
  <tr>
   <td>endPeriod
   </td>
   <td>Trip ending period
   </td>
  </tr>
  <tr>
   <td rowspan="26" >pickupIdsAtOrigin
   </td>
   <td>Trip id of the pick-up at origin.
   </td>
  </tr>
  <tr>
   <td>CR-RAMP:
   </td>
  </tr>
  <tr>
   <td>  Individual trips:
   </td>
  </tr>
  <tr>
   <td>  "I_" + personId + "_" + purpAbb + "_" + tourid + "_" + inbound + "_" + stopid
   </td>
  </tr>
  <tr>
   <td>   where purpAbb is the first 3 letters of the tour_purp field
   </td>
  </tr>
  <tr>
   <td>  Joint trips:
   </td>
  </tr>
  <tr>
   <td>  "J_" + hhid + "_" + purpAbb + "_" + tourid + "_" + inbound + "_" + stopid + ”_” + i
   </td>
  </tr>
  <tr>
   <td>   where i is a number ranging from 1 to the total number of participants.
   </td>
  </tr>
  <tr>
   <td>Visitor trips:
   </td>
  </tr>
  <tr>
   <td>  partySize == 1: "V_" + tourid + "_" + stopid
   </td>
  </tr>
  <tr>
   <td>  partySize > 1: "V_" + tourid + "_" + stopid + ”_” + i
   </td>
  </tr>
  <tr>
   <td>   where i is a number ranging from 1 to the total number of participants.
   </td>
  </tr>
  <tr>
   <td>Cross-border trips:
   </td>
  </tr>
  <tr>
   <td>  partySize == 1: "M_" + tourid + "_" + stopid
   </td>
  </tr>
  <tr>
   <td>  partySize > 1: "M_" + tourid + "_" + stopid + ”_” + i
   </td>
  </tr>
  <tr>
   <td>   where i is a number ranging from 1 to the total number of participants.
   </td>
  </tr>
  <tr>
   <td>CBX airport trips:
   </td>
  </tr>
  <tr>
   <td>  partySize == 1: "CBX_" + tourid + "_" + stopid
   </td>
  </tr>
  <tr>
   <td>  partySize>1: "CBX_" + tourid + "_" + stopid + ”_” + i
   </td>
  </tr>
  <tr>
   <td>   where i is a number ranging from 1 to the total number of participants.
   </td>
  </tr>
  <tr>
   <td>SAN airport trips:
   </td>
  </tr>
  <tr>
   <td>  partySize == 1: "SAN_" + tourid + "_" + stopid
   </td>
  </tr>
  <tr>
   <td>  partySize > 1: "SAN_" + tourid + "_" + stopid + ”_” + i
   </td>
  </tr>
  <tr>
   <td>   where i is a number ranging from 1 to the total number of participants.
   </td>
  </tr>
  <tr>
   <td>Internal-External trips:
   </td>
  </tr>
  <tr>
   <td>  "IE_" + tourid + "_" + inbound
   </td>
  </tr>
  <tr>
   <td>dropoffIdsAtOrigin
   </td>
   <td>Trip id of the drop-off at origin. See pickupIdsAtOrigin for trip id of the trip.
   </td>
  </tr>
  <tr>
   <td>pickupIdsAtDestination
   </td>
   <td>Trip id of the pick-up at destination. See pickupIdsAtOrigin for trip id of the trip.
   </td>
  </tr>
  <tr>
   <td>dropoffIdsAtDestination
   </td>
   <td>Trip id of the drop-off at destination. See pickupIdsAtOrigin for trip id of the trip.
   </td>
  </tr>
  <tr>
   <td>originPurpose
   </td>
   <td>Trip origin purpose
   </td>
  </tr>
  <tr>
   <td>destinationPurpose
   </td>
   <td>Trip destination purpose
   </td>
  </tr>
</table>



#### Household autonomous vehicle trip data


<table>
  <tr>
   <td>Column Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>hh_id
   </td>
   <td>Household id
   </td>
  </tr>
  <tr>
   <td>veh_id
   </td>
   <td>Vehicle id
   </td>
  </tr>
  <tr>
   <td>vehicleTrip_id
   </td>
   <td>Vehicle trip id
   </td>
  </tr>
  <tr>
   <td>orig_mgra
   </td>
   <td>Trip origin MGRA
   </td>
  </tr>
  <tr>
   <td>dest_gra
   </td>
   <td>Trip destination MGRA
   </td>
  </tr>
  <tr>
   <td>period
   </td>
   <td>Period:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Before 5:00AM
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>2 = 5:00AM-5:30AM
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>3 through 39 is every half hour time slots
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>40 = After 12:00AM
   </td>
  </tr>
  <tr>
   <td>occupants
   </td>
   <td>Number of occupants in the vehicle
   </td>
  </tr>
  <tr>
   <td>originIsHome
   </td>
   <td>Is origin home?
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = No
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Yes
   </td>
  </tr>
  <tr>
   <td>destinationIsHome
   </td>
   <td>Is destination home?
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = No
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Yes
   </td>
  </tr>
  <tr>
   <td>originIsRemoteParking
   </td>
   <td>Is origin remote parking?
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = No
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Yes
   </td>
  </tr>
  <tr>
   <td>destinationIsRemoteParking
   </td>
   <td>Is destination remote parking?
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = No
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Yes
   </td>
  </tr>
  <tr>
   <td>parkingChoiceAtDestination
   </td>
   <td>Parking choice at destination:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = Not constrained to remote parking
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Park at destination
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>2 = Remote parking
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>3 = Park at home
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person id
   </td>
  </tr>
  <tr>
   <td>person_num
   </td>
   <td>Person number
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour id
   </td>
  </tr>
  <tr>
   <td>stop_id
   </td>
   <td>Stop id
   </td>
  </tr>
  <tr>
   <td>inbound
   </td>
   <td>Is trip inbound?
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Yes
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = No
   </td>
  </tr>
  <tr>
   <td>tour_purpose
   </td>
   <td>Tour purpose:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Discretionary
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Eating Out
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Escort
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Home
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Maintenance
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>School
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Shop
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>University
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Visiting
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work-Based
   </td>
  </tr>
  <tr>
   <td>orig_purpose
   </td>
   <td>Origin trip purpose:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Discretionary
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Eating Out
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Escort
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Home
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Maintenance
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>School
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Shop
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>University
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Visiting
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work-Based
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work related
   </td>
  </tr>
  <tr>
   <td>dest_purpose
   </td>
   <td>Destination trip purpose:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Discretionary
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Eating Out
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Escort
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Home
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Maintenance
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>School
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Shop
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>University
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Visiting
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work-Based
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>Work related
   </td>
  </tr>
  <tr>
   <td>trip_orig_mgra
   </td>
   <td>Trip origin MGRA
   </td>
  </tr>
  <tr>
   <td>trip_dest_mgra
   </td>
   <td>Trip destination MGRA
   </td>
  </tr>
  <tr>
   <td>stop_period
   </td>
   <td>Stop period:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Before 5:00AM
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>2 = 5:00AM-5:30AM
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>3 through 39 is every half hour time slots
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>40 = After 12:00AM
   </td>
  </tr>
  <tr>
   <td>periodsUntilNextTrip
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>trip_mode
   </td>
   <td>Trip mode:
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>0 = Empty vehicle trip
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>1 = Drive Alone
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>2 = Shared Ride 2
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>3 = Shared Ride 3
   </td>
  </tr>
</table>



#### TNC vehicle trip matrix


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>TNC_&lt;<time period>>_0
   </td>
   <td>TNC trips for &lt;<time period>> with 0 passenger
   </td>
  </tr>
  <tr>
   <td>TNC_&lt;<time period>>_1
   </td>
   <td>TNC trips for &lt;<time period>> with 1 passenger
   </td>
  </tr>
  <tr>
   <td>TNC_&lt;<time period>>_2
   </td>
   <td>TNC trips for &lt;<time period>> with 2 passengers
   </td>
  </tr>
  <tr>
   <td>TNC_&lt;<time period>>_3
   </td>
   <td>TNC trips for &lt;<time period>> with 3 or more passengers
   </td>
  </tr>
</table>



#### Empty Autonomous vehicle trips data


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>EmptyAV_EA
   </td>
   <td>Empty AV trips for EA period
   </td>
  </tr>
  <tr>
   <td>EmptyAV_AM
   </td>
   <td>Empty AV trips for AM period
   </td>
  </tr>
  <tr>
   <td>EmptyAV_MD
   </td>
   <td>Empty AV trips for MD period
   </td>
  </tr>
  <tr>
   <td>EmptyAV_PM
   </td>
   <td>Empty AV trips for PM period
   </td>
  </tr>
  <tr>
   <td>EmptyAV_EV
   </td>
   <td>Empty AV trips for EV period
   </td>
  </tr>
</table>


Crossborder model outputs (.\crossborder)

This directory contains outputs from the Crossborder model, which represents all travel made by Mexico residents in San Diego County.


<table>
  <tr>
   <td>File 
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>final_accessibility.csv
   </td>
   <td>Accessibility file for Crossborder Model (not used, created by default)
   </td>
  </tr>
  <tr>
   <td>[final_households.csv](#### Crossborder Model household file (final_households.csv))
   </td>
   <td>Household file for Crossborder Model 
   </td>
  </tr>
  <tr>
   <td>final_land_use.csv
   </td>
   <td>Land-use file for Crossborder Model 
   </td>
  </tr>
  <tr>
   <td>[final_persons.csv](#### Crossborder Model person file (final_persons.csv))
   </td>
   <td>Persons file for Crossborder Model 
   </td>
  </tr>
  <tr>
   <td>[final_tours.csv](#### Crossborder Model tour file (final_tours.csv))
   </td>
   <td>Tour file for Crossborder Model 
   </td>
  </tr>
  <tr>
   <td>[final_trips.csv](#### Crossborder Model trip file (final_trips.csv))
   </td>
   <td>Tour file for Crossborder Model 
   </td>
  </tr>
  <tr>
   <td>model_metadata.yaml
   </td>
   <td>Model run meta data for use in Datalake storage and reporting
   </td>
  </tr>
  <tr>
   <td>nmCrossborderTrips_AM.omx
   </td>
   <td>Non-motorized trip table for Crossborder Model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>autoCrossborderTrips_AM.omx
   </td>
   <td>Auto trip table for Crossborder Model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>tranCrossborderTrips_AM.omx
   </td>
   <td>Transit trip table for Crossborder Model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>othrCrossborderTrips_AM.omx
   </td>
   <td>Other trip table for Crossborder Model by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
</table>



### Crossborder Model household file (final_households.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample Rate
   </td>
  </tr>
  <tr>
   <td>num_persons
   </td>
   <td>Number of persons in travel party
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA (Border crossing station)
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Home MGRA (Border crossing station)
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>poverty
   </td>
   <td>Poverty indicator utilized for social equity reports. Percentage value where value &lt;= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty.
   </td>
  </tr>
</table>



#### Crossborder Model person file (final_persons.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>work_time_factor
   </td>
   <td>Travel time sensitivity factor for work tours
   </td>
  </tr>
  <tr>
   <td>non_work_time_factor
   </td>
   <td>Travel time sensitivity factor for non-work tours (Sampled in person preprocessor)
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA (Border crossing station)
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Home MGRA (Border crossing station)
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
</table>



#### Crossborder Model tour file (final_tours.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>pass_type
   </td>
   <td>Type of border crossing pass. String. "no_pass": Does not own a pass, "sentri": SENTRI pass, or "ready": READY pass
   </td>
  </tr>
  <tr>
   <td>tour_type
   </td>
   <td>Tour purpose. String. "other", "school", "shop", "visit", or "work"
   </td>
  </tr>
  <tr>
   <td>purpose_id
   </td>
   <td>Tour purpose ID. 0: work, 1: school, 2: shop, 3: visit, 4: other
   </td>
  </tr>
  <tr>
   <td>tour_category
   </td>
   <td>Tour category. String. Mandatory: Work or school, Non-Mandatory: Shop, visit, other
   </td>
  </tr>
  <tr>
   <td>number_of_participants
   </td>
   <td>Number of participants in tour
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>start
   </td>
   <td>Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>end
   </td>
   <td>Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>duration
   </td>
   <td>Duration of the tour in number of half-hour periods, including all activity episodes and travel
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Tour origin (Border Crossing) MGRA
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Tour primary destination MGRA
   </td>
  </tr>
  <tr>
   <td>tour_od_logsum
   </td>
   <td>Tour origin-crossing-destination logsum
   </td>
  </tr>
  <tr>
   <td>poe_id
   </td>
   <td>Number of border crossing station
   </td>
  </tr>
  <tr>
   <td>tour_mode
   </td>
   <td>Tour mode
   </td>
  </tr>
  <tr>
   <td>mode_choice_logsum
   </td>
   <td>Tour mode choice logsum
   </td>
  </tr>
  <tr>
   <td>stop_frequency
   </td>
   <td>Number of stops on tour by direction. String. xout_yin where x is number of stops in the outbound direction and y is the number of stops in the inbound direction
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>will drop
   </td>
  </tr>
</table>



#### Crossborder Model trip file (final_trips.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>trip_id
   </td>
   <td>Trip ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Purpose at primary destination. String. "other", "school", "shop", "visit", or "work"
   </td>
  </tr>
  <tr>
   <td>trip_num
   </td>
   <td>Sequential number of trip on half-tour from 1 to 4
   </td>
  </tr>
  <tr>
   <td>outbound
   </td>
   <td>TRUE if outbound, else FALSE
   </td>
  </tr>
  <tr>
   <td>trip_count
   </td>
   <td>number of trips per tour. Will drop
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Destination MGRA
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>purpose
   </td>
   <td>Purpose at trip destination. String. "other", "school", "shop", "visit", or "work"
   </td>
  </tr>
  <tr>
   <td>depart
   </td>
   <td>Departure time period (1…48)
   </td>
  </tr>
  <tr>
   <td>trip_mode
   </td>
   <td>Trip mode (see trip mode table)
   </td>
  </tr>
  <tr>
   <td>trip_mode_choice_logsum
   </td>
   <td>Mode choice logsum for trip
   </td>
  </tr>
  <tr>
   <td>parking_cost
   </td>
   <td>Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered.
   </td>
  </tr>
  <tr>
   <td>tnc_single_wait_time
   </td>
   <td>Wait time for single pay TNC
   </td>
  </tr>
  <tr>
   <td>tnc_shared_wait_time
   </td>
   <td>Wait time for shared\pooled TNC
   </td>
  </tr>
  <tr>
   <td>taxi_wait_time
   </td>
   <td>Wait time for taxi
   </td>
  </tr>
  <tr>
   <td>cost_parking
   </td>
   <td>Cost of parking ($2023)
   </td>
  </tr>
  <tr>
   <td>cost_fare_drive
   </td>
   <td>Taxi/TNC fare (including Taxi/TNC cost of transit access/egress) ($2023)
   </td>
  </tr>
  <tr>
   <td>distance_walk
   </td>
   <td>Distance walked in miles (including access/egress walk distances of a transit mode)
   </td>
  </tr>
  <tr>
   <td>time_mm
   </td>
   <td>Micromobility time
   </td>
  </tr>
  <tr>
   <td>distance_mm
   </td>
   <td>Micromobility distance
   </td>
  </tr>
  <tr>
   <td>cost_fare_mm
   </td>
   <td>Micromobility cost ($2023)
   </td>
  </tr>
  <tr>
   <td>distance_bike
   </td>
   <td>Bike distance
   </td>
  </tr>
  <tr>
   <td>time_wait_drive
   </td>
   <td>Wait times for Taxi/TNC/NEV modes
   </td>
  </tr>
  <tr>
   <td>trip_period
   </td>
   <td>A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”)
   </td>
  </tr>
  <tr>
   <td>tour_participants
   </td>
   <td>Number of joint tour participants if joint tour, else 1
   </td>
  </tr>
  <tr>
   <td>distance_total
   </td>
   <td>Total distance traveled on a trip
   </td>
  </tr>
  <tr>
   <td>cost_operating_drive
   </td>
   <td>Auto operating cost ($2023)
   </td>
  </tr>
  <tr>
   <td>weight_trip
   </td>
   <td>Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3)
   </td>
  </tr>
  <tr>
   <td>weight_person_trip
   </td>
   <td>Person trip weight defined as the ratio of the number of participants on a trip to sample rate of the model run
   </td>
  </tr>
  <tr>
   <td>inbound
   </td>
   <td>TRUE if trip is in outbound direction, else FALSE
   </td>
  </tr>
  <tr>
   <td>time_drive
   </td>
   <td>Auto time
   </td>
  </tr>
  <tr>
   <td>distance_drive
   </td>
   <td>Auto distance
   </td>
  </tr>
  <tr>
   <td>cost_toll_drive
   </td>
   <td>Auto toll cost ($2023)
   </td>
  </tr>
  <tr>
   <td>time_transit_in_vehicle
   </td>
   <td>Transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_rapid_transit_in_vehicle
   </td>
   <td>Rapid transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_express_bus_transit_in_vehicle
   </td>
   <td>Express bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_local_bus_transit_in_vehicle
   </td>
   <td>Local bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_light_rail_transit_in_vehicle
   </td>
   <td>Light rail transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_commuter_rail_transit_in_vehicle
   </td>
   <td>Commuter rail in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_transit_initial_wait
   </td>
   <td>Transit initial-wait time
   </td>
  </tr>
  <tr>
   <td>cost_fare_transit
   </td>
   <td>Transit fare ($2023)
   </td>
  </tr>
  <tr>
   <td>transfers_transit
   </td>
   <td>Number of transfers
   </td>
  </tr>
  <tr>
   <td>time_bike
   </td>
   <td>Bike time
   </td>
  </tr>
  <tr>
   <td>time_walk
   </td>
   <td>Walk mode time
   </td>
  </tr>
  <tr>
   <td>cost_total
   </td>
   <td>Sum of all costs a trip might incur (auto operating, toll, transit fare)
   </td>
  </tr>
  <tr>
   <td>time_total
   </td>
   <td>Total travel time (including iIVT and access/egress and wait times for all modes)
   </td>
  </tr>
  <tr>
   <td>value_of_time_category_id
   </td>
   <td>Value of time bin. 1: Low, 2: Medium, 3: High
   </td>
  </tr>
  <tr>
   <td>origin_micro_prm_dist
   </td>
   <td>Distance from trip origin MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>dest_micro_prm_dist
   </td>
   <td>Distance from trip destination MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_orig
   </td>
   <td>Distance from trip origin MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_dest
   </td>
   <td>Distance from trip destination MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_available
   </td>
   <td>TRUE if microtransit is available for trip, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_orig
   </td>
   <td>True if Neighborhood Electric Vehicle is available at origin
   </td>
  </tr>
  <tr>
   <td>nev_dest
   </td>
   <td>True if Neighborhood Electric Vehicle is available at destination
   </td>
  </tr>
  <tr>
   <td>nev_available
   </td>
   <td>TRUE if Neighborhood Electric Vehicle is available, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_out
   </td>
   <td>TRUE if microtransit is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_access_available_out
   </td>
   <td>TRUE if neighborhood electric vehicle is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_out
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_out
   </td>
   <td>Availability of NEV egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_in
   </td>
   <td>Availability of microtransit access in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_access_available_in
   </td>
   <td>Availability of NEV egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_in
   </td>
   <td>Availability of microtransit egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_in
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>otaz
   </td>
   <td>Origin TAZ
   </td>
  </tr>
  <tr>
   <td>dtaz
   </td>
   <td>Destination TAZ
   </td>
  </tr>
</table>



#### Crossborder Model Tour Mode Definitions


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>DRIVEALONE
   </td>
   <td>Drive alone
   </td>
  </tr>
  <tr>
   <td>SHARED2
   </td>
   <td>Shared ride with 2 participants
   </td>
  </tr>
  <tr>
   <td>SHARED3
   </td>
   <td>Shared ride with 3+ participants
   </td>
  </tr>
  <tr>
   <td>WALK
   </td>
   <td>Walk
   </td>
  </tr>
</table>



### Commercial Vehicle Model (.\cvm)

//TODO

Update with CVM results once model is updated


### Parking cost calculations (.\parking)

This directory contains intermediate files and final expected parking costs calculated from input parking supply data and walk distances between MGRAs.


<table>
  <tr>
   <td>File 
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>aggregated_street_data.csv
   </td>
   <td>Street length and intersections aggregated to MGRA level, used to estimate free on-street parking spaces
   </td>
  </tr>
  <tr>
   <td>cache (directory)
   </td>
   <td>Directory containing intermediate calculations for expected parking costs
   </td>
  </tr>
  <tr>
   <td>distances.csv
   </td>
   <td>MGRA-MGRA distances used for expected parking cost calculations
   </td>
  </tr>
  <tr>
   <td>districts.csv
   </td>
   <td>Calculated parking districts at MGRA level used for expected parking cost calculations
   </td>
  </tr>
  <tr>
   <td>final_parking_data.csv
   </td>
   <td>Expected hourly, daily, and monthly parking costs, total spaces, and parking district at the MGRA level for use in travel models
   </td>
  </tr>
  <tr>
   <td>plots
   </td>
   <td>Directory containing plots of the parking model results
   </td>
  </tr>
  <tr>
   <td>shapefiles
   </td>
   <td>Directory containing shapefiles for parking model calculations
   </td>
  </tr>
</table>



### Resident model outputs (.\resident)

This directory contains San Diego resident travel model outputs.


<table>
  <tr>
   <td>File 
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>cdap_joint_spec_(persons).csv
   </td>
   <td>Model specification file for coordinated daily activity pattern model joint tour alternative for (persons)-way interaction terms
   </td>
  </tr>
  <tr>
   <td>cdap_spec_(persons).csv
   </td>
   <td>Model specification file for coordinated daily activity pattern model for (persons)-way interaction terms.
   </td>
  </tr>
  <tr>
   <td>data_dict.csv
   </td>
   <td>Data dictionary for resident model, csv format
   </td>
  </tr>
  <tr>
   <td>data_dict.txt
   </td>
   <td>Data dictionary for resident model, text format
   </td>
  </tr>
  <tr>
   <td>final_accessibility.csv
   </td>
   <td>Resident model aggregate accessibility file 
   </td>
  </tr>
  <tr>
   <td>final_disaggregate_accessibility.csv
   </td>
   <td>Resident model disaggregate accessibility file at MGRA level
   </td>
  </tr>
  <tr>
   <td>[final_households.csv](#### Resident Model household file (final_households.csv))
   </td>
   <td>Resident model household file
   </td>
  </tr>
  <tr>
   <td>[final_joint_tour_participants.csv](#### Resident Model joint tour participants file (final_joint_tour_participants.csv))
   </td>
   <td>Resident model joint tour participants file
   </td>
  </tr>
  <tr>
   <td>final_land_use.csv
   </td>
   <td>Resident model land-use file
   </td>
  </tr>
  <tr>
   <td>[final_persons.csv](#### Resident Model vehicle file (final_vehicles.csv))
   </td>
   <td>Resident model persons file
   </td>
  </tr>
  <tr>
   <td>final_proto_disaggregate_accessibility.csv
   </td>
   <td>Resident model disaggregate accessibility file at person level
   </td>
  </tr>
  <tr>
   <td>[final_tours.csv](#### Resident Model tour file (final_tours.csv))
   </td>
   <td>Resident model tour file
   </td>
  </tr>
  <tr>
   <td>[final_trips.csv](#### Resident Model trips file (final_trips.csv))
   </td>
   <td>Resident model trip file
   </td>
  </tr>
  <tr>
   <td>[final_vehicles.csv](#### Resident Model vehicle table (final_vehicles.csv))
   </td>
   <td>Resident model vehicle file
   </td>
  </tr>
  <tr>
   <td>log (directory)
   </td>
   <td>Directory for resident model logging output
   </td>
  </tr>
  <tr>
   <td>model_metadata.yaml
   </td>
   <td>Resident model Datalake metadata file
   </td>
  </tr>
  <tr>
   <td>autoTrips_[tod]_[vot].omx
   </td>
   <td>Residential Auto Trip Matrix for 5 time periods (tod = EA, AM, MD, PM, EV) and three value of time bins (vot = low, med, high)
   </td>
  </tr>
  <tr>
   <td>tranTrips_[tod].omx
   </td>
   <td>Residential Transit Trip Matrix for 5 time periods (tod = EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>nmotTrips_[tod].omx
   </td>
   <td>Residential Non-motorized Trip Matrix for 5 time periods (tod = EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>skim_usage.txt
   </td>
   <td>Skim usage file
   </td>
  </tr>
  <tr>
   <td>trace (directory)
   </td>
   <td>Directory for resident model trace output
   </td>
  </tr>
</table>



#### Resident Model household file (final_households.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Household MGRA - same as mgra
   </td>
  </tr>
  <tr>
   <td>income
   </td>
   <td>Household income in dollars ($2023)
   </td>
  </tr>
  <tr>
   <td>hhsize
   </td>
   <td>Number of persons in household
   </td>
  </tr>
  <tr>
   <td>HHT
   </td>
   <td>Household dwelling unit type. 0: N/A (GQ/vacant), 1: Married couple household, 2: Other family household: Male householder no spouse present, 3: Other family household: Female householder no spouse present, 4: Nonfamily household: Male householder living alone, 5: Nonfamily household: Male householder: Not living alone, 6: Nonfamily household: Female householder: Living alone, 7: Nonfamily household: Female householder: Not living alone
   </td>
  </tr>
  <tr>
   <td>auto_ownership
   </td>
   <td>(Model output) Auto ownership
   </td>
  </tr>
  <tr>
   <td>num_workers
   </td>
   <td>Number of workers in household
   </td>
  </tr>
  <tr>
   <td>building_category
   </td>
   <td>Units in structure. 0: N/A (GQ), 1: Mobile home or trailer, 2: One-family house detached, 3: One-family house attached, 4: 2 Apartments, 5: 3-4 Apartments, 6: 5-9 Apartments, 7: 10-19 Apartments, 8: 20-49 Apartments, 9: 50 or more apartments, 10: Boat, RV, van, etc.
   </td>
  </tr>
  <tr>
   <td>unittype
   </td>
   <td>Household unit type. 0: Non-GQ Household, 1: GQ Household (used in Visualizer)
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate for household  
   </td>
  </tr>
  <tr>
   <td>income_in_thousands
   </td>
   <td>Household income in thousands of dollars ($2023)
   </td>
  </tr>
  <tr>
   <td>income_segment
   </td>
   <td>Household income segment (1-4)
   </td>
  </tr>
  <tr>
   <td>num_non_workers
   </td>
   <td>Number of non-workers in household
   </td>
  </tr>
  <tr>
   <td>num_drivers
   </td>
   <td>Number of persons age 16+
   </td>
  </tr>
  <tr>
   <td>num_adults
   </td>
   <td>Number of persons age 18+
   </td>
  </tr>
  <tr>
   <td>ebike_owner
   </td>
   <td>TRUE if household owns an e-bike, else FALSE (output from e-bike owership simulation)
   </td>
  </tr>
  <tr>
   <td>av_ownership
   </td>
   <td>TRUE if household owns an autonomous vehicle, else FALSE (output from AV Ownership Model)
   </td>
  </tr>
  <tr>
   <td>workplace_location_accessibility
   </td>
   <td>Work location choice logsum (output from Disaggregate Accessibility Model)
   </td>
  </tr>
  <tr>
   <td>shopping_accessibility
   </td>
   <td>Shopping primary destination choice logsum (output from Disaggregate Accessibility Model)
   </td>
  </tr>
  <tr>
   <td>othdiscr_accessibility
   </td>
   <td>Other Discretionary primary destination choice logsum (output from Disaggregate Accessibility Model)
   </td>
  </tr>
  <tr>
   <td>numAVowned
   </td>
   <td>Number of autonomous vehicles owned by household (output from Vehicle Type Choice Model)
   </td>
  </tr>
  <tr>
   <td>transponder_ownership
   </td>
   <td>TRUE if household owns a transponder, else FALSE (output from Transponder Ownership Model)
   </td>
  </tr>
  <tr>
   <td>has_joint_tour
   </td>
   <td>1 if household has at least one fully joint tour, else false (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_under16_not_at_school
   </td>
   <td>Number of persons age less than 16 who do not attend school (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_travel_active
   </td>
   <td>Number of persons in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_travel_active_adults
   </td>
   <td>Number of adults in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_travel_active_preschoolers
   </td>
   <td>Number of preschool children in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_travel_active_children
   </td>
   <td>Number of children in household who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_travel_active_non_preschoolers
   </td>
   <td>Number of non-preschoolers household in who have an active (type M or N) travel pattern (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>participates_in_jtf_model
   </td>
   <td>TRUE if household has a joint tour frequency model, else FALSE (output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>school_escorting_outbound
   </td>
   <td>Alternative number for school escort model in the outbound direction (initial output from School Escort Model)
   </td>
  </tr>
  <tr>
   <td>school_escorting_inbound
   </td>
   <td>Alternative number for school escort model in the inbound direction (output from School Escort Model)
   </td>
  </tr>
  <tr>
   <td>school_escorting_outbound_cond
   </td>
   <td>Alternative number for school escort model in the outbound direction (final output from School Escort Model)
   </td>
  </tr>
  <tr>
   <td>auPkRetail
   </td>
   <td>Auto peak access to retail employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>auPkTotal
   </td>
   <td>Auto peak access to total employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>auOpRetail
   </td>
   <td>Auto offpeak access to retail employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>auOpTotal
   </td>
   <td>Auto offpeak access to total employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>trPkRetail
   </td>
   <td>Transit peak access to retail employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>trPkTotal
   </td>
   <td>Transit peak access to total employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>trPkHH
   </td>
   <td>Transit peak access to total employment from household (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>trOpRetail
   </td>
   <td>Transit offpeak access to retail employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>trOpTotal
   </td>
   <td>Transit offpeak access to total employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>nmRetail
   </td>
   <td>Walk access to retail employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>nmTotal
   </td>
   <td>Walk access to total employment from household TAZ (aggregate accessibility output)
   </td>
  </tr>
  <tr>
   <td>microtransit
   </td>
   <td>Microtransit access time in household MGRA
   </td>
  </tr>
  <tr>
   <td>nev
   </td>
   <td>Neighborhood electric vehicle access time in household MGRA
   </td>
  </tr>
  <tr>
   <td>mgra
   </td>
   <td>Household MGRA - same as home_zone_id
   </td>
  </tr>
  <tr>
   <td>TAZ
   </td>
   <td>Household TAZ
   </td>
  </tr>
  <tr>
   <td>micro_dist_local_bus
   </td>
   <td>Distance to closest local bus stop from household MGRA by microtransit, if available. 999999 if not available.
   </td>
  </tr>
  <tr>
   <td>micro_dist_premium_transit
   </td>
   <td>Distance to closest premium transit stop from household MGRA by microtransit, if available. 999999 if not available.
   </td>
  </tr>
  <tr>
   <td>joint_tour_frequency_composition
   </td>
   <td>Joint tour frequency and composition model choice (output from Joint Tour Frequency\Composition Model)
   </td>
  </tr>
  <tr>
   <td>num_hh_joint_tours
   </td>
   <td>Number of fully joint tours at the household level (0, 1 or 2) (output from Coordinated Daily Activity Pattern Model and Joint Tour Frequency\Composition Models)
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>poverty
   </td>
   <td>Poverty indicator utilized for social equity reports. Percentage value where value &lt;= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty.
   </td>
  </tr>
</table>


Resident Model person file (final_persons.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>age
   </td>
   <td>Person age in years
   </td>
  </tr>
  <tr>
   <td>PNUM
   </td>
   <td>Person number in household (1…n where n is number of persons in household)
   </td>
  </tr>
  <tr>
   <td>sex
   </td>
   <td>1: Male, 2: Female
   </td>
  </tr>
  <tr>
   <td>pemploy
   </td>
   <td>Employment status of person. 1: Employed Full-Time, 2: Employed Part-Time, 3: Unemployed or Not in Labor Force, 4: Less than 16 Years Old
   </td>
  </tr>
  <tr>
   <td>pstudent
   </td>
   <td>Student status of person. 1: Pre K-12, 2: College Undergrad+Grad and Prof. School, 3: Not Attending School
   </td>
  </tr>
  <tr>
   <td>ptype
   </td>
   <td>Person type  1: Full-time worker 2: Part-time worker 3: College\University Student 4: Non-Working Adult 5: Retired 6: Driving-age student 7: Non-driving age student 8: Pre-school\Age &lt;=5 
   </td>
  </tr>
  <tr>
   <td>educ
   </td>
   <td>Educational attainment. 1: No schooling completed, 9: High school graduate, 13: Bacehlor's degree
   </td>
  </tr>
  <tr>
   <td>soc2
   </td>
   <td>Two-digit Standard Occupational Classification (SOC) codes (https://www.bls.gov/oes/current/oes_stru.htm)
   </td>
  </tr>
  <tr>
   <td>is_student
   </td>
   <td>Person is a K12 or college student
   </td>
  </tr>
  <tr>
   <td>school_segment
   </td>
   <td>School location choice model's segment a student belongs to (preschool, grade school, high school, university)
   </td>
  </tr>
  <tr>
   <td>is_worker
   </td>
   <td>Person is a full-time or part-time worker
   </td>
  </tr>
  <tr>
   <td>is_internal_worker
   </td>
   <td>TRUE if worker works internal to region, else FALSE (output from Internal-External Worker Identification Model)
   </td>
  </tr>
  <tr>
   <td>is_external_worker
   </td>
   <td>TRUE if worker works external to region, else FALSE (output from Internal-External Worker Identification Model)
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Household MGRA
   </td>
  </tr>
  <tr>
   <td>time_factor_work
   </td>
   <td>Travel time sensitivity factor for work tours
   </td>
  </tr>
  <tr>
   <td>time_factor_nonwork
   </td>
   <td>Travel time sensitivity factor for non-work tours (Sampled in person preprocessor)
   </td>
  </tr>
  <tr>
   <td>naics_code
   </td>
   <td>Two-digit NAICS code (https://www.census.gov/naics/)
   </td>
  </tr>
  <tr>
   <td>occupation
   </td>
   <td>Occupation String
   </td>
  </tr>
  <tr>
   <td>work_from_home
   </td>
   <td>TRUE if worker and works from home, else FALSE (output from Work From Home Model)
   </td>
  </tr>
  <tr>
   <td>is_out_of_home_worker
   </td>
   <td>TRUE if worker has a usual out of home work location, else FALSE (output from Work From Home Model)
   </td>
  </tr>
  <tr>
   <td>external_workplace_zone_id
   </td>
   <td>MGRA number of external workplace if external worker, else -1 (output from External Workplace Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>external_workplace_location_logsum
   </td>
   <td>Location choice logsum for external workplace location choice model (output from External Workplace Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>external_workplace_modechoice_logsum
   </td>
   <td>Mode choice logsum for mode choice from external workplace location choice model (output from External Workplace Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>school_zone_id
   </td>
   <td>MGRA number of school location, else -9  (output from School Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>school_location_logsum
   </td>
   <td>Location choice logsum for school location choice model, else -9 (output from School Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>school_modechoice_logsum
   </td>
   <td>Mode choice logsum for mode choice from school location choice model, else -9 (output from School Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>distance_to_school
   </td>
   <td>Distance to school if student, else -9  (output from School Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>roundtrip_auto_time_to_school
   </td>
   <td>Round trip offpeak auto time to school, else -9  (output from School Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>workplace_zone_id
   </td>
   <td>MGRA number of internal work location, else -9  (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>workplace_location_logsum
   </td>
   <td>Location choice logsum for work location choice model, else -9 (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>workplace_modechoice_logsum
   </td>
   <td>Mode choice logsum for mode choice from work location choice model, else -9 (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>distance_to_work
   </td>
   <td>Distance to work if internal worker with work location, else -9  (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>work_zone_area_type
   </td>
   <td>Area type of work zone for worker if internal worker with work location, else -9 (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>auto_time_home_to_work
   </td>
   <td>Peak auto time from home to work if internal worker with work location, else -9  (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>roundtrip_auto_time_to_work
   </td>
   <td>Round trip auto travel time to and from work
   </td>
  </tr>
  <tr>
   <td>work_auto_savings
   </td>
   <td>Travel time savings as a result of  using auto vs. walk-transit mode
   </td>
  </tr>
  <tr>
   <td>exp_daily_work
   </td>
   <td>Expected daily cost of parking at work if internal worker with work location, else -9 (output from Internal Work Location Choice Model)
   </td>
  </tr>
  <tr>
   <td>non_toll_time_work
   </td>
   <td>Time from home to work for path without I-15, if worker with internal workplace, else -9
   </td>
  </tr>
  <tr>
   <td>toll_time_work
   </td>
   <td>Time from home to work for path with I-15, if worker with internal workplace, else -9
   </td>
  </tr>
  <tr>
   <td>toll_dist_work
   </td>
   <td>Travel distance for work using a tolled route 
   </td>
  </tr>
  <tr>
   <td>toll_cost_work
   </td>
   <td>Toll cost for going to work
   </td>
  </tr>
  <tr>
   <td>toll_travel_time_savings_work
   </td>
   <td>Work travel time savings for using tolled vs. non-tolled routes
   </td>
  </tr>
  <tr>
   <td>transit_pass_subsidy
   </td>
   <td>1 if person has subsidized transit from their employer or school, else 0 (Output from Transit Subsidy Model)
   </td>
  </tr>
  <tr>
   <td>transit_pass_ownership
   </td>
   <td>1 if person owns a transit pass, else 0 (Output from Transit Pass Ownership Model)
   </td>
  </tr>
  <tr>
   <td>free_parking_at_work
   </td>
   <td>TRUE if person has free parking at work, else FALSE (Output from Free Parking Model)
   </td>
  </tr>
  <tr>
   <td>telecommute_frequency
   </td>
   <td>Telecommute frequency if worker who does not work from hom, else null (Output from Telecommute Frequency Model) String "No_Telecommute", "1_day_week", "2_3_days_week", "4_days_week"
   </td>
  </tr>
  <tr>
   <td>cdap_activity
   </td>
   <td>Coordinated daily activity pattern type (Output from Coordinated Daily Activity Pattern Model) String "M": Mandatory pattern, "N": Non-mandatory pattern, "H": Home or out of region pattern
   </td>
  </tr>
  <tr>
   <td>travel_active
   </td>
   <td>TRUE if activity pattern is "M" or "N", else FALSE  (Output from Coordinated Daily Activity Pattern Model)
   </td>
  </tr>
  <tr>
   <td>num_joint_tours
   </td>
   <td>Total number of fully joint tours (Output from Fully Joint Tour Participation Model)
   </td>
  </tr>
  <tr>
   <td>non_mandatory_tour_frequency
   </td>
   <td>Non-Mandatory Tour Frequency Model Choice (Output from Non-Mandatory Tour Frequency Chopice Model)
   </td>
  </tr>
  <tr>
   <td>num_non_mand
   </td>
   <td>Total number of non-mandatory tours (Output from School Escort Model, Non-Mandatory Tour Frequency Model, and At-Work Subtour Model)
   </td>
  </tr>
  <tr>
   <td>num_escort_tours
   </td>
   <td>Total number of escorting tours (Output from School Escort Model and Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_eatout_tours
   </td>
   <td>Total number of eating out tours (Output from Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_shop_tours
   </td>
   <td>Total number of shopping tours (Output from Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_maint_tours
   </td>
   <td>Total number of other maintenance tours (Output from Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_discr_tours
   </td>
   <td>Total number of discretionary tours (Output from Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_social_tours
   </td>
   <td>Total number of social\visiting tours (Output from Non-Mandatory Tour Frequency Model)
   </td>
  </tr>
  <tr>
   <td>num_add_shop_maint_tours
   </td>
   <td>Total number of additional shopping and maintenance tours (Output from Non-Mandatory Tour Frequency Extension Model)
   </td>
  </tr>
  <tr>
   <td>num_add_soc_discr_tours
   </td>
   <td>Total number of additional social\visiting and other discretionary tours (Output from Non-Mandatory Tour Frequency Extension Model)
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>miltary
   </td>
   <td>1 if serves in the military, else 0
   </td>
  </tr>
  <tr>
   <td>grade
   </td>
   <td>School grade of person: 0 = N/A (not attending school), 2 = K to grade 8, 5 = Grade 9 to grade 12, 6 = College undergraduate
   </td>
  </tr>
  <tr>
   <td>weeks
   </td>
   <td>Weeks worked during past 12 months 0: N/A (less than 16 years old/did not work during the past 12 .months) 1: 50 to 52 weeks worked during past 12 months 2: 48 to 49 weeks worked during past 12 months 3: 40 to 47 weeks worked during past 12 months 4: 27 to 39 weeks worked during past 12 month 5: 14 to 26 weeks worked during past 12 months 6: 13 weeks or less worked during past 12 months
   </td>
  </tr>
  <tr>
   <td>hours
   </td>
   <td>Usual hours worked per week past 12 months
<p>
0: .N/A (less than 16 years old/did not work during the past .12 months), 1..98 .1 to 98 usual hours, 99 .99 or more usual hours
   </td>
  </tr>
  <tr>
   <td>race
   </td>
   <td>Recoded detailed race code 1: .White alone, 2: Black or African American alone, 3: American Indian alone, 4: Alaska Native alone, 5: American Indian and Alaska Native tribes specified; or .American Indian or Alaska Native, not specified and no other races, 6: Asian alone, 7: Native Hawaiian and Other Pacific Islander alone, 8: Some Other Race alone, 9: Two or More Races
   </td>
  </tr>
  <tr>
   <td>hispanic
   </td>
   <td>Hispanic flag: 1: Non-Hispanic, 2: Hispanic
   </td>
  </tr>
</table>


Resident Model vehicle file (final_vehicles.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>vehicle_id
   </td>
   <td>Vehicle ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>vehicle_num
   </td>
   <td>Vehicle number in household from 1…n where n is total vehicles owned by household
   </td>
  </tr>
  <tr>
   <td>vehicle_type
   </td>
   <td>String bodytype_age_fueltype
   </td>
  </tr>
  <tr>
   <td>auto_operating_cost
   </td>
   <td>Auto operating cost for vehicle ($2023 cents/mile)
   </td>
  </tr>
  <tr>
   <td>Range
   </td>
   <td>Range if electric vehicle, else 0
   </td>
  </tr>
  <tr>
   <td>MPG
   </td>
   <td>Miles per gallen for vehicle
   </td>
  </tr>
  <tr>
   <td>vehicle_year
   </td>
   <td>Year of vehicle 
   </td>
  </tr>
  <tr>
   <td>vehicle_category
   </td>
   <td>String, Body type (Car, Motorcycle, Pickup, SUV, Van. Autonomous vehicles have _AV extension on body type)
   </td>
  </tr>
  <tr>
   <td>num_occupants
   </td>
   <td>Number of occupants in the vehicle
   </td>
  </tr>
  <tr>
   <td>fuel_type
   </td>
   <td>String. BEV: Battery electric vehicle, Diesel, Gas, Hybrid: Gas\Electric non plug-in vehicle, PEV: Plug-in hybrid electric vehicle
   </td>
  </tr>
</table>



#### Resident Model joint tour participants file (final_joint_tour_participants.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>participant_id
   </td>
   <td>Participant ID
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>participant_num
   </td>
   <td>Sequent number of participant 1…n where n is total number of participants in joint tour
   </td>
  </tr>
</table>



#### Resident Model tour file (final_tours.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>tour_type
   </td>
   <td>Purpose string of the primary activity on the tour: For home-based tours, the purposes are: “work”, “school”, “escort”, “shopping”, “othmaint”, “eatout”, “social”, and “othdiscr”. For work-based subtours, the purposes are “business”, “eat”, and “maint”.
   </td>
  </tr>
  <tr>
   <td>tour_type_count
   </td>
   <td>The total number of tours within the tour_type
   </td>
  </tr>
  <tr>
   <td>tour_type_num
   </td>
   <td>The sequential number of the tour within the tour_category. In other words if a person has 3 tours; 1 work tour and 2 non-mandatory tours, the tour_type_num would be 1 for the work tour, 1 for the first non-mandatory tour and 2 for the second non-mandatory tour.
   </td>
  </tr>
  <tr>
   <td>tour_num
   </td>
   <td>Sequential tour ID number for a person
   </td>
  </tr>
  <tr>
   <td>tour_count
   </td>
   <td>Total number of tours per person
   </td>
  </tr>
  <tr>
   <td>tour_category
   </td>
   <td>The category string of the primary activity on the tour. “mandatory”, “joint”, “non_mandatory”, “atwork”
   </td>
  </tr>
  <tr>
   <td>number_of_participants
   </td>
   <td>Number of participants on the tour for fully joint tours, else 1
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>MGRA number of primary destination
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>MGRA number of tour origin
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>start
   </td>
   <td>Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>end
   </td>
   <td>Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>duration
   </td>
   <td>Duration of the tour in number of half-hour periods, including all activity episodes and travel
   </td>
  </tr>
  <tr>
   <td>school_esc_outbound
   </td>
   <td>For school tours where the child is being escorted according to the school pickup/dropoff model, this string field indicates the type of escorting in the outbound direction: “pure_escort” or “rideshare”
   </td>
  </tr>
  <tr>
   <td>school_esc_inbound
   </td>
   <td>For school tours where the child is being escorted according to the school pickup/dropoff model, this string field indicates the type of escorting in the inbound direction: “pure_escort” or “rideshare”
   </td>
  </tr>
  <tr>
   <td>num_escortees
   </td>
   <td>Number of children being escorted on this tour (max of outbound and inbound direction)
   </td>
  </tr>
  <tr>
   <td>tdd
   </td>
   <td>Tour departure and duration. Index of the tour departure and durarion alterntive configs
   </td>
  </tr>
  <tr>
   <td>composition
   </td>
   <td>Composition of tour if joint “adults”, “children”
   </td>
  </tr>
  <tr>
   <td>is_external_tour
   </td>
   <td>TRUE if primary destination activity is external to region, else FALSE
   </td>
  </tr>
  <tr>
   <td>is_internal_tour
   </td>
   <td>Whether tour is internal
   </td>
  </tr>
  <tr>
   <td>destination_logsum
   </td>
   <td>Logsum from tour destination choice model
   </td>
  </tr>
  <tr>
   <td>vehicle_occup_1
   </td>
   <td>Tour vehicle with occupancy of 1
   </td>
  </tr>
  <tr>
   <td>vehicle_occup_2
   </td>
   <td>Tour vehicle with occupancy of 2
   </td>
  </tr>
  <tr>
   <td>vehicle_occup_3_5
   </td>
   <td>Tour vehicle with occupancy of 3+
   </td>
  </tr>
  <tr>
   <td>tour_mode
   </td>
   <td>Tour mode string 
   </td>
  </tr>
  <tr>
   <td>mode_choice_logsum
   </td>
   <td>Logsum from tour mode choice model
   </td>
  </tr>
  <tr>
   <td>selected_vehicle
   </td>
   <td>Selected vehicle from vehicle type choice model; a string field consisting of [Body type][age][fuel type] and an optional extension “_AV” if the vehicle is an autonomous vehicle
   </td>
  </tr>
  <tr>
   <td>atwork_subtour_frequency
   </td>
   <td>At-work subtour frequency choice model result; a string field with the following values: “no_subtours”, “business1”, “business2”, “eat”, “eat_business”, “maint”, or blank for non-work tours.
   </td>
  </tr>
  <tr>
   <td>parent_tour_id
   </td>
   <td>Parent tour ID if this is a work-based subtour, else 0
   </td>
  </tr>
  <tr>
   <td>stop_frequency
   </td>
   <td>Stop frequency choice model result; a string value of the form [0…n]out_[0…n]in where the first number is the number of outbound stops and the second number is the number of inbound stops
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Recoding of tour_type where all atwork subtours are identified as “atwork” regardless of destination purpose
   </td>
  </tr>
</table>



#### Resident Model trip file (final_trips.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>trip_id
   </td>
   <td>Trip ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Primary purpose of tour (see tour table)
   </td>
  </tr>
  <tr>
   <td>trip_num
   </td>
   <td>Sequential number of trip by direction (1…n where n is maximum trips on half-tour, e.g. max stops + 1)
   </td>
  </tr>
  <tr>
   <td>outbound
   </td>
   <td>TRUE if trip is in the outbound direction, else FALSE
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>MGRA of trip destination
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>MGRA of trip origin
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>escort_participants
   </td>
   <td>Space delimited string field listing person IDs of other children escorted on this trip, else null
   </td>
  </tr>
  <tr>
   <td>school_escort_direction
   </td>
   <td>String field indicating whether child is being dropped off at school (“outbound”) or picked up from school (“inbound”). “null” if not a child being picked up or dropped off.
   </td>
  </tr>
  <tr>
   <td>purpose
   </td>
   <td>Purpose at destination
   </td>
  </tr>
  <tr>
   <td>destination_logsum
   </td>
   <td>Logsum from trip destination choice model. -9 if destination is tour origin or primary destination.
   </td>
  </tr>
  <tr>
   <td>depart
   </td>
   <td>Departure time period (1…48)
   </td>
  </tr>
  <tr>
   <td>trip_mode
   </td>
   <td>Trip mode string
   </td>
  </tr>
  <tr>
   <td>mode_choice_logsum
   </td>
   <td>Logsum from trip mode choice model
   </td>
  </tr>
  <tr>
   <td>vot
   </td>
   <td>Value of time for trip in dollars per hour ($2023)
   </td>
  </tr>
  <tr>
   <td>owns_transponder
   </td>
   <td>True if household owns transponder. Same as ownTrp 
   </td>
  </tr>
  <tr>
   <td>totalWaitSingleTNC
   </td>
   <td>Wait time for single pay TNC
   </td>
  </tr>
  <tr>
   <td>totalWaitSharedTNC
   </td>
   <td>Wait time for shared\pooled TNC
   </td>
  </tr>
  <tr>
   <td>s2_time_skims
   </td>
   <td>HOV2 travel time 
   </td>
  </tr>
  <tr>
   <td>s2_dist_skims
   </td>
   <td>HOV3 travel distance
   </td>
  </tr>
  <tr>
   <td>s2_cost_skims
   </td>
   <td>HOV2 travel toll cost
   </td>
  </tr>
  <tr>
   <td>cost_parking
   </td>
   <td>Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered.
   </td>
  </tr>
  <tr>
   <td>cost_fare_drive
   </td>
   <td>Taxi/TNC fare for any trip or trip portion taken on these modes
   </td>
  </tr>
  <tr>
   <td>distance_walk
   </td>
   <td>Distance walked on a trip (including access/egress for transit modes)
   </td>
  </tr>
  <tr>
   <td>time_mm
   </td>
   <td>Micromobility time
   </td>
  </tr>
  <tr>
   <td>distance_mm
   </td>
   <td>Micromobility distance
   </td>
  </tr>
  <tr>
   <td>cost_fare_mm
   </td>
   <td>Micromobility cost ($2023)
   </td>
  </tr>
  <tr>
   <td>distance_bike
   </td>
   <td>Bike distance
   </td>
  </tr>
  <tr>
   <td>time_wait_drive
   </td>
   <td>Waiting time for a TNC/ Taxi modes
   </td>
  </tr>
  <tr>
   <td>parking_zone
   </td>
   <td>MGRA from parking location choice model at destination, else -1
   </td>
  </tr>
  <tr>
   <td>trip_period
   </td>
   <td>A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”)
   </td>
  </tr>
  <tr>
   <td>tour_participants
   </td>
   <td>Number of joint tour participants if joint tour, else 1
   </td>
  </tr>
  <tr>
   <td>distance_total
   </td>
   <td>Trip distance in miles
   </td>
  </tr>
  <tr>
   <td>cost_operating_drive
   </td>
   <td>Auto operating cost ($2023)
   </td>
  </tr>
  <tr>
   <td>weight_trip
   </td>
   <td>Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3)
   </td>
  </tr>
  <tr>
   <td>weight_person_trip
   </td>
   <td>Person trip weigth defined as the ratio of the number of particpants on a trip to sample rate of the model run
   </td>
  </tr>
  <tr>
   <td>inbound
   </td>
   <td>TRUE if trip is in the inbound direction, else FALSE
   </td>
  </tr>
  <tr>
   <td>time_drive
   </td>
   <td>Auto time
   </td>
  </tr>
  <tr>
   <td>distance_drive
   </td>
   <td>Auto distance
   </td>
  </tr>
  <tr>
   <td>cost_toll_drive
   </td>
   <td>Auto toll cost ($2023)
   </td>
  </tr>
  <tr>
   <td>time_transit_in_vehicle
   </td>
   <td>Transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_rapid_transit_in_vehicle
   </td>
   <td>Rapid transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_express_bus_transit_in_vehicle
   </td>
   <td>Express bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_local_bus_transit_in_vehicle
   </td>
   <td>Local bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_light_rail_transit_in_vehicle
   </td>
   <td>Light rail transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_commuter_rail_transit_in_vehicle
   </td>
   <td>Commuter rail in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_transit_initial_wait
   </td>
   <td>Transit initial-wait time
   </td>
  </tr>
  <tr>
   <td>cost_fare_transit
   </td>
   <td>Transit fare before subsidy ($2023)
   </td>
  </tr>
  <tr>
   <td>transfers_transit
   </td>
   <td>Number of transfers
   </td>
  </tr>
  <tr>
   <td>time_bike
   </td>
   <td>Bike time
   </td>
  </tr>
  <tr>
   <td>time_walk
   </td>
   <td>Walk mode time
   </td>
  </tr>
  <tr>
   <td>cost_total
   </td>
   <td>total cost of a trip (sum of auto operating, toll, transit fare)
   </td>
  </tr>
  <tr>
   <td>time_total
   </td>
   <td>Total time (sum of drive, bike, walk, initial transit wait, transit time, transit transfer))
   </td>
  </tr>
  <tr>
   <td>time_transit_wait
   </td>
   <td>Total transit wait time (initial, transfer, NEV wait, waiting for school bus)
   </td>
  </tr>
  <tr>
   <td>value_of_time_category_id
   </td>
   <td>Value of time bin. 1: Low, 2: Medium, 3: High
   </td>
  </tr>
  <tr>
   <td>origin_micro_prm_dist
   </td>
   <td>Distance from trip origin MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>dest_micro_prm_dist
   </td>
   <td>Distance from trip destination MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_orig
   </td>
   <td>Distance from trip origin MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_dest
   </td>
   <td>Distance from trip destination MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_available
   </td>
   <td>TRUE if microtransit is available for trip, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_orig
   </td>
   <td>Availability of Neighborhood Electric vehicle at origin
   </td>
  </tr>
  <tr>
   <td>nev_dest
   </td>
   <td>Availability of Neighborhood Electric vehicle at destination
   </td>
  </tr>
  <tr>
   <td>nev_available
   </td>
   <td>TRUE if neighborhood electric vehicle is available, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_out
   </td>
   <td>TRUE if microtransit is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_access_available_out
   </td>
   <td>TRUE if neighborhood electric vehicle is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_out
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_out
   </td>
   <td>Availability of NEV egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_in
   </td>
   <td>Availability of microtransit access in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_access_available_in
   </td>
   <td>Availability of NEV egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_in
   </td>
   <td>Availability of microtransit egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_in
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>trip_veh_body
   </td>
   <td>Body type of vehicle used for trip, else “null”
   </td>
  </tr>
  <tr>
   <td>trip_veh_age
   </td>
   <td>Age of vehicle used for trip, else “null”
   </td>
  </tr>
  <tr>
   <td>trip_veh_fueltype
   </td>
   <td>Fuel type of vehicle used for trip, else “null”
   </td>
  </tr>
  <tr>
   <td>origin_purpose
   </td>
   <td>Purpose at origin
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>origin_parking_zone
   </td>
   <td>MGRA from parking location choice model at trip origin, else -1
   </td>
  </tr>
  <tr>
   <td>otaz
   </td>
   <td>Origin TAZ
   </td>
  </tr>
  <tr>
   <td>dtaz
   </td>
   <td>Destination TAZ
   </td>
  </tr>
</table>



#### Resident Model tour mode definitions


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>DRIVEALONE
   </td>
   <td>Drive alone
   </td>
  </tr>
  <tr>
   <td>SHARED2
   </td>
   <td>Shared ride with 2 participants
   </td>
  </tr>
  <tr>
   <td>SHARED3
   </td>
   <td>Shared ride with 3+ participants
   </td>
  </tr>
  <tr>
   <td>WALK
   </td>
   <td>Walk
   </td>
  </tr>
  <tr>
   <td>BIKE
   </td>
   <td>Bike
   </td>
  </tr>
  <tr>
   <td>WALK_LOC
   </td>
   <td>Local transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_PRM
   </td>
   <td>Premium transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_MIX
   </td>
   <td>Mix (local with premium transfers) transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_LOC
   </td>
   <td>Local transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_PRM
   </td>
   <td>Premium transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_MIX
   </td>
   <td>Mix (local with premium transfers) transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_LOC
   </td>
   <td>Local transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_PRM
   </td>
   <td>Premium transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_MIX
   </td>
   <td>Mix (local with premium transfers) transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_LOC
   </td>
   <td>Local transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_PRM
   </td>
   <td>Premium transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_MIX
   </td>
   <td>Mix (local with premium transfers) transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TAXI
   </td>
   <td>Taxi
   </td>
  </tr>
  <tr>
   <td>TNC_SINGLE
   </td>
   <td>Private TNC ride
   </td>
  </tr>
  <tr>
   <td>TNC_SHARED
   </td>
   <td>Shared TNC ride
   </td>
  </tr>
  <tr>
   <td>SCH_BUS
   </td>
   <td>School bus
   </td>
  </tr>
  <tr>
   <td>EBIKE
   </td>
   <td>E-bike
   </td>
  </tr>
  <tr>
   <td>ESCOOTER
   </td>
   <td>E-scooter
   </td>
  </tr>
</table>



#### Resident Model auto demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>SOVNOTRPDR_&lt;<time period>>
   </td>
   <td>Drive Alone Non-Transponder for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SOVTRPDR_&lt;<time period>>
   </td>
   <td>Drive Alone Transponder for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR2NOTRPDR_&lt;<time period>>
   </td>
   <td>Shared Ride 2 Non-Transponder for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR2TRPDR_&lt;<time period>>
   </td>
   <td>Shared Ride 2 Transponder for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR3NOTRPDR_&lt;<time period>>
   </td>
   <td>Shared Ride 3 Non-Transponder for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>SR3TRPDR_&lt;<time period>>
   </td>
   <td>Shared Ride 3 Transponder for &lt;<time period>>
   </td>
  </tr>
</table>



#### Resident Model transit demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_GENCOST__&lt;<time period>>
   </td>
   <td>Total generalized cost which includes perception factors from assignment
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_FIRSTWAIT__&lt;<time period>>
   </td>
   <td>actual wait time at initial boarding point
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_XFERWAIT__&lt;<time period>>
   </td>
   <td>actual wait time at all transfer boarding points
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_TOTALWAIT__&lt;<time period>>
   </td>
   <td>total actual wait time
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_FARE__&lt;<time period>>
   </td>
   <td>fare paid
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_XFERS__&lt;<time period>>
   </td>
   <td>number of transfers
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_ACCWALK__&lt;<time period>>
   </td>
   <td>access actual walk time prior to initial boarding
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_EGRWALK__&lt;<time period>>
   </td>
   <td>egress actual walk time after final alighting
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_TOTALWALK__&lt;<time period>>
   </td>
   <td>total actual walk time
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_TOTALIVTT__&lt;<time period>>
   </td>
   <td>Total actual in-vehicle travel time
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_DWELLTIME__&lt;<time period>>
   </td>
   <td>Total dwell time at stops
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_BUSIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on local bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_LRTIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on LRT mode
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class> _CMRIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on commuter rail mode
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class> _EXPIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on express bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_LTDEXPIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on premium bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;transit_class>_BRTIVTT__&lt;<time period>>
   </td>
   <td>actual in-vehicle travel time on  BRT mode
   </td>
  </tr>
  <tr>
   <td colspan="2" >*time period = EA, AM, MD, PM, EV
   </td>
  </tr>
  <tr>
   <td colspan="2" >transit_class = BUS, ALLPEN, PREM
   </td>
  </tr>
</table>



#### Resident Model non-motorized demand matrices


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>WALK_&lt;<time period>>
   </td>
   <td>Walk for &lt;<time period>>
   </td>
  </tr>
  <tr>
   <td>BIKE_&lt;<time period>>
   </td>
   <td>Bike for &lt;<time period>>
   </td>
  </tr>
</table>



### Visitor model outputs (.\visitor)

This directory contains outputs from the overnight visitor model.


<table>
  <tr>
   <td>File
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>[final_households.csv](#### Visitor Model household file (final_households.csv))
   </td>
   <td>Visitor model household file
   </td>
  </tr>
  <tr>
   <td>final_land_use.csv
   </td>
   <td>Visitor model land-use file
   </td>
  </tr>
  <tr>
   <td>[final_persons.csv](#### Visitor Model person file (final_persons.csv))
   </td>
   <td>Visitor model person file
   </td>
  </tr>
  <tr>
   <td>[final_tours.csv](#### Visitor Model tour file (final_tours.csv))
   </td>
   <td>Visitor model tour file
   </td>
  </tr>
  <tr>
   <td>[final_trips.csv](#### Visitor Model trip file (final_trips.csv))
   </td>
   <td>Visitor model trip file
   </td>
  </tr>
  <tr>
   <td>model_metadata.yaml
   </td>
   <td>Visitor model Datalake metadata file
   </td>
  </tr>
  <tr>
   <td>nmotVisitortrips_(period).omx
   </td>
   <td>Visitor model non-motorized trips by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>autoVisitortrips_(period).omx
   </td>
   <td>Visitor model auto trips by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
  <tr>
   <td>transVisitortrips_(period).omx
   </td>
   <td>Visitor model transit trips by period (EA, AM, MD, PM, EV)
   </td>
  </tr>
</table>



#### Visitor Model household file (final_households.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Home MGRA
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>poverty
   </td>
   <td>Poverty indicator utilized for social equity reports. Percentage value where value &lt;= 2 (200% of the Federal Poverty Level) indicates household is classified under poverty.
   </td>
  </tr>
</table>



#### Visitor Model person file (final_persons.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>home_zone_id
   </td>
   <td>Home MGRA
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
</table>



#### Visitor Model tour file (final_tours.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>tour_type
   </td>
   <td>Type of tour. String. "dining", "recreation", or "work"
   </td>
  </tr>
  <tr>
   <td>purpose_id
   </td>
   <td>Type of tour. 0: work, 1: "dining, 2: "recreation"
   </td>
  </tr>
  <tr>
   <td>visitor_travel_type
   </td>
   <td>Visitor purpose. String. "business" or "personal"
   </td>
  </tr>
  <tr>
   <td>tour_category
   </td>
   <td>Tour category. All tour categories in the visitor model are "non-mandatory"
   </td>
  </tr>
  <tr>
   <td>number_of_participants
   </td>
   <td>Number of participants on tour
   </td>
  </tr>
  <tr>
   <td>auto_available
   </td>
   <td>Auto availability indicator 0: not available, 1: available
   </td>
  </tr>
  <tr>
   <td>income
   </td>
   <td>Income 0 - 4
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Tour origin MGRA
   </td>
  </tr>
  <tr>
   <td>tour_num
   </td>
   <td>Sequential number of tour 1 to n where n is total number of tours
   </td>
  </tr>
  <tr>
   <td>tour_count
   </td>
   <td>Number of tours per person
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>start
   </td>
   <td>Half-hour time period of departure from tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>end
   </td>
   <td>Half-hour time period of arrival back at tour origin. Periods are number 1 through 48 where period 1 starts at 3:00 AM.
   </td>
  </tr>
  <tr>
   <td>duration
   </td>
   <td>Duration of the tour in number of half-hour periods, including all activity episodes and travel
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Tour primary destination MGRA
   </td>
  </tr>
  <tr>
   <td>destination_logsum
   </td>
   <td>Tour destination choice logsum
   </td>
  </tr>
  <tr>
   <td>tour_mode
   </td>
   <td>Tour mode
   </td>
  </tr>
  <tr>
   <td>mode_choice_logsum
   </td>
   <td>Tour mode choice logsum
   </td>
  </tr>
  <tr>
   <td>stop_frequency
   </td>
   <td>Number of stops on tour by direction. String. xout_yin where x is number of stops in the outbound direction and y is the number of stops in the inbound direction
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Primary purpose of a tour. String (recreation, dining, work)
   </td>
  </tr>
</table>



#### Visitor Model trip file ((final_trips.csv)


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>trip_id
   </td>
   <td>Trip ID
   </td>
  </tr>
  <tr>
   <td>person_id
   </td>
   <td>Person ID
   </td>
  </tr>
  <tr>
   <td>household_id
   </td>
   <td>Household ID
   </td>
  </tr>
  <tr>
   <td>primary_purpose
   </td>
   <td>Purpose at primary destination of tour. String. "dining", "recreation", or "work"
   </td>
  </tr>
  <tr>
   <td>trip_num
   </td>
   <td>Sequential number of trip on half-tour from 1 to 4
   </td>
  </tr>
  <tr>
   <td>outbound
   </td>
   <td>TRUE if outbound, else FALSE
   </td>
  </tr>
  <tr>
   <td>trip_count
   </td>
   <td>Number of trips in a tour
   </td>
  </tr>
  <tr>
   <td>destination
   </td>
   <td>Destination MGRA
   </td>
  </tr>
  <tr>
   <td>origin
   </td>
   <td>Origin MGRA
   </td>
  </tr>
  <tr>
   <td>tour_id
   </td>
   <td>Tour ID
   </td>
  </tr>
  <tr>
   <td>purpose
   </td>
   <td>Destination purpose. String. "dining", "recreation", or "work"
   </td>
  </tr>
  <tr>
   <td>destination_logsum
   </td>
   <td>Destination choice logsum
   </td>
  </tr>
  <tr>
   <td>depart
   </td>
   <td>Departure time period (1…48)
   </td>
  </tr>
  <tr>
   <td>trip_mode
   </td>
   <td>Trip mode (see trip mode table)
   </td>
  </tr>
  <tr>
   <td>trip_mode_choice_logsum
   </td>
   <td>Mode choice logsum for trip
   </td>
  </tr>
  <tr>
   <td>vot_da
   </td>
   <td>will drop
   </td>
  </tr>
  <tr>
   <td>vot_s2
   </td>
   <td>will drop
   </td>
  </tr>
  <tr>
   <td>vot_s3
   </td>
   <td>will drop
   </td>
  </tr>
  <tr>
   <td>parking_cost
   </td>
   <td>Parking costs at trip origin and destination, calculated as one-half of the costs at each end, with subsidies considered.
   </td>
  </tr>
  <tr>
   <td>tnc_single_wait_time
   </td>
   <td>Wait time for single pay TNC
   </td>
  </tr>
  <tr>
   <td>tnc_shared_wait_time
   </td>
   <td>Wait time for shared\pooled TNC
   </td>
  </tr>
  <tr>
   <td>taxi_wait_time
   </td>
   <td>Wait time for taxi
   </td>
  </tr>
  <tr>
   <td>cost_parking
   </td>
   <td>Cost of parking ($2023)
   </td>
  </tr>
  <tr>
   <td>cost_fare_drive
   </td>
   <td>Taxi/TNC fare for any trip or trip portion taken on these modes
   </td>
  </tr>
  <tr>
   <td>distance_walk
   </td>
   <td>Distance walked on a trip (including access/egress for transit modes)
   </td>
  </tr>
  <tr>
   <td>time_mm
   </td>
   <td>Micromobility time
   </td>
  </tr>
  <tr>
   <td>distance_mm
   </td>
   <td>Micromobility distance
   </td>
  </tr>
  <tr>
   <td>cost_fare_mm
   </td>
   <td>Micromobility cost ($2023)
   </td>
  </tr>
  <tr>
   <td>distance_bike
   </td>
   <td>Bike distance
   </td>
  </tr>
  <tr>
   <td>time_wait_drive
   </td>
   <td>Ridehail/Taxi wait times for a trip
   </td>
  </tr>
  <tr>
   <td>trip_period
   </td>
   <td>A string indicating the skim period for the trip (“EA”,”AM”,”MD”,”PM’,”EV”)
   </td>
  </tr>
  <tr>
   <td>tour_participants
   </td>
   <td>Number of tour participants
   </td>
  </tr>
  <tr>
   <td>distance_total
   </td>
   <td>Trip distance
   </td>
  </tr>
  <tr>
   <td>cost_operating_drive
   </td>
   <td>Auto operating cost ($2023)
   </td>
  </tr>
  <tr>
   <td>weight_trip
   </td>
   <td>Trip weight defined as the ratio of number of particpants on a  trip to the assumed occupancy rate of a mode (SHARED2,3)
   </td>
  </tr>
  <tr>
   <td>weight_person_trip
   </td>
   <td>Person trip weigth defined as the ratio of the number of particpants on a trip to sample rate of the model run
   </td>
  </tr>
  <tr>
   <td>vot
   </td>
   <td>Value of time in dollars per hour ($2023)
   </td>
  </tr>
  <tr>
   <td>inbound
   </td>
   <td>TRUE if trip is in outbound direction, else FALSE
   </td>
  </tr>
  <tr>
   <td>time_drive
   </td>
   <td>Auto time
   </td>
  </tr>
  <tr>
   <td>distance_drive
   </td>
   <td>Auto distance
   </td>
  </tr>
  <tr>
   <td>cost_toll_drive
   </td>
   <td>Auto toll cost ($2023)
   </td>
  </tr>
  <tr>
   <td>time_transit_in_vehicle
   </td>
   <td>Transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_rapid_transit_in_vehicle
   </td>
   <td>Rapid transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_express_bus_transit_in_vehicle
   </td>
   <td>Express bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_local_bus_transit_in_vehicle
   </td>
   <td>Local bus in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_light_rail_transit_in_vehicle
   </td>
   <td>Light rail transit in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_commuter_rail_transit_in_vehicle
   </td>
   <td>Commuter rail in-vehicle time
   </td>
  </tr>
  <tr>
   <td>time_transit_initial_wait
   </td>
   <td>Transit initial-wait time
   </td>
  </tr>
  <tr>
   <td>cost_fare_transit
   </td>
   <td>Transit fare ($2023)
   </td>
  </tr>
  <tr>
   <td>transfers_transit
   </td>
   <td>Number of transfers
   </td>
  </tr>
  <tr>
   <td>time_bike
   </td>
   <td>Bike time
   </td>
  </tr>
  <tr>
   <td>time_walk
   </td>
   <td>Walk mode time
   </td>
  </tr>
  <tr>
   <td>cost_total
   </td>
   <td>total cost of a trip (sum of auto operating, toll, transit fare)
   </td>
  </tr>
  <tr>
   <td>time_total
   </td>
   <td>Total travel time (including iIVT and access/egress and wait times for all modes)
   </td>
  </tr>
  <tr>
   <td>value_of_time_category_id
   </td>
   <td>Value of time bin. 1: Low, 2: Medium, 3: High
   </td>
  </tr>
  <tr>
   <td>origin_micro_prm_dist
   </td>
   <td>Distance from trip origin MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>dest_micro_prm_dist
   </td>
   <td>Distance from trip destination MGRA to closest premium transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_orig
   </td>
   <td>Distance from trip origin MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_dest
   </td>
   <td>Distance from trip destination MGRA to closest local transit stop by microtransit
   </td>
  </tr>
  <tr>
   <td>microtransit_available
   </td>
   <td>TRUE if microtransit is available for trip, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_orig
   </td>
   <td>True if Neghoborhood Electric Vehicle is available at origin
   </td>
  </tr>
  <tr>
   <td>nev_dest
   </td>
   <td>True if Neghoborhood Electric Vehicle is available at destination
   </td>
  </tr>
  <tr>
   <td>nev_available
   </td>
   <td>TRUE if neighborhood electric vehicle is available, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_out
   </td>
   <td>TRUE if microtransit is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>nev_access_available_out
   </td>
   <td>TRUE if neighborhood electric vehicle is available from the origin, else FALSE
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_out
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_out
   </td>
   <td>Availability of NEV egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_access_available_in
   </td>
   <td>Availability of microtransit access in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_access_available_in
   </td>
   <td>Availability of NEV egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>microtransit_egress_available_in
   </td>
   <td>Availability of microtransit egress in the inbound direction
   </td>
  </tr>
  <tr>
   <td>nev_egress_available_in
   </td>
   <td>Availability of microtransit egress in the outbound direction
   </td>
  </tr>
  <tr>
   <td>sample_rate
   </td>
   <td>Sample rate
   </td>
  </tr>
  <tr>
   <td>otaz
   </td>
   <td>Origin TAZ
   </td>
  </tr>
  <tr>
   <td>dtaz
   </td>
   <td>Destination TAZ
   </td>
  </tr>
</table>



#### Visitor model’s tour mode choice definitions


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>DRIVEALONE
   </td>
   <td>Drive alone
   </td>
  </tr>
  <tr>
   <td>SHARED2
   </td>
   <td>Shared ride with 2 participants
   </td>
  </tr>
  <tr>
   <td>SHARED3
   </td>
   <td>Shared ride with 3+ participants
   </td>
  </tr>
  <tr>
   <td>WALK
   </td>
   <td>Walk
   </td>
  </tr>
  <tr>
   <td>BIKE
   </td>
   <td>Bike
   </td>
  </tr>
  <tr>
   <td>WALK_LOC
   </td>
   <td>Local transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_PRM
   </td>
   <td>Premium transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_MIX
   </td>
   <td>Mix (local with premium transfers) transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>TAXI
   </td>
   <td>Taxi
   </td>
  </tr>
  <tr>
   <td>TNC_SINGLE
   </td>
   <td>Private TNC ride
   </td>
  </tr>
  <tr>
   <td>TNC_SHARED
   </td>
   <td>Shared TNC ride
   </td>
  </tr>
</table>



### Trip Mode Definitions


<table>
  <tr>
   <td>Field
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>DRIVEALONE
   </td>
   <td>Drive alone
   </td>
  </tr>
  <tr>
   <td>SHARED2
   </td>
   <td>Shared ride with 2 participants
   </td>
  </tr>
  <tr>
   <td>SHARED3
   </td>
   <td>Shared ride with 3+ participants
   </td>
  </tr>
  <tr>
   <td>WALK
   </td>
   <td>Walk
   </td>
  </tr>
  <tr>
   <td>BIKE
   </td>
   <td>Bike
   </td>
  </tr>
  <tr>
   <td>WALK_LOC
   </td>
   <td>Local transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_PRM
   </td>
   <td>Premium transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>WALK_MIX
   </td>
   <td>Mix (local with premium transfers) transit with walk access/egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_LOC
   </td>
   <td>Local transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_PRM
   </td>
   <td>Premium transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>PNR_MIX
   </td>
   <td>Mix (local with premium transfers) transit with Park&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_LOC
   </td>
   <td>Local transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_PRM
   </td>
   <td>Premium transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>KNR_MIX
   </td>
   <td>Mix (local with premium transfers) transit with Kiss&ride access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_LOC
   </td>
   <td>Local transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_PRM
   </td>
   <td>Premium transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TNC_MIX
   </td>
   <td>Mix (local with premium transfers) transit with TNC access or egress mode
   </td>
  </tr>
  <tr>
   <td>TAXI
   </td>
   <td>Taxi
   </td>
  </tr>
  <tr>
   <td>TNC_SINGLE
   </td>
   <td>Private TNC ride
   </td>
  </tr>
  <tr>
   <td>TNC_SHARED
   </td>
   <td>Shared TNC ride
   </td>
  </tr>
  <tr>
   <td>SCH_BUS
   </td>
   <td>School bus
   </td>
  </tr>
  <tr>
   <td>EBIKE
   </td>
   <td>E-bike
   </td>
  </tr>
  <tr>
   <td>ESCOOTER
   </td>
   <td>E-scooter
   </td>
  </tr>
</table>

