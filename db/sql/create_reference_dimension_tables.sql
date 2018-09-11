SET NOCOUNT ON;


-- create dimension schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='dimension')
EXEC (N'CREATE SCHEMA [dimension]')


-- create scenario dimension
CREATE TABLE [dimension].[scenario] (
	[scenario_id] int NOT NULL,
	[name] nchar(50) NOT NULL,
	[year] smallint NOT NULL,
	[iteration] tinyint NOT NULL,
	[sample_rate] decimal(6,4) NOT NULL,
	[abm_version] nchar(50) NOT NULL,
	[path] nchar(200) NOT NULL,
	[user_name] nchar(100) NOT NULL,
	[complete] bit NOT NULL,
	[date_loaded] smalldatetime NULL,
	[load_failed] bit NOT NULL,
	CONSTRAINT pk_scenario PRIMARY KEY ([scenario_id]))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)


-- create escort_stop_type dimension
CREATE TABLE [dimension].[escort_stop_type] (
	[escort_stop_type_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[escort_stop_type_description] nchar(20) NOT NULL,
	CONSTRAINT pk_escortstoptype PRIMARY KEY ([escort_stop_type_id]),
	CONSTRAINT ixuq_escortstoptype UNIQUE ([escort_stop_type_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[escort_stop_type] VALUES
('Not Applicable'), -- insert NULL record as 0
('No Escort'),
('Dropoff'),
('Pickup')
GO

-- create escort_stop_type role-playing views
-- origin
CREATE VIEW [dimension].[escort_stop_type_origin] AS
SELECT
	[escort_stop_type_id] AS [escort_stop_type_origin_id]
	,[escort_stop_type_description] AS [escort_stop_type_origin_description]
FROM
	[dimension].[escort_stop_type]
GO

-- destination
CREATE VIEW [dimension].[escort_stop_type_destination] AS
SELECT
	[escort_stop_type_id] AS [escort_stop_type_destination_id]
	,[escort_stop_type_description] AS [escort_stop_type_destination_description]
FROM
	[dimension].[escort_stop_type]
GO


-- create geography dimension
-- use an input file and an update query to fill at a later step
CREATE TABLE [dimension].[geography] (
	[geography_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[mgra_13] nchar(20) NOT NULL,
	[mgra_13_shape] geometry NULL,
	[taz_13] nchar(20) NOT NULL,
	[taz_13_shape] geometry NULL,
	[luz_13] nchar(20) NOT NULL,
	[luz_13_shape] geometry NULL,
	[cicpa_2016] nchar(20) NOT NULL,
	[cicpa_2016_name] nchar(50) NOT NULL,
	[cicpa_2016_shape] geometry NULL,
	[cocpa_2016] nchar(20) NOT NULL,
	[cocpa_2016_name] nchar(50) NOT NULL,
	[cocpa_2016_shape] geometry NULL,
	[jurisdiction_2016] nchar(20) NOT NULL,
	[jurisdiction_2016_name] nchar(50) NOT NULL,
	[jurisdiction_2016_shape] geometry NULL,
	[region_2004] nchar(20) NOT NULL,
	[region_2004_name] nchar(50) NOT NULL,
	[region_2004_shape] geometry NULL,
	[external_zone] nchar(20) NOT NULL,
	CONSTRAINT pk_geography PRIMARY KEY([geography_id]),
	CONSTRAINT ixuq_geography UNIQUE([mgra_13], [taz_13]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_mgra_13 NONCLUSTERED ([geography_id], [mgra_13]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_taz_13 NONCLUSTERED ([geography_id], [taz_13]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_luz_13 NONCLUSTERED ([geography_id], [luz_13]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_cicpa_2016 NONCLUSTERED ([geography_id], [cicpa_2016], [cicpa_2016_name]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_cocpa_2016 NONCLUSTERED ([geography_id], [cocpa_2016], [cocpa_2016_name]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_jurisdiction_2016 NONCLUSTERED ([geography_id], [jurisdiction_2016], [jurisdiction_2016_name]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_region_2004 NONCLUSTERED ([geography_id], [region_2004], [region_2004_name]) WITH (DATA_COMPRESSION = PAGE),
	INDEX ix_geography_external_zone NONCLUSTERED ([geography_id], [external_zone]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
GO


-- create geography role-playing views
-- household location
CREATE VIEW [dimension].[geography_household_location] AS
SELECT
	[geography_id] AS [geography_household_location_id]
	,[mgra_13] AS [household_location_mgra_13]
	,[mgra_13_shape] AS [household_location_mgra_13_shape]
	,[taz_13] AS [household_location_taz_13]
	,[taz_13_shape] AS [household_location_taz_13_shape]
	,[luz_13] AS [household_location_luz_13]
	,[luz_13_shape] AS [household_location_luz_13_shape]
	,[cicpa_2016] AS [household_location_cicpa_2016]
	,[cicpa_2016_name] AS [household_location_cicpa_2016_name]
	,[cicpa_2016_shape] AS [household_location_cicpa_2016_shape]
	,[cocpa_2016] AS [household_location_cocpa_2016]
	,[cocpa_2016_name] AS [household_location_cocpa_2016_name]
	,[cocpa_2016_shape] AS [household_location_cocpa_2016_shape]
	,[jurisdiction_2016] AS [household_location_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [household_location_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [household_location_jurisdiction_2016_shape]
	,[region_2004] AS [household_location_region_2004]
	,[region_2004_name] AS [household_location_region_2004_name]
	,[region_2004_shape] AS [household_location_region_2004_shape]
	,[external_zone] AS [household_location_external_zone]
FROM
	[dimension].[geography]
GO

-- parking location
CREATE VIEW [dimension].[geography_parking_destination] AS
SELECT
	[geography_id] AS [geography_parking_destination_id]
	,[mgra_13] AS [parking_destination_mgra_13]
	,[mgra_13_shape] AS [parking_destination_mgra_13_shape]
	,[taz_13] AS [parking_destination_taz_13]
	,[taz_13_shape] AS [parking_destination_taz_13_shape]
	,[luz_13] AS [parking_destination_luz_13]
	,[luz_13_shape] AS [parking_destination_luz_13_shape]
	,[cicpa_2016] AS [parking_destination_cicpa_2016]
	,[cicpa_2016_name] AS [parking_destination_cicpa_2016_name]
	,[cicpa_2016_shape] AS [parking_destination_cicpa_2016_shape]
	,[cocpa_2016] AS [parking_destination_cocpa_2016]
	,[cocpa_2016_name] AS [parking_destination_cocpa_2016_name]
	,[cocpa_2016_shape] AS [parking_destination_cocpa_2016_shape]
	,[jurisdiction_2016] AS [parking_destination_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [parking_destination_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [parking_destination_jurisdiction_2016_shape]
	,[region_2004] AS [parking_destination_region_2004]
	,[region_2004_name] AS [parking_destination_region_2004_name]
	,[region_2004_shape] AS [parking_destination_region_2004_shape]
	,[external_zone] AS [parking_destination_external_zone]
FROM
	[dimension].[geography]
GO

-- school location
CREATE VIEW [dimension].[geography_school_location] AS
SELECT
	[geography_id] AS [geography_school_location_id]
	,[mgra_13] AS [school_location_mgra_13]
	,[mgra_13_shape] AS [school_location_mgra_13_shape]
	,[taz_13] AS [school_location_taz_13]
	,[taz_13_shape] AS [school_location_taz_13_shape]
	,[luz_13] AS [school_location_luz_13]
	,[luz_13_shape] AS [school_location_luz_13_shape]
	,[cicpa_2016] AS [school_location_cicpa_2016]
	,[cicpa_2016_name] AS [school_location_cicpa_2016_name]
	,[cicpa_2016_shape] AS [school_location_cicpa_2016_shape]
	,[cocpa_2016] AS [school_location_cocpa_2016]
	,[cocpa_2016_name] AS [school_location_cocpa_2016_name]
	,[cocpa_2016_shape] AS [school_location_cocpa_2016_shape]
	,[jurisdiction_2016] AS [school_location_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [school_location_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [school_location_jurisdiction_2016_shape]
	,[region_2004] AS [school_location_region_2004]
	,[region_2004_name] AS [school_location_region_2004_name]
	,[region_2004_shape] AS [school_location_region_2004_shape]
	,[external_zone] AS [school_location_external_zone]
FROM
	[dimension].[geography]
GO

-- tour destination
CREATE VIEW [dimension].[geography_tour_destination] AS
SELECT
	[geography_id] AS [geography_tour_destination_id]
	,[mgra_13] AS [tour_destination_mgra_13]
	,[mgra_13_shape] AS [tour_destination_mgra_13_shape]
	,[taz_13] AS [tour_destination_taz_13]
	,[taz_13_shape] AS [tour_destination_taz_13_shape]
	,[luz_13] AS [tour_destination_luz_13]
	,[luz_13_shape] AS [tour_destination_luz_13_shape]
	,[cicpa_2016] AS [tour_destination_cicpa_2016]
	,[cicpa_2016_name] AS [tour_destination_cicpa_2016_name]
	,[cicpa_2016_shape] AS [tour_destination_cicpa_2016_shape]
	,[cocpa_2016] AS [tour_destination_cocpa_2016]
	,[cocpa_2016_name] AS [tour_destination_cocpa_2016_name]
	,[cocpa_2016_shape] AS [tour_destination_cocpa_2016_shape]
	,[jurisdiction_2016] AS [tour_destination_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [tour_destination_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [tour_destination_jurisdiction_2016_shape]
	,[region_2004] AS [tour_destination_region_2004]
	,[region_2004_name] AS [tour_destination_region_2004_name]
	,[region_2004_shape] AS [tour_destination_region_2004_shape]
	,[external_zone] AS [tour_destination_external_zone]
FROM
	[dimension].[geography]
GO

-- tour origin
CREATE VIEW [dimension].[geography_tour_origin] AS
SELECT
	[geography_id] AS [geography_tour_origin_id]
	,[mgra_13] AS [tour_origin_mgra_13]
	,[mgra_13_shape] AS [tour_origin_mgra_13_shape]
	,[taz_13] AS [tour_origin_taz_13]
	,[taz_13_shape] AS [tour_origin_taz_13_shape]
	,[luz_13] AS [tour_origin_luz_13]
	,[luz_13_shape] AS [tour_origin_luz_13_shape]
	,[cicpa_2016] AS [tour_origin_cicpa_2016]
	,[cicpa_2016_name] AS [tour_origin_cicpa_2016_name]
	,[cicpa_2016_shape] AS [tour_origin_cicpa_2016_shape]
	,[cocpa_2016] AS [tour_origin_cocpa_2016]
	,[cocpa_2016_name] AS [tour_origin_cocpa_2016_name]
	,[cocpa_2016_shape] AS [tour_origin_cocpa_2016_shape]
	,[jurisdiction_2016] AS [tour_origin_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [tour_origin_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [tour_origin_jurisdiction_2016_shape]
	,[region_2004] AS [tour_origin_region_2004]
	,[region_2004_name] AS [tour_origin_region_2004_name]
	,[region_2004_shape] AS [tour_origin_region_2004_shape]
	,[external_zone] AS [tour_origin_external_zone]
FROM
	[dimension].[geography]
GO

-- trip destination
CREATE VIEW [dimension].[geography_trip_destination] AS
SELECT
	[geography_id] AS [geography_trip_destination_id]
	,[mgra_13] AS [trip_destination_mgra_13]
	,[mgra_13_shape] AS [trip_destination_mgra_13_shape]
	,[taz_13] AS [trip_destination_taz_13]
	,[taz_13_shape] AS [trip_destination_taz_13_shape]
	,[luz_13] AS [trip_destination_luz_13]
	,[luz_13_shape] AS [trip_destination_luz_13_shape]
	,[cicpa_2016] AS [trip_destination_cicpa_2016]
	,[cicpa_2016_name] AS [trip_destination_cicpa_2016_name]
	,[cicpa_2016_shape] AS [trip_destination_cicpa_2016_shape]
	,[cocpa_2016] AS [trip_destination_cocpa_2016]
	,[cocpa_2016_name] AS [trip_destination_cocpa_2016_name]
	,[cocpa_2016_shape] AS [trip_destination_cocpa_2016_shape]
	,[jurisdiction_2016] AS [trip_destination_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [trip_destination_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [trip_destination_jurisdiction_2016_shape]
	,[region_2004] AS [trip_destination_region_2004]
	,[region_2004_name] AS [trip_destination_region_2004_name]
	,[region_2004_shape] AS [trip_destination_region_2004_shape]
	,[external_zone] AS [trip_destination_external_zone]
FROM
	[dimension].[geography]
GO

-- trip origin
CREATE VIEW [dimension].[geography_trip_origin] AS
SELECT
	[geography_id] AS [geography_trip_origin_id]
	,[mgra_13] AS [trip_origin_mgra_13]
	,[mgra_13_shape] AS [trip_origin_mgra_13_shape]
	,[taz_13] AS [trip_origin_taz_13]
	,[taz_13_shape] AS [trip_origin_taz_13_shape]
	,[luz_13] AS [trip_origin_luz_13]
	,[luz_13_shape] AS [trip_origin_luz_13_shape]
	,[cicpa_2016] AS [trip_origin_cicpa_2016]
	,[cicpa_2016_name] AS [trip_origin_cicpa_2016_name]
	,[cicpa_2016_shape] AS [trip_origin_cicpa_2016_shape]
	,[cocpa_2016] AS [trip_origin_cocpa_2016]
	,[cocpa_2016_name] AS [trip_origin_cocpa_2016_name]
	,[cocpa_2016_shape] AS [trip_origin_cocpa_2016_shape]
	,[jurisdiction_2016] AS [trip_origin_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [trip_origin_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [trip_origin_jurisdiction_2016_shape]
	,[region_2004] AS [trip_origin_region_2004]
	,[region_2004_name] AS [trip_origin_region_2004_name]
	,[region_2004_shape] AS [trip_origin_region_2004_shape]
	,[external_zone] AS [trip_origin_external_zone]
FROM
	[dimension].[geography]
GO

-- work_location
CREATE VIEW [dimension].[geography_work_location] AS
SELECT
	[geography_id] AS [geography_work_location_id]
	,[mgra_13] AS [work_location_mgra_13]
	,[mgra_13_shape] AS [work_location_mgra_13_shape]
	,[taz_13] AS [work_location_taz_13]
	,[taz_13_shape] AS [work_location_taz_13_shape]
	,[luz_13] AS [work_location_luz_13]
	,[luz_13_shape] AS [work_location_luz_13_shape]
	,[cicpa_2016] AS [work_location_cicpa_2016]
	,[cicpa_2016_name] AS [work_location_cicpa_2016_name]
	,[cicpa_2016_shape] AS [work_location_cicpa_2016_shape]
	,[cocpa_2016] AS [work_location_cocpa_2016]
	,[cocpa_2016_name] AS [work_location_cocpa_2016_name]
	,[cocpa_2016_shape] AS [work_location_cocpa_2016_shape]
	,[jurisdiction_2016] AS [work_location_jurisdiction_2016]
	,[jurisdiction_2016_name] AS [work_location_jurisdiction_2016_name]
	,[jurisdiction_2016_shape] AS [work_location_jurisdiction_2016_shape]
	,[region_2004] AS [work_location_region_2004]
	,[region_2004_name] AS [work_location_region_2004_name]
	,[region_2004_shape] AS [work_location_region_2004_shape]
	,[external_zone] AS [work_location_external_zone]
FROM
	[dimension].[geography]
GO


-- create inbound dimension
-- will add aggregations in later
CREATE TABLE [dimension].[inbound] (
	[inbound_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[inbound_description] nchar(20) NOT NULL,
	CONSTRAINT pk_inbound PRIMARY KEY ([inbound_id]),
	CONSTRAINT ixuq_inbound UNIQUE ([inbound_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[inbound] VALUES
('Not Applicable'), -- insert NULL record as 0
('Inbound'),
('Outbound')
GO


-- create mode dimension
-- will add aggregations in later
CREATE TABLE [dimension].[mode] (
	[mode_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[mode_description] nchar(75) NOT NULL,
	CONSTRAINT pk_mode PRIMARY KEY ([mode_id]),
	CONSTRAINT ixuq_mode UNIQUE ([mode_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[mode] VALUES
('Not Applicable'), -- insert NULL record as 0
('Drive Alone Non-Toll'),
('Drive Alone Toll Eligible'),
('Shared Ride 2 Non-Toll'),
('Shared Ride 2 Toll Eligible'),
('Shared Ride 3 Non-Toll'),
('Shared Ride 3 Toll Eligible'),
('Walk'),
('Bike'),
('Walk to Transit'),
('Walk to Transit - Local Bus Only'),
('Walk to Transit - Premium Transit Only'),
('Walk to Transit - Local Bus and Premium Transit'),
('Park and Ride to Transit'),
('Park and Ride to Transit - Local Bus Only'),
('Park and Ride to Transit - Premium Transit Only'),
('Park and Ride to Transit - Local Bus and Premium Transit'),
('Kiss and Ride to Transit'),
('Kiss and Ride to Transit - Local Bus Only'),
('Kiss and Ride to Transit - Premium Transit Only'),
('Kiss and Ride to Transit - Local Bus and Premium Transit'),
('School Bus'),
('Taxi'),
('Commuter Rail'),
('Light Rail'),
('Freeway Rapid'),
('Arterial Rapid'),
('Premium Express Bus'),
('Express Bus'),
('Local Bus'),
('Local Bus Only'),
('Local Bus and Premium Transit'),
('Premium Transit Only'),
('Heavy Truck - Non-Toll'),
('Heavy Truck - Toll'),
('Intermediate Truck - Non-Toll'),
('Intermediate Truck - Toll'),
('Light Vehicle - Non-Toll'),
('Light Vehicle - Toll'),
('Medium Truck - Non-Toll'),
('Medium Truck - Toll'),
('Light Heavy Duty Truck (Non-Toll)'),
('Light Heavy Duty Truck (Toll)'),
('Medium Heavy Duty Truck (Non-Toll)'),
('Medium Heavy Duty Truck (Toll)'),
('Heavy Heavy Duty Truck (Non-Toll)'),
('Heavy Heavy Duty Truck (Toll)'),
('Parking lot terminal'),
('Parking lot off-site San Diego Airport area'),
('Parking lot off-site private'),
('Pickup/Drop-off escort'),
('Pickup/Drop-off curbside'),
('Rental car'),
('Shuttle/Van/Courtesy Vehicle'),
('Transit'),
('Highway Network Preload - Bus')
GO


-- create mode role-playing views
-- airport arrival mode
CREATE VIEW [dimension].[mode_airport_arrival] AS
SELECT
	[mode_id] AS [mode_airport_arrival_id]
	,[mode_description] AS [mode_airport_arrival_description]
FROM
	[dimension].[mode]
GO


-- tour mode
CREATE VIEW [dimension].[mode_tour] AS
SELECT
	[mode_id] AS [mode_tour_id]
	,[mode_description] AS [mode_tour_description]
FROM
	[dimension].[mode]
GO

-- trip mode
CREATE VIEW [dimension].[mode_trip] AS
SELECT
	[mode_id] AS [mode_trip_id]
	,[mode_description] AS [mode_trip_description]
FROM
	[dimension].[mode]
GO

-- transit mode
CREATE VIEW [dimension].[mode_transit] AS
SELECT
	[mode_id] AS [mode_transit_id]
	,[mode_description] AS [mode_transit_description]
FROM
	[dimension].[mode]
GO

-- transit access mode
CREATE VIEW [dimension].[mode_transit_access] AS
SELECT
	[mode_id] AS [mode_transit_access_id]
	,[mode_description] AS [mode_transit_access_description]
FROM
	[dimension].[mode]
GO

-- transit route mode
CREATE VIEW [dimension].[mode_transit_route] AS
SELECT
	[mode_id] AS [mode_transit_route_id]
	,[mode_description] AS [mode_transit_route_description]
FROM
	[dimension].[mode]
GO


-- create model dimension
-- will add aggregations in later
CREATE TABLE [dimension].[model] (
	[model_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[model_description] nchar(20) NOT NULL,
	CONSTRAINT pk_model PRIMARY KEY ([model_id]),
	CONSTRAINT ixuq_model UNIQUE ([model_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[model] VALUES
('Not Applicable'), -- insert NULL record as 0
('Individual'),
('Joint'),
('Visitor'),
('Internal-External'),
('Cross Border'),
('Airport - CBX'),
('Airport - SAN'),
('Commercial Vehicle'),
('External-External'),
('External-Internal'),
('Truck')
GO

-- create model role-playing views
-- tour model
CREATE VIEW [dimension].[model_tour] AS
SELECT
	[model_id] AS [model_tour_id]
	,[model_description] AS [model_tour_description]
FROM
	[dimension].[model]
GO

-- trip model
CREATE VIEW [dimension].[model_trip] AS
SELECT
	[model_id] AS [model_trip_id]
	,[model_description] AS [model_trip_description]
FROM
	[dimension].[model]
GO


-- create purpose dimension
-- will add aggregations in later
CREATE TABLE [dimension].[purpose] (
	[purpose_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[purpose_description] nchar(25) NOT NULL,
	CONSTRAINT pk_purpose PRIMARY KEY ([purpose_id]),
	CONSTRAINT ixuq_purpose UNIQUE ([purpose_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[purpose] VALUES
('Not Applicable'), -- insert NULL record as 0
('None'),
('Work'),
('University'),
('School'),
('Escort'),
('Shop'),
('Maintenance'),
('Eating Out'),
('Visiting'),
('Discretionary'),
('Work-Based'),
('Work-Related'),
('Home'),
('Other'),
('Return to Origin'),
('External'),
('Cargo'),
('Visit'),
('Recreation'),
('Dining'),
('Business-Dining'),
('Business-Recreation'),
('Business-Work'),
('Personal-Dining'),
('Personal-Recreation'),
('Personal-Work'),
('Resident-Business'),
('Resident-Personal'),
('Visitor-Business'),
('Visitor-Personal'),
('Return to Establishment'),
('Goods'),
('Service'),
('Non-Work'),
('Unknown')
GO


-- create purpose role-playing views
-- tour purpose
CREATE VIEW [dimension].[purpose_tour] AS
SELECT
	[purpose_id] AS [purpose_tour_id],
	[purpose_description] AS [purpose_tour_description]
FROM
	[dimension].[purpose]
GO

-- trip start purpose
CREATE VIEW [dimension].[purpose_trip_destination] AS
SELECT
	[purpose_id] AS [purpose_trip_destination_id],
	[purpose_description] AS [purpose_trip_destination_description]
FROM
	[dimension].[purpose]
GO

-- trip start purpose
CREATE VIEW [dimension].[purpose_trip_origin] AS
SELECT
	[purpose_id] AS [purpose_trip_origin_id],
	[purpose_description] AS [purpose_trip_origin_description]
FROM
	[dimension].[purpose]
GO


-- create time dimension
CREATE TABLE [dimension].[time] (
	[time_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[abm_half_hour] nchar(20) NOT NULL,
	[abm_half_hour_period_start] time(0) NULL,
	[abm_half_hour_period_end] time(0) NULL,
	[abm_5_tod] nchar(20) NOT NULL,
	[abm_5_tod_period_start] time(0) NULL,
	[abm_5_tod_period_end] time(0) NULL,
	[day] nchar(20) NOT NULL,
	[day_period_start] time(0) NULL,
	[day_period_end] time(0) NULL,
	CONSTRAINT pk_time PRIMARY KEY([time_id]),
	CONSTRAINT ixuq_time UNIQUE([time_id], [abm_half_hour], [abm_5_tod], [day]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[time] VALUES
('Not Applicable',NULL,NULL,'Not Applicable',NULL,NULL,'Not Applicable',NULL,NULL), -- insert NULL record as 0
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

-- create time role-playing views
-- tour end time
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

-- tour start time
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

-- trip end time
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

-- trip start time
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


-- create value_of_time_drive_bin dimension
CREATE TABLE [dimension].[value_of_time_drive_bin] (
	[value_of_time_drive_bin_id] tinyint IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[value_of_time_drive_bin_description] nchar(20) NOT NULL,
	CONSTRAINT pk_valueoftimedrivebin PRIMARY KEY ([value_of_time_drive_bin_id]),
	CONSTRAINT ixuq_valueoftimedrivebin UNIQUE ([value_of_time_drive_bin_description]) WITH (DATA_COMPRESSION = PAGE))
ON reference_fg
WITH (DATA_COMPRESSION = PAGE)
INSERT INTO [dimension].[value_of_time_drive_bin] VALUES
('Not Applicable'), -- insert NULL record as 0
('Low'),
('Medium'),
('High')
GO