set PROJECT_DRIVE=%1
set PATH_NO_DRIVE=%2

%PROJECT_DRIVE%
cd %PATH_NO_DRIVE%

rem remove active connections so that limit is not exceeded
net session /delete /Y

call %PATH_NO_DRIVE%\bin\CTRampEnv.bat

%PATH_NO_DRIVE%\bin\pskill \\%NODE1%  -u %USERNAME% -p %PASSWORD% java
%PATH_NO_DRIVE%\bin\pskill \\%NODE2%  -u %USERNAME% -p %PASSWORD% java

rem Start HH Manager on node1
set PROGRAMSTRING=%PATH_NO_DRIVE%\bin\runHhMgr_1129.cmd %MAPDRIVE% %PATH_NO_DRIVE%
start %PATH_NO_DRIVE%\bin\psExec \\%NODE1% %MAPANDRUN% %MAPDRIVE% %MAPDRIVEFOLDER% %PASSWORD% %USERNAME% %PATH_NO_DRIVE% %PROGRAMSTRING% -u %USERNAME% -p %PASSWORD%

rem Start worker node: SANDAG01
call %PATH_NO_DRIVE%\bin\runSandag01.cmd %PROJECT_DRIVE% %PATH_NO_DRIVE%

rem Start remote worker nodes: SANDAG02
set PROGRAMSTRING=%PATH_NO_DRIVE%\bin\runSandag02.cmd %MAPDRIVE% %PATH_NO_DRIVE%
start %PATH_NO_DRIVE%\bin\psExec \\%NODE1% %MAPANDRUN% %MAPDRIVE% %MAPDRIVEFOLDER% %PASSWORD% %USERNAME% %PATH_NO_DRIVE% %PROGRAMSTRING% -u %USERNAME% -p %PASSWORD%

rem start remote worker nodes: SANDAG03
set PROGRAMSTRING=%PATH_NO_DRIVE%\bin\runSandag03.cmd %MAPDRIVE% %PATH_NO_DRIVE%
start %PATH_NO_DRIVE%\bin\psExec \\%NODE2% %MAPANDRUN% %MAPDRIVE% %MAPDRIVEFOLDER% %PASSWORD% %USERNAME% %PATH_NO_DRIVE% %PROGRAMSTRING% -u %USERNAME% -p %PASSWORD%

