package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class TelecommuteModel
        implements Serializable
{

    private transient Logger          logger                           = Logger.getLogger("tc");

    private static final String       TC_CONTROL_FILE_TARGET           = "te.uec.file";
    private static final String       TC_DATA_SHEET_TARGET             = "te.data.page";
    private static final String       TC_MODEL_SHEET_TARGET            = "te.model.page";

    public static final short           TC_MODEL_NO_TC_CHOICE            = -1;
    public static final short           TC_MODEL_NO_TELECOMMUTE          = 1;
    public static final short           TC_MODEL_1_3_DAYS_MONTH_CHOICE   = 2;
    public static final short           TC_MODEL_1_DAY_WEEK_CHOICE       = 3;
    public static final short           TC_MODEL_2_3_DAYS_WEEK_CHOICE    = 4;
    public static final short           TC_MODEL_4P_DAYS_WEEK_CHOICE     = 5;
    

    private MgraDataManager           mgraManager;

    private ChoiceModelApplication    tcModel;
    private TelecommuteDMU tcDmuObject;

    public TelecommuteModel(HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory)
    {
        mgraManager = MgraDataManager.getInstance(propertyMap);
        setupTelecommuteChoiceModelApplication(propertyMap, dmuFactory);
    }

    private void setupTelecommuteChoiceModelApplication(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {
        logger.info("Setting up telecommute choice model.");

        // locate the telecommute UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tcUecFile = uecFileDirectory + propertyMap.get(TC_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TC_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TC_MODEL_SHEET_TARGET);

        // create the telecommute model DMU object.
        tcDmuObject = dmuFactory.getTelecommuteDMU();

        // create the telecommute model object
        tcModel = new ChoiceModelApplication(tcUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) tcDmuObject);

    }

    public void applyModel(Household hhObject)
    {

        Random hhRandom = hhObject.getHhRandom();

        // person array is 1-based
        Person[] person = hhObject.getPersons();
        for (int i = 1; i < person.length; i++)
        {

            int workLoc = person[i].getWorkLocation();
            if (workLoc == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
            {

                person[i].setTelecommuteChoice(TC_MODEL_NO_TC_CHOICE);
     
            } else if (workLoc > 0 )
            {

                double randomNumber = hhRandom.nextDouble();
                short chosen = (short) getTelecommuteChoice(person[i], randomNumber);
                person[i].setTelecommuteChoice(chosen);

               
            } else
            {

                person[i].setTelecommuteChoice(TC_MODEL_NO_TC_CHOICE);
            
            }
        }

        hhObject.setFpRandomCount(hhObject.getHhRandomCount());
    }

    private int getTelecommuteChoice(Person personObj, double randomNumber)
    {

        // get the corresponding household object
        Household hhObj = personObj.getHouseholdObject();
        tcDmuObject.setPersonObject(personObj);

        // set the zone and dest attributes to the person's work location
        tcDmuObject.setDmuIndexValues(hhObj.getHhId(), personObj.getWorkLocation(),
                hhObj.getHhTaz(), personObj.getWorkLocation());

        // compute utilities and choose telecommute alternative.
        float logsum = (float) tcModel.computeUtilities(tcDmuObject, tcDmuObject.getDmuIndexValues());
        personObj.setTelecommuteLogsum(logsum);
        
        // if the choice model has at least one available alternative, make
        // choice.
        int chosenAlt;
        if (tcModel.getAvailabilityCount() > 0)
        {
            chosenAlt = tcModel.getChoiceResult(randomNumber);
        } else
        {
            String decisionMaker = String.format("HHID=%d, PERSID=%d", hhObj.getHhId(),
                    personObj.getPersonId());
            String errorMessage = String
                    .format("Exception caught for %s, no available telecommute alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
            logger.error(errorMessage);

            tcModel.logUECResults(logger, decisionMaker);
            throw new RuntimeException();
        }

        // write choice model alternative info to log file
        if (hhObj.getDebugChoiceModels())
        {
            String decisionMaker = String.format("HHID=%d, PERSID=%d", hhObj.getHhId(),
                    personObj.getPersonId());
            tcModel.logAlternativesInfo("Telecommute Choice", decisionMaker, logger);
            logger.info(String.format("%s result chosen for %s is %d with rn %.8f",
                    "Telecommute Choice", decisionMaker, chosenAlt, randomNumber));
            tcModel.logUECResults(logger, decisionMaker);
        }

        return chosenAlt;
    }

}
