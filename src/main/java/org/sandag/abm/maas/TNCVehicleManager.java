package org.sandag.abm.maas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.maas.TNCVehicleTrip.Purpose;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.math.MersenneTwister;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;

public class TNCVehicleManager {
	
	protected static final Logger logger = Logger.getLogger(TNCVehicleManager.class);
	protected HashMap<String, String> propertyMap = null;
	protected ArrayList<TNCVehicle>[] emptyVehicleList; //by taz
	protected ArrayList<TNCVehicle> vehiclesToRouteList;
	protected ArrayList<TNCVehicle> activeVehicleList;  
	protected ArrayList<TNCVehicle> refuelingVehicleList;
	
	protected MersenneTwister       random;
	protected static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
	protected static final String VEHICLETRIP_OUTPUT_FILE_PROPERTY = "Maas.RoutingModel.vehicletrip.output.file";
	protected static final String VEHICLETRIP_OUTPUT_MATRIX_PROPERTY = "Maas.RoutingModel.vehicletrip.output.matrix";
	protected static final String MAX_DISTANCE_BEFORE_REFUEL_PROPERTY = "Maas.RoutingModel.maxDistanceBeforeRefuel";
	protected static final String TIME_REQUIRED_FOR_REFUEL_PROPERTY = "Maas.RoutingModel.timeRequiredForRefuel";
	
	
	protected String vehicleTripOutputFile;
	protected TazDataManager                tazManager;
	protected MgraDataManager               mazManager;
	protected int maxTaz;
	protected int maxMaz;
	protected byte maxPassengers;
	protected TransportCostManager transportCostManager;
	protected int totalVehicles;
	protected int minutesPerSimulationPeriod;
	protected int vehicleDebug;
	protected int totalVehicleTrips;
    private byte[] skimPeriodLookup; //an array indexed by number of periods that corresponds to the skim period
    private int numberOfSimulationPeriods;
	protected float maxDistanceBeforeRefuel;
	protected float timeRequiredForRefuel;
	protected int periodsRequiredForRefuel;
    protected int[] closestMazWithRefeulingStation ; //the closest MAZ with a refueling station

    // one file per time period
    // matrices are indexed by periods, occupants
    private Matrix[][]  TNCTripMatrix;

    
    
 
    /**
     * 
     * @param propertyMap
     * @param transportCostManager
     */
    public TNCVehicleManager(HashMap<String, String> propertyMap, TransportCostManager transportCostManager, byte maxPassengers, int minutesPerSimulationPeriod){
		
		this.propertyMap = propertyMap;
		this.transportCostManager = transportCostManager;
		this.maxPassengers = maxPassengers;
		this.minutesPerSimulationPeriod = minutesPerSimulationPeriod;
		numberOfSimulationPeriods = ((24*60)/minutesPerSimulationPeriod);

	}
	
	@SuppressWarnings("unchecked")
	public void initialize(){
		
       int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
       random = new MersenneTwister(seed + 234324);
       
       tazManager = TazDataManager.getInstance();
       maxTaz = tazManager.getMaxTaz();
       
       mazManager = MgraDataManager.getInstance();
       maxMaz = mazManager.getMaxMgra();
       
       emptyVehicleList = new ArrayList[maxTaz+1];
       
       activeVehicleList = new ArrayList<TNCVehicle>();
       vehiclesToRouteList = new ArrayList<TNCVehicle>();
       refuelingVehicleList = new ArrayList<TNCVehicle>();
       
       String directory = Util.getStringValueFromPropertyMap(propertyMap, "Project.Directory");
       vehicleTripOutputFile = directory + Util.getStringValueFromPropertyMap(propertyMap, VEHICLETRIP_OUTPUT_FILE_PROPERTY);
       
   	   maxDistanceBeforeRefuel = Util.getFloatValueFromPropertyMap(propertyMap, MAX_DISTANCE_BEFORE_REFUEL_PROPERTY);
   	   timeRequiredForRefuel = Util.getFloatValueFromPropertyMap(propertyMap, TIME_REQUIRED_FOR_REFUEL_PROPERTY);
       periodsRequiredForRefuel = (int) Math.ceil(timeRequiredForRefuel/minutesPerSimulationPeriod);
   	   
       vehicleDebug = 1;
       
       calculateClosestRefuelingMazs();
       
    	calculateSkimPeriods();

	    //initialize the matrices for writing trips
       int maxTaz = tazManager.getMaxTaz();
       int[] tazIndex = new int[maxTaz + 1];

       // assume zone numbers are sequential
       for (int i = 1; i < tazIndex.length; ++i)
           tazIndex[i] = i;
       
       TNCTripMatrix = new Matrix[transportCostManager.NUM_PERIODS][];

	    for(int i =0;i<TNCTripMatrix.length;++i) {
	    	TNCTripMatrix[i] = new Matrix[4];
	    	for(int j=0;j<TNCTripMatrix[i].length;++j) {
	    		TNCTripMatrix[i][j] = new Matrix("TNC_" + transportCostManager.PERIODS[i] + "_"+j, "", maxTaz, maxTaz);
	    		TNCTripMatrix[i][j].setExternalNumbers(tazIndex);
	    	}
	    	
	    }


	}
	
	
	/**
	 * Iterate through the zones for each MAZ and find the closest MAZ with at least one refeuling station.
	 * 
	 */
	public void calculateClosestRefuelingMazs() {
	   	   
		//initialize the array
		closestMazWithRefeulingStation = new int[maxMaz+1];

		//iterate through origin MAZs
		for(int originMaz=1;originMaz<=maxMaz;++originMaz) {
			
			float minDist = 99999; //initialize to a really high value
			
			int originTaz = mazManager.getTaz(originMaz);
			if(originTaz<=0)
				continue;
			
			//iterate through destination MAZs
			for(int destinationMaz=1;destinationMaz<=maxMaz;++destinationMaz) {
				
				//no refueling stations in the destination, keep going
				if(mazManager.getRefeulingStations(originMaz)==0)
					continue;
				
				int destinationTaz = mazManager.getTaz(destinationMaz);
				if(destinationTaz<=0)
					continue;

				float dist = transportCostManager.getDistance(transportCostManager.MD, originTaz, destinationTaz);
				
				//lowest distance, so reset the closest MAZ
				if(dist<minDist)
					closestMazWithRefeulingStation[originMaz]=destinationMaz;
			}
			
		}
	}
	
	/**
	 * This method gets the closest empty tNCVehicle to the requested zone, and removes it from the empty tNCVehicle list.
	 * If there is no tNCVehicle within the maximum distance threshold, it generates
	 * a new tNCVehicle and returns it.
	 * 
	 * @param departureMaz	MAZ where tNCVehicle is requested from.
	 * @return
	 */
	public TNCVehicle getClosestEmptyVehicle(int skimPeriod, int simulationPeriod, int departureMaz ){
				
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
				
				TNCVehicle tNCVehicle = null;
				double rnum = random.nextDouble();
				tNCVehicle = getRandomVehicleFromList(emptyVehicleList[taz], rnum);
				
				//generate an empty trip for the tNCVehicle
				ArrayList<TNCVehicleTrip> trips = tNCVehicle.getVehicleTrips();
				
				if(trips.size()==0){
					logger.warn("Weird: got an empty tNCVehicle (id:"+tNCVehicle.getId()+") but no trips in tNCVehicle trip list");
					return tNCVehicle;
				}
				
				TNCVehicleTrip lastTrip = trips.get(trips.size()-1);
				int originTaz = lastTrip.getDestinationTaz();
				int originMaz = lastTrip.getDestinationMaz();
				ArrayList<String> pickupIdsAtOrigin = lastTrip.getPickupIdsAtDestination();
				ArrayList<String> dropoffIdsAtOrigin = lastTrip.getDropoffIdsAtDestination();
				
				++totalVehicleTrips;
				TNCVehicleTrip newTrip = new TNCVehicleTrip(tNCVehicle,totalVehicleTrips);
				
				newTrip.setOriginMaz(originMaz);
				newTrip.setOriginTaz((short) originTaz);
				newTrip.setDestinationMaz(departureMaz);
				newTrip.setDestinationTaz((short)departureTaz);
				newTrip.setStartPeriod(simulationPeriod);
				newTrip.setEndPeriod(simulationPeriod); //instantaneous arrivals? Need traveling tNCVehicle queue...
				if(lastTrip.getDestinationPurpose()==TNCVehicleTrip.Purpose.REFUEL)
					newTrip.setOriginPurpose(TNCVehicleTrip.Purpose.REFUEL);
				else {
					newTrip.setPickupIdsAtOrigin(pickupIdsAtOrigin);
					newTrip.setDropoffIdsAtOrigin(dropoffIdsAtOrigin);
				}
				newTrip.setDestinationPurpose(TNCVehicleTrip.Purpose.PICKUP_ONLY);
				tNCVehicle.addVehicleTrip(newTrip);
				
				return tNCVehicle;
			}
			
		}
		
		//Iterated through all TAZs, could not find an empty tNCVehicle. Return a new tNCVehicle.
		return generateVehicle(simulationPeriod, departureTaz);
	}

	/**
	 * Get a tNCVehicle at random from the arraylist of vehicles, remove it from the list, and return it.
	 * 
	 * @param emptyVehicleList
	 * @param rnum a random number used to draw a tNCVehicle from the list.
	 * @return The tNCVehicle chosen.
	 */
	private TNCVehicle getRandomVehicleFromList(ArrayList<TNCVehicle> vehicleList, double rnum){
		
				
		if(vehicleList==null)
			return null;
		
		int listSize = vehicleList.size();
		int element = (int) Math.floor(rnum * listSize);
		TNCVehicle tNCVehicle = vehicleList.get(element);
		vehicleList.remove(element);
		return tNCVehicle;
		
	}
	
	/**
	 * Encapsulating in method so that vehicles and some statistics can be tracked.
	 */
	private synchronized TNCVehicle generateVehicle(int simulationPeriod, int taz){
		++totalVehicles;
		TNCVehicle tNCVehicle = new TNCVehicle(totalVehicles, maxPassengers, maxDistanceBeforeRefuel);
		tNCVehicle.setGenerationPeriod((short)simulationPeriod);
		tNCVehicle.setGenerationTaz((short) taz);
		return tNCVehicle;
		
	}
	
	public int getTotalVehicles(){
		return totalVehicles;
	}

	/**
	 * Add empty tNCVehicle to the empty tNCVehicle list.
	 * 
	 * @param tNCVehicle
	 * @param taz
	 */
	public void storeEmptyVehicle(TNCVehicle tNCVehicle, int taz){
		
		if(emptyVehicleList[taz] == null)
			emptyVehicleList[taz] = new ArrayList<TNCVehicle>();
		
		emptyVehicleList[taz].add(tNCVehicle);
		
	}
	
	public void addActiveVehicle(TNCVehicle tNCVehicle){
		
		activeVehicleList.add(tNCVehicle);
	}
	
	public void addVehicleToRoute(TNCVehicle tNCVehicle){
		
		ArrayList<PersonTrip> personTrips = tNCVehicle.getPersonTripList();
		if(personTrips.size()==0){
			logger.info("Adding tNCVehicle "+tNCVehicle.getId()+" to vehicles to route list but no person trips");
			throw new RuntimeException();
		}
		vehiclesToRouteList.add(tNCVehicle);
	}

	/**
	 * All active vehicles are assigned passengers, now they must be routed through all pickups and dropoffs.
	 * THe method iterates through the vehiclesToRouteList and adds passengers based on the out-direction
	 * time required to pick them up and drop them off.
	 * 
	 * @param skimPeriod
	 * @param simulationPeriod
	 * @param transportCostManager
	 */
	public void routeActiveVehicles(int skimPeriod, int simulationPeriod, TransportCostManager transportCostManager){
		
		logger.info("Routing "+vehiclesToRouteList.size()+" vehicles in period "+simulationPeriod);
		ArrayList<TNCVehicle> vehiclesToRemove = new ArrayList<TNCVehicle>();
		
		
		//iterate through vehicles to route list
		for(TNCVehicle tNCVehicle: vehiclesToRouteList){
			
			// get the person list, if it is empty throw a warning (should never be empty)
			ArrayList<PersonTrip> personTrips = tNCVehicle.getPersonTripList();
			if(personTrips==null||personTrips.size()==0){
				logger.error("Attempting to route empty tNCVehicle "+tNCVehicle.getId());
			}
			
			if(tNCVehicle.getId()==vehicleDebug){
				logger.info("***********************************************************************************");
				logger.info("Debugging Vehicle routing for vehicle ID "+tNCVehicle.getId());
				logger.info("***********************************************************************************");
				logger.info("There are "+personTrips.size()+" person trips in vehicle ID "+tNCVehicle.getId());
				for(PersonTrip pTrip: personTrips){
					logger.info("Vehicle ID "+tNCVehicle.getId()+" person trip id: "+pTrip.getUniqueId()+" from pickup MAZ: "+pTrip.getPickupMaz()+ " to dropoff MAZ "+pTrip.getDropoffMaz());
				}
			}
			//some information on the first passenger
			PersonTrip firstTrip = personTrips.get(0);
			int firstOriginMaz = firstTrip.getPickupMaz();
			int firstOriginTaz = mazManager.getTaz(firstOriginMaz);
			int firstDestinationMaz = firstTrip.getDropoffMaz();
			int firstDestinationTaz = mazManager.getTaz(firstDestinationMaz);

			// get the arraylist of tNCVehicle trips for this tNCVehicle
			ArrayList<TNCVehicleTrip> existingVehicleTrips = tNCVehicle.getVehicleTrips();
			ArrayList<TNCVehicleTrip> newVehicleTrips = new ArrayList<TNCVehicleTrip>();
			
			if(tNCVehicle.getId()==vehicleDebug)
				logger.info("There are "+existingVehicleTrips.size()+" existing vehicle trips in vehicle ID "+tNCVehicle.getId());
			
			//iterate through person list and save HashMap of other passenger pickups and dropoffs by MAZ
			HashMap<Integer,ArrayList<PersonTrip>> pickupsByMaz = new HashMap<Integer,ArrayList<PersonTrip>>();
			HashMap<Integer,ArrayList<PersonTrip>> dropoffsByMaz = new HashMap<Integer,ArrayList<PersonTrip>>();
			
			//save the dropoff location of the first passenger in the dropoffsByMaz array (the pickup location must be the trip origin)
			ArrayList<PersonTrip> firstDropoffArray = new ArrayList<PersonTrip>();
			firstDropoffArray.add(personTrips.get(0));
			dropoffsByMaz.put(firstDestinationMaz, firstDropoffArray);
			
			//iterate through the rest of the person trips other than the first passenger
			for(int i = 1; i < personTrips.size();++i){
				
				PersonTrip personTrip = personTrips.get(i);
				
				int pickupMaz = personTrip.getPickupMaz();
				int dropoffMaz = personTrip.getDropoffMaz();
				
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
			
			if(tNCVehicle.getId()==vehicleDebug){
				logger.info("There are "+pickupsByMaz.size()+" pickup mazs in vehicle ID "+tNCVehicle.getId());
				logger.info("There are "+dropoffsByMaz.size()+" dropoff mazs in vehicle ID "+tNCVehicle.getId());
			}

			// the list of TAZs in order from closest to furthest, that will determine tNCVehicle routing.
			// any TAZ in the list with an origin or destination by a passenger will be visited.
			short[] tazs = transportCostManager.getZonesWithinMaxDiversionTime(skimPeriod, firstOriginTaz, firstDestinationTaz);
			
			//create a new tNCVehicle trip, and populate it with information from the first passenger
			++totalVehicleTrips;
			TNCVehicleTrip trip = new TNCVehicleTrip(tNCVehicle,totalVehicleTrips);
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
							
							//remove this person trip from the list of persons in this tNCVehicle since they are getting dropped off.
							tNCVehicle.removePersonTrip(pTrip);

						}
					}
					
					// this is not the first tNCVehicle trip for this tNCVehicle. So we need to find the last tNCVehicle trip
					// occupancy and destination pickups and dropoffs to set the trip occupancy and origin pickups & dropoffs accordingly.
					int lastTripPassengers=0;
					TNCVehicleTrip lastTrip = null;
					if(newVehicleTrips.size()==0 && existingVehicleTrips.size()>0){
						lastTrip = existingVehicleTrips.get(existingVehicleTrips.size()-1);
					}else if(newVehicleTrips.size()>0){
						lastTrip = newVehicleTrips.get(newVehicleTrips.size()-1);
					}
					//set the origin and other values for the trip
					if(lastTrip!=null){
						lastTripPassengers = lastTrip.getPassengers();
						ArrayList<String> dropoffsAtDestinationOfLastTrip = lastTrip.getDropoffIdsAtDestination();
						ArrayList<String> pickupsAtDestinationOfLastTrip = lastTrip.getPickupIdsAtDestination();
						
						//add pickups and dropoffs at origin from last trip
						trip.addDropoffIdsAtOrigin(dropoffsAtDestinationOfLastTrip);	
						trip.addPickupIdsAtOrigin(pickupsAtDestinationOfLastTrip);	
						
						//add pickup and dropoffs at origin of this trip to destination of last trip. (commenting to test write problem)
						//lastTrip.addDropoffIdsAtDestination(trip.getDropoffIdsAtOrigin());
						//lastTrip.addPickupIdsAtDestination(trip.getPickupIdsAtOrigin());
						
						
	
					}
					
					int passengers = lastTripPassengers + trip.getNumberOfPickupsAtOrigin() - trip.getNumberOfDropoffsAtOrigin();
					trip.setPassengers(passengers);
					trip.setDestinationMaz(maz);
					trip.setDestinationTaz((short) tazs[i]);

					//measure time from first trip to destination (current) or track time in tNCVehicle explicitly for each trip?
					float time = transportCostManager.getTime(skimPeriod, firstOriginTaz, trip.getDestinationTaz());
					float periods = time/(float)minutesPerSimulationPeriod;
					int endPeriod = (int) Math.floor(simulationPeriod + periods); //currently measuring time as simulation period + straight time to dest.
					trip.setEndPeriod(endPeriod);

					//measure distance for current trip origin and destination
					float distance = transportCostManager.getDistance(skimPeriod, trip.getOriginTaz(), trip.getDestinationTaz());
					trip.setDistance(distance);
					tNCVehicle.setDistanceSinceRefuel(tNCVehicle.getDistanceSinceRefuel()+distance);
					
					if(tNCVehicle.getId()==vehicleDebug){
						logger.info("Vehicle ID "+tNCVehicle.getId()+" now has vehicle trip ID "+trip.getId());
						trip.writeTrace();
					}

					newVehicleTrips.add(trip);
					
					//more trips to go!
					if(tNCVehicle.getPersonTripList().size()>0){
						++totalVehicleTrips;
						TNCVehicleTrip newTrip = new TNCVehicleTrip(tNCVehicle,totalVehicleTrips);
						newTrip.setOriginMaz(maz);
						newTrip.setOriginTaz((short) tazs[i]);
						newTrip.setStartPeriod(trip.getEndPeriod());
						trip = newTrip;
					}
					
				} //end mazs

			} //end tazs

			//add tNCVehicle trips to tNCVehicle
			tNCVehicle.addVehicleTrips(newVehicleTrips);
	
			//add tNCVehicle to active vehicles
			activeVehicleList.add(tNCVehicle);
			
			//track tNCVehicle in vehicles to remove from route list.
			vehiclesToRemove.add(tNCVehicle);
		} //end vehicles
		
		//Remove vehicles that have been routed
		vehiclesToRouteList.removeAll(vehiclesToRemove);
	}
	
	/**
	 * Free vehicles from the active tNCVehicle list and put them in the free tNCVehicle list if
	 * the last trip in the tNCVehicle ends in the current simulation period.
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
		ArrayList<TNCVehicle> vehiclesToRemove = new ArrayList<TNCVehicle>();
		// go through active vehicles (vehicles that have been routed and are picking up/dropping off passengers)
		for(int i = 0; i< activeVehicleList.size();++i){
			TNCVehicle tNCVehicle = activeVehicleList.get(i);
			
			ArrayList<TNCVehicleTrip> trips = tNCVehicle.getVehicleTrips();
			
			//this tNCVehicle has no trips; why is it in the active tNCVehicle list??
			if(trips.size()==0){
				logger.error("Vehicle ID "+tNCVehicle.getId()+" has no vehicle trips but is in active vehicle list");
				continue;
			}
			
			//Find out when the last dropoff occurs (the end period of the last trip)
			TNCVehicleTrip lastTrip = trips.get(trips.size()-1);
			if(lastTrip.endPeriod==simulationPeriod){
				int taz = lastTrip.getDestinationTaz();
				vehiclesToRemove.add(tNCVehicle);
				++freedVehicles;
				
				//store the empty tNCVehicle in the last dropoff location (the last trip destination TAZ)
				if(emptyVehicleList[taz]==null)
					emptyVehicleList[taz]= new ArrayList<TNCVehicle>();
				
				emptyVehicleList[taz].add(tNCVehicle);
			}
		}
		activeVehicleList.removeAll(vehiclesToRemove);
		logger.info("Freed "+freedVehicles+" vehicles from active tNCVehicle list");
		logger.info("There are now "+activeVehicleList.size()+" vehicles in the active tNCVehicle list");
	}
	
	
	/**
	 * First find vehicles that need to refuel, generate a trip to the closest refueling station, then 
	 * remove them from the empty tNCVehicle list, and add them to the refueling tNCVehicle list.
	 * Next, for all refueling vehicles, check if they are done refueling, and if so, remove them
	 * from the refueling list and add them to the empty tNCVehicle list.
	 * 
	 * @param skimPeriod
	 * @param simulationPeriod
	 */
	public void checkForRefuelingVehicles(int skimPeriod, int simulationPeriod) {
		
		//iterate through zones
        for(int i = 1; i <= maxTaz; ++ i){
        	if(emptyVehicleList[i]==null)
        		continue;
        	
    		//track the vehicles to remove
    		ArrayList<TNCVehicle> vehiclesToRemove = new ArrayList<TNCVehicle>();
        	
        	//iterate through vehicles in this zone
        	for(TNCVehicle tNCVehicle : emptyVehicleList[i]) {
        		
        		//if distance since refueling is greater than max, generate a new trip to the closest refueling station.
        		if(tNCVehicle.getDistanceSinceRefuel()>=maxDistanceBeforeRefuel) {
        			
        			ArrayList<TNCVehicleTrip> currentTrips =  tNCVehicle.getVehicleTrips();
        			TNCVehicleTrip lastTrip = currentTrips.get(currentTrips.size()-1);
        			
        			TNCVehicleTrip trip = new TNCVehicleTrip(tNCVehicle,totalVehicleTrips+1);
        			trip.setStartPeriod(lastTrip.endPeriod);
        			trip.setOriginMaz(lastTrip.destinationMaz);
        			trip.setOriginTaz(lastTrip.originTaz);
        			trip.setPassengers(0);
        			trip.setOriginPurpose(lastTrip.destinationPurpose);
        			trip.setDestinationPurpose(Purpose.REFUEL);
        			
        			int refeulingMaz = closestMazWithRefeulingStation[trip.getOriginMaz()];
        			trip.setDestinationMaz(refeulingMaz);
        			trip.setDestinationTaz((short) mazManager.getTaz(refeulingMaz));
        			float time = transportCostManager.getTime(skimPeriod, trip.getOriginTaz(), trip.getDestinationTaz() );
					float distance = transportCostManager.getDistance(skimPeriod, trip.getOriginTaz(), trip.getDestinationTaz());
					float periods = time/(float)minutesPerSimulationPeriod;
					int endPeriod = (int) Math.floor(simulationPeriod + periods);
					trip.setEndPeriod(endPeriod);
					trip.setDistance(distance);
					
					//add the tNCVehicle trip to the tNCVehicle
					tNCVehicle.addVehicleTrip(trip);
					
					vehiclesToRemove.add(tNCVehicle);
					
					if(tNCVehicle.getId()==vehicleDebug) {
						logger.info("*************");
						logger.info("Vehicle ID "+tNCVehicle.getId()+" refueling trip generated");
						logger.info("From origin MAZ "+trip.getOriginMaz()+" to refueling MAZ "+trip.getDestinationMaz()+" in TAZ "+trip.getDestinationTaz());
						logger.info("Start period "+trip.getStartPeriod()+" end period  "+trip.getEndPeriod());
						logger.info("*************");
						
					}

        		}
        			
        	}
    		//remove all the refueling vehicles from the empty tNCVehicle list
    		emptyVehicleList[i].removeAll(vehiclesToRemove);
    		
    		//add them to the refueling tNCVehicle list
    		refuelingVehicleList.addAll(vehiclesToRemove);
        }
        
 		//track the vehicles to remove
		ArrayList<TNCVehicle> vehiclesToRemove = new ArrayList<TNCVehicle>();

        //iterate through the refueling vehicles
        for(TNCVehicle tNCVehicle : refuelingVehicleList ) {
        	
			ArrayList<TNCVehicleTrip> currentTrips =  tNCVehicle.getVehicleTrips();
			TNCVehicleTrip lastTrip = currentTrips.get(currentTrips.size()-1);
			
			//trip is not refueling
			if(lastTrip.destinationPurpose!=Purpose.REFUEL)
				continue;
			
			//trip is still en-route to refueling
        	if(lastTrip.endPeriod>simulationPeriod)
        		continue;
        	
        	
        	//if its been refueling for appropriate periods, add to empty tNCVehicle list and remove it from the refueling tNCVehicle list
        	if(tNCVehicle.periodsRefueling==periodsRequiredForRefuel) {
        		tNCVehicle.setDistanceSinceRefuel(0);
        		vehiclesToRemove.add(tNCVehicle);
        		short refuelTaz = lastTrip.destinationTaz;

        		if(emptyVehicleList[refuelTaz] == null)
        			emptyVehicleList[refuelTaz] = new ArrayList<TNCVehicle>();

        		emptyVehicleList[refuelTaz].add(tNCVehicle);
        		
				if(tNCVehicle.getId()==vehicleDebug) {
					logger.info("*************");
					logger.info("Vehicle ID "+tNCVehicle.getId()+" has completed refueling in period "+simulationPeriod);
					logger.info("Distance to refuel is reset to 0 and vehicle added to empty vehicle list in TAZ "+refuelTaz);
					logger.info("*************");
				}

        		
        	// else increment up the number of periods refueling	
        	}else  {
        		tNCVehicle.setPeriodsRefueling(tNCVehicle.getPeriodsRefueling()+1);
				if(tNCVehicle.getId()==vehicleDebug) {
					logger.info("*************");
					logger.info("Vehicle ID "+tNCVehicle.getId()+" is still refueling in period "+simulationPeriod);
					logger.info("*************");
				}

        	}
        }
        
        refuelingVehicleList.removeAll(vehiclesToRemove);
	}
	
	
	/**
	 * This method writes tNCVehicle trips to the output file.
	 * 
	 */
	public void writeVehicleTrips(float sampleRate){
		
		logger.info("Writing tNCVehicle trips to file " + vehicleTripOutputFile);
        PrintWriter printWriter = null;
        try
        {
        	printWriter = new PrintWriter(new BufferedWriter(new FileWriter(vehicleTripOutputFile)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + vehicleTripOutputFile + " for writing\n");
            throw new RuntimeException();
        }
        
        TNCVehicleTrip.printHeader(printWriter);
        
        //count the total empty vehicles
        int totalEmptyVehicles =0;
        for(int i = 1; i <= maxTaz; ++ i){
        	if(emptyVehicleList[i]==null)
        		continue;
        	
        	totalEmptyVehicles+=emptyVehicleList[i].size();
        }
        logger.info("Writing "+totalEmptyVehicles+" total vehicles to file");
        
        for(int i = 1; i <= maxTaz; ++ i){
        	if(emptyVehicleList[i]==null)
        		continue;
        	
        	if(emptyVehicleList[i].size()==0)
        		continue;
        	for(TNCVehicle tNCVehicle : emptyVehicleList[i] ){
    			if(tNCVehicle.getId()==vehicleDebug) {
    				logger.info("Writing "+tNCVehicle.getVehicleTrips().size()+" vehicle trips for vehicle ID "+tNCVehicle.getId());
    			}

        		for(TNCVehicleTrip tNCVehicleTrip : tNCVehicle.getVehicleTrips()){
        			
         			tNCVehicleTrip.printData(printWriter);
        			
        			//save the data in the trip matrix
        			int startPeriod = tNCVehicleTrip.getStartPeriod();
        			int skimPeriod = skimPeriodLookup[startPeriod]; 
        			int origTaz = tNCVehicleTrip.getOriginTaz();
        			int destTaz =  tNCVehicleTrip.getDestinationTaz();
        			int occ = Math.min(tNCVehicleTrip.getPassengers(),3);
        			
        			float existingTrips = TNCTripMatrix[skimPeriod][occ].getValueAt(origTaz,destTaz);
        			TNCTripMatrix[skimPeriod][occ].setValueAt(origTaz,destTaz,existingTrips + (1*(1/sampleRate)));
        			
        			
        		}
        	}
        }
        printWriter.close();
	}
	
    /**
     * Get the output trip table file names from the properties file, and write
     * trip tables for all modes for the given time period.
     * 
     * @param period
     *            Time period, which will be used to find the period time string
     *            to append to each trip table matrix file
     */
    public void writeTripTable(MatrixDataServerRmi ms)
    {

        String directory = Util.getStringValueFromPropertyMap(propertyMap, "scenario.path");
        String matrixTypeName = Util.getStringValueFromPropertyMap(propertyMap, "Results.MatrixType");
        MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

        
        for(int i =0;i< transportCostManager.NUM_PERIODS;++i) {
        String fileName = directory + Util.getStringValueFromPropertyMap(propertyMap, VEHICLETRIP_OUTPUT_MATRIX_PROPERTY) + "_"+transportCostManager.PERIODS[i]+".omx";
       	try{
        		//Delete the file if it exists
        		File f = new File(fileName);
        		if(f.exists()){
        			logger.info("Deleting existing trip file: "+fileName);
        			f.delete();
        		}

        		if (ms != null) 
        			ms.writeMatrixFile(fileName, TNCTripMatrix[i], mt);
        		else 
        			writeMatrixFile(fileName, TNCTripMatrix[i]);
       			} catch (Exception e){
       				logger.error("exception caught writing " + mt.toString() + " matrix file = "
                       + fileName, e);
       				throw new RuntimeException();
       			}
        }

    }
    /**
     * Utility method to write a set of matrices to disk.
     * 
     * @param fileName
     *            The file name to write to.
     * @param m
     *            An array of matrices
     */
    private void writeMatrixFile(String fileName, Matrix[] m)
    {

        // auto trips
        MatrixWriter writer = MatrixWriter.createWriter(fileName);
        String[] names = new String[m.length];

        for (int i = 0; i < m.length; i++)
        {
            names[i] = m[i].getName();
            logger.info(m[i].getName() + " has " + m[i].getRowCount() + " rows, "
                    + m[i].getColumnCount() + " cols, and a total of " + m[i].getSum());
        }

        writer.writeMatrices(names, m);
    }

	/**
	 * Relate simulation periods to skim periods.
	 * 
	 */
	public void calculateSkimPeriods(){
		
		skimPeriodLookup = new byte[numberOfSimulationPeriods];
		int numberSkimPeriods = ModelStructure.SKIM_PERIOD_INDICES.length;
		int[] endSkimPeriod = new int[numberSkimPeriods];
		
		int lastPeriodEnd = 0;
		int lastEndSkimPeriod = 0;
		for(int skimPeriod = 0;skimPeriod<numberSkimPeriods;++skimPeriod){
			int periodLengthInMinutes = 30 * (ModelStructure.PERIOD_ENDS[skimPeriod] - lastPeriodEnd);
			lastPeriodEnd = ModelStructure.PERIOD_ENDS[skimPeriod];
			if(skimPeriod==0)
				periodLengthInMinutes += 90;
			
			if(skimPeriod==4)
				periodLengthInMinutes += 150;
				
			endSkimPeriod[skimPeriod] = periodLengthInMinutes/minutesPerSimulationPeriod + lastEndSkimPeriod;
			lastEndSkimPeriod = endSkimPeriod[skimPeriod];
			
		//	logger.info("Last simulation period for skim period "+skimPeriod+ " is "+endSkimPeriod[skimPeriod]);
			
		}
		
		//calculate lookup array
		for(int period = 0; period < numberOfSimulationPeriods;++period){
			
			for(int skimPeriod=(numberSkimPeriods-1);skimPeriod>=0;--skimPeriod){
				if(period<endSkimPeriod[skimPeriod]) 
					skimPeriodLookup[period]= (byte) skimPeriod;
			}
		//	logger.info("Simulation period "+period+" is in skim period "+skimPeriodLookup[period]);
		}
		
	}
}
