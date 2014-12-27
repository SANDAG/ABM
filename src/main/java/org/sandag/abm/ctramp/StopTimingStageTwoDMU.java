package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * Decision making unit object for Stage 2 of the ARC Stop Departure/Duration Model. This
 * DMU contains all the getters specified in the UEC, i.e. all the "@" variables. 
 *  
 * @author J. Guo
 *
 */
public class StopTimingStageTwoDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger.getLogger(StopTimingStageTwoDMU.class);

	protected HashMap<String, Integer> methodIndexMap;  
    protected IndexValues dmuIndex;
    
    private ModelStructure modelStructure;

    protected Household household;
    protected Person person;
    protected Tour tour;
    protected Stop stop;
    
	private double tripTimeToStop;    
	private int legTotalTime;
    
    public StopTimingStageTwoDMU(ModelStructure modelStructure ){

    	this.modelStructure = modelStructure;
    	dmuIndex = new IndexValues();

   }
    
    public void setDmuIndexValues( int hhid, int origTaz, int destTaz ) {

    	dmuIndex.setHHIndex( hhid );
        dmuIndex.setZoneIndex( destTaz );
        dmuIndex.setOriginZone( origTaz );
        dmuIndex.setDestZone( destTaz );
        dmuIndex.setStopZone( destTaz );

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( household.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug ST2 UEC" );
        }

    }
    
    public IndexValues getIndexValues() {
        return dmuIndex; 
    }
    
    public void setHouseholdObject ( Household household) {
        this.household = household;
    }

    public void setPersonObject ( Person person ) {
        this.person = person;
    }

    public void setTourObject ( Tour tour ) {
        this.tour = tour;
    }

    public void setStopObject ( Stop stop ) {
        this.stop = stop;
    }

    /*
     * get stop level attributes 
     */
   
    public int getStopPurposeIsWork() {
        return ( stop.getStopPurposeIndex() == ModelStructure.WORK_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsSchool() {
        return ( stop.getStopPurposeIndex() == ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsUniversity() {
        return ( stop.getStopPurposeIndex() == ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsEatOut() {
        return ( stop.getStopPurposeIndex() == ModelStructure.EAT_OUT_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsEscort() {
        return ( stop.getStopPurposeIndex() == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsOthMaint() {
        return ( stop.getStopPurposeIndex() == ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsShopping() {
        return ( stop.getStopPurposeIndex() == ModelStructure.SHOPPING_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }
    
    public int getStopPurposeIsSocial() {
        return ( stop.getStopPurposeIndex() == ModelStructure.VISITING_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }

    public int getStopPurposeIsOthDiscr() {
        return ( stop.getStopPurposeIndex() == ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_INDEX ) ? 1 : 0;
    }
        
    public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
    }
    
    public int getAssignmentIndexValue(String variableName) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue) {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue) {
        throw new UnsupportedOperationException();
    }

	public void setTripTimeToStop(double tripFFT) {
		this.tripTimeToStop = tripFFT;
	}
	
	public double getTripTimeToStop() {
		return tripTimeToStop;
	}
	
	public void setLegTotalDuration(int legTime) {
		this.legTotalTime = legTime;		
	}

	public int getLegTotalDuration() {
		return legTotalTime;
	}
}
