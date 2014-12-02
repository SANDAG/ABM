-- scenario_scheme IS SET AS THE ON FOR EVERY TABLE


-- AT_SKIMS
IF OBJECT_ID('abm.at_skims','U') IS NULL
BEGIN
	
CREATE TABLE 
	[abm].[at_skims] (
		[scenario_id] smallint NOT NULL,
		[at_skims_id] int IDENTITY(1,1) NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
    	[dest_geography_zone_id] int NOT NULL,
    	[mode_id] tinyint NOT NULL,
		[time_period_id] int NOT NULL,
    	[skim_id] tinyint NOT NULL,
    	[value] decimal(8,4) NOT NULL,
		CONSTRAINT pk_atskims PRIMARY KEY([scenario_id],[at_skims_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_atskims UNIQUE ([scenario_id],[orig_geography_zone_id],[dest_geography_zone_id],[mode_id],[time_period_id],[skim_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_atskims_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_atskims_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_atskims_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_atskims_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_atskims_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
		CONSTRAINT fk_atskims_skim FOREIGN KEY ([skim_id]) REFERENCES [ref].[skim] ([skim_id])
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
		[bike_flow_id] int IDENTITY(1,1) NOT NULL,
		[bike_link_ab_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[flow] decimal(8,4) NOT NULL,
		CONSTRAINT pk_bikeflow PRIMARY KEY ([scenario_id],[bike_flow_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_bikeflow UNIQUE ([scenario_id],[bike_link_ab_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_bikeflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_bikeflow_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id])
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
		[bike_link_id] int IDENTITY(1,1) NOT NULL,
		[roadsegid] int NOT NULL,
		[nm] varchar(50) NOT NULL,
		[at_func_class_id] smallint NOT NULL,
		[bike2sep] tinyint NOT NULL,
		[bike3blvd] tinyint NOT NULL,
		[speed] smallint NOT NULL,
		[scenicldx] decimal(11,9) NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_bikelink PRIMARY KEY ([scenario_id],[bike_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_bikelink UNIQUE ([scenario_id],[roadsegid]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_bikelink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
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
		[bike_link_ab_id] int IDENTITY(1,1) NOT NULL,
		[bike_link_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[from_node] int NOT NULL,
		[to_node] int NOT NULL,
		[gain] smallint NOT NULL,
		[bike_class_id] tinyint NOT NULL,
		[lanes] tinyint NOT NULL,
		[signal] tinyint NOT NULL,
		CONSTRAINT pk_bikelinkab PRIMARY KEY ([scenario_id],[bike_link_ab_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_bikelinkab UNIQUE ([scenario_id],[bike_link_id],[ab]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_bikelinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_bikelinkab_bikeclass FOREIGN KEY ([bike_class_id]) REFERENCES [ref].[bike_class]([bike_class_id])
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
		[cbd_vehicles_id] int IDENTITY(1,1) NOT NULL,
		[geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[vehicles] decimal(10,6) NOT NULL,
		CONSTRAINT pk_cbdvehicles PRIMARY KEY ([scenario_id],[cbd_vehicles_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_cbdvehicles UNIQUE ([scenario_id],[geography_zone_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_cbdvehicles_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_cbdvehicles_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_cbdvehicles_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone]([geography_zone_id])
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
		[hwy_flow_id] int IDENTITY(1,1) NOT NULL,
		[hwy_link_ab_tod_id] int NOT NULL,
		[flow_pce] decimal(12,6) NOT NULL,
		[time] decimal(10,6) NOT NULL,
		[voc] decimal(8,6) NOT NULL,
		[v_dist_t] decimal(12,6) NOT NULL,
		[vht] decimal(11,6) NOT NULL,
		[speed] decimal(9,6) NOT NULL,
		[vdf] decimal(10,6) NOT NULL,
		[msa_flow] decimal(12,6) NOT NULL,
		[msa_time] decimal(10,6) NOT NULL,
		[flow] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwyflow PRIMARY KEY ([scenario_id],[hwy_flow_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwyflow UNIQUE ([scenario_id],[hwy_link_ab_tod_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwyflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[hwy_flow_mode_id] int IDENTITY(1,1) NOT NULL,
		[hwy_flow_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[flow] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwyflowmode PRIMARY KEY ([scenario_id],[hwy_flow_mode_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwyflowmode UNIQUE ([scenario_id],[hwy_flow_id],[mode_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwyflowmode_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
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
		[hwy_link_id] int IDENTITY(1,1) NOT NULL,
		[hwycov_id] int NOT NULL,
		[length_mile] decimal(20,16) NOT NULL,
		[sphere] smallint NOT NULL,
		[nm] varchar(50) NOT NULL,
		[cojur] tinyint NOT NULL,
		[costat] smallint NOT NULL,
		[coloc] tinyint NOT NULL,
		[rloop] smallint NOT NULL,
		[adtlk] smallint NOT NULL,
		[adtvl] smallint NOT NULL,
		[aspd] tinyint NOT NULL,
		[iyr] smallint NOT NULL,
		[iproj] smallint NOT NULL,
		[ijur] tinyint NOT NULL,
		[ifc] tinyint NOT NULL,
		[ihov] tinyint NOT NULL,
		[itruck] tinyint NOT NULL,
		[ispd] tinyint NOT NULL,
		[iway] tinyint NOT NULL,
		[imed] tinyint NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_hwylink PRIMARY KEY ([scenario_id],[hwy_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwylink UNIQUE ([scenario_id],[hwycov_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwylink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[hwy_link_ab_id] int IDENTITY(1,1) NOT NULL,
		[hwy_link_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[from_node] smallint NOT NULL,
		[to_node] smallint NOT NULL,
		[from_nm] varchar(50) NOT NULL,
		[to_nm] varchar(50) NOT NULL,
		[au] tinyint NOT NULL,
		[pct] tinyint NOT NULL,
		[cnt] tinyint NOT NULL,
		CONSTRAINT pk_hwylinkab PRIMARY KEY ([scenario_id],[hwy_link_ab_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwylinkab UNIQUE ([scenario_id],[hwy_link_id],[ab]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwylinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[hwy_link_ab_tod_id] int IDENTITY(1,1) NOT NULL,
		[hwy_link_ab_id] int NOT NULL,
		[hwy_link_tod_id] int NOT NULL,
		[cp] decimal(12,6) NOT NULL,
		[cx] decimal(12,6) NOT NULL,
		[tm] decimal(12,6) NOT NULL,
		[tx] decimal(12,6) NOT NULL,
		[ln] decimal(12,6) NOT NULL,
		[stm] decimal(12,6) NOT NULL,
		[htm] decimal(12,6) NOT NULL,
		[preload] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwylinkabtod PRIMARY KEY ([scenario_id],[hwy_link_ab_tod_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwylinkabtod UNIQUE ([scenario_id],[hwy_link_ab_id],[hwy_link_tod_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwylinkabtod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[hwy_link_tod_id] int IDENTITY(1,1) NOT NULL,
		[hwy_link_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[itoll2] decimal(12,6) NOT NULL,
		[itoll3] decimal(12,6) NOT NULL,
		[itoll4] decimal(12,6) NOT NULL,
		[itoll5] decimal(12,6) NOT NULL,
		[itoll] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwylinktod PRIMARY KEY ([scenario_id],[hwy_link_tod_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwylinktod UNIQUE ([scenario_id],[hwy_link_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwylinktod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_hwylinktod_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]) 
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
		[hwy_skims_id] int IDENTITY(1,1) NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
    	[dist_drive_alone_toll] decimal(12,6) NOT NULL,
    	[time_drive_alone_toll] decimal(12,6) NOT NULL,
    	[cost_drive_alone_toll] decimal(4,2) NOT NULL,
    	[dist_drive_alone_free] decimal(12,6) NOT NULL,
    	[time_drive_alone_free] decimal(12,6) NOT NULL,
   		[dist_hov2_toll] decimal(12,6) NOT NULL,
    	[time_hov2_toll] decimal(12,6) NOT NULL,
   		[cost_hov2_toll] decimal(4,2) NOT NULL,
    	[dist_hov2_free] decimal(12,6) NOT NULL,
    	[time_hov2_free] decimal(12,6) NOT NULL,
    	[dist_hov3_toll] decimal(12,6) NOT NULL,
    	[time_hov3_toll] decimal(12,6) NOT NULL,
    	[cost_hov3_toll] decimal(4,2) NOT NULL,
    	[dist_hov3_free] decimal(12,6) NOT NULL,
    	[time_hov3_free] decimal(12,6) NOT NULL,
    	[dist_truck_toll] decimal(12,6) NOT NULL,
    	[time_truck_toll] decimal(12,6) NOT NULL,
   		[cost_truck_toll] decimal(4,2) NOT NULL,
    	[dist_truck_free] decimal(12,6) NOT NULL,
    	[time_truck_free] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwyskims PRIMARY KEY ([scenario_id],[hwy_skims_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_hwyskims UNIQUE ([scenario_id],[orig_geography_zone_id],[dest_geography_zone_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_hwyskims_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_hwyskims_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_hwyskims_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_hwyskims_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[lu_hh_id] int IDENTITY(1,1) NOT NULL,
		[hh_id] int NOT NULL,
		[hh_serial_no] bigint NOT NULL,
		[geography_zone_id] int NOT NULL,
		[hh_income_cat_id] tinyint NOT NULL,
		[hh_income] int NOT NULL,
		[workers] tinyint NOT NULL,
		[persons] tinyint NOT NULL,
		[unit_type_id] tinyint NOT NULL,
		[autos] tinyint NOT NULL,
		[transponder] tinyint NULL,
		[poverty] decimal(6,3) NULL,
		[version] smallint NOT NULL,
		CONSTRAINT pk_luhh PRIMARY KEY ([scenario_id],[lu_hh_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_luhh UNIQUE ([scenario_id],[hh_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_luhh_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_luhh_hhincomecat FOREIGN KEY ([hh_income_cat_id]) REFERENCES [ref].[hh_income_cat]([hh_income_cat_id]),
		CONSTRAINT fk_luhh_unittype FOREIGN KEY ([unit_type_id]) REFERENCES [ref].[unit_type]([unit_type_id]),
		CONSTRAINT fk_luhh_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[scenario_id] smallint NOT NULL,
		[lu_mgra_input_id] int IDENTITY(1,1)  NOT NULL,
		[geography_zone_id] int NOT NULL,
		[hs] smallint NOT NULL,
		[hs_sf] smallint NOT NULL,
		[hs_mf] smallint NOT NULL,
		[hs_mh] smallint NOT NULL,
		[hh] smallint NOT NULL,
		[hh_sf] smallint NOT NULL,
		[hh_mf] smallint NOT NULL,
		[hh_mh] smallint NOT NULL,
		[gq_civ] smallint NOT NULL,
		[gq_mil] smallint NOT NULL,
		[i1] smallint NOT NULL,
		[i2] smallint NOT NULL,
		[i3] smallint NOT NULL,
		[i4] smallint NOT NULL,
		[i5] smallint NOT NULL,
		[i6] smallint NOT NULL,
		[i7] smallint NOT NULL,
		[i8] smallint NOT NULL,
		[i9] smallint NOT NULL,
		[i10] smallint NOT NULL,
		[hhs] decimal(9,7) NOT NULL,
		[pop] smallint NOT NULL,
		[hhp] smallint NOT NULL,
		[emp_ag] decimal(12,7) NOT NULL,
		[emp_const_non_bldg_prod] decimal(12,7) NOT NULL,
		[emp_const_non_bldg_office] decimal(12,7) NOT NULL,
		[emp_utilities_prod] decimal(12,7) NOT NULL,
		[emp_utilities_office] decimal(12,7) NOT NULL,
		[emp_const_bldg_prod] decimal(12,7) NOT NULL,
		[emp_const_bldg_office] decimal(12,7) NOT NULL,
		[emp_mfg_prod] decimal(12,7) NOT NULL,
		[emp_mfg_office] decimal(12,7) NOT NULL,
		[emp_whsle_whs] decimal(12,7) NOT NULL,
		[emp_trans] decimal(12,7) NOT NULL,
		[emp_retail] decimal(12,7) NOT NULL,
		[emp_prof_bus_svcs] decimal(12,7) NOT NULL,
		[emp_prof_bus_svcs_bldg_maint] decimal(12,7) NOT NULL,
		[emp_pvt_ed_k12] decimal(12,7) NOT NULL,
		[emp_pvt_ed_post_k12_oth] decimal(12,7) NOT NULL,
		[emp_health] decimal(12,7) NOT NULL,
		[emp_personal_svcs_office] decimal(12,7) NOT NULL,
		[emp_amusement] decimal(12,7) NOT NULL,
		[emp_hotel] decimal(12,7) NOT NULL,
		[emp_restaurant_bar] decimal(12,7) NOT NULL,
		[emp_personal_svcs_retail] decimal(12,7) NOT NULL,
		[emp_religious] decimal(12,7) NOT NULL,
		[emp_pvt_hh] decimal(12,7) NOT NULL,
		[emp_state_local_gov_ent] decimal(12,7) NOT NULL,
		[emp_fed_non_mil] decimal(12,7) NOT NULL,
		[emp_fed_mil] decimal(12,7) NOT NULL,
		[emp_state_local_gov_blue] decimal(12,7) NOT NULL,
		[emp_state_local_gov_white] decimal(12,7) NOT NULL,
		[emp_public_ed] decimal(12,7) NOT NULL,
		[emp_own_occ_dwell_mgmt] decimal(12,7) NOT NULL,
		[emp_fed_gov_accts] decimal(12,7) NOT NULL,
		[emp_st_lcl_gov_accts] decimal(12,7) NOT NULL,
		[emp_cap_accts] decimal(12,7) NOT NULL,
		[emp_total] decimal(12,7) NOT NULL,
		[enrollgradekto8] decimal(12,7) NOT NULL,
		[enrollgrade9to12] decimal(12,7) NOT NULL,
		[collegeenroll] decimal(12,7) NOT NULL,
		[othercollegeenroll] decimal(12,7) NOT NULL,
		[adultschenrl] decimal(12,7) NOT NULL,
		[ech_dist] int NOT NULL,
		[hch_dist] int NOT NULL,
		[pseudomsa] tinyint NOT NULL,
		[parkarea] tinyint NOT NULL,
		[hstallsoth] smallint NOT NULL,
		[hstallssam] smallint NOT NULL,
		[hparkcost] decimal(10,8) NOT NULL,
		[numfreehrs] tinyint NOT NULL,
		[dstallsoth] smallint NOT NULL,
		[dstallssam] smallint NOT NULL,
		[dparkcost] decimal(10,8) NOT NULL,
		[mstallsoth] smallint NOT NULL,
		[mstallssam] smallint NOT NULL,
		[mparkcost] decimal(10,8) NOT NULL,
		[totint] smallint NOT NULL,
		[duden] decimal(11,8) NOT NULL,
		[empden] decimal(11,8) NOT NULL,
		[popden] decimal(11,8) NOT NULL,
		[retempden] decimal(11,8) NOT NULL,
		[totintbin] tinyint NOT NULL,
		[empdenbin] tinyint NOT NULL,
		[dudenbin] tinyint NOT NULL,
		[zip09] int NOT NULL,
		[parkactive] decimal(15,10) NOT NULL,
		[openspaceparkpreserve] decimal(15,10) NOT NULL,
		[beachactive] decimal(15,10) NOT NULL,
		[budgetroom] decimal(9,5) NOT NULL,
		[economyroom] decimal(9,5) NOT NULL,
		[luxuryroom] decimal(9,5) NOT NULL,
		[midpriceroom] decimal(9,5) NOT NULL,
		[upscaleroom] decimal(9,5) NOT NULL,
		[hotelroomtotal] decimal(9,5) NOT NULL,
		[luz_id] smallint NOT NULL,
		[truckregiontype] tinyint NOT NULL,
		[district27] tinyint NOT NULL,
		[milestocoast] decimal(12,9) NOT NULL,
		CONSTRAINT pk_lumgrainput PRIMARY KEY ([scenario_id],[lu_mgra_input_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_lumgrainput UNIQUE ([scenario_id],[geography_zone_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_lumgrainput_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_lumgrainput_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[lu_person_id] int IDENTITY(1,1) NOT NULL,
		[lu_hh_id] int NOT NULL,
		[pnum] tinyint NOT NULL,
		[age] tinyint NOT NULL,
		[sex_id] tinyint NOT NULL,
		[military_id] tinyint NOT NULL,
		[pemploy_id] tinyint NOT NULL,
		[pstudent_id] tinyint NOT NULL,
		[ptype_id] tinyint NOT NULL,
		[educ_id] tinyint NOT NULL,
		[grade_id] tinyint NOT NULL,
		[occen5] smallint NOT NULL,
		[occsoc5] varchar(15) NOT NULL,
		[indcen] smallint NOT NULL,
		[weeks_worked_id] tinyint NOT NULL,
		[hours_worked] tinyint NOT NULL,
		[race_id] tinyint NOT NULL,
		[hisp_id] tinyint NOT NULL,
		[activity_pattern_id] tinyint NOT NULL,
		[ie_choice] tinyint NOT NULL,
		[version] smallint NOT NULL,
		CONSTRAINT pk_luperson PRIMARY KEY ([scenario_id],[lu_person_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_luperson UNIQUE ([scenario_id],[lu_hh_id],[pnum]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_luperson_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
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
		[lu_person_fp_id] int IDENTITY(1,1) NOT NULL,
		[lu_person_id] int NOT NULL,
		[fp_choice_id] tinyint NOT NULL,
		[reimb_pct] decimal(8,6) NULL,
		CONSTRAINT pk_lupersonfp PRIMARY KEY ([scenario_id],[lu_person_fp_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_lupersonfp UNIQUE ([scenario_id],[lu_person_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_lupersonfp_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
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
		[lu_person_lc_id] int IDENTITY(1,1) NOT NULL,
		[lu_person_id] int NOT NULL,
		[loc_choice_segment_id] int NOT NULL,
		[geography_zone_id] int NOT NULL,
		CONSTRAINT pk_lupersonlc PRIMARY KEY ([scenario_id],[lu_person_lc_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_lupersonlc UNIQUE ([scenario_id],[lu_person_id],[loc_choice_segment_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_lupersonlc_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_lupersonlc_segment FOREIGN KEY ([loc_choice_segment_id]) REFERENCES [ref].[loc_choice_segment] ([loc_choice_segment_id]),
		CONSTRAINT fk_lupersonlc_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[tour_cb_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[purpose_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[start_time_period_id] int NOT NULL,
		[end_time_period_id] int NOT NULL,
		[crossing_mode_id] tinyint NOT NULL,
		[sentri] tinyint NOT NULL,
		[poe_id] tinyint NOT NULL,
		CONSTRAINT pk_tourcb PRIMARY KEY ([scenario_id],[tour_cb_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tourcb UNIQUE ([scenario_id],[model_type_id],[tour_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tourcb_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tourcb_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tourcb_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tourcb_startperiod FOREIGN KEY ([start_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourcb_endperiod FOREIGN KEY ([end_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourcb_mode FOREIGN KEY ([crossing_mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourcb_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tourcb_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tourcb_poe FOREIGN KEY ([poe_id]) REFERENCES [ref].[poe]([poe_id])
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
		[tour_ij_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[tour_cat_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[start_time_period_id] int NOT NULL,
		[end_time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		CONSTRAINT pk_tourij PRIMARY KEY ([scenario_id],[tour_ij_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tourij UNIQUE ([scenario_id],[model_type_id],[tour_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tourij_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tourij_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tourij_tourcat FOREIGN KEY ([tour_cat_id]) REFERENCES [ref].[tour_cat]([tour_cat_id]),
		CONSTRAINT fk_tourij_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tourij_startperiod FOREIGN KEY ([start_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourij_endperiod FOREIGN KEY ([end_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourij_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourij_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tourij_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[tour_ij_person_id] int IDENTITY(1,1) NOT NULL,
		[tour_ij_id] int NOT NULL,
		[lu_person_id] int NOT NULL,
		CONSTRAINT pk_tourijperson PRIMARY KEY ([scenario_id],[tour_ij_person_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tourijperson UNIQUE ([scenario_id], [tour_ij_id], [lu_person_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tourijperson_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[tour_vis_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[tour_id] int NOT NULL,
		[tour_cat_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[start_time_period_id] int NOT NULL,
		[end_time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[auto_available] tinyint NOT NULL,
		[hh_income_cat_id] tinyint NOT NULL,
		CONSTRAINT pk_tour_vis PRIMARY KEY ([scenario_id],[tour_vis_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tourvis UNIQUE ([scenario_id],[model_type_id],[tour_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tourvis_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tourvis_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tourvis_tourcat FOREIGN KEY ([tour_cat_id]) REFERENCES [ref].[tour_cat]([tour_cat_id]),
		CONSTRAINT fk_tourvis_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tourvis_startperiod FOREIGN KEY ([start_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourvis_endperiod FOREIGN KEY ([end_time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tourvis_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tourvis_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tourvis_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
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
		[transit_aggflow_id] int IDENTITY(1,1) NOT NULL,
		[transit_link_id] int NOT NULL,
		[ab] tinyint NOT NULL,
		[time_period_id] int NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[transit_flow] smallint NOT NULL,		
		[non_transit_flow] smallint NOT NULL,	
		[total_flow] smallint NOT NULL,	
		[access_walk_flow] smallint NOT NULL,	
		[xfer_walk_flow] smallint NOT NULL,	
		[egress_walk_flow] smallint NOT NULL,
		CONSTRAINT pk_transitaggflow PRIMARY KEY ([scenario_id],[transit_aggflow_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitaggflow UNIQUE ([scenario_id],[transit_link_id],[ab],[time_period_id],[transit_mode_id],[transit_access_mode_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitaggflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitaggflow_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
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
		[transit_flow_id] int IDENTITY(1,1) NOT NULL,
		[transit_route_id] int NOT NULL,
		[from_transit_stop_id] int NOT NULL,
		[to_transit_stop_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[from_mp] decimal(9,6) NOT NULL,
		[to_mp] decimal(9,6) NOT NULL,
		[baseivtt] decimal(9,6) NOT NULL,
		[cost] decimal(9,6) NOT NULL,
		[transit_flow] smallint NOT NULL,
		CONSTRAINT pk_transitflow PRIMARY KEY ([scenario_id],[transit_flow_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitflow UNIQUE ([scenario_id],[transit_route_id],[from_transit_stop_id],[to_transit_stop_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitflow_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
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
		[transit_link_id] int IDENTITY(1,1) NOT NULL,
		[trcov_id] int NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_transitlink PRIMARY KEY ([scenario_id],[transit_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitlink UNIQUE ([scenario_id],[trcov_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitlink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[transit_onoff_id] int IDENTITY(1,1) NOT NULL,
		[transit_route_id] int NOT NULL,
		[transit_stop_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[transit_access_mode_id] tinyint NOT NULL,
		[boardings] decimal(11,6) NOT NULL,
		[alightings] decimal(11,6) NOT NULL,
		[walk_access_on] decimal(11,6) NOT NULL,
		[direct_transfer_on] decimal(11,6) NOT NULL,
		[walk_transfer_on] decimal(11,6) NOT NULL,
		[direct_transfer_off] decimal(11,6) NOT NULL,
		[walk_transfer_off] decimal(11,6) NOT NULL,
		[egress_off] decimal(11,6) NOT NULL,
		CONSTRAINT pk_transitonoff PRIMARY KEY ([scenario_id],[transit_onoff_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitonoff UNIQUE ([scenario_id],[transit_route_id],[transit_stop_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitonoff_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitonoff_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
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
		[transit_pnr_id] int IDENTITY(1,1) NOT NULL,
		[transit_tap_id] int NOT NULL,
		[lot_id] smallint NOT NULL,
		[time_period_id] int NOT NULL,
		[parking_type_id] tinyint NOT NULL,
		[capacity] smallint NOT NULL,
		[distance] smallint NOT NULL,
		[vehicles] smallint NOT NULL,
		CONSTRAINT pk_transitpnr PRIMARY KEY ([scenario_id],[transit_pnr_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitpnr UNIQUE ([scenario_id],[transit_tap_id],[lot_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitpnr_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitpnr_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id])
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
		[transit_route_id] int IDENTITY(1,1) NOT NULL,
		[route_id] smallint NOT NULL,
		[transit_mode_id] tinyint NOT NULL,
		[am_headway] decimal(5,2) NOT NULL,
		[pm_headway] decimal(5,2) NOT NULL,
		[op_headway] decimal(5,2) NOT NULL,
		[nt_headway] decimal(5,2) NOT NULL,
		[nt_hour] tinyint NOT NULL,
		[config] int NOT NULL,
		[fare] decimal(4,2) NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_transitroute PRIMARY KEY ([scenario_id],[transit_route_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitroute UNIQUE ([scenario_id],[route_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitroute_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
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
		[transit_stop_id] int IDENTITY(1,1) NOT NULL,
		[transit_route_id] int NOT NULL,
		[transit_link_id] int NOT NULL,
		[stop_id] smallint NOT NULL,
		[mp] decimal(9,6) NOT NULL,
		[near_node] int NOT NULL,
		[fare_zone] smallint NOT NULL,
		[stop_name] VARCHAR(50) NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_transitstop PRIMARY KEY ([scenario_id],[transit_stop_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transitstop UNIQUE ([scenario_id],[stop_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transitstop_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[transit_tap_id] int IDENTITY(1,1) NOT NULL,
		[tap] smallint NOT NULL,
		[shape] geometry NULL,
		CONSTRAINT pk_transittap PRIMARY KEY ([scenario_id],[transit_tap_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transittap UNIQUE ([scenario_id],[tap]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transittap_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
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
		[transit_tap_skims_id] int IDENTITY(1,1),
		[orig_transit_tap_id] int NOT NULL,
		[dest_transit_tap_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[init_wait_premium] decimal(6,3) NOT NULL,
		[ivt_premium] decimal(6,3) NOT NULL,
		[walk_time_premium] decimal(6,3) NOT NULL,
		[transfer_time_premium] decimal(6,3) NOT NULL,
		[fare_premium] decimal(4,2) NOT NULL,
		CONSTRAINT pk_transittapskims PRIMARY KEY ([scenario_id],[transit_tap_skims_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transittapskims UNIQUE ([scenario_id],[orig_transit_tap_id],[dest_transit_tap_id],[time_period_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transittapskims_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transittapskims_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id])
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
		[transit_tap_walk_id] int IDENTITY(1,1) NOT NULL,
		[geography_zone_id] int NOT NULL,
		[transit_tap_id] int NOT NULL,
		[time_boarding_perceived] decimal(6,3) NOT NULL,
		[time_boarding_actual] decimal(6,3) NOT NULL,
		[time_alighting_perceived] decimal(6,3) NOT NULL,
		[time_alighting_actual] decimal(6,3) NOT NULL,
		[gain_boarding] decimal(8,4) NOT NULL,
		[gain_alighting] decimal(8,4) NOT NULL,
		CONSTRAINT pk_transittapwalk PRIMARY KEY ([scenario_id],[transit_tap_walk_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_transittapwalk UNIQUE ([scenario_id],[geography_zone_id],[transit_tap_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_transittapwalk_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transittapwalk_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[trip_ap_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[trip_id] int NOT NULL,
		[orig_geography_zone_id] int NULL,
		[dest_geography_zone_id] int NULL,
		[time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[inbound] bit NOT NULL,
		[party_size] tinyint NOT NULL,
		[board_transit_tap_id] int NULL,
		[alight_transit_tap_id] int NULL,
		[trip_time] decimal(11,6) NULL,
		[out_vehicle_time] decimal(11,6) NULL,
		[trip_distance] decimal(14,10) NULL,
		[trip_cost] decimal(4,2) NULL,
		[ap_income_cat_id] tinyint NOT NULL,
		[nights] tinyint NOT NULL,
		[ap_arrival_mode_id] tinyint NOT NULL,
		CONSTRAINT pk_tripap PRIMARY KEY ([scenario_id],[trip_ap_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripap UNIQUE ([scenario_id],[model_type_id],[trip_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripap_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripap_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tripap_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripap_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripap_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripap_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripap_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
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
		[trip_agg_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[purpose_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[trips] decimal(20,16) NOT NULL,
		CONSTRAINT pk_tripagg PRIMARY KEY ([scenario_id],[trip_agg_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripagg UNIQUE ([scenario_id],[model_type_id],[orig_geography_zone_id],[dest_geography_zone_id],[time_period_id],[purpose_id],[mode_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripagg_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripagg_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tripagg_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripagg_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripagg_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[MODE]([mode_id]),
		CONSTRAINT fk_tripagg_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripagg_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[trip_cb_id] int IDENTITY(1,1) NOT NULL,
		[tour_cb_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[inbound] bit NOT NULL,
		[party_size] tinyint NOT NULL,
		[board_transit_tap_id] int NULL,
		[alight_transit_tap_id] int NULL,
		[trip_time] decimal(11,6) NOT NULL,
		[out_vehicle_time] decimal(11,6) NOT NULL,
		[trip_distance] decimal(14,10) NOT NULL,
		[trip_cost] decimal(4,2) NOT NULL,
		CONSTRAINT pk_tripcb PRIMARY KEY ([scenario_id],[trip_cb_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripcb UNIQUE ([scenario_id],[tour_cb_id],[trip_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripcb_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripcb_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripcb_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripcb_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripcb_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripcb_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id])
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
		[trip_ie_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[trip_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[inbound] bit NOT NULL,
		[party_size] tinyint NOT NULL,
		[board_transit_tap_id] int NULL,
		[alight_transit_tap_id] int NULL,
		[trip_time] decimal(11,6) NOT NULL,
		[out_vehicle_time] decimal(11,6) NOT NULL,
		[trip_distance] decimal(14,10) NOT NULL,
		[trip_cost] decimal(4,2) NOT NULL,
		CONSTRAINT pk_tripie PRIMARY KEY ([scenario_id],[trip_ie_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripie UNIQUE ([scenario_id],[model_type_id],[trip_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripie_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripie_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tripie_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripie_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripie_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripie_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripie_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id])
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
		[trip_ij_id] int IDENTITY(1,1) NOT NULL,
		[tour_ij_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[inbound] bit NOT NULL,
		[party_size] tinyint NOT NULL,
		[board_transit_tap_id] int NULL,
		[alight_transit_tap_id] int NULL,
		[trip_time] decimal(11,6) NOT NULL,
		[out_vehicle_time] decimal(11,6) NOT NULL,
		[trip_distance] decimal(14,10) NOT NULL,
		[trip_cost] decimal(4,2) NOT NULL,
		[parking_geography_zone_id] int NULL,
		CONSTRAINT pk_tripij PRIMARY KEY ([scenario_id],[trip_ij_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripij UNIQUE ([scenario_id],[tour_ij_id],[trip_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripij_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripij_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripij_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripij_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripij_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripij_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripij_parkingzone FOREIGN KEY ([parking_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
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
		[trip_vis_id] int IDENTITY(1,1) NOT NULL,
		[tour_vis_id] int NOT NULL,
		[trip_id] int NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[purpose_id] int NOT NULL,
		[inbound] bit NOT NULL,
		[party_size] tinyint NOT NULL,
		[board_transit_tap_id] int NULL,
		[alight_transit_tap_id] int NULL,
		[trip_time] decimal(11,6) NOT NULL,
		[out_vehicle_time] decimal(11,6) NOT NULL,
		[trip_distance] decimal(14,10) NOT NULL,
		[trip_cost] decimal(4,2) NOT NULL,
		CONSTRAINT pk_tripvis PRIMARY KEY ([scenario_id],[trip_vis_id]) WITH (STATISTICS_INCREMENTAL = ON),
		CONSTRAINT ixuq_tripvis UNIQUE ([scenario_id],[tour_vis_id],[trip_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_tripvis_scenario FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripvis_orig FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripvis_dest FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripvis_period FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripvis_mode FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripvis_purpose FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id])
	) 
ON 
	scenario_scheme ([scenario_id])
WITH 
	(DATA_COMPRESSION = PAGE);

END	