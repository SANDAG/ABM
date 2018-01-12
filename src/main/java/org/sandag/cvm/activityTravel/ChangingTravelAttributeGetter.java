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
 * An interface that determines the travel time between two points, given a time (represented by a double) and
 * a vehicle type (represented by a char). 
 */
package org.sandag.cvm.activityTravel;

/**
 * @author jabraham
 *
 * An interface that determines a travel attribute between two points, given a time (represented by a double) and
 * a vehicle type (represented by a char).  For instance if there are matrices of travel time for peak and off-peak,
 * and implementation of this interface would be able to determine whether time was in the peak or off-peak, and
 * then return the travel conditions appropriately.
 */ 
public interface ChangingTravelAttributeGetter {
    /**
     * 
     * This method returns the travel attribute associated with travelling from origin to destination at time time by 
     * vehicle type vehicleType
     * 
     * @param origin
     * @param destination
     * @param time
     * @param vehicleType
     * @return
     */
    public abstract double getTravelAttribute(
        int origin,
        int destination,
        double time,
        char vehicleType);
}