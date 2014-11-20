USE [master]
GO

IF  EXISTS (SELECT name FROM sys.databases WHERE name = $(db_name_string))
DROP DATABASE $(db_name)
GO

USE [master]
GO

CREATE DATABASE $(db_name) ON  PRIMARY 
( NAME = N'$(db_name)_primary', FILENAME = $(mdf) , SIZE = 5GB , MAXSIZE = 5GB , FILEGROWTH = 0)
 LOG ON 
( NAME = N'$(db_name)_log', FILENAME = $(ldf) , SIZE = 25GB , MAXSIZE = 25GB , FILEGROWTH = 0 )
GO

ALTER DATABASE $(db_name) SET COMPATIBILITY_LEVEL = 120
GO

IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC $(db_name).[dbo].[sp_fulltext_database] @action = 'enable'
end
GO

ALTER DATABASE $(db_name) SET ANSI_NULL_DEFAULT OFF 
GO

ALTER DATABASE $(db_name) SET ANSI_NULLS OFF 
GO

ALTER DATABASE $(db_name) SET ANSI_PADDING OFF 
GO

ALTER DATABASE $(db_name) SET ANSI_WARNINGS OFF 
GO

ALTER DATABASE $(db_name) SET ARITHABORT OFF 
GO

ALTER DATABASE $(db_name) SET AUTO_CLOSE OFF 
GO

ALTER DATABASE $(db_name) SET AUTO_CREATE_STATISTICS ON (INCREMENTAL = ON) -- incremental statistics are very important for the abm database
GO

ALTER DATABASE $(db_name) SET AUTO_SHRINK OFF 
GO

ALTER DATABASE $(db_name) SET AUTO_UPDATE_STATISTICS ON 
GO

ALTER DATABASE $(db_name) SET CURSOR_CLOSE_ON_COMMIT OFF 
GO

ALTER DATABASE $(db_name) SET CURSOR_DEFAULT  GLOBAL 
GO

ALTER DATABASE $(db_name) SET CONCAT_NULL_YIELDS_NULL OFF 
GO

ALTER DATABASE $(db_name) SET NUMERIC_ROUNDABORT OFF 
GO

ALTER DATABASE $(db_name) SET QUOTED_IDENTIFIER OFF 
GO

ALTER DATABASE $(db_name) SET RECURSIVE_TRIGGERS OFF 
GO

ALTER DATABASE $(db_name) SET  DISABLE_BROKER 
GO

ALTER DATABASE $(db_name) SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO

ALTER DATABASE $(db_name) SET DATE_CORRELATION_OPTIMIZATION OFF 
GO

ALTER DATABASE $(db_name) SET TRUSTWORTHY OFF 
GO

ALTER DATABASE $(db_name) SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO

ALTER DATABASE $(db_name) SET PARAMETERIZATION SIMPLE 
GO

ALTER DATABASE $(db_name) SET READ_COMMITTED_SNAPSHOT OFF 
GO

ALTER DATABASE $(db_name) SET HONOR_BROKER_PRIORITY OFF 
GO

ALTER DATABASE $(db_name) SET  READ_WRITE 
GO

ALTER DATABASE $(db_name) SET RECOVERY SIMPLE 
GO

ALTER DATABASE $(db_name) SET  MULTI_USER 
GO

ALTER DATABASE $(db_name) SET PAGE_VERIFY CHECKSUM  
GO

ALTER DATABASE $(db_name) SET DB_CHAINING OFF 
GO


USE $(db_name);

-- ===================================
-- Create Partition Scheme
-- ===================================

-- Step 1. Add filegroups
-- Scenario 0 is placeholder for merges/splits
-- All scenarios have filegroup size at 45GB
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE name = N'scenario_fg_1')
BEGIN

ALTER DATABASE 
	$(db_name)
ADD FILEGROUP 
	scenario_fg_1;

ALTER DATABASE 
	$(db_name)
ADD FILE
(
	NAME = scenario_file_1_1,
	FILENAME = $(scen_file_string_1),
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0
	)
TO 
	FILEGROUP scenario_fg_1;

ALTER DATABASE 
	$(db_name)
ADD FILE
(
	NAME = scenario_file_1_2,
	FILENAME = $(scen_file_string_2),
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0
	)
TO 
	FILEGROUP scenario_fg_1;

ALTER DATABASE 
	$(db_name)
ADD FILE
(
	NAME = scenario_file_1_3,
	FILENAME = $(scen_file_string_3),
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0
	)
TO 
	FILEGROUP scenario_fg_1;

END


-- Step 2. Create partition function
IF NOT EXISTS (SELECT name FROM sys.partition_functions WHERE name = N'scenario_partition')
BEGIN
CREATE PARTITION FUNCTION 
	scenario_partition (smallint)
AS
	RANGE RIGHT FOR VALUES (1)
	-- (x < 1, 1 <= x)
END
GO


-- Step 3. Create partition scheme that references the function
IF NOT EXISTS (SELECT name FROM sys.partition_schemes WHERE name = N'scenario_scheme')
BEGIN
CREATE PARTITION SCHEME 
	scenario_scheme
AS 
	PARTITION 
		scenario_partition 
	TO 
		(scenario_fg_1,scenario_fg_1)
END


GO


-- Create filegroup for ref, non-partitioned tables to sit on
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE name = N'ref_fg')
BEGIN

ALTER DATABASE 
	$(db_name)
ADD 
	FILEGROUP ref_fg;

ALTER DATABASE 
	$(db_name)
ADD FILE
(
	NAME = ref_file,
	FILENAME = $(ref_file_string),
	SIZE = 5GB,
	MAXSIZE = 5GB,
	FILEGROWTH = 0
	)
TO 
	FILEGROUP ref_fg
	
END
