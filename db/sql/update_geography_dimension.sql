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


-- fill luz series 13 shape column
UPDATE
	[dimension].[geography]
SET
	[dimension].[geography].[luz_13_shape] = [shapes].[shape]
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
		[geography_type].[alias] = 'luz_13') AS [shapes]
ON
    [geography].[luz_13] = CONVERT(nchar, [shapes].[zone])
WHERE
	[geography].[luz_13] != 'Not Applicable'
GO


-- fill cicpa 2016 shape column
UPDATE
	[dimension].[geography]
SET
	[dimension].[geography].[cicpa_2016_shape] = [shapes].[shape]
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
		[geography_type].[alias] = 'cicpa_2016') AS [shapes]
ON
    [geography].[cicpa_2016] = CONVERT(nchar, [shapes].[zone])
WHERE
	[geography].[cicpa_2016] != 'Not Applicable'
GO


 -- fill cocpa 2016 shape column
UPDATE
	[dimension].[geography]
SET
	[dimension].[geography].[cocpa_2016_shape] = [shapes].[shape]
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
		[geography_type].[alias] = 'cocpa_2016') AS [shapes]
ON
    [geography].[cocpa_2016] = CONVERT(nchar, [shapes].[zone])
WHERE
	[geography].[cocpa_2016] != 'Not Applicable'
GO


 -- fill jurisdiction 2016 shape column
UPDATE
	[dimension].[geography]
SET
	[dimension].[geography].[jurisdiction_2016_shape] = [shapes].[shape]
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
		[geography_type].[alias] = 'jurisdiction_2016') AS [shapes]
ON
    [geography].[jurisdiction_2016] = CONVERT(nchar, [shapes].[zone])
WHERE
	[geography].[jurisdiction_2016] != 'Not Applicable'
GO


-- fill region 2004 shape column
UPDATE
	[dimension].[geography]
SET
	[dimension].[geography].[region_2004_shape] = [shapes].[shape]
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
		[geography_type].[alias] = 'region_2004') AS [shapes]
ON
    [geography].[region_2004] = CONVERT(nchar, [shapes].[zone])
WHERE
	[geography].[region_2004] != 'Not Applicable'
GO