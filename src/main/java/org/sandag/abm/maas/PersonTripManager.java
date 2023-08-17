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
	protected String[] modesToKeep;
	protected String[] rideShareEligibleModes;
	protected int numberOfTimeBins;
	protected int periodLengthInMinutes;
	protected int minTaz; //the minimum taz number with mazs; any origin or destination person trip less than this will be skipped.
	protected float maxWalkDistance;

	protected static final String ModelSeedProperty = "Model.Random.Seed";
	protected static final String DirectoryProperty = "Project.Directory";
	protected static final String TripDataFileProperty = "Results.TripDataFile";
	protected static final String ModesToKeepProperty = "Maas.RoutingModel.Modes";
	protected static final String SharedEligibleProperty = "Maas.RoutingModel.SharedEligible";	
	protected static final String MaxWalkDistance = "Maas.RoutingModel.maxWalkDistance";
	
	protected static final String MexResTripDataFileProperty ="crossBorder.trip.output.file";
	protected static final String VisitorTripDataFileProperty ="visitor.trip.output.file";
	protected static final String AirportSANTripDataFileProperty ="airport.SAN.output.file";
	protected static final String AirportCBXTripDataFileProperty ="airport.CBX.output.file";
	
	
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
	public void initialize(int periodLengthInMinutes){
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
	    
	    logger.info("Minimum TAZ number is "+minTaz);
	    logger.info("Maximum TAZ number is "+maxTaz);

        //initialize the end time in minutes (stored in double so no overlap between periods)
        endTimeMinutes = new double[48+1];
        endTimeMinutes[1]=29.999999; //first period
        for(int period=2;period<endTimeMinutes.length;++period)
        	endTimeMinutes[period] = endTimeMinutes[period-1] + 30; //all other periods are 30 minutes long
        endTimeMinutes[48] = endTimeMinutes[47] + 30; //last period is 12 - 2:59:99:99 AM
        
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, ModelSeedProperty);
        random = new MersenneTwister(seed);
        
        modesToKeep = Util.getStringArrayFromPropertyMap(propertyMap,ModesToKeepProperty);
        rideShareEligibleModes = Util.getStringArrayFromPropertyMap(propertyMap,SharedEligibleProperty);
        maxWalkDistance = Util.getFloatValueFromPropertyMap(propertyMap, MaxWalkDistance);
        
        readInputFiles();
        this.periodLengthInMinutes=periodLengthInMinutes;
        groupPersonTripsByDepartureTimePeriodAndOrigin();

        //if max walk distance > 0, implement hotspots
        if(maxWalkDistance>0)
        	moveRidesharersToHotspots();
        
		logger.info("Completed Initializing PersonTripManager");
		
		
	}
	
	/**
	 * Read the input individual and joint trip files. This function calls the method
	 * @readTripList for each table. This method is called from {@initialize()}
	 */
	private void readInputFiles(){
		
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);
        String tripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, TripDataFileProperty);
        if(iteration>0)
        	tripFile = insertIterationNumber(tripFile,iteration);

        //start with individual trips
        TableDataSet residentTripDataSet = readTableData(tripFile);
        personTripMap = readResidentTripList(personTripMap, residentTripDataSet);
        int tripsSoFar=personTripMap.size();
        
        logger.info("Read "+tripsSoFar+" resident person trips");
        
        String mexicanResidentTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, MexResTripDataFileProperty);
        TableDataSet mexicanResidentTripDataSet = readTableData(mexicanResidentTripFile);
        personTripMap = readMexicanResidentTripList(personTripMap, mexicanResidentTripDataSet);
        logger.info("Read "+(personTripMap.size()-tripsSoFar)+" mexican resident person trips");
        tripsSoFar=personTripMap.size();
    	
        
        String visitorTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, VisitorTripDataFileProperty);
        TableDataSet visitorTripDataSet = readTableData(visitorTripFile);
        personTripMap = readVisitorTripList(personTripMap, visitorTripDataSet);
        logger.info("Read "+(personTripMap.size()-tripsSoFar)+" visitor person trips");
        tripsSoFar=personTripMap.size();

        String airportSANTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, AirportSANTripDataFileProperty);
        TableDataSet airportSANTripDataSet = readTableData(airportSANTripFile);
        personTripMap = readAirportTripList(personTripMap, airportSANTripDataSet, -6,"SAN");
        logger.info("Read "+(personTripMap.size()-tripsSoFar)+" SAN airport person trips");
        tripsSoFar=personTripMap.size();

        String airportCBXTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, AirportCBXTripDataFileProperty);
        TableDataSet airportCBXTripDataSet = readTableData(airportCBXTripFile);
        personTripMap = readAirportTripList(personTripMap, airportCBXTripDataSet, -5,"CBX");
        logger.info("Read "+(personTripMap.size()-tripsSoFar)+" CBX airport person trips");
        tripsSoFar=personTripMap.size();

        logger.info("Read "+personTripMap.size()+" total person trips");	
        
	}
	
	/**
	 * Check if mode is in array of modes to keep.
	 * 
	 * @param mode
	 * @return true if mode is in array, else return false
	 */
	public boolean keepMode(String mode) {
		
		for(int i = 0; i < modesToKeep.length;++i)
			if(mode.equalsIgnoreCase(modesToKeep[i]))
				return true;
		return false;
	}
	
	/**
	 * Check if mode is in array of rideshare modes.
	 * 
	 * @param mode
	 * @return true if mode is in array, else return false
	 */
	public boolean isRideshareMode(String mode) {
		
		for(int i = 0; i < rideShareEligibleModes.length;++i)
			if(mode.equalsIgnoreCase(rideShareEligibleModes[i]))
				return true;
		return false;
	}

	
	/**
	 * Read the CTRAMP trip list in the TableDataSet. 
	 * 
	 * @param personTripList A HashMap of PersonTrips. If null will be instantiated in this method.
	 * @param inputTripTableData The TableDataSet containing the CT-RAMP output trip file.
	 * @param jointTripData A boolean indicating whether the data is for individual or joint trips.
	 */
	public HashMap<Integer, PersonTrip> readResidentTripList(HashMap<Integer, PersonTrip> personTripMap, TableDataSet inputTripTableData){
		
		if(personTripMap==null)
			personTripMap = new HashMap<Integer, PersonTrip>();
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        	
        	
           	String mode = inputTripTableData.getStringValueAt(row,"trip_mode");
        	if(!keepMode(mode))
        		continue;
        	
        	boolean rideShare=false;
        	if(isRideshareMode(mode))
        		rideShare=true;
        	
         	int oMaz = (int) inputTripTableData.getValueAt(row,"origin");
        	int dMaz = (int) inputTripTableData.getValueAt(row,"destination");
        	
        	int oTaz = mgraManager.getTaz(oMaz);
        	int dTaz = mgraManager.getTaz(dMaz);
        	
        	if((oTaz<minTaz) || (dTaz<minTaz))
        		continue;

        	++idNumber;
        	
        	
           	long hhid = (long) inputTripTableData.getValueAt(row,"household_id");	
           	long personId=-1;
           	//int personNumber=-1;
           	String uniqueID=null;
           	int tourid = (int) inputTripTableData.getValueAt(row,"tour_id");
        	int tripNumber = (int) inputTripTableData.getValueAt(row,"trip_num");
        	String inboundString = inputTripTableData.getStringValueAt(row,"inbound");
        	int inbound = inboundString.equalsIgnoreCase("TRUE") ? 1 : 0 ;
        	String purpose =inputTripTableData.getStringValueAt(row, "primary_purpose");
        	String purpAbb = purpose.substring(0, 3);
        	int num_participants= (int) inputTripTableData.getValueAt(row,"tour_participants");
        	
        	
        	if(num_participants==1){
           		personId = (long) inputTripTableData.getValueAt(row,"person_id");
           		uniqueID=new String("I_"+personId+"_"+purpAbb+"_"+tourid+"_"+inbound+"_"+tripNumber);
           	}else {
           		uniqueID=new String("J_"+hhid+"_"+purpAbb+"_"+tourid+"_"+inbound+"_"+tripNumber);
           	}
         	int depPeriod = (int) inputTripTableData.getValueAt(row,"depart");
        	float depTime = simulateExactTime(depPeriod);
        
        	float sRate = 1;
        	if(inputTripTableData.containsColumn("sample_rate"))
        		sRate = inputTripTableData.getValueAt(row,"sample_rate");        	
          	
          	PersonTrip personTrip = new PersonTrip(uniqueID,hhid,personId,tourid,tripNumber,inbound,(num_participants==1?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,rideShare);
        	if(num_participants>1) {
       			personTrip.setJoint(1);
         		personTrip.setUniqueId(uniqueID+"_1");
         	}
       		personTripMap.put(idNumber, personTrip);
       		
       		//replicate joint trips
       		if(num_participants>1)
       			for(int i=2;i<=num_participants;++i){
       	        	++idNumber;
       	        	PersonTrip newTrip = null;
       	        	try {
       	        		newTrip = (PersonTrip) personTrip.clone();
       	        	}catch(Exception e) {
       	        		
       	        		logger.fatal("Error attempting to clone joint trip object "+uniqueID);
       	        		throw new RuntimeException(e);
       	        	}
       	        	newTrip.setUniqueId(uniqueID+"_"+i);
             	    personTripMap.put(idNumber, newTrip);
       			}
        }
         
         return personTripMap;
 	}
	
	
	/**
	 * Read the visitor trip list in the TableDataSet. 
	 * 
	 * @param personTripList A HashMap of PersonTrips. If null will be instantiated in this method.
	 * @param inputTripTableData The TableDataSet containing the visitor output trip file.
	 */
	public HashMap<Integer, PersonTrip> readVisitorTripList(HashMap<Integer, PersonTrip> personTripMap, TableDataSet inputTripTableData){
		
		if(personTripMap==null)
			personTripMap = new HashMap<Integer, PersonTrip>();
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        	
           	String mode = inputTripTableData.getStringValueAt(row,"trip_mode");
         	if(!keepMode(mode))
         		continue;
         	
         	boolean rideShare=false;
         	if(isRideshareMode(mode))
         		rideShare=true;
         	
          	int oMaz = (int) inputTripTableData.getValueAt(row,"origin");
         	int dMaz = (int) inputTripTableData.getValueAt(row,"destination");
      	
        	int oTaz = mgraManager.getTaz(oMaz);
        	int dTaz = mgraManager.getTaz(dMaz);
        	
        	if((oTaz<minTaz) || (dTaz<minTaz))
        		continue;

        	++idNumber;
        	
          	long hhid = -9;	
           	long personId=-9;
           	
           	int tourid = (int) inputTripTableData.getValueAt(row,"tour_id");
        	int tripNumber = (int) inputTripTableData.getValueAt(row,"trip_num");
        	String outboundString = inputTripTableData.getStringValueAt(row,"outbound");
        	int inbound = outboundString.equalsIgnoreCase("TRUE") ? 0 : 1 ;
        	
        	int num_participants=1;
        	if(inputTripTableData.containsColumn("num_participants"))
        		num_participants= (int) inputTripTableData.getValueAt(row,"num_participants");

        	int depPeriod = (int) inputTripTableData.getValueAt(row,"depart");
        	float depTime = simulateExactTime(depPeriod);
        	
        	float sRate = 1;
        	if(inputTripTableData.containsColumn("sample_rate"))
        		sRate = inputTripTableData.getValueAt(row,"sample_rate");
        	
          	String uniqueID=new String("V_"+tourid+"_"+tripNumber);

       		PersonTrip personTrip = new PersonTrip(uniqueID,hhid,personId,tourid,tripNumber,inbound,0,oMaz,dMaz,depPeriod,depTime,sRate,mode,rideShare);

       		if(num_participants>1) {
       			personTrip.setJoint(1);
         		personTrip.setUniqueId(uniqueID+"_1");
         	}
       		personTripMap.put(idNumber, personTrip);
       		
      		//replicate joint trips
       		if(num_participants>1)
       			for(int i=2;i<=num_participants;++i){
       	        	++idNumber;
       	        	PersonTrip newTrip = null;
       	        	try {
       	        		newTrip = (PersonTrip) personTrip.clone();
       	        	}catch(Exception e) {
       	        		
       	        		logger.fatal("Error attempting to clone joint trip object "+uniqueID);
       	        		throw new RuntimeException(e);
       	        	}
       	        	newTrip.setUniqueId(uniqueID+"_"+i);
             	    personTripMap.put(idNumber, newTrip);
       			}
         }
         return personTripMap;
 	}

	/**
	 * Read the Mexican resident trip list in the TableDataSet. 
	 * 
	 * @param personTripList A HashMap of PersonTrips. If null will be instantiated in this method.
	 * @param inputTripTableData The TableDataSet containing the visitor output trip file.
	 */
	public HashMap<Integer, PersonTrip> readMexicanResidentTripList(HashMap<Integer, PersonTrip> personTripMap, TableDataSet inputTripTableData){
		
		if(personTripMap==null)
			personTripMap = new HashMap<Integer, PersonTrip>();
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        	
           	String mode = inputTripTableData.getStringValueAt(row,"trip_mode");
         	if(!keepMode(mode))
         		continue;
         	
         	boolean rideShare=false;
         	if(isRideshareMode(mode))
         		rideShare=true;
         	
          	int oMaz = (int) inputTripTableData.getValueAt(row,"origin");
         	int dMaz = (int) inputTripTableData.getValueAt(row,"destination");
 	
        	int oTaz = mgraManager.getTaz(oMaz);
        	int dTaz = mgraManager.getTaz(dMaz);
        	
        	if((oTaz<minTaz) || (dTaz<minTaz))
        		continue;

        	++idNumber;
        	
        	
           	long hhid = -8;	
           	long personId=-8;
           	
          	int tourid = (int) inputTripTableData.getValueAt(row,"tour_id");
        	int tripNumber = (int) inputTripTableData.getValueAt(row,"trip_num");
        	String outboundString = inputTripTableData.getStringValueAt(row,"outbound");
        	int inbound = outboundString.equalsIgnoreCase("TRUE") ? 0 : 1 ;
        	
         	int depPeriod = (int) inputTripTableData.getValueAt(row,"depart");
        	float depTime = simulateExactTime(depPeriod);
 
        	float sRate = 1;
        	if(inputTripTableData.containsColumn("sample_rate"))
        		sRate = inputTripTableData.getValueAt(row,"sample_rate");
        	
        	int num_participants = 1;
        	
       		String uniqueID=new String("M_"+tourid+"_"+tripNumber);

       		PersonTrip personTrip = new PersonTrip(uniqueID,hhid,personId,tourid,tripNumber,inbound,0,oMaz,dMaz,depPeriod,depTime,sRate,mode,rideShare);

       		if(num_participants>1) {
       			personTrip.setJoint(1);
         		personTrip.setUniqueId(uniqueID+"_1");
       		}
       		personTripMap.put(idNumber, personTrip);
      		 
    		//replicate joint trips
       		if(num_participants>1)
       			for(int i=2;i<=num_participants;++i){
       	        	++idNumber;
       	        	PersonTrip newTrip = null;
       	        	try {
       	        		newTrip = (PersonTrip) personTrip.clone();
       	        	}catch(Exception e) {
       	        		
       	        		logger.fatal("Error attempting to clone joint trip object "+uniqueID);
       	        		throw new RuntimeException(e);
       	        	}
       	        	newTrip.setUniqueId(uniqueID+"_"+i);
             	    personTripMap.put(idNumber, newTrip);
       			}
        }
         return personTripMap;
 	}

	/**
	 * Read the airport trip list in the TableDataSet. 
	 * 
	 * @param personTripList A HashMap of PersonTrips. If null will be instantiated in this method.
	 * @param inputTripTableData The TableDataSet containing the visitor output trip file.
	 */
	public HashMap<Integer, PersonTrip> readAirportTripList(HashMap<Integer, PersonTrip> personTripMap, TableDataSet inputTripTableData, int default_id, String airportCode){
		
		if(personTripMap==null)
			personTripMap = new HashMap<Integer, PersonTrip>();
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        		 
           	String mode = inputTripTableData.getStringValueAt(row,"trip_mode");
         	if(!keepMode(mode))
         		continue;
         	
         	boolean rideShare=false;
         	if(isRideshareMode(mode))
         		rideShare=true;
         	
          	int oMaz = (int) inputTripTableData.getValueAt(row,"origin");
         	int dMaz = (int) inputTripTableData.getValueAt(row,"destination");
   	
        	int oTaz = mgraManager.getTaz(oMaz);
        	int dTaz = mgraManager.getTaz(dMaz);
        	
        	if((oTaz<minTaz) || (dTaz<minTaz))
        		continue;

        	++idNumber;

        	long hhid = default_id;	
           	long personId= default_id;
           	
          	int tourid = (int) inputTripTableData.getValueAt(row,"tour_id");
        	int tripNumber = (int) inputTripTableData.getValueAt(row,"trip_num");
        	String outboundString = inputTripTableData.getStringValueAt(row,"outbound");
        	int inbound = outboundString.equalsIgnoreCase("TRUE") ? 0 : 1 ;
        	
         	int depPeriod = (int) inputTripTableData.getValueAt(row,"depart");
        	float depTime = simulateExactTime(depPeriod);
      
        	float sRate = 1;
        	if(inputTripTableData.containsColumn("sample_rate"))
        		sRate = inputTripTableData.getValueAt(row,"sample_rate");        	
          	
         	int num_participants = 1;
        	
       		String uniqueID=new String(airportCode+"_"+tripNumber);

       		PersonTrip personTrip = new PersonTrip(uniqueID,hhid,personId,tourid,tripNumber,inbound,0,oMaz,dMaz,depPeriod,depTime,sRate,mode,rideShare);

       		if(num_participants>1) {
       			personTrip.setJoint(1);
         		personTrip.setUniqueId(uniqueID+"_1");
     
       		}
       		personTripMap.put(idNumber, personTrip);
       		
    		//replicate joint trips
       		if(num_participants>1)
       			for(int i=2;i<=num_participants;++i){
       	        	++idNumber;
       	        	PersonTrip newTrip = null;
       	        	try {
       	        		newTrip = (PersonTrip) personTrip.clone();
       	        	}catch(Exception e) {
       	        		
       	        		logger.fatal("Error attempting to clone joint trip object "+uniqueID);
       	        		throw new RuntimeException(e);
       	        	}
       	        	newTrip.setUniqueId(uniqueID+"_"+i);
             	    personTripMap.put(idNumber, newTrip);
       			}
       

        }
         
         return personTripMap;
 	}

	
	
	/**
	 * Simulate the exact time for the period.
	 * 
	 * @param period The time period (1->48)
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
	 */
	@SuppressWarnings("unchecked")
	public void groupPersonTripsByDepartureTimePeriodAndOrigin(){
		
		numberOfTimeBins = ((24*60)/periodLengthInMinutes);
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
			
			int originMaz = personTrip.getPickupMaz();
		
			float departTime = personTrip.getDepartTime();
			int bin = (int) Math.floor(departTime/((float) periodLengthInMinutes));
			
			personTripArrayByDepartureBinAndMaz[bin][originMaz].add(personTrip);
			personTripArrayByDepartureBin[bin].add(personTrip);
			
			
		}
	}

		
	/**
	 * Get the person trips for the period bin (indexed from 0) and the origin MAZ. 
	 * 
	 * @param periodBin The number of the departure time period bin based on the period length used to group person trips.
	 * @param maz The number of the origin MAZ.
	 * 
	 * @return An arraylist of person trips.
	 */
	public ArrayList<PersonTrip> getPersonTripsByDepartureTimePeriodAndMaz(int periodBin, int maz){
		
		return personTripArrayByDepartureBinAndMaz[periodBin][maz];
		
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
		personTripArrayByDepartureBinAndMaz[simulationPeriod][personTrip.getPickupMaz()].remove(personTrip);
		
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
		
		int originMaz = trip.getPickupMaz();
		personTripArrayByDepartureBin[simulationPeriod].remove(trip);
		personTripArrayByDepartureBinAndMaz[simulationPeriod][originMaz].remove(trip);
		
	}
	
	/**
	 * Pre-process the array of person trips by moving nearby ridesharers to hotspots.
	 * The algorithm finds the maz in each TAZ and simulation period with the most ridesharers.
	 * It moves the ridesharers within the maximum walking distance to that MAZ.
	 */
	public void moveRidesharersToHotspots() {
		
		logger.info("Hotspots - moving ride-sharers to high demand MAZs");
		
		int[] tazs = tazManager.getTazs();
		int maxTaz = tazManager.getMaxTaz();
		
		//store the hotspot Maz for each period and taz
		int[][] hotspotMazs = new int[numberOfTimeBins][maxTaz+1];
		
		//track the number of ridesharers moved
		int totalRidesharersMoved = 0;
		
		for(int simulationPeriod=0;simulationPeriod<numberOfTimeBins;++simulationPeriod) {
		
			int ridesharersMoved = 0;
			
			for(int taz : tazs) {
				//find maz with most passengers in taz
				int[] mazs = tazManager.getMgraArray(taz);
				int hotspotMaz=-1;
				int maxRideSharers=-1;
				for(int maz : mazs) {
				
					ArrayList<PersonTrip> personTrips = personTripArrayByDepartureBinAndMaz[simulationPeriod][maz];
					if(personTrips==null)
						continue;
				
					//set maz and max ridesharers
					if(personTrips.size()>maxRideSharers) {
						maxRideSharers= personTrips.size();
						hotspotMaz = maz;
						hotspotMazs[simulationPeriod][taz] = hotspotMaz;
					}
				}	// end mazs
				
				//no mazs with ridesharers in this taz and simulation period
				if(maxRideSharers==-1)
					continue;
				else {
					
					//get nearby ridesharers and move them to hotspot
					ArrayList<PersonTrip> nearbySharers = findNearbyRideSharersByOriginMaz(hotspotMaz, simulationPeriod, mgraManager.getTaz(hotspotMaz));
					
					if(nearbySharers==null)
						continue;

					if(nearbySharers.size()==0)
						continue;
					
				//	logger.info("TAZ "+taz+": found "+nearbySharers.size()+" to move to hotspot MAZ "+hotspotMaz+" in period "+simulationPeriod);
					
					//add each ridesharer to the person trip array at the hotspot maz, and remove them from their origin
					for(PersonTrip personTrip : nearbySharers) {
						int originMaz = personTrip.getOriginMaz();
						personTrip.setPickupMaz(hotspotMaz);
						personTripArrayByDepartureBinAndMaz[simulationPeriod][originMaz].remove(personTrip);
						personTripArrayByDepartureBinAndMaz[simulationPeriod][hotspotMaz].add(personTrip);
						++ridesharersMoved;
						++totalRidesharersMoved;
					}
				}
			} //end for zones
			
			
			
			logger.info("Simulation period "+ simulationPeriod+" moved "+ridesharersMoved+" ridesharers");
		} // end for simulation periods
		
		//now move dropoffs to hotspot locations
		int movedDropoffs = 0;
		Collection<PersonTrip> personTripList = personTripMap.values();
		for(PersonTrip personTrip : personTripList){
			
			//skip non-ride shareres
			if(!personTrip.isRideSharer())
				continue;
			
			int destinationMaz = personTrip.getDestinationMaz();
			int destinationTaz = mgraManager.getTaz(destinationMaz);
			float departTime = personTrip.getDepartTime();
			int departBin = (int) Math.floor(departTime/((float) periodLengthInMinutes));
			int hotspotMaz = hotspotMazs[departBin][destinationTaz];
			
			//no hotspot for this person's destination taz
			if(hotspotMaz==0)
				continue;
			
			float distance = ((float) mgraManager.getMgraToMgraWalkDistFrom(destinationMaz,hotspotMaz))/((float)5280.0);
			
			if(distance==0)
				continue;
			
			//distance between destination and hotspot is less than max walk distance, so move this person
			if(distance<=maxWalkDistance) {
				personTrip.setDropoffMaz(hotspotMaz);
				++movedDropoffs;
			}
		}

		logger.info("Hotspots moved "+totalRidesharersMoved+" ride-share pickups and "+movedDropoffs+" dropoffs");
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
	public ArrayList<PersonTrip> findNearbyRideSharersByOriginMaz(int originMaz, int simulationPeriod, int constraintTaz) {
		
		int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(originMaz);
		
		if(walkMgras==null)
			return null;
		
		ArrayList<PersonTrip> nearbyRideSharers = new ArrayList<PersonTrip>();
		
		//cycle through walk mgras
		for(int walkMgra : walkMgras) {

			//skip intrazonal
			if(walkMgra==originMaz)
				continue;
			
			if(constraintTaz>0)
				if(mgraManager.getTaz(walkMgra)!=constraintTaz)
					continue;
			
			//walk mgra is less than max walk distance
			float distance = ((float) mgraManager.getMgraToMgraWalkDistFrom(originMaz,walkMgra))/((float)5280.0);
			
			if(distance==0)
				continue;
			if(distance<=maxWalkDistance) {
				
					ArrayList<PersonTrip> personTrips = personTripArrayByDepartureBinAndMaz[simulationPeriod][walkMgra];
				
					//cycle through person trips in this mgra and add them to the array if they are willing to rideshare
					for(PersonTrip personTrip : personTrips) {
						
						if(personTrip.isRideSharer()) {
							nearbyRideSharers.add(personTrip);
						}
					}
			}
		}
		
		return nearbyRideSharers;
		
	}
}
