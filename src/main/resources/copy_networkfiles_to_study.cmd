@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage

set STUDY_FOLDER=%1
set NETWORKDIR=%2

@echo creating study folder
md %STUDY_FOLDER%\network_build
cacls %STUDY_FOLDER% /t /e /g Everyone:f

@echo /Y/E %NETWORKDIR%\"*.*" %STUDY_FOLDER%\network_build
xcopy /Y/E %NETWORKDIR%\"*.*" %STUDY_FOLDER%\network_build

:usage

@echo Usage: %0 ^<study_folder^> ^<studynetwork^>
