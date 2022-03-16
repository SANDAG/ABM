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

---
Workplace Location Choice
* Changed `DISTWALK` to `DIST` in _annotate_persons_workplace.csv_ while waiting on walk distance skim
* Used MD `SOV_TR_M_TIME` skim in _annotate_persons_workplace.csv_ for time from home to work

---
Auto Ownership
* Deleted county specific ASCs in _auto_ownership.csv_

---
Free Parking
* Deleted work county specific ASCs in _free_parking.csv_
  - The preprocessor only had one line in it to calculate the work county, so after commenting it out, it crashed because there were no longer any expressions.  I deleted the call to the preprocessor in _free_parking.yaml_ and removed the work county definitions.  Also removed _free_parking_annotate_persons_preprocessor.csv_ from repo
---
Telecommute frequency
* commented out occupancy related varibales (df.occup), since I am not sure about the field in the person file. It is likely "occsoc5" that needs to be parsed for the first two digits?
* currently, only the 2016 calibration constant included, while the ct-ramp UEC has the constants based on scenario year. To be fixed later with EMME integration.
---
Mandatory tour frequency
* `schoolathome` row commented out for now
* escort related variables commented out for now (waiting for school dropoff/pickup model)   
---
Tour and Trip Scheduling
* Replaced all tour and trip scheduling files with SEMCOG versions
  - Updated the annotate person and houssehold files to calculate varibles used
---
Tour destination choice
* time pressure variable commented out in non-mandatory tour destination choice (max_window problematic for now -- to be fixed)
* escort-related variables commented out in non-mandatory tour destination choice (waiting for school pickup/dropoff model)
* nonMandatoryAccessibilityAlt related variable in non-mandatory tour destination choice commented out for now
* @bestTimeToWorkLocation is same as roundtrip_auto_time_to_work? or divided by 2?
---
Tour mode choice
* added coefficients to template that were missing: `cost_coef`, FIXED (COT_COEF WAS NOT NEEDED, REPLACED WITH COEF_INCOME BASED CALCULATION)
* changed expression to include the following coefficients and changed the actual coefficient to `coef_one`: `c_wacc`, `c_wegr`, `c_rel_inb`, `c_rel_oub`, `c_fwt`
  - these were causing crash because they were defined in the pre-processor, but were listed as actual coefficients
* commented out lines in _tour_mode_choice.csv_ that were missing constants defined:
  - `coef_hhsize` FIXED
  - `coef_walkTime` (This one will be a little more complicated because it is derived on c_ivt, but varies by purpose.  Implementation will probably have to include it as a fixed coef in the coef files.  Using placeholder for now because without walktime, lots of tour modes were walk.) FIXED
* there were a couple coefficients that I think were just typos.  They were used in defined in tour_mode_choice, but defined differently in coef files: FIXED (THEY WERE TYPOS!THANKS)
  - `coef_age[1624,4155,5664]p_[sr2,sr3]` instead of `coef_age[1624,4155,5664]_[sr2,sr3]`
  - `coef_age65pl_sr2` instead of `coef_age65p_sr2`
  - `coef_age65pl_sr3p` instead of `coef_age65pl_sr3`
  - `coef_age[1624,4155,5664]_sr3p` instead of `coef_age[1624,4155,5664]_sr3`
  - `coef_female_sr3p` instead of `coef_female_sr3`
* changed nesting structure in tour_mode_choice.yaml to match UEC. Set all nesting coefs to 0.5 in coeffs file (also matches UEC). Should be double checked.
* `coef_income` was missing.  Used to calcualte cost_coef in pre-preocessor.  Added to template and coefs files and set to arbitrary value of 0.05.  Needs to be addressed!! FIXED
* Changed parking cost to use `hparkcost`.  Expressions available for both on and off peak parking, but only one hourly parking cost is available in the landuse file, so they evaluate to the same value
  - same in trip mode choice
* `tour_type` is not yet decided when calculating logsums, so used df.get() function and set default to 'work'.
  - Same with `tour_category`, although default varied cause it was used in multiple places (GOOD TO KNOW)
* Added `coef_income` to _tour_mode_choice_coefficients.csv_ and set to arbitrary value of 0.5 and fixed to true FIXED
* Copied `cost_share_[s2,s3]`, `vot_threshold_[low,med], maz_walk_time` from xborder model to tour_mode_choice.yaml.  Should these be moved to (common) constants?
* `df.transponderOwnership` is not recognized in the preprocessor since there is no Transponder model yet implemented.  Setting `ownsTransponder` to 1 for now.
  - same in trip mode choice
* `walk_time_skims_[out,in]` need to use the walktime skims, but we don't have that. Putting placeholder in for now based on `SOV_TR_L_DIST * 3 / 60`
* `milestocoast` was not defined, replaced with arbitrary value of 5, TBD: CANNOT FIND THE VALUE OF THIS VARIABLE ANYWHERE MYSELF
  - applied in trip mode choice too
* All of the bike logsums and availability stuff is not defined. commenting out for now.
* None of the AV related constants are defined.  Commenting them out in prepreocessor for now or adding placeholders where unavoidable
* `freeParkingEligibility` is not defined, comes from not-implemented employer parking provision and reimbursement model. Set to 0 as placeholder.
  - same with `reimburseProportion`
  - `reimbursePurpose` is just set to 1... seems pointless?
* Double check units for parking costs in landuse
* origin and destination terminal times (`oTermTime, dTermTime`) are not included in landuse data, setting to 0 as placeholder until it can be joined
* Had to restructure calculation of `parkingCostBeforeReimb` in preprocessor. check to make it is still consistent with UEC
* double check `walkAvailable` function is correct in preprocessor, I THINK IT IS RIGHT
* `[WLK,PNR,KNR]_available` conditions need to call the TVPB logsums correctly. They are all turned on as a placeholder
  - large logsum values in the utility expression should make these roughly irrelevant
* `DTW` path is being used for PNR, KNR, and TNC.  maz to taz drive times only include taps connected to parking lots, meaning knr and tnc are restricted to only these taps
* Renaming of tour purposes in _tour_mode_choice_coefficients_template.csv_
  - Copied the maint column into three separate columns named escort, shopping, and othmaint
  - Copied the disc column into three separate columns named eatout, social, othdiscr
  - This was needed because all of the other models define different purposes and need to calculate logsums for each purpose. ActivitySim will crash if the purpose names do not match.
  - Since the columns were just copied and all of the coefficient names remain the same, the utility calculated for each of the maint purposes will be the same (and similarly for disc).
  - Since thing was applied in trip mode choice
  - Note that this has implications when performing estimation
* `coef_cost_out` (not defined) was replaced with `coef_cost` in the MAAS utility calculations. Is this correct? FIXED
* Made some changes to bike logsum calculations
  - Old version had different logsums for male and female. Input data only has one logsum.  Removed segmentation by gender.
  - Bike logsums were separated by inbound and outbound.  Since they don't vary by time of day, and if the path is the same, shouldn't the inbound bike logsum = outbound bike logsum?
     - This change was made because it is easy to access _od_skims_, but no _do_skims_ object exists (only _dot_skims_)
     - The logsum matrix is not symmetric about the diagonal, so this assumption doesn't really hold....
  - Bike is available when the sum of inbound and outbound logsums < -999
  - Bike time is not being used. bike time coefficient is calculated in pre-processor, but is not used anywhere... MANY VARIABLE ARE DEFINED BUT NOT USED IN abm2+, AND THIS IS ONE. 
  - Same in trip mode choice except there's no inbound / outbound distinction
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
Write Trip Matrices
* Modified to be consistent with new tour and trip modes and time period definitions
  - Do we want separate demand tables for drive transit modes?
---

