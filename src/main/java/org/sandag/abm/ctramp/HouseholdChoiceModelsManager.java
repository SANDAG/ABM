package org.sandag.abm.ctramp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import org.sandag.abm.accessibilities.NonTransitUtilities;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;

public final class HouseholdChoiceModelsManager
        implements Serializable
{

    private static transient Logger             logger                                         = Logger.getLogger(HouseholdChoiceModelsManager.class);

    private static final String                 USE_NEW_SOA_METHOD_PROPERTY_KEY                = "nmdc.use.new.soa";

    private static final String                 TAZ_FIELD_NAME                                 = "TAZ";
    private static final String                 TP_CHOICE_AVG_TTS_FILE                         = "tc.choice.avgtts.file";
    private static final String                 AVGTTS_COLUMN_NAME                             = "AVGTTS";
    private static final String                 TRANSP_DIST_COLUMN_NAME                        = "DIST";
    private static final String                 PCT_DETOUR_COLUMN_NAME                         = "PCTDETOUR";

    private static final String                 IE_EXTERNAL_TAZS_KEY                           = "external.tazs";
    private static final String                 IE_DISTANCE_LOGSUM_COEFF_KEY                   = "ie.logsum.distance.coeff";

    private static String                       PROPERTIES_NON_MANDATORY_DC_SOA_UEC_FILE       = "nonSchool.soa.uec.file";
    private static String                       PROPERTIES_ESCORT_DC_SOA_UEC_MODEL_PAGE        = "escort.soa.uec.model";
    private static String                       PROPERTIES_ESCORT_DC_SOA_UEC_DATA_PAGE         = "escort.soa.uec.data";
    private static String                       PROPERTIES_NON_MANDATORY_DC_SOA_UEC_MODEL_PAGE = "other.nonman.soa.uec.model";
    private static String                       PROPERTIES_NON_MANDATORY_DC_SOA_UEC_DATA_PAGE  = "other.nonman.soa.uec.data";
    private static String                       PROPERTIES_ATWORK_DC_SOA_UEC_MODEL_PAGE        = "atwork.soa.uec.model";
    private static String                       PROPERTIES_ATWORK_DC_SOA_UEC_DATA_PAGE         = "atwork.soa.uec.data";

    private static HouseholdChoiceModelsManager objInstance                                    = null;

    private LinkedList<HouseholdChoiceModels>   modelQueue                                     = null;

    private HashMap<String, String>             propertyMap;
    private String                              restartModelString;
    private ModelStructure                      modelStructure;
    private CtrampDmuFactoryIf                  dmuFactory;

    private MgraDataManager                     mgraManager;
    private TazDataManager                      tdm;

    private int                                 maxMgra;
    private int                                 maxTaz;

    private BuildAccessibilities                aggAcc;

    private int                                 completedHouseholds;
    private int                                 modelIndex;

    // store taz-taz exponentiated utilities (period, from taz, to taz)
    private double[][][]                        sovExpUtilities;
    private double[][][]                        hovExpUtilities;
    private double[][][]                        nMotorExpUtilities;

    private double[]                            pctHighIncome;
    private double[]                            pctMultipleAutos;

    private double[]                            avgtts;
    private double[]                            transpDist;
    private double[]                            pctDetour;

    private double[][][]                        nonMandatorySizeProbs;
    private double[][][]                        nonMandatoryTazDistProbs;
    private double[][][]                        subTourSizeProbs;
    private double[][][]                        subTourTazDistProbs;

    private AutoTazSkimsCalculator              tazDistanceCalculator;

    private boolean                             useNewSoaMethod;
    private boolean logResults=false;

    private HouseholdChoiceModelsManager()
    {
    }

    public static synchronized HouseholdChoiceModelsManager getInstance()
    {
        // logger.info(
        // "beginning of HouseholdChoiceModelsManager() - objInstance address = "
        // + objInstance );
        if (objInstance == null)
        {
            objInstance = new HouseholdChoiceModelsManager();
            // logger.info(
            // "after new HouseholdChoiceModelsManager() - objInstance address = "
            // + objInstance );
            return objInstance;
        } else
        {
            // logger.info(
            // "returning current HouseholdChoiceModelsManager() - objInstance address = "
            // + objInstance );
            return objInstance;
        }
    }

    // the task instances should call needToInitialize() first, then this method
    // if necessary.
    public synchronized void managerSetup(MatrixDataServerIf ms,
            HouseholdDataManagerIf hhDataManager, HashMap<String, String> propertyMap,
            String restartModelString, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory)
    {

        if (modelQueue != null) return;

        // get the DestChoiceModelManager instance and clear the objects that
        // hold large memory references
        DestChoiceModelManager.getInstance().clearDcModels();

        modelIndex = 0;
        completedHouseholds = 0;

        this.propertyMap = propertyMap;
        this.restartModelString = restartModelString;
        this.modelStructure = modelStructure;
        this.dmuFactory = dmuFactory;

        logResults = Util.getStringValueFromPropertyMap(propertyMap, "RunModel.LogResults")
                .equalsIgnoreCase("true");
        
        mgraManager = MgraDataManager.getInstance(propertyMap);
        maxMgra = mgraManager.getMaxMgra();

        tdm = TazDataManager.getInstance(propertyMap);
        maxTaz = tdm.getMaxTaz();

        pctHighIncome = hhDataManager.getPercentHhsIncome100Kplus();
        pctMultipleAutos = hhDataManager.getPercentHhsMultipleAutos();
        readTpChoiceAvgTtsFile();

        MatrixDataManager mdm = MatrixDataManager.getInstance();
        mdm.setMatrixDataServerObject(ms);

        aggAcc = BuildAccessibilities.getInstance();
        if (!aggAcc.getAccessibilitiesAreBuilt())
        {
            logger.info("creating Accessibilities Object for Household Choice Models.");

            aggAcc.setupBuildAccessibilities(propertyMap, false);

            aggAcc.calculateSizeTerms();
            aggAcc.calculateConstants();

            // assume that if the filename exists, at was created previously,
            // either in another model run, or by the main client
            // if the filename doesn't exist, then calculate the accessibilities
            String projectDirectory = propertyMap
                    .get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
            String accFileName = projectDirectory
                    + Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file");
            boolean accFileReadFlag = Util.getBooleanValueFromPropertyMap(propertyMap,
                    CtrampApplication.READ_ACCESSIBILITIES);

            if (accFileReadFlag && (new File(accFileName)).canRead())
            {

                logger.info("filling Accessibilities Object in HouseholdChoiceModelManager by reading file: "
                        + accFileName + ".");
                aggAcc.readAccessibilityTableFromFile(accFileName);

            } else
            {

                logger.info("filling Accessibilities Object HouseholdChoiceModelManager by calculating them.");
                aggAcc.calculateDCUtilitiesDistributed(propertyMap);

            }

        }

        useNewSoaMethod = Util.getBooleanValueFromPropertyMap(propertyMap,
                USE_NEW_SOA_METHOD_PROPERTY_KEY);

        if (useNewSoaMethod)
        {
            // compute the arrays of cumulative probabilities based on mgra size
            // for mgras within each origin taz.
            logger.info("pre-computing non-mandatory purpose SOA Distance and Size probabilities.");
            computeNonMandatorySegmentSizeArrays(dmuFactory);

            logger.info("pre-computing at-work sub-tour purpose SOA Distance and Size probabilities.");
            computeSubtourSegmentSizeArrays(modelStructure, dmuFactory);
        }

        tazDistanceCalculator = new AutoTazSkimsCalculator(propertyMap);
        tazDistanceCalculator.computeTazDistanceArrays();

        // the first thread to reach this method initializes the modelQueue used
        // to
        // recycle hhChoiceModels objects.
        modelQueue = new LinkedList<HouseholdChoiceModels>();

        mgraManager = MgraDataManager.getInstance(propertyMap);

    }

    /**
     * @return DestChoiceModel object created if none is available from the
     *         queue.
     * 
     */
    public synchronized HouseholdChoiceModels getHouseholdChoiceModelsObject(int taskIndex)
    {

        String message = "";
        HouseholdChoiceModels hhChoiceModels = null;

        if (modelQueue.isEmpty())
        {

            NonTransitUtilities ntUtilities = new NonTransitUtilities(propertyMap, sovExpUtilities,
                    hovExpUtilities, nMotorExpUtilities);

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
            logsumHelper.setupSkimCalculators(propertyMap);
            logsumHelper.setTazDistanceSkimArrays(
                    tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                    tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

            MandatoryAccessibilitiesCalculator mandAcc = new MandatoryAccessibilitiesCalculator(
                    propertyMap, ntUtilities, aggAcc.getExpConstants(),
                    logsumHelper.getBestTransitPathCalculator());

            // calculate array of distanceToExternalCordon logsums by taz for
            // use by internal-external model
            double[] distanceToCordonsLogsums = computeTazDistanceToExternalCordonLogsums();

            // create choice model object
            hhChoiceModels = new HouseholdChoiceModels(++modelIndex, restartModelString,
                    propertyMap, modelStructure, dmuFactory, aggAcc, logsumHelper, mandAcc,
                    pctHighIncome, pctMultipleAutos, avgtts, transpDist, pctDetour,
                    nonMandatoryTazDistProbs, nonMandatorySizeProbs, subTourTazDistProbs,
                    subTourSizeProbs, distanceToCordonsLogsums, tazDistanceCalculator);
            if(logResults){
            		message = String.format("created hhChoiceModels=%d, task=%d, thread=%s.", modelIndex,
                    taskIndex, Thread.currentThread().getName());
		            logger.info(message);
		            logger.info("");
            }

        } else
        {
            hhChoiceModels = modelQueue.remove();
            if(logResults){
	            	message = String.format("removed hhChoiceModels=%d from queue, task=%d, thread=%s.",
	                    hhChoiceModels.getModelIndex(), taskIndex, Thread.currentThread().getName());
		            logger.info(message);
		            logger.info("");
            }
        }
        
        return hhChoiceModels;

    }

    /**
     * return the HouseholdChoiceModels object to the manager's queue so that it
     * may be used by another thread without it having to create one.
     * 
     * @param hhModels
     */
    public void returnHouseholdChoiceModelsObject(HouseholdChoiceModels hhModels, int startIndex,
            int endIndex)
    {
        modelQueue.add(hhModels);
        completedHouseholds += (endIndex - startIndex + 1);
        if(logResults){
	        logger.info("returned hhChoiceModels=" + hhModels.getModelIndex() + " to queue: thread="
	                + Thread.currentThread().getName() + ", completedHouseholds=" + completedHouseholds
	                + ".");
        }
    }

    public synchronized void clearHhModels()
    {

        if (modelQueue == null) return;

        logger.info(String.format("%s:  clearing household choice models modelQueue, thread=%s.",
                new Date(), Thread.currentThread().getName()));
        while (!modelQueue.isEmpty())
            modelQueue.remove();

        modelIndex = 0;
        completedHouseholds = 0;

        modelQueue = null;

    }

    private void readTpChoiceAvgTtsFile()
    {

        // construct input household file name from properties file values
        String projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);

        String inputFileName = propertyMap.get(TP_CHOICE_AVG_TTS_FILE);
        String fileName = projectDirectory + inputFileName;

        TableDataSet table;
        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            table = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading tp choice avgtts data file: %s into TableDataSet object.",
                            fileName));
            throw new RuntimeException(e);
        }

        int[] tazField = table.getColumnAsInt(TAZ_FIELD_NAME);
        double[] avgttsField = table.getColumnAsDouble(AVGTTS_COLUMN_NAME);
        double[] transpDistField = table.getColumnAsDouble(TRANSP_DIST_COLUMN_NAME);
        double[] pctDetourField = table.getColumnAsDouble(PCT_DETOUR_COLUMN_NAME);

        avgtts = new double[tdm.getMaxTaz() + 1];
        transpDist = new double[tdm.getMaxTaz() + 1];
        pctDetour = new double[tdm.getMaxTaz() + 1];

        // loop over the number of mgra records in the TableDataSet.
        for (int k = 0; k < tdm.getMaxTaz(); k++)
        {

            // get the mgra value for TableDataSet row k from the mgra field.
            int taz = tazField[k];

            avgtts[taz] = avgttsField[k];
            transpDist[taz] = transpDistField[k];
            pctDetour[taz] = pctDetourField[k];

        }

    }

    private void computeNonMandatorySegmentSizeArrays(CtrampDmuFactoryIf dmuFactory)
    {

        // compute the array of cumulative taz distance based SOA probabilities
        // for each origin taz.
        DestChoiceTwoStageSoaTazDistanceUtilityDMU dcDistSoaDmu = dmuFactory
                .getDestChoiceSoaTwoStageTazDistUtilityDMU();

        // the size term array in aggAcc gives mgra*purpose - need an array of
        // all mgras for one purpose
        double[][] aggAccDcSizeArray = aggAcc.getSizeTerms();

        String[] tourPurposeNames = {ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME,
                ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME,
                ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME,
                ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME,
                ModelStructure.VISITING_PRIMARY_PURPOSE_NAME,
                ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME};

        int[] sizeSheetIndices = {BuildAccessibilities.ESCORT_INDEX,
                BuildAccessibilities.SHOP_INDEX, BuildAccessibilities.OTH_MAINT_INDEX,
                BuildAccessibilities.EATOUT_INDEX, BuildAccessibilities.VISIT_INDEX,
                BuildAccessibilities.OTH_DISCR_INDEX};

        HashMap<String, Integer> nonMandatorySegmentNameIndexMap = new HashMap<String, Integer>();
        HashMap<String, Integer> nonMandatorySizeSegmentNameIndexMap = new HashMap<String, Integer>();
        for (int k = 0; k < tourPurposeNames.length; k++)
        {
            nonMandatorySegmentNameIndexMap.put(tourPurposeNames[k], k);
            nonMandatorySizeSegmentNameIndexMap.put(tourPurposeNames[k], sizeSheetIndices[k]);
        }

        double[][] dcSizeArray = new double[tourPurposeNames.length][aggAccDcSizeArray.length];
        for (int i = 0; i < aggAccDcSizeArray.length; i++)
        {
            for (int m : nonMandatorySegmentNameIndexMap.values())
            {
                int s = sizeSheetIndices[m];
                dcSizeArray[m][i] = aggAccDcSizeArray[i][s];
            }
        }

        // compute the arrays of cumulative probabilities based on mgra size for
        // mgras within each origin taz.
        nonMandatorySizeProbs = new double[tourPurposeNames.length][][];
        nonMandatoryTazDistProbs = new double[tourPurposeNames.length][][];

        DestChoiceTwoStageSoaProbabilitiesCalculator nonManSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                propertyMap, dmuFactory, PROPERTIES_NON_MANDATORY_DC_SOA_UEC_FILE,
                PROPERTIES_NON_MANDATORY_DC_SOA_UEC_MODEL_PAGE,
                PROPERTIES_NON_MANDATORY_DC_SOA_UEC_DATA_PAGE);

        for (String tourPurpose : tourPurposeNames)
        {

            int purposeSizeIndex = nonMandatorySizeSegmentNameIndexMap.get(tourPurpose);

            // compute the TAZ size values from the mgra values and the
            // correspondence between mgras and tazs.
            if (tourPurpose.equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME))
            {

                double[] mgraData = new double[maxMgra + 1];
                double[] tazData = null;

                // aggregate TAZ grade school enrollment and set array in DMU
                for (int i = 1; i <= maxMgra; i++)
                    mgraData[i] = aggAcc.getMgraGradeSchoolEnrollment(i);
                tazData = computeTazSize(mgraData);
                dcDistSoaDmu.setTazGsEnrollment(tazData);

                // aggregate TAZ high school enrollment and set array in DMU
                for (int i = 1; i <= maxMgra; i++)
                    mgraData[i] = aggAcc.getMgraHighSchoolEnrollment(i);
                tazData = computeTazSize(mgraData);
                dcDistSoaDmu.setTazHsEnrollment(tazData);

                // aggregate TAZ households and set array in DMU
                for (int i = 1; i <= maxMgra; i++)
                    mgraData[i] = aggAcc.getMgraHouseholds(i);
                tazData = computeTazSize(mgraData);
                dcDistSoaDmu.setNumHhs(tazData);

                DestChoiceTwoStageSoaProbabilitiesCalculator escortSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                        propertyMap, dmuFactory, PROPERTIES_NON_MANDATORY_DC_SOA_UEC_FILE,
                        PROPERTIES_ESCORT_DC_SOA_UEC_MODEL_PAGE,
                        PROPERTIES_ESCORT_DC_SOA_UEC_DATA_PAGE);

                logger.info("     " + tourPurpose + " probabilities");
                nonMandatoryTazDistProbs[purposeSizeIndex] = escortSoaDistProbsObject
                        .computeDistanceProbabilities(dcDistSoaDmu);

            } else
            {

                // aggregate TAZ size for the non-mandatoy purpose and set array
                // in DMU
                double[] tazSize = computeTazSize(dcSizeArray[purposeSizeIndex]);
                dcDistSoaDmu.setDestChoiceTazSize(tazSize);

                logger.info("     " + tourPurpose + " probabilities");
                nonMandatoryTazDistProbs[purposeSizeIndex] = nonManSoaDistProbsObject
                        .computeDistanceProbabilities(dcDistSoaDmu);

            }

            nonMandatorySizeProbs[purposeSizeIndex] = computeSizeSegmentProbabilities(dcSizeArray[purposeSizeIndex]);

        }

    }

    private void computeSubtourSegmentSizeArrays(ModelStructure modelStructure,
            CtrampDmuFactoryIf dmuFactory)
    {

        // compute the array of cumulative taz distance based SOA probabilities
        // for each origin taz.
        DestChoiceTwoStageSoaTazDistanceUtilityDMU dcDistSoaDmu = dmuFactory
                .getDestChoiceSoaTwoStageTazDistUtilityDMU();

        // the size term array in aggAcc gives mgra*purpose - need an array of
        // all mgras for one purpose
        double[][] aggAccDcSizeArray = aggAcc.getSizeTerms();

        String[] tourPurposeNames = {modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME,
                modelStructure.AT_WORK_EAT_PURPOSE_NAME, modelStructure.AT_WORK_MAINT_PURPOSE_NAME};

        int[] sizeSheetIndices = {SubtourDestChoiceModel.PROPERTIES_AT_WORK_BUSINESS_SIZE_SHEET,
                SubtourDestChoiceModel.PROPERTIES_AT_WORK_EAT_OUT_SIZE_SHEET,
                SubtourDestChoiceModel.PROPERTIES_AT_WORK_OTHER_SIZE_SHEET};

        HashMap<String, Integer> segmentNameIndexMap = new HashMap<String, Integer>();
        HashMap<String, Integer> sizeSegmentNameIndexMap = new HashMap<String, Integer>();
        for (int k = 0; k < tourPurposeNames.length; k++)
        {
            segmentNameIndexMap.put(tourPurposeNames[k], k);
            sizeSegmentNameIndexMap.put(tourPurposeNames[k], sizeSheetIndices[k]);
        }

        double[][] dcSizeArray = new double[tourPurposeNames.length][aggAccDcSizeArray.length];
        for (int i = 0; i < aggAccDcSizeArray.length; i++)
        {
            for (int m : segmentNameIndexMap.values())
            {
                int s = sizeSheetIndices[m];
                dcSizeArray[m][i] = aggAccDcSizeArray[i][s];
            }
        }

        // compute the arrays of cumulative probabilities based on mgra size for
        // mgras within each origin taz.
        subTourSizeProbs = new double[tourPurposeNames.length][][];
        subTourTazDistProbs = new double[tourPurposeNames.length][][];

        DestChoiceTwoStageSoaProbabilitiesCalculator subTourSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                propertyMap, dmuFactory, PROPERTIES_NON_MANDATORY_DC_SOA_UEC_FILE,
                PROPERTIES_ATWORK_DC_SOA_UEC_MODEL_PAGE, PROPERTIES_ATWORK_DC_SOA_UEC_DATA_PAGE);

        for (String tourPurpose : tourPurposeNames)
        {

            int purposeSizeIndex = segmentNameIndexMap.get(tourPurpose);

            // aggregate TAZ size for the non-mandatoy purpose and set array in
            // DMU
            double[] tazSize = computeTazSize(dcSizeArray[purposeSizeIndex]);
            dcDistSoaDmu.setDestChoiceTazSize(tazSize);

            logger.info("     " + tourPurpose + " probabilities");
            subTourTazDistProbs[purposeSizeIndex] = subTourSoaDistProbsObject
                    .computeDistanceProbabilities(dcDistSoaDmu);

            subTourSizeProbs[purposeSizeIndex] = computeSizeSegmentProbabilities(dcSizeArray[purposeSizeIndex]);

        }

    }

    private double[] computeTazSize(double[] size)
    {

        // this is a 0-based array of cumulative probabilities
        double[] tazSize = new double[maxTaz + 1];

        for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
        {

            int[] mgraArray = tdm.getMgraArray(taz);
            if (mgraArray != null)
            {
                for (int mgra : mgraArray)
                {
                    tazSize[taz] += size[mgra] + (size[mgra] > 0 ? 1 : 0);
                }
            }

        }

        return tazSize;

    }

    private double[][] computeSizeSegmentProbabilities(double[] size)
    {

        // this is a 0-based array of cumulative probabilities
        double[][] sizeProbs = new double[maxTaz][];

        for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
        {

            int[] mgraArray = tdm.getMgraArray(taz);

            if (mgraArray == null)
            {
                sizeProbs[taz - 1] = new double[0];
            } else
            {
                double totalSize = 0;
                for (int mgra : mgraArray)
                    totalSize += size[mgra] + (size[mgra] > 0 ? 1 : 0);

                if (totalSize > 0)
                {
                    sizeProbs[taz - 1] = new double[mgraArray.length];
                    for (int i = 0; i < mgraArray.length; i++)
                    {
                        double mgraSize = size[mgraArray[i]];
                        if (mgraSize > 0) mgraSize += 1;
                        sizeProbs[taz - 1][i] = mgraSize / totalSize;
                    }
                } else
                {
                    sizeProbs[taz - 1] = new double[0];
                }
            }

        }

        return sizeProbs;

    }

    private double[] computeTazDistanceToExternalCordonLogsums()
    {

        int maxTaz = tdm.getMaxTaz();
        String uecPath = propertyMap.get("uec.path");
        String altFileName = uecPath + propertyMap.get("internalExternal.dc.uec.alts.file");
        TableDataSet altData = readFile(altFileName);

        int tazCol = altData.getColumnPosition("taz");
        int ieCol = altData.getColumnPosition("iePct");
        altData.buildIndex(tazCol);

        // get parameters used to develop distance to cordon logsums for IE
        // model
        String coeffString = propertyMap.get(IE_DISTANCE_LOGSUM_COEFF_KEY);
        double coeff = Double.parseDouble(coeffString);

        ArrayList<Integer> tazList = new ArrayList<Integer>();
        String externalTazListString = propertyMap.get(IE_EXTERNAL_TAZS_KEY);
        StringTokenizer st = new StringTokenizer(externalTazListString, ",");
        while (st.hasMoreTokens())
        {
            String listValue = st.nextToken();
            int tazValue = Integer.parseInt(listValue.trim());
            tazList.add(tazValue);
        }
        int[] externalTazs = new int[tazList.size()];
        for (int i = 0; i < externalTazs.length; i++)
            externalTazs[i] = tazList.get(i);

        // get stored distance arrays
        double[][][] periodDistanceMatrices = tazDistanceCalculator
                .getStoredFromTazToAllTazsDistanceSkims();

        // compute the TAZ x EXTERNAL TAZ distance based logsums.
        double[] tazDistLogsums = new double[maxTaz + 1];
        for (int i = 1; i <= maxTaz; i++)
        {

            double sum = 0;
            for (int j = 0; j < externalTazs.length; j++)
            {
                double distanceToExternal = periodDistanceMatrices[TransitWalkAccessUEC.MD][i][externalTazs[j]];
                double iePct = altData.getValueAt(externalTazs[j], ieCol);
                sum += iePct * MathUtil.exp(coeff * distanceToExternal);
            }

            tazDistLogsums[i] = MathUtil.log(sum);
        }

        return tazDistLogsums;

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

}
