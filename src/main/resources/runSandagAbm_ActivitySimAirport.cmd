ECHO OFF

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
MD airport.CBX
MD airport.SAN
CD ..

:: Copy outputs.yaml from configs/common to configs/common_airport
copy src\asim\configs\common\outputs.yaml src\asim\configs\common_airport

:: Run Models
ECHO Run ActivitySim AirportCBX Model
python src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.CBX -d input -o output/airport.CBX || exit /b 2
ECHO Run ActivitySim AirportSAN Model
python src/asim/scripts/airport/airport_model.py -a -c src/asim/configs/airport.SAN -d input -o output/airport.SAN || exit /b 2

::::::::::::::::::::::
cd /d %PROJECT_DIRECTORY%

python src/asim/scripts/set_zoneMapping.py airport.CBX output || exit /b 2
python src/asim/scripts/set_zoneMapping.py airport.SAN output || exit /b 2

ECHO ActivitySim Airport model runs complete!!
ECHO %startTime%%Time%

