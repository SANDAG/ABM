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
set HHMGR_IP=${household.server.host}

rem JVM memory allocations
set MEMORY_MTXMGR_MIN=${mtxmgr.memory.min}
set MEMORY_MTXMGR_MAX=${mtxmgr.memory.max}
set MEMORY_HHMGR_MIN=${hhmgr.memory.min}
set MEMORY_HHMGR_MAX=${hhmgr.memory.max}
set MEMORY_CLIENT_MIN=${client.memory.min}
set MEMORY_CLIENT_MAX=${client.memory.max}
set MEMORY_SPMARKET_MIN=${spmarket.memory.min}
set MEMORY_SPMARKET_MAX=${spmarket.memory.max}
set MEMORY_BIKELOGSUM_MIN=${bikelogsum.memory.min}
set MEMORY_BIKELOGSUM_MAX=${bikelogsum.memory.max}
set MEMORY_WALKLOGSUM_MIN=${walklogsum.memory.min}
set MEMORY_WALKLOGSUM_MAX=${walklogsum.memory.max}
set MEMORY_BIKEROUTE_MIN=${bikeroute.memory.min}
set MEMORY_BIKEROUTE_MAX=${bikeroute.memory.max}
set MEMORY_DATAEXPORT_MIN=${dataexport.memory.min}
set MEMORY_DATAEXPORT_MAX=${dataexport.memory.max}
set MEMORY_EMFAC_MIN=${emfac.memory.min}
set MEMORY_EMFAC_MAX=${emfac.memory.max}

rem set main property file name
set PROPERTIES_NAME=sandag_abm

rem all nodes need to map the scenario drive, currently mapped as x:
set MAPDRIVE=${MAPDRIVE}
rem set MAPDRIVEFOLDER=\\${master.node.name}\${map.folder}
rem uncomment next line if use T drive as data folder.  
rem !!!Note: much slower than a local data folder!!!
set MAPDRIVEFOLDER=${MAPDRIVEFOLDER}

rem account settings for remote access using psexec
set USERNAME=${USERNAME}
set PASSWORD=${PASSWORD}

rem location of mapAndRun.bat on remote machines
set MAPANDRUN=${MAPANDRUN}

rem set location of java
set JAVA_64_PATH=${JAVA_64_PATH}
set JAVA_32_PATH=${JAVA_32_PATH}
set JAVA_HOME_32=%JAVA_32_PATH%
set TRANSCAD_PATH=${TRANSCAD_PATH}

rem set location of python
set PYTHON_PATH=${PYTHON_PATH}
