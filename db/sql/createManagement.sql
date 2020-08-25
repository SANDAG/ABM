-- create stored procedure to remove scenario data
DROP PROCEDURE IF EXISTS [dbo].[sp_clear_scen]
GO


CREATE PROCEDURE [dbo].[sp_clear_scen]
    @scenario_id integer
WITH RECOMPILE AS
BEGIN

SET NOCOUNT ON;

-- make sure the scenario's partition boundary hasn't been merged
-- if it has stop execution since other scenario data can potentially be deleted
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

-- this is dangerous if partition function was already merged
SET @drop_partition = (SELECT $PARTITION.[scenario_partition](@scenario_id)) -- partition scenario belongs to
SET @filegroupname = 'scenario_fg_' + CAST(@scenario_id AS nvarchar)


-- create staging tables to drop on the scenario filegroup for all fact tables

-- create hwy_flow fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[hwy_flow]
CREATE TABLE [staging].[hwy_flow] (
    [scenario_id] int NOT NULL,
    [hwy_flow_id] int IDENTITY(1,1) NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwy_link_ab_tod_id] int NOT NULL,
    [time_id] int NOT NULL,
    [flow_pce] float NOT NULL,
    [time] float NOT NULL,
    [voc] float NOT NULL,
    [v_dist_t] float NOT NULL,
    [vht] float NOT NULL,
    [speed] float NOT NULL,
    [vdf] float NOT NULL,
    [msa_flow] float NOT NULL,
    [msa_time] float NOT NULL,
    [flow] float NOT NULL,
    INDEX ccsi_hwyFlow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create hwy_flow_mode fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[hwy_flow_mode]
CREATE TABLE [staging].[hwy_flow_mode] (
    [scenario_id] int NOT NULL,
    [hwy_flow_mode_id] int IDENTITY(1,1),
    [hwy_link_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwy_link_ab_tod_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_id] int NOT NULL,
    [value_of_time_category_id] int NOT NULL,
    [transponder_available_id] int NOT NULL,
    [flow] float NOT NULL,
    INDEX ccsi_hwyFlowMode CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create mgra_based_input fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[mgra_based_input]
CREATE TABLE [staging].[mgra_based_input] (
    [scenario_id] int NOT NULL,
    [mgra_based_input_id] int IDENTITY(1,1) NOT NULL,
    [geography_id] int NOT NULL,
    [hs] int NOT NULL,
    [hs_sf] int NOT NULL,
    [hs_mf] int NOT NULL,
    [hs_mh] int NOT NULL,
    [hh] int NOT NULL,
    [hh_sf] int NOT NULL,
    [hh_mf] int NOT NULL,
    [hh_mh] int NOT NULL,
    [gq_civ] int NOT NULL,
    [gq_mil] int NOT NULL,
    [i1] int NOT NULL,
    [i2] int NOT NULL,
    [i3] int NOT NULL,
    [i4] int NOT NULL,
    [i5] int NOT NULL,
    [i6] int NOT NULL,
    [i7] int NOT NULL,
    [i8] int NOT NULL,
    [i9] int NOT NULL,
    [i10] int NOT NULL,
    [hhs] float NOT NULL,
    [pop] int NOT NULL,
    [hhp] int NOT NULL,
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
    [adultschenrl] float NOT NULL,
    [ech_dist] int NOT NULL,
    [hch_dist] int NOT NULL,
    [pseudomsa] int NOT NULL,
    [parkarea] int NOT NULL,
    [hstallsoth] float NOT NULL,
    [hstallssam] float NOT NULL,
    [hparkcost] float NOT NULL,
    [numfreehrs] int NOT NULL,
    [dstallsoth] float NOT NULL,
    [dstallssam] float NOT NULL,
    [dparkcost] float NOT NULL,
    [mstallsoth] float NOT NULL,
    [mstallssam] float NOT NULL,
    [mparkcost] float NOT NULL,
    [zip09] int NOT NULL,
    [parkactive] float NOT NULL,
    [openspaceparkpreserve] float NOT NULL,
    [beachactive] float NOT NULL,
    [hotelroomtotal] float NOT NULL,
    [truckregiontype] int NOT NULL,
    [district27] int NOT NULL,
    [milestocoast] float NOT NULL,
    [acres] float NOT NULL,
    [effective_acres] float NOT NULL,
    [land_acres] float NOT NULL,
    [MicroAccessTime] int NOT NULL,
    [remoteAVParking] int NOT NULL,
    [refueling_stations] int NOT NULL,
    [totint] float NOT NULL,
    [duden] float NOT NULL,
    [empden] float NOT NULL,
    [popden] float NOT NULL,
    [retempden] float NOT NULL,
    [totintbin] int NOT NULL,
    [empdenbin] int NOT NULL,
    [dudenbin] int NOT NULL,
    [PopEmpDenPerMi] float NOT NULL,
    INDEX ccsi_mgraBasedInput CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_aggflow fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[transit_aggflow]
CREATE TABLE [staging].[transit_aggflow] (
    [scenario_id] int NOT NULL,
    [transit_aggflow_id] int IDENTITY(1,1) NOT NULL,
    [transit_link_id] int NOT NULL,
    [ab] bit NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [transit_flow] float NOT NULL,
    [non_transit_flow] float NOT NULL,
    [total_flow] float NOT NULL,
    [access_walk_flow] float NOT NULL,
    [xfer_walk_flow] float NOT NULL,
    [egress_walk_flow] float NOT NULL,
    INDEX ccsi_transitAggflow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_flow fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[transit_flow]
CREATE TABLE [staging].[transit_flow] (
    [scenario_id] int NOT NULL,
    [transit_flow_id] int IDENTITY(1,1) NOT NULL,
    [transit_route_id] int NOT NULL,
    [transit_stop_from_id] int NOT NULL,
    [transit_stop_to_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [from_mp] float NOT NULL,
    [to_mp] float NOT NULL,
    [baseivtt] float NOT NULL,
    [cost] float NOT NULL,
    [transit_flow] float NOT NULL,
    INDEX ccsi_transitFlow CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_onoff fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[transit_onoff]
CREATE TABLE [staging].[transit_onoff] (
    [scenario_id] int NOT NULL,
    [transit_onoff_id] int IDENTITY(1,1) NOT NULL,
    [transit_route_id] int NOT NULL,
    [transit_stop_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [boardings] float NOT NULL,
    [alightings] float NOT NULL,
    [walk_access_on] float NOT NULL,
    [direct_transfer_on] float NOT NULL,
    [direct_transfer_off] float NOT NULL,
    [egress_off] float NOT NULL,
    INDEX ccsi_transitOnoff CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create transit_pnr fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[transit_pnr]
CREATE TABLE [staging].[transit_pnr] (
    [scenario_id] int NOT NULL,
    [transit_pnr_id] int NOT NULL,
    [transit_tap_id] int NOT NULL,
    [lot_id] int NOT NULL,
    [geography_id] int NOT NULL,
    [time_id] int NOT NULL,
    [parking_type] nvarchar(60) NOT NULL,
    [capacity] int NOT NULL,
    [distance] float NOT NULL,
    [vehicles] float NOT NULL,
    INDEX ccsi_transitPnr CLUSTERED COLUMNSTORE)
ON ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create person trips fact staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[person_trip]
CREATE TABLE [staging].[person_trip] (
    [scenario_id] int NOT NULL,
    [person_trip_id] int IDENTITY(1,1) NOT NULL,
    [model_trip_id] int NOT NULL,
    [abm_trip_id] int NOT NULL,
    [tour_id] int NOT NULL,
    [person_id] int NOT NULL,
    [household_id] int NOT NULL,
    [mode_trip_id] int NOT NULL,
    [purpose_trip_origin_id] int NOT NULL,
    [purpose_trip_destination_id] int NOT NULL,
    [inbound_id] int NOT NULL,
    [time_trip_start_id] int NOT NULL,
    [time_trip_end_id] int NOT NULL,
    [geography_trip_origin_id] int NOT NULL,
    [geography_trip_destination_id] int NOT NULL,
    [geography_parking_destination_id] int NOT NULL,
    [transit_tap_boarding_id] int NOT NULL,
    [transit_tap_alighting_id] int NOT NULL,
    [mode_airport_arrival_id] int NOT NULL,
    [value_of_time_category_id] int NOT NULL,
    [transponder_available_id] int NOT NULL,
    [av_used_id] int NOT NULL,
    [weight_trip] float NOT NULL,
    [weight_person_trip] float NOT NULL,
    [cost_parking] float NOT NULL,
    [time_drive] float NOT NULL,
    [distance_drive] float NOT NULL,
    [cost_toll_drive] float NOT NULL,
    [cost_operating_drive] float NOT NULL,
    [cost_fare_drive] float NOT NULL,
    [time_wait_drive] float NOT NULL,
    [time_transit_in_vehicle] float NOT NULL,
    [time_tier1_transit_in_vehicle] float NOT NULL,
    [time_freeway_rapid_transit_in_vehicle] float NOT NULL,
    [time_arterial_rapid_transit_in_vehicle] float NOT NULL,
    [time_express_bus_transit_in_vehicle] float NOT NULL,
    [time_local_bus_transit_in_vehicle] float NOT NULL,
    [time_light_rail_transit_in_vehicle] float NOT NULL,
    [time_commuter_rail_transit_in_vehicle] float NOT NULL,
    [time_transit_initial_wait] float NOT NULL,
    [time_transit_wait] float NOT NULL,
    [distance_transit_in_vehicle] float NOT NULL,
    [cost_fare_transit] float NOT NULL,
    [transfers_transit] float NOT NULL,
    [time_walk] float NOT NULL,
    [distance_walk] float NOT NULL,
    [time_mm] float NOT NULL,
    [distance_mm] float NOT NULL,
    [cost_fare_mm] float NOT NULL,
    [time_mt] float NOT NULL,
    [distance_mt] float NOT NULL,
    [cost_fare_mt] float NOT NULL,
    [time_bike] float NOT NULL,
    [distance_bike] float NOT NULL,
    [time_total] float NOT NULL,
    [distance_total] float NOT NULL,
    [cost_total] float NOT NULL,
    INDEX ccsi_personTrip CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- switch and drop partitions for all fact tables
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




-- create staging tables to drop on the scenario filegroup for
-- dimension tables whose primary keys are not foreign key constraints

-- create household dimension staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[household]
CREATE TABLE [staging].[household] (
    [scenario_id] int NOT NULL,
    [household_id] int NOT NULL,  -- insert NULL record as 0
    [autos] int NULL,  -- allow NULLs
    [autos_human_vehicles] int NULL,  -- allow NULLs
    [autos_autonomous_vehicles] int NULL,  -- allow NULLs
    [transponder_available] nvarchar(25) NOT NULL,
    [geography_household_location_id] int NOT NULL,
    [household_income_category] nvarchar(20) NOT NULL,
    [household_income] int NULL,  -- allow NULLs
    [household_workers] nvarchar(20) NOT NULL,
    [household_persons] nvarchar(20) NOT NULL,
    [building_category] nvarchar(35) NOT NULL,
    [unit_type] nvarchar(35) NOT NULL,
    [poverty] float NULL,  -- allow NULLs
    INDEX ccsi_household CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create person dimension staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[person]
CREATE TABLE [staging].[person] (
    [scenario_id] int NOT NULL,
    [person_id] int NOT NULL,  -- insert NULL record as 0
    [household_id] int NOT NULL,  -- insert NULL record as 0
    [person_number] int NOT NULL,
    [age] int NULL,
    [sex] nvarchar(15) NOT NULL,
    [military_status] nvarchar(20) NOT NULL,
    [employment_status] nvarchar(35) NOT NULL,
    [student_status] nvarchar(40) NOT NULL,
    [abm_person_type] nvarchar(35) NOT NULL,
    [education] nvarchar(45) NOT NULL,
    [grade] nvarchar(35) NOT NULL,
    [weeks] nvarchar(35) NOT NULL,
    [hours] nvarchar(45) NOT NULL,
    [race] nvarchar(125) NOT NULL,
    [hispanic] nvarchar(20) NOT NULL,
    [abm_activity_pattern] nvarchar(15) NOT NULL,
    [free_parking_choice] nvarchar(35) NOT NULL,
    [parking_reimbursement_pct] float NULL,  -- allow NULLs
    [telecommute_choice] nvarchar(25) NOT NULL,
    [work_segment] nvarchar(55) NOT NULL,
    [school_segment] nvarchar(15) NOT NULL,
    [geography_work_location_id] int NOT NULL,
    [geography_school_location_id] int NOT NULL,
    INDEX ccsi_person CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- create hwy_link_ab_tod dimension staging table to drop
SET @SQL = '
DROP TABLE IF EXISTS [staging].[hwy_link_ab_tod]
CREATE TABLE [staging].[hwy_link_ab_tod] (
    [scenario_id] int NOT NULL,
    [hwy_link_ab_tod_id] int NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwycov_id] int NOT NULL,
    [ab] bit NOT NULL,
    [time_id] int NOT NULL,
    [cp] float NOT NULL,
    [cx] float NOT NULL,
    [tm] float NOT NULL,
    [tx] float NOT NULL,
    [ln] float NOT NULL,
    [stm] float NULL,  -- allow NULLs
    [htm] float NULL,  -- allow NULLs
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
SET @SQL = '
DROP TABLE IF EXISTS [staging].[tour]
CREATE TABLE [staging].[tour] (
    [scenario_id] int NOT NULL,
    [tour_id] int IDENTITY(0,1) NOT NULL,
    [model_tour_id] int NOT NULL,
    [abm_tour_id] int NOT NULL,
    [time_tour_start_id] int NOT NULL,
    [time_tour_end_id] int NOT NULL,
    [geography_tour_origin_id] int NOT NULL,
    [geography_tour_destination_id] int NOT NULL,
    [mode_tour_id] int NOT NULL,
    [purpose_tour_id] int NOT NULL,
    [tour_category] nvarchar(50) NOT NULL,
    [tour_crossborder_point_of_entry] nvarchar(20) NOT NULL,
    [tour_crossborder_sentri] nvarchar(15) NOT NULL,
    INDEX ccsi_tour CLUSTERED COLUMNSTORE)
ON  ' + @filegroupname + N';'
EXECUTE sp_executesql @SQL


-- switch and drop partitions for all dimension tables
-- whose primary keys are not foreign key constraints
ALTER TABLE [dimension].[household] SWITCH PARTITION @drop_partition TO [staging].[household]
DROP TABLE [staging].[household];

ALTER TABLE [dimension].[person] SWITCH PARTITION @drop_partition TO [staging].[person]
DROP TABLE [staging].[person];

ALTER TABLE [dimension].[hwy_link_ab_tod] SWITCH PARTITION @drop_partition TO [staging].[hwy_link_ab_tod]
DROP TABLE [staging].[hwy_link_ab_tod];

ALTER TABLE [dimension].[tour] SWITCH PARTITION @drop_partition TO [staging].[tour]
DROP TABLE [staging].[tour];




-- run regular delete statements for remaining scenario dimension tables
-- order matters due to foreign key constraints

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


-- add metadata for [dbo].[sp_clear_scen]
EXECUTE [db_meta].[add_xp] 'dbo.sp_clear_scen', 'MS_Description', 'stored procedure to remove scenario data'
GO




-- create stored procedure to remove scenario data and underlying sql files and filegroup
CREATE PROCEDURE [dbo].[sp_del_files]
	@scenario_id integer
WITH RECOMPILE AS
BEGIN

SET NOCOUNT ON;

DECLARE @SQL nvarchar(max)
DECLARE @db_name nvarchar(50)


-- make sure scenario is not set to 1
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


-- returns database name
SET @db_name = CAST(DB_NAME() AS nvarchar(50))

-- make sure scenario has been cleared from database
EXECUTE [dbo].[sp_clear_scen] @scenario_id

-- merge Partition boundary value for scenario
ALTER PARTITION FUNCTION scenario_partition()
MERGE RANGE (@scenario_id);

-- drop scenario file and filegroup
SET @SQL =
' ALTER DATABASE ' + @db_name + '
REMOVE FILE scenario_file_' + CAST(@scenario_id AS nvarchar(5)) + ';
ALTER DATABASE ' + @db_name + '
REMOVE FILEGROUP scenario_fg_' + CAST(@scenario_id AS nvarchar(5)) + ';'
EXECUTE sp_executesql @SQL
END
GO

-- add metadata for [dbo].[sp_del_files]
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