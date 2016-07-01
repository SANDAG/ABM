@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage

set FILE_LIST=hwycov.e00 turns.csv trcov.e00 trlink.bin trrt.bin trstop.bin trrt.csv tap.elev tap.ptype timexfer.bin timexfer.dcb fare.mtx tapcov.dbf tapcov.shp tapcov.shx tapcov.shp.xml SANDAG_Bike_Net.sbn SANDAG_Bike_Net.sbx SANDAG_Bike_Net.dbf SANDAG_Bike_Net.shp SANDAG_Bike_Net.shx SANDAG_Bike_Net.prj SANDAG_Bike_Node.sbn SANDAG_Bike_Node.sbx SANDAG_Bike_Node.dbf SANDAG_Bike_Node.shp SANDAG_Bike_Node.shx SANDAG_Bike_Node.prj

@echo %FILE_LIST%

for %%i in (%FILE_LIST%) do (
@echo Deleting %2\%%i
del %2\%%i)

for %%i in (%FILE_LIST%) do (
@echo Copying %1\%%i
copy /Y %1\%%i %2)

set FILE_LIST=trlink.BX trrt.BX trstop.BX
for %%i in (%FILE_LIST%) do (
del %2\%%i
)
goto :eof

:usage
@echo Usage: %0 ^<from_path^> ^<to_path^>