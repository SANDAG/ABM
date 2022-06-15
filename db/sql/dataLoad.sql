SET NOCOUNT ON;


-- create data_load schema
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='data_load')
    EXECUTE ('CREATE SCHEMA [data_load]')
    EXECUTE [db_meta].[add_xp] 'data_load', 'MS_Description', 'schema to hold all data load request objects'
GO


-- create data load request table
DROP TABLE IF EXISTS [data_load].[load_request]
CREATE TABLE [data_load].[load_request] (
    [load_request_id] int IDENTITY(1,1) NOT NULL,
    [name] nvarchar(50) NOT NULL,
    [year] int NOT NULL,
    [iteration] int NOT NULL,
    [sample_rate] float NOT NULL,
    [abm_version] nvarchar(50) NOT NULL,
    [path] nvarchar(200) NOT NULL,
    [user_name] nvarchar(100) NOT NULL,
    [load_status] nvarchar(10) NULL,
    [load_status_date] smalldatetime NOT NULL,
    [scenario_id] int NULL,  -- allow nulls,
	[geography_set_id] int NULL DEFAULT 1
    CONSTRAINT pk_scenarioLoadRequest PRIMARY KEY ([load_request_id]),
	CONSTRAINT fk_geography_set_load FOREIGN KEY ([geography_set_id]) REFERENCES [dimension].[geography_set] ([geography_set_id])) 
ON [reference_fg]
WITH (DATA_COMPRESSION = PAGE);
GO

-- add metadata for [data_load].[scenario_load_request]
EXECUTE [db_meta].[add_xp] 'data_load.load_request', 'MS_Description', 'table holding scenarios to be loaded'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.name', 'MS_Description', 'base file path name of scenario'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.year', 'MS_Description', 'scenario year'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.iteration', 'MS_Description', 'scenario iteration to be loaded into database'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.sample_rate', 'MS_Description', 'sample rate of scenario'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.abm_version', 'MS_Description', 'ABM model software version of scenario'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.path', 'MS_Description', 'full UNC file path of scenario'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.user_name', 'MS_Description', 'user who requested scenario be loaded into the database'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.load_status_date', 'MS_Description', 'loading process status of scenario'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.scenario.date', 'MS_Description', 'date and time of load status'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.scenario_id', 'MS_Description', 'scenario surrogate key and scenario identifier'
EXECUTE [db_meta].[add_xp] 'data_load.load_request.geography_set_id', 'MS_Description', 'geography dimension set identifier defaults to 1'
GO


-- create stored procedure that populates data load request table
DROP PROCEDURE IF EXISTS [data_load].[sp_request]
GO
CREATE PROCEDURE [data_load].[sp_request] 
	@year int, @path nvarchar(200), @iteration int, @sample_rate float, @abm_version nvarchar(50), 
	@geo_set_id int = 1  -- 1 is default value 
AS

DECLARE @name nvarchar(50)
DECLARE @network_path nvarchar(200)
SET @name = (SELECT REVERSE(SUBSTRING(REVERSE(@path), 0, CHARINDEX('\',REVERSE(@path)))))
SET @network_path = REPLACE(LOWER(@path), 't:', '\\sandag.org\transdata')

INSERT INTO [data_load].[load_request]
VALUES (@name, @year, @iteration, @sample_rate, @abm_version, @network_path, SYSTEM_USER, 'requested', GETDATE(), NULL, @geo_set_id)
GO

-- add metadata for [data_load].[sp_request]
EXECUTE [db_meta].[add_xp] 'data_load.sp_request', 'MS_Description', 'stored procedure to populate data load request table'
GO
