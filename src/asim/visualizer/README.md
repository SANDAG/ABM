Instructions for running Visualizer

1. Copy ActivitySim output to output folder and the landuse and raw skim file to the ./data folder.  This path should be then set in the visualizer/config/settings.yaml file.
2. Open terminal in the ./visualizer folder
3. Activite anaconda environment that was used to run ActivitySim (if you need to create a visualizer environment, uncomment associated lines in the RunViz.bat script)
4. Run RunViz.bat
5. Ensure the output csv files are updated in the ./visualizer/data folder
6. Open the visualizer.pbit in PowerBI and give it the path to the ./visualizer folder

How to change comparisons:

TODO


How it works:

The visualizer is created using code from \\sandag.org\transdata\data\tools\Data-Pipeline-Tool.  This is a separate repository built for the data pipeline.
However, the specific data processing procedures for this visualizer run are listed in the /visualizer/config folder.

The Data-Pipeline-Tool creates a set of summary csvs which are then read into powerBI.  The same summaries have been created for the household travel survey data and are contained in the ./visualizer/hts folder which are also loaded into powerBI for comparison.

Data Sources:
./visualizer/hts files came from BY2016 Summaries.  Sent by Joe Flood via email on 1/3/2023
./visualizer/ABM2P_2019Summaries came from ABM2+ 2019 run.  Sent by Joe Flood via email on 1/3/2023