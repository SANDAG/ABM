package org.sandag.abm.maas;

import java.util.ArrayList;

public class Vehicle {
	
	
	private ArrayList<PersonTrip> personTripList;
	
	private ArrayList<VehicleTrip> vehicleTrips;
	private byte maxPassengers;
	private short generationTaz;
	private short generationPeriod;
	private int id;
	
	public Vehicle(int id, byte maxPassengers){
		this.id= id;
		this.maxPassengers = maxPassengers;
		personTripList = new ArrayList<PersonTrip>();
		vehicleTrips = new ArrayList<VehicleTrip>();
	}
	
	public void addVehicleTrip(VehicleTrip vehicleTrip){
		vehicleTrips.add(vehicleTrip);
	}
	
	public void addVehicleTrips(ArrayList<VehicleTrip> vehicleTrips){
		this.vehicleTrips.addAll(vehicleTrips);
	}
	
	public void clearPersonTrips(){
		this.personTripList.clear();
	}

	public ArrayList<VehicleTrip> getVehicleTrips(){
		
		return vehicleTrips;
	}
	
	public int getId(){
		return this.id;
	}
	
	/**
	 * Add a passenger to the vehicle.
	 * 
	 * @param personTrip
	 */
	public void addPersonTrip(PersonTrip personTrip){
		
		personTripList.add(personTrip);
		
	}
	
	public void removePersonTrip(PersonTrip personTrip){
		
		personTripList.remove(personTrip);
	}
	
	/**
	 * Get number of passengers.
	 * 
	 * @return The number of passengers
	 */
	public byte getNumberPassengers(){
		
		return (byte) personTripList.size();
	}

	public byte getMaxPassengers() {
		return maxPassengers;
	}

	public void setMaxPassengers(byte maxPassengers) {
		this.maxPassengers = maxPassengers;
	}

	public short getGenerationTaz() {
		return generationTaz;
	}

	public void setGenerationTaz(short generationTaz) {
		this.generationTaz = generationTaz;
	}

	public short getGenerationPeriod() {
		return generationPeriod;
	}

	public void setGenerationPeriod(short generationPeriod) {
		this.generationPeriod = generationPeriod;
	}

	public ArrayList<PersonTrip> getPersonTripList() {
		return personTripList;
	}



		
}
