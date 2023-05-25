package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class DestChoiceTwoStageSoaTazDistanceUtilityDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger   = Logger.getLogger(DestChoiceTwoStageSoaTazDistanceUtilityDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected IndexValues              dmuIndex = null;

    protected double[]                 dcSize;
    protected double[]                 univEnrollment;
    protected double[]                 gsEnrollment;
    protected double[]                 hsEnrollment;
    protected double[]                 numHhs;
    protected int[]                    gsDistricts;
    protected int[]                    hsDistricts;

    public DestChoiceTwoStageSoaTazDistanceUtilityDMU()
    {
    }

    public void setIndexValuesObject(IndexValues index)
    {
        dmuIndex = index;
    }

    public void setDestChoiceTazSize(double[] size)
    {
        dcSize = size;
    }

    public void setTazUnivEnrollment(double[] enrollment)
    {
        univEnrollment = enrollment;
    }

    public void setTazGsEnrollment(double[] enrollment)
    {
        gsEnrollment = enrollment;
    }

    public void setTazHsEnrollment(double[] enrollment)
    {
        hsEnrollment = enrollment;
    }

    public void setNumHhs(double[] hhs)
    {
        numHhs = hhs;
    }

    public void setTazGsDistricts(int[] districts)
    {
        gsDistricts = districts;
    }

    public void setTazHsDistricts(int[] districts)
    {
        hsDistricts = districts;
    }

    public double getLnDestChoiceSizeTazAlt(int taz)
    {
        return dcSize[taz] == 0 ? -999 : Math.log(dcSize[taz]);
    }

    public double getSizeTazAlt(int taz)
    {
        return dcSize[taz];
    }

    public double getUniversityEnrollmentTazAlt(int taz)
    {
        return univEnrollment[taz];
    }

    public double getGradeSchoolEnrollmentTazAlt(int taz)
    {
        return gsEnrollment[taz];
    }

    public double getHighSchoolEnrollmentTazAlt(int taz)
    {
        return hsEnrollment[taz];
    }

    public double getHouseholdsTazAlt(int taz)
    {
        return numHhs[taz];
    }

    public int getHomeTazGradeSchoolDistrict()
    {
        return gsDistricts[dmuIndex.getZoneIndex()];
    }

    public int getGradeSchoolDistrictTazAlt(int taz)
    {
        return gsDistricts[taz];
    }

    public int getHomeTazHighSchoolDistrict()
    {
        return hsDistricts[dmuIndex.getZoneIndex()];
    }

    public int getHighSchoolDistrictTazAlt(int taz)
    {
        return hsDistricts[taz];
    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
