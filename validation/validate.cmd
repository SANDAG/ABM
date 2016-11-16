@ECHO OFF
set SCENNUM=%1
set OUTPUTDIR=%2

call ..\common\bin\CTRampEnv.bat

set CONFIG=..\conf
set JAR_LOCATION=..\common\application
SET currPath=%cd%
cd %currPath%
SET currPath=%currPath:\=/%

set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=%CONFIG%;%JAR_LOCATION%\*;%currPath%

%JAVA_64_PATH%\bin\java -showversion -server -Xms%MEMORY_VALIDATE_MIN% -Xmx%MEMORY_VALIDATE_MAX% -cp "%CLASSPATH%" org.sandag.abm.validation.MainApplication sandag_validate %SCENNUM% %OUTPUTDIR%

set CLASSPATH=%OLDCLASSPATH%

ECHO.
