set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set JAR_LOCATION=%PROJECT_DIRECTORY%/application

%JAVA_64_PATH%\bin\java -Xms%MEMORY_CLIENT_MIN% -Xmx%MEMORY_CLIENT_MAX% -Djxl.nowarnings=true -Dlog4j.configuration=log4j_d2t.xml -cp application/*;conf/ -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%JAR_LOCATION% org.sandag.abm.application.SandagMGRAtoPNR %PROPERTIES_NAME%