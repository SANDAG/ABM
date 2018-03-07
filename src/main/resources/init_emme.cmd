rem @echo off

if "%1"=="" goto usage
set SCENARIO_FOLDER=%1

rem add EMME to PATH
set PATH=%EMMEPATH%\\programs;%EMMEPATH%\\python27;%PATH%

rem delete existing emme_project folder
:removedir
if exist %SCENARIO_FOLDER%\emme_project (
    rd /s /q %SCENARIO_FOLDER%\\emme_project
    goto removedir
)

rem create EMME project folder
python .\\common\\python\\emme\\init_emme_project.py -r %SCENARIO_FOLDER% -t emmebank 

rem create toolbox
python .\\common\\python\\emme\\toolbox\\build_toolbox.py -s .\\common\\python\\emme\\toolbox -p %SCENARIO_FOLDER%\emme_project\Scripts\sandag_toolbox.mtbx

mkdir %SCENARIO_FOLDER%\emme_project\Scripts\yaml

copy .\\common\\python\\emme\\yaml\\*.* %SCENARIO_FOLDER%\emme_project\Scripts\yaml

:usage
@echo Usage: %0 ^<scenario_folder^>