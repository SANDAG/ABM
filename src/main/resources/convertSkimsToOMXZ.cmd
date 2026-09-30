set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

CALL %activate_uv_asim_151%

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%
cd output\skims
REM `wring omx` creates omxz files that have the same values as the omx files but are stored more efficiently
CALL wring omx
