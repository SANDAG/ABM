ECHO OFF

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

CALL %activate_uv_asim%

python src/asim/scripts/bike_route_choice/bike_route_choice.py src/asim/scripts/bike_route_choice/bike_route_choice_settings_mgra.yaml || exit /b 2

python src/asim/scripts/bike_route_choice/bike_route_choice.py src/asim/scripts/bike_route_choice/bike_route_choice_settings_taz.yaml || exit /b 2