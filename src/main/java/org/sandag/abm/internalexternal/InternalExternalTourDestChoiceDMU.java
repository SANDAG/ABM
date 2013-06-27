package org.sandag.abm.internalexternal;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;

public class InternalExternalTourDestChoiceDMU        implements Serializable, VariableTable
{

    protected transient Logger                logger                                    = Logger.getLogger("internalExternalModel");

    
    protected HashMap<String, Integer> methodIndexMap;
    protected IndexValues              dmuIndex;
    
    
    public InternalExternalTourDestChoiceDMU(InternalExternalModelStructure modelStructure)
    {
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
        
    }

 
   /**
     * Set this index values for this tour mode choice DMU object.
     * 
     * @param hhIndex is the DMU household index
     * @param zoneIndex is the DMU zone index
     * @param origIndex is the DMU origin index
     * @param destIndex is the DMU desatination index
     */
    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex, boolean debug)
    {
        dmuIndex.setHHIndex(hhIndex);
        dmuIndex.setZoneIndex(zoneIndex);
        dmuIndex.setOriginZone(origIndex);
        dmuIndex.setDestZone(destIndex);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (debug)
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug MC UEC");
        }

    }


	public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }


   
    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

//        methodIndexMap.put("getTimeOutbound", 0);
         
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;
        
        /*

        switch (variableIndex)
        {

            case 0:
                returnValue = getTimeOutbound();
                break;
           
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

         */
        return returnValue;

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