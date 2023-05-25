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

import org.sandag.cvm.common.model.DiscreteChoiceModelInterface;


/**
 * @author jabraham
 *
 * A model of tNCVehicle type together with tour type
 */
public interface VehicleTourTypeChoice extends DiscreteChoiceModelInterface {
    /**
     * @param myTour the tour to consider when making the tour type choice
     */
    public void setMyTour(Tour myTour);

    /**
     * 
     */
    public void writeTourAndTripSummary();

    /**
     * @return the tour associated with the tour type choice
     */
    Tour getMyTour();
    
    
}