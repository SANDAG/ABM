package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.ParkingChoiceDMU;

public class SandagParkingChoiceDMU extends ParkingChoiceDMU {

	public SandagParkingChoiceDMU() {
		super();
		setupMethodIndexMap();
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getParkMgraAlt", 1);
		methodIndexMap.put("getDistanceTripOrigToParkAlt", 2);
		methodIndexMap.put("getDistanceTripDestFromParkAlt", 3);
		methodIndexMap.put("getDestSameAsParkAlt", 4);
		methodIndexMap.put("getPersonType", 5);
		methodIndexMap.put("getActivityIntervals", 6);
		methodIndexMap.put("getTripDestPurpose", 7);
		methodIndexMap.put("getLsWgtAvgCostM", 8);
		methodIndexMap.put("getMstallsoth", 9);
		methodIndexMap.put("getMstallssam", 10);
		methodIndexMap.put("getMparkcost", 11);
		methodIndexMap.put("getDstallsoth", 12);
		methodIndexMap.put("getDstallssam", 13);
		methodIndexMap.put("getDparkcost", 14);
		methodIndexMap.put("getHstallsoth", 15);
		methodIndexMap.put("getHstallssam", 16);
		methodIndexMap.put("getHparkcost", 17);
		methodIndexMap.put("getNumfreehrs", 18);
		methodIndexMap.put("getReimbPct", 19);
	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		switch (variableIndex) {

		case 1:
			return getParkMgraAlt(arrayIndex);
		case 2:
			return getDistanceTripOrigToParkAlt(arrayIndex);
		case 3:
			return getDistanceTripDestFromParkAlt(arrayIndex);
		case 4:
			return getDestSameAsParkAlt(arrayIndex);
		case 5:
			return getPersonType();
		case 6:
			return getActivityIntervals();
		case 7:
			return getTripDestPurpose();
		case 8:
			return getLsWgtAvgCostM(arrayIndex);
		case 9:
			return getMstallsoth(arrayIndex);
		case 10:
			return getMstallssam(arrayIndex);
		case 11:
			return getMparkcost(arrayIndex);
		case 12:
			return getDstallsoth(arrayIndex);
		case 13:
			return getDstallssam(arrayIndex);
		case 14:
			return getDparkcost(arrayIndex);
		case 15:
			return getHstallsoth(arrayIndex);
		case 16:
			return getHstallssam(arrayIndex);
		case 17:
			return getHparkcost(arrayIndex);
		case 18:
			return getNumfreehrs(arrayIndex);
		case 19:
			return getReimbPct();

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}

	}

}