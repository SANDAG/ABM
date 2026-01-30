ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SCENARIO_YEAR_WITH_SUFFIX=%3

ECHO Activate ActivitySim for CVM...
CALL %activate_uv_asim%

set MKL_NUM_THREADS=1
set MKL=1

CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

:: Create directory to store CVM outputs
CD Output
ECHO Create directory to store CVM outputs...
IF exist CVM ( echo CVM folder exists ) ELSE ( MD CVM && echo CVM folder created)
CD ..

SET CVM_INPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\input,%PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\CVM,%PROJECT_DRIVE%%PROJECT_DIRECTORY%\Output\resident,%PROJECT_DRIVE%%PROJECT_DIRECTORY%\Output\skims
SET CVM_CONFIGS_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\configs
SET CVM_OUTPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\output\CVM
SET MODEL_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%

:: run run_cvm.py
ECHO Run CVM...
CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\
python run_cvm.py %CVM_INPUT_DIR% %CVM_CONFIGS_DIR% %CVM_OUTPUT_DIR% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-cvm.txt
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO CVM complete!
    ECHO %DATE% %TIME%

    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

    ::::::::::::::::::::::
    :: Post-process CVM outputs
	python src/asim-cvm/scripts/generate_summary.py %MODEL_DIR% %CVM_OUTPUT_DIR% %SCENARIO_YEAR_WITH_SUFFIX%
    :: sort TAZ zone index in omx
    python src/asim-cvm/scripts/set_zoneMapping.py cvm output

    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: CVM failed!
    ECHO %DATE% %TIME%
    EXIT /B 2