rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Run ActivitySim
:: ---------------------------------------------------------------------
SET ANACONDA3_DIR=%CONDA_PREFIX%
SET ANACONDA2_DIR=%CONDA_TWO_PREFIX%

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

SET PATH=%ANACONDA2_DIR%\Library\bin
SET PATH=%ANACONDA2_DIR%\Scripts;%ANACONDA2_DIR%\bin

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat
SET CONDA2_DEA=%ANACONDA2_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_140\python.exe
:: FIX PATH AND ENV HERE 
SET PYTHON2=%ANACONDA2_DIR%\python.exe

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
CD /d %ANACONDA2_DIR%\Scripts
ECHO %cd%
CALL %CONDA2_ACT% base

cd /d %PROJECT_DIRECTORY%

%PYTHON3% src/asim/scripts/set_zoneMapping.py visitor output || exit /b 2

%PYTHON2% src/asim/scripts/convert_tripTables.py visitor output || exit /b 2

ECHO ActivitySim Visitor run complete!!
ECHO %startTime%%Time%

