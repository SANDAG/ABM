REM This batch file deletes 
REM 1) files in report folder 
REM 2) intermediate files in /iter1 and /iter3 folders 
REM 3) temporary and intermediate files in output folder

REM Step 1: deleting files in report folder
del /F /Q /S  .\report\*.*

REM Step 2: deleting files in /iter1 and /iter2
t: 
cd %1
cd output
rmdir /s /q iter1
rmdir /s /q iter2

REM Step 3: deleting temporary and intermediate files in output folder
del /S dailyDistr*all.mtx
del /S distribution*
del /S *2050*
del /S temp*
del /S aoResults_pre.csv
del /S tazCommVeh*
del /S commVehTrips*
del /S usSd*PA*
del /S usSd*Daily*
del /S impprem_EA.mtx
del /S impprem_AM.mtx
del /S impprem_MD.mtx
del /S impprem_PM.mtx
del /S impprem_EV.mtx
del /S implocl_EA.mtx
del /S implocl_AM.mtx
del /S implocl_MD.mtx
del /S implocl_PM.mtx
del /S implocl_EV.mtx
del /S imptrk*mtx