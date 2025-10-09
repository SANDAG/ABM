rem @echo on

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: ---------------------------------------------------------------------
ECHO Activate ActivitySim Environment....
CALL %activate_uv_asim%

:: Run skimming
python src/main/python/2zoneSkim.py %PROJECT_DIRECTORY% || exit /b 2

ECHO %startTime%%Time%

