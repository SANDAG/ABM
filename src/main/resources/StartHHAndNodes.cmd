set PROJECT_DRIVE=%1
set PATH_NO_DRIVE=%2

%PROJECT_DRIVE%
cd %PATH_NO_DRIVE%

rem remove active connections so that limit is not exceeded
net session /delete /Y

call %PATH_NO_DRIVE%\bin\CTRampEnv.bat
If %SNODE%==true goto :snode

%PATH_NO_DRIVE%\bin\pskill \\%NODE1%  java
%PATH_NO_DRIVE%\bin\pskill \\%NODE2%  java

rem Start HH Manager on master node
call %PATH_NO_DRIVE%\bin\runHhMgr.cmd %PROJECT_DRIVE% %PATH_NO_DRIVE%

rem Start remote worker nodes: SANDAG02
set PROGRAMSTRING=%PATH_NO_DRIVE%\bin\runSandag02.cmd %MAPDRIVE% %PATH_NO_DRIVE%
start %PATH_NO_DRIVE%\bin\psExec \\%NODE1% -s -c -f %PATH_NO_DRIVE%\bin\%MAPANDRUN% %MAPDRIVE% %MAPDRIVEFOLDER% %PASSWORD% %USERNAME% %PATH_NO_DRIVE% %PROGRAMSTRING%

rem start remote worker nodes: SANDAG03
set PROGRAMSTRING=%PATH_NO_DRIVE%\bin\runSandag03.cmd %MAPDRIVE% %PATH_NO_DRIVE%
start %PATH_NO_DRIVE%\bin\psExec \\%NODE2% -s -c -f %PATH_NO_DRIVE%\bin\%MAPANDRUN% %MAPDRIVE% %MAPDRIVEFOLDER% %PASSWORD% %USERNAME% %PATH_NO_DRIVE% %PROGRAMSTRING%
goto :end

:snode
rem Start HH Manager on master node
call %PATH_NO_DRIVE%\bin\runHhMgr.cmd %PROJECT_DRIVE% %PATH_NO_DRIVE%

:end





