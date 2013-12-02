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
		setBikeLogsum(bls.getValue(new BikeLogsumSegment(true,mandatory,true),origin,dest),
				      bls.getValue(new BikeLogsumSegment(true,mandatory,false),origin,dest),
				      bls.getValue(new BikeLogsumSegment(false,mandatory,true),origin,dest),
			          bls.getValue(new BikeLogsumSegment(false,mandatory,false),origin,dest));
	}

  	private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTimeOutbound", 0);
        methodIndexMap.put("getTimeInbound", 1);
        methodIndexMap.put("getIncome", 2);
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

        methodIndexMap.put("getNm_walkTime_out", 90);
        methodIndexMap.put("getNm_walkTime_in", 91);
        methodIndexMap.put("getNm_bikeTime_out", 92);
        methodIndexMap.put("getNm_bikeTime_in", 93);

        methodIndexMap.put("getWtw_lb_LB_ivt_out", 100);
        methodIndexMap.put("getWtw_lb_LB_ivt_in", 101);
        methodIndexMap.put("getWtw_lb_fwait_out", 102);
        methodIndexMap.put("getWtw_lb_fwait_in", 103);
        methodIndexMap.put("getWtw_lb_xwait_out", 104);
        methodIndexMap.put("getWtw_lb_xwait_in", 105);
        methodIndexMap.put("getWtw_lb_AccTime_out", 106);
        methodIndexMap.put("getWtw_lb_AccTime_in", 107);
        methodIndexMap.put("getWtw_lb_EgrTime_out", 108);
        methodIndexMap.put("getWtw_lb_EgrTime_in", 109);
        methodIndexMap.put("getWtw_lb_WalkAuxTime_out", 110);
        methodIndexMap.put("getWtw_lb_WalkAuxTime_in", 111);
        methodIndexMap.put("getWtw_lb_fare_out", 112);
        methodIndexMap.put("getWtw_lb_fare_in", 113);
        methodIndexMap.put("getWtw_lb_xfers_out", 114);
        methodIndexMap.put("getWtw_lb_xfers_in", 115);
        methodIndexMap.put("getWtw_eb_LB_ivt_out", 116);
        methodIndexMap.put("getWtw_eb_LB_ivt_in", 117);
        methodIndexMap.put("getWtw_eb_EB_ivt_out", 118);
        methodIndexMap.put("getWtw_eb_EB_ivt_in", 119);
        methodIndexMap.put("getWtw_eb_fwait_out", 120);
        methodIndexMap.put("getWtw_eb_fwait_in", 121);
        methodIndexMap.put("getWtw_eb_xwait_out", 122);
        methodIndexMap.put("getWtw_eb_xwait_in", 123);
        methodIndexMap.put("getWtw_eb_AccTime_out", 124);
        methodIndexMap.put("getWtw_eb_AccTime_in", 125);
        methodIndexMap.put("getWtw_eb_EgrTime_out", 126);
        methodIndexMap.put("getWtw_eb_EgrTime_in", 127);
        methodIndexMap.put("getWtw_eb_WalkAuxTime_out", 128);
        methodIndexMap.put("getWtw_eb_WalkAuxTime_in", 129);
        methodIndexMap.put("getWtw_eb_fare_out", 130);
        methodIndexMap.put("getWtw_eb_fare_in", 131);
        methodIndexMap.put("getWtw_eb_xfers_out", 132);
        methodIndexMap.put("getWtw_eb_xfers_in", 133);
        methodIndexMap.put("getWtw_brt_LB_ivt_out", 134);
        methodIndexMap.put("getWtw_brt_LB_ivt_in", 135);
        methodIndexMap.put("getWtw_brt_EB_ivt_out", 136);
        methodIndexMap.put("getWtw_brt_EB_ivt_in", 137);
        methodIndexMap.put("getWtw_brt_BRT_ivt_out", 138);
        methodIndexMap.put("getWtw_brt_BRT_ivt_in", 139);
        methodIndexMap.put("getWtw_brt_fwait_out", 140);
        methodIndexMap.put("getWtw_brt_fwait_in", 141);
        methodIndexMap.put("getWtw_brt_xwait_out", 142);
        methodIndexMap.put("getWtw_brt_xwait_in", 143);
        methodIndexMap.put("getWtw_brt_AccTime_out", 144);
        methodIndexMap.put("getWtw_brt_AccTime_in", 145);
        methodIndexMap.put("getWtw_brt_EgrTime_out", 146);
        methodIndexMap.put("getWtw_brt_EgrTime_in", 147);
        methodIndexMap.put("getWtw_brt_WalkAuxTime_out", 148);
        methodIndexMap.put("getWtw_brt_WalkAuxTime_in", 149);
        methodIndexMap.put("getWtw_brt_fare_out", 150);
        methodIndexMap.put("getWtw_brt_fare_in", 151);
        methodIndexMap.put("getWtw_brt_xfers_out", 152);
        methodIndexMap.put("getWtw_brt_xfers_in", 153);
        methodIndexMap.put("getWtw_lr_LB_ivt_out", 154);
        methodIndexMap.put("getWtw_lr_LB_ivt_in", 155);
        methodIndexMap.put("getWtw_lr_EB_ivt_out", 156);
        methodIndexMap.put("getWtw_lr_EB_ivt_in", 157);
        methodIndexMap.put("getWtw_lr_BRT_ivt_out", 158);
        methodIndexMap.put("getWtw_lr_BRT_ivt_in", 159);
        methodIndexMap.put("getWtw_lr_LRT_ivt_out", 160);
        methodIndexMap.put("getWtw_lr_LRT_ivt_in", 161);
        methodIndexMap.put("getWtw_lr_fwait_out", 162);
        methodIndexMap.put("getWtw_lr_fwait_in", 163);
        methodIndexMap.put("getWtw_lr_xwait_out", 164);
        methodIndexMap.put("getWtw_lr_xwait_in", 165);
        methodIndexMap.put("getWtw_lr_AccTime_out", 166);
        methodIndexMap.put("getWtw_lr_AccTime_in", 167);
        methodIndexMap.put("getWtw_lr_EgrTime_out", 168);
        methodIndexMap.put("getWtw_lr_EgrTime_in", 169);
        methodIndexMap.put("getWtw_lr_WalkAuxTime_out", 170);
        methodIndexMap.put("getWtw_lr_WalkAuxTime_in", 171);
        methodIndexMap.put("getWtw_lr_fare_out", 172);
        methodIndexMap.put("getWtw_lr_fare_in", 173);
        methodIndexMap.put("getWtw_lr_xfers_out", 174);
        methodIndexMap.put("getWtw_lr_xfers_in", 175);
        methodIndexMap.put("getWtw_cr_LB_ivt_out", 176);
        methodIndexMap.put("getWtw_cr_LB_ivt_in", 177);
        methodIndexMap.put("getWtw_cr_EB_ivt_out", 178);
        methodIndexMap.put("getWtw_cr_EB_ivt_in", 179);
        methodIndexMap.put("getWtw_cr_BRT_ivt_out", 180);
        methodIndexMap.put("getWtw_cr_BRT_ivt_in", 181);
        methodIndexMap.put("getWtw_cr_LRT_ivt_out", 182);
        methodIndexMap.put("getWtw_cr_LRT_ivt_in", 183);
        methodIndexMap.put("getWtw_cr_CR_ivt_out", 184);
        methodIndexMap.put("getWtw_cr_CR_ivt_in", 185);
        methodIndexMap.put("getWtw_cr_fwait_out", 186);
        methodIndexMap.put("getWtw_cr_fwait_in", 187);
        methodIndexMap.put("getWtw_cr_xwait_out", 188);
        methodIndexMap.put("getWtw_cr_xwait_in", 189);
        methodIndexMap.put("getWtw_cr_AccTime_out", 190);
        methodIndexMap.put("getWtw_cr_AccTime_in", 191);
        methodIndexMap.put("getWtw_cr_EgrTime_out", 192);
        methodIndexMap.put("getWtw_cr_EgrTime_in", 193);
        methodIndexMap.put("getWtw_cr_WalkAuxTime_out", 194);
        methodIndexMap.put("getWtw_cr_WalkAuxTime_in", 195);
        methodIndexMap.put("getWtw_cr_fare_out", 196);
        methodIndexMap.put("getWtw_cr_fare_in", 197);
        methodIndexMap.put("getWtw_cr_xfers_out", 198);
        methodIndexMap.put("getWtw_cr_xfers_in", 199);

        methodIndexMap.put("getWtd_lb_LB_ivt_out", 200);
        methodIndexMap.put("getWtd_lb_LB_ivt_in", 201);
        methodIndexMap.put("getWtd_lb_fwait_out", 202);
        methodIndexMap.put("getWtd_lb_fwait_in", 203);
        methodIndexMap.put("getWtd_lb_xwait_out", 204);
        methodIndexMap.put("getWtd_lb_xwait_in", 205);
        methodIndexMap.put("getWtd_lb_AccTime_out", 206);
        methodIndexMap.put("getWtd_lb_AccTime_in", 207);
        methodIndexMap.put("getWtd_lb_EgrTime_out", 208);
        methodIndexMap.put("getWtd_lb_EgrTime_in", 209);
        methodIndexMap.put("getWtd_lb_WalkAuxTime_out", 210);
        methodIndexMap.put("getWtd_lb_WalkAuxTime_in", 211);
        methodIndexMap.put("getWtd_lb_fare_out", 212);
        methodIndexMap.put("getWtd_lb_fare_in", 213);
        methodIndexMap.put("getWtd_lb_xfers_out", 214);
        methodIndexMap.put("getWtd_lb_xfers_in", 215);
        methodIndexMap.put("getWtd_eb_LB_ivt_out", 216);
        methodIndexMap.put("getWtd_eb_LB_ivt_in", 217);
        methodIndexMap.put("getWtd_eb_EB_ivt_out", 218);
        methodIndexMap.put("getWtd_eb_EB_ivt_in", 219);
        methodIndexMap.put("getWtd_eb_fwait_out", 220);
        methodIndexMap.put("getWtd_eb_fwait_in", 221);
        methodIndexMap.put("getWtd_eb_xwait_out", 222);
        methodIndexMap.put("getWtd_eb_xwait_in", 223);
        methodIndexMap.put("getWtd_eb_AccTime_out", 224);
        methodIndexMap.put("getWtd_eb_AccTime_in", 225);
        methodIndexMap.put("getWtd_eb_EgrTime_out", 226);
        methodIndexMap.put("getWtd_eb_EgrTime_in", 227);
        methodIndexMap.put("getWtd_eb_WalkAuxTime_out", 228);
        methodIndexMap.put("getWtd_eb_WalkAuxTime_in", 229);
        methodIndexMap.put("getWtd_eb_fare_out", 230);
        methodIndexMap.put("getWtd_eb_fare_in", 231);
        methodIndexMap.put("getWtd_eb_xfers_out", 232);
        methodIndexMap.put("getWtd_eb_xfers_in", 233);
        methodIndexMap.put("getWtd_brt_LB_ivt_out", 234);
        methodIndexMap.put("getWtd_brt_LB_ivt_in", 235);
        methodIndexMap.put("getWtd_brt_EB_ivt_out", 236);
        methodIndexMap.put("getWtd_brt_EB_ivt_in", 237);
        methodIndexMap.put("getWtd_brt_BRT_ivt_out", 238);
        methodIndexMap.put("getWtd_brt_BRT_ivt_in", 239);
        methodIndexMap.put("getWtd_brt_fwait_out", 240);
        methodIndexMap.put("getWtd_brt_fwait_in", 241);
        methodIndexMap.put("getWtd_brt_xwait_out", 242);
        methodIndexMap.put("getWtd_brt_xwait_in", 243);
        methodIndexMap.put("getWtd_brt_AccTime_out", 244);
        methodIndexMap.put("getWtd_brt_AccTime_in", 245);
        methodIndexMap.put("getWtd_brt_EgrTime_out", 246);
        methodIndexMap.put("getWtd_brt_EgrTime_in", 247);
        methodIndexMap.put("getWtd_brt_WalkAuxTime_out", 248);
        methodIndexMap.put("getWtd_brt_WalkAuxTime_in", 249);
        methodIndexMap.put("getWtd_brt_fare_out", 250);
        methodIndexMap.put("getWtd_brt_fare_in", 251);
        methodIndexMap.put("getWtd_brt_xfers_out", 252);
        methodIndexMap.put("getWtd_brt_xfers_in", 253);
        methodIndexMap.put("getWtd_lr_LB_ivt_out", 254);
        methodIndexMap.put("getWtd_lr_LB_ivt_in", 255);
        methodIndexMap.put("getWtd_lr_EB_ivt_out", 256);
        methodIndexMap.put("getWtd_lr_EB_ivt_in", 257);
        methodIndexMap.put("getWtd_lr_BRT_ivt_out", 258);
        methodIndexMap.put("getWtd_lr_BRT_ivt_in", 259);
        methodIndexMap.put("getWtd_lr_LRT_ivt_out", 260);
        methodIndexMap.put("getWtd_lr_LRT_ivt_in", 261);
        methodIndexMap.put("getWtd_lr_fwait_out", 262);
        methodIndexMap.put("getWtd_lr_fwait_in", 263);
        methodIndexMap.put("getWtd_lr_xwait_out", 264);
        methodIndexMap.put("getWtd_lr_xwait_in", 265);
        methodIndexMap.put("getWtd_lr_AccTime_out", 266);
        methodIndexMap.put("getWtd_lr_AccTime_in", 267);
        methodIndexMap.put("getWtd_lr_EgrTime_out", 268);
        methodIndexMap.put("getWtd_lr_EgrTime_in", 269);
        methodIndexMap.put("getWtd_lr_WalkAuxTime_out", 270);
        methodIndexMap.put("getWtd_lr_WalkAuxTime_in", 271);
        methodIndexMap.put("getWtd_lr_fare_out", 272);
        methodIndexMap.put("getWtd_lr_fare_in", 273);
        methodIndexMap.put("getWtd_lr_xfers_out", 274);
        methodIndexMap.put("getWtd_lr_xfers_in", 275);
        methodIndexMap.put("getWtd_cr_LB_ivt_out", 276);
        methodIndexMap.put("getWtd_cr_LB_ivt_in", 277);
        methodIndexMap.put("getWtd_cr_EB_ivt_out", 278);
        methodIndexMap.put("getWtd_cr_EB_ivt_in", 279);
        methodIndexMap.put("getWtd_cr_BRT_ivt_out", 280);
        methodIndexMap.put("getWtd_cr_BRT_ivt_in", 281);
        methodIndexMap.put("getWtd_cr_LRT_ivt_out", 282);
        methodIndexMap.put("getWtd_cr_LRT_ivt_in", 283);
        methodIndexMap.put("getWtd_cr_CR_ivt_out", 284);
        methodIndexMap.put("getWtd_cr_CR_ivt_in", 285);
        methodIndexMap.put("getWtd_cr_fwait_out", 286);
        methodIndexMap.put("getWtd_cr_fwait_in", 287);
        methodIndexMap.put("getWtd_cr_xwait_out", 288);
        methodIndexMap.put("getWtd_cr_xwait_in", 289);
        methodIndexMap.put("getWtd_cr_AccTime_out", 290);
        methodIndexMap.put("getWtd_cr_AccTime_in", 291);
        methodIndexMap.put("getWtd_cr_EgrTime_out", 292);
        methodIndexMap.put("getWtd_cr_EgrTime_in", 293);
        methodIndexMap.put("getWtd_cr_WalkAuxTime_out", 294);
        methodIndexMap.put("getWtd_cr_WalkAuxTime_in", 295);
        methodIndexMap.put("getWtd_cr_fare_out", 296);
        methodIndexMap.put("getWtd_cr_fare_in", 297);
        methodIndexMap.put("getWtd_cr_xfers_out", 298);
        methodIndexMap.put("getWtd_cr_xfers_in", 299);

        methodIndexMap.put("getDtw_lb_LB_ivt_out", 300);
        methodIndexMap.put("getDtw_lb_LB_ivt_in", 301);
        methodIndexMap.put("getDtw_lb_fwait_out", 302);
        methodIndexMap.put("getDtw_lb_fwait_in", 303);
        methodIndexMap.put("getDtw_lb_xwait_out", 304);
        methodIndexMap.put("getDtw_lb_xwait_in", 305);
        methodIndexMap.put("getDtw_lb_AccTime_out", 306);
        methodIndexMap.put("getDtw_lb_AccTime_in", 307);
        methodIndexMap.put("getDtw_lb_EgrTime_out", 308);
        methodIndexMap.put("getDtw_lb_EgrTime_in", 309);
        methodIndexMap.put("getDtw_lb_WalkAuxTime_out", 310);
        methodIndexMap.put("getDtw_lb_WalkAuxTime_in", 311);
        methodIndexMap.put("getDtw_lb_fare_out", 312);
        methodIndexMap.put("getDtw_lb_fare_in", 313);
        methodIndexMap.put("getDtw_lb_xfers_out", 314);
        methodIndexMap.put("getDtw_lb_xfers_in", 315);
        methodIndexMap.put("getDtw_eb_LB_ivt_out", 316);
        methodIndexMap.put("getDtw_eb_LB_ivt_in", 317);
        methodIndexMap.put("getDtw_eb_EB_ivt_out", 318);
        methodIndexMap.put("getDtw_eb_EB_ivt_in", 319);
        methodIndexMap.put("getDtw_eb_fwait_out", 320);
        methodIndexMap.put("getDtw_eb_fwait_in", 321);
        methodIndexMap.put("getDtw_eb_xwait_out", 322);
        methodIndexMap.put("getDtw_eb_xwait_in", 323);
        methodIndexMap.put("getDtw_eb_AccTime_out", 324);
        methodIndexMap.put("getDtw_eb_AccTime_in", 325);
        methodIndexMap.put("getDtw_eb_EgrTime_out", 326);
        methodIndexMap.put("getDtw_eb_EgrTime_in", 327);
        methodIndexMap.put("getDtw_eb_WalkAuxTime_out", 328);
        methodIndexMap.put("getDtw_eb_WalkAuxTime_in", 329);
        methodIndexMap.put("getDtw_eb_fare_out", 330);
        methodIndexMap.put("getDtw_eb_fare_in", 331);
        methodIndexMap.put("getDtw_eb_xfers_out", 332);
        methodIndexMap.put("getDtw_eb_xfers_in", 333);
        methodIndexMap.put("getDtw_brt_LB_ivt_out", 334);
        methodIndexMap.put("getDtw_brt_LB_ivt_in", 335);
        methodIndexMap.put("getDtw_brt_EB_ivt_out", 336);
        methodIndexMap.put("getDtw_brt_EB_ivt_in", 337);
        methodIndexMap.put("getDtw_brt_BRT_ivt_out", 338);
        methodIndexMap.put("getDtw_brt_BRT_ivt_in", 339);
        methodIndexMap.put("getDtw_brt_fwait_out", 340);
        methodIndexMap.put("getDtw_brt_fwait_in", 341);
        methodIndexMap.put("getDtw_brt_xwait_out", 342);
        methodIndexMap.put("getDtw_brt_xwait_in", 343);
        methodIndexMap.put("getDtw_brt_AccTime_out", 344);
        methodIndexMap.put("getDtw_brt_AccTime_in", 345);
        methodIndexMap.put("getDtw_brt_EgrTime_out", 346);
        methodIndexMap.put("getDtw_brt_EgrTime_in", 347);
        methodIndexMap.put("getDtw_brt_WalkAuxTime_out", 348);
        methodIndexMap.put("getDtw_brt_WalkAuxTime_in", 349);
        methodIndexMap.put("getDtw_brt_fare_out", 350);
        methodIndexMap.put("getDtw_brt_fare_in", 351);
        methodIndexMap.put("getDtw_brt_xfers_out", 352);
        methodIndexMap.put("getDtw_brt_xfers_in", 353);
        methodIndexMap.put("getDtw_lr_LB_ivt_out", 354);
        methodIndexMap.put("getDtw_lr_LB_ivt_in", 355);
        methodIndexMap.put("getDtw_lr_EB_ivt_out", 356);
        methodIndexMap.put("getDtw_lr_EB_ivt_in", 357);
        methodIndexMap.put("getDtw_lr_BRT_ivt_out", 358);
        methodIndexMap.put("getDtw_lr_BRT_ivt_in", 359);
        methodIndexMap.put("getDtw_lr_LRT_ivt_out", 360);
        methodIndexMap.put("getDtw_lr_LRT_ivt_in", 361);
        methodIndexMap.put("getDtw_lr_fwait_out", 362);
        methodIndexMap.put("getDtw_lr_fwait_in", 363);
        methodIndexMap.put("getDtw_lr_xwait_out", 364);
        methodIndexMap.put("getDtw_lr_xwait_in", 365);
        methodIndexMap.put("getDtw_lr_AccTime_out", 366);
        methodIndexMap.put("getDtw_lr_AccTime_in", 367);
        methodIndexMap.put("getDtw_lr_EgrTime_out", 368);
        methodIndexMap.put("getDtw_lr_EgrTime_in", 369);
        methodIndexMap.put("getDtw_lr_WalkAuxTime_out", 370);
        methodIndexMap.put("getDtw_lr_WalkAuxTime_in", 371);
        methodIndexMap.put("getDtw_lr_fare_out", 372);
        methodIndexMap.put("getDtw_lr_fare_in", 373);
        methodIndexMap.put("getDtw_lr_xfers_out", 374);
        methodIndexMap.put("getDtw_lr_xfers_in", 375);
        methodIndexMap.put("getDtw_cr_LB_ivt_out", 376);
        methodIndexMap.put("getDtw_cr_LB_ivt_in", 377);
        methodIndexMap.put("getDtw_cr_EB_ivt_out", 378);
        methodIndexMap.put("getDtw_cr_EB_ivt_in", 379);
        methodIndexMap.put("getDtw_cr_BRT_ivt_out", 380);
        methodIndexMap.put("getDtw_cr_BRT_ivt_in", 381);
        methodIndexMap.put("getDtw_cr_LRT_ivt_out", 382);
        methodIndexMap.put("getDtw_cr_LRT_ivt_in", 383);
        methodIndexMap.put("getDtw_cr_CR_ivt_out", 384);
        methodIndexMap.put("getDtw_cr_CR_ivt_in", 385);
        methodIndexMap.put("getDtw_cr_fwait_out", 386);
        methodIndexMap.put("getDtw_cr_fwait_in", 387);
        methodIndexMap.put("getDtw_cr_xwait_out", 388);
        methodIndexMap.put("getDtw_cr_xwait_in", 389);
        methodIndexMap.put("getDtw_cr_AccTime_out", 390);
        methodIndexMap.put("getDtw_cr_AccTime_in", 391);
        methodIndexMap.put("getDtw_cr_EgrTime_out", 392);
        methodIndexMap.put("getDtw_cr_EgrTime_in", 393);
        methodIndexMap.put("getDtw_cr_WalkAuxTime_out", 394);
        methodIndexMap.put("getDtw_cr_WalkAuxTime_in", 395);
        methodIndexMap.put("getDtw_cr_fare_out", 396);
        methodIndexMap.put("getDtw_cr_fare_in", 397);
        methodIndexMap.put("getDtw_cr_xfers_out", 398);
        methodIndexMap.put("getDtw_cr_xfers_in", 399);
        
        methodIndexMap.put("getFemaleInParty", 400);
        methodIndexMap.put("getMaleInParty", 401);
        methodIndexMap.put("getInboundFemaleBikeLogsum", 402);
        methodIndexMap.put("getOutboundFemaleBikeLogsum", 403);
        methodIndexMap.put("getInboundMaleBikeLogsum", 404);
        methodIndexMap.put("getOutboundMaleBikeLogsum", 405);

        CreateReverseMap();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        return getValueForIndexLookup(variableIndex, arrayIndex);
    }
}