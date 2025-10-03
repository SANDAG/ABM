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
SET ANACONDA3_DIR=%CONDA_PREFIX%

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_140\python.exe
:: FIX PATH AND ENV HERE LATER

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_140

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs if it does not exist already
CD output
ECHO Create Output Directory
MD crossborder
CD ..

:: Run xborder wait time model
%PYTHON3% src/asim/scripts/xborder/cross_border_model.py -w -c src/asim/configs/crossborder -c src/asim/configs/common -d input -o output/crossborder || exit /b 2

ECHO ActivitySim Crossborder wait time model run complete!!
ECHO %startTime%%Time%

