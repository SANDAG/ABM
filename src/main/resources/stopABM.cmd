rem stopping all java processes on cluster

%CD%\pskill \\${master.node.name} java.exe
%CD%\pskill \\${node.1.name} java.exe
%CD%\pskill \\${node.2.name} java.exe
