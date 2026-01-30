rem COPY REQUIRED FILES TO SPECIFIED ABM SCENARIO
@echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SCENARIOYEAR=%3
set VALIDATION_DIRECTORY=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\analysis\validation

rem Create Validation Folder If Not Exist
if not exist %VALIDATION_DIRECTORY% mkdir %VALIDATION_DIRECTORY%

rem CREATE VIS WORKSHEET FOR POWER BI VISUALIZATIONS

rem ### Use ABM3 uv env (for both tcov and tned)
CALL %activate_uv_asim%

rem ### Running validation pipeline for input scenario
python python\validation.py %PROJECT_DRIVE%%PROJECT_DIRECTORY% %SCENARIOYEAR%