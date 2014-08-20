-- scenario_scheme IS SET AS THE ON FOR EVERY TABLE


-- AT_SKIMS
IF OBJECT_ID('abm.at_skims','U') IS NULL
BEGIN
	
CREATE TABLE 
	[abm].[at_skims] (
		[scenario_id] smallint NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[orig] smallint NOT NULL,
    	[dest] smallint NOT NULL,
    	[mode_id] tinyint NOT NULL,
    	[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
    	[skim_id] tinyint NOT NULL,
    	[value] decimal(8,4),
		CONSTRAINT pk_atskims PRIMARY KEY ([scenario_id],[geography_type_id],[orig],[dest],[mode_id],[time_resolution_id],[time_period_id],[skim_id]),
		CONSTRAINT fk_atskims_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_atskims_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_atskims_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_atskims_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_atskims_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_atskims_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
		CONSTRAINT fk_atskims_skim FOREIGN KEY ([skim_id]) REFERENCES [ref].[skim] ([skim_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- BIKE_LINK
IF OBJECT_ID('abm.bike_link','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[bike_link] (
		[scenario_id] smallint NOT NULL,
		[roadsegid] int NOT NULL,
		[nm] varchar(50),
		[at_func_class_id] smallint,
		[bike2sep] tinyint,
		[bike3blvd] tinyint,
		[speed] smallint,
		[parkarea] decimal(13,6),
		[seaview] tinyint,
		[scenicldx] decimal(11,9),
		[shape] geometry,
		CONSTRAINT pk_bikelink PRIMARY KEY ([scenario_id],[roadsegid]),
		CONSTRAINT fk_bikelink_atfuncclass FOREIGN KEY  ([at_func_class_id]) REFERENCES [ref].[at_func_class]([at_func_class_id])
	)
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END



-- BIKE_LINK_AB
IF OBJECT_ID('abm.bike_link_ab','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[bike_link_ab] (
		[scenario_id] smallint NOT NULL,
		[roadsegid] int NOT NULL,
		[ab] tinyint NOT NULL,
		[from_node] int,
		[to_node] int,
		[gain] smallint,
		[bike_class_id] tinyint,
		[lanes] tinyint,
		[signal] tinyint,
		CONSTRAINT pk_bikelinkab PRIMARY KEY ([scenario_id],[roadsegid],[ab]),
		CONSTRAINT fk_bikelinkab_bikeclass FOREIGN KEY ([bike_class_id]) REFERENCES [ref].[bike_class]([bike_class_id])
	)
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- BIKE_FLOW
IF OBJECT_ID('abm.bike_flow','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[bike_flow] (
		[scenario_id] smallint NOT NULL,
		[roadsegid] int NOT NULL,
		[ab] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[flow] decimal(8,4),
		CONSTRAINT pk_bikeflow PRIMARY KEY ([scenario_id],[roadsegid],[ab],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_bikeflow_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_bikeflow_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id])
	)
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- CBD_VEHICLES
IF OBJECT_ID('abm.cbd_vehicles','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[cbd_vehicles] (
		[scenario_id] smallint NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[zone] smallint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[vehicles] decimal(10,6),
		CONSTRAINT pk_cbdvehicles PRIMARY KEY ([scenario_id],[geography_type_id],[zone],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_cbdvehicles_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_cbdvehicles_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_cbdvehicles_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_cbdvehicles_zone FOREIGN KEY ([geography_type_id],[zone]) REFERENCES [ref].[geography_zone]([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- HWY_FLOW
IF OBJECT_ID('abm.hwy_flow','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_flow] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[flow_pce] decimal(12,6),
		[time] decimal(10,6),
		[voc] decimal(8,6),
		[v_dist_t] decimal(12,6),
		[vht] decimal(11,6),
		[speed] decimal(9,6),
		[vdf] decimal(10,6),
		[msa_flow] decimal(12,6),
		[msa_time] decimal(10,6),
		[flow] decimal(12,6),
		CONSTRAINT pk_hwyflow PRIMARY KEY ([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwyflow_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_hwyflow_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END			
		
		
-- HWY_FLOW_MODE
IF OBJECT_ID('abm.hwy_flow_mode','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_flow_mode] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[mode_id] tinyint NOT NULL,
		[flow] decimal(12,6),
		CONSTRAINT pk_hwyflowmode PRIMARY KEY ([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id],[mode_id]),
		CONSTRAINT fk_hwyflowmode_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_hwyflowmode_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwyflowmode_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END			


-- HWY_LINK
IF OBJECT_ID('abm.hwy_link','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_link] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[length_mile] decimal(20,16),
		[sphere] smallint,
		[nm] varchar(50),
		[cojur] tinyint,
		[costat] smallint,
		[coloc] tinyint,
		[rloop] smallint,
		[adtlk] smallint,
		[adtvl] smallint,
		[aspd] tinyint,
		[iyr] smallint,
		[iproj] smallint,
		[ijur] tinyint,
		[ifc] tinyint,
		[ihov] tinyint,
		[itruck] tinyint,
		[ispd] tinyint,
		[iway] tinyint,
		[imed] tinyint,
		[shape] geometry,
		CONSTRAINT pk_hwylink PRIMARY KEY ([scenario_id],[hwycov_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- HWY_LINK_AB
IF OBJECT_ID('abm.hwy_link_ab','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_link_ab] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[from_node] smallint,
		[to_node] smallint,
		[from_nm] varchar(50),
		[to_nm] varchar(50),
		[au] tinyint,
		[pct] tinyint,
		[cnt] tinyint,
		CONSTRAINT pk_hwylinkab PRIMARY KEY ([scenario_id],[hwycov_id],[ab]),
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- HWY_LINK_AB_TOD
IF OBJECT_ID('abm.hwy_link_ab_tod','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_link_ab_tod] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[cp] decimal(12,6),
		[cx] decimal(12,6),
		[tm] decimal(12,6),
		[tx] decimal(12,6),
		[ln] decimal(12,6),
		[stm] decimal(12,6),
		[htm] decimal(12,6),
		[preload] decimal(12,6),
		CONSTRAINT pk_hwylinkabtod PRIMARY KEY ([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwylinkabtod_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_hwylinkabtod_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]) 
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- HWY_LINK_TOD
IF OBJECT_ID('abm.hwy_link_tod','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[hwy_link_tod] (
		[scenario_id] smallint NOT NULL,
		[hwycov_id] int NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[itoll2] decimal(12,6),
		[itoll3] decimal(12,6),
		[itoll4] decimal(12,6),
		[itoll5] decimal(12,6),
		[itoll] decimal(12,6),
		CONSTRAINT pk_hwylinktod PRIMARY KEY ([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwylinktod_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_hwylinktod_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]) 
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- HWY_SKIMS
IF OBJECT_ID('abm.hwy_skims','U') IS NULL
BEGIN
	
CREATE TABLE 
	[abm].[hwy_skims] (
		[scenario_id] smallint NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[orig] smallint NOT NULL,
    	[dest] smallint NOT NULL,
    	[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
    	[dist_drive_alone_toll] decimal(12,6),
    	[time_drive_alone_toll] decimal(12,6),
    	[cost_drive_alone_toll] decimal(4,2),
    	[dist_drive_alone_free] decimal(12,6),
    	[time_drive_alone_free] decimal(12,6),
   		[dist_hov2_toll] decimal(12,6),
    	[time_hov2_toll] decimal(12,6),
   		[cost_hov2_toll] decimal(4,2),
    	[dist_hov2_free] decimal(12,6),
    	[time_hov2_free] decimal(12,6),
    	[dist_hov3_toll] decimal(12,6),
    	[time_hov3_toll] decimal(12,6),
    	[cost_hov3_toll] decimal(4,2),
    	[dist_hov3_free] decimal(12,6),
    	[time_hov3_free] decimal(12,6),
    	[dist_truck_toll] decimal(12,6),
    	[time_truck_toll] decimal(12,6),
   		[cost_truck_toll] decimal(4,2),
    	[dist_truck_free] decimal(12,6),
    	[time_truck_free] decimal(12,6),
		CONSTRAINT pk_hwyskims PRIMARY KEY ([scenario_id],[geography_type_id],[orig],[dest],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwyskims_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_hwyskims_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_hwyskims_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_hwyskims_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_hwyskims_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- LU_HH
IF OBJECT_ID('abm.lu_hh','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[lu_hh] (
		[scenario_id] smallint NOT NULL,
		[hh_id] int NOT NULL,
		[hh_serial_no] bigint,
		[geography_type_id] tinyint,
		[zone] smallint,
		[hh_income_cat_id] tinyint,
		[hh_income] int,
		[workers] tinyint,
		[persons] tinyint,
		[unit_type_id] tinyint,
		[autos] tinyint,
		[transponder] tinyint,
		[poverty] decimal(6,3),
		[version] smallint,
		CONSTRAINT pk_luhh PRIMARY KEY ([scenario_id],[hh_id]),
		CONSTRAINT fk_luhh_hhincomecat FOREIGN KEY ([hh_income_cat_id]) REFERENCES [ref].[hh_income_cat]([hh_income_cat_id]),
		CONSTRAINT fk_luhh_unittype FOREIGN KEY ([unit_type_id]) REFERENCES [ref].[unit_type]([unit_type_id]),
		CONSTRAINT fk_luhh_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_luhh_zone FOREIGN KEY ([geography_type_id],[zone]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- LU_MGRA_INPUT
IF OBJECT_ID('abm.lu_mgra_input','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[lu_mgra_input] (
		[scenario_id] smallint,
		[geography_type_id] tinyint,
		[zone] smallint,
		[hs] smallint,
		[hs_sf] smallint,
		[hs_mf] smallint,
		[hs_mh] smallint,
		[hh] smallint,
		[hh_sf] smallint,
		[hh_mf] smallint,
		[hh_mh] smallint,
		[gq_civ] smallint,
		[gq_mil] smallint,
		[i1] smallint,
		[i2] smallint,
		[i3] smallint,
		[i4] smallint,
		[i5] smallint,
		[i6] smallint,
		[i7] smallint,
		[i8] smallint,
		[i9] smallint,
		[i10] smallint,
		[hhs] decimal(9,7),
		[pop] smallint,
		[hhp] smallint,
		[emp_ag] decimal(12,7),
		[emp_const_non_bldg_prod] decimal(12,7),
		[emp_const_non_bldg_office] decimal(12,7),
		[emp_utilities_prod] decimal(12,7),
		[emp_utilities_office] decimal(12,7),
		[emp_const_bldg_prod] decimal(12,7),
		[emp_const_bldg_office] decimal(12,7),
		[emp_mfg_prod] decimal(12,7),
		[emp_mfg_office] decimal(12,7),
		[emp_whsle_whs] decimal(12,7),
		[emp_trans] decimal(12,7),
		[emp_retail] decimal(12,7),
		[emp_prof_bus_svcs] decimal(12,7),
		[emp_prof_bus_svcs_bldg_maint] decimal(12,7),
		[emp_pvt_ed_k12] decimal(12,7),
		[emp_pvt_ed_post_k12_oth] decimal(12,7),
		[emp_health] decimal(12,7),
		[emp_personal_svcs_office] decimal(12,7),
		[emp_amusement] decimal(12,7),
		[emp_hotel] decimal(12,7),
		[emp_restaurant_bar] decimal(12,7),
		[emp_personal_svcs_retail] decimal(12,7),
		[emp_religious] decimal(12,7),
		[emp_pvt_hh] decimal(12,7),
		[emp_state_local_gov_ent] decimal(12,7),
		[emp_fed_non_mil] decimal(12,7),
		[emp_fed_mil] decimal(12,7),
		[emp_state_local_gov_blue] decimal(12,7),
		[emp_state_local_gov_white] decimal(12,7),
		[emp_public_ed] decimal(12,7),
		[emp_own_occ_dwell_mgmt] decimal(12,7),
		[emp_fed_gov_accts] decimal(12,7),
		[emp_st_lcl_gov_accts] decimal(12,7),
		[emp_cap_accts] decimal(12,7),
		[emp_total] decimal(12,7),
		[enrollgradekto8] decimal(12,7),
		[enrollgrade9to12] decimal(12,7),
		[collegeenroll] decimal(12,7),
		[othercollegeenroll] decimal(12,7),
		[adultschenrl] decimal(12,7),
		[ech_dist] int,
		[hch_dist] int,
		[pseudomsa] tinyint,
		[parkarea] tinyint,
		[hstallsoth] smallint,
		[hstallssam] smallint,
		[hparkcost] decimal(10,8),
		[numfreehrs] tinyint,
		[dstallsoth] smallint,
		[dstallssam] smallint,
		[dparkcost] decimal(10,8),
		[mstallsoth] smallint,
		[mstallssam] smallint,
		[mparkcost] decimal(10,8),
		[totint] smallint,
		[duden] decimal(11,8),
		[empden] decimal(11,8),
		[popden] decimal(11,8),
		[retempden] decimal(11,8),
		[totintbin] tinyint,
		[empdenbin] tinyint,
		[dudenbin] tinyint,
		[zip09] int,
		[parkactive] decimal(15,10),
		[openspaceparkpreserve] decimal(15,10),
		[beachactive] decimal(15,10),
		[budgetroom] decimal(9,5),
		[economyroom] decimal(9,5),
		[luxuryroom] decimal(9,5),
		[midpriceroom] decimal(9,5),
		[upscaleroom] decimal(9,5),
		[hotelroomtotal] decimal(9,5),
		[luz_id] smallint,
		[truckregiontype] tinyint,
		[district27] tinyint,
		[milestocoast] decimal(12,9),
		CONSTRAINT pk_lumgrainput PRIMARY KEY ([scenario_id],[geography_type_id],[zone]),
		CONSTRAINT fk_lumgrainput_geores FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_lumgrainput_zone FOREIGN KEY ([geography_type_id],[zone]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- LU_PERSON
IF OBJECT_ID('abm.lu_person','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[lu_person] (
		[scenario_id] smallint NOT NULL,
		[hh_id] int NOT NULL,
		[pnum] tinyint NOT NULL,
		[age] tinyint,
		[sex_id] tinyint,
		[military_id] tinyint,
		[pemploy_id] tinyint,
		[pstudent_id] tinyint,
		[ptype_id] tinyint,
		[educ_id] tinyint,
		[grade_id] tinyint,
		[occen5] smallint,
		[occsoc5] varchar(15),
		[indcen] smallint,
		[weeks_worked_id] tinyint,
		[hours_worked] tinyint,
		[race_id] tinyint,
		[hisp_id] tinyint,
		[activity_pattern_id] tinyint,
		[ie_choice] tinyint,
		[version] smallint,
		CONSTRAINT pk_luperson PRIMARY KEY ([scenario_id],[hh_id],[pnum]),
		CONSTRAINT fk_luperson_sex FOREIGN KEY ([sex_id]) REFERENCES [ref].[sex] ([sex_id]),
		CONSTRAINT fk_luperson_military FOREIGN KEY ([military_id]) REFERENCES [ref].[military] ([military_id]),
		CONSTRAINT fk_luperson_pemploy FOREIGN KEY ([pemploy_id]) REFERENCES [ref].[pemploy] ([pemploy_id]),
		CONSTRAINT fk_luperson_pstudent FOREIGN KEY ([pstudent_id]) REFERENCES [ref].[pstudent] ([pstudent_id]),
		CONSTRAINT fk_luperson_ptype FOREIGN KEY ([ptype_id]) REFERENCES [ref].[ptype] ([ptype_id]),
		CONSTRAINT fk_luperson_educ FOREIGN KEY ([educ_id]) REFERENCES [ref].[educ] ([educ_id]),
		CONSTRAINT fk_luperson_grade FOREIGN KEY ([grade_id]) REFERENCES [ref].[grade] ([grade_id]),
		CONSTRAINT fk_luperson_weeks FOREIGN KEY ([weeks_worked_id]) REFERENCES [ref].[weeks_worked] ([weeks_worked_id]),
		CONSTRAINT fk_luperson_race FOREIGN KEY ([race_id]) REFERENCES [ref].[race] ([race_id]),
		CONSTRAINT fk_luperson_hisp FOREIGN KEY ([hisp_id]) REFERENCES [ref].[hisp] ([hisp_id]),
		CONSTRAINT fk_luperson_actpattern FOREIGN KEY ([activity_pattern_id]) REFERENCES [ref].[activity_pattern] ([activity_pattern_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- LU_PERSON_FP
IF OBJECT_ID('abm.lu_person_fp','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[lu_person_fp] (
		[scenario_id] smallint NOT NULL,
		[hh_id] int NOT NULL,
		[pnum] tinyint NOT NULL,
		[fp_choice_id] tinyint,
		[reimb_pct] decimal(8,6),
		CONSTRAINT pk_lupersonfp PRIMARY KEY ([scenario_id],[hh_id],[pnum]),
		CONSTRAINT fk_lupersonfp_fpchoice FOREIGN KEY ([fp_choice_id]) REFERENCES [ref].[fp_choice] ([fp_choice_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- LU_PERSON_LC
IF OBJECT_ID('abm.lu_person_lc','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[lu_person_lc] (
		[scenario_id] smallint NOT NULL,
		[hh_id] int NOT NULL,
		[pnum] tinyint NOT NULL,
		[loc_choice_id] tinyint NOT NULL,
		[loc_choice_segment_id] tinyint,
		[geography_type_id] tinyint,
		[zone] smallint,
		CONSTRAINT pk_lupersonlc PRIMARY KEY ([scenario_id],[hh_id],[pnum],[loc_choice_id]),
		CONSTRAINT fk_lupersonlc_lc FOREIGN KEY ([loc_choice_id]) REFERENCES [ref].[loc_choice] ([loc_choice_id]),
		CONSTRAINT fk_lupersonlc_segment FOREIGN KEY ([loc_choice_id],[loc_choice_segment_id]) REFERENCES [ref].[loc_choice_segment] ([loc_choice_id],[loc_choice_segment_id]),
		CONSTRAINT fk_lupersonlc_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_lupersonlc_zone FOREIGN KEY ([geography_type_id],[zone]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TOUR_IJ
IF OBJECT_ID('abm.tour_ij','U') IS NULL
BEGIN
	
CREATE TABLE 
	[abm].[tour_ij] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[tour_cat_id] tinyint,
		[purpose_id] tinyint,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[start_period_id] tinyint,
		[end_period_id] tinyint,
		[mode_id] tinyint,
		CONSTRAINT pk_tourij PRIMARY KEY ([scenario_id],[model_type_id],[tour_id]),
		CONSTRAINT fk_tourij_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tourij_tourcat FOREIGN KEY ([tour_cat_id]) REFERENCES [ref].[tour_cat]([tour_cat_id]),
		CONSTRAINT fk_tourij_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id]),
		CONSTRAINT fk_tourij_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tourij_startperiod FOREIGN KEY ([time_resolution_id],[start_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourij_endperiod FOREIGN KEY ([time_resolution_id],[end_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourij_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourij_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tourij_orig FOREIGN KEY ([geography_type_id],[ORIG]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tourij_dest FOREIGN KEY ([geography_type_id],[DEST]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TOUR_CB
IF OBJECT_ID('abm.tour_cb','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[tour_cb] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[purpose_id] tinyint,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[start_period_id] tinyint,
		[end_period_id] tinyint,
		[crossing_mode_id] tinyint,
		[sentri] tinyint,
		[poe_id] tinyint,
		CONSTRAINT pk_tourcb PRIMARY KEY ([scenario_id],[model_type_id],[tour_id]),
		CONSTRAINT fk_tourcb_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tourcb_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id]),
		CONSTRAINT fk_tourcb_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tourcb_startperiod FOREIGN KEY ([time_resolution_id],[start_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourcb_endperiod FOREIGN KEY ([time_resolution_id],[end_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourcb_mode FOREIGN KEY ([crossing_mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourcb_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tourcb_orig FOREIGN KEY ([geography_type_id],[ORIG]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tourcb_dest FOREIGN KEY ([geography_type_id],[DEST]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tourcb_poe FOREIGN KEY ([poe_id]) REFERENCES [ref].[poe]([poe_id])
	)
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TOUR_IJ_PERSON
IF OBJECT_ID('abm.tour_ij_person','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[tour_ij_person] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[hh_id] int NOT NULL,
		[pnum] tinyint NOT NULL,
		CONSTRAINT pk_tourijperson PRIMARY KEY ([scenario_id], [model_type_id], [tour_id], [hh_id], [pnum]),
		CONSTRAINT fk_tourijperson_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TOUR_VIS
IF OBJECT_ID('abm.tour_vis','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[tour_vis] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[tour_cat_id] tinyint,
		[purpose_id] tinyint,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[start_period_id] tinyint,
		[end_period_id] tinyint,
		[mode_id] tinyint,
		[auto_available] tinyint,
		[hh_income_cat_id] tinyint,
		CONSTRAINT pk_tourvis PRIMARY KEY ([scenario_id],[model_type_id],[tour_id]),
		CONSTRAINT fk_tourvis_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tourvis_tourcat FOREIGN KEY ([tour_cat_id]) REFERENCES [ref].[tour_cat]([tour_cat_id]),
		CONSTRAINT fk_tourvis_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id]),
		CONSTRAINT fk_tourvis_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tourvis_startperiod FOREIGN KEY ([time_resolution_id],[start_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourvis_endperiod FOREIGN KEY ([time_resolution_id],[end_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tourvis_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourvis_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tourvis_orig FOREIGN KEY ([geography_type_id],[ORIG]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tourvis_dest FOREIGN KEY ([geography_type_id],[DEST]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tourvis_income FOREIGN KEY ([hh_income_cat_id]) REFERENCES [ref].[hh_income_cat]([hh_income_cat_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TRANSIT_AGGFLOW
IF OBJECT_ID('abm.transit_aggflow','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_aggflow] (
		[scenario_id] smallint NOT NULL,
		[trcov_id] int NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[ab] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[transit_flow] smallint,		
		[non_transit_flow] smallint,	
		[total_flow] smallint,	
		[access_walk_flow] smallint,	
		[xfer_walk_flow] smallint,	
		[egress_walk_flow] smallint,
		CONSTRAINT pk_transitaggflow PRIMARY KEY ([scenario_id],[trcov_id],[ab],[time_resolution_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]),
		CONSTRAINT fk_transitaggflow_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_transitaggflow_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitaggflow_transitmode FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode]([transit_mode_id]),
		CONSTRAINT fk_transitaggflow_accessmode FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRANSIT_FLOW
IF OBJECT_ID('abm.transit_flow','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_flow] (
		[scenario_id] smallint NOT NULL,
		[route_id] smallint NOT NULL,
		[from_stop_id] smallint NOT NULL,
		[to_stop_id] smallint NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[from_mp] decimal(9,6),
		[to_mp] decimal(9,6),
		[baseivtt] decimal(9,6),
		[cost] decimal(9,6),
		[transit_flow] smallint,
		CONSTRAINT pk_transitflow PRIMARY KEY ([scenario_id],[route_id],[from_stop_id],[to_stop_id],[transit_mode_id],[transit_access_mode_id],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitflow_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_transitflow_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitflow_transitmode FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode]([transit_mode_id]),
		CONSTRAINT fk_transitflow_accessmode FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRANSIT_LINK
IF OBJECT_ID('abm.transit_link','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_link] (
		[scenario_id] smallint NOT NULL,
		[trcov_id] int NOT NULL,
		[shape] geometry,
		CONSTRAINT pk_transitlink PRIMARY KEY ([scenario_id],[trcov_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRANSIT_ONOFF
IF OBJECT_ID('abm.transit_onoff','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_onoff] (
		[scenario_id] smallint NOT NULL,
		[route_id] smallint NOT NULL,
		[stop_id] smallint NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[boardings] decimal(11,6),
		[alightings] decimal(11,6),
		[walk_access_on] decimal(11,6),
		[direct_transfer_on] decimal(11,6),
		[walk_transfer_on] decimal(11,6),
		[direct_transfer_off] decimal(11,6),
		[walk_transfer_off] decimal(11,6),
		[egress_off] decimal(11,6),
		CONSTRAINT pk_transitonoff PRIMARY KEY ([scenario_id],[route_id],[stop_id],[transit_mode_id],[transit_access_mode_id],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitonoff_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_transitonoff_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitonoff_transitmode FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode] ([transit_mode_id]),
		CONSTRAINT fk_transitonoff_accessmode FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRANSIT_PNR
IF OBJECT_ID('abm.transit_pnr','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_pnr] (
		[scenario_id] smallint NOT NULL,
		[tap] smallint NOT NULL,
		[lot_id] smallint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[parking_type_id] tinyint,
		[capacity] smallint,
		[distance] smallint,
		[vehicles] smallint,
		CONSTRAINT pk_transitpnr PRIMARY KEY ([scenario_id],[tap],[lot_id],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transitpnr_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_transitpnr_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- TRANSIT_ROUTE
IF OBJECT_ID('abm.transit_route','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_route] (
		[scenario_id] smallint NOT NULL,
		[route_id] smallint NOT NULL,
		[transit_mode_id] tinyint,
		[am_headway] decimal(5,2),
		[pm_headway] decimal(5,2),
		[op_headway] decimal(5,2),
		[nt_headway] decimal(5,2),
		[nt_hour] tinyint,
		[config] int,
		[fare] decimal(4,2),
		[shape] geometry,
		CONSTRAINT pk_transitroute PRIMARY KEY ([scenario_id],[route_id]),
		CONSTRAINT fk_transitroute_transitmode FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode]([transit_mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TRANSIT_STOP
IF OBJECT_ID('abm.transit_stop','U') IS NULL
BEGIN
	
CREATE TABLE 
	[abm].[transit_stop] (
		[scenario_id] smallint NOT NULL,
		[stop_id] smallint NOT NULL,
		[route_id] smallint,
		[trcov_id] int,
		[mp] decimal(9,6),
		[near_node] int,
		[fare_zone] smallint,
		[stop_name] VARCHAR(50),
		[shape] geometry,
		CONSTRAINT pk_transitstop PRIMARY KEY ([scenario_id],[stop_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TRANSIT_TAP
IF OBJECT_ID('abm.transit_tap','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_tap] (
		[scenario_id] smallint NOT NULL,
		[tap] smallint NOT NULL,
		[shape] geometry,
		CONSTRAINT pk_transittap PRIMARY KEY ([scenario_id],[tap])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- TRANSIT_TAP_SKIMS
IF OBJECT_ID('abm.transit_tap_skims','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_tap_skims] (
		[scenario_id] smallint NOT NULL,
		[orig_tap] smallint NOT NULL,
		[dest_tap] smallint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[init_wait_premium] decimal(6,3),
		[ivt_premium] decimal(6,3),
		[walk_time_premium] decimal(6,3),
		[transfer_time_premium] decimal(6,3),
		[fare_premium] decimal(4,2),
		CONSTRAINT pk_transittapskims PRIMARY KEY ([scenario_id],[orig_tap],[dest_tap],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_transittapskims_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_transittapskims_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRANSIT_TAP_WALK
IF OBJECT_ID('abm.transit_tap_walk','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[transit_tap_walk] (
		[scenario_id] smallint NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[zone] smallint NOT NULL,
		[tap] smallint NOT NULL,
		[time_boarding_perceived] decimal(6,3),
		[time_boarding_actual] decimal(6,3),
		[time_alighting_perceived] decimal(6,3),
		[time_alighting_actual] decimal(6,3),
		[gain_boarding] decimal(8,4),
		[gain_alighting] decimal(8,4),
		CONSTRAINT pk_transittapwalk PRIMARY KEY ([scenario_id],[geography_type_id],[zone],[tap]),
		CONSTRAINT fk_transittapwalk_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_transittapwalk_zone FOREIGN KEY ([geography_type_id],[zone]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TRIP_AP
IF OBJECT_ID('abm.trip_ap','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_ap] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[trip_id] int NOT NULL,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[time_period_id] tinyint,
		[mode_id] tinyint,
		[purpose_id] tinyint,
		[inbound] bit,
		[party_size] tinyint,
		[trip_board_tap] smallint,
		[trip_alight_tap] smallint,
		[trip_time] decimal(11,6),
		[out_vehicle_time] decimal(11,6),
		[trip_distance] decimal(14,10),
		[trip_cost] decimal(4,2),
		[ap_income_cat_id] tinyint,
		[nights] tinyint,
		[ap_arrival_mode_id] tinyint,
		CONSTRAINT pk_tripap PRIMARY KEY ([scenario_id],[model_type_id],[trip_id]),
		CONSTRAINT fk_tripap_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripap_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripap_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripap_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripap_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripap_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripap_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripap_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id]),
		CONSTRAINT fk_tripap_income FOREIGN KEY ([ap_income_cat_id]) REFERENCES [ref].[ap_income_cat]([ap_income_cat_id]),
		CONSTRAINT fk_tripap_arrivalmode FOREIGN KEY ([ap_arrival_mode_id]) REFERENCES [ref].[ap_arrival_mode]([ap_arrival_mode_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);
	
END


-- TRIP_AGG
IF OBJECT_ID('abm.trip_agg','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_agg] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[orig] smallint NOT NULL,
		[dest] smallint NOT NULL,
		[purpose_id] tinyint NOT NULL,
		[mode_id] tinyint NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_id] tinyint NOT NULL,
		[trips] decimal(20,16),
		CONSTRAINT pk_tripagg PRIMARY KEY ([scenario_id],[model_type_id],[geography_type_id],[orig],[dest],[purpose_id],[mode_id],[time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripagg_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tripagg_purpose FOREIGN KEY ([model_type_id],[PURPOSE_ID]) REFERENCES [ref].[PURPOSE]([model_type_id],[PURPOSE_ID]),
		CONSTRAINT fk_tripagg_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripagg_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripagg_mode FOREIGN KEY ([MODE_ID]) REFERENCES [ref].[MODE]([MODE_ID]),
		CONSTRAINT fk_tripagg_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripagg_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripagg_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRIP_CB
IF OBJECT_ID('abm.trip_cb','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_cb] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[time_period_id] tinyint,
		[mode_id] tinyint,
		[purpose_id] tinyint,
		[inbound] bit,
		[party_size] tinyint,
		[trip_board_tap] smallint,
		[trip_alight_tap] smallint,
		[trip_time] decimal(11,6),
		[out_vehicle_time] decimal(11,6),
		[trip_distance] decimal(14,10),
		[trip_cost] decimal(4,2),
		CONSTRAINT pk_tripcb PRIMARY KEY ([scenario_id],[model_type_id],[tour_id],[trip_id]),
		CONSTRAINT fk_tripcb_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripcb_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripcb_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripcb_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripcb_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripcb_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripcb_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripcb_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	


-- TRIP_IE
IF OBJECT_ID('abm.trip_ie','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_ie] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[trip_id] int NOT NULL,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[time_period_id] tinyint,
		[mode_id] tinyint,
		[purpose_id] tinyint,
		[inbound] bit,
		[party_size] tinyint,
		[trip_board_tap] smallint,
		[trip_alight_tap] smallint,
		[trip_time] decimal(11,6),
		[out_vehicle_time] decimal(11,6),
		[trip_distance] decimal(14,10),
		[trip_cost] decimal(4,2),
		CONSTRAINT pk_tripie PRIMARY KEY ([scenario_id],[model_type_id],[trip_id]),
		CONSTRAINT fk_tripie_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripie_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripie_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripie_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripie_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripie_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripie_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripie_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- TRIP_IJ
IF OBJECT_ID('abm.trip_ij','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_ij] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[time_period_id] tinyint,
		[mode_id] tinyint,
		[purpose_id] tinyint,
		[inbound] bit,
		[party_size] tinyint,
		[trip_board_tap] smallint,
		[trip_alight_tap] smallint,
		[trip_time] decimal(11,6),
		[out_vehicle_time] decimal(11,6),
		[trip_distance] decimal(14,10),
		[trip_cost] decimal(4,2),
		[parking_zone] smallint,
		CONSTRAINT pk_tripij PRIMARY KEY ([scenario_id],[model_type_id],[tour_id],[trip_id]),
		CONSTRAINT fk_tripij_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripij_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripij_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripij_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripij_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripij_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripij_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripij_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id]),
		CONSTRAINT fk_tripij_parking_zone FOREIGN KEY ([geography_type_id],[parking_zone]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- TRIP_VIS
IF OBJECT_ID('abm.trip_vis','U') IS NULL
BEGIN

CREATE TABLE 
	[abm].[trip_vis] (
		[scenario_id] smallint NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[geography_type_id] tinyint,
		[orig] smallint,
		[dest] smallint,
		[time_resolution_id] tinyint,
		[time_period_id] tinyint,
		[mode_id] tinyint,
		[purpose_id] tinyint,
		[inbound] bit,
		[party_size] tinyint,
		[trip_board_tap] smallint,
		[trip_alight_tap] smallint,
		[trip_time] decimal(11,6),
		[out_vehicle_time] decimal(11,6),
		[trip_distance] decimal(14,10),
		[trip_cost] decimal(4,2),
		CONSTRAINT pk_tripvis PRIMARY KEY ([scenario_id],[model_type_id],[tour_id],[trip_id]),
		CONSTRAINT fk_tripvis_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripvis_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id]),
		CONSTRAINT fk_tripvis_orig FOREIGN KEY ([geography_type_id],[orig]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripvis_dest FOREIGN KEY ([geography_type_id],[dest]) REFERENCES [ref].[geography_zone] ([geography_type_id],[zone]),
		CONSTRAINT fk_tripvis_timeres FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution]([time_resolution_id]),
		CONSTRAINT fk_tripvis_period FOREIGN KEY ([time_resolution_id],[time_period_id]) REFERENCES [ref].[time_period]([time_resolution_id],[time_period_id]),
		CONSTRAINT fk_tripvis_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripvis_purpose FOREIGN KEY ([model_type_id],[purpose_id]) REFERENCES [ref].[purpose]([model_type_id],[purpose_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	