package org.sandag.cvm.activityTravel;

public interface TourInterface {

    /**
     * Method getCurrentTime
     * @deprecated
     * @return double current time of day in hours
     */
    public double getCurrentTimeHrs();

    /**
     * Method getCurrentTime
     * @return double current time of day in minutes
     */
    public double getCurrentTimeMinutes();

    /**
     * Method getOrigin.
     * @return an int representing the origin location of the tour.
     */
    public int getOrigin();

    /**
     * @return the integer representing the origin of the tour
     */
    public int getOriginZone();

    /**
     * Method getStopCount.
     * @return int[] an integer array counting the stops that occur by type.  Element 0
     * is the total number of stops; other elements correspond to different stop purposes
     */
    public int[] getStopCounts();

    /**
     * Method getTotalElapsedTime.
     * @deprecated
     * @return double total elapsed time in hours
     */
    public double getTotalElapsedTimeHrs();

    /**
     * Method getTotalElapsedTime.
     * @return double total elapsed time in hours
     */
    public double getTotalElapsedTimeMinutes();

    /**
     * Method getTotalTravelTime.
     * @return double total travel time in minutes
     */
    public double getTotalTravelTimeMinutes();

    /**
     * Method getCurrentLocation.
     * @return int
     */
    public int getCurrentLocation();

}