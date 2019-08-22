package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.CoordinatedDailyActivityPatternDMU;

public class SandagCoordinatedDailyActivityPatternDMU
        extends CoordinatedDailyActivityPatternDMU
{

    public SandagCoordinatedDailyActivityPatternDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getFullTimeWorkerA", 0);
        methodIndexMap.put("getFullTimeWorkerB", 1);
        methodIndexMap.put("getFullTimeWorkerC", 2);
        methodIndexMap.put("getPartTimeWorkerA", 3);
        methodIndexMap.put("getPartTimeWorkerB", 4);
        methodIndexMap.put("getPartTimeWorkerC", 5);
        methodIndexMap.put("getUniversityStudentA", 6);
        methodIndexMap.put("getUniversityStudentB", 7);
        methodIndexMap.put("getUniversityStudentC", 8);
        methodIndexMap.put("getNonWorkingAdultA", 9);
        methodIndexMap.put("getNonWorkingAdultB", 10);
        methodIndexMap.put("getNonWorkingAdultC", 11);
        methodIndexMap.put("getRetiredA", 12);
        methodIndexMap.put("getRetiredB", 13);
        methodIndexMap.put("getRetiredC", 14);
        methodIndexMap.put("getDrivingAgeSchoolChildA", 15);
        methodIndexMap.put("getDrivingAgeSchoolChildB", 16);
        methodIndexMap.put("getDrivingAgeSchoolChildC", 17);
        methodIndexMap.put("getPreDrivingAgeSchoolChildA", 18);
        methodIndexMap.put("getPreDrivingAgeSchoolChildB", 19);
        methodIndexMap.put("getPreDrivingAgeSchoolChildC", 20);
        methodIndexMap.put("getPreSchoolChildA", 21);
        methodIndexMap.put("getPreSchoolChildB", 22);
        methodIndexMap.put("getPreSchoolChildC", 23);
        methodIndexMap.put("getAgeA", 24);
        methodIndexMap.put("getFemaleA", 25);
        methodIndexMap.put("getMoreCarsThanWorkers", 26);
        methodIndexMap.put("getFewerCarsThanWorkers", 27);
        methodIndexMap.put("getZeroCars", 28);
        methodIndexMap.put("getHHIncomeInDollars", 29);
        methodIndexMap.put("getHhDetach", 30);
        methodIndexMap.put("getUsualWorkLocationIsHomeA", 31);
        methodIndexMap.put("getNoUsualWorkLocationA", 32);
        methodIndexMap.put("getNoUsualSchoolLocationA", 33);
        methodIndexMap.put("getHhSize", 34);
        methodIndexMap.put("getWorkLocationModeChoiceLogsumA", 35);
        methodIndexMap.put("getSchoolLocationModeChoiceLogsumA", 36);
        methodIndexMap.put("getRetailAccessibility", 37);
        methodIndexMap.put("getNumAdultsWithNonMandatoryDap", 38);
        methodIndexMap.put("getNumAdultsWithMandatoryDap", 39);
        methodIndexMap.put("getNumKidsWithNonMandatoryDap", 40);
        methodIndexMap.put("getNumKidsWithMandatoryDap", 41);
        methodIndexMap.put("getAllAdultsAtHome", 42);
        methodIndexMap.put("getWorkAccessForMandatoryDap", 43);
        methodIndexMap.put("getTelecommuteFrequencyA", 44);
        methodIndexMap.put("getTelecommuteFrequencyB", 45);
        methodIndexMap.put("getTelecommuteFrequencyC", 46);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getFullTimeWorkerA();
            case 1:
                return getFullTimeWorkerB();
            case 2:
                return getFullTimeWorkerC();
            case 3:
                return getPartTimeWorkerA();
            case 4:
                return getPartTimeWorkerB();
            case 5:
                return getPartTimeWorkerC();
            case 6:
                return getUniversityStudentA();
            case 7:
                return getUniversityStudentB();
            case 8:
                return getUniversityStudentC();
            case 9:
                return getNonWorkingAdultA();
            case 10:
                return getNonWorkingAdultB();
            case 11:
                return getNonWorkingAdultC();
            case 12:
                return getRetiredA();
            case 13:
                return getRetiredB();
            case 14:
                return getRetiredC();
            case 15:
                return getDrivingAgeSchoolChildA();
            case 16:
                return getDrivingAgeSchoolChildB();
            case 17:
                return getDrivingAgeSchoolChildC();
            case 18:
                return getPreDrivingAgeSchoolChildA();
            case 19:
                return getPreDrivingAgeSchoolChildB();
            case 20:
                return getPreDrivingAgeSchoolChildC();
            case 21:
                return getPreSchoolChildA();
            case 22:
                return getPreSchoolChildB();
            case 23:
                return getPreSchoolChildC();
            case 24:
                return getAgeA();
            case 25:
                return getFemaleA();
            case 26:
                return getMoreCarsThanWorkers();
            case 27:
                return getFewerCarsThanWorkers();
            case 28:
                return getZeroCars();
            case 29:
                return getHHIncomeInDollars();
            case 30:
                return getHhDetach();
            case 31:
                return getUsualWorkLocationIsHomeA();
            case 32:
                return getNoUsualWorkLocationA();
            case 33:
                return getNoUsualSchoolLocationA();
            case 34:
                return getHhSize();
            case 35:
                return getWorkLocationModeChoiceLogsumA();
            case 36:
                return getSchoolLocationModeChoiceLogsumA();
            case 37:
                return getRetailAccessibility();
            case 38:
                return getNumAdultsWithNonMandatoryDap();
            case 39:
                return getNumAdultsWithMandatoryDap();
            case 40:
                return getNumKidsWithNonMandatoryDap();
            case 41:
                return getNumKidsWithMandatoryDap();
            case 42:
                return getAllAdultsAtHome();
            case 43:
                return getWorkAccessForMandatoryDap();
            case 44:
                return getTelecommuteFrequencyA();
            case 45:
                return getTelecommuteFrequencyB();
            case 46:
                return getTelecommuteFrequencyC();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}