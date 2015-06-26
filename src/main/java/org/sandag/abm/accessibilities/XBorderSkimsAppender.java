package org.sandag.abm.accessibilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

public final class XBorderSkimsAppender
{

    protected transient Logger        logger                       = Logger.getLogger(XBorderSkimsAppender.class);

    /*
     * for trip mode choice estimation files
     */
    private static final String       MGRA_O_D_RECORDS_FILE_KEY    = "xborder.mgra.list.file";
    private static final String       APPENDED_SKIMS_FILE_KEY      = "xborder.appended.file";

    private String[]                  inputFormats                 = {"NUMBER", "NUMBER", "NUMBER",
            "STRING", "NUMBER", "NUMBER", "STRING"                 };
    private static final int          INPUT_ORIG_MGRA              = 5;
    private static final int          INPUT_DEST_MGRA              = 6;
    private static final int          INPUT_DEPART_PERIOD          = 7;

    // survey periods are:
    // 0=not used,
    // 1=03:00-05:59,
    // 2=06:00-08:59,
    // 3=09:00-11:59,
    // 4=12:00-15:29,
    // 5=15:30-18:59,
    // 6=19:00-02:59
    // skim periods are: 0=0(N/A), 1=3(OP), 2=1(AM), 3=3(OP), 4=3(OP), 5=2(PM),
    // 6=3(OP)

    // define a conversion array to convert period values in the survey file to
    // skim
    // period indices used in this propgram: 1=am peak, 2=pm peak, 3=off-peak.
    private static final String[]     SKIM_PERIOD_LABELS           = {"am", "pm", "op"};
    private static final int[]        SURVEY_PERIOD_TO_SKIM_PERIOD = {0, 3, 1, 3, 3, 2, 3};

    private static int                debugOrigMgra                = 0;
    private static int                debugDestMgra                = 0;
    private static int                departModelPeriod            = 0;

    private MatrixDataServerIf        ms;
    private BestTransitPathCalculator bestPathUEC;
    private static final float defaultVOT = 15.0f;

    private XBorderSkimsAppender()
    {
    }

    private void runSkimsAppender(ResourceBundle rb)
    {

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        Logger autoLogger = Logger.getLogger("auto");
        Logger wtwLogger = Logger.getLogger("wtw");

        String outputFileNameHis = Util.getStringValueFromPropertyMap(rbMap,
                APPENDED_SKIMS_FILE_KEY);

        FileWriter writer;
        PrintWriter outStreamHis = null;

        AutoTazSkimsCalculator tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
        tazDistanceCalculator.computeTazDistanceArrays();

        McLogsumsAppender logsumHelper = new McLogsumsAppender(rbMap);
        bestPathUEC = logsumHelper.getBestTransitPathCalculator();
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        AutoAndNonMotorizedSkimsCalculator anm = logsumHelper.getAnmSkimCalculator();
        WalkTransitWalkSkimsCalculator wtw = logsumHelper.getWtwSkimCalculator();

        String heading = "seq";

        heading += ",origMgra,destMgra,departPeriod";
        heading += getAutoSkimsHeaderRecord("auto", anm.getAutoSkimNames());
        heading += getNonMotorizedSkimsHeaderRecord("nm", anm.getNmSkimNames());
        heading += getTransitSkimsHeaderRecord("wtw", wtw.getLocalSkimNames(),
                wtw.getPremiumSkimNames());

        try
        {
            writer = new FileWriter(new File(outputFileNameHis));
            outStreamHis = new PrintWriter(new BufferedWriter(writer));
        } catch (IOException e)
        {
            logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                    outputFileNameHis));
            throw new RuntimeException(e);
        }
        outStreamHis.println(heading);

        Logger[] loggers = new Logger[4];
        loggers[0] = autoLogger;
        loggers[1] = wtwLogger;

        int[] odt = new int[3];

        TableDataSet hisTds = getInputTableDataSet(rbMap);
        int[][] hisOdts = getInputOrigDestTimes(hisTds);
        // 11100, pnrCoaster, mgra 4357 = taz 3641, mgra 26931 = taz 1140
        // int[][] hisOdts = { { 4357, 26931, 5, 0 } };
        // 25040, wlkCoaster, mgra 4989 = taz 3270, mgra 7796 = taz 1986
        // int[][] hisOdts = { { 7796, 4989, 2, 0 } };

        // if ( debugOrigMgra <= 0 || debugDestMgra <= 0 || departModelPeriod <=
        // 0 || departModelPeriod > 6 )
        // {
        // logger.error("please set values for command line arguments: properties file, orig mgra, dest mgra, depart model period.");
        // System.exit(-1);
        // }
        // int[][] hisOdts = { { debugOrigMgra, debugDestMgra,
        // departModelPeriod, 0 } };

        // write skims data for home interview survey records
        int seq = 1;
        for (int[] hisOdt : hisOdts)
        {
            // write outbound direction
            odt[0] = hisOdt[0]; // orig
            odt[1] = hisOdt[1]; // dest
            odt[2] = SURVEY_PERIOD_TO_SKIM_PERIOD[hisOdt[2]]; // depart skim
                                                              // period

            try
            {

                writeSkimsToFile(seq, outStreamHis, false, odt, anm, wtw, loggers);
            } catch (Exception e)
            {
                logger.error("Exception caught processing record: " + seq + " of " + hisOdts.length
                        + ".");
                break;
            }

            if (seq % 1000 == 0) logger.info("wrote HIS record: " + seq);

            seq++;
        }

        outStreamHis.close();

    }

    private void writeSkimsToFile(int sequence, PrintWriter outStream, boolean loggingEnabled,
            int[] odt, AutoAndNonMotorizedSkimsCalculator anm, WalkTransitWalkSkimsCalculator wtw,
            Logger[] loggers)
    {

        Logger autoLogger = loggers[0];
        Logger wtwLogger = loggers[1];

        int[][] bestTapPairs = null;
        double[][] returnedSkims = null;

        outStream.print(String.format("%d,%d,%d,%s", sequence, odt[0], odt[1],
                SKIM_PERIOD_LABELS[odt[2] - 1]));

        double[] skims = anm.getAutoSkims(odt[0], odt[1], odt[2], defaultVOT, loggingEnabled, autoLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(odt[0], odt[1], odt[2], skims, "auto", autoLogger);

        String autoRecord = getAutoSkimsRecord(skims);
        outStream.print(autoRecord);

        skims = anm.getNonMotorizedSkims(odt[0], odt[1], odt[2], loggingEnabled, autoLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(odt[0], odt[1], odt[2], skims, "non-motorized", autoLogger);

        String nmRecord = getAutoSkimsRecord(skims);
        outStream.print(nmRecord);

        bestTapPairs = wtw.getBestTapPairs(odt[0], odt[1], odt[2], loggingEnabled, wtwLogger);
        returnedSkims = new double[bestTapPairs.length][];
        for (int i = 0; i < bestTapPairs.length; i++)
        {
            if (bestTapPairs[i] == null) returnedSkims[i] = wtw.getNullTransitSkims(i);
            else
            {
                returnedSkims[i] = wtw.getWalkTransitWalkSkims(i, BestTransitPathCalculator
                        .findWalkTransitAccessTime(odt[0], bestTapPairs[i][0]),
                        BestTransitPathCalculator.findWalkTransitEgressTime(odt[1],
                                bestTapPairs[i][1]), bestTapPairs[i][0], bestTapPairs[i][1],
                        odt[2], loggingEnabled);
            }
        }
        if (loggingEnabled) wtw.logReturnedSkims(odt, bestTapPairs, returnedSkims);

        String wtwRecord = getTransitSkimsRecord(odt, returnedSkims);
        outStream.println(wtwRecord);

    }

    /**
     * Start the matrix server
     * 
     * @param rb
     *            is a ResourceBundle for the properties file for this
     *            application
     */
    private void startMatrixServer(ResourceBundle rb)
    {

        logger.info("");
        logger.info("");
        String serverAddress = rb.getString("RunModel.MatrixServerAddress");
        int serverPort = new Integer(rb.getString("RunModel.MatrixServerPort"));
        logger.info("connecting to matrix server " + serverAddress + ":" + serverPort);

        try
        {

            MatrixDataManager mdm = MatrixDataManager.getInstance();
            ms = new MatrixDataServerRmi(serverAddress, serverPort,
                    MatrixDataServer.MATRIX_DATA_SERVER_NAME);
            ms.testRemote(Thread.currentThread().getName());
            mdm.setMatrixDataServerObject(ms);

        } catch (Exception e)
        {

            logger.error(
                    String.format("exception caught running ctramp model components -- exiting."),
                    e);
            throw new RuntimeException();

        }

    }

    /**
     * create a String which can be written to an output file with all the skim
     * values for the orig/dest/period.
     * 
     * @param odt
     *            is an int[] with the first element the origin mgra and the
     *            second element the dest mgra and third element the departure
     *            period index
     * @param skims
     *            is a double[][] of skim values with the first dimesion the
     *            ride mode indices and second dimention the skim categories
     */
    private String getTransitSkimsRecord(int[] odt, double[][] skims)
    {

        int nrows = skims.length;
        int ncols = 0;
        for (int i = 0; i < nrows; i++)
            if (skims[i].length > ncols) ncols = skims[i].length;

        String tableRecord = "";
        for (int i = 0; i < skims.length; i++)
        {
            for (int j = 0; j < skims[i].length; j++)
                tableRecord += String.format(",%.5f", skims[i][j]);
        }

        return tableRecord;

    }

    /**
     * create a String which can be written to an output file with all the skim
     * values for the orig/dest/period.
     * 
     * @param odt
     *            is an int[] with the first element the origin mgra and the
     *            second element the dest mgra and third element the departure
     *            period index
     * @param skims
     *            is a double[] of skim values
     */
    private String getAutoSkimsRecord(double[] skims)
    {

        String tableRecord = "";
        for (int i = 0; i < skims.length; i++)
        {
            tableRecord += String.format(",%.5f", skims[i]);
        }

        return tableRecord;

    }

    /**
     * create a String for the output file header record which can be written to
     * an output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt
     *            is an int[] with the first element the origin mgra and the
     *            second element the dest mgra and third element the departure
     *            period index
     */
    private String getTransitSkimsHeaderRecord(String transitServiveLabel, String[] localNames,
            String[] premiumNames)
    {

        Modes.TransitMode[] mode = Modes.TransitMode.values();

        String heading = "";

        for (int i = 0; i < mode.length; i++)
        {
            if (mode[i].isPremiumMode(mode[i]))
            {
                for (int j = 0; j < premiumNames.length; j++)
                    heading += String.format(",%s_%s_%s", transitServiveLabel, mode[i],
                            premiumNames[j]);
            } else
            {
                for (int j = 0; j < localNames.length; j++)
                    heading += String.format(",%s_%s_%s", transitServiveLabel, mode[i],
                            localNames[j]);
            }
        }

        return heading;
    }

    /**
     * create a String for the output file header record which can be written to
     * an output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt
     *            is an int[] with the first element the origin mgra and the
     *            second element the dest mgra and third element the departure
     *            period index
     */
    private String getAutoSkimsHeaderRecord(String label, String[] names)
    {

        String heading = "";

        for (int i = 0; i < names.length; i++)
            heading += String.format(",%s_%s", label, names[i]);

        return heading;
    }

    /**
     * create a String for the output file header record which can be written to
     * an output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt
     *            is an int[] with the first element the origin mgra and the
     *            second element the dest mgra and third element the departure
     *            period index
     */
    private String getNonMotorizedSkimsHeaderRecord(String label, String[] names)
    {

        String heading = "";

        for (int i = 0; i < names.length; i++)
            heading += String.format(",%s_%s", label, names[i]);

        return heading;
    }

    private TableDataSet getInputTableDataSet(HashMap<String, String> rbMap)
    {

        String hisFileName = Util.getStringValueFromPropertyMap(rbMap, MGRA_O_D_RECORDS_FILE_KEY);
        if (hisFileName == null)
        {
            logger.error("Error getting the filename from the properties file for the XBorder MGRA List data records file.");
            logger.error("Properties file target: " + MGRA_O_D_RECORDS_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + MGRA_O_D_RECORDS_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        try
        {
            TableDataSet inTds = null;
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            inTds = reader.readFileWithFormats(new File(hisFileName), inputFormats);
            // inTds = reader.readFile(new File(hisFileName));
            return inTds;
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading Sandag XBorder MGRA List data records file: %s into TableDataSet object.",
                            hisFileName));
            throw new RuntimeException(e);
        }

    }

    private int[][] getInputOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra,
        // departure period(1-6), and his sampno.
        int[][] odts = new int[hisTds.getRowCount()][4];

        int[] origs = hisTds.getColumnAsInt(INPUT_ORIG_MGRA);
        int[] dests = hisTds.getColumnAsInt(INPUT_DEST_MGRA);
        String[] departStrings = hisTds.getColumnAsString(INPUT_DEPART_PERIOD);
        int[] departs = new int[departStrings.length];

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            if (departStrings[r - 1].equalsIgnoreCase("am")) departs[r - 1] = 2;
            else if (departStrings[r - 1].equalsIgnoreCase("pm")) departs[r - 1] = 5;
            else if (departStrings[r - 1].equalsIgnoreCase("op")) departs[r - 1] = 1;
            else departs[r - 1] = -1;

            odts[r - 1][0] = origs[r - 1];
            odts[r - 1][1] = dests[r - 1];
            odts[r - 1][2] = departs[r - 1];
        }

        return odts;
    }

    public static void main(String[] args)
    {

        ResourceBundle rb = null;
        if (args.length == 0)
        {
            System.out
                    .println(String
                            .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else if (args.length == 4)
        {
            rb = ResourceBundle.getBundle(args[0]);

            debugOrigMgra = Integer.parseInt(args[1]);
            debugDestMgra = Integer.parseInt(args[2]);
            departModelPeriod = Integer.parseInt(args[3]);
        } else
        {
            System.out
                    .println("please set values for command line arguments: properties file, orig mgra, dest mgra, depart model period.");
            System.exit(-1);
        }

        MatrixIO32BitJvm ioVm32Bit = null;

        try
        {

            MatrixDataServerIf ms = null;
            String serverAddress = null;
            int serverPort = -1;

            HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

            System.out.println("");
            System.out.println("");
            serverAddress = (String) propertyMap.get("RunModel.MatrixServerAddress");

            String serverPortString = (String) propertyMap.get("RunModel.MatrixServerPort");
            if (serverPortString != null) serverPort = Integer.parseInt(serverPortString);

            if (serverAddress != null && serverPort > 0)
            {
                try
                {
                    System.out.println("attempting connection to matrix server " + serverAddress
                            + ":" + serverPort);

                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    ms = new MatrixDataServerRmi(serverAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    ms.testRemote(Thread.currentThread().getName());
                    mdm.setMatrixDataServerObject(ms);
                    System.out.println("connected to matrix server " + serverAddress + ":"
                            + serverPort);

                } catch (Exception e)
                {
                    System.out
                            .println("exception caught running ctramp model components -- exiting.");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            } else
            {
                System.out.println("starting matrix data server in a 32 bit process.");
                // start the 32 bit JVM used specifically for running matrix io
                // classes
                ioVm32Bit = MatrixIO32BitJvm.getInstance();
                ioVm32Bit.startJVM32();

                // establish that matrix reader and writer classes will use the
                // RMI versions for TRANSCAD format matrices
                ioVm32Bit.startMatrixDataServer(MatrixType.TRANSCAD);
            }

            TazDataManager tazs = TazDataManager.getInstance(propertyMap);
            MgraDataManager mgraManager = MgraDataManager.getInstance(propertyMap);
            TapDataManager tapManager = TapDataManager.getInstance(propertyMap);

            // create an appender object and run it
            XBorderSkimsAppender appender = new XBorderSkimsAppender();
            appender.runSkimsAppender(rb);

            if (ms == null)
            {
                // establish that matrix reader and writer classes will not use
                // the RMI versions any longer.
                // local matrix i/o, as specified by setting types, is now the
                // default again.
                ioVm32Bit.stopMatrixDataServer();

                // close the JVM in which the RMI reader/writer classes were
                // running
                ioVm32Bit.stopJVM32();
                System.out.println("matrix data server 32 bit process stopped.");
            } else
            {
                ms.stop32BitMatrixIoServer();
                System.out.println("matrix data server 32 bit process stopped.");
            }

        } catch (RuntimeException e)
        {

            // establish that matrix reader and writer classes will not use the
            // RMI versions any longer.
            // local matrix i/o, as specified by setting types, is now the
            // default again.
            ioVm32Bit.stopMatrixDataServer();

            // close the JVM in which the RMI reader/writer classes were running
            ioVm32Bit.stopJVM32();
            System.out.println("matrix data server 32 bit process stopped.");

            e.printStackTrace();

        }

    }

}
