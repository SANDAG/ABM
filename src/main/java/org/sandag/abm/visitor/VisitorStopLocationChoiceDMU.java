package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;

public class VisitorStopLocationChoiceDMU implements Serializable,
		VariableTable {

	protected transient Logger logger = Logger.getLogger("visitorModel");

	protected HashMap<String, Integer> methodIndexMap;
	protected IndexValues dmuIndex;

	protected int purpose;
	protected int stopsOnHalfTour;
	protected int stopNumber;
	protected int inboundStop;
	protected int tourDuration;

	protected double[][] sizeTerms; // by purpose, alternative (taz or sampled
									// mgra)
	protected double[] correctionFactors; // by alternative (sampled mgra, for
											// full model only)

	protected int[] sampleNumber; // by alternative (taz or sampled mgra)

	protected double[] osMcLogsumAlt;
	protected double[] sdMcLogsumAlt;

	protected double[] tourOrigToStopDistanceAlt;
	protected double[] stopToTourDestDistanceAlt;

	public VisitorStopLocationChoiceDMU(VisitorModelStructure modelStructure) {
		setupMethodIndexMap();
		dmuIndex = new IndexValues();

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

	/**
	 * @return the stopsOnHalfTour
	 */
	public int getStopsOnHalfTour() {
		return stopsOnHalfTour;
	}

	/**
	 * @param stopsOnHalfTour
	 *            the stopsOnHalfTour to set
	 */
	public void setStopsOnHalfTour(int stopsOnHalfTour) {
		this.stopsOnHalfTour = stopsOnHalfTour;
	}

	/**
	 * @return the stopNumber
	 */
	public int getStopNumber() {
		return stopNumber;
	}

	/**
	 * @param stopNumber
	 *            the stopNumber to set
	 */
	public void setStopNumber(int stopNumber) {
		this.stopNumber = stopNumber;
	}

	/**
	 * @return the inboundStop
	 */
	public int getInboundStop() {
		return inboundStop;
	}

	/**
	 * @param inboundStop
	 *            the inboundStop to set
	 */
	public void setInboundStop(int inboundStop) {
		this.inboundStop = inboundStop;
	}

	/**
	 * @return the tourDuration
	 */
	public int getTourDuration() {
		return tourDuration;
	}

	/**
	 * @param tourDuration
	 *            the tourDuration to set
	 */
	public void setTourDuration(int tourDuration) {
		this.tourDuration = tourDuration;
	}

	/**
	 * @return the sampleNumber
	 */
	public int getSampleNumber(int alt) {
		return sampleNumber[alt];
	}

	/**
	 * @param sampleNumber
	 *            the sampleNumber to set
	 */
	public void setSampleNumber(int[] sampleNumber) {
		this.sampleNumber = sampleNumber;
	}

	/**
	 * @return the osMcLogsumAlt
	 */
	public double getOsMcLogsumAlt(int alt) {
		return osMcLogsumAlt[alt];
	}

	/**
	 * @param osMcLogsumAlt
	 *            the osMcLogsumAlt to set
	 */
	public void setOsMcLogsumAlt(double[] osMcLogsumAlt) {
		this.osMcLogsumAlt = osMcLogsumAlt;
	}

	/**
	 * @return the sdMcLogsumAlt
	 */
	public double getSdMcLogsumAlt(int alt) {
		return sdMcLogsumAlt[alt];
	}

	/**
	 * @param sdMcLogsumAlt
	 *            the sdMcLogsumAlt to set
	 */
	public void setSdMcLogsumAlt(double[] sdMcLogsumAlt) {
		this.sdMcLogsumAlt = sdMcLogsumAlt;
	}

	/**
	 * @return the tourOrigToStopDistanceAlt
	 */
	public double getTourOrigToStopDistanceAlt(int alt) {
		return tourOrigToStopDistanceAlt[alt];
	}

	/**
	 * @param tourOrigToStopDistanceAlt
	 *            the tourOrigToStopDistanceAlt to set
	 */
	public void setTourOrigToStopDistanceAlt(double[] tourOrigToStopDistanceAlt) {
		this.tourOrigToStopDistanceAlt = tourOrigToStopDistanceAlt;
	}

	/**
	 * @return the stopToTourDestDistanceAlt
	 */
	public double getStopToTourDestDistanceAlt(int alt) {
		return stopToTourDestDistanceAlt[alt];
	}

	/**
	 * @param stopToTourDestDistanceAlt
	 *            the stopToTourDestDistanceAlt to set
	 */
	public void setStopToTourDestDistanceAlt(double[] stopToTourDestDistanceAlt) {
		this.stopToTourDestDistanceAlt = stopToTourDestDistanceAlt;
	}

	/**
	 * @return the sizeTerms. The size term is the size of the alternative north
	 *         of the border. It is indexed by alternative, where alternative is
	 *         either taz-station pair or mgra-station pair, depending on
	 *         whether the DMU is being used for the SOA model or the actual
	 *         model.
	 */
	public double getSizeTerm(int alt) {
		return sizeTerms[purpose][alt];
	}

	/**
	 * @param sizeTerms
	 *            the sizeTerms to set. The size term is the size of the
	 *            alternative north of the border. It is indexed by alternative,
	 *            where alternative is either taz-station pair or mgra-station
	 *            pair, depending on whether the DMU is being used for the SOA
	 *            model or the actual model.
	 */
	public void setSizeTerms(double[][] sizeTerms) {
		this.sizeTerms = sizeTerms;
	}

	/**
	 * @return the correctionFactors
	 */
	public double getCorrectionFactor(int alt) {
		return correctionFactors[alt];
	}

	/**
	 * @param correctionFactors
	 *            the correctionFactors to set
	 */
	public void setCorrectionFactors(double[] correctionFactors) {
		this.correctionFactors = correctionFactors;
	}

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	/**
	 * @return the purpose
	 */
	public int getPurpose() {
		return purpose;
	}

	/**
	 * @param purpose
	 *            the purpose to set
	 */
	public void setPurpose(int purpose) {
		this.purpose = purpose;
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();
		methodIndexMap.put("getPurpose", 0);
		methodIndexMap.put("getStopsOnHalfTour", 1);
		methodIndexMap.put("getStopNumber", 2);
		methodIndexMap.put("getInboundStop", 3);
		methodIndexMap.put("getTourDuration", 4);

		methodIndexMap.put("getSizeTerm", 5);
		methodIndexMap.put("getCorrectionFactor", 6);
		methodIndexMap.put("getSampleNumber", 7);
		methodIndexMap.put("getOsMcLogsumAlt", 8);
		methodIndexMap.put("getSdMcLogsumAlt", 9);
		methodIndexMap.put("getTourOrigToStopDistanceAlt", 10);
		methodIndexMap.put("getStopToTourDestDistanceAlt", 11);

	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		double returnValue = -1;

		switch (variableIndex) {

		case 0:
			returnValue = getPurpose();
			break;
		case 1:
			returnValue = getStopsOnHalfTour();
			break;
		case 2:
			returnValue = getStopNumber();
			break;
		case 3:
			returnValue = getInboundStop();
			break;
		case 4:
			returnValue = getTourDuration();
			break;
		case 5:
			returnValue = getSizeTerm(arrayIndex);
			break;
		case 6:
			returnValue = getCorrectionFactor(arrayIndex);
			break;
		case 7:
			returnValue = getSampleNumber(arrayIndex);
			break;
		case 8:
			returnValue = getOsMcLogsumAlt(arrayIndex);
			break;
		case 9:
			returnValue = getSdMcLogsumAlt(arrayIndex);
			break;
		case 10:
			returnValue = getTourOrigToStopDistanceAlt(arrayIndex);
			break;
		case 11:
			returnValue = getStopToTourDestDistanceAlt(arrayIndex);
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