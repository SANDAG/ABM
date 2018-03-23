package org.sandag.abm.ctramp;

import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Stop;
import org.sandag.abm.ctramp.StopLocationDMU;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * This class will be used for determining the trip departure time for outbound
 * stops, trip arrival time for inbound stops, location of stops, and trip mode
 * for trips between stops on individual mandatory, individual non-mandatory and
 * joint tours.
 * 
 * @author Jim Hicks
 * @version Oct 2010
 */
public class IntermediateStopChoiceModels
        implements Serializable
{

    private transient Logger                   logger                                              = Logger.getLogger(IntermediateStopChoiceModels.class);
    private transient Logger                   slcLogger                                           = Logger.getLogger("slcLogger");
    private transient Logger                   slcSoaLogger                                        = Logger.getLogger("slcSoaLogger");
    private transient Logger                   smcLogger                                           = Logger.getLogger("tripMcLog");
    private transient Logger                   tripDepartLogger                                    = Logger.getLogger("tripDepartLog");
    private transient Logger                   parkLocLogger                                       = Logger.getLogger("parkLocLog");

    private static final String                USE_NEW_SOA_METHOD_PROPERTY_KEY                     = "slc.use.new.soa";

    public static final String                 PROPERTIES_UEC_TRIP_MODE_CHOICE                     = "tripModeChoice.uec.file";

    private static final String                PROPERTIES_UEC_SLC_CHOICE                           = "slc.uec.file";
    private static final String                PROPERTIES_UEC_SLC_DATA_PAGE                        = "slc.uec.data.page";
    private static final String                PROPERTIES_UEC_MAND_SLC_MODEL_PAGE                  = "slc.mandatory.uec.model.page";
    private static final String                PROPERTIES_UEC_MAINT_SLC_MODEL_PAGE                 = "slc.maintenance.uec.model.page";
    private static final String                PROPERTIES_UEC_DISCR_SLC_MODEL_PAGE                 = "slc.discretionary.uec.model.page";

    public static final String                 PROPERTIES_UEC_SLC_SOA_CHOICE                       = "slc.soa.uec.file";
    public static final String                 PROPERTIES_UEC_SLC_SOA_DISTANCE_UTILITY             = "auto.slc.soa.distance.uec.file";
    public static final String                 PROPERTIES_UEC_SLC_SOA_DISTANCE_DATA_PAGE           = "auto.slc.soa.distance.data.page";
    public static final String                 PROPERTIES_UEC_SLC_SOA_DISTANCE_MODEL_PAGE          = "auto.slc.soa.distance.model.page";

    private static final String                PROPERTIES_UEC_STOP_LOCATION_SIZE                   = "slc.soa.size.uec.file";
    private static final String                PROPERTIES_UEC_STOP_LOCATION_SIZE_DATA              = "slc.soa.size.uec.data.page";
    private static final String                PROPERTIES_UEC_STOP_LOCATION_SIZE_MODEL             = "slc.soa.size.uec.model.page";

    private static final String                PROPERTIES_UEC_PARKING_LOCATION_CHOICE              = "plc.uec.file";
    private static final String                PROPERTIES_UEC_PLC_DATA_PAGE                        = "plc.uec.data.page";
    private static final String                PROPERTIES_UEC_PLC_MODEL_PAGE                       = "plc.uec.model.page";

    private static final String                PROPERTIES_UEC_PARKING_LOCATION_CHOICE_ALTERNATIVES = "plc.alts.corresp.file";

    public static final int                    WORK_SHEET                                          = 1;
    public static final int                    UNIVERSITY_SHEET                                    = 2;
    public static final int                    SCHOOL_SHEET                                        = 3;
    public static final int                    MAINTENANCE_SHEET                                   = 4;
    public static final int                    DISCRETIONARY_SHEET                                 = 5;
    public static final int                    SUBTOUR_SHEET                                       = 6;
    public static final int[]                  MC_PURPOSE_SHEET_INDICES                            = {
            -1, WORK_SHEET, UNIVERSITY_SHEET, SCHOOL_SHEET, MAINTENANCE_SHEET, MAINTENANCE_SHEET,
            MAINTENANCE_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET,
            SUBTOUR_SHEET                                                                          };

    public static final int                    WORK_CATEGORY                                       = 0;
    public static final int                    UNIVERSITY_CATEGORY                                 = 1;
    public static final int                    SCHOOL_CATEGORY                                     = 2;
    public static final int                    MAINTENANCE_CATEGORY                                = 3;
    public static final int                    DISCRETIONARY_CATEGORY                              = 4;
    public static final int                    SUBTOUR_CATEGORY                                    = 5;
    public static final String[]               PURPOSE_CATEGORY_LABELS                             = {
            "work", "university", "school", "maintenance", "discretionary", "subtour"              };
    public static final int[]                  PURPOSE_CATEGORIES                                  = {
            -1, WORK_CATEGORY, UNIVERSITY_CATEGORY, SCHOOL_CATEGORY, MAINTENANCE_CATEGORY,
            MAINTENANCE_CATEGORY, MAINTENANCE_CATEGORY, DISCRETIONARY_CATEGORY,
            DISCRETIONARY_CATEGORY, DISCRETIONARY_CATEGORY, SUBTOUR_CATEGORY                       };

    private static final String                PARK_MGRA_COLUMN                                    = "mgra";
    private static final String                PARK_AREA_COLUMN                                    = "parkarea";
    private static final int                   MAX_PLC_SAMPLE_SIZE                                 = 620;

    private static final int                   WORK_STOP_PURPOSE_INDEX                             = 1;
    private static final int                   UNIV_STOP_PURPOSE_INDEX                             = 2;
    private static final int                   ESCORT_STOP_PURPOSE_INDEX                           = 4;
    private static final int                   SHOP_STOP_PURPOSE_INDEX                             = 5;
    private static final int                   MAINT_STOP_PURPOSE_INDEX                            = 6;
    private static final int                   EAT_OUT_STOP_PURPOSE_INDEX                          = 7;
    private static final int                   VISIT_STOP_PURPOSE_INDEX                            = 8;
    private static final int                   DISCR_STOP_PURPOSE_INDEX                            = 9;

    private static final int                   OTHER_STOP_LOC_SOA_SHEET_INDEX                      = 2;
    private static final int                   WALK_STOP_LOC_SOA_SHEET_INDEX                       = 3;
    private static final int                   BIKE_STOP_LOC_SOA_SHEET_INDEX                       = 4;
    private static final int                   MAX_STOP_LOC_SOA_SHEET_INDEX                        = 4;

    private static final int                   WORK_STOP_PURPOSE_SOA_SIZE_INDEX                    = 0;
    private static final int                   UNIV_STOP_PURPOSE_SOA_SIZE_INDEX                    = 1;
    private static final int                   ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX                = 2;
    private static final int                   ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX               = 3;
    private static final int                   ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX               = 4;
    private static final int                   ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX               = 5;
    private static final int                   ESCORT_PS_GS_STOP_PURPOSE_SOA_SIZE_INDEX            = 6;
    private static final int                   ESCORT_PS_HS_STOP_PURPOSE_SOA_SIZE_INDEX            = 7;
    private static final int                   ESCORT_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX            = 8;
    private static final int                   ESCORT_PS_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX         = 9;
    private static final int                   SHOP_STOP_PURPOSE_SOA_SIZE_INDEX                    = 10;
    private static final int                   MAINT_STOP_PURPOSE_SOA_SIZE_INDEX                   = 11;
    private static final int                   EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX                 = 12;
    private static final int                   VISIT_STOP_PURPOSE_SOA_SIZE_INDEX                   = 13;
    private static final int                   DISCR_STOP_PURPOSE_SOA_SIZE_INDEX                   = 14;

    public static final int[]                  SLC_SIZE_SEGMENT_INDICES                            = {
            WORK_STOP_PURPOSE_SOA_SIZE_INDEX, UNIV_STOP_PURPOSE_SOA_SIZE_INDEX,
            ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX, ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX,
            ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX, ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX,
            ESCORT_PS_GS_STOP_PURPOSE_SOA_SIZE_INDEX, ESCORT_PS_HS_STOP_PURPOSE_SOA_SIZE_INDEX,
            ESCORT_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX, ESCORT_PS_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX,
            SHOP_STOP_PURPOSE_SOA_SIZE_INDEX, MAINT_STOP_PURPOSE_SOA_SIZE_INDEX,
            EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX, VISIT_STOP_PURPOSE_SOA_SIZE_INDEX,
            DISCR_STOP_PURPOSE_SOA_SIZE_INDEX                                                      };

    public static final String[]               SLC_SIZE_SEGMENT_NAMES                              = {
            "work", "univ", "escort_0", "escort_ps", "escort_gs", "escort_hs", "escort_ps_gs",
            "escort_ps_hs", "escort_gs_hs", "escort_ps_gs_hs", "shop", "maint", "eatout", "visit",
            "discr"                                                                                };

    private static final int                   MAND_SLC_MODEL_INDEX                                = 0;
    private static final int                   MAINT_SLC_MODEL_INDEX                               = 1;
    private static final int                   DISCR_SLC_MODEL_INDEX                               = 2;

    private boolean[]                          sampleAvailability;
    private int[]                              inSample;
    private boolean[]                          soaAvailability;
    private int[]                              soaSample;
    private boolean[]                          soaAvailabilityBackup;
    private int[]                              soaSampleBackup;
    private int[]                              finalSample;
    private double[]                           sampleCorrectionFactors;
    private double[]                           tripModeChoiceLogsums;
    private boolean[]                          sampleMgraInBoardingTapShed;
    private boolean[]                          sampleMgraInAlightingTapShed;
    private boolean                            earlierTripWasLocatedInAlightingTapShed;

    private double[]                           tourOrigToAllMgraDistances;
    private double[]                           tourDestToAllMgraDistances;
    private double[]                           ikDistance;
    private double[]                           kjDistance;
    private double[]                           okDistance;
    private double[]                           kdDistance;

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private ModelStructure                     modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;

    private int                                sampleSize;
    private HashMap<Integer, Integer>          altFreqMap;
    private StopLocationDMU                    stopLocDmuObj;
    private TripModeChoiceDMU                  mcDmuObject;
    private ParkingChoiceDMU                   parkingChoiceDmuObj;

    private double[][]                         slcSizeTerms;
    private int[][]                            slcSizeSample;
    private boolean[][]                        slcSizeAvailable;

    private double[]                           distanceFromStopOrigToAllMgras;
    private double[]                           distanceToFinalDestFromAllMgras;
    
	private final BikeLogsum bls;
	private BikeLogsumSegment segment;

    private double[]                           bikeLogsumFromStopOrigToAllMgras;
    private double[]                           bikeLogsumToFinalDestFromAllMgras;

    private double[][]                         mcCumProbsSegmentIk;
    private double[][]                         mcCumProbsSegmentKj;

    private int[][][]                          segmentIkBestTapPairs;
    private int[][][]                          segmentKjBestTapPairs;

    private ChoiceModelApplication[]           mcModelArray;
    private ChoiceModelApplication[]           slcSoaModel;
    private ChoiceModelApplication[]           slcModelArray;
    private ChoiceModelApplication             plcModel;

    private int[]                              altMgraIndices;
    private double[]                           altOsDistances;
    private double[]                           altSdDistances;
    private boolean[]                          altParkAvail;
    private int[]                              altParkSample;

    private int                                numAltsInSample;
    private int                                maxAltsInSample;

    private TableDataSet                       plcAltsTable;
    private HashMap<Integer, Integer>          mgraAltLocationIndex;
    private HashMap<Integer, Integer>          mgraAltParkArea;
    private int[]                              parkMgras;
    private int[]                              parkAreas;

    private int[]                              mgraParkArea;
    private int[]                              numfreehrs;
    private int[]                              hstallsoth;
    private int[]                              hstallssam;
    private float[]                            hparkcost;
    private int[]                              dstallsoth;
    private int[]                              dstallssam;
    private float[]                            dparkcost;
    private int[]                              mstallsoth;
    private int[]                              mstallssam;
    private float[]                            mparkcost;

    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;

    private double[]                           altParkingCostsM;
    private double[]                           altParkingCostsD;
    private double[]                           altParkingCostsH;
    private int[]                              altMstallsoth;
    private int[]                              altMstallssam;
    private float[]                            altMparkcost;
    private int[]                              altDstallsoth;
    private int[]                              altDstallssam;
    private float[]                            altDparkcost;
    private int[]                              altHstallsoth;
    private int[]                              altHstallssam;
    private float[]                            altHparkcost;
    private int[]                              altNumfreehrs;

    private HashMap<String, Integer>           sizeSegmentNameIndexMap;

    private StopDepartArrivePeriodModel        stopTodModel;

    private int                                availAltsToLog                                      = 55;    
                                                                                                                                                           // larger
                                                                                                                                                           // than
                                                                                                                                                           // number
                                                                                                                                                           // of
                                                                                                                                                           // mode
                                                                                                                                                           // alts
                                                                                                                                                           // to
                                                                                                                                                           // suppress
                                                                                                                                                           // this
                                                                                                                                                           // logging
    // private int availAltsToLog = 5;

    // set this constant for checking the number of times depart/arrive period
    // selection is made so that no infinite loop occurs.
    private static final int                   MAX_INVALID_FIRST_ARRIVAL_COUNT                     = 100;

    public static final int                    NUM_CPU_TIME_VALUES                                 = 9;
    private long                               soaAutoTime;
    private long                               soaOtherTime;
    private long                               slsTime;
    private long                               sldTime;
    private long                               slcTime;
    private long                               todTime;
    private long                               smcTime;
    private long[]                             hhTimes                                             = new long[NUM_CPU_TIME_VALUES];

    private DestChoiceTwoStageModel            dcTwoStageModelObject;

    private boolean                            useNewSoaMethod;

    private String                             loggerSeparator                                     = "";

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
     * @param modelStructure
     *            - holds the model structure info
     */
    public IntermediateStopChoiceModels(HashMap<String, String> propertyMap,
            ModelStructure myModelStructure, CtrampDmuFactoryIf dmuFactory,
            McLogsumsCalculator myLogsumHelper)
    {

        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        mgraParkArea = mgraManager.getMgraParkAreas();
        numfreehrs = mgraManager.getNumFreeHours();
        lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();
        mstallsoth = mgraManager.getMStallsOth();
        mstallssam = mgraManager.getMStallsSam();
        mparkcost = mgraManager.getMParkCost();
        dstallsoth = mgraManager.getDStallsOth();
        dstallssam = mgraManager.getDStallsSam();
        dparkcost = mgraManager.getDParkCost();
        hstallsoth = mgraManager.getHStallsOth();
        hstallssam = mgraManager.getHStallsSam();
        hparkcost = mgraManager.getHParkCost();

        modelStructure = myModelStructure;
        logsumHelper = myLogsumHelper;

        setupStopLocationChoiceModels(propertyMap, dmuFactory);
        setupParkingLocationModel(propertyMap, dmuFactory);
        
        bls = BikeLogsum.getBikeLogsum(propertyMap);

    }

    private void setupStopLocationChoiceModels(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up stop location choice models."));

        useNewSoaMethod = Util.getBooleanValueFromPropertyMap(propertyMap,
                USE_NEW_SOA_METHOD_PROPERTY_KEY);

        stopLocDmuObj = dmuFactory.getStopLocationDMU();

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String slcSoaUecFile = propertyMap.get(PROPERTIES_UEC_SLC_SOA_CHOICE);
        slcSoaUecFile = uecPath + slcSoaUecFile;

        String slcUecFile = propertyMap.get(PROPERTIES_UEC_SLC_CHOICE);
        slcUecFile = uecPath + slcUecFile;

        slcSoaModel = new ChoiceModelApplication[MAX_STOP_LOC_SOA_SHEET_INDEX + 1];
        slcSoaModel[OTHER_STOP_LOC_SOA_SHEET_INDEX] = new ChoiceModelApplication(slcSoaUecFile,
                OTHER_STOP_LOC_SOA_SHEET_INDEX, 0, propertyMap, (VariableTable) stopLocDmuObj);
        slcSoaModel[WALK_STOP_LOC_SOA_SHEET_INDEX] = new ChoiceModelApplication(slcSoaUecFile,
                WALK_STOP_LOC_SOA_SHEET_INDEX, 0, propertyMap, (VariableTable) stopLocDmuObj);
        slcSoaModel[BIKE_STOP_LOC_SOA_SHEET_INDEX] = new ChoiceModelApplication(slcSoaUecFile,
                BIKE_STOP_LOC_SOA_SHEET_INDEX, 0, propertyMap, (VariableTable) stopLocDmuObj);

        int numSlcSoaAlternatives = slcSoaModel[OTHER_STOP_LOC_SOA_SHEET_INDEX]
                .getNumberOfAlternatives();
        stopLocDmuObj = dmuFactory.getStopLocationDMU();

        sizeSegmentNameIndexMap = new HashMap<String, Integer>();
        for (int k = 0; k < SLC_SIZE_SEGMENT_INDICES.length; k++)
        {
            sizeSegmentNameIndexMap.put(SLC_SIZE_SEGMENT_NAMES[k], k);
            sizeSegmentNameIndexMap.put(SLC_SIZE_SEGMENT_NAMES[k], SLC_SIZE_SEGMENT_INDICES[k]);
        }

        // set the second argument to a positive, non-zero mgra value to get
        // logging for the size term calculation for the specified mgra.
        slcSizeTerms = calculateLnSlcSizeTerms(propertyMap, -1);

        String mcUecFile = propertyMap.get(PROPERTIES_UEC_TRIP_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        mcDmuObject = dmuFactory.getTripModeChoiceDMU();

        // logsumHelper.setupSkimCalculators(propertyMap);
        anm = logsumHelper.getAnmSkimCalculator();
        mcDmuObject.setParkingCostInfo(mgraParkArea, lsWgtAvgCostM, lsWgtAvgCostD, lsWgtAvgCostH);

        mcModelArray = new ChoiceModelApplication[5 + 1];
        mcModelArray[WORK_CATEGORY] = new ChoiceModelApplication(mcUecFile, WORK_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[UNIVERSITY_CATEGORY] = new ChoiceModelApplication(mcUecFile, UNIVERSITY_SHEET,
                0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[SCHOOL_CATEGORY] = new ChoiceModelApplication(mcUecFile, SCHOOL_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[MAINTENANCE_CATEGORY] = new ChoiceModelApplication(mcUecFile,
                MAINTENANCE_SHEET, 0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[DISCRETIONARY_CATEGORY] = new ChoiceModelApplication(mcUecFile,
                DISCRETIONARY_SHEET, 0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[SUBTOUR_CATEGORY] = new ChoiceModelApplication(mcUecFile, SUBTOUR_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);

        // set up the stop location choice model object
        int slcDataPage = Integer.parseInt(propertyMap.get(PROPERTIES_UEC_SLC_DATA_PAGE));
        int slcMandModelPage = Integer
                .parseInt(propertyMap.get(PROPERTIES_UEC_MAND_SLC_MODEL_PAGE));
        int slcMaintModelPage = Integer.parseInt(propertyMap
                .get(PROPERTIES_UEC_MAINT_SLC_MODEL_PAGE));
        int slcDiscrModelPage = Integer.parseInt(propertyMap
                .get(PROPERTIES_UEC_DISCR_SLC_MODEL_PAGE));
        slcModelArray = new ChoiceModelApplication[3];
        slcModelArray[MAND_SLC_MODEL_INDEX] = new ChoiceModelApplication(slcUecFile,
                slcMandModelPage, slcDataPage, propertyMap, (VariableTable) stopLocDmuObj);
        slcModelArray[MAINT_SLC_MODEL_INDEX] = new ChoiceModelApplication(slcUecFile,
                slcMaintModelPage, slcDataPage, propertyMap, (VariableTable) stopLocDmuObj);
        slcModelArray[DISCR_SLC_MODEL_INDEX] = new ChoiceModelApplication(slcUecFile,
                slcDiscrModelPage, slcDataPage, propertyMap, (VariableTable) stopLocDmuObj);

        sampleSize = slcModelArray[MAND_SLC_MODEL_INDEX].getNumberOfAlternatives();
        altFreqMap = new HashMap<Integer, Integer>(sampleSize);
        finalSample = new int[sampleSize + 1];
        sampleCorrectionFactors = new double[sampleSize + 1];

        // decalre the arrays for storing stop location choice ik and kj segment
        // mode choice probability arrays
        mcCumProbsSegmentIk = new double[sampleSize + 1][];
        mcCumProbsSegmentKj = new double[sampleSize + 1][];

        // decalre the arrays for storing stop location choice ik and kj segment
        // best tap pair arrays
        segmentIkBestTapPairs = new int[sampleSize + 1][][];
        segmentKjBestTapPairs = new int[sampleSize + 1][][];

        // declare the arrays that holds ik and kj segment logsum values for
        // each location choice sample alternative
        tripModeChoiceLogsums = new double[sampleSize + 1];

        // declare the arrays that holds ik and kj distance values for each
        // location choice sample alternative
        // these are set as stop location dmu attributes for stop orig to stop
        // alt, and stop alt to half-tour final dest.
        ikDistance = new double[sampleSize + 1];
        kjDistance = new double[sampleSize + 1];

        // declare the arrays that holds ik and kj distance values for each
        // location choice sample alternative
        // these are set as stop location dmu attributes for tour orig to stop
        // alt, and stop alt to tour dest.
        okDistance = new double[sampleSize + 1];
        kdDistance = new double[sampleSize + 1];

        // this array has elements with values of 0 if utility is not to be
        // computed for alternative, or 1 if utility is to be computed.
        inSample = new int[sampleSize + 1];

        // this array has elements that are boolean that set availability of
        // alternative - unavailable altrnatives do not get utility computed.
        sampleAvailability = new boolean[sampleSize + 1];

        soaSample = new int[numSlcSoaAlternatives + 1];
        soaAvailability = new boolean[numSlcSoaAlternatives + 1];
        soaSampleBackup = new int[numSlcSoaAlternatives + 1];
        soaAvailabilityBackup = new boolean[numSlcSoaAlternatives + 1];

        sampleMgraInBoardingTapShed = new boolean[mgraManager.getMaxMgra() + 1];
        sampleMgraInAlightingTapShed = new boolean[mgraManager.getMaxMgra() + 1];

        distanceFromStopOrigToAllMgras = new double[mgraManager.getMaxMgra() + 1];
        distanceToFinalDestFromAllMgras = new double[mgraManager.getMaxMgra() + 1];

        bikeLogsumFromStopOrigToAllMgras = new double[mgraManager.getMaxMgra() + 1];
        bikeLogsumToFinalDestFromAllMgras = new double[mgraManager.getMaxMgra() + 1];

        tourOrigToAllMgraDistances = new double[mgraManager.getMaxMgra() + 1];
        tourDestToAllMgraDistances = new double[mgraManager.getMaxMgra() + 1];

        // create the array of 1s for MGRAs that have a non-empty set of TAPs
        // within walk egress distance of them
        // for the setting walk transit available for teh stop location
        // alternatives.
        // createWalkTransitAvailableArray();

        setupTripDepartTimeModel(propertyMap, dmuFactory);

        loggerSeparator += "-";

    }

    public void setupSlcDistanceBaseSoaModel(HashMap<String, String> propertyMap,
            double[][] soaExpUtils, double[][][] soaSizeProbs, double[][] soaTazSize)
    {

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String slcSoaDistanceUecFile = propertyMap.get(PROPERTIES_UEC_SLC_SOA_DISTANCE_UTILITY);
        slcSoaDistanceUecFile = uecPath + slcSoaDistanceUecFile;

        dcTwoStageModelObject = new DestChoiceTwoStageModel(propertyMap, sampleSize);
        dcTwoStageModelObject.setSlcSoaProbsAndUtils(soaExpUtils, soaSizeProbs, soaTazSize);

    }

    private void setupTripDepartTimeModel(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {
        stopTodModel = new StopDepartArrivePeriodModel(propertyMap, modelStructure);
    }

    private void setupParkingLocationModel(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

        logger.info("setting up parking location choice models.");

        // locate the UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String plcUecFile = propertyMap.get(PROPERTIES_UEC_PARKING_LOCATION_CHOICE);
        plcUecFile = uecPath + plcUecFile;

        int plcDataPage = Integer.parseInt(propertyMap.get(PROPERTIES_UEC_PLC_DATA_PAGE));
        int plcModelPage = Integer.parseInt(propertyMap.get(PROPERTIES_UEC_PLC_MODEL_PAGE));

        altMgraIndices = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altOsDistances = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altSdDistances = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsM = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsD = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsH = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altMstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altMstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altMparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altDstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altDstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altDparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altHstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altHstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altHparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altNumfreehrs = new int[MAX_PLC_SAMPLE_SIZE + 1];

        altParkAvail = new boolean[MAX_PLC_SAMPLE_SIZE + 1];
        altParkSample = new int[MAX_PLC_SAMPLE_SIZE + 1];

        parkingChoiceDmuObj = dmuFactory.getParkingChoiceDMU();

        plcModel = new ChoiceModelApplication(plcUecFile, plcModelPage, plcDataPage, propertyMap,
                (VariableTable) parkingChoiceDmuObj);

        // read the parking choice alternatives data file to get alternatives
        // names
        String plcAltsFile = propertyMap.get(PROPERTIES_UEC_PARKING_LOCATION_CHOICE_ALTERNATIVES);
        plcAltsFile = uecPath + plcAltsFile;

        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            plcAltsTable = reader.readFile(new File(plcAltsFile));
        } catch (IOException e)
        {
            logger.error("problem reading table of cbd zones for parking location choice model.", e);
            System.exit(1);
        }

        parkMgras = plcAltsTable.getColumnAsInt(PARK_MGRA_COLUMN);
        parkAreas = plcAltsTable.getColumnAsInt(PARK_AREA_COLUMN);

        parkingChoiceDmuObj.setParkAreaMgraArray(parkMgras);
        parkingChoiceDmuObj.setSampleIndicesArray(altMgraIndices);
        parkingChoiceDmuObj.setDistancesOrigAlt(altOsDistances);
        parkingChoiceDmuObj.setDistancesAltDest(altSdDistances);
        parkingChoiceDmuObj.setParkingCostsM(altParkingCostsM);
        parkingChoiceDmuObj.setMstallsoth(altMstallsoth);
        parkingChoiceDmuObj.setMstallssam(altMstallssam);
        parkingChoiceDmuObj.setMparkCost(altMparkcost);
        parkingChoiceDmuObj.setDstallsoth(altDstallsoth);
        parkingChoiceDmuObj.setDstallssam(altDstallssam);
        parkingChoiceDmuObj.setDparkCost(altDparkcost);
        parkingChoiceDmuObj.setHstallsoth(altHstallsoth);
        parkingChoiceDmuObj.setHstallssam(altHstallssam);
        parkingChoiceDmuObj.setHparkCost(altHparkcost);
        parkingChoiceDmuObj.setNumfreehrs(altNumfreehrs);

        mgraAltLocationIndex = new HashMap<Integer, Integer>();
        mgraAltParkArea = new HashMap<Integer, Integer>();

        for (int i = 0; i < parkMgras.length; i++)
        {
            mgraAltLocationIndex.put(parkMgras[i], i);
            mgraAltParkArea.put(parkMgras[i], parkAreas[i]);
        }

    }

    private double[][] calculateLnSlcSizeTerms(HashMap<String, String> rbMap, int logMgra)
    {

        logger.info("calculating Stop Location SOA Size Terms");

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String slcSizeUecFile = rbMap.get(PROPERTIES_UEC_STOP_LOCATION_SIZE);
        slcSizeUecFile = uecPath + slcSizeUecFile;
        int slcSizeUecData = Integer.parseInt(rbMap.get(PROPERTIES_UEC_STOP_LOCATION_SIZE_DATA));
        int slcSizeUecModel = Integer.parseInt(rbMap.get(PROPERTIES_UEC_STOP_LOCATION_SIZE_MODEL));

        IndexValues iv = new IndexValues();
        UtilityExpressionCalculator slcSizeUec = new UtilityExpressionCalculator(new File(
                slcSizeUecFile), slcSizeUecModel, slcSizeUecData, rbMap, null);

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int maxMgra = mgraManager.getMaxMgra();
        int numSizeSegments = slcSizeUec.getNumberOfAlternatives();

        // create the array for storing logged size term values - to be returned
        // by this method
        double[][] lnSlcSoaSize = new double[numSizeSegments][maxMgra + 1];
        slcSizeSample = new int[numSizeSegments][maxMgra + 1];
        slcSizeAvailable = new boolean[numSizeSegments][maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            iv.setZoneIndex(mgra);
            double[] size = slcSizeUec.solve(iv, null, null);

            // if a logMgra values > 0 was specified, log the size term utility
            // calculation for that mgra
            if (mgra == logMgra)
                slcSizeUec.logAnswersArray(slcSoaLogger, "Stop Location SOA Size Terms, MGRA = "
                        + mgra);

            // store the logged size terms
            for (int i = 0; i < numSizeSegments; i++)
            {
                lnSlcSoaSize[i][mgra] = Math.log(size[i] + 1);
                if (size[i] > 0)
                {
                    slcSizeSample[i][mgra] = 1;
                    slcSizeAvailable[i][mgra] = true;
                }
            }

        }

        return lnSlcSoaSize;

    }

    private double[] getLnSlcSizeTermsForStopPurpose(int stopPurpose, Household hh)
    {

        double[] lnSlcSizeTerms = null;

        switch (stopPurpose)
        {

            case WORK_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[WORK_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[WORK_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[WORK_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case UNIV_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[UNIV_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[UNIV_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[UNIV_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case ESCORT_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = getLnSlcSizeTermsForEscortStopPurpose(hh);
                break;
            case SHOP_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[SHOP_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[SHOP_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[SHOP_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case MAINT_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[MAINT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[MAINT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[MAINT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case EAT_OUT_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case VISIT_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[VISIT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[VISIT_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[VISIT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case DISCR_STOP_PURPOSE_INDEX:
                lnSlcSizeTerms = slcSizeTerms[DISCR_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaSample = slcSizeSample[DISCR_STOP_PURPOSE_SOA_SIZE_INDEX];
                soaAvailability = slcSizeAvailable[DISCR_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
        }

        // save backup arrays with oroginal availability and sample values.
        // the procedure to generate availabilty for transit tours overwrites
        // the arrays used by the UECs,
        // so they need to be restored after that happens.
        for (int i = 0; i < soaSample.length; i++)
        {
            soaSampleBackup[i] = soaSample[i];
            soaAvailabilityBackup[i] = soaAvailability[i];
        }

        return lnSlcSizeTerms;

    }

    private double[] getLnSlcSizeTermsForEscortStopPurpose(Household hh)
    {

        double[] lnSlcSizeTermsForEscort = null;

        // set booleans for presence of preschool, grade school, and high school
        // students in the hh
        boolean psInHh = hh.getNumPreschool() > 0;
        boolean gsInHh = hh.getNumGradeSchoolStudents() > 0;
        boolean hsInHh = hh.getNumHighSchoolStudents() > 0;

        if (!psInHh && !gsInHh && !hsInHh)
        {
            // if hh has no preschool, grade school or high school children, set
            // the array to that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (psInHh && !gsInHh && !hsInHh)
        {
            // if hh has a preschool child and no gs or hs, set the array to
            // that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (!psInHh && gsInHh && !hsInHh)
        {
            // if hh has a grade school child and no ps or hs, set the array to
            // that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (!psInHh && !gsInHh && hsInHh)
        {
            // if hh has a high school child and no ps or gs, set the array to
            // that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (psInHh && gsInHh && !hsInHh)
        {
            // if hh has a preschool and a grade school child and no hs, set the
            // array to that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_PS_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_PS_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_PS_GS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (psInHh && !gsInHh && hsInHh)
        {
            // if hh has a preschool and a high school child and no gs, set the
            // array to that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_PS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_PS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_PS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (!psInHh && gsInHh && hsInHh)
        {
            // if hh has a grade school and a high school child and no ps, set
            // the array to that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
        } else if (psInHh && gsInHh && hsInHh)
        {
            // if hh has a preschool a grade school and a high school child, set
            // the array to that specific size term field
            lnSlcSizeTermsForEscort = slcSizeTerms[ESCORT_PS_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaSample = slcSizeSample[ESCORT_PS_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
            soaAvailability = slcSizeAvailable[ESCORT_PS_GS_HS_STOP_PURPOSE_SOA_SIZE_INDEX];
        }

        return lnSlcSizeTermsForEscort;
    }

    public void applyModel(Household household, boolean withTiming)
    {

        if (withTiming) zeroOutCpuTimes();

        if (household.getDebugChoiceModels())
        {
            slcLogger.info("applying SLC model for hhid=" + household.getHhId());
        }

        // get this household's person array
        Person[] personArray = household.getPersons();

        // loop through the person array (1-based)
        for (int j = 1; j < personArray.length; ++j)
        {

            ArrayList<Tour> tours = new ArrayList<Tour>();

            Person person = personArray[j];

            // apply stop location and mode choice for all individual tours.
            tours.addAll(person.getListOfWorkTours());
            tours.addAll(person.getListOfSchoolTours());
            tours.addAll(person.getListOfIndividualNonMandatoryTours());
            tours.addAll(person.getListOfAtWorkSubtours());

            for (Tour tour : tours)
            {

                if (withTiming) applyForOutboundStopsWithTiming(tour, person, household);
                else applyForOutboundStops(tour, person, household);

                if (withTiming) applyForInboundStopsWithTiming(tour, person, household);
                else applyForInboundStops(tour, person, household);

            } // tour loop

        } // j (person loop)

        // apply stop location and mode choice for all joint tours.
        Tour[] jointTours = household.getJointTourArray();
        if (jointTours != null)
        {

            for (Tour tour : jointTours)
            {

                if (withTiming) applyForOutboundStopsWithTiming(tour, null, household);
                else applyForOutboundStops(tour, null, household);

                if (withTiming) applyForInboundStopsWithTiming(tour, null, household);
                else applyForInboundStops(tour, null, household);

            } // tour loop

        }

        household.setStlRandomCount(household.getHhRandomCount());

    }

    private void applyForOutboundStops(Tour tour, Person person, Household household)
    {

        Stop[] stops = tour.getOutboundStops();

        // select trip depart periods for outbound stops
        if (stops != null)
        {
            setOutboundTripDepartTimes(stops);
        }

        int origMgra = tour.getTourOrigMgra();
        int destMgra = tour.getTourDestMgra();

        applySlcModel(household, person, tour, stops, origMgra, destMgra, false);

    }

    private void applyForOutboundStopsWithTiming(Tour tour, Person person, Household household)
    {

        long check = System.nanoTime();

        Stop[] stops = tour.getOutboundStops();

        // select trip depart periods for outbound stops
        if (stops != null)
        {
            setOutboundTripDepartTimes(stops);
        }

        int origMgra = tour.getTourOrigMgra();
        int destMgra = tour.getTourDestMgra();

        todTime += (System.nanoTime() - check);

        applySlcModelWithTiming(household, person, tour, stops, origMgra, destMgra, false);

    }

    private void applyForInboundStops(Tour tour, Person person, Household household)
    {

        Stop[] stops = tour.getInboundStops();

        // select trip arrive periods for inbound stops
        if (stops != null)
        {
            int lastOutboundTripDeparts = -1;

            // get the outbound stops array - note, if there were no outbound
            // stops for half-tour, one stop object would have been generated
            // to hold information about the half-tour trip, so this array
            // should never be null.
            Stop[] obStops = tour.getOutboundStops();
            if (obStops == null)
            {
                logger.error("error getting last outbound stop object for setting lastOutboundTripDeparts attribute for inbound stop arrival time choice.");
                logger.error("HHID=" + household.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                throw new RuntimeException();
            } else
            {
                Stop lastStop = obStops[obStops.length - 1];
                lastOutboundTripDeparts = lastStop.getStopPeriod();
            }

            setInboundTripDepartTimes(stops, lastOutboundTripDeparts);
        }

        int origMgra = tour.getTourDestMgra();
        int destMgra = tour.getTourOrigMgra();

        applySlcModel(household, person, tour, stops, origMgra, destMgra, true);

    }

    private void applyForInboundStopsWithTiming(Tour tour, Person person, Household household)
    {

        long check = System.nanoTime();

        Stop[] stops = tour.getInboundStops();

        // select trip arrive periods for inbound stops
        if (stops != null)
        {
            int lastOutboundTripDeparts = -1;

            // get the outbound stops array - note, if there were no outbound
            // stops for half-tour, one stop object would have been generated
            // to hold information about the half-tour trip, so this array
            // should never be null.
            Stop[] obStops = tour.getOutboundStops();
            if (obStops == null)
            {
                logger.error("error getting last outbound stop object for setting lastOutboundTripDeparts attribute for inbound stop arrival time choice.");
                logger.error("HHID=" + household.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                throw new RuntimeException();
            } else
            {
                Stop lastStop = obStops[obStops.length - 1];
                lastOutboundTripDeparts = lastStop.getStopPeriod();
            }

            setInboundTripDepartTimes(stops, lastOutboundTripDeparts);
        }

        int origMgra = tour.getTourDestMgra();
        int destMgra = tour.getTourOrigMgra();

        todTime += (System.nanoTime() - check);

        applySlcModelWithTiming(household, person, tour, stops, origMgra, destMgra, true);

    }

    private void applySlcModel(Household household, Person person, Tour tour, Stop[] stops,
            int origMgra, int destMgra, boolean directionIsInbound)
    {

        int lastDest = -1;
        int newOrig = -1;

        // get the array of distances from the tour origin mgra to all MGRAs.
        // use these distances for tour orig to stop alt distances
        anm.getDistancesFromMgra(origMgra, tourOrigToAllMgraDistances,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        anm.getDistancesFromMgra(destMgra, tourDestToAllMgraDistances,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        // anm.getDistancesToMgra( destMgra, tourDestToAllMgraDistances,
        // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() ) );

        // if there are stops on this half-tour, determine their destinations,
        // depart hours, trip modes, and parking tazs.
        if (stops != null)
        {

            int oldSelectedIndex = -1;
            for (int i = 0; i < stops.length; i++)
            {

                Stop stop = stops[i];

                // if i is 0, the stop origin is set to origMgra; otherwise stop
                // orig is the chosen dest from the previous stop.
                if (i == 0) newOrig = origMgra;
                else newOrig = lastDest;
                stop.setOrig(newOrig);

                stopLocDmuObj.setStopObject(stop);
                stopLocDmuObj.setDmuIndexValues(household.getHhId(), household.getHhMgra(),
                        newOrig, destMgra);

                int choice = -1;
                int selectedIndex = -1;
                int modeAlt = -1;
                // if not the last stop object, make a destination choice and a
                // mode choice from IK MC probabilities;
                // otherwise stop dest is set to destMgra, and make a mode
                // choice from KJ MC probabilities.
                if (i < stops.length - 1)
                {

                    try
                    {

                        selectedIndex = selectDestination(stop);
                        choice = finalSample[selectedIndex];
                        stop.setDest(choice);
                        lastDest = choice;

                        if (household.getDebugChoiceModels())
                        {
                            smcLogger
                                    .info("Monte Carlo selection for determining Mode Choice from IK Probabilities for "
                                            + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                            + " stop.");
                            smcLogger.info("HHID=" + household.getHhId() + ", persNum="
                                    + person.getPersonNum() + ", tourPurpose="
                                    + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                    + ", tourMode=" + tour.getTourModeChoice());
                            smcLogger.info("StopID=" + (stop.getStopId() + 1) + " of "
                                    + (stops.length - 1) + " stops, inbound="
                                    + stop.isInboundStop() + ", stopPurpose="
                                    + stop.getDestPurpose() + ", stopDepart="
                                    + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                    + ", stopDest=" + stop.getDest());
                        }

                        modeAlt = selectModeFromProbabilities(stop,
                                mcCumProbsSegmentIk[selectedIndex]);
                        if (modeAlt < 0)
                        {
                            logger.info("error getting trip mode choice for IK proportions, i=" + i);
                            logger.info("HHID=" + household.getHhId() + ", persNum="
                                    + person.getPersonNum() + ", tourPurpose="
                                    + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                    + ", tourMode=" + tour.getTourModeChoice());
                            logger.info("StopID=" + (stop.getStopId() + 1) + " of "
                                    + (stops.length - 1) + " stops, inbound="
                                    + stop.isInboundStop() + ", stopPurpose="
                                    + stop.getDestPurpose() + ", stopDepart="
                                    + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                    + ", stopDest=" + stop.getDest());
                            throw new RuntimeException();
                        }

                        int park = -1;
                        if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                        {
                            park = selectParkingLocation(household, tour, stop);
                            stop.setPark(park);
                            if (park > 0) lastDest = park;
                        }

                    } catch (Exception e)
                    {
                        logger.error(String
                                .format("Exception caught processing %s stop location choice model for %s type tour %s stop:  HHID=%d, personNum=%s, stop=%d.",
                                        (stopLocDmuObj.getInboundStop() == 1 ? "inbound"
                                                : "outbound"),
                                        tour.getTourCategory(),
                                        tour.getTourPurpose(),
                                        household.getHhId(),
                                        (person == null ? "N/A" : Integer.toString(person
                                                .getPersonNum())), (i + 1)));
                        throw new RuntimeException(e);
                    }

                    stop.setMode(modeAlt);

                    // if the trip is a transit mode, set the boarding and
                    // alighting tap pairs in the stop object based on the ik
                    // segment pairs
                    if (modelStructure.getTripModeIsWalkTransit(modeAlt)
                            | modelStructure.getTripModeIsPnrTransit(modeAlt)
                            || modelStructure.getTripModeIsKnrTransit(modeAlt))
                    {

                        if (segmentIkBestTapPairs[selectedIndex] == null)
                        {
                            stop.setBoardTap(0);
                            stop.setAlightTap(0);
                        } else
                        {
                            int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                            if (segmentIkBestTapPairs[selectedIndex][rideMode] == null)
                            {
                                stop.setBoardTap(0);
                                stop.setAlightTap(0);
                            } else
                            {
                                stop.setBoardTap(segmentIkBestTapPairs[selectedIndex][rideMode][0]);
                                stop.setAlightTap(segmentIkBestTapPairs[selectedIndex][rideMode][1]);
                            }
                        }

                    }

                    oldSelectedIndex = selectedIndex;

                } else
                {
                    // last stop on half-tour, so dest is the half-tour final
                    // dest, and oldSelectedIndex was
                    // the selectedIndex determined for the previous stop
                    // location choice.
                    stop.setDest(destMgra);

                    if (household.getDebugChoiceModels())
                    {
                        smcLogger
                                .info("Monte Carlo selection for determining Mode Choice from KJ Probabilities for "
                                        + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                        + " stop.");
                        smcLogger.info("HHID=" + household.getHhId() + ", persNum="
                                + person.getPersonNum() + ", tourPurpose="
                                + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                + ", tourMode=" + tour.getTourModeChoice());
                        smcLogger.info("StopID=End of "
                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                + " half-tour, stopPurpose=" + stop.getDestPurpose()
                                + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig="
                                + stop.getOrig() + ", stopDest=" + stop.getDest());
                    }

                    modeAlt = selectModeFromProbabilities(stop,
                            mcCumProbsSegmentKj[oldSelectedIndex]);
                    if (modeAlt < 0)
                    {
                        logger.error("error getting trip mode choice for KJ proportions, i=" + i);
                        logger.error("HHID=" + household.getHhId() + ", persNum="
                                + person.getPersonNum() + ", tourPurpose="
                                + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                + ", tourMode=" + tour.getTourModeChoice());
                        logger.error("StopID=End of "
                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                + " half-tour, stopPurpose=" + stop.getDestPurpose()
                                + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig="
                                + stop.getOrig() + ", stopDest=" + stop.getDest());
                        throw new RuntimeException();
                    }

                    // last stop on tour, so if inbound, only need park location
                    // choice if tour is work-based subtour;
                    // otherwise dest is home.
                    int park = -1;
                    if (directionIsInbound)
                    {
                        if (tour.getTourCategory()
                                .equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                        {
                            if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                            {
                                park = selectParkingLocation(household, tour, stop);
                                stop.setPark(park);
                            }
                        }
                    } else
                    {
                        if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                        {
                            park = selectParkingLocation(household, tour, stop);
                            stop.setPark(park);
                        }
                    }

                    stop.setMode(modeAlt);

                    // if the last trip is a transit mode, set the boarding and
                    // alighting tap pairs in the stop object based on the kj
                    // segment pairs
                    if (modelStructure.getTripModeIsWalkTransit(modeAlt)
                            | modelStructure.getTripModeIsPnrTransit(modeAlt)
                            || modelStructure.getTripModeIsKnrTransit(modeAlt))
                    {

                        if (segmentKjBestTapPairs[oldSelectedIndex] == null)
                        {
                            stop.setBoardTap(0);
                            stop.setAlightTap(0);
                        } else
                        {
                            int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                            if (segmentKjBestTapPairs[oldSelectedIndex][rideMode] == null)
                            {
                                stop.setBoardTap(0);
                                stop.setAlightTap(0);
                            } else
                            {
                                stop.setBoardTap(segmentKjBestTapPairs[oldSelectedIndex][rideMode][0]);
                                stop.setAlightTap(segmentKjBestTapPairs[oldSelectedIndex][rideMode][1]);
                            }
                        }

                    }

                }

            }

        } else
        {
            // create a stop object to hold attributes for orig, dest, mode,
            // departtime, etc.
            // for the half-tour with no stops.

            // create a Stop object for use in applying trip mode choice for
            // this half tour without stops
            String origStopPurpose = "";
            String destStopPurpose = "";
            if (!directionIsInbound)
            {
                origStopPurpose = tour.getTourCategory().equalsIgnoreCase(
                        ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
                destStopPurpose = tour.getTourPrimaryPurpose();
            } else
            {
                origStopPurpose = tour.getTourPrimaryPurpose();
                destStopPurpose = tour.getTourCategory().equalsIgnoreCase(
                        ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
            }

            Stop stop = null;
            try
            {
                stop = tour.createStop(modelStructure, origStopPurpose, destStopPurpose,
                        directionIsInbound,
                        tour.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY));
            } catch (Exception e)
            {
                logger.info("exception creating stop.");
            }

            // set stop origin and destination mgra, the stop period based on
            // direction, then calculate the half-tour trip mode choice
            stop.setOrig(origMgra);
            stop.setDest(destMgra);

            if (directionIsInbound) stop.setStopPeriod(tour.getTourArrivePeriod());
            else stop.setStopPeriod(tour.getTourDepartPeriod());

            int modeAlt = getHalfTourModeChoice(stop);
            if (modeAlt < 0)
            {
                logger.info("error getting trip mode choice for half-tour with no stops.");
                logger.info("HHID=" + household.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId());
                logger.info("StopID=" + (stop.getStopId() + 1) + " of no stops, inbound="
                        + stop.isInboundStop() + ", stopPurpose=" + stop.getDestPurpose()
                        + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                        + ", stopDest=" + stop.getDest());
                throw new RuntimeException();
            }

            stop.setMode(modeAlt);

            int[][] bestTaps = null;
            if (modelStructure.getTripModeIsWalkTransit(modeAlt))
            {
                if (directionIsInbound) bestTaps = tour.getBestWtwTapPairsIn();
                else bestTaps = tour.getBestWtwTapPairsOut();
            } else if (modelStructure.getTripModeIsPnrTransit(modeAlt)
                    || modelStructure.getTripModeIsKnrTransit(modeAlt))
            {
                if (directionIsInbound) bestTaps = tour.getBestWtdTapPairsIn();
                else bestTaps = tour.getBestDtwTapPairsOut();
            }

            if (bestTaps == null)
            {
                stop.setBoardTap(0);
                stop.setAlightTap(0);
            } else
            {
                int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                if (bestTaps[rideMode] == null)
                {
                    stop.setBoardTap(0);
                    stop.setAlightTap(0);
                } else
                {
                    stop.setBoardTap(bestTaps[rideMode][0]);
                    stop.setAlightTap(bestTaps[rideMode][1]);
                }
            }

            // inbound half-tour, only need park location choice if tour is
            // work-based subtour;
            // otherwise dest is home.
            int park = -1;
            if (directionIsInbound)
            {
                if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                {
                    if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                    {
                        park = selectParkingLocation(household, tour, stop);
                        stop.setPark(park);
                    }
                }
            } else
            {
                if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                {
                    park = selectParkingLocation(household, tour, stop);
                    stop.setPark(park);
                }
            }

        }

    }

    private void applySlcModelWithTiming(Household household, Person person, Tour tour,
            Stop[] stops, int origMgra, int destMgra, boolean directionIsInbound)
    {

        int lastDest = -1;
        int newOrig = -1;

        // get the array of distances from the tour origin mgra to all MGRAs.
        // use these distances for tour orig to stop alt distances
        long check = System.nanoTime();
        anm.getDistancesFromMgra(origMgra, tourOrigToAllMgraDistances,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        anm.getDistancesFromMgra(destMgra, tourDestToAllMgraDistances,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        // anm.getDistancesToMgra( destMgra, tourDestToAllMgraDistances,
        // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() ) );
        sldTime += (System.nanoTime() - check);

        // if there are stops on this half-tour, determine their destinations,
        // depart hours, trip modes, and parking tazs.
        if (stops != null)
        {

            int oldSelectedIndex = -1;
            earlierTripWasLocatedInAlightingTapShed = false;

            for (int i = 0; i < stops.length; i++)
            {

                Stop stop = stops[i];

                // if i is 0, the stop origin is set to origMgra; otherwise stop
                // orig is the chosen dest from the previous stop.
                if (i == 0) newOrig = origMgra;
                else newOrig = lastDest;
                stop.setOrig(newOrig);

                stopLocDmuObj.setStopObject(stop);
                stopLocDmuObj.setDmuIndexValues(household.getHhId(), household.getHhMgra(),
                        newOrig, destMgra);

                int choice = -1;
                int selectedIndex = -1;
                int modeAlt = -1;
                // if not the last stop object, make a destination choice and a
                // mode choice from IK MC probabilities;
                // otherwise stop dest is set to destMgra, and make a mode
                // choice from KJ MC probabilities.
                if (i < stops.length - 1)
                {

                    try
                    {

                        check = System.nanoTime();

                        selectedIndex = selectDestinationWithTiming(stop);
                        choice = finalSample[selectedIndex];
                        //Wu's temporary fix to set stop destination to origin if no valid stop is found
                        if (choice==-1) choice=stop.getOrig();
                        stop.setDest(choice);

                        if (sampleMgraInAlightingTapShed[choice])
                            earlierTripWasLocatedInAlightingTapShed = true;
                        lastDest = choice;
                        slcTime += (System.nanoTime() - check);

                        if (household.getDebugChoiceModels())
                        {
                            if (tour.getTourCategory().equalsIgnoreCase(
                                    ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
                            {
                                smcLogger
                                        .info("Monte Carlo selection for determining Mode Choice from IK Probabilities for "
                                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                                + " for joint tour stop.");
                                smcLogger.info("HHID=" + household.getHhId() + ", persNum=" + "N/A"
                                        + ", tourPurpose=" + tour.getTourPrimaryPurpose()
                                        + ", tourId=" + tour.getTourId() + ", tourMode="
                                        + tour.getTourModeChoice());
                                smcLogger.info("StopID=" + (stop.getStopId() + 1) + " of "
                                        + (stops.length - 1) + " stops, inbound="
                                        + stop.isInboundStop() + ", stopPurpose="
                                        + stop.getDestPurpose() + ", stopDepart="
                                        + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                        + ", stopDest=" + stop.getDest());
                            } else
                            {
                                smcLogger
                                        .info("Monte Carlo selection for determining Mode Choice from IK Probabilities for "
                                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                                + " stop.");
                                smcLogger.info("HHID=" + household.getHhId() + ", persNum="
                                        + person.getPersonNum() + ", tourPurpose="
                                        + tour.getTourPrimaryPurpose() + ", tourId="
                                        + tour.getTourId() + ", tourMode="
                                        + tour.getTourModeChoice());
                                smcLogger.info("StopID=" + (stop.getStopId() + 1) + " of "
                                        + (stops.length - 1) + " stops, inbound="
                                        + stop.isInboundStop() + ", stopPurpose="
                                        + stop.getDestPurpose() + ", stopDepart="
                                        + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                        + ", stopDest=" + stop.getDest());
                            }
                        }

                        check = System.nanoTime();
                        modeAlt = selectModeFromProbabilities(stop,
                                mcCumProbsSegmentIk[selectedIndex]);

                        if (modeAlt < 0)
                        {
                            logger.info("error getting trip mode choice for IK proportions, i=" + i);
                            logger.info("HHID=" + household.getHhId() + ", persNum="
                                    + person.getPersonNum() + ", tourPurpose="
                                    + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                    + ", tourMode=" + tour.getTourModeChoice());
                            logger.info("StopID=" + (stop.getStopId() + 1) + " of "
                                    + (stops.length - 1) + " stops, inbound="
                                    + stop.isInboundStop() + ", stopPurpose="
                                    + stop.getDestPurpose() + ", stopDepart="
                                    + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                    + ", stopDest=" + stop.getDest());
                            throw new RuntimeException();
                        }

                        int park = -1;
                        if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                        {
                            park = selectParkingLocation(household, tour, stop);
                            stop.setPark(park);
                            if (park > 0) lastDest = park;
                        }

                        smcTime += (System.nanoTime() - check);
                    } catch (Exception e)
                    {
                        logger.error(String.format(
                                "Exception caught processing %s stop location choice model.",
                                (stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound")));
                        logger.error("HHID=" + household.getHhId() + ", persNum="
                                + person.getPersonNum() + ", tour category="
                                + tour.getTourCategory() + ", tourPurpose="
                                + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                + ", tourMode=" + tour.getTourModeChoice());
                        logger.error("StopID=" + (stop.getStopId() + 1) + " of "
                                + (stops.length - 1) + " stops, inbound=" + stop.isInboundStop()
                                + ", stopPurpose=" + stop.getDestPurpose() + ", stopDepart="
                                + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                                + ", stopDest=" + stop.getDest());
                        logger.error(String
                                .format("origMgra=%d, destMgra=%d, newOrig=%d, lastDest=%d, modeAlt=%d, selectedIndex=%d, choice=%d.",
                                        origMgra, destMgra, newOrig, lastDest, modeAlt,
                                        selectedIndex, choice));
                        throw new RuntimeException(e);
                    }

                    stop.setMode(modeAlt);

                    // if the trip is a transit mode, set the boarding and
                    // alighting tap pairs in the stop object based on the ik
                    // segment pairs
                    if (modelStructure.getTripModeIsWalkTransit(modeAlt)
                            | modelStructure.getTripModeIsPnrTransit(modeAlt)
                            || modelStructure.getTripModeIsKnrTransit(modeAlt))
                    {

                        if (segmentIkBestTapPairs[selectedIndex] == null)
                        {
                            stop.setBoardTap(0);
                            stop.setAlightTap(0);
                        } else
                        {
                            int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                            if (segmentIkBestTapPairs[selectedIndex][rideMode] == null)
                            {
                                stop.setBoardTap(0);
                                stop.setAlightTap(0);
                            } else
                            {
                                stop.setBoardTap(segmentIkBestTapPairs[selectedIndex][rideMode][0]);
                                stop.setAlightTap(segmentIkBestTapPairs[selectedIndex][rideMode][1]);
                            }
                        }

                    }

                    oldSelectedIndex = selectedIndex;

                } else
                {

                    // last stop on half-tour, so dest is the half-tour final
                    // dest, and oldSelectedIndex was
                    // the selectedIndex determined for the previous stop
                    // location choice.
                    stop.setDest(destMgra);

                    if (household.getDebugChoiceModels())
                    	{
                    	if (tour.getTourCategory().equalsIgnoreCase(
                            ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
                    	{
                    		smcLogger
                                .info("Monte Carlo selection for determining Mode Choice from KJ Probabilities for "
                                        + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                        + " joint tour stop.");
                    		smcLogger.info("HHID=" + household.getHhId() + ", persNum=" + "N/A"
                                + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                                + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                    		smcLogger.info("StopID=End of "
                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                + " half-tour, stopPurpose=" + stop.getDestPurpose()
                                + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig="
                                + stop.getOrig() + ", stopDest=" + stop.getDest());
                    	} else
                    	{
                    		smcLogger
                                .info("Monte Carlo selection for determining Mode Choice from KJ Probabilities for "
                                        + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                        + " stop.");
                    		smcLogger.info("HHID=" + household.getHhId() + ", persNum="
                                + person.getPersonNum() + ", tourPurpose="
                                + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                + ", tourMode=" + tour.getTourModeChoice());
                    		smcLogger.info("StopID=End of "
                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                + " half-tour, stopPurpose=" + stop.getDestPurpose()
                                + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig="
                                + stop.getOrig() + ", stopDest=" + stop.getDest());
                    	}
                    }
                    check = System.nanoTime();
                    modeAlt = selectModeFromProbabilities(stop,
                            mcCumProbsSegmentKj[oldSelectedIndex]);
                    if (modeAlt < 0)
                    {
                        logger.error("error getting trip mode choice for KJ proportions, i=" + i);
                        logger.error("HHID=" + household.getHhId() + ", persNum="
                                + person.getPersonNum() + ", tourPurpose="
                                + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId()
                                + ", tourMode=" + tour.getTourModeChoice());
                        logger.error("StopID=End of "
                                + (stop.isInboundStop() ? "INBOUND" : "OUTBOUND")
                                + " half-tour, stopPurpose=" + stop.getDestPurpose()
                                + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig="
                                + stop.getOrig() + ", stopDest=" + stop.getDest());
                        throw new RuntimeException();
                    }

                    // last stop on tour, so if inbound, only need park location
                    // choice if tour is work-based subtour;
                    // otherwise dest is home.
                    int park = -1;
                    if (directionIsInbound)
                    {
                        if (tour.getTourCategory()
                                .equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                        {
                            if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                            {
                                park = selectParkingLocation(household, tour, stop);
                                stop.setPark(park);
                            }
                        }
                    } else
                    {
                        if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                        {
                            park = selectParkingLocation(household, tour, stop);
                            stop.setPark(park);
                        }
                    }

                    smcTime += (System.nanoTime() - check);

                    stop.setMode(modeAlt);

                    // if the last trip is a transit mode, set the boarding and
                    // alighting tap pairs in the stop object based on the kj
                    // segment pairs
                    if (modelStructure.getTripModeIsWalkTransit(modeAlt)
                            || modelStructure.getTripModeIsPnrTransit(modeAlt)
                            || modelStructure.getTripModeIsKnrTransit(modeAlt))
                    {

                        if (segmentKjBestTapPairs[oldSelectedIndex] == null)
                        {
                            stop.setBoardTap(0);
                            stop.setAlightTap(0);
                        } else
                        {
                            int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                            if (segmentKjBestTapPairs[oldSelectedIndex][rideMode] == null)
                            {
                                stop.setBoardTap(0);
                                stop.setAlightTap(0);
                            } else
                            {
                                stop.setBoardTap(segmentKjBestTapPairs[oldSelectedIndex][rideMode][0]);
                                stop.setAlightTap(segmentKjBestTapPairs[oldSelectedIndex][rideMode][1]);
                            }
                        }

                    }

                }

            }

        } else
        { // create a stop object to hold attributes for orig, dest, mode,
          // departtime, etc.
          // for the half-tour with no stops.

            check = System.nanoTime();

            // create a Stop object for use in applying trip mode choice for
            // this half tour without stops
            String origStopPurpose = "";
            String destStopPurpose = "";
            if (!directionIsInbound)
            {
                origStopPurpose = tour.getTourCategory().equalsIgnoreCase(
                        ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
                destStopPurpose = tour.getTourPrimaryPurpose();
            } else
            {
                origStopPurpose = tour.getTourPrimaryPurpose();
                destStopPurpose = tour.getTourCategory().equalsIgnoreCase(
                        ModelStructure.AT_WORK_CATEGORY) ? "Work" : "Home";
            }

            Stop stop = null;
            try
            {
                stop = tour.createStop(modelStructure, origStopPurpose, destStopPurpose,
                        directionIsInbound,
                        tour.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY));
            } catch (Exception e)
            {
                logger.info("exception creating stop.");
            }

            stop.setOrig(origMgra);
            stop.setDest(destMgra);

            if (directionIsInbound) stop.setStopPeriod(tour.getTourArrivePeriod());
            else stop.setStopPeriod(tour.getTourDepartPeriod());

            int modeAlt = getHalfTourModeChoice(stop);
            if (modeAlt < 0)
            {
                logger.info("error getting trip mode choice for half-tour with no stops.");
                logger.info("HHID=" + household.getHhId() + ", tourPurpose="
                        + tour.getTourPrimaryPurpose() + ", tourId=" + tour.getTourId());
                logger.info("StopID=" + (stop.getStopId() + 1) + " of no stops, inbound="
                        + stop.isInboundStop() + ", stopPurpose=" + stop.getDestPurpose()
                        + ", stopDepart=" + stop.getStopPeriod() + ", stopOrig=" + stop.getOrig()
                        + ", stopDest=" + stop.getDest());

                modeAlt = stop.getTour().getTourModeChoice();
                // throw new RuntimeException();
            }

            stop.setMode(modeAlt);

            int[][] bestTaps = null;
            if (modelStructure.getTripModeIsWalkTransit(modeAlt))
            {
                if (directionIsInbound) bestTaps = tour.getBestWtwTapPairsIn();
                else bestTaps = tour.getBestWtwTapPairsOut();
            } else if (modelStructure.getTripModeIsPnrTransit(modeAlt)
                    || modelStructure.getTripModeIsKnrTransit(modeAlt))
            {
                if (directionIsInbound) bestTaps = tour.getBestWtdTapPairsIn();
                else bestTaps = tour.getBestDtwTapPairsOut();
            }

            if (bestTaps == null)
            {
                stop.setBoardTap(0);
                stop.setAlightTap(0);
            } else
            {
                int rideMode = modelStructure.getRideModeIndexForTripMode(modeAlt);
                if (bestTaps[rideMode] == null)
                {
                    stop.setBoardTap(0);
                    stop.setAlightTap(0);
                } else
                {
                    stop.setBoardTap(bestTaps[rideMode][0]);
                    stop.setAlightTap(bestTaps[rideMode][1]);
                }
            }

            // inbound half-tour, only need park location choice if tour is
            // work-based subtour;
            // otherwise dest is home.
            int park = -1;
            if (directionIsInbound)
            {
                if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                {
                    if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                    {
                        park = selectParkingLocation(household, tour, stop);
                        stop.setPark(park);
                    }
                }
            } else
            {
                if (modelStructure.getTripModeIsSovOrHov(modeAlt))
                {
                    park = selectParkingLocation(household, tour, stop);
                    stop.setPark(park);
                }
            }

            smcTime += (System.nanoTime() - check);
        }

    }

    private int selectDestination(Stop s)
    {

        Logger modelLogger = slcLogger;

        int[] loggingSample = null;
        int[] debugLoggingSample = null;
        // int[] debugLoggingSample = { 0, 16569 };
        // int[] debugLoggingSample = { 0, 4886, 16859, 18355, 3222, 14879,
        // 26894, 16512, 9908, 18287, 14989 };

        Tour tour = s.getTour();
        Person person = tour.getPersonObject();
        Household household = person.getHouseholdObject();

        if (household.getDebugChoiceModels())
        {
            household.logHouseholdObject(
                    "Pre Stop Location Choice for trip: HH_" + household.getHhId() + ", Pers_"
                            + tour.getPersonObject().getPersonNum() + ", Tour Purpose_"
                            + tour.getTourPurpose() + ", Tour_" + tour.getTourId()
                            + ", Tour Purpose_" + tour.getTourPurpose() + ", Stop_"
                            + (s.getStopId() + 1), modelLogger);
            household.logPersonObject("Pre Stop Location Choice for person "
                    + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject());
            household.logTourObject("Pre Stop Location Choice for tour " + tour.getTourId(),
                    modelLogger, tour.getPersonObject(), tour);
            household.logStopObject("Pre Stop Location Choice for stop " + (s.getStopId() + 1),
                    modelLogger, s, modelStructure);

            loggingSample = debugLoggingSample;
        }

        int numAltsInSample = -1;

        stopLocDmuObj.setTourModeIndex(tour.getTourModeChoice());

        // set the size terms array for the stop purpose in the dmu object
        stopLocDmuObj
                .setLogSize(getLnSlcSizeTermsForStopPurpose(s.getStopPurposeIndex(), household));

        // get the array of distances from the stop origin mgra to all MGRAs and
        // set in the dmu object
        anm.getDistancesFromMgra(s.getOrig(), distanceFromStopOrigToAllMgras,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        stopLocDmuObj.setDistancesFromOrigMgra(distanceFromStopOrigToAllMgras);

        
        // bike logsums from origin to all destinations
        if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
        
            Arrays.fill(bikeLogsumFromStopOrigToAllMgras, 0);
        	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

            for (int dMgra = 1; dMgra <= mgraManager.getMaxMgra(); dMgra++)
            {
            	bikeLogsumFromStopOrigToAllMgras[dMgra] = bls.getLogsum(segment,s.getOrig(),dMgra);
            }
            stopLocDmuObj.setBikeLogsumsFromOrigMgra(bikeLogsumFromStopOrigToAllMgras);
        }
        
        
        
        // if tour mode is transit, set availablity of location alternatives
        // based on transit accessibility relative to best transit TAP pair for
        // tour
        if (modelStructure.getTourModeIsTransit(tour.getTourModeChoice()))
        {

            Arrays.fill(sampleMgraInBoardingTapShed, false);
            Arrays.fill(sampleMgraInAlightingTapShed, false);

            int numAvailableAlternatives = setSoaAvailabilityForTransitTour(s, tour);
            if (numAvailableAlternatives == 0)
            {
                logger.error("no available locations - empty sample.");
                logger.error("best tap pair which is empty: " +  Arrays.deepToString(s.isInboundStop() ? tour.getBestWtwTapPairsIn() : tour.getBestWtwTapPairsOut()));
                throw new RuntimeException();
            }
        }

        // get the array of distances to the half-tour final destination mgra
        // from all MGRAs and set in the dmu object
        if (s.isInboundStop())
        {
            // if inbound, final half-tour destination is the tour origin
            anm.getDistancesToMgra(tour.getTourOrigMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);
            

            // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourOrigMgra()]);

        
            // bike logsums from all MGRAs back to tour origin
            if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
            
                Arrays.fill(bikeLogsumToFinalDestFromAllMgras, 0);
            	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

                for (int oMgra = 1; oMgra <= mgraManager.getMaxMgra(); oMgra++)
                {
                	bikeLogsumToFinalDestFromAllMgras[oMgra] = bls.getLogsum(segment,oMgra,tour.getTourOrigMgra());
                }
                stopLocDmuObj.setBikeLogsumsToDestMgra(bikeLogsumToFinalDestFromAllMgras);
            }

        
        
        
        
        } else
        {
            // if outbound, final half-tour destination is the tour destination
            anm.getDistancesToMgra(tour.getTourDestMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);

            // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourDestMgra()]);
      
            // bike logsums from all MGRAs back to tour origin
            if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
            
                Arrays.fill(bikeLogsumToFinalDestFromAllMgras, 0);
            	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

                for (int oMgra = 1; oMgra <= mgraManager.getMaxMgra(); oMgra++)
                {
                	bikeLogsumToFinalDestFromAllMgras[oMgra] = bls.getLogsum(segment,oMgra,tour.getTourDestMgra());
                }
                stopLocDmuObj.setBikeLogsumsToDestMgra(bikeLogsumToFinalDestFromAllMgras);
            }
            

        
        
        }

        if (useNewSoaMethod)
        {
            if (modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice())) selectSampleOfAlternativesAutoTourNew(
                    s, tour, person, household, loggingSample);
            else selectSampleOfAlternativesOther(s, tour, person, household, loggingSample);

            numAltsInSample = dcTwoStageModelObject.getNumberofUniqueMgrasInSample();
        } else
        {
            if (modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice())) selectSampleOfAlternativesAutoTour(
                    s, tour, person, household, loggingSample);
            else selectSampleOfAlternativesOther(s, tour, person, household, loggingSample);

            numAltsInSample = altFreqMap.size();
        }

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";

        if (household.getDebugChoiceModels())
        {

            choiceModelDescription = "Stop Location Choice";
            decisionMakerLabel = String
                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourPurpose(), tour.getTourModeChoice(), tour.getTourId(),
                            s.getDestPurpose(), (s.getStopId() + 1));
            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            modelLogger.info(" ");
            for (int k = 0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info(loggingHeader);
            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

        }

        setupStopLocationChoiceAlternativeArrays(numAltsInSample, s);

        int slcModelIndex = -1;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.MANDATORY_CATEGORY)) slcModelIndex = MAND_SLC_MODEL_INDEX;
        else if (tour.getTourPrimaryPurposeIndex() == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX
                || tour.getTourPrimaryPurposeIndex() == ModelStructure.SHOPPING_PRIMARY_PURPOSE_INDEX
                || tour.getTourPrimaryPurposeIndex() == ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_INDEX) slcModelIndex = MAINT_SLC_MODEL_INDEX;
        else slcModelIndex = DISCR_SLC_MODEL_INDEX;

        slcModelArray[slcModelIndex].computeUtilities(stopLocDmuObj,
                stopLocDmuObj.getDmuIndexValues(), sampleAvailability, inSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        int selectedIndex = -1;
        if (slcModelArray[slcModelIndex].getAvailabilityCount() > 0)
        {
            chosen = slcModelArray[slcModelIndex].getChoiceResult(rn);
            selectedIndex = chosen;
        }else{	        
	        //wu's tempory fix to set chosen stop alternative to origin mgra if no alternative is available-8/27/2014        
	        //instead of this method, seems selectDestinationWithTiming(Stop s) is called (similar change made there too)
	        //tempory fix is put in here just in case.
	        chosen=tour.getTourOrigMgra();
        }

        // write choice model alternative info to log file
        if (household.getDebugChoiceModels() || chosen < 0)
        {

            if (chosen < 0)
            {

                modelLogger
                        .error("ERROR selecting stop location choice due to no alternatives available.");
                modelLogger
                        .error("setting debug to true and recomputing sample of alternatives selection.");
                modelLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d, StopOrig=%d",
                                        household.getHhId(), person.getPersonNum(),
                                        person.getPersonType(), tour.getTourPurpose(),
                                        tour.getTourModeChoice(), tour.getTourId(),
                                        s.getDestPurpose(), (s.getStopId() + 1), s.getOrig()));

                choiceModelDescription = "Stop Location Choice";
                decisionMakerLabel = String
                        .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d, StopOrig=%d",
                                household.getHhId(), person.getPersonNum(), person.getPersonType(),
                                tour.getTourPurpose(), tour.getTourModeChoice(), tour.getTourId(),
                                s.getDestPurpose(), (s.getStopId() + 1), s.getOrig());
                loggingHeader = String.format("%s for %s", choiceModelDescription,
                        decisionMakerLabel);

                modelLogger.error(" ");
                for (int k = 0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.error(loggingHeader);
                modelLogger.error(separator);
                modelLogger.error("");
                modelLogger.error("");

                // utilities and probabilities are 0 based.
                double[] utilities = slcModelArray[slcModelIndex].getUtilities();
                double[] probabilities = slcModelArray[slcModelIndex].getProbabilities();

                // availabilities is 1 based.
                boolean[] availabilities = slcModelArray[slcModelIndex].getAvailabilities();

                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();

                modelLogger
                        .error("Person num: " + personNum + ", Person type: " + personTypeString);
                modelLogger
                        .error("Alternative             Availability           Utility       Probability           CumProb");
                modelLogger
                        .error("---------------------   ------------       -----------    --------------    --------------");

                double cumProb = 0.0;
                for (int j = 1; j <= numAltsInSample; j++)
                {

                    int alt = finalSample[j];

                    if (j == chosen) selectedIndex = j;

                    cumProb += probabilities[j - 1];
                    String altString = String.format("%-3d %5d", j, alt);
                    modelLogger.error(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                            availabilities[j], utilities[j - 1], probabilities[j - 1], cumProb));
                }

                modelLogger.error(" ");
                String altString = String.format("%-3d %5d", selectedIndex, -1);
                modelLogger.error(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));

                modelLogger.error(separator);
                modelLogger.error("");
                modelLogger.error("");

                slcModelArray[slcModelIndex].logAlternativesInfo(choiceModelDescription,
                        decisionMakerLabel);
                slcModelArray[slcModelIndex].logSelectionInfo(choiceModelDescription,
                        decisionMakerLabel, rn, chosen);

                // write UEC calculation results to separate model specific log
                // file
                slcModelArray[slcModelIndex].logUECResults(modelLogger, loggingHeader);

                logger.error(String
                        .format("Error for HHID=%d, PersonNum=%d, no available %s stop destination choice alternatives to choose from in choiceModelApplication.",
                                tour.getHhId(), tour.getPersonObject().getPersonNum(),
                                tour.getTourPurpose()));
                throw new RuntimeException();

            }

            // utilities and probabilities are 0 based.
            double[] utilities = slcModelArray[slcModelIndex].getUtilities();
            double[] probabilities = slcModelArray[slcModelIndex].getProbabilities();

            // availabilities is 1 based.
            boolean[] availabilities = slcModelArray[slcModelIndex].getAvailabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("---------------------   ------------       -----------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 1; j <= numAltsInSample; j++)
            {

                int alt = finalSample[j];

                if (j == chosen) selectedIndex = j;

                cumProb += probabilities[j - 1];
                String altString = String.format("%-3d %5d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j], utilities[j - 1], probabilities[j - 1], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("%-3d %5d", selectedIndex, finalSample[selectedIndex]);
            modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            slcModelArray[slcModelIndex].logAlternativesInfo(choiceModelDescription,
                    decisionMakerLabel);
            slcModelArray[slcModelIndex].logSelectionInfo(choiceModelDescription,
                    decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            slcModelArray[slcModelIndex].logUECResults(modelLogger, loggingHeader);

        }

        return selectedIndex;
    }

    private int selectDestinationWithTiming(Stop s)
    {

        Logger modelLogger = slcLogger;

        int[] loggingSample = null;
        int[] debugLoggingSample = null;
        // int[] debugLoggingSample = { 0, 16569 };
        // int[] debugLoggingSample = { 0, 4886, 16859, 18355, 3222, 14879,
        // 26894, 16512, 9908, 18287, 14989 };

        Tour tour = s.getTour();
        Person person = tour.getPersonObject();
        Household household = person.getHouseholdObject();

        if (household.getDebugChoiceModels())
        {
            household.logHouseholdObject(
                    "Pre Stop Location Choice for trip: HH_" + household.getHhId() + ", Pers_"
                            + tour.getPersonObject().getPersonNum() + ", Tour Purpose_"
                            + tour.getTourPurpose() + ", Tour_" + tour.getTourId()
                            + ", Tour Purpose_" + tour.getTourPurpose() + ", Stop_"
                            + (s.getStopId() + 1), modelLogger);
            household.logPersonObject("Pre Stop Location Choice for person "
                    + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject());
            household.logTourObject("Pre Stop Location Choice for tour " + tour.getTourId(),
                    modelLogger, tour.getPersonObject(), tour);
            household.logStopObject("Pre Stop Location Choice for stop " + (s.getStopId() + 1),
                    modelLogger, s, modelStructure);

            loggingSample = debugLoggingSample;
        }

        int numAltsInSample = -1;

        stopLocDmuObj.setTourModeIndex(tour.getTourModeChoice());

        // set the size terms array for the stop purpose in the dmu object
        stopLocDmuObj
                .setLogSize(getLnSlcSizeTermsForStopPurpose(s.getStopPurposeIndex(), household));

        // get the array of distances from the stop origin mgra to all MGRAs and
        // set in the dmu object
        anm.getDistancesFromMgra(s.getOrig(), distanceFromStopOrigToAllMgras,
                modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
        stopLocDmuObj.setDistancesFromOrigMgra(distanceFromStopOrigToAllMgras);

        
        // bike logsums from origin to all destinations
        if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
        
            Arrays.fill(bikeLogsumFromStopOrigToAllMgras, 0);
        	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

            for (int dMgra = 1; dMgra <= mgraManager.getMaxMgra(); dMgra++)
            {
            	bikeLogsumFromStopOrigToAllMgras[dMgra] = bls.getLogsum(segment,s.getOrig(),dMgra);
            }
            stopLocDmuObj.setBikeLogsumsFromOrigMgra(bikeLogsumFromStopOrigToAllMgras);
        }

        // if tour mode is transit, set availablity of location alternatives
        // based on transit accessibility relative to best transit TAP pair for
        // tour
        if (modelStructure.getTourModeIsTransit(tour.getTourModeChoice()))
        {

            Arrays.fill(sampleMgraInBoardingTapShed, false);
            Arrays.fill(sampleMgraInAlightingTapShed, false);

            int numAvailableAlternatives = setSoaAvailabilityForTransitTour(s, tour);
            if (numAvailableAlternatives == 0)
            {
                logger.error("no available locations - empty sample.");
                logger.error("best tap pair which is empty: " +  Arrays.deepToString(s.isInboundStop() ? tour.getBestWtwTapPairsIn() : tour.getBestWtwTapPairsOut()));
                throw new RuntimeException();
            }
        }

        // get the array of distances to the half-tour final destination mgra
        // from all MGRAs and set in the dmu object
        if (s.isInboundStop())
        {
            // if inbound, final half-tour destination is the tour origin
            anm.getDistancesToMgra(tour.getTourOrigMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);

            // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourOrigMgra()]);

            // bike logsums from all MGRAs back to tour origin
            if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
            
                Arrays.fill(bikeLogsumToFinalDestFromAllMgras, 0);
            	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

                for (int oMgra = 1; oMgra <= mgraManager.getMaxMgra(); oMgra++)
                {
                	bikeLogsumToFinalDestFromAllMgras[oMgra] = bls.getLogsum(segment,oMgra,tour.getTourOrigMgra());
                }
                stopLocDmuObj.setBikeLogsumsToDestMgra(bikeLogsumToFinalDestFromAllMgras);
            }

        
        } else
        {
            // if outbound, final half-tour destination is the tour destination
            anm.getDistancesToMgra(tour.getTourDestMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);

            // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourDestMgra()]);

            // bike logsums from all MGRAs back to tour origin
            if(modelStructure.getTourModeIsBike(tour.getTourModeChoice())){
            
                Arrays.fill(bikeLogsumToFinalDestFromAllMgras, 0);
            	segment = new BikeLogsumSegment(person.getPersonIsFemale() == 1,tour.getTourPrimaryPurposeIndex() <= 3,s.isInboundStop());

                for (int oMgra = 1; oMgra <= mgraManager.getMaxMgra(); oMgra++)
                {
                	bikeLogsumToFinalDestFromAllMgras[oMgra] = bls.getLogsum(segment,oMgra,tour.getTourDestMgra());
                }
                stopLocDmuObj.setBikeLogsumsToDestMgra(bikeLogsumToFinalDestFromAllMgras);
            }

        
        }

        long check = System.nanoTime();
        if (useNewSoaMethod)
        {
            if (modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()))
            {
                selectSampleOfAlternativesAutoTourNew(s, tour, person, household, loggingSample);
                soaAutoTime += (System.nanoTime() - check);
                numAltsInSample = dcTwoStageModelObject.getNumberofUniqueMgrasInSample();
            } else
            {
                selectSampleOfAlternativesOther(s, tour, person, household, loggingSample);
                soaOtherTime += (System.nanoTime() - check);
                numAltsInSample = altFreqMap.size();
            }
        } else
        {
            if (modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()))
            {
                selectSampleOfAlternativesAutoTour(s, tour, person, household, loggingSample);
                soaAutoTime += (System.nanoTime() - check);
            } else
            {
                selectSampleOfAlternativesOther(s, tour, person, household, loggingSample);
                soaOtherTime += (System.nanoTime() - check);
            }
            numAltsInSample = altFreqMap.size();
        }

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";

        if (household.getDebugChoiceModels())
        {

            choiceModelDescription = "Stop Location Choice";
            decisionMakerLabel = String
                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourPurpose(), tour.getTourModeChoice(), tour.getTourId(),
                            s.getDestPurpose(), (s.getStopId() + 1));
            loggingHeader = String.format("%s for %s", choiceModelDescription, decisionMakerLabel);

            modelLogger.info(" ");
            for (int k = 0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info(loggingHeader);
            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

        }

        check = System.nanoTime();
        setupStopLocationChoiceAlternativeArrays(numAltsInSample, s);
        slsTime += (System.nanoTime() - check);

        int slcModelIndex = -1;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.MANDATORY_CATEGORY)) slcModelIndex = MAND_SLC_MODEL_INDEX;
        else if (tour.getTourPrimaryPurposeIndex() == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX
                || tour.getTourPrimaryPurposeIndex() == ModelStructure.SHOPPING_PRIMARY_PURPOSE_INDEX
                || tour.getTourPrimaryPurposeIndex() == ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_INDEX) slcModelIndex = MAINT_SLC_MODEL_INDEX;
        else slcModelIndex = DISCR_SLC_MODEL_INDEX;

        slcModelArray[slcModelIndex].computeUtilities(stopLocDmuObj,
                stopLocDmuObj.getDmuIndexValues(), sampleAvailability, inSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        int selectedIndex = -1;
        if (slcModelArray[slcModelIndex].getAvailabilityCount() > 0)
        {
            selectedIndex = slcModelArray[slcModelIndex].getChoiceResult(rn);
            chosen = finalSample[selectedIndex];
        }else{        
	       //wu's tempory fix to set chosen stop alternative to origin mgra if no alternative is available-8/27/2014 
	        chosen=tour.getTourOrigMgra();
        }

        // write choice model alternative info to log file
        if (household.getDebugChoiceModels() || chosen < 0)
        {

            if (chosen < 0)
            {

                modelLogger
                        .error("ERROR selecting stop location choice due to no alternatives available.");
                modelLogger
                        .error("setting debug to true and recomputing sample of alternatives selection.");
                modelLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d, StopOrig=%d",
                                        household.getHhId(), person.getPersonNum(),
                                        person.getPersonType(), tour.getTourPurpose(),
                                        tour.getTourModeChoice(), tour.getTourId(),
                                        s.getDestPurpose(), (s.getStopId() + 1), s.getOrig()));

                choiceModelDescription = "Stop Location Choice";
                decisionMakerLabel = String
                        .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopId=%d, StopOrig=%d",
                                household.getHhId(), person.getPersonNum(), person.getPersonType(),
                                tour.getTourPurpose(), tour.getTourModeChoice(), tour.getTourId(),
                                s.getDestPurpose(), (s.getStopId() + 1), s.getOrig());
                loggingHeader = String.format("%s for %s", choiceModelDescription,
                        decisionMakerLabel);

                modelLogger.error(" ");
                for (int k = 0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.error(loggingHeader);
                modelLogger.error(separator);
                modelLogger.error("");
                modelLogger.error("");

                // utilities and probabilities are 0 based.
                double[] utilities = slcModelArray[slcModelIndex].getUtilities();
                double[] probabilities = slcModelArray[slcModelIndex].getProbabilities();

                // availabilities is 1 based.
                boolean[] availabilities = slcModelArray[slcModelIndex].getAvailabilities();

                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();

                modelLogger
                        .error("Person num: " + personNum + ", Person type: " + personTypeString);
                modelLogger
                        .error("Alternative             Availability           Utility       Probability           CumProb");
                modelLogger
                        .error("---------------------   ------------       -----------    --------------    --------------");

                double cumProb = 0.0;
                for (int j = 1; j <= numAltsInSample; j++)
                {

                    int alt = finalSample[j];

                    if (j == chosen) selectedIndex = j;

                    cumProb += probabilities[j - 1];
                    String altString = String.format("%-3d %5d", j, alt);
                    modelLogger.error(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                            availabilities[j], utilities[j - 1], probabilities[j - 1], cumProb));
                }

                modelLogger.error(" ");
                String altString = String.format("%-3d %5d", selectedIndex, -1);
                modelLogger.error(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));

                modelLogger.error(separator);
                modelLogger.error("");
                modelLogger.error("");

                slcModelArray[slcModelIndex].logAlternativesInfo(choiceModelDescription,
                        decisionMakerLabel);
                slcModelArray[slcModelIndex].logSelectionInfo(choiceModelDescription,
                        decisionMakerLabel, rn, chosen);

                // write UEC calculation results to separate model specific log
                // file
                slcModelArray[slcModelIndex].logUECResults(modelLogger, loggingHeader);

                logger.error(String
                        .format("Error for HHID=%d, PersonNum=%d, no available %s stop destination choice alternatives to choose from in choiceModelApplication.",
                                tour.getHhId(), tour.getPersonObject().getPersonNum(),
                                tour.getTourPurpose()));
                throw new RuntimeException();

            }

            // utilities and probabilities are 0 based.
            double[] utilities = slcModelArray[slcModelIndex].getUtilities();
            double[] probabilities = slcModelArray[slcModelIndex].getProbabilities();

            // availabilities is 1 based.
            boolean[] availabilities = slcModelArray[slcModelIndex].getAvailabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger
                    .info("---------------------   ------------       -----------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 1; j <= numAltsInSample; j++)
            {

                int alt = finalSample[j];

                if (j == chosen) selectedIndex = j;

                cumProb += probabilities[j - 1];
                String altString = String.format("%-3d %5d", j, alt);
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j], utilities[j - 1], probabilities[j - 1], cumProb));
            }

            modelLogger.info(" ");
            String altString = String.format("%-3d %5d", selectedIndex, finalSample[selectedIndex]);
            modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                    rn, randomCount));

            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            slcModelArray[slcModelIndex].logAlternativesInfo(choiceModelDescription,
                    decisionMakerLabel);
            slcModelArray[slcModelIndex].logSelectionInfo(choiceModelDescription,
                    decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            slcModelArray[slcModelIndex].logUECResults(modelLogger, loggingHeader);

        }

        return selectedIndex;
    }

    private void setupStopLocationChoiceAlternativeArrays(int numAltsInSample, Stop s)
    {

        stopLocDmuObj.setNumberInSample(numAltsInSample);
        stopLocDmuObj.setSampleOfAlternatives(finalSample);
        stopLocDmuObj.setSlcSoaCorrections(sampleCorrectionFactors);

        // create arrays for ik and kj mode choice logsums for the stop origin,
        // the sample stop location, and the half-tour final destination.
        setupLogsumCalculation(s);

        int category = PURPOSE_CATEGORIES[s.getTour().getTourPrimaryPurposeIndex()];
        ChoiceModelApplication mcModel = mcModelArray[category];

        int halfTourFinalDest = s.isInboundStop() ? s.getTour().getTourOrigMgra() : s.getTour()
                .getTourDestMgra();

        // set the land use data items in the DMU for the stop origin
        mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(s.getOrig()));
        mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(s.getOrig()));
        mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(s.getOrig()));

        for (int i = 1; i <= numAltsInSample; i++)
        {

            int altMgra = finalSample[i];
            mcDmuObject.getDmuIndexValues().setDestZone(altMgra);
            
            // set distances to/from stop anchor points to stop location alternative.
            ikDistance[i] = distanceFromStopOrigToAllMgras[altMgra];
            kjDistance[i] = distanceToFinalDestFromAllMgras[altMgra];

            // set distances from tour anchor points to stop location
            // alternative.
            okDistance[i] = tourOrigToAllMgraDistances[altMgra];
            kdDistance[i] = tourDestToAllMgraDistances[altMgra];

            // set the land use data items in the DMU for the sample location
            mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(altMgra));
            mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(altMgra));
            mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(altMgra));

            mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager
                    .getTaz(altMgra)));

            // for walk-transit tours - if half-tour direction is outbound and
            // stop alternative is in the walk shed, walk and walk-transit
            // should be allowed for ik segments
            // if half-tour direction is inbound and stop alternative is in the
            // walk shed, walk and walk-transit should be allowed for kj
            // segments
            // if half-tour direction is outbound and stop alternative is in the
            // walk shed, walk and walk-transit should be allowed for kj
            // segments
            // if half-tour direction is inbound and stop alternative is in the
            // walk shed, walk and walk-transit should be allowed for ik
            // segments

            // for drive-transit tours - if half-tour direction is outbound and
            // stop alternative is in the drive shed, auto should be allowed for
            // ik segments
            // if half-tour direction is inbound and stop alternative is in the
            // drive shed, auto should be allowed for kj segments
            // if half-tour direction is outbound and stop alternative is in the
            // walk shed, walk and walk-transit should be allowed for kj
            // segments
            // if half-tour direction is inbound and stop alternative is in the
            // walk shed, walk and walk-transit should be allowed for ik
            // segments

            // set values for walk-transit and drive-transit tours according to
            // logic for IK segments
            mcDmuObject.setAutoModeRequiredForTripSegment(false);
            mcDmuObject.setWalkModeAllowedForTripSegment(false);

            mcDmuObject.setSegmentIsIk(true);

            double ikSegment = -999;
            // drive transit tours are handled differently than walk transit
            // tours
            if (modelStructure.getTourModeIsDriveTransit(s.getTour().getTourModeChoice()))
            {

                // if the direction is outbound
                if (!s.isInboundStop())
                {

                    // if the sampled mgra is in the outbound half-tour boarding
                    // tap shed (near tour origin)
                    if (sampleMgraInBoardingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                    // if the sampled mgra is in the outbound half-tour
                    // alighting tap shed (near tour primary destination)
                    if (sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper.setDtwTripMcDmuAttributes(mcDmuObject,s.getOrig(),altMgra,s.getStopPeriod(),
                        		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                    }

                    // if the trip origin and sampled mgra are in the outbound
                    // half-tour alighting tap shed (near tour origin)
                    if (sampleMgraInAlightingTapShed[s.getOrig()]
                            && sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                                s.getStopPeriod(), s.getTour().getPersonObject()
                                        .getHouseholdObject().getDebugChoiceModels());
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                } else
                {
                    // if the sampled mgra is in the inbound half-tour boarding
                    // tap shed (near tour primary destination)
                    if (sampleMgraInBoardingTapShed[altMgra])
                    {
                        logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                                s.getStopPeriod(), s.getTour().getPersonObject()
                                        .getHouseholdObject().getDebugChoiceModels());
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                    // if the sampled mgra is in the inbound half-tour alighting
                    // tap shed (near tour origin)
                    if (sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper.setWtdTripMcDmuAttributes(mcDmuObject,s.getOrig(),altMgra,s.getStopPeriod(),
                        		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                    }

                    // if the trip origin and sampled mgra are in the inbound
                    // half-tour alighting tap shed (near tour origin)
                    if (sampleMgraInAlightingTapShed[s.getOrig()]
                            && sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                }

            } else if (modelStructure.getTourModeIsWalkTransit(s.getTour().getTourModeChoice()))
            { // tour mode is walk-transit

                logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                        s.getStopPeriod(), s.getTour().getPersonObject().getHouseholdObject()
                                .getDebugChoiceModels());

            } else
            {
                logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                logsumHelper.setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
            }
            ikSegment = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra, s.getStopPeriod(),
                    mcModel, mcDmuObject, slcLogger);

            if (ikSegment < -900)
            {
                slcLogger.error("ERROR calculating trip mode choice logsum for "
                        + (s.isInboundStop() ? "inbound" : "outbound")
                        + " stop location choice - ikLogsum = " + ikSegment + ".");
                slcLogger
                        .error("setting debug to true and recomputing ik segment logsum in order to log utility expression results.");

                if (s.isInboundStop()) slcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, TourOrigMGRA=%d, TourDestMGRA=%d, StopPurpose=%s, StopDirection=%s, StopId=%d, NumIBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(), s.getTour().getTourOrigMgra(),s.getTour().getTourDestMgra(),s.getDestPurpose(), "inbound", (s
                                                .getStopId() + 1),
                                        s.getTour().getNumInboundStops() - 1, s.getOrig(), altMgra));
                else slcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d,TourOrigMGRA=%d,TourDestMGRA=%d,StopPurpose=%s, StopDirection=%s, StopId=%d, NumOBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(),s.getTour().getTourOrigMgra(),s.getTour().getTourDestMgra(),s.getDestPurpose(), "outbound", (s
                                                .getStopId() + 1), s.getTour()
                                                .getNumOutboundStops() - 1, s.getOrig(), altMgra));

                mcDmuObject.getDmuIndexValues().setDebug(true);
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(false);
                /* suppress log: Wu
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(true);
                */
                mcDmuObject.getDmuIndexValues().setHHIndex(
                        s.getTour().getPersonObject().getHouseholdObject().getHhId());
                ikSegment = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra,
                        s.getStopPeriod(), mcModel, mcDmuObject, slcLogger);
                mcDmuObject.getDmuIndexValues().setDebug(false);
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(false);

            }

            // store the mode choice probabilities for the segment
            mcCumProbsSegmentIk[i] = logsumHelper.getStoredSegmentCumulativeProbabilities();

            // store the best tap pairs for the segment
            segmentIkBestTapPairs[i] = logsumHelper.getBestTripTaps();

            // set values for walk-transit and drive-transit tours according to
            // logic for KJ segments
            mcDmuObject.setAutoModeRequiredForTripSegment(false);
            mcDmuObject.setWalkModeAllowedForTripSegment(false);

            mcDmuObject.setSegmentIsIk(false);
            // if ( sampleMgraInWalkTransitWalkShed[altMgra] )
            // mcDmuObject.setWalkModeAllowedForTripSegment( true );
            // if ( sampleMgraInDriveTransitWalkShed[altMgra] )
            // mcDmuObject.setWalkModeAllowedForTripSegment( true );

            // if a stop alternative is in the drive shed of a tap, and it's in
            // the inbound direction, the KJ mode HAS TO BE auto.
            // if ( sampleMgraInDriveTransitDriveShed[altMgra] &&
            // s.isInboundStop() )
            // mcDmuObject.setAutoModeRequiredForTripSegment( true );

            double kjSegment = -999;
            // drive transit tours are handled differently than walk transit
            // tours
            if (modelStructure.getTourModeIsDriveTransit(s.getTour().getTourModeChoice()))
            {

                // if the direction is outbound
                if (!s.isInboundStop())
                {

                    if (sampleMgraInBoardingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper.setDtwTripMcDmuAttributes(mcDmuObject,altMgra,halfTourFinalDest,s.getStopPeriod(),
                        		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                    }

                    // if the trip origin is in the outbound half-tour alighting
                    // tap shed (close to tour primary destination)
                    if (sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, altMgra,
                                halfTourFinalDest, s.getStopPeriod(), s.getTour().getPersonObject()
                                        .getHouseholdObject().getDebugChoiceModels());
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                } else
                {

                    if (sampleMgraInBoardingTapShed[altMgra])
                    {
                        logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                        logsumHelper.setWtdTripMcDmuAttributes(mcDmuObject,altMgra,halfTourFinalDest,s.getStopPeriod(),
                        		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                    }

                    // if the trip origin is in the inbound half-tour alighting
                    // tap shed (close to tour origin)
                    if (sampleMgraInAlightingTapShed[altMgra])
                    {
                        logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, altMgra,
                                halfTourFinalDest, s.getStopPeriod(), s.getTour().getPersonObject()
                                        .getHouseholdObject().getDebugChoiceModels());
                        logsumHelper
                                .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                    }

                }

            } else if (modelStructure.getTourModeIsWalkTransit(s.getTour().getTourModeChoice()))
            { // tour mode is walk-transit

                logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, altMgra, halfTourFinalDest,
                        s.getStopPeriod(), s.getTour().getPersonObject().getHouseholdObject()
                                .getDebugChoiceModels());

            } else
            {
                logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                logsumHelper.setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
            }
            kjSegment = logsumHelper.calculateTripMcLogsum(altMgra, halfTourFinalDest,
                    s.getStopPeriod(), mcModel, mcDmuObject, slcLogger);

            if (kjSegment < -900)
            {
                slcLogger.error("ERROR calculating trip mode choice logsum for "
                        + (s.isInboundStop() ? "inbound" : "outbound")
                        + " stop location choice - kjLogsum = " + kjSegment + ".");
                slcLogger
                        .error("setting debug to true and recomputing kj segment logsum in order to log utility expression results.");

                if (s.isInboundStop()) slcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, TourOrigMGRA=%d,TourDestMGRA=%d,StopPurpose=%s, StopDirection=%s, StopId=%d, NumIBStops=%d, AltStopLoc=%d, HalfTourDest=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(), s.getTour().getTourOrigMgra(),s.getTour().getTourDestMgra(),s.getDestPurpose(), "inbound", (s
                                                .getStopId() + 1),
                                        s.getTour().getNumInboundStops() - 1, altMgra,
                                        halfTourFinalDest));
                else slcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, TourOrigMGRA=%d,TourDestMGRA=%d,StopPurpose=%s, StopDirection=%s, StopId=%d, NumOBStops=%d, AltStopLoc=%d, HalfTourDest=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(), s.getTour().getTourOrigMgra(),s.getTour().getTourDestMgra(),s.getDestPurpose(), "outbound", (s
                                                .getStopId() + 1), s.getTour()
                                                .getNumOutboundStops() - 1, altMgra,
                                        halfTourFinalDest));
                
                mcDmuObject.getDmuIndexValues().setDebug(true);
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(false);
                /* suppress log: Wu
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(true);
                */
                mcDmuObject.getDmuIndexValues().setHHIndex(
                        s.getTour().getPersonObject().getHouseholdObject().getHhId());
                kjSegment = logsumHelper.calculateTripMcLogsum(altMgra, halfTourFinalDest,
                        s.getStopPeriod(), mcModel, mcDmuObject, slcLogger);
                mcDmuObject.getDmuIndexValues().setDebug(false);
                mcDmuObject.getHouseholdObject().setDebugChoiceModels(false);
            }

            // store the mode choice probabilities for the segment
            mcCumProbsSegmentKj[i] = logsumHelper.getStoredSegmentCumulativeProbabilities();

            // store the best tap pairs for the segment
            segmentKjBestTapPairs[i] = logsumHelper.getBestTripTaps();

            tripModeChoiceLogsums[i] = ikSegment + kjSegment;
        }

        stopLocDmuObj.setDistancesFromTourOrigMgra(okDistance);
        stopLocDmuObj.setDistancesToTourDestMgra(kdDistance);

        stopLocDmuObj.setDistancesFromOrigMgra(ikDistance);
        stopLocDmuObj.setDistancesToDestMgra(kjDistance);

        stopLocDmuObj.setMcLogsums(tripModeChoiceLogsums);
    }

    private void selectSampleOfAlternativesAutoTourNew(Stop s, Tour tour, Person person,
            Household household, int[] loggingSample)
    {

        int slcSegment = s.getStopPurposeIndex();
        int slcOrigTaz = mgraManager.getTaz(s.getOrig());
        int slcDestTaz = -1;
        if (s.isInboundStop()) slcDestTaz = mgraManager.getTaz(tour.getTourOrigMgra());
        else slcDestTaz = mgraManager.getTaz(tour.getTourDestMgra());

        // get sample of locations and correction factors for sample using the
        // alternate method
        dcTwoStageModelObject.chooseSlcSample(slcOrigTaz, slcDestTaz, slcSegment, sampleSize,
                household.getHhRandom(), household.getDebugChoiceModels());
        int[] tempSample = dcTwoStageModelObject.getUniqueSampleMgras();
        double[] tempFactors = dcTwoStageModelObject.getUniqueSampleMgraCorrectionFactors();
        int numUniqueAlts = dcTwoStageModelObject.getNumberofUniqueMgrasInSample();

        Arrays.fill(sampleAvailability, true);
        Arrays.fill(inSample, 1);

        for (int i = 0; i < numUniqueAlts; i++)
        {
            finalSample[i + 1] = tempSample[i];
            sampleCorrectionFactors[i + 1] = tempFactors[i];
        }
        for (int i = numUniqueAlts + 1; i < finalSample.length; i++)
        {
            finalSample[i] = -1;
            sampleCorrectionFactors[i] = Double.NaN;
            sampleAvailability[i] = false;
            inSample[i] = 0;
        }

    }

    private void selectSampleOfAlternativesAutoTour(Stop s, Tour tour, Person person,
            Household household, int[] loggingSample)
    {

        Logger soaLogger = Logger.getLogger("slcSoaLogger");

        altFreqMap.clear();

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";

        ChoiceModelApplication cm = slcSoaModel[OTHER_STOP_LOC_SOA_SHEET_INDEX];

        if (household.getDebugChoiceModels())
        {
            choiceModelDescription = String
                    .format("Stop Location SOA Choice Model for: stop purpose=%s, direction=%s, stopId=%d, stopOrig=%d",
                            s.getDestPurpose(), s.isInboundStop() ? "inbound" : "outbound",
                            (s.getStopId() + 1), s.getOrig());
            decisionMakerLabel = String
                    .format("HH=%d, persNum=%d, persType=%s, tourId=%d, tourPurpose=%s, tourOrig=%d, tourDest=%d, tourMode=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourId(), tour.getTourPrimaryPurpose(), tour.getTourOrigMgra(),
                            tour.getTourDestMgra(), tour.getTourModeChoice());
            cm.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);
        }

        IndexValues dmuIndex = stopLocDmuObj.getDmuIndexValues();
        dmuIndex.setDebug(household.getDebugChoiceModels());

        // stopLocDmuObj.setTourModeIndex( tour.getTourModeChoice() );
        //
        // // set the size terms array for the stop purpose in the dmu object
        // stopLocDmuObj.setLogSize(
        // getLnSlcSizeTermsForStopPurpose(s.getStopPurposeIndex(), household)
        // );
        //
        // // get the array of distances from the stop origin mgra to all MGRAs
        // and set in the dmu object
        // anm.getDistancesFromMgra( s.getOrig(),
        // distanceFromStopOrigToAllMgras, modelStructure.getTourModeIsSovOrHov(
        // tour.getTourModeChoice() ) );
        // stopLocDmuObj.setDistancesFromOrigMgra(
        // distanceFromStopOrigToAllMgras );
        //
        // // get the array of distances to the half-tour final destination mgra
        // from all MGRAs and set in the dmu object
        if (s.isInboundStop())
        {
            // // if inbound, final half-tour destination is the tour origin
            // anm.getDistancesToMgra( tour.getTourOrigMgra(),
            // distanceToFinalDestFromAllMgras,
            // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() )
            // );
            // stopLocDmuObj.setDistancesToDestMgra(
            // distanceToFinalDestFromAllMgras );
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            // stopLocDmuObj.setOrigDestDistance(
            // distanceFromStopOrigToAllMgras[tour.getTourOrigMgra()] );
            anm.getDistancesToMgra(tour.getTourOrigMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourOrigMgra()]);
            //
            // // not used in UEC to reference matrices, but may be for
            // debugging using $ORIG and $DEST as an expression
            dmuIndex.setOriginZone(mgraManager.getTaz(s.getOrig()));
            dmuIndex.setDestZone(mgraManager.getTaz(tour.getTourOrigMgra()));
        } else
        {
            // // if outbound, final half-tour destination is the tour
            // destination
            // anm.getDistancesToMgra( tour.getTourDestMgra(),
            // distanceToFinalDestFromAllMgras,
            // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() )
            // );
            // stopLocDmuObj.setDistancesToDestMgra(
            // distanceToFinalDestFromAllMgras );
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            // stopLocDmuObj.setOrigDestDistance(
            // distanceFromStopOrigToAllMgras[tour.getTourDestMgra()]);
            anm.getDistancesToMgra(tour.getTourDestMgra(), distanceToFinalDestFromAllMgras,
                    modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()));
            stopLocDmuObj.setDistancesToDestMgra(distanceToFinalDestFromAllMgras);
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            stopLocDmuObj
                    .setOrigDestDistance(distanceFromStopOrigToAllMgras[tour.getTourDestMgra()]);
            //
            // // not used in UEC to reference matrices, but may be for
            // debugging using $ORIG and $DEST as an expression
            dmuIndex.setOriginZone(mgraManager.getTaz(s.getOrig()));
            dmuIndex.setDestZone(mgraManager.getTaz(tour.getTourDestMgra()));
        }

        cm.computeUtilities(stopLocDmuObj, dmuIndex, soaAvailability, soaSample);
        double[] probabilitiesList = cm.getProbabilities();
        double[] cumProbabilitiesList = cm.getCumulativeProbabilities();

        // debug output
        if (household.getDebugChoiceModels())
        {

            // write choice model alternative info to debug log file
            cm.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);

            // write UEC calculation results to separate model specific log file
            loggingHeader = choiceModelDescription + ", " + decisionMakerLabel;

            if (loggingSample == null)
            {
                cm.logUECResultsSpecificAlts(soaLogger, loggingHeader, new int[] {0, s.getOrig(),
                        tour.getTourOrigMgra(), tour.getTourDestMgra()});
                // cm.logUECResults( soaLogger, loggingHeader, 10 );
            } else
            {
                cm.logUECResultsSpecificAlts(soaLogger, loggingHeader, loggingSample);
            }

        }

        // loop over sampleSize, select alternatives based on probabilitiesList,
        // and count frequency of alternatives chosen.
        // may include duplicate alternative selections.

        Random hhRandom = household.getHhRandom();
        int rnCount = household.getHhRandomCount();
        // when household.getHhRandom() was applied, the random count was
        // incremented, assuming a random number would be drawn right away.
        // so let's decrement by 1, then increment the count each time a random
        // number is actually drawn in this method.
        rnCount--;

        int chosenAlt = -1;
        for (int i = 0; i < sampleSize; i++)
        {

            double rn = hhRandom.nextDouble();
            rnCount++;
            chosenAlt = Util.binarySearchDouble(cumProbabilitiesList, rn) + 1;

            // write choice model alternative info to log file
            if (household.getDebugChoiceModels())
            {
                cm.logSelectionInfo(loggingHeader, String.format("rnCount=%d", rnCount), rn,
                        chosenAlt);
            }

            int freq = 0;
            if (altFreqMap.containsKey(chosenAlt)) freq = altFreqMap.get(chosenAlt);
            altFreqMap.put(chosenAlt, (freq + 1));

        }

        // sampleSize random number draws were made from this Random object, so
        // update the count in the hh's Random.
        household.setHhRandomCount(rnCount);

        Arrays.fill(sampleAvailability, true);
        Arrays.fill(inSample, 1);

        // create arrays of the unique chosen alternatives and the frequency
        // with which those alternatives were chosen.
        Iterator<Integer> it = altFreqMap.keySet().iterator();
        int k = 0;
        while (it.hasNext())
        {

            int alt = it.next();
            int freq = altFreqMap.get(alt);

            double prob = 0;
            prob = probabilitiesList[alt - 1];

            finalSample[k + 1] = alt;
            sampleCorrectionFactors[k + 1] = Math.log((double) freq / prob);

            k++;
        }

        while (k < sampleSize)
        {
            finalSample[k + 1] = -1;
            sampleCorrectionFactors[k + 1] = Double.NaN;
            sampleAvailability[k + 1] = false;
            inSample[k + 1] = 0;
            k++;
        }

    }

    private void selectSampleOfAlternativesOther(Stop s, Tour tour, Person person,
            Household household, int[] loggingSample)
    {

        Logger soaLogger = Logger.getLogger("slcSoaLogger");

        altFreqMap.clear();

        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";

        ChoiceModelApplication cm;
        if (modelStructure.getTourModeIsWalk(tour.getTourModeChoice())) cm = slcSoaModel[WALK_STOP_LOC_SOA_SHEET_INDEX];
        else if (modelStructure.getTourModeIsBike(tour.getTourModeChoice())) cm = slcSoaModel[BIKE_STOP_LOC_SOA_SHEET_INDEX];
        else cm = slcSoaModel[OTHER_STOP_LOC_SOA_SHEET_INDEX];

        if (household.getDebugChoiceModels())
        {
            choiceModelDescription = String
                    .format("Stop Location SOA Choice Model for: stop purpose=%s, direction=%s, stopId=%d, stopOrig=%d",
                            s.getDestPurpose(), s.isInboundStop() ? "inbound" : "outbound",
                            (s.getStopId() + 1), s.getOrig());
            decisionMakerLabel = String
                    .format("HH=%d, persNum=%d, persType=%s, tourId=%d, tourPurpose=%s, tourOrig=%d, tourDest=%d, tourMode=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourId(), tour.getTourPrimaryPurpose(), tour.getTourOrigMgra(),
                            tour.getTourDestMgra(), tour.getTourModeChoice());
            cm.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);
        }

        IndexValues dmuIndex = stopLocDmuObj.getDmuIndexValues();
        dmuIndex.setDebug(household.getDebugChoiceModels());

        // stopLocDmuObj.setTourModeIndex( tour.getTourModeChoice() );
        //
        // // set the size terms array for the stop purpose in the dmu object
        // stopLocDmuObj.setLogSize(
        // getLnSlcSizeTermsForStopPurpose(s.getStopPurposeIndex(), household)
        // );
        //
        // // get the array of distances from the stop origin mgra to all MGRAs
        // and set in the dmu object
        // anm.getDistancesFromMgra( s.getOrig(),
        // distanceFromStopOrigToAllMgras, modelStructure.getTourModeIsSovOrHov(
        // tour.getTourModeChoice() ) );
        // stopLocDmuObj.setDistancesFromOrigMgra(
        // distanceFromStopOrigToAllMgras );
        //
        // // if tour mode is transit, set availablity of location alternatives
        // based on transit accessibility relative to best transit TAP pair for
        // tour
        // if ( modelStructure.getTourModeIsTransit( tour.getTourModeChoice() )
        // ) {
        // int numAvailableAlternatives = setSoaAvailabilityForTransitTour(s,
        // tour);
        // if ( numAvailableAlternatives == 0 ) {
        // logger.error( "no available locations - empty sample." );
        // throw new RuntimeException();
        // }
        // }
        //
        // // get the array of distances to the half-tour final destination mgra
        // from all MGRAs and set in the dmu object
        if (s.isInboundStop())
        {
            // // if inbound, final half-tour destination is the tour origin
            // anm.getDistancesToMgra( tour.getTourOrigMgra(),
            // distanceToFinalDestFromAllMgras,
            // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() )
            // );
            // stopLocDmuObj.setDistancesToDestMgra(
            // distanceToFinalDestFromAllMgras );
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            // stopLocDmuObj.setOrigDestDistance(
            // distanceFromStopOrigToAllMgras[tour.getTourOrigMgra()] );
            //
            // // not used in UEC to reference matrices, but may be for
            // debugging using $ORIG and $DEST as an expression
            dmuIndex.setOriginZone(mgraManager.getTaz(s.getOrig()));
            dmuIndex.setDestZone(mgraManager.getTaz(tour.getTourOrigMgra()));
        } else
        {
            // // if outbound, final half-tour destination is the tour
            // destination
            // anm.getDistancesToMgra( tour.getTourDestMgra(),
            // distanceToFinalDestFromAllMgras,
            // modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() )
            // );
            // stopLocDmuObj.setDistancesToDestMgra(
            // distanceToFinalDestFromAllMgras );
            //
            // // set the distance from the stop origin to the final half-tour
            // destination
            // stopLocDmuObj.setOrigDestDistance(
            // distanceFromStopOrigToAllMgras[tour.getTourDestMgra()]);
            //
            // // not used in UEC to reference matrices, but may be for
            // debugging using $ORIG and $DEST as an expression
            dmuIndex.setOriginZone(mgraManager.getTaz(s.getOrig()));
            dmuIndex.setDestZone(mgraManager.getTaz(tour.getTourDestMgra()));
        }

        cm.computeUtilities(stopLocDmuObj, dmuIndex, soaAvailability, soaSample);
        double[] probabilitiesList = cm.getProbabilities();
        double[] cumProbabilitiesList = cm.getCumulativeProbabilities();

        // debug output
        if (household.getDebugChoiceModels())
        {

            // write choice model alternative info to debug log file
            cm.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);

            // write UEC calculation results to separate model specific log file
            loggingHeader = choiceModelDescription + ", " + decisionMakerLabel;

            if (loggingSample == null)
            {
                cm.logUECResultsSpecificAlts(soaLogger, loggingHeader, new int[] {0, s.getOrig(),
                        tour.getTourOrigMgra(), tour.getTourDestMgra()});
                // cm.logUECResults( soaLogger, loggingHeader, 10 );
            } else
            {
                cm.logUECResultsSpecificAlts(soaLogger, loggingHeader, loggingSample);
            }

        }

        // loop over sampleSize, select alternatives based on probabilitiesList,
        // and count frequency of alternatives chosen.
        // may include duplicate alternative selections.

        Random hhRandom = household.getHhRandom();
        int rnCount = household.getHhRandomCount();
        // when household.getHhRandom() was applied, the random count was
        // incremented, assuming a random number would be drawn right away.
        // so let's decrement by 1, then increment the count each time a random
        // number is actually drawn in this method.
        rnCount--;

        // log degenerative cases
        if (cm.getAvailabilityCount() == 0)
        {
            Logger badSlcLogger = Logger.getLogger("badSlc");

            choiceModelDescription = String
                    .format("Stop Location SOA Choice Model for: stop purpose=%s, direction=%s, stopId=%d, stopOrig=%d",
                            s.getDestPurpose(), s.isInboundStop() ? "inbound" : "outbound",
                            (s.getStopId() + 1), s.getOrig());
            decisionMakerLabel = String
                    .format("HH=%d, persNum=%d, persType=%s, tourId=%d, tourPurpose=%s, tourOrig=%d, tourDest=%d, tourMode=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourId(), tour.getTourPrimaryPurpose(), tour.getTourOrigMgra(),
                            tour.getTourDestMgra(), tour.getTourModeChoice());
            loggingHeader = choiceModelDescription + ", " + decisionMakerLabel;

            badSlcLogger.info("....... Start Logging .......");
            badSlcLogger
                    .info("setting stop location sample to be an array with 1 element - just the stop origin mgra.");
            badSlcLogger.info("");

            household.logHouseholdObject(
                    "Stop Location Choice for trip: HH_" + household.getHhId() + ", Pers_"
                            + tour.getPersonObject().getPersonNum() + ", Tour Purpose_"
                            + tour.getTourPurpose() + ", Tour_" + tour.getTourId()
                            + ", Tour Purpose_" + tour.getTourPurpose() + ", Stop_"
                            + (s.getStopId() + 1), badSlcLogger);
            household.logPersonObject("Stop Location Choice for person "
                    + tour.getPersonObject().getPersonNum(), badSlcLogger, tour.getPersonObject());
            household.logTourObject("Stop Location Choice for tour " + tour.getTourId(),
                    badSlcLogger, tour.getPersonObject(), tour);
            household.logStopObject("Stop Location Choice for stop " + (s.getStopId() + 1),
                    badSlcLogger, s, modelStructure);

            badSlcLogger.info(decisionMakerLabel + " has no available alternatives for "
                    + choiceModelDescription + ".");
            badSlcLogger.info("Logging StopLocation SOA Choice utility calculations for: stopOrig="
                    + s.getOrig() + ", tourOrig=" + tour.getTourOrigMgra() + ", and tourDest="
                    + tour.getTourDestMgra() + ".");
            cm.logUECResultsSpecificAlts(badSlcLogger, loggingHeader, new int[] {0, s.getOrig(),
                    tour.getTourOrigMgra(), tour.getTourDestMgra()});

            int chosenAlt = s.getOrig();
            probabilitiesList[chosenAlt - 1] = 1.0;
            for (int j = chosenAlt - 1; j < cumProbabilitiesList.length; j++)
                cumProbabilitiesList[j] = 1.0;

            double sum = 0;
            double epsilon = .0000001;
            for (int j = 0; j < probabilitiesList.length; j++)
            {
                sum += probabilitiesList[j];
                if (!(Math.abs(sum - cumProbabilitiesList[j]) < epsilon) || sum > 1.0)
                {
                    badSlcLogger.info("error condition found!  sum=" + sum + ", j=" + j
                            + ", cumProbabilitiesList[j]=" + cumProbabilitiesList[j]);
                    badSlcLogger.info("....... End Logging .......");
                    throw new RuntimeException();
                }
            }

            badSlcLogger.info("....... End Logging .......");
            badSlcLogger.info("");
            badSlcLogger.info("");
        }

        int chosenAlt = -1;
        for (int i = 0; i < sampleSize; i++)
        {

            double rn = hhRandom.nextDouble();
            rnCount++;
            chosenAlt = Util.binarySearchDouble(cumProbabilitiesList, rn) + 1;

            // write choice model alternative info to log file
            if (household.getDebugChoiceModels())
            {
                cm.logSelectionInfo(loggingHeader, String.format("rnCount=%d", rnCount), rn,
                        chosenAlt);
            }

            int freq = 0;
            if (altFreqMap.containsKey(chosenAlt)) freq = altFreqMap.get(chosenAlt);
            altFreqMap.put(chosenAlt, (freq + 1));

        }

        // sampleSize random number draws were made from this Random object, so
        // update the count in the hh's Random.
        household.setHhRandomCount(rnCount);

        Arrays.fill(sampleAvailability, true);
        Arrays.fill(inSample, 1);

        // create arrays of the unique chosen alternatives and the frequency
        // with which those alternatives were chosen.
        Iterator<Integer> it = altFreqMap.keySet().iterator();
        int k = 0;
        while (it.hasNext())
        {

            int alt = it.next();
            int freq = altFreqMap.get(alt);

            double prob = 0;
            prob = probabilitiesList[alt - 1];

            finalSample[k + 1] = alt;
            sampleCorrectionFactors[k + 1] = Math.log((double) freq / prob);

            k++;
        }

        while (k < sampleSize)
        {
            finalSample[k + 1] = -1;
            sampleCorrectionFactors[k + 1] = Double.NaN;
            sampleAvailability[k + 1] = false;
            inSample[k + 1] = 0;
            k++;
        }

        // if the sample was determined for a transit tour, the sample and
        // availability arrays for the full set of SOA alternatives need to be
        // restored.
        if (modelStructure.getTourModeIsTransit(tour.getTourModeChoice()))
        {
            for (int i = 0; i < soaSample.length; i++)
            {
                soaSample[i] = soaSampleBackup[i];
                soaAvailability[i] = soaAvailabilityBackup[i];
            }
        }

    }

    private void setupLogsumCalculation(Stop s)
    {

        Tour t = s.getTour();
        Person p = t.getPersonObject();
        Household hh = p.getHouseholdObject();

        mcDmuObject.setHouseholdObject(hh);
        mcDmuObject.setPersonObject(p);
        mcDmuObject.setTourObject(t);

        int tourMode = t.getTourModeChoice();
        int origMgra = s.getOrig();

        mcDmuObject.getDmuIndexValues().setHHIndex(hh.getHhId());
        mcDmuObject.getDmuIndexValues().setZoneIndex(hh.getHhMgra());
        mcDmuObject.getDmuIndexValues().setOriginZone(origMgra);
        mcDmuObject.getDmuIndexValues().setDebug(hh.getDebugChoiceModels());

        mcDmuObject.setOutboundStops(t.getOutboundStops() == null ? 0
                : t.getOutboundStops().length - 1);
        mcDmuObject.setInboundStops(t.getInboundStops() == null ? 0
                : t.getInboundStops().length - 1);

        mcDmuObject.setTripOrigIsTourDest(s.isInboundStop() && s.getStopId() == 0 ? 1 : 0);
        mcDmuObject.setTripDestIsTourDest(!s.isInboundStop()
                && ((s.getStopId() + 1) == (t.getNumOutboundStops() - 1)) ? 1 : 0);

        mcDmuObject.setFirstTrip(0);
        mcDmuObject.setLastTrip(0);
        if (s.isInboundStop())
        {
            mcDmuObject.setOutboundHalfTourDirection(0);
            // compare stopId (0-based, so add 1) with number of stops (stops
            // array length - 1); if last stop, set flag to 1, otherwise 0.
            mcDmuObject.setLastTrip(((s.getStopId() + 1) == (t.getNumInboundStops() - 1)) ? 1 : 0);
        } else
        {
            mcDmuObject.setOutboundHalfTourDirection(1);
            // if first stopId (0-based), set flag to 1, otherwise 0.
            mcDmuObject.setFirstTrip(s.getStopId() == 0 ? 1 : 0);
        }

        mcDmuObject.setJointTour(t.getTourCategory().equalsIgnoreCase(
                ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ? 1 : 0);
        mcDmuObject
                .setEscortTour(t.getTourPrimaryPurposeIndex() == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX ? 1
                        : 0);

        mcDmuObject.setIncomeInDollars(hh.getIncomeInDollars());
        mcDmuObject.setAdults(hh.getNumPersons18plus());
        mcDmuObject.setAutos(hh.getAutoOwnershipModelResult());
        mcDmuObject.setAge(p.getAge());
        mcDmuObject.setHhSize(hh.getHhSize());
        mcDmuObject.setPersonIsFemale(p.getPersonIsFemale());

        mcDmuObject.setTourModeIsDA(modelStructure.getTourModeIsSov(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsS2(modelStructure.getTourModeIsS2(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsS3(modelStructure.getTourModeIsS3(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsWalk(modelStructure.getTourModeIsWalk(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsBike(modelStructure.getTourModeIsBike(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsWTran(modelStructure.getTourModeIsWalkLocal(tourMode)
                || modelStructure.getTourModeIsWalkPremium(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsPnr(modelStructure.getTourModeIsPnr(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsKnr(modelStructure.getTourModeIsKnr(tourMode) ? 1 : 0);
        mcDmuObject.setTourModeIsSchBus(modelStructure.getTourModeIsSchoolBus(tourMode) ? 1 : 0);

        mcDmuObject
                .setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(origMgra)));

        mcDmuObject.setDepartPeriod(t.getTourDepartPeriod());
        mcDmuObject.setArrivePeriod(t.getTourArrivePeriod());
        mcDmuObject.setTripPeriod(s.getStopPeriod());

        double reimbursePct = mcDmuObject.getPersonObject().getParkingReimbursement();
        mcDmuObject.setReimburseProportion( reimbursePct );
        
    }

    /**
     * determine if each indexed mgra has transit access to the best tap pairs
     * for the tour create an array with 1 if the mgra indexed has at least one
     * TAP within walk egress distance of the mgra or zero if no walk TAPS exist
     * for the mgra.
     */
    private int setSoaAvailabilityForTransitTour(Stop s, Tour t)
    {

        int availableCount = 0;
        int[][] bestTaps = null;

        if (s.isInboundStop())
        {

            if (modelStructure.getTourModeIsWalkTransit(t.getTourModeChoice())) bestTaps = t
                    .getBestWtwTapPairsIn();
            else bestTaps = t.getBestWtdTapPairsIn();

            // loop through mgras and determine if they are available as a stop
            // location
            ArrayList<Integer> mgras = mgraManager.getMgras();
            for (int alt : mgras)
            {
                // if alternative mgra is unavailable because it has no size, no
                // need to check its accessibility
                // if ( ! soaAvailability[alt] )
                // continue;

                boolean accessible = false;
                for (int[] tapPair : bestTaps)
                {
                    if (tapPair == null) continue;

                    if (modelStructure.getTourModeIsWalkTransit(t.getTourModeChoice()))
                    {
                        // if alternative location mgra is accessible by walk to
                        // any of the best inbound boarding taps, AND it's
                        // accessible by walk to the stop origin, it's
                        // available.
                        if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[0])
                                && mgraManager.getMgrasAreWithinWalkDistance(s.getOrig(), alt)
                                && earlierTripWasLocatedInAlightingTapShed == false)
                        {
                            accessible = true;
                            sampleMgraInBoardingTapShed[alt] = true;
                        } else if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[1])
                                && mgraManager.getMgrasAreWithinWalkDistance(alt,
                                        t.getTourOrigMgra()))
                        {
                            // if alternative location mgra is accessible by
                            // walk to
                            // any of the best inbound alighting taps, AND it's
                            // accessible by walk to the tour origin, it's
                            // available.
                            accessible = true;
                            sampleMgraInAlightingTapShed[alt] = true;
                        }
                    } else
                    {
                        // if alternative location mgra is accessible by walk to
                        // any of the best origin taps, AND it's accessible by
                        // walk to the stop origin, it's available.
                        if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[0])
                                && mgraManager.getMgrasAreWithinWalkDistance(s.getOrig(), alt)
                                && earlierTripWasLocatedInAlightingTapShed == false)
                        {
                            accessible = true;
                            sampleMgraInBoardingTapShed[alt] = true;
                        } else if (mgraManager.getTapIsDriveAccessibleFromMgra(alt, tapPair[1]))
                        {
                            // if alternative location mgra is accessible by
                            // drive to any of the best destination taps
                            // it's available.
                            accessible = true;
                            sampleMgraInAlightingTapShed[alt] = true;
                        }
                    }

                    if (accessible) break;
                }

                if (accessible)
                {
                    availableCount++;
                } else
                {
                    soaSample[alt] = 0;
                    soaAvailability[alt] = false;
                }

            }

        } else
        {
            if (modelStructure.getTourModeIsWalkTransit(t.getTourModeChoice())) bestTaps = t
                    .getBestWtwTapPairsOut();
            else bestTaps = t.getBestDtwTapPairsOut();

            // loop through mgras and determine if they have walk egress
            ArrayList<Integer> mgras = mgraManager.getMgras();
            for (int alt : mgras)
            {
                // if alternative mgra is unavailable because it has no size, no
                // need to check its accessibility
                // if ( ! soaAvailability[alt] )
                // continue;

                // check whether any of the outbound dtw boarding taps or best
                // wtw alighting taps are in the set of walk accessible TAPs for
                // the alternative mgra.
                // if not, the alternative is not available.
                boolean accessible = false;
                for (int[] tapPair : bestTaps)
                {
                    if (tapPair == null) continue;

                    if (modelStructure.getTourModeIsWalkTransit(t.getTourModeChoice()))
                    {
                        // if alternative location mgra is accessible by walk to
                        // any of the best origin taps, AND it's accessible by
                        // walk to the stop origin, it's available.
                        if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[0])
                                && mgraManager.getMgrasAreWithinWalkDistance(s.getOrig(), alt)
                                && earlierTripWasLocatedInAlightingTapShed == false)
                        {
                            accessible = true;
                            sampleMgraInBoardingTapShed[alt] = true;
                        } else if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[1])
                                && mgraManager.getMgrasAreWithinWalkDistance(alt,
                                        t.getTourDestMgra()))
                        {
                            // if alternative location mgra is accessible by
                            // walk to any of the best destination taps,
                            // AND it's accessible by walk to the tour
                            // primary destination, it's available.
                            accessible = true;
                            sampleMgraInAlightingTapShed[alt] = true;
                        }
                    } else
                    {
                        // if alternative location mgra is accessible by drive
                        // to any of the best origin taps, it's available.
                        if (mgraManager.getTapIsDriveAccessibleFromMgra(alt, tapPair[0])
                                && earlierTripWasLocatedInAlightingTapShed == false)
                        {
                            accessible = true;
                            sampleMgraInBoardingTapShed[alt] = true;
                        } else if (mgraManager.getTapIsWalkAccessibleFromMgra(alt, tapPair[1])
                                && mgraManager.getMgrasAreWithinWalkDistance(alt,
                                        t.getTourDestMgra()))
                        {
                            // if alternative location mgra is accessible by
                            // walk to any of the best destination taps, AND
                            // it's accessible by walk to the tour primary
                            // destination, it's available.
                            accessible = true;
                            sampleMgraInAlightingTapShed[alt] = true;
                        }
                    }

                    if (accessible) break;

                }
                if (accessible)
                {
                    availableCount++;
                } else
                {
                    soaSample[alt] = 0;
                    soaAvailability[alt] = false;
                }

            }

        }

        return availableCount;
    }

    /**
     * create an array with 1 if the mgra indexed has at least one TAP within
     * walk egress distance of the mgra or zero if no walk TAPS exist for the
     * mgra. private void createWalkTransitAvailableArray() {
     * 
     * ArrayList<Integer> mgras = mgraManager.getMgras(); int maxMgra =
     * mgraManager.getMaxMgra();
     * 
     * walkTransitAvailable = new int[maxMgra+1];
     * 
     * // loop through mgras and determine if they have walk egress for (int alt
     * : mgras) {
     * 
     * // get the TAP set within walk egress distance of the stop location
     * alternative. int[] aMgraSet =
     * mgraManager.getMgraWlkTapsDistArray()[alt][0];
     * 
     * // set to 1 if the list of TAPS with walk accessible egress to alt is not
     * empty; 0 otherwise if ( aMgraSet != null && aMgraSet.length > 0 )
     * walkTransitAvailable[alt] = 1;
     * 
     * }
     * 
     * stopLocDmuObj.setWalkTransitAvailable( walkTransitAvailable ); }
     */

    /**
     * Do a monte carlo selection from the array of stored mode choice
     * cumulative probabilities (0 based array). The probabilities were saved at
     * the time the stop location alternative segment mode choice logsums were
     * calculated. If the stop is not the last stop for the half-tour, the IK
     * segment probabilities are passed in. If the stop is the last stop, the KJ
     * probabilities are passeed in.
     * 
     * @param household
     *            object frim which to get the Random object.
     * @param props
     *            is the array of stored mode choice probabilities - 0s based
     *            array.
     * 
     * @return the selected mode choice alternative from [1,...,numMcAlts].
     */
    private int selectModeFromProbabilities(Stop s, double[] cumProbs)
    {

        Household household = s.getTour().getPersonObject().getHouseholdObject();

        int selectedModeAlt = -1;
        double rn = household.getHhRandom().nextDouble();
        int randomCount = household.getHhRandomCount();

        int numAvailAlts = 0;
        double sumProb = 0.0;
        for (int i = 0; i < cumProbs.length; i++)
        {
            double tempProb = cumProbs[i] - sumProb;
            sumProb += tempProb;

            if (tempProb > 0) numAvailAlts++;

            if (rn < cumProbs[i])
            {
                selectedModeAlt = i + 1;
                break;
            }
        }

        if (household.getDebugChoiceModels() || selectedModeAlt < 0
                || numAvailAlts >= availAltsToLog)
        {

            // set the number of available alts to log value to a large number
            // so no more get logged.
            if (numAvailAlts >= availAltsToLog)
            {
                Person person = s.getTour().getPersonObject();
                Tour tour = s.getTour();
                smcLogger
                        .info("Monte Carlo selection for determining Mode Choice from Probabilities for stop with more than "
                                + availAltsToLog + " mode alts available.");
                smcLogger.info("HHID=" + household.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                smcLogger.info("StopID="
                        + (s.getStopId() + 1)
                        + " of "
                        + (s.isInboundStop() ? tour.getNumInboundStops() - 1 : tour
                                .getNumOutboundStops() - 1) + " stops, inbound="
                        + s.isInboundStop() + ", stopPurpose=" + s.getDestPurpose()
                        + ", stopDepart=" + s.getStopPeriod() + ", stopOrig=" + s.getOrig()
                        + ", stopDest=" + s.getDest());
                availAltsToLog = 9999;
            }

            smcLogger.info("");
            smcLogger.info("");
            String separator = "";
            for (int k = 0; k < 60; k++)
                separator += "+";
            smcLogger.info(separator);

            smcLogger
                    .info("Alternative                      Availability           Utility       Probability           CumProb");
            smcLogger
                    .info("---------------------            ------------       -----------    --------------    --------------");

            sumProb = 0.0;
            for (int j = 0; j < cumProbs.length; j++)
            {
                String altString = String.format("%-3d %-25s", j + 1, "");
                double tempProb = cumProbs[j] - sumProb;
                smcLogger.info(String.format("%-30s%15s%18s%18.6e%18.6e", altString, "", "",
                        tempProb, cumProbs[j]));
                sumProb += tempProb;
            }

            if (selectedModeAlt < 0)
            {
                smcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt,
                        "no MC alt available");
                smcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
                throw new RuntimeException();
            } else
            {
                smcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt, "");
                smcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
            }

            smcLogger.info(separator);
            smcLogger.info("");
            smcLogger.info("");

        }

        // if this statement is reached, there's a problem with the cumulative
        // probabilities array, so return -1.
        return selectedModeAlt;
    }

    /**
     * This method is taken from the setupStopLocationChoiceAlternativeArrays(),
     * except that the stop location choice dmu attributes are not set and the
     * logsum calculation setup is done only for the selected stop location
     * alternative.
     * 
     * @param stop
     *            object representing the half-tour.
     * 
     * @return the selected mode choice alternative from [1,...,numMcAlts].
     */
    private int getHalfTourModeChoice(Stop s)
    {

        Household hh = s.getTour().getPersonObject().getHouseholdObject();

        // create arrays for ik and kj mode choice logsums for the stop origin,
        // the sample stop location, and the half-tour final destination.
        setupLogsumCalculation(s);

        int category = PURPOSE_CATEGORIES[s.getTour().getTourPrimaryPurposeIndex()];
        ChoiceModelApplication mcModel = mcModelArray[category];

        int altMgra = s.getDest();
        mcDmuObject.getDmuIndexValues().setDestZone(altMgra);
        
        // set the mode choice attributes for the sample location
        mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(altMgra));
        mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(altMgra));
        mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(altMgra));

        mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager
                .getTaz(altMgra)));

        mcDmuObject.setAutoModeRequiredForTripSegment(false);
        mcDmuObject.setWalkModeAllowedForTripSegment(false);

        if (hh.getDebugChoiceModels())
        {
            smcLogger.info("LOGSUM calculation for determining Mode Choice Probabilities for "
                    + (s.isInboundStop() ? "INBOUND" : "OUTBOUND") + " half-tour with no stops.");
            smcLogger.info("HHID=" + hh.getHhId() + ", persNum="
                    + s.getTour().getPersonObject().getPersonNum() + ", tourPurpose="
                    + s.getTour().getTourPrimaryPurpose() + ", tourId=" + s.getTour().getTourId()
                    + ", tourMode=" + s.getTour().getTourModeChoice());
            smcLogger.info("StopID=" + (s.getStopId() + 1) + ", inbound=" + s.isInboundStop()
                    + ", stopPurpose=" + s.getDestPurpose() + ", stopDepart=" + s.getStopPeriod()
                    + ", stopOrig=" + s.getOrig() + ", stopDest=" + s.getDest());
        }

        if (modelStructure.getTourModeIsDriveTransit(s.getTour().getTourModeChoice()))
        {

            logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);

            if (s.isInboundStop()) logsumHelper.setWtdTripMcDmuAttributesForBestTapPairs(
                    mcDmuObject, s.getOrig(), altMgra, s.getStopPeriod(), s.getTour()
                            .getBestWtdTapPairsIn(), s.getTour().getPersonObject()
                            .getHouseholdObject().getDebugChoiceModels());
            else logsumHelper.setDtwTripMcDmuAttributesForBestTapPairs(mcDmuObject, s.getOrig(),
                    altMgra, s.getStopPeriod(), s.getTour().getBestDtwTapPairsOut(), s.getTour()
                            .getPersonObject().getHouseholdObject().getDebugChoiceModels());

        } else
        {

            logsumHelper.setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());

            if (s.isInboundStop()) logsumHelper.setWtwTripMcDmuAttributesForBestTapPairs(
                    mcDmuObject, s.getOrig(), altMgra, s.getStopPeriod(), s.getTour()
                            .getBestWtwTapPairsIn(), s.getTour().getPersonObject()
                            .getHouseholdObject().getDebugChoiceModels());
            else logsumHelper.setWtwTripMcDmuAttributesForBestTapPairs(mcDmuObject, s.getOrig(),
                    altMgra, s.getStopPeriod(), s.getTour().getBestWtwTapPairsOut(), s.getTour()
                            .getPersonObject().getHouseholdObject().getDebugChoiceModels());

        }
        double logsum = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra, s.getStopPeriod(),
                mcModel, mcDmuObject, smcLogger);

        double rn = hh.getHhRandom().nextDouble();
        int randomCount = hh.getHhRandomCount();

        int selectedModeAlt = -1;
        if (mcModel.getAvailabilityCount() > 0)
        {
            selectedModeAlt = mcModel.getChoiceResult(rn);
        }

        if (hh.getDebugChoiceModels() || selectedModeAlt < 0
                || mcModel.getAvailabilityCount() >= availAltsToLog)
        {

            // set the number of available alts to log value to a large number
            // so no more get logged.
            if (selectedModeAlt < 0 || mcModel.getAvailabilityCount() >= availAltsToLog)
            {
                Person person = s.getTour().getPersonObject();
                Tour tour = s.getTour();
                if (mcModel.getAvailabilityCount() >= availAltsToLog)
                {
                    availAltsToLog = 9999;
                    smcLogger
                            .info("Logsum calculation for determining Mode Choice for half-tour more than "
                                    + availAltsToLog + " mode alts available.");
                } else
                {
                    smcLogger
                            .info("Logsum calculation for determining Mode Choice for half-tour with no stops.");
                }
                smcLogger.info("HHID=" + hh.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                smcLogger.info("StopID="
                        + (s.getStopId() + 1)
                        + " of "
                        + (s.isInboundStop() ? tour.getNumInboundStops() - 1 : tour
                                .getNumOutboundStops() - 1) + " stops, inbound="
                        + s.isInboundStop() + ", stopPurpose=" + s.getDestPurpose()
                        + ", stopDepart=" + s.getStopPeriod() + ", stopOrig=" + s.getOrig()
                        + ", stopDest=" + s.getDest());
            }

            // altNames, utilities and probabilities are 0 based.
            String[] altNames = mcModel.getAlternativeNames();
            double[] utilities = mcModel.getUtilities();
            double[] probabilities = mcModel.getProbabilities();

            // availabilities is 1 based.
            boolean[] availabilities = mcModel.getAvailabilities();

            smcLogger.info("");
            smcLogger.info("");
            String separator = "";
            for (int k = 0; k < 60; k++)
                separator += "+";
            smcLogger.info(separator);

            smcLogger
                    .info("Alternative                      Availability           Utility       Probability           CumProb");
            smcLogger
                    .info("---------------------            ------------       -----------    --------------    --------------");

            double cumProb = 0.0;
            for (int j = 0; j < utilities.length; j++)
            {
                cumProb += probabilities[j];
                String altString = String.format("%-3d %-25s", j + 1, altNames[j]);
                smcLogger.info(String.format("%-30s%15s%18.6e%18.6e%18.6e", altString,
                        availabilities[j + 1], utilities[j], probabilities[j], cumProb));
            }

            if (selectedModeAlt < 0)
            {
                smcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt,
                        "no MC alt available");
                smcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
            } else
            {
                smcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt,
                        altNames[selectedModeAlt - 1]);
                smcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
            }

            smcLogger.info(separator);
            smcLogger.info("");
            smcLogger.info("");

            if (logsum < -900 || selectedModeAlt < 0)
            {
                if (logsum < -900 || selectedModeAlt < 0) smcLogger
                        .error("ERROR calculating trip mode choice logsum for "
                                + (s.isInboundStop() ? "inbound" : "outbound")
                                + " half-tour with no stops - ikLogsum = " + logsum + ".");
                else smcLogger.error("No half-tour mode choice alternatives available "
                        + (s.isInboundStop() ? "inbound" : "outbound")
                        + " half-tour with no stops - ikLogsum = " + logsum + ".");
                smcLogger
                        .error("setting debug to true and recomputing half-tour logsum in order to log utility expression results.");

                if (s.isInboundStop()) smcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopDirection=%s, StopId=%d, NumIBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(), s.getDestPurpose(), "inbound", (s
                                                .getStopId() + 1),
                                        s.getTour().getNumInboundStops() - 1, s.getOrig(), altMgra));
                else smcLogger
                        .error(String
                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopDirection=%s, StopId=%d, NumOBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                        s.getTour().getPersonObject().getHouseholdObject()
                                                .getHhId(), s.getTour().getPersonObject()
                                                .getPersonNum(), s.getTour().getPersonObject()
                                                .getPersonType(), s.getTour().getTourPurpose(), s
                                                .getTour().getTourModeChoice(), s.getTour()
                                                .getTourId(), s.getDestPurpose(), "outbound", (s
                                                .getStopId() + 1), s.getTour()
                                                .getNumOutboundStops() - 1, s.getOrig(), altMgra));

                mcDmuObject.getDmuIndexValues().setDebug(true);
                mcDmuObject.getDmuIndexValues().setHHIndex(
                        s.getTour().getPersonObject().getHouseholdObject().getHhId());
                logsum = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra,
                        s.getStopPeriod(), mcModel, mcDmuObject, smcLogger);
                mcDmuObject.getDmuIndexValues().setDebug(false);

                // throw new RuntimeException();
            }

        }

        return selectedModeAlt;
    }

    private void setOutboundTripDepartTimes(Stop[] stops)
    {

        // these stops are in outbound direction
        int halfTourDirection = 0;

        for (int i = 0; i < stops.length; i++)
        {

            // if tour depart and arrive periods are the same, set same values
            // for the stops
            Stop stop = stops[i];
            Tour tour = stop.getTour();
            Person person = tour.getPersonObject();
            Household household = person.getHouseholdObject();
            if (tour.getTourArrivePeriod() == tour.getTourDepartPeriod())
            {

                if (household.getDebugChoiceModels())
                {
                    tripDepartLogger
                            .info("Trip Depart Time Model Not Run Since Tour Depart and Arrive Periods are Equal; Stop Depart Period set to Tour Depart Period = "
                                    + tour.getTourDepartPeriod() + " for outbound half-tour.");
                    tripDepartLogger
                            .info(String
                                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                            household.getHhId(), person.getPersonNum(),
                                            person.getPersonType(), tour.getTourModeChoice(),
                                            tour.getTourCategory(), tour.getTourPrimaryPurpose(),
                                            tour.getTourId(), stop.getOrigPurpose(),
                                            stop.getDestPurpose(), (stop.getStopId() + 1),
                                            stops.length));
                    tripDepartLogger.info(String.format("tourDepartPeriod=%d, tourArrivePeriod=%d",
                            tour.getTourDepartPeriod(), tour.getTourArrivePeriod()));
                    tripDepartLogger.info("");
                }
                stop.setStopPeriod(tour.getTourDepartPeriod());

            } else
            {

                int tripIndex = i + 1;

                if (tripIndex == 1)
                {

                    if (household.getDebugChoiceModels())
                    {
                        tripDepartLogger
                                .info("Trip Depart Time Model Not Run Since Trip is first trip in sequence, departing from "
                                        + stop.getOrigPurpose()
                                        + "; Stop Depart Period set to Tour Depart Period = "
                                        + tour.getTourDepartPeriod() + " for outbound half-tour.");
                        tripDepartLogger
                                .info(String
                                        .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                household.getHhId(), person.getPersonNum(),
                                                person.getPersonType(), tour.getTourModeChoice(),
                                                tour.getTourCategory(),
                                                tour.getTourPrimaryPurpose(), tour.getTourId(),
                                                stop.getOrigPurpose(), stop.getDestPurpose(),
                                                (stop.getStopId() + 1), stops.length));
                        tripDepartLogger.info(String.format(
                                "tourDepartPeriod=%d, tourArrivePeriod=%d",
                                tour.getTourDepartPeriod(), tour.getTourArrivePeriod()));
                        tripDepartLogger.info("");
                    }
                    stop.setStopPeriod(tour.getTourDepartPeriod());

                } else
                {

                    int prevTripPeriod = stops[i - 1].getStopPeriod();

                    if (prevTripPeriod == tour.getTourArrivePeriod())
                    {

                        if (household.getDebugChoiceModels())
                        {
                            tripDepartLogger
                                    .info("Trip Depart Time Model Not Run Since Previous Trip Depart and Tour Arrive Periods are Equal; Stop Depart Period set to Tour Arrive Period = "
                                            + tour.getTourArrivePeriod()
                                            + " for outbound half-tour.");
                            tripDepartLogger
                                    .info(String
                                            .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                    household.getHhId(), person.getPersonNum(),
                                                    person.getPersonType(),
                                                    tour.getTourModeChoice(),
                                                    tour.getTourCategory(),
                                                    tour.getTourPrimaryPurpose(), tour.getTourId(),
                                                    stop.getOrigPurpose(), stop.getDestPurpose(),
                                                    (stop.getStopId() + 1), stops.length));
                            tripDepartLogger.info(String.format(
                                    "prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d",
                                    prevTripPeriod, tour.getTourDepartPeriod(),
                                    tour.getTourArrivePeriod()));
                            tripDepartLogger.info("");
                        }
                        stop.setStopPeriod(tour.getTourDepartPeriod());

                    } else
                    {

                        int tourPrimaryPurposeIndex = tour.getTourPrimaryPurposeIndex();

                        double[] proportions = stopTodModel.getStopTodIntervalProportions(
                                tourPrimaryPurposeIndex, halfTourDirection, prevTripPeriod,
                                tripIndex);

                        // for inbound trips, the first trip cannot arrive
                        // earlier than the last outbound trip departs
                        // if such a case is chosen, re-select.
                        int invalidCount = 0;
                        boolean validTripDepartPeriodSet = false;
                        while (validTripDepartPeriodSet == false)
                        {

                            double rn = household.getHhRandom().nextDouble();
                            int choice = getMonteCarloSelection(proportions, rn);

                            // check that this stop depart time departs at same
                            // time or later than the stop object preceding this
                            // one in the stop sequence.
                            if (choice >= prevTripPeriod && choice <= tour.getTourArrivePeriod())
                            {
                                validTripDepartPeriodSet = true;
                                if (household.getDebugChoiceModels())
                                {
                                    tripDepartLogger
                                            .info("Trip Depart Time Model for outbound half-tour.");
                                    tripDepartLogger
                                            .info(String
                                                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                            household.getHhId(),
                                                            person.getPersonNum(),
                                                            person.getPersonType(),
                                                            tour.getTourModeChoice(),
                                                            tour.getTourCategory(),
                                                            tour.getTourPrimaryPurpose(),
                                                            tour.getTourId(),
                                                            stop.getOrigPurpose(),
                                                            stop.getDestPurpose(),
                                                            (stop.getStopId() + 1), stops.length));
                                    tripDepartLogger
                                            .info(String
                                                    .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d",
                                                            prevTripPeriod,
                                                            tour.getTourDepartPeriod(),
                                                            tour.getTourArrivePeriod()));
                                    tripDepartLogger.info("tourPrimaryPurposeIndex="
                                            + tourPrimaryPurposeIndex + ", halfTourDirection="
                                            + halfTourDirection + ", tripIndex=" + tripIndex);
                                    tripDepartLogger.info("");

                                    tripDepartLogger.info(loggerSeparator);
                                    tripDepartLogger.info(String.format("%-4s %-8s  %10s  %10s",
                                            "alt", "time", "prob", "cumProb"));
                                    double cumProb = 0.0;
                                    for (int p = 1; p < proportions.length; p++)
                                    {
                                        int hr = 4 + (p / 2);
                                        int min = (p % 2) * 30;
                                        cumProb += proportions[p];
                                        String timeString = ((hr < 10) ? ("0" + hr)
                                                : ("" + hr + ":")) + ((min == 30) ? min : "00");
                                        tripDepartLogger.info(String.format(
                                                "%-4d  %-8s  %10.8f  %10.8f", p, timeString,
                                                proportions[p], cumProb));
                                    }
                                    tripDepartLogger.info(loggerSeparator);
                                    tripDepartLogger.info("rn=" + rn + ", choice=" + choice
                                            + ", try=" + invalidCount);
                                    tripDepartLogger.info("");
                                }
                                stop.setStopPeriod(choice);

                            } else
                            {
                                invalidCount++;
                            }

                            if (invalidCount > MAX_INVALID_FIRST_ARRIVAL_COUNT)
                            {
                                tripDepartLogger.error("Error in Outbound Trip Depart Time Model.");
                                tripDepartLogger
                                        .error("Outbound trip depart time less than previous trip depart time for "
                                                + invalidCount + " times.");
                                tripDepartLogger.error("Possible infinite loop?");
                                tripDepartLogger
                                        .error(String
                                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                        household.getHhId(), person.getPersonNum(),
                                                        person.getPersonType(),
                                                        tour.getTourModeChoice(),
                                                        tour.getTourCategory(),
                                                        tour.getTourPrimaryPurpose(),
                                                        tour.getTourId(), stop.getOrigPurpose(),
                                                        stop.getDestPurpose(),
                                                        (stop.getStopId() + 1), stops.length));
                                tripDepartLogger
                                        .error(String
                                                .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d, last choice=%d",
                                                        prevTripPeriod, tour.getTourDepartPeriod(),
                                                        tour.getTourArrivePeriod(), choice));
                                tripDepartLogger.error("=" + invalidCount + " times.");

                                System.out.println("Error in Outbound Trip Depart Time Model.");
                                System.out
                                        .println("Outbound trip depart time less than previous trip depart time for "
                                                + invalidCount + " times.");
                                System.out.println("Possible infinite loop?");
                                System.out
                                        .println(String
                                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                        household.getHhId(), person.getPersonNum(),
                                                        person.getPersonType(),
                                                        tour.getTourModeChoice(),
                                                        tour.getTourCategory(),
                                                        tour.getTourPrimaryPurpose(),
                                                        tour.getTourId(), stop.getOrigPurpose(),
                                                        stop.getDestPurpose(),
                                                        (stop.getStopId() + 1), stops.length));
                                System.out
                                        .println(String
                                                .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d, last choice=%d",
                                                        prevTripPeriod, tour.getTourDepartPeriod(),
                                                        tour.getTourArrivePeriod(), choice));
                                System.out.println("=" + invalidCount + " times.");
                                throw new RuntimeException();
                            }

                        }

                    }

                }

            }

        }

    }

    private void setInboundTripDepartTimes(Stop[] stops, int lastOutboundTripDeparts)
    {

        // these stops are in inbound direction
        int halfTourDirection = 1;

        for (int i = stops.length - 1; i >= 0; i--)
        {

            // if tour depart and arrive periods are the same, set same values
            // for the stops
            Stop stop = stops[i];
            Tour tour = stop.getTour();
            Person person = tour.getPersonObject();
            Household household = person.getHouseholdObject();
            if (tour.getTourArrivePeriod() == tour.getTourDepartPeriod())
            {

                if (household.getDebugChoiceModels())
                {
                    tripDepartLogger
                            .info("Trip Arrive Time Model Not Run Since Tour Depart and Arrive Periods are Equal; Stop Arrive Period set to Tour Arrive Period = "
                                    + tour.getTourDepartPeriod() + " for inbound half-tour.");
                    tripDepartLogger
                            .info(String
                                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                            household.getHhId(), person.getPersonNum(),
                                            person.getPersonType(), tour.getTourModeChoice(),
                                            tour.getTourCategory(), tour.getTourPrimaryPurpose(),
                                            tour.getTourId(), stop.getOrigPurpose(),
                                            stop.getDestPurpose(), (stop.getStopId() + 1),
                                            stops.length));
                    tripDepartLogger.info(String.format("tourDepartPeriod=%d, tourArrivePeriod=%d",
                            tour.getTourDepartPeriod(), tour.getTourArrivePeriod()));
                    tripDepartLogger.info("");
                }
                stop.setStopPeriod(tour.getTourArrivePeriod());

            } else
            {

                int tripIndex = stops.length - i;

                if (tripIndex == 1)
                {

                    if (household.getDebugChoiceModels())
                    {
                        tripDepartLogger
                                .info("Trip Arrive Time Model Not Run Since Trip is last trip in sequence, arriving at "
                                        + stop.getDestPurpose()
                                        + "; Stop Arrive Period set to Tour Arrive Period = "
                                        + tour.getTourArrivePeriod() + " for inbound half-tour.");
                        tripDepartLogger
                                .info(String
                                        .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                household.getHhId(), person.getPersonNum(),
                                                person.getPersonType(), tour.getTourModeChoice(),
                                                tour.getTourCategory(),
                                                tour.getTourPrimaryPurpose(), tour.getTourId(),
                                                stop.getOrigPurpose(), stop.getDestPurpose(),
                                                (stop.getStopId() + 1), stops.length));
                        tripDepartLogger.info(String.format(
                                "tourDepartPeriod=%d, tourArrivePeriod=%d",
                                tour.getTourDepartPeriod(), tour.getTourArrivePeriod()));
                        tripDepartLogger.info("");
                    }
                    stop.setStopPeriod(tour.getTourArrivePeriod());

                } else
                {

                    int prevTripPeriod = stops[i + 1].getStopPeriod();

                    if (prevTripPeriod == tour.getTourArrivePeriod())
                    {

                        if (household.getDebugChoiceModels())
                        {
                            tripDepartLogger
                                    .info("Trip Arrive Time Model Not Run Since Previous Trip Arrive and Tour Arrive Periods are Equal; Stop Arrive Period set to Tour Arrive Period = "
                                            + tour.getTourArrivePeriod()
                                            + " for intbound half-tour.");
                            tripDepartLogger
                                    .info(String
                                            .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                    household.getHhId(), person.getPersonNum(),
                                                    person.getPersonType(),
                                                    tour.getTourModeChoice(),
                                                    tour.getTourCategory(),
                                                    tour.getTourPrimaryPurpose(), tour.getTourId(),
                                                    stop.getOrigPurpose(), stop.getDestPurpose(),
                                                    (stop.getStopId() + 1), stops.length));
                            tripDepartLogger.info(String.format(
                                    "prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d",
                                    prevTripPeriod, tour.getTourDepartPeriod(),
                                    tour.getTourArrivePeriod()));
                            tripDepartLogger.info("");
                        }
                        stop.setStopPeriod(tour.getTourArrivePeriod());

                    } else
                    {

                        int tourPrimaryPurposeIndex = tour.getTourPrimaryPurposeIndex();

                        double[] proportions = stopTodModel.getStopTodIntervalProportions(
                                tourPrimaryPurposeIndex, halfTourDirection, prevTripPeriod,
                                tripIndex);

                        // for inbound trips, the first trip cannot arrive
                        // earlier than the last outbound trip departs
                        // if such a case is chosen, re-select.
                        int invalidCount = 0;
                        boolean validTripArrivePeriodSet = false;
                        while (validTripArrivePeriodSet == false)
                        {

                            double rn = household.getHhRandom().nextDouble();
                            int choice = getMonteCarloSelection(proportions, rn);

                            // check that this stop arrival time arrives at same
                            // time or earlier than the stop object following
                            // this one in the stop sequence.
                            // also check that this stop arrival is after the
                            // depart time for the last outbound stop.
                            if (choice <= prevTripPeriod && choice >= lastOutboundTripDeparts)
                            {
                                validTripArrivePeriodSet = true;
                                if (household.getDebugChoiceModels())
                                {
                                    tripDepartLogger
                                            .info("Trip Arrive Time Model for inbound half-tour.");
                                    tripDepartLogger
                                            .info(String
                                                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                            household.getHhId(),
                                                            person.getPersonNum(),
                                                            person.getPersonType(),
                                                            tour.getTourModeChoice(),
                                                            tour.getTourCategory(),
                                                            tour.getTourPrimaryPurpose(),
                                                            tour.getTourId(),
                                                            stop.getOrigPurpose(),
                                                            stop.getDestPurpose(),
                                                            (stop.getStopId() + 1), stops.length));
                                    tripDepartLogger.info("tourPrimaryPurposeIndex="
                                            + tourPrimaryPurposeIndex + ", halfTourDirection="
                                            + halfTourDirection + ", tripIndex=" + tripIndex
                                            + ", prevTripPeriod=" + prevTripPeriod);
                                    tripDepartLogger
                                            .info(String
                                                    .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d",
                                                            prevTripPeriod,
                                                            tour.getTourDepartPeriod(),
                                                            tour.getTourArrivePeriod()));
                                    tripDepartLogger.info(loggerSeparator);
                                    tripDepartLogger.info("");

                                    tripDepartLogger.info(String.format("%-4s %-8s  %10s  %10s",
                                            "alt", "time", "prob", "cumProb"));
                                    double cumProb = 0.0;
                                    for (int p = 1; p < proportions.length; p++)
                                    {
                                        int hr = 4 + (p / 2);
                                        int min = (p % 2) * 30;
                                        cumProb += proportions[p];
                                        String timeString = ((hr < 10) ? ("0" + hr)
                                                : ("" + hr + ":")) + ((min == 30) ? min : "00");
                                        tripDepartLogger.info(String.format(
                                                "%-4d  %-8s  %10.8f  %10.8f", p, timeString,
                                                proportions[p], cumProb));
                                    }
                                    tripDepartLogger.info(loggerSeparator);
                                    tripDepartLogger.info("rn=" + rn + ", choice=" + choice
                                            + ", try=" + invalidCount);
                                    tripDepartLogger.info("");
                                }
                                stop.setStopPeriod(choice);
                            } else
                            {
                                invalidCount++;
                            }

                            if (invalidCount > MAX_INVALID_FIRST_ARRIVAL_COUNT)
                            {
                                tripDepartLogger.error("Error in Inbound Trip Arrival Time Model.");
                                tripDepartLogger
                                        .error("Inbound trip arrive time greater than tour arrive time chosen for "
                                                + invalidCount + " times.");
                                tripDepartLogger.error("Possible infinite loop?");
                                tripDepartLogger
                                        .error(String
                                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                        household.getHhId(), person.getPersonNum(),
                                                        person.getPersonType(),
                                                        tour.getTourModeChoice(),
                                                        tour.getTourCategory(),
                                                        tour.getTourPrimaryPurpose(),
                                                        tour.getTourId(), stop.getOrigPurpose(),
                                                        stop.getDestPurpose(),
                                                        (stop.getStopId() + 1), stops.length));
                                tripDepartLogger
                                        .error(String
                                                .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d, last choice=%d",
                                                        prevTripPeriod, tour.getTourDepartPeriod(),
                                                        tour.getTourArrivePeriod(), choice));
                                tripDepartLogger.error("=" + invalidCount + " times.");

                                System.out.println("Error in Inbound Trip Arrival Time Model.");
                                System.out
                                        .println("Inbound trip arrive time greater than tour arrive time chosen for "
                                                + invalidCount + " times.");
                                System.out.println("Possible infinite loop?");
                                System.out
                                        .println(String
                                                .format("HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourCategory=%s, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, outboundStopsArray Length=%d",
                                                        household.getHhId(), person.getPersonNum(),
                                                        person.getPersonType(),
                                                        tour.getTourModeChoice(),
                                                        tour.getTourCategory(),
                                                        tour.getTourPrimaryPurpose(),
                                                        tour.getTourId(), stop.getOrigPurpose(),
                                                        stop.getDestPurpose(),
                                                        (stop.getStopId() + 1), stops.length));
                                System.out
                                        .println(String
                                                .format("prevTripPeriod=%d, tourDepartPeriod=%d, tourArrivePeriod=%d, last choice=%d",
                                                        prevTripPeriod, tour.getTourDepartPeriod(),
                                                        tour.getTourArrivePeriod(), choice));
                                System.out.println("=" + invalidCount + " times.");
                                throw new RuntimeException();
                            }

                        }

                    }

                }

            }

        }

    }

    /**
     * 
     * @param probabilities
     *            has 1s based indexing
     * @param randomNumber
     * @return
     */
    private int getMonteCarloSelection(double[] probabilities, double randomNumber)
    {

        int returnValue = 0;
        double sum = probabilities[1];
        // probabilities array passded into this method is 1s based.
        for (int i = 1; i < probabilities.length - 1; i++)
        {
            if (randomNumber <= sum)
            {
                returnValue = i;
                break;
            } else
            {
                sum += probabilities[i + 1];
                returnValue = i + 1;
            }
        }
        return returnValue;
    }

    private void zeroOutCpuTimes()
    {
        soaAutoTime = 0;
        soaOtherTime = 0;
        slsTime = 0;
        sldTime = 0;
        slcTime = 0;
        todTime = 0;
        smcTime = 0;
    }

    public long[] getStopTimes()
    {
        hhTimes[0] = soaAutoTime;
        hhTimes[1] = soaOtherTime;
        hhTimes[2] = slsTime;
        hhTimes[3] = sldTime;
        hhTimes[4] = slcTime - (soaAutoTime + soaOtherTime + slsTime);
        hhTimes[5] = slcTime;
        hhTimes[6] = todTime;
        hhTimes[7] = smcTime;
        hhTimes[8] = slcTime + sldTime + todTime + smcTime;

        return hhTimes;
    }

    // this method is called to determine the parking mgra location if the stop
    // location is in parkarea 1 and chosen mode is sov or hov.
    private int selectParkingLocation(Household household, Tour tour, Stop stop)
    {

        Logger modelLogger = parkLocLogger;

        // if the trip destination mgra is not in parking area 1, it's not
        // necessary to make a parking location choice
        if (mgraAltLocationIndex.containsKey(stop.getDest()) == false
                || mgraAltParkArea.get(stop.getDest()) != 1) return -1;

        // if person worked at home, no reason to make a parking location choice
        if (tour.getPersonObject().getFreeParkingAvailableResult() == ParkingProvisionModel.FP_MODEL_NO_REIMBURSEMENT_CHOICE)
            return -1;

        // if the person has free parking, set the parking location
        if (tour.getPersonObject().getFreeParkingAvailableResult() == 1) return stop.getDest();

        parkingChoiceDmuObj.setDmuIndexValues(household.getHhId(), stop.getOrig(), stop.getDest(),
                household.getDebugChoiceModels());

        parkingChoiceDmuObj.setPersonType(tour.getPersonObject().getPersonTypeNumber());

        Stop[] stops = null;
        if (stop.isInboundStop()) stops = tour.getInboundStops();
        else stops = tour.getOutboundStops();

        // determine activity duration in number od departure time intervals
        // if no stops on halftour, activity duration is tour duration
        int activityIntervals = 0;
        if (stops.length == 1)
        {
            activityIntervals = tour.getTourArrivePeriod() - tour.getTourDepartPeriod();
        } else
        {
            int stopId = stop.getStopId();
            if (stopId == stops.length - 1) activityIntervals = tour.getTourArrivePeriod()
                    - stop.getStopPeriod();
            else activityIntervals = stops[stopId + 1].getStopPeriod() - stop.getStopPeriod();
        }

        parkingChoiceDmuObj.setActivityIntervals(activityIntervals);

        parkingChoiceDmuObj.setDestPurpose(stop.getStopPurposeIndex());

        parkingChoiceDmuObj.setReimbPct(tour.getPersonObject().getParkingReimbursement());

        int[] sampleIndices = setupParkLocationChoiceAlternativeArrays(stop.getOrig(),
                stop.getDest());

        // if no alternatives in the sample, it's not necessary to make a
        // parking location choice
        if (sampleIndices == null) return -1;

        if (household.getDebugChoiceModels())
        {
            household.logHouseholdObject(
                    "Pre Parking Location Choice for trip: HH_" + household.getHhId() + ", Pers_"
                            + tour.getPersonObject().getPersonNum() + ", Tour Purpose_"
                            + tour.getTourPurpose() + ", Tour_" + tour.getTourId()
                            + ", Tour Purpose_" + tour.getTourPurpose() + ", Stop_"
                            + stop.getStopId(), modelLogger);
            household.logPersonObject("Pre Parking Location Choice for person "
                    + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject());
            household.logTourObject("Pre Parking Location Choice for tour " + tour.getTourId(),
                    modelLogger, tour.getPersonObject(), tour);
            household.logStopObject("Pre Parking Location Choice for stop " + stop.getStopId(),
                    modelLogger, stop, modelStructure);
        }

        Person person = tour.getPersonObject();

        String choiceModelDescription = "";
        String separator = "";
        String loggerString = "";
        String decisionMakerLabel = "";

        // log headers to traceLogger if the person making the destination
        // choice is from a household requesting trace information
        if (household.getDebugChoiceModels())
        {

            choiceModelDescription = "Parking Location Choice Model for trip";
            decisionMakerLabel = String
                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopPurpose=%s, StopId=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourPurpose(), tour.getTourId(), tour.getTourPurpose(),
                            stop.getStopId());

            modelLogger.info(" ");
            loggerString = choiceModelDescription + " for " + decisionMakerLabel + ".";
            for (int k = 0; k < loggerString.length(); k++)
                separator += "+";
            modelLogger.info(loggerString);
            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            plcModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        plcModel.computeUtilities(parkingChoiceDmuObj, parkingChoiceDmuObj.getDmuIndexValues(),
                altParkAvail, altParkSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        int chosenIndex = -1;
        int parkMgra = 0;
        if (plcModel.getAvailabilityCount() > 0)
        {
            // get the mgra number associated with the chosen alternative
            chosen = plcModel.getChoiceResult(rn);
            // sampleIndices is 1-based, but the values returned are 0-based,
            // parkMgras is 0-based
            chosenIndex = sampleIndices[chosen];
            parkMgra = parkMgras[chosenIndex];
        }

        // write choice model alternative info to log file
        if (household.getDebugChoiceModels() || chosen < 0)
        {

            double[] utilities = plcModel.getUtilities();
            double[] probabilities = plcModel.getProbabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative                                   Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------                   --------------    --------------    --------------");

            double cumProb = 0.0;

            for (int k = 1; k <= numAltsInSample; k++)
            {
                int index = sampleIndices[k];
                int altMgra = parkMgras[index];
                cumProb += probabilities[k - 1];
                String altString = String.format("k=%d, index=%d, altMgra=%d", k, index, altMgra);
                modelLogger.info(String.format("%-35s%18.6e%18.6e%18.6e", altString,
                        utilities[k - 1], probabilities[k - 1], cumProb));
            }

            modelLogger.info(" ");
            if (chosen < 0)
            {
                modelLogger.info(String.format("No Alternatives Available For Choice !!!"));
            } else
            {
                String altString = String.format("chosen=%d, chosenIndex=%d, chosenMgra=%d",
                        chosen, chosenIndex, parkMgra);
                modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));
            }

            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            plcModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            plcModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            plcModel.logUECResults(modelLogger, loggerString);

        }

        if (chosen > 0) return parkMgra;
        else
        {
            logger.error(String
                    .format("Exception caught for HHID=%d, personNum=%d, no available parking location alternatives in tourId=%d to choose from in plcModelApplication.",
                            household.getHhId(), person.getPersonNum(), tour.getTourId()));
            throw new RuntimeException();
        }

    }

    // this method is called for trips that require a park location choice --
    // trip destination in parkarea 1 and not a work trip with free onsite
    // parking
    // return false if no parking location alternatives are in walk distance of
    // trip destination; true otherwise.
    private int[] setupParkLocationChoiceAlternativeArrays(int tripOrigMgra, int tripDestMgra)
    {

        // get the array of mgras within walking distance of the trip
        // destination
        int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceTo(tripDestMgra);

        // set the distance values for the mgras walkable to the destination
        if (walkMgras != null)
        {

            // get distances, in feet, and convert to miles
            // get distances from destMgra since this is the direction of
            // distances read from the data file
            int altCount = 0;
            for (int wMgra : walkMgras)
            {
                // if wMgra is in the set of parkarea==1 MGRAs, add to list of
                // alternatives for this park location choice
                if (mgraAltLocationIndex.containsKey(wMgra))
                {

                    double curWalkDist = mgraManager.getMgraToMgraWalkDistTo(wMgra, tripDestMgra) / 5280.0;

                    if (curWalkDist > MgraDataManager.MAX_PARKING_WALK_DISTANCE) continue;

                    // the hashMap stores a 0-based index
                    int altIndex = mgraAltLocationIndex.get(wMgra);
                    int m = wMgra - 1;

                    altSdDistances[altCount + 1] = curWalkDist;
                    altMgraIndices[altCount + 1] = altIndex;

                    altParkingCostsM[altCount + 1] = lsWgtAvgCostM[m];
                    altParkingCostsD[altCount + 1] = lsWgtAvgCostD[m];
                    altParkingCostsH[altCount + 1] = lsWgtAvgCostH[m];
                    altMstallsoth[altCount + 1] = mstallsoth[m];
                    altMstallssam[altCount + 1] = mstallssam[m];
                    altMparkcost[altCount + 1] = mparkcost[m];
                    altDstallsoth[altCount + 1] = dstallsoth[m];
                    altDstallssam[altCount + 1] = dstallssam[m];
                    altDparkcost[altCount + 1] = dparkcost[m];
                    altHstallsoth[altCount + 1] = hstallsoth[m];
                    altHstallssam[altCount + 1] = hstallssam[m];
                    altHparkcost[altCount + 1] = hparkcost[m];
                    altNumfreehrs[altCount + 1] = numfreehrs[m];

                    altParkAvail[altCount + 1] = true;
                    altParkSample[altCount + 1] = 1;

                    altCount++;
                }
            }

            if (altCount > 0)
            {

                for (int i = altCount; i < MAX_PLC_SAMPLE_SIZE; i++)
                {
                    altOsDistances[i + 1] = Double.NaN;
                    altSdDistances[i + 1] = Double.NaN;
                    altMgraIndices[i + 1] = Integer.MAX_VALUE;

                    altParkingCostsM[i + 1] = Double.NaN;
                    altParkingCostsD[i + 1] = Double.NaN;
                    altParkingCostsH[i + 1] = Double.NaN;
                    altMstallsoth[i + 1] = Integer.MAX_VALUE;
                    altMstallssam[i + 1] = Integer.MAX_VALUE;
                    altMparkcost[i + 1] = Float.MAX_VALUE;
                    altDstallsoth[i + 1] = Integer.MAX_VALUE;
                    altDstallssam[i + 1] = Integer.MAX_VALUE;
                    altDparkcost[i + 1] = Float.MAX_VALUE;
                    altHstallsoth[i + 1] = Integer.MAX_VALUE;
                    altHstallssam[i + 1] = Integer.MAX_VALUE;
                    altHparkcost[i + 1] = Float.MAX_VALUE;
                    altNumfreehrs[i + 1] = Integer.MAX_VALUE;

                    altParkAvail[i + 1] = false;
                    altParkSample[i + 1] = 0;
                }
                numAltsInSample = altCount;
                if (numAltsInSample > maxAltsInSample) maxAltsInSample = numAltsInSample;
            }

            return altMgraIndices;

        }

        return null;

    }

    public int getMaxAltsInSample()
    {
        return maxAltsInSample;
    }

    public HashMap<String, Integer> getSizeSegmentNameIndexMap()
    {
        return sizeSegmentNameIndexMap;
    }

    public double[][] getSizeSegmentArray()
    {
        return slcSizeTerms;
    }

}