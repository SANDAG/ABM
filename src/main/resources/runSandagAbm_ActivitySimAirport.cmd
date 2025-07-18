ECHO OFF

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
:: FIX PATH AND ENV HERE LATER

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_140

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs
CD output
ECHO Create Output Directory
MD airport.CBX
MD airport.SAN
CD ..

:: Copy outputs.yaml from configs/common to configs/common_airport
copy src\asim\configs\common\outputs.yaml src\asim\configs\common_airport

:: Run Models
ECHO Run ActivitySim AirportCBX Model
%PYTHON3% src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.CBX -d input -o output/airport.CBX || exit /b 2
ECHO Run ActivitySim AirportSAN Model
%PYTHON3% src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.SAN -d input -o output/airport.SAN || exit /b 2

::::::::::::::::::::::
cd /d %PROJECT_DIRECTORY%

%PYTHON3% src/asim/scripts/set_zoneMapping.py airport.CBX output || exit /b 2
%PYTHON3% src/asim/scripts/set_zoneMapping.py airport.SAN output || exit /b 2

ECHO ActivitySim Airport model runs complete!!
ECHO %startTime%%Time%

