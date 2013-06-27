@echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%

call %PROJECT_DIRECTORY%\CTRampEnv.bat

rem ############  PARAMETERS  ############

rem ############  JPPF DRIVER  ############
set JPPF_LIB=%PROJECT_DIRECTORY%\application\sandag_abm_pb.jar
set CLASSPATH=%PROJECT_DIRECTORY%\config;%JPPF_LIB%

start %JAVA_64_PATH%\bin\java -server -Xmx16m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j-driver.properties -Djppf.config=jppf-driver.properties org.jppf.server.DriverLauncher
