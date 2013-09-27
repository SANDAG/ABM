package org.sandag.abm.visitor;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.crossborder.CrossBorderModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

public class VisitorModel {

	public static final int MATRIX_DATA_SERVER_PORT = 1171;
	public static final int MATRIX_DATA_SERVER_PORT_OFFSET = 0;

	private MatrixDataServerRmi ms;
	private static Logger logger = Logger.getLogger("visitorModel");
	private HashMap<String, String> rbMap;
	private McLogsumsCalculator logsumsCalculator;
	private AutoTazSkimsCalculator tazDistanceCalculator;
	private MgraDataManager mgraManager;
	private TazDataManager tazManager;

	private boolean seek;
	private int traceId;
	private double sampleRate = 1;

	/**
	 * Constructor
	 * 
	 * @param rbMap
	 */
	public VisitorModel(HashMap<String, String> rbMap) {
		this.rbMap = rbMap;
		mgraManager = MgraDataManager.getInstance(rbMap);
		tazManager = TazDataManager.getInstance(rbMap);

		seek = new Boolean(Util.getStringValueFromPropertyMap(rbMap,
				"visitor.seek"));
		traceId = new Integer(Util.getStringValueFromPropertyMap(rbMap,
				"visitor.trace"));

	}

	/**
	 * Run visitor model.
	 */
	public void runModel() {

		VisitorModelStructure modelStructure = new VisitorModelStructure();

		VisitorDmuFactoryIf dmuFactory = new VisitorDmuFactory(modelStructure);

		VisitorTourManager tourManager = new VisitorTourManager(rbMap);

		tourManager.generateVisitorTours();
		VisitorTour[] tours = tourManager.getTours();

		tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
		tazDistanceCalculator.computeTazDistanceArrays();
		logsumsCalculator = new McLogsumsCalculator();
		logsumsCalculator.setupSkimCalculators(rbMap);
		logsumsCalculator.setTazDistanceSkimArrays(
				tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
				tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

		VisitorTourTimeOfDayChoiceModel todChoiceModel = new VisitorTourTimeOfDayChoiceModel(
				rbMap);
		VisitorTourDestChoiceModel destChoiceModel = new VisitorTourDestChoiceModel(
				rbMap, modelStructure, dmuFactory, logsumsCalculator);
		VisitorTourModeChoiceModel tourModeChoiceModel = destChoiceModel
				.getTourModeChoiceModel();
		destChoiceModel.calculateSizeTerms(dmuFactory);
		destChoiceModel.calculateTazProbabilities(dmuFactory);

		VisitorStopFrequencyModel stopFrequencyModel = new VisitorStopFrequencyModel(
				rbMap);
		VisitorStopPurposeModel stopPurposeModel = new VisitorStopPurposeModel(
				rbMap);
		VisitorStopTimeOfDayChoiceModel stopTodChoiceModel = new VisitorStopTimeOfDayChoiceModel(
				rbMap);
		VisitorStopLocationChoiceModel stopLocationChoiceModel = new VisitorStopLocationChoiceModel(
				rbMap, modelStructure, dmuFactory, logsumsCalculator);
		VisitorTripModeChoiceModel tripModeChoiceModel = new VisitorTripModeChoiceModel(
				rbMap, modelStructure, dmuFactory, logsumsCalculator);
		double[][] mgraSizeTerms = destChoiceModel.getMgraSizeTerms();
		double[][] tazSizeTerms = destChoiceModel.getTazSizeTerms();
		double[][][] mgraProbabilities = destChoiceModel.getMgraProbabilities();
		stopLocationChoiceModel.setMgraSizeTerms(mgraSizeTerms);
		stopLocationChoiceModel.setTazSizeTerms(tazSizeTerms);
		stopLocationChoiceModel.setMgraProbabilities(mgraProbabilities);
		stopLocationChoiceModel.setTripModeChoiceModel(tripModeChoiceModel);

		// Run models for array of tours
		for (int i = 0; i < tours.length; ++i) {

			VisitorTour tour = tours[i];

			// sample tours
			double rand = tour.getRandom();
			if (rand > sampleRate)
				continue;

			if (i < 10 || i % 1000 == 0)
				logger.info("Processing tour " + (i + 1));

			if (seek && tour.getID() != traceId)
				continue;

			if (tour.getID() == traceId)
				tour.setDebugChoiceModels(true);
			todChoiceModel.calculateTourTOD(tour);
			destChoiceModel.chooseDestination(tour);
			tourModeChoiceModel.chooseTourMode(tour);

			stopFrequencyModel.calculateStopFrequency(tour);
			stopPurposeModel.calculateStopPurposes(tour);

			int outboundStops = tour.getNumberOutboundStops();
			int inboundStops = tour.getNumberInboundStops();

			// choose TOD for stops and location of each
			if (outboundStops > 0) {
				VisitorStop[] stops = tour.getOutboundStops();
				for (int j = 0; j < stops.length; ++j) {
					stopTodChoiceModel.chooseTOD(tour, stops[j]);
					stopLocationChoiceModel.chooseStopLocation(tour, stops[j]);
				}
			}
			if (inboundStops > 0) {
				VisitorStop[] stops = tour.getInboundStops();
				for (int j = 0; j < stops.length; ++j) {
					stopTodChoiceModel.chooseTOD(tour, stops[j]);
					stopLocationChoiceModel.chooseStopLocation(tour, stops[j]);
				}
			}

			// generate trips and choose mode for them
			VisitorTrip[] trips = new VisitorTrip[outboundStops + inboundStops
					+ 2];
			int tripNumber = 0;

			// outbound stops
			if (outboundStops > 0) {
				VisitorStop[] stops = tour.getOutboundStops();
				for (int j = 0; j < stops.length; ++j) {
					// generate a trip to the stop and choose a mode for it
					trips[tripNumber] = new VisitorTrip(tour, stops[j], true);
					tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
					++tripNumber;
				}
				// generate a trip from the last stop to the tour destination
				trips[tripNumber] = new VisitorTrip(tour,
						stops[stops.length - 1], false);
				tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
				++tripNumber;

			} else {
				// generate an outbound trip from the tour origin to the
				// destination and choose a mode
				trips[tripNumber] = new VisitorTrip(tour, true);
				tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
				++tripNumber;
			}

			// inbound stops
			if (inboundStops > 0) {
				VisitorStop[] stops = tour.getInboundStops();
				for (int j = 0; j < stops.length; ++j) {
					// generate a trip to the stop and choose a mode for it
					trips[tripNumber] = new VisitorTrip(tour, stops[j], true);
					tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
					++tripNumber;
				}
				// generate a trip from the last stop to the tour origin
				trips[tripNumber] = new VisitorTrip(tour,
						stops[stops.length - 1], false);
				tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
				++tripNumber;
			} else {

				// generate an inbound trip from the tour destination to the
				// origin and choose a mode
				trips[tripNumber] = new VisitorTrip(tour, false);
				tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
				++tripNumber;
			}

			// set the trips in the tour object
			tour.setTrips(trips);

		}

		tourManager.writeOutputFile(rbMap);

		logger.info("Visitor Model successfully completed!");

	}

	private MatrixDataServerRmi startMatrixServerProcess(String serverAddress,
			int serverPort, MatrixType mt) {

		String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

		MatrixDataServerRmi matrixServer = new MatrixDataServerRmi(
				serverAddress, serverPort,
				MatrixDataServer.MATRIX_DATA_SERVER_NAME);

		try {
			// create the concrete data server object
			matrixServer.start32BitMatrixIoServer(mt);
		} catch (RuntimeException e) {
			matrixServer.stop32BitMatrixIoServer();
			logger.error(
					"RuntimeException caught making remote method call to start 32 bit mitrix in remote MatrixDataServer.",
					e);
		}

		// bind this concrete object with the cajo library objects for managing
		// RMI
		try {
			Remote.config(serverAddress, serverPort, null, 0);
		} catch (Exception e) {
			logger.error(
					String.format(
							"UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.",
							serverAddress, serverPort), e);
			matrixServer.stop32BitMatrixIoServer();
			throw new RuntimeException();
		}

		try {
			ItemServer.bind(matrixServer, className);
		} catch (RemoteException e) {
			logger.error(
					String.format(
							"RemoteException. serverAddress = %s, serverPort = %d -- exiting.",
							serverAddress, serverPort), e);
			matrixServer.stop32BitMatrixIoServer();
			throw new RuntimeException();
		}

		return matrixServer;

	}

	/**
	 * @return the sampleRate
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	/**
	 * @param sampleRate
	 *            the sampleRate to set
	 */
	public void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String propertiesFile = null;
		HashMap<String, String> pMap;

		logger.info(String.format(
				"SANDAG Activity Based Model using CT-RAMP version %s",
				CtrampApplication.VERSION));

		logger.info(String.format("Running Visitor Model"));

		if (args.length == 0) {
			logger.error(String
					.format("no properties file base name (without .properties extension) was specified as an argument."));
			return;
		} else
			propertiesFile = args[0];

		pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
		VisitorModel visitorModel = new VisitorModel(pMap);

		float sampleRate = 1.0f;
		for (int i = 1; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-sampleRate")) {
				sampleRate = Float.parseFloat(args[i + 1]);
			}
		}
		logger.info(String.format("-sampleRate %.4f.", sampleRate));
		visitorModel.setSampleRate(sampleRate);

		String matrixServerAddress = "";
		int serverPort = 0;
		try {
			// get matrix server address. if "none" is specified, no server will
			// be
			// started, and matrix io will ocurr within the current process.
			matrixServerAddress = Util.getStringValueFromPropertyMap(pMap,
					"RunModel.MatrixServerAddress");
			try {
				// get matrix server port.
				serverPort = Util.getIntegerValueFromPropertyMap(pMap,
						"RunModel.MatrixServerPort");
			} catch (MissingResourceException e) {
				// if no matrix server address entry is found, leave undefined
				// --
				// it's eithe not needed or show could create an error.
			}
		} catch (MissingResourceException e) {
			// if no matrix server address entry is found, set to localhost, and
			// a
			// separate matrix io process will be started on localhost.
			matrixServerAddress = "localhost";
			serverPort = MATRIX_DATA_SERVER_PORT;
		}

		MatrixDataServerRmi matrixServer = null;
		String matrixTypeName = Util.getStringValueFromPropertyMap(pMap,
				"Results.MatrixType");
		MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

		try {

			if (!matrixServerAddress.equalsIgnoreCase("none")) {

				if (matrixServerAddress.equalsIgnoreCase("localhost")) {
					matrixServer = visitorModel.startMatrixServerProcess(
							matrixServerAddress, serverPort, mt);
					visitorModel.ms = matrixServer;
				} else {
					visitorModel.ms = new MatrixDataServerRmi(
							matrixServerAddress, serverPort,
							MatrixDataServer.MATRIX_DATA_SERVER_NAME);
					visitorModel.ms.testRemote("VisitorModel");
					visitorModel.ms
							.start32BitMatrixIoServer(mt, "VisitorModel");

					// these methods need to be called to set the matrix data
					// manager in the matrix data server
					MatrixDataManager mdm = MatrixDataManager.getInstance();
					mdm.setMatrixDataServerObject(visitorModel.ms);
				}

			}

		} catch (Exception e) {

			if (matrixServerAddress.equalsIgnoreCase("localhost")) {
				matrixServer.stop32BitMatrixIoServer();
			}
			logger.error(
					String.format("exception caught running ctramp model components -- exiting."),
					e);
			throw new RuntimeException();

		}

		visitorModel.runModel();

		// if a separate process for running matrix data mnager was started,
		// we're
		// done with it, so close it.
		if (matrixServerAddress.equalsIgnoreCase("localhost")) {
			matrixServer.stop32BitMatrixIoServer();
		} else {
			if (!matrixServerAddress.equalsIgnoreCase("none"))
				visitorModel.ms.stop32BitMatrixIoServer("VisitorModel");
		}

	}

}
