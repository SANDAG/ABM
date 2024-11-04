# Transit Skimming and Assignment

The transit assignment uses a headway-based approach, where the average headway between vehicle arrivals for each transit line is known, but not exact schedules. Passengers and vehicles arrive at stops randomly and passengers choose their travel itineraries considering the expected average waiting time.

The Emme Extended transit assignment is based on the concept of optimal strategy but extended to support a number of behavioral variants. The optimal strategy is a set of rules which define sequence(s) of walking links, boarding and alighting stops which produces the minimum expected travel time (generalized cost) to a destination. At each boarding point the strategy may include multiple possible attractive transit lines with different itineraries. A transit strategy will often be a tree of options, not just a single path. A line is considered attractive if it reduces the total expected travel time by its inclusion. The demand is assigned to the attractive lines in proportion to their relative frequencies.

The shortest "travel time" is a generalized cost formulation, including perception factors (or weights) on the different travel time components, along with fares, and other costs / perception biases such as transfer penalties which vary over the network and transit journey.

The model has three access modes to transit (walk, park-and-ride (PNR), and kiss-and-ride (KNR)) and three transit sets (local bus only, premium transit only, and local bus and premium transit sets), for 9 total demand classes by 5 TOD. These classes are assigned by slices, one at a time, to produce the total transit passenger flows on the network.

While there are 9 slices of demand, there are only three classes of skims: Local bus only, premium only, and all modes. The access mode does not change the assignment parameters or skims.