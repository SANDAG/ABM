rem this file has environment variables for CT-RAMP batch files

rem set ports
set MATRIX_MANAGER_PORT=1191
set HH_MANAGER_PORT=1129

rem set single node index
set SNODE=yes

rem set machine names
set MAIN=localhost
set NODE1=localhost
set NODE2=${node.2.name}
set NODE3=${node.3.name}


rem set IP addresses
set MAIN_IP=127.0.0.1
set HHMGR_IP=127.0.0.1

rem JVM memory allocations
set MEMORY_MTXMGR_MIN=60000m
set MEMORY_MTXMGR_MAX=70000m
set MEMORY_HHMGR_MIN=1000m
set MEMORY_HHMGR_MAX=35000m
set MEMORY_CLIENT_MIN=90000m
set MEMORY_CLIENT_MAX=130000m
set MEMORY_SPMARKET_MIN=30000m
set MEMORY_SPMARKET_MAX=140000m
set MEMORY_BIKELOGSUM_MIN=20000m
set MEMORY_BIKELOGSUM_MAX=30000m
set MEMORY_WALKLOGSUM_MIN=20000m
set MEMORY_WALKLOGSUM_MAX=30000m
set MEMORY_BIKEROUTE_MIN=20000m
set MEMORY_BIKEROUTE_MAX=30000m
rem set MEMORY_DATAEXPORT_MIN=${dataexport.memory.min}
rem set MEMORY_DATAEXPORT_MAX=${dataexport.memory.max}
set MEMORY_EMFAC_MIN=10000m
set MEMORY_EMFAC_MAX=15000m
set MEMORY_VALIDATE_MIN=10000m
set MEMORY_VALIDATE_MAX=15000m

rem set main property file name
set PROPERTIES_NAME=sandag_abm

rem all nodes need to map the scenario drive, currently mapped as x:
set MAPDRIVE=T:
rem set MAPDRIVEFOLDER=\\${master.node.name}\${map.folder}
rem uncomment next line if use T drive as data folder.  
rem !!!Note: much slower than a local data folder!!!
set MAPDRIVEFOLDER=\\sandag.org\transdata

rem account settings for remote access using psexec
set USERNAME=XX
set PASSWORD=XX

rem location of mapAndRun.bat on remote machines
set MAPANDRUN=mapAndRun.bat

rem set location of java
set JAVA_64_PATH=C:\\Progra~1\\Java\\jre1.8.0_162