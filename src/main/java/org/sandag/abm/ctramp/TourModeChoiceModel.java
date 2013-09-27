package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;

public class TourModeChoiceModel implements Serializable {

	private transient Logger logger = Logger
			.getLogger(TourModeChoiceModel.class);
	private transient Logger tourMCManLogger = Logger.getLogger("tourMcMan");
	private transient Logger tourMCNonManLogger = Logger
			.getLogger("tourMcNonMan");

	public static final String MANDATORY_MODEL_INDICATOR = ModelStructure.MANDATORY_CATEGORY;
	public static final String NON_MANDATORY_MODEL_INDICATOR = "Non-Mandatory";
	public static final String AT_WORK_SUBTOUR_MODEL_INDICATOR = ModelStructure.AT_WORK_CATEGORY;

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

	private static final int MC_DATA_SHEET = 0;
	private static final String PROPERTIES_UEC_TOUR_MODE_CHOICE = "tourModeChoice.uec.file";
	private static final String PROPERTIES_UEC_MAINT_TOUR_MODE_SHEET = "tourModeChoice.maint.model.page";
	private static final String PROPERTIES_UEC_DISCR_TOUR_MODE_SHEET = "tourModeChoice.discr.model.page";
	private static final String PROPERTIES_UEC_AT_WORK_TOUR_MODE_SHEET = "tourModeChoice.atwork.model.page";

	// A MyChoiceModelApplication object and modeAltsAvailable[] is needed for
	// each purpose
	private ChoiceModelApplication mcModel[];
	private TourModeChoiceDMU mcDmuObject;
	private McLogsumsCalculator logsumHelper;

	private ModelStructure modelStructure;

	private String tourCategory;
	private String[] tourPurposeList;

	private HashMap<String, Integer> purposeModelIndexMap;

	private String[][] modeAltNames;

	private boolean saveUtilsProbsFlag = false;

	private MgraDataManager mgraManager;

	public TourModeChoiceModel(HashMap<String, String> propertyMap,
			ModelStructure myModelStructure, String myTourCategory,
			CtrampDmuFactoryIf dmuFactory, McLogsumsCalculator myLogsumHelper) {

		modelStructure = myModelStructure;
		tourCategory = myTourCategory;
		logsumHelper = myLogsumHelper;
		// logsumHelper passed in, but if it were instantiated here, it woul be
		// as follows
		// logsumHelper = new McLogsumsAppender();
		// logsumHelper.setupSkimCalculators(propertyMap);

		mcDmuObject = dmuFactory.getModeChoiceDMU();
		setupModeChoiceModelApplicationArray(propertyMap, tourCategory);

		mgraManager = MgraDataManager.getInstance();
	}

	public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator() {
		return logsumHelper.getAnmSkimCalculator();
	}

	private void setupModeChoiceModelApplicationArray(
			HashMap<String, String> propertyMap, String tourCategory) {

		logger.info(String.format("setting up %s tour mode choice model.",
				tourCategory));

		// locate the individual mandatory tour mode choice model UEC
		String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
		String mcUecFile = propertyMap.get(PROPERTIES_UEC_TOUR_MODE_CHOICE);
		mcUecFile = uecPath + mcUecFile;

		// default is to not save the tour mode choice utils and probs for each
		// tour
		String saveUtilsProbsString = propertyMap
				.get(CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS);
		if (saveUtilsProbsString != null) {
			if (saveUtilsProbsString.equalsIgnoreCase("true"))
				saveUtilsProbsFlag = true;
		}

		// get the number of purposes and declare the array dimension to be this
		// size.
		HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();

		// create a HashMap to map purposeName to model index
		purposeModelIndexMap = new HashMap<String, Integer>();

		if (tourCategory.equalsIgnoreCase(MANDATORY_MODEL_INDICATOR)) {
			tourPurposeList = new String[3];
			tourPurposeList[0] = ModelStructure.WORK_PRIMARY_PURPOSE_NAME;
			tourPurposeList[1] = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
			tourPurposeList[2] = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;

			int uecIndex = 1;
			int mcModelIndex = 0;
			for (String purposeName : tourPurposeList) {
				if (!modelIndexMap.containsKey(uecIndex)) {
					modelIndexMap.put(uecIndex, mcModelIndex);
					purposeModelIndexMap.put(purposeName, mcModelIndex++);
				} else {
					purposeModelIndexMap.put(purposeName,
							modelIndexMap.get(uecIndex));
				}
				uecIndex++;
			}

		} else if (tourCategory.equalsIgnoreCase(NON_MANDATORY_MODEL_INDICATOR)) {
			tourPurposeList = new String[6];
			tourPurposeList[0] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
			tourPurposeList[1] = ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME;
			tourPurposeList[2] = ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME;
			tourPurposeList[3] = ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME;
			tourPurposeList[4] = ModelStructure.VISITING_PRIMARY_PURPOSE_NAME;
			tourPurposeList[5] = ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME;

			int maintSheet = Integer.parseInt(propertyMap
					.get(PROPERTIES_UEC_MAINT_TOUR_MODE_SHEET));
			int discrSheet = Integer.parseInt(propertyMap
					.get(PROPERTIES_UEC_DISCR_TOUR_MODE_SHEET));

			int uecIndex = 1;
			int mcModelIndex = 0;
			int i = 0;
			for (String purposeName : tourPurposeList) {

				uecIndex = -1;
				if (purposeName
						.equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)
						|| purposeName
								.equalsIgnoreCase(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)
						|| purposeName
								.equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME))
					uecIndex = maintSheet;
				else if (purposeName
						.equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)
						|| purposeName
								.equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)
						|| purposeName
								.equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME))
					uecIndex = discrSheet;

				// if the uec sheet for the model segment is not in the map, add
				// it, otherwise, get it from the map
				if (!modelIndexMap.containsKey(uecIndex)) {
					modelIndexMap.put(uecIndex, mcModelIndex);
					purposeModelIndexMap.put(purposeName, mcModelIndex++);
				} else {
					purposeModelIndexMap.put(purposeName,
							modelIndexMap.get(uecIndex));
				}
				i++;
			}

		} else if (tourCategory
				.equalsIgnoreCase(AT_WORK_SUBTOUR_MODEL_INDICATOR)) {
			tourPurposeList = new String[1];
			tourPurposeList[0] = ModelStructure.WORK_BASED_PRIMARY_PURPOSE_NAME;

			int[] uecSheets = new int[1];
			uecSheets[0] = Integer.parseInt(propertyMap
					.get(PROPERTIES_UEC_AT_WORK_TOUR_MODE_SHEET));

			int mcModelIndex = 0;
			int i = 0;
			for (String purposeName : tourPurposeList) {
				int uecIndex = uecSheets[i];

				// if the uec sheet for the model segment is not in the map, add
				// it, otherwise, get it from the map
				if (!modelIndexMap.containsKey(uecIndex)) {
					modelIndexMap.put(uecIndex, mcModelIndex);
					purposeModelIndexMap.put(purposeName, mcModelIndex++);
				} else {
					purposeModelIndexMap.put(purposeName,
							modelIndexMap.get(uecIndex));
				}
				i++;
			}

		}

		mcModel = new ChoiceModelApplication[modelIndexMap.size()];

		// declare dimensions for the array of choice alternative availability
		// by
		// purpose
		modeAltNames = new String[purposeModelIndexMap.size()][];

		// for each unique model index, create the ChoiceModelApplication object
		// and
		// the availabilty array
		int i = 0;
		for (int m : modelIndexMap.keySet()) {
			mcModel[i] = new ChoiceModelApplication(mcUecFile, m,
					MC_DATA_SHEET, propertyMap, (VariableTable) mcDmuObject);
			modeAltNames[i] = mcModel[i].getAlternativeNames();
			i++;
		}

	}

	public double getModeChoiceLogsum(Household household, Person person,
			Tour tour, Logger modelLogger, String choiceModelDescription,
			String decisionMakerLabel) {

		// update the MC dmuObjects for this person
		mcDmuObject.setHouseholdObject(household);
		mcDmuObject.setPersonObject(person);
		mcDmuObject.setTourObject(tour);
		mcDmuObject.setDmuIndexValues(household.getHhId(),
				tour.getTourDestMgra(), tour.getTourOrigMgra(),
				tour.getTourDestMgra(), household.getDebugChoiceModels());

		return getModeChoiceLogsum(mcDmuObject, tour, modelLogger,
				choiceModelDescription, decisionMakerLabel);

	}

	public double getModeChoiceLogsum(TourModeChoiceDMU mcDmuObject, Tour tour,
			Logger modelLogger, String choiceModelDescription,
			String decisionMakerLabel) {

		int modelIndex = purposeModelIndexMap.get(tour.getTourPrimaryPurpose());

		Household household = tour.getPersonObject().getHouseholdObject();

		// log headers to traceLogger
		if (household.getDebugChoiceModels()) {
			mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
					choiceModelDescription, decisionMakerLabel);
		}

		double logsum = logsumHelper.calculateTourMcLogsum(
				tour.getTourOrigMgra(), tour.getTourDestMgra(),
				tour.getTourDepartPeriod(), tour.getTourArrivePeriod(),
				mcModel[modelIndex], mcDmuObject);

		// write UEC calculation results to separate model specific log file
		if (household.getDebugChoiceModels()) {
			String loggingHeader = String.format("%s   %s",
					choiceModelDescription, decisionMakerLabel);
			mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
			modelLogger.info(choiceModelDescription + " Logsum value: "
					+ logsum);
			modelLogger.info("");
			modelLogger.info("");
		}

		return logsum;

	}

	public int getModeChoice(TourModeChoiceDMU mcDmuObject, String purposeName) {

		int modelIndex = purposeModelIndexMap.get(purposeName);

		Household household = mcDmuObject.getHouseholdObject();

		Logger modelLogger = null;
		if (tourCategory.equalsIgnoreCase(ModelStructure.MANDATORY_CATEGORY))
			modelLogger = tourMCManLogger;
		else
			modelLogger = tourMCNonManLogger;

		String choiceModelDescription = "";
		String decisionMakerLabel = "";
		String loggingHeader = "";
		String separator = "";

		Tour tour = mcDmuObject.getTourObject();

		if (household.getDebugChoiceModels()) {

			if (tour.getTourCategory().equalsIgnoreCase(
					ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
				Person person = null;
				Person[] persons = mcDmuObject.getHouseholdObject()
						.getPersons();
				int[] personNums = tour.getPersonNumArray();
				for (int n = 0; n < personNums.length; n++) {
					int p = personNums[n];
					person = persons[p];

					choiceModelDescription = String
							.format("%s Tour Mode Choice Model for: Purpose=%s, Home=%d, Dest=%d",
									tourCategory, purposeName,
									household.getHhMgra(),
									tour.getTourDestMgra());
					decisionMakerLabel = String
							.format("HH=%d, person record %d of %d in joint tour, PersonNum=%d, PersonType=%s, TourId=%d",
									person.getHouseholdObject().getHhId(), p,
									personNums.length, person.getPersonNum(),
									person.getPersonType(), tour.getTourId());
					loggingHeader = String.format("%s    %s",
							choiceModelDescription, decisionMakerLabel);

					mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
							choiceModelDescription, decisionMakerLabel);

					modelLogger.info(" ");
					for (int k = 0; k < loggingHeader.length(); k++)
						separator += "+";
					modelLogger.info(loggingHeader);
					modelLogger.info(separator);

					household.logTourObject(loggingHeader, modelLogger, person,
							tour);
				}
			} else {
				Person person = mcDmuObject.getPersonObject();

				choiceModelDescription = String
						.format("%s Tour Mode Choice Model for: Purpose=%s, Orig=%d, Dest=%d",
								tourCategory, purposeName,
								tour.getTourOrigMgra(), tour.getTourDestMgra());
				decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s, TourId=%d", person
								.getHouseholdObject().getHhId(), person
								.getPersonNum(), person.getPersonType(), tour
								.getTourId());
				loggingHeader = String.format("%s    %s",
						choiceModelDescription, decisionMakerLabel);

				mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
						choiceModelDescription, decisionMakerLabel);

				modelLogger.info(" ");
				for (int k = 0; k < loggingHeader.length(); k++)
					separator += "+";
				modelLogger.info(loggingHeader);
				modelLogger.info(separator);

				household.logTourObject(loggingHeader, modelLogger, person,
						tour);
			}

		}

		logsumHelper
				.setTourMcDmuAttributes(
						mcDmuObject,
						tour.getTourOrigMgra(),
						tour.getTourDestMgra(),
						tour.getTourDepartPeriod(),
						tour.getTourArrivePeriod(),
						(mcDmuObject.getDmuIndexValues().getDebug() && DEBUG_BEST_PATHS));

		// mode choice UEC references highway skim matrices directly, so set
		// index orig/dest to O/D TAZs.
		IndexValues mcDmuIndex = mcDmuObject.getDmuIndexValues();
		mcDmuIndex.setOriginZone(mgraManager.getTaz(tour.getTourOrigMgra()));
		mcDmuIndex.setDestZone(mgraManager.getTaz(tour.getTourDestMgra()));
		mcDmuIndex.setZoneIndex(tour.getTourDestMgra());

		mcModel[modelIndex].computeUtilities(mcDmuObject, mcDmuIndex);

		mcDmuIndex.setOriginZone(tour.getTourOrigMgra());
		mcDmuIndex.setDestZone(tour.getTourDestMgra());

		Random hhRandom = household.getHhRandom();
		int randomCount = household.getHhRandomCount();
		double rn = hhRandom.nextDouble();

		// if the choice model has at least one available alternative, make
		// choice.
		int chosen;
		if (mcModel[modelIndex].getAvailabilityCount() > 0) {

			chosen = mcModel[modelIndex].getChoiceResult(rn);

			// best tap pairs were determined and saved in mcDmuObject while
			// setting dmu skim attributes
			// if chosen mode is a transit mode, save these tap pairs in the
			// tour object; if not transit tour attributes remain null.
			if (modelStructure.getTourModeIsTransit(chosen)) {
				tour.setBestWtwTapPairsOut(logsumHelper.getBestWtwTapsOut());
				tour.setBestWtwTapPairsIn(logsumHelper.getBestWtwTapsIn());
				tour.setBestWtdTapPairsOut(logsumHelper.getBestWtdTapsOut());
				tour.setBestWtdTapPairsIn(logsumHelper.getBestWtdTapsIn());
				tour.setBestDtwTapPairsOut(logsumHelper.getBestDtwTapsOut());
				tour.setBestDtwTapPairsIn(logsumHelper.getBestDtwTapsIn());
			}

		} else {

			if (tour.getTourCategory().equalsIgnoreCase(
					ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
				Person person = null;
				Person[] persons = mcDmuObject.getHouseholdObject()
						.getPersons();
				int[] personNums = tour.getPersonNumArray();
				for (int n = 0; n < personNums.length; n++) {
					int p = personNums[n];
					person = persons[p];

					choiceModelDescription = String
							.format("No alternatives available for %s Tour Mode Choice Model for: Purpose=%s, Home=%d, Dest=%d",
									tourCategory, purposeName,
									household.getHhMgra(),
									tour.getTourDestMgra());
					decisionMakerLabel = String
							.format("HH=%d, person record %d of %d in joint tour, PersonNum=%d, PersonType=%s, TourId=%d",
									person.getHouseholdObject().getHhId(), p,
									personNums.length, person.getPersonNum(),
									person.getPersonType(), tour.getTourId());
					loggingHeader = String.format("%s    %s",
							choiceModelDescription, decisionMakerLabel);

					mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
							choiceModelDescription, decisionMakerLabel);

					modelLogger.info(" ");
					for (int k = 0; k < loggingHeader.length(); k++)
						separator += "+";
					modelLogger.info(loggingHeader);
					modelLogger.info(separator);

					household.logTourObject(loggingHeader, modelLogger, person,
							tour);
				}
			} else {
				Person person = mcDmuObject.getPersonObject();

				choiceModelDescription = String
						.format("No alternatives available for %s Tour Mode Choice Model for: Purpose=%s, Orig=%d, Dest=%d",
								tourCategory, purposeName,
								tour.getTourOrigMgra(), tour.getTourDestMgra());
				decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s, TourId=%d", person
								.getHouseholdObject().getHhId(), person
								.getPersonNum(), person.getPersonType(), tour
								.getTourId());
				loggingHeader = String.format("%s    %s",
						choiceModelDescription, decisionMakerLabel);

				mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading(
						choiceModelDescription, decisionMakerLabel);

				modelLogger.info(" ");
				for (int k = 0; k < loggingHeader.length(); k++)
					separator += "+";
				modelLogger.info(loggingHeader);
				modelLogger.info(separator);

				household.logTourObject(loggingHeader, modelLogger, person,
						tour);
			}

			mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
			modelLogger.info("");
			modelLogger.info("");

			logger.error(String
					.format("Exception caught for HHID=%d, no available %s tour mode alternatives to choose from in choiceModelApplication.",
							household.getHhId(), tourCategory));
			throw new RuntimeException();
		}

		// debug output
		if (household.getDebugChoiceModels()) {

			double[] utilities = mcModel[modelIndex].getUtilities(); // 0s-indexing
			double[] probabilities = mcModel[modelIndex].getProbabilities(); // 0s-indexing
			boolean[] availabilities = mcModel[modelIndex].getAvailabilities(); // 1s-indexing
			String[] altNames = mcModel[modelIndex].getAlternativeNames(); // 0s-indexing

			if (tour.getTourCategory().equalsIgnoreCase(
					ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
				modelLogger.info("Joint Tour Id: " + tour.getTourId());
			} else {
				Person person = mcDmuObject.getPersonObject();
				String personTypeString = person.getPersonType();
				int personNum = person.getPersonNum();
				modelLogger.info("Person num: " + personNum + ", Person type: "
						+ personTypeString + ", Tour Id: " + tour.getTourId());
			}
			modelLogger
					.info("Alternative                    Utility       Probability           CumProb");
			modelLogger
					.info("--------------------    --------------    --------------    --------------");

			double cumProb = 0.0;
			for (int k = 0; k < mcModel[modelIndex].getNumberOfAlternatives(); k++) {
				cumProb += probabilities[k];
				String altString = String
						.format("%-3d  %s", k + 1, altNames[k]);
				modelLogger.info(String.format("%-20s%15s%18.6e%18.6e%18.6e",
						altString, availabilities[k + 1], utilities[k],
						probabilities[k], cumProb));
			}

			modelLogger.info(" ");
			String altString = String.format("%-3d  %s", chosen,
					altNames[chosen - 1]);
			modelLogger.info(String.format(
					"Choice: %s, with rn=%.8f, randomCount=%d", altString, rn,
					randomCount));

			modelLogger.info(separator);
			modelLogger.info("");
			modelLogger.info("");

			// write choice model alternative info to log file
			mcModel[modelIndex].logAlternativesInfo(choiceModelDescription,
					decisionMakerLabel);
			mcModel[modelIndex].logSelectionInfo(choiceModelDescription,
					decisionMakerLabel, rn, chosen);
			mcModel[modelIndex].logLogitCalculations(choiceModelDescription,
					decisionMakerLabel);

			// write UEC calculation results to separate model specific log file
			mcModel[modelIndex].logUECResults(modelLogger, loggingHeader);
		}

		if (saveUtilsProbsFlag) {

			// get the utilities and probabilities arrays for the tour mode
			// choice
			// model for this tour and save them to the tour object
			double[] dUtils = mcModel[modelIndex].getUtilities();
			double[] dProbs = mcModel[modelIndex].getProbabilities();

			float[] utils = new float[dUtils.length];
			float[] probs = new float[dUtils.length];
			for (int k = 0; k < dUtils.length; k++) {
				utils[k] = (float) dUtils[k];
				probs[k] = (float) dProbs[k];
			}

			tour.setTourModalUtilities(utils);
			tour.setTourModalProbabilities(probs);

		}

		return chosen;

	}

	public String[] getModeAltNames(int purposeIndex) {
		int modelIndex = purposeModelIndexMap
				.get(tourPurposeList[purposeIndex]);
		return modeAltNames[modelIndex];
	}

}
