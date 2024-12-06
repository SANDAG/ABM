rem @echo off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SAMPLERATE=%3
set ITERATION=%4

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

rem ### First save the JAVA_PATH environment variable so it s value can be restored at the end.
set OLDJAVAPATH=%JAVA_PATH%

rem ### Set the directory of the jdk version desired for this model run
rem ### Note that a jdk is required; a jre is not sufficient, as the UEC class generates
rem ### and compiles code during the model run, and uses javac in the jdk to do this.
set JAVA_PATH=%JAVA_64_PATH%

rem ### Name the project directory.  This directory will hava data and runtime subdirectories
set RUNTIME=%PROJECT_DIRECTORY%
set CONFIG=%RUNTIME%/conf


set JAR_LOCATION=%PROJECT_DIRECTORY%/application
set LIB_JAR_PATH=%JAR_LOCATION%\*

rem ### Define the CLASSPATH environment variable for the classpath needed in this model run.
set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=%CONFIG%;%RUNTIME%;%LIB_JAR_PATH%;

rem ### Save the name of the PATH environment variable, so it can be restored at the end of the model run.
set OLDPATH=%PATH%

rem ### Change the PATH environment variable so that JAVA_HOME is listed first in the PATH.
rem ### Doing this ensures that the JAVA_HOME path we defined above is the on that gets used in case other java paths are in PATH.
set PATH=%JAVA_PATH%\bin;%OLDPATH%

rem run ping to add a pause so that hhMgr and mtxMgr have time to fully start
ping -n 10 %MAIN% > nul

cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%

rem TNC Fleet Model
%JAVA_64_PATH%\bin\java -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp "%CLASSPATH%" -Djxl.nowarnings=true -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.maas.TNCFleetModel %PROPERTIES_NAME% -iteration %ITERATION% -sampleRate %SAMPLERATE% || goto error

rem Household AV Allocation Model
%JAVA_64_PATH%\bin\java -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp "%CLASSPATH%" -Djxl.nowarnings=true -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.maas.HouseholdAVAllocationModelRunner %PROPERTIES_NAME% -iteration %ITERATION% -sampleRate %SAMPLERATE% || goto error
rem ### restore saved environment variable values, and change back to original current directory
set JAVA_PATH=%OLDJAVAPATH%
set PATH=%OLDPATH%
set CLASSPATH=%OLDCLASSPATH%

goto :EOF

:error
rem ### restore saved environment variable values, and change back to original current directory
set JAVA_PATH=%OLDJAVAPATH%
set PATH=%OLDPATH%
set CLASSPATH=%OLDCLASSPATH%
exit /b 2