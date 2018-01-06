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
import java.util.Collection;
import java.util.List;

import org.sandag.cvm.activityTravel.HouseholdInterface;
import org.sandag.cvm.activityTravel.PersonInterface;
import org.sandag.cvm.common.datafile.TableDataSet;


/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WeekendHousehold implements HouseholdInterface {

    ArrayList myPeople =null;
    private static int incrementalId = 0;
    int id;
    private boolean incomeValid;
    private float annualIncome;
    private int numberOfVehicles;
    private int adultOtherCount; // ao
    private int adultWorkerNeedingCarCount; // awnc
    private int adultWorkerNotNeedingCarCount;     // awnnc
    private int studentKto9Count;     //kejs
    private int postSecondaryStudentCount;    //pss
    private int seniorCount;    // Sen
    private int student10to12Count;    //SHS
    private  int youthOtherCount;    //YO
    private double currentTime;
    private boolean homelessHousehold = true;
    private int homeZone = 0;
    
    static NextWeekendTourStartTime myTourStartTimeModel = null;
    
    public WeekendHousehold(TableDataSet populationHouseholds, int rowNum, TableDataSet sampleHouseholds) {
        homeZone = (int) populationHouseholds.getValueAt(rowNum,"Zone");
        homelessHousehold = false;
        int hhid = (int) populationHouseholds.getValueAt(rowNum,"HHID");
        int sampleRowNum = sampleHouseholds.getIndexedRowNumber(hhid);
        fillInDataFromDataSet(sampleHouseholds, sampleRowNum);
        id = incrementalId++;
    }
    
    
    
    public WeekendHousehold(TableDataSet tds, int rowNum) {
        // For integration with household synthesis, need to get household attributes
        // and people information from another file.
        id=(int) tds.getValueAt(rowNum,"hh_ID");
        fillInDataFromDataSet(tds,rowNum);
    }
    
    void fillInDataFromDataSet(TableDataSet tds, int rowNum) {
        //TODO check to see if value targets in synthesis deal with missing values properly
        annualIncome = tds.getValueAt(rowNum,"Value");
        if (annualIncome < -100000) {
            annualIncome = 0;
            incomeValid = false;
        } else {
            incomeValid = true;
        }
        numberOfVehicles = (int) tds.getValueAt(rowNum,"CountOfveh_id");
        adultOtherCount = (int) tds.getValueAt(rowNum,"AO");
        adultWorkerNeedingCarCount = (int) tds.getValueAt(rowNum,"AWNC");
        adultWorkerNotNeedingCarCount = (int) tds.getValueAt(rowNum,"AWNNC");
        studentKto9Count = (int) tds.getValueAt(rowNum,"KEJS");
        postSecondaryStudentCount = (int) tds.getValueAt(rowNum,"PSS");
        seniorCount = (int)tds.getValueAt(rowNum,"Sen");
        student10to12Count = (int) tds.getValueAt(rowNum,"SHS");
        youthOtherCount = (int)tds.getValueAt(rowNum,"YO");
    }
    
    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.HouseholdInterface#getId()
     */
    public int getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.HouseholdInterface#getPersons()
     */
    public Collection getPersons() {
        if (myPeople == null) {
            makeMyPeople();
        }
        return myPeople;
    }

    private void makeMyPeople() {
        // TODO add in other attributes of people as necessary
        // TODO get RID of this method, as people should be read from the database.  use AddPeople instead
        myPeople = new ArrayList();
        int peopleToMake = getPersonCount();
        for (int p=0;p<peopleToMake;p++) {
            WeekendPerson person = new WeekendPerson(this);
            myPeople.add(person);
        }
        
    }



    /**
     * @return Returns the adultOtherCount.
     */
    public int getAdultOtherCount() {
        return adultOtherCount;
    }

    /**
     * @return Returns the adultWorkerNeedingCarCount.
     */
    public int getAdultWorkerNeedingCarCount() {
        return adultWorkerNeedingCarCount;
    }

    /**
     * @return Returns the adultWorkerNotNeedingCarCount.
     */
    public int getAdultWorkerNotNeedingCarCount() {
        return adultWorkerNotNeedingCarCount;
    }

    /**
     * @return Returns the annualIncome.
     */
    public float getAnnualIncome() {
        // TODO throw error if income is blank or zero
        return annualIncome;
    }

    /**
     * @return Returns the incomeValid.
     */
    public boolean isIncomeValid() {
        return incomeValid;
    }

    /**
     * @return Returns the numberOfVehicles.
     */
    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

    /**
     * @return Returns the postSecondaryStudentCount.
     */
    public int getPostSecondaryStudentCount() {
        return postSecondaryStudentCount;
    }

    /**
     * @return Returns the seniorCount.
     */
    public int getSeniorCount() {
        return seniorCount;
    }

    /**
     * @return Returns the student10to12Count.
     */
    public int getStudent10to12Count() {
        return student10to12Count;
    }

    /**
     * @return Returns the studentKto9Count.
     */
    public int getStudentKto9Count() {
        return studentKto9Count;
    }

    /**
     * @return Returns the youthOtherCount.
     */
    public int getYouthOtherCount() {
        return youthOtherCount;
    }

    /**
     * 
     */
    public void resetCurrentTime() {
        // TODO check if Midnight is the right start time;
        currentTime = 0.0; // Start at midnight
        if (myPeople!=null) {
            for (int p=0;p<myPeople.size();p++) {
                // TODO check if person at home at midnight, should be an attribute in the person file, and an attribute of the person
                ((WeekendPerson)myPeople.get(p)).atHome = true;
            }
        }
    }

    /**
     * @return
     */
    public WeekendTour sampleNextWeekendTour() {
        WeekendTour tour = new WeekendTour();
        // Sequence is 1) see if another tour in this time block
        //if so, sample time from uniform distribution, and if not
        //advance to next time step.
        
        // TODO check whether TourInTimeBand object needs to be different for different household types
        TourInTimeBand titb = tour.tourInTimeBand;
        titb.setBandBasedOnTime(getCurrentTime());
        titb.currentHousehold = this;
        boolean startedATour = false;
        while (! titb.beyondLastBand() && !startedATour) {
            if (titb.tourStartsInBand()) {
                startedATour = true;
            } else {
                titb.currentBand++;
                setCurrentTime(titb.getCurrentBandStart());
            }
        }
        if (!startedATour) return null;
        getMyTourStartTimeModel().setMyHousehold(this);
        getMyTourStartTimeModel().startTime=getCurrentTime();
        getMyTourStartTimeModel().endTime=titb.getCurrentBandEnd();
        tour.sampleStartTime();
        setCurrentTime(tour.getCurrentTimeHrs());
        tour.sampleVehicleAndTourType();
        tour.primaryPerson = this.selectTourLeader();
        tour.buildTourGroupAndFlagPeopleOutOfHome();
        tour.setOrigin(this.getHomeZone());
        tour.sampleStops();
        tour.setReturnHomeTimesForGroupMembers();
        return tour;
    }

    /**
     * @return
     */
    public int getHomeZone() {
        if (homelessHousehold) throw new RuntimeException ("tried to get the home zone of a household that hasn't been assigned a home");
        return homeZone;
    }

    /**
     * @return
     */
    private WeekendPerson selectTourLeader() {
        // TODO Select tour leader using tour leader model -- currently just pick a random person
        int peopleAtHome = 0;
        for (int p=0;p<getPersonCount();p++) {
            if (((WeekendPerson) getPeopleList().get(p)).atHome) peopleAtHome++;
        }
        int person = (int) (Math.random() * peopleAtHome) +1;
        int peopleAtHome2 = 0;
        for (int p=0;p<getPersonCount();p++) {
            WeekendPerson somebody = (WeekendPerson) getPeopleList().get(p);
            if (somebody.atHome) peopleAtHome2++;
            if (peopleAtHome2== person) return somebody;
        }
        // no one home
        return null;
    }

    /**
     * @return
     */
    public List getPeopleList() {
        return (List) getPersons(); 
    }



    /**
     * @return Returns the myTourStartTimeModel.
     */
    public static NextWeekendTourStartTime getMyTourStartTimeModel() {
        
        // TODO tour start time model will probably be different for different households perhaps.
        if(myTourStartTimeModel==null) {
            myTourStartTimeModel = (NextWeekendTourStartTime) GenerateWeekendTours.models.get("tourStart");
        }
        return myTourStartTimeModel;
    }

    /**
     * @param myTourStartTimeModel The myTourStartTimeModel to set.
     */
    public static void setMyTourStartTimeModel(NextWeekendTourStartTime myTimeToNextTour) {
        WeekendHousehold.myTourStartTimeModel = myTimeToNextTour;
    }

    /**
     * @return
     */
    public int getPersonCount() {
        return getAdultOtherCount()+getAdultWorkerNeedingCarCount()+
        getAdultWorkerNotNeedingCarCount()+getPostSecondaryStudentCount()+
        getSeniorCount()+getStudent10to12Count()+getStudentKto9Count()+getYouthOtherCount();
    }

    public void addPeople() {
        // TODO Auto-generated method stub
        
    }

    public void addPeople(TableDataSet thePeople) {
        // TODO Write this method to get the people out of the TableDataSet and attach them to the household.
        
    }



    /**
     * @return
     */
    public int countPeopleAtHome() {
        int count = 0;
        for (int p=0;p<getPersons().size();p++) {
            if (((WeekendPerson) myPeople.get(p)).atHome) count++;
        }
        return count;
    }



    void setCurrentTime(double currentTimeParameter) {
        if (currentTimeParameter < currentTime) {
            throw new RuntimeException("Time is rolling backwards for household "+id);
        }
        currentTime = currentTimeParameter;
        if (myPeople !=null) {
            for (int p =0;p<myPeople.size();p++) {
                WeekendPerson person = (WeekendPerson)myPeople.get(p);
                if (currentTime > person.returnTime) person.atHome = true;
            }
        }
    }



    double getCurrentTime() {
        return currentTime;
    }
    
}
