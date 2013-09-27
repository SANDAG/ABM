package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.util.*;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.sandag.abm.application.SandagCtrampDmuFactory;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TourDepartureTimeAndDurationDMU;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 11, 2008 Time: 9:25:30 AM To
 * change this template use File | Settings | File Templates.
 */
public class HouseholdIndividualMandatoryTourDepartureAndDurationTime implements
		Serializable {

	private transient Logger logger = Logger
			.getLogger(HouseholdIndividualMandatoryTourDepartureAndDurationTime.class);
	private transient Logger todLogger = Logger.getLogger("todLogger");
	private transient Logger tourMCManLogger = Logger.getLogger("tourMcMan");

	private static final String IMTOD_UEC_FILE_TARGET = "departTime.uec.file";
	private static final String IMTOD_UEC_DATA_TARGET = "departTime.data.page";
	private static final String IMTOD_UEC_WORK_MODEL_TARGET = "departTime.work.page";
	private static final String IMTOD_UEC_SCHOOL_MODEL_TARGET = "departTime.school.page";
	private static final String IMTOD_UEC_UNIV_MODEL_TARGET = "departTime.univ.page";

	private int[] workTourDepartureTimeChoiceSample;
	private int[] schoolTourDepartureTimeChoiceSample;

	// DMU for the UEC
	private TourDepartureTimeAndDurationDMU imtodDmuObject;
	private TourModeChoiceDMU mcDmuObject;

	private String tourCategory = ModelStructure.MANDATORY_CATEGORY;

	private ModelStructure modelStructure;

	private TazDataManager tazs;
	private MgraDataManager mgraManager;

	private ChoiceModelApplication workTourChoiceModel;
	private ChoiceModelApplication schoolTourChoiceModel;
	private ChoiceModelApplication univTourChoiceModel;
	private TourModeChoiceModel mcModel;

	private boolean[] needToComputeLogsum;
	private double[] modeChoiceLogsums;

	private int[] altStarts;
	private int[] altEnds;

	private int noAvailableWorkWindowCount = 0;
	private int noAvailableSchoolWindowCount = 0;

	private int noUsualWorkLocationForMandatoryActivity = 0;
	private int noUsualSchoolLocationForMandatoryActivity = 0;

	private HashMap<String, String> rbMap;

	private long mcTime;

	public HouseholdIndividualMandatoryTourDepartureAndDurationTime(
			HashMap<String, String> propertyMap, ModelStructure modelStructure,
			String[] tourPurposeList, CtrampDmuFactoryIf dmuFactory,
			TourModeChoiceModel mcModel) {

		setupHouseholdIndividualMandatoryTourDepartureAndDurationTime(
				propertyMap, modelStructure, tourPurposeList, dmuFactory,
				mcModel);

	}

	private void setupHouseholdIndividualMandatoryTourDepartureAndDurationTime(
			HashMap<String, String> propertyMap, ModelStructure modelStructure,
			String[] tourPurposeList, CtrampDmuFactoryIf dmuFactory,
			TourModeChoiceModel mcModel) {

		logger.info(String.format("setting up %s time-of-day choice model.",
				tourCategory));

		// set the model structure
		this.modelStructure = modelStructure;
		this.mcModel = mcModel;
		rbMap = propertyMap;

		tazs = TazDataManager.getInstance();
		mgraManager = MgraDataManager.getInstance();

		// locate the individual mandatory tour frequency choice model UEC
		String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
		String imtodUecFile = propertyMap.get(IMTOD_UEC_FILE_TARGET);
		imtodUecFile = uecPath + imtodUecFile;

		int dataPage = Util.getIntegerValueFromPropertyMap(propertyMap,
				IMTOD_UEC_DATA_TARGET);
		int workModelPage = Util.getIntegerValueFromPropertyMap(propertyMap,
				IMTOD_UEC_WORK_MODEL_TARGET);
		int schoolModelPage = Util.getIntegerValueFromPropertyMap(propertyMap,
				IMTOD_UEC_SCHOOL_MODEL_TARGET);
		int univModelPage = Util.getIntegerValueFromPropertyMap(propertyMap,
				IMTOD_UEC_UNIV_MODEL_TARGET);

		// get the dmu objects from the factory
		imtodDmuObject = dmuFactory.getTourDepartureTimeAndDurationDMU();
		mcDmuObject = dmuFactory.getModeChoiceDMU();

		// set up the models
		workTourChoiceModel = new ChoiceModelApplication(imtodUecFile,
				workModelPage, dataPage, propertyMap,
				(VariableTable) imtodDmuObject);
		schoolTourChoiceModel = new ChoiceModelApplication(imtodUecFile,
				schoolModelPage, dataPage, propertyMap,
				(VariableTable) imtodDmuObject);
		univTourChoiceModel = new ChoiceModelApplication(imtodUecFile,
				univModelPage, dataPage, propertyMap,
				(VariableTable) imtodDmuObject);

		// get the alternatives table from the work tod UEC.
		TableDataSet altsTable = workTourChoiceModel.getUEC()
				.getAlternativeData();
		altStarts = altsTable
				.getColumnAsInt(CtrampApplication.START_FIELD_NAME);
		altEnds = altsTable.getColumnAsInt(CtrampApplication.END_FIELD_NAME);
		altsTable = null;

		imtodDmuObject.setTodAlts(altStarts, altEnds);

		int numWorkDepartureTimeChoiceAlternatives = workTourChoiceModel
				.getNumberOfAlternatives();
		workTourDepartureTimeChoiceSample = new int[numWorkDepartureTimeChoiceAlternatives + 1];
		Arrays.fill(workTourDepartureTimeChoiceSample, 1);

		int numSchoolDepartureTimeChoiceAlternatives = schoolTourChoiceModel
				.getNumberOfAlternatives();
		schoolTourDepartureTimeChoiceSample = new int[numSchoolDepartureTimeChoiceAlternatives + 1];
		Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

		int numLogsumIndices = modelStructure.getSkimPeriodCombinationIndices().length;
		needToComputeLogsum = new boolean[numLogsumIndices];

		modeChoiceLogsums = new double[numLogsumIndices];

	}

	public void applyModel(Household household, boolean runModeChoice) {
		mcTime = 0;

		Logger modelLogger = todLogger;
		if (household.getDebugChoiceModels()) {
			household.logHouseholdObject(
					"Pre Individual Mandatory Departure Time Choice Model HHID="
							+ household.getHhId(), modelLogger);
			if (runModeChoice)
				household.logHouseholdObject(
						"Pre Individual Mandatory Tour Mode Choice Model HHID="
								+ household.getHhId(), tourMCManLogger);
		}

		// set the household id, origin taz, hh taz, and debugFlag=false in the
		// dmu
		imtodDmuObject.setHousehold(household);

		// get the array of persons for this household
		Person[] personArray = household.getPersons();

		// loop through the persons (1-based array)
		for (int j = 1; j < personArray.length; ++j) {

			Person person = personArray[j];
			person.resetTimeWindow();

			if (household.getDebugChoiceModels()) {
				String decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s",
						household.getHhId(), person.getPersonNum(),
						person.getPersonType());
				household.logPersonObject(decisionMakerLabel, modelLogger,
						person);
				if (runModeChoice)
					household.logPersonObject(decisionMakerLabel,
							tourMCManLogger, person);
			}

			// mandatory tour departure time and dureation choice models for
			// each
			// worker/student require a specific order:
			// 1. Work tours made by workers, school/university tours made by
			// students.
			// 2. Work tours made by students, school/university tours made by
			// workers.
			// TODO: check consistency of these definitions -
			// TODO: workers can also be students (school-age and university)?,
			// non-driving students can be workers?,
			// TODO: cannot be school-age student and university? etc...

			try {

				if (person.getPersonIsWorker() == 1) {
					applyDepartureTimeChoiceForWorkTours(person, runModeChoice);
					if (person.getListOfSchoolTours().size() > 0) {
						if (person.getPersonIsUniversityStudent() == 1) {
							applyDepartureTimeChoiceForUnivTours(person,
									runModeChoice);
						} else {
							applyDepartureTimeChoiceForSchoolTours(person,
									runModeChoice);
						}
					}
				} else if (person.getPersonIsStudent() == 1
						|| person.getPersonIsPreschoolChild() == 1) {
					if (person.getPersonIsUniversityStudent() == 1) {
						applyDepartureTimeChoiceForUnivTours(person,
								runModeChoice);
					} else {
						applyDepartureTimeChoiceForSchoolTours(person,
								runModeChoice);
					}
					if (person.getListOfWorkTours().size() > 0)
						applyDepartureTimeChoiceForWorkTours(person,
								runModeChoice);
				} else {
					if (person.getListOfWorkTours().size() > 0
							|| person.getListOfSchoolTours().size() > 0) {
						logger.error(String
								.format("error mandatory departure time choice model for j=%d, hhId=%d, persNum=%d, personType=%s.",
										j, person.getHouseholdObject()
												.getHhId(), person
												.getPersonNum(), person
												.getPersonType()));
						logger.error(String
								.format("person with type other than worker or student has %d work tours and %d school tours.",
										person.getListOfWorkTours().size(),
										person.getListOfSchoolTours().size()));
						throw new RuntimeException();
					}
				}

			} catch (Exception e) {
				logger.error(String
						.format("error mandatory departure time choice model for j=%d, hhId=%d, persId=%d, persNum=%d, personType=%s.",
								j, person.getHouseholdObject().getHhId(),
								person.getPersonId(), person.getPersonNum(),
								person.getPersonType()));
				throw new RuntimeException(e);
			}

		}

		household.setImtodRandomCount(household.getHhRandomCount());

	}

	/**
	 * 
	 * @param person
	 *            object for which time choice should be made
	 * @return the number of work tours this person had scheduled.
	 */
	private int applyDepartureTimeChoiceForWorkTours(Person person,
			boolean runModeChoice) {

		Logger modelLogger = todLogger;

		// set the dmu object
		imtodDmuObject.setPerson(person);

		Household household = person.getHouseholdObject();

		ArrayList<Tour> workTours = person.getListOfWorkTours();
		ArrayList<Tour> schoolTours = person.getListOfSchoolTours();

		for (int i = 0; i < workTours.size(); i++) {

			Tour t = workTours.get(i);
			t.setTourDepartPeriod(-1);
			t.setTourArrivePeriod(-1);

			// dest taz was set from result of usual school location choice when
			// tour
			// object was created in mandatory tour frequency model.
			// TODO: if the destMgra value is -1, then this mandatory tour was
			// created for a non-student (retired probably)
			// TODO: and we have to resolve this somehow - either genrate a
			// work/school location for retired, or change activity type for
			// person.
			// TODO: for now, we'll just skip the tour, and keep count of them.
			int destMgra = t.getTourDestMgra();
			if (destMgra <= 0) {
				noUsualWorkLocationForMandatoryActivity++;
				continue;
			}

			// write debug header
			String separator = "";
			String choiceModelDescription = "";
			String decisionMakerLabel = "";
			String loggingHeader = "";
			if (household.getDebugChoiceModels()) {

				choiceModelDescription = String
						.format("Individual Mandatory Work Tour Departure Time Choice Model for: Purpose=%s",
								t.getTourPurpose());
				decisionMakerLabel = String
						.format("HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
								household.getHhId(), person.getPersonNum(),
								person.getPersonType(), t.getTourId(),
								workTours.size());

				workTourChoiceModel.choiceModelUtilityTraceLoggerHeading(
						choiceModelDescription, decisionMakerLabel);

				modelLogger.info(" ");
				String loggerString = "Individual Mandatory Work Tour Departure Time Choice Model: Debug Statement for Household ID: "
						+ household.getHhId()
						+ ", Person Num: "
						+ person.getPersonNum()
						+ ", Person Type: "
						+ person.getPersonType()
						+ ", Work Tour Id: "
						+ t.getTourId()
						+ " of "
						+ workTours.size()
						+ " work tours.";
				for (int k = 0; k < loggerString.length(); k++)
					separator += "+";
				modelLogger.info(loggerString);
				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

				loggingHeader = String.format("%s    %s",
						choiceModelDescription, decisionMakerLabel);

			}

			imtodDmuObject.setDestinationZone(destMgra);
			imtodDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(t
					.getTourDestMgra()));

			// set the dmu object
			imtodDmuObject.setTour(t);

			int origMgra = t.getTourOrigMgra();
			imtodDmuObject.setOriginZone(mgraManager.getTaz(origMgra));
			imtodDmuObject.setDestinationZone(mgraManager.getTaz(destMgra));

			// set the choice availability and initialize sample array -
			// choicemodelapplication will change sample[] according to
			// availability[]
			boolean[] departureTimeChoiceAvailability = person
					.getAvailableTimeWindows(altStarts, altEnds);
			Arrays.fill(workTourDepartureTimeChoiceSample, 1);

			if (departureTimeChoiceAvailability.length != workTourDepartureTimeChoiceSample.length) {
				logger.error(String
						.format("error in work departure time choice model for hhId=%d, persId=%d, persNum=%d, work tour %d of %d.",
								person.getHouseholdObject().getHhId(),
								person.getPersonId(), person.getPersonNum(), i,
								workTours.size()));
				logger.error(String
						.format("length of the availability array determined by the number of alternatiuves set in the person scheduler=%d",
								departureTimeChoiceAvailability.length));
				logger.error(String
						.format("does not equal the length of the sample array determined by the number of alternatives in the work tour UEC=%d.",
								workTourDepartureTimeChoiceSample.length));
				throw new RuntimeException();
			}

			// if no time window is available for the tour, make the first and
			// last
			// alternatives available
			// for that alternative, and keep track of the number of times this
			// condition occurs.
			boolean noAlternativeAvailable = true;
			for (int a = 0; a < departureTimeChoiceAvailability.length; a++) {
				if (departureTimeChoiceAvailability[a]) {
					noAlternativeAvailable = false;
					break;
				}
			}

			if (noAlternativeAvailable) {
				noAvailableWorkWindowCount++;
				departureTimeChoiceAvailability[1] = true;
				departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
			}

			// check for multiple tours for this person
			// set the first or second switch if multiple tours for person
			if (workTours.size() == 1
					&& person.getListOfSchoolTours().size() == 0) {
				// not a multiple tour pattern
				imtodDmuObject.setFirstTour(0);
				imtodDmuObject.setSubsequentTour(0);
				imtodDmuObject.setTourNumber(1);
				imtodDmuObject.setEndOfPreviousScheduledTour(0);
				imtodDmuObject.setSubsequentTourIsWork(0);
				imtodDmuObject.setSubsequentTourIsSchool(0);
			} else if (workTours.size() > 1
					&& person.getListOfSchoolTours().size() == 0) {
				// Two work tour multiple tour pattern
				if (i == 0) {
					// first of 2 work tours
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(i + 1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(1);
					imtodDmuObject.setSubsequentTourIsSchool(0);
				} else {
					// second of 2 work tours
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = workTours.get(0)
							.getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this second work tour with depart
					// <= first work tour departure AND arrive >= first work
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= workTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= workTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			} else if (workTours.size() == 1 && schoolTours.size() == 1) {
				// One work tour, one school tour multiple tour pattern
				if (person.getPersonIsWorker() == 1) {
					// worker, so work tour is first scheduled, school tour
					// comes later.
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(1);

				} else {
					// student, so school tour was already scheduled, this work
					// tour is the second.
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = person.getListOfSchoolTours()
							.get(0).getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this work tour with depart <=
					// first school tour departure AND arrive >= first school
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= schoolTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= schoolTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			}

			// calculate and store the mode choice logsum for the usual work
			// location
			// for this worker at the various
			// departure time and duration alternativees
			setWorkTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
					person, t, departureTimeChoiceAvailability);

			if (household.getDebugChoiceModels()) {
				household.logTourObject(loggingHeader, modelLogger, person, t);
			}

			try {
				workTourChoiceModel.computeUtilities(imtodDmuObject,
						imtodDmuObject.getIndexValues(),
						departureTimeChoiceAvailability,
						workTourDepartureTimeChoiceSample);
			} catch (Exception e) {
				logger.error("exception caught computing work tour TOD choice utilities.");
				throw new RuntimeException();
			}

			Random hhRandom = imtodDmuObject.getDmuHouseholdObject()
					.getHhRandom();
			int randomCount = household.getHhRandomCount();
			double rn = hhRandom.nextDouble();

			// if the choice model has no available alternatives, choose between
			// the
			// first and last alternative.
			int chosen;
			if (workTourChoiceModel.getAvailabilityCount() > 0)
				chosen = workTourChoiceModel.getChoiceResult(rn);
			else
				chosen = rn < 0.5 ? 1 : altStarts.length;

			// schedule the chosen alternative
			int chosenStartPeriod = altStarts[chosen - 1];
			int chosenEndPeriod = altEnds[chosen - 1];
			try {
				person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
			} catch (Exception e) {
				logger.error("exception caught updating work tour TOD choice time windows.");
				throw new RuntimeException();
			}

			t.setTourDepartPeriod(chosenStartPeriod);
			t.setTourArrivePeriod(chosenEndPeriod);

			// debug output
			if (household.getDebugChoiceModels()) {

				double[] utilities = workTourChoiceModel.getUtilities();
				double[] probabilities = workTourChoiceModel.getProbabilities();
				boolean[] availabilities = workTourChoiceModel
						.getAvailabilities();

				String personTypeString = person.getPersonType();
				int personNum = person.getPersonNum();
				modelLogger.info("Person num: " + personNum + ", Person type: "
						+ personTypeString + ", Tour Id: " + t.getTourId());
				modelLogger
						.info("Alternative            Availability           Utility       Probability           CumProb");
				modelLogger
						.info("--------------------   ------------    --------------    --------------    --------------");

				double cumProb = 0.0;
				for (int k = 0; k < workTourChoiceModel
						.getNumberOfAlternatives(); k++) {
					cumProb += probabilities[k];
					String altString = String.format("%-3d out=%-3d, in=%-3d",
							k + 1, altStarts[k], altEnds[k]);
					modelLogger.info(String.format(
							"%-20s%15s%18.6e%18.6e%18.6e", altString,
							availabilities[k + 1], utilities[k],
							probabilities[k], cumProb));
				}

				modelLogger.info(" ");
				String altString = String.format("%-3d out=%-3d, in=%-3d",
						chosen, altStarts[chosen - 1], altEnds[chosen - 1]);
				modelLogger.info(String.format(
						"Choice: %s, with rn=%.8f, randomCount=%d", altString,
						rn, randomCount));

				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

				// write choice model alternative info to debug log file
				workTourChoiceModel.logAlternativesInfo(choiceModelDescription,
						decisionMakerLabel);
				workTourChoiceModel.logSelectionInfo(choiceModelDescription,
						decisionMakerLabel, rn, chosen);

				// write UEC calculation results to separate model specific log
				// file
				loggingHeader = String.format("%s  %s", choiceModelDescription,
						decisionMakerLabel);
				workTourChoiceModel.logUECResults(modelLogger, loggingHeader);

			}

			if (runModeChoice) {

				long check = System.nanoTime();

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, t,
						chosenStartPeriod, chosenEndPeriod);

				// use the mcModel object already setup for computing logsums
				// and get
				// the mode choice, where the selected
				// worklocation and subzone an departure time and duration are
				// set
				// for this work tour.
				int chosenMode = mcModel.getModeChoice(mcDmuObject,
						t.getTourPurpose());
				t.setTourModeChoice(chosenMode);

				mcTime += (System.nanoTime() - check);
			}

		}

		if (household.getDebugChoiceModels()) {
			String decisionMakerLabel = String
					.format("Final Work Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
							household.getHhId(), person.getPersonNum(),
							person.getPersonType());
			household.logPersonObject(decisionMakerLabel, modelLogger, person);
		}

		return workTours.size();

	}

	private void setWorkTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
			Person person, Tour tour, boolean[] altAvailable) {

		Household household = person.getHouseholdObject();

		Arrays.fill(needToComputeLogsum, true);
		Arrays.fill(modeChoiceLogsums, -999);

		Logger modelLogger = todLogger;
		String choiceModelDescription = String
				.format("Work Tour Mode Choice Logsum calculation for %s Departure Time Choice",
						tour.getTourPurpose());
		String decisionMakerLabel = String.format(
				"HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
				household.getHhId(), person.getPersonNum(), person
						.getPersonType(), tour.getTourId(), person
						.getListOfWorkTours().size());
		String loggingHeader = String.format("%s    %s",
				choiceModelDescription, decisionMakerLabel);

		for (int a = 1; a <= altStarts.length; a++) {

			// if the depart/arrive alternative is unavailable, no need to check
			// to see if a logsum has been calculated
			if (!altAvailable[a])
				continue;

			int startPeriod = altStarts[a - 1];
			int endPeriod = altEnds[a - 1];

			int index = modelStructure.getSkimPeriodCombinationIndex(
					startPeriod, endPeriod);
			if (needToComputeLogsum[index]) {

				String periodString = modelStructure
						.getSkimMatrixPeriodString(startPeriod)
						+ " to "
						+ modelStructure.getSkimMatrixPeriodString(endPeriod);

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, tour,
						startPeriod, endPeriod);

				if (household.getDebugChoiceModels())
					household.logTourObject(
							loggingHeader + ", " + periodString, modelLogger,
							person, mcDmuObject.getTourObject());

				try {
					modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(
							mcDmuObject, tour, modelLogger,
							choiceModelDescription, decisionMakerLabel + ", "
									+ periodString);
				} catch (Exception e) {
					logger.fatal("exception caught applying mcModel.getModeChoiceLogsum() for "
							+ periodString + " work tour.");
					logger.fatal("choiceModelDescription = "
							+ choiceModelDescription);
					logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
					throw new RuntimeException(e);
				}
				needToComputeLogsum[index] = false;
			}

		}

		imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

		mcDmuObject.getTourObject().setTourDepartPeriod(0);
		mcDmuObject.getTourObject().setTourArrivePeriod(0);
	}

	private void setSchoolTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
			Person person, Tour tour, boolean[] altAvailable) {

		Household household = person.getHouseholdObject();

		Arrays.fill(needToComputeLogsum, true);
		Arrays.fill(modeChoiceLogsums, -999);

		Logger modelLogger = todLogger;
		String choiceModelDescription = String
				.format("School Tour Mode Choice Logsum calculation for %s Departure Time Choice",
						tour.getTourPurpose());
		String decisionMakerLabel = String.format(
				"HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
				household.getHhId(), person.getPersonNum(), person
						.getPersonType(), tour.getTourId(), person
						.getListOfSchoolTours().size());
		String loggingHeader = String.format("%s    %s",
				choiceModelDescription, decisionMakerLabel);

		for (int a = 1; a <= altStarts.length; a++) {

			// if the depart/arrive alternative is unavailable, no need to check
			// to see if a logsum has been calculated
			if (!altAvailable[a])
				continue;

			int startPeriod = altStarts[a - 1];
			int endPeriod = altEnds[a - 1];

			int index = modelStructure.getSkimPeriodCombinationIndex(
					startPeriod, endPeriod);
			if (needToComputeLogsum[index]) {

				String periodString = modelStructure
						.getSkimMatrixPeriodString(startPeriod)
						+ " to "
						+ modelStructure.getSkimMatrixPeriodString(endPeriod);

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, tour,
						startPeriod, endPeriod);

				if (household.getDebugChoiceModels())
					household.logTourObject(
							loggingHeader + ", " + periodString, modelLogger,
							person, mcDmuObject.getTourObject());

				try {
					modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(
							mcDmuObject, tour, modelLogger,
							choiceModelDescription, decisionMakerLabel + ", "
									+ periodString);
				} catch (Exception e) {
					logger.error(e);
					logger.fatal("exception caught applying mcModel.getModeChoiceLogsum() for "
							+ periodString + " school tour.");
					logger.fatal("choiceModelDescription = "
							+ choiceModelDescription);
					logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
					throw new RuntimeException();
				}
				needToComputeLogsum[index] = false;
			}

		}

		imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

	}

	/**
	 * 
	 * @param person
	 *            object for which time choice should be made
	 * @return the number of school tours this person had scheduled.
	 */
	private int applyDepartureTimeChoiceForSchoolTours(Person person,
			boolean runModeChoice) {

		Logger modelLogger = todLogger;

		// set the dmu object
		imtodDmuObject.setPerson(person);

		Household household = person.getHouseholdObject();

		ArrayList<Tour> workTours = person.getListOfWorkTours();
		ArrayList<Tour> schoolTours = person.getListOfSchoolTours();

		for (int i = 0; i < schoolTours.size(); i++) {

			Tour t = schoolTours.get(i);
			t.setTourDepartPeriod(-1);
			t.setTourArrivePeriod(-1);

			// dest taz was set from result of usual school location choice when
			// tour
			// object was created in mandatory tour frequency model.
			// TODO: if the destMgra value is -1, then this mandatory tour was
			// created for a non-student (retired probably)
			// TODO: and we have to resolve this somehow - either genrate a
			// work/school location for retired, or change activity type for
			// person.
			// TODO: for now, we'll just skip the tour, and keep count of them.
			int destMgra = t.getTourDestMgra();
			if (destMgra <= 0) {
				noUsualSchoolLocationForMandatoryActivity++;
				continue;
			}

			// write debug header
			String separator = "";
			String choiceModelDescription = "";
			String decisionMakerLabel = "";
			String loggingHeader = "";
			if (household.getDebugChoiceModels()) {

				choiceModelDescription = String
						.format("Individual Mandatory School Tour Departure Time Choice Model for: Purpose=%s",
								t.getTourPurpose());
				decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
						household.getHhId(), person.getPersonNum(),
						person.getPersonType(), t.getTourId(),
						schoolTours.size());

				schoolTourChoiceModel.choiceModelUtilityTraceLoggerHeading(
						choiceModelDescription, decisionMakerLabel);

				modelLogger.info(" ");
				String loggerString = "Individual Mandatory School Tour Departure Time Choice Model: Debug Statement for Household ID: "
						+ household.getHhId()
						+ ", Person Num: "
						+ person.getPersonNum()
						+ ", Person Type: "
						+ person.getPersonType()
						+ ", Tour Id: "
						+ t.getTourId()
						+ " of "
						+ schoolTours.size()
						+ " school tours.";
				for (int k = 0; k < loggerString.length(); k++)
					separator += "+";
				modelLogger.info(loggerString);
				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

			}

			imtodDmuObject.setDestinationZone(destMgra);
			imtodDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(t
					.getTourDestMgra()));

			// set the dmu object
			imtodDmuObject.setTour(t);

			int origMgra = t.getTourOrigMgra();
			imtodDmuObject.setOriginZone(mgraManager.getTaz(origMgra));
			imtodDmuObject.setDestinationZone(mgraManager.getTaz(destMgra));

			// set the choice availability and sample
			boolean[] departureTimeChoiceAvailability = person
					.getAvailableTimeWindows(altStarts, altEnds);
			Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

			if (departureTimeChoiceAvailability.length != schoolTourDepartureTimeChoiceSample.length) {
				logger.error(String
						.format("error in school departure time choice model for hhId=%d, persId=%d, persNum=%d, school tour %d of %d.",
								person.getHouseholdObject().getHhId(),
								person.getPersonId(), person.getPersonNum(), i,
								schoolTours.size()));
				logger.error(String
						.format("length of the availability array determined by the number of alternatiuves set in the person scheduler=%d",
								departureTimeChoiceAvailability.length));
				logger.error(String
						.format("does not equal the length of the sample array determined by the number of alternatives in the school tour UEC=%d.",
								schoolTourDepartureTimeChoiceSample.length));
				throw new RuntimeException();
			}

			// if no time window is available for the tour, make the first and
			// last
			// alternatives available
			// for that alternative, and keep track of the number of times this
			// condition occurs.
			boolean noAlternativeAvailable = true;
			for (int a = 0; a < departureTimeChoiceAvailability.length; a++) {
				if (departureTimeChoiceAvailability[a]) {
					noAlternativeAvailable = false;
					break;
				}
			}

			if (noAlternativeAvailable) {
				noAvailableSchoolWindowCount++;
				departureTimeChoiceAvailability[1] = true;
				schoolTourDepartureTimeChoiceSample[1] = 1;
				departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
				schoolTourDepartureTimeChoiceSample[schoolTourDepartureTimeChoiceSample.length - 1] = 1;
			}

			// check for multiple tours for this person
			// set the first or second switch if multiple tours for person
			if (schoolTours.size() == 1
					&& person.getListOfWorkTours().size() == 0) {
				// not a multiple tour pattern
				imtodDmuObject.setFirstTour(0);
				imtodDmuObject.setSubsequentTour(0);
				imtodDmuObject.setTourNumber(1);
				imtodDmuObject.setEndOfPreviousScheduledTour(0);
				imtodDmuObject.setSubsequentTourIsWork(0);
				imtodDmuObject.setSubsequentTourIsSchool(0);
			} else if (schoolTours.size() > 1
					&& person.getListOfWorkTours().size() == 0) {
				// Two school tour multiple tour pattern
				if (i == 0) {
					// first of 2 school tours
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(i + 1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(1);
				} else {
					// second of 2 school tours
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = schoolTours.get(0)
							.getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this 2nd school tour with depart
					// <= first school tour departure AND arrive >= first school
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= schoolTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= schoolTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			} else if (schoolTours.size() == 1 && workTours.size() == 1) {
				// One school tour, one work tour multiple tour pattern
				if (person.getPersonIsStudent() == 1) {
					// student, so school tour is first scheduled, work comes
					// later.
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(1);
					imtodDmuObject.setSubsequentTourIsSchool(0);
				} else {
					// worker, so work tour was already scheduled, this school
					// tour is the second.
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = person.getListOfWorkTours()
							.get(0).getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this 2nd school tour with depart
					// <= first work tour departure AND arrive >= first work
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= workTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= workTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			}

			// calculate and store the mode choice logsum for the usual school
			// location for this student at the various
			// departure time and duration alternativees
			setSchoolTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
					person, t, departureTimeChoiceAvailability);

			if (household.getDebugChoiceModels()) {
				household.logTourObject(loggingHeader, modelLogger, person, t);
			}

			schoolTourChoiceModel.computeUtilities(imtodDmuObject,
					imtodDmuObject.getIndexValues(),
					departureTimeChoiceAvailability,
					schoolTourDepartureTimeChoiceSample);

			Random hhRandom = imtodDmuObject.getDmuHouseholdObject()
					.getHhRandom();
			int randomCount = household.getHhRandomCount();
			double rn = hhRandom.nextDouble();

			// if the choice model has no available alternatives, choose between
			// the
			// first and last alternative.
			int chosen;
			if (schoolTourChoiceModel.getAvailabilityCount() > 0)
				chosen = schoolTourChoiceModel.getChoiceResult(rn);
			else
				chosen = rn < 0.5 ? 1 : altStarts.length;

			// schedule the chosen alternative
			int chosenStartPeriod = altStarts[chosen - 1];
			int chosenEndPeriod = altEnds[chosen - 1];
			try {
				person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
			} catch (Exception e) {
				logger.error("exception caught updating school tour TOD choice time windows.");
				throw new RuntimeException();
			}

			t.setTourDepartPeriod(chosenStartPeriod);
			t.setTourArrivePeriod(chosenEndPeriod);

			// debug output
			if (household.getDebugChoiceModels()) {

				double[] utilities = schoolTourChoiceModel.getUtilities();
				double[] probabilities = schoolTourChoiceModel
						.getProbabilities();
				boolean[] availabilities = schoolTourChoiceModel
						.getAvailabilities();

				String personTypeString = person.getPersonType();
				int personNum = person.getPersonNum();
				modelLogger.info("Person num: " + personNum + ", Person type: "
						+ personTypeString + ", Tour Id: " + t.getTourId());
				modelLogger
						.info("Alternative            Availability           Utility       Probability           CumProb");
				modelLogger
						.info("--------------------   ------------    --------------    --------------    --------------");

				double cumProb = 0.0;
				for (int k = 0; k < schoolTourChoiceModel
						.getNumberOfAlternatives(); k++) {
					cumProb += probabilities[k];
					String altString = String.format("%-3d out=%-3d, in=%-3d",
							k + 1, altStarts[k], altEnds[k]);
					modelLogger.info(String.format(
							"%-20s%15s%18.6e%18.6e%18.6e", altString,
							availabilities[k + 1], utilities[k],
							probabilities[k], cumProb));
				}

				modelLogger.info(" ");
				String altString = String.format("%-3d out=%-3d, in=%-3d",
						chosen, altStarts[chosen - 1], altEnds[chosen - 1]);
				modelLogger.info(String.format(
						"Choice: %s, with rn=%.8f, randomCount=%d", altString,
						rn, randomCount));

				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

				// write choice model alternative info to debug log file
				schoolTourChoiceModel.logAlternativesInfo(
						choiceModelDescription, decisionMakerLabel);
				schoolTourChoiceModel.logSelectionInfo(choiceModelDescription,
						decisionMakerLabel, rn, chosen);

				// write UEC calculation results to separate model specific log
				// file
				loggingHeader = String.format("%s  %s", choiceModelDescription,
						decisionMakerLabel);
				schoolTourChoiceModel.logUECResults(modelLogger, loggingHeader,
						200);

			}

			if (runModeChoice) {

				long check = System.nanoTime();

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, t,
						chosenStartPeriod, chosenEndPeriod);

				// use the mcModel object already setup for computing logsums
				// and get
				// the mode choice, where the selected
				// school location and subzone and departure time and duration
				// are
				// set for this school tour.
				int chosenMode = -1;
				chosenMode = mcModel.getModeChoice(mcDmuObject,
						t.getTourPurpose());

				t.setTourModeChoice(chosenMode);

				mcTime += (System.nanoTime() - check);
			}

		}

		if (household.getDebugChoiceModels()) {
			String decisionMakerLabel = String
					.format("Final School Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
							household.getHhId(), person.getPersonNum(),
							person.getPersonType());
			household.logPersonObject(decisionMakerLabel, modelLogger, person);
		}

		return schoolTours.size();

	}

	private void setUnivTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
			Person person, Tour tour, boolean[] altAvailable) {

		Household household = person.getHouseholdObject();

		Arrays.fill(needToComputeLogsum, true);
		Arrays.fill(modeChoiceLogsums, -999);

		Logger modelLogger = todLogger;
		String choiceModelDescription = String
				.format("University Tour Mode Choice Logsum calculation for %s Departure Time Choice",
						tour.getTourPurpose());
		String decisionMakerLabel = String.format(
				"HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
				household.getHhId(), person.getPersonNum(), person
						.getPersonType(), tour.getTourId(), person
						.getListOfSchoolTours().size());
		String loggingHeader = String.format("%s    %s",
				choiceModelDescription, decisionMakerLabel);

		for (int a = 1; a <= altStarts.length; a++) {

			// if the depart/arrive alternative is unavailable, no need to check
			// to see if a logsum has been calculated
			if (!altAvailable[a])
				continue;

			int startPeriod = altStarts[a - 1];
			int endPeriod = altEnds[a - 1];

			int index = modelStructure.getSkimPeriodCombinationIndex(
					startPeriod, endPeriod);
			if (needToComputeLogsum[index]) {

				String periodString = modelStructure
						.getSkimMatrixPeriodString(startPeriod)
						+ " to "
						+ modelStructure.getSkimMatrixPeriodString(endPeriod);

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, tour,
						startPeriod, endPeriod);

				if (household.getDebugChoiceModels())
					household.logTourObject(
							loggingHeader + ", " + periodString, modelLogger,
							person, mcDmuObject.getTourObject());

				try {
					modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(
							mcDmuObject, tour, modelLogger,
							choiceModelDescription, decisionMakerLabel + ", "
									+ periodString);
				} catch (Exception e) {
					logger.error(e);
					logger.fatal("exception caught applying mcModel.getModeChoiceLogsum() for "
							+ periodString + " university tour.");
					logger.fatal("choiceModelDescription = "
							+ choiceModelDescription);
					logger.fatal("decisionMakerLabel = " + decisionMakerLabel);
					throw new RuntimeException();
				}
				needToComputeLogsum[index] = false;
			}

		}

		imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

	}

	/**
	 * 
	 * @param person
	 *            object for which time choice should be made
	 * @return the number of school tours this person had scheduled.
	 */
	private int applyDepartureTimeChoiceForUnivTours(Person person,
			boolean runModeChoice) {

		Logger modelLogger = todLogger;

		// set the dmu object
		imtodDmuObject.setPerson(person);

		Household household = person.getHouseholdObject();

		ArrayList<Tour> workTours = person.getListOfWorkTours();
		ArrayList<Tour> schoolTours = person.getListOfSchoolTours();

		for (int i = 0; i < schoolTours.size(); i++) {

			Tour t = schoolTours.get(i);
			t.setTourDepartPeriod(-1);
			t.setTourArrivePeriod(-1);

			// dest taz was set from result of usual school location choice when
			// tour
			// object was created in mandatory tour frequency model.
			// TODO: if the destMgra value is -1, then this mandatory tour was
			// created for a non-student (retired probably)
			// TODO: and we have to resolve this somehow - either genrate a
			// work/school location for retired, or change activity type for
			// person.
			// TODO: for now, we'll just skip the tour, and keep count of them.
			int destMgra = t.getTourDestMgra();
			if (destMgra <= 0) {
				noUsualSchoolLocationForMandatoryActivity++;
				continue;
			}

			// write debug header
			String separator = "";
			String choiceModelDescription = "";
			String decisionMakerLabel = "";
			String loggingHeader = "";
			if (household.getDebugChoiceModels()) {

				choiceModelDescription = String
						.format("Individual Mandatory University Tour Departure Time Choice Model for: Purpose=%s",
								t.getTourPurpose());
				decisionMakerLabel = String.format(
						"HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d",
						household.getHhId(), person.getPersonNum(),
						person.getPersonType(), t.getTourId(),
						schoolTours.size());

				univTourChoiceModel.choiceModelUtilityTraceLoggerHeading(
						choiceModelDescription, decisionMakerLabel);

				modelLogger.info(" ");
				String loggerString = "Individual Mandatory University Tour Departure Time Choice Model: Debug Statement for Household ID: "
						+ household.getHhId()
						+ ", Person Num: "
						+ person.getPersonNum()
						+ ", Person Type: "
						+ person.getPersonType()
						+ ", Tour Id: "
						+ t.getTourId()
						+ " of "
						+ schoolTours.size()
						+ " school tours.";
				for (int k = 0; k < loggerString.length(); k++)
					separator += "+";
				modelLogger.info(loggerString);
				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

			}

			imtodDmuObject.setDestinationZone(destMgra);
			imtodDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(t
					.getTourDestMgra()));

			// set the dmu object
			imtodDmuObject.setTour(t);

			int origMgra = t.getTourOrigMgra();
			imtodDmuObject.setOriginZone(mgraManager.getTaz(origMgra));
			imtodDmuObject.setDestinationZone(mgraManager.getTaz(destMgra));

			// set the choice availability and sample
			boolean[] departureTimeChoiceAvailability = person
					.getAvailableTimeWindows(altStarts, altEnds);
			Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

			if (departureTimeChoiceAvailability.length != schoolTourDepartureTimeChoiceSample.length) {
				logger.error(String
						.format("error in university departure time choice model for hhId=%d, persId=%d, persNum=%d, school tour %d of %d.",
								person.getHouseholdObject().getHhId(),
								person.getPersonId(), person.getPersonNum(), i,
								schoolTours.size()));
				logger.error(String
						.format("length of the availability array determined by the number of alternatives set in the person scheduler=%d",
								departureTimeChoiceAvailability.length));
				logger.error(String
						.format("does not equal the length of the sample array determined by the number of alternatives in the university tour UEC=%d.",
								schoolTourDepartureTimeChoiceSample.length));
				throw new RuntimeException();
			}

			// if no time window is available for the tour, make the first and
			// last
			// alternatives available
			// for that alternative, and keep track of the number of times this
			// condition occurs.
			boolean noAlternativeAvailable = true;
			for (int a = 0; a < departureTimeChoiceAvailability.length; a++) {
				if (departureTimeChoiceAvailability[a]) {
					noAlternativeAvailable = false;
					break;
				}
			}

			if (noAlternativeAvailable) {
				noAvailableSchoolWindowCount++;
				departureTimeChoiceAvailability[1] = true;
				schoolTourDepartureTimeChoiceSample[1] = 1;
				departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
				schoolTourDepartureTimeChoiceSample[schoolTourDepartureTimeChoiceSample.length - 1] = 1;
			}

			// check for multiple tours for this person
			// set the first or second switch if multiple tours for person
			if (schoolTours.size() == 1
					&& person.getListOfWorkTours().size() == 0) {
				// not a multiple tour pattern
				imtodDmuObject.setFirstTour(0);
				imtodDmuObject.setSubsequentTour(0);
				imtodDmuObject.setTourNumber(1);
				imtodDmuObject.setEndOfPreviousScheduledTour(0);
				imtodDmuObject.setSubsequentTourIsWork(0);
				imtodDmuObject.setSubsequentTourIsSchool(0);
			} else if (schoolTours.size() > 1
					&& person.getListOfWorkTours().size() == 0) {
				// Two school tour multiple tour pattern
				if (i == 0) {
					// first of 2 school tours
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(i + 1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(1);
				} else {
					// second of 2 school tours
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = schoolTours.get(0)
							.getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this 2nd school tour with depart
					// <= first school tour departure AND arrive >= first school
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= schoolTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= schoolTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			} else if (schoolTours.size() == 1 && workTours.size() == 1) {
				// One school tour, one work tour multiple tour pattern
				if (person.getPersonIsStudent() == 1) {
					// student, so school tour is first scheduled, work comes
					// later.
					imtodDmuObject.setFirstTour(1);
					imtodDmuObject.setSubsequentTour(0);
					imtodDmuObject.setTourNumber(1);
					imtodDmuObject.setEndOfPreviousScheduledTour(0);
					imtodDmuObject.setSubsequentTourIsWork(1);
					imtodDmuObject.setSubsequentTourIsSchool(0);
				} else {
					// worker, so work tour was already scheduled, this school
					// tour is the second.
					imtodDmuObject.setFirstTour(0);
					imtodDmuObject.setSubsequentTour(1);
					imtodDmuObject.setTourNumber(i + 1);
					int otherTourArrivePeriod = person.getListOfWorkTours()
							.get(0).getTourArrivePeriod();
					imtodDmuObject
							.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
					imtodDmuObject.setSubsequentTourIsWork(0);
					imtodDmuObject.setSubsequentTourIsSchool(0);

					// block alternatives for this 2nd school tour with depart
					// <= first work tour departure AND arrive >= first work
					// tour arrival.
					for (int a = 1; a <= altStarts.length; a++) {
						// if the depart/arrive alternative is unavailable, no
						// need to check to see if a logsum has been calculated
						if (!departureTimeChoiceAvailability[a])
							continue;

						int startPeriod = altStarts[a - 1];
						int endPeriod = altEnds[a - 1];

						if (startPeriod <= workTours.get(0)
								.getTourDepartPeriod()
								&& endPeriod >= workTours.get(0)
										.getTourArrivePeriod())
							departureTimeChoiceAvailability[a] = false;
					}
				}
			}

			// calculate and store the mode choice logsum for the usual school
			// location for this student at the various
			// departure time and duration alternativees
			setUnivTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(
					person, t, departureTimeChoiceAvailability);

			if (household.getDebugChoiceModels()) {
				household.logTourObject(loggingHeader, modelLogger, person, t);
			}

			univTourChoiceModel.computeUtilities(imtodDmuObject,
					imtodDmuObject.getIndexValues(),
					departureTimeChoiceAvailability,
					schoolTourDepartureTimeChoiceSample);

			Random hhRandom = imtodDmuObject.getDmuHouseholdObject()
					.getHhRandom();
			int randomCount = household.getHhRandomCount();
			double rn = hhRandom.nextDouble();

			// if the choice model has no available alternatives, choose between
			// the
			// first and last alternative.
			int chosen;
			if (univTourChoiceModel.getAvailabilityCount() > 0)
				chosen = univTourChoiceModel.getChoiceResult(rn);
			else
				chosen = rn < 0.5 ? 1 : altStarts.length;

			// schedule the chosen alternative
			int chosenStartPeriod = altStarts[chosen - 1];
			int chosenEndPeriod = altEnds[chosen - 1];
			try {
				person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
			} catch (Exception e) {
				logger.error("exception caught updating school tour TOD choice time windows.");
				throw new RuntimeException();
			}

			t.setTourDepartPeriod(chosenStartPeriod);
			t.setTourArrivePeriod(chosenEndPeriod);

			// debug output
			if (household.getDebugChoiceModels()) {

				double[] utilities = univTourChoiceModel.getUtilities();
				double[] probabilities = univTourChoiceModel.getProbabilities();
				boolean[] availabilities = univTourChoiceModel
						.getAvailabilities();

				String personTypeString = person.getPersonType();
				int personNum = person.getPersonNum();
				modelLogger.info("Person num: " + personNum + ", Person type: "
						+ personTypeString + ", Tour Id: " + t.getTourId());
				modelLogger
						.info("Alternative            Availability           Utility       Probability           CumProb");
				modelLogger
						.info("--------------------   ------------    --------------    --------------    --------------");

				double cumProb = 0.0;
				for (int k = 0; k < schoolTourChoiceModel
						.getNumberOfAlternatives(); k++) {
					cumProb += probabilities[k];
					String altString = String.format("%-3d out=%-3d, in=%-3d",
							k + 1, altStarts[k], altEnds[k]);
					modelLogger.info(String.format(
							"%-20s%15s%18.6e%18.6e%18.6e", altString,
							availabilities[k + 1], utilities[k],
							probabilities[k], cumProb));
				}

				modelLogger.info(" ");
				String altString = String.format("%-3d out=%-3d, in=%-3d",
						chosen, altStarts[chosen - 1], altEnds[chosen - 1]);
				modelLogger.info(String.format(
						"Choice: %s, with rn=%.8f, randomCount=%d", altString,
						rn, randomCount));

				modelLogger.info(separator);
				modelLogger.info("");
				modelLogger.info("");

				// write choice model alternative info to debug log file
				univTourChoiceModel.logAlternativesInfo(choiceModelDescription,
						decisionMakerLabel);
				univTourChoiceModel.logSelectionInfo(choiceModelDescription,
						decisionMakerLabel, rn, chosen);

				// write UEC calculation results to separate model specific log
				// file
				loggingHeader = String.format("%s  %s", choiceModelDescription,
						decisionMakerLabel);
				univTourChoiceModel.logUECResults(modelLogger, loggingHeader,
						200);

			}

			if (runModeChoice) {
				long check = System.nanoTime();

				// set the mode choice attributes needed by @variables in the
				// UEC spreadsheets
				setModeChoiceDmuAttributes(household, person, t,
						chosenStartPeriod, chosenEndPeriod);

				// use the mcModel object already setup for computing logsums
				// and get
				// the mode choice, where the selected
				// school location and subzone and departure time and duration
				// are
				// set for this school tour.
				int chosenMode = -1;
				chosenMode = mcModel.getModeChoice(mcDmuObject,
						t.getTourPurpose());

				t.setTourModeChoice(chosenMode);

				mcTime += (System.nanoTime() - check);
			}

		}

		if (household.getDebugChoiceModels()) {
			String decisionMakerLabel = String
					.format("Final University Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
							household.getHhId(), person.getPersonNum(),
							person.getPersonType());
			household.logPersonObject(decisionMakerLabel, modelLogger, person);
		}

		return schoolTours.size();

	}

	private void setModeChoiceDmuAttributes(Household household, Person person,
			Tour t, int startPeriod, int endPeriod) {

		t.setTourDepartPeriod(startPeriod);
		t.setTourArrivePeriod(endPeriod);

		// update the MC dmuObjects for this person
		mcDmuObject.setHouseholdObject(household);
		mcDmuObject.setPersonObject(person);
		mcDmuObject.setTourObject(t);
		mcDmuObject.setDmuIndexValues(household.getHhId(), t.getTourOrigMgra(),
				t.getTourOrigMgra(), t.getTourDestMgra(),
				household.getDebugChoiceModels());

		mcDmuObject
				.setOrigDuDen(mgraManager.getDuDenValue(t.getTourOrigMgra()));
		mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(t
				.getTourOrigMgra()));
		mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(t
				.getTourOrigMgra()));

		mcDmuObject
				.setDestDuDen(mgraManager.getDuDenValue(t.getTourDestMgra()));
		mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(t
				.getTourDestMgra()));
		mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(t
				.getTourDestMgra()));

		mcDmuObject.setPTazTerminalTime(tazs
				.getOriginTazTerminalTime(mgraManager.getTaz(t
						.getTourOrigMgra())));
		mcDmuObject.setATazTerminalTime(tazs
				.getDestinationTazTerminalTime(mgraManager.getTaz(t
						.getTourDestMgra())));

	}

	public long getModeChoiceTime() {
		return mcTime;
	}

	public static void main(String[] args) {

		// set values for these arguments so an object instance can be created
		// and setup run to test integrity of UEC files before running full
		// model.
		HashMap<String, String> propertyMap;
		TourModeChoiceModel mcModel = null;

		if (args.length == 0) {
			System.out
					.println("no properties file base name (without .properties extension) was specified as an argument.");
			return;
		} else {
			ResourceBundle rb = ResourceBundle.getBundle(args[0]);
			propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
		}

		ModelStructure modelStructure = new SandagModelStructure();
		SandagCtrampDmuFactory dmuFactory = new SandagCtrampDmuFactory(
				modelStructure);
		String[] tourPurposeList = { "White Collar", "Services", "Health",
				"Retail and Food", "Blue Collar", "Military" };

		HouseholdIndividualMandatoryTourDepartureAndDurationTime testObject = new HouseholdIndividualMandatoryTourDepartureAndDurationTime(
				propertyMap, modelStructure, tourPurposeList, dmuFactory,
				mcModel);

	}
}
