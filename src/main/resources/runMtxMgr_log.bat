rem @echo off

rem ### Declaring required environment variables
set JAVA_64_PATH = %1
set MEMORY_MTXMGR_MIN = %2
set MEMORY_MTXMGR_MAX = %3
set JAR_LOCATION = %4
set HOST_IP_ADDRESS = %5
set MATRIX_MANAGER_PORT = %6
set PROJECT_DIRECTORY = %7


rem ### Running matrix manager and redirecting output to {PROJECT_DIRECTORY}\logFiles\mtxMgrConsole.log
%JAVA_64_PATH%\bin\java -server -Xms%MEMORY_MTXMGR_MIN% -Xmx%MEMORY_MTXMGR_MAX% -Dlog4j.configuration=log4j_mtx.xml -Djava.library.path=%PROJECT_DIRECTORY%\application org.sandag.abm.ctramp.MatrixDataServer -hostname %HOST_IP_ADDRESS% -port %MATRIX_MANAGER_PORT% -ram 1500 -label "SANDAG Matrix Server" 2>&1 | %PROJECT_DIRECTORY%\application\GnuWin32\bin\tee.exe %PROJECT_DIRECTORY%\logFiles\mtxMgrConsole.log

rem ### Exit window
exit 0