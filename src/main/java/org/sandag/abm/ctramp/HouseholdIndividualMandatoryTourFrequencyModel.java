package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

/**
 * Implements an invidual mandatory tour frequency model, which selects the
 * number of work, school, or work and school tours for each person who selects
 * a mandatory activity. There are essentially seven separate models, one for
 * each person type (full-time worker, part-time worker, university student, non
 * working adults, retired, driving students, and non-driving students), except
 * pre-school students. The choices are one work tour, two work tours, one
 * school tour, two school tours, and one work and school tour. Availability
 * arrays are defined for each person type.
 * 
 * The UEC for the model has two additional matrix calcuation tabs, which
 * computes the one-way walk distance and the round-trip auto time to work
 * and/or school for the model. This allows us to compute the work and/or school
 * time, by setting the DMU destination index, just using the UEC.
 * 
 * @author D. Ory
 * 
 */
public class HouseholdIndividualMandatoryTourFrequencyModel
        implements Serializable
{

    private transient Logger                    logger                   = Logger.getLogger(HouseholdIndividualMandatoryTourFrequencyModel.class);
    private transient Logger                    tourFreq                 = Logger.getLogger("tourFreq");

    private static final String                 IMTF_CONTROL_FILE_TARGET = "imtf.uec.file";
    private static final String                 IMTF_DATA_SHEET_TARGET   = "imtf.data.page";
    private static final String                 IMTF_MODEL_SHEET_TARGET  = "imtf.model.page";

    private static final String                 MANDATORY_ACTIVITY       = Definitions.MANDATORY_PATTERN;

    // model results
    public static final int                     CHOICE_ONE_WORK          = 1;
    public static final int                     CHOICE_TWO_WORK          = 2;
    public static final int                     CHOICE_ONE_SCHOOL        = 3;
    public static final int                     CHOICE_TWO_SCHOOL        = 4;
    public static final int                     CHOICE_WORK_AND_SCHOOL   = 5;

    public static final String[]                CHOICE_RESULTS           = {"1 Work", "2 Work",
            "1 School", "2 School", "Wrk & Schl", "Worker Works At Home", "Student Works At Home",
            "Worker School At Home", "Student School At Home"            };

    private IndividualMandatoryTourFrequencyDMU imtfDmuObject;
    private ChoiceModelApplication              choiceModelApplication;

    private AccessibilitiesTable                accTable;
    private MandatoryAccessibilitiesCalculator  mandAcc;

    /**
     * Constructor establishes the ChoiceModelApplication, which applies the
     * logit model via the UEC spreadsheet, and it also establishes the UECs
     * used to compute the one-way walk distance to work and/or school and the
     * round-trip auto time to work and/or school. The model must be the first
     * UEC tab, the one-way distance calculations must be the second UEC tab,
     * round-trip time must be the third UEC tab.
     * 
     * @param dmuObject
     *            is the UEC dmu object for this choice model
     * @param uecFileName
     *            is the UEC control file name
     * @param resourceBundle
     *            is the application ResourceBundle, from which a properties
     *            file HashMap will be created for the UEC
     * @param tazDataManager
     *            is the object used to interact with the zonal data table
     * @param modelStructure
     *            is the ModelStructure object that defines segmentation and
     *            other model structure relate atributes
     */
    public HouseholdIndividualMandatoryTourFrequencyModel(HashMap<String, String> propertyMap,
            ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory,
            AccessibilitiesTable accTable, MandatoryAccessibilitiesCalculator myMandAcc)
    {

        setupHouseholdIndividualMandatoryTourFrequencyModel(propertyMap, modelStructure,
                dmuFactory, accTable, myMandAcc);

    }

    private void setupHouseholdIndividualMandatoryTourFrequencyModel(
            HashMap<String, String> propertyMap, ModelStructure modelStructure,
            CtrampDmuFactoryIf dmuFactory, AccessibilitiesTable myAccTable,
            MandatoryAccessibilitiesCalculator myMandAcc)
    {

        logger.info("setting up IMTF choice model.");

        accTable = myAccTable;

        // locate the individual mandatory tour frequency choice model UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String imtfUecFile = propertyMap.get(IMTF_CONTROL_FILE_TARGET);
        imtfUecFile = uecPath + imtfUecFile;

        int dataPage = Util.getIntegerValueFromPropertyMap(propertyMap, IMTF_DATA_SHEET_TARGET);
        int modelPage = Util.getIntegerValueFromPropertyMap(propertyMap, IMTF_MODEL_SHEET_TARGET);

        // get the dmu object from the factory
        imtfDmuObject = dmuFactory.getIndividualMandatoryTourFrequencyDMU();

        // set up the model
        choiceModelApplication = new ChoiceModelApplication(imtfUecFile, modelPage, dataPage,
                propertyMap, (VariableTable) imtfDmuObject);

        mandAcc = myMandAcc;

    }

    /**
     * Applies the model for the array of households that are stored in the
     * HouseholdDataManager. The results are summarized by person type.
     * 
     * @param householdDataManager
     *            is the object containg the Household objects for which this
     *            model is to be applied.
     */
    public void applyModel(Household household)
    {

        Logger modelLogger = tourFreq;
        if (household.getDebugChoiceModels())
            household.logHouseholdObject("Pre Individual Mandatory Tour Frequency Choice HHID="
                    + household.getHhId() + " Object", modelLogger);

        int choice = -1;

        // get this household's person array
        Person[] personArray = household.getPersons();

        // set the household id, origin taz, hh taz, and debugFlag=false in the
        // dmu
        imtfDmuObject.setHousehold(household);

        // set the auto sufficiency dependent escort accessibility value for the
        // household
        String[] types = {"", "escort0", "escort1", "escort2"};
        int autoSufficiency = household.getAutoSufficiency();
        float accessibility = accTable.getAggregateAccessibility(types[autoSufficiency],
                household.getHhMgra());
        imtfDmuObject.setEscortAccessibility(accessibility);

        // loop through the person array (1-based)
        for (int j = 1; j < personArray.length; ++j)
        {

            Person person = personArray[j];

            if (household.getDebugChoiceModels())
            {
                String decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s",
                        household.getHhId(), person.getPersonNum(), person.getPersonType());
                household.logPersonObject(decisionMakerLabel, modelLogger, person);
            }

            String activity = person.getCdapActivity();

            try
            {

                // only apply the model for those with mandatory activities and
                // not
                // preschool children
                if (person.getPersonIsPreschoolChild() == 0
                        && activity.equalsIgnoreCase(MANDATORY_ACTIVITY))
                {

                    // set the person
                    imtfDmuObject.setPerson(person);

                    // write debug header
                    String separator = "";
                    String choiceModelDescription = "";
                    String decisionMakerLabel = "";
                    String loggingHeader = "";
                    if (household.getDebugChoiceModels())
                    {

                        choiceModelDescription = String
                                .format("Individual Mandatory Tour Frequency Choice Model:");
                        decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s.",
                                household.getHhId(), person.getPersonNum(), person.getPersonType());

                        choiceModelApplication.choiceModelUtilityTraceLoggerHeading(
                                choiceModelDescription, decisionMakerLabel);

                        modelLogger.info(" ");
                        String loggerString = "Individual Mandatory Tour Frequency Choice Model: Debug Statement for Household ID: "
                                + household.getHhId()
                                + ", Person Num: "
                                + person.getPersonNum()
                                + ", Person Type: " + person.getPersonType() + ".";
                        for (int k = 0; k < loggerString.length(); k++)
                            separator += "+";
                        modelLogger.info(loggerString);
                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                        loggingHeader = String.format("%s   %s", choiceModelDescription,
                                decisionMakerLabel);

                    }

                    double distance = 999.0;
                    double time = 999.0;
                    if (person.getPersonIsWorker() == 1)
                    {

                        int workMgra = person.getUsualWorkLocation();
                        if (workMgra != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                        {

                            double[] accessibilities = mandAcc.calculateAccessibilitiesForMgraPair(
                                    household.getHhMgra(), workMgra,
                                    household.getDebugChoiceModels(), tourFreq);

                            distance = person.getWorkLocationDistance();
                            time = accessibilities[0]; // sov time
                            // wt time
                            if (accessibilities[2] > 0.0 && accessibilities[2] < time)
                                time = accessibilities[2];
                            // dt time
                            if (accessibilities[3] > 0.0 && accessibilities[3] < time)
                                time = accessibilities[3];

                        } else
                        {
                            // no work location; skip the rest if no school
                            // location.
                            int schoolMgra = person.getUsualSchoolLocation();
                            if (schoolMgra <= 0
                                    || schoolMgra == ModelStructure.NOT_ENROLLED_SEGMENT_INDEX)
                                continue;
                        }

                    }
                    imtfDmuObject.setDistanceToWorkLoc(distance);
                    imtfDmuObject.setBestTimeToWorkLoc(time);

                    distance = 999.0;
                    if (person.getPersonIsUniversityStudent() == 1
                            || person.getPersonIsStudentDriving() == 1
                            || person.getPersonIsStudentNonDriving() == 1)
                    {

                        int schoolMgra = person.getUsualSchoolLocation();
                        if (schoolMgra != ModelStructure.NOT_ENROLLED_SEGMENT_INDEX)
                        {
                            distance = person.getSchoolLocationDistance();
                        } else
                        {
                            // no school location; skip the rest if no work
                            // location.
                            int workMgra = person.getUsualWorkLocation();
                            if (workMgra <= 0
                                    || workMgra == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                                continue;
                        }

                    }
                    imtfDmuObject.setDistanceToSchoolLoc(distance);

                    // compute the utilities
                    IndexValues index = imtfDmuObject.getIndexValues();
                    choiceModelApplication.computeUtilities(imtfDmuObject, index);

                    // get the random number from the household
                    Random random = household.getHhRandom();
                    int randomCount = household.getHhRandomCount();
                    double rn = random.nextDouble();

                    // if the choice model has at least one available
                    // alternative,
                    // make choice.
                    if (choiceModelApplication.getAvailabilityCount() > 0) choice = choiceModelApplication
                            .getChoiceResult(rn);
                    else
                    {
                        logger.error(String
                                .format("Exception caught for j=%d, activity=%s, HHID=%d, no available alternatives to choose from in choiceModelApplication.",
                                        j, activity, household.getHhId()));
                        throw new RuntimeException();
                    }

                    // debug output
                    if (household.getDebugChoiceModels())
                    {

                        double[] utilities = choiceModelApplication.getUtilities();
                        double[] probabilities = choiceModelApplication.getProbabilities();

                        int personNum = person.getPersonNum();
                        modelLogger.info("Person num: " + personNum + ", Person type: "
                                + person.getPersonType());
                        modelLogger
                                .info("Alternative                 Utility       Probability           CumProb");
                        modelLogger
                                .info("------------------   --------------    --------------    --------------");

                        double cumProb = 0.0;
                        for (int k = 0; k < probabilities.length; ++k)
                        {
                            cumProb += probabilities[k];
                            String altString = String.format("%-3d %10s", k + 1, CHOICE_RESULTS[k]);
                            modelLogger.info(String.format("%-15s%18.6e%18.6e%18.6e", altString,
                                    utilities[k], probabilities[k], cumProb));
                        }

                        modelLogger.info(" ");
                        String altString = String.format("%-3d %10s", choice,
                                CHOICE_RESULTS[choice - 1]);
                        modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                                altString, rn, randomCount));

                        modelLogger.info(separator);
                        modelLogger.info("");
                        modelLogger.info("");

                        // write choice model alternative info to debug log file
                        choiceModelApplication.logAlternativesInfo(choiceModelDescription,
                                decisionMakerLabel);
                        choiceModelApplication.logSelectionInfo(choiceModelDescription,
                                decisionMakerLabel, rn, choice);

                        // write UEC calculation results to separate model
                        // specific
                        // log file
                        choiceModelApplication.logUECResults(modelLogger, loggingHeader);

                    }

                    person.setImtfChoice(choice);

                    // set the person choices
                    if (choice == CHOICE_ONE_WORK)
                    {
                        person.createWorkTours(1, 0, ModelStructure.WORK_PRIMARY_PURPOSE_NAME,
                                ModelStructure.WORK_PRIMARY_PURPOSE_INDEX);
                    } else if (choice == CHOICE_TWO_WORK)
                    {
                        person.createWorkTours(2, 0, ModelStructure.WORK_PRIMARY_PURPOSE_NAME,
                                ModelStructure.WORK_PRIMARY_PURPOSE_INDEX);
                    } else if (choice == CHOICE_ONE_SCHOOL)
                    {
                        if (person.getPersonIsUniversityStudent() == 1) person.createSchoolTours(1,
                                0, ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME,
                                ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX);
                        else person.createSchoolTours(1, 0,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX);
                    } else if (choice == CHOICE_TWO_SCHOOL)
                    {
                        if (person.getPersonIsUniversityStudent() == 1) person.createSchoolTours(2,
                                0, ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME,
                                ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX);
                        else person.createSchoolTours(2, 0,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX);
                    } else if (choice == CHOICE_WORK_AND_SCHOOL)
                    {
                        person.createWorkTours(1, 0, ModelStructure.WORK_PRIMARY_PURPOSE_NAME,
                                ModelStructure.WORK_PRIMARY_PURPOSE_INDEX);
                        if (person.getPersonIsUniversityStudent() == 1) person.createSchoolTours(1,
                                0, ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME,
                                ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX);
                        else person.createSchoolTours(1, 0,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME,
                                ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX);
                    }

                } else if (activity.equalsIgnoreCase(MANDATORY_ACTIVITY)
                        && person.getPersonIsPreschoolChild() == 1)
                {
                    // mandatory activity if
                    // pre-school child with mandatory activity type is assigned
                    // choice = 3 (1 school tour).
                    choice = 3;

                    person.setImtfChoice(choice);

                    // get the school purpose name for a non-driving age person
                    // to
                    // use for preschool tour purpose
                    person.createSchoolTours(1, 0, ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME,
                            ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX);
                }

            } catch (Exception e)
            {
                logger.error(String.format("Exception caught for j=%d, activity=%s, HHID=%d", j,
                        activity, household.getHhId()));
                throw new RuntimeException();
            }

        } // j (person loop)

        household.setImtfRandomCount(household.getHhRandomCount());

    }

}
