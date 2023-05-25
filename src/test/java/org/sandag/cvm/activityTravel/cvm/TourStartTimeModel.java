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

import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class TourStartTimeModel implements ModelUsesMatrices, RealNumberDistribution {
    
   /** returns tour start time in hours since midnight
     */
   static boolean startTimeInMinutes = false;
	
   double periodStart; // base time for the TourStartTimeModel
   int functionalForm; // power =1, cubic=2 or exponential =3
   double a;
   double b;
   double c;
   double d;
   double e;
   double f;
   
   
    public double sampleValue() {
        double x = Math.random();
        double y=0;
        switch (functionalForm) {
            case 1:
                y = a * Math.pow(x,b)+c*Math.pow(x,d) + e * x + f;
                break;
            case 2:
                y = a+b*x+c*x*x+d*x*x*x;
                break;
            case 3:
                y = c*Math.exp(a*x+b)+d;
                break;
        }
        if (startTimeInMinutes) y = y/60;
        if (y<0) y =0;
        if (y>24) y = 24;
        y += periodStart;
        return y;
    }
    
    
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
        double coefficient) throws CoefficientFormatError  {
            if (index1.equals("a")) a = coefficient;
            else if(index1.equals("b")) b = coefficient;
            else if(index1.equals("c")) c = coefficient;
            else if(index1.equals("d")) d = coefficient;
            else if(index1.equals("e")) e = coefficient;
            else if(index1.equals("f")) f = coefficient;
            else if(index1.equals("functionForm")) {
                if (index2.equals("power")) functionalForm =1;
                else if (index2.equals("cubic")) functionalForm = 2;
                else if (index2.equals("exponential")) functionalForm = 3;
                else throw new CoefficientFormatError("functionalForm for tour start model must have index2 as \"power\", \"cubic\" or \"exponential\"");
            }
            else if (index1.equals("periodStart")) periodStart=coefficient;
            else throw new CoefficientFormatError("Tour start time model model coefficients must have index1 as a,b,c,d,e,f, periodStart or functionalForm");
        }
        
    /**
     * Method readMatrices.
     * @param matrixReader
     */
    public void readMatrices(MatrixCacheReader matrixReader) {}

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */
    public void init() {
        readMatrices(GenerateCommercialTours.matrixReader);
    }

    

}
