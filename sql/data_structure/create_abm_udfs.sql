IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[abm].[sp_index_frag]') AND type in (N'P', N'PC'))
DROP PROCEDURE [abm].[sp_index_frag]
GO

CREATE PROCEDURE [abm].[sp_index_frag] 
	-- Add the parameters for the stored procedure here
	@scenario_id smallint 
AS

-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 08/05/2014
-- Description:	Index Fragmentation Statistics for selected scenario partition
-- =============================================

DECLARE @dest_partition smallint
SET @dest_partition = (SELECT $PARTITION.[scenario_partition](@scenario_id)) -- Partition containing scenario

SELECT 
	@scenario_id AS scenario_id
	,partition_number
	,ps.database_id
	,ps.OBJECT_ID
	,ps.index_id
	,b.name,
	ps.avg_fragmentation_in_percent
FROM 
	sys.dm_db_index_physical_stats (DB_ID(), NULL, NULL, NULL, NULL) AS ps
INNER JOIN 
	sys.indexes AS b 
ON 
	ps.OBJECT_ID = b.OBJECT_ID
	AND ps.index_id = b.index_id
WHERE 
	ps.database_id = DB_ID() AND partition_number = @dest_partition
ORDER BY 
	partition_number, ps.OBJECT_ID
	
GO


-- Add metadata for [abm].[sp_index_frag]
EXECUTE [db_meta].[add_xp] 'abm.sp_index_frag', 'SUBSYSTEM', 'abm'
EXECUTE [db_meta].[add_xp] 'abm.sp_index_frag', 'MS_Description', 'return index fragmentation statistics on associated parition for a scenario'
GO


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[abm].[sp_shrink_reorg]') AND type in (N'P', N'PC'))
DROP PROCEDURE [abm].[sp_shrink_reorg]
GO

CREATE PROCEDURE [abm].[sp_shrink_reorg] 
	@scenario_id smallint
	,@num_gb smallint
AS

-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 08/05/2014
-- Description:	Shrink and Reorganize selected scenario partition
-- =============================================

-- Declare local variables
DECLARE @dest_partition smallint
DECLARE @scen_file nvarchar(20)
DECLARE @SQL nvarchar(max)
DECLARE @db_name nvarchar(50)
DECLARE @num_mb smallint


-- Make sure gigabytes were specified
IF(@num_gb > 35)
BEGIN
	print 'WARNING: Other unit than gigabytes specified? Canceled execution.'
	RETURN
END


	-- Returns database name
	SET @db_name = CAST(DB_NAME() AS nvarchar(50))

	-- Finds the partition number and scenario file name of the scenario
	SET @dest_partition = 
	(SELECT $PARTITION.[scenario_partition](@scenario_id))
	SET @scen_file = N'scenario_file_' + CAST(@scenario_id AS nvarchar(5))
	
	-- Convert Gigabytes to Megabytes
	SET @num_mb = @num_gb * 1024

	-- Shrink scenario file to ??MB
	DBCC SHRINKFILE (@scen_file , @num_mb)
	-- Set the maxsize and filegrowth to ??GB and 0 respectively for the scenario file
	SET @SQL =
	N'ALTER DATABASE ' + @db_name + N' MODIFY FILE (NAME = ' + @scen_file + N', MAXSIZE = ' + CAST(@num_gb AS nvarchar) + 'GB, FILEGROWTH = 0)'
	EXECUTE sp_executesql @SQL

	-- Reorganize indices on the partition
	ALTER INDEX ALL ON [abm].[at_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[bike_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[bike_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[bike_link_ab] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[cbd_vehicles] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_flow_mode] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_link_ab] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_link_ab_tod] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_link_tod] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[hwy_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[lu_hh] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[lu_mgra_input] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[lu_person] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[lu_person_fp] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[lu_person_lc] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[tour_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[tour_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[tour_ij_person] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[tour_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_aggflow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_onoff] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_pnr] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_route] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_stop] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_tap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_tap_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[transit_tap_walk] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_agg] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX ALL ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
GO


-- Add metadata for [abm].[sp_shrink_reorg]
EXECUTE [db_meta].[add_xp] 'abm.sp_shrink_reorg', 'SUBSYSTEM', 'abm'
EXECUTE [db_meta].[add_xp] 'abm.sp_shrink_reorg', 'MS_Description', 'shrink associated file and reorganize associated partition indices for a selected scenario'
GO


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[abm].[sp_clear_scen]') AND type in (N'P', N'PC'))
DROP PROCEDURE [abm].[sp_clear_scen]
GO

CREATE PROCEDURE [abm].[sp_clear_scen] 
	-- Add the parameters for the stored procedure here
	@scenario_id smallint
AS

-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 08/05/2014
-- Description:	Clear out selected scenario data via partition switching
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
		AND [value] = @scenario_id
	)
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



/* Drop all SSIS staging data for the scenario if it exists */
IF OBJECT_ID('abm_staging.at_skims_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.at_skims_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.bike_flow_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.bike_flow_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.bike_link_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.bike_link_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.bike_link_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.bike_link_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.bike_link_ab_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.bike_link_ab_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.cbd_vehicles_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.cbd_vehicles_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_flow_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_flow_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_flow_mode_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_flow_mode_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_link_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_link_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_link_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_link_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_link_ab_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_link_ab_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_link_ab_tod_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_link_ab_tod_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_link_tod_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_link_tod_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.hwy_skims_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.hwy_skims_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.lu_hh_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.lu_hh_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.lu_mgra_input_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.lu_mgra_input_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.lu_person_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.lu_person_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.lu_person_fp_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.lu_person_fp_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.lu_person_lc_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.lu_person_lc_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.tour_cb_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.tour_cb_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.tour_ij_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.tour_ij_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.tour_ij_person_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.tour_ij_person_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.tour_vis_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.tour_vis_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_aggflow_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_aggflow_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_flow_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_flow_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_link_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_link_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_link_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_link_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_onoff_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_onoff_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_pnr_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_pnr_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_route_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_route_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_route_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_route_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_stop_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_stop_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_stop_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_stop_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_tap_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_tap_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_tap_shape_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_tap_shape_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_tap_skims_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_tap_skims_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_tap_walk_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_tap_walk_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_agg_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_agg_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_ap_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_ap_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_cb_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_cb_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_ie_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_ie_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_ij_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_ij_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.trip_vis_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.trip_vis_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END


-- Create staging tables on the same filegroup for non foreign keyed tables
SET @SQL =
N'IF OBJECT_ID(''abm_staging.at_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[at_skims_drop]

CREATE TABLE 
	[abm_staging].[at_skims_drop] (
		[scenario_id] smallint NOT NULL,
		[at_skims_id] int IDENTITY(1,1) NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
    	[dest_geography_zone_id] int NOT NULL,
    	[mode_id] tinyint NOT NULL,
		[time_period_id] int NOT NULL,
    	[skim_id] tinyint NOT NULL,
    	[value] decimal(8,4) NOT NULL,
		CONSTRAINT pk_atskims_drop PRIMARY KEY([scenario_id],[at_skims_id]),
		CONSTRAINT ixuq_atskims_drop UNIQUE ([scenario_id],[orig_geography_zone_id],[dest_geography_zone_id],[mode_id],[time_period_id],[skim_id]),
		CONSTRAINT fk_atskims_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_atskims_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_atskims_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_atskims_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_atskims_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
		CONSTRAINT fk_atskims_skim_drop FOREIGN KEY ([skim_id]) REFERENCES [ref].[skim] ([skim_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.bike_flow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[bike_flow_drop]

CREATE TABLE 
	[abm_staging].[bike_flow_drop] (
		[scenario_id] smallint NOT NULL,
		[bike_flow_id] int IDENTITY(1,1) NOT NULL,
		[bike_link_ab_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[flow] decimal(8,4) NOT NULL,
		CONSTRAINT pk_bikeflow_drop PRIMARY KEY ([scenario_id],[bike_flow_id]),
		CONSTRAINT ixuq_bikeflow_drop UNIQUE ([scenario_id],[bike_link_ab_id],[time_period_id]),
		CONSTRAINT fk_bikeflow_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_bikeflow_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_bikeflow_bikelinkab_drop FOREIGN KEY ([scenario_id],[bike_link_ab_id]) REFERENCES [abm].[bike_link_ab] ([scenario_id],[bike_link_ab_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.cbd_vehicles_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[cbd_vehicles_drop]

CREATE TABLE 
	[abm_staging].[cbd_vehicles_drop] (
		[scenario_id] smallint NOT NULL,
		[cbd_vehicles_id] int IDENTITY(1,1) NOT NULL,
		[geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[vehicles] decimal(10,6) NOT NULL,
		CONSTRAINT pk_cbdvehicles_drop PRIMARY KEY ([scenario_id],[cbd_vehicles_id]),
		CONSTRAINT ixuq_cbdvehicles_drop UNIQUE ([scenario_id],[geography_zone_id],[time_period_id]),
		CONSTRAINT fk_cbdvehicles_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_cbdvehicles_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_cbdvehicles_zone_drop FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone]([geography_zone_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.hwy_flow_mode_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[hwy_flow_mode_drop]

CREATE TABLE 
	[abm_staging].[hwy_flow_mode_drop] (
		[scenario_id] smallint NOT NULL,
		[hwy_flow_mode_id] int IDENTITY(1,1) NOT NULL,
		[hwy_flow_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[flow] decimal(12,6) NOT NULL,
		CONSTRAINT pk_hwyflowmode_drop PRIMARY KEY ([scenario_id],[hwy_flow_mode_id]),
		CONSTRAINT ixuq_hwyflowmode_drop UNIQUE ([scenario_id],[hwy_flow_id],[mode_id]),
		CONSTRAINT fk_hwyflowmode_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_hwyflowmode_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_hwyflowmode_hwyflow_drop FOREIGN KEY ([scenario_id],[hwy_flow_id]) REFERENCES [abm].[hwy_flow] ([scenario_id],[hwy_flow_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.hwy_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[hwy_skims_drop]

CREATE TABLE 
	[abm_staging].[hwy_skims_drop] (
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
		CONSTRAINT pk_hwyskims_drop PRIMARY KEY ([scenario_id],[hwy_skims_id]),
		CONSTRAINT ixuq_hwyskims_drop UNIQUE ([scenario_id],[orig_geography_zone_id],[dest_geography_zone_id],[time_period_id]),
		CONSTRAINT fk_hwyskims_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_hwyskims_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period] ([time_period_id]),
		CONSTRAINT fk_hwyskims_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_hwyskims_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.lu_mgra_input_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[lu_mgra_input_drop]

CREATE TABLE 
	[abm_staging].[lu_mgra_input_drop] (
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
		CONSTRAINT pk_lumgrainput_drop PRIMARY KEY ([scenario_id],[lu_mgra_input_id]),
		CONSTRAINT ixuq_lumgrainput_drop UNIQUE ([scenario_id],[geography_zone_id]),
		CONSTRAINT fk_lumgrainput_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_lumgrainput_zone_drop FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.lu_person_fp_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[lu_person_fp_drop]

CREATE TABLE 
	[abm_staging].[lu_person_fp_drop] (
		[scenario_id] smallint NOT NULL,
		[lu_person_fp_id] int IDENTITY(1,1) NOT NULL,
		[lu_person_id] int NOT NULL,
		[fp_choice_id] tinyint NOT NULL,
		[reimb_pct] decimal(8,6) NULL,
		CONSTRAINT pk_lupersonfp_drop PRIMARY KEY ([scenario_id],[lu_person_fp_id]),
		CONSTRAINT ixuq_lupersonfp_drop UNIQUE ([scenario_id],[lu_person_id]),
		CONSTRAINT fk_lupersonfp_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_lupersonfp_fpchoice_drop FOREIGN KEY ([fp_choice_id]) REFERENCES [ref].[fp_choice] ([fp_choice_id]),
		CONSTRAINT fk_lupersonfp_luperson_drop FOREIGN KEY ([scenario_id],[lu_person_id]) REFERENCES [abm].[lu_person] ([scenario_id],[lu_person_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.lu_person_lc_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[lu_person_lc_drop]

CREATE TABLE 
	[abm_staging].[lu_person_lc_drop] (
		[scenario_id] smallint NOT NULL,
		[lu_person_lc_id] int IDENTITY(1,1) NOT NULL,
		[lu_person_id] int NOT NULL,
		[loc_choice_segment_id] int NOT NULL,
		[geography_zone_id] int NOT NULL,
		CONSTRAINT pk_lupersonlc_drop PRIMARY KEY ([scenario_id],[lu_person_lc_id]),
		CONSTRAINT ixuq_lupersonlc_drop UNIQUE ([scenario_id],[lu_person_id],[loc_choice_segment_id]),
		CONSTRAINT fk_lupersonlc_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_lupersonlc_segment_drop FOREIGN KEY ([loc_choice_segment_id]) REFERENCES [ref].[loc_choice_segment] ([loc_choice_segment_id]),
		CONSTRAINT fk_lupersonlc_zone_drop FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_lupersonlc_luperson_drop FOREIGN KEY ([scenario_id],[lu_person_id]) REFERENCES [abm].[lu_person] ([scenario_id],[lu_person_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.tour_ij_person_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[tour_ij_person_drop]

CREATE TABLE 
	[abm_staging].[tour_ij_person_drop] (
		[scenario_id] smallint NOT NULL,
		[tour_ij_person_id] int IDENTITY(1,1) NOT NULL,
		[tour_ij_id] int NOT NULL,
		[lu_person_id] int NOT NULL,
		CONSTRAINT pk_tourijperson_drop PRIMARY KEY ([scenario_id],[tour_ij_person_id]),
		CONSTRAINT ixuq_tourijperson_drop UNIQUE ([scenario_id], [tour_ij_id], [lu_person_id]),
		CONSTRAINT fk_tourijperson_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tourijperson_tourij_drop FOREIGN KEY ([scenario_id],[tour_ij_id]) REFERENCES [abm].[tour_ij] ([scenario_id],[tour_ij_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_aggflow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_aggflow_drop]

CREATE TABLE 
	[abm_staging].[transit_aggflow_drop] (
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
		CONSTRAINT pk_transitaggflow_drop PRIMARY KEY ([scenario_id],[transit_aggflow_id]),
		CONSTRAINT ixuq_transitaggflow_drop UNIQUE ([scenario_id],[transit_link_id],[ab],[time_period_id],[transit_mode_id],[transit_access_mode_id]),
		CONSTRAINT fk_transitaggflow_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitaggflow_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_transitaggflow_transitmode_drop FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode]([transit_mode_id]),
		CONSTRAINT fk_transitaggflow_accessmode_drop FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id]),
		CONSTRAINT fk_transitaggflow_link_drop FOREIGN KEY ([scenario_id],[transit_link_id]) REFERENCES [abm].[transit_link] ([scenario_id],[transit_link_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_flow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_flow_drop]

CREATE TABLE 
	[abm_staging].[transit_flow_drop] (
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
		CONSTRAINT pk_transitflow_drop PRIMARY KEY ([scenario_id],[transit_flow_id]),
		CONSTRAINT ixuq_transitflow_drop UNIQUE ([scenario_id],[transit_route_id],[from_transit_stop_id],[to_transit_stop_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]),
		CONSTRAINT fk_transitflow_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitflow_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_transitflow_transitmode_drop FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode]([transit_mode_id]),
		CONSTRAINT fk_transitflow_accessmode_drop FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id]),
		CONSTRAINT fk_transitflow_route_drop FOREIGN KEY ([scenario_id],[transit_route_id]) REFERENCES [abm].[transit_route] ([scenario_id],[transit_route_id]),
		CONSTRAINT fk_transitflow_fromstop_drop FOREIGN KEY ([scenario_id],[from_transit_stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id],[transit_stop_id]),
		CONSTRAINT fk_transitflow_tostop_drop FOREIGN KEY ([scenario_id],[to_transit_stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id],[transit_stop_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_onoff_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_onoff_drop]

CREATE TABLE 
	[abm_staging].[transit_onoff_drop] (
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
		CONSTRAINT pk_transitonoff_drop PRIMARY KEY ([scenario_id],[transit_onoff_id]),
		CONSTRAINT ixuq_transitonoff_drop UNIQUE ([scenario_id],[transit_route_id],[transit_stop_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]),
		CONSTRAINT fk_transitonoff_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitonoff_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_transitonoff_transitmode_drop FOREIGN KEY ([transit_mode_id]) REFERENCES [ref].[transit_mode] ([transit_mode_id]),
		CONSTRAINT fk_transitonoff_accessmode_drop FOREIGN KEY ([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode]([transit_access_mode_id]),
		CONSTRAINT fk_transitonoff_route_drop FOREIGN KEY ([scenario_id],[transit_route_id]) REFERENCES [abm].[transit_route] ([scenario_id],[transit_route_id]),
		CONSTRAINT fk_transitonoff_stop_drop FOREIGN KEY ([scenario_id],[transit_stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id],[transit_stop_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_pnr_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_pnr_drop]

CREATE TABLE 
	[abm_staging].[transit_pnr_drop] (
		[scenario_id] smallint NOT NULL,
		[transit_pnr_id] int IDENTITY(1,1) NOT NULL,
		[transit_tap_id] int NOT NULL,
		[lot_id] smallint NOT NULL,
		[geography_zone_id] int NULL,
		[time_period_id] int NOT NULL,
		[parking_type_id] tinyint NOT NULL,
		[capacity] smallint NOT NULL,
		[distance] smallint NOT NULL,
		[vehicles] smallint NOT NULL,
		CONSTRAINT pk_transitpnr_drop PRIMARY KEY ([scenario_id],[transit_pnr_id]),
		CONSTRAINT ixuq_transitpnr_drop UNIQUE ([scenario_id],[transit_tap_id],[lot_id],[time_period_id]),
		CONSTRAINT fk_transitpnr_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transitpnr_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_transitpnr_zone_drop FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone]([geography_zone_id]),
		CONSTRAINT fk_transitpnr_tap_drop FOREIGN KEY ([scenario_id],[transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_tap_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_tap_skims_drop]

CREATE TABLE 
	[abm_staging].[transit_tap_skims_drop] (
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
		CONSTRAINT pk_transittapskims_drop PRIMARY KEY ([scenario_id],[transit_tap_skims_id]),
		CONSTRAINT ixuq_transittapskims_drop UNIQUE ([scenario_id],[orig_transit_tap_id],[dest_transit_tap_id],[time_period_id]),
		CONSTRAINT fk_transittapskims_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transittapskims_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_transittapskims_origtap_drop FOREIGN KEY ([scenario_id],[orig_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_transittapskims_desttap_drop FOREIGN KEY ([scenario_id],[dest_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_tap_walk_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_tap_walk_drop]

CREATE TABLE 
	[abm_staging].[transit_tap_walk_drop] (
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
		CONSTRAINT pk_transittapwalk_drop PRIMARY KEY ([scenario_id],[transit_tap_walk_id]),
		CONSTRAINT ixuq_transittapwalk_drop UNIQUE ([scenario_id],[geography_zone_id],[transit_tap_id]),
		CONSTRAINT fk_transittapwalk_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_transittapwalk_zone_drop FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_transittapwalk_tap_drop FOREIGN KEY ([scenario_id],[transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ap_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ap_drop]

CREATE TABLE 
	[abm_staging].[trip_ap_drop] (
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
		CONSTRAINT pk_tripap_drop PRIMARY KEY ([scenario_id],[trip_ap_id]),
		CONSTRAINT ixuq_tripap_drop UNIQUE ([scenario_id],[model_type_id],[trip_id]),
		CONSTRAINT fk_tripap_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripap_model_drop FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripap_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripap_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripap_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripap_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripap_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripap_income_drop FOREIGN KEY ([ap_income_cat_id]) REFERENCES [ref].[ap_income_cat]([ap_income_cat_id]),
		CONSTRAINT fk_tripap_arrivalmode_drop FOREIGN KEY ([ap_arrival_mode_id]) REFERENCES [ref].[ap_arrival_mode]([ap_arrival_mode_id]),
		CONSTRAINT fk_tripap_boardtap_drop FOREIGN KEY ([scenario_id],[board_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_tripap_alighttap_drop FOREIGN KEY ([scenario_id],[alight_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE NONCLUSTERED INDEX
	ix_tripap_boardtap_drop
ON
	[abm_staging].[trip_ap_drop] ([scenario_id],[board_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripap_alighttap_drop
ON
	[abm_staging].[trip_ap_drop] ([scenario_id],[alight_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_agg_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_agg_drop]

CREATE TABLE 
	[abm_staging].[trip_agg_drop] (
		[scenario_id] smallint NOT NULL,
		[trip_agg_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[orig_geography_zone_id] int NOT NULL,
		[dest_geography_zone_id] int NOT NULL,
		[time_period_id] int NOT NULL,
		[purpose_id] int NOT NULL,
		[mode_id] tinyint NOT NULL,
		[trips] decimal(20,16) NOT NULL,
		CONSTRAINT pk_tripagg_drop PRIMARY KEY ([scenario_id],[trip_agg_id]),
		CONSTRAINT fk_tripagg_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripagg_model_drop FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type]([model_type_id]),
		CONSTRAINT fk_tripagg_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[PURPOSE]([purpose_id]),
		CONSTRAINT fk_tripagg_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripagg_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[MODE]([mode_id]),
		CONSTRAINT fk_tripagg_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripagg_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE UNIQUE NONCLUSTERED INDEX 
	[ixuq_tripagg_drop] 
ON 
	[abm_staging].[trip_agg_drop]
	(
		[scenario_id] ASC,
		[model_type_id] ASC,
		[orig_geography_zone_id] ASC,
		[dest_geography_zone_id] ASC,
		[time_period_id] ASC,
		[purpose_id] ASC,
		[mode_id] ASC
	)
INCLUDE 
	([trips]) 
WITH 
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_cb_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_cb_drop]

CREATE TABLE 
	[abm_staging].[trip_cb_drop] (
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
		CONSTRAINT pk_tripcb_drop PRIMARY KEY ([scenario_id],[trip_cb_id]),
		CONSTRAINT ixuq_tripcb_drop UNIQUE ([scenario_id],[tour_cb_id],[trip_id]),
		CONSTRAINT fk_tripcb_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripcb_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripcb_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripcb_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripcb_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripcb_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripcb_tour_drop FOREIGN KEY ([scenario_id],[tour_cb_id]) REFERENCES [abm].[tour_cb] ([scenario_id],[tour_cb_id]),
		CONSTRAINT fk_tripcb_boardtap_drop FOREIGN KEY ([scenario_id],[board_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_tripcb_alighttap_drop FOREIGN KEY ([scenario_id],[alight_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE NONCLUSTERED INDEX
	ix_tripcb_boardtap_drop
ON
	[abm_staging].[trip_cb_drop] ([scenario_id],[board_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripcb_alighttap_drop
ON
	[abm_staging].[trip_cb_drop] ([scenario_id],[alight_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ie_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ie_drop]

CREATE TABLE 
	[abm_staging].[trip_ie_drop] (
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
		CONSTRAINT pk_tripie_drop PRIMARY KEY ([scenario_id],[trip_ie_id]),
		CONSTRAINT ixuq_tripie_drop UNIQUE ([scenario_id],[model_type_id],[trip_id]),
		CONSTRAINT fk_tripie_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripie_model_drop FOREIGN KEY ([model_type_id]) REFERENCES [ref].[MODEL_TYPE]([model_type_id]),
		CONSTRAINT fk_tripie_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripie_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripie_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripie_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripie_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripie_boardtap_drop FOREIGN KEY ([scenario_id],[board_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_tripie_alighttap_drop FOREIGN KEY ([scenario_id],[alight_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE NONCLUSTERED INDEX
	ix_tripie_boardtap_drop
ON
	[abm_staging].[trip_ie_drop] ([scenario_id],[board_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripie_alighttap_drop
ON
	[abm_staging].[trip_ie_drop] ([scenario_id],[alight_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ij_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ij_drop]

CREATE TABLE 
	[abm_staging].[trip_ij_drop] (
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
		CONSTRAINT pk_tripij_drop PRIMARY KEY ([scenario_id],[trip_ij_id]),
		CONSTRAINT ixuq_tripij_drop UNIQUE ([scenario_id],[tour_ij_id],[trip_id]),
		CONSTRAINT fk_tripij_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripij_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripij_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripij_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripij_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripij_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripij_parking_zone_drop FOREIGN KEY ([parking_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripij_tour_drop FOREIGN KEY ([scenario_id],[tour_ij_id]) REFERENCES [abm].[tour_ij] ([scenario_id],[tour_ij_id]),
		CONSTRAINT fk_tripij_boardtap_drop FOREIGN KEY ([scenario_id],[board_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_tripij_alighttap_drop FOREIGN KEY ([scenario_id],[alight_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE NONCLUSTERED INDEX
	ix_tripij_boardtap_drop
ON
	[abm_staging].[trip_ij_drop] ([scenario_id],[board_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripij_alighttap_drop
ON
	[abm_staging].[trip_ij_drop] ([scenario_id],[alight_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_vis_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_vis_drop]

CREATE TABLE 
	[abm_staging].[trip_vis_drop] (
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
		CONSTRAINT pk_tripvis_drop PRIMARY KEY ([scenario_id],[trip_vis_id]),
		CONSTRAINT ixuq_tripvis_drop UNIQUE ([scenario_id],[tour_vis_id],[trip_id]),
		CONSTRAINT fk_tripvis_scenario_drop FOREIGN KEY ([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
		CONSTRAINT fk_tripvis_orig_drop FOREIGN KEY ([orig_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripvis_dest_drop FOREIGN KEY ([dest_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_tripvis_period_drop FOREIGN KEY ([time_period_id]) REFERENCES [ref].[time_period]([time_period_id]),
		CONSTRAINT fk_tripvis_mode_drop FOREIGN KEY ([mode_id]) REFERENCES [ref].[mode]([mode_id]),
		CONSTRAINT fk_tripvis_purpose_drop FOREIGN KEY ([purpose_id]) REFERENCES [ref].[purpose]([purpose_id]),
		CONSTRAINT fk_tripvis_tour_drop FOREIGN KEY ([scenario_id],[tour_vis_id]) REFERENCES [abm].[tour_vis] ([scenario_id],[tour_vis_id]),
		CONSTRAINT fk_tripvis_boardtap_drop FOREIGN KEY ([scenario_id],[board_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id]),
		CONSTRAINT fk_tripvis_alighttap_drop FOREIGN KEY ([scenario_id],[alight_transit_tap_id]) REFERENCES [abm].[transit_tap] ([scenario_id],[transit_tap_id])
	) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'
CREATE NONCLUSTERED INDEX
	ix_tripvis_boardtap_drop
ON
	[abm_staging].[trip_vis_drop] ([scenario_id],[board_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripvis_alighttap_drop
ON
	[abm_staging].[trip_vis_drop] ([scenario_id],[alight_transit_tap_id])
WITH
	(DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL


-- Switch and drop partitions for non foreign keyed tables containing scenario data
ALTER TABLE [abm].[lu_mgra_input] SWITCH PARTITION @drop_partition TO [abm_staging].[lu_mgra_input_drop]
DROP TABLE [abm_staging].[lu_mgra_input_drop];
ALTER TABLE [abm].[hwy_flow_mode] SWITCH PARTITION @drop_partition TO [abm_staging].[hwy_flow_mode_drop]
DROP TABLE [abm_staging].[hwy_flow_mode_drop];
ALTER TABLE [abm].[lu_person_fp] SWITCH PARTITION @drop_partition TO [abm_staging].[lu_person_fp_drop]
DROP TABLE [abm_staging].[lu_person_fp_drop];
ALTER TABLE [abm].[lu_person_lc] SWITCH PARTITION @drop_partition TO [abm_staging].[lu_person_lc_drop]
DROP TABLE [abm_staging].[lu_person_lc_drop];
ALTER TABLE [abm].[tour_ij_person] SWITCH PARTITION @drop_partition TO [abm_staging].[tour_ij_person_drop]
DROP TABLE [abm_staging].[tour_ij_person_drop];
ALTER TABLE [abm].[bike_flow] SWITCH PARTITION @drop_partition TO [abm_staging].[bike_flow_drop]
DROP TABLE [abm_staging].[bike_flow_drop];
ALTER TABLE [abm].[transit_flow] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_flow_drop]
DROP TABLE [abm_staging].[transit_flow_drop];
ALTER TABLE [abm].[transit_onoff] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_onoff_drop]
DROP TABLE [abm_staging].[transit_onoff_drop];
ALTER TABLE [abm].[trip_cb] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_cb_drop]
DROP TABLE [abm_staging].[trip_cb_drop];
ALTER TABLE [abm].[trip_ij] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_ij_drop]
DROP TABLE [abm_staging].[trip_ij_drop];
ALTER TABLE [abm].[trip_vis] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_vis_drop]
DROP TABLE [abm_staging].[trip_vis_drop];
ALTER TABLE [abm].[cbd_vehicles] SWITCH PARTITION @drop_partition TO [abm_staging].[cbd_vehicles_drop]
DROP TABLE [abm_staging].[cbd_vehicles_drop];
ALTER TABLE [abm].[transit_aggflow] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_aggflow_drop]
DROP TABLE [abm_staging].[transit_aggflow_drop];
ALTER TABLE [abm].[transit_pnr] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_pnr_drop]
DROP TABLE [abm_staging].[transit_pnr_drop];
ALTER TABLE [abm].[transit_tap_skims] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_tap_skims_drop]
DROP TABLE [abm_staging].[transit_tap_skims_drop];
ALTER TABLE [abm].[transit_tap_walk] SWITCH PARTITION @drop_partition TO [abm_staging].[transit_tap_walk_drop]
DROP TABLE [abm_staging].[transit_tap_walk_drop];
ALTER TABLE [abm].[trip_ap] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_ap_drop]
DROP TABLE [abm_staging].[trip_ap_drop];
ALTER TABLE [abm].[trip_ie] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_ie_drop]
DROP TABLE [abm_staging].[trip_ie_drop];
ALTER TABLE [abm].[at_skims] SWITCH PARTITION @drop_partition TO [abm_staging].[at_skims_drop]
DROP TABLE [abm_staging].[at_skims_drop];
ALTER TABLE [abm].[hwy_skims] SWITCH PARTITION @drop_partition TO [abm_staging].[hwy_skims_drop]
DROP TABLE [abm_staging].[hwy_skims_drop];
ALTER TABLE [abm].[trip_agg] SWITCH PARTITION @drop_partition TO [abm_staging].[trip_agg_drop]
DROP TABLE [abm_staging].[trip_agg_drop];


-- Normal delete statements for foreign keyed tables, order matters due to foreign key constraints
DELETE FROM [abm].[hwy_flow]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[tour_cb]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[bike_link_ab]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[tour_ij]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[lu_person]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[tour_vis]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[hwy_link_ab_tod]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[lu_hh]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[bike_link]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[transit_stop]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[hwy_link_ab]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[transit_route]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[transit_tap]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[hwy_link_tod]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[transit_link]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[hwy_link]
WHERE [scenario_id] = @scenario_id


/* Delete scenario from scenario table */
DELETE FROM [ref].[scenario]
WHERE [scenario_id] = @scenario_id

GO


-- Add metadata for [abm].[sp_clear_scen]
EXECUTE [db_meta].[add_xp] 'abm.sp_clear_scen', 'SUBSYSTEM', 'abm'
EXECUTE [db_meta].[add_xp] 'abm.sp_clear_scen', 'MS_Description', 'stored procedure to wipe data from a scenario'
GO


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[abm].[sp_del_files]') AND type in (N'P', N'PC'))
DROP PROCEDURE [abm].[sp_del_files]
GO

CREATE PROCEDURE [abm].[sp_del_files] 
	@scenario_id smallint 
AS

-- =============================================
-- Author:		Gregor Schroeder
-- Create date: 08/05/2014
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
		AND [value] = @scenario_id
	)
BEGIN
	print 'WARNING: Partition function was merged. This could delete other scenarios data. Canceled execution.'
	RETURN
END


-- Returns database name
SET @db_name = CAST(DB_NAME() AS nvarchar(50))

-- Make sure scenario has been cleared from database
EXEC [abm].[sp_clear_scen] @scenario_id

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
GO


-- Add metadata for [abm].[sp_del_files]
EXECUTE [db_meta].[add_xp] 'abm.sp_del_files', 'SUBSYSTEM', 'abm'
EXECUTE [db_meta].[add_xp] 'abm.sp_del_files', 'MS_Description', 'stored procedure to wipe data, merge partition function, and delete files associated with a scenario'
GO





/* Create trip_micro_simul view */
IF  EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[abm].[vi_trip_micro_simul]'))
DROP VIEW [abm].[vi_trip_micro_simul]
GO

CREATE VIEW [abm].[vi_trip_micro_simul] AS
SELECT
	[scenario_id]
	,[trip_ap_id] AS [surrogate_trip_id]
	,NULL AS [surrogate_tour_id]
    ,[model_type_id]
    ,NULL AS [tour_id]
    ,[trip_id]
    ,[orig_geography_zone_id]
    ,[dest_geography_zone_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[board_transit_tap_id]
    ,[alight_transit_tap_id]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,[ap_income_cat_id]
    ,[nights]
    ,[ap_arrival_mode_id]
    ,NULL AS [parking_geography_zone_id]
FROM 
	[abm].[trip_ap]
UNION ALL
SELECT
	[trip_cb].[scenario_id]
	,[trip_cb_id] AS [surrogate_trip_id]
	,[trip_cb].[tour_cb_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[trip_cb].[orig_geography_zone_id]
    ,[trip_cb].[dest_geography_zone_id]
    ,[time_period_id]
    ,[mode_id]
    ,[trip_cb].[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[board_transit_tap_id]
    ,[alight_transit_tap_id]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_geography_zone_id]
FROM 
	[abm].[trip_cb]
INNER JOIN
	[abm].[tour_cb]
ON
	[trip_cb].[scenario_id] = [tour_cb].[scenario_id]
	AND [trip_cb].[tour_cb_id] = [tour_cb].[tour_cb_id]
UNION ALL
SELECT
	[scenario_id]
	,[trip_ie_id] AS [surrogate_trip_id]
	,NULL AS [surrogate_tour_id]
    ,[model_type_id]
    ,NULL AS [tour_id]
    ,[trip_id]
    ,[orig_geography_zone_id]
    ,[dest_geography_zone_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[board_transit_tap_id]
    ,[alight_transit_tap_id]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_geography_zone_id]
FROM 
	[abm].[trip_ie]
UNION ALL
SELECT
	[trip_ij].[scenario_id]
	,[trip_ij_id] AS [surrogate_trip_id]
	,[trip_ij].[tour_ij_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[trip_ij].[orig_geography_zone_id]
    ,[trip_ij].[dest_geography_zone_id]
    ,[time_period_id]
    ,[trip_ij].[mode_id]
    ,[trip_ij].[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[board_transit_tap_id]
    ,[alight_transit_tap_id]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,[parking_geography_zone_id]
FROM 
	[abm].[trip_ij]
INNER JOIN
	[abm].[tour_ij]
ON
	[trip_ij].[scenario_id] = [tour_ij].[scenario_id]
	AND [trip_ij].[tour_ij_id] = [tour_ij].[tour_ij_id]
UNION ALL
SELECT
	[trip_vis].[scenario_id]
	,[trip_vis_id] AS [surrogate_trip_id]
	,[trip_vis].[tour_vis_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[trip_vis].[orig_geography_zone_id]
    ,[trip_vis].[dest_geography_zone_id]
    ,[time_period_id]
    ,[trip_vis].[mode_id]
    ,[trip_vis].[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[board_transit_tap_id]
    ,[alight_transit_tap_id]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_geography_zone_id]
FROM 
	[abm].[trip_vis]
INNER JOIN
	[abm].[tour_vis]
ON
	[trip_vis].[scenario_id] = [tour_vis].[scenario_id]
	AND [trip_vis].[tour_vis_id] = [tour_vis].[tour_vis_id]
GO

-- Add metadata for [abm].[vi_trip_micro_simul]
EXECUTE [db_meta].[add_xp] 'abm.vi_trip_micro_simul', 'SUBSYSTEM', 'trips_tours'
EXECUTE [db_meta].[add_xp] 'abm.vi_trip_micro_simul', 'MS_Description', 'view that combines all the micro simulated trip tables'
GO




/* Create tour_micro_simul view */
IF  EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[abm].[vi_tour_micro_simul]'))
DROP VIEW [abm].[vi_tour_micro_simul]
GO

CREATE VIEW [abm].[vi_tour_micro_simul] AS
SELECT 
	[scenario_id]
	,[tour_cb_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,NULL AS [tour_cat_id]
    ,[purpose_id]
    ,[orig_geography_zone_id]
    ,[dest_geography_zone_id]
    ,[start_time_period_id]
    ,[end_time_period_id]
    ,NULL AS [mode_id]
    ,[crossing_mode_id]
    ,[sentri]
    ,[poe_id]
    ,NULL AS [auto_available]
    ,NULL AS [hh_income_cat_id]
FROM 
	[abm].[tour_cb]
UNION ALL
SELECT 
	[scenario_id]
	,[tour_ij_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,[tour_cat_id]
    ,[purpose_id]
    ,[orig_geography_zone_id]
    ,[dest_geography_zone_id]
    ,[start_time_period_id]
    ,[end_time_period_id]
    ,[mode_id]
    ,NULL AS [crossing_mode_id]
    ,NULL AS [sentri]
    ,NULL AS [poe_id]
    ,NULL AS [auto_available]
    ,NULL AS [hh_income_cat_id]
FROM 
	[abm].[tour_ij]
UNION ALL
SELECT 
	[scenario_id]
	,[tour_vis_id] AS [surrogate_tour_id]
    ,[model_type_id]
    ,[tour_id]
    ,[tour_cat_id]
    ,[purpose_id]
    ,[orig_geography_zone_id]
    ,[dest_geography_zone_id]
    ,[start_time_period_id]
    ,[end_time_period_id]
    ,[mode_id]
    ,NULL AS [crossing_mode_id]
    ,NULL AS [sentri]
    ,NULL AS [poe_id]
    ,[auto_available]
    ,[hh_income_cat_id]
FROM 
	[abm].[tour_vis]
GO

-- Add metadata for [abm].[vi_tour_micro_simul]
EXECUTE [db_meta].[add_xp] 'abm.vi_tour_micro_simul', 'SUBSYSTEM', 'trips_tours'
EXECUTE [db_meta].[add_xp] 'abm.vi_tour_micro_simul', 'MS_Description', 'view that combines all the micro simulated tour tables'
GO