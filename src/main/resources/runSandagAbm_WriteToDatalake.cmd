ECHO ON

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Run ActivitySim
:: ---------------------------------------------------------------------
SET ANACONDA3_DIR=C:\Anaconda3
SET ANACONDA2_DIR=C:\Anaconda2

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

SET PYTHON3=C:\Users\%USERNAME%\.conda\envs\asim_baydag\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_baydag

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Run Write to Datalake
%PYTHON3% src/asim/simulation.py -s settings_write.yaml -c src/asim/configs/resident -c src/asim/configs/common -d input -d output/skims -o output/resident
%PYTHON3% src/asim/simulation.py -s settings_write.yaml -c src/asim/configs/airport.CBX -c src/asim/configs/common_airport -d input -d output/skims -o output/airport.CBX
%PYTHON3% src/asim/simulation.py -s settings_write.yaml -c src/asim/configs/airport.SAN -c src/asim/configs/common_airport -d input -d output/skims -o output/airport.SAN
%PYTHON3% src/asim/simulation.py -s settings_write.yaml -c src/asim/configs/crossborder -d input -d output/skims -o output/crossborder
%PYTHON3% src/asim/simulation.py -s settings_write.yaml -c src/asim/configs/visitor -c src/asim/configs/common -d input -d output/skims -o output/visitor

ECHO Write to Datalake complete!!
ECHO %startTime%%Time%