package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class SchoolLocationChoiceModel
        implements Serializable
{

    private transient Logger           logger                         = Logger.getLogger(SchoolLocationChoiceModel.class);
    private transient Logger           dcManLogger                    = Logger.getLogger("tourDcMan");

    // this constant used as a dimension for saving distance and logsums for
    // alternatives in samples
    private static final int           MAXIMUM_SOA_ALTS_FOR_ANY_MODEL = 200;

    private static final int           DC_DATA_SHEET                  = 0;

    private MgraDataManager            mgraManager;
    private DestChoiceSize             dcSizeObj;

    private DestChoiceTwoStageModelDMU dcTwoStageDmuObject;

    private DestChoiceTwoStageModel    dcTwoStageModelObject;
    private TourModeChoiceModel        mcModel;

    private String[]                   segmentNameList;
    private HashMap<String, Integer>   segmentNameIndexMap;

    private int[]                      dcModelIndices;

    // A ChoiceModelApplication object and modeAltsAvailable[] is needed for
    // each purpose
    private ChoiceModelApplication[]   locationChoiceModels;
    private ChoiceModelApplication     locationChoiceModel;

    private boolean[]                  dcModelAltsAvailable;
    private int[]                      dcModelAltsSample;
    private int[]                      dcModelSampleValues;

    private int[]                      uecSheetIndices;

    int                                origMgra;

    private int                        modelIndex;
    private int                        shadowPricingIteration;

    private double[]                   sampleAlternativeDistances;
    private double[]                   sampleAlternativeLogsums;

    private double[]                   mgraDistanceArray;

    private BuildAccessibilities       aggAcc;

    private int                        soaSampleSize;

    private long                       soaRunTime;

    public SchoolLocationChoiceModel(int index, HashMap<String, String> propertyMap,
            DestChoiceSize dcSizeObj, BuildAccessibilities aggAcc, String dcUecFileName,
            String soaUecFile, int soaSampleSize, String modeChoiceUecFile,
            CtrampDmuFactoryIf dmuFactory, TourModeChoiceModel mcModel,
            double[][][] schoolSizeProbs, double[][][] schoolTazDistProbs)
    {

        this.aggAcc = aggAcc;
        this.dcSizeObj = dcSizeObj;
        this.mcModel = mcModel;
        this.soaSampleSize = soaSampleSize;

        modelIndex = index;

        mgraManager = MgraDataManager.getInstance();

        dcTwoStageDmuObject = dmuFactory.getDestChoiceSoaTwoStageDMU();
        dcTwoStageDmuObject.setAggAcc(this.aggAcc);

        dcTwoStageModelObject = new DestChoiceTwoStageModel(propertyMap, soaSampleSize);
        dcTwoStageModelObject.setTazDistProbs(schoolTazDistProbs);
        dcTwoStageModelObject.setMgraSizeProbs(schoolSizeProbs);

        shadowPricingIteration = 0;

        sampleAlternativeDistances = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];
        sampleAlternativeLogsums = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];

    }

    public void setupSchoolSegments()
    {
        aggAcc.createSchoolSegmentNameIndices();
        uecSheetIndices = aggAcc.getSchoolDcUecSheets();
        segmentNameList = aggAcc.getSchoolSegmentNameList();
    }

    public void setupDestChoiceModelArrays(HashMap<String, String> propertyMap,
            String dcUecFileName, String soaUecFile, int soaSampleSize)
    {

        segmentNameIndexMap = dcSizeObj.getSegmentNameIndexMap();

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
        // the value of dcModelIndex is the number of ChoiceModelApplication
        // objects to create
        // the modelIndexMap keys are the uec sheets to use in building
        // ChoiceModelApplication objects

        locationChoiceModels = new ChoiceModelApplication[modelIndexMap.size()];

        int i = 0;
        for (int uecIndex : modelIndexMap.keySet())
        {

            int modelIndex = -1;
            try
            {
                modelIndex = modelIndexMap.get(uecIndex);
                locationChoiceModels[modelIndex] = new ChoiceModelApplication(dcUecFileName,
                        uecIndex, DC_DATA_SHEET, propertyMap, (VariableTable) dcTwoStageDmuObject);
            } catch (RuntimeException e)
            {
                logger.error(String
                        .format("exception caught setting up DC ChoiceModelApplication[%d] for modelIndex=%d of %d models",
                                i, modelIndex, modelIndexMap.size()));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

        }

        dcModelAltsAvailable = new boolean[soaSampleSize + 1];
        dcModelAltsSample = new int[soaSampleSize + 1];
        dcModelSampleValues = new int[soaSampleSize];

        mgraDistanceArray = new double[mgraManager.getMaxMgra() + 1];

    }

    public void applySchoolLocationChoice(Household hh)
    {

        if (hh.getDebugChoiceModels())
        {
            String label = String.format("Pre school Location Choice HHId=%d Object", hh.getHhId());
            hh.logHouseholdObject(label, dcManLogger);
        }

        // declare these variables here so their values can be logged if a
        // RuntimeException occurs.
        int i = -1;

        int homeMgra = hh.getHhMgra();
        Person[] persons = hh.getPersons();

        int tourNum = 0;
        for (i = 1; i < persons.length; i++)
        {

            Person p = persons[i];

            int segmentIndex = -1;
            int segmentType = -1;
            if (p.getPersonIsPreschoolChild() == 1 || p.getPersonIsStudentNonDriving() == 1
                    || p.getPersonIsStudentDriving() == 1 || p.getPersonIsUniversityStudent() == 1)
            {

                if (p.getPersonIsPreschoolChild() == 1)
                {
                    segmentIndex = segmentNameIndexMap
                            .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.PRESCHOOL_SEGMENT_GROUP_INDEX]);
                    segmentType = BuildAccessibilities.PRESCHOOL_ALT_INDEX;
                } else if (p.getPersonIsGradeSchool() == 1)
                {
                    segmentIndex = aggAcc.getMgraGradeSchoolSegmentIndex(homeMgra);
                    segmentType = BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX;
                } else if (p.getPersonIsHighSchool() == 1)
                {
                    segmentIndex = aggAcc.getMgraHighSchoolSegmentIndex(homeMgra);
                    segmentType = BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX;
                } else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() < 30)
                {
                    segmentIndex = segmentNameIndexMap
                            .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_TYPICAL_SEGMENT_GROUP_INDEX]);
                    segmentType = BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX;
                } else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() >= 30)
                {
                    segmentIndex = segmentNameIndexMap
                            .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX]);
                    segmentType = BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX;
                }

                // if person type is a student but segment index is -1, the
                // person is not enrolled
                // assume home schooled
                if (segmentIndex < 0)
                {
                    p.setSchoolLocationSegmentIndex(ModelStructure.NOT_ENROLLED_SEGMENT_INDEX);
                    p.setSchoolLoc(ModelStructure.NOT_ENROLLED_SEGMENT_INDEX);
                    p.setSchoolLocDistance(0);
                    p.setSchoolLocLogsum(-999);
                    continue;
                } else
                {
                    // if the segment is in the skip shadow pricing set, and the
                    // iteration is > 0, dont' compute new choice
                    if (shadowPricingIteration == 0
                            || !dcSizeObj.getSegmentIsInSkipSegmentSet(segmentIndex))
                        p.setSchoolLocationSegmentIndex(segmentIndex);
                }

                if (segmentType < 0)
                {
                    segmentType = ModelStructure.NOT_ENROLLED_SEGMENT_INDEX;
                }
            } else // not a student person type
            {
                p.setSchoolLocationSegmentIndex(-1);
                p.setSchoolLoc(0);
                p.setSchoolLocDistance(0);
                p.setSchoolLocLogsum(-999);
                continue;
            }

            // save person information in decision maker label, and log person
            // object
            if (hh.getDebugChoiceModels())
            {
                String decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", p
                        .getHouseholdObject().getHhId(), p.getPersonNum(), p.getPersonType());
                hh.logPersonObject(decisionMakerLabel, dcManLogger, p);
            }

            // if the segment is in the skip shadow pricing set, and the
            // iteration is > 0, dont' compute new choice
            if (shadowPricingIteration > 0 && dcSizeObj.getSegmentIsInSkipSegmentSet(segmentIndex))
                continue;

            double[] results = null;
            int modelIndex = 0;
            try
            {

                origMgra = homeMgra;

                // update the DC dmuObject for this person
                dcTwoStageDmuObject.setHouseholdObject(hh);
                dcTwoStageDmuObject.setPersonObject(p);
                dcTwoStageDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra, 0);

                double[] homeMgraSizeArray = dcSizeObj.getDcSizeArray()[segmentIndex];
                mcModel.getAnmSkimCalculator().getAmPkSkimDistancesFromMgra(homeMgra,
                        mgraDistanceArray);

                // set size array for the tour segment and distance array from
                // the home mgra to all destinaion mgras.
                dcTwoStageDmuObject.setMgraSizeArray(homeMgraSizeArray);
                dcTwoStageDmuObject.setMgraDistanceArray(mgraDistanceArray);

                modelIndex = dcModelIndices[segmentIndex];
                locationChoiceModel = locationChoiceModels[modelIndex];

                // get the school location alternative chosen from the sample
                results = selectLocationFromSampleOfAlternatives("school", segmentType, p,
                        segmentNameList[segmentIndex], segmentIndex, tourNum++, homeMgraSizeArray,
                        mgraDistanceArray);

            } catch (RuntimeException e)
            {
                logger.fatal(String
                        .format("Exception caught in dcModel selecting location for i=%d, hh.hhid=%d, person i=%d, in school location choice, modelIndex=%d, segmentType=%d, segmentIndex=%d, segmentName=%s",
                                i, hh.getHhId(), i, modelIndex, segmentType, segmentIndex,
                                segmentNameList[segmentIndex]));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

            p.setSchoolLoc((int) results[0]);
            p.setSchoolLocDistance((float) results[1]);
            p.setSchoolLocLogsum((float) results[2]);

        }

    }

    /**
     * 
     * @return an array with chosen mgra, distance to chosen mgra, and logsum to
     *         chosen mgra.
     */
    private double[] selectLocationFromSampleOfAlternatives(String segmentType,
            int segmentTypeIndex, Person person, String segmentName, int sizeSegmentIndex,
            int tourNum, double[] homeMgraSizeArray, double[] homeMgraDistanceArray)
    {

        // set tour origin taz/subzone and start/end times for calculating mode
        // choice logsum
        Logger modelLogger = dcManLogger;

        Household household = person.getHouseholdObject();

        // get sample of locations and correction factors for sample using the
        // alternate method
        dcTwoStageModelObject.chooseSample(household.getHhTaz(), sizeSegmentIndex,
                segmentTypeIndex, soaSampleSize, household.getHhRandom(),
                household.getDebugChoiceModels());
        int[] finalSample = dcTwoStageModelObject.getUniqueSampleMgras();
        double[] sampleCorrectionFactors = dcTwoStageModelObject
                .getUniqueSampleMgraCorrectionFactors();
        int numUniqueAlts = dcTwoStageModelObject.getNumberofUniqueMgrasInSample();

        Arrays.fill(dcModelAltsAvailable, false);
        Arrays.fill(dcModelAltsSample, 0);
        Arrays.fill(dcModelSampleValues, 999999);

        // set sample of alternatives correction factors used in destination
        // choice utility for the sampled alternatives.
        dcTwoStageDmuObject.setDcSoaCorrections(sampleCorrectionFactors);

        // for the destination mgras in the sample, compute mc logsums and save
        // in dmuObject.
        // also save correction factor and set availability and sample value for
        // the
        // sample alternative to true. 1, respectively.
        for (int i = 0; i < numUniqueAlts; i++)
        {

            int destMgra = finalSample[i];
            dcModelSampleValues[i] = finalSample[i];

            // set logsum value in DC dmuObject for the logsum index, sampled
            // zone and subzone.
            double logsum = getModeChoiceLogsum(household, person, destMgra, segmentTypeIndex);
            dcTwoStageDmuObject.setMcLogsum(i, logsum);

            sampleAlternativeLogsums[i] = logsum;
            sampleAlternativeDistances[i] = homeMgraDistanceArray[finalSample[i]];

            // set availaibility and sample values for the purpose, dcAlt.
            dcModelAltsAvailable[i + 1] = true;
            dcModelAltsSample[i + 1] = 1;

        }

        dcTwoStageDmuObject.setSampleArray(dcModelSampleValues);

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
                    "Usual %s Location Choice Model for: Segment=%s", segmentType, segmentName);
            decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s, TourNum=%d",
                    person.getHouseholdObject().getHhId(), person.getPersonNum(),
                    person.getPersonType(), tourNum);

            modelLogger.info(" ");
            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info("Usual " + segmentType + " Location Choice Model for: Segment="
                    + segmentName + ", Person Num: " + person.getPersonNum() + ", Person Type: "
                    + person.getPersonType() + ", TourNum=" + tourNum);

            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            locationChoiceModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        // compute destination choice proportions and choose alternative
        locationChoiceModel.computeUtilities(dcTwoStageDmuObject,
                dcTwoStageDmuObject.getDmuIndexValues(), dcModelAltsAvailable, dcModelAltsSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        if (locationChoiceModel.getAvailabilityCount() > 0)
        {
            try
            {
                chosen = locationChoiceModel.getChoiceResult(rn);
            } catch (Exception e)
            {
            }
        } else
        {
            logger.error(String
                    .format("Exception caught for HHID=%d, PersonNum=%d, no available %s destination choice alternatives to choose from in choiceModelApplication.",
                            dcTwoStageDmuObject.getHouseholdObject().getHhId(), dcTwoStageDmuObject
                                    .getPersonObject().getPersonNum(), segmentName));
        }

        if (household.getDebugChoiceModels() || chosen <= 0)
        {

            double[] utilities = locationChoiceModel.getUtilities();
            double[] probabilities = locationChoiceModel.getProbabilities();
            boolean[] availabilities = locationChoiceModel.getAvailabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb          Distance            Logsum");
            modelLogger
                    .info("--------------------- --------------    --------------    --------------    --------------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 1; j < finalSample.length; j++)
            {
                int alt = finalSample[j];
                cumProb += probabilities[j];
                String altString = String.format("j=%d, mgra=%d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j + 1], utilities[j], probabilities[j], cumProb));
            }

            modelLogger.info(" ");
            if (chosen > 0)
            {
                String altString = String.format("j=%d, mgra=%d", chosen - 1,
                        finalSample[chosen - 1]);
                modelLogger.info(String.format("Choice: %s with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));
            } else
            {
                String altString = String.format("No Chosen Alt, availability count = %d",
                        locationChoiceModel.getAvailabilityCount());
                modelLogger.info(altString);
            }
            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info(" ");

            locationChoiceModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            locationChoiceModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn,
                    chosen);

            // write UEC calculation results to separate model specific log file
            locationChoiceModel.logUECResults(modelLogger, loggingHeader);

            if (chosen < 0)
            {
                logger.error(String
                        .format("Exception caught for HHID=%d, PersonNum=%d, no available %s destination choice alternatives to choose from in choiceModelApplication.",
                                dcTwoStageDmuObject.getHouseholdObject().getHhId(),
                                dcTwoStageDmuObject.getPersonObject().getPersonNum(), segmentName));
                System.exit(-1);
            }

        }

        double[] returnArray = new double[3];

        returnArray[0] = finalSample[chosen - 1];
        returnArray[1] = sampleAlternativeDistances[chosen - 1];
        returnArray[2] = sampleAlternativeLogsums[chosen - 1];

        return returnArray;

    }

    private double getModeChoiceLogsum(Household household, Person person, int sampleDestMgra,
            int segmentTypeIndex)
    {

        int purposeIndex = 0;
        String purpose = "";
        if (segmentTypeIndex < 0)
        {
            purposeIndex = ModelStructure.WORK_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.WORK_PRIMARY_PURPOSE_NAME;
        } else if (segmentTypeIndex == BuildAccessibilities.PRESCHOOL_ALT_INDEX)
        {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        } else if (segmentTypeIndex == BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX)
        {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        } else if (segmentTypeIndex == BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX)
        {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        } else if (segmentTypeIndex == BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX)
        {
            purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
        } else if (segmentTypeIndex == BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX)
        {
            purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
        }

        // create a temporary tour to use to calculate mode choice logsum
        Tour mcLogsumTour = new Tour(person, 0, purposeIndex);
        mcLogsumTour.setTourPurpose(purpose);
        mcLogsumTour.setTourOrigMgra(household.getHhMgra());
        mcLogsumTour.setTourDestMgra(sampleDestMgra);
        mcLogsumTour.setTourDepartPeriod(Person.DEFAULT_MANDATORY_START_PERIOD);
        mcLogsumTour.setTourArrivePeriod(Person.DEFAULT_MANDATORY_END_PERIOD);

        String choiceModelDescription = "";
        String decisionMakerLabel = "";

        if (household.getDebugChoiceModels())
        {
            dcManLogger.info("");
            dcManLogger.info("");
            choiceModelDescription = "location choice logsum for segmentTypeIndex="
                    + segmentTypeIndex + ", temp tour PurposeIndex=" + purposeIndex;
            decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: "
                    + person.getPersonNum();
            household.logPersonObject(choiceModelDescription + ", " + decisionMakerLabel,
                    dcManLogger, person);
        }

        double logsum = -1;
        try
        {
            logsum = mcModel.getModeChoiceLogsum(household, person, mcLogsumTour, dcManLogger,
                    choiceModelDescription, decisionMakerLabel);
        } catch (Exception e)
        {
            choiceModelDescription = "location choice logsum for segmentTypeIndex="
                    + segmentTypeIndex + ", temp tour PurposeIndex=" + purposeIndex;
            decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: "
                    + person.getPersonNum();
            logger.fatal("exception caught calculating ModeChoiceLogsum for usual work/school location choice.");
            logger.fatal("choiceModelDescription = " + choiceModelDescription);
            logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
            e.printStackTrace();
            System.exit(-1);
        }

        return logsum;
    }

    public int getModelIndex()
    {
        return modelIndex;
    }

    public void setDcSizeObject(DestChoiceSize dcSizeObj)
    {
        this.dcSizeObj = dcSizeObj;
    }

    public long getSoaRunTime()
    {
        return soaRunTime;
    }

    public void resetSoaRunTime()
    {
        soaRunTime = 0;
    }

}