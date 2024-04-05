ECHO OFF

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

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_baydag\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_baydag

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs
CD output
ECHO Create Output Directory
MD airport.CBX
MD airport.SAN
CD ..

:: Run Models
ECHO Run ActivitySim AirportCBX Model
%PYTHON3% src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.CBX -d input -o output/airport.CBX || exit /b 2
ECHO Run ActivitySim AirportSAN Model
%PYTHON3% src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.SAN -d input -o output/airport.SAN || exit /b 2

::::::::::::::::::::::
CD /d %ANACONDA2_DIR%\Scripts
ECHO %cd%
CALL %CONDA2_ACT% base

cd /d %PROJECT_DIRECTORY%

%PYTHON3% src/asim/scripts/set_zoneMapping.py airport.CBX output || exit /b 2
%PYTHON3% src/asim/scripts/set_zoneMapping.py airport.SAN output || exit /b 2

%PYTHON2% src/asim/scripts/convert_tripTables.py airport.CBX output || exit /b 2
%PYTHON2% src/asim/scripts/convert_tripTables.py airport.SAN output || exit /b 2

ECHO ActivitySim Airport model runs complete!!
ECHO %startTime%%Time%

