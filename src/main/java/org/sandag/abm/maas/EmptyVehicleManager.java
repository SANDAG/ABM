package org.sandag.abm.maas;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.math.MersenneTwister;

public class EmptyVehicleManager {
	
	private static final Logger logger = Logger.getLogger(EmptyVehicleManager.class);
    private HashMap<String, String> propertyMap = null;
    ArrayList<Vehicle>[] vehicleList; //by taz
    private MersenneTwister       random;
    private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
    private TazDataManager                tazManager;
    private int maxTaz;
    TransportCostManager transportCostManager;
    
 
    /**
     * 
     * @param propertyMap
     * @param transportCostManager
     */
    public EmptyVehicleManager(HashMap<String, String> propertyMap, TransportCostManager transportCostManager){
		
		this.propertyMap = propertyMap;
		this.transportCostManager = transportCostManager;
		
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(){
		
       int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
       random = new MersenneTwister(seed + 234324);
       
       tazManager = TazDataManager.getInstance();
       maxTaz = tazManager.getMaxTaz();
       
       vehicleList = new ArrayList[maxTaz+1];


	}
	
	/**
	 * This method gets the closest vehicle to the requested zone, and removes it from the empty vehicle list.
	 * 
	 * @param taz
	 * @return
	 */
	public Vehicle getClosestVehicle(int departurePeriod, int departureTaz ){
		
		int[] sortedTazs = transportCostManager.getZoneNumbersSortedByTime(departurePeriod, departureTaz);
		if(sortedTazs == null){
			
			logger.info("There are no TAZs within the maximum distance from TAZ: "+departureTaz+"; please check and/or increase maximum distance used for pickup property");
			return new Vehicle();
		}
			
		//iterate through zones that have already been ordered by distance from the departure TAZ.
		for(int i = 0; i < sortedTazs.length; ++ i){
			
			int taz = sortedTazs[i];
			
			//if there are empty vehicles in the TAZ, return one at random
			if(vehicleList[taz] != null){
				double rnum = random.nextDouble();
				Vehicle vehicle = getRandomVehicleFromList(vehicleList[taz], rnum);
				return vehicle;
			}
			
		}
		
		//Iterated through all TAZs, could not find an empty vehicle. Return a new vehicle.
		return new Vehicle();
	}

	/**
	 * Get a vehicle at random from the arraylist of vehicles, remove it from the list, and return it.
	 * 
	 * @param vehicleList
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

}
