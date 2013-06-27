package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * @author crf <br/>
 *         Started: Apr 14, 2009 11:09:58 AM
 */
public class InternalExternalTripChoiceDMU
        implements Serializable, VariableTable
{

    protected HashMap<String, Integer> methodIndexMap;

    private Household hh;
    private Person person;

    private double distanceToCordonsLogsum;
    private double vehiclesPerHouseholdMember;
    
    private IndexValues iv;
    
    
    public InternalExternalTripChoiceDMU()
    {
        iv = new IndexValues();
    }

    public void setDmuIndexValues( int hhid, int hhtaz ) {
        iv.setHHIndex(hhid);
        iv.setZoneIndex(hhtaz);
        iv.setDebug( hh.getDebugChoiceModels() );
    }
    
    public IndexValues getDmuIndexValues() {
        return iv;
    }
    
    public void setHouseholdObject(Household hhObj) {
        hh = hhObj;
    }

    public void setPersonObject(Person persObj) {
        person = persObj;
    }


    public void setDistanceToCordonsLogsum( double value) {
        distanceToCordonsLogsum = value;
    }

    public double getDistanceToCordonsLogsum() {
        return distanceToCordonsLogsum;
    }

    public void setVehiclesPerHouseholdMember( double value) {
        vehiclesPerHouseholdMember = value;
    }

    public double getVehiclesPerHouseholdMember() {
        return vehiclesPerHouseholdMember;
    }

    public int getHhIncomeInDollars() {
        return hh.getIncomeInDollars();
    }
    
    public int getAge() {
        return person.getAge();
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
