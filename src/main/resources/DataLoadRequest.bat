set PROJECT_DIRECTORY="%1"
set ITERATION=%2
set YEAR=${year}
set SAMPLE_RATE=%3
set ABM_VERSION=${version}

sqlcmd -d ${database_name} -E -S ${database_server} -Q "EXEC [data_load].[SP_REQUEST] $(year),'$(path)',$(iteration),$(sample_rate),'$(abm_version)'" -v year=%YEAR% path=%PROJECT_DIRECTORY% iteration=%ITERATION% sample_rate=%SAMPLE_RATE% abm_version=%ABM_VERSION%