package org.sandag.abm.maas;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 */
public class HouseholdAVAllocationModelTripUtilityDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(HouseholdAVAllocationModelTripUtilityDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues                dmuIndex;
    int timeTrip; //trip period
 
    public HouseholdAVAllocationModelTripUtilityDMU()
    {
        dmuIndex = new IndexValues();
        setupMethodIndexMap();
    }

    public void setDmuIndexValues(int hhId, int zoneId, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(hhId);
        dmuIndex.setZoneIndex(zoneId);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
    }


    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();
        methodIndexMap.put("getTimeTrip", 1);
    
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
        	case 1:
        		return getTimeTrip();
    
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

	public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
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

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

	public int getTimeTrip() {
		return timeTrip;
	}

	public void setTimeTrip(int timeTrip) {
		this.timeTrip = timeTrip;
	}

}
