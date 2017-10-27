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
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;

/**
 * This class is used to return walk-transit-drive skim values for MGRA pairs
 * associated with estimation data file records.
 * 
 * @author Jim Hicks
 * @version March, 2010
 */
public class WalkTransitDriveSkimsCalculator
        implements Serializable
{

    private transient Logger                        logger;

    private static final int              EA                            = TransitWalkAccessUEC.EA;
    private static final int              AM                            = TransitWalkAccessUEC.AM;
    private static final int              MD                            = TransitWalkAccessUEC.MD;
    private static final int              PM                            = TransitWalkAccessUEC.PM;
    private static final int              EV                            = TransitWalkAccessUEC.EV;
    private static final int              NUM_PERIODS                   = TransitWalkAccessUEC.NUM_PERIODS;
    private static final String[]         PERIODS                       = TransitWalkAccessUEC.PERIODS;

    private static final int              ACCESS_TIME_INDEX             = 0;
    private static final int              EGRESS_TIME_INDEX             = 1;
    private static final int              NA                            = -999;

    private int 						  maxWTDSkimSets                = 5;
    private int[]                         NUM_SKIMS;
    private double[]                      defaultSkims;

    // declare UEC object
    private UtilityExpressionCalculator   walkDriveSkimUEC;
    private IndexValues                   iv;

    // The simple auto skims UEC does not use any DMU variables
    private TransitDriveAccessDMU         dmu                           = new TransitDriveAccessDMU();                             // DMU
    // for
    // this
    // UEC

    private BestTransitPathCalculator     bestPathUEC;

    private MgraDataManager               mgraManager;
    private int                           maxTap;
    
    private String[] skimNames;

    // skim values for transit service type(local, premium),
    // transit ride mode(lbs, ebs, brt, lrt, crl),
    // depart skim period(am, pm, op), and Tap-Tap pair.
    private double[][][][][] storedDepartPeriodTapTapSkims;

    private MatrixDataServerIf            ms;

    public WalkTransitDriveSkimsCalculator(HashMap<String, String> rbMap)
    {
        mgraManager = MgraDataManager.getInstance();
        maxTap = mgraManager.getMaxTap();
    }

    public void setup(HashMap<String, String> rbMap, Logger aLogger,
            BestTransitPathCalculator myBestPathUEC)
    {

        logger = aLogger;

        // set the best transit path utility UECs
        bestPathUEC = myBestPathUEC;

        // Create the skim UECs
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,"skim.walk.transit.drive.data.page");
        int skimPage = Util.getIntegerValueFromPropertyMap(rbMap,"skim.walk.transit.drive.skim.page");
        int wtdNumSkims = Util.getIntegerValueFromPropertyMap(rbMap, "skim.walk.transit.drive.skims");
        String uecPath = Util.getStringValueFromPropertyMap(rbMap, CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = Paths.get(uecPath,Util.getStringValueFromPropertyMap(rbMap, "skim.walk.transit.drive.uec.file")).toString();
        File uecFile = new File(uecFileName);
        walkDriveSkimUEC = new UtilityExpressionCalculator(uecFile, skimPage, dataPage, rbMap, dmu);	

        skimNames = walkDriveSkimUEC.getAlternativeNames();

        //setup index values
        iv = new IndexValues();

        //setup default skim values
        defaultSkims = new double[wtdNumSkims];
        for (int j = 0; j < wtdNumSkims; j++) {
          defaultSkims[j] = NA;
        }
        
        // point the stored Array of skims: by Prem or Local, DepartPeriod, O tap, D tap, skim values[] to a shared data store
        StoredTransitSkimData storedDataObject = StoredTransitSkimData.getInstance( maxWTDSkimSets, NUM_PERIODS, maxTap );
        storedDepartPeriodTapTapSkims = storedDataObject.getStoredWtdDepartPeriodTapTapSkims();
    }

   

    /**
     * Return the array of walk-transit-drive skims for the ride mode, origin TAP,
     * destination TAP, and departure time period.
     * 
     * @param set for set source skims
     * @param origTap best Origin TAP for the MGRA pair
     * @param workTap best Destination TAP for the MGRA pair
     * @param departPeriod Departure time period - 1 = AM period, 2 = PM period, 3 =
     *            OffPeak period
     * @return Array of 55 skim values for the MGRA pair and departure period
     */
    public double[] getWalkTransitDriveSkims(int set, double pWalkTime, double aDriveTime, int origTap, int destTap, int departPeriod, boolean debug)
    {

        dmu.setMgraTapWalkTime(pWalkTime);
        dmu.setDriveTimeFromTap(aDriveTime);

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
            double[] results = walkDriveSkimUEC.solve(iv, dmu, null);
            if (debug)
            	walkDriveSkimUEC.logAnswersArray(logger, "Walk-Drive Skims");
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap] = results;
        }

        try {
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
        }
        catch ( Exception e ) {
            logger.error ("departPeriod=" + departPeriod + ", origTap=" + origTap + ", destTap=" + destTap + ", pWalkTime=" + pWalkTime);
            logger.error ("exception setting walk-transit-drive walk access time in stored array.", e);
        }

        try {
            storedDepartPeriodTapTapSkims[set][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aDriveTime;
        }
        catch ( Exception e ) {
            logger.error ("departPeriod=" + departPeriod + ", origTap=" + origTap + ", destTap=" + destTap + ", aDriveTime=" + aDriveTime);
            logger.error ("exception setting walk-transit-drive drive egress time in stored array.", e);
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
            ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
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

        Modes.TransitMode[] mode = Modes.TransitMode.values();

        int nrows = skims.length;
        int ncols = 0;
        for (int i = 0; i < nrows; i++)
            if (skims[i].length > ncols) ncols = skims[i].length;

        String separator = "";
        String header = "";

        logger.info("");
        logger.info("");
        header = "Returned walk-transit-drive skim value tables for origMgra=" + odt[0]
                + ", destMgra=" + odt[1] + ", period index=" + odt[2] + ", period label="
                + PERIODS[odt[2]];
        for (int i = 0; i < header.length(); i++)
            separator += "^";

        logger.info(separator);
        logger.info(header);
        logger.info("");

        String modeHeading = String.format("%-12s      %3s      ", "RideMode:", mode[0]);
        for (int i = 1; i < bestTapPairs.length; i++)
            modeHeading += String.format("      %3s      ", mode[i]);
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

  

}
