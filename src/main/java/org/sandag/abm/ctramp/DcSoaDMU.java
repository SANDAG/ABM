package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class DcSoaDMU implements SoaDMU, Serializable, VariableTable {

	protected transient Logger logger = Logger.getLogger(DcSoaDMU.class);

	protected HashMap<String, Integer> methodIndexMap;

	protected Household hh;
	protected Person person;
	protected Tour tour;

	protected IndexValues dmuIndex = null;
	protected String dmuLabel = "Origin Location";

	protected double[] dcSize;
	protected double[] distance;

	protected BuildAccessibilities aggAcc;

	public DcSoaDMU() {
		dmuIndex = new IndexValues();
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
			dmuIndex.setDebugLabel("Debug DC SOA UEC");
		}

	}

	public void setAggAcc(BuildAccessibilities myAggAcc) {
		aggAcc = myAggAcc;
	}

	public void setHouseholdObject(Household hhObject) {
		hh = hhObject;
	}

	public void setPersonObject(Person personObject) {
		person = personObject;
	}

	public void setTourObject(Tour tourObject) {
		tour = tourObject;
	}

	public void setDestChoiceSize(double[] dcSize) {
		this.dcSize = dcSize;
	}

	public void setDestDistance(double[] distance) {
		this.distance = distance;
	}

	public double[] getDestDistance() {
		return distance;
	}

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	public Household getHouseholdObject() {
		return hh;
	}

	public int getTourPurposeIsEscort() {
		return tour.getTourPrimaryPurpose().equalsIgnoreCase(
				ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME) ? 1 : 0;
	}

	public int getNumPreschool() {
		return hh.getNumPreschool();
	}

	public int getNumGradeSchoolStudents() {
		return hh.getNumGradeSchoolStudents();
	}

	public int getNumHighSchoolStudents() {
		return hh.getNumHighSchoolStudents();
	}

	protected double getLnDcSize(int alt) {

		double size = dcSize[alt];

		double logSize = 0.0;
		logSize = Math.log(size + 1);

		return logSize;

	}

	protected double getDcSizeAlt(int alt) {
		return dcSize[alt];
	}

	protected double getHouseholdsDestAlt(int mgra) {
		return aggAcc.getMgraHouseholds(mgra);
	}

	protected double getGradeSchoolEnrollmentDestAlt(int mgra) {
		return aggAcc.getMgraGradeSchoolEnrollment(mgra);
	}

	protected double getHighSchoolEnrollmentDestAlt(int mgra) {
		return aggAcc.getMgraHighSchoolEnrollment(mgra);
	}

	public int getGradeSchoolDistrictDestAlt(int mgra) {
		return aggAcc.getMgraGradeSchoolDistrict(mgra);
	}

	public int getHomeMgraGradeSchoolDistrict() {
		return aggAcc.getMgraGradeSchoolDistrict(hh.getHhMgra());
	}

	public double getHighSchoolDistrictDestAlt(int mgra) {
		return aggAcc.getMgraHighSchoolDistrict(mgra);
	}

	public double getHomeMgraHighSchoolDistrict() {
		return aggAcc.getMgraHighSchoolDistrict(hh.getHhMgra());
	}

	public double getOriginToMgraDistanceAlt(int alt) {
		return distance[alt];
	}

	public double getUniversityEnrollmentDestAlt(int mgra) {
		return aggAcc.getMgraUniversityEnrollment(mgra);
	}

	public String getDmuLabel() {
		return dmuLabel;
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
