package org.sandag.abm.maas;

import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.log4j.Logger;


/**
 * A vehicle trip has multiple potential pickups and dropoffs?
 * 
 * @author joel.freedman
 *
 */
public class VehicleTrip {

	protected static final Logger logger = Logger.getLogger(VehicleTrip.class);

	protected int id;
	protected Vehicle vehicle;
	protected short originTaz;
	protected short destinationTaz;
	protected int originMaz;
	protected int destinationMaz;
	protected int passengers;
	protected int startPeriod;
	protected int endPeriod;
	protected ArrayList<Integer> pickupIdsAtOrigin;
	protected ArrayList<Integer> dropoffIdsAtOrigin;
	protected ArrayList<Integer> pickupIdsAtDestination;
	protected ArrayList<Integer> dropoffIdsAtDestination;
	

	
	public VehicleTrip(Vehicle vehicle, int id){

		this.id=id;
		this.vehicle = vehicle;
		pickupIdsAtOrigin = new ArrayList<Integer>();
		dropoffIdsAtOrigin = new ArrayList<Integer>();
		pickupIdsAtDestination = new ArrayList<Integer>();
		dropoffIdsAtDestination = new ArrayList<Integer>();
		
	}

	public ArrayList<Integer> getPickupIdsAtOrigin() {
		return pickupIdsAtOrigin;
	}

	public void setPickupIdsAtOrigin(ArrayList<Integer> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin = pickupIdsAtOrigin;
	}

	public void addPickupIdsAtOrigin(ArrayList<Integer> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin.addAll(pickupIdsAtOrigin);
	}
	public ArrayList<Integer> getDropoffIdsAtOrigin() {
		return dropoffIdsAtOrigin;
	}

	public void setDropoffIdsAtOrigin(ArrayList<Integer> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin = dropoffIdsAtOrigin;
	}

	public void addDropoffIdsAtOrigin(ArrayList<Integer> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin.addAll(dropoffIdsAtOrigin);
	}
	public ArrayList<Integer> getPickupIdsAtDestination() {
		return pickupIdsAtDestination;
	}

	public void setPickupIdsAtDestination(ArrayList<Integer> pickupIdsAtDestination) {
		this.pickupIdsAtDestination = pickupIdsAtDestination;
	}

	public ArrayList<Integer> getDropoffIdsAtDestination() {
		return dropoffIdsAtDestination;
	}

	public void setDropoffIdsAtDestination(
			ArrayList<Integer> dropoffIdsAtDestination) {
		this.dropoffIdsAtDestination = dropoffIdsAtDestination;
	}


	
	public void addPickupAtOrigin(int id){
		pickupIdsAtOrigin.add(id);
	}

	public void addPickupAtDestination(int id){
		pickupIdsAtDestination.add(id);
	}

	public void addDropoffAtOrigin(int id){
		dropoffIdsAtOrigin.add(id);
	}

	public void addDropoffAtDestination(int id){
		dropoffIdsAtDestination.add(id);
	}

	public int getNumberOfPickupsAtOrigin(){
		return pickupIdsAtOrigin.size();
	}
	
	public int getNumberOfDropoffsAtOrigin(){
		return dropoffIdsAtOrigin.size();
	}
	
	public int getNumberOfPickupsAtDestination(){
		return pickupIdsAtDestination.size();
	}
	
	public int getNumberOfDropoffsAtDestination(){
		return dropoffIdsAtDestination.size();
	}

	public Vehicle getVehicle() {
		return vehicle;
	}


	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}


	public short getOriginTaz() {
		return originTaz;
	}


	public void setOriginTaz(short originTaz) {
		this.originTaz = originTaz;
	}


	public short getDestinationTaz() {
		return destinationTaz;
	}


	public void setDestinationTaz(short destinationTaz) {
		this.destinationTaz = destinationTaz;
	}


	public int getOriginMaz() {
		return originMaz;
	}


	public void setOriginMaz(int originMaz) {
		this.originMaz = originMaz;
	}


	public int getDestinationMaz() {
		return destinationMaz;
	}


	public void setDestinationMaz(int destinationMaz) {
		this.destinationMaz = destinationMaz;
	}


	public int getPassengers() {
		return passengers;
	}


	public void setPassengers(int passengers) {
		this.passengers = passengers;
	}

	public int getStartPeriod() {
		return startPeriod;
	}

	public void setStartPeriod(int startPeriod) {
		this.startPeriod = startPeriod;
	}

	public int getEndPeriod() {
		return endPeriod;
	}

	public void setEndPeriod(int endPeriod) {
		this.endPeriod = endPeriod;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public static void printHeader(PrintWriter writer){
	     String record = new String("trip_ID,vehicle_ID,originTaz,destinationTaz,originMaz,destinationMaz,totalPassengers,startPeriod,endPeriod,pickupIdsAtOrigin,dropoffIdsAtOrigin,pickupIdsAtDestination,dropoffIdsAtDestination");
	     writer.println(record);
	     writer.flush();
	}
	
	public void printData(PrintWriter writer){
		
		String pickupIdsAtOriginString = "";
		String dropoffIdsAtOriginString = "";
		String pickupIdsAtDestinationString = "";
		String dropoffIdsAtDestinationString = "";
		
		if(pickupIdsAtOrigin.size()>0)
			for(int pid : pickupIdsAtOrigin)
				pickupIdsAtOriginString += (pid + " ");
		
		if(dropoffIdsAtOrigin.size()>0)
			for(int pid : dropoffIdsAtOrigin)
				dropoffIdsAtOriginString += (pid + " ");

		if(pickupIdsAtDestination.size()>0)
			for(int pid : pickupIdsAtDestination)
				pickupIdsAtDestinationString += (pid + " ");

		if(dropoffIdsAtDestination.size()>0)
			for(int pid : dropoffIdsAtDestination)
				dropoffIdsAtDestinationString += (pid + " ");

		String record = new String(
				id + "," +
				vehicle.getId() +"," +
			    originTaz + "," +
			    destinationTaz + "," +
			    originMaz + "," +
			    destinationMaz + "," +
			    passengers + "," +
			    startPeriod + "," +
			    endPeriod + "," +
			    pickupIdsAtOriginString + "," +
			    dropoffIdsAtOriginString + "," +
			    pickupIdsAtDestinationString + "," +
			    dropoffIdsAtDestinationString);
		
		writer.println(record);
		writer.flush();
	}
	
	public void writeTrace(){
		
		String pickupIdsAtOriginString = "";
		String dropoffIdsAtOriginString = "";
		String pickupIdsAtDestinationString = "";
		String dropoffIdsAtDestinationString = "";
		
		if(pickupIdsAtOrigin.size()>0)
			for(int pid : pickupIdsAtOrigin)
				pickupIdsAtOriginString += (pid + " ");
		
		if(dropoffIdsAtOrigin.size()>0)
			for(int pid : dropoffIdsAtOrigin)
				dropoffIdsAtOriginString += (pid + " ");

		if(pickupIdsAtDestination.size()>0)
			for(int pid : pickupIdsAtDestination)
				pickupIdsAtDestinationString += (pid + " ");

		if(dropoffIdsAtDestination.size()>0)
			for(int pid : dropoffIdsAtDestination)
				dropoffIdsAtDestinationString += (pid + " ");

		logger.info("*********************************************************");
		logger.info("Trace for vehicle trip "+id+" in vehicle "+vehicle.getId());
		logger.info("Trip ID:                 " + id);
		logger.info("Vehicle ID:              "+vehicle.getId());
		logger.info("Origin TAZ:              "+originTaz);
		logger.info("Destination TAZ:         "+destinationTaz);
		logger.info("Origin MAZ:              "+originMaz);
		logger.info("Destination MAZ:         "+destinationMaz);
		logger.info("Passengers:              "+passengers);
		logger.info("Start period:            "+startPeriod);
		logger.info("End period:              "+endPeriod);
		logger.info("Pickups at Origin:       "+ pickupIdsAtOriginString);
		logger.info("Dropoffs at Origin:      "+ dropoffIdsAtOriginString);
		logger.info("Pickups at Destination:  "+ pickupIdsAtDestinationString);
		logger.info("Dropoffs at Destination: "+ dropoffIdsAtDestinationString);
		logger.info("*********************************************************");
		
		
	}


}
