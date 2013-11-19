/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.IndividualMandatoryTourFrequencyDMU;

/**
 * ArcIndividualMandatoryTourFrequencyDMU is a class that ...
 * 
 * @author Kimberly Grommes
 * @version 1.0, Jul 17, 2008 Created by IntelliJ IDEA.
 */
public class SandagIndividualMandatoryTourFrequencyDMU
        extends IndividualMandatoryTourFrequencyDMU
{

    public SandagIndividualMandatoryTourFrequencyDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getDistanceToWorkLocation", 1);
        methodIndexMap.put("getDistanceToSchoolLocation", 2);
        methodIndexMap.put("getEscortAccessibility", 3);
        methodIndexMap.put("getDrivers", 4);
        methodIndexMap.put("getPreschoolChildren", 5);
        methodIndexMap.put("getNumberOfChildren6To18WithoutMandatoryActivity", 6);
        methodIndexMap.put("getNonFamilyHousehold", 7);
        methodIndexMap.put("getIncomeInDollars", 8);
        methodIndexMap.put("getPersonType", 9);
        methodIndexMap.put("getFemale", 10);
        methodIndexMap.put("getAutos", 11);
        methodIndexMap.put("getAge", 12);
        methodIndexMap.put("getBestTimeToWorkLocation", 13);
        methodIndexMap.put("getNotEmployed", 14);
        methodIndexMap.put("getWorkAtHome", 15);
        methodIndexMap.put("getSchoolAtHome", 16);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 1:
                return getDistanceToWorkLocation();
            case 2:
                return getDistanceToSchoolLocation();
            case 3:
                return getEscortAccessibility();
            case 4:
                return getDrivers();
            case 5:
                return getPreschoolChildren();
            case 6:
                return getNumberOfChildren6To18WithoutMandatoryActivity();
            case 7:
                return getNonFamilyHousehold();
            case 8:
                return getIncomeInDollars();
            case 9:
                return getPersonType();
            case 10:
                return getFemale();
            case 11:
                return getAutos();
            case 12:
                return getAge();
            case 13:
                return getBestTimeToWorkLocation();
            case 14:
                return getNotEmployed();
            case 15:
                return getWorkAtHome();
            case 16:
                return getSchoolAtHome();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

}
