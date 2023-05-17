set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

SET ANACONDA3_DIR=C:\Anaconda3

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat

SET CONDA3=%ANACONDA3_DIR%\Scripts\conda.exe

SET PYTHON3=C:\Users\%USERNAME%\.conda\envs\asim_baydag\python.exe
:: FIX PATH AND ENV HERE LATER
SET PYTHON2=%ANACONDA2_DIR%\python.exe

ECHO Activate ActivitySim....
CD /d %ANACONDA3_DIR%\Scripts
CALL %CONDA3_ACT% asim_baydag

cd /d %PROJECT_DIRECTORY%

cd src/asim/visualizer/visualizer

CALL RunViz.bat 
