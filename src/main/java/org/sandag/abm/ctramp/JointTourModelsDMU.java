package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class JointTourModelsDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TourModeChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected Household                hh;
    protected Tour                     tour;
    protected IndexValues              dmuIndex;

    private float                      shopHOVAccessibility;
    private float                      maintHOVAccessibility;
    private float                      discrHOVAccessibility;

    public JointTourModelsDMU()
    {
        dmuIndex = new IndexValues();
    }

    public Household getHouseholdObject()
    {
        return hh;
    }

    public void setHouseholdObject(Household hhObject)
    {
        hh = hhObject;
    }

    public void setTourObject(Tour tourObject)
    {
        tour = tourObject;
    }

    // DMU methods - define one of these for every @var in the mode choice
    // control
    // file.

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public int getActiveCountFullTimeWorkers()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsFullTimeWorker() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountPartTimeWorkers()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsPartTimeWorker() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountUnivStudents()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsUniversityStudent() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountNonWorkers()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsNonWorkingAdultUnder65() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountRetirees()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsNonWorkingAdultOver65() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountDrivingAgeSchoolChildren()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsStudentDriving() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountPreDrivingAgeSchoolChildren()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsStudentNonDriving() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getActiveCountPreSchoolChildren()
    {
        int count = 0;
        for (Person p : hh.getPersons())
            if (p != null && p.getPersonIsPreschoolChild() == 1)
                if (!p.getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN)) count++;
        return count;
    }

    public int getMaxPairwiseAdultOverlapsHh()
    {
        return hh.getMaxAdultOverlaps();
    }

    public int getMaxPairwiseChildOverlapsHh()
    {
        return hh.getMaxChildOverlaps();
    }

    public int getMaxPairwiseMixedOverlapsHh()
    {
        return hh.getMaxMixedOverlaps();
    }

    public int getMaxPairwiseOverlapOtherAdults()
    {
        return tour.getPersonObject().getMaxAdultOverlaps();
    }

    public int getMaxPairwiseOverlapOtherChildren()
    {
        return tour.getPersonObject().getMaxChildOverlaps();
    }

    public int getTravelActiveAdults()
    {
        return hh.getTravelActiveAdults();
    }

    public int getTravelActiveChildren()
    {
        return hh.getTravelActiveChildren();
    }

    public int getPersonStaysHome()
    {
        Person p = tour.getPersonObject();
        return p.getCdapActivity().equalsIgnoreCase("H") ? 1 : 0;
    }

    public int getIncomeLessThan30K()
    {
        return hh.getIncomeInDollars() < 30000 ? 1 : 0;
    }

    public int getIncome30Kto60K()
    {
        int income = hh.getIncomeInDollars();
        return (income >= 30000 && income < 60000) ? 1 : 0;
    }

    public int getIncomeMoreThan100K()
    {
        return hh.getIncomeInDollars() >= 100000 ? 1 : 0;
    }

    public int getNumAdults()
    {
        int num = 0;
        Person[] persons = hh.getPersons();
        for (int i = 1; i < persons.length; i++)
            num += (persons[i].getPersonIsAdult() == 1 ? 1 : 0);
        return num;
    }

    public int getNumChildren()
    {
        int num = 0;
        Person[] persons = hh.getPersons();
        for (int i = 1; i < persons.length; i++)
            num += (persons[i].getPersonIsAdult() == 0 ? 1 : 0);
        return num;
    }

    public int getHhWorkers()
    {
        return hh.getWorkers();
    }

    public int getAutoOwnership()
    {
        return hh.getAutoOwnershipModelResult();
    }

    public int getTourPurposeIsMaint()
    {
        return tour.getTourPurpose()
                .equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME) ? 1 : 0;
    }

    public int getTourPurposeIsEat()
    {
        return tour.getTourPurpose().equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) ? 1
                : 0;
    }

    public int getTourPurposeIsVisit()
    {
        return tour.getTourPurpose().equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) ? 1
                : 0;
    }

    public int getTourPurposeIsDiscr()
    {
        return tour.getTourPurpose()
                .equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) ? 1 : 0;
    }

    public int getPersonType()
    {
        return tour.getPersonObject().getPersonTypeNumber();
    }

    public int getJointTourComposition()
    {
        return tour.getJointTourComposition();
    }

    public int getJTours()
    {
        return hh.getJointTourArray().length;
    }

    public void setShopHOVAccessibility(float accessibility)
    {
        shopHOVAccessibility = accessibility;
    }

    public float getShopHOVAccessibility()
    {
        return shopHOVAccessibility;
    }

    public void setMaintHOVAccessibility(float accessibility)
    {
        maintHOVAccessibility = accessibility;
    }

    public float getMaintHOVAccessibility()
    {
        return maintHOVAccessibility;
    }

    public void setDiscrHOVAccessibility(float accessibility)
    {
        discrHOVAccessibility = accessibility;
    }

    public float getDiscrHOVAccessibility()
    {
        return discrHOVAccessibility;
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
