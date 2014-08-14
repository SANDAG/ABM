SET NOCOUNT ON;

-- Create data_load schema
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='data_load')
EXEC ('CREATE SCHEMA [data_load]')
GO


-- Add metadata for [data_load]
IF EXISTS(SELECT * FROM [db_meta].[data_dictionary] WHERE [ObjectType] = 'SCHEMA' AND [FullObjectName] = '[data_load]' AND [PropertyName] = 'MS_Description')
EXECUTE [db_meta].[drop_xp] 'data_load', 'MS_Description'

EXECUTE [db_meta].[add_xp] 'data_load', 'MS_Description', 'Schema to hold all data load request objects'
GO


-- Create data load request table
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[data_load].[scenario_load_request]') AND type in (N'U'))
DROP TABLE [data_load].[scenario_load_request]
GO

CREATE TABLE [data_load].[scenario_load_request] (
	[scenario_year] [smallint] NOT NULL,
	[scenario_desc] [varchar](50) NOT NULL,
	[path] [varchar](200) NOT NULL,
	[iteration] [tinyint] NOT NULL,
	[user] [varchar](50) NOT NULL,
	[date_requested] [smalldatetime] NOT NULL,
	[loading] [tinyint] NULL
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
GO


-- Add metadata for [data_load].[scenario_load_request]
EXECUTE [db_meta].[add_xp] 'data_load.scenario_load_request', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'data_load.scenario_load_request', 'MS_Description', 'Table holding scenarios to be loaded'
GO


-- Create stored procedure that populates data load request table
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[data_load].[sp_request]') AND type in (N'P', N'PC'))
DROP PROCEDURE [data_load].[sp_request]
GO

CREATE PROCEDURE [data_load].[sp_request] 
	@year smallint, @path varchar(200), @iteration tinyint
AS

DECLARE @desc nvarchar(50);
SET @desc = (SELECT REVERSE(SUBSTRING(REVERSE(@path), 0, CHARINDEX('\',REVERSE(@path)))))

INSERT INTO [data_load].[scenario_load_request]
VALUES (@year, @desc, @path, @iteration, SYSTEM_USER, GETDATE(), 0)
GO


-- Add metadata for [data_load].[sp_request]
EXECUTE [db_meta].[add_xp] 'data_load.sp_request', 'SUBSYSTEM', 'ABM'
EXECUTE [db_meta].[add_xp] 'data_load.sp_request', 'MS_Description', 'Stored procedure to populate data load request table'
GO