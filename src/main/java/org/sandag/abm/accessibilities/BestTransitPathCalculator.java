/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.Modes.AccessMode;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.Tracer;

/**
 * WalkPathUEC calculates the best walk-transit utilities for a given MGRA pair.
 * 
 * @author Joel Freedman
 * @version 1.0, May 2009
 */
public class BestTransitPathCalculator
        implements Serializable
{

    private transient Logger              logger      = Logger.getLogger("bestPath");

    private Object                        lock        = new Object();

    private static final int              EA          = TransitWalkAccessUEC.EA;
    private static final int              AM          = TransitWalkAccessUEC.AM;
    private static final int              MD          = TransitWalkAccessUEC.MD;
    private static final int              PM          = TransitWalkAccessUEC.PM;
    private static final int              EV          = TransitWalkAccessUEC.EV;
    public static final int               NUM_PERIODS = TransitWalkAccessUEC.PERIODS.length;

    public static final int               WTW         = 1;
    public static final int               DTW         = 2;
    public static final int               WTD         = 3;
    public static final int               NUM_ACC_EGR = 3;

    // seek and trace
    private boolean                       trace;
    private int[]                         traceOtaz;
    private int[]                         traceDtaz;
    protected Tracer                      tracer;

    // DMUs for this UEC
    private TransitWalkAccessDMU          walkDmu     = new TransitWalkAccessDMU();
    private TransitDriveAccessDMU         driveDmu    = new TransitDriveAccessDMU();

    private TazDataManager                tazManager;
    private TapDataManager                tapManager;
    private MgraDataManager               mgraManager;

    private int                           maxMgra;
    private int                           maxTap;
    private int                           maxTaz;

    // piece-wise utilities are being computed
    private UtilityExpressionCalculator   walkAccessUEC;
    private UtilityExpressionCalculator   walkEgressUEC;
    private UtilityExpressionCalculator   driveAccessUEC;
    private UtilityExpressionCalculator   driveEgressUEC;
    private UtilityExpressionCalculator[] tapToTapUEC = new UtilityExpressionCalculator[NUM_PERIODS];

    // these arrays are shared by the BestTransitPathCalculator objects created
    // for each hh choice model object
    private double[][][]                  storedWalkAccessUtils;
    private double[][][]                  storedDriveAccessUtils;
    private double[][][]                  storedWalkEgressUtils;
    private double[][][]                  storedDriveEgressUtils;
    private double[][][][][]              storedTapToTapUtils;

    private StoredUtilityData             storedDataObject;

    private double                        pWalkTime;
    private double                        aWalkTime;
    private double                        pDriveTime;
    private double                        aDriveTime;

    private double[]                      combinedUtilities;

    private IndexValues                   index       = new IndexValues();

    private double[]                      bestUtilities;
    private int[]                         bestPTap;
    private int[]                         bestATap;

    private double[]                      bestAccessTime;
    private double[]                      bestEgressTime;

    private Modes.TransitMode[]           tm;

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
    public BestTransitPathCalculator(HashMap<String, String> rbMap)
    {

        // read in resource bundle properties
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if (trace)
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }

        tm = Modes.TransitMode.values();

        // bestUtility by Transit Ride mode
        bestUtilities = new double[tm.length];
        bestPTap = new int[tm.length];
        bestATap = new int[tm.length];
        bestAccessTime = new double[tm.length];
        bestEgressTime = new double[tm.length];

        String uecPath = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = uecPath
                + Util.getStringValueFromPropertyMap(rbMap, "utility.bestTransitPath.uec.file");

        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.data.page");

        int walkAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkAccess.page");
        int driveAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveAccess.page");
        int walkEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkEgress.page");
        int driveEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveEgress.page");

        int[] tapToTapPages = new int[NUM_PERIODS];
        tapToTapPages[EA] = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.tapToTap.ea.page");
        tapToTapPages[AM] = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.tapToTap.am.page");
        tapToTapPages[MD] = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.tapToTap.md.page");
        tapToTapPages[PM] = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.tapToTap.pm.page");
        tapToTapPages[EV] = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.tapToTap.ev.page");

        File uecFile = new File(uecFileName);
        walkAccessUEC = createUEC(uecFile, walkAccessPage, dataPage, rbMap, walkDmu);
        driveAccessUEC = createUEC(uecFile, driveAccessPage, dataPage, rbMap, driveDmu);
        walkEgressUEC = createUEC(uecFile, walkEgressPage, dataPage, rbMap, walkDmu);
        driveEgressUEC = createUEC(uecFile, driveEgressPage, dataPage, rbMap, driveDmu);

        for (int i = 0; i < NUM_PERIODS; i++)
            tapToTapUEC[i] = createUEC(uecFile, tapToTapPages[i], dataPage, rbMap, walkDmu);

        mgraManager = MgraDataManager.getInstance(rbMap);
        tazManager = TazDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);

        maxMgra = mgraManager.getMaxMgra();
        maxTap = mgraManager.getMaxTap();
        maxTaz = tazManager.getMaxTaz();

        // these arrays are shared by the BestTransitPathCalculator objects
        // created for each hh choice model object
        storedDataObject = StoredUtilityData.getInstance(maxMgra, maxTap, maxTaz, NUM_ACC_EGR,
                NUM_PERIODS);
        storedWalkAccessUtils = storedDataObject.getStoredWalkAccessUtils();
        storedDriveAccessUtils = storedDataObject.getStoredDriveAccessUtils();
        storedWalkEgressUtils = storedDataObject.getStoredWalkEgressUtils();
        storedDriveEgressUtils = storedDataObject.getStoredDriveEgressUtils();
        storedTapToTapUtils = storedDataObject.getStoredDepartPeriodTapTapUtils();

        // use the walk access UEC to get the number of alternatives for
        // dimensioning
        // combined utility array.
        // access, egress, and tap-tap UECs all have same number
        combinedUtilities = new double[walkAccessUEC.getNumberOfAlternatives()];

    }

    /**
     * find the walk time from the specified mgra to tap.
     * 
     * @param pMgra
     * @param pTap
     * @return
     */
    public static double findWalkTransitAccessTime(int pMgra, int pTap)
    {

        double accTime = -1;

        MgraDataManager mgraMgr = MgraDataManager.getInstance();

        // get taps with walk access from mgra
        int[] pTapSet = mgraMgr.getMgraWlkTapsDistArray()[pMgra][0];

        if (pTapSet != null)
        {
            // loop through tap set until the specified tap is found, then get
            // walk time from mgraManager
            int pPos = -1;
            for (int tap : pTapSet)
            {

                pPos++;

                if (tap == pTap)
                {
                    accTime = mgraMgr.getMgraToTapWalkBoardTime(pMgra, pPos);
                    break;
                }
            }
        }

        return accTime;
    }

    /**
     * find the drive time from the specified taz(mgra) to tap.
     * 
     * @param pMgra
     * @param pTap
     * @return
     */
    public static double findDriveTransitAccessTime(int pMgra, int pTap)
    {

        double accTime = -1;

        MgraDataManager mgraMgr = MgraDataManager.getInstance();
        int pTaz = mgraMgr.getTaz(pMgra);

        // get taps with walk access from mgra
        TazDataManager tazManager = TazDataManager.getInstance();
        int[] pTapSet = tazManager.getParkRideOrKissRideTapsForZone(pTaz, AccessMode.PARK_N_RIDE);

        if (pTapSet != null)
        {
            // loop through tap set until the specified tap is found, then get
            // walk time from mgraManager
            int pPos = -1;
            for (int tap : pTapSet)
            {

                pPos++;

                if (tap == pTap)
                {
                    accTime = tazManager.getTapTime(pTaz, pPos, AccessMode.PARK_N_RIDE);
                    break;
                }
            }
        }

        return accTime;
    }

    /**
     * find the walk time from the specified tap to mgra.
     * 
     * @param aTap
     * @param aMgra
     * @return
     */
    public static double findWalkTransitEgressTime(int aMgra, int aTap)
    {

        double egrTime = -1;

        // get taps with walk egress to mgra
        MgraDataManager mgraMgr = MgraDataManager.getInstance();
        int[] aTapSet = mgraMgr.getMgraWlkTapsDistArray()[aMgra][0];

        if (aTapSet != null)
        {
            // loop through tap set until the specified tap is found, then get
            // walk time from mgraManager
            int aPos = -1;
            for (int tap : aTapSet)
            {

                aPos++;

                if (tap == aTap)
                {
                    egrTime = mgraMgr.getMgraToTapWalkAlightTime(aMgra, aPos);
                    break;
                }
            }
        }

        return egrTime;
    }

    /**
     * find the walk time from the specified tap to mgra.
     * 
     * @param aTap
     * @param aMgra
     * @return
     */
    public static double findDriveTransitEgressTime(int aMgra, int aTap)
    {

        double egrTime = -1;

        MgraDataManager mgraMgr = MgraDataManager.getInstance();
        int aTaz = mgraMgr.getTaz(aMgra);

        // get taps with walk access from mgra
        TazDataManager tazManager = TazDataManager.getInstance();
        int[] aTapSet = tazManager.getParkRideOrKissRideTapsForZone(aTaz, AccessMode.PARK_N_RIDE);

        if (aTapSet != null)
        {
            // loop through tap set until the specified tap is found, then get
            // walk time from mgraManager
            int aPos = -1;
            for (int tap : aTapSet)
            {

                aPos++;

                if (tap == aTap)
                {
                    egrTime = tazManager.getTapTime(aTaz, aPos, AccessMode.PARK_N_RIDE);
                    break;
                }
            }
        }

        return egrTime;
    }

    /**
     * This is the main method that finds the best TAP-pairs for each ride mode.
     * It cycles through walk TAPs at the origin end (associated with the origin
     * MGRA) and alighting TAPs at the destination end (associated with the
     * destination MGRA) and calculates a utility for every available ride mode
     * for each TAP pair. It compares the utility calculated for that TAP-pair
     * to previously calculated utilities and stores the origin and destination
     * TAP that had the best utility for each ride mode.
     * 
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * 
     */
    public void findBestWalkTransitWalkTaps(int period, int pMgra, int aMgra, boolean debug,
            Logger myLogger)
    {

        // int dummy=0;
        // if ( aMgra == 1074 )
        // dummy = 1;

        clearBestArrays(Double.NEGATIVE_INFINITY);

        int[] pMgraSet = mgraManager.getMgraWlkTapsDistArray()[pMgra][0];
        int[] aMgraSet = mgraManager.getMgraWlkTapsDistArray()[aMgra][0];

        if (pMgraSet == null || aMgraSet == null)
        {
            return;
        }

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        boolean writeCalculations = false;
        if ((tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz)) || debug)
        {
            writeCalculations = true;
        }

        int pPos = -1;
        for (int pTap : pMgraSet)
        {
            // used to know where we are in time/dist arrays for taps
            pPos++;

            // Set the pMgra to pTap walk access utility values, if they haven't
            // already been computed.
            setWalkAccessUtility(pMgra, pPos, pTap, writeCalculations, myLogger);

            int aPos = -1;
            for (int aTap : aMgraSet)
            {
                // used to know where we are in time/dist arrays for taps
                aPos++;

                // set the pTap to aTap utility values, if they haven't already
                // been
                // computed.
                setUtilitiesForTapPair(WTW, period, pTap, aTap, writeCalculations, myLogger);

                // Set the aTap to aMgra walk egress utility values, if they
                // haven't
                // already been computed.
                setWalkEgressUtility(aTap, aMgra, aPos, writeCalculations, myLogger);

                // if ( aTap == 0 || pTap == 0 || aMgra == 0 || pMgra == 0 ) {
                // System.out.println( "aTap=" + aTap + "pTap=" + pTap +
                // "aMgra=" + aMgra + "pMgra=" + pMgra + "period=" + period );
                // System.out.flush();
                // System.exit( -1 );
                // }
                // else if ( storedWalkAccessUtils[pMgra][pTap] == null ) {
                // System.out.println( "aTap=" + aTap + "pTap=" + pTap +
                // "aMgra=" + aMgra + "pMgra=" + pMgra + "period=" + period );
                // System.out.println(
                // "storedWalkAccessUtils[pMgra][pTap] == null" );
                // System.out.flush();
                // System.exit( -1 );
                // }
                // else if ( storedWalkEgressUtils[aTap][aMgra] == null ) {
                // System.out.println( "aTap=" + aTap + "pTap=" + pTap +
                // "aMgra=" + aMgra + "pMgra=" + pMgra + "period=" + period );
                // System.out.println(
                // "storedWalkEgressUtils[aTap][aMgra] == null" );
                // System.out.flush();
                // System.exit( -1 );
                // }
                // else if (
                // storedDepartPeriodTapTapUtils[WTW][period][pTap][aTap] ==
                // null ) {
                // System.out.println( "aTap=" + aTap + "pTap=" + pTap +
                // "aMgra=" + aMgra + "pMgra=" + pMgra + "period=" + period );
                // System.out.println(
                // "storedDepartPeriodTapTapUtils[WTW][period][pTap][aTap] == null"
                // );
                // System.out.flush();
                // System.exit( -1 );
                // }

                // compare the utilities for this TAP pair to previously
                // calculated
                // utilities for each ride mode and store the TAP numbers if
                // this
                // TAP pair is the best.
                try
                {
                    for (int i = 0; i < combinedUtilities.length; i++)
                        combinedUtilities[i] = storedWalkAccessUtils[pMgra][pTap][i]
                                + storedTapToTapUtils[WTW][period][pTap][aTap][i]
                                + storedWalkEgressUtils[aTap][aMgra][i];
                } catch (Exception e)
                {
                    System.out.println("exception computing combinedUtilities for WTW");
                    System.out.println("aTap=" + aTap + "pTap=" + pTap + "aMgra=" + aMgra
                            + "pMgra=" + pMgra + "period=" + period);
                    System.out.flush();
                    System.exit(-1);
                }

                comparePaths(combinedUtilities, pTap, aTap, writeCalculations, myLogger);

            }
        }
        if (writeCalculations)
        {
            logBestUtilities(myLogger);
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
     * @param period
     *            The departure period ofr which the best path is to be
     *            determined
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * @param debug
     *            A boolean to be set to true if debug information is desired.
     * 
     */
    public void findBestDriveTransitWalkTaps(int period, int pMgra, int aMgra, boolean debug,
            Logger myLogger)
    {

        // int dummy=0;
        // if ( aMgra == 1074 )
        // dummy = 1;

        clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        if (tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode) == null
                || mgraManager.getMgraWlkTapsDistArray()[aMgra][0] == null)
        {
            return;
        }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }

        float[][][] tapParkingInfo = tapManager.getTapParkingInfo();

        int pPos = -1;
        int[] pTapArray = tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode);
        for (int pTap : pTapArray)
        {
            pPos++; // used to know where we are in time/dist arrays for taps

            // Set the pTaz to pTap drive access utility values, if they haven't
            // already been computed.
            setDriveAccessUtility(pTaz, pPos, pTap, accMode, writeCalculations, myLogger);

            int lotID = (int) tapParkingInfo[pTap][0][0]; // lot ID
            float lotCapacity = tapParkingInfo[pTap][2][0]; // lot capacity

            if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                    || (accMode == AccessMode.KISS_N_RIDE))
            {

                int aPos = -1;
                for (int aTap : mgraManager.getMgraWlkTapsDistArray()[aMgra][0])
                {
                    aPos++;

                    // Set the aTap to aMgra walk egress utility values, if they
                    // haven't already been computed.
                    setWalkEgressUtility(aTap, aMgra, aPos, writeCalculations, myLogger);

                    // int dummy=0;
                    // if ( pMgra==18736 && aMgra==4309 && period==1 ){
                    // if ( (pTap==839 && aTap==1889) || (pTap==839 &&
                    // aTap==2151) || (pTap==839 && aTap==1880) ){
                    // dummy = 1;
                    // }
                    // }

                    // set the pTap to aTap utility values, if they haven't
                    // already
                    // been computed.
                    setUtilitiesForTapPair(DTW, period, pTap, aTap, writeCalculations, myLogger);

                    // if ( aTap == 0 || pTap == 0 || aMgra == 0 || pMgra == 0 )
                    // {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if ( storedDriveAccessUtils[pTaz][pTap] == null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedDriveAccessUtils[pTaz][pTap] == null" );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if ( storedWalkEgressUtils[aTap][aMgra] == null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedWalkEgressUtils[aTap][aMgra] == null" );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if (
                    // storedDepartPeriodTapTapUtils[DTW][period][pTap][aTap] ==
                    // null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedDepartPeriodTapTapUtils[DTW][period][pTap][aTap] == null"
                    // );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }

                    // compare the utilities for this TAP pair to previously
                    // calculated
                    // utilities for each ride mode and store the TAP numbers if
                    // this
                    // TAP pair is the best.
                    try
                    {
                        for (int i = 0; i < combinedUtilities.length; i++)
                            combinedUtilities[i] = storedDriveAccessUtils[pTaz][pTap][i]
                                    + storedTapToTapUtils[DTW][period][pTap][aTap][i]
                                    + storedWalkEgressUtils[aTap][aMgra][i];
                    } catch (Exception e)
                    {
                        System.out.println("exception computing combinedUtilities for DTW");
                        System.out.println("aTap=" + aTap + ",pTap=" + pTap + ",aMgra=" + aMgra
                                + ",pMgra=" + pMgra + ",period=" + period);
                        System.out.flush();
                        System.exit(-1);
                    }

                    comparePaths(combinedUtilities, pTap, aTap, writeCalculations, myLogger);

                }
            }
            if (writeCalculations)
            {
                logBestUtilities(myLogger);
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
    public void findBestWalkTransitDriveTaps(int period, int pMgra, int aMgra, boolean debug,
            Logger myLogger)
    {

        // int dummy=0;
        // if ( aMgra == 1074 )
        // dummy = 1;

        clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        if (mgraManager.getMgraWlkTapsDistArray()[pMgra][0] == null
                || tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode) == null)
        {
            return;
        }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }

        int pPos = -1;
        for (int pTap : mgraManager.getMgraWlkTapsDistArray()[pMgra][0])
        {
            pPos++; // used to know where we are in time/dist arrays for taps

            // Set the pMgra to pTap walk access utility values, if they haven't
            // already been computed.
            setWalkAccessUtility(pMgra, pPos, pTap, writeCalculations, myLogger);

            int aPos = -1;
            for (int aTap : tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode))
            {
                aPos++;

                int lotID = (int) tapManager.getTapParkingInfo()[aTap][0][0]; // lot
                // ID
                float lotCapacity = tapManager.getTapParkingInfo()[aTap][2][0]; // lot
                // capacity
                if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                        || (accMode == AccessMode.KISS_N_RIDE))
                {

                    // Set the pTaz to pTap drive access utility values, if they
                    // haven't already been computed.
                    setDriveEgressUtility(aTap, aTaz, aPos, accMode, writeCalculations, myLogger);

                    // set the pTap to aTap utility values, if they haven't
                    // already
                    // been computed.
                    setUtilitiesForTapPair(WTD, period, pTap, aTap, writeCalculations, myLogger);

                    // if ( aTap == 0 || pTap == 0 || aMgra == 0 || pMgra == 0 )
                    // {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if ( storedWalkAccessUtils[pMgra][pTap] == null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedWalkAccessUtils[pMgra][pTap] == null" );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if ( storedDriveEgressUtils[aTap][aTaz] == null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedDriveEgressUtils[aTap][aTaz] == null" );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }
                    // else if (
                    // storedDepartPeriodTapTapUtils[WTD][period][pTap][aTap] ==
                    // null ) {
                    // System.out.println( "aTap=" + aTap + ",pTap=" + pTap +
                    // ",aMgra=" + aMgra + ",pMgra=" + pMgra + ",period=" +
                    // period );
                    // System.out.println(
                    // "storedDepartPeriodTapTapUtils[WTD][period][pTap][aTap] == null"
                    // );
                    // System.out.flush();
                    // System.exit( -1 );
                    // }

                    // compare the utilities for this TAP pair to previously
                    // calculated
                    // utilities for each ride mode and store the TAP numbers if
                    // this
                    // TAP pair is the best.
                    try
                    {
                        for (int i = 0; i < combinedUtilities.length; i++)
                            combinedUtilities[i] = storedWalkAccessUtils[pMgra][pTap][i]
                                    + storedTapToTapUtils[WTD][period][pTap][aTap][i]
                                    + storedDriveEgressUtils[aTap][aTaz][i];
                    } catch (Exception e)
                    {
                        System.out.println("exception computing combinedUtilities for WTD");
                        System.out.println("aTap=" + aTap + ",pTap=" + pTap + ",aMgra=" + aMgra
                                + ",pMgra=" + pMgra + ",period=" + period);
                        System.out.flush();
                        System.exit(-1);
                    }

                    comparePaths(combinedUtilities, pTap, aTap, writeCalculations, myLogger);

                }
            }
            if (writeCalculations)
            {
                logBestUtilities(myLogger);
            }
        }
    }

    private void setWalkAccessUtility(int pMgra, int pPos, int pTap, boolean myTrace,
            Logger myLogger)
    {
        pWalkTime = mgraManager.getMgraToTapWalkBoardTime(pMgra, pPos);
        if (storedWalkAccessUtils[pMgra][pTap] == null)
        {
            walkDmu.setMgraTapWalkTime(pWalkTime);
            storedWalkAccessUtils[pMgra][pTap] = walkAccessUEC.solve(index, walkDmu, null);

            // logging
            if (myTrace)
            {
                walkAccessUEC.logAnswersArray(myLogger, "Walk Orig Mgra=" + pMgra + ", to pTap="
                        + pTap + " Utility Piece");
            }
        }
    }

    private void setDriveAccessUtility(int pTaz, int pPos, int pTap, Modes.AccessMode accMode,
            boolean myTrace, Logger myLogger)
    {
        pDriveTime = tazManager.getTapTime(pTaz, pPos, accMode);
        if (storedDriveAccessUtils[pTaz][pTap] == null)
        {
            driveDmu.setDriveDistToTap(tazManager.getTapDist(pTaz, pPos, accMode));
            driveDmu.setDriveTimeToTap(pDriveTime);
            driveDmu.setEscalatorTime(tapManager.getEscalatorTime(pTap));
            storedDriveAccessUtils[pTaz][pTap] = driveAccessUEC.solve(index, driveDmu, null);

            // logging
            if (myTrace)
            {
                driveAccessUEC.logAnswersArray(myLogger, "Drive Orig Taz=" + pTaz + ", to pTap="
                        + pTap + " Utility Piece");
            }
        }
    }

    private void setWalkEgressUtility(int aTap, int aMgra, int aPos, boolean myTrace,
            Logger myLogger)
    {
        aWalkTime = mgraManager.getMgraToTapWalkAlightTime(aMgra, aPos);
        if (storedWalkEgressUtils[aTap][aMgra] == null)
        {
            walkDmu.setTapMgraWalkTime(aWalkTime);
            storedWalkEgressUtils[aTap][aMgra] = walkEgressUEC.solve(index, walkDmu, null);

            // logging
            if (myTrace)
            {
                walkEgressUEC.logAnswersArray(myLogger, "Walk aTap=" + aTap + ", from dest mgra="
                        + aMgra + " Utility Piece");
            }
        }
    }

    private void setDriveEgressUtility(int aTap, int aTaz, int aPos, Modes.AccessMode accMode,
            boolean myTrace, Logger myLogger)
    {
        aDriveTime = tazManager.getTapTime(aTaz, aPos, accMode);
        if (storedDriveEgressUtils[aTap][aTaz] == null)
        {
            driveDmu.setDriveDistToTap(tazManager.getTapDist(aTaz, aPos, accMode));
            driveDmu.setDriveTimeToTap(aDriveTime);
            driveDmu.setEscalatorTime(tapManager.getEscalatorTime(aTap));
            storedDriveEgressUtils[aTap][aTaz] = driveEgressUEC.solve(index, driveDmu, null);

            // logging
            if (myTrace)
            {
                driveEgressUEC.logAnswersArray(myLogger, "Drive Tap to Dest Taz Utility Piece");
                driveEgressUEC.logAnswersArray(myLogger, "Drive aTap=" + aTap + ", from dest taz="
                        + aTaz + " Utility Piece");
            }
        }
    }

    /**
     * This method calculates the utilities for a given Tap pair. It is called
     * from the method @link {@link #findBestWalkTaps(int, int)}. The walk times
     * in the dmu must be set separately, or set to 0 for a set of utilities
     * that are mgra-independent.
     * 
     * @param accEgr
     *            The access\egress mode
     * @param period
     *            The skim period (1-initialized)
     * @param pTap
     *            The origin/production Tap
     * @param aTap
     *            The destination/attraction Tap.
     * @param myTrace
     *            True if debug calculations are to be written to the logger for
     *            this Tap-pair.
     * @return A set of utilities for the Tap-pair, dimensioned by ride mode in @link
     *         <Modes>.
     */
    private void setUtilitiesForTapPair(int accEgr, int period, int pTap, int aTap,
            boolean myTrace, Logger myLogger)
    {

        // allocate space for the pTap if necessary
        if (storedDepartPeriodTapTapUtils[accEgr][period] == null)
        {
            synchronized (storedDepartPeriodTapTapUtils[accEgr]) {
                if (storedDepartPeriodTapTapUtils[accEgr][period] == null) {
                    storedDepartPeriodTapTapUtils[accEgr][period] = new double[maxTap + 1][][];
                }
            }
            if (storedDepartPeriodTapTapUtils[accEgr][period] == null)
            {
                logger.error("error allocating array of length " + (maxTap + 1)
                        + " for storedDepartPeriodTapTapUtils[accEgr][period].");
                throw new RuntimeException();
            }
        }

        // allocate space for the aTap if necessary
        if (storedDepartPeriodTapTapUtils[accEgr][period][pTap] == null)
        {
            synchronized (storedDepartPeriodTapTapUtils[accEgr][period]) {
                if (storedDepartPeriodTapTapUtils[accEgr][period][pTap] == null) {
                    storedDepartPeriodTapTapUtils[accEgr][period][pTap] = new double[maxTap + 1][];
                }
            }
            if (storedDepartPeriodTapTapUtils[accEgr][period][pTap] == null)
            {
                logger.error("error allocating array of length " + (maxTap + 1)
                        + " for storedDepartPeriodTapTapUtils[accEgr][period][pTap].");
                throw new RuntimeException();
            }
        }

        // calculate the tap-tap utilities if they haven't already been.
        if (storedTapToTapUtils[accEgr][period][pTap][aTap] == null)
        {

            // set up the index and dmu objects
            index.setOriginZone(pTap);
            index.setDestZone(aTap);
            // walkDmu.setEscalatorTime(tapManager.getEscalatorTime(pTap));

            // log DMU values
            if (myTrace)
            {
                if (Arrays.binarySearch(tapManager.getTaps(), pTap) > 0
                        && Arrays.binarySearch(tapManager.getTaps(), aTap) > 0)
                    tapToTapUEC[period].logDataValues(myLogger, pTap, aTap, 0);
                walkDmu.logValues(myLogger);
            }

            // solve
            storedTapToTapUtils[accEgr][period][pTap][aTap] = tapToTapUEC[period].solve(index,
                    walkDmu, null);
            if (storedTapToTapUtils[accEgr][period][pTap][aTap] == null)
            {
                System.out
                        .println("error calcuating UEC to store results in storedDepartPeriodTapTapUtils[accEgr][period][pTap][aTap].");
                System.out.println("accEgr=" + accEgr + ",period=" + period + ",pTap=" + pTap
                        + ",aTap=" + aTap);
                System.out.flush();
                System.exit(-1);
            }

            // logging
            if (myTrace)
            {
                tapToTapUEC[period].logAnswersArray(myLogger, "pTap=" + pTap + " to aTap=" + aTap
                        + " Utility Piece");
                tapToTapUEC[period].logResultsArray(myLogger, pTap, aTap);
            }

        }

    }

    /**
     * @return the origin to TAP access time stored for the best TAP-TAP pair.
     */
    public double getBestAccessTime(int rideModeIndex)
    {
        return bestAccessTime[rideModeIndex];
    }

    /**
     * @return the TAP to destination access time stored for the best TAP-TAP
     *         pair.
     */
    public double getBestEgressTime(int rideModeIndex)
    {
        return bestEgressTime[rideModeIndex];
    }

    /**
     * Compare the paths calculated for this TAP-pair to the paths for
     * previously- calculated TAP-pairs for each ride mode. If the current path
     * is the best path, for that ride mode, set the bestUtilities[], bestPTap[]
     * and bestATap[], bestAccessTime[] and bestEgressTime[] for that ride mode.
     * 
     * @param calculatedUtilities
     *            An array of utilities by ride mode.
     * @param pTap
     *            The origin TAP for this set of utilities.
     * @param aTap
     *            The destination TAP for this set of utilities.
     */
    public void comparePaths(double[] calculatedUtilities, int pTap, int aTap, boolean myTrace,
            Logger myLogger)
    {

        for (int i = 0; i < bestUtilities.length; i++)
        {

            if (myTrace)
            {
                myLogger.info("Mode " + i + " calculatedUtility " + calculatedUtilities[i]
                        + ", best utility " + bestUtilities[i]);
            }

            if (calculatedUtilities[i] > bestUtilities[i] && calculatedUtilities[i] > -999)
            {
                bestUtilities[i] = calculatedUtilities[i];
                bestPTap[i] = pTap;
                bestATap[i] = aTap;
                bestAccessTime[i] = pWalkTime;
                bestEgressTime[i] = aWalkTime;

                if (myTrace)
                {
                    myLogger.info("Best utility so far for mode " + tm[i] + " pTap " + pTap
                            + " to aTap " + aTap + " is " + bestUtilities[i]);
                }
            }
        }

    }

    /**
     * Log the best utilities so far to the logger.
     * 
     * @param localLogger
     *            The logger to use for output.
     */
    public void logBestUtilities(Logger localLogger)
    {

        // use the access UEC to get the number and names of alternatives for
        // dimensioning combined utility array.
        // access, egress, and tap-tap UECs all have same set of alternatives.
        UtilityExpressionCalculator uec = walkAccessUEC;

        // create the header
        String header = String.format("%16s", "Alternative");
        header += String.format("%14s", "Utility");
        header += String.format("%14s", "PTap");
        header += String.format("%14s", "ATap");

        localLogger.info("Best Utility and Tap to Tap Pair");
        localLogger.info(header);

        // log the utilities and tap number for each alternative
        for (String altName : uec.getAlternativeNames())
        {
            header = header + String.format("  %16s", altName);
        }
        for (String altName : uec.getAlternativeNames())
        {
            String line = String.format("%16s", altName);
            int index = Modes.getTransitModeIndex(altName);
            line = line + String.format("  %12.4f", bestUtilities[index]);
            line = line + String.format("  %12d", bestPTap[index]);
            line = line + String.format("  %12d", bestATap[index]);

            localLogger.info(line);
        }
    }

    public void setTrace(boolean myTrace)
    {
        tracer.setTrace(myTrace);
    }

    /**
     * Trace calculations for a zone pair.
     * 
     * @param itaz
     * @param jtaz
     * @return true if zone pair should be traced, otherwise false
     */
    public boolean isTraceZonePair(int itaz, int jtaz)
    {
        if (tracer.isTraceOn())
        {
            return tracer.isTraceZonePair(itaz, jtaz);
        } else
        {
            return false;
        }
    }

    /**
     * Get the best utilities.
     * 
     * @return An array of the best utilities, dimensioned by ride-mode in @link
     *         <Modes>.
     */
    public double[] getBestUtilities()
    {
        return bestUtilities;
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
    public UtilityExpressionCalculator createUEC(File uecSpreadsheet, int modelSheet,
            int dataSheet, HashMap<String, String> rbMap, VariableTable dmu)
    {
        return new UtilityExpressionCalculator(uecSpreadsheet, modelSheet, dataSheet, rbMap, dmu);
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
    public void clearBestArrays(double initialValue)
    {
        Arrays.fill(bestUtilities, initialValue);
        Arrays.fill(bestPTap, 0);
        Arrays.fill(bestATap, 0);
        Arrays.fill(bestAccessTime, 0);
        Arrays.fill(bestEgressTime, 0);
    }

    /**
     * Get the best ptap and atap in an array. Only to be called after
     * comparePaths() has been called.
     * 
     * @param transitMode
     *            Mode to look up.
     * @return element 0 = best ptap, element 1 = best atap
     */
    public int[] getBestTaps(Modes.TransitMode transitMode)
    {

        int[] bestTaps = new int[2];

        bestTaps[0] = bestPTap[transitMode.ordinal()];
        bestTaps[1] = bestATap[transitMode.ordinal()];

        return bestTaps;
    }

    /**
     * Get the best transit mode for a given transit path. Returns null if no
     * transit mode has a valid utility. Call only after calling
     * findBestWalkTransitWalkTaps().
     * 
     * @return The best transit mode (highest utility), or null if no modes have
     *         a valid utility.
     */
    public Modes.TransitMode getBestTransitMode()
    {

        int bestMode = -1;
        double bestUtility = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bestUtilities.length; ++i)
        {
            if (bestUtilities[i] > bestUtility)
            {
                bestMode = i;
                bestUtility = bestUtilities[i];
            }
        }

        Modes.TransitMode returnMode = null;
        if (bestMode > -1) returnMode = tm[bestMode];

        return returnMode;
    }

}
