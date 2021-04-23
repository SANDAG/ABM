@ECHO off

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

SET PYTHONPATH=C:\Program Files\INRO\Emme\Emme 4\Emme-4.3.7\Python27

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%

call %PROJECT_DIRECTORY%\bin\CTRampEnv.bat

:: First save the environment variable so it's value can be restored at the end.
SET OLDJAVAPATH=%JAVA_PATH%
SET OLDPATH=%PATH%

SET JAVA_PATH=%JAVA_64_PATH%
set CONFIG=%PROJECT_DIRECTORY%/conf
set LIB_PATH=%PROJECT_DIRECTORY%/application
SET JAR_PATH=%PROJECT_DIRECTORY%/application/sandag_abm.jar

SET PROPERTIES_NAME=sandag_abm

SET CLASSPATH=%CONFIG%;%JAVA_PATH%;%JAR_PATH%

:: Change the PATH environment variable so that JAVA_HOME is listed first in the PATH.
:: Doing this ensures that the JAVA_HOME path we defined above is the one that gets used in case other java paths are in PATH.
SET PATH=%PYTHONPATH%;%JAVA_PATH%\bin;%OLDPATH%

:: **************************************************************************************************************************************************
SET OUTPUT_FOLDER=%PROJECT_DIRECTORY%\output

IF EXIST %OUTPUT_FOLDER%\dtaTripsOut.csv DEL %OUTPUT_FOLDER%\dtaTripsOut.csv

:: Internal-External Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\internalExternalTrips.csv -marketSegment InternalExternalTrips -sampleRate %SAMPLERATE%

:: Commercial Vehicle Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\report\CommercialVehicleTrips.csv -marketSegment CommercialVehicleTrips -sampleRate %SAMPLERATE% -disaggregateSpace true

:: Heavy Truck Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\report\trucktrip.csv -marketSegment heavyTruckTrips -sampleRate %SAMPLERATE% -disaggregateSpace true -disaggregateTOD true

:: External-internal Vehicle Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\report\eitrip.csv -marketSegment ExternalInternalTrips -sampleRate %SAMPLERATE% -disaggregateSpace true -disaggregateTOD true

:: External-external Vehicle Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\report\eetrip.csv -marketSegment ExternalExternalTrips -sampleRate %SAMPLERATE% -disaggregateSpace true -disaggregateTOD true

:: Individual Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\indivTripData_3.csv -marketSegment IndividualTrips -sampleRate %SAMPLERATE%

:: Joint Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\jointTripData_3.csv -marketSegment JointTrips -sampleRate %SAMPLERATE%

:: Cross-Border Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\crossBorderTrips.csv -marketSegment CrossBorderTrips -sampleRate %SAMPLERATE%

:: Visitor Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\visitorTrips.csv -marketSegment VisitorTrips -sampleRate %SAMPLERATE%

:: Airport Trips
SET SAMPLERATE=1.0
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\airport_out.SAN.csv -marketSegment AirportTrips -sampleRate %SAMPLERATE%
java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile %PROJECT_DIRECTORY%\output\airport_out.CBX.csv -marketSegment AirportTrips -sampleRate %SAMPLERATE%


:: Ext-Ext, Ext-Int, Commercial and Truck Trips
::SET SAMPLERATE=1.0
::java.exe -server -Xms%MEMORY_SPMARKET_MIN% -Xmx%MEMORY_SPMARKET_MAX% -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% -Djava.library.path=%LIB_PATH% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType broad -inputFile broadFiles_full.csv

:: Output Summary
python .\python\summarize_dta_trips.py .\output\dtaTripsOut.csv .\output\dtaSummaryOut.csv

:: restore saved environment variable values, and change back to original current directory
SET JAVA_PATH=%OLDJAVAPATH%
SET PATH=%OLDPATH%