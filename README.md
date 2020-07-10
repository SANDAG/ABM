## SANDAG ABM

SANDAG maintains multiple versions of ABM software, including ABM1 for 2015 RP, ABM2 for 2019 Federal RTP, and ABM2+ for 2021 RP.  SANDAG’s ABM source-control follows a <a href="https://trunkbaseddevelopment.com/">trunk-based model (TBD) model:</a>

- Developers collaborate on code in a single branch called ‘trunk’:
  - ABM1_TRUNK for ABM1
  - ABM2_TRUNK for ABM2
  - asim-cross-border for cross border travel model (CBTM) and eventually ABM3.
- For each ABM version there is a release branch that should not receive continued development work.
  - Release 13.3.2 for ABM1
  - Release 14.1.1 for ABM2
  - Release 14.2.0 for ABM2+ (forthcoming)
- In some cases, short-lived feature branches are allowed. However, once codes on feature branch compiles and passes all tests, they should be merged to trunk and the feature branch should be deleted.

===

How to Build the Sandag ABM Java Programs, <a href="https://github.com/sandag/abm/wiki/Build-SANDAG-Jar">click here for detailed build steps</a>

  - Download Eclipse
  - Install Maven (m2e) if needed (check if it is already installed via Windows + Preferences)
  - File + Import + Maven + Existing Maven Projects and select pom.xml
  - Eclipse will import the SANDAG ABM maven project, which includes downloading the dependencies from SANDAG's svn server
  - Right click pom.xml and select Run As + Maven with the goal `clean package` and profile name and then check "skip tests"
  - The outputs are here: target\SANDAG CT-RAMP Activity Based Model

Dependencies are download to Windows + Preferences + Maven + User Settings + Local Repository.  Make sure you have access to SANDAG's SVN and Nexus servers (via VPN with the Nexus port open).


