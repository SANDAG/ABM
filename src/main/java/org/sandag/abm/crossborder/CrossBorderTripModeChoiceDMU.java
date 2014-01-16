package org.sandag.abm.crossborder;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.common.OutboundHalfTourDMU;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class CrossBorderTripModeChoiceDMU
        extends OutboundHalfTourDMU
        implements Serializable, VariableTable
{

    protected int          tourDepartPeriod;
    protected int          tourArrivePeriod;
    protected int          tripPeriod;
    protected int          workTour;
    protected int          outboundStops;
    protected int          returnStops;
    protected int          firstTrip;
    protected int          lastTrip;
    protected int          tourModeIsDA;
    protected int          tourModeIsS2;
    protected int          tourModeIsS3;
    protected int          tourModeIsWalk;
    protected int          tourCrossingIsSentri;
    protected float        hourlyParkingCostTourDest;
    protected float        dailyParkingCostTourDest;
    protected float        monthlyParkingCostTourDest;
    protected int          tripOrigIsTourDest;
    protected int          tripDestIsTourDest;
    protected float        hourlyParkingCostTripOrig;
    protected float        hourlyParkingCostTripDest;

    protected double       nmWalkTime;
    protected double       nmBikeTime;

    protected double[][][] transitSkim;

    public CrossBorderTripModeChoiceDMU(CrossBorderModelStructure modelStructure, Logger aLogger)
    {
        if (aLogger == null)
        {
            aLogger = Logger.getLogger("crossBorderModel");
        }
        logger = aLogger;
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
        transitSkim = new double[McLogsumsCalculator.NUM_ACC_EGR][McLogsumsCalculator.NUM_LOC_PREM][McLogsumsCalculator.NUM_SKIMS];

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
     * @return the workTour
     */
    public int getWorkTour()
    {
        return workTour;
    }

    /**
     * @param workTour
     *            the workTour to set
     */
    public void setWorkTour(int workTour)
    {
        this.workTour = workTour;
    }

    /**
     * @return the outboundStops
     */
    public int getOutboundStops()
    {
        return outboundStops;
    }

    /**
     * @param outboundStops
     *            the outboundStops to set
     */
    public void setOutboundStops(int outboundStops)
    {
        this.outboundStops = outboundStops;
    }

    /**
     * @return the returnStops
     */
    public int getReturnStops()
    {
        return returnStops;
    }

    /**
     * @param returnStops
     *            the returnStops to set
     */
    public void setReturnStops(int returnStops)
    {
        this.returnStops = returnStops;
    }

    /**
     * @return the firstTrip
     */
    public int getFirstTrip()
    {
        return firstTrip;
    }

    /**
     * @param firstTrip
     *            the firstTrip to set
     */
    public void setFirstTrip(int firstTrip)
    {
        this.firstTrip = firstTrip;
    }

    /**
     * @return the lastTrip
     */
    public int getLastTrip()
    {
        return lastTrip;
    }

    /**
     * @param lastTrip
     *            the lastTrip to set
     */
    public void setLastTrip(int lastTrip)
    {
        this.lastTrip = lastTrip;
    }

    /**
     * @return the tourModeIsDA
     */
    public int getTourModeIsDA()
    {
        return tourModeIsDA;
    }

    /**
     * @param tourModeIsDA
     *            the tourModeIsDA to set
     */
    public void setTourModeIsDA(int tourModeIsDA)
    {
        this.tourModeIsDA = tourModeIsDA;
    }

    /**
     * @return the tourModeIsS2
     */
    public int getTourModeIsS2()
    {
        return tourModeIsS2;
    }

    /**
     * @param tourModeIsS2
     *            the tourModeIsS2 to set
     */
    public void setTourModeIsS2(int tourModeIsS2)
    {
        this.tourModeIsS2 = tourModeIsS2;
    }

    /**
     * @return the tourModeIsS3
     */
    public int getTourModeIsS3()
    {
        return tourModeIsS3;
    }

    /**
     * @param tourModeIsS3
     *            the tourModeIsS3 to set
     */
    public void setTourModeIsS3(int tourModeIsS3)
    {
        this.tourModeIsS3 = tourModeIsS3;
    }

    /**
     * @return the tourModeIsWalk
     */
    public int getTourModeIsWalk()
    {
        return tourModeIsWalk;
    }

    /**
     * @param tourModeIsWalk
     *            the tourModeIsWalk to set
     */
    public void setTourModeIsWalk(int tourModeIsWalk)
    {
        this.tourModeIsWalk = tourModeIsWalk;
    }

    /**
     * @return the tourModeIsSentri
     */
    public int getTourCrossingIsSentri()
    {
        return tourCrossingIsSentri;
    }

    /**
     * @param tourModeIsSentri
     *            the tourModeIsSentri to set
     */
    public void setTourCrossingIsSentri(int tourCrossingIsSentri)
    {
        this.tourCrossingIsSentri = tourCrossingIsSentri;
    }

    /**
     * @return the hourlyParkingCostTourDest
     */
    public float getHourlyParkingCostTourDest()
    {
        return hourlyParkingCostTourDest;
    }

    /**
     * @param hourlyParkingCostTourDest
     *            the hourlyParkingCostTourDest to set
     */
    public void setHourlyParkingCostTourDest(float hourlyParkingCostTourDest)
    {
        this.hourlyParkingCostTourDest = hourlyParkingCostTourDest;
    }

    /**
     * @return the dailyParkingCostTourDest
     */
    public float getDailyParkingCostTourDest()
    {
        return dailyParkingCostTourDest;
    }

    /**
     * @param dailyParkingCostTourDest
     *            the dailyParkingCostTourDest to set
     */
    public void setDailyParkingCostTourDest(float dailyParkingCostTourDest)
    {
        this.dailyParkingCostTourDest = dailyParkingCostTourDest;
    }

    /**
     * @return the monthlyParkingCostTourDest
     */
    public float getMonthlyParkingCostTourDest()
    {
        return monthlyParkingCostTourDest;
    }

    /**
     * @param monthlyParkingCostTourDest
     *            the monthlyParkingCostTourDest to set
     */
    public void setMonthlyParkingCostTourDest(float monthlyParkingCostTourDest)
    {
        this.monthlyParkingCostTourDest = monthlyParkingCostTourDest;
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
     * @return the hourlyParkingCostTripOrig
     */
    public float getHourlyParkingCostTripOrig()
    {
        return hourlyParkingCostTripOrig;
    }

    /**
     * @param hourlyParkingCostTripOrig
     *            the hourlyParkingCostTripOrig to set
     */
    public void setHourlyParkingCostTripOrig(float hourlyParkingCostTripOrig)
    {
        this.hourlyParkingCostTripOrig = hourlyParkingCostTripOrig;
    }

    /**
     * @return the hourlyParkingCostTripDest
     */
    public float getHourlyParkingCostTripDest()
    {
        return hourlyParkingCostTripDest;
    }

    /**
     * @param hourlyParkingCostTripDest
     *            the hourlyParkingCostTripDest to set
     */
    public void setHourlyParkingCostTripDest(float hourlyParkingCostTripDest)
    {
        this.hourlyParkingCostTripDest = hourlyParkingCostTripDest;
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

    public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, double value)
    {
        transitSkim[accEgr][lbPrem][skimIndex] = value;
    }

    public double getTransitSkim(int accEgr, int lbPrem, int skimIndex)
    {
        return transitSkim[accEgr][lbPrem][skimIndex];
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTourDepartPeriod", 0);
        methodIndexMap.put("getTourArrivePeriod", 1);
        methodIndexMap.put("getTripPeriod", 2);

        methodIndexMap.put("getWorkTour", 4);
        methodIndexMap.put("getOutboundStops", 5);
        methodIndexMap.put("getReturnStops", 6);
        methodIndexMap.put("getFirstTrip", 7);
        methodIndexMap.put("getLastTrip", 8);
        methodIndexMap.put("getTourModeIsDA", 9);
        methodIndexMap.put("getTourModeIsS2", 10);
        methodIndexMap.put("getTourModeIsS3", 11);
        methodIndexMap.put("getTourModeIsWalk", 12);
        methodIndexMap.put("getTourCrossingIsSentri", 13);
        methodIndexMap.put("getHourlyParkingCostTourDest", 14);
        methodIndexMap.put("getDailyParkingCostTourDest", 15);
        methodIndexMap.put("getMonthlyParkingCostTourDest", 16);
        methodIndexMap.put("getTripOrigIsTourDest", 17);
        methodIndexMap.put("getTripDestIsTourDest", 18);
        methodIndexMap.put("getHourlyParkingCostTripOrig", 19);
        methodIndexMap.put("getHourlyParkingCostTripDest", 20);

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
        CreateReverseMap();

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        return getValueForIndexLookup(variableIndex, arrayIndex);
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