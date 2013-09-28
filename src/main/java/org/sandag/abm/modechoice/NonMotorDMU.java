package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

/**
 * This class is used for non-motorized DMU attributes.
 * 
 * @author Joel Freedman
 * @version May 28,2009
 *          <p/>
 */
public class NonMotorDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger.getLogger(NonMotorDMU.class);

	protected HashMap<String, Integer> methodIndexMap;

	private float mgraWalkTime;
	private float mgraBikeTime;

	public NonMotorDMU() {
		setupMethodIndexMap();
	}

	/**
	 * Get MGRA-MGRA walk time.
	 * 
	 * @return Mgra-mgra walk time in minutes.
	 */
	public float getMgraWalkTime() {
		return mgraWalkTime;
	}

	/**
	 * Set Mgra-Mgra walk time in minutes.
	 * 
	 * @param mgraWalkTime
	 *            Mgra walk time in minutes.
	 */
	public void setMgraWalkTime(float mgraWalkTime) {
		this.mgraWalkTime = mgraWalkTime;
	}

	/**
	 * Get MGRA-MGRA bike time.
	 * 
	 * @return Mgra-mgra bike time in minutes.
	 */
	public float getMgraBikeTime() {
		return mgraBikeTime;
	}

	/**
	 * Set Mgra-Mgra bike time in minutes.
	 * 
	 * @param mgraBikeTime
	 *            Mgra bike time in minutes.
	 */
	public void setMgraBikeTime(float mgraBikeTime) {
		this.mgraBikeTime = mgraBikeTime;
	}

	/**
	 * Log the DMU values.
	 * 
	 * @param localLogger
	 *            The logger to use.
	 */
	public void logValues(Logger localLogger) {

		localLogger.info("");
		localLogger.info("Non-Motorized DMU Values:");
		localLogger.info("");
		localLogger.info(String.format("MGRA-MGRA Walk Time: %9.4f",
				mgraWalkTime));
		localLogger.info(String.format("MGRA-MGRA Bike Time: %9.4f",
				mgraBikeTime));

	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getMgraBikeTime", 0);
		methodIndexMap.put("getMgraWalkTime", 1);

	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		double returnValue = 0;

		switch (variableIndex) {
		case 0:
			returnValue = getMgraBikeTime();
			break;
		case 1:
			returnValue = getMgraWalkTime();
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
