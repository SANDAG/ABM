package org.sandag.abm.ctramp;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;

public class DestChoiceModelManager
        implements Serializable
{

    private static String                                PROPERTIES_WORK_DC_SOA_UEC_FILE       = "work.soa.uec.file";
    private static String                                PROPERTIES_WORK_DC_SOA_UEC_MODEL_PAGE = "work.soa.uec.model";
    private static String                                PROPERTIES_WORK_DC_SOA_UEC_DATA_PAGE  = "work.soa.uec.data";

    private static String                                PROPERTIES_UNIV_DC_SOA_UEC_FILE       = "univ.soa.uec.file";
    private static String                                PROPERTIES_UNIV_DC_SOA_UEC_MODEL_PAGE = "univ.soa.uec.model";
    private static String                                PROPERTIES_UNIV_DC_SOA_UEC_DATA_PAGE  = "univ.soa.uec.data";

    private static String                                PROPERTIES_HS_DC_SOA_UEC_FILE         = "hs.soa.uec.file";
    private static String                                PROPERTIES_HS_DC_SOA_UEC_MODEL_PAGE   = "hs.soa.uec.model";
    private static String                                PROPERTIES_HS_DC_SOA_UEC_DATA_PAGE    = "hs.soa.uec.data";

    private static String                                PROPERTIES_GS_DC_SOA_UEC_FILE         = "gs.soa.uec.file";
    private static String                                PROPERTIES_GS_DC_SOA_UEC_MODEL_PAGE   = "gs.soa.uec.model";
    private static String                                PROPERTIES_GS_DC_SOA_UEC_DATA_PAGE    = "gs.soa.uec.data";

    private static String                                PROPERTIES_PS_DC_SOA_UEC_FILE         = "ps.soa.uec.file";
    private static String                                PROPERTIES_PS_DC_SOA_UEC_MODEL_PAGE   = "ps.soa.uec.model";
    private static String                                PROPERTIES_PS_DC_SOA_UEC_DATA_PAGE    = "ps.soa.uec.data";

    private static final int                             PRESCHOOL_ALT_INDEX                   = BuildAccessibilities.PRESCHOOL_ALT_INDEX;
    private static final int                             GRADE_SCHOOL_ALT_INDEX                = BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX;
    private static final int                             HIGH_SCHOOL_ALT_INDEX                 = BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX;
    private static final int                             UNIV_TYPICAL_ALT_INDEX                = BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX;
    private static final int                             UNIV_NONTYPICAL_ALT_INDEX             = BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX;
    private static final int                             NUMBER_OF_SCHOOL_SEGMENT_TYPES        = 5;

    private static transient Logger                      logger                                = Logger.getLogger(DestChoiceModelManager.class);

    private static DestChoiceModelManager                objInstance                           = null;

    private LinkedList<WorkLocationChoiceModel>          modelQueueWorkLoc                     = null;
    private LinkedList<SchoolLocationChoiceModel>        modelQueueSchoolLoc                   = null;
    private LinkedList<MandatoryDestChoiceModel>         modelQueueWork                        = null;
    private LinkedList<MandatoryDestChoiceModel>         modelQueueSchool                      = null;

    private MgraDataManager                              mgraManager;
    private TazDataManager                               tdm;

    private int                                          maxTaz;

    private BuildAccessibilities                         aggAcc;

    private HashMap<String, String>                      propertyMap;
    private String                                       dcUecFileName;
    private String                                       soaUecFileName;
    private int                                          soaSampleSize;
    private String                                       modeChoiceUecFileName;
    private CtrampDmuFactoryIf                           dmuFactory;

    private int                                          modelIndexWork;
    private int                                          modelIndexSchool;
    private int                                          currentIteration;

    private DestChoiceTwoStageSoaTazDistanceUtilityDMU   locChoiceDistSoaDmu;
    private DestChoiceTwoStageSoaProbabilitiesCalculator workLocSoaDistProbsObject;
    private DestChoiceTwoStageSoaProbabilitiesCalculator psLocSoaDistProbsObject;
    private DestChoiceTwoStageSoaProbabilitiesCalculator gsLocSoaDistProbsObject;
    private DestChoiceTwoStageSoaProbabilitiesCalculator hsLocSoaDistProbsObject;
    private DestChoiceTwoStageSoaProbabilitiesCalculator univLocSoaDistProbsObject;

    // the first dimension on these arrays is work location segments (worker
    // occupations)
    private double[][][]                                 workSizeProbs;
    private double[][][]                                 workTazDistProbs;

    // the first dimension on these arrays is school location segment type (ps,
    // gs, hs, univTypical, univNonTypical)
    private double[][][]                                 schoolSizeProbs;
    private double[][][]                                 schoolTazDistProbs;

    private AutoTazSkimsCalculator                       tazDistanceCalculator;

    private boolean                                      managerIsSetup                        = false;

    private int                                          completedHouseholdsWork;
    private int                                          completedHouseholdsSchool;

    private DestChoiceModelManager()
    {
    }

    public static synchronized DestChoiceModelManager getInstance()
    {
        // logger.info(
        // "beginning of DestChoiceModelManager.getInstance() - objInstance address = "
        // + objInstance );
        if (objInstance == null)
        {
            objInstance = new DestChoiceModelManager();
            // logger.info(
            // "after new DestChoiceModelManager() - objInstance address = " +
            // objInstance );
            return objInstance;
        } else
        {
            // logger.info(
            // "returning current DestChoiceModelManager() - objInstance address = "
            // + objInstance );
            return objInstance;
        }
    }

    // the task instances should call needToInitialize() first, then this method
    // if necessary.
    public synchronized void managerSetup(HashMap<String, String> propertyMap,
            ModelStructure modelStructure, MatrixDataServerIf ms, String dcUecFileName,
            String soaUecFileName, int soaSampleSize, CtrampDmuFactoryIf dmuFactory,
            String restartModelString)
    {

        if (managerIsSetup) return;

        // get the HouseholdChoiceModelsManager instance and clear the objects
        // that hold large memory references
        HouseholdChoiceModelsManager.getInstance().clearHhModels();

        modelIndexWork = 0;
        modelIndexSchool = 0;
        completedHouseholdsWork = 0;
        completedHouseholdsSchool = 0;

        System.out.println(String.format("initializing DC ModelManager: thread=%s.", Thread
                .currentThread().getName()));

        this.propertyMap = propertyMap;
        this.dcUecFileName = dcUecFileName;
        this.soaUecFileName = soaUecFileName;
        this.soaSampleSize = soaSampleSize;
        this.dmuFactory = dmuFactory;

        mgraManager = MgraDataManager.getInstance(propertyMap);
        tdm = TazDataManager.getInstance(propertyMap);
        maxTaz = tdm.getMaxTaz();

        modelQueueWorkLoc = new LinkedList<WorkLocationChoiceModel>();
        modelQueueSchoolLoc = new LinkedList<SchoolLocationChoiceModel>();
        modelQueueWork = new LinkedList<MandatoryDestChoiceModel>();
        modelQueueSchool = new LinkedList<MandatoryDestChoiceModel>();

        // Initialize the MatrixDataManager to use the MatrixDataServer instance
        // passed in, unless ms is null.
        if (ms == null)
        {

            logger.info(Thread.currentThread().getName()
                    + ": No remote MatrixServer being used, MatrixDataManager will get created when needed by DestChoiceModelManager.");
        } else
        {

            String testString = ms.testRemote(Thread.currentThread().getName());
            logger.info(String.format(Thread.currentThread().getName()
                    + ": DestChoiceModelManager connecting to remote MatrixDataServer."));
            logger.info(String.format("MatrixDataServer connection test: %s", testString));
            MatrixDataManager mdm = MatrixDataManager.getInstance();
            mdm.setMatrixDataServerObject(ms);

        }

        aggAcc = BuildAccessibilities.getInstance();

        // assume that if the filename exists, at was created previously, either
        // in another model run, or by the main client
        // if the filename doesn't exist, then calculate the accessibilities
        String projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String accFileName = projectDirectory
                + Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file");
        boolean accFileReadFlag = Util.getBooleanValueFromPropertyMap(propertyMap,
                CtrampApplication.READ_ACCESSIBILITIES);

        if ((new File(accFileName)).canRead())
        {

            logger.info("filling Accessibilities Object in DestChoiceModelManager by reading file: "
                    + accFileName + ".");
            aggAcc.readAccessibilityTableFromFile(accFileName);

            aggAcc.setupBuildAccessibilities(propertyMap, false);
            aggAcc.createSchoolSegmentNameIndices();

            aggAcc.calculateSizeTerms();

        } else
        {

            aggAcc.setupBuildAccessibilities(propertyMap, false);
            aggAcc.createSchoolSegmentNameIndices();

            aggAcc.calculateSizeTerms();
            aggAcc.calculateConstants();

            logger.info("filling Accessibilities Object in DestChoiceModelManager by calculating them.");
            aggAcc.calculateDCUtilitiesDistributed(propertyMap);

        }

        // compute the array of cumulative taz distance based SOA probabilities
        // for each origin taz.
        locChoiceDistSoaDmu = dmuFactory.getDestChoiceSoaTwoStageTazDistUtilityDMU();

        tazDistanceCalculator = new AutoTazSkimsCalculator(propertyMap);
        tazDistanceCalculator.computeTazDistanceArrays();

        managerIsSetup = true;

    }

    public synchronized void returnWorkLocModelObject(WorkLocationChoiceModel dcModel,
            int taskIndex, int startIndex, int endIndex)
    {
        modelQueueWorkLoc.add(dcModel);
        completedHouseholdsWork += (endIndex - startIndex + 1);
        logger.info(String
                .format("returned workLocationChoice[%d,%d] to workQueueLoc, task=%d, thread=%s, completedHouseholds=%d.",
                        currentIteration, dcModel.getModelIndex(), taskIndex, Thread
                                .currentThread().getName(), completedHouseholdsWork));
    }

    public synchronized void returnDcWorkModelObject(MandatoryDestChoiceModel dcModel,
            int taskIndex, int startIndex, int endIndex)
    {
        modelQueueWork.add(dcModel);
        completedHouseholdsWork += (endIndex - startIndex + 1);
        logger.info(String
                .format("returned dcModelWork[%d,%d] to workQueue, task=%d, thread=%s, completedHouseholds=%d.",
                        currentIteration, dcModel.getModelIndex(), taskIndex, Thread
                                .currentThread().getName(), completedHouseholdsWork));
    }

    public synchronized void returnSchoolLocModelObject(SchoolLocationChoiceModel dcModel,
            int taskIndex, int startIndex, int endIndex)
    {
        modelQueueSchoolLoc.add(dcModel);
        completedHouseholdsSchool += (endIndex - startIndex + 1);
        logger.info(String
                .format("returned schoolLocationChoice[%d,%d] to schoolQueueLoc, task=%d, thread=%s, completedHouseholds=%d.",
                        currentIteration, dcModel.getModelIndex(), taskIndex, Thread
                                .currentThread().getName(), completedHouseholdsSchool));
    }

    public synchronized void returnDcSchoolModelObject(MandatoryDestChoiceModel dcModel,
            int taskIndex, int startIndex, int endIndex)
    {
        modelQueueSchool.add(dcModel);
        completedHouseholdsSchool += (endIndex - startIndex + 1);
        logger.info(String
                .format("returned dcModelSchool[%d,%d] to schoolQueue, task=%d, thread=%s, completedHouseholds=%d.",
                        currentIteration, dcModel.getModelIndex(), taskIndex, Thread
                                .currentThread().getName(), completedHouseholdsSchool));
    }

    public synchronized WorkLocationChoiceModel getWorkLocModelObject(int taskIndex, int iteration,
            DestChoiceSize dcSizeObj, int[] uecIndices, int[] soaUecIndices)
    {

        // can release memory for the school location choice probabilities
        // before running school location choice
        clearSchoolProbabilitiesArrys();

        WorkLocationChoiceModel dcModel = null;

        if (!modelQueueWorkLoc.isEmpty())
        {

            // the first task processed with an iteration parameter greater than
            // the manager's
            // current iteration updates the manager's SOA size and dist
            // probabilities arrays and
            // updates the iteration count.
            if (iteration > currentIteration)
            {

                // update the arrays of cumulative probabilities based on mgra
                // size for mgras within each origin taz.
                double[][] dcSizeArray = dcSizeObj.getDcSizeArray();
                updateWorkSoaProbabilities(workLocSoaDistProbsObject, dcSizeObj, workSizeProbs,
                        workTazDistProbs, dcSizeArray);

                currentIteration = iteration;
                completedHouseholdsWork = 0;
            }

            dcModel = modelQueueWorkLoc.remove();
            dcModel.setDcSizeObject(dcSizeObj);

            logger.info(String.format(
                    "removed workLocationChoice[%d,%d] from workQueueLoc, task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        } else
        {

            if (modelIndexWork == 0 && iteration == 0)
            {

                // compute the arrays of cumulative probabilities based on mgra
                // size for mgras within each origin taz.
                logger.info("pre-computing work SOA Distance and Size probabilities.");
                workLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                        propertyMap, dmuFactory, PROPERTIES_WORK_DC_SOA_UEC_FILE,
                        PROPERTIES_WORK_DC_SOA_UEC_MODEL_PAGE, PROPERTIES_WORK_DC_SOA_UEC_DATA_PAGE);
                double[][] dcSizeArray = dcSizeObj.getDcSizeArray();
                workSizeProbs = new double[dcSizeArray.length][maxTaz][];
                workTazDistProbs = new double[dcSizeArray.length][][];
                updateWorkSoaProbabilities(workLocSoaDistProbsObject, dcSizeObj, workSizeProbs,
                        workTazDistProbs, dcSizeArray);

                currentIteration = 0;
                completedHouseholdsWork = 0;
            }

            modelIndexWork++;

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
            logsumHelper.setupSkimCalculators(propertyMap);
            logsumHelper.setTazDistanceSkimArrays(
                    tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                    tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

            // pass null in instead if modelStructure, since it's not available
            // and won't be needed for logsum calculation.
            TourModeChoiceModel immcModel = new TourModeChoiceModel(propertyMap, null,
                    TourModeChoiceModel.MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);

            dcModel = new WorkLocationChoiceModel(modelIndexWork, propertyMap, dcSizeObj, aggAcc,
                    dcUecFileName, soaUecFileName, soaSampleSize, modeChoiceUecFileName,
                    dmuFactory, immcModel, workSizeProbs, workTazDistProbs);

            dcModel.setupWorkSegments(uecIndices, soaUecIndices);
            dcModel.setupDestChoiceModelArrays(propertyMap, dcUecFileName, soaUecFileName,
                    soaSampleSize);

            logger.info(String.format("created workLocationChoice[%d,%d], task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        }

        return dcModel;

    }

    public synchronized MandatoryDestChoiceModel getDcWorkModelObject(int taskIndex, int iteration,
            DestChoiceSize dcSizeObj, int[] uecIndices, int[] soaUecIndices)
    {

        MandatoryDestChoiceModel dcModel = null;

        if (!modelQueueWork.isEmpty())
        {

            // the first task processed with an iteration parameter greater than
            // the manager's
            // current iteration updates the manager's SOA size and dist
            // probabilities arrays and
            // updates the iteration count.
            if (iteration > currentIteration)
            {

                currentIteration = iteration;
                completedHouseholdsWork = 0;
            }

            dcModel = modelQueueWork.remove();
            dcModel.setDcSizeObject(dcSizeObj);

            logger.info(String.format(
                    "removed dcModelWork[%d,%d] from workQueue, task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        } else
        {

            if (modelIndexWork == 0 && iteration == 0)
            {

                currentIteration = 0;
                completedHouseholdsWork = 0;
            }

            modelIndexWork++;

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
            logsumHelper.setupSkimCalculators(propertyMap);
            logsumHelper.setTazDistanceSkimArrays(
                    tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                    tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

            // pass null in instead if modelStructure, since it's not available
            // and won't be needed for logsum calculation.
            TourModeChoiceModel immcModel = new TourModeChoiceModel(propertyMap, null,
                    TourModeChoiceModel.MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);

            dcModel = new MandatoryDestChoiceModel(modelIndexWork, propertyMap, dcSizeObj, aggAcc,
                    mgraManager, dcUecFileName, soaUecFileName, soaSampleSize,
                    modeChoiceUecFileName, dmuFactory, immcModel);

            dcModel.setupWorkSegments(uecIndices, soaUecIndices);
            dcModel.setupDestChoiceModelArrays(propertyMap, dcUecFileName, soaUecFileName,
                    soaSampleSize);

            logger.info(String.format("created dcModelWork[%d,%d], task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        }

        return dcModel;

    }

    public synchronized SchoolLocationChoiceModel getSchoolLocModelObject(int taskIndex,
            int iteration, DestChoiceSize dcSizeObj)
    {
        // can release memory for the work location choice probabilities before
        // running school location choice
        clearWorkProbabilitiesArrys();
        clearWorkLocModels();

        SchoolLocationChoiceModel dcModel = null;

        int[] gsDistrict = new int[maxTaz + 1];
        int[] hsDistrict = new int[maxTaz + 1];
        double[] univEnrollment = new double[maxTaz + 1];

        if (!modelQueueSchoolLoc.isEmpty())
        {

            // the first task processed with an iteration parameter greater than
            // the
            // manager's current iteration count clears the dcModel cache and
            // updates the iteration count.
            if (iteration > currentIteration)
            {

                // compute the exponentiated distance utilities that all
                // segments of this tour purpose will share
                double[][] tazDistExpUtils = null;

                logger.info("updating pre-school SOA Distance and Size probabilities.");
                tazDistExpUtils = computeTazDistanceExponentiatedUtilities(psLocSoaDistProbsObject);
                updateSchoolSoaProbabilities(aggAcc.getPsSegmentNameIndexMap(), dcSizeObj,
                        tazDistExpUtils, schoolSizeProbs[PRESCHOOL_ALT_INDEX],
                        schoolTazDistProbs[PRESCHOOL_ALT_INDEX]);

                logger.info("updating grade school SOA Distance and Size probabilities.");
                tazDistExpUtils = computeTazDistanceExponentiatedUtilities(gsLocSoaDistProbsObject);
                updateSchoolSoaProbabilities(aggAcc.getGsSegmentNameIndexMap(), dcSizeObj,
                        tazDistExpUtils, schoolSizeProbs[GRADE_SCHOOL_ALT_INDEX],
                        schoolTazDistProbs[GRADE_SCHOOL_ALT_INDEX]);

                logger.info("updating high school SOA Distance and Size probabilities.");
                tazDistExpUtils = computeTazDistanceExponentiatedUtilities(hsLocSoaDistProbsObject);
                updateSchoolSoaProbabilities(aggAcc.getHsSegmentNameIndexMap(), dcSizeObj,
                        tazDistExpUtils, schoolSizeProbs[HIGH_SCHOOL_ALT_INDEX],
                        schoolTazDistProbs[HIGH_SCHOOL_ALT_INDEX]);

                logger.info("updating university-typical school SOA Distance and Size probabilities.");
                tazDistExpUtils = computeTazDistanceExponentiatedUtilities(univLocSoaDistProbsObject);
                updateSchoolSoaProbabilities(aggAcc.getUnivTypicalSegmentNameIndexMap(), dcSizeObj,
                        tazDistExpUtils, schoolSizeProbs[UNIV_TYPICAL_ALT_INDEX],
                        schoolTazDistProbs[UNIV_TYPICAL_ALT_INDEX]);

                logger.info("updating university-non-typical school SOA Distance and Size probabilities.");
                updateSchoolSoaProbabilities(aggAcc.getUnivNonTypicalSegmentNameIndexMap(),
                        dcSizeObj, tazDistExpUtils, schoolSizeProbs[UNIV_NONTYPICAL_ALT_INDEX],
                        schoolTazDistProbs[UNIV_NONTYPICAL_ALT_INDEX]);

                currentIteration = iteration;
                completedHouseholdsSchool = 0;

            }

            dcModel = modelQueueSchoolLoc.remove();
            dcModel.setDcSizeObject(dcSizeObj);

            logger.info(String.format(
                    "removed schoolLocationChoice[%d,%d] from schoolQueueLoc, task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        } else
        {

            if (modelIndexSchool == 0 && iteration == 0)
            {

                // if the schoolSizeProbs array is null, no task has yet
                // initialized the probabilities arrays, so enter the block.
                // if not null, the arrays have been computed, so it's ok to
                // skip.
                if (schoolSizeProbs == null)
                {

                    // compute the exponentiated distance utilities that all
                    // segments of this tour purpose will share
                    double[][] tazDistExpUtils = null;

                    int[] gsDistrictByMgra = aggAcc.getMgraGsDistrict();
                    int[] hsDistrictByMgra = aggAcc.getMgraHsDistrict();

                    // determine university enrollment by TAZs
                    for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
                    {
                        int[] mgraArray = tdm.getMgraArray(taz);
                        if (mgraArray != null)
                        {
                            for (int mgra : mgraArray)
                            {
                                univEnrollment[taz] = aggAcc.getMgraUniversityEnrollment(mgra);
                            }
                        }
                    }
                    locChoiceDistSoaDmu.setTazUnivEnrollment(univEnrollment);

                    // determine grade school and high school districts by TAZs
                    for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
                    {
                        int[] mgraArray = tdm.getMgraArray(taz);
                        if (mgraArray != null)
                        {
                            for (int mgra : mgraArray)
                            {
                                gsDistrict[taz] = gsDistrictByMgra[mgra];
                                hsDistrict[taz] = hsDistrictByMgra[mgra];
                                break;
                            }
                        }
                    }
                    locChoiceDistSoaDmu.setTazGsDistricts(gsDistrict);
                    locChoiceDistSoaDmu.setTazHsDistricts(hsDistrict);

                    schoolSizeProbs = new double[NUMBER_OF_SCHOOL_SEGMENT_TYPES][maxTaz][];
                    schoolTazDistProbs = new double[NUMBER_OF_SCHOOL_SEGMENT_TYPES][maxTaz][maxTaz];

                    // compute the arrays of cumulative probabilities based on
                    // mgra size for mgras within each origin taz.
                    try
                    {
                        logger.info("pre-computing pre-school SOA Distance and Size probabilities.");
                        psLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                                propertyMap, dmuFactory, PROPERTIES_PS_DC_SOA_UEC_FILE,
                                PROPERTIES_PS_DC_SOA_UEC_MODEL_PAGE,
                                PROPERTIES_PS_DC_SOA_UEC_DATA_PAGE);
                        tazDistExpUtils = computeTazDistanceExponentiatedUtilities(psLocSoaDistProbsObject);
                        updateSchoolSoaProbabilities(aggAcc.getPsSegmentNameIndexMap(), dcSizeObj,
                                tazDistExpUtils, schoolSizeProbs[PRESCHOOL_ALT_INDEX],
                                schoolTazDistProbs[PRESCHOOL_ALT_INDEX]);
                    } catch (Exception e)
                    {
                        logger.error("exception caught updating pre-school SOA probabilities", e);
                        System.exit(-1);
                    }

                    try
                    {
                        logger.info("pre-computing grade school SOA Distance and Size probabilities.");
                        gsLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                                propertyMap, dmuFactory, PROPERTIES_GS_DC_SOA_UEC_FILE,
                                PROPERTIES_GS_DC_SOA_UEC_MODEL_PAGE,
                                PROPERTIES_GS_DC_SOA_UEC_DATA_PAGE);
                        tazDistExpUtils = computeTazDistanceExponentiatedUtilities(gsLocSoaDistProbsObject);
                        updateSchoolSoaProbabilities(aggAcc.getGsSegmentNameIndexMap(), dcSizeObj,
                                tazDistExpUtils, schoolSizeProbs[GRADE_SCHOOL_ALT_INDEX],
                                schoolTazDistProbs[GRADE_SCHOOL_ALT_INDEX]);
                    } catch (Exception e)
                    {
                        logger.error("exception caught updating grade school SOA probabilities", e);
                        System.exit(-1);
                    }

                    try
                    {
                        logger.info("pre-computing high school SOA Distance and Size probabilities.");
                        hsLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                                propertyMap, dmuFactory, PROPERTIES_HS_DC_SOA_UEC_FILE,
                                PROPERTIES_HS_DC_SOA_UEC_MODEL_PAGE,
                                PROPERTIES_HS_DC_SOA_UEC_DATA_PAGE);
                        tazDistExpUtils = computeTazDistanceExponentiatedUtilities(hsLocSoaDistProbsObject);
                        updateSchoolSoaProbabilities(aggAcc.getHsSegmentNameIndexMap(), dcSizeObj,
                                tazDistExpUtils, schoolSizeProbs[HIGH_SCHOOL_ALT_INDEX],
                                schoolTazDistProbs[HIGH_SCHOOL_ALT_INDEX]);
                    } catch (Exception e)
                    {
                        logger.error("exception caught updating high school SOA probabilities", e);
                        System.exit(-1);
                    }

                    try
                    {
                        logger.info("pre-computing university-typical SOA Distance and Size probabilities.");
                        univLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                                propertyMap, dmuFactory, PROPERTIES_UNIV_DC_SOA_UEC_FILE,
                                PROPERTIES_UNIV_DC_SOA_UEC_MODEL_PAGE,
                                PROPERTIES_UNIV_DC_SOA_UEC_DATA_PAGE);
                        tazDistExpUtils = computeTazDistanceExponentiatedUtilities(univLocSoaDistProbsObject);
                        updateSchoolSoaProbabilities(aggAcc.getUnivTypicalSegmentNameIndexMap(),
                                dcSizeObj, tazDistExpUtils,
                                schoolSizeProbs[UNIV_TYPICAL_ALT_INDEX],
                                schoolTazDistProbs[UNIV_TYPICAL_ALT_INDEX]);
                    } catch (Exception e)
                    {
                        logger.error("exception caught updating university SOA probabilities", e);
                        System.exit(-1);
                    }

                    try
                    {
                        logger.info("pre-computing university-non-typical SOA Distance and Size probabilities.");
                        univLocSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                                propertyMap, dmuFactory, PROPERTIES_UNIV_DC_SOA_UEC_FILE,
                                PROPERTIES_UNIV_DC_SOA_UEC_MODEL_PAGE,
                                PROPERTIES_UNIV_DC_SOA_UEC_DATA_PAGE);
                        updateSchoolSoaProbabilities(aggAcc.getUnivNonTypicalSegmentNameIndexMap(),
                                dcSizeObj, tazDistExpUtils,
                                schoolSizeProbs[UNIV_NONTYPICAL_ALT_INDEX],
                                schoolTazDistProbs[UNIV_NONTYPICAL_ALT_INDEX]);
                    } catch (Exception e)
                    {
                        logger.error("exception caught updating university SOA probabilities", e);
                        System.exit(-1);
                    }

                    currentIteration = 0;
                    completedHouseholdsSchool = 0;

                }

            }

            modelIndexSchool++;

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
            logsumHelper.setupSkimCalculators(propertyMap);
            logsumHelper.setTazDistanceSkimArrays(
                    tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                    tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

            // pass null in instead if modelStructure, since it's not available
            // and won't be needed for logsum calculation.
            TourModeChoiceModel immcModel = new TourModeChoiceModel(propertyMap, null,
                    TourModeChoiceModel.MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);

            dcModel = new SchoolLocationChoiceModel(modelIndexSchool, propertyMap, dcSizeObj,
                    aggAcc, dcUecFileName, soaUecFileName, soaSampleSize, modeChoiceUecFileName,
                    dmuFactory, immcModel, schoolSizeProbs, schoolTazDistProbs);

            dcModel.setupSchoolSegments();
            dcModel.setupDestChoiceModelArrays(propertyMap, dcUecFileName, soaUecFileName,
                    soaSampleSize);

            logger.info(String.format("created schoolLocationChoice[%d,%d], task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        }

        return dcModel;

    }

    public synchronized MandatoryDestChoiceModel getDcSchoolModelObject(int taskIndex,
            int iteration, DestChoiceSize dcSizeObj)
    {

        MandatoryDestChoiceModel dcModel = null;
        if (!modelQueueSchool.isEmpty())
        {

            // the first task processed with an iteration parameter greater than
            // the
            // manager's current iteration count clears the dcModel cache and
            // updates the iteration count.
            if (iteration > currentIteration)
            {

                currentIteration = iteration;
                completedHouseholdsSchool = 0;

            }

            dcModel = modelQueueSchool.remove();
            dcModel.setDcSizeObject(dcSizeObj);

            logger.info(String.format(
                    "removed dcModelSchool[%d,%d] from schoolQueue, task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        } else
        {

            if (modelIndexSchool == 0 && iteration == 0)
            {
                currentIteration = 0;
                completedHouseholdsSchool = 0;
            }

            modelIndexSchool++;

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
            logsumHelper.setupSkimCalculators(propertyMap);
            logsumHelper.setTazDistanceSkimArrays(
                    tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                    tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

            // pass null in instead if modelStructure, since it's not available
            // and won't be needed for logsum calculation.
            TourModeChoiceModel immcModel = new TourModeChoiceModel(propertyMap, null,
                    TourModeChoiceModel.MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);

            dcModel = new MandatoryDestChoiceModel(modelIndexSchool, propertyMap, dcSizeObj,
                    aggAcc, mgraManager, dcUecFileName, soaUecFileName, soaSampleSize,
                    modeChoiceUecFileName, dmuFactory, immcModel);

            dcModel.setupSchoolSegments();
            dcModel.setupDestChoiceModelArrays(propertyMap, dcUecFileName, soaUecFileName,
                    soaSampleSize);

            logger.info(String.format("created dcModelSchool[%d,%d], task=%d, thread=%s.",
                    currentIteration, dcModel.getModelIndex(), taskIndex, Thread.currentThread()
                            .getName()));

        }

        return dcModel;

    }

    public synchronized void clearDcModels()
    {

        clearWorkLocModels();
        clearSchoolLocModels();
        clearWorkProbabilitiesArrys();
        clearSchoolProbabilitiesArrys();

        if (tazDistanceCalculator != null)
        {
            tazDistanceCalculator.clearStoredTazsDistanceSkims();
            tazDistanceCalculator = null;
        }

        logger.info("DestChoiceModelManager elements cleared.");

    }

    private void clearWorkLocModels()
    {

        if (modelQueueWorkLoc != null && !modelQueueWorkLoc.isEmpty())
        {

            logger.info(String.format(
                    "%s:  clearing dc choice models modelQueueWorkLoc, thread=%s.", new Date(),
                    Thread.currentThread().getName()));

            while (!modelQueueWorkLoc.isEmpty())
                modelQueueWorkLoc.remove();
            modelIndexWork = 0;
            completedHouseholdsWork = 0;

        }

        if (modelQueueWork != null && !modelQueueWork.isEmpty())
        {

            logger.info(String.format("%s:  clearing dc choice models modelQueueWork, thread=%s.",
                    new Date(), Thread.currentThread().getName()));
            while (!modelQueueWork.isEmpty())
                modelQueueWork.remove();

            modelIndexWork = 0;
            completedHouseholdsWork = 0;

        }

    }

    private void clearSchoolLocModels()
    {

        if (modelQueueSchoolLoc != null && !modelQueueSchoolLoc.isEmpty())
        {

            logger.info(String.format(
                    "%s:  clearing dc choice models modelQueueSchoolLoc, thread=%s.", new Date(),
                    Thread.currentThread().getName()));
            while (!modelQueueSchoolLoc.isEmpty())
                modelQueueSchoolLoc.remove();

            modelIndexSchool = 0;
            completedHouseholdsSchool = 0;

        }

        if (modelQueueSchool != null && !modelQueueSchool.isEmpty())
        {

            logger.info(String.format(
                    "%s:  clearing dc choice models modelQueueSchool, thread=%s.", new Date(),
                    Thread.currentThread().getName()));
            while (!modelQueueSchool.isEmpty())
                modelQueueSchool.remove();

            modelIndexSchool = 0;
            completedHouseholdsSchool = 0;

        }

    }

    private void clearWorkProbabilitiesArrys()
    {

        // null out the cache of probabilities arrays for work location choice
        if (workSizeProbs != null)
        {
            for (int i = 0; i < workSizeProbs.length; i++)
            {
                if (workSizeProbs[i] != null)
                {
                    for (int j = 0; j < workSizeProbs[i].length; j++)
                        workSizeProbs[i][j] = null;
                }
                workSizeProbs[i] = null;
            }
            workSizeProbs = null;
        }

        if (workTazDistProbs != null)
        {
            for (int i = 0; i < workTazDistProbs.length; i++)
            {
                if (workTazDistProbs[i] != null)
                {
                    for (int j = 0; j < workTazDistProbs[i].length; j++)
                        workTazDistProbs[i][j] = null;
                }
                workTazDistProbs[i] = null;
            }
            workTazDistProbs = null;
        }

    }

    private void clearSchoolProbabilitiesArrys()
    {

        // null out the cache of probabilities arrays for work location choice
        if (schoolSizeProbs != null)
        {
            for (int i = 0; i < schoolSizeProbs.length; i++)
            {
                if (schoolSizeProbs[i] != null)
                {
                    for (int j = 0; j < schoolSizeProbs[i].length; j++)
                        schoolSizeProbs[i][j] = null;
                }
                schoolSizeProbs[i] = null;
            }
            schoolSizeProbs = null;
        }

        if (schoolTazDistProbs != null)
        {
            for (int i = 0; i < schoolTazDistProbs.length; i++)
            {
                if (schoolTazDistProbs[i] != null)
                {
                    for (int j = 0; j < schoolTazDistProbs[i].length; j++)
                        schoolTazDistProbs[i][j] = null;
                }
                schoolTazDistProbs[i] = null;
            }
            schoolTazDistProbs = null;
        }

    }

    private void updateWorkSoaProbabilities(
            DestChoiceTwoStageSoaProbabilitiesCalculator locChoiceSoaDistProbsObject,
            DestChoiceSize dcSizeObj, double[][][] sizeProbs, double[][][] tazDistProbs,
            double[][] dcSizeArray)
    {

        HashMap<String, Integer> segmentNameIndexMap = dcSizeObj.getSegmentNameIndexMap();

        for (String segmentName : segmentNameIndexMap.keySet())
        {

            int segmentIndex = segmentNameIndexMap.get(segmentName);

            // compute the TAZ size values from the mgra values and the
            // correspondence between mgras and tazs.
            double[] tazSize = computeTazSize(dcSizeArray[segmentIndex]);
            locChoiceDistSoaDmu.setDestChoiceTazSize(tazSize);

            // tazDistProbs[segmentIndex] =
            // locChoiceSoaDistProbsObject.computeDistanceProbabilities( 3737,
            // locChoiceDistSoaDmu );
            tazDistProbs[segmentIndex] = locChoiceSoaDistProbsObject
                    .computeDistanceProbabilities(locChoiceDistSoaDmu);

            computeSizeSegmentProbabilities(sizeProbs[segmentIndex], dcSizeArray[segmentIndex]);

        }

    }

    private void updateSchoolSoaProbabilities(HashMap<String, Integer> segmentNameIndexMap,
            DestChoiceSize dcSizeObj, double[][] tazDistExpUtils, double[][] sizeProbs,
            double[][] tazDistProbs)
    {

        double[][] dcSizeArray = dcSizeObj.getDcSizeArray();

        double[] tempExpUtils = new double[tazDistExpUtils.length];

        // compute an array of SOA probabilities for each segment
        for (String segmentName : segmentNameIndexMap.keySet())
        {

            // compute the TAZ size values from the mgra values and the
            // correspondence between mgras and tazs.
            int segmentIndex = segmentNameIndexMap.get(segmentName);
            double[] tazSize = computeTazSize(dcSizeArray[segmentIndex]);

            // compute the taz dist probabilities from the exponentiated
            // utilities for this segmnet and the taz size terms
            for (int i = 0; i < tazDistExpUtils.length; i++)
            {

                // compute the final exponentiated utilities by multiplying with
                // taz size, and accumulate total exponentiated utility.
                double totalExpUtil = 0;
                for (int j = 0; j < tempExpUtils.length; j++)
                {
                    tempExpUtils[j] = tazDistExpUtils[i][j] * tazSize[j + 1];
                    totalExpUtil += tempExpUtils[j];
                }

                if (totalExpUtil > 0)
                {

                    // compute the SOA cumulative probabilities
                    tazDistProbs[i][0] = tempExpUtils[0] / totalExpUtil;
                    for (int j = 1; j < tempExpUtils.length - 1; j++)
                    {
                        double prob = tempExpUtils[j] / totalExpUtil;
                        tazDistProbs[i][j] = tazDistProbs[i][j - 1] + prob;
                    }
                    tazDistProbs[i][tempExpUtils.length - 1] = 1.0;

                }

            }

            computeSizeSegmentProbabilities(sizeProbs, dcSizeArray[segmentIndex]);

        }

    }

    private double[][] computeTazDistanceExponentiatedUtilities(
            DestChoiceTwoStageSoaProbabilitiesCalculator locChoiceSoaDistProbsObject)
    {

        double[][] tazDistExpUtils = locChoiceSoaDistProbsObject
                .computeDistanceUtilities(locChoiceDistSoaDmu);
        for (int i = 0; i < tazDistExpUtils.length; i++)
            for (int j = 0; j < tazDistExpUtils[i].length; j++)
            {
                if (tazDistExpUtils[i][j] < -500) tazDistExpUtils[i][j] = 0;
                else tazDistExpUtils[i][j] = Math.exp(tazDistExpUtils[i][j]);
            }

        return tazDistExpUtils;

    }

    private double[] computeTazSize(double[] size)
    {

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

    private void computeSizeSegmentProbabilities(double[][] sizeProbs, double[] size)
    {

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
                } else if (sizeProbs[taz - 1] == null)
                {
                    sizeProbs[taz - 1] = new double[0];
                }
            }

        }

    }

}
