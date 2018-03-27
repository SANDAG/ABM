set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set PROJECT_DIRECTORY_FWD=%3
set CVM_ScaleFactor=%4
set MGRA_DATA=%5
set TAZ_CENTROIDS=%6

set "SCEN_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%"
set "SCEN_DIR_FWD=%PROJECT_DRIVE%%PROJECT_DIRECTORY_FWD%"
%PROJECT_DRIVE%
cd %SCEN_DIR%

call %SCEN_DIR%\bin\CTRampEnv.bat

set CLASSPATH=%SCEN_DIR%/application/*

REM create the land-use data
python %SCEN_DIR%\python\cvm_input_create.py %SCEN_DIR% %MGRA_DATA% %TAZ_CENTROIDS% "Zonal Properties CVM.csv"

REM create the commercial vehicle tours
python %SCEN_DIR%\python\sdcvm.py -s %CVM_ScaleFactor% -p %SCEN_DIR%

REM run the java code
%JAVA_64_PATH%\bin\java.exe -Xmx24000m -Xmn16000M -Dlog4j.configuration=file:./conf/log4j.xml -Djava.library.path=%SCEN_DIR%/application -DSCENDIR=%SCEN_DIR_FWD% -cp %CLASSPATH% org.sandag.cvm.activityTravel.cvm.GenerateCommercialTours "conf/cvm.properties"

REM summarize model outputs
python %SCEN_DIR%\python\sdcvm_summarize.py -p %SCEN_DIR%
