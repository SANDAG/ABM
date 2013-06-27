package org.sandag.abm.ctramp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

public class WorkLocationChoiceModel implements Serializable {

    private transient Logger                     logger                           = Logger.getLogger(MandatoryDestChoiceModel.class);
    private transient Logger                     dcManLogger                      = Logger.getLogger("tourDcMan");

    
    // this constant used as a dimension for saving distance and logsums for
    // alternatives in samples
    private static final int                     MAXIMUM_SOA_ALTS_FOR_ANY_MODEL   = 200;

    private static final int                     DC_DATA_SHEET                    = 0;
    private static final int                     DC_WORK_AT_HOME_SHEET            = 1;

    //private TazDataManager                       tazs;
    private MgraDataManager                      mgraManager;
    private DestChoiceSize                       dcSizeObj;

    private DestChoiceTwoStageModelDMU           dcTwoStageDmuObject;
     
    private DestChoiceTwoStageModel              dcTwoStageModelObject;
    private TourModeChoiceModel                  mcModel;

    private String[]                             segmentNameList;
    private HashMap<Integer, Integer>            workOccupValueSegmentIndexMap;

    private int[]                                dcModelIndices;
    
    // A ChoiceModelApplication object and modeAltsAvailable[] is needed for each purpose
    private ChoiceModelApplication[]             locationChoiceModels;
    private ChoiceModelApplication               locationChoiceModel;
    private ChoiceModelApplication               worksAtHomeModel;

    private boolean[]                            dcModelAltsAvailable;
    private int[]                                dcModelAltsSample;
    private int[]                                dcModelSampleValues;
    
    private int[]                                uecSheetIndices;
    
    int                                          origMgra;

    private int                                  modelIndex;

    private double[]                             sampleAlternativeDistances;
    private double[]                             sampleAlternativeLogsums;

    private BuildAccessibilities                 aggAcc;

    private double[] mgraDistanceArray;
    
    private int soaSampleSize;

    private long soaRunTime;
    
    
    
    public WorkLocationChoiceModel( int index, HashMap<String, String> propertyMap,
            DestChoiceSize dcSizeObj, BuildAccessibilities aggAcc,
            String dcUecFileName, String soaUecFile, int soaSampleSize, String modeChoiceUecFile,
            CtrampDmuFactoryIf dmuFactory, TourModeChoiceModel mcModel,
            double[][][] workSizeProbs, double[][][] workTazDistProbs )
    {

        this.aggAcc = aggAcc;
        this.dcSizeObj = dcSizeObj;
        this.mcModel = mcModel;
        this.soaSampleSize = soaSampleSize;
        
        modelIndex = index;

        
        mgraManager = MgraDataManager.getInstance();

        dcTwoStageDmuObject = dmuFactory.getDestChoiceSoaTwoStageDMU();
        dcTwoStageDmuObject.setAggAcc( this.aggAcc );

        dcTwoStageModelObject = new DestChoiceTwoStageModel( propertyMap, soaSampleSize );
        dcTwoStageModelObject.setTazDistProbs( workTazDistProbs );        
        dcTwoStageModelObject.setMgraSizeProbs( workSizeProbs );        
        
        
        sampleAlternativeDistances = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];
        sampleAlternativeLogsums = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];

        workOccupValueSegmentIndexMap = aggAcc.getWorkOccupValueIndexMap();

    }

    public void setupWorkSegments(int[] myUecSheetIndices, int[] mySoaUecSheetIndices)
    {
        uecSheetIndices = myUecSheetIndices;
        segmentNameList = aggAcc.getWorkSegmentNameList();
    }
    
    public void setupDestChoiceModelArrays(HashMap<String, String> propertyMap,
            String dcUecFileName, String soaUecFile, int soaSampleSize)
    {

        // create the works-at-home ChoiceModelApplication object
        worksAtHomeModel = new ChoiceModelApplication(dcUecFileName, DC_WORK_AT_HOME_SHEET,
                DC_DATA_SHEET, propertyMap, (VariableTable)dcTwoStageDmuObject );


        // create a lookup array to map purpose index to model index
        dcModelIndices = new int[uecSheetIndices.length];

        // get a set of unique model sheet numbers so that we can create ChoiceModelApplication objects once for each model sheet used
        // also create a HashMap to relate size segment index to SOA Model objects 
        HashMap<Integer,Integer> modelIndexMap = new HashMap<Integer,Integer>();
        int dcModelIndex = 0;
        int dcSegmentIndex = 0;
        for ( int uecIndex : uecSheetIndices ) {
            // if the uec sheet for the model segment is not in the map, add it, otherwise, get it from the map
            if ( ! modelIndexMap.containsKey(uecIndex) ) {
                modelIndexMap.put( uecIndex, dcModelIndex );
                dcModelIndices[dcSegmentIndex] = dcModelIndex++;
            }
            else {
                dcModelIndices[dcSegmentIndex] = modelIndexMap.get( uecIndex );
            }
            
            dcSegmentIndex++;
        }
        // the value of dcModelIndex is the number of ChoiceModelApplication objects to create
        // the modelIndexMap keys are the uec sheets to use in building ChoiceModelApplication objects 
        
        
        locationChoiceModels = new ChoiceModelApplication[modelIndexMap.size()];

        int i = 0;
        for (int uecIndex : modelIndexMap.keySet() )
        {

            int modelIndex = -1;
            try
            {
                modelIndex = modelIndexMap.get(uecIndex); 
                locationChoiceModels[modelIndex] = new ChoiceModelApplication(dcUecFileName, uecIndex,
                        DC_DATA_SHEET, propertyMap, (VariableTable)dcTwoStageDmuObject);
            } catch (RuntimeException e)
            {
                logger.error( String.format("exception caught setting up DC ChoiceModelApplication[%d] for modelIndex=%d of %d models", i, modelIndex, modelIndexMap.size()) );
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


    public void applyWorkLocationChoice(Household hh)
    {

        if (hh.getDebugChoiceModels())
        {
            String label = String.format("Pre Work Location Choice HHId=%d Object", hh.getHhId());
            hh.logHouseholdObject(label, dcManLogger);
        }

        // declare these variables here so their values can be logged if a
        // RuntimeException occurs.
        int i = -1;
        int occupSegmentIndex = -1;
        int occup = -1;
        String occupSegmentName = "";

        int homeMgra = hh.getHhMgra();
        Person[] persons = hh.getPersons();

        int tourNum = 0;
        for (i = 1; i < persons.length; i++)
        {

            Person p = persons[i];

            // skip person if they are not a worker
            if (p.getPersonIsWorker() != 1)
            {
                p.setWorkLocationSegmentIndex(-1);
                p.setWorkLoc(0);
                p.setWorkLocDistance(0);
                p.setWorkLocLogsum(-999);
                continue;
            }

            // skip person if their work at home choice was work in the home
            // (alternative 2 in choice model)
            int worksAtHomeChoice = selectWorksAtHomeChoice(dcTwoStageDmuObject, hh, p);
            if (worksAtHomeChoice == ModelStructure.WORKS_AT_HOME_ALTERNATUVE_INDEX)
            {
                p.setWorkLocationSegmentIndex(ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR);
                p.setWorkLoc(ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR);
                p.setWorkLocDistance(0);
                p.setWorkLocLogsum(-999);
                continue;
            }

            // save person information in decision maker label, and log person object
            if (hh.getDebugChoiceModels())
            {
                String decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", p
                        .getHouseholdObject().getHhId(), p.getPersonNum(), p.getPersonType());
                hh.logPersonObject(decisionMakerLabel, dcManLogger, p);
            }

            double[] results = null;
            int modelIndex = 0;
            try
            {

                origMgra = homeMgra;

                occup = p.getPersPecasOccup();
                occupSegmentIndex = workOccupValueSegmentIndexMap.get(occup);
                occupSegmentName = segmentNameList[occupSegmentIndex];

                p.setWorkLocationSegmentIndex(occupSegmentIndex);

                // update the DC dmuObject for this person
                dcTwoStageDmuObject.setHouseholdObject(hh);
                dcTwoStageDmuObject.setPersonObject(p);
                dcTwoStageDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra, 0);

                double[] homeMgraSizeArray = dcSizeObj.getDcSizeArray()[occupSegmentIndex];
                mcModel.getAnmSkimCalculator().getAmPkSkimDistancesFromMgra( homeMgra, mgraDistanceArray );
                
                // set size array for the tour segment and distance array from the home mgra to all destinaion mgras.
                dcTwoStageDmuObject.setMgraSizeArray(homeMgraSizeArray);
                dcTwoStageDmuObject.setMgraDistanceArray( mgraDistanceArray );

                
                modelIndex = dcModelIndices[occupSegmentIndex];
                locationChoiceModel = locationChoiceModels[modelIndex];

                // get the work location alternative chosen from the sample
                results = selectLocationFromSampleOfAlternatives("work", -1, p, occupSegmentName, occupSegmentIndex, tourNum++,
                        homeMgraSizeArray, mgraDistanceArray );

                soaRunTime += dcTwoStageModelObject.getSoaRunTime();

            } catch (RuntimeException e)
            {
                logger.fatal(String.format("Exception caught in dcModel selecting location for i=%d, hh.hhid=%d, person i=%d, in work location choice, modelIndex=%d, occup=%d, segmentIndex=%d, segmentName=%s",
                    i, hh.getHhId(), i, modelIndex, occup, occupSegmentIndex, occupSegmentName));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

            p.setWorkLoc((int) results[0]);
            p.setWorkLocDistance((float) results[1]);
            p.setWorkLocLogsum((float) results[2]);

        }

    }


    /**
     * 
     * @return an array with chosen mgra, distance to chosen mgra, and logsum to
     *         chosen mgra.
     */
    private double[] selectLocationFromSampleOfAlternatives(String segmentType, int segmentTypeIndex,
            Person person, String segmentName, int sizeSegmentIndex, int tourNum,
            double[] homeMgraSizeArray, double[] homeMgraDistanceArray)
    {

        // set tour origin taz/subzone and start/end times for calculating mode
        // choice logsum
        Logger modelLogger = dcManLogger;

        Household household = person.getHouseholdObject();


        // get sample of locations and correction factors for sample using the alternate method
        // for work location, the sizeSegmentType INdex and sizeSegmentIndex are the same values.
        dcTwoStageModelObject.chooseSample( household.getHhTaz(), sizeSegmentIndex, sizeSegmentIndex, soaSampleSize, household.getHhRandom(), household.getDebugChoiceModels() );
        int[] finalSample = dcTwoStageModelObject.getUniqueSampleMgras();
        double[] sampleCorrectionFactors = dcTwoStageModelObject.getUniqueSampleMgraCorrectionFactors();
        int numUniqueAlts = dcTwoStageModelObject.getNumberofUniqueMgrasInSample();


        Arrays.fill( dcModelAltsAvailable, false );
        Arrays.fill( dcModelAltsSample, 0 );
        Arrays.fill( dcModelSampleValues, 999999 );


        // set sample of alternatives correction factors used in destination
        // choice utility for the sampled alternatives.
        dcTwoStageDmuObject.setDcSoaCorrections( sampleCorrectionFactors );


        // for the destination mgras in the sample, compute mc logsums and save in dmuObject.
        // also save correction factor and set availability and sample value for the
        // sample alternative to true. 1, respectively.
        for (int i = 0; i < numUniqueAlts; i++)
        {

            int destMgra = finalSample[i];
            dcModelSampleValues[i] = finalSample[i];
            
            // set logsum value in DC dmuObject for the logsum index, sampled zone and subzone.
            double logsum = getModeChoiceLogsum( household, person, destMgra, segmentTypeIndex );
            dcTwoStageDmuObject.setMcLogsum(i, logsum);
            
            sampleAlternativeLogsums[i] = logsum;
            sampleAlternativeDistances[i] = homeMgraDistanceArray[finalSample[i]];
            
            // set availaibility and sample values for the purpose, dcAlt.
            dcModelAltsAvailable[i+1] = true;
            dcModelAltsSample[i+1] = 1;

        }
        
        dcTwoStageDmuObject.setSampleArray( dcModelSampleValues );



        // log headers to traceLogger if the person making the destination choice is
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
                    person.getHouseholdObject().getHhId(), person.getPersonNum(), person.getPersonType(), tourNum);

            modelLogger.info(" ");
            modelLogger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info("Usual " + segmentType + " Location Choice Model for: Segment="
                    + segmentName + ", Person Num: " + person.getPersonNum() + ", Person Type: "
                    + person.getPersonType() + ", TourNum=" + tourNum);

            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            locationChoiceModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        // compute destination choice proportions and choose alternative
        locationChoiceModel.computeUtilities( dcTwoStageDmuObject, dcTwoStageDmuObject.getDmuIndexValues(), dcModelAltsAvailable, dcModelAltsSample );

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
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
            logger.error(String.format(
                "Exception caught for HHID=%d, PersonNum=%d, no available %s destination choice alternatives to choose from in choiceModelApplication.",
                dcTwoStageDmuObject.getHouseholdObject().getHhId(), dcTwoStageDmuObject.getPersonObject().getPersonNum(), segmentName));
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
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------- --------------    --------------    --------------    --------------");


            double cumProb = 0.0;
            for (int j = 0; j < finalSample.length; j++)
            {
                int alt = finalSample[j];
                cumProb += probabilities[j];
                String altString = String.format("j=%d, mgra=%d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e",
                        altString, availabilities[j+1], utilities[j], probabilities[j], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("j=%d, mgra=%d", chosen-1, finalSample[chosen-1]);
            modelLogger.info(String.format(
                    "Choice: %s with rn=%.8f, randomCount=%d", altString, rn, randomCount));

            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info(" ");

            locationChoiceModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            locationChoiceModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            locationChoiceModel.logUECResults(modelLogger, loggingHeader);

            if (chosen < 0)
            {
                logger.error(String.format(
                    "Exception caught for HHID=%d, PersonNum=%d, workSegment=%d, no available %s destination choice alternatives to choose from in ChoiceModelApplication.",
                    dcTwoStageDmuObject.getHouseholdObject().getHhId(), dcTwoStageDmuObject.getPersonObject().getPersonNum(), segmentName));
                throw new RuntimeException();
            }

        }

        double[] returnArray = new double[3];

        returnArray[0] = finalSample[chosen-1];
        returnArray[1] = sampleAlternativeDistances[chosen-1];
        returnArray[2] = sampleAlternativeLogsums[chosen-1];

        return returnArray;

    }


    
    private int selectWorksAtHomeChoice(DestChoiceTwoStageModelDMU dcTwoStageDmuObject, Household household, Person person)
    {

        // set tour origin taz/subzone and start/end times for calculating mode
        // choice logsum
        Logger modelLogger = dcManLogger;

        dcTwoStageDmuObject.setHouseholdObject(household);
        dcTwoStageDmuObject.setPersonObject(person);
        dcTwoStageDmuObject.setDmuIndexValues(household.getHhId(), household.getHhMgra(), origMgra, 0);

        double accessibility = aggAcc.getAccessibilitiesTableObject().getAggregateAccessibility("totEmp", household.getHhMgra());
        dcTwoStageDmuObject.setWorkAccessibility(accessibility);

        // log headers to traceLogger if the person making the destination choice is
        // from a household requesting trace information
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        if (household.getDebugChoiceModels())
        {

            // null tour means the DC is a mandatory usual location choice
            choiceModelDescription = String.format("Usual Work Location Is At Home Choice Model");
            decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", person
                    .getHouseholdObject().getHhId(), person.getPersonNum(), person.getPersonType());

            modelLogger.info(" ");
            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info("Usual Work Location Is At Home Choice Model: Person Num: "
                    + person.getPersonNum() + ", Person Type: " + person.getPersonType());

            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            worksAtHomeModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        // compute destination choice proportions and choose alternative
        worksAtHomeModel.computeUtilities(dcTwoStageDmuObject, dcTwoStageDmuObject.getDmuIndexValues());

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
        int chosen = -1;
        if (worksAtHomeModel.getAvailabilityCount() > 0)
        {
            chosen = worksAtHomeModel.getChoiceResult(rn);
        }

        // write choice model alternative info to log file
        if (household.getDebugChoiceModels() || chosen < 0)
        {

            double[] utilities = worksAtHomeModel.getUtilities();
            double[] probabilities = worksAtHomeModel.getProbabilities();
            boolean[] availabilities = worksAtHomeModel.getAvailabilities();

            String[] altNames = worksAtHomeModel.getAlternativeNames();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------- --------------    --------------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 0; j < utilities.length; j++)
            {
                cumProb += probabilities[j];
                String altString = String.format("%d, %s", j + 1, altNames[j]);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j + 1], utilities[j], probabilities[j], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("j=%d, alt=%s", chosen,
                    (chosen < 0 ? "N/A, no available alternatives" : altNames[chosen - 1]));
            modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

            modelLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            modelLogger.info(" ");

            worksAtHomeModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            worksAtHomeModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn,
                    chosen);

            // write UEC calculation results to separate model specific log file
            worksAtHomeModel.logUECResults(modelLogger, loggingHeader);

        }

        if (chosen < 0)
        {
            logger.error(String.format(
                "Exception caught for HHID=%d, PersonNum=%d, no available works at home alternatives to choose from in choiceModelApplication.",
                dcTwoStageDmuObject.getHouseholdObject().getHhId(), dcTwoStageDmuObject.getPersonObject().getPersonNum()) );
            throw new RuntimeException();
        }

        return chosen;

    }
    
    private double getModeChoiceLogsum( Household household, Person person, int sampleDestMgra, int segmentTypeIndex ) {
 
        int purposeIndex = 0;
        String purpose = "";
        if ( segmentTypeIndex < 0 ) {
            purposeIndex = ModelStructure.WORK_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.WORK_PRIMARY_PURPOSE_NAME;
        }
        else if ( segmentTypeIndex == BuildAccessibilities.PRESCHOOL_ALT_INDEX ) {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        }
        else if ( segmentTypeIndex == BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX ) {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        }
        else if ( segmentTypeIndex == BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX ) {
            purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
        }
        else if ( segmentTypeIndex == BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX ) {
            purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
        }
        else if ( segmentTypeIndex == BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX ) {
            purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
            purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
        }

        // create a temporary tour to use to calculate mode choice logsum
        Tour mcLogsumTour = new Tour( person, 0, purposeIndex );
        mcLogsumTour.setTourPurpose(purpose);
        mcLogsumTour.setTourOrigMgra( household.getHhMgra() );
        mcLogsumTour.setTourDestMgra( sampleDestMgra );
        mcLogsumTour.setTourDepartPeriod(Person.DEFAULT_MANDATORY_START_PERIOD);
        mcLogsumTour.setTourArrivePeriod(Person.DEFAULT_MANDATORY_END_PERIOD);
        
        
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        
        if (household.getDebugChoiceModels()) {
            dcManLogger.info("");
            dcManLogger.info("");
            choiceModelDescription = "location choice logsum for segmentTypeIndex=" + segmentTypeIndex + ", temp tour PurposeIndex=" + purposeIndex;
            decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: " + person.getPersonNum();
            household.logPersonObject( choiceModelDescription + ", " + decisionMakerLabel, dcManLogger, person );
        }

        double logsum = -1;
        try {
            logsum = mcModel.getModeChoiceLogsum ( household, person, mcLogsumTour, dcManLogger, choiceModelDescription, decisionMakerLabel );
        }
        catch(Exception e) {
            choiceModelDescription = "location choice logsum for segmentTypeIndex=" + segmentTypeIndex + ", temp tour PurposeIndex=" + purposeIndex;
            decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: " + person.getPersonNum();
            logger.fatal( "exception caught calculating ModeChoiceLogsum for usual work/school location choice." );
            logger.fatal( "choiceModelDescription = " + choiceModelDescription );
            logger.fatal( "decisionMakerLabel = " + decisionMakerLabel );
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
    
    public long getSoaRunTime() {
        return soaRunTime;
    }
    
    public void resetSoaRunTime() {
        soaRunTime = 0;
    }
            
}