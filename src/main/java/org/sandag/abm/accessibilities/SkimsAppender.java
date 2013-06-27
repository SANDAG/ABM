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
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public final class SkimsAppender
{

    protected transient Logger       logger                       = Logger.getLogger(SkimsAppender.class);

    private static final String   OBS_DATA_RECORDS_FILE_KEY    = "onBoard.survey.file";
    private static final String   HIS_DATA_RECORDS_FILE_KEY    = "homeInterview.survey.file";

    private static final int      OBS_UNIQUE_ID                = 1;
    private static final int      OBS_ORIG_MGRA                = 78;
    private static final int      OBS_DEST_MGRA                = 79;
    
//    used for trip file:    
    private static final int      OBS_OUT_TOUR_PERIOD           = 133;
    private static final int      OBS_IN_TOUR_PERIOD            = 134;

//    used for tour file:    
//    private static final int      OBS_DEPART_PERIOD            = 132;
//    private static final int      OBS_ARRIVE_PERIOD            = 133;

    /*
     * for home based tour mode choice estimation files private static final int
     * HIS_ORIG_MGRA = 72; private static final int HIS_DEST_MGRA = 75; private
     * static final int HIS_DEPART_PERIOD = 185; private static final int
     * HIS_ARRIVE_PERIOD = 186;
     */

    /*
     * for work based tour mode choice estimation files
     */
    private static final int      HIS_ORIG_MGRA                = 76;
    private static final int      HIS_DEST_MGRA                = 84;
    private static final int      HIS_DEPART_PERIOD            = 159;
    private static final int      HIS_ARRIVE_PERIOD            = 160;

    // survey periods are: 0=not used, 1=03:00-05:59, 2=06:00-08:59, 3=09:00-11:59,
    // 4=12:00-15:29, 5=15:30-18:59, 6=19:00-02:59
    // skim periods are: 0=0(N/A), 1=3(OP), 2=1(AM), 3=3(OP), 4=3(OP), 5=2(PM),
    // 6=3(OP)

    // define a conversion array to convert period values in the survey file to skim
    // period indices used in this propgram: 1=am peak, 2=pm peak,
    // 3=off-peak.
    private static final String[] SKIM_PERIOD_LABELS           = {"am", "pm", "op"};
    private static final int[]    SURVEY_PERIOD_TO_SKIM_PERIOD = {0, 3, 1, 3, 3, 2, 3};

    private MatrixDataServerIf    ms;
    private BestTransitPathCalculator bestPathUEC;
    
    
    private SkimsAppender()
    {
    }

    private void runSkimsAppender(ResourceBundle rb)
    {

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        
        // instantiate these objects right away
        TazDataManager tazs = TazDataManager.getInstance(rbMap);
        MgraDataManager mgraManager = MgraDataManager.getInstance(rbMap);
        TapDataManager tapManager = TapDataManager.getInstance(rbMap);
        
        AutoAndNonMotorizedSkimsCalculator anm = null;
        WalkTransitWalkSkimsCalculator wtw = null;
        WalkTransitDriveSkimsCalculator wtd = null;
        DriveTransitWalkSkimsCalculator dtw = null;

        Logger autoLogger = Logger.getLogger("auto");
        Logger wtwLogger = Logger.getLogger("wtw");
        Logger wtdLogger = Logger.getLogger("wtd");
        Logger dtwLogger = Logger.getLogger("dtw");


        String outputFileNameObs = Util.getStringValueFromPropertyMap(rbMap,
                "obs.skims.output.file");
        String outputFileNameHis = Util.getStringValueFromPropertyMap(rbMap,
                "his.skims.output.file");

        FileWriter writer;
        PrintWriter outStreamObs = null;
        PrintWriter outStreamHis = null;

        PrintWriter[] outStreamObsTod = new PrintWriter[SKIM_PERIOD_LABELS.length];
        PrintWriter[] outStreamHisTod = new PrintWriter[SKIM_PERIOD_LABELS.length];

        if (outputFileNameObs != "" || outputFileNameHis != "")
        {

            anm = new AutoAndNonMotorizedSkimsCalculator(rbMap);

            McLogsumsCalculator logsumHelper = new McLogsumsCalculator();                        
            bestPathUEC = logsumHelper.getBestTransitPathCalculator();

            wtw = new WalkTransitWalkSkimsCalculator();
            wtw.setup(rbMap, wtwLogger, bestPathUEC);

            wtd = new WalkTransitDriveSkimsCalculator();
            wtd.setup(rbMap, wtdLogger, bestPathUEC);

            dtw = new DriveTransitWalkSkimsCalculator();
            dtw.setup(rbMap, dtwLogger, bestPathUEC);

            String heading = "Seq,Id";

            heading += ",obOrigMgra,obDestMgra,obPeriod";
            heading += getAutoSkimsHeaderRecord("auto", anm.getAutoSkimNames());
            heading += getNonMotorizedSkimsHeaderRecord("nm", anm.getNmSkimNames());
            heading += getTransitSkimsHeaderRecord("wtw", wtw.getLocalSkimNames(), wtw
                    .getPremiumSkimNames());
            heading += getTransitSkimsHeaderRecord("wtd", wtd.getLocalSkimNames(), wtd
                    .getPremiumSkimNames());
            heading += getTransitSkimsHeaderRecord("dtw", dtw.getLocalSkimNames(), dtw
                    .getPremiumSkimNames());

            heading += ",ObsSeq,Id,ibOrigMgra,ibDestMgra,ibPeriod";
            heading += getAutoSkimsHeaderRecord("auto", anm.getAutoSkimNames());
            heading += getNonMotorizedSkimsHeaderRecord("nm", anm.getNmSkimNames());
            heading += getTransitSkimsHeaderRecord("wtw", wtw.getLocalSkimNames(), wtw
                    .getPremiumSkimNames());
            heading += getTransitSkimsHeaderRecord("wtd", wtd.getLocalSkimNames(), wtd
                    .getPremiumSkimNames());
            heading += getTransitSkimsHeaderRecord("dtw", dtw.getLocalSkimNames(), dtw
                    .getPremiumSkimNames());

            if (outputFileNameObs != "")
            {
                try
                {
                    // create an output stream for the mode choice estimation file
                    // with
                    // observed TOD
                    writer = new FileWriter(new File(outputFileNameObs));
                    outStreamObs = new PrintWriter(new BufferedWriter(writer));

                    // create an array of similar files, 1 for each TOD period, for
                    // TOD
                    // choice estimation
                    for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    {
                        int dotIndex = outputFileNameObs.lastIndexOf('.');
                        String newName = outputFileNameObs.substring(0, dotIndex) + "_"
                                + SKIM_PERIOD_LABELS[i] + outputFileNameObs.substring(dotIndex);
                        writer = new FileWriter(new File(newName));
                        outStreamObsTod[i] = new PrintWriter(new BufferedWriter(writer));
                    }
                } catch (IOException e)
                {
                    logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                            outputFileNameObs));
                    throw new RuntimeException(e);
                }

                outStreamObs.println("obs" + heading);
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamObsTod[i].println("obs" + heading);
            }

            if (outputFileNameHis != "")
            {
                try
                {
                    writer = new FileWriter(new File(outputFileNameHis));
                    outStreamHis = new PrintWriter(new BufferedWriter(writer));

                    // create an array of similar files, 1 for each TOD period, for
                    // TOD
                    // choice estimation
                    for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    {
                        int dotIndex = outputFileNameHis.lastIndexOf('.');
                        String newName = outputFileNameHis.substring(0, dotIndex) + "_"
                                + SKIM_PERIOD_LABELS[i] + outputFileNameHis.substring(dotIndex);
                        writer = new FileWriter(new File(newName));
                        outStreamHisTod[i] = new PrintWriter(new BufferedWriter(writer));
                    }
                } catch (IOException e)
                {
                    logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                            outputFileNameHis));
                    throw new RuntimeException(e);
                }

                outStreamHis.println("his" + heading);
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamHisTod[i].println("his" + heading);
            }

        }

        Logger[] loggers = new Logger[4];
        loggers[0] = autoLogger;
        loggers[1] = autoLogger;
        loggers[2] = wtdLogger;
        loggers[3] = dtwLogger;

        int[] odt = new int[5];

        if (outputFileNameObs != "")
        {
            TableDataSet obsTds = getOnBoardSurveyTableDataSet(rbMap);
            int[][] obsOdts = getOnBoardSurveyOrigDestTimes(obsTds);

            // write skims data for on-board survey records
            int seq = 1;
            for (int[] obsOdt : obsOdts)
            {
                // write outbound direction
                odt[0] = obsOdt[0]; // orig
                odt[1] = obsOdt[1]; // dest
                odt[2] = SURVEY_PERIOD_TO_SKIM_PERIOD[obsOdt[2]]; // depart skim
                                                                  // period
                odt[3] = obsOdt[3];
                odt[4] = obsOdt[4];

                if ( odt[0] == 0 || odt[1] == 0 ) {
                    outStreamObs.println(String.format("%d,%d,%d,%d,%d", seq, odt[4], odt[0], odt[1], odt[2]));
                    seq++;
                    continue;
                }

                // index

                // for debugging a specific mgra pair
                // odt[0] = 25646;
                // odt[1] = 4319;
                // odt[2] = 1;

                boolean debugFlag = false;
                if (odt[0] == 25646 && odt[1] == 4319) debugFlag = true;

                writeSkimsToFile(seq, outStreamObs, debugFlag, odt, anm, wtw, wtd, dtw, loggers);

                // set odt[2] to be each skim priod index (1,2,3) and write a
                // separate
                // output file
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                {
                    odt[2] = i + 1;
                    writeSkimsToFile(seq, outStreamObsTod[i], false, odt, anm, wtw, wtd, dtw, loggers);
                }

                // write inbound direction
                odt[0] = obsOdt[1]; // dest
                odt[1] = obsOdt[0]; // orig
                odt[2] = SURVEY_PERIOD_TO_SKIM_PERIOD[obsOdt[3]]; // arrival skim
                                                                  // period
                // index

                outStreamObs.print(",");
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamObsTod[i].print(",");

                writeSkimsToFile(seq, outStreamObs, debugFlag, odt, anm, wtw, wtd, dtw, loggers);

                // set odt[2] to be each skim priod index (1,2,3) and write a
                // separate
                // output file
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                {
                    odt[2] = i + 1;
                    writeSkimsToFile(seq, outStreamObsTod[i], false, odt, anm, wtw, wtd, dtw, loggers);
                }

                if (outStreamObs != null)
                {
                    outStreamObs.println("");
                    for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                        outStreamObsTod[i].println("");
                }

                if (seq % 1000 == 0) logger.info("wrote OBS record: " + seq);

                seq++;
            }
            if (outStreamObs != null)
            {
                outStreamObs.close();
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamObsTod[i].close();
            }
        }

        if (outputFileNameHis != "")
        {
            TableDataSet hisTds = getHomeInterviewSurveyTableDataSet(rbMap);
            int[][] hisOdts = getHomeInterviewSurveyOrigDestTimes(hisTds);

            // write skims data for home interview survey records
            int seq = 1;
            for (int[] hisOdt : hisOdts)
            {
                // write outbound direction
                odt[0] = hisOdt[0]; // orig
                odt[1] = hisOdt[1]; // dest
                odt[2] = SURVEY_PERIOD_TO_SKIM_PERIOD[hisOdt[2]]; // depart skim
                                                                  // period
                // index
                writeSkimsToFile(seq, outStreamHis, false, odt, anm, wtw, wtd, dtw, loggers);

                // set odt[2] to be each skim priod index (1,2,3) and write a
                // separate
                // output file
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                {
                    odt[2] = i + 1;
                    writeSkimsToFile(seq, outStreamHisTod[i], false, odt, anm, wtw, wtd, dtw, loggers);
                }

                // write inbound direction
                odt[0] = hisOdt[1]; // dest
                odt[1] = hisOdt[0]; // orig
                odt[2] = SURVEY_PERIOD_TO_SKIM_PERIOD[hisOdt[3]]; // arrival skim
                                                                  // period
                // index

                outStreamHis.print(",");
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamHisTod[i].print(",");

                writeSkimsToFile(seq, outStreamHis, false, odt, anm, wtw, wtd, dtw, loggers);

                // set odt[2] to be each skim priod index (1,2,3) and write a
                // separate
                // output file
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                {
                    odt[2] = i + 1;
                    writeSkimsToFile(seq, outStreamHisTod[i], false, odt, anm, wtw, wtd, dtw, loggers);
                }

                if (outStreamHis != null)
                {
                    outStreamHis.println("");
                    for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                        outStreamHisTod[i].println("");
                }

                if (seq % 1000 == 0) logger.info("wrote HIS record: " + seq);

                seq++;
            }
            if (outStreamHis != null)
            {
                outStreamHis.close();
                for (int i = 0; i < SKIM_PERIOD_LABELS.length; i++)
                    outStreamHisTod[i].close();
            }
        }

    }

    private void writeSkimsToFile(int sequence, PrintWriter outStream, boolean loggingEnabled,
            int[] odt, AutoAndNonMotorizedSkimsCalculator anm, WalkTransitWalkSkimsCalculator wtw,
            WalkTransitDriveSkimsCalculator wtd, DriveTransitWalkSkimsCalculator dtw,
            Logger[] loggers)
    {

        Logger autoLogger = loggers[0];
        Logger wtwLogger = loggers[1];
        Logger wtdLogger = loggers[2];
        Logger dtwLogger = loggers[3];

        
        int[][] bestTapPairs = null;
        double[][] returnedSkims = null;

        if (outStream != null)
            outStream.print(String.format("%d,%d,%d,%d,%d", sequence, odt[4], odt[0], odt[1], odt[2]));

        double[] skims = anm.getAutoSkims(odt[0], odt[1], odt[2], loggingEnabled, autoLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(odt[0], odt[1], odt[2], skims, "auto", autoLogger);

        if (outStream != null)
        {
            String autoRecord = getAutoSkimsRecord(skims);
            outStream.print(autoRecord);
        }

        skims = anm.getNonMotorizedSkims(odt[0], odt[1], odt[2], loggingEnabled, autoLogger);
        if (loggingEnabled)
            anm.logReturnedSkims(odt[0], odt[1], odt[2], skims, "non-motorized", autoLogger);

        if (outStream != null)
        {
            String nmRecord = getAutoSkimsRecord(skims);
            outStream.print(nmRecord);
        }

        bestTapPairs = wtw.getBestTapPairs(odt[0], odt[1], odt[2], loggingEnabled, wtwLogger);
        returnedSkims = new double[bestTapPairs.length][];
        for (int i = 0; i < bestTapPairs.length; i++)
        {
            if (bestTapPairs[i] == null) returnedSkims[i] = wtw.getNullTransitSkims(i);
            else
            {
                returnedSkims[i] = wtw.getWalkTransitWalkSkims(i, 
                        bestPathUEC.getBestAccessTime(i),
                        bestPathUEC.getBestEgressTime(i),
                        bestTapPairs[i][0], bestTapPairs[i][1], odt[2], loggingEnabled);
            }
        }
        if (loggingEnabled) wtw.logReturnedSkims(odt, bestTapPairs, returnedSkims);

        if (outStream != null)
        {
            String wtwRecord = getTransitSkimsRecord(odt, returnedSkims);
            outStream.print(wtwRecord);
        }

        bestTapPairs = wtd.getBestTapPairs(odt[0], odt[1], odt[2], loggingEnabled, wtdLogger);
        returnedSkims = new double[bestTapPairs.length][];
        for (int i = 0; i < bestTapPairs.length; i++)
        {
            if (bestTapPairs[i] == null) returnedSkims[i] = wtd.getNullTransitSkims(i);
            else
            {
                returnedSkims[i] = wtd.getWalkTransitDriveSkims(i, 
                        bestPathUEC.getBestAccessTime(i),
                        bestPathUEC.getBestEgressTime(i),
                        bestTapPairs[i][0], bestTapPairs[i][1], odt[2], loggingEnabled);
            }
        }
        if (loggingEnabled) wtd.logReturnedSkims(odt, bestTapPairs, returnedSkims);

        if (outStream != null)
        {
            String wtdRecord = getTransitSkimsRecord(odt, returnedSkims);
            outStream.print(wtdRecord);
        }

        bestTapPairs = dtw.getBestTapPairs(odt[0], odt[1], odt[2], loggingEnabled, dtwLogger);
        returnedSkims = new double[bestTapPairs.length][];
        for (int i = 0; i < bestTapPairs.length; i++)
        {
            if (bestTapPairs[i] == null) returnedSkims[i] = dtw.getNullTransitSkims(i);
            else
            {
                returnedSkims[i] = dtw.getDriveTransitWalkSkims(i, 
                        bestPathUEC.getBestAccessTime(i),
                        bestPathUEC.getBestEgressTime(i),
                        bestTapPairs[i][0], bestTapPairs[i][1], odt[2], loggingEnabled);
            }
        }
        if (loggingEnabled) dtw.logReturnedSkims(odt, bestTapPairs, returnedSkims);

        if (outStream != null)
        {
            String dtwRecord = getTransitSkimsRecord(odt, returnedSkims);
            outStream.print(dtwRecord);
        }

    }

    /**
     * Start the matrix server
     * 
     * @param rb is a ResourceBundle for the properties file for this application
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

            logger.error(String
                    .format("exception caught running ctramp model components -- exiting."), e);
            throw new RuntimeException();

        }

    }

    /**
     * create a String which can be written to an output file with all the skim
     * values for the orig/dest/period.
     * 
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
     * @param skims is a double[][] of skim values with the first dimesion the ride
     *            mode indices and second dimention the skim categories
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
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
     * @param skims is a double[] of skim values
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
     * create a String for the output file header record which can be written to an
     * output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
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
     * create a String for the output file header record which can be written to an
     * output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
     */
    private String getAutoSkimsHeaderRecord(String label, String[] names)
    {

        String heading = "";

        for (int i = 0; i < names.length; i++)
            heading += String.format(",%s_%s", label, names[i]);

        return heading;
    }

    /**
     * create a String for the output file header record which can be written to an
     * output file with all the skim value namess for the orig/dest/period.
     * 
     * @param odt is an int[] with the first element the origin mgra and the second
     *            element the dest mgra and third element the departure period index
     */
    private String getNonMotorizedSkimsHeaderRecord(String label, String[] names)
    {

        String heading = "";

        for (int i = 0; i < names.length; i++)
            heading += String.format(",%s_%s", label, names[i]);

        return heading;
    }

    private TableDataSet getOnBoardSurveyTableDataSet(HashMap<String, String> rbMap)
    {

        String obsFileName = Util.getStringValueFromPropertyMap(rbMap, OBS_DATA_RECORDS_FILE_KEY);
        if (obsFileName == null)
        {
            logger
                    .error("Error getting the filename from the properties file for the Sandag on-board survey data records file.");
            logger.error("Properties file target: " + OBS_DATA_RECORDS_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + OBS_DATA_RECORDS_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        try
        {
            TableDataSet inTds = null;
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            inTds = reader.readFile(new File(obsFileName));
            return inTds;
        } catch (Exception e)
        {
            logger
                    .fatal(String
                            .format(
                                    "Exception occurred reading Sandag on-board survey data records file: %s into TableDataSet object.",
                                    obsFileName));
            throw new RuntimeException(e);
        }

    }

    private TableDataSet getHomeInterviewSurveyTableDataSet(HashMap<String, String> rbMap)
    {

        String hisFileName = Util.getStringValueFromPropertyMap(rbMap, HIS_DATA_RECORDS_FILE_KEY);
        if (hisFileName == null)
        {
            logger
                    .error("Error getting the filename from the properties file for the Sandag home interview survey data records file.");
            logger.error("Properties file target: " + HIS_DATA_RECORDS_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + HIS_DATA_RECORDS_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        try
        {
            TableDataSet inTds = null;
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            inTds = reader.readFile(new File(hisFileName));
            return inTds;
        } catch (Exception e)
        {
            logger
                    .fatal(String
                            .format(
                                    "Exception occurred reading Sandag home interview survey data records file: %s into TableDataSet object.",
                                    hisFileName));
            throw new RuntimeException(e);
        }

    }

    private int[][] getOnBoardSurveyOrigDestTimes(TableDataSet obsTds)
    {

        // odts are an array with elements: origin mgra, destination mgra, departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[obsTds.getRowCount()][5];

        int[] origs = obsTds.getColumnAsInt(OBS_ORIG_MGRA);
        int[] dests = obsTds.getColumnAsInt(OBS_DEST_MGRA);
        int[] departs = obsTds.getColumnAsInt(OBS_OUT_TOUR_PERIOD);
        int[] arrives = obsTds.getColumnAsInt(OBS_IN_TOUR_PERIOD);
        int[] ids = obsTds.getColumnAsInt(OBS_UNIQUE_ID);
        
        for (int r = 1; r <= obsTds.getRowCount(); r++)
        {
            odts[r - 1][0] = origs[r - 1];
            odts[r - 1][1] = dests[r - 1];
            odts[r - 1][2] = departs[r - 1];
            odts[r - 1][3] = arrives[r - 1];
            odts[r - 1][4] = ids[r - 1];
        }

        return odts;
    }

    private int[][] getHomeInterviewSurveyOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra, departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[hisTds.getRowCount()][4];

        int[] origs = hisTds.getColumnAsInt(HIS_ORIG_MGRA);
        int[] dests = hisTds.getColumnAsInt(HIS_DEST_MGRA);
        int[] departs = hisTds.getColumnAsInt(HIS_DEPART_PERIOD);
        int[] arrives = hisTds.getColumnAsInt(HIS_ARRIVE_PERIOD);

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            odts[r - 1][0] = origs[r - 1];
            odts[r - 1][1] = dests[r - 1];
            odts[r - 1][2] = departs[r - 1];
            odts[r - 1][3] = arrives[r - 1];
        }

        return odts;
    }

    public static void main(String[] args)
    {

        ResourceBundle rb;
        if (args.length == 0)
        {
            System.out.println(String.format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
        }

        SkimsAppender appender = new SkimsAppender();

        appender.startMatrixServer(rb);
        appender.runSkimsAppender(rb);

    }

}
