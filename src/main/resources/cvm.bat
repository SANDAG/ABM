ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

ECHO Activate ActivitySim for CVM...
:: if the activitysim environment for CVM is in the src directory
set ENVPYTHON=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\ASIM-DEV-SANDAG-CVM\python.exe
:: TODO perhaps we don't want to keep the asim-cvm environment in the src directory

:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

set MKL_NUM_THREADS=1
set MKL=1

CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

:: Create directory to store CVM outputs
CD Output
ECHO Create directory to store CVM outputs...
MD CVM
CD ..

SET CVM_INPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\CVM,%PROJECT_DRIVE%%PROJECT_DIRECTORY%\Output\resident,%PROJECT_DRIVE%%PROJECT_DIRECTORY%\Output\skims
SET CVM_CONFIGS_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\configs
SET CVM_OUTPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\output\CVM

:: run run_cvm.py
ECHO Run CVM...
CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\
%ENVPYTHON% run_cvm.py %CVM_INPUT_DIR% %CVM_CONFIGS_DIR% %CVM_OUTPUT_DIR% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-cvm.txt
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO CVM complete!
    ECHO %DATE% %TIME%

    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

    ::::::::::::::::::::::
    :: Post-process CVM outputs

    :: sort TAZ zone index in omx
    %ENVPYTHON% src/asim-cvm/scripts/set_zoneMapping.py cvm output
    :: append cvm omx to assignment omx
    :: TODO Jielin to update the script to append omx
    :: %PYTHON2% src/asim-cvm/scripts/convert_tripTables.py cvm output

    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: CVM failed!
    ECHO %DATE% %TIME%
    PAUSE