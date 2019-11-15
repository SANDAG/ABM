package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 */
public class VisitorMicromobilityChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(VisitorMicromobilityChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues                dmuIndex;
    protected int                      income;
    protected float walkTime;

 
    public VisitorMicromobilityChoiceDMU()
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


    public int getIncome()
    {
        return income;
    }

	public void setIncome(int income) {
		this.income = income;
	}
 
    public float getWalkTime() {
		return walkTime;
	}

	public void setWalkTime(float walkTime) {
		this.walkTime = walkTime;
	}

	public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getIncome", 0);
        methodIndexMap.put("getWalkTime", 1);
               
      }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
        case 0:
            return getIncome();
        case 1:
            return getWalkTime();

        default:
            logger.error("method number = " + variableIndex + " not found");
            throw new RuntimeException("method number = " + variableIndex + " not found");

        }
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

}
