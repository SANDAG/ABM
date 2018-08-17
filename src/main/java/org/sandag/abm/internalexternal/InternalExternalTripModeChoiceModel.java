package org.sandag.abm.internalexternal;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.airport.AirportModelStructure;
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

public class InternalExternalTripModeChoiceModel
{

    private transient Logger                   logger                     = Logger.getLogger("internalExternalModel");

    private AutoTazSkimsCalculator  tazDistanceCalculator;

    private McLogsumsCalculator                logsumHelper;
    private InternalExternalModelStructure     modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;
    private InternalExternalTripModeChoiceDMU  dmu;
    private ChoiceModelApplication             tripModeChoiceModel;
    double                                     logsum                     = 0;

    private static final String                PROPERTIES_UEC_DATA_SHEET  = "internalExternal.trip.mc.data.page";
    private static final String                PROPERTIES_UEC_MODEL_SHEET = "internalExternal.trip.mc.model.page";
    private static final String                PROPERTIES_UEC_FILE        = "internalExternal.trip.mc.uec.file";
    private TripModeChoiceDMU 		 mcDmuObject;

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public InternalExternalTripModeChoiceModel(HashMap<String, String> propertyMap,
            InternalExternalModelStructure myModelStructure,
            InternalExternalDmuFactoryIf dmuFactory)
    {
        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        modelStructure = myModelStructure;
        
        tazDistanceCalculator = new AutoTazSkimsCalculator(propertyMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        
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
            InternalExternalDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up IE trip mode choice model."));

        dmu = dmuFactory.getInternalExternalTripModeChoiceDMU();

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
    public double computeUtilities(InternalExternalTour tour, InternalExternalTrip trip)
    {

        setDmuAttributes(tour, trip);

        tripModeChoiceModel.computeUtilities(dmu, dmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            tour.logTourObject(logger, 100);
            tripModeChoiceModel.logUECResults(logger, "IE trip mode choice model");

        }

        logsum = tripModeChoiceModel.getLogsum();

        if (tour.getDebugChoiceModels()) logger.info("Returning logsum " + logsum);

        return logsum;

    }

    /**
     * Choose a mode and store in the trip object.
     * 
     * @param tour
     *            InternalExternalTour
     * @param trip
     *            InternalExternalTrip
     * 
     */
    public void chooseMode(InternalExternalTour tour, InternalExternalTrip trip)
    {

        computeUtilities(tour, trip);

        double rand = tour.getRandom();
        int mode = tripModeChoiceModel.getChoiceResult(rand);

        trip.setTripMode(mode);
        
        //value of time; lookup vot, votS2, or votS3 from the UEC depending on chosen mode
        UtilityExpressionCalculator uec = tripModeChoiceModel.getUEC();
        
        double vot = 0.0;
        
        if(modelStructure.getTripModeIsS2(mode)){
            int votIndex = uec.lookupVariableIndex("votS2");
            vot = uec.getValueForIndex(votIndex);
        }else if (modelStructure.getTripModeIsS3(mode)){
            int votIndex = uec.lookupVariableIndex("votS3");
            vot = uec.getValueForIndex(votIndex);
        }else{
            int votIndex = uec.lookupVariableIndex("vot");
            vot = uec.getValueForIndex(votIndex);
        }
        trip.setValueOfTime(vot);
        
        
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

        
        


    }

    /**
     * Set DMU attributes.
     * 
     * @param tour
     * @param trip
     */
    public void setDmuAttributes(InternalExternalTour tour, InternalExternalTrip trip)
    {

        int tripOriginTaz = trip.getOriginTaz();
        int tripDestinationTaz = trip.getDestinationTaz();

        dmu.setDmuIndexValues(tripOriginTaz, tripDestinationTaz, tripOriginTaz, tripDestinationTaz,
                tour.getDebugChoiceModels());

        dmu.setTourDepartPeriod(tour.getDepartTime());
        dmu.setTourArrivePeriod(tour.getArriveTime());
        dmu.setTripPeriod(trip.getPeriod());

        dmu.setAutos(tour.getAutos());
        dmu.setIncome(tour.getIncome());
        dmu.setAge(tour.getAge());
        dmu.setFemale(tour.getFemale());
        
        dmu.setNonWorkTimeFactor(tour.getNonWorkTimeFactor());
        
        // set trip mc dmu values for transit logsum (gets replaced below by uec values)
        double c_ivt = -0.03;
        double c_cost = - 0.003; 

        // Solve trip mode level utilities
        mcDmuObject.setIvtCoeff(c_ivt * tour.getNonWorkTimeFactor());
        mcDmuObject.setCostCoeff(c_cost);
        
        dmu.setIvtCoeff(c_ivt * tour.getNonWorkTimeFactor());
        dmu.setCostCoeff(c_cost);
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

        dmu.setOutboundStops(tour.getNumberInboundStops());
        dmu.setReturnStops(tour.getNumberInboundStops());

        if (trip.isFirstTrip()) dmu.setFirstTrip(1);
        else dmu.setFirstTrip(0);

        if (trip.isLastTrip()) dmu.setLastTrip(1);
        else dmu.setLastTrip(0);

        if (trip.isOriginIsTourDestination()) dmu.setTripOrigIsTourDest(1);
        else dmu.setTripOrigIsTourDest(0);

        if (trip.isDestinationIsTourDestination()) dmu.setTripDestIsTourDest(1);
        else dmu.setTripDestIsTourDest(0);

    }

}
