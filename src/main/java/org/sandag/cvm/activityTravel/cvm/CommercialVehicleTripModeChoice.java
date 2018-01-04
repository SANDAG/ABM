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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.Alternative;
import org.sandag.cvm.common.model.ChoiceModelOverflowException;
import org.sandag.cvm.common.model.LogitModel;
import org.sandag.cvm.common.model.NoAlternativeAvailable;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2014
 */
public class CommercialVehicleTripModeChoice extends TripModeChoice implements ModelUsesMatrices {

	Double portionParam = null;
	Double dispersionParam = null;

	public CommercialVehicleTripModeChoice(String[] tripModes) {
		super();
		for (String type : tripModes) {
			myLogitModel.addAlternative(new CommercialTripMode(this, type));
		}
	}

	private static Logger logger = Logger.getLogger(CommercialVehicleTripModeChoice.class);
	/**
	 * Method addParameter.
	 * 
	 * @param alternative
	 * @param matrix
	 * @param coefficient
	 * @throws CoefficientFormatError
	 */
	public void addCoefficient(String alternative, String index1,
			String index2, String matrix, double coefficient)
					throws CoefficientFormatError {
		boolean found = false;
		Iterator<Alternative> myAltsIterator = myLogitModel.getAlternativesIterator();
		while (myAltsIterator.hasNext()) {
			Alternative alt = myAltsIterator.next();
			CommercialTripMode tm = (CommercialTripMode) alt;
			if (tm.getCode().equals(alternative)) {
				tm.addCoefficient(index1, index2, matrix, coefficient);
				found = true;
			}
		}
		if (!found) {
			if (index1.equalsIgnoreCase("dispersion")) {
				dispersionParam = coefficient;
			} else if (index1.equalsIgnoreCase("portion")) {
				portionParam = coefficient;
			} else {
				throw new RuntimeException("can't find alternative "+alternative+" in TripMode model");
			}
		}
	}


	/**
	 * Method readMatrices.
	 * @param matrixReader
	 */
	public void readMatrices(MatrixCacheReader matrixCacheReader) {
		Iterator<Alternative> myAltsIterator = myLogitModel.getAlternativesIterator();
		while (myAltsIterator.hasNext()) {
			CommercialTripMode tm = (CommercialTripMode) myAltsIterator.next();
			tm.readMatrices(matrixCacheReader);
		}
	}


	/* (non-Javadoc)
	 * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
	 */
	public void init() {
		readMatrices(GenerateCommercialTours.matrixReader);
	}

	@Override
	public TripMode chooseTripModeForDestination(int location)
			throws ChoiceModelOverflowException, NoAlternativeAvailable {
		CommercialTripMode choice = (CommercialTripMode) debugChooseTripModeForDestination(location);
		if (choice.getTripMode().equals("T")) {
			// going to check if any tolls are actually paid
			double tollDistance = choice.getTollDistance();
			if (tollDistance ==0) {
				// get the non toll alternative
				Iterator<Alternative> it = myLogitModel.getAlternativesIterator();
				while (it.hasNext()) {
					CommercialTripMode tm = (CommercialTripMode) it.next();
					if (tm.getTripMode().equals("NT")) {
						choice = tm;
						break;
					}
				}
			} else {
			//	logTollTripChoice(); turn this off for now
			}
		}
		return choice;
	}


	public TripMode debugChooseTripModeForDestination(int location)
			throws ChoiceModelOverflowException, NoAlternativeAvailable {
			/*// delete this debug stuff 
					Double tollTime = null;
					Double nTollTime = null;
				*/	
					
					Iterator<Alternative> m = myLogitModel.getAlternativesIterator();
					while (m.hasNext()) {
						CommercialTripMode tm = (CommercialTripMode) m.next();
						tm.setOrigin(getMyTour().getCurrentLocation());
						tm.setDestination(location);
						tm.setTime(getMyTour().getCurrentTimeHrs());
					
						/* delete this debug stuff 
						if (tm.getTripMode().equals("T")) {
							tollTime = tm.getTollTime();
						}
						if (tm.getTripMode().equals("NT")) {
							nTollTime = tm.getNonTollTime();
						}*/
					}
					
					/* delete this debug stuff
					if (tollTime==null || nTollTime==null) {
						String msg = "Oops couldn't extract toll and non toll times for debugging purposes";
						logger.fatal(msg);
						throw new RuntimeException(msg);
					}
					if (nTollTime < tollTime ) {
						logger.warn("nTollTime is less than tollTime for "+getMyTour().getCurrentLocation()+" to "+location);
					} 
					if (nTollTime - tollTime > 5) {
						// 5 minute savings!  Check it out
						logger.info("5 minute savings taking the toll road from "+getMyTour().getCurrentLocation()+" to "+location);
					}
					*/
					return (TripMode) myLogitModel.monteCarloChoice();
				}



	protected void logTollTripChoice() {
		
		StringBuffer msg = new StringBuffer("Toll chosen:");
		msg.append(((CommercialTripMode) myLogitModel.alternativeAt(0)).logOriginDestination());
		msg.append(" for ");
		msg.append(theTour.getMyVehicleTourType());
		msg.append(" ");
		double utility1= ((CommercialTripMode) myLogitModel.alternativeAt(0)).getUtility();
		double utility2 = ((CommercialTripMode) myLogitModel.alternativeAt(1)).getUtility();
		double prob = Math.exp(utility1)/(Math.exp(utility2)+Math.exp(utility1));
		msg.append(((CommercialTripMode)myLogitModel.alternativeAt(0)).getTripMode()+" prob "+prob+" {");
		msg.append(((CommercialTripMode) myLogitModel.alternativeAt(0)).reportAttributes());
		msg.append(" or ");
		msg.append(((CommercialTripMode) myLogitModel.alternativeAt(1)).reportAttributes());
		msg.append("}");
		logger.info(msg);
		
	}


	public boolean isTollAvailable(int origin, int destination, double timeOfDay) {
		Iterator<Alternative> it = myLogitModel.getAlternativesIterator();
		while (it.hasNext()) {
			CommercialTripMode tm = (CommercialTripMode) it.next();
			tm.setOrigin(origin);
			tm.setDestination(destination);
			tm.setTime(timeOfDay);
			if (tm.tripMode.equals("T")) {
				if (tm.getTollDistance() ==0) return false;
			} else {
				return true;
			}
		}
		throw new RuntimeException("No toll option for trip, can't report whether toll was actually available ");
	}

}