rem stopping all java processes on cluster

%CD%\pskill \\localhost java.exe
%CD%\pskill \\localhost java.exe
%CD%\pskill \\${node.2.name} java.exe
