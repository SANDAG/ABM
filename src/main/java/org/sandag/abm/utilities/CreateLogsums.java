package org.sandag.abm.utilities;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.application.SandagCtrampDmuFactory;
import org.sandag.abm.application.SandagHouseholdDataManager;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.HouseholdChoiceModelRunner;
import org.sandag.abm.ctramp.HouseholdDataManager;
import org.sandag.abm.ctramp.HouseholdDataManagerIf;
import org.sandag.abm.ctramp.HouseholdDataManagerRmi;
import org.sandag.abm.ctramp.HouseholdDataWriter;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.UsualWorkSchoolLocationChoiceModel;
import org.sandag.abm.ctramp.Util;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;

public class CreateLogsums {


	private BuildAccessibilities                       aggAcc;
	private JPPFClient 								   jppfClient;
	private static Logger      logger                  = Logger.getLogger(CreateLogsums.class);
	private HouseholdDataManagerIf householdDataManager;
	private HashMap<String,String> propertyMap;
	private ResourceBundle resourceBundle;
    // are used if no command line arguments are specified.
    private int                globalIterationNumber        = 0;
    private float              iterationSampleRate          = 0f;
    private int                sampleSeed                   = 0;
    private SandagModelStructure modelStructure;
    private SandagCtrampDmuFactory dmuFactory;
    private MatrixDataServerIf ms;
    private ModelOutputReader modelOutputReader;
    
 
    /**
     * Constructor.
     * 
     * @param propertiesFile
     * @param globalIterationNumber
     * @param globalSampleRate
     * @param sampleSeed
     */
	public CreateLogsums(String propertiesFile, int globalIterationNumber, float globalSampleRate, int sampleSeed){
		
		this.resourceBundle = ResourceBundle.getBundle(propertiesFile);
		propertyMap = ResourceUtil.getResourceBundleAsHashMap ( propertiesFile);
	    this.globalIterationNumber = globalIterationNumber;
	    this.iterationSampleRate = globalSampleRate;
	    this.sampleSeed = sampleSeed;
	 
	}
	
	/**
	 * Initialize data members
	 */
	public void initialize(){
		
    	startMatrixServer(propertyMap);

        // create modelStructure object
        modelStructure = new SandagModelStructure();

		householdDataManager = getHouseholdDataManager();
		logger.info("There are " + householdDataManager.getNumHouseholds()+" households in hh manager after getting household manager");

		// create a factory object to pass to various model components from which
        // they can create DMU objects
        dmuFactory = new SandagCtrampDmuFactory(modelStructure,propertyMap);

        modelOutputReader = new ModelOutputReader(propertyMap,modelStructure, globalIterationNumber);
	}
	
	
	/**
	 * Run all components.
	 * 
	 */
	public void run(){
		
		initialize();
		readModelOutputsAndCreateTours();
		createWorkLogsums();
		createNonWorkLogsums();
				
        HouseholdDataWriter dataWriter = new HouseholdDataWriter( propertyMap, modelStructure,  globalIterationNumber );
        dataWriter.writeDataToFiles(householdDataManager);

	}
	
	/**
	 * Read the model outputs and create tours.
	 */
	public void readModelOutputsAndCreateTours(){
		
		modelOutputReader.readHouseholdDataOutput();
		modelOutputReader.readPersonDataOutput();
		modelOutputReader.readTourDataOutput();
		
		logger.info("There are " + householdDataManager.getNumHouseholds()+" households in hh manager before reading model output");

		Household[] households = householdDataManager.getHhArray();
		for(Household household : households){
			
			modelOutputReader.setHouseholdAndPersonAttributes(household);
			
			if(modelOutputReader.hasJointTourFile())
				modelOutputReader.createJointTours(household);
			
			if(modelOutputReader.hasIndividualTourFile())
				modelOutputReader.createIndividualTours(household);
		}
		householdDataManager.setHhArray(households);
		logger.info("There are " + householdDataManager.getNumHouseholds()+" households in hh manager after reading model output");

	}
	
	
	
	/**
	 * Calculate and write work destination choice logsums for the synthetic population.
	 * 
	 * @param propertyMap
	 */
	public void createWorkLogsums(){
        
        jppfClient = new JPPFClient();

        if (aggAcc == null)
        {
            logger.info("creating Accessibilities Object for UWSL.");
            aggAcc = BuildAccessibilities.getInstance();
            aggAcc.setupBuildAccessibilities(propertyMap,false);
//            aggAcc.setJPPFClient(jppfClient);
            
            aggAcc.calculateSizeTerms();
            aggAcc.calculateConstants();
          
            boolean readAccessibilities = ResourceUtil.getBooleanProperty(resourceBundle, "acc.read.input.file");
            if (readAccessibilities)
            {
            	String projectDirectory = Util.getStringValueFromPropertyMap(propertyMap,"Project.Directory");
                String accFileName = Paths.get(projectDirectory,Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file")).toString();

                aggAcc.readAccessibilityTableFromFile(accFileName);

            } else
            {

                aggAcc.calculateDCUtilitiesDistributed(propertyMap);

            }
        }

        // new the usual school and location choice model object
        UsualWorkSchoolLocationChoiceModel usualWorkSchoolLocationChoiceModel = new UsualWorkSchoolLocationChoiceModel(
                resourceBundle, "none", jppfClient, modelStructure, ms, dmuFactory, aggAcc);

        // calculate and get the array of worker size terms table - MGRAs by
        // occupations
        aggAcc.createWorkSegmentNameIndices();
        aggAcc.calculateWorkerSizeTerms();
        double[][] workerSizeTerms = aggAcc.getWorkerSizeTerms();

        // run the model
        logger.info("Starting usual work location choice for logsum calculations.");
        usualWorkSchoolLocationChoiceModel.runWorkLocationChoiceModel(householdDataManager, workerSizeTerms);
        logger.info("Finished with usual work location choice for logsum calculations.");

		logger.info("There are " + householdDataManager.getNumHouseholds()+" households in hh manager after running school and work location choice");

	}
	
	public void createNonWorkLogsums(){
		
		logger.info("There are " + householdDataManager.getNumHouseholds()+" households in hh manager before running non-work logsums");

        HouseholdChoiceModelRunner runner = new HouseholdChoiceModelRunner( propertyMap, jppfClient, "False", householdDataManager, ms, modelStructure, dmuFactory );
        runner.runHouseholdChoiceModels();

	}
	

	
	/**
	 * Create the household data manager. Based on the code in MTCTM2TourBasedModel.runTourBasedModel() 
	 * @return The household data manager interface.
	 */
	public HouseholdDataManagerIf getHouseholdDataManager( ){

		
        boolean localHandlers = false;

       String testString;
 
        HouseholdDataManagerIf householdDataManager;
        String hhHandlerAddress = "";
        int hhServerPort = 0;
        try
        {
            // get household server address. if none is specified a local server in
            // the current process will be started.
            hhHandlerAddress = resourceBundle.getString("RunModel.HouseholdServerAddress");
            try
            {
                // get household server port.
                hhServerPort = Integer.parseInt(resourceBundle.getString("RunModel.HouseholdServerPort"));
                localHandlers = false;
            } catch (MissingResourceException e)
            {
                // if no household data server address entry is found, the object
                // will be created in the local process
                localHandlers = true;
            }
        } catch (MissingResourceException e)
        {
            localHandlers = true;
        }


        try
        {

            if (localHandlers)
            {

                // create a new local instance of the household array manager
                householdDataManager = new SandagHouseholdDataManager();
                householdDataManager.setPropertyFileValues(propertyMap);

                // have the household data manager read the synthetic population
                // files and apply its tables to objects mapping method.
                String inputHouseholdFileName = resourceBundle.getString(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_HH);
                String inputPersonFileName = resourceBundle.getString(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_PERS);
                householdDataManager.setHouseholdSampleRate(iterationSampleRate, sampleSeed);
                householdDataManager.setupHouseholdDataManager(modelStructure, inputHouseholdFileName, inputPersonFileName);

            } else
            {

                householdDataManager = new HouseholdDataManagerRmi(hhHandlerAddress, hhServerPort,
                        SandagHouseholdDataManager.HH_DATA_SERVER_NAME);
                testString = householdDataManager.testRemote();
                logger.info("HouseholdDataManager test: " + testString);

                householdDataManager.setPropertyFileValues(propertyMap);
            }
		
            //always starting from scratch (RunModel.RestartWithHhServer=none)
            householdDataManager.setDebugHhIdsFromHashmap();

            String inputHouseholdFileName = resourceBundle
                    .getString(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_HH);
            String inputPersonFileName = resourceBundle
                    .getString(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_PERS);
            householdDataManager.setHouseholdSampleRate(iterationSampleRate, sampleSeed);
            householdDataManager.setupHouseholdDataManager(modelStructure, inputHouseholdFileName, inputPersonFileName);

        }catch (Exception e)
        {

            logger.error(String
                    .format("Exception caught setting up household data manager."), e);
            throw new RuntimeException();

        }

        return householdDataManager;
	}
	
	
	/** 
	 * Start a new matrix server connection.
	 * 
	 * @param properties
	 */
	private void startMatrixServer(HashMap<String, String> properties) {
	        String serverAddress = (String) properties.get("RunModel.MatrixServerAddress");
	        int serverPort = new Integer((String) properties.get("RunModel.MatrixServerPort"));
	        logger.info("connecting to matrix server " + serverAddress + ":" + serverPort);

	        try{

	            MatrixDataManager mdm = MatrixDataManager.getInstance();
	            ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
	            ms.testRemote(Thread.currentThread().getName());
	            mdm.setMatrixDataServerObject(ms);

	        } catch (Exception e) {
	            logger.error("could not connect to matrix server", e);
	            throw new RuntimeException(e);

	        }

	    }
	
	
	public JPPFClient getJppfClient() {
		return jppfClient;
	}

	public void setJppfClient(JPPFClient jppfClient) {
		this.jppfClient = jppfClient;
	}

	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
	    int globalIterationNumber = -1;
	    float iterationSampleRate = -1.0f;
	    int sampleSeed = -1;
	        
	    ResourceBundle rb = null;

	    logger.info( String.format( "Generating Logsums from MTC Tour Based Model using CT-RAMP version %s, 22feb2011 build %s", CtrampApplication.VERSION, 2 ) );
	        
	    if ( args.length == 0 ) {
	    	logger.error( String.format( "no properties file base name (without .properties extension) was specified as an argument." ) );
	        return;
	    }
	    else {
	    	rb = ResourceBundle.getBundle( args[0] );

	        // optional arguments
	        for (int i=1; i < args.length; i++) {

	        	if (args[i].equalsIgnoreCase("-iteration")) {
	        		globalIterationNumber = Integer.parseInt( args[i+1] );
	                logger.info( String.format( "-iteration %d.", globalIterationNumber ) );
	        	}

	            if (args[i].equalsIgnoreCase("-sampleRate")) {
	            	iterationSampleRate = Float.parseFloat( args[i+1] );
	                logger.info( String.format( "-sampleRate %.4f.", iterationSampleRate ) );
	            }

	            if (args[i].equalsIgnoreCase("-sampleSeed")) {
	            	sampleSeed = Integer.parseInt( args[i+1] );
	                logger.info( String.format( "-sampleSeed %d.", sampleSeed ) );
	            }

	        }
	                
	        if ( globalIterationNumber < 0 ) {
	        	globalIterationNumber = 1;
	            logger.info( String.format( "no -iteration flag, default value %d used.", globalIterationNumber ) );
	        }

	        if ( iterationSampleRate < 0 ) {
	        	iterationSampleRate = 1;
	            logger.info( String.format( "no -sampleRate flag, default value %.4f used.", iterationSampleRate ) );
	        }

	        if ( sampleSeed < 0 ) {
	        	sampleSeed = 0;
	            logger.info( String.format( "no -sampleSeed flag, default value %d used.", sampleSeed ) );
	        }

	    }


	    String baseName;
	    if ( args[0].endsWith(".properties") ) {
	    	int index = args[0].indexOf(".properties");
	        baseName = args[0].substring(0, index);
	    }
	    else {
	    	baseName = args[0];
	    }


	    // create an instance of this class for main() to use.
	    CreateLogsums mainObject = new CreateLogsums(  args[0], globalIterationNumber, iterationSampleRate, sampleSeed );

	    // Create logsums
	    try {

	    	logger.info ("Creating logsums.");
	            mainObject.run();
	         
	        }
	        catch ( RuntimeException e ) {
	            logger.error ( "RuntimeException caught in com.pb.mtctm2.abm.reports.CreateLogsums.main() -- exiting.", e );
	            System.exit(2);
	        }


	        logger.info ("");
	        logger.info ("");
	        logger.info ("CreateLogsums finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes.");

	        System.exit(0);

	
	}

}
