rem Run EMFAC2011 after scenario is loaded into database
rem Takes 3 arguments: 1-prject drive 2-porject directory 3-scenario_id
rem EMFAC2011 results are written to a default location-%PROJECT_DRIVE%\%PROJECT_DIRECTORY%\output

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SCENARIO=%3

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat
python.exe %PROJECT_DIRECTORY%\bin\emfac2011_abm.py %SCENARO% %PROJECT_DRIVE%%PROJECT_DIRECTORY%\output