 # Calibration 


## Data
\
The primary data used for the ABM3 model calibration was the 2022 SANDAG Household Travel Survey (HTS) Wave 1. This was augmented with American Community Survey (ACS) 2021 data (specifically for the auto ownership model), DMV data (specifically for the vehicle type choice model), and the 2015 onboard survey (OBS) data for transit trips. \
\
Most models in ABM3 were calibrated, with the exception of Disaggregate Accessibility, AV Ownership, Vehicle Type Choice, Mandatory Tour Frequency, Mandatory Tour Scheduling, School Escorting, Joint Tour Scheduling, Non-Mandatory Tour Scheduling, Vehicle Allocation, At-work subtour scheduling, Trip Purpose, Trip Scheduling and Parking Location. Most of the calibrated models were calibrated to the 2022 HTS. However, the Auto Ownership, Work From Home and Telecommute Frequency were calibrated using ACS data. The Tour Mode Choice, At-work Subtour Mode Choice, and Trip Mode Choice models were calibrated using HTS data, and adjusted to OBS data. \
\
For further details on the data used for the calibration of the model, see the [ABM3 Calibration And Validation Report](https://sandag.github.io/ABM/pdf_reports/202403_ABM3_Calibration_And_Validation_Report.pdf).


## Method  

### Theory
\
All models in ABM3 follow the multinomial logit framework, a type of discrete choice model in which each alternative is assigned a utility value, and the probability of selecting each alternative is computed based on its utility. During model implementation, we sometimes encounter model results that do not sufficiently replicate observed shares (e.g., survey data), necessitating calibration. To improve model fit, alternative-specific constants are introduced to achieve a reasonable level of fit to observed data. These constants reflect non-included attributes of the alternative and measurement error. 

We introduce an alternative-specific constant \($ \alpha_i $ \) into \($ V_i $\), the utility function for each alternative:

$$ V_i' = V_i + \alpha_i $$

This adjustment modifies the choice probability equation as follows:

$$ P_i = \frac{e^{V_i + \alpha_i}}{\sum_{j} e^{V_j + \alpha_j}} =\frac{e^{V_i} }{\sum_{j} e^{{V_j} + \alpha_j}} * e^{a_i}$$

The first term on the right-hand side of the equation above is almost equal to the original model-predicted probability, or $ P_i^{model} $, with the difference being that the sum of utilities in the denominator also includes the impact of the added constant $ \alpha_i $. Assuming that this term is equal to the original model-predicted probability, we can determine the appropriate value of $ \alpha_i $ by setting the modified choice probability above to the observed share from data:
$ P_i^{observed} = P_i^{model} e^{\alpha_i} $

Solving for $ \alpha_i $:


$\alpha_i = \ln \left( \frac{P_i^{observed}}{P_i^{model}} \right) $

In simpler terms, the calibration constant is computed as follows:

$ \text{calibration constant} = \ln \left( \frac{\text{observed share}}{\text{model share}} \right) $

Due to ignoring the impact of $ \alpha_i $ in the denominator of $ P_i^{model} $, the calibration process cannot conclude in just one iteration but needs to be performed iteratively, where the results are added to the alternative-specific constant from the previous iteration. The adjustment increases or decreases the utility of the alternative based on the under- or over-estimated share, thus changing the probability of the alternative and ultimately the number of the predicted choices.
, calibration process cannot conclude in just one iteration, but needs to be performed iteratively, where the results are added to the alternative-specific constant from the previous iteration. The adjustment increases or decreases the utility of the alternative based on the under or over estimated share, thus changing the probability of the alternative and ultimately the number of the predicted.\
\
Typically there is one less alternative-specific constant than the number of alternatives; the alternative without a constant is referred to as the \'91base\'92 alternative. In some models, the alternative-specific constants are relatively simple, where the constants are not stratified by any attributes of the decision-maker, while in other models, such as the coordinated daily activity pattern model or tour mode choice, the constants are stratified by socio-economic variables. In the case of the coordinated daily activity pattern model, the constants are stratified by person type, while in tour mode choice, constants are stratified by auto sufficiency, income, or geographical district. \

### Application
\
Calibration was completed using [Jupyter notebooks](https://github.com/SANDAG/ABM/tree/ABM3_develop/src/asim/calibration/resident). During calibration, calibration coefficients were calculated for each model (work from home, external workplace location, etc.), and for each specified segmentation within the model if appropriate (person type, tour length, etc). For a full list of models, see the [ABM3 Calibration and Validation Report.](https://sandag.github.io/ABM/pdf_reports/202403_ABM3_Calibration_And_Validation_Report.pdf) The calibrated model was iteratively run and compared to the target until an optimal fit between the model and the target data was achieved. A visualizer (Power BI) was utilized to easily identify inconsistencies between the calibrated model and the target data. These inconsistencies were addressed by the team and on a case by case basis were justified or resolved. Summaries by district were also used to determine if there were regional differences of optimal fit between the calibration model and the target data. If necessary, regional calibration constants were also utilized. 