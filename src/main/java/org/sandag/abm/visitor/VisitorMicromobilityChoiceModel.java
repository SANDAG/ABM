package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class VisitorMicromobilityChoiceModel
        implements Serializable
{

    private transient Logger       logger                 = Logger.getLogger("micromobility");

    private static final String    MM_CONTROL_FILE_TARGET = "visitor.micromobility.uec.file";
    private static final String    MM_DATA_SHEET_TARGET   = "visitor.micromobility.data.page";
    private static final String    MM_MODEL_SHEET_TARGET  = "visitor.micromobility.model.page";

    public static final int        MM_MODEL_NO_ALT        = 0;
    public static final int        MM_MODEL_YES_ALT       = 1;

    private ChoiceModelApplication mmModel;
    private VisitorMicromobilityChoiceDMU   mmDmuObject;

    private VisitorModelStructure                     modelStructure;
    private MgraDataManager mgraDataManager;

    
    public VisitorMicromobilityChoiceModel(HashMap<String, String> propertyMap,
    		VisitorModelStructure myModelStructure, VisitorDmuFactoryIf dmuFactory)
    {
        
        setupMicromobilityChoiceModelApplication(propertyMap, myModelStructure, dmuFactory);
    }

    private void setupMicromobilityChoiceModelApplication(HashMap<String, String> propertyMap,
    		VisitorModelStructure myModelStructure,  VisitorDmuFactoryIf dmuFactory)
    {
       // logger.info("setting up micromobility choice model.");

        modelStructure = myModelStructure;
        
        // locate the micromobility choice UEC
        String uecFileDirectory = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mmUecFile = uecFileDirectory + propertyMap.get(MM_CONTROL_FILE_TARGET);

        int dataSheet = Util.getIntegerValueFromPropertyMap(propertyMap, MM_DATA_SHEET_TARGET);
        int modelSheet = Util.getIntegerValueFromPropertyMap(propertyMap, MM_MODEL_SHEET_TARGET);

        // create the micromobility choice model DMU object.
        mmDmuObject = dmuFactory.getVisitorMicromobilityChoiceDMU();

        // create the transponder choice model object
        mmModel = new ChoiceModelApplication(mmUecFile, modelSheet, dataSheet, propertyMap,
                (VariableTable) mmDmuObject);
        
        mgraDataManager = MgraDataManager.getInstance(); 

    }
    
    
    public void applyModel(VisitorTour tour) {
    	
    	//apply to trips on tour
    	if(tour.getTrips()!=null) {
    		
    		for(VisitorTrip trip: tour.getTrips())
    			applyModel(tour, trip);
    	}
    		
    	
    }

    public void applyModel(VisitorTour tour, VisitorTrip trip)
    {


    	if(!modelStructure.getTourModeIsWalk(trip.getTripMode()) && !modelStructure.getTourModeIsWalkTransit(trip.getTripMode())&& !modelStructure.getTourModeIsDriveTransit(trip.getTripMode()))
    		return;
    	
        mmDmuObject.setIncome(tour.getIncome());
        int originMaz = trip.getOriginMgra();
        int destMaz = trip.getDestinationMgra();

        if(modelStructure.getTourModeIsWalk(trip.getTripMode())) {
            
        	float walkTime = mgraDataManager.getMgraToMgraWalkTime(originMaz, destMaz); 
            mmDmuObject.setWalkTime(walkTime);
            
            //set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
            mmDmuObject.setDmuIndexValues(tour.getID(), originMaz, originMaz, originMaz);
	
            // compute utilities and choose micromobility choice alternative.
            float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
            trip.setMicromobilityWalkLogsum(logsum);
            
            // if the choice model has at least one available alternative, make choice
            byte chosenAlt = (byte) getChoice(tour, trip);
            trip.setMicromobilityWalkMode(chosenAlt);
            	
            // write choice model alternative info to log file
	        if (tour.getDebugChoiceModels())
	        {
        		String decisionMaker = String.format("Tour %d", tour.getID()+ " mode " +trip.getTripMode());
	            mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
	            logger.info(String.format("%s result chosen for %s is %d",
	            	"Transponder Choice", decisionMaker, chosenAlt));
	            mmModel.logUECResults(logger, decisionMaker);
	        }

            	
        }else if(modelStructure.getTourModeIsWalkTransit(trip.getTripMode())) {
        		   
        	//access
        	int tapPosition =  mgraDataManager.getTapPosition(originMaz, trip.getBoardTap());
        	float walkTime = mgraDataManager.getMgraToTapWalkTime(originMaz, tapPosition);
            mmDmuObject.setWalkTime(walkTime);
            
            //set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
            mmDmuObject.setDmuIndexValues(tour.getID(), originMaz, originMaz, originMaz);

        	// compute utilities and choose micromobility choice alternative.
            float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
            trip.setMicromobilityAccessLogsum(logsum);
               
            // if the choice model has at least one available alternative, make choice
	        byte chosenAlt = (byte) getChoice( tour, trip);
	        trip.setMicromobilityAccessMode(chosenAlt);

		    // write choice model alternative info to log file
		    if (tour.getDebugChoiceModels())
		    {
        		String decisionMaker = String.format("Tour %d", tour.getID()+ " mode " +trip.getTripMode());
		        mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
		        logger.info(String.format("%s result chosen for %s is %d",
		        	"Transponder Choice", decisionMaker, chosenAlt));
		        mmModel.logUECResults(logger, decisionMaker);
		    }
	        	//egress
	        	tapPosition =  mgraDataManager.getTapPosition(destMaz, trip.getAlightTap());
	        	walkTime = mgraDataManager.getMgraToTapWalkTime(destMaz, tapPosition);
	            mmDmuObject.setWalkTime(walkTime);

	            //set destination to closest mgra to alighting TAP so that Z can be used to find access to mode in mgra data file in UEC
	            int closestMazToAlightTap = mgraDataManager.getClosestMgra(trip.getAlightTap());
	            mmDmuObject.setDmuIndexValues(tour.getID(), closestMazToAlightTap, closestMazToAlightTap, closestMazToAlightTap);
	            
	        	// compute utilities and choose micromobility choice alternative.
	            logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
	            trip.setMicromobilityEgressLogsum(logsum);
	                
	            // if the choice model has at least one available alternative, make choice
		        chosenAlt = (byte) getChoice( tour, trip);
		        trip.setMicromobilityEgressMode(chosenAlt);

		        // write choice model alternative info to log file
		        if (tour.getDebugChoiceModels())
		        {
	        		String decisionMaker = String.format("Tour %d", tour.getID()+ " mode " +trip.getTripMode());
		        	mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
		            logger.info(String.format("%s result chosen for %s is %d",
		            		"Transponder Choice", decisionMaker, chosenAlt));
		            mmModel.logUECResults(logger, decisionMaker);
		        }
        	} else if( modelStructure.getTourModeIsDriveTransit(trip.getTripMode()) ) {     	   //drive-transit. Choose non-drive direction
        	     
        		int tapPosition = 0;
        		float walkTime = 9999;
        		
        		if(trip.isInbound()) { //inbound, so access mode is walk
        		   tapPosition =  mgraDataManager.getTapPosition(originMaz, trip.getBoardTap());
        		   walkTime = mgraDataManager.getMgraToTapWalkTime(originMaz, tapPosition);
                   //set destination to origin so that Z can be used to find origin zone access to mode in mgra data file in UEC
                   mmDmuObject.setDmuIndexValues(tour.getID(), originMaz, originMaz, originMaz);
        		   
        		}else { //outbound so egress mode is walk.
        		   tapPosition =  mgraDataManager.getTapPosition(destMaz, trip.getAlightTap());
        		   walkTime = mgraDataManager.getMgraToTapWalkTime(destMaz, tapPosition);
        		   //set destination to closest mgra to alighting TAP so that Z can be used to find access to mode in mgra data file in UEC
   	            	int closestMazToAlightTap = mgraDataManager.getClosestMgra(trip.getAlightTap());
   	            	mmDmuObject.setDmuIndexValues(tour.getID(), closestMazToAlightTap, closestMazToAlightTap, closestMazToAlightTap);
        		}
	            mmDmuObject.setWalkTime(walkTime);
	
        		// compute utilities and choose micromobility choice alternative.
               	float logsum = (float) mmModel.computeUtilities(mmDmuObject, mmDmuObject.getDmuIndexValues());
               		
             	// if the choice model has at least one available alternative, make choice
               	byte chosenAlt = (byte) getChoice(tour, trip);
            	
               	if(trip.isInbound()) { //inbound, set access
               		trip.setMicromobilityAccessMode(chosenAlt);
               		trip.setMicromobilityAccessLogsum(logsum);
               	}else { //outound, set egress
               		trip.setMicromobilityEgressMode(chosenAlt);
               		trip.setMicromobilityEgressLogsum(logsum);
               	}
	               	
            	// write choice model alternative info to log file
	             if (tour.getDebugChoiceModels())
	             {
	         		String decisionMaker = String.format("Tour %d", tour.getID()+ " mode " +trip.getTripMode());
	                 mmModel.logAlternativesInfo("Micromobility Choice", decisionMaker, logger);
	                 logger.info(String.format("%s result chosen for %s is %d",
	                         "Transponder Choice", decisionMaker, chosenAlt));
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
       private int getChoice(VisitorTour tour, VisitorTrip trip) {
        	// if the choice model has at least one available alternative, make
        	// choice.
        	int chosenAlt;
        	if (mmModel.getAvailabilityCount() > 0)
        	{
        		double randomNumber = tour.getRandom();
        		chosenAlt = mmModel.getChoiceResult(randomNumber);
        		return chosenAlt;
        	} else
        	{
        		String decisionMaker = String.format("Tour %d", tour.getID()+ " mode " +trip.getTripMode());
        		String errorMessage = String
                    .format("Exception caught for %s, no available micromobility choice alternatives to choose from in choiceModelApplication.",
                            decisionMaker);
        		logger.error(errorMessage);

        		mmModel.logUECResults(logger, decisionMaker);
        		throw new RuntimeException();
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
