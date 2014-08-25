SET GDAL_DATA=.\data

bin\ogr2ogr -overwrite -gt 5000 -sql "select CAST(ROADSEGID AS integer(12)) from SANDAG_Bike_Net" -a_srs EPSG:2230 -f MSSQLSpatial "MSSQL:server=${database_server};database=${database_name};trusted_connection=yes" %1 -nln %2 -lco OVERWRITE=YES -lco GEOM_NAME=geom -lco LAUNDER=YES -lco SCHEMA=abm_staging