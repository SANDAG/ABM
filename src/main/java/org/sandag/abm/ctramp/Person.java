package org.sandag.abm.ctramp;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public class Person
        implements java.io.Serializable
{

    // 8 am default departure period
    public static final int      DEFAULT_MANDATORY_START_PERIOD             = 8;
    // 10 am default departure period
    public static final int      DEFAULT_NON_MANDATORY_START_PERIOD         = 12;
    // 12 pm default departure period
    public static final int      DEFAULT_AT_WORK_SUBTOUR_START_PERIOD       = 16;
    // 5 pm default arrival period
    public static final int      DEFAULT_MANDATORY_END_PERIOD               = 26;
    // 3 pm default arrival period
    public static final int      DEFAULT_NON_MANDATORY_END_PERIOD           = 22;
    // 2 pm default arrival period
    public static final int      DEFAULT_AT_WORK_SUBTOUR_END_PERIOD         = 20;

    public static final int      MIN_ADULT_AGE                              = 19;
    public static final int      MIN_STUDENT_AGE                            = 5;

    // person type strings used for data summaries
    public static final String   PERSON_TYPE_FULL_TIME_WORKER_NAME          = "Full-time worker";
    public static final String   PERSON_TYPE_PART_TIME_WORKER_NAME          = "Part-time worker";
    public static final String   PERSON_TYPE_UNIVERSITY_STUDENT_NAME        = "University student";
    public static final String   PERSON_TYPE_NON_WORKER_NAME                = "Non-worker";
    public static final String   PERSON_TYPE_RETIRED_NAME                   = "Retired";
    public static final String   PERSON_TYPE_STUDENT_DRIVING_NAME           = "Student of driving age";
    public static final String   PERSON_TYPE_STUDENT_NON_DRIVING_NAME       = "Student of non-driving age";
    public static final String   PERSON_TYPE_PRE_SCHOOL_CHILD_NAME          = "Child too young for school";

    public static final String[] PERSON_TYPE_NAME_ARRAY                     = {
            PERSON_TYPE_FULL_TIME_WORKER_NAME, PERSON_TYPE_PART_TIME_WORKER_NAME,
            PERSON_TYPE_UNIVERSITY_STUDENT_NAME, PERSON_TYPE_NON_WORKER_NAME,
            PERSON_TYPE_RETIRED_NAME, PERSON_TYPE_STUDENT_DRIVING_NAME,
            PERSON_TYPE_STUDENT_NON_DRIVING_NAME, PERSON_TYPE_PRE_SCHOOL_CHILD_NAME};

    // Employment category (1-employed FT, 2-employed PT, 3-not employed,
    // 4-under age
    // 16)
    // Student category (1 - student in grade or high school; 2 - student in
    // college
    // or higher; 3 - not a student)

    public static final String   EMPLOYMENT_CATEGORY_FULL_TIME_WORKER_NAME  = "Full-time worker";
    public static final String   EMPLOYMENT_CATEGORY_PART_TIME_WORKER_NAME  = "Part-time worker";
    public static final String   EMPLOYMENT_CATEGORY_NOT_EMPLOYED_NAME      = "Not employed";
    public static final String   EMPLOYMENT_CATEGORY_UNDER_AGE_16_NAME      = "Under age 16";

    public static final String[] EMPLOYMENT_CATEGORY_NAME_ARRAY             = {
            EMPLOYMENT_CATEGORY_FULL_TIME_WORKER_NAME, EMPLOYMENT_CATEGORY_PART_TIME_WORKER_NAME,
            EMPLOYMENT_CATEGORY_NOT_EMPLOYED_NAME, EMPLOYMENT_CATEGORY_UNDER_AGE_16_NAME};

    public static final String   STUDENT_CATEGORY_GRADE_OR_HIGH_SCHOOL_NAME = "Grade or high school";
    public static final String   STUDENT_CATEGORY_COLLEGE_OR_HIGHER_NAME    = "College or higher";
    public static final String   STUDENT_CATEGORY_NOT_STUDENT_NAME          = "Not student";

    public static final String[] STUDENT_CATEGORY_NAME_ARRAY                = {
            STUDENT_CATEGORY_GRADE_OR_HIGH_SCHOOL_NAME, STUDENT_CATEGORY_COLLEGE_OR_HIGHER_NAME,
            STUDENT_CATEGORY_NOT_STUDENT_NAME                               };

    private Household            hhObj;

    private int                  persNum;
    private int                  persId;
    private int                  persAge;
    private int                  persGender;
    private int                  persPecasOccup;
    private int                  persActivityCode;
    private int                  persEmploymentCategory;
    private int                  persStudentCategory;
    private int                  personType;
    private boolean              gradeSchool;
    private boolean              highSchool;
    private boolean              highSchoolGraduate;
    private boolean              hasBachelors;

    // individual value-of-time in $/hr
    private float                persValueOfTime;

    private int                  workLocation;
    private int                  workLocSegmentIndex;
    private float                workLocDistance;
    private float                workLocLogsum;
    private int                  schoolLoc;
    private int                  schoolLocSegmentIndex;
    private float                schoolLocDistance;
    private float                schoolLocLogsum;
    
    private double               timeFactorWork;
    private double               timeFactorNonWork;

    private int                  freeParkingAvailable;
    private int                  internalExternalTripChoice                 = 1;
    private double               reimbursePercent;
    
    private float                worksFromHomeLogsum;
    private float                parkingProvisionLogsum;
	private float                ieLogsum;
    private float                cdapLogsum;
    private float                imtfLogsum;
    private float                inmtfLogsum;
     
    private String               cdapActivity;
    private int                  imtfChoice;
    private int                  inmtfChoice;

    private int                  maxAdultOverlaps;
    private int                  maxChildOverlaps;

    private ArrayList<Tour>      workTourArrayList;
    private ArrayList<Tour>      schoolTourArrayList;
    private ArrayList<Tour>      indNonManTourArrayList;
    private ArrayList<Tour>      atWorkSubtourArrayList;

    // private Scheduler scheduler;
    // windows[] is 1s based - indexed from 1 to number of intervals.
    private int[]                windows;

    private int                  windowBeforeFirstMandJointTour;
    private int                  windowBetweenFirstLastMandJointTour;
    private int                  windowAfterLastMandJointTour;

    private ModelStructure       modelStructure;

    public Person(Household hhObj, int persNum, ModelStructure modelStructure)
    {
        this.hhObj = hhObj;
        this.persNum = persNum;
        this.workTourArrayList = new ArrayList<Tour>();
        this.schoolTourArrayList = new ArrayList<Tour>();
        this.indNonManTourArrayList = new ArrayList<Tour>();
        this.atWorkSubtourArrayList = new ArrayList<Tour>();
        this.modelStructure = modelStructure;

        initializeWindows();

        freeParkingAvailable = ParkingProvisionModel.FP_MODEL_REIMB_ALT;
        reimbursePercent = 0.43;
    }

    public Household getHouseholdObject()
    {
        return hhObj;
    }

    public ArrayList<Tour> getListOfWorkTours()
    {
        return workTourArrayList;
    }

    public ArrayList<Tour> getListOfSchoolTours()
    {
        return schoolTourArrayList;
    }

    public ArrayList<Tour> getListOfIndividualNonMandatoryTours()
    {
        return indNonManTourArrayList;
    }

    public ArrayList<Tour> getListOfAtWorkSubtours()
    {
        return atWorkSubtourArrayList;
    }

    public int[] getTimeWindows()
    {
        return windows;
    }

    public String getTimePeriodLabel(int windowIndex)
    {
        return modelStructure.getTimePeriodLabel(windowIndex);
    }

    public void initializeWindows()
    {
        windows = new int[modelStructure.getNumberOfTimePeriods() + 1];
    }

    public void resetTimeWindow(int startPeriod, int endPeriod)
    {
        for (int i = startPeriod; i <= endPeriod; i++)
        {
            windows[i] = 0;
        }
    }

    public void resetTimeWindow()
    {
        for (int i = 0; i < windows.length; i++)
        {
            windows[i] = 0;
        }
    }

    /**
     * code the time window array for this tour being scheduled. 0: unscheduled,
     * 1: scheduled, middle of tour, 2: scheduled, start of tour, 3: scheduled,
     * end of tour, 4: scheduled, end of previous tour, start of current tour or
     * end of current tour, start of subsequent tour; or current tour start/end
     * same period.
     * 
     * @param start
     *            is the departure period index for the tour
     * @param end
     *            is the arrival period index for the tour
     */
    public void scheduleWindow(int start, int end)
    {

        /*
         * This is the logic used in ARC/MTC, but for SANDAG, we don't allow
         * overlapping tours
         * 
         * 
         * if (start == end) { windows[start] = 4; } else { if (windows[start]
         * == 3) windows[start] = 4; else if (windows[start] == 0)
         * windows[start] = 2;
         * 
         * if (windows[end] == 2) windows[end] = 4; else if (windows[end] == 0)
         * windows[end] = 3; }
         * 
         * for (int h = start + 1; h < end; h++) { windows[h] = 1; }
         */

        for (int h = start; h <= end; h++)
        {
            windows[h] = 1;
        }

    }

    public boolean[] getAvailableTimeWindows(int[] altStarts, int[] altEnds)
    {

        // availability array is used by UEC based choice model, which uses
        // 1-based
        // indexing
        boolean[] availability = new boolean[altStarts.length + 1];

        for (int i = 1; i <= altStarts.length; i++)
        {
            int start = altStarts[i - 1];
            int end = altEnds[i - 1];
            availability[i] = isWindowAvailable(start, end);
        }

        return availability;
    }

    public boolean isWindowAvailable(int start, int end)
    {

        /*
         * This is the logic used in ARC/MTC, but for SANDAG, we don't allow
         * overlapping tours
         * 
         * 
         * // check start period, if window is 0, it is unscheduled; // if
         * window is 3, it is the last period of another tour, and available //
         * as the first period of this tour. if (windows[start] == 1) return
         * false; else if (windows[start] == 2 && start != end) return false;
         * 
         * // check end period, if window is 0, it is unscheduled; // if window
         * is 2, it is the first period of another tour, and available // as the
         * last period of this tour. if (windows[end] == 1) return false; else
         * if (windows[end] == 3 && start != end) return false;
         * 
         * // the alternative is available if start and end are available, and
         * all periods // from start+1,...,end-1 are available. for (int h =
         * start + 1; h < end; h++) { if (windows[h] > 0) return false; }
         * 
         * return true;
         */

        // the alternative is available if all intervals between start and end,
        // inclusive, are available
        for (int h = start; h <= end; h++)
        {
            if (windows[h] > 0) return false;
        }

        return true;

    }

    /**
     * @return true if the window for the argument is the end of a previously
     *         scheduled tour and this period does not overlap with any other
     *         tour.
     */
    public boolean isPreviousArrival(int period)
    {

        if (windows[period] == 3 || windows[period] == 4) return true;
        else return false;

    }

    /**
     * @return true if the window for the argument is the start of a previously
     *         scheduled tour and this period does not overlap with any other
     *         tour.
     */
    public boolean isPreviousDeparture(int period)
    {

        if (windows[period] == 2 || windows[period] == 4) return true;
        else return false;

    }

    public boolean isPeriodAvailable(int period)
    {
        // if windows[index] == 0, the period is available.

        // if window[index] is 0 (available), 2 (start of another tour), 3 (end
        // of
        // another tour), 4 available for this period only, the period is
        // available;
        // otherwise, if window[index] is 1 (middle of another tour), it is not
        // available.
        if (windows[period] == 1) return false;
        else return true;
    }

    public void setPersId(int id)
    {
        persId = id;
    }

    public void setFreeParkingAvailableResult(int chosenAlt)
    {
        freeParkingAvailable = chosenAlt;
    }

    /**
     * set the chosen alternative number: 1=no, 2=yes
     * 
     * @param chosenAlt
     */
    public void setInternalExternalTripChoiceResult(int chosenAlt)
    {
        internalExternalTripChoice = chosenAlt;
    }

    public void setParkingReimbursement(double pct)
    {
        reimbursePercent = pct;
    }

    public void setWorkLocationSegmentIndex(int workSegment)
    {
        workLocSegmentIndex = workSegment;
    }

    public void setSchoolLocationSegmentIndex(int schoolSegment)
    {
        schoolLocSegmentIndex = schoolSegment;
    }

    public void setPersAge(int age)
    {
        persAge = age;
    }

    public void setPersGender(int gender)
    {
        persGender = gender;
    }

    public void setPersPecasOccup(int occup)
    {
        persPecasOccup = occup;
    }

    public void setPersActivityCode(int actCode)
    {
        persActivityCode = actCode;
    }

    public void setPersEmploymentCategory(int category)
    {
        persEmploymentCategory = category;
    }

    public void setPersStudentCategory(int category)
    {
        persStudentCategory = category;
    }

    public void setPersonTypeCategory(int personTypeCategory)
    {
        personType = personTypeCategory;
    }

    public void setValueOfTime(float vot)
    {
        persValueOfTime = vot;
    }

    public void setWorkLocation(int aWorkLocationMgra)
    {
        workLocation = aWorkLocationMgra;
    }

    public void setWorkLocDistance(float distance)
    {
        workLocDistance = distance;
    }

    public void setWorkLocLogsum(float logsum)
    {
        workLocLogsum = logsum;
    }

    public void setSchoolLoc(int loc)
    {
        schoolLoc = loc;
    }

    public void setSchoolLocDistance(float distance)
    {
        schoolLocDistance = distance;
    }

    public void setSchoolLocLogsum(float logsum)
    {
        schoolLocLogsum = logsum;
    }

    public void setImtfChoice(int choice)
    {
        imtfChoice = choice;
    }

    public void setInmtfChoice(int choice)
    {
        inmtfChoice = choice;
    }

    public int getImtfChoice()
    {
        return imtfChoice;
    }

    public int getInmtfChoice()
    {
        return inmtfChoice;
    }

    public void clearIndividualNonMandatoryToursArray()
    {
        indNonManTourArrayList.clear();
    }

    public void createIndividualNonMandatoryTours(int numberOfTours, String primaryPurposeName)
    {

        /*
         * // if purpose is escort, need to determine if household has kids or
         * not String purposeName = primaryPurposeName; if (
         * purposeName.equalsIgnoreCase( modelStructure.ESCORT_PURPOSE_NAME ) )
         * { if ( hhObj.getNumChildrenUnder19() > 0 ) purposeName += "_" +
         * modelStructure.ESCORT_SEGMENT_NAMES[0]; else purposeName += "_" +
         * modelStructure.ESCORT_SEGMENT_NAMES[1]; } int purposeIndex =
         * modelStructure.getDcModelPurposeIndex( purposeName );
         */

        int id = indNonManTourArrayList.size();

        int primaryIndex = modelStructure.getPrimaryPurposeNameIndexMap().get(primaryPurposeName);

        for (int i = 0; i < numberOfTours; i++)
        {
            Tour tempTour = new Tour(id++, this.hhObj, this, primaryPurposeName,
                    ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY, primaryIndex);

            tempTour.setTourOrigMgra(this.hhObj.getHhMgra());
            tempTour.setTourDestMgra(0);

            tempTour.setTourPurpose(primaryPurposeName);

            tempTour.setTourDepartPeriod(DEFAULT_NON_MANDATORY_START_PERIOD);
            tempTour.setTourArrivePeriod(DEFAULT_NON_MANDATORY_END_PERIOD);

            indNonManTourArrayList.add(tempTour);
        }

    }

    public void createWorkTours(int numberOfTours, int startId, String tourPurpose,
            int tourPurposeIndex)
    {

        workTourArrayList.clear();

        for (int i = 0; i < numberOfTours; i++)
        {
            int id = startId + i;
            Tour tempTour = new Tour(this, id, tourPurposeIndex);

            tempTour.setTourOrigMgra(hhObj.getHhMgra());

            if (workLocation == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR) tempTour
                    .setTourDestMgra(hhObj.getHhMgra());
            else tempTour.setTourDestMgra(workLocation);

            tempTour.setTourPurpose(tourPurpose);

            tempTour.setTourDepartPeriod(-1);
            tempTour.setTourArrivePeriod(-1);
            // tempTour.setTourDepartPeriod(DEFAULT_MANDATORY_START_PERIOD);
            // tempTour.setTourArrivePeriod(DEFAULT_MANDATORY_END_PERIOD);

            workTourArrayList.add(tempTour);
        }

    }

    public void clearAtWorkSubtours()
    {

        atWorkSubtourArrayList.clear();

    }

    public void createAtWorkSubtour(int id, int choice, int workMgra, String subtourPurpose)
    {

        /*
         * String segmentedPurpose = modelStructure.AT_WORK_PURPOSE_NAME + "_" +
         * tourPurpose; int purposeIndex =
         * modelStructure.getDcModelPurposeIndex( segmentedPurpose );
         */

        Tour tempTour = new Tour(id, this.hhObj, this,
                ModelStructure.WORK_BASED_PRIMARY_PURPOSE_NAME, ModelStructure.AT_WORK_CATEGORY,
                ModelStructure.WORK_BASED_PRIMARY_PURPOSE_INDEX);

        tempTour.setTourOrigMgra(workMgra);
        tempTour.setTourDestMgra(0);

        tempTour.setTourPurpose(ModelStructure.WORK_BASED_PRIMARY_PURPOSE_NAME);
        tempTour.setSubTourPurpose(subtourPurpose);

        tempTour.setTourDepartPeriod(DEFAULT_AT_WORK_SUBTOUR_START_PERIOD);
        tempTour.setTourArrivePeriod(DEFAULT_AT_WORK_SUBTOUR_END_PERIOD);

        atWorkSubtourArrayList.add(tempTour);

    }

    public void createSchoolTours(int numberOfTours, int startId, String tourPurpose,
            int tourPurposeIndex)
    {

        schoolTourArrayList.clear();

        for (int i = 0; i < numberOfTours; i++)
        {
            int id = startId + i;
            Tour tempTour = new Tour(this, id, tourPurposeIndex);

            tempTour.setTourOrigMgra(this.hhObj.getHhMgra());

            if (schoolLoc == ModelStructure.NOT_ENROLLED_SEGMENT_INDEX) tempTour
                    .setTourDestMgra(hhObj.getHhMgra());
            else tempTour.setTourDestMgra(schoolLoc);

            tempTour.setTourPurpose(tourPurpose);

            tempTour.setTourDepartPeriod(-1);
            tempTour.setTourArrivePeriod(-1);
            // tempTour.setTourDepartPeriod(DEFAULT_MANDATORY_START_PERIOD);
            // tempTour.setTourArrivePeriod(DEFAULT_MANDATORY_END_PERIOD);

            schoolTourArrayList.add(tempTour);
        }
    }

    public int getWorkLocationSegmentIndex()
    {
        return workLocSegmentIndex;
    }

    public int getSchoolLocationSegmentIndex()
    {
        return schoolLocSegmentIndex;
    }

    public void setDailyActivityResult(String activity)
    {
        this.cdapActivity = activity;
    }

    public int getPersonIsChildUnder16WithHomeOrNonMandatoryActivity()
    {

        // check the person type
        if (persIsStudentNonDrivingAge() == 1 || persIsPreschoolChild() == 1)
        {

            // check the activity type
            if (cdapActivity.equalsIgnoreCase(ModelStructure.HOME_PATTERN)) return (1);

            if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return (1);

        }

        return (0);
    }

    /**
     * @return 1 if M, 2 if N, 3 if H
     */
    public int getCdapIndex()
    {

        // return the activity type
        if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return 1;

        if (cdapActivity.equalsIgnoreCase(ModelStructure.NONMANDATORY_PATTERN)) return 2;

        if (cdapActivity.equalsIgnoreCase(ModelStructure.HOME_PATTERN)) return 3;

        return (0);
    }

    public int getPersonIsChild6To18WithoutMandatoryActivity()
    {

        // check the person type
        if (persIsStudentDrivingAge() == 1 || persIsStudentNonDrivingAge() == 1)
        {

            // check the activity type
            if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return 0;
            else return 1;

        }

        return 0;
    }

    // methods DMU will use to get info from household object

    public int getAge()
    {
        return persAge;
    }

    public int getHomemaker()
    {
        return persIsHomemaker();
    }

    public int getGender()
    {
        return persGender;
    }

    public int getPersonIsFemale()
    {
        if (persGender == 2) return 1;
        return 0;
    }

    public int getPersonIsMale()
    {
        if (persGender == 1) return 1;
        return 0;
    }

    public int getPersonId()
    {
        return this.persId;
    }

    public int getPersonNum()
    {
        return this.persNum;
    }

    public String getPersonType()
    {
        return PERSON_TYPE_NAME_ARRAY[personType - 1];
    }

    public void setPersonIsHighSchool(boolean flag)
    {
        highSchool = flag;
    }

    public int getPersonIsHighSchool()
    {
        return highSchool ? 1 : 0;
    }

    public void setPersonIsGradeSchool(boolean flag)
    {
        gradeSchool = flag;
    }

    public int getPersonIsGradeSchool()
    {
        return gradeSchool ? 1 : 0;
    }

    public int getPersonIsHighSchoolGraduate()
    {
        return highSchoolGraduate ? 1 : 0;
    }

    public void setPersonIsHighSchoolGraduate(boolean hsGrad)
    {
        highSchoolGraduate = hsGrad;
    }

    public void setPersonHasBachelors(boolean hasBS)
    {
        hasBachelors = hasBS;
    }

    public int getPersonTypeNumber()
    {
        return personType;
    }

    public int getPersPecasOccup()
    {
        return persPecasOccup;
    }

    public int getPersActivityCode()
    {
        return persActivityCode;
    }

    public int getPersonEmploymentCategoryIndex()
    {
        return persEmploymentCategory;
    }

    public String getPersonEmploymentCategory()
    {
        return EMPLOYMENT_CATEGORY_NAME_ARRAY[persEmploymentCategory - 1];
    }

    public int getPersonStudentCategoryIndex()
    {
        return persStudentCategory;
    }

    public String getPersonStudentCategory()
    {
        return STUDENT_CATEGORY_NAME_ARRAY[persStudentCategory - 1];
    }

    public float getValueOfTime()
    {
        return persValueOfTime;
    }

    public int getWorkLocation()
    {
        return workLocation;
    }

    public int getPersonSchoolLocationZone()
    {
        return schoolLoc;
    }

    public int getFreeParkingAvailableResult()
    {
        return freeParkingAvailable;
    }

    public int getInternalExternalTripChoiceResult()
    {
        return internalExternalTripChoice;
    }

    public double getParkingReimbursement()
    {
        return reimbursePercent;
    }

    public String getCdapActivity()
    {
        return cdapActivity;
    }

    public float getWorkLocationDistance()
    {
        return workLocDistance;
    }

    public float getWorkLocationLogsum()
    {
        return workLocLogsum;
    }

    public int getUsualSchoolLocation()
    {
        return schoolLoc;
    }

    public float getSchoolLocationDistance()
    {
        return schoolLocDistance;
    }

    public float getSchoolLocationLogsum()
    {
        return schoolLocLogsum;
    }

    public int getHasBachelors()
    {
        return hasBachelors ? 1 : 0;
    }

    public int getNumWorkTours()
    {
        ArrayList<Tour> workTours = getListOfWorkTours();
        if (workTours != null) return workTours.size();
        else return 0;
    }

    public int getNumSchoolTours()
    {
        ArrayList<Tour> schoolTours = getListOfSchoolTours();
        if (schoolTours != null) return schoolTours.size();
        else return 0;
    }

    public int getNumIndividualEscortTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.ESCORT_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualShoppingTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.SHOPPING_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumIndividualEatOutTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.EAT_OUT_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualOthMaintTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.OTH_MAINT_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumIndividualSocialTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.SOCIAL_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualOthDiscrTours()
    {
        int num = 0;
        for (Tour tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.OTH_DISCR_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumMandatoryTours()
    {
        int numTours = 0;
        ArrayList<Tour> workTours = getListOfWorkTours();
        if (workTours != null) numTours += workTours.size();

        ArrayList<Tour> schoolTours = getListOfSchoolTours();
        if (schoolTours != null) numTours += schoolTours.size();

        return numTours;
    }

    public int getNumNonMandatoryTours()
    {
        ArrayList<Tour> nonMandTours = getListOfIndividualNonMandatoryTours();
        if (nonMandTours == null) return 0;
        else return nonMandTours.size();
    }

    public int getNumSubtours()
    {
        ArrayList<Tour> subtours = getListOfAtWorkSubtours();
        if (subtours == null) return 0;
        else return subtours.size();
    }

    public int getNumTotalIndivTours()
    {
        return getNumMandatoryTours() + getNumNonMandatoryTours() + getNumSubtours();
    }

    public int getNumJointShoppingTours()
    {
        return getNumJointToursForPurpose(modelStructure.SHOPPING_PURPOSE_NAME);
    }

    public int getNumJointOthMaintTours()
    {
        return getNumJointToursForPurpose(modelStructure.OTH_MAINT_PURPOSE_NAME);
    }

    public int getNumJointEatOutTours()
    {
        return getNumJointToursForPurpose(modelStructure.EAT_OUT_PURPOSE_NAME);
    }

    public int getNumJointSocialTours()
    {
        return getNumJointToursForPurpose(modelStructure.SOCIAL_PURPOSE_NAME);
    }

    public int getNumJointOthDiscrTours()
    {
        return getNumJointToursForPurpose(modelStructure.OTH_DISCR_PURPOSE_NAME);
    }

    private int getNumJointToursForPurpose(String purposeName)
    {
        int count = 0;
        Tour[] jt = hhObj.getJointTourArray();
        if (jt == null) return 0;

        for (int i = 0; i < jt.length; i++)
        {
            if (jt[i] == null) continue;
            String jtPurposeName = jt[i].getTourPurpose();
            int[] personNumsParticipating = jt[i].getPersonNumArray();
            for (int p : personNumsParticipating)
            {
                if (p == persNum)
                {
                    if (jtPurposeName.equalsIgnoreCase(purposeName)) count++;
                    break;
                }
            }
        }

        return count;
    }

    public void computeIdapResidualWindows()
    {

        // find the start of the earliest mandatory or joint tour for this
        // person
        // and end of last one.
        int firstTourStart = 9999;
        int lastTourEnd = -1;
        int firstTourEnd = 0;
        int lastTourStart = 0;

        // first check mandatory tours
        for (Tour tour : workTourArrayList)
        {
            int tourDeparts = tour.getTourDepartPeriod();
            int tourArrives = tour.getTourArrivePeriod();

            if (tourDeparts < firstTourStart)
            {
                firstTourStart = tourDeparts;
                firstTourEnd = tourArrives;
            }

            if (tourArrives > lastTourEnd)
            {
                lastTourStart = tourDeparts;
                lastTourEnd = tourArrives;
            }
        }

        for (Tour tour : schoolTourArrayList)
        {
            int tourDeparts = tour.getTourDepartPeriod();
            int tourArrives = tour.getTourArrivePeriod();

            if (tourDeparts < firstTourStart)
            {
                firstTourStart = tourDeparts;
                firstTourEnd = tourArrives;
            }

            if (tourArrives > lastTourEnd)
            {
                lastTourStart = tourDeparts;
                lastTourEnd = tourArrives;
            }
        }

        // now check joint tours
        Tour[] jointTourArray = hhObj.getJointTourArray();
        if (jointTourArray != null)
        {
            for (Tour tour : jointTourArray)
            {

                if (tour == null) continue;

                // see if this person is in the joint tour or not
                if (tour.getPersonInJointTour(this))
                {

                    int tourDeparts = tour.getTourDepartPeriod();
                    int tourArrives = tour.getTourArrivePeriod();

                    if (tourDeparts < firstTourStart)
                    {
                        firstTourStart = tourDeparts;
                        firstTourEnd = tourArrives;
                    }

                    if (tourArrives > lastTourEnd)
                    {
                        lastTourStart = tourDeparts;
                        lastTourEnd = tourArrives;
                    }

                }

            }
        }

        if (firstTourStart > modelStructure.getNumberOfTimePeriods() - 1 && lastTourEnd < 0)
        {
            int numPeriods = windows.length;
            windowBeforeFirstMandJointTour = numPeriods;
            windowAfterLastMandJointTour = numPeriods;
            windowBetweenFirstLastMandJointTour = numPeriods;
        } else
        {

            // since first tour first period and last tour last period are
            // available,
            // account for them.
            windowBeforeFirstMandJointTour = firstTourStart + 1;
            windowAfterLastMandJointTour = modelStructure.getNumberOfTimePeriods() - lastTourEnd;

            // find the number of unscheduled periods between end of first tour
            // and
            // start of last tour
            windowBetweenFirstLastMandJointTour = 0;
            for (int i = firstTourEnd; i <= lastTourStart; i++)
            {
                if (isPeriodAvailable(i)) windowBetweenFirstLastMandJointTour++;
            }
        }

    }

    public int getWindowBeforeFirstMandJointTour()
    {
        return windowBeforeFirstMandJointTour;
    }

    public int getWindowBetweenFirstLastMandJointTour()
    {
        return windowBetweenFirstLastMandJointTour;
    }

    public int getWindowAfterLastMandJointTour()
    {
        return windowAfterLastMandJointTour;
    }

    // public int getNumberOfMandatoryWorkTours( String workPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPurposeString().equalsIgnoreCase(
    // workPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }
    //
    // public int getNumberOfMandatorySchoolTours( String schoolPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPurposeString().equalsIgnoreCase(
    // schoolPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }
    //
    // public int getNumberOfMandatoryWorkAndSchoolTours( String
    // workAndschoolPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPurposeString().equalsIgnoreCase(
    // workAndschoolPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }

    /**
     * determine if person is a worker (indepdent of person type).
     * 
     * @return 1 if worker, 0 otherwise.
     */
    public int getPersonIsWorker()
    {
        return persIsWorker();
    }

    /**
     * Determine if person is a student (of any age, independent of person type)
     * 
     * @return 1 if student, 0 otherwise
     */
    public int getPersonIsStudent()
    {
        return persIsStudent();
    }

    public int getPersonIsUniversityStudent()
    {
        return persIsUniversity();
    }

    public int getPersonIsTypicalUniversityStudent()
    {
        if (persIsUniversity() == 1) if (persAge < 30) return 1;
        else return 0;
        else return 0;
    }

    public int getPersonIsStudentDriving()
    {
        return persIsStudentDrivingAge();
    }

    public int getPersonIsStudentNonDriving()
    {
        return persIsStudentNonDrivingAge();
    }

    /**
     * Determine if person is a full-time worker (independent of person type)
     * 
     * @return 1 if full-time worker, 0 otherwise
     */
    public int getPersonIsFullTimeWorker()
    {
        return persIsFullTimeWorker();
    }

    /**
     * Determine if person is a part-time worker (indepdent of person type)
     */
    public int getPersonIsPartTimeWorker()
    {
        return persIsPartTimeWorker();
    }

    public int getPersonTypeIsFullTimeWorker()
    {
        return persTypeIsFullTimeWorker();
    }

    public int getPersonTypeIsPartTimeWorker()
    {
        return persTypeIsPartTimeWorker();
    }

    public int getPersonIsNonWorkingAdultUnder65()
    {
        return persIsNonWorkingAdultUnder65();
    }

    public int getPersonIsNonWorkingAdultOver65()
    {
        return persIsNonWorkingAdultOver65();
    }

    public int getPersonIsPreschoolChild()
    {
        return persIsPreschoolChild();
    }

    public int getPersonIsAdult()
    {
        if (persIsPreschoolChild() == 1 || getPersonIsStudentNonDriving() == 1) return 0;
        else return 1;
    }

    private int persIsHomemaker()
    {
        if (persAge >= MIN_ADULT_AGE
                && persEmploymentCategory == EmployStatus.NOT_EMPLOYED.ordinal()) return 1;
        else return 0;
    }

    public int notEmployed()
    {
        if (persEmploymentCategory == EmployStatus.NOT_EMPLOYED.ordinal()) return 1;
        else return 0;
    }

    private int persIsWorker()
    {
        if (persEmploymentCategory == EmployStatus.FULL_TIME.ordinal()
                || persEmploymentCategory == EmployStatus.PART_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudent()
    {
        if (persStudentCategory == StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS.ordinal()
                || persStudentCategory == StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal())
        {
            return 1;
        } else
        {
            return 0;
        }
    }

    private int persIsFullTimeWorker()
    {
        if (persEmploymentCategory == EmployStatus.FULL_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persIsPartTimeWorker()
    {
        if (persEmploymentCategory == EmployStatus.PART_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persTypeIsFullTimeWorker()
    {
        if (personType == PersonType.FT_worker_age_16plus.ordinal()) return 1;
        else return 0;
    }

    private int persTypeIsPartTimeWorker()
    {
        if (personType == PersonType.PT_worker_nonstudent_age_16plus.ordinal()) return 1;
        else return 0;
    }

    private int persIsUniversity()
    {
        if (personType == PersonType.University_student.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudentDrivingAge()
    {
        if (personType == PersonType.Student_age_16_19_not_FT_wrkr_or_univ_stud.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudentNonDrivingAge()
    {
        if (personType == PersonType.Student_age_6_15_schpred.ordinal()) return 1;
        else return 0;
    }

    private int persIsPreschoolChild()
    {
        if (personType == PersonType.Preschool_under_age_6.ordinal()) return 1;
        else return 0;

    }

    private int persIsNonWorkingAdultUnder65()
    {
        if (personType == PersonType.Nonworker_nonstudent_age_16_64.ordinal()) return 1;
        else return 0;
    }

    private int persIsNonWorkingAdultOver65()
    {
        if (personType == PersonType.Nonworker_nonstudent_age_65plus.ordinal())
        {
            return 1;
        } else
        {
            return 0;
        }
    }

    /**
     * return maximum periods of overlap between this person and other adult
     * persons in the household.
     * 
     * @return the most number of periods mutually available between this person
     *         and other adult household members
     */
    public int getMaxAdultOverlaps()
    {
        return maxAdultOverlaps;
    }

    /**
     * set maximum periods of overlap between this person and other adult
     * persons in the household.
     * 
     * @param overlaps
     *            are the most number of periods mutually available between this
     *            person and other adult household members
     */
    public void setMaxAdultOverlaps(int overlaps)
    {
        maxAdultOverlaps = overlaps;
    }

    /**
     * return maximum periods of overlap between this person and other children
     * in the household.
     * 
     * @return the most number of periods mutually available between this person
     *         and other child household members
     */
    public int getMaxChildOverlaps()
    {
        return maxChildOverlaps;
    }

    /**
     * set maximum periods of overlap between this person and other children in
     * the household.
     * 
     * @param overlaps
     *            are the most number of periods mutually available between this
     *            person and other child household members
     */
    public void setMaxChildOverlaps(int overlaps)
    {
        maxChildOverlaps = overlaps;
    }

    /**
     * return available time window for this person in the household.
     * 
     * @return the total number of periods available for this person
     */
    public int getAvailableWindow()
    {
        int numPeriodsAvailable = 0;
        for (int i = 1; i < windows.length; i++)
            if (windows[i] != 1) numPeriodsAvailable++;

        return numPeriodsAvailable;
    }

    /**
     * determine the maximum consecutive available time window for the person
     * 
     * @return the length of the maximum available window in units of time
     *         intervals
     */
    public int getMaximumContinuousAvailableWindow()
    {
        int maxWindow = 0;
        int currentWindow = 0;
        for (int i = 1; i < windows.length; i++)
        {
            if (windows[i] == 0)
            {
                currentWindow++;
            } else
            {
                if (currentWindow > maxWindow) maxWindow = currentWindow;
                currentWindow = 0;
            }
        }
        if (currentWindow > maxWindow) maxWindow = currentWindow;

        return maxWindow;
    }

    /**
     * determine the maximum consecutive pairwise available time window for this
     * person and the person for which a window was passed
     * 
     * @return the length of the maximum pairwise available window in units of
     *         time intervals
     */
    public int getMaximumContinuousPairwiseAvailableWindow(int[] otherWindow)
    {
        int maxWindow = 0;
        int currentWindow = 0;
        for (int i = 1; i < windows.length; i++)
        {
            if (windows[i] == 0 && otherWindow[i] == 0)
            {
                currentWindow++;
            } else
            {
                if (currentWindow > maxWindow) maxWindow = currentWindow;
                currentWindow = 0;
            }
        }
        if (currentWindow > maxWindow) maxWindow = currentWindow;

        return maxWindow;
    }

    public void setTimeWindows(int[] win)
    {
        windows = win;
    }

    public void initializeForAoRestart()
    {

        cdapActivity = "-";
        imtfChoice = 0;
        inmtfChoice = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;

        workTourArrayList.clear();
        schoolTourArrayList.clear();
        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        initializeWindows();

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    public void initializeForImtfRestart()
    {

        imtfChoice = 0;
        inmtfChoice = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;

        workTourArrayList.clear();
        schoolTourArrayList.clear();
        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        initializeWindows();

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the
     * model at joint tour frequency
     */
    public void initializeForJtfRestart()
    {

        inmtfChoice = 0;

        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            Tour t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            Tour t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the
     * model at individual non-mandatory tour frequency.
     */
    public void initializeForInmtfRestart()
    {

        inmtfChoice = 0;

        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            Tour t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            Tour t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the
     * model at at-work sub-tour frequency.
     */
    public void initializeForAwfRestart()
    {

        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            Tour t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            Tour t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < indNonManTourArrayList.size(); i++)
        {
            Tour t = indNonManTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

    }

    /**
     * initialize the person attributes and tour objects for restarting the
     * model at stop frequency.
     */
    public void initializeForStfRestart()
    {

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            Tour t = workTourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            Tour t = schoolTourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < atWorkSubtourArrayList.size(); i++)
        {
            Tour t = atWorkSubtourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < indNonManTourArrayList.size(); i++)
        {
            Tour t = indNonManTourArrayList.get(i);
            t.clearStopModelResults();
        }

    }

    public float getParkingProvisionLogsum() {
		return parkingProvisionLogsum;
	}

	public void setParkingProvisionLogsum(float parkingProvisionLogsum) {
		this.parkingProvisionLogsum = parkingProvisionLogsum;
	}

	public float getIeLogsum() {
		return ieLogsum;
	}

	public void setIeLogsum(float ieLogsum) {
		this.ieLogsum = ieLogsum;
	}

	public float getCdapLogsum() {
		return cdapLogsum;
	}

	public void setCdapLogsum(float cdapLogsum) {
		this.cdapLogsum = cdapLogsum;
	}


    public float getImtfLogsum() {
		return imtfLogsum;
	}

	public void setImtfLogsum(float imtfLogsum) {
		this.imtfLogsum = imtfLogsum;
	}

	public float getInmtfLogsum() {
		return inmtfLogsum;
	}

	public void setInmtfLogsum(float inmtfLogsum) {
		this.inmtfLogsum = inmtfLogsum;
	}

	public float getWorksFromHomeLogsum() {
		return worksFromHomeLogsum;
	}

	public void setWorksFromHomeLogsum(float worksFromHomeLogsum) {
		this.worksFromHomeLogsum = worksFromHomeLogsum;
	}

	public void logPersonObject(Logger logger, int totalChars)
    {

        Household.logHelper(logger, "persNum: ", persNum, totalChars);
        Household.logHelper(logger, "persId: ", persId, totalChars);
        Household.logHelper(logger, "persAge: ", persAge, totalChars);
        Household.logHelper(logger, "persGender: ", persGender, totalChars);
        Household.logHelper(logger, "persEmploymentCategory: ", persEmploymentCategory, totalChars);
        Household.logHelper(logger, "persStudentCategory: ", persStudentCategory, totalChars);
        Household.logHelper(logger, "personType: ", personType, totalChars);
        Household.logHelper(logger, "workLoc: ", workLocation, totalChars);
        Household.logHelper(logger, "schoolLoc: ", schoolLoc, totalChars);
        Household.logHelper(logger, "workLocSegmentIndex: ", workLocSegmentIndex, totalChars);
        Household.logHelper(logger, "schoolLocSegmentIndex: ", schoolLocSegmentIndex, totalChars);
        
        Household.logHelper(logger, "timeFactorWork: ",  String.format("%.2f%%",timeFactorWork), totalChars);
        Household.logHelper(logger, "timeFactorNonWork: ",  String.format("%.2f%%",timeFactorNonWork), totalChars);
        Household.logHelper(logger, "freeParkingAvailable: ", freeParkingAvailable, totalChars);
        Household.logHelper(logger, "reimbursementPct: ",
                String.format("%.2f%%", (100 * reimbursePercent)), totalChars);
        Household.logHelper(logger, "cdapActivity: ", cdapActivity, totalChars);
        Household.logHelper(logger, "imtfChoice: ", imtfChoice, totalChars);
        Household.logHelper(logger, "inmtfChoice: ", inmtfChoice, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "windowBeforeFirstMandJointTour: ",
                windowBeforeFirstMandJointTour, totalChars);
        Household.logHelper(logger, "windowBetweenFirstLastMandJointTour: ",
                windowBetweenFirstLastMandJointTour, totalChars);
        Household.logHelper(logger, "windowAfterLastMandJointTour: ", windowAfterLastMandJointTour,
                totalChars);

        String header1 = "      Index:     |";
        String header2 = "     Period:     |";
        String windowString = "     Window:     |";
        String periodString = "";
        for (int i = 1; i < windows.length; i++)
        {
            header1 += String.format(" %2d |", i);
            header2 += String.format("%4s|", modelStructure.getTimePeriodLabel(i));
            switch (windows[i])
            {
                case 0:
                    periodString = "    ";
                    break;
                case 1:
                    periodString = "XXXX";
                    break;
            }
            windowString += String.format("%4s|", periodString);
        }

        logger.info(header1);
        logger.info(header2);
        logger.info(windowString);

        if (workTourArrayList.size() > 0)
        {
            for (Tour tour : workTourArrayList)
            {
                int id = tour.getTourId();
                logger.info(tour.getTourWindow(String.format("W%d", id)));
            }
        }
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (Tour tour : atWorkSubtourArrayList)
            {
                int id = tour.getTourId();
                String alias = "";
                String purposeName = tour.getSubTourPurpose();
                if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME)) alias = "aB";
                else if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_EAT_PURPOSE_NAME)) alias = "aE";
                else if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_MAINT_PURPOSE_NAME))
                    alias = "aM";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }
        if (schoolTourArrayList.size() > 0)
        {
            for (Tour tour : schoolTourArrayList)
            {
                int id = tour.getTourId();
                String alias = "S";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }
        if (hhObj.getJointTourArray() != null && hhObj.getJointTourArray().length > 0)
        {
            for (Tour tour : hhObj.getJointTourArray())
            {
                if (tour == null) continue;

                // log this persons time window if they are in the joint tour
                // party.
                int[] persNumArray = tour.getPersonNumArray();
                if (persNumArray != null)
                {
                    for (int num : persNumArray)
                    {
                        if (num == persNum)
                        {

                            Person person = hhObj.getPersons()[num];
                            tour.setPersonObject(person);

                            int id = tour.getTourId();
                            String alias = "";
                            if (tour.getTourPurpose().equalsIgnoreCase(
                                    ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)) alias = "jE";
                            else if (tour.getTourPurpose().equalsIgnoreCase(
                                    ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) alias = "jS";
                            else if (tour.getTourPurpose().equalsIgnoreCase(
                                    ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) alias = "jM";
                            else if (tour.getTourPurpose().equalsIgnoreCase(
                                    ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) alias = "jV";
                            else if (tour.getTourPurpose().equalsIgnoreCase(
                                    ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) alias = "jD";
                            logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
                        }
                    }
                }
            }
        }
        if (indNonManTourArrayList.size() > 0)
        {
            for (Tour tour : indNonManTourArrayList)
            {
                int id = tour.getTourId();
                String alias = "";
                if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)) alias = "ie";
                else if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)) alias = "iE";
                else if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) alias = "iS";
                else if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) alias = "iM";
                else if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) alias = "iV";
                else if (tour.getTourPurpose().equalsIgnoreCase(
                        ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) alias = "iD";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }

    }

    public void logTourObject(Logger logger, int totalChars, Tour tour)
    {
        tour.logTourObject(logger, totalChars);
    }

    public void logEntirePersonObject(Logger logger)
    {

        int totalChars = 60;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        Household.logHelper(logger, "persNum: ", persNum, totalChars);
        Household.logHelper(logger, "persId: ", persId, totalChars);
        Household.logHelper(logger, "persAge: ", persAge, totalChars);
        Household.logHelper(logger, "persGender: ", persGender, totalChars);
        Household.logHelper(logger, "persEmploymentCategory: ", persEmploymentCategory, totalChars);
        Household.logHelper(logger, "persStudentCategory: ", persStudentCategory, totalChars);
        Household.logHelper(logger, "personType: ", personType, totalChars);
        Household.logHelper(logger, "workLoc: ", workLocation, totalChars);
        Household.logHelper(logger, "schoolLoc: ", schoolLoc, totalChars);
        Household.logHelper(logger, "workLocSegmentIndex: ", workLocSegmentIndex, totalChars);
        Household.logHelper(logger, "schoolLocSegmentIndex: ", schoolLocSegmentIndex, totalChars);
        Household.logHelper(logger, "freeParkingAvailable: ", freeParkingAvailable, totalChars);
        Household.logHelper(logger, "reimbursementPct: ",
                String.format("%.2f%%", (100 * reimbursePercent)), totalChars);
        Household.logHelper(logger, "cdapActivity: ", cdapActivity, totalChars);
        Household.logHelper(logger, "imtfChoice: ", imtfChoice, totalChars);
        Household.logHelper(logger, "inmtfChoice: ", inmtfChoice, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "windowBeforeFirstMandJointTour: ",
                windowBeforeFirstMandJointTour, totalChars);
        Household.logHelper(logger, "windowBetweenFirstLastMandJointTour: ",
                windowBetweenFirstLastMandJointTour, totalChars);
        Household.logHelper(logger, "windowAfterLastMandJointTour: ", windowAfterLastMandJointTour,
                totalChars);

        String header = "     Period:     |";
        String windowString = "     Window:     |";
        for (int i = 1; i < windows.length; i++)
        {
            header += String.format("%4s|", modelStructure.getTimePeriodLabel(i));
            windowString += String.format("%4s|", windows[i] == 0 ? "    " : "XXXX");
        }

        logger.info(header);
        logger.info(windowString);

        if (workTourArrayList.size() > 0)
        {
            for (Tour tour : workTourArrayList)
            {
                int id = tour.getTourId();
                logger.info(tour.getTourWindow(String.format("W%d", id)));
            }
        }
        if (schoolTourArrayList.size() > 0)
        {
            for (Tour tour : schoolTourArrayList)
            {
                logger.info(tour
                        .getTourWindow(tour.getTourPurpose().equalsIgnoreCase("university") ? "U"
                                : "S"));
            }
        }
        if (indNonManTourArrayList.size() > 0)
        {
            for (Tour tour : indNonManTourArrayList)
            {
                logger.info(tour.getTourWindow("N"));
            }
        }
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (Tour tour : atWorkSubtourArrayList)
            {
                logger.info(tour.getTourWindow("A"));
            }
        }
        if (hhObj.getJointTourArray() != null && hhObj.getJointTourArray().length > 0)
        {
            for (Tour tour : hhObj.getJointTourArray())
            {
                if (tour != null) logger.info(tour.getTourWindow("J"));
            }
        }

        logger.info(separater);

        logger.info("Work Tours:");
        if (workTourArrayList.size() > 0)
        {
            for (Tour tour : workTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No work tours");
        }

        logger.info("School Tours:");
        if (schoolTourArrayList.size() > 0)
        {
            for (Tour tour : schoolTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No school tours");
        }

        logger.info("Individual Non-mandatory Tours:");
        if (indNonManTourArrayList.size() > 0)
        {
            for (Tour tour : indNonManTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No individual non-mandatory tours");
        }

        logger.info("Work based subtours Tours:");
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (Tour tour : atWorkSubtourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No work based subtours");
        }

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public double getTimeFactorWork() {
		return timeFactorWork;
	}

	public void setTimeFactorWork(double timeFactorWork) {
		this.timeFactorWork = timeFactorWork;
	}

	public double getTimeFactorNonWork() {
		return timeFactorNonWork;
	}

	public void setTimeFactorNonWork(double timeFactorNonWork) {
		this.timeFactorNonWork = timeFactorNonWork;
	}

	public enum EmployStatus
    {
        nul, FULL_TIME, PART_TIME, NOT_EMPLOYED, UNDER16
    }

    public enum StudentStatus
    {
        nul, STUDENT_HIGH_SCHOOL_OR_LESS, STUDENT_COLLEGE_OR_HIGHER, NON_STUDENT
    }

    public enum PersonType
    {
        nul, FT_worker_age_16plus, PT_worker_nonstudent_age_16plus, University_student, Nonworker_nonstudent_age_16_64, Nonworker_nonstudent_age_65plus, Student_age_16_19_not_FT_wrkr_or_univ_stud, Student_age_6_15_schpred, Preschool_under_age_6
    }

}
