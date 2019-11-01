rem @echo off

if "%1"=="" goto usage
set SCENARIO_FOLDER=%1
set EMME_VERSION=4.3.7

rem add EMME to PATH
set E_PATH=C:\\Program Files\\INRO\\Emme\\Emme 4\\Emme-%EMME_VERSION%
set PATH=%E_PATH%\\programs;%E_PATH%\\python27;%PATH%
set EMMEPATH=%E_PATH%

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

rem create a batch script at startup
(
echo set python_virtualenv=C:\python_virtualenv\abm14_1_0
echo "%E_PATH%\\programs\\EmmeDesktop.exe" ./emme_project.emp
)>%SCENARIO_FOLDER%\emme_project\start_emme_with_virtualenv.bat

rem mkdir %SCENARIO_FOLDER%\emme_project\Scripts\yaml
rem copy .\\common\\python\\emme\\yaml\\*.* %SCENARIO_FOLDER%\emme_project\Scripts\yaml

:usage
@echo Usage: %0 ^<scenario_folder^>