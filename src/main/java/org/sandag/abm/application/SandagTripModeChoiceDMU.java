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

    public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, double value)
    {
        transitSkim[accEgr][lbPrem][skimIndex] = value;
    }

    public double getTransitSkim(int accEgr, int lbPrem, int skimIndex)
    {
        return transitSkim[accEgr][lbPrem][skimIndex];
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
        methodIndexMap.put("getIncome", 5);
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
        
        methodIndexMap.put("getWorkTimeFactor", 55);
        methodIndexMap.put("getNonWorkTimeFactor", 56);
        methodIndexMap.put("getJointTourTimeFactor", 57);
        
        methodIndexMap.put("getInbound",58);

        
        methodIndexMap.put("getNm_walkTime", 90);
        methodIndexMap.put("getNm_bikeTime", 91);
        
        methodIndexMap.put("getOriginMgra", 93);
        methodIndexMap.put("getDestMgra", 94);

        methodIndexMap.put("getWtw_lb_LB_ivt", 100);
        methodIndexMap.put("getWtw_lb_fwait", 101);
        methodIndexMap.put("getWtw_lb_xwait", 102);
        methodIndexMap.put("getWtw_lb_AccTime", 103);
        methodIndexMap.put("getWtw_lb_EgrTime", 104);
        methodIndexMap.put("getWtw_lb_WalkAuxTime", 105);
        methodIndexMap.put("getWtw_lb_fare", 106);
        methodIndexMap.put("getWtw_lb_xfers", 107);
        methodIndexMap.put("getWtw_eb_LB_ivt", 108);
        methodIndexMap.put("getWtw_eb_EB_ivt", 109);
        methodIndexMap.put("getWtw_eb_fwait", 110);
        methodIndexMap.put("getWtw_eb_xwait", 111);
        methodIndexMap.put("getWtw_eb_AccTime", 112);
        methodIndexMap.put("getWtw_eb_EgrTime", 113);
        methodIndexMap.put("getWtw_eb_WalkAuxTime", 114);
        methodIndexMap.put("getWtw_eb_fare", 115);
        methodIndexMap.put("getWtw_eb_xfers", 116);
        methodIndexMap.put("getWtw_brt_LB_ivt", 117);
        methodIndexMap.put("getWtw_brt_EB_ivt", 118);
        methodIndexMap.put("getWtw_brt_BRT_ivt", 119);
        methodIndexMap.put("getWtw_brt_fwait", 120);
        methodIndexMap.put("getWtw_brt_xwait", 121);
        methodIndexMap.put("getWtw_brt_AccTime", 122);
        methodIndexMap.put("getWtw_brt_EgrTime", 123);
        methodIndexMap.put("getWtw_brt_WalkAuxTime", 124);
        methodIndexMap.put("getWtw_brt_fare", 125);
        methodIndexMap.put("getWtw_brt_xfers", 126);
        methodIndexMap.put("getWtw_lr_LB_ivt", 127);
        methodIndexMap.put("getWtw_lr_EB_ivt", 128);
        methodIndexMap.put("getWtw_lr_BRT_ivt", 129);
        methodIndexMap.put("getWtw_lr_LRT_ivt", 130);
        methodIndexMap.put("getWtw_lr_fwait", 131);
        methodIndexMap.put("getWtw_lr_xwait", 132);
        methodIndexMap.put("getWtw_lr_AccTime", 133);
        methodIndexMap.put("getWtw_lr_EgrTime", 134);
        methodIndexMap.put("getWtw_lr_WalkAuxTime", 135);
        methodIndexMap.put("getWtw_lr_fare", 136);
        methodIndexMap.put("getWtw_lr_xfers", 137);
        methodIndexMap.put("getWtw_cr_LB_ivt", 138);
        methodIndexMap.put("getWtw_cr_EB_ivt", 139);
        methodIndexMap.put("getWtw_cr_BRT_ivt", 140);
        methodIndexMap.put("getWtw_cr_LRT_ivt", 141);
        methodIndexMap.put("getWtw_cr_CR_ivt", 142);
        methodIndexMap.put("getWtw_cr_fwait", 143);
        methodIndexMap.put("getWtw_cr_xwait", 144);
        methodIndexMap.put("getWtw_cr_AccTime", 145);
        methodIndexMap.put("getWtw_cr_EgrTime", 146);
        methodIndexMap.put("getWtw_cr_WalkAuxTime", 147);
        methodIndexMap.put("getWtw_cr_fare", 148);
        methodIndexMap.put("getWtw_cr_xfers", 149);

        methodIndexMap.put("getDt_lb_LB_ivt", 150);
        methodIndexMap.put("getDt_lb_fwait", 151);
        methodIndexMap.put("getDt_lb_xwait", 152);
        methodIndexMap.put("getDt_lb_AccTime", 153);
        methodIndexMap.put("getDt_lb_EgrTime", 154);
        methodIndexMap.put("getDt_lb_DrvTime", 155);
        methodIndexMap.put("getDt_lb_WalkAuxTime", 156);
        methodIndexMap.put("getDt_lb_fare", 157);
        methodIndexMap.put("getDt_lb_xfers", 158);
        methodIndexMap.put("getDt_eb_LB_ivt", 159);
        methodIndexMap.put("getDt_eb_EB_ivt", 160);
        methodIndexMap.put("getDt_eb_fwait", 161);
        methodIndexMap.put("getDt_eb_xwait", 162);
        methodIndexMap.put("getDt_eb_AccTime", 163);
        methodIndexMap.put("getDt_eb_EgrTime", 164);
        methodIndexMap.put("getDt_eb_DrvTime", 165);
        methodIndexMap.put("getDt_eb_WalkAuxTime", 166);
        methodIndexMap.put("getDt_eb_fare", 167);
        methodIndexMap.put("getDt_eb_xfers", 168);
        methodIndexMap.put("getDt_brt_LB_ivt", 169);
        methodIndexMap.put("getDt_brt_EB_ivt", 170);
        methodIndexMap.put("getDt_brt_BRT_ivt", 171);
        methodIndexMap.put("getDt_brt_fwait", 172);
        methodIndexMap.put("getDt_brt_xwait", 173);
        methodIndexMap.put("getDt_brt_AccTime", 174);
        methodIndexMap.put("getDt_brt_EgrTime", 175);
        methodIndexMap.put("getDt_brt_DrvTime", 176);
        methodIndexMap.put("getDt_brt_WalkAuxTime", 177);
        methodIndexMap.put("getDt_brt_fare", 178);
        methodIndexMap.put("getDt_brt_xfers", 179);
        methodIndexMap.put("getDt_lr_LB_ivt", 180);
        methodIndexMap.put("getDt_lr_EB_ivt", 181);
        methodIndexMap.put("getDt_lr_BRT_ivt", 182);
        methodIndexMap.put("getDt_lr_LRT_ivt", 183);
        methodIndexMap.put("getDt_lr_fwait", 184);
        methodIndexMap.put("getDt_lr_xwait", 185);
        methodIndexMap.put("getDt_lr_AccTime", 186);
        methodIndexMap.put("getDt_lr_EgrTime", 187);
        methodIndexMap.put("getDt_lr_DrvTime", 188);
        methodIndexMap.put("getDt_lr_WalkAuxTime", 189);
        methodIndexMap.put("getDt_lr_fare", 190);
        methodIndexMap.put("getDt_lr_xfers", 191);
        methodIndexMap.put("getDt_cr_LB_ivt", 192);
        methodIndexMap.put("getDt_cr_EB_ivt", 193);
        methodIndexMap.put("getDt_cr_BRT_ivt", 194);
        methodIndexMap.put("getDt_cr_LRT_ivt", 195);
        methodIndexMap.put("getDt_cr_CR_ivt", 196);
        methodIndexMap.put("getDt_cr_fwait", 197);
        methodIndexMap.put("getDt_cr_xwait", 198);
        methodIndexMap.put("getDt_cr_AccTime", 199);
        methodIndexMap.put("getDt_cr_EgrTime", 200);
        methodIndexMap.put("getDt_cr_DrvTime", 201);
        methodIndexMap.put("getDt_cr_WalkAuxTime", 202);
        methodIndexMap.put("getDt_cr_fare", 203);
        methodIndexMap.put("getDt_cr_xfers", 204);

        CreateReverseMap();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
    	return getValueForIndexLookup(variableIndex, arrayIndex);
    }

}