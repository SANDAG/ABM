/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.util.Tracer;

import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.Modes.AccessMode;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;
import org.sandag.abm.ctramp.Util;

import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.newmodel.Alternative;
import com.pb.common.newmodel.ConcreteAlternative;
import com.pb.common.newmodel.LogitModel;
/**
 * WalkPathUEC calculates the best walk-transit utilities for a given MGRA pair.
 * 
 * @author Joel Freedman
 * @version 1.0, May 2009
 */
public class BestTransitPathCalculator implements Serializable
{

    private transient Logger                 logger        = Logger.getLogger(BestTransitPathCalculator.class);

    //TODO: combine APP_TYPE_xxx constants into a enum structure
    public static final int              APP_TYPE_GENERIC = 0;
    public static final int              APP_TYPE_TOURMC  = 1;
    public static final int              APP_TYPE_TRIPMC  = 2;

    private static final int              EA                            = ModelStructure.EA_SKIM_PERIOD_INDEX;
    private static final int              AM                            = ModelStructure.AM_SKIM_PERIOD_INDEX;
    private static final int              MD                            = ModelStructure.MD_SKIM_PERIOD_INDEX;
    private static final int              PM                            = ModelStructure.PM_SKIM_PERIOD_INDEX;
    private static final int              EV                            = ModelStructure.EV_SKIM_PERIOD_INDEX;
    public static final int              NUM_PERIODS                   = ModelStructure.SKIM_PERIOD_INDICES.length;

    public static final float               NA            = -999;
    public static final int               WTW           = 0;
    public static final int               WTD           = 1;
    public static final int               DTW           = 2;
    public static final int[]             ACC_EGR       = {WTW,WTD,DTW};
    public static final int               NUM_ACC_EGR   = ACC_EGR.length;
    public static final String[]          ACC_EGR_STRING = {"WTW","WTD","DTW"};

    // seek and trace
    private boolean                       trace;
    private int[]                         traceOtaz;
    private int[]                         traceDtaz;
    protected Tracer                      tracer;

    private TazDataManager                tazManager;
    private TapDataManager                tapManager;
    private MgraDataManager               mgraManager;

    private int                           maxMgra;
    private int                           maxTap;
    private int                           maxTaz;

    // piece-wise utilities are being computed
    private UtilityExpressionCalculator   walkAccessUEC;
    private UtilityExpressionCalculator   walkEgressUEC;
    private UtilityExpressionCalculator   driveAccessUEC;
    private UtilityExpressionCalculator   driveEgressUEC;
    private UtilityExpressionCalculator   tapToTapUEC;

    // utility data cache for each transit path segment 
    private StoredUtilityData storedDataObject; //Encapsulates data shared by the BestTransitPathCalculator objects created for each hh choice model object
    // note that access/egress utilities are independent of transit skim set 
    private float[][]                  storedWalkAccessUtils;	// references StoredUtilityData.storedWalkAccessUtils
    private float[][]                  storedDriveAccessUtils;// references StoredUtilityData.storedDriveAccessUtils
    private float[][]                  storedWalkEgressUtils;	// references StoredUtilityData.storedWalkEgressUtils
    private float[][]                  storedDriveEgressUtils;// references StoredUtilityData.storedDriveEgressUtils    
    private HashMap<Integer,HashMap<Integer,ConcurrentHashMap<Long,float[]>>> storedDepartPeriodTapTapUtils; //references StoredUtilityData.storedDepartPeriodTapTapUtils

    private IndexValues                   index         = new IndexValues();

    // arrays storing information about the n (array length) best paths
    private double[]                      bestUtilities;
    private double[]                      bestAccessUtilities;
    private double[]                      bestEgressUtilities;
    private int[]                         bestPTap;
    private int[]                         bestATap;
    private int[]                         bestSet;  //since two of the best paths can be in the same set, need to store set as well   
    
    private int numSkimSets;
    private int numTransitAlts;
    private int[] maxLogsumUtilitiesBySkimSet;				//maximum number of utilities for each skims set in logsum calcs
    private int[] utilityCount;								//counter for utilities    
    private double[] expUtilities;							//exponentiated utility array for path choice
      
    private float nestingCoefficient;
    /**
     * Constructor.
     * 
     * @param rbMap HashMap<String, String>
     * @param UECFileName The path/name of the UEC containing the walk-transit model.
     * @param modelSheet The sheet (0-indexed) containing the model specification.
     * @param dataSheet The sheet (0-indexed) containing the data specification.
     */
    public BestTransitPathCalculator(HashMap<String, String> rbMap)
    {

        // read in resource bundle properties
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if ( trace )
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                 }
            }
        }
        

        String uecPath = Util.getStringValueFromPropertyMap(rbMap,CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = Paths.get(uecPath,rbMap.get("utility.bestTransitPath.uec.file")).toString();

        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.data.page");

        int walkAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkAccess.page");
        int driveAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveAccess.page");
        int walkEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkEgress.page");
        int driveEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveEgress.page");
        int tapToTapPage = Util.getIntegerValueFromPropertyMap( rbMap, 
        		"utility.bestTransitPath.tapToTap.page" );
        
        File uecFile = new File(uecFileName);
        walkAccessUEC = createUEC(uecFile, walkAccessPage, dataPage, rbMap, new TransitWalkAccessDMU());
        driveAccessUEC = createUEC(uecFile, driveAccessPage, dataPage, rbMap, new TransitDriveAccessDMU());
        walkEgressUEC = createUEC(uecFile, walkEgressPage, dataPage, rbMap, new TransitWalkAccessDMU());
        driveEgressUEC = createUEC(uecFile, driveEgressPage, dataPage, rbMap, new TransitDriveAccessDMU());
        tapToTapUEC = createUEC(uecFile, tapToTapPage, dataPage, rbMap, new TransitWalkAccessDMU());
        
        mgraManager = MgraDataManager.getInstance(rbMap);
        tazManager = TazDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);

        maxMgra = mgraManager.getMaxMgra();
        maxTap = mgraManager.getMaxTap();
        maxTaz = tazManager.getMaxTaz();

        // these arrays are shared by the BestTransitPathCalculator objects created for each hh choice model object
        storedDataObject = StoredUtilityData.getInstance( maxMgra, maxTap, maxTaz, ACC_EGR, ModelStructure.PERIODCODES);
        storedWalkAccessUtils = storedDataObject.getStoredWalkAccessUtils();
        storedDriveAccessUtils = storedDataObject.getStoredDriveAccessUtils();
        storedWalkEgressUtils = storedDataObject.getStoredWalkEgressUtils();
        storedDriveEgressUtils = storedDataObject.getStoredDriveEgressUtils();
        storedDepartPeriodTapTapUtils = storedDataObject.getStoredDepartPeriodTapTapUtils();
        
        //setup arrays
        numSkimSets = Util.getIntegerValueFromPropertyMap( rbMap, "utility.bestTransitPath.skim.sets" );
        numTransitAlts = Util.getIntegerValueFromPropertyMap( rbMap, "utility.bestTransitPath.alts" );
        
        bestUtilities = new double[numTransitAlts];
        bestPTap = new int[numTransitAlts];
        bestATap = new int[numTransitAlts];
        bestSet = new int[numTransitAlts];
        maxLogsumUtilitiesBySkimSet = Util.getIntegerArrayFromPropertyMap(rbMap, "utility.bestTransitPath.maxPathsPerSkimSetForLogsum");
        utilityCount = new int[numSkimSets];
        expUtilities = new double[numTransitAlts];
        
        nestingCoefficient =  new Float(Util.getStringValueFromPropertyMap(rbMap, "utility.bestTransitPath.nesting.coeff")).floatValue();
        
     }
    
   

    /**
     * This is the main method that finds the best N TAP-pairs. It
     * cycles through walk TAPs at the origin end (associated with the origin MGRA)
     * and alighting TAPs at the destination end (associated with the destination
     * MGRA) and calculates a utility for every available alt for each TAP
     * pair. It stores the N origin and destination TAP that had the best utility.
     * 
     * @param pMgra The origin/production MGRA.
     * @param aMgra The destination/attraction MGRA.
     * 
     */
    public void findBestWalkTransitWalkTaps(TransitWalkAccessDMU walkDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);

        int[] pMgraSet = mgraManager.getMgraWlkTapsDistArray()[pMgra][0];
        int[] aMgraSet = mgraManager.getMgraWlkTapsDistArray()[aMgra][0];

        if (pMgraSet == null || aMgraSet == null)
        {
            return;
        }

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        boolean writeCalculations = false;
        if ((tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz))|| debug)
        {
            writeCalculations = true;
        }

        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();
        
        for (int pTap : pMgraSet)
        {

            // Calculate the pMgra to pTap walk access utility values
            float accUtil; 
            if (storedWalkAccessUtils[pMgra][pTap] == StoredUtilityData.default_utility) {
    			accUtil = calcWalkAccessUtility(walkDmu, pMgra, pTap, writeCalculations, myLogger);
    			storedWalkAccessUtils[pMgra][pTap] = accUtil;
            } else {
            	accUtil = storedWalkAccessUtils[pMgra][pTap];
            	if(writeCalculations){
            		myLogger.info("Stored walk access utility from Mgra "+pMgra+" to Tap "+pTap+" is "+accUtil);
            	}
            }

            for (int aTap : aMgraSet)
            {
                
                // Calculate the aTap to aMgra walk egress utility values
                float egrUtil;
                if (storedWalkEgressUtils[aTap][aMgra] == StoredUtilityData.default_utility) {
                	egrUtil = calcWalkEgressUtility(walkDmu, aTap, aMgra, writeCalculations, myLogger);
        			storedWalkEgressUtils[aTap][aMgra] = egrUtil;
                } else {
                	egrUtil = storedWalkEgressUtils[aTap][aMgra];	
                	if(writeCalculations){
                		myLogger.info("Stored walk egress utility from Tap "+aTap+" to Mgra "+aMgra+" is "+egrUtil);
                	}
                }
                	
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets];
        		if(!storedDepartPeriodTapTapUtils.get(WTW).get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			
        			//loop across number of skim sets  the pTap to aTap utility values 
        			for (int set=0; set<numSkimSets; set++) {
	            		tapTapUtil[set] = calcUtilitiesForTapPair(walkDmu, period, WTW, pTap, aTap, set, pMgra, aMgra, writeCalculations, myLogger);
        			}
        			storedDepartPeriodTapTapUtils.get(WTW).get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(WTW).get(period).get(storedDataObject.paTapKey(pTap, aTap));
	            	if(writeCalculations)
	        			for (int set=0; set<numSkimSets; set++)
	        				myLogger.info("Stored WTW tap to tap utility set "+set+" from Tap "+pTap+" to Tap "+aTap+" is "+tapTapUtil[set]);
	       	}
        		
        		//create path for each skim set
        		for (int set=0; set<numSkimSets; set++) {
        			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, set, WTW, accUtil, tapTapUtil[set], egrUtil));
            	}
            
            }
        }
        
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
    }

    public void findBestDriveTransitWalkTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        if (tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode) == null
                || mgraManager.getMgraWlkTapsDistArray()[aMgra][0] == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if ((tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz)) || debug)
        {
            writeCalculations = true;
        }
        
        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();

        float[][][] tapParkingInfo = tapManager.getTapParkingInfo();

        int[] pTapArray = tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode);
        for ( int pTap : pTapArray )
        {
            // Calculate the pTaz to pTap drive access utility values
            float accUtil;
            if (storedDriveAccessUtils[pTaz][pTap] == StoredUtilityData.default_utility) {
    			accUtil = calcDriveAccessUtility(driveDmu, pMgra, pTaz, pTap, accMode, writeCalculations, myLogger);
    			storedDriveAccessUtils[pTaz][pTap] = accUtil;
            } else {
            	accUtil = storedDriveAccessUtils[pTaz][pTap];
        		if(writeCalculations)
        			myLogger.info("Stored drive access utility from TAZ "+pTaz+" to Tap "+pTap+" is "+accUtil);
            }
            
            int lotID = (int)tapParkingInfo[pTap][0][0]; // lot ID
            float lotCapacity = tapParkingInfo[pTap][2][0]; // lot capacity
            
            if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                    || (accMode == AccessMode.KISS_N_RIDE))
            {

                for (int aTap : mgraManager.getMgraWlkTapsDistArray()[aMgra][0])
                {
                    
                    // Calculate the aTap to aMgra walk egress utility values
                    float egrUtil;
                    if (storedWalkEgressUtils[aTap][aMgra] == StoredUtilityData.default_utility) {
            			egrUtil = calcWalkEgressUtility(walkDmu, aTap, aMgra, writeCalculations, myLogger);
            			storedWalkEgressUtils[aTap][aMgra] = egrUtil;
                    } else {
                    	egrUtil = storedWalkEgressUtils[aTap][aMgra];	
                    	if(writeCalculations){
                    		myLogger.info("Stored walk egress utility from Tap "+aTap+" to Mgra "+aMgra+" is "+egrUtil);
                    	}
                    }                
                    // Calculate the pTap to aTap utility values
            		float tapTapUtil[] = new float[numSkimSets];
            		if(!storedDepartPeriodTapTapUtils.get(DTW).get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
            			
            			//loop across number of skim sets  the pTap to aTap utility values 
            			for (int set=0; set<numSkimSets; set++) {
    	            		tapTapUtil[set] = calcUtilitiesForTapPair(walkDmu, period, DTW, pTap, aTap, set, pMgra, aMgra, writeCalculations, myLogger);
            			}
            			storedDepartPeriodTapTapUtils.get(DTW).get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
            		} else {
    	                tapTapUtil = storedDepartPeriodTapTapUtils.get(DTW).get(period).get(storedDataObject.paTapKey(pTap, aTap));
    	            	if(writeCalculations)
    	        			for (int set=0; set<numSkimSets; set++)
    	        				myLogger.info("Stored DTW tap to tap utility set "+set+" from Tap "+pTap+" to Tap "+aTap+" is "+tapTapUtil[set]);
            		}
            		//create path for each skim set
            		for (int set=0; set<numSkimSets; set++) {
            			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, set, DTW, accUtil, tapTapUtil[set], egrUtil));
                	}
            		
                }
            }
            
            //save N best paths
            trimPaths(paths);
            if (writeCalculations) {
                logBestUtilities(myLogger);
            }
        }
    }

    public void findBestWalkTransitDriveTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int pTaz = mgraManager.getTaz(pMgra);
        int aTaz = mgraManager.getTaz(aMgra);

        if (mgraManager.getMgraWlkTapsDistArray()[pMgra][0] == null
                || tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode) == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }

        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();
        
        for (int pTap : mgraManager.getMgraWlkTapsDistArray()[pMgra][0])
        {
            // Calculate the pMgra to pTap walk access utility values
            float accUtil;
            if (storedWalkAccessUtils[pMgra][pTap] == StoredUtilityData.default_utility) {
    			accUtil = calcWalkAccessUtility(walkDmu, pMgra, pTap, writeCalculations, myLogger);
    			storedWalkAccessUtils[pMgra][pTap] = accUtil;
            } else {
            	accUtil = storedWalkAccessUtils[pMgra][pTap];
            	if(writeCalculations){
            		myLogger.info("Stored walk access utility from Mgra "+pMgra+" to Tap "+pTap+" is "+accUtil);
            	}
            }
            for (int aTap : tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode))
            {

                int lotID = (int) tapManager.getTapParkingInfo()[aTap][0][0]; // lot
                // ID
                float lotCapacity = tapManager.getTapParkingInfo()[aTap][2][0]; // lot
                // capacity
                if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                        || (accMode == AccessMode.KISS_N_RIDE))
                {

                	// Calculate the aTap to aMgra drive egress utility values
                    float egrUtil;
                    if (storedDriveEgressUtils[aTap][aTaz] == StoredUtilityData.default_utility) {
            			egrUtil = calcDriveEgressUtility(driveDmu, aTap, aTaz, aMgra, accMode, writeCalculations, myLogger);
            			storedDriveEgressUtils[aTap][aTaz] = egrUtil;
                    } else {
                    	egrUtil = storedDriveEgressUtils[aTap][aTaz];	
                    	if(writeCalculations){
                    		myLogger.info("Stored drive egress utility from Tap "+aTap+" to TAZ "+aTaz+" is "+egrUtil);
                    	}
                    }
                	
                    // Calculate the pTap to aTap utility values
            		float tapTapUtil[] = new float[numSkimSets];
            		if(!storedDepartPeriodTapTapUtils.get(WTD).get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
            			
            			//loop across number of skim sets  the pTap to aTap utility values 
            			for (int set=0; set<numSkimSets; set++) {
    	            		tapTapUtil[set] = calcUtilitiesForTapPair(walkDmu, period, WTD, pTap, aTap, set, pMgra, aMgra, writeCalculations, myLogger);
            			}
            			storedDepartPeriodTapTapUtils.get(WTD).get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
            		} else {
    	                tapTapUtil = storedDepartPeriodTapTapUtils.get(WTD).get(period).get(storedDataObject.paTapKey(pTap, aTap));
    	            	if(writeCalculations)
    	        			for (int set=0; set<numSkimSets; set++)
    	        				myLogger.info("Stored WTD tap to tap utility set "+set+" from Tap "+pTap+" to Tap "+aTap+" is "+tapTapUtil[set]);
            		}
            		
            		//create path for each skim set
            		for (int set=0; set<numSkimSets; set++) {
            			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, set, WTD, accUtil, tapTapUtil[set], egrUtil));
                	}
   
                }
            }
            
            //save N best paths
            trimPaths(paths);
            if (writeCalculations) {
                logBestUtilities(myLogger);
            }
        }
    }
    
    public float calcWalkAccessUtility(TransitWalkAccessDMU walkDmu, int pMgra, int pTap, boolean myTrace, Logger myLogger)
    {
    	int pPos = mgraManager.getTapPosition(pMgra, pTap);
    	double pWalkTime = mgraManager.getMgraToTapWalkTime(pMgra, pPos);
    	
    	if(pWalkTime==0){
    		myLogger.info("Walk time from mgra "+pMgra+" to tap "+pTap+" at pos "+pPos+" is 0; setting walk access utility to "+NA);
    		return NA;
    	}
    	
        walkDmu.setMgraTapWalkTime(pWalkTime);
        float util = (float)walkAccessUEC.solve(index, walkDmu, null)[0];
        
        // logging
        if (myTrace) {
            walkAccessUEC.logAnswersArray(myLogger, "Walk Orig Mgra=" + pMgra + ", to pTap=" + pTap + " Utility Piece");
        }
        
        return(util);
        
    }
    
    public float calcDriveAccessUtility(TransitDriveAccessDMU driveDmu, int pMgra, int pTaz, int pTap, AccessMode accMode, boolean myTrace, Logger myLogger)
    {
    	int pPos = tazManager.getTapPosition(pTaz, pTap, accMode);
    	double pDriveTime = tazManager.getTapTime(pTaz, pPos, accMode);
    	
    	if(pDriveTime==0){
    		myLogger.info("Drive time from mgra "+pMgra+" to tap "+pTap+" at pos "+pPos+" is 0; setting drive access utility to "+NA);
    		return NA;
    	}

        driveDmu.setDriveDistToTap(tazManager.getTapDist(pTaz, pPos, accMode));
        driveDmu.setDriveTimeToTap(pDriveTime);
        float util = (float)driveAccessUEC.solve(index, driveDmu, null)[0];

        // logging
        if (myTrace) {
        	driveAccessUEC.logAnswersArray(myLogger, "Drive from Orig Taz=" + pTaz + ", to Dest pTap=" + pTap + " Utility Piece");
        }
        return(util);
    }
    
    public float calcWalkEgressUtility(TransitWalkAccessDMU walkDmu, int aTap, int aMgra, boolean myTrace, Logger myLogger)
    {
    	int aPos = mgraManager.getTapPosition(aMgra, aTap);
    	double aWalkTime = mgraManager.getMgraToTapWalkTime(aMgra, aPos);        
 
    	if(aWalkTime==0){
    		myLogger.info("Walk time from tap "+aTap+" to mgra "+aMgra+" at pos "+aPos+" is 0; setting walk egress utility to "+NA);
    		return NA;
    	}

    	walkDmu.setTapMgraWalkTime(aWalkTime);
        float util = (float)walkEgressUEC.solve(index, walkDmu, null)[0];

        // logging
        if (myTrace) {
        	walkEgressUEC.logAnswersArray(myLogger, "Walk from Orig aTap=" + aTap + ", to Dest Mgra=" + aMgra + " Utility Piece");
        }    
        return(util);
    }
    
    public float calcDriveEgressUtility(TransitDriveAccessDMU driveDmu, int aTap, int aTaz, int aMgra, AccessMode accMode, boolean myTrace, Logger myLogger)
    {
    	int aPos = tazManager.getTapPosition(aTaz, aTap, accMode);   
    	double aDriveTime = tazManager.getTapTime(aTaz, aPos, accMode);
        
    	if(aDriveTime==0){
    		myLogger.info("Drive time from tap "+aTap+ " to mgra "+aMgra+" at pos "+aPos+" is 0; setting drive egress utility to "+NA);
    		return NA;
    	}
    	
    	driveDmu.setDriveDistFromTap(tazManager.getTapDist(aTaz, aPos, accMode));
        driveDmu.setDriveTimeFromTap(aDriveTime);
        float util = (float)driveEgressUEC.solve(index, driveDmu, null)[0];

        // logging
        if (myTrace) {
            //driveEgressUEC.logAnswersArray(myLogger, "Drive Tap to Dest Taz Utility Piece");
        	driveEgressUEC.logAnswersArray(myLogger, "Drive from Orig aTap=" + aTap + ", to Dest Taz=" + aTaz + " Utility Piece");
        }
        return(util);
    }
    
    public float calcUtilitiesForTapPair(TransitWalkAccessDMU walkDmu, int period, int accessEgressMode, int pTap, int aTap, int set, int origMgra, int destMgra, boolean myTrace, Logger myLogger) {
   	
        // set up the index and dmu objects
        index.setOriginZone(pTap);
        index.setDestZone(aTap);
        walkDmu.setTOD(period);
        walkDmu.setSet(set);
        walkDmu.setAccessEgressMode(accessEgressMode);

        // solve
        float util = (float)tapToTapUEC.solve(index, walkDmu, null)[0];  
        
        // logging
        if (myTrace) {
        	tapToTapUEC.logAnswersArray(myLogger, ACC_EGR_STRING[accessEgressMode]+" TAP-TAP Utilities From Orig pTap=" + pTap + " (Origin MAZ:" + origMgra +") " +  " to Dest aTap=" + aTap + " (Dest MAZ:" + destMgra +") " + " Utility Piece");
            tapToTapUEC.logResultsArray(myLogger, pTap, aTap);
        }
        return(util);
    }

    
    /**
     * Trim the paths calculated for this TAP-pair to the best N.  
     * Set the bestUtilities[], bestSet[], bestPTap[] and bestATap[]
     * 
     * @param ArrayList<TransitPath> paths Collection of paths
     */
    public void trimPaths(ArrayList<TransitPath> paths)
    {

    	//sort paths by total utility in reverse order to get highest utility first
    	Collections.sort(paths, Collections.reverseOrder());
    	
    	//get best N paths
		int count = 0;
		for(TransitPath path : paths) {
			
			if (path.getTotalUtility() > NA) {
			
				//get data
				bestUtilities[count] = path.getTotalUtility();
	            bestPTap[count] = path.pTap;
	            bestATap[count] = path.aTap;
	            bestSet[count] = path.set;
	            
	            count = count + 1;
				if(count == numTransitAlts) { 
					break;
				}
			}
		}
    }
    
    public float calcPathUtility(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accEgr, int period, int origMgra, int pTap, int aTap, int destMgra, int set, boolean myTrace, Logger myLogger) {
    	
    	float accUtil    =NA;
        float egrUtil    =NA;
        float tapTapUtil =NA;
        
    	if(accEgr==WTW) {
    		accUtil = calcWalkAccessUtility(walkDmu, origMgra, pTap, myTrace, myLogger);
            egrUtil = calcWalkEgressUtility(walkDmu, aTap, destMgra, myTrace, myLogger);
            tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, WTW, pTap, aTap, set, origMgra, destMgra, myTrace, myLogger);
    	} else if(accEgr==WTD) {
    		int aTaz = mgraManager.getTaz(destMgra);
    		AccessMode accMode = AccessMode.PARK_N_RIDE;
    		accUtil = calcWalkAccessUtility(walkDmu, origMgra, pTap, myTrace, myLogger);
    		egrUtil = calcDriveEgressUtility(driveDmu, aTap, aTaz, destMgra, accMode, myTrace, myLogger);
    		tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, WTD, pTap, aTap, set, origMgra, destMgra, myTrace, myLogger);
    	} else if(accEgr==DTW) {
    		int pTaz = mgraManager.getTaz(origMgra);
    		AccessMode accMode = AccessMode.PARK_N_RIDE;
    		accUtil = calcDriveAccessUtility(driveDmu, origMgra, pTaz, pTap, accMode, myTrace, myLogger);
    		egrUtil = calcWalkEgressUtility(walkDmu, aTap, destMgra, myTrace, myLogger);
    		tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, DTW, pTap, aTap, set, origMgra, destMgra, myTrace, myLogger);
    	}
        return(accUtil + tapTapUtil + egrUtil);
    }
    
    /**
     * Return the array of transit best tap pairs for the given access/egress mode, origin MGRA,
     * destination MGRA, and departure time period.
     * 
     * @param TransitWalkAccessDMU walkDmu
     * @param TransitDriveAccessDMU driveDmu
     * @param Modes.AccessMode accMode
     * @param origMgra Origin MGRA
     * @param workMgra Destination MGRA
     * @param departPeriod Departure time period - 1 = AM period, 2 = PM period, 3 =OffPeak period
     * @param debug boolean flag to indicate if debugging reports should be logged
     * @param logger Logger to which debugging reports should be logged if debug is true
     * @return double[][] Array of best tap pair values - rows are N-path, columns are orig tap, dest tap, skim set, utility
     */
    public double[][] getBestTapPairs(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accMode, int origMgra, int destMgra, int departPeriod, boolean debug, Logger myLogger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info("");
            header = ACC_EGR[accMode] + " best tap pairs debug info for origMgra=" + origMgra
                    + ", destMgra=" + destMgra + ", period index=" + departPeriod
                    + ", period label=" + ModelStructure.SKIM_PERIOD_STRINGS[departPeriod];
            for (int i = 0; i < header.length(); i++)
                separator += "^";

            myLogger.info("");
            myLogger.info(separator);
            myLogger.info("Calculating " + header);
        }

        double[][] bestTaps = null;

        if(accMode==WTW) {
        	findBestWalkTransitWalkTaps(walkDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==DTW) {
    		findBestDriveTransitWalkTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==WTD) {
    		findBestWalkTransitDriveTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	}

        // get and log the best tap-tap utilities by alt
        double[] bestUtilities = getBestUtilities();
        bestTaps = new double[bestUtilities.length][];
        
        for (int i = 0; i < bestUtilities.length; i++)
        {
            //only initialize tap data if valid; otherwise null array
        	if (bestUtilities[i] > NA) bestTaps[i] = getBestTaps(i);
        }
        
        // log the best utilities and tap pairs for each alt
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info(separator);
        	myLogger.info(header);
        	myLogger.info("Final Best Utilities:");
        	myLogger.info("Alt, Alt, Utility, bestITap, bestJTap, bestSet");
            for (int i = 0; i < bestUtilities.length; i++)
            {
                myLogger.info(i + "," + i + "," + bestUtilities[i] + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][0]) + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][1]) + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][2]));
            }

            myLogger.info(separator);
        }
        return bestTaps;
    }
    
    /**
     * Calculate utilities for the best tap pairs using person specific attributes.
     * 
     * @param double[][] bestTapPairs
     * @param TransitWalkAccessDMU walkDmu
     * @param TransitDriveAccessDMU driveDmu
     * @param Modes.AccessMode accMode
     * @param origMgra Origin MGRA
     * @param workMgra Destination MGRA
     * @param departPeriod Departure time period - 1 = AM period, 2 = PM period, 3 =OffPeak period
     * @param debug boolean flag to indicate if debugging reports should be logged
     * @param logger Logger to which debugging reports should be logged if debug is true
     * @return double[][] Array of best tap pair values - rows are N-path, columns are orig tap, dest tap, skim set, utility
     */
    public double[][] calcPersonSpecificUtilities(double[][] bestTapPairs, TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accMode, int origMgra, int destMgra, int departPeriod, boolean debug, Logger myLogger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info("");
            header = accMode + " best tap pairs person specific utility info for origMgra=" + origMgra
                    + ", destMgra=" + destMgra + ", period index=" + departPeriod
                    + ", period label=" + ModelStructure.SKIM_PERIOD_STRINGS[departPeriod];
            for (int i = 0; i < header.length(); i++)
                separator += "^";

            myLogger.info("");
            myLogger.info(separator);
            myLogger.info("Calculating " + header);
        }

        //re-calculate utilities
        for (int i = 0; i < bestTapPairs.length; i++) {
            if (bestTapPairs[i] != null) {
            	int pTap = (int)bestTapPairs[i][0];
            	int aTap = (int)bestTapPairs[i][1];
            	int set  = (int)bestTapPairs[i][2];
            	double utility =  calcPathUtility(walkDmu, driveDmu, accMode, departPeriod, origMgra, pTap, aTap, destMgra, set, debug, myLogger);
            	bestTapPairs[i][3] = utility;
            }
        }
        
        // log the best utilities and tap pairs for each alt
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info(separator);
        	myLogger.info(header);
        	myLogger.info("Final Person Specific Best Utilities:");
        	myLogger.info("Alt, Alt, Utility, bestITap, bestJTap, bestSet");
            int availableModeCount = 0;
            for (int i = 0; i < bestUtilities.length; i++)
            {
                if (bestTapPairs[i] != null) availableModeCount++;

                myLogger.info(i + "," + i + "," 
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][3]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][0]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][1]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][2]));
            }

            myLogger.info(separator);
        }
        return bestTapPairs;
    }
    
    /*
    private LogitModel setupTripLogSum(double[][] bestTapPairs, boolean myTrace, Logger myLogger) {      
    	
    	//must size logit model ahead of time
    	int alts = 0;
    	for (int i=0; i<bestTapPairs.length; i++) {
        	if (bestTapPairs[i] != null) {
        		alts = alts + 1;
        	}
    	}
    	
    	LogitModel tripNPaths = new LogitModel("trip-paths",0, alts);
        for (int i=0; i<bestTapPairs.length; i++) {
        	
        	if (bestTapPairs[i] != null) {
        		ConcreteAlternative alt = new ConcreteAlternative(String.valueOf(i),i);
                alt.setUtility(bestTapPairs[i][3]);
                tripNPaths.addAlternative(alt);
        	}
    		
        }

        return(tripNPaths);
    }
    */
    
    /**
     * Returns the constrained logsum using the getTransitBestPathLogsum() method.
     * 
     * @param bestTapPairs
     * @param myTrace
     * @param myLogger
     * @return
     */
    public float calcTripLogSum(double[][] bestTapPairs, boolean myTrace, Logger myLogger) {      
    	
    	return (float) getTransitBestPathLogsum(bestTapPairs, myTrace, myLogger);
    }

    /**
     * Choose a trip path across all available tap pairs.
     * 
     * @param rnum
     * @param bestTapPairs
     * @param myTrace
     * @param myLogger
     * @return
     */
    public int chooseTripPath(float rnum, double[][] bestTapPairs, boolean myTrace, Logger myLogger) {
    	
    	if(myTrace){
    		myLogger.info("****************************************");
    		myLogger.info("Choosing best TAP pair with rnum "+rnum);
    		myLogger.info("BestPath    Utility   ExpUtility  Prob  Cumul.P");
    		myLogger.info("-----------------------------------------------");
    	}
    	Arrays.fill(expUtilities, 0);
    	int alt=-1;
    	//iterate through paths and calculate exponentiated utility and sum
    	double sumExpUtility=0;
    	for(int i = 0; i<bestTapPairs.length;++i){
    		if(bestTapPairs[i] == null)
    			continue;
    		if(bestTapPairs[i][3]<-500)
    			continue;
    		expUtilities[i] = Math.exp(bestTapPairs[i][3]/nestingCoefficient);
    		sumExpUtility += expUtilities[i]; 
        		
    	}
    	if(sumExpUtility>0){
			double cumProb=0;
    		//re-iterate through paths and calculate probability, choose alternative based on rnum
    		for(int i = 0; i<bestTapPairs.length;++i){
        		if(bestTapPairs[i] == null)
        			continue;
        		if(bestTapPairs[i][3]<-500)
        			continue;
   				cumProb += (expUtilities[i]/sumExpUtility);
   				
   				if(myTrace){
   					myLogger.info(i+"     "+bestTapPairs[i][3]+"  "+expUtilities[i]+"  "+(expUtilities[i]/sumExpUtility)+"  "+ cumProb);
   				}
   				if(rnum<=cumProb){
   					alt = i;
   					if(myTrace)
   						myLogger.info("Chose Tap Pair "+i);
   					break;
    			}
    		}
    	}
    	else{
    		myLogger.info("No best taps to pick set from");
    	}
    	return alt;
    }
    
    /**
     * Log the best utilities so far to the logger.
     * 
     * @param localLogger The logger to use for output.
     */
    public void logBestUtilities(Logger localLogger)
    {

        // create the header
        String header = String.format("%16s", "Alternative");
        header += String.format("%14s", "Utility");
        header += String.format("%14s", "PTap");
        header += String.format("%14s", "ATap");
        header += String.format("%14s", "Set");

        localLogger.info("Best Utility and Tap to Tap Pair");
        localLogger.info(header);

        // log the utilities and tap number for each alternative
        for (int i=0; i<numTransitAlts; i++)
        {
            header = header + String.format("  %16s", i);
        }
        for (int i=0; i<numTransitAlts; i++)
        {
            String line = String.format("%16s", i);
            line = line + String.format("  %12.4f", bestUtilities[i]);
            line = line + String.format("  %12d", bestPTap[i]);
            line = line + String.format("  %12d", bestATap[i]);
            line = line + String.format("  %12d", bestSet[i]);

            localLogger.info(line);
        }
    }

    public void setTrace(boolean myTrace)
    {
        tracer.setTrace(myTrace);
    }

    /**
     * Trace calculations for a zone pair.
     * 
     * @param itaz
     * @param jtaz
     * @return true if zone pair should be traced, otherwise false
     */
    public boolean isTraceZonePair(int itaz, int jtaz)
    {
        if (tracer.isTraceOn()) {
            return tracer.isTraceZonePair(itaz, jtaz);
        } else {
            return false;
        }
    }

    /**
     * Get the best utilities. Note that these utilities have
     * not been trimmed by the maximum number of utilities
     * for each skim set. If a transit logsum is desired, use
     * getTransitBestPathLogsum(). 
     * 
     * @return An array of the best utilities.
     */
    public double[] getBestUtilities()
    {
        return bestUtilities;
    }
    
    
    /**
     * Get the best path logsum, subject to constraints. The constraints
     * are that the logsum only include a certain number of paths for each
     * skim set, as defined in the property utility.bestTransitPath.maxPathsPerSkimSetForLogsum.
     * This allows the logsum 
     * to reduce or eliminate path overlap should any exist in the path set, without having
     * access to actual route data in the utility calculation. Transit trips
     * are still subject to choice across all paths in the best utility set.
     * 
     * @return The constrained transit logsum. 
     */
    public double getTransitBestPathLogsum(){
    	
    	double logsum = NA;
    	double sumExpUtility  = getSumExpUtilities();
    	if(sumExpUtility>0)
    		logsum = Math.log(sumExpUtility);
    	return logsum;
    }
    
    /**
     * Get the best path logsum, subject to constraints. The constraints
     * are that the logsum only include a certain number of paths for each
     * skim set, as defined in the property utility.bestTransitPath.maxPathsPerSkimSetForLogsum.
     * This allows the logsum 
     * to reduce or eliminate path overlap should any exist in the path set, without having
     * access to actual route data in the utility calculation. Transit trips
     * are still subject to choice across all paths in the best utility set.
     * 
     * @return The constrained transit logsum. 
     */
    public double getTransitBestPathLogsum(double[][] bestTapPairs, boolean myTrace, Logger myLogger){
    	
    	double logsum = NA;
    	double sumExpUtility  = getSumExpUtilities(bestTapPairs, myTrace, myLogger);
    	if(sumExpUtility>0.0)
    		logsum = Math.log(sumExpUtility);
    	
    	if(myTrace)
    		myLogger.info("Best Transit Path Logsum "+logsum);
    	return logsum;
    }

    
    /**
     * Get the sum of exponentiated utilities, subject to constraints. The constraints
     * are that the sum only include a certain number of paths for each
     * skim set, as defined in the property utility.bestTransitPath.maxPathsPerSkimSetForLogsum.
     * to reduce or eliminate path overlap should any exist in the path set, without having
     * access to actual route data in the utility calculation. Transit trips
     * are still subject to choice across all paths in the best utility set.
     * 
     * @param bestTapPairs The tap pairs to calculate the sum exponentiated utility over
     * @param myTrace  Trace calculations
     * @param myLogger The logger to write tracing to
     * 
     * @return The constrained sum of exponentiated utilities. 
     */
     public double getSumExpUtilities(double[][] bestTapPairs, boolean myTrace, Logger myLogger){
    	double sumExpUtility=0;
    	
    	if(myTrace){
    		myLogger.info("Calculating sum of exponentiated utilities for transit best TAP pairs");
    		myLogger.info("Best_Path   Utility   Skim_Set  Included?  ExpUtility  Sum");
    	}
    	
    	//utilityCount tracks how many utilities included in logsum calc by skimset
    	Arrays.fill(utilityCount,0); 
    	for(int i = 0; i<bestTapPairs.length;++i){
    		byte included=0;
    		if(bestTapPairs[i] != null){
    			int skimSet = (int)bestTapPairs[i][2];
    			
    			//only include the utility in the logsum if the count
    			//by skimset hasn't been met yet.
        		if(utilityCount[skimSet]<maxLogsumUtilitiesBySkimSet[skimSet]){
        			included=1;
        			sumExpUtility += Math.exp(bestTapPairs[i][3]); 
        			++utilityCount[skimSet];
        		}
    		}
    		if(myTrace)
    			if(bestTapPairs[i] != null)
    				myLogger.info(i+"  "+bestTapPairs[i][3]+" "+bestTapPairs[i][2]+"    "+included+"    "+Math.exp(bestTapPairs[i][3])+" "+sumExpUtility);
    	}

    	return sumExpUtility;
    }
    
    
    /**
     * Get the sum of exponentiated utilities, subject to constraints. The constraints
     * are that the sum only include a certain number of paths for each
     * skim set, as defined in the property utility.bestTransitPath.maxPathsPerSkimSetForLogsum.
     * to reduce or eliminate path overlap should any exist in the path set, without having
     * access to actual route data in the utility calculation. Transit trips
     * are still subject to choice across all paths in the best utility set.
     * 
     * @return The constrained sum of exponentiated utilities. 
     */
    public double getSumExpUtilities(){
    	double sumExpUtility=0;
    	//utilityCount tracks how many utilities included in logsum calc by skimset
    	Arrays.fill(utilityCount,0); 
    	for(int i = 0; i<bestUtilities.length;++i){
    		if(bestUtilities[i] > -500){
    			int skimSet = bestSet[i];
    			
    			//only include the utility in the logsum if the count
    			//by skimset hasn't been met yet.
        		if(utilityCount[skimSet]<maxLogsumUtilitiesBySkimSet[skimSet]){
        			sumExpUtility += Math.exp(bestUtilities[i]); 
        			++utilityCount[skimSet];
        		}
    		}
    			
    	}

    	return sumExpUtility;
    }
    

    /**
     * Create the UEC for the main transit portion of the utility.
     * 
     * @param uecSpreadsheet The .xls workbook with the model specification.
     * @param modelSheet The sheet with model specifications.
     * @param dataSheet The sheet with the data specifications.
     * @param rb A resource bundle with the path to the skims "skims.path"
     * @param dmu The DMU class for this UEC.
     */
    public UtilityExpressionCalculator createUEC(File uecSpreadsheet, int modelSheet,
            int dataSheet, HashMap<String, String> rbMap, VariableTable dmu)
    {
        return new UtilityExpressionCalculator(uecSpreadsheet, modelSheet, dataSheet, rbMap, dmu);
    }

    /**
     * Clears the arrays. This method gets called for two different purposes. One is
     * to compare alternatives based on utilities and the other based on
     * exponentiated utilities. For this reason, the bestUtilities will be
     * initialized by the value passed in as an argument set by the calling method.
     * 
     * @param initialization value
     */
    public void clearBestArrays(double initialValue)
    {
        Arrays.fill(bestUtilities, initialValue);
        Arrays.fill(bestPTap, 0);
        Arrays.fill(bestATap, 0);
        Arrays.fill(bestSet, 0);
    }

    /**
     * Get the best ptap, atap, and skim set in an array. Only to be called after trimPaths() has been called.
     * 
     * @param alt.
     * @return element 0 = best ptap, element 1 = best atap, element 2 = set, element 3= utility
     */
    public double[] getBestTaps(int alt)
    {

    	double[] bestTaps = new double[4];

        bestTaps[0] = bestPTap[alt];
        bestTaps[1] = bestATap[alt];
        bestTaps[2] = bestSet[alt];
        bestTaps[3] = bestUtilities[alt];

        return bestTaps;
    }

    /**
     * Get the best transit alt. Returns null if no transit alt has a valid utility. 
     * Call only after calling findBestWalkTransitWalkTaps().
     * 
     * @return The best transit alt (highest utility), or null if no alt have a valid utility.
     */
    public int getBestTransitAlt()
    {

        int best = -1;
        double bestUtility = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bestUtilities.length; ++i)
        {
            if (bestUtilities[i] > bestUtility) {
            	best = i;
                bestUtility = bestUtilities[i];
            }
        }
        
        int returnSet = best;
        if (best > -1) {
        	returnSet = best;
        }
        return returnSet;
    }
 
    
    /**
     * This method writes the utilities for all TAP-pairs for each ride mode.
     * It cycles through walk TAPs at the origin end (associated with the origin
     * MGRA) and alighting TAPs at the destination end (associated with the
     * destination MGRA) and calculates a utility for every available ride mode
     * for each TAP pair and writes the result to the outwriter.
     * 
     *  The results written will be as follows:
     *    label,WTW,period,pTap,aTap,mode,combinedUtilities[mode]
     * 
     * @param period The time period (AM, PM, Off)
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * @param myLogger A logger for logging problems
     * @param outwriter A printwriter for writing results
     * @param label A label for the record.
     */
    public void writeAllWalkTransitWalkTaps(int period, int pMgra, int aMgra, Logger myLogger, PrintWriter outwriter, String label)
    {
    	
    	//TODO: Fix this
    	
    	/*

        clearBestArrays(Double.NEGATIVE_INFINITY);

        int[] pMgraSet = mgraManager.getMgraWlkTapsDistArray()[pMgra][0];
        int[] aMgraSet = mgraManager.getMgraWlkTapsDistArray()[aMgra][0];

        if (pMgraSet == null || aMgraSet == null)
        {
            return;
        }

        int pPos = -1;
        for (int pTap : pMgraSet)
        {
            // used to know where we are in time/dist arrays for taps
            pPos++;

            // Set the pMgra to pTap walk access utility values, if they haven't
            // already been computed.
            setWalkAccessUtility(pMgra, pPos, pTap, false, myLogger);

            int aPos = -1;
            for (int aTap : aMgraSet)
            {
                // used to know where we are in time/dist arrays for taps
                aPos++;

                // set the pTap to aTap utility values, if they haven't already
                // been computed.
                setUtilitiesForTapPair(WTW, period, pTap, aTap, false, myLogger);

                // Set the aTap to aMgra walk egress utility values, if they
                // haven't already been computed.
                setWalkEgressUtility(aTap, aMgra, aPos, false, myLogger);

                // write the utilities for each ride mode 
                try
                {
                    for (int i = 0; i < combinedUtilities.length; i++){
                        combinedUtilities[i] = storedWalkAccessUtils[pMgra][pTap][i]
                                + storedTapToTapUtils[WTW][period][pTap][aTap][i]
                                + storedWalkEgressUtils[aTap][aMgra][i];
                        
                        if(combinedUtilities[i]>-500){
                        	outwriter.print(label);
                        	outwriter.format(",%d,%d,%d,%d,%d,%9.4f\n",WTW,period,pTap,aTap,i,combinedUtilities[i]);
                        }
                    }
                } catch (Exception e)
                {
                    logger.error("exception computing combinedUtilities for WTW");
                    logger.error("aTap=" + aTap + "pTap=" + pTap + "aMgra=" + aMgra + "pMgra="
                            + pMgra + "period=" + period, e);
                    throw new RuntimeException();
                }

               

            }
        }
       */
    }
    
    /**
     * This method writes all TAP-pairs for each ride mode. It cycles
     * through drive access TAPs at the origin end (associated with the origin
     * MGRA) and alighting TAPs at the destination end (associated with the
     * destination MGRA) and calculates a utility for every available ride mode
     * for each TAP pair. 
     *  The results written will be as follows:
     *  
     *    label,DTW,period,pTap,aTap,mode,combinedUtilities[mode]
     * 
     * @param period The time period (AM, PM, Off)
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * @param myLogger A logger for logging problems
     * @param outwriter A printwriter for writing results
     * @param label A label for the record.
     * 
     */
    public void writeAllDriveTransitWalkTaps(int period, int pMgra, int aMgra,
            Logger myLogger, PrintWriter outwriter, String label)
    {

   	// TODO: Fix this
    	
    	/*

         clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int pTaz = mgraManager.getTaz(pMgra);
 
        if (tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode) == null
                || mgraManager.getMgraWlkTapsDistArray()[aMgra][0] == null)
        {
            return;
        }

        float[][][] tapParkingInfo = tapManager.getTapParkingInfo();

        int pPos = -1;
        int[] pTapArray = tazManager.getParkRideOrKissRideTapsForZone(pTaz, accMode);
        for (int pTap : pTapArray)
        {
            pPos++; // used to know where we are in time/dist arrays for taps

            // Set the pTaz to pTap drive access utility values, if they haven't
            // already been computed.
            setDriveAccessUtility(pTaz, pPos, pTap, accMode, false, myLogger);

            int lotID = (int) tapParkingInfo[pTap][0][0]; // lot ID
            float lotCapacity = tapParkingInfo[pTap][2][0]; // lot capacity

            if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                    || (accMode == AccessMode.KISS_N_RIDE))
            {

                int aPos = -1;
                for (int aTap : mgraManager.getMgraWlkTapsDistArray()[aMgra][0])
                {
                    aPos++;

                    // Set the aTap to aMgra walk egress utility values, if they
                    // haven't already been computed.
                    setWalkEgressUtility(aTap, aMgra, aPos, false, myLogger);

          
                    // set the pTap to aTap utility values, if they haven't
                    // already been computed.
                    setUtilitiesForTapPair(DTW, period, pTap, aTap, false, myLogger);

                    // compare the utilities for this TAP pair to previously
                    // calculated utilities for each ride mode and store the TAP numbers if
                    // this TAP pair is the best.
                    try
                    {
                        for (int i = 0; i < combinedUtilities.length; i++){
                            combinedUtilities[i] = storedDriveAccessUtils[pTaz][pTap][i]
                                    + storedTapToTapUtils[DTW][period][pTap][aTap][i]
                                    + storedWalkEgressUtils[aTap][aMgra][i];
                            if(combinedUtilities[i]>-500){
                            	outwriter.print(label);
                            	outwriter.format(",%d,%d,%d,%d,%d,%9.4f\n",DTW,period,pTap,aTap,i,combinedUtilities[i]);
                            }
                        }
                    } catch (Exception e)
                    {
                        logger.error("exception computing combinedUtilities for DTW");
                        logger.error("aTap=" + aTap + ",pTap=" + pTap + ",aMgra=" + aMgra
                                + ",pMgra=" + pMgra + ",period=" + period, e);
                        throw new RuntimeException();
                    }

                }
            }
           
        }
        */
    }
    
    /**
     * This method finds the best TAP-pairs for each ride mode. It cycles
     * through drive access TAPs at the origin end (associated with the origin
     * MGRA) and alighting TAPs at the destination end (associated with the
     * destination MGRA) and calculates a utility for every available ride mode
     * for each TAP pair.      
     *  The results written will be as follows:
     *  
     *    label,WTD,period,pTap,aTap,mode,combinedUtilities[mode]
     * 
     * @param period The time period (AM, PM, Off)
     * @param pMgra
     *            The origin/production MGRA.
     * @param aMgra
     *            The destination/attraction MGRA.
     * @param myLogger A logger for logging problems
     * @param outwriter A printwriter for writing results
     * @param label A label for the record.

     * 
     */
    public void writeAllWalkTransitDriveTaps(int period, int pMgra, int aMgra,
            Logger myLogger, PrintWriter outwriter, String label)
    {

    	// TODO: Fix this
    	
    	/*
        clearBestArrays(Double.NEGATIVE_INFINITY);

        Modes.AccessMode accMode = AccessMode.PARK_N_RIDE;

        int aTaz = mgraManager.getTaz(aMgra);

        if (mgraManager.getMgraWlkTapsDistArray()[pMgra][0] == null
                || tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode) == null)
        {
            return;
        }

        int pPos = -1;
        for (int pTap : mgraManager.getMgraWlkTapsDistArray()[pMgra][0])
        {
            pPos++; // used to know where we are in time/dist arrays for taps

            // Set the pMgra to pTap walk access utility values, if they haven't
            // already been computed.
            setWalkAccessUtility(pMgra, pPos, pTap, false, myLogger);

            int aPos = -1;
            for (int aTap : tazManager.getParkRideOrKissRideTapsForZone(aTaz, accMode))
            {
                aPos++;

                int lotID = (int) tapManager.getTapParkingInfo()[aTap][0][0]; // lot
                // ID
                float lotCapacity = tapManager.getTapParkingInfo()[aTap][2][0]; // lot
                // capacity
                if ((accMode == AccessMode.PARK_N_RIDE && tapManager.getLotUse(lotID) < lotCapacity)
                        || (accMode == AccessMode.KISS_N_RIDE))
                {

                    // Set the pTaz to pTap drive access utility values, if they
                    // haven't already been computed.
                    setDriveEgressUtility(aTap, aTaz, aPos, accMode, false, myLogger);

                    // set the pTap to aTap utility values, if they haven't
                    // already
                    // been computed.
                    setUtilitiesForTapPair(WTD, period, pTap, aTap, false, myLogger);

                    // compare the utilities for this TAP pair to previously
                    // calculated utilities for each ride mode and store the TAP numbers if
                    // this TAP pair is the best.
                    try
                    {
                        for (int i = 0; i < combinedUtilities.length; i++){
                            combinedUtilities[i] = storedWalkAccessUtils[pMgra][pTap][i]
                                    + storedTapToTapUtils[WTD][period][pTap][aTap][i]
                                    + storedDriveEgressUtils[aTap][aTaz][i];
                            if(combinedUtilities[i]>-500){
                            	outwriter.print(label);
                            	outwriter.format(",%d,%d,%d,%d,%d,%9.4f\n",WTD,period,pTap,aTap,i,combinedUtilities[i]);
                            }
                        }
                    } catch (Exception e)
                    {
                        logger.error("exception computing combinedUtilities for WTD");
                        logger.error("aTap=" + aTap + ",pTap=" + pTap + ",aMgra=" + aMgra
                                + ",pMgra=" + pMgra + ",period=" + period, e);
                        throw new RuntimeException();
                    }

                }
            }
        
        }
        */
    }


}
