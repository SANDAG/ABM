package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.TransponderChoiceDMU;

public class SandagTransponderChoiceDMU extends TransponderChoiceDMU {

	public SandagTransponderChoiceDMU() {
		super();
		setupMethodIndexMap();
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getAutoOwnership", 0);
		methodIndexMap.put("getPctHighIncome", 1);
		methodIndexMap.put("getPctMultipleAutos", 2);
		methodIndexMap.put("getAvgtts", 3);
		methodIndexMap.put("getDistanceFromFacility", 4);
		methodIndexMap.put("getPctAltTimeCBD", 5);
		methodIndexMap.put("getAvgTransitAccess", 6);
	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		switch (variableIndex) {
		case 0:
			return getAutoOwnership();
		case 1:
			return getPctIncome100Kplus();
		case 2:
			return getPctTazMultpleAutos();
		case 3:
			return getExpectedTravelTimeSavings();
		case 4:
			return getTransponderDistance();
		case 5:
			return getPctDetour();
		case 6:
			return getAccessibility();

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}
	}

}