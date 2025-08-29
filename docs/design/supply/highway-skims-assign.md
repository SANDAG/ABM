# Highway Skimming and Assignment

The traffic assignment for the SANDAG model is a 15-class assignment with generalized cost on links and BPR-type volume-delay functions which include capacities on links and at intersection approaches. The assignment is run using the fast-converging Second-Order Linear Approximation (SOLA) method in Emme to a relative gap of 5×10⁻⁴. The per-link fixed costs include toll values and operating costs which vary by class of demand (see Table x for the complete list of classes). Assignment matrices and resulting network flows are always in PCE. 

*Table 1. List of traffic demand classes and key class parameter values*

| Name      | Mode | VOT ($0.01/min) | PCE | Cost Attribute     |
|-----------|------|------------------|-----|--------------------|
| SOV_NT_L  | s    | 8.81             |     | @cost_auto         |
| SOV_TR_L  | S    | 8.81             |     | @cost_auto         |
| HOV2_L    | H    | 8.81             |     | @cost_hov2         |
| HOV3_L    | I    | 8.81             |     | @cost_hov3         |
| SOV_NT_M  | s    | 18               |     | @cost_auto         |
| SOV_TR_M  | S    | 18               |     | @cost_auto         |
| HOV2_M    | H    | 18               |     | @cost_hov2         |
| HOV3_M    | I    | 18               |     | @cost_hov3         |
| SOV_NT_H  | s    | 85               |     | @cost_auto         |
| SOV_TR_H  | S    | 85               |     | @cost_auto         |
| HOV2_H    | H    | 85               |     | @cost_hov2         |
| HOV3_H    | I    | 85               |     | @cost_hov3         |
| TRK_L     | T    | 67               | 1.3 | @cost_lgt_truck    |
| TRK_M     | M    | 68               | 1.5 | @cost_med_truck    |
| TRK_H     | V    | 89               | 2.5 | @cost_hvy_truck    |


## Volume-delay Functions

The volume-delay functions are specified as open-ended algebraic expressions supporting standard functions. The VDF functions for SANDAG are a modified BPR of the form:

$$
T = T_0 \cdot \left(1 + \alpha_1 \left(\frac{\text{FLOW} + \text{PRELOAD}}{\text{CAPACITY}}\right)^{\beta_1} \right)
+ \frac{\text{CYCLE}}{2} (1 - \text{GC})^2 \cdot \left(1 + \alpha_2 \left(\frac{\text{FLOW} + \text{PRELOAD}}{\text{INT_CAPACITY}}\right)^{\beta_2} \right)
$$


#### Where:
- **T0** is the free-flow travel time along the link in minutes  
- **ALPHA1**, **BETA1**, **ALPHA2**, and **BETA2** are BPR calibration terms  
- **FLOW** is the assigned flow from the traffic demand in PCEs  
- **PRELOAD** is the background volume from transit vehicles in PCEs  
- **CAPACITY** is the link mid-block capacity  
- **INT_CAPACITY** is the total intersection approach capacity in PCEs  
- **CYCLE** is the signal cycle length in minutes  
- **GC** is the green-to-cycle length for the link approach  

#### Attributes:
- Attribute keyword for **FLOW**: `volau`  
- Attribute for **PRELOAD**: `volad`  
    - This is calculated from the transit itineraries, their frequency, and the length of the period and is also stored in link data 2 (`ul2`)

#### Per-link attributes:
- **T0**: link data 1 (`ul1`)  
- **CAPACITY**: link data 2 (`ul3`)  
- **GC**: `@green_to_cycle`, cross-referenced by `el1`  
- **INT_CAPACITY**: `@capacity_inter`, cross-referenced by `el3`  

#### Global parameters (small subset of values for all links):
- **CYCLE**: 1.25, 1.5, 2.0, or 2.5  
- **ALPHA1**: always 0.8  
- **BETA1**: 5.5 or 4  
- **ALPHA2**: 6.0 or 4.5  
- **BETA2**: always 2  

---

With these global parameters, there are 7 total volume-delay functions:


- **fd10** for freeways and links which do not end at an intersection:  

$$
\text{ul1} \cdot \left(1.0 + 0.24 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^{5.5} \right)
$$

- **fd20** for local collector and lower intersection and stop-controlled approaches:  

$$
\text{ul1} \cdot \left(1.0 + 0.8 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right) +
\frac{1.25}{2} \cdot (1 - \text{el1})^2 \cdot \left(1 + 4.5 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{el3}}\right)^2 \right)
$$

- **fd21** for collector intersection approaches:  

$$
\text{ul1} \cdot \left(1.0 + 0.8 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right) +
\frac{1.5}{2} \cdot (1 - \text{el1})^2 \cdot \left(1.0 + 4.5 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{el3}}\right)^2 \right)
$$

- **fd22** for major arterial and major or prime arterial intersection approaches:  

$$
\text{ul1} \cdot \left(1.0 + 0.8 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right) +
\frac{2.0}{2} \cdot (1 - \text{el1})^2 \cdot \left(1.0 + 4.5 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{el3}}\right)^2 \right)
$$

- **fd23** for primary arterial intersection approaches:  

$$
\text{ul1} \cdot \left(1.0 + 0.8 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right) +
\frac{2.5}{2} \cdot (1 - \text{el1})^2 \cdot \left(1.0 + 4.5 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{el3}}\right)^2 \right)
$$

- **fd24** for metered ramps:  

$$
\text{ul1} \cdot \left(1.0 + 0.8 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right) +
\frac{2.5}{2} \cdot (1 - \text{el1})^2 \cdot \left(1.0 + 6.0 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{el3}}\right)^2 \right)
$$

- **fd25** for freeway node approach (AM and PM only):  

$$
\text{ul1} \cdot \left(1.0 + 0.6 \cdot \left(\frac{\text{volau} + \text{volad}}{\text{ul3}}\right)^4 \right)
$$


## Traffic Skims and Results

The traffic skims are calculated by fixing the flows and running a second zero-iteration assignment, which computes the O-D skim values using path analyses. The total generalized cost, travel time, and distance are computed for all classes, as well as the toll cost, HOV facility distance, and managed lane distance for the applicable classes. The fixed flows are the MSA averaged flows for iterations after the first global iteration.  

Note that the assigned flows for trucks are in PCE values unless otherwise specified. This includes any select type results. 
