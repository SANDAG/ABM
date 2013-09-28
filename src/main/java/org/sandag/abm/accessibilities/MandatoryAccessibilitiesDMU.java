package org.sandag.abm.accessibilities;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

public class MandatoryAccessibilitiesDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger
			.getLogger(MandatoryAccessibilitiesDMU.class);

	protected HashMap<String, Integer> methodIndexMap;

	protected double sovNestLogsum;
	protected double hovNestLogsum;
	protected double wlkNestLogsum;
	protected double drvNestLogsum;
	protected int bestMode;
	protected float mgraTapWalkTime;
	protected float tapMgraWalkTime;
	protected float driveDistToTap;
	protected float driveTimeToTap;
	protected int autoSufficiency;

	public MandatoryAccessibilitiesDMU() {
		setupMethodIndexMap();
	}

	public int getAutoSufficiency() {
		return autoSufficiency;
	}

	public void setAutoSufficiency(int autoSufficiency) {
		this.autoSufficiency = autoSufficiency;
	}

	public double getSovNestLogsum() {
		return sovNestLogsum;
	}

	public void setSovNestLogsum(double sovNestLogsum) {
		this.sovNestLogsum = sovNestLogsum;
	}

	public double getHovNestLogsum() {
		return hovNestLogsum;
	}

	public void setHovNestLogsum(double hovNestLogsum) {
		this.hovNestLogsum = hovNestLogsum;
	}

	public double getWlkNestLogsum() {
		return wlkNestLogsum;
	}

	public void setWlkNestLogsum(double wlkNestLogsum) {
		this.wlkNestLogsum = wlkNestLogsum;
	}

	public double getDrvNestLogsum() {
		return drvNestLogsum;
	}

	public void setDrvNestLogsum(double drvNestLogsum) {
		this.drvNestLogsum = drvNestLogsum;
	}

	public int getBestMode() {
		return bestMode;
	}

	public void setBestMode(int bestMode) {
		this.bestMode = bestMode;
	}

	public float getMgraTapWalkTime() {
		return mgraTapWalkTime;
	}

	public void setMgraTapWalkTime(float mgraTapWalkTime) {
		this.mgraTapWalkTime = mgraTapWalkTime;
	}

	public float getTapMgraWalkTime() {
		return tapMgraWalkTime;
	}

	public void setTapMgraWalkTime(float tapMgraWalkTime) {
		this.tapMgraWalkTime = tapMgraWalkTime;
	}

	public float getDriveDistToTap() {
		return driveDistToTap;
	}

	public void setDriveDistToTap(float drvDistToTap) {
		this.driveDistToTap = drvDistToTap;
	}

	public float getDriveTimeToTap() {
		return driveTimeToTap;
	}

	public void setDriveTimeToTap(float drvTimeToTap) {
		this.driveTimeToTap = drvTimeToTap;
	}

	public int getIndexValue(String variableName) {
		return methodIndexMap.get(variableName);
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getBestMode", 0);
		methodIndexMap.put("getDriveDistToTap", 1);
		methodIndexMap.put("getDriveTimeToTap", 2);
		methodIndexMap.put("getWlkNestLogsum", 3);
		methodIndexMap.put("getDrvNestLogsum", 4);
		methodIndexMap.put("getSovNestLogsum", 5);
		methodIndexMap.put("getHovNestLogsum", 6);
		methodIndexMap.put("getMgraTapWalkTime", 7);
		methodIndexMap.put("getTapMgraWalkTime", 8);
		methodIndexMap.put("getAutoSufficiency", 9);
	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		switch (variableIndex) {
		case 0:
			return getBestMode();
		case 1:
			return getDriveDistToTap();
		case 2:
			return getDriveTimeToTap();
		case 3:
			return getWlkNestLogsum();
		case 4:
			return getDrvNestLogsum();
		case 5:
			return getSovNestLogsum();
		case 6:
			return getHovNestLogsum();
		case 7:
			return getMgraTapWalkTime();
		case 8:
			return getTapMgraWalkTime();
		case 9:
			return getAutoSufficiency();

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}
	}

	public int getAssignmentIndexValue(String variableName) {
		throw new UnsupportedOperationException();
	}

	public double getValueForIndex(int variableIndex) {
		throw new UnsupportedOperationException();
	}

	public void setValue(String variableName, double variableValue) {
		throw new UnsupportedOperationException();
	}

	public void setValue(int variableIndex, double variableValue) {
		throw new UnsupportedOperationException();
	}

}
