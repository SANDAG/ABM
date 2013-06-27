package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TourDepartureTimeAndDurationDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger                               = Logger
                                                                                    .getLogger(TourDepartureTimeAndDurationDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected IndexValues              dmuIndex;

    protected Person                   person;
    protected Household                household;
    protected Tour                     tour;

    protected double destEmpDen;
    protected int subsequentTourIsWork;
    protected int subsequentTourIsSchool;
    
    protected double[]                 modeChoiceLogsums;

    private int[]                      altStarts;
    private int[]                      altEnds;

    protected int                      originAreaType, destinationAreaType;

    protected int                      tourNumber;

    protected int                      firstTour;
    protected int                      subsequentTour;
    protected int                      endOfPreviousScheduledTour;

    protected ModelStructure           modelStructure;

    public TourDepartureTimeAndDurationDMU(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
        dmuIndex = new IndexValues();
    }

    public void setPerson(Person passedInPerson)
    {
        person = passedInPerson;
    }

    public void setHousehold(Household passedInHousehold)
    {
        household = passedInHousehold;

        // set the origin and zone indices
        dmuIndex.setZoneIndex(household.getHhMgra());
        dmuIndex.setHHIndex(household.getHhId());

        // set the debug flag that can be used in the UEC
        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (household.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug DepartTime UEC");
        }

    }

    public void setTour(Tour passedInTour)
    {
        tour = passedInTour;
    }

    public void setOriginZone(int zone)
    {
        dmuIndex.setOriginZone(zone);
    }

    public void setDestinationZone(int zone)
    {
        dmuIndex.setDestZone(zone);
    }

    public void setOriginAreaType(int areaType)
    {
        originAreaType = areaType;
    }

    public void setDestinationAreaType(int areaType)
    {
        destinationAreaType = areaType;
    }

    public void setDestEmpDen(double arg)
    {
        destEmpDen = arg;
    }

    public void setFirstTour(int trueOrFalse)
    {
        firstTour = trueOrFalse;
    }

    public void setSubsequentTour(int trueOrFalse)
    {
        subsequentTour = trueOrFalse;
    }

    public void setSubsequentTourIsWork(int trueOrFalse)
    {
        subsequentTourIsWork = trueOrFalse;
    }

    public void setSubsequentTourIsSchool(int trueOrFalse)
    {
        subsequentTourIsSchool = trueOrFalse;
    }


    /**
     * Set the sequence number of this tour among all scheduled
     * 
     * @param tourNum
     */
    public void setTourNumber(int tourNum)
    {
        tourNumber = tourNum;
    }

    public void setEndOfPreviousScheduledTour(int endHr)
    {
        endOfPreviousScheduledTour = endHr;
    }

    public void setModeChoiceLogsums(double[] logsums)
    {
        modeChoiceLogsums = logsums;
    }

    public void setTodAlts(int[] altStarts, int[] altEnds)
    {
        this.altStarts = altStarts;
        this.altEnds = altEnds;
    }

    public IndexValues getIndexValues()
    {
        return (dmuIndex);
    }

    public Household getDmuHouseholdObject()
    {
        return household;
    }

    public int getOriginZone()
    {
        return (dmuIndex.getOriginZone());
    }

    public int getDestinationZone()
    {
        return (dmuIndex.getDestZone());
    }

    public int getOriginAreaType()
    {
        return (originAreaType);
    }

    public int getDestinationAreaType()
    {
        return (destinationAreaType);
    }

    public int getPreDrivingAgeChild()
    {
        return (person.getPersonIsStudentNonDriving() == 1 || person.getPersonIsPreschoolChild() == 1 ) ? 1 : 0;
    }
    
    public int getPersonAge()
    {
        return person.getAge();
    }

    public int getPersonIsFemale()
    {
        return person.getGender() == 2 ? 1 : 0;
    }

    public int getHouseholdSize()
    {
        return household.getHhSize();
    }

    public int getNumPreschoolChildrenInHh()
    {
        return household.getNumPreschool();
    }

    public int getNumChildrenUnder16InHh()
    {
        return household.getNumChildrenUnder16();
    }

    public int getNumNonWorkingAdultsInHh()
    {
        return household.getNumberOfNonWorkingAdults();
    }

    public int getFullTimeWorker()
    {
        return (this.person.getPersonTypeIsFullTimeWorker());
    }

    public int getPartTimeWorker()
    {
        return (this.person.getPersonTypeIsPartTimeWorker());
    }

    public int getUniversityStudent()
    {
        return (this.person.getPersonIsUniversityStudent());
    }

    public int getStudentDrivingAge()
    {
        return (this.person.getPersonIsStudentDriving());
    }

    public int getStudentNonDrivingAge()
    {
        return (this.person.getPersonIsStudentNonDriving());
    }

    public int getNonWorker()
    {
        return (this.person.getPersonIsNonWorkingAdultUnder65());
    }

    public int getRetired()
    {
        return (this.person.getPersonIsNonWorkingAdultOver65());
    }

    public int getAllAdultsFullTimeWorkers()
    {
        Person[] p = household.getPersons();
        boolean allAdultsAreFullTimeWorkers = true;
        for (int i = 1; i < p.length; i++)
        {
            if (p[i].getPersonIsAdult() == 1 && p[i].getPersonIsFullTimeWorker() == 0)
            {
                allAdultsAreFullTimeWorkers = false;
                break;
            }
        }

        if (allAdultsAreFullTimeWorkers) return 1;
        else return 0;
    }

    public int getSubtourPurposeIsEatOut()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(modelStructure.AT_WORK_EAT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getSubtourPurposeIsBusiness()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getSubtourPurposeIsOther()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(modelStructure.AT_WORK_MAINT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPurposeIsShopping()
    {
        if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.SHOPPING_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPurposeIsEatOut()
    {
        if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.EAT_OUT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPurposeIsMaint()
    {
        if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.OTH_MAINT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPurposeIsVisit()
    {
        if (tour.getTourPurpose().equalsIgnoreCase(modelStructure.SOCIAL_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPurposeIsDiscr()
    {
        if (tour.getTourPurpose().equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getNumIndivShopTours()
    {
        int count = 0;
        for ( Tour t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPurpose().equalsIgnoreCase(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivMaintTours()
    {
        int count = 0;
        for ( Tour t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPurpose().equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivVisitTours()
    {
        int count = 0;
        for ( Tour t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPurpose().equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivDiscrTours()
    {
        int count = 0;
        for ( Tour t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPurpose().equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    /*
     * if ( tour.getTourCategory() == ModelStructure.AT_WORK_CATEGORY ) { return
     * tour.getTourPurposeIndex(); } else { return 0; } }
     */

    public int getAdultsInTour()
    {

        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            Person[] persons = household.getPersons();

            int[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if (persons[p].getPersonIsAdult() == 1) count++;
            }
        } else if (tour.getTourCategory().equalsIgnoreCase(
                ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY))
        {
            if (person.getPersonIsAdult() == 1) count = 1;
        }

        return count;
    }

    public int getJointTourPartySize()
    {
        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
            count = tour.getPersonNumArray().length;
        
        return count;
    }
    
    public int getKidsOnJointTour()
    {

        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            Person[] persons = household.getPersons();

            int[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if ((persons[p].getPersonIsPreschoolChild() + persons[p].getPersonIsStudentNonDriving() + persons[p].getPersonIsStudentDriving()) > 0 ) count++;
            }
        }

        return count > 0 ? 1 : 0;

    }

    // return 1 if at least one preschool or pre-driving child is in joint tour,
    // otherwise 0.
    public int getPreschoolPredrivingInTour()
    {

        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            Person[] persons = household.getPersons();
            int[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if (persons[p].getPersonIsPreschoolChild() == 1
                        || persons[p].getPersonIsStudentNonDriving() == 1) return 1;
            }
        } else if (tour.getTourCategory().equalsIgnoreCase(
                ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY))
        {
            if (person.getPersonIsPreschoolChild() == 1
                    || person.getPersonIsStudentNonDriving() == 1) count = 1;
        }

        return count;

    }

    // return 1 if the person is preschool
    public int getPreschool()
    {
        return person.getPersonIsPreschoolChild() == 1 ? 1 : 0;

    }

    // return 1 if at least one university student is in joint tour, otherwise 0.
    public int getUnivInTour()
    {

        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            Person[] persons = household.getPersons();
            int[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if (persons[p].getPersonIsUniversityStudent() == 1) return 1;
            }
        } else if (tour.getTourCategory().equalsIgnoreCase(
                ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY))
        {
            if (person.getPersonIsUniversityStudent() == 1) count = 1;
        }

        return count;

    }

    // return 1 if all adults in joint tour are fulltime workers, 0 otherwise;
    public int getAllWorkFull()
    {

        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            int adultCount = 0;
            int ftWorkerAdultCount = 0;

            Person[] persons = household.getPersons();
            int[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if (persons[p].getPersonIsAdult() == 1)
                {
                    adultCount++;
                    if (persons[p].getPersonIsFullTimeWorker() == 1) ftWorkerAdultCount++;
                }
            }

            if (adultCount > 0 && adultCount == ftWorkerAdultCount) return 1;
            else return 0;
        }

        return 0;

    }

    public int getPartyComp()
    {
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            return tour.getJointTourComposition();
        } else
        {
            return 0;
        }
    }

    /**
     * @return number of individual non-mandatory tours, including escort, for the
     *         person
     */
    public int getPersonNonMandatoryTotalWithEscort()
    {
        return person.getListOfIndividualNonMandatoryTours().size();
    }

    /**
     * @return number of individual non-mandatory tours, excluding escort, for the
     *         person
     */
    public int getPersonNonMandatoryTotalNoEscort()
    {
        int count = 0;
        for (Tour t : person.getListOfIndividualNonMandatoryTours())
            if (!t.getTourPurpose().startsWith("escort")) count++;
        return count;
    }

    /**
     * @return number of individual non-mandatory discretionary tours for the person
     */
    public int getPersonDiscrToursTotal()
    {
        int count = 0;
        for (Tour t : person.getListOfIndividualNonMandatoryTours())
        {
            if (t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) ||
                t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) ||
                t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) )
                    count++;
        }
        return count;
    }

    /**
     * @return number of individual non-mandatory tours, excluding escort, for the person
     */
    public int getPersonEscortTotal()
    {
        int count = 0;
        for (Tour t : person.getListOfIndividualNonMandatoryTours())
            if (t.getTourPurpose().startsWith("escort")) count++;
        return count;
    }

    public int getHhJointTotal()
    {
        Tour[] jt = household.getJointTourArray();
        if (jt == null) return 0;
        else return jt.length;
    }

    public int getPersonMandatoryTotal()
    {
        return person.getListOfWorkTours().size() + person.getListOfSchoolTours().size();
    }

    public int getPersonJointTotal()
    {
        Tour[] jtArray = household.getJointTourArray();
        if (jtArray == null)
        {
            return 0;
        } else
        {
            int numJtParticipations = 0;
            for (Tour jt : jtArray)
            {
                int[] personJtIndices = jt.getPersonNumArray();
                for (int pNum : personJtIndices)
                {
                    if (pNum == person.getPersonNum())
                    {
                        numJtParticipations++;
                        break;
                    }
                }
            }
            return numJtParticipations;
        }
    }

    public int getPersonJointAndIndivDiscrToursTotal()
    {

        int totDiscr = getPersonDiscrToursTotal(); 
        
        Tour[] jtArray = household.getJointTourArray();
        if (jtArray == null)
        {
            return totDiscr;
        } else
        {
            // count number of joint discretionary tours person participates in
            int numJtParticipations = 0;
            for (Tour jt : jtArray)
            {
                if (jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) ||
                        jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) ||
                        jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) )
                {
                    int[] personJtIndices = jt.getPersonNumArray();
                    for (int pNum : personJtIndices)
                    {
                        if (pNum == person.getPersonNum())
                        {
                            numJtParticipations++;
                            break;
                        }
                    }
                }
            }
            return numJtParticipations + totDiscr;
        }
    }

    public int getFirstTour()
    {
        return firstTour;
    }

    public int getSubsequentTour()
    {
        return subsequentTour;
    }

    public int getWorkAndSchoolToursByWorker()
    {
        int returnValue = 0;
        if (person.getPersonIsWorker() == 1)
        {
            if (person.getImtfChoice() == HouseholdIndividualMandatoryTourFrequencyModel.CHOICE_WORK_AND_SCHOOL)
                returnValue = 1;
        }
        return returnValue;
    }

    public int getWorkAndSchoolToursByStudent()
    {
        int returnValue = 0;
        if (person.getPersonIsStudent() == 1)
        {
            if (person.getImtfChoice() == HouseholdIndividualMandatoryTourFrequencyModel.CHOICE_WORK_AND_SCHOOL)
                returnValue = 1;
        }
        return returnValue;
    }

    public double getModeChoiceLogsumAlt(int alt)
    {

        int startPeriod = altStarts[alt - 1];
        int endPeriod = altEnds[alt - 1];

        int index = modelStructure.getSkimPeriodCombinationIndex(startPeriod, endPeriod);

        return modeChoiceLogsums[index];

    }

    public int getPrevTourEndsThisDeparturePeriodAlt(int alt)
    {

        // get the departure period for the current alternative
        int thisTourStartsPeriod = altStarts[alt - 1];

        if (person.isPreviousArrival(thisTourStartsPeriod)) return 1;
        else return 0;

    }

    public int getPrevTourBeginsThisArrivalPeriodAlt(int alt)
    {

        // get the arrival period for the current alternative
        int thisTourEndsPeriod = altStarts[alt - 1];

        if (person.isPreviousDeparture(thisTourEndsPeriod)) return 1;
        else return 0;

    }

    public int getAdjWindowBeforeThisPeriodAlt(int alt)
    {

        int thisTourStartsPeriod = altStarts[alt - 1];

        int numAdjacentPeriodsAvailable = 0;
        for (int i = thisTourStartsPeriod - 1; i >= 0; i--)
        {
            if (person.isPeriodAvailable(i)) numAdjacentPeriodsAvailable++;
            else break;
        }

        return numAdjacentPeriodsAvailable;

    }

    public int getAdjWindowAfterThisPeriodAlt(int alt)
    {

        int thisTourEndsPeriod = altEnds[alt - 1];

        int numAdjacentPeriodsAvailable = 0;
        for (int i = thisTourEndsPeriod + 1; i < modelStructure.getNumberOfTimePeriods(); i++)
        {
            if (person.isPeriodAvailable(i)) numAdjacentPeriodsAvailable++;
            else break;
        }

        return numAdjacentPeriodsAvailable;

    }

    public int getRemainingPeriodsAvailableAlt(int alt)
    {

        int periodsAvail = person.getAvailableWindow();

        int start = altStarts[alt - 1];
        int end = altEnds[alt - 1];

        // determine the availabilty of each period after the alternative time window
        // is hypothetically scheduled
        // if start == end, the availability won't change, so no need to compute.
        if (start != end)
        {

            // the start and end periods will always be available after scheduling, so
            // don't need to check them.
            // the periods between start/end must be 0 or the alternative could not
            // have been available,
            // so count them all as unavailable after scheduling this window.
            periodsAvail -= (end - start - 1);

        }

        return periodsAvail;

    }

    public float getRemainingInmToursToAvailablePeriodsRatioAlt(int alt)
    {
        int periodsAvail = getRemainingPeriodsAvailableAlt(alt);
        if (periodsAvail > 0)
        {
            float ratio = (float) (person.getListOfIndividualNonMandatoryTours().size() - tourNumber)
                    / periodsAvail;
            return ratio;
        } else return -999;
    }

    public int getMaximumAvailableTimeWindow()
    {
        return person.getMaximumContinuousAvailableWindow();
    }

    public int getMaxJointTimeWindow()
    {
        return household.getMaxJointTimeWindow( tour );
    }


    /**
     * get the number of tours left to be scheduled, including the current tour
     * @return number of tours left to be scheduled, including the current tour
     */
    public int getToursLeftToSchedule()
    {
        if ( tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ){
            Tour[] jt = household.getJointTourArray();
            return jt.length - tourNumber + 1;
        }
        else
            return person.getListOfIndividualNonMandatoryTours().size() - tourNumber + 1;
    }
    
    public int getEndOfPreviousTour()
    {
        return endOfPreviousScheduledTour;
    }

    public int getTourCategoryIsJoint()
    {
        return tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ? 1 : 0;
    }

    public float getOpSovTimeOd()
    {
        return 1;
    }

    public float getOpSovTimeDo()
    {
        return 1;
    }

    public int getDestMgraInCbd()
    {
        return 0;
    }

    public int getOrigMgraInRural()
    {
        return 0;
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
