# Commercial Vehicle Model

#### Introduction
The SANDAG Commercial Vehicle Model (CVM) simulates the weekday demand patterns of commercial 
vehicle movements throughout the San Diego region. The CVM is an important part of the complete travel 
demand modeling system for the region, representing a market of travel that dominates the middle part of 
most weekdays and has been steadily growing as consumer demands for home deliveries and personal 
services have increased.

The primary sources of data for developing the CVM were the 2022 SANDAG Commercial Vehicle
Establishment and TNC-driver surveys, which focused on goods, services, and maintenance trips, and 
obtained travel diaries from employees of these establishments whose jobs involve routine travel for either 
goods pickup and delivery or for service provision. The TNC driver survey used an identical travel diary 
format to the Establishment survey, the difference being that individual TNC drivers were surveyed as their 
own establishments who worked on behalf of an online pickup and delivery service. The Establishment and 
TNC surveys provided detailed travel pattern data for individual drivers and vehicles, which formed the basis 
for estimating and calibrating model components. The categorical definitions of attribute variables in the two 
surveys set the possibilities for segmentation of the model system, such as establishment industry sectors;
trip origin and destination purposes, land uses, and place types; and vehicle types.

The geographic scope of the CVM are internal-to-internal trip movements. The market scope of the model 
includes commercial goods movements (pickup and deliveries) as well as trips made for commercial and 
public services. Trips made for other purposes, namely maintenance and personal, are also included in the 
CVM if these trips are made in the context of a commercial vehicle tour pattern. The CVM explicitly 
distinguishes between residential and non-residential customer types, and between three vehicle types—
light, medium, and heavy—consistent with the definitions used in the Establishment Survey.

The CVM does not cover the types of work-related travel that would be expected to be covered in the ABM3
Resident model, namely workers traveling for meetings, sales calls, out-of-town travel, and similar activities.
The CVM also does not model long-distance freight truck movements that enter and exit the region, which 
are covered by the Heavy Truck Model (HTM).

The HTM covers long-distance freight movements into and out of San Diego County. The source of the 
demand in the HTM are commodity flows between shippers and receivers throughout North America, 
focusing on those with either a trip end (shipper or receiver) in San Diego County or which pass through San 
Diego County, for example, between Mexico and Los Angeles. Commodity flows are derived from the 
Federal Highway Administration (FHWA) Freight Analysis Framework version 5 (FAF5). The model design 
assumes that freight truck trips between establishments within San Diego County are covered by the CVM, 
which has been designed to explicitly account for truck movements involving warehouse and distribution 
centers and port facilities.

## Design

#### Design Objectives and Overview
The 2024 CVM design was conceived as an improvement upon the SANDAG CVM created in 2014, which 
used a similar approach for simulating commercial vehicle tours. The new CVM design differs from the 
previous model in several important ways. Improvement objectives of the new design include:

• Better accuracy for representing total commercial vehicle demand, as represented in the new 
surveys, particularly more accurate accounting of vehicle miles traveled (VMT) by vehicle size;

• Leveraging the new survey data to capture contemporary e-commerce trends in direct deliveries 
between warehouse fulfillment centers and consumers;

• Distinguishing between residential and non-residential customer types;

• Creating linkages between the CVM and the Resident model to better represent household demand 
for food and package deliveries and for services;

• Leveraging the TNC driver survey to represent this recently emerged type of on-line establishment 
and the “gig” workers it employs.

In addition, the HTM update to the most current version of FAF5 was important to representing commodity 
trading and supply chain trends after the COVID-19 pandemic. The HTM update also includes improvements 
to process efficiency and accuracy.

#### CVM Overview and Flow Between Components

At its core, the CVM is a dynamic simulation of commercial vehicle travel patterns during a representative 
weekday in the San Diego region. The CVM creates travel patterns composed of trips to stop locations, with 
each stop defined by a purpose, customer type if appropriate, the vehicle type used, geographic location 
(MGRA and TAZ identifications), and arrival and departure times. The model uses the term “route” to 
describe a day’s worth of trips made by the same vehicle and is consistent with the operational notion of 
commercial vehicle route planning and scheduling. A commercial vehicle route is comprised of one or 
multiple stops and may include one or more full or partial tours. The CVM is dynamic in the sense that it 
generates routes as simulation elements with starting conditions, then simulates the stops and trips on the 
route incrementally while considering time of day, elapsed time, and the travel distance to a pre-chosen 
terminal location for the route. 

The new CVM differs from the previous version of the model in that it does not use the concept of complete 
tours, with all tours beginning and ending at establishment, as an organizing principal and constraint. The 
reasons for this are that commercial vehicle diary surveys revealed that for about one-third of vehicle days 
the vehicle did not return to the same place that it started, and the starting or ending locations for vehicle 
days were often not the establishment itself. Therefore, the decision was made to allow for “open jaw” tours 
whereby the simulation does not force a vehicle to begin and end the simulation day at the establishment, 
but rather models possible choices of non-establishment starting and ending locations. Open jaw tours are 
common for businesses that have multiple establishment locations, particularly those with separate
warehouses and vehicle maintenance yards or garages.

The CVM represents establishments as individual simulation entities, which differs from the predecessor 
model’s generation and attraction of commercial vehicle trips from zone-level aggregate employment. This 
was an important design feature because, as the establishment surveys showed, there is a non-linear 
decreasing relationship between the number of employees at an establishment location and the number of 
commercial vehicle trips that location produces and attracts. Small businesses generate more trips per 
employee than larger businesses, a relationship that is lost when generating commercial vehicle trips from 
aggregate zonal employment. Moreover, over time, trends in establishment sizes change, with recent 
smaller-size establishments becoming more prevalent in most parts of the U.S.

The CVM comprises three primary modeling stages: (1) pre-processing models which create inputs variables that are essential to the simulation; (2) generation of vehicle routes and their starting conditions; and (3) the dynamic simulation of stops and travel for each route. 

##### Pre-Processes

The purpose of the pre-processing steps is to create scenario-specific variables that are important to how the CVM represents demand and its policy sensitivity. 

Establishment Synthesis creates records for individual establishments, which form the basis for generating establishment trip attractions and for generating commercial vehicle routes in downstream models. Representation of establishments by industry and size enables the CVM to more accurately represents the 
non-linear decreasing rates of both trip attractions and route productions. 

Household Attraction Generation links the CVM to the Resident model. This step generates commercial vehicle trip attractions to households, reading in the synthetic households and tours from the Resident model. These attractions include home deliveries of food and packages as well as various types of service 
visits. Household attractions are used in the creation of accessibility variables as a measure of residential customer demand and used as attractor variables in Stop Location Choice models where the customer type is residential.

Establishment Attraction Generation uses the synthetic establishments to generate commercial vehicle trip attractions to establishments. These attractions include goods deliveries and various types of service visits. Establishment attractions are used in the creation of accessibility variables as a measure of non residential customer demand and are used as attractor variables in Stop Location Choice models where the customer type is non-residential.

Accessibility creates a set of variables to represent access to household attractions and access to establishment attractions. Accessibility variables are used in Route Generation and Stop Generation models to represent potential customer demand and affect the propensity of establishments to generate more 
vehicle routes or make additional stops on a route. Accessibility variables allow demand to vary throughout the region based on the density of households and businesses.


##### Route Generation and Starting Conditions

The purpose of the route starting conditions step is to generate the commercial vehicle routes to be simulated and to create parameters for those routes that represent contextual variables. There are two different sets of models, one for regular establishments and another for TNCs, which are not establishment based.

Route Generation begins by generating some number of commercial vehicle routes for each establishment. Even within the same industry and firm, some establishment locations will generate daily commercial vehicle 
routes while others will not, which reflects different functions at each site. For example, corporate offices, which are unlikely to produce commercial vehicle goods and service trips, may be in a different location from production facilities, warehouses, or service facilities, which do produce commercial vehicle trips. Accordingly, the route generation model for establishments is a two-stage model. Stage 1 predicts whether an establishment will generate at least one commercial vehicle route, and Stage 2 predicts how many routes will be generated given at least one. The CVM software then creates separate simulation objects equal to the number of routes predicted for each establishment.

The route generation model for TNCs works differently. TNC drivers are effectively independent agents who work for online pickup and delivery services and are treated as their own establishments in the model. The demand for TNC pickup and delivery services is a function of the establishments which hire them through the online services, such as restaurants, grocery stores, and other businesses. For TNCs, the CVM generates a total number of TNC routes for an entire land use zone (LUZ), based on equations specified to estimate 
demand for pickups by the types of businesses that use them. As described below, the CVM then chooses a starting and ending location for each TNC route. The idea behind the choice of the LUZs was to generate TNC routes spatially as a function of the collective demand of businesses that use TNCs within the same 
general area. Although the model restricts the starting and ending locations of the TNC routes to the LUZ that generated them, the downstream model that chooses the location of pickup and delivery stops is not 
restricted to the starting LUZ.

Route Vehicle, Purpose, and Customer Type models are applied to each route generated in the simulation. The model is formulated as the multinomial joint choice across these three dimensions, which are closely correlated. Choice of vehicle type will depend on whether the route purpose involves goods pickup/delivery or service provision. For goods deliveries, if the customer type is a residential, a smaller or medium-size vehicle is more likely to be used, whereas multi-unit (tractor trailers) are seldom used to make deliveries to residences.

For TNCs, each route is assumed to use a light-duty vehicle type and the purpose to be for goods pickup/delivery, which represents 95 percent of the route observations in the TNC driver survey. TNC routes are also assumed to be eligible to serve a mix of residential and business customer type.

Route Start Time Choice models are applied to each CVM route, after the vehicle, purpose, and customer types have been chosen. The route starting time represents the departure time for the first trip on the route. The model is specified as an empirical distribution derived from the commercial vehicle survey and 
segmented by four groups of industries and by vehicle type, which were chosen for being statistically distinct from one another. For TNCs, distributions were derived based on the type of businesses being served by the TNC river—restaurant, retail, or other. 

Starting times are specified at 15-minute quarterly intervals within each hour of the day. The model is applied by drawing a starting time interval from the distribution appropriate to each combination of business and vehicle type. Note that the ending times for commercial vehicle routes are not pre-determined by a choice model, but rather determined by the sequence of downstream model outcomes—choices of next stop type, including to terminate the route; travel times between stops; and the time spent at each stop.

Route Origin and Termination Location Choice models are applied to choose the starting and ending location for each CVM route. The CVM allows for vehicles to begin or end their day at locations other than their home establishment. Separate models are used to choose locations for the origin of the route and for 
the location where the route will terminate. This is formulated as a two-level choice in which the first level is a location type choice, and the second level is the choice of zone (MGRA) given the type. The location type alternatives are:

• Establishment Location

• Other Warehouse/Distribution Center Land Use

• Other Residential Land Use

• Other Commercial/Public Land Use

The choice of location type for the termination of the route considers the location type choice made for the origin of the route. If the establishment location is chosen, then the zone (MGRA) is given by the establishment. The non-establishment location types were specified to support more accurate representation of commercial vehicle parking locations at the start and end of each day. While a majority of vehicle routes will originate and terminate at the establishment location, other common location types are warehouse and 
distribution centers, most belonging to the same company; residential locations, which are commonly a driver’s home; and other commercial or public land uses, such as maintenance facilities or customer sites. If the non-establishment location type has been chosen, the model will then choose an actual zone (MGRA), 
with the choice alternatives constrained by the availability of the particular land use type (warehousing, residential, other non-residential). The model also considers the various attraction variables such as employment or households in the MGRA as well as the distance from the establishment.

For TNCs, which are generated by LUZ, a single location is chosen to be both the origin of the route and the termination point of the route. This simple model which chooses a location within the LUZ based on household and employment attributes. In the TNC survey, some drivers recorded route origin or termination locations as “home.” Others began or ended their recorded routes at non-home locations, which appear to be based on when they began and ended working for their particular TNC service.


##### Route Stop Simulation Loop

The route stop simulation models produce the actual trips made by commercial vehicles. This is dynamic model incrementally creates each stop on the vehicle route, given the starting conditions of vehicle, purpose, and customer type; starting time of day; and the starting and ending locations for the route. As each stop is simulated, the CVM updates the location of the vehicle, the time of day, and the elapsed time on the route. These three factors are considered in each of the route simulation sub-models. The three models are run in the order shown until the “terminate route” purpose is chosen as the next stop purpose, which will create a trip to the pre-determined termination location and 
end the vehicle route.

Next Stop Purpose Choice decides what will happen next on each commercial vehicle route. This is the choice of one of seven stop purposes:

• Goods Pickup – a stop to pick up goods 

• Goods Delivery – a stop to deliver goods to a customer

• Service – a stop to provide a professional service to a customer

• Maintenance/Other – a stop for either vehicle maintenance/refueling or driver breaks

• Base Establishment – a trip the establishment where the vehicle is based, without ending the route

• Home – a trip to the driver’s home, without ending the route

• Terminate Route – a trip to the final stop on the route where the vehicle will be parked, ending the route. This is the location chosen by the Route Termination Location Choice model describe above

The next stop purpose model applies logical transition constraints, such that it is not possible to go from base to base or from home to home, since base and home each refer to a unique location. The model does not impose a limit on the number of stops that can be made; however, it is specified so that as the elapsed time 
on the tour increases, the probability of choosing the “terminate route” alternative increases. In addition, the terminate route alternative is more likely during certain time periods, irrespective of the starting time of the 
route, to reflect common business hours. 

The next stop purpose model for TNCs is very similar to regular establishments, except that TNCs are assumed to not have the “service” alternative, with customer interactions limited to goods pickup and delivery. 

Next Stop Location Choice determines the destination of the trip, where the next stop purpose will take place. Similar to the route origin and termination location choices, this is a two-level model. The first level is the choice of a land use type, and the second level is the choice of an actual zone (MGRA) given the land use type. The model is not applied if the next stop purpose is either “base” (establishment) or “terminate route” because those locations are already known. For other stop purposes, the location type alternatives are:

• Residential Land Use 

• Warehouse/Distribution Center Land Use

• Port Facilities (Seaport, Airport, Intermodal Rail)

• Other Commercial/Public Land Use

These land use types were specified to enable more accurate representation of the demand attracted to each of these types through model calibration. The location type choice is a function of many factors, including the stop purpose, customer type, vehicle type, elapsed time, and time of day. For example, if the 
stop purpose is “home” or the customer type for a goods delivery or service stops is “residential,” then the residential land use type should be chosen. Other location land use types would be more likely for certain business types than others. 

Given the choice of a location land use type, the CVM then chooses an actual zone (MGRA) from among the zones eligible for that land use type. The residential location type is expected to include only those zones with households; the warehouse/distribution center location type considers only zones with employment in transportation/warehousing; the port facilities type includes only zones with designated seaport, airport, or intermodal rail facilities; and the other commercial/public land use type must contain employment in any 
industry sector, other than transportation/warehousing. 

The location choices model is specified to consider the household and establishment attractors in each zone, as predicted by those pre-processing models as well as other employment variables, depending on the stop 
purpose. The model also considers travel times and vehicle operating costs, including the travel time from the current location to alternative next stop locations and the travel time from the eventual terminal location 
to the alternative next stop locations, and the elapsed time on the route. As the elapsed time on the route increases, the model will be more likely to choose locations closer to the terminal location.

Next Stop Duration Simulation determines the simulated time spent at each location. For goods purposes, the stop duration would include time spent loading or unloading items and interacting with customers. For services, the duration represents the amount of time spent at the customer site providing the service, which will vary widely depending on the business type, such as installation/repairs, landscaping, construction, health care, and cleaning, to name a few common service types. For maintenance/other purposes, the stop duration could involve vehicle refueling or repairs or it could involve driver meal breaks or other personal or company business. For the “home” purpose, duration is assumed to be personal time spent at the driver’s home.

For regular establishments, the duration is simulated using a model that considers the stop purpose, the vehicle type used, and whether the stop begins before or after 12 p.m., with stops later in the day expected to be more time constrained, particularly for service provision. For TNCs, the duration model considers the stop purpose and the land use density at the stop location, with increased density assumed to require longer stop times, as would be expected for deliveries to multi-family residential building and to office buildings.


## Inputs

CVM inputs include the following, which are generated from SANDAG ABM resident model prior to the CVM sub-model step. 

• final_households.csv: from resident model 

• final_tours.csv: from resident model 

• final_persons.csv: from resident model 

• land_use.csv: from resident model 

• SynthEstablishments.csv: from the establishment synthesis process 

• Highway skims 

-> traffic_skims_EA.omx 

-> traffic_skims_AM.omx 

-> traffic_skims_MD.omx 

-> traffic_skims_PM.omx 

-> traffic_skims_EV.omx 

## Outputs

• final_commercial_accessibility.csv: contains the commercial vehicle accessibility by category for each zone. 

• final_cv_trips.csv: contains the commercial vehicle trips by OD, trip purpose and travel time. 

• final_establishments.csv: establishment details generated for each zone 

• final_establishments_all.csv 

• final_households.csv: household information which attract CVM trips 

• final_land_use.csv: MGRA zonal land use information  

• final_routes.csv: CVM trip route information  

• final_trips.csv: final trip-based summary, and it has more attributes than cvm_cv_trips.csv, e.g. distance, OD zone, toll cost. It will go into SANDAG final trip summary. 

• cvmtrips_TOD.omx: contains the commercial vehicle trips of the TOD, has three components (modes): car, light-heavy truck, medium-heavy truck and heavy-heavy truck. Trip tables included in each matrix are: CAR, LIGHT_TRUCK, MEDIUM_TRUCK and HEAVY_TRUCK 