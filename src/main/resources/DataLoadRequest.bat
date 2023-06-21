set PROJECT_DIRECTORY="%1"
set ITERATION=%2
set YEAR=%3
set SAMPLE_RATE=%4
set ABM_VERSION=version_14_2_2

sqlcmd -d abm_14_2_0 -E -S DDAMWSQL16 -Q "EXEC [data_load].[SP_REQUEST] $(year),'$(path)',$(iteration),$(sample_rate),'$(abm_version)'" -v year=%YEAR% path=%PROJECT_DIRECTORY% iteration=%ITERATION% sample_rate=%SAMPLE_RATE% abm_version=%ABM_VERSION%