package org.sandag.abm.crossborder;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class CrossBorderTripModeChoiceModel
{

    private transient Logger                   logger                     = Logger.getLogger("crossBorderModel");

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private CrossBorderModelStructure          modelStructure;
    private SandagModelStructure               sandagModelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;
    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;
    private CrossBorderTripModeChoiceDMU       dmu;
    private ChoiceModelApplication             tripModeChoiceModel;
    double                                     logsum                     = 0;
    
    private TripModeChoiceDMU 		 mcDmuObject;
    private AutoTazSkimsCalculator   tazDistanceCalculator;


    private static final String                PROPERTIES_UEC_DATA_SHEET  = "crossBorder.trip.mc.data.page";
    private static final String                PROPERTIES_UEC_MODEL_SHEET = "crossBorder.trip.mc.model.page";
    private static final String                PROPERTIES_UEC_FILE        = "crossBorder.trip.mc.uec.file";

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public CrossBorderTripModeChoiceModel(HashMap<String, String> propertyMap,
            CrossBorderModelStructure myModelStructure, CrossBorderDmuFactoryIf dmuFactory, AutoTazSkimsCalculator tazDistanceCalculator)
    {
        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();

        modelStructure = myModelStructure;
        sandagModelStructure = new SandagModelStructure();
        
        this.tazDistanceCalculator = tazDistanceCalculator;
        
        setupTripModeChoiceModel(propertyMap, dmuFactory);

    }

    /**
     * Read the UEC file and set up the trip mode choice model.
     * 
     * @param propertyMap
     * @param dmuFactory
     */
    private void setupTripModeChoiceModel(HashMap<String, String> propertyMap,
            CrossBorderDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up cross border trip mode choice model."));

        dmu = dmuFactory.getCrossBorderTripModeChoiceDMU();

        int dataPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_DATA_SHEET));
        int modelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_MODEL_SHEET));

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tripModeUecFile = propertyMap.get(PROPERTIES_UEC_FILE);
        tripModeUecFile = uecPath + tripModeUecFile;

        tripModeChoiceModel = new ChoiceModelApplication(tripModeUecFile, modelPage, dataPage,
                propertyMap, (VariableTable) dmu);
        
        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(propertyMap);
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());
        
        SandagModelStructure modelStructure = new SandagModelStructure();
        mcDmuObject = new TripModeChoiceDMU(modelStructure, logger);


    }

    /**
     * Calculate utilities and return logsum for the tour and stop.
     * 
     * @param tour
     * @param trip
     */
    public double computeUtilities(CrossBorderTour tour, CrossBorderTrip trip)
    {

        setDmuAttributes(tour, trip);
   
        tripModeChoiceModel.computeUtilities(dmu, dmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            tour.logTourObject(logger, 100);
            tripModeChoiceModel.logUECResults(logger, "Cross border trip mode choice model");

        }

        logsum = tripModeChoiceModel.getLogsum();

        if (tour.getDebugChoiceModels()) logger.info("Returning logsum " + logsum);

        return logsum;

    }

    /**
     * Choose a mode and store in the trip object.
     * 
     * @param tour
     *            CrossBorderTour
     * @param trip
     *            CrossBorderTrip
     * 
     */
    public void chooseMode(CrossBorderTour tour, CrossBorderTrip trip)
    {
        computeUtilities(tour, trip);

        double rand = tour.getRandom();
        int mode=0;
        try{
            mode = tripModeChoiceModel.getChoiceResult(rand); 
        	trip.setTripMode(mode);
        	
        	if(sandagModelStructure.getTripModeIsTransit(mode)){ 
            	double[][] bestTapPairs = logsumHelper.getBestWtwTripTaps();
              	//pick transit path from N-paths
                float rn = new Double(tour.getRandom()).floatValue();
            	int pathIndex = logsumHelper.chooseTripPath(rn, bestTapPairs, tour.getDebugChoiceModels(), logger);
            	int boardTap = (int) bestTapPairs[pathIndex][0];
            	int alightTap = (int) bestTapPairs[pathIndex][1];
            	int set = (int) bestTapPairs[pathIndex][2];
            	trip.setBoardTap(boardTap);
            	trip.setAlightTap(alightTap);
            	trip.setSet(set);
      
            	 
        	}
        }catch(Exception e){
        	logger.info("rand="+rand);
        	tour.logTourObject(logger, 100);
        	logger.error(e.getMessage());
        }
        
    }
    
    /**
     * This method looks up the value of time from the last call to the UEC and returns
     * it based on the occupancy of the mode passed in as an argument. this method ensures
     * that the value of time at a tour level is the same for all trips on the tour (even
     * though the actual trip level VOT might vary based on the trip occupancy).
     * 
     * @param tourMode
     * @return The value of time
     */
    public float getValueOfTime(int tourMode){
       
    	//value of time; lookup vot, votS2, or votS3 from the UEC depending on chosen mode
        UtilityExpressionCalculator uec = tripModeChoiceModel.getUEC();
        
        double vot = 0.0;
        
        if(tourMode== modelStructure.SHARED2){
            int votIndex = uec.lookupVariableIndex("votS2");
            vot = uec.getValueForIndex(votIndex);
        }else if (tourMode== modelStructure.SHARED3){
            int votIndex = uec.lookupVariableIndex("votS3");
            vot = uec.getValueForIndex(votIndex);
        }else{
            int votIndex = uec.lookupVariableIndex("vot");
            vot = uec.getValueForIndex(votIndex);
        }
        return (float) vot;

    	
    }

    /**
     * Set DMU attributes.
     * 
     * @param tour
     * @param trip
     */
    public void setDmuAttributes(CrossBorderTour tour, CrossBorderTrip trip)
    {

        int tourDestinationMgra = tour.getDestinationMGRA();
        int tripOriginMgra = trip.getOriginMgra();
        int tripDestinationMgra = trip.getDestinationMgra();

        int tripOriginTaz = trip.getOriginTAZ();
        int tripDestinationTaz = trip.getDestinationTAZ();

        dmu.setDmuIndexValues(tripOriginTaz, tripDestinationTaz, tripOriginTaz, tripDestinationTaz,
                tour.getDebugChoiceModels());

        dmu.setTourDepartPeriod(tour.getDepartTime());
        dmu.setTourArrivePeriod(tour.getArriveTime());
        dmu.setTripPeriod(trip.getPeriod());
        
        dmu.setWorkTimeFactor((float)tour.getWorkTimeFactor());
        dmu.setNonWorkTimeFactor((float)tour.getNonWorkTimeFactor());

        // set trip mc dmu values for transit logsum (gets replaced below by uec values)
        double c_ivt = -0.03;
        double c_cost = - 0.0003; 

        // Solve trip mode level utilities
        mcDmuObject.setIvtCoeff(c_ivt);
        mcDmuObject.setCostCoeff(c_cost);
        double walkTransitLogsum = -999.0;
   
        logsumHelper.setNmTripMcDmuAttributes(mcDmuObject, trip.getOriginMgra(), trip.getDestinationMgra(), trip.getPeriod(), tour.getDebugChoiceModels());
        dmu.setNonMotorizedWalkTime(mcDmuObject.getNm_walkTime());
        dmu.setNonMotorizedBikeTime(mcDmuObject.getNm_bikeTime());

        logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, tripOriginMgra, tripDestinationMgra, trip.getPeriod(), tour.getDebugChoiceModels());
        walkTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.WTW);

    	dmu.setWalkTransitLogsum(walkTransitLogsum);

        if (tour.getPurpose() == modelStructure.WORK) dmu.setWorkTour(1);
        else dmu.setWorkTour(0);

        dmu.setOutboundStops(tour.getNumberInboundStops());
        dmu.setReturnStops(tour.getNumberInboundStops());

        if (trip.isFirstTrip()) dmu.setFirstTrip(1);
        else dmu.setFirstTrip(0);

        if (trip.isLastTrip()) dmu.setLastTrip(1);
        else dmu.setLastTrip(0);

        if (tour.getTourMode() == modelStructure.DRIVEALONE) dmu.setTourModeIsDA(1);
        else dmu.setTourModeIsDA(0);

        if (tour.getTourMode() == modelStructure.SHARED2) dmu.setTourModeIsS2(1);
        else dmu.setTourModeIsS2(0);

        if (tour.getTourMode() == modelStructure.SHARED3) dmu.setTourModeIsS3(1);
        else dmu.setTourModeIsS3(0);

        if (tour.getTourMode() == modelStructure.WALK) dmu.setTourModeIsWalk(1);
        else dmu.setTourModeIsWalk(0);

        if (tour.isSentriAvailable()) dmu.setTourCrossingIsSentri(1);
        else dmu.setTourCrossingIsSentri(0);

        if (trip.isOriginIsTourDestination()) dmu.setTripOrigIsTourDest(1);
        else dmu.setTripOrigIsTourDest(0);

        if (trip.isDestinationIsTourDestination()) dmu.setTripDestIsTourDest(1);
        else dmu.setTripDestIsTourDest(0);

        dmu.setHourlyParkingCostTourDest((float) lsWgtAvgCostH[tourDestinationMgra]);
        dmu.setDailyParkingCostTourDest((float) lsWgtAvgCostD[tourDestinationMgra]);
        dmu.setMonthlyParkingCostTourDest((float) lsWgtAvgCostM[tourDestinationMgra]);
        dmu.setHourlyParkingCostTripOrig((float) lsWgtAvgCostH[tripOriginMgra]);
        dmu.setHourlyParkingCostTripDest((float) lsWgtAvgCostH[tripDestinationMgra]);

    }

    public McLogsumsCalculator getMcLogsumsCalculator(){
    	return logsumHelper;
    }
}
