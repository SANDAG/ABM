package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.ResourceBundle;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.application.SandagCtrampDmuFactory;
import org.sandag.abm.application.SandagHouseholdDataManager;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.JointTourModelsDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 11, 2008 Time: 9:25:30 AM To change
 * this template use File | Settings | File Templates.
 */
public class JointTourModels implements Serializable
{

    private transient Logger       logger                                  = Logger.getLogger(JointTourModels.class);
    private transient Logger       tourFreq                                = Logger.getLogger("tourFreq");

    // these are public because CtrampApplication creates a ChoiceModelAppplication in order to get the alternatives for a log report
    public static final String     UEC_FILE_PROPERTIES_TARGET              = "jtfcp.uec.file";
    public static final String     UEC_DATA_PAGE_TARGET                    = "jtfcp.data.page";
    public static final String     UEC_JOINT_TOUR_FREQ_COMP_MODEL_PAGE     = "jtfcp.freq.comp.page";
    
    private static final String    UEC_JOINT_TOUR_PARTIC_MODEL_PAGE        = "jtfcp.participate.page";

    public static final int        JOINT_TOUR_COMPOSITION_ADULTS           = 1;
    public static final int        JOINT_TOUR_COMPOSITION_CHILDREN         = 2;
    public static final int        JOINT_TOUR_COMPOSITION_MIXED            = 3;

    public static final String[]   JOINT_TOUR_COMPOSITION_NAMES            = {"", "adult", "child", "mixed"};

    public static final int        PURPOSE_1_FIELD                         = 2;
    public static final int        PURPOSE_2_FIELD                         = 3;
    public static final int        PARTY_1_FIELD                           = 4;
    public static final int        PARTY_2_FIELD                           = 5;
    
    
    // DMU for the UEC
    private JointTourModelsDMU     dmuObject;
    private AccessibilitiesTable accTable;
    
    private ChoiceModelApplication jointTourFrequencyModel;
    private ChoiceModelApplication jointTourParticipation;
    private TableDataSet jointModelsAltsTable;
    private HashMap<Integer,String> purposeIndexNameMap;
    

    private int[]                  invalidCount                            = new int[5];

    private String                 threadName                              = null;

    

    public JointTourModels(HashMap<String, String> propertyMap, AccessibilitiesTable myAccTable, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory)
    {

        accTable = myAccTable;
        
        try
        {
            threadName = "[" + java.net.InetAddress.getLocalHost().getHostName() + ": " + Thread.currentThread().getName() + "]";
        } catch (UnknownHostException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        setUpModels(propertyMap, modelStructure, dmuFactory);
    }

    public void setUpModels(HashMap<String, String> propertyMap, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up %s tour frequency model on %s",
                ModelStructure.JOINT_NON_MANDATORY_CATEGORY, threadName));

        String uecFileDirectory = propertyMap.get( CtrampApplication.PROPERTIES_UEC_PATH );

        String uecFileName = propertyMap.get(UEC_FILE_PROPERTIES_TARGET);
        uecFileName = uecFileDirectory + uecFileName;

        dmuObject = dmuFactory.getJointTourModelsDMU();

        purposeIndexNameMap = new HashMap<Integer,String>();
        purposeIndexNameMap.put(ModelStructure.SHOPPING_PRIMARY_PURPOSE_INDEX, ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME);
        purposeIndexNameMap.put(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_INDEX, ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME);
        purposeIndexNameMap.put(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_INDEX, ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME);
        purposeIndexNameMap.put(ModelStructure.VISITING_PRIMARY_PURPOSE_INDEX, ModelStructure.VISITING_PRIMARY_PURPOSE_NAME);
        purposeIndexNameMap.put(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_INDEX, ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME);

        int dataSheet = Integer.parseInt( propertyMap.get(UEC_DATA_PAGE_TARGET) );
        int freqCompSheet = Integer.parseInt( propertyMap.get(UEC_JOINT_TOUR_FREQ_COMP_MODEL_PAGE) );
        int particSheet = Integer.parseInt( propertyMap.get(UEC_JOINT_TOUR_PARTIC_MODEL_PAGE) );
        
        // set up the models
        jointTourFrequencyModel = new ChoiceModelApplication(uecFileName, freqCompSheet, dataSheet, propertyMap, (VariableTable) dmuObject);
        jointModelsAltsTable = jointTourFrequencyModel.getUEC().getAlternativeData();
        modelStructure.setJtfAltLabels( jointTourFrequencyModel.getAlternativeNames() );
        
        jointTourParticipation = new ChoiceModelApplication(uecFileName, particSheet, dataSheet, propertyMap, (VariableTable) dmuObject);
    }

    public void applyModel(Household household)
    {
        
        // this household does not make joint tours if the CDAP pattern does not contain "j".
        if ( ! household.getCoordinatedDailyActivityPattern().contains("j") )
            return;

        household.calculateTimeWindowOverlaps();
        
        try
        {

            // joint tour frequency choice is not applied to a household unless it has:
            // 2 or more persons, each with at least one out-of home activity, and at
            // least 1 of the persons not a pre-schooler.

            Logger modelLogger = tourFreq;
            if (household.getDebugChoiceModels())
                household.logHouseholdObject("Pre Joint Tour Frequency Choice HHID=" + household.getHhId() + " Object", modelLogger);

            // if it's not a valid household for joint tour frequency, keep track of
            // count for logging later, and return.
            int validIndex = household.getValidHouseholdForJointTourFrequencyModel();
            if (validIndex != 1)
            {
                invalidCount[validIndex]++;
                switch (validIndex)
                {
                    case 2:
                        household.setJointTourFreqResult(-2, "-2_1 person");
                        break;
                    case 3:
                        household.setJointTourFreqResult(-3, "-3_< 2 travel");
                        break;
                    case 4:
                        household.setJointTourFreqResult(-4, "-4_only preschool travel");
                        break;
                }
                return;
            }

            // set the household id, origin taz, hh taz, and debugFlag=false in the dmu
            dmuObject.setHouseholdObject(household);
            
            // set the accessibility values needed based on auto sufficiency category for the hh.
            if ( household.getAutoSufficiency() == 1 ) {
                dmuObject.setShopHOVAccessibility(accTable.getAggregateAccessibility("shopHov0", household.getHhMgra()));
                dmuObject.setMaintHOVAccessibility(accTable.getAggregateAccessibility("maintHov0", household.getHhMgra()));
                dmuObject.setDiscrHOVAccessibility(accTable.getAggregateAccessibility("discrHov0", household.getHhMgra()));
            }
            else if ( household.getAutoSufficiency() == 2 ) {
                dmuObject.setShopHOVAccessibility(accTable.getAggregateAccessibility("shopHov1", household.getHhMgra()));
                dmuObject.setMaintHOVAccessibility(accTable.getAggregateAccessibility("maintHov1", household.getHhMgra()));
                dmuObject.setDiscrHOVAccessibility(accTable.getAggregateAccessibility("discrHov1", household.getHhMgra()));
            }
            else if ( household.getAutoSufficiency() == 3 ) {
                dmuObject.setShopHOVAccessibility(accTable.getAggregateAccessibility("shopHov2", household.getHhMgra()));
                dmuObject.setMaintHOVAccessibility(accTable.getAggregateAccessibility("maintHov2", household.getHhMgra()));
                dmuObject.setDiscrHOVAccessibility(accTable.getAggregateAccessibility("discrHov2", household.getHhMgra()));
            }
            
            IndexValues index = dmuObject.getDmuIndexValues();

            // write debug header
            String separator = "";
            String choiceModelDescription = "";
            String decisionMakerLabel = "";
            String loggingHeader = "";
            if (household.getDebugChoiceModels())
            {

                choiceModelDescription = String
                        .format("Joint Non-Mandatory Tour Frequency Choice Model:");
                decisionMakerLabel = String.format("HH=%d, hhSize=%d.", household.getHhId(),
                        household.getHhSize());

                jointTourFrequencyModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);

                modelLogger.info(" ");
                loggingHeader = choiceModelDescription + " for " + decisionMakerLabel;
                for (int k = 0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.info(loggingHeader);
                modelLogger.info(separator);
                modelLogger.info("");
                modelLogger.info("");
            }

            jointTourFrequencyModel.computeUtilities(dmuObject, index);

            // get the random number from the household
            Random random = household.getHhRandom();
            int randomCount = household.getHhRandomCount();
            double rn = random.nextDouble();

            // if the choice model has at least one available alternative, make
            // choice.
            int chosenFreqAlt = -1;
            if (jointTourFrequencyModel.getAvailabilityCount() > 0)
            {
                chosenFreqAlt = jointTourFrequencyModel.getChoiceResult(rn);
                household.setJointTourFreqResult(chosenFreqAlt, jointTourFrequencyModel.getAlternativeNames()[chosenFreqAlt - 1]);
            } else
            {
                logger.error(String.format("Exception caught for HHID=%d, no available joint tour frequency alternatives to choose from in choiceModelApplication.", household.getHhId()));
                throw new RuntimeException();
            }

            // debug output
            if (household.getDebugChoiceModels())
            {
                
                String[] altNames = jointTourFrequencyModel.getAlternativeNames(); 

                double[] utilities = jointTourFrequencyModel.getUtilities();
                double[] probabilities = jointTourFrequencyModel.getProbabilities();

                modelLogger.info("HHID: " + household.getHhId() + ", HHSize: " + household.getHhSize());
                modelLogger.info("Alternative                 Utility       Probability           CumProb");
                modelLogger.info("------------------   --------------    --------------    --------------");

                double cumProb = 0.0;
                for (int k = 0; k < altNames.length; k++)
                {
                    cumProb += probabilities[k];
                    String altString = String.format("%-3d %10s", k + 1, altNames[k]);
                    modelLogger.info(String.format("%-15s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
                }

                modelLogger.info(" ");
                String altString = String.format("%-3d %10s", chosenFreqAlt, altNames[chosenFreqAlt-1]);
                modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount));

                modelLogger.info(separator);
                modelLogger.info("");
                modelLogger.info("");

                // write choice model alternative info to debug log file
                jointTourFrequencyModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
                jointTourFrequencyModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosenFreqAlt);

                // write UEC calculation results to separate model specific log file
                jointTourFrequencyModel.logUECResults(modelLogger, loggingHeader);

            }

            createJointTours(household, chosenFreqAlt);

        } catch (Exception e)
        {
            logger.error(String.format("error joint tour choices model for hhId=%d.", household
                    .getHhId()));
            throw new RuntimeException();
        }

        household.setJtfRandomCount(household.getHhRandomCount());

    }

    private void jointTourParticipation(Tour jointTour)
    {

        // get the Household object for this joint tour
        Household household = dmuObject.getHouseholdObject();

        // get the array of Person objects for this hh
        Person[] persons = household.getPersons();

        // define an ArrayList to hold indices of person objects participating in the joint tour
        ArrayList<Integer> jointTourPersonList = null;

        // make sure each joint tour has a valid composition before going to the next one.
        boolean validParty = false;

        int adults = 0;
        int children = 0;

        Logger modelLogger = tourFreq;

        int loopCount = 0;
        while(!validParty)
        {

            dmuObject.setTourObject( jointTour );
            
            adults = 0;
            children = 0;

            jointTourPersonList = new ArrayList<Integer>();

            String separator = "";
            String choiceModelDescription = "";
            String decisionMakerLabel = "";
            String loggingHeader = "";

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];
                jointTour.setPersonObject(persons[p]);

                if (household.getDebugChoiceModels() || loopCount == 1000)
                {
                    decisionMakerLabel = String.format( "HH=%d, hhSize=%d, PersonNum=%d, PersonType=%s, tourId=%d.", household.getHhId(), household.getHhSize(), person.getPersonNum(),
                            person.getPersonType(), jointTour.getTourId());
                    household.logPersonObject(decisionMakerLabel, modelLogger, person);
                }

                // if person type is inconsistent with tour composition, person's
                // participation is by definition no,
                // so skip making the choice and go to next person
                switch (jointTour.getJointTourComposition())
                {

                    // adults only in joint tour
                    case 1:
                        if (persons[p].getPersonIsAdult() == 1)
                        {

                            // write debug header
                            if (household.getDebugChoiceModels() || loopCount == 1000)
                            {

                                choiceModelDescription = String.format("Adult Party Joint Tour Participation Choice Model:");
                                jointTourParticipation.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);

                                modelLogger.info(" ");
                                loggingHeader = choiceModelDescription + " for " + decisionMakerLabel + ".";
                                for (int k = 0; k < loggingHeader.length(); k++)
                                    separator += "+";
                                modelLogger.info(loggingHeader);
                                modelLogger.info(separator);
                                modelLogger.info("");
                                modelLogger.info("");
                            }

                            jointTourParticipation.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                            
                            // get the random number from the household
                            Random random = household.getHhRandom();
                            int randomCount = household.getHhRandomCount();
                            double rn = random.nextDouble();

                            // if the choice model has at least one available
                            // alternative, make choice.
                            int chosen = -1;
                            if (jointTourParticipation.getAvailabilityCount() > 0)
                                chosen = jointTourParticipation.getChoiceResult(rn);
                            else
                            {
                                logger.error(String.format("Exception caught for HHID=%d, person p=%d, no available adults only joint tour participation alternatives to choose from in choiceModelApplication.", jointTour.getHhId(), p));
                                throw new RuntimeException();
                            }

                            // debug output
                            if (household.getDebugChoiceModels() || loopCount == 1000)
                            {

                                String[] altNames = jointTourParticipation.getAlternativeNames();

                                double[] utilities = jointTourParticipation.getUtilities();
                                double[] probabilities = jointTourParticipation.getProbabilities();

                                modelLogger.info("HHID: " + household.getHhId() + ", HHSize: " + household.getHhSize() + ", tourId: " + jointTour.getTourId() + ", jointFreqChosen: " + household.getJointTourFreqChosenAltName() );
                                modelLogger.info("Alternative                 Utility       Probability           CumProb");
                                modelLogger.info("------------------   --------------    --------------    --------------");

                                double cumProb = 0.0;
                                for (int k = 0; k < altNames.length; k++)
                                {
                                    cumProb += probabilities[k];
                                    String altString = String.format("%-3d %13s", k + 1, altNames[k]);
                                    modelLogger.info(String.format("%-18s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
                                }

                                modelLogger.info(" ");
                                String altString = String.format("%-3d %13s", chosen, altNames[chosen - 1]);
                                modelLogger.info(String.format( "Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount));

                                modelLogger.info(separator);
                                modelLogger.info("");
                                modelLogger.info("");

                                // write choice model alternative info to debug log
                                // file
                                jointTourParticipation.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
                                jointTourParticipation.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

                                // write UEC calculation results to separate model
                                // specific log file
                                jointTourParticipation.logUECResults(modelLogger, loggingHeader);
                                
                                if ( loopCount == 1000 ) {
                                    jointTourFrequencyModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);
                                    jointTourFrequencyModel.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                                    jointTourFrequencyModel.logUECResults(modelLogger, loggingHeader);
                                }
                                
                            }

                            // particpate is alternative 1, not participating is alternative 2.
                            if (chosen == 1)
                            {
                                jointTourPersonList.add(p);
                                adults++;
                            }
                        }
                        break;

                    // children only in joint tour
                    case 2:
                        if (persons[p].getPersonIsAdult() == 0)
                        {

                            // write debug header
                            if (household.getDebugChoiceModels() || loopCount == 1000)
                            {

                                choiceModelDescription = String.format("Child Party Joint Tour Participation Choice Model:");
                                jointTourParticipation.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);

                                modelLogger.info(" ");
                                loggingHeader = choiceModelDescription + " for " + decisionMakerLabel + ".";
                                for (int k = 0; k < loggingHeader.length(); k++)
                                    separator += "+";
                                modelLogger.info(loggingHeader);
                                modelLogger.info(separator);
                                modelLogger.info("");
                                modelLogger.info("");
                            }

                            jointTourParticipation.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                            Random random = household.getHhRandom();
                            int randomCount = household.getHhRandomCount();
                            double rn = random.nextDouble();

                            // if the choice model has at least one available
                            // alternative, make choice.
                            int chosen = -1;
                            if (jointTourParticipation.getAvailabilityCount() > 0)
                                chosen = jointTourParticipation.getChoiceResult(rn);
                            else
                            {
                                logger.error(String.format("Exception caught for HHID=%d, person p=%d, no available children only joint tour participation alternatives to choose from in choiceModelApplication.", jointTour.getHhId(), p));
                                throw new RuntimeException();
                            }

                            // debug output
                            if (household.getDebugChoiceModels() || loopCount == 1000)
                            {

                                String[] altNames = jointTourParticipation.getAlternativeNames();

                                double[] utilities = jointTourParticipation.getUtilities();
                                double[] probabilities = jointTourParticipation.getProbabilities();

                                modelLogger.info("HHID: " + household.getHhId() + ", HHSize: " + household.getHhSize() + ", tourId: " + jointTour.getTourId());
                                modelLogger.info("Alternative                 Utility       Probability           CumProb");
                                modelLogger.info("------------------   --------------    --------------    --------------");

                                double cumProb = 0.0;
                                for (int k = 0; k < altNames.length; k++)
                                {
                                    cumProb += probabilities[k];
                                    String altString = String.format("%-3d %13s", k + 1, altNames[k]);
                                    modelLogger.info(String.format("%-18s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
                                }

                                modelLogger.info(" ");
                                String altString = String.format("%-3d %13s", chosen, altNames[chosen - 1]);
                                modelLogger.info(String.format( "Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount));

                                modelLogger.info(separator);
                                modelLogger.info("");
                                modelLogger.info("");

                                // write choice model alternative info to debug log
                                // file
                                jointTourParticipation.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
                                jointTourParticipation.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

                                // write UEC calculation results to separate model
                                // specific log file
                                jointTourParticipation.logUECResults(modelLogger, loggingHeader);

                                if ( loopCount == 1000 ) {
                                    jointTourFrequencyModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);
                                    jointTourFrequencyModel.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                                    jointTourFrequencyModel.logUECResults(modelLogger, loggingHeader);
                                }
                            }

                            // particpate is alternative 1, not participating is alternative 2.
                            if (chosen == 1)
                            {
                                jointTourPersonList.add(p);
                                children++;
                            }
                        }
                        break;

                    // mixed, adults and children in joint tour
                    case 3:

                        // write debug header
                        if (household.getDebugChoiceModels() || loopCount == 1000)
                        {

                            choiceModelDescription = String.format("Mixed Party Joint Tour Participation Choice Model:");
                            jointTourParticipation.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);

                            modelLogger.info(" ");
                            loggingHeader = choiceModelDescription + " for " + decisionMakerLabel + ".";
                            for (int k = 0; k < loggingHeader.length(); k++)
                                separator += "+";
                            modelLogger.info(loggingHeader);
                            modelLogger.info(separator);
                            modelLogger.info("");
                            modelLogger.info("");
                        }

                        jointTourParticipation.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                        Random random = household.getHhRandom();
                        int randomCount = household.getHhRandomCount();
                        double rn = random.nextDouble();

                        // if the choice model has at least one available
                        // alternative, make choice.
                        int chosen = -1;
                        if (jointTourParticipation.getAvailabilityCount() > 0)
                            chosen = jointTourParticipation.getChoiceResult(rn);
                        else
                        {
                            logger.error(String.format("Exception caught for HHID=%d, person p=%d, no available mixed adult/children joint tour participation alternatives to choose from in choiceModelApplication.", jointTour.getHhId(), p));
                            throw new RuntimeException();
                        }

                        // debug output
                        if (household.getDebugChoiceModels() || loopCount == 1000)
                        {

                            String[] altNames = jointTourParticipation.getAlternativeNames();

                            double[] utilities = jointTourParticipation.getUtilities();
                            double[] probabilities = jointTourParticipation.getProbabilities();

                            modelLogger.info("HHID: " + household.getHhId() + ", HHSize: " + household.getHhSize() + ", tourId: " + jointTour.getTourId());
                            modelLogger.info("Alternative                 Utility       Probability           CumProb");
                            modelLogger.info("------------------   --------------    --------------    --------------");

                            double cumProb = 0.0;
                            for (int k = 0; k < altNames.length; k++)
                            {
                                cumProb += probabilities[k];
                                String altString = String.format("%-3d %13s", k + 1, altNames[k]);
                                modelLogger.info(String.format("%-18s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
                            }

                            modelLogger.info(" ");
                            String altString = String.format("%-3d %13s", chosen, altNames[chosen - 1]);
                            modelLogger.info(String.format( "Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount));

                            modelLogger.info(separator);
                            modelLogger.info("");
                            modelLogger.info("");

                            // write choice model alternative info to debug log file
                            jointTourParticipation.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
                            jointTourParticipation.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

                            // write UEC calculation results to separate model
                            // specific log file
                            jointTourParticipation.logUECResults(modelLogger, loggingHeader);

                            if ( loopCount == 1000 ) {
                                jointTourFrequencyModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);
                                jointTourFrequencyModel.computeUtilities(dmuObject, dmuObject.getDmuIndexValues());
                                jointTourFrequencyModel.logUECResults(modelLogger, loggingHeader);
                            }
                        }

                        // particpate is alternative 1, not participating is alternative 2.
                        if (chosen == 1)
                        {
                            jointTourPersonList.add(p);
                            if (persons[p].getPersonIsAdult() == 1)
                                adults++;
                            else
                                children++;
                        }
                        break;

                }

            }

            // done with all persons, so see if the chosen participation is a valid
            // composition, and if not, repeat the participation choice.
            switch (jointTour.getJointTourComposition())
            {

                case 1:
                    if (adults > 1 && children == 0) validParty = true;
                    break;

                case 2:
                    if (adults == 0 && children > 1) validParty = true;
                    break;

                case 3:
                    if (adults > 0 && children > 0) validParty = true;
                    break;

            }
            
            if ( ! validParty )
                loopCount++;

            if ( loopCount > 1000 ) {
                logger.warn( "loop count in joint tour participation model is " + loopCount);
                if ( loopCount > 2000 ){
                    logger.error( "terminating on excessive loop count." );
                    throw new RuntimeException();
                }
            }
            
        } // end while

        // create an array of person indices for participation in the tour
        int[] personNums = new int[jointTourPersonList.size()];
        for (int i = 0; i < personNums.length; i++)
            personNums[i] = jointTourPersonList.get(i);
        jointTour.setPersonNumArray(personNums);

        if (household.getDebugChoiceModels())
        {
            for (int i = 0; i < personNums.length; i++)
            {
                Person person = household.getPersons()[personNums[i]];
                String decisionMakerLabel = String.format(
                                "Person in Party, HH=%d, hhSize=%d, PersonNum=%d, PersonType=%s, tourId=%d.",
                                household.getHhId(), household.getHhSize(), person.getPersonNum(),
                                person.getPersonType(), jointTour.getTourId());
                household.logPersonObject(decisionMakerLabel, modelLogger, person);
            }
        }

    }

    /**
     * creates the tour objects in the Household object given the chosen joint tour
     * frequency alternative.
     * 
     * @param chosenAlt
     */
    private void createJointTours(Household household, int chosenAlt)
    {
        
        int purposeIndex1 = (int)jointModelsAltsTable.getValueAt( chosenAlt, PURPOSE_1_FIELD); 
        int purposeIndex2 = (int)jointModelsAltsTable.getValueAt( chosenAlt, PURPOSE_2_FIELD); 

        if ( purposeIndex1 > 0 && purposeIndex2 > 0 ) {            

            Tour t1 = new Tour(household, (String) purposeIndexNameMap.get(purposeIndex1), ModelStructure.JOINT_NON_MANDATORY_CATEGORY, purposeIndex1);
            int party1 = (int)jointModelsAltsTable.getValueAt( chosenAlt, PARTY_1_FIELD); 
            t1.setJointTourComposition(party1);

            Tour t2 = new Tour(household, (String) purposeIndexNameMap.get(purposeIndex2), ModelStructure.JOINT_NON_MANDATORY_CATEGORY, purposeIndex2);                
            int party2 = (int)jointModelsAltsTable.getValueAt( chosenAlt, PARTY_2_FIELD); 
            t2.setJointTourComposition(party2);

            household.createJointTourArray(t1, t2);

            jointTourParticipation(t1);
            jointTourParticipation(t2);
            
            }
        else{

            Tour t1 = new Tour(household, (String) purposeIndexNameMap.get(purposeIndex1), ModelStructure.JOINT_NON_MANDATORY_CATEGORY, purposeIndex1);
            int party1 = (int)jointModelsAltsTable.getValueAt( chosenAlt, PARTY_1_FIELD); 
            t1.setJointTourComposition(party1);

            household.createJointTourArray(t1);

            jointTourParticipation(t1);

        }

    }


    public static void main( String[] args )
    {
                
        // set values for these arguments so an object instance can be created
        // and setup run to test integrity of UEC files before running full model.
        HashMap<String, String> propertyMap;
 
        if (args.length == 0)
        {
            System.out.println("no properties file base name (without .properties extension) was specified as an argument.");
            return;
        } else
        {
            ResourceBundle rb = ResourceBundle.getBundle(args[0]);
            propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        }

        
        
        /*
         *         
         */          
        String matrixServerAddress = (String) propertyMap.get("RunModel.MatrixServerAddress");
        String matrixServerPort = (String) propertyMap.get("RunModel.MatrixServerPort");

        MatrixDataServerIf ms = new MatrixDataServerRmi(matrixServerAddress, Integer.parseInt(matrixServerPort), MatrixDataServer.MATRIX_DATA_SERVER_NAME);
        ms.testRemote(Thread.currentThread().getName());
        ms.start32BitMatrixIoServer(MatrixType.TRANSCAD);

        MatrixDataManager mdm = MatrixDataManager.getInstance();
        mdm.setMatrixDataServerObject(ms);

        
        MgraDataManager mgraManager = MgraDataManager.getInstance(propertyMap);
        TazDataManager tazManager = TazDataManager.getInstance(propertyMap);
        
        ModelStructure modelStructure = new SandagModelStructure();
        SandagCtrampDmuFactory dmuFactory = new SandagCtrampDmuFactory(modelStructure);
 
                
        
        String projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String accFileName = projectDirectory + Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file");      
        AccessibilitiesTable accTable = new AccessibilitiesTable(accFileName);
    
        
        
        String hhHandlerAddress = (String) propertyMap.get("RunModel.HouseholdServerAddress");
        int hhServerPort = Integer.parseInt((String) propertyMap.get("RunModel.HouseholdServerPort"));
        
        HouseholdDataManagerIf householdDataManager = new HouseholdDataManagerRmi(hhHandlerAddress, hhServerPort,
                SandagHouseholdDataManager.HH_DATA_SERVER_NAME);


        householdDataManager.setPropertyFileValues(propertyMap);
        householdDataManager.setHouseholdSampleRate( 1.0f, 0 );
        householdDataManager.setDebugHhIdsFromHashmap();
        householdDataManager.setTraceHouseholdSet();

        int id = householdDataManager.getArrayIndex( 423804 );
        Household[] hh = householdDataManager.getHhArray( id, id );

        
        JointTourModels jtfModel = new JointTourModels( propertyMap, accTable, modelStructure, dmuFactory );
        jtfModel.applyModel( hh[0] );
        

        
        
        /*
         * Use this block to instantiate a UEC for the joint freq/comp model and a UEC for the joint participate model.
         * After checking the UECs are instantiated correctly (spelling/typos/dmu methods implemented/etc.), test model implementation.
        String uecFileDirectory = propertyMap.get( CtrampApplication.PROPERTIES_UEC_PATH );

        ModelStructure modelStructure = new SandagModelStructure();
        SandagCtrampDmuFactory dmuFactory = new SandagCtrampDmuFactory(modelStructure);
        
        JointTourModelsDMU dmuObject = dmuFactory.getJointTourModelsDMU();
        File uecFile = new File(uecFileDirectory + propertyMap.get( UEC_FILE_PROPERTIES_TARGET ));
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(uecFile, 1, 0, propertyMap, (VariableTable)dmuObject);
        System.out.println("Jount tour freq/comp choice UEC passed.");

        uec = new UtilityExpressionCalculator(uecFile, 2, 0, propertyMap, (VariableTable)dmuObject);
        System.out.println("Joint tour participation choice UEC passed.");
         */

        
        ms.stop32BitMatrixIoServer();
    
    }

}