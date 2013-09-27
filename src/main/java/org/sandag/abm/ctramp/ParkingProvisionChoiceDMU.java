package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * @author crf <br/>
 *         Started: Apr 14, 2009 11:09:58 AM
 */
public class ParkingProvisionChoiceDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger
			.getLogger(ParkingProvisionChoiceDMU.class);

	protected HashMap<String, Integer> methodIndexMap;

	private Household hh;
	private Person person;
	private IndexValues dmuIndex;

	private int mgraParkArea;
	private int numFreeHours;
	private int mstallsoth;
	private int mstallssam;
	private float mparkcost;
	private int dstallsoth;
	private int dstallssam;
	private float dparkcost;
	private int hstallsoth;
	private int hstallssam;
	private float hparkcost;

	private double lsWgtAvgCostM;
	private double lsWgtAvgCostD;
	private double lsWgtAvgCostH;

	public ParkingProvisionChoiceDMU() {
		dmuIndex = new IndexValues();
	}

	/** need to set hh and home taz before using **/
	public void setPersonObject(Person person) {
		hh = person.getHouseholdObject();
		this.person = person;
	}

	public void setDmuIndexValues(int hhId, int zoneId, int origTaz, int destTaz) {
		dmuIndex.setHHIndex(hhId);
		dmuIndex.setZoneIndex(zoneId);
		dmuIndex.setOriginZone(origTaz);
		dmuIndex.setDestZone(destTaz);

		dmuIndex.setDebug(false);
		dmuIndex.setDebugLabel("");
		if (hh.getDebugChoiceModels()) {
			dmuIndex.setDebug(true);
			dmuIndex.setDebugLabel("Debug Free Parking UEC");
		}
	}

	public void setMgraParkArea(int value) {
		mgraParkArea = value;
	}

	public void setNumFreeHours(int value) {
		numFreeHours = value;
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

	public void setMStallsOth(int value) {
		mstallsoth = value;
	}

	public void setMStallsSam(int value) {
		mstallssam = value;
	}

	public void setMParkCost(float value) {
		mparkcost = value;
	}

	public void setDStallsOth(int value) {
		dstallsoth = value;
	}

	public void setDStallsSam(int value) {
		dstallssam = value;
	}

	public void setDParkCost(float value) {
		dparkcost = value;
	}

	public void setHStallsOth(int value) {
		hstallsoth = value;
	}

	public void setHStallsSam(int value) {
		hstallssam = value;
	}

	public void setHParkCost(float value) {
		hparkcost = value;
	}

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	/* dmu @ functions */

	public int getIncomeInDollars() {
		return hh.getIncomeInDollars();
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

	public int getMgraParkArea() {
		return mgraParkArea;
	}

	public int getNumFreeHours() {
		return numFreeHours;
	}

	public int getMStallsOth() {
		return mstallsoth;
	}

	public int getMStallsSam() {
		return mstallssam;
	}

	public float getMParkCost() {
		return mparkcost;
	}

	public int getDStallsOth() {
		return dstallsoth;
	}

	public int getDStallsSam() {
		return dstallssam;
	}

	public float getDParkCost() {
		return dparkcost;
	}

	public int getHStallsOth() {
		return hstallsoth;
	}

	public int getHStallsSam() {
		return hstallssam;
	}

	public float getHParkCost() {
		return hparkcost;
	}

	public int getWorkLocMgra() {
		return person.getPersonWorkLocationZone();
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
