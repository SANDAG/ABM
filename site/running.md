# Installing and Running ABM3

This page describes how to install and run ABM3, including hardware and software requirements. In general, a powerful server is required to run the model. The main software required for the model includes EMME, Python, and Java. EMME is a commercial transportation modeling platform that must be purchased separately and requires a computer with a Windows operating system. Python is an open-source cross-platform programming language that is currently one of the most popular programming languages. Python is the core language of ActivitySim. Java is an open-source programming language required for certain bespoke non-ActivitySim model components. 


## System Requirements

ABM3 runs on a Microsoft Windows workstation or server, with the minimum and recommended system specification as follows:

Minimum specification:

- Operating System: 64-bit Windows 7, 64-bit Windows 8 (8.1), 64-bit Windows 10, 64-bit Windows Server 2019

- Processor: 24-core CPU processor

- Memory: 1 TB RAM

- Disk space: 500 GB

Recommended specification:

- Operating System: 64-bit Windows 10

- Processor: Intel CPU Xeon Gold / AMD CPU Threadripper Pro (24+ cores)

- Memory: 1 TB RAM

- Disk space: 1000 GB

In general, a higher CPU core count and RAM will result in faster run times as long as ActivitySim is configured to utilize the additional processors and RAM.

Note that the model is unlikely to run on servers that have less than 1 TB of RAM, unless chunking is set to active and in explicit mode (requires an upcoming version of ActivitySim 1.3)


## Software Requirements

Three software applications, [EMME](https://www.bentley.com/software/emme/), Python package manager [Anaconda](https://www.anaconda.com/), and [Java](https://www.java.com/en/) should be installed on the computer that will be used to run the model.

The ABM3 model system is an integrated model that is controlled by and primarily runs in the EMME transportation planning software platform. EMME is used for network assignment and creating transportation skims, and the model's Graphical User Interface (GUI). The software also provides functionality for viewing and editing highway and transit network files and viewing of matrix files. The Bentley Connect Edition software (the license manager for EMME) will need to be logged in and activated prior to running the model. 

A Python package manager is software that creates an environment for an instance of Python. ActivitySim and related Python processes in the model are executed in an environment that is setup with a specific version of Python and specific library versions. This ensures that changes outside of the Python environment will not cause errors or change model results, and additionally ensure that the specific version of Python and specific libraries needed by the model do not cause errors or changes to other Python software installations on the server. The libraries needed by ActivitySim extend the base functionality of Python. Note that Anaconda requires a paid subscription for agencies larger than 200 users. To install Anaconda, follow the instructions [here](https://www.anaconda.com/). 

Java is required in order to create bicycle logsums, run the taxi/TNC routing model, and run the intra-household autonomous vehicle routing model. The model has been tested against Java version 8 (e.g. 1.8) but should run in later versions as well. 


## Installing ABM3 Model


### Setting up the Python environments

As noted above, User needs to install Anaconda on the machine they are working on, if it is not already installed. The following step is creating a specific environment to run ActivitySim. The environment is a configuration of Python that is for ActivitySim - this environment allows ActivitySim to use specific software libraries without interfering with the server's installed version of Python (if one exists, it is not required) and keeps other Python installations from interfering with ActivitySim. 

To run ABM3, user needs to install two different python environments, one in Python 3, which will be used by all ActivitySim-based models, and one in Python 2, which is required as long as the EMME version in use still depends on Python 2, and is used to convert the omx rip tables out of the ActivitySim models. To set up these environments, use the following instruction from within the Anaconda 3 PowerShell Prompt for Python 3 and Anaconda 2 PowerShell Prompt for Python 2. 

To set up the Python 3 environment, first, change directories using cd /d to the environment folder under the ActivtySim source code directory. As of June 2024, this directory may be cloned from the BayDAG_estimation branch located on the SANDAG's forked version of ActivitySim [here](https://github.com/SANDAG/activitysim/tree/BayDAG_estimation). The environment folder in this directory contains a number of yaml files that may be used to install the environment. User may use the following command to install the AcitvitySim environment along with SANDAG's version of AcitivtySim under the asim_baydag name.

```conda env create --file=activitysim-dev.yml -n asim_baydag```

After installing the environment, do a quick test of it by activating it, using:

```conda activate asim_baydag```

To set up the Python 2 environment, user simply needs to install the openmatrix package in the base environment. To do so, first open the Anaconda 2 terminal and use the following command to install the openmatrix package:

```pip install openmatrix```


### Installing Java

Java version 1.81 needs to be installed on the server. SANDAG servers usually have this version of Java already installed on them.


### Creating a scenario folder

Follow the steps below to create a model scenario folder using SANDAG’s tool:



* Check for an open server using the DOS tool mascot-abm.
* Log into server
* Using the show hidden icons arrow on the taskbar, click the INRO Software Manager to check if EMME license is operating. If it's not, log into license server on Mustang by typing in the Host box: mustang.sandag.org:5170 (automatic)
* Using the server’s Windows Explorer, navigate to T:\ABM\release\ABM\version_14_2_2\dist
* Double click on createStudyAndScenario.exe and click Run.
* On pop-up, enter fields in Create an ABM scenario. Make sure to enter the correct ABM version, EMME version, year, and know your Scenario Folder (where you will run the model from; typically study area/abm_runs) and your Network Folder (input networks). Browse to an existing folder or copy (ctrl + c) and paste (ctrl + v) paths to minimize chance of typos. After fields are entered, click Create.
* A DOS window will open and necessary files will be copied. If you have a server with a valid license, it will pause for about 30-45 seconds on init emme folder. You will get a pop-up window that says you have successfully created the scenario. Click Quit. Exit the GUI by clicking on Quit.
* Go to your newly created abm_run folder and double check on input files/date stamps to verify they match expected date stamps. Make any modifications to input files (i.e. ```parametersByYears.csv``` for adjusting auto operating costs, ```filesByYears.csv``` for specifying year-specific files, or ```mgra_based_inputXXX.csv``` file for adjusting parking costs)


## Running ABM3

To open the EMME application from the created scenario directory, user needs to go to the emme_project folder, and open the start_emme_with_virtualenv.bat file. This opens up the EMME application, where the application prompts the choice of a scenario. It is recommended to select the main highway scenario (Scen. 100) to start off the model run, although other scenarios may be selected as well. 

Following this step, user should open the EMME Modeler by clicking on the gold square sign at the top left of the screen. 
<br>
<div align="center">
    <img src="images\running\1.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>EMME Modeler icon</em>
</div>

<br>
The EMME Modeler opens to the EMME Standard Toolbox, but needs to be switched to the SANDAG toolbox by selecting it from the bottom-left of the screen. From this toolbox, open the Master Run tool.

<br>
<div align="center">
    <img src="images\running\2.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Opening SANDAG Toolbox and Master run</em>
</div>
<br>
Opening the Master Run tool allows the user to run all or part of the model, and set a number of settings such as sample size.

<br>
<div align="center">
    <img src="images\running\3.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Master Run tool</em>
</div>
<br>
The Master run tool operates the SANDAG travel demand model. To operate the model, configure the inputs by providing Scenario ID, Scenario title and Emmebank title, and keeping Number of Processors to default. Select main ABM directory will automatically be set to the current project directory and does not require change.



* Main ABM directory: directory which contains the ABM scenario data, including this project. The default is the parent directory of the current Emme project.
* Scenario ID: Scenario ID for the base imported network data. The result scenarios are indexed in the next five scenarios by time period.
* Scenario title: title to use for the scenario.
* Emmebank title: title to use for the Emmebank (Emme database)
* Number of processors: the number of processors to use for traffic and transit assignments and skims, aggregate demand models (where required) and other parallelized procedures in Emme. Default is ```Max available – 1```.
* Properties (```conf/sandag_abm.properties```)
    * On opening the **Master run** tool the ```sandag_abm.properties``` file is read and the values cached and the inputs below are pre-set. When the Run button is clicked this file is written out with the values specified. Any manual changes to the file in-between opening the tool and clicking the Run button are overwritten.
    * Sample rate by iteration: three comma-separated values for the ActivitySim sample rates for each iteration
    * Start from iteration: start iteration for the model run
    * Skip steps: optional checkboxes to skip model steps.
* Select link: add select link analyses for traffic.

By expanding the Run model – skip steps drop down, the user can make any custom changes. Usually the defaults should be sufficient although if you are using a new bike network, you should uncheck the Skip bike logsums and check the Skip copy of bikelogsum.

<br>
<div align="center">
    <img src="images\running\4.png" alt="alt_text" title="image_tooltip">
    <br>
    <em>Run model tool</em>
</div>
<br>

Following this setup, you can click Run to start the model run. We recommend occasionally checking the model run status to make sure the run is going smoothly. When the model run finishes successfully, the Master Run tool will show a model run successful message in green at the top of the tool window.

If the run is unsuccessful (there will be an error prompt from Emme), check Emme logbook and log files (under “logfiles”) for clues to where it stopped.

As the model runs, a full runtime trace of the model steps, inputs and reports is recorded in the Modeller Logbook. As each step completes, it will record an entry in the Logbook along with reports and other results. The Logbook can be opened from the Clock-like icon in the upper right of the Modeller window. This icon can also be found in the toolbar in the Emme Desktop. If a Modeller tool is running, a window will pop-up over the Emme Desktop which includes a Show Logbook button (this window can be closed to use Desktop worksheets and tables while any tool is running). Click on the Refresh button to update the logbook view with the latest status.

<br>
<div align="center">
    <img src="images\running\7.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Modeller logbook</em>
</div>
<br>


The Logbook provides a real time, automated documentation of the model execution. The overall structure of the model is represented at the top level, with the occurrence, sequence and repetition of the steps involved in the model process. Nested Logbook entries may be collapsed or expanded to see detail. For the Emme assignment procedures, interactive charts are recorded. The statistical summaries of the impedance matrices are recorded for each time period following the assignment. These summary tables provide an easy way to check for skims with obvious outlier values.
