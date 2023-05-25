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

import org.sandag.cvm.activityTravel.HouseholdInterface;
import org.sandag.cvm.activityTravel.PersonInterface;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WeekendPerson implements PersonInterface {

    WeekendHousehold myHousehold;
    /**
     * @param household
     */
    public WeekendPerson(WeekendHousehold household) {
        myHousehold = household;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.PersonInterface#getMyHousehold()
     */
    public HouseholdInterface getMyHousehold() {
        return myHousehold;
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.PersonInterface#getAge()
     */
    public int getAge() {
        // TODO add age attribute
        throw new RuntimeException("getAge() is not yet implemented for WeekendPerson");
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.PersonInterface#getPersonID()
     */
    public long getPersonID() {
        // TODO Auto-generated method stub
        throw new RuntimeException("getPersonID() is not yet implemented for WeekendPerson");
    }

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.PersonInterface#isFemale()
     */
    public boolean isFemale() {
        // TODO Auto-generated method stub
        throw new RuntimeException("isFemale() is not yet implemented for WeekendPerson");
    }
    
    public boolean atHome= true;
    
    public double returnTime=0;

    /**
     * @return Returns the atHome.
     */
    public boolean isAtHome() {
        return atHome;
    }

    /**
     * @param atHome The atHome to set.
     */
    public void setAtHome(boolean atHome) {
        this.atHome = atHome;
    }

    /**
     * @return Returns the returnTime.
     */
    public double getReturnTime() {
        return returnTime;
    }

    /**
     * @param returnTime The returnTime to set.
     */
    public void setReturnTime(double returnTime) {
        this.returnTime = returnTime;
    }

}
