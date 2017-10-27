package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class VisitorTourModeChoiceDMU 
        implements Serializable, VariableTable
{
    protected transient Logger         logger = Logger.getLogger(VisitorTourModeChoiceDMU.class);

    public static final int                WTW = McLogsumsCalculator.WTW;
    public static final int                WTD = McLogsumsCalculator.WTD;
    public static final int                DTW = McLogsumsCalculator.DTW;
    protected static final int             NUM_ACC_EGR = McLogsumsCalculator.NUM_ACC_EGR;
        
    protected static final int             OUT = McLogsumsCalculator.OUT;
    protected static final int             IN = McLogsumsCalculator.IN;
    protected static final int             NUM_DIR = McLogsumsCalculator.NUM_DIR;

    protected HashMap<String, Integer> methodIndexMap;
    protected IndexValues              dmuIndex;

    protected float                    tourDepartPeriod;
    protected float                    tourArrivePeriod;
    protected double                   origDuDen;
    protected double                   origEmpDen;
    protected double                   origTotInt;
    protected double                   destDuDen;
    protected double                   destEmpDen;
    protected double                   destTotInt;

    protected int                      partySize;
    protected int                      autoAvailable;
    protected int                      income;
    protected int                      tourPurpose;

    protected float                    pTazTerminalTime;
    protected float                    aTazTerminalTime;

    protected double                   nmWalkTimeOut;
    protected double                   nmWalkTimeIn;
    protected double                   nmBikeTimeOut;
    protected double                   nmBikeTimeIn;
    protected double                   lsWgtAvgCostM;
    protected double                   lsWgtAvgCostD;
    protected double                   lsWgtAvgCostH;

    protected double[][]                 transitLogSum;

     public VisitorTourModeChoiceDMU(VisitorModelStructure modelStructure, Logger aLogger)
    {
        if (aLogger == null) aLogger = Logger.getLogger(TourModeChoiceDMU.class);
        logger = aLogger;
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
        
        //accEgr by in/outbound
        transitLogSum = new double[NUM_ACC_EGR][NUM_DIR];
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

    public void setLsWgtAvgCostM(double cost)
    {
        lsWgtAvgCostM = cost;
    }

    public void setLsWgtAvgCostD(double cost)
    {
        lsWgtAvgCostD = cost;
    }

    public void setLsWgtAvgCostH(double cost)
    {
        lsWgtAvgCostH = cost;
    }

    public double getMonthlyParkingCost()
    {
        return lsWgtAvgCostM;
    }

    public double getDailyParkingCost()
    {
        return lsWgtAvgCostD;
    }

    public double getHourlyParkingCost()
    {
        return lsWgtAvgCostH;
    }

    public float getTimeOutbound()
    {
        return tourDepartPeriod;
    }

    public float getTimeInbound()
    {
        return tourArrivePeriod;
    }

    /**
     * @param tourDepartPeriod
     *            the tourDepartPeriod to set
     */
    public void setTourDepartPeriod(float tourDepartPeriod)
    {
        this.tourDepartPeriod = tourDepartPeriod;
    }

    /**
     * @param tourArrivePeriod
     *            the tourArrivePeriod to set
     */
    public void setTourArrivePeriod(float tourArrivePeriod)
    {
        this.tourArrivePeriod = tourArrivePeriod;
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

    public int getTourPurpose()
    {
        return tourPurpose;
    }

    public void setTourPurpose(int tourPurpose)
    {
        this.tourPurpose = tourPurpose;
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

    public void setNmWalkTimeOut(double nmWalkTime)
    {
        nmWalkTimeOut = nmWalkTime;
    }

    public double getNm_walkTime_out()
    {
        return nmWalkTimeOut;
    }

    public void setNmWalkTimeIn(double nmWalkTime)
    {
        nmWalkTimeIn = nmWalkTime;
    }

    public double getNm_walkTime_in()
    {
        return nmWalkTimeIn;
    }

    public void setNmBikeTimeOut(double nmBikeTime)
    {
        nmBikeTimeOut = nmBikeTime;
    }

    public double getNm_bikeTime_out()
    {
        return nmBikeTimeOut;
    }

    public void setNmBikeTimeIn(double nmBikeTime)
    {
        nmBikeTimeIn = nmBikeTime;
    }

    public double getNm_bikeTime_in()
    {
        return nmBikeTimeIn;
    }

    public void setPTazTerminalTime(float time)
    {
        pTazTerminalTime = time;
    }

    public void setATazTerminalTime(float time)
    {
        aTazTerminalTime = time;
    }

    public double getPTazTerminalTime()
    {
        return pTazTerminalTime;
    }

    public double getATazTerminalTime()
    {
        return aTazTerminalTime;
    }

    public int getPartySize()
    {
        return partySize;
    }

    public void setPartySize(int partySize)
    {
        this.partySize = partySize;
    }

    public int getAutoAvailable()
    {
        return autoAvailable;
    }

    public void setAutoAvailable(int autoAvailable)
    {
        this.autoAvailable = autoAvailable;
    }

    public int getIncome()
    {
        return income;
    }

    public void setIncome(int income)
    {
        this.income = income;
    }
    
    public void setTransitLogSum(int accEgr, boolean inbound, double value){
    	transitLogSum[accEgr][inbound == true ? 1 : 0] = value;
    }

    protected double getTransitLogSum(int accEgr,boolean inbound){
        return transitLogSum[accEgr][inbound == true ? 1 : 0];
    }


    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTimeOutbound", 0);
        methodIndexMap.put("getTimeInbound", 1);
        methodIndexMap.put("getPTazTerminalTime", 14);
        methodIndexMap.put("getATazTerminalTime", 15);
        methodIndexMap.put("getODUDen", 16);
        methodIndexMap.put("getOEmpDen", 17);
        methodIndexMap.put("getOTotInt", 18);
        methodIndexMap.put("getDDUDen", 19);
        methodIndexMap.put("getDEmpDen", 20);
        methodIndexMap.put("getDTotInt", 21);
        methodIndexMap.put("getMonthlyParkingCost", 23);
        methodIndexMap.put("getDailyParkingCost", 24);
        methodIndexMap.put("getHourlyParkingCost", 25);
        methodIndexMap.put("getPartySize", 30);
        methodIndexMap.put("getAutoAvailable", 31);
        methodIndexMap.put("getIncome", 32);
        methodIndexMap.put("getTourPurpose", 33);

        methodIndexMap.put("getIvtCoeff", 56);
        methodIndexMap.put("getCostCoeff", 57);
        methodIndexMap.put("getWalkSetLogSum", 59);
        methodIndexMap.put("getPnrSetLogSum", 60);
        methodIndexMap.put("getKnrSetLogSum", 61);

        methodIndexMap.put("getNm_walkTime_out", 90);
        methodIndexMap.put("getNm_walkTime_in", 91);
        methodIndexMap.put("getNm_bikeTime_out", 92);
        methodIndexMap.put("getNm_bikeTime_in", 93);

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
        case 23:
        	returnValue = getMonthlyParkingCost();
            break;
        case 24:
        	returnValue = getDailyParkingCost();
            break;
        case 25:
        	returnValue = getHourlyParkingCost();
            break;
        case 30:
        	returnValue = getPartySize();
            break;
        case 31:
        	returnValue = getAutoAvailable();
            break;
        case 32:
        	returnValue = getIncome();
            break;
        case 33:
        	returnValue = getTourPurpose();
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
        default:
            logger.error("method number = " + variableIndex + " not found");
            throw new RuntimeException("method number = " + variableIndex + " not found");
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
