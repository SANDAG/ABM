ECHO OFF

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set ITERATION=%3
set SCENYEAR=%4

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

ECHO Activate ActivitySim....
CALL %activate_uv_asim%

CD output
MD airport.CBX
MD airport.SAN
MD crossborder
MD visitor
MD resident
MD assignment
CD ..

if %ITERATION% equ 1 (

    python src/asim/scripts/resident/resident_preprocessing.py input output %SCENYEAR% %PROJECT_DIRECTORY% || goto error

    ECHO Running Airport models pre-processing
    python src/asim/scripts/airport/airport_model.py -p -c src/asim/configs/airport.CBX -d input -o output/airport.CBX || goto error
    python src/asim/scripts/airport/airport_model.py -p -c src/asim/configs/airport.SAN -d input -o output/airport.SAN || goto error
    python src/asim/scripts/airport/createPOIomx.py %PROJECT_DIRECTORY% %SCENYEAR% || goto error

    ECHO Running xborder model pre-processing
    python src/asim/scripts/xborder/cross_border_model.py -p -c src/asim/configs/crossborder -c src/asim/configs/common -d input -o output/crossborder || goto error
    python src/asim/scripts/xborder/createPMSAomx.py %PROJECT_DIRECTORY% || goto error

    ECHO Running visitor model pre-processing
    python src/asim/scripts/visitor/visitor_model.py -t -c src/asim/configs/visitor -d input -o output/visitor || goto error
) else (
    ECHO Running resident model pre-processing
    python src/asim/scripts/resident/resident_preprocessing.py input output %SCENYEAR% %PROJECT_DIRECTORY% || goto error
)

goto :EOF

:error
exit /b 2