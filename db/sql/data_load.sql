SET NOCOUNT ON;

-- Create data_load schema
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='data_load')
EXEC ('CREATE SCHEMA [data_load]')
GO


-- Add metadata for [data_load]
IF EXISTS(SELECT * FROM [db_meta].[data_dictionary] WHERE [ObjectType] = 'SCHEMA' AND [FullObjectName] = '[data_load]' AND [PropertyName] = 'MS_Description')
EXECUTE [db_meta].[drop_xp] 'data_load', 'MS_Description'

EXECUTE [db_meta].[add_xp] 'data_load', 'MS_Description', 'schema to hold all data load request objects'
GO


-- Create data load request table
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[data_load].[load_request]') AND type in (N'U'))
DROP TABLE [data_load].[load_request]
GO

CREATE TABLE [data_load].[load_request] (
	[load_request_id] [int] IDENTITY(1,1) NOT NULL,
	[year] [smallint] NOT NULL,
	[name] [nchar](50) NOT NULL,
	[path] [nchar](200) NOT NULL,
	[iteration] [tinyint] NOT NULL,
	[sample_rate] [decimal](6,4) NOT NULL,
	[abm_version] [nchar](50) NOT NULL,
	[user_name] [nchar](50) NOT NULL,
	[date_requested] [smalldatetime] NOT NULL,
	[loading] [bit] NULL,
	[loading_failed] [bit] NULL,
	[scenario_id] [int] NULL,
	CONSTRAINT pk_scenarioloadrequest PRIMARY KEY ([load_request_id])
	) 
ON 
	[reference_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
GO


-- Add metadata for [data_load].[scenario_load_request]
EXECUTE [db_meta].[add_xp] 'data_load.load_request', 'SUBSYSTEM', 'data_load'
EXECUTE [db_meta].[add_xp] 'data_load.load_request', 'MS_Description', 'table holding scenarios to be loaded'
GO


-- Create stored procedure that populates data load request table
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[data_load].[sp_request]') AND type in (N'P', N'PC'))
DROP PROCEDURE [data_load].[sp_request]
GO

CREATE PROCEDURE [data_load].[sp_request] 
	@year smallint, @path nvarchar(200), @iteration tinyint, @sample_rate decimal(6,4) = 1, @abm_version nvarchar(50) = 'pre 13.2.4'
AS

DECLARE @name nvarchar(50)
DECLARE @network_path nvarchar(200)
SET @name = (SELECT REVERSE(SUBSTRING(REVERSE(@path), 0, CHARINDEX('\',REVERSE(@path)))))
SET @network_path = REPLACE(LOWER(@path), 't:', '\\sandag.org\transdata')

INSERT INTO [data_load].[load_request]
VALUES (@year, @name, @network_path, @iteration, @sample_rate, @abm_version, SYSTEM_USER, GETDATE(), 0, 0, NULL)
GO


-- Add metadata for [data_load].[sp_request]
EXECUTE [db_meta].[add_xp] 'data_load.sp_request', 'SUBSYSTEM', 'data_load'
EXECUTE [db_meta].[add_xp] 'data_load.sp_request', 'MS_Description', 'stored procedure to populate data load request table'
GO
