# Flexible Fleets
The of the five big moves defined in SANDAG's 2021 regional plan was [Flexible Fleets](https://www.sandag.org/projects-and-programs/innovative-mobility/flexible-fleets), which involves on-demand transit services. The [initial concept](https://www.sandag.org/-/media/SANDAG/Documents/PDF/regional-plan/2025-regional-plan/2025-rp-draft-initial-concept-2024-1-25.pdf) of the 2025 Regional Plan involves rapidly expanding these services, with many new services planned to be in operation by 2035. For this reason, it is important that these services be modeled by ABM3. There are two flavors of flexible fleets that were incorporated into ABM3, Neighborhood Electric Vehicles (NEV) and microtransit. A table contrasting these services is shown below.

| Characteristic  | NEV     | Microtransit |
| --------------- | ------- | ------------ |
| Vehicle Size    | Smaller | Larger       |
| Service Area    | Smaller | Larger       |
| Operating Speed | Slower  | Faster       |

## Incorporation into ABM3

Rather than creating new modes for flexible fleet services, microtransit and NEV were incorporated into existing modes. How this was done was dependent on whether the trip was a full flexible fleet trip, first-mile access to fixed-route transit*, or last-mile egress from fixed-route transit*. A table explaining how each of these trip types was incorporated into ABM3 is shown below. Further, a heirarchy of services is enforced. ActivitySim first checks if NEV is available (based on a new land use attribute), and if it is, it's assumed that NEV is used. If not, ActivitySim checks if microtransit is available (based on a corresponding land use attribute), and if it is, it's assumed that microtransit is used. If neither are available, ActivitySim looks at the other services that are already available.

**For trips on the return leg of a tour the access and egress attributes are swapped*

|                                                               | Full microtransit trip                              | First-mile access to fixed-route transit                                                    | Last-mile egress from fixed-route transit                                                                                                                                                                    |
| ------------------------------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| What models allow for this type of trip?                      | Resident, Visitor, Crossborder                      | Resident                                                                                    | Resident, Visitor, Crossborder                                                                                                                                                                               |
| Which mode is used?                                           | TNC Shared                                          | TNC to transit                                                                              | All transit modes                                                                                                                                                                                            |
| How is the flexible fleet travel time factored into the trip? | The travel time is the full travel time of the trip | The travel time is added to the transit access time and a transfer is added                 | The travel time is added to the transit egress time and a transfer is added if the destination is further from the nearest transit stop than a user would be willing to walk (that distance is configurable) |
| How is the flexible fleet cost factored into the trip?        | The cost is the full cost of the trip               | It is assumed that flexible fleet services are free when used to access fixed-route transit | It is assumed that flexible fleet services are free when egressing from fixed-route transit                                                                                                                  |

## New Attributes
Several new attributes were added to allow the user to configure how flexible fleet services are operated. These are all defined in the common [constants.yaml](https://github.com/SANDAG/ABM/blob/ABM3_develop/src/asim/configs/common/constants.yaml#L255-L273) file. Each attribute is defined as follow:

| Attribute                  | Definition                                                                                                                                   | Default value   |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | --------------- |
| Speed                      | Assumed operating speed in miles per hour                                                                                                    | MT: 30, NEV: 17 |
| Cost                       | Cost of using service in US Cents                                                                                                            | 125 for both    |
| WaitTime                   | Assumed time passengers wait to wait to use service in minutes                                                                               | 12 for both     |
| MaxDist                    | Maximum distance in miles that the service can be used                                                                                       | MT: 4.5, NEV: 3 |
| DiversionConstant          | Additional travel time to divert for servicing other passengers                                                                              | 6 for both      |
| DiversionFactor            | Time multiplier accounting for diversion to service other passengers                                                                         | 1.25 for both   |
| StartPeriod                | Time period to start service (not yet implemented)                                                                                           | 9 for both      |
| EndPeriod                  | Time period to end service (not yet implemented)                                                                                             | MT: 32, NEV: 38 |
| maxWalkIfMTAccessAvailable | Maximum disatance someone is willing to walk at the destination end if flexible fleet services are available (same for microtransit and NEV) | 0.5             |

## Travel Time Calculation
### Direct Time
The flexible fleet travel time calculation is a two-step process. The first step is to calculate the time that it would take to travel from the origin to the destination* directly without any diversion to pick up or drop off any passengers. This is done by taking the maximum of the time implied by the operating speed and the congested travel time:

$t_{\textnormal{direct}} = \textnormal{max}(60\times\frac{s}{d}, t_{\textnormal{congested}})$

where:

$t_{\textnormal{direct}} = \textnormal{Direct flexible fleet travel time}$

$s = \textnormal{speed}$

$d = \textnormal{Distance from origin to destination (taken from distance skim)}$

$t_{\textnormal{congested}} = \textnormal{Congested travel time from origin to destination (taken from Shared Ride 3 time skim)}$

**When used to access fixed-route transit, the destination is the nearest transit stop to the trip origin. When used to egress from fixed-route transit, the origin is the nearest transit stop to the trip destination.*

### Total Time
The second step of the travel time calculation was to account for diversion to pick up other passengers. These were based on guidelines used in a NEV pilot. The formula to calculated the total flexible fleet travel time is as follows:

$t_{\textnormal{total}} = \textnormal{max}(t_{\textnormal{direct}}+c, \alpha\times t_{\textnormal{direct}})$

where:

$t_{\textnormal{total}} = \textnormal{Total flexible fleet travel time}$

$c = \textnormal{DiversionConstant}$

$\alpha = \textnormal{DiversionFactor}$
