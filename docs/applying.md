# Applying the model

This page contains information needed to apply the model.

## Network Coding

//TODO: Describe network attributes, how to code network

## Population Synthesis

//TODO: Describe population synthesis procedure, how to modify inputs and construct new future-year synthetic population

## Land-Use Data Preparation

//TODO: Describe how to prepare land-use data.

Describe how to update parking costs, enrollment data.

## Micromobility

//TODO: Describe how to run micromobility policy tests


## Scenario manager

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

## Electric Vehicle Rebates
One of the policies that SANDAG planners would like to test for the 2025 Regional Plan is providing rebates for low- and middle-income households to purchase electric vehicles. One of the variables in the vehicle type choice model is the [new purchase price](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/resident/vehicle_type_choice_op4.csv#L12-L17) for a vehicle of a given age, body type, and fuel type. The way the EV rebate is implemented in ABM3 is by deducting the appropriate rebate value for plugin and battery vehicles if a household meets the criteria (based on percentage of the federal poverty level). To configure the rebate values and poverty level thresholds, [new constants](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/common/constants.yaml#L290) were added to the common/constants.yaml configuration file. The constants fit into the policy as follows:

| Fuel Type | `LowIncomeEVRebateCutoff` < Household Poverty Level <= `MedIncomeEVRebateCutoff` | Household Poverty Level <= `LowIncomeEVRebateCutoff` |
| --------- | -------------------------------------------------------------------------------- | ---------------------------------------------------- |
| BEV       | `MedIncomeBEVRebate`                                                             | `LowIncomeBEVRebate`                                 |
| PEV       | `MedIncomePEVRebate`                                                             | `LowIncomePEVRebate`                                 |

For example, if the following policy were to be tested...
| Fuel Type | 300-400% Federal Poverty Limit | 300% Federal Poverty Limit or lower |
| --------- | ------------------------------ | ----------------------------------- |
| BEV       | $2,000                         | $6,750                              |
| PEV       | $1,000                         | $3,375                              |

...then the constants would need to be set as follows:
~~~
LowIncomeEVRebateCutoff: 3
MedIncomeEVRebateCutoff: 4
LowIncomeBEVRebate: 6750
LowIncomePEVRebate: 3375
MedIncomeBEVRebate: 2000
MedIncomePEVRebate: 1000
~~~

## Flexible Fleets
The of the five big moves defined in SANDAG's 2021 regional plan was [Flexible Fleets](https://www.sandag.org/projects-and-programs/innovative-mobility/flexible-fleets), which involves on-demand transit services. The [initial concept](https://www.sandag.org/-/media/SANDAG/Documents/PDF/regional-plan/2025-regional-plan/2025-rp-draft-initial-concept-2024-1-25.pdf) of the 2025 Regional Plan involves rapidly expanding these services, with many new services planned to be in operation by 2035. For this reason, it is important that these services be modeled by ABM3. There are two flavors of flexible fleets that were incorporated into ABM3, Neighborhood Electric Vehicles (NEV) and microtransit. A table contrasting these services is shown below.
| Characteristic  | NEV     | Microtransit |
| --------------- | ------- | ------------ |
| Vehicle Size    | Smaller | Larger       |
| Service Area    | Smaller | Larger       |
| Operating Speed | Slower  | Faster       |
