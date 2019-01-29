package org.sandag.abm.maas;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.math.MersenneTwister;

public class AVFleetModel {

	private static final Logger logger = Logger.getLogger(AVFleetModel.class);
    private HashMap<String, String> propertyMap = null;
	TransportCostManager transportCostManager;		//manages transport costs!
	PersonTripManager personTripManager;			//manages person trips!
	VehicleManager vehicleManager;					//manages vehicles!
    
	private int iteration;
	private int minutesPerSimulationPeriod;
	private int numberOfSimulationPeriods;
    private MersenneTwister       random;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    private byte maxSharedTNCPassengers;

	
	
    private static final String MAX_PICKUP_DISTANCE_PROPERTY = "TNC.shared.maxDistanceForPickup";
    private static final String MAX_PICKUP_DIVERSON_TIME_PROPERTY = "TNC.shared.maxDiversionTimeForPickup";
    private static final String MINUTES_PER_SIMULATION_PERIOD_PROPERTY = "TNC.shared.minutesPerSimulationPeriod";
    private static final String MAX_SHARED_TNC_PASSENGERS_PROPERTY = "TNC.shared.maxPassengers";
    private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
    
    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param iteration
     */
	public AVFleetModel(HashMap<String, String> propertyMap, int iteration){
		this.propertyMap = propertyMap;
		this.iteration = iteration;
	}
	
	/**
	 * Initialize all the data members.
	 * 
	 */
	public void initialize(){
	   
		//managers for MAZ and TAZ data
		mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);

	    //some controlling properties
		float maxPickupDistance = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DISTANCE_PROPERTY);
		float maxDiversionTime = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DIVERSON_TIME_PROPERTY);
		maxSharedTNCPassengers = (byte) Util.getIntegerValueFromPropertyMap(propertyMap, MAX_SHARED_TNC_PASSENGERS_PROPERTY);
		
		//set the length of a simulation period
		int minutesPerSimulationPeriod = Util.getIntegerValueFromPropertyMap(propertyMap, MINUTES_PER_SIMULATION_PERIOD_PROPERTY);
		numberOfSimulationPeriods = ((24*60)/minutesPerSimulationPeriod);
		logger.info("Calculated "+numberOfSimulationPeriods+" simulation periods using a period length of "+minutesPerSimulationPeriod+" minutes");
		
		//create a new transport cost manager and create data structures
		transportCostManager = new TransportCostManager(propertyMap,maxDiversionTime,maxPickupDistance);
		transportCostManager.initialize();
		transportCostManager.calculateTazsByTimeFromOrigin();
		
		//create a person trip manager, read person trips
		personTripManager = new PersonTripManager(propertyMap, iteration);
		personTripManager.initialize();
		personTripManager.groupPersonTripsByDepartureTimePeriodAndOrigin(minutesPerSimulationPeriod);
		
		//create a vehicle manager
		vehicleManager = new VehicleManager(propertyMap, transportCostManager, maxSharedTNCPassengers);
		vehicleManager.initialize();
		
		//seed the random number generator so that results can be replicated if desired.
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
        random = new MersenneTwister(seed + 4292);

	}
	
	
	public void runModel(){
		
		
		//iterate through simulation periods
		for(int simulationPeriod = 0;simulationPeriod < numberOfSimulationPeriods; ++simulationPeriod){
			
			//need a do loop here. while there are still trips to simulate, do. Add check on number of 
			//remaining trips to simulate departing in a certain period to personTripManager, and check it.
			
			//get a person trip at random from the person trip manager for the simulation period
			ArrayList<PersonTrip> personTripArray  = personTripManager.getPersonTripsDepartingInTimePeriod(simulationPeriod);
			double rnum = random.nextDouble();
			PersonTrip personTrip = personTripManager.samplePersonTripFromArrayList(personTripArray, rnum);
			
			int origTaz = mgraManager.getTaz(personTrip.getOriginMaz());
			int destTaz = mgraManager.getTaz(personTrip.getDestinationMaz());
			
			//create a vehicle for this person
			Vehicle vehicle = vehicleManager.getClosestEmptyVehicle(simulationPeriod, origTaz);
			vehicle.addPersonTrip(personTrip);
			
			//get list of zones within max time deviation
			int[] maxDiversionTimeTazArray = transportCostManager.getZonesWithinMaxDiversionTime(simulationPeriod, origTaz, destTaz);
			
			//iterate through array and find if there are any other travelers in this period 
			// who need a ride and whose origin & destination is within the max diversion time
			for(int i = 0; i < maxDiversionTimeTazArray.length; ++i){
				
				int taz = maxDiversionTimeTazArray[i];
				
				//iterate through MAZs in this TAZ
				int[] mazArray = tazManager.getMgraArray(taz);
				for(int maz : mazArray){
					
					//if vehicle is full, break.
					if(vehicle.getNumberPassengers()>=vehicle.getMaxPassengers())
						break;

					//get a list of people leaving in this period from this MAZ
					ArrayList<PersonTrip> potentialTrips = personTripManager.getPersonTripsByDepartureTimePeriodAndMaz(simulationPeriod, maz);
					
					//if arrayList is empty or null, continue
					if(potentialTrips == null)
						continue;
					if(potentialTrips.isEmpty())
						continue;

					//iterate through people in this list
					for(PersonTrip trip : potentialTrips){
						
						int tripDestinationMaz = trip.getDestinationMaz();
						int tripDestinationTaz = mgraManager.getTaz(tripDestinationMaz);
						
						boolean destinationIsWithinMax = transportCostManager.stopZoneIsWithinMaxDiversionTime(simulationPeriod, origTaz, destTaz, tripDestinationTaz);
						
						if(destinationIsWithinMax)
							vehicle.addPersonTrip(trip);

						//if vehicle is full, break.
						if(vehicle.getNumberPassengers()>=vehicle.getMaxPassengers())
							break;
						
					}
					
				}
			
				//todo: optimize vehicle pickups and dropoffs.
				
			}
			
			
			
			
		}
		
		
		
		
	}
	
}
