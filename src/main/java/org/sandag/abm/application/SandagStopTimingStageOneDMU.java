package org.sandag.abm.application;

import java.util.HashMap;

import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.StopTimingStageOneDMU;


public class SandagStopTimingStageOneDMU extends StopTimingStageOneDMU {

	public SandagStopTimingStageOneDMU(ModelStructure modelStructure) {
		super(modelStructure);
        setupMethodIndexMap();
	}
	
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getMainLegFFT", 0);
        methodIndexMap.put( "getTourPurposeIsWork" ,1);
        methodIndexMap.put( "getTourPurposeIsSchool", 2);
        methodIndexMap.put( "getTourPurposeIsUniversity", 3);
        methodIndexMap.put( "getTourPurposeIsEscort", 4);
        methodIndexMap.put( "getFullTimeWorker", 5);
        methodIndexMap.put( "getFourierSin1", 6);
        methodIndexMap.put( "getFourierSin2", 7);
        methodIndexMap.put( "getFourierCos1", 8);
        methodIndexMap.put( "getFourierCos2", 9);
        methodIndexMap.put( "getOutboundLegFFT", 10);
        methodIndexMap.put( "getNumOutboundStops", 11);
        methodIndexMap.put( "getNumWorkStopsOnObLeg", 12);
        methodIndexMap.put( "getNumSchoolStopsOnObLeg", 13);
        methodIndexMap.put( "getNumEscortStopsOnObLeg", 14);
        methodIndexMap.put( "getNumShoppingStopsOnObLeg", 15);
        methodIndexMap.put( "getNumOtherMaintStopsOnObLeg", 16);
        methodIndexMap.put( "getNumEatOutStopsOnObLeg", 17);
        methodIndexMap.put( "getNumSocialStopsOnObLeg", 18);
        methodIndexMap.put( "getNumOtherDiscrStopsOnObLeg", 19);
        methodIndexMap.put( "getInboundLegFFT", 20);
        methodIndexMap.put( "getNumInboundStops", 21);
        methodIndexMap.put( "getNumWorkStopsOnIbLeg", 22);
        methodIndexMap.put( "getNumSchoolStopsOnIbLeg", 23);
        methodIndexMap.put( "getNumEscortStopsOnIbLeg", 24);
        methodIndexMap.put( "getNumOtherMaintStopsOnIbLeg", 25);
        methodIndexMap.put( "getNumEatOutStopsOnIbLeg", 26);
        methodIndexMap.put( "getNumShoppingStopsOnIbLeg", 27);
        methodIndexMap.put( "getNumSocialStopsOnIbLeg", 28);
        methodIndexMap.put( "getNumOtherDiscrStopsOnIbLeg", 29);
        methodIndexMap.put( "getTourPurposeIsShopping",30);
        methodIndexMap.put( "getTourPurposeIsEatOut",31);
        methodIndexMap.put( "getTourPurposeIsOthMaint",32);
        methodIndexMap.put( "getTourPurposeIsSocial",33);
        methodIndexMap.put( "getTourPurposeIsOthDiscr",34);
        methodIndexMap.put( "getTourTime", 35);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
        
            case 0: return getMainLegFFT();
            case 1: return getTourPurposeIsWork();
            case 2: return getTourPurposeIsSchool();
            case 3: return getTourPurposeIsUniversity();
            case 4: return getTourPurposeIsEscort();
            case 5: return getFullTimeWorker();
            case 6: return getFourierSin1();
            case 7: return getFourierSin2();
            case 8: return getFourierCos1();
            case 9: return getFourierCos2();
            case 10: return getOutboundLegFFT();
            case 11: return getNumOutboundStops();
            case 12: return getNumWorkStopsOnObLeg();
            case 13: return getNumSchoolStopsOnObLeg();
            case 14: return getNumEscortStopsOnObLeg();
            case 15: return getNumShoppingStopsOnObLeg();
            case 16: return getNumOtherMaintStopsOnObLeg();
            case 17: return getNumEatOutStopsOnObLeg();
            case 18: return getNumSocialStopsOnObLeg();
            case 19: return getNumOtherDiscrStopsOnObLeg();
            case 20: return getInboundLegFFT();
            case 21: return getNumInboundStops();
            case 22: return getNumWorkStopsOnIbLeg();
            case 23: return getNumSchoolStopsOnIbLeg();
            case 24: return getNumEscortStopsOnIbLeg();
            case 25: return getNumOtherMaintStopsOnIbLeg();
            case 26: return getNumEatOutStopsOnIbLeg();
            case 27: return getNumShoppingStopsOnIbLeg();
            case 28: return getNumSocialStopsOnIbLeg();
            case 29: return getNumOtherDiscrStopsOnIbLeg();
            case 30: return getTourPurposeIsShopping();
            case 31: return getTourPurposeIsEatOut();
            case 32: return getTourPurposeIsOthMaint();
            case 33: return getTourPurposeIsSocial();
            case 34: return getTourPurposeIsOthDiscr();
            case 35: return getTourTime();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");        
        }
    }

}
