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
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.LogitModel;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

import java.util.*;

import org.apache.log4j.Logger;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class CommercialNextStopPurposeChoice extends LogitModel implements ModelUsesMatrices, TourNextStopPurposeChoice {

	/**
	 * Constructor for VehicleTypeChoice.
	 */
    
    final static int SERVICE = 1;
    final static int GOODS = 2;
    final static int OTHER = 3;
    final static int RETURNTOORIGIN = 4;
	private static Logger logger = Logger.getLogger(CommercialNextStopPurposeChoice.class);
    
    final char tourType;
    
	/**
	 * Constructor CommercialNextStopPurposeChoice.
	 * @param c
	 */
	public CommercialNextStopPurposeChoice(char c) {
        if (c == 'S' || c == 's') this.addAlternative(new NextStopPurpose(SERVICE));
        if (c == 'G' || c == 'g') this.addAlternative(new NextStopPurpose(GOODS));
        this.addAlternative(new NextStopPurpose(OTHER));
        this.addAlternative(new NextStopPurpose(RETURNTOORIGIN));
        tourType = c;
	}
	
	static String decodeStopPurpose(int s) {
		if (s==SERVICE) return "Srv";
		if (s==GOODS) return "Gds";
		if (s==OTHER) return "Oth";
		if (s==RETURNTOORIGIN) return "Est";
		String msg = "Bad stop purpose code "+s;
		logger.fatal(msg);
		throw new RuntimeException(msg);
		}

    
    private CommercialTour myTour;
    
    
    public class NextStopPurpose implements AlternativeUsesMatrices {
        double[] transitionConstants = {0,0,0,0};
        double[] stopCountCoefficients = {0,0,0,0};
		/**
		 * Constructor VehicleTypeAlternative.
		 * @param stopType
		 */
		public NextStopPurpose(int stopType) {
            this.stopType = stopType;
		}
        
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
            utility += transitionConstants[previousStopType];
            int[] stopCounts = getMyTour().getStopCounts();
            // can't return home on first stop
            if (stopCounts[0]==0 && stopType==RETURNTOORIGIN) utility += Double.NEGATIVE_INFINITY;
            utility += stopCountCoefficients[0]*Math.log(stopCounts[0] +1) +
                       stopCountCoefficients[1]*Math.log(stopCounts[1]+1) +
                       stopCountCoefficients[2]*Math.log(stopCounts[2]+1) +
                       stopCountCoefficients[3]*Math.log(stopCounts[3]+1);
            double returnHomeUtility = returnToOriginUtility.calcForIndex(myTour.getCurrentLocation(),getMyTour().getOriginZone());

            // make people return home more -- Doug and Kevin Hack of Jan 5th
            //if (myTour.getTotalElapsedTime()>240.0) returnHomeUtility *=3;
            utility += returnHomeUtility;
            
            utility += totalTravelTimeCoefficient*getMyTour().getTotalTravelTimeMinutes();
            utility += totalTripTimeCoefficient*getMyTour().getTotalElapsedTimeHrs();
            utility += timeToOriginCoefficient*getMyTour().getElapsedTravelTimeCalculator().getTravelAttribute(myTour.getCurrentLocation(),getMyTour().getOrigin(),getMyTour().getCurrentTimeHrs(),getMyTour().getMyVehicleTourType().vehicleType);
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
                if (index2.equals("goods")) transitionConstants[GOODS] = coefficient;
                else if (index2.equals("service")) transitionConstants[SERVICE] = coefficient;
                else if (index2.equals("other")) transitionConstants[OTHER] = coefficient;
                else if (index2.equals("return")) transitionConstants[RETURNTOORIGIN]= coefficient;
                else throw new RuntimeException("previous stop type not known: "+index2);
            } else  if (index1.equals("logStopCount")) {
                if (index2.equals("goods")) stopCountCoefficients[GOODS] = coefficient;
                else if (index2.equals("service")) stopCountCoefficients[SERVICE] = coefficient;
                else if (index2.equals("other")) stopCountCoefficients[OTHER] = coefficient;
                else if (index2.equals("all")) stopCountCoefficients[0] = coefficient;
                else throw new RuntimeException("stop count type not known: "+index2);
            } else  if (index1.equals("timeAccumulator")) {
                totalTravelTimeCoefficient += coefficient;
            } else  if (index1.equals("totalAccumulator")) {
                totalTripTimeCoefficient += coefficient;
            } else  if (index1.equals("travelDisutility") && index2.equals("origin")) {
                disutilityToOriginCoefficient += coefficient;
            } else  if (index1.equals("travelTime") && index2.equals("origin")) {
                timeToOriginCoefficient += coefficient;
            } else if (index1.equals("") && index2.equals("")) {
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
            if (stopType == SERVICE) return "S";
            if (stopType == OTHER) return "O";
            if (stopType == GOODS) return "G";
            if (stopType == RETURNTOORIGIN) return "R";
			return null;
		}
		
		@Override
		public String toString() {
			return "StopPurpose:"+getCode();
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
            if (!found) throw new CoefficientFormatError("Bad alternative in next stop purpose choice model: "+alternative);
	}

	/**
	 * Method readMatrices.
	 * @param matrixReader
	 */
	public void readMatrices(MatrixCacheReader matrixCacheReader) {
       Iterator alternativeIterator = alternatives.iterator();
        while (alternativeIterator.hasNext()) {
           AlternativeUsesMatrices alt = (AlternativeUsesMatrices) alternativeIterator.next();
           alt.readMatrices(matrixCacheReader);
        }
	}

    public void setMyTour(Tour myTour) {
        this.myTour = (CommercialTour)  myTour;
    }

    public Tour getMyTour() {
        return myTour;
    }
    
    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */
    public void init() {
        readMatrices(GenerateCommercialTours.matrixReader);
    }


}