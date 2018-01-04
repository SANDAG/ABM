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

import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.ModelWithCoefficients;
import org.sandag.cvm.activityTravel.Tour;
import org.sandag.cvm.activityTravel.TourType;
import org.sandag.cvm.activityTravel.VehicleTourTypeChoice;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.LogitModel;
import com.pb.common.matrix.Emme2MatrixReader;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WeekendTourTypeChoice extends LogitModel
    implements VehicleTourTypeChoice, ModelWithCoefficients {

    Tour myTour;

    /**
     * 
     */
    public WeekendTourTypeChoice() {
        super();
        // Add in the auto alternatives
        this.addAlternative(new WeekendTourType("SELSE",'A',this));
        this.addAlternative(new WeekendTourType("work",'A',this));
        this.addAlternative(new WeekendTourType("school",'A',this));
        this.addAlternative(new WeekendTourType("relCivic",'A',this));
        this.addAlternative(new WeekendTourType("exercise",'A',this));
        this.addAlternative(new WeekendTourType("outOfTown",'A',this));
        this.addAlternative(new WeekendTourType("chauf",'A',this));
        
        // TODO add in the transit/walk alternatives
        // TODO add in the bike alternatives
    }

    /**
     * @param numberOfAlternatives
     */
    public WeekendTourTypeChoice(int numberOfAlternatives) {
        super(numberOfAlternatives);
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.VehicleTourTypeChoice#writeTourAndTripSummary()
     */
    public void writeTourAndTripSummary() {
        // TODO Write out summary of tours and trpis

    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.VehicleTourTypeChoice#getMyTour()
     */
    public Tour getMyTour() {
        return myTour;
    }

    MatrixCacheReader matrixReader;

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#addCoefficient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, double)
     */
    public void addCoefficient(String alternative, String index1, String index2, String matrixName, double coefficient) throws CoefficientFormatError {
        boolean found = false;
        for (int i=0;i<alternatives.size();i++) {
            WeekendTourType a = (WeekendTourType) alternatives.get(i);
            if (a.getTourTypeName().equals(alternative)) {
                a.addCoefficient(alternative,index1,index2,matrixName,coefficient);
                found = true;
            }
        }
        if (found == false) throw new CoefficientFormatError(alternative+" is not a valid alternative in tour type choice");
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#init()
     */
    public void init() {
        for (int i =0;i<alternatives.size();i++) {
            WeekendTourType a = (WeekendTourType) alternatives.get(i);
            a.init();
        }
    }

    /**
     * @return Returns the matrixReader.
     */
    public MatrixCacheReader getMatrixReader() {
        return matrixReader;
    }

    /**
     * @param matrixReader2 The matrixReader to set.
     */
    public void setMatrixReader(MatrixCacheReader matrixReader2) {
        this.matrixReader = matrixReader2;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.VehicleTourTypeChoice#setMyTour(org.sandag.cvm.activityTravel.Tour)
     */
    public void setMyTour(Tour aTour) {
        this.myTour = aTour;
    }

}
