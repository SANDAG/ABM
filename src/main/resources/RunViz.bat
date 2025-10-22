set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

ECHO Activate ActivitySim....
CALL %activate_uv_asim%

cd /d %PROJECT_DIRECTORY%

cd src/asim/visualizer/visualizer

CALL RunViz.bat 
