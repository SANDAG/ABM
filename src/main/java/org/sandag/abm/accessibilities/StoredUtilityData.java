package org.sandag.abm.accessibilities;

public final class StoredUtilityData
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
        storedDepartPeriodTapTapUtils = new double[numAccEgrSegments + 1][numPeriods + 1][maxTap + 1][maxTap + 1][];
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

    public void deallocateArrays()
    {

        for (int i = 0; i < storedWalkAccessUtils.length; i++)
        {
            for (int j = 0; j < storedWalkAccessUtils[i].length; j++)
            {
                storedWalkAccessUtils[i][j] = null;
            }
            storedWalkAccessUtils[i] = null;
        }
        storedWalkAccessUtils = null;

        for (int i = 0; i < storedDriveAccessUtils.length; i++)
        {
            for (int j = 0; j < storedDriveAccessUtils[i].length; j++)
            {
                storedDriveAccessUtils[i][j] = null;
            }
            storedDriveAccessUtils[i] = null;
        }
        storedDriveAccessUtils = null;

        for (int i = 0; i < storedWalkEgressUtils.length; i++)
        {
            for (int j = 0; j < storedWalkEgressUtils[i].length; j++)
            {
                storedWalkEgressUtils[i][j] = null;
            }
            storedWalkEgressUtils[i] = null;
        }
        storedWalkEgressUtils = null;

        for (int i = 0; i < storedDriveEgressUtils.length; i++)
        {
            for (int j = 0; j < storedDriveEgressUtils[i].length; j++)
            {
                storedDriveEgressUtils[i][j] = null;
            }
            storedDriveEgressUtils[i] = null;
        }
        storedDriveEgressUtils = null;

        for (int i = 0; i < storedDepartPeriodTapTapUtils.length; i++)
        {
            for (int j = 0; j < storedDepartPeriodTapTapUtils[i].length; j++)
            {
                for (int k = 0; k < storedDepartPeriodTapTapUtils[i][j].length; k++)
                {
                    for (int l = 0; l < storedDepartPeriodTapTapUtils[i][j][k].length; l++)
                    {
                        storedDepartPeriodTapTapUtils[i][j][k][l] = null;
                    }
                    storedDepartPeriodTapTapUtils[i][j][k] = null;
                }
                storedDepartPeriodTapTapUtils[i][j] = null;
            }
            storedDepartPeriodTapTapUtils[i] = null;
        }
        storedDepartPeriodTapTapUtils = null;

    }
}
