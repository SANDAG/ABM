@echo off

if "%1"=="" goto usage
if "%2"=="" goto usage

set FILE_LIST=hwycov.e00 turns.csv trcov.e00 trlink.bin trrt.bin trstop.bin trrt.csv tap.elev tap.ptype timexfer.bin timexfer.dcb accessam.prp accesspm.prp fare.mtx tapcov.dbf tapcov.shp tapcov.shx tapcov.shp.xml

@echo %FILE_LIST%

for %%i in (%FILE_LIST%) do (
@echo Copying %1\%%i
copy /Y %1\%%i %2)

set FILE_LIST=trlink.BX trrt.BX trstop.BX
for %%i in (%FILE_LIST%) do (
del %2\%%i
)
goto :eof

:usage
@echo Usage: %0 ^<from_path^> ^<to_path^>