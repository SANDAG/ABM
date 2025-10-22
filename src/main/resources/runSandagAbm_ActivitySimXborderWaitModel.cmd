rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Loop
:: -------------------------------------------------------------------------------------------------
ECHO ****MODEL ITERATION %ITERATION%

:: -------------------------------------------------------------------------------------------------
:: Run ActivitySim
:: ---------------------------------------------------------------------
ECHO Activate ActivitySim....
CALL %activate_uv_asim%

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs if it does not exist already
CD output
ECHO Create Output Directory
MD crossborder
CD ..

:: Run xborder wait time model
python src/asim/scripts/xborder/cross_border_model.py -w -c src/asim/configs/crossborder -c src/asim/configs/common -d input -o output/crossborder || exit /b 2

ECHO ActivitySim Crossborder wait time model run complete!!
ECHO %startTime%%Time%

