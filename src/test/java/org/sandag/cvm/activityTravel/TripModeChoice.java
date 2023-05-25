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

/*
 * Created on Feb 4, 2005
 *
 */
package org.sandag.cvm.activityTravel;

import java.util.Iterator;

import org.apache.log4j.Logger;

import org.sandag.cvm.activityTravel.cvm.CommercialTripMode;
import org.sandag.cvm.common.emme2.MatrixAndTAZTableCache;
import org.sandag.cvm.common.model.Alternative;
import org.sandag.cvm.common.model.ChoiceModelOverflowException;
import org.sandag.cvm.common.model.DiscreteChoiceModelInterface;
import org.sandag.cvm.common.model.LogitModel;
import org.sandag.cvm.common.model.NoAlternativeAvailable;


/**
 * @author jabraham
 *
 * A model of tNCVehicle type together with tour type
 */
public abstract class TripModeChoice implements ChangingTravelAttributeGetter {
	
	protected LogitModel myLogitModel = new LogitModel();
	
	protected static Logger logger = Logger.getLogger(TripModeChoice.class);
    
	protected Tour theTour;

	/**
     * @param myTour the tour to consider when making the tour type choice
     */
    public void setMyTour(Tour myTour) {
    	theTour = myTour;
    }
      


    /**
     * @return the tour associated with the tour type choice
     */
    public Tour getMyTour() {
    	return theTour;
    }



	@Override
	public double getTravelAttribute(int origin, int destination, double time,
			char vehicleType) {
		Iterator<Alternative> m = myLogitModel.getAlternativesIterator();
		while (m.hasNext()) {
			TripMode tm = (TripMode) m.next();
			tm.setOrigin(origin);
			tm.setDestination(destination);
			tm.setTime(time);
		}
		return myLogitModel.getUtility();
	}



	public void readMatrices(MatrixAndTAZTableCache matrixReader) {
		Iterator<Alternative> m = myLogitModel.getAlternativesIterator();
		while (m.hasNext()) {
			CommercialTripMode tm = (CommercialTripMode) m.next();
			tm.readMatrices(matrixReader);
		}
	}
    
	
	public abstract TripMode chooseTripModeForDestination(int location) throws ChoiceModelOverflowException, NoAlternativeAvailable;
    
}