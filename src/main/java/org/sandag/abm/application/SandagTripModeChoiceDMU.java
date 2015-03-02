package org.sandag.abm.application;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.BikeLogsumSegment;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TripModeChoiceDMU;

import com.pb.common.calculator.IndexValues;

public class SandagTripModeChoiceDMU
        extends TripModeChoiceDMU
{
	private int setPersonHhTourCounter = 0;
	private BikeLogsum bls;
    protected double femaleBikeLogsum;
    protected double maleBikeLogsum;
    protected double femaleInParty;
    protected double maleInParty;
     
    public SandagTripModeChoiceDMU(ModelStructure modelStructure, Logger aLogger)
    {
        super(modelStructure, aLogger);
        setupMethodIndexMap();
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
     *            is the DMU destination index
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

     public int getEscortTour()
    {
        return escortTour;
    }

    @Override
    public int getTourCategoryJoint()
    {
        return jointTour;
    }

    @Override
    public int getNumberOfParticipantsInJointTour()
    {
        return partySize;
    }

    public int getAutos()
    {
        return autos;
    }

    public int getAge()
    {
        return age;
    }

    public int getAdults()
    {
        return adults;
    }

    public int getHhSize()
    {
        return hhSize;
    }

    public int getFemale()
    {
        return personIsFemale;
    }

    public int getIncome()
    {
        return incomeInDollars;
    }

    @Override
    public float getTimeOutbound()
    {
        return departPeriod;
    }

    @Override
    public float getTimeInbound()
    {
        return arrivePeriod;
    }

    public int getTimeTrip()
    {
        return tripPeriod;
    }

    public int getOutboundStops()
    {
        return outboundStops;
    }

    public int getReturnStops()
    {
        return inboundStops;
    }

    public int getTourModeIsDA()
    {
        return tourModeIsDA;
    }

    public int getTourModeIsS2()
    {
        return tourModeIsS2;
    }

    public int getTourModeIsS3()
    {
        return tourModeIsS3;
    }

    public int getTourModeIsWalk()
    {
        return tourModeIsWalk;
    }

    public int getTourModeIsBike()
    {
        return tourModeIsBike;
    }

    public int getTourModeIsWTran()
    {
        return tourModeIsWTran;
    }

    public int getTourModeIsPNR()
    {
        return tourModeIsPnr;
    }

    public int getTourModeIsKNR()
    {
        return tourModeIsKnr;
    }

    public int getTourModeIsSchBus()
    {
        return tourModeIsSchBus;
    }

    public void setBikeLogsum(BikeLogsum bls) 
    {
    	this.bls = bls;
    }
    
    public void setPersonObject(Person person) 
    {
    	super.setPersonObject(person);
    	checkSetPersonHhTour();
    }

    public void setHouseholdObject(Household hh) 
    {
    	super.setHouseholdObject(hh);
    	checkSetPersonHhTour();
    }

    public void setTourObject(Tour tour) 
    {
    	super.setTourObject(tour);
    	checkSetPersonHhTour();
    }
    
    private void checkSetPersonHhTour() 
    {
    	setPersonHhTourCounter = (setPersonHhTourCounter+1) % 3;
    	if (setPersonHhTourCounter == 0) {
    		setParty(person,tour,hh);
    	}
    }
	
	public double getFemaleInParty() 
	{
		return femaleInParty;
	}
	
	public double getMaleInParty() 
	{
		return maleInParty;
	}
	
	public void setParty(Person person, Tour tour, Household hh) 
	{
        if (person != null) {
        	femaleInParty = person.getPersonIsFemale();
        	maleInParty = femaleInParty == 0 ? 1 : 0;
        } else {
    		femaleInParty = 0;
    		maleInParty = 0;
        	for (int participant : tour.getPersonNumArray()) {
        		if (hh.getPerson(participant).getPersonIsFemale() == 1)
        			femaleInParty = 1;
        		else
        			maleInParty = 1;
        	}
        }
	}
	
	public void setBikeLogsum(int origin, int dest, boolean inbound) {
		boolean mandatory = tour.getTourPrimaryPurposeIndex() <= 3;
		femaleBikeLogsum = bls.getLogsum(new BikeLogsumSegment(true,mandatory,inbound),origin,dest);
		maleBikeLogsum = bls.getLogsum(new BikeLogsumSegment(false,mandatory,inbound),origin,dest);
	}
    
    public double getFemaleBikeLogsum() {
		return femaleBikeLogsum;
	}
    
    public double getMaleBikeLogsum() {
		return maleBikeLogsum;
	}
    
    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getAutos", 1);
        methodIndexMap.put("getAdults", 2);
        methodIndexMap.put("getHhSize", 3);
        methodIndexMap.put("getFemale", 4);
        methodIndexMap.put("getIncomeCategory", 5);
        methodIndexMap.put("getTimeOutbound", 6);
        methodIndexMap.put("getTimeInbound", 7);
        methodIndexMap.put("getTimeTrip", 8);
        methodIndexMap.put("getTourCategoryJoint", 9);
        methodIndexMap.put("getNumberOfParticipantsInJointTour", 10);
        methodIndexMap.put("getOutboundStops", 11);
        methodIndexMap.put("getReturnStops", 12);
        methodIndexMap.put("getFirstTrip", 13);
        methodIndexMap.put("getLastTrip", 14);
        methodIndexMap.put("getTourModeIsDA", 15);
        methodIndexMap.put("getTourModeIsS2", 16);
        methodIndexMap.put("getTourModeIsS3", 17);
        methodIndexMap.put("getTourModeIsWalk", 18);
        methodIndexMap.put("getTourModeIsBike", 19);
        methodIndexMap.put("getTourModeIsWTran", 20);
        methodIndexMap.put("getTourModeIsPNR", 21);
        methodIndexMap.put("getTourModeIsKNR", 22);
        methodIndexMap.put("getODUDen", 23);
        methodIndexMap.put("getOEmpDen", 24);
        methodIndexMap.put("getOTotInt", 25);
        methodIndexMap.put("getDDUDen", 26);
        methodIndexMap.put("getDEmpDen", 27);
        methodIndexMap.put("getDTotInt", 28);
        methodIndexMap.put("getPTazTerminalTime", 30);
        methodIndexMap.put("getATazTerminalTime", 31);
        methodIndexMap.put("getAge", 32);
        methodIndexMap.put("getTourModeIsSchBus", 33);
        methodIndexMap.put("getEscortTour", 34);
        methodIndexMap.put("getAutoModeAllowedForTripSegment", 35);
        methodIndexMap.put("getWalkModeAllowedForTripSegment", 36);
        methodIndexMap.put("getSegmentIsIk", 37);
        methodIndexMap.put("getReimburseAmount", 38);
        methodIndexMap.put("getMonthlyParkingCostTourDest", 39);
        methodIndexMap.put("getDailyParkingCostTourDest", 40);
        methodIndexMap.put("getHourlyParkingCostTourDest", 41);
        methodIndexMap.put("getHourlyParkingCostTripOrig", 42);
        methodIndexMap.put("getHourlyParkingCostTripDest", 43);
        methodIndexMap.put("getTripOrigIsTourDest", 44);
        methodIndexMap.put("getTripDestIsTourDest", 45);
        methodIndexMap.put("getFreeOnsite", 46);
        methodIndexMap.put("getPersonType", 47);
        
        methodIndexMap.put("getFemaleInParty", 50);
        methodIndexMap.put("getMaleInParty", 51);
        methodIndexMap.put("getFemaleBikeLogsum", 52);
        methodIndexMap.put("getMaleBikeLogsum", 53);
        methodIndexMap.put("getPersonIsTourDriver",54);
        
        methodIndexMap.put("getTransponderOwnership", 54);
        methodIndexMap.put("getWorkTimeFactor", 55);
        methodIndexMap.put("getNonWorkTimeFactor", 56);
        methodIndexMap.put("getJointTourTimeFactor", 57);
        
        methodIndexMap.put("getInbound",58);
        
        methodIndexMap.put("getIncomeInDollars",59);
        methodIndexMap.put("getIvtCoeff", 60);
        methodIndexMap.put("getCostCoeff", 61);
               
        methodIndexMap.put("getWalkSetLogSum", 62);
        methodIndexMap.put("getPnrSetLogSum", 63);
        methodIndexMap.put("getKnrSetLogSum", 64);

        methodIndexMap.put("getWaitTimeTaxi", 70);
        methodIndexMap.put("getWaitTimeSingleTNC", 71);
        methodIndexMap.put("getWaitTimeSharedTNC", 72);
        methodIndexMap.put("getUseOwnedAV", 73);

        methodIndexMap.put("getNm_walkTime", 90);
        methodIndexMap.put("getNm_bikeTime", 91);
        
        methodIndexMap.put("getOriginMgra", 93);
        methodIndexMap.put("getDestMgra", 94);

   }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;

        switch (variableIndex)
        {

            case 1:
                returnValue = getAutos();
                break;
            case 2:
                returnValue = getAdults();
                break;
            case 3:
                returnValue = getHhSize();
                break;
            case 4:
                returnValue = getFemale();
                break;
            case 5:
                returnValue = getIncome();
                break;
            case 6:
                returnValue = getTimeOutbound();
                break;
            case 7:
                returnValue = getTimeInbound();
                break;
            case 8:
                returnValue = getTimeTrip();
                break;
            case 9:
                returnValue = getTourCategoryJoint();
                break;
            case 10:
                returnValue = getNumberOfParticipantsInJointTour();
                break;
            case 11:
                returnValue = getOutboundStops();
                break;
            case 12:
                returnValue = getReturnStops();
                break;
            case 13:
                returnValue = getFirstTrip();
                break;
            case 14:
                returnValue = getLastTrip();
                break;
            case 15:
                returnValue = getTourModeIsDA();
                break;
            case 16:
                returnValue = getTourModeIsS2();
                break;
            case 17:
                returnValue = getTourModeIsS3();
                break;
            case 18:
                returnValue = getTourModeIsWalk();
                break;
            case 19:
                returnValue = getTourModeIsBike();
                break;
            case 20:
                returnValue = getTourModeIsWTran();
                break;
            case 21:
                returnValue = getTourModeIsPnr();
                break;
            case 22:
                returnValue = getTourModeIsKnr();
                break;
            case 23:
                returnValue = getODUDen();
                break;
            case 24:
                returnValue = getOEmpDen();
                break;
            case 25:
                returnValue = getOTotInt();
                break;
            case 26:
                returnValue = getDDUDen();
                break;
            case 27:
                returnValue = getDEmpDen();
                break;
            case 28:
                returnValue = getDTotInt();
                break;
            case 30:
                returnValue = getPTazTerminalTime();
                break;
            case 31:
                returnValue = getATazTerminalTime();
                break;
            case 32:
                returnValue = getAge();
                break;
            case 33:
                returnValue = getTourModeIsSchBus();
                break;
            case 34:
                returnValue = getEscortTour();
                break;
            case 35:
                returnValue = getAutoModeAllowedForTripSegment();
                break;
            case 36:
                returnValue = getWalkModeAllowedForTripSegment();
                break;
            case 37:
                returnValue = getSegmentIsIk();
                break;
            case 38:
                returnValue = getReimburseAmount();
                break;
            case 39:
                returnValue = getMonthlyParkingCostTourDest();
                break;
            case 40:
                returnValue = getDailyParkingCostTourDest();
                break;
            case 41:
                returnValue = getHourlyParkingCostTourDest();
                break;
            case 42:
                returnValue = getHourlyParkingCostTripOrig();
                break;
            case 43:
                returnValue = getHourlyParkingCostTripDest();
                break;
            case 44:
                returnValue = getTripOrigIsTourDest();
                break;
            case 45:
                returnValue = getTripDestIsTourDest();
                break;
            case 46:
                returnValue = getFreeOnsite();
                break;                
            case 47:
                returnValue = getPersonType();
                break;                
            case 50:
            	returnValue = getFemaleInParty();
                break;
            case 51:
            	returnValue = getMaleInParty();
                break;
            case 52:
            	returnValue = getFemaleBikeLogsum();
                break;
            case 53:
            	returnValue = getMaleBikeLogsum();
                break;
            case 54:
            	returnValue = getTransponderOwnership();
                break;
            case 55:
            	returnValue = getWorkTimeFactor();
                break;
            case 56:
            	returnValue = getNonWorkTimeFactor();
                break;
            case 57:
            	returnValue = getJointTourTimeFactor();
                break;
            case 58:
            	returnValue = getInbound();
                break;
            case 59:
            	returnValue = getIncomeInDollars();
                break;
            case 60:
            	returnValue = getIvtCoeff();
                break;
            case 61:
            	returnValue = getCostCoeff();
                break;
            case 62:
                returnValue = getTransitLogSum(WTW);
                break;
            case 63:
            	if ( outboundHalfTourDirection == 1 )
                    returnValue = getTransitLogSum(DTW);
                else
                    returnValue = getTransitLogSum(WTD);
                break;
            case 64:
            	if ( outboundHalfTourDirection == 1 )
                    returnValue = getTransitLogSum(DTW);
                else
                    returnValue = getTransitLogSum(WTD);
                break;
            case 70: return getWaitTimeTaxi();
            case 71: return getWaitTimeSingleTNC();
            case 72: return getWaitTimeSharedTNC();
            case 73: return getUseOwnedAV();
            case 90:
            	returnValue = getNm_walkTime();
            	break;
            case 91:
            	returnValue = getNm_bikeTime();
                break;
            case 93:
            	returnValue = getOriginMgra();
            	break;
            case 94:
            	returnValue = getDestMgra();
                break;
            default:
                logger.error( "method number = " + variableIndex + " not found" );
                throw new RuntimeException( "method number = " + variableIndex + " not found" );
        }
        return returnValue;
    }
}