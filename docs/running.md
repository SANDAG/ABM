# Installing and Running ABM3

This page describes how to install and run ABM3, including hardware and software requirements. In general, a powerful server is required to run the model. The main software required for the model includes EMME, Python, and Java. EMME is a commercial transportation modeling platform that must be purchased separately and requires a computer with a Windows operating system. Python is an open-source cross-platform programming language that is currently one of the most popular programming languages. Python is the core language of ActivitySim. Java is an open-source programming language required for certain model components. 

## System Requirements

ABM3 runs on a Microsoft Windows workstation or server, with the minimum and recommended system specification as follows:

Minimum specification:

- Operating System: 64-bit Windows 7, 64-bit Windows 8 (8.1), 64-bit Windows 10, 64-bit Windows Server 2019
- Processor: 24-core CPU processor
- Memory: 1 TB RAM
- Disk space: 600 GB

Recommended specification:

- Operating System: 64-bit Windows 10
- Processor: Intel CPU Xeon Gold / AMD CPU Threadripper Pro (24+ cores)
- Memory: 1 TB RAM
- Disk space: 1200 GB

In general, a higher CPU core count and RAM will result in faster run times as long as ActivitySim is configured to utilize the additional processors and RAM.
Note that the model is unlikely to run on servers that have less than 1 TB of RAM.


## Software Requirements

Three software applications, [EMME](https://www.bentley.com/software/emme/), Python package manager [Anaconda](https://www.anaconda.com/), and [Java](https://www.java.com/en/) should be installed on the computer that will be used to run the model.
The ABM3 model system is an integrated model that is controlled by and primarily runs in the EMME transportation planning software platform. EMME is used for network assignment and creating transportation skims, and the model's Graphical User Interface (GUI). The software also provides functionality for viewing and editing highway and transit network files and viewing of matrix files. The Bentley Connect Edition software (the license manager for EMME) will need to be logged in and activated prior to running the model. 

A Python package manager is software that creates an environment for an instance of Python. ActivitySim and related Python processes in the model are executed in an environment that is setup with a specific version of Python and specific library versions. This ensures that changes outside of the Python environment will not cause errors or change model results, and additionally ensure that the specific version of Python and specific libraries needed by the model do not cause errors or changes to other Python software installations on the server. The libraries needed by ActivitySim extend the base functionality of Python. Note that Anaconda requires a paid subscription for agencies larger than 200 users. To install Anaconda, follow the instructions [here](https://www.anaconda.com/). 

Java is required in order to create bicycle logsums, run the taxi/TNC routing model, and run the intra-household autonomous vehicle routing model. The model has been tested against Java version 8 (e.g. 1.8) but should run in later versions as well. 


## Installing ABM3 Model

As noted above, Anaconda will need to be installed. Once one of those two package managers is installed, a specific computer environment must be created to run ActivitySim. The environment is a configuration of Python that is for ActivitySim - this environment allows ActivitySim to use specific software libraries without interfering with the server's installed version of Python (if one exists, it is not required) and keeps other Python installations from interfering with ActivitySim. To create the environment, use the following commands from within the Anaconda PowerShell Prompt. 

First, change directories using cd /d to the model's ActivitySim config folder, which is /source/configs/activitysim (e.g., cd /d e:\ABM3\source\configs\activitysim) prior to running the command below.

conda env create --file environment.yml 

After installing the environment, do a quick test of it by activating it, using:

conda activate asim_baydag


## The ABM3 GUI

//TODO

Show the GUI and describe the options

## Setting up your machine to run ABM3

1. Add the following to System Properties > Environment Variables
    - Under System Variables
        - CONDA_PREFIX={path to Anaconda3}
            - e.g., CONDA_PREFIX=C:\Anaconda3
        - CONDA_PREFIX_TWO={path to Anaconda2}
            - e.g., CONDA_TWO_PREFIX=C:\ProgramData\Anaconda2
        - Add the following to *Path*
            - {path to Anaconda2}
            - {path to Anaconda2}\Scripts
            - {path to Anaconda2}\Library\bin\

2. Create a directory called *python_virtualenv* on your machine's C Drive and copy in the following files:
    - [add_python_virtualenv.bat](https://github.com/SANDAG/ABM/blob/ABM3_develop/add_python_virtualenv.bat)
    - [requirements.txt](https://github.com/SANDAG/ABM/blob/ABM3_develop/requirements.txt)

3. Open up the Emme Shell as Administrator and launch *add_python_virtualenv.bat*
    - If you have a Power User account, you can input the following code into the Emme Shell to run as both Administrator and as a Power User:
        <code>runas /netonly /user:YourUser cmd.exe</code>
    - Ensure all relevant libraries were created under e.g., *C:\python_virtualenv\abm14_2_0\Lib\site-packages*

4. Under e.g, *C:\Program Files\INRO\Emme\Emme 4\Emme-4.3.7\Python27\Lib\site-packages* copy in [python_virtualenv.pth](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/main/emme/python_virtualenv.pth)
