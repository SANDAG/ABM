package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.Random;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import org.sandag.abm.accessibilities.NonTransitUtilities;
import org.sandag.abm.application.SandagCtrampDmuFactory;
import org.sandag.abm.application.SandagHouseholdDataManager;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.VariableTable;
import com.pb.common.matrix.MatrixType;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.IndexSort;
import com.pb.common.util.ResourceUtil;

public class SubtourDestChoiceModel
        implements Serializable
{

    private transient Logger                     logger                                     = Logger.getLogger(SubtourDestChoiceModel.class);
    private transient Logger                     dcNonManLogger                             = Logger.getLogger("tourDcNonMan");
    private transient Logger                     todMcLogger                                = Logger.getLogger("todMcLogsum");

    // TODO eventually remove this target
    private static final String                  PROPERTIES_DC_UEC_FILE                     = "nmdc.uec.file";
    private static final String                  PROPERTIES_DC_UEC_FILE2                    = "nmdc.uec.file2";
    private static final String                  PROPERTIES_DC_SOA_UEC_FILE                 = "nmdc.soa.uec.file";

    private static final String                  USE_NEW_SOA_METHOD_PROPERTY_KEY            = "nmdc.use.new.soa";

    private static final String                  PROPERTIES_DC_SOA_NON_MAND_SAMPLE_SIZE_KEY = "nmdc.soa.SampleSize";

    private static final String                  PROPERTIES_DC_DATA_SHEET                   = "nmdc.data.page";

    private static final String                  PROPERTIES_DC_AT_WORK_MODEL_SHEET          = "nmdc.atwork.model.page";

    private static final String                  PROPERTIES_DC_SOA_AT_WORK_MODEL_SHEET      = "nmdc.soa.atwork.model.page";

    // size term array indices for purposes are 0-based
    public static final int                      PROPERTIES_AT_WORK_BUSINESS_SIZE_SHEET     = 11;
    public static final int                      PROPERTIES_AT_WORK_EAT_OUT_SIZE_SHEET      = 10;
    public static final int                      PROPERTIES_AT_WORK_OTHER_SIZE_SHEET        = 9;

    private static String[]                      tourPurposeNames;
    private static int[]                         tourPurposeIndices;

    // all three subtour purposes use the same DC sheet
    private static final String[]                DC_MODEL_SHEET_KEYS                        = {
            PROPERTIES_DC_AT_WORK_MODEL_SHEET, PROPERTIES_DC_AT_WORK_MODEL_SHEET,
            PROPERTIES_DC_AT_WORK_MODEL_SHEET                                               };

    // all three subtour purposes use the same SOA sheet
    private static final String[]                DC_SOA_MODEL_SHEET_KEYS                    = {
            PROPERTIES_DC_SOA_AT_WORK_MODEL_SHEET, PROPERTIES_DC_SOA_AT_WORK_MODEL_SHEET,
            PROPERTIES_DC_SOA_AT_WORK_MODEL_SHEET                                           };

    // all three subtour purposes use the same SOA sheet
    private final int[]                          sizeSheetKeys                              = {
            PROPERTIES_AT_WORK_BUSINESS_SIZE_SHEET, PROPERTIES_AT_WORK_EAT_OUT_SIZE_SHEET,
            PROPERTIES_AT_WORK_OTHER_SIZE_SHEET                                             };

    // set default depart periods that represents each model period
    private static final int                     EA                                         = 1;
    private static final int                     AM                                         = 8;
    private static final int                     MD                                         = 16;
    private static final int                     PM                                         = 26;
    private static final int                     EV                                         = 36;

    private static final int[][][]               PERIOD_COMBINATIONS                        = {
            { {AM, AM}, {MD, MD}, {PM, PM}}, { {AM, AM}, {MD, MD}, {PM, PM}},
            { {AM, AM}, {MD, MD}, {PM, PM}}                                                 };

    private static final double[][]              PERIOD_COMBINATION_COEFFICIENTS            = {
            {-3.1453, -0.1029, -2.9056}, {-3.1453, -0.1029, -2.9056}, {-3.1453, -0.1029, -2.9056}};

    private String                               tourCategory;
    private ModelStructure                       modelStructure;

    private int[]                                dcModelIndices;
    private HashMap<String, Integer>             purposeNameIndexMap;

    private HashMap<String, Integer>             subtourSegmentNameIndexMap;

    private double[][]                           dcSizeArray;

    private TourModeChoiceDMU                    mcDmuObject;
    private DestChoiceDMU                        dcDmuObject;
    private DestChoiceTwoStageModelDMU           dcDistSoaDmuObject;
    private DcSoaDMU                             dcSoaDmuObject;

    private boolean[]                            needToComputeLogsum;
    private double[]                             modeChoiceLogsums;

    private TourModeChoiceModel                  mcModel;
    private DestinationSampleOfAlternativesModel dcSoaModel;
    private ChoiceModelApplication[]             dcModel;
    private ChoiceModelApplication[]             dcModel2;

    private boolean[]                            dcModel2AltsAvailable;
    private int[]                                dcModel2AltsSample;
    private int[]                                dcModel2SampleValues;

    private BuildAccessibilities                 aggAcc;

    private TazDataManager                       tazs;
    private MgraDataManager                      mgraManager;

    private double[]                             mgraDistanceArray;

    private DestChoiceTwoStageModel              dcSoaTwoStageObject;

    private boolean                              useNewSoaMethod;

    private int                                  soaSampleSize;

    private long                                 soaRunTime;

    public SubtourDestChoiceModel(HashMap<String, String> propertyMap,
            ModelStructure myModelStructure, BuildAccessibilities myAggAcc,
            CtrampDmuFactoryIf dmuFactory, TourModeChoiceModel myMcModel)
    {

        tourCategory = ModelStructure.AT_WORK_CATEGORY;
        modelStructure = myModelStructure;
        mcModel = myMcModel;
        aggAcc = myAggAcc;

        tourPurposeIndices = new int[3];
        tourPurposeIndices[0] = modelStructure.AT_WORK_PURPOSE_INDEX_BUSINESS;
        tourPurposeIndices[1] = modelStructure.AT_WORK_PURPOSE_INDEX_EAT;
        tourPurposeIndices[2] = modelStructure.AT_WORK_PURPOSE_INDEX_MAINT;

        tourPurposeNames = new String[3];
        tourPurposeNames[0] = modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME;
        tourPurposeNames[1] = modelStructure.AT_WORK_EAT_PURPOSE_NAME;
        tourPurposeNames[2] = modelStructure.AT_WORK_MAINT_PURPOSE_NAME;

        logger.info(String.format("creating %s subtour dest choice mode instance", tourCategory));

        mgraManager = MgraDataManager.getInstance();
        tazs = TazDataManager.getInstance();  

        soaSampleSize = Util.getIntegerValueFromPropertyMap(propertyMap,
                PROPERTIES_DC_SOA_NON_MAND_SAMPLE_SIZE_KEY);

        useNewSoaMethod = Util.getBooleanValueFromPropertyMap(propertyMap,
                USE_NEW_SOA_METHOD_PROPERTY_KEY);

        if (useNewSoaMethod)
            dcSoaTwoStageObject = new DestChoiceTwoStageModel(propertyMap, soaSampleSize);

        // create an array of ChoiceModelApplication objects for each choice
        // purpose
        setupDestChoiceModelArrays(propertyMap, dmuFactory);

    }

    private void setupDestChoiceModelArrays(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);

        String dcUecFileName = propertyMap.get(PROPERTIES_DC_UEC_FILE);
        dcUecFileName = uecFileDirectory + dcUecFileName;

        String dcUecFileName2 = propertyMap.get(PROPERTIES_DC_UEC_FILE2);
        dcUecFileName2 = uecFileDirectory + dcUecFileName2;

        String soaUecFileName = propertyMap.get(PROPERTIES_DC_SOA_UEC_FILE);
        soaUecFileName = uecFileDirectory + soaUecFileName;

        int dcModelDataSheet = Util.getIntegerValueFromPropertyMap(propertyMap,
                PROPERTIES_DC_DATA_SHEET);
        int soaSampleSize = Util.getIntegerValueFromPropertyMap(propertyMap,
                PROPERTIES_DC_SOA_NON_MAND_SAMPLE_SIZE_KEY);

        dcDmuObject = dmuFactory.getDestChoiceDMU();
        dcDmuObject.setAggAcc(aggAcc);
        dcDmuObject.setAccTable(aggAcc.getAccessibilitiesTableObject());

        if (useNewSoaMethod)
        {
            dcDistSoaDmuObject = dmuFactory.getDestChoiceSoaTwoStageDMU();
            dcDistSoaDmuObject.setAggAcc(aggAcc);
            dcDistSoaDmuObject.setAccTable(aggAcc.getAccessibilitiesTableObject());
        }

        dcSoaDmuObject = dmuFactory.getDcSoaDMU();
        dcSoaDmuObject.setAggAcc(aggAcc);

        mcDmuObject = dmuFactory.getModeChoiceDMU();

        int numLogsumIndices = modelStructure.getSkimPeriodCombinationIndices().length;
        needToComputeLogsum = new boolean[numLogsumIndices];
        modeChoiceLogsums = new double[numLogsumIndices];

        // create the arrays of dc model and soa model indices
        int[] uecSheetIndices = new int[tourPurposeNames.length];
        int[] soaUecSheetIndices = new int[tourPurposeNames.length];

        purposeNameIndexMap = new HashMap<String, Integer>(tourPurposeNames.length);

        int i = 0;
        for (String purposeName : tourPurposeNames)
        {
            int uecIndex = Util.getIntegerValueFromPropertyMap(propertyMap, DC_MODEL_SHEET_KEYS[i]);
            int soaUecIndex = Util.getIntegerValueFromPropertyMap(propertyMap,
                    DC_SOA_MODEL_SHEET_KEYS[i]);
            purposeNameIndexMap.put(purposeName, i);
            uecSheetIndices[i] = uecIndex;
            soaUecSheetIndices[i] = soaUecIndex;
            i++;
        }

        // create a lookup array to map purpose index to model index
        dcModelIndices = new int[uecSheetIndices.length];

        // get a set of unique model sheet numbers so that we can create
        // ChoiceModelApplication objects once for each model sheet used
        // also create a HashMap to relate size segment index to SOA Model
        // objects
        HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();
        int dcModelIndex = 0;
        int dcSegmentIndex = 0;
        for (int uecIndex : uecSheetIndices)
        {
            // if the uec sheet for the model segment is not in the map, add it,
            // otherwise, get it from the map
            if (!modelIndexMap.containsKey(uecIndex))
            {
                modelIndexMap.put(uecIndex, dcModelIndex);
                dcModelIndices[dcSegmentIndex] = dcModelIndex++;
            } else
            {
                dcModelIndices[dcSegmentIndex] = modelIndexMap.get(uecIndex);
            }

            dcSegmentIndex++;
        }

        // the size term array in aggAcc gives mgra*purpose - need an array of
        // all mgras for one purpose
        double[][] aggAccDcSizeArray = aggAcc.getSizeTerms();
        subtourSegmentNameIndexMap = new HashMap<String, Integer>();
        for (int k = 0; k < tourPurposeIndices.length; k++)
        {
            subtourSegmentNameIndexMap.put(tourPurposeNames[k], k);
        }

        dcSizeArray = new double[tourPurposeNames.length][aggAccDcSizeArray.length];
        for (i = 0; i < aggAccDcSizeArray.length; i++)
        {
            for (int m : subtourSegmentNameIndexMap.values())
            {
                int s = sizeSheetKeys[m];
                dcSizeArray[m][i] = aggAccDcSizeArray[i][s];
            }
        }

        // create a sample of alternatives choice model object for use in
        // selecting a sample
        // of all possible destination choice alternatives.
        dcSoaModel = new DestinationSampleOfAlternativesModel(soaUecFileName, soaSampleSize,
                propertyMap, mgraManager, dcSizeArray, dcSoaDmuObject, soaUecSheetIndices);

        dcModel = new ChoiceModelApplication[modelIndexMap.size()];

        if (useNewSoaMethod)
        {
            dcModel2 = new ChoiceModelApplication[modelIndexMap.size()];
            dcModel2AltsAvailable = new boolean[soaSampleSize + 1];
            dcModel2AltsSample = new int[soaSampleSize + 1];
            dcModel2SampleValues = new int[soaSampleSize];
        }

        i = 0;
        for (int uecIndex : modelIndexMap.keySet())
        {

            try
            {
                dcModel[i] = new ChoiceModelApplication(dcUecFileName, uecIndex, dcModelDataSheet,
                        propertyMap, (VariableTable) dcDmuObject);

                if (useNewSoaMethod)
                {
                    dcModel2[i] = new ChoiceModelApplication(dcUecFileName2, uecIndex,
                            dcModelDataSheet, propertyMap, (VariableTable) dcDistSoaDmuObject);
                }

                i++;
            } catch (RuntimeException e)
            {
                logger.error(String
                        .format("exception caught setting up ATWork DC ChoiceModelApplication[%d] for model index=%d of %d models",
                                i, i, modelIndexMap.size()));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

        }

        mgraDistanceArray = new double[mgraManager.getMaxMgra() + 1];

    }

    public void applyModel(Household hh)
    {

        soaRunTime = 0;

        if (useNewSoaMethod) dcSoaTwoStageObject.resetSoaRunTime();
        else dcSoaModel.resetSoaRunTime();

        // declare these variables here so their values can be logged if a
        // RuntimeException occurs.
        int i = -1;

        Logger modelLogger = dcNonManLogger;
        if (hh.getDebugChoiceModels())
            hh.logHouseholdObject("Pre Subtour Location Choice Household " + hh.getHhId()
                    + " Object", modelLogger);

        Person[] persons = hh.getPersons();

        for (i = 1; i < persons.length; i++)
        {

            Person p = persons[i];

            // get the at-work subtours for this person and choose a destination
            // for each.
            ArrayList<Tour> tourList = p.getListOfAtWorkSubtours();

            int currentTourNum = 0;
            for (Tour tour : tourList)
            {

                Tour workTour = null;
                int workTourIndex = 0;
                workTourIndex = tour.getWorkTourIndexFromSubtourId(tour.getTourId());
                workTour = p.getListOfWorkTours().get(workTourIndex);

                int chosen = -1;
                try
                {

                    int homeMgra = hh.getHhMgra();
                    int homeTaz = hh.getHhTaz();
                    int origMgra = workTour.getTourDestMgra();
                    tour.setTourOrigMgra(origMgra);

                    // update the MC dmuObject for this person
                    mcDmuObject.setHouseholdObject(hh);
                    mcDmuObject.setPersonObject(p);
                    mcDmuObject.setTourObject(tour);
                    mcDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra, 0,
                            hh.getDebugChoiceModels());
                    mcDmuObject.setOriginMgra(origMgra);

                    // update the DC dmuObject for this person
                    dcDmuObject.setHouseholdObject(hh);
                    dcDmuObject.setPersonObject(p);
                    dcDmuObject.setTourObject(tour);
                    dcDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra, 0);

                    if (useNewSoaMethod)
                    {
                        dcDistSoaDmuObject.setHouseholdObject(hh);
                        dcDistSoaDmuObject.setPersonObject(p);
                        dcDistSoaDmuObject.setTourObject(tour);
                        dcDistSoaDmuObject.setDmuIndexValues(hh.getHhId(), homeTaz, origMgra, 0);
                    }

                    // for At-work Subtour DC, just count remaining At-work
                    // Subtour tours
                    int toursLeftCount = tourList.size() - currentTourNum;
                    dcDmuObject.setToursLeftCount(toursLeftCount);
                    if (useNewSoaMethod) dcDistSoaDmuObject.setToursLeftCount(toursLeftCount);

                    // get the tour location alternative chosen from the sample
                    if (useNewSoaMethod)
                    {
                        chosen = selectLocationFromTwoStageSampleOfAlternatives(tour, mcDmuObject);
                        soaRunTime += dcSoaTwoStageObject.getSoaRunTime();
                    } else
                    {
                        chosen = selectLocationFromSampleOfAlternatives(tour, dcDmuObject,
                                dcSoaDmuObject, mcDmuObject);
                        soaRunTime += dcSoaModel.getSoaRunTime();
                    }

                } catch (RuntimeException e)
                {
                    logger.fatal(String
                            .format("exception caught selecting %s tour destination choice for hh.hhid=%d, personNum=%d, tourId=%d, purposeName=%s",
                                    tourCategory, hh.getHhId(), p.getPersonNum(), tour.getTourId(),
                                    tour.getSubTourPurpose()));
                    logger.fatal("Exception caught:", e);
                    logger.fatal("Throwing new RuntimeException() to terminate.");
                    throw new RuntimeException();
                }

                // set chosen values in tour object
                tour.setTourDestMgra(chosen);

                currentTourNum++;
            }

        }

        hh.setAwlRandomCount(hh.getHhRandomCount());

    }

    /**
     * 
     * @return chosen mgra.
     */
    private int selectLocationFromSampleOfAlternatives(Tour tour, DestChoiceDMU dcDmuObject,
            DcSoaDMU dcSoaDmuObject, TourModeChoiceDMU mcDmuObject)
    {

        Logger modelLogger = dcNonManLogger;

        // get the Household object for the person making this subtour
        Person person = tour.getPersonObject();

        // get the Household object for the person making this subtour
        Household household = person.getHouseholdObject();

        // get the tour purpose name
        String tourPurposeName = tour.getSubTourPurpose();
        int tourPurposeIndex = purposeNameIndexMap.get(tourPurposeName);

        dcSoaDmuObject.setDestChoiceSize(dcSizeArray[tourPurposeIndex]);

        // the originMgra in the tour object is already set to the work tour
        // dest mgra
        // double[] workMgraDistanceArray =
        // mandAcc.calculateDistancesForAllMgras( tour.getTourOrigMgra() );
        mcModel.getAnmSkimCalculator().getOpSkimDistancesFromMgra(tour.getTourOrigMgra(),
                mgraDistanceArray);
        dcSoaDmuObject.setDestDistance(mgraDistanceArray);

        dcDmuObject.setDestChoiceSize(dcSizeArray[tourPurposeIndex]);
        dcDmuObject.setDestChoiceDistance(mgraDistanceArray);

        // compute the sample of alternatives set for the person
        dcSoaModel.computeDestinationSampleOfAlternatives(dcSoaDmuObject, tour, person,
                tourPurposeName, tourPurposeIndex, tour.getTourOrigMgra());

        // get sample of locations and correction factors for sample
        int[] finalSample = dcSoaModel.getSampleOfAlternatives();
        float[] sampleCorrectionFactors = dcSoaModel.getSampleOfAlternativesCorrections();

        int m = dcModelIndices[tourPurposeIndex];
        int numAlts = dcModel[m].getNumberOfAlternatives();

        // set the destAltsAvailable array to true for all destination choice
        // alternatives for each purpose
        boolean[] destAltsAvailable = new boolean[numAlts + 1];
        for (int k = 0; k <= numAlts; k++)
            destAltsAvailable[k] = false;

        // set the destAltsSample array to 1 for all destination choice
        // alternatives
        // for each purpose
        int[] destAltsSample = new int[numAlts + 1];
        for (int k = 0; k <= numAlts; k++)
            destAltsSample[k] = 0;

        int[] sampleValues = new int[finalSample.length];

        // for the destinations and sub-zones in the sample, compute mc logsums
        // and
        // save in DC dmuObject.
        // also save correction factor and set availability and sample value for
        // the
        // sample alternative to true. 1, respectively.
        for (int i = 1; i < finalSample.length; i++)
        {

            int destMgra = finalSample[i];
            sampleValues[i] = finalSample[i];

            // set logsum value in DC dmuObject for the logsum index, sampled
            // zone and subzone.
            double logsum = calculateSimpleTODChoiceLogsum(person, tour, destMgra, i);
            dcDmuObject.setMcLogsum(destMgra, logsum);

            // set sample of alternatives correction factor used in destination
            // choice utility for the sampled alternative.
            dcDmuObject.setDcSoaCorrections(destMgra, sampleCorrectionFactors[i]);

            // set availaibility and sample values for the purpose, dcAlt.
            destAltsAvailable[finalSample[i]] = true;
            destAltsSample[finalSample[i]] = 1;

        }

        // log headers to traceLogger if the person making the destination
        // choice is
        // from a household requesting trace information
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";

        if (household.getDebugChoiceModels())
        {

            // null tour means the DC is a mandatory usual location choice
            choiceModelDescription = String.format(
                    "At-work Subtour Location Choice Model for: tour purpose=%s", tourPurposeName);
            decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s, TourId=%d",
                    person.getHouseholdObject().getHhId(), person.getPersonNum(),
                    person.getPersonType(), tour.getTourId());

            modelLogger.info(" ");
            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info("At-work Subtour Location Choice Model for tour purpose="
                    + tourPurposeName + ", Person Num: " + person.getPersonNum()
                    + ", Person Type: " + person.getPersonType() + ", TourId=" + tour.getTourId());

            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            dcModel[m].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        // compute destination choice proportions and choose alternative
        float logsum = (float) dcModel[m].computeUtilities(dcDmuObject, dcDmuObject.getDmuIndexValues(),
                destAltsAvailable, destAltsSample);

        tour.setTourDestinationLogsum(logsum);
        
        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        if (dcModel[m].getAvailabilityCount() > 0)
        {
            try
            {
                chosen = dcModel[m].getChoiceResult(rn);
            } catch (Exception e)
            {
                logger.error(String
                        .format("Exception caught for HHID=%d, PersonNum=%d, tourId=%d, in %s destination choice.",
                                dcDmuObject.getHouseholdObject().getHhId(), dcDmuObject
                                        .getPersonObject().getPersonNum(), tour.getTourId(),
                                tourPurposeName));
                throw new RuntimeException();
            }
        }

        // write choice model alternative info to log file
        int selectedIndex = -1;
        for (int j = 1; j < finalSample.length; j++)
        {
            if (finalSample[j] == chosen)
            {
                selectedIndex = j;
                break;
            }
        }

        if (household.getDebugChoiceModels() || chosen <= 0)
        {

            double[] utilities = dcModel[m].getUtilities();
            double[] probabilities = dcModel[m].getProbabilities();
            boolean[] availabilities = dcModel[m].getAvailabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------- --------------    --------------    --------------    --------------");

            int[] sortedSampleValueIndices = IndexSort.indexSort(sampleValues);

            double cumProb = 0.0;
            for (int j = 1; j < finalSample.length; j++)
            {
                int k = sortedSampleValueIndices[j];
                int alt = finalSample[k];

                if (finalSample[k] == chosen) selectedIndex = j;

                cumProb += probabilities[alt - 1];
                String altString = String.format("j=%d, mgra=%d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[alt], utilities[alt - 1], probabilities[alt - 1], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("j=%d, mgra=%d", selectedIndex, chosen);
            modelLogger.info(String.format("Choice: %s with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info(" ");

            dcModel[m].logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            dcModel[m].logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            dcModel[m].logUECResults(modelLogger, loggingHeader);

            if (chosen < 0)
            {
                logger.error(String
                        .format("Exception caught for HHID=%d, PersonNum=%d, tourId=%d, tourPurpose=%d, no available %s destination choice alternatives to choose from in ChoiceModelApplication.",
                                dcDmuObject.getHouseholdObject().getHhId(), dcDmuObject
                                        .getPersonObject().getPersonNum(), tour.getTourId(),
                                tourPurposeName));
                throw new RuntimeException();
            }

        }

        return chosen;

    }

    /**
     * 
     * @return chosen mgra.
     */
    private int selectLocationFromTwoStageSampleOfAlternatives(Tour tour,
            TourModeChoiceDMU mcDmuObject)
    {

        // set tour origin taz/subzone and start/end times for calculating mode
        // choice logsum
        Logger modelLogger = dcNonManLogger;

        // get the Household object for the person making this non-mandatory
        // tour
        Person person = tour.getPersonObject();

        // get the Household object for the person making this non-mandatory
        // tour
        Household household = person.getHouseholdObject();

        // get the tour purpose name
        String tourPurposeName = tour.getSubTourPurpose();
        int tourPurposeIndex = purposeNameIndexMap.get(tourPurposeName);

        // get sample of locations and correction factors for sample using the
        // alternate method
        // for non-mandatory tour destination choice, the sizeSegmentType INdex
        // and sizeSegmentIndex are the same values.
        dcSoaTwoStageObject.chooseSample(mgraManager.getTaz(tour.getTourOrigMgra()),
                tourPurposeIndex, tourPurposeIndex, soaSampleSize, household.getHhRandom(),
                household.getDebugChoiceModels());
        int[] finalSample = dcSoaTwoStageObject.getUniqueSampleMgras();
        double[] sampleCorrectionFactors = dcSoaTwoStageObject
                .getUniqueSampleMgraCorrectionFactors();
        int numUniqueAlts = dcSoaTwoStageObject.getNumberofUniqueMgrasInSample();

        int m = dcModelIndices[tourPurposeIndex];
        int numAlts = dcModel2[m].getNumberOfAlternatives();

        Arrays.fill(dcModel2AltsAvailable, false);
        Arrays.fill(dcModel2AltsSample, 0);
        Arrays.fill(dcModel2SampleValues, 999999);

        mcModel.getAnmSkimCalculator().getOpSkimDistancesFromMgra(tour.getTourOrigMgra(),
                mgraDistanceArray);
        dcDistSoaDmuObject.setMgraDistanceArray(mgraDistanceArray);

        int sizeIndex = subtourSegmentNameIndexMap.get(tourPurposeName);
        dcDistSoaDmuObject.setMgraSizeArray(dcSizeArray[sizeIndex]);

        // set sample of alternatives correction factors used in destination
        // choice utility for the sampled alternatives.
        dcDistSoaDmuObject.setDcSoaCorrections(sampleCorrectionFactors);

        // for the destination mgras in the sample, compute mc logsums and save
        // in dmuObject.
        // also save correction factor and set availability and sample value for
        // the
        // sample alternative to true. 1, respectively.
        for (int i = 0; i < numUniqueAlts; i++)
        {

            int destMgra = finalSample[i];
            dcModel2SampleValues[i] = finalSample[i];

            // set logsum value in DC dmuObject for the logsum index, sampled
            // zone and subzone.
            double logsum = calculateSimpleTODChoiceLogsum(person, tour, destMgra, i);
            dcDistSoaDmuObject.setMcLogsum(i, logsum);

            // set availaibility and sample values for the purpose, dcAlt.
            dcModel2AltsAvailable[i + 1] = true;
            dcModel2AltsSample[i + 1] = 1;

        }

        dcDistSoaDmuObject.setSampleArray(dcModel2SampleValues);

        // log headers to traceLogger if the person making the destination
        // choice is
        // from a household requesting trace information
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";

        if (household.getDebugChoiceModels())
        {

            // null tour means the DC is a mandatory usual location choice
            choiceModelDescription = String.format(
                    "Non-Mandatory Location Choice Model for: tour purpose=%s", tourPurposeName);
            decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s, TourId=%d",
                    person.getHouseholdObject().getHhId(), person.getPersonNum(),
                    person.getPersonType(), tour.getTourId());

            modelLogger.info(" ");
            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info("Non-Mandatory Location Choice Model for tour purpose="
                    + tourPurposeName + ", Person Num: " + person.getPersonNum()
                    + ", Person Type: " + person.getPersonType() + ", TourId=" + tour.getTourId());

            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            dcModel2[m].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        // compute destination choice proportions and choose alternative
        float modelLogsum = (float) dcModel2[m].computeUtilities(dcDistSoaDmuObject, dcDistSoaDmuObject.getDmuIndexValues(),
                dcModel2AltsAvailable, dcModel2AltsSample);
        tour.setTourDestinationLogsum(modelLogsum);
        

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        if (dcModel2[m].getAvailabilityCount() > 0)
        {
            try
            {
                chosen = dcModel2[m].getChoiceResult(rn);
            } catch (Exception e)
            {
                logger.error(String
                        .format("Exception caught for HHID=%d, PersonNum=%d, tourId=%d, in %s destination choice.",
                                dcDistSoaDmuObject.getHouseholdObject().getHhId(),
                                dcDistSoaDmuObject.getPersonObject().getPersonNum(),
                                tour.getTourId(), tourPurposeName));
                throw new RuntimeException();
            }
        }

        if (household.getDebugChoiceModels() || chosen <= 0)
        {

            double[] utilities = dcModel2[m].getUtilities();
            double[] probabilities = dcModel2[m].getProbabilities();
            boolean[] availabilities = dcModel2[m].getAvailabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------- --------------    --------------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 0; j < finalSample.length; j++)
            {
                int alt = finalSample[j];
                cumProb += probabilities[j];
                String altString = String.format("j=%d, mgra=%d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j + 1], utilities[j], probabilities[j], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("j=%d, mgra=%d", chosen - 1, finalSample[chosen - 1]);
            modelLogger.info(String.format("Choice: %s with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info(" ");

            dcModel2[m].logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            dcModel2[m].logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            dcModel2[m].logUECResults(modelLogger, loggingHeader);

            if (chosen < 0)
            {
                logger.error(String
                        .format("Exception caught for HHID=%d, PersonNum=%d, tourId=%d, tourPurpose=%d, no available %s destination choice alternatives to choose from in ChoiceModelApplication.",
                                dcDistSoaDmuObject.getHouseholdObject().getHhId(),
                                dcDistSoaDmuObject.getPersonObject().getPersonNum(),
                                tour.getTourId(), tourPurposeName));
                throw new RuntimeException();
            }

        }

        return chosen;

    }

    private void setModeChoiceDmuAttributes(Household household, Person person, Tour t,
            int startPeriod, int endPeriod, int sampleDestMgra)
    {

        t.setTourDestMgra(sampleDestMgra);
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
                t.getTourOrigMgra(), sampleDestMgra, household.getDebugChoiceModels());
        

        mcDmuObject.setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(t
                .getTourOrigMgra())));
        mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager
                .getTaz(sampleDestMgra)));
        
        mcDmuObject.setOriginMgra(t.getTourOrigMgra());
        mcDmuObject.setDestMgra(t.getTourDestMgra());

    }

    private double calculateSimpleTODChoiceLogsum(Person person, Tour tour, int sampleDestMgra,
            int sampleNum)
    {

        Household household = person.getHouseholdObject();

        Arrays.fill(needToComputeLogsum, true);
        Arrays.fill(modeChoiceLogsums, -999);

        Logger modelLogger = todMcLogger;
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        if (household.getDebugChoiceModels())
        {
            choiceModelDescription = String
                    .format("At-work Subtour Simplified TOD logsum calculations for %s Location Choice, Sample Number %d",
                            tour.getSubTourPurpose(), sampleNum);
            decisionMakerLabel = String.format(
                    "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d non-mand tours",
                    household.getHhId(), person.getPersonNum(), person.getPersonType(),
                    tour.getTourId(), person.getListOfAtWorkSubtours().size());
            loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);
        }

        int i = 0;
        int tourPurposeIndex = purposeNameIndexMap.get(tour.getSubTourPurpose());
        double totalExpUtility = 0.0;
        for (int[] combo : PERIOD_COMBINATIONS[tourPurposeIndex])
        {
            int startPeriod = combo[0];
            int endPeriod = combo[1];

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
                setModeChoiceDmuAttributes(household, person, tour, startPeriod, endPeriod,
                        sampleDestMgra);

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
                            + periodString + " " + tour.getTourPrimaryPurpose() + " tour.");
                    logger.fatal("choiceModelDescription = " + choiceModelDescription);
                    logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
                    e.printStackTrace();
                    System.exit(-1);
                    // throw new RuntimeException(e);
                }
                needToComputeLogsum[index] = false;
            }

            double expUtil = Math.exp(modeChoiceLogsums[index]
                    + PERIOD_COMBINATION_COEFFICIENTS[tourPurposeIndex][i]);
            totalExpUtility += expUtil;

            if (household.getDebugChoiceModels())
                modelLogger
                        .info("i = "
                                + i
                                + ", purpose = "
                                + tourPurposeIndex
                                + ", "
                                + modelStructure.getModelPeriodLabel(modelStructure
                                        .getModelPeriodIndex(startPeriod))
                                + " to "
                                + modelStructure.getModelPeriodLabel(modelStructure
                                        .getModelPeriodIndex(endPeriod))
                                + " MCLS = "
                                + modeChoiceLogsums[index]
                                + ", ASC = "
                                + PERIOD_COMBINATION_COEFFICIENTS[tourPurposeIndex][i]
                                + ", (MCLS + ASC) = "
                                + (modeChoiceLogsums[index] + PERIOD_COMBINATION_COEFFICIENTS[tourPurposeIndex][i])
                                + ", exp(MCLS + ASC) = " + expUtil + ", cumExpUtility = "
                                + totalExpUtility);

            i++;
        }

        double logsum = Math.log(totalExpUtility);

        if (household.getDebugChoiceModels())
            modelLogger.info("final simplified TOD logsum = " + logsum);

        return logsum;
    }

    public void setNonMandatorySoaProbs(double[][][] soaDistProbs, double[][][] soaSizeProbs)
    {
        if (useNewSoaMethod)
        {
            dcSoaTwoStageObject.setTazDistProbs(soaDistProbs);
            dcSoaTwoStageObject.setMgraSizeProbs(soaSizeProbs);
        }
    }

    public long getSoaRunTime()
    {
        return soaRunTime;
    }

    public void resetSoaRunTime()
    {
        soaRunTime = 0;
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

        MatrixDataManager mdm = MatrixDataManager.getInstance();
        mdm.setMatrixDataServerObject(ms);

        MgraDataManager mgraManager = MgraDataManager.getInstance(propertyMap);
        TazDataManager tazManager = TazDataManager.getInstance(propertyMap);

        /*
         *         
         */
        ModelStructure modelStructure = new SandagModelStructure();
        SandagCtrampDmuFactory dmuFactory = new SandagCtrampDmuFactory(modelStructure,propertyMap);

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
        double[][][] maasExpUtilities = null;
        NonTransitUtilities ntUtilities = new NonTransitUtilities(propertyMap, sovExpUtilities,
                hovExpUtilities, nMotorExpUtilities, maasExpUtilities);

        MandatoryAccessibilitiesCalculator mandAcc = new MandatoryAccessibilitiesCalculator(
                propertyMap, ntUtilities, expConstants, logsumHelper.getBestTransitPathCalculator());

        String hhHandlerAddress = (String) propertyMap.get("RunModel.HouseholdServerAddress");
        int hhServerPort = Integer.parseInt((String) propertyMap
                .get("RunModel.HouseholdServerPort"));

        HouseholdDataManagerIf householdDataManager = new HouseholdDataManagerRmi(hhHandlerAddress,
                hhServerPort, SandagHouseholdDataManager.HH_DATA_SERVER_NAME);

        householdDataManager.setPropertyFileValues(propertyMap);

        // have the household data manager read the synthetic population
        // files and apply its tables to objects mapping method.
        boolean restartHhServer = false;
        try
        {
            // possible values for the following can be none, ao, cdap, imtf,
            // imtod, awf, awl, awtod, jtf, jtl, jtod, inmtf, inmtl, inmtod,
            // stf, stl
            String restartModel = (String) propertyMap.get("RunModel.RestartWithHhServer");
            if (restartModel.equalsIgnoreCase("none")) restartHhServer = true;
            else if (restartModel.equalsIgnoreCase("uwsl") || restartModel.equalsIgnoreCase("ao")
                    || restartModel.equalsIgnoreCase("fp") || restartModel.equalsIgnoreCase("cdap")
                    || restartModel.equalsIgnoreCase("imtf")
                    || restartModel.equalsIgnoreCase("imtod")
                    || restartModel.equalsIgnoreCase("awf") || restartModel.equalsIgnoreCase("awl")
                    || restartModel.equalsIgnoreCase("awtod")
                    || restartModel.equalsIgnoreCase("jtf") || restartModel.equalsIgnoreCase("jtl")
                    || restartModel.equalsIgnoreCase("jtod")
                    || restartModel.equalsIgnoreCase("inmtf")
                    || restartModel.equalsIgnoreCase("inmtl")
                    || restartModel.equalsIgnoreCase("inmtod")
                    || restartModel.equalsIgnoreCase("stf") || restartModel.equalsIgnoreCase("stl"))
                restartHhServer = false;
        } catch (MissingResourceException e)
        {
            restartHhServer = true;
        }

        if (restartHhServer)
        {

            householdDataManager.setDebugHhIdsFromHashmap();

            String inputHouseholdFileName = (String) propertyMap
                    .get(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_HH);
            String inputPersonFileName = (String) propertyMap
                    .get(HouseholdDataManager.PROPERTIES_SYNPOP_INPUT_PERS);
            householdDataManager.setHouseholdSampleRate(1.0f, 0);
            householdDataManager.setupHouseholdDataManager(modelStructure, inputHouseholdFileName,
                    inputPersonFileName);

        } else
        {

            householdDataManager.setHouseholdSampleRate(1.0f, 0);
            householdDataManager.setDebugHhIdsFromHashmap();
            householdDataManager.setTraceHouseholdSet();

        }

        // int id = householdDataManager.getArrayIndex( 1033380 );
        // int id = householdDataManager.getArrayIndex( 1033331 );
        int id = householdDataManager.getArrayIndex(423804);
        Household[] hh = householdDataManager.getHhArray(id, id);

        TourModeChoiceModel awmcModel = new TourModeChoiceModel(propertyMap, modelStructure,
                ModelStructure.AT_WORK_CATEGORY, dmuFactory, logsumHelper);

        SubtourDestChoiceModel testObject = new SubtourDestChoiceModel(propertyMap, modelStructure,
                aggAcc, dmuFactory, awmcModel);

        testObject.applyModel(hh[0]);

        /**
         * used this block of code to test for typos and implemented dmu methods
         * in the TOD choice UECs
         * 
         * String uecFileDirectory = propertyMap.get(
         * CtrampApplication.PROPERTIES_UEC_PATH );
         * 
         * ModelStructure modelStructure = new SandagModelStructure();
         * SandagCtrampDmuFactory dmuFactory = new
         * SandagCtrampDmuFactory(modelStructure);
         * 
         * String dcUecFileName = propertyMap.get( PROPERTIES_DC_UEC_FILE );
         * DestChoiceDMU dcDmuObject = dmuFactory.getDestChoiceDMU(); File
         * uecFile = new File(uecFileDirectory + dcUecFileName);
         * UtilityExpressionCalculator uec = new
         * UtilityExpressionCalculator(uecFile, 13, 0, propertyMap,
         * (VariableTable) dcDmuObject);
         * System.out.println("Subtour destination choice UEC passed");
         * 
         * String soaUecFileName = propertyMap.get( PROPERTIES_DC_SOA_UEC_FILE
         * ); DcSoaDMU dcSoaDmuObject = dmuFactory.getDcSoaDMU(); uecFile = new
         * File(uecFileDirectory + soaUecFileName); uec = new
         * UtilityExpressionCalculator(uecFile, 7, 0, propertyMap,
         * (VariableTable) dcSoaDmuObject);
         * System.out.println("Subtour destination choice SOA UEC passed");
         */

    }

}