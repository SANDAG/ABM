package org.sandag.abm.internalexternal;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;


public class InternalExternalTripModeChoiceModel {

    private transient Logger logger = Logger.getLogger("internalExternalModel");

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                 logsumHelper;
    private InternalExternalModelStructure      modelStructure;
    private TazDataManager                      tazs;
    private MgraDataManager                     mgraManager;
     private InternalExternalTripModeChoiceDMU dmu;
    private ChoiceModelApplication tripModeChoiceModel;
    double logsum = 0;
    
    private static final String PROPERTIES_UEC_DATA_SHEET   = "internalExternal.trip.mc.data.page";
    private static final String PROPERTIES_UEC_MODEL_SHEET  = "internalExternal.trip.mc.model.page";
    private static final String PROPERTIES_UEC_FILE         = "internalExternal.trip.mc.uec.file";
    
    /**
	 * Constructor.
	 * 
	 * @param propertyMap
	 * @param myModelStructure
	 * @param dmuFactory
	 * @param myLogsumHelper
	 */
	public InternalExternalTripModeChoiceModel(HashMap<String, String> propertyMap, InternalExternalModelStructure myModelStructure, InternalExternalDmuFactoryIf dmuFactory, McLogsumsCalculator myLogsumHelper ){
		tazs = TazDataManager.getInstance(propertyMap);
	    mgraManager = MgraDataManager.getInstance(propertyMap);

	    modelStructure = myModelStructure;
	    logsumHelper = myLogsumHelper;
	    
        setupTripModeChoiceModel( propertyMap, dmuFactory );

	}

	/**
	 * Read the UEC file and set up the trip mode choice model.
	 * 
	 * @param propertyMap
	 * @param dmuFactory
	 */
    private void setupTripModeChoiceModel( HashMap<String, String> propertyMap, InternalExternalDmuFactoryIf dmuFactory  ) {
        
        logger.info( String.format( "setting up IE trip mode choice model." ) );
                
        dmu = dmuFactory.getInternalExternalTripModeChoiceDMU();

        int dataPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,PROPERTIES_UEC_DATA_SHEET));
        int modelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,PROPERTIES_UEC_MODEL_SHEET));
        
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tripModeUecFile = propertyMap.get(PROPERTIES_UEC_FILE);
        tripModeUecFile = uecPath + tripModeUecFile;
        
       tripModeChoiceModel = new ChoiceModelApplication(tripModeUecFile, modelPage, dataPage, propertyMap, (VariableTable) dmu);

    }
    
    
    /**
     * Calculate utilities and return logsum for the tour and stop.
     * 
     * @param tour
     * @param trip
     */
    public double computeUtilities(InternalExternalTour tour, InternalExternalTrip trip){
    	
    	setDmuAttributes(tour, trip);

    	tripModeChoiceModel.computeUtilities(dmu, dmu.getDmuIndexValues());
    	
    	if(tour.getDebugChoiceModels()){
    		tour.logTourObject(logger, 100);
    		tripModeChoiceModel.logUECResults(logger, "IE trip mode choice model");
        	
    	}
    	
    	logsum = tripModeChoiceModel.getLogsum();

    	if(tour.getDebugChoiceModels())
    		logger.info("Returning logsum "+logsum);

    	return logsum;
    	
    }
    
    /**
     * Choose a mode and store in the trip object.
     * 
     * @param tour InternalExternalTour 
     * @param trip InternalExternalTrip 
     * 
      */
    public void chooseMode(InternalExternalTour tour, InternalExternalTrip trip){
    	
    	computeUtilities(tour,trip);
    	
    	double rand = tour.getRandom();
    	int mode = tripModeChoiceModel.getChoiceResult(rand);

    	trip.setTripMode(mode);
    
    }
   
	
    
    /**
     * Set DMU attributes.
     * 
     * @param tour
     * @param trip
     */
    public void setDmuAttributes(InternalExternalTour tour, InternalExternalTrip trip){

       
    	int tripOriginTaz = trip.getOriginTaz();
    	int tripDestinationTaz =  trip.getDestinationTaz();
    	
    	dmu.setDmuIndexValues(tripOriginTaz, tripDestinationTaz, tripOriginTaz, tripDestinationTaz, 
    	            tour.getDebugChoiceModels());
    	
    	dmu.setTourDepartPeriod(tour.getDepartTime());
    	dmu.setTourArrivePeriod(tour.getArriveTime());
    	dmu.setTripPeriod(trip.getPeriod());
    	
    	dmu.setAutos(tour.getAutos());
    	dmu.setIncome(tour.getIncome());
    	dmu.setAge(tour.getAge());
    	dmu.setFemale(tour.getFemale());
    	
		//set the dmu skim attributes (which involves setting the best wtw taps, since the tour taps are null
    	logsumHelper.setTripMcDmuSkimAttributes(tour, trip, dmu);
    	
   		dmu.setOutboundStops(tour.getNumberInboundStops());
    	dmu.setReturnStops(tour.getNumberInboundStops());
    	
    	if(trip.isFirstTrip())
    		dmu.setFirstTrip(1);
    	else
       		dmu.setFirstTrip(0);
    	     		
    	if(trip.isLastTrip())
    		dmu.setLastTrip(1);
    	else
    		dmu.setLastTrip(0);
    	
         	if(trip.isOriginIsTourDestination())
    		dmu.setTripOrigIsTourDest(1);
    	else
    		dmu.setTripOrigIsTourDest(0);
    	    		
    	if(trip.isDestinationIsTourDestination())
    		dmu.setTripDestIsTourDest(1);
    	else
    		dmu.setTripDestIsTourDest(0);
   		
  
    }

}
