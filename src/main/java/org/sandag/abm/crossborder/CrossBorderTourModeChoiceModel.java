package org.sandag.abm.crossborder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;

public class CrossBorderTourModeChoiceModel
        implements Serializable
{

    private transient Logger    logger           = Logger.getLogger("crossBorderModel");

    public static final boolean DEBUG_BEST_PATHS = false;

    private MgraDataManager     mgraManager;

    /**
     * A private class used as a key to store the wait times for a particular
     * station.
     * 
     * @author Freedman
     * 
     */
    private class WaitTimeClass
    {
        int[]   beginPeriod;   // by time periods
        int[]   endPeriod;     // by time periods
        float[] StandardWait;  // by time periods
        float[] SENTRIWait;    // by time periods
        float[] PedestrianWait; // by time periods
    }

    HashMap<Integer, WaitTimeClass>        waitTimeMap;

    private static final String            PROPERTIES_UEC_TOUR_MODE_CHOICE         = "crossBorder.tour.mc.uec.file";
    private static final String            PROPERTIES_UEC_TOUR_DATA_SHEET          = "crossBorder.tour.mc.data.page";
    private static final String            PROPERTIES_UEC_MANDATORY_MODEL_SHEET    = "crossBorder.tour.mc.mandatory.model.page";
    private static final String            PROPERTIES_UEC_NONMANDATORY_MODEL_SHEET = "crossBorder.tour.mc.nonmandatory.model.page";
    private static final String            PROPERTIES_POE_WAITTIMES                = "crossBorder.poe.waittime.file";

    private ChoiceModelApplication[]       mcModel;                                                                                // by
                                                                                                                                    // segment
                                                                                                                                    // -
                                                                                                                                    // mandatory
                                                                                                                                    // vs
                                                                                                                                    // non-mandatory
                                                                                                                                    // (each
                                                                                                                                    // has
                                                                                                                                    // different
                                                                                                                                    // nesting
                                                                                                                                    // coefficients)
    private CrossBorderTripModeChoiceModel tripModeChoiceModel;
    private CrossBorderTourModeChoiceDMU   mcDmuObject;
    private McLogsumsCalculator            logsumHelper;

    private CrossBorderModelStructure      modelStructure;

    private String                         tourCategory;

    private String[]                       modeAltNames;

    private boolean                        saveUtilsProbsFlag                      = false;

    double                                 logsum                                  = 0;

    // placeholders for calculation of logsums
    private CrossBorderTour                tour;
    private CrossBorderTrip                trip;

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public CrossBorderTourModeChoiceModel(HashMap<String, String> propertyMap,
            CrossBorderModelStructure myModelStructure, CrossBorderDmuFactoryIf dmuFactory,
            AutoTazSkimsCalculator tazDistanceCalculator)
    {

        mgraManager = MgraDataManager.getInstance(propertyMap);
        modelStructure = myModelStructure;
        
        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(propertyMap);
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        mcDmuObject = dmuFactory.getCrossBorderTourModeChoiceDMU();
        setupModeChoiceModelApplicationArray(propertyMap);

        // Create a trip mode choice model object for calculation of logsums
        tripModeChoiceModel = new CrossBorderTripModeChoiceModel(propertyMap, myModelStructure,
                dmuFactory, tazDistanceCalculator);

        tour = new CrossBorderTour(0);
        trip = new CrossBorderTrip();

        String directory = Util.getStringValueFromPropertyMap(propertyMap, "Project.Directory");
        String waitTimeFile = Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_POE_WAITTIMES);
        waitTimeFile = directory + waitTimeFile;

        readWaitTimeFile(waitTimeFile);
    }

    /**
     * Read UECs and create model application objects.
     * 
     * @param propertyMap
     */
    private void setupModeChoiceModelApplicationArray(HashMap<String, String> propertyMap)
    {

        logger.info(String
                .format("Setting up cross border tour (border crossing) mode choice model."));

        // locate the mandatory tour mode choice model UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_TOUR_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        logger.info("Will read mcUECFile " + mcUecFile);
        int dataPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_TOUR_DATA_SHEET));
        int mandatoryModelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_MANDATORY_MODEL_SHEET));
        int nonmandatoryModelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_NONMANDATORY_MODEL_SHEET));

        // default is to not save the tour mode choice utils and probs for each
        // tour
        String saveUtilsProbsString = propertyMap
                .get(CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS);
        if (saveUtilsProbsString != null)
        {
            if (saveUtilsProbsString.equalsIgnoreCase("true")) saveUtilsProbsFlag = true;
        }

        mcModel = new ChoiceModelApplication[2];

        mcModel[0] = new ChoiceModelApplication(mcUecFile, mandatoryModelPage, dataPage,
                propertyMap, (VariableTable) mcDmuObject);
        mcModel[1] = new ChoiceModelApplication(mcUecFile, nonmandatoryModelPage, dataPage,
                propertyMap, (VariableTable) mcDmuObject);

        modeAltNames = mcModel[0].getAlternativeNames();

    }

    /**
     * Get the Logsum.
     * 
     * @param tour
     * @param modelLogger
     * @param choiceModelDescription
     * @param decisionMakerLabel
     * @return
     */
    public double getLogsum(CrossBorderTour tour, Logger modelLogger,
            String choiceModelDescription, String decisionMakerLabel)
    {

        // set all tour mode DMU attributes including calculation of trip mode
        // choice logsums for inbound & outbound directions.
        setDmuAttributes(tour);

        return getModeChoiceLogsum(tour, modelLogger, choiceModelDescription, decisionMakerLabel);

    }

    /**
     * Set the tour mode choice attributes.
     * 
     * @param tour
     */
    public void setDmuAttributes(CrossBorderTour tour)
    {

        codeWaitTime(tour);
        setTripLogsums(tour);
    }

    /**
     * Code wait times in the mc dmu object.
     * 
     * @param tour
     *            The tour with an origin MGRA and departure time period.
     */
    public void codeWaitTime(CrossBorderTour tour)
    {

        // get the wait time class from the waitTimeMap HashMap
        int station = tour.getPoe();
        int period = tour.getDepartTime();
        WaitTimeClass wait = waitTimeMap.get(station);
        int[] beginTime = wait.beginPeriod;
        int[] endTime = wait.endPeriod;

        // iterate through time arrays, find corresponding row, and set wait
        // times
        for (int i = 0; i < beginTime.length; ++i)
        {
            if (period >= beginTime[i] && period <= endTime[i])
            {
                mcDmuObject.borderWaitStd = wait.StandardWait[i];
                mcDmuObject.borderWaitSentri = wait.SENTRIWait[i];
                mcDmuObject.borderWaitPed = wait.PedestrianWait[i];
                break;
            }
        }
    }

    /**
     * Set trip mode choice logsums (outbound and inbound) for calculation of
     * tour mode choice model.
     * 
     * @param tour
     *            The tour with other attributes such as origin, destination,
     *            purpose coded.
     */
    public void setTripLogsums(CrossBorderTour tour)
    {

        // outbound
        trip.initializeFromTour(tour, true);

        // DA logsum
        tour.setTourMode(modelStructure.DRIVEALONE);
        double logsumDAOut = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setOutboundTripMcLogsumDA(logsumDAOut);

        // S2 logsum
        tour.setTourMode(modelStructure.SHARED2);
        double logsumS2Out = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setOutboundTripMcLogsumSR2(logsumS2Out);

        // S2 logsum
        tour.setTourMode(modelStructure.SHARED3);
        double logsumS3Out = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setOutboundTripMcLogsumSR3(logsumS3Out);

        // walk logsum
        tour.setTourMode(modelStructure.WALK);
        double logsumWalkOut = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setOutboundTripMcLogsumWalk(logsumWalkOut);

        // inbound
        trip.initializeFromTour(tour, false);

        // DA logsum
        tour.setTourMode(modelStructure.DRIVEALONE);
        double logsumDAIn = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setInboundTripMcLogsumDA(logsumDAIn);

        // S2 logsum
        tour.setTourMode(modelStructure.SHARED2);
        double logsumS2In = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setInboundTripMcLogsumSR2(logsumS2In);

        // S2 logsum
        tour.setTourMode(modelStructure.SHARED3);
        double logsumS3In = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setInboundTripMcLogsumSR3(logsumS3In);

        // walk logsum
        tour.setTourMode(modelStructure.WALK);
        double logsumWalkIn = tripModeChoiceModel.computeUtilities(tour, trip);
        mcDmuObject.setInboundTripMcLogsumWalk(logsumWalkIn);

    }

    /**
     * Get an index into the mcModel array for the tour purpose.
     * 
     * @param tour
     * @return The index.
     */
    public int getModelIndex(CrossBorderTour tour)
    {
        int modelIndex = 1;
        if (tour.getPurpose() == modelStructure.WORK || tour.getPurpose() == modelStructure.SCHOOL)
            modelIndex = 0;
        return modelIndex;
    }

    /**
     * Get the tour mode choice logsum.
     * 
     * @param tour
     * @param modelLogger
     * @param choiceModelDescription
     * @param decisionMakerLabel
     * @return Tour mode choice logsum
     */
    public double getModeChoiceLogsum(CrossBorderTour tour, Logger modelLogger,
            String choiceModelDescription, String decisionMakerLabel)
    {
        setDmuAttributes(tour);

        int modelIndex = getModelIndex(tour);

        // log headers to traceLogger
        if (tour.getDebugChoiceModels())
        {

            mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);
        }

        mcModel[modelIndex].computeUtilities(mcDmuObject, mcDmuObject.getDmuIndexValues());

        double logsum = mcModel[modelIndex].getLogsum();

        // write UEC calculation results to separate model specific log file
        if (tour.getDebugChoiceModels())
        {
            String loggingHeader = String.format("%s   %s", choiceModelDescription,
                    decisionMakerLabel);
            mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
            modelLogger.info(choiceModelDescription + " Logsum value: " + logsum);
            modelLogger.info("");
            modelLogger.info("");
        }

        return logsum;

    }

    /**
     * Use to choose tour mode and set result in tour object. Also set value of time in tour object.
     * 
     * @param tour
     *            The crossborder tour
     */
    public void chooseTourMode(CrossBorderTour tour)
    {

        byte tourMode = (byte) getModeChoice(tour);
        tour.setTourMode(tourMode);
        
        float valueOfTime = tripModeChoiceModel.getValueOfTime(tourMode);
        tour.setValueOfTime(valueOfTime);
    }

    /**
     * Use to return the tour mode without setting in the tour object.
     * 
     * @param tour
     *            The cross border tour whose mode to choose.
     * @return An integer corresponding to the tour mode.
     */
    public int getModeChoice(CrossBorderTour tour)
    {
        int modelIndex = getModelIndex(tour);

        Logger modelLogger = null;
        modelLogger = logger;

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";

        if (tour.getDebugChoiceModels())
        {
            String purposeName = modelStructure.CROSSBORDER_PURPOSES[tour.getPurpose()];
            choiceModelDescription = String.format(
                    "%s Tour Mode Choice Model for: Purpose=%s, Origin=%d, Dest=%d", tourCategory,
                    purposeName, tour.getOriginMGRA(), tour.getDestinationMGRA());
            decisionMakerLabel = String.format(" tour ID =%d", tour.getID());
            loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);

            mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

            modelLogger.info(" ");
            for (int k = 0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info(loggingHeader);
            modelLogger.info(separator);

            tour.logTourObject(modelLogger, loggingHeader.length());
        }

        setDmuAttributes(tour);

        mcModel[modelIndex].computeUtilities(mcDmuObject, mcDmuObject.getDmuIndexValues());

        double rn = tour.getRandom();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen;
        if (mcModel[modelIndex].getAvailabilityCount() > 0)
        {

            chosen = mcModel[modelIndex].getChoiceResult(rn);

        } else
        {

            String purposeName = modelStructure.CROSSBORDER_PURPOSES[tour.getPurpose()];
            choiceModelDescription = String
                    .format("No alternatives available for %s Tour Mode Choice Model for: Purpose=%s, Orig=%d, Dest=%d",
                            tourCategory, purposeName, tour.getOriginMGRA(),
                            tour.getDestinationMGRA());
            decisionMakerLabel = String.format("TourId=%d", tour.getID());
            loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);

            mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

            modelLogger.info(" ");
            for (int k = 0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info(loggingHeader);
            modelLogger.info(separator);

            tour.logTourObject(modelLogger, loggingHeader.length());

            mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
            modelLogger.info("");
            modelLogger.info("");

            logger.error(String
                    .format("Exception caught for HHID=%d, no available %s tour mode alternatives to choose from in choiceModelApplication.",
                            tour.getID(), tourCategory));
            throw new RuntimeException();
        }

        // debug output
        if (tour.getDebugChoiceModels())
        {

            double[] utilities = mcModel[modelIndex].getUtilities(); // 0s-indexing
            double[] probabilities = mcModel[modelIndex].getProbabilities(); // 0s-indexing
            boolean[] availabilities = mcModel[modelIndex].getAvailabilities(); // 1s-indexing
            String[] altNames = mcModel[modelIndex].getAlternativeNames(); // 0s-indexing

            modelLogger.info("Tour Id: " + tour.getID());
            modelLogger
                    .info("Alternative                    Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------    --------------    --------------    --------------");

            double cumProb = 0.0;
            for (int k = 0; k < mcModel[modelIndex].getNumberOfAlternatives(); k++)
            {
                cumProb += probabilities[k];
                String altString = String.format("%-3d  %s", k + 1, altNames[k]);
                modelLogger.info(String.format("%-20s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[k + 1], utilities[k], probabilities[k], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("%-3d  %s", chosen, altNames[chosen - 1]);
            modelLogger.info(String.format("Choice: %s, with rn=%.8f", altString, rn));

            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            // write choice model alternative info to log file
            mcModel[modelIndex].logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            mcModel[modelIndex].logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn,
                    chosen);
            mcModel[modelIndex].logLogitCalculations(choiceModelDescription, decisionMakerLabel);

            // write UEC calculation results to separate model specific log file
            mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
        }

        if (saveUtilsProbsFlag)
        {

            // get the utilities and probabilities arrays for the tour mode
            // choice
            // model for this tour and save them to the tour object
            double[] dUtils = mcModel[modelIndex].getUtilities();
            double[] dProbs = mcModel[modelIndex].getProbabilities();

            float[] utils = new float[dUtils.length];
            float[] probs = new float[dUtils.length];
            for (int k = 0; k < dUtils.length; k++)
            {
                utils[k] = (float) dUtils[k];
                probs[k] = (float) dProbs[k];
            }

            // tour.setTourModalUtilities(utils);
            // tour.setTourModalProbabilities(probs);

        }

        return chosen;

    }

    /**
     * Read wait time file and store wait times in waitTimeMap HashMap.
     * 
     * @param fileName
     *            Name of file containing station, beginPeriod, endPeriod and
     *            wait time for standard, SENTRI, and pedestrians.
     */
    protected void readWaitTimeFile(String fileName)
    {
        logger.info("Begin reading the data in file " + fileName);
        TableDataSet waitTimeTable;
        waitTimeMap = new HashMap<Integer, WaitTimeClass>();

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            waitTimeTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        int rowCount = waitTimeTable.getRowCount();

        // iterate through file and fill up waitTimeMap
        int lastStation = -999;
        int index = 0;
        WaitTimeClass wait = null;
        for (int row = 1; row <= rowCount; ++row)
        {

            int station = (int) waitTimeTable.getValueAt(row, "poe");
            if (station != lastStation)
            {
                wait = new WaitTimeClass();
                wait.beginPeriod = new int[modelStructure.TIME_PERIODS];
                wait.endPeriod = new int[modelStructure.TIME_PERIODS];
                wait.StandardWait = new float[modelStructure.TIME_PERIODS];
                wait.SENTRIWait = new float[modelStructure.TIME_PERIODS];
                wait.PedestrianWait = new float[modelStructure.TIME_PERIODS];
                index = 0;
                lastStation = station;
            } else
            {
                ++index;
            }

            wait.beginPeriod[index] = (int) waitTimeTable.getValueAt(row, "StartPeriod");
            wait.endPeriod[index] = (int) waitTimeTable.getValueAt(row, "EndPeriod");
            wait.StandardWait[index] = waitTimeTable.getValueAt(row, "StandardWait");
            wait.SENTRIWait[index] = waitTimeTable.getValueAt(row, "SENTRIWait");
            wait.PedestrianWait[index] = waitTimeTable.getValueAt(row, "PedestrianWait");

            waitTimeMap.put(station, wait);

        }
        logger.info("End reading the data in file " + fileName);

    }

    public String[] getModeAltNames(int purposeIndex)
    {
        return modeAltNames;
    }

    /**
     * @return the tripModeChoiceModel
     */
    public CrossBorderTripModeChoiceModel getTripModeChoiceModel()
    {
        return tripModeChoiceModel;
    }

}
