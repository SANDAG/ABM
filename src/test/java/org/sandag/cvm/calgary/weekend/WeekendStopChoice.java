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

import org.sandag.cvm.activityTravel.StopAlternative;
import org.sandag.cvm.activityTravel.StopChoice;
import com.pb.common.matrix.Matrix;

/**
 * @author John Abraham
 *
 * A cool class created by John Abraham (c) 2003
 */
public class WeekendStopChoice extends StopChoice {

	public WeekendStopChoice(Matrix r, int minZone, int maxZone) {
		super();
        int[] zoneNums = r.getExternalNumbers();
        for (int z = 1;z<zoneNums.length;z++) {
            if (zoneNums[z]!=0) {
            	int theNumber = zoneNums[z];
            	if (theNumber >= minZone && theNumber <= maxZone) {
            		this.addAlternative(new StopAlternative(this, zoneNums[z]));
            	}
            }
        }
	}

    /* (non-Javadoc)
     * @see org.sandag.cvm.activityTravel.ModelWithCoefficients#init()
     */
    public void init() {
        readMatrices(GenerateWeekendTours.matrixReader);
    }

}