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
public class TelecommuteDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TelecommuteDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private Household                  hh;
    private Person                     person;
    private IndexValues                dmuIndex;

       public TelecommuteDMU()
    {
        dmuIndex = new IndexValues();
    }

    /** need to set hh and home taz before using **/
    public void setPersonObject(Person person)
    {
        hh = person.getHouseholdObject();
        this.person = person;
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
            dmuIndex.setDebugLabel("Debug Telecommute UEC");
        }
    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /* dmu @ functions */

    public int getIncomeInDollars()
    {
        return hh.getIncomeInDollars();
    }

    public int getNumberOfAdults() 
    {
    	Person[] persons = hh.getPersons();
    	int adults=0;
    	for(int i=0;i<persons.length;++i) {
    		
    		if(persons[i].getAge()>=18)
    			++adults;
    	}
    	return adults;
    }
    
    public int getHasKids_0_5() 
    {
    	Person[] persons = hh.getPersons();
    	int hasKids_0_5=0;
    	for(int i=0;i<persons.length;++i) {
    		
    		if((persons[i].getAge()>=0) && persons[i].getAge()<=5) {
    			hasKids_0_5=1;
    			break;
    		}
    	}
    	return hasKids_0_5;
    }


    public int getHasKids_6_12() 
    {
    	Person[] persons = hh.getPersons();
    	int hasKids_6_12=0;
    	for(int i=0;i<persons.length;++i) {
    		
    		if((persons[i].getAge()>=6) && persons[i].getAge()<=12) {
    			hasKids_6_12=1;
    			break;
    		}
    	}
    	return hasKids_6_12;
    }

    public int getFemale()
    {
    	return person.getPersonIsFemale();
    }
    
    public int getPersonType()
    {
    	return person.getPersonTypeNumber();
    }
    
    public int getNumberOfAutos()
    {
    	return hh.getAutosOwned();
    	   
    }
    
    public int getOccupation()
    {
    	return person.getPersPecasOccup();
    }
    
    public int getPaysToPark()
    {
	
    	int freeParkingChoice = person.getFreeParkingAvailableResult();
    	if((freeParkingChoice==ParkingProvisionModel.FP_MODEL_PAY_ALT)||
    			(freeParkingChoice==ParkingProvisionModel.FP_MODEL_REIMB_ALT))
	    	
	    	return 1;
    	return 0;
    }

    public float getWorkDistance() {
    	
    	return person.getWorkLocationDistance();
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
