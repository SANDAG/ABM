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
	CONSTRAINT pk_bikeflow PRIMARY KEY ([scenario_id], [bike_flow_id]),
	CONSTRAINT ixuq_bikeflow UNIQUE ([scenario_id], [bike_link_ab_id], [time_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_bikeflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_bikeflow_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_bikeflow_bikelink FOREIGN KEY ([scenario_id], [bike_link_id]) REFERENCES [dimension].[bike_link] ([scenario_id], [bike_link_id]),
	CONSTRAINT fk_bikeflow_bikelinkab FOREIGN KEY ([scenario_id], [bike_link_ab_id]) REFERENCES [dimension].[bike_link_ab] ([scenario_id], [bike_link_ab_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_hwyflow PRIMARY KEY ([scenario_id], [hwy_flow_id]),
	CONSTRAINT fk_hwyflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwyflow_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_hwyflow_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	CONSTRAINT fk_hwyflow_hwylinkab FOREIGN KEY ([scenario_id], [hwy_link_ab_id]) REFERENCES [dimension].[hwy_link_ab] ([scenario_id], [hwy_link_ab_id]),
	CONSTRAINT fk_hwyflow_hwylinktod FOREIGN KEY ([scenario_id], [hwy_link_tod_id]) REFERENCES [dimension].[hwy_link_tod] ([scenario_id], [hwy_link_tod_id]),
	CONSTRAINT fk_hwyflow_hwylinkabtod FOREIGN KEY ([scenario_id], [hwy_link_ab_tod_id]) REFERENCES [dimension].[hwy_link_ab_tod] ([scenario_id], [hwy_link_ab_tod_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_hwyflowmode PRIMARY KEY ([scenario_id], [hwy_flow_mode_id]),
	CONSTRAINT fk_hwyflowmode_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwyflowmode_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_hwyflowmode_mode FOREIGN KEY ([mode_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_hwyflowmode_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	CONSTRAINT fk_hwyflowmode_hwylinkab FOREIGN KEY ([scenario_id], [hwy_link_ab_id]) REFERENCES [dimension].[hwy_link_ab] ([scenario_id], [hwy_link_ab_id]),
	CONSTRAINT fk_hwyflowmode_hwylinktod FOREIGN KEY ([scenario_id], [hwy_link_tod_id]) REFERENCES [dimension].[hwy_link_tod] ([scenario_id], [hwy_link_tod_id]),
	CONSTRAINT fk_hwyflowmode_hwylinkabtod FOREIGN KEY ([scenario_id], [hwy_link_ab_tod_id]) REFERENCES [dimension].[hwy_link_ab_tod] ([scenario_id], [hwy_link_ab_tod_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL


-- create mgra_based_input fact staging table to drop
SET @SQL = N'
IF OBJECT_ID(''staging.mgra_based_input'',''U'') IS NOT NULL
DROP TABLE [staging].[mgra_based_input]

CREATE TABLE [staging].[mgra_based_input] (
	[scenario_id] int NOT NULL,
	[mgra_based_input_id] int IDENTITY(1,1) NOT NULL,
	[geography_id] int NOT NULL,
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
	[hhs] decimal(6,4) NOT NULL,
	[pop] smallint NOT NULL,
	[hhp] smallint NOT NULL,
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
	[hparkcost] decimal(4,2) NOT NULL,
	[numfreehrs] tinyint NOT NULL,
	[dstallsoth] smallint NOT NULL,
    [dstallssam] smallint NOT NULL,
    [dparkcost] decimal(4,2) NOT NULL,
    [mstallsoth] smallint NOT NULL,
    [mstallssam] smallint NOT NULL,
    [mparkcost] decimal(4,2) NOT NULL,
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
	[budgetroom] decimal(12,7) NOT NULL,
    [economyroom] decimal(12,7) NOT NULL,
    [luxuryroom] decimal(12,7) NOT NULL,
    [midpriceroom] decimal(12,7) NOT NULL,
    [upscaleroom] decimal(12,7) NOT NULL,
    [hotelroomtotal] decimal(12,7) NOT NULL,
	[truckregiontype] tinyint NOT NULL,
    [district27] tinyint NOT NULL,
    [milestocoast] decimal(7,4) NOT NULL,
    [acres] decimal(10,5) NOT NULL,
    [effective_acres] decimal(10,5) NOT NULL,
    [land_acres] decimal(10,5) NOT NULL,
	CONSTRAINT pk_mgrabasedinput PRIMARY KEY ([scenario_id], [mgra_based_input_id]),
	CONSTRAINT fk_mgrabasedinput_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_mgrabasedinput_geography FOREIGN KEY ([geography_id]) REFERENCES [dimension].[geography] ([geography_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_transitaggflow PRIMARY KEY ([scenario_id], [transit_aggflow_id]),
	CONSTRAINT ixuq_transitaggflow UNIQUE ([scenario_id], [transit_link_id], [ab], [time_id], [mode_transit_id], [mode_transit_access_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitaggflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitaggflow_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_transitaggflow_modetransit FOREIGN KEY ([mode_transit_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitaggflow_modetransitaccess FOREIGN KEY ([mode_transit_access_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitaggflow_transitlink FOREIGN KEY ([scenario_id], [transit_link_id]) REFERENCES [dimension].[transit_link] ([scenario_id], [transit_link_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_transitflow PRIMARY KEY ([scenario_id], [transit_flow_id]),
	CONSTRAINT ixuq_transitflow UNIQUE ([scenario_id], [transit_route_id], [transit_stop_from_id], [transit_stop_to_id], [time_id], [mode_transit_id], [mode_transit_access_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitflow_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitflow_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_transitflow_modetransit FOREIGN KEY ([mode_transit_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitflow_modetransitaccess FOREIGN KEY ([mode_transit_access_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitflow_transitroute FOREIGN KEY ([scenario_id], [transit_route_id]) REFERENCES [dimension].[transit_route] ([scenario_id], [transit_route_id]),
	CONSTRAINT fk_transitflow_transitstopfrom FOREIGN KEY ([scenario_id], [transit_stop_from_id]) REFERENCES [dimension].[transit_stop] ([scenario_id], [transit_stop_id]),
	CONSTRAINT fk_transitflow_totransitstopto FOREIGN KEY ([scenario_id], [transit_stop_to_id]) REFERENCES [dimension].[transit_stop] ([scenario_id], [transit_stop_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_transitonoff PRIMARY KEY ([scenario_id], [transit_onoff_id]),
	CONSTRAINT ixuq_transitonoff UNIQUE ([scenario_id], [transit_route_id], [transit_stop_id], [time_id], [mode_transit_id], [mode_transit_access_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitonoff_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitonoff_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_transitonoff_modetransit FOREIGN KEY ([mode_transit_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitonoff_modetransitaccess FOREIGN KEY ([mode_transit_access_id]) REFERENCES [dimension].[mode] ([mode_id]),
	CONSTRAINT fk_transitonoff_transitroute FOREIGN KEY ([scenario_id], [transit_route_id]) REFERENCES [dimension].[transit_route] ([scenario_id], [transit_route_id]),
	CONSTRAINT fk_transitonoff_transitstop FOREIGN KEY ([scenario_id], [transit_stop_id]) REFERENCES [dimension].[transit_stop] ([scenario_id], [transit_stop_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
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
	CONSTRAINT pk_transitpnr PRIMARY KEY ([scenario_id], [transit_pnr_id]),
	CONSTRAINT ixuq_transitpnr UNIQUE ([scenario_id], [transit_tap_id], [time_id]) WITH (DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitpnr_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitpnr_geography FOREIGN KEY ([time_id]) REFERENCES [dimension].[geography] ([geography_id]),
	CONSTRAINT fk_transitpnr_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_transitonoff_transittap FOREIGN KEY ([scenario_id], [transit_tap_id]) REFERENCES [dimension].[transit_tap] ([scenario_id], [transit_tap_id]))
ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL



-- create person trips fact staging table to drop
-- partitioned clustered columnstore
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
	[person_escort_drive_id] int NOT NULL,
	[escort_stop_type_origin_id] tinyint NOT NULL,
	[person_escort_origin_id] int NOT NULL,
	[escort_stop_type_destination_id] tinyint NOT NULL,
	[person_escort_destination_id] int NOT NULL,
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
	[cost_transit] decimal(4, 2) NOT NULL,
	[time_transit_auxiliary] decimal(10, 4) NOT NULL,
	[time_transit_wait] decimal(10, 4) NOT NULL,
	[transit_transfers] decimal(6,4) NOT NULL,
	[time_total] decimal(10, 4) NOT NULL,
	[dist_total] decimal(10, 4) NOT NULL,
	[cost_total] decimal(4, 2) NOT NULL,
	[value_of_time] decimal(8,2) NOT NULL,
	[value_of_time_drive_bin_id] tinyint NOT NULL,
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


-- Run regular delete statements for scenario dimension tables
-- Order matters due to foreign key constraints

-- bike network
DELETE FROM [dimension].[bike_link_ab] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[bike_link] WHERE [scenario_id] = @scenario_id

-- households and persons
DELETE FROM [dimension].[person] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[household] WHERE [scenario_id] = @scenario_id

-- highway network
DELETE FROM [dimension].[hwy_link_ab_tod] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[hwy_link_ab] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[hwy_link_tod] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[hwy_link] WHERE [scenario_id] = @scenario_id

-- transit network
DELETE FROM [dimension].[transit_tap] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_stop] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_route] WHERE [scenario_id] = @scenario_id
DELETE FROM [dimension].[transit_link] WHERE [scenario_id] = @scenario_id

-- tours
DELETE FROM [dimension].[tour] WHERE [scenario_id] = @scenario_id

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