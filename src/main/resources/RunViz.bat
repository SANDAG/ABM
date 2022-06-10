SET PROJECT_DRIVE=%1
SET PROJECT_DIRECTORY=%2
SET PROJECT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\

python T:\ABM\user\jflo\scripts\vizlog.py T:\ABM\user\jflo\scripts\vizlog.txt "Setting up virtual environment" True

:: Creating virtual Python environment
set PYTHON_ENV=C:\ProgramData\Anaconda3
call %PYTHON_ENV%\Scripts\activate.bat %PYTHON_ENV%
call conda env list | find /i "viz"
if not errorlevel 1 (
	call conda env update --name viz --file ..\visualizer\config\environment.yml
	call activate viz
) else (
	call conda env create -f ..\visualizer\config\environment.yml
)
call activate viz

python T:\ABM\user\jflo\scripts\vizlog.py T:\ABM\user\jflo\scripts\vizlog.txt "Setting up visualization directory" False

call python %PROJECT_DIR%visualizer\scripts\setup.py

python T:\ABM\user\jflo\scripts\vizlog.py T:\ABM\user\jflo\scripts\vizlog.txt "Running data pipeline" False

call python %PROJECT_DIR%visualizer\data_pipeliner\run.py

python T:\ABM\user\jflo\scripts\vizlog.py T:\ABM\user\jflo\scripts\vizlog.txt "Combinining survey and model data" False

call python %PROJECT_DIR%visualizer\scripts\combine.py

call conda deactivate viz