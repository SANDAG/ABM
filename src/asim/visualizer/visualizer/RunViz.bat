:: Creating virtual Python environment
:: if you need to create an environment, uncomment below conda calls:
:: call conda env create --file environment.yml

:: call conda activate abm3_viz

:: Run the data pipeline tool to summarize model results
:: If path not found, try un-commenting below
:: call python T:\data\tools\Data-Pipeline-Tool\run.py
call python \\sandag.org\transdata\data\tools\Data-Pipeline-Tool\run.py
if ERRORLEVEL 1 goto error

:: Combine data pipeline outputs with survey results for comparison
call python scripts\combine.py
if ERRORLEVEL 1 goto error

:: call conda deactivate

:success
ECHO "Visualizer created successfully!"
goto done

:error
ECHO "Visualizer not Created!"

:done