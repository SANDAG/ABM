package org.sandag.abm.maas;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.pb.common.calculator.VariableTable;

/**
 * This class is for resimulating transit path choice. it is used 
 * as the DMU in the selection UEC.
 * 
 * @author Joel Freedman
 * @version August 27, 2018
 */
public class ResimulateTransitPathDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(ResimulateTransitPathDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    private int originMaz;
    private int destinationMaz;
    private int boardingTap;
    private int alightingTap;
    private int set;
    private int tod;

    public ResimulateTransitPathDMU()
    {
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getOriginMaz", 0);
        methodIndexMap.put("getDestinationMaz", 1);
        methodIndexMap.put("getBoardingTap", 2);
        methodIndexMap.put("getAlightingTap", 3);
        methodIndexMap.put("getSet", 4);
        methodIndexMap.put("getTOD", 5);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getOriginMaz();
            case 1:
                return getDestinationMaz();
            case 2:
                return getBoardingTap();
            case 3:
                return getAlightingTap();
            case 4:
                return getSet();
            case 5:
                return getTOD();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
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

	public int getOriginMaz() {
		return originMaz;
	}

	public void setOriginMaz(int originMaz) {
		this.originMaz = originMaz;
	}

	public int getDestinationMaz() {
		return destinationMaz;
	}

	public void setDestinationMaz(int destinationMaz) {
		this.destinationMaz = destinationMaz;
	}

	public int getBoardingTap() {
		return boardingTap;
	}

	public void setBoardingTap(int boardingTap) {
		this.boardingTap = boardingTap;
	}

	public int getAlightingTap() {
		return alightingTap;
	}

	public void setAlightingTap(int alightingTap) {
		this.alightingTap = alightingTap;
	}

	public int getSet() {
		return set;
	}

	public void setSet(int set) {
		this.set = set;
	}

	public int getTOD() {
		return tod;
	}

	public void setTOD(int tod) {
		this.tod = tod;
	}

}
