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
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.NonTransitUtilities;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.airport.AirportModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;

import com.pb.common.calculator.MatrixDataManager;
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
    
     protected PrintWriter writer;
    
    
    public OutputTapPairs(HashMap<String, String> propertyMap, String inputFile, String outputFile){
    	this.inputFile = inputFile;
    	this.outputFile = outputFile;
    	initialize(propertyMap);
    }

    /**
     * Initialize best path builders.
     * 
     * @param propertyMap A property map with relevant properties.
     */
	public void initialize(HashMap<String, String> propertyMap){
		
		logger.info("Initializing OutputTapPairs");
		
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
                "id,accessEgressMode,period,pTap,aTap,mainMode,utility\n");
        writer.print(headerString);

	}
	
	/**
	 * Iterate through input data and process\write taps.
	 */
	private void run(){
		
		//iterate through data and calculate
		for(int row = 1; row<=inputDataTable.getRowCount();++row ){
		
			if((row<=10) || ((row % 100) == 0))
				logger.info("Processing input record "+row);
			
			String label=inputDataTable.getStringValueAt(row, "ID");
			int originMaz = (int) inputDataTable.getValueAt(row, "OMAZ");
			int destinationMaz = (int) inputDataTable.getValueAt(row, "DMAZ");
			int period = (int) inputDataTable.getValueAt(row, "TIMEPERIOD") - 1; //Input is 1=EA, 2=AM, 3=MD, 4=PM, 5=EV
			int accessEgressMode = (int) inputDataTable.getValueAt(row, "ACC_EGR_MODE_SEQ"); //1=WTW, 2=DTW, 3=WTD
			
			if(originMaz==0||destinationMaz==0||period==0||accessEgressMode==0)
				continue;
		
			if(accessEgressMode==1)
				bestPathCalculator.writeAllWalkTransitWalkTaps(period, originMaz, destinationMaz, logger, writer, label);
			else if(accessEgressMode==2)
				bestPathCalculator.writeAllDriveTransitWalkTaps(period, originMaz, destinationMaz, logger, writer, label);
			else
				bestPathCalculator.writeAllWalkTransitDriveTaps(period, originMaz, destinationMaz, logger, writer, label);
	
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

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
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

        String matrixServerAddress = "";
        int serverPort = 0;
        try
        {
            // get matrix server address. if "none" is specified, no server will
            // be
            // started, and matrix io will ocurr within the current process.
            matrixServerAddress = Util.getStringValueFromPropertyMap(pMap,
                    "RunModel.MatrixServerAddress");
            try
            {
                // get matrix server port.
                serverPort = Util.getIntegerValueFromPropertyMap(pMap, "RunModel.MatrixServerPort");
            } catch (MissingResourceException e)
            {
                // if no matrix server address entry is found, leave undefined
                // --
                // it's eithe not needed or show could create an error.
            }
        } catch (MissingResourceException e)
        {
            // if no matrix server address entry is found, set to localhost, and
            // a
            // separate matrix io process will be started on localhost.
            matrixServerAddress = "localhost";
            serverPort = MATRIX_DATA_SERVER_PORT;
        }

        MatrixDataServerRmi matrixServer = null;
        String matrixTypeName = Util.getStringValueFromPropertyMap(pMap, "Results.MatrixType");
        MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

        try
        {

            if (!matrixServerAddress.equalsIgnoreCase("none"))
            {

                if (matrixServerAddress.equalsIgnoreCase("localhost"))
                {
                    matrixServer = outputTapPairs.startMatrixServerProcess(matrixServerAddress,
                            serverPort, mt);
                    outputTapPairs.ms = matrixServer;
                } else
                {
                	outputTapPairs.ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                	outputTapPairs.ms.testRemote("OutputTapPairs");
                	outputTapPairs.ms.start32BitMatrixIoServer(mt, "OutputTapPairs");

                    // these methods need to be called to set the matrix data
                    // manager in the matrix data server
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(outputTapPairs.ms);
                }

            }

        } catch (Exception e)
        {

            if (matrixServerAddress.equalsIgnoreCase("localhost"))
            {
                matrixServer.stop32BitMatrixIoServer();
            }
            logger.error(
                    String.format("exception caught running ctramp model components -- exiting."),
                    e);
            throw new RuntimeException();

        }

        outputTapPairs.run();

        // if a separate process for running matrix data mnager was started,
        // we're
        // done with it, so close it.
        if (matrixServerAddress.equalsIgnoreCase("localhost"))
        {
            matrixServer.stop32BitMatrixIoServer();
        } else
        {
            if (!matrixServerAddress.equalsIgnoreCase("none"))
            	outputTapPairs.ms.stop32BitMatrixIoServer("AirportModel");
        }

    
	}

}
