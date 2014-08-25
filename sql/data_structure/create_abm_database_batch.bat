@echo off

rem create_abm_database.sql, create_schemas.sql, create_ref_tables.sql, create_abm_tables.sql, create_abm_foreign_keys.sql, create_db_meta.sql
rem create_abm_dd.sql, create_abm_udfs.sql, create_emfac.sql, create_data_load.sql
rem update_schema_change_log.sql


set db_server=
set db_name=
set log_path=
set db_path=
set script_path=


echo Creating %db_name% on %db_server% at %db_path%
sqlcmd -S %db_server% -i %script_path%create_abm_database.sql -E -C -v db_name=%db_name% db_name_string="N'%db_name%'" mdf="N'%db_path%%db_name%_primary.mdf'" ldf="N'%log_path%%db_name%_log.ldf'" scen_file_string="N'%db_path%scenario_file_1.ndf'" ref_file_string="N'%db_path%ref_file.ndf'"
if not errorlevel 1 goto next1
echo == An error occurred creating %db_name% on %db_server%
exit /B

:next1

echo Creating base schemas
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_schemas.sql -E -C
if not errorlevel 1 goto next2
echo == An error occurred creating schemas, may need to drop %db_name% on %db_server%
exit /B

:next2

echo Creating reference tables and static data
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_ref_tables.sql -E -C
if not errorlevel 1 goto next3
echo == An error occurred creating reference tables for %db_name% on %db_server%
exit /B

:next3

echo Creating abm data tables
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_abm_tables.sql -E -C
if not errorlevel 1 goto next4
echo == An error occurred creating abm data tables for %db_name% on %db_server%
exit /B

:next4

echo Creating abm data table foreign key constraints
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_abm_foreign_keys.sql -E -C
if not errorlevel 1 goto next5
echo == An error occurred creating abm data table foreign key constraints for %db_name% on %db_server%
exit /B

:next5

echo Creating db_meta schema and objects
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_db_meta.sql -E -C
if not errorlevel 1 goto next6
echo == An error occurred creating db_meta objects for %db_name% on %db_server%
exit /B

:next6

echo Creating abm base tables data dictionary
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_abm_dd.sql -E -C
if not errorlevel 1 goto next7
echo == An error occurred creating abm data dictionary for %db_name% on %db_server%
exit /B

:next7

echo Creating abm user defined functions
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_abm_udfs.sql -E -C
if not errorlevel 1 goto next8
echo == An error occurred creating abm udfs for %db_name% on %db_server%
exit /B

:next8

echo Creating emfac schema and objects
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_emfac.sql -E -C
if not errorlevel 1 goto next9
echo == An error occurred creating emfac objects for %db_name% on %db_server%
exit /B

:next9

echo Creating data load schema and objects
sqlcmd -S %db_server% -d %db_name% -i %script_path%create_data_load.sql -E -C
if not errorlevel 1 goto next10
echo == An error occurred creating data_load objects for %db_name% on %db_server%
exit /B

:next10





sqlcmd -S %db_server% -d %db_name% -i %script_path%update_schema_change_log.sql -E -C

echo %db_name% on %db_server% successfully created
echo data files located at:
echo %db_path%
echo log file located at:
echo %log_path%
