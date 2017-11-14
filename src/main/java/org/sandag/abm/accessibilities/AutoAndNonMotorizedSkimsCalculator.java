package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * This class is used to return auto skim values and non-motorized skim values
 * for MGRA pairs associated with estimation data file records.
 * 
 * @author Jim Hicks
 * @version March, 2010
 */
public class AutoAndNonMotorizedSkimsCalculator
        implements Serializable
{

    private transient Logger              logger                 = Logger.getLogger(AutoAndNonMotorizedSkimsCalculator.class);

    private static final int              EA                            = ModelStructure.EA_SKIM_PERIOD_INDEX;
    private static final int              AM                            = ModelStructure.AM_SKIM_PERIOD_INDEX;
    private static final int              MD                            = ModelStructure.MD_SKIM_PERIOD_INDEX;
    private static final int              PM                            = ModelStructure.PM_SKIM_PERIOD_INDEX;
    private static final int              EV                            = ModelStructure.EV_SKIM_PERIOD_INDEX;
    public static final int              NUM_PERIODS                   = ModelStructure.SKIM_PERIOD_INDICES.length;
    private static final String[]         PERIODS                = ModelStructure.SKIM_PERIOD_STRINGS;

    // set the indices used for the non-motorized names array and the return
    // skims
    // array
    private static final int              WALK_INDEX             = 0;
    private static final int              BIKE_INDEX             = 1;

    private static final double           WALK_SPEED             = 3.0;                                                       // mph
    private static final double           BIKE_SPEED             = 12.0;                                                      // mph

    // declare an array of UEC objects, 1 for each time period
    private UtilityExpressionCalculator[] autoSkimUECs;
    private IndexValues                   iv;

    //  A simple DMU with no variables
    private VariableTable                 dmu                    = null;
    private AutoSkimsDMU				  autoSkimsDMU = null;
    
    private MgraDataManager               mgraManager;

    private static final String[]         AUTO_SKIM_NAMES        = {"DA_NT_Time", "DA_NT_FFTime",
            "DA_NT_Dist", "DA_TL_Time", "DA_TL_FFTime", "DA_TL_Dist", "DA_TL_Value",
            "DA_TL_FacLen", "HOV_NT_Time", "HOV_NT_FFTime", "HOV_NT_Dist", "HOV_NT_FacLen"};
    private static final int              NUM_AUTO_SKIMS         = AUTO_SKIM_NAMES.length;

    private static final String[]         AUTO_SKIM_DESCRIPTIONS = {"DA NonToll - Time", // 0
            "DA NonToll - Free Flow Time", // 1
            "DA NonToll - Distance", // 2
            "DA Toll - Time", // 3
            "DA Toll - Free Flow Time", // 4
            "DA Toll - Distance", // 5
            "DA Toll - Toll Value", // 6
            "DA Toll - Length on Toll Facility", // 7
            "S2 NonToll - Time", // 8
            "S2 NonToll - Free Flow Time", // 9
            "S2 NonToll - Distance", // 10
            "S2 NonToll - Length on HOV Facility", // 11
            "S2 Toll - Time", // 12
            "S2 Toll - Free Flow Time", // 13
            "S2 Toll - Distance", // 14
            "S2 Toll - Toll Value", // 15
            "S2 Toll - Length on HOV Facility", // 16
            "S3 NonToll - Time", // 17
            "S3 NonToll - Free Flow Time", // 18
            "S3 NonToll - Distance", // 19
            "S3 NonToll - Length on HOV Facility", // 20
            "S3 Toll - Time", // 21
            "S3 Toll - Free Flow Time", // 22
            "S3 Toll - Distance", // 23
            "S3 Toll - Toll Value", // 24
            "S3 Toll - Length on HOV Facility", // 25
            "DA NonToll - Std Dev Time", // 26
            "DA Toll - Std Dev Time", // 27
            "S2 NonToll - Std Dev Time", //28
            "S2 Toll - Std Dev Time", //29
            "S3 NonToll - Std Dev Time", //30
            "S3 Toll - Std Dev Time" //31
            
            };

    private static final String[]         NM_SKIM_NAMES          = {"walkTime", "bikeTime"};
    private static final int              NUM_NM_SKIMS           = NM_SKIM_NAMES.length;

    private static final String[]         NM_SKIM_DESCRIPTIONS   = {"walk time", "bike time"};

    private double[][][]                  storedFromTazDistanceSkims;
    private double[][][]                  storedToTazDistanceSkims;

    /**
     * Get distance from taz to all zones.
     * 
     * @param taz
     * @param period
     * @return An array of distances to all other zones.
     */
    public double[] getTazDistanceFromTaz(int taz, int period)
    {

        return storedFromTazDistanceSkims[period][taz];
    }

    /**
     * Get distance from taz to all zones.
     * 
     * @param taz
     * @param period
     * @return An array of distances to all other zones.
     */
    public double[] getTazDistanceToTaz(int taz, int period)
    {

        return storedToTazDistanceSkims[period][taz];
    }

    public AutoAndNonMotorizedSkimsCalculator(HashMap<String, String> rbMap)
    {

        // Create the UECs
        String uecPath = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = uecPath
                + Util.getStringValueFromPropertyMap(rbMap, "skims.auto.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.data.page");
        int autoSkimEaPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.ea.page");
        int autoSkimAmPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.am.page");
        int autoSkimMdPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.md.page");
        int autoSkimPmPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.pm.page");
        int autoSkimEvPage = Util.getIntegerValueFromPropertyMap(rbMap, "skims.auto.ev.page");

        File uecFile = new File(uecFileName);
        autoSkimsDMU = new AutoSkimsDMU();
        autoSkimUECs = new UtilityExpressionCalculator[NUM_PERIODS];
        autoSkimUECs[EA] = new UtilityExpressionCalculator(uecFile, autoSkimEaPage, dataPage,
                rbMap, autoSkimsDMU);
        autoSkimUECs[AM] = new UtilityExpressionCalculator(uecFile, autoSkimAmPage, dataPage,
                rbMap, autoSkimsDMU);
        autoSkimUECs[MD] = new UtilityExpressionCalculator(uecFile, autoSkimMdPage, dataPage,
                rbMap, autoSkimsDMU);
        autoSkimUECs[PM] = new UtilityExpressionCalculator(uecFile, autoSkimPmPage, dataPage,
                rbMap, autoSkimsDMU);
        autoSkimUECs[EV] = new UtilityExpressionCalculator(uecFile, autoSkimEvPage, dataPage,
                rbMap, autoSkimsDMU);
        iv = new IndexValues();

        mgraManager = MgraDataManager.getInstance();

        // distances = new double[mgraManager.getMaxMgra()+1];
    }

    public void setTazDistanceSkimArrays(double[][][] storedFromTazDistanceSkims,
            double[][][] storedToTazDistanceSkims)
    {
        this.storedFromTazDistanceSkims = storedFromTazDistanceSkims;
        this.storedToTazDistanceSkims = storedToTazDistanceSkims;
    }

    /**
     * Return the array of auto skims for the origin MGRA, destination MGRA, and
     * departure time period. Used for appending skims data to estimation files,
     * not part of main ABM.
     * 
     * @param origMgra
     *            Origin MGRA
     * @param workMgra
     *            Destination MGRA
     * @param departPeriod
     *            Departure skim period index (currently 1-5)
     * @param vot Value-of-time ($/hr)
     * @return Array of 9 skim values for the MGRA pair and departure period
     */
    public double[] getAutoSkims(int origMgra, int destMgra, int departPeriod, float vot, boolean debug,
            Logger logger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
            logger.info("");
            logger.info("");
            header = "get auto skims debug info for origMgra=" + origMgra + ", destMgra="
                    + destMgra + ", period index=" + departPeriod + ", period label="
                    + PERIODS[departPeriod];
            for (int i = 0; i < header.length(); i++)
                separator += "^";
        }

        // assign a helper UEC object to the array element for the desired
        // departure
        // time period
        UtilityExpressionCalculator autoSkimUEC = autoSkimUECs[departPeriod];

        // declare the array to hold the skim values, which will be returned
        double[] autoSkims = null;

        if (origMgra > 0 && destMgra > 0)
        {

            int oTaz = mgraManager.getTaz(origMgra);
            int dTaz = mgraManager.getTaz(destMgra);

            iv.setOriginZone(oTaz);
            iv.setDestZone(dTaz);
            
            autoSkimsDMU.setVOT(vot);

            // use the UEC to return skim values for the orign/destination TAZs
            // associated with the MGRAs
            autoSkims = autoSkimUEC.solve(iv, autoSkimsDMU, null);
            if (debug)
                autoSkimUEC.logAnswersArray(logger, String.format(
                        "autoSkimUEC:  oMgra=%d, dMgra=%d, period=%d", origMgra, destMgra,
                        departPeriod));

        }

        if (debug)
        {

            logger.info(separator);
            logger.info(header);
            logger.info(separator);

            logger.info("auto skims array values");
            logger.info(String.format("%5s %40s %15s", "i", "skimName", "value"));
            logger.info(String.format("%5s %40s %15s", "-----", "----------", "----------"));
            for (int i = 0; i < autoSkims.length; i++)
            {
                logger.info(String.format("%5d %40s %15.2f", i, AUTO_SKIM_DESCRIPTIONS[i],
                        autoSkims[i]));
            }

        }

        return autoSkims;

    }

    /**
     * Get the non-motorized skims.
     * 
     * Get all the mgras within walking distance of the origin mgra. If the set
     * of mgras is not null, and the destination mgra is in the set, get the
     * walk and bike times from the mgraManager;
     * 
     * If the destination mgra is not within walking distance of the origin
     * MGRA, get the drive-alone non-toll off-peak distance skim value for the
     * mgra pair and calculate the walk time and bike time.
     * 
     * @param origMgra
     *            The origin mgra
     * @param destMgra
     *            The destination mgra
     * @return An array of distances
     */
    public double[] getNonMotorizedSkims(int origMgra, int destMgra, int departPeriod,
            boolean debug, Logger logger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
            logger.info("");
            logger.info("");
            header = "get non-motorized skims debug info for origMgra=" + origMgra + ", destMgra="
                    + destMgra;
            for (int i = 0; i < header.length(); i++)
                separator += "^";
        }

        double[] nmSkims = new double[NUM_NM_SKIMS];

        // get the array of mgras within walking distance of the origin
        int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(origMgra);

        // if one of the walk mgras is the destination, set the skim values and
        // return
        if (walkMgras != null)
        {

            for (int wMgra : walkMgras)
            {

                if (wMgra == destMgra)
                {
                    nmSkims[WALK_INDEX] = mgraManager.getMgraToMgraWalkTime(origMgra, destMgra);
                    nmSkims[BIKE_INDEX] = mgraManager.getMgraToMgraBikeTime(origMgra, destMgra);

                    if (debug)
                    {

                        logger.info(separator);
                        logger.info(header);
                        logger.info(separator);

                        logger.info("non-motorized skims array values");
                        logger.info("determined from the mgraManager for an mgra pair within walking distance of each other.");
                        logger.info(String.format("%5s %40s %15s", "i", "skimName", "value"));
                        logger.info(String.format("%5s %40s %15s", "-----", "----------",
                                "----------"));
                        for (int i = 0; i < nmSkims.length; i++)
                        {
                            logger.info(String.format("%5d %40s %15.2f", i,
                                    NM_SKIM_DESCRIPTIONS[i], nmSkims[i]));
                        }

                    }

                    return nmSkims;
                }

            }

        }

        // the destination was not within walk distance, so calculate walk and
        // bike
        // times from the TAZ-TAZ skim distance
        int oTaz = mgraManager.getTaz(origMgra);
        int dTaz = mgraManager.getTaz(destMgra);

        if (debug)
        {

            logger.info(separator);
            logger.info(header);
            logger.info(separator);

            logger.info("non-motorized skims array values");
            logger.info("calculated for an mgra pair not within walking distance of each other.");
            logger.info("origTaz = " + oTaz + ", destTaz = " + dTaz + ", period = " + departPeriod
                    + ", od distance = "
                    + (float) storedFromTazDistanceSkims[departPeriod][oTaz][dTaz]);
            logger.info(String.format("%5s %40s %15s", "i", "skimName", "value"));
            logger.info(String.format("%5s %40s %15s", "-----", "----------", "----------"));
            for (int i = 0; i < nmSkims.length; i++)
            {
                logger.info(String
                        .format("%5d %40s %15.2f", i, NM_SKIM_DESCRIPTIONS[i], nmSkims[i]));
            }

        }

        nmSkims[WALK_INDEX] = (storedFromTazDistanceSkims[departPeriod][oTaz][dTaz] / WALK_SPEED) * 60.0;
        nmSkims[BIKE_INDEX] = (storedFromTazDistanceSkims[departPeriod][oTaz][dTaz] / BIKE_SPEED) * 60.0;

        return nmSkims;

    }

    /*
     * public double[] getNonMotorizedSkims(int origMgra, int destMgra, int
     * departPeriod, boolean debug, Logger logger) {
     * 
     * String separator = ""; String header = ""; if (debug) { logger.info("");
     * logger.info(""); header =
     * "get non-motorized skims debug info for origMgra=" + origMgra +
     * ", destMgra=" + destMgra; for (int i = 0; i < header.length(); i++)
     * separator += "^"; }
     * 
     * double[] nmSkims = new double[NUM_NM_SKIMS];
     * 
     * // get the array of mgras within walking distance of the origin int[]
     * walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(origMgra);
     * 
     * // if one of the walk mgras is the destination, set the skim values and
     * // return if (walkMgras != null) {
     * 
     * for (int wMgra : walkMgras) {
     * 
     * if (wMgra == destMgra) { nmSkims[WALK_INDEX] =
     * mgraManager.getMgraToMgraWalkTime(origMgra, destMgra);
     * nmSkims[BIKE_INDEX] = mgraManager.getMgraToMgraBikeTime(origMgra,
     * destMgra);
     * 
     * if (debug) {
     * 
     * logger.info(separator); logger.info(header); logger.info(separator);
     * 
     * logger.info("non-motorized skims array values"); logger .info(
     * "determined from the mgraManager for an mgra pair within walking distance of each other."
     * ); logger.info(String.format("%5s %40s %15s", "i", "skimName", "value"));
     * logger.info(String.format("%5s %40s %15s", "-----", "----------",
     * "----------")); for (int i = 0; i < nmSkims.length; i++) {
     * logger.info(String.format("%5d %40s %15.2f", i, NM_SKIM_DESCRIPTIONS[i],
     * nmSkims[i])); }
     * 
     * }
     * 
     * return nmSkims; }
     * 
     * }
     * 
     * }
     * 
     * // the destination was not within walk distance, so calculate walk and
     * bike // times from the TAZ-TAZ skim distance int oTaz =
     * mgraManager.getTaz(origMgra); int dTaz = mgraManager.getTaz(destMgra);
     * 
     * iv.setOriginZone(oTaz); iv.setDestZone(dTaz);
     * 
     * // get the DA NT OP distance value for the mgra pair double[] autoSkims =
     * autoSkimUECs[OP].solve(iv, dmu, null); double distance = autoSkims[2];
     * 
     * nmSkims[WALK_INDEX] = (distance / WALK_SPEED) * 60.0; nmSkims[BIKE_INDEX]
     * = (distance / BIKE_SPEED) * 60.0;
     * 
     * if (debug) {
     * 
     * logger.info(separator); logger.info(header); logger.info(separator);
     * 
     * logger.info("non-motorized skims array values"); logger.info(
     * "calculated for an mgra pair not within walking distance of each other."
     * ); logger.info("origTaz = " + oTaz + ", destTaz = " + dTaz +
     * ", od distance = " + (float) distance);
     * logger.info(String.format("%5s %40s %15s", "i", "skimName", "value"));
     * logger.info(String.format("%5s %40s %15s", "-----", "----------",
     * "----------")); for (int i = 0; i < nmSkims.length; i++) {
     * logger.info(String .format("%5d %40s %15.2f", i, NM_SKIM_DESCRIPTIONS[i],
     * nmSkims[i])); }
     * 
     * }
     * 
     * return nmSkims;
     * 
     * }
     */
    /**
     * Get all the mgras within walking distance of the origin mgra and set the
     * distances to those mgras.
     * 
     * Then loop through all mgras without a distance and get the drive-alone
     * non-toll off-peak distance skim value for the taz pair associated with
     * each mgra pair.
     * 
     * @param origMgra
     *            The origin mgra
     * @param An
     *            array in which to put the distances
     * @param tourModeIsAuto
     *            is a boolean set to true if tour mode is not non-motorized,
     *            transit, or school bus. if auto tour mode, then no need to
     *            determine walk distance, and drive skims can be used directly.
     *            public void getDistancesFromMgra( int origMgra, double[]
     *            distances, boolean tourModeIsAuto ) {
     * 
     *            Arrays.fill(distances, 0);
     * 
     *            if ( ! tourModeIsAuto ){
     * 
     *            // get the array of mgras within walking distance of the
     *            destination int[] walkMgras =
     *            mgraManager.getMgrasWithinWalkDistanceFrom(origMgra);
     * 
     *            // set the distance values for the mgras walkable to the
     *            destination if (walkMgras != null) {
     * 
     *            // get distances, in feet, and convert to miles for (int wMgra
     *            : walkMgras) distances[wMgra] =
     *            mgraManager.getMgraToMgraWalkDistFrom(origMgra, wMgra) /
     *            5280.0;
     * 
     *            }
     * 
     *            }
     * 
     * 
     *            int oTaz = mgraManager.getTaz(origMgra);
     *            iv.setOriginZone(oTaz); for (int wMgra=1; wMgra <=
     *            mgraManager.getMaxMgra(); wMgra++) {
     * 
     *            // skip mgras where distance has already been set if (
     *            distances[wMgra] > 0 ) continue;
     * 
     *            // calculate distances from the TAZ-TAZ skim distance int dTaz
     *            = mgraManager.getTaz(wMgra); iv.setDestZone(dTaz); double[]
     *            autoSkims = autoSkimUECs[OP].solve(iv, dmu, null);
     * 
     *            distances[wMgra] = autoSkims[2]; }
     * 
     *            }
     */

    /**
     * Get all the mgras within walking distance of the origin mgra and set the
     * distances to those mgras.
     * 
     * Then loop through all mgras without a distance and get the drive-alone
     * non-toll off-peak distance skim value for the taz pair associated with
     * each mgra pair.
     * 
     * @param origMgra
     *            The origin mgra
     * @param An
     *            array in which to put the distances
     * @param tourModeIsAuto
     *            is a boolean set to true if tour mode is not non-motorized,
     *            transit, or school bus. if auto tour mode, then no need to
     *            determine walk distance, and drive skims can be used directly.
     * 
     */

    public void getDistancesFromMgra(int origMgra, double[] distances, boolean tourModeIsAuto)
    {

        Arrays.fill(distances, 0);

        if (!tourModeIsAuto)
        {

            // get the array of mgras within walking distance of the destination
            int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(origMgra);

            // set the distance values for the mgras walkable to the destination
            if (walkMgras != null)
            {

                // get distances, in feet, and convert to miles
                for (int wMgra : walkMgras)
                    distances[wMgra] = mgraManager.getMgraToMgraWalkDistFrom(origMgra, wMgra) / 5280.0;

            }

        }

        int oTaz = mgraManager.getTaz(origMgra);

        for (int wMgra = 1; wMgra <= mgraManager.getMaxMgra(); wMgra++)
        {

            // skip mgras where distance has already been set
            if (distances[wMgra] > 0) continue;

            int dTaz = mgraManager.getTaz(wMgra);
            distances[wMgra] = storedFromTazDistanceSkims[MD][oTaz][dTaz];
        }

    }

    /**
     * Get all the mgras within walking distance of the destination mgra and set
     * the distances from those mgras.
     * 
     * Then loop through all mgras without a distance and get the drive-alone
     * non-toll off-peak distance skim value for the taz pair associated with
     * each mgra pair.
     * 
     * @param destMgra
     *            The destination mgra
     * @param An
     *            array in which to put the distances
     * @param tourModeIsAuto
     *            is a boolean set to true if tour mode is not non-motorized,
     *            transit, or school bus. if auto tour mode, then no need to
     *            determine walk distance, and drive skims can be used directly.
     */

    public void getDistancesToMgra(int destMgra, double[] distances, boolean tourModeIsAuto)
    {

        Arrays.fill(distances, 0);

        if (!tourModeIsAuto)
        {

            // get the array of mgras within walking distance of the destination
            int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceTo(destMgra);

            // set the distance values for the mgras walkable to the destination
            if (walkMgras != null)
            {

                // get distances, in feet, and convert to miles
                // get distances from destMgra since this is the direction of
                // distances read from the data file
                for (int wMgra : walkMgras)
                    distances[wMgra] = mgraManager.getMgraToMgraWalkDistTo(wMgra, destMgra) / 5280.0;

            }

        }

        // if the TAZ distances have not been computed yet for this destination
        // TAZ, compute them from the UEC.
        int dTaz = mgraManager.getTaz(destMgra);

        for (int wMgra = 1; wMgra <= mgraManager.getMaxMgra(); wMgra++)
        {

            // skip mgras where distance has already been set
            if (distances[wMgra] > 0) continue;

            int oTaz = mgraManager.getTaz(wMgra);
            distances[wMgra] = storedToTazDistanceSkims[MD][dTaz][oTaz];

        }

    }

    /*
     * public void getDistancesToMgra( int destMgra, double[] distances, boolean
     * tourModeIsAuto ) {
     * 
     * Arrays.fill(distances, 0);
     * 
     * if ( ! tourModeIsAuto ){
     * 
     * // get the array of mgras within walking distance of the destination
     * int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceTo(destMgra);
     * 
     * // set the distance values for the mgras walkable to the destination if
     * (walkMgras != null) {
     * 
     * // get distances, in feet, and convert to miles // get distances from
     * destMgra since this is the direction of distances read from the data file
     * for (int wMgra : walkMgras) distances[wMgra] =
     * mgraManager.getMgraToMgraWalkDistTo(destMgra, wMgra) / 5280.0;
     * 
     * }
     * 
     * }
     * 
     * 
     * int dTaz = mgraManager.getTaz(destMgra); iv.setDestZone(dTaz); for (int
     * wMgra=1; wMgra <= mgraManager.getMaxMgra(); wMgra++) {
     * 
     * // skip mgras where distance has already been set if ( distances[wMgra] >
     * 0 ) continue;
     * 
     * // calculate distances from the TAZ-TAZ skim distance int oTaz =
     * mgraManager.getTaz(wMgra); iv.setOriginZone(oTaz); double[] autoSkims =
     * autoSkimUECs[OP].solve(iv, dmu, null);
     * 
     * distances[wMgra] = autoSkims[2]; }
     * 
     * }
     */

    /*
     * public double[] getDistancesToMgra( int destMgra, boolean tourModeIsAuto
     * ) {
     * 
     * 
     * Arrays.fill(distances, 0);
     * 
     * if ( ! tourModeIsAuto ){
     * 
     * // get the array of mgras within walking distance of the destination
     * int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceTo(destMgra);
     * 
     * // set the distance values for the mgras walkable to the destination if
     * (walkMgras != null) {
     * 
     * // get distances, in feet, and convert to miles // get distances from
     * destMgra since this is the direction of distances read from the data file
     * for (int wMgra : walkMgras) distances[wMgra] =
     * mgraManager.getMgraToMgraWalkDistTo(destMgra, wMgra) / 5280.0;
     * 
     * }
     * 
     * }
     * 
     * 
     * 
     * // if the TAZ distances have not been computed yet for this destination
     * TAZ, compute them from the UEC. int dTaz = mgraManager.getTaz(destMgra);
     * 
     * for (int wMgra=1; wMgra <= mgraManager.getMaxMgra(); wMgra++) {
     * 
     * // skip mgras where distance has already been set if ( distances[wMgra] >
     * 0 ) continue;
     * 
     * int oTaz = mgraManager.getTaz(wMgra); distances[wMgra] =
     * storedToTazDistanceSkims[OP][dTaz][oTaz];
     * 
     * }
     * 
     * return Arrays.copyOf( distances, distances.length );
     * 
     * }
     * 
     * 
     * /** Calculate utility expressions for auto skims for a given origin to
     * get distances to all destination mgras, and return off-peak sov distance.
     * 
     * @param oMgra The origin mgra
     * 
     * @return An array of off-peak sov distances
     */
    /*
     * public void getOpSkimDistancesFromMgra( int oMgra, double[] distances ) {
     * 
     * int oTaz = mgraManager.getTaz(oMgra);
     * 
     * for (int i=1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     * // calculate distances from the TAZ-TAZ skim distance int dTaz =
     * mgraManager.getTaz(i); distances[i] =
     * storedFromTazDistanceSkims[OP][oTaz][dTaz];
     * 
     * }
     * 
     * }
     */

    /*
     * public double[] getAmPkSkimDistancesFromMgra( int oMgra ) {
     * 
     * int oTaz = mgraManager.getTaz(oMgra);
     * 
     * for (int i=1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     * // calculate distances from the TAZ-TAZ skim distance int dTaz =
     * mgraManager.getTaz(i); distances[i] =
     * storedFromTazDistanceSkims[AM][oTaz][dTaz];
     * 
     * }
     * 
     * return Arrays.copyOf( distances, distances.length );
     * 
     * }
     */

    /*
     * public double[] getOpSkimDistancesFromMgra(int oMgra) {
     * 
     * double[] distances = new double[mgraManager.getMaxMgra() + 1];
     * 
     * int oTaz = mgraManager.getTaz(oMgra); iv.setOriginZone(oTaz);
     * 
     * for (int i = 1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     * int dTaz = mgraManager.getTaz(i); iv.setDestZone(dTaz);
     * 
     * // sov distance double[] autoResults = autoSkimUECs[OP].solve(iv, dmu,
     * null); distances[i] = autoResults[2];
     * 
     * }
     * 
     * return distances; }
     */

    /*
     * public void getOpSkimDistancesFromMgra(int oMgra, double[] distances) {
     * 
     * Arrays.fill( distances, 0 );
     * 
     * int oTaz = mgraManager.getTaz(oMgra); iv.setOriginZone(oTaz);
     * 
     * for (int i = 1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     * int dTaz = mgraManager.getTaz(i); iv.setDestZone(dTaz);
     * 
     * // sov distance double[] autoResults = autoSkimUECs[OP].solve(iv, dmu,
     * null); distances[i] = autoResults[2];
     * 
     * }
     * 
     * }
     */

    public void getOpSkimDistancesFromMgra(int oMgra, double[] distances)
    {

        int oTaz = mgraManager.getTaz(oMgra);

        for (int i = 1; i <= mgraManager.getMaxMgra(); i++)
        {

            // calculate distances from the TAZ-TAZ skim distance
            int dTaz = mgraManager.getTaz(i);
            distances[i] = storedFromTazDistanceSkims[MD][oTaz][dTaz];

        }

    }

    /**
     * Calculate utility expressions for auto skims for a given origin to get
     * distances to all destination mgras, and return am peak sov distance.
     * 
     * @param oMgra
     *            The origin mgra
     * @return An array of am peak sov distances public double[]
     *         getAmPkSkimDistancesFromMgra(int oMgra) {
     * 
     *         double[] distances = new double[mgraManager.getMaxMgra() + 1];
     * 
     *         int oTaz = mgraManager.getTaz(oMgra); iv.setOriginZone(oTaz);
     * 
     *         for (int i = 1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     *         int dTaz = mgraManager.getTaz(i); iv.setDestZone(dTaz);
     * 
     *         // sov distance double[] autoResults = autoSkimUECs[AM].solve(iv,
     *         dmu, null); distances[i] = autoResults[2];
     * 
     *         }
     * 
     *         return distances; }
     */

    public void getAmPkSkimDistancesFromMgra(int oMgra, double[] distances)
    {

        int oTaz = mgraManager.getTaz(oMgra);

        for (int i = 1; i <= mgraManager.getMaxMgra(); i++)
        {

            // calculate distances from the TAZ-TAZ skim distance
            int dTaz = mgraManager.getTaz(i);
            distances[i] = storedFromTazDistanceSkims[AM][oTaz][dTaz];

        }

    }

    /*
     * public void getAmPkSkimDistancesFromMgra(int oMgra, double[] distances) {
     * 
     * Arrays.fill( distances, 0 );
     * 
     * int oTaz = mgraManager.getTaz(oMgra); iv.setOriginZone(oTaz);
     * 
     * for (int i = 1; i <= mgraManager.getMaxMgra(); i++) {
     * 
     * int dTaz = mgraManager.getTaz(i); iv.setDestZone(dTaz);
     * 
     * // sov distance double[] autoResults = autoSkimUECs[AM].solve(iv, dmu,
     * null); distances[i] = autoResults[2];
     * 
     * }
     * 
     * }
     */

    /**
     * log a report of the final skim values for the MGRA odt
     * 
     * @param orig
     *            is the origin mgra for the segment
     * @param dest
     *            is the destination mgra for the segment
     * @param depart
     *            is the departure period for the segment
     * @param bestTapPairs
     *            is an int[][] of TAP values with the first dimesion the ride
     *            mode and second dimension a 2 element array with best orig and
     *            dest TAP
     * @param returnedSkims
     *            is a double[][] of skim values with the first dimesion the
     *            ride mode indices and second dimention the skim categories
     */
    public void logReturnedSkims(int orig, int dest, int depart, double[] skims, String skimLabel,
            Logger logger)
    {

        String separator = "";
        String header = "";

        logger.info("");
        logger.info("");
        header = skimLabel + " skim value tables for origMgra=" + orig + ", destMgra=" + dest
                + ", departperiod=" + depart;
        for (int i = 0; i < header.length(); i++)
            separator += "^";

        logger.info(separator);
        logger.info(header);
        logger.info("");

        String tableRecord = "";
        for (int i = 0; i < skims.length; i++)
        {
            tableRecord = String.format("%-5d %12.5f  ", i + 1, skims[i]);
            logger.info(tableRecord);
        }

        logger.info("");
        logger.info(separator);
    }

    public int getNumSkimPeriods()
    {
        return NUM_PERIODS;
    }

    public int getNumAutoSkims()
    {
        return NUM_AUTO_SKIMS;
    }

    public String[] getAutoSkimNames()
    {
        return AUTO_SKIM_NAMES;
    }

    public int getNumNmSkims()
    {
        return NUM_NM_SKIMS;
    }

    public String[] getNmSkimNames()
    {
        return NM_SKIM_NAMES;
    }

    public int getNmWalkTimeSkimIndex()
    {
        return WALK_INDEX;
    }

    public int getNmBikeTimeSkimIndex()
    {
        return BIKE_INDEX;
    }

}