package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * This class is used for ...
 * 
 * @author Christi Willison
 * @version Nov 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public abstract class StopFrequencyDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger                            = Logger.getLogger(StopFrequencyDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    public static final int[]          NUM_OB_STOPS_FOR_ALT              = {-99999999, 0, 0, 0, 0,
            1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3                           };
    public static final int[]          NUM_IB_STOPS_FOR_ALT              = {-99999999, 0, 1, 2, 3,
            0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3                           };

    public String                      STOP_PURPOSE_FILE_WORK_NAME       = "Work";
    public String                      STOP_PURPOSE_FILE_UNIVERSITY_NAME = "University";
    public String                      STOP_PURPOSE_FILE_SCHOOL_NAME     = "School";
    public String                      STOP_PURPOSE_FILE_ESCORT_NAME     = "Escort";
    public String                      STOP_PURPOSE_FILE_SHOPPING_NAME   = "Shopping";
    public String                      STOP_PURPOSE_FILE_MAINT_NAME      = "Maint";
    public String                      STOP_PURPOSE_FILE_EAT_OUT_NAME    = "EatOut";
    public String                      STOP_PURPOSE_FILE_VISIT_NAME      = "Visit";
    public String                      STOP_PURPOSE_FILE_DISCR_NAME      = "Discr";
    public String                      STOP_PURPOSE_FILE_WORK_BASED_NAME = "WorkBased";

    protected IndexValues              dmuIndex;
    protected Household                household;
    protected Person                   person;
    protected Tour                     tour;

    protected ModelStructure           modelStructure;

    private double                     shoppingAccessibility;
    private double                     maintenanceAccessibility;
    private double                     discretionaryAccessibility;

    public StopFrequencyDMU(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
        dmuIndex = new IndexValues();
    }

    public abstract HashMap<Integer, Integer> getTourPurposeChoiceModelIndexMap();

    public abstract int[] getModelSheetValuesArray();

    public void setDmuIndexValues(int hhid, int homeTaz, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(hhid);
        dmuIndex.setZoneIndex(homeTaz);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (household.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug SF UEC");
        }

    }

    public void setHouseholdObject(Household household)
    {
        this.household = household;
    }

    public void setPersonObject(Person person)
    {
        this.person = person;
    }

    public void setTourObject(Tour tour)
    {
        this.tour = tour;
    }

    public int getTourIsJoint()
    {
        return tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ? 1
                : 0;
    }

    public void setShoppingAccessibility(double shoppingAccessibility)
    {
        this.shoppingAccessibility = shoppingAccessibility;
    }

    public void setMaintenanceAccessibility(double maintenanceAccessibility)
    {
        this.maintenanceAccessibility = maintenanceAccessibility;
    }

    public void setDiscretionaryAccessibility(double discretionaryAccessibility)
    {
        this.discretionaryAccessibility = discretionaryAccessibility;
    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /**
     * @return the household income in dollars
     */
    public int getIncomeInDollars()
    {
        return household.getIncomeInDollars();
    }

    /**
     * @return the number of full time workers in the household
     */
    public int getNumFtWorkers()
    {
        return household.getNumFtWorkers();
    }

    /**
     * @return the number of part time workers in the household
     */
    public int getNumPtWorkers()
    {
        return household.getNumPtWorkers();
    }

    /**
     * @return the person type index for the person making the tour
     */
    public int getPersonType()
    {
        return person.getPersonTypeNumber();
    }

    /**
     * @return the number of persons in the household
     */
    public int getHhSize()
    {
        return household.getHhSize();
    }

    /**
     * @return the number of driving age students in the household
     */
    public int getNumHhDrivingStudents()
    {
        return household.getNumDrivingStudents();
    }

    /**
     * @return the number of non-driving age students in the household
     */
    public int getNumHhNonDrivingStudents()
    {
        return household.getNumNonDrivingStudents();
    }

    /**
     * @return the number of preschool age students in the household
     */
    public int getNumHhPreschool()
    {
        return household.getNumPreschool();
    }

    /**
     * @return the number of work tours made by this person
     */
    public int getWorkTours()
    {
        return person.getNumWorkTours();
    }

    /**
     * 
     * @return 1 if the outbound portion of the tour is escort, in which case stops are already determined, else 0
     */
    public int getOutboundIsEscort(){
    	
    	return ((tour.getEscortTypeOutbound() == ModelStructure.RIDE_SHARING_TYPE) ||(tour.getEscortTypeOutbound() == ModelStructure.PURE_ESCORTING_TYPE))
    			? 1 : 0;
    }
    
    /**
     * 
     * @return 1 if the inbound portion of the tour is escort, in which case stops are already determined, else 0
     */
   public int getInboundIsEscort(){
    	
    	return ((tour.getEscortTypeInbound() == ModelStructure.RIDE_SHARING_TYPE) ||(tour.getEscortTypeInbound() == ModelStructure.PURE_ESCORTING_TYPE))
    			? 1 : 0;
    }

   /**
     * @return the total number of tours made by this person
     */
    public int getTotalTours()
    {
        return person.getNumTotalIndivTours();
    }

    /**
     * @return the total number of tours made by this person
     */
    public int getTotalHouseholdTours()
    {
        return household.getNumTotalIndivTours();
    }

    /**
     * @return the distance from the home mgra to the work mgra for this person
     */
    public double getWorkLocationDistance()
    {
        return person.getWorkLocationDistance();
    }

    /**
     * @return the distance from the home mgra to the school mgra for this
     *         person
     */
    public double getSchoolLocationDistance()
    {
        return person.getSchoolLocationDistance();
    }

    /**
     * @return the age of this person
     */
    public int getAge()
    {
        return person.getAge();
    }

    /**
     * @return the number of school tours made by this person
     */
    public int getSchoolTours()
    {
        return person.getNumSchoolTours();
    }

    /**
     * @return the number of escort tours made by this person
     */
    public int getEscortTours()
    {
        return person.getNumIndividualEscortTours();
    }

    /**
     * @return the number of shopping tours made by this person
     */
    public int getShoppingTours()
    {
        return person.getNumIndividualShoppingTours();
    }

    /**
     * @return the number of maintenance tours made by this person
     */
    public int getMaintenanceTours()
    {
        return person.getNumIndividualOthMaintTours();
    }

    /**
     * @return the number of eating out tours made by this person
     */
    public int getEatTours()
    {
        return person.getNumIndividualEatOutTours();
    }

    /**
     * @return the number of visit tours made by this person
     */
    public int getVisitTours()
    {
        return person.getNumIndividualSocialTours();
    }

    /**
     * @return the number of discretionary tours made by this person
     */
    public int getDiscretionaryTours()
    {
        return person.getNumIndividualOthDiscrTours();
    }

    /**
     * @return the shopping accessibility for the household (alts 28-30)
     */
    public double getShoppingAccessibility()
    {
        return shoppingAccessibility;
    }

    /**
     * @return the maintenance accessibility for the household (alts 31-33)
     */
    public double getMaintenanceAccessibility()
    {
        return maintenanceAccessibility;
    }

    /**
     * @return the discretionary accessibility for the household (alts 40-42)
     */
    public double getDiscretionaryAccessibility()
    {
        return discretionaryAccessibility;
    }

    /**
     * @return the number of inbound stops that correspond to the chosen stop
     *         frequency alternative
     */
    public int getNumIbStopsAlt(int alt)
    {
        return NUM_IB_STOPS_FOR_ALT[alt];
    }

    /**
     * @return the number of outbound stops that correspond to the chosen stop
     *         frequency alternative
     */
    public int getNumObStopsAlt(int alt)
    {
        return NUM_OB_STOPS_FOR_ALT[alt];
    }

    /**
     * get the tour duration, measured in hours
     * 
     * @return duration of tour in hours - number of half-hour intervals -
     *         arrive period - depart period divided by 2.
     */
    public float getTourDurationInHours()
    {
        return (tour.getTourArrivePeriod() - tour.getTourDepartPeriod()) / 2;
    }

    public int getTourModeIsAuto()
    {
        return modelStructure.getTourModeIsSovOrHov(tour.getTourModeChoice()) ? 1 : 0;
    }

    public int getTourModeIsTransit()
    {
        return modelStructure.getTourModeIsTransit(tour.getTourModeChoice()) ? 1 : 0;
    }

    public int getTourModeIsNonMotorized()
    {
        return modelStructure.getTourModeIsNonMotorized(tour.getTourModeChoice()) ? 1 : 0;
    }

    public int getTourModeIsSchoolBus()
    {
        return modelStructure.getTourModeIsSchoolBus(tour.getTourModeChoice()) ? 1 : 0;
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
