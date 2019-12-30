-- create stored procedure to remove scenario data
CREATE PROCEDURE [dbo].[sp_clear_scen]
	@scenario_id integer
WITH RECOMPILE AS
BEGIN
-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 04/16/2018
-- Description:	Clear out selected scenario data
-- =============================================

SET NOCOUNT ON;

-- Make sure the scenario's partition boundary hasn't been merged
-- If it has stop execution since other scenario data can potentially be deleted
IF NOT EXISTS (
	SELECT
		*
	FROM
		[sys].[partition_range_values]
	INNER JOIN
		[sys].[partition_functions]
	ON
		[partition_range_values].[function_id] = [partition_functions].[function_id]
	WHERE
		[name] = 'scenario_partition'
		AND [value] = @scenario_id)
BEGIN
	print 'WARNING: Partition function was merged. This could delete other scenarios data. Canceled execution.'
	RETURN
END


DECLARE @SQL nvarchar(max)
DECLARE @filegroupname nvarchar(20)
DECLARE @drop_partition smallint

-- This is dangerous if partition function was already merged
SET @drop_partition = (SELECT $PARTITION.[scenario_partition](@scenario_id)) -- Partition scenario belongs to
SET @filegroupname = 'scenario_fg_' + CAST(@scenario_id AS nvarchar)


-- Create staging tables to drop on the scenario filegroup for all fact tables

-- create bike_flow fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.bike_flow'',''U'') IS NOT NULL
DROP TABLE [staging].[bike_flow]

CREATE TABLE [staging].[bike_flow] (
	[scenario_id] int NOT NULL,
	[bike_flow_id] int IDENTITY(1,1) NOT NULL,
	[bike_link_id] int NOT NULL,
	[bike_link_ab_id] int NOT NULL,
	[time_id] int NOT NULL,
	[flow] decimal(8,4) NOT NULL,
	INDEX ccsi_bikeflow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create hwy_flow fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.hwy_flow'',''U'') IS NOT NULL
DROP TABLE [staging].[hwy_flow]

CREATE TABLE [staging].[hwy_flow] (
	[scenario_id] int NOT NULL,
	[hwy_flow_id] int IDENTITY(1,1) NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[time_id] int NOT NULL,
	[flow_pce] decimal(12,6) NOT NULL,
	[time] decimal(10,6) NOT NULL,
	[voc] decimal(8,6) NOT NULL,
	[v_dist_t] decimal(10,6) NOT NULL,
	[vht] decimal(11,6) NOT NULL,
	[speed] decimal(9,6) NOT NULL,
	[vdf] decimal(10,6) NOT NULL,
	[msa_flow] decimal(12,6) NOT NULL,
	[msa_time] decimal(10,6) NOT NULL,
	[flow] decimal(12,6) NOT NULL,
	INDEX ccsi_hwyflow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create hwy_flow_mode fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.hwy_flow_mode'',''U'') IS NOT NULL
DROP TABLE [staging].[hwy_flow_mode]

CREATE TABLE [staging].[hwy_flow_mode] (
	[scenario_id] int NOT NULL,
	[hwy_flow_mode_id] int IDENTITY(1,1) NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_id] tinyint NOT NULL,
	[flow] decimal(12,6) NOT NULL,
	INDEX ccsi_hwyflowmode CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create mgra_based_input fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.mgra_based_input'',''U'') IS NOT NULL
DROP TABLE [staging].[mgra_based_input]

CREATE TABLE [staging].[mgra_based_input] (
	[scenario_id] int NOT NULL,
	[mgra_based_input_id] int IDENTITY(1,1) NOT NULL,
	[geography_id] int NOT NULL,
	[hs] integer NOT NULL,
	[hs_sf] smallint NOT NULL,
	[hs_mf] integer NOT NULL,
	[hs_mh] smallint NOT NULL,
	[hh] integer NOT NULL,
	[hh_sf] smallint NOT NULL,
	[hh_mf] integer NOT NULL,
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
	[hhs] decimal(6,4) NOT NULL,
	[pop] integer NOT NULL,
	[hhp] integer NOT NULL,
	[emp_ag] int NOT NULL,
	[emp_const_non_bldg_prod] int NOT NULL,
	[emp_const_non_bldg_office] int NOT NULL,
	[emp_utilities_prod] int NOT NULL,
	[emp_utilities_office] int NOT NULL,
	[emp_const_bldg_prod] int NOT NULL,
	[emp_const_bldg_office] int NOT NULL,
	[emp_mfg_prod] int NOT NULL,
	[emp_mfg_office] int NOT NULL,
	[emp_whsle_whs] int NOT NULL,
	[emp_trans] int NOT NULL,
	[emp_retail] int NOT NULL,
	[emp_prof_bus_svcs] int NOT NULL,
	[emp_prof_bus_svcs_bldg_maint] int NOT NULL,
	[emp_pvt_ed_k12] int NOT NULL,
	[emp_pvt_ed_post_k12_oth] int NOT NULL,
	[emp_health] int NOT NULL,
	[emp_personal_svcs_office] int NOT NULL,
	[emp_amusement] int NOT NULL,
	[emp_hotel] int NOT NULL,
	[emp_restaurant_bar] int NOT NULL,
	[emp_personal_svcs_retail] int NOT NULL,
	[emp_religious] int NOT NULL,
	[emp_pvt_hh] int NOT NULL,
	[emp_state_local_gov_ent] int NOT NULL,
	[emp_fed_non_mil] int NOT NULL,
	[emp_fed_mil] int NOT NULL,
	[emp_state_local_gov_blue] int NOT NULL,
	[emp_state_local_gov_white] int NOT NULL,
	[emp_public_ed] int NOT NULL,
	[emp_own_occ_dwell_mgmt] int NOT NULL,
	[emp_fed_gov_accts] int NOT NULL,
	[emp_st_lcl_gov_accts] int NOT NULL,
	[emp_cap_accts] int NOT NULL,
	[emp_total] int NOT NULL,
	[enrollgradekto8] int NOT NULL,
	[enrollgrade9to12] int NOT NULL,
	[collegeenroll] int NOT NULL,
	[othercollegeenroll] int NOT NULL,
	[adultschenrl] decimal(12,7) NOT NULL,
    [ech_dist] int NOT NULL,
    [hch_dist] int NOT NULL,
	[pseudomsa] tinyint NOT NULL,
    [parkarea] tinyint NOT NULL,
	[hstallsoth] decimal(12,7) NOT NULL,
    [hstallssam] decimal(12,7) NOT NULL,
	[hparkcost] decimal(5,2) NOT NULL,
	[numfreehrs] tinyint NOT NULL,
	[dstallsoth] decimal(12,7) NOT NULL,
    [dstallssam] decimal(12,7) NOT NULL,
    [dparkcost] decimal(5,2) NOT NULL,
    [mstallsoth] decimal(12,7) NOT NULL,
    [mstallssam] decimal(12,7) NOT NULL,
    [mparkcost] decimal(6,2) NOT NULL,
	[totint] decimal(9,5) NOT NULL,
	[duden] decimal(8,4) NOT NULL,
    [empden] decimal(8,4) NOT NULL,
    [popden] decimal(8,4) NOT NULL,
    [retempden] decimal(8,4) NOT NULL,
	[totintbin] tinyint NOT NULL,
    [empdenbin] tinyint NOT NULL,
    [dudenbin] tinyint NOT NULL,
	[zip09] int NOT NULL,
	[parkactive] decimal(9,4) NOT NULL,
    [openspaceparkpreserve] decimal(9,4) NOT NULL,
    [beachactive] decimal(9,4) NOT NULL,
    [hotelroomtotal] decimal(12,7) NOT NULL,
	[truckregiontype] tinyint NOT NULL,
    [district27] tinyint NOT NULL,
    [milestocoast] decimal(7,4) NOT NULL,
    [acres] decimal(10,5) NOT NULL,
    [effective_acres] decimal(10,5) NOT NULL,
    [land_acres] decimal(10,5) NOT NULL,
	INDEX ccsi_mgrabasedinput CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_aggflow fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.transit_aggflow'',''U'') IS NOT NULL
DROP TABLE [staging].[transit_aggflow]

CREATE TABLE [staging].[transit_aggflow] (
	[scenario_id] int NOT NULL,
	[transit_aggflow_id] int IDENTITY(1,1) NOT NULL,
	[transit_link_id] int NOT NULL,
	[ab] bit NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[transit_flow] decimal(11,6) NOT NULL,
	[non_transit_flow] decimal(11,6) NOT NULL,
	[total_flow] decimal(11,6) NOT NULL,
	[access_walk_flow] decimal(11,6) NOT NULL,
	[xfer_walk_flow] decimal(11,6) NOT NULL,
	[egress_walk_flow] decimal(11,6) NOT NULL,
	INDEX ccsi_transitaggflow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_flow fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.transit_flow'',''U'') IS NOT NULL
DROP TABLE [staging].[transit_flow]

CREATE TABLE [staging].[transit_flow] (
	[scenario_id] int NOT NULL,
	[transit_flow_id] int IDENTITY(1,1) NOT NULL,
	[transit_route_id] int NOT NULL,
	[transit_stop_from_id] int NOT NULL,
	[transit_stop_to_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[from_mp] decimal(9,6) NOT NULL,
	[to_mp] decimal(9,6) NOT NULL,
	[baseivtt] decimal(9,6) NOT NULL,
	[cost] decimal(9,6) NOT NULL,
	[transit_flow] decimal(11,6) NOT NULL,
	INDEX ccsi_transitflow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_onoff fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.transit_onoff'',''U'') IS NOT NULL
DROP TABLE [staging].[transit_onoff]

CREATE TABLE [staging].[transit_onoff] (
	[scenario_id] int NOT NULL,
	[transit_onoff_id] int IDENTITY(1,1) NOT NULL,
	[transit_route_id] int NOT NULL,
	[transit_stop_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[boardings] decimal(11,6) NOT NULL,
	[alightings] decimal(11,6) NOT NULL,
	[walk_access_on] decimal(11,6) NOT NULL,
	[direct_transfer_on] decimal(11,6) NOT NULL,
	[direct_transfer_off] decimal(11,6) NOT NULL,
	[egress_off] decimal(11,6) NOT NULL,
	INDEX ccsi_transitonoff CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_pnr fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.transit_pnr'',''U'') IS NOT NULL
DROP TABLE [staging].[transit_pnr]

CREATE TABLE [staging].[transit_pnr] (
	[scenario_id] int NOT NULL,
	[transit_pnr_id] int IDENTITY(1,1) NOT NULL,
	[transit_tap_id] int NOT NULL,
	[lot_id] smallint NOT NULL,
	[geography_id] int NOT NULL,
	[time_id] int NOT NULL,
	[parking_type] nchar(60) NOT NULL,
	[capacity] smallint NOT NULL,
	[distance] smallint NOT NULL,
	[vehicles] smallint NOT NULL,
	INDEX ccsi_transitpnr CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create person trips fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.person_trip'',''U'') IS NOT NULL
DROP TABLE [staging].[person_trip]

CREATE TABLE [staging].[person_trip] (
	[scenario_id] int NOT NULL,
	[person_trip_id] int IDENTITY(1,1) NOT NULL,
	[person_id] int NOT NULL,
	[household_id] int NOT NULL,
	[tour_id] int NOT NULL,
	[model_trip_id] tinyint NOT NULL,
	[mode_trip_id] tinyint NOT NULL,
	[purpose_trip_origin_id] tinyint NOT NULL,
	[purpose_trip_destination_id] tinyint NOT NULL,
	[inbound_id] tinyint NOT NULL,
	[time_trip_start_id] int NOT NULL,
	[time_trip_end_id] int NOT NULL,
	[geography_trip_origin_id] int NOT NULL,
	[geography_trip_destination_id] int NOT NULL,
	[geography_parking_destination_id] int NOT NULL,
	[transit_tap_boarding_id] int NOT NULL,
	[transit_tap_alighting_id] int NOT NULL,
	[mode_airport_arrival_id] tinyint NOT NULL,
	[time_drive] decimal(10, 4) NOT NULL,
	[dist_drive] decimal(10, 4) NOT NULL,
	[toll_cost_drive] decimal(4,2) NOT NULL,
	[operating_cost_drive] decimal(4, 2) NOT NULL,
	[time_walk] decimal(10, 4) NOT NULL,
	[dist_walk] decimal(10, 4) NOT NULL,
	[time_bike] decimal(10, 4) NOT NULL,
	[dist_bike] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_local] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_express] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_rapid] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_light_rail] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_commuter_rail] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle] decimal(10, 4) NOT NULL,
	[dist_transit_in_vehicle] decimal(10, 4) NOT NULL,
	[cost_transit] decimal(4, 2) NOT NULL,
	[time_transit_wait] decimal(10, 4) NOT NULL,
	[transit_transfers] decimal(6,4) NOT NULL,
	[time_total] decimal(10, 4) NOT NULL,
	[dist_total] decimal(10, 4) NOT NULL,
	[cost_total] decimal(4, 2) NOT NULL,
	[value_of_time] decimal(8,2) NOT NULL,
	[weight_person_trip] decimal(8, 5) NOT NULL,
	[weight_trip] decimal(8, 5) NOT NULL,
	INDEX ccsi_persontrip CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- Switch and drop partitions for all fact tables
ALTER TABLE [fact].[bike_flow] SWITCH PARTITION @drop_partition TO [staging].[bike_flow]
DROP TABLE [staging].[bike_flow];

ALTER TABLE [fact].[hwy_flow] SWITCH PARTITION @drop_partition TO [staging].[hwy_flow]
DROP TABLE [staging].[hwy_flow];

ALTER TABLE [fact].[hwy_flow_mode] SWITCH PARTITION @drop_partition TO [staging].[hwy_flow_mode]
DROP TABLE [staging].[hwy_flow_mode];

ALTER TABLE [fact].[mgra_based_input] SWITCH PARTITION @drop_partition TO [staging].[mgra_based_input]
DROP TABLE [staging].[mgra_based_input];

ALTER TABLE [fact].[person_trip] SWITCH PARTITION @drop_partition TO [staging].[person_trip]
DROP TABLE [staging].[person_trip];

ALTER TABLE [fact].[transit_aggflow] SWITCH PARTITION @drop_partition TO [staging].[transit_aggflow]
DROP TABLE [staging].[transit_aggflow];

ALTER TABLE [fact].[transit_flow] SWITCH PARTITION @drop_partition TO [staging].[transit_flow]
DROP TABLE [staging].[transit_flow];

ALTER TABLE [fact].[transit_onoff] SWITCH PARTITION @drop_partition TO [staging].[transit_onoff]
DROP TABLE [staging].[transit_onoff];

ALTER TABLE [fact].[transit_pnr] SWITCH PARTITION @drop_partition TO [staging].[transit_pnr]
DROP TABLE [staging].[transit_pnr];




-- Create staging tables to drop on the scenario filegroup for
-- dimension tables whose primary keys are not foreign key constraints
-- create bike_link_ab dimension staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.bike_link_ab'',''U'') IS NOT NULL
DROP TABLE [staging].[bike_link_ab]

CREATE TABLE [staging].[bike_link_ab] (
	[scenario_id] int NOT NULL,
	[bike_link_ab_id] int NOT NULL,
	[bike_link_id] int NOT NULL,
	[roadsegid] int NOT NULL,
	[ab] tinyint NOT NULL,
	[from_node] int NOT NULL,
	[to_node] int NOT NULL,
	[gain] smallint NOT NULL,
	[bike_class] nchar(100) NOT NULL,
	[lanes] tinyint NOT NULL,
	[from_signal] bit NOT NULL,
	[to_signal] bit NOT NULL,
	CONSTRAINT pk_bikelinkab PRIMARY KEY ([scenario_id], [bike_link_ab_id]),
	CONSTRAINT ixuq_bikelinkab UNIQUE ([scenario_id], [roadsegid], [ab]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_bikelinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_bikelinkab_bikelink FOREIGN KEY ([scenario_id], [bike_link_id]) REFERENCES [dimension].[bike_link] ([scenario_id], [bike_link_id]),
	INDEX ix_bikelinkab_bikelink NONCLUSTERED ([scenario_id], [bike_link_id])  WITH (DATA_COMPRESSION = PAGE))
ON  ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL


-- create household dimension staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.household'',''U'') IS NOT NULL
DROP TABLE [staging].[household]

CREATE TABLE [staging].[household] (
	[scenario_id] int NOT NULL,
	[household_id] int NOT NULL, -- insert NULL record as 0
	[income] int NULL,
	[income_category] nchar(20) NOT NULL,
	[household_size] nchar(20) NOT NULL,
	[household_workers] nchar(20) NOT NULL,
	[bldgsz] nchar(35) NOT NULL,
	[unittype] nchar(35) NOT NULL,
	[autos] nchar(20) NOT NULL,
	[transponder] nchar(20) NOT NULL,
	[poverty] decimal(7,4) NULL,
	[geography_household_location_id] int NOT NULL,
	[version_household] nchar(20) NOT NULL,
	[weight_household] tinyint NOT NULL,
	INDEX ccsi_household CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create person dimension staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.person'',''U'') IS NOT NULL
DROP TABLE [staging].[person]

CREATE TABLE [staging].[person] (
	[scenario_id] int NOT NULL,
	[person_id] int NOT NULL,
	[household_id] int NOT NULL,
	[age] smallint NULL,
	[sex] nchar(20) NOT NULL,
	[military_status] nchar(20) NOT NULL,
	[employment_status] nchar(35) NOT NULL,
	[student_status] nchar(40) NOT NULL,
	[abm_person_type] nchar(40) NOT NULL,
	[education] nchar(45) NOT NULL,
	[grade] nchar(45) NOT NULL,
	[weeks] nchar(50) NOT NULL,
	[hours] nchar(40) NOT NULL,
	[race] nchar(130) NOT NULL,
	[hispanic] nchar(20) NOT NULL,
	[version_person] nchar(20) NOT NULL,
	[abm_activity_pattern] nchar(20) NOT NULL,
	[freeparking_choice] nchar(35) NOT NULL,
	[freeparking_reimbpct] float NULL,
	[work_segment] nchar(55) NOT NULL,
	[school_segment] nchar(20) NOT NULL,
	[geography_work_location_id] int NOT NULL,
	[geography_school_location_id] int NOT NULL,
	[work_distance] int NULL,
	[school_distance] int NULL,
	[weight_person] tinyint NOT NULL,
	INDEX ccsi_person CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create hwy_link_ab_tod dimension staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.hwy_link_ab_tod'',''U'') IS NOT NULL
DROP TABLE [staging].[hwy_link_ab_tod]

CREATE TABLE [staging].[hwy_link_ab_tod] (
	[scenario_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwycov_id] int NOT NULL,
	[ab] bit NOT NULL,
	[time_id] int NOT NULL,
	[cp] decimal(12,6) NOT NULL,
	[cx] decimal(12,6) NOT NULL,
	[tm] decimal(12,6) NOT NULL,
	[tx] decimal(12,6) NOT NULL,
	[ln] decimal(12,6) NOT NULL,
	[stm] decimal(12,6) NOT NULL,
	[htm] decimal(12,6) NOT NULL,
	CONSTRAINT pk_hwylinkabtod PRIMARY KEY ([scenario_id], [hwy_link_ab_tod_id]),
	CONSTRAINT ixuq_hwylinkabtod UNIQUE ([scenario_id], [hwycov_id], [ab], [time_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_hwylinkabtod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwylinkabtod_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_hwylinkabtod_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	CONSTRAINT fk_hwylinkabtod_hwylinkab FOREIGN KEY ([scenario_id], [hwy_link_ab_id]) REFERENCES [dimension].[hwy_link_ab] ([scenario_id], [hwy_link_ab_id]),
	CONSTRAINT fk_hwylinkabtod_hwylinktod FOREIGN KEY ([scenario_id], [hwy_link_tod_id]) REFERENCES [dimension].[hwy_link_tod] ([scenario_id], [hwy_link_tod_id]),
	INDEX ix_hwylinkabtod_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_hwylinkabtod_hwylinkab NONCLUSTERED ([scenario_id], [hwy_link_ab_id])  WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_hwylinkabtod_hwylinktod NONCLUSTERED ([scenario_id], [hwy_link_tod_id])  WITH (DATA_COMPRESSION = PAGE))
ON  ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL


-- create tour dimension staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.tour'',''U'') IS NOT NULL
DROP TABLE [staging].[tour]

CREATE TABLE [staging].[tour] (
	[scenario_id] int NOT NULL,
	[tour_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[model_tour_id] tinyint NOT NULL,
	[abm_tour_id] int NOT NULL,
	[time_tour_start_id] int NOT NULL,
	[time_tour_end_id] int NOT NULL,
	[geography_tour_origin_id] int NOT NULL,
	[geography_tour_destination_id] int NOT NULL,
	[mode_tour_id] tinyint NOT NULL,
	[purpose_tour_id] tinyint NOT NULL,
	[tour_category] nchar(50) NOT NULL,
	[tour_crossborder_point_of_entry] nchar(30) NOT NULL,
	[tour_crossborder_sentri] nchar(20) NOT NULL,
	[tour_visitor_auto] nchar(20) NOT NULL,
	[tour_visitor_income] nchar(15) NOT NULL,
	[weight_person_tour] decimal(4,2) NOT NULL,
	[weight_tour] smallint NOT NULL,
	INDEX ccsi_tour CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- Switch and drop partitions for all dimension tables
-- whose primary keys are not foreign key constraints
ALTER TABLE [dimension].[bike_link_ab] SWITCH PARTITION @drop_partition TO [staging].[bike_link_ab]
DROP TABLE [staging].[bike_link_ab];

ALTER TABLE [dimension].[household] SWITCH PARTITION @drop_partition TO [staging].[household]
DROP TABLE [staging].[household];

ALTER TABLE [dimension].[person] SWITCH PARTITION @drop_partition TO [staging].[person]
DROP TABLE [staging].[person];

ALTER TABLE [dimension].[hwy_link_ab_tod] SWITCH PARTITION @drop_partition TO [staging].[hwy_link_ab_tod]
DROP TABLE [staging].[hwy_link_ab_tod];

ALTER TABLE [dimension].[tour] SWITCH PARTITION @drop_partition TO [staging].[tour]
DROP TABLE [staging].[tour];




-- Run regular delete statements for remaining scenario dimension tables
-- Order matters due to foreign key constraints
-- bike_link
DELETE FROM [dimension].[bike_link] WHERE [scenario_id] = @scenario_id

-- households
DELETE FROM [dimension].[household] WHERE [scenario_id] = @scenario_id

-- highway network
DELETE FROM [dimension].[hwy_link_ab] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[hwy_link_tod] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[hwy_link] WHERE [scenario_id] = @scenario_id

-- transit network
DELETE FROM [dimension].[transit_tap] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_stop] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_route] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_link] WHERE [scenario_id] = @scenario_id

-- delete scenario from scenario table
DELETE FROM [dimension].[scenario] WHERE [scenario_id] = @scenario_id

-- remove data load request entry for the scenario in load_request table
DELETE FROM [data_load].[load_request] WHERE [scenario_id] = @scenario_id

print 'Data for scenario_id = ' + CONVERT(nvarchar, @scenario_id) +' successfully removed.'
END
GO

-- Add metadata for [dbo].[sp_clear_scen]
EXECUTE [db_meta].[add_xp] 'dbo.sp_clear_scen', 'SUBSYSTEM', 'dbo'
EXECUTE [db_meta].[add_xp] 'dbo.sp_clear_scen', 'MS_Description', 'stored procedure to remove scenario data'
GO




-- create stored procedure to remove scenario data and underlying sql files and filegroup
CREATE PROCEDURE [dbo].[sp_del_files]
	@scenario_id integer
WITH RECOMPILE AS
BEGIN
-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 04/16/2018
-- Description:	Delete scenario data, filegroup, and files
-- =============================================

SET NOCOUNT ON;

DECLARE @SQL nvarchar(max)
DECLARE @db_name nvarchar(50)


-- Make sure scenario is not set to 1
IF(@scenario_id = 1)
BEGIN
	print 'WARNING: Merging leftmost partition boundary and deleting filegroup can have unintended consequences. Canceled execution.'
	RETURN
END


-- Make sure the scenario's partition boundary hasn't been merged
-- If it has stop execution since other scenario data can potentially be deleted
IF NOT EXISTS (
	SELECT
		*
	FROM
		[sys].[partition_range_values]
	INNER JOIN
		[sys].[partition_functions]
	ON
		[partition_range_values].[function_id] = [partition_functions].[function_id]
	WHERE
		[name] = 'scenario_partition'
		AND [value] = @scenario_id)
BEGIN
	print 'WARNING: Partition function was merged. This could delete other scenarios data. Canceled execution.'
	RETURN
END


-- Returns database name
SET @db_name = CAST(DB_NAME() AS nvarchar(50))

-- Make sure scenario has been cleared from database
EXEC [dbo].[sp_clear_scen] @scenario_id

-- Merge Partition boundary value for scenario
ALTER PARTITION FUNCTION scenario_partition()
MERGE RANGE (@scenario_id);

-- Drop scenario file and filegroup
SET @SQL =
N' ALTER DATABASE ' + @db_name + '
REMOVE FILE scenario_file_' + CAST(@scenario_id AS nvarchar(5)) + N'_1;
ALTER DATABASE ' + @db_name + '
REMOVE FILE scenario_file_' + CAST(@scenario_id AS nvarchar(5)) + N'_2;
ALTER DATABASE ' + @db_name + '
REMOVE FILE scenario_file_' + CAST(@scenario_id AS nvarchar(5)) + N'_3;
ALTER DATABASE ' + @db_name + '
REMOVE FILEGROUP scenario_fg_' + CAST(@scenario_id AS nvarchar(5)) + N';'
EXEC sp_executesql @SQL
END
GO

-- Add metadata for [dbo].[sp_del_files]
EXECUTE [db_meta].[add_xp] 'dbo.sp_del_files', 'SUBSYSTEM', 'dbo'
EXECUTE [db_meta].[add_xp] 'dbo.sp_del_files', 'MS_Description', 'stored procedure to remove scenario data and delete underlying sql files and filegroup'
GO




-- typical abm user permissions
-- allows for data loading
CREATE ROLE [abm_user]
GRANT CONNECT TO [abm_user]
EXEC sp_addrolemember [db_datareader], [abm_user]
GRANT SELECT ON OBJECT:: [db_meta].[data_dictionary] TO [abm_user]
GRANT DELETE ON OBJECT:: [data_load].[load_request] TO [abm_user]
GRANT INSERT ON OBJECT:: [data_load].[load_request] TO [abm_user]
GRANT UPDATE ON OBJECT:: [data_load].[load_request] TO [abm_user]
GRANT EXECUTE ON OBJECT:: [data_load].[sp_request] TO [abm_user]
GRANT SELECT ON SCHEMA :: [dimension] TO [abm_user]
GRANT SELECT ON SCHEMA :: [fact] TO [abm_user]
GRANT VIEW DEFINITION ON SCHEMA :: [dimension] TO [abm_user]
GRANT VIEW DEFINITION ON SCHEMA :: [fact] TO [abm_user]
DENY CONTROL ON SCHEMA :: [staging] TO [abm_user]