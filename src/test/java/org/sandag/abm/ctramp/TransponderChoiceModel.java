package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class TransponderChoiceModel
        implements Serializable
{

    private transient Logger       logger                 = Logger.getLogger("tp");

    private static final String    TP_CONTROL_FILE_TARGET = "tc.uec.file";
    private static final String    TP_DATA_SHEET_TARGET   = "tc.data.page";
    private static final String    TP_MODEL_SHEET_TARGET  = "tc.model.page";

    public static final int        TP_MODEL_NO_ALT        = 1;
    public static final int        TP_MODEL_YES_ALT       = 2;

    private ChoiceModelApplication tpModel;
    private TransponderChoiceDMU   tpDmuObject;

    private AccessibilitiesTable   accTable;

    private double[]               pctHighIncome;
    private double[]               pctMultipleAutos;
    private double[]               avgtts;
    private double[]               transpDist;
    private double[]               pctDetour;

    public TransponderChoiceModel(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory, AccessibilitiesTable accTable, double[] pctHighIncome,
            double[] pctMultipleAutos, double[] avgtts, double[] transpDist, double[] pctDetour)
    {
        this.accTable = accTable;
        this.pctHighIncome = pctHighIncome;
        this.pctMultipleAutos = pctMultipleAutos;
        this.avgtts = avgtts;
        this.transpDist = transpDist;
        this.pctDetour = pctDetour;

        setupTransponderChoiceModelApplication(propertyMap, dmuFactory);
    }

    private void setupTransponderChoiceModelApplication(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {
        logger.info("setting up transponder choice model.");

        // locate the transponder choice UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tpUecFile = uecFileDirectory + propertyMap.get(TP_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TP_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TP_MODEL_SHEET_TARGET);

        // create the transponder choice model DMU object.
        tpDmuObject = dmuFactory.getTransponderChoiceDMU();

        // create the transponder choice model object
        tpModel = new ChoiceModelApplication(tpUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) tpDmuObject);

    }

    public void applyModel(Household hhObject)
    {

        int homeTaz = hhObject.getHhTaz();

        tpDmuObject.setHouseholdObject(hhObject);

        // set the zone, orig and dest attributes
        tpDmuObject.setDmuIndexValues(hhObject.getHhId(), hhObject.getHhTaz(), hhObject.getHhTaz(),
                0);

        tpDmuObject.setPctIncome100Kplus(pctHighIncome[homeTaz]);
        tpDmuObject.setPctTazMultpleAutos(pctMultipleAutos[homeTaz]);
        tpDmuObject.setExpectedTravelTimeSavings(avgtts[homeTaz]);
        tpDmuObject.setTransponderDistance(transpDist[homeTaz]);
        tpDmuObject.setPctDetour(pctDetour[homeTaz]);

        float accessibility = accTable.getAggregateAccessibility("transit", hhObject.getHhMgra());
        tpDmuObject.setAccessibility(accessibility);

        Random hhRandom = hhObject.getHhRandom();
        double randomNumber = hhRandom.nextDouble();

        // compute utilities and choose transponder choice alternative.
        float logsum = (float) tpModel.computeUtilities(tpDmuObject, tpDmuObject.getDmuIndexValues());

        hhObject.setTransponderLogsum(logsum);
        
        // if the choice model has at least one available alternative, make
        // choice.
        int chosenAlt;
        if (tpModel.getAvailabilityCount() > 0)
        {
            chosenAlt = tpModel.getChoiceResult(randomNumber);
        } else
        {
            String decisionMaker = String.format("HHID=%d", hhObject.getHhId());
            String errorMessage = String
                    .format("Exception caught for %s, no available transponder choice alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
            logger.error(errorMessage);

            tpModel.logUECResults(logger, decisionMaker);
            throw new RuntimeException();
        }

        // write choice model alternative info to log file
        if (hhObject.getDebugChoiceModels())
        {
            String decisionMaker = String.format("HHID=%d", hhObject.getHhId());
            tpModel.logAlternativesInfo("Transponder Choice", decisionMaker, logger);
            logger.info(String.format("%s result chosen for %s is %d with rn %.8f",
                    "Transponder Choice", decisionMaker, chosenAlt, randomNumber));
            tpModel.logUECResults(logger, decisionMaker);
        }

        hhObject.setTpChoice(chosenAlt - 1);

        hhObject.setTpRandomCount(hhObject.getHhRandomCount());
    }

}
