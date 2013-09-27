package org.sandag.abm.ctramp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

public class DestChoiceTwoStageModel {

	private transient Logger logger = Logger
			.getLogger(DestChoiceTwoStageModel.class);

	// dimensioned for maxMgra, holds the number of times a specific mgra was
	// selected to be in the the sample
	private int[] mgraSampleFreq;

	// these arrays are dimesnioned to the maxSampleSize and hold values mgra
	// selected for the sample.
	private int[] sampleMgras;
	private double[] sampleProbabilities;

	// these arrays are dimesnioned to the maxSampleSize, but hold values up to
	// the number of unique mgras selected for the sample.
	// array values after the number of unique selected mgras are default array
	// values.
	private int[] uniqueMgraSample;
	private double[] uniqueCorrectionFactors;

	// this array holds the sampleIndex associated with a unique mgra, and is
	// used to lookup the sample probability for the unique mgra.
	private int[] uniqueSampleIndices;

	// use this variable to keep track of the number of unique sampled mgras
	// while choosing the sample
	private int uniqueIndex;

	private TazDataManager tdm;
	private MgraDataManager mgraManager;

	// for each purpose index, the 2D array is 0-based on origTaz and is 0-based
	// on destTaz, giving cumulative taz distance probabilities.
	private double[][][] tazDistCumProbs;

	// for each purpose index, the 2D array is 0-based on TAZ and is 0-based on
	// MGRAs in the taz, giving size probabilities for MGRAs in the TAZ.
	private double[][][] mgraSizeProbs;

	private double[][][] slcSizeProbs;
	private double[][] slcTazSize;
	private double[][] slcTazDistExpUtils;

	// create an array to re-use to hold cumulative probabilities for selecting
	// an MGRA from a TAZ.
	private double[] tempMgraCumProbs = new double[200];

	private double[] slcTazProbs;
	private double[] slcTazCumProbs;
	private int maxTaz;

	private long soaRunTime;

	public DestChoiceTwoStageModel(HashMap<String, String> propertyMap,
			int soaMaxSampleSize) {

		mgraManager = MgraDataManager.getInstance(propertyMap);
		int maxMgra = mgraManager.getMaxMgra();

		tdm = TazDataManager.getInstance(propertyMap);
		maxTaz = tdm.getMaxTaz();

		slcTazProbs = new double[maxTaz];
		slcTazCumProbs = new double[maxTaz];

		mgraSampleFreq = new int[maxMgra + 1];

		sampleMgras = new int[soaMaxSampleSize];
		sampleProbabilities = new double[soaMaxSampleSize];

		uniqueMgraSample = new int[soaMaxSampleSize];
		uniqueCorrectionFactors = new double[soaMaxSampleSize];

		uniqueSampleIndices = new int[soaMaxSampleSize];
	}

	private void resetSampleArrays() {
		Arrays.fill(mgraSampleFreq, 0);
		Arrays.fill(sampleMgras, 0);
		Arrays.fill(sampleProbabilities, 0);
		Arrays.fill(uniqueMgraSample, 0);
		Arrays.fill(uniqueSampleIndices, -1);
		Arrays.fill(uniqueCorrectionFactors, 0);
		uniqueIndex = 0;
	}

	/**
	 * get the array of unique mgras selected in the sample. The number of
	 * unique mgras may be fewer than the number selected for the sample - the
	 * overall sample size. If so, values in this array from
	 * 0,...,numUniqueMgras-1 will be the selected unique mgras, and values from
	 * numUniqueMgras,...,maxSampleSize-1 will be 0.
	 * 
	 * @return uniqueMgraSample array.
	 */
	public int[] getUniqueSampleMgras() {
		return uniqueMgraSample;
	}

	/**
	 * get the number of unique mgra values in the sample. It gives the
	 * upperbound of unique values in uniqueMgraSample[0,...,numUniqueMgras-1].
	 * 
	 * @return number of unique mgra values in the sample
	 */
	public int getNumberofUniqueMgrasInSample() {
		return uniqueIndex;
	}

	public double[] getUniqueSampleMgraCorrectionFactors() {

		for (int i = 0; i < uniqueIndex; i++) {
			int chosenMgra = uniqueMgraSample[i];
			int freq = mgraSampleFreq[chosenMgra];

			int sampleIndex = uniqueSampleIndices[i];
			double prob = sampleProbabilities[sampleIndex];

			uniqueCorrectionFactors[i] = (float) Math.log((double) freq / prob);
		}

		return uniqueCorrectionFactors;
	}

	public void computeSoaProbabilities(int origTaz, int segmentTypeIndex) {

		double[][] sizeProbs = mgraSizeProbs[segmentTypeIndex];
		double[] probs = new double[mgraManager.getMaxMgra() + 1];

		for (int taz = 1; taz <= tdm.getMaxTaz(); taz++) {

			int[] mgraArray = tdm.getMgraArray(taz);
			if (mgraArray == null)
				continue;

			if (sizeProbs[taz - 1].length == 0)
				continue;

			double tazProb = 0;
			if (taz > 1)
				tazProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][taz - 1]
						- tazDistCumProbs[segmentTypeIndex][origTaz - 1][taz - 2];
			else
				tazProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][0];

			for (int mgraIndex = 0; mgraIndex < mgraArray.length; mgraIndex++) {
				double mgraProb = sizeProbs[taz - 1][mgraIndex];
				probs[mgraArray[mgraIndex]] = tazProb * mgraProb;
			}

		}

		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(new File(
					"distSoaProbs.csv"))));

			for (int i = 1; i < probs.length; i++) {
				out.println(i + "," + probs[i]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out.close();

	}

	public void chooseSampleMgra(int sampleIndex, int origTaz,
			int sizePurposeIndex, int segmentTypeIndex, double rn, boolean debug) {

		// get the chosen TAZ array index for the 0-based cumulative TAZ
		// distance probabilities array
		int chosenTazIndex = Util.binarySearchDouble(
				tazDistCumProbs[segmentTypeIndex][origTaz - 1], rn);

		if (mgraSizeProbs[segmentTypeIndex][chosenTazIndex].length == 0) {
			logger.error("The MGRA size probabilities array for chosen TAZ index = "
					+ chosenTazIndex + " has 0 length.");
			logger.error("This should not be the case.  If a TAZ was chosen, its TAZ Size > 0, so there should be at least one MGRA with size > 0 in the TAZ.");
			logger.error("Likely cause is an indexing bug.  sampleIndex="
					+ sampleIndex + ", origTaz=" + origTaz
					+ ", sizePurposeIndex=" + sizePurposeIndex
					+ ", segmentTypeIndex=" + segmentTypeIndex);
			throw new RuntimeException();
		}

		// get the chosen TAZ distance probability from the taz distance
		// cumulative probabilities array
		// also initialize the 0 index cumulative MGRA probability to the
		// cumulative taz distance propbaility
		double tazProb = 0;
		double cumProbabilityLowerBound = 0;
		if (chosenTazIndex > 0) {
			tazProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][chosenTazIndex]
					- tazDistCumProbs[segmentTypeIndex][origTaz - 1][chosenTazIndex - 1];
			cumProbabilityLowerBound = tazDistCumProbs[segmentTypeIndex][origTaz - 1][chosenTazIndex - 1];
		} else {
			tazProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][0];
			cumProbabilityLowerBound = 0;
		}

		// get the array of MGRAs for the chosen TAZ (the chosen index + 1)
		int[] mgraArray = tdm.getMgraArray(chosenTazIndex + 1);

		// get the unscaled MGRA size probability, scale by the TAZ distance
		// probability, and accumulate cumulative probabilities
		tempMgraCumProbs[0] = cumProbabilityLowerBound
				+ (mgraSizeProbs[segmentTypeIndex][chosenTazIndex][0] * tazProb);
		for (int i = 1; i < mgraArray.length; i++)
			tempMgraCumProbs[i] = tempMgraCumProbs[i - 1]
					+ (mgraSizeProbs[segmentTypeIndex][chosenTazIndex][i] * tazProb);

		// get the chosen array index for the 0-based cumulative probabilities
		// array
		int chosenMgraIndex = Util.binarySearchDouble(cumProbabilityLowerBound,
				tempMgraCumProbs, mgraArray.length, rn);

		// use the chosen mgra index to get the chosenMgra value from the
		// 0-based array of MGRAs associated with the chosen TAZ
		int chosenMgra = mgraArray[chosenMgraIndex];

		// store the sampled mgra and its selection probability
		sampleMgras[sampleIndex] = chosenMgra;
		sampleProbabilities[sampleIndex] = (mgraSizeProbs[segmentTypeIndex][chosenTazIndex][chosenMgraIndex] * tazProb);

		// if the sample freq is 0, this mgra has not been selected yet, so add
		// it to the array of unique sampled mgras.
		if (mgraSampleFreq[chosenMgra] == 0) {
			uniqueMgraSample[uniqueIndex] = chosenMgra;
			uniqueSampleIndices[uniqueIndex] = sampleIndex;
			uniqueIndex++;
		}

		// increment the frequency of times this mgra was selected for the
		// sample
		mgraSampleFreq[chosenMgra]++;

		if (debug) {

			double cumDistProb = 0;
			double prevDistCumProb = 0;
			if (chosenTazIndex > 1) {
				cumDistProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][chosenTazIndex];
				prevDistCumProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][chosenTazIndex - 1];
			} else {
				cumDistProb = tazDistCumProbs[segmentTypeIndex][origTaz - 1][0];
				prevDistCumProb = 0;
			}

			double cumSizeProb = 0;
			double prevSizeCumProb = 0;
			if (chosenMgraIndex > 0) {
				cumSizeProb = tempMgraCumProbs[chosenMgraIndex];
				prevSizeCumProb = tempMgraCumProbs[chosenMgraIndex - 1];
			} else {
				cumSizeProb = tempMgraCumProbs[0];
				prevSizeCumProb = 0;
			}

			logger.info(String
					.format("%-12d %10d %10.6f %16.8f %16.8f %18d %18.8f %18.8f %12d %18.8f",
							sampleIndex,
							chosenTazIndex,
							rn,
							prevDistCumProb,
							cumDistProb,
							chosenMgraIndex,
							prevSizeCumProb,
							cumSizeProb,
							chosenMgra,
							((cumSizeProb - prevSizeCumProb) * (cumDistProb - prevDistCumProb))));
		}

	}

	private void chooseSlcSampleMgraBinarySearch(int sampleIndex,
			int slcOrigTaz, int slcDestTaz, int slcSizeSegmentIndex, double rn,
			boolean debug) {

		// compute stop location sample probabilities from the pre-computed
		// sample exponentiated utilities and taz size terms.
		// first compute exponentiated utilites for each alternative from the
		// pre-computed component exponentiated utilities
		double totalExponentiatedUtility = 0;
		for (int k = 0; k < maxTaz; k++) {
			slcTazProbs[k] = (slcTazDistExpUtils[slcOrigTaz - 1][k]
					* slcTazDistExpUtils[k][slcDestTaz - 1] / slcTazDistExpUtils[slcOrigTaz - 1][slcDestTaz - 1])
					* slcTazSize[slcSizeSegmentIndex][k + 1];
			totalExponentiatedUtility += slcTazProbs[k];
		}

		// now compute alterantive probabilities and determine selected
		// alternative
		slcTazCumProbs[0] = slcTazProbs[0] / totalExponentiatedUtility;
		for (int k = 1; k < maxTaz - 1; k++)
			slcTazCumProbs[k] = slcTazCumProbs[k - 1]
					+ (slcTazProbs[k] / totalExponentiatedUtility);
		slcTazCumProbs[maxTaz - 1] = 1.0;

		// get the chosen TAZ array index for the 0-based cumulative TAZ
		// distance probabilities array
		int chosenTazIndex = Util.binarySearchDouble(slcTazCumProbs, rn);

		/*
		 * // now compute alterantive probabilities and determine selected
		 * alternative int chosenTazIndex0 = -1; double sum = slcTazProbs[0] /
		 * totalExponentiatedUtility; if ( rn < sum ) { chosenTazIndex0 = 0; }
		 * else { for ( int k=1; k < maxTaz; k++ ) { slcTazProbs[k] /=
		 * totalExponentiatedUtility; sum += slcTazProbs[k]; if ( rn < sum ) {
		 * chosenTazIndex0 = k; break; } } }
		 * 
		 * 
		 * if ( chosenTazIndex0 != chosenTazIndex ) { logger.error (
		 * "error - inconsistent choices made by two alternative monte carlo methods. "
		 * ); System.exit(-1); }
		 */

		if (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex].length == 0) {
			logger.error("The MGRA size probabilities array for chosen stop location TAZ index = "
					+ chosenTazIndex + " has 0 length.");
			logger.error("This should not be the case.  If a TAZ was chosen, its TAZ Size > 0, so there should be at least one MGRA with size > 0 in the TAZ.");
			logger.error("Likely cause is an indexing bug.  sampleIndex="
					+ sampleIndex + ", slcOrigTaz=" + slcOrigTaz
					+ ", slcDestTaz=" + slcDestTaz + ", slcSizeSegmentIndex="
					+ slcSizeSegmentIndex);
			throw new RuntimeException();
		}

		// get the chosen SLC TAZ distance probability from the taz distance
		// cumulative probabilities array
		// also initialize the 0 index cumulative MGRA probability to the
		// cumulative taz distance propbaility
		double tazProb = 0;
		double cumProbabilityLowerBound = 0;
		if (chosenTazIndex > 0) {
			tazProb = slcTazCumProbs[chosenTazIndex]
					- slcTazCumProbs[chosenTazIndex - 1];
			cumProbabilityLowerBound = slcTazCumProbs[chosenTazIndex - 1];
		} else {
			tazProb = slcTazCumProbs[0];
			cumProbabilityLowerBound = 0;
		}

		// get the array of MGRAs for the chosen TAZ (the chosen index + 1)
		int[] mgraArray = tdm.getMgraArray(chosenTazIndex + 1);

		// get the unscaled MGRA size probability, scale by the TAZ distance
		// probability, and accumulate cumulative probabilities
		tempMgraCumProbs[0] = cumProbabilityLowerBound
				+ (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][0] * tazProb);
		for (int i = 1; i < mgraArray.length; i++)
			tempMgraCumProbs[i] = tempMgraCumProbs[i - 1]
					+ (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][i] * tazProb);

		// get the chosen array index for the 0-based cumulative probabilities
		// array
		int chosenMgraIndex = Util.binarySearchDouble(cumProbabilityLowerBound,
				tempMgraCumProbs, mgraArray.length, rn);

		// use the chosen mgra index to get the chosenMgra value from the
		// 0-based array of MGRAs associated with the chosen TAZ
		int chosenMgra = mgraArray[chosenMgraIndex];

		// store the sampled mgra and its selection probability
		sampleMgras[sampleIndex] = chosenMgra;
		sampleProbabilities[sampleIndex] = (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][chosenMgraIndex] * tazProb);

		// if the sample freq is 0, this mgra has not been selected yet, so add
		// it to the array of unique sampled mgras.
		if (mgraSampleFreq[chosenMgra] == 0) {
			uniqueMgraSample[uniqueIndex] = chosenMgra;
			uniqueSampleIndices[uniqueIndex] = sampleIndex;
			uniqueIndex++;
		}

		// increment the frequency of times this mgra was selected for the
		// sample
		mgraSampleFreq[chosenMgra]++;

	}

	private void chooseSlcSampleMgraLinearWalk(int sampleIndex, int slcOrigTaz,
			int slcDestTaz, int slcSizeSegmentIndex, double rn, boolean debug) {

		// compute stop location sample probabilities from the pre-computed
		// sample exponentiated utilities and taz size terms.
		// first compute exponentiated utilites for each alternative from the
		// pre-computed component exponentiated utilities
		double totalExponentiatedUtility = 0;
		for (int k = 0; k < maxTaz; k++) {
			slcTazProbs[k] = (slcTazDistExpUtils[slcOrigTaz - 1][k]
					* slcTazDistExpUtils[k][slcDestTaz - 1] / slcTazDistExpUtils[slcOrigTaz - 1][slcDestTaz - 1])
					* slcTazSize[slcSizeSegmentIndex][k + 1];
			totalExponentiatedUtility += slcTazProbs[k];
		}

		/*
		 * // now compute alterantive probabilities and determine selected
		 * alternative slcTazCumProbs[0] = slcTazProbs[0] /
		 * totalExponentiatedUtility; for ( int k=1; k < maxTaz - 1; k++ )
		 * slcTazCumProbs[k] = slcTazCumProbs[k-1] + (slcTazProbs[k] /
		 * totalExponentiatedUtility); slcTazCumProbs[maxTaz - 1] = 1.0;
		 * 
		 * 
		 * /* // get the chosen TAZ array index for the 0-based cumulative TAZ
		 * distance probabilities array int chosenTazIndex0 =
		 * Util.binarySearchDouble( slcTazCumProbs, rn );
		 */

		// now compute alterantive probabilities and determine selected
		// alternative
		int chosenTazIndex = -1;
		double sum = slcTazProbs[0] / totalExponentiatedUtility;
		double cumProbabilityLowerBound = 0;
		double tazProb = 0;
		if (rn < sum) {
			chosenTazIndex = 0;
			tazProb = sum;
		} else {
			for (int k = 1; k < maxTaz; k++) {
				tazProb = slcTazProbs[k] / totalExponentiatedUtility;
				cumProbabilityLowerBound = sum;
				sum += tazProb;
				if (rn < sum) {
					chosenTazIndex = k;
					break;
				}
			}
		}

		/*
		 * if ( chosenTazIndex0 != chosenTazIndex ) { logger.error (
		 * "error - inconsistent choices made by two alternative monte carlo methods. "
		 * ); System.exit(-1); }
		 */

		if (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex].length == 0) {
			logger.error("The MGRA size probabilities array for chosen stop location TAZ index = "
					+ chosenTazIndex + " has 0 length.");
			logger.error("This should not be the case.  If a TAZ was chosen, its TAZ Size > 0, so there should be at least one MGRA with size > 0 in the TAZ.");
			logger.error("Likely cause is an indexing bug.  sampleIndex="
					+ sampleIndex + ", slcOrigTaz=" + slcOrigTaz
					+ ", slcDestTaz=" + slcDestTaz + ", slcSizeSegmentIndex="
					+ slcSizeSegmentIndex);
			throw new RuntimeException();
		}

		/*
		 * // get the chosen SLC TAZ distance probability from the taz distance
		 * cumulative probabilities array // also initialize the 0 index
		 * cumulative MGRA probability to the cumulative taz distance
		 * propbaility double tazProb = 0; double cumProbabilityLowerBound = 0;
		 * if ( chosenTazIndex > 0 ) { tazProb = slcTazCumProbs[chosenTazIndex]
		 * - slcTazCumProbs[chosenTazIndex-1]; cumProbabilityLowerBound =
		 * slcTazCumProbs[chosenTazIndex-1]; } else { tazProb =
		 * slcTazCumProbs[0]; cumProbabilityLowerBound = 0; }
		 */

		// get the array of MGRAs for the chosen TAZ (the chosen index + 1)
		int[] mgraArray = tdm.getMgraArray(chosenTazIndex + 1);

		/*
		 * // get the unscaled MGRA size probability, scale by the TAZ distance
		 * probability, and accumulate cumulative probabilities
		 * tempMgraCumProbs[0] = cumProbabilityLowerBound + (
		 * slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][0] * tazProb ); for
		 * ( int i=1; i < mgraArray.length; i++ ) tempMgraCumProbs[i] =
		 * tempMgraCumProbs[i-1] + (
		 * slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][i] * tazProb );
		 */

		// now compute alterantive probabilities and determine selected
		// alternative
		int chosenMgraIndex = -1;
		sum = cumProbabilityLowerBound
				+ (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][0] * tazProb);
		if (rn < sum) {
			chosenMgraIndex = 0;
		} else {
			for (int k = 1; k < mgraArray.length; k++) {
				sum += (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][k] * tazProb);
				if (rn < sum) {
					chosenMgraIndex = k;
					break;
				}
			}
		}

		/*
		 * // get the chosen array index for the 0-based cumulative
		 * probabilities array int chosenMgraIndex = Util.binarySearchDouble(
		 * cumProbabilityLowerBound, tempMgraCumProbs, mgraArray.length, rn );
		 */

		// use the chosen mgra index to get the chosenMgra value from the
		// 0-based array of MGRAs associated with the chosen TAZ
		int chosenMgra = mgraArray[chosenMgraIndex];

		// store the sampled mgra and its selection probability
		sampleMgras[sampleIndex] = chosenMgra;
		sampleProbabilities[sampleIndex] = (slcSizeProbs[slcSizeSegmentIndex][chosenTazIndex][chosenMgraIndex] * tazProb);

		// if the sample freq is 0, this mgra has not been selected yet, so add
		// it to the array of unique sampled mgras.
		if (mgraSampleFreq[chosenMgra] == 0) {
			uniqueMgraSample[uniqueIndex] = chosenMgra;
			uniqueSampleIndices[uniqueIndex] = sampleIndex;
			uniqueIndex++;
		}

		// increment the frequency of times this mgra was selected for the
		// sample
		mgraSampleFreq[chosenMgra]++;

	}

	public void chooseSample(int origTaz, int sizeSegmentIndex,
			int segmentTypeIndex, int numInSample, Random rand, boolean debug) {

		long timeCheck = System.nanoTime();

		if (debug) {
			computeSoaProbabilities(origTaz, segmentTypeIndex);
		}

		resetSampleArrays();
		for (int i = 0; i < numInSample; i++) {
			chooseSampleMgra(i, origTaz, sizeSegmentIndex, segmentTypeIndex,
					rand.nextDouble(), debug);
		}

		soaRunTime += (System.nanoTime() - timeCheck);

	}

	public void chooseSlcSample(int origTaz, int destTaz, int sizeSegmentIndex,
			int numInSample, Random rand, boolean debug) {

		long timeCheck = System.nanoTime();

		resetSampleArrays();
		for (int i = 0; i < numInSample; i++) {
			// chooseSlcSampleMgraBinarySearch( i, origTaz, destTaz,
			// sizeSegmentIndex, rand.nextDouble(), debug );
			chooseSlcSampleMgraLinearWalk(i, origTaz, destTaz,
					sizeSegmentIndex, rand.nextDouble(), debug);
		}

		soaRunTime += (System.nanoTime() - timeCheck);

	}

	public void setSlcSoaProbsAndUtils(double[][] slcTazDistExpUtils,
			double[][][] slcSizeProbs, double[][] slcTazSize) {
		this.slcSizeProbs = slcSizeProbs;
		this.slcTazSize = slcTazSize;
		this.slcTazDistExpUtils = slcTazDistExpUtils;
	}

	public void setMgraSizeProbs(double[][][] probs) {
		mgraSizeProbs = probs;
	}

	public void setTazDistProbs(double[][][] probs) {
		tazDistCumProbs = probs;
	}

	public long getSoaRunTime() {
		return soaRunTime;
	}

	public void resetSoaRunTime() {
		soaRunTime = 0;
	}

}
