package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.DestChoiceTwoStageSoaTazDistanceUtilityDMU;

public class SandagDestChoiceSoaTwoStageTazDistUtilityDMU
        extends DestChoiceTwoStageSoaTazDistanceUtilityDMU
{

    public SandagDestChoiceSoaTwoStageTazDistUtilityDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getLnDestChoiceSizeTazAlt", 0);
        methodIndexMap.put("getSizeTazAlt", 1);
        methodIndexMap.put("getUniversityEnrollmentTazAlt", 2);
        methodIndexMap.put("getGradeSchoolDistrictTazAlt", 3);
        methodIndexMap.put("getHighSchoolDistrictTazAlt", 4);
        methodIndexMap.put("getHomeTazGradeSchoolDistrict", 5);
        methodIndexMap.put("getHomeTazHighSchoolDistrict", 6);
        methodIndexMap.put("getGradeSchoolEnrollmentTazAlt", 7);
        methodIndexMap.put("getHighSchoolEnrollmentTazAlt", 8);
        methodIndexMap.put("getHouseholdsTazAlt", 9);

    }


    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getLnDestChoiceSizeTazAlt(arrayIndex);
            case 1:
                return getSizeTazAlt(arrayIndex);
            case 2:
                return getUniversityEnrollmentTazAlt(arrayIndex);
            case 3:
                return getGradeSchoolDistrictTazAlt(arrayIndex);
            case 4:
                return getHighSchoolDistrictTazAlt(arrayIndex);
            case 5:
                return getHomeTazGradeSchoolDistrict();
            case 6:
                return getHomeTazHighSchoolDistrict();
            case 7:
                return getGradeSchoolEnrollmentTazAlt(arrayIndex);
            case 8:
                return getHighSchoolEnrollmentTazAlt(arrayIndex);
            case 9:
                return getHouseholdsTazAlt(arrayIndex);

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}