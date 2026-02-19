# Active Transport Bike Model

This directory contains a Python-based implementation of a bike route 
choice model first developed in Java. The bike route choice model is a 
multinomial logit discrete choice model that estimates the probabilities
of an individual’s choosing among multiple alternative routes between a given
origin and destination. This discrete choice model forms the basis of both 
the estimation of the level of service between OD pairs used by the demand 
models and the estimation of the number of cyclists assigned to network links. 
The level of service is the expected maximum utility, or logsum, from the model,
and network link assignments are made with individual route probabilities.

## Setup and Running
The UV environment used to run AcitivtySim (asim_140) is also used to run the bike model.
The bike model is launched from the `bike_route_choice.py` file and accepts a
single (optional) command-line argument specifying the settings YAML file to
read. The default value for this argument is `bike_route_choice_settings.yaml`.

### Settings
In the specified settings YAML file, several options are available for configuration:

**Network**
- `data_dir`: path to the directory in which the model inputs are stored
- `node_file`: name of the input shapefile containing bike network nodes
- `link_file`: name of the input shapefile containing bike network links
- `zone_level`: zone level on which path choice should be performed. Allowed values: `taz`,`mgra`
- `zone_subset`: subset of zones to use for model testing

**Utilities**
- `edge_util_file`: name of the file containing the utility function variable specification for link costs
- `traversal_util_file`: name of the file containing the utility function variable specification for edtraversalge costs
- `random_scale_coef`: scaling coefficient to use for alternative utilities
- `random_scale_link`: scaling coefficient to use for randomized edge costs

**Model Hyperparameters**
- `number_of_iterations`: maximum number of paths which should be found before terminating
- `number_of_batches`: number of batches into which origin centroids should be divided for sequential processing
- `number_of_processors`: number of processors to use for processing each batch
- `max_dijkstra_utility`: cutoff threshold utility (positive) for early termination of the shortest-paths search
- `min_iterations`: the minimum number of paths found - zone pairs with fewer paths will be discarded

**Output and Caching**
- `output_path`: path to the directory in which model outputs should be written
- `output_file_path`: path to the final bike logsum file (bikeMgraLogsum.csv, bikeTazLogsum.csv)
- `save_bike_net`: whether to write the derived network to a set of CSVs
- `read_cached_bike_net`: whether to read the derived network from a cache instead of re-deriving from the original network
- `bike_speed`: the speed in mph to use for calculating bike travel times in output

**Tracing**
- `trace_bike_utilities`: whether to output the chosen path sets from a specified list of origin-destination pairs
- `trace_origins`: ordered list of origins whose paths will be output in tracing
- `trace_destinations`: ordered list of destinations corresponding to the origins for tracing
- `generate_shapefile`: whether to output the trace paths as a shapefile in addition to CSV
- `crs`: network coordinate reference system to attach to the output shapefiles

## Utility Calculation
The variable specifications used in the utility function for edges and traversals
are stored in `bike_edge_utils.csv` and `bike_traversal_utils.csv`, respectively,
including the expressions and their associated coefficients. The utility accounts 
for the distance on different types of facilities, the gain in elevation, turns, 
signal delay, and navigating un-signalized intersections with high-volume facilities. 
To account for the correlation in the random utilities of overlapping routes, the
utility function in the model includes a “path size” measure, described in the 
following section.

## Path Size
The size of path alternative $n_i$ in alternative set $\textbf{n}$ is calculated using the
formula:
```math
size(n_i)=\sum_{l\in n_i}\frac{edgeLength(l)}{pathLength(n_i)*pathsUsing(\textbf{n},l)}
```
where $pathsUsing$ is the integer number of paths in the alternative set $\textbf{n}$
which contain link $l$. Its use derives from the theory of aggregate and elemental 
alternatives, where a link is an aggregation of all paths that use the link. If multiple
routes overlap, their "size" is less than one. If two routes overlap completely, their
size will be one-half.

## Path Sampling
Given an origin, paths are sampled to all relevant destinations in the network by repeatedly
applying Dijstra’s algorithm to search for the paths that minimize a stochastic link
generalized cost with a mean given by the additive inverse of the path utility. For each path
sampling iteration, a random coefficient vector is first sampled from a non-negative
multivariate uniform distribution with zero covariance and mean equal to the link
generalized cost coefficients corresponding to the path choice utility function. As the path
search extends outward from the origin, the random cost coefficients do not vary over links,
but only over iterations of path sampling. In the path search, the cost of each subsequent
link is calculated by summing the product of the random coefficients with their respective
link attributes, and then multiplying the result by a non-negative discrete random edge cost
multiplier.

## Threshold Utility and Distance
To improve runtime, the Dijkstra's algorithm implementation allows for early termination once a
predetermined utility threshold has been reached in its search, with no paths found for zone pairs
whose utility is beyond the threshold. However, the utility of paths is not necessarily the most
user-friendly metric - it is reasonable to aim to define the cutoff by distance rather than utility.
However, because the underlying implementation of Dijkstra's algorithm is only aware of utilities,
not distances, this is not directly feasible, as utility and distance do not correspond directly.

To address this, the `bike_threshold_calculator` script has been built to provide a handy mechanism
to search for a utility threshold which approximates a desired distance cutoff. Given a valid 
configuration YAML file (as described above), the script iteratively performs a binary search,
using the YAML file's `max_dijkstra_utility` setting as a starting threshold utility and modifying
its value on subsequent iterations until the target distance is within the allowed margin of error
(or the maximum number of bike model iterations has been completed). The calling signature of the
script is shown below and requires a minimum of two command line arguments: the settings filepath
and the target distance – the remainder of the parameters are optional, with each optional argument 
requiring those listed before it in sequence:

~~~
Usage:
    python bike_threshold_calculator.py <settings filepath> <target distance> [target_margin [percentile [max_iterations]]]

    parameters:
        settings filepath:  path to YAML file containing bike model settings
        target distance:    the distance for which the search should aim (in miles)
        target margin:      the margin of error (< 1) allowed before termination (optional, default: 0.1)
        percentile:         the percentile of distance to compare against the target (optional, default: 0.99)
        max iterations:     the most bike model iterations that can be performed in the search (optional, default: 20)

    examples:
        
        python bike_threshold_calculator.py bike_route_choice_settings_taz.yaml 20 
        # the resulting 99th %ile distance must be w/in 10% of the 20-mile target distance
        # equivalent to:
            python bike_threshold_calculator.py bike_route_choice_settings_taz.yaml 20 0.1 0.99 20
        
        python bike_threshold_calculator.py bike_route_choice_settings_mgra.yaml 3 0.05
        # the resulting 99th %ile distance must be w/in 5% of the three-mile target distance
~~~

With the default parameters used, the script will search until either 20 iterations have elapsed or
the 99th percentile of distances found is within 10% of the specified target distance. At termination, 
a CSV named `threshold_results.csv` will be written to the output directory (set in the `output_path` 
setting in the YAML file) with the input utility thresholds, the iteration runtime, and the chosen 
percentile of distance. These will also be scatter plotted on a graph and displayed, but note that 
this graph is not saved automatically to the output directory.