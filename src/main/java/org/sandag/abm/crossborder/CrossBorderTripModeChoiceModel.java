package org.sandag.abm.crossborder;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class CrossBorderTripModeChoiceModel
{

    private transient Logger                   logger                     = Logger.getLogger("crossBorderModel");

    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private CrossBorderModelStructure          modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;
    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;
    private CrossBorderTripModeChoiceDMU       dmu;
    private ChoiceModelApplication             tripModeChoiceModel;
    double                                     logsum                     = 0;

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
            CrossBorderModelStructure myModelStructure, CrossBorderDmuFactoryIf dmuFactory,
            McLogsumsCalculator myLogsumHelper)
    {
        tazs = TazDataManager.getInstance(propertyMap);
        mgraManager = MgraDataManager.getInstance(propertyMap);

        lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();

        modelStructure = myModelStructure;
        logsumHelper = myLogsumHelper;

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
        int mode = tripModeChoiceModel.getChoiceResult(rand);

        trip.setTripMode(mode);

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

        // set the dmu skim attributes (which involves setting the best wtw taps, since the tour taps are null
        logsumHelper.setTripMcDmuSkimAttributes(tour, trip, dmu);

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

}
