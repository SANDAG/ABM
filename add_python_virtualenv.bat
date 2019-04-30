REM
REM Must be run from the Emme Shell for the installed version of Emme
REM Requires administrative permssions
REM Requires the "requirements.txt" with the list of Python libraries
REM and versions on the C drive (or change the path to requirements.txt)
REM
python -m pip install --upgrade pip
python -m pip install virtualenv
cd C:\
IF NOT EXIST python_virtualenv (
    mkdir python_virtualenv
)
python -m virtualenv python_virtualenv\abm14_1_0
python_virtualenv\abm_14_1_0\Scripts\activate
pip install -r requirements.txt
deactivate