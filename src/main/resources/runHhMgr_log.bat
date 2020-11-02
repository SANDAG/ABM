rem @echo off

rem ### Declaring required environment variables
set JAVA_64_PATH = %1
set MEMORY_HHMGR_MIN = %2
set MEMORY_HHMGR_MAX = %3
set CLASSPATH = %4;%5;%6;%7
set HOST_IP_ADDRESS = %8
set HOST_PORT = %9
shift
set RUNTIME = %9


rem ### Running household manager and redirecting output to {PROJECT_DIRECTORY}\logFiles\hhMgrConsole.log
2>&1 (%JAVA_64_PATH%/bin/java -server -Xms%MEMORY_HHMGR_MIN% -Xmx%MEMORY_HHMGR_MAX% -cp "%CLASSPATH%" -Dlog4j.configuration=log4j_hh.xml org.sandag.abm.application.SandagHouseholdDataManager2 -hostname %HOST_IP_ADDRESS% -port %HOST_PORT%) | %RUNTIME%\application\GnuWin32\bin\tee.exe %RUNTIME%\logFiles\hhMgrConsole.log

rem ### Exit window
exit 0