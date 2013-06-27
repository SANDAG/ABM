package org.sandag.abm.ctramp;

import java.util.HashMap;
import java.io.Serializable;

/**
 * Holds the tour purpose list as well as the market segments for each tour.
 * 
 * @author D. Ory
 * 
 */
public abstract class ModelStructure
        implements Serializable
{

    public static final String[]                        DC_SIZE_AREA_TYPE_BASED_SEGMENTS  = {"CBD",
            "URBAN", "SUBURBAN", "RURAL"                                                  };

    public static final String                          MANDATORY_CATEGORY                = "MANDATORY";
    public static final String                          JOINT_NON_MANDATORY_CATEGORY      = "JOINT_NON_MANDATORY";
    public static final String                          INDIVIDUAL_NON_MANDATORY_CATEGORY = "INDIVIDUAL_NON_MANDATORY";
    public static final String                          AT_WORK_CATEGORY                  = "AT_WORK";

    public static final String                          MANDATORY_PATTERN                 = "M";
    public static final String                          NONMANDATORY_PATTERN              = "N";
    public static final String                          HOME_PATTERN                      = "H";

    public static final int                             FIRST_DEPART_HOUR                 = 4;
    public static final int                             LAST_DEPART_HOUR                  = 24;
    public static final int                             FIRST_TOD_INTERVAL_HOUR           = 430;
    public static final int                             LAST_TOD_INTERVAL_HOUR            = 2400;
    protected String[]                                  TOD_INTERVAL_LABELS;
    

    public static final int      EA_SKIM_PERIOD_INDEX   = 0;
    public static final int      AM_SKIM_PERIOD_INDEX   = 1;
    public static final int      MD_SKIM_PERIOD_INDEX   = 2;
    public static final int      PM_SKIM_PERIOD_INDEX   = 3;
    public static final int      EV_SKIM_PERIOD_INDEX   = 4;
    public static final int[]    SKIM_PERIOD_INDICES    = {
                                        EA_SKIM_PERIOD_INDEX,
                                        AM_SKIM_PERIOD_INDEX,
                                        MD_SKIM_PERIOD_INDEX,
                                        PM_SKIM_PERIOD_INDEX,
                                        EV_SKIM_PERIOD_INDEX
                                        };
    
    public static final String[] SKIM_PERIOD_STRINGS    = {
                                        "EA",
                                        "AM",
                                        "MD",
                                        "PM",
                                        "EV"
                                        };

    // define indices associated with valid skim period combinations
    public static final int      EA_EA                                                              = 0;
    public static final int      EA_AM                                                              = 1;
    public static final int      EA_MD                                                              = 2;
    public static final int      EA_PM                                                              = 3;
    public static final int      EA_EV                                                              = 4;
    // AM cannot be before EA
    public static final int      AM_EA                                                              = -1;
    public static final int      AM_AM                                                              = 5;
    public static final int      AM_MD                                                              = 6;
    public static final int      AM_PM                                                              = 7;
    public static final int      AM_EV                                                              = 8;
    // MD cannot be before EA or AM
    public static final int      MD_EA                                                              = -1;
    public static final int      MD_AM                                                              = -1;
    public static final int      MD_MD                                                              = 9;
    public static final int      MD_PM                                                              = 10;
    public static final int      MD_EV                                                              = 11;
    // PM cannot be before EA, AM or PM
    public static final int      PM_EA                                                              = -1;
    public static final int      PM_AM                                                              = -1;
    public static final int      PM_MD                                                              = -1;
    public static final int      PM_PM                                                              = 12;
    public static final int      PM_EV                                                              = 13;
    // EV cannot be before EA, AM, MD or PM
    public static final int      EV_EA                                                              = -1;
    public static final int      EV_AM                                                              = -1;
    public static final int      EV_MD                                                              = -1;
    public static final int      EV_PM                                                              = -1;
    public static final int      EV_EV                                                              = 14;

    // define an array that contains the set of the valid skim period combination indices
    public static final int[]    SKIM_PERIOD_COMBINATION_INDICES  = {
                                        EA_EA,
                                        EA_AM,
                                        EA_MD,
                                        EA_PM,
                                        EA_EV,
                                        AM_AM,
                                        AM_MD,
                                        AM_PM,
                                        AM_EV,
                                        MD_MD,
                                        MD_PM,
                                        MD_EV,
                                        PM_PM,
                                        PM_EV,
                                        EV_EV };

    // define a 2-D array for the set of skim period combinations associatedf with each skim period index value 
    public static final int[][]  SKIM_PERIOD_COMBINATIONS = {
                                        { EA_EA, EA_AM, EA_MD, EA_PM, EA_EV },
                                        { AM_EA, AM_AM, AM_MD, AM_PM, AM_EV },
                                        { MD_EA, MD_AM, MD_MD, MD_PM, MD_EV },
                                        { PM_EA, PM_AM, PM_MD, PM_PM, PM_EV },
                                        { EV_EA, EV_AM, EV_MD, EV_PM, EV_EV }
                                        };
    
    // define model period labels associated with each model period index
    public static final String[] MODEL_PERIOD_LABELS = { "EA", "AM", "MD", "PM", "EV" };

    
    // the upper TOD interval index for each model period (EA:1-3, AM:6-9, MD:10-22, PM:23-29, EV:30-40) 
    public static final int UPPER_EA = 3;
    public static final int UPPER_AM = 9;
    public static final int UPPER_MD = 22;
    public static final int UPPER_PM = 29;

    
    private HashMap<Integer,Integer>                    indexTimePeriodMap;
    private HashMap<Integer,Integer>                    timePeriodIndexMap;
    
    public static final int                             WORKS_AT_HOME_ALTERNATUVE_INDEX  = 2;
    public static final int                             WORKS_AT_HOME_LOCATION_INDICATOR = 99999;
    public static final int                             NOT_ENROLLED_SEGMENT_INDEX       = 88888;

    
    private HashMap<String, Integer>                    primaryTourPurposeNameIndexMap    = new HashMap<String, Integer>();
    private HashMap<Integer, String>                    indexPrimaryTourPurposeNameMap    = new HashMap<Integer, String>();

    public static final String                          WORK_PRIMARY_PURPOSE_NAME         = "Work";
    public static final String                          UNIVERSITY_PRIMARY_PURPOSE_NAME   = "University";
    public static final String                          SCHOOL_PRIMARY_PURPOSE_NAME       = "School";
    public static final String                          ESCORT_PRIMARY_PURPOSE_NAME       = "Escort";
    public static final String                          SHOPPING_PRIMARY_PURPOSE_NAME     = "Shop";
    public static final String                          OTH_MAINT_PRIMARY_PURPOSE_NAME    = "Maintenance";
    public static final String                          EAT_OUT_PRIMARY_PURPOSE_NAME      = "Eating Out";
    public static final String                          VISITING_PRIMARY_PURPOSE_NAME     = "Visiting";
    public static final String                          OTH_DISCR_PRIMARY_PURPOSE_NAME    = "Discretionary";
    public static final String                          WORK_BASED_PRIMARY_PURPOSE_NAME   = "Work-Based";

    public static final int                             WORK_PRIMARY_PURPOSE_INDEX        = 1;
    public static final int                             UNIVERSITY_PRIMARY_PURPOSE_INDEX  = 2;
    public static final int                             SCHOOL_PRIMARY_PURPOSE_INDEX      = 3;
    public static final int                             ESCORT_PRIMARY_PURPOSE_INDEX      = 4;
    public static final int                             SHOPPING_PRIMARY_PURPOSE_INDEX    = 5;
    public static final int                             OTH_MAINT_PRIMARY_PURPOSE_INDEX   = 6;
    public static final int                             EAT_OUT_PRIMARY_PURPOSE_INDEX     = 7;
    public static final int                             VISITING_PRIMARY_PURPOSE_INDEX    = 8;
    public static final int                             OTH_DISCR_PRIMARY_PURPOSE_INDEX   = 9;
    public static final int                             WORK_BASED_PRIMARY_PURPOSE_INDEX  = 10;
    public static final int                             NUM_PRIMARY_PURPOSES              = 10;

    public String                                       WORK_PURPOSE_NAME;
    public String                                       UNIVERSITY_PURPOSE_NAME;
    public String                                       SCHOOL_PURPOSE_NAME;
    public String                                       ESCORT_PURPOSE_NAME;
    public String                                       SHOPPING_PURPOSE_NAME;
    public String                                       EAT_OUT_PURPOSE_NAME;
    public String                                       OTH_MAINT_PURPOSE_NAME;
    public String                                       SOCIAL_PURPOSE_NAME;
    public String                                       OTH_DISCR_PURPOSE_NAME;
    public String                                       AT_WORK_PURPOSE_NAME;
    public String                                       AT_WORK_EAT_PURPOSE_NAME;
    public String                                       AT_WORK_BUSINESS_PURPOSE_NAME;
    public String                                       AT_WORK_MAINT_PURPOSE_NAME;

    public int                                          AT_WORK_PURPOSE_INDEX_EAT;
    public int                                          AT_WORK_PURPOSE_INDEX_BUSINESS;
    public int                                          AT_WORK_PURPOSE_INDEX_MAINT;

    public String[]                                     ESCORT_SEGMENT_NAMES;
    public String[]                                     AT_WORK_SEGMENT_NAMES;

    protected HashMap<String, Integer>                  workSegmentNameIndexMap;
    protected HashMap<String, Integer>                  schoolSegmentNameIndexMap;
    protected HashMap<Integer, String>                  workSegmentIndexNameMap;
    protected HashMap<Integer, String>                  schoolSegmentIndexNameMap;

    // TODO: Determine which of the following can be eliminated
    protected HashMap<String, Integer>                  dcSoaUecIndexMap;
    protected HashMap<String, Integer>                  dcUecIndexMap;
    protected HashMap<String, Integer>                  tourModeChoiceUecIndexMap;

    protected HashMap<String, String>                   dcSizeDcModelPurposeMap;
    protected HashMap<String, String>                   dcModelDcSizePurposeMap;

    protected HashMap<String, Integer>                  dcModelPurposeIndexMap;                                            // segments
    // for
    // which
    // dc
    // soa alternative models
    // are applied
    protected HashMap<Integer, String>                  dcModelIndexPurposeMap;                                            // segments
    // for
    // which
    // dc
    // soa alternative models
    // are applied

    protected HashMap<String, Integer>                  dcSizeSegmentIndexMap;                                             // segments
    // for
    // which
    // separate dc size
    // coefficients are
    // specified
    protected HashMap<Integer, String>                  dcSizeIndexSegmentMap;
    protected HashMap<String, Integer>                  dcSizeArrayPurposeIndexMap;                                        // segments
    // for
    // which
    // dc
    // size terms are stored
    protected HashMap<Integer, String>                  dcSizeArrayIndexPurposeMap;
    protected HashMap<String, HashMap<String, Integer>> dcSizePurposeSegmentMap;

    private String                                      dcSizeCoeffPurposeFieldName       = "purpose";
    private String                                      dcSizeCoeffSegmentFieldName       = "segment";

    // TODO meld with what jim is doing on this front
    protected String[]                                  mandatoryDcModelPurposeNames;
    protected String[]                                  jointDcModelPurposeNames;
    protected String[]                                  nonMandatoryDcModelPurposeNames;
    protected String[]                                  atWorkDcModelPurposeNames;

    protected String                                    workPurposeName;
    protected String                                    universityPurposeName;
    protected String                                    schoolPurposeName;

    protected String[]                                  workPurposeSegmentNames;
    protected String[]                                  universityPurposeSegmentNames;
    protected String[]                                  schoolPurposeSegmentNames;

    protected HashMap<String, Integer>                  stopFreqUecIndexMap;
    protected HashMap<String, Integer>                  stopLocUecIndexMap;
    protected HashMap<String, Integer>                  tripModeChoiceUecIndexMap;

    protected String[]                                  jtfAltLabels;
    protected String[]                                  awfAltLabels;


    
    /**
     * Assume name of the columns in the destination size coefficients file that
     * contain the purpose strings is "purpose" and the column that contains the
     * segment strings is "segment"
     */
    public ModelStructure()
    {

        workSegmentNameIndexMap = new HashMap<String, Integer>();
        schoolSegmentNameIndexMap = new HashMap<String, Integer>();
        workSegmentIndexNameMap = new HashMap<Integer, String>();
        schoolSegmentIndexNameMap = new HashMap<Integer, String>();

        dcModelPurposeIndexMap = new HashMap<String, Integer>();
        dcModelIndexPurposeMap = new HashMap<Integer, String>();
        dcSoaUecIndexMap = new HashMap<String, Integer>();
        dcUecIndexMap = new HashMap<String, Integer>();
        tourModeChoiceUecIndexMap = new HashMap<String, Integer>();
        stopFreqUecIndexMap = new HashMap<String, Integer>();
        stopLocUecIndexMap = new HashMap<String, Integer>();
        tripModeChoiceUecIndexMap = new HashMap<String, Integer>();

        // create a mapping between primary purpose names and purpose indices
        createPrimaryPurposeMappings();

        createIndexTimePeriodMap();
        
    }

    abstract public HashMap<String, Integer> getWorkSegmentNameIndexMap();

    abstract public HashMap<String, Integer> getSchoolSegmentNameIndexMap();

    abstract public HashMap<Integer, String> getWorkSegmentIndexNameMap();

    abstract public HashMap<Integer, String> getSchoolSegmentIndexNameMap();

    // a derived class must implement these methods to retrieve purpose names for
    // various personTypes making mandatory tours.
    abstract public String getWorkPurpose(int incomeCategory);

    abstract public String getWorkPurpose(boolean isPtWorker, int incomeCategory);

    abstract public String getUniversityPurpose();

    abstract public String getSchoolPurpose(int age);

    abstract public boolean getTourModeIsSov(int tourMode);

    abstract public boolean getTourModeIsSovOrHov(int tourMode);

    abstract public boolean getTourModeIsS2(int tourMode);

    abstract public boolean getTourModeIsS3(int tourMode);

    abstract public boolean getTourModeIsHov(int tourMode);

    abstract public boolean getTourModeIsNonMotorized(int tourMode);

    abstract public boolean getTourModeIsBike(int tourMode);

    abstract public boolean getTourModeIsWalk(int tourMode);

    abstract public boolean getTourModeIsWalkLocal(int tourMode);

    abstract public boolean getTourModeIsWalkPremium(int tourMode);

    abstract public boolean getTourModeIsTransit(int tourMode);

    abstract public boolean getTourModeIsWalkTransit(int tourMode);

    abstract public boolean getTourModeIsDriveTransit(int tourMode);

    abstract public boolean getTourModeIsPnr(int tourMode);

    abstract public boolean getTourModeIsKnr(int tourMode);

    abstract public boolean getTourModeIsSchoolBus(int tourMode);

    abstract public boolean getTripModeIsSovOrHov(int tripMode);

    abstract public boolean getTripModeIsWalkTransit(int tripMode);
    
    abstract public boolean getTripModeIsPnrTransit(int tripMode);
    
    abstract public boolean getTripModeIsKnrTransit(int tripMode);
    
    abstract public int getRideModeIndexForTripMode( int tripMode );
    
    abstract public double[][] getCdap6PlusProps();

    abstract public int getDefaultAmPeriod();

    abstract public int getDefaultPmPeriod();

    abstract public int getDefaultMdPeriod();

    abstract public int getMaxTourModeIndex();

    abstract public String getModelPeriodLabel(int period);

    abstract public int[] getSkimPeriodCombinationIndices();

    abstract public int getSkimPeriodCombinationIndex(int startPeriod, int endPeriod);

    abstract public String getSkimMatrixPeriodString(int period);

    abstract public HashMap<String, HashMap<String, Integer>> getDcSizePurposeSegmentMap();
    
    abstract public String[] getJtfAltLabels();
       
    abstract public void setJtfAltLabels( String[] labels );    
    
    

    private void createPrimaryPurposeMappings()
    {

        primaryTourPurposeNameIndexMap.put(WORK_PRIMARY_PURPOSE_NAME, WORK_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(WORK_PRIMARY_PURPOSE_INDEX, WORK_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(UNIVERSITY_PRIMARY_PURPOSE_NAME,
                UNIVERSITY_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(UNIVERSITY_PRIMARY_PURPOSE_INDEX,
                UNIVERSITY_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(SCHOOL_PRIMARY_PURPOSE_NAME,
                SCHOOL_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(SCHOOL_PRIMARY_PURPOSE_INDEX,
                SCHOOL_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(ESCORT_PRIMARY_PURPOSE_NAME,
                ESCORT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(ESCORT_PRIMARY_PURPOSE_INDEX,
                ESCORT_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(SHOPPING_PRIMARY_PURPOSE_NAME,
                SHOPPING_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(SHOPPING_PRIMARY_PURPOSE_INDEX,
                SHOPPING_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(OTH_MAINT_PRIMARY_PURPOSE_NAME,
                OTH_MAINT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(OTH_MAINT_PRIMARY_PURPOSE_INDEX,
                OTH_MAINT_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(EAT_OUT_PRIMARY_PURPOSE_NAME,
                EAT_OUT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(EAT_OUT_PRIMARY_PURPOSE_INDEX,
                EAT_OUT_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(VISITING_PRIMARY_PURPOSE_NAME,
                VISITING_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(VISITING_PRIMARY_PURPOSE_INDEX,
                VISITING_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(OTH_DISCR_PRIMARY_PURPOSE_NAME,
                OTH_DISCR_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(OTH_DISCR_PRIMARY_PURPOSE_INDEX,
                OTH_DISCR_PRIMARY_PURPOSE_NAME);
        primaryTourPurposeNameIndexMap.put(WORK_BASED_PRIMARY_PURPOSE_NAME,
                WORK_BASED_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(WORK_BASED_PRIMARY_PURPOSE_INDEX,
                WORK_BASED_PRIMARY_PURPOSE_NAME);

    }

    /**
     * @return the HashMap<String,Integer> object that maps primary tour purpose
     *         names common to all CTRAMP implementations to indices (1-10).
     */
    public HashMap<String, Integer> getPrimaryPurposeNameIndexMap()
    {
        return primaryTourPurposeNameIndexMap;
    }

    /**
     * @return the HashMap<Integer,String> object that maps indices (1-10) to primary
     *         tour purpose names common to all CTRAMP implementations.
     */
    public HashMap<Integer, String> getIndexPrimaryPurposeNameMap()
    {
        return indexPrimaryTourPurposeNameMap;
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getSoaUecIndexForPurpose(String purposeKey)
    {
        return dcSoaUecIndexMap.get(purposeKey);
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getDcUecIndexForPurpose(String purposeKey)
    {
        return dcUecIndexMap.get(purposeKey);
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getTourModeChoiceUecIndexForPurpose(String purposeKey)
    {
        return tourModeChoiceUecIndexMap.get(purposeKey);
    }

    public String[] getDcModelPurposeList(String tourCategory)
    {
        if (tourCategory.equalsIgnoreCase(MANDATORY_CATEGORY)) return mandatoryDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(JOINT_NON_MANDATORY_CATEGORY)) return jointDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(INDIVIDUAL_NON_MANDATORY_CATEGORY)) return nonMandatoryDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(AT_WORK_CATEGORY)) return atWorkDcModelPurposeNames;
        else return null;
    }

    public String getDcSizeCoeffPurposeFieldName()
    {
        return dcSizeCoeffPurposeFieldName;
    }

    public String getDcSizeCoeffSegmentFieldName()
    {
        return this.dcSizeCoeffSegmentFieldName;
    }

    public String getAtWorkEatPurposeName()
    {
        return AT_WORK_EAT_PURPOSE_NAME;
    }

    public String[] getAtWorkSegmentNames()
    {
        return AT_WORK_SEGMENT_NAMES;
    }

    public String getAtWorkBusinessPurposeName()
    {
        return AT_WORK_BUSINESS_PURPOSE_NAME;
    }

    public String getAtWorkMaintPurposeName()
    {
        return AT_WORK_MAINT_PURPOSE_NAME;
    }

    public int getAtWorkEatPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_EAT;
    }

    public int getAtWorkBusinessPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_BUSINESS;
    }

    public int getAtWorkMaintPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_MAINT;
    }

    /** 
     * @param departPeriod is the model TOD interval for the departure period (for tour or trip)
     * @return the skim period index associated with the departure interval
     */
    public static int getSkimPeriodIndex(int departPeriod)
    {

        int skimPeriodIndex = 0;

        if (departPeriod <= UPPER_EA)
            skimPeriodIndex = EA_SKIM_PERIOD_INDEX;
        else if (departPeriod <= UPPER_AM)
            skimPeriodIndex = AM_SKIM_PERIOD_INDEX;
        else if (departPeriod <= UPPER_MD)
            skimPeriodIndex = MD_SKIM_PERIOD_INDEX;
        else if (departPeriod <= UPPER_PM)
            skimPeriodIndex = PM_SKIM_PERIOD_INDEX;
        else
            skimPeriodIndex = EV_SKIM_PERIOD_INDEX;

        return skimPeriodIndex;

    }

    
    /**
     * @param departPeriod is the model TOD interval for the departure period (for tour or trip)
     * @return the model period index associated with the departure interval
     *  Model periods: 0=EA, 1=AM, 2=MD, 3=PM, 4=EV
     */
    public static int getModelPeriodIndex(int departPeriod)
    {

        int modelPeriodIndex = 0;

        if (departPeriod <= UPPER_EA)
            modelPeriodIndex = 0;
        else if (departPeriod <= UPPER_AM)
            modelPeriodIndex = 1;
        else if (departPeriod <= UPPER_MD)
            modelPeriodIndex = 2;
        else if (departPeriod <= UPPER_PM)
            modelPeriodIndex = 3;
        else
            modelPeriodIndex = 4;

        return modelPeriodIndex;

    }

    private void createIndexTimePeriodMap(){
        indexTimePeriodMap = new HashMap<Integer,Integer>();
        timePeriodIndexMap = new HashMap<Integer,Integer>();
        
        int numHours = LAST_DEPART_HOUR - FIRST_DEPART_HOUR;
        int numHalfHours = numHours*2;
        
        TOD_INTERVAL_LABELS = new String[numHalfHours+1];
        
        for (int i=1; i <= numHalfHours; i++){
            int time = ((int)(i/2) + FIRST_DEPART_HOUR)*100 + (i%2)*30;
            indexTimePeriodMap.put(i, time);
            timePeriodIndexMap.put(time, i);
            TOD_INTERVAL_LABELS[i] = Integer.toString(time);
        }
    }
    
    public String[] getTimePeriodLabelArray(){
        return TOD_INTERVAL_LABELS;
    }
    
    public String getTimePeriodLabel(int timePeriodIndex){
        return TOD_INTERVAL_LABELS[timePeriodIndex]; 
    }
    
    // time argument is specified as: 500 for 5 am, 530 for 5:30 am, 1530 for 3:30 pm, etc.
    public int getTimePeriodIndexForTime(int time){
        return timePeriodIndexMap.get(time);
    }

    public int getNumberOfTimePeriods(){
        return TOD_INTERVAL_LABELS.length - 1;
    }
    
    public String[] getAwfAltLabels()
    {
        return awfAltLabels;
    }

}
