package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;






import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;

public class TourModeChoiceModel
        implements Serializable
{

    private transient Logger         logger                                 = Logger.getLogger(TourModeChoiceModel.class);
    private transient Logger         tourMCManLogger                        = Logger.getLogger("tourMcMan");
    private transient Logger         tourMCNonManLogger                     = Logger.getLogger("tourMcNonMan");

    public static final String       MANDATORY_MODEL_INDICATOR              = ModelStructure.MANDATORY_CATEGORY;
    public static final String       NON_MANDATORY_MODEL_INDICATOR          = "Non-Mandatory";
    public static final String       AT_WORK_SUBTOUR_MODEL_INDICATOR        = ModelStructure.AT_WORK_CATEGORY;

    public static final boolean      DEBUG_BEST_PATHS                       = true;

    protected static final int       OUT                                    = McLogsumsCalculator.OUT;
    protected static final int       IN                                     = McLogsumsCalculator.IN;
    protected static final int       NUM_DIR                                = McLogsumsCalculator.NUM_DIR;

    private static final int         MC_DATA_SHEET                          = 0;
    private static final String      PROPERTIES_UEC_TOUR_MODE_CHOICE        = "tourModeChoice.uec.file";
    private static final String      PROPERTIES_UEC_MAINT_TOUR_MODE_SHEET   = "tourModeChoice.maint.model.page";
    private static final String      PROPERTIES_UEC_DISCR_TOUR_MODE_SHEET   = "tourModeChoice.discr.model.page";
    private static final String      PROPERTIES_UEC_AT_WORK_TOUR_MODE_SHEET = "tourModeChoice.atwork.model.page";
    
    
    private static final String       PROPERTIES_TOUR_UTILITY_IVT_COEFFS          = "tour.utility.ivt.coeffs";
    private static final String       PROPERTIES_TOUR_UTILITY_INCOME_COEFFS       = "tour.utility.income.coeffs"; 
    private static final String       PROPERTIES_TOUR_UTILITY_INCOME_EXPONENTS    = "tour.utility.income.exponents";

    // A MyChoiceModelApplication object and modeAltsAvailable[] is needed for
    // each purpose
    private ChoiceModelApplication[] mcModel;
    private TourModeChoiceDMU        mcDmuObject;
    private McLogsumsCalculator      logsumHelper;

    private ModelStructure           modelStructure;

    private String                   tourCategory;
    private String[]                 tourPurposeList;

    private HashMap<String, Integer> purposeModelIndexMap;

    private String[][]               modeAltNames;

    private boolean                  saveUtilsProbsFlag                     = false;
    
    // following arrays used to store ivt coefficients, and income coefficients, income exponents to calculate cost coefficient, by tour purpose 
    double[]                         ivtCoeffs;
    double[]                         incomeCoeffs;
    double[]                         incomeExponents;

    private MgraDataManager mgraManager;
    
    //added for TNC and Taxi modes
    TNCAndTaxiWaitTimeCalculator tncTaxiWaitTimeCalculator = null;

    public TourModeChoiceModel(HashMap<String, String> propertyMap, ModelStructure myModelStructure,
            String myTourCategory, CtrampDmuFactoryIf dmuFactory, McLogsumsCalculator myLogsumHelper)
    {

        modelStructure = myModelStructure;
        tourCategory = myTourCategory;
        logsumHelper = myLogsumHelper;
        // logsumHelper passed in, but if it were instantiated here, it woul be
        // as follows
        // logsumHelper = new McLogsumsAppender();
        // logsumHelper.setupSkimCalculators(propertyMap);

        mcDmuObject = dmuFactory.getModeChoiceDMU();
        setupModeChoiceModelApplicationArray(propertyMap, tourCategory);

        mgraManager = MgraDataManager.getInstance(); 
        
        //get the coefficients for ivt and the coefficients to calculate the cost coefficient
        ivtCoeffs = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TOUR_UTILITY_IVT_COEFFS);
        incomeCoeffs = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TOUR_UTILITY_INCOME_COEFFS);
        incomeExponents = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TOUR_UTILITY_INCOME_EXPONENTS);

        tncTaxiWaitTimeCalculator = new TNCAndTaxiWaitTimeCalculator();
        tncTaxiWaitTimeCalculator.createWaitTimeDistributions(propertyMap);

    }

    public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator()
    {
        return logsumHelper.getAnmSkimCalculator();
    }

    private void setupModeChoiceModelApplicationArray(HashMap<String, String> propertyMap,
            String tourCategory)
    {

        logger.info(String.format("setting up %s tour mode choice model.", tourCategory));

        // locate the individual mandatory tour mode choice model UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = propertyMap.get(PROPERTIES_UEC_TOUR_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        // default is to not save the tour mode choice utils and probs for each
        // tour
        String saveUtilsProbsString = propertyMap
                .get(CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS);
        if (saveUtilsProbsString != null)
        {
            if (saveUtilsProbsString.equalsIgnoreCase("true")) saveUtilsProbsFlag = true;
        }

        // get the number of purposes and declare the array dimension to be this
        // size.
        HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();

        // create a HashMap to map purposeName to model index
        purposeModelIndexMap = new HashMap<String, Integer>();

        if (tourCategory.equalsIgnoreCase(MANDATORY_MODEL_INDICATOR))
        {
            tourPurposeList = new String[3];
            tourPurposeList[0] = ModelStructure.WORK_PRIMARY_PURPOSE_NAME;
            tourPurposeList[1] = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
            tourPurposeList[2] = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;

            int uecIndex = 1;
            int mcModelIndex = 0;
            for (String purposeName : tourPurposeList)
            {
                if (!modelIndexMap.containsKey(uecIndex))
                {
                    modelIndexMap.put(uecIndex, mcModelIndex);
                    purposeModelIndexMap.put(purposeName, mcModelIndex++);
                } else
                {
                    purposeModelIndexMap.put(purposeName, modelIndexMap.get(uecIndex));
                }
                uecIndex++;
            }

        } else if (tourCategory.equalsIgnoreCase(NON_MANDATORY_MODEL_INDICATOR))
        {
            tourPurposeList = new String[6];
            tourPurposeList[0] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
            tourPurposeList[1] = ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME;
            tourPurposeList[2] = ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME;
            tourPurposeList[3] = ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME;
            tourPurposeList[4] = ModelStructure.VISITING_PRIMARY_PURPOSE_NAME;
            tourPurposeList[5] = ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME;

            int maintSheet = Integer
                    .parseInt(propertyMap.get(PROPERTIES_UEC_MAINT_TOUR_MODE_SHEET));
            int discrSheet = Integer
                    .parseInt(propertyMap.get(PROPERTIES_UEC_DISCR_TOUR_MODE_SHEET));

            int uecIndex = 1;
            int mcModelIndex = 0;
            int i = 0;
            for (String purposeName : tourPurposeList)
            {

                uecIndex = -1;
                if (purposeName.equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)
                        || purposeName
                                .equalsIgnoreCase(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)
                        || purposeName
                                .equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) uecIndex = maintSheet;
                else if (purposeName.equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)
                        || purposeName
                                .equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)
                        || purposeName
                                .equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME))
                    uecIndex = discrSheet;

                // if the uec sheet for the model segment is not in the map, add
                // it, otherwise, get it from the map
                if (!modelIndexMap.containsKey(uecIndex))
                {
                    modelIndexMap.put(uecIndex, mcModelIndex);
                    purposeModelIndexMap.put(purposeName, mcModelIndex++);
                } else
                {
                    purposeModelIndexMap.put(purposeName, modelIndexMap.get(uecIndex));
                }
                i++;
            }

        } else if (tourCategory.equalsIgnoreCase(AT_WORK_SUBTOUR_MODEL_INDICATOR))
        {
            tourPurposeList = new String[1];
            tourPurposeList[0] = ModelStructure.WORK_BASED_PRIMARY_PURPOSE_NAME;

            int[] uecSheets = new int[1];
            uecSheets[0] = Integer
                    .parseInt(propertyMap.get(PROPERTIES_UEC_AT_WORK_TOUR_MODE_SHEET));

            int mcModelIndex = 0;
            int i = 0;
            for (String purposeName : tourPurposeList)
            {
                int uecIndex = uecSheets[i];

                // if the uec sheet for the model segment is not in the map, add
                // it, otherwise, get it from the map
                if (!modelIndexMap.containsKey(uecIndex))
                {
                    modelIndexMap.put(uecIndex, mcModelIndex);
                    purposeModelIndexMap.put(purposeName, mcModelIndex++);
                } else
                {
                    purposeModelIndexMap.put(purposeName, modelIndexMap.get(uecIndex));
                }
                i++;
            }

        }

        mcModel = new ChoiceModelApplication[modelIndexMap.size()];

        // declare dimensions for the array of choice alternative availability
        // by
        // purpose
        modeAltNames = new String[purposeModelIndexMap.size()][];

        // for each unique model index, create the ChoiceModelApplication object
        // and
        // the availabilty array
        int i = 0;
        for (int m : modelIndexMap.keySet())
        {
            mcModel[i] = new ChoiceModelApplication(mcUecFile, m, MC_DATA_SHEET, propertyMap,
                    (VariableTable) mcDmuObject);
            modeAltNames[i] = mcModel[i].getAlternativeNames();
            i++;
        }
        

    }

    public double getModeChoiceLogsum(Household household, Person person, Tour tour,
            Logger modelLogger, String choiceModelDescription, String decisionMakerLabel)
    {

        // update the MC dmuObjects for this person
        mcDmuObject.setHouseholdObject(household);
        mcDmuObject.setPersonObject(person);
        mcDmuObject.setTourObject(tour);
        mcDmuObject.setDmuIndexValues(household.getHhId(), tour.getTourDestMgra(),
                tour.getTourOrigMgra(), tour.getTourDestMgra(), household.getDebugChoiceModels());
        mcDmuObject.setOriginMgra(tour.getTourOrigMgra());
        mcDmuObject.setDestMgra(tour.getTourDestMgra());
        
        float SingleTNCWaitTimeOrig = 0;
        float SingleTNCWaitTimeDest = 0;
        float SharedTNCWaitTimeOrig = 0;
        float SharedTNCWaitTimeDest = 0;
        float TaxiWaitTimeOrig = 0;
        float TaxiWaitTimeDest = 0;
        float popEmpDenOrig = (float) mgraManager.getPopEmpPerSqMi(tour.getTourOrigMgra());
        float popEmpDenDest = (float) mgraManager.getPopEmpPerSqMi(tour.getTourDestMgra());
        
        if(household!=null){
            Random hhRandom = household.getHhRandom();
            double rnum = hhRandom.nextDouble();
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenDest);
        }else{
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime( popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime(popEmpDenDest);
        }

        mcDmuObject.setOrigTaxiWaitTime(TaxiWaitTimeOrig);
        mcDmuObject.setDestTaxiWaitTime(TaxiWaitTimeDest);
        mcDmuObject.setOrigSingleTNCWaitTime(SingleTNCWaitTimeOrig);
        mcDmuObject.setDestSingleTNCWaitTime(SingleTNCWaitTimeDest);
        mcDmuObject.setOrigSharedTNCWaitTime(SharedTNCWaitTimeOrig);
        mcDmuObject.setDestSharedTNCWaitTime(SharedTNCWaitTimeDest);
        
        return getModeChoiceLogsum(mcDmuObject, tour, modelLogger, choiceModelDescription,
                decisionMakerLabel);
    }

    public double getModeChoiceLogsum(TourModeChoiceDMU mcDmuObject, Tour tour, Logger modelLogger,
            String choiceModelDescription, String decisionMakerLabel)
    {

        int modelIndex = purposeModelIndexMap.get(tour.getTourPrimaryPurpose());

        Household household = tour.getPersonObject().getHouseholdObject();
        double income = (double) household.getIncomeInDollars();
        double timeFactor = 1.0f;
        if(tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        	timeFactor = mcDmuObject.getJointTourTimeFactor();
        else if(tour.getTourPrimaryPurposeIndex()==ModelStructure.WORK_PRIMARY_PURPOSE_INDEX)
        	timeFactor = mcDmuObject.getWorkTimeFactor();
        else
        	timeFactor = mcDmuObject.getNonWorkTimeFactor();
        
        double ivtCoeff    = ivtCoeffs[modelIndex];
        double incomeCoeff = incomeCoeffs[modelIndex];
        double incomeExpon = incomeExponents[modelIndex];
        double costCoeff = calculateCostCoefficient(income, incomeCoeff,incomeExpon);

        mcDmuObject.setIvtCoeff(ivtCoeff*timeFactor);
        mcDmuObject.setCostCoeff(costCoeff);

        float SingleTNCWaitTimeOrig = 0;
        float SingleTNCWaitTimeDest = 0;
        float SharedTNCWaitTimeOrig = 0;
        float SharedTNCWaitTimeDest = 0;
        float TaxiWaitTimeOrig = 0;
        float TaxiWaitTimeDest = 0;
        float popEmpDenOrig = (float) mgraManager.getPopEmpPerSqMi(tour.getTourOrigMgra());
        float popEmpDenDest = (float) mgraManager.getPopEmpPerSqMi(tour.getTourDestMgra());
        
        if(household!=null){
            Random hhRandom = household.getHhRandom();
            double rnum = hhRandom.nextDouble();
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenDest);
        }else{
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime( popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime(popEmpDenDest);
        }

        mcDmuObject.setOrigTaxiWaitTime(TaxiWaitTimeOrig);
        mcDmuObject.setDestTaxiWaitTime(TaxiWaitTimeDest);
        mcDmuObject.setOrigSingleTNCWaitTime(SingleTNCWaitTimeOrig);
        mcDmuObject.setDestSingleTNCWaitTime(SingleTNCWaitTimeDest);
        mcDmuObject.setOrigSharedTNCWaitTime(SharedTNCWaitTimeOrig);
        mcDmuObject.setDestSharedTNCWaitTime(SharedTNCWaitTimeDest);

        // log headers to traceLogger
        if (household.getDebugChoiceModels())
        {
            mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);
        }

        double logsum = logsumHelper.calculateTourMcLogsum(tour.getTourOrigMgra(),
                tour.getTourDestMgra(), tour.getTourDepartPeriod(), tour.getTourArrivePeriod(),
                mcModel[modelIndex], mcDmuObject);

        // write UEC calculation results to separate model specific log file
        if (household.getDebugChoiceModels())
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

    public int getModeChoice(TourModeChoiceDMU mcDmuObject, String purposeName)
    {

        int modelIndex = purposeModelIndexMap.get(purposeName);

        Household household = mcDmuObject.getHouseholdObject();
        Tour tour = mcDmuObject.getTourObject();
        double income = (double) household.getIncomeInDollars();
        double ivtCoeff    = ivtCoeffs[modelIndex];
        double incomeCoeff = incomeCoeffs[modelIndex];
        double incomeExpon = incomeExponents[modelIndex];
        double costCoeff = calculateCostCoefficient(income, incomeCoeff,incomeExpon);
        double timeFactor = 1.0f;
        if(tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        	timeFactor = mcDmuObject.getJointTourTimeFactor();
        else if(tour.getTourPrimaryPurposeIndex()==ModelStructure.WORK_PRIMARY_PURPOSE_INDEX)
        	timeFactor = mcDmuObject.getWorkTimeFactor();
        else
        	timeFactor = mcDmuObject.getNonWorkTimeFactor();
        
        mcDmuObject.setIvtCoeff(ivtCoeff * timeFactor);
        mcDmuObject.setCostCoeff(costCoeff);

        float SingleTNCWaitTimeOrig = 0;
        float SingleTNCWaitTimeDest = 0;
        float SharedTNCWaitTimeOrig = 0;
        float SharedTNCWaitTimeDest = 0;
        float TaxiWaitTimeOrig = 0;
        float TaxiWaitTimeDest = 0;
        float popEmpDenOrig = (float) mgraManager.getPopEmpPerSqMi(tour.getTourOrigMgra());
        float popEmpDenDest = (float) mgraManager.getPopEmpPerSqMi(tour.getTourDestMgra());
        
        if(household!=null){
            Random hhRandom = household.getHhRandom();
            double rnum = hhRandom.nextDouble();
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSingleTNCWaitTimeDistribution(rnum, popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromSharedTNCWaitTimeDistribution(rnum, popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.sampleFromTaxiWaitTimeDistribution(rnum, popEmpDenDest);
        }else{
            SingleTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenOrig);
            SingleTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDenDest);
            SharedTNCWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenOrig);
            SharedTNCWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDenDest);
            TaxiWaitTimeOrig = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime( popEmpDenOrig);
            TaxiWaitTimeDest = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime(popEmpDenDest);
        }

        mcDmuObject.setOrigTaxiWaitTime(TaxiWaitTimeOrig);
        mcDmuObject.setDestTaxiWaitTime(TaxiWaitTimeDest);
        mcDmuObject.setOrigSingleTNCWaitTime(SingleTNCWaitTimeOrig);
        mcDmuObject.setDestSingleTNCWaitTime(SingleTNCWaitTimeDest);
        mcDmuObject.setOrigSharedTNCWaitTime(SharedTNCWaitTimeOrig);
        mcDmuObject.setDestSharedTNCWaitTime(SharedTNCWaitTimeDest);

        Logger modelLogger = null;
        if (tourCategory.equalsIgnoreCase(ModelStructure.MANDATORY_CATEGORY)) modelLogger = tourMCManLogger;
        else modelLogger = tourMCNonManLogger;

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";


        if (household.getDebugChoiceModels())
        {

            if (tour.getTourCategory()
                    .equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
            {
                Person person = null;
                Person[] persons = mcDmuObject.getHouseholdObject().getPersons();
                int[] personNums = tour.getPersonNumArray();
                for (int n = 0; n < personNums.length; n++)
                {
                    int p = personNums[n];
                    person = persons[p];

                    choiceModelDescription = String.format(
                            "%s Tour Mode Choice Model for: Purpose=%s, Home=%d, Dest=%d",
                            tourCategory, purposeName, household.getHhMgra(),
                            tour.getTourDestMgra());
                    decisionMakerLabel = String
                            .format("HH=%d, person record %d of %d in joint tour, PersonNum=%d, PersonType=%s, TourId=%d",
                                    person.getHouseholdObject().getHhId(), p, personNums.length,
                                    person.getPersonNum(), person.getPersonType(), tour.getTourId());
                    loggingHeader = String.format("%s    %s", choiceModelDescription,
                            decisionMakerLabel);

                    mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
                            choiceModelDescription, decisionMakerLabel);

                    modelLogger.info(" ");
                    for (int k = 0; k < loggingHeader.length(); k++)
                        separator += "+";
                    modelLogger.info(loggingHeader);
                    modelLogger.info(separator);

                    household.logTourObject(loggingHeader, modelLogger, person, tour);
                }
            } else
            {
                Person person = mcDmuObject.getPersonObject();

                choiceModelDescription = String.format(
                        "%s Tour Mode Choice Model for: Purpose=%s, Orig=%d, Dest=%d",
                        tourCategory, purposeName, tour.getTourOrigMgra(), tour.getTourDestMgra());
                decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s, TourId=%d",
                        person.getHouseholdObject().getHhId(), person.getPersonNum(),
                        person.getPersonType(), tour.getTourId());
                loggingHeader = String.format("%s    %s", choiceModelDescription,
                        decisionMakerLabel);

                mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                        decisionMakerLabel);

                modelLogger.info(" ");
                for (int k = 0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.info(loggingHeader);
                modelLogger.info(separator);

                household.logTourObject(loggingHeader, modelLogger, person, tour);
            }

        }

        logsumHelper.setTourMcDmuAttributes(mcDmuObject, tour.getTourOrigMgra(),
                tour.getTourDestMgra(), tour.getTourDepartPeriod(), tour.getTourArrivePeriod(),
                (household.getDebugChoiceModels() && DEBUG_BEST_PATHS));

        // mode choice UEC references highway skim matrices directly, so set
        // index orig/dest to O/D TAZs.
        IndexValues mcDmuIndex = mcDmuObject.getDmuIndexValues();
        mcDmuIndex.setOriginZone(mgraManager.getTaz(tour.getTourOrigMgra()));
        mcDmuIndex.setDestZone(mgraManager.getTaz(tour.getTourDestMgra()));
        mcDmuIndex.setZoneIndex(tour.getTourDestMgra());
        mcDmuObject.setOriginMgra(tour.getTourOrigMgra());
        mcDmuObject.setDestMgra(tour.getTourDestMgra());
        
        float logsum = (float) mcModel[modelIndex].computeUtilities(mcDmuObject, mcDmuIndex);
        tour.setTourModeLogsum(logsum);

        mcDmuIndex.setOriginZone(tour.getTourOrigMgra());
        mcDmuIndex.setDestZone(tour.getTourDestMgra());

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen;
        if (mcModel[modelIndex].getAvailabilityCount() > 0)
        {

            chosen = mcModel[modelIndex].getChoiceResult(rn);

            // best tap pairs were determined and saved in mcDmuObject while
            // setting dmu skim attributes
            // if chosen mode is a transit mode, save these tap pairs in the
            // tour object; if not transit tour attributes remain null.
            if (modelStructure.getTourModeIsTransit(chosen))
            {
                tour.setBestWtwTapPairsOut(logsumHelper.getBestWtwTapsOut());
                tour.setBestWtwTapPairsIn(logsumHelper.getBestWtwTapsIn());
                tour.setBestWtdTapPairsOut(logsumHelper.getBestWtdTapsOut());
                tour.setBestWtdTapPairsIn(logsumHelper.getBestWtdTapsIn());
                tour.setBestDtwTapPairsOut(logsumHelper.getBestDtwTapsOut());
                tour.setBestDtwTapPairsIn(logsumHelper.getBestDtwTapsIn());
            }
            
            //value of time; lookup vot, votS2, or votS3 from the UEC depending on chosen mode
            UtilityExpressionCalculator uec = mcModel[modelIndex].getUEC();
            
            double vot = 0.0;
            
            if(modelStructure.getTourModeIsS2(chosen)){
                int votIndex = uec.lookupVariableIndex("votS2");
                vot = uec.getValueForIndex(votIndex);
            }else if (modelStructure.getTourModeIsS3(chosen)){
                int votIndex = uec.lookupVariableIndex("votS3");
                vot = uec.getValueForIndex(votIndex);
            }else{
                int votIndex = uec.lookupVariableIndex("vot");
                vot = uec.getValueForIndex(votIndex);
            }
            tour.setValueOfTime(vot);
            

        } else
        {

            if (tour.getTourCategory()
                    .equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
            {
                Person person = null;
                Person[] persons = mcDmuObject.getHouseholdObject().getPersons();
                int[] personNums = tour.getPersonNumArray();
                for (int n = 0; n < personNums.length; n++)
                {
                    int p = personNums[n];
                    person = persons[p];

                    choiceModelDescription = String
                            .format("No alternatives available for %s Tour Mode Choice Model for: Purpose=%s, Home=%d, Dest=%d",
                                    tourCategory, purposeName, household.getHhMgra(),
                                    tour.getTourDestMgra());
                    decisionMakerLabel = String
                            .format("HH=%d, person record %d of %d in joint tour, PersonNum=%d, PersonType=%s, TourId=%d",
                                    person.getHouseholdObject().getHhId(), p, personNums.length,
                                    person.getPersonNum(), person.getPersonType(), tour.getTourId());
                    loggingHeader = String.format("%s    %s", choiceModelDescription,
                            decisionMakerLabel);

                    mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
                            choiceModelDescription, decisionMakerLabel);

                    modelLogger.info(" ");
                    for (int k = 0; k < loggingHeader.length(); k++)
                        separator += "+";
                    modelLogger.info(loggingHeader);
                    modelLogger.info(separator);

                    household.logTourObject(loggingHeader, modelLogger, person, tour);
                }
            } else
            {
                Person person = mcDmuObject.getPersonObject();

                choiceModelDescription = String
                        .format("No alternatives available for %s Tour Mode Choice Model for: Purpose=%s, Orig=%d, Dest=%d",
                                tourCategory, purposeName, tour.getTourOrigMgra(),
                                tour.getTourDestMgra());
                decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s, TourId=%d",
                        person.getHouseholdObject().getHhId(), person.getPersonNum(),
                        person.getPersonType(), tour.getTourId());
                loggingHeader = String.format("%s    %s", choiceModelDescription,
                        decisionMakerLabel);

                mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                        decisionMakerLabel);

                modelLogger.info(" ");
                for (int k = 0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.info(loggingHeader);
                modelLogger.info(separator);

                household.logTourObject(loggingHeader, modelLogger, person, tour);
            }

            mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
            modelLogger.info("");
            modelLogger.info("");

            logger.error(String
                    .format("Exception caught for HHID=%d, no available %s tour mode alternatives to choose from in choiceModelApplication.",
                            household.getHhId(), tourCategory));
            throw new RuntimeException();
        }

        // debug output
        if (household.getDebugChoiceModels())
        {

            double[] utilities = mcModel[modelIndex].getUtilities(); // 0s-indexing
            double[] probabilities = mcModel[modelIndex].getProbabilities(); // 0s-indexing
            boolean[] availabilities = mcModel[modelIndex].getAvailabilities(); // 1s-indexing
            String[] altNames = mcModel[modelIndex].getAlternativeNames(); // 0s-indexing

            if (tour.getTourCategory()
                    .equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
            {
                modelLogger.info("Joint Tour Id: " + tour.getTourId());
            } else
            {
                Person person = mcDmuObject.getPersonObject();
                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();
                modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString
                        + ", Tour Id: " + tour.getTourId());
            }
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
            modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

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

            tour.setTourModalUtilities(utils);
            tour.setTourModalProbabilities(probs);

        }

        return chosen;

    }

    public String[] getModeAltNames(int purposeIndex)
    {
        int modelIndex = purposeModelIndexMap.get(tourPurposeList[purposeIndex]);
        return modeAltNames[modelIndex];
    }
    
    /**
     * This method calculates a cost coefficient based on the following formula:
     * 
     *   costCoeff = incomeCoeff * 1/(max(income,1000)^incomeExponent)
     * 
     * 
     * @param incomeCoeff
     * @param incomeExponent
     * @return A cost coefficent that should be multiplied by cost variables (cents) in tour mode choice
     */
    public double calculateCostCoefficient(double income, double incomeCoeff, double incomeExponent){
    	
    	return incomeCoeff * 1.0/(Math.pow(Math.max(income,1000.0),incomeExponent));
    	
    }

}
