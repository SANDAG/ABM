set PROJECT_DRIVE_BASE=%1
set PROJECT_DIRECTORY_BASE=%2
set PROJECT_DRIVE_BUILD=%3
set PROJECT_DIRECTORY_BUILD=%4

# **********************************************************************************************************************************
# STEP 1:
#   Create base trips with base skims
# **********************************************************************************************************************************

%PROJECT_DRIVE_BASE%
cd %PROJECT_DIRECTORY_BASE%
call bin\CTRampEnv.bat

set PATH=%TRANSCAD_PATH%;C:\Windows\System32;application

%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -Djava.library.path=%TRANSCAD_PATH%;application -cp %TRANSCAD_PATH%/GISDK/Matrices/*;application/*;conf/ org.sandag.abm.reporting.DataExporter

#
# copy reports directory
#
echo d|xcopy report report_basetripbaseskim /S /Y

# **********************************************************************************************************************************
# STEP 2:
#   Create build trips with build skims
# **********************************************************************************************************************************

%PROJECT_DRIVE_BUILD%
cd %PROJECT_DIRECTORY_BUILD%

%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -Djava.library.path=%TRANSCAD_PATH%;application -cp %TRANSCAD_PATH%/GISDK/Matrices/*;application/*;conf/ org.sandag.abm.reporting.DataExporter

#
# copy reports directory
#
echo d|xcopy report report_buildtripbuildskim /S /Y

# **********************************************************************************************************************************
# STEP 3:
#   Create base trips with build skims
# **********************************************************************************************************************************

# currently in build directory: rename the disaggregate data
#
rename output\airport_out.csv               airport_out.csv.build              
rename output\crossBorderTours.csv          crossBorderTours.csv.build         
rename output\crossBorderTrips.csv          crossBorderTrips.csv.build         
rename output\householdData_3.csv           householdData_3.csv.build          
rename output\indivTourData_3.csv           indivTourData_3.csv.build          
rename output\indivTripData_3.csv           indivTripData_3.csv.build          
rename output\internalExternalTrips.csv     internalExternalTrips.csv.build    
rename output\jointTourData_3.csv           jointTourData_3.csv.build          
rename output\jointTripData_3.csv           jointTripData_3.csv.build          
rename output\luLogsums_logit.csv           luLogsums_logit.csv.build          
rename output\luLogsums_simple.csv          luLogsums_simple.csv.build         
rename output\personData_3.csv              personData_3.csv.build             
rename output\visitorTours.csv              visitorTours.csv.build             
rename output\visitorTrips.csv              visitorTrips.csv.build             
#
# copy base data to build directory
#
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\airport_out.csv               output\airport_out.csv              
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\crossBorderTours.csv          output\crossBorderTours.csv         
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\crossBorderTrips.csv          output\crossBorderTrips.csv         
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\householdData_3.csv           output\householdData_3.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\indivTourData_3.csv           output\indivTourData_3.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\indivTripData_3.csv           output\indivTripData_3.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\internalExternalTrips.csv     output\internalExternalTrips.csv    
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\jointTourData_3.csv           output\jointTourData_3.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\jointTripData_3.csv           output\jointTripData_3.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\luLogsums_logit.csv           output\luLogsums_logit.csv          
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\luLogsums_simple.csv          output\luLogsums_simple.csv         
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\personData_3.csv              output\personData_3.csv             
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\visitorTours.csv              output\visitorTours.csv             
copy %PROJECT_DRIVE_BASE%\%PROJECT_DIRECTORY_BASE%\output\visitorTrips.csv              output\visitorTrips.csv    
#
# run
#

cd %PROJECT_DIRECTORY_BUILD%

%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -Djava.library.path=%TRANSCAD_PATH%;application -cp %TRANSCAD_PATH%/GISDK/Matrices/*;application/*;conf/ org.sandag.abm.reporting.DataExporter
#
# rename report directory
#
echo d|xcopy report report_basetripbuildskim /S /Y
#
# **********************************************************************************************************************************
# STEP 4:
#   Create build trips with base skims
# **********************************************************************************************************************************

%PROJECT_DRIVE_BASE%
cd %PROJECT_DIRECTORY_BASE%

# currently in base directory: rename the disaggregate data
#
rename output\airport_out.csv               airport_out.csv.base              
rename output\crossBorderTours.csv          crossBorderTours.csv.base         
rename output\crossBorderTrips.csv          crossBorderTrips.csv.base         
rename output\householdData_3.csv           householdData_3.csv.base          
rename output\indivTourData_3.csv           indivTourData_3.csv.base          
rename output\indivTripData_3.csv           indivTripData_3.csv.base          
rename output\internalExternalTrips.csv     internalExternalTrips.csv.base    
rename output\jointTourData_3.csv           jointTourData_3.csv.base          
rename output\jointTripData_3.csv           jointTripData_3.csv.base          
rename output\luLogsums_logit.csv           luLogsums_logit.csv.base          
rename output\luLogsums_simple.csv          luLogsums_simple.csv.base         
rename output\personData_3.csv              personData_3.csv.base             
rename output\visitorTours.csv              visitorTours.csv.base             
rename output\visitorTrips.csv              visitorTrips.csv.base             

#
# copy build data to base directory
#

copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\airport_out.csv.build               output\airport_out.csv              
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\crossBorderTours.csv.build          output\crossBorderTours.csv         
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\crossBorderTrips.csv.build          output\crossBorderTrips.csv         
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\householdData_3.csv.build           output\householdData_3.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\indivTourData_3.csv.build           output\indivTourData_3.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\indivTripData_3.csv.build           output\indivTripData_3.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\internalExternalTrips.csv.build     output\internalExternalTrips.csv    
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\jointTourData_3.csv.build           output\jointTourData_3.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\jointTripData_3.csv.build           output\jointTripData_3.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\luLogsums_logit.csv.build           output\luLogsums_logit.csv          
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\luLogsums_simple.csv.build          output\luLogsums_simple.csv         
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\personData_3.csv.build              output\personData_3.csv             
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\visitorTours.csv.build              output\visitorTours.csv             
copy %PROJECT_DRIVE_BUILD%\%PROJECT_DIRECTORY_BUILD%\output\visitorTrips.csv.build              output\visitorTrips.csv    

#
# run
#
 
%JAVA_64_PATH%\bin\java -Xms%MEMORY_DATAEXPORT_MIN% -Xmx%MEMORY_DATAEXPORT_MAX% -Djava.library.path=%TRANSCAD_PATH%;application -cp %TRANSCAD_PATH%/GISDK/Matrices/*;application/*;conf/ org.sandag.abm.reporting.DataExporter

#
# copy report directory
#
echo d|xcopy report report_buildtripbaseskim /S /Y
