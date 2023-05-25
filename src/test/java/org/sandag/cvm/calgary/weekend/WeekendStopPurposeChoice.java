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
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.LogitModel;
import org.sandag.cvm.common.model.NoAlternativeAvailable;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

import java.util.*;

import org.apache.log4j.Logger;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2005
 */
public class WeekendStopPurposeChoice extends LogitModel implements ModelUsesMatrices, TourNextStopPurposeChoice {
    

    private static Logger logger = Logger.getLogger("org.sandag.cvm.calgary.weekend");
    Tour myTour;
    static final int RETURN=WeekendTour.convertPurposeToInt("return");
    
    public class NextStopPurpose implements AlternativeUsesMatrices {
		public NextStopPurpose(int stopType) {
            this.stopType = stopType;
		}
        
        double[] transitionConstants = new double[14];
        double[] stopCountCoefficients = new double[14];
        double constant = 0;

        final int stopType;
        IndexLinearFunction previousStopUtility = new IndexLinearFunction();
        IndexLinearFunction originUtility = new IndexLinearFunction();
        IndexLinearFunction returnToOriginUtility = new IndexLinearFunction();
        double timeToOriginCoefficient = 0;
        double disutilityToOriginCoefficient = 0;
        double totalTravelTimeCoefficient = 0;
        double totalTripTimeCoefficient = 0;
        public double getUtility() {
            double utility = previousStopUtility.calcForIndex(myTour.getCurrentLocation(),1);
            utility += originUtility.calcForIndex(getMyTour().getOriginZone(),1);
            int previousStopType= myTour.getLastStopType();
            // TODO make sure the stop count code works properly for weekend model; default implementation just counts total stops, not by type
            int[] stopCounts = getMyTour().getStopCounts();
            // can't return home on first stop
            if (stopCounts[0]==0 && stopType==RETURN) utility += Double.NEGATIVE_INFINITY;
            for (int type =0;type < stopCountCoefficients.length;type++) {
                utility += stopCountCoefficients[type]*Math.log(stopCounts[type]+1);
            }
            double returnHomeUtility = returnToOriginUtility.calcForIndex(myTour.getCurrentLocation(),getMyTour().getOriginZone());

            // make people return home more -- Doug and Kevin Hack of Jan 5th
            //if (myTour.getTotalElapsedTime()>240.0) returnHomeUtility *=3;
            utility += returnHomeUtility;
            
            utility += totalTravelTimeCoefficient*getMyTour().getTotalTravelTimeMinutes();
            utility += totalTripTimeCoefficient*getMyTour().getTotalElapsedTimeHrs();
            utility += timeToOriginCoefficient*myTour.getElapsedTravelTimeCalculator().getTravelAttribute(myTour.getCurrentLocation(),getMyTour().getOrigin(),getMyTour().getCurrentTimeHrs(),getMyTour().getMyVehicleTourType().vehicleType);
            utility += disutilityToOriginCoefficient*getMyTour().getTravelDisutilityTracker().getTravelAttribute(myTour.getCurrentLocation(),getMyTour().getOrigin(),getMyTour().getCurrentTimeHrs(),getMyTour().getMyVehicleTourType().vehicleType);
            utility += constant;
            return utility;
        }
            
        /**
         * Method addParameter.
         * @param matrix
         * @param coefficient
         */
        public void addCoefficient(String index1, String index2, String matrix, double coefficient) throws CoefficientFormatError {
            if(index1.equals("origin")) {
                originUtility.addCoefficient(matrix,coefficient);
            } else if (index1.equals("cstop")) {
                if (index2.equals("origin")) returnToOriginUtility.addCoefficient(matrix,coefficient);
                else previousStopUtility.addCoefficient(matrix,coefficient);
            } else  if (index1.equals("prevStopType")) {
                int type2 = WeekendTour.convertPurposeToInt(index2);
                transitionConstants[type2] = coefficient;
            } else  if (index1.equals("logStopCount")) {
                int type2 = WeekendTour.convertPurposeToInt(index2);
                stopCountCoefficients[type2] = coefficient;
            } else  if (index1.equals("timeAccumulator")) {
                totalTravelTimeCoefficient += coefficient;
            } else  if (index1.equals("totalAccumulator")) {
                totalTripTimeCoefficient += coefficient;
            } else  if (index1.equals("travelDisutility") && index2.equals("origin")) {
                disutilityToOriginCoefficient += coefficient;
            } else  if (index1.equals("travelTime") && index2.equals("origin")) {
                timeToOriginCoefficient += coefficient;
            } else if ((index1.equals("") || index1.equals("none")) && (index2.equals("") ||index2.equals("none"))) {
                constant += coefficient;
            } else {
                throw new CoefficientFormatError("invalid indexing "+index1+ ","+index2+" in matrix "+matrix +" for next stop purpose model ");
            }
        }
        


        /**
         * Method readMatrices.
         * @param mr
         */
        public void readMatrices(MatrixCacheReader mr) {
            previousStopUtility.readMatrices(mr);
            originUtility.readMatrices(mr);
            returnToOriginUtility.readMatrices(mr);
        }

    		/**
		 * Method getStopPurposeCode.
		 * @return String
		 */
		public String getCode() {
            return WeekendTour.convertPurposeToString(stopType);
		}

}

	/**
	 * Method addParameter.
	 * @param alternative
	 * @param matrix
	 * @param coefficient
	 */
	public void addCoefficient(
		String alternative,
        String index1,
        String index2,
		String matrix,
		double coefficient) throws CoefficientFormatError {
            Iterator alternativeIterator = alternatives.iterator();
            boolean found = false;
            while (alternativeIterator.hasNext()) {
                AlternativeUsesMatrices alt = (AlternativeUsesMatrices) alternativeIterator.next();
                if (alternative.equals(alt.getCode())) {
                    alt.addCoefficient(index1,index2,matrix,coefficient);
                    found = true;
                }
            }
            if (!found) {
                logger.info("adding alternative "+alternative+" to "+name);
                NextStopPurpose newPurpose = new NextStopPurpose(WeekendTour.convertPurposeToInt(alternative));
                addAlternative(newPurpose);
                newPurpose.addCoefficient(index1,index2,matrix,coefficient);
            }
	}

	/**
	 * Method readMatrices.
	 * @param matrixReader
	 */
	public void readMatrices(MatrixCacheReader mr) {
       Iterator alternativeIterator = alternatives.iterator();
        while (alternativeIterator.hasNext()) {
           AlternativeUsesMatrices alt = (AlternativeUsesMatrices) alternativeIterator.next();
           alt.readMatrices(mr);
        }
	}

    public void setMyTour(Tour myTour) {
        this.myTour =  myTour;
    }

    public Tour getMyTour() {
        return myTour;
    }
    
    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */
    public void init() {
        readMatrices(GenerateWeekendTours.matrixReader);
    }


    final String name;

    public WeekendStopPurposeChoice(String myName) {
        super();
        this.name = myName;
    }
    
    int monteCarloSamplePurpose() {
        NextStopPurpose purpose;
        try {
            purpose = (NextStopPurpose) monteCarloChoice();
        } catch (NoAlternativeAvailable e) {
            e.printStackTrace();
            throw new RuntimeException("No valid purposes available",e);
        }
        return purpose.stopType;
    }
}