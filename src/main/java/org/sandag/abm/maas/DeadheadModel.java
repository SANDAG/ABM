package org.sandag.abm.maas;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
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

public class DeadheadModel {
	
	private static final Logger logger = Logger.getLogger(DeadheadModel.class);
    public static final int         MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET = 0;
    private MatrixDataServerRmi     ms;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    AutoTazSkimsCalculator tazDistanceCalculator;
    private int iteration;
    private HashMap<String, String> propertyMap = null;
    private double[] endTimeMinutes; // the period end time in number of minutes past 3 AM , starting in period 1 (index 1)
    private MersenneTwister       random;
    private ModelStructure modelStructure;
   /* 
    private HashMap<Integer,VehicleTrip> TNCTrips = null; //for now key off rownumber*10 + (1 if indiv,2 if joint) from input file 
    private HashMap<Integer,VehicleTrip> TaxiTrips = null; //for now key off rownumber*10 + (1 if indiv,2 if joint) from input file
    private HashMap<Long,HashMap<Integer,VehicleTrip>> PrivateAVTrips = null; //extra dimension is household ID
*/
    private ArrayList<PersonTrip> TNCTrips = null;
    private ArrayList<PersonTrip> TaxiTrips = null;
    private HashMap<Long,ArrayList<PersonTrip>> PrivateAVTrips = null; //extra dimension is household ID
   
    private static final String DirectoryProperty = "Project.Directory";
    private static final String IndivTripDataFileProperty = "Results.IndivTripDataFile";
    private static final String JointTripDataFileProperty = "Results.JointTripDataFile";
    private static final String TNCTripDataFileProperty = "Results.TNCTripDataFile";
    private static final String TaxiTripDataFileProperty = "Results.TaxiTripDataFile";
    private static final String TNCVehicleDataFileProperty = "Results.TNCVehicleDataFile";
    private static final String TaxiVehicleDataFileProperty = "Results.TaxiVehicleDataFile";
    private static final String ModelSeedProperty = "Model.Random.Seed";
    
    /**
     * Create a Deadhead Model.
     * @param propertyMap
     * @param iteration
     */
    public void DeadHeadModel(HashMap<String, String> propertyMap, int iteration){
     	
    	this.iteration = iteration;
    	this.propertyMap = propertyMap;
    	startMatrixServer(propertyMap);
    	initialize(propertyMap);
    	
    	modelStructure = new SandagModelStructure();
    }
	
    /**
     * Initialize the arrays and other data members.
     * @param propertyMap
     */
	public void initialize(HashMap<String, String> propertyMap){
		
		logger.info("Initializing Deadhead Model");
	    mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);

        tazDistanceCalculator = new AutoTazSkimsCalculator(propertyMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        
        //initialize the end time in minutes (stored in double so no overlap between periods)
        endTimeMinutes = new double[40+1];
        endTimeMinutes[1]=119.999999; //first period is 3-3:59:99:99
        for(int period=2;period<endTimeMinutes.length;++period)
        	endTimeMinutes[period] = endTimeMinutes[period-1] + 30; //all other periods are 30 minutes long
        endTimeMinutes[40] = endTimeMinutes[39] + 3*60; //last period is 12 - 2:59:99:99 AM
        
        int seed = Util.getIntegerValueFromPropertyMap(propertyMap, ModelSeedProperty);
        random = new MersenneTwister(seed);
        
        /*
        TNCTrips = new HashMap<Integer,VehicleTrip>();
        TaxiTrips = new HashMap<Integer,VehicleTrip>();
        PrivateAVTrips = new HashMap<Long,HashMap<Integer,VehicleTrip>>();
         */
        TNCTrips = new ArrayList<PersonTrip>();
        TaxiTrips = new ArrayList<PersonTrip>();
        PrivateAVTrips = new HashMap<Long,ArrayList<PersonTrip>>();


	}
	
	/**
	 * Read the input individual and joint trip files. This function calls the method
	 * @readTripList for each table.
	 */
	public void readInputTrips(){
		
        String directory = Util.getStringValueFromPropertyMap(propertyMap, DirectoryProperty);
        String indivTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, IndivTripDataFileProperty);
        indivTripFile = insertIterationNumber(indivTripFile,iteration);
        String jointTripFile = directory
                + Util.getStringValueFromPropertyMap(propertyMap, JointTripDataFileProperty);
        jointTripFile = insertIterationNumber(jointTripFile,iteration);

         
        //start with individual trips
        TableDataSet indivTripDataSet = readTableData(indivTripFile);
        readTripList(indivTripDataSet, false);
        
        //now read joint trip data
        TableDataSet jointTripDataSet = readTableData(jointTripFile);
        readTripList(jointTripDataSet, true);
        
	}
	
	/**
	 * Read the trip list in the TableDataSet. 
	 * 
	 * @param inputTripTableData The TableDataSet containing the CT-RAMP output trip file.
	 * @param jointTripData A boolean indicating whether the data is for individual or joint trips.
	 */
	public void readTripList(TableDataSet inputTripTableData, boolean jointTripData){
		
         for(int row = 1; row <= inputTripTableData.getRowCount();++row){
        	
           	long hhid = (long) inputTripTableData.getValueAt(row,"hh_id");	
           	int personNumber=-1;
           	long personId=-1;
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
        	
            //put the vehicle trip in one of the hashmaps
/*        	if(modelStructure.getTripModeIsTNC(mode)){
        		PersonTrip trip = new PersonTrip(hhid,personId,personNumber,tourid,stopid,inbound,(jointTripData?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,0,0,0);
        		//TNCTrips.put(row*10+tableIndex,trip);
        		TNCTrips.add(trip);
        	} else if(modelStructure.getTripModeIsTaxi(mode)){
        		PersonTrip trip = new PersonTrip(hhid,personId,personNumber,tourid,stopid,inbound,(jointTripData?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,0,0,0);
        		//TaxiTrips.put(row*10+tableIndex,trip);
        		TaxiTrips.add(trip);
        	} else if((avAvailable==1) && modelStructure.getTripModeIsSovOrHov(mode)){
        		PersonTrip trip = new PersonTrip(hhid,personId,personNumber,tourid,stopid,inbound,(jointTripData?1:0),oMaz,dMaz,depPeriod,depTime,sRate,mode,0,0,0);
        		if(PrivateAVTrips.containsKey(hhid)==false){
        			//PrivateAVTrips.put(hhid,new HashMap<Integer,VehicleTrip>());
        			PrivateAVTrips.put(hhid,new ArrayList<PersonTrip>());
        		}
        		

                // HashMap<Integer,VehicleTrip> hhAVMap = PrivateAVTrips.get(hhid);
        		// hhAVMap.put(row*10+tableIndex,trip);
        		// PrivateAVTrips.put(hhid,hhAVMap);
        		ArrayList<PersonTrip> hhAVList = PrivateAVTrips.get(hhid);
        		hhAVList.add(trip);
        	}
        	
        	*/
        }
 	}
	
	public void generateVehicles(){
		
    	Collections.sort(TNCTrips);
    	

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

	
	private class deadheadTrip{
		int originMaz;
		int destinationMaz;
		int departTime;
		int occupancy; //should be 1 or 0
		float sampleRate;
	}
	
	private class availableVehicle{
	
		int maz;
		float sampleRate;
	}

}
