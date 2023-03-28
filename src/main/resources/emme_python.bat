rem //////////////////////////////////////////////////////////////////////////////
rem ////
rem //// emme_python.bat 
rem ////
rem //// Configure environment and start Python script to run Emme-related task.
rem //// Passes the input script name and one argument for the python script.
rem ////     1 : drive, e.g. "T:"
rem ////     2 : full path for working directory, including drive
rem ////     3 : full path to Emme python script
rem ////     4 : single argument for python script
rem ////
rem ////
rem //////////////////////////////////////////////////////////////////////////////
rem
rem if necessary can set the EMMEPATH to point to a specific version of Emme
rem set EMMEPATH=C:\Program Files\INRO\Emme\Emme 4\Emme-4.3.5
rem
rem
set MODELLER_PYTHON=%EMMEPATH%\Python37\
set path=%EMMEPATH%\programs;%MODELLER_PYTHON%;%PATH%
rem map T drive for file access
net use t: \\sandag.org\transdata /persistent:yes
%1      rem set the drive
cd %2   rem change to the correct directory
rem restart the ISM as script user, must be configured to already be connected to mustang
taskkill /F /IM INROSoftwareManager.exe /T
PING localhost -n 5 >NUL
start /d "C:\Program Files (x86)\INRO\INRO Software Manager\INRO Software Manager 1.1.0" INROSoftwareManager.exe
PING localhost -n 5 >NUL
rem start the python script with one input
python %3 %4