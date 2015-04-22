-- Create ref schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='ref')
BEGIN

EXEC ('CREATE SCHEMA [ref]')
	
END


-- Create abm Schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='abm')
BEGIN

EXEC ('CREATE SCHEMA [abm]')

END


-- Create abm_staging schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='abm_staging')
BEGIN
	EXEC (N'CREATE SCHEMA [abm_staging]')
END