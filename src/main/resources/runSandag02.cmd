@echo off

rem ############  PARAMETERS  ############
set DRIVE=%1
set PROJECT_DIRECTORY=%2

rem ############  JPPF DRIVER  ############
set JPPF_LIB=%PROJECT_DIRECTORY%\application\*
set CLASSPATH=%PROJECT_DIRECTORY%\conf;%JPPF_LIB%

call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

%DRIVE%
cd %PROJECT_DRIVE%/%PROJECT_DIRECTORY%
start %JAVA_64_PATH%\bin\java -server -Xms16m -Xmx16m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j-sandag02.properties -Djppf.config=jppf-sandag02.properties org.jppf.node.NodeLauncher
