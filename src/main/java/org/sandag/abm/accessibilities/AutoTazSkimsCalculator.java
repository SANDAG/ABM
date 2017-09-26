package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
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
public class AutoTazSkimsCalculator
        implements Serializable
{

	public static final int              EA          = TransitWalkAccessUEC.EA;
    public static final int              AM          = TransitWalkAccessUEC.AM;
    public static final int              MD          = TransitWalkAccessUEC.MD;
    public static final int              PM          = TransitWalkAccessUEC.PM;
    public static final int              EV          = TransitWalkAccessUEC.EV;
    public static final int              NUM_PERIODS = TransitWalkAccessUEC.PERIODS.length;

    // declare an array of UEC objects, 1 for each time period
    private UtilityExpressionCalculator[] autoDistOD_UECs;

    // The simple auto skims UEC does not use any DMU variables
    private VariableTable                 dmu         = null;

    private TazDataManager                tazManager;

    private double[][][]                  storedFromTazDistanceSkims;
    private double[][][]                  storedToTazDistanceSkims;
    private int                           maxTaz;

    public AutoTazSkimsCalculator(HashMap<String, String> rbMap)
    {

        // Create the UECs
        String uecPath = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = uecPath
                + Util.getStringValueFromPropertyMap(rbMap, "taz.distance.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "taz.distance.data.page");
        int autoSkimEaOdPage = Util
                .getIntegerValueFromPropertyMap(rbMap, "taz.od.distance.ea.page");
        int autoSkimAmOdPage = Util
                .getIntegerValueFromPropertyMap(rbMap, "taz.od.distance.am.page");
        int autoSkimMdOdPage = Util
                .getIntegerValueFromPropertyMap(rbMap, "taz.od.distance.md.page");
        int autoSkimPmOdPage = Util
                .getIntegerValueFromPropertyMap(rbMap, "taz.od.distance.pm.page");
        int autoSkimEvOdPage = Util
                .getIntegerValueFromPropertyMap(rbMap, "taz.od.distance.ev.page");

        File uecFile = new File(uecFileName);
        autoDistOD_UECs = new UtilityExpressionCalculator[NUM_PERIODS];
        autoDistOD_UECs[EA] = new UtilityExpressionCalculator(uecFile, autoSkimEaOdPage, dataPage,
                rbMap, dmu);
        autoDistOD_UECs[AM] = new UtilityExpressionCalculator(uecFile, autoSkimAmOdPage, dataPage,
                rbMap, dmu);
        autoDistOD_UECs[MD] = new UtilityExpressionCalculator(uecFile, autoSkimMdOdPage, dataPage,
                rbMap, dmu);
        autoDistOD_UECs[PM] = new UtilityExpressionCalculator(uecFile, autoSkimPmOdPage, dataPage,
                rbMap, dmu);
        autoDistOD_UECs[EV] = new UtilityExpressionCalculator(uecFile, autoSkimEvOdPage, dataPage,
                rbMap, dmu);

        tazManager = TazDataManager.getInstance();
        maxTaz = tazManager.getMaxTaz();

        storedFromTazDistanceSkims = new double[NUM_PERIODS + 1][maxTaz + 1][];
        storedToTazDistanceSkims = new double[NUM_PERIODS + 1][maxTaz + 1][];

    }

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
     */
    public void computeTazDistanceArrays()
    {

        IndexValues iv = new IndexValues();

        for (int oTaz = 1; oTaz <= maxTaz; oTaz++)
        {

            storedFromTazDistanceSkims[EA][oTaz] = new double[maxTaz + 1];
            storedToTazDistanceSkims[EA][oTaz] = new double[maxTaz + 1];
            storedFromTazDistanceSkims[AM][oTaz] = new double[maxTaz + 1];
            storedToTazDistanceSkims[AM][oTaz] = new double[maxTaz + 1];
            storedFromTazDistanceSkims[MD][oTaz] = new double[maxTaz + 1];
            storedToTazDistanceSkims[MD][oTaz] = new double[maxTaz + 1];
            storedFromTazDistanceSkims[PM][oTaz] = new double[maxTaz + 1];
            storedToTazDistanceSkims[PM][oTaz] = new double[maxTaz + 1];
            storedFromTazDistanceSkims[EV][oTaz] = new double[maxTaz + 1];
            storedToTazDistanceSkims[EV][oTaz] = new double[maxTaz + 1];

        }

        for (int oTaz = 1; oTaz <= maxTaz; oTaz++)
        {

            iv.setOriginZone(oTaz);

            double[] eaAutoDist = autoDistOD_UECs[EA].solve(iv, dmu, null);
            double[] amAutoDist = autoDistOD_UECs[AM].solve(iv, dmu, null);
            double[] mdAutoDist = autoDistOD_UECs[MD].solve(iv, dmu, null);
            double[] pmAutoDist = autoDistOD_UECs[PM].solve(iv, dmu, null);
            double[] evAutoDist = autoDistOD_UECs[EV].solve(iv, dmu, null);

            for (int d = 0; d < maxTaz; d++)
            {

                storedFromTazDistanceSkims[EA][oTaz][d + 1] = eaAutoDist[d];
                storedFromTazDistanceSkims[AM][oTaz][d + 1] = amAutoDist[d];
                storedFromTazDistanceSkims[MD][oTaz][d + 1] = mdAutoDist[d];
                storedFromTazDistanceSkims[PM][oTaz][d + 1] = pmAutoDist[d];
                storedFromTazDistanceSkims[EV][oTaz][d + 1] = evAutoDist[d];

                storedToTazDistanceSkims[EA][d + 1][oTaz] = eaAutoDist[d];
                storedToTazDistanceSkims[AM][d + 1][oTaz] = amAutoDist[d];
                storedToTazDistanceSkims[MD][d + 1][oTaz] = mdAutoDist[d];
                storedToTazDistanceSkims[PM][d + 1][oTaz] = pmAutoDist[d];
                storedToTazDistanceSkims[EV][d + 1][oTaz] = evAutoDist[d];

            }

            // iv.setDestZone( oTaz );
            //
            // amAutoDist = autoDistDO_UECs[AM].solve(iv, dmu, null);
            // opAutoDist = autoDistDO_UECs[OP].solve(iv, dmu, null);
            //
            // for (int d=0; d < maxTaz; d++)
            // {
            //
            // storedToTazDistanceSkims[AM][d+1][oTaz] = amAutoDist[d];
            // storedToTazDistanceSkims[OP][d+1][oTaz] = opAutoDist[d];
            //
            // }

        }

    }

    public double[][][] getStoredFromTazToAllTazsDistanceSkims()
    {
        return storedFromTazDistanceSkims;
    }

    public double[][][] getStoredToTazFromAllTazsDistanceSkims()
    {
        return storedToTazDistanceSkims;
    }

    public void clearStoredTazsDistanceSkims()
    {

        for (int i = 0; i < storedFromTazDistanceSkims.length; i++)
        {
            for (int j = 0; j < storedFromTazDistanceSkims[i].length; j++)
                storedFromTazDistanceSkims[i][j] = null;
            storedFromTazDistanceSkims[i] = null;
        }
        storedFromTazDistanceSkims = null;

        for (int i = 0; i < storedToTazDistanceSkims.length; i++)
        {
            for (int j = 0; j < storedToTazDistanceSkims[i].length; j++)
                storedToTazDistanceSkims[i][j] = null;
            storedToTazDistanceSkims[i] = null;
        }
        storedToTazDistanceSkims = null;

    }

}