# Bike Logsums

## Usage

This model generates bike logsums and times for MGRA and TAZ pairs. It is designed to reflect the impact of bike infrastructure on path and mode choice for bicycle trips. It identifies likely routes along the all-streets network, considering factors such as presence and type of bike lane, turns, and gain in elevation. Note that the Bike mode uses bike logsums, while Ebike and Escooter use bike times.

This process generates the output\bikeMgraLogsum.csv and output\bikeTazLogsum.csv files. Bike logsums only need to be run when the active transportation (AT) network is updated. If logsums do not need to be updated, use the "Skip bike logsums" setting to automatically copy from input\bikeMgraLogsum.csv and input\bikeTazLogsum.csv, sourced from T:\ABM\release\ABM\version_X_X_X\input\20XX. This directory is updated manually.

## Design

<!-- ![](../../images/design/bike_logsum_design.png) -->
<img src="../../images/design/bike_logsum_design.png" width="200"/>

**Inputs:** The AT network is read from input\SANDAG_Bike_Net.dbf and input\SANDAG_Bike_Node.dbf. The bike model settings and utility files are found in src\asim\scripts\bike_route_choice.

**Network Construction:** Node and edge attributes are processed from the network files. Traversals are derived for edge pairs, using angle to determine turn type.

**Path Sampling:**

- Doubly stochastic, coefficients are randomized and resulting edge cost is randomized
- Dijkstra's algorithm finds path each origin to destination with minimum combined edge and traversal cost
- Add paths to path alternatives list and calculate path sizes from overlap of alternatives
- Repeat for preset number of iterations (default 10)

**Path Choice Utility Calculation:** Calculate bike logsum values for each origin/destination pair from utility expression on each path alternative.

**Outputs:** Bike logsums and times are written to output\bikeMgraLogsum.csv and output\bikeTazLogsum.csv. Log and trace files are written to output\bike. During ActivitySim preprocessing, TAZ values are added to BIKE_LOGSUM and BIKE_TIME matrices of output\skims\traffic_skims_AM.omx and MGRA values are written to output\skims\maz_maz_bike.csv.

## Further reading

[Bike Route Choice README](../../../src/asim/scripts/bike_route_choice/README.md)

[ABM3 Bike Model Report (2025)](../../pdf_reports/SANDAG_ABM3_Bike_Model_Report.pdf)

[Active Transportation Improvements Report (2015)](https://github.com/SANDAG/ABM/wiki/files/at.pdf) (Prior bike logsum implementation in Java)