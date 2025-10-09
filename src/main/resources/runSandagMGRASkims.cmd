rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%

CALL %activate_uv_asim%

rem   rem build walk skims
ECHO Running 2-zone skimming procedure... 
python src/asim/scripts/resident/2zoneSkim.py %PROJECT_DIRECTORY% || goto error
