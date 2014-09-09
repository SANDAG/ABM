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
EXECUTE [db_meta].[add_xp] 'abm.sp_index_frag', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.sp_index_frag', 'MS_Description', 'Return index fragmentation statistics on associated parition for a scenario'
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
IF(@num_gb > 30)
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
	ALTER INDEX [pk_atskims] ON [abm].[at_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_bikeflow] ON [abm].[bike_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_bikelink] ON [abm].[bike_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_bikelinkab] ON [abm].[bike_link_ab] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_cbdvehicles] ON [abm].[cbd_vehicles] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwyflow] ON [abm].[hwy_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwyflowmode] ON [abm].[hwy_flow_mode] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwylink] ON [abm].[hwy_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwylinkab] ON [abm].[hwy_link_ab] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwylinkabtod] ON [abm].[hwy_link_ab_tod] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwylinktod] ON [abm].[hwy_link_tod] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_hwyskims] ON [abm].[hwy_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_luhh] ON [abm].[lu_hh] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_lu_hh_zone] ON [abm].[lu_hh] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_lumgrainput] ON [abm].[lu_mgra_input] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_luperson] ON [abm].[lu_person] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_lupersonfp] ON [abm].[lu_person_fp] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_lupersonlc] ON [abm].[lu_person_lc] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_lupersonlc_zone] ON [abm].[lu_person_lc] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tourcb] ON [abm].[tour_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourcb_dest] ON [abm].[tour_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourcb_orig] ON [abm].[tour_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tourij] ON [abm].[tour_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourij_dest] ON [abm].[tour_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourij_orig] ON [abm].[tour_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tourijperson] ON [abm].[tour_ij_person] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tourvis] ON [abm].[tour_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourvis_dest] ON [abm].[tour_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tourvis_orig] ON [abm].[tour_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitaggflow] ON [abm].[transit_aggflow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitflow] ON [abm].[transit_flow] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitlink] ON [abm].[transit_link] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitonoff] ON [abm].[transit_onoff] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitpnr] ON [abm].[transit_pnr] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_transitpnr_tap] ON [abm].[transit_pnr] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitroute] ON [abm].[transit_route] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transitstop] ON [abm].[transit_stop] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transittap] ON [abm].[transit_tap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transittapskims] ON [abm].[transit_tap_skims] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_transittapwalk] ON [abm].[transit_tap_walk] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripagg] ON [abm].[trip_agg] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripap] ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripap_alighttap] ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripap_boardtap] ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripap_dest] ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripap_orig] ON [abm].[trip_ap] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripcb] ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripcb_alighttap] ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripcb_boardtap] ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripcb_dest] ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripcb_orig] ON [abm].[trip_cb] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripie] ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripie_alighttap] ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripie_boardtap] ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripie_dest] ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripie_orig] ON [abm].[trip_ie] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripij] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripij_alighttap] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripij_boardtap] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripij_dest] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripij_orig] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripij_parking] ON [abm].[trip_ij] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [pk_tripvis] ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripvis_alighttap] ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripvis_boardtap] ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripvis_dest] ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
	ALTER INDEX [ix_tripvis_orig] ON [abm].[trip_vis] REORGANIZE PARTITION = @dest_partition
GO


-- Add metadata for [abm].[sp_shrink_reorg]
EXECUTE [db_meta].[add_xp] 'abm.sp_shrink_reorg', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.sp_shrink_reorg', 'MS_Description', 'Shrink associated file and reorganize associated partition indices for a selected scenario'
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


-- Create staging tables on the same filegroup for non foreign keyed tables
SET @SQL =
N'IF OBJECT_ID(''abm_staging.at_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[at_skims_drop]

CREATE TABLE [abm_staging].[at_skims_drop] (
	[scenario_id] [smallint] NOT NULL,
	[geography_type_id] [tinyint] NOT NULL,
	[orig] [smallint] NOT NULL,
	[dest] [smallint] NOT NULL,
	[mode_id] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[skim_id] [tinyint] NOT NULL,
	[value] [decimal](8, 4) NULL,
	CONSTRAINT [pk_atskims_drop] PRIMARY KEY CLUSTERED ([scenario_id],[geography_type_id],[orig],[dest],[mode_id],[time_resolution_id],[time_period_id],[skim_id]),
	CONSTRAINT [fk_atskims_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_atskims_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_atskims_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_atskims_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_atskims_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_atskims_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_atskims_skim_drop] FOREIGN KEY([skim_id]) REFERENCES [ref].[skim] ([skim_id]),
	CONSTRAINT [fk_atskims_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.bike_flow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[bike_flow_drop]

CREATE TABLE [abm_staging].[bike_flow_drop] (
	[scenario_id] [smallint] NOT NULL,
	[roadsegid] [int] NOT NULL,
	[ab] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[flow] [decimal](8, 4) NULL,
	CONSTRAINT [pk_bikeflow_drop] PRIMARY KEY CLUSTERED ([scenario_id],[roadsegid],[ab],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_bikeflow_bikelink_drop] FOREIGN KEY([scenario_id], [roadsegid]) REFERENCES [abm].[bike_link] ([scenario_id], [roadsegid]),
	CONSTRAINT [fk_bikeflow_bikelinkab_drop] FOREIGN KEY([scenario_id], [roadsegid], [ab]) REFERENCES [abm].[bike_link_ab] ([scenario_id], [roadsegid], [ab]),
	CONSTRAINT [fk_bikeflow_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_bikeflow_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_bikeflow_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.cbd_vehicles_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[cbd_vehicles_drop]

CREATE TABLE [abm_staging].[cbd_vehicles_drop] (
	[scenario_id] [smallint] NOT NULL,
	[geography_type_id] [tinyint] NOT NULL,
	[zone] [smallint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[vehicles] [decimal](10, 6) NULL,
	CONSTRAINT [pk_cbdvehicles_drop] PRIMARY KEY CLUSTERED ([scenario_id],[geography_type_id],[zone],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_cbdvehicles_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_cbdvehicles_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [zone]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_cbdvehicles_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_cbdvehicles_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_cbdvehicles_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_cbdvehicles_zone_drop] FOREIGN KEY([geography_type_id], [zone]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.hwy_flow_mode_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[hwy_flow_mode_drop]

CREATE TABLE [abm_staging].[hwy_flow_mode_drop] (
	[scenario_id] [smallint] NOT NULL,
	[hwycov_id] [int] NOT NULL,
	[ab] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[mode_id] [tinyint] NOT NULL,
	[flow] [decimal](12, 6) NULL,
	CONSTRAINT [pk_hwyflowmode_drop] PRIMARY KEY CLUSTERED ([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id],[mode_id]),
	CONSTRAINT [fk_hwyflowmode_hwyflow_drop] FOREIGN KEY([scenario_id], [hwycov_id], [ab], [time_resolution_id], [time_period_id]) REFERENCES [abm].[hwy_flow] ([scenario_id], [hwycov_id], [ab], [time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_hwyflowmode_hwylink_drop] FOREIGN KEY([scenario_id], [hwycov_id]) REFERENCES [abm].[hwy_link] ([scenario_id], [hwycov_id]),
	CONSTRAINT [fk_hwyflowmode_hwylinkab_drop] FOREIGN KEY([scenario_id], [hwycov_id], [ab]) REFERENCES [abm].[hwy_link_ab] ([scenario_id], [hwycov_id], [ab]),
	CONSTRAINT [fk_hwyflowmode_hwylinkabtod_drop] FOREIGN KEY([scenario_id], [hwycov_id], [ab], [time_resolution_id], [time_period_id]) REFERENCES [abm].[hwy_link_ab_tod] ([scenario_id], [hwycov_id], [ab], [time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_hwyflowmode_hwylinktod_drop] FOREIGN KEY([scenario_id], [hwycov_id], [time_resolution_id], [time_period_id]) REFERENCES [abm].[hwy_link_tod] ([scenario_id], [hwycov_id], [time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_hwyflowmode_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_hwyflowmode_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_hwyflowmode_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_hwyflowmode_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.hwy_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[hwy_skims_drop]

CREATE TABLE [abm_staging].[hwy_skims_drop] (
	[scenario_id] [smallint] NOT NULL,
	[geography_type_id] [tinyint] NOT NULL,
	[orig] [smallint] NOT NULL,
	[dest] [smallint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[dist_drive_alone_toll] [decimal](12, 6) NULL,
	[time_drive_alone_toll] [decimal](12, 6) NULL,
	[cost_drive_alone_toll] [decimal](4, 2) NULL,
	[dist_drive_alone_free] [decimal](12, 6) NULL,
	[time_drive_alone_free] [decimal](12, 6) NULL,
	[dist_hov2_toll] [decimal](12, 6) NULL,
	[time_hov2_toll] [decimal](12, 6) NULL,
	[cost_hov2_toll] [decimal](4, 2) NULL,
	[dist_hov2_free] [decimal](12, 6) NULL,
	[time_hov2_free] [decimal](12, 6) NULL,
	[dist_hov3_toll] [decimal](12, 6) NULL,
	[time_hov3_toll] [decimal](12, 6) NULL,
	[cost_hov3_toll] [decimal](4, 2) NULL,
	[dist_hov3_free] [decimal](12, 6) NULL,
	[time_hov3_free] [decimal](12, 6) NULL,
	[dist_truck_toll] [decimal](12, 6) NULL,
	[time_truck_toll] [decimal](12, 6) NULL,
	[cost_truck_toll] [decimal](4, 2) NULL,
	[dist_truck_free] [decimal](12, 6) NULL,
	[time_truck_free] [decimal](12, 6) NULL,
	CONSTRAINT [pk_hwyskims_drop] PRIMARY KEY CLUSTERED ([scenario_id],[geography_type_id],[orig],[dest],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_hwyskims_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_hwyskims_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_hwyskims_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_hwyskims_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_hwyskims_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_hwyskims_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.lu_person_fp_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[lu_person_fp_drop]

CREATE TABLE [abm_staging].[lu_person_fp_drop] (
	[scenario_id] [smallint] NOT NULL,
	[hh_id] [int] NOT NULL,
	[pnum] [tinyint] NOT NULL,
	[fp_choice_id] [tinyint] NULL,
	[reimb_pct] [decimal](8, 6) NULL,
	CONSTRAINT [pk_lupersonfp_drop] PRIMARY KEY CLUSTERED ([scenario_id],[hh_id],[pnum]),
	CONSTRAINT [fk_lupersonfp_fpchoice_drop] FOREIGN KEY([fp_choice_id]) REFERENCES [ref].[fp_choice] ([fp_choice_id]),
	CONSTRAINT [fk_lupersonfp_luhh_drop] FOREIGN KEY([scenario_id], [hh_id]) REFERENCES [abm].[lu_hh] ([scenario_id], [hh_id]),
	CONSTRAINT [fk_lupersonfp_luperson_drop] FOREIGN KEY([scenario_id], [hh_id], [pnum]) REFERENCES [abm].[lu_person] ([scenario_id], [hh_id], [pnum]),
	CONSTRAINT [fk_lupersonfp_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.lu_person_lc_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[lu_person_lc_drop]

CREATE TABLE [abm_staging].[lu_person_lc_drop] (
	[scenario_id] [smallint] NOT NULL,
	[hh_id] [int] NOT NULL,
	[pnum] [tinyint] NOT NULL,
	[loc_choice_id] [tinyint] NOT NULL,
	[loc_choice_segment_id] [tinyint] NULL,
	[geography_type_id] [tinyint] NULL,
	[zone] [smallint] NULL,
	CONSTRAINT [pk_lupersonlc_drop] PRIMARY KEY CLUSTERED ([scenario_id],[hh_id],[pnum],[loc_choice_id]),
	CONSTRAINT [fk_lupersonlc_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_lupersonlc_lc_drop] FOREIGN KEY([loc_choice_id]) REFERENCES [ref].[loc_choice] ([loc_choice_id]),
	CONSTRAINT [fk_lupersonlc_luhh_drop] FOREIGN KEY([scenario_id], [hh_id]) REFERENCES [abm].[lu_hh] ([scenario_id], [hh_id]),
	CONSTRAINT [fk_lupersonlc_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [zone]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_lupersonlc_luperson_drop] FOREIGN KEY([scenario_id], [hh_id], [pnum]) REFERENCES [abm].[lu_person] ([scenario_id], [hh_id], [pnum]),
	CONSTRAINT [fk_lupersonlc_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_lupersonlc_segment_drop] FOREIGN KEY([loc_choice_id], [loc_choice_segment_id]) REFERENCES [ref].[loc_choice_segment] ([loc_choice_id], [loc_choice_segment_id]),
	CONSTRAINT [fk_lupersonlc_zone_drop] FOREIGN KEY([geography_type_id], [zone]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.tour_ij_person_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[tour_ij_person_drop]

CREATE TABLE [abm_staging].[tour_ij_person_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[tour_id] [int] NOT NULL,
	[hh_id] [int] NOT NULL,
	[pnum] [tinyint] NOT NULL,
	CONSTRAINT [pk_tourijperson_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[tour_id],[hh_id],[pnum]),
	CONSTRAINT [fk_tourijperson_luhh_drop] FOREIGN KEY([scenario_id], [hh_id]) REFERENCES [abm].[lu_hh] ([scenario_id], [hh_id]),
	CONSTRAINT [fk_tourijperson_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tourijperson_person_drop] FOREIGN KEY([scenario_id], [hh_id], [pnum]) REFERENCES [abm].[lu_person] ([scenario_id], [hh_id], [pnum]),
	CONSTRAINT [fk_tourijperson_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tourijperson_tourij_drop] FOREIGN KEY([scenario_id], [model_type_id], [tour_id]) REFERENCES [abm].[tour_ij] ([scenario_id], [model_type_id], [tour_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_aggflow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_aggflow_drop]

CREATE TABLE [abm_staging].[transit_aggflow_drop] (
	[scenario_id] [smallint] NOT NULL,
	[trcov_id] [int] NOT NULL,
	[transit_mode_id] [tinyint] NOT NULL,
	[transit_access_mode_id] [tinyint] NOT NULL,
	[ab] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[transit_flow] [smallint] NULL,
	[non_transit_flow] [smallint] NULL,
	[total_flow] [smallint] NULL,
	[access_walk_flow] [smallint] NULL,
	[xfer_walk_flow] [smallint] NULL,
	[egress_walk_flow] [smallint] NULL,
	CONSTRAINT [pk_transitaggflow_drop] PRIMARY KEY CLUSTERED ([scenario_id],[trcov_id],[ab],[time_resolution_id],[time_period_id],[transit_mode_id],[transit_access_mode_id]),
	CONSTRAINT [fk_transitaggflow_accessmode_drop] FOREIGN KEY([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode] ([transit_access_mode_id]),
	CONSTRAINT [fk_transitaggflow_link_drop] FOREIGN KEY([scenario_id], [trcov_id]) REFERENCES [abm].[transit_link] ([scenario_id], [trcov_id]),
	CONSTRAINT [fk_transitaggflow_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_transitaggflow_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transitaggflow_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_transitaggflow_transitmode_drop] FOREIGN KEY([transit_mode_id]) REFERENCES [ref].[transit_mode] ([transit_mode_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_flow_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_flow_drop]

CREATE TABLE [abm_staging].[transit_flow_drop] (
	[scenario_id] [smallint] NOT NULL,
	[route_id] [smallint] NOT NULL,
	[from_stop_id] [smallint] NOT NULL,
	[to_stop_id] [smallint] NOT NULL,
	[transit_mode_id] [tinyint] NOT NULL,
	[transit_access_mode_id] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[from_mp] [decimal](9, 6) NULL,
	[to_mp] [decimal](9, 6) NULL,
	[baseivtt] [decimal](9, 6) NULL,
	[cost] [decimal](9, 6) NULL,
	[transit_flow] [smallint] NULL,
	CONSTRAINT [pk_transitflow_drop] PRIMARY KEY CLUSTERED ([scenario_id],[route_id],[from_stop_id],[to_stop_id],[transit_mode_id],[transit_access_mode_id],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_transitflow_accessmode_drop] FOREIGN KEY([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode] ([transit_access_mode_id]),
	CONSTRAINT [fk_transitflow_fromstop_drop] FOREIGN KEY([scenario_id], [from_stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id], [stop_id]),
	CONSTRAINT [fk_transitflow_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_transitflow_route_drop] FOREIGN KEY([scenario_id], [route_id]) REFERENCES [abm].[transit_route] ([scenario_id], [route_id]),
	CONSTRAINT [fk_transitflow_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transitflow_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_transitflow_tostop_drop] FOREIGN KEY([scenario_id], [to_stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id], [stop_id]),
	CONSTRAINT [fk_transitflow_transitmode_drop] FOREIGN KEY([transit_mode_id]) REFERENCES [ref].[transit_mode] ([transit_mode_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_onoff_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_onoff_drop]

CREATE TABLE [abm_staging].[transit_onoff_drop] (
	[scenario_id] [smallint] NOT NULL,
	[route_id] [smallint] NOT NULL,
	[stop_id] [smallint] NOT NULL,
	[transit_mode_id] [tinyint] NOT NULL,
	[transit_access_mode_id] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[boardings] [decimal](11, 6) NULL,
	[alightings] [decimal](11, 6) NULL,
	[walk_access_on] [decimal](11, 6) NULL,
	[direct_transfer_on] [decimal](11, 6) NULL,
	[walk_transfer_on] [decimal](11, 6) NULL,
	[direct_transfer_off] [decimal](11, 6) NULL,
	[walk_transfer_off] [decimal](11, 6) NULL,
	[egress_off] [decimal](11, 6) NULL,
	CONSTRAINT [pk_transitonoff_drop] PRIMARY KEY CLUSTERED ([scenario_id],[route_id],[stop_id],[transit_mode_id],[transit_access_mode_id],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_transitonoff_accessmode_drop] FOREIGN KEY([transit_access_mode_id]) REFERENCES [ref].[transit_access_mode] ([transit_access_mode_id]),
	CONSTRAINT [fk_transitonoff_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_transitonoff_route_drop] FOREIGN KEY([scenario_id], [route_id]) REFERENCES [abm].[transit_route] ([scenario_id], [route_id]),
	CONSTRAINT [fk_transitonoff_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transitonoff_stop_drop] FOREIGN KEY([scenario_id], [stop_id]) REFERENCES [abm].[transit_stop] ([scenario_id], [stop_id]),
	CONSTRAINT [fk_transitonoff_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_transitonoff_transitmode_drop] FOREIGN KEY([transit_mode_id]) REFERENCES [ref].[transit_mode] ([transit_mode_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_pnr_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_pnr_drop]

CREATE TABLE [abm_staging].[transit_pnr_drop] (
	[scenario_id] [smallint] NOT NULL,
	[tap] [smallint] NOT NULL,
	[lot_id] [smallint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[parking_type_id] [tinyint] NULL,
	[capacity] [smallint] NULL,
	[distance] [smallint] NULL,
	[vehicles] [smallint] NULL,
	CONSTRAINT [pk_transitpnr_drop] PRIMARY KEY CLUSTERED ([scenario_id],[tap],[lot_id],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_transitpnr_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_transitpnr_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transitpnr_tap_drop] FOREIGN KEY([scenario_id], [tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_transitpnr_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_tap_skims_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_tap_skims_drop]

CREATE TABLE [abm_staging].[transit_tap_skims_drop] (
	[scenario_id] [smallint] NOT NULL,
	[orig_tap] [smallint] NOT NULL,
	[dest_tap] [smallint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[init_wait_premium] [decimal](6, 3) NULL,
	[ivt_premium] [decimal](6, 3) NULL,
	[walk_time_premium] [decimal](6, 3) NULL,
	[transfer_time_premium] [decimal](6, 3) NULL,
	[fare_premium] [decimal](4, 2) NULL,
	CONSTRAINT [pk_transittapskims_drop] PRIMARY KEY CLUSTERED ([scenario_id],[orig_tap],[dest_tap],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_transittapskims_desttap_drop] FOREIGN KEY([scenario_id], [dest_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_transittapskims_origtap_drop] FOREIGN KEY([scenario_id], [orig_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_transittapskims_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_transittapskims_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transittapskims_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.transit_tap_walk_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[transit_tap_walk_drop]

CREATE TABLE [abm_staging].[transit_tap_walk_drop] (
	[scenario_id] [smallint] NOT NULL,
	[geography_type_id] [tinyint] NOT NULL,
	[zone] [smallint] NOT NULL,
	[tap] [smallint] NOT NULL,
	[time_boarding_perceived] [decimal](6, 3) NULL,
	[time_boarding_actual] [decimal](6, 3) NULL,
	[time_alighting_perceived] [decimal](6, 3) NULL,
	[time_alighting_actual] [decimal](6, 3) NULL,
	[gain_boarding] [decimal](8, 4) NULL,
	[gain_alighting] [decimal](8, 4) NULL,
	CONSTRAINT [pk_transitwalktap_drop] PRIMARY KEY CLUSTERED ([scenario_id],[geography_type_id],[zone],[tap]),
	CONSTRAINT [fk_transittapwalk_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_transittapwalk_tap_drop] FOREIGN KEY([scenario_id], [tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_transittapwalk_zone_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [zone]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_transitwalktap_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_transitwalktap_zone_drop] FOREIGN KEY([geography_type_id], [zone]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_agg_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_agg_drop]

CREATE TABLE [abm_staging].[trip_agg_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[geography_type_id] [tinyint] NOT NULL,
	[orig] [smallint] NOT NULL,
	[dest] [smallint] NOT NULL,
	[purpose_id] [tinyint] NOT NULL,
	[mode_id] [tinyint] NOT NULL,
	[time_resolution_id] [tinyint] NOT NULL,
	[time_period_id] [tinyint] NOT NULL,
	[trips] [decimal](20, 16) NULL,
	CONSTRAINT [pk_tripagg_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[geography_type_id],[orig],[dest],[purpose_id],[mode_id],[time_resolution_id],[time_period_id]),
	CONSTRAINT [fk_tripagg_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripagg_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripagg_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripagg_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripagg_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripagg_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripagg_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripagg_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripagg_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ap_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ap_drop]

CREATE TABLE [abm_staging].[trip_ap_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[trip_id] [int] NOT NULL,
	[geography_type_id] [tinyint] NULL,
	[orig] [smallint] NULL,
	[dest] [smallint] NULL,
	[time_resolution_id] [tinyint] NULL,
	[time_period_id] [tinyint] NULL,
	[mode_id] [tinyint] NULL,
	[purpose_id] [tinyint] NULL,
	[inbound] [bit] NULL,
	[party_size] [tinyint] NULL,
	[trip_board_tap] [smallint] NULL,
	[trip_alight_tap] [smallint] NULL,
	[trip_time] [decimal](11, 6) NULL,
	[out_vehicle_time] [decimal](11, 6) NULL,
	[trip_distance] [decimal](14, 10) NULL,
	[trip_cost] [decimal](4, 2) NULL,
	[ap_income_cat_id] [tinyint] NULL,
	[nights] [tinyint] NULL,
	[ap_arrival_mode_id] [tinyint] NULL,
	CONSTRAINT [pk_tripap_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[trip_id]),
	CONSTRAINT [fk_tripap_alighttap_drop] FOREIGN KEY([scenario_id], [trip_alight_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripap_arrivalmode_drop] FOREIGN KEY([ap_arrival_mode_id]) REFERENCES [ref].[ap_arrival_mode] ([ap_arrival_mode_id]),
	CONSTRAINT [fk_tripap_boardtap_drop] FOREIGN KEY([scenario_id], [trip_board_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripap_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripap_dest_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [dest]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripap_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripap_income_drop] FOREIGN KEY([ap_income_cat_id]) REFERENCES [ref].[ap_income_cat] ([ap_income_cat_id]),
	CONSTRAINT [fk_tripap_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripap_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripap_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripap_orig_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [orig]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripap_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripap_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripap_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripap_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_cb_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_cb_drop]

CREATE TABLE [abm_staging].[trip_cb_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[tour_id] [int] NOT NULL,
	[trip_id] [int] NOT NULL,
	[geography_type_id] [tinyint] NULL,
	[orig] [smallint] NULL,
	[dest] [smallint] NULL,
	[time_resolution_id] [tinyint] NULL,
	[time_period_id] [tinyint] NULL,
	[mode_id] [tinyint] NULL,
	[purpose_id] [tinyint] NULL,
	[inbound] [bit] NULL,
	[party_size] [tinyint] NULL,
	[trip_board_tap] [smallint] NULL,
	[trip_alight_tap] [smallint] NULL,
	[trip_time] [decimal](11, 6) NULL,
	[out_vehicle_time] [decimal](11, 6) NULL,
	[trip_distance] [decimal](14, 10) NULL,
	[trip_cost] [decimal](4, 2) NULL,
	CONSTRAINT [pk_tripcb_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[tour_id],[trip_id]),
	CONSTRAINT [fk_tripcb_alighttap_drop] FOREIGN KEY([scenario_id], [trip_alight_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripcb_boardtap_drop] FOREIGN KEY([scenario_id], [trip_board_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripcb_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripcb_dest_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [dest]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripcb_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripcb_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripcb_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripcb_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripcb_orig_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [orig]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripcb_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripcb_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripcb_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripcb_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_tripcb_tour_drop] FOREIGN KEY([scenario_id], [model_type_id], [tour_id]) REFERENCES [abm].[tour_cb] ([scenario_id], [model_type_id], [tour_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ie_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ie_drop]

CREATE TABLE [abm_staging].[trip_ie_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[trip_id] [int] NOT NULL,
	[geography_type_id] [tinyint] NULL,
	[orig] [smallint] NULL,
	[dest] [smallint] NULL,
	[time_resolution_id] [tinyint] NULL,
	[time_period_id] [tinyint] NULL,
	[mode_id] [tinyint] NULL,
	[purpose_id] [tinyint] NULL,
	[inbound] [bit] NULL,
	[party_size] [tinyint] NULL,
	[trip_board_tap] [smallint] NULL,
	[trip_alight_tap] [smallint] NULL,
	[trip_time] [decimal](11, 6) NULL,
	[out_vehicle_time] [decimal](11, 6) NULL,
	[trip_distance] [decimal](14, 10) NULL,
	[trip_cost] [decimal](4, 2) NULL,
	CONSTRAINT [pk_tripie_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[trip_id]),
	CONSTRAINT [fk_tripie_alighttap_drop] FOREIGN KEY([scenario_id], [trip_alight_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripie_boardtap_drop] FOREIGN KEY([scenario_id], [trip_board_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripie_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripie_dest_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [dest]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripie_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripie_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripie_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripie_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripie_orig_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [orig]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripie_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripie_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripie_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripie_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_ij_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_ij_drop]

CREATE TABLE [abm_staging].[trip_ij_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[tour_id] [int] NOT NULL,
	[trip_id] [int] NOT NULL,
	[geography_type_id] [tinyint] NULL,
	[orig] [smallint] NULL,
	[dest] [smallint] NULL,
	[time_resolution_id] [tinyint] NULL,
	[time_period_id] [tinyint] NULL,
	[mode_id] [tinyint] NULL,
	[purpose_id] [tinyint] NULL,
	[inbound] [bit] NULL,
	[party_size] [tinyint] NULL,
	[trip_board_tap] [smallint] NULL,
	[trip_alight_tap] [smallint] NULL,
	[trip_time] [decimal](11, 6) NULL,
	[out_vehicle_time] [decimal](11, 6) NULL,
	[trip_distance] [decimal](14, 10) NULL,
	[trip_cost] [decimal](4, 2) NULL,
	[parking_zone] [smallint] NULL,
	CONSTRAINT [pk_tripij_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[tour_id],[trip_id]),
	CONSTRAINT [fk_tripij_alighttap_drop] FOREIGN KEY([scenario_id], [trip_alight_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripij_boardtap_drop] FOREIGN KEY([scenario_id], [trip_board_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripij_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_dest_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [dest]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripij_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripij_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripij_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_orig_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [orig]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_parking_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [parking_zone]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_parking_zone_drop] FOREIGN KEY([geography_type_id], [parking_zone]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripij_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripij_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripij_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripij_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_tripij_tour_drop] FOREIGN KEY([scenario_id], [model_type_id], [tour_id]) REFERENCES [abm].[tour_ij] ([scenario_id], [model_type_id], [tour_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL

SET @SQL =
N'IF OBJECT_ID(''abm_staging.trip_vis_drop'',''U'') IS NOT NULL
DROP TABLE [abm_staging].[trip_vis_drop]

CREATE TABLE [abm_staging].[trip_vis_drop] (
	[scenario_id] [smallint] NOT NULL,
	[model_type_id] [tinyint] NOT NULL,
	[tour_id] [int] NOT NULL,
	[trip_id] [int] NOT NULL,
	[geography_type_id] [tinyint] NULL,
	[orig] [smallint] NULL,
	[dest] [smallint] NULL,
	[time_resolution_id] [tinyint] NULL,
	[time_period_id] [tinyint] NULL,
	[mode_id] [tinyint] NULL,
	[purpose_id] [tinyint] NULL,
	[inbound] [bit] NULL,
	[party_size] [tinyint] NULL,
	[trip_board_tap] [smallint] NULL,
	[trip_alight_tap] [smallint] NULL,
	[trip_time] [decimal](11, 6) NULL,
	[out_vehicle_time] [decimal](11, 6) NULL,
	[trip_distance] [decimal](14, 10) NULL,
	[trip_cost] [decimal](4, 2) NULL,
	CONSTRAINT [pk_tripvis_drop] PRIMARY KEY CLUSTERED ([scenario_id],[model_type_id],[tour_id],[trip_id]),
	CONSTRAINT [fk_tripvis_alighttap_drop] FOREIGN KEY([scenario_id], [trip_alight_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripvis_boardtap_drop] FOREIGN KEY([scenario_id], [trip_board_tap]) REFERENCES [abm].[transit_tap] ([scenario_id], [tap]),
	CONSTRAINT [fk_tripvis_dest_drop] FOREIGN KEY([geography_type_id], [dest]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripvis_dest_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [dest]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripvis_geotype_drop] FOREIGN KEY([geography_type_id]) REFERENCES [ref].[geography_type] ([geography_type_id]),
	CONSTRAINT [fk_tripvis_mode_drop] FOREIGN KEY([mode_id]) REFERENCES [ref].[mode] ([mode_id]),
	CONSTRAINT [fk_tripvis_model_drop] FOREIGN KEY([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id]),
	CONSTRAINT [fk_tripvis_orig_drop] FOREIGN KEY([geography_type_id], [orig]) REFERENCES [ref].[geography_zone] ([geography_type_id], [zone]),
	CONSTRAINT [fk_tripvis_orig_lumgra_drop] FOREIGN KEY([scenario_id], [geography_type_id], [orig]) REFERENCES [abm].[lu_mgra_input] ([scenario_id], [geography_type_id], [zone]),
	CONSTRAINT [fk_tripvis_period_drop] FOREIGN KEY([time_resolution_id], [time_period_id]) REFERENCES [ref].[time_period] ([time_resolution_id], [time_period_id]),
	CONSTRAINT [fk_tripvis_purpose_drop] FOREIGN KEY([model_type_id], [purpose_id]) REFERENCES [ref].[purpose] ([model_type_id], [purpose_id]),
	CONSTRAINT [fk_tripvis_scenario_drop] FOREIGN KEY([scenario_id]) REFERENCES [ref].[scenario] ([scenario_id]),
	CONSTRAINT [fk_tripvis_timeres_drop] FOREIGN KEY([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id]),
	CONSTRAINT [fk_tripvis_tour_drop] FOREIGN KEY([scenario_id], [model_type_id], [tour_id]) REFERENCES [abm].[tour_vis] ([scenario_id], [model_type_id], [tour_id])
) ON ' + @filegroupname + N'
WITH (DATA_COMPRESSION = PAGE);'
EXECUTE sp_executesql @SQL


-- Switch and drop partitions for non foreign keyed tables containing scenario data
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
DELETE FROM [abm].[lu_mgra_input]
WHERE [scenario_id] = @scenario_id
DELETE FROM [abm].[hwy_link]
WHERE [scenario_id] = @scenario_id


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

IF OBJECT_ID('abm_staging.transit_stop_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_stop_' + CAST(@scenario_id AS nvarchar(5))
	EXEC sp_executesql @SQL
END

IF OBJECT_ID('abm_staging.transit_tap_' + CAST(@scenario_id AS nvarchar(5)),'U') IS NOT NULL
BEGIN
	SET @SQL =
		N'DROP TABLE abm_staging.transit_tap_' + CAST(@scenario_id AS nvarchar(5))
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


/* Delete scenario from scenario table */
DELETE FROM [ref].[scenario]
WHERE [scenario_id] = @scenario_id

GO


-- Add metadata for [abm].[sp_clear_scen]
EXECUTE [db_meta].[add_xp] 'abm.sp_clear_scen', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.sp_clear_scen', 'MS_Description', 'Stored procedure to wipe data from a scenario'
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
REMOVE FILE scenario_file_' + CAST(@scenario_id AS nvarchar(5)) + N';
ALTER DATABASE ' + @db_name + '
REMOVE FILEGROUP scenario_fg_' + CAST(@scenario_id AS nvarchar(5)) + N';'
EXEC sp_executesql @SQL
GO


-- Add metadata for [abm].[sp_del_files]
EXECUTE [db_meta].[add_xp] 'abm.sp_del_files', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.sp_del_files', 'MS_Description', 'Stored procedure to wipe data, merge partition function, and delete files associated with a scenario'
GO





/* Create trip_micro_simul view */
IF  EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[abm].[trip_micro_simul]'))
DROP VIEW [abm].[trip_micro_simul]
GO

CREATE VIEW [abm].[trip_micro_simul] AS
SELECT
	[scenario_id]
    ,[model_type_id]
    ,NULL AS [tour_id]
    ,[trip_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[trip_board_tap]
    ,[trip_alight_tap]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,[ap_income_cat_id]
    ,[nights]
    ,[ap_arrival_mode_id]
    ,NULL AS [parking_zone]
FROM 
	[abm].[trip_ap]
UNION ALL
SELECT
	[scenario_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[trip_board_tap]
    ,[trip_alight_tap]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_zone]
FROM 
	[abm].[trip_cb]
UNION ALL
SELECT
	[scenario_id]
    ,[model_type_id]
    ,NULL AS [tour_id]
    ,[trip_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[trip_board_tap]
    ,[trip_alight_tap]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_zone]
FROM 
	[abm].[trip_ie]
UNION ALL
SELECT
	[scenario_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[trip_board_tap]
    ,[trip_alight_tap]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,[parking_zone]
FROM 
	[abm].[trip_ij]
UNION ALL
SELECT
	[scenario_id]
    ,[model_type_id]
    ,[tour_id]
    ,[trip_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[time_period_id]
    ,[mode_id]
    ,[purpose_id]
    ,[inbound]
    ,[party_size]
    ,[trip_board_tap]
    ,[trip_alight_tap]
    ,[trip_time]
    ,[out_vehicle_time]
    ,[trip_distance]
    ,[trip_cost]
    ,NULL AS [ap_income_cat_id]
    ,NULL AS [nights]
    ,NULL AS [ap_arrival_mode_id]
    ,NULL AS [parking_zone]
FROM 
	[abm].[trip_vis]
GO

-- Add metadata for [abm].[trip_micro_simul]
EXECUTE [db_meta].[add_xp] 'abm.trip_micro_simul', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.trip_micro_simul', 'MS_Description', 'View that combines all the micro simualted trip tables.'
GO




/* Create tour_micro_simul view */
IF  EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[abm].[tour_micro_simul]'))
DROP VIEW [abm].[tour_micro_simul]
GO

CREATE VIEW [abm].[tour_micro_simul] AS
SELECT 
	[scenario_id]
    ,[model_type_id]
    ,[tour_id]
    ,NULL AS [tour_cat_id]
    ,[purpose_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[start_period_id]
    ,[end_period_id]
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
    ,[model_type_id]
    ,[tour_id]
    ,[tour_cat_id]
    ,[purpose_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[start_period_id]
    ,[end_period_id]
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
    ,[model_type_id]
    ,[tour_id]
    ,[tour_cat_id]
    ,[purpose_id]
    ,[geography_type_id]
    ,[orig]
    ,[dest]
    ,[time_resolution_id]
    ,[start_period_id]
    ,[end_period_id]
    ,[mode_id]
    ,NULL AS [crossing_mode_id]
    ,NULL AS [sentri]
    ,NULL AS [poe_id]
    ,[auto_available]
    ,[hh_income_cat_id]
FROM 
	[abm].[tour_vis]
GO

-- Add metadata for [abm].[tour_micro_simul]
EXECUTE [db_meta].[add_xp] 'abm.tour_micro_simul', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'abm.tour_micro_simul', 'MS_Description', 'View that combines all the micro simualted tour tables.'
GO




/* Create MGRA-TAZ xref table */
IF OBJECT_ID('ref.mgra13_xref_taz13','U') IS NOT NULL
DROP TABLE [ref].[mgra13_xref_taz13]
GO

CREATE TABLE [ref].[mgra13_xref_taz13](
	[mgra13] smallint,
	[taz13] smallint,
	CONSTRAINT pk_mgra13xreftaz13 PRIMARY KEY ([mgra13])
	) 
ON 
	[ref_fg]
GO

with mgra_layer AS (
SELECT
	[zone] AS mgra
	,[centroid] AS mgra_centroid
FROM
	[ref].[geography_zone]
WHERE
	[geography_type_id] = 90
),
taz_layer AS (
SELECT
	[zone] as taz
	,[shape] AS taz_shape
FROM
	[ref].[geography_zone]
WHERE
	[geography_type_id] = 34
)
INSERT INTO [ref].[mgra13_xref_taz13]
SELECT
	mgra
	,taz
FROM
	mgra_layer
	,taz_layer
WHERE
	mgra_centroid.STWithin(taz_shape) = 1
GO

-- Add metadata for [ref].[mgra13_xref_taz13]
EXECUTE [db_meta].[add_xp] 'ref.mgra13_xref_taz13', 'SUBSYSTEM', 'REFERENCE'
EXECUTE [db_meta].[add_xp] 'ref.mgra13_xref_taz13', 'MS_Description', 'Cross reference table of the TAZ13 that contains the given MGRA13s centroid'
GO