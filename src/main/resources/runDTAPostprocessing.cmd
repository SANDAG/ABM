SET PROJECT_DRIVE=C:
SET PROJECT_DIRECTORY=C:\Projects\SANDAG_DTA

%PROJECT_DRIVE%
cd %PROJECT_DIRECTORY%
call %PROJECT_DIRECTORY%\tod_disaggregation\CTRampEnv.bat


:: First save the environment variable so it's value can be restored at the end.
SET OLDJAVAPATH=%JAVA_PATH%
SET OLDPATH=%PATH%

:: Set the directory of the jre version desired for this model run
SET JAVA_PATH=%JAVA_64_PATH%

:: Set the directory of the transcad version
SET TRANSCAD_PATH=C:/Progra~1/TransC~1.0

:: Name the project directory.  This directory will hava data and runtime subdirectories
SET CONFIG=%PROJECT_DIRECTORY%

:: Set the name of the properties file the application uses by giving just the base part of the name (without ".xxx" extension)
SET PROPERTIES_NAME=sandag_Postprocessing

SET LIB_JAR_PATH=%PROJECT_DIRECTORY%/sandag_dta.jar

:: Define the CLASSPATH environment variable for the classpath needed in this model run.
SET CLASSPATH=%TRANSCAD_PATH%/GISDK/Matrices/TranscadMatrix.jar;%CONFIG%;%JAVA_PATH%;%LIB_JAR_PATH%;

:: Change the PATH environment variable so that JAVA_HOME is listed first in the PATH.
:: Doing this ensures that the JAVA_HOME path we defined above is the one that gets used in case other java paths are in PATH.
SET PYTHONPATH=C:\Python27\ArcGIS10.1
SET PATH=%PYTHONPATH%;%TRANSCAD_PATH%;%JAVA_PATH%\bin;%OLDPATH%


:: run ping to add a pause so that hhMgr and mtxMgr have time to fully start
::ping -n 10 %MAIN% > nul

:: **************************************************************************************************************************************************
SET MODELFILES=%PROJECT_DIRECTORY%\model_files

copy /y %MODELFILES%\indivTripData_3.csv .\output\indivTripData.csv
copy /y %MODELFILES%\jointTripData_3.csv .\output\jointTripData.csv
copy /y %MODELFILES%\internalExternalTrips.csv .\output
copy /y %MODELFILES%\airport_out.csv .\output
copy /y %MODELFILES%\crossBorderTrips.csv .\output
copy /y %MODELFILES%\visitorTrips.csv .\output
copy /y %MODELFILES%\dailyDistributionMatricesTruck* .\output
copy /y %MODELFILES%\usSd* .\output
copy /y %MODELFILES%\commVehTODTrips.mtx .\output
copy /y %MODELFILES%\externalExternalTrips.mtx .\output
copy /y %MODELFILES%\impdan_* .\output
copy /y %MODELFILES%\mgra13_based_input2012.csv .\output

SET RSCFILE_PATH=%PROJECT_DIRECTORY%\gisdk\
SET RSCFILE_NAME=createNewEETables
SET MACRO_NAME=CreateExternalExternalTables
SET MACRO_DESCIPTION=Macro to Process EE Trip Tables

REM COMPILE RSC TO UI FOR BATCHING
"C:\Program Files\TransCAD 6.0\RSCC.exe" -c -u "%RSCFILE_PATH%%RSCFILE_NAME%_UI.DBD" "%RSCFILE_PATH%%RSCFILE_NAME%.RSC"

REM CALL THE UI TO RUN
"C:\Program Files\TransCAD 6.0\Tcw.exe" -q -a "%RSCFILE_PATH%%RSCFILE_NAME%_UI" -ai "%MACRO_NAME%" -n %MACRO_DESCIPTION%

DEL %RSCFILE_PATH%%RSCFILE_NAME%_UI*.*

DEL .\output\dtaTripsOut.csv

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile indivTripData.csv -marketSegment IndividualTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_IndividualTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile jointTripData.csv -marketSegment JointTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_JointTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile airport_out.csv -marketSegment AirportTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_AirportTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile crossBorderTrips.csv -marketSegment CrossBorderTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_CrossBoarderTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile visitorTrips.csv -marketSegment VisitorTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_VisitorTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType detailed -inputFile internalExternalTrips.csv -marketSegment InternalExternalTrips -sampleRate %SAMPLERATE% 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_InternalExternalTrips.log

SET SAMPLERATE=1.0
java.exe -server -Xms30000m -Xmx30000m -cp %CLASSPATH% -Dlog4j.configuration=log4j.xml -Dproject.folder=%PROJECT_DIRECTORY% org.sandag.abm.dta.postprocessing.PostprocessModel %PROPERTIES_NAME% -todType broad -inputFile broadFiles.csv 2>&1 | %GNUWIN32_PATH%\tee.exe .\log\todModel_Broad.log

::python .\tod_Disaggregation\summarizeDTATrips.py .\output\dtaTripsOut.csv .\output\dtaSummaryOut.csv

:: delete rest of the model files from output dir
::MKDIR %PROJECT_DIRECTORY%\output\temp
::MOVE %PROJECT_DIRECTORY%\output\dtaTripsOut.csv %PROJECT_DIRECTORY%\output\temp\dtaTripsOut.csv
::DEL %PROJECT_DIRECTORY%\output\*.* /f /q
::MOVE %PROJECT_DIRECTORY%\output\temp\*.* %PROJECT_DIRECTORY%\output\
::RMDIR %PROJECT_DIRECTORY%\output\temp

:done
:: restore saved environment variable values, and change back to original current directory
SET JAVA_PATH=%OLDJAVAPATH%
SET PATH=%OLDPATH%