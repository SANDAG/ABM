@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage

set SCENARIO_FOLDER=%1
set YEAR=%2
set CLUSTER=%3

@echo creating scenario folders
set FOLDERS=input application bin conf gisdk input_truck logFiles output python report sql uec
for %%i in (%FOLDERS%) do (
md %SCENARIO_FOLDER%\%%i)

@echo checking if single node
set RELEASE_DIR=%CLUSTER%
@echo RELEASE_DIR=%CLUSTER%

set SNODES=highlander scarlett
for %%i in (%SNODES%) do (
if %%i==%3 set RELEASE_DIR=local)

@echo \%RELEASE_DIR%\%YEAR%\application\

xcopy /Y .\%RELEASE_DIR%\%YEAR%\application\"*.*" %SCENARIO_FOLDER%\application
xcopy /Y .\%RELEASE_DIR%\%YEAR%\input_truck\"*.*" %SCENARIO_FOLDER%\input_truck
xcopy /Y/E .\%RELEASE_DIR%\%YEAR%\python\"*.*" %SCENARIO_FOLDER%\python
xcopy /Y/E .\%RELEASE_DIR%\%YEAR%\sql\"*.*" %SCENARIO_FOLDER%\sql
xcopy /Y .\%RELEASE_DIR%\%YEAR%\uec\"*.*" %SCENARIO_FOLDER%\uec
xcopy /Y .\%RELEASE_DIR%\%YEAR%\conf\"*.*" %SCENARIO_FOLDER%\conf
xcopy /Y .\%RELEASE_DIR%\%YEAR%\gisdk\"*.*" %SCENARIO_FOLDER%\gisdk
xcopy /Y .\%RELEASE_DIR%\%YEAR%\bin\"*.*" %SCENARIO_FOLDER%\bin

@echo copy year specific folders
xcopy /Y .\input\%YEAR%\"*.*" %SCENARIO_FOLDER%\input

@echo replace \ with \\
set SCENARIO_FOLDER2=%SCENARIO_FOLDER:\=\\%

@echo subsitute strings in GISDK
call BatchSubstitute.bat WORKPATH %SCENARIO_FOLDER2% %SCENARIO_FOLDER%\gisdk\sandag_abm_generic.lst>%SCENARIO_FOLDER%\gisdk\sandag_abm.lst
call BatchSubstitute.bat WORKPATH %SCENARIO_FOLDER2% %SCENARIO_FOLDER%\gisdk\sandag_abm_master_generic.rsc>%SCENARIO_FOLDER%\gisdk\sandag_abm_master.rsc
del %SCENARIO_FOLDER%\gisdk\sandag_abm_generic.lst
del %SCENARIO_FOLDER%\gisdk\sandag_abm_master_generic.rsc

:usage
@echo Usage: %0 ^<scenario_folder^> ^<year^> ^<cluster^>