package org.sandag.abm.ctramp;


import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import org.sandag.abm.ctramp.ModelStructure;
/**
 * Decision making unit object for Stage 1 of the ARC Stop Departure/Duration Model. This
 * DMU contains all the getters specified in the UEC, i.e. all the "@" variables. 
 *  
 * @author J. Guo
 * edited for SANDAG  J. Freedman 
 *
 */
public class StopTimingStageOneDMU implements Serializable, VariableTable {

	protected transient Logger logger = Logger.getLogger(StopTimingStageOneDMU.class);

	protected HashMap<String, Integer> methodIndexMap;  
    protected IndexValues dmuIndex;
    
    private ModelStructure modelStructure;

    protected Household household;
    protected Person person;
    protected Tour tour;

	private double mainLegFFT;

	private double obLegFFT;

	private double ibLegFFT;
    
    
    public StopTimingStageOneDMU(ModelStructure modelStructure ){

    	this.modelStructure = modelStructure;
    	dmuIndex = new IndexValues();

   }
    
    public void setDmuIndexValues( int hhid, int homeTaz, int origTaz, int destTaz ) {

    	dmuIndex.setHHIndex( hhid );
        dmuIndex.setZoneIndex( homeTaz );
        dmuIndex.setOriginZone( origTaz );
        dmuIndex.setDestZone( destTaz );

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( household.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug ST1 UEC" );
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
    
    /*
     * get person level attributes 
     */
    public int getFullTimeWorker() {
    	if (person !=null)
    		return person.getPersonIsFullTimeWorker();
    	else
    		return 0;
    }

    /*
     * get tour level attributes 
     */
   
    public int getTourPurposeIsWork() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.WORK_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsSchool() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsUniversity() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsEscort() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsShopping() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsEatOut() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsOthMaint() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }

    public int getTourPurposeIsSocial() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }
    
    public int getTourPurposeIsOthDiscr() {
        return tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME ) ? 1 : 0;
    }
    

    public int getTourStartPeriod() {
    	return tour.getTourDepartPeriod();
    }
    
	// returns total tour length in # time periods
	// length of 0 means it tour starts and ends in the same time period
    public int getTourTime() {
    	int tourLength = tour.getTourArrivePeriod() - tour.getTourDepartPeriod(); 
    	return tourLength;
    }

    public int getNumOutboundStops() {
    	return tour.getNumOutboundStops();
    }

    public int getNumInboundStops() {
    	return tour.getNumInboundStops();
    }

    public int getNumWorkStopsOnLeg(boolean isOutbound) {
    	
    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is work, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.WORK_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    }
    
    public int getNumSchoolStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is school, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    	
    }
    public int getNumEscortStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is escort, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    	
    }
    
    public int getNumShoppingStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is shopping, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    	
    }
    
    public int getNumOtherMaintStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is other maintenance, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    	
    }
    
    public int getNumEatOutStopsOnLeg(boolean isOutbound) { 

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is eat out, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
   	
    }
    
    public int getNumSocialStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is social visit, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
   	
    }
    
    public int getNumOtherDiscrStopsOnLeg(boolean isOutbound) {

    	Stop[] stops = null;
    	int numStops;
    	
    	if ( isOutbound == true ) {
    		stops = tour.getOutboundStops();
    		numStops = tour.getNumOutboundStops();
    	}
    	else {
    		stops = tour.getInboundStops();
    		numStops = tour.getNumInboundStops();
    	}
    	
		int count = 0;
    	if ( stops != null ) {

        	for ( int s = 0; s < numStops; s++ )
        		// if stop purpose is other discretionary, increment count by 1
            	if ( stops[s].getDestPurpose().equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) == true )
            		count++;
    	}
    	return count;
    	
    }

    // count outbound stops by purpose
    public int getNumWorkStopsOnObLeg() {
    	return getNumWorkStopsOnLeg(true);
    }
    
    public int getNumSchoolStopsOnObLeg() {
    	return getNumSchoolStopsOnLeg(true);
    }
    
    public int getNumEscortStopsOnObLeg() {
    	return getNumEscortStopsOnLeg(true);
    }
    
    public int getNumShoppingStopsOnObLeg() {
    	return getNumShoppingStopsOnLeg(true);
    }
    
    public int getNumOtherMaintStopsOnObLeg() {
    	return getNumOtherMaintStopsOnLeg(true);    	
    }
    
    public int getNumEatOutStopsOnObLeg() {
    	return getNumEatOutStopsOnLeg(true);
    }
    
    public int getNumSocialStopsOnObLeg() {
    	return getNumSocialStopsOnLeg(true);
    }
    
    public int getNumOtherDiscrStopsOnObLeg() {
    	return getNumOtherDiscrStopsOnLeg(true);
    }
    
    // count inbound stops by purpose
    public int getNumWorkStopsOnIbLeg() {
    	return getNumWorkStopsOnLeg(false);
    }
    
    public int getNumSchoolStopsOnIbLeg() {
    	return getNumSchoolStopsOnLeg(false);
    }
    
    public int getNumEscortStopsOnIbLeg() {
    	return getNumEscortStopsOnLeg(false);
    }
    
    public int getNumShoppingStopsOnIbLeg() {
    	return getNumShoppingStopsOnLeg(false);
    }
    
    public int getNumOtherMaintStopsOnIbLeg() {
    	return getNumOtherMaintStopsOnLeg(false);    	
    }
    
    public int getNumEatOutStopsOnIbLeg() {
    	return getNumEatOutStopsOnLeg(false);
    }
    
    public int getNumSocialStopsOnIbLeg() {
    	return getNumSocialStopsOnLeg(false);
    }
    
    public int getNumOtherDiscrStopsOnIbLeg() {
    	return getNumOtherDiscrStopsOnLeg(false);
    }
    

    
    
    // calculate Fourier series terms here (instead of adding SIN/COS functions to expression calculator)  

    // compute SIN( two_Pi * tourStartPeriod / 48 )
    public double getFourierSin1() {
    	
    	double fractionPeriod = ( (double)tour.getTourDepartPeriod() ) / modelStructure.getNumberOfTimePeriods();
    	
    	return Math.sin( 2 * Math.PI *fractionPeriod );
    }
    
    // compute COS( two_Pi * tourStartPeriod / 48 )
    public double getFourierCos1() {
    	
    	double fractionPeriod = ( (double)tour.getTourDepartPeriod() ) / modelStructure.getNumberOfTimePeriods();
    	
    	return Math.cos( 2 * Math.PI *fractionPeriod );
    }
    
    // compute SIN( four_Pi * tourStartPeriod / 48 )
    public double getFourierSin2() {
    	
    	double fractionPeriod = ( (double)tour.getTourDepartPeriod() ) / modelStructure.getNumberOfTimePeriods();
    	
    	return Math.sin( 4 * Math.PI *fractionPeriod );
    }
    
    // compute COS( four_Pi * tourStartPeriod / 48 )
    public double getFourierCos2() {
    	
    	double fractionPeriod = ( (double)tour.getTourDepartPeriod() ) / modelStructure.getNumberOfTimePeriods();
    	
    	return Math.cos( 4 * Math.PI *fractionPeriod );
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

	public void setMainLegFFT(double mainLegFFT) {
		this.mainLegFFT = mainLegFFT;
	}
	
	public void setOutboundLegFFT(double obLegFFT) {
		this.obLegFFT = obLegFFT;
	}
	
	public void setInboundLegFFT(double ibLegFFT) {
		this.ibLegFFT = ibLegFFT;
	}

	public double getMainLegFFT() {
		return mainLegFFT;
	}
	
	public double getOutboundLegFFT() {
		return obLegFFT;
	}
	
	public double getInboundLegFFT() {
		return ibLegFFT;
	}

}
