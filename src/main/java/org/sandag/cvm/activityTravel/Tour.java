/*
 Travel Model Microsimulation library
 Copyright (C) 2005 John Abraham jabraham@ucalgary.ca and others


  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/

/*
 * Created on Feb 4, 2005
 *
 */
package org.sandag.cvm.activityTravel;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

//import org.sandag.cvm.calgary.commercial.TourStartTimeModel;
//import org.sandag.cvm.calgary.commercial.WeekendTravelTimeTracker;
import org.sandag.cvm.activityTravel.cvm.TourStartTimeModel;
import org.sandag.cvm.common.model.DiscreteChoiceModelInterface;
import org.sandag.cvm.common.model.NoAlternativeAvailable;


/**
 * @author jabraham
 *
 * This is a representation of a tour.
 */
public abstract class Tour implements TourInterface {
	
	public abstract ChangingTravelAttributeGetter getElapsedTravelTimeCalculator();
	public abstract TourStartTimeModel getTourStartTimeModel();

//    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer("Tour from "+originZone+" via ");
        for (int i=0;i<stops.size();i++) {
            Object p = stops.get(i);
            Stop stop = (Stop) p;
            buff.append(stop.location);
            buff.append(",");
        }
        return new String(buff);
    }

    public abstract double getCurrentTimeMinutes();
    

    public abstract double getTotalElapsedTimeMinutes(); 
    

    private double currentTimeHrs;
    protected DiscreteChoiceModelInterface myNextStopPurposeChoice;

    protected TourType myVehicleTourType;


    private int originZone;
    /**
     * <code>stops</code> is an ArrayList containing instances of Tour.Stop, one for each stop made on the tour
     */
    protected ArrayList stops = new ArrayList();

    private double tourStartTimeHrs = 0;
    private double travelTimeMinutes;

	static Logger logger = Logger.getLogger(Tour.class);

    /**
     * Adds a stop to the <code>stops</code> and alsu updates <code>travelTimeMinutes</code> and <code>currentTimeHrs</code>
     * @param newStop
     */
    protected void addStop(Stop newStop) {
    	int lastStopLocation = getOrigin();
    	if (stops.size()!=0) lastStopLocation = ((Stop) stops.get(stops.size()-1)).location;
    	stops.add(newStop);
        // TODO need to use trip mode not tour mode for travel time tracking.
    	// TODO should account for toll/non toll for travel time tracking (e.g. if toll is a trip mode.)
    	double legTravelTime = getElapsedTravelTimeCalculator().getTravelAttribute(lastStopLocation,newStop.location,currentTimeHrs,myVehicleTourType.getVehicleType());
    	if (legTravelTime == 0 ) {
    		// a problem
    		logger.warn("Leg travel time is zero for "+lastStopLocation + " to " + newStop.location);
    	}
    	newStop.travelTimeMinutes=legTravelTime;
    	travelTimeMinutes+=legTravelTime;
    	currentTimeHrs += (legTravelTime/60 + newStop.duration);
    }

    protected double calcCurrentTime() {
        double travelTime =0;
        double currentTimeCalc = getTourStartTimeHrs();
        Iterator stopIt = stops.iterator();
        int lastStop = getOriginZone();
        while (stopIt.hasNext()) {
            Stop stop = (Stop) stopIt.next();
            // TODO need to use trip mode not tour mode for travel time tracking.
            double legTravelTime = getElapsedTravelTimeCalculator().getTravelAttribute(lastStop,stop.location,currentTimeCalc,myVehicleTourType.getVehicleType());
            travelTime += legTravelTime;
            currentTimeCalc += legTravelTime/60;
            currentTimeCalc += stop.duration;
            lastStop = stop.location;
        }
        return currentTimeCalc;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getCurrentTimeHrs()
     */
    public double getCurrentTimeHrs() {
    	return currentTimeHrs;
    }

    /**
     * Method getLastStopType.
     * @return int
     */
    public int getLastStopType() {
        if (stops.size()==0) return 0;
        Stop theLastStop = (Stop) stops.get(stops.size()-1);
        return theLastStop.purpose;
    }

    /**
     * @return Returns the myVehicleTourType.
     */
    public TourType getMyVehicleTourType() {
        return myVehicleTourType;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getOrigin()
     */
    public int getOrigin() {
    	return getOriginZone();
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getOriginZone()
     */
    public int getOriginZone() {
        return originZone;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getStopCount()
     */
    public int[] getStopCounts() {
        int[] stopCounter = new int[getMaxTourTypes()+1];
        Iterator stopIt = stops.iterator();
        while (stopIt.hasNext()) {
            Stop stop = (Stop) stopIt.next();
            stopCounter[0]++;
            stopCounter[stop.purpose]++;
        }
        return stopCounter;
    }
    
    public abstract int getMaxTourTypes();

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getTotalElapsedTime()
     */
    public double getTotalElapsedTimeHrs() {
        return getCurrentTimeHrs()-getTourStartTimeHrs();
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getTotalTravelTimeMinutes()
     */
    public double getTotalTravelTimeMinutes() {
    	return travelTimeMinutes;
    }

    /**
     * Method getTourStartTime.
     * @return double
     */
    protected double getTourStartTimeHrs() {
    	return tourStartTimeHrs;
    }

    /**
     * Method getTourTypeCode.
     * @return A string grepresenting the type of tour -- perhaps representing the types
     * of activities that occur in the tour.
     */
    protected String getTourTypeCode() {
    	return myVehicleTourType.getCode().substring(1);
    }

    /**
     * Method getVehicleCode.
     * @return a string representing the vehicle code used for the tour
     */
    protected String getVehicleCode() {
        return myVehicleTourType.getCode().substring(0,1);
    }

    /**
     * @return Returns the vehicleTourTypeChoice.
     */
    public abstract VehicleTourTypeChoice getVehicleTourTypeChoice();

    /**
     * Method sampleStartTime.
     */
    public void sampleStartTime() {
        tourStartTimeHrs = getTourStartTimeModel().sampleValue();
        if (stops.size()==0) currentTimeHrs = tourStartTimeHrs;
        else currentTimeHrs = calcCurrentTime();
    
    }

    /**
     *  This method uses a random number generator to sample the stops along the tour -- their location, 
     * duration and purpose.
     */
    public abstract void sampleStops();

    /**
     * This method uses a random number generator to sample the type of tour, including
     * the type of vehicle(s) used for the tour and perhaps some information about the types
     * of activities that occur along the tour.
     */
    public void sampleVehicleAndTourType() {
        getVehicleTourTypeChoice().setMyTour(this);
        try {
           myVehicleTourType = (TourType) getVehicleTourTypeChoice().monteCarloChoice();
        } catch (NoAlternativeAvailable e) {
            myVehicleTourType = null;
            //Leave it null for an error to occur when it's actually needed
        }
    }

    /**
     * @param currentTimeHrs The currentTimeHrs to set.
     */
    protected void setCurrentTimeHrs(double currentTimeHrs) {
        this.currentTimeHrs = currentTimeHrs;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#setOrigin(int)
     */
    public void setOrigin(int z) {
        setOriginZone(z);
    }

    void setOriginZone(int originZone) {
        this.originZone = originZone;
    }

    /**
     * @param tourStartTimeHrs The time when the tour starts.
     */
    protected void setTourStartTimeHrs(double tourStartTimeHrs) {
        this.tourStartTimeHrs = tourStartTimeHrs;
    }

    /**
     * @param vehicleTourTypeChoice The vehicleTourTypeChoice to set.
     */
    public abstract void setVehicleTourTypeChoice(VehicleTourTypeChoice vehicleTourTypeChoice);

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourInterface#getCurrentLocation()
     */
    public int getCurrentLocation() {
        if (stops.size()==0) return getOriginZone();
        Stop stop = (Stop) stops.get(stops.size()-1);
        return stop.location;
    }

    public abstract ChangingTravelAttributeGetter getTravelDisutilityTracker() ;

}
