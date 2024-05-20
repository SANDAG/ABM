# Release Notes

## Version 15.0.0 (May 2024)
For use in the 2025 Region Plan (RP), SANDAG has developed a new activity-based travel model, known as ABM3. The biggest change from SANDAG’s previous ABM, ABM2+, is the transition from the Java-based CT-RAMP modeling platform to the open-source Python-based ActivitySim platform that has been developed by a consortium of public agencies (of which SANDAG is a founding member) for the past decade. ABM3 will be the first ActivitySim model to be used in production for planning purposes. While most of the model components from ABM3 were translated from ABM2+, there were several notable enhancements that were made, which are described below. Further, several models were either re-estimated and/or recalibrated to match 2022 data.
### Features
- The base year was updated from 2016 to 2022, allowing the behavioral models to reflect changes in travel behavior derived from the onset of the COVID-19 pandemic.
- The zone system was transitioned from a 3-zone system (TAZ, MAZ, TAP) to a 2-zone system (TAZ, MAZ)
- E-bike and E-scooter were added as travel modes.
- Flexible fleet services were added for full trips along with first-mile access to and last-mile egress from fixed-route transit.
- The airport model has been updated with new access options, including the proposed automated people-mover.
- A wait time model was added to the cross-border model.
- A new disaggregate accessibility calculation step was introduced to improve the precision of accessibilities used by modeling components.
- The auto ownership model has been moved before work and school location.
- The shadow pricing algorithm was updated to constrain mandatory tour locations to availability instead of traditional shadow pricing.
- Model results are automatically written to a database stored in the cloud via Microsoft Azure Services
- The following modeling components were added to the resident model:
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
### Bugs
