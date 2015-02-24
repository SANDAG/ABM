package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import com.pb.common.calculator.VariableTable;
import com.pb.common.model.ModelException;
import com.pb.common.newmodel.ChoiceModelApplication;

public class HouseholdAutoOwnershipModel
        implements Serializable
{

    private transient Logger                   logger                 = Logger.getLogger(HouseholdAutoOwnershipModel.class);
    private transient Logger                   aoLogger               = Logger.getLogger("ao");

    private static final String                AO_CONTROL_FILE_TARGET = "ao.uec.file";
    private static final String                AO_MODEL_SHEET_TARGET  = "ao.model.page";
    private static final String                AO_DATA_SHEET_TARGET   = "ao.data.page";

    private static final int                   AUTO_LOGSUM_INDEX      = 6;
    private static final int                   TRANSIT_LOGSUM_INDEX   = 8;
    private static final int                   DT_RAIL_PROP_INDEX     = 10;

    private AccessibilitiesTable               accTable;
    private MandatoryAccessibilitiesCalculator mandAcc;
    private ChoiceModelApplication             aoModel;
    private AutoOwnershipChoiceDMU             aoDmuObject;

    public HouseholdAutoOwnershipModel(HashMap<String, String> rbMap,
            CtrampDmuFactoryIf dmuFactory, AccessibilitiesTable myAccTable,
            MandatoryAccessibilitiesCalculator myMandAcc)
    {

        logger.info("setting up AO choice model.");

        // set the aggAcc class variable, which will serve as a flag: null -> no
        // accessibilities, !null -> set accessibilities.
        // if the BuildAccessibilities object is null, the AO utility does not
        // need
        // to use the accessibilities components.
        accTable = myAccTable;
        mandAcc = myMandAcc;

        // locate the auto ownership UEC
        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String autoOwnershipUecFile = rbMap.get(AO_CONTROL_FILE_TARGET);
        autoOwnershipUecFile = uecPath + autoOwnershipUecFile;

        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, AO_DATA_SHEET_TARGET);
        int modelPage = Util.getIntegerValueFromPropertyMap(rbMap, AO_MODEL_SHEET_TARGET);

        // create the auto ownership choice model DMU object.
        aoDmuObject = dmuFactory.getAutoOwnershipDMU();

        // create the auto ownership choice model object
        aoModel = new ChoiceModelApplication(autoOwnershipUecFile, modelPage, dataPage, rbMap,
                (VariableTable) aoDmuObject);

    }

    /**
     * Set the dmu attributes, compute the pre-AO or AO utilities, and select an
     * alternative
     * 
     * @param hhObj
     *            for which to apply thye model
     * @param preAutoOwnership
     *            is true if running pre-auto ownership, or false to run primary
     *            auto ownership model.
     */

    public void applyModel(Household hhObj, boolean preAutoOwnership)
    {

        // update the AO dmuObject for this hh
        aoDmuObject.setHouseholdObject(hhObj);
        aoDmuObject.setDmuIndexValues(hhObj.getHhId(), hhObj.getHhMgra(), hhObj.getHhMgra(), 0);

        // set the non-mandatory accessibility values for the home MGRA.
        // values used by both pre-ao and ao models.
        aoDmuObject.setHomeTazAutoAccessibility(accTable.getAggregateAccessibility("auto",
                hhObj.getHhMgra()));
        aoDmuObject.setHomeTazTransitAccessibility(accTable.getAggregateAccessibility("transit",
                hhObj.getHhMgra()));
        aoDmuObject.setHomeTazNonMotorizedAccessibility(accTable.getAggregateAccessibility(
                "nonmotor", hhObj.getHhMgra()));

        if (preAutoOwnership)
        {

            aoDmuObject.setUseAccessibilities(false);

        } else
        {

            aoDmuObject.setUseAccessibilities(true);

            // compute the disaggregate accessibilities for the home MGRA to
            // work and
            // school MGRAs summed accross workers and students
            double workAutoDependency = 0.0;
            double schoolAutoDependency = 0.0;
            double workRailProp = 0.0;
            double schoolRailProp = 0.0;
            Person[] persons = hhObj.getPersons();
            for (int i = 1; i < persons.length; i++)
            {

                // sum over all workers (full time or part time)
                if (persons[i].getPersonIsWorker() == 1)
                {

                    int workMgra = persons[i].getWorkLocation();
                    if (workMgra != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                    {

                        // Non-Motorized Factor = 0.5*MIN(MAX(DIST,1),3)-0.5
                        // if 0 <= dist < 1, nmFactor = 0
                        // if 1 <= dist <= 3, nmFactor = [0.0, 1.0]
                        // if 3 <= dist, nmFactor = 1.0
                        double nmFactor = 0.5 * (Math.min(
                                Math.max(persons[i].getWorkLocationDistance(), 1.0), 3.0)) - 0.5;

                        // if auto logsum < transit logsum, do not accumulate
                        // auto
                        // dependency
                        double[] workerAccessibilities = mandAcc
                                .calculateWorkerMandatoryAccessibilities(hhObj.getHhMgra(),
                                        workMgra);
                        if (workerAccessibilities[AUTO_LOGSUM_INDEX] >= workerAccessibilities[TRANSIT_LOGSUM_INDEX])
                        {
                            double logsumDiff = workerAccessibilities[AUTO_LOGSUM_INDEX]
                                    - workerAccessibilities[TRANSIT_LOGSUM_INDEX];

                            // need to scale and cap logsum difference
                            logsumDiff = Math.min(logsumDiff / 3.0, 1.0);
                            workAutoDependency += (logsumDiff * nmFactor);
                        }

                        workRailProp += workerAccessibilities[DT_RAIL_PROP_INDEX];

                    }

                }

                // sum over all students of driving age
                if (persons[i].getPersonIsUniversityStudent() == 1
                        || persons[i].getPersonIsStudentDriving() == 1)
                {

                    int schoolMgra = persons[i].getUsualSchoolLocation();
                    if (schoolMgra != ModelStructure.NOT_ENROLLED_SEGMENT_INDEX)
                    {

                        // Non-Motorized Factor = 0.5*MIN(MAX(DIST,1),3)-0.5
                        // if 0 <= dist < 1, nmFactor = 0
                        // if 1 <= dist <= 3, nmFactor = [0.0, 1.0]
                        // if 3 <= dist, nmFactor = 1.0
                        double nmFactor = 0.5 * (Math.min(
                                Math.max(persons[i].getWorkLocationDistance(), 1.0), 3.0)) - 0.5;

                        // if auto logsum < transit logsum, do not accumulate
                        // auto
                        // dependency
                        double[] studentAccessibilities = mandAcc
                                .calculateStudentMandatoryAccessibilities(hhObj.getHhMgra(),
                                        schoolMgra);
                        if (studentAccessibilities[AUTO_LOGSUM_INDEX] >= studentAccessibilities[TRANSIT_LOGSUM_INDEX])
                        {
                            double logsumDiff = studentAccessibilities[AUTO_LOGSUM_INDEX]
                                    - studentAccessibilities[TRANSIT_LOGSUM_INDEX];

                            // need to scale and cap logsum difference
                            logsumDiff = Math.min(logsumDiff / 3.0, 1.0);
                            schoolAutoDependency += (logsumDiff * nmFactor);
                        }

                        schoolRailProp += studentAccessibilities[DT_RAIL_PROP_INDEX];

                    }
                }

            }

            aoDmuObject.setWorkAutoDependency(workAutoDependency);
            aoDmuObject.setSchoolAutoDependency(schoolAutoDependency);

            aoDmuObject.setWorkersRailProportion(workRailProp);
            aoDmuObject.setStudentsRailProportion(schoolRailProp);

        }

        // compute utilities and choose auto ownership alternative.
        aoModel.computeUtilities(aoDmuObject, aoDmuObject.getDmuIndexValues());

        Random hhRandom = hhObj.getHhRandom();
        int randomCount = hhObj.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosenAlt = -1;
        if (aoModel.getAvailabilityCount() > 0)
        {
            try
            {
                chosenAlt = aoModel.getChoiceResult(rn);
            } catch (ModelException e)
            {
                logger.error(String.format(
                        "exception caught for HHID=%d in choiceModelApplication.", hhObj.getHhId()));
            }
        } else
        {
            logger.error(String
                    .format("error: HHID=%d has no available auto ownership alternatives to choose from in choiceModelApplication.",
                            hhObj.getHhId()));
            throw new RuntimeException();
        }

        // write choice model alternative info to log file
        if (hhObj.getDebugChoiceModels() || chosenAlt < 0)
        {

            String loggerString = (preAutoOwnership ? "Pre-AO without" : "AO with")
                    + " accessibilities, Household " + hhObj.getHhId() + " Object";
            hhObj.logHouseholdObject(loggerString, aoLogger);

            double[] utilities = aoModel.getUtilities();
            double[] probabilities = aoModel.getProbabilities();

            aoLogger.info("Alternative                    Utility       Probability           CumProb");
            aoLogger.info("--------------------   ---------------      ------------      ------------");

            double cumProb = 0.0;
            for (int k = 0; k < aoModel.getNumberOfAlternatives(); k++)
            {
                cumProb += probabilities[k];
                aoLogger.info(String.format("%-20s%18.6e%18.6e%18.6e", k + " autos", utilities[k],
                        probabilities[k], cumProb));
            }

            aoLogger.info(" ");
            aoLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", chosenAlt, rn,
                    randomCount));

            aoLogger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            aoLogger.info("");
            aoLogger.info("");

            // write choice model alternative info to debug log file
            aoModel.logAlternativesInfo("Household Auto Ownership Choice",
                    String.format("HH_%d", hhObj.getHhId()));
            aoModel.logSelectionInfo("Household Auto Ownership Choice",
                    String.format("HH_%d", hhObj.getHhId()), rn, chosenAlt);

            // write UEC calculation results to separate model specific log file
            aoModel.logUECResults(aoLogger,
                    String.format("Household Auto Ownership Choice, HH_%d", hhObj.getHhId()));
        }

        if (preAutoOwnership) hhObj.setPreAoRandomCount(hhObj.getHhRandomCount());
        else hhObj.setAoRandomCount(hhObj.getHhRandomCount());

        hhObj.setAutoOwnershipModelResult(chosenAlt);

    }

}
