package org.sandag.abm.specialevent;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class SpecialEventOriginChoiceDMU        implements Serializable, VariableTable
{

    protected transient Logger                logger                                    = Logger.getLogger("specialEventModel");

    
    protected HashMap<String, Integer> methodIndexMap;
    protected IndexValues              dmuIndex;

    protected float tourDepartPeriod;
    protected float tourArrivePeriod;
    protected int purpose;
    protected double[][] sizeTerms;       //by purpose, alternative (taz or sampled mgras)
    
    
    protected double                   nmWalkTimeOut;
    protected double                   nmWalkTimeIn;
    protected double                   nmBikeTimeOut;
    protected double                   nmBikeTimeIn;
    protected double                   lsWgtAvgCostM;
    protected double                   lsWgtAvgCostD;
    protected double                   lsWgtAvgCostH;


    public SpecialEventOriginChoiceDMU(SandagModelStructure modelStructure)
    {
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
        
    }

 
   /**
     * Set this index values for this tour mode choice DMU object.
     * 
     * @param hhIndex is the DMU household index
     * @param zoneIndex is the DMU zone index
     * @param origIndex is the DMU origin index
     * @param destIndex is the DMU desatination index
     */
    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex, boolean debug)
    {
        dmuIndex.setHHIndex(hhIndex);
        dmuIndex.setZoneIndex(zoneIndex);
        dmuIndex.setOriginZone(origIndex);
        dmuIndex.setDestZone(destIndex);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (debug)
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug MC UEC");
        }

    }

    /**
	 * @return the sizeTerm.  The size term is the size of the origin taz.
	 */
	public double getSizeTerm(int alt) {
		return sizeTerms[purpose][alt];
	}

	/**
	 * @param sizeTerms the sizeTerms to set.  The size term is the array of origin taz sizes.
	 */
	public void setSizeTerms(double[][] sizeTerms) {
		this.sizeTerms = sizeTerms;
	}



	public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /**
	 * @return the purpose
	 */
	public int getPurpose() {
		return purpose;
	}

	/**
	 * @param purpose the purpose to set
	 */
	public void setPurpose(int purpose) {
		this.purpose = purpose;
	}

	  
    public float getTimeOutbound()
    {
        return tourDepartPeriod;
    }

    public float getTimeInbound()
    {
        return tourArrivePeriod;
    }

    /**
	 * @param tourDepartPeriod the tourDepartPeriod to set
	 */
	public void setTourDepartPeriod(float tourDepartPeriod) {
		this.tourDepartPeriod = tourDepartPeriod;
	}

	/**
	 * @param tourArrivePeriod the tourArrivePeriod to set
	 */
	public void setTourArrivePeriod(float tourArrivePeriod) {
		this.tourArrivePeriod = tourArrivePeriod;
	}

   
    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTimeOutbound", 0);
        methodIndexMap.put("getTimeInbound", 1);
        methodIndexMap.put("getSizeTerm",2);
          
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;

        switch (variableIndex)
        {

            case 0:
                returnValue = getTimeOutbound();
                break;
            case 1:
                returnValue = getTimeInbound();
                break;
           case 2:
                returnValue = getSizeTerm(arrayIndex);
                break;
           default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

        return returnValue;

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


}