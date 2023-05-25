package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 */
public class MicromobilityChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(MicromobilityChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues                dmuIndex;
    protected double ivtCoeff;
    protected double costCoeff;
    protected float walkTime;
    protected boolean isTransit;
    protected boolean microTransitAvailable;

 
    public MicromobilityChoiceDMU()
    {
        dmuIndex = new IndexValues();
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


	public double getIvtCoeff() {
		return ivtCoeff;
	}

	public void setIvtCoeff(double ivtCoeff) {
		this.ivtCoeff = ivtCoeff;
	}

	public double getCostCoeff() {
		return costCoeff;
	}

	public void setCostCoeff(double costCoeff) {
		this.costCoeff = costCoeff;
	}
    
    public float getWalkTime() {
		return walkTime;
	}

	public void setWalkTime(float walkTime) {
		this.walkTime = walkTime;
	}

	public boolean isTransit() {
		return isTransit;
	}

	public void setTransit(boolean isTransit) {
		this.isTransit = isTransit;
	}

	public boolean isMicroTransitAvailable() {
		return microTransitAvailable;
	}

	public void setMicroTransitAvailable(boolean microTransitAvailable) {
		this.microTransitAvailable = microTransitAvailable;
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
