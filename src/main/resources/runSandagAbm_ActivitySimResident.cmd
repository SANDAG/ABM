ECHO ON

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
:: Run ActivitySim
:: ---------------------------------------------------------------------
SET ANACONDA3_DIR=C:\Anaconda3
SET ANACONDA2_DIR=C:\Anaconda2

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

SET PATH=%ANACONDA2_DIR%\Library\bin
SET PATH=%ANACONDA2_DIR%\Scripts;%ANACONDA2_DIR%\bin

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
SET CONDA2_ACT=%ANACONDA2_DIR%\Scripts\activate.bat

SET CONDA3_DEA=%ANACONDA3_DIR%\Scripts\deactivate.bat
SET CONDA2_DEA=%ANACONDA2_DIR%\Scripts\deactivate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe
SET CONDA2=%ANACONDA2_DIR%\Scripts\conda.exe

SET PYTHON3=C:\Users\%USERNAME%\.conda\envs\asim_baydag\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_baydag

set MKL_NUM_THREADS=1
set MKL=1

cd /d %PROJECT_DIRECTORY%

:: Create Directory to Store ActivitySim Outputs
CD Output
ECHO Create Output Directory
MD resident
MD resident\trace
MD resident\log
CD ..

:: Run simulation.py
%PYTHON3% src/asim/simulation.py -s settings_mp.yaml -c src/asim/configs/resident -c src/asim/configs/common -d input -d output/skims -o output/resident

::::::::::::::::::::::
CD /d %ANACONDA2_DIR%\Scripts
ECHO %cd%
CALL %CONDA2_ACT% base

cd /d %PROJECT_DIRECTORY%
%PYTHON2% src/asim/scripts/convert_tripTables.py resident output


ECHO ActivitySim run complete!!
ECHO %startTime%%Time%