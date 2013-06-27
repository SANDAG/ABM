package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * @author crf <br/>
 *         Started: Apr 14, 2009 11:09:58 AM
 */
public class TransponderChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TransponderChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues dmuIndex;

    private Household hh;

    private double percentTazIncome100Kplus;
    private double percentTazMultpleAutos;
    private double expectedTravelTimeSavings;
    private double transpDist;
    private double pctDetour;
    private double accessibility;
    
    
    public TransponderChoiceDMU()
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
        if (hh.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug Free Parking UEC");
        }
    }

    public void setHouseholdObject(Household hhObj) {
        hh = hhObj;
    }


    public void setPctIncome100Kplus( double value) {
        percentTazIncome100Kplus = value;
    }

    public void setPctTazMultpleAutos( double value) {
        percentTazMultpleAutos = value;
    }

    public void setExpectedTravelTimeSavings( double value) {
        expectedTravelTimeSavings = value;
    }

    public void setTransponderDistance( double value) {
        transpDist = value;
    }
    
    public void setPctDetour( double value) {
        pctDetour = value;
    }
    
    public void setAccessibility( double value) {
        accessibility = value;
    }
    
    
    public double getPctIncome100Kplus() {
        return percentTazIncome100Kplus;
    }
    
    public double getPctTazMultpleAutos() {
        return percentTazMultpleAutos;
    }
    
    public double getExpectedTravelTimeSavings() {
        return expectedTravelTimeSavings;
    }
    
    public double getTransponderDistance() {
        return transpDist;
    }
    
    public double getPctDetour() {
        return pctDetour;
    }
    
    public double getAccessibility() {
        return accessibility;
    }
    
    public int getAutoOwnership() {
        return hh.getAutoOwnershipModelResult();
    }

    public IndexValues getDmuIndexValues() {
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
