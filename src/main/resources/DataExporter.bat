set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%
call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

%JAVA_64_PATH%\bin\java -Xms40000m -Xmx40000m -cp application/*;conf/;%TRANSCAD_PATH%/GISDK/Matrices/* org.sandag.abm.reporting.DataExporter