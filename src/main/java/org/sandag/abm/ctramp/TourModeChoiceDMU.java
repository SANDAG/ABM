package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagTourModeChoiceDMU;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TourModeChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger       = null;

    protected static final int         LB           = McLogsumsCalculator.LB;
    protected static final int         EB           = McLogsumsCalculator.EB;
    protected static final int         BRT          = McLogsumsCalculator.BRT;
    protected static final int         LR           = McLogsumsCalculator.LR;
    protected static final int         CR           = McLogsumsCalculator.CR;
    protected static final int         NUM_LOC_PREM = McLogsumsCalculator.NUM_LOC_PREM;

    protected static final int         WTW          = McLogsumsCalculator.WTW;
    protected static final int         WTD          = McLogsumsCalculator.WTD;
    protected static final int         DTW          = McLogsumsCalculator.DTW;
    protected static final int         NUM_ACC_EGR  = McLogsumsCalculator.NUM_ACC_EGR;

    protected static final int         LB_IVT       = McLogsumsCalculator.LB_IVT;
    protected static final int         EB_IVT       = McLogsumsCalculator.EB_IVT;
    protected static final int         BRT_IVT      = McLogsumsCalculator.BRT_IVT;
    protected static final int         LR_IVT       = McLogsumsCalculator.LR_IVT;
    protected static final int         CR_IVT       = McLogsumsCalculator.CR_IVT;
    protected static final int         ACC          = McLogsumsCalculator.ACC;
    protected static final int         EGR          = McLogsumsCalculator.EGR;
    protected static final int         AUX          = McLogsumsCalculator.AUX;
    protected static final int         FWAIT        = McLogsumsCalculator.FWAIT;
    protected static final int         XWAIT        = McLogsumsCalculator.XWAIT;
    protected static final int         FARE         = McLogsumsCalculator.FARE;
    protected static final int         XFERS        = McLogsumsCalculator.XFERS;
    protected static final int         NUM_SKIMS    = McLogsumsCalculator.NUM_SKIMS;

    protected static final int         OUT          = McLogsumsCalculator.OUT;
    protected static final int         IN           = McLogsumsCalculator.IN;
    protected static final int         NUM_DIR      = McLogsumsCalculator.NUM_DIR;

    protected HashMap<String, Integer> methodIndexMap;
    protected HashMap<Integer, String> reverseMethodIndexMap;
    protected IndexValues              dmuIndex;

    protected Household                hh;
    protected Tour                     tour;
    protected Tour                     workTour;
    protected Person                   person;

    protected ModelStructure           modelStructure;

    protected double                   origDuDen;
    protected double                   origEmpDen;
    protected double                   origTotInt;
    protected double                   destDuDen;
    protected double                   destEmpDen;
    protected double                   destTotInt;

    protected double                   lsWgtAvgCostM;
    protected double                   lsWgtAvgCostD;
    protected double                   lsWgtAvgCostH;
    protected double                   reimburseProportion;
    protected int                      parkingArea;

    protected float                    pTazTerminalTime;
    protected float                    aTazTerminalTime;

    protected double                   nmWalkTimeOut;
    protected double                   nmWalkTimeIn;
    protected double                   nmBikeTimeOut;
    protected double                   nmBikeTimeIn;

    protected double[][][][]           transitSkim;

    public TourModeChoiceDMU(ModelStructure modelStructure, Logger aLogger)
    {
    	logger = aLogger;
    	if ( logger == null )
    		logger = Logger.getLogger(TourModeChoiceDMU.class);
        this.modelStructure = modelStructure;
        dmuIndex = new IndexValues();

        transitSkim = new double[McLogsumsCalculator.NUM_ACC_EGR][McLogsumsCalculator.NUM_LOC_PREM][McLogsumsCalculator.NUM_SKIMS][McLogsumsCalculator.NUM_DIR];

    }

    public void setHouseholdObject(Household hhObject)
    {
        hh = hhObject;
    }

    public Household getHouseholdObject()
    {
        return hh;
    }

    public void setPersonObject(Person personObject)
    {
        person = personObject;
    }

    public Person getPersonObject()
    {
        return person;
    }

    public void setTourObject(Tour tourObject)
    {
        tour = tourObject;
    }

    public void setWorkTourObject(Tour tourObject)
    {
        workTour = tourObject;
    }

    public Tour getTourObject()
    {
        return tour;
    }

    public int getParkingArea()
    {
        return parkingArea;
    }

    public void setParkingArea(int parkingArea)
    {
        this.parkingArea = parkingArea;
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

    public int getPersonType()
    {
        return person.getPersonTypeNumber();
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

    public void setReimburseProportion(double proportion)
    {
        reimburseProportion = proportion;
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

    public void setPTazTerminalTime(float time)
    {
        pTazTerminalTime = time;
    }

    public void setATazTerminalTime(float time)
    {
        aTazTerminalTime = time;
    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public void setIndexDest(int d)
    {
        dmuIndex.setDestZone(d);
    }

    public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir, double value)
    {
        transitSkim[accEgr][lbPrem][skimIndex][dir] = value;
    }

    public double getTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir)
    {
        return transitSkim[accEgr][lbPrem][skimIndex][dir];
    }

    public int getWorkTourModeIsSov()
    {
        boolean tourModeIsSov = modelStructure.getTourModeIsSov(workTour.getTourModeChoice());
        return tourModeIsSov ? 1 : 0;
    }

    public int getWorkTourModeIsHov()
    {
        boolean tourModeIsHov = modelStructure.getTourModeIsHov(workTour.getTourModeChoice());
        return tourModeIsHov ? 1 : 0;
    }

    public int getWorkTourModeIsBike()
    {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike(workTour.getTourModeChoice());
        return tourModeIsBike ? 1 : 0;
    }

    public int getTourCategoryJoint()
    {
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) return 1;
        else return 0;
    }

    public int getTourCategoryEscort()
    {
        if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourCategorySubtour()
    {
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY)) return 1;
        else return 0;
    }

    public int getNumberOfParticipantsInJointTour()
    {
        int[] participants = tour.getPersonNumArray();
        int returnValue = 0;
        if (participants != null) returnValue = participants.length;
        return returnValue;
    }

    public int getHhSize()
    {
        return hh.getHhSize();
    }

    public int getAutos()
    {
        return hh.getAutoOwnershipModelResult();
    }

    public int getAge()
    {
        return person.getAge();
    }

    public int getIncome()
    {
        return hh.getIncome();
    }

    public void setNmWalkTimeOut(double nmWalkTime)
    {
        nmWalkTimeOut = nmWalkTime;
    }

    public double getNmWalkTimeOut()
    {
        return nmWalkTimeOut;
    }

    public void setNmWalkTimeIn(double nmWalkTime)
    {
        nmWalkTimeIn = nmWalkTime;
    }

    public double getNmWalkTimeIn()
    {
        return nmWalkTimeIn;
    }

    public void setNmBikeTimeOut(double nmBikeTime)
    {
        nmBikeTimeOut = nmBikeTime;
    }

    public double getNmBikeTimeOut()
    {
        return nmBikeTimeOut;
    }

    public void setNmBikeTimeIn(double nmBikeTime)
    {
        nmBikeTimeIn = nmBikeTime;
    }

    public double getNmBikeTimeIn()
    {
        return nmBikeTimeIn;
    }

    public int getFreeParkingEligibility()
    {
        return person.getFreeParkingAvailableResult();
    }

    public double getReimburseProportion()
    {
        return reimburseProportion;
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

    public double getPTazTerminalTime()
    {
        return pTazTerminalTime;
    }

    public double getATazTerminalTime()
    {
        return aTazTerminalTime;
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

    public double getValueForIndex(int variableIndex, int arrayIndex)
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

    protected void CreateReverseMap() {
    	reverseMethodIndexMap = new HashMap<Integer, String>();
		Iterator<String> i = methodIndexMap.keySet().iterator();
		while (i.hasNext())
		{
			String value = i.next();
			Integer key = methodIndexMap.get(value);
			reverseMethodIndexMap.put(key, value);
		}
		
	}
    
    protected double getValueForIndexLookup(int variableIndex, int arrayIndex) {
		if (variableIndex < 100)
    	{
			try {
				Method method = this.getClass().getMethod(reverseMethodIndexMap.get(variableIndex));
				Object o = method.invoke(this);
				if (o instanceof Double)
					return (double)o;
				else if (o instanceof Float)
					return ((Float)o).doubleValue(); 
				else if (o instanceof Integer)
					return ((Integer)o).doubleValue();
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");
			}
    	}
    	
    	try {
			return getTransitSkimFromMethodName(reverseMethodIndexMap.get(variableIndex));
		} catch (Exception e) {
			logger.error("method number = " + variableIndex + " not found");
            throw new RuntimeException("method number = " + variableIndex + " not found");
		}		
	}
    
    protected double getTransitSkimFromMethodName(String methodName) throws Exception 
	{
    	//"getWtw_lb_LB_ivt_out"
    	String[] parts = methodName.split("_");
    	String part1 = parts[0].replace("get", ""); 
    	String part2 = parts[1];
    	boolean part3HasUnderscore = (parts.length > 4);
    	String part3 = parts[2];
    	String part4 = parts[3];
    	if (part3HasUnderscore)
    	{
    		part3 += "_" + parts[3];
    		part4 = parts[4];
    	}
    	part3 = part3.replace("Time", "").replace("Walk", "");
    	
    	int var1 = -1, var2 = -1, var3 = -1, var4 = -1;
    	
    	if (part1.equalsIgnoreCase("WTW"))
    		var1 = WTW;
    	else if (part1.equalsIgnoreCase("WTD"))
    		var1 = WTD;
    	else if (part1.equalsIgnoreCase("DTW"))
    		var1 = DTW;
    	else
    		throw new Exception("First part of getTransitSkim is invalid with variable " + var1);
    	    	
    	if (part2.equalsIgnoreCase("LB"))
    		var2 = LB;
    	else if (part2.equalsIgnoreCase("EB"))
    		var2 = EB;
    	else if (part2.equalsIgnoreCase("BRT"))
    		var2 = BRT;
    	else if (part2.equalsIgnoreCase("LR"))
    		var2 = LR;
    	else if (part2.equalsIgnoreCase("CR"))
    		var2 = CR;
    	else
    		throw new Exception("Second part of getTransitSkim is invalid with variable " + var1);
    	
    	if (part3.equalsIgnoreCase("FWAIT"))
    		var3 = FWAIT;
    	else if (part3.equalsIgnoreCase("XWAIT"))
    		var3 = XWAIT;
    	else if (part3.equalsIgnoreCase("ACC"))
    		var3 = ACC;
    	else if (part3.equalsIgnoreCase("EGR"))
    		var3 = EGR;
    	else if (part3.equalsIgnoreCase("AUX"))
    		var3 = AUX;
    	else if (part3.equalsIgnoreCase("FARE"))
    		var3 = FARE;
    	else if (part3.equalsIgnoreCase("XFERS"))
    		var3 = XFERS;
    	else if (part3.equalsIgnoreCase("LB_IVT"))
    		var3 = LB_IVT;
    	else if (part3.equalsIgnoreCase("EB_IVT"))
    		var3 = EB_IVT;
    	else if (part3.equalsIgnoreCase("BRT_IVT"))
    		var3 = BRT_IVT;
    	else if (part3.equalsIgnoreCase("LRT_IVT")) //Note difference
    		var3 = LR_IVT;
    	else if (part3.equalsIgnoreCase("CR_IVT"))
    		var3 = CR_IVT;
    	else
    		throw new Exception("Third part of getTransitSkim is invalid with variable " + var1);
    	
    	if (part4.equalsIgnoreCase("OUT"))
    		var4 = OUT;
    	else if (part4.equalsIgnoreCase("IN"))
    		var4 = IN;
    	else
    		throw new Exception("First part of getTransitSkim is invalid with variable " + var1);
    	return getTransitSkim(var1, var2, var3, var4);    	
	}
}
