rem @echo off

rem ### Declaring required environment variables
set JAVA_64_PATH = %1
set CLASSPATH = %2;%3
set PROJECT_DIRECTORY = %4


rem ### Running master node and redirecting output to {PROJECT_DIRECTORY}\logFiles\sandag01Console.log
2>&1 (%JAVA_64_PATH%\bin\java -server -Xms16m -Xmx16m -cp "%CLASSPATH%" -Dlog4j.configuration=log4j-sandag01.properties -Djppf.config=jppf-sandag01.properties org.jppf.node.NodeLauncher) | %PROJECT_DIRECTORY%\application\GnuWin32\bin\tee.exe %PROJECT_DIRECTORY%\logFiles\sandag01Console.log

rem ### Exit window
exit 0