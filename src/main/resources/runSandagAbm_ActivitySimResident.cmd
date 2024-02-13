ECHO ON

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SAMPLERATE=%3
set ITERATION=%4

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Loop
:: -------------------------------------------------------------------------------------------------

SET SAMPLE=%SAMPLERATE%

ECHO CURRENT DIRECTORY: %cd%
CD src\asim\configs\resident
:: Set sample_rate in configs file dynamically
FOR /F "USEBACKQ delims=" %%i IN (`type "settings_mp.yaml" ^| find /V /N ""`) DO (
    SETLOCAL EnableDelayedExpansion
    SET LINE=%%i
    set LINE=!LINE:*]=!
    SET SUBSTR=!LINE:~0,23!
    IF !SUBSTR! == households_sample_size: (
        ECHO households_sample_size: %SAMPLE% >> settings_mp_temp.yaml
    ) ELSE (
        ECHO:!LINE! >> settings_mp_temp.yaml
    )
    ENDLOCAL
)
del settings_mp.yaml
move settings_mp_temp.yaml settings_mp.yaml
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
CD Output
ECHO Create Output Directory
MD resident
MD resident\trace
MD resident\log
CD ..

:: Run simulation.py

%PYTHON3% src/asim/simulation.py -s settings_mp.yaml -c src/asim/configs/resident -c src/asim/configs/common -d input -d output/skims -o output/resident
if ERRORLEVEL 1 exit 2

::::::::::::::::::::::
CD /d %ANACONDA2_DIR%\Scripts
ECHO %cd%
CALL %CONDA2_ACT% base

cd /d %PROJECT_DIRECTORY%
%PYTHON3% src/asim/scripts/set_zoneMapping.py resident output

%PYTHON2% src/asim/scripts/convert_tripTables.py resident output

ECHO ActivitySim run complete!!
ECHO %startTime%%Time%