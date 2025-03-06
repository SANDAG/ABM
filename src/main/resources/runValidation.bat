rem COPY REQUIRED FILES TO SPECIFIED ABM SCENARIO
@echo off

set SOURCE_DIRECTORY=\\sandag.org\transdata\ABM\release\tools\PowerBI_validation_tool
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set VALIDATION_DIRECTORY=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\analysis\validation

rem Create Validation Folder If Not Exist
if not exist %VALIDATION_DIRECTORY% mkdir %VALIDATION_DIRECTORY%

rem ### Copy files using xcopy
set SCENARIOYEAR=%3
xcopy %SOURCE_DIRECTORY%\data\%SCENARIOYEAR%\ %VALIDATION_DIRECTORY% /S /E /Y

rem CREATE VIS WORKSHEET FOR POWER BI VISUALIZATIONS
rem ### Connecting to Anaconda3 Environment
set ENV=%CONDA_PREFIX%
call %ENV%\Scripts\activate.bat %ENV%

rem ### Use ABM3 conda env (for both tcov and tned)
call activate asim_134

rem ### Running validation pipeline for input scenario
python %SOURCE_DIRECTORY%\src\%SCENARIOYEAR%\validation.py %PROJECT_DRIVE%%PROJECT_DIRECTORY% %SCENARIOYEAR%