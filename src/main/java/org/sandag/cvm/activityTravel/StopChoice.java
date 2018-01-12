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

import org.apache.log4j.Logger;

import org.sandag.cvm.common.emme2.IndexConditionFunction;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.LogitModel;
import com.pb.common.matrix.*;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 * Modified 2014 for trip mode choice
 */
public abstract class StopChoice extends LogitModel implements ModelUsesMatrices {

    static int angleCalculationCounter = 50;
    public static MatrixCacheReader myMatrixCacheReader=null;
    
    static Logger logger = Logger.getLogger(StopChoice.class);
    /*private static Boolean useTripModeChoice = null;*/
    
    
        
    protected double angleCoefficient;
    protected double maxDist;

    protected IndexLinearFunction destinationUtilityFunction = new IndexLinearFunction();

    protected double disutilityToNextStopAdditionalCoefficientForStopGT1;
    protected double disutilityToNextStopCoefficient;
    protected double disutilityToOriginAdditionalCoefficientForStopGT1;
    protected double disutilityToOriginCoefficient;
    protected Tour myTour;
    protected IndexLinearFunction returnHomeUtilityFunction = new IndexLinearFunction();
    protected IndexLinearFunction sizeTerm = null;
    protected double sizeTermCoefficient = 0;
    protected double timeToNextStopCoefficient;
    protected double timeToOriginCoefficient;
    protected IndexLinearFunction travelUtilityFunction = new IndexLinearFunction();
    protected Matrix xMatrix;
    private String xMatrixName;
    protected Matrix yMatrix;
    private String yMatrixName;
    protected IndexConditionFunction zoneTypeUtilityFunction = new IndexConditionFunction();
	public Matrix distanceMatrix;
	private String distanceMatrixName;
    /**
     * Method addCoefficient.
     * @param alternative
     * @param index1
     * @param index2
     * @param matrix
     * @param coefficient
     */
    public void addCoefficient(
    	String alternative,
    	String index1,
    	String index2,
    	String matrix,
    	double coefficient) throws CoefficientFormatError {
            if (!alternative.equals("zone")) throw new RuntimeException("StopAlternative coefficients must have \"zone\" as alternative");
            
            //If Index1 is "cstop" then Index2 must equal "nstop", and the term in the utility function is the entry from the mf matrix identified in the Matrix field, indexed with i being the current stop location and j being the next stop location whose utility is being evaluated.
            if(index1.equals("cstop")) {
                if(index2.equals("nstop")) {
                    travelUtilityFunction.addCoefficient(matrix,coefficient);
                } else throw new CoefficientFormatError("cstop coefficients for next stop location must index an mf matrix using nstop as the J");
            }
            
            
            else if (index1.equals("nstop")) {
                //If Index1 is "nstop" and Index2 is "origin" then Matrix identifies an mf matrix, and the term in the utility function is the matrix value indexed with i being the next stop location whose utility is being evaluated and j is the origin of the tour.
                if(index2.equals("origin")) {
                    returnHomeUtilityFunction.addCoefficient(matrix,coefficient);
            //If Index1 is "nstop" and Index2 is blank, then Matrix identifies an mo or md matrix.  The next stop location under consideration is used to retrieve the appropriate entry from the matrix. 
                } else if (index2.equals("") ||index2.equals("none")) {
                    destinationUtilityFunction.addCoefficient(matrix,coefficient);
                } else throw new CoefficientFormatError("nstop coefficients for next stop location must have index2=\"\" or index2 = origin");
            }
            else if (index1.equals("angle")) {
                angleCoefficient = coefficient;
                setXYNames(matrix, index2);
            }
            else if (index1.equals("travelDisutility")) {
                if (index2.equals("nstop")) {
                    disutilityToNextStopCoefficient+=coefficient;
                } else if (index2.equals("origin")) {
                    disutilityToOriginCoefficient+=coefficient;
                } else if (index2.equalsIgnoreCase("nstopx")){
                    disutilityToNextStopAdditionalCoefficientForStopGT1 += coefficient;
                } else if (index2.equalsIgnoreCase("originx")) {
                    disutilityToOriginAdditionalCoefficientForStopGT1 += coefficient;
                } else {
                    throw new CoefficientFormatError("travelDisutility coefficients for next stop must have be to either \"origin\" or to \"nstop\"");
                }
            }
            else if (index1.equals("travelTime")) {
                if (index2.equals("nstop")) {
                    timeToNextStopCoefficient+=coefficient;
                } else if (index2.equals("origin")) {
                    timeToOriginCoefficient+=coefficient;
                } else {
                    throw new CoefficientFormatError("travelDisutility coefficients for next stop must have be to either \"origin\" or to \"nstop\"");
                }
            }
            else if (index1.equals("sizeTerm1")) {
                sizeTermCoefficient += coefficient;
                if (sizeTerm== null) sizeTerm = new IndexLinearFunction();
                sizeTerm.addCoefficient(matrix,1.0);
            }
            else if (index1.equals("sizeTerm2") || index1.equals("sizeTermx")) {
                if (sizeTerm== null) sizeTerm = new IndexLinearFunction();
                sizeTerm.addCoefficient(matrix,coefficient);
            }
            else if (index1.equals("maxDist")) {
            	maxDist = coefficient;
                distanceMatrixName = matrix;
            }
            else {
                int destinationCondition;
                int originCondition;
                try {
                    destinationCondition = Integer.valueOf(index1).intValue();
                } catch (NumberFormatException e) {
                    throw new CoefficientFormatError("Can't convert "+index1+" to a number, not allowed as an index type for stop location choice");
                }
                boolean twoTypeCondition=false;
                try {
                    originCondition = Integer.valueOf(index2).intValue();
                    zoneTypeUtilityFunction.addCoefficient(matrix,destinationCondition,originCondition,coefficient);
                    twoTypeCondition= true;
               } catch (NumberFormatException e) {
               }
               if (!twoTypeCondition) { 
                zoneTypeUtilityFunction.addCoefficient(matrix,destinationCondition,coefficient);
               }
            }
    }
	private void setXYNames(String xName, String yName) {
		if (xMatrixName==null) {
			xMatrixName = xName;
		} else{
			if (!xMatrixName.equals(xName)) {
				String msg = "xName for angle and maxdist needs to be the same, "+xMatrixName+"!="+xName;
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}
		}
		if (yMatrixName == null) {
			yMatrixName = yName;
		} else {
			if (!yMatrixName.equals(yName)) {
				String msg = "yName for angle and maxdist needs to be the same, "+yMatrixName+"!="+yName;
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}
		}
	}
    /**
     * Returns the myTour.
     * @return CommercialTour
     */
    public Tour getTour() {
    	return myTour;
    }
    public void init() {
        if (myMatrixCacheReader==null) throw new RuntimeException("StopChoice needs an initialized Emme2MatrixReader before it can be initialized");
        readMatrices(myMatrixCacheReader);
    }

    /**
     * Method readMatrices.
     * @param matrixReader
     */
    public void readMatrices(MatrixCacheReader mr) {
        travelUtilityFunction.readMatrices(mr);
        destinationUtilityFunction.readMatrices(mr);
        returnHomeUtilityFunction.readMatrices(mr);
        zoneTypeUtilityFunction.readMatrices(mr);
        if (sizeTerm != null) sizeTerm.readMatrices(mr);
        if (xMatrixName != null) xMatrix = mr.readMatrix(xMatrixName);
        if (yMatrixName!=null) yMatrix = mr.readMatrix(yMatrixName);
        if (distanceMatrixName!=null) {
        	distanceMatrix = mr.readMatrix(distanceMatrixName);
        } else {
        	logger.warn("Distance matrix name not specified, no maximum distance set");
        }
    }

    /**
     * Sets the myTour.
     * @param myTour The myTour to set
     */
    public void setTour(Tour myTour) {
    	this.myTour = myTour;
    }
	/*public static Boolean getUseTripModeChoice() {
		return useTripModeChoice;
	}
	public static void setUseTripModeChoice(Boolean useTripModeChoice) {
		StopChoice.useTripModeChoice = useTripModeChoice;
	}*/

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */

}