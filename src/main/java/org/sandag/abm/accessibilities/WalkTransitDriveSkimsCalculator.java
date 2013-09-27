package org.sandag.abm.accessibilities;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * This class is used to return walk-transit-drive skim values for MGRA pairs
 * associated with estimation data file records.
 * 
 * @author Jim Hicks
 * @version March, 2010
 */
public class WalkTransitDriveSkimsCalculator implements Serializable {

	private transient Logger logger;

	private static final int EA = TransitWalkAccessUEC.EA;
	private static final int AM = TransitWalkAccessUEC.AM;
	private static final int MD = TransitWalkAccessUEC.MD;
	private static final int PM = TransitWalkAccessUEC.PM;
	private static final int EV = TransitWalkAccessUEC.EV;
	private static final int NUM_PERIODS = TransitWalkAccessUEC.NUM_PERIODS;
	private static final String[] PERIODS = TransitWalkAccessUEC.PERIODS;

	private static final int ACCESS_TIME_INDEX = 0;
	private static final int EGRESS_TIME_INDEX = 1;

	private static final String[] LOC_SKIM_NAMES = { "AccTime", "EgrTime",
			"WalkAuxTime", "LB_ivt", "fwait", "xwait", "LB_fare", "xfers" };
	private static final int NUM_LOCAL_SKIMS = LOC_SKIM_NAMES.length;
	private double[] defaultLocalSkims = new double[NUM_LOCAL_SKIMS];

	private static final String[] PREM_SKIM_NAMES = { "AccTime", "EgrTime",
			"WalkAuxTime", "LB_ivt", "EB_ivt", "BRT_ivt", "LRT_ivt", "CR_ivt",
			"fwait", "xwait", "fare", "Main_Mode", "xfers" };
	private static final int NUM_PREMIUM_SKIMS = PREM_SKIM_NAMES.length;
	private double[] defaultPremiumSkims = new double[NUM_PREMIUM_SKIMS];

	private static final int LOC = 1;
	private static final int PREM = 2;
	private static final String[] SERVICE_TYPES = { "", "LOCAL", "PREMIUM" };
	private static final int NUM_SERVICE_TYPES = SERVICE_TYPES.length - 1;

	// declare an array of UEC objects, 1 for each time period
	private UtilityExpressionCalculator[] walkLocalDriveSkimUECs;
	private UtilityExpressionCalculator[] walkPremiumDriveSkimUECs;
	private IndexValues iv;

	// The simple auto skims UEC does not use any DMU variables
	private TransitDriveAccessDMU dmu = new TransitDriveAccessDMU(); // DMU
	// for
	// this
	// UEC

	private BestTransitPathCalculator bestPathUEC;

	private MgraDataManager mgraManager;
	private int maxTap;

	// skim values for transit service type(local, premium),
	// transit ride mode(lbs, ebs, brt, lrt, crl),
	// depart skim period(am, pm, op), and Tap-Tap pair.
	private double[][][][][] storedDepartPeriodTapTapSkims;

	private MatrixDataServerIf ms;

	public WalkTransitDriveSkimsCalculator() {
		mgraManager = MgraDataManager.getInstance();
		maxTap = mgraManager.getMaxTap();

		// point the stored Array of skims: by Prem or Local, DepartPeriod, O
		// tap, D tap, skim values[] to a shared data store
		StoredTransitSkimData storedDataObject = StoredTransitSkimData
				.getInstance(NUM_SERVICE_TYPES, NUM_PERIODS, maxTap);
		storedDepartPeriodTapTapSkims = storedDataObject
				.getStoredWtdDepartPeriodTapTapSkims();
	}

	public void setup(HashMap<String, String> rbMap, Logger aLogger,
			BestTransitPathCalculator myBestPathUEC) {

		logger = aLogger;

		// set the best transit path utility UECs
		bestPathUEC = myBestPathUEC;

		// Create the skim UECs
		String uecPath = Util.getStringValueFromPropertyMap(rbMap,
				CtrampApplication.PROPERTIES_UEC_PATH);
		String uecFileName = uecPath
				+ Util.getStringValueFromPropertyMap(rbMap,
						"skim.walk.transit.drive.uec.file");
		int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.transit.drive.data.page");

		int wtdLocSkimEaPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.drive.ea.page");
		int wtdLocSkimAmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.drive.am.page");
		int wtdLocSkimMdPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.drive.md.page");
		int wtdLocSkimPmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.drive.pm.page");
		int wtdLocSkimEvPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.drive.ev.page");

		int wtdPremSkimEaPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.drive.ea.page");
		int wtdPremSkimAmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.drive.am.page");
		int wtdPremSkimMdPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.drive.md.page");
		int wtdPremSkimPmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.drive.pm.page");
		int wtdPremSkimEvPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.drive.ev.page");

		File uecFile = new File(uecFileName);
		walkLocalDriveSkimUECs = new UtilityExpressionCalculator[NUM_PERIODS + 1];
		walkLocalDriveSkimUECs[EA] = new UtilityExpressionCalculator(uecFile,
				wtdLocSkimEaPage, dataPage, rbMap, dmu);
		walkLocalDriveSkimUECs[AM] = new UtilityExpressionCalculator(uecFile,
				wtdLocSkimAmPage, dataPage, rbMap, dmu);
		walkLocalDriveSkimUECs[MD] = new UtilityExpressionCalculator(uecFile,
				wtdLocSkimMdPage, dataPage, rbMap, dmu);
		walkLocalDriveSkimUECs[PM] = new UtilityExpressionCalculator(uecFile,
				wtdLocSkimPmPage, dataPage, rbMap, dmu);
		walkLocalDriveSkimUECs[EV] = new UtilityExpressionCalculator(uecFile,
				wtdLocSkimEvPage, dataPage, rbMap, dmu);

		walkPremiumDriveSkimUECs = new UtilityExpressionCalculator[NUM_PERIODS + 1];
		walkPremiumDriveSkimUECs[EA] = new UtilityExpressionCalculator(uecFile,
				wtdPremSkimEaPage, dataPage, rbMap, dmu);
		walkPremiumDriveSkimUECs[AM] = new UtilityExpressionCalculator(uecFile,
				wtdPremSkimAmPage, dataPage, rbMap, dmu);
		walkPremiumDriveSkimUECs[MD] = new UtilityExpressionCalculator(uecFile,
				wtdPremSkimMdPage, dataPage, rbMap, dmu);
		walkPremiumDriveSkimUECs[PM] = new UtilityExpressionCalculator(uecFile,
				wtdPremSkimPmPage, dataPage, rbMap, dmu);
		walkPremiumDriveSkimUECs[EV] = new UtilityExpressionCalculator(uecFile,
				wtdPremSkimEvPage, dataPage, rbMap, dmu);

		iv = new IndexValues();

		for (int i = 0; i < NUM_PREMIUM_SKIMS; i++)
			defaultPremiumSkims[i] = -999;

		for (int i = 0; i < NUM_LOCAL_SKIMS; i++)
			defaultLocalSkims[i] = -999;
	}

	/**
	 * Return the array of best walk-transit-drive tap pairs for the given
	 * origin MGRA, destination MGRA, and departure time period.
	 * 
	 * @param origMgra
	 *            Origin MGRA
	 * @param workMgra
	 *            Destination MGRA
	 * @param departPeriod
	 *            Departure time period - 1 = AM period, 2 = PM period, 3 =
	 *            OffPeak period
	 * @param debug
	 *            boolean flag to indicate if debugging reports should be logged
	 * @param logger
	 *            Logger to which debugging reports should be logged if debug is
	 *            tru
	 * @return int[][] Array of best tap pair values - rows are ride modes,
	 *         columns are orig and dest tap, respectively.
	 */
	public int[][] getBestTapPairs(int origMgra, int destMgra,
			int departPeriod, boolean debug, Logger myLogger) {

		String separator = "";
		String header = "";
		if (debug) {
			myLogger.info("");
			myLogger.info("");
			header = "best walk-transit-drive tap pairs debug info for origMgra="
					+ origMgra
					+ ", destMgra="
					+ destMgra
					+ ", period index="
					+ departPeriod + ", period label=" + PERIODS[departPeriod];
			for (int i = 0; i < header.length(); i++)
				separator += "^";

			myLogger.info("");
			myLogger.info(separator);
			myLogger.info("Calculating " + header);
		}

		int[][] bestTaps = null;

		bestPathUEC.findBestWalkTransitDriveTaps(departPeriod, origMgra,
				destMgra, debug, myLogger);

		// log the best tap-tap utilities by ride mode
		double[] bestUtilities = bestPathUEC.getBestUtilities();
		Modes.TransitMode[] mode = Modes.TransitMode.values();
		bestTaps = new int[bestUtilities.length][];
		for (int i = 0; i < bestUtilities.length; i++) {
			if (bestUtilities[i] > -999)
				bestTaps[i] = bestPathUEC.getBestTaps(mode[i]);
		}

		// log the best utilities and tap pairs for each ride mode
		if (debug) {
			myLogger.info("");
			myLogger.info(separator);
			myLogger.info(header);
			myLogger.info("Final Best Utilities:");
			myLogger.info("ModeNumber, Mode, ExpUtility, bestITap, bestJTap, bestAccTime, bestEgrTime");
			int availableModeCount = 0;
			for (int i = 0; i < bestUtilities.length; i++) {
				if (bestTaps[i] != null)
					availableModeCount++;

				myLogger.info(i
						+ ","
						+ mode[i]
						+ ","
						+ bestUtilities[i]
						+ ","
						+ (bestTaps[i] == null ? "NA" : bestTaps[i][0])
						+ ","
						+ (bestTaps[i] == null ? "NA" : bestTaps[i][1])
						+ ","
						+ (bestTaps[i] == null ? "NA" : bestPathUEC
								.getBestAccessTime(i))
						+ ","
						+ (bestTaps[i] == null ? "NA" : bestPathUEC
								.getBestEgressTime(i)));
			}

			myLogger.info(separator);
		}

		return bestTaps;

	}

	/**
	 * Return the array of walk-transit-drive skims for the ride mode, origin
	 * TAP, destination TAP, and departure time period.
	 * 
	 * @param rideModeIndex
	 *            rode mode indices, for which best utilities and best tap pairs
	 *            were determined 0 = CR, 1 = LR, 2 = BRT, 3 = Exp bus, 4 = Loc
	 *            bus
	 * @param origTap
	 *            best Origin TAP for the MGRA pair
	 * @param workTap
	 *            best Destination TAP for the MGRA pair
	 * @param departPeriod
	 *            Departure time period - 1 = AM period, 2 = PM period, 3 =
	 *            OffPeak period
	 * @return Array of 55 skim values for the MGRA pair and departure period
	 */
	public double[] getWalkTransitDriveSkims(int rideModeIndex,
			double pWalkTime, double aDriveTime, int origTap, int destTap,
			int departPeriod, boolean debug) {

		dmu.setMgraTapWalkTime(pWalkTime);
		dmu.setDriveTimeFromTap(aDriveTime);

		iv.setOriginZone(origTap);
		iv.setDestZone(destTap);

		if (Modes.getIsPremiumTransit(rideModeIndex)) {
			// allocate space for the origin tap if it hasn't been allocated
			// already
			if (storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap] == null) {
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap] = new double[maxTap + 1][];
			}

			// if the destTap skims are not already stored, calculate them and
			// store
			// them
			if (storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap] == null) {
				double[] results = walkPremiumDriveSkimUECs[departPeriod]
						.solve(iv, dmu, null);
				if (debug)
					walkPremiumDriveSkimUECs[departPeriod].logAnswersArray(
							logger, "Walk-Premium-Drive Skims");
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap] = results;
			}

			try {
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", pWalkTime="
						+ pWalkTime);
				logger.error(
						"exception setting walk-transit-drive premium walk access time in stored array.",
						e);
			}

			try {
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aDriveTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", aDriveTime="
						+ aDriveTime);
				logger.error(
						"exception setting walk-transit-drive premium drive egress time in stored array.",
						e);
			}
			return storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap];
		} else {
			// allocate space for the origin tap if it hasn't been allocated
			// already
			if (storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap] == null) {
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap] = new double[maxTap + 1][];
			}

			// if the destTap skims are not already stored, calculate them and
			// store
			// them
			if (storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap] == null) {
				double[] results = walkLocalDriveSkimUECs[departPeriod].solve(
						iv, dmu, null);
				if (debug)
					walkLocalDriveSkimUECs[departPeriod].logAnswersArray(
							logger, "Walk-Local-Drive Skims");
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap] = results;
			}

			try {
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", pWalkTime="
						+ pWalkTime);
				logger.error(
						"exception setting walk-transit-drive local walk access time in stored array.",
						e);
			}

			try {
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aDriveTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", aDriveTime="
						+ aDriveTime);
				logger.error(
						"exception setting walk-transit-drive local drive egress time in stored array.",
						e);
			}
			return storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap];
		}

	}

	public double[] getNullTransitSkims(int rideModeIndex) {

		if (Modes.getIsPremiumTransit(rideModeIndex))
			return defaultPremiumSkims;
		else
			return defaultLocalSkims;

	}

	/**
	 * Start the matrix server
	 * 
	 * @param rb
	 *            is a ResourceBundle for the properties file for this
	 *            application
	 */
	private void startMatrixServer(ResourceBundle rb) {

		logger.info("");
		logger.info("");
		String serverAddress = rb.getString("RunModel.MatrixServerAddress");
		int serverPort = new Integer(rb.getString("RunModel.MatrixServerPort"));
		logger.info("connecting to matrix server " + serverAddress + ":"
				+ serverPort);

		try {

			MatrixDataManager mdm = MatrixDataManager.getInstance();
			ms = new MatrixDataServerRmi(serverAddress, serverPort,
					MatrixDataServer.MATRIX_DATA_SERVER_NAME);
			ms.testRemote(Thread.currentThread().getName());
			mdm.setMatrixDataServerObject(ms);

		} catch (Exception e) {

			logger.error(
					String.format("exception caught running ctramp model components -- exiting."),
					e);
			throw new RuntimeException();

		}

	}

	/**
	 * log a report of the final skim values for the MGRA odt
	 * 
	 * @param odt
	 *            is an int[] with the first element the origin mgra and the
	 *            second element the dest mgra and third element the departure
	 *            period index
	 * @param bestTapPairs
	 *            is an int[][] of TAP values with the first dimesion the ride
	 *            mode and second dimension a 2 element array with best orig and
	 *            dest TAP
	 * @param returnedSkims
	 *            is a double[][] of skim values with the first dimesion the
	 *            ride mode indices and second dimention the skim categories
	 */
	public void logReturnedSkims(int[] odt, int[][] bestTapPairs,
			double[][] skims) {

		Modes.TransitMode[] mode = Modes.TransitMode.values();

		int nrows = skims.length;
		int ncols = 0;
		for (int i = 0; i < nrows; i++)
			if (skims[i].length > ncols)
				ncols = skims[i].length;

		String separator = "";
		String header = "";

		logger.info("");
		logger.info("");
		header = "Returned walk-transit-drive skim value tables for origMgra="
				+ odt[0] + ", destMgra=" + odt[1] + ", period index=" + odt[2]
				+ ", period label=" + PERIODS[odt[2]];
		for (int i = 0; i < header.length(); i++)
			separator += "^";

		logger.info(separator);
		logger.info(header);
		logger.info("");

		String modeHeading = String.format("%-12s      %3s      ", "RideMode:",
				mode[0]);
		for (int i = 1; i < bestTapPairs.length; i++)
			modeHeading += String.format("      %3s      ", mode[i]);
		logger.info(modeHeading);

		String tapHeading = String.format("%-12s   %4s-%4s   ", "TAP Pair:",
				bestTapPairs[0] != null ? String.valueOf(bestTapPairs[0][0])
						: "NA",
				bestTapPairs[0] != null ? String.valueOf(bestTapPairs[0][1])
						: "NA");
		for (int i = 1; i < bestTapPairs.length; i++)
			tapHeading += String.format(
					"   %4s-%4s   ",
					bestTapPairs[i] != null ? String
							.valueOf(bestTapPairs[i][0]) : "NA",
					bestTapPairs[i] != null ? String
							.valueOf(bestTapPairs[i][1]) : "NA");
		logger.info(tapHeading);

		String underLine = String.format("%-12s   %9s   ", "---------",
				"---------");
		for (int i = 1; i < bestTapPairs.length; i++)
			underLine += String.format("   %9s   ", "---------");
		logger.info(underLine);

		for (int j = 0; j < ncols; j++) {
			String tableRecord = "";
			if (j < skims[0].length)
				tableRecord = String.format("%-12d %12.5f  ", j + 1,
						skims[0][j]);
			else
				tableRecord = String.format("%-12d %12s  ", j + 1, "");
			for (int i = 1; i < bestTapPairs.length; i++) {
				if (j < skims[i].length)
					tableRecord += String.format(" %12.5f  ", skims[i][j]);
				else
					tableRecord += String.format(" %12s  ", "");
			}
			logger.info(tableRecord);
		}

		logger.info("");
		logger.info(separator);
	}

	public static void main(String[] args) {

		Logger logger = Logger.getLogger(WalkTransitDriveSkimsCalculator.class);

		ResourceBundle rb;
		if (args.length == 0) {
			logger.error(String
					.format("no properties file base name (without .properties extension) was specified as an argument."));
			return;
		} else {
			rb = ResourceBundle.getBundle(args[0]);
		}

		HashMap<String, String> rbMap = ResourceUtil
				.changeResourceBundleIntoHashMap(rb);

		WalkTransitDriveSkimsCalculator wtd = new WalkTransitDriveSkimsCalculator();
		wtd.startMatrixServer(rb);

		McLogsumsCalculator logsumHelper = new McLogsumsCalculator();

		logsumHelper.setupSkimCalculators(rbMap);
		wtd.setup(rbMap, logger, logsumHelper.getBestTransitPathCalculator());

		double[][] returnedSkims = null;
		int[][] testOdts = { { 27, 765, 1 }, { 650, 2000, 2 }, { 100, 200, 3 } };

		for (int[] odt : testOdts) {
			int[][] bestTapPairs = wtd.getBestTapPairs(odt[0], odt[1], odt[2],
					true, logger);
			returnedSkims = new double[bestTapPairs.length][];
			for (int i = 0; i < bestTapPairs.length; i++) {
				if (bestTapPairs[i] == null)
					returnedSkims[i] = wtd.getNullTransitSkims(i);
				else {
					returnedSkims[i] = wtd.getWalkTransitDriveSkims(i,
							logsumHelper.getBestTransitPathCalculator()
									.getBestAccessTime(i), logsumHelper
									.getBestTransitPathCalculator()
									.getBestEgressTime(i), bestTapPairs[i][0],
							bestTapPairs[i][1], odt[2], true);
				}
			}

			wtd.logReturnedSkims(odt, bestTapPairs, returnedSkims);
		}

	}

	public int getNumSkimPeriods() {
		return NUM_PERIODS;
	}

	public int getNumLocalSkims() {
		return NUM_LOCAL_SKIMS;
	}

	public String[] getLocalSkimNames() {
		return LOC_SKIM_NAMES;
	}

	public int getNumPremiumSkims() {
		return NUM_PREMIUM_SKIMS;
	}

	public String[] getPremiumSkimNames() {
		return PREM_SKIM_NAMES;
	}

}
