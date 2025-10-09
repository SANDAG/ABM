ECHO ON

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
CD Output
ECHO Create Output Directory
MD resident
MD resident\trace
MD resident\log
CD ..

:: Run simulation.py
python src/asim/simulation.py -s settings_mp.yaml -c src/asim/configs/resident -c src/asim/configs/common -d input -d output/skims -o output/resident || exit /b 2

cd /d %PROJECT_DIRECTORY%
python src/asim/scripts/set_zoneMapping.py resident output || exit /b 2

ECHO ActivitySim run complete!!
ECHO %startTime%%Time%