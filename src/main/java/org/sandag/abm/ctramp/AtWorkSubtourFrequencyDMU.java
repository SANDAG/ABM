package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class AtWorkSubtourFrequencyDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(AtWorkSubtourFrequencyDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected Household                hh;
    protected Person                   person;
    protected Tour                     tour;
    protected IndexValues              dmuIndex;

    protected double                   nmEatOutAccessibillity;

    protected ModelStructure           modelStructure;

    public AtWorkSubtourFrequencyDMU(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
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

    public void setPersonObject(Person persObject)
    {
        person = persObject;
    }

    public void setTourObject(Tour tourObject)
    {
        tour = tourObject;
    }

    // DMU methods - define one of these for every @var in the mode choice control
    // file.

    public void setDmuIndexValues(int hhId, int zoneId, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(hhId);
        dmuIndex.setZoneIndex(zoneId);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (hh.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug INMTF UEC");
        }

    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /**
     * @return household income category
     */
    public int getIncomeInDollars()
    {
        return hh.getIncomeInDollars();
    }

    /**
     * @return person type category index
     */
    public int getPersonType()
    {
        return person.getPersonTypeNumber();
    }

    /**
     * @return person type category index
     */
    public int getFemale()
    {
        if (person.getPersonIsFemale() == 1) return 1;
        else return 0;
    }

    /**
     * @return number of driving age people in household
     */
    public int getDrivers()
    {
        return hh.getDrivers();
    }

    /**
     * @return number of people of preschool person type in household
     */
    public int getNumPreschoolChildren()
    {
        return hh.getNumPreschool();
    }

    /**
     * @return number of individual non-mandatory eat-out tours for the person.
     */
    public int getNumIndivEatOutTours()
    {
        int numTours = 0;
        ArrayList<Tour> tourList = person.getListOfIndividualNonMandatoryTours();
        if (tourList != null)
        {
            for (Tour t : tourList)
            {
                String tourPurpose = t.getTourPurpose();
                if (tourPurpose.equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME))
                {
                    numTours++;
                }
            }
        }
        return numTours;
    }

    /**
     * @return total mandatory and non-mandatory tours for the person.
     */
    public int getNumTotalTours()
    {
        int numTours = 0;

        ArrayList<Tour> wTourList = person.getListOfWorkTours();
        if (wTourList != null) numTours += wTourList.size();

        ArrayList<Tour> sTourList = person.getListOfSchoolTours();
        if (sTourList != null) numTours += sTourList.size();

        ArrayList<Tour> nmTourList = person.getListOfIndividualNonMandatoryTours();
        if (nmTourList != null) numTours += nmTourList.size();

        return numTours;
    }

    public double getNmEatOutAccessibilityWorkplace()
    {
        return nmEatOutAccessibillity;
    }

    /**
     * set the value of the non-mandatory eat out accessibility for this decision maker
     */
    public void setNmEatOutAccessibilityWorkplace(double nmEatOutAccessibillity)
    {
        this.nmEatOutAccessibillity = nmEatOutAccessibillity;
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
