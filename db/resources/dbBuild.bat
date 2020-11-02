@echo off


set scriptPath=
set dbServer=
set dbName=
rem dbPath and logPath values must be enclosed in double quotes
set dbPath=
set logPath=


echo Creating %dbName% on %dbServer% at %dbPath%
echo Log file at %logPath%
sqlcmd -E -C -b -S %dbServer% -i %scriptPath%createDb.sql -v dbName=%dbName% dbPath=%dbPath% logPath=%logPath% || goto :EOF

echo Creating db_meta schema and objects
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%dbMeta.sql || goto :EOF

echo Creating reference dimension tables
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%createReferenceDimensionTables.sql || goto :EOF

echo Inserting values into geography reference table
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%insertGeographyDimension.sql || goto :EOF

echo Inserting shape values into geography reference table
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%insertGeographyShape.sql || goto :EOF

echo Creating scenario dimension tables
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%createScenarioDimensionTables.sql || goto :EOF

echo Creating fact tables
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%createFactTables.sql || goto :EOF

echo Creating data_load schema and objects
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%dataLoad.sql || goto :EOF

echo Creating database management objects and permissions roles
sqlcmd -E -C -b -S %dbServer% -d %dbName% -i %scriptPath%createManagement.sql || goto :EOF

echo Successfully created  %dbName% on %dbServer% at %dbPath%
