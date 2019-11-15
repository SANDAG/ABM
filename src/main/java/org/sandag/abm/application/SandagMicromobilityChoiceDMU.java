package org.sandag.abm.application;

import java.util.HashMap;

import org.sandag.abm.ctramp.MicromobilityChoiceDMU;

public class SandagMicromobilityChoiceDMU
        extends MicromobilityChoiceDMU
{

    public SandagMicromobilityChoiceDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getIvtCoeff", 0);
        methodIndexMap.put("getCostCoeff", 1);
        methodIndexMap.put("getWalkTime", 2);
               
      }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
        case 0:
            return getIvtCoeff();
        case 1:
            return getCostCoeff();
        case 2:
            return getWalkTime();

        default:
            logger.error("method number = " + variableIndex + " not found");
            throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}