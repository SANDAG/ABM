@echo off


set script_path=
set db_server=${database_server}
set db_name=${database_name}
rem db_path and log_path values must be enclosed in double quotes
set db_path=
set log_path=


echo Creating %db_name% on %db_server% at %db_path%
echo Log file at %log_path%
sqlcmd -E -C -b -S %db_server% -i %script_path%create_db.sql -v db_name=%db_name% db_path=%db_path% log_path=%log_path% || goto :EOF

echo Creating reference dimension tables
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%create_reference_dimension_tables.sql || goto :EOF

echo Inserting values into geography reference table
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%insert_geography_dimension.sql || goto :EOF

echo Updating shape values in geography reference table
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%update_geography_dimension.sql || goto :EOF

echo Creating scenario dimension tables
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%create_scenario_dimension_tables.sql || goto :EOF

echo Creating fact tables
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%create_fact_tables.sql || goto :EOF

echo Creating db_meta schema and objects
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%db_meta.sql || goto :EOF

echo Creating data dictionary
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%data_dictionary.sql || goto :EOF

echo Creating data_load schema and objects
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%data_load.sql || goto :EOF

echo Creating database management objects and permissions roles
sqlcmd -E -C -b -S %db_server% -d %db_name% -i %script_path%create_management.sql || goto :EOF

echo Successfully created  %db_name% on %db_server% at %db_path%