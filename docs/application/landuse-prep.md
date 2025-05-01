# Land-Use Data Preparation

The repo [https://github.com/SANDAG/landuse_prep_tool](https://github.com/SANDAG/landuse_prep_tool) is used to process land use data for ABM3. 

The [land use preparation tool](https://github.com/SANDAG/landuse_prep_tool) processes outputs from the Estimates and Forecasts team for use in ABM3. It consists of three main components: 

  1. [Processing outputs of the 2022 parking inventory to impute missing data](https://github.com/SANDAG/landuse_prep_tool/tree/main/1_1_Parking#1-reduction-and-imputing-raw-parking-inventory-data). 

  2. Estimating regression models to [predict the number of free and paid parking spaces](https://github.com/SANDAG/landuse_prep_tool/tree/main/1_1_Parking#2-creation-of-space-estimation-model) within each MGRA. 

  3. Generating [household, person, and land use inputs](https://github.com/SANDAG/landuse_prep_tool/tree/main/2_ABM_Preprocess#landuse_prep_tool) required to run the ABM. 

In most cases, only the third step needs to be executed—unless there are updates to the base year network or parking inventory data. 