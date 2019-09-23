package org.sandag.abm.maas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;

public class PersonTripManager {
	
	protected static final Logger logger = Logger.getLogger(PersonTripManager.class);
	protected HashMap<String, String> propertyMap = null;
	protected MersenneTwister       random;
	protected ModelStructure modelStructure;
	protected HashMap<Integer,PersonTrip> personTripMap;
	protected ArrayList<PersonTrip>[][] personTripArrayByDepartureBinAndMaz; //an array of PersonTrips by departure time increment and origin MAZ.
	protected ArrayList<PersonTrip>[] personTripArrayByDepartureBin; //an array of PersonTrips by departure time increment
	protected double[] endTimeMinutes; // the period end time in number of minutes past 3 AM , starting in period 1 (index 1)
	protected int iteration;
	protected MgraDataManager mgraManager;
	protected TazDataManager tazManager;
	protected int idNumber;
	protected int[] modesToKeep;
	protected int[] rideShareEligibleModes;
	protected int minTaz; //the minimum taz number with mazs; any origin or destination person trip less than this will be skipped.
	protected float maxWalkDistance;

	protected static final String ModelSeedProperty = "Model.Random.Seed";
	protected static final String DirectoryProperty = "Project.Directory";
	protected static final String IndivTripDataFileProperty = "Results.IndivTripDataFile";
	protected static final String JointTripDataFileProperty = "Results.JointTripDataFile";
	protected static final String ModesToKeepProperty = "Maas.RoutingModel.Modes";
	protected static final String SharedEligibleProperty = "Maas.RoutingModel.SharedEligible";	
	protected static final String MaxWalkDistance = "Maas.RoutingModel.maxWalkDistance";
	/**
	 * Constructor.
	 * 
	 * @param propertyMap
	 * @param iteration
	 */
	public PersonTripManager(HashMap<String, String> propertyMap, int iteration){
    	this.iteration = iteration;
    	this.propertyMap = propertyMap;
    	
    	modelStructure = new SandagModelStructure();
    
    }

	/**
	 * Initialize (not done by default).
	 *   Initializes array to simulate actual departure time
	 *   Reads in individual and joint person trips
	 */
	public void initialize(){
		logger.info("Initializing PersonTripManager");
		
	    mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);
	    
	    //find minimum TAZ with mazs
	    int maxTaz = tazManager.getMaxTaz();
	    minTaz = -1;
	    for(int i=1;i<=maxTaz;++i){
			int[] mazs = tazManager.getMgraArray(i);
			
			if(mazs==null|| mazs.length==0)
				minTaz = Math.max(i, minTaz);
	    }
	    
	    logger.info("Minimum TAZ number is +minTaz");
	    logger.info("Maximum TAZ number is +maxTaz");

        //initialize the end time in minutes (stored in double so no overlap between periods)
        endTimeMinutes = new double[40+1];
        endTimeMinutes[1]=119.999999; //first period is 3-3:59:99:99
        for(int period=2;period<endTimeMinutes.length;++period)
        	endTimeMinutes[period] = endTimeMinutes[period-1] + 30; //all other periods are 30 minutes long
        endTimeMinutes[40] = endTimeMinutes[39] + 3*60; //last period is 12 - 2:59:99:99 AM
        
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, ModelSeedProperty);
        random = new MersenneTwister(seed);
        
        modesToKeep = Util.getIntegerArrayFromPropertyMap(propertyMap,ModesToKeepProperty);
        rideShareEligibleModes = Util.getIntegerArrayFromPropertyMap(propertyMap,SharedEligibleProperty);
        maxWalkDistance = Util.getFloatValueFromPropertyMap(propertyMap, MaxWalkDistance);
        
        readInputFiles();
          
		logger.info("Completed Initializing PersonTripManager");
		
		
	}
	
	/**
	 * Read the input individual and joint trip files. This function calls the method
	 * @readTripList for each table. This method is called from {@initialize()}
	 */
	private void readInputFiles(){
		
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);
        String indivTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, IndivTripDataFileProperty);
        indivTripFile = insertIterationNumber(indivTripFile,iteration);
        String jointTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, JointTripDataFileProperty);
        jointTripFile = insertIterationNumber(jointTripFile,iteration);

        //start with individual trips
        TableDataSet indivTripDataSet = readTableData(indivTripFile);
        personTripMap = readTripList(personTripMap, indivTripDataSet, false);
        int individualPersonTrips=personTripMap.size();
        
        logger.info("Read "+individualPersonTrips+" individual person trips");
        
        //now read joint trip data
        TableDataSet jointTripDataSet = readTableData(jointTripFile);
        personTripMap = readTripList(personTripMap, jointTripDataSet, true);
        int jointPersonTrips = personTripMap.size() - individualPersonTrips;
        
        logger.info("Read "+jointPersonTrips+" joint person trips");
        logger.info("Read "+personTripMap.size()+" total person trips");
        
	}
	
	/**
	 * Read the trip list in the TableDataSet. 
	 * 
	 * @param personTripList A HashMap of PersonTrips. If null will be instantiated in this method.
	 * @param inputTripTableData The TableDataSet containing the CT-RAMP output trip file.
	 * @param jointTripData A boolean indicating whether the data is for individual or joint trips.
	 */
	public HashMap<Integer, PersonTrip> readTripList(HashMap<Integer, PersonTrip> personTripMap, TableDataSet inputTripTableData, boolean jointTripData){
		
		if(personTripMap==null)
			personTripMap = new HashMap<Integer, PersonTrip>();
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        	
        	
           	int mode = (int) inputTripTableData.getValueAt(row,"trip_mode");
        	if(modesToKeep[mode]!=1)
        		continue;
        	
        	boolean rideShare=false;
        	if(rideShareEligibleModes[mode]==1)
        		rideShare=true;
        	
         	int oMaz = (int) inputTripTableData.getValueAt(row,"orig_mgra");
        	int dMaz = (int) inputTripTableData.getValueAt(row,"dest_mgra");
        	
        	int oTaz = mgraManager.getTaz(oMaz);
        	int dTaz = mgraManager.getTaz(dMaz);
        	
        	if((oTaz<minTaz) || (dTaz<minTaz))
        		continue;

        	++idNumber;
        	
        	
           	long hhid = (long) inputTripTableData.getValueAt(row,"hh_id");	
           	long personId=-1;
           	int personNumber=-1;
           	if(jointTripData==false){
           		personId = (long) inputTripTableData.getValueAt(row,"person_id");
           		personNumber = (int) inputTripTableData.getValueAt(row,"person_num");
           		
           	}
        	int tourid = (int) inputTripTableData.getValueAt(row,"tour_id");
        	int stopid = (int) inputTripTableData.getValueAt(row,"stop_id");
        	int inbound = (int)inputTripTableData.getValueAt(row,"inbound");
         	int depPeriod = (int) inputTripTableData.getValueAt(row,"stop_period");
        	float depTime = simulateExactTime(depPeriod);
        	int tour_mode = (int)inputTripTableData.getValueAt(row,"tour_mode");

        	
        	//TODO: doesn't handle sampling yet
        	float sRate = 1;
        	if(inputTripTableData.containsColumn("sampleRate"))
        		sRate = inputTripTableData.getValueAt(row,"sampleRate");
        	
          	
          	int avAvailable = 0;
          	if(inputTripTableData.containsColumn("avAvailable"))
          		avAvailable = (int) inputTripTableData.getValueAt(row,"avAvailable");
        	
        	int boardingTap = (int) inputTripTableData.getValueAt(row,"trip_board_tap");  
        	int alightingTap = (int) inputTripTableData.getValueAt(row,"trip_alight_tap");  
        	String tour_purpose	= inputTripTableData.getStringValueAt(row, "tour_purpose");
        	String orig_purpose	= inputTripTableData.getStringValueAt(row, "orig_purpose");
        	String dest_purpose = inputTripTableData.getStringValueAt(row, "dest_purpose");
        	
        	float distance = 0;
         	if(inputTripTableData.containsColumn("trip_dist"))
         		distance = inputTripTableData.getValueAt(row, "trip_dist");
        	
        	
        	int num_participants=-1;
        	if(jointTripData){
        		num_participants = (int) inputTripTableData.getValueAt(row,"num_participants");
        	}
        	int set = (int)inputTripTableData.getValueAt(row,"set"); 
        	
       		PersonTrip personTrip = new PersonTrip(idNumber,hhid,personId,personNumber,tourid,stopid,inbound,(jointTripData?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,boardingTap,alightingTap,set,rideShare);
       		personTrip.setAvAvailable((byte) avAvailable);
       		personTrip.setNumberParticipants(num_participants);
       		if(num_participants>-1)
       			personTrip.setJoint(1);
       		personTripMap.put(idNumber, personTrip);
       		
       		//replicate joint trips
       		if(num_participants>1)
       			for(int i=2;i<=num_participants;++i){
       	        	++idNumber;
       	        	personTripMap.put(idNumber, personTrip);
       			}
        }
         
         return personTripMap;
 	}
	/**
	 * Simulate the exact time for the period.
	 * 
	 * @param period The time period (1->40)
	 * @return The exact time in double precision (number of minutes past 3 AM)
	 */
	public float simulateExactTime(int period){
		
		double lowerEnd = endTimeMinutes[period-1];
		double upperEnd = endTimeMinutes[period];
        double randomNumber = random.nextDouble();
        
        float time = (float) ((upperEnd - lowerEnd) * randomNumber + lowerEnd);

		return time;
	}
	 
	/** 
	 * A simple helper function to insert the iteration number into the file name.
	 * 
	 * @param filename The input file name (ex: inputFile.csv)
	 * @param iteration The iteration number (ex: 3)
	 * @return The new string (ex: inputFile_3.csv)
	 */
	private String insertIterationNumber(String filename, int iteration){
		
		String newFileName = filename.replace(".csv", "_"+new Integer(iteration).toString()+".csv");
		return newFileName;
	}

	/**
	 * Read data into inputDataTable tabledataset.
	 * 
	 */
	private TableDataSet readTableData(String inputFile){
		
		TableDataSet tableDataSet = null;
		
		logger.info("Begin reading the data in file " + inputFile);
	    
	    try
	    {
	    	OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
	    	tableDataSet = csvFile.readFile(new File(inputFile));
	    } catch (IOException e)
	    {
	    	throw new RuntimeException(e);
       }
       logger.info("End reading the data in file " + inputFile);
       
       return tableDataSet;
	}
	
	/**
	 * Go through the person trip list, sort the person trips by departure time and MAZ.
	 *  
	 * @param periodLengthInMinutes
	 */
	@SuppressWarnings("unchecked")
	public void groupPersonTripsByDepartureTimePeriodAndOrigin(int periodLengthInMinutes){
		
		int numberOfTimeBins = ((24*60)/periodLengthInMinutes);
		int maxMaz = mgraManager.getMaxMgra();
		
		logger.info("Calculated "+numberOfTimeBins+" simulation periods using a period length of "+periodLengthInMinutes+" minutes");
		personTripArrayByDepartureBinAndMaz = new ArrayList[numberOfTimeBins][maxMaz+1];
		personTripArrayByDepartureBin = new ArrayList[numberOfTimeBins];
		
		//initialize
		for(int i = 0; i < numberOfTimeBins;++i){
			personTripArrayByDepartureBin[i] = new ArrayList<PersonTrip>();
			for(int j = 0; j <=maxMaz;++j){
				personTripArrayByDepartureBinAndMaz[i][j] = new ArrayList<PersonTrip>();
			}
				
		}
		
		Collection<PersonTrip> personTripList = personTripMap.values();
		for(PersonTrip personTrip : personTripList){
			
			int originMaz = personTrip.getOriginMaz();
		
			float departTime = personTrip.getDepartTime();
			int bin = (int) Math.floor(departTime/((float) periodLengthInMinutes));
			
			personTripArrayByDepartureBinAndMaz[bin][originMaz].add(personTrip);
			personTripArrayByDepartureBin[bin].add(personTrip);
			
			
		}
	}

		
	/**
	 * Get the person trips for the period bin (indexed from 0) and the origin MAZ. This method also finds nearby trips willing
	 * to rideshare if they are within the maximum walk distance from the origin.
	 * 
	 * @param periodBin The number of the departure time period bin based on the period length used to group person trips.
	 * @param maz The number of the origin MAZ.
	 * 
	 * @return An arraylist of person trips.
	 */
	public ArrayList<PersonTrip> getPersonTripsByDepartureTimePeriodAndMaz(int periodBin, int maz){
		
		
		if(maxWalkDistance==0)
			return personTripArrayByDepartureBinAndMaz[periodBin][maz];
		
		ArrayList<PersonTrip> returnList = new ArrayList<PersonTrip>();
		
		if(personTripArrayByDepartureBinAndMaz[periodBin][maz]!=null)
			returnList.addAll(personTripArrayByDepartureBinAndMaz[periodBin][maz]);
		
		ArrayList<PersonTrip> nearbyTrips = findNearbyRideSharers(maz,periodBin);
		
		if(nearbyTrips != null)
			returnList.addAll(nearbyTrips);
		
		return returnList;
	}
	
	/**
	 * Sample a person trip from the array for the given period. REMOVE IT from the person trip arrays.
	 * 
	 * @param simulationPeriod  The simulation period to sample a trip from.
	 * @param rnum A random number to be used in sampling.
	 * @return A person trip, or null if the ArrayList is null or empty.
	 */
	PersonTrip samplePersonTrip(int simulationPeriod, double rnum){
		
		ArrayList<PersonTrip> personTripArray = personTripArrayByDepartureBin[simulationPeriod];
		
		if(personTripArray==null)
			return null;
		
		int listSize = personTripArray.size();
		
		if(listSize==0)
			return null;
		
		int element = (int) Math.floor(rnum * listSize);
		PersonTrip personTrip = personTripArray.get(element);
		personTripArrayByDepartureBin[simulationPeriod].remove(personTrip);
		personTripArrayByDepartureBinAndMaz[simulationPeriod][personTrip.getOriginMaz()].remove(personTrip);
		
		return personTrip;
	}

	/**
	 * Sample a person trip from the array for the given period. REMOVE IT from the array.
	 * 
	 * @param simulationPeriod  The period to sample a trip from.
	 * @param maz The maz to sample a trip from.
	 * @param rnum A random number to be used in sampling.
	 * @return A person trip, or null if the ArrayList is null or empty.
	 */
	PersonTrip samplePersonTrip(int simulationPeriod, int maz, double rnum){
		
		ArrayList<PersonTrip> personTripArray = personTripArrayByDepartureBinAndMaz[simulationPeriod][maz];
		
		if(personTripArray==null)
			return null;
		
		int listSize = personTripArray.size();
		
		if(listSize==0)
			return null;
		
		int element = (int) Math.floor(rnum * listSize);
		PersonTrip personTrip = personTripArray.get(element);
		personTripArrayByDepartureBinAndMaz[simulationPeriod][maz].remove(personTrip);
		personTripArrayByDepartureBin[simulationPeriod].remove(personTrip);
		
		return personTrip;
	}

	/**
	 * Check if there are more person trips in this simulation period.
	 * 
	 * @param simulationPeriod
	 * @return true if there are more person trips, false if not.
	 */
	public boolean morePersonTripsInSimulationPeriod(int simulationPeriod){
		ArrayList<PersonTrip> personTripArray = personTripArrayByDepartureBin[simulationPeriod];
		
		if(personTripArray==null)
			return false;
		
		int listSize = personTripArray.size();
		
		if(listSize==0)
			return false;
		
		return true;

	}
	
	/**
	 * Check if there are more person trips in this simulation period and maz.
	 * 
	 * @param simulationPeriod
	 * @return true if there are more person trips, false if not.
	 */
	public boolean morePersonTripsInSimulationPeriodAndMaz(int simulationPeriod, int maz){
		ArrayList<PersonTrip> personTripArray = personTripArrayByDepartureBinAndMaz[simulationPeriod][maz];
		
		if(personTripArray==null)
			return false;
		
		int listSize = personTripArray.size();
		
		if(listSize==0)
			return false;
		
		return true;

	}
	/**
	 * Remove the person trip.
	 * 
	 * @param trip
	 * @param simulationPeriod
	 */
	public void removePersonTrip(PersonTrip trip, int simulationPeriod){
		
		int originMaz = trip.getOriginMaz();
		personTripArrayByDepartureBin[simulationPeriod].remove(trip);
		personTripArrayByDepartureBinAndMaz[simulationPeriod][originMaz].remove(trip);
		
		
	}
	
	/**
	 * Cycle through all the MAZs within maximum walk distance of the origin MAZ, and find
	 * rideshare passengers departing within the same period. Add them to an ArrayList and 
	 * return it.
	 * 
	 * @param originMaz The origin for searching
	 * @param simulationPeriod The simulation period
	 * @return The ArrayList of ridesharers.
	 */
	public ArrayList<PersonTrip> findNearbyRideSharers(int originMaz, int simulationPeriod) {
		
		int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(originMaz);
		
		if(walkMgras==null)
			return null;
		
		ArrayList<PersonTrip> nearbyRideSharers = new ArrayList<PersonTrip>();
		
		//cycle through walk mgras
		for(int walkMgra : walkMgras) {

			//skip intrazonal
			if(walkMgra==originMaz)
				continue;
			
			//walk mgra is less than max walk distance
			if(mgraManager.getMgraToMgraWalkDistFrom(originMaz,walkMgra)<=maxWalkDistance) {
				
					ArrayList<PersonTrip> personTrips = personTripArrayByDepartureBinAndMaz[simulationPeriod][walkMgra];
				
					//cycle through person trips in this mgra and add them to the array if they are willing to rideshare
					for(PersonTrip personTrip : personTrips) {
						
						if(personTrip.isRideSharer()) {
							personTrip.setPickupMaz(originMaz);
							nearbyRideSharers.add(personTrip);
						}
					}
			}
		}
		
		return nearbyRideSharers;
		
	}
}
