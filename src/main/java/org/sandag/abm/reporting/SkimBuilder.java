package org.sandag.abm.reporting;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoAndNonMotorizedSkimsCalculator;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;

/**
 * The {@code SkimBuilder} ...
 * 
 * @author crf Started 10/17/12 9:12 AM
 */
public class SkimBuilder
{
    private static final Logger                      logger                                 = Logger.getLogger(SkimBuilder.class);

    private static final int                         WALK_TIME_INDEX                        = 0;
    private static final int                         BIKE_TIME_INDEX                        = 0;

    private static final int                         DA_TIME_INDEX                          = 0;
    private static final int                         DA_FF_TIME_INDEX                       = 1;
    private static final int                         DA_DIST_INDEX                          = 2;
    private static final int                         DA_TOLL_TIME_INDEX                     = 3;
    private static final int                         DA_TOLL_FF_TIME_INDEX                  = 4;
    private static final int                         DA_TOLL_DIST_INDEX                     = 5;
    private static final int                         DA_TOLL_COST_INDEX                     = 6;
    private static final int                         DA_TOLL_TOLLDIST_INDEX                 = 7;
    private static final int                         SR2_TIME_INDEX                         = 8;
    private static final int                         SR2_FF_TIME_INDEX                      = 9;
    private static final int                         SR2_DIST_INDEX                         = 10;
    private static final int                         SR2_HOVDIST_INDEX                      = 11;
    private static final int                         SR2_TOLL_TIME_INDEX                    = 12;
    private static final int                         SR2_TOLL_FF_TIME_INDEX                 = 13;
    private static final int                         SR2_TOLL_DIST_INDEX                    = 14;
    private static final int                         SR2_TOLL_COST_INDEX                    = 15;
    private static final int                         SR2_TOLL_TOLLDIST_INDEX                = 16;
    private static final int                         SR3_TIME_INDEX                         = 17;
    private static final int                         SR3_FF_TIME_INDEX                      = 18;
    private static final int                         SR3_DIST_INDEX                         = 19;
    private static final int                         SR3_HOVDIST_INDEX                      = 20;
    private static final int                         SR3_TOLL_TIME_INDEX                    = 21;
    private static final int                         SR3_TOLL_FF_TIME_INDEX                 = 22;
    private static final int                         SR3_TOLL_DIST_INDEX                    = 23;
    private static final int                         SR3_TOLL_COST_INDEX                    = 24;
    private static final int                         SR3_TOLL_TOLLDIST_INDEX                = 25;
    private static final int                         DA_STD_INDEX                           = 26;
    private static final int                         DA_TOLL_STD_INDEX                      = 27;
    private static final int                         SR2_STD_INDEX                          = 28;
    private static final int                         SR2_TOLL_STD_INDEX                     = 29;
    private static final int                         SR3_STD_INDEX                          = 30;
    private static final int                         SR3_TOLL_STD_INDEX                     = 31;
    
    private static final int TRANSIT_SET_ACCESS_TIME_INDEX = 0;
    private static final int TRANSIT_SET_EGRESS_TIME_INDEX = 1;
    private static final int TRANSIT_SET_AUX_WALK_TIME_INDEX = 2;
     private static final int TRANSIT_SET_LOCAL_BUS_TIME_INDEX = 3;
    private static final int TRANSIT_SET_EXPRESS_BUS_TIME_INDEX = 4;
    private static final int TRANSIT_SET_BRT_TIME_INDEX = 5;
    private static final int TRANSIT_SET_LRT_TIME_INDEX = 6;
    private static final int TRANSIT_SET_CR_TIME_INDEX = 7;
    private static final int TRANSIT_SET_FIRST_WAIT_TIME_INDEX = 8;
    private static final int TRANSIT_SET_TRANSFER_WAIT_TIME_INDEX = 9;
    private static final int TRANSIT_SET_FARE_INDEX = 10;
    private static final int TRANSIT_SET_MAIN_MODE_INDEX = 11;
    private static final int TRANSIT_SET_XFERS_INDEX = 12;
    private static final int TRANSIT_SET_DIST_INDEX = 13;

     private static final double                      FEET_IN_MILE                           = 5280.0;

    private final TapDataManager                     tapManager;
    private final TazDataManager                     tazManager;
    private final MgraDataManager                    mgraManager;
    private final AutoTazSkimsCalculator             tazDistanceCalculator;
    private final AutoAndNonMotorizedSkimsCalculator autoNonMotSkims;
    private final WalkTransitWalkSkimsCalculator     wtw;
    private final WalkTransitDriveSkimsCalculator    wtd;
    private final DriveTransitWalkSkimsCalculator    dtw;
    
    private final String 			FUEL_COST_PROPERTY          = "aoc.fuel";
    private final String 			MAINTENANCE_COST_PROPERTY = "aoc.maintenance";
    private float autoOperatingCost;
    

    public SkimBuilder(Properties properties)
    {

        HashMap<String, String> rbMap = new HashMap<String, String>(
                (Map<String, String>) (Map) properties);
        tapManager = TapDataManager.getInstance(rbMap);
        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
        tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        autoNonMotSkims = new AutoAndNonMotorizedSkimsCalculator(rbMap);
        autoNonMotSkims.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        BestTransitPathCalculator bestPathUEC = new BestTransitPathCalculator(rbMap);
        wtw = new WalkTransitWalkSkimsCalculator(rbMap);
        wtw.setup(rbMap, logger, bestPathUEC);
        wtd = new WalkTransitDriveSkimsCalculator(rbMap);
        wtd.setup(rbMap, logger, bestPathUEC);
        dtw = new DriveTransitWalkSkimsCalculator(rbMap);
        dtw.setup(rbMap, logger, bestPathUEC);
        
        float fuelCost = new Float(properties.getProperty(FUEL_COST_PROPERTY));
        float mainCost = new Float(properties.getProperty(MAINTENANCE_COST_PROPERTY));
        autoOperatingCost = (fuelCost + mainCost)  * 0.01f;

    }

    // todo: hard coding these next two lookups because it is convenient, but
    // probably should move to a lookup file
    private final String[]         modeNameLookup   = {
            "UNKNOWN", // ids start at one
            "DRIVEALONEFREE", "DRIVEALONEPAY", "SHARED2HOV", "SHARED2PAY",
            "SHARED3HOV", "SHARED3PAY", "WALK", "BIKE", "WALK_SET", "PNR_SET", 
            "KNR_TRN", "TNC_TRN", "SCHBUS", "TAXI", "TNC_SINGLE", "TNC_SHARED"};

    private final TripModeChoice[] modeChoiceLookup = {TripModeChoice.UNKNOWN,
            TripModeChoice.DRIVE_ALONE_NO_TOLL, TripModeChoice.DRIVE_ALONE_TOLL,
            TripModeChoice.SR2_HOV, TripModeChoice.SR2_TOLL,
            TripModeChoice.SR3_HOV, TripModeChoice.SR3_TOLL,
            TripModeChoice.WALK, TripModeChoice.BIKE, TripModeChoice.WALK_SET,
            TripModeChoice.PNR_SET, TripModeChoice.KNR_SET, TripModeChoice.KNR_SET, 
            TripModeChoice.DRIVE_ALONE_NO_TOLL,
            TripModeChoice.SR2_HOV, TripModeChoice.SR2_HOV,  TripModeChoice.SR2_HOV       };

    private int getTod(int tripTimePeriod)
    {
        return ModelStructure.getSkimPeriodIndex(tripTimePeriod);
    }

    private int getStartTime(int tripTimePeriod)
    {
        return (tripTimePeriod - 1) * 30 + 270; // starts at 4:30 and goes half
                                                // hour intervals after that
    }

    public TripAttributes getTripAttributes(int origin, int destination, int tripModeIndex,
            int boardTap, int alightTap, int tripTimePeriod, boolean inbound, float valueOfTime, int set)
    {
        int tod = getTod(tripTimePeriod);
        TripModeChoice tripMode = modeChoiceLookup[tripModeIndex < 0 ? 0 : tripModeIndex];
       
        TripAttributes attributes = getTripAttributes(tripMode, origin, destination, boardTap,
                alightTap, tod, inbound, valueOfTime, set);
        attributes.setTripModeName(modeNameLookup[tripModeIndex < 0 ? 0 : tripModeIndex]);
        attributes.setTripStartTime(getStartTime(tripTimePeriod));
        return attributes;
    }

    private TripAttributes getTripAttributesUnknown()
    {
        return new TripAttributes(0,0,0,0,0,0,0,0,0,0,0,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
    }

    private final float DEFAULT_BIKE_SPEED = 12;
    private final float DEFAULT_WALK_SPEED = 3;

    private TripAttributes getTripAttributes(TripModeChoice modeChoice, int origin,
            int destination, int boardTap, int alightTap, int tod, boolean inbound, float vot, int set)
    {
        int timeIndex = -1;
        int distIndex = -1;
        int costIndex = -1;
        int stdIndex  = -1;
      
        switch (modeChoice)
        {
            case UNKNOWN:
                return getTripAttributesUnknown();
            case DRIVE_ALONE_NO_TOLL:
            {
                timeIndex = DA_TIME_INDEX;
                distIndex = DA_DIST_INDEX;
                costIndex = -1;
                stdIndex  = DA_STD_INDEX;
                double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex]);
            }
            case DRIVE_ALONE_TOLL:
            {
                timeIndex = DA_TOLL_TIME_INDEX;
                distIndex = DA_TOLL_DIST_INDEX;
                costIndex = DA_TOLL_COST_INDEX;
                stdIndex  = DA_TOLL_STD_INDEX;
               double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                
                //if IVT for toll is zero, get non-toll skim
                if(autoSkims[timeIndex]==0){
                    getTripAttributes(TripModeChoice.DRIVE_ALONE_NO_TOLL, origin,
                            destination, boardTap,  alightTap,  tod,  inbound,  vot, set);
                }
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex], autoSkims[costIndex]);
            }
            case SR2_HOV: // wu added
            {
                timeIndex = SR2_TIME_INDEX;
                distIndex = SR2_DIST_INDEX;
                costIndex = -1;
                stdIndex  = SR2_STD_INDEX;               
                double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                //if IVT for HOV is zero, get non-toll skim
                if(autoSkims[timeIndex]==0){
                    getTripAttributes(TripModeChoice.DRIVE_ALONE_NO_TOLL, origin,
                            destination, boardTap,  alightTap,  tod,  inbound,  vot, set);
                }
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex]);
                            }
            case SR2_TOLL: // wu added
            {
                timeIndex = SR2_TOLL_TIME_INDEX;
                distIndex = SR2_TOLL_DIST_INDEX;
                costIndex = SR2_TOLL_COST_INDEX;
                stdIndex  = SR2_TOLL_STD_INDEX;
                double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                //if IVT for toll is zero, get non-toll HOV skim
                if(autoSkims[timeIndex]==0){
                    getTripAttributes(TripModeChoice.SR2_HOV, origin,
                            destination, boardTap,  alightTap,  tod,  inbound,  vot, set);
                }
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex], autoSkims[costIndex]);
                            }
            case SR3_HOV: // wu added
            {
                timeIndex = SR3_TIME_INDEX;
                distIndex = SR3_DIST_INDEX;
                costIndex = -1;
                stdIndex  = SR2_STD_INDEX;
                double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                //if IVT for HOV is zero, get non-HOV skim
                if(autoSkims[timeIndex]==0){
                    getTripAttributes(TripModeChoice.DRIVE_ALONE_NO_TOLL, origin,
                            destination, boardTap,  alightTap,  tod,  inbound,  vot, set);
                }
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex]);
            }
            case SR3_TOLL:
            {
                timeIndex = SR3_TOLL_TIME_INDEX;
                distIndex = SR3_TOLL_DIST_INDEX;
                costIndex = SR3_TOLL_COST_INDEX;
                stdIndex  = SR3_TOLL_STD_INDEX;
                double[] autoSkims = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger);
                //if IVT for toll is zero, get non-toll HOV skim
                if(autoSkims[timeIndex]==0){
                    getTripAttributes(TripModeChoice.SR3_HOV, origin,
                            destination, boardTap,  alightTap,  tod,  inbound,  vot, set);
                }
                return new TripAttributes(autoSkims[timeIndex], autoSkims[distIndex], autoSkims[distIndex]*autoOperatingCost, autoSkims[stdIndex], autoSkims[costIndex]);
            }
            case WALK:
            {
                // first, look in mgra manager, otherwise default to auto skims
                double distance = mgraManager.getMgraToMgraWalkDistFrom(origin, destination) / FEET_IN_MILE;
                double time =0;
                if (distance > 0)
                {
                    time = mgraManager.getMgraToMgraWalkTime(origin, destination);
                }else{
                	distance = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false, logger)[DA_DIST_INDEX];
                	time = distance * 60 / DEFAULT_WALK_SPEED;
                }
                return new TripAttributes(0, 0, 0, 0, 0, 0, 0, 0, time, 0, distance, -1, -1, 0,0,0,0,0,0,0,0,0,0,0,0,0,0);
            }
            case BIKE:
            {
                double time = mgraManager.getMgraToMgraBikeTime(origin, destination);
                double distance = 0;
                if (time > 0)
                {
                    distance = time * DEFAULT_BIKE_SPEED / 60;
                   
                }else{
                	distance = autoNonMotSkims.getAutoSkims(origin, destination, tod, vot,false,
                        logger)[DA_DIST_INDEX];
                	time = distance * 60 / DEFAULT_BIKE_SPEED;
                }
                return new TripAttributes(0, 0, 0, 0, 0, 0, 0, 0, 0, time, distance, -1, -1, 0,0,0,0,0,0,0,0,0,0,0,0,0,0);
            }
            case WALK_SET : 
            case PNR_SET : 
            case KNR_SET : {
                boolean isDrive = modeChoice.isDrive;
                double walkTime = 0.0;
                double driveTime = 0.0;
  
                double[] skims;
                int boardTaz = -1;
                int alightTaz = -1;
                double boardAccessTime = 0.0;
                double alightEgressTime = 0.0;
                double accessDistance = 0.0;
                double egressDistance = 0.0;
                int originTaz = mgraManager.getTaz(origin);
                int destTaz = mgraManager.getTaz(destination);
                if (isDrive) { 
                    if (!inbound) { //outbound: drive to transit stop at origin, then transit to destination
                        boardAccessTime = tazManager.getTimeToTapFromTaz(originTaz,boardTap,( modeChoice==TripModeChoice.PNR_SET ? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                        accessDistance = tazManager.getDistanceToTapFromTaz(originTaz,boardTap,( modeChoice==TripModeChoice.PNR_SET ? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                        alightEgressTime = mgraManager.getWalkTimeFromMgraToTap(destination,alightTap);
                        egressDistance = mgraManager.getWalkDistanceFromMgraToTap(destination,alightTap);
                        
                        if (boardAccessTime ==-1) {
                            logger.info("Error: TAP not accessible from origin TAZ by "+ (modeChoice==TripModeChoice.PNR_SET ? "PNR" : "KNR" )+" access");
                            logger.info("mc: " + modeChoice);
                            logger.info("origin MAZ: " + origin);
                            logger.info("origin TAZ" + originTaz);
                            logger.info("dest MAZ: " + destination);
                            logger.info("board tap: " + boardTap);
                            logger.info("alight tap: " + alightTap);
                            logger.info("tod: " + tod);
                            logger.info("inbound: " + inbound);
                            logger.info("set: " + set);
                        } 
                        
                        if (alightEgressTime == -1){
                            logger.info("Error: TAP not accessible from destination MAZ by walk access");
                            logger.info("mc: " + modeChoice);
                            logger.info("origin MAZ: " + origin);
                            logger.info("origin TAZ" + originTaz);
                            logger.info("dest MAZ: " + destination);
                            logger.info("board tap: " + boardTap);
                            logger.info("alight tap: " + alightTap);
                            logger.info("tod: " + tod);
                            logger.info("inbound: " + inbound);
                            logger.info("set: " + set);
                       	
                        }
                        skims = dtw.getDriveTransitWalkSkims(set,boardAccessTime,alightEgressTime,boardTap,alightTap,tod,false);
                        walkTime = alightEgressTime;
                        driveTime= boardAccessTime;
                        
                    } else { //inbound: transit from origin to destination, then drive
                        boardAccessTime = mgraManager.getWalkTimeFromMgraToTap(origin,boardTap);
                        accessDistance = mgraManager.getWalkDistanceFromMgraToTap(origin,boardTap);
                        alightEgressTime = tazManager.getTimeToTapFromTaz(destTaz,alightTap,( modeChoice==TripModeChoice.PNR_SET ? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                        egressDistance = tazManager.getDistanceToTapFromTaz(destTaz,alightTap,( modeChoice==TripModeChoice.PNR_SET ? Modes.AccessMode.PARK_N_RIDE : Modes.AccessMode.KISS_N_RIDE));
                        if (boardAccessTime ==-1) {
                            logger.info("Error: TAP not accessible from origin MAZ by walk access");
                            logger.info("mc: " + modeChoice);
                            logger.info("origin MAZ: " + origin);
                            logger.info("origin TAZ" + originTaz);
                            logger.info("dest MAZ: " + destination);
                            logger.info("board tap: " + boardTap);
                            logger.info("alight tap: " + alightTap);
                            logger.info("tod: " + tod);
                            logger.info("inbound: " + inbound);
                            logger.info("set: " + set);
                        } 
                        
                        if (alightEgressTime == -1){
                            logger.info("Error: TAP not accessible from destination TAZ by "+ (modeChoice==TripModeChoice.PNR_SET ? "PNR" : "KNR" )+" access");
                            logger.info("mc: " + modeChoice);
                            logger.info("origin MAZ: " + origin);
                            logger.info("origin TAZ" + originTaz);
                            logger.info("dest MAZ: " + destination);
                            logger.info("board tap: " + boardTap);
                            logger.info("alight tap: " + alightTap);
                            logger.info("tod: " + tod);
                            logger.info("inbound: " + inbound);
                            logger.info("set: " + set);
                       	
                        }
                   skims = wtd.getWalkTransitDriveSkims(set,boardAccessTime,alightEgressTime,boardTap,alightTap,tod,false);
                   walkTime = boardAccessTime ;
                   driveTime= alightEgressTime;
                    }
                } else {
                    int bt = mgraManager.getTapPosition(origin,boardTap);
                    int at = mgraManager.getTapPosition(destination,alightTap);
                    if (bt < 0 || at < 0) {
                        logger.info("bad tap position: " + bt + "  " + at);
                        logger.info("mc: " + modeChoice);
                        logger.info("origin: " + origin);
                        logger.info("dest: " + destination);
                        logger.info("board tap: " + boardTap);
                        logger.info("alight tap: " + alightTap);
                        logger.info("tod: " + tod);
                        logger.info("inbound: " + inbound);
                        logger.info("set: " + set);
                        logger.info("board tap position: " + bt);
                        logger.info("alight tap position: " + at);
                    } else {
                        boardAccessTime = mgraManager.getMgraToTapWalkTime(origin,bt);
                        accessDistance = mgraManager.getWalkDistanceFromMgraToTap(origin,boardTap);
                        alightEgressTime = mgraManager.getMgraToTapWalkTime(destination,at);
                        egressDistance = mgraManager.getWalkDistanceFromMgraToTap(destination,alightTap);
                    }
                    walkTime = boardAccessTime + alightEgressTime;
                    skims = wtw.getWalkTransitWalkSkims(set,boardAccessTime,alightEgressTime,boardTap,alightTap,tod,false);
                }

                double transitInVehicleTime = 0.0;
                
                transitInVehicleTime += skims[TRANSIT_SET_CR_TIME_INDEX];
                transitInVehicleTime += skims[TRANSIT_SET_LRT_TIME_INDEX];
                transitInVehicleTime += skims[TRANSIT_SET_BRT_TIME_INDEX];
                transitInVehicleTime += skims[TRANSIT_SET_EXPRESS_BUS_TIME_INDEX];
                transitInVehicleTime += skims[TRANSIT_SET_LOCAL_BUS_TIME_INDEX];
                
                double crTime = skims[TRANSIT_SET_CR_TIME_INDEX];
                double lrtTime = skims[TRANSIT_SET_LRT_TIME_INDEX];
                double brtTime = skims[TRANSIT_SET_BRT_TIME_INDEX];
                double expTime = skims[TRANSIT_SET_EXPRESS_BUS_TIME_INDEX];
                double locTime = skims[TRANSIT_SET_LOCAL_BUS_TIME_INDEX];
                
                //wsu 9/17/18, walkTime already set
                //walkTime += skims[TRANSIT_SET_ACCESS_TIME_INDEX];
                //walkTime += skims[TRANSIT_SET_EGRESS_TIME_INDEX ];
                walkTime += skims[TRANSIT_SET_AUX_WALK_TIME_INDEX];
                
                double auxiliaryTime = skims[TRANSIT_SET_AUX_WALK_TIME_INDEX];
                
                double waitTime = 0.0;
                waitTime += skims[TRANSIT_SET_FIRST_WAIT_TIME_INDEX];
                waitTime += skims[TRANSIT_SET_TRANSFER_WAIT_TIME_INDEX];
                
                double transfers = skims[TRANSIT_SET_XFERS_INDEX];
                
                double transitFare = 0.0;
                transitFare += skims[TRANSIT_SET_FARE_INDEX];
 
                double transitDist = skims[TRANSIT_SET_DIST_INDEX];
                /*
                int modeIndex = 0;
                for(modeIndex = TRANSIT_SET_LOCAL_BUS_TIME_INDEX; modeIndex <= TRANSIT_SET_CR_TIME_INDEX; modeIndex++){
                	if(skims[modeIndex] > 0)
                		break;
                }
                */
                double dist = autoNonMotSkims.getAutoSkims(origin,destination,tod,vot,false,logger)[DA_DIST_INDEX];  //todo: is this correct enough?
                return new TripAttributes(driveTime, driveTime/60*35*autoOperatingCost, 0, 0,  transitInVehicleTime, 
                		waitTime, walkTime, transitFare, 0, 0, dist, boardTaz, alightTaz, vot, set,
                		accessDistance,egressDistance,auxiliaryTime,boardAccessTime,alightEgressTime,transfers,locTime,expTime,brtTime,lrtTime,crTime,transitDist);
            }
            default:
                throw new IllegalStateException("Should not be here: " + modeChoice);
        }
    }

    public static enum TripModeChoice
    {
        UNKNOWN(false,false), 
        DRIVE_ALONE_NO_TOLL(true,false), 
        DRIVE_ALONE_TOLL(true,true), 
        SR2_HOV(true,false), 
        SR2_TOLL(true,true), 
        SR3_HOV(true,false), 
        SR3_TOLL(true,true), 
        WALK(false,false), 
        BIKE(false,false), 
        WALK_SET(false, false), 
        PNR_SET(true, false),
        KNR_SET(true, false);
       
        private final boolean isDrive;
        private final boolean isToll;

        private TripModeChoice(boolean drive, boolean toll)
        {
            isDrive = drive;
            isToll = toll;
        }

    }

    public static class TripAttributes
    {
        private final float autoInVehicleTime;
        private final float autoOperatingCost;
        private final float autoStandardDeviationTime;
        private final float autoTollCost;
        private final float transitInVehicleTime;
        private final float transitWaitTime;
        private final float transitWalkTime;
        private final float transitFare;
        private final float walkModeTime;
        private final float bikeModeTime;
        private final float tripDistance;
        private final int   tripBoardTaz;
        private final int   tripAlightTaz;
        private final int   set;
        private final float valueOfTime;
        private final float transitAccessDistance;
        private final float transitEgressDistance;
        private final float transitAuxiliaryTime;
        private final float transitAccessTime;
        private final float transitEgressTime;
        private final float transitTransfers;
        private final float locTime;
        private final float expTime;
        private final float brtTime;
        private final float lrtTime;
        private final float crTime;
        private final float transitDistance;
    
        private String      tripModeName;

        public int getTripStartTime()
        {
            return tripStartTime;
        }

        public void setTripStartTime(int tripStartTime)
        {
            this.tripStartTime = tripStartTime;
        }

        private int tripStartTime;

        public TripAttributes(double autoInVehicleTime, double autoOperatingCost, double autoStandardDeviationTime, double autoTollCost, double transitInVehicleTime, 
        		double transitWaitTime, double transitWalkTime, double transitFare, double walkModeTime, double bikeModeTime, double tripDistance,
        		int tripBoardTaz, int tripAlightTaz, float valueOfTime, int set, double accessDistance,
        		double egressDistance, double auxiliaryTime, double accessTime,double egressTime, double transfers, double locTime, double expTime, double brtTime, double lrtTime, double crTime, double trnDist)
        {
            this.autoInVehicleTime = (float) autoInVehicleTime;
            this.autoOperatingCost = (float)  autoOperatingCost;
            this.autoStandardDeviationTime = (float) autoStandardDeviationTime;
            this.autoTollCost = (float) autoTollCost;
            this.transitInVehicleTime = (float) transitInVehicleTime;
            this.transitWaitTime = (float) transitWaitTime;
            this.transitWalkTime = (float) transitWalkTime;
            this.transitFare = (float) transitFare;
            this.walkModeTime = (float) walkModeTime;
            this.bikeModeTime = (float) bikeModeTime;
            this.tripDistance = (float) tripDistance;
            this.tripBoardTaz = tripBoardTaz;
            this.tripAlightTaz = tripAlightTaz;
            this.set = set;
            this.valueOfTime = valueOfTime;
            this.transitAccessDistance = (float) accessDistance;
            this.transitEgressDistance = (float) egressDistance;
            this.transitAuxiliaryTime = (float) auxiliaryTime;
            this.transitAccessTime = (float) accessTime;
            this.transitEgressTime = (float) egressTime;
            this.transitTransfers = (float) transfers;
            this.locTime = (float) locTime;
            this.expTime = (float) expTime;
            this.brtTime = (float) brtTime;
            this.lrtTime = (float) lrtTime;
            this.crTime = (float) crTime;
            this.transitDistance = (float) trnDist;
            
        }
        
        
         /**
         * A method to set create trip attributes for a non-toll auto choice.
         * 
         * @param autoInVehicleTime
         * @param tripDistance
         * @param autoOperatingCost
         */
        public TripAttributes(double autoInVehicleTime, double tripDistance, double autoOperatingCost, double stdDevTime)
        {
            this(autoInVehicleTime, autoOperatingCost, stdDevTime, 0,0,0,0,0,0,0,tripDistance,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
        }

        /**
         * A method to create trip attributes for a toll auto choice.
         * 
         * @param autoInVehicleTime
         * @param tripDistance
         * @param autoOperatingCost
         * @param tollCost
         */
        public TripAttributes(double autoInVehicleTime, double tripDistance, double autoOperatingCost, double stdDevTime, double tollCost)
        {
            this(autoInVehicleTime, autoOperatingCost, stdDevTime, tollCost,0,0,0,0,0,0,tripDistance,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
        }
       
         
        
        public void setTripModeName(String tripModeName)
        {
            this.tripModeName = tripModeName;
        }

        public float getAutoInVehicleTime() {
			return autoInVehicleTime;
		}

		public float getAutoOperatingCost() {
			return autoOperatingCost;
		}

		public float getAutoStandardDeviationTime() {
			return autoStandardDeviationTime;
		}

		public float getAutoTollCost() {
			return autoTollCost;
		}

		public float getTransitInVehicleTime() {
			return transitInVehicleTime;
		}

		public float getTransitWaitTime() {
			return transitWaitTime;
		}

		public float getTransitFare() {
			return transitFare;
		}

		public float getTransitWalkTime() {
			return transitWalkTime;
		}

		public float getWalkModeTime() {
			return walkModeTime;
		}

		public float getBikeModeTime() {
			return bikeModeTime;
		}

		public float getTripDistance() {
			return tripDistance;
		}

		public String getTripModeName()
        {
            return tripModeName;
        }

        public int getTripBoardTaz()
        {
            return tripBoardTaz;
        }

        public int getTripAlightTaz()
        {
            return tripAlightTaz;
        }

		public float getValueOfTime() {
			return valueOfTime;
		}

		public int getSet() {
			return set;
		}

		public float getTransitAccessDistance() {
			return transitAccessDistance;
		}

		public float getTransitEgressDistance() {
			return transitEgressDistance;
		}

		public float getTransitAuxiliaryTime() {
			return transitAuxiliaryTime;
		}

		public float getTransitAccessTime() {
			return transitAccessTime;
		}

		public float getTransitEgressTime() {
			return transitEgressTime;
		}
		
		public float getTransitTransfers() {
			return transitTransfers;
		}

		public float getLocTime() {
			return locTime;
		}

		public float getExpTime() {
			return expTime;
		}

		public float getBrtTime() {
			return brtTime;
		}

		public float getLrtTime() {
			return lrtTime;
		}

		public float getCrTime() {
			return crTime;
		}
		
		public float getTransitDistance(){
			return transitDistance;
		}
    }
    
    public float getLotWalkTime(int parkingLotMaz, int destinationMaz) {

    	// first, look in mgra manager, otherwise default to auto skims
        double distance = mgraManager.getMgraToMgraWalkDistFrom(parkingLotMaz, destinationMaz) / FEET_IN_MILE;
        if (distance <= 0) {
        	distance = autoNonMotSkims.getAutoSkims(parkingLotMaz, destinationMaz, SandagModelStructure.EA_SKIM_PERIOD_INDEX +1, (float)15.0,false, logger)[DA_DIST_INDEX];
        }
       	
       	return (float) (distance * 60 / DEFAULT_WALK_SPEED);
    }


}
