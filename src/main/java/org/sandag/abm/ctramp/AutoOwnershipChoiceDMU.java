package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class AutoOwnershipChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger                           = Logger.getLogger(AutoOwnershipChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private Household                  hh;
    private IndexValues                dmuIndex;

    private boolean                    useAccessibility                 = false;

    private double                     workAutoDependency               = 0.0;
    private double                     schoolAutoDependency             = 0.0;

    private double                     workersRailProportion            = 0.0;
    private double                     studentsRailProportion           = 0.0;

    private double                     homeTazAutoAccessibility         = 0.0;
    private double                     homeTazTransitAccessibility      = 0.0;
    private double                     homeTazNonMotorizedAccessibility = 0.0;

    public AutoOwnershipChoiceDMU()
    {
        dmuIndex = new IndexValues();
    }

    public void setHouseholdObject(Household hhObject)
    {
        hh = hhObject;
    }

    // DMU methods - define one of these for every @var in the mode choice
    // control
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
            dmuIndex.setDebugLabel("Debug AO UEC");
        }

    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public Household getHouseholdObject()
    {
        return hh;
    }

    public int getGq()
    {
        return hh.getIsGroupQuarters();
    }

    public int getDrivers()
    {
        return hh.getDrivers();
    }

    public int getNumFtWorkers()
    {
        return hh.getNumFtWorkers();
    }

    public int getNumPtWorkers()
    {
        return hh.getNumPtWorkers();
    }

    public int getNumPersons18to24()
    {
        return hh.getNumPersons18to24();
    }

    public int getNumPersons6to15()
    {
        return hh.getNumPersons6to15();
    }

    public int getNumPersons80plus()
    {
        return hh.getNumPersons80plus();
    }

    public int getNumPersons65to79()
    {
        return hh.getNumPersons65to79();
    }

    public int getHhIncomeInDollars()
    {
        return hh.getIncomeInDollars();
    }

    public int getNumHighSchoolGraduates()
    {
        return hh.getNumHighSchoolGraduates();
    }

    public int getDetachedDwellingType()
    {
        return hh.getHhBldgsz();
    }

    public double getUseAccessibilities()
    {
        return useAccessibility ? 1 : 0;
    }

    public double getHomeTazAutoAccessibility()
    {
        return homeTazAutoAccessibility;
    }

    public double getHomeTazTransitAccessibility()
    {
        return homeTazTransitAccessibility;
    }

    public double getHomeTazNonMotorizedAccessibility()
    {
        return homeTazNonMotorizedAccessibility;
    }

    public double getWorkAutoDependency()
    {
        return workAutoDependency;
    }

    public double getSchoolAutoDependency()
    {
        return schoolAutoDependency;
    }

    public double getWorkersRailProportion()
    {
        return workersRailProportion;
    }

    public double getStudentsRailProportion()
    {
        return studentsRailProportion;
    }

    public void setUseAccessibilities(boolean flag)
    {
        useAccessibility = flag;
    }

    public void setHomeTazAutoAccessibility(double acc)
    {
        homeTazAutoAccessibility = acc;
    }

    public void setHomeTazTransitAccessibility(double acc)
    {
        homeTazTransitAccessibility = acc;
    }

    public void setHomeTazNonMotorizedAccessibility(double acc)
    {
        homeTazNonMotorizedAccessibility = acc;
    }

    public void setWorkAutoDependency(double value)
    {
        workAutoDependency = value;
    }

    public void setSchoolAutoDependency(double value)
    {
        schoolAutoDependency = value;
    }

    public void setWorkersRailProportion(double proportion)
    {
        workersRailProportion = proportion;
    }

    public void setStudentsRailProportion(double proportion)
    {
        studentsRailProportion = proportion;
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
