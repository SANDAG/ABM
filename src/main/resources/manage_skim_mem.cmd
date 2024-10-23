ECHO ON

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

SET ANACONDA3_DIR=%CONDA_PREFIX%

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

SET PYTHON3=%ANACONDA3_DIR%\envs\asim_131\python.exe

ECHO Activate asim_131....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_131

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

%PYTHON3% python/skim_shared_mem_manager.py