set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set JAR_LOCATION=%PROJECT_DIRECTORY%/application

rem ### Connecting to Anaconda3 Environment
set ENV=C:\ProgramData\Anaconda3
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

rem ### Exiting all Anaconda3 environments
call conda deactivate
call conda deactivate