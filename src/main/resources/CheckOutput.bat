rem ### Declaring required environment variables
set PROJECT_DIRECTORY=%1
set CHECK=%2
set ITERATION=%3

rem ### Checking that files were generated
python %PROJECT_DIRECTORY%\python\check_output.py %PROJECT_DIRECTORY% %CHECK% %ITERATION%
if ERRORLEVEL 1 exit 2
