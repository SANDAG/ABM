SET GDAL_DATA=.\data

bin\ogr2ogr -overwrite -gt 5000 -select %1 -a_srs EPSG:2230 -f MSSQLSpatial "MSSQL:server=${database_server};database=${database_name};trusted_connection=yes" %2 -nln %3 -lco OVERWRITE=YES -lco GEOM_NAME=geom -lco LAUNDER=YES -lco SCHEMA=abm_staging