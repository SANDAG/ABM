ECHO ON

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

ECHO Activate ActivitySim Python Environment....
CALL %activate_uv_asim%

:: Run Taxi/TNC Routing Model
python src/asim/scripts/taxi_tnc_routing/taxi_tnc_routing.py  %PROJECT_DIRECTORY% --settings src/asim/scripts/taxi_tnc_routing/taxi_tnc_routing_settings.yaml || exit /b 2
ECHO Taxi / TNC routing model run complete!

:: Create demand matrices from AV and TNC vehicle trips
python src/asim/scripts/taxi_tnc_routing/tnc_av_matrix_builder.py  %PROJECT_DIRECTORY% --settings src/asim/scripts/taxi_tnc_routing/taxi_tnc_routing_settings.yaml || exit /b 2

ECHO AV/TNC matrix building complete!
ECHO %startTime%%Time%