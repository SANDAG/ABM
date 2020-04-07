ABM
===

How to Build the Sandag ABM Java Programs

  - Download Eclipse
  - Install Maven (m2e) if needed (check if it is already installed via Windows + Preferences)
  - File + Import + Maven + Existing Maven Projects and select pom.xml
  - Eclipse will import the SANDAG ABM maven project, which includes downloading the dependencies from SANDAG's svn server
  - Right click pom.xml and select Run As + Maven with the goal `clean package` and check "skip tests"
  - The outputs are here: target\SANDAG CT-RAMP Activity Based Model

Dependencies are download to Windows + Preferences + Maven + User Settings + Local Repository.  Make sure you have access to SANDAG's SVN and Nexus servers (via VPN with the Nexus port open).


