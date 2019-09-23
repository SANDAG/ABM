package org.sandag.abm.maas;

import java.util.ArrayList;

public class Vehicle {
	
	
	protected ArrayList<PersonTrip> personTripList;
	
	protected ArrayList<VehicleTrip> vehicleTrips;
	protected byte maxPassengers;
	protected short generationTaz;
	protected short generationPeriod;
	protected int id;
	protected float maxDistanceBeforeRefuel;
	protected float distanceSinceRefuel;
	protected int periodsRefueling;

	/**
	 * Create a new vehicle.
	 * 
	 * @param id
	 * @param maxPassengers
	 * @param maxDistanceBeforeRefuel
	 */
	public Vehicle(int id, byte maxPassengers, float maxDistanceBeforeRefuel){
		this.id= id;
		this.maxPassengers = maxPassengers;
		personTripList = new ArrayList<PersonTrip>();
		vehicleTrips = new ArrayList<VehicleTrip>();
		this.maxDistanceBeforeRefuel = maxDistanceBeforeRefuel;
	}
	
	/**
	 * Add a vehicle trip to this vehicle.
	 * 
	 * @param vehicleTrip
	 */
	public void addVehicleTrip(VehicleTrip vehicleTrip){
		vehicleTrips.add(vehicleTrip);
	}
	
	/**
	 * Add an ArrayList of vehicle trips to this vehicle.
	 * 
	 * @param vehicleTrips
	 */
	public void addVehicleTrips(ArrayList<VehicleTrip> vehicleTrips){
		this.vehicleTrips.addAll(vehicleTrips);
	}
	
	/**
	 * Clear all the person trips from this vehicle. Used after routing the vehicle.
	 * 
	 */
	public void clearPersonTrips(){
		this.personTripList.clear();
	}

	/**
	 * Get all the vehicle trips for this vehicle.
	 * 
	 * @return VehicleTrips
	 */
	public ArrayList<VehicleTrip> getVehicleTrips(){
		
		return vehicleTrips;
	}
	
	/**
	 * Get the vehicle ID.
	 * 
	 * @return
	 */
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
	
	/**
	 * Remove one person trip from the vehicle. Used after routing.
	 * 
	 * @param personTrip
	 */
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

	public float getDistanceSinceRefuel() {
		return distanceSinceRefuel;
	}

	public void setDistanceSinceRefuel(float distanceSinceRefuel) {
		this.distanceSinceRefuel = distanceSinceRefuel;
	}

	public int getPeriodsRefueling() {
		return periodsRefueling;
	}

	public void setPeriodsRefueling(int periodsRefueling) {
		this.periodsRefueling = periodsRefueling;
	}


		
}
