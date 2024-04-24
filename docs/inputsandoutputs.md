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

//TODO
List here

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
| final_(airport)households.csv |  | Household file for airport (cbx, san) | 
| final_(airport)land_use.csv |  | Land-use file for airport (cbx, san) | 
| final_(airport)persons.csv |  | Persons file for airport (cbx, san) | 
| final_(airport)tours.csv |  | Tour file for airport (cbx, san) | 
| final_(airport)trips.csv |  | Trip file for airport (cbx, san) | 
| model_metadata.yaml |  | Datalake metadata file | 
| nmotairporttrips.(airport)_(period).omx |  | Non-motorized trip table for airport (CBX, SAN) by period (EA, AM, MD, PM, EV) | 

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
| final_households.csv | Household file for Crossborder Model | 
| final_land_use.csv | Land-use file for Crossborder Model | 
| final_persons.csv | Persons file for Crossborder Model | 
| final_tours.csv | Tour file for Crossborder Model | 
| final_trips.csv | Tour file for Crossborder Model | 
| model_metadata.yaml | Model run meta data for use in Datalake storage and reporting | 
| nmCrossborderTrips_AM.omx | Non-motorized trip table for Crossborder Model by period (EA, AM, MD, PM, EV) | 
| othrCrossborderTrips_AM.omx | Other trip table for Crossborder Model by period (EA, AM, MD, PM, EV) | 

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
| [final_households.csv](Resident Model household file (final_households.csv)) | Resident model household file | 
| [final_joint_tour_participants.csv](Resident Model joint tour participants file (final_joint_tour_participants.csv)) | Resident model joint tour participants file | 
| final_land_use.csv | Resident model land-use file | 
| [final_persons.csv](Resident Model vehicle file (final_vehicles.csv)) | Resident model persons file | 
| final_proto_disaggregate_accessibility.csv | Resident model disaggregate accessibility file at person level | 
| [final_tours.csv](Resident Model tour file (final_tours.csv)) | Resident model tour file | 
| [final_trips.csv](Resident Model trips file (final_trips.csv)) | Resident model trip file | 
| [final_vehicles.csv](Resident Model vehicle table (final_vehicles.csv)) | Resident model vehicle file | 
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
| HHT | Household dwelling unit type 0: N/A (GQ/vacant)
1: Married couple household
2: Other family household: Male householder, no spouse present
3: Other family household: Female householder, no spouse present
4: Nonfamily household: Male householder: Living alone
5: Nonfamily household: Male householder: Not living alone
6: Nonfamily household: Female householder: Living alone
7: Nonfamily household: Female householder: Not living alone | 
| auto_ownership | (Model output) Auto ownership | 
| num_workers | Number of workers in household | 
| building_category | Units in structure
0: N/A (GQ)
1: Mobile home or trailer
2: One-family house detached
3: One-family house attached
4: 2 Apartments
5: 3-4 Apartments
6: 5-9 Apartments
7: 10-19 Apartments
8: 20-49 Apartments
9: 50 or more apartments
10: Boat, RV, van, etc. | 
| unittype | Household unit type:
0 = Non-GQ Household
1 = GQ Household (used in Visualizer) | 
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

### Resident Model person file (final_persons.csv)

| **Field** | **Description** | 
| --- | --- |
| household_id | Household ID | 
| age | Person age in years | 
| PNUM | Person number in household (1…n where n is number of persons in household) | 
| sex | 1: Male
2: Female | 
| pemploy | Employment status of person:
1 = Employed Full-Time
2 = Employed Part-Time
3 = Unemployed or Not in Labor Force
4 = Less than 16 Years Old | 
| pstudent | Student status of person:
1 = Pre K-12
2 = College Undergrad+Grad and Prof. School
3 = Not Attending School | 
| ptype | Person type  1: Full-time worker 2: Part-time worker 3: College\University Student 4: Non-Working Adult 5: Retired 6: Driving-age student 7: Non-driving age student 8: Pre-school\Age <=5 | 
| educ | Educational attainment:
1 = No schooling completed
9 = High school graduate
13 = Bacehlor's degree | 
| soc2 | Two-digit Standard Occupational Classification (SOC) codes (https://www.bls.gov/oes/current/oes_stru.htm) | 
| is_student | Person is a K12 or college student | 
| is_worker | Person is a full-time or part-time worker | 
| is_internal_worker | TRUE if worker works internal to region, else FALSE (output from Internal-External Worker Identification Model) | 
| is_external_worker | TRUE if worker works external to region, else FALSE (output from Internal-External Worker Identification Model) | 
| home_zone_id | Household MGRA | 
| time_factor_work | Travel time sensitivity factor for work tours | 
| time_factor_nonwork | Travel time sensitivity factor for non-work tours (Sampled in person preprocessor) | 
| naics_code | Two-digit NAICS code (https://www.census.gov/naics/) | 
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
| grade | School grade of person:
0 = N/A (not attending school)
2 = K to grade 8
5 = Grade 9 to grade 12
6 = College undergraduate | 
| weeks | Weeks worked during past 12 months
0: N/A (less than 16 years old/did not work during the past 12 .months)
1: 50 to 52 weeks worked during past 12 months
2: 48 to 49 weeks worked during past 12 months
3: 40 to 47 weeks worked during past 12 months
4: 27 to 39 weeks worked during past 12 month
5: 14 to 26 weeks worked during past 12 months
6: 13 weeks or less worked during past 12 months | 
| hours | Usual hours worked per week past 12 months
0: .N/A (less than 16 years old/did not work during the past .12 months)
1..98 .1 to 98 usual hours
99 .99 or more usual hours | 
| race | Recoded detailed race code
1: .White alone
2: Black or African American alone
3: American Indian alone
4: Alaska Native alone
5: American Indian and Alaska Native tribes specified; or .American Indian or Alaska Native, not specified and no other .races
6: Asian alone
7: Native Hawaiian and Other Pacific Islander alone
8: Some Other Race alone
9: Two or More Races | 
| hispanic | Hispanic flag:
Non-Hispanic: 1
Hispanic: 2 | 

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
| final_households.csv | Visitor model household file | 
| final_land_use.csv | Visitor model land-use file | 
| final_persons.csv | Visitor model person file | 
| final_tours.csv | Visitor model tour file | 
| final_trips.csv | Visitor model trip file | 
| model_metadata.yaml | Visitor model Datalake metadata file | 
| nmotVisitortrips_(period).omx | Visitor model non-motorized trips by period (EA, AM, MD, PM, EV) |
