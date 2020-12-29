@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage
if "%4"=="" goto usage

set STUDY_FOLDER=%1
set NETWORKDIR=%2
set NETWORKYEAR=%3
set DefaultNETWORKDIR=%4

@echo creating study folder
md %STUDY_FOLDER%\network_build\%NETWORKYEAR%

cacls %STUDY_FOLDER% /t /e /g Everyone:f

@echo /Y/E %NETWORKDIR% %STUDY_FOLDER%\network_build\%NETWORKYEAR%
xcopy /Y/E %NETWORKDIR% %STUDY_FOLDER%\network_build\%NETWORKYEAR%

@echo /Y/E %DefaultNETWORKDIR%\other\"*.*" %STUDY_FOLDER%\network_build
xcopy /Y/E %DefaultNETWORKDIR%\other\"*.*" %STUDY_FOLDER%\network_build


:usage

@echo Usage: %0 ^<study_folder^> ^<studynetwork^> ^<studynetworkyear^> ^<defaultnetwork^>

