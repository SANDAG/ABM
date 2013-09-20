/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.Definitions;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TourDepartureTimeAndDurationDMU;

/**
 * ArcTourDepartureTimeAndDurationDMU is a class that ...
 * 
 * @author Kimberly Grommes
 * @version 1.0, Jul 17, 2008 Created by IntelliJ IDEA.
 */
public class SandagTourDepartureTimeAndDurationDMU
        extends TourDepartureTimeAndDurationDMU
{

    public SandagTourDepartureTimeAndDurationDMU(ModelStructure modelStructure)
    {
        super(modelStructure);
        setupMethodIndexMap();
    }

    public double getDestinationEmploymentDensity()
    {
        return destEmpDen;
    }

    public int getIncomeLessThan30k()
    {
        float incomeInDollars = (float) household.getIncomeInDollars();
        return (incomeInDollars < 30000) ? 1 : 0;
    }

    public int getIncome30kTo60k()
    {
        float incomeInDollars = (float) household.getIncomeInDollars();
        return (incomeInDollars >= 30000 && incomeInDollars < 60000) ? 1 : 0;
    }

    public int getIncomeHigherThan100k()
    {
        float incomeInDollars = (float) household.getIncomeInDollars();
        return (incomeInDollars >= 100000) ? 1 : 0;
    }

    public int getAge()
    {
        return getPersonAge();
    }

    public int getFemale()
    {
        return getPersonIsFemale();
    }

    public int getFemaleWithPreschooler()
    {
        return ((getPersonIsFemale() == 1) && (getNumPreschoolChildrenInHh() > 1)) ? 1 : 0;
    }

    public int getDrivingAgeStudent()
    {
        return (getStudentDrivingAge() == 1) ? 1 : 0;
    }

    public int getSchoolChildWithMandatoryTour()
    {
        return (getStudentNonDrivingAge() == 1 && getPersonMandatoryTotal() > 0) ? 1 : 0;
    }

    public int getUniversityWithMandatoryPattern()
    {
        return (getUniversityStudent() == 1 && person.getCdapActivity().equalsIgnoreCase(
                Definitions.MANDATORY_PATTERN)) ? 1 : 0;
    }

    public int getWorkerWithMandatoryPattern()
    {
        return ((getFullTimeWorker() == 1 || getPartTimeWorker() == 1) && person.getCdapActivity()
                .equalsIgnoreCase(Definitions.MANDATORY_PATTERN)) ? 1 : 0;
    }

    public int getPreschoolChildWithMandatoryTour()
    {
        return (getPreschool() == 1 && getPersonMandatoryTotal() > 0) ? 1 : 0;
    }

    public int getNonWorkerInHH()
    {
        return (getNumNonWorkingAdultsInHh() > 0) ? 1 : 0;
    }

    public int getJointTour()
    {
        return (tour.getTourCategory()
                .equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) ? 1 : 0;
    }

    public int getIndividualTour()
    {
        return (tour.getTourCategory()
                .equalsIgnoreCase(ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY)) ? 1 : 0;
    }

    public int getJointTourInHH()
    {
        return (getHhJointTotal() > 0) ? 1 : 0;
    }

    public int getSubsequentTourIsWorkTour()
    {
        return subsequentTourIsWork;
    }

    public int getSubsequentTourIsSchoolTour()
    {
        return subsequentTourIsSchool;
    }

    public int getNumberOfNonEscortingIndividualTours()
    {
        return getPersonNonMandatoryTotalNoEscort();
    }

    public int getNumberOfDiscretionaryTours()
    {
        return getPersonJointAndIndivDiscrToursTotal();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getDestinationEmploymentDensity", 1);
        methodIndexMap.put("getIncomeLessThan30k", 2);
        methodIndexMap.put("getIncome30kTo60k", 3);
        methodIndexMap.put("getIncomeHigherThan100k", 4);
        methodIndexMap.put("getAge", 5);
        methodIndexMap.put("getFemale", 6);
        methodIndexMap.put("getFemaleWithPreschooler", 7);
        methodIndexMap.put("getFullTimeWorker", 8);
        methodIndexMap.put("getPartTimeWorker", 9);
        methodIndexMap.put("getUniversityStudent", 10);
        methodIndexMap.put("getDrivingAgeStudent", 11);
        methodIndexMap.put("getNonWorkerInHH", 12);
        methodIndexMap.put("getJointTourInHH", 13);
        methodIndexMap.put("getFirstTour", 14);
        methodIndexMap.put("getSubsequentTour", 15);
        methodIndexMap.put("getModeChoiceLogsumAlt", 16);
        methodIndexMap.put("getSubsequentTourIsWorkTour", 17);
        methodIndexMap.put("getSubsequentTourIsSchoolTour", 18);
        methodIndexMap.put("getEndOfPreviousTour", 19);
        methodIndexMap.put("getAllAdultsFullTimeWorkers", 20);
        methodIndexMap.put("getNonWorker", 21);
        methodIndexMap.put("getRetired", 22);
        methodIndexMap.put("getSchoolChildWithMandatoryTour", 23);
        methodIndexMap.put("getPreschoolChildWithMandatoryTour", 24);
        methodIndexMap.put("getNumberOfNonEscortingIndividualTours", 25);
        methodIndexMap.put("getNumberOfDiscretionaryTours", 26);
        methodIndexMap.put("getIndividualTour", 27);
        methodIndexMap.put("getJointTour", 28);
        methodIndexMap.put("getHouseholdSize", 29);
        methodIndexMap.put("getKidsOnJointTour", 30);
        methodIndexMap.put("getAdditionalShoppingTours", 31);
        methodIndexMap.put("getAdditionalMaintenanceTours", 32);
        methodIndexMap.put("getAdditionalVisitingTours", 33);
        methodIndexMap.put("getAdditionalDiscretionaryTours", 34);
        methodIndexMap.put("getMaximumAvailableTimeWindow", 35);
        methodIndexMap.put("getWorkerWithMandatoryPattern", 36);
        methodIndexMap.put("getUnivStudentWithMandatoryPattern", 37);
        methodIndexMap.put("getHhChildUnder16", 38);
        methodIndexMap.put("getToursLeftToSchedule", 39);
        methodIndexMap.put("getPreDrivingAgeChild", 40);
        methodIndexMap.put("getJointTourPartySize", 41);
        methodIndexMap.put("getSubtourPurposeIsEatOut", 42);
        methodIndexMap.put("getSubtourPurposeIsBusiness", 43);
        methodIndexMap.put("getSubtourPurposeIsOther", 44);
        methodIndexMap.put("getMaxJointTimeWindow", 45);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;

        switch (variableIndex)
        {
            case 1:
                returnValue = getDestinationEmploymentDensity();
                break;
            case 2:
                returnValue = getIncomeLessThan30k();
                break;
            case 3:
                returnValue = getIncome30kTo60k();
                break;
            case 4:
                returnValue = getIncomeHigherThan100k();
                break;
            case 5:
                returnValue = getAge();
                break;
            case 6:
                returnValue = getFemale();
                break;
            case 7:
                returnValue = getFemaleWithPreschooler();
                break;
            case 8:
                returnValue = getFullTimeWorker();
                break;
            case 9:
                returnValue = getPartTimeWorker();
                break;
            case 10:
                returnValue = getUniversityStudent();
                break;
            case 11:
                returnValue = getDrivingAgeStudent();
                break;
            case 12:
                returnValue = getNonWorkerInHH();
                break;
            case 13:
                returnValue = getJointTourInHH();
                break;
            case 14:
                returnValue = getFirstTour();
                break;
            case 15:
                returnValue = getSubsequentTour();
                break;
            case 16:
                returnValue = getModeChoiceLogsumAlt(arrayIndex);
                break;
            case 17:
                returnValue = getSubsequentTourIsWorkTour();
                break;
            case 18:
                returnValue = getSubsequentTourIsSchoolTour();
                break;
            case 19:
                returnValue = getEndOfPreviousTour();
                break;
            case 20:
                returnValue = getAllAdultsFullTimeWorkers();
                break;
            case 21:
                returnValue = getNonWorker();
                break;
            case 22:
                returnValue = getRetired();
                break;
            case 23:
                returnValue = getSchoolChildWithMandatoryTour();
                break;
            case 24:
                returnValue = getPreschoolChildWithMandatoryTour();
                break;
            case 25:
                returnValue = getNumberOfNonEscortingIndividualTours();
                break;
            case 26:
                returnValue = getNumberOfDiscretionaryTours();
                break;
            case 27:
                returnValue = getIndividualTour();
                break;
            case 28:
                returnValue = getJointTour();
                break;
            case 29:
                returnValue = getHouseholdSize();
                break;
            case 30:
                returnValue = getKidsOnJointTour();
                break;
            case 31:
                returnValue = getNumIndivShopTours() - 1;
                break;
            case 32:
                returnValue = getNumIndivMaintTours() - 1;
                break;
            case 33:
                returnValue = getNumIndivVisitTours() - 1;
                break;
            case 34:
                returnValue = getNumIndivDiscrTours() - 1;
                break;
            case 35:
                returnValue = getMaximumAvailableTimeWindow();
                break;
            case 36:
                returnValue = getWorkerWithMandatoryPattern();
                break;
            case 37:
                returnValue = getUniversityWithMandatoryPattern();
                break;
            case 38:
                returnValue = getNumChildrenUnder16InHh() > 0 ? 1 : 0;
                break;
            case 39:
                returnValue = getToursLeftToSchedule();
                break;
            case 40:
                returnValue = getPreDrivingAgeChild();
                break;
            case 41:
                returnValue = getJointTourPartySize();
                break;
            case 42:
                returnValue = getSubtourPurposeIsEatOut();
                break;
            case 43:
                returnValue = getSubtourPurposeIsBusiness();
                break;
            case 44:
                returnValue = getSubtourPurposeIsOther();
                break;
            case 45:
                returnValue = getMaxJointTimeWindow();
                break;

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

        return returnValue;

    }

}
