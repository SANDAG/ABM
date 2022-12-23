set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set JAR_LOCATION=%PROJECT_DIRECTORY%/application

rem ### Connecting to Anaconda3 Environment
rem CONDA_PREFIX is a system environment variable that points to the location of Anaconda3
set ENV=%CONDA_PREFIX%
call %ENV%\Scripts\activate.bat %ENV%

rem ### Checking if Data Exporter environment exists
rem ### Otherwise creates environment
set EXPORT_ENV=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\python\dataExporter\environment.yml
call conda env list | find /i "abmDataExporter"
if not errorlevel 1 (
	call conda env update --name abmDataExporter --file %EXPORT_ENV%
	call activate abmDataExporter
) else (
	call conda env create -f %EXPORT_ENV%
	call activate abmDataExporter 
)

rem ### Running Data Exporter on scenario
python %PROJECT_DRIVE%%PROJECT_DIRECTORY%\python\dataExporter\serialRun.py %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem ### Check for the Data Exporter output files
call %PROJECT_DIRECTORY%\bin\CheckOutput.bat %PROJECT_DIRECTORY% Exporter %ITERATION%

rem ### Exiting all Anaconda3 environments
call conda deactivate
call conda deactivate