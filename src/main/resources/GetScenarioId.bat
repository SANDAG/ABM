ECHO OFF

set SCENARIO_GUID=%1
set SCENARIO_NAME=%2
set ENV=%3

if %ENV%=="dev" (
    sqlcmd -d data-pipeline-engine -S %ABM_DATABASE_DEV% -U %ABM_METADATA_ID% -I -Q "EXEC [abm3].[GetScenarioId] '$(guid)', '$(name)'" -v guid=%SCENARIO_GUID% name=%SCENARIO_NAME%
) else (
    sqlcmd -d data-pipeline-engine -S %ABM_DATABASE_PROD% -U %ABM_METADATA_ID% -I -Q "EXEC [abm3].[GetScenarioId] '$(guid)', '$(name)'" -v guid=%SCENARIO_GUID% name=%SCENARIO_NAME%
)