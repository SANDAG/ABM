:: ############################################################################
:: # Batch file to summarize CT-RAMP outputs and generate HTML Visualizer
:: # khademul.haque@rsginc.com, March 2019
:: #
:: # This script summarizes the CTRAMP outputs of both ABM and Reference scenarios and generates a visualizer comparing the ABM to the reference scenario
:: # ------------------------------------------------------------------------------
:: # To-Do
:: # 1. create log files from scripts (for later)
:: # 2. identify the input files names used by the summary R scripts and visualizer. maybe we can put them in the batch file
:: ############################################################################

@ECHO off

REM SET PROJECT_DRIVE=C:
REM SET PROJECT_DIRECTORY=\ABM_runs\maint_2019_RSG\Model\ABM2_14_2_0
REM SET REFER_DIR=T:\projects\sr14\abm2_test\abm_runs\14_1_0\2016_local_mask_2\
REM SET OUTPUT_HTML_NAME=SANDAG_Dashboard_2016_calib_3_19_19_final_test
REM SET IS_BASE_SURVEY=No
REM SET BASE_SCENARIO_NAME=REFERENCE
REM SET BUILD_SCENARIO_NAME=SDABM16
REM SET MGRA_INPUT_FILE=input/mgra13_based_input2016.csv

:: Inputs from arguments
SET PROJECT_DRIVE=%1
SET PROJECT_DIRECTORY=%2
SET REFER_DIR=%3
SET OUTPUT_HTML_NAME=%4
SET IS_BASE_SURVEY=%5
SET BASE_SCENARIO_NAME=%6
SET BUILD_SCENARIO_NAME=%7
SET MGRA_INPUT_FILE=%8

:: Default inputs
SET MAX_ITER=3
SET BASE_SAMPLE_RATE=1.0
SET BUILD_SAMPLE_RATE=1.0
SET SHP_FILE_NAME=pseudomsa.shp

:: Set Directories

SET PROJECT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\
SET REFER_DIR=%REFER_DIR%\
SET CURRENT_DIR=%PROJECT_DIR%visualizer\
SET WORKING_DIR=%CURRENT_DIR%
SET SUMM_DIR=%WORKING_DIR%outputs\summaries\
SET REF_DIR=%REFER_DIR%output
SET REF_DIR_INP=%REFER_DIR%input
SET BASE_SUMMARY_DIR=%SUMM_DIR%REF
SET BUILD_SUMMARY_DIR=%SUMM_DIR%BUILD
SET R_SCRIPT=%WORKING_DIR%dependencies\R-3.4.1\bin\Rscript
SET R_LIBRARY=%WORKING_DIR%dependencies\R-3.4.1\library
SET RSTUDIO_PANDOC=%WORKING_DIR%dependencies\Pandoc

:: Summarize BUILD
SET WD=%BUILD_SUMMARY_DIR%
SET ABMOutputDir=%PROJECT_DIR%output
SET INPUT_FILE_ABM=%SUMM_DIR%summ_inputs_abm.csv

ECHO Key,Value > %INPUT_FILE_ABM%
ECHO WD,%WD% >> %INPUT_FILE_ABM%
ECHO ABMOutputDir,%ABMOutputDir% >> %INPUT_FILE_ABM%
ECHO geogXWalkDir,%WORKING_DIR%data >> %INPUT_FILE_ABM%
ECHO SkimDir,%ABMOutputDir% >> %INPUT_FILE_ABM%
ECHO MAX_ITER,%MAX_ITER% >> %INPUT_FILE_ABM%
:: Call R script to summarize BUILD outputs
ECHO %startTime%%Time%: Running R script to summarize BUILD outputs...
%R_SCRIPT% %WORKING_DIR%scripts\SummarizeABM2016.R %INPUT_FILE_ABM%

:: Summarize REF
SET WD=%BASE_SUMMARY_DIR%
SET INPUT_FILE_REF=%SUMM_DIR%summ_inputs_ref.csv

ECHO Key,Value > %INPUT_FILE_REF%
ECHO WD,%WD% >> %INPUT_FILE_REF%
ECHO ABMOutputDir,%REF_DIR% >> %INPUT_FILE_REF%
ECHO geogXWalkDir,%WORKING_DIR%data >> %INPUT_FILE_REF%
ECHO SkimDir,%REF_DIR% >> %INPUT_FILE_REF%
ECHO MAX_ITER,%MAX_ITER% >> %INPUT_FILE_REF%

:: Call R script to summarize REF outputs
ECHO %startTime%%Time%: Running R script to summarize REF outputs...
%R_SCRIPT% %WORKING_DIR%scripts\SummarizeABM2016.R %INPUT_FILE_REF%

:: Create Visualizer
:: Parameters file 
SET PARAMETERS_FILE=%WORKING_DIR%runtime\parameters.csv

ECHO Key,Value > %PARAMETERS_FILE%
ECHO PROJECT_DIR,%PROJECT_DIR% >> %PARAMETERS_FILE%
ECHO WORKING_DIR,%WORKING_DIR% >> %PARAMETERS_FILE%
ECHO REF_DIR,%REF_DIR% >> %PARAMETERS_FILE%
ECHO REF_DIR_INP,%REF_DIR_INP% >> %PARAMETERS_FILE%
ECHO BASE_SUMMARY_DIR,%BASE_SUMMARY_DIR% >> %PARAMETERS_FILE%
ECHO BUILD_SUMMARY_DIR,%BUILD_SUMMARY_DIR% >> %PARAMETERS_FILE%
ECHO BASE_SCENARIO_NAME,%BASE_SCENARIO_NAME% >> %PARAMETERS_FILE%
ECHO BUILD_SCENARIO_NAME,%BUILD_SCENARIO_NAME% >> %PARAMETERS_FILE%
ECHO BASE_SAMPLE_RATE,%BASE_SAMPLE_RATE% >> %PARAMETERS_FILE%
ECHO BUILD_SAMPLE_RATE,%BUILD_SAMPLE_RATE% >> %PARAMETERS_FILE%
ECHO R_LIBRARY,%R_LIBRARY% >> %PARAMETERS_FILE%
ECHO OUTPUT_HTML_NAME,%OUTPUT_HTML_NAME% >> %PARAMETERS_FILE%
ECHO SHP_FILE_NAME,%SHP_FILE_NAME% >> %PARAMETERS_FILE%
ECHO IS_BASE_SURVEY,%IS_BASE_SURVEY% >> %PARAMETERS_FILE%
ECHO MAX_ITER,%MAX_ITER% >> %PARAMETERS_FILE%
ECHO geogXWalkDir,%WORKING_DIR%data >> %PARAMETERS_FILE%
ECHO mgraInputFile,%MGRA_INPUT_FILE% >> %PARAMETERS_FILE%

:: Call the R Script to process REF and BUILD output
:: #######################################
ECHO %startTime%%Time%: Running R script to process REF output...
%R_SCRIPT% %WORKING_DIR%scripts\workersByMAZ.R %PARAMETERS_FILE% TRUE

ECHO %startTime%%Time%: Running R script to process BUILD output...
%R_SCRIPT% %WORKING_DIR%scripts\workersByMAZ.R %PARAMETERS_FILE% FALSE

:: Call the master R script
:: ########################
ECHO %startTime%%Time%: Running R script to generate visualizer...
%R_SCRIPT% %WORKING_DIR%scripts\Master.R %PARAMETERS_FILE%
IF %ERRORLEVEL% EQU 11 (
   ECHO File missing error. Check error file in outputs.
   EXIT /b %errorlevel%
)
ECHO %startTime%%Time%: Dashboard creation complete...