package org.sandag.abm.ctramp;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * Handles building and storing destination choice size variables
 * 
 */

public class DestChoiceSize implements Serializable {

	private transient Logger logger = Logger.getLogger(DestChoiceSize.class);
	private transient Logger convergeLogger = Logger.getLogger("converge");

	public static final String PROPERTIES_DC_SHADOW_OUTPUT = "uwsl.ShadowPricing.OutputFile";
	public static final String PROPERTIES_WORK_DC_SHADOW_NITER = "uwsl.ShadowPricing.Work.MaximumIterations";
	public static final String PROPERTIES_SCHOOL_DC_SHADOW_NITER = "uwsl.ShadowPricing.School.MaximumIterations";

	private int numSegments;
	private double[][] segmentSizeTerms;
	private HashMap<Integer, String> segmentIndexNameMap;
	private HashMap<String, Integer> segmentNameIndexMap;
	private HashSet<Integer> noShadowPriceSchoolSegmentIndices;
	private MgraDataManager mgraManager;

	// 1st dimension is an index for the set of DC Size variables used in Sample
	// of
	// Alternative choice and destination choice,
	// 2nd dimension is zone number (1,...,numZones), 3rd dimension walk subzone
	// index is 0: no walk %, 1: shrt %, 2: long %.
	protected double[][] dcSize;
	protected double[][] originalSize;
	protected double[][] originalAdjSize;
	protected double[][] scaledSize;
	protected double[][] balanceSize;
	protected double[][] previousSize;
	protected double[][] shadowPrice;

	protected double[][] externalFactors;

	protected int maxShadowPriceIterations;

	protected String dcShadowOutputFileName;

	protected boolean dcSizeCalculated = false;

	/**
	 * 
	 * @param propertyMap
	 *            is the model properties file key:value pairs
	 * @param segmentNameIndexMap
	 *            is a map from segment name to size term array index.
	 * @param segmentIndexNameMap
	 *            is a map from size term array index to segment name.
	 * @param segmentSizeTerms
	 *            is an array by segment index and MGRA index
	 */
	public DestChoiceSize(HashMap<String, String> propertyMap,
			HashMap<Integer, String> segmentIndexNameMap,
			HashMap<String, Integer> segmentNameIndexMap,
			double[][] segmentSizeTerms, int maxIterations) {

		this.segmentIndexNameMap = segmentIndexNameMap;
		this.segmentNameIndexMap = segmentNameIndexMap;
		this.segmentSizeTerms = segmentSizeTerms;

		// get the number of segments from the segmentIndexNameMap
		numSegments = segmentIndexNameMap.size();

		maxShadowPriceIterations = maxIterations;

		String projectDirectory = Util.getStringValueFromPropertyMap(
				propertyMap, CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
		dcShadowOutputFileName = projectDirectory
				+ propertyMap.get(PROPERTIES_DC_SHADOW_OUTPUT);

		mgraManager = MgraDataManager.getInstance();

		// set default external factors array (all 1.0s)
		// an object creating a destChoice object can override these valuse by
		// calling setExternalFactors().
		externalFactors = new double[numSegments][mgraManager.getMaxMgra() + 1];
		for (int i = 0; i < numSegments; i++)
			Arrays.fill(externalFactors[i], 1.0);

	}

	/**
	 * @return a boolean for whether or not the size terms in this object have
	 *         been calculated
	 */
	public boolean getDcSizeCalculated() {
		return dcSizeCalculated;
	}

	public HashMap<Integer, String> getSegmentIndexNameMap() {
		return segmentIndexNameMap;
	}

	public HashMap<String, Integer> getSegmentNameIndexMap() {
		return segmentNameIndexMap;
	}

	/**
	 * @return the maximum number of shadow price iterations set in the
	 *         properties file
	 */
	public int getMaxShadowPriceIterations() {
		return maxShadowPriceIterations;
	}

	public void setExternalFactors(double[][] factors) {
		externalFactors = factors;
	}

	public void setNoShadowPriceSchoolSegmentIndices(HashSet<Integer> indexSet) {
		noShadowPriceSchoolSegmentIndices = indexSet;
	}

	/**
	 * Scale the destination choice size values so that the total modeled
	 * destinations by segment match the total origins. total Origin/Destination
	 * constraining usuallu done for home oriented mandatory tours, e.g. work
	 * university, school. This method also has the capability to read a file of
	 * destination size adjustments and apply them during the balancing
	 * procedure. This capability was used in the Morpc model and was
	 * transferred to the Baylanta project, but may or may not be used.
	 * 
	 * @param originsByHomeZone
	 *            - total long term choice origin locations (i.e. number of
	 *            workers, university students, or school age students) in
	 *            residence zone, subzone, by segment.
	 * 
	 */
	public void balanceSizeVariables(int[][] originsByHomeMgra) {

		// store the original size variable values.
		// set the initial sizeBalance values to the original size variable
		// values.
		originalSize = duplicateDouble2DArray(segmentSizeTerms);
		balanceSize = duplicateDouble2DArray(segmentSizeTerms);

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		// create the shadow price array - num
		shadowPrice = new double[numSegments][maxMgra + 1];

		// create the total origin locations array to store total tours by
		// segment
		double[] totalOriginLocations = new double[numSegments];

		// create the total destination choice size array to store total tours
		// by
		// segment
		double[] totalDestSize = new double[numSegments];

		// initialize shadow prices with 1.0
		// accumulate total tours and size by segment.
		for (int i = 0; i < numSegments; i++) {
			for (int j = 1; j <= maxMgra; j++) {
				shadowPrice[i][j] = 1.0;
				totalOriginLocations[i] += originsByHomeMgra[i][j];
				totalDestSize[i] += (segmentSizeTerms[i][j] * externalFactors[i][j]);
			} // j (mgra)
		} // i (segment)

		// log a report of total origin locations by segment
		logger.info("");
		logger.info("total origin locations by segment before any balancing, destination size adjustments, or shadow price scaling:");
		double segmentSum = 0.0;
		for (int i = 0; i < numSegments; i++) {
			String segmentString = segmentIndexNameMap.get(i);
			segmentSum += totalOriginLocations[i];
			logger.info(String.format("    %-6d  %-55s:  %10.1f", i,
					segmentString, totalOriginLocations[i]));
		} // i
		logger.info(String.format("    %-6s  %-55s:  %10.1f", " ", "Total",
				segmentSum));
		logger.info("");

		// log a report of total destination choice size calculated by segment
		logger.info("");
		logger.info("total destination choice size by segment before any balancing, destination choice size adjustments, or shadow price scaling:");
		segmentSum = 0.0;
		for (int i = 0; i < numSegments; i++) {
			String segmentString = segmentIndexNameMap.get(i);
			segmentSum += totalDestSize[i];
			logger.info(String.format("    %-6d  %-55s:  %10.1f", i,
					segmentString, totalDestSize[i]));
		}
		logger.info(String.format("    %-6s  %-55s:  %10.1f", " ", "Total",
				segmentSum));
		logger.info("");

		// save original adjusted size variable arrays prior to balancing - used
		// in
		// reporting size variable calculations to output files.
		originalAdjSize = duplicateDouble2DArray(balanceSize);

		// Balance destination choice size variables to equal total origin
		// locations by segment.
		// The scaledSize calculated is what is adjusted by shadow pricing
		// adjustments, and dcSize, the array referenced
		// by UEC DMUs is a duplicate copy of this array after the shadow
		// pricing
		// calculations are made.
		scaledSize = new double[balanceSize.length][maxMgra + 1];
		double tot = 0.0;
		for (int i = 0; i < numSegments; i++) {

			tot = 0.0;
			for (int j = 1; j <= maxMgra; j++) {

				if (totalDestSize[i] > 0.0)
					scaledSize[i][j] = (balanceSize[i][j]
							* externalFactors[i][j] * totalOriginLocations[i])
							/ totalDestSize[i];
				else
					scaledSize[i][j] = 0.0f;

				tot += scaledSize[i][j];

			}

		}

		// set destination choice size variables for the first iteration of
		// shadow
		// pricing to calculated scaled values
		dcSize = duplicateDouble2DArray(scaledSize);

		// sum scaled destination size values by segment for reporting
		double[] sumScaled = new double[numSegments];
		for (int i = 0; i < numSegments; i++) {
			for (int j = 1; j <= maxMgra; j++)
				sumScaled[i] += scaledSize[i][j];
		}

		// log a report of total destination locations by segment
		logger.info("");
		logger.info("total destination choice size by segment after destination choice size adjustments, after shadow price scaling:");
		segmentSum = 0.0;
		for (int i = 0; i < numSegments; i++) {
			String segmentString = segmentIndexNameMap.get(i);
			segmentSum += sumScaled[i];
			logger.info(String.format("    %-6d  %-55s:  %10.1f", i,
					segmentString, sumScaled[i]));
		}
		logger.info(String.format("    %-6s  %-55s:  %10.1f", " ", "Total",
				segmentSum));
		logger.info("");

		// save scaled size variables used in shadow price adjustmnents for
		// reporting
		// to output file
		previousSize = new double[numSegments][];
		for (int i = 0; i < numSegments; i++)
			previousSize[i] = duplicateDouble1DArray(scaledSize[i]);

	}

	public double getDcSize(int segmentIndex, int mgra) {
		return dcSize[segmentIndex][mgra];
	}

	public double getDcSize(String segmentName, int mgra) {
		int segmentIndex = segmentNameIndexMap.get(segmentName);
		return dcSize[segmentIndex][mgra];
	}

	public double[][] getDcSizeArray() {
		return dcSize;
	}

	public int getNumberOfSegments() {
		return dcSize.length;
	}

	public void updateSizeVariables() {

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		for (int i = 0; i < numSegments; i++) {
			for (int j = 1; j <= maxMgra; j++) {
				dcSize[i][j] = scaledSize[i][j] * shadowPrice[i][j];
				if (dcSize[i][j] < 0.0f)
					dcSize[i][j] = 0.0f;
			}
		}

	}

	public void updateShadowPrices(int[][] modeledDestinationLocationsByDestMgra) {

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		for (int i = 0; i < numSegments; i++) {
			if (noShadowPriceSchoolSegmentIndices != null
					&& noShadowPriceSchoolSegmentIndices.contains(i))
				continue;

			for (int j = 1; j <= maxMgra; j++) {
				if (modeledDestinationLocationsByDestMgra[i][j] > 0)
					shadowPrice[i][j] *= (scaledSize[i][j] / modeledDestinationLocationsByDestMgra[i][j]);
				// else
				// shadowPrice[i][j] *= scaledSize[i][j];
			}
		}

	}

	public void reportMaxDiff(int iteration,
			int[][] modeledDestinationLocationsByDestMgra) {

		double[] maxSize = { 10, 100, 1000, Double.MAX_VALUE };
		double[] maxDeltas = { 0.05, 0.10, 0.25, 0.50, 1.0, Double.MAX_VALUE };

		int[] nObs = new int[maxSize.length];
		double[] sse = new double[maxSize.length];
		double[] sumObs = new double[maxSize.length];

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		logger.info("Shadow Price Iteration " + iteration);

		double minRange = 0.0;
		for (int r = 0; r < maxSize.length; r++) {

			logger.info(String
					.format("Frequency of chosen mgra locations with non-zero DC Size < %s by range of relative error",
							(maxSize[r] < 1000000 ? String.format("%.1f",
									maxSize[r]) : "+Inf")));
			logger.info(String.format(
					"%-6s  %-55s %15s %15s %15s %15s %15s %15s %15s %8s",
					"index", "segment", "0 DCs", "< 5%", "< 10%", "< 25%",
					"< 50%", "< 100%", "100% +", "Total"));

			int tot = 0;
			int[] tots = new int[maxDeltas.length + 1];
			String logRecord = "";
			for (int i = 0; i < numSegments; i++) {

				tot = 0;
				int[] freqs = new int[maxDeltas.length + 1];
				int nonZeroSizeLocs = 0;
				for (int j = 1; j <= maxMgra; j++) {

					if (scaledSize[i][j] > minRange
							&& scaledSize[i][j] <= maxSize[r]) {

						nonZeroSizeLocs++;

						if (modeledDestinationLocationsByDestMgra[i][j] == 0.0) {
							// store the number of DC alternatives where DC Size
							// > 0,
							// but alternative was not chosen.
							// relative error measure is not meaningful for this
							// case, so report number of cases separately.
							freqs[0]++;

							// calculations for %RMSE
							sse[r] += scaledSize[i][j] * scaledSize[i][j];
						} else {

							double relDiff = Math
									.abs(scaledSize[i][j]
											- modeledDestinationLocationsByDestMgra[i][j])
									/ scaledSize[i][j];
							for (int k = 0; k < maxDeltas.length; k++) {
								if (relDiff < maxDeltas[k]) {
									// store number of DC alternatives chosen
									// where
									// DC Size > 0, by relative error range.
									freqs[k + 1]++;
									break;
								}
							}

							// calculations for %RMSE
							sse[r] += relDiff * relDiff;
						}

						// calculations for %RMSE
						sumObs[r] += scaledSize[i][j];
						nObs[r]++;

					}

				}

				for (int k = 0; k < freqs.length; k++) {
					tots[k] += freqs[k];
					tot += freqs[k];
				}

				String segmentString = segmentIndexNameMap.get(i);
				logRecord = String.format("%-6d  %-55s", i, segmentString);

				for (int k = 0; k < freqs.length; k++) {
					float pct = 0.0f;
					if (tot > 0)
						pct = (float) (100.0 * freqs[k] / tot);
					logRecord += String.format(" %6d (%5.1f%%)", freqs[k], pct);
				}

				logRecord += String.format(" %8d", tot);
				logger.info(logRecord);

			}

			tot = 0;
			for (int k = 0; k < tots.length; k++) {
				tot += tots[k];
			}

			logRecord = String.format("%-6s  %-55s", " ", "Total");
			String underline = String.format("------------------------");

			for (int k = 0; k < tots.length; k++) {
				float pct = 0.0f;
				if (tot > 0)
					pct = (float) (100.0 * tots[k] / tot);
				logRecord += String.format(" %6d (%5.1f%%)", tots[k], pct);
				underline += String.format("----------------");
			}

			logRecord += String.format(" %8d", tot);
			underline += String.format("---------");

			logger.info(underline);
			logger.info(logRecord);

			double rmse = -1.0;
			if (nObs[r] > 1)
				rmse = 100.0 * (Math.sqrt(sse[r] / (nObs[r] - 1)) / (sumObs[r] / nObs[r]));

			logger.info("%RMSE = "
					+ (rmse < 0 ? "N/A, no observations" : String.format(
							"%.1f, with mean %.1f, for %d observations.", rmse,
							(sumObs[r] / nObs[r]), nObs[r])));

			logger.info("");

			minRange = maxSize[r];

		}

		logger.info("");
		logger.info("");

	}

	public void saveSchoolMaxDiffValues(int iteration,
			int[][] modeledDestinationLocationsByDestMgra) {

		// define labels for the schoolsegment categories
		String[] segmentRangelabels = { "Pre-School", "K-8", "9-12", "Univ" };

		// define the highest index value for the range of segments for the
		// school segment category
		int[] segmentRange = { 0, 36, 54, 56 };

		double[] maxSize = { 10, 100, 1000, Double.MAX_VALUE };
		double[] maxDeltas = { 0.05, 0.10, 0.25, 0.50, 1.0, 999.9 };

		int[][][] freqs = new int[segmentRangelabels.length][maxSize.length][maxDeltas.length];

		int[][] nObs = new int[segmentRangelabels.length][maxSize.length];
		double[][] sse = new double[segmentRangelabels.length][maxSize.length];
		double[][] sumObs = new double[segmentRangelabels.length][maxSize.length];

		double[][] rmse = new double[segmentRangelabels.length][maxSize.length];
		double[][] meanSize = new double[segmentRangelabels.length][maxSize.length];

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		convergeLogger.info("School Shadow Price Iteration " + iteration);

		double[] minRange = new double[segmentRangelabels.length];

		int minS = 0;
		for (int s = 0; s < segmentRangelabels.length; s++) {

			convergeLogger.info("");
			convergeLogger.info("");
			convergeLogger.info(segmentRangelabels[s]
					+ " convergence statistics");

			if (s > 0)
				minS = segmentRange[s - 1] + 1;

			for (int r = 0; r < maxSize.length; r++) {

				for (int i = minS; i <= segmentRange[s]; i++) {

					for (int j = 1; j <= maxMgra; j++) {

						if (scaledSize[i][j] > minRange[s]
								&& scaledSize[i][j] <= maxSize[r]) {

							if (modeledDestinationLocationsByDestMgra[i][j] > 0.0) {

								int delta = maxDeltas.length - 1;
								double diff = Math
										.abs(scaledSize[i][j]
												- modeledDestinationLocationsByDestMgra[i][j]);
								double relDiff = diff / scaledSize[i][j];
								for (int k = 0; k < maxDeltas.length; k++) {
									if (relDiff < maxDeltas[k]) {
										delta = k;
										break;
									}
								}

								freqs[s][r][delta]++;

								// calculations for %RMSE
								sse[s][r] += (diff * diff);

							}

							// calculations for %RMSE
							sumObs[s][r] += scaledSize[i][j];
							nObs[s][r]++;

						}

					}

				}

				rmse[s][r] = -1.0;
				if (nObs[s][r] > 1) {
					meanSize[s][r] = sumObs[s][r] / nObs[s][r];
					rmse[s][r] = 100.0 * (Math
							.sqrt((sse[s][r] / (nObs[s][r] - 1))) / meanSize[s][r]);
				}

				minRange[s] = maxSize[r];

			}

			convergeLogger.info("%RMSE by DC Size Range Category");
			for (int i = 0; i < maxSize.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12.2f",
						maxSize[i], rmse[s][i]));
			convergeLogger.info(String.format("%-8s %14.2f", "  1000+",
					rmse[s][maxSize.length - 1]));

			convergeLogger.info("");

			convergeLogger.info("%Mean DC Size by DC Size Range Category");
			for (int i = 0; i < maxSize.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12.2f",
						maxSize[i], meanSize[s][i]));
			convergeLogger.info(String.format("%-8s %14.2f", "  1000+",
					meanSize[s][maxSize.length - 1]));

			convergeLogger.info("");

			convergeLogger
					.info("Freq of MGRAs by DC Size Range Category and Relative Error");
			for (int r = 0; r < maxSize.length - 1; r++) {

				convergeLogger.info(String.format("Size < %-8.0f", maxSize[r]));
				for (int i = 0; i < maxDeltas.length - 1; i++)
					convergeLogger.info(String.format("< %-8.2f %12d",
							maxDeltas[i], freqs[s][r][i]));
				convergeLogger.info(String.format("%-8s %14d", "  1.0+",
						freqs[s][r][maxDeltas.length - 1]));

				convergeLogger.info("");
			}

			convergeLogger.info(String.format("Size >= 1000"));
			for (int i = 0; i < maxDeltas.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12d",
						maxDeltas[i], freqs[s][maxSize.length - 1][i]));
			convergeLogger.info(String.format("%-8s %14d", "  1.0+",
					freqs[s][maxSize.length - 1][maxDeltas.length - 1]));

			convergeLogger.info("");

		}

		convergeLogger.info("");
		convergeLogger.info("");
		convergeLogger.info("");
		convergeLogger.info("");

	}

	public void saveWorkMaxDiffValues(int iteration,
			int[][] modeledDestinationLocationsByDestMgra) {

		// define labels for the schoolsegment categories
		String[] segmentRangelabels = { "White Collar", "Services", "Health",
				"Retail and Food", "Blue Collar", "Military" };

		// define the highest index value for the range of segments for the
		// school segment category
		int[] segmentRange = { 0, 1, 2, 3, 4, 5 };

		double[] maxSize = { 10, 100, 1000, Double.MAX_VALUE };
		double[] maxDeltas = { 0.05, 0.10, 0.25, 0.50, 1.0, 999.9 };

		int[][][] freqs = new int[segmentRangelabels.length][maxSize.length][maxDeltas.length];

		int[][] nObs = new int[segmentRangelabels.length][maxSize.length];
		double[][] sse = new double[segmentRangelabels.length][maxSize.length];
		double[][] sumObs = new double[segmentRangelabels.length][maxSize.length];

		double[][] rmse = new double[segmentRangelabels.length][maxSize.length];
		double[][] meanSize = new double[segmentRangelabels.length][maxSize.length];

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		convergeLogger.info("Work Shadow Price Iteration " + iteration);

		double[] minRange = new double[segmentRangelabels.length];

		int minS = 0;
		for (int s = 0; s < segmentRangelabels.length; s++) {

			convergeLogger.info("");
			convergeLogger.info("");
			convergeLogger.info(segmentRangelabels[s]
					+ " convergence statistics");

			if (s > 0)
				minS = segmentRange[s - 1] + 1;

			for (int r = 0; r < maxSize.length; r++) {

				for (int i = minS; i <= segmentRange[s]; i++) {

					for (int j = 1; j <= maxMgra; j++) {

						if (scaledSize[i][j] > minRange[s]
								&& scaledSize[i][j] <= maxSize[r]) {

							if (modeledDestinationLocationsByDestMgra[i][j] > 0.0) {

								int delta = maxDeltas.length - 1;
								double diff = Math
										.abs(scaledSize[i][j]
												- modeledDestinationLocationsByDestMgra[i][j]);
								double relDiff = diff / scaledSize[i][j];
								for (int k = 0; k < maxDeltas.length; k++) {
									if (relDiff < maxDeltas[k]) {
										delta = k;
										break;
									}
								}

								freqs[s][r][delta]++;

								// calculations for %RMSE
								sse[s][r] += (diff * diff);

							}

							// calculations for %RMSE
							sumObs[s][r] += scaledSize[i][j];
							nObs[s][r]++;

						}

					}

				}

				rmse[s][r] = -1.0;
				if (nObs[s][r] > 1) {
					meanSize[s][r] = sumObs[s][r] / nObs[s][r];
					rmse[s][r] = 100.0 * (Math
							.sqrt((sse[s][r] / (nObs[s][r] - 1))) / meanSize[s][r]);
				}

				minRange[s] = maxSize[r];

			}

			convergeLogger.info("%RMSE by DC Size Range Category");
			for (int i = 0; i < maxSize.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12.2f",
						maxSize[i], rmse[s][i]));
			convergeLogger.info(String.format("%-8s %14.2f", "  1000+",
					rmse[s][maxSize.length - 1]));

			convergeLogger.info("");

			convergeLogger.info("%Mean DC Size by DC Size Range Category");
			for (int i = 0; i < maxSize.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12.2f",
						maxSize[i], meanSize[s][i]));
			convergeLogger.info(String.format("%-8s %14.2f", "  1000+",
					meanSize[s][maxSize.length - 1]));

			convergeLogger.info("");

			convergeLogger
					.info("Freq of MGRAs by DC Size Range Category and Relative Error");
			for (int r = 0; r < maxSize.length - 1; r++) {

				convergeLogger.info(String.format("Size < %-8.0f", maxSize[r]));
				for (int i = 0; i < maxDeltas.length - 1; i++)
					convergeLogger.info(String.format("< %-8.2f %12d",
							maxDeltas[i], freqs[s][r][i]));
				convergeLogger.info(String.format("%-8s %14d", "  1.0+",
						freqs[s][r][maxDeltas.length - 1]));

				convergeLogger.info("");
			}

			convergeLogger.info(String.format("Size >= 1000"));
			for (int i = 0; i < maxDeltas.length - 1; i++)
				convergeLogger.info(String.format("< %-8.2f %12d",
						maxDeltas[i], freqs[s][maxSize.length - 1][i]));
			convergeLogger.info(String.format("%-8s %14d", "  1.0+",
					freqs[s][maxSize.length - 1][maxDeltas.length - 1]));

			convergeLogger.info("");
		}

		convergeLogger.info("");
		convergeLogger.info("");
		convergeLogger.info("");
		convergeLogger.info("");

	}

	public boolean getSegmentIsInSkipSegmentSet(int segment) {
		return noShadowPriceSchoolSegmentIndices.contains(segment);
	}

	public void updateShadowPricingInfo(int iteration,
			int[][] originsByHomeMgra,
			int[][] modeledDestinationLocationsByDestMgra, String mandatoryType) {

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		ArrayList<String> tableHeadings = new ArrayList<String>();
		tableHeadings.add("alt");
		tableHeadings.add("mgra");

		for (int i = 0; i < numSegments; i++) {

			String segmentString = segmentIndexNameMap.get(i);

			tableHeadings.add(String.format("%s_origins", segmentString));
			tableHeadings.add(String.format("%s_sizeOriginal", segmentString));
			tableHeadings.add(String
					.format("%s_sizeAdjOriginal", segmentString));
			tableHeadings.add(String.format("%s_sizeScaled", segmentString));
			tableHeadings.add(String.format("%s_sizePrevious", segmentString));
			tableHeadings.add(String.format("%s_modeledDests", segmentString));
			tableHeadings.add(String.format("%s_sizeFinal", segmentString));
			tableHeadings.add(String.format("%s_shadowPrices", segmentString));

		}

		// define a TableDataSet for use in writing output file
		float[][] tableData = new float[maxMgra + 1][tableHeadings.size()];

		int alt = 0;
		for (int i = 1; i <= maxMgra; i++) {

			tableData[alt][0] = alt + 1;
			tableData[alt][1] = i;

			int index = 2;

			for (int p = 0; p < numSegments; p++) {
				tableData[alt][index++] = (float) originsByHomeMgra[p][i];
				tableData[alt][index++] = (float) originalSize[p][i];
				tableData[alt][index++] = (float) originalAdjSize[p][i];
				tableData[alt][index++] = (float) scaledSize[p][i];
				tableData[alt][index++] = (float) previousSize[p][i];
				tableData[alt][index++] = (float) modeledDestinationLocationsByDestMgra[p][i];
				tableData[alt][index++] = (float) dcSize[p][i];
				tableData[alt][index++] = (float) shadowPrice[p][i];
			}
			alt++;

		}

		TableDataSet outputTable = TableDataSet
				.create(tableData, tableHeadings);

		// write outputTable to new output file
		try {
			String newFilename = this.dcShadowOutputFileName.replaceFirst(
					".csv", "_" + mandatoryType + "_" + iteration + ".csv");
			CSVFileWriter writer = new CSVFileWriter();
			writer.writeFile(outputTable, new File(newFilename),
					new DecimalFormat("#.000000000000"));
			// writer.writeFile( outputTable, new File(newFilename) );
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// save scaled size variables used in shadow price adjustmnents for
		// reporting
		// to output file
		for (int i = 0; i < numSegments; i++)
			previousSize[i] = duplicateDouble1DArray(dcSize[i]);

	}

	public void restoreShadowPricingInfo(String fileName) {

		OLD_CSVFileReader reader = new OLD_CSVFileReader();

		TableDataSet tds = null;
		try {
			tds = reader.readFileAsDouble(new File(fileName));
		} catch (IOException e) {
			logger.error("exception reading saved shadow price file: "
					+ fileName + " from previous model run.", e);
		}

		// the following are based on format used to write the shadow pricing
		// file
		// first three columns are indices: ALT, ZONE, SUBZONE.
		int columnIndex = 2;
		int numberOfColumnsPerPurpose = 8;
		int scaledSizeColumnOffset = 3;
		int previousSizeColumnOffset = 4;
		int finalSizeColumnOffset = 6;
		int finalShadowPriceOffset = 7;

		// get the number of MGRAs
		int maxMgra = mgraManager.getMaxMgra();

		for (int i = 0; i < numSegments; i++) {

			// first restore the scaled size values; getColumnAsFloat(column)
			// takes a
			// 1s based column value, returns a 0s based array of values
			int column = columnIndex + i * numberOfColumnsPerPurpose
					+ scaledSizeColumnOffset + 1;
			double[] columnData = tds.getColumnAsDoubleFromDouble(column);
			for (int z = 1; z <= maxMgra; z++)
				scaledSize[i][z] = columnData[z - 1];

			// next restore the final size values
			column = columnIndex + i * numberOfColumnsPerPurpose
					+ finalSizeColumnOffset + 1;
			columnData = tds.getColumnAsDoubleFromDouble(column);
			for (int z = 1; z <= maxMgra; z++)
				dcSize[i][z] = columnData[z - 1];

			// next restore the previous size values from the final size of the
			// previous iteration
			column = columnIndex + i * numberOfColumnsPerPurpose
					+ finalSizeColumnOffset + 1;
			columnData = tds.getColumnAsDoubleFromDouble(column);
			for (int z = 1; z <= maxMgra; z++)
				previousSize[i][z] = columnData[z - 1];

			// finally restore the final shadow price values
			column = columnIndex + i * numberOfColumnsPerPurpose
					+ finalShadowPriceOffset + 1;
			columnData = tds.getColumnAsDoubleFromDouble(column);
			for (int z = 1; z <= maxMgra; z++)
				shadowPrice[i][z] = columnData[z - 1];

		}

	}

	/**
	 * Create a new double[], dimension it exactly as the argument array, and
	 * copy the element values from the argument array to the new one.
	 * 
	 * @param in
	 *            a 1-dimension double array to be duplicated
	 * @return an exact duplicate of the argument array
	 */
	private double[] duplicateDouble1DArray(double[] in) {
		double[] out = new double[in.length];
		for (int i = 0; i < in.length; i++) {
			out[i] = in[i];
		}
		return out;
	}

	/**
	 * Create a new double[][], dimension it exactly as the argument array, and
	 * copy the element values from the argument array to the new one.
	 * 
	 * @param in
	 *            a 2-dimensional double array to be duplicated
	 * @return an exact duplicate of the argument array
	 */
	private double[][] duplicateDouble2DArray(double[][] in) {
		double[][] out = new double[in.length][];
		for (int i = 0; i < in.length; i++) {
			out[i] = new double[in[i].length];
			for (int j = 0; j < in[i].length; j++) {
				out[i][j] = in[i][j];
			}
		}
		return out;
	}

}
