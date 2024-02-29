ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

SET ANACONDA3_DIR=C:\Anaconda3
SET ANACONDA2_DIR=C:\Anaconda2

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat
SET CONDA2_DEA=%ANACONDA2_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_sandag_cvm\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim for CVM...
:: Option 1: if the activitysim environment for CVM is in the src directory
:: set ENVPYTHON=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\src\asim-cvm\ASIM-DEV-SANDAG-CVM\python.exe
:: Option 2: SANDAG prefers. Keep the same set up as resident asim, don't keep the asim-cvm environment in the src directory
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_sandag_cvm

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
%PYTHON3% run_cvm.py %CVM_INPUT_DIR% %CVM_CONFIGS_DIR% %CVM_OUTPUT_DIR% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-cvm.txt
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO CVM complete!
    ECHO %DATE% %TIME%
	
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

    ::::::::::::::::::::::
    :: Post-process CVM outputs

    :: sort TAZ zone index in omx
    %PYTHON3% src/asim-cvm/scripts/set_zoneMapping.py cvm output
	
	:: convert trip tables into Python2
	%PYTHON2% src/asim-cvm/scripts/convert_tripTables.py cvm output

    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: CVM failed!
    ECHO %DATE% %TIME%
    EXIT /B