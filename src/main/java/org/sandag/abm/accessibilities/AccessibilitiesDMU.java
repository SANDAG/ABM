package org.sandag.abm.accessibilities;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;

public class AccessibilitiesDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(AccessibilitiesDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private double[]                   workSizeTerms;
    private double[]                   schoolSizeTerms;
    private double[]                   sizeTerms;
    // size
    // terms
    // purpose
    // (as
    // defined
    // in
    // uec),
    // for
    // a
    // given
    // mgra

    private double[]                   logsums;                                            // logsums/accessibilities,
    // for
    // a
    // given
    // mgra-pair

    private Household                  hhObject;

    // the alternativeData tabledataset has the following fields
    // sizeTermIndex: Used to index into the sizeTerms array
    // logsumIndex: Used to index into the logsums array
    private TableDataSet               alternativeData;
    private int                        logsumIndex, sizeIndex;

    private int                        autoSufficiency;

    public AccessibilitiesDMU()
    {
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getAutoSufficiency", 0);
        methodIndexMap.put("getSizeTerm", 1);
        methodIndexMap.put("getLogsum", 2);
        methodIndexMap.put("getNumPreschool", 3);
        methodIndexMap.put("getNumGradeSchoolStudents", 4);
        methodIndexMap.put("getNumHighSchoolStudents", 5);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getAutoSufficiency();
            case 1:
                return getSizeTerm(arrayIndex);
            case 2:
                return getLogsum(arrayIndex);
            case 3:
                return getNumPreschool();
            case 4:
                return getNumGradeSchoolStudents();
            case 5:
                return getNumHighSchoolStudents();
            case 6:
                return getWorkSizeTerm(arrayIndex);
            case 7:
                return getSchoolSizeTerm(arrayIndex);

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

    public void setAlternativeData(TableDataSet alternativeData)
    {
        this.alternativeData = alternativeData;
        logsumIndex = alternativeData.getColumnPosition("logsumIndex");
        sizeIndex = alternativeData.getColumnPosition("sizeTermIndex");

    }

    public int getNumPreschool()
    {
        return hhObject.getNumPreschool();
    }

    public int getNumGradeSchoolStudents()
    {
        return hhObject.getNumGradeSchoolStudents();
    }

    public int getNumHighSchoolStudents()
    {
        return hhObject.getNumHighSchoolStudents();
    }

    public int getAutoSufficiency()
    {
        return autoSufficiency;
    }

    public void setHouseholdObject(Household hh)
    {
        hhObject = hh;
    }

    public void setAutoSufficiency(int autoSufficiency)
    {
        this.autoSufficiency = autoSufficiency;
    }

    public void setSizeTerms(double[] sizeTerms)
    {
        this.sizeTerms = sizeTerms;
    }

    public void setWorkSizeTerms(double[] sizeTerms)
    {
        workSizeTerms = sizeTerms;
    }

    public void setSchoolSizeTerms(double[] sizeTerms)
    {
        schoolSizeTerms = sizeTerms;
    }

    public void setLogsums(double[] logsums)
    {
        this.logsums = logsums;
    }

    /**
     * For the given alternative, look up the work size term and return it.
     * 
     * @param alt
     * @return
     */
    public double getWorkSizeTerm(int alt)
    {

        int index = (int) alternativeData.getValueAt(alt, sizeIndex);

        return workSizeTerms[index];
    }

    /**
     * For the given alternative, look up the school size term and return it.
     * 
     * @param alt
     * @return
     */
    public double getSchoolSizeTerm(int alt)
    {

        int index = (int) alternativeData.getValueAt(alt, sizeIndex);

        return schoolSizeTerms[index];
    }

    /**
     * For the given alternative, look up the size term and return it.
     * 
     * @param alt
     * @return
     */
    public double getSizeTerm(int alt)
    {

        int index = (int) alternativeData.getValueAt(alt, sizeIndex);

        return sizeTerms[index];
    }

    /**
     * For the given alternative, look up the size term and return it.
     * 
     * @param alt
     * @return
     */
    public double getLogsum(int alt)
    {

        int index = (int) alternativeData.getValueAt(alt, logsumIndex);

        return logsums[index];
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

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
