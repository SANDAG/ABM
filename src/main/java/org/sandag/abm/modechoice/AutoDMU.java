package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

/**
 * This class is used for ...
 * 
 * @author Christi Willison
 * @version Mar 9, 2009
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class AutoDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(AutoDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private double                     avgHourlyParkingCostAtDestTaz;
    private float                      pTazTerminalTime;
    private float                      aTazTerminalTime;

    public AutoDMU()
    {
        setupMethodIndexMap();
    }

    public double getAvgHourlyParkingCostAtDestTaz()
    {
        return avgHourlyParkingCostAtDestTaz;
    }

    public void setAvgHourlyParkingCostAtDestTaz(double cost)
    {
        avgHourlyParkingCostAtDestTaz = cost;
    }

    public float getPTazTerminalTime()
    {
        return pTazTerminalTime;
    }

    public void setPTazTerminalTime(float pTazTerminalTime)
    {
        this.pTazTerminalTime = pTazTerminalTime;
    }

    public float getATazTerminalTime()
    {
        return aTazTerminalTime;
    }

    public void setATazTerminalTime(float aTazTerminalTime)
    {
        this.aTazTerminalTime = aTazTerminalTime;
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
        localLogger.info("Auto DMU Values:");
        localLogger.info("");
        localLogger.info(String.format("Average TAZ Parking cost at destination:     %9f",
                avgHourlyParkingCostAtDestTaz));
        localLogger.info(String.format("Production/Origin Terminal Time: %9.4f", pTazTerminalTime));
        localLogger.info(String.format("Attraction/Destin Terminal Time: %9.4f", aTazTerminalTime));

    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getAvgHourlyParkingCostAtDestTaz", 0);
        methodIndexMap.put("getATazTerminalTime", 1);
        methodIndexMap.put("getPTazTerminalTime", 2);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getAvgHourlyParkingCostAtDestTaz();
            case 1:
                return getATazTerminalTime();
            case 2:
                return getPTazTerminalTime();

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
