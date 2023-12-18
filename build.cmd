@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage

set RELEASE_FOLDER=%1
set VERSION=%2

set ROOT=%RELEASE_FOLDER%\%VERSION%

if not exist %RELEASE_FOLDER% goto notexist

if exist %ROOT% ???

if not exist %ROOT% mkdir %ROOT%

@REM Copy scenario creation scripts
for /f "tokens=*" %%i in (build_create_scenario_scripts.txt) DO (
    xcopy /Y .\src\main\resources\%%i %ROOT%
)

@REM Copy application files
if not exist %ROOT%\common\application mkdir %ROOT%\common\application
xcopy /Y .\src\main\resources\"*.dll" %ROOT%\common\application
xcopy /Y .\src\main\resources\"*.jar" %ROOT%\common\application

@REM Copy bin files
if not exist %ROOT%\common\bin mkdir %ROOT%\common\bin
xcopy /Y /exclude:build_create_scenario_scripts.txt .\src\main\resources\"*.bat" %ROOT%\common\bin
xcopy /Y /exclude:build_create_scenario_scripts.txt .\src\main\resources\"*.cmd" %ROOT%\common\bin
xcopy /Y /exclude:build_create_scenario_scripts.txt .\src\main\resources\"*.py" %ROOT%\common\bin
xcopy /Y /exclude:build_create_scenario_scripts.txt .\src\main\resources\"*.exe" %ROOT%\common\bin
xcopy /Y /exclude:build_create_scenario_scripts.txt .\src\main\resources\"*.dat" %ROOT%\common\bin

@REM Copy configuration files
if not exist %ROOT%\common\conf mkdir %ROOT%\common\conf
xcopy /Y .\src\main\resources\"*.properties" %ROOT%\common\conf
xcopy /Y .\src\main\resources\"*.xml" %ROOT%\common\conf
xcopy /Y .\src\main\resources\"*.csv" %ROOT%\common\conf

@REM Copy input files
if not exist %ROOT%\common\input mkdir %ROOT%\common\input
xcopy /Y/S/E .\input %ROOT%\common\input

@REM Copy python files
if not exist %ROOT%\common\python mkdir %ROOT%\common\python
xcopy /Y/S/E .\src\main\python %ROOT%\common\python
xcopy /Y/S/E .\src\main\emme %ROOT%\common\python\emme

@REM Copy ActivitySim files
if not exist %ROOT%\common\src\asim mkdir %ROOT%\common\src\asim
xcopy /Y/S/E .\src\asim %ROOT%\common\src\asim

@REM Copy git folder
@REM TODO: Create git yaml here instead of scenario creation
xcopy /Y/S/E/H/I .\.git %ROOT%\common\src\asim\.git

goto done

:usage
@echo Usage: %0 ^<release_folder^> ^<version^>
goto done

:notexist
@echo %RELEASE_FOLDER% does not exist

:done