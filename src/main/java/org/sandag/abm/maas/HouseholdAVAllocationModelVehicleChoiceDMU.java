package org.sandag.abm.maas;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 */
public class HouseholdAVAllocationModelVehicleChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(HouseholdAVAllocationModelVehicleChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues                dmuIndex;
    int vehicle1IsAvailable;
    int vehicle2IsAvailable;
    int vehicle3IsAvailable;
    int personWithVehicle1;
    int personWithVehicle2;
    int personWithVehicle3;
    float travelUtilityToPersonVeh1;
    float travelUtilityToPersonVeh2;
    float travelUtilityToPersonVeh3;
    float minutesUntilNextTrip;
 
    public HouseholdAVAllocationModelVehicleChoiceDMU()
    {
        dmuIndex = new IndexValues();
        setupMethodIndexMap();
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


    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();
        methodIndexMap.put("getVehicle1IsAvailable",1);
        methodIndexMap.put("getVehicle2IsAvailable",2);
        methodIndexMap.put("getVehicle3IsAvailable",3);
        methodIndexMap.put("getPersonWithVehicle1",4);
        methodIndexMap.put("getPersonWithVehicle2",5);
        methodIndexMap.put("getPersonWithVehicle3",6);
        methodIndexMap.put("getTravelUtilityToPersonVeh1",7);
        methodIndexMap.put("getTravelUtilityToPersonVeh2",8);
        methodIndexMap.put("getTravelUtilityToPersonVeh3",9);
        methodIndexMap.put("getMinutesUntilNextTrip",10);
        
        
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
        	case 1:
        		return getVehicle1IsAvailable();
        	case 2:
        		return getVehicle2IsAvailable();
        	case 3:
        		return getVehicle3IsAvailable();
        	case 4:
        		return getPersonWithVehicle1();
        	case 5:
        		return getPersonWithVehicle2();
        	case 6:
        		return getPersonWithVehicle3();
        	case 7:
        		return getTravelUtilityToPersonVeh1();
        	case 8:
        		return getTravelUtilityToPersonVeh2();
        	case 9:
        		return getTravelUtilityToPersonVeh3();
        	case 10:
        		return getMinutesUntilNextTrip();

        	default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

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

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

	

	public float getTravelUtilityToPersonVeh1() {
		return travelUtilityToPersonVeh1;
	}

	public void setTravelUtilityToPersonVeh1(float travelUtilityToPersonVeh1) {
		this.travelUtilityToPersonVeh1 = travelUtilityToPersonVeh1;
	}

	public float getTravelUtilityToPersonVeh2() {
		return travelUtilityToPersonVeh2;
	}

	public void setTravelUtilityToPersonVeh2(float travelUtilityToPersonVeh2) {
		this.travelUtilityToPersonVeh2 = travelUtilityToPersonVeh2;
	}

	public float getTravelUtilityToPersonVeh3() {
		return travelUtilityToPersonVeh3;
	}

	public void setTravelUtilityToPersonVeh3(float travelUtilityToPersonVeh3) {
		this.travelUtilityToPersonVeh3 = travelUtilityToPersonVeh3;
	}

	public int getVehicle1IsAvailable() {
		return vehicle1IsAvailable;
	}

	public void setVehicle1IsAvailable(int vehicle1IsAvailable) {
		this.vehicle1IsAvailable = vehicle1IsAvailable;
	}

	public int getVehicle2IsAvailable() {
		return vehicle2IsAvailable;
	}

	public void setVehicle2IsAvailable(int vehicle2IsAvailable) {
		this.vehicle2IsAvailable = vehicle2IsAvailable;
	}

	public int getVehicle3IsAvailable() {
		return vehicle3IsAvailable;
	}

	public void setVehicle3IsAvailable(int vehicle3IsAvailable) {
		this.vehicle3IsAvailable = vehicle3IsAvailable;
	}

	public int getPersonWithVehicle1() {
		return personWithVehicle1;
	}

	public void setPersonWithVehicle1(int personWithVehicle1) {
		this.personWithVehicle1 = personWithVehicle1;
	}

	public int getPersonWithVehicle2() {
		return personWithVehicle2;
	}

	public void setPersonWithVehicle2(int personWithVehicle2) {
		this.personWithVehicle2 = personWithVehicle2;
	}

	public int getPersonWithVehicle3() {
		return personWithVehicle3;
	}

	public void setPersonWithVehicle3(int personWithVehicle3) {
		this.personWithVehicle3 = personWithVehicle3;
	}

	public float getMinutesUntilNextTrip() {
		return minutesUntilNextTrip;
	}

	public void setMinutesUntilNextTrip(float minutesUntilNextTrip) {
		this.minutesUntilNextTrip = minutesUntilNextTrip;
	}

}
