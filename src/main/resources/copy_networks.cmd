@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage

set FILE_LIST=hwycov.e00 trcov.e00 turns.csv trlink.csv trrt.csv trstop.csv tap.elev tap.ptype timexfer_EA.csv timexfer_AM.csv timexfer_MD.csv timexfer_PM.csv timexfer_EV.csv special_fares.txt linktypeturns.dbf tapcov.dbf tapcov.shp tapcov.shx tapcov.shp.xml mobilityhubtaps.csv mobilityHubMGRAs.csv SANDAG_Bike_Net.sbn SANDAG_Bike_Net.sbx SANDAG_Bike_Net.dbf SANDAG_Bike_Net.shp SANDAG_Bike_Net.shx SANDAG_Bike_Net.prj SANDAG_Bike_Node.sbn SANDAG_Bike_Node.sbx SANDAG_Bike_Node.dbf SANDAG_Bike_Node.shp SANDAG_Bike_Node.shx SANDAG_Bike_Node.prj rtcov.shp rtcov.shx rtcov.dbf rtcov.shp.xml

@echo %FILE_LIST%

for %%i in (%FILE_LIST%) do (
@echo Copying and overwriting %1\%%i
xcopy /Y %1\%%i %2)

goto :eof

:usage
@echo Usage: %0 ^<from_path^> ^<to_path^>