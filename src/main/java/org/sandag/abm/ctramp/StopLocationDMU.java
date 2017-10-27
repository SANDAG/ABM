package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

/**
 * This class is used for ...
 * 
 * @author Christi Willison
 * @version Nov 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class StopLocationDMU
        implements Serializable, VariableTable
{

    protected HashMap<String, Integer> methodIndexMap;

    protected IndexValues              dmuIndex;
    protected Household                household;
    protected Person                   person;
    protected Tour                     tour;
    protected Stop                     stop;
    protected ModelStructure           modelStructure;

    protected int                      numberInSample;
    protected int                      tourModeIndex;
    protected double                   origDestDistance;

    // these arrays are dimensioned to the total number of location choice
    // alternatives (number of MGRAs)
    protected int[]                    walkTransitAvailableAtMgra;
    protected double[]                 distancesFromOrigMgra;
    protected double[]                 distancesFromTourOrigMgra;
    protected double[]                 distancesToDestMgra;
    protected double[]                 distancesToTourDestMgra;
    protected double[]                 logSizeTerms;
    
    protected double[]                 bikeLogsumsFromOrigMgra;
    protected double[]                 bikeLogsumsToDestMgra;



    // these arrays are dimensioned to the maximum number of alternatives in the
    // sample
    protected double[]                 mcLogsums;
    protected double[]                 slcSoaCorrections;
    protected int[]                    sampleArray;

    public StopLocationDMU(ModelStructure modelStructure)
    {
        dmuIndex = new IndexValues();

        this.modelStructure = modelStructure;
    }

    public void setDmuIndexValues(int hhid, int homeTaz, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(hhid);
        dmuIndex.setZoneIndex(homeTaz);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (household.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug SL UEC");
        }

    }

    public void setStopObject(Stop myStop)
    {
        stop = myStop;
        tour = stop.getTour();
        person = tour.getPersonObject();
        household = person.getHouseholdObject();
    }

    /**
     * set the value for the number of unique alternatives in the sample.
     * sampleArray can be indexed as i=1; i <= numberInSample.
     * sampleArray.length - 1 is the maximum number of locations in the sample.
     * 
     * @param num
     *            - number of unique alternatives in the sample.
     */
    public void setNumberInSample(int num)
    {
        numberInSample = num;
    }

    /**
     * set the array of sample MGRA values from which the stop location MGRA
     * will be selected.
     * 
     * @param sample
     *            - the sample array of MGRA location choice alternatives. - use
     *            numberInSample as upperbound of relevant choices in sample
     */
    public void setSampleOfAlternatives(int[] sample)
    {
        sampleArray = sample;
    }

    public void setSlcSoaCorrections(double[] corrections)
    {
        slcSoaCorrections = corrections;
    }

    public void setMcLogsums(double[] logsums)
    {
        mcLogsums = logsums;
    }

    public void setLogSize(double[] size)
    {
        logSizeTerms = size;
    }

    /**
     * set the array of distance values from the origin MGRA of the stop to all
     * MGRAs.
     * 
     * @param distances
     */
    public void setDistancesFromOrigMgra(double[] distances)
    {
        distancesFromOrigMgra = distances;
    }

    /**
     * set the array of distance values from the tour origin MGRA to all MGRAs.
     * 
     * @param distances
     */
    public void setDistancesFromTourOrigMgra(double[] distances)
    {
        distancesFromTourOrigMgra = distances;
    }

    /**
     * set the array of distance values from all MGRAs to the final destination
     * MGRA of the stop.
     * 
     * @param distances
     */
    public void setDistancesToDestMgra(double[] distances)
    {
        distancesToDestMgra = distances;
    }

    /**
     * set the array of distance values from all MGRAs to the tour destination
     * MGRA.
     * 
     * @param distances
     */
    public void setDistancesToTourDestMgra(double[] distances)
    {
        distancesToTourDestMgra = distances;
    }

    /**
	 * @param bikeLogsumsFromOrigMgra the bikeLogsumsFromOrigMgra to set
	 */
	public void setBikeLogsumsFromOrigMgra(double[] bikeLogsumsFromOrigMgra) {
		this.bikeLogsumsFromOrigMgra = bikeLogsumsFromOrigMgra;
	}

	/**
	 * @param bikeLogsumsToDestMgra the bikeLogsumsToDestMgra to set
	 */
	public void setBikeLogsumsToDestMgra(double[] bikeLogsumsToDestMgra) {
		this.bikeLogsumsToDestMgra = bikeLogsumsToDestMgra;
	}

	/**
     * set the OD distance value from the stop origin MGRA to the final
     * destination MGRA of the stop.
     * 
     * @param distances
     */
    public void setOrigDestDistance(double distance)
    {
        origDestDistance = distance;
    }

    /**
     * set the tour mode index value for the tour of the stop being located
     * 
     * @param tour
     */
    public void setTourModeIndex(int index)
    {
        tourModeIndex = index;
    }

    /**
     * set the array of attributes for all MGRAs that says their is walk transit
     * access for the indexed mgra
     * 
     * @param tour
     */
    public void setWalkTransitAvailable(int[] avail)
    {
        walkTransitAvailableAtMgra = avail;
    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public int getSampleMgraAlt(int alt)
    {
        return sampleArray[alt];
    }

    public double getSlcSoaCorrectionsAlt(int alt)
    {
        return slcSoaCorrections[alt];
    }

    public double getMcLogsumAlt(int alt)
    {
        return mcLogsums[alt];
    }

    /**
     * get the logged size term from the full set of size terms for all mgra
     * associated with the sample alternative
     * 
     * @param alt
     *            - element number for the sample array
     * @return logged size term for mgra associated with the sample element
     */
    public double getLnSlcSizeSampleAlt(int alt)
    {
        int mgra = sampleArray[alt];
        return logSizeTerms[mgra];
    }

    /**
     * get the logged size term ffrom the full set of size terms for all mgra
     * alternatives
     * 
     * @param mgra
     *            - mgra location alternive
     * @return logged size term for mgra
     */
    public double getLnSlcSizeAlt(int mgra)
    {
        return logSizeTerms[mgra];
    }

    protected int getTourIsJoint()
    {
        return tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ? 1
                : 0;
    }

    public int getTourMode()
    {
        return tour.getTourModeChoice();
    }

    public int getTourPurpose()
    {
        return tour.getTourPrimaryPurposeIndex();
    }

    public int getFemale()
    {
        return person.getPersonIsFemale();
    }

    public int getIncomeInDollars()
    {
        return household.getIncomeInDollars();
    }

    public int getAge()
    {
        return person.getAge();
    }

    public int getStopPurpose()
    {
        return stop.getStopPurposeIndex();
    }

    public int getStopNumber()
    {
        return (stop.getStopId() + 1);
    }

    public int getInboundStop()
    {
        return stop.isInboundStop() ? 1 : 0;
    }

    public int getStopsOnHalfTour()
    {
        return stop.isInboundStop() ? tour.getInboundStops().length
                : tour.getOutboundStops().length;
    }

    public double getOrigToMgraDistanceAlt(int alt)
    {
        // int dummy=0;
        // double dist = Math.abs(distancesFromOrigMgra[alt] -
        // distancesToDestMgra[alt]);
        // double maxSegDist = Math.max(distancesFromOrigMgra[alt],
        // distancesToDestMgra[alt]);
        // if ( dist > 0 && dist < 1 && origDestDistance > 40 )
        // dummy = 1;

        return distancesFromOrigMgra[alt];
    }

    public double getTourOrigToMgraDistanceAlt(int alt)
    {
        return distancesFromTourOrigMgra[alt];
    }

    public double getMgraToDestDistanceAlt(int alt)
    {
        return distancesToDestMgra[alt];
    }

    public double getMgraToTourDestDistanceAlt(int alt)
    {
        return distancesToTourDestMgra[alt];
    }

    public double getOrigToMgraBikeLogsumAlt(int alt)
    {
    	return bikeLogsumsFromOrigMgra[alt];
    }

    public double getMgraToDestBikeLogsumAlt(int alt)
    {
    	return bikeLogsumsToDestMgra[alt];
    }

	
	
    public double getOdDistance()
    {
        return origDestDistance;
    }

    public int getTourModeIsWalk()
    {
        boolean tourModeIsWalk = modelStructure.getTourModeIsWalk(tourModeIndex);
        return tourModeIsWalk ? 1 : 0;
    }

    public int getTourModeIsBike()
    {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike(tourModeIndex);
        return tourModeIsBike ? 1 : 0;
    }

    public int getTourModeIsWalkTransit()
    {
        return (modelStructure.getTourModeIsWalkTransit(tourModeIndex) ? 1 : 0);
    }

    public int getWalkTransitAvailableAlt(int alt)
    {
        return walkTransitAvailableAtMgra[alt];
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