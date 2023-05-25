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
package org.sandag.cvm.activityTravel.cvm;

import java.util.Enumeration;
import java.util.Hashtable;

import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;


public class CommercialVehicleTourType extends TourType implements AlternativeUsesMatrices {
	public CommercialVehicleTourType(CommercialVehicleTourTypeChoice choice, String vehicleTourType) {
        super (vehicleTourType, vehicleTourType.charAt(0), (VehicleTourTypeChoice) choice);
        myId = vehicleTourType;
	}
	
	final String myId;
	
	
	
	@Override
	public String toString() {
		return "VehicleTourType:"+myId;

	}

	/**
     * Method readMatrices.
     * @param mr
     */
    public void readMatrices(MatrixCacheReader mr) {
        utilityFunction.readMatrices(mr);
        Enumeration it = conditionalUtilityFunctions.elements();
        while (it.hasMoreElements()) {
        	IndexLinearFunction bob = (IndexLinearFunction) it.nextElement();
        	bob.readMatrices(mr);
        }
    }




    IndexLinearFunction utilityFunction = new IndexLinearFunction();
    Hashtable conditionalUtilityFunctions = new Hashtable();
    
    class TripOutputMatrixSpec {
        String name;
        Matrix matrix;
        float startTime;
        float endTime;
    }

    public double getUtility() {
        double utility = utilityFunction.calcForIndex(this.myChoice.getMyTour().getOriginZone(),1);
        Integer myZoneType = new Integer(((CommercialTour) this.myChoice.getMyTour()).getOriginZoneType());
       	ZonePairDisutility uf = (ZonePairDisutility) conditionalUtilityFunctions.get(myZoneType);
       	if (uf != null) {
       		utility += uf.calcForIndex(this.myChoice.getMyTour().getOriginZone(),1);
       	}
       	return utility;
    }
        
    public void addCoefficient(String index1, String index2, String matrix, double coefficient) {
        if (index1.equals("origin")) {
            utilityFunction.addCoefficient(matrix,coefficient);
        } else if (index1.equals("originLU") || index1.equals("originConstant")){
        	Integer luCondition = Integer.valueOf(index2);
        	IndexLinearFunction conditionalUtilityFunction = (IndexLinearFunction) conditionalUtilityFunctions.get(luCondition);
        	if (conditionalUtilityFunction == null) {
        		conditionalUtilityFunction = new IndexLinearFunction();
        		conditionalUtilityFunctions.put(luCondition,conditionalUtilityFunction);
        	}
        	if (index1.equals("originConstant")) {
        		conditionalUtilityFunction.addConstant(coefficient);
        	} else if (index1.equals("originLU")) {
        		conditionalUtilityFunction.addCoefficient(matrix,coefficient);
        	}
        }else if (index1.equals("")) {
        	utilityFunction.addConstant(coefficient);
        } else {             
            throw new RuntimeException("Invalid index1 for alternative "+getCode());
        }
    }

}