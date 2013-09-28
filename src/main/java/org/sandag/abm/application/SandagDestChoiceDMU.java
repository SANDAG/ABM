package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.DestChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

public class SandagDestChoiceDMU extends DestChoiceDMU {

	public SandagDestChoiceDMU(ModelStructure modelStructure) {
		super(modelStructure);
		setupMethodIndexMap();
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getMcLogsumDestAlt", 3);
		methodIndexMap.put("getNumGradeSchoolStudents", 4);
		methodIndexMap.put("getNumHighSchoolStudents", 5);
		methodIndexMap.put("getHouseholdsDestAlt", 8);
		methodIndexMap.put("getPopulationDestAlt", 9);
		methodIndexMap.put("getGradeSchoolEnrollmentDestAlt", 10);
		methodIndexMap.put("getHighSchoolEnrollmentDestAlt", 11);
		methodIndexMap.put("getUniversityEnrollmentDestAlt", 16);
		methodIndexMap.put("getPersonIsWorker", 20);
		methodIndexMap.put("getPersonHasBachelors", 21);
		methodIndexMap.put("getPersonType", 22);
		methodIndexMap.put("getSubtourType", 23);
		methodIndexMap.put("getDcSoaCorrectionsAlt", 24);
		methodIndexMap.put("getNumberOfNonWorkingAdults", 25);
		methodIndexMap.put("getNumPreschool", 26);
		methodIndexMap.put("getFemale", 27);
		methodIndexMap.put("getIncome", 28);
		methodIndexMap.put("getFemaleWorker", 29);
		methodIndexMap.put("getIncomeInDollars", 30);
		methodIndexMap.put("getAutos", 31);
		methodIndexMap.put("getWorkers", 32);
		methodIndexMap.put("getNumChildrenUnder16", 33);
		methodIndexMap.put("getNumChildrenUnder19", 34);
		methodIndexMap.put("getAge", 35);
		methodIndexMap.put("getFullTimeWorker", 36);
		methodIndexMap.put("getWorkTaz", 37);
		methodIndexMap.put("getWorkTourModeIsSOV", 38);
		methodIndexMap.put("getTourIsJoint", 39);
		methodIndexMap.put("getOpSovDistanceAlt", 42);
		methodIndexMap.put("getLnDcSizeAlt", 43);
		methodIndexMap.put("getWorkAccessibility", 44);
		methodIndexMap.put("getNonMandatoryAccessibilityAlt", 45);
		methodIndexMap.put("getToursLeft", 46);
		methodIndexMap.put("getMaxWindow", 47);
		methodIndexMap.put("getDcSizeAlt", 48);
	}

	public void setMcLogsum(int mgra, double logsum) {
		modeChoiceLogsums[mgra] = logsum;
	}

	public double getLogsumDestAlt(int alt) {
		return getMcLogsumDestAlt(alt);
	}

	public int getPersonIsFullTimeWorker() {
		return person.getPersonIsFullTimeWorker();
	}

	/*
	 * public int getSubtourType() { if (
	 * tour.getTourCategory().equalsIgnoreCase( ModelStructure.AT_WORK_CATEGORY
	 * ) ) return tour.getTourPurposeIndex(); else return 0; }
	 */

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		switch (variableIndex) {
		case 3:
			return getMcLogsumDestAlt(arrayIndex);
		case 4:
			return getNumGradeSchoolStudents();
		case 5:
			return getNumHighSchoolStudents();
		case 8:
			return getHouseholdsDestAlt(arrayIndex);
		case 9:
			return getPopulationDestAlt(arrayIndex);
		case 10:
			return getGradeSchoolEnrollmentDestAlt(arrayIndex);
		case 11:
			return getHighSchoolEnrollmentDestAlt(arrayIndex);
		case 16:
			return getUniversityEnrollmentDestAlt(arrayIndex);
		case 20:
			return getPersonIsWorker();
		case 21:
			return getPersonHasBachelors();
		case 22:
			return getPersonType();
		case 24:
			return getDcSoaCorrectionsAlt(arrayIndex);
		case 25:
			return getNumberOfNonWorkingAdults();
		case 26:
			return getNumPreschool();
		case 27:
			return getFemale();
		case 28:
			return getIncome();
		case 29:
			return getFemaleWorker();
		case 30:
			return getIncomeInDollars();
		case 31:
			return getAutos();
		case 32:
			return getWorkers();
		case 33:
			return getNumChildrenUnder16();
		case 34:
			return getNumChildrenUnder19();
		case 35:
			return getAge();
		case 36:
			return getFullTimeWorker();
		case 37:
			return getWorkTaz();
		case 38:
			return getWorkTourModeIsSOV();
		case 39:
			return getTourIsJoint();
		case 42:
			return getOpSovDistanceAlt(arrayIndex);
		case 43:
			return getLnDcSizeAlt(arrayIndex);
		case 44:
			return getWorkAccessibility();
		case 45:
			return getNonMandatoryAccessibilityAlt(arrayIndex);
		case 46:
			return getToursLeftCount();
		case 47:
			return getMaxContinuousAvailableWindow();
		case 48:
			return getDcSizeAlt(arrayIndex);

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}

	}

}