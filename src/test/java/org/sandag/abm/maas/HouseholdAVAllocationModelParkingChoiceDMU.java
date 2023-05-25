package org.sandag.abm.maas;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 */
public class HouseholdAVAllocationModelParkingChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(HouseholdAVAllocationModelParkingChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private IndexValues                dmuIndex;
    float durationBeforeNextTrip;
    int personType;
    int atWork;
    int freeParkingEligibility;
    float reimburseProportion;
    float dailyParkingCost;
    float hourlyParkingCost;
    float monthlyParkingCost;
    int parkingArea;
    float utilityToClosestRemoteLot;
    float utilityToHome;
    float utilityFromHomeToNextTrip;

 
    public HouseholdAVAllocationModelParkingChoiceDMU()
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
        
        methodIndexMap.put("getDurationBeforeNextTrip",1); 
        methodIndexMap.put("getPersonType",2); 
        methodIndexMap.put("getAtWork",3); 
        methodIndexMap.put("getFreeParkingEligibility",4); 
        methodIndexMap.put("getReimburseProportion",5); 
        methodIndexMap.put("getDailyParkingCost",6); 
       	methodIndexMap.put("getHourlyParkingCost",7); 
       	methodIndexMap.put("getMonthlyParkingCost",8); 
       	methodIndexMap.put("getParkingArea",9); 
       	methodIndexMap.put("getUtilityToClosestRemoteLot",10); 
       	methodIndexMap.put("getUtilityToHome",11); 
       	methodIndexMap.put("getUtilityFromHomeToNextTrip",12); 
          
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
        	case 1:
        		return getDurationBeforeNextTrip();
        	case 2:
        		return getPersonType();
        	case 3:
        		return getAtWork();
        	case 4:
        		return getFreeParkingEligibility();
        	case 5:
        		return getReimburseProportion();
        	case 6:
        		return getDailyParkingCost();
        	case 7:
        		return getHourlyParkingCost(); 
        	case 8:
        		return getMonthlyParkingCost();
        	case 9:
        		return getParkingArea();
        	case 10:
        		return getUtilityToClosestRemoteLot();
        	case 11: 
        		return getUtilityToHome();
        	case 12:
        		return getUtilityFromHomeToNextTrip();

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

	public float getDurationBeforeNextTrip() {
		return durationBeforeNextTrip;
	}

	public void setDurationBeforeNextTrip(float durationBeforeNextTrip) {
		this.durationBeforeNextTrip = durationBeforeNextTrip;
	}

	public int getPersonType() {
		return personType;
	}

	public void setPersonType(int personType) {
		this.personType = personType;
	}

	public int getAtWork() {
		return atWork;
	}

	public void setAtWork(int atWork) {
		this.atWork = atWork;
	}

	public int getFreeParkingEligibility() {
		return freeParkingEligibility;
	}

	public void setFreeParkingEligibility(int freeParkingEligibility) {
		this.freeParkingEligibility = freeParkingEligibility;
	}

	public float getReimburseProportion() {
		return reimburseProportion;
	}

	public void setReimburseProportion(float reimburseProportion) {
		this.reimburseProportion = reimburseProportion;
	}

	public float getDailyParkingCost() {
		return dailyParkingCost;
	}

	public void setDailyParkingCost(float dailyParkingCost) {
		this.dailyParkingCost = dailyParkingCost;
	}

	public float getHourlyParkingCost() {
		return hourlyParkingCost;
	}

	public void setHourlyParkingCost(float hourlyParkingCost) {
		this.hourlyParkingCost = hourlyParkingCost;
	}

	public float getMonthlyParkingCost() {
		return monthlyParkingCost;
	}

	public void setMonthlyParkingCost(float monthlyParkingCost) {
		this.monthlyParkingCost = monthlyParkingCost;
	}

	public int getParkingArea() {
		return parkingArea;
	}

	public void setParkingArea(int parkingArea) {
		this.parkingArea = parkingArea;
	}

	public float getUtilityToClosestRemoteLot() {
		return utilityToClosestRemoteLot;
	}

	public void setUtilityToClosestRemoteLot(float utilityToClosestRemoteLot) {
		this.utilityToClosestRemoteLot = utilityToClosestRemoteLot;
	}

	public float getUtilityToHome() {
		return utilityToHome;
	}

	public void setUtilityToHome(float utilityToHome) {
		this.utilityToHome = utilityToHome;
	}

	public float getUtilityFromHomeToNextTrip() {
		return utilityFromHomeToNextTrip;
	}

	public void setUtilityFromHomeToNextTrip(float utilityFromHomeToNextTrip) {
		this.utilityFromHomeToNextTrip = utilityFromHomeToNextTrip;
	}

	

	

}
