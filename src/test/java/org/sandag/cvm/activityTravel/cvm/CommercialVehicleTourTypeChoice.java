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
import org.sandag.cvm.common.model.LogitModel;
import com.pb.common.matrix.Emme2MatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class CommercialVehicleTourTypeChoice extends LogitModel implements ModelUsesMatrices, VehicleTourTypeChoice {
    
    ArrayList<Alternative> alternativesWaitingToBeAddedToNestingStructure = new ArrayList<Alternative>();
    ArrayList<CommercialVehicleTourType> allElementalAlternatives = new ArrayList<CommercialVehicleTourType>();
    
    private void addAlternativeInitially(CommercialVehicleTourType a) {
        alternativesWaitingToBeAddedToNestingStructure.add(a);
        allElementalAlternatives.add(a);
    }

	/**
	 * Constructor for VehicleTypeChoice.
	 * @param types 
	 */
    
    // tour types are LS,LG,LO,MS,MG,MO,HS,HG,HO
	public CommercialVehicleTourTypeChoice(String[] types, boolean nesting) {
		super();
		for (String type : types) {
			addAlternativeInitially(new CommercialVehicleTourType(this, type));
		}
		if (!nesting) {
			// no nesting structure specified
			for (String type : types ) {
				setUpNestingElement(type,"top",1.0);
			}
		}
			
 	}
    
    private CommercialTour myTour;
	private static Logger logger = Logger.getLogger(CommercialVehicleTourTypeChoice.class);
    
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
        // if index1 is "nest" we will set up a nesting structure.
        if (index1.equalsIgnoreCase("nest")) {
            setUpNestingElement(alternative, matrix, coefficient);
        } else {
            boolean found = false;
            Iterator<CommercialVehicleTourType> myAltsIterator = allElementalAlternatives.iterator();
            while (myAltsIterator.hasNext()) {
                Alternative alt = (Alternative) myAltsIterator.next();
                CommercialVehicleTourType vtt = (CommercialVehicleTourType) alt;
                if (vtt.getCode().equals(alternative)) {
                     vtt.addCoefficient(index1, index2, matrix, coefficient);
                     found = true;
                }
            }
            if (!found) throw new RuntimeException("can't find alternative "+alternative+" in vehicleTourType model");
        }
    }

    private void setUpNestingElement(String alternativeName, String nestName, double coefficient) {
        boolean found = false;
        // first check if an elemental alternative;
        for (int i=0;i<allElementalAlternatives.size();i++) {
            CommercialVehicleTourType vtt = allElementalAlternatives.get(i);
            if (vtt.getCode().equalsIgnoreCase(alternativeName)) {
                if (nestName.equalsIgnoreCase("top")) {
                    addAlternative(vtt);
                    alternativesWaitingToBeAddedToNestingStructure.remove(vtt);
                } else {
                    VehicleTourTypeNest nest = findOrMakeNest(nestName);
                    nest.addAlternative(vtt);
                    alternativesWaitingToBeAddedToNestingStructure.remove(vtt);
                }
                found = true;
            }
        }
        if (!found) {
            // not an elemental alternative
            VehicleTourTypeNest nest = findOrMakeNest(alternativeName);
            nest.setAlogitNestingCoefficient(coefficient);
            if (nestName.equalsIgnoreCase("top")) {
                addAlternative(nest);
                alternativesWaitingToBeAddedToNestingStructure.remove(nest);
            } else {
                VehicleTourTypeNest higherNest = findOrMakeNest(nestName);
                higherNest.addAlternative(nest);
                alternativesWaitingToBeAddedToNestingStructure.remove(nest);
            }
        }
    }

    private VehicleTourTypeNest findOrMakeNest(String alternative) {
        // search alternatives waiting to be added into nesting structure
        for (int i =0;i<alternativesWaitingToBeAddedToNestingStructure.size();i++) {
            if (alternativesWaitingToBeAddedToNestingStructure.get(i) instanceof VehicleTourTypeNest) {
                VehicleTourTypeNest n = (VehicleTourTypeNest) alternativesWaitingToBeAddedToNestingStructure.get(i);
                if (n.getCode().equalsIgnoreCase(alternative)) {
                    return n;
                }
            }
        }
        // search nesting structure
        try {
            return recursivlyLookForNestingElementNamed(this, alternative);
        } catch (AlternativeNotFoundException e) {
            // now we have to make it 'cause we didn't find it
        }
        VehicleTourTypeNest vttn = new VehicleTourTypeNest(alternative);
        alternativesWaitingToBeAddedToNestingStructure.add(vttn);
        return vttn;
    }
    
    public static class AlternativeNotFoundException extends Exception {

        public AlternativeNotFoundException(String alternative) {
            super(alternative);
        }
        
    }

    private static VehicleTourTypeNest recursivlyLookForNestingElementNamed(LogitModel lm,String alternative) throws AlternativeNotFoundException {
        for (int i=0;i<lm.numberOfAlternatives();i++) {
            if (lm.alternativeAt(i) instanceof LogitModel) {
                try {
                    return recursivlyLookForNestingElementNamed((LogitModel) lm.alternativeAt(i), alternative);
                } catch (AlternativeNotFoundException e) {
                    // keep looking if we're not at the top level
                }
            }
            if (lm.alternativeAt(i) instanceof VehicleTourTypeNest) {
                VehicleTourTypeNest vttn = (VehicleTourTypeNest) lm.alternativeAt(i);
                if (vttn.getCode().equalsIgnoreCase(alternative)) return vttn;
            }
        }
        throw new AlternativeNotFoundException(alternative);
    }


    /**
	 * Method readMatrices.
	 * @param matrixReader
	 */
	public void readMatrices(MatrixCacheReader matrixCacheReader) {
        // first check to make sure all alternatives have been added to the nesting structure
        if (alternativesWaitingToBeAddedToNestingStructure.size()!=0) {
            String msg = "Not all VehicleTourType alternatives were added to the nesting structure\n" + alternativesWaitingToBeAddedToNestingStructure;
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
        Collection myAlts = this.alternatives;
        Iterator myAltsIterator = allElementalAlternatives.iterator();
        while (myAltsIterator.hasNext()) {
            CommercialVehicleTourType vtt = (CommercialVehicleTourType) myAltsIterator.next();
            vtt.readMatrices(matrixCacheReader);
        }
	}

	/**
	 * 
	 */
	public void writeTourAndTripSummary() {
		Collection myAlts = this.allElementalAlternatives;
		Iterator myAltsIterator = myAlts.iterator();
		while (myAltsIterator.hasNext()) {
			CommercialVehicleTourType vtt = (CommercialVehicleTourType) myAltsIterator.next();
			logger.info("  Type "+vtt.getTourTypeName()+" generates "+vtt.getTourCount()+" tours and "+vtt.getTripCount()+" trips.");
		}
	}

    public void setMyTour(Tour myTour) {
        if (! (myTour instanceof CommercialTour)) {
            throw new RuntimeException("Commercial TNCVehicle Tour Type Choice can only work with Tours of type CommercialTour");
        }
        this.myTour = (CommercialTour) myTour;
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