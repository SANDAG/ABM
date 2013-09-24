rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%

call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

rem ############  PARAMETERS  ############

rem ############  JPPF DRIVER  ############
SET JPPF_LIB=%PROJECT_DIRECTORY%\application\jppf-server-2.5.jar;%PROJECT_DIRECTORY%\application\jppf-common-2.5.jar;%PROJECT_DIRECTORY%\application\jppf-common-node-2.5.jar;%PROJECT_DIRECTORY%\application\crowbar-unknown.jar
set CLASSPATH=%PROJECT_DIRECTORY%\conf;%JPPF_LIB%

start %JAVA_64_PATH%\bin\java -server -Xmx16m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j-driver.properties -Djppf.config=jppf-driver.properties org.jppf.server.DriverLauncher