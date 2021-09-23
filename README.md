## SANDAG ABM

SANDAG maintains multiple ABM software versions, including ABM1 for <a href="https://www.sdforward.com/2019-federal-rtp/2015-regional-plan">San Diego Forward: The Regional Plan</a> adopted by SANDAG Board of Directors in 10/2015, ABM2 for <a href="https://www.sdforward.com/2019-federal-rtp">the SANDAG 2019 Federal Regional Transportation Plan</a> adopted in 10/2019, and ABM2+ for ongoing applications in <a href="https://www.sdforward.com/about-san-diego-forward/developing-the-2021-regional-plan">the San Diego Forward:  The 2021 Regional Plan (2021 Regional Plan).</a> SANDAG is currently working on developing ABM3, a <a href="https://github.com/ActivitySim/activitysim/wiki">ActivitySim</a> based modeling platform, for the 2025 Regional Plan.  Please refer to this <a href="https://github.com/SANDAG/ABM/wiki">WIKI page</a> for SANDAG's ABM design, data dictionaries, calibration and valiation, and sensitivity testing reports. 

As an <a href="https://github.com/ActivitySim/activitysim/wiki">ActivitySim</a> member, SANDAG worked with the consortium and developed three ActivitySim prototypes <a href="https://github.com/ActivitySim/activitysim/tree/master/activitysim/examples/example_sandag">(1-zone, 2-zone, and 3-zone</a>) using SANDAG's data.  Additionally, a San Diego specific special market model-<a href="https://github.com/SANDAG/ABM/wiki/CB-Project-Page">Crossborder Model (CBM) software</a> has been converted from CT-RAMP to ActivitySim.

SANDAG’s ABM source-control follows a <a href="https://trunkbaseddevelopment.com/">trunk-based model (TBD) model:</a>

- Developers collaborate on code in a single branch called ‘trunk’:
  - ABM1_TRUNK for ABM1
  - ABM2_TRUNK for ABM2 & ABM2+
  - asim-cross-border for CBM: <a href="https://github.com/ActivitySim/activitysim/wiki">ActivitySim</a>-based
- For each ABM version there is a release branch that should not receive continued development work.
  - Release 13.3.3 for ABM1
  - Release 14.1.1 for ABM2
  - Release 14.2.2 for ABM2+
- In some cases, short-lived feature branches are allowed. However, once codes on feature branch compiles and passes all tests, they should be merged to trunk and the feature branch should be deleted.

##

## How to Build the SANDAG ABM Java Programs, <a href="https://github.com/sandag/abm/wiki/Build-SANDAG-Jar">click here for details</a>

  - Download Eclipse
  - Install Maven (m2e) if needed (check if it is already installed via Windows + Preferences)
  - File + Import + Maven + Existing Maven Projects and select pom.xml
  - Eclipse will import the SANDAG ABM maven project, which includes downloading the dependencies from SANDAG's svn server
  - Right click pom.xml and select Run As + Maven with the goal `clean package` and profile name and then check "skip tests"
  - The outputs are here: target\SANDAG CT-RAMP Activity Based Model

Dependencies are download to Windows + Preferences + Maven + User Settings + Local Repository.  Make sure you have access to SANDAG's SVN and Nexus servers (via VPN with the Nexus port open).


