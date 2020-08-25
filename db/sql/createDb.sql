USE [master]
GO

-- create database primary filegroup and log
CREATE DATABASE $(dbName) ON  PRIMARY
( NAME = N'$(dbName)_primary',
  FILENAME = N'$(dbPath)$(dbName)_primary.ndf',
  SIZE = 5GB ,
  MAXSIZE = 5GB ,
  FILEGROWTH = 0GB)
LOG ON
( NAME = N'$(dbName)_log',
  FILENAME = N'$(logPath)$(dbName)_log.ldf',
  SIZE = 10GB ,
  MAXSIZE = 10GB ,
  FILEGROWTH = 0GB)
GO

-- set compatibility to sql server 2016
ALTER DATABASE $(dbName) SET COMPATIBILITY_LEVEL = 130

-- set database options
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
BEGIN
EXEC $(dbName).[dbo].[sp_fulltext_database] @action = 'enable'
END
ALTER DATABASE $(dbName) SET ANSI_NULL_DEFAULT OFF
ALTER DATABASE $(dbName) SET ANSI_NULLS OFF
ALTER DATABASE $(dbName) SET ANSI_PADDING OFF
ALTER DATABASE $(dbName) SET ANSI_WARNINGS OFF
ALTER DATABASE $(dbName) SET ARITHABORT OFF
ALTER DATABASE $(dbName) SET AUTO_CLOSE OFF

-- incremental statistics on
ALTER DATABASE $(dbName) SET AUTO_CREATE_STATISTICS ON (INCREMENTAL = ON)

ALTER DATABASE $(dbName) SET AUTO_SHRINK OFF
ALTER DATABASE $(dbName) SET AUTO_UPDATE_STATISTICS ON
ALTER DATABASE $(dbName) SET CURSOR_CLOSE_ON_COMMIT OFF
ALTER DATABASE $(dbName) SET CURSOR_DEFAULT  GLOBAL
ALTER DATABASE $(dbName) SET CONCAT_NULL_YIELDS_NULL OFF
ALTER DATABASE $(dbName) SET NUMERIC_ROUNDABORT OFF
ALTER DATABASE $(dbName) SET QUOTED_IDENTIFIER OFF
ALTER DATABASE $(dbName) SET RECURSIVE_TRIGGERS OFF
ALTER DATABASE $(dbName) SET  DISABLE_BROKER
ALTER DATABASE $(dbName) SET AUTO_UPDATE_STATISTICS_ASYNC OFF
ALTER DATABASE $(dbName) SET DATE_CORRELATION_OPTIMIZATION OFF
ALTER DATABASE $(dbName) SET TRUSTWORTHY OFF
ALTER DATABASE $(dbName) SET ALLOW_SNAPSHOT_ISOLATION OFF
ALTER DATABASE $(dbName) SET PARAMETERIZATION SIMPLE
ALTER DATABASE $(dbName) SET READ_COMMITTED_SNAPSHOT OFF
ALTER DATABASE $(dbName) SET HONOR_BROKER_PRIORITY OFF
ALTER DATABASE $(dbName) SET  READ_WRITE
ALTER DATABASE $(dbName) SET RECOVERY SIMPLE
ALTER DATABASE $(dbName) SET  MULTI_USER
ALTER DATABASE $(dbName) SET PAGE_VERIFY CHECKSUM
ALTER DATABASE $(dbName) SET DB_CHAINING OFF
GO


-- go into the newly created database
USE $(dbName);
-- ===================================
-- Create Partition Scheme
-- ===================================
-- Step 1. Add initial scenario filegroup
-- Scenario 0 is placeholder for merges/splits
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE name = N'scenario_fg_1')
BEGIN
-- add initial scenario filegroup
ALTER DATABASE $(dbName) ADD FILEGROUP scenario_fg_1;

-- add file to the filegroup
ALTER DATABASE $(dbName) ADD FILE (
	NAME = scenario_file_1,
	FILENAME = N'$(dbPath)scenario_file_1.ndf',
	SIZE = 5GB,
	MAXSIZE = 5GB,
	FILEGROWTH = 0GB) TO FILEGROUP scenario_fg_1;
END

-- Step 2. Create partition function
IF NOT EXISTS (SELECT name FROM sys.partition_functions WHERE name = N'scenario_partition')
BEGIN
-- create the partition function
CREATE PARTITION FUNCTION scenario_partition (int) AS
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
IF NOT EXISTS (SELECT name FROM sys.filegroups WHERE name = N'reference_fg')
BEGIN
ALTER DATABASE $(dbName) ADD FILEGROUP reference_fg;
ALTER DATABASE $(dbName) ADD FILE
( NAME = reference_file,
  FILENAME = N'$(dbPath)reference_file.ndf',
  SIZE = 5GB,
  MAXSIZE = 5GB,
  FILEGROWTH = 0GB) TO FILEGROUP reference_fg
END
GO


-- Create schema for staging tables
CREATE SCHEMA [staging]
GO

EXECUTE [sys].[sp_addextendedproperty]
    @name = 'MS_Description'
    ,@value = 'schema to hold intermediary loading data tables'
    ,@level0type = 'SCHEMA'
    ,@level0name = 'staging'
