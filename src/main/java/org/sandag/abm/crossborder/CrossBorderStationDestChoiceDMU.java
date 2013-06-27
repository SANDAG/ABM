package org.sandag.abm.crossborder;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;

public class CrossBorderStationDestChoiceDMU        implements Serializable, VariableTable
{

    protected transient Logger                logger                                    = Logger.getLogger("crossBorderModel");

    
    protected HashMap<String, Integer> methodIndexMap;
    protected IndexValues              dmuIndex;

    protected float tourDepartPeriod;
    protected float tourArrivePeriod;
    protected int purpose;
    protected double[][] sizeTerms;       //by purpose, alternative (station-taz or sampled station-mgra)
    protected double[] stationSizeTerms;  //by alternative (station-taz or sampled station-mgra)
    protected double[] correctionFactors; //by alternative (sampled station-mgra pair, for full model only)
    protected double[] tourModeLogsums;   //by alternative (sampled station-mgra pair, for full model only)
    protected int[] poeNumbers;           //by alternative (station-taz or sampled station-mgra)
    protected int[] originTazs;           //by alternative (station-taz or sampled station-mgra)
    protected int[] destinationTazs;      //by alternative (station-taz or sampled station-mgra)
    
    
    protected double                   nmWalkTimeOut;
    protected double                   nmWalkTimeIn;
    protected double                   nmBikeTimeOut;
    protected double                   nmBikeTimeIn;
    protected double                   lsWgtAvgCostM;
    protected double                   lsWgtAvgCostD;
    protected double                   lsWgtAvgCostH;


    public CrossBorderStationDestChoiceDMU(CrossBorderModelStructure modelStructure)
    {
        setupMethodIndexMap();
        dmuIndex = new IndexValues();
        
    }

    /**
     * Get the POE number for the alternative.
     * 
     * @param alt  Either station-taz or sampled station-mgra
     * @return
     */
    public int getPoe(int alt){
    	return poeNumbers[alt];
    }
    
    /**
     * Set the poe number array
     * 
     * @param poeNumbers  An array of POE numbers, one for each alternative (either station-taz or sampled station-mgra)
     */
    public void setPoeNumbers(int[] poeNumbers){
    	this.poeNumbers = poeNumbers;
    }
    /**
     * Get the tour mode choice logsum for the sampled station-mgra pair.
     * 
     * @param alt  Sampled station-mgra
     * @return
     */
    public double getTourModeLogsum(int alt){
    	return tourModeLogsums[alt];
    }
    
    /**
     * Set the tour mode choice logsums
     * 
     * @param poeNumbers  An array of tour mode choice logsums, one for each alternative (sampled station-mgra)
     */
    public void setTourModeLogsums(double[] logsums){
    	this.tourModeLogsums = logsums;
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

    /**
	 * @return the sizeTerms.  The size term is the size of the alternative north of the border.  It is
	 * indexed by alternative, where alternative is either taz-station pair or mgra-station pair, depending
	 * on whether the DMU is being used for the SOA model or the actual model.
	 */
	public double getSizeTerm(int alt) {
		return sizeTerms[purpose][alt];
	}

	/**
	 * @param sizeTerms the sizeTerms to set.  The size term is the size of the alternative north of the border.  It is
	 * indexed by alternative, where alternative is either taz-station pair or mgra-station pair, depending
	 * on whether the DMU is being used for the SOA model or the actual model.
	 */
	public void setSizeTerms(double[][] sizeTerms) {
		this.sizeTerms = sizeTerms;
	}

	   /**
	 * @return the accessibility of the station to population south of the border.  The size term is indexed by alternative, 
	 * where alternative is either taz-station pair or mgra-station pair, depending
	 * on whether the DMU is being used for the SOA model or the actual model.
	 */	 
	public double getStationPopulationAccessibility(int alt) {
		return stationSizeTerms[alt];
	}

	/**
	 * @param accessibilities is the accessibility of the station to population south of the border.  
	 * The size term is indexed by alternative, 
	 * where alternative is either taz-station pair or mgra-station pair, depending
	 * on whether the DMU is being used for the SOA model or the actual model.
	 */
	public void setStationPopulationAccessibilities(double[] accessibilities) {
		this.stationSizeTerms = accessibilities;
	}

	/**
	 * @return the correctionFactors
	 */
	public double getCorrectionFactor(int alt) {
		return correctionFactors[alt];
	}

	/**
	 * @param correctionFactors the correctionFactors to set
	 */
	public void setCorrectionFactors(double[] correctionFactors) {
		this.correctionFactors = correctionFactors;
	}

	/**
	 * @return the origin taz
	 */
	public int getOriginTaz(int alt) {
		return originTazs[alt];
	}

	/**
	 * @param originTazs  The origin tazs to set
	 */
	public void setOriginTazs(int[] originTazs) {
		this.originTazs = originTazs;
	}

	/**
	 * @return the destination taz
	 */
	public int getDestinationTaz(int alt) {
		return destinationTazs[alt];
	}

	/**
	 * @param stopTazs  The destination tazs to set
	 */
	public void setDestinationTazs(int[] destinationTazs) {
		this.destinationTazs = destinationTazs;
	}

	public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    /**
	 * @return the purpose
	 */
	public int getPurpose() {
		return purpose;
	}

	/**
	 * @param purpose the purpose to set
	 */
	public void setPurpose(int purpose) {
		this.purpose = purpose;
	}

	  
    public float getTimeOutbound()
    {
        return tourDepartPeriod;
    }

    public float getTimeInbound()
    {
        return tourArrivePeriod;
    }

    /**
	 * @param tourDepartPeriod the tourDepartPeriod to set
	 */
	public void setTourDepartPeriod(float tourDepartPeriod) {
		this.tourDepartPeriod = tourDepartPeriod;
	}

	/**
	 * @param tourArrivePeriod the tourArrivePeriod to set
	 */
	public void setTourArrivePeriod(float tourArrivePeriod) {
		this.tourArrivePeriod = tourArrivePeriod;
	}

   
    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getTimeOutbound", 0);
        methodIndexMap.put("getTimeInbound", 1);
        methodIndexMap.put("getStationPopulationAccessibility",2);
        methodIndexMap.put("getSizeTerm",3);
        methodIndexMap.put("getCorrectionFactor",4);
        methodIndexMap.put("getPoe",5);
        methodIndexMap.put("getPurpose",6);
        methodIndexMap.put("getTourModeLogsum",7);
        methodIndexMap.put("getOriginTaz",8);
        methodIndexMap.put("getDestinationTaz",9);
         
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        double returnValue = -1;

        switch (variableIndex)
        {

            case 0:
                returnValue = getTimeOutbound();
                break;
            case 1:
                returnValue = getTimeInbound();
                break;
            case 2:
                returnValue = getStationPopulationAccessibility(arrayIndex);
                break;
            case 3:
                returnValue = getSizeTerm(arrayIndex);
                break;
            case 4:
                returnValue = getCorrectionFactor(arrayIndex);
                break;
            case 5:
            	returnValue = getPoe(arrayIndex);
            	break;
            case 6:
            	returnValue = getPurpose();
            	break;
            case 7:
            	returnValue = getTourModeLogsum(arrayIndex);
            	break;
            case 8:
            	returnValue = getOriginTaz(arrayIndex);
            	break;
            case 9:
            	returnValue = getDestinationTaz(arrayIndex);
            	break;
            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

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