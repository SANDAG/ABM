package org.sandag.abm.crossborder;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagTourBasedModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.crossborder.CrossBorderTourManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.sawdust.util.concurrent.DnCRecursiveAction;

public class CrossBorderModel
{
    public static final int         MATRIX_DATA_SERVER_PORT             = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET      = 0;
    public static final String      RUN_MODEL_CONCURRENT_PROPERTY_KEY   = "crossBorder.run.concurrent";
    public static final String      CONCURRENT_PARALLELISM_PROPERTY_KEY = "crossBorder.concurrent.parallelism";

    private static final Logger     LOGGER                              = Logger.getLogger(SandagTourBasedModel.class);
    private static final Object     INITIALIZATION_LOCK                 = new Object();

    private MatrixDataServerRmi     ms;
    private HashMap<String, String> rbMap;
    private AutoTazSkimsCalculator  tazDistanceCalculator;
    private MgraDataManager         mgraManager;
    private TazDataManager          tazManager;
    private CrossBorderTourManager  tourManager;

    private boolean                 seek;
    private int                     traceId;
    private double                  sampleRate                          = 1;

    /**
     * Constructor
     * 
     * @param rbMap
     */
    public CrossBorderModel(HashMap<String, String> rbMap)
    {
        this.rbMap = rbMap;

        synchronized (INITIALIZATION_LOCK)
        { // lock to make sure only one of
          // these actually initializes
          // things so we don't cross
          // threads
            mgraManager = MgraDataManager.getInstance(rbMap);
            tazManager = TazDataManager.getInstance(rbMap);
            tourManager = new CrossBorderTourManager(rbMap);
        }

        seek = Boolean.valueOf(Util.getStringValueFromPropertyMap(rbMap, "crossBorder.seek"));
        traceId = Integer.valueOf(Util.getStringValueFromPropertyMap(rbMap, "crossBorder.trace"));

    }

    // global variable used for reporting
    private static final AtomicInteger TOUR_COUNTER           = new AtomicInteger(0);
    private final AtomicBoolean        calculatorsInitialized = new AtomicBoolean(false);

    /**
     * Run the model for a subset of tours in an array of tours.
     * 
     * @param tours
     *            The array of tours.
     * @param start
     *            The starting index of the tours to process.
     * @param end
     *            The (exclusive) ending index of the tours to process.
     */
    private void runModel(CrossBorderTour[] tours, int start, int end)
    {
        CrossBorderModelStructure modelStructure = new CrossBorderModelStructure();
        CrossBorderDmuFactoryIf dmuFactory = new CrossBorderDmuFactory(modelStructure);

        if (!calculatorsInitialized.get())
        {
            // only let one thread in to initialize
            synchronized (calculatorsInitialized)
            {
                // if still not initialized, then this is the first in so do the
                // initialization (otherwise skip)
                if (!calculatorsInitialized.get())
                {
                    tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
                    tazDistanceCalculator.computeTazDistanceArrays();
                    calculatorsInitialized.set(true);
                }
            }
        }

        CrossBorderTourTimeOfDayChoiceModel todChoiceModel = new CrossBorderTourTimeOfDayChoiceModel(
                rbMap);
        CrossBorderStationDestChoiceModel destChoiceModel = new CrossBorderStationDestChoiceModel(
                rbMap, modelStructure, dmuFactory, tazDistanceCalculator);
        CrossBorderTourModeChoiceModel tourModeChoiceModel = new CrossBorderTourModeChoiceModel(rbMap, modelStructure, dmuFactory,
                		tazDistanceCalculator);

        CrossBorderTripModeChoiceModel tripModeChoiceModel = new CrossBorderTripModeChoiceModel(rbMap, modelStructure,
                dmuFactory, tazDistanceCalculator);
        destChoiceModel.calculateSizeTerms(dmuFactory);
        destChoiceModel.calculateTazProbabilities(dmuFactory);

        CrossBorderStopFrequencyModel stopFrequencyModel = new CrossBorderStopFrequencyModel(rbMap);
        CrossBorderStopPurposeModel stopPurposeModel = new CrossBorderStopPurposeModel(rbMap);

        CrossBorderStopTimeOfDayChoiceModel stopTodChoiceModel = new CrossBorderStopTimeOfDayChoiceModel(
                rbMap);
        CrossBorderStopLocationChoiceModel stopLocationChoiceModel = new CrossBorderStopLocationChoiceModel(
                rbMap, modelStructure, dmuFactory, tazDistanceCalculator);
        
        double[][] mgraSizeTerms = destChoiceModel.getMgraSizeTerms();
        double[][] tazSizeTerms = destChoiceModel.getTazSizeTerms();
        double[][][] mgraProbabilities = destChoiceModel.getMgraProbabilities();
        stopLocationChoiceModel.setMgraSizeTerms(mgraSizeTerms);
        stopLocationChoiceModel.setTazSizeTerms(tazSizeTerms);
        stopLocationChoiceModel.setMgraProbabilities(mgraProbabilities);
    	String purposeControlFileName = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory")+"input/crossBorder_tourPurpose_control.csv";       
        
    	for (int i = start; i < end; i++)
        {
            CrossBorderTour tour = tours[i];

            // sample tours
            double rand = tour.getRandom();
            if (rand > sampleRate) continue;

            int tourCount = TOUR_COUNTER.incrementAndGet();
            if (tourCount % 1000 == 0) LOGGER.info("Processing tour " + tourCount);

            if (seek && tour.getID() != traceId) continue;

            if (tour.getID() == traceId) tour.setDebugChoiceModels(true);

            todChoiceModel.calculateTourTOD(tour);
            destChoiceModel.chooseStationAndDestination(tour);
        	resetCrossingPurpose(purposeControlFileName,tour);
            tourModeChoiceModel.chooseTourMode(tour);
            stopFrequencyModel.calculateStopFrequency(tour);
            stopPurposeModel.calculateStopPurposes(tour);

            int outboundStops = tour.getNumberOutboundStops();
            int inboundStops = tour.getNumberInboundStops();

            // choose TOD for stops and location of each
            if (outboundStops > 0)
            {
                CrossBorderStop[] stops = tour.getOutboundStops();
                for (CrossBorderStop stop : stops)
                {
                    stopTodChoiceModel.chooseTOD(tour, stop);
                    stopLocationChoiceModel.chooseStopLocation(tour, stop);
                }
            }
            if (inboundStops > 0)
            {
                CrossBorderStop[] stops = tour.getInboundStops();
                for (CrossBorderStop stop : stops)
                {
                    stopTodChoiceModel.chooseTOD(tour, stop);
                    stopLocationChoiceModel.chooseStopLocation(tour, stop);
                }
            }

            // generate trips and choose mode for them
            CrossBorderTrip[] trips = new CrossBorderTrip[outboundStops + inboundStops + 2];
            int tripNumber = 0;

            // outbound stops
            if (outboundStops > 0)
            {
                CrossBorderStop[] stops = tour.getOutboundStops();
                for (CrossBorderStop stop : stops)
                {
                    // generate a trip to the stop and choose a mode for it
                    trips[tripNumber] = new CrossBorderTrip(tour, stop, true);
                    tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                    ++tripNumber;
                }
                // generate a trip from the last stop to the tour destination
                trips[tripNumber] = new CrossBorderTrip(tour, stops[stops.length - 1], false);
                tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                ++tripNumber;

            } else
            {
                // generate an outbound trip from the tour origin to the
                // destination and choose a mode
                trips[tripNumber] = new CrossBorderTrip(tour, true);
                tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                ++tripNumber;
            }

            // inbound stops
            if (inboundStops > 0)
            {
                CrossBorderStop[] stops = tour.getInboundStops();
                for (CrossBorderStop stop : stops)
                {
                    // generate a trip to the stop and choose a mode for it
                    trips[tripNumber] = new CrossBorderTrip(tour, stop, true);
                    tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                    ++tripNumber;
                }
                // generate a trip from the last stop to the tour origin
                trips[tripNumber] = new CrossBorderTrip(tour, stops[stops.length - 1], false);
                tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                ++tripNumber;
            } else
            {

                // generate an inbound trip from the tour destination to the
                // origin and choose a mode
                trips[tripNumber] = new CrossBorderTrip(tour, false);
                tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
                ++tripNumber;
            }

            // set the trips in the tour object
            tour.setTrips(trips);

        }

    }

    /**
     * This class is the divide-and-conquer action (void return task) for
     * running the cross-border model using the fork-join framework. The
     * divisible problem is an array of tours, and the actual work is the
     * {@link CrossBorderModel#runModel(CrossBorderTour[],int,int)} method,
     * applied to a section of the array.
     */
    private class CrossBorderModelAction
            extends DnCRecursiveAction
    {
        private final HashMap<String, String> rbMap;
        private final CrossBorderTour[]       tours;

        private CrossBorderModelAction(HashMap<String, String> rbMap, CrossBorderTour[] tours)
        {
            super(0, tours.length);
            this.rbMap = rbMap;
            this.tours = tours;
        }

        private CrossBorderModelAction(HashMap<String, String> rbMap, CrossBorderTour[] tours,
                long start, long length, DnCRecursiveAction next)
        {
            super(start, length, next);
            this.rbMap = rbMap;
            this.tours = tours;
        }

        @Override
        protected void computeAction(long start, long length)
        {
            runModel(tours, (int) start, (int) (start + length));
        }

        @Override
        protected DnCRecursiveAction getNextAction(long start, long length, DnCRecursiveAction next)
        {
            return new CrossBorderModelAction(rbMap, tours, start, length, next);
        }

        @Override
        protected boolean continueDividing(long length)
        {
            // if there are 3 extra tasks queued up, then start executing
            // if there are 1000 or less tours to process, then start executing
            // otherwise, keep dividing to build up tasks for the threads to
            // process
            return getSurplusQueuedTaskCount() < 3 && length > 1000;
        }
    }

    /**
     * Run visitor model.
     */
    public void runModel()
    {
        tourManager.generateCrossBorderTours();
        CrossBorderTour[] tours = tourManager.getTours();

        // get new keys to see if we want to run in concurrent mode, and the
        // parallelism
        // (defaults to single threaded and parallelism = # of processors)
        // note that concurrent can use up memory very quickly, so setting the
        // parallelism might be prudent
        boolean concurrent = rbMap.containsKey(RUN_MODEL_CONCURRENT_PROPERTY_KEY)
                && Boolean.valueOf(Util.getStringValueFromPropertyMap(rbMap,
                        RUN_MODEL_CONCURRENT_PROPERTY_KEY));
        int parallelism = rbMap.containsKey(CONCURRENT_PARALLELISM_PROPERTY_KEY) ? Integer
                .valueOf(Util.getStringValueFromPropertyMap(rbMap,
                        CONCURRENT_PARALLELISM_PROPERTY_KEY)) : Runtime.getRuntime()
                .availableProcessors();
                
        if (concurrent)
        { // use fork-join
            CrossBorderModelAction action = new CrossBorderModelAction(rbMap, tours);
            new ForkJoinPool(parallelism).execute(action);
            action.getResult(); // wait for finish
        } else
        { // single-threaded: call the model runner in this thread
            runModel(tours, 0, tours.length);
        }

        tourManager.writeOutputFile(rbMap);
        LOGGER.info("Cross Border Model successfully completed!");
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
            LOGGER.error(String.format(
                    "UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.",
                    serverAddress, serverPort), e);
            throw new RuntimeException();
        }

        try
        {
            ItemServer.bind(matrixServer, className);
        } catch (RemoteException e)
        {
            LOGGER.error(String.format(
                    "RemoteException. serverAddress = %s, serverPort = %d -- exiting.",
                    serverAddress, serverPort), e);
            throw new RuntimeException();
        }

        return matrixServer;

    }
    
    /**
     * Reset tour NB crossing purpose using purpose distribution by POE from 2011 survey
     */
    private void resetCrossingPurpose(String purposeControlFile, CrossBorderTour tour) {
    	int poe=tour.getPoe();
        double [][] purpDistributionByPoe=new double[5][CrossBorderModelStructure.NUMBER_CROSSBORDER_PURPOSES];
        
        // Read the distributions by poe
        for (int i=0; i<5; i++) {
        	purpDistributionByPoe[i] = tourManager.setPurposeDistribution(purposeControlFile,purpDistributionByPoe[i],i+3);
        }
        int purpose = tourManager.choosePurpose(tour.getRandom(), purpDistributionByPoe[poe]);
        tour.setPurpose((byte) purpose);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Runtime gfg = Runtime.getRuntime(); 
        long memory1; 
        // checking the total memeory 
        System.out.println("Total memory is: "+ gfg.totalMemory()); 
        // checking free memory 
        memory1 = gfg.freeMemory(); 
        System.out.println("Initial free memory at Xborder model: "+ memory1); 
        // calling the garbage collector on demand 
        gfg.gc(); 
        memory1 = gfg.freeMemory(); 
        System.out.println("Free memory after garbage "+ "collection: " + memory1); 

        String propertiesFile = null;
        HashMap<String, String> pMap;

        LOGGER.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        LOGGER.info(String.format("Running Cross-Border Model"));

        if (args.length == 0)
        {
            LOGGER.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
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

        LOGGER.info("Crossborder Model:"+String.format("-sampleRate %.4f.", sampleRate)+"-iteration  " + iteration);
        CrossBorderModel crossBorderModel = new CrossBorderModel(pMap);
        crossBorderModel.setSampleRate(sampleRate);

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
                    matrixServer = crossBorderModel.startMatrixServerProcess(matrixServerAddress,
                            serverPort, mt);
                    crossBorderModel.ms = matrixServer;
                } else
                {
                    crossBorderModel.ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    crossBorderModel.ms.testRemote("CrossBorderModel");

                    // these methods need to be called to set the matrix data
                    // manager in the matrix data server
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(crossBorderModel.ms);
                }

            }

        } catch (Exception e)
        {

            LOGGER.error(
                    String.format("exception caught running ctramp model components -- exiting."),
                    e);
            throw new RuntimeException();

        }

        crossBorderModel.runModel();

    }

}
