@echo off
setlocal enableDelayedExpansion

if "%1"=="" goto usage
if "%2"=="" goto usage

set RELEASE_FOLDER=%1
set VERSION=%2

set ROOT=%RELEASE_FOLDER%\%VERSION%

if not exist %RELEASE_FOLDER% goto notexist

if exist %ROOT% (
    set /p "UPDATE=%ROOT% already exists. Do you want to update it? (Y/N) "
    if "!UPDATE!" == "y" goto build
    if "!UPDATE!" == "Y" goto build
    goto done
) else (
    mkdir %ROOT%
)

:build

@REM Delete common directory first so deleted files are not preserved
if exist %ROOT%\common rmdir /s /q %ROOT%\common

@REM Copy scenario creation scripts
for /f "tokens=*" %%i in (build_create_scenario_scripts.txt) DO (
    xcopy /Y .\src\main\resources\%%i %ROOT%
)

@REM Copy application files
if not exist %ROOT%\common\application mkdir %ROOT%\common\application
xcopy /Y .\src\main\resources\"*.dll" %ROOT%\common\application
xcopy /Y .\src\main\resources\application\"*.jar" %ROOT%\common\application
if not exist %ROOT%\common\application\GnuWin32 mkdir %ROOT%\common\application\GnuWin32
xcopy /Y/S/E .\src\main\resources\application\GnuWin32 %ROOT%\common\application\GnuWin32

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
xcopy /Y .\src\main\resources\"*.yaml" %ROOT%\common\conf
xcopy /Y .\src\main\resources\"*.xml" %ROOT%\common\conf
xcopy /Y .\src\main\resources\"*.csv" %ROOT%\common\conf

@REM Copy UEC files
if not exist %ROOT%\common\uec mkdir %ROOT%\common\uec
xcopy /Y .\uec\"*.xls" %ROOT%\common\uec
xcopy /Y .\uec\"*.csv" %ROOT%\common\uec

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

@REM Copy CVM files
if not exist %ROOT%\common\src\asim-cvm mkdir %ROOT%\common\src\asim-cvm
xcopy /Y/S/E .\src\asim-cvm %ROOT%\common\src\asim-cvm

@REM get git info
@echo get git info
set GIT_FOLDER=.\.git
set GIT_FILE=%ROOT%\common\git_info.yaml
type nul>%GIT_FILE%
WHERE git
IF NOT ERRORLEVEL 0 GOTO NOGIT
echo|set /p="branch: " >> %GIT_FILE%
git --git-dir=%GIT_FOLDER% branch --show-current >> %GIT_FILE%
echo|set /p="commit: " >> %GIT_FILE%
git --git-dir=%GIT_FOLDER% rev-parse HEAD >> %GIT_FILE%
GOTO GITDONE
:NOGIT
set /p GIT_HEAD=< %GIT_FOLDER%\HEAD
for %%A in (%GIT_HEAD%) do set GIT_BRANCH=%%~nxA
set /p GIT_COMMIT=< %GIT_FOLDER%\refs\heads\%GIT_BRANCH%
echo branch: %GIT_BRANCH% >> %GIT_FILE%
echo commit: %GIT_COMMIT% >> %GIT_FILE%
:GITDONE

@REM create build info
set LOG_FILE=%ROOT%\build_info.txt
type nul>%LOG_FILE%
echo Last updated: %date% >> %LOG_FILE%
echo User: %USERNAME% >> %LOG_FILE%

goto done

:usage
@echo Usage: %0 ^<release_folder^> ^<version^>
goto done

:notexist
@echo %RELEASE_FOLDER% does not exist

:done