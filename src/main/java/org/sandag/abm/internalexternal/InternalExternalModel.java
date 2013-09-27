package org.sandag.abm.internalexternal;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.crossborder.CrossBorderModel;
import org.sandag.abm.crossborder.CrossBorderStop;
import org.sandag.abm.crossborder.CrossBorderStopLocationChoiceModel;
import org.sandag.abm.crossborder.CrossBorderTrip;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

public class InternalExternalModel {

	public static final int MATRIX_DATA_SERVER_PORT = 1171;
	public static final int MATRIX_DATA_SERVER_PORT_OFFSET = 0;

	private MatrixDataServerRmi ms;
	private static Logger logger = Logger.getLogger("internalExternalModel");
	private HashMap<String, String> rbMap;
	private McLogsumsCalculator logsumsCalculator;
	private AutoTazSkimsCalculator tazDistanceCalculator;
	private MgraDataManager mgraManager;
	private TazDataManager tazManager;

	private boolean seek;
	private int traceId;

	private int iteration = 1;

	/**
	 * Constructor
	 * 
	 * @param rbMap
	 */
	public InternalExternalModel(HashMap<String, String> rbMap) {
		this.rbMap = rbMap;
		mgraManager = MgraDataManager.getInstance(rbMap);
		tazManager = TazDataManager.getInstance(rbMap);
		seek = new Boolean(Util.getStringValueFromPropertyMap(rbMap,
				"internalExternal.seek"));
		traceId = new Integer(Util.getStringValueFromPropertyMap(rbMap,
				"internalExternal.trace"));

	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	/**
	 * Run InternalExternal model.
	 */
	public void runModel() {

		InternalExternalModelStructure modelStructure = new InternalExternalModelStructure();

		InternalExternalDmuFactoryIf dmuFactory = new InternalExternalDmuFactory(
				modelStructure);

		InternalExternalTourManager tourManager = new InternalExternalTourManager(
				rbMap, iteration);

		tourManager.generateTours();

		InternalExternalTour[] tours = tourManager.getTours();

		tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
		tazDistanceCalculator.computeTazDistanceArrays();
		logsumsCalculator = new McLogsumsCalculator();
		logsumsCalculator.setupSkimCalculators(rbMap);
		logsumsCalculator.setTazDistanceSkimArrays(
				tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
				tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

		InternalExternalTourTimeOfDayChoiceModel todChoiceModel = new InternalExternalTourTimeOfDayChoiceModel(
				rbMap);
		InternalExternalTourDestChoiceModel destChoiceModel = new InternalExternalTourDestChoiceModel(
				rbMap, modelStructure, dmuFactory);
		destChoiceModel.calculateTazProbabilities(dmuFactory);

		InternalExternalTripModeChoiceModel tripModeChoiceModel = new InternalExternalTripModeChoiceModel(
				rbMap, modelStructure, dmuFactory, logsumsCalculator);

		// Run models for array of tours
		for (int i = 0; i < tours.length; ++i) {

			InternalExternalTour tour = tours[i];

			if (i < 10 || i % 1000 == 0)
				logger.info("Processing tour " + i);

			if (seek && tour.getID() != traceId)
				continue;

			if (tour.getID() == traceId)
				tour.setDebugChoiceModels(true);

			todChoiceModel.calculateTourTOD(tour);
			destChoiceModel.chooseDestination(tour);

			// generate trips and choose mode for them - note this assumes two
			// trips per tour
			InternalExternalTrip[] trips = new InternalExternalTrip[2];
			int tripNumber = 0;

			// generate an outbound trip from the tour origin to the destination
			// and choose a mode
			trips[tripNumber] = new InternalExternalTrip(tour, true,
					mgraManager);
			tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
			++tripNumber;

			// generate an inbound trip from the tour destination to the origin
			// and choose a mode
			trips[tripNumber] = new InternalExternalTrip(tour, false,
					mgraManager);
			tripModeChoiceModel.chooseMode(tour, trips[tripNumber]);
			++tripNumber;

			// set the trips in the tour object
			tour.setTrips(trips);

		}

		tourManager.writeOutputFile(rbMap);

		logger.info("Internal-External Model successfully completed!");

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
	 * @param args
	 */
	public static void main(String[] args) {

		String propertiesFile = null;
		HashMap<String, String> pMap;

		logger.info(String.format(
				"SANDAG Activity Based Model using CT-RAMP version %s",
				CtrampApplication.VERSION));

		logger.info(String.format("Running InternalExternal Model"));

		if (args.length == 0) {
			logger.error(String
					.format("no properties file base name (without .properties extension) was specified as an argument."));
			return;
		} else
			propertiesFile = args[0];

		pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);

		// sampleRate is not relevant for internal-external model, since
		// sampling
		// would have been applied in CT-RAMP model
		int iteration = 1;

		for (int i = 1; i < args.length; ++i) {
			if (args[i].equalsIgnoreCase("-iteration")) {
				iteration = Integer.parseInt(args[i + 1]);
			}
		}

		logger.info("-iteration  " + iteration);
		InternalExternalModel internalExternalModel = new InternalExternalModel(
				pMap);
		internalExternalModel.setIteration(iteration);

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
					matrixServer = internalExternalModel
							.startMatrixServerProcess(matrixServerAddress,
									serverPort, mt);
					internalExternalModel.ms = matrixServer;
				} else {
					internalExternalModel.ms = new MatrixDataServerRmi(
							matrixServerAddress, serverPort,
							MatrixDataServer.MATRIX_DATA_SERVER_NAME);
					internalExternalModel.ms
							.testRemote("InternalExternalModel");
					internalExternalModel.ms.start32BitMatrixIoServer(mt,
							"InternalExternalModel");

					// these methods need to be called to set the matrix data
					// manager in the matrix data server
					MatrixDataManager mdm = MatrixDataManager.getInstance();
					mdm.setMatrixDataServerObject(internalExternalModel.ms);
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

		internalExternalModel.runModel();

		// if a separate process for running matrix data mnager was started,
		// we're
		// done with it, so close it.
		if (matrixServerAddress.equalsIgnoreCase("localhost")) {
			matrixServer.stop32BitMatrixIoServer();
		} else {
			if (!matrixServerAddress.equalsIgnoreCase("none"))
				internalExternalModel.ms
						.stop32BitMatrixIoServer("InternalExternalModel");
		}

	}

}
