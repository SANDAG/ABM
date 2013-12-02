package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.TourModeChoiceDMU;

import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.BikeLogsumSegment;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;
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
    public SandagTripModeChoiceDMU(ModelStructure modelStructure)
    {
        super(modelStructure);
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

    public int getJointTour()
    {
        return jointTour;
    }

    public int getPartySize()
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

    public int getDepartPeriod()
    {
        return departPeriod;
    }

    public int getArrivePeriod()
    {
        return arrivePeriod;
    }

    public int getTripPeriod()
    {
        return tripPeriod;
    }

    public int getOutboundStops()
    {
        return outboundStops;
    }

    public int getInboundStops()
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

    public int getTourModeIsPnr()
    {
        return tourModeIsPnr;
    }

    public int getTourModeIsKnr()
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
        	for (int participant : tour.getPersonNumArray()) {
        		femaleInParty = 0;
        		maleInParty = 0;
        		if (hh.getPerson(participant).getPersonIsFemale() == 1)
        			femaleInParty = 1;
        		else
        			maleInParty = 1;
        	}
        }
	}
	
	public void setBikeLogsum(int origin, int dest, boolean inbound) {
		boolean mandatory = tour.getTourPrimaryPurposeIndex() <= 3;
		femaleBikeLogsum = bls.getValue(new BikeLogsumSegment(true,mandatory,inbound),origin,dest);
		maleBikeLogsum = bls.getValue(new BikeLogsumSegment(false,mandatory,inbound),origin,dest);
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
        methodIndexMap.put("getNm_walkTime", 90);
        methodIndexMap.put("getNm_bikeTime", 91);

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
        
        methodIndexMap.put("getFemaleInParty", 205);
        methodIndexMap.put("getMaleInParty", 206);
        methodIndexMap.put("getFemaleBikeLogsum", 207);
        methodIndexMap.put("getMaleBikeLogsum", 208);

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
                returnValue = getDepartPeriod();
                break;
            case 7:
                returnValue = getArrivePeriod();
                break;
            case 8:
                returnValue = getTripPeriod();
                break;
            case 9:
                returnValue = getJointTour();
                break;
            case 10:
                returnValue = getPartySize();
                break;
            case 11:
                returnValue = getOutboundStops();
                break;
            case 12:
                returnValue = getInboundStops();
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
            case 90:
                returnValue = getNmWalkTime();
                break;
            case 91:
                returnValue = getNmBikeTime();
                break;
            case 100:
                returnValue = getTransitSkim(WTW, LB, LB_IVT);
                break;
            case 101:
                returnValue = getTransitSkim(WTW, LB, FWAIT);
                break;
            case 102:
                returnValue = getTransitSkim(WTW, LB, XWAIT);
                break;
            case 103:
                returnValue = getTransitSkim(WTW, LB, ACC);
                break;
            case 104:
                returnValue = getTransitSkim(WTW, LB, EGR);
                break;
            case 105:
                returnValue = getTransitSkim(WTW, LB, AUX);
                break;
            case 106:
                returnValue = getTransitSkim(WTW, LB, FARE);
                break;
            case 107:
                returnValue = getTransitSkim(WTW, LB, XFERS);
                break;
            case 108:
                returnValue = getTransitSkim(WTW, EB, LB_IVT);
                break;
            case 109:
                returnValue = getTransitSkim(WTW, EB, EB_IVT);
                break;
            case 110:
                returnValue = getTransitSkim(WTW, EB, FWAIT);
                break;
            case 111:
                returnValue = getTransitSkim(WTW, EB, XWAIT);
                break;
            case 112:
                returnValue = getTransitSkim(WTW, EB, ACC);
                break;
            case 113:
                returnValue = getTransitSkim(WTW, EB, EGR);
                break;
            case 114:
                returnValue = getTransitSkim(WTW, EB, AUX);
                break;
            case 115:
                returnValue = getTransitSkim(WTW, EB, FARE);
                break;
            case 116:
                returnValue = getTransitSkim(WTW, EB, XFERS);
                break;
            case 117:
                returnValue = getTransitSkim(WTW, BRT, LB_IVT);
                break;
            case 118:
                returnValue = getTransitSkim(WTW, BRT, EB_IVT);
                break;
            case 119:
                returnValue = getTransitSkim(WTW, BRT, BRT_IVT);
                break;
            case 120:
                returnValue = getTransitSkim(WTW, BRT, FWAIT);
                break;
            case 121:
                returnValue = getTransitSkim(WTW, BRT, XWAIT);
                break;
            case 122:
                returnValue = getTransitSkim(WTW, BRT, ACC);
                break;
            case 123:
                returnValue = getTransitSkim(WTW, BRT, EGR);
                break;
            case 124:
                returnValue = getTransitSkim(WTW, BRT, AUX);
                break;
            case 125:
                returnValue = getTransitSkim(WTW, BRT, FARE);
                break;
            case 126:
                returnValue = getTransitSkim(WTW, BRT, XFERS);
                break;
            case 127:
                returnValue = getTransitSkim(WTW, LR, LB_IVT);
                break;
            case 128:
                returnValue = getTransitSkim(WTW, LR, EB_IVT);
                break;
            case 129:
                returnValue = getTransitSkim(WTW, LR, BRT_IVT);
                break;
            case 130:
                returnValue = getTransitSkim(WTW, LR, LR_IVT);
                break;
            case 131:
                returnValue = getTransitSkim(WTW, LR, FWAIT);
                break;
            case 132:
                returnValue = getTransitSkim(WTW, LR, XWAIT);
                break;
            case 133:
                returnValue = getTransitSkim(WTW, LR, ACC);
                break;
            case 134:
                returnValue = getTransitSkim(WTW, LR, EGR);
                break;
            case 135:
                returnValue = getTransitSkim(WTW, LR, AUX);
                break;
            case 136:
                returnValue = getTransitSkim(WTW, LR, FARE);
                break;
            case 137:
                returnValue = getTransitSkim(WTW, LR, XFERS);
                break;
            case 138:
                returnValue = getTransitSkim(WTW, CR, LB_IVT);
                break;
            case 139:
                returnValue = getTransitSkim(WTW, CR, EB_IVT);
                break;
            case 140:
                returnValue = getTransitSkim(WTW, CR, BRT_IVT);
                break;
            case 141:
                returnValue = getTransitSkim(WTW, CR, LR_IVT);
                break;
            case 142:
                returnValue = getTransitSkim(WTW, CR, CR_IVT);
                break;
            case 143:
                returnValue = getTransitSkim(WTW, CR, FWAIT);
                break;
            case 144:
                returnValue = getTransitSkim(WTW, CR, XWAIT);
                break;
            case 145:
                returnValue = getTransitSkim(WTW, CR, ACC);
                break;
            case 146:
                returnValue = getTransitSkim(WTW, CR, EGR);
                break;
            case 147:
                returnValue = getTransitSkim(WTW, CR, AUX);
                break;
            case 148:
                returnValue = getTransitSkim(WTW, CR, FARE);
                break;
            case 149:
                returnValue = getTransitSkim(WTW, CR, XFERS);
                break;
            case 150:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, LB_IVT);
                else returnValue = getTransitSkim(WTD, LB, LB_IVT);
                break;
            case 151:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, FWAIT);
                else returnValue = getTransitSkim(WTD, LB, FWAIT);
                break;
            case 152:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, XWAIT);
                else returnValue = getTransitSkim(WTD, LB, XWAIT);
                break;
            case 153:
                if (outboundHalfTourDirection == 1) returnValue = 0;
                else returnValue = getTransitSkim(WTD, LB, ACC);
                break;
            case 154:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, EGR);
                else returnValue = 0;
                break;
            case 155:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, ACC);
                else returnValue = getTransitSkim(WTD, LB, EGR);
                break;
            case 156:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, AUX);
                else returnValue = getTransitSkim(WTD, LB, AUX);
                break;
            case 157:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, FARE);
                else returnValue = getTransitSkim(WTD, LB, FARE);
                break;
            case 158:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LB, XFERS);
                else returnValue = getTransitSkim(WTD, LB, XFERS);
                break;
            case 159:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, LB_IVT);
                else returnValue = getTransitSkim(WTD, EB, LB_IVT);
                break;
            case 160:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, EB_IVT);
                else returnValue = getTransitSkim(WTD, EB, EB_IVT);
                break;
            case 161:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, FWAIT);
                else returnValue = getTransitSkim(WTD, EB, FWAIT);
                break;
            case 162:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, XWAIT);
                else returnValue = getTransitSkim(WTD, EB, XWAIT);
                break;
            case 163:
                if (outboundHalfTourDirection == 1) returnValue = 0;
                else returnValue = getTransitSkim(WTD, EB, ACC);
                break;
            case 164:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, EGR);
                else returnValue = 0;
                break;
            case 165:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, ACC);
                else returnValue = getTransitSkim(WTD, EB, EGR);
                break;
            case 166:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, AUX);
                else returnValue = getTransitSkim(WTD, EB, AUX);
                break;
            case 167:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, FARE);
                else returnValue = getTransitSkim(WTD, EB, FARE);
                break;
            case 168:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, EB, XFERS);
                else returnValue = getTransitSkim(WTD, EB, XFERS);
                break;
            case 169:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, LB_IVT);
                else returnValue = getTransitSkim(WTD, BRT, LB_IVT);
                break;
            case 170:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, EB_IVT);
                else returnValue = getTransitSkim(WTD, BRT, EB_IVT);
                break;
            case 171:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, BRT_IVT);
                else returnValue = getTransitSkim(WTD, BRT, BRT_IVT);
                break;
            case 172:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, FWAIT);
                else returnValue = getTransitSkim(WTD, BRT, FWAIT);
                break;
            case 173:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, XWAIT);
                else returnValue = getTransitSkim(WTD, BRT, XWAIT);
                break;
            case 174:
                if (outboundHalfTourDirection == 1) returnValue = 0;
                else returnValue = getTransitSkim(WTD, BRT, ACC);
                break;
            case 175:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, EGR);
                else returnValue = 0;
                break;
            case 176:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, ACC);
                else returnValue = getTransitSkim(WTD, BRT, EGR);
                break;
            case 177:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, AUX);
                else returnValue = getTransitSkim(WTD, BRT, AUX);
                break;
            case 178:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, FARE);
                else returnValue = getTransitSkim(WTD, BRT, FARE);
                break;
            case 179:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, BRT, XFERS);
                else returnValue = getTransitSkim(WTD, BRT, XFERS);
                break;
            case 180:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, LB_IVT);
                else returnValue = getTransitSkim(WTD, LR, LB_IVT);
                break;
            case 181:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, EB_IVT);
                else returnValue = getTransitSkim(WTD, LR, EB_IVT);
                break;
            case 182:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, BRT_IVT);
                else returnValue = getTransitSkim(WTD, LR, BRT_IVT);
                break;
            case 183:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, LR_IVT);
                else returnValue = getTransitSkim(WTD, LR, LR_IVT);
                break;
            case 184:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, FWAIT);
                else returnValue = getTransitSkim(WTD, LR, FWAIT);
                break;
            case 185:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, XWAIT);
                else returnValue = getTransitSkim(WTD, LR, XWAIT);
                break;
            case 186:
                if (outboundHalfTourDirection == 1) returnValue = 0;
                else returnValue = getTransitSkim(WTD, LR, ACC);
                break;
            case 187:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, EGR);
                else returnValue = 0;
                break;
            case 188:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, ACC);
                else returnValue = getTransitSkim(WTD, LR, EGR);
                break;
            case 189:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, AUX);
                else returnValue = getTransitSkim(WTD, LR, AUX);
                break;
            case 190:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, FARE);
                else returnValue = getTransitSkim(WTD, LR, FARE);
                break;
            case 191:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, LR, XFERS);
                else returnValue = getTransitSkim(WTD, LR, XFERS);
                break;
            case 192:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, LB_IVT);
                else returnValue = getTransitSkim(WTD, CR, LB_IVT);
                break;
            case 193:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, EB_IVT);
                else returnValue = getTransitSkim(WTD, CR, EB_IVT);
                break;
            case 194:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, BRT_IVT);
                else returnValue = getTransitSkim(WTD, CR, BRT_IVT);
                break;
            case 195:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, LR_IVT);
                else returnValue = getTransitSkim(WTD, CR, LR_IVT);
                break;
            case 196:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, CR_IVT);
                else returnValue = getTransitSkim(WTD, CR, CR_IVT);
                break;
            case 197:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, FWAIT);
                else returnValue = getTransitSkim(WTD, CR, FWAIT);
                break;
            case 198:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, XWAIT);
                else returnValue = getTransitSkim(WTD, CR, XWAIT);
                break;
            case 199:
                if (outboundHalfTourDirection == 1) returnValue = 0;
                else returnValue = getTransitSkim(WTD, CR, ACC);
                break;
            case 200:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, EGR);
                else returnValue = 0;
                break;
            case 201:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, ACC);
                else returnValue = getTransitSkim(WTD, CR, EGR);
                break;
            case 202:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, AUX);
                else returnValue = getTransitSkim(WTD, CR, AUX);
                break;
            case 203:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, FARE);
                else returnValue = getTransitSkim(WTD, CR, FARE);
                break;
            case 204:
                if (outboundHalfTourDirection == 1) returnValue = getTransitSkim(DTW, CR, XFERS);
                else returnValue = getTransitSkim(WTD, CR, XFERS);
                break;
            case 205:
            	returnValue = getFemaleInParty();
            	break;
            case 206:
            	returnValue = getMaleInParty();
            	break;
            case 207:
            	returnValue = getFemaleBikeLogsum();
            	break;
            case 208:
            	returnValue = getMaleBikeLogsum();
            	break;

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

        return returnValue;

    }

}