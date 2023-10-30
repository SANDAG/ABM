package org.sandag.abm.maas;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.MicromobilityChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Household;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Person;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Trip;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Vehicle;
import org.sandag.abm.maas.HouseholdAVAllocationManager.VehicleTrip;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.matrix.MatrixType;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.ResourceUtil;

public class HouseholdAVAllocationModel {

	private static final Logger logger = Logger.getLogger(HouseholdAVAllocationModel.class);
    private HashMap<String, String> propertyMap = null;
	HouseholdAVAllocationManager avManager;
    private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
    private static final String MINUTES_PER_SIMULATION_PERIOD_PROPERTY = "Maas.RoutingModel.minutesPerSimulationPeriod";

    private static final String    AV_CONTROL_FILE_TARGET = "Maas.AVAllocation.uec.file";
    private static final String    AV_DATA_SHEET_TARGET   = "Maas.AVAllocation.data.page";
    private static final String    AV_VEHICLECHOICE_SHEET_TARGET  = "Maas.AVAllocation.vehiclechoice.model.page";
    private static final String    AV_PARKINGCHOICE_SHEET_TARGET  = "Maas.AVAllocation.parkingchoice.model.page";
    private static final String    AV_TRIPUTILITY_SHEET_TARGET  = "Maas.AVAllocation.triputility.model.page";
    
    private static final int parkingChoiceStay =1;
    private static final int parkingChoiceRemote=2;
    private static final int parkingChoiceHome=3;
    
    private static final int vehicleChoiceOther=4;
    
    private MersenneTwister       random;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    
    private ChoiceModelApplication parkingChoiceModel;
    private ChoiceModelApplication vehicleChoiceModel;
    private ChoiceModelApplication tripUtilityModel;
    
    private HouseholdAVAllocationModelParkingChoiceDMU parkingChoiceDMU;
    private HouseholdAVAllocationModelVehicleChoiceDMU vehicleChoiceDMU;
    private HouseholdAVAllocationModelTripUtilityDMU tripUtilityDMU;
    
    int vehicleChoiceOffset = 23942345;
    int parkingChoiceOffset =984388432;
    int[] closestRemoteLotToMaz;
     
    
    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param iteration
     */
	public HouseholdAVAllocationModel(HashMap<String, String> propertyMap, MgraDataManager mgraManager,
			TazDataManager tazManager, int[] closestRemoteLotToMaz){
		this.propertyMap = propertyMap;
		this.mgraManager = mgraManager;
		this.tazManager = tazManager;
		this.closestRemoteLotToMaz=closestRemoteLotToMaz;
	}
	
	/**
	 * Initialize all the data members.
	 * 
	 */
	public void initialize(){
		
     
		//seed the random number generator so that results can be replicated if desired.
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, MODEL_SEED_PROPERTY);

        random = new MersenneTwister(seed + 4292);

        //create the model UECs
        // locate the micromobility choice UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String avUecFile = uecFileDirectory + propertyMap.get(AV_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, AV_DATA_SHEET_TARGET);
        int vehicleModelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, AV_VEHICLECHOICE_SHEET_TARGET);
        int parkingModelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, AV_PARKINGCHOICE_SHEET_TARGET);
        int tripModelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, AV_TRIPUTILITY_SHEET_TARGET);

        // create the DMU objects.
        vehicleChoiceDMU = new HouseholdAVAllocationModelVehicleChoiceDMU();
        parkingChoiceDMU = new HouseholdAVAllocationModelParkingChoiceDMU();
        tripUtilityDMU = new HouseholdAVAllocationModelTripUtilityDMU();

        // create the choice model objects
        vehicleChoiceModel= new ChoiceModelApplication(avUecFile, vehicleModelSheet, dataSheet, propertyMap,
                (VariableTable) vehicleChoiceDMU);
        parkingChoiceModel= new ChoiceModelApplication(avUecFile, parkingModelSheet, dataSheet, propertyMap,
                (VariableTable) parkingChoiceDMU);
        tripUtilityModel= new ChoiceModelApplication(avUecFile, tripModelSheet, dataSheet, propertyMap,
                (VariableTable) tripUtilityDMU);
        
     
	}
	
	/**
	 * Run the AV allocation model for all households in HashMap.
	 * 
	 * @param hhMap A hashmap of households
	 * @return the completed hashmap
	 */
	public HashMap<Integer,Household> runModel(HashMap<Integer,Household> hhMap){
		
		//iterate through map
		Set<Integer> keySet = hhMap.keySet();
		for(Integer key : keySet) {
			
			Household hh = hhMap.get(key);
			ArrayList<Trip> trips = hh.getTrips();
			if(trips==null)
				continue;
			if(trips.size()==0)
				continue;
			
	    	ArrayList<Vehicle> hhVehicles = hh.getAutonomousVehicles();

		    //iterate through trips, choose vehicle
		    for(int i =0;i<trips.size();++i) {
		    	Trip trip = trips.get(i);
		    	setVehicleChoiceDMUAttributes(hh,trip);
		    	vehicleChoiceModel.computeUtilities(vehicleChoiceDMU, vehicleChoiceDMU.getDmuIndexValues());
		    	int chosenVehicle = getVehicleChoice(hh, trip);
		    	trip.setVeh_used(chosenVehicle);
		    	
		    	if(chosenVehicle==vehicleChoiceOther)
		    		continue;
		    	
		    	//at this point just add new vehicle trips for each passenger,
		    	//without worrying about the empty trips
		    	Vehicle vehicle = hhVehicles.get(chosenVehicle-1);
		    	ArrayList<VehicleTrip> vehicleTrips = vehicle.getVehicleTrips();
		    	VehicleTrip newVehicleTrip = vehicle.createNewVehicleTrip();
		    	
		    	if(trip.getOrig_purpose().compareToIgnoreCase("Home")==0)
		    		newVehicleTrip.setOriginIsHome(true);
		    	else
		    		newVehicleTrip.setOriginIsHome(false);
		    	
		    	if(trip.getDest_purpose().compareToIgnoreCase("Home")==0)
		    		newVehicleTrip.setDestinationIsHome(true);
		    	else
		    		newVehicleTrip.setDestinationIsHome(false);
		    	
		    	newVehicleTrip.setOrigMaz(trip.getOrig_maz());
		    	newVehicleTrip.setDestMaz(trip.getDest_maz());
		    	newVehicleTrip.setOccupants(trip.getNum_participants());
		    	newVehicleTrip.setPeriod(trip.getStop_period());
		    	newVehicleTrip.setTripServed(trip);
		    	vehicleTrips.add(newVehicleTrip);
		    	
		    	//update the time for which the vehicle will be available if it 
		    	//is an AV, and update the vehicle location
		    	int period = trip.getStop_period();
				if(period>1 && period<40) {
			    	float timeInMinutes = getTravelTime(hh.getId(),trip.getOrig_maz(),trip.getDest_maz(),period);
					int additionalPeriods = (int) (timeInMinutes/30);
					vehicle.setPeriodAvailable(period + additionalPeriods);
				}else {
					vehicle.setPeriodAvailable(period);
				}
				vehicle.setMaz(trip.getDest_maz());
			    if(trip.getDest_purpose().compareToIgnoreCase("Home")==0)
			    	vehicle.setHome(true);
			    else
			    	vehicle.setHome(false);
			    
			    vehicle.setWithPersonId(trip.getPerson_id());

		    }
			
			if(hh.isDebug()) {
				
				logger.info("***********************************************");
				logger.info("AV allocation model trace (After vehicle choice) for household "+hh.getId());
				logger.info("");
				hh.writeDebug(logger, true);
				logger.info("***********************************************");
				
			}

		    //iterate through vehicles, choose parking location for each trip in each vehicle.
		    for(int i =0;i<hhVehicles.size();++i) {
		    	Vehicle veh = hhVehicles.get(i);
		    	ArrayList<VehicleTrip> vehicleTrips = veh.getVehicleTrips();
		    	if(vehicleTrips.size()==0)
		    		continue;
		    	
		    	for(int j =0; j<vehicleTrips.size();++j) {
		    		VehicleTrip vehicleTrip = vehicleTrips.get(j);
		    		Trip trip = vehicleTrip.getTripServed();
		    	
		    		//skip parking choice if destination of trip is home
		    		if(trip.getDest_purpose().compareToIgnoreCase("Home")==0)
		    			continue;
		    		
		    		//skip parking choice if destination not in parkarea 1
		    		int destMaz = trip.getDest_maz();
		    		if(mgraManager.getMgraParkAreas()[destMaz]!=1)
		    			continue;
		    		
		    		Trip nextTrip = null;
		    		if(j<(vehicleTrips.size()-1)) {
		    			nextTrip = vehicleTrips.get(j+1).getTripServed();
		    		}
		    		ArrayList<Person> persons = trip.getPersons();
		    		Person person=null;
		    		if(persons!=null)
		    			person = persons.get(0);
		    	
		    		setParkingChoiceDMUAttributes(hh,person,trip, nextTrip);
		    		parkingChoiceModel.computeUtilities(parkingChoiceDMU, parkingChoiceDMU.getDmuIndexValues());
		    		int parkingChoice = getParkingChoice(hh,trip);
		    		vehicleTrip.setParkingChoiceAtDestination(parkingChoice);
		    	}
		    }
		    
			if(hh.isDebug()) {
				
				logger.info("***********************************************");
				logger.info("AV allocation model trace (After parking choice) for household "+hh.getId());
				logger.info("");
				hh.writeDebug(logger, true);
				logger.info("***********************************************");
				
			}

		    //iterate through vehicles, generate empty vehicle trips.
		    for(int i =0;i<hhVehicles.size();++i) {
		    	generateEmptyTrips(hh,hhVehicles.get(i));
		    }

	
			if(hh.isDebug()) {
				
				logger.info("***********************************************");
				logger.info("AV allocation model trace (After empty vehicle generation) for household "+hh.getId());
				logger.info("");
				hh.writeDebug(logger, true);
				logger.info("***********************************************");
				
			}

		
		}
		return hhMap;
	}
	
	
	/**
	 * iterate through trip list, create empty trips as follows:
	 * 1. to the first person trip if the first person trip isn't at home
	 * 2. to remote parking if the person trip served chooses remote parking
	 * 3. to home if the person trip served chooses to send the car home
	 * 4. to the next person trip.
	 * 
	 * @param hh
	 * @param vehicle
	 */
	public void generateEmptyTrips(Household hh, Vehicle vehicle) {
		
		ArrayList<VehicleTrip> vehicleTrips = vehicle.getVehicleTrips();
		if(vehicleTrips.size()==0)
			return;
		
		int homeMaz = hh.getHomeMaz();
		
		ArrayList<VehicleTrip> newVehicleTrips = new ArrayList<VehicleTrip>();
		for(int i =0; i < vehicleTrips.size();++i) {
			VehicleTrip vehicleTrip = vehicleTrips.get(i);
			Trip tripServed = vehicleTrip.getTripServed();
			int personId = tripServed.getPerson_id();
			
			//first vehicle trip
			if(i==0) {
				
				//vehicle is home, but first trip is NOT home, create a trip to it
				if(tripServed.orig_purpose.compareToIgnoreCase("Home")!=0) {
					VehicleTrip newTrip = vehicle.createNewVehicleTrip();
					newTrip.setOriginIsHome(true);
					newTrip.setOrigMaz(homeMaz);
					newTrip.setDestMaz(tripServed.getOrig_maz());
					newTrip.setPeriod(tripServed.getStop_period());
					newTrip.setOccupants(0);
					newTrip.setOriginIsRemoteParking(false);
					newTrip.setDestinationIsRemoteParking(false);
					newTrip.setDestinationIsHome(false);
					newTrip.setOccupants(0);
					newVehicleTrips.add(newTrip);
				}
			}
			newVehicleTrips.add(vehicleTrip);
			
			//generate empty trip to remote lot if parking is remote lot
			int parkingChoice = vehicleTrip.getParkingChoiceAtDestination();
			if(parkingChoice==parkingChoiceRemote) {
				VehicleTrip newTrip = vehicle.createNewVehicleTrip();
				newTrip.setOriginIsHome(false);
				newTrip.setOrigMaz(vehicleTrip.getDestMaz());
				newTrip.setDestMaz(closestRemoteLotToMaz[vehicleTrip.getDestMaz()]);
				newTrip.setPeriod(tripServed.getStop_period());
				newTrip.setDestinationIsHome(false);
				newTrip.setOccupants(0);
				newTrip.setOriginIsRemoteParking(false);
				newTrip.setDestinationIsRemoteParking(true);
				newTrip.setDestinationIsHome(false);
				newVehicleTrips.add(newTrip);
			//or a trip to home if parking choice is home	
			}else if(parkingChoice==parkingChoiceHome) {
				VehicleTrip newTrip = vehicle.createNewVehicleTrip();
				newTrip.setOriginIsHome(false);
				newTrip.setOrigMaz(vehicleTrip.getDestMaz());
				newTrip.setDestMaz(homeMaz);
				newTrip.setPeriod(tripServed.getStop_period());
				newTrip.setDestinationIsHome(true);
				newTrip.setOccupants(0);
				newTrip.setOriginIsRemoteParking(false);
				newTrip.setDestinationIsRemoteParking(false);
				newTrip.setDestinationIsHome(true);
				newVehicleTrips.add(newTrip);
			}
			//next trip
			if(i<(vehicleTrips.size()-1)) {
				VehicleTrip nextTrip = vehicleTrips.get(i+1);
				Trip nextTripServed = nextTrip.getTripServed();
				VehicleTrip lastTrip = newVehicleTrips.get(newVehicleTrips.size()-1);
				//if the trip is not already in the same MAZ generate an empty trip
				if(lastTrip.getDestMaz()!=nextTrip.getOrigMaz()) { 
					VehicleTrip newTrip = vehicle.createNewVehicleTrip();
					newTrip.setOriginIsHome(lastTrip.isDestinationIsHome());
					newTrip.setOrigMaz(lastTrip.getDestMaz());
					newTrip.setDestMaz(nextTripServed.getOrig_maz());
					newTrip.setPeriod(nextTripServed.getStop_period());
					newTrip.setDestinationIsHome(nextTripServed.getOrig_purpose().compareToIgnoreCase("home")==0);
					newTrip.setOccupants(0);
					newTrip.setOriginIsRemoteParking(lastTrip.isDestinationIsRemoteParking());
					newTrip.setDestinationIsRemoteParking(false);
					newTrip.setDestinationIsHome(nextTripServed.orig_purpose.compareToIgnoreCase("Home")==0);
					newVehicleTrips.add(newTrip);
				}
			}
		}
		vehicle.setVehicleTrips(newVehicleTrips);
	}
	
	
	/**
	 * Set attributes for the vehicle allocation model.
	 * @param hh
	 * @param thisTrip
	 */
	public void setVehicleChoiceDMUAttributes(Household hh,Trip thisTrip) {
	    int[] vehicleIsAvailable= {0,0,0};
	    float[] travelUtilityToPerson= {0,0,0};
	    int[] vehicleIsWithPerson = {0,0,0};
	    
	    int[] avail = {1,1,1};

		ArrayList<Vehicle> vehicles = hh.getAutonomousVehicles();
		for(int i = 0;i<vehicles.size();++i) {
			Vehicle veh = vehicles.get(i);
			int period = thisTrip.getStop_period();
			
			//if the period that the trip is occuring in is after the period that the vehicle is available,
			//set the availability to 1 for the vehicle. If not, make it unavailable and continue.
			if(period>veh.getPeriodAvailable())
				vehicleIsAvailable[i]=1;
			else {
				vehicleIsAvailable[i]=0;
				continue;
			}
			
			//if the vehicle is with the person
			if(thisTrip.getPerson_id()==veh.getWithPersonId())
				vehicleIsWithPerson[i]=1;

			int origMgra=veh.getMaz();
			int origTaz = mgraManager.getTaz(origMgra);
			int destMgra=thisTrip.getDest_maz();
			int destTaz=mgraManager.getTaz(destMgra);
			vehicleChoiceDMU.setDmuIndexValues(hh.getId(), origTaz, origTaz, destTaz);
				
			//the utility to the person is 0, so don't calculate anything
			if(veh.isHome() && thisTrip.orig_purpose.compareToIgnoreCase("Home")==0)
				continue;

			float utility= getTravelUtility(hh, origMgra,destMgra,period);
			
			travelUtilityToPerson[i]=utility;
				
		}
			
		vehicleChoiceDMU.setVehicle1IsAvailable(vehicleIsAvailable[0]);
		vehicleChoiceDMU.setVehicle2IsAvailable(vehicleIsAvailable[1]);
		vehicleChoiceDMU.setVehicle3IsAvailable(vehicleIsAvailable[2]);

		vehicleChoiceDMU.setPersonWithVehicle1(vehicleIsWithPerson[0]);
		vehicleChoiceDMU.setPersonWithVehicle2(vehicleIsWithPerson[1]);
		vehicleChoiceDMU.setPersonWithVehicle3(vehicleIsWithPerson[2]);
		
		vehicleChoiceDMU.setTravelUtilityToPersonVeh1(travelUtilityToPerson[0]);
		vehicleChoiceDMU.setTravelUtilityToPersonVeh2(travelUtilityToPerson[1]);
		vehicleChoiceDMU.setTravelUtilityToPersonVeh3(travelUtilityToPerson[2]);
		
		vehicleChoiceDMU.setMinutesUntilNextTrip(thisTrip.getPeriodsUntilNextTrip()*30);
		
		
	}
	
	/**
	 * After the trip is complete, a parking choice is made for the vehicle used for 
	 * the trip. This should only be called for trips for which an AV was used.
	 * 
	 * @param hh
	 * @param person
	 * @param thisTrip
	 * @param nextTrip
	 */
	public void setParkingChoiceDMUAttributes(Household hh, Person person, Trip thisTrip, Trip nextTrip) {
		
		int[] parkArea = mgraManager.getMgraParkAreas();
		float[] monthlyCosts =mgraManager.getMParkCost();
		float[] dailyCosts = mgraManager.getDParkCost();
		float[] hourlyCosts = mgraManager.getHParkCost();
	
		int id = hh.getId();
		int hhMaz = hh.getHomeMaz();
		int destMaz = thisTrip.getDest_maz();
		int period = thisTrip.getStop_period();
		
		float durationBeforeNextTrip=8*60; //assume 8 hrs before the next trip if there isn't one
		if(nextTrip!=null)
			durationBeforeNextTrip=(nextTrip.getStop_period()- thisTrip.getStop_period())*30;
		
		int personType=0;
		int atWork=0;
		float reimburseProportion=0;
		int freeParkingEligibility=0;
		if(person!=null) {
			personType = person.getType();
			atWork = thisTrip.getDest_purpose().compareToIgnoreCase("Work")==0? 1 : 0;
			reimburseProportion = person.getReimb_pct();
			freeParkingEligibility = person.getFp_choice()==1 ? 1 : 0;
		}
		int parkingArea= parkArea[destMaz];
		float dailyParkingCost= dailyCosts[destMaz];
		float hourlyParkingCost=hourlyCosts[destMaz];
		float monthlyParkingCost=monthlyCosts[destMaz];

		float utilityToClosestRemoteLot =  getTravelUtility(hh, destMaz,destMaz,period);
		float utilityToHome = getTravelUtility(hh, destMaz,hhMaz,period);
		
		float utilityFromHomeToNextTrip =0;
		if(nextTrip!=null) {
			int nextMaz = nextTrip.getOrig_maz();
			utilityFromHomeToNextTrip = getTravelUtility(hh, destMaz,nextMaz,period);
		}

		parkingChoiceDMU.setDurationBeforeNextTrip(durationBeforeNextTrip);
		parkingChoiceDMU.setPersonType(personType);
		parkingChoiceDMU.setAtWork(atWork);
		parkingChoiceDMU.setFreeParkingEligibility(freeParkingEligibility);
		parkingChoiceDMU.setReimburseProportion(reimburseProportion);
		parkingChoiceDMU.setDailyParkingCost(dailyParkingCost);
		parkingChoiceDMU.setHourlyParkingCost(hourlyParkingCost);
		parkingChoiceDMU.setMonthlyParkingCost(monthlyParkingCost);
		parkingChoiceDMU.setParkingArea(parkingArea);
		parkingChoiceDMU.setUtilityToClosestRemoteLot(utilityToClosestRemoteLot);
		parkingChoiceDMU.setUtilityToHome(utilityToHome);
		parkingChoiceDMU.setUtilityFromHomeToNextTrip(utilityFromHomeToNextTrip);

	}
	
	/**
	 * Get the travel time for the origin and destination.
	 * 
	 * @param id An ID for the trip
	 * @param originMaz origin MAZ
	 * @param destMaz destination MAZ
	 * @param period departure period
	 * @return Time from the tripUtilityUEC (2nd alternative)
	 */
	public float getTravelTime(int id, int originMaz,int destMaz,int period) {
		
		int[] avail = {1,1,1};
		
		//calculate when the vehicle would be available for the next trip given the current
		//period and the travel time to the next trip.
		int origTaz = mgraManager.getTaz(originMaz);
		int destTaz=mgraManager.getTaz(destMaz);
		tripUtilityDMU.setDmuIndexValues(id, origTaz, origTaz, destTaz);
		vehicleChoiceDMU.setDmuIndexValues(id, origTaz, origTaz, destTaz);
		tripUtilityDMU.setTimeTrip(period);
			
		UtilityExpressionCalculator tripUtilityUEC = tripUtilityModel.getUEC();
		double[] util = tripUtilityUEC.solve(tripUtilityDMU.getDmuIndexValues(), tripUtilityDMU, avail);

		return (float) util[1];
	}
	    
	/**
	 * Get the travel utility for the origin and destination.
	 * 
	 * @param id An ID for the trip
	 * @param originMaz origin MAZ
	 * @param destMaz destination MAZ
	 * @param period departure period
	 * @return Utility from the tripUtilityUEC (1st alternative)
	 */
	public float getTravelUtility(Household hh, int originMaz,int destMaz,int period) {
		
		int[] avail = {1,1,1};
		
		//calculate when the vehicle would be available for the next trip given the current
		//period and the travel time to the next trip.
		int origTaz = mgraManager.getTaz(originMaz);
		int destTaz=mgraManager.getTaz(destMaz);
		tripUtilityDMU.setDmuIndexValues(hh.getId(), origTaz, origTaz, destTaz);
		vehicleChoiceDMU.setDmuIndexValues(hh.getId(), origTaz, origTaz, destTaz);
		tripUtilityDMU.setTimeTrip(period);
			
		UtilityExpressionCalculator tripUtilityUEC = tripUtilityModel.getUEC();
		double[] util = tripUtilityUEC.solve(tripUtilityDMU.getDmuIndexValues(), tripUtilityDMU, avail);

	    // write choice model alternative info to log file
	    if (hh.isDebug())
	    {
	    	logger.info("Calculating travel utility calculation for household " + hh.getId()+ " trip in period " + period +" from MAZ "+originMaz+" to MAZ "+destMaz);
	    	tripUtilityUEC.logAnswersArray( logger, "Trip utility");   
	    }

		
		return (float) util[0];
	}
	    
	   /**
     * Select the vehicle choice from the UEC. This  is helper code for applyModel(), where utilities have already been calculated.
   
     * @return The vehicle alternative 1,2,3,4.1-3 are AV veh numbers, 4 is other
     */
       private int getVehicleChoice(Household hh, Trip trip) {
        	// if the choice model has at least one available alternative, make
        	// choice.
        	int chosenAlt;
            long seed = hh.getSeed() + (vehicleChoiceOffset + trip.getTrip_num()*23 +trip.getPerson_id()*34 + trip.getStop_period()*23);
            random.setSeed(seed);

        	if (vehicleChoiceModel.getAvailabilityCount() > 0)
        	{
        		double randomNumber = random.nextDouble();
        		chosenAlt = vehicleChoiceModel.getChoiceResult(randomNumber);
        		
    		    // write choice model alternative info to log file
    		    if (hh.isDebug())
    		    {
    		    	String decisionMaker = String.format("Household " + hh.getId()+ " trip in period " + trip.stop_period +" from MAZ "+trip.getOrig_maz()+" to MAZ "+trip.getDest_maz());
    		    	vehicleChoiceModel.logAlternativesInfo("Vehicle Choice", decisionMaker, logger);
    		        logger.info(String.format("%s result chosen for %s is %d",
    		        	"Vehicle Choice", decisionMaker, chosenAlt));
    		        vehicleChoiceModel.logUECResults(logger, decisionMaker);
    		    }

        		
        		return chosenAlt;
        	} else
        	{
        		String decisionMaker = String.format("Household " + hh.getId()+" "+trip.getTrip_num());
        		String errorMessage = String
                    .format("Exception caught for %s, no available vehicle choice alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
        		logger.error(errorMessage);

        		vehicleChoiceModel.logUECResults(logger, decisionMaker);
        		throw new RuntimeException();
        	}

        }

	    
	   /**
     * Select the parking choice from the UEC. This  is helper code for applyModel(), where utilities have already been calculated.
   
     * @return The parking alternative 
     */
       private int getParkingChoice(Household hh, Trip trip) {
        	// if the choice model has at least one available alternative, make
        	// choice.
        	int chosenAlt;
            long seed = hh.getSeed() + (parkingChoiceOffset + trip.getTrip_num()*123 +trip.getPerson_id()*23 + trip.getStop_period()*18);
            random.setSeed(seed);

        	if (parkingChoiceModel.getAvailabilityCount() > 0)
        	{
        		double randomNumber = random.nextDouble();
        		chosenAlt = parkingChoiceModel.getChoiceResult(randomNumber);
        		
    		    // write choice model alternative info to log file
    		    if (hh.isDebug())
    		    {
    		    	String decisionMaker = String.format("Household " + hh.getId()+ " trip in period " + trip.stop_period +" from MAZ "+trip.getOrig_maz()+" to MAZ "+trip.getDest_maz());
    		    	parkingChoiceModel.logAlternativesInfo("Parking Choice", decisionMaker, logger);
    		        logger.info(String.format("%s result chosen for %s is %d",
    		        	"Parking Choice", decisionMaker, chosenAlt));
    		        parkingChoiceModel.logUECResults(logger, decisionMaker);
    		    }

        		

        		return chosenAlt;
        	} else
        	{
        		String decisionMaker = String.format("Household " + hh.getId()+" "+trip.getTrip_num());
        		String errorMessage = String
                    .format("Exception caught for %s, no available vehicle choice alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
        		logger.error(errorMessage);

        		parkingChoiceModel.logUECResults(logger, decisionMaker);
        		throw new RuntimeException();
        	}

        }
		
	
	
	
	/**
	 * Main run method
	 * @param args
	 */
	public static void main(String[] args) {
   
        
	}
	


}
