package org.sandag.abm.active.sandag;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.DestChoiceTwoStageSoaTazDistanceUtilityDMU;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.StopLocationDMU;
import org.sandag.abm.ctramp.Tour;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class SandagBikePathChoiceDmu implements VariableTable,Serializable {
	
	private final transient Logger logger = Logger.getLogger(SandagBikePathChoiceDmu.class);

    protected final Map<String, Integer> methodIndexMap;
    private final IndexValues dmuIndex;

    private int personIsFemale;
    private int isInboundTrip;
    private int tourPurpose;
    private SandagBikePathAlternatives paths;
    private int pathCount = 0;
    
    
    public SandagBikePathChoiceDmu() {
        methodIndexMap = new HashMap<>();
        dmuIndex = new IndexValues();
        setupMethodIndexMap();
    }

    //not needed right now
//    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex, boolean debug) {
//        dmuIndex.setHHIndex(hhIndex);
//        dmuIndex.setZoneIndex(zoneIndex);
//        dmuIndex.setOriginZone(origIndex);
//        dmuIndex.setDestZone(destIndex);
//
//        dmuIndex.setDebug(false);
//        dmuIndex.setDebugLabel("");
//        if (debug)  {
//            dmuIndex.setDebug(true);
//            dmuIndex.setDebugLabel("Debug Path Choice UEC");
//        }
//
//    }

    public void setPersonIsFemale(boolean isFemale) {
        personIsFemale = isFemale ? 1 : 0;
    }
    
    public void setIsInboundTrip(boolean isInboundTrip) {
    	this.isInboundTrip = isInboundTrip ? 1 : 0; 
    }
    
    public void setTourPurpose(int tourPurpose) {
    	this.tourPurpose = tourPurpose;
    }
    
    public void setPathAlternatives(SandagBikePathAlternatives paths) {
    	this.paths = paths;
    	pathCount = paths.getPathCount();
    }
    
    public SandagBikePathAlternatives getPathAlternatives() {
    	return paths;
    }
    
    public int getFemale() {
    	return personIsFemale;
    }
    
    public int getInbound() {
    	return isInboundTrip;
    }
    
    public int getTourPurpose() {
    	return tourPurpose;
    }
    
    public double getSizeAlt(int path) {
    	return paths.getSizeAlt(path-1);
    }
    
    public double getDistanceAlt(int path) {
    	return paths.getDistanceAlt(path-1);
    }
    
    public double getDistanceClass1Alt(int path) {
    	return paths.getDistanceClass1Alt(path-1);
    }
    
    public double getDistanceClass2Alt(int path) {
    	return paths.getDistanceClass2Alt(path-1);
    }
    
    public double getDistanceClass3Alt(int path) {
    	return paths.getDistanceClass3Alt(path-1);
    }
    
    public double getDistanceArtNoLaneAlt(int path) {
    	return paths.getDistanceArtNoLaneAlt(path-1);
    }
    
    public double getDistanceCycleTrackAlt(int path) {
    	return paths.getDistanceCycleTrackAlt(path-1);
    }
    
    public double getDistanceBikeBlvdAlt(int path) {
    	return paths.getDistanceBikeBlvdAlt(path-1);
    }
    
    public double getDistanceWrongWayAlt(int path) {
    	return paths.getDistanceWrongWayAlt(path-1);
    }
    
    public double getGainAlt(int path) {
    	return paths.getGainAlt(path-1);
    }
    
    public double getTurnsAlt(int path) {
    	return paths.getTurnsAlt(path-1);
    }
    
    public IndexValues getDmuIndexValues() {
    	return dmuIndex;
    }

	@Override
	public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
	}

	@Override
	public int getAssignmentIndexValue(String variableName) {
        throw new UnsupportedOperationException();
	}

	@Override
	public double getValueForIndex(int variableIndex) {
        throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String variableName, double variableValue) {
        throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(int variableIndex, double variableValue) {
        throw new UnsupportedOperationException();
	}

    private void setupMethodIndexMap() {
        methodIndexMap.clear();
        
        methodIndexMap.put("getSizeAlt", 0);
        methodIndexMap.put("getDistanceAlt", 1);
        methodIndexMap.put("getDistanceClass1Alt", 2);
        methodIndexMap.put("getDistanceClass2Alt", 3);
        methodIndexMap.put("getDistanceClass3Alt", 4);
        methodIndexMap.put("getDistanceArtNoLaneAlt", 5);
        methodIndexMap.put("getDistanceCycleTrackAlt", 6);
        methodIndexMap.put("getDistanceBikeBlvdAlt", 7);
        methodIndexMap.put("getDistanceWrongWayAlt", 8);
        methodIndexMap.put("getGainAlt", 9);
        methodIndexMap.put("getTurnsAlt", 10);
        
        methodIndexMap.put("getFemale", 11);
        methodIndexMap.put("getInbound", 12);
        methodIndexMap.put("getTourPurpose", 13);

    }

    
    public double getValueForIndex(int variableIndex, int arrayIndex)  {
        switch (variableIndex) {
            case 0  : return getSizeAlt(arrayIndex);
            case 1  : return getDistanceAlt(arrayIndex);
            case 2  : return getDistanceClass1Alt(arrayIndex);
            case 3  : return getDistanceClass2Alt(arrayIndex);
            case 4  : return getDistanceClass3Alt(arrayIndex);
            case 5  : return getDistanceArtNoLaneAlt(arrayIndex);
            case 6  : return getDistanceCycleTrackAlt(arrayIndex);
            case 7  : return getDistanceBikeBlvdAlt(arrayIndex);
            case 8  : return getDistanceWrongWayAlt(arrayIndex);
            case 9  : return getGainAlt(arrayIndex);
            case 10 : return getTurnsAlt(arrayIndex);
            case 11 : return getFemale();
            case 12 : return getInbound();
            case 13 : return getTourPurpose();
                
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
        
    }

}
