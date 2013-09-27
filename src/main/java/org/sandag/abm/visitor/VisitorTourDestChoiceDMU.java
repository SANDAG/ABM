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

public class VisitorTourDestChoiceDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger.getLogger("visitorModel");

	protected HashMap<String, Integer> methodIndexMap;
	protected IndexValues dmuIndex;

	protected float tourDepartPeriod;
	protected float tourArrivePeriod;
	protected int purpose;
	protected double[][] sizeTerms; // by purpose, alternative (taz or sampled
									// mgras)
	protected double[] correctionFactors; // by alternative (sampled mgra, for
											// full model only)
	protected double[] tourModeLogsums; // by alternative (sampled mgra pair,
										// for full model only)
	protected int[] sampleMGRA; // by alternative (sampled mgra)
	protected int[] sampleTAZ; // by alternative (sampled taz)

	protected double nmWalkTimeOut;
	protected double nmWalkTimeIn;
	protected double nmBikeTimeOut;
	protected double nmBikeTimeIn;
	protected double lsWgtAvgCostM;
	protected double lsWgtAvgCostD;
	protected double lsWgtAvgCostH;

	public VisitorTourDestChoiceDMU(VisitorModelStructure modelStructure) {
		setupMethodIndexMap();
		dmuIndex = new IndexValues();

	}

	/**
	 * Get the tour mode choice logsum for the sampled station-mgra pair.
	 * 
	 * @param alt
	 *            Sampled station-mgra
	 * @return
	 */
	public double getTourModeLogsum(int alt) {
		return tourModeLogsums[alt];
	}

	/**
	 * Set the tour mode choice logsums
	 * 
	 * @param poeNumbers
	 *            An array of tour mode choice logsums, one for each alternative
	 *            (sampled station-mgra)
	 */
	public void setTourModeLogsums(double[] logsums) {
		this.tourModeLogsums = logsums;
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

	public int getSampleMgra(int alt) {
		return sampleMGRA[alt];
	}

	public void setSampleMgra(int[] sampleNumber) {
		this.sampleMGRA = sampleNumber;
	}

	public int getSampleTaz(int alt) {
		return sampleTAZ[alt];
	}

	public void setSampleTaz(int[] sampleNumber) {
		this.sampleTAZ = sampleNumber;
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

	public float getTimeOutbound() {
		return tourDepartPeriod;
	}

	public float getTimeInbound() {
		return tourArrivePeriod;
	}

	/**
	 * @param tourDepartPeriod
	 *            the tourDepartPeriod to set
	 */
	public void setTourDepartPeriod(float tourDepartPeriod) {
		this.tourDepartPeriod = tourDepartPeriod;
	}

	/**
	 * @param tourArrivePeriod
	 *            the tourArrivePeriod to set
	 */
	public void setTourArrivePeriod(float tourArrivePeriod) {
		this.tourArrivePeriod = tourArrivePeriod;
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getTimeOutbound", 0);
		methodIndexMap.put("getTimeInbound", 1);
		methodIndexMap.put("getSizeTerm", 2);
		methodIndexMap.put("getCorrectionFactor", 3);
		methodIndexMap.put("getPurpose", 4);
		methodIndexMap.put("getTourModeLogsum", 5);
		methodIndexMap.put("getSampleMgra", 6);
		methodIndexMap.put("getSampleTaz", 7);

	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		double returnValue = -1;

		switch (variableIndex) {

		case 0:
			returnValue = getTimeOutbound();
			break;
		case 1:
			returnValue = getTimeInbound();
			break;
		case 2:
			returnValue = getSizeTerm(arrayIndex);
			break;
		case 3:
			returnValue = getCorrectionFactor(arrayIndex);
			break;
		case 4:
			returnValue = getPurpose();
			break;
		case 5:
			returnValue = getTourModeLogsum(arrayIndex);
			break;
		case 6:
			returnValue = getSampleMgra(arrayIndex);
			break;
		case 7:
			returnValue = getSampleTaz(arrayIndex);
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