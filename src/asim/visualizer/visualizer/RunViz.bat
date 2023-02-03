:: Creating virtual Python environment
call conda env create --file environment.yml

call conda activate abm3_viz

:: Run the data pipeline tool to summarize model results
call python T:\data\tools\Data-Pipeline-Tool\run.py
if ERRORLEVEL 1 goto error

:: Combine data pipeline outputs with survey results for comparison
call python scripts\combine.py
if ERRORLEVEL 1 goto error

call conda deactivate

:success
ECHO "Visualizer created successfully!"
goto done

:error
ECHO "Visualizer not Created!"

:done