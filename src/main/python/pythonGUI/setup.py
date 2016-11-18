__author__ = 'wsu'
from distutils.core import setup
import py2exe

setup(windows=['./src/main/python/pythonGUI/creaScenario.py'])
setup(windows=['./src/main/python/pythonGUI/ABMGUI.py'])
setup(windows=['./src/main/python/pythonGUI/parameterEditor.py'])

