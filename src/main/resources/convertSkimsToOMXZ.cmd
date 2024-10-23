set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%/output/skims

wring omx

del traffic_skims*.omx
del transit_skims*.omx