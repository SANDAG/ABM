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

package org.sandag.cvm.activityTravel;

import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
//import org.sandag.cvm.calgary.commercial.GenerateCommercialTours;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class DurationModel implements ModelUsesMatrices, RealNumberDistribution {
    
    protected double a=0;
    protected double b=0;
    protected double c=0;
    protected double d=0;
    protected double e=0;
    protected double f=0;

    
    /** @return stop duration in hours
     */
    public double sampleValue() {
        double x = -Math.random();
        double y = a*Math.exp(b*x)+c*Math.exp(d*x)+f;
        if (y<0) y=0;
        if (y>24) y = 24;
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
            else if (index1.equals("e")) e = coefficient;
            else if(index1.equals("f")) f = coefficient;
            else throw new CoefficientFormatError("Duration model coefficients must have index1 as a,b,c or d");
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
//        readMatrices(GenerateCommercialTours.matrixReader);
    }

    

}
