package org.sandag.abm.application;

import java.util.HashMap;
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

    public SandagAppendMcLogsumDMU(ModelStructure modelStructure)
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

    public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir, double value)
    {
        transitSkim[accEgr][lbPrem][skimIndex][dir] = value;
    }

    public double getTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir)
    {
        return transitSkim[accEgr][lbPrem][skimIndex][dir];
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
                returnValue = hhSize;
                break;
            case 6:
                returnValue = autos;
                break;
            case 7:
                returnValue = age;
                break;
            case 8:
                returnValue = tourCategoryJoint;
                break;
            case 9:
                returnValue = numberOfParticipantsInJointTour;
                break;
            case 10:
                returnValue = workTourModeIsSOV;
                break;
            case 11:
                returnValue = workTourModeIsBike;
                break;
            case 12:
                returnValue = tourCategorySubtour;
                break;
            case 14:
                returnValue = pTazTerminalTime;
                break;
            case 15:
                returnValue = aTazTerminalTime;
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
                returnValue = tourCategoryEscort;
                break;
            case 90:
                returnValue = nmWalkTimeOut;
                break;
            case 91:
                returnValue = nmWalkTimeIn;
                break;
            case 92:
                returnValue = nmBikeTimeOut;
                break;
            case 93:
                returnValue = nmBikeTimeIn;
                break;
            case 100:
                returnValue = getTransitSkim(WTW, LB, LB_IVT, OUT);
                break;
            case 101:
                returnValue = getTransitSkim(WTW, LB, LB_IVT, IN);
                break;
            case 102:
                returnValue = getTransitSkim(WTW, LB, FWAIT, OUT);
                break;
            case 103:
                returnValue = getTransitSkim(WTW, LB, FWAIT, IN);
                break;
            case 104:
                returnValue = getTransitSkim(WTW, LB, XWAIT, OUT);
                break;
            case 105:
                returnValue = getTransitSkim(WTW, LB, XWAIT, IN);
                break;
            case 106:
                returnValue = getTransitSkim(WTW, LB, ACC, OUT);
                break;
            case 107:
                returnValue = getTransitSkim(WTW, LB, ACC, IN);
                break;
            case 108:
                returnValue = getTransitSkim(WTW, LB, EGR, OUT);
                break;
            case 109:
                returnValue = getTransitSkim(WTW, LB, EGR, IN);
                break;
            case 110:
                returnValue = getTransitSkim(WTW, LB, AUX, OUT);
                break;
            case 111:
                returnValue = getTransitSkim(WTW, LB, AUX, IN);
                break;
            case 112:
                returnValue = getTransitSkim(WTW, LB, FARE, OUT);
                break;
            case 113:
                returnValue = getTransitSkim(WTW, LB, FARE, IN);
                break;
            case 114:
                returnValue = getTransitSkim(WTW, LB, XFERS, OUT);
                break;
            case 115:
                returnValue = getTransitSkim(WTW, LB, XFERS, IN);
                break;
            case 116:
                returnValue = getTransitSkim(WTW, EB, LB_IVT, OUT);
                break;
            case 117:
                returnValue = getTransitSkim(WTW, EB, LB_IVT, IN);
                break;
            case 118:
                returnValue = getTransitSkim(WTW, EB, EB_IVT, OUT);
                break;
            case 119:
                returnValue = getTransitSkim(WTW, EB, EB_IVT, IN);
                break;
            case 120:
                returnValue = getTransitSkim(WTW, EB, FWAIT, OUT);
                break;
            case 121:
                returnValue = getTransitSkim(WTW, EB, FWAIT, IN);
                break;
            case 122:
                returnValue = getTransitSkim(WTW, EB, XWAIT, OUT);
                break;
            case 123:
                returnValue = getTransitSkim(WTW, EB, XWAIT, IN);
                break;
            case 124:
                returnValue = getTransitSkim(WTW, EB, ACC, OUT);
                break;
            case 125:
                returnValue = getTransitSkim(WTW, EB, ACC, IN);
                break;
            case 126:
                returnValue = getTransitSkim(WTW, EB, EGR, OUT);
                break;
            case 127:
                returnValue = getTransitSkim(WTW, EB, EGR, IN);
                break;
            case 128:
                returnValue = getTransitSkim(WTW, EB, AUX, OUT);
                break;
            case 129:
                returnValue = getTransitSkim(WTW, EB, AUX, IN);
                break;
            case 130:
                returnValue = getTransitSkim(WTW, EB, FARE, OUT);
                break;
            case 131:
                returnValue = getTransitSkim(WTW, EB, FARE, IN);
                break;
            case 132:
                returnValue = getTransitSkim(WTW, EB, XFERS, OUT);
                break;
            case 133:
                returnValue = getTransitSkim(WTW, EB, XFERS, IN);
                break;
            case 134:
                returnValue = getTransitSkim(WTW, BRT, LB_IVT, OUT);
                break;
            case 135:
                returnValue = getTransitSkim(WTW, BRT, LB_IVT, IN);
                break;
            case 136:
                returnValue = getTransitSkim(WTW, BRT, EB_IVT, OUT);
                break;
            case 137:
                returnValue = getTransitSkim(WTW, BRT, EB_IVT, IN);
                break;
            case 138:
                returnValue = getTransitSkim(WTW, BRT, BRT_IVT, OUT);
                break;
            case 139:
                returnValue = getTransitSkim(WTW, BRT, BRT_IVT, IN);
                break;
            case 140:
                returnValue = getTransitSkim(WTW, BRT, FWAIT, OUT);
                break;
            case 141:
                returnValue = getTransitSkim(WTW, BRT, FWAIT, IN);
                break;
            case 142:
                returnValue = getTransitSkim(WTW, BRT, XWAIT, OUT);
                break;
            case 143:
                returnValue = getTransitSkim(WTW, BRT, XWAIT, IN);
                break;
            case 144:
                returnValue = getTransitSkim(WTW, BRT, ACC, OUT);
                break;
            case 145:
                returnValue = getTransitSkim(WTW, BRT, ACC, IN);
                break;
            case 146:
                returnValue = getTransitSkim(WTW, BRT, EGR, OUT);
                break;
            case 147:
                returnValue = getTransitSkim(WTW, BRT, EGR, IN);
                break;
            case 148:
                returnValue = getTransitSkim(WTW, BRT, AUX, OUT);
                break;
            case 149:
                returnValue = getTransitSkim(WTW, BRT, AUX, IN);
                break;
            case 150:
                returnValue = getTransitSkim(WTW, BRT, FARE, OUT);
                break;
            case 151:
                returnValue = getTransitSkim(WTW, BRT, FARE, IN);
                break;
            case 152:
                returnValue = getTransitSkim(WTW, BRT, XFERS, OUT);
                break;
            case 153:
                returnValue = getTransitSkim(WTW, BRT, XFERS, IN);
                break;
            case 154:
                returnValue = getTransitSkim(WTW, LR, LB_IVT, OUT);
                break;
            case 155:
                returnValue = getTransitSkim(WTW, LR, LB_IVT, IN);
                break;
            case 156:
                returnValue = getTransitSkim(WTW, LR, EB_IVT, OUT);
                break;
            case 157:
                returnValue = getTransitSkim(WTW, LR, EB_IVT, IN);
                break;
            case 158:
                returnValue = getTransitSkim(WTW, LR, BRT_IVT, OUT);
                break;
            case 159:
                returnValue = getTransitSkim(WTW, LR, BRT_IVT, IN);
                break;
            case 160:
                returnValue = getTransitSkim(WTW, LR, LR_IVT, OUT);
                break;
            case 161:
                returnValue = getTransitSkim(WTW, LR, LR_IVT, IN);
                break;
            case 162:
                returnValue = getTransitSkim(WTW, LR, FWAIT, OUT);
                break;
            case 163:
                returnValue = getTransitSkim(WTW, LR, FWAIT, IN);
                break;
            case 164:
                returnValue = getTransitSkim(WTW, LR, XWAIT, OUT);
                break;
            case 165:
                returnValue = getTransitSkim(WTW, LR, XWAIT, IN);
                break;
            case 166:
                returnValue = getTransitSkim(WTW, LR, ACC, OUT);
                break;
            case 167:
                returnValue = getTransitSkim(WTW, LR, ACC, IN);
                break;
            case 168:
                returnValue = getTransitSkim(WTW, LR, EGR, OUT);
                break;
            case 169:
                returnValue = getTransitSkim(WTW, LR, EGR, IN);
                break;
            case 170:
                returnValue = getTransitSkim(WTW, LR, AUX, OUT);
                break;
            case 171:
                returnValue = getTransitSkim(WTW, LR, AUX, IN);
                break;
            case 172:
                returnValue = getTransitSkim(WTW, LR, FARE, OUT);
                break;
            case 173:
                returnValue = getTransitSkim(WTW, LR, FARE, IN);
                break;
            case 174:
                returnValue = getTransitSkim(WTW, LR, XFERS, OUT);
                break;
            case 175:
                returnValue = getTransitSkim(WTW, LR, XFERS, IN);
                break;
            case 176:
                returnValue = getTransitSkim(WTW, CR, LB_IVT, OUT);
                break;
            case 177:
                returnValue = getTransitSkim(WTW, CR, LB_IVT, IN);
                break;
            case 178:
                returnValue = getTransitSkim(WTW, CR, EB_IVT, OUT);
                break;
            case 179:
                returnValue = getTransitSkim(WTW, CR, EB_IVT, IN);
                break;
            case 180:
                returnValue = getTransitSkim(WTW, CR, BRT_IVT, OUT);
                break;
            case 181:
                returnValue = getTransitSkim(WTW, CR, BRT_IVT, IN);
                break;
            case 182:
                returnValue = getTransitSkim(WTW, CR, LR_IVT, OUT);
                break;
            case 183:
                returnValue = getTransitSkim(WTW, CR, LR_IVT, IN);
                break;
            case 184:
                returnValue = getTransitSkim(WTW, CR, CR_IVT, OUT);
                break;
            case 185:
                returnValue = getTransitSkim(WTW, CR, CR_IVT, IN);
                break;
            case 186:
                returnValue = getTransitSkim(WTW, CR, FWAIT, OUT);
                break;
            case 187:
                returnValue = getTransitSkim(WTW, CR, FWAIT, IN);
                break;
            case 188:
                returnValue = getTransitSkim(WTW, CR, XWAIT, OUT);
                break;
            case 189:
                returnValue = getTransitSkim(WTW, CR, XWAIT, IN);
                break;
            case 190:
                returnValue = getTransitSkim(WTW, CR, ACC, OUT);
                break;
            case 191:
                returnValue = getTransitSkim(WTW, CR, ACC, IN);
                break;
            case 192:
                returnValue = getTransitSkim(WTW, CR, EGR, OUT);
                break;
            case 193:
                returnValue = getTransitSkim(WTW, CR, EGR, IN);
                break;
            case 194:
                returnValue = getTransitSkim(WTW, CR, AUX, OUT);
                break;
            case 195:
                returnValue = getTransitSkim(WTW, CR, AUX, IN);
                break;
            case 196:
                returnValue = getTransitSkim(WTW, CR, FARE, OUT);
                break;
            case 197:
                returnValue = getTransitSkim(WTW, CR, FARE, IN);
                break;
            case 198:
                returnValue = getTransitSkim(WTW, CR, XFERS, OUT);
                break;
            case 199:
                returnValue = getTransitSkim(WTW, CR, XFERS, IN);
                break;
            case 200:
                returnValue = getTransitSkim(WTD, LB, LB_IVT, OUT);
                break;
            case 201:
                returnValue = getTransitSkim(WTD, LB, LB_IVT, IN);
                break;
            case 202:
                returnValue = getTransitSkim(WTD, LB, FWAIT, OUT);
                break;
            case 203:
                returnValue = getTransitSkim(WTD, LB, FWAIT, IN);
                break;
            case 204:
                returnValue = getTransitSkim(WTD, LB, XWAIT, OUT);
                break;
            case 205:
                returnValue = getTransitSkim(WTD, LB, XWAIT, IN);
                break;
            case 206:
                returnValue = getTransitSkim(WTD, LB, ACC, OUT);
                break;
            case 207:
                returnValue = getTransitSkim(WTD, LB, ACC, IN);
                break;
            case 208:
                returnValue = getTransitSkim(WTD, LB, EGR, OUT);
                break;
            case 209:
                returnValue = getTransitSkim(WTD, LB, EGR, IN);
                break;
            case 210:
                returnValue = getTransitSkim(WTD, LB, AUX, OUT);
                break;
            case 211:
                returnValue = getTransitSkim(WTD, LB, AUX, IN);
                break;
            case 212:
                returnValue = getTransitSkim(WTD, LB, FARE, OUT);
                break;
            case 213:
                returnValue = getTransitSkim(WTD, LB, FARE, IN);
                break;
            case 214:
                returnValue = getTransitSkim(WTD, LB, XFERS, OUT);
                break;
            case 215:
                returnValue = getTransitSkim(WTD, LB, XFERS, IN);
                break;
            case 216:
                returnValue = getTransitSkim(WTD, EB, LB_IVT, OUT);
                break;
            case 217:
                returnValue = getTransitSkim(WTD, EB, LB_IVT, IN);
                break;
            case 218:
                returnValue = getTransitSkim(WTD, EB, EB_IVT, OUT);
                break;
            case 219:
                returnValue = getTransitSkim(WTD, EB, EB_IVT, IN);
                break;
            case 220:
                returnValue = getTransitSkim(WTD, EB, FWAIT, OUT);
                break;
            case 221:
                returnValue = getTransitSkim(WTD, EB, FWAIT, IN);
                break;
            case 222:
                returnValue = getTransitSkim(WTD, EB, XWAIT, OUT);
                break;
            case 223:
                returnValue = getTransitSkim(WTD, EB, XWAIT, IN);
                break;
            case 224:
                returnValue = getTransitSkim(WTD, EB, ACC, OUT);
                break;
            case 225:
                returnValue = getTransitSkim(WTD, EB, ACC, IN);
                break;
            case 226:
                returnValue = getTransitSkim(WTD, EB, EGR, OUT);
                break;
            case 227:
                returnValue = getTransitSkim(WTD, EB, EGR, IN);
                break;
            case 228:
                returnValue = getTransitSkim(WTD, EB, AUX, OUT);
                break;
            case 229:
                returnValue = getTransitSkim(WTD, EB, AUX, IN);
                break;
            case 230:
                returnValue = getTransitSkim(WTD, EB, FARE, OUT);
                break;
            case 231:
                returnValue = getTransitSkim(WTD, EB, FARE, IN);
                break;
            case 232:
                returnValue = getTransitSkim(WTD, EB, XFERS, OUT);
                break;
            case 233:
                returnValue = getTransitSkim(WTD, EB, XFERS, IN);
                break;
            case 234:
                returnValue = getTransitSkim(WTD, BRT, LB_IVT, OUT);
                break;
            case 235:
                returnValue = getTransitSkim(WTD, BRT, LB_IVT, IN);
                break;
            case 236:
                returnValue = getTransitSkim(WTD, BRT, EB_IVT, OUT);
                break;
            case 237:
                returnValue = getTransitSkim(WTD, BRT, EB_IVT, IN);
                break;
            case 238:
                returnValue = getTransitSkim(WTD, BRT, BRT_IVT, OUT);
                break;
            case 239:
                returnValue = getTransitSkim(WTD, BRT, BRT_IVT, IN);
                break;
            case 240:
                returnValue = getTransitSkim(WTD, BRT, FWAIT, OUT);
                break;
            case 241:
                returnValue = getTransitSkim(WTD, BRT, FWAIT, IN);
                break;
            case 242:
                returnValue = getTransitSkim(WTD, BRT, XWAIT, OUT);
                break;
            case 243:
                returnValue = getTransitSkim(WTD, BRT, XWAIT, IN);
                break;
            case 244:
                returnValue = getTransitSkim(WTD, BRT, ACC, OUT);
                break;
            case 245:
                returnValue = getTransitSkim(WTD, BRT, ACC, IN);
                break;
            case 246:
                returnValue = getTransitSkim(WTD, BRT, EGR, OUT);
                break;
            case 247:
                returnValue = getTransitSkim(WTD, BRT, EGR, IN);
                break;
            case 248:
                returnValue = getTransitSkim(WTD, BRT, AUX, OUT);
                break;
            case 249:
                returnValue = getTransitSkim(WTD, BRT, AUX, IN);
                break;
            case 250:
                returnValue = getTransitSkim(WTD, BRT, FARE, OUT);
                break;
            case 251:
                returnValue = getTransitSkim(WTD, BRT, FARE, IN);
                break;
            case 252:
                returnValue = getTransitSkim(WTD, BRT, XFERS, OUT);
                break;
            case 253:
                returnValue = getTransitSkim(WTD, BRT, XFERS, IN);
                break;
            case 254:
                returnValue = getTransitSkim(WTD, LR, LB_IVT, OUT);
                break;
            case 255:
                returnValue = getTransitSkim(WTD, LR, LB_IVT, IN);
                break;
            case 256:
                returnValue = getTransitSkim(WTD, LR, EB_IVT, OUT);
                break;
            case 257:
                returnValue = getTransitSkim(WTD, LR, EB_IVT, IN);
                break;
            case 258:
                returnValue = getTransitSkim(WTD, LR, BRT_IVT, OUT);
                break;
            case 259:
                returnValue = getTransitSkim(WTD, LR, BRT_IVT, IN);
                break;
            case 260:
                returnValue = getTransitSkim(WTD, LR, LR_IVT, OUT);
                break;
            case 261:
                returnValue = getTransitSkim(WTD, LR, LR_IVT, IN);
                break;
            case 262:
                returnValue = getTransitSkim(WTD, LR, FWAIT, OUT);
                break;
            case 263:
                returnValue = getTransitSkim(WTD, LR, FWAIT, IN);
                break;
            case 264:
                returnValue = getTransitSkim(WTD, LR, XWAIT, OUT);
                break;
            case 265:
                returnValue = getTransitSkim(WTD, LR, XWAIT, IN);
                break;
            case 266:
                returnValue = getTransitSkim(WTD, LR, ACC, OUT);
                break;
            case 267:
                returnValue = getTransitSkim(WTD, LR, ACC, IN);
                break;
            case 268:
                returnValue = getTransitSkim(WTD, LR, EGR, OUT);
                break;
            case 269:
                returnValue = getTransitSkim(WTD, LR, EGR, IN);
                break;
            case 270:
                returnValue = getTransitSkim(WTD, LR, AUX, OUT);
                break;
            case 271:
                returnValue = getTransitSkim(WTD, LR, AUX, IN);
                break;
            case 272:
                returnValue = getTransitSkim(WTD, LR, FARE, OUT);
                break;
            case 273:
                returnValue = getTransitSkim(WTD, LR, FARE, IN);
                break;
            case 274:
                returnValue = getTransitSkim(WTD, LR, XFERS, OUT);
                break;
            case 275:
                returnValue = getTransitSkim(WTD, LR, XFERS, IN);
                break;
            case 276:
                returnValue = getTransitSkim(WTD, CR, LB_IVT, OUT);
                break;
            case 277:
                returnValue = getTransitSkim(WTD, CR, LB_IVT, IN);
                break;
            case 278:
                returnValue = getTransitSkim(WTD, CR, EB_IVT, OUT);
                break;
            case 279:
                returnValue = getTransitSkim(WTD, CR, EB_IVT, IN);
                break;
            case 280:
                returnValue = getTransitSkim(WTD, CR, BRT_IVT, OUT);
                break;
            case 281:
                returnValue = getTransitSkim(WTD, CR, BRT_IVT, IN);
                break;
            case 282:
                returnValue = getTransitSkim(WTD, CR, LR_IVT, OUT);
                break;
            case 283:
                returnValue = getTransitSkim(WTD, CR, LR_IVT, IN);
                break;
            case 284:
                returnValue = getTransitSkim(WTD, CR, CR_IVT, OUT);
                break;
            case 285:
                returnValue = getTransitSkim(WTD, CR, CR_IVT, IN);
                break;
            case 286:
                returnValue = getTransitSkim(WTD, CR, FWAIT, OUT);
                break;
            case 287:
                returnValue = getTransitSkim(WTD, CR, FWAIT, IN);
                break;
            case 288:
                returnValue = getTransitSkim(WTD, CR, XWAIT, OUT);
                break;
            case 289:
                returnValue = getTransitSkim(WTD, CR, XWAIT, IN);
                break;
            case 290:
                returnValue = getTransitSkim(WTD, CR, ACC, OUT);
                break;
            case 291:
                returnValue = getTransitSkim(WTD, CR, ACC, IN);
                break;
            case 292:
                returnValue = getTransitSkim(WTD, CR, EGR, OUT);
                break;
            case 293:
                returnValue = getTransitSkim(WTD, CR, EGR, IN);
                break;
            case 294:
                returnValue = getTransitSkim(WTD, CR, AUX, OUT);
                break;
            case 295:
                returnValue = getTransitSkim(WTD, CR, AUX, IN);
                break;
            case 296:
                returnValue = getTransitSkim(WTD, CR, FARE, OUT);
                break;
            case 297:
                returnValue = getTransitSkim(WTD, CR, FARE, IN);
                break;
            case 298:
                returnValue = getTransitSkim(WTD, CR, XFERS, OUT);
                break;
            case 299:
                returnValue = getTransitSkim(WTD, CR, XFERS, IN);
                break;
            case 300:
                returnValue = getTransitSkim(DTW, LB, LB_IVT, OUT);
                break;
            case 301:
                returnValue = getTransitSkim(DTW, LB, LB_IVT, IN);
                break;
            case 302:
                returnValue = getTransitSkim(DTW, LB, FWAIT, OUT);
                break;
            case 303:
                returnValue = getTransitSkim(DTW, LB, FWAIT, IN);
                break;
            case 304:
                returnValue = getTransitSkim(DTW, LB, XWAIT, OUT);
                break;
            case 305:
                returnValue = getTransitSkim(DTW, LB, XWAIT, IN);
                break;
            case 306:
                returnValue = getTransitSkim(DTW, LB, ACC, OUT);
                break;
            case 307:
                returnValue = getTransitSkim(DTW, LB, ACC, IN);
                break;
            case 308:
                returnValue = getTransitSkim(DTW, LB, EGR, OUT);
                break;
            case 309:
                returnValue = getTransitSkim(DTW, LB, EGR, IN);
                break;
            case 310:
                returnValue = getTransitSkim(DTW, LB, AUX, OUT);
                break;
            case 311:
                returnValue = getTransitSkim(DTW, LB, AUX, IN);
                break;
            case 312:
                returnValue = getTransitSkim(DTW, LB, FARE, OUT);
                break;
            case 313:
                returnValue = getTransitSkim(DTW, LB, FARE, IN);
                break;
            case 314:
                returnValue = getTransitSkim(DTW, LB, XFERS, OUT);
                break;
            case 315:
                returnValue = getTransitSkim(DTW, LB, XFERS, IN);
                break;
            case 316:
                returnValue = getTransitSkim(DTW, EB, LB_IVT, OUT);
                break;
            case 317:
                returnValue = getTransitSkim(DTW, EB, LB_IVT, IN);
                break;
            case 318:
                returnValue = getTransitSkim(DTW, EB, EB_IVT, OUT);
                break;
            case 319:
                returnValue = getTransitSkim(DTW, EB, EB_IVT, IN);
                break;
            case 320:
                returnValue = getTransitSkim(DTW, EB, FWAIT, OUT);
                break;
            case 321:
                returnValue = getTransitSkim(DTW, EB, FWAIT, IN);
                break;
            case 322:
                returnValue = getTransitSkim(DTW, EB, XWAIT, OUT);
                break;
            case 323:
                returnValue = getTransitSkim(DTW, EB, XWAIT, IN);
                break;
            case 324:
                returnValue = getTransitSkim(DTW, EB, ACC, OUT);
                break;
            case 325:
                returnValue = getTransitSkim(DTW, EB, ACC, IN);
                break;
            case 326:
                returnValue = getTransitSkim(DTW, EB, EGR, OUT);
                break;
            case 327:
                returnValue = getTransitSkim(DTW, EB, EGR, IN);
                break;
            case 328:
                returnValue = getTransitSkim(DTW, EB, AUX, OUT);
                break;
            case 329:
                returnValue = getTransitSkim(DTW, EB, AUX, IN);
                break;
            case 330:
                returnValue = getTransitSkim(DTW, EB, FARE, OUT);
                break;
            case 331:
                returnValue = getTransitSkim(DTW, EB, FARE, IN);
                break;
            case 332:
                returnValue = getTransitSkim(DTW, EB, XFERS, OUT);
                break;
            case 333:
                returnValue = getTransitSkim(DTW, EB, XFERS, IN);
                break;
            case 334:
                returnValue = getTransitSkim(DTW, BRT, LB_IVT, OUT);
                break;
            case 335:
                returnValue = getTransitSkim(DTW, BRT, LB_IVT, IN);
                break;
            case 336:
                returnValue = getTransitSkim(DTW, BRT, EB_IVT, OUT);
                break;
            case 337:
                returnValue = getTransitSkim(DTW, BRT, EB_IVT, IN);
                break;
            case 338:
                returnValue = getTransitSkim(DTW, BRT, BRT_IVT, OUT);
                break;
            case 339:
                returnValue = getTransitSkim(DTW, BRT, BRT_IVT, IN);
                break;
            case 340:
                returnValue = getTransitSkim(DTW, BRT, FWAIT, OUT);
                break;
            case 341:
                returnValue = getTransitSkim(DTW, BRT, FWAIT, IN);
                break;
            case 342:
                returnValue = getTransitSkim(DTW, BRT, XWAIT, OUT);
                break;
            case 343:
                returnValue = getTransitSkim(DTW, BRT, XWAIT, IN);
                break;
            case 344:
                returnValue = getTransitSkim(DTW, BRT, ACC, OUT);
                break;
            case 345:
                returnValue = getTransitSkim(DTW, BRT, ACC, IN);
                break;
            case 346:
                returnValue = getTransitSkim(DTW, BRT, EGR, OUT);
                break;
            case 347:
                returnValue = getTransitSkim(DTW, BRT, EGR, IN);
                break;
            case 348:
                returnValue = getTransitSkim(DTW, BRT, AUX, OUT);
                break;
            case 349:
                returnValue = getTransitSkim(DTW, BRT, AUX, IN);
                break;
            case 350:
                returnValue = getTransitSkim(DTW, BRT, FARE, OUT);
                break;
            case 351:
                returnValue = getTransitSkim(DTW, BRT, FARE, IN);
                break;
            case 352:
                returnValue = getTransitSkim(DTW, BRT, XFERS, OUT);
                break;
            case 353:
                returnValue = getTransitSkim(DTW, BRT, XFERS, IN);
                break;
            case 354:
                returnValue = getTransitSkim(DTW, LR, LB_IVT, OUT);
                break;
            case 355:
                returnValue = getTransitSkim(DTW, LR, LB_IVT, IN);
                break;
            case 356:
                returnValue = getTransitSkim(DTW, LR, EB_IVT, OUT);
                break;
            case 357:
                returnValue = getTransitSkim(DTW, LR, EB_IVT, IN);
                break;
            case 358:
                returnValue = getTransitSkim(DTW, LR, BRT_IVT, OUT);
                break;
            case 359:
                returnValue = getTransitSkim(DTW, LR, BRT_IVT, IN);
                break;
            case 360:
                returnValue = getTransitSkim(DTW, LR, LR_IVT, OUT);
                break;
            case 361:
                returnValue = getTransitSkim(DTW, LR, LR_IVT, IN);
                break;
            case 362:
                returnValue = getTransitSkim(DTW, LR, FWAIT, OUT);
                break;
            case 363:
                returnValue = getTransitSkim(DTW, LR, FWAIT, IN);
                break;
            case 364:
                returnValue = getTransitSkim(DTW, LR, XWAIT, OUT);
                break;
            case 365:
                returnValue = getTransitSkim(DTW, LR, XWAIT, IN);
                break;
            case 366:
                returnValue = getTransitSkim(DTW, LR, ACC, OUT);
                break;
            case 367:
                returnValue = getTransitSkim(DTW, LR, ACC, IN);
                break;
            case 368:
                returnValue = getTransitSkim(DTW, LR, EGR, OUT);
                break;
            case 369:
                returnValue = getTransitSkim(DTW, LR, EGR, IN);
                break;
            case 370:
                returnValue = getTransitSkim(DTW, LR, AUX, OUT);
                break;
            case 371:
                returnValue = getTransitSkim(DTW, LR, AUX, IN);
                break;
            case 372:
                returnValue = getTransitSkim(DTW, LR, FARE, OUT);
                break;
            case 373:
                returnValue = getTransitSkim(DTW, LR, FARE, IN);
                break;
            case 374:
                returnValue = getTransitSkim(DTW, LR, XFERS, OUT);
                break;
            case 375:
                returnValue = getTransitSkim(DTW, LR, XFERS, IN);
                break;
            case 376:
                returnValue = getTransitSkim(DTW, CR, LB_IVT, OUT);
                break;
            case 377:
                returnValue = getTransitSkim(DTW, CR, LB_IVT, IN);
                break;
            case 378:
                returnValue = getTransitSkim(DTW, CR, EB_IVT, OUT);
                break;
            case 379:
                returnValue = getTransitSkim(DTW, CR, EB_IVT, IN);
                break;
            case 380:
                returnValue = getTransitSkim(DTW, CR, BRT_IVT, OUT);
                break;
            case 381:
                returnValue = getTransitSkim(DTW, CR, BRT_IVT, IN);
                break;
            case 382:
                returnValue = getTransitSkim(DTW, CR, LR_IVT, OUT);
                break;
            case 383:
                returnValue = getTransitSkim(DTW, CR, LR_IVT, IN);
                break;
            case 384:
                returnValue = getTransitSkim(DTW, CR, CR_IVT, OUT);
                break;
            case 385:
                returnValue = getTransitSkim(DTW, CR, CR_IVT, IN);
                break;
            case 386:
                returnValue = getTransitSkim(DTW, CR, FWAIT, OUT);
                break;
            case 387:
                returnValue = getTransitSkim(DTW, CR, FWAIT, IN);
                break;
            case 388:
                returnValue = getTransitSkim(DTW, CR, XWAIT, OUT);
                break;
            case 389:
                returnValue = getTransitSkim(DTW, CR, XWAIT, IN);
                break;
            case 390:
                returnValue = getTransitSkim(DTW, CR, ACC, OUT);
                break;
            case 391:
                returnValue = getTransitSkim(DTW, CR, ACC, IN);
                break;
            case 392:
                returnValue = getTransitSkim(DTW, CR, EGR, OUT);
                break;
            case 393:
                returnValue = getTransitSkim(DTW, CR, EGR, IN);
                break;
            case 394:
                returnValue = getTransitSkim(DTW, CR, AUX, OUT);
                break;
            case 395:
                returnValue = getTransitSkim(DTW, CR, AUX, IN);
                break;
            case 396:
                returnValue = getTransitSkim(DTW, CR, FARE, OUT);
                break;
            case 397:
                returnValue = getTransitSkim(DTW, CR, FARE, IN);
                break;
            case 398:
                returnValue = getTransitSkim(DTW, CR, XFERS, OUT);
                break;
            case 399:
                returnValue = getTransitSkim(DTW, CR, XFERS, IN);
                break;

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

        return returnValue;

    }

}