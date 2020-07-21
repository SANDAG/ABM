package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;

public class MicromobilityChoiceModel
        implements Serializable
{

    private transient Logger       logger                 = Logger.getLogger("micromobility");

    private static final String    MM_CONTROL_FILE_TARGET = "micromobility.uec.file";
    private static final String    MM_DATA_SHEET_TARGET   = "micromobility.data.page";
    private static final String    MM_MODEL_SHEET_TARGET  = "micromobility.model.page";
    private static final String    MT_TAP_FILE_TARGET     = "active.microtransit.tap.file";
    private static final String    MT_MAZ_FILE_TARGET     = "active.microtransit.mgra.file";

    public static final int        MM_MODEL_WALK_ALT                = 0;
    public static final int        MM_MODEL_MICROMOBILITY_ALT       = 1;
    public static final int        MM_MODEL_MICROTRANSIT_ALT       = 2;
    

    private ChoiceModelApplication mmModel;
    private MicromobilityChoiceDMU   mmDmuObject;

    // following arrays used to store ivt coefficients, and income coefficients, income exponents to calculate cost coefficient, by tour purpose 
    double[]                         ivtCoeffs;
    double[]                         incomeCoeffs;
    double[]                         incomeExponents;

    private static final String       PROPERTIES_TRIP_UTILITY_IVT_COEFFS          = "trip.utility.ivt.coeffs";
    private static final String       PROPERTIES_TRIP_UTILITY_INCOME_COEFFS       = "trip.utility.income.coeffs"; 
    private static final String       PROPERTIES_TRIP_UTILITY_INCOME_EXPONENTS    = "trip.utility.income.exponents";
    private ModelStructure                     modelStructure;
    private MgraDataManager mgraDataManager;

    private HashSet<Integer> microtransitTaps;
    private HashSet<Integer> microtransitMazs;
    
    
    public MicromobilityChoiceModel(HashMap<String, String> propertyMap,
    		ModelStructure myModelStructure, CtrampDmuFactoryIf dmuFactory)
    {
        
        setupMicromobilityChoiceModelApplication(propertyMap, myModelStructure, dmuFactory);
    }

    private void setupMicromobilityChoiceModelApplication(HashMap<String, String> propertyMap,
    		ModelStructure myModelStructure,  CtrampDmuFactoryIf dmuFactory)
    {
        logger.info("setting up micromobility choice model.");

        modelStructure = myModelStructure;
        
        // locate the micromobility choice UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mmUecFile = uecFileDirectory + propertyMap.get(MM_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, MM_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, MM_MODEL_SHEET_TARGET);

        // create the micromobility choice model DMU object.
        mmDmuObject = dmuFactory.getMicromobilityChoiceDMU();

        // create the transponder choice model object
        mmModel = new ChoiceModelApplication(mmUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) mmDmuObject);
        
        
        //get the coefficients for ivt and the coefficients to calculate the cost coefficient
        ivtCoeffs = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TRIP_UTILITY_IVT_COEFFS);
        incomeCoeffs = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TRIP_UTILITY_INCOME_COEFFS);
        incomeExponents = Util.getDoubleArrayFromPropertyMap(propertyMap, PROPERTIES_TRIP_UTILITY_INCOME_EXPONENTS);
        
        mgraDataManager = MgraDataManager.getInstance(); 
        
        String projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String microTransitTapFile = projectDirectory + propertyMap.get(MT_TAP_FILE_TARGET);
        String microTransitMazFile = projectDirectory + propertyMap.get(MT_MAZ_FILE_TARGET);
        
        TableDataSet microTransitTapData = Util.readTableDataSet(microTransitTapFile);
        TableDataSet microTransitMazData = Util.readTableDataSet(microTransitMazFile);
        
        microtransitTaps = new HashSet<Integer>();
        microtransitMazs = new HashSet<Integer>();
        
        for(int i=1;i<=microTransitTapData.getRowCount();++i) {
        	
        	int tap = (int) microTransitTapData.getValueAt(i,"TAP");
        	microtransitTaps.add(tap);
        }
        	
       for(int i=1;i<=microTransitMazData.getRowCount();++i) {
        	
        	int maz = (int) microTransitMazData.getValueAt(i,"MGRA");
        	microtransitMazs.add(maz);
        }
      

    }
    
    
    /**
     * Apply model to all trips for the household. 
     * 
     * @param household
     */
    public void applyModel(Household household) {
    	
    	for(Person person : household.getPersons()) {
    		
    		if(person==null)
    			continue;
    		
    		//work tours
    		if(person.getListOfWorkTours()!=null) {
    			
    			for(Tour tour:person.getListOfWorkTours())
    				applyModel(household, person, tour);
    		}
    		
    		//school tours
    		if(person.getListOfSchoolTours()!=null) {
    			
    			for(Tour tour:person.getListOfSchoolTours())
    				applyModel(household, person, tour);
    		}
   		
    		//non-mandatory tours
    		if(person.getListOfIndividualNonMandatoryTours()!=null) {
    			
    			for(Tour tour:person.getListOfIndividualNonMandatoryTours())
    				applyModel(household, person, tour);
    		}
    		
    		//at-work sub tours
    		if(person.getListOfAtWorkSubtours()!=null) {
    			
    			for(Tour tour:person.getListOfAtWorkSubtours())
    				applyModel(household, person, tour);
    		}

    	}
    	
    	
    }
    
    public void applyModel(Household household, Person person, Tour tour) {
    	
    	//apply to outbound stops
    	if(tour.getOutboundStops()!=null) {
    		
    		for(Stop s: tour.getOutboundStops())
    			applyModel(household, person, tour, s);
    	}
    		
    	//apply to inbound stops
    	if(tour.getInboundStops()!=null) {
    		
    		for(Stop s: tour.getInboundStops())
    			applyModel(household, person, tour, s);
    	}
    	
    	
    }

    public void applyModel(Household household, Person person, Tour tour, Stop s)
    {
    	
    	if(tour==null)
    		return;


    	if(!modelStructure.getTourModeIsWalk(s.getMode()) && !modelStructure.getTourModeIsWalkTransit(s.getMode())&& !modelStructure.getTourModeIsDriveTransit(s.getMode()))
    		return;
    	
        int homeMaz = household.getHhMgra();
        double income = (double) household.getIncomeInDollars();
        
        int category = IntermediateStopChoiceModels.PURPOSE_CATEGORIES[tour.getTourPrimaryPurposeIndex()];
        double ivtCoeff    = ivtCoeffs[category];
        double incomeCoeff = incomeCoeffs[category];
        double incomeExpon = incomeExponents[category];
        double costCoeff = calculateCostCoefficient(income, incomeCoeff,incomeExpon);
        double timeFactor = 1.0f;
        if(tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
           	timeFactor = tour.getJointTourTimeFactor();
        else if(tour.getTourPrimaryPurposeIndex()==ModelStructure.WORK_PRIMARY_PURPOSE_INDEX)
        	timeFactor = person.getTimeFactorWork();
        else
        	timeFactor = person.getTimeFactorNonWork();
            
        mmDmuObject.setIvtCoeff(ivtCoeff * timeFactor);
        mmDmuObject.setCostCoeff(costCoeff);
        int originMaz = s.getOrig();
        int destMaz = s.getDest();

        if(modelStructure.getTourModeIsWalk(s.getMode()))
	        mmDmuObject.setTransit(false);
        else
	        mmDmuObject.setTransit(true);
        
        if(modelStructure.getTourModeIsWalk(s.getMode())) {
            
        	float walkTime = mgraDataManager.getMgraToMgraWalkTime(originMaz, destMaz); 
            mmDmuObject.setWalkTime(walkTime);
            
            if(microtransitMazs.contains(originMaz) && microtransitMazs.contains(destMaz))
            	mmDmuObject.setMicroTransitAvailable(true);
            else
            	mmDmuObject.setMicroTransitAvailable(false);
            	
            
            //set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
            mmDmuObject.setDmuIndexValues(household.getHhId(), originMaz, originMaz, originMaz);
	
            // compute utilities and choose micromobility choice alternative.
            float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
            s.setMicromobilityWalkLogsum(logsum);
            
            // if the choice model has at least one available alternative, make choice
            byte chosenAlt = (byte) getChoice(household, person, tour, s);
            s.setMicromobilityWalkMode(chosenAlt);
            	
            // write choice model alternative info to log file
	        if (household.getDebugChoiceModels())
	        {
	        	String decisionMaker = String.format("Household " + household.getHhId()+ "Person " + person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
	        	//String decisionMaker = String.format("Household %d", household.getHhId()+ "Person %d", person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
	            mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
	            logger.info(String.format("%s result chosen for %s is %d",
	            	"Micromobility Choice", decisionMaker, chosenAlt));
	            mmModel.logUECResults(logger, decisionMaker);
	        }

         }else if(modelStructure.getTourModeIsWalkTransit(s.getMode())) {
        		   
        	//access
        	int tapPosition =  mgraDataManager.getTapPosition(originMaz, s.boardTap);
        	if(tapPosition==-1) {
        		logger.warn("Problem with hh "+household.getHhId()+" Person "+person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
        		logger.warn("Origin MAZ "+originMaz+ " Board TAP "+s.boardTap+ " Alight TAP "+s.alightTap+" Destination MAZ "+destMaz);
        		logger.warn("Can't find walk connection from origin to board TAP; skipping micromobility choice");
        		return;
        	}
        	float walkTime = mgraDataManager.getMgraToTapWalkTime(originMaz, tapPosition);
            mmDmuObject.setWalkTime(walkTime);
            
            if(microtransitMazs.contains(originMaz) && microtransitTaps.contains(s.boardTap))
            	mmDmuObject.setMicroTransitAvailable(true);
            else
            	mmDmuObject.setMicroTransitAvailable(false);

            
            //set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
            mmDmuObject.setDmuIndexValues(household.getHhId(), originMaz, originMaz, originMaz);

        	// compute utilities and choose micromobility choice alternative.
            float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
            s.setMicromobilityAccessLogsum(logsum);
               
            // if the choice model has at least one available alternative, make choice
	        byte chosenAlt = (byte) getChoice(household, person, tour, s);
	        s.setMicromobilityAccessMode(chosenAlt);

		    // write choice model alternative info to log file
		    if (household.getDebugChoiceModels())
		    {
		    	String decisionMaker = String.format("Household " + household.getHhId()+ "Person " + person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
		    	//String decisionMaker = String.format("Household %d", household.getHhId()+ "Person %d", person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode()+ " access choice");
		        mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
		        logger.info(String.format("%s result chosen for %s is %d",
		        	"Micromobility Choice", decisionMaker, chosenAlt));
		        mmModel.logUECResults(logger, decisionMaker);
		    }
	        //egress
	        tapPosition =  mgraDataManager.getTapPosition(destMaz, s.alightTap);
        	if(tapPosition==-1) {
        		logger.warn("Problem with hh "+household.getHhId()+" Person "+person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
        		logger.warn("Origin MAZ "+originMaz+ " Board TAP "+s.boardTap+ " Alight TAP "+s.alightTap+" Destination MAZ "+destMaz);
        		logger.warn("Can't find walk connection from alight TAP to destination; skipping micromobility choice");
        		return;
        	}
	        walkTime = mgraDataManager.getMgraToTapWalkTime(destMaz, tapPosition);
	        mmDmuObject.setWalkTime(walkTime);

            if(microtransitMazs.contains(destMaz) && microtransitTaps.contains(s.alightTap))
            	mmDmuObject.setMicroTransitAvailable(true);
            else
            	mmDmuObject.setMicroTransitAvailable(false);

	        //set destination to closest mgra to alighting TAP so that Z can be used to find access to mode in mgra data file in UEC
	        int closestMazToAlightTap = mgraDataManager.getClosestMgra(s.alightTap);
	        mmDmuObject.setDmuIndexValues(household.getHhId(), closestMazToAlightTap, closestMazToAlightTap, closestMazToAlightTap);
	            
	        // compute utilities and choose micromobility choice alternative.
	        logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
	        s.setMicromobilityEgressLogsum(logsum);
	                
	        // if the choice model has at least one available alternative, make choice
		    chosenAlt = (byte) getChoice(household, person, tour, s);
		    s.setMicromobilityEgressMode(chosenAlt);

		    // write choice model alternative info to log file
		    if (household.getDebugChoiceModels())
		    {
		    	String decisionMaker = String.format("Household " + household.getHhId()+ "Person " + person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
		      	//String decisionMaker = String.format("Household %d", household.getHhId()+ "Person %d", person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode()+ " egress choice");
		       	mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
		        logger.info(String.format("%s result chosen for %s is %d",
		           		"Micromobility Choice", decisionMaker, chosenAlt));
		        mmModel.logUECResults(logger, decisionMaker);
		    }
		    

        } else if( modelStructure.getTourModeIsDriveTransit(s.getMode()) ) {     	   //drive-transit. Choose non-drive direction
        	     
        	int tapPosition = 0;
        	float walkTime = 9999;
        		
        	if(s.isInboundStop()) { //inbound, so access mode is walk
        		tapPosition =  mgraDataManager.getTapPosition(originMaz, s.boardTap);
            	if(tapPosition==-1) {
            		logger.warn("Problem with hh "+household.getHhId()+" Person "+person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
            		logger.warn("Origin MAZ "+originMaz+ " Board TAP "+s.boardTap+ " Alight TAP "+s.alightTap+" Destination MAZ "+destMaz);
            		logger.warn("Can't find walk connection from origin to board TAP; skipping micromobility choice");
            		return;
            	}

        		walkTime = mgraDataManager.getMgraToTapWalkTime(originMaz, tapPosition);
        		//set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
        		mmDmuObject.setDmuIndexValues(household.getHhId(), originMaz, originMaz, originMaz);
        		   
                if(microtransitMazs.contains(originMaz) && microtransitTaps.contains(s.boardTap))
                	mmDmuObject.setMicroTransitAvailable(true);
                else
                	mmDmuObject.setMicroTransitAvailable(false);
                
        	}else { //outbound so egress mode is walk.
        		   tapPosition =  mgraDataManager.getTapPosition(destMaz, s.alightTap);
        		   if(tapPosition==-1) {
        			   logger.warn("Problem with hh "+household.getHhId()+" Person "+person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
        			   logger.warn("Origin MAZ "+originMaz+ " Board TAP "+s.boardTap+ " Alight TAP "+s.alightTap+" Destination MAZ "+destMaz);
        			   logger.warn("Can't find walk connection from destination MAZ to alight TAP; skipping micromobility choice");
        			   return;
        		   }
        		   walkTime = mgraDataManager.getMgraToTapWalkTime(destMaz, tapPosition);
        		   //set destination to closest mgra to alighting TAP so that Z can be used to find access to mode in mgra data file in UEC
   	            	int closestMazToAlightTap = mgraDataManager.getClosestMgra(s.alightTap);
   	            	mmDmuObject.setDmuIndexValues(household.getHhId(), closestMazToAlightTap, closestMazToAlightTap, closestMazToAlightTap);

   	            	if(microtransitMazs.contains(destMaz) && microtransitTaps.contains(s.alightTap))
   	            		mmDmuObject.setMicroTransitAvailable(true);
                   else
                   		mmDmuObject.setMicroTransitAvailable(false);
        	
        	}
	        mmDmuObject.setWalkTime(walkTime);
	
        	// compute utilities and choose micromobility choice alternative.
            float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
               		
            // if the choice model has at least one available alternative, make choice
            byte chosenAlt = (byte) getChoice(household, person, tour, s);
            	
            if(s.isInboundStop()) { //inbound, set access
            	s.setMicromobilityAccessMode(chosenAlt);
               	s.setMicromobilityAccessLogsum(logsum);
            }else { //outound, set egress
            	s.setMicromobilityEgressMode(chosenAlt);
               	s.setMicromobilityEgressLogsum(logsum);
            }
	               	
            // write choice model alternative info to log file
	        if (household.getDebugChoiceModels())
	        {
	        	String decisionMaker = String.format("Household " + household.getHhId()+ "Person " + person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
	        	//String decisionMaker = String.format("Household %d", household.getHhId()+ "Person %d", person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
	            mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
	            logger.info(String.format("%s result chosen for %s is %d",
	                     "Micromobility Choice", decisionMaker, chosenAlt));
	            mmModel.logUECResults(logger, decisionMaker);
	        }
     		   
       	 }
        
      }
        
        
    /**
     * Select the micromobility mode from the UEC. This  is helper code for applyModel(), where utilities have already been calculated.
     * 
     * @param household
     * @param person
     * @param tour
     * @param s
     * @return The micromobility mode.
     */
       private int getChoice(Household household, Person person, Tour tour, Stop s) {
        	// if the choice model has at least one available alternative, make
        	// choice.
        	int chosenAlt;
        	Random hhRandom = household.getHhRandom();
        	if (mmModel.getAvailabilityCount() > 0)
        	{
        		double randomNumber = hhRandom.nextDouble();
        		chosenAlt = mmModel.getChoiceResult(randomNumber);
        		return chosenAlt;
        	} else
        	{
        		String decisionMaker = String.format("Household " + household.getHhId()+ "Person " + person.getPersonNum()+" "+tour.getTourCategory()+" tour ID "+tour.getTourId()+ "stop "+s.getStopId()+ " mode " +s.getMode());
        		String errorMessage = String
                    .format("Exception caught for %s, no available micromobility choice alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
        		logger.info(errorMessage);
        		logger.info("Setting mode to walk");

        		mmModel.logUECResults(logger, decisionMaker);
        		return MM_MODEL_WALK_ALT;
        	}

        }

        /**
         * This method calculates a cost coefficient based on the following formula:
         * 
         *   costCoeff = incomeCoeff * 1/(max(income,1000)^incomeExponent)
         * 
         * 
         * @param incomeCoeff
         * @param incomeExponent
         * @return A cost coefficent that should be multiplied by cost variables (cents) in tour mode choice
         */
        public double calculateCostCoefficient(double income, double incomeCoeff, double incomeExponent){
        	
        	return incomeCoeff * 1.0/(Math.pow(Math.max(income,1000.0),incomeExponent));
        	
        }

}
