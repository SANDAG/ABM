package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import org.sandag.abm.accessibilities.NonTransitUtilities;
import org.sandag.abm.application.SandagCtrampDmuFactory;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.ResourceUtil;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 11, 2008 Time: 9:25:30 AM To
 * change this template use File | Settings | File Templates.
 */
public class SubtourDepartureAndDurationTime
        implements Serializable
{

    private transient Logger                logger                       = Logger.getLogger(SubtourDepartureAndDurationTime.class);
    private transient Logger                todLogger                    = Logger.getLogger("todLogger");
    private transient Logger                tourMCNonManLogger           = Logger.getLogger("tourMcNonMan");

    private static final String             TOD_UEC_FILE_TARGET          = "departTime.uec.file";
    private static final String             TOD_UEC_DATA_TARGET          = "departTime.data.page";
    private static final String             TOD_UEC_AT_WORK_MODEL_TARGET = "departTime.atwork.page";

    private static String[]                 tourPurposeNames;

    private int[]                           todModelIndices;
    private HashMap<String, Integer>        purposeNameIndexMap;

    private static final String[]           DC_MODEL_SHEET_KEYS          = {
            TOD_UEC_AT_WORK_MODEL_TARGET, TOD_UEC_AT_WORK_MODEL_TARGET,
            TOD_UEC_AT_WORK_MODEL_TARGET                                 };

    private int[]                           tourDepartureTimeChoiceSample;

    // DMU for the UEC
    private TourDepartureTimeAndDurationDMU todDmuObject;
    private TourModeChoiceDMU               mcDmuObject;

    // model structure to compare the .properties time of day with the UECs
    private ModelStructure                  modelStructure;

    private TazDataManager                  tazs;
    private MgraDataManager                 mgraManager;

    // private double[][] dcSizeArray;

    private ChoiceModelApplication[]        todModels;
    private TourModeChoiceModel             mcModel;

    private int[]                           altStarts;
    private int[]                           altEnds;

    private boolean[]                       needToComputeLogsum;
    private double[]                        modeChoiceLogsums;

    private int[]                           tempWindow;

    // create an array to count the subtours propcessed within work tours
    // there are at most 2 work tours per person
    private int[]                           subtourNumForWorkTours       = new int[2];

    private int                             noAltChoice                  = 1;

    private long                            mcTime;

    public SubtourDepartureAndDurationTime(HashMap<String, String> propertyMap,
            ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory,
            TourModeChoiceModel mcModel)
    {

        // set the model structure
        this.modelStructure = modelStructure;
        this.mcModel = mcModel;

        logger.info(String.format("setting up %s time-of-day choice model.",
                ModelStructure.AT_WORK_CATEGORY));

        setupTodChoiceModels(propertyMap, dmuFactory);
    }

    private void setupTodChoiceModels(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

        tazs = TazDataManager.getInstance();
        mgraManager = MgraDataManager.getInstance();

        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);

        String todUecFileName = propertyMap.get(TOD_UEC_FILE_TARGET);
        todUecFileName = uecFileDirectory + todUecFileName;

        todDmuObject = dmuFactory.getTourDepartureTimeAndDurationDMU();

        mcDmuObject = dmuFactory.getModeChoiceDMU();

        int numLogsumIndices = modelStructure.getSkimPeriodCombinationIndices().length;
        needToComputeLogsum = new boolean[numLogsumIndices];
        modeChoiceLogsums = new double[numLogsumIndices];

        tourPurposeNames = new String[3];
        tourPurposeNames[0] = modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME;
        tourPurposeNames[1] = modelStructure.AT_WORK_EAT_PURPOSE_NAME;
        tourPurposeNames[2] = modelStructure.AT_WORK_MAINT_PURPOSE_NAME;

        // create the array of tod model indices
        int[] uecSheetIndices = new int[tourPurposeNames.length];

        purposeNameIndexMap = new HashMap<String, Integer>(tourPurposeNames.length);

        int i = 0;
        for (String purposeName : tourPurposeNames)
        {
            int uecIndex = Util.getIntegerValueFromPropertyMap(propertyMap, DC_MODEL_SHEET_KEYS[i]);
            purposeNameIndexMap.put(purposeName, i);
            uecSheetIndices[i] = uecIndex;
            i++;
        }

        // create a lookup array to map purpose index to model index
        todModelIndices = new int[uecSheetIndices.length];

        // get a set of unique model sheet numbers so that we can create
        // ChoiceModelApplication objects once for each model sheet used
        // also create a HashMap to relate size segment index to SOA Model
        // objects
        HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();
        int todModelIndex = 0;
        int todSegmentIndex = 0;
        for (int uecIndex : uecSheetIndices)
        {
            // if the uec sheet for the model segment is not in the map, add it,
            // otherwise, get it from the map
            if (!modelIndexMap.containsKey(uecIndex))
            {
                modelIndexMap.put(uecIndex, todModelIndex);
                todModelIndices[todSegmentIndex] = todModelIndex++;
            } else
            {
                todModelIndices[todSegmentIndex] = modelIndexMap.get(uecIndex);
            }

            todSegmentIndex++;
        }

        todModels = new ChoiceModelApplication[modelIndexMap.size()];
        int todModelDataSheet = Util.getIntegerValueFromPropertyMap(propertyMap,
                TOD_UEC_DATA_TARGET);

        for (int uecIndex : modelIndexMap.keySet())
        {
            int modelIndex = modelIndexMap.get(uecIndex);
            try
            {
                todModels[modelIndex] = new ChoiceModelApplication(todUecFileName, uecIndex,
                        todModelDataSheet, propertyMap, (VariableTable) todDmuObject);
            } catch (RuntimeException e)
            {
                logger.error(String
                        .format("exception caught setting up At-work Subtour TOD ChoiceModelApplication[%d] for model index=%d of %d models",
                                i, i, modelIndexMap.size()));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

        }

        // get the alternatives table from the work tod UEC.
        TableDataSet altsTable = todModels[0].getUEC().getAlternativeData();

        altStarts = altsTable.getColumnAsInt(CtrampApplication.START_FIELD_NAME);
        altEnds = altsTable.getColumnAsInt(CtrampApplication.END_FIELD_NAME);
        todDmuObject.setTodAlts(altStarts, altEnds);

        int numDepartureTimeChoiceAlternatives = todModels[0].getNumberOfAlternatives();
        tourDepartureTimeChoiceSample = new int[numDepartureTimeChoiceAlternatives + 1];
        Arrays.fill(tourDepartureTimeChoiceSample, 1);

        tempWindow = new int[modelStructure.getNumberOfTimePeriods() + 1];

    }

    public void applyModel(Household hh, boolean runModeChoice)
    {

        mcTime = 0;

        Logger modelLogger = todLogger;

        // get the peron objects for this household
        Person[] persons = hh.getPersons();
        for (int p = 1; p < persons.length; p++)
        {

            Person person = persons[p];

            // get the work tours for the person
            ArrayList<Tour> subtourList = person.getListOfAtWorkSubtours();

            // if no work subtours for person, nothing to do.
            if (subtourList.size() == 0) continue;

            ArrayList<Tour> workTourList = person.getListOfWorkTours();
            int numWorkTours = workTourList.size();

            // save a copy of this person's original time windows
            int[] personWindow = person.getTimeWindows();
            for (int w = 0; w < personWindow.length; w++)
                tempWindow[w] = personWindow[w];

            for (int i = 0; i < subtourNumForWorkTours.length; i++)
                subtourNumForWorkTours[i] = 0;

            int m = -1;
            int tourPurpNum = 1;
            int noWindowCountFirstTemp = 0;
            int noWindowCountLastTemp = 0;
            int noLaterAlternativeCountTemp = 0;
            for (Tour t : subtourList)
            {

                Tour workTour = null;
                int workTourIndex = 0;

                try
                {

                    workTourIndex = t.getWorkTourIndexFromSubtourId(t.getTourId());
                    subtourNumForWorkTours[workTourIndex]++;
                    workTour = workTourList.get(workTourIndex);

                    // if the first subtour for a work tour, make window of work
                    // tour available, and other windows not available
                    if (subtourNumForWorkTours[workTourIndex] == 1)
                    {
                        person.resetTimeWindow(workTour.getTourDepartPeriod(),
                                workTour.getTourArrivePeriod());
                        person.scheduleWindow(0, workTour.getTourDepartPeriod() - 1);
                        person.scheduleWindow(workTour.getTourArrivePeriod() + 1,
                                modelStructure.getNumberOfTimePeriods());
                    } else if (subtourNumForWorkTours[workTourIndex] > 2)
                    {
                        logger.error("too many subtours for a work tour.  workTourIndex="
                                + workTourIndex + ", subtourNumForWorkTours[workTourIndex]"
                                + subtourNumForWorkTours[workTourIndex]);
                        logger.error("hhid=" + hh.getHhId() + ", persNum=" + person.getPersonNum());
                        throw new RuntimeException();
                    }

                    // get the choice model for the tour purpose
                    String tourPurposeName = t.getSubTourPurpose();

                    int tourPurposeIndex = purposeNameIndexMap.get(tourPurposeName);
                    m = todModelIndices[tourPurposeIndex];

                    // write debug header
                    String separator = "";
                    String choiceModelDescription = "";
                    String decisionMakerLabel = "";
                    String loggingHeader = "";
                    if (hh.getDebugChoiceModels())
                    {

                        choiceModelDescription = String.format(
                                "AtWork Subtour Departure Time Choice Model for: Purpose=%s",
                                tourPurposeName);
                        decisionMakerLabel = String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, tourId=%d, num=%d of %d %s tours",
                                        hh.getHhId(), person.getPersonNum(),
                                        person.getPersonType(), t.getTourId(), tourPurpNum,
                                        subtourList.size(), tourPurposeName);
                        todModels[m].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                                decisionMakerLabel);

                        modelLogger.info(" ");
                        String loggerString = "AtWork Subtour Departure Time Choice Model: Debug Statement for Household ID: "
                                + hh.getHhId()
                                + ", Person Num: "
                                + person.getPersonNum()
                                + ", Person Type: "
                                + person.getPersonType()
                                + ", Tour Id: "
                                + t.getTourId()
                                + ", num "
                                + tourPurpNum
                                + " of "
                                + subtourList.size() + " " + tourPurposeName + " tours.";
                        for (int k = 0; k < loggerString.length(); k++)
                            separator += "+";
                        modelLogger.info(loggerString);
                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                        loggingHeader = String.format("%s for %s", choiceModelDescription,
                                decisionMakerLabel);

                    }

                    // set the dmu object
                    todDmuObject.setHousehold(hh);
                    todDmuObject.setPerson(person);
                    todDmuObject.setTour(t);

                    int otherTourEndHour = -1;

                    // check for multiple tours for this person, by purpose
                    // set the first or second switch if multiple tours for
                    // person, by purpose
                    if (subtourList.size() == 1)
                    {
                        // not a multiple tour pattern
                        todDmuObject.setFirstTour(0);
                        todDmuObject.setSubsequentTour(0);
                        todDmuObject.setTourNumber(1);
                        todDmuObject.setEndOfPreviousScheduledTour(0);
                    } else if (subtourList.size() > 1)
                    {
                        // Two-plus tour multiple tour pattern
                        if (tourPurpNum == 1)
                        {
                            // first of 2+ tours
                            todDmuObject.setFirstTour(1);
                            todDmuObject.setSubsequentTour(0);
                            todDmuObject.setTourNumber(tourPurpNum);
                            todDmuObject.setEndOfPreviousScheduledTour(0);
                        } else
                        {
                            // 2nd or greater tours
                            todDmuObject.setFirstTour(0);
                            todDmuObject.setSubsequentTour(1);
                            todDmuObject.setTourNumber(tourPurpNum);
                            // the ArrayList is 0-based, and we want the
                            // previous tour, so subtract 2 from tourPurpNum to
                            // get the right index
                            otherTourEndHour = subtourList.get(tourPurpNum - 2)
                                    .getTourArrivePeriod();
                            todDmuObject.setEndOfPreviousScheduledTour(otherTourEndHour);
                        }
                    }

                    // set the choice availability and sample
                    boolean[] departureTimeChoiceAvailability = person.getAvailableTimeWindows(
                            altStarts, altEnds);
                    Arrays.fill(tourDepartureTimeChoiceSample, 1);

                    if (departureTimeChoiceAvailability.length != tourDepartureTimeChoiceSample.length)
                    {
                        logger.error(String
                                .format("error in at-work subtour departure time choice model for hhId=%d, personNum=%d, tour purpose index=%d, tour ArrayList index=%d.",
                                        hh.getHhId(), person.getPersonNum(), tourPurposeIndex,
                                        tourPurpNum - 1));
                        logger.error(String
                                .format("length of the availability array determined by the number of alternatives set in the person scheduler=%d",
                                        departureTimeChoiceAvailability.length));
                        logger.error(String
                                .format("does not equal the length of the sample array determined by the number of alternatives in the at-work subtour UEC=%d.",
                                        tourDepartureTimeChoiceSample.length));
                        throw new RuntimeException();
                    }

                    // if no time window is available for the tour, make the
                    // first and last alternatives available
                    // for that alternative, and keep track of the number of
                    // times this condition occurs.
                    int alternativeAvailable = -1;
                    for (int a = 0; a < departureTimeChoiceAvailability.length; a++)
                    {
                        if (departureTimeChoiceAvailability[a])
                        {
                            alternativeAvailable = a;
                            break;
                        }
                    }

                    int chosen = -1;
                    int chosenStartPeriod = -1;
                    int chosenEndPeriod = -1;

                    // alternate making the first and last periods chosen if no
                    // alternatives are available
                    if (alternativeAvailable < 0)
                    {

                        if (noAltChoice == 1)
                        {
                            if (subtourList.size() > 1 && tourPurpNum > 1)
                            {
                                chosenStartPeriod = otherTourEndHour;
                                chosenEndPeriod = otherTourEndHour;
                                if (hh.getDebugChoiceModels())
                                    modelLogger
                                            .info("All alternatives already scheduled, depart AND arrive set to previous sub-tour arrive period="
                                                    + chosenStartPeriod + ".");
                            } else
                            {
                                chosenStartPeriod = workTour.getTourDepartPeriod();
                                chosenEndPeriod = workTour.getTourDepartPeriod();
                                if (hh.getDebugChoiceModels())
                                    modelLogger
                                            .info("All alternatives already scheduled and no previous sub-tour, depart AND arrive set to work tour depart period="
                                                    + chosenStartPeriod + ".");
                            }
                            noWindowCountFirstTemp++;
                            noAltChoice = departureTimeChoiceAvailability.length - 1;
                        } else
                        {
                            if (subtourList.size() > 1 && tourPurpNum > 1)
                            {
                                chosenStartPeriod = otherTourEndHour;
                                chosenEndPeriod = otherTourEndHour;
                                if (hh.getDebugChoiceModels())
                                    modelLogger
                                            .info("All alternatives already scheduled, depart AND arrive set to previous sub-tour arrive period="
                                                    + chosenStartPeriod + ".");
                            } else
                            {
                                chosenStartPeriod = workTour.getTourArrivePeriod();
                                chosenEndPeriod = workTour.getTourArrivePeriod();
                                if (hh.getDebugChoiceModels())
                                    modelLogger
                                            .info("All alternatives already scheduled and no previous sub-tour, depart AND arrive set to work tour arrive period="
                                                    + chosenStartPeriod + ".");
                            }
                            noWindowCountLastTemp++;
                            noAltChoice = 1;
                        }

                        // schedule the chosen alternative
                        person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
                        t.setTourDepartPeriod(chosenStartPeriod);
                        t.setTourArrivePeriod(chosenEndPeriod);

                        if (runModeChoice)
                        {

                            long check = System.nanoTime();

                            if (hh.getDebugChoiceModels())
                                hh.logHouseholdObject(
                                        "Pre At-work Subtour Tour Mode Choice Household "
                                                + hh.getHhId() + ", Tour " + tourPurpNum + " of "
                                                + subtourList.size(), tourMCNonManLogger);

                            // set the mode choice attributes needed by
                            // @variables in the UEC spreadsheets
                            setModeChoiceDmuAttributes(hh, person, t, chosenStartPeriod,
                                    chosenEndPeriod);

                            // use the mcModel object already setup for
                            // computing logsums and get
                            // the mode choice, where the selected
                            // worklocation and subzone an departure time and
                            // duration are set
                            // for this work tour.
                            int chosenMode = mcModel.getModeChoice(mcDmuObject,
                                    t.getTourPrimaryPurpose());
                            t.setTourModeChoice(chosenMode);

                            mcTime += (System.nanoTime() - check);

                        }

                    } else
                    {

                        // calculate and store the mode choice logsum for the
                        // usual work location for this worker at the various
                        // departure time and duration alternativees
                        setTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(t,
                                departureTimeChoiceAvailability);

                        if (hh.getDebugChoiceModels())
                        {
                            hh.logTourObject(loggingHeader, modelLogger, person, t);
                        }

                        todDmuObject.setOriginZone(mgraManager.getTaz(t.getTourOrigMgra()));
                        todDmuObject.setDestinationZone(mgraManager.getTaz(t.getTourDestMgra()));

                        todModels[m].computeUtilities(todDmuObject, todDmuObject.getIndexValues(),
                                departureTimeChoiceAvailability, tourDepartureTimeChoiceSample);

                        Random hhRandom = hh.getHhRandom();
                        int randomCount = hh.getHhRandomCount();
                        double rn = hhRandom.nextDouble();

                        // if the choice model has at least one available
                        // alternative, make choice.
                        if (todModels[m].getAvailabilityCount() > 0)
                        {
                            chosen = todModels[m].getChoiceResult(rn);

                            // debug output
                            if (hh.getDebugChoiceModels())
                            {

                                double[] utilities = todModels[m].getUtilities();
                                double[] probabilities = todModels[m].getProbabilities();
                                boolean[] availabilities = todModels[m].getAvailabilities();

                                String personTypeString = person.getPersonType();
                                int personNum = person.getPersonNum();
                                modelLogger.info("Person num: " + personNum + ", Person type: "
                                        + personTypeString + ", Tour Id: " + t.getTourId()
                                        + ", Tour num: " + tourPurpNum + " of "
                                        + subtourList.size() + " " + tourPurposeName + " tours.");
                                modelLogger
                                        .info("Alternative            Availability           Utility       Probability           CumProb");
                                modelLogger
                                        .info("--------------------   ------------    --------------    --------------    --------------");

                                double cumProb = 0.0;
                                for (int k = 0; k < todModels[m].getNumberOfAlternatives(); k++)
                                {
                                    cumProb += probabilities[k];
                                    String altString = String.format("%-3d out=%-3d, in=%-3d",
                                            k + 1, altStarts[k], altEnds[k]);
                                    modelLogger.info(String.format("%-20s%15s%18.6e%18.6e%18.6e",
                                            altString, availabilities[k + 1], utilities[k],
                                            probabilities[k], cumProb));
                                }

                                modelLogger.info(" ");
                                String altString = String.format("%-3d out=%-3d, in=%-3d", chosen,
                                        altStarts[chosen - 1], altEnds[chosen - 1]);
                                modelLogger.info(String.format(
                                        "Choice: %s, with rn=%.8f, randomCount=%d", altString, rn,
                                        randomCount));

                                modelLogger.info(separator);
                                modelLogger.info("");
                                modelLogger.info("");

                                // write choice model alternative info to debug
                                // log file
                                todModels[m].logAlternativesInfo(choiceModelDescription,
                                        decisionMakerLabel);
                                todModels[m].logSelectionInfo(choiceModelDescription,
                                        decisionMakerLabel, rn, chosen);

                                // write UEC calculation results to separate
                                // model specific log file
                                loggingHeader = String.format("%s for %s", choiceModelDescription,
                                        decisionMakerLabel);
                                todModels[m].logUECResults(modelLogger, loggingHeader);

                            }

                        } else
                        {

                            // since there were no alternatives with valid
                            // utility, assuming previous
                            // tour of this type scheduled up to the last
                            // period, so no periods left
                            // for this tour.

                            // TODO: do a formal check for this so we can still
                            // flag other reasons why there's
                            // no valid utility for any alternative
                            chosen = departureTimeChoiceAvailability.length - 1;
                            noLaterAlternativeCountTemp++;

                        }

                        // schedule the chosen alternative
                        chosenStartPeriod = altStarts[chosen - 1];
                        chosenEndPeriod = altEnds[chosen - 1];
                        person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
                        t.setTourDepartPeriod(chosenStartPeriod);
                        t.setTourArrivePeriod(chosenEndPeriod);

                        if (runModeChoice)
                        {

                            if (hh.getDebugChoiceModels())
                                hh.logHouseholdObject(
                                        "Pre At-work Subtour Tour Mode Choice Household "
                                                + hh.getHhId() + ", Tour " + tourPurpNum + " of "
                                                + subtourList.size(), tourMCNonManLogger);

                            // set the mode choice attributes needed by
                            // @variables in the UEC spreadsheets
                            setModeChoiceDmuAttributes(hh, person, t, chosenStartPeriod,
                                    chosenEndPeriod);

                            // use the mcModel object already setup for
                            // computing logsums and get
                            // the mode choice, where the selected
                            // worklocation and subzone an departure time and
                            // duration are set
                            // for this work tour.
                            int chosenMode = mcModel.getModeChoice(mcDmuObject,
                                    t.getTourPrimaryPurpose());
                            t.setTourModeChoice(chosenMode);

                        }

                    }

                } catch (Exception e)
                {
                    String errorMessage = String
                            .format("Exception caught for HHID=%d, personNum=%d, At-work Subtour Departure time choice, tour ArrayList index=%d.",
                                    hh.getHhId(), person.getPersonNum(), tourPurpNum - 1);
                    String decisionMakerLabel = String
                            .format("Final At-work Subtour Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
                                    hh.getHhId(), person.getPersonNum(), person.getPersonType());
                    hh.logPersonObject(decisionMakerLabel, modelLogger, person);
                    todModels[m].logUECResults(modelLogger, errorMessage);

                    logger.error(errorMessage, e);
                    throw new RuntimeException();
                }

                tourPurpNum++;

            }

            for (int w = 0; w < person.getTimeWindows().length; w++)
                person.getTimeWindows()[w] = tempWindow[w];

            if (hh.getDebugChoiceModels())
            {
                String decisionMakerLabel = String
                        .format("Final At-work Subtour Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
                                hh.getHhId(), person.getPersonNum(), person.getPersonType());
                hh.logPersonObject(decisionMakerLabel, modelLogger, person);
            }

        }

        hh.setAwtodRandomCount(hh.getHhRandomCount());

    }

    private void setModeChoiceDmuAttributes(Household household, Person person, Tour t,
            int startPeriod, int endPeriod)
    {

        t.setTourDepartPeriod(startPeriod);
        t.setTourArrivePeriod(endPeriod);

        int workTourIndex = t.getWorkTourIndexFromSubtourId(t.getTourId());
        Tour workTour = person.getListOfWorkTours().get(workTourIndex);

        // update the MC dmuObjects for this person
        mcDmuObject.setHouseholdObject(household);
        mcDmuObject.setPersonObject(person);
        mcDmuObject.setTourObject(t);
        mcDmuObject.setWorkTourObject(workTour);
        mcDmuObject.setDmuIndexValues(household.getHhId(), household.getHhMgra(),
                t.getTourOrigMgra(), t.getTourDestMgra(), household.getDebugChoiceModels());

        mcDmuObject.setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(t
                .getTourOrigMgra())));
        mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager.getTaz(t
                .getTourDestMgra())));

    }

    private void setTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(Tour tour,
            boolean[] altAvailable)
    {

        Person person = tour.getPersonObject();
        Household household = person.getHouseholdObject();

        Arrays.fill(needToComputeLogsum, true);
        Arrays.fill(modeChoiceLogsums, -999);

        Logger modelLogger = todLogger;
        String choiceModelDescription = String.format(
                "At-work Subtour Mode Choice Logsum calculation for %s Departure Time Choice",
                tour.getTourPurpose());
        String decisionMakerLabel = String.format(
                "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person
                        .getPersonNum(), person.getPersonType(), tour.getTourId(), person
                        .getListOfWorkTours().size());
        String loggingHeader = String
                .format("%s    %s", choiceModelDescription, decisionMakerLabel);

        for (int a = 1; a <= altStarts.length; a++)
        {

            // if the depart/arrive alternative is unavailable, no need to check
            // to see if a logsum has been calculated
            if (!altAvailable[a]) continue;

            int startPeriod = altStarts[a - 1];
            int endPeriod = altEnds[a - 1];

            int index = modelStructure.getSkimPeriodCombinationIndex(startPeriod, endPeriod);
            if (needToComputeLogsum[index])
            {

                String periodString = modelStructure.getModelPeriodLabel(modelStructure
                        .getModelPeriodIndex(startPeriod))
                        + " to "
                        + modelStructure.getModelPeriodLabel(modelStructure
                                .getModelPeriodIndex(endPeriod));

                // set the mode choice attributes needed by @variables in the
                // UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, tour, startPeriod, endPeriod);

                if (household.getDebugChoiceModels())
                    household.logTourObject(loggingHeader + ", " + periodString, modelLogger,
                            person, mcDmuObject.getTourObject());

                try
                {
                    modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(mcDmuObject, tour,
                            modelLogger, choiceModelDescription, decisionMakerLabel + ", "
                                    + periodString);
                } catch (Exception e)
                {
                    logger.fatal("exception caught applying mcModel.getModeChoiceLogsum() for "
                            + periodString + " work tour.");
                    logger.fatal("choiceModelDescription = " + choiceModelDescription);
                    logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
                    throw new RuntimeException(e);
                }
                needToComputeLogsum[index] = false;
            }

        }

        todDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

        mcDmuObject.getTourObject().setTourDepartPeriod(0);
        mcDmuObject.getTourObject().setTourArrivePeriod(0);
    }

    public long getModeChoiceTime()
    {
        return mcTime;
    }

    public static void main(String[] args)
    {

        // set values for these arguments so an object instance can be created
        // and setup run to test integrity of UEC files before running full
        // model.
        HashMap<String, String> propertyMap;

        if (args.length == 0)
        {
            System.out
                    .println("no properties file base name (without .properties extension) was specified as an argument.");
            return;
        } else
        {
            ResourceBundle rb = ResourceBundle.getBundle(args[0]);
            propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        }

        String matrixServerAddress = (String) propertyMap.get("RunModel.MatrixServerAddress");
        String matrixServerPort = (String) propertyMap.get("RunModel.MatrixServerPort");

        MatrixDataServerIf ms = new MatrixDataServerRmi(matrixServerAddress,
                Integer.parseInt(matrixServerPort), MatrixDataServer.MATRIX_DATA_SERVER_NAME);
        ms.testRemote(Thread.currentThread().getName());
        ms.start32BitMatrixIoServer(MatrixType.TRANSCAD);

        MatrixDataManager mdm = MatrixDataManager.getInstance();
        mdm.setMatrixDataServerObject(ms);

        /*        
         */
        ModelStructure modelStructure = new SandagModelStructure();
        SandagCtrampDmuFactory dmuFactory = new SandagCtrampDmuFactory(modelStructure);

        MgraDataManager mgraManager = MgraDataManager.getInstance(propertyMap);
        TazDataManager tazManager = TazDataManager.getInstance(propertyMap);

        BuildAccessibilities aggAcc = BuildAccessibilities.getInstance();
        if (!aggAcc.getAccessibilitiesAreBuilt())
        {
            aggAcc.setupBuildAccessibilities(propertyMap, false);

            aggAcc.calculateSizeTerms();
            aggAcc.calculateConstants();
            // aggAcc.buildAccessibilityComponents(propertyMap);

            boolean readAccessibilities = Util.getBooleanValueFromPropertyMap(propertyMap,
                    CtrampApplication.READ_ACCESSIBILITIES);
            if (readAccessibilities)
            {

                // output data
                String projectDirectory = propertyMap
                        .get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
                String accFileName = projectDirectory
                        + Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file");

                aggAcc.readAccessibilityTableFromFile(accFileName);

            } else
            {

                aggAcc.calculateDCUtilitiesDistributed(propertyMap);

            }

        }

        double[][] expConstants = aggAcc.getExpConstants();

        McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(propertyMap);

        double[][][] sovExpUtilities = null;
        double[][][] hovExpUtilities = null;
        double[][][] nMotorExpUtilities = null;
        NonTransitUtilities ntUtilities = new NonTransitUtilities(propertyMap, sovExpUtilities,
                hovExpUtilities, nMotorExpUtilities);

        MandatoryAccessibilitiesCalculator mandAcc = new MandatoryAccessibilitiesCalculator(
                propertyMap, ntUtilities, expConstants, logsumHelper.getBestTransitPathCalculator());

        TourModeChoiceModel awmcModel = new TourModeChoiceModel(propertyMap, modelStructure,
                ModelStructure.AT_WORK_CATEGORY, dmuFactory, logsumHelper);

        SubtourDestChoiceModel testObject = new SubtourDestChoiceModel(propertyMap, modelStructure,
                aggAcc, dmuFactory, awmcModel);
        System.out.println("SubtourDestChoiceModel object creation passed.");

        SubtourDepartureAndDurationTime testObject2 = new SubtourDepartureAndDurationTime(
                propertyMap, modelStructure, dmuFactory, awmcModel);
        System.out.println("SubtourDepartureAndDurationTime object creation passed.");

        // String hhHandlerAddress = (String)
        // propertyMap.get("RunModel.HouseholdServerAddress");
        // int hhServerPort = Integer.parseInt((String)
        // propertyMap.get("RunModel.HouseholdServerPort"));
        //
        // HouseholdDataManagerIf householdDataManager = new
        // HouseholdDataManagerRmi(hhHandlerAddress, hhServerPort,
        // SandagHouseholdDataManager.HH_DATA_SERVER_NAME);
        //
        //
        // householdDataManager.setPropertyFileValues(propertyMap);
        //
        // // have the household data manager read the synthetic population
        // // files and apply its tables to objects mapping method.
        // boolean restartHhServer = false;
        // try
        // {
        // // possible values for the following can be none, ao, cdap, imtf,
        // // imtod, awf, awl, awtod, jtf, jtl, jtod, inmtf, inmtl, inmtod,
        // // stf, stl
        // String restartModel = (String)
        // propertyMap.get("RunModel.RestartWithHhServer");
        // if (restartModel.equalsIgnoreCase("none")) restartHhServer = true;
        // else if (restartModel.equalsIgnoreCase("uwsl")
        // || restartModel.equalsIgnoreCase("ao")
        // || restartModel.equalsIgnoreCase("fp")
        // || restartModel.equalsIgnoreCase("cdap")
        // || restartModel.equalsIgnoreCase("imtf")
        // || restartModel.equalsIgnoreCase("imtod")
        // || restartModel.equalsIgnoreCase("awf")
        // || restartModel.equalsIgnoreCase("awl")
        // || restartModel.equalsIgnoreCase("awtod")
        // || restartModel.equalsIgnoreCase("jtf")
        // || restartModel.equalsIgnoreCase("jtl")
        // || restartModel.equalsIgnoreCase("jtod")
        // || restartModel.equalsIgnoreCase("inmtf")
        // || restartModel.equalsIgnoreCase("inmtl")
        // || restartModel.equalsIgnoreCase("inmtod")
        // || restartModel.equalsIgnoreCase("stf")
        // || restartModel.equalsIgnoreCase("stl")) restartHhServer = false;
        // } catch (MissingResourceException e)
        // {
        // restartHhServer = true;
        // }
        //
        // if (restartHhServer)
        // {
        //
        // householdDataManager.setDebugHhIdsFromHashmap();
        //
        // String inputHouseholdFileName = (String)
        // propertyMap.get(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_HH);
        // String inputPersonFileName = (String)
        // propertyMap.get(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_PERS);
        // householdDataManager.setHouseholdSampleRate( 1.0f, 0 );
        // householdDataManager.setupHouseholdDataManager(modelStructure, null,
        // inputHouseholdFileName, inputPersonFileName);
        //
        // } else
        // {
        //
        // householdDataManager.setHouseholdSampleRate( 1.0f, 0 );
        // householdDataManager.setDebugHhIdsFromHashmap();
        // householdDataManager.setTraceHouseholdSet();
        //
        // }

        // int id = householdDataManager.getArrayIndex( 1033380 );
        // int id = householdDataManager.getArrayIndex( 1033331 );
        // int id = householdDataManager.getArrayIndex( 423804 );
        // Household[] hh = householdDataManager.getHhArray( id, id );
        // testObject.applyModel( hh[0] );
        // testObject2.applyModel( hh[0], true );

        /**
         * used this block of code to test for typos and implemented dmu
         * methiods in the TOD choice UECs
         * 
         * String uecFileDirectory = propertyMap.get(
         * CtrampApplication.PROPERTIES_UEC_PATH ); String todUecFileName =
         * propertyMap.get( TOD_UEC_FILE_TARGET ); todUecFileName =
         * uecFileDirectory + todUecFileName;
         * 
         * ModelStructure modelStructure = new SandagModelStructure();
         * SandagCtrampDmuFactory dmuFactory = new
         * SandagCtrampDmuFactory(modelStructure);
         * TourDepartureTimeAndDurationDMU todDmuObject =
         * dmuFactory.getTourDepartureTimeAndDurationDMU();
         * 
         * File uecFile = new File(todUecFileName); UtilityExpressionCalculator
         * uec = new UtilityExpressionCalculator(uecFile, 10, 0, propertyMap,
         * (VariableTable) todDmuObject);
         * System.out.println("Subtour departure and duration UEC passed");
         */

        ms.stop32BitMatrixIoServer();

    }

}