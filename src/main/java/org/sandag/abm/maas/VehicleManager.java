package org.sandag.abm.maas;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.math.MersenneTwister;

public class VehicleManager {
	
	private static final Logger logger = Logger.getLogger(VehicleManager.class);
	private HashMap<String, String> propertyMap = null;
	private ArrayList<Vehicle>[] emptyVehicleList; //by taz
	private ArrayList<Vehicle> vehiclesToRouteList;
	private ArrayList<Vehicle> activeVehicleList;  
	private MersenneTwister       random;
	private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
	private static final String VEHICLETRIP_OUTPUT_FILE_PROPERTY = "TNC.shared.vehicletrip.output.file";
	private String vehicleTripOutputFile;
	private TazDataManager                tazManager;
	private MgraDataManager               mazManager;
	private int maxTaz;
	private byte maxPassengers;
	private TransportCostManager transportCostManager;
	private int totalVehicles;
	private int minutesPerSimulationPeriod;
	private int vehicleDebug;
	private int totalVehicleTrips;
    
    
 
    /**
     * 
     * @param propertyMap
     * @param transportCostManager
     */
    public VehicleManager(HashMap<String, String> propertyMap, TransportCostManager transportCostManager, byte maxPassengers, int minutesPerSimulationPeriod){
		
		this.propertyMap = propertyMap;
		this.transportCostManager = transportCostManager;
		this.maxPassengers = maxPassengers;
		this.minutesPerSimulationPeriod = minutesPerSimulationPeriod;
		
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(){
		
       int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
       random = new MersenneTwister(seed + 234324);
       
       tazManager = TazDataManager.getInstance();
       maxTaz = tazManager.getMaxTaz();
       
       mazManager = MgraDataManager.getInstance();
       
       emptyVehicleList = new ArrayList[maxTaz+1];
       
       activeVehicleList = new ArrayList<Vehicle>();
       vehiclesToRouteList = new ArrayList<Vehicle>();
       
       String directory = Util.getStringValueFromPropertyMap(propertyMap, "Project.Directory");
       vehicleTripOutputFile = directory + Util.getStringValueFromPropertyMap(propertyMap, VEHICLETRIP_OUTPUT_FILE_PROPERTY);
       
       vehicleDebug = 1;

	}
	
	/**
	 * This method gets the closest empty vehicle to the requested zone, and removes it from the empty vehicle list.
	 * If there is no vehicle within the maximum distance threshold, it generates
	 * a new vehicle and returns it.
	 * 
	 * @param departureMaz	MAZ where vehicle is requested from.
	 * @return
	 */
	public Vehicle getClosestEmptyVehicle(int skimPeriod, int simulationPeriod, int departureMaz ){
				
		int departureTaz = mazManager.getTaz(departureMaz);
		short[] sortedTazs = transportCostManager.getZoneNumbersSortedByTime(skimPeriod, departureTaz);
		if(sortedTazs == null){
			
			logger.info("There are no TAZs within the maximum distance from TAZ: "+departureTaz+"; please check and/or increase maximum distance used for pickup property");
			return generateVehicle(simulationPeriod, departureTaz);
		}
			
		//iterate through zones that have already been ordered by distance from the departure TAZ.
		for(int i = 0; i < sortedTazs.length; ++ i){
			
			int taz = sortedTazs[i];
			
			//if there are empty vehicles in the TAZ, return one at random
			if(emptyVehicleList[taz] != null && emptyVehicleList[taz].size()>0){
				
				Vehicle vehicle = null;
				double rnum = random.nextDouble();
				vehicle = getRandomVehicleFromList(emptyVehicleList[taz], rnum);
				
				//generate an empty trip for the vehicle
				ArrayList<VehicleTrip> trips = vehicle.getVehicleTrips();
				
				if(trips.size()==0){
					logger.warn("Weird: got an empty vehicle (id:"+vehicle.getId()+") but no trips in vehicle trip list");
					return vehicle;
				}
				
				VehicleTrip lastTrip = trips.get(trips.size()-1);
				int originTaz = lastTrip.getDestinationTaz();
				int originMaz = lastTrip.getDestinationMaz();
				ArrayList<Integer> pickupIdsAtOrigin = lastTrip.getPickupIdsAtDestination();
				ArrayList<Integer> dropoffIdsAtOrigin = lastTrip.getDropoffIdsAtDestination();
				
				++totalVehicleTrips;
				VehicleTrip newTrip = new VehicleTrip(vehicle,totalVehicleTrips);
				
				newTrip.setOriginMaz(originMaz);
				newTrip.setOriginTaz((short) originTaz);
				newTrip.setDestinationMaz(departureMaz);
				newTrip.setDestinationTaz((short)departureTaz);
				newTrip.setStartPeriod(simulationPeriod);
				newTrip.setPickupIdsAtOrigin(pickupIdsAtOrigin);
				newTrip.setDropoffIdsAtOrigin(dropoffIdsAtOrigin);
				vehicle.addVehicleTrip(newTrip);
				
				return vehicle;
			}
			
		}
		
		//Iterated through all TAZs, could not find an empty vehicle. Return a new vehicle.
		return generateVehicle(simulationPeriod, departureTaz);
	}

	/**
	 * Get a vehicle at random from the arraylist of vehicles, remove it from the list, and return it.
	 * 
	 * @param emptyVehicleList
	 * @param rnum a random number used to draw a vehicle from the list.
	 * @return The vehicle chosen.
	 */
	private Vehicle getRandomVehicleFromList(ArrayList<Vehicle> vehicleList, double rnum){
		
				
		if(vehicleList==null)
			return null;
		
		int listSize = vehicleList.size();
		int element = (int) Math.floor(rnum * listSize);
		Vehicle vehicle = vehicleList.get(element);
		vehicleList.remove(element);
		return vehicle;
		
	}
	
	/**
	 * Encapsulating in method so that vehicles and some statistics can be tracked.
	 */
	private Vehicle generateVehicle(int simulationPeriod, int taz){
		++totalVehicles;
		Vehicle vehicle = new Vehicle(totalVehicles, maxPassengers);
		vehicle.setGenerationPeriod((short)simulationPeriod);
		vehicle.setGenerationTaz((short) taz);
		return vehicle;
		
	}
	
	public int getTotalVehicles(){
		return totalVehicles;
	}

	/**
	 * Add empty vehicle to the empty vehicle list.
	 * 
	 * @param vehicle
	 * @param taz
	 */
	public void storeEmptyVehicle(Vehicle vehicle, int taz){
		
		if(emptyVehicleList[taz] == null)
			emptyVehicleList[taz] = new ArrayList<Vehicle>();
		
		emptyVehicleList[taz].add(vehicle);
		
	}
	
	public void addActiveVehicle(Vehicle vehicle){
		
		activeVehicleList.add(vehicle);
	}
	
	public void addVehicleToRoute(Vehicle vehicle){
		
		ArrayList<PersonTrip> personTrips = vehicle.getPersonTripList();
		if(personTrips.size()==0){
			logger.info("Adding vehicle "+vehicle.getId()+" to vehicles to route list but no person trips");
			throw new RuntimeException();
		}
		vehiclesToRouteList.add(vehicle);
	}

	public void routeActiveVehicles(int skimPeriod, int simulationPeriod, TransportCostManager transportCostManager){
		
		logger.info("Routing "+vehiclesToRouteList.size()+" vehicles in period "+simulationPeriod);
		ArrayList<Vehicle> vehiclesToRemove = new ArrayList<Vehicle>();
		
		
		//iterate through vehicles to route list
		for(Vehicle vehicle: vehiclesToRouteList){
			
			// get the person list, if it is empty throw a warning (should never be empty)
			ArrayList<PersonTrip> personTrips = vehicle.getPersonTripList();
			if(personTrips==null||personTrips.size()==0){
				logger.error("Attempting to route empty vehicle "+vehicle.getId());
			}
			
			if(vehicle.getId()==vehicleDebug){
				logger.info("***********************************************************************************");
				logger.info("Debugging vehicle routing for vehicle "+vehicle.getId());
				logger.info("***********************************************************************************");
				logger.info("There are "+personTrips.size()+" person trips in vehicle "+vehicle.getId());
				for(PersonTrip pTrip: personTrips){
					logger.info("Vehicle "+vehicle.getId()+" person trip id: "+pTrip.getUniqueId()+" from origin MAZ: "+pTrip.getOriginMaz()+ " to destination MAZ "+pTrip.getDestinationMaz());
				}
			}
			//some information on the first passenger
			PersonTrip firstTrip = personTrips.get(0);
			int firstOriginMaz = firstTrip.getOriginMaz();
			int firstOriginTaz = mazManager.getTaz(firstOriginMaz);
			int firstDestinationMaz = firstTrip.getDestinationMaz();
			int firstDestinationTaz = mazManager.getTaz(firstDestinationMaz);

			// get the arraylist of vehicle trips for this vehicle
			ArrayList<VehicleTrip> existingVehicleTrips = vehicle.getVehicleTrips();
			ArrayList<VehicleTrip> newVehicleTrips = new ArrayList<VehicleTrip>();
			
			if(vehicle.getId()==vehicleDebug)
				logger.info("There are "+existingVehicleTrips.size()+" existing vehicle trips in vehicle "+vehicle.getId());
			
			//iterate through person list and save HashMap of other passenger pickups and dropoffs by MAZ
			HashMap<Integer,ArrayList<PersonTrip>> pickupsByMaz = new HashMap<Integer,ArrayList<PersonTrip>>();
			HashMap<Integer,ArrayList<PersonTrip>> dropoffsByMaz = new HashMap<Integer,ArrayList<PersonTrip>>();
			
			//save the dropoff location of the first passenger in the dropoffsByMaz array (the pickup location must be the trip origin)
			ArrayList<PersonTrip> firstDropoffArray = new ArrayList<PersonTrip>();
			firstDropoffArray.add(personTrips.get(0));
			dropoffsByMaz.put(firstDestinationMaz, firstDropoffArray);
			
			for(int i = 1; i < personTrips.size();++i){
				
				PersonTrip personTrip = personTrips.get(i);
				
				int pickupMaz = personTrip.getOriginMaz();
				int dropoffMaz = personTrip.getDestinationMaz();
				
				//only add pickup maz for passengers other than first passenger
				if(!pickupsByMaz.containsKey(pickupMaz) ){
					ArrayList<PersonTrip> pickups = new ArrayList<PersonTrip>();
					pickups.add(personTrip);
					pickupsByMaz.put(pickupMaz,pickups);
				}else{
					ArrayList<PersonTrip> pickups = pickupsByMaz.get(pickupMaz);
					pickups.add(personTrip);
					pickupsByMaz.put(pickupMaz,pickups);
				}
				
				if(!dropoffsByMaz.containsKey(dropoffMaz)){
					ArrayList<PersonTrip> dropoffs = new ArrayList<PersonTrip>();
					dropoffs.add(personTrip);
					dropoffsByMaz.put(dropoffMaz,dropoffs);
				}else{
					ArrayList<PersonTrip> dropoffs = dropoffsByMaz.get(dropoffMaz);
					dropoffs.add(personTrip);
					dropoffsByMaz.put(dropoffMaz,dropoffs);
				}
			}
			
			if(vehicle.getId()==vehicleDebug){
				logger.info("There are "+pickupsByMaz.size()+" pickup mazs in vehicle "+vehicle.getId());
				logger.info("There are "+dropoffsByMaz.size()+" dropoff mazs in vehicle "+vehicle.getId());
			}

			// the list of TAZs in order from closest to furthest, that will determine vehicle routing.
			// any TAZ in the list with an origin or destination by a passenger will be visited.
			short[] tazs = transportCostManager.getZonesWithinMaxDiversionTime(skimPeriod, firstOriginTaz, firstDestinationTaz);
			
			//create a new vehicle trip, and populate it with information from the first passenger
			++totalVehicleTrips;
			VehicleTrip trip = new VehicleTrip(vehicle,totalVehicleTrips);
			trip.setStartPeriod(simulationPeriod);
			trip.addPickupAtOrigin(firstTrip.getUniqueId());
			trip.setOriginMaz(firstOriginMaz);
			trip.setOriginTaz((short) firstOriginTaz);
			trip.setPassengers(1);
				
			//iterate through tazs sorted by time from first passenger's origin, and 
			//assign person trips to pickup and dropoff arrays based on diversion time.
			for(int i=0;i<tazs.length;++i){
				
				int[] mazs = tazManager.getMgraArray(tazs[i]);
				
				//external taz
				if(mazs==null){
					continue;
				}
				//iterate through mazs
				for(int maz : mazs){
					
					//no pickups or dropoffs in this maz, so keep going
					if((!pickupsByMaz.containsKey(maz)) && (!dropoffsByMaz.containsKey(maz))){
						continue;
					}

					//there are pickups in this maz
					if(pickupsByMaz.containsKey(maz)){
						ArrayList<PersonTrip> pickups = pickupsByMaz.get(maz);
						for(int p = 0; p< pickups.size();++p){
							PersonTrip pTrip = pickups.get(p);
							trip.addPickupAtDestination(pTrip.getUniqueId());
						}
					}
			
					//there are dropoffs in this maz
					if(dropoffsByMaz.containsKey(maz)){
						ArrayList<PersonTrip> dropoffs = dropoffsByMaz.get(maz);
						for(int p = 0; p< dropoffs.size();++p){
							PersonTrip pTrip = dropoffs.get(p);
							trip.addDropoffAtDestination(pTrip.getUniqueId());
							
							//remove this person trip from the list of persons in this vehicle since they are getting dropped off.
							vehicle.removePersonTrip(pTrip);

						}
					}
					
					// this is not the first vehicle trip for this vehicle. So we need to find the last vehicle trip
					// occupancy and destination pickups and dropoffs to set the trip occupancy and origin pickups & dropoffs accordingly.
					int lastTripPassengers=0;
					VehicleTrip lastTrip = null;
					if(newVehicleTrips.size()==0 && existingVehicleTrips.size()>0){
						lastTrip = existingVehicleTrips.get(existingVehicleTrips.size()-1);
					}else if(newVehicleTrips.size()>0){
						lastTrip = newVehicleTrips.get(newVehicleTrips.size()-1);
					}
					//set the origin and other values for the trip
					if(lastTrip!=null){
						lastTripPassengers = lastTrip.getPassengers();
						ArrayList<Integer> dropoffsAtDestinationOfLastTrip = lastTrip.getDropoffIdsAtDestination();
						ArrayList<Integer> pickupsAtDestinationOfLastTrip = lastTrip.getPickupIdsAtDestination();
						
						//add pickups and dropoffs at origin from last trip
						trip.addDropoffIdsAtOrigin(dropoffsAtDestinationOfLastTrip);	
						trip.addPickupIdsAtOrigin(pickupsAtDestinationOfLastTrip);	
	
					}
					
					int passengers = lastTripPassengers + trip.getNumberOfPickupsAtOrigin() - trip.getNumberOfDropoffsAtOrigin();
					trip.setPassengers(passengers);
					trip.setDestinationMaz(maz);
					trip.setDestinationTaz((short) tazs[i]);

					float time = transportCostManager.getTime(skimPeriod, firstOriginTaz, tazs[i]);
					float periods = time/(float)minutesPerSimulationPeriod;
					int endPeriod = (int) Math.floor(simulationPeriod + periods);
					trip.setEndPeriod(endPeriod);
					
					if(vehicle.getId()==vehicleDebug){
						logger.info("Vehicle "+vehicle.getId()+" now has vehicle trip "+trip.getId());
						trip.writeTrace();
					}

					newVehicleTrips.add(trip);
					
					//more trips to go!
					if(vehicle.getPersonTripList().size()>0){
						++totalVehicleTrips;
						VehicleTrip newTrip = new VehicleTrip(vehicle,totalVehicleTrips);
						newTrip.setOriginMaz(maz);
						newTrip.setOriginTaz((short) tazs[i]);
						newTrip.setStartPeriod(trip.getEndPeriod());
						trip = newTrip;
					}
					
				} //end mazs

			} //end tazs

			//add vehicle trips to vehicle
			vehicle.addVehicleTrips(newVehicleTrips);
	
			//add vehicle to active vehicles
			activeVehicleList.add(vehicle);
			
			//track vehicle in vehicles to remove from route list.
			vehiclesToRemove.add(vehicle);
		} //end vehicles
		
		//Remove vehicles that have been routed
		vehiclesToRouteList.removeAll(vehiclesToRemove);
	}
	
	/**
	 * Free vehicles from the active vehicle list and put them in the free vehicle list if
	 * the last trip in the vehicle ends in the current simulation period.
	 * 
	 * @param simulationPeriod
	 */
	public void freeVehicles(int simulationPeriod){
		
		int freedVehicles=0;
		
		//no active vehicles in the simulation period
		if(activeVehicleList.size()==0){
			logger.warn("Trying to free vehicles from active vehicle list in simulation period "+simulationPeriod+" but there are no active vehicles.");
		}else{
			logger.info("There are "+activeVehicleList.size()+" active vehicles in period "+simulationPeriod);
		}
	
		//track the vehicles to remove
		ArrayList<Vehicle> vehiclesToRemove = new ArrayList<Vehicle>();
		// go through active vehicles (vehicles that have been routed and are picking up/dropping off passengers)
		for(int i = 0; i< activeVehicleList.size();++i){
			Vehicle vehicle = activeVehicleList.get(i);
			
			ArrayList<VehicleTrip> trips = vehicle.getVehicleTrips();
			
			//this vehicle has no trips; why is it in the active vehicle list??
			if(trips.size()==0){
				logger.error("Vehicle "+vehicle.getId()+" has no vehicle trips but is in active vehicle list");
				continue;
			}
			
			//Find out when the last dropoff occurs (the end period of the last trip)
			VehicleTrip lastTrip = trips.get(trips.size()-1);
			if(lastTrip.endPeriod==simulationPeriod){
				int taz = lastTrip.getDestinationTaz();
				vehiclesToRemove.add(vehicle);
				++freedVehicles;
				
				//store the empty vehicle in the last dropoff location (the last trip destination TAZ)
				if(emptyVehicleList[taz]==null)
					emptyVehicleList[taz]= new ArrayList<Vehicle>();
				
				emptyVehicleList[taz].add(vehicle);
			}
		}
		activeVehicleList.removeAll(vehiclesToRemove);
		logger.info("Freed "+freedVehicles+" vehicles from active vehicle list");
		logger.info("There are now "+activeVehicleList.size()+" vehicles in the active vehicle list");
	}
	
	
	public void writeVehicleTrips(){
		
		logger.info("Writing vehicle trips to file " + vehicleTripOutputFile);
        PrintWriter printWriter = null;
        try
        {
        	printWriter = new PrintWriter(new BufferedWriter(new FileWriter(vehicleTripOutputFile)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + vehicleTripOutputFile + " for writing\n");
            throw new RuntimeException();
        }
        
        VehicleTrip.printHeader(printWriter);
        for(int i = 1; i <= maxTaz; ++ i){
        	if(emptyVehicleList[i]==null)
        		continue;
        	
        	if(emptyVehicleList[i].size()==0)
        		continue;

        	for(Vehicle vehicle : emptyVehicleList[i] ){
        		for(VehicleTrip vehicleTrip : vehicle.getVehicleTrips()){
        			vehicleTrip.printData(printWriter);
        		}
        	}
        }
        printWriter.close();
	}
}
