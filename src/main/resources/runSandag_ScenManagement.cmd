ECHO OFF

set PROJECT_DIRECTORY=%1
set SCENYEAR=%2
set SCENYEARWITHSUFFIX=%3

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

CALL %activate_uv_asim%

python src/asim/scripts/ScenarioManagement/scenManagement.py %PROJECT_DIRECTORY% %SCENYEAR% %SCENYEARWITHSUFFIX%






