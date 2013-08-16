package org.sandag.abm.accessibilities;

public class StoredUtilityData
{

    private static StoredUtilityData objInstance = null;

    // these arrays are shared by multiple BestTransitPathCalculator objects in
    // a distributed computing environment
    private double[][][]             storedWalkAccessUtils;
    private double[][][]             storedDriveAccessUtils;
    private double[][][]             storedWalkEgressUtils;
    private double[][][]             storedDriveEgressUtils;
    private double[][][][][]         storedDepartPeriodTapTapUtils;

    private StoredUtilityData()
    {
    }

    public static synchronized StoredUtilityData getInstance(int maxMgra, int maxTap, int maxTaz,
            int numAccEgrSegments, int numPeriods)
    {
        if (objInstance == null)
        {
            objInstance = new StoredUtilityData();
            objInstance.setupStoredDataArrays(maxMgra, maxTap, maxTaz, numAccEgrSegments,
                    numPeriods);
            return objInstance;
        } else
        {
            return objInstance;
        }
    }

    private void setupStoredDataArrays(int maxMgra, int maxTap, int maxTaz, int numAccEgrSegments,
            int numPeriods)
    {
        storedWalkAccessUtils = new double[maxMgra + 1][maxTap + 1][];
        storedDriveAccessUtils = new double[maxTaz + 1][maxTap + 1][];
        storedWalkEgressUtils = new double[maxTap + 1][maxMgra + 1][];
        storedDriveEgressUtils = new double[maxTap + 1][maxTaz + 1][];
        storedDepartPeriodTapTapUtils = new double[numAccEgrSegments + 1][numPeriods + 1][][][];
    }

    public double[][][] getStoredWalkAccessUtils()
    {
        return storedWalkAccessUtils;
    }

    public double[][][] getStoredDriveAccessUtils()
    {
        return storedDriveAccessUtils;
    }

    public double[][][] getStoredWalkEgressUtils()
    {
        return storedWalkEgressUtils;
    }

    public double[][][] getStoredDriveEgressUtils()
    {
        return storedDriveEgressUtils;
    }

    public double[][][][][] getStoredDepartPeriodTapTapUtils()
    {
        return storedDepartPeriodTapTapUtils;
    }

}
