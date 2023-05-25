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

import org.sandag.cvm.activityTravel.ZonePairDisutility;
import com.pb.common.matrix.*;
import java.util.*;

/**
 * @author John Abraham
 *
 * A linear function of values in emme2 matrices
 */
public class IndexLinearFunction implements ZonePairDisutility {

	/**
	 * Constructor
	 */
	public IndexLinearFunction() {
		super();
	}
    
    private double[] coefficients = null;
    private Matrix [] matrices = null;
    
    private ArrayList coefficientValues = new ArrayList();
    private ArrayList matrixNames = new ArrayList();
    
    /** Adds another term into the utility function, with the coefficient on the term coming from 
     * an MS matrix in the emme2 databank.  In other words adds a term<br>
     * MSValue * MFValue
     * <br>Where MSValue is constant and MFValue is the value from the MF matrix
     * @param matrix the MF matrix where the value is looked up based on the indices
     * @param coeffMSMatrix the MS matrix where the coefficient value comes from
     */
    public void addCoefficient(String matrix, String coeffMSMatrix) {
    	matrixNames.add(matrix);
    	coefficientValues.add(coeffMSMatrix);
    	coefficients = null;
    	matrices = null;
    }
    
	/**
	 * Method addCoefficient, adds a term into the utility function <br>
     * coeffValue * MFValue
	 * @param matrix the emme/2 name of the matrix to be used (if
     * blank or "none" the coefficient will be taken as part of the constant term)
	 * @param coeffValue value of the coefficient to be multiplied by the emme/2 matrix term
	 */
    public void addCoefficient(String matrix, double coeffValue) {
        if (matrix.length() ==0 || matrix.equalsIgnoreCase("none")) addConstant(coeffValue);
        else {
            matrixNames.add(matrix);
            coefficientValues.add(new Double(coeffValue));
            coefficients = null;
            matrices = null;
        }
    }
    
    /** Changes the value of a specific coefficient (normally use addCoefficient instead)
     * @param coeffIndex the previously added coefficient to change the value of
     * @param matrix the matrix that is multiplied by the coefficient value
     * @param coeffValue the coefficient value
     */
    void setCoefficient(int coeffIndex,String matrix,double coeffValue) {
        coefficientValues.set(coeffIndex,new Double(coeffValue));
        matrixNames.set(coeffIndex,matrix);
        coefficients = null;
        matrices = null;
    }
    
    /**
     * Reads in the emme2matrices into memory, also initializes some internal data.  Call this method after the 
     * terms have been added to the utility function but before the utility function is evaluated.
     * @param matrixReader the matrix reader to use to read in the matrices from the emme2 databank
     */
    public void readMatrices(MatrixCacheReader matrixReader) {
        coefficients = new double[coefficientValues.size()];
        matrices = new Matrix[matrixNames.size()];
        for (int i=0;i<matrices.length;i++ ) {
        	Object thing = coefficientValues.get(i);
        	double value = 0;
        	if (thing instanceof Double) {
        		value = ((Double) thing).doubleValue();
        	}
        	if (thing instanceof String) {
        		Matrix msMatrix = matrixReader.readMatrix((String) thing);
                int externalNumber = msMatrix.getExternalNumber(0);
        		value = msMatrix.getValueAt(externalNumber,externalNumber);
        	}
        	coefficients[i] = value;
            matrices[i] = matrixReader.readMatrix((String) matrixNames.get(i));
            if (matrices[i] instanceof RowVector) {
                matrices[i] = matrices[i].getTranspose();
            }
            
        }
            
    }       
    
    /* (non-Javadoc)
	 * @see org.sandag.cvm.common.emme2.ZonePairDisutility#calcForIndex(int, int)
	 */
    @Override
	public double calcForIndex(int i,int j) {
        if (coefficients == null) throw new RuntimeException("Didn't read matrices from emme2");
        double value =0;
        for (int count = 0; count<matrices.length;count++) {
            value += matrices[count].getValueAt(i,j)*coefficients[count];
        }
        return value + constant;
            
    }       
    
    private double constant = 0;
    
	/**
	 * Method addConstant.
	 * @param coefficient
	 */
	public void setConstant(double constantValue) {
        constant = constantValue;
	}
    
    public void addConstant(double constantValue) {
        constant += constantValue;
    }

}
