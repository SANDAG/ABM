package org.sandag.abm.ctramp;

import com.pb.common.calculator.VariableTable;

import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.TourDriverDMU;

import com.pb.common.newmodel.ChoiceModelApplication;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

import umontreal.iro.lecuyer.probdist.GumbelDist;


public class TourDriverModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(TourDriverModel.class);
    private transient Logger tourDriverLogger = Logger.getLogger("tripMcLog"); 
    
    private static final String TD_CONTROL_FILE_TARGET = "td.uec.file";
    private static final String TD_DATA_SHEET_TARGET = "td.data.page";
    private static final String TD_MODEL_SHEET_TARGET = "td.model.page";
    private static final int CHOICE_NA = -1;
    private static final int CHOICE_DRIVER = 1;
    private static final int NA = -9999;
    
   
    private ModelStructure modelStructure;
    private TourDriverDMU dmuObject;
	private ChoiceModelApplication choiceModelApplication;
	private GumbelDist logitDist;


    public TourDriverModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory) { 
        this.modelStructure = modelStructure;
        setupModels( propertyMap, dmuFactory);
    }

    private void setupModels( HashMap<String, String> propertyMap,  CtrampDmuFactoryIf dmuFactory) { 
        
        logger.info( String.format( "setting up tour driver." ) );        
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tdUecFile = propertyMap.get( TD_CONTROL_FILE_TARGET );
        String uecFileName = uecPath + tdUecFile;
        dmuObject = dmuFactory.getTourDriverDMU();
        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TD_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, TD_MODEL_SHEET_TARGET);

        choiceModelApplication = new ChoiceModelApplication( uecFileName, modelSheet, dataSheet, propertyMap, (VariableTable)dmuObject );
        logitDist = new GumbelDist();
    }

    public void applyModel( Household household ){

	    //apply for joint tours
        Tour[] jointTours = household.getJointTourArray();
        if (jointTours!=null) {
        	for ( int i=0; i < jointTours.length; i++ ) {
        		Tour tour = jointTours[i];  
        		applyJointModel(household, tour); 
        	}
        }
        
        // loop through the person array (1-based), and process all individual tours for each person
        Person[] personArray = household.getPersons();
        for(int j=1;j<personArray.length;++j){

            // apply model for all person tours
            Person person = personArray[j];
            List<Tour> tourList = new ArrayList<Tour>();
            tourList.addAll( person.getListOfWorkTours() );
            tourList.addAll( person.getListOfSchoolTours() );
            tourList.addAll( person.getListOfIndividualNonMandatoryTours() );
            tourList.addAll( person.getListOfAtWorkSubtours() );

            for ( Tour tour : tourList ) {
            	if( !tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) { //skip joint tours
                    double[] utilChoiceRand = applyIndividualModel(household, person, tour);
                    if (utilChoiceRand[1] == CHOICE_DRIVER) {
                    	tour.setTourDriver(person.getPersonNum());
                    } else {
                    	tour.setTourDriver(CHOICE_NA);
                    }
            	}
            }

        } // j (person loop)

        //rest random seed counter
        household.setTdRandomCount( household.getHhRandomCount() );

    }
        
    public void applyJointModel( Household household, Tour tour){
    	
        if ( household.getDebugChoiceModels() ) {
            tourDriverLogger.info( "Joint Tour Driver Choice: HH=" + household.getHhId());
        }
    	
    	//loop through persons and calculate utility of being driver and take person 
        //with highest utility (which includes a random term) as the driver

        double bestUtil = NA;
    	int bestPerson = CHOICE_NA;
    	
        Person[] personArray = household.getPersons();
        for(int j=1;j<personArray.length;++j){

            Person person = personArray[j];
            if(tour.getPersonInJointTour(person)) {
            	double[] utilChoiceRand = applyIndividualModel(household, person, tour);
            	double totalUtil = utilChoiceRand[0] + utilChoiceRand[2];
            	if (household.getDebugChoiceModels()){
            		tourDriverLogger.info( String.format("Joint Tour Driver Choice: PerNum=%d, util=%.8f, logitRn=%.8f", person.getPersonNum(), utilChoiceRand[0], utilChoiceRand[2]));
            	}
            	if (totalUtil > bestUtil){
            		bestUtil = totalUtil;
            		bestPerson = person.getPersonNum();
            	}
            }
        }
        
        //set to best person
        tour.setTourDriver(bestPerson);
        
    }

    public double[] applyIndividualModel( Household household, Person person, Tour tour){

        Logger modelLogger = tourDriverLogger;
        if ( household.getDebugChoiceModels() ) {
            household.logHouseholdObject( "Tour Driver Choice: HH=" + household.getHhId(), modelLogger );
            String decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s", household.getHhId(), person.getPersonNum(), person.getPersonType() );
            household.logPersonObject( decisionMakerLabel, modelLogger, person );
        }
        
        //setup dmu
        dmuObject.setHouseholdObject(household);
        dmuObject.setPersonObject(person);
        dmuObject.setTourObject(tour);

        //return utility and choice for use with joint tours  	
        double[] utilChoiceRand	= new double[3];
        utilChoiceRand[0] = NA;
        utilChoiceRand[1] = CHOICE_NA;
        utilChoiceRand[2] = 0;
        
        //only run for HOV and drive transit
    	if (modelStructure.getTourModeIsS2(tour.getTourModeChoice()) | modelStructure.getTourModeIsS3(tour.getTourModeChoice()) | modelStructure.getTourModeIsTransit(tour.getTourModeChoice())) {
    		
            try {

            	// write debug header
                String separator = "";
                String choiceModelDescription = "" ;
                String decisionMakerLabel = "";
                String loggingHeader = "";
                if( household.getDebugChoiceModels() ) {

                    choiceModelDescription = String.format ( "Tour Driver Choice Model:" );
                    decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourType=%s, TourId=%d, TourPurpose=%s.", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourCategory(), tour.getTourId(), tour.getTourPrimaryPurpose() );
                    
                    choiceModelApplication.choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
                        
                    modelLogger.info(" ");
                    loggingHeader = choiceModelDescription + " for " + decisionMakerLabel;
                    for (int k=0; k < loggingHeader.length(); k++)
                        separator += "+";
                    modelLogger.info( loggingHeader );
                    modelLogger.info( separator );
                    modelLogger.info( "" );
                    modelLogger.info( "" );
                 
                }

                // compute the utilities
                choiceModelApplication.computeUtilities(dmuObject, dmuObject.getDmuIndexValues() );

                // get the random number from the household
                Random random = household.getHhRandom();
                int randomCount = household.getHhRandomCount();
                double rn = random.nextDouble();
                int choice = -1;

                // if the choice model has at least one available alternative, make choice.
                if ( choiceModelApplication.getAvailabilityCount() > 0 ) {
                	choice = choiceModelApplication.getChoiceResult( rn );
                	utilChoiceRand[2] = logitDist.inverseF(rn);
                	utilChoiceRand[1] = (double)choice;
                	utilChoiceRand[0] = choiceModelApplication.getUtilities()[CHOICE_DRIVER-1];
                } else {
                    logger.error ( String.format( "Exception caught applying Tour Driver choice model for %s type tour: HHID=%d, personNum=%d, tourCount=%d, randomCount=%f -- no avaialable stop frequency alternative to choose.", tour.getTourCategory(), household.getHhId(), person.getPersonNum(), randomCount ) );
                    throw new RuntimeException();
                }

                // debug output
                if( household.getDebugChoiceModels() ){

                    double[] utilities     = choiceModelApplication.getUtilities();
                    double[] probabilities = choiceModelApplication.getProbabilities();
                    String[] altNames      = choiceModelApplication.getAlternativeNames();        // 0s-indexing

                    modelLogger.info( decisionMakerLabel );
                    modelLogger.info("Alternative                 Utility       Probability           CumProb");
                    modelLogger.info("------------------   --------------    --------------    --------------");

                    double cumProb = 0.0;
                    for(int k=0;k<altNames.length;++k){
                        cumProb += probabilities[k];
                        String altString = String.format( "%-3d %15s", k+1, altNames[k] );
                        modelLogger.info(String.format("%-20s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
                    }

                    modelLogger.info(" ");
                    String altString = String.format( "%-3d  %s", choice, altNames[choice-1] );
                    modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );

                    modelLogger.info( separator );
                    modelLogger.info("");
                    modelLogger.info("");
                    

                    // write choice model alternative info to debug log file
                    choiceModelApplication.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
                    choiceModelApplication.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, choice );

                    // write UEC calculation results to separate model specific log file
                    choiceModelApplication.logUECResults( modelLogger, loggingHeader );
                    
                }

            }
            catch ( Exception e ) {
                logger.error ( String.format( "Exception caught processing Tour Driver choice model for %s type tour, tourPurpose=%s, HHID=%d, personNum=%d", tour.getTourCategory(), tour.getTourPrimaryPurpose(), household.getHhId(), person.getPersonNum() ) );
                throw new RuntimeException(e);
            }
                        
    	}
    
    //return utility and choice
    return(utilChoiceRand);
    
    }
    
}
