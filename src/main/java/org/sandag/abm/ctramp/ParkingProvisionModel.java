package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class ParkingProvisionModel
        implements Serializable
{

    private transient Logger          logger                           = Logger.getLogger("fp");

    private static final String       FP_CONTROL_FILE_TARGET           = "fp.uec.file";
    private static final String       FP_DATA_SHEET_TARGET             = "fp.data.page";
    private static final String       FP_MODEL_SHEET_TARGET            = "fp.model.page";

    public static final int           FP_MODEL_NO_REIMBURSEMENT_CHOICE = -1;
    public static final int           FP_MODEL_FREE_ALT                = 1;
    public static final int           FP_MODEL_PAY_ALT                 = 2;
    public static final int           FP_MODEL_REIMB_ALT               = 3;

    private static final String       REIMBURSEMENT_MEAN               = "park.cost.reimb.mean";
    private static final String       REIMBURSEMENT_STD_DEV            = "park.cost.reimb.std.dev";

    private MgraDataManager           mgraManager;

    private double                    meanReimb;
    private double                    stdDevReimb;

    private int[]                     mgraParkArea;
    private int[]                     numfreehrs;
    private int[]                     hstallsoth;
    private int[]                     hstallssam;
    private float[]                   hparkcost;
    private int[]                     dstallsoth;
    private int[]                     dstallssam;
    private float[]                   dparkcost;
    private int[]                     mstallsoth;
    private int[]                     mstallssam;
    private float[]                   mparkcost;

    private double[]                  lsWgtAvgCostM;
    private double[]                  lsWgtAvgCostD;
    private double[]                  lsWgtAvgCostH;

    private ChoiceModelApplication    fpModel;
    private ParkingProvisionChoiceDMU fpDmuObject;

    public ParkingProvisionModel(HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory)
    {
        mgraManager = MgraDataManager.getInstance(propertyMap);
        setupFreeParkingChoiceModelApplication(propertyMap, dmuFactory);
    }

    private void setupFreeParkingChoiceModelApplication(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {
        logger.info("setting up free parking choice model.");

        // locate the free parking UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String fpUecFile = uecFileDirectory + propertyMap.get(FP_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, FP_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, FP_MODEL_SHEET_TARGET);

        // create the auto ownership choice model DMU object.
        fpDmuObject = dmuFactory.getFreeParkingChoiceDMU();

        // create the auto ownership choice model object
        fpModel = new ChoiceModelApplication(fpUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) fpDmuObject);

        meanReimb = Float.parseFloat(propertyMap.get(REIMBURSEMENT_MEAN));
        stdDevReimb = Float.parseFloat(propertyMap.get(REIMBURSEMENT_STD_DEV));

        mgraParkArea = mgraManager.getMgraParkAreas();
        numfreehrs = mgraManager.getNumFreeHours();
        lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();
        mstallsoth = mgraManager.getMStallsOth();
        mstallssam = mgraManager.getMStallsSam();
        mparkcost = mgraManager.getMParkCost();
        dstallsoth = mgraManager.getDStallsOth();
        dstallssam = mgraManager.getDStallsSam();
        dparkcost = mgraManager.getDParkCost();
        hstallsoth = mgraManager.getHStallsOth();
        hstallssam = mgraManager.getHStallsSam();
        hparkcost = mgraManager.getHParkCost();

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

                person[i].setFreeParkingAvailableResult(FP_MODEL_NO_REIMBURSEMENT_CHOICE);
                person[i].setParkingReimbursement(FP_MODEL_NO_REIMBURSEMENT_CHOICE);

            } else if (workLoc > 0 && mgraParkArea[workLoc] == MgraDataManager.PARK_AREA_ONE)
            {

                double randomNumber = hhRandom.nextDouble();
                int chosen = getParkingChoice(person[i], randomNumber);
                person[i].setFreeParkingAvailableResult(chosen);

                if (chosen == FP_MODEL_REIMB_ALT)
                {
                    double logReimbPct = meanReimb + hhRandom.nextGaussian() * stdDevReimb;
                    person[i].setParkingReimbursement(Math.exp(logReimbPct));
                } else if (chosen == FP_MODEL_FREE_ALT)
                {
                    person[i].setParkingReimbursement(0.0);
                } else if (chosen == FP_MODEL_PAY_ALT)
                {
                    person[i].setParkingReimbursement(0.0);
                }

            } else
            {

                person[i].setFreeParkingAvailableResult(FP_MODEL_NO_REIMBURSEMENT_CHOICE);
                person[i].setParkingReimbursement(0.0);

            }
        }

        hhObject.setFpRandomCount(hhObject.getHhRandomCount());
    }

    private int getParkingChoice(Person personObj, double randomNumber)
    {

        // get the corresponding household object
        Household hhObj = personObj.getHouseholdObject();
        fpDmuObject.setPersonObject(personObj);

        fpDmuObject.setMgraParkArea(mgraParkArea[personObj.getWorkLocation()]);
        fpDmuObject.setNumFreeHours(numfreehrs[personObj.getWorkLocation()]);
        fpDmuObject.setLsWgtAvgCostM(lsWgtAvgCostM[personObj.getWorkLocation()]);
        fpDmuObject.setLsWgtAvgCostD(lsWgtAvgCostD[personObj.getWorkLocation()]);
        fpDmuObject.setLsWgtAvgCostH(lsWgtAvgCostH[personObj.getWorkLocation()]);
        fpDmuObject.setMStallsOth(mstallsoth[personObj.getWorkLocation()]);
        fpDmuObject.setMStallsSam(mstallssam[personObj.getWorkLocation()]);
        fpDmuObject.setMParkCost(mparkcost[personObj.getWorkLocation()]);
        fpDmuObject.setDStallsSam(dstallssam[personObj.getWorkLocation()]);
        fpDmuObject.setDStallsOth(dstallsoth[personObj.getWorkLocation()]);
        fpDmuObject.setDParkCost(dparkcost[personObj.getWorkLocation()]);
        fpDmuObject.setHStallsOth(hstallsoth[personObj.getWorkLocation()]);
        fpDmuObject.setHStallsSam(hstallssam[personObj.getWorkLocation()]);
        fpDmuObject.setHParkCost(hparkcost[personObj.getWorkLocation()]);

        // set the zone and dest attributes to the person's work location
        fpDmuObject.setDmuIndexValues(hhObj.getHhId(), personObj.getWorkLocation(),
                hhObj.getHhTaz(), personObj.getWorkLocation());

        // compute utilities and choose auto ownership alternative.
        fpModel.computeUtilities(fpDmuObject, fpDmuObject.getDmuIndexValues());

        // if the choice model has at least one available alternative, make
        // choice.
        int chosenAlt;
        if (fpModel.getAvailabilityCount() > 0)
        {
            chosenAlt = fpModel.getChoiceResult(randomNumber);
        } else
        {
            String decisionMaker = String.format("HHID=%d, PERSID=%d", hhObj.getHhId(),
                    personObj.getPersonId());
            String errorMessage = String
                    .format("Exception caught for %s, no available free parking alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
            logger.error(errorMessage);

            fpModel.logUECResults(logger, decisionMaker);
            throw new RuntimeException();
        }

        // write choice model alternative info to log file
        if (hhObj.getDebugChoiceModels())
        {
            String decisionMaker = String.format("HHID=%d, PERSID=%d", hhObj.getHhId(),
                    personObj.getPersonId());
            fpModel.logAlternativesInfo("Free parking Choice", decisionMaker, logger);
            logger.info(String.format("%s result chosen for %s is %d with rn %.8f",
                    "Free parking Choice", decisionMaker, chosenAlt, randomNumber));
            fpModel.logUECResults(logger, decisionMaker);
        }

        return chosenAlt;
    }

}
