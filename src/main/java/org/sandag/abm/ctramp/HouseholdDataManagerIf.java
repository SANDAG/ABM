package org.sandag.abm.ctramp;

import java.util.HashMap;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from
 *         synthetic population files.
 */
public interface HouseholdDataManagerIf {

	public String testRemote();

	public void setPropertyFileValues(HashMap<String, String> propertyMap);

	public void setDebugHhIdsFromHashmap();

	public void computeTransponderChoiceTazPercentArrays();

	public double[] getPercentHhsIncome100Kplus();

	public double[] getPercentHhsMultipleAutos();

	public int[] getRandomOrderHhIndexArray(int numHhs);

	public int getArrayIndex(int hhId);

	public void setHhArray(Household[] hhs);

	public void setHhArray(Household[] tempHhs, int startIndex);

	public void setSchoolDistrictMappings(
			HashMap<String, Integer> segmentNameIndexMap, int[] mgraGsDist,
			int[] mgraHsDist, HashMap<Integer, Integer> gsDistSegMap,
			HashMap<Integer, Integer> hsDistSegMap);

	public void setupHouseholdDataManager(ModelStructure modelStructure,
			String inputHouseholdFileName, String inputPersonFileName);

	public int[][] getTourPurposePersonsByHomeMgra(String[] purposeList);

	public int[][] getWorkersByHomeMgra(
			HashMap<Integer, Integer> segmentValueIndexMap);

	public int[][] getStudentsByHomeMgra();

	public int[][] getWorkToursByDestMgra(
			HashMap<Integer, Integer> segmentValueIndexMap);

	public int[] getWorksAtHomeBySegment(
			HashMap<Integer, Integer> segmentValueIndexMap);

	public int[][] getSchoolToursByDestMgra();

	public int[] getIndividualNonMandatoryToursByHomeMgra(String purposeString);

	public int[] getJointToursByHomeMgra(String purposeString);

	public int[] getAtWorkSubtoursByWorkMgra(String purposeString);

	public void logPersonSummary();

	public void setUwslRandomCount(int iter);

	public void resetUwslRandom(int iter);

	public void resetPreAoRandom();

	public void resetAoRandom(int iter);

	public void resetFpRandom();

	public void resetCdapRandom();

	public void resetImtfRandom();

	public void resetImtodRandom();

	public void resetAwfRandom();

	public void resetAwlRandom();

	public void resetAwtodRandom();

	public void resetJtfRandom();

	public void resetJtlRandom();

	public void resetJtodRandom();

	public void resetInmtfRandom();

	public void resetInmtlRandom();

	public void resetInmtodRandom();

	public void resetStfRandom();

	public void resetStlRandom();

	/**
	 * Sets the HashSet used to trace households for debug purposes and sets the
	 * debug switch for each of the listed households. Also sets
	 */
	public void setTraceHouseholdSet();

	/**
	 * Sets the HashSet used to trace households for debug purposes and sets the
	 * debug switch for each of the listed households. Also sets
	 */
	public void setHouseholdSampleRate(float sampleRate, int sampleSeed);

	/**
	 * return the array of Household objects holding the synthetic population
	 * and choice model outcomes.
	 * 
	 * @return hhs
	 */
	public Household[] getHhArray();

	public Household[] getHhArray(int firstHhIndex, int lastHhIndex);

	/**
	 * return the number of household objects read from the synthetic
	 * population.
	 * 
	 * @return
	 */
	public int getNumHouseholds();

	/**
	 * set walk segment (0-none, 1-short, 2-long walk to transit access) for the
	 * origin for this tour
	 */
	public int getInitialOriginWalkSegment(int taz, double randomNumber);

	public long getBytesUsedByHouseholdArray();

}