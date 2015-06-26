package org.sandag.abm.visitor;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

public class VisitorTripTables
{

    private static Logger           logger                  = Logger.getLogger("tripTables");
    public static final int         MATRIX_DATA_SERVER_PORT = 1171;

    private TableDataSet            tripData;

    // Some parameters
    private int[]                   modeIndex;                                               // an
                                                                                              // index
                                                                                              // array,
                                                                                              // dimensioned
                                                                                              // by
                                                                                              // number
                                                                                              // of
                                                                                              // total
                                                                                              // modes,
                                                                                              // returns
                                                                                              // 0=auto
                                                                                              // modes,
                                                                                              // 1=non-motor,
                                                                                              // 2=transit,
                                                                                              // 3=
                                                                                              // other
    private int[]                   matrixIndex;                                             // an
                                                                                              // index
                                                                                              // array,
                                                                                              // dimensioned
                                                                                              // by
                                                                                              // number
                                                                                              // of
                                                                                              // modes,
                                                                                              // returns
                                                                                              // the
                                                                                              // element
                                                                                              // of
                                                                                              // the
                                                                                              // matrix
                                                                                              // array
                                                                                              // to
                                                                                              // store
                                                                                              // value

    // array modes: AUTO, NON-MOTORIZED, TRANSIT, OTHER
    private int                     autoModes               = 0;
    private int                     tranModes               = 0;
    private int                     nmotModes               = 0;
    private int                     othrModes               = 0;

    // one file per time period
    private int                     numberOfPeriods;

    private HashMap<String, String> rbMap;

    // matrices are indexed by modes, vot bins, submodes
    private Matrix[][][]              matrix;

    private ResourceBundle          rb;
    private MgraDataManager         mgraManager;
    private TazDataManager          tazManager;
    private TapDataManager          tapManager;
    private SandagModelStructure    modelStructure;

    private MatrixDataServerRmi     ms;
    private float                   sampleRate;
    private static int iteration=1;
    private static final String VOT_THRESHOLD_LOW = "valueOfTime.threshold.low";
    private static final String VOT_THRESHOLD_MED = "valueOfTime.threshold.med";
    private float valueOfTimeThresholdLow = 0;
    private float valueOfTimeThresholdMed = 0;
    //value of time bins by mode group
    int[] votBins = {3,1,1,1};

    public VisitorTripTables(HashMap<String, String> rbMap)
    {

        this.rbMap = rbMap;
        tazManager = TazDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        modelStructure = new SandagModelStructure();

        // Time period limits
        numberOfPeriods = modelStructure.getNumberModelPeriods();

        // number of modes
        modeIndex = new int[modelStructure.MAXIMUM_TOUR_MODE_ALT_INDEX + 1];
        matrixIndex = new int[modeIndex.length];

        // set the mode arrays
        for (int i = 1; i < modeIndex.length; ++i)
        {
            if (modelStructure.getTourModeIsSovOrHov(i))
            {
                modeIndex[i] = 0;
                matrixIndex[i] = autoModes;
                ++autoModes;
            } else if (modelStructure.getTourModeIsNonMotorized(i))
            {
                modeIndex[i] = 1;
                matrixIndex[i] = nmotModes;
                ++nmotModes;
            } else if (modelStructure.getTourModeIsWalkTransit(i)
                    || modelStructure.getTourModeIsDriveTransit(i))
            {
                modeIndex[i] = 2;
                matrixIndex[i] = tranModes;
                ++tranModes;
            } else
            {
                modeIndex[i] = 3;
                matrixIndex[i] = othrModes;
                ++othrModes;
            }
        }
        //value of time thresholds
        valueOfTimeThresholdLow = new Float(rbMap.get(VOT_THRESHOLD_LOW));
        valueOfTimeThresholdMed = new Float(rbMap.get(VOT_THRESHOLD_MED));
    }

    /**
     * Initialize all the matrices for the given time period.
     * 
     * @param periodName
     *            The name of the time period.
     */
    public void initializeMatrices(String periodName)
    {

        /*
         * This won't work because external stations aren't listed in the MGRA
         * file int[] tazIndex = tazManager.getTazsOneBased(); int tazs =
         * tazIndex.length-1;
         */
        // Instead, use maximum taz number
        int maxTaz = tazManager.getMaxTaz();
        int[] tazIndex = new int[maxTaz + 1];

        // assume zone numbers are sequential
        for (int i = 1; i < tazIndex.length; ++i)
            tazIndex[i] = i;

        // get the tap index
        int[] tapIndex = tapManager.getTaps();
        int taps = tapIndex.length - 1;

        // Initialize matrices; one for each mode group (auto, non-mot, tran,
        // other)
        // All matrices will be dimensioned by TAZs except for transit, which is
        // dimensioned by TAPs
        int numberOfModes = 4;
        matrix = new Matrix[numberOfModes][][];
        for (int i = 0; i < numberOfModes; ++i)
        {

            String modeName;

            matrix[i] = new Matrix[votBins[i]][];
            
        	for(int j = 0; j< votBins[i];++j){
        		if (i == 0)
        		{
        			matrix[i][j] = new Matrix[autoModes];
        			for (int k = 0; k < autoModes; ++k)
        			{
        				modeName = modelStructure.getModeName(k + 1);
        				matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
        				matrix[i][j][k].setExternalNumbers(tazIndex);
        			}
        		} else if (i == 1)
        		{
        			matrix[i][j] = new Matrix[nmotModes];
        			for (int k = 0; k < nmotModes; ++k)
        			{
        				modeName = modelStructure.getModeName(k + 1 + autoModes);
        				matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
        				matrix[i][j][k].setExternalNumbers(tazIndex);
        			}
        		} else if (i == 2)
        		{
        			matrix[i][j] = new Matrix[tranModes];
        			for (int k = 0; k < tranModes; ++k)
        			{
        				modeName = modelStructure.getModeName(k + 1 + autoModes + nmotModes);
        				matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", taps, taps);
        				matrix[i][j][k].setExternalNumbers(tapIndex);
        			}
        		} else
        		{
        			matrix[i][j] = new Matrix[othrModes];
        			for (int k = 0; k < othrModes; ++k)
        			{
        				modeName = modelStructure
                            .getModeName(k + 1 + autoModes + nmotModes + tranModes);
        				matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
        				matrix[i][j][k].setExternalNumbers(tazIndex);
        			}
        		}
        	}
        }
    }

    /**
     * Create trip tables for all time periods and modes. This is the main entry
     * point into the class; it should be called after instantiating the
     * SandagTripTables object.
     * 
     */
    public void createTripTables(MatrixType mt)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "scenario.path");

        // Open the individual trip file
        String tripFile = Util.getStringValueFromPropertyMap(rbMap, "visitor.trip.output.file");
        tripData = openTripFile(directory + tripFile);

        // Iterate through periods so that we don't have to keep
        // trip tables for all periods in memory.
        for (int i = 0; i < numberOfPeriods; ++i)
        {

            // Initialize the matrices
            initializeMatrices(modelStructure.getModelPeriodLabel(i));

            // process trips
            processTrips(i, tripData);

            logger.info("Begin writing matrices");
            writeTrips(i, mt);
            logger.info("End writingMatrices");

        }

    }

    /**
     * Open a trip file and return the Tabledataset.
     * 
     * @fileName The name of the trip file
     * @return The tabledataset
     */
    public TableDataSet openTripFile(String fileName)
    {

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet tripData;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            tripData = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);
        return tripData;
    }

    /**
     * This is the main workhorse method in this class. It iterates over records
     * in the trip file. Attributes for the trip record are read, and the trip
     * record is accumulated in the relevant matrix.
     * 
     * @param timePeriod
     *            The time period to process
     * @param tripData
     *            The trip data file to process
     */
    public void processTrips(int timePeriod, TableDataSet tripData)
    {

        logger.info("Begin processing trips for period " + timePeriod);
        int valueOfTimeCol = tripData.getColumnPosition("valueOfTime");

        // iterate through the trip data and save trips in arrays
        for (int i = 1; i <= tripData.getRowCount(); ++i)
        {

            if (i <= 5 || i % 1000 == 0) logger.info("Reading record " + i);

            int departTime = (int) tripData.getValueAt(i, "period");
            int period = modelStructure.getModelPeriodIndex(departTime);
            if (period != timePeriod) continue;

            //value of time
            float valueOfTime = tripData.getValueAt(i,valueOfTimeCol);

            int originMGRA = (int) tripData.getValueAt(i, "originMGRA");
            int destinationMGRA = (int) tripData.getValueAt(i, "destinationMGRA");
            int tripMode = (int) tripData.getValueAt(i, "tripMode");

            // save taxi trips as shared-2
            if (tripMode == modelStructure.TAXI)
            {
                tripMode = 3;
            }
            int originTAZ = mgraManager.getTaz(originMGRA);
            int destinationTAZ = mgraManager.getTaz(destinationMGRA);
            boolean inbound = tripData.getBooleanValueAt(i, "inbound");

            // transit trip - get boarding and alighting tap
            int boardTap = 0;
            int alightTap = 0;

            if (modelStructure.getTourModeIsWalkTransit(tripMode)
                    || modelStructure.getTourModeIsDriveTransit(tripMode))
            {
                boardTap = (int) tripData.getValueAt(i, "boardingTAP");
                alightTap = (int) tripData.getValueAt(i, "alightingTAP");
            }

            // all person trips are 1 per party (for now)
            float personTrips = 1.0f / sampleRate;

            // all auto trips are 1 per party
            float vehicleTrips = 1.0f / sampleRate;

            // Store in matrix
            int mode = modeIndex[tripMode];
            int mat = matrixIndex[tripMode];
            int votBin=0;
            if(votBins[mode]>1)
            	votBin = getValueOfTimeBin(valueOfTime);

            if (mode == 0)
            {
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + vehicleTrips));
            } else if (mode == 1)
            {
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + personTrips));
            } else if (mode == 2)
            {

                if (boardTap == 0 || alightTap == 0) continue;

                float value = matrix[mode][votBin][mat].getValueAt(boardTap, alightTap);
                matrix[mode][votBin][mat].setValueAt(boardTap, alightTap, (value + personTrips));

                // Store PNR transit trips in SOV free mode skim (mode 0 mat 0)
                if (modelStructure.getTourModeIsDriveTransit(tripMode))
                {

                    // add the vehicle trip portion to the trip table
                    if (inbound)
                    { // from origin to lot (boarding tap)
                        int PNRTAZ = tapManager.getTazForTap(boardTap);
                        value = matrix[0][votBin][0].getValueAt(originTAZ, PNRTAZ);
                        matrix[0][votBin][0].setValueAt(originTAZ, PNRTAZ, (value + vehicleTrips));

                    } else
                    { // from lot (alighting tap) to destination
                        int PNRTAZ = tapManager.getTazForTap(alightTap);
                        value = matrix[0][votBin][0].getValueAt(PNRTAZ, destinationTAZ);
                        matrix[0][votBin][0].setValueAt(PNRTAZ, destinationTAZ, (value + vehicleTrips));
                    }

                }
            } else
            {
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + personTrips));
            }

            logger.info("End creating trip tables for period " + timePeriod);
        }
    }

    /**
     * Return the value of time bin 0 through 2 based on the thresholds provided in the property map
     * @param valueOfTime
     * @return value of time bin 0 through 2
     */
    public int getValueOfTimeBin(float valueOfTime){
    	
    	if(valueOfTime<valueOfTimeThresholdLow)
    		return 0;
    	else if (valueOfTime<valueOfTimeThresholdMed)
    		return 1;
    	else
    		return 2;
    }

    /**
     * Get the output trip table file names from the properties file, and write
     * trip tables for all modes for the given time period.
     * 
     * @param period
     *            Time period, which will be used to find the period time string
     *            to append to each trip table matrix file
     */
    public void writeTrips(int period, MatrixType mt)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "scenario.path");
        String per = modelStructure.getModelPeriodLabel(period);
        String[][] end = new String[4][];
        String[] fileName = new String[4];

        fileName[0] = directory
                + Util.getStringValueFromPropertyMap(rbMap, "visitor.results.autoTripMatrix");
        fileName[1] = directory
                + Util.getStringValueFromPropertyMap(rbMap, "visitor.results.nMotTripMatrix");
        fileName[2] = directory
                + Util.getStringValueFromPropertyMap(rbMap, "visitor.results.tranTripMatrix");
        fileName[3] = directory
                + Util.getStringValueFromPropertyMap(rbMap, "visitor.results.othrTripMatrix");

        //the end of the name depends on whether there are multiple vot bins or not
        String[] votBinName = {"low","med","high"};
        
        for(int i = 0; i<4;++i){
        	end[i] = new String[votBins[i]];
        	for(int j = 0; j < votBins[i];++j){
        		if(votBins[i]>1)
        			end[i][j] = "_" + per + "_"+ votBinName[j]+ ".mtx";
        		else
        			end[i][j] = "_" + per + ".mtx";
        	}
        }
        for (int i = 0; i < 4; ++i){
           	for(int j = 0; j < votBins[i];++j){
        		try
        		{
        			if (ms != null) ms.writeMatrixFile(fileName[i]+end[i][j], matrix[i][j], mt);
        			else writeMatrixFile(fileName[i]+end[i][j], matrix[i][j]);
        		} catch (Exception e)
        		{
        			logger.error("exception caught writing " + mt.toString() + " matrix file = "
                        + fileName[i] +end[i][j] + ", for mode index = " + i, e);
        			throw new RuntimeException();
        		}
        	}
        }

    }

    /**
     * Utility method to write a set of matrices to disk.
     * 
     * @param fileName
     *            The file name to write to.
     * @param m
     *            An array of matrices
     */
    public void writeMatrixFile(String fileName, Matrix[] m)
    {

        // auto trips
        MatrixWriter writer = MatrixWriter.createWriter(fileName);
        String[] names = new String[m.length];

        for (int i = 0; i < m.length; i++)
        {
            names[i] = m[i].getName();
            logger.info(m[i].getName() + " has " + m[i].getRowCount() + " rows, "
                    + m[i].getColumnCount() + " cols, and a total of " + m[i].getSum());
        }

        writer.writeMatrices(names, m);
    }

    /**
     * Start matrix server
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
     * @param args
     */
    public static void main(String[] args)
    {

        HashMap<String, String> pMap;
        String propertiesFile = null;

        logger.info(String.format(
                "SANDAG Visitor Model Trip Table Generation Program using CT-RAMP version %s",
                CtrampApplication.VERSION));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        float sampleRate = 1.0f;
        for (int i = 1; i < args.length; ++i)
        {
            if (args[i].equalsIgnoreCase("-sampleRate"))
            {
                sampleRate = Float.parseFloat(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-iteration"))
            {
                iteration = Integer.parseInt(args[i + 1]);
            }
        }
        logger.info("Visitor Model Trip Table:"+String.format("-sampleRate %.4f.", sampleRate)+"-iteration  " + iteration);
        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        VisitorTripTables tripTables = new VisitorTripTables(pMap);
        tripTables.setSampleRate(sampleRate);

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
                    matrixServer = tripTables.startMatrixServerProcess(matrixServerAddress,
                            serverPort, mt);
                    tripTables.ms = matrixServer;
                } else
                {
                    tripTables.ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    tripTables.ms.testRemote("VisitorTripTables");
                    tripTables.ms.start32BitMatrixIoServer(mt, "VisitorTripTables");

                    // mdm = MatrixDataManager.getInstance();
                    // mdm.setMatrixDataServerObject(ms);
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

        tripTables.createTripTables(mt);

        // if a separate process for running matrix data mnager was started,
        // we're
        // done with it, so close it.
        if (matrixServerAddress.equalsIgnoreCase("localhost"))
        {
            matrixServer.stop32BitMatrixIoServer();
        } else
        {
            if (!matrixServerAddress.equalsIgnoreCase("none"))
                tripTables.ms.stop32BitMatrixIoServer("VisitorTripTables");
        }
    }

    /**
     * @return the sampleRate
     */
    public double getSampleRate()
    {
        return sampleRate;
    }

    /**
     * @param sampleRate
     *            the sampleRate to set
     */
    public void setSampleRate(float sampleRate)
    {
        this.sampleRate = sampleRate;
    }

}
