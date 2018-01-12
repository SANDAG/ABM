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

package org.sandag.cvm.calgary.weekend;

import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.ModelWithCoefficients;
import org.sandag.cvm.activityTravel.TourType;
import org.sandag.cvm.activityTravel.VehicleTourTypeChoice;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.model.Alternative;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WeekendTourType extends TourType implements ModelWithCoefficients, Alternative {

    /**
     * @param vehicleTourType
     * @param vehicleType
     * @param theChoiceModel
     */
    public WeekendTourType(
        String tourType,
        char vehicleType,
        VehicleTourTypeChoice theChoiceModel) {
        super(tourType, vehicleType, theChoiceModel);
    }
    
    IndexLinearFunction myUtilityFunction = new IndexLinearFunction();

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.TourType#getUtility()
     */
    public double getUtility() {
        return myUtilityFunction.calcForIndex(0,0);
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#addCoefficient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, double)
     */
    public void addCoefficient(String alternative, String index1, String index2, String matrixName, double coefficient) throws CoefficientFormatError {
        myUtilityFunction.addCoefficient(matrixName,coefficient);
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#init()
     */
    public void init() {
        WeekendTourTypeChoice wttc = (WeekendTourTypeChoice) myChoice;
        myUtilityFunction.readMatrices(wttc.getMatrixReader());
    }

}
