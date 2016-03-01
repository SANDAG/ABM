set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DRIVE%/%PROJECT_DIRECTORY%
call bin\CTRampEnv.bat
set PATH=%TRANSCAD_PATH%

%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -Dlog4j.configuration=log4j_d2t.xml -cp application/*;conf/;%TRANSCAD_PATH%/GISDK/Matrices/* -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.application.SandagMGRAtoPNR %PROPERTIES_NAME%