package org.sandag.cvm.activityTravel;

/**
 * @author jabraham
 *
 * A representation of a stop within a tour -- a stop has a location, a purpose and a duration
 */
public class Stop {
	
	/**
	 * 
	 */
	private final Tour myTour;
	public Stop(Tour tour, int previousLocation, double departureTimeFromPrevious) {
		myTour = tour;
		this.previousLocation = previousLocation;
		this.departureTimeFromPreviousStop = departureTimeFromPrevious;
	}
	
	@Override
	public String toString() {
		return "left at "+departureTimeFromPreviousStop+" for "+location+" from "+previousLocation+", arrived at "+(departureTimeFromPreviousStop+travelTimeMinutes/60)+", stayed for "+duration;
	}
	
    
    /** <code>duration</code> in hours
     */
    public float duration;
    /**
     * <code>location</code> represents the location of the stop
     */
    public int location;
    /**
     * <code>purpose</code> represents the purpose of the stop
     */
    public int purpose;
    public double travelTimeMinutes;
	public String tripMode = "NA";
	public final int previousLocation;
	public final double departureTimeFromPreviousStop;
	
}