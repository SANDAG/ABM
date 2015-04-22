rem Run EMFAC2014 after scenario is loaded into database
rem Takes 3 arguments: 1-prject drive 2-porject directory 3-scenario_id 4-SB375 switch
rem EMFAC2014 results are written to a default location-%PROJECT_DRIVE%\%PROJECT_DIRECTORY%\output

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SCENARIO=%3
set SB375=%4

call %PROJECT_DRIVE%\%PROJECT_DIRECTORY%\bin\CTRampEnv.bat

cd %PROJECT_DRIVE%\%PROJECT_DIRECTORY%\python

python.exe emfac2014_abm.py %SCENARIO% Annual %SB375% %PROJECT_DRIVE%\%PROJECT_DIRECTORY%\output
python.exe emfac2014_abm.py %SCENARIO% Summer %SB375% %PROJECT_DRIVE%\%PROJECT_DIRECTORY%\output
python.exe emfac2014_abm.py %SCENARIO% Winter %SB375% %PROJECT_DRIVE%\%PROJECT_DIRECTORY%\output
