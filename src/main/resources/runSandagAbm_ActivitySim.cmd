rem @echo on

set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set SAMPLERATE=%3
set ITERATION=%4

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%

SET SAMPLE_ITERATION1=300000
SET SAMPLE_ITERATION2=600000
SET SAMPLE_ITERATION3=0

:: -------------------------------------------------------------------------------------------------
:: Loop
:: -------------------------------------------------------------------------------------------------
ECHO ****MODEL ITERATION %ITERATION%
ECHO REINDEX %REINDEX%

IF %ITERATION% EQU 1 SET SAMPLE=%SAMPLE_ITERATION1%
IF %ITERATION% EQU 2 SET SAMPLE=%SAMPLE_ITERATION2%
IF %ITERATION% EQU 3 SET SAMPLE=%SAMPLE_ITERATION3%


ECHO CURRENT DIRECTORY: %cd%
CD src\asim\configs\resident
:: Set sample_rate in configs file dynamically
ECHO # Configs File with Sample Rate set by Model Runner > settings_mp.yaml
FOR /F "delims=*" %%i IN (settings_mp_source.yaml) DO (
    SET LINE=%%i
    SETLOCAL EnableDelayedExpansion
    SET LINE=!LINE:%%sample_size%%=%SAMPLE%!
    ECHO !LINE!>>settings_mp.yaml
    ENDLOCAL
)
:: -------------------------------------------------------------------------------------------------
:: Run SEMCOG ActivitySim
:: Anaconda installation directory is set in the Paremters tab in transcAD Add-In
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

ECHO Activate ActivitySim....
CD /d %ANACONDA_DIR%\Scripts
CALL %CONDA_ACT% asim_140

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs
CD Output
ECHO Create Output Directory
MD ActivitySim
MD ActivitySim\trace
MD ActivitySim\log
CD ..

:: Run simulation.py
%PYTHON% src/asim/simulation.py -s settings_mp.yaml -c src/asim/configs/resident -c src/asim/configs/common -d input -o output/ActivitySim || exit /b 2

CD %BATCH_DIR%

ECHO ActivitySim run complete!!
ECHO %startTime%%Time%

