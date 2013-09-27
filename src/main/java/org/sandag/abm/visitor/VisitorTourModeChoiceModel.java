package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;

public class VisitorTourModeChoiceModel implements Serializable {

	private transient Logger logger = Logger.getLogger("visitorModel");

	public static final boolean DEBUG_BEST_PATHS = false;

	protected static final int LB = McLogsumsCalculator.LB;
	protected static final int EB = McLogsumsCalculator.EB;
	protected static final int BRT = McLogsumsCalculator.BRT;
	protected static final int LR = McLogsumsCalculator.LR;
	protected static final int CR = McLogsumsCalculator.CR;
	protected static final int NUM_LOC_PREM = McLogsumsCalculator.NUM_LOC_PREM;

	protected static final int WTW = McLogsumsCalculator.WTW;
	protected static final int WTD = McLogsumsCalculator.WTD;
	protected static final int DTW = McLogsumsCalculator.DTW;
	protected static final int NUM_ACC_EGR = McLogsumsCalculator.NUM_ACC_EGR;

	protected static final int LB_IVT = McLogsumsCalculator.LB_IVT;
	protected static final int EB_IVT = McLogsumsCalculator.EB_IVT;
	protected static final int BRT_IVT = McLogsumsCalculator.BRT_IVT;
	protected static final int LR_IVT = McLogsumsCalculator.LR_IVT;
	protected static final int CR_IVT = McLogsumsCalculator.CR_IVT;
	protected static final int ACC = McLogsumsCalculator.ACC;
	protected static final int EGR = McLogsumsCalculator.EGR;
	protected static final int AUX = McLogsumsCalculator.AUX;
	protected static final int FWAIT = McLogsumsCalculator.FWAIT;
	protected static final int XWAIT = McLogsumsCalculator.XWAIT;
	protected static final int FARE = McLogsumsCalculator.FARE;
	protected static final int XFERS = McLogsumsCalculator.XFERS;
	protected static final int NUM_SKIMS = McLogsumsCalculator.NUM_SKIMS;

	protected static final int OUT = McLogsumsCalculator.OUT;
	protected static final int IN = McLogsumsCalculator.IN;
	protected static final int NUM_DIR = McLogsumsCalculator.NUM_DIR;
	private MgraDataManager mgraManager;

	private static final String PROPERTIES_UEC_TOUR_MODE_CHOICE = "visitor.mc.uec.file";
	private static final String PROPERTIES_UEC_TOUR_DATA_SHEET = "visitor.mc.data.page";
	private static final String PROPERTIES_UEC_TOUR_MODEL_SHEET = "visitor.mc.model.page";

	private ChoiceModelApplication mcModel;
	private VisitorTourModeChoiceDMU mcDmuObject;
	private McLogsumsCalculator logsumsCalculator;

	private VisitorModelStructure modelStructure;

	private String tourCategory;

	private String[] modeAltNames;

	private boolean saveUtilsProbsFlag = false;

	/**
	 * Constructor.
	 * 
	 * @param propertyMap
	 * @param myModelStructure
	 * @param dmuFactory
	 * @param myLogsumHelper
	 */
	public VisitorTourModeChoiceModel(HashMap<String, String> propertyMap,
			VisitorModelStructure myModelStructure,
			VisitorDmuFactoryIf dmuFactory, McLogsumsCalculator myLogsumHelper) {

		mgraManager = MgraDataManager.getInstance(propertyMap);
		modelStructure = myModelStructure;
		logsumsCalculator = myLogsumHelper;

		mcDmuObject = dmuFactory.getVisitorTourModeChoiceDMU();
		setupModeChoiceModelApplicationArray(propertyMap);

	}

	/**
	 * Set up the mode choice model.
	 * 
	 * @param propertyMap
	 */
	private void setupModeChoiceModelApplicationArray(
			HashMap<String, String> propertyMap) {

		logger.info(String.format("setting up visitor tour mode choice model."));

		// locate the individual mandatory tour mode choice model UEC
		String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
		String mcUecFile = Util.getStringValueFromPropertyMap(propertyMap,
				PROPERTIES_UEC_TOUR_MODE_CHOICE);
		mcUecFile = uecPath + mcUecFile;

		logger.info("Will read mcUECFile " + mcUecFile);
		int dataPage = new Integer(Util.getStringValueFromPropertyMap(
				propertyMap, PROPERTIES_UEC_TOUR_DATA_SHEET));
		int modelPage = new Integer(Util.getStringValueFromPropertyMap(
				propertyMap, PROPERTIES_UEC_TOUR_MODEL_SHEET));

		// default is to not save the tour mode choice utils and probs for each
		// tour
		String saveUtilsProbsString = propertyMap
				.get(CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS);
		if (saveUtilsProbsString != null) {
			if (saveUtilsProbsString.equalsIgnoreCase("true"))
				saveUtilsProbsFlag = true;
		}

		mcModel = new ChoiceModelApplication(mcUecFile, modelPage, dataPage,
				propertyMap, (VariableTable) mcDmuObject);
		modeAltNames = mcModel.getAlternativeNames();

	}

	public double getModeChoiceLogsum(VisitorTour tour, Logger modelLogger,
			String choiceModelDescription, String decisionMakerLabel) {

		setDmuAttributes(tour);

		// log headers to traceLogger
		if (tour.getDebugChoiceModels()) {
			mcModel.choiceModelUtilityTraceLoggerHeading(
					choiceModelDescription, decisionMakerLabel);
		}

		double logsum = logsumsCalculator.calculateTourMcLogsum(
				tour.getOriginMGRA(), tour.getDestinationMGRA(),
				tour.getDepartTime(), tour.getArriveTime(), mcModel,
				mcDmuObject);

		// write UEC calculation results to separate model specific log file
		if (tour.getDebugChoiceModels()) {
			String loggingHeader = String.format("%s   %s",
					choiceModelDescription, decisionMakerLabel);
			mcModel.logUECResults(modelLogger, loggingHeader);
			modelLogger.info(choiceModelDescription + " Logsum value: "
					+ logsum);
			modelLogger.info("");
			modelLogger.info("");
		}

		return logsum;

	}

	/**
	 * Set the DMU attributes for the tour.
	 * 
	 * @param tour
	 */
	private void setDmuAttributes(VisitorTour tour) {

		// update the MC dmuObjects for this person
		int originTaz = mgraManager.getTaz(tour.getOriginMGRA());
		int destinationTaz = mgraManager.getTaz(tour.getDestinationMGRA());
		mcDmuObject.setDmuIndexValues(tour.getID(), originTaz, originTaz,
				destinationTaz, tour.getDebugChoiceModels());

		mcDmuObject.setTourDepartPeriod(tour.getDepartTime());
		mcDmuObject.setTourArrivePeriod(tour.getArriveTime());
		mcDmuObject.setIncome((byte) tour.getIncome());
		mcDmuObject.setAutoAvailable(tour.getAutoAvailable());
		mcDmuObject.setPartySize(tour.getNumberOfParticipants());
		mcDmuObject.setTourPurpose(tour.getPurpose());
		logsumsCalculator.setTourMcDmuAttributes(mcDmuObject,
				tour.getOriginMGRA(), tour.getDestinationMGRA(),
				tour.getDepartTime(), tour.getArriveTime(),
				tour.getDebugChoiceModels());

	}

	/**
	 * Use to choose tour mode and set result in tour object.
	 * 
	 * @param tour
	 *            The crossborder tour
	 */
	public void chooseTourMode(VisitorTour tour) {

		byte tourMode = (byte) getModeChoice(tour);
		tour.setTourMode(tourMode);
	}

	/**
	 * Get the choice of mode for the tour, and return as an integer (don't
	 * store in tour object)
	 * 
	 * @param tour
	 * @return
	 */
	private int getModeChoice(VisitorTour tour) {

		String choiceModelDescription = "";
		String decisionMakerLabel = "";
		String loggingHeader = "";
		String separator = "";
		String purposeName = modelStructure.VISITOR_PURPOSES[tour.getPurpose()];

		if (tour.getDebugChoiceModels()) {

			tour.logTourObject(logger, 100);
			logger.info("Logging tour mode choice model");
		}

		setDmuAttributes(tour);

		mcModel.computeUtilities(mcDmuObject, mcDmuObject.getDmuIndexValues());

		if (tour.getDebugChoiceModels())
			mcModel.logUECResults(logger, "Visitor tour mode choice model");

		double rn = tour.getRandom();

		// if the choice model has at least one available alternative, make
		// choice.
		int chosen;
		if (mcModel.getAvailabilityCount() > 0) {

			chosen = mcModel.getChoiceResult(rn);

			// best tap pairs were determined and saved in mcDmuObject while
			// setting dmu skim attributes
			// if chosen mode is a transit mode, save these tap pairs in the
			// tour object; if not transit tour attributes remain null.
			if (modelStructure.getTourModeIsTransit(chosen)) {
				tour.setBestWtwTapPairsOut(logsumsCalculator
						.getBestWtwTapsOut());
				tour.setBestWtwTapPairsIn(logsumsCalculator.getBestWtwTapsIn());
				tour.setBestWtdTapPairsOut(logsumsCalculator
						.getBestWtdTapsOut());
				tour.setBestWtdTapPairsIn(logsumsCalculator.getBestWtdTapsIn());
				tour.setBestDtwTapPairsOut(logsumsCalculator
						.getBestDtwTapsOut());
				tour.setBestDtwTapPairsIn(logsumsCalculator.getBestDtwTapsIn());
			}

		} else {

			tour.logTourObject(logger, loggingHeader.length());

			mcModel.logUECResults(logger, loggingHeader);
			logger.info("");
			logger.info("");

			logger.error(String
					.format("Exception caught for HHID=%d, no available %s tour mode alternatives to choose from in choiceModelApplication.",
							tour.getID(), tourCategory));
			throw new RuntimeException();
		}

		// debug output
		if (tour.getDebugChoiceModels()) {

			double[] utilities = mcModel.getUtilities(); // 0s-indexing
			double[] probabilities = mcModel.getProbabilities(); // 0s-indexing
			boolean[] availabilities = mcModel.getAvailabilities(); // 1s-indexing
			String[] altNames = mcModel.getAlternativeNames(); // 0s-indexing

			logger.info("Tour Id: " + tour.getID());
			logger.info("Alternative                    Utility       Probability           CumProb");
			logger.info("--------------------    --------------    --------------    --------------");

			double cumProb = 0.0;
			for (int k = 0; k < mcModel.getNumberOfAlternatives(); k++) {
				cumProb += probabilities[k];
				String altString = String
						.format("%-3d  %s", k + 1, altNames[k]);
				logger.info(String.format("%-20s%15s%18.6e%18.6e%18.6e",
						altString, availabilities[k + 1], utilities[k],
						probabilities[k], cumProb));
			}

			logger.info(" ");
			String altString = String.format("%-3d  %s", chosen,
					altNames[chosen - 1]);
			logger.info(String
					.format("Choice: %s, with rn=%.8f", altString, rn));

			logger.info(separator);
			logger.info("");
			logger.info("");

			// write choice model alternative info to log file
			mcModel.logAlternativesInfo(choiceModelDescription,
					decisionMakerLabel);
			mcModel.logSelectionInfo(choiceModelDescription,
					decisionMakerLabel, rn, chosen);
			mcModel.logLogitCalculations(choiceModelDescription,
					decisionMakerLabel);

		}

		if (saveUtilsProbsFlag) {

			// get the utilities and probabilities arrays for the tour mode
			// choice
			// model for this tour and save them to the tour object
			double[] dUtils = mcModel.getUtilities();
			double[] dProbs = mcModel.getProbabilities();

			float[] utils = new float[dUtils.length];
			float[] probs = new float[dUtils.length];
			for (int k = 0; k < dUtils.length; k++) {
				utils[k] = (float) dUtils[k];
				probs[k] = (float) dProbs[k];
			}

			// tour.setTourModalUtilities(utils);
			// tour.setTourModalProbabilities(probs);

		}

		return chosen;

	}

	public String[] getModeAltNames(int purposeIndex) {
		return modeAltNames;
	}

}
