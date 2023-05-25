package org.sandag.abm.maas;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.UtilityExpressionCalculator;

import drasys.or.util.Array;

public class TransportCostManager {

    protected transient Logger              logger                 = Logger.getLogger(TransportCostManager.class);

    protected static final int              EA                            = ModelStructure.EA_SKIM_PERIOD_INDEX;
    protected static final int              AM                            = ModelStructure.AM_SKIM_PERIOD_INDEX;
    protected static final int              MD                            = ModelStructure.MD_SKIM_PERIOD_INDEX;
    protected static final int              PM                            = ModelStructure.PM_SKIM_PERIOD_INDEX;
    protected static final int              EV                            = ModelStructure.EV_SKIM_PERIOD_INDEX;
    public static final int              NUM_PERIODS                   = ModelStructure.SKIM_PERIOD_INDICES.length;
    protected static final String[]         PERIODS                = ModelStructure.SKIM_PERIOD_STRINGS;

    protected static int TAZ_CALCULATOR_THREADS = 20; //default
    
    //by period, origin, destination -  ragged array of zone numbers of zones within max time diversion
    //sorted by time from origin (assuming pickups would be en-route)
    protected short[][][][]                  tazsWithinOriginAndDestination; 
  //  private float[][][][]                addTimeWithinOriginAndDestination; 
    
    //by period, origin, destination
    protected float[][][]                  tazTimeSkims;		//travel time
    protected float[][][]                  tazDistanceSkims;	//travel distance
    
    protected short[][][]                    tazsByTimeFromOrigin; //array of TAZs sorted by time from origin, by period and origin TAZ

    protected float 						  maxTimeDiversion;
    protected float 						  maxDistanceToPickup;
    protected int                           maxTaz;

    // declare an array of UEC objects, 1 for each time period
    protected UtilityExpressionCalculator[] autoDistOD_UECs;
    protected UtilityExpressionCalculator[] autoTimeOD_UECs;
    
    // The simple auto skims UEC does not use any DMU variables
    protected VariableTable                 dmu         = null;
    protected TazDataManager                tazManager;
    int totalThreads;
    

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
       
            autoDistOD_UECs[i] = new UtilityExpressionCalculator(uecFile, distancePage, dataPage,
                    rbMap, dmu);
       
            autoTimeOD_UECs[i] = new UtilityExpressionCalculator(uecFile, timePage, dataPage,
                    rbMap, dmu);
        }
       
        tazManager = TazDataManager.getInstance();
        maxTaz = tazManager.getMaxTaz();
        
        totalThreads = Util.getIntegerValueFromPropertyMap(rbMap, "TNC.totalThreads");
        
    }

    /**
     * Instantiate all of the arrays used to hold times and distances and call the method
     * to find stop zones for each origin-destination zone pair and save the zone number
     * and diversion time, sorted by distance from origin.
     * 
     */
    public void initialize()
    {

    	logger.info("Initializing TransportCostManager");
    	
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
        
    	logger.info("Completed Initializing TransportCostManager");

    }
    
    
    /**
     * A class that calculates TAZs within maximum distance, and creates the tazsWithinOriginAndDestination array.
     * Run method does the work for a specific time period and range of origin TAZs. Implements Runnable so that
     * threading can be used.
     * 
     * @author joel.freedman
     *
     */
    private class TazDistanceCalculatorThread implements Runnable{ 
    
    	private String threadName;
    	   
    	int period;
    	int startOriginTaz;
    	int endOriginTaz;
    	
    	public TazDistanceCalculatorThread( String threadName, int period, int startOriginTaz, int endOriginTaz ) {
    		this.threadName = threadName;
    		this.period = period;
    		this.startOriginTaz = startOriginTaz;
    		this.endOriginTaz = endOriginTaz;
    		logger.info("Creating " +  threadName );
        }
   
        /**
         * Run the thread. Calls @calculateTazsWithinDistanceThreshold
         */
    	public void run() {
    	   logger.info("Running " +  threadName + " for period " + period + " from origin TAZ "+startOriginTaz+ " to origin TAZ "+endOriginTaz );
    	   calculateTazsWithinDistanceThreshold( period,  startOriginTaz,  endOriginTaz);
    	   logger.info("Thread " +  threadName + " exiting.");
    	//   notify();
    	}
    	
    	/**
    	 * The main work method in the thread, which calculates TAZs within the max distance threshold
    	 * for the given time period and from start to end TAZ number.
    	 * 
    	 * @param period
    	 * @param startOriginTaz
    	 * @param endOriginTaz
    	 */
    	private void calculateTazsWithinDistanceThreshold(int period, int startOriginTaz, int endOriginTaz){
   			
    		ArrayList<StopTaz> stopTazList = new ArrayList<StopTaz>();
    	
    		for (int oTaz = startOriginTaz; oTaz <= endOriginTaz; oTaz++){

    			if((oTaz==startOriginTaz)||(oTaz % 100 == 0))
    				logger.info("Thread "+threadName + " Period "+period+" Origin TAZ "+oTaz);

    			for (int dTaz = 1; dTaz <= maxTaz; dTaz++){	
	            	
	          	stopTazList.clear();
	            	
	           	//Stop TAZs
	           	for(int kTaz = 1; kTaz <= maxTaz; ++kTaz){
	            	
	           		//Calculate additional time to stop
	           		float ikTime = tazTimeSkims[period][oTaz][kTaz];
	           		float kjTime = tazTimeSkims[period][kTaz][dTaz];
	           		float totalIKJTime = ikTime + kjTime;
	           		float divertTime = totalIKJTime - tazTimeSkims[period][oTaz][dTaz];
	            	
	           		//if time is less than max diversion time (or the stop zone is the origin or destination zone), add zone and time to arraylist
	           		if( (divertTime < maxTimeDiversion) || (kTaz==oTaz) || (kTaz==dTaz)){
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
	           		tazsWithinOriginAndDestination[period][oTaz][dTaz] = new short[numberOfStops];
	           		//addTimeWithinOriginAndDestination[period][oTaz][dTaz] = new float[numberOfStops];
	           	
	           		for(int k = 0; k < numberOfStops; ++k){
	           			StopTaz stopTaz = stopTazList.get(k);
	           			tazsWithinOriginAndDestination[period][oTaz][dTaz][k] = (short) stopTaz.tazNumber;
	           			//addTimeWithinOriginAndDestination[period][oTaz][dTaz][k] = stopTaz.diversionTime;
	           		}
	           	}
       		}

       	}
    

    	
    	
    	}
    }
    
    /**
     * This method finds stop zones for each origin-destination zone pair and saves the zone number
     * and diversion time, sorted by distance from origin.
     * 
     */
    private void calculateTazsWithinDistanceThreshold(){
    	
    	
        tazsWithinOriginAndDestination = new short[NUM_PERIODS][maxTaz+1][maxTaz+1][];
        //addTimeWithinOriginAndDestination = new float[NUM_PERIODS][maxTaz+1][maxTaz+1][];
        int processors = Runtime.getRuntime().availableProcessors();
        //use 80% of the machine's processing power
        TAZ_CALCULATOR_THREADS = totalThreads;
        int chunkSize = (int) Math.floor(maxTaz / TAZ_CALCULATOR_THREADS);
       
    	logger.info("...Calculating TAZs within distance thresholds with "+TAZ_CALCULATOR_THREADS+ " threads ("+processors+" processors)");

    	for( int period = 0; period < NUM_PERIODS;++period ){

        	int endZone = 0;

        	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(TAZ_CALCULATOR_THREADS);

            for(int i = 0; i < TAZ_CALCULATOR_THREADS; ++ i){
        		
        		int startZone = endZone + 1;
        		
        		if(i==(TAZ_CALCULATOR_THREADS-1))
        			endZone = maxTaz;
        		else
        			endZone = startZone+chunkSize;
       		
        		executor.execute(new TazDistanceCalculatorThread( "Thread-"+i,period,startZone,endZone));
        	 	
        	}
            executor.shutdown();
            try{ 
            	executor.awaitTermination(60, TimeUnit.MINUTES);
            }catch(InterruptedException e){
            	throw new RuntimeException(e);
            }
        }
     }
    
    /**
     * Calculate zones sorted by time from origin. Always include intrazonal as within the maximum distance range.
     * 
     */
    public void calculateTazsByTimeFromOrigin(){
    	
       	ArrayList<StopTaz> stopTazList = new ArrayList<StopTaz>();
        
       	tazsByTimeFromOrigin = new short[NUM_PERIODS][maxTaz+1][];
 
    	for(int period = 0; period<NUM_PERIODS;++period){
           	for (int oTaz = 1; oTaz <= maxTaz; oTaz++){

            	stopTazList.clear();

           		for (int dTaz = 1; dTaz <= maxTaz; dTaz++){	
	            	
           			if((tazDistanceSkims[period][oTaz][dTaz]<maxDistanceToPickup)||(oTaz==dTaz)){
           				StopTaz stopTaz = new StopTaz();
           				stopTaz.tazNumber = dTaz;
           				stopTaz.diversionTime = tazTimeSkims[period][oTaz][dTaz];
           				stopTazList.add(stopTaz);
           			}
           		}
           		
           		if(!stopTazList.isEmpty()){
           			int numberOfStops = stopTazList.size();
           			Collections.sort(stopTazList);
           	       	tazsByTimeFromOrigin[period][oTaz] = new short[numberOfStops];
           	       	for(int i = 0; i < numberOfStops; ++i){
           	       		StopTaz stopTaz = stopTazList.get(i);
           	       		tazsByTimeFromOrigin[period][oTaz][i] = (short) stopTaz.tazNumber;
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
     * @param skimPeriod
     * @param origTaz
     * @param destTaz
     * @return The array of zones, or null if there are no zones within the max diversion time.
     */
   public short[] getZonesWithinMaxDiversionTime(int skimPeriod, int origTaz, int destTaz){
	   
	   return tazsWithinOriginAndDestination[skimPeriod][origTaz][destTaz];
	   
   }
   
   /**
    * Is the zone within the set of zones that is within maximum diversion time from the origin to the destination?
 	* 
    * @param skimPeriod
    * @param origTaz  The origin TAZ
    * @param destTaz  The destination TAZ
    * @param taz      The stop TAZ
    * @return A boolean indicating whether the zone is within the maximum deviation time from the origin to the destination.
    */
  public boolean stopZoneIsWithinMaxDiversionTime(int skimPeriod, int origTaz, int destTaz, int taz){
	   
	   short[] tazArray = getZonesWithinMaxDiversionTime(skimPeriod, origTaz, destTaz);
	   for(int i = 0; i < tazArray.length; ++i)
		   if(tazArray[i]==taz)
			   return true;
	   return false;
	   
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
	   
	   //return addTimeWithinOriginAndDestination[period][origTaz][destTaz];
	   logger.fatal("Error trying to call getDiversionTimes when additional time array not initialized");
	   throw new RuntimeException();
  }

  /**
   * Get a ragged array of zone numbers sorted by time from the origin. The array is ragged 
   * because it is capped by the maximum distance for hailing a TNC\TAXI.
   * 
   * @param period
   * @param origTaz
   * @return A sorted array of zone numbers, or null if there are no zones within the maximum distance.
   */
  public short[] getZoneNumbersSortedByTime(int period, int origTaz){
	  
	  return tazsByTimeFromOrigin[period][origTaz];
  }
  
  public float getTime(int period, int origTaz, int destTaz){
	  
	  return tazTimeSkims[period][origTaz][destTaz]; 
  }
  
  public float getDistance(int period, int origTaz, int destTaz){
	  
	  return tazDistanceSkims[period][origTaz][destTaz]; 
  }

}
