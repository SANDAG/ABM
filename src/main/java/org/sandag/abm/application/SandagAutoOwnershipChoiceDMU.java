package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.AutoOwnershipChoiceDMU;

public class SandagAutoOwnershipChoiceDMU
        extends AutoOwnershipChoiceDMU
{

    public SandagAutoOwnershipChoiceDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getDrivers", 1);
        methodIndexMap.put("getNumFtWorkers", 2);
        methodIndexMap.put("getNumPtWorkers", 3);
        methodIndexMap.put("getNumPersons18to24", 4);
        methodIndexMap.put("getNumPersons6to15", 5);
        methodIndexMap.put("getNumPersons80plus", 6);
        methodIndexMap.put("getNumPersons65to79", 7);
        methodIndexMap.put("getHhIncomeInDollars", 8);
        methodIndexMap.put("getNumHighSchoolGraduates", 9);
        methodIndexMap.put("getDetachedDwellingType", 10);
        methodIndexMap.put("getUseAccessibilities", 11);
        methodIndexMap.put("getHomeTazNonMotorizedAccessibility", 12);
        methodIndexMap.put("getHomeTazAutoAccessibility", 13);
        methodIndexMap.put("getHomeTazTransitAccessibility", 14);
        methodIndexMap.put("getWorkAutoDependency", 15);
        methodIndexMap.put("getSchoolAutoDependency", 16);
        methodIndexMap.put("getWorkersRailProportion", 17);
        methodIndexMap.put("getStudentsRailProportion", 18);
        methodIndexMap.put("getGq", 19);
        methodIndexMap.put("getNumPersons18to35", 25);
        methodIndexMap.put("getNumPersons65plus", 26);
        methodIndexMap.put("getWorkAutoTime", 27);
        methodIndexMap.put("getHomeTazMaasAccessibility", 28);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 1:
                return getDrivers();
            case 2:
                return getNumFtWorkers();
            case 3:
                return getNumPtWorkers();
            case 4:
                return getNumPersons18to24();
            case 5:
                return getNumPersons6to15();
            case 6:
                return getNumPersons80plus();
            case 7:
                return getNumPersons65to79();
            case 8:
                return getHhIncomeInDollars();
            case 9:
                return getNumHighSchoolGraduates();
            case 10:
                return getDetachedDwellingType();
            case 11:
                return getUseAccessibilities();
            case 12:
                return getHomeTazNonMotorizedAccessibility();
            case 13:
                return getHomeTazAutoAccessibility();
            case 14:
                return getHomeTazTransitAccessibility();
            case 15:
                return getWorkAutoDependency();
            case 16:
                return getSchoolAutoDependency();
            case 17:
                return getWorkersRailProportion();
            case 18:
                return getStudentsRailProportion();
            case 19:
                return getGq();
            case 25:
            	return getNumPersons18to35();
            case 26:
            	return getNumPersons65Plus();
            case 27:
            	return getWorkAutoTime();
            case 28:
                return getHomeTazMaasAccessibility();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}