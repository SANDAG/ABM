# Release Notes
## Version 15.2.2 (April X, 2025)
A few new features were added and minor bugs were fixed. Version 15.2.2 was used for the draft EIR.

### Features
- [PR 291](https://github.com/SANDAG/ABM/pull/291) & [PR 293](https://github.com/SANDAG/ABM/pull/293): Adding work from home calibration coefficient to parametersByYears.csv
- [PR 287](https://github.com/SANDAG/ABM/pull/287): Refactoring of [2zoneSkim.py](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/scripts/resident/2zoneSkim.py) script
- [PR 286](https://github.com/SANDAG/ABM/pull/286), [PR 288](https://github.com/SANDAG/ABM/pull/288), [PR 290](https://github.com/SANDAG/ABM/pull/290), [PR 292](https://github.com/SANDAG/ABM/pull/292), [PR 295](https://github.com/SANDAG/ABM/pull/295), [PR 296](https://github.com/SANDAG/ABM/pull/296), [PR 299](https://github.com/SANDAG/ABM/pull/299), [PR 302](https://github.com/SANDAG/ABM/pull/302), & [PR 303](https://github.com/SANDAG/ABM/pull/303): Documentation updates

### Bug Fixes
- [PR 289](https://github.com/SANDAG/ABM/pull/289) & [PR 297](https://github.com/SANDAG/ABM/pull/297): Removed unused files and settings

## Version 15.2.1 (December 20, 2024)
Multiple bugs were fixed and some recalibration was done since the release of version 15.2.0. Version 15.2.1 was used for the draft 2025 RP modeling.

### Features
- [PR 249](https://github.com/SANDAG/ABM/pull/249): Addition of calibration scripts
- [PR 251](https://github.com/SANDAG/ABM/pull/251), [PR 258](https://github.com/SANDAG/ABM/pull/258), [PR 259](https://github.com/SANDAG/ABM/pull/259), & [PR 262](https://github.com/SANDAG/ABM/pull/262): Calibration updates (atwork stop location, work from home, bike shares, work location) including additional mode choice coefficients for ebike owners
- [PR 256](https://github.com/SANDAG/ABM/pull/256): Increase travel time reporter threshold from 30 to 45 minutes
- [PR 257](https://github.com/SANDAG/ABM/pull/257): Adds option to skip travel time reporter (was previously included in data exporter)
- [PR 266](https://github.com/SANDAG/ABM/pull/266): 2032 and 2040 added as options when selecting a scenario year in the scenario creation GUI
- [PR 268](https://github.com/SANDAG/ABM/pull/268): Assume owned ebike in travel time reporter
- [PR 271](https://github.com/SANDAG/ABM/pull/271): Recalibrate bike mode share in response to AT network updates 
- [PR 252](https://github.com/SANDAG/ABM/pull/252), [PR 254](https://github.com/SANDAG/ABM/pull/254), [PR 260](https://github.com/SANDAG/ABM/pull/260), [PR 265](https://github.com/SANDAG/ABM/pull/265), [PR 280](https://github.com/SANDAG/ABM/pull/280), & [PR 284](https://github.com/SANDAG/ABM/pull/284): Documentation updates

### Bug Fixes
- [PR 253](https://github.com/SANDAG/ABM/pull/253): Removal of bike logsum copy from Emme GUI
- [PR 255](https://github.com/SANDAG/ABM/pull/255): Removal of input checker step
- [PR 263](https://github.com/SANDAG/ABM/pull/263): Update bike lane AT network field
- [PR 274](https://github.com/SANDAG/ABM/pull/274), [PR 276](https://github.com/SANDAG/ABM/pull/276), & [PR 278](https://github.com/SANDAG/ABM/pull/278): MAAS model fixes including crash on failure and force escort participants column to be read in as string

## Version 15.2.0 (November 14, 2024)
Since release 15.1.0, several bug fixes and new features were added. Results of ABM3 using Version 15.2 are anticipated to be presented to the SANDAG Board of Directors in Spring 2025.

### ActivitySim Version
No changes made to ActivitySim version.

### Features
- [PR 169](https://github.com/SANDAG/ABM/pull/169): Set TNC to transit modes to use KNR to transit skims and removed TNC to transit skims
- [PR 203](https://github.com/SANDAG/ABM/pull/203): Added tracking of disk space usage
- [PR 204](https://github.com/SANDAG/ABM/pull/204), [PR 219](https://github.com/SANDAG/ABM/pull/219), & [PR 232](https://github.com/SANDAG/ABM/pull/232): Flexible fleets improvement and calibration
- [PR 206](https://github.com/SANDAG/ABM/pull/206): Reduced sensitivity of electric vehicle ownership to number of chargers
- [PR 210](https://github.com/SANDAG/ABM/pull/210): Added input network and land use paths to scenario table in Datalake
- [PR 222](https://github.com/SANDAG/ABM/pull/222): Updated version number in the property file
- [PR 223](https://github.com/SANDAG/ABM/pull/223): Updated validation to work with networks created from TNED
- [PR 226](https://github.com/SANDAG/ABM/pull/226): Included FFC attribute in Emme network for reporting
- [PR 239](https://github.com/SANDAG/ABM/pull/239): Added option to skip validation in master run
- [PR 240](https://github.com/SANDAG/ABM/pull/240), [PR 242](https://github.com/SANDAG/ABM/pull/242), & [PR 243](https://github.com/SANDAG/ABM/pull/243): Fixes to MAZ to transit stop distances and walk times
- [PR 241](https://github.com/SANDAG/ABM/pull/241): Fixes to bike logsums and micromobility mode choice
- [PR 215](https://github.com/SANDAG/ABM/pull/215), [PR 235](https://github.com/SANDAG/ABM/pull/235),  [PR 237](https://github.com/SANDAG/ABM/pull/237), [PR 238](https://github.com/SANDAG/ABM/pull/238), [PR 244](https://github.com/SANDAG/ABM/pull/244), & [PR 246](https://github.com/SANDAG/ABM/pull/246): Documentation updates

### Bug Fixes
- [PR 212](https://github.com/SANDAG/ABM/pull/212), [PR 213](https://github.com/SANDAG/ABM/pull/213), [PR 217](https://github.com/SANDAG/ABM/pull/217), & [PR 245](https://github.com/SANDAG/ABM/pull/245): File and configuration cleanup
- [PR 214](https://github.com/SANDAG/ABM/pull/214), [PR 216](https://github.com/SANDAG/ABM/pull/216), [PR 220](https://github.com/SANDAG/ABM/pull/220), [PR 221](https://github.com/SANDAG/ABM/pull/221), [PR 225](https://github.com/SANDAG/ABM/pull/225), [PR 228](https://github.com/SANDAG/ABM/pull/228), [PR 230](https://github.com/SANDAG/ABM/pull/230), [PR 231](https://github.com/SANDAG/ABM/pull/231), & [PR 233](https://github.com/SANDAG/ABM/pull/233): Various fixes to Travel Time Reporter
- [PR 218](https://github.com/SANDAG/ABM/pull/218): Allowed for traffic assignment to work without any tolled links
- [PR 224](https://github.com/SANDAG/ABM/pull/224): Removed Java-based walk logsum step
- [PR 227](https://github.com/SANDAG/ABM/pull/227): Fixed issue with link transit travel times
- [PR 229](https://github.com/SANDAG/ABM/pull/229): Fixed Java unicode error with filepaths containing folders starting with the letter U
- [PR 234](https://github.com/SANDAG/ABM/pull/234): Corrected mixed party type to correct value
- [PR 242](https://github.com/SANDAG/ABM/pull/242) & [PR 243](https://github.com/SANDAG/ABM/pull/243): Fix issues with MAZ to stop distances

## Version 15.1.0 (September 4, 2024)
As mentioned in the notes for Version 15.0.2, several improvements to the Commercial Vehicle Model (CVM) were made, largely due to SANDAG staff realizing that the survey used to estimate the CVM had likely overestimated the amount of commercial vehicle travel that was made on a given day in the region. New weights were estimated, and then the CVM was recalibrated to match these new weights. After doing this, it was found that modeled highway volumes were lower than observed counts, so some further adjustments were made to get them back up. Some components of the resident model were recalibrated to better match the survey, a new database started being used, a bug in the transit network was fixed, and other miscelaneous improvements were made.

### ActivitySim Version
No changes made to ActivitySim version.

### Features
- [PR 167](https://github.com/SANDAG/ABM/pull/167), [PR 172](https://github.com/SANDAG/ABM/pull/172), & [PR 173](https://github.com/SANDAG/ABM/pull/173): Recalibration of the Commercial Vehicle Model to reflect newly-calculated Commercial Vehicle Survey weights.
- [PR 182](https://github.com/SANDAG/ABM/pull/182), [PR 186](https://github.com/SANDAG/ABM/pull/186), [PR 194](https://github.com/SANDAG/ABM/pull/194), & [PR 195](https://github.com/SANDAG/ABM/pull/195): Miscellaneous calibration, particularly to counter the decrease in freeway travel caused by the CVM update.
- [PR 176](https://github.com/SANDAG/ABM/pull/176) & [PR 177](https://github.com/SANDAG/ABM/pull/177): A new database was created for Version 15.1.0 and necessary scripts were modified to point to the new database. Go [here](https://dev.azure.com/SANDAG-ADS/ORTAD/_wiki/wikis/ORTAD.wiki/3/Comparison-abm_15_1_0-and-abm_15_0_0) to see detailed changes in the database and [here](https://dev.azure.com/SANDAG-ADS/ORTAD/_wiki/wikis/ORTAD.wiki/2/Release-Notes-for-abm_15_1_0-and-abm_15_1_0_reporting) for changes in the reporting tool.
- [PR 184](https://github.com/SANDAG/ABM/pull/184), [PR 186](https://github.com/SANDAG/ABM/pull/186), & [PR 193](https://github.com/SANDAG/ABM/pull/193): The number of EV chargers each year were updated using numbers given by planning, and recalibration was done to ensure that the EV ownership rates for no build scenarios matched the targets.
- [PR 178](https://github.com/SANDAG/ABM/pull/178), [PR 188](https://github.com/SANDAG/ABM/pull/188), & [PR 189](https://github.com/SANDAG/ABM/pull/189): In previous releases the crossborder model had its own constants.yaml file, which contained a lot of the same values in the common constants.yaml file, meaning that both files would need to be edited. The crossborder model now instead reads from the common constants.yaml file.
- [PR 179](https://github.com/SANDAG/ABM/pull/179), [PR 181](https://github.com/SANDAG/ABM/pull/181), [PR 190](https://github.com/SANDAG/ABM/pull/190), [PR 191](https://github.com/SANDAG/ABM/pull/191), & [PR 192](https://github.com/SANDAG/ABM/pull/192): Miscellaneous updates to the scenario creation GUI and scenario build process.

### Bug Fixes
- [PR 168](https://github.com/SANDAG/ABM/pull/168), [PR 170](https://github.com/SANDAG/ABM/pull/170), [PR 171](https://github.com/SANDAG/ABM/pull/171), [PR 175](https://github.com/SANDAG/ABM/pull/175), & [PR 185](https://github.com/SANDAG/ABM/pull/185): A bug in the transit network where connectors were being encoded with the wrong units was fixed. This resulted in a sharp increase in the number of transit boardings so some additional calibration was needed to get them back down to observed levels.
- [PR 174](https://github.com/SANDAG/ABM/pull/174) & [PR 180](https://github.com/SANDAG/ABM/pull/180): Miscellaneous file cleanup and restoration.


## Version 15.0.2 (July 3, 2024)
Since the release of version 15.0.1 more updates were made, particularly regarding the new commercial vehicle model. More updates to the CVM will be forthcoming due to ongoing revision based on new data sources.

### ActivitySim Version
No changes made to ActivitySim version.

### Features
- [PR 155](https://github.com/SANDAG/ABM/pull/155) & [PR 159](https://github.com/SANDAG/ABM/pull/159): Updated the Wiki to reflect changes made for ABM3.
- [PR 156](https://github.com/SANDAG/ABM/pull/156): Made changes to distribution of commercial vehicle type mapping to the categories used in assignment.
- [PR 157](https://github.com/SANDAG/ABM/pull/157): Added calibration factors for determining the probability of a tour made by a single- or multi-unit truck terminating at a given stop.
- [PR 158](https://github.com/SANDAG/ABM/pull/158): Changed firm synthesis to be done at the TAZ level instead of the MGRA level.

### Bug Fixes
- [PR 160](https://github.com/SANDAG/ABM/pull/160): Removed an input file no longer used by the updated heavy truck model.


## Version 15.0.1 (June 12, 2024)
During the initial testing of the 2025 Regional Plan initial concept, some critical model issues were identified and fixed.

### ActivitySim Version
No changes made to ActivitySim version.

### Features
- [PR 152](https://github.com/SANDAG/ABM/pull/152): TNC_SHARED was not being properly disabled in 2022 due to misplaced parentheses.
- [PR 148](https://github.com/SANDAG/ABM/pull/148): Fixed TNC_SHARED mode choice to restrict to ride hail tours and help shares on joint tours.
- [Issue 143](https://github.com/SANDAG/ABM/issues/143): Added parameter to discount wait times for rapid bus.

### Bug Fixes
- [PR 150](https://github.com/SANDAG/ABM/pull/150): Updated EI inputs at interstate cordons to reflect 2022 counts.
- [PR 149](https://github.com/SANDAG/ABM/pull/149): Updated MAZ ID for Otay Mesa East Point of Entry from Series 13 to Series 15.
- [PR 147](https://github.com/SANDAG/ABM/pull/147): Updated MGRA references for external cordons in airport models from Series 13 to Series 15.
- [PR 146](https://github.com/SANDAG/ABM/pull/146): Removed suffix from labels in runtime summary.
- [PR 145](https://github.com/SANDAG/ABM/pull/145): Added parking location placeholder datasets needed to open calibration visualizer.
- [Commit a825ae3](https://github.com/SANDAG/ABM/commit/a825ae36e81f60c99ca36aad382bdc78388029c9): Updated CBX inputs to use number of enplanements.
- [Commit 305cdcf](https://github.com/SANDAG/ABM/commit/305cdcf8db2c22926fd93a0f933a9a05b32b759d): Updated reporting of airport transit costs to be in dollars instead of cents.
- [Commit 363d05a](https://github.com/SANDAG/ABM/commit/363d05a66f652c757e7ce423a4d7b221f81c21f9): Removed addition of "poverty" field when writing output tables as that had been added to reading of input table.
- [Commit 34e4a1d](https://github.com/SANDAG/ABM/commit/34e4a1dff48b69eb474db7cb75c1f971f3afa9dd): Set year after which vehicles must have transponders from 2035 to 2029.
- [Commit dc07cc9](https://github.com/SANDAG/ABM/commit/dc07cc957c6e10776cca4cea53c5a45d7e2cf9cd): Increased default flexible fleet diversion constants.
- [Commit 726f98a](https://github.com/SANDAG/ABM/commit/726f98a4872dce8a79cd6eee8b079f63f78ed797): Updated some future year signal factors and truck toll factors on SR-11.

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
