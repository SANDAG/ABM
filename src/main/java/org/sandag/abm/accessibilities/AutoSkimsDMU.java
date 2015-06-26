package org.sandag.abm.accessibilities;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

public class AutoSkimsDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(AutoSkimsDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected float                      vot;

    public AutoSkimsDMU()
    {
        setupMethodIndexMap();
    }

    public float getVOT()
    {
        return vot;
    }

    public void setVOT(float vot)
    {
        this.vot = vot;
    }

 

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getVOT", 0);
      }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getVOT();
            
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
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
