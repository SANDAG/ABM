package org.sandag.abm.accessibilities;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.newmodel.UtilityExpressionCalculator;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;

/**
 * This class is used to return walk-transit-walk skim values for MGRA pairs
 * associated with estimation data file records.
 * 
 * @author Jim Hicks
 * @version March, 2010
 */
public class WalkTransitWalkSkimsCalculator
        implements Serializable
{

    private transient Logger                        logger;

    private static final int              EA                            = ModelStructure.EA_SKIM_PERIOD_INDEX;
    private static final int              AM                            = ModelStructure.AM_SKIM_PERIOD_INDEX;
    private static final int              MD                            = ModelStructure.MD_SKIM_PERIOD_INDEX;
    private static final int              PM                            = ModelStructure.PM_SKIM_PERIOD_INDEX;
    private static final int              EV                            = ModelStructure.EV_SKIM_PERIOD_INDEX;
    public static final int              NUM_PERIODS                   = ModelStructure.SKIM_PERIOD_INDICES.length;
    private static final String[]         PERIODS                = ModelStructure.SKIM_PERIOD_STRINGS;

    private static final int              ACCESS_TIME_INDEX             = 0;
    private static final int              EGRESS_TIME_INDEX             = 1;
    private static final int              NA                            = -999;

    private int 						  maxWTWSkimSets                = 5;
    private int[]                         NUM_SKIMS;
    private double[]                      defaultSkims;
    
    // declare UEC object
    private UtilityExpressionCalculator   walkWalkSkimUEC;
    private IndexValues                   iv;

    private String[] skimNames;
    
    // The simple auto skims UEC does not use any DMU variables
    private TransitWalkAccessDMU          dmu                           = new TransitWalkAccessDMU();
    // DMU
    // for
    // this
    // UEC

    private MgraDataManager               mgraManager;
    private int                           maxTap;

    // skim values for transit skim set
    // depart skim period(am, pm, op)
    // and Tap-Tap pair.
    private double[][][][][] storedDepartPeriodTapTapSkims;

    private BestTransitPathCalculator     bestPathUEC;

    private MatrixDataServerIf            ms;

    public WalkTransitWalkSkimsCalculator(HashMap<String, String> rbMap)
    {
        mgraManager = MgraDataManager.getInstance();
        maxTap = mgraManager.getMaxTap();
    }

    public void setup(HashMap<String, String> rbMap, Logger aLogger, BestTransitPathCalculator myBestPathUEC)
    {

        logger = aLogger;

        // Create the utility UECs
        bestPathUEC = myBestPathUEC;

        // Create the skim UECs
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,"skim.walk.transit.walk.data.page");
        int skimPage = Util.getIntegerValueFromPropertyMap(rbMap,"skim.walk.transit.walk.skim.page");
        int wtwNumSkims = Util.getIntegerValueFromPropertyMap(rbMap, "skim.walk.transit.walk.skims");
        String uecPath = Util.getStringValueFromPropertyMap(rbMap, CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = Paths.get(uecPath,Util.getStringValueFromPropertyMap(rbMap, "skim.walk.transit.walk.uec.file")).toString();
        File uecFile = new File(uecFileName);
        walkWalkSkimUEC = new UtilityExpressionCalculator(uecFile, skimPage, dataPage, rbMap, dmu);	

        //setup index values
        iv = new IndexValues();
        
        //setup default skim values
        defaultSkims = new double[wtwNumSkims];
        for (int j = 0; j < wtwNumSkims; j++) {
          defaultSkims[j] = NA;
        }
        
        skimNames = walkWalkSkimUEC.getAlternativeNames();
        
        // point the stored Array of skims: skim set, period, O tap, D tap, skim values[] to a shared data store
        StoredTransitSkimData storedDataObject = StoredTransitSkimData.getInstance( maxWTWSkimSets, NUM_PERIODS, maxTap );
        storedDepartPeriodTapTapSkims = storedDataObject.getStoredWtwDepartPeriodTapTapSkims();
    
    }

    

    /**
     * Return the array of walk-transit skims for the ride mode, origin TAP,
     * destination TAP, and departure time period.
     * 
     * @param set for set source skims
     * @param origTap best Origin TAP for the MGRA pair
     * @param destTap best Destination TAP for the MGRA pair
     * @param departPeriod skim period index for the departure period - 0 = AM
     *            period, 1 = PM period, 2 = OffPeak period
     * @return Array of skim values for the MGRA pair and departure period for the
     *         skim set
     */
    public double[] getWalkTransitWalkSkims(int set, double pWalkTime, double aWalkTime, int origTap, int destTap,
            int departPeriod, boolean debug)
    {

        dmu.setMgraTapWalkTime(pWalkTime);
        dmu.setTapMgraWalkTime(aWalkTime);

        iv.setOriginZone(origTap);
        iv.setDestZone(destTap);

        // allocate space for the origin tap if it hasn't been allocated already
        if (storedDepartPeriodTapTapSkims[set][departPeriod][origTap] == null)
        {
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap] = new double[maxTap + 1][];
        }

        // if the destTap skims are not already stored, calculate them and store
        // them
        if (storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap] == null)
        {
        	dmu.setTOD(departPeriod);
        	dmu.setSet(set);
        	double[] results = walkWalkSkimUEC.solve(iv, dmu, null);
            if (debug)
            	walkWalkSkimUEC.logAnswersArray(logger, "Walk-Walk Tap-Tap Skims");
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap] = results;
        }

        try {
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
        }
        catch ( Exception e ) {
            logger.error ("departPeriod=" + departPeriod + ", origTap=" + origTap + ", destTap=" + destTap + ", pWalkTime=" + pWalkTime);
            logger.error ("exception setting walk-transit-walk walk access time in stored array.", e);
        }

        try {
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aWalkTime;
        }
        catch ( Exception e ) {
            logger.error ("departPeriod=" + departPeriod + ", origTap=" + origTap + ", destTap=" + destTap + ", aWalkTime=" + aWalkTime);
            logger.error ("exception setting walk-transit-walk walk egress time in stored array.", e);
        }
        return storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap];
         

    }

    public double[] getNullTransitSkims()
    {
        return defaultSkims;
    }

    /**
     * Start the matrix server
     * 
     * @param rb is a ResourceBundle for the properties file for this application
     */
    private void startMatrixServer(ResourceBundle rb)
    {

        logger.info("");
        logger.info("");
        String serverAddress = rb.getString("RunModel.MatrixServerAddress");
        int serverPort = new Integer(rb.getString("RunModel.MatrixServerPort"));
        logger.info("connecting to matrix server " + serverAddress + ":" + serverPort);

        try
        {

            MatrixDataManager mdm = MatrixDataManager.getInstance();
            ms = new MatrixDataServerRmi(serverAddress, serverPort,
                    MatrixDataServer.MATRIX_DATA_SERVER_NAME);
            ms.testRemote(Thread.currentThread().getName());
            mdm.setMatrixDataServerObject(ms);

        } catch (Exception e)
        {

            logger.error(String
                    .format("exception caught running ctramp model components -- exiting."), e);
            throw new RuntimeException();

        }

    }

    /**
     * log a report of the final skim values for the MGRA odt
     * 
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
     * @param bestTapPairs is an int[][] of TAP values with the first dimesion the
     *            ride mode and second dimension a 2 element array with best orig and
     *            dest TAP
     * @param returnedSkims is a double[][] of skim values with the first dimesion
     *            the ride mode indices and second dimention the skim categories
     */
    public void logReturnedSkims(int[] odt, int[][] bestTapPairs, double[][] skims)
    {

        int nrows = skims.length;
        int ncols = 0;
        for (int i = 0; i < nrows; i++)
            if (skims[i].length > ncols) ncols = skims[i].length;

        String separator = "";
        String header = "";

        logger.info("");
        logger.info("");
        header = "Returned walktransit skim value tables for origMgra=" + odt[0] + ", destMgra="
                + odt[1] + ", period index=" + odt[2] + ", period label=" + PERIODS[odt[2]];
        for (int i = 0; i < header.length(); i++)
            separator += "^";

        logger.info(separator);
        logger.info(header);
        logger.info("");

        String modeHeading = String.format("%-12s      %3s      ", "Alt:");
        for (int i = 1; i < bestTapPairs.length; i++)
            modeHeading += String.format("      %3s      ", i);
        logger.info(modeHeading);

        String tapHeading = String.format("%-12s   %4s-%4s   ", "TAP Pair:",
                bestTapPairs[0] != null ? String.valueOf(bestTapPairs[0][0]) : "NA",
                bestTapPairs[0] != null ? String.valueOf(bestTapPairs[0][1]) : "NA");
        for (int i = 1; i < bestTapPairs.length; i++)
            tapHeading += String.format("   %4s-%4s   ", bestTapPairs[i] != null ? String
                    .valueOf(bestTapPairs[i][0]) : "NA", bestTapPairs[i] != null ? String
                    .valueOf(bestTapPairs[i][1]) : "NA");
        logger.info(tapHeading);

        String underLine = String.format("%-12s   %9s   ", "---------", "---------");
        for (int i = 1; i < bestTapPairs.length; i++)
            underLine += String.format("   %9s   ", "---------");
        logger.info(underLine);

        for (int j = 0; j < ncols; j++)
        {
            String tableRecord = "";
            if (j < skims[0].length) tableRecord = String.format("%-12d %12.5f  ", j + 1,
                    skims[0][j]);
            else tableRecord = String.format("%-12d %12s  ", j + 1, "");
            for (int i = 1; i < bestTapPairs.length; i++)
            {
                if (j < skims[i].length) tableRecord += String.format(" %12.5f  ", skims[i][j]);
                else tableRecord += String.format(" %12s  ", "");
            }
            logger.info(tableRecord);
        }

        logger.info("");
        logger.info(separator);
    }

	public String[] getSkimNames() {
		return skimNames;
	}

	public BestTransitPathCalculator getBestPathUEC() {
		return bestPathUEC;
	}

 

}
