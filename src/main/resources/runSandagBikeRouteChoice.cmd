rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SAMPLERATE=%3
set ITERATION=%4
set PROPERTIES_NAME=sandag_abm

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%/%PROJECT_DIRECTORY%
call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

rem ### Name the project directory.  This directory will hava data and runtime subdirectories
set RUNTIME=%PROJECT_DIRECTORY%
set CONFIG=%RUNTIME%/conf

rem ### Set the name of the properties file the application uses by giving just the base part of the name (with ".xxx" extension)
set JAR_LOCATION=%PROJECT_DIRECTORY%/application
set LIB_JAR_PATH=%JAR_LOCATION%\*

rem ### Define the CLASSPATH environment variable for the classpath needed in this model run.
set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=%CONFIG%;%RUNTIME%;%LIB_JAR_PATH%;

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%/%PROJECT_DIRECTORY%

rem run bike assignment
%JAVA_64_PATH%\bin\java -showversion -server -Xmx%MEMORY_BIKEROUTE_MAX% -XX:-UseGCOverheadLimit -cp "%CLASSPATH%" -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.active.sandag.SandagBikePathChoiceEdgeAssignmentApplication %PROPERTIES_NAME% %SAMPLERATE% %ITERATION%
if ERRORLEVEL 1 goto DONE

:done
rem kill java tasks
rem taskkill /F /IM java.exe

rem ### restore saved environment variable values, and change back to original current directory
set JAVA_PATH=%OLDJAVAPATH%
set PATH=%OLDPATH%
