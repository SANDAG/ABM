rem @echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
set SCENARIO_FOLDER=%1
set EMME_VERSION=%2

rem add EMME to PATH
set E_PATH=C:\\Program Files\\Bentley\\OpenPaths\\EMME %EMME_VERSION%
set PATH=%E_PATH%\\programs;%E_PATH%\\python311;%PATH%
set EMMEPATH=%E_PATH%

rem delete existing emme_project folder
:removedir
if exist %SCENARIO_FOLDER%\emme_project (
    rd /s /q %SCENARIO_FOLDER%\\emme_project
    goto removedir
)

rem create EMME project folder
python .\\common\\python\\emme\\init_emme_project.py -r %SCENARIO_FOLDER% -t emmebank -v %EMME_VERSION%

rem create toolbox
python .\\common\\python\\emme\\toolbox\\build_toolbox.py -s .\\common\\python\\emme\\toolbox -p %SCENARIO_FOLDER%\emme_project\Scripts\sandag_toolbox.mtbx
copy ".\\common\\python\\emme\\solutions.mtbx" "%SCENARIO_FOLDER%\emme_project\Scripts\solutions.mtbx"

rem mkdir %SCENARIO_FOLDER%\emme_project\Scripts\yaml
rem copy .\\common\\python\\emme\\yaml\\*.* %SCENARIO_FOLDER%\emme_project\Scripts\yaml

:usage
@echo Usage: %0 ^<scenario_folder^> ^<emme_version^>