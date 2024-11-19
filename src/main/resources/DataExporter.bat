set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set JAR_LOCATION=%PROJECT_DIRECTORY%/application

rem ### Connecting to Anaconda3 Environment
SET ANACONDA3_DIR=%CONDA_PREFIX%
SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_baydag\python.exe 

rem ### Call environment
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_baydag

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%                                        
rem ### Running Data Exporter on scenario
%PYTHON3% python\dataExporter\serialRun.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem ### Check for the Data Exporter output files
rem ### call %PROJECT_DIRECTORY%\bin\CheckOutput.bat %PROJECT_DIRECTORY% Exporter %ITERATION%
