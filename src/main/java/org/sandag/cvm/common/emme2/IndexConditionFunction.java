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

package org.sandag.cvm.common.emme2;

import com.pb.common.matrix.*;

import java.util.*;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class IndexConditionFunction {

	public void readMatrices(MatrixCacheReader mr) {
        coefficients = new double[coefficientValues.size()];
        matrices = new Matrix[matrixNames.size()];
//        conditions = new int[conditionValues.size()];
        for (int i=0;i<matrices.length;i++ ) {
            coefficients[i] = ((Double) coefficientValues.get(i)).doubleValue();
//            conditions[i] = ((Integer) conditionValues.get(i)).intValue();
            matrices[i] = mr.readMatrix((String) matrixNames.get(i));
            if (matrices[i] instanceof RowVector) matrices[i]=matrices[i].getTranspose();
        }
            
    }       
    

	/**
	 * Constructor for SingleIndexLinearFunction.
	 */
	public IndexConditionFunction() {
		super();
	}
    
    private double[] coefficients = null;
    private Matrix [] matrices = null;
   // private int[] conditions = null;
    
    private ArrayList coefficientValues = new ArrayList();
    private ArrayList matrixNames = new ArrayList();
    private ArrayList conditionValues = new ArrayList();
    
	/**
	 * Method addCoefficient.
	 * @param matrix the emme/2 name of the matrix to be used (if
     * blank the coefficient will be taken as part of the constant term
	 * @param coeffValue value of the coefficient to be multiplied by the emme/2 matrix term
	 */
    public void addCoefficient(String matrix, int condition, double coeffValue) {
        matrixNames.add(matrix);
        coefficientValues.add(new Double(coeffValue));
        conditionValues.add(new Integer(condition));
        coefficients = null;
        matrices = null;
    }
    
    
    public double calcForIndex(int i,int j) {
        if (coefficients == null) throw new RuntimeException("Didn't read matrices from emme2");
        double value =0;
        for (int count = 0; count<matrices.length;count++) {
            if (conditionValues.get(count) instanceof Integer) {
                int condition = ((Integer) conditionValues.get(count)).intValue();
                if (Math.abs(matrices[count].getValueAt(j,j)-condition)<.001) value += coefficients[count];
            } else if (conditionValues.get(count) instanceof TwoIntegers) {
                TwoIntegers twoInt = (TwoIntegers) conditionValues.get(count);
                if (Math.abs(matrices[count].getValueAt(j,j)-twoInt.i1)<0.001 &&
                        (Math.abs(matrices[count].getValueAt(i,i)-twoInt.i2)<0.001)) {
                    value += coefficients[count];
                }
               
            }
        }
        return value;
            
    }
    
    static class TwoIntegers {
        final int i1;
        final int i2;
        
        TwoIntegers(int i1p,int i2p) {
            i1 = i1p;
            i2 = i2p;
        }
    }


    /**
     * @param matrix
     * @param destinationCondition
     * @param originCondition
     * @param coefficient
     */
    public void addCoefficient(String matrix, int destinationCondition, int originCondition, double coefficient) {
        matrixNames.add(matrix);
        coefficientValues.add(new Double(coefficient));
        conditionValues.add(new TwoIntegers(destinationCondition,originCondition));
        coefficients = null;
        matrices = null;
        
    }       
    

}
