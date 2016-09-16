@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage

set SCENARIO_FOLDER=%1
set YEAR=%2
set CLUSTER=%3
set NETWORKDIR=%4

@echo creating scenario folders
set FOLDERS=input application bin conf gisdk input_truck logFiles output python report sql uec
for %%i in (%FOLDERS%) do (
md %SCENARIO_FOLDER%\%%i)

@echo checking if single node
set RELEASE_DIR=%CLUSTER%
@echo RELEASE_DIR=%CLUSTER%

set SNODES=local
for %%i in (%SNODES%) do (
if %%i==%3 set RELEASE_DIR=local)

@echo \%RELEASE_DIR%\%YEAR%\conf\

xcopy /Y .\common\application\"*.*" %SCENARIO_FOLDER%\application
xcopy /Y .\common\input_truck\"*.*" %SCENARIO_FOLDER%\input_truck
xcopy /Y/E .\common\python\"*.*" %SCENARIO_FOLDER%\python
xcopy /Y/E .\common\sql\"*.*" %SCENARIO_FOLDER%\sql
xcopy /Y .\common\uec\"*.*" %SCENARIO_FOLDER%\uec
xcopy /Y .\common\gisdk\"*.*" %SCENARIO_FOLDER%\gisdk
xcopy /Y .\%RELEASE_DIR%\%YEAR%\conf\"*.*" %SCENARIO_FOLDER%\conf
xcopy /Y .\%RELEASE_DIR%\%YEAR%\bin\"*.*" %SCENARIO_FOLDER%\bin

@echo copy year specific folders
del %SCENARIO_FOLDER%\input /q
xcopy /Y .\input\%YEAR%\"*.*" %SCENARIO_FOLDER%\input

@echo replace \ with \\
set SCENARIO_FOLDER2=%SCENARIO_FOLDER:\=\\%

@echo subsitute strings in GISDK
call BatchSubstitute.bat WORKPATH %SCENARIO_FOLDER2% %SCENARIO_FOLDER%\gisdk\sandag_abm_generic.lst>%SCENARIO_FOLDER%\gisdk\sandag_abm.lst
call BatchSubstitute.bat WORKPATH %SCENARIO_FOLDER2% %SCENARIO_FOLDER%\gisdk\gui_generic.rsc>%SCENARIO_FOLDER%\gisdk\gui.rsc
del %SCENARIO_FOLDER%\gisdk\sandag_abm_generic.lst
del %SCENARIO_FOLDER%\gisdk\sandag_abm_master_generic.rsc
del %SCENARIO_FOLDER%\gisdk\gui_generic.rsc

@echo copy network inputs
call copy_networks.cmd %NETWORKDIR% %SCENARIO_FOLDER%\input

rem @echo off & setlocal
rem set t=%SCENARIO_FOLDER%
rem for /f "tokens=1,2 delims=:" %%a in ("%t%") do (
rem    set x=%%a
rem    set y=%%b
rem    )

rem @echo check AT and Transit networks consistency

rem @echo call %SCENARIO_FOLDER%\bin\checkAtTransitNetworkConsistency.cmd %x%: %y%
rem call %SCENARIO_FOLDER%\bin\checkAtTransitNetworkConsistency.cmd %x%: %y%
rem @echo -----------------------------------------------------------------
rem @echo If error message logged out in DOS window, CHECK your SANDAG_Bike_Node.dbf and tapcov.dbf.
rem @echo If you used default inputs, check in release/input;
rem @echo otherwise, check in the network folder you provided %NETWORKDIR%.
rem @echo -----------------------------------------------------------------

:usage

@echo Usage: %0 ^<scenario_folder^> ^<year^> ^<local/(aztec,gaucho,wildcat)^> ^<network^>
@echo If 4th parameter is empty, default network inputs in standard release are used