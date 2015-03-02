package org.sandag.abm.ctramp;

import java.util.HashMap;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from
 *         synthetic population files.
 */
public interface HouseholdDataManagerIf
{

    String testRemote();

    void setPropertyFileValues(HashMap<String, String> propertyMap);

    void setDebugHhIdsFromHashmap();

    void computeTransponderChoiceTazPercentArrays();

    double[] getPercentHhsIncome100Kplus();

    double[] getPercentHhsMultipleAutos();

    int[] getRandomOrderHhIndexArray(int numHhs);

    int getArrayIndex(int hhId);

    void setHhArray(Household[] hhs);

    void setHhArray(Household[] tempHhs, int startIndex);

    void setSchoolDistrictMappings(HashMap<String, Integer> segmentNameIndexMap, int[] mgraGsDist,
            int[] mgraHsDist, HashMap<Integer, Integer> gsDistSegMap,
            HashMap<Integer, Integer> hsDistSegMap);

    void setupHouseholdDataManager(ModelStructure modelStructure, String inputHouseholdFileName,
            String inputPersonFileName);

    int[][] getTourPurposePersonsByHomeMgra(String[] purposeList);

    int[][] getWorkersByHomeMgra(HashMap<Integer, Integer> segmentValueIndexMap);

    int[][] getStudentsByHomeMgra();

    int[][] getWorkToursByDestMgra(HashMap<Integer, Integer> segmentValueIndexMap);

    int[] getWorksAtHomeBySegment(HashMap<Integer, Integer> segmentValueIndexMap);

    int[][] getSchoolToursByDestMgra();

    int[] getIndividualNonMandatoryToursByHomeMgra(String purposeString);

    int[] getJointToursByHomeMgra(String purposeString);

    int[] getAtWorkSubtoursByWorkMgra(String purposeString);

    void logPersonSummary();

    void setUwslRandomCount(int iter);

    void resetUwslRandom(int iter);

    void resetPreAoRandom();

    void resetAoRandom(int iter);

    void resetFpRandom();

    void resetCdapRandom();

    void resetImtfRandom();

    void resetImtodRandom();

    void resetAwfRandom();

    void resetAwlRandom();

    void resetAwtodRandom();

    void resetJtfRandom();

    void resetJtlRandom();

    void resetJtodRandom();

    void resetInmtfRandom();

    void resetInmtlRandom();

    void resetInmtodRandom();
    
    void resetTdRandom();

    void resetStfRandom();

    void resetStlRandom();

    /**
     * Sets the HashSet used to trace households for debug purposes and sets the
     * debug switch for each of the listed households. Also sets
     */
    void setTraceHouseholdSet();

    /**
     * Sets the HashSet used to trace households for debug purposes and sets the
     * debug switch for each of the listed households. Also sets
     */
    void setHouseholdSampleRate(float sampleRate, int sampleSeed);

    /**
     * return the array of Household objects holding the synthetic population
     * and choice model outcomes.
     * 
     * @return hhs
     */
    Household[] getHhArray();

    Household[] getHhArray(int firstHhIndex, int lastHhIndex);

    /**
     * return the number of household objects read from the synthetic
     * population.
     * 
     * @return
     */
    int getNumHouseholds();

    /**
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the
     * origin for this tour
     */
    int getInitialOriginWalkSegment(int taz, double randomNumber);

    long getBytesUsedByHouseholdArray();

}