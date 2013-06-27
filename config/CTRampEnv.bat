rem this file has environment variables for CT-RAMP batch files


rem set ports
set JAVA_32_PORT=1190
set MATRIX_MANAGER_PORT=1191
set HH_MANAGER_PORT=1129

rem set machine names
set MAIN=MUSTANG
set NODE1=GALAXIE
set NODE2=COBRA
set NODE3=

rem set IP addresses
set MAIN_IP=172.16.34.40
set NODE1_IP=172.16.34.42

rem all nodes need to map the scenario drive, currently mapped as x:
set MAPDRIVE=x:
set MAPDRIVEFOLDER=\\MUSTANG\MustangD

rem account settings for remote access using psexec
set USERNAME=joelf
set PASSWORD=BluePen!

rem location of mapAndRun.bat on remote machines
set MAPANDRUN=e:\remoteExecution\mapAndRun.bat

rem set location of java
set JAVA_64_PATH=C:\Progra~1\Java\jdk1.6.0_26
set JAVA_32_PATH="C:\Program Files (x86)\Java\jre6"
set JAVA_HOME_32=%JAVA_32_PATH%
set TRANSCAD_PATH=C:\Progra~2\TransCAD50
