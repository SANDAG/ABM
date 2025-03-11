ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set luz_data_file=%3

SET ANACONDA3_DIR=%CONDA_PREFIX%
SET ANACONDA2_DIR=%CONDA_TWO_PREFIX%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat
SET CONDA2_DEA=%ANACONDA2_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_134\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim for CVM...
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_134

:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

set MKL_NUM_THREADS=1
set MKL=1

CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%

:: Create directory to store CVM outputs
CD input
ECHO Create directory to store CVM Emp outputs...
IF exist CVM ( echo CVM folder exists ) ELSE ( MD CVM && echo CVM folder created)
CD ..

SET MODEL_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%
SET OUTPUT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\CVM
SET luz_data=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\%luz_data_file%

:: Aggregate employment data to TAZ level
ECHO Aggregate employment data to TAZ level
CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%\python\
%PYTHON3% aggregateEmpData.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use.csv %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use_taz.csv

:: run MGRAEmpByEstSize2.py
ECHO Run CVMEstablishmentSynthesis
%PYTHON3% MGRAEmpByEstSize2.py %MODEL_DIR% %OUTPUT_DIR% %luz_data% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-cvmEmp.txt

:: Recode establishment zone IDs from TAZ to MGRA
ECHO Recode establishment zone IDs from TAZ to MGRA
%PYTHON3% recodeSynthEstabToMGRA.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\CVM\SynthEstablishments.csv %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use.csv
IF %ERRORLEVEL% NEQ 0 (GOTO :ERROR) else (GOTO :SUCCESS)

:SUCCESS
    ECHO CVM_Est complete!
    ECHO %DATE% %TIME%

    :: finish and exit batch file
    EXIT /B 0

:ERROR
    CD /d %PROJECT_DRIVE%%PROJECT_DIRECTORY%
    ECHO ERROR: CVM_Est failed!
    ECHO %DATE% %TIME%
    EXIT /B 2
