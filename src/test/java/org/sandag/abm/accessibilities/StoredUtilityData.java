package org.sandag.abm.accessibilities;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;


public class StoredUtilityData
{

    private static StoredUtilityData objInstance = null;
    public static final float		 default_utility = -999;

    // these arrays are shared by multiple BestTransitPathCalculator objects in a distributed computing environment
    private float[][] storedWalkAccessUtils;	// dim#1: MGRA id, dim#2: TAP id
    private float[][] storedDriveAccessUtils; // dim#1: TAZ id, dim#2: TAP id
    private float[][] storedWalkEgressUtils; 	// dim#1: TAP id, dim#2: MGRA id
    private float[][] storedDriveEgressUtils; // dim#1: TAP id, dim#2: TAZ id
    
    // {0:WTW, 1:WTD, 2:DTW} -> TOD period number -> pTAP*100000+aTAP -> utility
    private HashMap<Integer,HashMap<Integer,ConcurrentHashMap<Long,float[]>>> storedDepartPeriodTapTapUtils;
       
    
    private StoredUtilityData(){
    }
    
    public static synchronized StoredUtilityData getInstance( int maxMgra, int maxTap, int maxTaz, int[] accEgrSegments, int[] periods)
    {
        if (objInstance == null) {
            objInstance = new StoredUtilityData();
            objInstance.setupStoredDataArrays( maxMgra, maxTap, maxTaz, accEgrSegments, periods);
            return objInstance;
        }
        else {
            return objInstance;
        }
    }    
    
    private void setupStoredDataArrays( int maxMgra, int maxTap, int maxTaz, int[] accEgrSegments, int[] periods){        
    	// dimension the arrays
    	storedWalkAccessUtils = new float[maxMgra + 1][maxTap + 1];
        storedDriveAccessUtils = new float[maxTaz + 1][maxTap + 1];
        storedWalkEgressUtils = new float[maxTap + 1][maxMgra + 1];
        storedDriveEgressUtils = new float[maxTap + 1][maxTaz + 1];
        // assign default values to array elements
        for (int i=0; i<=maxMgra; i++)
        	for (int j=0; j<=maxTap; j++) {
        		storedWalkAccessUtils[i][j] = default_utility;
        		storedWalkEgressUtils[j][i] = default_utility;
        	}
        // assign default values to array elements
        for (int i=0; i<=maxTaz; i++)
        	for (int j=0; j<=maxTap; j++) {
        		storedDriveAccessUtils[i][j] = default_utility;
        		storedDriveEgressUtils[j][i] = default_utility;
        	}
        
        //put into concurrent hashmap
        storedDepartPeriodTapTapUtils = new HashMap<Integer,HashMap<Integer,ConcurrentHashMap<Long,float[]>>>();
        for(int i=0; i<accEgrSegments.length; i++) {
        	storedDepartPeriodTapTapUtils.put(accEgrSegments[i], new HashMap<Integer,ConcurrentHashMap<Long,float[]>>());
        	for(int j=0; j<periods.length; j++) {
        		HashMap<Integer,ConcurrentHashMap<Long,float[]>> hm = storedDepartPeriodTapTapUtils.get(accEgrSegments[i]);
        		hm.put(periods[j], new ConcurrentHashMap<Long,float[]>()); //key method paTapKey below
        	}
    	}        
    }
    
    public float[][] getStoredWalkAccessUtils() {
        return storedWalkAccessUtils;
    }
    
    public float[][] getStoredDriveAccessUtils() {
        return storedDriveAccessUtils;
    }
    
    public float[][] getStoredWalkEgressUtils() {
        return storedWalkEgressUtils;
    }
    
    public float[][]getStoredDriveEgressUtils() {
        return storedDriveEgressUtils;
    }
    
    public HashMap<Integer,HashMap<Integer,ConcurrentHashMap<Long,float[]>>> getStoredDepartPeriodTapTapUtils() {
        return storedDepartPeriodTapTapUtils;
    }
    
    //create p to a hash key - up to 99,999 
    public long paTapKey(int p, int a) {
    	return(p * 100000 + a);
    }
    
    //convert double array to float array
    public float[] d2f(double[] d) {
    	float[] f = new float[d.length];
    	for(int i=0; i<d.length; i++) {
    		f[i] = (float)d[i];
    	}
    	return(f);
    }
    
    public void deallocateArrays()
    {

        for (int i = 0; i < storedWalkAccessUtils.length; i++)
        {
         storedWalkAccessUtils[i] = null;
        }
        storedWalkAccessUtils = null;

        for (int i = 0; i < storedDriveAccessUtils.length; i++)
        {
            storedDriveAccessUtils[i] = null;
        }
        storedDriveAccessUtils = null;

        for (int i = 0; i < storedWalkEgressUtils.length; i++)
        {
            storedWalkEgressUtils[i] = null;
        }
        storedWalkEgressUtils = null;

        for (int i = 0; i < storedDriveEgressUtils.length; i++)
        {
            storedDriveEgressUtils[i] = null;
        }
        storedDriveEgressUtils = null;

        storedDepartPeriodTapTapUtils = null;

    }
    
}
