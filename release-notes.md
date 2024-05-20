# Release Notes

## Version 15.0.0 (May 2024)
For use in the 2025 Region Plan (RP), SANDAG has developed a new activity-based travel model, known as ABM3. The biggest change from SANDAG’s previous ABM, ABM2+, is the transition from the Java-based CT-RAMP modeling platform to the open-source Python-based [ActivitySim](https://research.ampo.org/activitysim/) platform that has been developed by a consortium of public agencies (of which SANDAG is a founding member) for the past decade. ABM3 will be the first ActivitySim model to be used in production for planning purposes. While most of the model components from ABM3 were translated from ABM2+, there were several notable enhancements that were made, which are described below. Further, several models were either re-estimated and/or recalibrated to match 2022 data.
### Features
- The base year was [updated](https://github.com/SANDAG/ABM/pull/68) from 2016 to 2022, allowing the behavioral models to reflect changes in travel behavior derived from the onset of the COVID-19 pandemic.
- The zone system was transitioned from a 3-zone system (TAZ, MAZ, TAP) to a [2-zone system](https://github.com/SANDAG/ABM/pull/65) (TAZ, MAZ).
- E-bike and E-scooter were added as [travel modes](https://github.com/SANDAG/ABM/pull/66).
- A new [commercial vehicle model](https://github.com/SANDAG/ABM/pull/64) (CVM) was implemented using ActivitySim, that includes firm synthesis, commercial vehicle tours, and TNC services such as food delivery.
- Network editing was changed from the ArcInfo-based TCOVED to [TNED](https://github.com/SANDAG/ABM/pull/133), a new network editing system.
- [Flexible fleet services](https://github.com/SANDAG/ABM/pull/104) were added for full trips along with first-mile access to and last-mile egress from fixed-route transit.
- The airport model has been [updated](https://github.com/SANDAG/ABM/pull/9) with new access options, including the proposed automated people-mover.
- The cross-border model was [updated](https://github.com/SANDAG/ABM/pull/53), including a new wait time model.
- A new [disaggregate accessibility](https://github.com/ActivitySim/activitysim/pull/635) calculation step was introduced to improve the precision of accessibilities used by modeling components.
- Transit assignment for different time periods is now run in [parallel](https://github.com/SANDAG/ABM/pull/82).
- The auto ownership model has been moved [before](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/resident/settings_mp.yaml#L32) work and school location.
- The shadow pricing algorithm was [updated](https://github.com/ActivitySim/activitysim/pull/613) to constrain mandatory tour locations to availability instead of traditional shadow pricing.
- Model results are [automatically written](https://github.com/SANDAG/ABM/pull/86) to a database stored in the cloud via Microsoft Azure Services.
#### New Resident Model Components
- Autonomous Vehicle Ownership
- External Worker Identification
- External Workplace Location
- Transit Pass Subsidy
- Transit Pass Ownership
- Vehicle Type Choice
- Telecommute Frequency
- External Joint Tour Identification
- External Joint Tour Location
- External Nonmandatory Tour Identification
- External Nonmandatory Tour Destination
- Vehicle Allocation
