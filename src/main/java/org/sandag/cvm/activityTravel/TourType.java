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

/**
 * @author jabraham
 *
 * This class is a representation of a type of tour. Contained it in
 * are counters for the number of instances of the type of tour and trips 
 * as well as a choice model for choosing a tNCVehicle type for the tour.
 * The class is identified by a name which is a String.
 */
public abstract class TourType {

    /**
     * @param tourType a string representing the type of tour, which may also have information on tNCVehicle types
     * @param vehicleType a char representing the type of tNCVehicle
     * @param theChoiceModel the choice model associated with the tour type
     */
    public TourType(String tourType, char vehicleType, VehicleTourTypeChoice theChoiceModel){
        myChoice = theChoiceModel;
        this.tourTypeName = tourType;
        this.vehicleType= vehicleType;
    }

    /**
     * <code>tourCount</code> is a counter for the number of tours of this type.
     */
    protected int tourCount = 0;
    protected int tripCount = 0;

    /**
     * This class can serve as a place to keep track of the number of tours and trips of different types.
     * This method increments the number of tours and trips.
     * @param tours the number of tours to increment the tour counter by
     * @param trips the number of trips to increment the trip counter by
     */
    public void incrementTourAndTripCount(int tours, int trips) {
    	tourCount += tours;
    	tripCount += trips;
    }

    /**
     * <code>tourTypeName</code> is the unique identifier of the type of tour 
     */
    public final String tourTypeName;
    /**
     * <code>vehicleType</code> is the identifier for the tNCVehicle type used in the tour
     */
    public final char vehicleType;

    /**
     * @return the tourTypeName
     */
    public String getCode() {
        return getTourTypeName();
    }

    /**
     * @return the tourTypeName
     */
    public String getTourTypeName() {
        return tourTypeName;
    }

    /**
     * @return the char representing the tNCVehicle type
     */
    public char getVehicleType() {
        return vehicleType;
    }

    /**
     * @return the utility of this alternative
     */
    public abstract double getUtility();

    /**
     * @return Returns the tourCount.
     */
    public int getTourCount() {
        return tourCount;
    }

    /**
     * @return Returns the tripCount.
     */
    public int getTripCount() {
        return tripCount;
    }

    protected final VehicleTourTypeChoice myChoice;

}
