package org.sandag.abm.dta.postprocessing;

import java.io.PrintWriter;
import java.io.Serializable;

public class dtaTrip implements Serializable {

	private int id;
    private int hhid;
    private int persid;
    private int tourid;
    private int originTaz;
    private int destinationTaz;
    private int originMGRA;
    private int destinationMGRA;
    private int originNode;
    private int destinationNode;
    private String vehicleType;
    private int vehOccupancy;
    private int tollEligible;
    private String marketSegment;
    private int broadPeriod;
    private int detailedPeriod;
    private int dtaPeriod;
    private int driver;
    private double expansionFactor;

    	
       
    /**
    * Default constructor; nothing initialized.
    */
    public dtaTrip(){
        	
    }
    /**
     * Initialize a trip will zero values for all fields
     */
    public void initializeTrip() {
       	this.id=0;
       	this.hhid=0;
       	this.persid=0;
       	this.tourid=0;
       	this.originTaz=0;
       	this.destinationTaz=0;
       	this.originMGRA=0;
       	this.destinationMGRA=0;
       	this.originNode=0;
       	this.destinationNode=0;
       	this.vehicleType="na";
       	this.vehOccupancy=0;
       	this.tollEligible=0;
       	this.marketSegment="na";
       	this.broadPeriod=0;
       	this.detailedPeriod=0;
       	this.dtaPeriod=0;
       	this.driver=-1;
       	this.expansionFactor=1.0;
        			
    }
    /**
     * @return the household id
     */
    public int getHHId() {
    	return hhid;
    }
    /**
     * @param hhid the household id to set
     */
    public void setHHId(int hhid) {
    	this.hhid = hhid;
    }
    /**
     * @return the person id
     */
    public int getPersonId() {
    	return persid;
    }
    /**
     * @param persid the person id to set
     */
    public void setPersonId(int persid) {
    	this.persid = persid;
    }
    /**
     * @return the tour id
     */
    public int getTourId() {
    return tourid;
    }
    /**
     * @param tourid the tour id to set
     */
    public void setTourId(int tourid) {
    	this.tourid = tourid;
    }                         
    /**
     * @return the id
     */
    public int getId() {
    	return id;
    }
   	/**
   	 * @param id the id to set
   	 */
   	public void setId(int id) {
   		this.id = id;
   	}
   	
   	/**
   	 * @return the originTaz
   	 */
   	public int getOriginTaz() {
   		return originTaz;
   	}
   	/**
   	 * @param originTaz the originTaz to set
   	 */
   	public void setOriginTaz(int originTaz) {
   		this.originTaz = originTaz;
   	}
   	/**
   	 * @return the destinationTaz
   	 */
   	public int getDestinationTaz() {
   		return destinationTaz;
   	}
   	/**
   	 * @param destinationTaz the destinationTaz to set
   	 */
   	public void setDestinationTaz(int destinationTaz) {
   		this.destinationTaz = destinationTaz;
   	}
   	
   	/**
   	 * @return the originMGRA
   	 */
   	public int getOriginMGRA() {
   		return originMGRA;
   	}
   	/**
   	 * @param originMGRA the originMGRA to set
   	 */
   	public void setOriginMGRA(int originMGRA) {
   		this.originMGRA = originMGRA;
   	}
   	/**
   	 * @return the destinationMGRA
   	 */
   	public int getDestinationMGRA() {
   		return destinationMGRA;
   	}
   	/**
   	 * @param destinationMGRA the destinationMGRA to set
   	 */
   	public void setDestinationMGRA(int destinationMGRA) {
   		this.destinationMGRA = destinationMGRA;
   	}
   	
   	/**
   	 * @return the originNode
   	 */
   	public int getOriginNode() {
   		return originNode;
   	}
   	/**
   	 * @param originNode the originNode to set
   	 */
   	public void setOriginNode(int originNode) {
   		this.originNode = originNode;
   	}
   	/**
   	 * @return the destinationNode
   	 */
   	public int getDestinationNode() {
   		return destinationNode;
   	}
   	/**
   	 * @param destinationMGRA the destinationMGRA to set
   	 */
   	public void setDestinationNode(int destinationNode) {
   		this.destinationNode = destinationNode;
   	}
   	/**
   	 * set trip mode values based on trip mode in input file
   	 */
   	public void setTripMode(int mode) {
   		if(mode<=8||mode>=27){
   			setVehicleType("passengerCar");
			setVehicleOccupancy(1);
   			setTollEligible(0);
   			if(mode==2){
   				setTollEligible(1);
   			}
   			if((mode>=3 && mode<=5)||mode==27){
   				setVehicleOccupancy(2);
   			}
   			if(mode==5){
   				setTollEligible(1);
   			}
   			if(mode>=6 && mode<=8){
   				setVehicleOccupancy(3);
   			}
   			if(mode==8){
   				setTollEligible(1);   				
   			}
   		}
   		if(mode>8 && mode<11){
   			setVehicleType("nonMotorized");
   			setVehicleOccupancy(0);
   			setTollEligible(0);
   		}
   		if(mode>=11 && mode<16){
   			setVehicleType("WalkTransit");
   			setVehicleOccupancy(0);
   			setTollEligible(0);
   		}
   		if(mode>=16 && mode<26){
   			setVehicleType("DriveTransit");
   			setVehicleOccupancy(1);
   			setTollEligible(0);
   		}
   		if(mode==26){
   			setVehicleType("SchoolBus");
   		}
   	}
   	/**
   	 * @return the person number of the driver
   	 */
   	public int getTourDriver() {
   		return driver;
   	}
   	/**
   	 * @param driver the tour driver to set
   	 */
   	public void setTourDriver(int tourDriver) {
   		this.driver = tourDriver;
   	}
   	
   	/**
   	 * @return the vehicleType
   	 */
   	public String getVehicleType() {
   		return vehicleType;
   	}
   	/**
   	 * @param vehicleType the vehicleType to set
   	 */
   	public void setVehicleType(String vehicleType) {
   		this.vehicleType = vehicleType;
   	}
   	/**
   	 * @return the vehicleOccupancy
   	 */
   	public int getVehicleOccupancy() {
   		return vehOccupancy;
   	}
   	/**
   	 * @param vecOccupancy the vehOccupancy to set
   	 */
   	public void setVehicleOccupancy(int vehOccupancy) {
   		this.vehOccupancy = vehOccupancy;
   	}
   	/**
   	 * @return the tollEligibility
   	 */
   	public int getTollEligible() {
   		return tollEligible;
   	}
   	/**
   	 * @param tollEligible the tollEligible to set
   	 */
   	public void setTollEligible(int tollEligible) {
   		this.tollEligible = tollEligible;
   	}
   	/**
   	 * @return the market segment
   	 */
   	public String getMarketSegment() {
   		return marketSegment;
   	}
   	
   	/**
   	 * @param marketSegment the marketSegment to set
   	 */
   	public void setMarketSegment(String marketSegment){
   		this.marketSegment = marketSegment;
   	}
    	
   	/**
   	 * @return the broad time period
   	 */
   	public int getBroadPeriod() {
   		return broadPeriod;
   	}
   	/**
   	 * @param broadPeriod the broadPeriod to set
   	 */
   	public void setBroadPeriod(int Period) {
   		this.broadPeriod = Period;
   	}

   	/**
   	 * @return the detailed time period
   	 */
   	public int getDetailedPeriod() {
   		return detailedPeriod;
   	}
   	/**
   	 * @param detailedPeriod the detailedPeriod to set
   	 */
   	public void setDetailedPeriod(int Period) {
   		this.detailedPeriod = Period;
   	}

   	/**
   	 * @return the dta time period
   	 */
   	public int getDTAPeriod() {
   		return dtaPeriod;
   	}
   	/**
   	 * @param dtaPeriod the dtaPeriod to set
   	 */
   	public void setDTAPeriod(int Period) {
   		this.dtaPeriod = Period;
   	}
   	/**
   	 * @return the trip expansion factor
   	 */
   	public double getExpansionFactor() {
   		return expansionFactor;
   	}
   	/**
   	 * @param dtaPeriod the dtaPeriod to set
   	 */
   	public void setExpansionFactor(double expansionFactor) {
   		this.expansionFactor = expansionFactor;
   	}

    /**
     * Write the trip
     * 
     * @param writer
     */
    public void writeTrip(PrintWriter writer){
     	String record = new String(
       	   hhid + "," +
       	   persid + "," +
       	   tourid + "," +
       	   id + "," +
       	   originTaz + "," +
       	   destinationTaz + "," +
       	   originMGRA + "," +
       	   destinationMGRA + "," +
       	   originNode + "," +
       	   destinationNode + "," +
       	   vehicleType + "," +
       	   vehOccupancy + "," +
       	   tollEligible + "," +
       	   marketSegment + "," +
       	   detailedPeriod + "," +
       	   broadPeriod + "," +
       	   dtaPeriod + "," +
       	   driver + "," +
       	   expansionFactor
       	   );
       	writer.print(record);
    }
        
    /**
     * Write a header record
     * 
     * @param writer
     */
    /**
     * Write a header record
     * 
     * @param writer
     */
    public void writeHeader(PrintWriter writer){
    	String header = "hh_id,person_id,tour_id,trip_id,originTaz,destinationTaz,originMGRA,destinationMGRA,originNode,destinationNode,vehicleType,vehicleOccupancy,tollEligibility,marketSegment,detailedPeriod,broadPeriod,dtaPeriod,driver,expansionFactor";
    	writer.print(header);
    }
        
}