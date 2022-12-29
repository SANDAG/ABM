rem CONDA_PREFIX is a system environment variable that points to the location of Anaconda3
set root=%CONDA_PREFIX%
call %root%\Scripts\activate.bat %root%
cd..
cd python
python assignScenarioID.py
pause