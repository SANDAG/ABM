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