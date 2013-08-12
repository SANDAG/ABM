package org.sandag.abm.crossborder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;

public class McLogsumsCalculator
        implements Serializable
{

    private transient Logger                                logger                                = Logger.getLogger("crossBorderModel");
    private transient Logger                                autoSkimLogger                        = Logger.getLogger("crossBorderModel");
    private transient Logger                                wtwSkimLogger                         = Logger.getLogger("crossBorderModel");
    private transient Logger                                wtdSkimLogger                         = Logger.getLogger("crossBorderModel");
    private transient Logger                                dtwSkimLogger                         = Logger.getLogger("crossBorderModel");

    // public static final String PROPERTIES_UEC_TOUR_MODE_CHOICE = "tourModeChoice.uec.file";
    // public static final String PROPERTIES_UEC_TRIP_MODE_CHOICE = "tripModeChoice.uec.file";

    private static final int                                LB_ACC_TIME_INDEX                     = 0;
    private static final int                                LB_EGR_TIME_INDEX                     = 1;
    private static final int                                LB_AUX_TIME_INDEX                     = 2;
    private static final int                                LB_LB_IVT_INDEX                       = 3;
    private static final int                                LB_FWAIT_INDEX                        = 4;
    private static final int                                LB_XWAIT_INDEX                        = 5;
    private static final int                                LB_FARE_INDEX                         = 6;
    private static final int                                LB_XFERS_INDEX                        = 7;

    private static final int                                PREM_ACC_TIME_INDEX                   = 0;
    private static final int                                PREM_EGR_TIME_INDEX                   = 1;
    private static final int                                PREM_AUX_TIME_INDEX                   = 2;
    private static final int                                PREM_LB_IVT_INDEX                     = 3;
    private static final int                                PREM_EB_IVT_INDEX                     = 4;
    private static final int                                PREM_BRT_IVT_INDEX                    = 5;
    private static final int                                PREM_LR_IVT_INDEX                     = 6;
    private static final int                                PREM_CR_IVT_INDEX                     = 7;
    private static final int                                PREM_FWAIT_INDEX                      = 8;
    private static final int                                PREM_XWAIT_INDEX                      = 9;
    private static final int                                PREM_FARE_INDEX                       = 10;
    private static final int                                PREM_MAIN_MODE_INDEX                  = 11;
    private static final int                                PREM_XFERS_INDEX                      = 12;

    public static final int                                 LB                                    = 0;
    public static final int                                 EB                                    = 1;
    public static final int                                 BRT                                   = 2;
    public static final int                                 LR                                    = 3;
    public static final int                                 CR                                    = 4;
    public static final int                                 NUM_LOC_PREM                          = 5;

    public static final int                                 WTW                                   = 0;
    public static final int                                 WTD                                   = 1;
    public static final int                                 DTW                                   = 2;
    public static final int                                 NUM_ACC_EGR                           = 3;

    public static final int                                 LB_IVT                                = 0;
    public static final int                                 EB_IVT                                = 1;
    public static final int                                 BRT_IVT                               = 2;
    public static final int                                 LR_IVT                                = 3;
    public static final int                                 CR_IVT                                = 4;
    public static final int                                 ACC                                   = 5;
    public static final int                                 EGR                                   = 6;
    public static final int                                 AUX                                   = 7;
    public static final int                                 FWAIT                                 = 8;
    public static final int                                 XWAIT                                 = 9;
    public static final int                                 FARE                                  = 10;
    public static final int                                 XFERS                                 = 11;
    public static final int                                 NUM_SKIMS                             = 13;

    public static final int                                 OUT                                   = 0;
    public static final int                                 IN                                    = 1;
    public static final int                                 NUM_DIR                               = 2;

    private ThreadLocal<BestTransitPathCalculator>          bestPathUEC;
    private double[]                                        tripModeChoiceSegmentStoredProbabilities;

    private TazDataManager                                  tazManager;
    private MgraDataManager                                 mgraManager;

    private double[]                                        lsWgtAvgCostM;
    private double[]                                        lsWgtAvgCostD;
    private double[]                                        lsWgtAvgCostH;

    private int[][]                                         bestWtwTapPairsOut;
    private int[][]                                         bestWtwTapPairsIn;
    private int[][]                                         bestWtdTapPairsOut;
    private int[][]                                         bestWtdTapPairsIn;
    private int[][]                                         bestDtwTapPairsOut;
    private int[][]                                         bestDtwTapPairsIn;

    private ThreadLocal<AutoAndNonMotorizedSkimsCalculator> anm;
    private ThreadLocal<WalkTransitWalkSkimsCalculator>     wtw;
    private ThreadLocal<WalkTransitDriveSkimsCalculator>    wtd;
    private ThreadLocal<DriveTransitWalkSkimsCalculator>    dtw;

    private int                                             setTourMcLogsumDmuAttributesTotalTime = 0;
    private int                                             setTripMcLogsumDmuAttributesTotalTime = 0;

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
        return bestPathUEC.get();
    }

    public void setupSkimCalculators(final HashMap<String, String> rbMap)
    {
        bestPathUEC = new ThreadLocal<BestTransitPathCalculator>()
        {
            @Override
            protected BestTransitPathCalculator initialValue()
            {
                return new BestTransitPathCalculator(rbMap);
            }
        };

        anm = new ThreadLocal<AutoAndNonMotorizedSkimsCalculator>()
        {
            @Override
            protected AutoAndNonMotorizedSkimsCalculator initialValue()
            {
                return new AutoAndNonMotorizedSkimsCalculator(rbMap);
            }
        };
        wtw = new ThreadLocal<WalkTransitWalkSkimsCalculator>()
        {
            @Override
            protected WalkTransitWalkSkimsCalculator initialValue()
            {
                WalkTransitWalkSkimsCalculator w = new WalkTransitWalkSkimsCalculator();
                w.setup(rbMap, wtwSkimLogger, bestPathUEC.get());
                return w;
            }
        };
        wtd = new ThreadLocal<WalkTransitDriveSkimsCalculator>()
        {
            @Override
            protected WalkTransitDriveSkimsCalculator initialValue()
            {
                WalkTransitDriveSkimsCalculator w = new WalkTransitDriveSkimsCalculator();
                w.setup(rbMap, wtdSkimLogger, bestPathUEC.get());
                return w;
            }
        };
        dtw = new ThreadLocal<DriveTransitWalkSkimsCalculator>()
        {
            @Override
            protected DriveTransitWalkSkimsCalculator initialValue()
            {
                DriveTransitWalkSkimsCalculator w = new DriveTransitWalkSkimsCalculator();
                w.setup(rbMap, dtwSkimLogger, bestPathUEC.get());
                return w;
            }
        };
    }

    public void setTazDistanceSkimArrays(double[][][] storedFromTazDistanceSkims,
            double[][][] storedToTazDistanceSkims)
    {
        anm.get().setTazDistanceSkimArrays(storedFromTazDistanceSkims, storedToTazDistanceSkims);
    }

    public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator()
    {
        return anm.get();
    }

    public WalkTransitWalkSkimsCalculator getWtwSkimCalculator()
    {
        return wtw.get();
    }

    public WalkTransitDriveSkimsCalculator getWtdSkimCalculator()
    {
        return wtd.get();
    }

    public DriveTransitWalkSkimsCalculator getDtwSkimCalculator()
    {
        return dtw.get();
    }

    /**
     * This method finds the best walk-transit-walk tap pairs if they don't exist in the trip object (and sets the best tap pairs in the trip object),
     * then fills in the trip mode choice DMU object with the skim values corresponding to those tap pairs.
     * 
     * @param tour
     * @param trip
     * @param mcDmuObject
     */
    public void setTripMcDmuSkimAttributes(CrossBorderTour tour, CrossBorderTrip trip,
            CrossBorderTripModeChoiceDMU mcDmuObject)
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
            bestTapPairs = wtw.get().getBestTapPairs(origMgra, destMgra, outSkimPeriod, debug,
                    logger);
            trip.setBestWtwTapPairs(bestTapPairs);
        }

        setWtwTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, bestTapPairs,
                debug);

        setWtdTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, null, debug);

        setDtwTripMcDmuAttributes(mcDmuObject, origMgra, destMgra, departPeriod, null, debug);

    }

    /*
     * public double calculateTripMcLogsum(int origMgra, int destMgra, int departPeriod, boolean isInbound, ChoiceModelApplication mcModel,
     * TripModeChoiceDMU mcDmuObject, Logger myLogger) {
     * 
     * long currentTime = System.currentTimeMillis(); setTripMcDmuSkimAttributes( mcDmuObject, origMgra, destMgra, departPeriod, isInbound,
     * mcDmuObject.getDmuIndexValues().getDebug() ); setTripMcLogsumDmuAttributesTotalTime += ( System.currentTimeMillis() - currentTime );
     * 
     * // set the land use data items in the DMU for the origin mcDmuObject.setOrigDuDen( mgraManager.getDuDenValue( origMgra ) );
     * mcDmuObject.setOrigEmpDen( mgraManager.getEmpDenValue( origMgra ) ); mcDmuObject.setOrigTotInt( mgraManager.getTotIntValue( origMgra ) );
     * 
     * // set the land use data items in the DMU for the destination mcDmuObject.setDestDuDen( mgraManager.getDuDenValue( destMgra ) );
     * mcDmuObject.setDestEmpDen( mgraManager.getEmpDenValue( destMgra ) ); mcDmuObject.setDestTotInt( mgraManager.getTotIntValue( destMgra ) );
     * 
     * // mode choice UEC references highway skim matrices directly, so set index orig/dest to O/D TAZs. IndexValues mcDmuIndex =
     * mcDmuObject.getDmuIndexValues(); mcDmuIndex.setOriginZone(mgraManager.getTaz(origMgra)); mcDmuIndex.setDestZone(mgraManager.getTaz(destMgra));
     * 
     * // double dailyParkingCost = getPersonDailyParkingCost( mcDmuObject.getPersonObject(), destMgra ); // mcDmuObject.setDailyParkingCost(
     * dailyParkingCost ); // mcDmuObject.setHourlyParkingCost( lsWgtAvgCostH[destMgra] ); // mcDmuObject.setReimburseAmount( lsWgtAvgCostM[destMgra]
     * - dailyParkingCost );
     * 
     * 
     * mcModel.computeUtilities(mcDmuObject, mcDmuIndex); double logsum = mcModel.getLogsum(); tripModeChoiceSegmentStoredProbabilities =
     * Arrays.copyOf( mcModel.getCumulativeProbabilities(), mcModel.getNumberOfAlternatives() );
     * 
     * if ( mcDmuIndex.getDebug() ) mcModel.logUECResults(myLogger, "Trip Mode Choice Utility Expressions for mgras: " + origMgra + " to " + destMgra
     * + " for HHID: " + mcDmuIndex.getHHIndex() );
     * 
     * return logsum;
     * 
     * }
     */

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

    private void setNmTripMcDmuAttributes(CrossBorderTripModeChoiceDMU tripMcDmuObject,
            int origMgra, int destMgra, int departPeriod, boolean loggingEnabled)
    {

        double[] nmSkims = null;

        // non-motorized, outbound then inbound
        int skimPeriodIndex = ModelStructure.getSkimPeriodIndex(departPeriod);
        departPeriod = skimPeriodIndex;
        nmSkims = anm.get().getNonMotorizedSkims(origMgra, destMgra, departPeriod, loggingEnabled,
                autoSkimLogger);
        if (loggingEnabled)
            anm.get().logReturnedSkims(origMgra, destMgra, departPeriod, nmSkims,
                    "non-motorized trip mode choice skims", autoSkimLogger);

        int walkIndex = anm.get().getNmWalkTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedWalkTime(nmSkims[walkIndex]);

        int bikeIndex = anm.get().getNmBikeTimeSkimIndex();
        tripMcDmuObject.setNonMotorizedBikeTime(nmSkims[bikeIndex]);

    }

    private void setWtwTripMcDmuAttributes(CrossBorderTripModeChoiceDMU tripMcDmuObject,
            int origMgra, int destMgra, int departPeriod, int[][] bestTapPairs,
            boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = wtw.get().getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtw.get().getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtw.get().getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtw.get().getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtw.get().getNullTransitSkims(Modes.getTransitModeIndex("LB"));
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
            crSkims = wtw.get().getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtw.get().getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtw.get().getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtw.get().getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtw.get().getWalkTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtw.get().getNullTransitSkims(i);
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

    private void setWtdTripMcDmuAttributes(CrossBorderTripModeChoiceDMU tripMcDmuObject,
            int origMgra, int destMgra, int departPeriod, int[][] bestTapPairs,
            boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = wtd.get().getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = wtd.get().getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = wtd.get().getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = wtd.get().getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = wtd.get().getNullTransitSkims(Modes.getTransitModeIndex("LB"));
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
            crSkims = wtd.get().getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = wtd.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = wtd.get().getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = wtd.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = wtd.get().getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = wtd.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = wtd.get().getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = wtd.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findWalkTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findDriveTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = wtd.get().getWalkTransitDriveSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = wtd.get().getNullTransitSkims(i);
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

    private void setDtwTripMcDmuAttributes(CrossBorderTripModeChoiceDMU tripMcDmuObject,
            int origMgra, int destMgra, int departPeriod, int[][] bestTapPairs,
            boolean loggingEnabled)
    {

        double[] lbSkims = null;
        double[] ebSkims = null;
        double[] brSkims = null;
        double[] lrSkims = null;
        double[] crSkims = null;

        if (bestTapPairs == null)
        {
            crSkims = dtw.get().getNullTransitSkims(Modes.getTransitModeIndex("CR"));
            lrSkims = dtw.get().getNullTransitSkims(Modes.getTransitModeIndex("LR"));
            brSkims = dtw.get().getNullTransitSkims(Modes.getTransitModeIndex("BRT"));
            ebSkims = dtw.get().getNullTransitSkims(Modes.getTransitModeIndex("EB"));
            lbSkims = dtw.get().getNullTransitSkims(Modes.getTransitModeIndex("LB"));
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
            crSkims = dtw.get().getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            crSkims = dtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LR");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lrSkims = dtw.get().getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lrSkims = dtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("BRT");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            brSkims = dtw.get().getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            brSkims = dtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("EB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            ebSkims = dtw.get().getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            ebSkims = dtw.get().getNullTransitSkims(i);
        }

        i = Modes.getTransitModeIndex("LB");
        if (bestTapPairs[i] != null)
        {
            double pWalkTime = BestTransitPathCalculator.findDriveTransitAccessTime(origMgra,
                    bestTapPairs[i][0]);
            double aWalkTime = BestTransitPathCalculator.findWalkTransitEgressTime(destMgra,
                    bestTapPairs[i][1]);
            lbSkims = dtw.get().getDriveTransitWalkSkims(i, pWalkTime, aWalkTime,
                    bestTapPairs[i][0], bestTapPairs[i][1], departPeriod, loggingEnabled);
        } else
        {
            lbSkims = dtw.get().getNullTransitSkims(i);
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
