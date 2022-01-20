This readme is a record of things that might want to be fixed or changed in subsequent versions


---
Input Data Prep:

* Took maz and tap files from xborder implementation
* Had to add an extra `DIST` skim to call for school and work location choice (check out skim lists for other example models)
  - attempting to call `SOV_TR_M_DIST__AM` skim directly in school location choice model (via multiple methods) did not succeed

---
Changes in mutiple files
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
---
_settings.yaml_

* re-named columns in households and persons
* Kept all columns from landuse.  Decreasing the number of landuse columns might make a substantial impact on RAM requirements as they are often jointed to the person and hh tables

---
_network_los.yaml_

* No drive or bike to maz skims are included.  They have pre-processors that look like aren't ever being called?
  - Cropping script currently has drive and bike to maz skims commented out since they do not exist
* Added accessibility settings (not present in xborder model) and updated for SANDAG skim file names / columns
  - changed `df.walk_time` to `df.walkTime` in _tvpb_accessibility_walk_maz_tap.csv_
* copied `tvpb_accessibility_tap_tap_.csv` from SEMCOG three zone implementation and updated skim names
* Does it make sense that `max_paths_across_tap_sets: 4` and `max_paths_per_tap_set: 1`?
* Added `demographic_segments`.  They are called in _tour_mode_choice_.  Looks like the cross border model "fakes" these segments by defining them in it's _annotate_persons.csv_
  - want to the `TVPB_demographic_segments_by_income_segment` related constants to _constants.csv_? (they were taken from SEMCOG and values should be checked)

---

_annotate_households.csv_

* `home_is_urban` and `home_is_rural` values are now computed based on `pseudomsa` instead of the previous `area_type` variable that exists in other examples.  Is there a better substitute for this?

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
Tour mode choice
* added coefficients to template that were missing: `cost_coef`,
* changed expression to include the following coefficients and changed the actual coefficient to `coef_one`: `c_wacc`, `c_wegr`, `c_rel_inb`, `c_rel_oub`, `c_fwt`
  - these were causing crash because they were defined in the pre-processor, but were listed as actual coefficients
* commented out lines in _tour_mode_choice.csv_ that were missing constants defined:
  - `coef_age65p_sr2`
  - `coef_hhsize`
  - `coef_cost_out`
  - `coef_walkTime` (This one will be a little more complicated because it is derived on c_ivt, but varies by purpose.  Implementation will probably have to include it as a fixed coef in the coef files.)
* there were a couple coefficients that I think were just typos.  They were used in defined in tour_mode_choice, but defined differently in coef files:
  - `coef_age[1624,4155,5664]p_[sr2,sr3]` instead of `coef_age[1624,4155,5664]_[sr2,sr3]`
  - `coef_age65pl_sr2` instead of `coef_age65p_sr2`
  - `coef_age65pl_sr3p` instead of `coef_age65pl_sr3`
  - `coef_age[1624,4155,5664]_sr3p` instead of `coef_age[1624,4155,5664]_sr3`
  - `coef_female_sr3p` instead of `coef_female_sr3`
* changed nesting structure in tour_mode_choice.yaml to match UEC. Set all nesting coefs to 0.5 in coeffs file (also matches UEC). Should be double checked.
* `coef_income` was missing.  Used to calcualte cost_coef in pre-preocessor.  Added to template and coefs files and set to arbitrary value of 0.05.  Needs to be addressed!!
* Changed parking cost to use `hparkcost`.  Expressions available for both on and off peak parking, but only one hourly parking cost is available in the landuse file, so they evaluate to the same value
* `tour_type` is not yet decided when calculating logsums, so used df.get() function and set default to 'work'.
  - Same with `tour_category`, although default varied cause it was used in multiple places
* Added `coef_income` to _tour_mode_choice_coefficients.csv_ and set to arbitrary value of 0.5 and fixed to true
* Copied `cost_share_[s2,s3]`, `vot_threshold_[low,med], maz_walk_time` from xborder model to tour_mode_choice.yaml.  Should these be moved to (common) constants?
* `df.transponderOwnership` is not recognized in the preprocessor since there is no Transponder model yet implemented.  Setting `ownsTransponder` to 1 for now.
* `walk_time_skims_[out,in]` need to use the walktime skims, but we don't have that. Putting placeholder in for now based on `SOV_TR_L_DIST * 3 / 60`
* `milestocoast` was not defined, replaced with arbitrary value of 5
* All of the bike logsums and availability stuff is not defined. commenting out for now.
* None of the AV related constants are defined.  Commenting them out in prepreocessor for now or adding placeholders where unavoidable
* `freeParkingEligibility` is not defined, comes from not-implemented employer parking provision and reimbursement model. Set to 0 as placeholder.
  - same with `reimburseProportion`
  - `reimbursePurpose` is just set to 1... seems pointless?
* Double check units for parking costs in landuse
* origin and destination terminal times (`oTermTime, dTermTime`) are not included in landuse data, setting to 0 as placeholder until it can be joined
* Had to restructure calculation of `parkingCostBeforeReimb` in preprocessor. check to make it is still consistent with UEC
* double check `walkAvailable` function is correct in preprocessor
* `[WLK,PNR,KNR]_available` conditions need to call the TVPB logsums correctly. They are all turned on as a placeholder
* `DTW, NTW, and KTW` TVPB settings are not set up yet.  Put in WTW for all modes for now as placeholder
* `sharedTNCIVTFactor` not defined.  Set to 1 as placeholder in preprocessor
