rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: SET UP PATHS
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
SET ANACONDA3_DIR=%CONDA_PREFIX%
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

CALL %CONDA3_ACT% asim_baydag
SET PYTHON3=%ANACONDA3_DIR%\envs\asim_baydag\python.exe


%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem   rem build walk skims
ECHO Running 2-zone skimming procedure... 
%PYTHON3% src/asim/scripts/resident/2zoneSkim.py %PROJECT_DIRECTORY% || goto error

@REM python %PROJECT_DRIVE%%PROJECT_DIRECTORY%\python\calculate_micromobility.py  --properties_file %PROJECT_DRIVE%%PROJECT_DIRECTORY%\conf\sandag_abm.properties --outputs_directory %PROJECT_DRIVE%%PROJECT_DIRECTORY%\output --inputs_parent_directory %PROJECT_DRIVE%%PROJECT_DIRECTORY%

:done
rem ### restore saved environment variable values, and change back to original current directory
set JAVA_PATH=%OLDJAVAPATH%
set PATH=%OLDPATH%
