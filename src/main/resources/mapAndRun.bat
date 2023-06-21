:: script to map a drive and then call a batch file remotely using psexec
:: 1 is the drive letter to map (e.g. �M:�)
:: 2 is the share to map (e.g. �\\w-ampdx-d-sag01\mtc�)
:: 3 is the password
:: 4 is the user
:: 5 is the working directory for calling the batch file (starting from the mapped drive letter)
:: 6 is the name of the batch file to call
:: 7-10 are extra arguments (note DOS only does 9 arguments unless you use SHIFT)

SET ONE=%1
SET TWO=%2
SET THREE=%3
SET FOUR=%4
SET FIVE=%5
SET SIX=%6
SET SEVEN=%7
SET EIGHT=%8
SET NINE=%9
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
SHIFT
SET TEN=%1

net use %ONE% %TWO% /persistent:yes
%ONE%
cd %FIVE%
call %SIX% %SEVEN% %EIGHT% %NINE% %TEN%
