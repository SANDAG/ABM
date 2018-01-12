/*
 Travel Model Microsimulation library
 Copyright (C) 2005 PbConsult, JE Abraham and others


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


package org.sandag.cvm.common.model;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class LinearInParametersFunction {

	/**
	 * Constructor for LinearInParametersFunction.
	 */
	public LinearInParametersFunction() {
		new LinearInParametersFunction(0);
	}
    
    public LinearInParametersFunction(int size) {
        coefficients = new double[size];
    }
    
    private double[] coefficients;
    
    public void addCoefficient(double coeffValue) {
        double[] oldCoefficients = coefficients;
        coefficients = new double[oldCoefficients.length+1];
        System.arraycopy(oldCoefficients,0,coefficients,0,oldCoefficients.length);
        coefficients[coefficients.length-1]=coeffValue;
    }
    
    public void setCoefficient(int coeffIndex,double coeffValue) {
        if (coefficients.length <= coeffIndex) {
            double[] oldCoefficients = coefficients;
            coefficients = new double[coeffIndex+1];
            System.arraycopy(oldCoefficients,0,coefficients,0,oldCoefficients.length);
        }
        coefficients[coeffIndex]=coeffValue;
    }
    
    public double getCoefficient(int coeffIndex) {
        return coefficients[coeffIndex];
    }
    
    public double calcProduct(double[] values) {
        double value = 0;
        for (int i=0; i<values.length && i<coefficients.length; i++ ) {
            value += coefficients[i]*values[i];
        }
        return value;
    }
}
        
            
