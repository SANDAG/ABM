ECHO OFF

set SCENARIO_GUID=%1
set SCENARIO_NAME=%2

sqlcmd -d data-pipeline-engine -S sql-das-ads-dev-west.database.windows.net -U %db_pipeline_user% -P %db_pipeline_pw% -I -Q "EXEC [abm3].[GetScenarioId] '$(guid)', '$(name)'" -v guid=%SCENARIO_GUID% name=%SCENARIO_NAME%