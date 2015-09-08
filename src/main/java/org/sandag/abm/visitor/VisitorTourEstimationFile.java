package org.sandag.abm.visitor;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagTourBasedModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

public class VisitorTourEstimationFile
{

    public static final int            MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int            MATRIX_DATA_SERVER_PORT_OFFSET = 0;

    private MatrixDataServerRmi        ms;
    private String                     inputFileName;
    private String                     outputFileName;
    private TableDataSet               estimationData;

    private VisitorModelStructure      myModelStructure;
    private VisitorDmuFactoryIf        dmuFactory;
    private McLogsumsCalculator        logsumsCalculator;
    private VisitorTourModeChoiceModel tourModeChoiceModel;
    private HashMap<String, String>    rbMap;
    private AutoTazSkimsCalculator     tazDistanceCalculator;

    private static Logger              logger                         = Logger.getLogger(SandagTourBasedModel.class);
    private static final int           SAMPLE_SIZE                    = 30;
    private MgraDataManager            mgraManager;

    /**
     * Default constructor
     */
    public VisitorTourEstimationFile(HashMap<String, String> propertyMap)
    {
        this.rbMap = propertyMap;
        mgraManager = MgraDataManager.getInstance(propertyMap);
        myModelStructure = new VisitorModelStructure();

        dmuFactory = new VisitorDmuFactory(myModelStructure);

    }

    public void createEstimationFile()
    {
        tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        logsumsCalculator = new McLogsumsCalculator();
        logsumsCalculator.setupSkimCalculators(rbMap);
        logsumsCalculator.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        tourModeChoiceModel = new VisitorTourModeChoiceModel(rbMap, myModelStructure, dmuFactory,
                logsumsCalculator);

        // open file
        estimationData = openFile(inputFileName);

        // iterate through file and calculate logsums
        calculateTourMCLogsums();

        // write the file
        writeFile(outputFileName, estimationData);
    }

    /**
     * Open a trip file and return the Tabledataset.
     * 
     * @fileName The name of the trip file
     * @return The tabledataset
     */
    public TableDataSet openFile(String fileName)
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
     * Write the file to disk.
     * 
     * @param fileName
     *            Name of file
     * @param data
     *            TableDataSet to write
     */
    public void writeFile(String fileName, TableDataSet data)
    {
        logger.info("Begin writing the data to file " + fileName);

        try
        {
            CSVFileWriter csvFile = new CSVFileWriter();
            csvFile.writeFile(data, new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        logger.info("End writing the data to file " + fileName);

    }

    /**
     * Calculate mode choice logsums
     * 
     * TOURNUM: Unique ID SURVNUM: Number associated with traveler PersonType: 0
     * - Business, 1 - Personal Persons: Number of persons (in addition to
     * traveler) on tour HHIncome: 1= $0-29,999; 2 = 30,000-59,999; 3 =
     * 60,000-99,999; 4 = 100,000-149,000; 5=$150,000 or more; 6 = DK/Refused
     * AutoAvail: 0 - No auto, 1 - Auto Available PURPOSE: 1 - Work, 2 - Other
     * (non-dining) 3 - Dining originMGRA - origin of tour (hotel/overnight
     * location) destMGRA - primary destination MGRA PeriodDepart: Period of
     * Tour Departure from hotel/overnight location PeriodArrive: Period of Tour
     * Arrival at primary destination SAMPLE_: 1:30 sampled alternatives
     * 
     */
    public void calculateTourMCLogsums()
    {

        int[] sample = new int[SAMPLE_SIZE];
        float[][] sampleLogsum = new float[SAMPLE_SIZE][estimationData.getRowCount()];
        float[] chosenLogsum = new float[estimationData.getRowCount()];
        String fieldName = null;

        for (int i = 0; i < estimationData.getRowCount(); ++i)
        {

            if ((i + 1) <= 10 || (i + 1) % 100 == 0)
            {
                logger.info("Processing record " + (i + 1));
            }
            int ID = (int) estimationData.getValueAt(i + 1, "TOURNUM");
            byte segment = (byte) estimationData.getValueAt(i + 1, "PersonType");
            byte purpose = (byte) estimationData.getValueAt(i + 1, "PURPOSE");
            byte income = (byte) estimationData.getValueAt(i + 1, "HHIncome");
            byte autoAvailable = (byte) estimationData.getValueAt(i + 1, "AutoAvail");
            byte participants = (byte) (estimationData.getValueAt(i + 1, "Persons") + 1);
            int departTime = (int) estimationData.getValueAt(i + 1, "PeriodDepart");
            int arriveTime = (int) estimationData.getValueAt(i + 1, "PeriodArrive");
            int originMGRA = (int) estimationData.getValueAt(i + 1, "originMGRA");
            int destinationMGRA = (int) estimationData.getValueAt(i + 1, "destMGRA");

            for (int j = 0; j < SAMPLE_SIZE; ++j)
            {
                fieldName = "SAMPLE_" + new Integer(j + 1).toString();
                sample[j] = (int) estimationData.getValueAt(i + 1, fieldName);
            }

            VisitorTour tour = new VisitorTour(ID + 10000);
            tour.setID(ID);
            tour.setSegment(segment);
            tour.setPurpose(purpose);
            tour.setIncome(income);
            tour.setAutoAvailable(autoAvailable);
            tour.setNumberOfParticipants(participants);
            tour.setDepartTime(departTime);
            tour.setArriveTime(arriveTime);
            tour.setOriginMGRA(originMGRA);

            if ((i + 1) == 1 || (i + 1) == 500) tour.setDebugChoiceModels(true);
            else tour.setDebugChoiceModels(false);

            double logsum = 0;
            // for each sampled destination
            for (int j = 0; j < SAMPLE_SIZE; ++j)
            {

                tour.setDestinationMGRA(sample[j]);

                // some of the samples are 0
                if (sample[j] > 0 && originMGRA > 0)
                {
                    logsum = tourModeChoiceModel.getModeChoiceLogsum(tour, logger,
                            "DCEstimationFileLogsum", "ID " + ID);
                    sampleLogsum[j][i] = (float) logsum;
                }
            }

            // for the chosen destination
            tour.setDestinationMGRA(destinationMGRA);
            if (originMGRA > 0 && destinationMGRA > 0)
                logsum = tourModeChoiceModel.getModeChoiceLogsum(tour, logger,
                        "DCEstimationFileLogsum", "ID " + ID);
            chosenLogsum[i] = (float) logsum;

        }

        // append the logsum fields to the tabledata
        estimationData.appendColumn(chosenLogsum, "CHSN_LS");

        for (int j = 0; j < SAMPLE_SIZE; ++j)
        {
            fieldName = "SAMPLE_" + (j + 1) + "_LS";
            estimationData.appendColumn(sampleLogsum[j], fieldName);
        }

    }

    /**
     * Write the destination choice estimation file with logsums appended.
     */
    public void writeDCEstimationFile(String name)
    {

    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running Visitor Tour Estimation File Model"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        String inFile = args[1];
        String outFile = args[2];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        VisitorTourEstimationFile visitorEstimationFile = new VisitorTourEstimationFile(pMap);

        visitorEstimationFile.inputFileName = inFile;
        visitorEstimationFile.outputFileName = outFile;

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
                    matrixServer = visitorEstimationFile.startMatrixServerProcess(
                            matrixServerAddress, serverPort, mt);
                    visitorEstimationFile.ms = matrixServer;
                } else
                {
                    visitorEstimationFile.ms = new MatrixDataServerRmi(matrixServerAddress,
                            serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    visitorEstimationFile.ms.testRemote(Thread.currentThread().getName());
                    visitorEstimationFile.ms.start32BitMatrixIoServer(mt);

                    // these methods need to be called to set the matrix data
                    // manager in the matrix data server
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(visitorEstimationFile.ms);
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

        visitorEstimationFile.createEstimationFile();

        // if a separate process for running matrix data mnager was started,
        // we're
        // done with it, so close it.
        if (matrixServerAddress.equalsIgnoreCase("localhost"))
        {
            matrixServer.stop32BitMatrixIoServer();
        } else
        {
            if (!matrixServerAddress.equalsIgnoreCase("none"))
                visitorEstimationFile.ms.stop32BitMatrixIoServer();
        }

        if (args.length < 3)
        {
            System.out
                    .println("Error: please specifiy inputFileName and outputFileName on command line");
            throw new RuntimeException();
        }

    }

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

}
