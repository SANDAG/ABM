rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SAMPLERATE=%3

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: -------------------------------------------------------------------------------------------------
:: Loop
:: -------------------------------------------------------------------------------------------------
SET SAMPLE=%SAMPLERATE%

ECHO CURRENT DIRECTORY: %cd%
CD src\asim\configs\crossborder
:: Set sample_rate in configs file dynamically
FOR /F "USEBACKQ delims=" %%i IN (`type "settings.yaml" ^| find /V /N ""`) DO (
    SETLOCAL EnableDelayedExpansion
    SET LINE=%%i
    set LINE=!LINE:*]=!
    SET SUBSTR=!LINE:~0,23!
    IF !SUBSTR! == households_sample_size: (
        ECHO households_sample_size: %SAMPLE% >> settings_temp.yaml
    ) ELSE (
        ECHO:!LINE! >> settings_temp.yaml
    )
    ENDLOCAL
)
del settings.yaml
move settings_temp.yaml settings.yaml

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
MD crossborder
CD ..

:: Run Models
%PYTHON3% src/asim/scripts/xborder/cross_border_model.py -a -c src/asim/configs/crossborder -d input -o output/crossborder

::::::::::::::::::::::
CD /d %ANACONDA2_DIR%\Scripts
ECHO %cd%
CALL %CONDA2_ACT% base

cd /d %PROJECT_DIRECTORY%
%PYTHON3% src/asim/scripts/set_zoneMapping.py crossborder output

%PYTHON2% src/asim/scripts/convert_tripTables.py crossborder output

ECHO ActivitySim Crossborder run complete!!
ECHO %startTime%%Time%

