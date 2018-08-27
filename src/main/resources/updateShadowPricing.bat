set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set PROJECT_DIRECTORY_FWD=%3
set SAMPLE_RATE=%4
set ITERATIONS=%5

%PROJECT_DRIVE%
set "SCEN_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%"
set "SCEN_DIR_FWD=%PROJECT_DRIVE%%PROJECT_DIRECTORY_FWD%"

python.exe %SCEN_DIR%\python\sandag\shadowpricing\scaleShadowPricing.py  %SCEN_DIR_FWD%\output %ITERATIONS% work %SAMPLE_RATE%
python.exe %SCEN_DIR%\python\sandag\shadowpricing\scaleShadowPricing.py  %SCEN_DIR_FWD%\output %ITERATIONS% school %SAMPLE_RATE%

