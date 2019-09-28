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
	protected Purpose originPurpose;
	protected Purpose destinationPurpose;
	protected float distance;
	
	protected enum Purpose { HOME, PICKUP_ONLY, DROPOFF_ONLY, PICKUP_AND_DROPOFF, REFUEL } 

	
	public VehicleTrip(Vehicle vehicle, int id){

		this.id=id;
		this.vehicle = vehicle;
		pickupIdsAtOrigin = new ArrayList<Integer>();
		dropoffIdsAtOrigin = new ArrayList<Integer>();
		pickupIdsAtDestination = new ArrayList<Integer>();
		dropoffIdsAtDestination = new ArrayList<Integer>();
		originPurpose=Purpose.HOME;
		destinationPurpose=Purpose.HOME;
		
	}

	public ArrayList<Integer> getPickupIdsAtOrigin() {
		return pickupIdsAtOrigin;
	}

	public void setPickupIdsAtOrigin(ArrayList<Integer> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin = pickupIdsAtOrigin;
		
		if(pickupIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;
	}

	public void addPickupIdsAtOrigin(ArrayList<Integer> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin.addAll(pickupIdsAtOrigin);
		
		if(pickupIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;

	}
	public ArrayList<Integer> getDropoffIdsAtOrigin() {
		return dropoffIdsAtOrigin;
	}

	public void setDropoffIdsAtOrigin(ArrayList<Integer> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin = dropoffIdsAtOrigin;
		
		if(dropoffIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	}

	public void addDropoffIdsAtOrigin(ArrayList<Integer> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin.addAll(dropoffIdsAtOrigin);
		
		if(dropoffIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	}
	public ArrayList<Integer> getPickupIdsAtDestination() {
		return pickupIdsAtDestination;
	}

	public void setPickupIdsAtDestination(ArrayList<Integer> pickupIdsAtDestination) {
		this.pickupIdsAtDestination = pickupIdsAtDestination;
		
		if(pickupIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.DROPOFF_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.PICKUP_ONLY;

	}

	public ArrayList<Integer> getDropoffIdsAtDestination() {
		return dropoffIdsAtDestination;
	}

	public void setDropoffIdsAtDestination(
			ArrayList<Integer> dropoffIdsAtDestination) {
		this.dropoffIdsAtDestination = dropoffIdsAtDestination;
		
		if(dropoffIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.PICKUP_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.DROPOFF_ONLY;

	}


	
	public void addPickupAtOrigin(int id){
		pickupIdsAtOrigin.add(id);
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;
	}

	public void addPickupAtDestination(int id){
		pickupIdsAtDestination.add(id);

		if(destinationPurpose==Purpose.DROPOFF_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.PICKUP_ONLY;
	}

	public void addDropoffAtOrigin(int id){
		dropoffIdsAtOrigin.add(id);
	
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	
	}

	public void addDropoffAtDestination(int id){
		dropoffIdsAtDestination.add(id);

		if(destinationPurpose==Purpose.PICKUP_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.DROPOFF_ONLY;
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
	     String record = new String("trip_ID,vehicle_ID,originTaz,destinationTaz,originMaz,destinationMaz,totalPassengers,startPeriod,endPeriod,pickupIdsAtOrigin,dropoffIdsAtOrigin,pickupIdsAtDestination,dropoffIdsAtDestination, originPurpose, destinationPurpose");
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
			    dropoffIdsAtDestinationString + "," +
			    originPurpose.ordinal() + "," +
			    destinationPurpose.ordinal());
		
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
		logger.info("Origin Purpose:          "+ originPurpose);
		logger.info("Destination Purpose:     "+ destinationPurpose);
		
		logger.info("*********************************************************");
		
		
	}

	public Purpose getOriginPurpose() {
		return originPurpose;
	}

	public void setOriginPurpose(Purpose originPurpose) {
		this.originPurpose = originPurpose;
	}

	public Purpose getDestinationPurpose() {
		return destinationPurpose;
	}

	public void setDestinationPurpose(Purpose destinationPurpose) {
		this.destinationPurpose = destinationPurpose;
	}


	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}


}
