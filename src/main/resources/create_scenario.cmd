@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage

set SCENARIO_FOLDER=%1
set YEAR=%2
set NETWORKDIR=%3

@echo creating scenario folders
set FOLDERS=input application bin conf input_truck logFiles output python report sql uec
for %%i in (%FOLDERS%) do (
md %SCENARIO_FOLDER%\%%i)

rem grant full permissions to scenario folder
cacls %SCENARIO_FOLDER% /t /e /g Everyone:f

rem copy master server-config.csv to a scenario folder
xcopy /Y T:\ABM\release\ABM\config\server-config.csv %SCENARIO_FOLDER%\conf

rem setup model folders
xcopy /Y .\common\application\"*.*" %SCENARIO_FOLDER%\application
xcopy /Y/E .\common\python\"*.*" %SCENARIO_FOLDER%\python
xcopy /Y/E .\common\sql\"*.*" %SCENARIO_FOLDER%\sql
xcopy /Y .\common\uec\"*.*" %SCENARIO_FOLDER%\uec
xcopy /Y .\common\bin\"*.*" %SCENARIO_FOLDER%\bin
xcopy /Y .\conf\%YEAR%\"*.*" %SCENARIO_FOLDER%\conf
xcopy /Y .\common\output\"*.*" %SCENARIO_FOLDER%\output

@echo assemble inputs
del %SCENARIO_FOLDER%\input /q
rem copy network inputs
call copy_networks.cmd %NETWORKDIR% %SCENARIO_FOLDER%\input
rem copy pop, hh, landuse, and other input files
xcopy /Y .\input\%YEAR%\"*.*" %SCENARIO_FOLDER%\input
rem copy common geography files to input folder
xcopy /Y .\common\input\geography\"*.*" %SCENARIO_FOLDER%\input
rem copy ctm paramter tables to input folder
xcopy /Y .\common\input\ctm\"*.*" %SCENARIO_FOLDER%\input
rem copy common model files to input folder
xcopy /Y .\common\input\model\"*.*" %SCENARIO_FOLDER%\input
rem copy common truck files to input_truck folder
xcopy /Y .\common\input\truck\"*.*" %SCENARIO_FOLDER%\input_truck
rem copy airport input files
xcopy /Y .\common\input\airports\"*.*" %SCENARIO_FOLDER%\input
rem copy ei input files
xcopy /Y .\common\input\ei\"*.*" %SCENARIO_FOLDER%\input
rem copy ie input files
xcopy /Y .\common\input\ie\"*.*" %SCENARIO_FOLDER%\input
rem copy ee input files
xcopy /Y .\common\input\ee\"*.*" %SCENARIO_FOLDER%\input
rem copy emfact input files
xcopy /Y .\common\input\emfact\"*.*" %SCENARIO_FOLDER%\input
rem copy special event input files
xcopy /Y .\common\input\specialevent\"*.*" %SCENARIO_FOLDER%\input
rem copy xborder input files
xcopy /Y .\common\input\xborder\"*.*" %SCENARIO_FOLDER%\input
rem copy visitor input files
xcopy /Y .\common\input\visitor\"*.*" %SCENARIO_FOLDER%\input

@echo init emme folder
call init_emme.cmd %SCENARIO_FOLDER%

:usage

@echo Usage: %0 ^<scenario_folder^> ^<year^> ^<network^>
@echo If 3rd parameter is empty, default network inputs in standard release are used