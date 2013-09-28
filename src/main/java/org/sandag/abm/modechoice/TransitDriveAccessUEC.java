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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.Modes.AccessMode;

import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * ParkRidePathUEC calculates the best PNR utilities for a given
 * production/origin TDZ and attraction/destination MGRA.
 * 
 * @author Christi Willison
 * @version 1.0, Feb 12, 2009
 */
public class TransitDriveAccessUEC extends TransitPathUEC {

	private TransitDriveAccessDMU dmu = new TransitDriveAccessDMU(); // DMU for
																		// this
	// UEC
	private AccessMode accMode; // gets passed
	// in to
	// constructor; either
	// PNR or KNR
	private TazDataManager tazManager;
	private TapDataManager tapManager;
	private MgraDataManager mgraManager;

	private float bestWalkAccessTime;
	private float bestDriveEgressTime;
	private float bestDriveAccessTime;
	private float bestWalkEgressTime;

	/**
	 * Constructor.
	 * 
	 * @param rb
	 *            ResourceBundle
	 * @param UECFileName
	 *            The path/name of the UEC containing the walk-transit model.
	 * @param modelSheet
	 *            The sheet (0-indexed) containing the model specification.
	 * @param dataSheet
	 *            The sheet (0-indexed) containing the data specification.
	 */
	public TransitDriveAccessUEC(HashMap<String, String> rbMap,
			String UECFileName, int modelSheet, int dataSheet, AccessMode am) {
		super(rbMap);
		UECFile = new File(UECFileName);
		createUEC(UECFile, modelSheet, dataSheet, rbMap, dmu);
		tazManager = TazDataManager.getInstance();
		tapManager = TapDataManager.getInstance();
		mgraManager = MgraDataManager.getInstance();
		accMode = am;

	}

	/**
	 * Create a drive-transit access UECFile. This method is only required if
	 * piece-wise utilities are built, where TAP-TAP pair utilities are computed
	 * separately from MGRA-TAP walk utilities, to cut down on processing time.
	 * 
	 * @param rb
	 *            ResourceBundle
	 * @param UECFileName
	 *            The path/name of the UEC containing the drive-transit model.
	 * @param modelSheet
	 *            The sheet (0-indexed) containing the model specification.
	 * @param dataSheet
	 *            The sheet (0-indexed) containing the data specification.
	 */
	public void createDriveAccessUEC(HashMap<String, String> rbMap,
			String UECFileName, int modelSheet, int dataSheet) {
		UECFile = new File(UECFileName);
		driveAccessUEC = new UtilityExpressionCalculator(UECFile, modelSheet,
				dataSheet, rbMap, dmu);

	}

	/**
	 * This method finds the best TAP-pairs for each ride mode. It cycles
	 * through drive access TAPs at the origin end (associated with the origin
	 * MGRA) and alighting TAPs at the destination end (associated with the
	 * destination MGRA) and calculates a utility for every available ride mode
	 * for each TAP pair. It compares the utility calculated for that TAP-pair
	 * to previously calculated utilities and stores the origin and destination
	 * TAP that had the best utility for each ride mode.
	 * 
	 * @param pTaz
	 *            The origin/production MGRA.
	 * @param aMgra
	 *            The destination/attraction MGRA.
	 * 
	 */
	public void findBestDriveTransitWalkTaps(int pMgra, int aMgra, boolean debug) {

		super.clearArrays(Double.NEGATIVE_INFINITY);
		bestDriveAccessTime = 0;
		bestWalkEgressTime = 0;

		int pTaz = mgraManager.getTaz(pMgra);
		int aTaz = mgraManager.getTaz(aMgra);

		if (tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode) == null
				|| mgraManager.getMgraWlkTapsDistArray()[aMgra][0] == null) {
			return;
		}

		boolean writeCalculations = false;
		if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz)) {
			writeCalculations = true;
		}

		int pPos = -1;
		for (int pTap : tazManager.getParkRideOrKissRideTapsForZone(pTaz,
				accMode)) {
			pPos++; // used to know where we are in time/dist arrays for taps

			float driveTime = tazManager.getTapTime(pTaz, pPos, accMode);
			dmu.setAccessMode(accMode.ordinal());
			dmu.setDriveDistToTap(tazManager.getTapDist(pTaz, pPos, accMode));
			dmu.setDriveTimeToTap(driveTime);
			dmu.setCarToStationWalkTime(0f);

			int lotID = (int) tapManager.getTapParkingInfo()[pTap][0][0]; // lot
																			// ID
			float lotCapacity = tapManager.getTapParkingInfo()[pTap][2][0]; // lot
			// capacity
			if ((accMode == AccessMode.PARK_N_RIDE && tapManager
					.getLotUse(lotID) < lotCapacity)
					|| (accMode == AccessMode.KISS_N_RIDE)) {

				int aPos = -1;
				for (int aTap : mgraManager.getMgraWlkTapsDistArray()[aMgra][0]) {
					aPos++;

					float aWalkTime = mgraManager.getMgraToTapWalkTime(aMgra,
							aPos);

					// Set DMU values
					dmu.setTapMgraWalkTime(aWalkTime);

					// set up the index and dmu objects
					index.setOriginZone(pTap);
					index.setDestZone(aTap);
					dmu.setEscalatorTime(tapManager.getEscalatorTime(pTap));

					// log DMU values
					if (trace) {
						if (Arrays.binarySearch(tapManager.getTaps(), pTap) > 0
								&& Arrays.binarySearch(tapManager.getTaps(),
										aTap) > 0)
							uec.logDataValues(logger, pTap, aTap, 0);
						dmu.logValues(logger);
					}

					// solve
					double[] results = uec.solve(index, dmu, null);

					// logging
					if (debug) {
						uec.logAnswersArray(logger,
								"Drive-Transit-Walk Utility");
						uec.logResultsArray(logger, pTap, aTap);
					}

					// compare the utilities for this TAP pair to previously
					// calculated utilities
					// for each ride mode and store the TAP numbers if this TAP
					// pair
					// is the best.
					boolean foundNewBestPath = super.comparePaths(results,
							pTap, aTap, writeCalculations);

					if (foundNewBestPath) {
						bestDriveAccessTime = driveTime;
						bestWalkEgressTime = aWalkTime;
					}
				}
			}
			if (writeCalculations || debug) {
				logBestUtilities(logger);
			}
		}
	}

	/**
	 * This method finds the best TAP-pairs for each ride mode. It cycles
	 * through drive access TAPs at the origin end (associated with the origin
	 * MGRA) and alighting TAPs at the destination end (associated with the
	 * destination MGRA) and calculates a utility for every available ride mode
	 * for each TAP pair. It compares the utility calculated for that TAP-pair
	 * to previously calculated utilities and stores the origin and destination
	 * TAP that had the best utility for each ride mode.
	 * 
	 * @param pTaz
	 *            The origin/production MGRA.
	 * @param aMgra
	 *            The destination/attraction MGRA.
	 * 
	 */
	public void findBestWalkTransitDriveTaps(int pMgra, int aMgra, boolean debug) {

		super.clearArrays(Double.NEGATIVE_INFINITY);
		bestWalkAccessTime = 0;
		bestDriveEgressTime = 0;

		int pTaz = mgraManager.getTaz(pMgra);
		int aTaz = mgraManager.getTaz(aMgra);

		if (mgraManager.getMgraWlkTapsDistArray()[pMgra][0] == null
				|| tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode) == null) {
			return;
		}

		boolean writeCalculations = false;
		if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz)) {
			writeCalculations = true;
		}

		int pPos = -1;
		for (int pTap : mgraManager.getMgraWlkTapsDistArray()[pMgra][0]) {
			pPos++; // used to know where we are in time/dist arrays for taps

			// Set DMU values
			float pWalkTime = mgraManager.getMgraToTapWalkTime(pMgra, pPos);
			dmu.setTapMgraWalkTime(pWalkTime);

			int aPos = -1;
			for (int aTap : tazManager.getParkRideOrKissRideTapsForZone(aTaz,
					accMode)) {
				aPos++;

				int lotID = (int) tapManager.getTapParkingInfo()[aTap][0][0]; // lot
				// ID
				float lotCapacity = tapManager.getTapParkingInfo()[aTap][2][0]; // lot
				// capacity
				if ((accMode == AccessMode.PARK_N_RIDE && tapManager
						.getLotUse(lotID) < lotCapacity)
						|| (accMode == AccessMode.KISS_N_RIDE)) {

					// Set DMU values
					float driveTime = tazManager
							.getTapTime(aTaz, aPos, accMode);
					dmu.setAccessMode(accMode.ordinal());
					dmu.setDriveDistToTap(tazManager.getTapDist(aTaz, aPos,
							accMode));
					dmu.setDriveTimeToTap(driveTime);
					dmu.setCarToStationWalkTime(0f);
					dmu.setEscalatorTime(tapManager.getEscalatorTime(aTap));

					// set up the index and dmu objects
					index.setOriginZone(pTap);
					index.setDestZone(aTap);

					// log DMU values
					if (trace) {
						if (Arrays.binarySearch(tapManager.getTaps(), pTap) > 0
								&& Arrays.binarySearch(tapManager.getTaps(),
										aTap) > 0)
							uec.logDataValues(logger, pTap, aTap, 0);
						dmu.logValues(logger);
					}

					// solve
					double[] results = uec.solve(index, dmu, null);

					// logging
					if (debug) {
						uec.logAnswersArray(logger,
								"Walk-Transit-Drive Utility");
						uec.logResultsArray(logger, pTap, aTap);
					}

					// compare the utilities for this TAP pair to previously
					// calculated utilities
					// for each ride mode and store the TAP numbers if this TAP
					// pair
					// is the best.
					boolean foundNewBestPath = super.comparePaths(results,
							pTap, aTap, writeCalculations);

					if (foundNewBestPath) {
						bestWalkAccessTime = pWalkTime;
						bestDriveEgressTime = driveTime;
					}
				}
			}
			if (writeCalculations || debug) {
				logBestUtilities(logger);
			}
		}
	}

	/**
	 * This method calculates the utilities for a given Tap pair. It is called
	 * from the method @link {@link #findBestDriveAccessTaps(int, int)}. The
	 * walk times in the dmu must be set separately, or set to 0 for a set of
	 * utilities that are mgra-independent.
	 * 
	 * @param pTap
	 *            The origin/production Tap
	 * @param aTap
	 *            The destination/attraction Tap.
	 * @param trace
	 *            True if debug calculations are to be written to the logger for
	 *            this Tap-pair.
	 * @return A set of utilities for the Tap-pair, dimensioned by ride mode in @link
	 *         <Modes>.
	 */
	public double[] calculateUtilitiesForTapPair(int pTap, int aTap,
			boolean trace) {

		// set up the index and dmu objects
		index.setOriginZone(pTap);
		index.setDestZone(aTap);
		dmu.setEscalatorTime(tapManager.getEscalatorTime(pTap));

		// log DMU values
		if (trace) {
			if (Arrays.binarySearch(tapManager.getTaps(), pTap) > 0
					&& Arrays.binarySearch(tapManager.getTaps(), aTap) > 0)
				uec.logDataValues(logger, pTap, aTap, 0);
			dmu.logValues(logger);
		}

		// solve
		double[] results = uec.solve(index, dmu, null);

		// logging
		if (trace) {
			uec.logAnswersArray(logger, "Drive-Transit-Walk Tap-Tap UEC");
			uec.logResultsArray(logger, pTap, aTap);
		}

		// return the array
		return results;

	}

	/**
	 * This method calculates the drive-access portion of the utility for a
	 * given TAZ to a given tap.
	 * 
	 * @param taz
	 *            The TAZ
	 * @param tapPosition
	 *            The position of the Tap in the MGRA manager
	 * @param trace
	 *            True if debug calculations are to be written to the logger for
	 *            this mgra-tap pair.
	 * @return A drive-access utility for the Taz-Tap pair.
	 */
	public double calculateDriveAccessUtilityForTaz(int taz, int tapPosition,
			AccessMode aMode, boolean trace) {

		dmu.setAccessMode(aMode.ordinal());
		dmu.setDriveDistToTap(tazManager.getTapDist(taz, tapPosition, aMode));
		dmu.setDriveTimeToTap(tazManager.getTapTime(taz, tapPosition, aMode));
		dmu.setTapMgraWalkTime(0.0f);
		dmu.setCarToStationWalkTime(0f);

		if (trace)
			dmu.logValues(logger);

		// solve
		double[] results = driveAccessUEC.solve(index, dmu, null);

		// logging
		if (trace) {
			driveAccessUEC.logAnswersArray(logger, "DriveAccess UEC");
			driveAccessUEC.logResultsArray(logger, taz, tapPosition);
		}

		return results[0];

	}

	/**
	 * @return the origin MGRA to TAP walk time stored for the best TAP-TAP
	 *         pair.
	 */
	public float getBestWalkAccessTime() {
		return bestWalkAccessTime;
	}

	/**
	 * @return the TAP to destination MGRA drive time stored for the best
	 *         TAP-TAP pair.
	 */
	public float getBestDriveEgressTime() {
		return bestDriveEgressTime;
	}

	/**
	 * @return the origin MGRA to TAP drive time stored for the best TAP-TAP
	 *         pair.
	 */
	public float getBestDriveAccessTime() {
		return bestDriveAccessTime;
	}

	/**
	 * @return the TAP to destination MGRA walk time stored for the best TAP-TAP
	 *         pair.
	 */
	public float getBestWalkEgressTime() {
		return bestWalkEgressTime;
	}

	/**
	 * set a Logger object which has been configured to direct logging to a
	 * specific file.
	 */
	public void setLogger(Logger newLogger) {
		logger = newLogger;
	}
}
