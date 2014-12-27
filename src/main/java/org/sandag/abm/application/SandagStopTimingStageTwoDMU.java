package org.sandag.abm.application;

import java.util.HashMap;

import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.StopTimingStageTwoDMU;

public class SandagStopTimingStageTwoDMU extends StopTimingStageTwoDMU {

	public SandagStopTimingStageTwoDMU(ModelStructure modelStructure) {
		super(modelStructure);
        setupMethodIndexMap();
	}
	
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put( "getStopPurposeIsWork", 0);
        methodIndexMap.put( "getStopPurposeIsSchool" ,1);
        methodIndexMap.put( "getStopPurposeIsUniversity", 2);
        methodIndexMap.put( "getStopPurposeIsEatOut", 3);
        methodIndexMap.put( "getStopPurposeIsEscort", 4);
        methodIndexMap.put( "getStopPurposeIsOthMaint", 5);
        methodIndexMap.put( "getStopPurposeIsShopping", 6);
        methodIndexMap.put( "getStopPurposeIsSocial", 7);
        methodIndexMap.put( "getStopPurposeIsOthDiscr", 8);
        methodIndexMap.put( "getTripTimeToStop", 9);
        methodIndexMap.put( "getLegTotalDuration", 10);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){

            case 0: return getStopPurposeIsWork();
            case 1: return getStopPurposeIsSchool();
            case 2: return getStopPurposeIsUniversity();
            case 3: return getStopPurposeIsEatOut();
            case 4: return getStopPurposeIsEscort();
            case 5: return getStopPurposeIsOthMaint();
            case 6: return getStopPurposeIsShopping();
            case 7: return getStopPurposeIsSocial();
            case 8: return getStopPurposeIsOthDiscr();
            case 9: return getTripTimeToStop();
            case 10: return getLegTotalDuration();
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");        
        }
    }
    
}


