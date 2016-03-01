SET GDAL_PATH=%1

SET GDAL_DATA=%GDAL_PATH%\data

%GDAL_PATH%\bin\ogr2ogr -overwrite -gt 5000 -sql %2 -s_srs EPSG:2230 -t_srs EPSG:4326 -f MSSQLSpatial "MSSQL:server=%3;database=%4;trusted_connection=yes" %5 -nln %6 -lco OVERWRITE=YES -lco GEOM_NAME=geom -lco LAUNDER=YES -lco SCHEMA=abm_staging