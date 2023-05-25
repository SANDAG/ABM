package org.sandag.abm.specialevent;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class SpecialEventTripModeChoiceDMU
                implements Serializable, VariableTable
{
    protected transient Logger logger = Logger.getLogger(SpecialEventTripModeChoiceDMU.class);
    protected HashMap<String, Integer> methodIndexMap;
    protected IndexValues              dmuIndex;

    protected int          tourDepartPeriod;
    protected int          tourArrivePeriod;
    protected int          tripPeriod;
    protected int          tripOrigIsTourDest;
    protected int          tripDestIsTourDest;
    protected float        parkingCost;
    protected float        parkingTime;
    protected int          income;
    protected int          partySize;

    protected double       nmWalkTime;
    protected double       nmBikeTime;
    
	protected double ivtCoeff;
    protected double costCoeff;

    protected double walkTransitLogsum;
    protected double pnrTransitLogsum;
    protected double knrTransitLogsum;

    protected int outboundHalfTourDirection;

    public SpecialEventTripModeChoiceDMU(SandagModelStructure modelStructure, Logger aLogger)
    {
        if (aLogger == null)
        {
            aLogger = Logger.getLogger("specialEventModel");
        }
        logger = aLogger;
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
 
    }

    /**
     * Set this index values for this tour mode choice DMU object.
     * 
     * @param hhIndex
     *            is the DMU household index
     * @param zoneIndex
     *            is the DMU zone index
     * @param origIndex
     *            is the DMU origin index
     * @param destIndex
     *            is the DMU desatination index
     */
    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex,
            boolean debug)
    {
        dmuIndex.setHHIndex(hhIndex);
        dmuIndex.setZoneIndex(zoneIndex);
        dmuIndex.setOriginZone(origIndex);
        dmuIndex.setDestZone(destIndex);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (debug)
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug MC UEC");
        }

    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /**
     * @return the tripPeriod
     */
    public int getTripPeriod()
    {
        return tripPeriod;
    }

    /**
     * @param tripPeriod
     *            the tripPeriod to set
     */
    public void setTripPeriod(int tripPeriod)
    {
        this.tripPeriod = tripPeriod;
    }

    /**
     * @return the tripOrigIsTourDest
     */
    public int getTripOrigIsTourDest()
    {
        return tripOrigIsTourDest;
    }

    /**
     * @param tripOrigIsTourDest
     *            the tripOrigIsTourDest to set
     */
    public void setTripOrigIsTourDest(int tripOrigIsTourDest)
    {
        this.tripOrigIsTourDest = tripOrigIsTourDest;
    }

    /**
     * @return the tripDestIsTourDest
     */
    public int getTripDestIsTourDest()
    {
        return tripDestIsTourDest;
    }

    /**
     * @param tripDestIsTourDest
     *            the tripDestIsTourDest to set
     */
    public void setTripDestIsTourDest(int tripDestIsTourDest)
    {
        this.tripDestIsTourDest = tripDestIsTourDest;
    }

    /**
     * @return the outboundHalfTourDirection
     */
    public int getOutboundHalfTourDirection()
    {
        return outboundHalfTourDirection;
    }

    /**
     * @param outboundHalfTourDirection
     *            the outboundHalfTourDirection to set
     */
    public void setOutboundHalfTourDirection(int outboundHalfTourDirection)
    {
        this.outboundHalfTourDirection = outboundHalfTourDirection;
    }

    /**
     * @return the tourDepartPeriod
     */
    public int getTourDepartPeriod()
    {
        return tourDepartPeriod;
    }

    /**
     * @param tourDepartPeriod
     *            the tourDepartPeriod to set
     */
    public void setTourDepartPeriod(int tourDepartPeriod)
    {
        this.tourDepartPeriod = tourDepartPeriod;
    }

    /**
     * @param tourArrivePeriod
     *            the tourArrivePeriod to set
     */
    public void setTourArrivePeriod(int tourArrivePeriod)
    {
        this.tourArrivePeriod = tourArrivePeriod;
    }

    /**
     * @return the tourArrivePeriod
     */
    public int getTourArrivePeriod()
    {
        return tourArrivePeriod;
    }

    public double getNm_walkTime()
    {
        return nmWalkTime;
    }

    public void setNonMotorizedWalkTime(double nmWalkTime)
    {
        this.nmWalkTime = nmWalkTime;
    }

    public void setNonMotorizedBikeTime(double nmBikeTime)
    {
        this.nmBikeTime = nmBikeTime;
    }

    public double getNm_bikeTime()
    {
        return nmBikeTime;
    }

    /**
     * @return the parkingCost
     */
    public float getParkingCost()
    {
        return parkingCost;
    }

    /**
     * @param parkingCost
     *            the parkingCost to set
     */
    public void setParkingCost(float parkingCost)
    {
        this.parkingCost = parkingCost;
    }

    /**
     * @return the parkingTime
     */
    public float getParkingTime()
    {
        return parkingTime;
    }

    /**
     * @param parkingTime
     *            the parkingTime to set
     */
    public void setParkingTime(float parkingTime)
    {
        this.parkingTime = parkingTime;
    }

    /**
     * @return the income
     */
    public int getIncome()
    {
        return income;
    }

    /**
     * @param income
     *            the income to set
     */
    public void setIncome(int income)
    {
        this.income = income;
    }

    /**
     * @return the partySize
     */
    public int getPartySize()
    {
        return partySize;
    }

    /**
     * @param partySize
     *            the partySize to set
     */
    public void setPartySize(int partySize)
    {
        this.partySize = partySize;
    }

    public double getNmWalkTime() {
		return nmWalkTime;
	}

	public void setNmWalkTime(double nmWalkTime) {
		this.nmWalkTime = nmWalkTime;
	}

	public double getNmBikeTime() {
		return nmBikeTime;
	}

	public void setNmBikeTime(double nmBikeTime) {
		this.nmBikeTime = nmBikeTime;
	}

	public double getIvtCoeff() {
		return ivtCoeff;
	}

	public void setIvtCoeff(double ivtCoeff) {
		this.ivtCoeff = ivtCoeff;
	}

	public double getCostCoeff() {
		return costCoeff;
	}

	public void setCostCoeff(double costCoeff) {
		this.costCoeff = costCoeff;
	}

	public double getWalkTransitLogsum() {
		return walkTransitLogsum;
	}

	public void setWalkTransitLogsum(double walkTransitLogsum) {
		this.walkTransitLogsum = walkTransitLogsum;
	}

	public double getPnrTransitLogsum() {
		return pnrTransitLogsum;
	}

	public void setPnrTransitLogsum(double pnrTransitLogsum) {
		this.pnrTransitLogsum = pnrTransitLogsum;
	}

	public double getKnrTransitLogsum() {
		return knrTransitLogsum;
	}

	public void setKnrTransitLogsum(double knrTransitLogsum) {
		this.knrTransitLogsum = knrTransitLogsum;
	}

	private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTourDepartPeriod", 0);
        methodIndexMap.put("getTourArrivePeriod", 1);
        methodIndexMap.put("getTripPeriod", 2);
        methodIndexMap.put("getParkingCost", 3);
        methodIndexMap.put("getParkingTime", 4);
        methodIndexMap.put("getTripOrigIsTourDest", 5);
        methodIndexMap.put("getTripDestIsTourDest", 6);
        methodIndexMap.put("getIncome", 7);
        methodIndexMap.put("getPartySize", 8);

        methodIndexMap.put("getIvtCoeff", 60);
        methodIndexMap.put("getCostCoeff", 61);
               
        methodIndexMap.put("getWalkSetLogSum", 62);
        methodIndexMap.put("getPnrSetLogSum", 63);
        methodIndexMap.put("getKnrSetLogSum", 64);

        methodIndexMap.put("getNm_walkTime", 90);
        methodIndexMap.put("getNm_bikeTime", 91);
        
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
 
    	double returnValue = -1;

        switch (variableIndex)
        {

            case 0:
                returnValue = getTourDepartPeriod();
                break;
            case 1:
            	returnValue = getTourArrivePeriod();
                break;
            case 2:
            	returnValue = getTripPeriod();
                break;
            case 3:
            	returnValue = getParkingCost();
                break;
            case 4:
            	returnValue = getParkingTime();
                break;
            case 5:
            	returnValue = getTripOrigIsTourDest();
                break;
            case 6:
            	returnValue = getTripDestIsTourDest();
                break;
            case 7:
            	returnValue = getIncome();
                break;
            case 8:
            	returnValue = getPartySize();
                break;
            case 60:
            	returnValue = getIvtCoeff();
            	break;
            case 61:
            	returnValue = getCostCoeff();
                break;
            case 62:
                returnValue = getWalkTransitLogsum();
                break;
            case 63:
            	returnValue = getPnrTransitLogsum();
                break;
            case 64:
            	returnValue = getKnrTransitLogsum();
                break;
            case 90:
            	 returnValue = getNm_walkTime();
            	 break;
            case 91:
            	returnValue = getNm_bikeTime();
            	break;
            default:
                logger.error( "method number = " + variableIndex + " not found" );
                throw new RuntimeException( "method number = " + variableIndex + " not found" );
        }
        return returnValue;

    
    
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
