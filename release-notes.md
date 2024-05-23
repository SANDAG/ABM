# Release Notes

## Version 15.0.0 (May 23, 2024)
For use in the 2025 Region Plan (RP), SANDAG has developed a new activity-based travel model, known as ABM3. The biggest change from SANDAG’s previous ABM, ABM2+, is the transition from the Java-based CT-RAMP modeling platform to the open-source Python-based [ActivitySim](https://research.ampo.org/activitysim/) platform that has been developed by a consortium of public agencies (of which SANDAG is a founding member) for the past decade. ABM3 will be the first ActivitySim model to be used in production for planning purposes. While most of the model components from ABM3 were translated from ABM2+, there were several notable enhancements that were made, which are described below. Further, several models were either re-estimated and/or recalibrated to match 2022 data.
### ActivitySim Version
- [SANDAG/activitysim v15.0.0](https://github.com/SANDAG/activitysim/releases/tag/v15.0.0): This is based on a fork of the ActivitySim consortium repo and customized code in [BayDAG_estimation](https://github.com/SANDAG/activitysim/tree/BayDAG_estimation) branch.
- [SANDAG/activitysim-cvm v15.0.0](https://github.com/SANDAG/activitysim-cvm/releases/tag/v15.0.0): This specific version of ActivitySim is required to run the new Commercial Vehicle Model (CVM) in ABM3. It is based on a fork of the ActivitySim repo under [CamSys](https://github.com/camsys/activitysim) and customized code in [time-settings](https://github.com/SANDAG/activitysim-cvm/tree/time-settings) branch.
### Features
- [ABM PR 68](https://github.com/SANDAG/ABM/pull/68): The base year was updated from 2016 to 2022, allowing the behavioral models to reflect changes in travel behavior derived from the onset of the COVID-19 pandemic.
- [ABM PR 65](https://github.com/SANDAG/ABM/pull/65): The zone system was transitioned from a 3-zone system (TAZ, MAZ, TAP) to a 2-zone system (TAZ, MAZ).
- [ABM PR 66](https://github.com/SANDAG/ABM/pull/66): E-bike and E-scooter were added as travel modes.
- [ABM PR 64](https://github.com/SANDAG/ABM/pull/64): A new commercial vehicle model (CVM) was implemented using ActivitySim, that includes firm synthesis, commercial vehicle tours, and TNC services such as food delivery.
- [ABM PR 133](https://github.com/SANDAG/ABM/pull/133): Network editing was changed from the ArcInfo-based TCOVED to TNED, a new network editing system.
- [ABM PR 104](https://github.com/SANDAG/ABM/pull/104): Flexible fleet services were added for full trips along with first-mile access to and last-mile egress from fixed-route transit.
- [ABM PR 9](https://github.com/SANDAG/ABM/pull/9): The airport model has been updated with new access options, including the proposed automated people-mover.
- [ABM PR 53](https://github.com/SANDAG/ABM/pull/53): The cross-border model was updated, including a new wait time model.
- [Asim PR 635](https://github.com/ActivitySim/activitysim/pull/635): A new disaggregate accessibility calculation step was introduced to improve the precision of accessibilities used by modeling components.
- [ABM PR 82](https://github.com/SANDAG/ABM/pull/82): Transit assignment for different time periods is now run in parallel.
- [ABM Configs](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/resident/settings_mp.yaml#L32): The auto ownership model has been moved before work and school location.
- [Asim PR 613](https://github.com/ActivitySim/activitysim/pull/613): The shadow pricing algorithm was updated to constrain mandatory tour locations to availability instead of traditional shadow pricing.
- [ABM PR 86](https://github.com/SANDAG/ABM/pull/86): Model results are automatically written to a database stored in the cloud via Microsoft Azure Services.
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
