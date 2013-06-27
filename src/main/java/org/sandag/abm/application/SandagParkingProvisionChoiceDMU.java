package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.ParkingProvisionChoiceDMU;

public class SandagParkingProvisionChoiceDMU
        extends ParkingProvisionChoiceDMU
{

    public SandagParkingProvisionChoiceDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getHhIncomeInDollars", 0);
        methodIndexMap.put("getWorkLocMgra", 1);
        methodIndexMap.put("getLsWgtAvgCostM", 2);
        methodIndexMap.put("getLsWgtAvgCostD", 3);
        methodIndexMap.put("getLsWgtAvgCostH", 4);
        methodIndexMap.put("getMgraParkArea", 5);
        methodIndexMap.put("getNumFreeHours", 6);
        methodIndexMap.put("getMStallsOth", 7);
        methodIndexMap.put("getMStallsSam", 8);
        methodIndexMap.put("getMParkCost", 9);
        methodIndexMap.put("getDStallsOth", 10);
        methodIndexMap.put("getDStallsSam", 11);
        methodIndexMap.put("getDParkCost", 12);
        methodIndexMap.put("getHStallsOth", 13);
        methodIndexMap.put("getHStallsSam", 14);
        methodIndexMap.put("getHParkCost", 15);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getIncomeInDollars();
            case 1:
                return getWorkLocMgra();
            case 2:
                return getLsWgtAvgCostM();
            case 3:
                return getLsWgtAvgCostD();
            case 4:
                return getLsWgtAvgCostH();
            case 5:
                return getMgraParkArea();
            case 6:
                return getNumFreeHours();
            case 7:
                return getMStallsOth();
            case 8:
                return getMStallsSam();
            case 9:
                return getMParkCost();
            case 10:
                return getDStallsOth();
            case 11:
                return getDStallsSam();
            case 12:
                return getDParkCost();
            case 13:
                return getHStallsOth();
            case 14:
                return getHStallsSam();
            case 15:
                return getHParkCost();


            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}