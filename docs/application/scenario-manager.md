# Scenario manager

ABM3 uses a python module as the scenario manager. The job of this scenario manager is updating the parameters used throughout the model to match a specific scenario’s definition and needs. A number of these parameters including auto operating cost, taxi and TNC fare, micromobility cost, and AV ownership penetration are usually assumed to change by forecast year or scenario.

Manually changing these parameters requires the model user to know where each parameter is located, and individually changing them according to the scenario forecast values. A scenario manager, therefore, can be a convenient and efficient tool to automate this process.

The ABM3 Scenario Manager reads in a CSV input file (located under ```input/parametersByYears.csv```) containing the parameter values for each scenario, and updates the associated parameters in the ActivitySim config files. A snapshot of this input parameter CSV file is shown below, where each row is associated with a specific scenario year/name. The parameter names used here can either be identical to the parameter names used in ActivitySim, or different. In case the parameter names are different, a separate file is used to map the parameters names between the input CSV and ActivitySim config files.


| Scenario Year | AOC fuel | AOC maintenance | Taxi baseFare | Taxi costPerMile | Taxi costPerMinute |
| ------------- | -------- | --------------- | ------------- | ---------------- | ----------------- |
| 2012          | 13.5     | 6.3             | 1.78          | 1.87             | 0.08              |
| 2014          | 12.9     | 6.3             | 1.78          | 1.87             | 0.08              |
| 2015          | 19.5     | 6.2             | 1.78          | 1.87             | 0.08              |
| 2016          | 10.7     | 5.6             | 1.78          | 1.87             | 0.08              |
| 2017          | 10.8     | 5.5             | 1.78          | 1.87             | 0.08              |

The scenario manager is run as part of the model setup in the Master Run tool before any ActivitySim model is run (usually only in the first iteration of the run). Model user can choose to run or skip this step, although it is highly recommended to run with each run to ensure correct parameters.
