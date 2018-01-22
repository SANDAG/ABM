set PROJECT_DRIVE=%1
set PROJECT_DIRECTORY=%2
set PROJECT_DIRECTORY_FWD=%3
set PROPERTY=%4
set OLDPVALUE=%5
set NEWPVALUE=%6

%PROJECT_DRIVE%
set "SCEN_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%"
set "SCEN_DIR_FWD=%PROJECT_DRIVE%%PROJECT_DIRECTORY_FWD%"

python.exe %SCEN_DIR%\python\sandag\shadowpricing\updateProperty.py --path %SCEN_DIR_FWD%  --pfile sandag_abm.properties --pname %PROPERTY% --oldpvalue %OLDPVALUE% --newpvalue %NEWPVALUE%