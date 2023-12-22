ECHO OFF

set PROJECT_DIRECTORY=%1
set SCENYEAR=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: SET UP PATHS
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
SET ANACONDA3_DIR=C:\Anaconda3

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: CALL ON THE ENVIRONMENT, AND IF IT DOES NOT EXIST, CREATE IT FROM THE YAML FILE
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
CALL %CONDA3_ACT% abm3_baydag

SET PYTHON3=C:\Users\%USERNAME%\.conda\envs\abm3_baydag\python.exe
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

cd /d %PROJECT_DIRECTORY%

%PYTHON3% src/asim/scripts/ScenarioManagement/scenManagement.py %PROJECT_DIRECTORY% %SCENYEAR%






