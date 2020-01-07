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

import java.util.ArrayList;
import java.util.Iterator;

import org.sandag.cvm.activityTravel.ChangingTravelAttributeGetter;
import org.sandag.cvm.activityTravel.ModelUsesMatrices;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class TravelTimeTracker
    implements ChangingTravelAttributeGetter, ModelUsesMatrices {

    protected final ArrayList travelTimeMatrices = new ArrayList();

    
    
    public static class TravelTimeMatrixSpec {
        final String name;
        Matrix matrix;
        final float startTime;
        final float endTime;
        final char vehicleType;
        
        public TravelTimeMatrixSpec(String name, float startTime, float endTime, char vehicleType) {
            this.name = name;
            this.startTime = startTime;
            this.endTime=endTime;
            this.vehicleType = vehicleType;
        }
    
        /**
         * Method readMatrices.
         * @param matrixReader
         */
        void readMatrices(MatrixCacheReader matrixReader) {
            matrix = matrixReader.readMatrix(name);
        }
    
        /**
         * Method getTimeFromMatrix.
         * @param origin
         * @param destination
         * @return double
         * 
         * Note: modified to return 0 if value is greater than 99999
         */
        double getTimeFromMatrix(int origin, int destination) {
            double value= matrix.getValueAt(origin,destination);
            if((value>(-99999)) && (value<99999))
            	return value;
            else
            	return 0;
        }
    
    
    
    }

    /**
     * Method get.
     * @param lastStop
     * @param i
     * @param currentTime
     * @return double
     */
    public double getTravelAttribute(int origin, int destination, double timeOfDay, char vehicleType) {
        TravelTimeMatrixSpec defaultMatrix = null;
        while (timeOfDay>=24.00) timeOfDay -=24.00;
        for (int i =0; i<travelTimeMatrices.size(); i++) {
            TravelTimeMatrixSpec s = (TravelTimeMatrixSpec) travelTimeMatrices.get(i);
            if (s.vehicleType == vehicleType) {
                if (timeOfDay>=s.startTime && timeOfDay < s.endTime && s.vehicleType == vehicleType) return s.getTimeFromMatrix(origin,destination);
                else if (s.startTime <0) defaultMatrix = s;
            }
        }
        if (defaultMatrix==null) throw new RuntimeException("no default travel time matrix for tNCVehicle type "+vehicleType);
        return defaultMatrix.getTimeFromMatrix(origin,destination);
    }

    public void readMatrices(MatrixCacheReader matrixReader) {
        Iterator it = travelTimeMatrices.iterator();
        while (it.hasNext()) {
            TravelTimeMatrixSpec s = (TravelTimeMatrixSpec) it.next();
            s.readMatrices(matrixReader);
        }
    }


}
