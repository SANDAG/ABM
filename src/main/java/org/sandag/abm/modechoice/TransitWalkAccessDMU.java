/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

/**
 * WalkDMU is the Decision-Making Unit class for the Walk-transit choice. The class
 * contains getter and setter methods for the variables used in the WalkPathUEC.
 * 
 * @author Joel Freedman
 * @version 1.0, March, 2009
 * 
 */
public class TransitWalkAccessDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TransitWalkAccessDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    double                              tapToMgraWalkTime;
    double                              mgraToTapWalkTime;
    double                              escalatorTime;

    public TransitWalkAccessDMU()
    {
        setupMethodIndexMap();
    }

    /**
     * Get the time from the production/origin MGRA to the boarding TAP.
     * 
     * @return The time from the production/origin MGRA to the boarding TAP.
     */
    public double getMgraTapWalkTime()
    {
        return mgraToTapWalkTime;
    }

    /**
     * Set the time from the production/origin MGRA to the boarding TAP.
     * 
     * @param walkTime The time from the production/origin MGRA to the boarding TAP.
     */
    public void setMgraTapWalkTime(double walkTime)
    {
        this.mgraToTapWalkTime = walkTime;
    }

    /**
     * Get the time from the alighting TAP to the attraction/destination MGRA.
     * 
     * @return The time from the alighting TAP to the attraction/destination MGRA.
     */
    public double getTapMgraWalkTime()
    {
        return tapToMgraWalkTime;
    }

    /**
     * Set the time from the alighting TAP to the attraction/destination MGRA.
     * 
     * @param walkTime The time from the alighting TAP to the attraction/destination
     *            MGRA.
     */
    public void setTapMgraWalkTime(double walkTime)
    {
        this.tapToMgraWalkTime = walkTime;
    }

    /**
     * Get the time to get to the platform.
     * 
     * @return The time in minutes.
     */
    public double getEscalatorTime()
    {
        return escalatorTime;
    }

    /**
     * Set the time to get to the platform.
     * 
     * @param escalatorTime The time in minutes.
     */
    public void setEscalatorTime(double escalatorTime)
    {
        this.escalatorTime = escalatorTime;
    }

    /**
     * Log the DMU values.
     * 
     * @param localLogger The logger to use.
     */
    public void logValues(Logger localLogger)
    {

        localLogger.info("");
        localLogger.info("Walk DMU Values:");
        localLogger.info("");
        localLogger.info(String.format("MGRA to TAP walk time:    %9.4f", mgraToTapWalkTime));
        localLogger.info(String.format("TAP to MGRA walk time:    %9.4f", tapToMgraWalkTime));
        localLogger.info(String.format("Escalator time:           %9.4f", escalatorTime));

    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getEscalatorTime", 0);
        methodIndexMap.put("getMgraTapWalkTime", 1);
        methodIndexMap.put("getTapMgraWalkTime", 2);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getEscalatorTime();
            case 1:
                return getMgraTapWalkTime();
            case 2:
                return getTapMgraWalkTime();

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
