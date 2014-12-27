package org.sandag.abm.ctramp;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class TripModeChoiceModel {
    private transient Logger                   tmcLogger                                           = Logger.getLogger("tripMcLog");

    public static final String                 PROPERTIES_UEC_TRIP_MODE_CHOICE                     = "tripModeChoice.uec.file";
    public static final int                    WORK_SHEET                                          = 1;
    public static final int                    UNIVERSITY_SHEET                                    = 2;
    public static final int                    SCHOOL_SHEET                                        = 3;
    public static final int                    MAINTENANCE_SHEET                                   = 4;
    public static final int                    DISCRETIONARY_SHEET                                 = 5;
    public static final int                    SUBTOUR_SHEET                                       = 6;
    public static final int[]                  MC_PURPOSE_SHEET_INDICES                            = {
            -1, WORK_SHEET, UNIVERSITY_SHEET, SCHOOL_SHEET, MAINTENANCE_SHEET, MAINTENANCE_SHEET,
            MAINTENANCE_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET,
            SUBTOUR_SHEET                                                                          };

    public static final int                    WORK_CATEGORY                                       = 0;
    public static final int                    UNIVERSITY_CATEGORY                                 = 1;
    public static final int                    SCHOOL_CATEGORY                                     = 2;
    public static final int                    MAINTENANCE_CATEGORY                                = 3;
    public static final int                    DISCRETIONARY_CATEGORY                              = 4;
    public static final int                    SUBTOUR_CATEGORY                                    = 5;
    public static final String[]               PURPOSE_CATEGORY_LABELS                             = {
            "work", "university", "school", "maintenance", "discretionary", "subtour"              };
    public static final int[]                  PURPOSE_CATEGORIES                                  = {
            -1, WORK_CATEGORY, UNIVERSITY_CATEGORY, SCHOOL_CATEGORY, MAINTENANCE_CATEGORY,
            MAINTENANCE_CATEGORY, MAINTENANCE_CATEGORY, DISCRETIONARY_CATEGORY,
            DISCRETIONARY_CATEGORY, DISCRETIONARY_CATEGORY, SUBTOUR_CATEGORY                       };

    
    private int                                availAltsToLog                                      = 55;    
    private AutoAndNonMotorizedSkimsCalculator anm;
    private McLogsumsCalculator                logsumHelper;
    private ModelStructure                     modelStructure;
    private TazDataManager                     tazs;
    private MgraDataManager                    mgraManager;
    private ChoiceModelApplication[]           mcModelArray;

    private TripModeChoiceDMU                  mcDmuObject;

    
    public TripModeChoiceModel(HashMap<String, String> propertyMap,
            ModelStructure myModelStructure, CtrampDmuFactoryIf dmuFactory,
            McLogsumsCalculator myLogsumHelper){
    
    	
        mgraManager = MgraDataManager.getInstance(propertyMap);
        modelStructure = myModelStructure;
        logsumHelper = myLogsumHelper;

        setup(propertyMap, dmuFactory);
 
    }
    
    
    
    /**
     * Set up the trip mode choice model.
     */
    private void setup(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory){
    	
        double[] lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        double[] lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        double[] lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();
        int[] mgraParkArea = mgraManager.getMgraParkAreas();

        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = propertyMap.get(PROPERTIES_UEC_TRIP_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        mcDmuObject = dmuFactory.getTripModeChoiceDMU();

        // logsumHelper.setupSkimCalculators(propertyMap);
        anm = logsumHelper.getAnmSkimCalculator();
        mcDmuObject.setParkingCostInfo(mgraParkArea, lsWgtAvgCostM, lsWgtAvgCostD, lsWgtAvgCostH);

        mcModelArray = new ChoiceModelApplication[5 + 1];
        mcModelArray[WORK_CATEGORY] = new ChoiceModelApplication(mcUecFile, WORK_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[UNIVERSITY_CATEGORY] = new ChoiceModelApplication(mcUecFile, UNIVERSITY_SHEET,
                0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[SCHOOL_CATEGORY] = new ChoiceModelApplication(mcUecFile, SCHOOL_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[MAINTENANCE_CATEGORY] = new ChoiceModelApplication(mcUecFile,
                MAINTENANCE_SHEET, 0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[DISCRETIONARY_CATEGORY] = new ChoiceModelApplication(mcUecFile,
                DISCRETIONARY_SHEET, 0, propertyMap, (VariableTable) mcDmuObject);
        mcModelArray[SUBTOUR_CATEGORY] = new ChoiceModelApplication(mcUecFile, SUBTOUR_SHEET, 0,
                propertyMap, (VariableTable) mcDmuObject);


    	
    }
    
    public void setDmuVariables(Stop s, int altMgra, boolean[] mgraInBoardingTapShed, boolean[] mgraInAlightingTapShed){
    	

        int category = PURPOSE_CATEGORIES[s.getTour().getTourPrimaryPurposeIndex()];
        ChoiceModelApplication mcModel = mcModelArray[category];

        // set the land use data items in the DMU for the stop origin
        mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(s.getOrig()));
        mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(s.getOrig()));
        mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(s.getOrig()));
        mcDmuObject.getDmuIndexValues().setDestZone(altMgra);

        // set the land use data items in the DMU for the sample location
        mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(altMgra));
        mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(altMgra));
        mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(altMgra));

        mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager
                .getTaz(altMgra)));

        mcDmuObject.setAutoModeRequiredForTripSegment(false);
        mcDmuObject.setWalkModeAllowedForTripSegment(false);

        mcDmuObject.setSegmentIsIk(true);

        double ikSegment = -999;
        // drive transit tours are handled differently than walk transit
        // tours
        if (modelStructure.getTourModeIsDriveTransit(s.getTour().getTourModeChoice()))
        {

            // if the direction is outbound
            if (!s.isInboundStop())
            {

                // if the sampled mgra is in the outbound half-tour boarding
                // tap shed (near tour origin)
                if (mgraInBoardingTapShed[altMgra])
                {
                    logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                    logsumHelper
                            .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                }

                // if the sampled mgra is in the outbound half-tour
                // alighting tap shed (near tour primary destination)
                if (mgraInAlightingTapShed[altMgra])
                {
                    logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                    logsumHelper.setDtwTripMcDmuAttributes(mcDmuObject,s.getOrig(),altMgra,s.getStopPeriod(),
                    		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                }

                // if the trip origin and sampled mgra are in the outbound
                // half-tour alighting tap shed (near tour origin)
                if (mgraInAlightingTapShed[s.getOrig()]
                        && mgraInAlightingTapShed[altMgra])
                {
                    logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                            s.getStopPeriod(), s.getTour().getPersonObject()
                                    .getHouseholdObject().getDebugChoiceModels());
                    logsumHelper
                            .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                }

            } else
            {
                // if the sampled mgra is in the inbound half-tour boarding
                // tap shed (near tour primary destination)
                if (mgraInBoardingTapShed[altMgra])
                {
                    logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                            s.getStopPeriod(), s.getTour().getPersonObject()
                                    .getHouseholdObject().getDebugChoiceModels());
                    logsumHelper
                            .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                }

                // if the sampled mgra is in the inbound half-tour alighting
                // tap shed (near tour origin)
                if (mgraInAlightingTapShed[altMgra])
                {
                    logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                    logsumHelper.setWtdTripMcDmuAttributes(mcDmuObject,s.getOrig(),altMgra,s.getStopPeriod(),
                    		s.getTour().getPersonObject().getHouseholdObject().getDebugChoiceModels());
                }

                // if the trip origin and sampled mgra are in the inbound
                // half-tour alighting tap shed (near tour origin)
                if (mgraInAlightingTapShed[s.getOrig()]
                        && mgraInAlightingTapShed[altMgra])
                {
                    logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
                    logsumHelper
                            .setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
                }

            }

        } else if (modelStructure.getTourModeIsWalkTransit(s.getTour().getTourModeChoice()))
        { // tour mode is walk-transit

            logsumHelper.setWtwTripMcDmuAttributes(mcDmuObject, s.getOrig(), altMgra,
                    s.getStopPeriod(), s.getTour().getPersonObject().getHouseholdObject()
                            .getDebugChoiceModels());

        } else
        {
            logsumHelper.setWalkTransitSkimsUnavailable(mcDmuObject);
            logsumHelper.setDriveTransitSkimsUnavailable(mcDmuObject, s.isInboundStop());
        }
        ikSegment = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra, s.getStopPeriod(),
                mcModel, mcDmuObject, tmcLogger);

        if (ikSegment < -900)
        {
            tmcLogger.error("ERROR calculating trip mode choice logsum for "
                    + (s.isInboundStop() ? "inbound" : "outbound")
                    + " stop location choice - ikLogsum = " + ikSegment + ".");
            tmcLogger
                    .error("setting debug to true and recomputing ik segment logsum in order to log utility expression results.");

            if (s.isInboundStop()) tmcLogger
                    .error(String
                            .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopDirection=%s, StopId=%d, NumIBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                    s.getTour().getPersonObject().getHouseholdObject()
                                            .getHhId(), s.getTour().getPersonObject()
                                            .getPersonNum(), s.getTour().getPersonObject()
                                            .getPersonType(), s.getTour().getTourPurpose(), s
                                            .getTour().getTourModeChoice(), s.getTour()
                                            .getTourId(), s.getDestPurpose(), "inbound", (s
                                            .getStopId() + 1),
                                    s.getTour().getNumInboundStops() - 1, s.getOrig(), altMgra));
            else tmcLogger
                    .error(String
                            .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourMode=%d, TourId=%d, StopPurpose=%s, StopDirection=%s, StopId=%d, NumOBStops=%d, StopOrig=%d, AltStopLoc=%d",
                                    s.getTour().getPersonObject().getHouseholdObject()
                                            .getHhId(), s.getTour().getPersonObject()
                                            .getPersonNum(), s.getTour().getPersonObject()
                                            .getPersonType(), s.getTour().getTourPurpose(), s
                                            .getTour().getTourModeChoice(), s.getTour()
                                            .getTourId(), s.getDestPurpose(), "outbound", (s
                                            .getStopId() + 1), s.getTour()
                                            .getNumOutboundStops() - 1, s.getOrig(), altMgra));

            mcDmuObject.getDmuIndexValues().setDebug(true);
            mcDmuObject.getHouseholdObject().setDebugChoiceModels(true);
            mcDmuObject.getDmuIndexValues().setHHIndex(
                    s.getTour().getPersonObject().getHouseholdObject().getHhId());
            ikSegment = logsumHelper.calculateTripMcLogsum(s.getOrig(), altMgra,
                    s.getStopPeriod(), mcModel, mcDmuObject, tmcLogger);
            mcDmuObject.getDmuIndexValues().setDebug(false);
            mcDmuObject.getHouseholdObject().setDebugChoiceModels(false);

        }

    }
    
    
    /**
     * Do a monte carlo selection from the array of stored mode choice
     * cumulative probabilities (0 based array). The probabilities were saved at
     * the time the stop location alternative segment mode choice logsums were
     * calculated. If the stop is not the last stop for the half-tour, the IK
     * segment probabilities are passed in. If the stop is the last stop, the KJ
     * probabilities are passed in.
     * 
     * @param household
     *            object from which to get the Random object.
     * @param props
     *            is the array of stored mode choice probabilities - 0s based
     *            array.
     * 
     * @return the selected mode choice alternative from [1,...,numMcAlts].
     */
    private int selectModeFromProbabilities(Stop s, double[] cumProbs)
    {

        Household household = s.getTour().getPersonObject().getHouseholdObject();

        int selectedModeAlt = -1;
        double rn = household.getHhRandom().nextDouble();
        int randomCount = household.getHhRandomCount();

        int numAvailAlts = 0;
        double sumProb = 0.0;
        for (int i = 0; i < cumProbs.length; i++)
        {
            double tempProb = cumProbs[i] - sumProb;
            sumProb += tempProb;

            if (tempProb > 0) numAvailAlts++;

            if (rn < cumProbs[i])
            {
                selectedModeAlt = i + 1;
                break;
            }
        }

        if (household.getDebugChoiceModels() || selectedModeAlt < 0
                || numAvailAlts >= availAltsToLog)
        {

            // set the number of available alts to log value to a large number
            // so no more get logged.
            if (numAvailAlts >= availAltsToLog)
            {
                Person person = s.getTour().getPersonObject();
                Tour tour = s.getTour();
                tmcLogger
                        .info("Monte Carlo selection for determining Mode Choice from Probabilities for stop with more than "
                                + availAltsToLog + " mode alts available.");
                tmcLogger.info("HHID=" + household.getHhId() + ", persNum=" + person.getPersonNum()
                        + ", tourPurpose=" + tour.getTourPrimaryPurpose() + ", tourId="
                        + tour.getTourId() + ", tourMode=" + tour.getTourModeChoice());
                tmcLogger.info("StopID="
                        + (s.getStopId() + 1)
                        + " of "
                        + (s.isInboundStop() ? tour.getNumInboundStops() - 1 : tour
                                .getNumOutboundStops() - 1) + " stops, inbound="
                        + s.isInboundStop() + ", stopPurpose=" + s.getDestPurpose()
                        + ", stopDepart=" + s.getStopPeriod() + ", stopOrig=" + s.getOrig()
                        + ", stopDest=" + s.getDest());
                availAltsToLog = 9999;
            }

            tmcLogger.info("");
            tmcLogger.info("");
            String separator = "";
            for (int k = 0; k < 60; k++)
                separator += "+";
            tmcLogger.info(separator);

            tmcLogger
                    .info("Alternative                      Availability           Utility       Probability           CumProb");
            tmcLogger
                    .info("---------------------            ------------       -----------    --------------    --------------");

            sumProb = 0.0;
            for (int j = 0; j < cumProbs.length; j++)
            {
                String altString = String.format("%-3d %-25s", j + 1, "");
                double tempProb = cumProbs[j] - sumProb;
                tmcLogger.info(String.format("%-30s%15s%18s%18.6e%18.6e", altString, "", "",
                        tempProb, cumProbs[j]));
                sumProb += tempProb;
            }

            if (selectedModeAlt < 0)
            {
                tmcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt,
                        "no MC alt available");
                tmcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
                throw new RuntimeException();
            } else
            {
                tmcLogger.info(" ");
                String altString = String.format("%-3d %-25s", selectedModeAlt, "");
                tmcLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString,
                        rn, randomCount));
            }

            tmcLogger.info(separator);
            tmcLogger.info("");
            tmcLogger.info("");

        }

        // if this statement is reached, there's a problem with the cumulative
        // probabilities array, so return -1.
        return selectedModeAlt;
    }

    
    
}
