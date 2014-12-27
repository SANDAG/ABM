package org.sandag.abm.ctramp;

import com.pb.common.model.ModelException;
import com.pb.common.newmodel.Alternative;
import com.pb.common.newmodel.ConcreteAlternative;
import com.pb.common.newmodel.LogitModel;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
//fix todos
/**
 * This class will be used for determining the departure/arrival and duration of all stops
 * on individual mandatory, individual non-mandatory and joint tours.
 *
 * @author Jessica Guo
 * @version Sep, 2013
 * 
 */public class StopTimingChoiceModel implements Serializable {
	    
	    private transient Logger logger = Logger.getLogger(StopTimingChoiceModel.class);
	    private transient Logger stc1Logger = Logger.getLogger("stc1Logger");
	    private transient Logger stc2Logger = Logger.getLogger("stc2Logger");

	    private static final String PROPERTIES_UEC_STOP_TIMING_CHOICE_1 = "UecFile.StopTimingChoiceStageOne";
	    private static final String PROPERTIES_UEC_STOP_TIMING_CHOICE_2 = "UecFile.StopTimingChoiceStageTwo";

	    // page setup for the UECs; the first two pages are the same for both models
	    private static final int UEC_DATA_PAGE = 0;
	    private static final int UEC_TRAVEL_TIME_PAGE = 1;
	    // specific to stage 1 model
	    private static final int UEC_MAIN_LEG_PAGE = 2;
	    private static final int UEC_OUTBOUND_LEG_PAGE = 3;
	    private static final int UEC_INBOUND_LEG_PAGE = 4;   
	    // specific to stage 2 model
	    private static final int UEC_OUTBOUND_STOP_PAGE = 2;
	    private static final int UEC_INBOUND_STOP_PAGE = 3;

	    private static int TIMING_ALT_COLUMN = 2;
	    
	    private static int MAX_LEG_DURATION = 47;
	    private static int MAX_STOP_DURATION = 47;

	    private ModelStructure modelStructure;

	    private StopTimingStageOneDMU stStageOneDmuObj;
	    private StopTimingStageTwoDMU stStageTwoDmuObj;
		private TimeDMU legTimeDmuObj, tripTimeDmuObj; 

	    // three separate UECs to compute segments of the utility for stage one model
		private UtilityExpressionCalculator mainLegUec, obLegUec, ibLegUec;

	    // two separate UECs to compute segments of the utility for stage two model
		private UtilityExpressionCalculator obStopUec, ibStopUec;

		// UEC for obtaining free flow travel time 
	    private UtilityExpressionCalculator legTimeUec, tripTimeUec;

	    private int[] legTimeAlts;	// from alternatives file for stage 1 UEC
	    private int[] stopTimeAlts;	// from alternatives file for stage 2 UEC
	    
	    private MgraDataManager mgraManager;

		
	    /*
	     * Constructor
	     */
	    public StopTimingChoiceModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory, ModelStructure modelStructure  ) {
	        
	         this.modelStructure = modelStructure;
    
	        mgraManager = MgraDataManager.getInstance(); 
	        
	        setupStageOneModel(propertyMap, dmuFactory);
	        setupStageTwoModel(propertyMap, dmuFactory);
	    }
	    
	    private void setupStageOneModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
	        
	        logger.info( String.format( "setting up stage one of stop timing choice models." ) );
	        
	        // ******** stage 1 model **********
	        
	        // locate the UEC
	        String uecFileName = propertyMap.get( PROPERTIES_UEC_STOP_TIMING_CHOICE_1 );
	        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
	        uecFileName = projectDirectory + uecFileName;

	        // create the DMU object
    		//todo: Fix
// 	        stStageOneDmuObj = dmuFactory.getStopTimingStageOneDMU();
	        //stStageOneDmuObj = new StopTimingStageOneDMU(modelStructure, uecFileName, propertyMap);
	        		
	        // create the three UECs, which are used to compute 3 segments of the total utility
	        mainLegUec = new UtilityExpressionCalculator( new File(uecFileName), UEC_MAIN_LEG_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)stStageOneDmuObj );
	        obLegUec = new UtilityExpressionCalculator( new File(uecFileName), UEC_OUTBOUND_LEG_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)stStageOneDmuObj );
	        ibLegUec = new UtilityExpressionCalculator( new File(uecFileName), UEC_INBOUND_LEG_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)stStageOneDmuObj );
	        
	        // get the leg time alternatives table from the UEC
	        // the same set of alternatives applies to all 3 legs  
	        TableDataSet altsTable = mainLegUec.getAlternativeData();
	        legTimeAlts = altsTable.getColumnAsInt( TIMING_ALT_COLUMN );
	        altsTable = null;

	    	// create the travel time DMU for obtaining FFT 
	    	legTimeDmuObj = new TimeDMU();

	    	// create UEC   	
	    	legTimeUec = new UtilityExpressionCalculator(new File(uecFileName), UEC_TRAVEL_TIME_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)legTimeDmuObj );

	        }
	    
	    private void setupStageTwoModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
	        
	        logger.info( String.format( "setting up stage two of stop timing choice model." ) );
	        	        	        
	        // ******** stage 2 model **********
	        
	        // locate the UEC
	        String uecFileName = propertyMap.get( PROPERTIES_UEC_STOP_TIMING_CHOICE_2 );
	        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
	        uecFileName = projectDirectory + uecFileName;

	        // create the dmu object
    		//todo: Fix
// 	        stStageTwoDmuObj = dmuFactory.getStopTimingStageTwoDMU();

	        // create the two UECs, one for stops on outbound leg and one for stops on inbound leg 
	        obStopUec = new UtilityExpressionCalculator( new File(uecFileName), UEC_OUTBOUND_STOP_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)stStageTwoDmuObj );
	        ibStopUec = new UtilityExpressionCalculator( new File(uecFileName), UEC_INBOUND_STOP_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)stStageTwoDmuObj );

		    // get the stop time alternatives table from the UEC
	        // the same set of alternatives applies to both inbound and outbound stops
	        TableDataSet altsTable = obStopUec.getAlternativeData();
	        stopTimeAlts = altsTable.getColumnAsInt( TIMING_ALT_COLUMN );
	        altsTable = null;

	    	// create the travel time DMU for obtaining FFT 
	    	tripTimeDmuObj = new TimeDMU();

	    	// create UEC   	
	    	tripTimeUec = new UtilityExpressionCalculator(new File(uecFileName), UEC_TRAVEL_TIME_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)tripTimeDmuObj );
  
	    }

	    public void applyModel( Household household ) {

	        // get this household's person array
	        Person[] personArray = household.getPersons();

        	int[] timeSplit = new int[3]; // splits the tour duration into 3 segments: main leg, ob leg, ib leg 

	        // loop through the person array (1-based) and collect all individual tours
	        for(int j=1; j < personArray.length; ++j) {

	            Person person = personArray[j];

	            ArrayList<Tour> tours = new ArrayList<Tour>();
	            tours.addAll( person.getListOfWorkTours() );
	            tours.addAll( person.getListOfSchoolTours() );
	            tours.addAll( person.getListOfIndividualNonMandatoryTours() );
	            tours.addAll( person.getListOfAtWorkSubtours() );

	            for ( Tour tour : tours ) {
	            	
	            	// initialize leg times
	            	timeSplit[0] = tour.getTourArrivePeriod() - tour.getTourDepartPeriod();
	            	timeSplit[1] = 0;
	            	timeSplit[2] = 0;
	            	
            		// if there is no stop on tour except the primary destination          	
	            	if ( ( tour.getNumOutboundStops()==0 ) && ( tour.getNumInboundStops()==0 ) ) {
	            		// TODO: anything needs to be done? set arrival/departure time at primary destination?
	            		//todo: Fix
	            		// 		            	tour.setLegTimes(timeSplit[0], timeSplit[1], timeSplit[2]);
	            	}
	            	else {           		
	            		applyStageOneModel( timeSplit, tour, person, household ); 	   
	            		//todo: Fix
	            		// 		            	tour.setLegTimes(timeSplit[0], timeSplit[1], timeSplit[2]);
	            		
		            	applyStageTwoModel( timeSplit, tour, person, household );
	            	}
	            	
	            } //tour loop

	        } // j (person loop)
	        
	        // process joint tours
	        Tour[] jointTours = household.getJointTourArray();
	        
	        if ( jointTours != null ) {
	        	
	            for ( Tour tour : jointTours ) {
	            	
	            	// initialize leg times
	            	timeSplit[0] = tour.getTourArrivePeriod() - tour.getTourDepartPeriod();
	            	timeSplit[1] = 0;
	            	timeSplit[2] = 0;

	            	// if there is no stop on tour except the primary destination          	
	            	if ( ( tour.getNumOutboundStops()==0 ) && ( tour.getNumInboundStops()==0 ) ) {
	            		// TODO: anything needs to be done? set arrival/departure time at primary destination?
	            		//todo: Fix
	            		// 		            	tour.setLegTimes(timeSplit[0], timeSplit[1], timeSplit[2]);
	            	}
	            	else {           		
	            		applyStageOneModel( timeSplit, tour, null, household ); 	   
	            		//todo: Fix
	            		// 		            	tour.setLegTimes(timeSplit[0], timeSplit[1], timeSplit[2]);

		            	applyStageTwoModel( timeSplit, tour, null, household );
	            	}
	            	
	            } //tour loop
	        }
	        
	        household.setStlRandomCount( household.getHhRandomCount() ); // TODO - check
	    
	    }
	    
	   /* The goal of stage one model is to determine the the duration of outbound, main, and inbound legs.
	    * The utilities are computed using 3 separate UEC spreadsheets, one for each leg. These utilities are then
	    * aggregated to represent each possible leg timing pattern for the tour, and the choice is made.
	    * 
	    */
	    private void applyStageOneModel( int[] timeSplit, Tour tour, Person person, Household household ) {
	    	
	    	// ******** First, compute utility segments **********

	    	// setup the DMU
	    	stStageOneDmuObj.setTourObject(tour);
	    	stStageOneDmuObj.setPersonObject(person);
	    	stStageOneDmuObj.setHouseholdObject(household);
	    	
	    	// compute FFT and pass onto DMU
	    	stStageOneDmuObj.setMainLegFFT( computeMainLegFFT(tour) );
	    	stStageOneDmuObj.setOutboundLegFFT( computeOutboundLegFFT(tour) );
	    	stStageOneDmuObj.setInboundLegFFT( computeInboundLegFFT(tour) );
	    	
	    	// compute utilities for each leg component 
	    	double[][] utilities = new double[3][];	// 0-based 
	    	utilities[0] = mainLegUec.solve(stStageOneDmuObj.getIndexValues(), stStageOneDmuObj, null);
	    	utilities[1] = obLegUec.solve(stStageOneDmuObj.getIndexValues(), stStageOneDmuObj, null);
	    	utilities[2] = ibLegUec.solve(stStageOneDmuObj.getIndexValues(), stStageOneDmuObj, null);
	    	
	        // write component utilities out if we have a trace household
	    	
	        if(household.getDebugChoiceModels()){

	        	if ( person!=null ) {
	        	
	        		household.logHouseholdObject( "Pre Stop Timing Choice for tour: HH=" + household.getHhId() + ", Pers=" + person.getPersonNum() + ", Tour Purpose=" + tour.getTourPurpose() + ", TourId=" + tour.getTourId(), stc1Logger );
	        		household.logPersonObject( "Pre Stop Timing Choice for person " + person.getPersonNum(), stc1Logger, tour.getPersonObject() );
	        		household.logTourObject("Pre Stop Timing Choice for tour " + tour.getTourId(), stc1Logger, person, tour );
	        	} 
	        	else {
                    String label = String.format ( "Pre Stop Timing Choice for tour: HH=%d, tourId=%d.", household.getHhId(), tour.getTourId() );
                    household.logTourObject( label, stc1Logger, household.getPersons()[tour.getPersonNumArray()[0]], tour );
	        	}
	        		

	        	stc1Logger.info(" ");
	            stc1Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            stc1Logger.info("Stop Timing Choice - Stage One Model: Debug Statement ");
	            stc1Logger.info("Alterantive            M Util        Ob Util        Ib Util");
	            stc1Logger.info("--------------  -------------- -------------- --------------");
	            
	            int numAlts = utilities[0].length;
	            for (int i = 0; i < numAlts; i++) {
		            stc1Logger.info(String.format("%-6d  %-6d  %14.8e  %14.8e  %14.8e", 
		            		i+1, legTimeAlts[i], utilities[0][i], utilities[1][i], utilities[2][i] ) );
	            }
	            stc1Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            stc1Logger.info("");
	            mainLegUec.logAnswersArray(stc1Logger, "Main Leg");
	            obLegUec.logAnswersArray(stc1Logger, "Outbound Leg");
	            ibLegUec.logAnswersArray(stc1Logger, "Inbound Leg");
	            
	        }      
    	

	    	// ******** Next, compute total utility for each timing pattern alternative **********

	    	// total tour length in # time periods
	    	// length of 0 means it tour starts and ends in the same time period
	    	int tourLength = tour.getTourArrivePeriod() - tour.getTourDepartPeriod(); 
	    	
	    	// Given tour duration, generate all possible leg timing patterns (main leg, obLeg, ibLeg) for the tour 
	    	int[][] legTimingPatterns= generateLegTimingPatterns( tourLength, tour );  // 0-based
	    	int numLegTimingPatterns = legTimingPatterns[0].length;

	    	if(household.getDebugChoiceModels()){
	            stc1Logger.info("************************************************");
	            stc1Logger.info("Total number of leg timing patterns: " + String.format("%-6d", numLegTimingPatterns) );
	            stc1Logger.info("************************************************");
	    	}
	    	
		    // create the choice model 
		    LogitModel stageOneChoiceModel = new LogitModel("Stage One of Stop Timing Choice Model", 0, numLegTimingPatterns);

	    	// add the leg timing pattern choice alternatives to the logit model 
	    	for ( int i = 0; i < numLegTimingPatterns; i++ ) {
		    	
	    		// format alternative name as "mainLegTime_OuboundLegTime_InboundLegTime" 
	    		String alternativeName = String.valueOf(legTimingPatterns[0][i])+"_";
	            alternativeName += String.valueOf(legTimingPatterns[1][i])+"_";
	            alternativeName += String.valueOf(legTimingPatterns[2][i]);
	            
	            ConcreteAlternative newAlt = new ConcreteAlternative(alternativeName, i);
	            stageOneChoiceModel.addAlternative(newAlt);
	    	}
	    	    	
	    	// loop through the all timing pattern alternatives
	    	// compute total utility for each pattern by adding up the 3 components
	        Alternative[] altPatternList = stageOneChoiceModel.getAlternatives();
	    	int legTime;
	    	int alt;
	    	// TODO: check array 0- or 1-based?
	    	for ( int p = 0; p < numLegTimingPatterns; p++ ) { 
	    		
	    		double totalUtility = 0;
	    		
	    		for ( int i = 0; i < 3; i++ ) {
	    			legTime = legTimingPatterns[i][p]; // length of the i'th leg for the 'p'th timing pattern
	    			// TODO- map legTime to alt no. in the utility array 
	    			alt = legTime; // this works for now because utilities[] is 0-based
	    			totalUtility += utilities[i][alt];
	    		}
	    		// set the total utility in the choice model
    			altPatternList[p].setUtility(totalUtility);
	    	}    	
	    	
	        // compute the exponentiated utility, logging debug info needed
	        stageOneChoiceModel.getUtility();
	        
	        stageOneChoiceModel.calculateProbabilities();

	        // write choice utilities out if we have a trace household
	        if(household.getDebugChoiceModels()){
	        	
	        	stageOneChoiceModel.setDebug(true);
		        stageOneChoiceModel.writeUtilityHeader();
		        
	            int numAlts = stageOneChoiceModel.getNumberOfAlternatives();
	            for (int i = 0; i < numAlts; i++) {
	            	Alternative thisAlt = stageOneChoiceModel.getAlternatives()[i];
	            	stc1Logger.info(
	                        	String.format("%-12s", thisAlt.getName())
	                            + "\t\t"
	                            + String.format("%-10.8e", thisAlt.getUtility())
	                            + "\t\t\t"
	                            + String.format("%-10.8e", thisAlt.getExpUtility()) );	            
	            } // end for each alt

	            stc1Logger.info("");
	            
	        } // end if trace     

	        // make a leg timing pattern choice 
	        Random hhRandom = household.getHhRandom();
	        double randomNumber = hhRandom.nextDouble();
            ConcreteAlternative chosen = (ConcreteAlternative) stageOneChoiceModel.chooseAlternative(randomNumber);

	        if(household.getDebugChoiceModels()){
	            stc1Logger.info( String.format("Choice: %s, with rn=%.8f ", chosen.getName(), randomNumber ) );
	            stc1Logger.info("");
	            stc1Logger.info("");
	        }


            // return the three leg times corresponding to the chosen alternative
            int alternativeNumber = chosen.getNumber();
            timeSplit[0] = legTimingPatterns[0][alternativeNumber];
            timeSplit[1] = legTimingPatterns[1][alternativeNumber];
            timeSplit[2] = legTimingPatterns[2][alternativeNumber];

	        if(household.getDebugChoiceModels()){
	            stc1Logger.info("Tour depart time: "+tour.getTourDepartPeriod()+". Tour arrive time: "+tour.getTourArrivePeriod() );
	            stc1Logger.info("Outbound -- Total duration: "+timeSplit[1]+", number of stops: "+tour.getNumOutboundStops() );
	            stc1Logger.info("Main Leg -- Total duration: "+timeSplit[0] );
	            stc1Logger.info("Inbound  -- Total duration: "+timeSplit[2]+", number of stops: "+tour.getNumInboundStops() );
	            stc1Logger.info("");
	        }

	    }

	    public double computeMainLegFFT(Tour tour) {
	    	
	    	Household household = tour.getPersonObject().getHouseholdObject();
	    	
	    	// lookup the zone id of the last outbound stop (if any), the first inbound stop (if any), 
	    	int lastObStopZone, firstIbStopZone;    	
	    	if ( tour.getNumOutboundStops()>0 )
	    		lastObStopZone = tour.getOutboundStops()[tour.getNumOutboundStops()-1].getDest();
	    	else
	    		lastObStopZone = mgraManager.getTaz(tour.getTourOrigMgra()); 
	    	if ( tour.getNumInboundStops()>0 )
	    		firstIbStopZone = tour.getInboundStops()[tour.getNumInboundStops()-1].getDest();
	    	else
	    		firstIbStopZone = tour.getTourOrigMgra();

	    	// look up the primary destination    	   	
	    	int tourDestZone = mgraManager.getTaz(tour.getTourDestMgra());
	    	
	        boolean debugFlag = false;
	        if ( household.getDebugChoiceModels() )
	        	debugFlag = household.getDebugChoiceModels();
	        
	        int[] availability = new int[2];
	        availability[1] = 1;

	        // the first trip: from outbound stop to primary destination
	        legTimeDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), lastObStopZone, tourDestZone, debugFlag );
	        double tripTime1[] = legTimeUec.solve( legTimeDmuObj.getDmuIndexValues(), legTimeDmuObj, availability );
	        
	        // the 2nd trip: from primary destination to first inbound stop 
	        legTimeDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), tourDestZone, firstIbStopZone, debugFlag );
	        double tripTime2[] = legTimeUec.solve( legTimeDmuObj.getDmuIndexValues(), legTimeDmuObj, availability );
	        
	        return tripTime1[0] + tripTime2[0];
	    }

	    public double computeOutboundLegFFT(Tour tour) {

	    	Household household = tour.getPersonObject().getHouseholdObject();

	    	double 	totalFFT = 0.0; // accumulative free-flow travel time 
	    	int 	numStops = tour.getNumOutboundStops();

	    	if ( numStops > 0 ) {
	    		
		    	// attributes needed for the DMU/UEC
		        boolean debugFlag = false;
		        if ( household.getDebugChoiceModels() )
		        	debugFlag = household.getDebugChoiceModels();
		        
		        int[] availability = new int[2];
		        availability[1] = 1;
		
		    	// trip start and end zone
		    	int origTaz = tour.getTourOrigMgra();
		    	int destTaz = -1;
		    	Stop obStops[] = tour.getOutboundStops();		// points to array of outbound stops
		    	for ( int s=0; s<numStops; s++ ) {
		    		
		    		// update trip end zone
		    		destTaz = obStops[s].getDest();
		    		
		    		// look up trip travel time
		    		legTimeDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), origTaz, destTaz, debugFlag );
		            double tripTime[] = legTimeUec.solve( legTimeDmuObj.getDmuIndexValues(), legTimeDmuObj, availability );
		
		            totalFFT += tripTime[0];
		    	
		            // update start zone for next trip
		            origTaz = destTaz;
		    	}
	    	}	
	    		
	    	return totalFFT;
	    }
	    
	    public double computeInboundLegFFT(Tour tour) {
	    	
	    	Household household = tour.getPersonObject().getHouseholdObject();

	    	double 	totalFFT = 0; // accumulative free-flow travel time 
	    	int 	numStops = tour.getNumInboundStops();

	    	if ( numStops > 0 ) {
		
		    	// attributes needed for the DMU/UEC
		        boolean debugFlag = false;
		        if ( household.getDebugChoiceModels() )
		        	debugFlag = household.getDebugChoiceModels();
		        
		        int[] availability = new int[2];
		        availability[1] = 1;
		
		    	// trip start and end zone
		    	int origTaz = -1;
		    	int destTaz = tour.getTourOrigMgra();;
		    	Stop ibStops[] = tour.getInboundStops();		// points to array of inbound stops
		    	
		    	// loop through stops backwards
		    	for ( int s=numStops-1; s>=0; s-- ) {
		    		
		    		// update trip end zone
		    		origTaz = ibStops[s].getDest();
		    		
		    		// look up trip travel time
		    		legTimeDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), origTaz, destTaz, debugFlag );
		            double tripTime[] = legTimeUec.solve( legTimeDmuObj.getDmuIndexValues(), legTimeDmuObj, availability );
		
		            totalFFT += tripTime[0];
		    	
		            // update start zone for next trip
		            destTaz = origTaz;
		    	}
	    	}		
	    	
	    	return totalFFT;
	       
	    }
	    
    	/*
    	 * Given the tour duration, generate stage one choice alternatives that  
    	 * include all possible combinations of main leg, ob leg, and ib leg durations
    	 * 		legTimingPatterns[0] - value for main leg
	     * 		legTimingPatterns[1] - value for outbound leg
	     *		legTimingPatterns[2] - value for inbound leg
	     *
	     * Returns the number of choice alternatives generated 
	     */   
	    private int[][] generateLegTimingPatterns( int tourLength, Tour tour) {
	    	
	    	// maximum duration of a leg is capped at MAX_LEG_DURATION
	    	int maxLegTime = Math.min(tourLength, MAX_LEG_DURATION);
	    	
	    	int obAvail = (tour.getNumOutboundStops()>0) ? 1 : 0;
	    	int ibAvail = (tour.getNumInboundStops()>0) ? 1 : 0;
	    	
	    	int[][] alts = new int[3][ (maxLegTime+1)*(maxLegTime+1) ];
	    	
	    	int count = 0;
	    	
	    	if (obAvail+ibAvail == 0) // no stops on either legs
	    	{
	    		// there can be only one timing pattern
            	alts[0][count] = tourLength;
            	alts[1][count] = 0;
            	alts[2][count] = 0;
            	count = 1;    		
	    	}
	    	else if (obAvail+ibAvail == 1) // no stops on one of the legs
	    	{
		        for (int m = 0; m <= maxLegTime; m++ ) {
	                int availTime = tourLength - m;
		            if (availTime <= maxLegTime ) {	                        
		            	alts[0][count] = m;
		                alts[1][count] = availTime*obAvail;
		                alts[2][count] = availTime*ibAvail;
		                count++;	                
	                }
		        }
	    	}
	    	else  // at least one stop on both legs 
		        for (int m = 0; m <= maxLegTime; m++ ) {
	                for (int ob = 0; ob <= (tourLength - m); ob++) {
	                	if (ob <= maxLegTime ) {
	                		int ib = tourLength - m - ob;
		                    if (ib <= maxLegTime ) {	                        
		                    	alts[0][count] = m;
		                    	alts[1][count] = ob;
		                    	alts[2][count] = ib;
		                        count++;	                
		                    }
	                	}
	                }
		        }
	        
	        // copy content of alts to stageOneAlternatives
	        int[][] legTimingPatterns = new int[3][count];
	        for (int i = 0; i < 3; i++)
	        	legTimingPatterns[i] = Arrays.copyOfRange(alts[i], 0, count);
	        
/*	        // write legTimingPatterns out if we have a trace household
	    	Household householdObject = tour.getPersonObject().getHouseholdObject();
	        if(householdObject.getDebugChoiceModels()){
	            LogitModel.setLogger(stc1Logger);

	            householdObject.logHouseholdObject( "Pre Leg Timing Choice for tour: HH_" + householdObject.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPurpose() + ", Tour_" + tour.getTourId() + ", Tour Purpose_" + tour.getTourPurpose(), stc1Logger );
	            householdObject.logPersonObject( "Pre Leg Timing Choice for person " + tour.getPersonObject().getPersonNum(), stc1Logger, tour.getPersonObject() );
	            householdObject.logTourObject("Pre Leg Timing Choice for tour " + tour.getTourId(), stc1Logger, tour.getPersonObject(), tour );

                stc1Logger.info(" ");
	            stc1Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            stc1Logger.info("Stop Timing Choice - Stage One Model: Debug Statement for Household ID: "+householdObject.getHhId()+ ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPurpose() + ", Tour_" + tour.getTourId() );
	            stc1Logger.info("");
	            stc1Logger.info("Leg Timing Alts       Main Leg         Ob Leg        Ib Leg");
	            stc1Logger.info("---------------  -------------- -------------- --------------");
	            
	            int numAlts = legTimingPatterns[0].length;
	            for (int j = 0; j < numAlts; j++) {
		            stc1Logger.info(String.format("%-14d  %-14d  %-14d  %-14d", 
		            		j, legTimingPatterns[0][j], legTimingPatterns[1][j], legTimingPatterns[2][j] ) );
	            }
	            stc1Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	        }
*/
	        return legTimingPatterns;
	    }

	    private void applyStageTwoModel( int[] legTimeSplit, Tour tour, Person person, Household household ) {
	    	
	    	int mainLegTime = legTimeSplit[0];
	    	int obLegTime = legTimeSplit[1];
	    	int ibLegTime = legTimeSplit[2];
	    	
	    	int[] stopTimeSplit;
	    	
/*	    	Stop[] obStops = tour.getOutboundStops();
	    	
	    	if ( obStops!=null ) {
	    		for ( Stop thisStop: obStops ) {
	    			thisStop.setDepartPeriod( tour.getDepartPeriod() );
	    		}
	    	}
	    	
	    	Stop[] ibStops = tour.getInboundStops();
	    	
	    	if ( ibStops!=null ) {
	    		for ( Stop thisStop: ibStops ) {
	    			thisStop.setDepartPeriod( tour.getArrivePeriod() );
	    		}
	    	}*/
	    			    	
	    	// process outbound leg first
	    	int numObStops = tour.getNumOutboundStops(); 
    		Stop[] outboundStops = tour.getOutboundStops();
	    	
	    	if ( numObStops>1 ) {

	    		// split time across more than 1 stop on the leg
	    		stopTimeSplit = selectStopTime(obLegTime, true, tour, person, household);
	    		
	    		// set stop departure times
    			int departTime = tour.getTourDepartPeriod(); 
    			outboundStops[0].setStopPeriod( departTime );
	    		for (int s = 0; s < numObStops; s++) {
	   				departTime += stopTimeSplit[s];
	    			outboundStops[s+1].setStopPeriod( departTime );
	    		}
	    			    	
	    	}
	    	else if ( numObStops==1 ) { // no need to split leg time
	    	
	    		int departTime = tour.getTourDepartPeriod(); 
	    		outboundStops[0].setStopPeriod( departTime );
    			outboundStops[1].setStopPeriod( departTime + obLegTime );	    		
	    		
	    	}
	    	else {
	    		// no stops on the leg
	    		// TODO: anything?
	    	}

	    	// process inbound leg 
	    	int numIbStops = tour.getNumInboundStops(); 
    		Stop[] inboundStops = tour.getInboundStops();	    	
	    	
	    	if ( numIbStops>1 ) {

	    		// split time across more than 1 stop on the leg
	    		stopTimeSplit = selectStopTime(ibLegTime, false, tour, person, household);
	    		
	    		// set stop departure times
    			int departTime = tour.getTourDepartPeriod() + obLegTime + mainLegTime; 
    			inboundStops[0].setStopPeriod( departTime );
	    		for (int s = 0; s < numIbStops; s++) {
	   				departTime += stopTimeSplit[s];
	    			inboundStops[s+1].setStopPeriod( departTime );
	    		}
	    		// note: departure period of inboundStops[numIbStops] should work to be the same as arrival period of tour
	    	}
	    	else if ( numIbStops==1 ) { // no need to split leg time

    			inboundStops[0].setStopPeriod( tour.getTourDepartPeriod() + obLegTime + mainLegTime );
    			inboundStops[1].setStopPeriod( tour.getTourArrivePeriod() );	    		
	    	}
	    	else {
	    		// no stops on the inbound leg
	    		// TODO anything?
	    	}
	    	
	    }


	    private int[] selectStopTime( int legLength, boolean isOutbound, Tour tour, Person person, Household household ) {
	    	   	
	    	stStageTwoDmuObj.setTourObject(tour);
	    	stStageTwoDmuObj.setPersonObject(person);
	    	stStageTwoDmuObj.setHouseholdObject(household);
	    	stStageTwoDmuObj.setLegTotalDuration(legLength);

	    	int numStopsOnLeg = isOutbound? tour.getNumOutboundStops(): tour.getNumInboundStops();
	    	Stop[] stops = isOutbound? tour.getOutboundStops() : tour.getInboundStops();
	    	double[][] utilities = new double[numStopsOnLeg][];	// 0-based 

	    	// ******** First, compute utility segments for each stop on leg **********
	    	
		    // write header out if we have a trace household
	        if(household.getDebugChoiceModels()){

	            String legStr = isOutbound? "Outbound Leg" : "Inbound Leg";
	            stc2Logger.info(" ");
	            stc2Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            if ( person==null )
		            stc2Logger.info("Stop Timing Choice - Stage Two Model: Debug Statement for Household Id: "+household.getHhId()+
	        				", Joint Tour Id: "+tour.getTourId()+", "+legStr );
	            else
	            	stc2Logger.info("Stop Timing Choice - Stage Two Model: Debug Statement for Household Id: "+household.getHhId()+
	            			", Person Num: "+person.getPersonNum()+", Tour Id: "+tour.getTourId()+", "+legStr );
	            stc2Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            stc2Logger.info("");
	        }
	        
	    	if ( isOutbound ) {

	    		UtilityExpressionCalculator stopUec = obStopUec;
	    		
		    	// compute utility for each stop duration alternative for each stop on the leg
		    	int origTaz = tour.getTourOrigMgra();
		    	int destTaz = -1;
		    	
		    	for ( int s=0; s<numStopsOnLeg; s++ ) {
		    		
			    	
			    	// compute FFT and pass onto DMU
		    		destTaz = stops[s].getDest();
			    	double tripFFT = computeTripFFT(origTaz, destTaz, household);
			    	// setup the DMU
			    	stStageTwoDmuObj.setStopObject( stops[s] );
			    	stStageTwoDmuObj.setDmuIndexValues(household.getHhId(),origTaz, destTaz);
			    	stStageTwoDmuObj.setTripTimeToStop( tripFFT );
			    	
			    	// compute utility for the stop duration alternatives
			    	utilities[s] = stopUec.solve(stStageTwoDmuObj.getIndexValues(), stStageTwoDmuObj, null);
		    	
			        if(household.getDebugChoiceModels()){
			            stopUec.logAnswersArray(stc2Logger, "Stop #"+stops[s].getStopId());
			        }
			        
			        // update orig for next trip
			        origTaz = destTaz;
			    }

	    	} // end processing outbound leg
	    	else { // is Inbound 
	    		
	    		UtilityExpressionCalculator stopUec = ibStopUec;
	    		
		    	// compute utility for each stop duration alternative for each stop on the leg  
	    		int origTaz = -1;
	    		int destTaz = tour.getTourOrigMgra();
		    	for ( int s=numStopsOnLeg-1; s>=0; s-- ) { // process last stop first
			    	
			    	// compute FFT and pass onto DMU
		    		origTaz = stops[s].getDest();
			    	double tripFFT = computeTripFFT(origTaz, destTaz, household);
		    		
			    	// setup the DMU
			    	stStageTwoDmuObj.setStopObject( stops[s] );
			    	stStageTwoDmuObj.setDmuIndexValues(household.getHhId(), origTaz, destTaz);
			    	stStageTwoDmuObj.setTripTimeToStop( tripFFT );
			    	
			    	// compute utility for the stop duration alternatives
			    	utilities[s] = stopUec.solve(stStageTwoDmuObj.getIndexValues(), stStageTwoDmuObj, null);
		    	
			        if(household.getDebugChoiceModels()){
			            stopUec.logAnswersArray(stc2Logger, "Stop #"+stops[s].getStopId());
			            stopUec.logResultsArray(stc2Logger);
			            
			            stc2Logger.info("MY OWN UTILITIES ARRAY");
						String stringToLog="";
		    			for(int l=0;l<utilities[s].length;++l){
		    				stringToLog  += String.format("%10.4f", utilities[s][l]);
		    			}
		    			stc2Logger.info(stringToLog);

			        }
			        
			        // update dest for next trip
			        destTaz = origTaz;
			    }
		    } // end processing inbound leg
	    	
	    	
	    	// ******** Next, compute total utility for each timing pattern alternative **********

	    	// Given leg length, generate all possible stop timing patterns for the leg
	    	int[][] stopTimingPatterns= generateStopTimingPatterns( legLength, numStopsOnLeg, tour );  // 0-based
	    	int numStopTimingPatterns = stopTimingPatterns[0].length;

	    	if(household.getDebugChoiceModels()){
	    		stc2Logger.info("");
	            stc2Logger.info("Total number of stop timing patterns: "+numStopTimingPatterns+" with "+numStopsOnLeg+" stops on leg" );
	    	}
	    	
		    // create the choice model 
		    LogitModel stageTwoChoiceModel = new LogitModel("Stage Two of Stop Timing Choice Model", 0, numStopTimingPatterns);

	    	// add the stop timing pattern choice alternatives to the logit model 
	    	for ( int i = 0; i < numStopTimingPatterns; i++ ) {
		    	
	    		// format alternative name as "StopOneTime_StopTwoTime_..." 
	    		String alternativeName = "";
	    		for ( int s = 0; s < numStopsOnLeg; s++ )
	    			alternativeName += String.valueOf(stopTimingPatterns[s][i])+"_";
	            
	            ConcreteAlternative newAlt = new ConcreteAlternative(alternativeName, i);
	            stageTwoChoiceModel.addAlternative(newAlt);
	    	}
	    	    	
	    	// loop through the all timing pattern alternatives
	    	// compute total utility for each pattern by adding up each stop components
	        Alternative[] altPatternList = stageTwoChoiceModel.getAlternatives();
	    	int stopTime;
	    	int alt;
	    	for ( int i = 0; i < numStopTimingPatterns; i++ ) { 
	    		
	    		double totalUtility = 0;
	    		
	    		for ( int s = 0; s < numStopsOnLeg; s++ ) {
	    			stopTime = stopTimingPatterns[s][i]; // length of the s'th stop for the 'i'th timing pattern
	    			// TODO- map duration to alt no. in the utility array 
	    			alt = stopTime; // this works for now because utilities[] is 0-based
			    	
			        if(household.getDebugChoiceModels()){
		            	stc2Logger.info("Stop #" + stops[s].getStopId() + " of Pattern #" + i + " has Stop Time = " + stopTime + ", Utility = "+ utilities[s][alt]);

			        }
	    			
	    			totalUtility += utilities[s][alt];
	    		}
	    		// set the total utility in the choice model
    			altPatternList[i].setUtility(totalUtility);
	    	}    	
	    	
	        // compute the exponentiated utility, logging debug info needed
	        if(household.getDebugChoiceModels()){
	        	stageTwoChoiceModel.setDebug(true);
	        	stageTwoChoiceModel.writeUtilityHeader();
	        }
	        stageTwoChoiceModel.getUtility();
	        
	        stageTwoChoiceModel.calculateProbabilities();
	        
	        // make a leg timing pattern choice 
	        Random hhRandom = household.getHhRandom();
	        double randomNumber = hhRandom.nextDouble();
            ConcreteAlternative chosen = (ConcreteAlternative) stageTwoChoiceModel.chooseAlternative(randomNumber);

	        // write choice utilities out if we have a trace household
	        if(household.getDebugChoiceModels()){
	            for (int i = 0; i < numStopTimingPatterns; i++) {
	            	Alternative thisAlt = stageTwoChoiceModel.getAlternatives()[i];
	            	stc2Logger.info(
	                        	String.format("%-20s", thisAlt.getName())
	                            + "\t\t"
	                            + String.format("%-14.8e", thisAlt.getUtility())
	                            + "\t\t\t"
	                            + String.format("%-14.8e", thisAlt.getExpUtility()) );	            
	            } // end for each alt

	            stc2Logger.info("");
	            stc2Logger.info( String.format("Choice: %s, with rn=%.8f ", chosen.getName(), randomNumber ) );
	            stc2Logger.info("");
	            stc1Logger.info("");
	        } // end if trace     

            int alternativeNumber = chosen.getNumber();
            
            // prepare array of stop times to return to caller
            int[] stopTimeSplit = new int[numStopsOnLeg];            
            for ( int s=0; s < numStopsOnLeg; s++ ) {
            	stopTimeSplit[s] = stopTimingPatterns[s][alternativeNumber];
            }
	        
            return stopTimeSplit;
	    }
	   
	    
    	/*
    	 * Given the outbound or inbound leg duration, generate stage two choice alternatives that  
    	 * include all possible ways of splitting the duration across stops. Each slice includes trip time and stop duration  
    	 * 		stopTimingPatterns[0] - value for stop #0
	     * 		stopTimingPatterns[1] - value for stop #1
	     *		stopTimingPatterns[2] - value for stop #2 ....
	     *
	     * Returns the number of choice alternatives generated 
	     */   
	    private int[][] generateStopTimingPatterns( int legLength, int numStops, Tour tour) {
	    	
	    	// maximum duration of a stop is capped at MAX_STOP_DURATION
	    	int maxStopTime = Math.min( legLength, MAX_STOP_DURATION );
	    	
	    	int[][] alts = new int[4][ (maxStopTime+1)*(maxStopTime+1) ];
	    	int[] time = new int[4];
	    	int[] maxTime = new int[4];
	    	int[] available = new int[4]; 
	    	
	    	// only the first 'numStops stops are available
	    	// unavailable stops get 0 time
	    	for ( int s = 0; s < numStops; s++ )
	    		    available[s] = 1;
	    	for ( int s = numStops; s < 4; s++ )
	    		    available[s] = 0;
	    	
	    	int count = 0;
	        
	    	maxTime[1] = maxStopTime * available[1];
	    	
	    	for ( time[1] = 0; time[1] <= maxTime[1]; time[1]++ ) {
	        	
	        	// update maxTime[2]
	        	maxTime[2] = available[2] * (maxTime[1] - time[1]);
	        	
                for ( time[2] = 0; (time[2] <= maxTime[2]) && (time[2] <= maxStopTime) ; time[2]++) {
	                    
                	// update maxTime[3]
                	maxTime[3] = available[3] * (maxTime[2] - time[2]);
                	
                	for ( time[3] = 0; (time[3] <= maxTime[3]) && (time[3] <= maxStopTime); time[3]++) {
                		                                  		
                		time[0]= legLength - time[1] - time[2] - time[3];

                		if (time[0] <= maxStopTime ) {	                        
	                    	alts[0][count] = time[0];
	                    	alts[1][count] = time[1];
	                    	alts[2][count] = time[2];
	                    	alts[3][count] = time[3];
	                        count++;	                
	                    }
                	}
                }
	        }
	        
	        // resize array to return to caller
	        int[][] stopTimingPatterns = new int[numStops][count];
	        for (int i = 0; i < numStops; i++)
	        	stopTimingPatterns[i] = Arrays.copyOfRange(alts[i], 0, count);
	        
	        // write stopTimingPatterns out if we have a trace household
	    	Household householdObject = tour.getPersonObject().getHouseholdObject();
	        if(householdObject.getDebugChoiceModels()){

	            stc2Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	            stc2Logger.info("");
	            stc2Logger.info("Stop Timing Alts         Stop 0         Stop 1         Stop 2         Stop 3 ");
	            stc2Logger.info("----------------  -------------- -------------- -------------- --------------");
	            
	            for (int j = 0; j < count; j++) {
	            	stc2Logger.info(String.format("%-28d  %-14d  %-14d  %-14d  %-14d", 
		            		j, alts[0][j], alts[1][j], alts[2][j], alts[3][j] ) );
	            }
	            stc2Logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	        }

	        return stopTimingPatterns;
	    }

	    private double computeTripFFT(int origTaz, int destTaz, Household household) {
	    	   		
		    // attributes needed for the DMU/UEC
	        boolean debugFlag = false;
	        if ( household.getDebugChoiceModels() )
	        	debugFlag = household.getDebugChoiceModels();
		        
	        int[] availability = new int[2];
	        availability[1] = 1;
		
    		// look up trip travel time
    		tripTimeDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), origTaz, destTaz, debugFlag );
            double tripTime[] = tripTimeUec.solve( tripTimeDmuObj.getDmuIndexValues(), tripTimeDmuObj, availability );
		
            return tripTime[0];
	    	
	    }
}
