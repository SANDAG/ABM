ECHO OFF

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set ITERATION=%3
set SCENYEAR=%4

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: SET UP PATHS
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
SET ANACONDA3_DIR=%CONDA_PREFIX%
SET ANACONDA2_DIR=%CONDA_TWO_PREFIX%

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

SET PATH=%ANACONDA2_DIR%\Library\bin
SET PATH=%ANACONDA2_DIR%\Scripts;%ANACONDA2_DIR%\bin

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: CALL ON THE ENVIRONMENT, AND IF IT DOES NOT EXIST, CREATE IT FROM THE YAML FILE
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
CALL %CONDA3_ACT% asim_132

if %errorlevel% equ 0 (
    ECHO Python environment %ENV_NAME% is already installed.
    goto end
)
CD src\asim
rem Install the environment from the YAML file
CALL %CONDA3% env create -f environment.yml -n asim_132

CALL %CONDA3_ACT% asim_132

:end

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_132\python.exe
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

cd /d %PROJECT_DIRECTORY%

CD output
MD airport.CBX
MD airport.SAN
MD crossborder
MD visitor
MD resident
MD parking
MD parking/plots
MD assignment
CD ..

if %ITERATION% equ 1 (
    ECHO Running resident model pre-processing   
    %PYTHON3% src/asim/scripts/resident/2zoneSkim.py %PROJECT_DIRECTORY% || goto error

    %PYTHON3% src/asim/scripts/resident/resident_preprocessing.py input output %SCENYEAR% %PROJECT_DIRECTORY% || goto error

    ECHO Running Airport models pre-processing
    %PYTHON3% src/asim/scripts/airport/airport_model.py -p -c src/asim/configs/airport.CBX -d input -o output/airport.CBX || goto error
    %PYTHON3% src/asim/scripts/airport/airport_model.py -p -c src/asim/configs/airport.SAN -d input -o output/airport.SAN || goto error
    %PYTHON3% src/asim/scripts/airport/createPOIomx.py %PROJECT_DIRECTORY% %SCENYEAR% || goto error

    ECHO Running xborder model pre-processing
    %PYTHON3% src/asim/scripts/xborder/cross_border_model.py -p -c src/asim/configs/crossborder -c src/asim/configs/common -d input -o output/crossborder || goto error
    %PYTHON3% src/asim/scripts/xborder/createPMSAomx.py %PROJECT_DIRECTORY% || goto error

    ECHO Running visitor model pre-processing
    %PYTHON3% src/asim/scripts/visitor/visitor_model.py -t -c src/asim/configs/visitor -d input -o output/visitor || goto error
) else (
    ECHO Running resident model pre-processing
    %PYTHON3% src/asim/scripts/resident/resident_preprocessing.py input output %SCENYEAR% %PROJECT_DIRECTORY% || goto error
)

goto :EOF

:error
exit /b 2