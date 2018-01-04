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


package org.sandag.cvm.activityTravel.cvm;

import org.sandag.cvm.activityTravel.ChangingTravelAttributeGetter;
import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.ModelUsesMatrices;
import org.sandag.cvm.activityTravel.TravelTimeTracker;


/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class CommercialTravelTimeTracker extends TravelTimeTracker implements ModelUsesMatrices, ChangingTravelAttributeGetter {

    /**
     * Method addCoefficient.
     * @param alternative
     * @param index1
     * @param index2
     * @param matrix
     * @param coefficient
     */
    public void addCoefficient (
        String alternative,
        String index1,
        String index2,
        String matrix,
        double coefficient)  throws CoefficientFormatError 
    {
        if (alternative.length()!=1) {
            throw new CoefficientFormatError("Alternative must be L, M, I or H for TravelTimeMatrix");
        }
        char vehicleType = alternative.charAt(0);
        if (vehicleType!='L' && vehicleType!='M' && vehicleType!='H' && vehicleType != 'I') {
            throw new CoefficientFormatError("Alternative must be L, M, I or H for TravelTimeMatrix");
        }
        if (index1.equals("default")) {
            travelTimeMatrices.add(new TravelTimeMatrixSpec(matrix, -1, -1, vehicleType));
        } else if (index1.equals("")) {
            travelTimeMatrices.add(new TravelTimeMatrixSpec(matrix, Float.valueOf(index2).floatValue(), (float) coefficient, vehicleType));
        } else throw new CoefficientFormatError("Index1 must be \"default\" or blank for TravelTimeMatrices");
    }


    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */
    public void init() {
        readMatrices(GenerateCommercialTours.matrixReader);
    }

    



}
