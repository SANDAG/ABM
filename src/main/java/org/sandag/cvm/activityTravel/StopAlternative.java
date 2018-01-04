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


public class StopAlternative implements CodedAlternative {
		final StopChoice c;
        public final int location;
        public double getUtility() {
            
            if (c.maxDist >0) {
            	assert c.distanceMatrix!=null : "Distance Matrix didn't get initialized, yet maxDist set to "+c.maxDist;
            	double distance = c.distanceMatrix.getValueAt(c.myTour.getOriginZone(),location);
            	
            	if((distance<-99999) || (distance>99999))
            		distance=0;
            	
            	if (distance >c.maxDist) {
            		return Double.NEGATIVE_INFINITY;
            	}
            }

            boolean firstStop = (c.myTour.getStopCounts()[0]==0);
            
            double utility = c.travelUtilityFunction.calcForIndex(c.myTour.getCurrentLocation(),location);
            utility += c.destinationUtilityFunction.calcForIndex(location,1);
            utility += c.returnHomeUtilityFunction.calcForIndex(location,c.myTour.getOriginZone());
            
            if (c.disutilityToOriginCoefficient!=0 || c.disutilityToOriginAdditionalCoefficientForStopGT1!=0) {
                double coefficient = c.disutilityToOriginCoefficient + (firstStop ? 0 : c.disutilityToOriginAdditionalCoefficientForStopGT1);
                utility += coefficient*c.myTour.getTravelDisutilityTracker().getTravelAttribute(location,c.myTour.getOriginZone(),c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
            }
            if (c.disutilityToNextStopCoefficient!=0 || c.disutilityToNextStopAdditionalCoefficientForStopGT1 !=0) {
                double coefficient = c.disutilityToNextStopCoefficient + (firstStop ? 0 : c.disutilityToNextStopAdditionalCoefficientForStopGT1);
                utility += coefficient*c.myTour.getTravelDisutilityTracker().getTravelAttribute(c.myTour.getCurrentLocation(),location,c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
            }
            if (c.timeToOriginCoefficient!=0) {
                double timeToOriginUtility = c.timeToOriginCoefficient*c.myTour.getElapsedTravelTimeCalculator().getTravelAttribute(location,c.myTour.getOriginZone(),c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
                // Doug and Kevin Hack of Jan 5 2004
//                if (myTour.getTotalElapsedTime()>240.0) timeToOriginUtility*=3;
                utility += timeToOriginUtility;
            }
            if (c.timeToNextStopCoefficient!=0) {
                double timeToNextStopUtility = c.timeToNextStopCoefficient*c.myTour.getElapsedTravelTimeCalculator().getTravelAttribute(c.myTour.getCurrentLocation(),location,c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
                // Doug and Kevin Hack of Jan 5 2004
//                if (myTour.getTotalElapsedTime()>240.0) timeToNextStopUtility*=3;
                utility += timeToNextStopUtility;
            }
            
            
            if (c.xMatrix !=null && c.yMatrix != null) {
            	// angle calculation and max distance

                double xOrig = c.xMatrix.getValueAt(c.myTour.getOriginZone(),1);
                double yOrig = c.yMatrix.getValueAt(c.myTour.getOriginZone(),1);
                double xNow = c.xMatrix.getValueAt(c.myTour.getCurrentLocation(),1);
                double yNow = c.yMatrix.getValueAt(c.myTour.getCurrentLocation(),1);
                double xMaybe = c.xMatrix.getValueAt(location,1);
                double yMaybe = c.yMatrix.getValueAt(location,1);
                
                double angle1 = Math.atan2(yNow-yOrig,xNow-xOrig);
                double angle2 = Math.atan2(yMaybe-yNow,xMaybe-xNow);
                double angle = (angle2-angle1)+Math.PI;
                if (angle > Math.PI*2) angle -= Math.PI*2;
                if (angle <0) angle += Math.PI*2;
                if (angle > Math.PI) angle =2*Math.PI-angle;
                utility += c.angleCoefficient*angle*180/Math.PI;
            }
            utility += c.zoneTypeUtilityFunction.calcForIndex(c.myTour.getCurrentLocation(),location);
            if (c.sizeTermCoefficient !=0) {
                double sizeTermValue = Math.log(c.sizeTerm.calcForIndex(location,1));
                utility += c.sizeTermCoefficient*sizeTermValue;
            }
            return utility;
        }   
        
        public StopAlternative(StopChoice choice, int stopLocation) {
            this.location = stopLocation;
            this.c = choice;
		}
        
        public String getCode() {
            return String.valueOf(location);
        }




    }