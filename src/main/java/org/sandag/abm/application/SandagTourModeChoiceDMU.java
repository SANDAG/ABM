package org.sandag.abm.application;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.BikeLogsumSegment;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;


public class SandagTourModeChoiceDMU
        extends TourModeChoiceDMU
{
    
	private int setPersonHhTourCounter = 0;
	private BikeLogsum bls;
    protected double inboundFemaleBikeLogsum;
    protected double outboundFemaleBikeLogsum;
    protected double inboundMaleBikeLogsum;
    protected double outboundMaleBikeLogsum;
    protected double femaleInParty;
    protected double maleInParty;

    public SandagTourModeChoiceDMU(ModelStructure modelStructure, Logger aLogger)
    {
        super(modelStructure, aLogger);
        setupMethodIndexMap();
    }

    public float getTimeOutbound()
    {
        return tour.getTourDepartPeriod();
    }

    public float getTimeInbound()
    {
        return tour.getTourArrivePeriod();
    }

    public int getIncome()
    {
        return hh.getIncomeInDollars();
    }

    public int getAdults()
    {
        return hh.getNumPersons18plus();
    }

    public int getFemale()
    {
        return person.getPersonIsFemale();
    }

    public void setOrigDuDen(double arg)
    {
        origDuDen = arg;
    }

    public void setOrigEmpDen(double arg)
    {
        origEmpDen = arg;
    }

    public void setOrigTotInt(double arg)
    {
        origTotInt = arg;
    }

    public void setDestDuDen(double arg)
    {
        destDuDen = arg;
    }

    public void setDestEmpDen(double arg)
    {
        destEmpDen = arg;
    }

    public void setDestTotInt(double arg)
    {
        destTotInt = arg;
    }

    public double getODUDen()
    {
        return origDuDen;
    }

    public double getOEmpDen()
    {
        return origEmpDen;
    }

    public double getOTotInt()
    {
        return origTotInt;
    }

    public double getDDUDen()
    {
        return destDuDen;
    }

    public double getDEmpDen()
    {
        return destEmpDen;
    }

    public double getDTotInt()
    {
        return destTotInt;
    }

    public double getNm_walkTime_out()
    {
        return getNmWalkTimeOut();
    }

    public double getNm_walkTime_in()
    {
        return getNmWalkTimeIn();
    }

    public double getNm_bikeTime_out()
    {
        return getNmBikeTimeOut();
    }

    public double getNm_bikeTime_in()
    {
        return getNmBikeTimeIn();
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
    		setBikeLogsum();
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
    
    public double getInboundFemaleBikeLogsum() 
    {
		return inboundFemaleBikeLogsum;
	}
    
    public double getOutboundFemaleBikeLogsum() 
    {
		return outboundFemaleBikeLogsum;
	}
    
    public double getInboundMaleBikeLogsum() 
    {
		return inboundMaleBikeLogsum;
	}
    
    public double getOutboundMaleBikeLogsum() 
    {
		return outboundMaleBikeLogsum;
	}


	private void setBikeLogsum(double inboundFemaleBikeLogsum, double outboundFemaleBikeLogsum,
			                   double inboundMaleBikeLogsum  , double outboundMaleBikeLogsum) 
	{
		this.inboundFemaleBikeLogsum = inboundFemaleBikeLogsum;
		this.outboundFemaleBikeLogsum = outboundFemaleBikeLogsum;
		this.inboundMaleBikeLogsum = inboundMaleBikeLogsum;
		this.outboundMaleBikeLogsum = outboundMaleBikeLogsum;
	}
	
	private void setBikeLogsum() 
	{
		int origin = tour.getTourOrigMgra();
		int dest = tour.getTourDestMgra();
		boolean mandatory = tour.getTourPrimaryPurposeIndex() <= 3;
		setBikeLogsum(bls.getLogsum(new BikeLogsumSegment(true,mandatory,true),dest,origin),
				      bls.getLogsum(new BikeLogsumSegment(true,mandatory,false),origin,dest),
				      bls.getLogsum(new BikeLogsumSegment(false,mandatory,true),dest,origin),
			          bls.getLogsum(new BikeLogsumSegment(false,mandatory,false),origin,dest));
	}

  	private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTimeOutbound", 0);
        methodIndexMap.put("getTimeInbound", 1);
        methodIndexMap.put("getIncomeCategory", 2);
        methodIndexMap.put("getAdults", 3);
        methodIndexMap.put("getFemale", 4);
        methodIndexMap.put("getHhSize", 5);
        methodIndexMap.put("getAutos", 6);
        methodIndexMap.put("getAge", 7);
        methodIndexMap.put("getTourCategoryJoint", 8);
        methodIndexMap.put("getNumberOfParticipantsInJointTour", 9);
        methodIndexMap.put("getWorkTourModeIsSov", 10);
        methodIndexMap.put("getWorkTourModeIsBike", 11);
        methodIndexMap.put("getWorkTourModeIsHov", 12);
        methodIndexMap.put("getPTazTerminalTime", 14);
        methodIndexMap.put("getATazTerminalTime", 15);
        methodIndexMap.put("getODUDen", 16);
        methodIndexMap.put("getOEmpDen", 17);
        methodIndexMap.put("getOTotInt", 18);
        methodIndexMap.put("getDDUDen", 19);
        methodIndexMap.put("getDEmpDen", 20);
        methodIndexMap.put("getDTotInt", 21);
        methodIndexMap.put("getTourCategoryEscort", 22);
        methodIndexMap.put("getMonthlyParkingCost", 23);
        methodIndexMap.put("getDailyParkingCost", 24);
        methodIndexMap.put("getHourlyParkingCost", 25);
        methodIndexMap.put("getReimburseProportion", 26);
        methodIndexMap.put("getPersonType", 27);
        methodIndexMap.put("getFreeParkingEligibility", 28);
        methodIndexMap.put("getParkingArea", 29);
        
        methodIndexMap.put("getWorkTimeFactor", 30);
        methodIndexMap.put("getNonWorkTimeFactor", 31);
        methodIndexMap.put("getJointTourTimeFactor", 32);
        methodIndexMap.put("getTransponderOwnership", 33);
        
        methodIndexMap.put("getFemaleInParty", 50);
        methodIndexMap.put("getMaleInParty", 51);
        methodIndexMap.put("getInboundFemaleBikeLogsum", 52);
        methodIndexMap.put("getOutboundFemaleBikeLogsum", 53);
        methodIndexMap.put("getInboundMaleBikeLogsum", 54);
        methodIndexMap.put("getOutboundMaleBikeLogsum", 55);

        methodIndexMap.put("getIvtCoeff", 56);
        methodIndexMap.put("getCostCoeff", 57);
        methodIndexMap.put("getIncomeInDollars", 58);
        methodIndexMap.put("getWalkSetLogSum", 59);
        methodIndexMap.put("getPnrSetLogSum", 60);
        methodIndexMap.put("getKnrSetLogSum", 61);
                
        methodIndexMap.put("getNm_walkTime_out", 90);
        methodIndexMap.put("getNm_walkTime_in", 91);
        methodIndexMap.put("getNm_bikeTime_out", 92);
        methodIndexMap.put("getNm_bikeTime_in", 93);
        
        methodIndexMap.put("getOriginMgra", 96);
        methodIndexMap.put("getDestMgra", 97);
        
        


    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;

        switch (variableIndex)
        {

            case 0:
                returnValue = getTimeOutbound();
                break;
            case 1:
                returnValue = getTimeInbound();
                break;
            case 2:
                returnValue = getIncomeCategory();
                break;
            case 3:
                returnValue = getAdults();
                break;
            case 4:
                returnValue = getFemale();
                break;
            case 5:
                returnValue = getHhSize();
                break;
            case 6:
                returnValue = getAutos();
                break;
            case 7:
                returnValue = getAge();
                break;
            case 8:
                returnValue = getTourCategoryJoint();
                break;
            case 9:
                returnValue = getNumberOfParticipantsInJointTour();
                break;
            case 10:
                returnValue = getWorkTourModeIsSov();
                break;
            case 11:
                returnValue = getWorkTourModeIsBike();
                break;
            case 12:
                returnValue = getWorkTourModeIsHov();
                break;
            case 14:
                returnValue = getPTazTerminalTime();
                break;
            case 15:
                returnValue = getATazTerminalTime();
                break;
            case 16:
                returnValue = getODUDen();
                break;
            case 17:
                returnValue = getOEmpDen();
                break;
            case 18:
                returnValue = getOTotInt();
                break;
            case 19:
                returnValue = getDDUDen();
                break;
            case 20:
                returnValue = getDEmpDen();
                break;
            case 21:
                returnValue = getDTotInt();
                break;
            case 22:
                returnValue = getTourCategoryEscort();
                break;
            case 23:
                returnValue = getMonthlyParkingCost();
                break;
            case 24:
                returnValue = getDailyParkingCost();
                break;
            case 25:
                returnValue = getHourlyParkingCost();
                break;
            case 26:
                returnValue = getReimburseProportion();
                break;
            case 27:
                returnValue = getPersonType();
                break;
            case 28:
                returnValue = getFreeParkingEligibility();
                break;
            case 29:
                returnValue = getParkingArea();
                break;
            case 30:
                getWorkTimeFactor();
                break;
            case 31:
            	getNonWorkTimeFactor();
            	break;
            case 32:
            	getJointTourTimeFactor();
            	break;
            case 33:
            	getTransponderOwnership();
            	break;
            case 50:    
                getFemaleInParty();
                break;
            case 51:
            	getMaleInParty();
            	break;
            case 52:
            	getInboundFemaleBikeLogsum();
            	break;
            case 53:
            	getOutboundFemaleBikeLogsum();
            	break;
            case 54:
            	getInboundMaleBikeLogsum();
            	break;
            case 55:
            	getOutboundMaleBikeLogsum();
            	break;
            case 56:
            	getIvtCoeff();
            	break;
            case 57:
            	getCostCoeff();
            	break;
            case 58:    
            	getIncomeInDollars();
            	break;
            case 59:
                returnValue = getTransitLogSum(WTW, true) + getTransitLogSum(WTW, false);
                break;
            case 60:
                returnValue = getTransitLogSum(WTD, true) + getTransitLogSum(DTW, false);
                break;
            case 61:
                returnValue = getTransitLogSum(WTD, true) + getTransitLogSum(DTW, false);
                break;
            case 90:
                returnValue = getNmWalkTimeOut();
                break;
            case 91:
                returnValue = getNmWalkTimeIn();
                break;
            case 92:
                returnValue = getNmBikeTimeOut();
                break;
            case 93:
                returnValue = getNmBikeTimeIn();
                break;   
            case 96:
                getOriginMgra();
                break;
            case 97:
                getDestMgra();
                break;
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");
        }

        return returnValue;

    }
}