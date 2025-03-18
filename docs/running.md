# Installing and Running ABM3

This page describes how to install and run ABM3, including hardware and software requirements. In general, a powerful server is required to run the model. The main software required for the model includes EMME, Python, and Java. EMME is a commercial transportation modeling platform that must be purchased separately and requires a computer with a Windows operating system. Python is an open-source cross-platform programming language that is the core language of ActivitySim. Java is an open-source programming language required for certain bespoke non-ActivitySim model components. 


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

To run ABM3, there are certain software that should be installed on your workstation including: [EMME](https://www.bentley.com/software/emme/), Python2.7 package manager [Anaconda2](https://repo.anaconda.com/archive/), Python3 package manager [Anaconda3](https://www.anaconda.com/), and [Java](https://www.java.com/en/).

The ABM3 model system is an integrated model that is controlled by and primarily runs in the EMME transportation planning software platform. EMME is used for network assignment, creating transportation skims, and the model's Graphical User Interface (GUI). The software also provides functionality for viewing and editing highway and transit network files and viewing of matrix files. The [Bentley CONNECTION Client](https://www.bentley.com/software/connection-client/) software (the license manager for EMME) will need to be logged into and activated prior to running the model.

A Python package manager is software that creates an environment for an instance of Python. ActivitySim and related Python processes in the model are executed in an environment that is setup with a specific version of Python and specific library versions. This ensures that changes outside of the Python environment will not cause errors or change model results, and additionally ensure that the specific version of Python and specific libraries needed by the model do not cause errors or changes to other Python software installations on the server. The libraries needed by ActivitySim extend the base functionality of Python.

Java is required in order to create bicycle logsums, run the taxi/TNC routing model, and run the intra-household autonomous vehicle routing model. The model has been tested against Java version 8 (i.e., 1.8) and is therefore the recommended version to be used when running the SANDAG ABM.

## Installing ABM3

### Setting up the Python Environments

As noted above, the user needs to install Anaconda2 and Anaconda3 on the workstation they intend to install ABM3 on. The Anaconda2 software is utilized to create a Python2-based environment required to interface with a specific version of EMME (4.3.7) whereas Anaconda3 is utilized to create environments to run ActivitySim.

#### Python2 Environment

After Anaconda installation, the following step is to create the Python2-based environment, which can be done via the following steps:

* Create a directory called *python_virtualenv* on the server's local drive and copy in the following files:
    * [add_python_virtualenv.bat](https://github.com/SANDAG/ABM/blob/ABM2_TRUNK/add_python_virtualenv.bat)
    * [requirements.txt](https://github.com/SANDAG/ABM/blob/ABM2_TRUNK/requirements.txt)
* Open EMME Shell (likely as Administrator)
* Navigate (via *cd* command) to wherever *add_python_virtualenv.bat* was copied to
* Execute *add_python_virtualenv.bat*
* Copy [python_virtualenv.pth](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/main/emme/python_virtualenv.pth) to EMME's site-packages directory
    * For example, *C:\Program Files\INRO\Emme\EMME 4\Emme-4.3.7\Python27\Lib\site-packages*

If installation was succesfully completed, you will have created an environment at e.g., *C:\python_virtualenv\abm14_2_0*. Ensure relevant libraries were installed under the environment's *site-packages* directory. 

#### Python3 Environments

The following step is creating two Python3-based Activitysim environments: one for running ActivitySim and the other specificallly for running the ActivitySim-based Commercial Vehicle Model (CVM).

##### ActivitySim Environment

###### SANDAG Users

To create the ActivitySim (*asim_baydag*) environment:

* Open Anaconda3 Prompt as Administrator
* Execute the following commands:

```
conda update -n base conda
net use T: \\sandag.org\transdata
cd T:\ABM\dev\ABM3\src\asim\scripts
T:
conda env create --file=environment.yml -n asim_baydag
conda activate asim_baydag
pip install azure-identity
pip install azure-storage-blob
cd T:/ABM/dev/ActivitySim
pip install –e .
```

###### External Users

To create the ActivitySim (*asim_baydag*) environment, first, change directories using ```cd /d``` to the *conda-environments* folder under the ActivtySim source code directory. As of June 2024, this directory may be cloned from the *BayDAG_estimation* branch located on [SANDAG's forked version of ActivitySim](https://github.com/SANDAG/activitysim/tree/BayDAG_estimation). The *conda-environments* folder in this directory contains a number of yaml files that may be used to install the environment. Users may use the following command to install the AcitvitySim environment along with SANDAG's version of AcitivtySim under the *asim_baydag* name:

```conda env create --file=activitysim-dev.yml -n asim_baydag```

After installing the environment, users may confirm successful installation by activating it using:

```conda activate asim_baydag```

##### CVM ActivitySim Environment

###### SANDAG Users

To create the ActivitySim CVM environment:

* Open Anaconda3 Prompt as Administrator
* Execute the following commands:

```
cd T:\ABM\dev\CVM
T:
conda env create -p {path to Anaconda3}/envs/asim_sandag_cvm --file activitysim/conda-environments/activitysim-dev-base.yml
conda activate asim_sandag_cvm
pip install -e ./sharrow
pip install -e ./activitysim
```

Note: On servers, {path to anaconda3} could be e.g., ```C:/ProgramData/Anaconda3/``` or ```C:/Anaconda3```

###### External Users

To create the ActivitySim CVM environment:

* Open Anaconda3 Prompt
* Execute the following commands

```
mkdir workspace
cd workspace
git clone https://github.com/ActivitySim/sharrow.git
git clone https://github.com/camsys/activitysim.git
cd activitysim
git switch time-settings
cd ..
conda env create -p {path to Anaconda3}/envs/asim_sandag_cvm --file activitysim/conda-environments/activitysim-dev-base.yml
conda activate C:/Anaconda3/envs/asim_sandag_cvm
pip install -e ./sharrow
pip install -e ./activitysim
```

Note: For {path to anaconda3} point to wherever Anaconda3 is installed on your workstation

### Environment Variables

During an ABM3 model run, there are certain system environment variables that get referenced. These variables must be added to your workstation's environment variables. To do so:

* Navigate to your workstation's *System Properties* > *Advanced* > *Environment Variables*
* Under *System variables*, using the *New...* button, add the following variables:
    * CONDA_PREFIX={path to Anaconda3}
        * e.g., C:\Anaconda3
    * CONDA_TWO_PREFIX={path to Anaconda2}
        * e.g., C:\Anaconda2
* Under *System variables* > *Path*, using the *Edit...* button, add (if not already present) the following variables to *Path*:
    * {path to Anaconda2}
    * {path to Anaconda2}\Scripts
    * {path to Anaconda2}\Library\bin
    * {path to Anaconda3}
    * {path to Anaconda3}\Scripts
    * {path to Anaconda3}\Library\bin

#### Azure Environment Variables

*Note: This section mainly relevant to SANDAG users.*

When an ABM3 scenario finalizes, it loads onto Azure for post-processing. There are a number of system environment variables necessary to establish a connection to the Azure system. 

To create those Azure system environment variables:

* Open PowerShell as Administrator
* Navigate (via *cd* command) to *T:\ABM\software\PS1*
* Execute *.\ABM_Variables.ps1*
    * If you encounter an error message indicating the .ps1 file is not digitally signed, you may bypass the execution policy for your current PowerShell session by running: ```Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass``` Then, re-execute *ABM_Variables.ps1*.

### SQLCMD

*Note: This section mainly relevant to SANDAG users.*

An installation of the *sqlcmd* utility is necessary to execute Transact-SQL statements directly in the command line. Within the ABM3 modeling framework, it is utilized to execute a command that identifies the next available Azure scenario ID in preparation for loading a finalized ABM3 scenario onto Azure. 

To install the sqlcmd utility, download and execute the latest Microsoft Software Installer (i.e., .msi) file from the [Microsoft sqlcmd GitHub repository](https://github.com/microsoft/go-sqlcmd/releases) (hint: *sqlcmd-amd64.msi*)

To verify successful installation of sqlcmd utility on your workstation:

* Copy [GetScenarioId.bat](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/main/resources/GetScenarioId.bat) somewhere onto your workstation
* Open command prompt
* Navigate (via *cd* command) to location where *GetScenarioId.bat* was copied to
* Execute the following command:
    * ```GetScenarioId.bat 9999 sqlcmdTest "dev"```
        * *Note: The steps under Azure Environment Variables must be executed beforehand*
* If installation and command execution were successful, you should see the following:

<br>
<div align="center">
    <img src="images\running\sqlcmd_test.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>SQLCMD Utility Test</em>
</div>

### Installing Java

Java version 1.8 needs to be installed on the workstation. SANDAG servers usually have this version of Java already installed on them. 

Alternatively, the exact version may be found on Oracle's [Java SE 8 Archive Downloads](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) site. (hint: *jre-8u162-windows-x64.exe*)

## Creating an ABM3 Scenario

*Note: This section mainly relevant to SANDAG users.*

Follow the steps below to create an ABM3 scenario directory:

* Check for an available server using the [SANDAG Live Report: ABM Compute Servers](http://vpdamwiis-01.sandag.org:5000/modeling) page
* Log onto available server and ensure:
    1. Server's C Drive has at least 500 GB worth of space
    2. You are logged onto the Bentley CONNECTION Client license manager software
* Using the server’s Windows Explorer, navigate to the latest approved ABM3 release. As of March 2025, this is *T:\ABM\release\ABM\version_15_2_2*
* Double click on *createStudyAndScenario.bat* and click Run.
* On pop-up, fill out the fields under *Create an ABM scenario*.
    * **EMME Version**: As of March 2025, only available version is 4.3.7
    * **Year**: Using drop-down menu, select scenario year you'd like to run
    * **Base**: Whether you would like to run a Build or No Build scenario. Note that the base 2022 year only has the option to run as Base.
    * **Geography ID**: Leave as 1. As of March 2025, this field has no functionality. 
    * **Scenario Folder**: File path you would like to create scenario at. The tool will create the directory if it doesn't already exist. For cases where you point to an exisitng directory, the tool will notify users that location exists to avoid unintended overwriting.
    * **Network Folder**: File path to input network. 
    * **Land Use Folder**: File path to input land use files.
* After fields are filled out, click *Create*.
* A DOS window will open and all required files will be copied. If you have a server with a valid license, it will pause for about 30-45 seconds on *init EMME folder*. You will get a pop-up window that indicates you have successfully created the scenario. Click *Quit*. Then, you may exit the GUI by clicking on *Quit*.
* Navigate to newly created scenario and ensure the correct input files were copied over.

## Running ABM3

To open the EMME application from the created scenario directory, users need to go to the *emme_project* folder, and double-click on the *start_emme_with_virtualenv.bat* file. This opens up the EMME application, where the application prompts the choice of a scenario. It is recommended to select the main highway scenario (Scen. 100) to start off the model run, although other scenarios may be selected as well. 

Following this step, users should open the *EMME Modeler* by clicking on the gold square icon at the top left of the screen. 
<br>
<div align="center">
    <img src="images\running\1.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>EMME Modeler icon</em>
</div>

<br>
The *EMME Modeler* opens to the *EMME Standard Toolbox*, but needs to be switched to the *SANDAG toolbox* by selecting it from the bottom-left of the screen. From this toolbox, open the *Master Run* tool by double-clicking it.

<br>
<div align="center">
    <img src="images\running\2.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Opening SANDAG Toolbox and Master run</em>
</div>
<br>

The *Master Run* tool allows the user to run all or part of the model, and set a number of settings such as sample size per iteration.

<br>
<div align="center">
    <img src="images\running\master_run.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Master Run tool</em>
</div>
<br>
The *Master run* tool operates the SANDAG travel demand model. Users have the option to configure a number of settings prior to launching the model run:

* **Select main ABM directory**: No changes necessary. The file path to the scenario. Defaults to the parent directory of opened EMME project.
* **Scenario ID**: No changes necessary. Scenario ID for the base imported network data. The result scenarios are indexed in the next five scenarios by time period. 
* **Scenario title**: Title to use for the scenario. Standard practice is to input scenario title.
* **Emmebank title**: Title to use for the Emmebank (EMME database). Standard practice is to input scenario title.
* **Number of processors**: the number of processors to use for traffic and transit assignments and skims, aggregate demand models (where required) and other parallelized procedures in Emme. Default is ```Max available – 1```.
* **Datalake Environment**: Datalake environment to which scenario outputs will be loaded onto: Prod (Production), or Dev (Development). This parameter mostly relevant to SANDAG users.
* **Sampe rate by iteration**: Three comma-separated values for the ActivitySim sample rates for each iteration.

By expanding the *Run model – skip steps* drop down, the user has the option to skip certain steps of the model run. Usually, the defaults (see image below) should be sufficient. It should be noted that checkmarking a step means that it will be skipped.

<br>
<div align="center">
    <img src="images\running\master_run_skip_steps.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Run model tool</em>
</div>
<br>

On opening the **Master run** tool, the ```conf/sandag_abm.properties``` file is read and its values pre-set the *Run model - skip steps* inputs. When the *Run* button is clicked, this file is written out with the values specified. Any manual changes to the file in-between opening the tool and clicking the *Run* button are overwritten.

* **Start from iteration**: Indicate from what iteration you would like model run to start.
* ***Skip steps***: Optional checkboxes to skip model steps.
* **Select link(s)**: Add select link analyses for traffic.

Following this setup, you can click *Run* to start the model run. It is recommended to occasionally check the model run status to make sure the run is going smoothly. When the model run finishes successfully, the *Master run* tool will show a model run successful message in green at the top of the tool window.

If the run is unsuccessful (there will be an error prompt from EMME), check the *EMME Logbook* and log files (under the *logFiles* or *output* directories) for clues to where it may have crashed.

As the model runs, a full runtime trace of the model steps, inputs and reports is recorded in the *Modeller Logbook*. As each step completes, it will record an entry in the Logbook along with reports and other results. The Logbook can be opened from the clock-like icon in the upper right of the *Modeller* window. This icon can also be found in the upper left toolbar in the *EMME Desktop*, or upper right in the *EMME Logbook*. If a *Modeller* tool is running, a window will pop-up over the *EMME Desktop* which includes a *Show Logbook* button (this window can be closed to use Desktop worksheets and tables while any tool is running). Click on the *Refresh* button to update the Logbook view with the latest status.

<br>
<div align="center">
    <img src="images\running\modeller_logbook.jpg" alt="alt_text" title="image_tooltip">
    <br>
    <em>Modeller logbook</em>
</div>
<br>


The Logbook provides a real-time, automated documentation of the model execution. The overall structure of the model is represented at the top level, with the occurrence, sequence and repetition of the steps involved in the model process. Nested Logbook entries may be collapsed or expanded to see detail. For the EMME assignment procedures, interactive charts are recorded. The statistical summaries of the impedance matrices are recorded for each time period following the assignment. These summary tables provide an easy way to check for skims with obvious outlier values.
