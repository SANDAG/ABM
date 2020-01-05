package org.sandag.abm.maas;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.MicromobilityChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Household;
import org.sandag.abm.maas.HouseholdAVAllocationManager.Trip;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
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

public class HouseholdAVAllocationModelRunner {

	private static final Logger logger = Logger.getLogger(HouseholdAVAllocationModelRunner.class);
    private HashMap<String, String> propertyMap = null;
	HouseholdAVAllocationManager avManager;
    private static final String MODEL_SEED_PROPERTY = "Model.Random.Seed";
   
 	private int iteration;
	  private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    private HouseholdAVAllocationModel AVAllocationModel; //one for now, can multi-thread later
    private int[] closestMazWithRemoteLot;

    private UtilityExpressionCalculator distanceUEC;
    protected VariableTable                 dmu         = null;
    protected float[][]                  tazDistanceSkims;	//travel distance

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param iteration
     */
	public HouseholdAVAllocationModelRunner(HashMap<String, String> propertyMap, int iteration){
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

		
	    //create a household AV manager, read trips
		avManager = new HouseholdAVAllocationManager(propertyMap, iteration);
		avManager.setup();
		avManager.readInputFiles();

        calculateDistanceSkims();
        calculateClosestRemoteLotMazs();
        
	}
	
	/**
	 * Creates a midday distance UEC, solves it for all zones, stores results in tazDistanceSkims[][].
	 */
	public void calculateDistanceSkims() {
		
		logger.info("Calculating distance skims");
        // Create the distance UEC
        String uecPath = Util.getStringValueFromPropertyMap(propertyMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = uecPath
                + Util.getStringValueFromPropertyMap(propertyMap, "taz.distance.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(propertyMap, "taz.distance.data.page");
        int distancePage = Util.getIntegerValueFromPropertyMap(propertyMap, "taz.od.distance.md.page");
        distanceUEC = new UtilityExpressionCalculator(new File(uecFileName), distancePage, dataPage,
        		propertyMap, dmu);
        IndexValues iv = new IndexValues();

        int maxTaz = tazManager.getMaxTaz();
     	tazDistanceSkims  = new float[maxTaz+1][maxTaz+1];
        
        for (int oTaz = 1; oTaz <= maxTaz; oTaz++){
       
        	iv.setOriginZone(oTaz);

	        double[] autoDist = distanceUEC.solve(iv, dmu, null);
	        for (int d = 0; d < maxTaz; d++){	            
	            	tazDistanceSkims[oTaz][d + 1] = (float) autoDist[d];
	        }
        }
		logger.info("Completed calculating distance skims");

	}
	
	public void runModel(){
		
		//iterate through map
		HashMap<Integer, Household> hhMap = avManager.getHouseholdMap();
        AVAllocationModel = new HouseholdAVAllocationModel(propertyMap, mgraManager,tazManager,closestMazWithRemoteLot);
        AVAllocationModel.initialize();

        AVAllocationModel.runModel(hhMap);
		
        avManager.writeVehicleTrips();
	
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
	 * Iterate through the zones for each MAZ and find the closest MAZ with a remote parking lot.
	 * 
	 */
	public void calculateClosestRemoteLotMazs() {
		
		int maxMaz = mgraManager.getMaxMgra();
	   	   
		//initialize the array
		closestMazWithRemoteLot = new int[maxMaz+1];

		//iterate through origin MAZs
		for(int originMaz=1;originMaz<=maxMaz;++originMaz) {
			
			float minDist = 99999; //initialize to a really high value
			
			int originTaz = mgraManager.getTaz(originMaz);
			if(originTaz<=0)
				continue;
			
			//iterate through destination MAZs
			for(int destinationMaz=1;destinationMaz<=maxMaz;++destinationMaz) {
				
				//no refueling stations in the destination, keep going
				if(mgraManager.getRemoteParkingLot(originMaz)==0)
					continue;
				
				int destinationTaz = mgraManager.getTaz(destinationMaz);
				if(destinationTaz<=0)
					continue;

				float dist = getDistance(originTaz, destinationTaz);
				
				//lowest distance, so reset the closest MAZ
				if(dist<minDist)
					closestMazWithRemoteLot[originMaz]=destinationMaz;
			}
			
		}
	}
	  public float getDistance(int origTaz, int destTaz){
		  
		  return tazDistanceSkims[origTaz][destTaz]; 
	  }


	/**
	 * Main run method
	 * @param args
	 */
	public static void main(String[] args) {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("Household AV Fleet Allocation Program using CT-RAMP version ",
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
        HouseholdAVAllocationModelRunner householdAVModel = new HouseholdAVAllocationModelRunner(pMap, iteration);
        householdAVModel.initialize();
        householdAVModel.runModel();

        
	}
	


}
