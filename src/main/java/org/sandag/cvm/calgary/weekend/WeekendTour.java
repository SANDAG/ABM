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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.sandag.cvm.activityTravel.ChangingTravelAttributeGetter;
import org.sandag.cvm.activityTravel.RealNumberDistribution;
import org.sandag.cvm.activityTravel.Stop;
import org.sandag.cvm.activityTravel.StopAlternative;
import org.sandag.cvm.activityTravel.StopChoice;
import org.sandag.cvm.activityTravel.Tour;
import org.sandag.cvm.activityTravel.VehicleTourTypeChoice;
import org.sandag.cvm.activityTravel.cvm.TourStartTimeModel;
import org.sandag.cvm.common.model.NoAlternativeAvailable;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WeekendTour extends Tour {
    
    static final Logger logger = Logger.getLogger("org.sandag.cvm.calgary.weekend");
    
    
    /**
     * The <code>primaryPerson</code> is the person who makes a full tour, from origin and back again.
     */
    public WeekendPerson primaryPerson; 
    ArrayList otherPeople = new ArrayList(); // the other people in the tour
    

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#sampleStops()
     */
    public void sampleStops() {
        String tourType = myVehicleTourType.getTourTypeName();
        if (tourType.equals("SELSE") || tourType.equals("chauf")) {
            sampleReturnStops();
        }
        else {
            samplePrimaryStop();
            sampleIntermediateOutboundStop();
            samplePrimaryAndIntermediateDurationAndAddToStopList();
            sampleReturnStops();
        }
    }
    
    private void samplePrimaryAndIntermediateDurationAndAddToStopList() {
        if (intermediateStop !=null) {
            String stopTypeCode = convertPurposeToString(intermediateStop.purpose);
            RealNumberDistribution myDurationModel = (RealNumberDistribution) GenerateWeekendTours.models.get(stopTypeCode+"Duration");
            if (myDurationModel == null) throw new RuntimeException("Can't find duration model "+stopTypeCode+"Duration for stop type number "+intermediateStop.purpose);
            intermediateStop.duration = (float) myDurationModel.sampleValue();
            addStop(intermediateStop);
        }
        String stopTypeCode = convertPurposeToString(primaryStop.purpose);
        RealNumberDistribution myDurationModel = (RealNumberDistribution) GenerateWeekendTours.models.get(stopTypeCode+"Duration");
        if (myDurationModel == null) throw new RuntimeException("Can't find duration model "+stopTypeCode+"Duration for stop type number "+primaryStop.purpose);
        primaryStop.duration = (float) myDurationModel.sampleValue();
        addStop(primaryStop);
    }

    public static int convertPurposeToInt(String stopPurpose) {
        if (stopPurpose.equals("work")) return 1;
        if (stopPurpose.equals("school")) return 2;
        if (stopPurpose.equals("exercise")) return 3;
        if (stopPurpose.equals("relCivic")) return 4;
        if (stopPurpose.equals("social")) return 5;
        if (stopPurpose.equals("entLeisure")) return 6;
        if (stopPurpose.equals("shop")) return 7;
        if (stopPurpose.equals("eat")) return 8;
        if (stopPurpose.equals("dropOff")) return 9;
        if (stopPurpose.equals("outOfTown")) return 10;
        if (stopPurpose.equals("return")) return 11;
        if (stopPurpose.equals("pickUp")) return 12;
        if (stopPurpose.equals("dropOff")) return 13;
        throw new RuntimeException("stop purpose "+stopPurpose+" is not a valid stop purpose type");
    }


    /**
     * @return String representing the stop type
     */
    public static  String convertPurposeToString(int purpose) {
        switch (purpose) {
        case 1: 
            return "work";
        case 2:
            return "school";
        case 3:
            return "exercise";
        case 4:
            return "relCivic";
        case 5:
            return "social";
        case 6:
            return "entLeisure";
        case 7:
            return "shop";
        case 8:
            return "eat";
        case 9:
            return "dropOff";
        case 10:
            return "outOfTown";
        case 11:
            return "return";
        case 12:
            return "pickUp";
        case 13:
            return "dropOff";
        }
        throw new RuntimeException("invalid stop purpose code "+purpose);
    }

    Stop intermediateStop = null;
    
    private void sampleIntermediateOutboundStop() {
        
        //TODO smarter intermediate stop choice existance model
        if (Math.random() > 0.3) return; // 70% chance of no intermediate stop

        StopChoice theModel;
        String stopModelStringCode = getTourTypeCode()+"IntermediateStop";
        theModel = (StopChoice) GenerateWeekendTours.models.get(stopModelStringCode);
        if (theModel == null) throw new RuntimeException("Can't find stop choice model for "+stopModelStringCode);
        theModel.setTour(this);
        intermediateStop = new Stop(this, getCurrentLocation(),getTotalElapsedTimeHrs());
        try {
            intermediateStop.location = ((StopAlternative) theModel.monteCarloChoice()).location;
        } catch (NoAlternativeAvailable e) {
            e.printStackTrace();
            throw new RuntimeException("Can't find a viable intermediate stop alternative",e);
        }

        WeekendStopPurposeChoice purposeModel = (WeekendStopPurposeChoice) GenerateWeekendTours.models.get(getTourTypeCode()+"StopType");
        if (purposeModel == null)throw new RuntimeException("Can't find stop purpose model for "+getTourTypeCode());
        purposeModel.setMyTour(this);
        intermediateStop.purpose = purposeModel.monteCarloSamplePurpose();

    }
    
    Stop primaryStop = null;

    private void samplePrimaryStop() {
        StopChoice theModel;
        String stopModelStringCode = getTourTypeCode()+"PrimaryStop";
        theModel = (StopChoice) GenerateWeekendTours.models.get(stopModelStringCode);
        if (theModel == null) throw new RuntimeException("Can't find stop choice model for "+stopModelStringCode);
        theModel.setTour(this);
        // FIXME will need to rewrite start location and start time if there are any intermediate stops outbound
        primaryStop = new Stop(this, getCurrentLocation(),getCurrentTimeHrs());
        try {
            primaryStop.location= ((StopAlternative) theModel.monteCarloChoice()).location;
        } catch (NoAlternativeAvailable e) {
            e.printStackTrace();
            throw new RuntimeException("Can't find a viable primary stop alternative",e);
        }
        primaryStop.purpose = convertPurposeToInt(getTourTypeCode()); // assume tour type code with primary stops are subset of stop type codes
    }
    
    final int returnStopTypeCode = convertPurposeToInt("return");

    private void sampleReturnStops() {
        StopChoice theModel;
        String stopModelStringCode = getTourTypeCode()+"ReturnStop";
        theModel = (StopChoice) GenerateWeekendTours.models.get(stopModelStringCode);
        if (theModel == null) throw new RuntimeException("Can't find stop choice model for "+stopModelStringCode);
        final int maxReturnStops = 100;
        int returnStops = 0;
        Stop stop;
        do {
            theModel.setTour(this);
            stop = new Stop(this, getCurrentLocation(),getCurrentTimeHrs());
            WeekendStopPurposeChoice purposeModel = (WeekendStopPurposeChoice) GenerateWeekendTours.models.get(getTourTypeCode()+"StopType");
            if (purposeModel == null)throw new RuntimeException("Can't find stop purpose model for "+getTourTypeCode());
            purposeModel.setMyTour(this);
            stop.purpose = purposeModel.monteCarloSamplePurpose();
            if (stop.purpose == returnStopTypeCode) {
                stop.location = getOriginZone();
            }
            try {
                stop.location= ((StopAlternative) theModel.monteCarloChoice()).location;
            } catch (NoAlternativeAvailable e) {
                e.printStackTrace();
                throw new RuntimeException("Can't find a viable return stop alternative",e);
            }
            if (stop.purpose != returnStopTypeCode) {
                RealNumberDistribution myDurationModel = (RealNumberDistribution) GenerateWeekendTours.models.get(WeekendTour.convertPurposeToString(stop.purpose)+"Duration");
                if (myDurationModel == null) throw new RuntimeException("no "+WeekendTour.convertPurposeToString(stop.purpose)+"Duration model");
                stop.duration = (float) myDurationModel.sampleValue();
            }
            addStop(stop);
            returnStops ++;
        } while (stop.purpose != returnStopTypeCode && returnStops <= maxReturnStops);
        if (returnStops >= maxReturnStops) {
            logger.warn("Return stops hit maximum, "+maxReturnStops);
        }
    }

    /**
     * <code>tourTypeChoiceModel</code> is the choice model for the vehicle tour type.  It is currently
     * a static variable, but it is accesssed by getters and setters so could be an instance variable instead, if
     * different tours need different models for choosing the tour type
     */
    static WeekendTourTypeChoice tourTypeChoiceModel;

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#getVehicleTourTypeChoice()
     */
    public VehicleTourTypeChoice getVehicleTourTypeChoice() {
        return tourTypeChoiceModel;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#setVehicleTourTypeChoice(org.sandag.cvm.activityTravel.VehicleTourTypeChoice)
     */
    public void setVehicleTourTypeChoice(VehicleTourTypeChoice vehicleTourTypeChoice) {
        WeekendTour.tourTypeChoiceModel = (WeekendTourTypeChoice) vehicleTourTypeChoice;

    }

   
    static TourInTimeBand tourInTimeBand;

    /**
     * @param titb
     */
    public static void setTourInTimeBand(TourInTimeBand titb) {
        tourInTimeBand = titb;
        
    }

    /**
     * 
     */
    public void buildTourGroupAndFlagPeopleOutOfHome() {
        primaryPerson.atHome = false;
        List peopleInHousehold = (List) primaryPerson.getMyHousehold().getPersons();
        for (int p=0;p<peopleInHousehold.size();p++) {
            WeekendPerson person = (WeekendPerson) peopleInHousehold.get(p);
            if (person.atHome) {
                // TODO build a smarter model about who in the household is coming with the tour leader
                if (Math.random()>0.5) {
                    person.atHome = false;
                    otherPeople.add(person);
                }
            }
        }
        
    }

    /**
     * 
     */
    public void setReturnHomeTimesForGroupMembers() {
        primaryPerson.returnTime = getCurrentTimeHrs();
        for (int p=0;p<otherPeople.size();p++) {
            ((WeekendPerson) otherPeople.get(p)).returnTime=getCurrentTimeHrs();
        }
        
    }

    private static ChangingTravelAttributeGetter travelDisutilityTracker = null;

    static void setTravelDisutilityTracker(ChangingTravelAttributeGetter travelDisutilityTrackerParam) {
        travelDisutilityTracker = travelDisutilityTrackerParam;
    }

    public ChangingTravelAttributeGetter getTravelDisutilityTracker() {
        return travelDisutilityTracker;
    }

    
    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#getTourTypeCode()
     */
    protected String getTourTypeCode() {
        // not return myVehicleTourType.getCode().substring(1);
        // for the weekend model tour type codes do not have the vehicle type at the beginning
        return myVehicleTourType.getCode();
        
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.Tour#getMaxTourTypes()
     */
    public int getMaxTourTypes() {
        return 14;
    }

    public double getCurrentTimeMinutes() {
        return getCurrentTimeHrs()*60;
    }

    public double getTotalElapsedTimeMinutes() {
        return getTotalElapsedTimeHrs()*60;
    }

	public static void setTourStartTimeModel(NextWeekendTourStartTime ttnt) {
		throw new RuntimeException("Not implemented");
		
	}

	public static void setElapsedTravelTimeCalculator(
			WeekendTravelTimeTracker timeTracker) {
		throw new RuntimeException("Not implemented");
		
	}

	@Override
	public ChangingTravelAttributeGetter getElapsedTravelTimeCalculator() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public TourStartTimeModel getTourStartTimeModel() {
		throw new RuntimeException("Not implemented");
	}

}
