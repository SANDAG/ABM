ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set faf_file_name=%3
set htm_input_file_name=%4
set skim_period=%5
set scenario_year=%6
set scenario_year_with_suffix=%7

ECHO Activate ActivitySim for CVM...
CALL %activate_uv_asim%

set MKL_NUM_THREADS=1
set MKL=1

CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

:: Create directory to store HTM outputs
CD Output
ECHO Create directory to store HTM outputs...
IF exist HTM ( echo HTM folder exists ) ELSE ( MD HTM && echo HTM folder created)
::MD HTM
CD ..

SET MODEL_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%
SET OUTPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\output

:: run run_htm.py
ECHO Run HTM...
CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%\python\
python run_htm.py %MODEL_DIR% %OUTPUT_DIR% %faf_file_name% %htm_input_file_name% %skim_period% %scenario_year% %scenario_year_with_suffix% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-htm.txt
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO HTM complete!
    ECHO %DATE% %TIME%
	
    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: HTM failed!
    ECHO %DATE% %TIME%
    EXIT /B 2
