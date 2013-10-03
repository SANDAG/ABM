package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import com.pb.common.calculator.VariableTable;

/**
 * @author crf <br/>
 *         Started: Nov 14, 2008 3:32:58 PM
 */
public class StopDCSoaDMU
        implements Serializable, VariableTable
{

    protected HashMap<String, Integer> methodIndexMap;

    protected int                      tourModeIndex;
    protected int[]                    walkTransitAvailableAtMgra;
    protected double                   origDestDistance;
    protected double[]                 distancesFromOrigMgra;
    protected double[]                 distancesToDestMgra;
    protected double[]                 logSizeTerms;
    protected ModelStructure           modelStructure;

    public StopDCSoaDMU(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
    }

    /**
     * set the array of distance values from the origin MGRA of the stop to all
     * MGRAs.
     * 
     * @param distances
     */
    public void setDistancesFromOrigMgra(double[] distances)
    {
        distancesFromOrigMgra = distances;
    }

    /**
     * set the array of distance values from all MGRAs to the final destination
     * MGRA of the stop.
     * 
     * @param distances
     */
    public void setDistancesToDestMgra(double[] distances)
    {
        distancesToDestMgra = distances;
    }

    /**
     * set the OD distance value from the stop origin MGRA to the final
     * destination MGRA of the stop.
     * 
     * @param distances
     */
    public void setOrigDestDistance(double distance)
    {
        origDestDistance = distance;
    }

    /**
     * set the tour mode index value for the tour of the stop being located
     * 
     * @param tour
     */
    public void setTourModeIndex(int index)
    {
        tourModeIndex = index;
    }

    /**
     * set the array of attributes for all MGRAs that says their is walk transit
     * access for the indexed mgra
     * 
     * @param tour
     */
    public void setWalkTransitAvailable(int[] avail)
    {
        walkTransitAvailableAtMgra = avail;
    }

    /**
     * set the array of logged size terms for all MGRAs for the stop being
     * located
     * 
     * @param size
     */
    public void setLnSlcSizeAlt(double[] size)
    {
        logSizeTerms = size;
    }

    public double getOrigToMgraDistanceAlt(int alt)
    {
        return distancesFromOrigMgra[alt];
    }

    public double getMgraToDestDistanceAlt(int alt)
    {
        return distancesToDestMgra[alt];
    }

    public double getOdDistance()
    {
        return origDestDistance;
    }

    public int getTourModeIsWalk()
    {
        boolean tourModeIsWalk = modelStructure.getTourModeIsWalk(tourModeIndex);
        return tourModeIsWalk ? 1 : 0;
    }

    public int getTourModeIsBike()
    {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike(tourModeIndex);
        return tourModeIsBike ? 1 : 0;
    }

    public int getTourModeIsWalkTransit()
    {
        boolean tourModeIsWalkLocal = modelStructure.getTourModeIsWalkLocal(tourModeIndex);
        boolean tourModeIsWalkPremium = modelStructure.getTourModeIsWalkPremium(tourModeIndex);
        return tourModeIsWalkLocal || tourModeIsWalkPremium ? 1 : 0;
    }

    public int getWalkTransitAvailableAlt(int alt)
    {
        return walkTransitAvailableAtMgra[alt];
    }

    public double getLnSlcSizeAlt(int alt)
    {
        return logSizeTerms[alt];
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
