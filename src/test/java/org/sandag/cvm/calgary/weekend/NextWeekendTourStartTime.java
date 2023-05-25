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

import org.sandag.cvm.activityTravel.*;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class NextWeekendTourStartTime
    implements ModelWithCoefficients, RealNumberDistribution {
    
    double startTime = 0;
    double endTime = 1;
    private WeekendHousehold myHousehold;

    /**
     * 
     */
    public NextWeekendTourStartTime() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#addCoefficient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, double)
     */
    public void addCoefficient(
        String alternative,
        String index1,
        String index2,
        String matrixName,
        double coefficient)
        throws CoefficientFormatError {
        if (alternative.equalsIgnoreCase("EndTime")) {
            endTime = coefficient;
        } else if (alternative.equalsIgnoreCase("StartTime")) {
            startTime = coefficient;
        } else {
            throw new CoefficientFormatError("Valid coefficients for tour start time model are \"StartTime\" and \"EndTime\"");
        }

    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#init()
     */
    public void init() {
        // Nothing to do here.

    }
    
    public void shiftTime(double shift) {
        startTime+=shift;
        endTime +=shift;
    }
    
    public void resetStart() {
        endTime = endTime-startTime;
        startTime = 0;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.common.model.SingleValueRealDistribution#sampleValue()
     */
    public double sampleValue() {
        // TODO use a different distribution?
        double timeToNextTour = Math.random()*(endTime-startTime);
        return timeToNextTour+startTime;
    }

    public void setMyHousehold(WeekendHousehold myHousehold) {
        this.myHousehold = myHousehold;
    }

    public WeekendHousehold getMyHousehold() {
        return myHousehold;
    }

}
