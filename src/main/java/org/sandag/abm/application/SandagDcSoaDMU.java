package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.DcSoaDMU;

public class SandagDcSoaDMU
        extends DcSoaDMU
{

    public SandagDcSoaDMU()
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getLnDcSizeAlt", 0);
        methodIndexMap.put("getOriginToMgraDistanceAlt", 1);
        methodIndexMap.put("getTourPurposeIsEscort", 2);
        methodIndexMap.put("getNumPreschool", 3);
        methodIndexMap.put("getNumGradeSchoolStudents", 4);
        methodIndexMap.put("getNumHighSchoolStudents", 5);
        methodIndexMap.put("getDcSizeAlt", 6);
        methodIndexMap.put("getHouseholdsDestAlt", 8);
        methodIndexMap.put("getGradeSchoolEnrollmentDestAlt", 9);
        methodIndexMap.put("getHighSchoolEnrollmentDestAlt", 10);
        methodIndexMap.put("getGradeSchoolDistrictDestAlt", 11);
        methodIndexMap.put("getHomeMgraGradeSchoolDistrict", 12);
        methodIndexMap.put("getHighSchoolDistrictDestAlt", 14);
        methodIndexMap.put("getHomeMgraHighSchoolDistrict", 15);
        methodIndexMap.put("getUniversityEnrollmentDestAlt", 16);

    }

    // DMU methods - define one of these for every @var in the mode choice
    // control
    // file.
    public double getLnDcSizeAlt(int alt)
    {
        return getLnDcSize(alt);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getLnDcSizeAlt(arrayIndex);
            case 1:
                return getOriginToMgraDistanceAlt(arrayIndex);
            case 2:
                return getTourPurposeIsEscort();
            case 3:
                return getNumPreschool();
            case 4:
                return getNumGradeSchoolStudents();
            case 5:
                return getNumHighSchoolStudents();
            case 6:
                return getDcSizeAlt(arrayIndex);
            case 8:
                return getHouseholdsDestAlt(arrayIndex);
            case 9:
                return getGradeSchoolEnrollmentDestAlt(arrayIndex);
            case 10:
                return getHighSchoolEnrollmentDestAlt(arrayIndex);
            case 11:
                return getGradeSchoolDistrictDestAlt(arrayIndex);
            case 12:
                return getHomeMgraGradeSchoolDistrict();
            case 14:
                return getHighSchoolDistrictDestAlt(arrayIndex);
            case 15:
                return getHomeMgraHighSchoolDistrict();
            case 16:
                return getUniversityEnrollmentDestAlt(arrayIndex);
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}