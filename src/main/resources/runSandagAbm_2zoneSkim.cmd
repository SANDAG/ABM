rem @echo on

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

:: ---------------------------------------------------------------------
SET ANACONDA_DIR=%CONDA_PREFIX%


SET PATH=%ANACONDA_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA_DIR%\Scripts;%ANACONDA_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA_ACT=%ANACONDA_DIR%\Scripts\activate.bat
ECHO CONDA_ACT: %CONDA_ACT%

SET CONDA_DEA=%ANACONDA_DIR%\Scripts\deactivate.bat
ECHO CONDA_DEA: %CONDA_DEA%

SET PYTHON=C:\Users\%USERNAME%\.conda\envs\asim_140\python.exe
ECHO PYTHON: %PYTHON%

ECHO Activate ActivitySim Environment....
CD /d %ANACONDA_DIR%\Scripts
CALL %CONDA_ACT% asim_140


cd /d %PROJECT_DIRECTORY%

:: Run skimming
%PYTHON% src/main/python/2zoneSkim.py %PROJECT_DIRECTORY% || exit /b 2

ECHO %startTime%%Time%

