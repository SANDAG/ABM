/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Created on Mar 31, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.sandag.cvm.common.skims;

import java.util.ArrayList;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LinearFunctionOfSomeSkims
    implements TravelUtilityCalculatorInterface {
    
    SomeSkims lastSkims;
    
    ArrayList coefficientsList = new ArrayList();
    ArrayList namesList = new ArrayList();
    double[] coefficients;
    int[] matrixIndices;

    /* (non-Javadoc)
     * @see com.pb.models.pecas.TravelUtilityCalculatorInterface#getUtility(com.pb.models.pecas.TravelAttributesInterface)
     */
    public double getUtility(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            double utility = 0;
            for (int i=0;i<coefficients.length;i++) {
            	double matrixValue = lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
            	if((matrixValue<-99999)||(matrixValue>99999))
            		matrixValue=0;
                utility += coefficients[i]*matrixValue;
            }
            return utility;
        }
        if (travelConditions instanceof SomeSkims) {
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            return getUtility(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }
    
    public void addSkim(String name, double coefficient) {
        namesList.add(name);
        coefficientsList.add(new Double(coefficient));
        coefficients = new double[coefficientsList.size()];
        // store it in a double array for speed, and an ArrayList for resizing.
        for (int i=0;i<namesList.size();i++) {
            coefficients[i] = ((Double) (coefficientsList.get(i))).doubleValue();
        }
        
    }

    /* (non-Javadoc)
     * @see com.pb.models.pecas.TravelUtilityCalculatorInterface#getUtilityComponents(int, int, com.pb.models.pecas.TravelAttributesInterface)
     */
    public double[] getUtilityComponents(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            double[] components = new double[coefficients.length];
            for (int i=0;i<coefficients.length;i++) {
            	double matrixValue = lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
            	if((matrixValue<-99999)||(matrixValue>99999))
            		matrixValue=0;
                components[i] = coefficients[i]*matrixValue;
            }
            return components;
        }
        if (travelConditions instanceof SomeSkims) {
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            return getUtilityComponents(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }

    public double getUtility(Location origin, Location destination, TravelAttributesInterface travelConditions) {
        // ENHANCEMENT eventually we want to be able to have parcel or grid cell or x-y coordinates for the origin and destination
        return getUtility(origin.getZoneNumber(),destination.getZoneNumber(), travelConditions);
    }

}
