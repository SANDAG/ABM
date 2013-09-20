package org.sandag.abm.accessibilities;

import com.pb.common.util.Tracer;
import com.pb.common.calculator.IndexValues;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * This class builds accessibility components for all modes.
 * 
 * @author Joel Freedman
 * @version May, 2009
 */
public final class BuildAccessibilities
        implements Serializable
{

    protected transient Logger          logger                                    = Logger.getLogger(BuildAccessibilities.class);

    /*
     * OLD names private static final String[] WORK_OCCUP_SEGMENT_NAME_LIST = { "White Collar", "Services", "Health", "Retail and Food",
     * "Blue Collar", "Military" }; private static final int[] WORK_OCCUP_SEGMENT_VALUE_LIST = { 71, 72,74, 75, 76, 77 };
     */
    private static final String[]       WORK_OCCUP_SEGMENT_NAME_LIST              = {
            "Management Business Science and Arts", "Services", "Sales and Office",
            "Natural Resources Construction and Maintenance",
            "Production Transportation and Material Moving", "Military"           };
    private static final int[]          WORK_OCCUP_SEGMENT_VALUE_LIST             = {50, 51, 53,
            54, 55, 56                                                            };

    // these segment group labels and indices are used for creating all the school location choice segments
    public static final String[]        SCHOOL_DC_SIZE_SEGMENT_NAME_LIST          = {"preschool",
            "k-8", "unified k-8", "9-12", "unified 9-12", "univ typical", "univ non-typical"};
    public static final int             PRESCHOOL_SEGMENT_GROUP_INDEX             = 0;
    public static final int             GRADE_SCHOOL_SEGMENT_GROUP_INDEX          = 1;
    public static final int             UNIFIED_GRADE_SCHOOL_SEGMENT_GROUP_INDEX  = 2;
    public static final int             HIGH_SCHOOL_SEGMENT_GROUP_INDEX           = 3;
    public static final int             UNIFIED_HIGH_SCHOOL_SEGMENT_GROUP_INDEX   = 4;
    public static final int             UNIV_TYPICAL_SEGMENT_GROUP_INDEX          = 5;
    public static final int             UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX       = 6;

    private static final int            UNIFIED_DISTRICT_OFFSET                   = 1000000;

    // these indices define the alternative numbers and size term calculation indices
    public static final int             PRESCHOOL_ALT_INDEX                       = 0;
    public static final int             GRADE_SCHOOL_ALT_INDEX                    = 1;
    public static final int             HIGH_SCHOOL_ALT_INDEX                     = 2;
    public static final int             UNIV_TYPICAL_ALT_INDEX                    = 3;
    public static final int             UNIV_NONTYPICAL_ALT_INDEX                 = 4;

    // school segments: preschool, grade school, high school, university typical, university non-typical
    private static final int[]          SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX     = {3, 4, 5, 6, 6};
    private static final int[]          SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX = {5, 4, 3, 2, 2};
    private static final int[]          SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX     = {3, 3, 3, 2, 2};

    // MAX_LUZ is the largest LUZ value, including external LUZs.
    public static final int             MAX_LUZ                                   = 236;

    // using trip mode choice IVT coefficient value
    public static final double          TIME_COEFFICIENT                          = -0.032;

    public static final int[]           EXTERNAL_LUZS                             = {230, 231, 232,
            233, 234, 235, 236                                                    };

    public static final int[]           EXTERNAL_LUZ_CORDON_LUZS                  = {156, 156, 213,
            157, 122, 156, 156                                                    };

    public static final int[]           MINUTES_TO_ADD_TO_CORDON_FOR_EXTERNAL_LUZ = {105, 480,
            1800, 1740, 155, 8160, 8760                                           };

    public static final int[]           CORDON_LUZS                               = {122, 156, 157,
            213                                                                   };

    public static final int[][]         EXTERNAL_LUZS_FOR_CORDON_LUZ              = { {234},
            {230, 231, 235, 236}, {233}, {232}                                    };

    // in the LU logsums array, 1st dimension is averaging type, 2nd is pk or op, 3rd is auto sufficiency segment, 4th is orig LUZ, 5th is dest LUZ.
    public static final int             SIMPLE                                    = 0;
    public static final int             LOGIT                                     = 1;
    public static final int             PK                                        = 0;
    public static final int             OP                                        = 1;
    public static final int             LS0                                       = 0;
    public static final int             LS1                                       = 1;
    public static final int             LS2                                       = 2;

    private int[][]                     externalLuzsForCordonLuz;
    private int[]                       cordonLuzForExternalLuz;
    private int[]                       cordonLuzMinutesForExternalLuz;

    public static final int             NUM_AVG_METHODS                           = 2;
    public static final int             NUM_PERIODS                               = 2;
    public static final int             NUM_SUFFICIENCY_SEGMENTS                  = 3;

    private static BuildAccessibilities objInstance                               = null;

    private int[]                       nonUniversitySegments;
    private int[]                       universitySegments;

    private int                         univTypicalSegment;
    private int                         univNonTypicalSegment;

    private int[]                       mgraGsDistrict;
    private int[]                       mgraHsDistrict;
    private HashMap<Integer, Integer>   gsDistrictIndexMap                        = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer>   hsDistrictIndexMap                        = new HashMap<Integer, Integer>();

    private String[]                    schoolSegmentSizeNames;
    private int[]                       schoolDcSoaUecSheets;
    private int[]                       schoolDcUecSheets;
    private int[]                       schoolStfUecSheets;

    // a set of school segment indices for which shadow pricing is not done - currently includes pre-school segment only.
    private HashSet<Integer>            noShadowPriceSchoolSegmentIndices;

    private HashMap<Integer, String>    psSegmentIndexNameMap;
    private HashMap<String, Integer>    psSegmentNameIndexMap;
    private HashMap<Integer, String>    gsSegmentIndexNameMap;
    private HashMap<String, Integer>    gsSegmentNameIndexMap;
    private HashMap<Integer, String>    hsSegmentIndexNameMap;
    private HashMap<String, Integer>    hsSegmentNameIndexMap;
    private HashMap<Integer, String>    univTypSegmentIndexNameMap;
    private HashMap<String, Integer>    univTypSegmentNameIndexMap;
    private HashMap<Integer, String>    univNonTypSegmentIndexNameMap;
    private HashMap<String, Integer>    univNonTypSegmentNameIndexMap;

    private HashMap<Integer, String>    schoolSegmentIndexNameMap;
    private HashMap<String, Integer>    schoolSegmentNameIndexMap;

    private HashMap<Integer, String>    workSegmentIndexNameMap;
    private HashMap<String, Integer>    workSegmentNameIndexMap;

    public static final int             TOTAL_LOGSUM_FIELD_NUMBER                 = 13;

    private static int                  numThreads                                = 10;
    private static final int            DISTRIBUTED_PACKET_SIZE                   = 1000;

    private HashMap<Integer, Integer>   workerOccupValueSegmentIndexMap;

    private UtilityExpressionCalculator constantsUEC;
    private UtilityExpressionCalculator sizeTermUEC;
    private UtilityExpressionCalculator workerSizeTermUEC;
    private UtilityExpressionCalculator schoolSizeTermUEC;

    private AccessibilitiesDMU          aDmu;

    private IndexValues                 iv;

    private double[][][]                sovExpUtilities;
    private double[][][]                hovExpUtilities;
    private double[][][]                nMotorExpUtilities;

    private MgraDataManager             mgraManager;

    // purpose (defined in UEC)
    private double[][]                  sizeTerms;                                                                               // mgra,

    // indicates whether this mgra has a size term
    private boolean[]                   hasSizeTerm;                                                                             // mgra,

    // purpose (defined in UEC)
    private double[][]                  workerSizeTerms;                                                                         // mgra,

    // purpose (defined in UEC)
    private double[][]                  schoolSizeTerms;                                                                         // mgra,

    // Land Use size terms for school are not segmented by school district
    private double[][]                  luSchoolSizeTerms;

    // auto sufficiency (0 autos, autos<adults, autos>=adults),
    // and mode (SOV,HOV,Walk-Transit,Non-Motorized)
    private double[][]                  expConstants;

    // accessibilities by mgra, accessibility alternative
    private AccessibilitiesTable        accessibilitiesTableObject;

    // array for storing land use accessibility mode choice logsum values
    private double[][][][][]            landUseLogsums;
    private int[][]                     landUseLogsumsCount;
    private float[][]                   landUseAccessibilities;

    private boolean                     calculateLuAccessibilities;

    private static final int            MARKET_SEGMENTS                           = 3;

    public static final int             ESCORT_INDEX                              = 0;
    public static final int             SHOP_INDEX                                = 1;
    public static final int             OTH_MAINT_INDEX                           = 2;
    public static final int             EATOUT_INDEX                              = 3;
    public static final int             VISIT_INDEX                               = 4;
    public static final int             OTH_DISCR_INDEX                           = 5;

    private HashMap<String, Integer>    nonMandatorySizeSegmentNameIndexMap;

    private boolean                     trace;
    private int[]                       traceOtaz;
    private int[]                       traceDtaz;
    private Tracer                      tracer;
    private boolean                     seek;

    private int                         maxMgra;

    private boolean                     accessibilitiesBuilt                      = false;

    private BuildAccessibilities()
    {
    }

    public static synchronized BuildAccessibilities getInstance()
    {
        if (objInstance == null)
        {
            objInstance = new BuildAccessibilities();
            objInstance.accessibilitiesBuilt = false;
            return objInstance;
        } else
        {
            objInstance.accessibilitiesBuilt = true;
            return objInstance;
        }
    }

    public void setupBuildAccessibilities(HashMap<String, String> rbMap,
            boolean calculateLuAccessibilities)
    {

        this.calculateLuAccessibilities = calculateLuAccessibilities;

        Runtime runtime = Runtime.getRuntime();
        if (numThreads < 0)
        {
            int nrOfProcessors = runtime.availableProcessors();
            numThreads = nrOfProcessors;
        }

        gsDistrictIndexMap = new HashMap<Integer, Integer>();
        hsDistrictIndexMap = new HashMap<Integer, Integer>();
        workerOccupValueSegmentIndexMap = new HashMap<Integer, Integer>();

        for (int i = 0; i < WORK_OCCUP_SEGMENT_VALUE_LIST.length; ++i)
        {
            workerOccupValueSegmentIndexMap.put(WORK_OCCUP_SEGMENT_VALUE_LIST[i], i);
        }
        aDmu = new AccessibilitiesDMU();

        // Create the UECs
        String uecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.data.page");
        int constantsPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.constants.page");
        int sizeTermPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.sizeTerm.page");
        int workerSizeTermPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.workerSizeTerm.page");
        int schoolSizeTermPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.schoolSizeTerm.page");

        File uecFile = new File(uecFileName);
        constantsUEC = new UtilityExpressionCalculator(uecFile, constantsPage, dataPage, rbMap,
                aDmu);
        sizeTermUEC = new UtilityExpressionCalculator(uecFile, sizeTermPage, dataPage, rbMap, aDmu);
        workerSizeTermUEC = new UtilityExpressionCalculator(uecFile, workerSizeTermPage, dataPage,
                rbMap, aDmu);
        schoolSizeTermUEC = new UtilityExpressionCalculator(uecFile, schoolSizeTermPage, dataPage,
                rbMap, aDmu);

        mgraManager = MgraDataManager.getInstance(rbMap);
        maxMgra = mgraManager.getMaxMgra();

        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if (trace)
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");

        iv = new IndexValues();

        workSegmentIndexNameMap = new HashMap<Integer, String>();
        workSegmentNameIndexMap = new HashMap<String, Integer>();

        noShadowPriceSchoolSegmentIndices = new HashSet<Integer>();

        schoolSegmentIndexNameMap = new HashMap<Integer, String>();
        schoolSegmentNameIndexMap = new HashMap<String, Integer>();

        psSegmentIndexNameMap = new HashMap<Integer, String>();
        psSegmentNameIndexMap = new HashMap<String, Integer>();
        gsSegmentIndexNameMap = new HashMap<Integer, String>();
        gsSegmentNameIndexMap = new HashMap<String, Integer>();
        hsSegmentIndexNameMap = new HashMap<Integer, String>();
        hsSegmentNameIndexMap = new HashMap<String, Integer>();
        univTypSegmentIndexNameMap = new HashMap<Integer, String>();
        univTypSegmentNameIndexMap = new HashMap<String, Integer>();
        univNonTypSegmentIndexNameMap = new HashMap<Integer, String>();
        univNonTypSegmentNameIndexMap = new HashMap<String, Integer>();

        nonMandatorySizeSegmentNameIndexMap = new HashMap<String, Integer>();
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME,
                ESCORT_INDEX);
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME,
                SHOP_INDEX);
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME,
                OTH_MAINT_INDEX);
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME,
                EATOUT_INDEX);
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME,
                VISIT_INDEX);
        nonMandatorySizeSegmentNameIndexMap.put(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME,
                OTH_DISCR_INDEX);

    }

    /**
     * Build utility components for SOV,HOV,Walk, and Walk-Transit modes.
     * 
     * @param rb
     *            Resourcebundle with appropriate keys.
     */
    // public void buildAccessibilityComponents(HashMap<String, String> rbMap)
    // {
    //
    // boolean logObjectSize = false;
    //
    // if (ntUtilities == null)
    // {
    // ntUtilities = new NonTransitUtilities(rbMap);
    // //ntUtilities.buildUtilities();
    //
    // if (logObjectSize)
    // logger.info( "ntUtilities size:     " + ObjectUtil.sizeOf(ntUtilities) );
    //
    // }
    //
    // if (transitUtilities == null)
    // {
    // transitUtilities = new TransitUtilities(rbMap);
    // transitUtilities.calculateUtilityComponents();
    //
    // if (logObjectSize)
    // {
    // try
    // {
    // logger.info("transitUtilities size:     "
    // + ObjectUtil.checkObjectSize(transitUtilities));
    // } catch (IOException e1)
    // {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    // }
    //
    // }
    //
    // }

    // This method is only called if the main ABM command line argument for calculating land use accessibilities is true
    // otherwise, the array lanUseLogsums is null and serves as an indicator that LU accessibilities are not needed
    public void setCalculatedLandUseAccessibilities()
    {

        landUseLogsums = new double[NUM_AVG_METHODS][NUM_PERIODS][NUM_SUFFICIENCY_SEGMENTS][MAX_LUZ + 1][MAX_LUZ + 1];
        landUseLogsumsCount = new int[MAX_LUZ + 1][MAX_LUZ + 1];

        int maxMgra = mgraManager.getMaxMgra();
        landUseAccessibilities = new float[maxMgra + 1][];

        externalLuzsForCordonLuz = new int[MAX_LUZ + 1][];
        cordonLuzForExternalLuz = new int[MAX_LUZ + 1];
        cordonLuzMinutesForExternalLuz = new int[MAX_LUZ + 1];

        // associate the array of external LUZs that belong to each cordon LUZ.
        for (int i = 0; i < CORDON_LUZS.length; i++)
        {
            int cordonLuz = CORDON_LUZS[i];
            externalLuzsForCordonLuz[cordonLuz] = EXTERNAL_LUZS_FOR_CORDON_LUZ[i];
        }

        // associate the cordon LUZ that belongs to each external LUZ.
        for (int i = 0; i < EXTERNAL_LUZS.length; i++)
        {
            int externalLuz = EXTERNAL_LUZS[i];
            cordonLuzForExternalLuz[externalLuz] = EXTERNAL_LUZ_CORDON_LUZS[i];
        }

        // associate the minutes to add to cordon LUZ to represent time to each external LUZ.
        for (int i = 0; i < EXTERNAL_LUZS.length; i++)
        {
            int externalLuz = EXTERNAL_LUZS[i];
            cordonLuzMinutesForExternalLuz[externalLuz] = MINUTES_TO_ADD_TO_CORDON_FOR_EXTERNAL_LUZ[i];
        }

    }

    /**
     * Calculate size terms and store in sizeTerms array. This method initializes the sizeTerms array and loops through mgras in the mgraManager,
     * calculates the size term for all size term purposes as defined in the size term uec, and stores the results in the sizeTerms array.
     * 
     */
    public void calculateSizeTerms()
    {

        logger.info("Calculating Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();
        int alternatives = sizeTermUEC.getNumberOfAlternatives();
        sizeTerms = new double[maxMgra + 1][alternatives];
        hasSizeTerm = new boolean[maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            int taz = mgraTaz[mgra];
            iv.setZoneIndex(mgra);
            double[] utilities = sizeTermUEC.solve(iv, aDmu, null);

            // if ( mgra < 100 )
            // sizeTermUEC.logAnswersArray(logger, "NonMandatory Size Terms, MGRA = " + mgra );

            // store the size terms
            for (int purp = 0; purp < alternatives; ++purp)
            {
                sizeTerms[mgra][purp] = utilities[purp];
                if (sizeTerms[mgra][purp] > 0) hasSizeTerm[mgra] = true;
            }

            // log
            if (tracer.isTraceOn() && tracer.isTraceZone(taz))
            {

                logger.info("Size Term calculations for mgra " + mgra);
                sizeTermUEC.logResultsArray(logger, 0, mgra);

            }
        }
    }

    /**
     * Calculate size terms used for worker DC and store in workerSizeTerms array. This method initializes the workerSizeTerms array and loops through
     * mgras in the mgraManager, calculates the size term for all work size term occupation categories as defined in the worker size term uec, and
     * stores the results in the workerSizeTerms array.
     * 
     */
    public void calculateWorkerSizeTerms()
    {

        logger.info("Calculating Worker DC Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();
        int alternatives = workerSizeTermUEC.getNumberOfAlternatives();
        workerSizeTerms = new double[alternatives][maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            int taz = mgraTaz[mgra];
            iv.setZoneIndex(mgra);
            double[] utilities = workerSizeTermUEC.solve(iv, aDmu, null);

            // store the size terms
            for (int segment = 0; segment < alternatives; segment++)
            {
                workerSizeTerms[segment][mgra] = utilities[segment];
            }

            // log
            if (tracer.isTraceOn() && tracer.isTraceZone(taz))
            {
                logger.info("Worker Size Term calculations for mgra " + mgra);
                workerSizeTermUEC.logResultsArray(logger, 0, mgra);
            }
        }
    }

    /**
     * Calculate size terms used for school DC and store in schoolSizeTerms array. This method initializes the schoolSizeTerms array and loops through
     * mgras in the mgraManager, calculates the size term for all school size term categories, preschool is defined in the preschool size term uec,
     * K-8 and 9-12 use their respective enrollments as size terms, and university uses a size term uec, segmented by "typical student". Size terms
     * for preschool, k-8, 9-12, university typical amd university non-typical are stored in the studentSizeTerms array.
     * 
     */
    public void calculateSchoolSizeTerms()
    {

        logger.info("Calculating Student DC Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();

        String[] schoolSizeNames = getSchoolSegmentNameList();
        schoolSizeTerms = new double[schoolSizeNames.length][maxMgra + 1];
        luSchoolSizeTerms = new double[UNIV_NONTYPICAL_ALT_INDEX + 1][maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            // int dummy=0;
            // if ( mgra == 1801 ){
            // dummy = 1;
            // }

            int gsDistrict = getMgraGradeSchoolDistrict(mgra);
            int hsDistrict = getMgraHighSchoolDistrict(mgra);

            int taz = mgraTaz[mgra];
            iv.setZoneIndex(mgra);
            double[] utilities = schoolSizeTermUEC.solve(iv, aDmu, null);

            // store the preschool size terms
            schoolSizeTerms[PRESCHOOL_ALT_INDEX][mgra] = utilities[PRESCHOOL_ALT_INDEX];
            luSchoolSizeTerms[PRESCHOOL_ALT_INDEX][mgra] = utilities[PRESCHOOL_ALT_INDEX];

            // store the grade school size term for the district this mgra is in
            int seg = getGsDistrictIndex(gsDistrict);
            schoolSizeTerms[seg][mgra] = utilities[GRADE_SCHOOL_ALT_INDEX];
            luSchoolSizeTerms[GRADE_SCHOOL_ALT_INDEX][mgra] = utilities[GRADE_SCHOOL_ALT_INDEX];

            // store the high school size term for the district this mgra is in
            seg = getHsDistrictIndex(hsDistrict);
            schoolSizeTerms[seg][mgra] = utilities[HIGH_SCHOOL_ALT_INDEX];
            luSchoolSizeTerms[HIGH_SCHOOL_ALT_INDEX][mgra] = utilities[HIGH_SCHOOL_ALT_INDEX];

            // store the university typical size terms
            schoolSizeTerms[univTypicalSegment][mgra] = utilities[UNIV_TYPICAL_ALT_INDEX];
            luSchoolSizeTerms[UNIV_TYPICAL_ALT_INDEX][mgra] = utilities[UNIV_TYPICAL_ALT_INDEX];

            // store the university non-typical size terms
            schoolSizeTerms[univNonTypicalSegment][mgra] = utilities[UNIV_NONTYPICAL_ALT_INDEX];
            luSchoolSizeTerms[UNIV_NONTYPICAL_ALT_INDEX][mgra] = utilities[UNIV_NONTYPICAL_ALT_INDEX];

            // log
            if (tracer.isTraceOn() && tracer.isTraceZone(taz))
            {
                logger.info("School Size Term calculations for mgra " + mgra);
                schoolSizeTermUEC.logResultsArray(logger, 0, mgra);
            }
        }
    }

    public double[][] calculateSchoolSegmentFactors()
    {

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int maxMgra = mgraManager.getMaxMgra();
        String[] schoolSizeNames = getSchoolSegmentNameList();
        double[][] schoolFactors = new double[schoolSizeNames.length][maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            // store the size terms
            double univEnrollment = getTotalMgraUniversityEnrollment(mgra);

            for (int seg : nonUniversitySegments)
                schoolFactors[seg][mgra] = 1.0;

            double totalSize = 0.0;
            for (int seg : universitySegments)
                totalSize += schoolSizeTerms[seg][mgra];

            for (int seg : universitySegments)
                if (totalSize == 0) schoolFactors[seg][mgra] = 0;
                else schoolFactors[seg][mgra] = univEnrollment / totalSize;

        }

        return schoolFactors;

    }

    /**
     * Calculate constant terms, exponentiate, and store in constants array.
     */
    public void calculateConstants()
    {

        logger.info("Calculating constants");

        int modes = constantsUEC.getNumberOfAlternatives();
        expConstants = new double[MARKET_SEGMENTS + 1][modes]; // last element in
        // market segments is
        // for total

        for (int i = 0; i < MARKET_SEGMENTS + 1; ++i)
        {

            aDmu.setAutoSufficiency(i);

            double[] utilities = constantsUEC.solve(iv, aDmu, null);

            // exponentiate the constants
            for (int j = 0; j < modes; ++j)
            {
                expConstants[i][j] = Math.exp(utilities[j]);
                logger.info("Exp. Constant, market " + i + " mode " + j + " = "
                        + expConstants[i][j]);
            }
        }
    }

    public void calculateDCUtilitiesDistributed(HashMap<String, String> rbMap)
    {

        int packetSize = DISTRIBUTED_PACKET_SIZE;
        int numPackets = mgraManager.getMgras().size();

        int startIndex = 0;
        int endIndex = 0;

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        // assign start, end MGRA ranges to be used to assign to tasks
        while (endIndex < numPackets - 1)
        {
            endIndex = startIndex + packetSize - 1;

            if (endIndex + packetSize > numPackets) endIndex = numPackets - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += packetSize;
        }

        float[][] accessibilities = submitTasks(startEndIndexList, rbMap);
        accessibilitiesTableObject = new AccessibilitiesTable(accessibilities);

        // output data
        String projectDirectory = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String accFileName = projectDirectory
                + Util.getStringValueFromPropertyMap(rbMap, "acc.output.file");
        accessibilitiesTableObject.writeAccessibilityTableToFile(accFileName);
        accessibilitiesBuilt = true;

        if (landUseLogsums != null)
        {

            boolean useSimpleMethod = false;
            useSimpleMethod = Util.getBooleanValueFromPropertyMap(rbMap,
                    "lu.acc.simple.averaging.method");

            boolean useLogitMethod = false;
            useLogitMethod = Util.getBooleanValueFromPropertyMap(rbMap,
                    "lu.acc.logit.averaging.method");

            // output land use data
            String luAccFileName = projectDirectory
                    + Util.getStringValueFromPropertyMap(rbMap, "lu.acc.output.file");
            float[][][] luzAccessibilities = aggregateMgraAccessibilitiesToLuz(landUseAccessibilities);
            if (useSimpleMethod)
                accessibilitiesTableObject
                        .writeLandUseAccessibilityTableToFile(
                                luAccFileName.substring(0, luAccFileName.indexOf('.')) + "_simple"
                                        + ".csv", luzAccessibilities[SIMPLE]);
            if (useLogitMethod)
                accessibilitiesTableObject.writeLandUseAccessibilityTableToFile(
                        luAccFileName.substring(0, luAccFileName.indexOf('.')) + "_logit" + ".csv",
                        luzAccessibilities[LOGIT]);

            // output land use mode choice logsums
            String luMcLogsumsFileName = projectDirectory
                    + Util.getStringValueFromPropertyMap(rbMap, "lu.acc.mc.logsums.output.file");
            if (useSimpleMethod)
                accessibilitiesTableObject.writeLandUseLogsumTablesToFile(
                        luMcLogsumsFileName.substring(0, luMcLogsumsFileName.indexOf('.'))
                                + "_simple" + ".csv", landUseLogsums[SIMPLE]);
            if (useLogitMethod)
                accessibilitiesTableObject.writeLandUseLogsumTablesToFile(
                        luMcLogsumsFileName.substring(0, luMcLogsumsFileName.indexOf('.'))
                                + "_logit" + ".csv", landUseLogsums[LOGIT]);

        }

    }

    private float[][][] aggregateMgraAccessibilitiesToLuz(float[][] landUseAccessibilities)
    {

        // simple averaging uses accumulated logsum values, logit averaging uses accumulated utility values
        float[][][] returnValues = new float[NUM_AVG_METHODS][MAX_LUZ + 1][];
        int[] numberOfMgraValues = new int[MAX_LUZ + 1];

        for (int i = 1; i < landUseAccessibilities.length; i++)
        {

            if (landUseAccessibilities[i] == null) continue;

            // the last column of accessibilities holds the mgra value
            int numColumns = landUseAccessibilities[i].length;
            int mgra = (int) landUseAccessibilities[i][numColumns - 1];
            int luz = mgraManager.getMgraLuz(mgra);

            // allocate the columns for aggregate arrays
            for (int k = 0; k < NUM_AVG_METHODS; k++)
            {
                if (returnValues[k][luz] == null)
                {
                    returnValues[k][luz] = new float[numColumns];
                    returnValues[k][luz][numColumns - 1] = luz;
                }
            }

            numberOfMgraValues[luz]++;

            // aggregate the mgra column values in the corresponding luz row of the return array - excluding the last column
            for (int j = 0; j < numColumns - 1; j++)
            {
                returnValues[SIMPLE][luz][j] += landUseAccessibilities[i][j];
                returnValues[LOGIT][luz][j] += Math.exp(landUseAccessibilities[i][j]);
            }

            // calculate logsums from external LUZs to all destination LUZs if the origin LUZ is a cordon LUZ
            if (externalLuzsForCordonLuz[luz] != null)
            {

                for (int exLuz : externalLuzsForCordonLuz[luz])
                {

                    // allocate the columns for aggregate arrays
                    for (int k = 0; k < NUM_AVG_METHODS; k++)
                    {
                        if (returnValues[k][exLuz] == null)
                        {
                            returnValues[k][exLuz] = new float[numColumns];
                            returnValues[k][exLuz][numColumns - 1] = exLuz;
                        }
                    }

                    numberOfMgraValues[exLuz]++;

                    for (int j = 0; j < numColumns - 1; j++)
                    {
                        returnValues[SIMPLE][exLuz][j] += landUseAccessibilities[i][j];
                        returnValues[LOGIT][exLuz][j] += Math.exp(landUseAccessibilities[i][j]);
                    }

                }

            }

        }

        for (int i = 0; i < returnValues[SIMPLE].length; i++)
        {

            if (returnValues[SIMPLE][i] == null) continue;

            // the last column of accessibilities holds the mgra value
            int numColumns = returnValues[SIMPLE][i].length;
            int luz = (int) returnValues[SIMPLE][i][numColumns - 1];

            // average the the luz values of the return array - excluding the last column
            for (int j = 0; j < numColumns - 1; j++)
            {
                returnValues[SIMPLE][luz][j] /= numberOfMgraValues[luz];
                returnValues[LOGIT][luz][j] = (float) Math.log(returnValues[LOGIT][luz][j]);
            }

            // calculate logsums from external LUZs to all destination LUZs if the origin LUZ is a cordon LUZ
            if (externalLuzsForCordonLuz[luz] != null)
            {

                for (int exLuz : externalLuzsForCordonLuz[luz])
                {

                    for (int j = 0; j < numColumns - 1; j++)
                    {
                        returnValues[SIMPLE][exLuz][j] /= numberOfMgraValues[exLuz];
                        returnValues[LOGIT][exLuz][j] = (float) Math
                                .log(returnValues[LOGIT][exLuz][j]);
                    }

                }

            }

        }

        return returnValues;

    }

    public void readAccessibilityTableFromFile(String accFileName)
    {

        accessibilitiesTableObject = new AccessibilitiesTable(accFileName);
        logger.info("accessibilities table read from file.");

    }

    public boolean getAccessibilitiesAreBuilt()
    {
        return objInstance.accessibilitiesBuilt;
    }

    public AccessibilitiesTable getAccessibilitiesTableObject()
    {
        return accessibilitiesTableObject;
    }

    /**
     * @param client
     *            is a JPPFClient object which is used to establish a connection to a computing node, submit tasks, and receive results.
     */
    private float[][] submitTasks(ArrayList<int[]> startEndIndexList, HashMap<String, String> rbMap)
    {

        // Create a setup task object and submit it to the computing node.
        // This setup task creates the HouseholdChoiceModelManager and causes it to
        // create the necessary numuber
        // of HouseholdChoiceModels objects which will operate in parallel on the
        // computing node.

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        ArrayList<Future<List<Object>>> results = new ArrayList<Future<List<Object>>>();

        float[][][][][] accumulatedLandUseLogsums;
        int[][] accumulatedLandUseLogsumsCount;

        int startIndex = 0;
        int endIndex = 0;
        int taskIndex = 1;
        for (int[] startEndIndices : startEndIndexList)
        {
            startIndex = startEndIndices[0];
            endIndex = startEndIndices[1];

            logger.info(String.format("creating TASK: %d range: %d to %d.", taskIndex, startIndex,
                    endIndex));

            DcUtilitiesTaskJppf task = new DcUtilitiesTaskJppf(taskIndex, startIndex, endIndex,
                    sovExpUtilities, hovExpUtilities, nMotorExpUtilities, hasSizeTerm,
                    expConstants, sizeTerms, workerSizeTerms, luSchoolSizeTerms,
                    externalLuzsForCordonLuz, cordonLuzForExternalLuz,
                    cordonLuzMinutesForExternalLuz, rbMap, calculateLuAccessibilities);

            results.add(exec.submit(task));
            taskIndex++;
        }

        float[][] accessibilities = new float[maxMgra + 1][];

        for (Future<List<Object>> fs : results)
        {

            try
            {
                List<Object> resultBundle = fs.get();
                int task = (Integer) resultBundle.get(0);
                int start = (Integer) resultBundle.get(1);
                int end = (Integer) resultBundle.get(2);
                logger.info(String.format("returned TASK: %d, start=%d, end=%d.", task, start, end));
                float[][] taskAccessibilities = (float[][]) resultBundle.get(3);
                for (int i = 0; i < taskAccessibilities.length; i++)
                {
                    if (taskAccessibilities[i] == null) continue;
                    int numColumns = taskAccessibilities[i].length;
                    int mgra = (int) taskAccessibilities[i][numColumns - 1];
                    accessibilities[mgra] = taskAccessibilities[i];
                }

                if (landUseLogsums != null)
                {

                    // get land use accessibilities result if they were calculated
                    taskAccessibilities = (float[][]) resultBundle.get(4);
                    if (taskAccessibilities != null)
                    {
                        for (int i = 0; i < taskAccessibilities.length; i++)
                        {
                            if (taskAccessibilities[i] == null) continue;
                            int numColumns = taskAccessibilities[i].length;
                            int mgra = (int) taskAccessibilities[i][numColumns - 1];
                            landUseAccessibilities[mgra] = taskAccessibilities[i];
                        }
                    }

                    // store the land use logsums array
                    accumulatedLandUseLogsums = (float[][][][][]) resultBundle.get(5);
                    accumulatedLandUseLogsumsCount = (int[][]) resultBundle.get(6);
                    accumulateLandUseModeChoiceLogsums(accumulatedLandUseLogsums,
                            accumulatedLandUseLogsumsCount);

                }

            } catch (InterruptedException e)
            {
                e.printStackTrace();
                throw new RuntimeException();
            } catch (ExecutionException e)
            {
                logger.error("Exception returned in place of result object.", e);
                throw new RuntimeException();
            } finally
            {
                exec.shutdown();
            }

        } // future

        // calculate the averages for the land use mode choice logsums
        if (landUseLogsums != null) averageLandUseModeChoiceLogsums();

        return accessibilities;

    }

    private void averageLandUseModeChoiceLogsums()
    {

        for (int i = 0; i < NUM_PERIODS; i++)
        {
            for (int j = 0; j < NUM_SUFFICIENCY_SEGMENTS; j++)
            {
                for (int k = 1; k <= MAX_LUZ; k++)
                {
                    for (int m = 1; m <= MAX_LUZ; m++)
                    {
                        landUseLogsums[SIMPLE][i][j][k][m] = landUseLogsums[SIMPLE][i][j][k][m]
                                / landUseLogsumsCount[k][m];
                        landUseLogsums[LOGIT][i][j][k][m] = Math
                                .log(landUseLogsums[LOGIT][i][j][k][m]);
                    }
                }
            }
        }
    }

    private void accumulateLandUseModeChoiceLogsums(float[][][][][] accumulatedLandUseLogsums,
            int[][] accumulatedLandUseLogsumsCount)
    {

        for (int k = 1; k <= MAX_LUZ; k++)
        {
            for (int m = 1; m <= MAX_LUZ; m++)
            {
                landUseLogsumsCount[k][m] += accumulatedLandUseLogsumsCount[k][m];
                for (int i = 0; i < NUM_PERIODS; i++)
                {
                    for (int j = 0; j < NUM_SUFFICIENCY_SEGMENTS; j++)
                    {
                        landUseLogsums[SIMPLE][i][j][k][m] += accumulatedLandUseLogsums[SIMPLE][i][j][k][m];
                        landUseLogsums[LOGIT][i][j][k][m] += accumulatedLandUseLogsums[LOGIT][i][j][k][m];
                    }
                }
            }
        }

    }

    public double[][] getExpConstants()
    {
        return expConstants;
    }

    /**
     * @return the array of alternative labels from the UEC used to calculate work tour destination choice size terms.
     */
    public String[] getWorkSegmentNameList()
    {
        return WORK_OCCUP_SEGMENT_NAME_LIST;
    }

    /**
     * @return the table, MGRAs by occupations, of size terms calcuated for worker DC.
     */
    public double[][] getWorkerSizeTerms()
    {
        return workerSizeTerms;
    }

    /**
     * @return the array of alternative labels from the UEC used to calculate work tour destination choice size terms.
     */
    public String[] getSchoolSegmentNameList()
    {
        return schoolSegmentSizeNames;
    }

    /**
     * Specify the mapping between PECAS occupation codes, occupation segment labels, and work location choice segment indices.
     */
    public void createWorkSegmentNameIndices()
    {

        // get the list of segment names for worker tour destination choice size
        String[] occupNameList = WORK_OCCUP_SEGMENT_NAME_LIST;

        // get the list of segment values for worker tour destination choice size
        int[] occupValueList = WORK_OCCUP_SEGMENT_VALUE_LIST;

        for (int value : occupValueList)
        {
            int index = workerOccupValueSegmentIndexMap.get(value);
            String name = occupNameList[index];
            workSegmentIndexNameMap.put(index, name);
            workSegmentNameIndexMap.put(name, index);
        }

    }

    /**
     * Specify/create the mapping between school segment labels, and school location choice segment indices.
     */
    public void createSchoolSegmentNameIndices()
    {

        ArrayList<String> segmentNames = new ArrayList<String>();
        Set<Integer> gsDistrictSet = getGradeSchoolDistrictIndices();
        Set<Integer> hsDistrictSet = getHighSchoolDistrictIndices();

        String segmentName = "";

        // add preschool segment to list of segments which will not be shadow price adjusted
        int sizeSegmentIndex = 0;
        segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[PRESCHOOL_SEGMENT_GROUP_INDEX];
        noShadowPriceSchoolSegmentIndices.add(sizeSegmentIndex);

        // add preschool segment as the first segment, with index=0.
        segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[PRESCHOOL_SEGMENT_GROUP_INDEX];
        segmentNames.add(segmentName);
        schoolSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        schoolSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
        psSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        psSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);

        // increment the segmentIndex so it's new value is the first grade school index
        // add grade school segments to list
        sizeSegmentIndex++;
        gsDistrictIndexMap = new HashMap<Integer, Integer>();
        for (int gsDist : gsDistrictSet)
        {
            if (gsDist > UNIFIED_DISTRICT_OFFSET) segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[UNIFIED_GRADE_SCHOOL_SEGMENT_GROUP_INDEX]
                    + "_" + (gsDist - UNIFIED_DISTRICT_OFFSET);
            else segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[GRADE_SCHOOL_SEGMENT_GROUP_INDEX]
                    + "_" + gsDist;
            segmentNames.add(segmentName);
            schoolSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
            schoolSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
            gsSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
            gsSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
            gsDistrictIndexMap.put(gsDist, sizeSegmentIndex);
            sizeSegmentIndex++;
        }

        // add high school segments to list
        hsDistrictIndexMap = new HashMap<Integer, Integer>();
        for (int hsDist : hsDistrictSet)
        {
            if (hsDist > UNIFIED_DISTRICT_OFFSET) segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[UNIFIED_HIGH_SCHOOL_SEGMENT_GROUP_INDEX]
                    + "_" + (hsDist - UNIFIED_DISTRICT_OFFSET);
            else segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[HIGH_SCHOOL_SEGMENT_GROUP_INDEX]
                    + "_" + hsDist;
            segmentNames.add(segmentName);
            schoolSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
            schoolSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
            hsSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
            hsSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
            hsDistrictIndexMap.put(hsDist, sizeSegmentIndex);
            sizeSegmentIndex++;
        }

        // add typical university/colleger segments to list
        segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[UNIV_TYPICAL_SEGMENT_GROUP_INDEX];
        segmentNames.add(segmentName);
        schoolSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        schoolSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
        univTypSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        univTypSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
        univTypicalSegment = sizeSegmentIndex;
        sizeSegmentIndex++;

        // add non-typical university/colleger segments to list
        segmentName = SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX];
        segmentNames.add(segmentName);
        schoolSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        schoolSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
        univNonTypSegmentIndexNameMap.put(sizeSegmentIndex, segmentName);
        univNonTypSegmentNameIndexMap.put(segmentName, sizeSegmentIndex);
        univNonTypicalSegment = sizeSegmentIndex;

        // create arrays dimensioned as the number of school segments created and assign their values

        // 2 university segments
        universitySegments = new int[2];

        // num GS Dists + num HS dists + preschool non-university segments
        nonUniversitySegments = new int[segmentNames.size() - universitySegments.length];

        int u = 0;
        int nu = 0;

        schoolSegmentSizeNames = new String[segmentNames.size()];
        for (int i = 0; i < schoolSegmentSizeNames.length; i++)
            schoolSegmentSizeNames[i] = segmentNames.get(i);

        schoolDcSoaUecSheets = new int[schoolSegmentSizeNames.length];
        schoolDcUecSheets = new int[schoolSegmentSizeNames.length];
        schoolStfUecSheets = new int[schoolSegmentSizeNames.length];

        sizeSegmentIndex = 0;
        schoolDcSoaUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX[PRESCHOOL_ALT_INDEX];
        schoolDcUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX[PRESCHOOL_ALT_INDEX];
        schoolStfUecSheets[sizeSegmentIndex] = SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX[PRESCHOOL_ALT_INDEX];
        nonUniversitySegments[nu++] = sizeSegmentIndex;
        sizeSegmentIndex++;

        for (int segmentIndex : gsDistrictIndexMap.values())
        {
            schoolDcSoaUecSheets[segmentIndex] = SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX[GRADE_SCHOOL_ALT_INDEX];
            schoolDcUecSheets[segmentIndex] = SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX[GRADE_SCHOOL_ALT_INDEX];
            schoolStfUecSheets[segmentIndex] = SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX[GRADE_SCHOOL_ALT_INDEX];
            nonUniversitySegments[nu++] = segmentIndex;
            sizeSegmentIndex++;
        }

        for (int segmentIndex : hsDistrictIndexMap.values())
        {
            schoolDcSoaUecSheets[segmentIndex] = SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX[HIGH_SCHOOL_ALT_INDEX];
            schoolDcUecSheets[segmentIndex] = SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX[HIGH_SCHOOL_ALT_INDEX];
            schoolStfUecSheets[segmentIndex] = SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX[HIGH_SCHOOL_ALT_INDEX];
            nonUniversitySegments[nu++] = segmentIndex;
            sizeSegmentIndex++;
        }

        schoolDcSoaUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX[UNIV_TYPICAL_ALT_INDEX];
        schoolDcUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX[UNIV_TYPICAL_ALT_INDEX];
        schoolStfUecSheets[sizeSegmentIndex] = SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX[UNIV_TYPICAL_ALT_INDEX];
        universitySegments[u++] = sizeSegmentIndex;
        sizeSegmentIndex++;

        schoolDcSoaUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX[UNIV_NONTYPICAL_ALT_INDEX];
        schoolDcUecSheets[sizeSegmentIndex] = SCHOOL_LOC_SEGMENT_TO_UEC_SHEET_INDEX[UNIV_NONTYPICAL_ALT_INDEX];
        schoolStfUecSheets[sizeSegmentIndex] = SCHOOL_SEGMENT_TO_STF_UEC_SHEET_INDEX[UNIV_NONTYPICAL_ALT_INDEX];
        universitySegments[u++] = sizeSegmentIndex;

    }

    public HashSet<Integer> getNoShadowPriceSchoolSegmentIndexSet()
    {
        return noShadowPriceSchoolSegmentIndices;
    }

    public HashMap<Integer, String> getSchoolSegmentIndexNameMap()
    {
        return schoolSegmentIndexNameMap;
    }

    public HashMap<String, Integer> getSchoolSegmentNameIndexMap()
    {
        return schoolSegmentNameIndexMap;
    }

    /**
     * @return pre-school segment index to segment name hashmap
     */
    public HashMap<Integer, String> getPsSegmentIndexNameMap()
    {
        return psSegmentIndexNameMap;
    }

    /**
     * @return pre-school segment name to segment index hashmap
     */
    public HashMap<String, Integer> getPsSegmentNameIndexMap()
    {
        return psSegmentNameIndexMap;
    }

    /**
     * @return grade school segment index to segment name hashmap
     */
    public HashMap<Integer, String> getGsSegmentIndexNameMap()
    {
        return gsSegmentIndexNameMap;
    }

    /**
     * @return grade school segment name to segment index hashmap
     */
    public HashMap<String, Integer> getGsSegmentNameIndexMap()
    {
        return gsSegmentNameIndexMap;
    }

    /**
     * @return high school segment index to segment name hashmap
     */
    public HashMap<Integer, String> getHsSegmentIndexNameMap()
    {
        return hsSegmentIndexNameMap;
    }

    /**
     * @return high school segment name to segment index hashmap
     */
    public HashMap<String, Integer> getHsSegmentNameIndexMap()
    {
        return hsSegmentNameIndexMap;
    }

    /**
     * @return university typical segment index to segment name hashmap
     */
    public HashMap<Integer, String> getUnivTypicalSegmentIndexNameMap()
    {
        return univTypSegmentIndexNameMap;
    }

    /**
     * @return university typical segment name to segment index hashmap
     */
    public HashMap<String, Integer> getUnivTypicalSegmentNameIndexMap()
    {
        return univTypSegmentNameIndexMap;
    }

    /**
     * @return university non typical segment index to segment name hashmap
     */
    public HashMap<Integer, String> getUnivNonTypicalSegmentIndexNameMap()
    {
        return univNonTypSegmentIndexNameMap;
    }

    /**
     * @return university non typical segment name to segment index hashmap
     */
    public HashMap<String, Integer> getUnivNonTypicalSegmentNameIndexMap()
    {
        return univNonTypSegmentNameIndexMap;
    }

    public HashMap<Integer, String> getWorkSegmentIndexNameMap()
    {
        return workSegmentIndexNameMap;
    }

    public HashMap<String, Integer> getWorkSegmentNameIndexMap()
    {
        return workSegmentNameIndexMap;
    }

    public HashMap<Integer, Integer> getWorkOccupValueIndexMap()
    {
        return workerOccupValueSegmentIndexMap;
    }

    public int getGsDistrictIndex(int district)
    {
        return gsDistrictIndexMap.get(district);
    }

    public int getHsDistrictIndex(int district)
    {
        return hsDistrictIndexMap.get(district);
    }

    public int[] getSchoolDcSoaUecSheets()
    {
        return schoolDcSoaUecSheets;
    }

    public int[] getSchoolDcUecSheets()
    {
        return schoolDcUecSheets;
    }

    /**
     * @return the array of stop frequency uec model sheet indices, indexed by school segment
     */
    public int[] getSchoolStfUecSheets()
    {
        return schoolStfUecSheets;
    }

    /**
     * @return the table, MGRAs by school types, of size terms calcuated for school DC.
     */
    public double[][] getSchoolSizeTerms()
    {
        return schoolSizeTerms;
    }

    /**
     * @return the table, MGRAs by non-mandatory types, of size terms calcuated.
     */
    public double[][] getSizeTerms()
    {
        return sizeTerms;
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return population for the specified mgra.
     */
    public double getMgraPopulation(int mgra)
    {
        return mgraManager.getMgraPopulation(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return households for the specified mgra.
     */
    public double getMgraHouseholds(int mgra)
    {
        return mgraManager.getMgraHouseholds(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return grade school enrollment for the specified mgra.
     */
    public double getMgraGradeSchoolEnrollment(int mgra)
    {
        return mgraManager.getMgraGradeSchoolEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return high school enrollment for the specified mgra.
     */
    public double getMgraHighSchoolEnrollment(int mgra)
    {
        return mgraManager.getMgraHighSchoolEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return university enrollment for the specified mgra.
     */
    public double getTotalMgraUniversityEnrollment(int mgra)
    {
        return mgraManager.getMgraUniversityEnrollment(mgra)
                + mgraManager.getMgraOtherCollegeEnrollment(mgra)
                + mgraManager.getMgraAdultSchoolEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return university enrollment for the specified mgra.
     */
    public double getMgraUniversityEnrollment(int mgra)
    {
        return mgraManager.getMgraUniversityEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return other college enrollment for the specified mgra.
     */
    public double getMgraOtherCollegeEnrollment(int mgra)
    {
        return mgraManager.getMgraOtherCollegeEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return adult school enrollment for the specified mgra.
     */
    public double getMgraAdultSchoolEnrollment(int mgra)
    {
        return mgraManager.getMgraAdultSchoolEnrollment(mgra);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return grade school district for the specified mgra.
     */
    public int getMgraGradeSchoolDistrict(int mgra)
    {
        int gsDist = mgraManager.getMgraGradeSchoolDistrict(mgra);
        int hsDist = mgraManager.getMgraHighSchoolDistrict(mgra);
        if (gsDist == 0) gsDist = UNIFIED_DISTRICT_OFFSET + hsDist;
        return gsDist;
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return high school district for the specified mgra.
     */
    public int getMgraHighSchoolDistrict(int mgra)
    {
        int gsDist = mgraManager.getMgraGradeSchoolDistrict(mgra);
        int hsDist = mgraManager.getMgraHighSchoolDistrict(mgra);
        if (gsDist == 0) hsDist = UNIFIED_DISTRICT_OFFSET + hsDist;
        return hsDist;
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return school location choice model segment for the specified mgra.
     */
    public int getMgraGradeSchoolSegmentIndex(int mgra)
    {
        int gsDist = mgraManager.getMgraGradeSchoolDistrict(mgra);
        int hsDist = mgraManager.getMgraHighSchoolDistrict(mgra);
        if (gsDist == 0) gsDist = UNIFIED_DISTRICT_OFFSET + hsDist;

        return gsDistrictIndexMap.get(gsDist);
    }

    /**
     * @param mgra
     *            for which table data is desired
     * @return school location choice model segment for the specified mgra.
     */
    public int getMgraHighSchoolSegmentIndex(int mgra)
    {
        int gsDist = mgraManager.getMgraGradeSchoolDistrict(mgra);
        int hsDist = mgraManager.getMgraHighSchoolDistrict(mgra);
        if (gsDist == 0) hsDist = UNIFIED_DISTRICT_OFFSET + hsDist;

        return hsDistrictIndexMap.get(hsDist);
    }

    /**
     * @return set of unique grade school district indices
     */
    private TreeSet<Integer> getGradeSchoolDistrictIndices()
    {
        int maxMgra = mgraManager.getMaxMgra();
        mgraGsDistrict = new int[maxMgra + 1];
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (int r = 1; r <= maxMgra; r++)
        {
            int gsDist = mgraManager.getMgraGradeSchoolDistrict(r);
            int hsDist = mgraManager.getMgraHighSchoolDistrict(r);
            if (gsDist == 0) gsDist = UNIFIED_DISTRICT_OFFSET + hsDist;
            set.add(gsDist);
            mgraGsDistrict[r] = gsDist;
        }
        return set;
    }

    /**
     * @return set of unique high school district indices
     */
    private TreeSet<Integer> getHighSchoolDistrictIndices()
    {
        int maxMgra = mgraManager.getMaxMgra();
        mgraHsDistrict = new int[maxMgra + 1];
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (int r = 1; r <= maxMgra; r++)
        {
            int gsDist = mgraManager.getMgraGradeSchoolDistrict(r);
            int hsDist = mgraManager.getMgraHighSchoolDistrict(r);
            if (gsDist == 0) hsDist = UNIFIED_DISTRICT_OFFSET + hsDist;
            set.add(hsDist);
            mgraHsDistrict[r] = hsDist;
        }
        return set;
    }

    public int[] getMgraGsDistrict()
    {
        return mgraGsDistrict;
    }

    public int[] getMgraHsDistrict()
    {
        return mgraHsDistrict;
    }

    public HashMap<Integer, Integer> getGsDistrictIndexMap()
    {
        return gsDistrictIndexMap;
    }

    public HashMap<Integer, Integer> getHsDistrictIndexMap()
    {
        return hsDistrictIndexMap;
    }

    public HashMap<String, Integer> getNonMandatoryPurposeNameIndexMap()
    {
        return nonMandatorySizeSegmentNameIndexMap;
    }

    public int getEscortSizeArraySegmentIndex()
    {
        return nonMandatorySizeSegmentNameIndexMap.get(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME);
    }

}
