package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class InternalExternalTripChoiceModel
        implements Serializable
{

    private transient Logger              logger                 = Logger.getLogger("ie");

    private static final String           IE_CONTROL_FILE_TARGET = "ie.uec.file";
    private static final String           IE_DATA_SHEET_TARGET   = "ie.data.page";
    private static final String           IE_MODEL_SHEET_TARGET  = "ie.model.page";

    public static final int               IE_MODEL_NO_ALT        = 1;
    public static final int               IE_MODEL_YES_ALT       = 2;

    private ChoiceModelApplication        ieModel;
    private InternalExternalTripChoiceDMU ieDmuObject;

    public InternalExternalTripChoiceModel(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

        logger.info("setting up internal-external trip choice model.");

        // locate the IE choice UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String ieUecFile = uecFileDirectory + propertyMap.get(IE_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, IE_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, IE_MODEL_SHEET_TARGET);

        // create the choice model DMU object.
        ieDmuObject = dmuFactory.getInternalExternalTripChoiceDMU();

        // create the choice model object
        ieModel = new ChoiceModelApplication(ieUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) ieDmuObject);

    }

    public void applyModel(Household hhObject, double[] distanceToCordonsLogsums)
    {

        int homeTaz = hhObject.getHhTaz();
        ieDmuObject.setDistanceToCordonsLogsum(distanceToCordonsLogsums[homeTaz]);

        ieDmuObject.setHouseholdObject(hhObject);
        double vehiclesPerHouseholdMember = hhObject.getAutoOwnershipModelResult()
                / hhObject.getHhSize();
        ieDmuObject.setVehiclesPerHouseholdMember(vehiclesPerHouseholdMember);

        Random hhRandom = hhObject.getHhRandom();

        // person array is 1-based
        Person[] person = hhObject.getPersons();
        for (int i = 1; i < person.length; i++)
        {

            ieDmuObject.setPersonObject(person[i]);
            ieDmuObject.setDmuIndexValues(hhObject.getHhId(), hhObject.getHhTaz());

            double randomNumber = hhRandom.nextDouble();

            // compute utilities and choose alternative.
            ieModel.computeUtilities(ieDmuObject, ieDmuObject.getDmuIndexValues());

            // if the choice model has at least one available alternative, make
            // choice.
            int chosenAlt;
            if (ieModel.getAvailabilityCount() > 0)
            {
                chosenAlt = ieModel.getChoiceResult(randomNumber);
            } else
            {
                String decisionMaker = String.format("HHID=%d, PersonNum=%d", hhObject.getHhId(),
                        person[i].getPersonNum());
                String errorMessage = String
                        .format("Exception caught for %s, no available internal-external trip choice alternatives to choose from in choiceModelApplication.",
                                decisionMaker);
                logger.error(errorMessage);

                ieModel.logUECResults(logger, decisionMaker);
                throw new RuntimeException();
            }

            // write choice model alternative info to log file
            if (hhObject.getDebugChoiceModels())
            {
                String decisionMaker = String.format("HHID=%d, PersonNum=%d", hhObject.getHhId(),
                        person[i].getPersonNum());
                ieModel.logAlternativesInfo("Internal-External Trip Choice", decisionMaker, logger);
                logger.info(String.format("%s result chosen for %s is %d with rn %.8f",
                        "Internal-External Trip Choice", decisionMaker, chosenAlt, randomNumber));
                ieModel.logUECResults(logger, decisionMaker);
            }

            person[i].setInternalExternalTripChoiceResult(chosenAlt);

        }

        hhObject.setIeRandomCount(hhObject.getHhRandomCount());

    }

}
