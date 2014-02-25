@echo off
::set the batch file directory
SET BD=%~dp0

::set the install directory; it is the directory behind this one
:: first save the current directory, change directories to get absolute path, then go back
:: save current directory
pushd .
cd %BD%..
set MDIR=%CD%

:: restore current directory
popd

CALL:copy_dll ntlmauth.dll
::pause for a few seconds
ping 127.0.0.1 -n 3 > NUL

set MDIRF=%MDIR:\=/%

SET JDIR=%MDIR%\application\
SET CP=%JDIR%jtds-1.3.0.jar;%JDIR%jxl.jar;%JDIR%logback-classic-0.9.29.jar;%JDIR%logback-core-0.9.29.jar;%JDIR%poi-3.7-20101029.jar;%JDIR%poi-ooxml-3.7-20101029.jar;%JDIR%sandag_abm.jar;%JDIR%slf4j-api-1.6.2.jar

SET OLD_PATH=%PATH%
SET PATH=%PATH%;%JDIR%

set JAVA_64_PATH=C:\Progra~1\Java\jre7
echo %JAVA_64_PATH%\bin\java -Xmx10000m -cp "%CP%" org.sandag.abm.reporting.emfac2011.Emfac2011Runner "%MDIRF%/config/sandag_emfac2011.properties" "@@model.dir=%MDIRF%/"
%JAVA_64_PATH%\bin\java -Xmx10000m -cp "%CP%" org.sandag.abm.reporting.emfac2011.Emfac2011Runner "%MDIRF%/conf/sandag_abm.properties" "@@model.dir=%MDIRF%/"
SET PATH=%OLD_PATH%

GOTO END

:copy_dll
SET found=
FOR %%X in (%~1) do (set found=%%~$PATH:X)
IF NOT DEFINED found COPY %MDIR%\code\%~1 c:\Windows\System32\%~1
GOTO:END

:END
