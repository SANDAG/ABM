SET NOCOUNT ON;


-- create dimension schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='dimension')
BEGIN
    EXECUTE ('CREATE SCHEMA [dimension]')
    EXECUTE [db_meta].[add_xp] 'dimension', 'MS_Description', 'schema to hold and manage ABM dimension tables and views'
END


-- create scenario dimension
CREATE TABLE [dimension].[scenario] (
    [scenario_id] int NOT NULL,
    [name] nvarchar(50) NOT NULL,
    [year] int NOT NULL,
    [iteration] int NOT NULL,
    [sample_rate] float NOT NULL,
    [abm_version] nvarchar(50) NOT NULL,
    [path] nvarchar(200) NOT NULL,
    [user_name] nvarchar(100) NOT NULL,
    [load_status] nvarchar(10) NOT NULL,    
    [load_status_date] smalldatetime NULL,
	[geography_set_id] int NOT NULL DEFAULT 1
    CONSTRAINT pk_scenario PRIMARY KEY ([scenario_id]))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
GO

EXECUTE [db_meta].[add_xp] 'dimension.scenario', 'MS_Description', 'dimension table of loaded scenarios in the database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.scenario_id', 'MS_Description', 'scenario surrogate key and scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.name', 'MS_Description', 'base file path name of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.year', 'MS_Description', 'scenario year'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.iteration', 'MS_Description', 'scenario iteration loaded into database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.sample_rate', 'MS_Description', 'sample rate of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.abm_version', 'MS_Description', 'ABM model software version of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.path', 'MS_Description', 'full UNC file path of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.user_name', 'MS_Description', 'user who requested scenario be loaded into the database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.load_status', 'MS_Description', 'loading process status of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.load_status_date', 'MS_Description', 'date and time of load status'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.geography_set_id', 'MS_Description', 'geography dimension set identifier defaults to 1'

GO

CREATE TABLE [dbo].[scenario_delete](
	[scenario_id] [smallint] NOT NULL,
	CONSTRAINT [pk_scenario_delete] PRIMARY KEY CLUSTERED ([scenario_id] ASC)) 
	ON reference_fg
GO

EXECUTE [db_meta].[add_xp] 'dbo.scenario_delete', 'MS_Description', 'scenario id to delete from database'

-- create av used dimension
CREATE TABLE [dimension].[av_used] (
    [av_used_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
    [av_used_description] nvarchar(20) NOT NULL,
    CONSTRAINT pk_avUsed PRIMARY KEY ([av_used_id]),
    CONSTRAINT ixuq_avUsed UNIQUE ([av_used_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[av_used] VALUES
    ('Not Applicable'), -- insert NULL record as 0
    ('AV Used'),
    ('AV Not Used')
GO

EXECUTE [db_meta].[add_xp] 'dimension.av_used', 'MS_Description', 'dimension table for autonomous vehicle usage'
EXECUTE [db_meta].[add_xp] 'dimension.av_used.av_used_id', 'MS_Description', 'av_used surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.av_used.av_used_description', 'MS_Description', 'av_used description'
GO


-- create geography set dimension
CREATE TABLE [dimension].[geography_set] (
    [geography_set_id] int IDENTITY(0,1) NOT NULL, 
    [geography_set_description] nvarchar(400) NOT NULL,
    CONSTRAINT pk_geoSet PRIMARY KEY ([geography_set_id]),
    CONSTRAINT ixuq_geoSet UNIQUE ([geography_set_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[geography_set] VALUES
    ('Not Applicable'), -- insert NULL record as 0
    ('default geography for the San Diego Region using mgra_13 and taz_13')
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_set', 'MS_Description', 'dimension table for set of SANDAG geography'
EXECUTE [db_meta].[add_xp] 'dimension.geography_set.geography_set_id', 'MS_Description', 'geography set surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.geography_set.geography_set_description', 'MS_Description', 'geography_set description'
GO


-- create geography dimension
-- use an input file and an update query to fill at a later step
CREATE TABLE [dimension].[geography] (
    [geography_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
	[geography_set_id] int NOT NULL,
    [mgra_13] nvarchar(20) NOT NULL,
    [mgra_13_shape] geometry NULL,  -- model geographies are given geometries
    [taz_13] nvarchar(20) NOT NULL,
    [taz_13_shape] geometry NULL,  -- model geographies are given geometries
    [luz_13] nvarchar(20) NOT NULL,
    [cicpa_2016] nvarchar(20) NOT NULL,
    [cicpa_2016_name] nvarchar(100) NOT NULL,
    [cocpa_2016] nvarchar(20) NOT NULL,
    [cocpa_2016_name] nvarchar(50) NOT NULL,
    [jurisdiction_2016] nvarchar(20) NOT NULL,
    [jurisdiction_2016_name] nvarchar(50) NOT NULL,
    [tract_2010] nvarchar(20) NOT NULL,
    [region_2004] nvarchar(20) NOT NULL,
    [region_2004_name] nvarchar(50) NOT NULL,
    [external_zone] nvarchar(20) NOT NULL,
    CONSTRAINT pk_geography PRIMARY KEY([geography_id]),
	CONSTRAINT fk_geography_set FOREIGN KEY ([geography_set_id]) REFERENCES [dimension].[geography_set] ([geography_set_id]),
    CONSTRAINT ixuq_geography UNIQUE([mgra_13], [taz_13], [geography_set_id]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_mgra13 NONCLUSTERED ([geography_id], [mgra_13]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_taz13 NONCLUSTERED ([geography_id], [taz_13]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_luz13 NONCLUSTERED ([geography_id], [luz_13]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_cicpa2016 NONCLUSTERED ([geography_id], [cicpa_2016], [cicpa_2016_name]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_cocpa2016 NONCLUSTERED ([geography_id], [cocpa_2016], [cocpa_2016_name]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_jurisdiction2016 NONCLUSTERED ([geography_id], [jurisdiction_2016], [jurisdiction_2016_name]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_tract2010 NONCLUSTERED ([geography_id], [tract_2010]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_region2004 NONCLUSTERED ([geography_id], [region_2004], [region_2004_name]) WITH (DATA_COMPRESSION = PAGE),
    INDEX ix_geography_externalzone NONCLUSTERED ([geography_id], [external_zone]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography', 'MS_Description', 'geography dimension for ABM model including cross references'
EXECUTE [db_meta].[add_xp] 'dimension.geography.geography_id', 'MS_Description', 'geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.geography.geography_set_id', 'MS_Description', 'id for a set of geographies - standard geographies are set id 1'
EXECUTE [db_meta].[add_xp] 'dimension.geography.mgra_13', 'MS_Description', 'series 13 MGRA geography zones - base geography unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.geography.mgra_13_shape', 'MS_Description', 'series 13 MGRA geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.taz_13', 'MS_Description', 'series 13 TAZ geography zones - base and secondary geography unit of ABM model - MGRA geograhy nests within excluding external TAZs'
EXECUTE [db_meta].[add_xp] 'dimension.geography.taz_13_shape', 'MS_Description', 'series 13 TAZ geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.luz_13', 'MS_Description', 'series 13 LUZ geography zones - for aggregation - abm geographies nest within excluding external TAZs'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cicpa_2016', 'MS_Description', 'Community Planning Areas (City of San Diego) 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cicpa_2016_name', 'MS_Description', 'Community Planning Areas (City of San Diego) 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cocpa_2016', 'MS_Description', 'Community Planning Areas (County of San Diego) 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cocpa_2016_name', 'MS_Description', 'Community Planning Areas (County of San Diego) 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.jurisdiction_2016', 'MS_Description', 'Jurisdictions Year 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.jurisdiction_2016_name', 'MS_Description', 'Jurisdictions Year 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.tract_2010', 'MS_Description', 'Census Tract Year 2010 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup - no one to one lookup exists for series 13 TAZ'
EXECUTE [db_meta].[add_xp] 'dimension.geography.region_2004', 'MS_Description', '2004 San Diego Region geography zones - for aggregation - all geographies excluding external TAZs nest within'
EXECUTE [db_meta].[add_xp] 'dimension.geography.region_2004_name', 'MS_Description', '2004 San Diego Region geography names - for aggregation - all geographies excluding external TAZs nest within'
EXECUTE [db_meta].[add_xp] 'dimension.geography.external_zone', 'MS_Description', 'Non-San Diego Region External Zone names - map to Series 13 TAZ 1-12'
GO


-- create geography role-playing views
-- household location
DROP VIEW IF EXISTS [dimension].[geography_household_location]
GO

CREATE VIEW [dimension].[geography_household_location] AS
SELECT
    [geography_id] AS [geography_household_location_id]
	,[geography_set_id] AS [geography_household_location_set_id]
    ,[mgra_13] AS [household_location_mgra_13]
    ,[mgra_13_shape] AS [household_location_mgra_13_shape]
    ,[taz_13] AS [household_location_taz_13]
    ,[taz_13_shape] AS [household_location_taz_13_shape]
    ,[luz_13] AS [household_location_luz_13]
    ,[cicpa_2016] AS [household_location_cicpa_2016]
    ,[cicpa_2016_name] AS [household_location_cicpa_2016_name]
    ,[cocpa_2016] AS [household_location_cocpa_2016]
    ,[cocpa_2016_name] AS [household_location_cocpa_2016_name]
    ,[jurisdiction_2016] AS [household_location_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [household_location_jurisdiction_2016_name]
    ,[tract_2010] AS [household_location_tract_2010]
    ,[region_2004] AS [household_location_region_2004]
    ,[region_2004_name] AS [household_location_region_2004_name]
    ,[external_zone] AS [household_location_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_household_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- parking location
DROP VIEW IF EXISTS [dimension].[geography_parking_destination]
GO

CREATE VIEW [dimension].[geography_parking_destination] AS
SELECT
    [geography_id] AS [geography_parking_destination_id]
	,[geography_set_id] AS [geography_parking_destination_set_id]
    ,[mgra_13] AS [parking_destination_mgra_13]
    ,[mgra_13_shape] AS [parking_destination_mgra_13_shape]
    ,[taz_13] AS [parking_destination_taz_13]
    ,[taz_13_shape] AS [parking_destination_taz_13_shape]
    ,[luz_13] AS [parking_destination_luz_13]
    ,[cicpa_2016] AS [parking_destination_cicpa_2016]
    ,[cicpa_2016_name] AS [parking_destination_cicpa_2016_name]
    ,[cocpa_2016] AS [parking_destination_cocpa_2016]
    ,[cocpa_2016_name] AS [parking_destination_cocpa_2016_name]
    ,[jurisdiction_2016] AS [parking_destination_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [parking_destination_jurisdiction_2016_name]
    ,[tract_2010] AS [parking_destination_tract_2010]
    ,[region_2004] AS [parking_destination_region_2004]
    ,[region_2004_name] AS [parking_destination_region_2004_name]
    ,[external_zone] AS [parking_destination_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_parking_destination', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- school location
DROP VIEW IF EXISTS [dimension].[geography_school_location]
GO

CREATE VIEW [dimension].[geography_school_location] AS
SELECT
    [geography_id] AS [geography_school_location_id]
	,[geography_set_id] AS [geography_school_location_set_id]
    ,[mgra_13] AS [school_location_mgra_13]
    ,[mgra_13_shape] AS [school_location_mgra_13_shape]
    ,[taz_13] AS [school_location_taz_13]
    ,[taz_13_shape] AS [school_location_taz_13_shape]
    ,[luz_13] AS [school_location_luz_13]
    ,[cicpa_2016] AS [school_location_cicpa_2016]
    ,[cicpa_2016_name] AS [school_location_cicpa_2016_name]
    ,[cocpa_2016] AS [school_location_cocpa_2016]
    ,[cocpa_2016_name] AS [school_location_cocpa_2016_name]
    ,[jurisdiction_2016] AS [school_location_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [school_location_jurisdiction_2016_name]
    ,[tract_2010] AS [school_location_tract_2010]
    ,[region_2004] AS [school_location_region_2004]
    ,[region_2004_name] AS [school_location_region_2004_name]
    ,[external_zone] AS [school_location_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_school_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- tour destination
DROP VIEW IF EXISTS [dimension].[geography_tour_destination]
GO

CREATE VIEW [dimension].[geography_tour_destination] AS
SELECT
    [geography_id] AS [geography_tour_destination_id]
	,[geography_set_id] AS [geography_tour_destination_set_id]
    ,[mgra_13] AS [tour_destination_mgra_13]
    ,[mgra_13_shape] AS [tour_destination_mgra_13_shape]
    ,[taz_13] AS [tour_destination_taz_13]
    ,[taz_13_shape] AS [tour_destination_taz_13_shape]
    ,[luz_13] AS [tour_destination_luz_13]
    ,[cicpa_2016] AS [tour_destination_cicpa_2016]
    ,[cicpa_2016_name] AS [tour_destination_cicpa_2016_name]
    ,[cocpa_2016] AS [tour_destination_cocpa_2016]
    ,[cocpa_2016_name] AS [tour_destination_cocpa_2016_name]
    ,[jurisdiction_2016] AS [tour_destination_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [tour_destination_jurisdiction_2016_name]
    ,[tract_2010] AS [tour_destination_tract_2010]
    ,[region_2004] AS [tour_destination_region_2004]
    ,[region_2004_name] AS [tour_destination_region_2004_name]
    ,[external_zone] AS [tour_destination_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_destination', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- tour origin
DROP VIEW IF EXISTS [dimension].[geography_tour_origin]
GO

CREATE VIEW [dimension].[geography_tour_origin] AS
SELECT
    [geography_id] AS [geography_tour_origin_id]
	,[geography_set_id] AS [geography_tour_origin_set_id]
    ,[mgra_13] AS [tour_origin_mgra_13]
    ,[mgra_13_shape] AS [tour_origin_mgra_13_shape]
    ,[taz_13] AS [tour_origin_taz_13]
    ,[taz_13_shape] AS [tour_origin_taz_13_shape]
    ,[luz_13] AS [tour_origin_luz_13]
    ,[cicpa_2016] AS [tour_origin_cicpa_2016]
    ,[cicpa_2016_name] AS [tour_origin_cicpa_2016_name]
    ,[cocpa_2016] AS [tour_origin_cocpa_2016]
    ,[cocpa_2016_name] AS [tour_origin_cocpa_2016_name]
    ,[jurisdiction_2016] AS [tour_origin_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [tour_origin_jurisdiction_2016_name]
    ,[tract_2010] AS [tour_origin_tract_2010]
    ,[region_2004] AS [tour_origin_region_2004]
    ,[region_2004_name] AS [tour_origin_region_2004_name]
    ,[external_zone] AS [tour_origin_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_origin', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- trip destination
DROP VIEW IF EXISTS [dimension].[geography_trip_destination]
GO

CREATE VIEW [dimension].[geography_trip_destination] AS
SELECT
    [geography_id] AS [geography_trip_destination_id]
	,[geography_set_id] AS [geography_trip_destination_set_id]
    ,[mgra_13] AS [trip_destination_mgra_13]
    ,[mgra_13_shape] AS [trip_destination_mgra_13_shape]
    ,[taz_13] AS [trip_destination_taz_13]
    ,[taz_13_shape] AS [trip_destination_taz_13_shape]
    ,[luz_13] AS [trip_destination_luz_13]
    ,[cicpa_2016] AS [trip_destination_cicpa_2016]
    ,[cicpa_2016_name] AS [trip_destination_cicpa_2016_name]
    ,[cocpa_2016] AS [trip_destination_cocpa_2016]
    ,[cocpa_2016_name] AS [trip_destination_cocpa_2016_name]
    ,[jurisdiction_2016] AS [trip_destination_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [trip_destination_jurisdiction_2016_name]
    ,[tract_2010] AS [trip_destination_tract_2010]
    ,[region_2004] AS [trip_destination_region_2004]
    ,[region_2004_name] AS [trip_destination_region_2004_name]
    ,[external_zone] AS [trip_destination_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_destination', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- trip origin
DROP VIEW IF EXISTS [dimension].[geography_trip_origin]
GO

CREATE VIEW [dimension].[geography_trip_origin] AS
SELECT
    [geography_id] AS [geography_trip_origin_id]
	,[geography_set_id] AS [geography_trip_origin_set_id]
    ,[mgra_13] AS [trip_origin_mgra_13]
    ,[mgra_13_shape] AS [trip_origin_mgra_13_shape]
    ,[taz_13] AS [trip_origin_taz_13]
    ,[taz_13_shape] AS [trip_origin_taz_13_shape]
    ,[luz_13] AS [trip_origin_luz_13]
    ,[cicpa_2016] AS [trip_origin_cicpa_2016]
    ,[cicpa_2016_name] AS [trip_origin_cicpa_2016_name]
    ,[cocpa_2016] AS [trip_origin_cocpa_2016]
    ,[cocpa_2016_name] AS [trip_origin_cocpa_2016_name]
    ,[jurisdiction_2016] AS [trip_origin_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [trip_origin_jurisdiction_2016_name]
    ,[tract_2010] AS [trip_origin_tract_2010]
    ,[region_2004] AS [trip_origin_region_2004]
    ,[region_2004_name] AS [trip_origin_region_2004_name]
    ,[external_zone] AS [trip_origin_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_origin', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- work_location
DROP VIEW IF EXISTS [dimension].[geography_work_location]
GO

CREATE VIEW [dimension].[geography_work_location] AS
SELECT
    [geography_id] AS [geography_work_location_id]
	,[geography_set_id] AS [geography_work_location_set_id]
    ,[mgra_13] AS [work_location_mgra_13]
    ,[mgra_13_shape] AS [work_location_mgra_13_shape]
    ,[taz_13] AS [work_location_taz_13]
    ,[taz_13_shape] AS [work_location_taz_13_shape]
    ,[luz_13] AS [work_location_luz_13]
    ,[cicpa_2016] AS [work_location_cicpa_2016]
    ,[cicpa_2016_name] AS [work_location_cicpa_2016_name]
    ,[cocpa_2016] AS [work_location_cocpa_2016]
    ,[cocpa_2016_name] AS [work_location_cocpa_2016_name]
    ,[jurisdiction_2016] AS [work_location_jurisdiction_2016]
    ,[jurisdiction_2016_name] AS [work_location_jurisdiction_2016_name]
    ,[tract_2010] AS [work_location_tract_2010]
    ,[region_2004] AS [work_location_region_2004]
    ,[region_2004_name] AS [work_location_region_2004_name]
    ,[external_zone] AS [work_location_external_zone]
FROM
    [dimension].[geography]
GO

EXECUTE [db_meta].[add_xp] 'dimension.geography_work_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- create inbound dimension
CREATE TABLE [dimension].[inbound] (
    [inbound_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
    [inbound_description] nvarchar(20) NOT NULL,
    CONSTRAINT pk_inbound PRIMARY KEY ([inbound_id]),
    CONSTRAINT ixuq_inbound UNIQUE ([inbound_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[inbound] VALUES
    ('Not Applicable'), -- insert NULL record as 0
    ('Inbound'),
    ('Outbound')
GO

EXECUTE [db_meta].[add_xp] 'dimension.inbound', 'MS_Description', 'trip inbound or outbound direction reference table'
EXECUTE [db_meta].[add_xp] 'dimension.inbound.inbound_id', 'MS_Description', 'inbound surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.inbound.inbound_description', 'MS_Description', 'inbound description'
GO


-- create mode dimension
CREATE TABLE [dimension].[mode] (
    [mode_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
    [mode_description] nvarchar(75) NOT NULL,
    [mode_aggregate_description] nvarchar(50) NOT NULL,
    CONSTRAINT pk_mode PRIMARY KEY ([mode_id]),
    CONSTRAINT ixuq_mode UNIQUE ([mode_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[mode] VALUES
    ('Not Applicable', 'Not Applicable'), -- insert NULL record as 0
    ('Drive Alone', 'Drive Alone'),
    ('Shared Ride 2', 'Shared Ride 2'),
    ('Shared Ride 3+', 'Shared Ride 3+'),
    ('Walk', 'Walk'),
    ('Micro-Mobility', 'Super-Walk'),
    ('Micro-Transit', 'Super-Walk'),
    ('Bike', 'Bike'),
    ('Walk to Transit', 'Transit'),
    ('Walk to Transit - Local Bus', 'Transit'),
    ('Walk to Transit - Premium Transit', 'Transit'),
    ('Walk to Transit - Local Bus and Premium Transit', 'Transit'),
    ('Park and Ride to Transit', 'Transit'),
    ('Park and Ride to Transit - Local Bus', 'Transit'),
    ('Park and Ride to Transit - Premium Transit', 'Transit'),
    ('Park and Ride to Transit - Local Bus and Premium Transit', 'Transit'),
    ('Kiss and Ride to Transit', 'Transit'),
    ('Kiss and Ride to Transit - Local Bus', 'Transit'),
    ('Kiss and Ride to Transit - Premium Transit', 'Transit'),
    ('Kiss and Ride to Transit - Local Bus and Premium Transit', 'Transit'),
    ('TNC to Transit', 'Transit'),
    ('TNC to Transit - Local Bus', 'Transit'),
    ('TNC to Transit - Premium Transit', 'Transit'),
    ('TNC to Transit - Local Bus and Premium Transit', 'Transit'),
    ('Taxi', 'Taxi'),
    ('Non-Pooled TNC', 'Non-Pooled TNC'),
    ('Pooled TNC', 'Pooled TNC'),
    ('School Bus', 'School Bus'),
    ('Light Heavy Duty Truck', 'Light Heavy Duty Truck'),
    ('Medium Heavy Duty Truck', 'Medium Heavy Duty Truck'),
    ('Heavy Heavy Duty Truck', 'Heavy Heavy Duty Truck'),
    ('Tier 1 Transit', 'Transit'),
    ('Freeway Rapid', 'Transit'),
    ('Arterial Rapid', 'Transit'),
    ('Premium Express Bus', 'Transit'),
    ('Express Bus', 'Transit'),
    ('Local Bus', 'Transit'),
    ('Light Rail', 'Transit'),
    ('Commuter Rail', 'Transit'),
    ('Local Bus and Premium Transit', 'Transit'),
    ('Premium Transit', 'Transit'),
    ('Parking lot terminal', 'Parking Lot'),
    ('Parking lot off-site San Diego Airport area', 'Parking Lot'),
    ('Parking lot off-site private', 'Parking Lot'),
    ('Pickup/Drop-off escort', 'Pickup/Drop-off'),
    ('Pickup/Drop-off curbside', 'Pickup/Drop-off'),
    ('Rental car', 'Rental car'),
    ('Shuttle/Van/Courtesy Vehicle', 'Shuttle/Van/Courtesy Vehicle'),
    ('Transit', 'Transit'),
    ('Highway Network Preload - Bus', 'Transit'),
	('Drive and park location 1', 'Pickup/Drop-off'),
	('Drive and park location 2', 'Pickup/Drop-off'),
	('Drive and park location 3', 'Pickup/Drop-off'),
	('Drive and park location 4', 'Pickup/Drop-off'),
	('Drive and park location 5', 'Pickup/Drop-off'),
	('Park and escort', 'Pickup/Drop-off'),
	('Ride hailing location 1', 'Pickup/Drop-off'),
	('Ride hailing location 2', 'Pickup/Drop-off'),
	('Curbside drop off location 1', 'Pickup/Drop-off'),
	('Curbside drop off location 2', 'Pickup/Drop-off'),
	('Curbside drop off location 3', 'Pickup/Drop-off'),
	('Curbside drop off location 4', 'Pickup/Drop-off'),
	('Curbside drop off location 5', 'Pickup/Drop-off'),
	('Employee/Airport access point to terminal', 'Parking Lot')
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode', 'MS_Description', 'travel mode dimension'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_description', 'MS_Description', 'mode description'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_aggregate_description', 'MS_Description', 'aggregate mode category description'
GO


-- create mode role-playing views
-- airport arrival mode
DROP VIEW IF EXISTS [dimension].[mode_airport_arrival]
GO

CREATE VIEW [dimension].[mode_airport_arrival] AS
SELECT
    [mode_id] AS [mode_airport_arrival_id]
    ,[mode_description] AS [mode_airport_arrival_description]
    ,[mode_aggregate_description] AS [mode_aggregate_airport_arrival_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_airport_arrival', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- tour mode
DROP VIEW IF EXISTS [dimension].[mode_tour]
GO

CREATE VIEW [dimension].[mode_tour] AS
SELECT
    [mode_id] AS [mode_tour_id]
    ,[mode_description] AS [mode_tour_description]
    ,[mode_aggregate_description] AS [mode_aggregate_tour_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_tour', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- trip mode
DROP VIEW IF EXISTS [dimension].[mode_trip]
GO

CREATE VIEW [dimension].[mode_trip] AS
SELECT
    [mode_id] AS [mode_trip_id]
    ,[mode_description] AS [mode_trip_description]
    ,[mode_aggregate_description] AS [mode_aggregate_trip_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_trip', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- transit mode
DROP VIEW IF EXISTS [dimension].[mode_transit]
GO

CREATE VIEW [dimension].[mode_transit] AS
SELECT
    [mode_id] AS [mode_transit_id]
    ,[mode_description] AS [mode_transit_description]
    ,[mode_aggregate_description] AS [mode_aggregate_transit_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_transit', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- transit access mode
DROP VIEW IF EXISTS [dimension].[mode_transit_access]
GO

CREATE VIEW [dimension].[mode_transit_access] AS
SELECT
    [mode_id] AS [mode_transit_access_id]
    ,[mode_description] AS [mode_transit_access_description]
    ,[mode_aggregate_description] AS [mode_aggregate_transit_access_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_access', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- transit route mode
DROP VIEW IF EXISTS [dimension].[mode_transit_route]
GO

CREATE VIEW [dimension].[mode_transit_route] AS
SELECT
    [mode_id] AS [mode_transit_route_id]
    ,[mode_description] AS [mode_transit_route_description]
    ,[mode_aggregate_description] AS [mode_aggregate_transit_route_description]
FROM
    [dimension].[mode]
GO

EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_route', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- create model dimension
CREATE TABLE [dimension].[model] (
    [model_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [model_description] nvarchar(20) NOT NULL,
    [model_aggregate_description] nvarchar(20) NOT NULL,
    CONSTRAINT pk_model PRIMARY KEY ([model_id]),
    CONSTRAINT ixuq_model UNIQUE ([model_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[model] VALUES
    ('Not Applicable', 'Not Applicable'),  -- insert NULL record as 0
    ('Individual', 'Resident Models'),
    ('Joint', 'Resident Models'),
    ('Visitor', 'Visitor'),
    ('Internal-External', 'Resident Models'),
    ('Cross Border', 'Cross Border'),
    ('Airport - CBX', 'Airport - CBX'),
    ('Airport - SAN', 'Airport - SAN'),
    ('Commercial Vehicle', 'Commercial Vehicle'),
    ('External-External', 'External-External'),
    ('External-Internal', 'External-Internal'),
    ('Truck', 'Truck'),
    ('AV 0-Passenger', 'AV 0-Passenger'),
    ('TNC 0-Passenger', 'TNC 0-Passenger')
GO

EXECUTE [db_meta].[add_xp] 'dimension.model', 'MS_Description', 'ABM sub-model dimension'
EXECUTE [db_meta].[add_xp] 'dimension.model.model_id', 'MS_Description', 'model surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.model.model_description', 'MS_Description', 'model description'
EXECUTE [db_meta].[add_xp] 'dimension.model.model_aggregate_description', 'MS_Description', 'aggregate model category description'
GO

-- create model role-playing views
-- tour model
DROP VIEW IF EXISTS [dimension].[model_tour]
GO

CREATE VIEW [dimension].[model_tour] AS
SELECT
    [model_id] AS [model_tour_id]
    ,[model_description] AS [model_tour_description]
    ,[model_aggregate_description] AS [model_tour_aggregate_description]
FROM
    [dimension].[model]
GO

EXECUTE [db_meta].[add_xp] 'dimension.model_tour', 'MS_Description', '[dimension].[model] role playing view'
GO


-- trip model
DROP VIEW IF EXISTS [dimension].[model_trip]
GO

CREATE VIEW [dimension].[model_trip] AS
SELECT
    [model_id] AS [model_trip_id]
    ,[model_description] AS [model_trip_description]
    ,[model_aggregate_description] AS [model_trip_aggregate_description]
FROM
    [dimension].[model]
GO

EXECUTE [db_meta].[add_xp] 'dimension.model_trip', 'MS_Description', '[dimension].[model] role playing view'
GO


-- create purpose dimension
CREATE TABLE [dimension].[purpose] (
    [purpose_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [purpose_description] nvarchar(25) NOT NULL,
    [purpose_aggregate_description] nvarchar(25) NOT NULL,
    CONSTRAINT pk_purpose PRIMARY KEY ([purpose_id]),
    CONSTRAINT ixuq_purpose UNIQUE ([purpose_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[purpose] VALUES
    ('Not Applicable', 'Not Applicable'), -- insert NULL record as 0
    ('Work', 'Work'),
    ('University', 'School/University'),
    ('School', 'School/University'),
    ('Escort', 'Escort'),
    ('Shop', 'Shop'),
    ('Maintenance', 'Maintenance'),
    ('Eating Out', 'Eating Out/Dining'),
    ('Visiting', 'Other'),
    ('Discretionary', 'Other'),
    ('Work-Based', 'Other'),
    ('work related', 'Other'),
    ('Home', 'Return to Origin'),
    ('Other', 'Other'),
    ('Return to Origin', 'Return to Origin'),
    ('External', 'Other'),
    ('Cargo', 'Other'),
    ('Visit', 'Other'),
    ('Recreation', 'Other'),
    ('Dining', 'Eating Out/Dining'),
    ('Resident Business', 'Work'),
    ('Resident Personal', 'Other'),
    ('Visitor Business', 'Work'),
    ('Visitor Personal', 'Other'),
    ('Return to Establishment', 'Return to Origin'),
    ('Goods', 'Other'),
    ('Service', 'Other'),
    ('Non-Work', 'Other'),
    ('Unknown', 'Other'),
    ('Pickup Only', 'TNC Routing'),
    ('Drop-off Only', 'TNC Routing'),
    ('Pickup and Drop-off', 'TNC Routing'),
    ('Refuel', 'TNC Routing'),
	('Employee Parking', 'Work')
GO

EXECUTE [db_meta].[add_xp] 'dimension.purpose', 'MS_Description', 'purpose dimension'
EXECUTE [db_meta].[add_xp] 'dimension.purpose.purpose_id', 'MS_Description', 'purpose surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.purpose.purpose_description', 'MS_Description', 'purpose description'
EXECUTE [db_meta].[add_xp] 'dimension.purpose.purpose_aggregate_description', 'MS_Description', 'aggregate purpose category description'
GO


-- create purpose role-playing views
-- tour purpose
DROP VIEW IF EXISTS [dimension].[purpose_tour]
GO

CREATE VIEW [dimension].[purpose_tour] AS
SELECT
    [purpose_id] AS [purpose_tour_id],
    [purpose_description] AS [purpose_tour_description],
    [purpose_aggregate_description] AS [purpose_tour_aggregate_description]
FROM
    [dimension].[purpose]
GO

EXECUTE [db_meta].[add_xp] 'dimension.purpose_tour', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- trip destination purpose
DROP VIEW IF EXISTS [dimension].[purpose_trip_destination]
GO

CREATE VIEW [dimension].[purpose_trip_destination] AS
SELECT
    [purpose_id] AS [purpose_trip_destination_id],
    [purpose_description] AS [purpose_trip_destination_description],
    [purpose_aggregate_description] AS [purpose_trip_destination_aggregate_description]
FROM
    [dimension].[purpose]
GO

EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_destination', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- trip origin purpose
DROP VIEW IF EXISTS [dimension].[purpose_trip_origin]
GO

CREATE VIEW [dimension].[purpose_trip_origin] AS
SELECT
    [purpose_id] AS [purpose_trip_origin_id],
    [purpose_description] AS [purpose_trip_origin_description],
    [purpose_aggregate_description] AS [purpose_trip_origin_aggregate_description]
FROM
    [dimension].[purpose]
GO

EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_origin', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- create time dimension
CREATE TABLE [dimension].[time] (
    [time_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [abm_half_hour] nvarchar(20) NOT NULL,
    [abm_half_hour_period_start] time(0) NULL,
    [abm_half_hour_period_end] time(0) NULL,
    [abm_5_tod] nvarchar(20) NOT NULL,
    [abm_5_tod_period_start] time(0) NULL,
    [abm_5_tod_period_end] time(0) NULL,
    [day] nvarchar(20) NOT NULL,
    [day_period_start] time(0) NULL,
    [day_period_end] time(0) NULL,
    CONSTRAINT pk_time PRIMARY KEY([time_id]),
    CONSTRAINT ixuq_time UNIQUE([time_id], [abm_half_hour], [abm_5_tod], [day]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[time] VALUES
    ('Not Applicable',NULL,NULL,'Not Applicable',NULL,NULL,'Not Applicable',NULL,NULL),  -- insert NULL record as 0
    ('Not Applicable',NULL,NULL,'Not Applicable',NULL,NULL,'1','00:00:00','23:59:59'),
    ('Not Applicable',NULL,NULL,'1','03:00:00','05:59:59','1','00:00:00','23:59:59'),
    ('Not Applicable',NULL,NULL,'2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('Not Applicable',NULL,NULL,'3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('Not Applicable',NULL,NULL,'4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('Not Applicable',NULL,NULL,'5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('1','03:00:00','04:59:59','1','03:00:00','05:59:59','1','00:00:00','23:59:59'),
    ('2','05:00:00','05:29:59','1','03:00:00','05:59:59','1','00:00:00','23:59:59'),
    ('3','05:30:00','05:59:59','1','03:00:00','05:59:59','1','00:00:00','23:59:59'),
    ('4','06:00:00','06:29:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('5','06:30:00','06:59:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('6','07:00:00','07:29:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('7','07:30:00','07:59:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('8','08:00:00','08:29:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('9','08:30:00','08:59:59','2','06:00:00','08:59:59','1','00:00:00','23:59:59'),
    ('10','09:00:00','09:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('11','09:30:00','09:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('12','10:00:00','10:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('13','10:30:00','10:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('14','11:00:00','11:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('15','11:30:00','11:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('16','12:00:00','12:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('17','12:30:00','12:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('18','13:00:00','13:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('19','13:30:00','13:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('20','14:00:00','14:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('21','14:30:00','14:59:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('22','15:00:00','15:29:59','3','09:00:00','15:29:59','1','00:00:00','23:59:59'),
    ('23','15:30:00','15:59:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('24','16:00:00','16:29:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('25','16:30:00','16:59:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('26','17:00:00','17:29:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('27','17:30:00','17:59:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('28','18:00:00','18:29:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('29','18:30:00','18:59:59','4','15:30:00','18:59:59','1','00:00:00','23:59:59'),
    ('30','19:00:00','19:29:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('31','19:30:00','19:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('32','20:00:00','20:29:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('33','20:30:00','20:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('34','21:00:00','21:29:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('35','21:30:00','21:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('36','22:00:00','22:29:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('37','22:30:00','22:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('38','23:00:00','23:29:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('39','23:30:00','23:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59'),
    ('40','00:00:00','02:59:59','5','19:00:00','02:59:59','1','00:00:00','23:59:59')
GO

EXECUTE [db_meta].[add_xp] 'dimension.time', 'MS_Description', 'time dimension for ABM model including cross references'
EXECUTE [db_meta].[add_xp] 'dimension.time.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour', 'MS_Description', 'ABM half hour time period - base time unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour_period_start', 'MS_Description', 'start time of ABM half hour time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour_period_end', 'MS_Description', 'end time of ABM half hour time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod', 'MS_Description', 'ABM five time of day time period - base and secondary time unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod_period_start', 'MS_Description', 'start time of ABM five time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod_period_end', 'MS_Description', 'end time of ABM five time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.day', 'MS_Description', 'day time period - for aggregation'
EXECUTE [db_meta].[add_xp] 'dimension.time.day_period_start', 'MS_Description', 'start time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.day_period_end', 'MS_Description', 'end time of day time period'
GO


-- create time role-playing views
-- tour end time
DROP VIEW IF EXISTS [dimension].[time_tour_end]
GO

CREATE VIEW [dimension].[time_tour_end] AS
SELECT
    [time_id] AS [time_tour_end_id]
    ,[abm_half_hour] AS [tour_end_abm_half_hour]
    ,[abm_half_hour_period_start] AS [tour_end_abm_half_hour_period_start]
    ,[abm_half_hour_period_end] AS [tour_end_abm_half_hour_period_end]
    ,[abm_5_tod] AS [tour_end_abm_5_tod]
    ,[abm_5_tod_period_start] AS [tour_end_abm_5_tod_period_start]
    ,[abm_5_tod_period_end] AS [tour_end_abm_5_tod_period_end]
    ,[day] AS [tour_end_day]
    ,[day_period_start] AS [tour_end_day_period_start]
    ,[day_period_end] AS [tour_end_day_period_end]
FROM
    [dimension].[time]
GO

EXECUTE [db_meta].[add_xp] 'dimension.time_tour_end', 'MS_Description', '[dimension].[time] role playing view'
GO


-- tour start time
DROP VIEW IF EXISTS [dimension].[time_tour_start]
GO

CREATE VIEW [dimension].[time_tour_start] AS
SELECT
    [time_id] AS [time_tour_start_id]
    ,[abm_half_hour] AS [tour_start_abm_half_hour]
    ,[abm_half_hour_period_start] AS [tour_start_abm_half_hour_period_start]
    ,[abm_half_hour_period_end] AS [tour_start_abm_half_hour_period_end]
    ,[abm_5_tod] AS [tour_start_abm_5_tod]
    ,[abm_5_tod_period_start] AS [tour_start_abm_5_tod_period_start]
    ,[abm_5_tod_period_end] AS [tour_start_abm_5_tod_period_end]
    ,[day] AS [tour_start_day]
    ,[day_period_start] AS [tour_start_day_period_start]
    ,[day_period_end] AS [tour_start_day_period_end]
FROM
    [dimension].[time]
GO

EXECUTE [db_meta].[add_xp] 'dimension.time_tour_start', 'MS_Description', '[dimension].[time] role playing view'
GO


-- trip end time
DROP VIEW IF EXISTS [dimension].[time_trip_end]
GO

CREATE VIEW [dimension].[time_trip_end] AS
SELECT
    [time_id] AS [time_trip_end_id]
    ,[abm_half_hour] AS [trip_end_abm_half_hour]
    ,[abm_half_hour_period_start] AS [trip_end_abm_half_hour_period_start]
    ,[abm_half_hour_period_end] AS [trip_end_abm_half_hour_period_end]
    ,[abm_5_tod] AS [trip_end_abm_5_tod]
    ,[abm_5_tod_period_start] AS [trip_end_abm_5_tod_period_start]
    ,[abm_5_tod_period_end] AS [trip_end_abm_5_tod_period_end]
    ,[day] AS [trip_end_day]
    ,[day_period_start] AS [trip_end_day_period_start]
    ,[day_period_end] AS [trip_end_day_period_end]
FROM
    [dimension].[time]
GO

EXECUTE [db_meta].[add_xp] 'dimension.time_trip_end', 'MS_Description', '[dimension].[time] role playing view'
GO


-- trip start time
DROP VIEW IF EXISTS [dimension].[time_trip_start]
GO

CREATE VIEW [dimension].[time_trip_start] AS
SELECT
    [time_id] AS [time_trip_start_id]
    ,[abm_half_hour] AS [trip_start_abm_half_hour]
    ,[abm_half_hour_period_start] AS [trip_start_abm_half_hour_period_start]
    ,[abm_half_hour_period_end] AS [trip_start_abm_half_hour_period_end]
    ,[abm_5_tod] AS [trip_start_abm_5_tod]
    ,[abm_5_tod_period_start] AS [trip_start_abm_5_tod_period_start]
    ,[abm_5_tod_period_end] AS [trip_start_abm_5_tod_period_end]
    ,[day] AS [trip_start_day]
    ,[day_period_start] AS [trip_start_day_period_start]
    ,[day_period_end] AS [trip_start_day_period_end]
FROM
    [dimension].[time]
GO

EXECUTE [db_meta].[add_xp] 'dimension.time_trip_start', 'MS_Description', '[dimension].[time] role playing view'
GO


-- create transponder availability dimension
CREATE TABLE [dimension].[transponder_available] (
    [transponder_available_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [transponder_available_description] nvarchar(25) NOT NULL,
    CONSTRAINT pk_transponderAvailable PRIMARY KEY([transponder_available_id]),
    CONSTRAINT ixuq_transponderAvailable UNIQUE([transponder_available_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[transponder_available] VALUES
    ('Not Applicable'),
    ('Transponder Not Available'),
    ('Transponder Available')
GO

EXECUTE [db_meta].[add_xp] 'dimension.transponder_available', 'MS_Description', 'dimension table for ABM transponder availability'
EXECUTE [db_meta].[add_xp] 'dimension.transponder_available.transponder_available_id', 'MS_Description', 'ABM transponder availability surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transponder_available.transponder_available_description', 'MS_Description', 'ABM transponder availability description'
GO


-- create value of time category dimension
CREATE TABLE [dimension].[value_of_time_category] (
    [value_of_time_category_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [value_of_time_category_description] nvarchar(20) NOT NULL,
    CONSTRAINT pk_valueOfTimeCategory PRIMARY KEY([value_of_time_category_id]),
    CONSTRAINT ixuq_valueOfTimeCategory UNIQUE([value_of_time_category_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[value_of_time_category] VALUES
    ('Not Applicable'),
    ('Low'),
    ('Medium'),
    ('High')
GO

EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_category', 'MS_Description', 'dimension table for ABM value of time categories'
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_category.value_of_time_category_id', 'MS_Description', 'ABM value of time category surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_category.value_of_time_category_description', 'MS_Description', 'ABM value of time category description'
GO
