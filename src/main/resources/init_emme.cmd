rem @echo off

if "%1"=="" goto usage
set SCENARIO_FOLDER=%1

rem add EMME to PATH
set PATH=%EMMEPATH%\\programs;%EMMEPATH%\\python27;%PATH%

rem create EMME project folder
python .\\common\\python\\emme\\init_emme_project.py -r %SCENARIO_FOLDER% -t testbank 
rem python ./src/main/emme/init_emme_project.py -r ${project.build.directory}/${project.name}/${project.version} -t testbank 

rem create toolbox
python .\\common\\python\\emme\\toolbox\\build_toolbox.py -s %SCENARIO_FOLDER%\emme_project\Scripts -p %SCENARIO_FOLDER%\emme_project\Scripts\sandag_toolbox.mtbx
rem python ./src/main/emme/toolbox/build_toolbox.py -s ${project.build.directory}/${project.name}/${project.version}\emme_project\Scripts -p ${project.build.directory}/${project.name}/${project.version}\emme_project\Scripts\sandag_toolbox.mtbx

:usage
@echo Usage: %0 ^<scenario_folder^>