package org.sandag.cvm.activityTravel;

import org.sandag.cvm.common.emme2.MatrixCacheReader;

public abstract class TripMode {

	/**
	 * First part of string is tNCVehicle type aka tour mode.
	 * Followed by a colon and then the trip mode.
	 * e.g L:T means tour "Light" and trip-mode "toll"
	 */
	protected final String myType;
	protected final TripModeChoice myChoiceModel;
	public final char vehicleType;
	public final String tripMode;
	protected int origin;
	protected int destination;
	protected double timeOfDay;
	
	public TripMode(
			TripModeChoice choiceModel,
			String type) {
		myType = type;
		myChoiceModel = choiceModel;
		vehicleType = myType.split(":")[0].charAt(0);
		tripMode = myType.split(":")[1];
		
	}


	public abstract double getUtility();


	public String getCode() {
		return myType;
	}

	public abstract void readMatrices(MatrixCacheReader matrixReader);

	public abstract void addCoefficient(String index1, String index2,
			String matrix, double coefficient) throws CoefficientFormatError;

	public int getDestination() {
		return destination;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public void setTime(double time) {
		timeOfDay = time;
	
	}

	public String logOriginDestination() {
		return String.valueOf(origin)+" to "+destination;
	}

	public String getTripMode() {
		return tripMode;
	}



}