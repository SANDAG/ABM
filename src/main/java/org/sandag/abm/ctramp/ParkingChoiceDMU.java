package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.util.HashMap;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * @author crf <br/>
 *         Started: Apr 14, 2009 1:34:03 PM
 */
public class ParkingChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger logger = Logger.getLogger(ParkingChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues dmuIndex;
    
    private int personType;
    private int activityIntervals;
    private int destPurpose;
    private double reimbPct;

    private double[] distancesOrigAlt;
    private double[] distancesAltDest;

    private double[] altParkingCostsM;
    private int[] altMstallsoth;
    private int[] altMstallssam;
    private float[] altMparkcost;
    private int[] altDstallsoth;
    private int[] altDstallssam;
    private float[] altDparkcost;
    private int[] altHstallsoth;
    private int[] altHstallssam;
    private float[] altHparkcost;
    private int[] altNumfreehrs;

    private int[] parkAreaMgras;
    private int[] altMgraIndices;
    
    
    public ParkingChoiceDMU()
    {
        dmuIndex = new IndexValues();
    }

    public void setDmuIndexValues(int hhId, int origMgra, int destMgra, boolean hhDebug)
    {
        dmuIndex.setHHIndex(hhId);
        dmuIndex.setOriginZone(origMgra);
        dmuIndex.setDestZone(destMgra);
        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");

        if (hhDebug)
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug Parking Choice UEC");
        }

    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }
    

    public void setPersonType(int value) {
        personType = value;
    }
    
    public void setActivityIntervals(int value) {
        activityIntervals = value;
    }
    
    public void setDestPurpose(int value) {
        destPurpose = value;
    }

    public void setReimbPct(double value) {
        reimbPct = value;
    }

    /**
     * @param mgras is the array of MGRAs in parking area from "plc.alts.corresp.file".
     * This is a 0-based array
     */
    public void setParkAreaMgraArray( int[] mgras ) {
        parkAreaMgras = mgras;
    }
    
    /**
     * @param indices is an array of indices representing this person's park location choice sample.
     * the index value in this array points to the parkAreaMgras element, and the corresponding mgra value.
     * This is a 0-based array
     */
    public void setSampleIndicesArray( int[] indices ) {
        altMgraIndices = indices;
    }
    
    public void setDistancesOrigAlt( double[] distances ) {
        distancesOrigAlt = distances;
    }
    
    public void setDistancesAltDest( double[] distances ) {
        distancesAltDest = distances;
    }
    
    public void setParkingCostsM( double[] values ) {
        altParkingCostsM = values;
    }
    
    public void setMstallsoth( int[] values ) {
        altMstallsoth = values;
    }

    public void setMstallssam( int[] values ) {
        altMstallssam = values;
    }

    public void setMparkCost( float[] values ) {
        altMparkcost = values;
    }

    public void setDstallsoth( int[] values ) {
        altDstallsoth = values;
    }

    public void setDstallssam( int[] values ) {
        altDstallssam = values;
    }

    public void setDparkCost( float[] values ) {
        altDparkcost = values;
    }

    public void setHstallsoth( int[] values ) {
        altHstallsoth = values;
    }

    public void setHstallssam( int[] values ) {
        altHstallssam = values;
    }

    public void setHparkCost( float[] values ) {
        altHparkcost = values;
    }

    public void setNumfreehrs( int[] values ) {
        altNumfreehrs = values;
    }

    
    public int getPersonType() {
        return personType;
    }
    
    public int getActivityIntervals() {
        return activityIntervals;
    }
    
    public int getTripDestPurpose() {
        return destPurpose;
    }

    public double getReimbPct() {
        return reimbPct;
    }

    
    /**
     * @param alt is the index value in the alternatives array (0,...,num alts).
     * @return the distance between the trip origin mgra and the alternative park mgra.
     */
    public double getDistanceTripOrigToParkAlt( int alt ) {
        return distancesOrigAlt[alt];
    }
    
    /**
     * @param alt is the index value in the alternatives array (0,...,num alts).
     * @return the distance between the alternative park mgra and the trip destination mgra.
     */
    public double getDistanceTripDestFromParkAlt( int alt ) {
        return distancesAltDest[alt];
    }
    
    /**
     * @param alt is the index value in the alternatives array (0,...,num alts).
     * @return the cost for this person to park at the alternative park mgra.
     */
    public double getLsWgtAvgCostM( int alt ) {
        return altParkingCostsM[alt];
    }

    public int getMstallsoth( int alt ) {
        return altMstallsoth[alt];
    }

    public int getMstallssam( int alt ) {
        return altMstallssam[alt];
    }

    public float getMparkcost( int alt ) {
        return altMparkcost[alt];
    }

    public int getDstallsoth( int alt ) {
        return altDstallsoth[alt];
    }

    public int getDstallssam( int alt ) {
        return altDstallssam[alt];
    }

    public float getDparkcost( int alt ) {
        return altDparkcost[alt];
    }

    public int getHstallsoth( int alt ) {
        return altHstallsoth[alt];
    }

    public int getHstallssam( int alt ) {
        return altHstallssam[alt];
    }

    public float getHparkcost( int alt ) {
        return altHparkcost[alt];
    }

    public int getNumfreehrs( int alt ) {
        return altNumfreehrs[alt];
    }

    /**
     * @return 1 if the altMgra attribute that was set equals the trip destination
     */    
    public int getDestSameAsParkAlt( int alt ) {
        int index = altMgraIndices[alt];
        int altMgra = parkAreaMgras[index];
        return altMgra == dmuIndex.getDestZone() ? 1 : 0;
    }
    
    /**
     * @return the altMgra attribute for this alternative
     */    
    public int getParkMgraAlt( int alt ) {
        int index = altMgraIndices[alt];
        int altMgra = parkAreaMgras[index];
        return altMgra;
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
