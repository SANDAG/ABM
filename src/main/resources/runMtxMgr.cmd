rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2


%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem call ctramp properties
call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

rem JVM allocations
set MEMORY_MTXMGR_MIN=%MEMORY_MTXMGR_MIN%
set MEMORY_MTXMGR_MAX=%MEMORY_MTXMGR_MAX%

rem Run the matrix manager on SAG1
set HOST_IP_ADDRESS=%MAIN_IP%

rem kill java tasks
taskkill /F /IM java.exe

rem ### Name the project directory.  This directory will hava data and runtime subdirectories
set CONFIG=%PROJECT_DIRECTORY%/conf

set JAR_LOCATION=%PROJECT_DIRECTORY%/application
set LIB_JAR_PATH=%JAR_LOCATION%\*

rem ### Define the CLASSPATH environment variable for the classpath needed in this model run.
set OLDCLASSPATH=%CLASSPATH%
set OLDPATH=%PATH%
set CLASSPATH=%CONFIG%;%JAR_LOCATION%\*

rem java -Dname=p%2 -Xdebug -Xrunjdwp:transport=dt_socket,address=1049,server=y,suspend=y -server -Xms8000m -Xmx8000m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j_mtx.xml org.sandag.abm.ctramp.MatrixDataServer -hostname %HOST_IP_ADDRESS% -port %HOST_MATRIX_PORT% -label "SANDAG Matrix Sever"
start %JAVA_64_PATH%\bin\java -server -Xms%MEMORY_MTXMGR_MIN% -Xmx%MEMORY_MTXMGR_MAX% -Dlog4j.configuration=log4j_mtx.xml -Djava.library.path=%JAR_LOCATION% org.sandag.abm.ctramp.MatrixDataServer -hostname %HOST_IP_ADDRESS% -port %MATRIX_MANAGER_PORT% -ram 1500 -label "SANDAG Matrix Server"

set CLASSPATH=%OLDCLASSPATH%
set PATH=%OLDPATH%
