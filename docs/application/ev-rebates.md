# Electric Vehicle Rebates
One of the policies that SANDAG planners would like to test for the 2025 Regional Plan is providing rebates for low- and middle-income households to purchase electric vehicles. One of the variables in the vehicle type choice model is the [new purchase price](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/resident/vehicle_type_choice_op4.csv#L12-L17) for a vehicle of a given age, body type, and fuel type. The way the EV rebate is implemented in ABM3 is by deducting the appropriate rebate value for plugin and battery vehicles if a household meets the criteria (based on percentage of the federal poverty level). To configure the rebate values and poverty level thresholds, new constants were added to the common/constants.yaml configuration file. Two describe income group cutoffs and need to be updated in [src\asim\configs\common\constants.yaml](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/common/constants.yaml) and four specific rebate values for various income groups and need to be edited for the scenario's given year in [input\parametersByYears.csv](https://github.com/SANDAG/ABM/blob/ABM3_develop/input/model/parametersByYears.csv).

| Constant | parametersByYears field | Description | 
| -------- | ----------------------- | ----------- |
| LowIncomeEVRebateCutoff | N/A | % of poverty limit below which a household is considered low-income for the purposes of EV rebates |
| MedIncomeEVRebateCutoff | N/A | % of poverty limit below which a non low-income household is considered medium-income for the purposes of EV rebates |
| LowIncomeBEVRebate | ev.rebate.lowinc.bev | Rebate value for low-income households buying battery electric vehicles |
| LowIncomePEVRebate | ev.rebate.lowinc.pev | Rebate value for low-income households buying plugin electric vehicles |
| MedIncomeBEVRebate | ev.rebate.medinc.bev | Rebate value for medium-income households buying battery electric vehicles |
| MedIncomePEVRebate | ev.rebate.medinc.pev	| Rebate value for medium-income households buying plugin electric vehicles |

The constants fit into the policy as follows:

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
