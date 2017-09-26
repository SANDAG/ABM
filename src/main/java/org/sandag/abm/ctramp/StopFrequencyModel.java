package org.sandag.abm.ctramp;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;

/**
 * This class will be used for determining the number of stops on individual
 * mandatory, individual non-mandatory and joint tours.
 * 
 * @author Christi Willison
 * @version Nov 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class StopFrequencyModel
        implements Serializable
{

    private transient Logger            logger                                    = Logger.getLogger(StopFrequencyModel.class);
    private transient Logger            stopFreqLogger                            = Logger.getLogger("stopFreqLog");

    private static final String         PROPERTIES_UEC_STOP_FREQ                  = "stf.uec.file";
    private static final String         PROPERTIES_STOP_PURPOSE_LOOKUP_FILE       = "stf.purposeLookup.proportions";

    private static String[]             shopTypes                                 = {"",
            "shopSov0", "shopSov1", "shopSov2"                                    };
    private static String[]             maintTypes                                = {"",
            "maintSov0", "maintSov1", "maintSov2"                                 };
    private static String[]             discrTypes                                = {"",
            "discrSov0", "discrSov1", "discrSov2"                                 };

    private static final int            UEC_DATA_PAGE                             = 0;

    // define names used in lookup file
    private static final String         TOUR_PRIMARY_PURPOSE_COLUMN_HEADING       = "PrimPurp";
    private static final String         HALF_TOUR_DIRECTION_COLUMN_HEADING        = "Direction";
    private static final String         TOUR_DEPARTURE_START_RANGE_COLUMN_HEADING = "DepartRangeStart";
    private static final String         TOUR_DEPARTURE_END_RANGE_COLUMN_HEADING   = "DepartRangeEnd";
    private static final String         PERSON_TYPE_COLUMN_HEADING                = "Ptype";

    private static final String         OUTBOUND_DIRECTION_NAME                   = "Outbound";
    private static final String         INBOUND_DIRECTION_NAME                    = "Inbound";

    private static final String         FT_WORKER_PERSON_TYPE_NAME                = "FT Worker";
    private static final String         PT_WORKER_PERSON_TYPE_NAME                = "PT Worker";
    private static final String         UNIVERSITY_PERSON_TYPE_NAME               = "University Student";
    private static final String         NONWORKER_PERSON_TYPE_NAME                = "Homemaker";
    private static final String         RETIRED_PERSON_TYPE_NAME                  = "Retired";
    private static final String         DRIVING_STUDENT_PERSON_TYPE_NAME          = "Driving-age Child";
    private static final String         NONDRIVING_STUDENT_PERSON_TYPE_NAME       = "Pre-Driving Child";
    private static final String         PRESCHOOL_PERSON_TYPE_NAME                = "Preschool";
    private static final String         ALL_PERSON_TYPE_NAME                      = "All";

    private StopFrequencyDMU            dmuObject;
    private ChoiceModelApplication[]    choiceModelApplication;

    HashMap<Integer, Integer>           tourPurposeModelIndexMap;
    HashMap<Integer, String>            tourPrimaryPurposeIndexNameMap;

    private HashMap<Integer, String>    indexPurposeMap;
    private HashMap<String, double[]>[] outProportionsMaps;
    private HashMap<String, double[]>[] inProportionsMaps;

    private AccessibilitiesTable        accTable;
    private ModelStructure              modelStructure;
    private MgraDataManager             mgraManager;

    /**
     * Constructor that will be used to set up the ChoiceModelApplications for
     * each type of tour
     * 
     * @param projectDirectory
     *            - name of root level project directory
     * @param resourceBundle
     *            - properties file with paths identified
     * @param dmuObject
     *            - decision making unit for stop frequency
     * @param tazDataManager
     *            - holds information about TAZs in the model.
     */
    public StopFrequencyModel(HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory,
            ModelStructure myModelStructure, AccessibilitiesTable myAccTable)
    {
        accTable = myAccTable;
        modelStructure = myModelStructure;
        setupModels(propertyMap, dmuFactory);
    }

    private void setupModels(HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory)
    {

        mgraManager = MgraDataManager.getInstance(propertyMap);

        logger.info(String.format("setting up stop frequency choice models."));

        // String projectDirectory = propertyMap.get(
        // CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String stfUecFile = propertyMap.get(PROPERTIES_UEC_STOP_FREQ);
        String uecFileName = uecPath + stfUecFile;

        dmuObject = dmuFactory.getStopFrequencyDMU();

        tourPrimaryPurposeIndexNameMap = modelStructure.getIndexPrimaryPurposeNameMap();

        tourPurposeModelIndexMap = dmuObject.getTourPurposeChoiceModelIndexMap();
        int[] modelSheetsArray = dmuObject.getModelSheetValuesArray();

        // one choice model for each model sheet specified
        choiceModelApplication = new ChoiceModelApplication[modelSheetsArray.length];
        for (int i = 0; i < modelSheetsArray.length; i++)
            choiceModelApplication[i] = new ChoiceModelApplication(uecFileName,
                    modelSheetsArray[i], UEC_DATA_PAGE, propertyMap, (VariableTable) dmuObject);

        String purposeLookupFileName = uecPath
                + propertyMap.get(PROPERTIES_STOP_PURPOSE_LOOKUP_FILE);

        // read the stop purpose lookup table data and populate the maps used to
        // assign stop purposes
        readPurposeLookupProportionsTable(purposeLookupFileName);

    }

    public void applyModel(Household household)
    {

        int totalStops = 0;
        int totalTours = 0;

        Logger modelLogger = stopFreqLogger;
        if (household.getDebugChoiceModels())
            household.logHouseholdObject("Pre Stop Frequency Choice: HH=" + household.getHhId(),
                    stopFreqLogger);

        // get this household's person array
        Person[] personArray = household.getPersons();

        // set the household id, origin taz, hh taz, and debugFlag=false in the
        // dmu
        dmuObject.setHouseholdObject(household);

        // set the auto sufficiency dependent non-mandatory accessibility values
        // for
        // the household
        int autoSufficiency = household.getAutoSufficiency();
        dmuObject.setShoppingAccessibility(accTable.getAggregateAccessibility(
                shopTypes[autoSufficiency], household.getHhMgra()));
        dmuObject.setMaintenanceAccessibility(accTable.getAggregateAccessibility(
                maintTypes[autoSufficiency], household.getHhMgra()));
        dmuObject.setDiscretionaryAccessibility(accTable.getAggregateAccessibility(
                discrTypes[autoSufficiency], household.getHhMgra()));

        // process the joint tours for the household first
        Tour[] jt = household.getJointTourArray();
        if (jt != null)
        {

            List<Tour> tourList = new ArrayList<Tour>();
            for (Tour t : jt)
                tourList.add(t);

            int tourCount = 0;
            for (Tour tour : tourList)
            {

                try
                {

                    tour.clearStopModelResults();

                    int modelIndex = tourPurposeModelIndexMap
                            .get(tour.getTourPrimaryPurposeIndex());

                    // write debug header
                    String separator = "";
                    String choiceModelDescription = "";
                    String decisionMakerLabel = "";
                    String loggingHeader = "";

                    if (household.getDebugChoiceModels())
                    {
                        choiceModelDescription = String
                                .format("Joint Tour Stop Frequency Choice Model:");
                        decisionMakerLabel = String.format(
                                "HH=%d, TourType=%s, TourId=%d, TourPurpose=%s.",
                                household.getHhId(), tour.getTourCategory(), tour.getTourId(),
                                tour.getTourPurpose());
                        choiceModelApplication[modelIndex].choiceModelUtilityTraceLoggerHeading(
                                choiceModelDescription, decisionMakerLabel);
                        modelLogger.info(" ");
                        loggingHeader = choiceModelDescription + " for " + decisionMakerLabel;

                        for (int k = 0; k < loggingHeader.length(); k++)
                            separator += "+";

                        modelLogger.info(loggingHeader);
                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");
                    }

                    // set the tour object
                    dmuObject.setTourObject(tour);

                    // set the tour orig/dest TAZs associated with the tour
                    // orig/dest MGRAs in the IndexValues object.
                    dmuObject.setDmuIndexValues(household.getHhId(), household.getHhTaz(),
                            mgraManager.getTaz(tour.getTourOrigMgra()),
                            mgraManager.getTaz(tour.getTourDestMgra()));

                    // compute the utilities
                    choiceModelApplication[modelIndex].computeUtilities(dmuObject,
                            dmuObject.getDmuIndexValues());

                    // get the random number from the household
                    Random random = household.getHhRandom();
                    int randomCount = household.getHhRandomCount();
                    double rn = random.nextDouble();

                    // if the choice model has at least one available
                    // alternative, make choice.
                    int choice = -1;
                    if (choiceModelApplication[modelIndex].getAvailabilityCount() > 0) choice = choiceModelApplication[modelIndex]
                            .getChoiceResult(rn);
                    else
                    {
                        logger.error(String
                                .format("Exception caught applying joint tour stop frequency choice model for %s type tour: HHID=%d, tourCount=%d, randomCount=%f -- no avaialable stop frequency alternative to choose.",
                                        tour.getTourCategory(), household.getHhId(), tourCount,
                                        randomCount));
                        throw new RuntimeException();
                    }

                    // debug output
                    if (household.getDebugChoiceModels())
                    {

                        double[] utilities = choiceModelApplication[modelIndex].getUtilities();
                        double[] probabilities = choiceModelApplication[modelIndex]
                                .getProbabilities();
                        String[] altNames = choiceModelApplication[modelIndex]
                                .getAlternativeNames();

                        // 0s-indexing
                        modelLogger.info(decisionMakerLabel);
                        modelLogger
                                .info("Alternative                 Utility       Probability           CumProb");
                        modelLogger
                                .info("------------------   --------------    --------------    --------------");

                        double cumProb = 0.0;
                        for (int k = 0; k < altNames.length; k++)
                        {
                            cumProb += probabilities[k];
                            String altString = String.format("%-3d %15s", k + 1, altNames[k]);
                            modelLogger.info(String.format("%-20s%18.6e%18.6e%18.6e", altString,
                                    utilities[k], probabilities[k], cumProb));
                        }

                        modelLogger.info(" ");
                        String altString = String.format("%-3d  %s", choice, altNames[choice - 1]);
                        modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                                altString, rn, randomCount));
                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                        // write choice model alternative info to debug log file
                        choiceModelApplication[modelIndex].logAlternativesInfo(
                                choiceModelDescription, decisionMakerLabel);
                        choiceModelApplication[modelIndex].logSelectionInfo(choiceModelDescription,
                                decisionMakerLabel, rn, choice);

                        // write UEC calculation results to separate model
                        // specific log file
                        choiceModelApplication[modelIndex]
                                .logUECResults(modelLogger, loggingHeader);
                    }

                    // save the chosen alternative and create and populate the
                    // arrays of inbound/outbound
                    // stops in the tour object
                    totalStops += setStopFreqChoice(tour, choice);

                    totalTours++;
                    tourCount++;

                } catch (Exception e)
                {
                    logger.error(String
                            .format("Exception caught processing joint tour stop frequency choice model for %s type tour:  HHID=%d, tourCount=%d.",
                                    tour.getTourCategory(), household.getHhId(), tourCount));
                    throw new RuntimeException(e);
                }

            }

        }

        // now loop through the person array (1-based), and process all tours
        // for
        // each person
        for (int j = 1; j < personArray.length; ++j)
        {

            Person person = personArray[j];

            if (household.getDebugChoiceModels())
            {
                String decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s",
                        household.getHhId(), person.getPersonNum(), person.getPersonType());
                household.logPersonObject(decisionMakerLabel, modelLogger, person);
            }

            // set the person
            dmuObject.setPersonObject(person);

            List<Tour> tourList = new ArrayList<Tour>();

            // apply stop frequency for all person tours
            tourList.addAll(person.getListOfWorkTours());
            tourList.addAll(person.getListOfSchoolTours());
            tourList.addAll(person.getListOfIndividualNonMandatoryTours());
            tourList.addAll(person.getListOfAtWorkSubtours());

            int tourCount = 0;
            for (Tour tour : tourList)
            {

                try
                {

                    tour.clearStopModelResults();

                    int modelIndex = tourPurposeModelIndexMap
                            .get(tour.getTourPrimaryPurposeIndex());

                    // write debug header
                    String separator = "";
                    String choiceModelDescription = "";
                    String decisionMakerLabel = "";
                    String loggingHeader = "";
                    if (household.getDebugChoiceModels())
                    {

                        choiceModelDescription = String
                                .format("Individual Tour Stop Frequency Choice Model:");
                        decisionMakerLabel = String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourType=%s, TourId=%d, TourPurpose=%s, modelIndex=%d.",
                                        household.getHhId(), person.getPersonNum(),
                                        person.getPersonType(), tour.getTourCategory(),
                                        tour.getTourId(), tour.getTourPurpose(), modelIndex);

                        choiceModelApplication[modelIndex].choiceModelUtilityTraceLoggerHeading(
                                choiceModelDescription, decisionMakerLabel);

                        modelLogger.info(" ");
                        loggingHeader = choiceModelDescription + " for " + decisionMakerLabel;
                        for (int k = 0; k < loggingHeader.length(); k++)
                            separator += "+";
                        modelLogger.info(loggingHeader);
                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                    }

                    // set the tour object
                    dmuObject.setTourObject(tour);

                    // compute the utilities
                    dmuObject.setDmuIndexValues(household.getHhId(), household.getHhTaz(),
                            mgraManager.getTaz(tour.getTourOrigMgra()),
                            mgraManager.getTaz(tour.getTourDestMgra()));

                    choiceModelApplication[modelIndex].computeUtilities(dmuObject,
                            dmuObject.getDmuIndexValues());

                    // get the random number from the household
                    Random random = household.getHhRandom();
                    int randomCount = household.getHhRandomCount();
                    double rn = random.nextDouble();

                    // if the choice model has at least one available
                    // alternative,
                    // make choice.
                    int choice = -1;
                    if (choiceModelApplication[modelIndex].getAvailabilityCount() > 0) choice = choiceModelApplication[modelIndex]
                            .getChoiceResult(rn);
                    else
                    {
                        logger.error(String
                                .format("Exception caught applying Individual Tour stop frequency choice model for %s type tour: j=%d, HHID=%d, personNum=%d, tourCount=%d, randomCount=%f -- no avaialable stop frequency alternative to choose.",
                                        tour.getTourCategory(), j, household.getHhId(),
                                        person.getPersonNum(), tourCount, randomCount));
                        throw new RuntimeException();
                    }

                    // debug output
                    if (household.getDebugChoiceModels())
                    {

                        double[] utilities = choiceModelApplication[modelIndex].getUtilities();
                        double[] probabilities = choiceModelApplication[modelIndex]
                                .getProbabilities();
                        String[] altNames = choiceModelApplication[modelIndex]
                                .getAlternativeNames(); // 0s-indexing

                        modelLogger.info(decisionMakerLabel);
                        modelLogger
                                .info("Alternative                 Utility       Probability           CumProb");
                        modelLogger
                                .info("------------------   --------------    --------------    --------------");

                        double cumProb = 0.0;
                        for (int k = 0; k < altNames.length; ++k)
                        {
                            cumProb += probabilities[k];
                            String altString = String.format("%-3d %15s", k + 1, altNames[k]);
                            modelLogger.info(String.format("%-20s%18.6e%18.6e%18.6e", altString,
                                    utilities[k], probabilities[k], cumProb));
                        }

                        modelLogger.info(" ");
                        String altString = String.format("%-3d  %s", choice, altNames[choice - 1]);
                        modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                                altString, rn, randomCount));

                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                        // write choice model alternative info to debug log file
                        choiceModelApplication[modelIndex].logAlternativesInfo(
                                choiceModelDescription, decisionMakerLabel);
                        choiceModelApplication[modelIndex].logSelectionInfo(choiceModelDescription,
                                decisionMakerLabel, rn, choice);

                        // write UEC calculation results to separate model
                        // specific
                        // log file
                        choiceModelApplication[modelIndex]
                                .logUECResults(modelLogger, loggingHeader);

                    }

                    // choiceResultsFreq[choice][modelIndex]++;

                    // save the chosen alternative and create and populate the
                    // arrays
                    // of inbound/outbound stops in the tour object
                    totalStops += setStopFreqChoice(tour, choice);
                    totalTours++;

                    tourCount++;

                } catch (Exception e)
                {
                    logger.error(String
                            .format("Exception caught processing Individual Tour stop frequency choice model for %s type tour:  j=%d, HHID=%d, personNum=%d, tourCount=%d.",
                                    tour.getTourCategory(), j, household.getHhId(),
                                    person.getPersonNum(), tourCount));
                    throw new RuntimeException(e);
                }

            }

        } // j (person loop)

        household.setStfRandomCount(household.getHhRandomCount());

    }

    private int setStopFreqChoice(Tour tour, int stopFreqChoice)
    {

        tour.setStopFreqChoice(stopFreqChoice);

        // set argument values for method call to get stop purpose
        Household hh = tour.getPersonObject().getHouseholdObject();
        int tourDepartPeriod = tour.getTourDepartPeriod();
        int tourArrivePeriod = tour.getTourArrivePeriod();
        
        //log out tour details if invalid tour departure and arrival time periods are found
        if(tourDepartPeriod==-1||tourArrivePeriod==-1) tour.logTourObject(logger, 100);
        
        int tourPrimaryPurposeIndex = tour.getTourPrimaryPurposeIndex();
        String tourPrimaryPurpose = tourPrimaryPurposeIndexNameMap.get(tourPrimaryPurposeIndex);
        String personType = tour.getPersonObject().getPersonType();

        int numObStops = dmuObject.getNumObStopsAlt(stopFreqChoice);
        if ((numObStops > 0) && (tour.getEscortTypeOutbound()!=ModelStructure.RIDE_SHARING_TYPE) && (tour.getEscortTypeOutbound()!=ModelStructure.PURE_ESCORTING_TYPE))
        {
            // get a stop purpose for each outbound stop generated, plus the
            // stop at
            // the primary destination
            String[] obStopOrigPurposes = new String[numObStops + 1];
            String[] obStopDestPurposes = new String[numObStops + 1];
            int[] obStopPurposeIndices = new int[numObStops + 1];
            obStopOrigPurposes[0] = tour.getTourCategory().equalsIgnoreCase(
                    ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
            for (int i = 0; i < numObStops; i++)
            {
                if (i > 0) obStopOrigPurposes[i] = obStopDestPurposes[i - 1];
                obStopPurposeIndices[i] = getStopPurpose(hh, OUTBOUND_DIRECTION_NAME,
                        tourDepartPeriod, tourPrimaryPurpose, personType);
                obStopDestPurposes[i] = indexPurposeMap.get(obStopPurposeIndices[i]);
            }
            obStopOrigPurposes[numObStops] = obStopDestPurposes[numObStops - 1];
            obStopDestPurposes[numObStops] = tourPrimaryPurpose;
            // the last stop record is for the trip from stop to destination

            // pass in the array of stop purposes; length of array determines
            // number
            // of outbound stop objects created.
            if (tour.getOutboundStops() != null)
            {
                Exception e = new RuntimeException();
                logger.error("outbound stops array for hhid=" + tour.getHhId() + ", person="
                        + tour.getPersonObject().getPersonNum() + ", tour=" + tour.getTourId()
                        + ", purpose=" + tour.getTourPurpose(), e);
                try
                {
                    throw e;
                } catch (Exception e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            tour.createOutboundStops(obStopOrigPurposes, obStopDestPurposes, obStopPurposeIndices);
        }

        int numIbStops = dmuObject.getNumIbStopsAlt(stopFreqChoice);
        if ((numIbStops > 0) && (tour.getEscortTypeInbound()!=ModelStructure.RIDE_SHARING_TYPE) && (tour.getEscortTypeInbound()!=ModelStructure.PURE_ESCORTING_TYPE))
        {
            // get a stop purpose for each inbound stop generated
            String[] ibStopOrigPurposes = new String[numIbStops + 1];
            String[] ibStopDestPurposes = new String[numIbStops + 1];
            int[] ibStopPurposeIndices = new int[numIbStops + 1];
            ibStopOrigPurposes[0] = tour.getTourPrimaryPurpose();
            for (int i = 0; i < numIbStops; i++)
            {
                if (i > 0) ibStopOrigPurposes[i] = ibStopDestPurposes[i - 1];
                ibStopPurposeIndices[i] = getStopPurpose(hh, INBOUND_DIRECTION_NAME,
                        tourArrivePeriod, tourPrimaryPurpose, personType);
                ibStopDestPurposes[i] = indexPurposeMap.get(ibStopPurposeIndices[i]);
            }
            ibStopOrigPurposes[numIbStops] = ibStopDestPurposes[numIbStops - 1];
            ibStopDestPurposes[numIbStops] = tour.getTourCategory().equalsIgnoreCase(
                    ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
            // the last stop record is for the trip from stop to home or work

            // pass in the array of stop purposes; length of array determines
            // number
            // of inbound stop objects created.
            if (tour.getInboundStops() != null)
            {
                Exception e = new RuntimeException();
                logger.error("inbound stops array for hhid=" + tour.getHhId() + ", person="
                        + tour.getPersonObject().getPersonNum() + ", tour=" + tour.getTourId()
                        + ", purpose=" + tour.getTourPurpose(), e);
                try
                {
                    throw e;
                } catch (Exception e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            tour.createInboundStops(ibStopOrigPurposes, ibStopDestPurposes, ibStopPurposeIndices);
        }

        return numObStops + numIbStops;

    }

    private void readPurposeLookupProportionsTable(String purposeLookupFilename)
    {

        // read the stop purpose proportions into a TableDataSet
        TableDataSet purposeLookupTable = null;
        String fileName = "";
        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            purposeLookupTable = reader.readFile(new File(purposeLookupFilename));
        } catch (Exception e)
        {
            logger.error(String.format(
                    "Exception occurred reading stop purpose lookup proportions file: %s.",
                    fileName), e);
            throw new RuntimeException();
        }

        // allocate a HashMap array for each direction, dimensioned to maximum
        // departure hour, to map keys determined by combination of categories
        // to
        // proportions arrays.
        int numDepartPeriods = modelStructure.getNumberOfTimePeriods();
        outProportionsMaps = new HashMap[numDepartPeriods + 1];
        inProportionsMaps = new HashMap[numDepartPeriods + 1];
        for (int i = 0; i <= numDepartPeriods; i++)
        {
            outProportionsMaps[i] = new HashMap<String, double[]>();
            inProportionsMaps[i] = new HashMap<String, double[]>();
        }

        // create a mapping between names used in lookup file and purpose names
        // used
        // in model
        HashMap<String, String> primaryPurposeMap = new HashMap<String, String>();
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_WORK_NAME,
                ModelStructure.WORK_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_UNIVERSITY_NAME,
                ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_SCHOOL_NAME,
                ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_ESCORT_NAME,
                ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_SHOPPING_NAME,
                ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_EAT_OUT_NAME,
                ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_MAINT_NAME,
                ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_VISIT_NAME,
                ModelStructure.VISITING_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_DISCR_NAME,
                ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME);
        primaryPurposeMap.put(dmuObject.STOP_PURPOSE_FILE_WORK_BASED_NAME,
                ModelStructure.WORK_BASED_PRIMARY_PURPOSE_NAME);

        // create a mapping between stop purpose alternative indices selected
        // from
        // monte carlo process and stop purpose names used in model
        // the indices are the order of the proportions columns in the table
        indexPurposeMap = new HashMap<Integer, String>();
        indexPurposeMap.put(1, "work related");
        indexPurposeMap.put(2, ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(3, ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(4, ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(5, ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(6, ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(7, ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(8, ModelStructure.VISITING_PRIMARY_PURPOSE_NAME);
        indexPurposeMap.put(9, ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME);

        // create a mapping between names used in lookup file and person type
        // names
        // used in model
        HashMap<String, String> personTypeMap = new HashMap<String, String>();
        personTypeMap.put(FT_WORKER_PERSON_TYPE_NAME, Person.PERSON_TYPE_FULL_TIME_WORKER_NAME);
        personTypeMap.put(PT_WORKER_PERSON_TYPE_NAME, Person.PERSON_TYPE_PART_TIME_WORKER_NAME);
        personTypeMap.put(UNIVERSITY_PERSON_TYPE_NAME, Person.PERSON_TYPE_UNIVERSITY_STUDENT_NAME);
        personTypeMap.put(NONWORKER_PERSON_TYPE_NAME, Person.PERSON_TYPE_NON_WORKER_NAME);
        personTypeMap.put(RETIRED_PERSON_TYPE_NAME, Person.PERSON_TYPE_RETIRED_NAME);
        personTypeMap
                .put(DRIVING_STUDENT_PERSON_TYPE_NAME, Person.PERSON_TYPE_STUDENT_DRIVING_NAME);
        personTypeMap.put(NONDRIVING_STUDENT_PERSON_TYPE_NAME,
                Person.PERSON_TYPE_STUDENT_NON_DRIVING_NAME);
        personTypeMap.put(PRESCHOOL_PERSON_TYPE_NAME, Person.PERSON_TYPE_PRE_SCHOOL_CHILD_NAME);
        personTypeMap.put(ALL_PERSON_TYPE_NAME, ALL_PERSON_TYPE_NAME);

        // fields in lookup file are:
        // PrimPurp Direction DepartRangeStart DepartRangeEnd Ptype Work
        // University
        // School Escort Shop Maintenance Eating Out Visiting Discretionary

        // populate the outProportionsMaps and inProportionsMaps arrays of maps
        // from
        // data in the TableDataSet.
        // when stops are generated, they can lookup the proportions for stop
        // purpose
        // selection from a map determined
        // by tour purpose, person type, outbound/inbound direction and tour
        // departure time. From these proportions,
        // a stop purpose can be drawn.

        // loop over rows in the TableDataSet
        for (int i = 0; i < purposeLookupTable.getRowCount(); i++)
        {

            // get the tour primary purpose
            String tourPrimPurp = primaryPurposeMap.get(purposeLookupTable.getStringValueAt(i + 1,
                    TOUR_PRIMARY_PURPOSE_COLUMN_HEADING));

            // get the half tour direction
            String direction = purposeLookupTable.getStringValueAt(i + 1,
                    HALF_TOUR_DIRECTION_COLUMN_HEADING);

            // get the beginning of the range of departure hours
            int departPeriodRangeStart = (int) purposeLookupTable.getValueAt(i + 1,
                    TOUR_DEPARTURE_START_RANGE_COLUMN_HEADING);

            // get the end of the range of departure hours
            int arriveperiodRangeEnd = (int) purposeLookupTable.getValueAt(i + 1,
                    TOUR_DEPARTURE_END_RANGE_COLUMN_HEADING);

            int startRange = modelStructure.getTimePeriodIndexForTime(departPeriodRangeStart);
            int endRange = modelStructure.getTimePeriodIndexForTime(arriveperiodRangeEnd);

            // get the person type
            String personType = personTypeMap.get(purposeLookupTable.getStringValueAt(i + 1,
                    PERSON_TYPE_COLUMN_HEADING));

            // columns following person type are proportions by stop purpose.
            // Get the
            // index of the first stop purpose proportion.
            int firstPropColumn = purposeLookupTable.getColumnPosition(PERSON_TYPE_COLUMN_HEADING) + 1;

            // starting at this column, read the proportions for all stop
            // purposes.
            // Create the array of proportions for this table record.
            double[] props = new double[indexPurposeMap.size()];
            for (int j = 0; j < props.length; j++)
            {
                props[j] = purposeLookupTable.getValueAt(i + 1, firstPropColumn + j);
            }

            // get a HashMap for the direction and each hour in the start/end
            // range,
            // and store the proportions in that map for the key.
            // the key to use for any of these HashMaps is created consisting of
            // "TourPrimPurp_PersonType"
            // if the person type for the record is "All", a key is defined for
            // each
            // person type, and the proportions stored for each key.
            if (personType.equalsIgnoreCase(ALL_PERSON_TYPE_NAME))
            {
                for (String ptype : personTypeMap.values())
                {
                    String key = tourPrimPurp + "_" + ptype;
                    if (direction.equalsIgnoreCase(OUTBOUND_DIRECTION_NAME))
                    {
                        for (int k = startRange; k <= endRange; k++)
                        {
                            outProportionsMaps[k].put(key, props);
                        }
                    } else if (direction.equalsIgnoreCase(INBOUND_DIRECTION_NAME))
                    {
                        for (int k = startRange; k <= endRange; k++)
                            inProportionsMaps[k].put(key, props);
                    }
                }
            } else
            {
                String key = tourPrimPurp + "_" + personType;
                if (direction.equalsIgnoreCase(OUTBOUND_DIRECTION_NAME))
                {
                    for (int k = startRange; k <= endRange; k++)
                        outProportionsMaps[k].put(key, props);
                } else if (direction.equalsIgnoreCase(INBOUND_DIRECTION_NAME))
                {
                    for (int k = startRange; k <= endRange; k++)
                        inProportionsMaps[k].put(key, props);
                }
            }

        }

    }

    private int getStopPurpose(Household household, String halfTourDirection, int tourDepartPeriod,
            String tourPrimaryPurpose, String personType)
    {

        double[] props = null;
        String key = tourPrimaryPurpose + "_" + personType;

        try
        {
            if (halfTourDirection.equalsIgnoreCase(OUTBOUND_DIRECTION_NAME)) props = outProportionsMaps[tourDepartPeriod]
                    .get(key);
            else if (halfTourDirection.equalsIgnoreCase(INBOUND_DIRECTION_NAME))
                props = inProportionsMaps[tourDepartPeriod].get(key);

            double rn = household.getHhRandom().nextDouble();
            int choice = ChoiceModelApplication.getMonteCarloSelection(props, rn);

            return (choice + 1);

        } catch (Exception e)
        {
            logger.error("exception caught trying to determine stop purpose.");
            logger.error("key=" + key + ", tourPrimaryPurpose=" + tourPrimaryPurpose
                    + ", personType=" + personType + ", halfTourDirection=" + halfTourDirection
                    + ", tourDepartPeriod=" + tourDepartPeriod);
            throw new RuntimeException();
        }

    }

}
