rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Run ActivitySim
:: ---------------------------------------------------------------------
ECHO Activate ActivitySim....
CALL %activate_uv_asim%

set MKL_NUM_THREADS=1
set MKL=1

:: Create Directory to Store ActivitySim Outputs
CD output
ECHO Create Output Directory
MD visitor
CD ..

:: Run Models
python src/asim/scripts/visitor/visitor_model.py -a -c src/asim/configs/visitor -d input -o output/visitor || exit /b 2

::::::::::::::::::::::

cd /d %PROJECT_DIRECTORY%

python src/asim/scripts/set_zoneMapping.py visitor output || exit /b 2


ECHO ActivitySim Visitor run complete!!
ECHO %startTime%%Time%

