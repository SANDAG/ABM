:: Creating virtual Python environment
REM set PYTHON_ENV=C:\ProgramData\Anaconda3
REM call %PYTHON_ENV%\Scripts\activate.bat %PYTHON_ENV%
REM call conda env list | find /i "viz"
REM if not errorlevel 1 (
REM 	call conda env update --name viz --file ..\visualizer\config\environment.yml
REM 	call activate viz
REM ) else (
REM 	call conda env create -f ..\visualizer\config\environment.yml
REM )
REM call activate viz

call python ..\visualizer\scripts\setup.py
if ERRORLEVEL 1 goto error

call python ..\visualizer\data_pipeliner\run.py
if ERRORLEVEL 1 goto error

call python ..\visualizer\scripts\combine.py
if ERRORLEVEL 1 goto error

REM call conda deactivate viz

:success
ECHO "Visualizer created successfully!"
goto done

:error
ECHO "Visualizer not Created!"

:done