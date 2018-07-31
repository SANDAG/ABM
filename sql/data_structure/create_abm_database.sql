USE [master]
GO

-- create database primary filegroup and log
CREATE DATABASE $(db_name) ON  PRIMARY
( NAME = N'$(db_name)_primary',
  FILENAME = N'$(db_path)$(db_name)_primary.ndf',
  SIZE = 5GB ,
  MAXSIZE = 5GB ,
  FILEGROWTH = 0GB)
LOG ON
( NAME = N'$(db_name)_log',
  FILENAME = N'$(log_path)$(db_name)_log.ldf',
  SIZE = 25GB ,
  MAXSIZE = 25GB ,
  FILEGROWTH = 0GB)
GO

-- set compatibility to sql server 2016
ALTER DATABASE $(db_name) SET COMPATIBILITY_LEVEL = 130
GO

-- set database options
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
BEGIN
EXEC $(db_name).[dbo].[sp_fulltext_database] @action = 'enable'
END
ALTER DATABASE $(db_name) SET ANSI_NULL_DEFAULT OFF
ALTER DATABASE $(db_name) SET ANSI_NULLS OFF
ALTER DATABASE $(db_name) SET ANSI_PADDING OFF
ALTER DATABASE $(db_name) SET ANSI_WARNINGS OFF
ALTER DATABASE $(db_name) SET ARITHABORT OFF
ALTER DATABASE $(db_name) SET AUTO_CLOSE OFF

-- incremental statistics on
ALTER DATABASE $(db_name) SET AUTO_CREATE_STATISTICS ON (INCREMENTAL = ON)

ALTER DATABASE $(db_name) SET AUTO_SHRINK OFF
ALTER DATABASE $(db_name) SET AUTO_UPDATE_STATISTICS ON
ALTER DATABASE $(db_name) SET CURSOR_CLOSE_ON_COMMIT OFF
ALTER DATABASE $(db_name) SET CURSOR_DEFAULT  GLOBAL
ALTER DATABASE $(db_name) SET CONCAT_NULL_YIELDS_NULL OFF
ALTER DATABASE $(db_name) SET NUMERIC_ROUNDABORT OFF
ALTER DATABASE $(db_name) SET QUOTED_IDENTIFIER OFF
ALTER DATABASE $(db_name) SET RECURSIVE_TRIGGERS OFF
ALTER DATABASE $(db_name) SET  DISABLE_BROKER
ALTER DATABASE $(db_name) SET AUTO_UPDATE_STATISTICS_ASYNC OFF
ALTER DATABASE $(db_name) SET DATE_CORRELATION_OPTIMIZATION OFF
ALTER DATABASE $(db_name) SET TRUSTWORTHY OFF
ALTER DATABASE $(db_name) SET ALLOW_SNAPSHOT_ISOLATION OFF
ALTER DATABASE $(db_name) SET PARAMETERIZATION SIMPLE
ALTER DATABASE $(db_name) SET READ_COMMITTED_SNAPSHOT OFF
ALTER DATABASE $(db_name) SET HONOR_BROKER_PRIORITY OFF
ALTER DATABASE $(db_name) SET  READ_WRITE
ALTER DATABASE $(db_name) SET RECOVERY SIMPLE
ALTER DATABASE $(db_name) SET  MULTI_USER
ALTER DATABASE $(db_name) SET PAGE_VERIFY CHECKSUM
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
-- add initial scenario filegroup
ALTER DATABASE $(db_name) ADD FILEGROUP scenario_fg_1;

-- add three files to the filegroup
ALTER DATABASE $(db_name) ADD FILE (
	NAME = scenario_file_1_1,
	FILENAME = N'$(db_path)scenario_file_1_1.ndf',
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0GB) TO FILEGROUP scenario_fg_1;

-- add three files to the filegroup
ALTER DATABASE $(db_name) ADD FILE (
	NAME = scenario_file_1_2,
	FILENAME = N'$(db_path)scenario_file_1_2.ndf',
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0GB) TO FILEGROUP scenario_fg_1;

-- add three files to the filegroup
ALTER DATABASE $(db_name) ADD FILE (
	NAME = scenario_file_1_3,
	FILENAME = N'$(db_path)scenario_file_1_3.ndf',
	SIZE = 15GB,
	MAXSIZE = 15GB,
	FILEGROWTH = 0GB) TO FILEGROUP scenario_fg_1;
END


-- Step 2. Create partition function
IF NOT EXISTS (SELECT name FROM sys.partition_functions WHERE name = N'scenario_partition')
BEGIN
-- create the partition function
CREATE PARTITION FUNCTION scenario_partition (smallint) AS
-- (x < 1, 1 <= x)
RANGE RIGHT FOR VALUES (1)
END
GO


-- Step 3. Create partition scheme that references the function
IF NOT EXISTS (SELECT name FROM sys.partition_schemes WHERE name = N'scenario_scheme')
BEGIN
-- assign range values 0 and 1 to the initial scenario filegroup
CREATE PARTITION SCHEME scenario_scheme AS
PARTITION scenario_partition TO (scenario_fg_1,scenario_fg_1)
END
GO


-- Create filegroup for reference and non-partitioned tables
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE name = N'ref_fg')
BEGIN
ALTER DATABASE $(db_name) ADD FILEGROUP ref_fg;
ALTER DATABASE $(db_name) ADD FILE
( NAME = ref_file,
  FILENAME = N'$(db_path)ref_file.ndf',
  SIZE = 5GB,
  MAXSIZE = 5GB,
  FILEGROWTH = 0GB) TO FILEGROUP ref_fg
END
