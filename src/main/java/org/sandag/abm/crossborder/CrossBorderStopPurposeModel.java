package org.sandag.abm.crossborder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class is the stop purpose choice model for cross border tours. It is
 * currently based on a static probability distribution stored in an input file,
 * and indexed into by purpose, tour leg direction (inbound or outbound), the
 * stop number, and whether there is just one or multiple stops on the tour leg.
 * 
 * @author Freedman
 * 
 */
public class CrossBorderStopPurposeModel {
	private transient Logger logger = Logger.getLogger("crossBorderModel");

	private double[][] cumProbability; // by alternative, stop purpose:
										// cumulative probability distribution
	CrossBorderModelStructure modelStructure;

	HashMap<Integer, Integer> arrayElementMap; // Hashmap used to get the
												// element number of the
												// cumProbability array based on
												// the
												// tour purpose, tour leg
												// direction, stop number, and
												// stop complexity.

	/**
	 * Constructor.
	 */
	public CrossBorderStopPurposeModel(HashMap<String, String> rbMap) {

		String directory = Util.getStringValueFromPropertyMap(rbMap,
				"Project.Directory");
		String stopFrequencyFile = Util.getStringValueFromPropertyMap(rbMap,
				"crossBorder.stop.purpose.file");
		stopFrequencyFile = directory + stopFrequencyFile;

		modelStructure = new CrossBorderModelStructure();

		arrayElementMap = new HashMap<Integer, Integer>();
		readStopPurposeFile(stopFrequencyFile);

	}

	/**
	 * Read the stop frequency distribution in the file and populate the arrays.
	 * 
	 * @param fileName
	 */
	private void readStopPurposeFile(String fileName) {

		logger.info("Begin reading the data in file " + fileName);
		TableDataSet probabilityTable;

		try {
			OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
			probabilityTable = csvFile.readFile(new File(fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.info("End reading the data in file " + fileName);

		logger.info("Begin calculating stop purpose probability distribution");

		// take a pass through the data and see how many alternatives there are
		// for each purpose
		int rowCount = probabilityTable.getRowCount();
		int purposes = modelStructure.NUMBER_CROSSBORDER_PURPOSES; // start at 0

		cumProbability = new double[rowCount][purposes];
		for (int row = 1; row <= rowCount; ++row) {

			int purpose = (int) probabilityTable.getValueAt(row, "TourPurp");

			int inbound = (int) probabilityTable.getValueAt(row, "Inbound");
			int stopNumber = (int) probabilityTable.getValueAt(row, "StopNum");
			int multiple = (int) probabilityTable.getValueAt(row, "Multiple");

			// store cumulative probabilities
			float cumProb = 0;
			for (int p = 0; p < purposes; ++p) {
				String label = "StopPurp" + p;
				cumProb += probabilityTable.getValueAt(row, label);
				cumProbability[row - 1][p] += cumProb;
			}

			if (Math.abs(cumProb - 1.0) > 0.00001)
				logger.info("Cumulative probability for tour purpose "
						+ purpose + " inbound " + inbound + " stopNumber "
						+ stopNumber + " multiple " + multiple + " is "
						+ cumProb);

			int key = getKey(purpose, inbound, stopNumber, multiple);
			arrayElementMap.put(key, row - 1);

		}

		logger.info("End calculating stop purpose probability distribution");

	}

	/**
	 * Get the key for the arrayElementMap.
	 * 
	 * @param tourPurp
	 *            Tour purpose
	 * @param isInbound
	 *            1 if the stop is on the inbound direction, else 0.
	 * @param stopNumber
	 *            The number of the stop.
	 * @param multipleStopsOnLeg
	 *            1 if multiple stops on leg, else 0.
	 * @return arrayElementMap key.
	 */
	private int getKey(int tourPurp, int isInbound, int stopNumber,
			int multipleStopsOnLeg) {

		return tourPurp * 1000 + isInbound * 100 + stopNumber * 10
				+ multipleStopsOnLeg;
	}

	/**
	 * Calculate purposes all stops on the tour
	 * 
	 * @param tour
	 *            A cross border tour (with tour purpose)
	 */
	public void calculateStopPurposes(CrossBorderTour tour) {

		// outbound stops first
		if (tour.getNumberOutboundStops() != 0) {

			int tourPurp = tour.getPurpose();
			CrossBorderStop[] stops = tour.getOutboundStops();
			int multiple = 0;
			if (stops.length > 1)
				multiple = 1;

			// iterate through stop list and calculate purpose for each
			for (int i = 0; i < stops.length; ++i) {
				int key = getKey(tourPurp, 0, i + 1, multiple);
				int element = arrayElementMap.get(key);
				double[] cumProb = cumProbability[element];
				double rand = tour.getRandom();
				int purpose = chooseFromDistribution(rand, cumProb);
				stops[i].setPurpose((byte) purpose);
			}
		}
		// inbound stops last
		if (tour.getNumberInboundStops() != 0) {

			int tourPurp = tour.getPurpose();
			CrossBorderStop[] stops = tour.getInboundStops();
			int multiple = 0;
			if (stops.length > 1)
				multiple = 1;

			// iterate through stop list and calculate purpose for each
			for (int i = 0; i < stops.length; ++i) {
				int key = getKey(tourPurp, 1, i + 1, multiple);
				int element = arrayElementMap.get(key);
				double[] cumProb = cumProbability[element];
				double rand = tour.getRandom();
				int purpose = chooseFromDistribution(rand, cumProb);
				stops[i].setPurpose((byte) purpose);
			}
		}
	}

	/**
	 * Choose purpose from the cumulative probability distribution
	 * 
	 * @param random
	 *            Uniformly distributed random number
	 * @param cumProb
	 *            Cumulative probability distribution
	 * @return Stop purpose (0 init).
	 */
	private int chooseFromDistribution(double random, double[] cumProb) {

		int choice = -1;
		for (int i = 0; i < cumProb.length; ++i) {
			if (random < cumProb[i]) {
				choice = i;
				break;
			}

		}
		return choice;
	}

}
