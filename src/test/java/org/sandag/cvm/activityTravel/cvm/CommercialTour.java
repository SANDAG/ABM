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
import org.sandag.cvm.common.emme2.MatrixCacheReader;
import org.sandag.cvm.common.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.log4j.Logger;

import com.pb.common.matrix.*;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class CommercialTour extends Tour {
	
	static Logger logger = Logger.getLogger(CommercialTour.class);

    
	private final PrintWriter tripLog;
	private Hashtable models;
	private final GenerateCommercialTours generator;

	
        
//        private String myTripOutputMatrixName;
//        Matrix emme2OutputMatrix;

	static class TripOutputMatrixSpec {

			final String name;
            boolean write;
            Matrix matrix;
            final float startTime;
            final float endTime;
            final char vehicleType;
			public final String tripMode;
            
            TripOutputMatrixSpec(String name, String type, float startTime, float endTime, char vehicleType, String tripMode) {
            	if (type.equals("tripOut")) {
            		write = true;
            	} else if (type.equals("timeDef")) {
            		write = false;
            	} else {
            		String msg = "Invalid type in index1 in TripMatrix "+type;
            		logger.fatal(msg);
            		throw new RuntimeException(msg);
            	}
                this.name = name;
                this.startTime = startTime;
                this.endTime=endTime;
                this.vehicleType = vehicleType;
                this.tripMode = tripMode;
            }

            @Override
			public String toString() {
				return "Outmatrix "+name+":"+vehicleType+"("+startTime+":"+endTime+")";
			}

            /**
             * Method readMatrices.
             * @param matrixReader
             */
            void readMatrices(MatrixCacheReader matrixReader) {
            	matrix = null;
                if (write) matrix = matrixReader.readMatrix(name);
            }

			public void createMatrix(int size,int[] externalNumbers) {
				matrix = new Matrix(name, "Trips for "+vehicleType+" between "+startTime+" and "+endTime,size,size);
				matrix.setExternalNumbers(externalNumbers);
			}

        }


    
	public CommercialTour(Hashtable models, GenerateCommercialTours generator, PrintWriter tripLog, int tourNumber) {
		super();
		this.tripLog = tripLog;
		this.generator = generator;
		this.models = models;
		tourNum = tourNumber;
	}

	/**
	 * Method sampleVehicleAndTourType.
	 */
	public void sampleVehicleAndTourType() {
        synchronized (generator.vehicleTourTypeChoice) {
            generator.vehicleTourTypeChoice.setMyTour(this);
            try {
                myVehicleTourType = (TourType) generator.vehicleTourTypeChoice.monteCarloElementalChoice();
                myNextStopPurposeChoice = (CommercialNextStopPurposeChoice) models.get(myVehicleTourType.getCode()+"StopType");
                ((CommercialNextStopPurposeChoice) myNextStopPurposeChoice).setMyTour(this);
            } catch (NoAlternativeAvailable e) {
                throw new RuntimeException(e);
            }
        }
	}

	/**
	 * Method addTripsToMatrix.
	 */
	public void addTripsToMatrix() {
		int trips = 0;
        Iterator stopIterator = stops.iterator();
        int lastLocation = getOriginZone();
        double currentTime = getTourStartTimeHrs();
        String prevStopType = "Est";
        while (stopIterator.hasNext()) {
            Stop s = (Stop) stopIterator.next();
            trips ++;
            int location = s.location;
            // FIXME should take into account trip mode.
            double legTravelTime = getElapsedTravelTimeCalculator().getTravelAttribute(lastLocation,location,currentTime,myVehicleTourType.getVehicleType());
            float midPointOfTripTime = (float) (currentTime + legTravelTime/60/2);
            
            String newStopType = CommercialNextStopPurposeChoice.decodeStopPurpose(s.purpose);
            
            String mNameForTripLog = "None";
            
            TripOutputMatrixSpec spec = generator.getTripOutputMatrixSpec(getVehicleCode().charAt(0),s.tripMode,midPointOfTripTime);
            if (spec!=null) {
            	mNameForTripLog = spec.name;
            	if (spec.write) {
            		synchronized(spec.matrix) {
            			// increment stop count in the matrix if this matrix is going to be written out
            			float mTripCountEntry = spec.matrix.getValueAt(lastLocation,location);
            			mTripCountEntry++;
            			spec.matrix.setValueAt(lastLocation,location,mTripCountEntry);
            		}
            	}
            }
            // write to trip log
            if (tripLog !=null) {
            	tripLog.print("3,"+tourNum+",1,"+trips+",1,"+getOriginZone()+","+generator.segmentString1+","+prevStopType+","+newStopType+","+lastLocation+","+location+","+mNameForTripLog+","+getVehicleCode()+","+currentTime+",");
            }
            boolean tollAvailable = isTollAvailable(lastLocation,location,currentTime);
            if (s.tripMode.equals("T") && !tollAvailable) {
            	logger.error("No toll available but toll chosen for "+s);
            }

            currentTime += legTravelTime/60;
            currentTime += s.duration;
            if (tripLog !=null) {
            	tripLog.println(currentTime+","+s.duration+","+getTourTypeCode()+","+generator.segmentString2+","+s.tripMode+","+tollAvailable);
            }
            lastLocation = location;
            prevStopType = newStopType;
        }
        myVehicleTourType.incrementTourAndTripCount(1,trips);
	}

	/**
	 * Method sampleStops.
	 */
	public void sampleStops() {
        ((CommercialNextStopPurposeChoice) myNextStopPurposeChoice).setMyTour(this);
        CommercialNextStopChoice myNextStopModel;
        do {
            Stop thisStop = new Stop(this, getCurrentLocation(),getCurrentTimeHrs());
            Alternative temp;
            try {
                temp = myNextStopPurposeChoice.monteCarloChoice();
            } catch (NoAlternativeAvailable e) {
            	logger.fatal("no valid purpose alternative available for "+this, e);
                throw new RuntimeException("no valid purpose alternative available for "+this, e);
            } catch (RuntimeException e) {
            	logger.fatal("Cannot sample stops for "+this, e);
                throw new RuntimeException("Cannot sample stops for "+this, e);
            }
            CommercialNextStopPurposeChoice.NextStopPurpose nextStopPurpose = (CommercialNextStopPurposeChoice.NextStopPurpose) temp;
            thisStop.purpose=nextStopPurpose.stopType;
            if (thisStop.purpose==CommercialNextStopPurposeChoice.RETURNTOORIGIN) {
                thisStop.location=getOriginZone();
                chooseTripMode(thisStop);
                addStop(thisStop);
                break;
            }
            String nextStopModelCode = getVehicleCode()+getTourTypeCode()+nextStopPurpose.getCode() + "StopLocation";
            myNextStopModel = (CommercialNextStopChoice) models.get(nextStopModelCode);
            if (myNextStopModel == null) throw new RuntimeException("Can't find stop model "+nextStopModelCode);
            myNextStopModel.setTour(this);
            try {
                temp = myNextStopModel.monteCarloChoice();
            } catch (NoAlternativeAvailable e) {
            	logger.error("no valid location alternative available from "+this.toString()+" for "+generator.segmentString+" stop purpose "+nextStopModelCode);
                throw new RuntimeException("no valid location alternative available from "+this.toString());
            }
            thisStop.location = ((StopAlternative) temp).location;
            DurationModel myDurationModel = (DurationModel) models.get(myVehicleTourType.getCode()+"Duration");
            thisStop.duration = (float) myDurationModel.sampleValue();
            chooseTripMode(thisStop);
            addStop(thisStop);
        } while (true);
	}

	private void chooseTripMode(Stop thisStop) {
		if (generator.isUseTripModes()) {
			CommercialVehicleTripModeChoice tmc = (CommercialVehicleTripModeChoice) generator.getTravelDisutilityTracker(getVehicleCode());
			tmc.setMyTour(this);
			TripMode m;
			try {
				m = (TripMode) tmc.chooseTripModeForDestination(thisStop.location);
			} catch (ChoiceModelOverflowException | NoAlternativeAvailable e) {
				String msg = "Can't sample trip mode for "+this;
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}
			thisStop.tripMode=m.getCode().split(":")[1];
		}
	}

	private boolean isTollAvailable(int origin, int destination, double timeOfDay) {
		if (!generator.isUseTripModes()) {
			return false;
		}
		CommercialVehicleTripModeChoice tmc = (CommercialVehicleTripModeChoice) generator.getTravelDisutilityTracker(getVehicleCode());
		tmc.setMyTour(this);
		
		return tmc.isTollAvailable(origin,destination,timeOfDay);
	}
	
	/**
	 * Method getTourTypeCode.
	 * @return String
	 */
	public String getTourTypeCode() {
		return myVehicleTourType.getCode().substring(1);
	}


	/**
	 * Method getVehicleCode.
	 */
	public String getVehicleCode() {
        return myVehicleTourType.getCode().substring(0,1);
	}


	private int tourNum;




	/**
	 * @return
	 */
	public int getOriginZoneType() {
		return Math.round(generator.landUseTypeMatrix.getValueAt(getOriginZone(),1));
	}

    public ChangingTravelAttributeGetter getTravelDisutilityTracker() {
    	if (generator.isUseTripModes()) {
    		return generator.getTravelDisutilityTracker(getVehicleCode());
    	}
        return generator.getTravelDisutilityTracker();
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#getMaxTourTypes()
     */
    public int getMaxTourTypes() {
        return 4;
    }

    public double getCurrentTimeMinutes() {
        return getCurrentTimeHrs()*60;
    }

    public double getTotalElapsedTimeMinutes() {
        return getTotalElapsedTimeHrs()*60;
    }

	@Override
	public VehicleTourTypeChoice getVehicleTourTypeChoice() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVehicleTourTypeChoice(
			VehicleTourTypeChoice vehicleTourTypeChoice) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ChangingTravelAttributeGetter getElapsedTravelTimeCalculator() {
		// FIXME should use trip modes, like getTravelDisutilityTracker() does.
		return generator.getElapsedTravelTimeCalculator();
	}

	@Override
	public TourStartTimeModel getTourStartTimeModel() {
		return generator.getTourStartTimeModel();
	}

	

}
