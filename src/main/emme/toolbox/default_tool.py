#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2019.                                                 ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/default_tool.py                                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Sets the environment, must be the first tool opened to ensure correct
# paths are used
#


# Add the custom Sandag virtualenv to the system path
import sys
VIRUTALENV_PATH = "C:\\python_virtualenv\\abm14_1_0\\Lib\\site-packages"
if not VIRUTALENV_PATH in sys.path:
    sys.path.insert(1, VIRUTALENV_PATH)


# Delegate to load the standard Default tool
import inro.modeller as _m

default_mod = _m.Modeller().module("inro.emme.default_tool")


class Tool(_m.Tool(), default_mod.DefaultTool):
    pass