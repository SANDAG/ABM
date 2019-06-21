SET GDAL_DATA=%1
SET OGR2OGR_PATH=%2
SET SCENARIO_PATH=%3
SET SCENARIO_ID=%4

%OGR2OGR_PATH%\ogr2ogr -f "ESRI Shapefile" "%SCENARIO_PATH%\output\hwyload_%SCENARIO_ID%.shp" "MSSQL:server=${database_server};database=abm_2_reporting;trusted_connection=yes;" -sql "SELECT * FROM [report].[vi_hwyload] WHERE [scen_id] = %SCENARIO_ID%" -overwrite -s_srs "EPSG:2230" -t_srs "EPSG:2230"