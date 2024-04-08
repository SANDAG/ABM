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

ActivitySim writes out various log files when it runs; these have standard names for each model component. Therefore we list them separately, but copies of these files may be in each model's output directory depending upon the settings used to run ActivitySim for that model component.

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
| final_households.csv | Resident model household file | 
| final_joint_tour_participants.csv | Resident model joint tour participants file | 
| final_land_use.csv | Resident model land-use file | 
| final_persons.csv | Resident model persons file | 
| final_proto_disaggregate_accessibility.csv | Resident model disaggregate accessibility file at person level | 
| final_tours.csv | Resident model tour file | 
| final_trips.csv | Resident model trip file | 
| final_vehicles.csv | Resident model vehicle file | 
| log (directory) | Directory for resident model logging output | 
| model_metadata.yaml | Resident model Datalake metadata file | 
| nmottrips_AM.omx | Resident model non-motorized trip tables by period (EA, AM, MD, PM, EV) | 
| skim_usage.txt | Skim usage file | 
| trace (directory) | Directory for resident model trace output | 

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
