# Instructions for running Visualizer
1. Open Anaconda prompt and activate the asim_baydag environment.
2. Navigate to [MODEL_RUN]/src/asim/visualizer/visualizer.
3. Run RunViz.bat
4. Ensure the output csv files are updated in the ./visualizer/data folder
5. Open the visualizer.pbit in PowerBI and give it the path to the ./visualizer folder

# How to change comparisons:
Run the visualizer for another model run, and then copy the contents of that output folder into a folder and set that folder as [runs][survey] in configs/combine.yaml. The comparison run will be labeled "Reference" in the visualizer.

# How it works:
The visualizer is created using code from \\sandag.org\transdata\data\tools\Data-Pipeline-Tool.  This is a separate repository built for the data pipeline.
However, the specific data processing procedures for this visualizer run are listed in the /visualizer/config folder.

The Data-Pipeline-Tool creates a set of summary csvs which are then read into powerBI.  The same summaries have been created for the household travel survey data and are contained in the ./visualizer/hts folder which are also loaded into powerBI for comparison. First the data pipeline tool is run. This runs many merging and preprocessing steps defined in settings.yaml and processor.csv, respectively. Then, many groupby operations are performed as defined in expressions.csv. Finally some post-procesesing steps are performed as defined in processor.csv. The full sequencing of what is done is defined in settings.yaml. To debug any part of the process, add a step in processor.csv of type "debug," which will call `pdb.set_trace()` in the console window. All objects stored in memory are within a dictionary called `data_dict`. After the data pipeline is run, a script called combine.py is run combining the model run summaries with summaries of a survey or reference run. The ways in which the files are combined is defined in combine.yaml.

# Data Sources:
./visualizer/hts files came from BY2016 Summaries.  Sent by Joe Flood via email on 1/3/2023
./visualizer/ABM2P_2019Summaries came from ABM2+ 2019 run.  Sent by Joe Flood via email on 1/3/2023
./visualizer/hts_2022 files came from BY2022 Summaries. Sent by Ali Etezady via email on 1/23/2024 and processed by Joe Flood, Madhav Ponnudurai, and Alexander Berryhill-Williams

# NOTE:
The following visualizations have placeholder data for both the model and reference data:
- Average Distance to Parking Location
- Distance to Parking Location

The follwing visualizations have placeholder data for the reference data:
- Autonomous Vehicle Ownership
- E-Bike Ownership
- External Tour Frequencies
- Parking Subsidy
- Households with At Least 1 Joint Tour
- Vehicle Allocation Summaries
