SET NOCOUNT ON;


-- fill mgra series 13 shape column
UPDATE
    [dimension].[geography]
SET
    [dimension].[geography].[mgra_13_shape] = [shapes].[shape]
FROM
    [dimension].[geography]
INNER JOIN (
    SELECT
        [zone]
        ,[shape]
     FROM
        [data_cafe].[ref].[geography_zone] -- geography zone reference table in [data_cafe] reference database
     INNER JOIN
        [data_cafe].[ref].[geography_type] -- geography type reference table in [data_cafe] reference database
     ON
        [geography_zone].[geography_type_id] = [geography_type].[geography_type_id]
    WHERE
        [geography_type].[alias] = 'mgra_13') AS [shapes]
    ON
        [geography].[mgra_13] = CONVERT(nchar, [shapes].[zone])
    WHERE
        [geography].[mgra_13] != 'Not Applicable'
GO


-- fill taz series 13 shape column
UPDATE
    [dimension].[geography]
SET
    [dimension].[geography].[taz_13_shape] = [shapes].[shape]
FROM
    [dimension].[geography]
INNER JOIN (
    SELECT
        [zone]
        ,[shape]
    FROM
        [data_cafe].[ref].[geography_zone] -- geography zone reference table in [data_cafe] reference database
    INNER JOIN
        [data_cafe].[ref].[geography_type] -- geography type reference table in [data_cafe] reference database
    ON
        [geography_zone].[geography_type_id] = [geography_type].[geography_type_id]
    WHERE
        [geography_type].[alias] = 'taz_13') AS [shapes]
    ON
        [geography].[taz_13] = CONVERT(nchar, [shapes].[zone])
    WHERE
        [geography].[taz_13] != 'Not Applicable'
GO
