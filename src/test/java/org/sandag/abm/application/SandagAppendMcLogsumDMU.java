package org.sandag.abm.application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TourModeChoiceDMU;

import com.pb.common.calculator.IndexValues;

public class SandagAppendMcLogsumDMU
        extends TourModeChoiceDMU
{

    private int departPeriod;
    private int arrivePeriod;

    private int incomeInDollars;
    private int adults;
    private int autos;
    private int hhSize;
    private int personIsFemale;
    private int age;
    private int tourCategoryJoint;
    private int tourCategoryEscort;
    private int numberOfParticipantsInJointTour;
    private int workTourModeIsHOV;
    private int workTourModeIsSOV;
    private int workTourModeIsBike;
    private int tourCategorySubtour;

    public SandagAppendMcLogsumDMU(ModelStructure modelStructure, Logger aLogger)
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

 
    public void setNonMotorizedWalkTimeOut(double walkTime)
    {
        nmWalkTimeOut = walkTime;
    }

    public void setNonMotorizedWalkTimeIn(double walkTime)
    {
        nmWalkTimeIn = walkTime;
    }

    public void setNonMotorizedBikeTimeOut(double bikeTime)
    {
        nmBikeTimeOut = bikeTime;
    }

    public void setNonMotorizedBikeTimeIn(double bikeTime)
    {
        nmBikeTimeIn = bikeTime;
    }

    public float getTimeOutbound()
    {
        return departPeriod;
    }

    public float getTimeInbound()
    {
        return arrivePeriod;
    }

    public void setDepartPeriod(int period)
    {
        departPeriod = period;
    }

    public void setArrivePeriod(int period)
    {
        arrivePeriod = period;
    }

    public void setHhSize(int arg)
    {
        hhSize = arg;
    }

    public void setAge(int arg)
    {
        age = arg;
    }

    public void setTourCategoryJoint(int arg)
    {
        tourCategoryJoint = arg;
    }

    public void setTourCategoryEscort(int arg)
    {
        tourCategoryEscort = arg;
    }

    public void setNumberOfParticipantsInJointTour(int arg)
    {
        numberOfParticipantsInJointTour = arg;
    }

    public void setWorkTourModeIsSOV(int arg)
    {
        workTourModeIsSOV = arg;
    }

    public void setWorkTourModeIsHOV(int arg)
    {
        workTourModeIsHOV = arg;
    }

    public void setWorkTourModeIsBike(int arg)
    {
        workTourModeIsBike = arg;
    }

    public void setPTazTerminalTime(float arg)
    {
        pTazTerminalTime = arg;
    }

    public void setATazTerminalTime(float arg)
    {
        aTazTerminalTime = arg;
    }

    public void setIncomeInDollars(int arg)
    {
        incomeInDollars = arg;
    }

    public int getIncome()
    {
        return incomeInDollars;
    }

    public void setAdults(int arg)
    {
        adults = arg;
    }

    public int getAdults()
    {
        return adults;
    }

    public void setAutos(int arg)
    {
        autos = arg;
    }

    public int getAutos()
    {
        return autos;
    }

    public int getAge()
    {
        return age;
    }

    public int getHhSize()
    {
        return hhSize;
    }

    public int getTourCategoryJoint()
    {
        return tourCategoryJoint;
    }

    public int getTourCategoryEscort()
    {
        return tourCategoryEscort;
    }

    public int getNumberOfParticipantsInJointTour()
    {
        return numberOfParticipantsInJointTour;
    }

    public int getWorkTourModeIsSov()
    {
        return workTourModeIsSOV;
    }

    public int getWorkTourModeIsHov()
    {
        return workTourModeIsHOV;
    }

    public int getWorkTourModeIsBike()
    {
        return workTourModeIsBike;
    }

    public void setPersonIsFemale(int arg)
    {
        personIsFemale = arg;
    }

    public int getFemale()
    {
        return personIsFemale;
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
        return nmWalkTimeOut;
    }

    public double getNm_walkTime_in()
    {
        return nmWalkTimeIn;
    }

    public double getNm_bikeTime_out()
    {
        return nmBikeTimeOut;
    }

    public double getNm_bikeTime_in()
    {
        return nmBikeTimeIn;
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

        methodIndexMap.put("getNm_walkTime_out", 90);
        methodIndexMap.put("getNm_walkTime_in", 91);
        methodIndexMap.put("getNm_bikeTime_out", 92);
        methodIndexMap.put("getNm_bikeTime_in", 93);
        methodIndexMap.put("getWtw_LB_ivt_out", 176);
        methodIndexMap.put("getWtw_LB_ivt_in", 177);
        methodIndexMap.put("getWtw_EB_ivt_out", 178);
        methodIndexMap.put("getWtw_EB_ivt_in", 179);
        methodIndexMap.put("getWtw_BRT_ivt_out", 180);
        methodIndexMap.put("getWtw_BRT_ivt_in", 181);
        methodIndexMap.put("getWtw_LRT_ivt_out", 182);
        methodIndexMap.put("getWtw_LRT_ivt_in", 183);
        methodIndexMap.put("getWtw_CR_ivt_out", 184);
        methodIndexMap.put("getWtw_CR_ivt_in", 185);
        methodIndexMap.put("getWtw_fwait_out", 186);
        methodIndexMap.put("getWtw_fwait_in", 187);
        methodIndexMap.put("getWtw_xwait_out", 188);
        methodIndexMap.put("getWtw_xwait_in", 189);
        methodIndexMap.put("getWtw_AccTime_out", 190);
        methodIndexMap.put("getWtw_AccTime_in", 191);
        methodIndexMap.put("getWtw_EgrTime_out", 192);
        methodIndexMap.put("getWtw_EgrTime_in", 193);
        methodIndexMap.put("getWtw_WalkAuxTime_out", 194);
        methodIndexMap.put("getWtw_WalkAuxTime_in", 195);
        methodIndexMap.put("getWtw_fare_out", 196);
        methodIndexMap.put("getWtw_fare_in", 197);
        methodIndexMap.put("getWtw_xfers_out", 198);
        methodIndexMap.put("getWtw_xfers_in", 199);

        methodIndexMap.put("getWtd_LB_ivt_out", 276);
        methodIndexMap.put("getWtd_LB_ivt_in", 277);
        methodIndexMap.put("getWtd_EB_ivt_out", 278);
        methodIndexMap.put("getWtd_EB_ivt_in", 279);
        methodIndexMap.put("getWtd_BRT_ivt_out", 280);
        methodIndexMap.put("getWtd_BRT_ivt_in", 281);
        methodIndexMap.put("getWtd_LRT_ivt_out", 282);
        methodIndexMap.put("getWtd_LRT_ivt_in", 283);
        methodIndexMap.put("getWtd_CR_ivt_out", 284);
        methodIndexMap.put("getWtd_CR_ivt_in", 285);
        methodIndexMap.put("getWtd_fwait_out", 286);
        methodIndexMap.put("getWtd_fwait_in", 287);
        methodIndexMap.put("getWtd_xwait_out", 288);
        methodIndexMap.put("getWtd_xwait_in", 289);
        methodIndexMap.put("getWtd_AccTime_out", 290);
        methodIndexMap.put("getWtd_AccTime_in", 291);
        methodIndexMap.put("getWtd_EgrTime_out", 292);
        methodIndexMap.put("getWtd_EgrTime_in", 293);
        methodIndexMap.put("getWtd_WalkAuxTime_out", 294);
        methodIndexMap.put("getWtd_WalkAuxTime_in", 295);
        methodIndexMap.put("getWtd_fare_out", 296);
        methodIndexMap.put("getWtd_fare_in", 297);
        methodIndexMap.put("getWtd_xfers_out", 298);
        methodIndexMap.put("getWtd_xfers_in", 299);
        methodIndexMap.put("getDtw_LB_ivt_out", 376);
        methodIndexMap.put("getDtw_LB_ivt_in", 377);
        methodIndexMap.put("getDtw_EB_ivt_out", 378);
        methodIndexMap.put("getDtw_EB_ivt_in", 379);
        methodIndexMap.put("getDtw_BRT_ivt_out", 380);
        methodIndexMap.put("getDtw_BRT_ivt_in", 381);
        methodIndexMap.put("getDtw_LRT_ivt_out", 382);
        methodIndexMap.put("getDtw_LRT_ivt_in", 383);
        methodIndexMap.put("getDtw_CR_ivt_out", 384);
        methodIndexMap.put("getDtw_CR_ivt_in", 385);
        methodIndexMap.put("getDtw_fwait_out", 386);
        methodIndexMap.put("getDtw_fwait_in", 387);
        methodIndexMap.put("getDtw_xwait_out", 388);
        methodIndexMap.put("getDtw_xwait_in", 389);
        methodIndexMap.put("getDtw_AccTime_out", 390);
        methodIndexMap.put("getDtw_AccTime_in", 391);
        methodIndexMap.put("getDtw_EgrTime_out", 392);
        methodIndexMap.put("getDtw_EgrTime_in", 393);
        methodIndexMap.put("getDtw_WalkAuxTime_out", 394);
        methodIndexMap.put("getDtw_WalkAuxTime_in", 395);
        methodIndexMap.put("getDtw_fare_out", 396);
        methodIndexMap.put("getDtw_fare_in", 397);
        methodIndexMap.put("getDtw_xfers_out", 398);
        methodIndexMap.put("getDtw_xfers_in", 399);

 
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
        		returnValue = getIncome();
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
        	case 90:
        		returnValue = getNm_walkTime_out();
        		break;
        	case 91:
        		returnValue = getNm_walkTime_in();
        		break;
        	case 92:
        		returnValue = getNm_bikeTime_out();
        		break;
        	case 93:
        		returnValue = getNm_bikeTime_in();
        		break;
        /* TODO
        	case 176:
        		methodIndexMap.put("getWtw_LB_ivt_out", 176);
        	case 177:
        		methodIndexMap.put("getWtw_LB_ivt_in", 177);
        	case 178:
        		methodIndexMap.put("getWtw_EB_ivt_out", 178);
        	case 179:
        		methodIndexMap.put("getWtw_EB_ivt_in", 179);
        	case 180:
        		methodIndexMap.put("getWtw_BRT_ivt_out", 180);
        	case 181:
        		methodIndexMap.put("getWtw_BRT_ivt_in", 181);
        	case 182:
        		methodIndexMap.put("getWtw_LRT_ivt_out", 182);
        	case 183:
        		methodIndexMap.put("getWtw_LRT_ivt_in", 183);
        	        methodIndexMap.put("getWtw_CR_ivt_out", 184);
        	        methodIndexMap.put("getWtw_CR_ivt_in", 185);
        	        methodIndexMap.put("getWtw_fwait_out", 186);
        	        methodIndexMap.put("getWtw_fwait_in", 187);
        	        methodIndexMap.put("getWtw_xwait_out", 188);
        	        methodIndexMap.put("getWtw_xwait_in", 189);
        	        methodIndexMap.put("getWtw_AccTime_out", 190);
        	        methodIndexMap.put("getWtw_AccTime_in", 191);
        	        methodIndexMap.put("getWtw_EgrTime_out", 192);
        	        methodIndexMap.put("getWtw_EgrTime_in", 193);
        	        methodIndexMap.put("getWtw_WalkAuxTime_out", 194);
        	        methodIndexMap.put("getWtw_WalkAuxTime_in", 195);
        	        methodIndexMap.put("getWtw_fare_out", 196);
        	        methodIndexMap.put("getWtw_fare_in", 197);
        	        methodIndexMap.put("getWtw_xfers_out", 198);
        	        methodIndexMap.put("getWtw_xfers_in", 199);

        	        methodIndexMap.put("getWtd_LB_ivt_out", 276);
        	        methodIndexMap.put("getWtd_LB_ivt_in", 277);
        	        methodIndexMap.put("getWtd_EB_ivt_out", 278);
        	        methodIndexMap.put("getWtd_EB_ivt_in", 279);
        	        methodIndexMap.put("getWtd_BRT_ivt_out", 280);
        	        methodIndexMap.put("getWtd_BRT_ivt_in", 281);
        	        methodIndexMap.put("getWtd_LRT_ivt_out", 282);
        	        methodIndexMap.put("getWtd_LRT_ivt_in", 283);
        	        methodIndexMap.put("getWtd_CR_ivt_out", 284);
        	        methodIndexMap.put("getWtd_CR_ivt_in", 285);
        	        methodIndexMap.put("getWtd_fwait_out", 286);
        	        methodIndexMap.put("getWtd_fwait_in", 287);
        	        methodIndexMap.put("getWtd_xwait_out", 288);
        	        methodIndexMap.put("getWtd_xwait_in", 289);
        	        methodIndexMap.put("getWtd_AccTime_out", 290);
        	        methodIndexMap.put("getWtd_AccTime_in", 291);
        	        methodIndexMap.put("getWtd_EgrTime_out", 292);
        	        methodIndexMap.put("getWtd_EgrTime_in", 293);
        	        methodIndexMap.put("getWtd_WalkAuxTime_out", 294);
        	        methodIndexMap.put("getWtd_WalkAuxTime_in", 295);
        	        methodIndexMap.put("getWtd_fare_out", 296);
        	        methodIndexMap.put("getWtd_fare_in", 297);
        	        methodIndexMap.put("getWtd_xfers_out", 298);
        	        methodIndexMap.put("getWtd_xfers_in", 299);
        	        methodIndexMap.put("getDtw_LB_ivt_out", 376);
        	        methodIndexMap.put("getDtw_LB_ivt_in", 377);
        	        methodIndexMap.put("getDtw_EB_ivt_out", 378);
        	        methodIndexMap.put("getDtw_EB_ivt_in", 379);
        	        methodIndexMap.put("getDtw_BRT_ivt_out", 380);
        	        methodIndexMap.put("getDtw_BRT_ivt_in", 381);
        	        methodIndexMap.put("getDtw_LRT_ivt_out", 382);
        	        methodIndexMap.put("getDtw_LRT_ivt_in", 383);
        	        methodIndexMap.put("getDtw_CR_ivt_out", 384);
        	        methodIndexMap.put("getDtw_CR_ivt_in", 385);
        	        methodIndexMap.put("getDtw_fwait_out", 386);
        	        methodIndexMap.put("getDtw_fwait_in", 387);
        	        methodIndexMap.put("getDtw_xwait_out", 388);
        	        methodIndexMap.put("getDtw_xwait_in", 389);
        	        methodIndexMap.put("getDtw_AccTime_out", 390);
        	        methodIndexMap.put("getDtw_AccTime_in", 391);
        	        methodIndexMap.put("getDtw_EgrTime_out", 392);
        	        methodIndexMap.put("getDtw_EgrTime_in", 393);
        	        methodIndexMap.put("getDtw_WalkAuxTime_out", 394);
        	        methodIndexMap.put("getDtw_WalkAuxTime_in", 395);
        	        methodIndexMap.put("getDtw_fare_out", 396);
        	        methodIndexMap.put("getDtw_fare_in", 397);
        	        methodIndexMap.put("getDtw_xfers_out", 398);
        	        methodIndexMap.put("getDtw_xfers_in", 399);
        		*/
        		
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");
               
        }
        return returnValue;

    }

 
}