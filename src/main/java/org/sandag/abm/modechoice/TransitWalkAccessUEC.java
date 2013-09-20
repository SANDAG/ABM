/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.modechoice;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * WalkPathUEC calculates the best walk-transit utilities for a given MGRA pair.
 * 
 * @author Joel Freedman
 * @version 1.0, May 2009
 */
public class TransitWalkAccessUEC
        extends TransitPathUEC
{

    public static final int             EA          = 0;
    public static final int             AM          = 1;
    public static final int             MD          = 2;
    public static final int             PM          = 3;
    public static final int             EV          = 4;
    public static final String[]        PERIODS     = {"EA", "AM", "MD", "PM", "EV"};
    public static final int             NUM_PERIODS = PERIODS.length;

    // DMU for this UEC
    private TransitWalkAccessDMU        dmu         = new TransitWalkAccessDMU();

    private TapDataManager              tapManager;
    private MgraDataManager             mgraManager;

    // this is only used if piece-wise utilities are being computed
    private UtilityExpressionCalculator walkAccessUEC;

    private float                       bestWalkAccessTime;
    private float                       bestWalkEgressTime;

    /**
     * Constructor.
     * 
     * @param rbMap
     *            HashMap<String, String>
     * @param UECFileName
     *            The path/name of the UEC containing the walk-transit model.
     * @param modelSheet
     *            The sheet (0-indexed) containing the model specification.
     * @param dataSheet
     *            The sheet (0-indexed) containing the data specification.
     */
    public TransitWalkAccessUEC(HashMap<String, String> rbMap, String UECFileName, int modelSheet,
            int dataSheet)
    {
        super(rbMap);
        UECFile = new File(UECFileName);
        createUEC(UECFile, modelSheet, dataSheet, rbMap, dmu);
        tapManager = TapDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
    }

    /**
     * Create a walk-transit access UECFile. This method is only required if piece-wise utilities are built, where TAP-TAP pair utilities are computed
     * separately from MGRA-TAP walk utilities, to cut down on processing time.
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
    public void createWalkAccessUEC(HashMap<String, String> rbMap, String UECFileName,
            int modelSheet, int dataSheet)
    {
        UECFile = new File(UECFileName);
        walkAccessUEC = new UtilityExpressionCalculator(UECFile, modelSheet, dataSheet, rbMap, dmu);

    }

    /**
     * This is the main method that finds the best TAP-pairs for each ride mode. It cycles through walk TAPs at the origin end (associated with the
     * origin MGRA) and alighting TAPs at the destination end (associated with the destination MGRA) and calculates a utility for every available ride
     * mode for each TAP pair. It compares the utility calculated for that TAP-pair to previously calculated utilities and stores the origin and
     * destination TAP that had the best utility for each ride mode.
     * 
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * 
     */
    public void findBestWalkTransitWalkTaps(int pMgra, int aMgra, boolean debug)
    {

        super.clearArrays(Double.NEGATIVE_INFINITY);
        bestWalkAccessTime = 0;
        bestWalkEgressTime = 0;

        int[] pMgraSet = mgraManager.getMgraWlkTapsDistArray()[pMgra][0];
        int[] aMgraSet = mgraManager.getMgraWlkTapsDistArray()[aMgra][0];

        if (pMgraSet == null || aMgraSet == null)
        {
            return;
        }

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz))
        {
            writeCalculations = true;
        }

        int pPos = -1;
        for (int pTap : pMgraSet)
        {
            // used to know where we are in time/dist arrays for taps
            pPos++;

            // Set DMU values
            float pWalkTime = mgraManager.getMgraToTapWalkTime(pMgra, pPos);
            dmu.setMgraTapWalkTime(pWalkTime);

            int aPos = -1;
            for (int aTap : aMgraSet)
            {
                // used to know where we are in time/dist arrays for taps
                aPos++;

                // Set DMU values
                float aWalkTime = mgraManager.getMgraToTapWalkTime(aMgra, aPos);
                dmu.setTapMgraWalkTime(aWalkTime);

                double[] results = calculateUtilitiesForTapPair(pTap, aTap, writeCalculations);

                // logging
                if (debug)
                {
                    uec.logAnswersArray(logger, "Walk-Transit-Walk Utility");
                    uec.logResultsArray(logger, pTap, aTap);
                }

                // compare the utilities for this TAP pair to previously calculated
                // utilities
                // for each ride mode and store the TAP numbers if this TAP pair is
                // the best.
                boolean foundNewBestPath = super.comparePaths(results, pTap, aTap,
                        writeCalculations);

                if (foundNewBestPath)
                {
                    bestWalkAccessTime = pWalkTime;
                    bestWalkEgressTime = aWalkTime;
                }
            }
        }
        if (writeCalculations || debug)
        {
            logBestUtilities(logger);
        }
    }

    /**
     * This method calculates the utilities for a given Tap pair. It is called from the method @link {@link #findBestWalkTaps(int, int)}. The walk
     * times in the dmu must be set separately, or set to 0 for a set of utilities that are mgra-independent.
     * 
     * @param pTap
     *            The origin/production Tap
     * @param aTap
     *            The destination/attraction Tap.
     * @param trace
     *            True if debug calculations are to be written to the logger for this Tap-pair.
     * @return A set of utilities for the Tap-pair, dimensioned by ride mode in @link <Modes>.
     */
    public double[] calculateUtilitiesForTapPair(int pTap, int aTap, boolean trace)
    {

        // set up the index and dmu objects
        index.setOriginZone(pTap);
        index.setDestZone(aTap);
        dmu.setEscalatorTime(tapManager.getEscalatorTime(pTap));

        // log DMU values
        if (trace)
        {
            if (Arrays.binarySearch(tapManager.getTaps(), pTap) > 0
                    && Arrays.binarySearch(tapManager.getTaps(), aTap) > 0)
                uec.logDataValues(logger, pTap, aTap, 0);
            dmu.logValues(logger);
        }

        // solve
        double[] results = uec.solve(index, dmu, null);

        // logging
        if (trace)
        {
            uec.logAnswersArray(logger, "Walk Tap-Tap UEC");
            uec.logResultsArray(logger, pTap, aTap);
        }

        // return the array
        return results;

    }

    /**
     * This method calculates the walk-access utility for a given mgra to a given tap.
     * 
     * @param mgra
     *            The mgra
     * @param tapPosition
     *            The position of the Tap in the MGRA manager
     * @param trace
     *            True if debug calculations are to be written to the logger for this mgra-tap pair.
     * @return A walk-access utility for the mgra-Tap pair.
     */
    public double calculateWalkAccessUtilityForMgra(int mgra, int tapPosition, boolean trace)
    {

        float walkTime = mgraManager.getMgraToTapWalkTime(mgra, tapPosition);
        dmu.setMgraTapWalkTime(walkTime);
        dmu.setTapMgraWalkTime(0.0f);

        if (trace) dmu.logValues(logger);

        // solve
        double[] results = walkAccessUEC.solve(index, dmu, null);

        // logging
        if (trace)
        {
            walkAccessUEC.logAnswersArray(logger, "WalkAccess UEC");
            walkAccessUEC.logResultsArray(logger, mgra, tapPosition);
        }

        return results[0];

    }

    /**
     * @return the origin MGRA to TAP walk time stored for the best TAP-TAP pair.
     */
    public float getBestWalkAccessTime()
    {
        return bestWalkAccessTime;
    }

    /**
     * @return the TAP to destination MGRA walk time stored for the best TAP-TAP pair.
     */
    public float getBestWalkEgressTime()
    {
        return bestWalkEgressTime;
    }

    /**
     * set a Logger object which has been configured to direct logging to a specific file.
     */
    public void setLogger(Logger newLogger)
    {
        logger = newLogger;
    }
}
