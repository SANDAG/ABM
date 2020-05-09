package org.sandag.abm.visitor;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class VisitorTripModeChoiceModel
{

    private transient Logger                   logger                     = Logger.getLogger("visitorModel");

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private VisitorModelStructure              modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;
    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;
    private VisitorTripModeChoiceDMU           dmu;
    private ChoiceModelApplication             tripModeChoiceModel;
    double                                     logsum                     = 0;

    private static final String                PROPERTIES_UEC_DATA_SHEET  = "visitor.trip.mc.data.page";
    private static final String                PROPERTIES_UEC_MODEL_SHEET = "visitor.trip.mc.model.page";
    private static final String                PROPERTIES_UEC_FILE        = "visitor.trip.mc.uec.file";
    private TripModeChoiceDMU 		 mcDmuObject;
    private AutoTazSkimsCalculator   tazDistanceCalculator;

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public VisitorTripModeChoiceModel(HashMap<String, String> propertyMap,
            VisitorModelStructure myModelStructure, VisitorDmuFactoryIf dmuFactory, AutoTazSkimsCalculator tazDistanceCalculator)
    {
        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();

        modelStructure = myModelStructure;
        this.tazDistanceCalculator = tazDistanceCalculator;
        
        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(propertyMap);
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        SandagModelStructure modelStructure = new SandagModelStructure();
        mcDmuObject = new TripModeChoiceDMU(modelStructure, logger);

        setupTripModeChoiceModel(propertyMap, dmuFactory);

    }

    /**
     * Read the UEC file and set up the trip mode choice model.
     * 
     * @param propertyMap
     * @param dmuFactory
     */
    private void setupTripModeChoiceModel(HashMap<String, String> propertyMap,
            VisitorDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up visitor trip mode choice model."));

        dmu = dmuFactory.getVisitorTripModeChoiceDMU();

        int dataPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_DATA_SHEET));
        int modelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_MODEL_SHEET));

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tripModeUecFile = propertyMap.get(PROPERTIES_UEC_FILE);
        tripModeUecFile = uecPath + tripModeUecFile;

        tripModeChoiceModel = new ChoiceModelApplication(tripModeUecFile, modelPage, dataPage,
                propertyMap, (VariableTable) dmu);

    }

    /**
     * Calculate utilities and return logsum for the tour and stop.
     * 
     * @param tour
     * @param trip
     */
    public double computeUtilities(VisitorTour tour, VisitorTrip trip)
    {

        setDmuAttributes(tour, trip);

        tripModeChoiceModel.computeUtilities(dmu, dmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            tour.logTourObject(logger, 100);
            tripModeChoiceModel.logUECResults(logger, "Visitor trip mode choice model");

        }

        logsum = tripModeChoiceModel.getLogsum();

        if (tour.getDebugChoiceModels()) logger.info("Returning logsum " + logsum);

        return logsum;

    }

    /**
     * Choose a mode and store in the trip object.
     * 
     * @param tour
     *            VisitorTour
     * @param trip
     *            VisitorTrip
     * 
     */
    public void chooseMode(VisitorTour tour, VisitorTrip trip)
    {

        computeUtilities(tour, trip);
        
        double rand = tour.getRandom();
        try{
        	int mode = tripModeChoiceModel.getChoiceResult(rand); 
        	trip.setTripMode(mode);
        	
        	//value of time; lookup vot, votS2, or votS3 from the UEC depending on chosen mode
            UtilityExpressionCalculator uec = tripModeChoiceModel.getUEC();
            
            int votIndex = uec.lookupVariableIndex("vot");
            double vot = uec.getValueForIndex(votIndex);
            trip.setValueOfTime((float)vot);
           	
            float parkingCost = getTripParkingCost(mode);
        	trip.setParkingCost(parkingCost);
 
            if(modelStructure.getTripModeIsTransit(mode)){
            	double[][] bestTapPairs = null;
            
            	if (modelStructure.getTripModeIsWalkTransit(mode)){
            		bestTapPairs = logsumHelper.getBestWtwTripTaps();
            	}
            	else if (modelStructure.getTripModeIsPnrTransit(mode)||modelStructure.getTripModeIsKnrTransit(mode)){
            		if (!trip.isInbound())
            			bestTapPairs = logsumHelper.getBestDtwTripTaps();
            		else
            			bestTapPairs = logsumHelper.getBestWtdTripTaps();
            	}
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
        	logger.info("Error calculating visitor trip mode choice with rand="+rand);
        	tour.logTourObject(logger, 100);
        	logger.error(e.getMessage());
        }

    }

    /**
     * Return parking cost from UEC if auto trip, else return 0.
     * 
     * @param tripMode
     * @return Parking cost if auto mode, else 0
     */
    public float getTripParkingCost(int tripMode) {
    	
    	float parkingCost=0;
    	
    	if(modelStructure.getTripModeIsSovOrHov(tripMode)) {
     		UtilityExpressionCalculator uec = tripModeChoiceModel.getUEC();
    		int parkingCostIndex = uec.lookupVariableIndex("parkingCost");
    		parkingCost = (float) uec.getValueForIndex(parkingCostIndex);
    		return parkingCost;
    	}
    	return parkingCost;
    }
    

    /**
     * Set DMU attributes.
     * 
     * @param tour
     * @param trip
     */
    public void setDmuAttributes(VisitorTour tour, VisitorTrip trip)
    {

        int tourDestinationMgra = tour.getDestinationMGRA();
        int tripOriginMgra = trip.getOriginMgra();
        int tripDestinationMgra = trip.getDestinationMgra();

        int tripOriginTaz = mgraManager.getTaz(tripOriginMgra);
        int tripDestinationTaz = mgraManager.getTaz(tripDestinationMgra);

        int tourMode = tour.getTourMode();

        dmu.setDmuIndexValues(tripOriginTaz, tripDestinationTaz, tripOriginTaz, tripDestinationTaz,
                tour.getDebugChoiceModels());

        dmu.setTourDepartPeriod(tour.getDepartTime());
        dmu.setTourArrivePeriod(tour.getArriveTime());
        dmu.setTripPeriod(trip.getPeriod());

        // set trip mc dmu values for transit logsum (gets replaced below by uec values)
        double c_ivt = -0.03;
        double c_cost = - 0.0033; 

        // Solve trip mode level utilities
        mcDmuObject.setIvtCoeff(c_ivt);
        mcDmuObject.setCostCoeff(c_cost);
        double walkTransitLogsum = -999.0;
        double driveTransitLogsum = -999.0;
   
        logsumHelper.setNmTripMcDmuAttributes(mcDmuObject, trip.getOriginMgra(), trip.getDestinationMgra(), trip.getPeriod(), tour.getDebugChoiceModels());
        dmu.setNonMotorizedWalkTime(mcDmuObject.getNm_walkTime());
        dmu.setNonMotorizedBikeTime(mcDmuObject.getNm_bikeTime());
        
        logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, trip.getOriginMgra(), trip.getDestinationMgra(), trip.getPeriod(),tour.getDebugChoiceModels());
        walkTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.WTW);

    	dmu.setWalkTransitLogsum(walkTransitLogsum);
        if (!trip.isInbound())
        {
            logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, trip.getOriginMgra(), trip.getDestinationMgra(), trip.getPeriod(), tour.getDebugChoiceModels());
            driveTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.DTW);
        } else
        {
        	logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, trip.getOriginMgra(), trip.getDestinationMgra(), trip.getPeriod(), tour.getDebugChoiceModels());
            driveTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.WTD);
        }

        dmu.setPnrTransitLogsum(driveTransitLogsum);
        dmu.setKnrTransitLogsum(driveTransitLogsum);
        
        dmu.setTourPurpose(tour.getPurpose());

        dmu.setOutboundStops(tour.getNumberInboundStops());
        dmu.setReturnStops(tour.getNumberInboundStops());

        if (trip.isFirstTrip()) dmu.setFirstTrip(1);
        else dmu.setFirstTrip(0);

        if (trip.isLastTrip()) dmu.setLastTrip(1);
        else dmu.setLastTrip(0);

        if (modelStructure.getTourModeIsSov(tourMode)) dmu.setTourModeIsDA(1);
        else dmu.setTourModeIsDA(0);

        if (modelStructure.getTourModeIsS2(tourMode)) dmu.setTourModeIsS2(1);
        else dmu.setTourModeIsS2(0);

        if (modelStructure.getTourModeIsS3(tourMode)) dmu.setTourModeIsS3(1);
        else dmu.setTourModeIsS3(0);

        if (modelStructure.getTourModeIsWalk(tourMode)) dmu.setTourModeIsWalk(1);
        else dmu.setTourModeIsWalk(0);

        if (modelStructure.getTourModeIsBike(tourMode)) dmu.setTourModeIsBike(1);
        else dmu.setTourModeIsBike(0);

        if (modelStructure.getTourModeIsWalkTransit(tourMode)) dmu.setTourModeIsWalkTransit(1);
        else dmu.setTourModeIsWalkTransit(0);

        if (modelStructure.getTourModeIsPnr(tourMode)) dmu.setTourModeIsPNRTransit(1);
        else dmu.setTourModeIsPNRTransit(0);

        if (modelStructure.getTourModeIsKnr(tourMode)) dmu.setTourModeIsKNRTransit(1);
        else dmu.setTourModeIsKNRTransit(0);

        if (modelStructure.getTourModeIsMaas(tourMode)) dmu.setTourModeIsMaas(1);
        else dmu.setTourModeIsMaas(0);

        if (modelStructure.getTourModeIsTncTransit(tourMode)) dmu.setTourModeIsTNCTransit(1);
        else dmu.setTourModeIsTNCTransit(0);

        if (trip.isOriginIsTourDestination()) dmu.setTripOrigIsTourDest(1);
        else dmu.setTripOrigIsTourDest(0);

        if (trip.isDestinationIsTourDestination()) dmu.setTripDestIsTourDest(1);
        else dmu.setTripDestIsTourDest(0);

        dmu.setIncome((byte) tour.getIncome());
        dmu.setAutoAvailable(tour.getAutoAvailable());
        dmu.setPartySize(tour.getNumberOfParticipants());

        dmu.setHourlyParkingCostTourDest((float) lsWgtAvgCostH[tourDestinationMgra]);
        dmu.setDailyParkingCostTourDest((float) lsWgtAvgCostD[tourDestinationMgra]);
        dmu.setMonthlyParkingCostTourDest((float) lsWgtAvgCostM[tourDestinationMgra]);
        dmu.setHourlyParkingCostTripOrig((float) lsWgtAvgCostH[tripOriginMgra]);
        dmu.setHourlyParkingCostTripDest((float) lsWgtAvgCostH[tripDestinationMgra]);

    }

}
