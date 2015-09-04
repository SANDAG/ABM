set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%
call bin\CTRampEnv.bat

%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -cp application/*;conf/;%TRANSCAD_PATH%/GISDK/Matrices/* -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.application.SandagMGRAtoPNR %PROPERTIES_NAME%