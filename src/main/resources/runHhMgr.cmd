rem @echo off

rem %1 is the project directory
set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%

call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

rem JVM allocations
set MEMORY_HHMGR_MIN=%MEMORY_HHMGR_MIN%
set MEMORY_HHMGR_MAX=%MEMORY_HHMGR_MAX%

rem Running on SAG02
set HOST_IP_ADDRESS=%HHMGR_IP%

rem 1129 used to calibrate the 20% sample
set HOST_PORT=%HH_MANAGER_PORT%

rem (X:) is mapped to \\w-ampdx-d-sag01\C
rem set DRIVE=%MAPDRIVE%
set DRIVE=%PROJECT_DRIVE%

rem ### Name the project directory.  This directory will hava data and runtime subdirectories
set RUNTIME=%DRIVE%%PROJECT_DIRECTORY%
set CONFIG=%RUNTIME%/conf

set JAR_LOCATION=%RUNTIME%/application

set LIB_JAR_PATH=%JAR_LOCATION%\*

rem ### Define the CLASSPATH environment variable for the classpath needed in this model run.
set CLASSPATH=%CONFIG%;%RUNTIME%;%LIB_JAR_PATH%;%JAR_LOCATION%\*


rem ### Save the name of the PATH environment variable, so it can be restored at the end of the model run.
set OLDPATH=%PATH%

rem ### Change the PATH environment variable so that JAVA_HOME is listed first in the PATH.
rem ### Doing this ensures that the JAVA_HOME path we defined above is the on that gets used in case other java paths are in PATH.
set PATH=%JAVA_64_PATH%\bin;%OLDPATH%


rem ### Change current directory to RUNTIME, and issue the java command to run the model.
start %JAVA_64_PATH%/bin/java -server -Xms%MEMORY_HHMGR_MIN% -Xmx%MEMORY_HHMGR_MAX% -cp "%CLASSPATH%" -Dlog4j.configuration=log4j_hh.xml org.sandag.abm.application.SandagHouseholdDataManager2 -hostname %HOST_IP_ADDRESS% -port %HOST_PORT%
rem java -Xdebug -Xrunjdwp:transport=dt_socket,address=1044,server=y,suspend=y -server -Xmx12000m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j_hh.xml org.sandag.abm.application.SandagHouseholdDataManager2 -hostname %HOST_IP_ADDRESS%
 
rem ### restore saved environment variable values, and change back to original current directory
set PATH=%OLDPATH%

