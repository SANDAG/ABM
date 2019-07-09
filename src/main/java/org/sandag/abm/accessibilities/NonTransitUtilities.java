package org.sandag.abm.accessibilities;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import java.io.File;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.TNCAndTaxiWaitTimeCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.AutoUEC;
import org.sandag.abm.modechoice.MaasUEC;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.NonMotorUEC;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.Tracer;

/**
 * This class builds utility components for auto modes (SOV and HOV).
 * 
 * @author Joel Freedman
 * @version May, 2009
 */
public class NonTransitUtilities
        implements Serializable
{

    protected transient Logger           logger               = Logger.getLogger(NonTransitUtilities.class);

    public static final int              OFFPEAK_PERIOD_INDEX = 0;
    public static final int              PEAK_PERIOD_INDEX    = 1;

    private static final String[]        SOVPERIODS           = {"OP", "PK"};
    private static final String[]        HOVPERIODS           = {"OP", "PK"};
    private static final String[]        NMTPERIODS           = {"OP"};
    private static final String[]        MAASPERIODS           = {"OP", "PK"};
    

    // store taz-taz exponentiated utilities (period, from taz, to taz)
    private double[][][]                 sovExpUtilities;
    private double[][][]                 hovExpUtilities;
    private double[][][]                 nMotorExpUtilities;
    private double[][][]                 maasExpUtilities; 

    private double[]                     avgTazHourlyParkingCost;
    private float[]                     avgTazTaxiWaitTime;
    private float[]                     avgTazSingleTNCWaitTime;
    private float[]                     avgTazSharedTNCWaitTime;

    // A HashMap of non-motorized utilities, period,oMgra,dMgra (where dMgra is
    // ragged)
    private HashMap<Integer, Double>[][] mgraNMotorExpUtilities;

    private TazDataManager               tazManager;
    private MgraDataManager              mgraManager;

    private AutoUEC[]                    sovUEC;
    private AutoUEC[]                    hovUEC;
    private NonMotorUEC[]                nMotorUEC;
    private MaasUEC[]                    maasUEC;

    private int                          maxTaz;

    private boolean                      trace, seek;
    private Tracer                       tracer;
    
    //added for TNC and Taxi modes
    TNCAndTaxiWaitTimeCalculator tncTaxiWaitTimeCalculator = null;


    /**
     * Constructor.
     * 
     * @param rb
     *            Resourcebundle with path to acc.uec.file, acc.data.page,
     *            acc.sov.offpeak.page, and acc.hov.offpeak.page
     */

    public NonTransitUtilities(HashMap<String, String> rbMap, double[][][] mySovExpUtilities,
            double[][][] myHovExpUtilities, double[][][] myNMotorExpUtilities, double[][][] myMaasExpUtilities)
    {

        sovExpUtilities = mySovExpUtilities;
        hovExpUtilities = myHovExpUtilities;
        nMotorExpUtilities = myNMotorExpUtilities;
        maasExpUtilities = myMaasExpUtilities;

        mgraManager = MgraDataManager.getInstance();
        tazManager = TazDataManager.getInstance(rbMap);

        maxTaz = tazManager.maxTaz;

        logger.info("max Taz " + maxTaz);

        // Create the peak and off-peak UECs
        String uecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.data.page");
        int offpeakSOVPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.sov.offpeak.page");
        int offpeakHOVPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.hov.offpeak.page");
        int peakSOVPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.sov.peak.page");
        int peakHOVPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.hov.peak.page");
        int nonMotorPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.nonmotorized.page");
        int peakMaasPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.maas.peak.page");
        int offpeakMaasPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.maas.offpeak.page");

        sovUEC = new AutoUEC[SOVPERIODS.length];
        hovUEC = new AutoUEC[HOVPERIODS.length];
        nMotorUEC = new NonMotorUEC[NMTPERIODS.length];
        maasUEC = new MaasUEC[NMTPERIODS.length];

        sovUEC[OFFPEAK_PERIOD_INDEX] = new AutoUEC(rbMap, uecFileName, offpeakSOVPage, dataPage);
        sovUEC[PEAK_PERIOD_INDEX] = new AutoUEC(rbMap, uecFileName, peakSOVPage, dataPage);
        hovUEC[OFFPEAK_PERIOD_INDEX] = new AutoUEC(rbMap, uecFileName, offpeakHOVPage, dataPage);
        hovUEC[PEAK_PERIOD_INDEX] = new AutoUEC(rbMap, uecFileName, peakHOVPage, dataPage);
        nMotorUEC[OFFPEAK_PERIOD_INDEX] = new NonMotorUEC(rbMap, uecFileName, nonMotorPage,
                dataPage);
        maasUEC[OFFPEAK_PERIOD_INDEX] = new MaasUEC(rbMap, uecFileName, offpeakMaasPage, dataPage);
        maasUEC[PEAK_PERIOD_INDEX] = new MaasUEC(rbMap, uecFileName, peakMaasPage, dataPage);
 
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        int[] traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        int[] traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

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
                    logger.info("Setting trace zone pair in NonTransitUtilities Object for i: "+ traceOtaz[i] + " j: " + traceDtaz[j]);
                }
            }
        }
        seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");

        int maxMgra = mgraManager.getMaxMgra();
//        logger.info("max Mgra " + maxMgra);

        mgraNMotorExpUtilities = new HashMap[NMTPERIODS.length][maxMgra + 1];

        sovExpUtilities = new double[SOVPERIODS.length][maxTaz + 1][];
        hovExpUtilities = new double[HOVPERIODS.length][maxTaz + 1][];
        nMotorExpUtilities = new double[NMTPERIODS.length][maxTaz + 1][];

        maasExpUtilities = new double[MAASPERIODS.length][maxTaz + 1][];
        
        calculateAverageTazParkingCosts();
        
        tncTaxiWaitTimeCalculator = new TNCAndTaxiWaitTimeCalculator();
        tncTaxiWaitTimeCalculator.createWaitTimeDistributions(rbMap);
        
        calculateAverageMaasWaitTimes();

        

    }

    /**
     * set the utilities values created by another object by calling
     * buildUtilities()
     */
    public void setAllUtilities(double[][][][] ntUtilities)
    {
        this.sovExpUtilities = ntUtilities[0];
        this.hovExpUtilities = ntUtilities[1];
        this.nMotorExpUtilities = ntUtilities[2];
        this.maasExpUtilities = ntUtilities[3];
    }

    /**
     * get the set of utilities arrays built by calling buildUtilities().
     * 
     * @return array of 4 utilities arrays: sovExpUtilities, hovExpUtilities,
     *         nMotorExpUtilities, maasExpUtilities
     */
    public double[][][][] getAllUtilities()
    {
        double[][][][] allUtilities = new double[3][][][];
        allUtilities[0] = sovExpUtilities;
        allUtilities[1] = hovExpUtilities;
        allUtilities[2] = nMotorExpUtilities;
        allUtilities[3] = maasExpUtilities;
        return allUtilities;
    }

    /**
     * set the HashMap of non-motorized utilities, period,oMgra,dMgra (where
     * dMgra is ragged)
     * 
     * @param mgraNMotorExpUtilities
     */
    public void setNonMotorUtilsMap(HashMap<Integer, Double>[][] aMgraNMotorExpUtilities)
    {
        this.mgraNMotorExpUtilities = aMgraNMotorExpUtilities;
    }

    /**
     * get the HashMap of non-motorized utilities, period,oMgra,dMgra (where
     * dMgra is ragged) that was built by calling buildUtilities().
     * 
     * @return mgraNMotorExpUtilities
     */
    public HashMap<Integer, Double>[][] getNonMotorUtilsMap()
    {
        return mgraNMotorExpUtilities;
    }

    /**
     * Build SOV, HOV, and non-motorized exponentiated utilities for all
     * TAZ-pairs. Also builds non-motorized exponentiated utilities for close-in
     * mgra pairs.
     * 
     */
    public void buildUtilities()
    {
    }

    /*
     * public void buildUtilities() {
     * 
     * logger.info("Calculating Non-Transit Zonal Utilities");
     * 
     * // first calculate the Tap-Tap utilities, exponentiate, and store
     * logger.info("Calculating Taz-Taz utilities");
     * 
     * int maxMgra = mgraManager.getMaxMgra(); logger.info("max Mgra " +
     * maxMgra);
     * 
     * mgraNMotorExpUtilities = new HashMap[NMTPERIODS.length][maxMgra + 1];
     * 
     * sovExpUtilities = new double[SOVPERIODS.length][maxTaz + 1][maxTaz + 1];
     * hovExpUtilities = new double[HOVPERIODS.length][maxTaz + 1][maxTaz + 1];
     * nMotorExpUtilities = new double[NMTPERIODS.length][maxTaz + 1][maxTaz +
     * 1];
     * 
     * for (int iTaz = 1; iTaz <= maxTaz; ++iTaz) {
     * 
     * if (iTaz <= 10 || (iTaz % 500) == 0) logger.info("...Origin TAZ " +
     * iTaz);
     * 
     * // calculate the utilities for close-in mgras int[] oMgras =
     * tazManager.getMgraArray(iTaz); if ( oMgras == null ) continue;
     * 
     * for (int oMgra : oMgras) {
     * 
     * // if there are mgras within walking distance if
     * (mgraManager.getMgrasWithinWalkDistanceFrom(oMgra) != null) { int[]
     * dMgras = mgraManager.getMgrasWithinWalkDistanceFrom(oMgra);
     * 
     * int mgraNumber = 0;
     * 
     * // cycle through periods, and calculate utilities for (int period = 0;
     * period < NMTPERIODS.length; ++period) {
     * 
     * mgraNMotorExpUtilities[period][oMgra] = new HashMap<Integer, Double>();
     * 
     * // cycle through the destination mgras for (int dMgra : dMgras) {
     * 
     * double nmtUtility = nMotorUEC[period].calculateUtilitiesForMgraPair(
     * oMgra, dMgra );
     * 
     * // exponentiate the utility if (nmtUtility > -500)
     * mgraNMotorExpUtilities[period][oMgra].put( dMgra, Math.exp(nmtUtility) );
     * ++mgraNumber;
     * 
     * }
     * 
     * }
     * 
     * } }
     * 
     * for (int jTaz = 1; jTaz <= maxTaz; ++jTaz) {
     * 
     * if (seek && !tracer.isTraceZonePair(iTaz, jTaz)) continue;
     * 
     * for (int period = 0; period < SOVPERIODS.length; ++period) {
     * 
     * double sovUtility = sovUEC[period].calculateUtilitiesForTazPair(iTaz,
     * jTaz); // exponentiate the SOV utility if (sovUtility > -500)
     * sovExpUtilities[period][iTaz][jTaz] = Math.exp(sovUtility); }
     * 
     * for (int period = 0; period < HOVPERIODS.length; ++period) {
     * 
     * double hovUtility = hovUEC[period].calculateUtilitiesForTazPair(iTaz,
     * jTaz); // exponentiate the SOV utility if (hovUtility > -500)
     * hovExpUtilities[period][iTaz][jTaz] = Math.exp(hovUtility); }
     * 
     * for (int period = 0; period < NMTPERIODS.length; ++period) {
     * 
     * double nmtUtility = nMotorUEC[period].calculateUtilitiesForTazPair(iTaz,
     * jTaz); // exponentiate the SOV utility if (nmtUtility > -500)
     * nMotorExpUtilities[period][iTaz][jTaz] = Math.exp(nmtUtility); }
     * 
     * } }
     * 
     * }
     */

    /**
     * calculate an average TAZ parking cost to use in accessibilities
     * calculation which are done at TAZ level.
     */
    private void calculateAverageTazParkingCosts()
    {

        avgTazHourlyParkingCost = new double[maxTaz + 1];

        for (int jTaz = 1; jTaz <= maxTaz; ++jTaz)
        {

            int[] mgras = tazManager.getMgraArray(jTaz);
            if (mgras == null || mgras.length == 0) continue;

            double cost = 0;
            int count = 0;
            for (int mgra : mgras)
            {
                float mgraCost = mgraManager.getMgraHourlyParkingCost(mgra);
                if (mgraCost > 0)
                {
                    cost += mgraCost;
                    count++;
                }
                if (count > 0) cost /= count;
            }

            avgTazHourlyParkingCost[jTaz] = cost;

        }

    }

    /**
     * calculate an average TAZ parking cost to use in accessibilities
     * calculation which are done at TAZ level.
     */
    private void calculateAverageMaasWaitTimes()
    {

        avgTazTaxiWaitTime = new float[maxTaz + 1];
        avgTazSingleTNCWaitTime = new float[maxTaz + 1];
        avgTazSharedTNCWaitTime = new float[maxTaz + 1];

        for (int jTaz = 1; jTaz <= maxTaz; ++jTaz)
        {

            int[] mgras = tazManager.getMgraArray(jTaz);
            if (mgras == null || mgras.length == 0) continue;

            float taxiWait = 0;
            float singleTNCWait = 0;
            float sharedTNCWait = 0;
            int singleTNCCount = 0;
            int sharedTNCCount = 0;
            int taxiCount = 0;
            
            
            for (int mgra : mgras)
            {
                float popEmpDen = (float) mgraManager.getPopEmpPerSqMi(mgra);
                float singleTNCWaitTime = (float) tncTaxiWaitTimeCalculator.getMeanSingleTNCWaitTime( popEmpDen);
                float sharedTNCWaitTime = (float) tncTaxiWaitTimeCalculator.getMeanSharedTNCWaitTime( popEmpDen);
                float taxiWaitTime = (float) tncTaxiWaitTimeCalculator.getMeanTaxiWaitTime( popEmpDen);
        
                if (taxiWaitTime > 0)
                {
                	taxiWait += taxiWaitTime;
                	taxiCount++;
                }
                if (singleTNCWaitTime > 0)
                {
                	singleTNCWait += singleTNCWaitTime;
                	singleTNCCount++;
                }
               if (sharedTNCWaitTime > 0)
                {
                	sharedTNCWait += sharedTNCWaitTime;
                	sharedTNCCount++;
                }
                if (taxiCount > 0) taxiWait /= taxiCount;
                if (singleTNCCount > 0) singleTNCWait /= singleTNCCount;
                if (sharedTNCCount > 0) sharedTNCWait /= sharedTNCCount;
            }

            avgTazTaxiWaitTime[jTaz] = taxiWait;
            avgTazSingleTNCWaitTime[jTaz] = singleTNCWait;
            avgTazSharedTNCWaitTime[jTaz] = sharedTNCWait;
         }

    }

    public void buildUtilitiesForOrigMgraAndPeriod(int iMgra, int period)
    {

        int iTaz = mgraManager.getTaz(iMgra);
        if (sovExpUtilities[period][iTaz] != null) return;

        sovExpUtilities[period][iTaz] = new double[maxTaz + 1];
        hovExpUtilities[period][iTaz] = new double[maxTaz + 1];
        maasExpUtilities[period][iTaz] = new double[maxTaz + 1];

        for (int jTaz = 1; jTaz <= maxTaz; ++jTaz)
        {

            double sovUtility = sovUEC[period].calculateUtilitiesForTazPair(iTaz, jTaz,
                    avgTazHourlyParkingCost[jTaz]);
            // exponentiate the SOV utility
            if (sovUtility > -500) sovExpUtilities[period][iTaz][jTaz] = Math.exp(sovUtility);

            double hovUtility = hovUEC[period].calculateUtilitiesForTazPair(iTaz, jTaz,
                    avgTazHourlyParkingCost[jTaz]);
            // exponentiate the HOV utility
            if (hovUtility > -500) hovExpUtilities[period][iTaz][jTaz] = Math.exp(hovUtility);

            double maasUtility = maasUEC[period].calculateUtilitiesForTazPair(iTaz, jTaz,
                    avgTazTaxiWaitTime[jTaz], avgTazSingleTNCWaitTime[jTaz], avgTazSharedTNCWaitTime[jTaz]);
            // exponentiate the SOV utility
            if (maasUtility > -500) maasExpUtilities[period][iTaz][jTaz] = Math.exp(maasUtility);
            
            
        }

        // non-motorized utilities are only needed for off-peak period, so if
        // period index == 1 (peak) no nead to calculate off-peak
        if (nMotorExpUtilities[OFFPEAK_PERIOD_INDEX][iTaz] == null)
        {

            nMotorExpUtilities[OFFPEAK_PERIOD_INDEX][iTaz] = new double[maxTaz + 1];

            for (int jTaz = 1; jTaz <= maxTaz; ++jTaz)
            {

                double nmtUtility = nMotorUEC[OFFPEAK_PERIOD_INDEX].calculateUtilitiesForTazPair(
                        iTaz, jTaz);
                // exponentiate the SOV utility
                if (nmtUtility > -500)
                    nMotorExpUtilities[OFFPEAK_PERIOD_INDEX][iTaz][jTaz] = Math.exp(nmtUtility);

            }

        }

    }

    /**
     * Get the non-motorized exponentiated utility for the mgra-pair and period.
     * This method will return the taz-taz exponentiated non-motorized utility
     * if the mgra-mgra exp utility doesn't exist. Otherwise the mgra-mgra exp.
     * utility will be returned.
     * 
     * @param iMgra
     *            Origin/production mgra.
     * @param jMgra
     *            Destination/attraction mgra.
     * @param period
     *            Period.
     * @return The non-motorized exponentiated utility.
     */
    /*
     * public double getNMotorExpUtility(int iMgra, int jMgra, int period) { //
     * no mgra-mgra utilities for this origin if
     * (mgraNMotorExpUtilities[period][iMgra] == null) { int iTaz =
     * mgraManager.getTaz(iMgra); int jTaz = mgraManager.getTaz(jMgra); return
     * nMotorExpUtilities[period][iTaz][jTaz]; }
     * 
     * // mgra-mgra utilities exist if
     * (mgraNMotorExpUtilities[period][iMgra].containsKey(jMgra)) { return
     * mgraNMotorExpUtilities[period][iMgra].get(jMgra); }
     * 
     * // no mgra-mgra utilities for this destination int iTaz =
     * mgraManager.getTaz(iMgra); int jTaz = mgraManager.getTaz(jMgra); return
     * nMotorExpUtilities[period][iTaz][jTaz]; }
     */
    public double getNMotorExpUtility(int iMgra, int jMgra, int period)
    {

        // if no utilities exist for period and origin mgra, try to compute them
        if (mgraNMotorExpUtilities[period][iMgra] == null)
        {

            // get the mgras within walking distance of the iMgra
            int[] dMgras = mgraManager.getMgrasWithinWalkDistanceFrom(iMgra);

            if (dMgras == null)
            {
                mgraNMotorExpUtilities[period][iMgra] = new HashMap<Integer, Double>(0);
            } else
            {
                mgraNMotorExpUtilities[period][iMgra] = new HashMap<Integer, Double>(dMgras.length);

                // cycle through the destination mgras
                for (int dMgra : dMgras)
                {
                    // calculate utility for the specified mgra and period
                    double nmtUtility = nMotorUEC[period].calculateUtilitiesForMgraPair(iMgra,
                            dMgra);

                    // exponentiate the utility
                    if (nmtUtility > -500)
                        mgraNMotorExpUtilities[period][iMgra].put(dMgra, Math.exp(nmtUtility));
                }

            }

        }

        // if jMgra is in the HashMap, return its utility value
        if (mgraNMotorExpUtilities[period][iMgra].containsKey(jMgra))
            return mgraNMotorExpUtilities[period][iMgra].get(jMgra);

        // otherwise, get exponentiated utilities based on highway skim values
        // for the taz pair associated with iMgra and jMgra.
        int iTaz = mgraManager.getTaz(iMgra);
        int jTaz = mgraManager.getTaz(jMgra);
        return nMotorExpUtilities[period][iTaz][jTaz];

    }

    /**
     * Get the SOV Exponentiated Utility for a given ptaz, ataz, and period
     * 
     * @param pTaz
     *            Production/Origin TAZ
     * @param aTaz
     *            Attraction/Destination TAZ
     * @param period
     *            Period
     * @return SOV Exponentiated Utility.
     */
    public double getSovExpUtility(int pTaz, int aTaz, int period)
    {
        return sovExpUtilities[period][pTaz][aTaz];
    }

    /**
     * Get the HOV Exponentiated Utility for a given ptaz, ataz, and period
     * 
     * @param pTaz
     *            Production/Origin TAZ
     * @param aTaz
     *            Attraction/Destination TAZ
     * @param period
     *            Period
     * @return SOV Exponentiated Utility.
     */
    public double getHovExpUtility(int pTaz, int aTaz, int period)
    {
        return hovExpUtilities[period][pTaz][aTaz];
    }

    /**
     * Get the Maas Exponentiated Utility for a given ptaz, ataz, and period
     * 
     * @param pTaz
     *            Production/Origin TAZ
     * @param aTaz
     *            Attraction/Destination TAZ
     * @param period
     *            Period
     * @return Maas Exponentiated Utility.
     */
    public double getMaasExpUtility(int pTaz, int aTaz, int period)
    {
        return maasExpUtilities[period][pTaz][aTaz];
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
        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        boolean os64bit = false;
        MatrixDataServer matrixServer = null;

        os64bit = Boolean.parseBoolean(Util.getStringValueFromPropertyMap(rbMap,
                "operatingsystem.64bit"));
        if (os64bit)
        {

            String serverAddress = Util.getStringValueFromPropertyMap(rbMap, "server.address");

            int serverPort = Util.getIntegerValueFromPropertyMap(rbMap, "server.port");
            String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

            matrixServer = new MatrixDataServer();

            // bind this concrete object with the cajo library objects for
            // managing
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
                throw new RuntimeException();
            }
        }

        double[][][] sovExpUtilities = null;
        double[][][] hovExpUtilities = null;
        double[][][] nMotorExpUtilities = null;
        double[][][] maasExpUtilities = null;
        
        NonTransitUtilities au = new NonTransitUtilities(rbMap, sovExpUtilities, hovExpUtilities,
                nMotorExpUtilities, maasExpUtilities);
        au.buildUtilities();

    }
}
