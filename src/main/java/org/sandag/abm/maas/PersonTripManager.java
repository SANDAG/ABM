package org.sandag.abm.maas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;

public class PersonTripManager {
	
	private static final Logger logger = Logger.getLogger(PersonTripManager.class);
    private HashMap<String, String> propertyMap = null;
    private MersenneTwister       random;
    private ModelStructure modelStructure;
    private HashMap<Integer,PersonTrip> personTripMap;
	private ArrayList<PersonTrip>[][] personTripArrayByDepartureBinAndMaz; //an array of PersonTrips by departure time increment and origin MAZ.
	private ArrayList<PersonTrip>[] personTripArrayByDepartureBin; //an array of PersonTrips by departure time increment
    private double[] endTimeMinutes; // the period end time in number of minutes past 3 AM , starting in period 1 (index 1)
    private int iteration;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    private int idNumber;

    private static final String ModelSeedProperty = "Model.Random.Seed";
    private static final String DirectoryProperty = "Project.Directory";
    private static final String IndivTripDataFileProperty = "Results.IndivTripDataFile";
    private static final String JointTripDataFileProperty = "Results.JointTripDataFile";

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

        //initialize the end time in minutes (stored in double so no overlap between periods)
        endTimeMinutes = new double[40+1];
        endTimeMinutes[1]=119.999999; //first period is 3-3:59:99:99
        for(int period=2;period<endTimeMinutes.length;++period)
        	endTimeMinutes[period] = endTimeMinutes[period-1] + 30; //all other periods are 30 minutes long
        endTimeMinutes[40] = endTimeMinutes[39] + 3*60; //last period is 12 - 2:59:99:99 AM
        
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, ModelSeedProperty);
        random = new MersenneTwister(seed);
        
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
        
        //now read joint trip data
        TableDataSet jointTripDataSet = readTableData(jointTripFile);
        personTripMap = readTripList(personTripMap, jointTripDataSet, true);
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
         	int oMaz = (int) inputTripTableData.getValueAt(row,"orig_mgra");
        	int dMaz = (int) inputTripTableData.getValueAt(row,"dest_mgra");
        	int depPeriod = (int) inputTripTableData.getValueAt(row,"stop_period");
        	float depTime = simulateExactTime(depPeriod);
        	float sRate = inputTripTableData.getValueAt(row,"sampleRate");
          	int mode = (int) inputTripTableData.getValueAt(row,"trip_mode");
            int avAvailable = (int) inputTripTableData.getValueAt(row,"avAvailable");  	
        	int boardingTap = (int) inputTripTableData.getValueAt(row,"trip_board_tap");  
        	int alightingTap = (int) inputTripTableData.getValueAt(row,"trip_alight_tap");  
        	String tour_purpose	= inputTripTableData.getStringValueAt(row, "tour_purpose");
        	String orig_purpose	= inputTripTableData.getStringValueAt(row, "orig_purpose");
        	String dest_purpose = inputTripTableData.getStringValueAt(row, "dest_purpose");
        	float distance = inputTripTableData.getValueAt(row,"trip_dist");
        	
        	int num_participants=-1;
        	if(jointTripData){
        		num_participants = (int) inputTripTableData.getValueAt(row,"num_participants");
        	}
        	int tour_mode = (int)inputTripTableData.getValueAt(row,"tour_mode");
        	
        	int set = (int)inputTripTableData.getValueAt(row,"set"); 
        	
            if(modelStructure.getTripModeIsTransit(mode)){
        		PersonTrip personTrip = new PersonTrip(idNumber,hhid,personId,personNumber,tourid,stopid,inbound,(jointTripData?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,boardingTap,alightingTap,set);
        		personTrip.setAvAvailable(avAvailable);
        		personTrip.setTourPurpose(tour_purpose);
        		personTrip.setOriginPurpose(orig_purpose);
        		personTrip.setDestinationPurpose(dest_purpose);
        		personTrip.setDistance(distance);
        		personTrip.setNumberParticipants(num_participants);
        		personTrip.setTourMode(tour_mode);
        		if(num_participants>-1)
        			personTrip.setJoint(1);
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
	 * Go through the person trip list, sort the person trips by departure time and TAZ.
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
	 * Get the person trips for the period bin (indexed from 0)
	 * 
	 * @param periodBin The number of the departure time period bin based on the period length used to group person trips.
	 * 
	 * @return An arraylist of person trips.
	 */
	public ArrayList<PersonTrip> getPersonTripsDepartingInTimePeriod(int periodBin){
		
		return personTripArrayByDepartureBin[periodBin];
	}
	
	/**
	 * Get the person trips for the period bin (indexed from 0) and the origin MAZ
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
	 * Sample a person trip from the array. REMOVE IT from the array.
	 * 
	 * @param personTripArray
	 * @param rnum
	 * @return
	 */
	PersonTrip samplePersonTripFromArrayList(ArrayList<PersonTrip> personTripArray, double rnum){
		
		if(personTripArray==null)
			return null;
		
		int listSize = personTripArray.size();
		int element = (int) Math.floor(rnum * listSize);
		PersonTrip personTrip = personTripArray.get(element);
		personTripArray.remove(personTrip);
		
		return personTrip;
	}


}
