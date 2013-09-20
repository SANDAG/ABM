package org.sandag.abm.accessibilities;

public class StoredTransitSkimData
{

    private static StoredTransitSkimData objInstance = null;

    // these arrays are shared by McLogsumsAppender objects and are used by wtw, wtd, and dtw calculators.
    private double[][][][][]             storedWtwDepartPeriodTapTapSkims;
    private double[][][][][]             storedWtdDepartPeriodTapTapSkims;
    private double[][][][][]             storedDtwDepartPeriodTapTapSkims;

    private StoredTransitSkimData()
    {
    }

    public static synchronized StoredTransitSkimData getInstance(int numServiceTypes,
            int numPeriods, int maxTap)
    {
        if (objInstance == null)
        {
            objInstance = new StoredTransitSkimData();
            objInstance.setupStoredDataArrays(numServiceTypes, numPeriods, maxTap);
            return objInstance;
        } else
        {
            return objInstance;
        }
    }

    private void setupStoredDataArrays(int numServiceTypes, int numPeriods, int maxTap)
    {
        storedWtwDepartPeriodTapTapSkims = new double[numServiceTypes + 1][numPeriods + 1][maxTap + 1][][];
        storedWtdDepartPeriodTapTapSkims = new double[numServiceTypes + 1][numPeriods + 1][maxTap + 1][][];
        storedDtwDepartPeriodTapTapSkims = new double[numServiceTypes + 1][numPeriods + 1][maxTap + 1][][];
    }

    public double[][][][][] getStoredWtwDepartPeriodTapTapSkims()
    {
        return storedWtwDepartPeriodTapTapSkims;
    }

    public double[][][][][] getStoredWtdDepartPeriodTapTapSkims()
    {
        return storedWtdDepartPeriodTapTapSkims;
    }

    public double[][][][][] getStoredDtwDepartPeriodTapTapSkims()
    {
        return storedDtwDepartPeriodTapTapSkims;
    }

}
