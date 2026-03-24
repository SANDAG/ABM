# Running ABM3

This page describes how to create and run an ABM3 scenario.

## Creating an ABM3 Scenario

*Note: This section mainly relevant to SANDAG users.*

Follow the steps below to create an ABM3 scenario directory:

* Check for an available server using the [SANDAG Live Report: ABM Compute Servers](http://vpdamwiis-01.sandag.org:5000/modeling) page
* Log onto available server and ensure:
    1. Server's C Drive has at least 500 GB worth of space
    2. You are logged onto the Bentley CONNECTION Client license manager software
* Using the server’s Windows Explorer, navigate to the latest approved ABM3 release. As of March 2026, this is *T:\ABM\release\ABM\version_15_3_1*
* Double click on *createStudyAndScenario.bat* and click Run.
* On pop-up, fill out the fields under *Create an ABM scenario*.
    * **EMME Version**: As of March 2026, EMME version is OpenPaths EMME 25.00.01
    * **Year**: Using drop-down menu, select scenario year you'd like to run
    * **Base**: Whether you would like to run a Build or No Build scenario. Note that the base 2022 year only has the option to run as Base.
    * **Geography ID**: Leave as 1. As of March 2026, this field has no functionality. 
    * **Scenario Folder**: File path you would like to create scenario at. The tool will create the directory if it doesn't already exist. For cases where you point to an exisitng directory, the tool will notify users that location exists to avoid unintended overwriting.
    * **Network Folder**: File path to input network. 
    * **Land Use Folder**: File path to input land use files.
* After fields are filled out, click *Create*.
* A DOS window will open and all required files will be copied. If you have a server with a valid license, it will pause for about 30-45 seconds on *init EMME folder*. You will get a pop-up window that indicates you have successfully created the scenario. Click *Quit*. Then, you may exit the GUI by clicking on *Quit*.
* Navigate to newly created scenario and ensure the correct input files were copied over.

## Running ABM3

To open the EMME application from the created scenario directory, users need to go to the *emme_project* folder, and double-click on the *emme_project.emp* file. This opens up the EMME application, where the application prompts the choice of a scenario. It is recommended to select the main highway scenario (Scen. 100) to start off the model run, although other scenarios may be selected as well. 

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
    <img src="images\running\master_run.JPG" alt="alt_text" title="image_tooltip">
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
    <img src="images\running\master_run_skip_steps.JPG" alt="alt_text" title="image_tooltip">
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

### Rerunning an ABM3 Scenario

*Note: This section mainly relevant to SANDAG users.*

Within the SANDAG workspace, an ABM3 scenario is typically created on an internal shared drive called the T Drive. If the *Use the local drive during the model run* is checkmarked within the *Master Run* tool, when launched, the scenario is first copied over and then ran on the machine's local Drive (i.e. C Drive). After model completion, all necessary files are copied back to the T Drive and the scenario is deleted off the C Drive.

If the same scenario from above would like to be reran:

* From beginning to end (i.e., from iteration 1):
    * The modeler simply needs to open the scenario via the *emme_project.emp* file (on T Drive), edit (if applicable) the setup in the *Master Run* tool, and launch.
* Or, starting from the 2nd or 3rd iteration:
    * The modeler must use the *copyRun* tool to copy the entire ABM3 scenario to the machine's C Drive (typically under C:\abm_runs\\{username}). The *copyRun* tool is required (rather than a manual copy) because with ABM release 15.3, the skims are converted into the OMXZ format for more efficient reading into ActivitySim. This conversion is not performed after final assignment, so manually copying a run without using the *copyRun* tool would result in the resident model failing to run. In particular, the modeler should ensure the files found within the *emme_project* folder are present on the C Drive before relaunching the scenario.
    * From there, the modeler may open the scenario via the *emme_project.emp* file on the T Drive, edit the setup in the *Master Run* tool, and launch.

Note that under the above two workflows, the original scenario's (on T Drive) outputs will be overwritten when the scenario is copied from the C Drive back to the T Drive. The modeler should consider saving the original scenario's outputs in a different location if wanting to preserve the original data. 

An alternative workflow, that may be useful for development or debugging purposes, involves rerunning the scenario entirely on the C Drive. It should be noted that under this worfklow, the model outputs of the rerun will not be copied back to the T Drive as part of the model run flow (may be copied over manually if desired). To proceed with this workflow:

* The modeler must use the *copyRun* tool to copy the entire ABM3 scenario to the machine's C Drive (typically under C:\abm_runs\\{username}). The *copyRun* tool processes the skims into the OMXZ format required for ActivitySim in release 15.3.
* Then, the modeler must open the scenario via the *emme_project.emp* file on the C Drive. 
* Lastly, in addition to any desired setup edits under the *Master Run* tool, the modeler must uncheck *Use the local drive during the model run* so that the model run does not attempt to copy scenario files to a different drive.

One other point the modeler should consider when rerunning a scenario is whether model outputs should be loaded onto Azure Data Lake, which is controlled via the *Skip write to datalake* parameter under the *Master Run* tool:

* Unchecking this step will result in model outputs being loaded onto Azure Data Lake under a new scenario ID and will not overwrite the original run's data on Azure Data Lake
* Checkmarking this step will skip model outputs from being loaded onto Azure Data Lake. This will likely be the preferred approach for scenario reruns related to development or debugging purposes.

### Resuming a Crashed ABM3 Run

*Note: This section mainly relevant to SANDAG users.*

If an ABM3 model run crashes before completion, the modeler should first consult the *EMME Logbook*, which provides detailed error messages to help diagnose the cause of the crash. After investigating the error and fixing any input or script issues, the modeler can resume the run from various points depending on where the crash occurred. The table below provides guidance on which steps to skip in the *Master Run* tool to resume from different crash locations:

| **Crash Location** | **Start from iteration** | **Steps to Skip** |
|-------------------|-------------------------|-------------------|
| Import network | 1 | None - rerun from beginning |
| Bike logsum | 1 | Skip steps before bike logsums |
| Iter 1 traffic/transit assignment | 1 | Skip steps before Iter 1 traffic/transit assignment |
| Iter 1 resident model | 1 | Skip steps before Iter 1 ActivitySim Preprocessing |
| Iter 1 special market model | 1 | Skip steps before Iter 1 ActivitySim Preprocessing, and skip Iter 1 Resident Model |
| Iter 2 traffic/transit assignment | 2 | Skip steps before Iter 2 traffic/transit assignment |
| Iter 2 resident model | 2 | Skip steps before Iter 2 ActivitySim Preprocessing |
| Iter 2 special market model | 2 | Skip steps before Iter 2 special marker model |
| Iter 3 traffic/transit assignment | 3 | Skip steps before Iter 3 traffic/transit assignment |
| Iter 3 resident model | 3 | Skip steps before Iter 3 ActivitySim Preprocessing |
| Iter 3 special market model | 3 | Skip steps before Iter 3 special marker model |
| Iter 4 traffic/transit assignment | 4 | Skip steps before Iter 4 traffic/transit assignment |
| Iter 4 write to datalake | 4 | Skip all previous steps and uncheck the box of 'Skip write to datalake' |

**Important Notes:**

* Before resuming a crashed run, verify that the outputs from previously completed steps are present in the scenario directory.
* When resuming from an iteration other than iteration 1, ensure the *Start from iteration* parameter in the *Master Run* tool is set correctly.
* For crashes during traffic/transit assignment, it's typically safer to re-run the assignment rather than trying to resume partway through.
* If uncertain about which steps to skip, consult the *EMME Logbook* to determine exactly which steps completed successfully before the crash.

