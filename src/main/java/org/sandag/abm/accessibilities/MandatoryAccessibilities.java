package org.sandag.abm.accessibilities;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixType;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.InTextFile;
import com.pb.common.util.OutTextFile;
import com.pb.common.util.ResourceUtil;

public class MandatoryAccessibilities
{

    protected transient Logger          logger          = Logger.getLogger(MandatoryAccessibilities.class);

    private TableDataSet                surveyData;

    private UtilityExpressionCalculator autoSkimUEC;
    private UtilityExpressionCalculator bestWalkTransitUEC;
    private UtilityExpressionCalculator bestDriveTransitUEC;
    private UtilityExpressionCalculator autoLogsumUEC;
    private UtilityExpressionCalculator transitLogsumUEC;
    private UtilityExpressionCalculator constantsUEC;

    private MandatoryAccessibilitiesDMU dmu;
    private IndexValues                 iv;

    private NonTransitUtilities         ntUtilities;
    private TransitUtilities            transitUtilities;

    private MgraDataManager             mgraManager;
    private TapDataManager              tapManager;
    private TazDataManager              tazManager;

    // auto sufficiency (0 autos, autos<adults, autos>=adults),
    // and mode (SOV,HOV,Transit,Non-Motorized)
    private double[][]                  expConstants;
    private static final int            MARKET_SEGMENTS = 3;

    private boolean                     os64bit;
    private MatrixDataServer            matrixServer;

    private String[]                    accNames        = {"SovTime", // 0
            "SovDist", // 1
            "WTTime", // 2
            "DTTime", // 3
            "SovUtility", // 4
            "WTUtility", // 5
            "AutoLogsum", // 6
            "WTLogsum", // 7
            "TransitLogsum", // 8
            "WTRailShare", // 9
            "DTRailShare", // 10
            "DTLogsum", // 11
            "HovUtility" // 12
                                                        };

    private String[]                    purposes        = {"work", "school"};

    /**
     * Constructor.
     * 
     * @param rb
     *            ResourceBundle with appropriate properties set.
     */
    public MandatoryAccessibilities(HashMap<String, String> rbMap)
    {

        // Create the UECs
        String uecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.mandatory.uec.file");
        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.mandatory.data.page");
        int autoSkimPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.mandatory.auto.page");
        int bestWalkTransitPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.bestWalkTransit.page");
        int bestDriveTransitPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.bestDriveTransit.page");
        int autoLogsumPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.autoLogsum.page");
        int transitLogsumPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "acc.mandatory.transitLogsum.page");
        int constantsPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.constants.page");

        dmu = new MandatoryAccessibilitiesDMU();

        File uecFile = new File(uecFileName);
        autoSkimUEC = new UtilityExpressionCalculator(uecFile, autoSkimPage, dataPage, rbMap, dmu);
        bestWalkTransitUEC = new UtilityExpressionCalculator(uecFile, bestWalkTransitPage,
                dataPage, rbMap, dmu);
        bestDriveTransitUEC = new UtilityExpressionCalculator(uecFile, bestDriveTransitPage,
                dataPage, rbMap, dmu);
        autoLogsumUEC = new UtilityExpressionCalculator(uecFile, autoLogsumPage, dataPage, rbMap,
                dmu);
        transitLogsumUEC = new UtilityExpressionCalculator(uecFile, transitLogsumPage, dataPage,
                rbMap, dmu);

        String accUECFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.uec.file");
        File accUECFile = new File(accUECFileName);
        int accDataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.data.page");
        constantsUEC = new UtilityExpressionCalculator(accUECFile, constantsPage, accDataPage,
                rbMap, dmu);

        mgraManager = MgraDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);
        tazManager = TazDataManager.getInstance(rbMap);

        iv = new IndexValues();
    }

    /**
     * Build utility components for SOV,HOV,Walk, and Transit modes.
     * 
     * @param rb
     *            Resourcebundle with appropriate keys.
     */
    public void buildAccessibilityComponents(HashMap<String, String> rbMap)
    {

        double[][][] sovExpUtilities = null;
        double[][][] hovExpUtilities = null;
        double[][][] nMotorExpUtilities = null;
        ntUtilities = new NonTransitUtilities(rbMap, sovExpUtilities, hovExpUtilities,
                nMotorExpUtilities);
        ntUtilities.buildUtilities();
        transitUtilities = new TransitUtilities(rbMap);
        transitUtilities.calculateUtilityComponents();

    }

    /**
     * Read household survey data.
     * 
     * @param rb
     *            ResourceBundle with survey.file property set.
     */
    public void readData(HashMap<String, String> rbMap)
    {

        File surveyDataFile = new File(Util.getStringValueFromPropertyMap(rbMap,
                "survey.input.file"));
        OLD_CSVFileReader csvReader = new OLD_CSVFileReader();

        try
        {
            surveyData = csvReader.readFile(surveyDataFile, true);
        } catch (IOException e)
        {
            logger.fatal("Error: Trying to read survey data file " + surveyDataFile);
            throw new RuntimeException(e);
        }
    }

    /**
     * This method calculates work location choice logsums for a work location
     * choice estimation file, which contains records for workers, including
     * chosen and unchosen (sampled) work mgras.
     * 
     * @param rb
     */
    public void calculateWorkLogsums(ResourceBundle rb)
    {

        String inFileName = rb.getString("worklocationchoice.input.file");
        String outFileName = rb.getString("worklocationchoice.output.file");

        logger.info("Reading and calculating work location choice logsums from " + inFileName);
        int numberOfFields = 255; // number of fields on input file
        int hhMgraField = 7; // number of field with household MGRA (starting at
                             // 1)
        int chosenMgraField = 10; // number of actual work MGRA
        int carSuffField = 15; // number of field with car sufficiency for
                               // household
        int startSampledMgraField = 16; // number of field that starts listing
        // sampled MGRAs
        int numberSampledMgras = 40; // number of sampled mgras (assumed
                                     // consecutive
        // starting at startSampledMgraField

        String[] fields = new String[numberOfFields];
        InTextFile surveyFile = new InTextFile();
        surveyFile.open(inFileName);

        // open output file
        OutTextFile outTextFile = new OutTextFile();
        PrintWriter outFile = outTextFile.open(outFileName);

        String inLine = new String();
        int linesRead = 0;

        try
        {
            while ((inLine = surveyFile.readLine()) != null)
            {

                StringTokenizer inToken = new StringTokenizer(inLine, ",");

                if (!inToken.hasMoreTokens()) continue;

                for (int i = 0; i < fields.length; ++i)
                    fields[i] = inToken.nextToken();

                ++linesRead;

                // skip header row
                char c = fields[0].charAt(0);
                if (new Character(c).isLetter(c))
                {

                    // print the header line to the output file
                    outFile.print(inLine);
                    outFile.print(",chosenLS");
                    for (int i = 0; i < numberSampledMgras; ++i)
                        outFile.print(",LS" + (i + 1));
                    outFile.print("\n");

                    logger.info("Skipping header row");
                    continue;
                }
                outFile.print(inLine);

                if (linesRead < 10 || linesRead % 100 == 0)
                {
                    logger.info("Processing line: " + linesRead);
                }

                // calculate the accessibilities
                int hhMgra = new Integer(fields[hhMgraField - 1]);
                int chosenMgra = new Integer(fields[chosenMgraField - 1]);
                int carSuff = new Integer(fields[carSuffField - 1]);

                double logsum = calculateWorkLogsum(hhMgra, chosenMgra, carSuff);
                outFile.print("," + String.format("%9.4f", logsum));
                outFile.flush();

                for (int i = 0; i < numberSampledMgras; ++i)
                {
                    int workMgraField = (startSampledMgraField - 1) + i;
                    int workMgra = new Integer(fields[workMgraField]);

                    logsum = calculateWorkLogsum(hhMgra, workMgra, carSuff);

                    outFile.print("," + String.format("%9.4f", logsum));
                }

                outFile.print("\n");
                outFile.flush();
            }
        } catch (Exception e)
        {
            logger.fatal("Error trying to read survey file " + inFileName + " line " + linesRead);
            throw new RuntimeException(e);
        }

    }

    /**
     * Calculate the work logsum for the household MGRA and sampled work
     * location MGRA.
     * 
     * @param hhMgra
     *            Household MGRA
     * @param workMgra
     *            Sampled work MGRA
     * @param autoSufficiency
     *            Auto sufficiency category
     * @return Work mode choice logsum
     */
    public double calculateWorkLogsum(int hhMgra, int workMgra, int autoSufficiency)
    {

        double[] accessibilities = calculateAccessibilitiesForMgraPair(hhMgra, workMgra);

        double sovUtility = accessibilities[4];
        double hovUtility = accessibilities[12];
        double transitLogsum = accessibilities[8]; // includes both walk and
                                                   // drive
        // access
        double nmExpUtility = ntUtilities.getNMotorExpUtility(hhMgra, workMgra, 0);

        // constrain auto sufficiency to 0,1,2
        autoSufficiency = Math.min(autoSufficiency, 2);

        double logsum = Math.log(Math.exp(sovUtility) * expConstants[autoSufficiency][0]
                + Math.exp(hovUtility) * expConstants[autoSufficiency][1] + Math.exp(transitLogsum)
                * expConstants[autoSufficiency][2] + nmExpUtility
                * expConstants[autoSufficiency][3]);

        return logsum;
    }

    public void calculateMandatoryAccessibilities(HashMap<String, String> rbMap)
    {

        if (surveyData == null) readData(rbMap);

        if (ntUtilities == null) buildAccessibilityComponents(rbMap);

        int records = surveyData.getRowCount();

        float[][] accessibilities = new float[accNames.length * purposes.length][records];

        // sampn perno hhaddr hhtaz hhmgra wadd wtaz wmgra sadd staz smgra

        for (int row = 1; row <= records; ++row)
        {

            int sampn = (int) surveyData.getValueAt(row, "sampn");
            int perno = (int) surveyData.getValueAt(row, "perno");

            int hhTaz = (int) surveyData.getValueAt(row, "hhtaz");
            int hhMgra = (int) surveyData.getValueAt(row, "hhmgra");
            int workTaz = (int) surveyData.getValueAt(row, "wtaz");
            int workMgra = (int) surveyData.getValueAt(row, "wmgra");
            int schoolTaz = (int) surveyData.getValueAt(row, "staz");
            int schoolMgra = (int) surveyData.getValueAt(row, "smgra");

            logger.info("Processing " + sampn);

            double[] recordAccessibilities = calculateAccessibilitiesForMgraPair(hhMgra, workMgra);

            for (int i = 0; i < recordAccessibilities.length; ++i)
                accessibilities[i][row - 1] = (float) recordAccessibilities[i];

            recordAccessibilities = calculateAccessibilitiesForMgraPair(hhMgra, schoolMgra);
            for (int i = 0; i < recordAccessibilities.length; ++i)
                accessibilities[i + accNames.length][row - 1] = (float) recordAccessibilities[i];

        } // end records

        // append columns to survey data and write it out
        String[] columnNames = new String[accNames.length * purposes.length];
        int col = 0;
        for (int p = 0; p < purposes.length; ++p)
            for (int a = 0; a < accNames.length; ++a)
            {
                columnNames[col] = purposes[p] + "_" + accNames[a];
                ++col;
            }

        for (col = 0; col < accNames.length * purposes.length; ++col)
            surveyData.appendColumn(accessibilities[col], columnNames[col]);

        File surveyDataFile = new File(Util.getStringValueFromPropertyMap(rbMap,
                "survey.output.file"));
        CSVFileWriter csvWriter = new CSVFileWriter();

        try
        {
            csvWriter.writeFile(surveyData, surveyDataFile);
        } catch (IOException e)
        {
            logger.fatal("Error: Trying to write survey data file " + surveyDataFile);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate the accessibilities for a given origin and destination mgra
     * 
     * @param oMgra
     *            The origin mgra
     * @param dMgra
     *            The destination mgra
     * @return An array of accessibilities
     */
    public double[] calculateAccessibilitiesForMgraPair(int oMgra, int dMgra)
    {

        double[] accessibilities = new double[accNames.length];

        if (oMgra > 0 && dMgra > 0)
        {

            int oTaz = mgraManager.getTaz(oMgra);
            int dTaz = mgraManager.getTaz(dMgra);

            iv.setOriginZone(oTaz);
            iv.setDestZone(dTaz);

            // sov time and distance
            double[] autoResults = autoSkimUEC.solve(iv, dmu, null);

            accessibilities[0] = autoResults[0];
            accessibilities[1] = autoResults[1];

            // auto logsum
            double pkSovExpUtility = ntUtilities.getSovExpUtility(oTaz, dTaz, 1);
            double pkHovExpUtility = ntUtilities.getHovExpUtility(oTaz, dTaz, 1);

            dmu.setSovNestLogsum(-999);
            if (pkSovExpUtility > 0)
            {
                dmu.setSovNestLogsum(Math.log(pkSovExpUtility));
                accessibilities[4] = dmu.getSovNestLogsum();
            }
            dmu.setHovNestLogsum(-999);
            if (pkHovExpUtility > 0)
            {
                dmu.setHovNestLogsum(Math.log(pkHovExpUtility));
                accessibilities[12] = dmu.getHovNestLogsum();
            }
            double[] autoLogsum = autoLogsumUEC.solve(iv, dmu, null);
            accessibilities[6] = autoLogsum[0];

            // walk transit

            // calculate the exp utilities, which will also calculate and store
            // best
            // mode
            double[] walkTransitExpUtilities = transitUtilities.calculateWalkTransitExpUtilities(
                    oMgra, dMgra, 1);

            // add up the exp utilities
            dmu.setWlkNestLogsum(-999f);
            double sumWlkExpUtilities = 0;
            for (int i = 0; i < walkTransitExpUtilities.length; ++i)
                sumWlkExpUtilities += walkTransitExpUtilities[i];

            if (sumWlkExpUtilities > 0) dmu.setWlkNestLogsum(Math.log(sumWlkExpUtilities));

            accessibilities[7] = dmu.getWlkNestLogsum();

            Modes.TransitMode bestMode = transitUtilities.getBestWalkTransitMode(1);

            if (bestMode != null)
            {
                int[] bestTaps = transitUtilities.getBestWalkTaps(bestMode, 1);
                dmu.setBestMode(bestMode.ordinal());
                int oTapPosition = mgraManager.getTapPosition(oMgra, bestTaps[0]);
                int dTapPosition = mgraManager.getTapPosition(dMgra, bestTaps[1]);

                if (oTapPosition == -1 || dTapPosition == -1)
                {
                    logger.fatal("Error:  Best walk mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but oTap pos "
                            + oTapPosition + " and dTap pos " + dTapPosition);
                    throw new RuntimeException();
                }

                if (walkTransitExpUtilities[bestMode.ordinal()] <= 0.0)
                {
                    logger.fatal("Error:  Best walk mode " + bestMode + " found for origin mgra "
                            + oMgra + " to destination mgra " + dMgra + " but exp Utility = "
                            + walkTransitExpUtilities[bestMode.ordinal()]);
                    throw new RuntimeException();
                }
                accessibilities[5] = Math.log(walkTransitExpUtilities[bestMode.ordinal()]);

                dmu.setMgraTapWalkTime(mgraManager.getMgraToTapWalkBoardTime(oMgra, oTapPosition));
                dmu.setTapMgraWalkTime(mgraManager.getMgraToTapWalkAlightTime(dMgra, dTapPosition));
                iv.setOriginZone(bestTaps[0]);
                iv.setDestZone(bestTaps[1]);
                double[] wlkTransitTimes = bestWalkTransitUEC.solve(iv, dmu, null);
                accessibilities[2] = wlkTransitTimes[0];
                accessibilities[9] = wlkTransitTimes[1];

            }

            // drive transit

            // calculate the exp utilities, which will also calculate and store
            // best
            // mode
            double[] driveTransitExpUtilities = transitUtilities.calculateDriveTransitExpUtilities(
                    oTaz, dMgra, 1);

            // add up the exp utilities
            dmu.setDrvNestLogsum(-999);
            double sumDrvExpUtilities = 0;
            for (int i = 0; i < driveTransitExpUtilities.length; ++i)
                sumDrvExpUtilities += driveTransitExpUtilities[i];
            if (sumDrvExpUtilities > 0) dmu.setDrvNestLogsum(Math.log(sumDrvExpUtilities));

            accessibilities[11] = dmu.getDrvNestLogsum();

            bestMode = transitUtilities.getBestDriveTransitMode(1);

            if (bestMode != null)
            {
                int[] bestTaps = transitUtilities.getBestDriveTaps(bestMode, 1);
                dmu.setBestMode(bestMode.ordinal());
                int oTapPosition = tazManager.getTapPosition(oTaz, bestTaps[0],
                        Modes.AccessMode.PARK_N_RIDE);
                int dTapPosition = mgraManager.getTapPosition(dMgra, bestTaps[1]);

                if (oTapPosition == -1 || dTapPosition == -1)
                {
                    logger.fatal("Error:  Best drive mode " + bestMode + " found for origin taz "
                            + oTaz + " to destination mgra " + dMgra + " but oTap pos "
                            + oTapPosition + " and dTap pos " + dTapPosition);
                    throw new RuntimeException();
                }

                dmu.setDriveTimeToTap(tazManager.getTapTime(oTaz, oTapPosition,
                        Modes.AccessMode.PARK_N_RIDE));
                dmu.setDriveDistToTap(tazManager.getTapDist(oTaz, oTapPosition,
                        Modes.AccessMode.PARK_N_RIDE));

                dmu.setTapMgraWalkTime(mgraManager.getMgraToTapWalkAlightTime(dMgra, dTapPosition));

                iv.setOriginZone(bestTaps[0]);
                iv.setDestZone(bestTaps[1]);
                double[] drvTransitTimes = bestDriveTransitUEC.solve(iv, dmu, null);
                accessibilities[3] = drvTransitTimes[0];
                accessibilities[10] = drvTransitTimes[1];

            }

            double[] transitLogsumResults = transitLogsumUEC.solve(iv, dmu, null);
            accessibilities[8] = transitLogsumResults[0];
        } // end if oMgra and dMgra > 0

        return accessibilities;
    }

    /**
     * Calculate constant terms, exponentiate, and store in constants array.
     */
    public void calculateConstants()
    {

        logger.info("Calculating constants");

        int modes = constantsUEC.getNumberOfAlternatives();
        expConstants = new double[MARKET_SEGMENTS + 1][modes]; // last element
                                                               // in
        // market segments is
        // for total
        IndexValues myIv = new IndexValues();

        for (int i = 0; i < MARKET_SEGMENTS + 1; ++i)
        {

            dmu.setAutoSufficiency(i);

            double[] utilities = constantsUEC.solve(myIv, dmu, null);

            // exponentiate the constants
            for (int j = 0; j < modes; ++j)
            {
                expConstants[i][j] = Math.exp(utilities[j]);
                logger.info("Exp. Constant, market " + i + " mode " + j + " = "
                        + expConstants[i][j]);
            }
        }
    }

    /**
     * Stop the matrix server
     */
    public void stopMatrixServer()
    {
        logger.info("Stopping matrix server");
        matrixServer.stop32BitMatrixIoServer();

    }

    /**
     * Start the matrix server
     * 
     * @param rb
     */
    public void startMatrixServer(ResourceBundle rb)
    {

        logger.info("Starting Matrix Server");
        String serverAddress = rb.getString("server.address");

        int serverPort = new Integer(rb.getString("server.port"));
        String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

        matrixServer = new MatrixDataServer();

        try
        {

            // create the concrete data server object
            matrixServer.start32BitMatrixIoServer(MatrixType.TRANSCAD);
        } catch (RuntimeException e)
        {
            matrixServer.stop32BitMatrixIoServer();
            logger.error(
                    "RuntimeException caught in com.pb.sandag.accessibilities.main() -- exiting.",
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

    }

    public static void main(String[] args)
    {

        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File(args[0]));
        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        MandatoryAccessibilities ma = new MandatoryAccessibilities(rbMap);

        ma.os64bit = Boolean.parseBoolean(rb.getString("operatingsystem.64bit"));
        if (ma.os64bit) ma.startMatrixServer(rb);

        ma.readData(rbMap);
        ma.buildAccessibilityComponents(rbMap);
        ma.calculateMandatoryAccessibilities(rbMap);

        if (ma.os64bit) ma.stopMatrixServer();

    }

}
