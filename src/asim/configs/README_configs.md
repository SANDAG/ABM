This readme is a record of things that might want to be fixed or changed in subsequent versions


---
Input Data Prep:

* Took maz and tap files from xborder implementation
* Had to add an extra 'DIST' skim to call for school and work location choice (check out skim lists for other example models)
  - attempting to call SOV_TR_M_DIST__AM skim directly in school location choice model (via multiple methods) did not succeed
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
_school_location.csv_ and _school_location_sample.csv_

* changed `_DIST@skims['DIST']` to `_DIST@skims[('SOV_TR_M_DIST__AM', 'MD')]`.

---
Tour mode choice
* added coefficients to template that were missing: cost_coef
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
* nesting coefficients for AUTO_[DRIVEALONE, SHAREDRIDE2, SHAREDRIDE3] were missing in template and coefs files. They were added and set to 0.5.  Nesting coefficients need to be checked!!
* `coef_income` and `income_exponent` are missing.  Used to calcualte cost_coef.  Added to pre-processor and set to arbitrary values.  Needs to be addressed!!
