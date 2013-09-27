/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.modechoice;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.Tracer;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.Modes.TransitMode;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * TransitPathUEC is a class that ...
 * 
 * @author Christi Willison
 * @version 1.0, Feb 3, 2009
 * 
 * @version 1.1, May 4, 2009 JEF
 * 
 */
public class TransitPathUEC implements Serializable {

	protected transient Logger logger = Logger.getLogger(TransitPathUEC.class);

	double[] bestUtilities;
	int[] bestPTap;
	int[] bestATap;

	File UECFile;

	UtilityExpressionCalculator uec;
	IndexValues index = new IndexValues();

	UtilityExpressionCalculator driveAccessUEC;
	TransitDriveAccessDMU driveAccessDMU;

	// seek and trace
	boolean trace;
	int[] traceOtaz;
	int[] traceDtaz;
	protected Tracer tracer;

	/**
	 * Constructor.
	 * 
	 * @param rb
	 *            ResourceBundle for the UEC.
	 */
	public TransitPathUEC(HashMap<String, String> rbMap) {

		// read in resource bundle properties
		trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
		traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
		traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

		// set up the tracer object
		tracer = Tracer.getTracer();
		tracer.setTrace(trace);
		for (int i = 0; i < traceOtaz.length; i++) {
			for (int j = 0; j < traceDtaz.length; j++) {
				tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
			}
		}
		bestUtilities = new double[Modes.TransitMode.values().length]; // bestUtility
		// by Transit
		// Ride mode
		bestPTap = new int[Modes.TransitMode.values().length];
		bestATap = new int[Modes.TransitMode.values().length];
	}

	/**
	 * Create the UEC for the main transit portion of the utility.
	 * 
	 * @param uecSpreadsheet
	 *            The .xls workbook with the model specification.
	 * @param modelSheet
	 *            The sheet with model specifications.
	 * @param dataSheet
	 *            The sheet with the data specifications.
	 * @param rb
	 *            A resource bundle with the path to the skims "skims.path"
	 * @param dmu
	 *            The DMU class for this UEC.
	 */
	public void createUEC(File uecSpreadsheet, int modelSheet, int dataSheet,
			HashMap<String, String> rbMap, VariableTable dmu) {
		uec = new UtilityExpressionCalculator(uecSpreadsheet, modelSheet,
				dataSheet, rbMap, dmu);
	}

	/**
	 * Clears the arrays. This method gets called for two different purposes.
	 * One is to compare alternatives based on utilities and the other based on
	 * exponentiated utilities. For this reason, the bestUtilities will be
	 * initialized by the value passed in as an argument set by the calling
	 * method.
	 * 
	 * @param initialization
	 *            value
	 */
	public void clearArrays(double initialValue) {
		Arrays.fill(bestUtilities, initialValue);
		Arrays.fill(bestPTap, 0);
		Arrays.fill(bestATap, 0);

	}

	/**
	 * Get the UEC file for this UEC.
	 * 
	 * @return The UEC file.
	 */
	public File getUECFile() {
		return UECFile;
	}

	/**
	 * Set the UEC file for this UEC.
	 * 
	 * @param UECFile
	 *            The UEC file for this UEC.
	 */
	public void setUECFile(File UECFile) {
		this.UECFile = UECFile;
	}

	/**
	 * Compare the paths calculated for this TAP-pair to the paths for
	 * previously- calculated TAP-pairs for each ride mode. If the current path
	 * is the best path, for that ride mode, set the bestUtilities[], bestPTap[]
	 * and bestATap[] for that ride mode.
	 * 
	 * @param calculatedUtilities
	 *            An array of utilities by ride mode.
	 * @param pTap
	 *            The origin TAP for this set of utilities.
	 * @param aTap
	 *            The destination TAP for this set of utilities.
	 */
	public boolean comparePaths(double[] calculatedUtilities, int pTap,
			int aTap, boolean trace) {
		boolean newBestPath = false;
		for (int i = 0; i < bestUtilities.length; i++) {

			if (trace) {
				logger.info("Mode " + i + " calculatedUtility "
						+ calculatedUtilities[i] + ", best utility "
						+ bestUtilities[i]);
			}

			if (calculatedUtilities[i] > bestUtilities[i]) {
				newBestPath = true;
				bestUtilities[i] = calculatedUtilities[i];
				bestPTap[i] = pTap;
				bestATap[i] = aTap;

				if (trace) {
					TransitMode[] tm = TransitMode.values();
					logger.info("Best utility so far for mode " + tm[i]
							+ " pTap " + pTap + " to aTap " + aTap + " is "
							+ bestUtilities[i]);
				}
			}
		}

		return newBestPath;
	}

	/**
	 * Create a UEC object and DMU object for the auto-access portion of utility
	 * (for drive-access modes).
	 * 
	 * @param uecSpreadsheet
	 *            The .xls workbook with the model specification.
	 * @param modelSheet
	 *            The sheet with model specifications.
	 * @param dataSheet
	 *            The sheet with the data specifications.
	 * @param rb
	 *            A resource bundle with the path to the skims "skims.path"
	 * @param dmu
	 *            The DMU class for this UEC.
	 */
	public void createDriveAccessUEC(File uecSpreadsheet, int modelSheet,
			int dataSheet, HashMap<String, String> rbMap, VariableTable dmu) {
		driveAccessUEC = new UtilityExpressionCalculator(uecSpreadsheet,
				modelSheet, dataSheet, rbMap, dmu);
		driveAccessDMU = new TransitDriveAccessDMU();
	}

	/**
	 * Calculate the auto-access portion of the utility (for drive-access
	 * modes).
	 * 
	 * @param tdzManager
	 *            TDZ data manager
	 * @param itdz
	 *            The origin TDZ.
	 * @param pos
	 *            The position of the TAP for this origin TDZ.
	 * @param accessMode
	 *            The access mode (PARK_N_RIDE or KISS_N_RIDE).
	 * 
	 * @return The auto-access utility.
	 */
	public double[] calculateDriveUtilityForTransit(TazDataManager tazManager,
			int itdz, int pos, Modes.AccessMode accessMode) {

		driveAccessDMU.setDriveTimeToTap(tazManager.getTapTime(itdz, pos,
				accessMode));
		driveAccessDMU.setDriveDistToTap(tazManager.getTapDist(itdz, pos,
				accessMode));
		int[] availFlag = { 1, 1 };

		double[] utilities = driveAccessUEC.solve(new IndexValues(),
				driveAccessDMU, availFlag);

		return utilities;
	}

	/**
	 * Log the best utilities so far to the logger.
	 * 
	 * @param localLogger
	 *            The logger to use for output.
	 */
	public void logBestUtilities(Logger localLogger) {
		// create the header
		String header = String.format("%16s", "Alternative");
		header += String.format("%14s", "Utility");
		header += String.format("%14s", "PTap");
		header += String.format("%14s", "ATap");

		localLogger.info("Best Utility and Tap to Tap Pair");
		localLogger.info(header);

		// log the utilities and tap number for each alternative
		for (String altName : uec.getAlternativeNames()) {
			header = header + String.format("  %16s", altName);
		}
		for (String altName : uec.getAlternativeNames()) {
			String line = String.format("%16s", altName);
			line = line
					+ String.format("  %12.4f",
							bestUtilities[Modes.getTransitModeIndex(altName)]);
			line = line
					+ String.format("  %12d",
							bestPTap[Modes.getTransitModeIndex(altName)]);
			line = line
					+ String.format("  %12d",
							bestATap[Modes.getTransitModeIndex(altName)]);

			localLogger.info(line);
		}
	}

	public void setTrace(boolean trace) {
		tracer.setTrace(trace);
	}

	/**
	 * Trace calculations for a zone pair.
	 * 
	 * @param itaz
	 * @param jtaz
	 * @return true if zone pair should be traced, otherwise false
	 */
	public boolean isTraceZonePair(int itaz, int jtaz) {
		if (tracer.isTraceOn()) {
			return tracer.isTraceZonePair(itaz, jtaz);
		} else {
			return false;
		}
	}

	/**
	 * Get the best utilities.
	 * 
	 * @return An array of the best utilities, dimensioned by ride-mode in @link
	 *         <Modes>.
	 */
	public double[] getBestUtilities() {
		return bestUtilities;
	}

	/**
	 * Get the best ptap and atap in an array. Only to be called after
	 * comparePaths() has been called.
	 * 
	 * @param transitMode
	 *            Mode to look up.
	 * @return element 0 = best ptap, element 1 = best atap
	 */
	public int[] getBestTaps(Modes.TransitMode transitMode) {

		int[] bestTaps = new int[2];

		bestTaps[0] = bestPTap[transitMode.ordinal()];
		bestTaps[1] = bestATap[transitMode.ordinal()];

		return bestTaps;
	}

}
