package org.sandag.abm.survey;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;
import org.sandag.abm.modechoice.Modes;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

/**
 * This class reads and processes on-board transit survey data. It writes out 
 * all TAP-pairs given the origin/destination MAZ, the time period, and the 
 * access/egress mode sequence for the observation.
 * 
 * @author joel.freedman
 *
 */
public class OutputTapPairs {
	private static final Logger logger = Logger.getLogger(OutputTapPairs.class);
    private BestTransitPathCalculator         bestPathCalculator;
    protected WalkTransitWalkSkimsCalculator  wtw;
    protected WalkTransitDriveSkimsCalculator wtd;
    protected DriveTransitWalkSkimsCalculator dtw;
    public static final int         MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET = 0;
    private MatrixDataServerRmi     ms;
    private String inputFile;
    private String outputFile;
    private TableDataSet inputDataTable;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    private HashMap<Integer,Integer> sequentialMaz;
    
     protected PrintWriter writer;
    
    
    public OutputTapPairs(HashMap<String, String> propertyMap, String inputFile, String outputFile){
    	this.inputFile = inputFile;
    	this.outputFile = outputFile;
    	
    	startMatrixServer(propertyMap);
    	initialize(propertyMap);
    }
    
    
    /**
     * Initialize best path builders.
     * 
     * @param propertyMap A property map with relevant properties.
     */
	public void initialize(HashMap<String, String> propertyMap){
		
		logger.info("Initializing OutputTapPairs");
	    mgraManager = MgraDataManager.getInstance(propertyMap);
	    tazManager = TazDataManager.getInstance(propertyMap);

        bestPathCalculator = new BestTransitPathCalculator(propertyMap);

        wtw = new WalkTransitWalkSkimsCalculator(propertyMap);
        wtw.setup(propertyMap, logger, bestPathCalculator);
        wtd = new WalkTransitDriveSkimsCalculator(propertyMap);
        wtd.setup(propertyMap, logger, bestPathCalculator);
        dtw = new DriveTransitWalkSkimsCalculator(propertyMap);
        dtw.setup(propertyMap, logger, bestPathCalculator);
  
        readData();
        createOutputFile();

	}
	
	/**
	 * Read data into inputDataTable tabledataset.
	 * 
	 */
	private void readData(){
		
		logger.info("Begin reading the data in file " + inputFile);
	    
	    try
	    {
	    	OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
	        inputDataTable = csvFile.readFile(new File(inputFile));
	    } catch (IOException e)
	    {
	    	throw new RuntimeException(e);
        }
        logger.info("End reading the data in file " + inputFile);
	}
	
	/**
	 * Create the output file and write a header record.
	 */
	private void createOutputFile(){
        
		logger.info("Creating file " + outputFile);
		
		try
        {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + outputFile + " for writing\n");
            throw new RuntimeException();
        }
        String headerString = new String(
                "rownames,npath,access_mode_recode,period,set,boardTap,alightTap,bestUtility,accessTime,egressTime,auxWalkTime,"
                + "localBusIvt,expressBusIvt,brtIvt,lrtIvt,crIvt,firstWaitTime,trfWaitTime,fare,totalIVT,xfers\n");
        
        
        writer.print(headerString);

	}
	

	/**
	 * Iterate through input data and process\write taps.
	 */
	private void run(){
		
		TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	double[][] bestTaps = null;
		double[] skims = null;
		double boardAccessTime;
		double alightAccessTime;
		//iterate through data and calculate
		for(int row = 1; row<=inputDataTable.getRowCount();++row ){
		
			if((row<=100) || ((row % 100) == 0))
				logger.info("Processing input record "+row);
			
			String label=inputDataTable.getStringValueAt(row, "id");
			int originMaz = (int) inputDataTable.getValueAt(row, "orig_maz");
			int destinationMaz = (int) inputDataTable.getValueAt(row, "dest_maz");
			int period = (int) inputDataTable.getValueAt(row, "period")-1; //Input is 1=EA, 2=AM, 3=MD, 4=PM, 5=EV
			int accessMode = (int) inputDataTable.getValueAt(row, "accessEgress"); // 1 walk, 2 PNR, 3 KNR\bike
			int inbound = (int) inputDataTable.getValueAt(row, "inbound"); // 1 if inbound, else 0
			
			int accessEgressMode = -1;
			
			if(accessMode ==1) 
				accessEgressMode=bestPathCalculator.WTW;
			else if ((accessMode == 2||accessMode==3) && inbound==0)
				accessEgressMode = bestPathCalculator.DTW;
			else if ((accessMode == 2||accessMode==3) && inbound==1)
				accessEgressMode = bestPathCalculator.WTD;
			
			if(originMaz==0||destinationMaz==0||accessEgressMode==-1)
				continue;
			
			
			int originTaz = mgraManager.getTaz(originMaz);
			int destinationTaz = mgraManager.getTaz(destinationMaz);
		
			bestTaps = bestPathCalculator.getBestTapPairs(walkDmu, driveDmu, accessEgressMode, originMaz, destinationMaz, period, false, logger);
			double[] bestUtilities = bestPathCalculator.getBestUtilities();
			
			//iterate through n-best paths
	        for (int i = 0; i < bestTaps.length; i++)
	        {
	           	if(bestUtilities[i]<-500)
	           		continue;

	        	writer.print(label);
	    	
	        	//write transit TAP pairs and utility
	        	int boardTap = (int) bestTaps[i][0];
	        	int alightTap = (int) bestTaps[i][1];
	        	int set = (int) bestTaps[i][2];
	        	
	 	       writer.format(",%d,%d,%d,%d,%d,%d,%9.4f",i,accessMode,period,set,boardTap,alightTap,bestUtilities[i]);			
	        
	       // 	System.out.println(label+String.format(",%d,%d,%d,%d,%d,%d,%9.4f",i,accessEgressMode,period,set,boardTap,alightTap,bestUtilities[i]));
	        	//write skims
				if(accessEgressMode==bestPathCalculator.WTW){
                    boardAccessTime = mgraManager.getWalkTimeFromMgraToTap(originMaz,boardTap);
                    alightAccessTime = mgraManager.getWalkTimeFromMgraToTap(destinationMaz,alightTap);
					skims = wtw.getWalkTransitWalkSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, period, false); 
				}else if (accessEgressMode==bestPathCalculator.DTW){
					boardAccessTime = tazManager.getTimeToTapFromTaz(originTaz,boardTap,( accessMode==2? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                    alightAccessTime = mgraManager.getWalkTimeFromMgraToTap(destinationMaz,alightTap);
					skims = dtw.getDriveTransitWalkSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, period, false); 
				}else if(accessEgressMode==bestPathCalculator.WTD){
                    boardAccessTime = mgraManager.getWalkTimeFromMgraToTap(originMaz,boardTap);
                    alightAccessTime = tazManager.getTimeToTapFromTaz(destinationTaz,alightTap,( accessMode==2? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                    skims = wtd.getWalkTransitDriveSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, period, false); 
				}
	        	
				for(int j=0; j < skims.length; ++j)
					writer.format(",%9.2f",skims[j]);	
				
				writer.format("\n");
			
			}
	        writer.flush();
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

        logger.info(String.format("Best Tap Pairs Program using CT-RAMP version ",
                CtrampApplication.VERSION));

        logger.info(String.format("Outputting TAP pairs and utilities for on-board survey data"));

        
        String inputFile = null;
        String outputFile = null;
        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else {
        	propertiesFile = args[0];

	        for (int i = 1; i < args.length; ++i)
	        {
	            if (args[i].equalsIgnoreCase("-inputFile"))
	            {
	                inputFile = args[i + 1];
	            }
	            if (args[i].equalsIgnoreCase("-outputFile"))
	            {
	                outputFile = args[i + 1];
	            }
	        }
        }
        
        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        OutputTapPairs outputTapPairs = new OutputTapPairs(pMap, inputFile, outputFile);

   
        outputTapPairs.run();

  

    
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

}
