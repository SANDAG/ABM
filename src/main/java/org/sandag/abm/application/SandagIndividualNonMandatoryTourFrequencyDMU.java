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
import org.sandag.abm.ctramp.IndividualNonMandatoryTourFrequencyDMU;

/**
 * ArcIndividualNonMandatoryTourFrequencyDMU is a class that ...
 * 
 */
public class SandagIndividualNonMandatoryTourFrequencyDMU
        extends IndividualNonMandatoryTourFrequencyDMU
{

    public SandagIndividualNonMandatoryTourFrequencyDMU()
    {
        super();
        setupMethodIndexMap();

        // set names used in SANDAG stop purpose file
        TOUR_FREQ_ALTERNATIVES_FILE_ESCORT_NAME = "escort";
        TOUR_FREQ_ALTERNATIVES_FILE_SHOPPING_NAME = "shopping";
        TOUR_FREQ_ALTERNATIVES_FILE_MAINT_NAME = "othmaint";
        TOUR_FREQ_ALTERNATIVES_FILE_EAT_OUT_NAME = "eatout";
        TOUR_FREQ_ALTERNATIVES_FILE_VISIT_NAME = "visit";
        TOUR_FREQ_ALTERNATIVES_FILE_DISCR_NAME = "othdiscr";
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getIncomeInDollars", 0);
        methodIndexMap.put("getHouseholdSize", 1);
        methodIndexMap.put("getNumAutos", 2);
        methodIndexMap.put("getCarsEqualsWorkers", 3);
        methodIndexMap.put("getMoreCarsThanWorkers", 4);
        methodIndexMap.put("getNumAdults", 5);
        methodIndexMap.put("getNumChildren", 6);
        methodIndexMap.put("getPersonIsAdult", 7);
        methodIndexMap.put("getPersonIsChild", 8);
        methodIndexMap.put("getPersonIsFullTimeWorker", 9);
        methodIndexMap.put("getPersonIsPartTimeWorker", 10);
        methodIndexMap.put("getPersonIsUniversity", 11);
        methodIndexMap.put("getPersonIsNonworker", 12);
        methodIndexMap.put("getPersonIsPreschool", 13);
        methodIndexMap.put("getPersonIsStudentNonDriving", 14);
        methodIndexMap.put("getPersonIsStudentDriving", 15);
        methodIndexMap.put("getPersonStaysHome", 16);
        methodIndexMap.put("getFemale", 17);
        methodIndexMap.put("getFullTimeWorkers", 18);
        methodIndexMap.put("getPartTimeWorkers", 19);
        methodIndexMap.put("getUniversityStudents", 20);
        methodIndexMap.put("getNonWorkers", 21);
        methodIndexMap.put("getDrivingAgeStudents", 22);
        methodIndexMap.put("getNonDrivingAgeStudents", 23);
        methodIndexMap.put("getPreSchoolers", 24);
        // methodIndexMap.put("getMaxAdultOverlaps", 26);
        // methodIndexMap.put("getMaxChildOverlaps", 27);
        // methodIndexMap.put("getMaxMixedOverlaps", 28);
        // methodIndexMap.put("getMaxPairwiseOverlapAdult", 29);
        // methodIndexMap.put("getMaxPairwiseOverlapChild", 30);
        // methodIndexMap.put("getWindowBeforeFirstMandJointTour", 31);
        // methodIndexMap.put("getWindowBetweenFirstLastMandJointTour", 32);
        // methodIndexMap.put("getWindowAfterLastMandJointTour", 33);
        methodIndexMap.put("getNumHhFtWorkers", 34);
        methodIndexMap.put("getNumHhPtWorkers", 35);
        methodIndexMap.put("getNumHhUnivStudents", 36);
        methodIndexMap.put("getNumHhNonWorkAdults", 37);
        methodIndexMap.put("getNumHhRetired", 38);
        methodIndexMap.put("getNumHhDrivingStudents", 39);
        methodIndexMap.put("getNumHhNonDrivingStudents", 40);
        methodIndexMap.put("getNumHhPreschool", 41);
        methodIndexMap.put("getTravelActiveAdults ", 42);
        methodIndexMap.put("getTravelActiveChildren ", 43);
        methodIndexMap.put("getNumMandatoryTours", 44);
        methodIndexMap.put("getNumJointShoppingTours", 45);
        methodIndexMap.put("getNumJointOthMaintTours", 46);
        methodIndexMap.put("getNumJointEatOutTours", 47);
        methodIndexMap.put("getNumJointSocialTours", 48);
        methodIndexMap.put("getNumJointOthDiscrTours", 49);
        methodIndexMap.put("getJTours", 50);
        methodIndexMap.put("getPreDrivingAtHome", 51);
        methodIndexMap.put("getPreschoolAtHome", 52);
        methodIndexMap.put("getDistanceToWorkLocation", 53);
        methodIndexMap.put("getDistanceToSchoolLocation", 54);
        methodIndexMap.put("getEscortAccessibility", 55);
        methodIndexMap.put("getShopAccessibility", 56);
        methodIndexMap.put("getMaintAccessibility", 57);
        methodIndexMap.put("getEatOutAccessibility", 58);
        methodIndexMap.put("getVisitAccessibility", 59);
        methodIndexMap.put("getDiscrAccessibility", 60);
        methodIndexMap.put("getCdapIndex", 61);
        methodIndexMap.put("getNonMotorizedDcLogsum", 62);
        methodIndexMap.put("getNumPredrivingKidsGoOut", 63);
        methodIndexMap.put("getNumPreschoolKidsGoOut", 64);
        methodIndexMap.put("getCollegeEducation", 65);
        methodIndexMap.put("getLowEducation", 66);
        methodIndexMap.put("getDetachedHh", 67);
        methodIndexMap.put("getWorksAtHome", 68);
        methodIndexMap.put("getWorkAccessibility", 69);
        methodIndexMap.put("getSchoolAccessibility", 70);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {

            case 0:
                return getIncomeInDollars();
            case 1:
                return getHouseholdSize();
            case 2:
                return getNumAutos();
            case 3:
                return getCarsEqualsWorkers();
            case 4:
                return getMoreCarsThanWorkers();
            case 5:
                return getNumAdults();
            case 6:
                return getNumChildren();
            case 7:
                return getPersonIsAdult();
            case 8:
                return getPersonIsChild();
            case 9:
                return getPersonIsFullTimeWorker();
            case 10:
                return getPersonIsPartTimeWorker();
            case 11:
                return getPersonIsUniversity();
            case 12:
                return getPersonIsNonworker();
            case 13:
                return getPersonIsPreschool();
            case 14:
                return getPersonIsStudentNonDriving();
            case 15:
                return getPersonIsStudentDriving();
            case 16:
                return getPersonStaysHome();
            case 17:
                return getFemale();
            case 18:
                return getFullTimeWorkers();
            case 19:
                return getPartTimeWorkers();
            case 20:
                return getUniversityStudents();
            case 21:
                return getNonWorkers();
            case 22:
                return getDrivingAgeStudents();
            case 23:
                return getNonDrivingAgeStudents();
            case 24:
                return getPreSchoolers();
                // case 26:
                // return getMaxAdultOverlaps();
                // case 27:
                // return getMaxChildOverlaps();
                // case 28:
                // return getMaxMixedOverlaps();
                // case 29:
                // return getMaxPairwiseOverlapAdult();
                // case 30:
                // return getMaxPairwiseOverlapChild();
                // case 31:
                // return getWindowBeforeFirstMandJointTour();
                // case 32:
                // return getWindowBetweenFirstLastMandJointTour();
                // case 33:
                // return getWindowAfterLastMandJointTour();
            case 34:
                return getNumHhFtWorkers();
            case 35:
                return getNumHhPtWorkers();
            case 36:
                return getNumHhUnivStudents();
            case 37:
                return getNumHhNonWorkAdults();
            case 38:
                return getNumHhRetired();
            case 39:
                return getNumHhDrivingStudents();
            case 40:
                return getNumHhNonDrivingStudents();
            case 41:
                return getNumHhPreschool();
            case 42:
                return getTravelActiveAdults();
            case 43:
                return getTravelActiveChildren();
            case 44:
                return getNumMandatoryTours();
            case 45:
                return getNumJointShoppingTours();
            case 46:
                return getNumJointOthMaintTours();
            case 47:
                return getNumJointEatOutTours();
            case 48:
                return getNumJointSocialTours();
            case 49:
                return getNumJointOthDiscrTours();
            case 50:
                return getJTours();
            case 51:
                return getPreDrivingAtHome();
            case 52:
                return getPreschoolAtHome();
            case 53:
                return getDistanceToWorkLocation();
            case 54:
                return getDistanceToSchoolLocation();
            case 55:
                return getEscortAccessibility();
            case 56:
                return getShopAccessibility();
            case 57:
                return getMaintAccessibility();
            case 58:
                return getEatOutAccessibility();
            case 59:
                return getVisitAccessibility();
            case 60:
                return getDiscrAccessibility();
            case 61:
                return getCdapIndex();
            case 62:
                return getNonMotorizedDcLogsum();
            case 63:
                return getNumPredrivingKidsGoOut();
            case 64:
                return getNumPreschoolKidsGoOut();
            case 65:
                return getCollegeEducation();
            case 66:
                return getLowEducation();
            case 67:
                return getDetachedHh();
            case 68:
                return getWorksAtHome();
            case 69:
                return getWorkAccessibility();
            case 70:
                return getSchoolAccessibility();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

}
