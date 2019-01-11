package org.sandag.abm.maas;

import java.util.ArrayList;

public class Vehicle {
	
	
	ArrayList<PersonTrip> personTripList;
	
	public Vehicle(){
		
		personTripList = new ArrayList<PersonTrip>();
		
	}
	
	public void addPersonTrip(PersonTrip personTrip){
		
		personTripList.add(personTrip);
		
	}

	private class Stop{
		
		int maz;
		int taz;
		int arrivalTime;
	
	}
	
}
