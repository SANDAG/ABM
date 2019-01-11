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
	TransportCostManager transportCostManager;
	PersonTripManager personTripManager;
	EmptyVehicleManager emptyVehicleManager;
    
	private int iteration;
	private int minutesPerSimulationPeriod;
	private int numberOfSimulationPeriods;
    private MersenneTwister       random;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;

	
	
    private static final String MAX_PICKUP_DISTANCE_PROPERTY = "TNC.shared.maxDistanceForPickup";
    private static final String MAX_PICKUP_DIVERSON_TIME_PROPERTY = "TNC.shared.maxDiversionTimeForPickup";
    private static final String MINUTES_PER_SIMULATION_PERIOD_PROPERTY = "TNC.shared.minutesPerSimulationPeriod";
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
	   
		mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);

		float maxPickupDistance = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DISTANCE_PROPERTY);
		float maxDiversionTime = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DIVERSON_TIME_PROPERTY);
		
		int minutesPerSimulationPeriod = Util.getIntegerValueFromPropertyMap(propertyMap, MINUTES_PER_SIMULATION_PERIOD_PROPERTY);
		numberOfSimulationPeriods = ((24*60)/minutesPerSimulationPeriod);
		logger.info("Calculated "+numberOfSimulationPeriods+" simulation periods using a period length of "+minutesPerSimulationPeriod+" minutes");
		
		transportCostManager = new TransportCostManager(propertyMap,maxDiversionTime,maxPickupDistance);
		transportCostManager.initialize();
		transportCostManager.calculateTazsByTimeFromOrigin();
		
		personTripManager = new PersonTripManager(propertyMap, iteration);
		personTripManager.initialize();
		personTripManager.groupPersonTripsByDepartureTimePeriodAndOrigin(minutesPerSimulationPeriod);
		
		emptyVehicleManager = new EmptyVehicleManager(propertyMap, transportCostManager);
		emptyVehicleManager.initialize();
		
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
        random = new MersenneTwister(seed + 4292);

	}
	
	
	public void runModel(){
		
		
		//iterate through simulation periods
		for(int simulationPeriod = 0;simulationPeriod < numberOfSimulationPeriods; ++simulationPeriod){
			
			//get a person trip at random from the person trip manager for the simulation period
			ArrayList<PersonTrip> personTripArray  = personTripManager.getPersonTripsDepartingInTimePeriod(simulationPeriod);
			double rnum = random.nextDouble();
			PersonTrip personTrip = personTripManager.samplePersonTripFromArrayList(personTripArray, rnum);
			
			int origTaz = mgraManager.getTaz(personTrip.getOriginMaz());
			int destTaz = mgraManager.getTaz(personTrip.getDestinationMaz());
			
			Vehicle vehicle = emptyVehicleManager.getClosestVehicle(simulationPeriod, origTaz);
			
			//are there any other people departing in this time bin in this maz? If so assume instantaneous vehicle boarding up to max.
			
			
			
		}
		
		
		
		
	}
	
}
