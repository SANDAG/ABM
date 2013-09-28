package org.sandag.abm.crossborder;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class CrossBorderTourModeChoiceDMU implements Serializable,
		VariableTable {

	protected transient Logger logger = Logger
			.getLogger(TourModeChoiceDMU.class);

	protected HashMap<String, Integer> methodIndexMap;
	protected IndexValues dmuIndex;

	protected double tourPurpose;
	protected double tourModelsSentri;
	protected double borderWaitStd;
	protected double borderWaitPed;
	protected double borderWaitSentri;

	protected double outboundTripMcLogsumDA;
	protected double outboundTripMcLogsumSR2;
	protected double outboundTripMcLogsumSR3;
	protected double outboundTripMcLogsumWalk;
	protected double inboundTripMcLogsumDA;
	protected double inboundTripMcLogsumSR2;
	protected double inboundTripMcLogsumSR3;
	protected double inboundTripMcLogsumWalk;

	/**
	 * Constructor.
	 * 
	 * @param modelStructure
	 */
	public CrossBorderTourModeChoiceDMU(CrossBorderModelStructure modelStructure) {
		setupMethodIndexMap();
		dmuIndex = new IndexValues();

	}

	/**
	 * @return the tourPurpose
	 */
	public double getTourPurpose() {
		return tourPurpose;
	}

	/**
	 * @param tourPurpose
	 *            the tourPurpose to set
	 */
	public void setTourPurpose(double tourPurpose) {
		this.tourPurpose = tourPurpose;
	}

	/**
	 * @return the tourModelsSentri
	 */
	public double getTourModelsSentri() {
		return tourModelsSentri;
	}

	/**
	 * @param tourModelsSentri
	 *            the tourModelsSentri to set
	 */
	public void setTourModelsSentri(double tourModelsSentri) {
		this.tourModelsSentri = tourModelsSentri;
	}

	/**
	 * @return the borderWaitStd
	 */
	public double getBorderWaitStd() {
		return borderWaitStd;
	}

	/**
	 * @param borderWaitStd
	 *            the borderWaitStd to set
	 */
	public void setBorderWaitStd(double borderWaitStd) {
		this.borderWaitStd = borderWaitStd;
	}

	/**
	 * @return the borderWaitPed
	 */
	public double getBorderWaitPed() {
		return borderWaitPed;
	}

	/**
	 * @param borderWaitPed
	 *            the borderWaitPed to set
	 */
	public void setBorderWaitPed(double borderWaitPed) {
		this.borderWaitPed = borderWaitPed;
	}

	/**
	 * @return the borderWaitSentri
	 */
	public double getBorderWaitSentri() {
		return borderWaitSentri;
	}

	/**
	 * @param borderWaitSentri
	 *            the borderWaitSentri to set
	 */
	public void setBorderWaitSentri(double borderWaitSentri) {
		this.borderWaitSentri = borderWaitSentri;
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

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	/**
	 * @return the outboundTripMcLogsumDA
	 */
	public double getOutboundTripMcLogsumDA() {
		return outboundTripMcLogsumDA;
	}

	/**
	 * @param outboundTripMcLogsumDA
	 *            the outboundTripMcLogsumDA to set
	 */
	public void setOutboundTripMcLogsumDA(double outboundTripMcLogsumDA) {
		this.outboundTripMcLogsumDA = outboundTripMcLogsumDA;
	}

	/**
	 * @return the outboundTripMcLogsumSR2
	 */
	public double getOutboundTripMcLogsumSR2() {
		return outboundTripMcLogsumSR2;
	}

	/**
	 * @param outboundTripMcLogsumSR2
	 *            the outboundTripMcLogsumSR2 to set
	 */
	public void setOutboundTripMcLogsumSR2(double outboundTripMcLogsumSR2) {
		this.outboundTripMcLogsumSR2 = outboundTripMcLogsumSR2;
	}

	/**
	 * @return the outboundTripMcLogsumSR3
	 */
	public double getOutboundTripMcLogsumSR3() {
		return outboundTripMcLogsumSR3;
	}

	/**
	 * @param outboundTripMcLogsumSR3
	 *            the outboundTripMcLogsumSR3 to set
	 */
	public void setOutboundTripMcLogsumSR3(double outboundTripMcLogsumSR3) {
		this.outboundTripMcLogsumSR3 = outboundTripMcLogsumSR3;
	}

	/**
	 * @return the outboundTripMcLogsumWalk
	 */
	public double getOutboundTripMcLogsumWalk() {
		return outboundTripMcLogsumWalk;
	}

	/**
	 * @param outboundTripMcLogsumWalk
	 *            the outboundTripMcLogsumWalk to set
	 */
	public void setOutboundTripMcLogsumWalk(double outboundTripMcLogsumWalk) {
		this.outboundTripMcLogsumWalk = outboundTripMcLogsumWalk;
	}

	/**
	 * @return the inboundTripMcLogsumDA
	 */
	public double getInboundTripMcLogsumDA() {
		return inboundTripMcLogsumDA;
	}

	/**
	 * @param inboundTripMcLogsumDA
	 *            the inboundTripMcLogsumDA to set
	 */
	public void setInboundTripMcLogsumDA(double inboundTripMcLogsumDA) {
		this.inboundTripMcLogsumDA = inboundTripMcLogsumDA;
	}

	/**
	 * @return the inboundTripMcLogsumSR2
	 */
	public double getInboundTripMcLogsumSR2() {
		return inboundTripMcLogsumSR2;
	}

	/**
	 * @param inboundTripMcLogsumSR2
	 *            the inboundTripMcLogsumSR2 to set
	 */
	public void setInboundTripMcLogsumSR2(double inboundTripMcLogsumSR2) {
		this.inboundTripMcLogsumSR2 = inboundTripMcLogsumSR2;
	}

	/**
	 * @return the inboundTripMcLogsumSR3
	 */
	public double getInboundTripMcLogsumSR3() {
		return inboundTripMcLogsumSR3;
	}

	/**
	 * @param inboundTripMcLogsumSR3
	 *            the inboundTripMcLogsumSR3 to set
	 */
	public void setInboundTripMcLogsumSR3(double inboundTripMcLogsumSR3) {
		this.inboundTripMcLogsumSR3 = inboundTripMcLogsumSR3;
	}

	/**
	 * @return the inboundTripMcLogsumWalk
	 */
	public double getInboundTripMcLogsumWalk() {
		return inboundTripMcLogsumWalk;
	}

	/**
	 * @param inboundTripMcLogsumWalk
	 *            the inboundTripMcLogsumWalk to set
	 */
	public void setInboundTripMcLogsumWalk(double inboundTripMcLogsumWalk) {
		this.inboundTripMcLogsumWalk = inboundTripMcLogsumWalk;
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getTourPurpose", 0);
		methodIndexMap.put("getTourModelsSentri", 1);
		methodIndexMap.put("getBorderWaitStd", 2);
		methodIndexMap.put("getBorderWaitPed", 3);
		methodIndexMap.put("getBorderWaitSentri", 4);

		methodIndexMap.put("getOutboundTripMcLogsumDA", 30);
		methodIndexMap.put("getOutboundTripMcLogsumSR2", 31);
		methodIndexMap.put("getOutboundTripMcLogsumSR3", 32);
		methodIndexMap.put("getOutboundTripMcLogsumWalk", 33);
		methodIndexMap.put("getInboundTripMcLogsumDA", 34);
		methodIndexMap.put("getInboundTripMcLogsumSR2", 35);
		methodIndexMap.put("getInboundTripMcLogsumSR3", 36);
		methodIndexMap.put("getInboundTripMcLogsumWalk", 37);

	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		double returnValue = -1;

		switch (variableIndex) {

		case 0:
			returnValue = getTourPurpose();
			break;
		case 1:
			returnValue = getTourModelsSentri();
			break;
		case 2:
			returnValue = getBorderWaitStd();
			break;
		case 3:
			returnValue = getBorderWaitPed();
			break;
		case 4:
			returnValue = getBorderWaitSentri();
			break;
		case 30:
			returnValue = getOutboundTripMcLogsumDA();
			break;
		case 31:
			returnValue = getOutboundTripMcLogsumSR2();
			break;
		case 32:
			returnValue = getOutboundTripMcLogsumSR3();
			break;
		case 33:
			returnValue = getOutboundTripMcLogsumWalk();
			break;
		case 34:
			returnValue = getInboundTripMcLogsumDA();
			break;
		case 35:
			returnValue = getInboundTripMcLogsumSR2();
			break;
		case 36:
			returnValue = getInboundTripMcLogsumSR3();
			break;
		case 37:
			returnValue = getInboundTripMcLogsumWalk();
			break;

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}

		return returnValue;

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

	public void setValue(String variableName, double variableValue) {
		throw new UnsupportedOperationException();
	}

	public void setValue(int variableIndex, double variableValue) {
		throw new UnsupportedOperationException();
	}

}