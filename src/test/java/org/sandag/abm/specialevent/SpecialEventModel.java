package org.sandag.abm.specialevent;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.application.SandagTourBasedModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

/**
 * Trips for special events.
 * 
 * This programs models trips to special events using the Event Model framework.
 * The Event Model framework generates and distributes trips. These trips are
 * put through a mode choice model. User benefits are optionally calculated and
 * written to a SUMMIT formatted file.
 * 
 */
public final class SpecialEventModel
{

    public static final int         MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET = 0;

    private MatrixDataServerRmi     ms;
    private static Logger           logger                         = Logger.getLogger(SandagTourBasedModel.class);
    private HashMap<String, String> rbMap;
    private McLogsumsCalculator     logsumsCalculator;
    private AutoTazSkimsCalculator  tazDistanceCalculator;
    private MgraDataManager         mgraManager;
    private TazDataManager          tazManager;
    private boolean                 seek;
    private int                     traceId;

    private TableDataSet            eventData;
    private double                  sampleRate                          = 1;

    /**
     * Default Constructor.
     */
    private SpecialEventModel(HashMap<String, String> rbMap)
    {
        this.rbMap = rbMap;
        mgraManager = MgraDataManager.getInstance(rbMap);
        tazManager = TazDataManager.getInstance(rbMap);

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");

        // read the event data
        String eventFile = Util.getStringValueFromPropertyMap(rbMap, "specialEvent.event.file");
        eventFile = directory + eventFile;
        eventData = readFile(eventFile);

        seek = new Boolean(Util.getStringValueFromPropertyMap(rbMap, "specialEvent.seek"));
        traceId = new Integer(Util.getStringValueFromPropertyMap(rbMap, "specialEvent.trace"));

    }

    /**
     * Read the file and return the TableDataSet.
     * 
     * @param fileName
     * @return data
     */
    private TableDataSet readFile(String fileName)
    {

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet data;
        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            data = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);
        return data;
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
    public void setSampleRate(double sampleRate)
    {
        this.sampleRate = sampleRate;
    }

    /**
     * Run the Event Model.
     * 
     * The Event Model is run for each mgra specified in the events.file
     */
    public void runModel()
    {

        SandagModelStructure modelStructure = new SandagModelStructure();

        SpecialEventDmuFactoryIf dmuFactory = new SpecialEventDmuFactory(modelStructure);

        SpecialEventTourManager tourManager = new SpecialEventTourManager(rbMap, eventData);

        tourManager.generateTours();
        SpecialEventTour[] tours = tourManager.getTours();

        tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        logsumsCalculator = new McLogsumsCalculator();
        logsumsCalculator.setupSkimCalculators(rbMap);
        logsumsCalculator.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());
        SpecialEventOriginChoiceModel originChoiceModel = new SpecialEventOriginChoiceModel(rbMap,
                dmuFactory, eventData);
        originChoiceModel.calculateSizeTerms(dmuFactory);
        originChoiceModel.calculateTazProbabilities(dmuFactory);

        SpecialEventTripModeChoiceModel tripModeChoiceModel = new SpecialEventTripModeChoiceModel(
                rbMap, modelStructure, dmuFactory, logsumsCalculator, eventData);

        // Run models for array of tours
        for (int i = 0; i < tours.length; ++i)
        {

            SpecialEventTour tour = tours[i];
            
            // Wu added for sampling tours
            double rand = tour.getRandom();
            if (rand > sampleRate) continue;

            if (i < 10 || i % 1000 == 0) logger.info("Processing tour " + (i + 1));

            if (seek && tour.getID() != traceId) continue;

            if (tour.getID() == traceId) tour.setDebugChoiceModels(true);
            originChoiceModel.chooseOrigin(tour);
            // generate trips and choose mode for them
            SpecialEventTrip[] trips = new SpecialEventTrip[2];
            int tripNumber = 0;

            // generate an outbound trip from the tour origin to the destination
            // and choose a mode
            trips[tripNumber] = new SpecialEventTrip(tour, true);
            tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
            ++tripNumber;

            // generate an inbound trip from the tour destination to the origin
            // and choose a mode
            trips[tripNumber] = new SpecialEventTrip(tour, false);
            tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
            ++tripNumber;

            // set the trips in the tour object
            tour.setTrips(trips);

        }

        tourManager.writeOutputFile(rbMap);

        logger.info("Special Event Model successfully completed!");

    }

    private MatrixDataServerRmi startMatrixServerProcess(String serverAddress, int serverPort,
            MatrixType mt)
    {

        String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;
        MatrixDataServerRmi matrixServer = new MatrixDataServerRmi(serverAddress, serverPort,
                MatrixDataServer.MATRIX_DATA_SERVER_NAME);

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
            throw new RuntimeException();
        }

        return matrixServer;

    }

    /**
     * Run Special Events Model.
     * 
     * The Special Events Model generates, distributes, and chooses modes for
     * attendees of sporting events and similar activities.
     */
    public static void main(String[] args)
    {
        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running Special Event Model"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        SpecialEventModel specialEventModel = new SpecialEventModel(pMap);
        
        //Wu added for sampling special event tours based on sample rate
        float sampleRate = 1.0f;
        int iteration = 1;

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
        logger.info("Special Event Model:"+String.format("-sampleRate %.4f.", sampleRate)+"-iteration  " + iteration);
        specialEventModel.setSampleRate(sampleRate);
        

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
                    matrixServer = specialEventModel.startMatrixServerProcess(matrixServerAddress,
                            serverPort, mt);
                    specialEventModel.ms = matrixServer;
                } else
                {
                    specialEventModel.ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    specialEventModel.ms.testRemote("SpecialEventModel");

                    // these methods need to be called to set the matrix data
                    // manager in the matrix data server
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(specialEventModel.ms);
                }

            }

        } catch (Exception e)
        {

            logger.error(
                    String.format("exception caught running ctramp model components -- exiting."),
                    e);
            throw new RuntimeException();

        }

        specialEventModel.runModel();
        
    }

}
