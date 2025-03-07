set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2

SET PATH=%ANACONDA3_DIR%\Library\bin;%PATH%
SET PATH=%ANACONDA3_DIR%\Scripts;%ANACONDA3_DIR%\bin;%PATH%

SET ANACONDA3_DIR=%CONDA_PREFIX%
SET CONDA3_ACT=%ANACONDA3_DIR%\Scripts\activate.bat
CALL %CONDA3_ACT% asim_134

%PROJECT_DRIVE%
cd /d %PROJECT_DIRECTORY%
cd output\skims
REM `wring omx` creates omxz files that have the same values as the omx files but are stored more efficiently
CALL wring omx
