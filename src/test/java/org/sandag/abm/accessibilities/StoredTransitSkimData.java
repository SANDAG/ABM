package org.sandag.abm.accessibilities;


public class StoredTransitSkimData
{

    private static StoredTransitSkimData objInstance = null;
    private int numSets;

    // these arrays are shared by McLogsumsAppender objects and are used by wtw, wtd, and dtw calculators.
    private double[][][][][] storedWtwDepartPeriodTapTapSkims;
    private double[][][][][] storedWtdDepartPeriodTapTapSkims;
    private double[][][][][] storedDtwDepartPeriodTapTapSkims;
        
    
    private StoredTransitSkimData(){
    	this.numSets = numSets;
    }
    
    public static synchronized StoredTransitSkimData getInstance( int numSets, int numPeriods, int maxTap )
    {
        if (objInstance == null) {
            objInstance = new StoredTransitSkimData();
            objInstance.setupStoredDataArrays(numSets, numPeriods, maxTap );
            return objInstance;
        }
        else {
            return objInstance;
        }
    }    
    
    private void setupStoredDataArrays(int numSets, int numPeriods, int maxTap ){        
        storedWtwDepartPeriodTapTapSkims = new double[numSets][numPeriods + 1][maxTap + 1][][];
        storedWtdDepartPeriodTapTapSkims = new double[numSets][numPeriods + 1][maxTap + 1][][];
        storedDtwDepartPeriodTapTapSkims = new double[numSets][numPeriods + 1][maxTap + 1][][];
    }
    
    public double[][][][][] getStoredWtwDepartPeriodTapTapSkims() {
        return storedWtwDepartPeriodTapTapSkims;
    }
    
    public double[][][][][] getStoredWtdDepartPeriodTapTapSkims() {
        return storedWtdDepartPeriodTapTapSkims;
    }
    
    public double[][][][][] getStoredDtwDepartPeriodTapTapSkims() {
        return storedDtwDepartPeriodTapTapSkims;
    }
    
    
}
