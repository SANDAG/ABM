package org.sandag.abm.accessibilities;

import com.pb.common.util.Tracer;
import com.pb.common.calculator.IndexValues;
import org.sandag.abm.ctramp.Util;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * This class builds accessibility components for all modes.
 * 
 * @author Joel Freedman
 * @version May, 2009
 */
public class MandatoryAccessibilitiesCalculator
        implements Serializable
{

    protected transient Logger          logger                         = Logger.getLogger(MandatoryAccessibilitiesCalculator.class);

    private static final int            MIN_EXP_FUNCTION_ARGUMENT      = -500;

    private static final int            PEAK_NONTOLL_SOV_TIME_INDEX    = 0;
    private static final int            PEAK_NONTOLL_SOV_DIST_INDEX    = 1;
    private static final int            OFFPEAK_NONTOLL_SOV_TIME_INDEX = 2;
    private static final int            OFFPEAK_NONTOLL_SOV_DIST_INDEX = 3;

    private UtilityExpressionCalculator autoSkimUEC;
    private UtilityExpressionCalculator bestWalkTransitUEC;
    private UtilityExpressionCalculator bestDriveTransitUEC;
    private UtilityExpressionCalculator autoLogsumUEC;
    private UtilityExpressionCalculator transitLogsumUEC;

    private MandatoryAccessibilitiesDMU dmu;
    private IndexValues                 iv;

    private NonTransitUtilities         ntUtilities;
    // private TransitUtilities transitUtilities;

    private MgraDataManager             mgraManager;
    private TazDataManager              tazManager;
    private TapDataManager              tapManager;

    // auto sufficiency (0 autos, autos<adults, autos>=adults),
    // and mode (SOV,HOV,Walk-Transit,Non-Motorized)
    private double[][]                  expConstants;

    private String[]                    accNames                       = {"SovTime", // 0
            "SovDist", // 1
            "WTTime", // 2
            "DTTime", // 3
            "SovUtility", // 4
            "WTUtility", // 5
            "AutoLogsum", // 6
            "WTLogsum", // 7
            "TransitLogsum", // 8
            "WTRailShare", // 9
            "DTRailShare", // 10
            "DTLogsum", // 11
            "HovUtility" // 12
                                                                       };

    private BestTransitPathCalculator   bestPathCalculator;

    public MandatoryAccessibilitiesCalculator(HashMap<String, String> rbMap,
            NonTransitUtilities aNtUtilities, double[][] aExpConstants,
            BestTransitPathCalculator myBestPathCalculator)
    {

        ntUtilities = aNtUtilities;
        // transitUtilities = aTransitUtilities;
        expConstants = aExpConstants;

        // Create the UECs
        String uecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.mandatory.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.mandatory.data.page");
        int autoSkimPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.mandatory.auto.page");
        int bestWalkTransitPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.bestWalkTransit.page");
        int bestDriveTransitPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.bestDriveTransit.page");
        int autoLogsumPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.autoLogsum.page");
        int transitLogsumPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.transitLogsum.page");

        dmu = new MandatoryAccessibilitiesDMU();

        File uecFile = new File(uecFileName);
        autoSkimUEC = new UtilityExpressionCalculator(uecFile, autoSkimPage, dataPage, rbMap, dmu);
        bestWalkTransitUEC = new UtilityExpressionCalculator(uecFile, bestWalkTransitPage,
                dataPage, rbMap, dmu);
        bestDriveTransitUEC = new UtilityExpressionCalculator(uecFile, bestDriveTransitPage,
                dataPage, rbMap, dmu);
        autoLogsumUEC = new UtilityExpressionCalculator(uecFile, autoLogsumPage, dataPage, rbMap,
                dmu);
        transitLogsumUEC = new UtilityExpressionCalculator(uecFile, transitLogsumPage, dataPage,
                rbMap, dmu);

        iv = new IndexValues();

        tazManager = TazDataManager.getInstance();
        tapManager = TapDataManager.getInstance();
        mgraManager = MgraDataManager.getInstance();

        bestPathCalculator = myBestPathCalculator;
    }

    public double[] calculateWorkerMandatoryAccessibilities(int hhMgra, int workMgra)
    {
        return calculateAccessibilitiesForMgraPair(hhMgra, workMgra, false, null);
    }

    public double[] calculateStudentMandatoryAccessibilities(int hhMgra, int schoolMgra)
    {
        return calculateAccessibilitiesForMgraPair(hhMgra, schoolMgra, false, null);
    }

    /**
     * Calculate the work logsum for the household MGRA and sampled work
     * location MGRA.
     * 
     * @param hhMgra
     *            Household MGRA
     * @param workMgra
     *            Sampled work MGRA
     * @param autoSufficiency
     *            Auto sufficiency category
     * @return Work mode choice logsum
     */
    public double calculateWorkLogsum(int hhMgra, int workMgra, int autoSufficiency, boolean debug,
            Logger aLogger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
            aLogger.info("");
            aLogger.info("");
            header = "calculateWorkLogsum() debug info for homeMgra=" + hhMgra + ", workMgra="
                    + workMgra;
            for (int i = 0; i < header.length(); i++)
                separator += "^";
        }

        double[] accessibilities = calculateAccessibilitiesForMgraPair(hhMgra, workMgra, debug,
                aLogger);

        double sovUtility = accessibilities[4];
        double hovUtility = accessibilities[12];
        double transitLogsum = accessibilities[8]; // includes both walk and
                                                   // drive access
        double nmExpUtility = ntUtilities.getNMotorExpUtility(hhMgra, workMgra,
                NonTransitUtilities.OFFPEAK_PERIOD_INDEX);

        // constrain auto sufficiency to 0,1,2
        autoSufficiency = Math.min(autoSufficiency, 2);

        double utilSum = Math.exp(sovUtility) * expConstants[autoSufficiency][0]
                + Math.exp(hovUtility) * expConstants[autoSufficiency][1] + Math.exp(transitLogsum)
                * expConstants[autoSufficiency][2] + nmExpUtility
                * expConstants[autoSufficiency][3];

        double logsum = Math.log(utilSum);

        if (debug)
        {

            aLogger.info(separator);
            aLogger.info(header);
            aLogger.info(separator);

            aLogger.info("accessibilities array values");
            aLogger.info(String.format("%5s %15s %15s", "i", "accName", "value"));
            aLogger.info(String.format("%5s %15s %15s", "-----", "----------", "----------"));
            for (int i = 0; i < accessibilities.length; i++)
            {
                aLogger.info(String.format("%5d %15s %15.5e", i, accNames[i], accessibilities[i]));
            }

            aLogger.info("");
            aLogger.info("");
            aLogger.info("logsum component values");
            aLogger.info(String.format("autoSufficiency = %d", autoSufficiency));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                    "sovUtility", sovUtility, "exp(sovUtility)", Math.exp(sovUtility),
                    String.format("expConst suff=%d 0", autoSufficiency),
                    expConstants[autoSufficiency][0]));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                    "hovUtility", hovUtility, "exp(hovUtility)", Math.exp(hovUtility),
                    String.format("expConst suff=%d 1", autoSufficiency),
                    expConstants[autoSufficiency][1]));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                    "transitLogsum", transitLogsum, "exp(transitLogsum)", Math.exp(transitLogsum),
                    String.format("expConst suff=%d 2", autoSufficiency),
                    expConstants[autoSufficiency][2]));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e", "nmExpUtility",
                    nmExpUtility, String.format("expConst suff=%d 3", autoSufficiency),
                    expConstants[autoSufficiency][3]));
            aLogger.info(String.format("%-15s = %15.5e", "utilSum", utilSum));
            aLogger.info(String.format("%-15s = %15.5e", "logsum", logsum));
            aLogger.info(separator);
        }

        return logsum;
    }

    /**
     * Calculate the school logsum for the household MGRA and sampled school
     * location MGRA.
     * 
     * @param hhMgra
     *            Household MGRA
     * @param schoolMgra
     *            Sampled work MGRA
     * @param autoSufficiency
     *            Auto sufficiency category
     * @param studentType
     *            Student type 0=Pre-school (SOV not available) 1=K-8 (SOV not
     *            available) 2=9-12 (Normal car-sufficiency-based logsum)
     *            3=College/university(typical) (Normal car-sufficiency-based
     *            logsum) 4=College/university(non-typical) (Normal
     *            car-sufficiency-based logsum)
     * @return School mode choice logsum
     */
    public double calculateSchoolLogsum(int hhMgra, int schoolMgra, int autoSufficiency,
            int studentType, boolean debug, Logger aLogger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
            aLogger.info("");
            aLogger.info("");
            header = "calculateSchoolLogsum() debug info for homeMgra=" + hhMgra + ", schoolMgra="
                    + schoolMgra;
            for (int i = 0; i < header.length(); i++)
                separator += "^";
        }

        double[] accessibilities = calculateAccessibilitiesForMgraPair(hhMgra, schoolMgra, debug,
                aLogger);

        double sovUtility = accessibilities[4];
        double hovUtility = accessibilities[12];
        double transitLogsum = accessibilities[8]; // includes both walk and
                                                   // drive access
        double nmExpUtility = ntUtilities.getNMotorExpUtility(hhMgra, schoolMgra,
                NonTransitUtilities.OFFPEAK_PERIOD_INDEX);

        // constrain auto sufficiency to 0,1,2
        autoSufficiency = Math.min(autoSufficiency, 2);

        double logsum = Math.exp(hovUtility) * expConstants[autoSufficiency][1]
                + Math.exp(transitLogsum) * expConstants[autoSufficiency][2] + nmExpUtility
                * expConstants[autoSufficiency][3];

        // used for debugging
        double logsum1 = logsum;

        if (studentType >= 2)
        {
            logsum = logsum + Math.exp(sovUtility) * expConstants[autoSufficiency][0];
        }

        // used for debugging
        double logsum2 = logsum;

        logsum = Math.log(logsum);

        if (debug)
        {

            aLogger.info(separator);
            aLogger.info(header);
            aLogger.info(separator);

            aLogger.info("accessibilities array values");
            aLogger.info(String.format("%5s %15s %15s", "i", "accName", "value"));
            aLogger.info(String.format("%5s %15s %15s", "-----", "----------", "----------"));
            for (int i = 0; i < accessibilities.length; i++)
            {
                aLogger.info(String.format("%5d %15s %15.5e", i, accNames[i], accessibilities[i]));
            }

            aLogger.info("");
            aLogger.info("");
            aLogger.info("logsum component values");
            aLogger.info(String.format("autoSufficiency = %d", autoSufficiency));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                    "hovUtility", hovUtility, "exp(hovUtility)", Math.exp(hovUtility),
                    String.format("expConst suff=%d 1", autoSufficiency),
                    expConstants[autoSufficiency][1]));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                    "transitLogsum", transitLogsum, "exp(transitLogsum)", Math.exp(transitLogsum),
                    String.format("expConst suff=%d 2", autoSufficiency),
                    expConstants[autoSufficiency][2]));
            aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e", "nmExpUtility",
                    nmExpUtility, String.format("expConst suff=%d 3", autoSufficiency),
                    expConstants[autoSufficiency][3]));
            aLogger.info(String.format("%s = %15.5e", "utility sum (before adding sovUtility)",
                    logsum1));
            if (studentType >= 2)
            {
                aLogger.info(String.format("studentType = %d", studentType));
                aLogger.info(String.format("%-15s = %15.5e, %-18s = %15.5e, %-18s = %15.5e",
                        "sovUtility", sovUtility, "exp(sovUtility)", Math.exp(sovUtility),
                        String.format("expConst suff=%d 0", autoSufficiency),
                        expConstants[autoSufficiency][0]));
                aLogger.info(String.format("%s = %15.5e", "utility sum (after adding sovUtility)",
                        logsum2));
            } else
            {
                aLogger.info(String.format(
                        "studentType = %d, no additional contribution to utility sum", studentType));
            }
            aLogger.info(String.format("%s = %15.5e, %s = %15.5e", "final utility sum", logsum2,
                    "final logsum", logsum));
            aLogger.info(separator);
        }

        return logsum;
    }

    /**
     * Calculate the accessibilities for a given origin and destination mgra
     * 
     * @param oMgra
     *            The origin mgra
     * @param dMgra
     *            The destination mgra
     * @return An array of accessibilities
     */
    public double[] calculateAccessibilitiesForMgraPair(int oMgra, int dMgra, boolean debug,
            Logger aLogger)
    {

        double[] accessibilities = new double[accNames.length];

        if (oMgra > 0 && dMgra > 0)
        {

            int oTaz = mgraManager.getTaz(oMgra);
            int dTaz = mgraManager.getTaz(dMgra);

            iv.setOriginZone(oTaz);
            iv.setDestZone(dTaz);

            // sov time and distance
            double[] autoResults = autoSkimUEC.solve(iv, dmu, null);
            if (debug)
                autoSkimUEC.logAnswersArray(aLogger,
                        String.format("autoSkimUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));

            // autoResults[0] is peak non-toll sov time, autoResults[1] is peak
            // non-toll sov dist
            // autoResults[2] is off-peak non-toll sov time, autoResults[3] is
            // off-peak non-toll sov dist
            accessibilities[0] = autoResults[PEAK_NONTOLL_SOV_TIME_INDEX];
            accessibilities[1] = autoResults[PEAK_NONTOLL_SOV_DIST_INDEX];

            // pre-calculate the hov, sov, and non-motorized exponentiated
            // utilities for the origin MGRA.
            // the method called returns cached values if they were already
            // calculated.
            ntUtilities.buildUtilitiesForOrigMgraAndPeriod(oMgra,
                    NonTransitUtilities.PEAK_PERIOD_INDEX);

            // auto logsum
            double pkSovExpUtility = ntUtilities.getSovExpUtility(oTaz, dTaz,
                    NonTransitUtilities.PEAK_PERIOD_INDEX);
            double pkHovExpUtility = ntUtilities.getHovExpUtility(oTaz, dTaz,
                    NonTransitUtilities.PEAK_PERIOD_INDEX);

            dmu.setSovNestLogsum(-999);
            if (pkSovExpUtility > 0)
            {
                dmu.setSovNestLogsum(Math.log(pkSovExpUtility));
                accessibilities[4] = dmu.getSovNestLogsum();
            }
            dmu.setHovNestLogsum(-999);
            if (pkHovExpUtility > 0)
            {
                dmu.setHovNestLogsum(Math.log(pkHovExpUtility));
                accessibilities[12] = dmu.getHovNestLogsum();
            }

            double[] autoLogsum = autoLogsumUEC.solve(iv, dmu, null);
            if (debug)
                autoLogsumUEC.logAnswersArray(aLogger,
                        String.format("autoLogsumUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));
            accessibilities[6] = autoLogsum[0];

            // walk transit

            // determine the best transit path, which also stores the best
            // utilities array and the best mode
            bestPathCalculator.findBestWalkTransitWalkTaps(TransitWalkAccessUEC.AM, oMgra, dMgra,
                    debug, aLogger);

            // sum the exponentiated utilities over modes
            double sumWlkExpUtilities = 0;
            double[] walkTransitWalkUtilities = bestPathCalculator.getBestUtilities();
            for (int i = 0; i < walkTransitWalkUtilities.length; i++)
            {
                if (walkTransitWalkUtilities[i] > MIN_EXP_FUNCTION_ARGUMENT)
                    sumWlkExpUtilities += Math.exp(walkTransitWalkUtilities[i]);
            }

            // calculate ln( sum of exponentiated utilities ) and set in
            // accessibilities array and the dmu object
            if (sumWlkExpUtilities > 0) accessibilities[7] = Math.log(sumWlkExpUtilities);
            else accessibilities[7] = -999;

            dmu.setWlkNestLogsum(accessibilities[7]);

            Modes.TransitMode bestMode = bestPathCalculator.getBestTransitMode();

            if (bestMode != null)
            {
                int[] bestTaps = bestPathCalculator.getBestTaps(bestMode);
                dmu.setBestMode(bestMode.ordinal());
                int oTapPosition = mgraManager.getTapPosition(oMgra, bestTaps[0]);
                int dTapPosition = mgraManager.getTapPosition(dMgra, bestTaps[1]);

                if (oTapPosition == -1 || dTapPosition == -1)
                {
                    logger.fatal("Error:  Best walk mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but oTap pos "
                            + oTapPosition + " and dTap pos " + dTapPosition);
                    throw new RuntimeException();
                }

                if (walkTransitWalkUtilities[bestMode.ordinal()] <= MIN_EXP_FUNCTION_ARGUMENT)
                {
                    logger.fatal("Error:  Best walk mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but Utility = "
                            + walkTransitWalkUtilities[bestMode.ordinal()]);
                    throw new RuntimeException();
                }
                accessibilities[5] = Math.log(walkTransitWalkUtilities[bestMode.ordinal()]);

                dmu.setMgraTapWalkTime((float) bestPathCalculator.getBestAccessTime(bestMode
                        .ordinal()));
                dmu.setTapMgraWalkTime((float) bestPathCalculator.getBestEgressTime(bestMode
                        .ordinal()));
                iv.setOriginZone(bestTaps[0]);
                iv.setDestZone(bestTaps[1]);
                double[] wlkTransitTimes = bestWalkTransitUEC.solve(iv, dmu, null);

                if (debug)
                {
                    bestWalkTransitUEC.logAnswersArray(aLogger,
                            String.format("bestWalkTransitUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));
                }

                accessibilities[2] = wlkTransitTimes[0];
                accessibilities[9] = wlkTransitTimes[1];

            }

            /*
             * // walk transit
             * 
             * // calculate the exp utilities, which will also calculate and
             * store best mode double[] walkTransitExpUtilities =
             * transitUtilities.calculateWalkTransitExpUtilities( oMgra, dMgra,
             * 1 );
             * 
             * // add up the exp utilities dmu.setWlkNestLogsum(-999f); double
             * sumWlkExpUtilities = 0; for (int i = 0; i <
             * walkTransitExpUtilities.length; ++i) sumWlkExpUtilities +=
             * walkTransitExpUtilities[i];
             * 
             * if (sumWlkExpUtilities > 0)
             * dmu.setWlkNestLogsum(Math.log(sumWlkExpUtilities));
             * 
             * accessibilities[7] = dmu.getWlkNestLogsum();
             * 
             * Modes.TransitMode bestMode =
             * transitUtilities.getBestWalkTransitMode(1);
             * 
             * if (bestMode != null) { int[] bestTaps =
             * transitUtilities.getBestWalkTaps(bestMode, 1);
             * dmu.setBestMode(bestMode.ordinal()); int oTapPosition =
             * mgraManager.getTapPosition(oMgra, bestTaps[0]); int dTapPosition
             * = mgraManager.getTapPosition(dMgra, bestTaps[1]);
             * 
             * if (oTapPosition == -1 || dTapPosition == -1) {
             * logger.fatal("Error:  Best walk mode " + bestMode +
             * " found for origin mgra " + oMgra + " to destination mgra " +
             * dMgra + " but oTap pos " + oTapPosition + " and dTap pos " +
             * dTapPosition); throw new RuntimeException(); }
             * 
             * if (walkTransitExpUtilities[bestMode.ordinal()] <= 0.0) {
             * logger.fatal("Error:  Best walk mode " + bestMode +
             * " found for origin mgra " + oMgra + " to destination mgra " +
             * dMgra + " but exp Utility = " +
             * walkTransitExpUtilities[bestMode.ordinal()]); throw new
             * RuntimeException(); } accessibilities[5] =
             * Math.log(walkTransitExpUtilities[bestMode.ordinal()]);
             * 
             * dmu.setMgraTapWalkTime(mgraManager.getMgraToTapWalkTime(oMgra,
             * oTapPosition));
             * dmu.setTapMgraWalkTime(mgraManager.getMgraToTapWalkTime(dMgra,
             * dTapPosition)); iv.setOriginZone(bestTaps[0]);
             * iv.setDestZone(bestTaps[1]); double[] wlkTransitTimes =
             * bestWalkTransitUEC.solve(iv, dmu, null); if (debug)
             * bestWalkTransitUEC.logAnswersArray(aLogger, String.format(
             * "bestWalkTransitUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));
             * accessibilities[2] = wlkTransitTimes[0]; accessibilities[9] =
             * wlkTransitTimes[1];
             * 
             * }
             */

            // drive transit

            // determine the best transit path, which also stores the best
            // utilities array and the best mode
            bestPathCalculator.findBestDriveTransitWalkTaps(TransitWalkAccessUEC.AM, oMgra, dMgra,
                    debug, aLogger);

            // sum the exponentiated utilities over modes
            double sumDrvExpUtilities = 0;
            double[] driveTransitWalkUtilities = bestPathCalculator.getBestUtilities();
            for (int i = 0; i < driveTransitWalkUtilities.length; i++)
            {
                if (driveTransitWalkUtilities[i] > MIN_EXP_FUNCTION_ARGUMENT)
                    sumDrvExpUtilities += Math.exp(driveTransitWalkUtilities[i]);
            }

            // calculate ln( sum of exponentiated utilities ) and set in
            // accessibilities array and the dmu object
            if (sumDrvExpUtilities > 0) accessibilities[11] = Math.log(sumDrvExpUtilities);
            else accessibilities[11] = -999;

            dmu.setDrvNestLogsum(accessibilities[11]);

            bestMode = bestPathCalculator.getBestTransitMode();

            if (bestMode != null)
            {
                int[] bestTaps = bestPathCalculator.getBestTaps(bestMode);
                dmu.setBestMode(bestMode.ordinal());
                int oTapPosition = tazManager.getTapPosition(oTaz, bestTaps[0],
                        Modes.AccessMode.PARK_N_RIDE);
                int dTapPosition = mgraManager.getTapPosition(dMgra, bestTaps[1]);

                if (oTapPosition == -1 || dTapPosition == -1)
                {
                    logger.fatal("Error:  Best drive mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but oTap pos "
                            + oTapPosition + " and dTap pos " + dTapPosition);
                    throw new RuntimeException();
                }

                if (driveTransitWalkUtilities[bestMode.ordinal()] <= MIN_EXP_FUNCTION_ARGUMENT)
                {
                    logger.fatal("Error:  Best drive mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but Utility = "
                            + driveTransitWalkUtilities[bestMode.ordinal()]);
                    throw new RuntimeException();
                }

                dmu.setDriveTimeToTap((float) bestPathCalculator.getBestAccessTime(bestMode
                        .ordinal()));
                dmu.setDriveDistToTap(tazManager.getTapDist(oTaz, oTapPosition,
                        Modes.AccessMode.PARK_N_RIDE));
                dmu.setTapMgraWalkTime((float) bestPathCalculator.getBestEgressTime(bestMode
                        .ordinal()));

                iv.setOriginZone(bestTaps[0]);
                iv.setDestZone(bestTaps[1]);
                double[] drvTransitTimes = bestDriveTransitUEC.solve(iv, dmu, null);

                if (debug)
                {
                    bestDriveTransitUEC
                            .logAnswersArray(aLogger, String.format(
                                    "bestDriveTransitUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));
                }

                accessibilities[3] = drvTransitTimes[0];
                accessibilities[10] = drvTransitTimes[1];

            }

            /*
             * // drive transit
             * 
             * // calculate the exp utilities, which will also calculate and
             * store best // mode double[] driveTransitExpUtilities =
             * transitUtilities.calculateDriveTransitExpUtilities( oTaz, dMgra,
             * 1);
             * 
             * // add up the exp utilities dmu.setDrvNestLogsum(-999); double
             * sumDrvExpUtilities = 0; for (int i = 0; i <
             * driveTransitExpUtilities.length; ++i) sumDrvExpUtilities +=
             * driveTransitExpUtilities[i]; if (sumDrvExpUtilities > 0)
             * dmu.setDrvNestLogsum(Math.log(sumDrvExpUtilities));
             * 
             * accessibilities[11] = dmu.getDrvNestLogsum();
             * 
             * bestMode = transitUtilities.getBestDriveTransitMode(1);
             * 
             * if (bestMode != null) { int[] bestTaps =
             * transitUtilities.getBestDriveTaps(bestMode, 1);
             * dmu.setBestMode(bestMode.ordinal()); int oTapPosition =
             * tazManager.getTapPosition(oTaz, bestTaps[0],
             * Modes.AccessMode.PARK_N_RIDE); int dTapPosition =
             * mgraManager.getTapPosition(dMgra, bestTaps[1]);
             * 
             * if (oTapPosition == -1 || dTapPosition == -1) {
             * logger.fatal("Error:  Best drive mode " + bestMode +
             * " found for origin taz " + oTaz + " to destination mgra " + dMgra
             * + " but oTap pos " + oTapPosition + " and dTap pos " +
             * dTapPosition); throw new RuntimeException(); }
             * 
             * dmu.setDriveTimeToTap(tazManager.getTapTime(oTaz, oTapPosition,
             * Modes.AccessMode.PARK_N_RIDE));
             * dmu.setDriveDistToTap(tazManager.getTapDist(oTaz, oTapPosition,
             * Modes.AccessMode.PARK_N_RIDE));
             * 
             * dmu.setTapMgraWalkTime(mgraManager.getMgraToTapWalkTime(dMgra,
             * dTapPosition));
             * 
             * iv.setOriginZone(bestTaps[0]); iv.setDestZone(bestTaps[1]);
             * double[] drvTransitTimes = bestDriveTransitUEC.solve(iv, dmu,
             * null); if (debug) bestDriveTransitUEC.logAnswersArray(aLogger,
             * String.format( "bestDriveTransitUEC:  oMgra=%d, dMgra=%d", oMgra,
             * dMgra)); accessibilities[3] = drvTransitTimes[0];
             * accessibilities[10] = drvTransitTimes[1];
             * 
             * }
             */

            double[] transitLogsumResults = transitLogsumUEC.solve(iv, dmu, null);
            if (debug)
            {
                transitLogsumUEC.logAnswersArray(aLogger,
                        String.format("transitLogsumUEC:  oMgra=%d, dMgra=%d", oMgra, dMgra));
            }

            // transit logsum results array has only 1 alternative, so result is
            // in 0 element.
            accessibilities[8] = transitLogsumResults[0];

        } // end if oMgra and dMgra > 0

        return accessibilities;
    }

    /**
     * Calculate auto skims for a given origin to all destination mgras, and
     * return auto distance.
     * 
     * @param oMgra
     *            The origin mgra
     * @return An array of distances
     */
    public double[] calculateDistancesForAllMgras(int oMgra)
    {

        double[] distances = new double[mgraManager.getMaxMgra() + 1];

        int oTaz = mgraManager.getTaz(oMgra);
        iv.setOriginZone(oTaz);

        for (int i = 1; i <= mgraManager.getMaxMgra(); i++)
        {

            int dTaz = mgraManager.getTaz(i);
            iv.setDestZone(dTaz);

            // sov distance
            double[] autoResults = autoSkimUEC.solve(iv, dmu, null);
            distances[i] = autoResults[PEAK_NONTOLL_SOV_DIST_INDEX];

        }

        return distances;
    }

    /**
     * Calculate auto skims for a given origin to all destination mgras, and
     * return auto distance.
     * 
     * @param oMgra
     *            The origin mgra
     * @return An array of distances
     */
    public double[] calculateOffPeakDistancesForAllMgras(int oMgra)
    {

        double[] distances = new double[mgraManager.getMaxMgra() + 1];

        int oTaz = mgraManager.getTaz(oMgra);
        iv.setOriginZone(oTaz);

        for (int i = 1; i <= mgraManager.getMaxMgra(); i++)
        {

            int dTaz = mgraManager.getTaz(i);
            iv.setDestZone(dTaz);

            // sov distance
            double[] autoResults = autoSkimUEC.solve(iv, dmu, null);
            distances[i] = autoResults[OFFPEAK_NONTOLL_SOV_DIST_INDEX];

        }

        return distances;
    }

}
