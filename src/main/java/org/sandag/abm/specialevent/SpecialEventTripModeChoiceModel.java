package org.sandag.abm.specialevent;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class SpecialEventTripModeChoiceModel
{

    private transient Logger                   logger                     = Logger.getLogger("specialEventModel");

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private ModelStructure                     modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;

    private SpecialEventTripModeChoiceDMU      dmu;
    private ChoiceModelApplication             tripModeChoiceModel;
    private boolean                            saveUtilsAndProbs;
    double                                     logsum                     = 0;
    TableDataSet                               eventData;
    private TripModeChoiceDMU 		 mcDmuObject;

    private static final String                PROPERTIES_UEC_DATA_SHEET  = "specialEvent.trip.mc.data.page";
    private static final String                PROPERTIES_UEC_MODEL_SHEET = "specialEvent.trip.mc.model.page";
    private static final String                PROPERTIES_UEC_FILE        = "specialEvent.trip.mc.uec.file";

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public SpecialEventTripModeChoiceModel(HashMap<String, String> propertyMap,
            ModelStructure myModelStructure, SpecialEventDmuFactoryIf dmuFactory,
            McLogsumsCalculator myLogsumHelper, TableDataSet eventData)
    {
        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        modelStructure = myModelStructure;
        logsumHelper = myLogsumHelper;
        this.eventData = eventData;
        
        SandagModelStructure modelStructure = new SandagModelStructure();
        mcDmuObject = new TripModeChoiceDMU(modelStructure, logger);
        
        setupTripModeChoiceModel(propertyMap, dmuFactory);
        saveUtilsAndProbs = Util.getBooleanValueFromPropertyMap(propertyMap,
                "specialEvent.saveUtilsAndProbs");

    }

    /**
     * Read the UEC file and set up the trip mode choice model.
     * 
     * @param propertyMap
     * @param dmuFactory
     */
    private void setupTripModeChoiceModel(HashMap<String, String> propertyMap,
            SpecialEventDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up Special Event trip mode choice model."));

        dmu = dmuFactory.getSpecialEventTripModeChoiceDMU();
        
        int dataPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_DATA_SHEET));
        int modelPage = new Integer(Util.getStringValueFromPropertyMap(propertyMap,
                PROPERTIES_UEC_MODEL_SHEET));

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String tripModeUecFile = propertyMap.get(PROPERTIES_UEC_FILE);
        tripModeUecFile = uecPath + tripModeUecFile;
        logger.info(tripModeUecFile);

        tripModeChoiceModel = new ChoiceModelApplication(tripModeUecFile, modelPage, dataPage,
                propertyMap, (VariableTable) dmu);
        logger.info(String.format("Finished setting up Special Event trip mode choice model."));
    }

    /**
     * Calculate utilities and return logsum for the tour and stop.
     * 
     * @param tour
     * @param trip
     */
    public double computeUtilities(SpecialEventTour tour, SpecialEventTrip trip)
    {

        setDmuAttributes(tour, trip);

        tripModeChoiceModel.computeUtilities(dmu, dmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            tour.logTourObject(logger, 100);
            tripModeChoiceModel.logUECResults(logger, "Special Event trip mode choice model");

        }

        logsum = tripModeChoiceModel.getLogsum();

        if (tour.getDebugChoiceModels()) logger.info("Returning logsum " + logsum);

        return logsum;

    }

    /**
     * Choose a mode and store in the trip object.
     * 
     * @param tour
     *            SpecialEventTour
     * @param trip
     *            SpecialEventTrip
     * 
     */
    public void chooseMode(SpecialEventTour tour, SpecialEventTrip trip)
    {

        computeUtilities(tour, trip);

        double rand = tour.getRandom();
        int mode = tripModeChoiceModel.getChoiceResult(rand);

        trip.setTripMode(mode);
        
        //value of time; lookup vot, votS2, or votS3 from the UEC depending on chosen mode
        UtilityExpressionCalculator uec = tripModeChoiceModel.getUEC();
        
        float vot = 0.0f;
        
        if(modelStructure.getTourModeIsS2(mode)){
            int votIndex = uec.lookupVariableIndex("votS2");
            vot = (float) uec.getValueForIndex(votIndex);
        }else if (modelStructure.getTourModeIsS3(mode)){
            int votIndex = uec.lookupVariableIndex("votS3");
            vot = (float) uec.getValueForIndex(votIndex);
        }else{
            int votIndex = uec.lookupVariableIndex("vot");
            vot = (float) uec.getValueForIndex(votIndex);
        }
        tour.setValueOfTime(vot);

        if(mode>=9){
        	double[][] bestTapPairs = null;
        
        	if (mode == 9){
        		bestTapPairs = logsumHelper.getBestWtwTripTaps();
        	}
        	else if (mode==10||mode==11){
        		if (!trip.isInbound())
        			bestTapPairs = logsumHelper.getBestDtwTripTaps();
        		else
        			bestTapPairs = logsumHelper.getBestWtdTripTaps();
        	}
        	double rn = tour.getRandom();
        	int pathIndex = logsumHelper.chooseTripPath(rn, bestTapPairs, tour.getDebugChoiceModels(), logger);
        	int boardTap = (int) bestTapPairs[pathIndex][0];
        	int alightTap = (int) bestTapPairs[pathIndex][1];
        	int set = (int) bestTapPairs[pathIndex][2];
        	trip.setBoardTap(boardTap);
        	trip.setAlightTap(alightTap);
        	trip.setSet(set);
        }


        if (tour.getDebugChoiceModels())
        {
            logger.info("Chose mode " + mode + " with random number " + rand);
        }

        if (saveUtilsAndProbs)
        {
            double[] probs = tripModeChoiceModel.getProbabilities();
            float[] localProbs = new float[probs.length];
            for (int i = 0; i < probs.length; ++i)
                localProbs[i] = (float) probs[i];

            double[] utils = tripModeChoiceModel.getUtilities();
            float[] localUtils = new float[utils.length];
            for (int i = 0; i < utils.length; ++i)
                localUtils[i] = (float) utils[i];

            trip.setModeUtilities(localUtils);
            trip.setModeProbabilities(localProbs);
        }

    }

    /**
     * Set DMU attributes.
     * 
     * @param tour
     * @param trip
     */
    public void setDmuAttributes(SpecialEventTour tour, SpecialEventTrip trip)
    {

        int tourDestinationMgra = tour.getDestinationMGRA();
        int tripOriginMgra = trip.getOriginMgra();
        int tripDestinationMgra = trip.getDestinationMgra();

        int tripOriginTaz = mgraManager.getTaz(tripOriginMgra);
        int tripDestinationTaz = mgraManager.getTaz(tripDestinationMgra);

        dmu.setDmuIndexValues(tripOriginTaz, tripDestinationTaz, tripOriginTaz, tripDestinationTaz,
                tour.getDebugChoiceModels());

        dmu.setTourDepartPeriod(tour.getDepartTime());
        dmu.setTourArrivePeriod(tour.getArriveTime());
        dmu.setTripPeriod(trip.getPeriod());
        dmu.setIncome(tour.getIncome());
        dmu.setPartySize(tour.getPartySize());
        if (trip.isInbound()) dmu.setOutboundHalfTourDirection(0);
        else dmu.setOutboundHalfTourDirection(1);

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

        int eventNumber = tour.getEventNumber();

        float parkingCost = eventData.getValueAt(eventNumber, "ParkingCost");
        float parkingTime = eventData.getValueAt(eventNumber, "ParkingTime");

        dmu.setParkingCost(parkingCost);
        dmu.setParkingTime(parkingTime);

        if (trip.isOriginIsTourDestination()) dmu.setTripOrigIsTourDest(1);
        else dmu.setTripOrigIsTourDest(0);

        if (trip.isDestinationIsTourDestination()) dmu.setTripDestIsTourDest(1);
        else dmu.setTripDestIsTourDest(0);

    }

}
