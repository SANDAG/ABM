ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set faf_file_name=%3
set htm_input_file_name=%4
set skim_period=%5
set scenario_year=%6
set scenario_year_with_suffix=%7

SET ANACONDA3_DIR=%CONDA_PREFIX%
SET ANACONDA2_DIR=%CONDA_TWO_PREFIX%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat
SET CONDA2_DEA=%ANACONDA2_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_140\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim for CVM...
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_140

:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

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
%PYTHON3% run_htm.py %MODEL_DIR% %OUTPUT_DIR% %faf_file_name% %htm_input_file_name% %skim_period% %scenario_year% %scenario_year_with_suffix% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-htm.txt
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO HTM complete!
    ECHO %DATE% %TIME%

    CD /d %ANACONDA2_DIR%\Scripts
    ECHO %cd%
    CALL %CONDA2_ACT% base
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
	
	:: convert trip tables into Python2
	%PYTHON2% src/asim-cvm/scripts/convert_tripTables.py htm output

    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: HTM failed!
    ECHO %DATE% %TIME%
    EXIT /B 2
