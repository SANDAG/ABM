# Network Import from TNED

This section describes the procedure by which the ABM3 model system imports (into Emme) network (highway and transit) files along with a general description of the different network files.

## Network Files

The ABM3 model system has been configured to be compatible with SANDAG's Transportation Network Editing Database (TNED) system, which is utilized to edit, maintain and generate transportation networks. The TNED network files, generated via an ETL (i.e., Extract, Tranform, Load) procedure, serve as inputs to the ABM3 model system's import network procedure and are produced in text file, shapefile, geodatabase table and geodatabase feature class geodatabase formats. There are, additionally, some non-TNED input network files which are manually maintained.

The following are the required network files used during the Emme import network procedure:

| **File**                       | **Source**          | **Description**                                                                              |
|--------------------------------|---------------------|----------------------------------------------------------------------------------------------|
| EMMEOutputs.gdb/TNED_HwyNet    | TNED                | Roadway network links                                                                        |
| EMMEOutputs.gdb/TNED_HwyNodes  | TNED                | Roadway network nodes                                                                        |
| EMMEOutputs.gdb/TNED_RailNet   | TNED                | Rail network links                                                                           |
| EMMEOutputs.gdb/TNED_RailNodes | TNED                | Rail network nodes                                                                           |
| EMMEOutputs.gdb/Turns          | TNED                | Turn prohibition records                                                                     |
| special_fares.txt              | Manually Maintained | Special fares in terms of boarding and incremental in-vehicle costs                          |
| timexfer_{time_of_day}.csv     | Manually Maintained | Timed transfer pairs of lines, by period. Where time_of_day refers to EA, AM, MD, PM, or EV. |
| trrt.csv                       | TNED                | Attribute data (modes, headways) for the transit lines                                       |
| trlink.csv                     | TNED                | Sequence of links (routing) for the transit lines                                            |
| trstop.csv                     | TNED                | Stop data for the transit lines                                                              |
| MODE5TOD.csv                   | Manually Maintained | Global (per-mode) transit cost and perception attributes                                     |
| vehicle_class_toll_factors.csv | Manually Maintained | Factors to adjust the toll cost by facility name and class                                   |

## Import Network Procedure

This section describes the main steps carried out during the Emme import network procedure. The entire process is executed by the [import_network.py](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/main/emme/toolbox/import/import_network.py) script.

#### Create Modes

This step creates the different combinations of traffic and transit modes that will get applied to the network links. A mode defines a group of vehicles or users which have access to the same parts of the network. Modes are used in both the traffic and transit assignments to define the available network for each class of demand. Each mode is uniquely identified by a single case-sensitive character. The modes which have access to a given link are listed on that link, and each link must allow at least one mode.

#### Create Roadway Base Network

This step creates the base roadway network by importing it from the EMMEOutputs.gdb/TNED_HwyNet and EMMEOutputs.gdb/TNED_HwyNodes. The nodes and links (referred to as the base network in Emme) for the traffic network are imported from the TNED_HwyNode and TNED_HwyNet geodatabase feature classes. The nodes are created first and the links connect between them. The I-node (from node, field AN) and J-node (to node, field BN) are used to associate the nodes and links and uniquely identify the link in the Emme network. Separate forward (AB) and reverse (BA) links are generated for links that have been coded as two-way. 

#### Create Turns

This step processes the EMMEOutputs.gdb/Turns input network file to generate turn restrictions by to- and from- link ID. If the indicated link IDs do not make a valid turn (links not adjacent) an error is reported.

#### Calculate Traffic Attributes

This step calculates derived traffic attributes. It utilizes the vehicle_class_toll_factors.csv to adjust toll costs by facility name and class.

#### Check Zone Access

This step verifies that every centroid has at least one available access and egress connector.

#### Create Rail Base Network

This step creates the base roadway network by importing it from the EMMEOutputs.gdb/TNED_RailNet and EMMEOutputs.gdb/TNED_RailNodes. The nodes and links (referred to as the base network in Emme) for the rail network are imported from the TNED_RailNode and TNED_RailNet geodatabase feature classes. The nodes are created first and the links connect between them. The I-node (from node, field AN) and J-node (to node, field BN) are used to associate the nodes and links and uniquely identify the link in the Emme network. Separate forward (AB) and reverse (BA) links are generated for links that have been coded as two-way. 

#### Create Tranist Lines

This step creates the transit lines by importing them from the trrt.csv, trlink.csv and trstop.csv input network files and matched to the transit base network. The mode-level attributes from MODE5TOD.csv, which vary by mode, are copied to transit line attributes and used in  transit assignment. It is in this step also where the timexfer_{time_of_day}.csv files are used to explicitly set route-to-route specific transfer transit times.

#### Calculate Transit Attributes

The transit line and stop / segment attributes (including fares) are imported to Emme attributes. The special_fares.txt lists network-level incremental fares by boarding (line and/or stop) and in-vehicle segment. They specify additive fares based on the network elements encountered on a transit journey and are used to represent the Coaster (or other) zonal fare system.