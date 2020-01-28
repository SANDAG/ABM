package org.sandag.abm.maas;

import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.log4j.Logger;


/**
 * A tNCVehicle trip has multiple potential pickups and dropoffs?
 * 
 * @author joel.freedman
 *
 */
public class TNCVehicleTrip {

	protected static final Logger logger = Logger.getLogger(TNCVehicleTrip.class);

	protected int id;
	protected TNCVehicle tNCVehicle;
	protected short originTaz;
	protected short destinationTaz;
	protected int originMaz;
	protected int destinationMaz;
	protected int passengers;
	protected int startPeriod;
	protected int endPeriod;
	protected ArrayList<String> pickupIdsAtOrigin;
	protected ArrayList<String> dropoffIdsAtOrigin;
	protected ArrayList<String> pickupIdsAtDestination;
	protected ArrayList<String> dropoffIdsAtDestination;
	protected Purpose originPurpose;
	protected Purpose destinationPurpose;
	protected float distance;
	
	protected enum Purpose { HOME, PICKUP_ONLY, DROPOFF_ONLY, PICKUP_AND_DROPOFF, REFUEL } 

	
	public TNCVehicleTrip(TNCVehicle tNCVehicle, int id){

		this.id=id;
		this.tNCVehicle = tNCVehicle;
		pickupIdsAtOrigin = new ArrayList<String>();
		dropoffIdsAtOrigin = new ArrayList<String>();
		pickupIdsAtDestination = new ArrayList<String>();
		dropoffIdsAtDestination = new ArrayList<String>();
		originPurpose=Purpose.HOME;
		destinationPurpose=Purpose.HOME;
		
	}

	public ArrayList<String> getPickupIdsAtOrigin() {
		return pickupIdsAtOrigin;
	}

	public void setPickupIdsAtOrigin(ArrayList<String> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin = pickupIdsAtOrigin;
		
		if(pickupIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;
	}

	public void addPickupIdsAtOrigin(ArrayList<String> pickupIdsAtOrigin) {
		this.pickupIdsAtOrigin.addAll(pickupIdsAtOrigin);
		
		if(pickupIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;

	}
	public void addPickupIdsAtDestination(ArrayList<String> pickupIdsAtDestination) {
		this.pickupIdsAtDestination.addAll(pickupIdsAtDestination);
		
		if(pickupIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.DROPOFF_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.PICKUP_ONLY;

	}
	public ArrayList<String> getDropoffIdsAtOrigin() {
		return dropoffIdsAtOrigin;
	}

	public void setDropoffIdsAtOrigin(ArrayList<String> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin = dropoffIdsAtOrigin;
		
		if(dropoffIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	}

	public void addDropoffIdsAtOrigin(ArrayList<String> dropoffIdsAtOrigin) {
		this.dropoffIdsAtOrigin.addAll(dropoffIdsAtOrigin);
		
		if(dropoffIdsAtOrigin.isEmpty())
			return;
		
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	}
	public void addDropoffIdsAtDestination(ArrayList<String> dropoffIdsAtDestination) {
		this.dropoffIdsAtDestination.addAll(dropoffIdsAtDestination);
		
		if(dropoffIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.PICKUP_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.DROPOFF_ONLY;

	}
	public ArrayList<String> getPickupIdsAtDestination() {
		return pickupIdsAtDestination;
	}

	public void setPickupIdsAtDestination(ArrayList<String> pickupIdsAtDestination) {
		this.pickupIdsAtDestination = pickupIdsAtDestination;
		
		if(pickupIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.DROPOFF_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.PICKUP_ONLY;

	}

	public ArrayList<String> getDropoffIdsAtDestination() {
		return dropoffIdsAtDestination;
	}

	public void setDropoffIdsAtDestination(
			ArrayList<String> dropoffIdsAtDestination) {
		this.dropoffIdsAtDestination = dropoffIdsAtDestination;
		
		if(dropoffIdsAtDestination.isEmpty())
			return;
		
		if(destinationPurpose==Purpose.PICKUP_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.DROPOFF_ONLY;

	}


	
	public void addPickupAtOrigin(String id){
		pickupIdsAtOrigin.add(id);
		
		if(originPurpose==Purpose.DROPOFF_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.PICKUP_ONLY;
	}

	public void addPickupAtDestination(String id){
		pickupIdsAtDestination.add(id);

		if(destinationPurpose==Purpose.DROPOFF_ONLY)
			destinationPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			destinationPurpose = Purpose.PICKUP_ONLY;
	}

	public void addDropoffAtOrigin(String id){
		dropoffIdsAtOrigin.add(id);
	
		if(originPurpose==Purpose.PICKUP_ONLY)
			originPurpose = Purpose.PICKUP_AND_DROPOFF;
		else
			originPurpose = Purpose.DROPOFF_ONLY;

	
	}

	public void addDropoffAtDestination(String id){
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

	public TNCVehicle getVehicle() {
		return tNCVehicle;
	}


	public void setVehicle(TNCVehicle tNCVehicle) {
		this.tNCVehicle = tNCVehicle;
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
	     String record = new String("trip_ID,vehicle_ID,originTaz,destinationTaz,originMgra,destinationMgra,totalPassengers,startPeriod,endPeriod,pickupIdsAtOrigin,dropoffIdsAtOrigin,pickupIdsAtDestination,dropoffIdsAtDestination, originPurpose, destinationPurpose");
	     writer.println(record);
	     writer.flush();
	}
	
	public void printData(PrintWriter writer){
		
		String pickupIdsAtOriginString = "";
		String dropoffIdsAtOriginString = "";
		String pickupIdsAtDestinationString = "";
		String dropoffIdsAtDestinationString = "";
		
		if(pickupIdsAtOrigin.size()>0)
			for(String pid : pickupIdsAtOrigin)
				pickupIdsAtOriginString += (pid + " ");
		
		if(dropoffIdsAtOrigin.size()>0)
			for(String pid : dropoffIdsAtOrigin)
				dropoffIdsAtOriginString += (pid + " ");

		if(pickupIdsAtDestination.size()>0)
			for(String pid : pickupIdsAtDestination)
				pickupIdsAtDestinationString += (pid + " ");

		if(dropoffIdsAtDestination.size()>0)
			for(String pid : dropoffIdsAtDestination)
				dropoffIdsAtDestinationString += (pid + " ");

		String record = new String(
				id + "," +
				tNCVehicle.getId() +"," +
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
			for(String pid : pickupIdsAtOrigin)
				pickupIdsAtOriginString += (pid + " ");
		
		if(dropoffIdsAtOrigin.size()>0)
			for(String pid : dropoffIdsAtOrigin)
				dropoffIdsAtOriginString += (pid + " ");

		if(pickupIdsAtDestination.size()>0)
			for(String pid : pickupIdsAtDestination)
				pickupIdsAtDestinationString += (pid + " ");

		if(dropoffIdsAtDestination.size()>0)
			for(String pid : dropoffIdsAtDestination)
				dropoffIdsAtDestinationString += (pid + " ");

		logger.info("*********************************************************");
		logger.info("Trace for tNCVehicle trip "+id+" in tNCVehicle "+tNCVehicle.getId());
		logger.info("Trip ID:                 " + id);
		logger.info("TNCVehicle ID:              "+tNCVehicle.getId());
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
