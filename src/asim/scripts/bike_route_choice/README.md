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
A list of necessary dependencies is stored in `environment_bike_test.yaml` which
can be used to create an Anaconda environment named `bike_test` as follows:
```
conda env create --file environment_bike_test.yml
```
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
- `max_dijkstra_distance`: "cutoff threshold"* distance for early termination of the shortest-paths search

*Note that the threshold distance is not strictly observed as a cutoff, as it
must be converted to a generalized cost using the mean cost per unit distance,
which may lead to over-threshold paths being accepted due to variations in cost
sensitivity.

**Output and Caching**
- `output_path`: path to the directory in which model outputs should be written
- `save_bike_net`: whether to write the derived network to a set of CSVs
- `read_cached_bike_net`: whether to read the derived network from a cache instead of re-deriving from the original network

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
multiplier . Path sampling is repeated until both a minimum count of paths and a preset
target for the total of all path sizes in each alternative list is reached. If the total path size
does not reach its target after a given maximum number of sampling iterations, sampling
terminates to prevent excessively long computation time.