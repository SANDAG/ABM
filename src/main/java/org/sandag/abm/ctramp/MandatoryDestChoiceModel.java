package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.Random;
import java.util.HashMap;
import com.pb.common.calculator.VariableTable;
import com.pb.common.util.IndexSort;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

public class MandatoryDestChoiceModel implements Serializable {

	private transient Logger logger = Logger
			.getLogger(MandatoryDestChoiceModel.class);
	private transient Logger dcManLogger = Logger.getLogger("tourDcMan");

	// this constant used as a dimension for saving distance and logsums for
	// alternatives in samples
	private static final int MAXIMUM_SOA_ALTS_FOR_ANY_MODEL = 200;

	private static final int DC_DATA_SHEET = 0;
	private static final int DC_WORK_AT_HOME_SHEET = 1;

	private MgraDataManager mgraManager;
	private DestChoiceSize dcSizeObj;

	private DestChoiceDMU dcDmuObject;
	private DcSoaDMU dcSoaDmuObject;

	private TourModeChoiceModel mcModel;
	private DestinationSampleOfAlternativesModel dcSoaModel;

	private String[] segmentNameList;
	private HashMap<String, Integer> segmentNameIndexMap;
	private HashMap<Integer, Integer> workOccupValueSegmentIndexMap;

	private int[] dcModelIndices;

	// A ChoiceModelApplication object and modeAltsAvailable[] is needed for
	// each purpose
	private ChoiceModelApplication[] locationChoiceModels;
	private ChoiceModelApplication locationChoiceModel;
	private ChoiceModelApplication worksAtHomeModel;

	private int[] uecSheetIndices;
	private int[] soaUecSheetIndices;

	int origMgra;

	private int modelIndex;
	private int shadowPricingIteration;

	private double[] sampleAlternativeDistances;
	private double[] sampleAlternativeLogsums;

	private double[] mgraDistanceArray;

	private BuildAccessibilities aggAcc;

	public MandatoryDestChoiceModel(int index,
			HashMap<String, String> propertyMap, DestChoiceSize dcSizeObj,
			BuildAccessibilities aggAcc, MgraDataManager mgraManager,
			String dcUecFileName, String soaUecFile, int soaSampleSize,
			String modeChoiceUecFile, CtrampDmuFactoryIf dmuFactory,
			TourModeChoiceModel mcModel) {

		// set the model structure and the tour purpose list
		this.mgraManager = mgraManager;
		this.aggAcc = aggAcc;
		this.dcSizeObj = dcSizeObj;
		this.mcModel = mcModel;

		modelIndex = index;

		dcDmuObject = dmuFactory.getDestChoiceDMU();
		dcDmuObject.setAggAcc(aggAcc);

		dcSoaDmuObject = dmuFactory.getDcSoaDMU();
		dcSoaDmuObject.setAggAcc(aggAcc);

		shadowPricingIteration = 0;

		sampleAlternativeDistances = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];
		sampleAlternativeLogsums = new double[MAXIMUM_SOA_ALTS_FOR_ANY_MODEL];

		workOccupValueSegmentIndexMap = aggAcc.getWorkOccupValueIndexMap();

	}

	public void setupWorkSegments(int[] myUecSheetIndices,
			int[] mySoaUecSheetIndices) {
		uecSheetIndices = myUecSheetIndices;
		soaUecSheetIndices = mySoaUecSheetIndices;
		segmentNameList = aggAcc.getWorkSegmentNameList();
	}

	public void setupSchoolSegments() {
		aggAcc.createSchoolSegmentNameIndices();
		uecSheetIndices = aggAcc.getSchoolDcUecSheets();
		soaUecSheetIndices = aggAcc.getSchoolDcSoaUecSheets();
		segmentNameList = aggAcc.getSchoolSegmentNameList();
	}

	public void setupDestChoiceModelArrays(HashMap<String, String> propertyMap,
			String dcUecFileName, String soaUecFile, int soaSampleSize) {

		segmentNameIndexMap = dcSizeObj.getSegmentNameIndexMap();

		// create a sample of alternatives choice model object for use in
		// selecting a sample
		// of all possible destination choice alternatives.
		dcSoaModel = new DestinationSampleOfAlternativesModel(soaUecFile,
				soaSampleSize, propertyMap, mgraManager,
				dcSizeObj.getDcSizeArray(), dcSoaDmuObject, soaUecSheetIndices);

		// create the works-at-home ChoiceModelApplication object
		worksAtHomeModel = new ChoiceModelApplication(dcUecFileName,
				DC_WORK_AT_HOME_SHEET, DC_DATA_SHEET, propertyMap,
				(VariableTable) dcDmuObject);

		dcSoaModel.setAvailabilityForSampleOfAlternatives(dcSizeObj
				.getDcSizeArray());

		// create a lookup array to map purpose index to model index
		dcModelIndices = new int[uecSheetIndices.length];

		// get a set of unique model sheet numbers so that we can create
		// ChoiceModelApplication objects once for each model sheet used
		// also create a HashMap to relate size segment index to SOA Model
		// objects
		HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();
		int dcModelIndex = 0;
		int dcSegmentIndex = 0;
		for (int uecIndex : uecSheetIndices) {
			// if the uec sheet for the model segment is not in the map, add it,
			// otherwise, get it from the map
			if (!modelIndexMap.containsKey(uecIndex)) {
				modelIndexMap.put(uecIndex, dcModelIndex);
				dcModelIndices[dcSegmentIndex] = dcModelIndex++;
			} else {
				dcModelIndices[dcSegmentIndex] = modelIndexMap.get(uecIndex);
			}

			dcSegmentIndex++;
		}
		// the value of dcModelIndex is the number of ChoiceModelApplication
		// objects to create
		// the modelIndexMap keys are the uec sheets to use in building
		// ChoiceModelApplication objects

		locationChoiceModels = new ChoiceModelApplication[modelIndexMap.size()];

		int i = 0;
		for (int uecIndex : modelIndexMap.keySet()) {

			int modelIndex = -1;
			try {
				modelIndex = modelIndexMap.get(uecIndex);
				locationChoiceModels[modelIndex] = new ChoiceModelApplication(
						dcUecFileName, uecIndex, DC_DATA_SHEET, propertyMap,
						(VariableTable) dcDmuObject);
			} catch (RuntimeException e) {
				logger.fatal(String
						.format("exception caught setting up DC ChoiceModelApplication[%d] for modelIndex=%d of %d models",
								i, modelIndex, modelIndexMap.size()));
				logger.fatal("Exception caught:", e);
				logger.fatal("calling System.exit(-1) to terminate.");
				System.exit(-1);
			}

		}

		mgraDistanceArray = new double[mgraManager.getMaxMgra() + 1];

	}

	public void applyWorkLocationChoice(Household hh) {

		if (hh.getDebugChoiceModels()) {
			String label = String.format(
					"Pre Work Location Choice HHId=%d Object", hh.getHhId());
			hh.logHouseholdObject(label, dcManLogger);
		}

		// declare these variables here so their values can be logged if a
		// RuntimeException occurs.
		int i = -1;
		int occupSegmentIndex = -1;
		int occup = -1;
		String occupSegmentName = "";

		Person[] persons = hh.getPersons();

		int tourNum = 0;
		for (i = 1; i < persons.length; i++) {

			Person p = persons[i];

			// skip person if they are not a worker
			if (p.getPersonIsWorker() != 1) {
				p.setWorkLocationSegmentIndex(-1);
				p.setWorkLoc(0);
				p.setWorkLocDistance(0);
				p.setWorkLocLogsum(-999);
				continue;
			}

			// skip person if their work at home choice was work in the home
			// (alternative 2 in choice model)
			int worksAtHomeChoice = selectWorksAtHomeChoice(dcDmuObject, hh, p);
			if (worksAtHomeChoice == ModelStructure.WORKS_AT_HOME_ALTERNATUVE_INDEX) {
				p.setWorkLocationSegmentIndex(ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR);
				p.setWorkLoc(ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR);
				p.setWorkLocDistance(0);
				p.setWorkLocLogsum(-999);
				continue;
			}

			// save person information in decision maker label, and log person
			// object
			if (hh.getDebugChoiceModels()) {
				String decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s", p
								.getHouseholdObject().getHhId(), p
								.getPersonNum(), p.getPersonType());
				hh.logPersonObject(decisionMakerLabel, dcManLogger, p);
			}

			double[] results = null;
			try {

				int homeMgra = hh.getHhMgra();
				origMgra = homeMgra;

				occup = p.getPersPecasOccup();
				occupSegmentIndex = workOccupValueSegmentIndexMap.get(occup);
				occupSegmentName = segmentNameList[occupSegmentIndex];

				p.setWorkLocationSegmentIndex(occupSegmentIndex);

				// update the DC dmuObject for this person
				dcDmuObject.setHouseholdObject(hh);
				dcDmuObject.setPersonObject(p);
				dcDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra,
						0);

				double[] homeMgraSizeArray = dcSizeObj.getDcSizeArray()[occupSegmentIndex];
				mcModel.getAnmSkimCalculator().getAmPkSkimDistancesFromMgra(
						homeMgra, mgraDistanceArray);

				// set size array for the tour segment and distance array from
				// the home mgra to all destinaion mgras.
				dcSoaDmuObject.setDestChoiceSize(homeMgraSizeArray);
				dcSoaDmuObject.setDestDistance(mgraDistanceArray);

				dcDmuObject.setDestChoiceSize(homeMgraSizeArray);
				dcDmuObject.setDestChoiceDistance(mgraDistanceArray);

				int choiceModelIndex = dcModelIndices[occupSegmentIndex];
				locationChoiceModel = locationChoiceModels[choiceModelIndex];

				// get the work location alternative chosen from the sample
				results = selectLocationFromSampleOfAlternatives("work", -1, p,
						occupSegmentName, occupSegmentIndex, tourNum++,
						homeMgraSizeArray, mgraDistanceArray);

			} catch (RuntimeException e) {
				logger.fatal(String
						.format("Exception caught in dcModel selecting location for i=%d, hh.hhid=%d, person i=%d, in work location choice, occup=%d, segmentIndex=%d, segmentName=%s",
								i, hh.getHhId(), i, occup, occupSegmentIndex,
								occupSegmentName));
				logger.fatal("Exception caught:", e);
				logger.fatal("calling System.exit(-1) to terminate.");
				System.exit(-1);
			}

			p.setWorkLoc((int) results[0]);
			p.setWorkLocDistance((float) results[1]);
			p.setWorkLocLogsum((float) results[2]);

		}

	}

	public void applySchoolLocationChoice(Household hh) {

		if (hh.getDebugChoiceModels()) {
			String label = String.format(
					"Pre school Location Choice HHId=%d Object", hh.getHhId());
			hh.logHouseholdObject(label, dcManLogger);
		}

		// declare these variables here so their values can be logged if a
		// RuntimeException occurs.
		int i = -1;

		int homeMgra = hh.getHhMgra();
		Person[] persons = hh.getPersons();

		int tourNum = 0;
		for (i = 1; i < persons.length; i++) {

			Person p = persons[i];

			int segmentIndex = -1;
			int segmentType = -1;
			if (p.getPersonIsPreschoolChild() == 1
					|| p.getPersonIsStudentNonDriving() == 1
					|| p.getPersonIsStudentDriving() == 1
					|| p.getPersonIsUniversityStudent() == 1) {

				if (p.getPersonIsPreschoolChild() == 1) {
					segmentIndex = segmentNameIndexMap
							.get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.PRESCHOOL_SEGMENT_GROUP_INDEX]);
					segmentType = BuildAccessibilities.PRESCHOOL_ALT_INDEX;
				} else if (p.getPersonIsGradeSchool() == 1) {
					segmentIndex = aggAcc
							.getMgraGradeSchoolSegmentIndex(homeMgra);
					segmentType = BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX;
				} else if (p.getPersonIsHighSchool() == 1) {
					segmentIndex = aggAcc
							.getMgraHighSchoolSegmentIndex(homeMgra);
					segmentType = BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX;
				} else if (p.getPersonIsUniversityStudent() == 1
						&& p.getAge() < 30) {
					segmentIndex = segmentNameIndexMap
							.get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_TYPICAL_SEGMENT_GROUP_INDEX]);
					segmentType = BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX;
				} else if (p.getPersonIsUniversityStudent() == 1
						&& p.getAge() >= 30) {
					segmentIndex = segmentNameIndexMap
							.get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX]);
					segmentType = BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX;
				}

				// if person type is a student but segment index is -1, the
				// person is not enrolled
				// assume home schooled
				if (segmentIndex < 0) {
					p.setSchoolLocationSegmentIndex(ModelStructure.NOT_ENROLLED_SEGMENT_INDEX);
					p.setSchoolLoc(ModelStructure.NOT_ENROLLED_SEGMENT_INDEX);
					p.setSchoolLocDistance(0);
					p.setSchoolLocLogsum(-999);
					continue;
				} else {
					// if the segment is in the skip shadow pricing set, and the
					// iteration is > 0, dont' compute new choice
					if (shadowPricingIteration == 0
							|| !dcSizeObj
									.getSegmentIsInSkipSegmentSet(segmentIndex))
						p.setSchoolLocationSegmentIndex(segmentIndex);
				}

				if (segmentType < 0) {
					segmentType = ModelStructure.NOT_ENROLLED_SEGMENT_INDEX;
				}
			}
			// not a student person type
			else {
				p.setSchoolLocationSegmentIndex(-1);
				p.setSchoolLoc(0);
				p.setSchoolLocDistance(0);
				p.setSchoolLocLogsum(-999);
				continue;
			}

			// save person information in decision maker label, and log person
			// object
			if (hh.getDebugChoiceModels()) {
				String decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s", p
								.getHouseholdObject().getHhId(), p
								.getPersonNum(), p.getPersonType());
				hh.logPersonObject(decisionMakerLabel, dcManLogger, p);
			}

			// if the segment is in the skip shadow pricing set, and the
			// iteration is > 0, dont' compute new choice
			if (shadowPricingIteration > 0
					&& dcSizeObj.getSegmentIsInSkipSegmentSet(segmentIndex))
				continue;

			double[] results = null;
			int modelIndex = 0;
			try {

				origMgra = homeMgra;

				// update the DC dmuObject for this person
				dcDmuObject.setHouseholdObject(hh);
				dcDmuObject.setPersonObject(p);
				dcDmuObject.setDmuIndexValues(hh.getHhId(), homeMgra, origMgra,
						0);

				/**
				 * remove following - don't need non-mandatory accessibility
				 * since we're doing shadow pricing for school tours // set the
				 * auto sufficiency dependent non-mandatory accessibility value
				 * for the household int autoSufficiency =
				 * hh.getAutoSufficiency(); float accessibility =
				 * aggAcc.getAggregateAccessibility(
				 * nonMandatoryAccessibilityTypes[autoSufficiency],
				 * hh.getHhMgra() ); dcDmuObject.setNonMandatoryAccessibility(
				 * accessibility );
				 */

				double[] homeMgraSizeArray = dcSizeObj.getDcSizeArray()[segmentIndex];
				mcModel.getAnmSkimCalculator().getAmPkSkimDistancesFromMgra(
						homeMgra, mgraDistanceArray);

				// set size array for the tour segment and distance array from
				// the home mgra to all destinaion mgras.
				dcSoaDmuObject.setDestChoiceSize(homeMgraSizeArray);
				dcSoaDmuObject.setDestDistance(mgraDistanceArray);

				dcDmuObject.setDestChoiceSize(homeMgraSizeArray);
				dcDmuObject.setDestChoiceDistance(mgraDistanceArray);

				modelIndex = dcModelIndices[segmentIndex];
				locationChoiceModel = locationChoiceModels[modelIndex];

				// get the school location alternative chosen from the sample
				results = selectLocationFromSampleOfAlternatives("school",
						segmentType, p, segmentNameList[segmentIndex],
						segmentIndex, tourNum++, homeMgraSizeArray,
						mgraDistanceArray);

			} catch (RuntimeException e) {
				logger.fatal(String
						.format("Exception caught in dcModel selecting location for i=%d, hh.hhid=%d, person i=%d, in school location choice, modelIndex=%d, segmentIndex=%d, segmentName=%s",
								i, hh.getHhId(), i, modelIndex, segmentIndex,
								segmentNameList[segmentIndex]));
				logger.fatal("Exception caught:", e);
				logger.fatal("calling System.exit(-1) to terminate.");
				System.exit(-1);
			}

			p.setSchoolLoc((int) results[0]);
			p.setSchoolLocDistance((float) results[1]);
			p.setSchoolLocLogsum((float) results[2]);

		}

	}

	/**
	 * 
	 * @return an array with chosen mgra, distance to chosen mgra, and logsum to
	 *         chosen mgra.
	 */
	private double[] selectLocationFromSampleOfAlternatives(String segmentType,
			int segmentTypeIndex, Person person, String segmentName,
			int sizeSegmentIndex, int tourNum, double[] homeMgraSizeArray,
			double[] homeMgraDistanceArray) {

		// set tour origin taz/subzone and start/end times for calculating mode
		// choice logsum
		Logger modelLogger = dcManLogger;

		Household household = person.getHouseholdObject();

		// compute the sample of alternatives set for the person
		dcSoaModel.computeDestinationSampleOfAlternatives(dcSoaDmuObject, null,
				person, segmentName, sizeSegmentIndex, household.getHhMgra());

		// get sample of locations and correction factors for sample
		int[] finalSample = dcSoaModel.getSampleOfAlternatives();
		float[] sampleCorrectionFactors = dcSoaModel
				.getSampleOfAlternativesCorrections();

		int numAlts = locationChoiceModel.getNumberOfAlternatives();

		// set the destAltsAvailable array to true for all destination choice
		// alternatives for each purpose
		boolean[] destAltsAvailable = new boolean[numAlts + 1];
		for (int k = 0; k <= numAlts; k++)
			destAltsAvailable[k] = false;

		// set the destAltsSample array to 1 for all destination choice
		// alternatives
		// for each purpose
		int[] destAltsSample = new int[numAlts + 1];
		for (int k = 0; k <= numAlts; k++)
			destAltsSample[k] = 0;

		int[] sampleValues = new int[finalSample.length];

		dcDmuObject.setDestChoiceSize(homeMgraSizeArray);
		dcDmuObject.setDestChoiceDistance(homeMgraDistanceArray);

		// for the destinations and sub-zones in the sample, compute mc logsums
		// and
		// save in DC dmuObject.
		// also save correction factor and set availability and sample value for
		// the
		// sample alternative to true. 1, respectively.
		for (int i = 1; i < finalSample.length; i++) {

			int destMgra = finalSample[i];
			sampleValues[i] = finalSample[i];

			// get the mode choice logsum for the destination choice sample
			// alternative
			double logsum = getModeChoiceLogsum(household, person, destMgra,
					segmentTypeIndex);

			sampleAlternativeLogsums[i] = logsum;
			sampleAlternativeDistances[i] = homeMgraDistanceArray[finalSample[i]];

			// set logsum value in DC dmuObject for the logsum index, sampled
			// zone and subzone.
			dcDmuObject.setMcLogsum(destMgra, logsum);

			// set sample of alternatives correction factor used in destination
			// choice utility for the sampled alternative.
			dcDmuObject.setDcSoaCorrections(destMgra,
					sampleCorrectionFactors[i]);

			// set availaibility and sample values for the purpose, dcAlt.
			destAltsAvailable[finalSample[i]] = true;
			destAltsSample[finalSample[i]] = 1;

		}

		// log headers to traceLogger if the person making the destination
		// choice is
		// from a household requesting trace information
		String choiceModelDescription = "";
		String decisionMakerLabel = "";
		String loggingHeader = "";
		if (household.getDebugChoiceModels()) {

			// null tour means the DC is a mandatory usual location choice
			choiceModelDescription = String.format(
					"Usual %s Location Choice Model for: Segment=%s",
					segmentType, segmentName);
			decisionMakerLabel = String.format(
					"HH=%d, PersonNum=%d, PersonType=%s, TourNum=%d", person
							.getHouseholdObject().getHhId(), person
							.getPersonNum(), person.getPersonType(), tourNum);

			modelLogger.info(" ");
			modelLogger
					.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			modelLogger.info("Usual " + segmentType
					+ " Location Choice Model for: Segment=" + segmentName
					+ ", Person Num: " + person.getPersonNum()
					+ ", Person Type: " + person.getPersonType() + ", TourNum="
					+ tourNum);

			loggingHeader = String.format("%s for %s", choiceModelDescription,
					decisionMakerLabel);

			locationChoiceModel.choiceModelUtilityTraceLoggerHeading(
					choiceModelDescription, decisionMakerLabel);

		}

		// compute destination choice proportions and choose alternative
		locationChoiceModel.computeUtilities(dcDmuObject,
				dcDmuObject.getDmuIndexValues(), destAltsAvailable,
				destAltsSample);

		Random hhRandom = household.getHhRandom();
		int randomCount = household.getHhRandomCount();
		double rn = hhRandom.nextDouble();

		// if the choice model has at least one available alternative, make
		// choice.
		int chosen = -1;
		if (locationChoiceModel.getAvailabilityCount() > 0) {
			chosen = locationChoiceModel.getChoiceResult(rn);
		} else {
			logger.error(String
					.format("Exception caught for HHID=%d, PersonNum=%d, no available %s destination choice alternatives to choose from in choiceModelApplication.",
							dcDmuObject.getHouseholdObject().getHhId(),
							dcDmuObject.getPersonObject().getPersonNum(),
							segmentName));
		}

		// write choice model alternative info to log file
		int selectedIndex = -1;
		for (int j = 1; j < finalSample.length; j++) {
			if (finalSample[j] == chosen) {
				selectedIndex = j;
				break;
			}
		}

		if (household.getDebugChoiceModels() || chosen <= 0) {

			double[] utilities = locationChoiceModel.getUtilities();
			double[] probabilities = locationChoiceModel.getProbabilities();
			boolean[] availabilities = locationChoiceModel.getAvailabilities();

			String personTypeString = person.getPersonType();
			int personNum = person.getPersonNum();

			modelLogger.info("Person num: " + personNum + ", Person type: "
					+ personTypeString);
			modelLogger
					.info("Alternative             Availability           Utility       Probability           CumProb          Distance            Logsum");
			modelLogger
					.info("--------------------- --------------    --------------    --------------    --------------    --------------    --------------");

			int[] sortedSampleValueIndices = IndexSort.indexSort(sampleValues);

			int sortedSelectedIndex = 0;
			double cumProb = 0.0;
			for (int j = 1; j < finalSample.length; j++) {
				int k = sortedSampleValueIndices[j];
				int alt = finalSample[k];

				if (alt == chosen)
					sortedSelectedIndex = j;

				cumProb += probabilities[alt - 1];
				String altString = String.format("j=%-2d, k=%-2d, mgra=%-5d",
						j, k, alt);
				modelLogger.info(String.format(
						"%-21s%15s%18.6e%18.6e%18.6e%18.6e%18.6e", altString,
						availabilities[alt], utilities[alt - 1],
						probabilities[alt - 1], cumProb,
						sampleAlternativeDistances[k],
						sampleAlternativeLogsums[k]));
			}

			if (sortedSelectedIndex >= 0) {
				modelLogger.info(" ");
				String altString = String.format("j=%d, mgra=%d",
						sortedSelectedIndex, chosen);
				modelLogger
						.info(String
								.format("Choice: %s, dist=%.6e, logsum=%.6e with rn=%.8f, randomCount=%d",
										altString,
										sampleAlternativeDistances[selectedIndex],
										sampleAlternativeLogsums[selectedIndex],
										rn, randomCount));
			} else {
				modelLogger.info(" ");
				modelLogger.info(String.format(
						"j=%d, mgra=None selected, no alternatives available",
						selectedIndex));
				modelLogger.info(String.format(
						"Choice: %s, rn=%.8f, randomCount=%d", "N/A", rn,
						randomCount));
			}

			modelLogger
					.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			modelLogger.info(" ");

			locationChoiceModel.logAlternativesInfo(choiceModelDescription,
					decisionMakerLabel);
			locationChoiceModel.logSelectionInfo(choiceModelDescription,
					decisionMakerLabel, rn, chosen);

			// write UEC calculation results to separate model specific log file
			locationChoiceModel.logUECResults(modelLogger, loggingHeader);

			if (chosen < 0) {
				logger.fatal(String
						.format("Exception caught for HHID=%d, PersonNum=%d, no available %s destination choice alternatives to choose from in choiceModelApplication.",
								dcDmuObject.getHouseholdObject().getHhId(),
								dcDmuObject.getPersonObject().getPersonNum(),
								segmentName));
				logger.fatal("calling System.exit(-1) to terminate.");
				System.exit(-1);
			}

		}

		double[] returnArray = new double[3];

		returnArray[0] = chosen;
		returnArray[1] = sampleAlternativeDistances[selectedIndex];
		returnArray[2] = sampleAlternativeLogsums[selectedIndex];

		return returnArray;

	}

	private int selectWorksAtHomeChoice(DestChoiceDMU dcDmuObject,
			Household household, Person person) {

		// set tour origin taz/subzone and start/end times for calculating mode
		// choice logsum
		Logger modelLogger = dcManLogger;

		dcDmuObject.setHouseholdObject(household);
		dcDmuObject.setPersonObject(person);
		dcDmuObject.setDmuIndexValues(household.getHhId(),
				household.getHhMgra(), origMgra, 0);

		double accessibility = aggAcc.getAccessibilitiesTableObject()
				.getAggregateAccessibility("totEmp", household.getHhMgra());
		dcDmuObject.setWorkAccessibility(accessibility);

		// log headers to traceLogger if the person making the destination
		// choice is
		// from a household requesting trace information
		String choiceModelDescription = "";
		String decisionMakerLabel = "";
		String loggingHeader = "";
		if (household.getDebugChoiceModels()) {

			// null tour means the DC is a mandatory usual location choice
			choiceModelDescription = String
					.format("Usual Work Location Is At Home Choice Model");
			decisionMakerLabel = String.format(
					"HH=%d, PersonNum=%d, PersonType=%s", person
							.getHouseholdObject().getHhId(), person
							.getPersonNum(), person.getPersonType());

			modelLogger.info(" ");
			modelLogger
					.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			modelLogger
					.info("Usual Work Location Is At Home Choice Model: Person Num: "
							+ person.getPersonNum()
							+ ", Person Type: "
							+ person.getPersonType());

			loggingHeader = String.format("%s for %s", choiceModelDescription,
					decisionMakerLabel);

			worksAtHomeModel.choiceModelUtilityTraceLoggerHeading(
					choiceModelDescription, decisionMakerLabel);

		}

		// compute destination choice proportions and choose alternative
		worksAtHomeModel.computeUtilities(dcDmuObject,
				dcDmuObject.getDmuIndexValues());

		Random hhRandom = household.getHhRandom();
		int randomCount = household.getHhRandomCount();
		double rn = hhRandom.nextDouble();

		// if the choice model has at least one available alternative, make
		// choice.
		int chosen = -1;
		if (worksAtHomeModel.getAvailabilityCount() > 0) {
			chosen = worksAtHomeModel.getChoiceResult(rn);
		}

		// write choice model alternative info to log file
		if (household.getDebugChoiceModels() || chosen < 0) {

			double[] utilities = worksAtHomeModel.getUtilities();
			double[] probabilities = worksAtHomeModel.getProbabilities();
			boolean[] availabilities = worksAtHomeModel.getAvailabilities();

			String[] altNames = worksAtHomeModel.getAlternativeNames();

			String personTypeString = person.getPersonType();
			int personNum = person.getPersonNum();

			modelLogger.info("Person num: " + personNum + ", Person type: "
					+ personTypeString);
			modelLogger
					.info("Alternative             Availability           Utility       Probability           CumProb");
			modelLogger
					.info("--------------------- --------------    --------------    --------------    --------------");

			double cumProb = 0.0;
			for (int j = 0; j < utilities.length; j++) {
				cumProb += probabilities[j];
				String altString = String.format("%d, %s", j + 1, altNames[j]);
				modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e",
						altString, availabilities[j + 1], utilities[j],
						probabilities[j], cumProb));
			}

			modelLogger.info(" ");
			String altString = String.format("j=%d, alt=%s", chosen,
					(chosen < 0 ? "N/A, no available alternatives"
							: altNames[chosen - 1]));
			modelLogger.info(String.format(
					"Choice: %s, with rn=%.8f, randomCount=%d", altString, rn,
					randomCount));

			modelLogger
					.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			modelLogger.info(" ");

			worksAtHomeModel.logAlternativesInfo(choiceModelDescription,
					decisionMakerLabel);
			worksAtHomeModel.logSelectionInfo(choiceModelDescription,
					decisionMakerLabel, rn, chosen);

			// write UEC calculation results to separate model specific log file
			worksAtHomeModel.logUECResults(modelLogger, loggingHeader);

		}

		if (chosen < 0) {
			logger.fatal(String
					.format("Exception caught for HHID=%d, PersonNum=%d, no available works at home alternatives to choose from in choiceModelApplication.",
							dcDmuObject.getHouseholdObject().getHhId(),
							dcDmuObject.getPersonObject().getPersonNum()));
			logger.fatal("calling System.exit(-1) to terminate.");
			System.exit(-1);
		}

		return chosen;

	}

	private double getModeChoiceLogsum(Household household, Person person,
			int sampleDestMgra, int segmentTypeIndex) {

		int purposeIndex = 0;
		String purpose = "";
		if (segmentTypeIndex < 0) {
			purposeIndex = ModelStructure.WORK_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.WORK_PRIMARY_PURPOSE_NAME;
		} else if (segmentTypeIndex == BuildAccessibilities.PRESCHOOL_ALT_INDEX) {
			purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
		} else if (segmentTypeIndex == BuildAccessibilities.GRADE_SCHOOL_ALT_INDEX) {
			purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
		} else if (segmentTypeIndex == BuildAccessibilities.HIGH_SCHOOL_ALT_INDEX) {
			purposeIndex = ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME;
		} else if (segmentTypeIndex == BuildAccessibilities.UNIV_TYPICAL_ALT_INDEX) {
			purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
		} else if (segmentTypeIndex == BuildAccessibilities.UNIV_NONTYPICAL_ALT_INDEX) {
			purposeIndex = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX;
			purpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME;
		}

		// create a temporary tour to use to calculate mode choice logsum
		Tour mcLogsumTour = new Tour(person, 0, purposeIndex);
		mcLogsumTour.setTourPurpose(purpose);
		mcLogsumTour.setTourOrigMgra(household.getHhMgra());
		mcLogsumTour.setTourDestMgra(sampleDestMgra);
		mcLogsumTour.setTourDepartPeriod(Person.DEFAULT_MANDATORY_START_PERIOD);
		mcLogsumTour.setTourArrivePeriod(Person.DEFAULT_MANDATORY_END_PERIOD);

		String choiceModelDescription = "";
		String decisionMakerLabel = "";

		if (household.getDebugChoiceModels()) {
			dcManLogger.info("");
			dcManLogger.info("");
			choiceModelDescription = "location choice logsum for segmentTypeIndex="
					+ segmentTypeIndex
					+ ", temp tour PurposeIndex="
					+ purposeIndex;
			decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: "
					+ person.getPersonNum();
			household.logPersonObject(choiceModelDescription + ", "
					+ decisionMakerLabel, dcManLogger, person);
		}

		double logsum = -1;
		try {
			logsum = mcModel.getModeChoiceLogsum(household, person,
					mcLogsumTour, dcManLogger, choiceModelDescription,
					decisionMakerLabel);
		} catch (Exception e) {
			choiceModelDescription = "location choice logsum for segmentTypeIndex="
					+ segmentTypeIndex
					+ ", temp tour PurposeIndex="
					+ purposeIndex;
			decisionMakerLabel = "HHID: " + household.getHhId() + ", PersNum: "
					+ person.getPersonNum();
			logger.fatal("exception caught calculating ModeChoiceLogsum for usual work/school location choice.");
			logger.fatal("choiceModelDescription = " + choiceModelDescription);
			logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
			logger.fatal("Exception caught:", e);
			System.exit(-1);
		}

		return logsum;
	}

	public int getModelIndex() {
		return modelIndex;
	}

	public void setDcSizeObject(DestChoiceSize dcSizeObj) {
		this.dcSizeObj = dcSizeObj;
	}

}