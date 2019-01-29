package org.sandag.abm.maas;

import java.util.ArrayList;

public class Vehicle {
	
	
	private ArrayList<PersonTrip> personTripList;
	
	private ArrayList<Integer> pickupMazs;
	private ArrayList<Integer> dropoffMazs;
	private byte maxPassengers;
	
	public Vehicle(byte maxPassengers){
		
		this.maxPassengers = maxPassengers;
		personTripList = new ArrayList<PersonTrip>();
		pickupMazs = new ArrayList<Integer>();
		dropoffMazs = new ArrayList<Integer>();
	}
	
	/**
	 * Add a passenger to the vehicle.
	 * 
	 * @param personTrip
	 */
	public void addPersonTrip(PersonTrip personTrip){
		
		personTripList.add(personTrip);
		
		pickupMazs.add(personTrip.getOriginMaz());
		dropoffMazs.add(personTrip.getDestinationMaz());
		
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

	private class Stop{
		
		int maz;
		int taz;
		int arrivalTime;
	
	}
	
}
