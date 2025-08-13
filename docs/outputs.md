# Model Outputs

Model outputs are stored in the .\outputs directory. The contents of the directory are listed in the table below.

## Output Directory (.\output)


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
   <td>runtime_summary.csv
   </td>
   <td>Summary of model runtime
   </td>
  </tr>
  
  <tr>
   <td>transponderModelAccessibilities.csv
   </td>
   <td>Transponder model accessibilities (not used)
   </td>
  </tr>
  <tr>
   <td><a href="#trip-tables">trip_(period).omx</a>
   </td>
   <td>Total Trip tables to assign to the highway network by 5 time periods (tod = EA, AM, MD, PM, EV)
   </td>
  </tr>
  
</table>

<a id="trip-tables"></a>
### Total Vehicle Trips Assigned to the Highway Network by period

```TRIP_<TIME PERIOD>.OMX```


<table>
  <tr>
   <td>Table Name
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_HOV3_&lt;vot>
   </td>
   <td>Shared Ride 3 trips for time period and vot
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_TRK_&lt;vot>
   </td>
   <td>Truck trips for time period and vot
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_SOV_NT_&lt;vot>
   </td>
   <td>Drive Alone Non-Transponder trips for time period and vot
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_SOV_TR_&lt;vot>
   </td>
   <td>Drive Alone Transponder trips for time period and vot
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_HOV2_&lt;vot>
   </td>
   <td>Shared Ride 2 trips for time period and vot
   </td>
  </tr>
  <tr>
   <td>vot = L (low), M (medium), and H (high)
   </td>
   <td>
   </td>
   </tr>
  <tr>
   <td>*time period = EA, AM, MD, PM, EV
   </td>
   <td>
   </td>
  </tr>
  
</table>

## Skims (.\skims)

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



<a id="auto-skims"></a>
### Auto skims by period

```TRAFFIC_SKIMS_<time period>.OMX```


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
   <td>Travel time (minutes) for evaluation of volume-delay functions
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_DIST
   </td>
   <td>Travel distance (miles)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;traffic_class>&lt;_vot>_REL
   </td>
   <td>Impedance (cost/time) compared to other routes
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
   <td>Total distance (miles) on toll facilities (only for TOLL traffic classes)
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
   <td>Distance (miles) on HOV facilities (only for HOV traffic classes)
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



<a id="transit-skims"></a>
### Transit skims by period

```TRANSIT_SKIMS_<time_period>.OMX```


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
   <td>actual wait time (minutes) at initial boarding point
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_XFERWAIT
   </td>
   <td>actual wait time (minutes) at all transfer boarding points
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALWAIT
   </td>
   <td>total actual wait time (minutes)
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
   <td>access actual walk time (minutes) prior to initial boarding
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_EGRWALK
   </td>
   <td>egress actual walk time (minutes) after final alighting
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALWALK
   </td>
   <td>total actual walk time (minutes)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_TOTALIVTT
   </td>
   <td>Total actual in-vehicle travel time (minutes)
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_DWELLTIME
   </td>
   <td>Total dwell time (minutes) at stops
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_BUSIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on local bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_LRTIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on LRT mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class> _CMRIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on commuter rail mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class> _EXPIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on express bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_LTDEXPIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on premium bus mode
   </td>
  </tr>
  <tr>
   <td>&lt;time_period>_&lt;transit_class>_BRTIVTT
   </td>
   <td>actual in-vehicle travel time (minutes) on  BRT mode
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



## ActivitySim log files

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



## Airport model outputs (.\airport.CBX, .\airport.SAN)

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



### Airport Model household file (final_(airport)households.csv)


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



### Airport Model person file (final_(airport)persons.csv)


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


### Airport Model tour file (final_(airport)tours.csv)


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


### Airport Model trip file (final_(airport)trips.csv)

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


### Arrival Mode Table for Airport Models

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

### Airport Model auto demand matrices

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


### Airport Model transit demand matrices

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


### Airport Model non-motorized demand matrices

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

## Assignment model trip tables (.\assignment)

This directory contains trip tables from auto and transit assignments.

### Demand Matrices

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
   <td>cvmtrips_(period).omx
   </td>
   <td>CVM trip table for model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
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
   <td>htmtrips_(period).omx
   </td>
   <td>HTM trip table for model by period (EA, AM, MD, PM, EV) and value of time (low, medium, high)
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



### TNC Vehicle trip demand table

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


### Household autonomous vehicle trip data

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
   <td>Is origin home
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
   <td>Is destination home
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
   <td>Is origin remote parking
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
   <td>Is destination remote parking
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
   <td>Is trip inbound
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


### TNC vehicle trip matrix

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



### Empty Autonomous vehicle trips data

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


## Crossborder model outputs (.\crossborder)

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


### Crossborder Model person file (final_persons.csv)

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


### Crossborder Model tour file (final_tours.csv)

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


### Crossborder Model trip file (final_trips.csv)

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



### Crossborder Model Tour Mode Definitions

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

### Crossborder Model auto demand matrices

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


### Crossborder Model transit demand matrices

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


### Crossborder Model non-motorized demand matrices

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



## Commercial Vehicle Model (.\cvm)

This directory contains San Diego commercial travel model outputs.

<table>
  <tr>
   <td>File 
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>final_commercial_accessibility.csv
   </td>
   <td>contains the commercial vehicle accessibility by category for each zone
   </td>
  </tr>
  <tr>
   <td>final_cv_trips.csv
   </td>
   <td>contains the commercial vehicle trips by Origin-Destination, trip purpose and travel time
   </td>
  </tr>
  <tr>
   <td>final_establishments.csv
   </td>
   <td>details of the establishments generated for each zone
   </td>
  </tr>
  <tr>
   <td>final_establishments_all.csv
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td>final_households.csv
   </td>
   <td>information of the households which attract CVM trips 
   </td>
  </tr>
  <tr>
   <td>final_land use.csv
   </td>
   <td>land use information of the MGRA Zones
   </td>
  </tr>
  <tr>
   <td>final_routes
   </td>
   <td>route information of CVM trips
   </td>
  </tr>
  <tr>
   <td>final_trips
   </td>
   <td>Final trip-based CVM summary
   </td>
  </tr>
  <tr>
   <td>Trip Tables (where TOD is AM, MD, PM or EV)
   </td>
  </tr>
  <tr>
   <td>cvmtrips_TOD.omx
   </td>
   <td>contains the commercial vehicle trips of the TOD, has three components (modes): Car, light truck, medium truck and heavy truck
   </td>
  </tr>
  
</table>

### CVM commercial accessibility file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>estab_acc_estab_group1
    </td>
    <td>Establishments access to establishment customer attractions:establishment_attraction_ret + establishment_attraction_whl + establishment_attraction_mfg
    </td>
  </tr>
  <tr>
    <td>estab_acc_estab_group2
    </td>
    <td>Establishments access to establishment customer attractions:establishment_attraction_iut + establishment_attraction_laf
    </td>
  </tr>
  <tr>
    <td>estab_acc_estab_group3
    </td>
    <td>Establishments access to establishment customer attractions:establishment_attraction_agm + establishment_attraction_con + establishment_attraction_epo + establishment_attraction_mhs + establishment_attraction_trn

  </tr>
  <tr>
    <td>estab_acc_estab_group4
    </td>
    <td>Establishments access to establishment customer attractions:establishment_attraction_ifr
  </tr>
  <tr>
    <td>tnc_acc_estab_all
    </td>
    <td>TNC access to establishment customer attractions (all industries)
  </tr>
  <tr>
    <td>estab_acc_hh_food 
    </td>
    <td>Establishment access to household food delivery attractors
  </tr>
  <tr>
    <td>estab_acc_hh_package
    </td>
    <td>Establishment access to household package delivery attractors
  </tr>
  <tr>
    <td>estab_acc_logsum
    </td>
    <td>Establishment accessibility logsum (total establishment attractors – visits to non-residential customers of all types)
  </tr>
  <tr>
    <td>household_acc_logsum
    </td>
    <td>Household accessibility logsum (total household attractors – visits to residential customers)

  </tr>
  <tr>
    <td>accessibility
    </td>
    <td>estab_acc_logsum + household_acc_logsum

  </tr>
  <tr>
    <td>acc_hh_goods
    </td>
    <td>Accessibility to households for goods delivery only

  </tr>
  <tr>
    <td>zone_id
    </td>
    <td>The MGRA id of the zone

  </tr>
  

</table>

### CVM trips file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>route_id
    </td>
    <td>the route id on which the trip is assigned to
    </td>
  </tr>
  <tr>
    <td>route_trip_num
    </td>
    <td>the number of trips on the route_id
    </td>
  </tr>
  <tr>
    <td>trip_origin
    </td>
    <td>origin zone (MGRA) of the trip

  </tr>
  <tr>
    <td>trip_destination
    </td>
    <td>destination zone (MGRA) of the trip

  </tr>
  <tr>
    <td>trip_origin_purpose
    </td>
    <td>purpose of the stop at the origin of the trip:
    <p>
      • base - return to establishment
    <p>
      • goods_delivery - deliver goods
    <p>
      • goods_pickup - pickup goods
    <p>
      • home - go to driver's home
    <p>
      • maintenance - maintenance (refueling, vehicle maintenance, driver breaks/meals)
    <p>
      • service - provide a service
    <p>
    terminate - end route (vehicle day), go to terminal destination chosen for route
  </tr>
  <td>trip_destination_purpose
    </td>
    <td>purpose of the stop at the destination of the trip:
    <p>
      • base - return to establishment
    <p>
      • goods_delivery - deliver goods
    <p>
      • goods_pickup - pickup goods
    <p>
      • home - go to driver's home
    <p>
      • maintenance - maintenance (refueling, vehicle maintenance, driver breaks/meals)
    <p>
      • service - provide a service
    <p>
    terminate - end route (vehicle day), go to terminal destination chosen for route
  </tr>
  <tr>
    <td>trip_destination_type
    </td>
    <td>attraction land use type at the destination:
    <p>
      • base - establishment location (any land use type)
    <p>
      • warehouse - warehouse, distribution center land use type
    <p>
      • intermodal - airport, seaport, rail intermodal land use type
    <p>
      • commercial - commercial land use type (not warehouse or intermodal)
    <p>
      • residential - residential land use type
  </tr>
  <tr>
    <td>trip_start_time
    </td>
    <td>start time of the trip (half-hour intervals counted from beginning)
  </tr>
  <tr>
    <td>trip_travel_time
    </td>
    <td>travel time (in motion time) of the vehicle during the trip

  </tr>
  <tr>
    <td>dwell_time
    </td>
    <td>resting/dwelling time of the vehicle during the trip (stop duration)

  </tr>
  <tr>
    <td>route_elapsed_time
    </td>
    <td>total time elapsed during the trip (trip_travel_time + dwell_time)


  </tr>
  <tr>
    <td>cv_trip_id
    </td>
    <td>the unique trip id of the cv trip (route_id + route_trip_num)

  </tr>
  

</table>

### CVM establishments file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>industry_number
    </td>
    <td>The category of industry that the establishment belongs to range from 1 to 12
    </td>
  </tr>
  <tr>
    <td>industry_name
    </td>
    <td>The name of the industry that the establishment belongs to (EPO, AGM, CON etc.)
    </td>
  </tr>
  <tr>
    <td>LUZ
    </td>
    <td>Land Use Zone ( TAZ Number)
    </td>
  </tr>
  <tr>
    <td>zone_id
    </td>
    <td>MGRA Number

  </tr>
  <tr>
    <td>employees
    </td>
    <td>The number of employees in the establishment
    </td>
  </tr>
  <tr>
    <td>size_class
    </td>
    <td>The categorized size of the establishment based on the number of employees
(range: 1-7)
    </td>
  </tr>
  <tr>
    <td>sample_rate
    </td>
    <td>the percentage of sample selected (generally 1)
    </td>
  </tr>
  <tr>
    <td>has_attraction
    </td>
    <td>TRUE if the zone has trip attraction, otherwise FALSE
    </td>

  </tr>
  <tr>
    <td>attractions
    </td>
    <td>number of trip attractions by the establishment
    </td>
  </tr>

  <tr>
    <td>has_generation
    </td>
    <td>TRUE if the zone has trip generation, otherwise FALSE
    </td>
  </tr>
  <tr>
    <td>n_routes
    </td>
    <td>number of routes the establishment generated
    </td>
  </tr>
  <tr>
    <td>accessibility
    </td>
    <td>Total accessibility to household + establishment attractions
    </td>
  </tr>
  <tr>
    <td>establishment_id
    </td>
    <td>unique id of the establishment
    </td>
  </tr>
</table>

### CVM establishments_all file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>industry_number
    </td>
    <td>The category of industry that the establishment belongs to range from 1 to 12
    </td>
  </tr>
  <tr>
    <td>industry_name
    </td>
    <td>The name of the industry that the establishment belongs to (EPO, AGM, CON etc.)
    </td>
  </tr>
  <tr>
    <td>LUZ
    </td>
    <td>Land Use Zone ( TAZ Number)
    </td>
  </tr>
  <tr>
    <td>zone_id
    </td>
    <td>MGRA Number

  </tr>
  <tr>
    <td>employees
    </td>
    <td>The number of employees in the establishment
    </td>
  </tr>
  <tr>
    <td>size_class
    </td>
    <td>The categorized size of the establishment based on the number of employees
(range: 1-7)
    </td>
  </tr>
  <tr>
    <td>sample_rate
    </td>
    <td>the percentage of sample selected (generally 1)
    </td>
  </tr>
   <tr>
    <td>attractions
    </td>
    <td>number of trip attractions by the establishment
    </td>
  </tr>
 <tr>
    <td>industry_group
    </td>
    <td>Range 1-5
    </td>
  </tr>
 <tr>
    <td>beta_industry_group
    </td>
    <td>Parameter used in the establishment attractions model
    </td>
  </tr>
 <tr>
    <td>constant
    </td>
    <td>Parameter used in the establishment attractions model
    </td>
  </tr>
 <tr>
    <td>has_attraction_probability
    </td>
    <td>Intermediate result of establishments attractor model. Probability that the establishment will have deliveries or service stops
    </td>
  </tr>
 <tr>
    <td>random
    </td>
    <td>Random number used in attraction generation calculations
    </td>
  </tr>

  <tr>
    <td>has_attraction
    </td>
    <td>TRUE if the zone has trip attraction, otherwise FALSE
    </td>

  </tr>
 
  <tr>
    <td>establishment_id
    </td>
    <td>unique id of the establishment
    </td>
  </tr>
</table>


### CVM households file

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
    <td>MGRA of the household
    </td>
  </tr>
  <tr>
    <td>income
    </td>
    <td>annual household income
    </td>
  </tr>
  <tr>
    <td>hhsize
    </td>
    <td>household size
    </td>
  </tr>
  <tr>
    <td>HHT
    </td>
    <td>household type (ranges 1-7)

  </tr>
  <tr>
    <td>auto_ownership
    </td>
    <td>number of vehicles owned by the household
    </td>
  </tr>
  <tr>
    <td>num_workers
    </td>
    <td>number of workers in the household
    </td>
  </tr>
  <tr>
    <td>sample_rate
    </td>
    <td>the percentage of sample selected (generally 1)
    </td>
  </tr>
   <tr>
    <td>num_adults
    </td>
    <td>number of adults in the household
    </td>
  </tr>
 <tr>
    <td>num_nonworker_adults
    </td>
    <td>number of non-working adults in the households
    </td>
  </tr>
 <tr>
    <td>num_children
    </td>
    <td>number of children in the household
    </td>
  </tr>
 <tr>
    <td>has_attraction_food
    </td>
    <td>TRUE if the household attracts food delivery trips, otherwise FALSE
    </td>
  </tr>
 <tr>
    <td>has_attraction_package
    </td>
    <td>TRUE if the household attracts package delivery trips, otherwise FALSE
    </td>
  </tr>

  <tr>
    <td>has_attraction_service
    </td>
    <td>TRUE if the household attracts service trips, otherwise FALSE
    </td>

  </tr>
 
  <tr>
    <td>household_id
    </td>
    <td>unique household id
    </td>
  </tr>
</table>

### CVM land_use file

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
    <td>TAZ number
    </td>
  </tr>
  <tr>
    <td>luz_id
    </td>
    <td>LUZ id
    </td>
  </tr>
  <tr>
    <td>pop
    </td>
    <td>population of the zone
</td>
  </tr>
  <tr>
    <td>hhp
    </td>
    <td>total household population ( excluding GQ population )
    </td>
  </tr>
  <tr>
    <td>hh
    </td>
    <td>number of households in the zone
    </td>
  </tr>
  <tr>
    <td>hhs
    </td>
    <td>average household size
    </td>
  </tr>
   <tr>
    <td>gq_civ
    </td>
    <td>GQ Civilian
    </td>
  </tr>
 <tr>
    <td>gq_mil
    </td>
    <td>GQ Military
    </td>
  </tr>
 <tr>
    <td>i10
    </td>
    <td>number of households with income $200,000 or more
    </td>
  </tr>
 <tr>
    <td>emp_gov
    </td>
    <td>Government employment
    </td>
  </tr>
 <tr>
    <td>emp_mil
    </td>
    <td> Military employment
    </td>
  </tr>

  <tr>
    <td>emp_ag_min
    </td>
    <td>Agriculture and mining employment
    </td>

  </tr>
 
  <tr>
    <td>emp_bus_svcs
    </td>
    <td>Business services employment
    </td>
  </tr>

  <tr>
    <td>emp_bus_svcs
    </td>
    <td>Business services employment
    </td>
  </tr>

  <tr>
    <td>emp_fin_res_mgm
    </td>
    <td>Finance and Resource Management employment
    </td>
  </tr>

  <tr>
    <td>emp_educ
    </td>
    <td>Education employment
    </td>
  </tr>

  <tr>
    <td>emp_hlth
    </td>
    <td>Health employment
    </td>
  </tr>

  <tr>
    <td>emp_ret
    </td>
    <td>Retail employment
    </td>
  </tr>

  <tr>
    <td>emp_trn_wrh
    </td>
    <td>Transportation and warehouse employment
    </td>
  </tr>

  <tr>
    <td>emp_con
    </td>
    <td>Construction employment
    </td>
  </tr>
  <tr>
    <td>emp_utl
    </td>
    <td>Utilities employment
    </td>
  </tr>
  <tr>
    <td>emp_mnf
    </td>
    <td>Manufacturing employment
    </td>
  </tr>
  <tr>
    <td>emp_whl
    </td>
    <td>Wholesale employment
    </td>
  </tr>
  <tr>
    <td>emp_ent
    </td>
    <td>Entertainment employment
    </td>
  </tr>
  <tr>
    <td>emp_accm
    </td>
    <td>Accomodation employment
    </td>
  </tr>
  <tr>
    <td>emp_food
    </td>
    <td>Food employment
    </td>
  </tr>
  <tr>
    <td>emp_oth
    </td>
    <td>Other employment
    </td>
  </tr>
  <tr>
    <td>emp_non_ws_wfh
    </td>
    <td>Non-wage and salary work from home employment
    </td>
  </tr>
  <tr>
    <td>emp_non_ws_oth
    </td>
    <td>Non-wage and salary other employment
    </td>
  </tr>
  <tr>
    <td>emp_total
    </td>
    <td>Total employment
    </td>
  </tr>
  <tr>
    <td>pseudomsa
    </td>
    <td>Pseudo MSA -
    <p>
      1: Downtown
    <p>
      2: Central
    <p>
      3: North City
    <p>
      4: South Suburban
    <p>
      5: East Suburban
    <p>
      6: North County West
    <p>
      7: North County East
    <p>
      8: East County
    </td>
  </tr>
  <tr>
    <td>enrollgradekto8
    </td>
    <td>Grade School K-8 enrollment
    </td>
  </tr>
  <tr>
    <td>enrollgrade9to12
    </td>
    <td>Grade School 9-12 enrollment
    </td>
  </tr>
  <tr>
    <td>othercollegeenroll
    </td>
    <td>Other College Enrollment
    </td>
  </tr>
  <tr>
    <td>hotelroomtotal
    </td>
    <td>Total Number of hotel rooms
    </td>
  </tr>
  <tr>
    <td>parkactive
    </td>
    <td>Acres of active park
    </td>
  </tr>
  <tr>
    <td>openspaceparkpreserve
    </td>
    <td>Acres of Open Park or Preserve
    </td>
  </tr>
  <tr>
    <td>beachactive
    </td>
    <td>Acres of active beaches
    </td>
  </tr>
  <tr>
    <td>district27
    </td>
    <td>District 27
    </td>
  </tr>
  <tr>
    <td>milestocoast
    </td>
    <td>Distance in miles to the nearest coast
    </td>
  </tr>
  <tr>
    <td>acres
    </td>
    <td>Total acres in the MGRA
    </td>
  </tr>
  <tr>
    <td>land_acres
    </td>
    <td>Acres of land in the MGRA
    </td>
  </tr>
  <tr>
    <td>effective_acres
    </td>
    <td>Effective acres in the MGRA
    </td>
  </tr>
  <tr>
    <td>truckregiontype
    </td>
    <td>Truck Region type
    </td>
  </tr>
  <tr>
    <td>exp_hourly
    </td>
    <td>Hourly Parking Expenditure
    </td>
  </tr>
  <tr>
    <td>exp_daily
    </td>
    <td>Daily Parking Expenditure
    </td>
  </tr>
  <tr>
    <td>exp_monthly
    </td>
    <td>Monthly Parking Expenditure
    </td>
  </tr>
  <tr>
    <td>parking_type
    </td>
    <td>Parking choice at destination:
      <p>
        0 = Not constrained to remote parking
      <p>
        1 = Park at destination
      <p>
        2 = Remote parking
      <p>
        3 = Park at home
    </td>
  </tr>
  <tr>
    <td>parking_spaces
    </td>
    <td>Number of parking spaces available
    </td>
  </tr>
  <tr>
    <td>ech_dist
    </td>
    <td>Elementary school district
    </td>
  </tr>
  <tr>
    <td>hch_dist
    </td>
    <td>High School district
    </td>
  </tr>
  <tr>
    <td>remoteAVParking
    </td>
    <td>Remote AV parking available at MGRA:
    <p>
      0 = Not available
    <p>
      1 = Available
    </td>
  </tr>
  <tr>
    <td>refueling_stations
    </td>
    <td>Number of refueling stations in the MGRA
    </td>
  </tr>
  <tr>
    <td>MicroAccessTime
    </td>
    <td>Micro-mobility access time (mins)
    </td>
  </tr>
  <tr>
    <td>microtransit
    </td>
    <td>Number of micro transit facilities
    </td>
  </tr>
  <tr>
    <td>nev
    </td>
    <td>Number of EV Charging stations
    </td>
  </tr>
  <tr>
    <td>toint
    </td>
    <td>Total Intersection
    </td>
  </tr>
  <tr>
    <td>duden
    </td>
    <td>Dwelling unit density
    </td>
  </tr>
  <tr>
    <td>empden
    </td>
    <td>Employment density
    </td>
  </tr>
  <tr>
    <td>popden
    </td>
    <td>Population density
    </td>
  </tr>
  <tr>
    <td>retempden
    </td>
    <td>Retail Employment density
    </td>
  </tr>
  <tr>
    <td>totintbin
    </td>
    <td>Total Intersection Bin
    </td>
  </tr>
  <tr>
    <td>empdenbin
    </td>
    <td>Employment density bin
    </td>
  </tr>
  <tr>
    <td>dudenbin
    </td>
    <td>Dwelling unit density per bin
    </td>
  </tr>
  <tr>
    <td>PopEmpDenPerMi
    </td>
    <td>Population and employment density per mile
    </td>
  </tr>
  <tr>
    <td>TAZ
    </td>
    <td>TAZ id
    </td>
  </tr>
  <tr>
    <td>poe_id
    </td>
    <td>Port of Entry ID ( not used by CVM)
    </td>
  </tr>
  <tr>
    <td>external_work
    </td>
    <td>number of workers if an external zone
    </td>
  </tr>
  <tr>
    <td>external_nonwork
    </td>
    <td>number of nonworkers if an external zone
    </td>
  </tr>
  <tr>
    <td>external_TAZ
    </td>
    <td>is an external TAZ ( 1 if true, 0 if false)
    </td>
  </tr>
  <tr>
    <td>external_MAZ
    </td>
    <td>is an external MAZ ( 1 if true, 0 if false)
    </td>
  </tr>
  <tr>
    <td>walk_dist_local_bus
    </td>
    <td>walking distance ( in miles) to nearest local bus
    </td>
  </tr>
  <tr>
    <td>walk_dist_premium_transit
    </td>
    <td>walking distance ( in miles) to premium transit
    </td>
  </tr>
  <tr>
    <td>micro_dist_local_bus
    </td>
    <td>micro district distance to local bus
    </td>
  </tr>
  <tr>
    <td>micro_dist_premium_transit
    </td>
    <td>micro district distance to premium transit
    </td>
  </tr>
  <tr>
    <td>ML_DIST
    </td>
    <td>distance in miles to managed lanes 
    </td>
  </tr>
  <tr>
    <td>AVGTTS
    </td>
    <td>Average travel time savings for all households in each zone across all possible destinations
    </td>
  </tr>
  <tr>
    <td>PCTDETOUR
    </td>
    <td>Percent detour is the percent difference between the AM transponder travel time and the AM non-transponder travel time to sample zones when the general-purpose lanes parallel to all toll lanes using transponders are unavailable
    </td>
  </tr>
  <tr>
    <td>terminal_time
    </td>
    <td>Terminal Time (0,3,4,5,7,10 minutes)
    </td>
  </tr>
  <tr>
    <td>num_hh_food_delivery
    </td>
    <td>Number of households which attract food deliveries
    </td>
  </tr>
  <tr>
    <td>num_hh_package_delivery
    </td>
    <td>Number of households which attract package deliveries
    </td>
  </tr>
  <tr>
    <td>num_hh_service
    </td>
    <td>Number of households which attract service trips
    </td>
  </tr>
  <tr>
    <td>establishment_attraction
    </td>
    <td>Attraction of establishments
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_agm
    </td>
    <td>Attraction of establishments for agriculture and mining
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_con
    </td>
    <td>Attraction of establishments for construction
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_epo
    </td>
    <td>Attraction of establishments for education
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_ifr
    </td>
    <td> Attraction of establishments for information, real estate, finance,professional services
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_iut
    </td>
    <td>Attraction of establishments for industrial and utilities
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_laf
    </td>
    <td> Attraction of establishments for leisure, accommodations, and food
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_mfg
    </td>
    <td>Attraction of establishments for manufacturing
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_mhs
    </td>
    <td> Attraction of establishments for medical and health services
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_mil
    </td>
    <td>Attraction of establishments for military
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_ret
    </td>
    <td> Attraction of establishments for retail trade
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_trn
    </td>
    <td>  Attraction of establishments for transportation and warehousing
    </td>
  </tr>
  <tr>
    <td>establishment_attraction_whl
    </td>
    <td> Attraction of establishments for wholesale trade
    </td>
  </tr>
  <tr>
    <td>is_port
    </td>
    <td>TRUE if the zone is a port, otherwise FALSE
    </td>
  </tr>
  <tr>
    <td>zone_id
    </td>
    <td>MGRA id
    </td>
  </tr>
   
  
</table>

### CVM final_routes file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>establishment_id
    </td>
    <td>Establishment ID ( Industry ID )
    </td>
  </tr>
  <tr>
    <td>business_type
    </td>
    <td>Name of the industry
    </td>
  </tr>
  <tr>
    <td>route_purpose
    </td>
    <td>Primary purpose of the route:
        <p>
        • Goods - pickup and/or deliver goods
        <p>
        • Service - provide services to customers
        <p>
        • Maintenance - vehicle maintenance or driver business
    </td>
  </tr>
  <tr>
    <td>customer_type
    </td>
    <td>Type of the customer ( residential; non-residential; mixed)
  </tr>
  <tr>
    <td>vehicle_type
    </td>
    <td>Type of the vehicle (LV, MUT, SUT)
    </td>
  </tr>
  <tr>
    <td>is_tnc
    </td>
    <td>TRUE if route is a TNC, otherwise FALSE
    </td>
  </tr>
  <tr>
    <td>vehicle_type_abm3
    </td>
    <td>Type of the vehicle ( Passenger car, LHDU, MHDU, and HHDU)
    </td>
  </tr>
  <tr>
    <td>random
    </td>
    <td>Random number used in route generation calculations
    </td>

  </tr>
  <tr>
    <td>start_time
    </td>
    <td>Start Time ( half-hour intervals )
    </td>
  </tr>

  <tr>
    <td>route_start_time_period_
    </td>
    <td>Start time period
    </td>
  </tr>
  <tr>
    <td>origin_stop_type
    </td>
    <td>Origin Stop Type:
      <p>
      • base - establishment location (any land use type)
      <p>
      • warehouse - warehouse, distribution center land use type
      <p>
      • commercial - commercial land use type (not warehouse)
      <p>
      • residential - residential land use type
    </td>
  </tr>
  <tr>
    <td>origination_zone
    </td>
    <td>Origin Zone for start of route
    </td>
  </tr>
  <tr>
    <td>destination_stop_type
    </td>
    <td>Destination Stop Type:
      <p>
      • base - establishment location (any land use type)
      <p>
      • warehouse - warehouse, distribution center land use type
      <p>
      • commercial - commercial land use type (not warehouse)
      <p>
      • residential - residential land use type
    </td>
  </tr>
  <tr>
    <td>terminal_zone
    </td>
    <td>Destination Zone for termination of route
    </td>
  </tr>
  <tr>
    <td>route_id
    </td>
    <td>unique route id ( or trip id )
    </td>
  </tr>
</table>

### CVM final_trips file

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>route_id
    </td>
    <td>the route id on which the trip is assigned to
    </td>
  </tr>
  <tr>
    <td>route_trip_num
    </td>
    <td>the number of trips on the route_id
    </td>
  </tr>
  <tr>
    <td>trip_origin
    </td>
    <td>origin zone (MGRA) of the trip
    </td>
  </tr>
  <tr>
    <td>trip_destination
    </td>
    <td>destination zone (MGRA) of the trip
  </tr>
  <tr>
    <td>trip_destination_purpose
    </td>
    <td>purpose of the trip:
      <p>
        • base - return to establishment
      <p>
        • goods_delivery - deliver goods
      <p>
        • goods_pickup - pickup goods
      <p>
        • home - go to driver's home
      <p>
        • maintenance - maintenance (refueling, vehicle maintenance, driver breaks/meals)
      <p>
        • service - provide a service
      <p>
        • terminate - end route (vehicle day), go to terminal destination chosen for route
    </td>
  </tr>
  <tr>
    <td>trip_destination_type
    </td>
    <td>attraction land use type at the destination:
      <p>
        • base - establishment location (any land use type)
      <p>
        • warehouse - warehouse, distribution center land use type
      <p>
        • intermodal - airport, seaport, rail intermodal land use type
      <p>
        • commercial - commercial land use type (not warehouse or intermodal)
      <p>
        • residential - residential land use type
    </td>
  </tr>
  <tr>
    <td>trip_start_time
    </td>
    <td>start time of the trip (half hour intervals counted from beginning)
    </td>
  </tr>
  <tr>
    <td>trip_travel_time
    </td>
    <td>travel time ( in motion time ) of the vehicle during the trip
    </td>
  </tr>
  <tr>
    <td>dwell_time
    </td>
    <td>resting/dwelling time of the vehicle during the trip
    </td>
  </tr>
  <tr>
    <td>route_elapsed_time
    </td>
    <td>total time elapsed during the trip ( trip_travel_time + route_trip_num)
    </td>
  </tr>
  <tr>
    <td>cv_trip_id
    </td>
    <td>the unique trip id of the cv trip (route_id + route_trip_num)
    </td>
  </tr>
  <tr>
    <td>taz_origin
    </td>
    <td>origin zone ( TAZ ) of the trip
    </td>
  </tr>
  <tr>
    <td>taz_destination
    </td>
    <td>destination zone (TAZ) of the trip
    </td>
  </tr>
  <tr>
    <td>vehicle_type
    </td>
    <td>Vehicle types: DRIVEALONE ( passenger car), LHGT, MHDT, or HHDT
    </td>
  </tr>
  <tr>
    <td>tod
    </td>
    <td>Start time period of the trip ( ABM3 categories)
    </td>
  </tr>
  <tr>
    <td>distanceDrive
    </td>
    <td>Trip distance, from traffic_skims_MD
    </td>
  </tr>
  <tr>
    <td>costTollDrive
    </td>
    <td>Trip toll cost, from traffic_skims_MD
    </td>
  </tr>
  <tr>
    <td>costOperatingDrive
    </td>
    <td>Trip operating costs (cents)
    </td>
  </tr>
</table>


## Resident model outputs (.\resident)

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



### Resident Model household file (final_households.csv)


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


### Resident Model person file (final_persons.csv)


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
   <td>MGRA number of school location, else -1  (output from School Location Choice Model)
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
   <td>MGRA number of internal work location, else -1  (output from Internal Work Location Choice Model)
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
   <td>mandatory_tour_frequency
   </td>
   <td>Mandatory Tour Frequency Model Choice if worker or student, else null (Output from Mandatory Tour Frequency Model). String "work1": 1 work tour, "work2" 2 work tours, "school1: 1 school tour, "school2" 2 school tours, "work_and_school": 1 work and 1 school tour
   </td>
  </tr>
  <tr>
   <td>num_non_mand
   </td>
   <td>Total number of non-mandatory tours (Output from School Escort Model, Non-Mandatory Tour Frequency Model, and At-Work Subtour Model)
   </td>
  </tr>
  <tr>
   <td>num_mand
   </td>
   <td>Total number of mandatory tours  (Output from Mandatory Tour Frequency Model)
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
   <td>num_work_tours
   </td>
   <td>Total number of work tours  (Output from Mandatory Tour Frequency Model)
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


### Resident Model vehicle file (final_vehicles.csv)


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



### Resident Model joint tour participants file (final_joint_tour_participants.csv)


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



### Resident Model tour file (final_tours.csv)


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



### Resident Model trip file (final_trips.csv)


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



### Resident Model tour mode definitions


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



### Resident Model auto demand matrices


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



### Resident Model transit demand matrices


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



### Resident Model non-motorized demand matrices


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

## Heavy Truck Model (.\htm)

This directory contains San Diego heavy truck model outputs.

<table>
  <tr>
   <td>File 
   </td>
   <td>Description
   </td>
  </tr>
  <tr>
   <td>htmtrips_(period).omx ((period) = EA, AM, MD, PM, EV)

   </td>
   <td> HTM trips of (period), by mode (Light, Medium or Heavy trucks) and type of trip (IE, EI or EE)
   </td>
  </tr>
  <tr>
   <td>Htmsummary_files.xlsx
   </td>
   <td>OD-based HTM trip tables by Mode (Light, Medium or Heavy trucks)
   </td>
  </tr>
  <tr>
   <td>final_trips.csv
   </td>
   <td>Aggregate all HTM trips by TAZ and truck type
   </td>
  </tr>
  
</table>

### Heavy Truck Model Trip File ( htmsummary_files.xlsx )

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>Light
    </td>
    <td>OD-based HTM trip tables for light trucks (LHDT)
    </td>
  </tr>
  <tr>
    <td>Medium
    </td>
    <td>OD-based HTM trip tables for medium trucks (MHDT)
    </td>
  </tr>
  <tr>
    <td>Heavy
    </td>
    <td>OD-based HTM trip tables for heavy trucks (HHDT)
  </tr>

</table>

### Heavy Truck Model Trip File ( final_trips.csv )

<table>
  <tr>
    <td>Field
    </td>
    <td>Description
    </td>
  </tr>
  <tr>
    <td>taz_p
    </td>
    <td>origin zone (TAZ) of the trip
    </td>
  </tr>
  <tr>
    <td>taz_a
    </td>
    <td>destination zone (TAZ) of the trip
    </td>
  </tr>
  <tr>
    <td>Truck_Type
    </td>
    <td>Truck type (Light, Medium and High trucks)
  </tr>
  <tr>
    <td>tod
    </td>
    <td>Start time period of the trip (ABM3 time periods)
  </tr>
  <tr>
    <td>trips
    </td>
    <td>Sum of trips for corresponding TAZ and Truck Type
  </tr>
  <tr>
    <td>distanceDrive
    </td>
    <td>Trip distance, from traffic_skims_MD
  </tr>
  <tr>
    <td>timeDrive
    </td>
    <td>Trip time, from traffic_skims_MD
  </tr>
  <tr>
    <td>costTollDrive
    </td>
    <td>Trip toll cost, from traffic_skims_MD
  </tr>
  <tr>
    <td>costOperatingDrive
    </td>
    <td>Trip operation cost
  </tr>
  

</table>


## Visitor model outputs (.\visitor)

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



### Visitor Model household file (final_households.csv)


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



### Visitor Model person file (final_persons.csv)


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



### Visitor Model tour file (final_tours.csv)


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



### Visitor Model trip file (final_trips.csv)


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



### Visitor model’s tour mode choice definitions


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

### Visitor Model auto demand matrices

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


### Visitor Model transit demand matrices

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



### Visitor Model non-motorized demand matrices

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


#### Trip Mode Definitions

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