package org.sandag.abm.application;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.InternalExternalTripChoiceDMU;

public class SandagInternalExternalTripChoiceDMU
        extends InternalExternalTripChoiceDMU
{

    private transient Logger logger = Logger.getLogger(SandagInternalExternalTripChoiceDMU.class);

    public SandagInternalExternalTripChoiceDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getDistanceToCordonsLogsum", 0);
        methodIndexMap.put("getVehiclesPerHouseholdMember", 1);
        methodIndexMap.put("getHhIncomeInDollars", 2);
        methodIndexMap.put("getAge", 3);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getDistanceToCordonsLogsum();
            case 1:
                return getVehiclesPerHouseholdMember();
            case 2:
                return getHhIncomeInDollars();
            case 3:
                return getAge();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}