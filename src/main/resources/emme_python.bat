rem //////////////////////////////////////////////////////////////////////////////
rem ////
rem //// emme_python.bat 
rem ////
rem //// Configure environment and start Python script to run Emme-related task.
rem //// Passes the input script name and two arguments for the python script.
rem ////
rem ////
rem //////////////////////////////////////////////////////////////////////////////
rem
rem if necessary can set the EMMEPATH to point to a specific version of Emme
rem set EMMEPATH=C:\Program Files\INRO\Emme\Emme 4\Emme-4.3.3
rem
rem
set MODELLER_PYTHON=%EMMEPATH%\Python27\
set path=%EMMEPATH%\programs;%MODELLER_PYTHON%;%PATH%
rem map T drive for file access
net use t: \\sandag.org\transdata /persistent:yes
python %1 %2 %3