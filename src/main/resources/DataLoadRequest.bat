set PROJECT_DIRECTORY="%1"
set ITERATION=%2
set YEAR=${year}

sqlcmd -C -d ${database_name} -E -S ${database_server} -Q "EXEC [data_load].[SP_REQUEST] $(year),'$(path)',$(iteration)" -v year=%YEAR% path=%PROJECT_DIRECTORY% iteration=%ITERATION%