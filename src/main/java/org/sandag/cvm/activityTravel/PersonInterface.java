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
 * Created on 24-Mar-2005
 *
 */
package org.sandag.cvm.activityTravel;

/**
 * @author jabraham
 *
 * A representation of a person, and a few important elements of what a person is
 */
public interface PersonInterface {
    
    
    /**
     * @return the household that the person belongs to
     */
    public HouseholdInterface getMyHousehold();
    
    /**
     * @return the aga of the person in years
     */
    public int getAge();
    
    /**
     * @return a unique identifier for the person
     */
    public long getPersonID();
    
    /**
     * @return whether the person is female or not
     */
    public boolean isFemale();

}
