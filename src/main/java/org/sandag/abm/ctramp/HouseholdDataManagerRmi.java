package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from synthetic population files.
 */
public class HouseholdDataManagerRmi
        implements HouseholdDataManagerIf, Serializable
{

    UtilRmi remote;
    String  connectString;

    public HouseholdDataManagerRmi(String hostname, int port, String className)
    {

        connectString = String.format("//%s:%d/%s", hostname, port, className);
        remote = new UtilRmi(connectString);

    }

    public void setPropertyFileValues(HashMap<String, String> propertyMap)
    {
        Object[] objArray = {propertyMap};
        remote.method("setPropertyFileValues", objArray);
    }

    public void setDebugHhIdsFromHashmap()
    {
        Object[] objArray = {};
        remote.method("setDebugHhIdsFromHashmap", objArray);
    }

    public void setupHouseholdDataManager(ModelStructure modelStructure,
            String inputHouseholdFileName, String inputPersonFileName)
    {
        Object[] objArray = {modelStructure, inputHouseholdFileName, inputPersonFileName};
        remote.method("setupHouseholdDataManager", objArray);
    }

    public void setSchoolDistrictMappings(HashMap<String, Integer> segmentNameIndexMap,
            int[] mgraGsDist, int[] mgraHsDist, HashMap<Integer, Integer> gsDistSegMap,
            HashMap<Integer, Integer> hsDistSegMap)
    {
        Object[] objArray = {segmentNameIndexMap, mgraGsDist, mgraHsDist, gsDistSegMap,
                hsDistSegMap};
        remote.method("setSchoolDistrictMappings", objArray);
    }

    public void computeTransponderChoiceTazPercentArrays()
    {
        Object[] objArray = {};
        remote.method("computeTransponderChoiceTazPercentArrays", objArray);
    }

    public double[] getPercentHhsIncome100Kplus()
    {
        Object[] objArray = {};
        return (double[]) remote.method("getPercentHhsIncome100Kplus", objArray);
    }

    public double[] getPercentHhsMultipleAutos()
    {
        Object[] objArray = {};
        return (double[]) remote.method("getPercentHhsMultipleAutos", objArray);
    }

    public void logPersonSummary()
    {
        Object[] objArray = {};
        remote.method("logPersonSummary", objArray);
    }

    public int getArrayIndex(int hhId)
    {
        Object[] objArray = {hhId};
        return (Integer) remote.method("getArrayIndex", objArray);
    }

    public int[] getWorksAtHomeBySegment(HashMap<Integer, Integer> segmentValueIndexMap)
    {
        Object[] objArray = {segmentValueIndexMap};
        return (int[]) remote.method("getWorksAtHomeBySegment", objArray);
    }

    public int[][] getWorkToursByDestMgra(HashMap<Integer, Integer> segmentValueIndexMap)
    {
        Object[] objArray = {segmentValueIndexMap};
        return (int[][]) remote.method("getWorkToursByDestMgra", objArray);
    }

    public int[][] getSchoolToursByDestMgra()
    {
        Object[] objArray = {};
        return (int[][]) remote.method("getSchoolToursByDestMgra", objArray);
    }

    public int[][] getWorkersByHomeMgra(HashMap<Integer, Integer> segmentValueIndexMap)
    {
        Object[] objArray = {segmentValueIndexMap};
        return (int[][]) remote.method("getWorkersByHomeMgra", objArray);
    }

    public int[][] getStudentsByHomeMgra()
    {
        Object[] objArray = {};
        return (int[][]) remote.method("getStudentsByHomeMgra", objArray);
    }

    public int[][] getTourPurposePersonsByHomeMgra(String[] purposeList)
    {
        Object[] objArray = {purposeList};
        return (int[][]) remote.method("getTourPurposePersonsByHomeMgra", objArray);
    }

    public int[] getIndividualNonMandatoryToursByHomeMgra(String purposeString)
    {
        Object[] objArray = {purposeString};
        return (int[]) remote.method("getIndividualNonMandatoryToursByHomeMgra", objArray);
    }

    public int[] getJointToursByHomeMgra(String purposeString)
    {
        Object[] objArray = {purposeString};
        return (int[]) remote.method("getJointToursByHomeMgra", objArray);
    }

    public int[] getAtWorkSubtoursByWorkMgra(String purposeString)
    {
        Object[] objArray = {purposeString};
        return (int[]) remote.method("getAtWorkSubtoursByWorkMgra", objArray);
    }

    public String testRemote()
    {
        Object[] objArray = {};
        return (String) remote.method("testRemote", objArray);
    }

    public void mapTablesToHouseholdObjects()
    {
        Object[] objArray = {};
        remote.method("mapTablesToHouseholdObjects", objArray);
    }

    public void writeResultData()
    {
        Object[] objArray = {};
        remote.method("writeResultData", objArray);
    }

    public int[] getRandomOrderHhIndexArray(int numHhs)
    {
        Object[] objArray = {numHhs};
        return (int[]) remote.method("getRandomOrderHhIndexArray", objArray);
    }

    /**
     * set the hh id for which debugging info from choice models applied to this household will be logged if debug logging.
     */
    public void setDebugHouseholdId(int debugHhId, boolean value)
    {
        Object[] objArray = {debugHhId, value};
        remote.method("setDebugHouseholdId", objArray);
    }

    /**
     * Sets the HashSet used to trace households for debug purposes and sets the debug switch for each of the listed households. Also sets
     */
    public void setTraceHouseholdSet()
    {
        Object[] objArray = {};
        remote.method("setTraceHouseholdSet", objArray);
    }

    public void setHouseholdSampleRate(float sampleRate, int sampleSeed)
    {
        Object[] objArray = {sampleRate, sampleSeed};
        remote.method("setHouseholdSampleRate", objArray);
    }

    public void resetUwslRandom(int iter)
    {
        Object[] objArray = {iter};
        remote.method("resetUwslRandom", objArray);
    }

    public void resetPreAoRandom()
    {
        Object[] objArray = {};
        remote.method("resetPreAoRandom", objArray);
    }

    public void setUwslRandomCount(int iter)
    {
        Object[] objArray = {iter};
        remote.method("setUwslRandomCount", objArray);
    }

    public void resetAoRandom(int iter)
    {
        Object[] objArray = {iter};
        remote.method("resetAoRandom", objArray);
    }

    public void resetFpRandom()
    {
        Object[] objArray = {};
        remote.method("resetFpRandom", objArray);
    }

    public void resetCdapRandom()
    {
        Object[] objArray = {};
        remote.method("resetCdapRandom", objArray);
    }

    public void resetImtfRandom()
    {
        Object[] objArray = {};
        remote.method("resetImtfRandom", objArray);
    }

    public void resetImtodRandom()
    {
        Object[] objArray = {};
        remote.method("resetImtodRandom", objArray);
    }

    public void resetAwfRandom()
    {
        Object[] objArray = {};
        remote.method("resetAwfRandom", objArray);
    }

    public void resetAwlRandom()
    {
        Object[] objArray = {};
        remote.method("resetAwlRandom", objArray);
    }

    public void resetAwtodRandom()
    {
        Object[] objArray = {};
        remote.method("resetAwtodRandom", objArray);
    }

    public void resetJtfRandom()
    {
        Object[] objArray = {};
        remote.method("resetJtfRandom", objArray);
    }

    public void resetJtlRandom()
    {
        Object[] objArray = {};
        remote.method("resetJtlRandom", objArray);
    }

    public void resetJtodRandom()
    {
        Object[] objArray = {};
        remote.method("resetJtodRandom", objArray);
    }

    public void resetInmtfRandom()
    {
        Object[] objArray = {};
        remote.method("resetInmtfRandom", objArray);
    }

    public void resetInmtlRandom()
    {
        Object[] objArray = {};
        remote.method("resetInmtlRandom", objArray);
    }

    public void resetInmtodRandom()
    {
        Object[] objArray = {};
        remote.method("resetInmtodRandom", objArray);
    }

    public void resetStfRandom()
    {
        Object[] objArray = {};
        remote.method("resetStfRandom", objArray);
    }

    public void resetStlRandom()
    {
        Object[] objArray = {};
        remote.method("resetStlRandom", objArray);
    }

    /**
     * return the array of Household objects holding the synthetic population and choice model outcomes.
     * 
     * @return hhs
     */
    public Household[] getHhArray()
    {
        Object[] objArray = {};
        return (Household[]) remote.method("getHhArray", objArray);
    }

    public Household[] getHhArray(int first, int last)
    {
        Object[] objArray = {first, last};
        return (Household[]) remote.method("getHhArray", objArray);
    }

    public void setHhArray(Household[] hhs)
    {
        Object[] objArray = {hhs};
        remote.method("setHhArray", objArray);
    }

    public void setHhArray(Household[] tempHhs, int startIndex)
    {
        Object[] objArray = {tempHhs, startIndex};
        remote.method("setHhArray", objArray);
    }

    /**
     * return the array of Household objects holding the synthetic population and choice model outcomes.
     * 
     * @return hhs
     */
    public int[] getHhIndexArray()
    {
        Object[] objArray = {};
        return (int[]) remote.method("getHhIndexArray", objArray);
    }

    /**
     * return the number of household objects read from the synthetic population.
     * 
     * @return number of households in synthetic population
     */
    public int getNumHouseholds()
    {
        Object[] objArray = {};
        return (Integer) remote.method("getNumHouseholds", objArray);
    }

    /**
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the origin for this tour
     */
    public int getInitialOriginWalkSegment(int taz, double randomNumber)
    {
        Object[] objArray = {taz, randomNumber};
        return (Integer) remote.method("getInitialOriginWalkSegment", objArray);
    }

    public long getBytesUsedByHouseholdArray()
    {
        Object[] objArray = {};
        return (Long) remote.method("getBytesUsedByHouseholdArray", objArray);
    }

}