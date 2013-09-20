package org.sandag.abm.visitor;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.newmodel.ChoiceModelApplication;

public class McLogsumsCalculator
        implements Serializable
{

    private transient Logger                   logger                                = Logger.getLogger("vistorModel");
    private transient Logger                   autoSkimLogger                        = Logger.getLogger("autoSkim");
    private transient Logger                   wtwSkimLogger                         = Logger.getLogger("wtwSkim");
    private transient Logger                   wtdSkimLogger                         = Logger.getLogger("wtdSkim");
    private transient Logger                   dtwSkimLogger                         = Logger.getLogger("dtwSkim");

    public static final String                 PROPERTIES_UEC_TOUR_MODE_CHOICE       = "tourModeChoice.uec.file";
    public static final String                 PROPERTIES_UEC_TRIP_MODE_CHOICE       = "tripModeChoice.uec.file";

    private static final int                   LB_ACC_TIME_INDEX                     = 0;
    private static final int                   LB_EGR_TIME_INDEX                     = 1;
    private static final int                   LB_AUX_TIME_INDEX                     = 2;
    private static final int                   LB_LB_IVT_INDEX                       = 3;
    private static final int                   LB_FWAIT_INDEX                        = 4;
    private static final int                   LB_XWAIT_INDEX                        = 5;
    private static final int                   LB_FARE_INDEX                         = 6;
    private static final int                   LB_XFERS_INDEX                        = 7;

    private static final int                   PREM_ACC_TIME_INDEX                   = 0;
    private static final int                   PREM_EGR_TIME_INDEX                   = 1;
    private static final int                   PREM_AUX_TIME_INDEX                   = 2;
    private static final int                   PREM_LB_IVT_INDEX                     = 3;
    private static final int                   PREM_EB_IVT_INDEX                     = 4;
    private static final int                   PREM_BRT_IVT_INDEX                    = 5;
    private static final int                   PREM_LR_IVT_INDEX                     = 6;
    private static final int                   PREM_CR_IVT_INDEX                     = 7;
    private static final int                   PREM_FWAIT_INDEX                      = 8;
    private static final int                   PREM_XWAIT_INDEX                      = 9;
    private static final int                   PREM_FARE_INDEX                       = 10;
    private static final int                   PREM_MAIN_MODE_INDEX                  = 11;
    private static final int                   PREM_XFERS_INDEX                      = 12;

    public static final int                    LB                                    = 0;
    public static final int                    EB                                    = 1;
    public static final int                    BRT                                   = 2;
    public static final int                    LR                                    = 3;
    public static final int                    CR                                    = 4;
    public static final int                    NUM_LOC_PREM                          = 5;

    public static final int                    WTW                                   = 0;
    public static final int                    WTD                                   = 1;
    public static final int                    DTW                                   = 2;
    public static final int                    NUM_ACC_EGR                           = 3;

    public static final int                    LB_IVT                                = 0;
    public static final int                    EB_IVT                                = 1;
    public static final int                    BRT_IVT                               = 2;
    public static final int                    LR_IVT                                = 3;
    public static final int                    CR_IVT                                = 4;
    public static final int                    ACC                                   = 5;
    public static final int                    EGR                                   = 6;
    public static final int                    AUX                                   = 7;
    public static final int                    FWAIT                                 = 8;
    public static final int                    XWAIT                                 = 9;
    public static final int                    FARE                                  = 10;
    public static final int                    XFERS                                 = 11;
    public static final int                    NUM_SKIMS                             = 13;

    public static final int                    OUT                                   = 0;
    public static final int                    IN                                    = 1;
    public static final int                    NUM_DIR                               = 2;

    private BestTransitPathCalculator          bestPathUEC;
    private double[]                           tripModeChoiceSegmentStoredProbabilities;

    private TazDataManager                     tazManager;
    private MgraDataManager                    mgraManager;

    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;

    private int[][]                            bestWtwTapPairsOut;
    private int[][]                            bestWtwTapPairsIn;
    private int[][]                            bestWtdTapPairsOut;
    private int[][]                            bestWtdTapPairsIn;
    private int[][]                            bestDtwTapPairsOut;
    private int[][]                            bestDtwTapPairsIn;

    private AutoAndNonMotorizedSkimsCalculator anm;
    private WalkTransitWalkSkimsCalculator     wtw;
    private WalkTransitDriveSkimsCalculator    wtd;
    private DriveTransitWalkSkimsCalculator    dtw;

    private int                                setTourMcLogsumDmuAttributesTotalTime = 0;
    private int                                setTripMcLogsumDmuAttributesTotalTime = 0;

    public McLogsumsCalculator()
    {
        if (mgraManager == null) mgraManager = MgraDataManager.getInstance();

        if (tazManager == null) tazManager = TazDataManager.getInstance();

        this.lsWgtAvgCostM = mgraManager.getLsWgtAvgCostM();
        this.lsWgtAvgCostD = mgraManager.getLsWgtAvgCostD();
        this.lsWgtAvgCostH = mgraManager.getLsWgtAvgCostH();

    }

    public BestTransitPathCalculator getBestTransitPathCalculator()
    {
        return bestPathUEC;
    }

    public void setupSkimCalculators(HashMap<String, String> rbMap)
    {
        bestPathUEC = new BestTransitPathCalculator(rbMap);

        anm = new AutoAndNonMotorizedSkimsCalculator(rbMap);
        wtw = new WalkTransitWalkSkimsCalculator();
        wtw.setup(rbMap, wtwSkimLogger, bestPathUEC);
        wtd = new WalkTransitDriveSkimsCalculator();
        wtd.setup(rbMap, wtdSkimLogger, bestPathUEC);
        dtw = new DriveTransitWalkSkimsCalculator();
        dtw.setup(rbMap, dtwSkimLogger, bestPathUEC);
    }

    public void setTazDistanceSkimArrays(double[][][] storedFromTazDistanceSkims,
            double[][][] storedToTazDistanceSkims)
    {
        anm.setTazDistanceSkimArrays(storedFromTazDistanceSkims, storedToTazDistanceSkims);
    }

    public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator()
    {
        return anm;
    }

    public WalkTransitWalkSkimsCalculator getWtwSkimCalculator()
    {
        return wtw;
    }

    public WalkTransitDriveSkimsCalculator getWtdSkimCalculator()
    {
        return wtd;
    }

    public DriveTransitWalkSkimsCalculator getDtwSkimCalculator()
    {
        return dtw;
    }

    public void setTourMcDmuAttributes(VisitorTourModeChoiceDMU mcDmuObject, int origMgra,
            int destMgra, int departPeriod, int arrivePeriod, boolean debug)
    {

        setNmTourMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod, debug);
        setWtwTourMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod,
                debug);
        setWtdTourMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod,
                debug);
        setDtwTourMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod,
                debug);

    }

    /**
     * This method finds the best walk-transit-walk tap pairs if they don't exist in the trip object (and sets the best tap pairs in the trip object),
     * then fills in the trip mode choice DMU object with the skim values corresponding to those tap pairs.
     * 
     * @param tour
     * @param trip
     * @param mcDmuObject
     */
    public void setTripMcDmuSkimAttributes(VisitorTour tour, VisitorTrip trip,
            VisitorTripModeChoiceDMU mcDmuObject)
    {

        int origMgra = trip.getOriginMgra();
        int destMgra = trip.getDestinationMgra();
        int departPeriod = trip.getPeriod();
        boolean debug = tour.getDebugChoiceModels();

        setNmTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, debug);

        int[][] bestTapPairs = null;

        if (trip.getBestWtwTapPairs() == null)
        {

            int outSkimPeriod = ModelStructure.getSkimPeriodIndex(departPeriod);
            bestTapPairs = wtw.getBestTapPairs(origMgra, destMgra, outSkimPeriod, debug, logger);
            trip.setBestWtwTapPairs(bestTapPairs);
        } else
        {
            bestTapPairs = trip.getBestWtwTapPairs();
        }

        setWtwTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, bestTapPairs,
                debug);

        setWtdTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, null, debug);

        setDtwTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, null, debug);

    }

    public double calculateTourMcLogsum(int origMgra, int destMgra, int departPeriod,
            int arrivePeriod, ChoiceModelApplication mcModel, VisitorTourModeChoiceDMU mcDmuObject)
    {

        long currentTime = System.currentTimeMillis();
        setTourMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, arrivePeriod,
                mcDmuObject.getDmuIndexValues().getDebug());
        setTourMcLogsumDmuAttributesTotalTime += (System.currentTimeMillis() - currentTime);

        // set the land use data items in the DMU for the origin
        mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(origMgra));
        mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(origMgra));
        mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(origMgra));

        // set the land use data items in the DMU for the destination
        mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(destMgra));
        mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(destMgra));
        mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(destMgra));

        mcDmuObject.setLsWgtAvgCostM(lsWgtAvgCostM[destMgra]);
        mcDmuObject.setLsWgtAvgCostD(lsWgtAvgCostD[destMgra]);
        mcDmuObject.setLsWgtAvgCostH(lsWgtAvgCostH[destMgra]);

        // mode choice UEC references highway skim matrices directly, so set index orig/dest to O/D TAZs.
        IndexValues mcDmuIndex = mcDmuObject.getDmuIndexValues();
        int tourOrigTaz = mgraManager.getTaz(origMgra);
        int tourDestTaz = mgraManager.getTaz(destMgra);
        mcDmuIndex.setOriginZone(tourOrigTaz);
        mcDmuIndex.setDestZone(tourDestTaz);

        mcDmuObject.setPTazTerminalTime(tazManager.getOriginTazTerminalTime(tourOrigTaz));
        mcDmuObject.setATazTerminalTime(tazManager.getDestinationTazTerminalTime(tourDestTaz));

        mcModel.computeUtilities(mcDmuObject, mcDmuIndex);
        double logsum = mcModel.getLogsum();

        return logsum;

    }

    private void setNmTripMcDmuAttributes(VisitorTripModeChoiceDMU tripMcDmuObject, int origMgra,
            int destMgra, int departPeriod, boolean loggingEnabled)
    {

        double[] nmSkims = null;

        // non-motorized, outbound then inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        nmSkims = anm.getNonMotorizedSkims(origMgra, destMgra, departPeriod, loggingEnabled,
                autoSkimLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(origMgra, destMgra, departPeriod, nmSkims,
                    "non-motorized trip mode choice skims", autoSkimLogger);

        int walkIndex = anm.getNmWalkTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedWalkTime(nmSkims[walkIndex]);

        int bikeIndex = anm.getNmBikeTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedBikeTime(nmSkims[bikeIndex]);

    }

    /**
     * return the array of mode choice model cumulative probabilities determined while computing the mode choice logsum for the trip segmen during
     * stop location choice. These probabilities arrays are stored for each sampled stop location so that when the selected sample stop location is
     * known, the mode choice can be drawn from the already computed probabilities.
     * 
     * @return mode choice cumulative probabilities array
     */
    public double[] getStoredSegmentCumulativeProbabilities()
    {
        return tripModeChoiceSegmentStoredProbabilities;
    }

    /******************************************************/

    public int[][] getBestWtwTapsOut()
    {
        return bestWtwTapPairsOut;
    }

    public int[][] getBestWtwTapsIn()
    {
        return bestWtwTapPairsIn;
    }

    public int[][] getBestWtdTapsOut()
    {
        return bestWtdTapPairsOut;
    }

    public int[][] getBestWtdTapsIn()
    {
        return bestWtdTapPairsIn;
    }

    public int[][] getBestDtwTapsOut()
    {
        return bestDtwTapPairsOut;
    }

    public int[][] getBestDtwTapsIn()
    {
        return bestDtwTapPairsIn;
    }

    private void setNmTourMcDmuAttributes(VisitorTourModeChoiceDMU mcDmuObject, int origMgra,
            int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled)
    {
        // non-motorized, outbound then inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        double[] nmSkimsOut = anm.getNonMotorizedSkims(origMgra, destMgra, departPeriod,
                loggingEnabled, autoSkimLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(origMgra, destMgra, departPeriod, nmSkimsOut,
                    "non-motorized outbound", autoSkimLogger);

        skimPeriodIndex = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        arrivePeriod = skimPeriodIndex;
        double[] nmSkimsIn = anm.getNonMotorizedSkims(destMgra, origMgra, arrivePeriod,
                loggingEnabled, autoSkimLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(destMgra, origMgra, arrivePeriod, nmSkimsIn,
                    "non-motorized inbound", autoSkimLogger);

        int walkIndex = anm.getNmWalkTimeSkimIndex();
        mcDmuObject.setNmWalkTimeOut(nmSkimsOut[walkIndex]);
        mcDmuObject.setNmWalkTimeIn(nmSkimsIn[walkIndex]);

        int bikeIndex = anm.getNmBikeTimeSkimIndex();
        mcDmuObject.setNmBikeTimeOut(nmSkimsOut[bikeIndex]);
        mcDmuObject.setNmBikeTimeIn(nmSkimsIn[bikeIndex]);

    }

    private void setWtwTourMcDmuAttributes(VisitorTourModeChoiceDMU mcDmuObject, int origMgra,
            int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled)
    {
        // walk access, walk egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        bestWtwTapPairsOut = wtw.getBestTapPairs(origMgra, destMgra, departPeriod, loggingEnabled,
                wtwSkimLogger);

        int i = Modes.getTransitModeIndex("CR");
        double[] crSkimsOut = null;
        if (bestWtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsOut = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsOut[i][0], bestWtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            crSkimsOut = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsOut = null;
        if (bestWtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsOut = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsOut[i][0], bestWtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lrSkimsOut = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsOut = null;
        if (bestWtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsOut = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsOut[i][0], bestWtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            brSkimsOut = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsOut = null;
        if (bestWtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsOut = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsOut[i][0], bestWtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            ebSkimsOut = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsOut = null;
        if (bestWtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsOut = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsOut[i][0], bestWtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lbSkimsOut = wtw.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(WTW, LB, LB_IVT, OUT, lbSkimsOut[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, FWAIT, OUT, lbSkimsOut[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, XWAIT, OUT, lbSkimsOut[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, ACC, OUT, lbSkimsOut[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, EGR, OUT, lbSkimsOut[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, AUX, OUT, lbSkimsOut[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, FARE, OUT, lbSkimsOut[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, XFERS, OUT, lbSkimsOut[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, EB, LB_IVT, OUT, ebSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, EB_IVT, OUT, ebSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, FWAIT, OUT, ebSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, XWAIT, OUT, ebSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, ACC, OUT, ebSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, EGR, OUT, ebSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, AUX, OUT, ebSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, FARE, OUT, ebSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, XFERS, OUT, ebSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, BRT, LB_IVT, OUT, brSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, EB_IVT, OUT, brSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, BRT_IVT, OUT, brSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, FWAIT, OUT, brSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, XWAIT, OUT, brSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, ACC, OUT, brSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, EGR, OUT, brSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, AUX, OUT, brSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, FARE, OUT, brSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, XFERS, OUT, brSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, LR, LB_IVT, OUT, lrSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, EB_IVT, OUT, lrSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, BRT_IVT, OUT, lrSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, LR_IVT, OUT, lrSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, FWAIT, OUT, lrSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, XWAIT, OUT, lrSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, ACC, OUT, lrSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, EGR, OUT, lrSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, AUX, OUT, lrSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, FARE, OUT, lrSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, XFERS, OUT, lrSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, CR, LB_IVT, OUT, crSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, EB_IVT, OUT, crSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, BRT_IVT, OUT, crSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, LR_IVT, OUT, crSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, CR_IVT, OUT, crSkimsOut[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, FWAIT, OUT, crSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, XWAIT, OUT, crSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, ACC, OUT, crSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, EGR, OUT, crSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, AUX, OUT, crSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, FARE, OUT, crSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, XFERS, OUT, crSkimsOut[PREM_XFERS_INDEX]);

        // walk access, walk egress transit, inbound
        skimPeriodIndex = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        arrivePeriod = skimPeriodIndex;
        bestWtwTapPairsIn = wtw.getBestTapPairs(destMgra, origMgra, arrivePeriod, loggingEnabled,
                wtwSkimLogger);

        i = Modes.getTransitModeIndex("CR");
        double[] crSkimsIn = null;
        if (bestWtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsIn = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsIn[i][0], bestWtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            crSkimsIn = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsIn = null;
        if (bestWtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsIn = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsIn[i][0], bestWtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lrSkimsIn = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsIn = null;
        if (bestWtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsIn = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsIn[i][0], bestWtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            brSkimsIn = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsIn = null;
        if (bestWtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsIn = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsIn[i][0], bestWtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            ebSkimsIn = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsIn = null;
        if (bestWtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsIn = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestWtwTapPairsIn[i][0], bestWtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lbSkimsIn = wtw.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(WTW, LB, LB_IVT, IN, lbSkimsIn[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, FWAIT, IN, lbSkimsIn[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, XWAIT, IN, lbSkimsIn[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, ACC, IN, lbSkimsIn[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, EGR, IN, lbSkimsIn[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, AUX, IN, lbSkimsIn[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, FARE, IN, lbSkimsIn[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LB, XFERS, IN, lbSkimsIn[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, EB, LB_IVT, IN, ebSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, EB_IVT, IN, ebSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, FWAIT, IN, ebSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, XWAIT, IN, ebSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, ACC, IN, ebSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, EGR, IN, ebSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, AUX, IN, ebSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, FARE, IN, ebSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, EB, XFERS, IN, ebSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, BRT, LB_IVT, IN, brSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, EB_IVT, IN, brSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, BRT_IVT, IN, brSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, FWAIT, IN, brSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, XWAIT, IN, brSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, ACC, IN, brSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, EGR, IN, brSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, AUX, IN, brSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, FARE, IN, brSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, BRT, XFERS, IN, brSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, LR, LB_IVT, IN, lrSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, EB_IVT, IN, lrSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, BRT_IVT, IN, lrSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, LR_IVT, IN, lrSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, FWAIT, IN, lrSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, XWAIT, IN, lrSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, ACC, IN, lrSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, EGR, IN, lrSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, AUX, IN, lrSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, FARE, IN, lrSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, LR, XFERS, IN, lrSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTW, CR, LB_IVT, IN, crSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, EB_IVT, IN, crSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, BRT_IVT, IN, crSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, LR_IVT, IN, crSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, CR_IVT, IN, crSkimsIn[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, FWAIT, IN, crSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, XWAIT, IN, crSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, ACC, IN, crSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, EGR, IN, crSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, AUX, IN, crSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, FARE, IN, crSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTW, CR, XFERS, IN, crSkimsIn[PREM_XFERS_INDEX]);

    }

    private void setWtdTourMcDmuAttributes(VisitorTourModeChoiceDMU mcDmuObject, int origMgra,
            int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled)
    {
        // walk access, drive egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        bestWtdTapPairsOut = wtd.getBestTapPairs(origMgra, destMgra, departPeriod, loggingEnabled,
                wtdSkimLogger);

        int i = Modes.getTransitModeIndex("CR");
        double[] crSkimsOut = null;
        if (bestWtdTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsOut = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsOut[i][0], bestWtdTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            crSkimsOut = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsOut = null;
        if (bestWtdTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsOut = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsOut[i][0], bestWtdTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lrSkimsOut = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsOut = null;
        if (bestWtdTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsOut = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsOut[i][0], bestWtdTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            brSkimsOut = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsOut = null;
        if (bestWtdTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsOut = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsOut[i][0], bestWtdTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            ebSkimsOut = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsOut = null;
        if (bestWtdTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsOut = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsOut[i][0], bestWtdTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lbSkimsOut = wtd.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(WTD, LB, LB_IVT, OUT, lbSkimsOut[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, FWAIT, OUT, lbSkimsOut[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, XWAIT, OUT, lbSkimsOut[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, ACC, OUT, lbSkimsOut[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, EGR, OUT, lbSkimsOut[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, AUX, OUT, lbSkimsOut[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, FARE, OUT, lbSkimsOut[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, XFERS, OUT, lbSkimsOut[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, EB, LB_IVT, OUT, ebSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, EB_IVT, OUT, ebSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, FWAIT, OUT, ebSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, XWAIT, OUT, ebSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, ACC, OUT, ebSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, EGR, OUT, ebSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, AUX, OUT, ebSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, FARE, OUT, ebSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, XFERS, OUT, ebSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, BRT, LB_IVT, OUT, brSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, EB_IVT, OUT, brSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, BRT_IVT, OUT, brSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, FWAIT, OUT, brSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, XWAIT, OUT, brSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, ACC, OUT, brSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, EGR, OUT, brSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, AUX, OUT, brSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, FARE, OUT, brSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, XFERS, OUT, brSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, LR, LB_IVT, OUT, lrSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, EB_IVT, OUT, lrSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, BRT_IVT, OUT, lrSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, LR_IVT, OUT, lrSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, FWAIT, OUT, lrSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, XWAIT, OUT, lrSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, ACC, OUT, lrSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, EGR, OUT, lrSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, AUX, OUT, lrSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, FARE, OUT, lrSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, XFERS, OUT, lrSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, CR, LB_IVT, OUT, crSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, EB_IVT, OUT, crSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, BRT_IVT, OUT, crSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, LR_IVT, OUT, crSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, CR_IVT, OUT, crSkimsOut[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, FWAIT, OUT, crSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, XWAIT, OUT, crSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, ACC, OUT, crSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, EGR, OUT, crSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, AUX, OUT, crSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, FARE, OUT, crSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, XFERS, OUT, crSkimsOut[PREM_XFERS_INDEX]);

        // walk access, drive egress transit, inbound
        skimPeriodIndex = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        arrivePeriod = skimPeriodIndex;
        bestWtdTapPairsIn = wtd.getBestTapPairs(destMgra, origMgra, arrivePeriod, loggingEnabled,
                wtdSkimLogger);

        i = Modes.getTransitModeIndex("CR");
        double[] crSkimsIn = null;
        if (bestWtdTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsIn = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsIn[i][0], bestWtdTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            crSkimsIn = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsIn = null;
        if (bestWtdTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsIn = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsIn[i][0], bestWtdTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lrSkimsIn = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsIn = null;
        if (bestWtdTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsIn = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsIn[i][0], bestWtdTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            brSkimsIn = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsIn = null;
        if (bestWtdTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsIn = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsIn[i][0], bestWtdTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            ebSkimsIn = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsIn = null;
        if (bestWtdTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsIn = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestWtdTapPairsIn[i][0], bestWtdTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lbSkimsIn = wtd.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(WTD, LB, LB_IVT, IN, lbSkimsIn[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, FWAIT, IN, lbSkimsIn[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, XWAIT, IN, lbSkimsIn[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, ACC, IN, lbSkimsIn[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, EGR, IN, lbSkimsIn[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, AUX, IN, lbSkimsIn[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, FARE, IN, lbSkimsIn[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LB, XFERS, IN, lbSkimsIn[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, EB, LB_IVT, IN, ebSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, EB_IVT, IN, ebSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, FWAIT, IN, ebSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, XWAIT, IN, ebSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, ACC, IN, ebSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, EGR, IN, ebSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, AUX, IN, ebSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, FARE, IN, ebSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, EB, XFERS, IN, ebSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, BRT, LB_IVT, IN, brSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, EB_IVT, IN, brSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, BRT_IVT, IN, brSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, FWAIT, IN, brSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, XWAIT, IN, brSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, ACC, IN, brSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, EGR, IN, brSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, AUX, IN, brSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, FARE, IN, brSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, BRT, XFERS, IN, brSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, LR, LB_IVT, IN, lrSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, EB_IVT, IN, lrSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, BRT_IVT, IN, lrSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, LR_IVT, IN, lrSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, FWAIT, IN, lrSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, XWAIT, IN, lrSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, ACC, IN, lrSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, EGR, IN, lrSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, AUX, IN, lrSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, FARE, IN, lrSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, LR, XFERS, IN, lrSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(WTD, CR, LB_IVT, IN, crSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, EB_IVT, IN, crSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, BRT_IVT, IN, crSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, LR_IVT, IN, crSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, CR_IVT, IN, crSkimsIn[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, FWAIT, IN, crSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, XWAIT, IN, crSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, ACC, IN, crSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, EGR, IN, crSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, AUX, IN, crSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, FARE, IN, crSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(WTD, CR, XFERS, IN, crSkimsIn[PREM_XFERS_INDEX]);

    }

    private void setDtwTourMcDmuAttributes(VisitorTourModeChoiceDMU mcDmuObject, int origMgra,
            int destMgra, int departPeriod, int arrivePeriod, boolean loggingEnabled)
    {
        // drive access, walk egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        bestDtwTapPairsOut = dtw.getBestTapPairs(origMgra, destMgra, departPeriod, loggingEnabled,
                dtwSkimLogger);

        int i = Modes.getTransitModeIndex("CR");
        double[] crSkimsOut = null;
        if (bestDtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsOut = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsOut[i][0], bestDtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            crSkimsOut = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsOut = null;
        if (bestDtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsOut = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsOut[i][0], bestDtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lrSkimsOut = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsOut = null;
        if (bestDtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsOut = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsOut[i][0], bestDtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            brSkimsOut = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsOut = null;
        if (bestDtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsOut = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsOut[i][0], bestDtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            ebSkimsOut = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsOut = null;
        if (bestDtwTapPairsOut[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsOut = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsOut[i][0], bestDtwTapPairsOut[i][1], departPeriod,
                    loggingEnabled);
        } else
        {
            lbSkimsOut = dtw.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(DTW, LB, LB_IVT, OUT, lbSkimsOut[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, FWAIT, OUT, lbSkimsOut[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, XWAIT, OUT, lbSkimsOut[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, ACC, OUT, lbSkimsOut[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, EGR, OUT, lbSkimsOut[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, AUX, OUT, lbSkimsOut[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, FARE, OUT, lbSkimsOut[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, XFERS, OUT, lbSkimsOut[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, EB, LB_IVT, OUT, ebSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, EB_IVT, OUT, ebSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, FWAIT, OUT, ebSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, XWAIT, OUT, ebSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, ACC, OUT, ebSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, EGR, OUT, ebSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, AUX, OUT, ebSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, FARE, OUT, ebSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, XFERS, OUT, ebSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, BRT, LB_IVT, OUT, brSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, EB_IVT, OUT, brSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, BRT_IVT, OUT, brSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, FWAIT, OUT, brSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, XWAIT, OUT, brSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, ACC, OUT, brSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, EGR, OUT, brSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, AUX, OUT, brSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, FARE, OUT, brSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, XFERS, OUT, brSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, LR, LB_IVT, OUT, lrSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, EB_IVT, OUT, lrSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, BRT_IVT, OUT, lrSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, LR_IVT, OUT, lrSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, FWAIT, OUT, lrSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, XWAIT, OUT, lrSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, ACC, OUT, lrSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, EGR, OUT, lrSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, AUX, OUT, lrSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, FARE, OUT, lrSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, XFERS, OUT, lrSkimsOut[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, CR, LB_IVT, OUT, crSkimsOut[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, EB_IVT, OUT, crSkimsOut[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, BRT_IVT, OUT, crSkimsOut[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, LR_IVT, OUT, crSkimsOut[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, CR_IVT, OUT, crSkimsOut[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, FWAIT, OUT, crSkimsOut[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, XWAIT, OUT, crSkimsOut[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, ACC, OUT, crSkimsOut[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, EGR, OUT, crSkimsOut[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, AUX, OUT, crSkimsOut[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, FARE, OUT, crSkimsOut[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, XFERS, OUT, crSkimsOut[PREM_XFERS_INDEX]);

        // drive access, walk egress transit, inbound
        skimPeriodIndex = ModelStructure.getSkimPeriodIndex(arrivePeriod);
        arrivePeriod = skimPeriodIndex;
        bestDtwTapPairsIn = dtw.getBestTapPairs(destMgra, origMgra, arrivePeriod, loggingEnabled,
                dtwSkimLogger);

        i = Modes.getTransitModeIndex("CR");
        double[] crSkimsIn = null;
        if (bestDtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            crSkimsIn = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsIn[i][0], bestDtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            crSkimsIn = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        double[] lrSkimsIn = null;
        if (bestDtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lrSkimsIn = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsIn[i][0], bestDtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lrSkimsIn = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        double[] brSkimsIn = null;
        if (bestDtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            brSkimsIn = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsIn[i][0], bestDtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            brSkimsIn = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        double[] ebSkimsIn = null;
        if (bestDtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            ebSkimsIn = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsIn[i][0], bestDtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            ebSkimsIn = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        double[] lbSkimsIn = null;
        if (bestDtwTapPairsIn[i] != null)
        {
            double pWalkTime = bestPathUEC.getBestAccessTime(i);
            double aWalkTime = bestPathUEC.getBestEgressTime(i);
            lbSkimsIn = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestDtwTapPairsIn[i][0], bestDtwTapPairsIn[i][1], arrivePeriod, loggingEnabled);
        } else
        {
            lbSkimsIn = dtw.getNullTransitSkims(i);
        }

        mcDmuObject.setTransitSkim(DTW, LB, LB_IVT, IN, lbSkimsIn[LB_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, FWAIT, IN, lbSkimsIn[LB_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, XWAIT, IN, lbSkimsIn[LB_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, ACC, IN, lbSkimsIn[LB_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, EGR, IN, lbSkimsIn[LB_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, AUX, IN, lbSkimsIn[LB_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, FARE, IN, lbSkimsIn[LB_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LB, XFERS, IN, lbSkimsIn[LB_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, EB, LB_IVT, IN, ebSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, EB_IVT, IN, ebSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, FWAIT, IN, ebSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, XWAIT, IN, ebSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, ACC, IN, ebSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, EGR, IN, ebSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, AUX, IN, ebSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, FARE, IN, ebSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, EB, XFERS, IN, ebSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, BRT, LB_IVT, IN, brSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, EB_IVT, IN, brSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, BRT_IVT, IN, brSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, FWAIT, IN, brSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, XWAIT, IN, brSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, ACC, IN, brSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, EGR, IN, brSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, AUX, IN, brSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, FARE, IN, brSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, BRT, XFERS, IN, brSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, LR, LB_IVT, IN, lrSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, EB_IVT, IN, lrSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, BRT_IVT, IN, lrSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, LR_IVT, IN, lrSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, FWAIT, IN, lrSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, XWAIT, IN, lrSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, ACC, IN, lrSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, EGR, IN, lrSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, AUX, IN, lrSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, FARE, IN, lrSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, LR, XFERS, IN, lrSkimsIn[PREM_XFERS_INDEX]);

        mcDmuObject.setTransitSkim(DTW, CR, LB_IVT, IN, crSkimsIn[PREM_LB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, EB_IVT, IN, crSkimsIn[PREM_EB_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, BRT_IVT, IN, crSkimsIn[PREM_BRT_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, LR_IVT, IN, crSkimsIn[PREM_LR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, CR_IVT, IN, crSkimsIn[PREM_CR_IVT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, FWAIT, IN, crSkimsIn[PREM_FWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, XWAIT, IN, crSkimsIn[PREM_XWAIT_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, ACC, IN, crSkimsIn[PREM_ACC_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, EGR, IN, crSkimsIn[PREM_EGR_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, AUX, IN, crSkimsIn[PREM_AUX_TIME_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, FARE, IN, crSkimsIn[PREM_FARE_INDEX]);
        mcDmuObject.setTransitSkim(DTW, CR, XFERS, IN, crSkimsIn[PREM_XFERS_INDEX]);

    }

    private void setWtwTripMcDmuAttributes(VisitorTripModeChoiceDMU tripMcDmuObject, int origMgra,
            int destMgra, int departPeriod, int[][] bestTapPairs, boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtw.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // walk access, walk egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtw.getWalkTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtw.getNullTransitSkims(i);
        }

        tripMcDmuObject.setTransitSkim(WTW, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, FARE, lbSkims[LB_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTW, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTW, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTW, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTW, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, FARE, crSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTW, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

    private void setWtdTripMcDmuAttributes(VisitorTripModeChoiceDMU tripMcDmuObject, int origMgra,
            int destMgra, int departPeriod, int[][] bestTapPairs, boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtd.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // walk access, drive egress transit, inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtd.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtd.getWalkTransitDriveSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtd.getNullTransitSkims(i);
        }

        tripMcDmuObject.setTransitSkim(WTD, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, FARE, lbSkims[LB_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTD, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTD, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTD, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(WTD, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, FARE, crSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(WTD, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

    private void setDtwTripMcDmuAttributes(VisitorTripModeChoiceDMU tripMcDmuObject, int origMgra,
            int destMgra, int departPeriod, int[][] bestTapPairs, boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = dtw.getNullTransitSkims(Modes.getTransitModeIndex("LB"));
            return;
        }

        // drive access, walk egress transit, outbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;

        int i = Modes.getTransitModeIndex("CR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            crSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = dtw.getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = dtw.getDriveTransitWalkSkims(i, pWalkTime, aWalkTime, bestTapPairs[i][0],
                    bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = dtw.getNullTransitSkims(i);
        }

        tripMcDmuObject.setTransitSkim(DTW, LB, LB_IVT, lbSkims[LB_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, FWAIT, lbSkims[LB_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, XWAIT, lbSkims[LB_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, ACC, lbSkims[LB_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, EGR, lbSkims[LB_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, AUX, lbSkims[LB_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, FARE, lbSkims[LB_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LB, XFERS, lbSkims[LB_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(DTW, EB, LB_IVT, ebSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, EB_IVT, ebSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, FWAIT, ebSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, XWAIT, ebSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, ACC, ebSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, EGR, ebSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, AUX, ebSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, FARE, ebSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, EB, XFERS, ebSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(DTW, BRT, LB_IVT, brSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, EB_IVT, brSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, BRT_IVT, brSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, FWAIT, brSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, XWAIT, brSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, ACC, brSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, EGR, brSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, AUX, brSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, FARE, brSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, BRT, XFERS, brSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(DTW, LR, LB_IVT, lrSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, EB_IVT, lrSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, BRT_IVT, lrSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, LR_IVT, lrSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, FWAIT, lrSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, XWAIT, lrSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, ACC, lrSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, EGR, lrSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, AUX, lrSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, FARE, lrSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, LR, XFERS, lrSkims[PREM_XFERS_INDEX]);

        tripMcDmuObject.setTransitSkim(DTW, CR, LB_IVT, crSkims[PREM_LB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, EB_IVT, crSkims[PREM_EB_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, BRT_IVT, crSkims[PREM_BRT_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, LR_IVT, crSkims[PREM_LR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, CR_IVT, crSkims[PREM_CR_IVT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, FWAIT, crSkims[PREM_FWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, XWAIT, crSkims[PREM_XWAIT_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, ACC, crSkims[PREM_ACC_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, EGR, crSkims[PREM_EGR_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, AUX, crSkims[PREM_AUX_TIME_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, FARE, crSkims[PREM_FARE_INDEX]);
        tripMcDmuObject.setTransitSkim(DTW, CR, XFERS, crSkims[PREM_XFERS_INDEX]);

    }

}
