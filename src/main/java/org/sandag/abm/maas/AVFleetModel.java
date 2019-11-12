package org.sandag.abm.maas;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

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
    
    private byte[] skimPeriodLookup; //an array indexed by number of periods that corresponds to the skim period

    private boolean routeIntrazonal;
	
	
    private static final String MAX_PICKUP_DISTANCE_PROPERTY = "Maas.RoutingModel.maxDistanceForPickup";
    private static final String MAX_PICKUP_DIVERSON_TIME_PROPERTY = "Maas.RoutingModel.maxDiversionTimeForPickup";
    private static final String MINUTES_PER_SIMULATION_PERIOD_PROPERTY = "Maas.RoutingModel.minutesPerSimulationPeriod";
    private static final String MAX_SHARED_TNC_PASSENGERS_PROPERTY = "Maas.RoutingModel.maxPassengers";
    private static final String ROUTE_INTRAZONAL_PROPERTY = "Maas.RoutingModel.routeIntrazonal";
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
		
    	startMatrixServer(propertyMap);
	   
		//managers for MAZ and TAZ data
		mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);

	    //some controlling properties
		float maxPickupDistance = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DISTANCE_PROPERTY);
		float maxDiversionTime = Util.getFloatValueFromPropertyMap(propertyMap, MAX_PICKUP_DIVERSON_TIME_PROPERTY);
		maxSharedTNCPassengers = (byte) Util.getIntegerValueFromPropertyMap(propertyMap, MAX_SHARED_TNC_PASSENGERS_PROPERTY);
		routeIntrazonal = Util.getBooleanValueFromPropertyMap(propertyMap, ROUTE_INTRAZONAL_PROPERTY);
		
		//set the length of a simulation period
		minutesPerSimulationPeriod = Util.getIntegerValueFromPropertyMap(propertyMap, MINUTES_PER_SIMULATION_PERIOD_PROPERTY);
		numberOfSimulationPeriods = ((24*60)/minutesPerSimulationPeriod);
		logger.info("Running "+numberOfSimulationPeriods+" simulation periods using a period length of "+minutesPerSimulationPeriod+" minutes");
		calculateSkimPeriods();
		
		//create a new transport cost manager and create data structures
		transportCostManager = new TransportCostManager(propertyMap,maxDiversionTime,maxPickupDistance);
		transportCostManager.initialize();
		transportCostManager.calculateTazsByTimeFromOrigin();
		
		//create a person trip manager, read person trips
		personTripManager = new PersonTripManager(propertyMap, iteration);
		personTripManager.initialize(minutesPerSimulationPeriod);


		//create a vehicle manager
		vehicleManager = new VehicleManager(propertyMap, transportCostManager, maxSharedTNCPassengers, minutesPerSimulationPeriod);
		vehicleManager.initialize();
		
		//seed the random number generator so that results can be replicated if desired.
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);
        random = new MersenneTwister(seed + 4292);

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
			
			logger.info("Last simulation period for skim period "+skimPeriod+ " is "+endSkimPeriod[skimPeriod]);
			
		}
		
		//calculate lookup array
		for(int period = 0; period < numberOfSimulationPeriods;++period){
			
			for(int skimPeriod=(numberSkimPeriods-1);skimPeriod>=0;--skimPeriod){
				if(period<endSkimPeriod[skimPeriod]) 
					skimPeriodLookup[period]= (byte) skimPeriod;
			}
			logger.info("Simulation period "+period+" is in skim period "+skimPeriodLookup[period]);
		}
		
	}
	
	public void runModel(){
		
		int simulatedPersonTrips=0;
		int intraZonalTrips=0;
		
		//iterate through simulation periods
		for(int simulationPeriod = 0;simulationPeriod < numberOfSimulationPeriods; ++simulationPeriod){
	
			logger.info("Simulating period "+simulationPeriod);
			
			int skimPeriod = skimPeriodLookup[simulationPeriod];
			
			while(personTripManager.morePersonTripsInSimulationPeriod(simulationPeriod)){
				
				double rnum = random.nextDouble();
				PersonTrip personTrip = personTripManager.samplePersonTrip(simulationPeriod, rnum);
			
				int origMaz = personTrip.getPickupMaz();
				int destMaz = personTrip.getDropoffMaz();
				int origTaz = mgraManager.getTaz(origMaz);
				int destTaz = mgraManager.getTaz(destMaz);
				if((origTaz==destTaz) && routeIntrazonal==false){
					++intraZonalTrips;
					continue;
				}
					
			
				//create a vehicle for this person
				Vehicle vehicle = vehicleManager.getClosestEmptyVehicle(skimPeriod, simulationPeriod, origMaz);
				vehicle.addPersonTrip(personTrip);
				++simulatedPersonTrips;
		
				if(simulatedPersonTrips<10||( ( simulatedPersonTrips % 1000) == 0) )
					logger.info("...Total simulated person trips = "+simulatedPersonTrips);
				
				if(!personTrip.isRideSharer())
					continue;

				//get list of zones within max time deviation
				short[] maxDiversionTimeTazArray = transportCostManager.getZonesWithinMaxDiversionTime(skimPeriod, origTaz, destTaz);
			
				if(maxDiversionTimeTazArray==null)
					continue;
				
				//iterate through array and find if there are any other travelers in this period 
				// who need a ride and whose origin & destination is within the max diversion time
				for(int i = 0; i < maxDiversionTimeTazArray.length; ++i){
				
					//if vehicle is full, break.
					if(vehicle.getNumberPassengers()>=vehicle.getMaxPassengers())
						break;

					int taz = maxDiversionTimeTazArray[i];
				
					//iterate through MAZs in this TAZ
					int[] mazArray = tazManager.getMgraArray(taz);
					
					if(mazArray==null)
						continue;
					
					for(int maz : mazArray){
					
						//if vehicle is full, break.
						if(vehicle.getNumberPassengers()>=vehicle.getMaxPassengers())
							break;

						//if(!personTripManager.morePersonTripsInSimulationPeriodAndMaz(simulationPeriod,maz))
						//	continue;
						
						//get a list of people leaving in this period from this MAZ
						ArrayList<PersonTrip> potentialTrips = personTripManager.getPersonTripsByDepartureTimePeriodAndMaz(simulationPeriod, maz);
					    ArrayList<PersonTrip> tripsToRemove = new ArrayList<PersonTrip>();
					    
						if(potentialTrips==null)
							continue;
						
						//iterate through people in this list
						for(PersonTrip trip : potentialTrips){
							
							if(!trip.isRideSharer())
								continue;
						
							int tripOriginMaz = trip.getPickupMaz();
							int tripOriginTaz = mgraManager.getTaz(tripOriginMaz);
							
							if((tripOriginMaz==tripOriginTaz) && routeIntrazonal==false){
								++intraZonalTrips;
								continue;
							}

							int tripDestinationMaz = trip.getDropoffMaz();
							int tripDestinationTaz = mgraManager.getTaz(tripDestinationMaz);
						
							//check if destination is within maximum diversion time
							boolean destinationIsWithinMax = transportCostManager.stopZoneIsWithinMaxDiversionTime(skimPeriod, origTaz, destTaz, tripDestinationTaz);
						    if(!destinationIsWithinMax)
						    	continue;
							
						    //check if origin is closer to first trip origin taz than destination; this makes sure that pickup is before dropoff
						    //in other words, passenger is traveling in the right direction!
							float firstPickupToPassengerPickupTime = transportCostManager.getTime(skimPeriod, origTaz, tripOriginTaz);
							float firstPickupToPassengerDropoffTime = transportCostManager.getTime(skimPeriod, origTaz, tripDestinationTaz);
							
							boolean tripIsInDirection = firstPickupToPassengerPickupTime < firstPickupToPassengerDropoffTime ? true : false;
							
							if(destinationIsWithinMax && tripIsInDirection){
								vehicle.addPersonTrip(trip);
								tripsToRemove.add(trip);
								++simulatedPersonTrips;
							}
							//if vehicle is full, break.
							if(vehicle.getNumberPassengers()>=vehicle.getMaxPassengers())
								break;
						
						} //no more potential trips in TAZ
						
						//remove trips from arraylist that have been routed (outside potentialTrips loop to avoid ConcurrentModificationException)
						for(PersonTrip trip: tripsToRemove)
							personTripManager.removePersonTrip(trip, simulationPeriod);
					
					} //no more MAZs within max diversion for first passenger
				} //no more TAZs within max diversion for first passenger
			
				vehicleManager.addVehicleToRoute(vehicle);
				
			} //until no more person trips in simulation period
			
			vehicleManager.routeActiveVehicles(skimPeriod, simulationPeriod, transportCostManager);
			vehicleManager.freeVehicles(simulationPeriod);
			vehicleManager.checkForRefuelingVehicles(skimPeriod, simulationPeriod);
			
			
			logger.info("...Total simulated vehicles period "+simulationPeriod+" = "+vehicleManager.getTotalVehicles());
			logger.info("...Total simulated person trips period "+simulationPeriod+" = "+simulatedPersonTrips);
			if(routeIntrazonal==false)
				logger.info("...Total skipped intra-zonal person trips period "+simulationPeriod+" = "+intraZonalTrips);

		} //end simulation period
		
		//write vehicle trips
		vehicleManager.writeVehicleTrips();
	}
	
	/**
	 * Start a matrix server
	 * 
	 * @param properties
	 */
	private void startMatrixServer(HashMap<String, String> properties) {
	        String serverAddress = (String) properties.get("RunModel.MatrixServerAddress");
	        int serverPort = new Integer((String) properties.get("RunModel.MatrixServerPort"));
	        logger.info("connecting to matrix server " + serverAddress + ":" + serverPort);

	        try{

	            MatrixDataManager mdm = MatrixDataManager.getInstance();
	            MatrixDataServerIf ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
	            ms.testRemote(Thread.currentThread().getName());
	            mdm.setMatrixDataServerObject(ms);

	        } catch (Exception e) {
	            logger.error("could not connect to matrix server", e);
	            throw new RuntimeException(e);

	        }

	    }

	  /**
	 * Startup a connection to the matrix manager.
	 * 
	 * @param serverAddress
	 * @param serverPort
	 * @param mt
	 * @return
	 */
	private MatrixDataServerRmi startMatrixServerProcess(String serverAddress, int serverPort,
	            MatrixType mt)
	    {

	        String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

	        MatrixDataServerRmi matrixServer = new MatrixDataServerRmi(serverAddress, serverPort,
	                MatrixDataServer.MATRIX_DATA_SERVER_NAME);

	        try
	        {
	            // create the concrete data server object
	            matrixServer.start32BitMatrixIoServer(mt);
	        } catch (RuntimeException e)
	        {
	            matrixServer.stop32BitMatrixIoServer();
	            logger.error(
	                    "RuntimeException caught making remote method call to start 32 bit mitrix in remote MatrixDataServer.",
	                    e);
	        }

	        // bind this concrete object with the cajo library objects for managing
	        // RMI
	        try
	        {
	            Remote.config(serverAddress, serverPort, null, 0);
	        } catch (Exception e)
	        {
	            logger.error(String.format(
	                    "UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.",
	                    serverAddress, serverPort), e);
	            matrixServer.stop32BitMatrixIoServer();
	            throw new RuntimeException();
	        }

	        try
	        {
	            ItemServer.bind(matrixServer, className);
	        } catch (RemoteException e)
	        {
	            logger.error(String.format(
	                    "RemoteException. serverAddress = %s, serverPort = %d -- exiting.",
	                    serverAddress, serverPort), e);
	            matrixServer.stop32BitMatrixIoServer();
	            throw new RuntimeException();
	        }

	        return matrixServer;

	    }

	/**
	 * Main run method
	 * @param args
	 */
	public static void main(String[] args) {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("AV Fleet Simulation Program using CT-RAMP version ",
                CtrampApplication.VERSION));

        int iteration=0;
        
        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else {
        	propertiesFile = args[0];

	        for (int i = 1; i < args.length; ++i)
	        {
	            if (args[i].equalsIgnoreCase("-iteration"))
	            {
	                iteration = Integer.valueOf(args[i + 1]);
	            }
	           
	        }
        }
        
        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        AVFleetModel fleetModel = new AVFleetModel(pMap, iteration);
        fleetModel.initialize();
        fleetModel.runModel();

        
	}
	

}
