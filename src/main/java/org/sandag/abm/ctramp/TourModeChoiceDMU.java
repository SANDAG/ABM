package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TourModeChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger                logger                                    = Logger.getLogger(TourModeChoiceDMU.class);

    protected static final int                LB  = McLogsumsCalculator.LB;
    protected static final int                EB  = McLogsumsCalculator.EB;
    protected static final int                BRT = McLogsumsCalculator.BRT;
    protected static final int                LR  = McLogsumsCalculator.LR;
    protected static final int                CR  = McLogsumsCalculator.CR;
    protected static final int                NUM_LOC_PREM = McLogsumsCalculator.NUM_LOC_PREM;

    protected static final int                WTW = McLogsumsCalculator.WTW;
    protected static final int                WTD = McLogsumsCalculator.WTD;
    protected static final int                DTW = McLogsumsCalculator.DTW;
    protected static final int                NUM_ACC_EGR = McLogsumsCalculator.NUM_ACC_EGR;
    
    protected static final int                LB_IVT = McLogsumsCalculator.LB_IVT;
    protected static final int                EB_IVT = McLogsumsCalculator.EB_IVT;
    protected static final int                BRT_IVT = McLogsumsCalculator.BRT_IVT;
    protected static final int                LR_IVT = McLogsumsCalculator.LR_IVT;
    protected static final int                CR_IVT = McLogsumsCalculator.CR_IVT;
    protected static final int                ACC = McLogsumsCalculator.ACC;
    protected static final int                EGR = McLogsumsCalculator.EGR;
    protected static final int                AUX = McLogsumsCalculator.AUX;
    protected static final int                FWAIT = McLogsumsCalculator.FWAIT;
    protected static final int                XWAIT = McLogsumsCalculator.XWAIT;
    protected static final int                FARE = McLogsumsCalculator.FARE;
    protected static final int                XFERS = McLogsumsCalculator.XFERS;
    protected static final int                NUM_SKIMS = McLogsumsCalculator.NUM_SKIMS;
    
    protected static final int                OUT = McLogsumsCalculator.OUT;
    protected static final int                IN = McLogsumsCalculator.IN;
    protected static final int                NUM_DIR = McLogsumsCalculator.NUM_DIR;
    
    protected HashMap<String, Integer> methodIndexMap;
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

    protected double[][][][] transitSkim;
    

    public TourModeChoiceDMU(ModelStructure modelStructure)
    {
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

    public int getParkingArea() {
  		return parkingArea;
  	}


  	public void setParkingArea(int parkingArea) {
  		this.parkingArea = parkingArea;
  	}
  /**
     * Set this index values for this tour mode choice DMU object.
     * 
     * @param hhIndex is the DMU household index
     * @param zoneIndex is the DMU zone index
     * @param origIndex is the DMU origin index
     * @param destIndex is the DMU desatination index
     */
    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex, boolean debug)
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
    
    public void setReimburseProportion( double proportion ) {
        reimburseProportion = proportion;
    }
    
    public void setLsWgtAvgCostM( double cost ) {
        lsWgtAvgCostM = cost;
    }
    
    public void setLsWgtAvgCostD( double cost ) {
        lsWgtAvgCostD = cost;
    }
    
    public void setLsWgtAvgCostH( double cost ) {
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

    
    public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir, double value){
        transitSkim[accEgr][lbPrem][skimIndex][dir] = value;
    }

    protected double getTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir){
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
        if (tour.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)) return 1;
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

    public void setNmWalkTimeOut( double nmWalkTime )
    {
        nmWalkTimeOut = nmWalkTime;
    }
    
    public double getNmWalkTimeOut()
    {
        return nmWalkTimeOut;
    }
    
    public void setNmWalkTimeIn( double nmWalkTime )
    {
        nmWalkTimeIn = nmWalkTime;
    }
    
    public double getNmWalkTimeIn()
    {
        return nmWalkTimeIn;
    }
    
    public void setNmBikeTimeOut( double nmBikeTime )
    {
        nmBikeTimeOut = nmBikeTime;
    }
    
    public double getNmBikeTimeOut()
    {
        return nmBikeTimeOut;
    }
    
    public void setNmBikeTimeIn( double nmBikeTime )
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
           
    public double getReimburseProportion() {
        return reimburseProportion;
    }
    
    public double getLsWgtAvgCostM() {
        return lsWgtAvgCostM;
    }
    
    public double getLsWgtAvgCostD() {
        return lsWgtAvgCostD;
    }

    public double getLsWgtAvgCostH() {
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

}
