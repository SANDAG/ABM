# Validation


## Data

The model validation process was used to test the model’s predictive capabilities before using it to produce forecasts. Highway validation of the model was conducted using observed count data as the on-the-ground target. Observed traffic count and transit boardings as collected and maintained by SANDAG were used in the validation of the ABM3 model. Observed traffic counts for highway validation were collected through the Caltrans Highway Performance Monitoring System (HPMS) estimates as well as local jurisdictions. SANDAG staff developed analyses to cross-reference the counts with the ABM3 model network. For the transit validation the model’s predicted transit ridership was compared to observed ridership for each transit market. Observed transit boardings were derived from local transit agencies. The boardings were preprocessed to match ABM3 mode and time of day periods.


## Methods

For validation, two types of model validations were used;dynamic validation, and static validation. Dynamic validation involves systematically varying model inputs to assess the reasonableness of model responses. Dynamic validation is often done in the form of sensitivity testing. Sensitivity tests must be carefully formulated in order to isolate model responses to the inputs that vary. This requires limiting input changes to only those directly related to the sensitivity test and comparing the outputs against a baseline scenario. More details on sensitivity testing may be found in the Sensitivity Test Documentation. 

The static validation process compares model outputs with observed data that was not used to build the travel model. In the assignment step, model demand (e.g. trips by time period, mode, and vehicle class/value-of-time) are loaded onto the network. In highway assignment, the output includes vehicle flows on every link (road) in the highway network and for transit assignment, the output includes the number of boardings on each route. These are compared to observed traffic counts and observed transit ridership respectively. 

Vehicle miles of travel (VMT) and link volumes were used for highway validation. Predicted VMT was compared against observed VMT based on count data, and Caltrans HPMS estimates. Comparisons between predictions and these two data sources yielded opposite results (underestimate of daily VMT compared to observed daily VMT based on count data, and an over prediction of regional model VMT compared to HPMS data). Predicted regional traffic flows were also compared against observed traffic counts through link volumes. FHWA recommendations of statistical measures of model fit were used to assess model performance (R<sup>2</sup> higher than 0.88 , PRMSE of lower than %40). 

For transit validation, ridership (boarding) is compared by transit line-haul mode and time of day comparing the predicted counts against the observed ridership obtained from the Passenger Count Program. Validation followed the FHWA recommendation of predicted ridership values by route group (local bus, express bus, etc.) within 20% of the target values.
