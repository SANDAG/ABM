package org.sandag.abm.maas;


import java.util.ArrayList;

public class TNCVehicle {
	
	
	protected ArrayList<PersonTrip> personTripList;
	
	protected ArrayList<TNCVehicleTrip> tNCVehicleTrips;
	protected byte maxPassengers;
	protected short generationTaz;
	protected short generationPeriod;
	protected int id;
	protected float maxDistanceBeforeRefuel;
	protected float distanceSinceRefuel;
	protected int periodsRefueling;

	/**
	 * Create a new tNCVehicle.
	 * 
	 * @param id
	 * @param maxPassengers
	 * @param maxDistanceBeforeRefuel
	 */
	public TNCVehicle(int id, byte maxPassengers, float maxDistanceBeforeRefuel){
		this.id= id;
		this.maxPassengers = maxPassengers;
		personTripList = new ArrayList<PersonTrip>();
		tNCVehicleTrips = new ArrayList<TNCVehicleTrip>();
		this.maxDistanceBeforeRefuel = maxDistanceBeforeRefuel;
	}
	
	/**
	 * Add a tNCVehicle trip to this tNCVehicle.
	 * 
	 * @param tNCVehicleTrip
	 */
	public void addVehicleTrip(TNCVehicleTrip tNCVehicleTrip){
		tNCVehicleTrips.add(tNCVehicleTrip);
	}
	
	/**
	 * Add an ArrayList of tNCVehicle trips to this tNCVehicle.
	 * 
	 * @param tNCVehicleTrips
	 */
	public void addVehicleTrips(ArrayList<TNCVehicleTrip> tNCVehicleTrips){
		this.tNCVehicleTrips.addAll(tNCVehicleTrips);
	}
	
	/**
	 * Clear all the person trips from this tNCVehicle. Used after routing the tNCVehicle.
	 * 
	 */
	public void clearPersonTrips(){
		this.personTripList.clear();
	}

	/**
	 * Get all the tNCVehicle trips for this tNCVehicle.
	 * 
	 * @return VehicleTrips
	 */
	public ArrayList<TNCVehicleTrip> getVehicleTrips(){
		
		return tNCVehicleTrips;
	}
	
	/**
	 * Get the tNCVehicle ID.
	 * 
	 * @return
	 */
	public int getId(){
		return this.id;
	}
	
	/**
	 * Add a passenger to the tNCVehicle.
	 * 
	 * @param personTrip
	 */
	public void addPersonTrip(PersonTrip personTrip){
		
		personTripList.add(personTrip);
		
	}
	
	/**
	 * Remove one person trip from the tNCVehicle. Used after routing.
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
