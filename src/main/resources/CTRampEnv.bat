rem this file has environment variables for CT-RAMP batch files


rem set ports
set JAVA_32_PORT=${java.32.port}
set MATRIX_MANAGER_PORT=${matrix.server.port}
set HH_MANAGER_PORT=${household.server.port}

rem set machine names
set MAIN=${master.node.name}
set NODE1=${node.1.name}
set NODE2=${node.2.name}


rem set IP addresses
set MAIN_IP=${master.node.ip}
set HHMGR_IP=${node.1.ip}


rem all nodes need to map the scenario drive, currently mapped as x:
set MAPDRIVE=x:
set MAPDRIVEFOLDER=\\${master.node.name}\${map.folder}

rem account settings for remote access using psexec
set USERNAME=joelf
set PASSWORD=B@seB@ll!

rem location of mapAndRun.bat on remote machines
set MAPANDRUN=${MAPANDRUN}

rem set location of java
set JAVA_64_PATH=C:\Progra~1\Java\jre7
set JAVA_32_PATH=C:\Progra~2\Java\jre7
set JAVA_HOME_32=%JAVA_32_PATH%
set TRANSCAD_PATH=${TRANSCAD_PATH}
