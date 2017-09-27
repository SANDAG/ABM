set vs_path=C:\Program Files (x86)\Microsoft Visual Studio 14.0
set OLDPATH=%PATH%
set PATH=%PATH%;%vs_path%\vc;%vs_path%\bin

call vcvarsall.bat x64


rem CL /nologo /Gr /LD /O2 /Feshrp.rev.vdf SHRP.rev.c user32.lib
rem for debug use: 
CL /nologo /Gr /LD /Zi /Od /Feshrp.rev.vdf SHRP.rev.c user32.lib
set PATH=%OLDPATH%
