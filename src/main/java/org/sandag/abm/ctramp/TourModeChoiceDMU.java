package org.sandag.abm.ctramp;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.common.TourDMU;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TourModeChoiceDMU extends TourDMU implements Serializable,
		VariableTable {
	protected IndexValues dmuIndex;

	protected Household hh;
	protected Tour tour;
	protected Tour workTour;
	protected Person person;

	protected ModelStructure modelStructure;

	protected double origDuDen;
	protected double origEmpDen;
	protected double origTotInt;
	protected double destDuDen;
	protected double destEmpDen;
	protected double destTotInt;

	protected double lsWgtAvgCostM;
	protected double lsWgtAvgCostD;
	protected double lsWgtAvgCostH;
	protected double reimburseProportion;
	protected int parkingArea;

	protected float pTazTerminalTime;
	protected float aTazTerminalTime;

	protected double nmWalkTimeOut;
	protected double nmWalkTimeIn;
	protected double nmBikeTimeOut;
	protected double nmBikeTimeIn;

	protected int originMgra;
    protected int destMgra;
	  
	protected double[][][][] transitSkim;
	

	public TourModeChoiceDMU(ModelStructure modelStructure, Logger aLogger) {
		logger = aLogger;
		if (logger == null)
			logger = Logger.getLogger(TourModeChoiceDMU.class);
		this.modelStructure = modelStructure;
		dmuIndex = new IndexValues();

		transitSkim = new double[McLogsumsCalculator.NUM_ACC_EGR][McLogsumsCalculator.NUM_LOC_PREM][McLogsumsCalculator.NUM_SKIMS][McLogsumsCalculator.NUM_DIR];

	}

	public void setHouseholdObject(Household hhObject) {
		hh = hhObject;
	}


	public Household getHouseholdObject() {
		return hh;
	}

	public void setPersonObject(Person personObject) {
		person = personObject;
	}

	public Person getPersonObject() {
		return person;
	}

	public void setWorkTourObject(Tour tourObject) {
		workTour = tourObject;
	}

	public void setTourObject(Tour tourObject) {
		tour = tourObject;
	}

	public Tour getTourObject() {
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
	 * @param hhIndex
	 *            is the DMU household index
	 * @param zoneIndex
	 *            is the DMU zone index
	 * @param origIndex
	 *            is the DMU origin index
	 * @param destIndex
	 *            is the DMU desatination index
	 */
	public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex,
			int destIndex, boolean debug) {
		dmuIndex.setHHIndex(hhIndex);
		dmuIndex.setZoneIndex(zoneIndex);
		dmuIndex.setOriginZone(origIndex);
		dmuIndex.setDestZone(destIndex);

		dmuIndex.setDebug(false);
		dmuIndex.setDebugLabel("");
		if (debug) {
			dmuIndex.setDebug(true);
			dmuIndex.setDebugLabel("Debug MC UEC");
		}

	}

	public int getPersonType() {
		return person.getPersonTypeNumber();
	}

	public void setOrigDuDen(double arg) {
		origDuDen = arg;
	}

	public void setOrigEmpDen(double arg) {
		origEmpDen = arg;
	}

	public void setOrigTotInt(double arg) {
		origTotInt = arg;
	}

	public void setDestDuDen(double arg) {
		destDuDen = arg;
	}

	public void setDestEmpDen(double arg) {
		destEmpDen = arg;
	}

	public void setDestTotInt(double arg) {
		destTotInt = arg;
	}

	public void setReimburseProportion(double proportion) {
		reimburseProportion = proportion;
	}

	public void setLsWgtAvgCostM(double cost) {
		lsWgtAvgCostM = cost;
	}

	public void setLsWgtAvgCostD(double cost) {
		lsWgtAvgCostD = cost;
	}

	public void setLsWgtAvgCostH(double cost) {
		lsWgtAvgCostH = cost;
	}

	public void setPTazTerminalTime(float time) {
		pTazTerminalTime = time;
	}

	public void setATazTerminalTime(float time) {
		aTazTerminalTime = time;
	}

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	public void setIndexDest(int d) {
		dmuIndex.setDestZone(d);
	}

	public void setTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir,
			double value) {
		transitSkim[accEgr][lbPrem][skimIndex][dir] = value;
	}

	public double getTransitSkim(int accEgr, int lbPrem, int skimIndex, int dir) {
		return transitSkim[accEgr][lbPrem][skimIndex][dir];
	}

	public int getWorkTourModeIsSov() {
		boolean tourModeIsSov = modelStructure.getTourModeIsSov(workTour
				.getTourModeChoice());
		return tourModeIsSov ? 1 : 0;
	}

	public int getWorkTourModeIsHov() {
		boolean tourModeIsHov = modelStructure.getTourModeIsHov(workTour
				.getTourModeChoice());
		return tourModeIsHov ? 1 : 0;
	}

	public int getWorkTourModeIsBike() {
		boolean tourModeIsBike = modelStructure.getTourModeIsBike(workTour
				.getTourModeChoice());
		return tourModeIsBike ? 1 : 0;
	}

	public int getTourCategoryJoint() {
		if (tour.getTourCategory().equalsIgnoreCase(
				ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
			return 1;
		else
			return 0;
	}

	public int getTourCategoryEscort() {
		if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
				ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME))
			return 1;
		else
			return 0;
	}

	public int getTourCategorySubtour() {
		if (tour.getTourCategory().equalsIgnoreCase(
				ModelStructure.AT_WORK_CATEGORY))
			return 1;
		else
			return 0;
	}

	public int getNumberOfParticipantsInJointTour() {
		int[] participants = tour.getPersonNumArray();
		int returnValue = 0;
		if (participants != null)
			returnValue = participants.length;
		return returnValue;
	}

	public int getHhSize() {
		return hh.getHhSize();
	}

	public int getAutos() {
		return hh.getAutoOwnershipModelResult();
	}

	public int getAge() {
		return person.getAge();
	}

	public int getIncome() {
		return hh.getIncome();
	}

	public void setNmWalkTimeOut(double nmWalkTime) {
		nmWalkTimeOut = nmWalkTime;
	}

	public double getNmWalkTimeOut() {
		return nmWalkTimeOut;
	}

	public void setNmWalkTimeIn(double nmWalkTime) {
		nmWalkTimeIn = nmWalkTime;
	}

	public double getNmWalkTimeIn() {
		return nmWalkTimeIn;
	}

	public void setNmBikeTimeOut(double nmBikeTime) {
		nmBikeTimeOut = nmBikeTime;
	}

	public double getNmBikeTimeOut() {
		return nmBikeTimeOut;
	}

	public void setNmBikeTimeIn(double nmBikeTime) {
		nmBikeTimeIn = nmBikeTime;
	}

	public double getNmBikeTimeIn() {
		return nmBikeTimeIn;
	}
	
	public double getWorkTimeFactor() {
		return person.getTimeFactorWork();
	}
	
	public double getNonWorkTimeFactor(){
		return person.getTimeFactorNonWork();
	}
	
	/**
	 * Iterate through persons on tour and return non-work time factor
	 * for oldest person. If the person array is null then return 1.0.
	 * 
	 * @return Time factor for oldest person on joint tour.
	 */
	public double getJointTourTimeFactor() {
		int[] personNumArray = tour.getPersonNumArray();
		int oldestAge = -999;
		Person oldestPerson = null;
		for (int num : personNumArray){
			Person p = 	hh.getPerson(num);
			if(p.getAge() > oldestAge){
				oldestPerson = p;
				oldestAge = p.getAge();
			}
	    }
		if(oldestPerson != null)
			return oldestPerson.getTimeFactorNonWork();
		
		return 1.0;
	}


	public int getFreeParkingEligibility() {
		return person.getFreeParkingAvailableResult();
	}

	public double getReimburseProportion() {
		return reimburseProportion;
	}

	public double getMonthlyParkingCost() {
		return lsWgtAvgCostM;
	}

	public double getDailyParkingCost() {
		return lsWgtAvgCostD;
	}

	public double getHourlyParkingCost() {
		return lsWgtAvgCostH;
	}

	public double getPTazTerminalTime() {
		return pTazTerminalTime;
	}

	public double getATazTerminalTime() {
		return aTazTerminalTime;
	}
	
	public void setOriginMgra( int value ) {
    originMgra = value; 
  }
    
  public void setDestMgra( int value ) {
    destMgra = value; 
  }
    
  public int getOriginMgra() {
    return originMgra;
  }
    
  public int getDestMgra() {
    return destMgra; 
  }
  
  /**
   * 1 if household owns transponder, else 0
   * @return 1 if household owns transponder, else 0
   */
  public int getTransponderOwnership(){
	  return hh.getTpChoice();
  }

	public int getIndexValue(String variableName) {
		return methodIndexMap.get(variableName);
	}

	public int getAssignmentIndexValue(String variableName) {
		throw new UnsupportedOperationException();
	}

	public double getValueForIndex(int variableIndex) {
		throw new UnsupportedOperationException();
	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {
		throw new UnsupportedOperationException();
	}

	public void setValue(String variableName, double variableValue) {
		throw new UnsupportedOperationException();
	}

	public void setValue(int variableIndex, double variableValue) {
		throw new UnsupportedOperationException();
	}
}
