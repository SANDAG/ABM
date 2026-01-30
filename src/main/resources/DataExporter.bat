set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set JAR_LOCATION=%PROJECT_DIRECTORY%/application

rem ### Call environment
CALL %activate_uv_asim%

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%                                        
rem ### Running Data Exporter on scenario
python python\dataExporter\serialRun.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem ### Check for the Data Exporter output files
rem ### call %PROJECT_DIRECTORY%\bin\CheckOutput.bat %PROJECT_DIRECTORY% Exporter %ITERATION%
