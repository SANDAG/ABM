/*
Borrowed from leversandturtles presentation and materials:
https://code.google.com/archive/p/caderoux/
*/


-- create the db_meta schema and add description
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='db_meta')
BEGIN

    EXECUTE ('CREATE SCHEMA [db_meta]')

    EXECUTE [sys].[sp_addextendedproperty]
        @name = 'MS_Description'
        ,@value = 'schema for metadata utilities'
        ,@level0type = 'SCHEMA'
        ,@level0name = 'db_meta'

END
GO


-- create [db_meta].[object_info] function
DROP FUNCTION IF EXISTS [db_meta].[object_info]
GO

CREATE FUNCTION [db_meta].[object_info]( @obj varchar(max) )
RETURNS TABLE AS RETURN (
    SELECT
        OBJECT_SCHEMA_NAME([object_id]) AS [ObjectSchema]
        ,OBJECT_NAME([object_id]) AS [ObjectName]
        ,QUOTENAME(OBJECT_SCHEMA_NAME([object_id])) + '.' + QUOTENAME([name]) AS [FullObjectName]
        ,[type_desc] AS [ObjectType]
        ,CAST(0 AS bit) AS [IsSchema]
        ,'SCHEMA' AS [level0type]
        ,OBJECT_SCHEMA_NAME([object_id]) AS [level0name]
        ,CASE   WHEN [type_desc] = 'VIEW' THEN 'VIEW'
                WHEN [type_desc] = 'USER_TABLE' THEN 'TABLE'
                WHEN [type_desc] = 'SQL_STORED_PROCEDURE' THEN 'PROCEDURE'
                WHEN [type_desc] = 'SQL_INLINE_TABLE_VALUED_FUNCTION' THEN 'FUNCTION'
                ELSE [type_desc] END AS [level1type]
        ,OBJECT_NAME([object_id]) AS [level1name]
        ,NULL AS [level2type]
        ,NULL AS [level2name]
	FROM
        [sys].[objects]
	WHERE 
        [object_id] = OBJECT_ID(@obj)

    UNION ALL

    SELECT
        SCHEMA_NAME(SCHEMA_ID(@obj)) AS [ObjectSchema]
        ,NULL AS [ObjectName]
        ,QUOTENAME(SCHEMA_NAME(SCHEMA_ID(@obj))) AS [FullObjectName]
        ,'SCHEMA' AS [ObjectType]
        ,CAST(1 AS bit) AS [IsSchema]
        ,'SCHEMA' AS [level0type]
        ,SCHEMA_NAME(SCHEMA_ID(@obj)) AS [level0name]
        ,NULL AS [level1type]
        ,NULL AS [level1name]
        ,NULL AS [level2type]
        ,NULL AS [level2name]
    WHERE
        SCHEMA_ID(@obj) IS NOT NULL

    UNION ALL

    SELECT
        OBJECT_SCHEMA_NAME([object_id]) AS [ObjectSchema]
        ,OBJECT_NAME([object_id]) AS [ObjectName]
        ,QUOTENAME(OBJECT_SCHEMA_NAME([object_id])) + '.' + QUOTENAME([name]) AS [FullObjectName]
        ,'COLUMN' AS [ObjectType]
        ,CAST(0 AS bit) AS [IsSchema]
        ,'SCHEMA' AS [level0type]
        ,OBJECT_SCHEMA_NAME([object_id]) AS [level0name]
        ,'TABLE' AS [level1type]
        ,OBJECT_NAME([object_id]) AS [level1name]
        ,'COLUMN' AS [level2type]
        ,PARSENAME(@obj, 1) AS [level2name]
    FROM
        [sys].[objects]
    WHERE
        PARSENAME(@obj, 1) IS NOT NULL
        AND PARSENAME(@obj, 2) IS NOT NULL
        AND PARSENAME(@obj, 3) IS NOT NULL
        AND PARSENAME(@obj, 4) IS NULL
        AND [object_id] = OBJECT_ID(QUOTENAME(PARSENAME(@obj, 3)) + '.' + QUOTENAME(PARSENAME(@obj, 2)))
)
GO

EXECUTE [sys].[sp_addextendedproperty]
	@name = 'MS_Description'
	,@value = 'return xp friendly object types'
	,@level0type = 'SCHEMA'
	,@level0name = 'db_meta'
	,@level1type = 'FUNCTION'
	,@level1name = 'object_info'
GO


-- create [db_meta].[add_xp] function
DROP PROCEDURE IF EXISTS [db_meta].[add_xp]
GO

CREATE PROCEDURE [db_meta].[add_xp]
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
	
    SELECT
        @level0type = [level0type]
        ,@level0name = [level0name]
        ,@level1type = CASE WHEN [level1type] = 'SQL_TABLE_VALUED_FUNCTION'
                            THEN 'FUNCTION'
                            ELSE [level1type]
                            END
        ,@level1name = [level1name]
        ,@level2type = [level2type]
        ,@level2name = [level2name]
    FROM
        [db_meta].[object_info]( @obj )

	EXECUTE [sys].[sp_addextendedproperty] 
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

EXECUTE [db_meta].[add_xp] 'db_meta.add_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to use'
GO


-- create [db_meta].[drop_xp] function
DROP PROCEDURE IF EXISTS [db_meta].[drop_xp]
GO

CREATE PROCEDURE [db_meta].[drop_xp]
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
	
    SELECT
        @level0type = [level0type]
        ,@level0name = [level0name]
        ,@level1type = CASE WHEN [level1type] = 'SQL_TABLE_VALUED_FUNCTION'
                            THEN 'FUNCTION' 
                            ELSE [level1type]
                            END
        ,@level1name = [level1name]
        ,@level2type = [level2type]
        ,@level2name = [level2name]
    FROM
        [db_meta].[object_info]( @obj )

    EXECUTE [sys].[sp_dropextendedproperty] 
        @name = @name
        ,@level0type = @level0type
        ,@level0name = @level0name
        ,@level1type = @level1type
        ,@level1name = @level1name
        ,@level2type = @level2type
        ,@level2name = @level2name
END
GO

EXEC [db_meta].[add_xp] 'db_meta.drop_xp', 'MS_Description', 'procedure to make sys.extended_properties easier to drop'
GO


-- create view for extended properties
DROP VIEW IF EXISTS [db_meta].[data_dictionary]
GO

CREATE VIEW [db_meta].[data_dictionary] AS
    SELECT
        'SCHEMA' AS [ObjectType]
        ,QUOTENAME([s].[name]) AS [FullObjectName]
        ,[s].[name] AS [ObjectSchema]
        ,NULL AS [ObjectName]
        ,NULL AS [SubName]
        ,[xp].[name] AS [PropertyName]
        ,[xp].[value] AS [PropertyValue]
    FROM
        [sys].[extended_properties] AS [xp]
    INNER JOIN
        [sys].[schemas] AS [s]
    ON
        [s].[schema_id] = [xp].[major_id]
        AND [xp].[class_desc] = 'SCHEMA'

    UNION ALL

    SELECT
        [o].[type_desc] AS [ObjectType]
        ,QUOTENAME(OBJECT_SCHEMA_NAME([o].[object_id])) + '.' + QUOTENAME([o].[name]) AS [FullObjectName]
        ,OBJECT_SCHEMA_NAME([o].[object_id]) AS [ObjectSchema]
        ,[o].[name] AS [ObjectName]
        ,NULL AS [SubName]
        ,[xp].[name] AS [PropertyName]
        ,[xp].[value] AS [PropertyValue]
    FROM
        [sys].[extended_properties] AS [xp]
    INNER JOIN
        [sys].[objects] AS [o]
    ON 
        [o].[object_id] = [xp].[major_id]
        AND [xp].[minor_id] = 0
        AND [xp].[class_desc] = 'OBJECT_OR_COLUMN'

    UNION ALL

    SELECT
        'COLUMN' AS [ObjectType]
        ,QUOTENAME(OBJECT_SCHEMA_NAME([o].[object_id])) + '.' + QUOTENAME([o].[name]) + '.' + QUOTENAME([c].[name]) AS [FullObjectName]
        ,OBJECT_SCHEMA_NAME([o].[object_id]) AS [ObjectSchema]
        ,[o].[name] AS [ObjectName]
        ,[c].[name] AS [SubName]
        ,[xp].[name] AS [PropertyName]
        ,[xp].[value] AS [PropertyValue]
    FROM
        [sys].[extended_properties] AS [xp]
    INNER JOIN 
        [sys].[objects] AS [o]
    ON
        [o].[object_id] = [xp].[major_id]
        AND [xp].[minor_id] <> 0
        AND [xp].[class_desc] = 'OBJECT_OR_COLUMN'
        AND [o].[type_desc] = 'USER_TABLE'
    INNER JOIN 
        sys.columns AS [c]
    ON
        [c].[object_id] = [o].[object_id]
        AND [c].[column_id] = [xp].[minor_id]	
GO

EXECUTE [db_meta].[add_xp] 'db_meta.data_dictionary', 'MS_Description', 'view to see extended properties of database objects'
GO


DROP TABLE IF EXISTS [db_meta].[schema_change_log]
CREATE TABLE [db_meta].[schema_change_log] (
    [id] int IDENTITY(1,1) NOT NULL,
    [major_release_no] nvarchar(2) NOT NULL,
    [minor_release_no] nvarchar(2) NOT NULL,
    [point_release_no] nvarchar(4) NOT NULL,
    [description] nvarchar(max) NOT NULL,
    [sql] nvarchar(max) NOT NULL,
    [date_applied] datetime NOT NULL,
    CONSTRAINT [pk_schemaChangeLog] PRIMARY KEY CLUSTERED ([id])) 
ON 
    [reference_fg]
WITH (DATA_COMPRESSION = PAGE);
GO


-- add metadata to schema change log table
EXECUTE [db_meta].[add_xp] 'db_meta.schema_change_log', 'MS_Description', 'database version and changes tracking table'
GO
