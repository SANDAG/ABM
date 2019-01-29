package org.sandag.abm.maas;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.math.MersenneTwister;

public class VehicleManager {
	
	private static final Logger logger = Logger.getLogger(VehicleManager.class);
    private HashMap<String, String> propertyMap = null;
    ArrayList<Vehicle>[] emptyVehicleList; //by taz
    ArrayList<Vehicle> vehicleList;
    private MersenneTwister       random;
    private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
    private TazDataManager                tazManager;
    private int maxTaz;
    private byte maxPassengers;
    TransportCostManager transportCostManager;
    
 
    /**
     * 
     * @param propertyMap
     * @param transportCostManager
     */
    public VehicleManager(HashMap<String, String> propertyMap, TransportCostManager transportCostManager, byte maxPassengers){
		
		this.propertyMap = propertyMap;
		this.transportCostManager = transportCostManager;
		this.maxPassengers = maxPassengers;
		
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(){
		
       int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
       random = new MersenneTwister(seed + 234324);
       
       tazManager = TazDataManager.getInstance();
       maxTaz = tazManager.getMaxTaz();
       
       emptyVehicleList = new ArrayList[maxTaz+1];
       vehicleList = new ArrayList<Vehicle>();


	}
	
	/**
	 * This method gets the closest empty vehicle to the requested zone, and removes it from the empty vehicle list.
	 * If there is no vehicle within the maximum distance threshold, it generates
	 * a new vehicle and returns it.
	 * 
	 * @param taz
	 * @return
	 */
	public Vehicle getClosestEmptyVehicle(int departurePeriod, int departureTaz ){
		
		int[] sortedTazs = transportCostManager.getZoneNumbersSortedByTime(departurePeriod, departureTaz);
		if(sortedTazs == null){
			
			logger.info("There are no TAZs within the maximum distance from TAZ: "+departureTaz+"; please check and/or increase maximum distance used for pickup property");
			return generateVehicle(departurePeriod, departureTaz);
		}
			
		//iterate through zones that have already been ordered by distance from the departure TAZ.
		for(int i = 0; i < sortedTazs.length; ++ i){
			
			int taz = sortedTazs[i];
			
			//if there are empty vehicles in the TAZ, return one at random
			if(emptyVehicleList[taz] != null){
				
				Vehicle vehicle = null;
				double rnum = random.nextDouble();
				vehicle = getRandomVehicleFromList(emptyVehicleList[taz], rnum);
				return vehicle;
			}
			
		}
		
		//Iterated through all TAZs, could not find an empty vehicle. Return a new vehicle.
		return generateVehicle(departurePeriod, departureTaz);
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
	private Vehicle generateVehicle(int period, int taz){
		
		Vehicle vehicle = new Vehicle(maxPassengers);
		vehicleList.add(vehicle);
		return vehicle;
		
	}

	/**
	 * Add empty vehicle to the empty vehicle list.
	 * 
	 * @param vehicle
	 * @param taz
	 */
	public void storeEmptyVehicle(Vehicle vehicle, int taz){
		
		emptyVehicleList[taz].add(vehicle);
		
	}
}
