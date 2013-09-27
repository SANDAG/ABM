package org.sandag.abm.application;

import java.util.HashMap;
import com.pb.common.calculator.VariableTable;
import org.sandag.abm.ctramp.AtWorkSubtourFrequencyDMU;
import org.sandag.abm.ctramp.ModelStructure;

public class SandagAtWorkSubtourFrequencyDMU extends AtWorkSubtourFrequencyDMU
		implements VariableTable {

	public SandagAtWorkSubtourFrequencyDMU(ModelStructure modelStructure) {
		super(modelStructure);
		this.modelStructure = modelStructure;
		setupMethodIndexMap();
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getIncomeInDollars", 0);
		methodIndexMap.put("getPersonType", 1);
		methodIndexMap.put("getFemale", 2);
		methodIndexMap.put("getDrivers", 3);
		methodIndexMap.put("getNumPreschoolChildren", 4);
		methodIndexMap.put("getNumIndivEatOutTours", 5);
		methodIndexMap.put("getNumTotalTours", 6);
		methodIndexMap.put("getNmEatOutAccessibilityWorkplace", 7);
	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		switch (variableIndex) {
		case 0:
			return getIncomeInDollars();
		case 1:
			return getPersonType();
		case 2:
			return getFemale();
		case 3:
			return getDrivers();
		case 4:
			return getNumPreschoolChildren();
		case 5:
			return getNumIndivEatOutTours();
		case 6:
			return getNumTotalTours();
		case 7:
			return getNmEatOutAccessibilityWorkplace();

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}

	}

}
