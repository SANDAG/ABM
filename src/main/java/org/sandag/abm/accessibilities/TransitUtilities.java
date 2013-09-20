package org.sandag.abm.accessibilities;

import com.pb.common.util.ResourceUtil;
import com.pb.common.util.Tracer;
import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import java.rmi.RemoteException;
import java.net.UnknownHostException;
import com.pb.common.matrix.MatrixType;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.Util;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitDriveAccessUEC;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
import org.sandag.abm.modechoice.Modes.AccessMode;

/**
 * This class builds utility components for walk-transit.
 * 
 * @author Joel Freedman
 * @version May, 2009
 */
public class TransitUtilities
        implements Serializable
{

    protected transient Logger      logger     = Logger.getLogger(TransitUtilities.class);

    private static final String[]   TAPPERIODS = {"OP", "PK"};

    // store tap-tap exponentiated utilities (period, from tap, to tap, ride mode)
    private double[][][][]          tapExpUtilities;

    // store walk-access mgra-tap exponentiated utilities (mgra, tap)
    private double[][]              wlkAccessExpUtilities;

    // store drive-access taz-tap exponentiated utilities (mgra, tap)
    private double[][]              drvAccessExpUtilities;

    private TransitWalkAccessUEC[]  walkUEC;
    private TransitDriveAccessUEC[] driveUEC;

    private MgraDataManager         mgraManager;

    private boolean                 trace;
    private int                     maxTap, maxMgra, maxTaz;
    private Tracer                  tracer;
    private boolean[]               traceOTap, traceDTap;

    private TapDataManager          tapManager;
    private TazDataManager          tazManager;

    /**
     * Default constructor.
     * 
     * @param rb
     *            The ResourceBundle for this class.
     */
    public TransitUtilities(HashMap<String, String> rbMap)
    {

        // initialize the arrays
        mgraManager = MgraDataManager.getInstance();
        tazManager = TazDataManager.getInstance();
        tapManager = TapDataManager.getInstance(rbMap);

        maxTap = mgraManager.getMaxTap();
        maxMgra = mgraManager.getMaxMgra();
        maxTaz = tazManager.maxTaz;

        logger.info("max Tap " + maxTap);
        logger.info("max Taz " + maxTaz);

        // Create the peak and off-peak UECs
        String uecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.data.page");
        int offpeakModelPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.transit.offpeak.page");
        int peakModelPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.transit.peak.page");
        int walkTransitModelPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.transit.walkaccess.page");
        int driveTransitModelPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.transit.driveaccess.page");

        // check this code if adding more periods
        walkUEC = new TransitWalkAccessUEC[TAPPERIODS.length];
        walkUEC[0] = new TransitWalkAccessUEC(rbMap, uecFileName, offpeakModelPage, dataPage);
        walkUEC[1] = new TransitWalkAccessUEC(rbMap, uecFileName, peakModelPage, dataPage);

        // store the walk-access UEC with the first period UEC
        walkUEC[0].createWalkAccessUEC(rbMap, uecFileName, walkTransitModelPage, dataPage);

        // drive uec
        driveUEC = new TransitDriveAccessUEC[TAPPERIODS.length];
        driveUEC[0] = new TransitDriveAccessUEC(rbMap, uecFileName, offpeakModelPage, dataPage,
                AccessMode.PARK_N_RIDE);
        driveUEC[1] = new TransitDriveAccessUEC(rbMap, uecFileName, peakModelPage, dataPage,
                AccessMode.PARK_N_RIDE);

        driveUEC[0].createDriveAccessUEC(rbMap, uecFileName, driveTransitModelPage, dataPage);

        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        int[] traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        int[] traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        traceOTap = new boolean[maxTap + 1];
        traceDTap = new boolean[maxTap + 1];

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        for (int i = 0; i < traceOtaz.length; i++)
        {

            // get the mgras associated with the origin taz
            int[] mgraList = tazManager.getMgraArray(traceOtaz[i]);
            if (mgraList == null) continue;

            // iterate through them, and add all taps within walking distance
            // to the traceTap array

            for (int oMgra : mgraList)
            {
                if (mgraManager.getMgraWlkTapsDistArray()[oMgra][0] != null)
                {
                    for (int tap : mgraManager.getMgraWlkTapsDistArray()[oMgra][0])
                        traceOTap[tap] = true;
                }
            }

            for (int j = 0; j < traceDtaz.length; j++)
            {
                // get the mgras associated with the origin taz
                mgraList = tazManager.getMgraArray(traceDtaz[j]);
                if (mgraList == null) continue;

                // iterate through them, and add all taps within walking distance
                // to the traceTap array
                for (int dMgra : mgraList)
                {
                    if (mgraManager.getMgraWlkTapsDistArray()[dMgra][0] != null)
                    {
                        for (int tap : mgraManager.getMgraWlkTapsDistArray()[dMgra][0])
                            traceDTap[tap] = true;
                    }
                }

                tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
            }
        }

    }

    /**
     * set the utilities values created by another object by calling calculateUtilityComponents()
     */
    public void setUtilitiesArrays(double[][][] utilitiesArrays)
    {
        wlkAccessExpUtilities = utilitiesArrays[0];
        drvAccessExpUtilities = utilitiesArrays[1];
    }

    /**
     * get the wlk abd drv utilities arrays built by calling calculateUtilityComponents().
     * 
     * @return array of 2 utilities arrays: wlkAccessExpUtilities and drvAccessExpUtilities
     */
    public double[][][] getUtilitiesArrays()
    {
        double[][][] returnArray = new double[2][][];
        returnArray[0] = wlkAccessExpUtilities;
        returnArray[1] = drvAccessExpUtilities;
        return returnArray;
    }

    /**
     * set the array of TAP utilities built by calling calculateUtilityComponents().
     */
    public void setTapUtilitiesArray(double[][][][] tapExpUtils)
    {
        tapExpUtilities = tapExpUtils;
    }

    /**
     * get the array of TAP utilities built by calling calculateUtilityComponents().
     */
    public double[][][][] getTapUtilitiesArray()
    {
        return tapExpUtilities;
    }

    /**
     * This is the main method, that builds the utility components for TAP-TAP and MGRA-TAP. The arrays that are filled in are: tapExpUtilities[][][]
     * and mgraExpUtilities[][].
     * 
     * @param rb
     *            The ResourceBundle for this class.
     */
    public void calculateUtilityComponents()
    {

        logger.info("Calculating Walk-Transit Utilities");

        tapExpUtilities = new double[TAPPERIODS.length][maxTap + 1][maxTap + 1][Modes.TransitMode
                .values().length];

        wlkAccessExpUtilities = new double[maxMgra + 1][];
        drvAccessExpUtilities = new double[maxTaz + 1][];

        // first calculate the Tap-Tap utilities, exponentiate, and store
        logger.info("Calculating Tap-Tap utilities");

        int[] taps = tapManager.getTaps();

        for (int period = 0; period < TAPPERIODS.length; ++period)
        {

            logger.info("...Period " + TAPPERIODS[period]);
            for (int i = 1; i < taps.length; ++i)
            {
                int iTap = taps[i];

                if (iTap <= 10 || (iTap % 500) == 0) logger.info("......Origin TAP " + iTap);

                for (int j = 1; j < taps.length; ++j)
                {

                    int jTap = taps[j];

                    trace = false;
                    if (tracer.isTraceOn() && traceOTap[iTap] && traceDTap[jTap]) trace = true;

                    double[] results = walkUEC[period].calculateUtilitiesForTapPair(iTap, jTap,
                            trace);

                    for (int mode = 0; mode < results.length; ++mode)
                    {
                        if (results[mode] > -500)
                            tapExpUtilities[period][iTap][jTap][mode] = Math.exp(results[mode]);

                    }
                }
            }
        }

        // next calculate the walk utilities, exponentiate, and store
        logger.info("Calculating Mgra-Tap walk-access utilities");
        for (Integer mgra : mgraManager.getMgras())
        { // MGRA

            // skip mgras with no TAPs within walking distance
            if (mgraManager.getMgraWlkTapsDistArray()[mgra][0] == null) continue;

            int taz = mgraManager.getTaz(mgra);

            if (mgra <= 10 || (mgra % 500) == 0) logger.info("...Origin MGRA " + mgra);

            trace = false;
            if (tracer.isTraceOn() && tracer.isTraceZone(taz)) trace = true;

            int tapsInMgra = mgraManager.getMgraWlkTapsDistArray()[mgra][0].length;
            wlkAccessExpUtilities[mgra] = new double[tapsInMgra];

            for (int tapPosition = 0; tapPosition < tapsInMgra; ++tapPosition)
            {
                double result = walkUEC[0].calculateWalkAccessUtilityForMgra(mgra, tapPosition,
                        trace);
                wlkAccessExpUtilities[mgra][tapPosition] = Math.exp(result);
            }

        }

        // next calculate the drive-access utilities, exponentiate, and store
        logger.info("Calculating Taz-Tap drive-access utilities");
        for (int taz : tazManager.tazs)
        { // TAZ

            int[] drvTaps = tazManager
                    .getParkRideOrKissRideTapsForZone(taz, AccessMode.PARK_N_RIDE);
            if (drvTaps == null) continue;

            if (drvTaps.length == 0) continue;

            int tapsInTaz = drvTaps.length;
            drvAccessExpUtilities[taz] = new double[tapsInTaz];

            if (taz <= 10 || (taz % 500) == 0) logger.info("...Origin TAZ " + taz);

            trace = false;
            if (tracer.isTraceOn() && tracer.isTraceZone(taz)) trace = true;

            for (int tapPosition = 0; tapPosition < drvTaps.length; ++tapPosition)
            {

                double result = driveUEC[0].calculateDriveAccessUtilityForTaz(taz, tapPosition,
                        AccessMode.PARK_N_RIDE, trace);
                drvAccessExpUtilities[taz][tapPosition] = Math.exp(result);

            }

        }

    }

    /**
     * Calculate the full mgra-mgra utilities.
     * 
     * 
     * public void calculateFullMgraUtilities(){
     * 
     * logger.info("Calculating Full MGRA-MGRA exponentiated utilities"); double[][] expUtilities = new
     * double[WTPERIODS.length][Modes.TransitModes.values().length];
     * 
     * for(int period=0;period<WTPERIODS.length;++period){
     * 
     * logger.info("...Period "+WTPERIODS[period]);
     * 
     * //LOOP OVER ORIGIN MGRA int originMgras = 0; for(Integer iMgra : mgraManager.mgras){ //Origin MGRA
     * 
     * ++originMgras; if(originMgras<=10 || (originMgras % 500) ==0 ) logger.info("...Origin MGRA "+iMgra);
     * 
     * //skip mgras with no TAPs within walking distance if(mgraManager.mgraWlkTapsDistArray[iMgra][0] == null) continue;
     * 
     * //LOOP OVER DESTINATION MGRA for(Integer jMgra : mgraManager.mgras){ //Destination MGRA
     * 
     * //skip mgras with no TAPs within walking distance if(mgraManager.mgraWlkTapsDistArray[jMgra][0] == null) continue;
     * 
     * walkUEC[period].clearArrays();
     * 
     * expUtilities[period] = getWalkTransitExpUtilities(iMgra, jMgra,period);
     * 
     * if(trace) walkUEC[period].logBestUtilities(logger);
     * 
     * } } }
     * 
     * }
     */

    /**
     * Calculate the utilities for all tap-pairs for this mgra-pair, and return the best utilities by mode.
     * 
     * @param oMgra
     *            Origin/Production MGRA
     * @param dMgra
     *            Destination/Attraction MGRA
     * @param period
     *            The period
     * @return An array of exponentiated utilities, by mode.
     */
    public double[] calculateWalkTransitExpUtilities(int oMgra, int dMgra, int period)
    {

        double[] expUtilities = new double[Modes.TransitMode.values().length];
        walkUEC[period].clearArrays(0);

        // skip mgras with no TAPs within walking distance
        if (mgraManager.getMgraWlkTapsDistArray()[oMgra][0] == null
                || mgraManager.getMgraWlkTapsDistArray()[dMgra][0] == null) return expUtilities;

        int iTaz = mgraManager.getTaz(oMgra);
        int jTaz = mgraManager.getTaz(dMgra);
        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(iTaz, jTaz))
        {
            logger.info("");
            logger.info("Walk-Transit Mgra-Mgra calculations for " + oMgra + " to " + dMgra
                    + " period " + period);
            logger.info("");
            logger.info("iTap,jTap,mode,tapExpUtility,mgraTapExpUtility,tapMgraExpUtility,totalExpUtility");
            writeCalculations = true;
        }
        int iPos = -1;
        for (int iTap : mgraManager.getMgraWlkTapsDistArray()[oMgra][0])
        {
            iPos++; // used to know where we are in time/dist arrays for taps

            int jPos = -1;
            for (int jTap : mgraManager.getMgraWlkTapsDistArray()[dMgra][0])
            {
                jPos++;

                for (int mode = 0; mode < Modes.TransitMode.values().length; ++mode)
                {
                    expUtilities[mode] = tapExpUtilities[period][iTap][jTap][mode]
                            * wlkAccessExpUtilities[oMgra][iPos]
                            * wlkAccessExpUtilities[dMgra][jPos];

                    if (writeCalculations)
                    {
                        logger.info(iTap + "," + jTap + "," + mode + ","
                                + tapExpUtilities[period][iTap][jTap][mode] + ","
                                + wlkAccessExpUtilities[oMgra][iPos] + ","
                                + wlkAccessExpUtilities[dMgra][jPos] + "," + expUtilities[mode]);
                    }
                }

                // the uec object stores the best utilities for this interchange.
                walkUEC[period].comparePaths(expUtilities, iTap, jTap, writeCalculations);

            } // end jTap

        } // end iTap

        double[] bestUtilities = walkUEC[period].getBestUtilities();
        if (writeCalculations)
        {
            logger.info("");
            logger.info("Best Utilities for mgra pair " + oMgra + " to " + dMgra + " period "
                    + period);
            logger.info("ModeNumber, Mode, ExpUtility, bestITap, bestJTap");
            Modes.TransitMode[] mode = Modes.TransitMode.values();
            for (int i = 0; i < bestUtilities.length; ++i)
            {
                int[] bestTaps = walkUEC[period].getBestTaps(mode[i]);
                logger.info(i + "," + mode[i] + "," + bestUtilities[i] + "," + bestTaps[0] + ","
                        + bestTaps[1]);
            }
        }
        return bestUtilities;
    }

    /**
     * Calculate the utilities for all tap-pairs for this mgra-pair, and return the best utilities by mode.
     * 
     * @param oTaz
     *            Origin/Production TAZ
     * @param dMgra
     *            Destination/Attraction MGRA
     * @param period
     *            The period
     * @return An array of exponentiated utilities, by mode.
     */
    public double[] calculateDriveTransitExpUtilities(int oTaz, int dMgra, int period)
    {

        double[] expUtilities = new double[Modes.TransitMode.values().length];
        driveUEC[period].clearArrays(0);

        AccessMode accMode = AccessMode.PARK_N_RIDE;

        // skip Tazs with no TAPs within driving distance on origin or within walking
        // distance on destination end
        if (tazManager.getParkRideOrKissRideTapsForZone(oTaz, accMode) == null
                || mgraManager.getMgraWlkTapsDistArray()[dMgra][0] == null) return expUtilities;

        int dTaz = mgraManager.getTaz(dMgra);

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(oTaz, dTaz))
        {
            writeCalculations = true;
        }

        trace = false;
        if (writeCalculations)
        {
            logger.info("");
            logger.info("Drive-Transit Taz-Mgra calculations for " + oTaz + " to " + dMgra
                    + " period " + period);
            logger.info("");
            logger.info("iTap,jTap,mode,tapExpUtility,tazTapExpUtility,tapMgraExpUtility,totalExpUtility");
            trace = true;
        }

        int iPos = -1;
        for (int iTap : tazManager.getParkRideOrKissRideTapsForZone(oTaz, accMode))
        {
            iPos++; // used to know where we are in time/dist arrays for taps

            int jPos = -1;
            for (int jTap : mgraManager.getMgraWlkTapsDistArray()[dMgra][0])
            {
                jPos++;

                for (int mode = 0; mode < Modes.TransitMode.values().length; ++mode)
                {
                    expUtilities[mode] = tapExpUtilities[period][iTap][jTap][mode]
                            * drvAccessExpUtilities[oTaz][iPos]
                            * wlkAccessExpUtilities[dMgra][jPos];

                    if (trace)
                    {
                        logger.info(iTap + "," + jTap + "," + mode + ","
                                + tapExpUtilities[period][iTap][jTap][mode] + ","
                                + drvAccessExpUtilities[oTaz][iPos] + ","
                                + wlkAccessExpUtilities[dMgra][jPos] + "," + expUtilities[mode]);
                    }
                }

                // the uec object stores the best utilities for this interchange.
                driveUEC[period].comparePaths(expUtilities, iTap, jTap, writeCalculations);

            } // end jTap

        } // end iTap

        double[] bestUtilities = driveUEC[period].getBestUtilities();
        if (trace)
        {
            logger.info("");
            logger.info("Best Utilities for mgra pair " + oTaz + " to " + dMgra + " period "
                    + period);
            logger.info("ModeNumber, Mode, ExpUtility, bestITap, bestJTap");
            Modes.TransitMode[] mode = Modes.TransitMode.values();
            for (int i = 0; i < bestUtilities.length; ++i)
            {
                int[] bestTaps = driveUEC[period].getBestTaps(mode[i]);
                logger.info(i + "," + mode[i] + "," + bestUtilities[i] + "," + bestTaps[0] + ","
                        + bestTaps[1]);
            }
        }

        return bestUtilities;
    }

    /**
     * Get the best walk taps for the given transit mode and period.
     * 
     * @param transitMode
     *            The transit mode to look up.
     * @param period
     *            0 For off-peak, 1 for peak
     * @return An array where element 0 = best pTap, and element 1 = best aTap.
     */
    public int[] getBestWalkTaps(Modes.TransitMode transitMode, int period)
    {

        return walkUEC[period].getBestTaps(transitMode);

    }

    /**
     * Get the best taps for the given period.
     * 
     * @param period
     *            0 For off-peak, 1 for peak
     * @return An array where element 0 = best pTap, and element 1 = best aTap.
     */
    public int[] getBestWalkTaps(int period)
    {

        Modes.TransitMode bestMode = getBestWalkTransitMode(period);
        return walkUEC[period].getBestTaps(bestMode);

    }

    /**
     * Get the best walk taps for the given transit mode and period.
     * 
     * @param transitMode
     *            The transit mode to look up.
     * @param period
     *            0 For off-peak, 1 for peak
     * @return An array where element 0 = best pTap, and element 1 = best aTap.
     */
    public int[] getBestDriveTaps(Modes.TransitMode transitMode, int period)
    {

        return driveUEC[period].getBestTaps(transitMode);

    }

    /**
     * Get the best taps for the given period.
     * 
     * @param period
     *            0 For off-peak, 1 for peak
     * @return An array where element 0 = best pTap, and element 1 = best aTap.
     */
    public int[] getBestDriveTaps(int period)
    {

        Modes.TransitMode bestMode = getBestDriveTransitMode(period);
        return driveUEC[period].getBestTaps(bestMode);

    }

    public double[] getBestWalkUtilities(int period)
    {
        return walkUEC[period].getBestUtilities();
    }

    public double[] getBestDriveUtilities(int period)
    {
        return driveUEC[period].getBestUtilities();
    }

    /**
     * Get the best transit mode for a given period. Returns null if no transit mode has an exponentiated utility. Call only after calling
     * getWalkTransitExpUtilities().
     * 
     * @param period
     *            0 For off-peak, 1 for peak
     * @return The best transit mode (highest exponentiated utility)
     */
    public Modes.TransitMode getBestWalkTransitMode(int period)
    {

        Modes.TransitMode[] tm = Modes.TransitMode.values();

        int bestMode = -1;
        double bestUtility = 0;
        double[] bestUtilities = walkUEC[period].getBestUtilities();
        for (int i = 0; i < bestUtilities.length; ++i)
            if (bestUtilities[i] > bestUtility)
            {
                bestMode = i;
                bestUtility = bestUtilities[i];
            }
        if (bestMode > -1) return tm[bestMode];

        return null;
    }

    /**
     * Get the best drive transit mode for a given period. Returns null if no transit mode has an exponentiated utility. Call only after calling
     * getWalkTransitExpUtilities().
     * 
     * @param period
     *            0 For off-peak, 1 for peak
     * @return The best transit mode (highest exponentiated utility)
     */
    public Modes.TransitMode getBestDriveTransitMode(int period)
    {

        Modes.TransitMode[] tm = Modes.TransitMode.values();

        int bestMode = -1;
        double bestUtility = 0;
        double[] bestUtilities = driveUEC[period].getBestUtilities();
        for (int i = 0; i < bestUtilities.length; ++i)
            if (bestUtilities[i] > bestUtility)
            {
                bestMode = i;
                bestUtility = bestUtilities[i];
            }
        if (bestMode > -1) return tm[bestMode];

        return null;
    }

    /**
     * The main method runs this class, for testing purposes.
     * 
     * @param args
     *            args[0] is the property file for this test run.
     */
    public static void main(String[] args)
    {

        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0]));
        boolean os64bit = false;
        MatrixDataServer matrixServer = null;

        os64bit = Boolean.parseBoolean(rb.getString("operatingsystem.64bit"));
        if (os64bit)
        {

            String serverAddress = rb.getString("server.address");

            int serverPort = new Integer(rb.getString("server.port"));
            String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

            matrixServer = new MatrixDataServer();

            try
            {

                // create the concrete data server object
                matrixServer.start32BitMatrixIoServer(MatrixType.TRANSCAD);
            } catch (RuntimeException e)
            {
                matrixServer.stop32BitMatrixIoServer();
                System.out
                        .println("RuntimeException caught in com.pb.sandag.accessibilities.main() -- exiting.");
                e.printStackTrace();
            }

            // bind this concrete object with the cajo library objects for managing
            // RMI
            try
            {
                Remote.config(serverAddress, serverPort, null, 0);
            } catch (Exception e)
            {
                System.out.println(String.format(
                        "UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.",
                        serverAddress, serverPort));
                e.printStackTrace();
                matrixServer.stop32BitMatrixIoServer();
                throw new RuntimeException();
            }

            try
            {
                ItemServer.bind(matrixServer, className);
            } catch (RemoteException e)
            {
                System.out.println(String.format(
                        "RemoteException. serverAddress = %s, serverPort = %d -- exiting.",
                        serverAddress, serverPort));
                e.printStackTrace();
                matrixServer.stop32BitMatrixIoServer();
                throw new RuntimeException();
            }
        }

        TransitUtilities wtu = new TransitUtilities(
                ResourceUtil.changeResourceBundleIntoHashMap(rb));
        wtu.calculateUtilityComponents();

        if (os64bit)
        {
            matrixServer.stop32BitMatrixIoServer();
        }

    }
}
