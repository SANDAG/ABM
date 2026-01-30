ECHO ON

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

ECHO Activate asim_140....
CALL %activate_uv_asim%

set MKL_NUM_THREADS=1
set MKL=1

python python/hwyShapeExport.py %PROJECT_DIRECTORY%