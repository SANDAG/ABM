/*
Borrowed from leversandturtles presentation and materials:
T:\ABM\user\gks\ABM DataBase\Learning Materials\Metadata\leversandturtles
*/


-- Create db_meta schema
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='db_meta')
BEGIN

EXEC ('CREATE SCHEMA [db_meta]')

EXEC sys.sp_addextendedproperty 
    @name = 'MS_Description'
   ,@value = 'schema for metadata utilities'
   ,@level0type = 'SCHEMA'
   ,@level0name = 'db_meta'
   ,@level1type = NULL
   ,@level1name = NULL
   ,@level2type = NULL
   ,@level2name = NULL

END

GO


-- Create [db_meta].[object_info] function
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[db_meta].[object_info]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
	DROP FUNCTION [db_meta].[object_info]
GO

CREATE FUNCTION db_meta.object_info(@obj varchar(max))
RETURNS TABLE AS RETURN (
	SELECT ObjectSchema = OBJECT_SCHEMA_NAME(o.object_id)
			,ObjectName = OBJECT_NAME(o.object_id)
			,FullObjectName = QUOTENAME(OBJECT_SCHEMA_NAME(o.object_id)) + '.' + QUOTENAME(o.name)
			,ObjectType = o.type_desc
			,IsSchema = CAST(0 AS bit)
			,level0type = 'SCHEMA'
			,level0name = OBJECT_SCHEMA_NAME(o.object_id)
			,level1type = CASE WHEN o.type_desc = 'VIEW' THEN 'VIEW'
								WHEN o.type_desc = 'USER_TABLE' THEN 'TABLE'
								WHEN o.type_desc = 'SQL_STORED_PROCEDURE' THEN 'PROCEDURE'
								WHEN o.type_desc = 'SQL_INLINE_TABLE_VALUED_FUNCTION' THEN 'FUNCTION'
								ELSE o.type_desc
							END
			,level1name = OBJECT_NAME(o.object_id)
			,level2type = NULL
			,level2name = NULL
	FROM sys.objects AS o
	WHERE o.object_id = OBJECT_ID(@obj)

	UNION ALL

	SELECT ObjectSchema = SCHEMA_NAME(SCHEMA_ID(@obj))
		,ObjectName = NULL
		,FullObjectName = QUOTENAME(SCHEMA_NAME(SCHEMA_ID(@obj)))
		,ObjectType = 'SCHEMA'
		,IsSchema = CAST(1 AS bit)
		,level0type = 'SCHEMA'
		,level0name = SCHEMA_NAME(SCHEMA_ID(@obj))
		,level1type = NULL
		,level1name = NULL
		,level2type = NULL
		,level2name = NULL
	WHERE SCHEMA_ID(@obj) IS NOT NULL

	UNION ALL
	
	SELECT ObjectSchema = OBJECT_SCHEMA_NAME(o.object_id)
			,ObjectName = OBJECT_NAME(o.object_id)
			,FullObjectName = QUOTENAME(OBJECT_SCHEMA_NAME(o.object_id)) + '.' + QUOTENAME(o.name)
			,ObjectType = 'COLUMN'
			,IsSchema = CAST(0 AS bit)
			,level0type = 'SCHEMA'
			,level0name = OBJECT_SCHEMA_NAME(o.object_id)
			,level1type = 'TABLE'
			,level1name = OBJECT_NAME(o.object_id)
			,level2type = 'COLUMN'
			,level2name = PARSENAME(@obj, 1)
	FROM sys.objects AS o
	WHERE PARSENAME(@obj, 1) IS NOT NULL
		AND PARSENAME(@obj, 2) IS NOT NULL
		AND PARSENAME(@obj, 3) IS NOT NULL
		AND PARSENAME(@obj, 4) IS NULL
		AND o.object_id = OBJECT_ID(QUOTENAME(PARSENAME(@obj, 3)) + '.' + QUOTENAME(PARSENAME(@obj, 2)))
)
GO

EXEC sys.sp_addextendedproperty 
	@name = 'MS_Description'
	,@value = 'return xp friendly object types'
	,@level0type = 'SCHEMA'
	,@level0name = 'db_meta'
	,@level1type = 'FUNCTION'
	,@level1name = 'object_info'
	,@level2type = NULL
	,@level2name = NULL
GO
EXEC sys.sp_addextendedproperty 
	@name = 'SUBSYSTEM'
	,@value = 'meta'
	,@level0type = 'SCHEMA'
	,@level0name = 'db_meta'
	,@level1type = 'FUNCTION'
	,@level1name = 'object_info'
	,@level2type = NULL
	,@level2name = NULL
GO


-- Create [db_meta].[add_xp] function
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[db_meta].[add_xp]') AND type in (N'P', N'PC'))
	DROP PROCEDURE [db_meta].[add_xp]
GO

CREATE PROCEDURE db_meta.add_xp
	@obj AS varchar(max)
	,@name AS sysname
	,@value AS varchar(7500)
AS
BEGIN
	DECLARE @level0type AS varchar(128)
	DECLARE @level0name AS sysname
	DECLARE @level1type AS varchar(128)
	DECLARE @level1name AS sysname
	DECLARE @level2type AS varchar(128)
	DECLARE @level2name AS sysname
	
	SELECT @level0type = level0type
		,@level0name = level0name
		,@level1type = CASE WHEN level1type = 'SQL_TABLE_VALUED_FUNCTION' THEN 'FUNCTION' ELSE level1type END
		,@level1name = level1name
		,@level2type = level2type
		,@level2name = level2name
	FROM db_meta.object_info(@obj)

	EXEC sys.sp_addextendedproperty 
		@name = @name
		,@value = @value
		,@level0type = @level0type
		,@level0name = @level0name
		,@level1type = @level1type
		,@level1name = @level1name
		,@level2type = @level2type
		,@level2name = @level2name
END
GO

--EXEC sys.sp_addextendedproperty 
--	@name = 'MS_Description'
--	,@value = 'Procedure to make sys.extended_properties easier to use'
--	,@level0type = 'SCHEMA'
--	,@level0name = 'db_meta'
--	,@level1type = 'PROCEDURE'
--	,@level1name = 'add_xp'
--	,@level2type = NULL
--	,@level2name = NULL

EXEC db_meta.add_xp 'db_meta.add_xp', 'MS_Description', 'Procedure to make sys.extended_properties easier to use'
GO
EXEC db_meta.add_xp 'db_meta.add_xp', 'SUBSYSTEM', 'META'
GO


-- Create [db_meta].[update_xp] function
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[db_meta].[update_xp]') AND type in (N'P', N'PC'))
	DROP PROCEDURE [db_meta].[update_xp]
GO

CREATE PROCEDURE db_meta.update_xp
	@obj AS varchar(max)
	,@name AS sysname
	,@value AS varchar(7500)
AS
BEGIN
	DECLARE @level0type AS varchar(128)
	DECLARE @level0name AS sysname
	DECLARE @level1type AS varchar(128)
	DECLARE @level1name AS sysname
	DECLARE @level2type AS varchar(128)
	DECLARE @level2name AS sysname
	
	SELECT @level0type = level0type
		,@level0name = level0name
		,@level1type = CASE WHEN level1type = 'SQL_TABLE_VALUED_FUNCTION' THEN 'FUNCTION' ELSE level1type END
		,@level1name = level1name
		,@level2type = level2type
		,@level2name = level2name
	FROM db_meta.object_info(@obj)

	EXEC sys.sp_updateextendedproperty 
		@name = @name
		,@value = @value
		,@level0type = @level0type
		,@level0name = @level0name
		,@level1type = @level1type
		,@level1name = @level1name
		,@level2type = @level2type
		,@level2name = @level2name
END
GO

EXEC db_meta.add_xp 'db_meta.update_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to use'
GO
EXEC db_meta.update_xp 'db_meta.update_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to update'
GO
EXEC db_meta.add_xp 'db_meta.update_xp', 'SUBSYSTEM', 'meta'
GO




-- Create [db_meta].[drop_xp] function
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[db_meta].[drop_xp]') AND type in (N'P', N'PC'))
	DROP PROCEDURE [db_meta].[drop_xp]
GO

CREATE PROCEDURE db_meta.drop_xp
	@obj AS varchar(max)
	,@name AS sysname
AS
BEGIN
	DECLARE @level0type AS varchar(128)
	DECLARE @level0name AS sysname
	DECLARE @level1type AS varchar(128)
	DECLARE @level1name AS sysname
	DECLARE @level2type AS varchar(128)
	DECLARE @level2name AS sysname
	
	SELECT @level0type = level0type
		,@level0name = level0name
		,@level1type = CASE WHEN level1type = 'SQL_TABLE_VALUED_FUNCTION' THEN 'FUNCTION' ELSE level1type END
		,@level1name = level1name
		,@level2type = level2type
		,@level2name = level2name
	FROM db_meta.object_info(@obj)

	EXEC sys.sp_dropextendedproperty 
		@name = @name
		,@level0type = @level0type
		,@level0name = @level0name
		,@level1type = @level1type
		,@level1name = @level1name
		,@level2type = @level2type
		,@level2name = @level2name
END
GO

EXEC db_meta.add_xp 'db_meta.drop_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to use'
GO
EXEC db_meta.drop_xp 'db_meta.drop_xp', 'MS_Description'
GO
EXEC db_meta.add_xp 'db_meta.drop_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to drop'
GO
EXEC db_meta.add_xp 'db_meta.drop_xp', 'SUBSYSTEM', 'meta'
GO


-- Create View for extended properties
IF  EXISTS (SELECT * FROM sys.views WHERE object_id = OBJECT_ID(N'[db_meta].[data_dictionary]'))
	DROP VIEW [db_meta].[data_dictionary]
GO

CREATE VIEW db_meta.data_dictionary AS
	SELECT 'SCHEMA' AS ObjectType
			,QUOTENAME(s.name) AS FullObjectName
			,s.name as ObjectSchema
			,NULL AS ObjectName
			,NULL AS SubName
			,xp.name AS PropertyName
			,xp.value AS PropertyValue
	FROM sys.extended_properties AS xp
	INNER JOIN sys.schemas AS s
		ON s.schema_id = xp.major_id
		AND xp.class_desc = 'SCHEMA'

	UNION ALL

	SELECT o.type_desc AS ObjectType
			,QUOTENAME(OBJECT_SCHEMA_NAME(o.object_id)) + '.' + QUOTENAME(o.name) AS FullObjectName
			,OBJECT_SCHEMA_NAME(o.object_id) AS ObjectSchema
			,o.name AS ObjectName
			,NULL AS SubName
			,xp.name AS PropertyName
			,xp.value AS PropertyValue
	FROM sys.extended_properties AS xp
	INNER JOIN sys.objects AS o
		ON o.object_id = xp.major_id
		AND xp.minor_id = 0
		AND xp.class_desc = 'OBJECT_OR_COLUMN'

	UNION ALL

	SELECT 'COLUMN' AS ObjectType
			,QUOTENAME(OBJECT_SCHEMA_NAME(t.object_id)) + '.' + QUOTENAME(t.name) + '.' + QUOTENAME(c.name) AS FullObjectName
			,OBJECT_SCHEMA_NAME(t.object_id) AS ObjectSchema
			,t.name AS ObjectName
			,c.name AS SubName
			,xp.name AS PropertyName
			,xp.value AS PropertyValue
	FROM sys.extended_properties AS xp
	INNER JOIN sys.objects AS t
		ON t.object_id = xp.major_id
		AND xp.minor_id <> 0
		AND xp.class_desc = 'OBJECT_OR_COLUMN'
		AND t.type_desc = 'USER_TABLE'
	INNER JOIN sys.columns AS c
		ON c.object_id = t.object_id
		AND c.column_id = xp.minor_id
		
GO
EXEC db_meta.add_xp 'db_meta.data_dictionary', 'MS_Description', 'view to see extended properties of database objects'
GO
EXEC db_meta.add_xp 'db_meta.data_dictionary', 'SUBSYSTEM', 'meta'
GO


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[db_meta].[schema_change_log]') AND type in (N'U'))
DROP TABLE [db_meta].[schema_change_log]
GO

CREATE TABLE [db_meta].[schema_change_log] (
	[id] [smallint] IDENTITY(1,1) NOT NULL,
	[major_release_no] [nchar](2) NOT NULL,
	[minor_release_no] [nchar](2) NOT NULL,
	[point_release_no] [nchar](4) NOT NULL,
	[description] [nchar](100) NOT NULL,
	[date_applied] [datetime] NOT NULL,
	CONSTRAINT [pk_schema_change_log] PRIMARY KEY CLUSTERED ([id])
	) 
ON 
	[reference_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
GO


-- Add metadata to schema change log
EXEC db_meta.add_xp 'db_meta.schema_change_log', 'MS_Description', 'database version and changes tracking table'
GO
EXEC db_meta.add_xp 'db_meta.schema_change_log', 'SUBSYSTEM', 'meta'
GO
