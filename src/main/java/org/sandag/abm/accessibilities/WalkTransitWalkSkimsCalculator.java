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
import org.sandag.abm.modechoice.TransitWalkAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * This class is used to return walk-transit-walk skim values for MGRA pairs
 * associated with estimation data file records.
 * 
 * @author Jim Hicks
 * @version March, 2010
 */
public class WalkTransitWalkSkimsCalculator implements Serializable {

	private transient Logger logger;

	private static final int EA = TransitWalkAccessUEC.EA;
	private static final int AM = TransitWalkAccessUEC.AM;
	private static final int MD = TransitWalkAccessUEC.MD;
	private static final int PM = TransitWalkAccessUEC.PM;
	private static final int EV = TransitWalkAccessUEC.EV;
	private static final String[] PERIODS = TransitWalkAccessUEC.PERIODS;
	private static final int NUM_PERIODS = TransitWalkAccessUEC.NUM_PERIODS;

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
	private UtilityExpressionCalculator[] walkLocalWalkSkimUECs;
	private UtilityExpressionCalculator[] walkPremiumWalkSkimUECs;
	private IndexValues iv;

	// The simple auto skims UEC does not use any DMU variables
	private TransitWalkAccessDMU dmu = new TransitWalkAccessDMU();
	// DMU
	// for
	// this
	// UEC

	private MgraDataManager mgraManager;
	private int maxTap;

	// skim values for transit service type(local, premium),
	// transit ride mode(lbs, ebs, brt, lrt, crl),
	// depart skim period(am, pm, op), and Tap-Tap pair.
	private double[][][][][] storedDepartPeriodTapTapSkims;

	private BestTransitPathCalculator bestPathUEC;

	private MatrixDataServerIf ms;

	public WalkTransitWalkSkimsCalculator() {
		mgraManager = MgraDataManager.getInstance();
		maxTap = mgraManager.getMaxTap();

		// point the stored Array of skims: by Prem or Local, DepartPeriod, O
		// tap, D tap, skim values[] to a shared data store
		StoredTransitSkimData storedDataObject = StoredTransitSkimData
				.getInstance(NUM_SERVICE_TYPES, NUM_PERIODS, maxTap);
		storedDepartPeriodTapTapSkims = storedDataObject
				.getStoredWtwDepartPeriodTapTapSkims();
	}

	public void setup(HashMap<String, String> rbMap, Logger aLogger,
			BestTransitPathCalculator myBestPathUEC) {

		logger = aLogger;

		// Create the utility UECs
		bestPathUEC = myBestPathUEC;

		// Create the skim UECs
		int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.transit.walk.data.page");

		int wtwLocSkimEaPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.walk.ea.page");
		int wtwLocSkimAmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.walk.am.page");
		int wtwLocSkimMdPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.walk.md.page");
		int wtwLocSkimPmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.walk.pm.page");
		int wtwLocSkimEvPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.local.walk.ev.page");

		int wtwPremSkimEaPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.walk.ea.page");
		int wtwPremSkimAmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.walk.am.page");
		int wtwPremSkimMdPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.walk.md.page");
		int wtwPremSkimPmPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.walk.pm.page");
		int wtwPremSkimEvPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"skim.walk.premium.walk.ev.page");

		String uecPath = Util.getStringValueFromPropertyMap(rbMap,
				CtrampApplication.PROPERTIES_UEC_PATH);
		String uecFileName = uecPath
				+ Util.getStringValueFromPropertyMap(rbMap,
						"skim.walk.transit.walk.uec.file");
		File uecFile = new File(uecFileName);

		walkLocalWalkSkimUECs = new UtilityExpressionCalculator[NUM_PERIODS];
		walkLocalWalkSkimUECs[EA] = new UtilityExpressionCalculator(uecFile,
				wtwLocSkimEaPage, dataPage, rbMap, dmu);
		walkLocalWalkSkimUECs[AM] = new UtilityExpressionCalculator(uecFile,
				wtwLocSkimAmPage, dataPage, rbMap, dmu);
		walkLocalWalkSkimUECs[MD] = new UtilityExpressionCalculator(uecFile,
				wtwLocSkimMdPage, dataPage, rbMap, dmu);
		walkLocalWalkSkimUECs[PM] = new UtilityExpressionCalculator(uecFile,
				wtwLocSkimPmPage, dataPage, rbMap, dmu);
		walkLocalWalkSkimUECs[EV] = new UtilityExpressionCalculator(uecFile,
				wtwLocSkimEvPage, dataPage, rbMap, dmu);

		walkPremiumWalkSkimUECs = new UtilityExpressionCalculator[NUM_PERIODS];
		walkPremiumWalkSkimUECs[EA] = new UtilityExpressionCalculator(uecFile,
				wtwPremSkimEaPage, dataPage, rbMap, dmu);
		walkPremiumWalkSkimUECs[AM] = new UtilityExpressionCalculator(uecFile,
				wtwPremSkimAmPage, dataPage, rbMap, dmu);
		walkPremiumWalkSkimUECs[MD] = new UtilityExpressionCalculator(uecFile,
				wtwPremSkimMdPage, dataPage, rbMap, dmu);
		walkPremiumWalkSkimUECs[PM] = new UtilityExpressionCalculator(uecFile,
				wtwPremSkimPmPage, dataPage, rbMap, dmu);
		walkPremiumWalkSkimUECs[EV] = new UtilityExpressionCalculator(uecFile,
				wtwPremSkimEvPage, dataPage, rbMap, dmu);

		iv = new IndexValues();

		for (int i = 0; i < NUM_PREMIUM_SKIMS; i++)
			defaultPremiumSkims[i] = -999;

		for (int i = 0; i < NUM_LOCAL_SKIMS; i++)
			defaultLocalSkims[i] = -999;
	}

	/**
	 * Return the array of walk-transit best tap pairs for the given origin
	 * MGRA, destination MGRA, and departure time period.
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
			int departPeriod, boolean debug, Logger aLogger) {

		String separator = "";
		String header = "";
		if (debug) {
			aLogger.info("");
			aLogger.info("");
			header = "walk-transit-walk best tap pairs debug info for origMgra="
					+ origMgra
					+ ", destMgra="
					+ destMgra
					+ ", period index="
					+ departPeriod + ", period label=" + PERIODS[departPeriod];
			for (int i = 0; i < header.length(); i++)
				separator += "^";

			aLogger.info("");
			aLogger.info(separator);
			aLogger.info("Calculating " + header);
		}

		int[][] bestTaps = null;

		bestPathUEC.findBestWalkTransitWalkTaps(departPeriod, origMgra,
				destMgra, debug, aLogger);

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
			aLogger.info("");
			aLogger.info(separator);
			aLogger.info(header);
			aLogger.info("Final Best Utilities:");
			aLogger.info("ModeNumber, Mode, ExpUtility, bestITap, bestJTap, bestAccTime, bestEgrTime");
			int availableModeCount = 0;
			for (int i = 0; i < bestUtilities.length; i++) {
				if (bestTaps[i] != null)
					availableModeCount++;

				aLogger.info(i
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

			aLogger.info(separator);
		}

		return bestTaps;

	}

	/**
	 * Return the array of walk-transit skims for the ride mode, origin TAP,
	 * destination TAP, and departure time period.
	 * 
	 * @param rideModeIndex
	 *            rode mode indices, for which best utilities and best tap pairs
	 *            were determined 0 = CR, 1 = LR, 2 = BRT, 3 = Exp bus, 4 = Loc
	 *            bus
	 * @param origTap
	 *            best Origin TAP for the MGRA pair
	 * @param destTap
	 *            best Destination TAP for the MGRA pair
	 * @param departPeriod
	 *            skim period index for the departure period - 0 = AM period, 1
	 *            = PM period, 2 = OffPeak period
	 * @return Array of skim values for the MGRA pair and departure period for
	 *         the ride mode type - local or premium
	 */
	public double[] getWalkTransitWalkSkims(int rideModeIndex,
			double pWalkTime, double aWalkTime, int origTap, int destTap,
			int departPeriod, boolean debug) {

		dmu.setMgraTapWalkTime(pWalkTime);
		dmu.setTapMgraWalkTime(aWalkTime);

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
				double[] results = walkPremiumWalkSkimUECs[departPeriod].solve(
						iv, dmu, null);
				if (debug)
					walkPremiumWalkSkimUECs[departPeriod].logAnswersArray(
							logger, "Walk Tap-Tap Premium Skims");
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap] = results;
			}

			try {
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", pWalkTime="
						+ pWalkTime);
				logger.error(
						"exception setting walk-transit-walk premium walk access time in stored array.",
						e);
			}

			try {
				storedDepartPeriodTapTapSkims[PREM][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", aWalkTime="
						+ aWalkTime);
				logger.error(
						"exception setting walk-transit-walk premium walk egress time in stored array.",
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
				double[] results = walkLocalWalkSkimUECs[departPeriod].solve(
						iv, dmu, null);
				if (debug)
					walkLocalWalkSkimUECs[departPeriod].logAnswersArray(logger,
							"Walk Tap-Tap Local Skims");
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap] = results;
			}

			try {
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap][ACCESS_TIME_INDEX] = pWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", pWalkTime="
						+ pWalkTime);
				logger.error(
						"exception setting walk-transit-walk local walk access time in stored array.",
						e);
			}

			try {
				storedDepartPeriodTapTapSkims[LOC][departPeriod][origTap][destTap][EGRESS_TIME_INDEX] = aWalkTime;
			} catch (Exception e) {
				logger.error("departPeriod=" + departPeriod + ", origTap="
						+ origTap + ", destTap=" + destTap + ", aWalkTime="
						+ aWalkTime);
				logger.error(
						"exception setting walk-transit-walk local walk egress time in stored array.",
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
		header = "Returned walktransit skim value tables for origMgra="
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

		Logger logger = Logger.getLogger(WalkTransitWalkSkimsCalculator.class);

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

		WalkTransitWalkSkimsCalculator wtw = new WalkTransitWalkSkimsCalculator();
		wtw.startMatrixServer(rb);

		McLogsumsCalculator logsumHelper = new McLogsumsCalculator();

		logsumHelper.setupSkimCalculators(rbMap);
		wtw.setup(rbMap, logger, logsumHelper.getBestTransitPathCalculator());

		double[][] returnedSkims = null;
		int[][] testOdts = { { 27, 765, 1 }, { 650, 2000, 2 }, { 100, 200, 3 } };

		for (int[] odt : testOdts) {
			int[][] bestTapPairs = wtw.getBestTapPairs(odt[0], odt[1], odt[2],
					true, logger);
			returnedSkims = new double[bestTapPairs.length][];
			for (int i = 0; i < bestTapPairs.length; i++) {
				if (bestTapPairs[i] == null) {
					returnedSkims[i] = wtw.getNullTransitSkims(i);
				} else {
					returnedSkims[i] = wtw.getWalkTransitWalkSkims(i,
							logsumHelper.getBestTransitPathCalculator()
									.getBestAccessTime(i), logsumHelper
									.getBestTransitPathCalculator()
									.getBestAccessTime(i), bestTapPairs[i][0],
							bestTapPairs[i][1], odt[2], true);
				}
			}

			wtw.logReturnedSkims(odt, bestTapPairs, returnedSkims);
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
