package org.sandag.abm.application;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.StopLocationDMU;

public class SandagStopLocationDMU
        extends StopLocationDMU
{

    public SandagStopLocationDMU(ModelStructure modelStructure)
    {
        super(modelStructure);
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getSlcSoaCorrectionsAlt", 0);
        methodIndexMap.put("getOrigToMgraDistanceAlt", 1);
        methodIndexMap.put("getMgraToDestDistanceAlt", 2);
        methodIndexMap.put("getOdDistance", 3);
        methodIndexMap.put("getTourModeIsWalk", 4);
        methodIndexMap.put("getTourModeIsBike", 5);
        methodIndexMap.put("getTourModeIsWalkTransit", 6);
        methodIndexMap.put("getWalkTransitAvailableAlt", 7);
        methodIndexMap.put("getLnSlcSizeAlt", 8);
        methodIndexMap.put("getStopPurpose", 9);
        methodIndexMap.put("getTourPurpose", 10);
        methodIndexMap.put("getTourMode", 11);
        methodIndexMap.put("getStopNumber", 12);
        methodIndexMap.put("getStopsOnHalfTour", 13);
        methodIndexMap.put("getInboundStop", 14);
        methodIndexMap.put("getTourIsJoint", 15);
        methodIndexMap.put("getFemale", 16);
        methodIndexMap.put("getAge", 17);
        methodIndexMap.put("getTourOrigToMgraDistanceAlt", 18);
        methodIndexMap.put("getMgraToTourDestDistanceAlt", 19);
        methodIndexMap.put("getMcLogsumAlt", 20);
        methodIndexMap.put("getSampleMgraAlt", 21);
        methodIndexMap.put("getLnSlcSizeSampleAlt", 22);
        methodIndexMap.put("getIncome", 23);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getSlcSoaCorrectionsAlt(arrayIndex);
            case 1:
                return getOrigToMgraDistanceAlt(arrayIndex);
            case 2:
                return getMgraToDestDistanceAlt(arrayIndex);
            case 3:
                return getOdDistance();
            case 4:
                return getTourModeIsWalk();
            case 5:
                return getTourModeIsBike();
            case 6:
                return getTourModeIsWalkTransit();
            case 7:
                return getWalkTransitAvailableAlt(arrayIndex);
            case 8:
                return getLnSlcSizeAlt(arrayIndex);
            case 9:
                return getStopPurpose();
            case 10:
                return getTourPurpose();
            case 11:
                return getTourMode();
            case 12:
                return getStopNumber();
            case 13:
                return getStopsOnHalfTour();
            case 14:
                return getInboundStop();
            case 15:
                return getTourIsJoint();
            case 16:
                return getFemale();
            case 17:
                return getAge();
            case 18:
                return getTourOrigToMgraDistanceAlt(arrayIndex);
            case 19:
                return getMgraToTourDestDistanceAlt(arrayIndex);
            case 20:
                return getMcLogsumAlt(arrayIndex);
            case 21:
                return getSampleMgraAlt(arrayIndex);
            case 22:
                return getLnSlcSizeSampleAlt(arrayIndex);
            case 23:
                return getIncomeInDollars();

            default:
                Logger logger = Logger.getLogger(StopLocationDMU.class);
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

}