package org.sandag.abm.ctramp;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;

public class Stop implements Serializable {

	int id;
	int orig;
	int dest;
	int park;
	int mode;
	int stopPeriod;
	int boardTap;
	int alightTap;
	boolean inbound;

	String origPurpose;
	String destPurpose;
	int stopPurposeIndex;

	Tour parentTour;

	public Stop(Tour parentTour, String origPurpose, String destPurpose,
			int id, boolean inbound, int stopPurposeIndex) {
		this.parentTour = parentTour;
		this.origPurpose = origPurpose;
		this.destPurpose = destPurpose;
		this.stopPurposeIndex = stopPurposeIndex;
		this.id = id;
		this.inbound = inbound;
	}

	public void setOrig(int orig) {
		this.orig = orig;
	}

	public void setDest(int dest) {
		this.dest = dest;
	}

	public void setPark(int park) {
		this.park = park;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setBoardTap(int tap) {
		boardTap = tap;
	}

	public void setAlightTap(int tap) {
		alightTap = tap;
	}

	public void setStopPeriod(int period) {
		stopPeriod = period;
	}

	public int getOrig() {
		return orig;
	}

	public int getDest() {
		return dest;
	}

	public int getPark() {
		return park;
	}

	public String getOrigPurpose() {
		return origPurpose;
	}

	public String getDestPurpose() {
		return destPurpose;
	}

	public int getStopPurposeIndex() {
		return stopPurposeIndex;
	}

	public int getMode() {
		return mode;
	}

	public int getBoardTap() {
		return boardTap;
	}

	public int getAlightTap() {
		return alightTap;
	}

	public int getStopPeriod() {
		return stopPeriod;
	}

	public Tour getTour() {
		return parentTour;
	}

	public boolean isInboundStop() {
		return inbound;
	}

	public int getStopId() {
		return id;
	}

	public void logStopObject(Logger logger, int totalChars) {

		String separater = "";
		for (int i = 0; i < totalChars; i++)
			separater += "-";

		Household.logHelper(logger, "stopId: ", id, totalChars);
		Household.logHelper(logger, "origPurpose: ", origPurpose, totalChars);
		Household.logHelper(logger, "destPurpose: ", destPurpose, totalChars);
		Household.logHelper(logger, "orig: ", orig, totalChars);
		Household.logHelper(logger, "dest: ", dest, totalChars);
		Household.logHelper(logger, "mode: ", mode, totalChars);
		Household.logHelper(logger, "direction: ", inbound ? "inbound"
				: "outbound", totalChars);
		Household.logHelper(logger, inbound ? "outbound departPeriod: "
				: "inbound arrivePeriod: ", stopPeriod, totalChars);
		logger.info(separater);
		logger.info("");
		logger.info("");

	}

}
