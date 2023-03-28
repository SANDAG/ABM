__author__ = 'wsu'
import sys
import ctypes
"""
ctypes is a foreign function library for Python.
It provides C compatible data types, and allows calling functions in DLLs or shared libraries.

sys provides access to operating system.
It provides access to some variables used by the interpreter.
"""
path=sys.argv[1]
minSpace=sys.argv[2]
_, total, free = ctypes.c_ulonglong(), ctypes.c_ulonglong(), ctypes.c_ulonglong()
if sys.version_info >= (3,) or isinstance(path, str):
    fun = ctypes.windll.kernel32.GetDiskFreeSpaceExW
else:
    fun = ctypes.windll.kernel32.GetDiskFreeSpaceExA
ret = fun(path, ctypes.byref(_), ctypes.byref(total), ctypes.byref(free))
if ret == 0:
    raise ctypes.WinError()
totalMB=total.value/1024.0/1024.0
freeMB=free.value/2014.0/1024.0
usedMB = totalMB- freeMB

if freeMB < int(minSpace):
    print("free space on C <",minSpace,"MB!")
    sys.exit()
else:
    print("Total MB on C:",totalMB)
    print("Used MB on C:",usedMB)
    print("Free MB on C:",freeMB)


