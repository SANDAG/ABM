package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.ParkingChoiceDMU;
import org.sandag.abm.ctramp.TelecommuteDMU;

public class SandagTelecommuteDMU
        extends TelecommuteDMU
{

    public SandagTelecommuteDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

       methodIndexMap.put("getIncomeInDollars", 1);
       methodIndexMap.put("getNumberOfAdults", 2);
       methodIndexMap.put("getHasKids_0_5", 3);
       methodIndexMap.put("getHasKids_6_12", 4);
       methodIndexMap.put("getFemale", 5);
       methodIndexMap.put("getPersonType", 6);
       methodIndexMap.put("getNumberOfAutos", 7);
       methodIndexMap.put("getOccupation", 8);
       methodIndexMap.put("getPaysToPark", 9);
       methodIndexMap.put("getWorkDistance", 10);
      }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {

            case 1:
                return getIncomeInDollars();
            case 2:
                return getNumberOfAdults();
            case 3:
                return getHasKids_0_5();
            case 4:
                return getHasKids_6_12();
            case 5:
                return getFemale();
            case 6:
                return getPersonType();
            case 7:
                return getNumberOfAutos();
            case 8:
                return getOccupation();
            case 9:
                return getPaysToPark();
            case 10:
                return getWorkDistance();
          default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

}