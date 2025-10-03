rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Run ActivitySim
:: ---------------------------------------------------------------------
SET ANACONDA3_DIR=%CONDA_PREFIX%

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_140\python.exe
:: FIX PATH AND ENV HERE 

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_140

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs
CD output
ECHO Create Output Directory
MD visitor
CD ..

:: Run Models
%PYTHON3% src/asim/scripts/visitor/visitor_model.py -a -c src/asim/configs/visitor -d input -o output/visitor || exit /b 2

::::::::::::::::::::::

cd /d %PROJECT_DIRECTORY%

%PYTHON3% src/asim/scripts/set_zoneMapping.py visitor output || exit /b 2


ECHO ActivitySim Visitor run complete!!
ECHO %startTime%%Time%

