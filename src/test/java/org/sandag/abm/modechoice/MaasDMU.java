package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

/**
 * This class is the DMU object for MAAS
 * joel freedman
 * RSG 2019-07-08
 **/

public class MaasDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(MaasDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected float waitTimeTaxi;
    protected float waitTimeSingleTNC;
    protected float waitTimeSharedTNC;

    public MaasDMU()
    {
        setupMethodIndexMap();
    }

    public float getWaitTimeTaxi() {
 		return waitTimeTaxi;
 	}

 	public void setWaitTimeTaxi(float waitTimeTaxi) {
 		this.waitTimeTaxi = waitTimeTaxi;
 	}

 	public float getWaitTimeSingleTNC() {
 		return waitTimeSingleTNC;
 	}

 	public void setWaitTimeSingleTNC(float waitTimeSingleTNC) {
 		this.waitTimeSingleTNC = waitTimeSingleTNC;
 	}

 	public float getWaitTimeSharedTNC() {
 		return waitTimeSharedTNC;
 	}

 	public void setWaitTimeSharedTNC(float waitTimeSharedTNC) {
 		this.waitTimeSharedTNC = waitTimeSharedTNC;
 	}
 	

    /**
     * Log the DMU values.
     * 
     * @param localLogger
     *            The logger to use.
     */
    public void logValues(Logger localLogger)
    {

        localLogger.info("");
        localLogger.info("Maas DMU Values:");
        localLogger.info("");
        localLogger.info(String.format("Taxi wait time:     %9.2f",   waitTimeTaxi));
        localLogger.info(String.format("Single TNC wait time: %9.2f", waitTimeSingleTNC));
        localLogger.info(String.format("Shared TNC wait time: %9.2f", waitTimeSharedTNC));

    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getWaitTimeTaxi", 0);
        methodIndexMap.put("getWaitTimeSingleTNC", 1);
        methodIndexMap.put("getWaitTimeSharedTNC", 2);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getWaitTimeTaxi();
            case 1:
                return getWaitTimeSingleTNC();
            case 2:
                return getWaitTimeSharedTNC();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
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
