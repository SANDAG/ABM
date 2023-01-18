This readme is a record of things that might want to be fixed or changed in subsequent versions

---
Input Data Prep:

* Had to add an extra `DIST` skim to call for school and work location choice (check out skim lists for other example models)
  - attempting to call `SOV_TR_M_DIST__AM` skim directly in school location choice model (via multiple methods) did not succeed
  - will also need `DISTWALK` and `DISTBIKE` skims
* created maz_tap_drive.csv file by using _accessam.csv_
  - _accessam.csv_ provides TAZ to TAP drive times
  - there are only 105 unique taps in that file -- only taps that are connected to a parking lot.  This means that only these taps are available for knr and tnc-transit
  - joined maz to taz crosswalk from landuse file to create maz to tap drive times.  This means all mazs within a taz receive the same drive time.
  - joined tap.ptype to maz_to_tap_drive to get walk distances from the lot to the tap.  Walk distances are used in _tvbp_utility_drive_maz_tap.csv_
* Bike Times and Logsums
  - Used the _bikeTazLogsum.csv_ file to create BIKE_LOGSUM and BIKE_TIME skims and added them to the _traffic_skims_processed_AM.omx_ file.  Do we want these in a separate skim file? Could also include other non-time-of-day specific skims in there like DIST
  - Used the _bikeMgraLogsum.csv_ file to create _maz_maz_bike.csv_ that has columns [OMAZ, DMAZ, bikeLogsum, bikeTime]

---
Changes in multiple files
* Landuse field called `area_type` is not a SANDAG variable.  It was used to determine CBD, urban, and rural status.
  - changed to use SANDAG's `pseudomsa` field
  - changed the thresholds for `[cbd,urb,rural]_threshold` in _common/constants.yaml_ to reflect the change
  - This change appears in the following list of files (do a search in configs for land_use.pseudomsa):
     - _annotate_households.csv_
     - _annotate_landuse.csv_
     - _annotate_persons_workplace.csv_, It was used to create the `work_zone_area_type` field which is used in _atwork_subtour_destination.csv_
     - _annotate_persons_
     - _stop_frequency_annotate_tours_preprocessor.csv_ to create `destination_area_type`
     - _non_mandatory_tour_scheduling_annotate_tours_preprocessor.csv_
     - _joint_tour_scheduling_annotate_tours_preprocessor.csv_
     - _trip_destination_sample.csv_ and _trip_destination.csv_ (although these should be replaced soon)
  - all of these files should be checked to make sure their expressions related to landuse area type still make sense with this change
* Changed `SOV_DIST, MD` to `DIST` in the following files:
  - _school_location.csv_ and _school_location_choice.csv_
  - _workplace_location.csv_ and _workplace_location_sample.csv_
  - _joint_tour_scheduling_annotate_tours_preprocessor.csv_
---
_settings.yaml_

* re-named columns in households and persons
* Kept all columns from landuse.  Decreasing the number of landuse columns might make a substantial impact on RAM requirements as they are often jointed to the person and hh tables
---
Disaggregate Accessibilities
* Currently set number of origin zones to 5000 and number of destinations to 100.  These should be reviewed.
---
_annotate_landuse.yaml_

* Capped the employment density (tot_emp / acre) to 500
  - had to make this change to avoid infinte probabilities in the walk term in the trip destination logsum
  - for reference, MAZ 2623 has and employment density of 2734 (which was the one causing the trouble), with the next highest at 856 for maz 2626
---
_network_los.yaml_

* Only have walk transit and drive transit options for building the tvpb
* Added accessibility settings (not present in xborder model) and updated for SANDAG skim file names / columns
  - changed `df.walk_time` to `df.walkTime` in _tvpb_accessibility_walk_maz_tap.csv_
* copied `tvpb_accessibility_tap_tap_.csv` from SEMCOG three zone implementation and updated skim names
* Added `demographic_segments`.  They are called in _tour_mode_choice_.  Looks like the cross border model "fakes" these segments by defining them in it's _annotate_persons.csv_
  - want to the `TVPB_demographic_segments_by_income_segment` related constants to _constants.csv_? (they were taken from SEMCOG and values should be checked)
* Added drive access & egress to tour_mode_choice and trip_mode_choice settings
* Trip mode choice constants to not match tour mode choice.  Is this on purpose?
* Updated time periods in network_los.yaml
  - Changed time periods in _write_trip_matrices.yaml_ to match

---
Initialize Households

* In _annotate_households.csv_, `home_is_urban` and `home_is_rural` values are now computed based on `pseudomsa` instead of the previous `area_type` variable that exists in other examples.  Is there a better substitute for this?
* Added some definitions to _annotate_households.csv_, _annotate_persons.csv_, and added additional _annotate_persons_after_hh.csv_ for variables used in the scheduling models
  - The _annotate_persons_after_hh.csv_ is needed to calculate person attributes based on household attributes that are not available when annotate_persons first runs.  (This was copied from SEMCOG along with the scheduling models)

---
_annotate_landuse.csv_

* employment and household densities are calculated using by the landuse `acres` variable
* `is_cbd` is calculated by `pseudomsa = 1` (downtown)

---
_destination_choice_size_terms.csv_

* changed column names to match existing columns in landuse.
  * previous columns: TOTHH, RETEMPN, FPSEMPN, HEREMPN, OTHEMPN, AGREMPN, MWTEMPN, AGE0519, HSENROLL, COLLFTE, COLLPTE.  (data dictionary [here](https://github.com/BayAreaMetro/modeling-website/wiki/TazData#:~:text=%C2%A0-,RETEMPN,%C2%A0,-PRKCST))
  * new columns (respectively): emp_retail, emp_personal_svcs_office, emp_health, emp_restaurant_bar, emp_ag, emp_mfg_prod, enrollgradekto8, enrollgrade9to12, collegeenroll, othercollegeenroll
* Column changes are just placeholders -- these will need to be redone to be consistent with CT-RAMP implementation

---
_accessibility.csv_ and _accessibility.yaml_

* changed all instances of `TOTEMP` to `emp_total` and `RETEMP` to `emp_retail`
* changed `SOVTOLL_TIME` skim to `SOV_TR_M_TIME`

---
School Location Choice
* Used MD `SOV_TR_M_TIME` skim in _annotate_persons_school.csv_ for time from home to school
* Now only runs for persons with `is_internal_student` (set in _school_location.yaml_)

---
Workplace Location Choice
* Changed `DISTWALK` to `DIST` in _annotate_persons_workplace.csv_ while waiting on walk distance skim
* Used MD `SOV_TR_M_TIME` skim in _annotate_persons_workplace.csv_ for time from home to work
* Now only runs for persons with `is_internal_worker` (set in _workplace_location.yaml_)

---
Auto Ownership
* Converted from MTC example.  Things that should be signed off on:
  - No `educational_attainment` term in synthetic population.  Commented these terms out.
  - Density terms are supposed to be in value / acre?
  - `use_accessibility` setting in MTC configs that were commented out. Left those commented out.
  - Used aggregate accessibilities.  Should be checked.
  - CLEANUP: can remove unused coefficients from coefficients file 

---
Free Parking
* Deleted work county specific ASCs in _free_parking.csv_
  - The preprocessor only had one line in it to calculate the work county, so after commenting it out, it crashed because there were no longer any expressions.  I deleted the call to the preprocessor in _free_parking.yaml_ and removed the work county definitions.  Also removed _free_parking_annotate_persons_preprocessor.csv_ from repo
---
Telecommute frequency
* commented out occupancy related varibales (df.occup), since I am not sure about the field in the person file. It is likely "occsoc5" that needs to be parsed for the first two digits?
* currently, only the 2016 calibration constant included, while the ct-ramp UEC has the constants based on scenario year. To be fixed later with EMME integration.
---
CDAP
* Took configs from WSP's MTC work
* MTC spec had retail accessibilities by auto occupancy and crossed it with person type.  Replaced this with disaggregate shopping accessibility and collapsed since coefficients accross occupancies were the same. (This appears in _cdap_indiv_and_hhsize1.csv_ and in _cdap_joint_tour_coefficients.csv_)
* Commented out expressions using `building_size`.  This is a variable included in MTC's input data.
* Had to add term to turn off M pattern for workers working from home (otherwise you get a zero probability error in MTF).
  - Segmented cdap_fixed_relative_proportions.csv according to this discussion: https://github.com/ActivitySim/activitysim/discussions/619
  - This happens because individual utilities are only calculated for the first 5 persons in the hh based on cdap rank (see util\cdap.py line 1170 with cut on MAX_HHSIZE)
---
Joint Tour Frequency Composition
* Took configs from WSP's MTC work
* accessibilities were segemented by auto occupancy.  Removed the segmentation and swapped the disaggregate shopping_accessibility for shopping and maintenance, and the othdisc_accessibility for other discretionary.
---
Mandatory tour frequency
* `schoolathome` row commented out for now
* escort related variables commented out for now (waiting for school dropoff/pickup model)   
---
Non-Mandatory tour frequency
* Took configs from WSP's MTC work
* Commented out lines referring to `educational_attainment`
* Commented out lines referring to `building_size`
* Removed acessibilities by auto occupancy and added disaggregate accessibilities in where appropriate.  Should be reviewed.
* Used `popden` in landuse file for population density. Does this match MTC's units?
---
Tour and Trip Scheduling
* Replaced all tour and trip scheduling files with SEMCOG versions
  - Updated the annotate person and houssehold files to calculate varibles used
* FIXME Had to change the coef_unavailable to coef_unlikely (-50) for escort tours.  This arose from the second work tour getting scheduled over pure escort school tour.  Need to update availability conditions / escort bundle creation in school escorting model to look out for second mandatory tour.
---
Tour destination choice
* time pressure variable commented out in non-mandatory tour destination choice (max_window problematic for now -- to be fixed)
* escort-related variables commented out in non-mandatory tour destination choice (waiting for school pickup/dropoff model)
* nonMandatoryAccessibilityAlt related variable in non-mandatory tour destination choice commented out for now
* @bestTimeToWorkLocation is same as roundtrip_auto_time_to_work? or divided by 2?
---
Tour mode choice
* changed nesting structure in tour_mode_choice.yaml to match UEC. Set all nesting coefs to 0.5 in coeffs file (also matches UEC). Should be double checked.
* Changed parking cost to use `hparkcost`.  Expressions available for both on and off peak parking, but only one hourly parking cost is available in the landuse file, so they evaluate to the same value
  - same in trip mode choice
* `walk_time_skims_[out,in]` both use the od walkTime skim (assumes walk symmetry).  
  - Would need to add the do_skim to the skim roster tour_mode_choice.py ActivitySim code.
  - walkTime skim has too small of value (see meeting notes & slides for 1/17/23), so put in placeholder
* Access and egress times from maz to stop need to be integrated for 2 zone model!
* `milestocoast` was not defined, replaced with arbitrary value of 5
  - applied in trip mode choice too
* None of the AV related constants are defined.  Commenting them out in prepreocessor for now or adding placeholders where unavoidable
* `freeParkingEligibility` is not defined, comes from not-implemented employer parking provision and reimbursement model. Set to 0 as placeholder.
  - same with `reimburseProportion`
  - `reimbursePurpose` is just set to 1... seems pointless?
* Double check units for parking costs in landuse
* origin and destination terminal times (`oTermTime, dTermTime`) are not included in landuse data, setting to 0 as placeholder until it can be joined
* Had to restructure calculation of `parkingCostBeforeReimb` in preprocessor. check to make it is still consistent with UEC
---
Stop frequency
* check the tod used in the calibration lines in the uec
* accessibility line is commeneted out in the uecs
---
Trip purpose
* `depart_range_start` and `depart_range_end` columns need to be updated to match the new 48 half-hour time periods. Values were changed to the following mapping
  - 5,8 -> 1,12   for outbound work (bin 12 = 8:30-9am)
  - 9,23 -> 13,48 for outbound work
  - 5,14 -> 1,24  for inbound work (bin 24 = 2:30-3pm)
  - 15,23 -> 25,48  for inbound work
  - all other purposes didn't differentiate by time of day: 5,23 -> 1,48
---
Trip Destination
* `DISTBIKE` and `DISTWALK` changed to `DIST` in _trip_destination_sample.csv_ while waiting on bike and walk distance skims
---
Trip Purpose and Destination
* Set the number of iterations 2 to instead of 5 because it was taking forever to run.  These two models are currently not consistent and are failing a lot of trips.
---
Trip Mode Choice
* origin and destination terminal times (`oTermTime, dTermTime`) are not included in landuse data, setting to 0 as placeholder until it can be joined
* Removed the following variables and associated calculations since they were not used anywhere:
  - `total_terminal_time`
  - `daily_parking_cost` (not to be confused with `parkingCost` talked about in the next bullet)
  - `trip_topology`
  - `origin_density_index`
* Parking cost calculations were commented out and `parkingCost` set to 0 for now
  - Need to add things like `reimburseAmount`, tour destination parking cost (different than landuse with tour destination, i.e. is reimbursement included?), `freeOnsite`, etc.
* `oMGRAMix, dMGRAMix` calculations updated to use landuse data like in tour mode choice
* Making note of this FIXME: FIXME no transit subzones so all zones short walk to transit
* `time_factor` not defined, setting to 1 for now
* `coef_cost` not defined, using tour mode choice value of -.001 for now
* copied `cost_share_s2, cost_share_s3, vot_threshld_low, vot_threshold_high, max_walk_time` variables from _tour_mode_choice.yaml_ to _trip_mode_choice.yaml_
* `is_mandatory` variable in parking calculation costs was created.  Does this expression make sense?
* Added AV and TNC/TAXI related constants to _constants.yaml_
  - had to make placeholder for `useAV` False for now
* Bike logsum calculations commented out
  - set bike availability placeholder to true for now
* Should we move the SR2 and SR3 cost factors to _constants.yaml_?
* Determined tour mode by using hard coded tour mode names in pre-processor.  Do we want to pull these out into the constants / yaml files?
* Changed walk available expression to check only against max walk threshold
* Added `costPerMile` constant to _trip_mode_choice.yaml_ and set to 0.05 as placeholder
* Does the 'DTW' logsum calculation need to be 'WTD' if the trip is inbound?
  - Do we need to change the tvpb settings to make this happen?
* Utility calculations with `parkingArea` commented out -- no `parkingArea` variable and didn't see corresponding expression in trip mode choice uec.
* `coef_bikeTime` in the coefficients temlate is not being used. Is there an expression missing? Commented it out for now.
* check expression for `dest_zone_sharedTNC_wait_time_mean`, I made a change to match the other similar variables and left the old one there for review.
---
Parking location Choice
* Only allows people to park in zone with `is_parking_zone`.  This was set to true in annotate_landuse.csv when `parkarea` > 0.  Is this the correct filter?
---
Write Trip Matrices
* Modified to be consistent with new tour and trip modes and time period definitions
  - Do we want separate demand tables for drive transit modes?
---
External Model Development
* Implemented external worker and student identification models
  - Determines whether each worker or student is internal or external
  - Set `CHOOSER_FILTER_COLUMN_NAME: is_internal_student/worker` in school and workplace location yamls to exclude external workers or students from the model
  - Added `is_internal_[student,worker]` to annotate_person file with a default of everyone being internal.  Gets overwritten by external identification models.  This means that we can theoretically choose to turn off the external models and not have to make changes in other configs
* Implemented external workplace and school location models
  - shadow pricing is skipped for external workplace or external school location
* Added `external` options in _shadow_pricing.yaml_ in order to get size terms passed to the calculations
  - Added an `external` segment to _destination_choice_size_terms.csv_ which is currently looking for two landuse columns `external_work`, and `external_nonwork`
    - data was added to landuse using the _input_file_creation.ipynb_ notebook
  - _annotate_persons_after[workplace,school].csv_ files set school and workplace zone id to be the external zones if the worker/student was external.  (internal models overwrite the school or workplace ids, so results need to be performed after)
  - Again using placeholder utility specifications.
  - There is no worker or student segmentation (e.g. by income, school level, etc.)
* joint and non-mandatory identification models use the same configs.  Added a joint tour term to the spec though.
* identification models have placeholder utility terms.  there is an ASC that you can play with for testing / calibration before phase 2 estimation work
* external non_mandatory tour destination model just uses the same config as the internal model.  The size terms are 1 for the external_nonwork landuse column.
  - regular internal joint tour model just calls the non_mandatory tour destination settings.  Doing the same thing for external joint tours -- just inherit all the settings from the external non_mandatory tour destination model, which in this case, just uses the internal non_mandatory tour destination configs
* external tour destination choice models are run after the internal destination choice models.  Source code has hard-coded cut on tour_category to select choosers, so they have to be run first.  Subsequent external models overwrite choice. Could _maybe_ get the internal models to operate only in internal tours by playing games with the tour category variable, but it would likely be pretty hacky...
