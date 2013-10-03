package org.sandag.abm.ctramp;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import com.pb.common.calculator.VariableTable;
import com.pb.common.model.Alternative;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * Implements a coordinated daily activity pattern model, which is a joint
 * choice of activity types of each member of a household. The class builds and
 * applies separate choice models for households of sizes 1, 2, 3, 4, and 5. For
 * households larger than 5, the persons in the household are ordered such that
 * the first 5 members include up to 2 workers and 3 children (youngest to
 * oldest), the 5-person model is applied for these 5 household members, than a
 * separate, simple cross-sectional distribution is looked up for the remaining
 * household members.
 * 
 * The utilities are computed using four separate UEC spreadsheets. The first
 * computes the activity utility for each person individually; the second
 * computes the activity utility for each person when paired with each other
 * person; the third computes the activity utility for each person when paired
 * with each group of two other people in the household; and the fourth computes
 * the activity utility considering all the members of the household. These
 * utilities are then aggregated to represent each possible activity pattern for
 * the household, and the choice is made. For households larger than 5, a second
 * model is applied after the first, which selects a pattern for the 5+
 * household members from a predefined distribution.
 * 
 * @author D. Ory
 * 
 */
public class HouseholdCoordinatedDailyActivityPatternModel
        implements Serializable
{

    private transient Logger                   logger                                 = Logger.getLogger(HouseholdCoordinatedDailyActivityPatternModel.class);
    private transient Logger                   cdapLogger                             = Logger.getLogger("cdap");
    private transient Logger                   cdapUecLogger                          = Logger.getLogger("cdap_uec");
    private transient Logger                   cdapLogsumLogger                       = Logger.getLogger("cdap_logsum");

    private static final String                UEC_FILE_NAME_PROPERTY                 = "cdap.uec.file";
    private static final String                UEC_DATA_PAGE_PROPERTY                 = "cdap.data.page";
    private static final String                UEC_ONE_PERSON_UTILITY_PAGE_PROPERTY   = "cdap.one.person.page";
    private static final String                UEC_TWO_PERSON_UTILITY_PAGE_PROPERTY   = "cdap.two.person.page";
    private static final String                UEC_THREE_PERSON_UTILITY_PAGE_PROPERTY = "cdap.three.person.page";
    private static final String                UEC_ALL_PERSON_UTILITY_PAGE_PROPERTY   = "cdap.all.person.page";
    private static final String                UEC_JOINT_UTILITY_PAGE_PROPERTY        = "cdap.joint.page";

    public static final int                    MAX_MODEL_HH_SIZE                      = 5;

    private static final String                MANDATORY_PATTERN                      = Definitions.MANDATORY_PATTERN;
    private static final String                NONMANDATORY_PATTERN                   = Definitions.NONMANDATORY_PATTERN;
    private static final String                HOME_PATTERN                           = Definitions.HOME_PATTERN;
    private static final String[]              activityNameArray                      = {
            MANDATORY_PATTERN, NONMANDATORY_PATTERN, HOME_PATTERN                     };

    private ModelStructure                     modelStructure;
    private double[][]                         fixedCumulativeProportions;

    // collection of logit models - one for each household size
    private ArrayList<LogitModel>              logitModelList;

    private AccessibilitiesTable               accTable;

    // DMU for the UEC
    private CoordinatedDailyActivityPatternDMU cdapDmuObject;

    // re-ordered collection of households
    private Person[]                           cdapPersonArray;

    // Five separate UECs to compute segments of the utility
    private UtilityExpressionCalculator        onePersonUec, twoPeopleUec, threePeopleUec,
            allMemberInteractionUec, jointUec;

    public HouseholdCoordinatedDailyActivityPatternModel(HashMap<String, String> propertyMap,
            ModelStructure myModelStructure, CtrampDmuFactoryIf dmuFactory,
            AccessibilitiesTable myAccTable)
    {

        modelStructure = myModelStructure;
        accTable = myAccTable;

        // setup the coordinated daily activity pattern choice model objects
        createLogitModels();
        setupCoordinatedDailyActivityPatternModelApplication(propertyMap, dmuFactory);

    }

    private void setupCoordinatedDailyActivityPatternModelApplication(
            HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory)
    {

        logger.info("setting up CDAP choice model.");

        // locate the coordinated daily activity pattern choice model UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String cdapUecFile = propertyMap.get(UEC_FILE_NAME_PROPERTY);
        cdapUecFile = uecPath + cdapUecFile;

        int dataPage = Util.getIntegerValueFromPropertyMap(propertyMap, UEC_DATA_PAGE_PROPERTY);
        int onePersonPage = Util.getIntegerValueFromPropertyMap(propertyMap,
                UEC_ONE_PERSON_UTILITY_PAGE_PROPERTY);
        int twoPersonPage = Util.getIntegerValueFromPropertyMap(propertyMap,
                UEC_TWO_PERSON_UTILITY_PAGE_PROPERTY);
        int threePersonPage = Util.getIntegerValueFromPropertyMap(propertyMap,
                UEC_THREE_PERSON_UTILITY_PAGE_PROPERTY);
        int allPersonPage = Util.getIntegerValueFromPropertyMap(propertyMap,
                UEC_ALL_PERSON_UTILITY_PAGE_PROPERTY);
        int jointPage = Util.getIntegerValueFromPropertyMap(propertyMap,
                UEC_JOINT_UTILITY_PAGE_PROPERTY);

        // create the coordinated daily activity pattern choice model DMU
        // object.
        cdapDmuObject = dmuFactory.getCoordinatedDailyActivityPatternDMU();

        // create the uecs
        onePersonUec = new UtilityExpressionCalculator(new File(cdapUecFile), onePersonPage,
                dataPage, propertyMap, (VariableTable) cdapDmuObject);
        twoPeopleUec = new UtilityExpressionCalculator(new File(cdapUecFile), twoPersonPage,
                dataPage, propertyMap, (VariableTable) cdapDmuObject);
        threePeopleUec = new UtilityExpressionCalculator(new File(cdapUecFile), threePersonPage,
                dataPage, propertyMap, (VariableTable) cdapDmuObject);
        allMemberInteractionUec = new UtilityExpressionCalculator(new File(cdapUecFile),
                allPersonPage, dataPage, propertyMap, (VariableTable) cdapDmuObject);
        jointUec = new UtilityExpressionCalculator(new File(cdapUecFile), jointPage, dataPage,
                propertyMap, (VariableTable) cdapDmuObject);

        // get the proportions by person type
        double[][] fixedRelativeProportions = modelStructure.getCdap6PlusProps();
        fixedCumulativeProportions = new double[fixedRelativeProportions.length][];

        // i loops over personTypes, 0 not used.
        for (int i = 1; i < fixedRelativeProportions.length; i++)
        {
            fixedCumulativeProportions[i] = new double[fixedRelativeProportions[i].length];

            // j loops over cdap patterns, can skip index 0.
            fixedCumulativeProportions[i][0] = fixedRelativeProportions[i][0];
            for (int j = 1; j < fixedRelativeProportions[i].length; j++)
                fixedCumulativeProportions[i][j] = fixedCumulativeProportions[i][j - 1]
                        + fixedRelativeProportions[i][j];

            // calculate the difference between 1.0 and the cumulative
            // proportion and
            // add to the Mandatory category (j==0)
            // to make sure the cumulative propbabilities sum to exactly 1.0.
            double diff = 1.0 - fixedCumulativeProportions[i][fixedRelativeProportions[i].length - 1];
            fixedCumulativeProportions[i][0] += diff;
        }

    }

    public void applyModel(Household hhObject)
    {

        if (hhObject.getDebugChoiceModels())
            hhObject.logHouseholdObject("Pre CDAP Household " + hhObject.getHhId() + " Object",
                    cdapLogger);

        // get the activity pattern choice
        String pattern = getCoordinatedDailyActivityPatternChoice(hhObject);

        // set the pattern for the household
        hhObject.setCoordinatedDailyActivityPatternResult(pattern);

        // set the pattern for each person and count by person type
        Person[] personArray = hhObject.getPersons();
        for (int j = 1; j < personArray.length; ++j)
        {
            String activityString = pattern.substring(j - 1, j);
            personArray[j].setDailyActivityResult(activityString);
        } // j (person loop)

        // log results for debug households
        if (hhObject.getDebugChoiceModels())
        {

            cdapLogger.info(" ");
            cdapLogger.info("CDAP Chosen Pattern by Person Type");
            cdapLogger
                    .info("(* indicates person was involved in coordinated choice; no * indicates choice by fixed proportions)");
            cdapLogger.info("CDAP # Type FT W PT W UNIV NONW RETR SCHD SCHN PRES");
            cdapLogger.info("------ ---- ---- ---- ---- ---- ---- ---- ---- ----");

            String bString = "";
            for (int j = 1; j < personArray.length; ++j)
            {

                Person[] tempPersonArray = getPersonsNotModeledByCdap(MAX_MODEL_HH_SIZE);

                boolean persNumMatch = false;
                for (int jj = 1; jj < tempPersonArray.length; jj++)
                {
                    if (tempPersonArray[jj].getPersonNum() == personArray[j].getPersonNum())
                        persNumMatch = true;
                }

                String persNumString = "";
                if (persNumMatch) persNumString = String.format("%d  ", j);
                else persNumString = String.format("%d *", j);

                String pString = pattern.substring(j - 1, j);
                String stringToLog = "";

                if (personArray[j].getPersonTypeIsFullTimeWorker() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "FT W", pString, bString, bString, bString, bString, bString, bString,
                            bString);
                }

                else if (personArray[j].getPersonTypeIsPartTimeWorker() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "PT W", bString, pString, bString, bString, bString, bString, bString,
                            bString);
                }

                else if (personArray[j].getPersonIsUniversityStudent() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "UNIV", bString, bString, pString, bString, bString, bString, bString,
                            bString);
                }

                else if (personArray[j].getPersonIsNonWorkingAdultUnder65() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "NONW", bString, bString, bString, pString, bString, bString, bString,
                            bString);
                }

                else if (personArray[j].getPersonIsNonWorkingAdultOver65() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "RETR", bString, bString, bString, bString, pString, bString, bString,
                            bString);
                }

                else if (personArray[j].getPersonIsStudentDriving() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "SCHD", bString, bString, bString, bString, bString, pString, bString,
                            bString);
                }

                else if (personArray[j].getPersonIsStudentNonDriving() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "SCHN", bString, bString, bString, bString, bString, bString, pString,
                            bString);
                }

                else if (personArray[j].getPersonIsPreschoolChild() == 1)
                {
                    stringToLog = String.format("%6s%5s%5s%5s%5s%5s%5s%5s%5s%5s", persNumString,
                            "PRES", bString, bString, bString, bString, bString, bString, bString,
                            pString);
                }

                cdapLogger.info(stringToLog);

            } // j (person loop)

            cdapLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            cdapLogger.info("");
            cdapLogger.info("");

        } // if traceMe

        hhObject.setCdapRandomCount(hhObject.getHhRandomCount());

    }

    /**
     * Prepares a separate logit model for households of size 1, 2, 3, 4, and 5.
     * Each model has 3^n alternatives, where n is the household size. The
     * models are cleared and re-used for each household of the specified size.
     * 
     */
    private void createLogitModels()
    {

        // new the collection of logit models
        logitModelList = new ArrayList<LogitModel>(MAX_MODEL_HH_SIZE);

        // build a model for each HH size
        for (int i = 0; i < MAX_MODEL_HH_SIZE; ++i)
        {

            int hhSize = i + 1;

            // create the working model
            LogitModel workingLogitModel = new LogitModel(hhSize + " Person HH");

            // compute the number of alternatives
            int numberOfAlternatives = 1;
            for (int j = 0; j < hhSize; ++j)
                numberOfAlternatives *= activityNameArray.length;

            // create a counter for each of the people in the hh
            int[] counterForEachPerson = new int[hhSize];
            Arrays.fill(counterForEachPerson, 0);

            // create the alternatives and add them to the logit model
            int numberOfAltsCounter = 0;
            int totalAltsCounter = 0;
            while (numberOfAltsCounter < numberOfAlternatives)
            {

                // set the string for the alternative
                String alternativeName = "";
                int numOutOfHomeActivites = 0;
                for (int j = 0; j < hhSize; ++j)
                {
                    alternativeName += activityNameArray[counterForEachPerson[j]];
                    if (!activityNameArray[counterForEachPerson[j]].equalsIgnoreCase(HOME_PATTERN))
                        numOutOfHomeActivites++;
                }

                // create the alternative and add it to the model
                if (numOutOfHomeActivites < 2)
                {
                    ConcreteAlternative tempAlt = new ConcreteAlternative(alternativeName + "0",
                            totalAltsCounter);
                    workingLogitModel.addAlternative(tempAlt);
                    numberOfAltsCounter++;
                    totalAltsCounter++;

                } else
                {

                    ConcreteAlternative tempAlt = new ConcreteAlternative(alternativeName + "0",
                            totalAltsCounter);
                    workingLogitModel.addAlternative(tempAlt);
                    numberOfAltsCounter++;
                    totalAltsCounter++;

                    tempAlt = new ConcreteAlternative(alternativeName + "j", totalAltsCounter);
                    workingLogitModel.addAlternative(tempAlt);
                    totalAltsCounter++;

                }

                // check increment the counters
                for (int j = 0; j < hhSize; ++j)
                {
                    counterForEachPerson[j]++;
                    if (counterForEachPerson[j] == activityNameArray.length) counterForEachPerson[j] = 0;
                    else break;
                }

            }

            // add the model to the array list
            logitModelList.add(i, workingLogitModel);

        } // for i max hh size

    }

    /**
     * Selects the coordinated daily activity pattern choice for the passed in
     * Household. The method works for households of all sizes, though two
     * separate models are applied for households with more than 5 members.
     * 
     * @param householdObject
     * @return a string of length household size, where each character in the
     *         string represents the activity pattern for that person, in order
     *         (see Household.reOrderPersonsForCdap method).
     */
    public String getCoordinatedDailyActivityPatternChoice(Household householdObject)
    {

        // set all household level dmu variables
        cdapDmuObject.setHousehold(householdObject);

        // set the hh size (cap modeled size at MAX_MODEL_HH_SIZE)
        int actualHhSize = householdObject.getSize();
        int modelHhSize = Math.min(MAX_MODEL_HH_SIZE, actualHhSize);

        // reorder persons for large households if need be
        reOrderPersonsForCdap(householdObject);

        // get the logit model we need and clear it of any lingering probilities
        LogitModel workingLogitModel = logitModelList.get(modelHhSize - 1);
        workingLogitModel.clear();

        // get the alternatives and reset the utilities to zero
        ArrayList<Alternative> alternativeList = workingLogitModel.getAlternatives();
        for (int i = 0; i < alternativeList.size(); ++i)
        {
            Alternative tempAlt = (Alternative) alternativeList.get(i);
            tempAlt.setUtility(0.0);
        }

        // write the debug header if we have a trace household
        if (householdObject.getDebugChoiceModels())
        {

            LogitModel.setLogger(cdapLogsumLogger);

            cdapLogger.info(" ");
            cdapLogger
                    .info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            cdapLogger.info("CDAP Model: Debug Statement for Household ID: "
                    + householdObject.getHhId());
            String firstHeader = "Utility Segment                 PersonA  PersonB  PersonC";
            String secondHeader = "------------------------------ -------- -------- --------";
            for (int j = 0; j < activityNameArray.length; ++j)
            {
                firstHeader += "    " + activityNameArray[j] + " util";
                secondHeader += " ---------";
            }

            cdapLogger.info(firstHeader);
            cdapLogger.info(secondHeader);

        }

        // all the alternatives are available for all households (1-based,
        // ignore 0
        // index and set other three to 1.)
        int[] availability = {-1, 1, 1, 1};

        String[] accStrings = {"", "hov0", "hov1", "hov2"};
        float retAccess = accTable.getAggregateAccessibility(
                accStrings[householdObject.getAutoSufficiency()], householdObject.getHhMgra());

        cdapDmuObject.setRetailAccessibility(retAccess);

        // loop through each person
        for (int i = 0; i < modelHhSize; ++i)
        {

            // get personA
            Person personA = getCdapPerson(i + 1);

            // set the person level dmu variables
            cdapDmuObject.setPersonA(personA);

            int workMgra = personA.getPersonWorkLocationZone();
            if (workMgra > 0 && workMgra != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR) cdapDmuObject
                    .setWorkLocationModeChoiceLogsumA(personA.getWorkLocationLogsum());
            else cdapDmuObject.setWorkLocationModeChoiceLogsumA(0.0);

            int schoolMgra = personA.getPersonSchoolLocationZone();
            int studentType = getStudentTypeForThisCdapPerson(personA);
            if (studentType > 0 && schoolMgra > 0)
            {
                cdapDmuObject.setSchoolLocationModeChoiceLogsumA(personA.getSchoolLocationLogsum());
            } else
            {
                cdapDmuObject.setSchoolLocationModeChoiceLogsumA(0.0);
            }

            // compute the single person utilities
            double[] firstPersonUtilities = onePersonUec.solve(cdapDmuObject.getIndexValues(),
                    cdapDmuObject, availability);

            // log these utilities for trace households
            if (householdObject.getDebugChoiceModels())
            {

                String stringToLog = String.format("%-30s%9d%9s%9s", "OnePerson", (i + 1), "--",
                        "--");

                for (int j = 0; j < activityNameArray.length; ++j)
                {
                    stringToLog += String.format("%10.4f", firstPersonUtilities[j]);
                }
                cdapLogger.info(stringToLog);

                cdapUecLogger.info("PersonA:");
                personA.logEntirePersonObject(cdapUecLogger);
                onePersonUec.logAnswersArray(cdapUecLogger, "ONE PERSON, personA personNum="
                        + personA.getPersonNum());

            } // debug trace

            // align the one person utilities with the alternatives for person i
            for (int j = 0; j < alternativeList.size(); ++j)
            {

                // get the name of the alternative
                Alternative tempAlt = (Alternative) alternativeList.get(j);
                String altName = tempAlt.getName();

                // get the name of the activity for this person in the
                // alternative
                // string
                String altNameForPersonA = altName.substring(i, i + 1);

                // align the utility results with this activity
                for (int k = 0; k < activityNameArray.length; ++k)
                {

                    if (altNameForPersonA.equalsIgnoreCase(activityNameArray[k]))
                    {
                        double currentUtility = tempAlt.getUtility();
                        tempAlt.setUtility(currentUtility + firstPersonUtilities[k]);
                    }
                } // k

            } // j

            // loop through all possible person Bs
            for (int j = 0; j < modelHhSize; ++j)
            {

                // skip if same as person A
                if (i == j) continue;

                Person personB = getCdapPerson(j + 1);

                // skip if i>j because if we have 1,2 for person 1, we don't
                // also
                // want 2,1; that's the
                // same combination of two people
                if (i > j) continue;

                // set the two person level dmu variables
                cdapDmuObject.setPersonB(personB);

                // compute the two people utilities
                double[] twoPersonUtilities = twoPeopleUec.solve(cdapDmuObject.getIndexValues(),
                        cdapDmuObject, availability);

                // log these utilities for trace households
                if (householdObject.getDebugChoiceModels())
                {

                    String stringToLog = String.format("%-30s%9d%9d%9s", "TwoPeople", (i + 1),
                            (j + 1), "--");

                    for (int k = 0; k < activityNameArray.length; ++k)
                    {
                        stringToLog += String.format("%10.4f", twoPersonUtilities[k]);
                    }
                    cdapLogger.info(stringToLog);

                    cdapUecLogger.info("PersonA:");
                    personA.logEntirePersonObject(cdapUecLogger);
                    cdapUecLogger.info("PersonB:");
                    personB.logEntirePersonObject(cdapUecLogger);
                    twoPeopleUec.logAnswersArray(cdapUecLogger,
                            "TWO PERSON, personA personNum=" + personA.getPersonNum()
                                    + " personB personNum=" + personB.getPersonNum());

                } // debug trace

                // align the two person utilities with the alternatives for
                // person i
                for (int k = 0; k < alternativeList.size(); ++k)
                {
                    Alternative tempAlt = (Alternative) alternativeList.get(k);
                    String altName = tempAlt.getName();

                    // get the name of the activity for this person in the
                    // alternative string
                    String altNameForPersonA = altName.substring(i, i + 1);
                    String altNameForPersonB = altName.substring(j, j + 1);

                    for (int l = 0; l < activityNameArray.length; ++l)
                    {
                        if (altNameForPersonA.equalsIgnoreCase(activityNameArray[l])
                                && altNameForPersonB.equalsIgnoreCase(activityNameArray[l]))
                        {
                            double currentUtility = tempAlt.getUtility();
                            tempAlt.setUtility(currentUtility + twoPersonUtilities[l]);
                        }
                    } // l
                } // k

                // loop through all possible person Cs
                for (int k = 0; k < modelHhSize; ++k)
                {

                    // skip if same as person A
                    if (i == k) continue;

                    // skip if same as person B
                    if (j == k) continue;

                    // skip if j>k because if we have 1,2,3 for person 1, we
                    // don't
                    // also want 1,3,2; that's the
                    // same combination of three people
                    if (j > k) continue;

                    Person personC = getCdapPerson(k + 1);

                    // set the three level dmu variables
                    cdapDmuObject.setPersonC(personC);

                    // compute the three person utilities
                    double[] threePersonUtilities = threePeopleUec.solve(
                            cdapDmuObject.getIndexValues(), cdapDmuObject, availability);

                    // log these utilities for trace households
                    if (householdObject.getDebugChoiceModels())
                    {

                        String stringToLog = String.format("%-30s%9d%9d%9d", "ThreePeople",
                                (i + 1), (j + 1), (k + 1));

                        for (int l = 0; l < activityNameArray.length; ++l)
                        {
                            stringToLog += String.format("%10.4f", threePersonUtilities[l]);
                        }
                        cdapLogger.info(stringToLog);

                        cdapUecLogger.info("PersonA:");
                        personA.logEntirePersonObject(cdapUecLogger);
                        cdapUecLogger.info("PersonB:");
                        personB.logEntirePersonObject(cdapUecLogger);
                        cdapUecLogger.info("PersonC:");
                        personC.logEntirePersonObject(cdapUecLogger);
                        threePeopleUec.logAnswersArray(cdapUecLogger,
                                "THREE PERSON, personA personNum=" + personA.getPersonNum()
                                        + " personB personNum=" + personB.getPersonNum()
                                        + " personC personNum=" + personC.getPersonNum());

                    } // debug trace

                    // align the three person utilities with the alternatives
                    // for
                    // person i
                    for (int l = 0; l < alternativeList.size(); ++l)
                    {
                        Alternative tempAlt = (Alternative) alternativeList.get(l);
                        String altName = tempAlt.getName();

                        // get the name of the activity for this person in the
                        // alternative string
                        String altNameForPersonA = altName.substring(i, i + 1);
                        String altNameForPersonB = altName.substring(j, j + 1);
                        String altNameForPersonC = altName.substring(k, k + 1);

                        for (int m = 0; m < activityNameArray.length; ++m)
                        {
                            if (altNameForPersonA.equalsIgnoreCase(activityNameArray[m])
                                    && altNameForPersonB.equalsIgnoreCase(activityNameArray[m])
                                    && altNameForPersonC.equalsIgnoreCase(activityNameArray[m]))
                            {
                                double currentUtility = tempAlt.getUtility();
                                tempAlt.setUtility(currentUtility + threePersonUtilities[m]);
                            }
                        } // m
                    } // l

                } // k (person C loop)

            } // j (person B loop)

        } // i (person A loop)

        // compute the interaction utilities
        double[] allMemberInteractionUtilities = allMemberInteractionUec.solve(
                cdapDmuObject.getIndexValues(), cdapDmuObject, availability);

        // log these utilities for trace households
        if (householdObject.getDebugChoiceModels())
        {

            String stringToLog = String.format("%-30s%9s%9s%9s", "AllMembers", "--", "--", "--");

            for (int i = 0; i < activityNameArray.length; ++i)
            {
                stringToLog += String.format("%10.4f", allMemberInteractionUtilities[i]);
            }
            cdapLogger.info(stringToLog);

            allMemberInteractionUec.logAnswersArray(cdapUecLogger, "ALL MEMBER INTERACTIONS");

        } // debug trace

        // align the utilities with the proper alternatives
        for (int i = 0; i < alternativeList.size(); ++i)
        {
            Alternative tempAlt = (Alternative) alternativeList.get(i);
            String altName = tempAlt.getName();

            for (int j = 0; j < activityNameArray.length; ++j)
            {

                boolean samePattern = true;
                for (int k = 0; k < modelHhSize; ++k)
                {

                    // alternative should have pattern j for each member k
                    String altNameForThisPerson = altName.substring(k, k + 1);
                    if (altNameForThisPerson.equalsIgnoreCase(activityNameArray[j])) continue;
                    else
                    {
                        samePattern = false;
                        break;
                    }
                } // k

                // if all have the same pattern, add the new utilities
                if (samePattern)
                {
                    double currentUtility = tempAlt.getUtility();
                    tempAlt.setUtility(currentUtility + allMemberInteractionUtilities[j]);
                }
            } // j

        } // i

        // compute the joint utilities to be added to alternatives with joint
        // tour
        // indicator

        int adultsWithMand = 0;
        int adultsWithNonMand = 0;
        int kidsWithMand = 0;
        int kidsWithNonMand = 0;
        int adultsLeaveHome = 0;
        int workMgra = 0;
        double workLocationAccessibilityForWorkers = 0.0;
        for (int i = 0; i < alternativeList.size(); ++i)
        {

            Alternative tempAlt = (Alternative) alternativeList.get(i);
            String altName = tempAlt.getName();

            adultsWithMand = 0;
            adultsWithNonMand = 0;
            kidsWithMand = 0;
            kidsWithNonMand = 0;
            adultsLeaveHome = 0;
            workMgra = 0;
            workLocationAccessibilityForWorkers = 0.0;
            for (int k = 0; k < modelHhSize; ++k)
            {

                // alternative should have pattern j for each member k
                String altNameForThisPerson = altName.substring(k, k + 1);
                if (altNameForThisPerson.equalsIgnoreCase(MANDATORY_PATTERN))
                {
                    if (isThisCdapPersonAnAdult(k + 1))
                    {
                        adultsWithMand++;
                        adultsLeaveHome++;
                    } else
                    {
                        kidsWithMand++;
                    }

                    workMgra = getWorkLocationForThisCdapPerson(k + 1);
                    if (workMgra > 0 && workMgra != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                    {
                        Person tempPerson = getCdapPerson(k + 1);
                        workLocationAccessibilityForWorkers += tempPerson.getWorkLocationLogsum();
                    }

                } else if (altNameForThisPerson.equalsIgnoreCase(NONMANDATORY_PATTERN))
                {
                    if (isThisCdapPersonAnAdult(k + 1))
                    {
                        adultsWithNonMand++;
                        adultsLeaveHome++;
                    } else
                    {
                        kidsWithNonMand++;
                    }
                }

            } // k

            cdapDmuObject.setNumAdultsWithNonMandatoryDap(adultsWithNonMand);
            cdapDmuObject.setNumAdultsWithMandatoryDap(adultsWithMand);
            cdapDmuObject.setNumKidsWithNonMandatoryDap(kidsWithNonMand);
            cdapDmuObject.setNumKidsWithMandatoryDap(kidsWithMand);
            cdapDmuObject.setAllAdultsAtHome(adultsLeaveHome == 0 ? 1 : 0);
            cdapDmuObject.setWorkAccessForMandatoryDap(workLocationAccessibilityForWorkers);

            double[] jointUtilities = jointUec.solve(cdapDmuObject.getIndexValues(), cdapDmuObject,
                    availability);

            // log these utilities for trace households
            if (householdObject.getDebugChoiceModels())
            {
                String stringToLog = String.format("%-13s%4d %-12s%9s%9s%9s", "Joint", (i + 1),
                        altName, "--", "--", "--");
                stringToLog += String.format("%10.4f",
                        (altName.indexOf("j") > 0 ? jointUtilities[0] : 0.0));
                cdapLogger.info(stringToLog);

                jointUec.logAnswersArray(cdapUecLogger, "JOINT Utility for CDAP Alt index = "
                        + (i + 1) + ", Alt name = " + altName);
            } // debug trace

            // align the utilities with the proper alternatives
            if (altName.indexOf("j") > 0)
            {
                double currentUtility = tempAlt.getUtility();
                tempAlt.setUtility(currentUtility + jointUtilities[0]);
            }

        } // i

        // TODO: check this out - use computeAvailabilty() checks that an
        // alternative
        // is available
        // all utilities are set - compute probabilities
        // workingLogitModel.setAvailability(true);
        workingLogitModel.computeAvailabilities();

        // compute the exponentiated utility, logging debug if need be
        if (householdObject.getDebugChoiceModels())
        {
            workingLogitModel.setDebug(true);
            workingLogitModel.writeUtilityHeader();
        }
        workingLogitModel.getUtility();

        // compute the probabilities, logging debug if need be
        if (householdObject.getDebugChoiceModels())
        {
            workingLogitModel.writeProbabilityHeader();
        }
        workingLogitModel.calculateProbabilities();

        // turn debug off for the next guy
        workingLogitModel.setDebug(false);

        // make a choice for the first five
        Random hhRandom = householdObject.getHhRandom();

        double randomNumber = hhRandom.nextDouble();

        if (householdObject.getDebugChoiceModels())
        {
            cdapLogger.info("randomNumber = " + randomNumber);
        }

        String firstFiveChosenName = "";
        try
        {
            ConcreteAlternative chosenAlternative = (ConcreteAlternative) workingLogitModel
                    .chooseElementalAlternative(randomNumber);
            firstFiveChosenName = chosenAlternative.getName();

            if (householdObject.getDebugChoiceModels())
            {

                int chosenIndex = 0;
                // get the index number for the alternative chosen
                for (int i = 0; i < alternativeList.size(); ++i)
                {
                    Alternative tempAlt = (Alternative) alternativeList.get(i);
                    String altName = tempAlt.getName();
                    if (altName.equalsIgnoreCase(firstFiveChosenName))
                    {
                        chosenIndex = i + 1;
                        break;
                    }
                }

                cdapLogger.info("chosen pattern (5 or fewer hh members): Alt index = "
                        + chosenIndex + ", Alt name = " + firstFiveChosenName);
                cdapLogger.info("");
                cdapLogger.info("");
            }
        } catch (ModelException e)
        {
            logger.error(String.format(
                    "Exception caught for HHID=%d, no available CDAP alternatives to choose.",
                    householdObject.getHhId()), e);
            throw new RuntimeException();
        }

        // make a choice for additional hh members if need be
        if (actualHhSize > MAX_MODEL_HH_SIZE)
        {

            String allMembersChosenPattern = applyModelForExtraHhMembers(householdObject,
                    firstFiveChosenName);

            String extraChar = "";
            int extraCharIndex = allMembersChosenPattern.indexOf("0");
            if (extraCharIndex > 0)
            {
                extraChar = "0";
            } else
            {
                extraCharIndex = allMembersChosenPattern.indexOf("j");
                if (extraCharIndex > 0) extraChar = "j";
            }

            allMembersChosenPattern = allMembersChosenPattern.substring(0, extraCharIndex)
                    + allMembersChosenPattern.substring(extraCharIndex + 1);

            // re-order the activities by the original order of persons
            String finalHhPattern = "";
            String[] finalHhPatternActivities = new String[cdapPersonArray.length];
            for (int i = 1; i < cdapPersonArray.length; i++)
            {
                int k = cdapPersonArray[i].getPersonNum();
                finalHhPatternActivities[k] = allMembersChosenPattern.substring(i - 1, i);
            }

            for (int i = 1; i < cdapPersonArray.length; i++)
                finalHhPattern += finalHhPatternActivities[i];

            String finalString = finalHhPattern + extraChar;
            if (householdObject.getDebugChoiceModels())
            {
                cdapLogger.info("final pattern (more than 5 hh members) = " + finalString);
                cdapLogger.info("");
                cdapLogger.info("");
            }
            return finalString;

        }

        if (householdObject.getDebugChoiceModels())
        {
            // reset the logger to what it was before we changed it
            LogitModel.setLogger(Logger.getLogger(LogitModel.class));
        }

        // no need to re-order the activities - hhsize <= MAX_MODEL_HH_SIZE have
        // original order of persons
        return firstFiveChosenName;

    }

    /**
     * Applies a simple choice from fixed proportions by person type for members
     * of households with more than 5 people who are not included in the CDAP
     * model. The choices of the additional household members are independent of
     * each other.
     * 
     * @param householdObject
     * @param patternStringForOtherHhMembers
     * @return the pattern for the entire household, including the 5-member
     *         pattern chosen by the logit model and the additional members
     *         chosen by the fixed-distribution model.
     * 
     */
    private String applyModelForExtraHhMembers(Household householdObject,
            String patternStringForOtherHhMembers)
    {

        String allMembersPattern = patternStringForOtherHhMembers;

        // get the persons not yet modeled
        Person[] personArray = getPersonsNotModeledByCdap(MAX_MODEL_HH_SIZE);

        // person array is 1-based to be consistent with other person arrays
        for (int i = 1; i < personArray.length; i++)
        {

            int personType = personArray[i].getPersonTypeNumber();

            // get choice index from fixed proportions for 6 plus persons
            int chosen = ChoiceModelApplication.getMonteCarloSelection(
                    fixedCumulativeProportions[personType], householdObject.getHhRandom()
                            .nextDouble());

            allMembersPattern += activityNameArray[chosen];

        }

        return allMembersPattern;

    }

    /**
     * Method reorders the persons in the household for use with the CDAP model,
     * which only explicitly models the interaction of five persons in a HH.
     * Priority in the reordering is first given to full time workers (up to
     * two), then to part time workers (up to two workers, of any type), then to
     * children (youngest to oldest, up to three). If the method is called for a
     * household with less than 5 people, the cdapPersonArray is the same as the
     * person array.
     * 
     */
    public void reOrderPersonsForCdap(Household household)
    {

        // set the person array
        Person[] persons = household.getPersons();

        // if hh is not too big, set cdap equal to persons and return
        int hhSize = household.getSize();
        if (hhSize <= MAX_MODEL_HH_SIZE)
        {
            cdapPersonArray = persons;
            return;
        }

        // create the end game array
        cdapPersonArray = new Person[persons.length];

        // keep track of which persons you count
        boolean[] iCountedYou = new boolean[persons.length];
        Arrays.fill(iCountedYou, false);

        // define the persons we want to find among the five
        int firstWorkerIndex = -99;
        int secondWorkerIndex = -99;

        int youngestChildIndex = -99;
        int secondYoungestChildIndex = -99;
        int thirdYoungestChildIndex = -99;

        int youngestChildAge = 99;
        int secondYoungestChildAge = 99;
        int thirdYoungestChildAge = 99;

        // first: full-time workers (persons is 1-based array)
        for (int i = 1; i < persons.length; ++i)
        {

            if (iCountedYou[i]) continue;

            // is the person a full-time worker
            if (persons[i].getPersonIsFullTimeWorker() == 1)
            {

                // check our indices
                if (firstWorkerIndex == -99)
                {
                    firstWorkerIndex = i;
                    iCountedYou[i] = true;
                } else if (secondWorkerIndex == -99)
                {
                    secondWorkerIndex = i;
                    iCountedYou[i] = true;
                }
            }

        } // i (full time workers)

        // second: part-time workers (only if we don't have two workers)
        if (firstWorkerIndex == -99 || secondWorkerIndex == -99)
        {

            for (int i = 1; i < persons.length; ++i)
            {

                if (iCountedYou[i]) continue;

                // is the person part-time worker
                if (persons[i].getPersonIsPartTimeWorker() == 1)
                {

                    // check our indices
                    if (firstWorkerIndex == -99)
                    {
                        firstWorkerIndex = i;
                        iCountedYou[i] = true;
                    } else if (secondWorkerIndex == -99)
                    {
                        secondWorkerIndex = i;
                        iCountedYou[i] = true;
                    }

                }

            } // i (part-time workers)
        }

        // third: youngest child loop
        for (int i = 1; i < persons.length; ++i)
        {

            if (iCountedYou[i]) continue;

            if (persons[i].getPersonIsPreschoolChild() == 1
                    || persons[i].getPersonIsStudentNonDriving() == 1
                    || persons[i].getPersonIsStudentDriving() == 1)
            {

                // check our indices
                if (youngestChildIndex == -99)
                {
                    youngestChildIndex = i;
                    youngestChildAge = persons[i].getAge();
                    iCountedYou[i] = true;
                } else
                {

                    // see if this child is younger than the one on record
                    int age = persons[i].getAge();
                    if (age < youngestChildAge)
                    {

                        // reset iCountedYou for previous child
                        iCountedYou[youngestChildIndex] = false;

                        // set variables for this child
                        youngestChildIndex = i;
                        youngestChildAge = age;
                        iCountedYou[i] = true;

                    }
                }

            } // if person is child

        } // i (youngest child loop)

        // fourth: second youngest child loop (skip if youngest child is not
        // filled)
        if (youngestChildIndex != -99)
        {

            for (int i = 1; i < persons.length; ++i)
            {

                if (iCountedYou[i]) continue;

                if (persons[i].getPersonIsPreschoolChild() == 1
                        || persons[i].getPersonIsStudentNonDriving() == 1
                        || persons[i].getPersonIsStudentDriving() == 1)
                {

                    // check our indices
                    if (secondYoungestChildIndex == -99)
                    {
                        secondYoungestChildIndex = i;
                        secondYoungestChildAge = persons[i].getAge();
                        iCountedYou[i] = true;
                    } else
                    {

                        // see if this child is younger than the one on record
                        int age = persons[i].getAge();
                        if (age < secondYoungestChildAge)
                        {

                            // reset iCountedYou for previous child
                            iCountedYou[secondYoungestChildIndex] = false;

                            // set variables for this child
                            secondYoungestChildIndex = i;
                            secondYoungestChildAge = age;
                            iCountedYou[i] = true;

                        }
                    }

                } // if person is child

            } // i (second youngest child loop)
        }

        // fifth: third youngest child loop (skip if second kid not included)
        if (secondYoungestChildIndex != -99)
        {

            for (int i = 1; i < persons.length; ++i)
            {

                if (iCountedYou[i]) continue;

                if (persons[i].getPersonIsPreschoolChild() == 1
                        || persons[i].getPersonIsStudentNonDriving() == 1
                        || persons[i].getPersonIsStudentDriving() == 1)
                {

                    // check our indices
                    if (thirdYoungestChildIndex == -99)
                    {
                        thirdYoungestChildIndex = i;
                        thirdYoungestChildAge = persons[i].getAge();
                        iCountedYou[i] = true;
                    } else
                    {

                        // see if this child is younger than the one on record
                        int age = persons[i].getAge();
                        if (age < thirdYoungestChildAge)
                        {

                            // reset iCountedYou for previous child
                            iCountedYou[thirdYoungestChildIndex] = false;

                            // set variables for this child
                            thirdYoungestChildIndex = i;
                            thirdYoungestChildAge = age;
                            iCountedYou[i] = true;

                        }
                    }

                } // if person is child

            } // i (third youngest child loop)
        }

        // assign any missing spots among the top 5 to random members
        int cdapPersonIndex;

        Random hhRandom = household.getHhRandom();

        int randomCount = household.getHhRandomCount();
        // when household.getHhRandom() was applied, the random count was
        // incremented, assuming a random number would be drawn right away.
        // so let's decrement by 1, then increment the count each time a random
        // number is actually drawn in this method.
        randomCount--;

        // first worker
        cdapPersonIndex = 1; // persons and cdapPersonArray are 1-based
        if (firstWorkerIndex == -99)
        {

            int randomIndex = (int) (hhRandom.nextDouble() * hhSize);
            randomCount++;
            while (iCountedYou[randomIndex] || randomIndex == 0)
            {
                randomIndex = (int) (hhRandom.nextDouble() * hhSize);
                randomCount++;
            }

            cdapPersonArray[cdapPersonIndex] = persons[randomIndex];
            iCountedYou[randomIndex] = true;

        } else
        {
            cdapPersonArray[cdapPersonIndex] = persons[firstWorkerIndex];
        }

        // second worker
        cdapPersonIndex = 2;
        if (secondWorkerIndex == -99)
        {

            int randomIndex = (int) (hhRandom.nextDouble() * hhSize);
            randomCount++;
            while (iCountedYou[randomIndex] || randomIndex == 0)
            {
                randomIndex = (int) (hhRandom.nextDouble() * hhSize);
                randomCount++;
            }

            cdapPersonArray[cdapPersonIndex] = persons[randomIndex];
            iCountedYou[randomIndex] = true;

        } else
        {
            cdapPersonArray[cdapPersonIndex] = persons[secondWorkerIndex];
        }

        // youngest child
        cdapPersonIndex = 3;
        if (youngestChildIndex == -99)
        {

            int randomIndex = (int) (hhRandom.nextDouble() * hhSize);
            randomCount++;
            while (iCountedYou[randomIndex] || randomIndex == 0)
            {
                randomIndex = (int) (hhRandom.nextDouble() * hhSize);
                randomCount++;
            }

            cdapPersonArray[cdapPersonIndex] = persons[randomIndex];
            iCountedYou[randomIndex] = true;

        } else
        {
            cdapPersonArray[cdapPersonIndex] = persons[youngestChildIndex];
        }

        // second youngest child
        cdapPersonIndex = 4;
        if (secondYoungestChildIndex == -99)
        {

            int randomIndex = (int) (hhRandom.nextDouble() * hhSize);
            randomCount++;
            while (iCountedYou[randomIndex] || randomIndex == 0)
            {
                randomIndex = (int) (hhRandom.nextDouble() * hhSize);
                randomCount++;
            }

            cdapPersonArray[cdapPersonIndex] = persons[randomIndex];
            iCountedYou[randomIndex] = true;

        } else
        {
            cdapPersonArray[cdapPersonIndex] = persons[secondYoungestChildIndex];
        }

        // third youngest child
        cdapPersonIndex = 5;
        if (thirdYoungestChildIndex == -99)
        {

            int randomIndex = (int) (hhRandom.nextDouble() * hhSize);
            randomCount++;
            while (iCountedYou[randomIndex] || randomIndex == 0)
            {
                randomIndex = (int) (hhRandom.nextDouble() * hhSize);
                randomCount++;
            }

            cdapPersonArray[cdapPersonIndex] = persons[randomIndex];
            iCountedYou[randomIndex] = true;

        } else
        {
            cdapPersonArray[cdapPersonIndex] = persons[thirdYoungestChildIndex];
        }

        // fill spots outside the top 5
        cdapPersonIndex = 6;
        for (int i = 1; i < persons.length; ++i)
        {

            if (iCountedYou[i]) continue;

            cdapPersonArray[cdapPersonIndex] = persons[i];
            cdapPersonIndex++;
        }

        household.setHhRandomCount(randomCount);

    }

    protected Person getCdapPerson(int persNum)
    {
        if (persNum < 1 || persNum > cdapPersonArray.length - 1)
        {
            logger.fatal(String.format("persNum value = %d is out of range for hhSize = %d",
                    persNum, cdapPersonArray.length - 1));
            throw new RuntimeException();
        }

        return cdapPersonArray[persNum];
    }

    /**
     * Method returns an array of persons not modeled by the CDAP model (i.e.
     * persons 6 to X, when ordered by the reOrderPersonsForCdap method
     * 
     * @param personsModeledByCdap
     * @return
     */
    public Person[] getPersonsNotModeledByCdap(int personsModeledByCdap)
    {

        // create a 1-based person array to be consistent
        Person[] personArray = null;
        if (cdapPersonArray.length > personsModeledByCdap + 1) personArray = new Person[cdapPersonArray.length
                - personsModeledByCdap];
        else personArray = new Person[0];

        for (int i = 1; i < personArray.length; ++i)
        {
            personArray[i] = cdapPersonArray[personsModeledByCdap + i];
        }

        return personArray;

    }

    /**
     * Returns true if this CDAP person number (meaning the number of persons
     * for the purposes of the CDAP model) is an adult; false if not.
     * 
     * @param cdapPersonNumber
     * @return
     */
    public boolean isThisCdapPersonAnAdult(int cdapPersonNumber)
    {

        if (cdapPersonArray[cdapPersonNumber].getPersonIsAdult() == 1) return true;
        return false;
    }

    public int getStudentTypeForThisCdapPerson(int cdapPersonNumber)
    {

        Person p = cdapPersonArray[cdapPersonNumber];
        int type = 0;
        if (p.getPersonIsPreschoolChild() == 1) type = 1;
        else if (p.getPersonIsGradeSchool() == 1) type = 2;
        else if (p.getPersonIsHighSchool() == 1) type = 3;
        else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() < 30) type = 4;
        else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() >= 30) type = 5;

        return type;
    }

    public int getStudentTypeForThisCdapPerson(Person p)
    {

        int type = 0;
        if (p.getPersonIsPreschoolChild() == 1) type = 1;
        else if (p.getPersonIsGradeSchool() == 1) type = 2;
        else if (p.getPersonIsHighSchool() == 1) type = 3;
        else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() < 30) type = 4;
        else if (p.getPersonIsUniversityStudent() == 1 && p.getAge() >= 30) type = 5;

        return type;
    }

    public int getWorkLocationForThisCdapPerson(int cdapPersonNumber)
    {
        int loc = cdapPersonArray[cdapPersonNumber].getUsualWorkLocation();
        if (loc > 0 && loc != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR) return loc;
        else return 0;
    }

    public int getSchoolLocationForThisCdapPerson(int cdapPersonNumber)
    {
        return cdapPersonArray[cdapPersonNumber].getUsualSchoolLocation();
    }

}
