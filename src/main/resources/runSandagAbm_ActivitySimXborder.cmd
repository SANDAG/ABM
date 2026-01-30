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
MD crossborder
CD ..

:: Run Models
python src/asim/scripts/xborder/cross_border_model.py -a -c src/asim/configs/crossborder -c src/asim/configs/common -d input -o output/crossborder || exit /b 2

::::::::::::::::::::::
cd /d %PROJECT_DIRECTORY%
python src/asim/scripts/set_zoneMapping.py crossborder output || exit /b 2


ECHO ActivitySim Crossborder run complete!!
ECHO %startTime%%Time%

