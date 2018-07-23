SET OGR2OGR_PATH=%1
SET SCENARIO_PATH=%2
SET SCENARIO_ID=%3

%OGR2OGR_PATH%\ogr2ogr -f "ESRI Shapefile" "%SCENARIO_PATH%\output\hwyload_%SCENARIO_ID%.shp" "MSSQL:server=${database_server};database=${database_name};trusted_connection=yes;" -sql "SELECT * FROM [report].[vi_hwyload] WHERE [scen_id] = %SCENARIO_ID%" -overwrite -s_srs "EPSG:2230" -t_srs "EPSG:2230"