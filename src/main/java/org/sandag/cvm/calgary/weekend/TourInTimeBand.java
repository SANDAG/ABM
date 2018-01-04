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

import java.util.ArrayList;

import org.sandag.cvm.activityTravel.AlternativeUsesMatrices;
import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.ModelWithCoefficients;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.Alternative;
import org.sandag.cvm.common.model.LogitModel;
import org.sandag.cvm.common.model.NoAlternativeAvailable;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.MatrixReader;

/**
 * @author jabraham
 *
 * Controls the time bands, increments through them, and has the model as
 * to whether another tour occurs in the time band
 */
public class TourInTimeBand extends LogitModel implements ModelWithCoefficients {
    
    ArrayList bandStarts = new ArrayList();
    double dayEnd = 24*60;
    final Alternative noTour;
    final Alternative makeATour;
    int currentBand = 0;
    double makeATourConstant = 0;
    WeekendHousehold currentHousehold=null;
    
    double getCurrentBandStart() {
        if (currentBand >= bandStarts.size()) {
            return dayEnd;
        } else {
            return ((Double)bandStarts.get(currentBand)).doubleValue();
        }
    }
    
    double getCurrentBandEnd() {
        if (currentBand +1 >= bandStarts.size()) {
            return dayEnd;
        } else {
            return ((Double)bandStarts.get(currentBand+1)).doubleValue();
        }
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#addCoefficient(java.lang.String, java.lang.String, java.lang.String, java.lang.String, double)
     */
    public void addCoefficient(String alternative, String index1, String index2, String matrixName, double coefficient) throws CoefficientFormatError {
        Double lastBandStart = null;
        if (bandStarts.size() == 0) lastBandStart = new Double(Double.NEGATIVE_INFINITY);
        else lastBandStart =(Double)(bandStarts.get(bandStarts.size()-1));
        if (alternative.equalsIgnoreCase("bandStart")) {
            if (coefficient <= lastBandStart.doubleValue()) {
                throw new CoefficientFormatError("Start bands for tour time band model need to be specified in increasing order");
            }
            bandStarts.add(new Double(coefficient));
        } else if (alternative.equalsIgnoreCase("dayEnd")) {
            if (coefficient <= lastBandStart.doubleValue()) {
                throw new CoefficientFormatError("End of day for tour time band model need to be greater than last time band");
            }
            dayEnd = coefficient;
        } else if (alternative.equalsIgnoreCase("constant")) {
            makeATourConstant = coefficient;
        } else {
            throw new CoefficientFormatError("Bad coefficient for tour band model "+alternative+" "+index1+" "+index2+" "+matrixName);
        }
            
        // TODO other coefficients
        
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.calgary.weekend.ModelWithCoefficients#init()
     */
    public void init() {
        // TODO read matrices if necessary
    }

    public TourInTimeBand() {
        super();
        noTour = new AlternativeUsesMatrices() {
            public void addCoefficient(String index1, String index2, String matrix, double coefficient) throws CoefficientFormatError {
                throw new CoefficientFormatError("The NoTour alternative has no coefficients and baseline utility zero");
            }

            public void readMatrices(MatrixCacheReader mr) {
                // nothing to do
            }

            public String getCode() {
                return "noTour";
            }

            public double getUtility() {
                return 0;
            }
        };
        makeATour = new AlternativeUsesMatrices() {
            public void addCoefficient(String index1, String index2, String matrix, double coefficient) throws CoefficientFormatError {
                throw new CoefficientFormatError("The NoTour alternative has no coefficients and baseline utility zero");
            }

            public void readMatrices(MatrixCacheReader mr) {
                // read matrices if we need to.
            }

            public String getCode() {
                return "makeATour";
            }

            public double getUtility() {
                if (getCurrentBandEnd()<=getCurrentBandStart()){
                    return Double.NEGATIVE_INFINITY;
                }
                return makeATourConstant + Math.log(getCurrentBandEnd()-getCurrentBandStart()) + Math.log(currentHousehold.countPeopleAtHome());
                // TODO add in other parameters related to number of people at home and household size etc.
            }
        };
        addAlternative(noTour);
        addAlternative(makeATour);
    }

    public boolean beyondLastBand() {
        if (currentBand>=bandStarts.size()) return true;
        return false;
    }

    /**
     * @return
     */
    public boolean tourStartsInBand() {
        Alternative chosen=null;
        try {
            chosen = this.monteCarloChoice();
        } catch (NoAlternativeAvailable e) {
            e.printStackTrace();
            throw new RuntimeException("Error in TourStartsInBand module",e);
        }
        return chosen == makeATour;
    }

    /**
     * @param currentTime
     */
    public void setBandBasedOnTime(double currentTime) {
        if (bandStarts.size() == 0) {
            currentBand = 0;
            return;
        }
        if (currentTime > dayEnd) {
            currentBand = bandStarts.size();
            return;
        }
        if (currentTime < ((Double)bandStarts.get(0)).doubleValue()) {
            throw new RuntimeException("Current household time isn't in any time band");
        }
        for (currentBand = bandStarts.size()-1;currentBand >=0;currentBand--) {
            if (currentTime >= ((Double)bandStarts.get(currentBand)).doubleValue()) {
                return;
            }
        }
    }
}
