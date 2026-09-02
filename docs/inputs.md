# ABM3 Model Inputs

The main inputs to ABM3 include the [transportation network](networks.md), land-use data, synthetic population data, parameters files, and model specifications. Outputs include a set of files that describe travel decisions made by all travel markets considered by the model (residents, overnight visitors, airport ground access trips, commercial vehicles and trucks, Mexico residents traveling in San Diego County, and travel made by all other non-residents into and through San Diego County).

## Model Inputs

The table below contains brief descriptions of the input files required to execute the SANDAG ABM3. 

A separate [Networks](networks.md) page exists for all network-related ABM3 inputs. 

*Note: Click on file name for additional details.*

<table>
    <tr>
        <td><strong>File Name</strong></td>
        <td><strong>Description</strong></td>
        <td><strong>File Format</strong></td>
        <td><strong>Source</strong></td>
    </tr>
    <tr>
        <td><strong><a href=#land-use>Land Use</a></strong></td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td><a href=#master-geographic-reference-areas-data>mgra15_based_input{year}.csv</a></td>
        <td>MGRA land use, demographics, and employment</td>
        <td>CSV</td>
        <td>Land Use Modelers, Transportation Modelers, and GIS</td>
    </tr>
    <tr>
        <td><strong><a href=#synthetic-population>Synthetic Population</a></strong></td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td><a href=#population-synthesizer-household-data>households.csv</a></td>
        <td>Synthetic households</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#population-synthesizer-person-data>persons.csv</a></td>
        <td>Synthetic persons</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><strong><a href=#crossborder>Crossborder Model</a></strong></td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>closest_maz_to_external_tazs.csv</td>
        <td>Crossborder Model list of closest MGRA to each external TAZ</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-inbound-stop-duration-distribution>crossBorder_inboundStopDuration.csv</a></td>
        <td>Crossborder Model time-of-day offsets for inbound stops</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-outbound-stop-duration-distribution>crossBorder_outboundStopDuration.csv</a></td>
        <td>Crossborder Model time-of-day offsets for outbound stops</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-point-of-entry-wait-time>crossBorder_pointOfEntryWaitTime.csv</a></td>
        <td>Crossborder Model wait times at border crossing locations</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-stop-frequency>crossBorder_stopFrequency.csv</a></td>
        <td>Crossborder Model stop frequency data</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-stop-purpose-distribution>crossBorder_stopPurpose.csv</a></td>
        <td>Crossborder Model stop purpose distribution</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-supercolonia>crossBorder_supercolonia.csv</a></td>
        <td>Crossborder Model distance from Colonias to border crossing locations</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td><a href=#crossborder-model-tour-entry-and-return-distribution>crossBorder_tourEntryAndReturn.csv</a></td>
        <td>Crossborder Model tour entry and return time-of-day distribution</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>crossBorder_tourPurpose_control.csv</td>
        <td>Crossborder Model tour purpose reassignment probability lookup table</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>crossBorder_tourPurpose_nonSENTRI.csv</td>
        <td>Crossborder Model tour purpose distribution for Non-SENTRI tours</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>crossBorder_tourPurpose_SENTRI.csv</td>
        <td>Crossborder Model tour purpose distribution for SENTRI tours</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>**External Models**</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>[externalExternalTripsByYear.csv](#external_trip) &lt;i&gt; (raw inputs have these by year) &lt;i&gt;</td>
        <td>External origin-destination station trip matrix</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[externalInternalControlTotalsByYear.csv](#external_internal) &lt;i&gt; (raw inputs have these by year) &lt;i&gt;</td>
        <td>External Internal station control totals read by GISDK</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[internalExternal_tourTOD.csv](#internal_external_tod)</td>
        <td>Internal-External Model tour time-of-day frequency distribution</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[resident_ie_size_term.csv](#resident_ie_size_term)</td>
        <td></td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>**Commercial Vehicle Model**</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>land_use(output from preprocessing step)</td>
        <td>MGRA based land use file</td>
        <td>CSV</td>
        <td></td>
    </tr>
    <tr>
        <td>percent_of_establishments_by_luz_size_emp_cat.xlsx</td>
        <td>Percent of establishments in LUZ that belong in each size category by industry sector</td>
        <td>Excel Workbook</td>
        <td></td>
    </tr>
    <tr>
        <td>CVM\SynthEstablishments.csv</td>
        <td>Output from CVM establishment synthesis, similar description as previous part</td>
        <td>CSV</td>
        <td></td>
    </tr>
    <tr>
        <td>CVM\MGRAEmpByEstSize.csv</td>
        <td>MGRA Based synthetically generated establishments. Used for disgnostic purposes, not for simulation</td>
        <td>CSV</td>
        <td></td>
    </tr>
    <tr>
        <td>CVM\SummaryEstablishments.csv</td>
        <td>Contains information about synthetically generated establishments to be used as inputs to the commercial vehicle model</td>
        <td>CSV</td>
        <td></td>
    </tr>
    <tr>
        <td>**Heavy Truck Model (HTM)**</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>HTM\inputs_sandag_htm_&lt;Scenario_Year&gt;.xlsx</td>
        <td>Contains all the required inputs ( in different sheets) for the Heavy Truck Model</td>
        <td>Excel Workbook</td>
        <td></td>
    </tr>
    <tr>
        <td>HTM\FAF5_BaseAndFutureYears_Oct27_2023.csv</td>
        <td>FAF5 Data (filtered) containing FAF flows for required years</td>
        <td>CSV</td>
        <td></td>
    </tr>
    <tr>
        <td>**Other**</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>[bikeTazLogsum.csv](#bike_taz_logsum)</td>
        <td>Bike TAZ logsum</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[bikeMgraLogsum.csv](#bike_mgra_logsum)</td>
        <td>Bike MGRA logsum</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[parametersByYears.csv](#parametersbyyearscsv)</td>
        <td>Parameters by scenario years. Includes AOC, aiport enplanements, cross-border tours, cross-border sentri share.</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>[filesByYears.csv](#filesbyyearscsv)</td>
        <td>File names by scenario years.</td>
        <td>CSV</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>trip_XX.omx</td>
        <td>Warm start trip table; XX is the TOD (EA, AM, MD, PM, and EV)</td>
        <td>OMX</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>zone_term.csv</td>
        <td>TAZ terminal times</td>
        <td>Space Delimited Text File</td>
        <td>Transportation Modelers</td>
    </tr>
    <tr>
        <td>mgra15.shp</td>
        <td></td>
        <td>SHP</td>
        <td></td>
    </tr>
    <tr>
        <td>taz15.shp</td>
        <td></td>
        <td>SHP</td>
        <td></td>
    </tr>
    <tr>
        <td>all_vol_dfs.csv [to be updated]</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>all_wait_times.csv [to be updated]</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td>specialEvents_() [to be updated]</td>
        <td></td>
        <td></td>
        <td></td>
    </tr>
</table>

## Land Use

### Master Geographic Reference Areas Data
`mgra15_based_input{year}.csv`

<table>
    <tr>
        <th>Column name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>mgra</td>
        <td>MGRA ID number</td>
    </tr>
    <tr>
        <td>taz</td>
        <td>TAZ ID number</td>
    </tr>
    <tr>
        <td>luz_id</td>
        <td>Land Use Zone ID number</td>
    </tr>
    <tr>
        <td>pop</td>
        <td>Total population</td>
    </tr>
    <tr>
        <td>hhp</td>
        <td>Total household population (exclude gq pop)</td>
    </tr>
    <tr>
        <td>hs</td>
        <td>Housing structures</td>
    </tr>
    <tr>
        <td>hs_sf</td>
        <td>Single family housing structures</td>
    </tr>
    <tr>
        <td>hs_mf</td>
        <td>Multi-family housing structures</td>
    </tr>
    <tr>
        <td>hs_mh</td>
        <td>Mobile home housing structures</td>
    </tr>
    <tr>
        <td>hh</td>
        <td>Total number of households</td>
    </tr>
    <tr>
        <td>hh_sf</td>
        <td>Number of single family households</td>
    </tr>
    <tr>
        <td>hh_mf</td>
        <td>Number of multi-family households</td>
    </tr>
    <tr>
        <td>hh_mh</td>
        <td>Number of mobile home households</td>
    </tr>
    <tr>
        <td>hhs</td>
        <td>Average household size</td>
    </tr>
    <tr>
        <td>gq_civ</td>
        <td>Civilian Group Quarterss</td>
    </tr>
    <tr>
        <td>gq_mil</td>
        <td>Military Group Quarters</td>
    </tr>
    <tr>
        <td>i1</td>
        <td>Number of households with income less than $15,000 ($2022)</td>
    </tr>
    <tr>
        <td>i2</td>
        <td>Number of households with income $15,000-$29,999 ($2022)</td>
    </tr>
    <tr>
        <td>i3</td>
        <td>Number of households with income $30,000-$44,999 ($2022)</td>
    </tr>
    <tr>
        <td>i4</td>
        <td>Number of households with income $45,000-$59,999 ($2022)</td>
    </tr>
    <tr>
        <td>i5</td>
        <td>Number of households with income $60,000-$74,999 ($2022)</td>
    </tr>
    <tr>
        <td>i6</td>
        <td>Number of households with income $75,000-$99,999 ($2022)</td>
    </tr>
    <tr>
        <td>i7</td>
        <td>Number of households with income $100,000-$124,999 ($2022)</td>
    </tr>
    <tr>
        <td>i8</td>
        <td>Number of households with income $125,000-$149,999 ($2022)</td>
    </tr>
    <tr>
        <td>i9</td>
        <td>Number of households with income $150,000-$199,999 ($2022)</td>
    </tr>
    <tr>
        <td>i10</td>
        <td>Number of households with income $200,000 or more ($2022)</td>
    </tr>
    <tr>
        <td>emp_gov</td>
        <td>Government employment</td>
    </tr>
    <tr>
        <td>emp_mil</td>
        <td>Military employment</td>
    </tr>
    <tr>
        <td>emp_ag_min</td>
        <td>Agriculture and mining employment (NAICS:11,21)</td>
    </tr>
    <tr>
        <td>emp_bus_svcs</td>
        <td>Professional and Business Services employment (NAICS:51,54,56)</td>
    </tr>
    <tr>
        <td>emp_fin_res_mgm</td>
        <td>Financial and resource management employment (NAICS:52,53,55)</td>
    </tr>
    <tr>
        <td>emp_educ</td>
        <td>Education services employment (NAICS:61)</td>
    </tr>
    <tr>
        <td>emp_hlth</td>
        <td>Health services employment (NAICS:62)</td>
    </tr>
    <tr>
        <td>emp_ret</td>
        <td>Retail services employment (NAICS:44,45)</td>
    </tr>
    <tr>
        <td>emp_trn_wrh</td>
        <td>Transportation and Warehousing employment (NAICS:48,49)</td>
    </tr>
    <tr>
        <td>emp_con</td>
        <td>Construction employment (NAICS:23)</td>
    </tr>
    <tr>
        <td>emp_utl</td>
        <td>Utilities office support employment (NAICS:22)</td>
    </tr>
    <tr>
        <td>emp_mnf</td>
        <td>Manufacturing employment (NAICS:31,32,33)</td>
    </tr>
    <tr>
        <td>emp_whl</td>
        <td>Wholesale employment (NAICS:42)</td>
    </tr>
    <tr>
        <td>emp_ent</td>
        <td>Entertainment services employment (NAICS:71)</td>
    </tr>
    <tr>
        <td>emp_accm</td>
        <td>Hotel and accomodation services (NAICS:721)</td>
    </tr>
    <tr>
        <td>emp_food</td>
        <td>Food services employment (NAICS:722)</td>
    </tr>
    <tr>
        <td>emp_oth</td>
        <td>Other employment (NAICS:81)</td>
    </tr>
    <tr>
        <td>emp_non_ws_wfh</td>
        <td>Non-wage and salary work from home employments</td>
    </tr>
    <tr>
        <td>emp_non_ws_oth</td>
        <td>Non-wage and salary other employments</td>
    </tr>
    <tr>
        <td>emp_total</td>
        <td>Total employment</td>
    </tr>
    <tr>
        <td>pseudomsa</td>
        <td>
            Pseudo MSA<br/>
            1: Downtown<br/>
            2: Central<br/>
            3: North City<br/>
            4: South Suburban<br/>
            5: East Suburban<br/>
            6: North County West<br/>
            7: North County East<br/>
            8: East County
        </td>
    </tr>
    <tr>
        <td>zip09</td>
        <td>2009 Zip Code</td>
    </tr>
    <tr>
        <td>enrollgradekto8</td>
        <td>Grade School K-8 enrollment</td>
    </tr>
    <tr>
        <td>enrollgrade9to12</td>
        <td>Grade School 9-12 enrollment</td>
    </tr>
    <tr>
        <td>collegeenroll</td>
        <td>Major College enrollment</td>
    </tr>
    <tr>
        <td>othercollegeenroll</td>
        <td>Other College enrollment</td>
    </tr>
    <tr>
        <td>hotelroomtotal</td>
        <td>Total number of hotel rooms</td>
    </tr>
    <tr>
        <td>parkactive</td>
        <td>Acres of Active Park</td>
    </tr>
    <tr>
        <td>openspaceparkpreserve</td>
        <td>Acres of Open Park or Preserve</td>
    </tr>
    <tr>
        <td>beachactive</td>
        <td>Acres of Active Beach</td>
    </tr>
    <tr>
        <td>district27</td>
        <td>27 district system</td>
    </tr>
    <tr>
        <td>milestocoast</td>
        <td>Distance (miles) to the nearest coast</td>
    </tr>
    <tr>
        <td>acres</td>
        <td>Total acres in the MGRA</td>
    </tr>
    <tr>
        <td>land_acres</td>
        <td>Acres of land in the MGRA</td>
    </tr>
    <tr>
        <td>effective_acres</td>
        <td>Effective acres in the MGRA</td>
    </tr>
    <tr>
        <td>truckregiontype</td>
        <td>Truck region type</td>
    </tr>
    <tr>
        <td>exp_hourly</td>
        <td>Hourly parking expense</td>
    </tr>
    <tr>
        <td>exp_daily</td>
        <td>Daily parking expense</td>
    </tr>
    <tr>
        <td>exp_monthly</td>
        <td>Monthly parking expense</td>
    </tr>
    <tr>
        <td>parking_type</td>
        <td>
            1 = parking constrained area: has cluster_id and district_id<br/>
            2 = buffer around parking constrained area which is used to include free spaces to average into parking cost calculation: has district_id but no cluster_id<br/>
            3 = no parking cost: Has neither cluster_id nor district_id
        </td>
    </tr>
    <tr>
        <td>parking_spaces</td>
        <td>Estimated parking spaces in MGRA</td>
    </tr>
    <tr>
        <td>ech_dist</td>
        <td>Elementary school district</td>
    </tr>
    <tr>
        <td>hch_dist</td>
        <td>High school district</td>
    </tr>
    <tr>
        <td>remoteAVParking</td>
        <td>
            Remote AV parking available at MGRA:<br/>
            0 = Not available<br/>
            1 = Available
        </td>
    </tr>
    <tr>
        <td>refueling_stations</td>
        <td>Number of refueling stations at MGRA</td>
    </tr>
    <tr>
        <td>MicroAccessTime</td>
        <td>Shared Micro-mobility (e-scooter/e-bike) access time (mins)</td>
    </tr>
    <tr>
        <td>microtransit</td>
        <td>The microtransit service area ID [0 means no service]</td>
    </tr>
    <tr>
        <td>nev</td>
        <td>Neighborhood Electric Vehicle (NEV) service area ID [0 means no service]</td>
    </tr>
    <tr>
        <td>totint</td>
        <td>Total intersections within 0.65 miles of the MGRA</td>
    </tr>
    <tr>
        <td>duden</td>
        <td>Dwelling units per acre within 0.65 miles of the MGRA</td>
    </tr>
    <tr>
        <td>empden</td>
        <td>Jobs per acre within 0.65 miles of the MGRA</td>
    </tr>
    <tr>
        <td>popden</td>
        <td>Population per acre within 0.65 miles of the MGRA</td>
    </tr>
    <tr>
        <td>retempden</td>
        <td>Retail jobs per acre within 0.65 miles of the MGRA</td>
    </tr>
    <tr>
        <td>totintbin</td>
        <td>Total intersection bin</td>
    </tr>
    <tr>
        <td>empdenbin</td>
        <td>Employment density bin</td>
    </tr>
    <tr>
        <td>dudenbin</td>
        <td>Dwelling unit density bin</td>
    </tr>
    <tr>
        <td>PopEmpDenPerMi</td>
        <td>Population and employment density per mile within 0.65 miles of the MGRA</td>
    </tr>
</table>

## Synthetic Population

### Population Synthesizer Household Data
`households.csv`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>hhid</td>
        <td>Unique Household ID</td>
    </tr>
    <tr>
        <td>household_serial_no</td>
        <td>Household serial number</td>
    </tr>
    <tr>
        <td>taz</td>
        <td>TAZ of household</td>
    </tr>
    <tr>
        <td>mgra</td>
        <td>MGRA of household</td>
    </tr>
    <tr>
        <td>hinccat1</td>
        <td>
            Household income category:<br/>
            1 = &lt;$30k<br/>
            2 = $30-60k<br/>
            3 = $60-100k<br/>
            4 = $100-150k<br/>
            5 = $150k+
        </td>
    </tr>
    <tr>
        <td>hinc</td>
        <td>Household income</td>
    </tr>
    <tr>
        <td>num_workers</td>
        <td>Number of workers in household</td>
    </tr>
    <tr>
        <td>veh</td>
        <td>Number of vehicles in household</td>
    </tr>
    <tr>
        <td>persons</td>
        <td>Number of persons in household</td>
    </tr>
    <tr>
        <td>hht</td>
        <td>
            Household/family type:<br/>
            0 = Not in universe (vacant or GQ)<br/>
            1 = Family household: married-couple<br/>
            2 = Family household: male householder, no wife present<br/>
            3 = Family household: female householder, no husband present<br/>
            4 = Nonfamily household: male householder, living alone<br/>
            5 = Nonfamily household: male householder, not living alone<br/>
            6 = Nonfamily household: female householder, living alone<br/>
            7 = Nonfamily household: female householder, not living alone
        </td>
    </tr>
    <tr>
        <td>bldgsz</td>
        <td>
            Units in Structure:<br/>
            0 = N/A (Group Quarter)
            1 = Mobile home or trailer<br/>
            2 = One-family house detached<br/>
            3 = One-family house attached<br/>
            4 = 2 Apartments<br/>
            5 = 3-4 Apartments<br/>
            6 = 5-9 Apartments<br/>
            7 = 10-19 Apartments<br/>
            8 = 20-49 Apartments<br/>
            9 = 50 or more apartments<br/>
            10 = Boat, RV, van, etc.
        </td>
    </tr>
    <tr>
        <td>unittype</td>
        <td>
            Household unit type:<br/>
            0 = Non-GQ Household<br/>
            1 = GQ Household
        </td>
    </tr>
    <tr>
        <td>version</td>
        <td>Synthetic population run version. Presently set to 0.</td>
    </tr>
    <tr>
        <td>poverty</td>
        <td>Poverty indicator utilized for social equity reports. Percentage value where value &lt;= 2 (200% of the <a href="https://aspe.hhs.gov/2020-poverty-guidelines">Federal Poverty Level</a>) indicates household is classified under poverty.</td>
    </tr>
</table>

### Population Synthesizer Person Data
`persons.csv`

<table>
    <tr>
        <th>Column Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>hhid</td>
        <td>Household ID</td>
    </tr>
    <tr>
        <td>perid</td>
        <td>Person ID</td>
    </tr>
    <tr>
        <td>household_serial_no</td>
        <td>Household serial number</td>
    </tr>
    <tr>
        <td>pnum</td>
        <td>Person Number</td>
    </tr>
    <tr>
        <td>age</td>
        <td>Age of person</td>
    </tr>
    <tr>
        <td>sex</td>
        <td>
            Gender of person<br/>
            1 = Male<br/>
            2 = Female
        </td>
    </tr>
    <tr>
        <td>miltary</td>
        <td>
            Military status of person:<br/>
            0 = N/A Less than 17 Years Old<br/>
            1 = Yes, Now on Active Duty
        </td>
    </tr>
    <tr>
        <td>pemploy</td>
        <td>
            Employment status of person:<br/>
            1 = Employed Full-Time<br/>
            2 = Employed Part-Time<br/>
            3 = Unemployed or Not in Labor Force<br/>
            4 = Less than 16 Years Old
        </td>
    </tr>
    <tr>
        <td>pstudent</td>
        <td>
            Student status of person:<br/>
            1 = Pre K-12<br/>
            2 = College Undergrad+Grad and Prof. School<br/>
            3 = Not Attending School
        </td>
    </tr>
    <tr>
        <td>ptype</td>
        <td>
            Person type:<br/>
            1 = Full-time Worker<br/>
            2 = Part-time Worker<br/>
            3 = College Student<br/>
            4 = Non-working Adult<br/>
            5 = Non-working Senior<br/>
            6 = Driving Age Student<br/>
            7 = Non-driving Student<br/>
            8 = Pre-school
        </td>
    </tr>
    <tr>
        <td>educ</td>
        <td>
            Educational attainment:<br/>
            1 = No schooling completed<br/>
            9 = High school graduate<br/>
            13 = Bachelor's degree
        </td>
    </tr>
    <tr>
        <td>grade</td>
        <td>
            School grade of person:<br/>
            0 = N/A (not attending school)<br/>
            2 = K to grade 8<br/>
            5 = Grade 9 to grade 12<br/>
            6 = College undergraduate
        </td>
    </tr>
    <tr>
        <td>occen5</td>
        <td>
            Occupation:<br/>
            0 = Not in universe (Under 16 years or LAST-WRK = 2)<br/>
            1..997 = Legal census occupation code
        </td>
    </tr>
    <tr>
        <td>occsoc5</td>
        <td>Detailed occupation codes defined by the Bureau of Labor Statistics</td>
    </tr>
    <tr>
        <td>indcen</td>
        <td>
            Industry code.<br/>
            0 = default<br/>
            9970 = NAICS2 is MIL
        </td>
    </tr>
    <tr>
        <td>weeks</td>
        <td>
            Weeks worked during past 12 months<br/>
            0 .N/A (less than 16 years old/did not work during the past 12 .months)<br/>
            1 .50 to 52 weeks worked during past 12 months<br/>
            2 .48 to 49 weeks worked during past 12 months<br/>
            3 .40 to 47 weeks worked during past 12 months<br/>
            4 .27 to 39 weeks worked during past 12 month<br/>
            5 .14 to 26 weeks worked during past 12 months<br/>
            6 .13 weeks or less worked during past 12 months
        </td>
    </tr>
    <tr>
        <td>hours</td>
        <td>
            Hours worked per week past 12 months<br/>
            0 .N/A (less than 16 years old/did not work during the past .12 months)<br/>
            1..98 .1 to 98 usual hours<br/>
            99 .99 or more usual hours
        </td>
    </tr>
    <tr>
        <td>rac1p</td>
        <td>
            Race:<br/>
            1 = White alone<br/>
            2 = Black or African American alone<br/>
            3 = American Indian alone<br/>
            4 = Alaska Native alone<br/>
            5 = American Indian and Alaska Native tribes specified; or .American Indian or Alaska Native, not specified and no other .races<br/>
            6 = Asian alone<br/>
            7 = Native Hawaiian and Other Pacific Islander alone<br/>
            8 = Some Other Race alone<br/>
            9 =Two or More Races
        </td>
    </tr>
    <tr>
        <td>hisp</td>
        <td>
            Hispanic origin:<br/>
            1 = Not Hispanic<br/>
            2 = Hispanic
        </td>
    </tr>
    <tr>
        <td>version</td>
        <td>Synthetic population run version. Presently set to 0.</td>
    </tr>
    <tr>
        <td>naics2_original_code</td>
        <td>
            2 digit North American Industry Classification System (NAICS)<br/>
            11 = Agriculture, Forestry, Fishing and Hunting<br/>
            21 = Mining, Quarrying, and Oil and Gas Extraction<br/>
            22 = Utilities<br/>
            23 = Construction<br/>
            31 = Manufacturing<br/>
            32 = Wood Product Manufacturing<br/>
            33 = Primary Metal Manufacturing<br/>
            42 = Wholesale Trade<br/>
            44 = Retail Trade<br/>
            45 = General Merchandise Retailers<br/>
            48 = Transportation and Warehousing<br/>
            49 = Postal Service<br/>
            51 = Information<br/>
            52 = Finance and Insurance<br/>
            53 = Real Estate and Rental and Leasing<br/>
            54 = Professional, Scientific, and Technical Services<br/>
            55 = Management of Companies and Enterprises<br/>
            56 = Administrative and Support and Waste Management and Remediation Services<br/>
            61 = Educational Services<br/>
            62 = Health Care and Social Assistance<br/>
            71 = Arts, Entertainment, and Recreation<br/>
            721 = Accommodation<br/>
            722 = Food Services and Drinking Places<br/>
            81 = Other Services (except Public Administration)<br/>
            92 =  Public Administration
        </td>
    </tr>
    <tr>
        <td>soc2</td>
        <td>2 digit Standard Occupational Classification</td>
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

### `CVM/SYNTHESTABLISHMENTS.CSV`

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

<a id ="cvm"></a>

### `CVM/MGRAEMPBYESTSIZE.CSV`

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

### Crossborder Model Tour Entry and Return Distribution
`crossBorder_tourEntryAndReturn.csv`

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

### Crossborder Model Supercolonia
`crossBorder_supercolonia.csv`

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
    <tr>
        <td>Distance_poe3</td>
        <td>Distance from colonia to point of entry 3 (Otay Mesa East)</td>
    </tr>
    <tr>
        <td>Distance_poe4</td>
        <td>Distance from colonia to point of entry 4 (Jacumba)</td>
    </tr>
</table>

### Crossborder Model Point of Entry Wait Time
`crossBorder_pointOfEntryWaitTime.csv`

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
        <td>PedestrianWait</td>
        <td>Pedestrian wait time in minutes</td>
    </tr>
    <tr>
        <td>ReadyWait</td>
        <td>Ready Lane wait time in minutes</td>
    </tr>
    <tr>
        <td>SENTRIWait</td>
        <td>SENTRI Lane wait time in minutes</td>
    </tr>
    <tr>
        <td>StandardWait</td>
        <td>Standard Lane wait time in minutes</td>
    </tr>
</table>

### Crossborder Model Stop Frequency
`crossBorder_StopFrequency.csv`

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

### Crossborder Model Stop Purpose Distribution
`crossBorder_stopPurpose.csv`

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

### Crossborder Model Outbound Stop Duration Distribution
`crossBorder_outboundStopDuration.csv`

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

### Crossborder Model Inbound Stop Duration Distribution
`crossBorder_inboundStopDuration.csv`

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
#### `HTM/INPUTS_SANDAG_HTM_<SCENARIO_YEAR>.XLSX`

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

#### `HTM/FAF5_BaseAndFutureYears_Oct27_2023.CSV`

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

<a id="bike_taz_logsum"></a>

### Bike TAZ Logsum
#### `bikeTazLogsum.csv`

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
#### `bikeMgraLogsum.csv`

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
#### `ZONE_TERM.CSV`

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
