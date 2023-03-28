rem @echo off

if "%1"=="" goto usage
if "%2"=="" goto usage
set SCENARIO_FOLDER=%1
set EMME_VERSION=%2

rem add EMME to PATH
set E_PATH=C:\\Program Files\\INRO\\Emme\\Emme 4\\Emme-%EMME_VERSION%
set PATH=%E_PATH%\\programs;%E_PATH%\\Python37;%PATH%
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

rem compile the toolbox?
python "%SCENARIO_FOLDER%\python\emme\toolbox\build_toolbox.py" --link -p "%SCENARIO_FOLDER%\emme_project\Scripts\sandag_toolbox.mtbx" -s %SCENARIO_FOLDER%\python\emme\toolbox 

rem create a batch script at startup
(
echo set python_virtualenv=C:\python_virtualenv\abm14_4_0
echo start "TITLE" "%E_PATH%\\programs\\EmmeDesktop.exe" ./emme_project.emp
)>%SCENARIO_FOLDER%\emme_project\start_emme_with_virtualenv.bat

rem mkdir %SCENARIO_FOLDER%\emme_project\Scripts\yaml
rem copy .\\common\\python\\emme\\yaml\\*.* %SCENARIO_FOLDER%\emme_project\Scripts\yaml

:usage
@echo Usage: %0 ^<scenario_folder^> ^<emme_version^>