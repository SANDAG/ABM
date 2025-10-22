ECHO ON
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set luz_data_file=%3

ECHO Activate ActivitySim for CVM...
CALL %activate_uv_asim%

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
python aggregateEmpData.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use.csv %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use_taz.csv

:: run MGRAEmpByEstSize2.py
ECHO Run CVMEstablishmentSynthesis
python MGRAEmpByEstSize2.py %MODEL_DIR% %OUTPUT_DIR% %luz_data% 2>>%PROJECT_DRIVE%%PROJECT_DIRECTORY%\logFiles\event-cvmEmp.txt

:: Recode establishment zone IDs from TAZ to MGRA
ECHO Recode establishment zone IDs from TAZ to MGRA
python recodeSynthEstabToMGRA.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\CVM\SynthEstablishments.csv %PROJECT_DRIVE%%PROJECT_DIRECTORY%\input\land_use.csv
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
