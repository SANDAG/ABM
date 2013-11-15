package org.sandag.abm.specialevent;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;

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

        tripModeChoiceModel = new ChoiceModelApplication(tripModeUecFile, modelPage, dataPage,
                propertyMap, (VariableTable) dmu);

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

        // set the dmu skim attributes (which involves setting the best wtw
        // taps, since the tour taps are null
        logsumHelper.setTripMcDmuSkimAttributes(tour, trip, dmu);

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
