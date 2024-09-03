rem create_scenario.cmd T:\projects\sr14\version_14_1_x\abm_runs\2016 2016 T:\projects\sr14\version_14_1_x\network_build\2016 4.4.0

@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage
if "%4"=="" goto usage
if "%5"=="" goto usage
if "%6"=="" goto usage

set SCENARIO_FOLDER=%1
set YEAR=%2
set NETWORKDIR=%3
set EMME_VERSION=%4
set LANDUSE_INPUT_PATH=%5
set SUFFIX=%6

@echo creating scenario folders
set FOLDERS=input application bin conf logFiles output python report sql uec analysis input_checker src src\asim src\asim-cvm
for %%i in (%FOLDERS%) do (
md %SCENARIO_FOLDER%\%%i)

rem grant full permissions to scenario folder
cacls %SCENARIO_FOLDER% /t /e /g Everyone:f

rem copy master server-config.csv to a scenario folder
rem to make local copy of server configuration file effective, user needs to rename it to server-config-local.csv
xcopy /Y T:\ABM\release\ABM\config\server-config.csv %SCENARIO_FOLDER%\conf

rem setup model folders
xcopy /Y .\common\application\"*.*" %SCENARIO_FOLDER%\application
xcopy /E/Y/i .\common\application\GnuWin32\"*.*" %SCENARIO_FOLDER%\application\GnuWin32
xcopy /Y/E .\common\python\"*.*" %SCENARIO_FOLDER%\python
xcopy /Y/E .\common\sql\"*.*" %SCENARIO_FOLDER%\sql
xcopy /Y .\common\uec\"*.*" %SCENARIO_FOLDER%\uec
xcopy /Y .\common\bin\"*.*" %SCENARIO_FOLDER%\bin
rem xcopy /Y .\conf\%YEAR%\"*.*" %SCENARIO_FOLDER%\conf
xcopy /Y .\common\conf\"*.*" %SCENARIO_FOLDER%\conf
xcopy /Y .\common\output\"*.*" %SCENARIO_FOLDER%\output
xcopy /Y/s/E .\common\input\input_checker\"*.*" %SCENARIO_FOLDER%\input_checker
xcopy /Y/s/E .\common\src\asim\"*.*" %SCENARIO_FOLDER%\src\asim
xcopy /Y/s/E .\common\src\asim-cvm\"*.*" %SCENARIO_FOLDER%\src\asim-cvm

@echo assemble inputs
del %SCENARIO_FOLDER%\input /q
rem copy pop, hh, landuse, and other input files
xcopy /s/Y .\input\%YEAR%%SUFFIX%\"*.*" %SCENARIO_FOLDER%\input
rem copy common geography files to input folder
xcopy /Y .\common\input\geography\"*.*" %SCENARIO_FOLDER%\input
xcopy /Y .\common\input\geography\mgra\"*.*" %SCENARIO_FOLDER%\input
xcopy /Y .\common\input\geography\pmsa\"*.*" %SCENARIO_FOLDER%\input
xcopy /Y .\common\input\geography\taz\"*.*" %SCENARIO_FOLDER%\input
rem copy common model files to input folder
xcopy /Y .\common\input\model\"*.*" %SCENARIO_FOLDER%\input
rem copy ei input files
xcopy /Y .\common\input\ei\"*.*" %SCENARIO_FOLDER%\input
rem copy ie input files
xcopy /Y .\common\input\ie\"*.*" %SCENARIO_FOLDER%\input
rem copy ee input files
xcopy /Y .\common\input\ee\"*.*" %SCENARIO_FOLDER%\input
rem copy special event input files
xcopy /Y .\common\input\specialevent\"*.*" %SCENARIO_FOLDER%\input
rem copy xborder input files
xcopy /Y .\common\input\xborder\"*.*" %SCENARIO_FOLDER%\input
rem copy input checker config files
xcopy /Y .\common\input\input_checker\"*.*" %SCENARIO_FOLDER%
rem copy network inputs
call copy_networks.cmd %NETWORKDIR% %SCENARIO_FOLDER%\input

rem copying land use inputs
xcopy /Y %LANDUSE_INPUT_PATH%\"households.csv" %SCENARIO_FOLDER%\input
xcopy /Y %LANDUSE_INPUT_PATH%\"persons.csv" %SCENARIO_FOLDER%\input
xcopy /Y %LANDUSE_INPUT_PATH%\"mgra15_based_input"%YEAR%".csv" %SCENARIO_FOLDER%\input

rem copy analysis templates
@echo copy analysis templates
if %YEAR%==2016 (xcopy /Y/S   .\common\input\template\validation\2016\"*.*" %SCENARIO_FOLDER%\analysis\validation\)
if %YEAR%==2018 (xcopy /Y/S   .\common\input\template\validation\2018\"*.*" %SCENARIO_FOLDER%\\analysis\validation\)
if %YEAR%==2022 (xcopy /Y/S   .\common\input\template\validation\2022\"*.*" %SCENARIO_FOLDER%\analysis\validation\) 
xcopy /Y/S   .\common\input\template\summary\"*.*" %SCENARIO_FOLDER%\analysis\summary\     

rem copy git info
xcopy /Y .\common\git_info.yaml %SCENARIO_FOLDER%

rem populate scenario year into sandag_abm.properties
set PROP_FILE=%SCENARIO_FOLDER%\conf\sandag_abm.properties
@REM set TEMP_FILE=%SCENARIO_FOLDER%\conf\temp.properties
@REM set RAW_YEAR=%YEAR:nb=%
@REM type nul>%TEMP_FILE%
echo %PROP_FILE%
@REM for /f "USEBACKQ delims=" %%A in (`type "%PROP_FILE%" ^| find /V /N ""`) do (
@REM   set ln=%%A
@REM   setlocal enableDelayedExpansion
@REM   set ln=!ln:${year}=%RAW_YEAR%!
@REM   set ln=!ln:${year_build}=%YEAR%!
@REM   set ln=!ln:*]=!
@REM   echo(!ln!>>%TEMP_FILE%
@REM   endlocal
@REM )
@REM del %PROP_FILE%
@REM move %TEMP_FILE% %PROP_FILE%
python .\common\python\update_properties.py %PROP_FILE% %YEAR% %SUFFIX%

@echo init emme folder
call init_emme.cmd %SCENARIO_FOLDER% %EMME_VERSION%

:usage

@echo Usage: %0 ^<scenario_folder^> ^<year^> ^<network^> ^<emme_version^> ^<landuse_input_path^> ^<year_suffix^>
@echo If 3rd parameter is empty, default network inputs in standard release are used



