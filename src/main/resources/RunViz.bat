SET PROJECT_DRIVE=%1
SET PROJECT_DIRECTORY=%2
SET PROJECT_DIR=%PROJECT_DRIVE%%PROJECT_DIRECTORY%\

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

call python %PROJECT_DIR%visualizer\scripts\setup.py

call python %PROJECT_DIR%visualizer\data_pipeliner\run.py

call python %PROJECT_DIR%visualizer\scripts\combine.py

call conda deactivate viz