package org.sandag.abm.maas;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class TransportCostManager {

    private transient Logger              logger                 = Logger.getLogger(TransportCostManager.class);

    private static final int              EA                            = ModelStructure.EA_SKIM_PERIOD_INDEX;
    private static final int              AM                            = ModelStructure.AM_SKIM_PERIOD_INDEX;
    private static final int              MD                            = ModelStructure.MD_SKIM_PERIOD_INDEX;
    private static final int              PM                            = ModelStructure.PM_SKIM_PERIOD_INDEX;
    private static final int              EV                            = ModelStructure.EV_SKIM_PERIOD_INDEX;
    public static final int              NUM_PERIODS                   = ModelStructure.SKIM_PERIOD_INDICES.length;
    private static final String[]         PERIODS                = ModelStructure.SKIM_PERIOD_STRINGS;

    //by period, origin, destination -  ragged array of zone numbers of zones within max time diversion
    //sorted by time from origin (assuming pickups would be en-route)
    private int[][][][]                  tazsWithinOriginAndDestination; 
    private float[][][][]                addTimeWithinOriginAndDestination; 
    
    //by period, origin, destination
    private float[][][]                  tazTimeSkims;		//travel time
    private float[][][]                  tazDistanceSkims;	//travel distance
    
    private int[][][]                    tazsByTimeFromOrigin; //array of TAZs sorted by time from origin, by period and origin TAZ

    private float 						  maxTimeDiversion;
    private float 						  maxDistanceToPickup;
    private int                           maxTaz;

    // declare an array of UEC objects, 1 for each time period
    private UtilityExpressionCalculator[] autoDistOD_UECs;
    private UtilityExpressionCalculator[] autoTimeOD_UECs;
    
    // The simple auto skims UEC does not use any DMU variables
    private VariableTable                 dmu         = null;
    private TazDataManager                tazManager;

    /**
     * Instantiate transport cost manager.
     * 
     * @param rbMap
     * @param maxTimeDiversion
     */
    public TransportCostManager(HashMap<String, String> rbMap, float maxTimeDiversion, float maxDistanceToPickup)
    {

    	this.maxTimeDiversion=maxTimeDiversion;
    	this.maxDistanceToPickup=maxDistanceToPickup;
    	
        // Create the UECs
        String uecPath = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String uecFileName = uecPath
                + Util.getStringValueFromPropertyMap(rbMap, "taz.distance.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "taz.distance.data.page");
        
        
        //iterate thru settings in properties file and create time and distance UECs
        autoDistOD_UECs = new UtilityExpressionCalculator[NUM_PERIODS];
        autoTimeOD_UECs = new UtilityExpressionCalculator[NUM_PERIODS];
        File uecFile = new File(uecFileName);
       
        for(int i =0; i<NUM_PERIODS;++i){
        	
        	String distName = "taz.od.distance."+PERIODS[i].toLowerCase()+".page";
        	String timeName = "taz.od.time."+PERIODS[i].toLowerCase()+".page";
        	int distancePage = Util.getIntegerValueFromPropertyMap(rbMap, distName);
        	int timePage = Util.getIntegerValueFromPropertyMap(rbMap, timeName);
       
            autoDistOD_UECs[EA] = new UtilityExpressionCalculator(uecFile, distancePage, dataPage,
                    rbMap, dmu);
       
            autoTimeOD_UECs[EA] = new UtilityExpressionCalculator(uecFile, timePage, dataPage,
                    rbMap, dmu);
        }
       
        tazManager = TazDataManager.getInstance();
        maxTaz = tazManager.getMaxTaz();
    }

    /**
     * Instantiate all of the arrays used to hold times and distances and call the method
     * to find stop zones for each origin-destination zone pair and save the zone number
     * and diversion time, sorted by distance from origin.
     * 
     */
    public void initialize()
    {

    	//create time and distance matrices
    	tazTimeSkims  = new float[NUM_PERIODS][maxTaz+1][maxTaz+1];
    	tazDistanceSkims  = new float[NUM_PERIODS][maxTaz+1][maxTaz+1];
    	
        IndexValues iv = new IndexValues();

        for (int period = 0; period < NUM_PERIODS; ++period){
        	for (int oTaz = 1; oTaz <= maxTaz; oTaz++){
       
        		iv.setOriginZone(oTaz);

	            double[] autoDist = autoDistOD_UECs[period].solve(iv, dmu, null);
	            double[] autoTime = autoTimeOD_UECs[period].solve(iv, dmu, null);
        
	            for (int d = 0; d < maxTaz; d++){	            
	            	tazDistanceSkims[period][oTaz][d + 1] = (float) autoDist[d];
	            	tazTimeSkims[period][oTaz][d + 1] = (float) autoTime[d];
	            }
        	}
        }

        calculateTazsWithinDistanceThreshold();
    }
    
    /**
     * This method finds stop zones for each origin-destination zone pair and saves the zone number
     * and diversion time, sorted by distance from origin.
     * 
     */
    private void calculateTazsWithinDistanceThreshold(){
    	
    	ArrayList<StopTaz> stopTazList = new ArrayList<StopTaz>();
    
        tazsWithinOriginAndDestination = new int[NUM_PERIODS][maxTaz+1][maxTaz+1][];
        addTimeWithinOriginAndDestination = new float[NUM_PERIODS][maxTaz+1][maxTaz+1][];

    	for(int period = 0; period<NUM_PERIODS;++period){
           	for (int oTaz = 1; oTaz <= maxTaz; oTaz++){
	            for (int dTaz = 1; dTaz <= maxTaz; dTaz++){	
	            	
	            	stopTazList.clear();
	            	
	            	//Stop TAZs
	            	for(int kTaz = 1; kTaz <= maxTaz; ++kTaz){
	            	
	            		//Calculate additional time to stop
	            		float ikTime = tazTimeSkims[period][oTaz][kTaz];
	            		float kjTime = tazTimeSkims[period][kTaz][dTaz];
	            		float totalIKJTime = ikTime + kjTime;
	            		float divertTime = totalIKJTime - tazTimeSkims[period][oTaz][dTaz];
	            	
	            		//if time is less than max diversion time, add zone and time to arraylist
	            		if( divertTime < maxTimeDiversion){
	            			StopTaz stopTaz = new StopTaz();
	            			stopTaz.tazNumber = kTaz;
	            			stopTaz.diversionTime = divertTime;
	            			stopTaz.originStopTime = ikTime;
	            			stopTazList.add(stopTaz);
	            		}
	            		
	            	} //end for stops
	            	
	            	//initialize arrays for saving tazs, time and set the values in the ragged arrays
	            	if(!stopTazList.isEmpty()){
	            		Collections.sort(stopTazList);
	            		int numberOfStops = stopTazList.size();
	            		tazsWithinOriginAndDestination[period][oTaz][dTaz] = new int[numberOfStops];
	            		addTimeWithinOriginAndDestination[period][oTaz][dTaz] = new float[numberOfStops];
	            	
	            		for(int k = 0; k < numberOfStops; ++k){
	            			StopTaz stopTaz = stopTazList.get(k);
	            			tazsWithinOriginAndDestination[period][oTaz][dTaz][k] = stopTaz.tazNumber;
	            			addTimeWithinOriginAndDestination[period][oTaz][dTaz][k] = stopTaz.diversionTime;
	            		}
	            	}
	            }
           	}

    	}
    
    }
    
    /**
     * Calculate zones sorted by time from origin.
     * 
     */
    public void calculateTazsByTimeFromOrigin(){
    	
       	ArrayList<StopTaz> stopTazList = new ArrayList<StopTaz>();
        
       	tazsByTimeFromOrigin = new int[NUM_PERIODS][maxTaz+1][];
 
    	for(int period = 0; period<NUM_PERIODS;++period){
           	for (int oTaz = 1; oTaz <= maxTaz; oTaz++){

            	stopTazList.clear();

           		for (int dTaz = 1; dTaz <= maxTaz; dTaz++){	
	            	
           			if(tazDistanceSkims[period][oTaz][dTaz]<maxDistanceToPickup){
           				StopTaz stopTaz = new StopTaz();
           				stopTaz.tazNumber = dTaz;
           				stopTaz.diversionTime = tazTimeSkims[period][oTaz][dTaz];
           				stopTazList.add(stopTaz);
           			}
           		}
           		
           		if(!stopTazList.isEmpty()){
           			int numberOfStops = stopTazList.size();
           			Collections.sort(stopTazList);
           	       	tazsByTimeFromOrigin[period][oTaz] = new int[numberOfStops];
           	       	for(int i = 0; i < numberOfStops; ++i){
           	       		StopTaz stopTaz = stopTazList.get(i);
           	       		tazsByTimeFromOrigin[period][oTaz][i] = stopTaz.tazNumber;
           	       	}
           		}
           	}
    	}
    }
    
    /**
     * This is a convenience class used by the private methods used to sort TAZs.
     * @author joel.freedman
     *
     */
    private class StopTaz implements Comparable{
    	
    	int tazNumber;
    	float diversionTime;
    	float originStopTime;
    	
    	public StopTaz(){
    		
    	}
    
    	/**
    	 * Compare based on departure time.
    	 */
    	public int compareTo(Object aThat) {
    	    final int BEFORE = -1;
    	    final int EQUAL = 0;
    	    final int AFTER = 1;

    	    final StopTaz that = (StopTaz)aThat;

    	    //primitive numbers follow this form
    	    if (this.originStopTime < that.originStopTime) return BEFORE;
    	    if (this.originStopTime > that.originStopTime) return AFTER;

    		return EQUAL;
    	}

    
    }
    
    /**
     * Get the array of zones that are within the diversion time from the origin to the 
     * destination, sorted by time from origin.
     * 
     * @param period
     * @param origTaz
     * @param destTaz
     * @return The array of zones, or null if there are no zones within the max diversion time.
     */
   public int[] getZonesWithinMaxDiversionTime(int period, int origTaz, int destTaz){
	   
	   return tazsWithinOriginAndDestination[period][origTaz][destTaz];
	   
   }
 
   /**
    * Get the diversion times for the zones that are within the diversion time from the origin to the 
    * destination, sorted by time from origin.
    * 
    * @param period
    * @param origTaz
    * @param destTaz
    * @return The array of diversion times, or null if there are no zones within the max diversion time.
    */
  public float[] getDiversionTimes(int period, int origTaz, int destTaz){
	   
	   return addTimeWithinOriginAndDestination[period][origTaz][destTaz];
	   
  }

  /**
   * Get a ragged array of zone numbers sorted by time from the origin. The array is ragged 
   * because it is capped by the maximum distance for hailing a TNC\TAXI.
   * 
   * @param period
   * @param origTaz
   * @return A sorted array of zone numbers, or null if there are no zones within the maximum distance.
   */
  public int[] getZoneNumbersSortedByTime(int period, int origTaz){
	  
	  return tazsByTimeFromOrigin[period][origTaz];
  }
}
