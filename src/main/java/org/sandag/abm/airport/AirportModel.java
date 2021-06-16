package org.sandag.abm.airport;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagTourBasedModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

public class AirportModel
{

    public static final int         MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET = 0;

    private MatrixDataServerRmi     ms;
    private static Logger           logger                         = Logger.getLogger(SandagTourBasedModel.class);
    private HashMap<String, String> rbMap;
    private static float sampleRate;
    private static int iteration;

	/**
     * Constructor
     * 
     * @param rbMap
     */
    public AirportModel(HashMap<String, String> rbMap, float aSampleRate)
    {
        this.rbMap = rbMap;
        this.sampleRate=aSampleRate;
    }

    /**
     * Run airport model.
     */
    public void runModel()
    {
        AirportDmuFactory dmuFactory = new AirportDmuFactory();

        AirportPartyManager apm = new AirportPartyManager(rbMap, sampleRate);

        apm.generateAirportParties();
        AirportParty[] parties = apm.getParties();

        AirportDestChoiceModel destChoiceModel = new AirportDestChoiceModel(rbMap, dmuFactory);
        destChoiceModel.calculateMgraProbabilities(dmuFactory);
        destChoiceModel.calculateTazProbabilities(dmuFactory);
        destChoiceModel.chooseOrigins(parties);

        AirportModeChoiceModel modeChoiceModel = new AirportModeChoiceModel(rbMap, dmuFactory);
        modeChoiceModel.initializeBestPathCalculators();
        modeChoiceModel.chooseModes(rbMap, parties, dmuFactory);

        apm.writeOutputFile(rbMap);

        logger.info("Airport Model successfully completed!");

    }

    private MatrixDataServerRmi startMatrixServerProcess(String serverAddress, int serverPort,
            MatrixType mt)
    {

        String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

        MatrixDataServerRmi matrixServer = new MatrixDataServerRmi(serverAddress, serverPort,
                MatrixDataServer.MATRIX_DATA_SERVER_NAME);

        try
        {
            // create the concrete data server object
            matrixServer.start32BitMatrixIoServer(mt);
        } catch (RuntimeException e)
        {
            matrixServer.stop32BitMatrixIoServer();
            logger.error(
                    "RuntimeException caught making remote method call to start 32 bit mitrix in remote MatrixDataServer.",
                    e);
        }

        // bind this concrete object with the cajo library objects for managing
        // RMI
        try
        {
            Remote.config(serverAddress, serverPort, null, 0);
        } catch (Exception e)
        {
            logger.error(String.format(
                    "UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.",
                    serverAddress, serverPort), e);
            matrixServer.stop32BitMatrixIoServer();
            throw new RuntimeException();
        }

        try
        {
            ItemServer.bind(matrixServer, className);
        } catch (RemoteException e)
        {
            logger.error(String.format(
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
    public static void main(String[] args)
    {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running Airport Model"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else {
        	propertiesFile = args[0];

	        for (int i = 1; i < args.length; ++i)
	        {
	            if (args[i].equalsIgnoreCase("-sampleRate"))
	            {
	                sampleRate = Float.parseFloat(args[i + 1]);
	            }
	            if (args[i].equalsIgnoreCase("-iteration"))
	            {
	                iteration = Integer.parseInt(args[i + 1]);
	            }
	        }
        }
        
        logger.info("Airport Model:"+String.format("-sampleRate %.4f.", sampleRate)+"-iteration  " + iteration);

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        AirportModel airportModel = new AirportModel(pMap, sampleRate);

        String matrixServerAddress = "";
        int serverPort = 0;
        try
        {
            // get matrix server address. if "none" is specified, no server will
            // be
            // started, and matrix io will ocurr within the current process.
            matrixServerAddress = Util.getStringValueFromPropertyMap(pMap,
                    "RunModel.MatrixServerAddress");
            try
            {
                // get matrix server port.
                serverPort = Util.getIntegerValueFromPropertyMap(pMap, "RunModel.MatrixServerPort");
            } catch (MissingResourceException e)
            {
                // if no matrix server address entry is found, leave undefined
                // --
                // it's eithe not needed or show could create an error.
            }
        } catch (MissingResourceException e)
        {
            // if no matrix server address entry is found, set to localhost, and
            // a
            // separate matrix io process will be started on localhost.
            matrixServerAddress = "localhost";
            serverPort = MATRIX_DATA_SERVER_PORT;
        }

        MatrixDataServerRmi matrixServer = null;
        String matrixTypeName = Util.getStringValueFromPropertyMap(pMap, "Results.MatrixType");
        MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

        try
        {

            if (!matrixServerAddress.equalsIgnoreCase("none"))
            {

                if (matrixServerAddress.equalsIgnoreCase("localhost"))
                {
                    matrixServer = airportModel.startMatrixServerProcess(matrixServerAddress,
                            serverPort, mt);
                    airportModel.ms = matrixServer;
                } else
                {
                    airportModel.ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    airportModel.ms.testRemote("AirportModel");
                    airportModel.ms.start32BitMatrixIoServer(mt, "AirportModel");

                    // these methods need to be called to set the matrix data
                    // manager in the matrix data server
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(airportModel.ms);
                }

            }

        } catch (Exception e)
        {

            if (matrixServerAddress.equalsIgnoreCase("localhost"))
            {
                matrixServer.stop32BitMatrixIoServer();
            }
            logger.error(
                    String.format("exception caught running ctramp model components -- exiting."),
                    e);
            throw new RuntimeException();

        }

        airportModel.runModel();

        // if a separate process for running matrix data mnager was started,
        // we're
        // done with it, so close it.
        if (matrixServerAddress.equalsIgnoreCase("localhost"))
        {
            matrixServer.stop32BitMatrixIoServer();
        } else
        {
            if (!matrixServerAddress.equalsIgnoreCase("none"))
                airportModel.ms.stop32BitMatrixIoServer("AirportModel");
        }

    }

}
