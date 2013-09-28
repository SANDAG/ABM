package org.sandag.abm.airport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.Tracer;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class AirportDestChoiceModel {

	private double[][] sizeTerms; // by segment, tazNumber
	private double[][][] mgraProbabilities; // by segment, tazNumber, mgra index
											// (sequential, 0-based)
	private double[][] tazProbabilities; // by segment, origin taz 0-based (note
											// since airport model, only 1 taz
											// dimension is needed)
	private int[] zipCodes; // by taz

	private TableDataSet alternativeData; // the alternatives, with a dest field
											// indicating tazNumber

	private transient Logger logger = Logger.getLogger("airportModel");

	private TazDataManager tazManager;
	private MgraDataManager mgraManager;

	private ChoiceModelApplication[] destModel;
	private UtilityExpressionCalculator sizeTermUEC;
	private Tracer tracer;
	private boolean trace;
	private int[] traceOtaz;
	private int[] traceDtaz;
	private boolean seek;
	private HashMap<String, String> rbMap;

	private int airportMgra;

	/**
	 * Constructor
	 * 
	 * @param propertyMap
	 *            Resource properties file map.
	 * @param dmuFactory
	 *            Factory object for creation of airport model DMUs
	 */
	public AirportDestChoiceModel(HashMap<String, String> rbMap,
			AirportDmuFactoryIf dmuFactory) {

		this.rbMap = rbMap;

		tazManager = TazDataManager.getInstance(rbMap);
		mgraManager = MgraDataManager.getInstance(rbMap);

		String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
				CtrampApplication.PROPERTIES_UEC_PATH);
		String airportDistUecFileName = Util.getStringValueFromPropertyMap(
				rbMap, "airport.dc.uec.file");
		airportDistUecFileName = uecFileDirectory + airportDistUecFileName;

		int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(
				rbMap, "airport.dc.data.page"));
		int sizePage = Integer.parseInt(Util.getStringValueFromPropertyMap(
				rbMap, "airport.dc.size.page"));

		// read the model pages from the property file, create one choice model
		// for each
		destModel = new ChoiceModelApplication[AirportModelStructure.INTERNAL_PURPOSES];
		for (int i = 0; i < AirportModelStructure.INTERNAL_PURPOSES; ++i) {

			// get page from property file
			String purposeName = "airport.dc.segment" + (i + 1) + ".page";
			String purposeString = Util.getStringValueFromPropertyMap(rbMap,
					purposeName);
			purposeString.replaceAll(" ", "");
			int destModelPage = Integer.parseInt(purposeString);

			// initiate a DMU for each segment
			AirportModelDMU dcDmu = dmuFactory.getAirportModelDMU();

			// create a ChoiceModelApplication object for the filename, model
			// page and data page.
			destModel[i] = new ChoiceModelApplication(airportDistUecFileName,
					destModelPage, dataPage, rbMap, (VariableTable) dcDmu);
		}

		// get the alternative data from the first segment
		UtilityExpressionCalculator uec = destModel[0].getUEC();
		alternativeData = uec.getAlternativeData();

		// create a UEC to solve size terms for each MGRA
		sizeTermUEC = new UtilityExpressionCalculator(new File(
				airportDistUecFileName), sizePage, dataPage, rbMap,
				dmuFactory.getAirportModelDMU());

		// set up the tracer object
		trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
		traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
		traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");
		tracer = Tracer.getTracer();
		tracer.setTrace(trace);
		if (trace) {
			for (int i = 0; i < traceOtaz.length; i++) {
				for (int j = 0; j < traceDtaz.length; j++) {
					tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
				}
			}
		}
		seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");

		airportMgra = Util.getIntegerValueFromPropertyMap(rbMap,
				"airport.airportMgra");

		// calculate the zip code array
		calculateZipCodes();

	}

	/**
	 * Iterate through the segments, calculating the mgra probabilities for each
	 */
	public void calculateMgraProbabilities(AirportDmuFactoryIf dmuFactory) {

		logger.info("Calculating Airport Model Size Terms");

		ArrayList<Integer> mgras = mgraManager.getMgras();
		int[] mgraTaz = mgraManager.getMgraTaz();
		int maxMgra = mgraManager.getMaxMgra();
		int alternatives = sizeTermUEC.getNumberOfAlternatives();

		double[][] mgraSizeTerms = new double[alternatives][maxMgra + 1];
		IndexValues iv = new IndexValues();
		AirportModelDMU aDmu = dmuFactory.getAirportModelDMU();

		// loop through mgras and calculate size terms
		for (int mgra : mgras) {

			int taz = mgraTaz[mgra];
			iv.setZoneIndex(mgra);
			double[] utilities = sizeTermUEC.solve(iv, aDmu, null);

			// store the size terms
			for (int segment = 0; segment < alternatives; ++segment) {
				mgraSizeTerms[segment][mgra] = utilities[segment];
			}

			// log
			if (tracer.isTraceOn() && tracer.isTraceZone(taz)) {

				logger.info("Size Term calculations for mgra " + mgra);
				sizeTermUEC.logResultsArray(logger, 0, mgra);

			}
		}

		// now iterate through tazs, calculate probabilities
		int[] tazs = tazManager.getTazs();
		int maxTaz = tazManager.getMaxTaz();

		// initialize arrays
		mgraProbabilities = new double[alternatives][maxTaz + 1][];
		sizeTerms = new double[alternatives][maxTaz + 1];

		// calculate arrays
		for (int segment = 0; segment < alternatives; ++segment) {
			for (int taz = 0; taz < tazs.length; ++taz) {
				int tazNumber = tazs[taz];
				int[] mgraArray = tazManager.getMgraArray(tazNumber);

				// initialize the vector of mgras for this purpose-taz
				mgraProbabilities[segment][tazNumber] = new double[mgraArray.length];

				// first calculate the sum of size for all the mgras in the taz
				double sum = 0;
				for (int mgra = 0; mgra < mgraArray.length; ++mgra) {

					int mgraNumber = mgraArray[mgra];

					sum += mgraSizeTerms[segment][mgraNumber];
				}
				// store the logsum in the size term array by taz
				if (sum > 0.0)
					sizeTerms[segment][tazNumber] = Math.log(sum + 1.0);

				// now calculate the cumulative probability distribution
				double lastProb = 0.0;
				for (int mgra = 0; mgra < mgraArray.length; ++mgra) {

					int mgraNumber = mgraArray[mgra];
					if (sum > 0.0)
						mgraProbabilities[segment][tazNumber][mgra] = lastProb
								+ mgraSizeTerms[segment][mgraNumber] / sum;
					lastProb = mgraProbabilities[segment][tazNumber][mgra];
				}
				if (sum > 0.0 && Math.abs(lastProb - 1.0) > 0.000001)
					logger.info("Error: segment " + segment + " taz "
							+ tazNumber + " cum prob adds up to " + lastProb);
			}

		}
		logger.info("Finished Calculating Airport Model Size Terms");
	}

	/**
	 * Calculate the zip codes at a taz level from the mgra data file. This
	 * requires the mgra data to be specified as mgra.socec.file in the
	 * properties file. The mgra file must have four fields: zone, taz, pop, and
	 * zip The taz zip is coded based upon the highest population mgra within
	 * the taz.
	 * 
	 * @return a zip code array dimensioned by taz numbers
	 */
	public void calculateZipCodes() {

		logger.info("Calculating Airport Model TAZ Zip Code Array");

		zipCodes = new int[tazManager.maxTaz + 1];

		String directory = Util.getStringValueFromPropertyMap(rbMap,
				"Project.Directory");
		String fileName = directory
				+ Util.getStringValueFromPropertyMap(rbMap, "mgra.socec.file");

		logger.info("Begin reading the data in file " + fileName);
		TableDataSet mgraTable;

		try {
			OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
			mgraTable = csvFile.readFile(new File(fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.info("End reading the data in file " + fileName);

		// iterate through TAZs and store zip codes in zipCodes array
		int[] tazs = tazManager.getTazs();
		for (int i = 0; i < tazs.length; ++i) {

			int tazNumber = tazs[i];
			int maxPop = 0;
			int zip = 0;
			for (int row = 1; row <= mgraTable.getRowCount(); ++row) {

				if (mgraTable.getValueAt(row, "taz") == tazNumber) {
					int pop = (int) mgraTable.getValueAt(row, "pop");
					if (pop > maxPop) {
						maxPop = pop;
						zip = (int) mgraTable.getValueAt(row, "ZIP09");
					}
				}
			}
			// if iterate through mgra data, and no mgras with pop found, then
			// choose zip of first mgra in taz
			if (zip == 0) {
				for (int row = 1; row <= mgraTable.getRowCount(); ++row) {

					if (mgraTable.getValueAt(row, "taz") == tazNumber) {
						zip = (int) mgraTable.getValueAt(row, "ZIP09");
						break;
					}
				}
			}
			// store zip in array
			zipCodes[tazNumber] = zip;
		}
		logger.info("Finished Calculating Airport Model TAZ Zip Code Array");
	}

	/**
	 * Calculate taz probabilities. This method initializes and calculates the
	 * tazProbabilities array.
	 */
	public void calculateTazProbabilities(AirportDmuFactoryIf dmuFactory) {

		if (sizeTerms == null) {
			logger.error("Error:  attemping to execute airportmodel.calculateTazProbabilities() before calling calculateMgraProbabilities()");
			throw new RuntimeException();
		}

		logger.info("Calculating Airport Model TAZ Probabilities Arrays");

		// initialize taz probabilities array
		int segments = sizeTerms.length;
		int maxTaz = tazManager.getMaxTaz();
		tazProbabilities = new double[segments][maxTaz + 1];

		// Note: this is an aggregate model to calculate utilities, but we need
		// income so we need a party object
		AirportParty airportParty = new AirportParty(1001);

		AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
		dmu.setZips(zipCodes);
		dmu.setSizeTerms(sizeTerms);
		dmu.setAirportParty(airportParty);

		int airportTaz = mgraManager.getTaz(airportMgra);

		// segments are combinations of 4 purposes and 8 income groups, which
		// apply only to resident purposes
		for (int segment = 0; segment < segments; ++segment) {

			int purpose = AirportModelStructure
					.getPurposeFromDCSizeSegment(segment);
			int income = AirportModelStructure
					.getIncomeFromDCSizeSegment(segment);

			airportParty.setPurpose((byte) purpose);
			airportParty.setIncome((byte) income);

			// set airport taz as origin. Destination tazs controlled by
			// alternative file.
			IndexValues dmuIndex = dmu.getDmuIndex();
			dmuIndex.setOriginZone(airportTaz);

			// Calculate utilities & probabilities
			destModel[purpose].computeUtilities(dmu, dmuIndex);

			// Store probabilities (by segment)
			tazProbabilities[segment] = Arrays.copyOf(
					destModel[purpose].getCumulativeProbabilities(),
					destModel[purpose].getCumulativeProbabilities().length);
		}
		logger.info("Finished Calculating Airport Model TAZ Probabilities Arrays");
	}

	/**
	 * Choose an MGRA
	 * 
	 * @param purpose
	 *            Purpose
	 * @param income
	 *            Income
	 * @param randomNumber
	 *            Random number
	 * @return The chosen MGRA number
	 */
	public int chooseMGRA(int purpose, int income, double randomNumber) {

		// first find a TAZ
		int segment = AirportModelStructure.getDCSizeSegment(purpose, income);
		int alt = 0;
		double[] tazCumProb = tazProbabilities[segment];
		double altProb = 0;
		double cumProb = 0;
		for (int i = 0; i < tazCumProb.length; ++i) {
			if (tazCumProb[i] > randomNumber) {
				alt = i;
				if (i != 0) {
					cumProb = tazCumProb[i - 1];
					altProb = tazCumProb[i] - tazCumProb[i - 1];
				} else {
					altProb = tazCumProb[i];
				}
				break;
			}
		}

		// get the taz number of the alternative, and an array of mgras in that
		// taz
		int tazNumber = (int) alternativeData.getValueAt(alt + 1, "dest");
		int[] mgraArray = tazManager.getMgraArray(tazNumber);

		// now find an MGRA in the taz corresponding to the random number drawn:
		// note that the indexing needs to be offset by the cumulative
		// probability of the chosen taz and the
		// mgra probabilities need to be scaled by the alternatives probability
		int mgraNumber = 0;
		double[] mgraCumProb = mgraProbabilities[segment][tazNumber];
		for (int i = 0; i < mgraCumProb.length; ++i) {
			cumProb += mgraCumProb[i] * altProb;
			if (cumProb > randomNumber) {
				mgraNumber = mgraArray[i];
			}
		}
		// return the chosen MGRA number
		return mgraNumber;
	}

	/**
	 * Iterate through an array of AirportParty objects, choosing origin MGRAs
	 * for each and setting the result back in the objects.
	 * 
	 * @param airportParties
	 *            An array of AirportParty objects
	 */
	public void chooseOrigins(AirportParty[] airportParties) {

		// iterate through the array, choosing mgras and setting them
		for (AirportParty party : airportParties) {

			int income = party.getIncome();
			int purpose = party.getPurpose();
			double random = party.getRandom();
			int mgra = -99;
			if (purpose < AirportModelStructure.INTERNAL_PURPOSES)
				mgra = chooseMGRA(purpose, income, random);

			// if this is a departing travel party, the origin is the chosen
			// mgra, and the destination is the airport terminal
			if (party.getDirection() == AirportModelStructure.DEPARTURE) {
				party.setOriginMGRA(mgra);
				party.setDestinationMGRA(airportMgra);
			} else {
				party.setOriginMGRA(airportMgra);
				party.setDestinationMGRA(mgra);
			}

		}
	}

}
