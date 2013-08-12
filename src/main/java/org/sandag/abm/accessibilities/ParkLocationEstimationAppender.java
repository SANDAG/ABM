package org.sandag.abm.accessibilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public final class ParkLocationEstimationAppender
{

    private transient Logger      logger                           = Logger.getLogger(ParkLocationEstimationAppender.class);

    private static final int      DEBUG_EST_RECORD                 = 1;

    private static final String   ESTIMATION_DATA_RECORDS_FILE_KEY = "plc.estimation.data.file";
    private static final String   OUTPUT_DATA_RECORDS_FILE_KEY     = "plc.est.skims.output.file";

    // define input table field indices
    private static final int      ID_FIELD                         = 1;
    private static final int      DEPART_PERIOD_FIELD              = 2;
    private static final int      TYPE_FIELD                       = 4;
    private static final int      ORIG_FIELD                       = 7;
    private static final int      DEST_FIELD                       = 8;

    // define indices for storing input data in an internal table.
    // start field indices at 1; reserve 0 for the input file record sequence
    // number.
    private static final int      ID                               = 1;
    private static final int      DEPART                           = 2;
    private static final int      TYPE                             = 3;
    private static final int      ORIG                             = 4;
    private static final int      DEST                             = 5;
    private static final int      NUM_FIELDS                       = 5;

    private static final int      OD_TYPE_INDEX                    = 0;
    private static final int      OP_TYPE_INDEX                    = 1;
    private static final int      PD_TYPE_INDEX                    = 2;
    private static final int[]    TYPE_INDICES                     = {OD_TYPE_INDEX, OP_TYPE_INDEX,
            PD_TYPE_INDEX                                          };
    private static final String   OD_TYPE                          = "OD";
    private static final String   OP_TYPE                          = "OP";
    private static final String   PD_TYPE                          = "PD";
    private static final String[] TYPE_LABELS                      = {OD_TYPE, OP_TYPE, PD_TYPE};

    private static final int      AUTO_TIME_SKIM_INDEX             = 0;
    private static final int      AUTO_DIST_SKIM_INDEX             = 2;

    private static final double   WALK_SPEED                       = 3.0;                                                   // mph;

    private MatrixDataServerIf    ms;

    public ParkLocationEstimationAppender()
    {
    }

    private void runAppender(ResourceBundle rb)
    {

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        MgraDataManager mgraManager = MgraDataManager.getInstance(rbMap);
        SandagModelStructure modelStructure = new SandagModelStructure();

        double[] distances = new double[mgraManager.getMaxMgra() + 1];

        String outputFileName = Util.getStringValueFromPropertyMap(rbMap,
                OUTPUT_DATA_RECORDS_FILE_KEY);
        if (outputFileName == null)
        {
            logger.info("no output file name was specified in the properties file.  Nothing to do.");
            return;
        }

        int dotIndex = outputFileName.indexOf(".");
        String baseName = outputFileName.substring(0, dotIndex);
        String extension = outputFileName.substring(dotIndex);
        String outputName = baseName + extension;

        PrintWriter outStream = null;

        try
        {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputName))));
        } catch (IOException e)
        {
            logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                    outputFileName));
            throw new RuntimeException(e);
        }

        outStream
                .println("seq,id,origMgra,destMgra,departPeriod,index,autoDist,autoTime,walkDist,walkTime");

        TableDataSet estTds = getEstimationDataTableDataSet(rbMap);
        int[][] estDataOdts = getPlcEstimationData(estTds);

        AutoAndNonMotorizedSkimsCalculator anm = new AutoAndNonMotorizedSkimsCalculator(rbMap);

        // write skims data for estimation data file records
        int seq = 1;
        for (int i = 0; i < estDataOdts.length; i++)
        {

            int[] odtSet = estDataOdts[i];
            odtSet[0] = seq;

            double aDist = 999;
            double aTime = 999;
            double wDist = 999;
            double wTime = 999;
            if (odtSet[ORIG] > 0 && odtSet[DEST] > 0)
            {

                int skimPeriodIndex = modelStructure.getSkimPeriodIndex(odtSet[DEPART]) + 1; // depart
                                                                                             // skim
                                                                                             // period
                double[] autoSkims = anm.getAutoSkims(odtSet[ORIG], odtSet[DEST], skimPeriodIndex,
                        (seq == DEBUG_EST_RECORD), logger);
                aDist = autoSkims[AUTO_DIST_SKIM_INDEX];
                aTime = autoSkims[AUTO_TIME_SKIM_INDEX];

                // get the array of mgras within walking distance of the
                // destination
                int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceFrom(odtSet[ORIG]);

                // set the distance values for the mgras walkable to the
                // destination
                if (walkMgras != null)
                {

                    // get distances, in feet, and convert to miles
                    for (int wMgra : walkMgras)
                    {
                        if (wMgra == odtSet[DEST])
                        {
                            wDist = mgraManager.getMgraToMgraWalkDistFrom(odtSet[ORIG], wMgra) / 5280.0;
                            wTime = (wDist / WALK_SPEED) * 60.0;
                            break;
                        }
                    }
                }

            }

            outStream.println(seq + "," + odtSet[ID] + "," + odtSet[ORIG] + "," + odtSet[DEST]
                    + "," + odtSet[DEPART] + "," + TYPE_LABELS[odtSet[TYPE]] + "," + aDist + ","
                    + aTime + "," + wDist + "," + wTime);

            if (seq % 1000 == 0) logger.info("wrote PLC Estimation file record: " + seq);

            seq++;
        }

        outStream.close();

    }

    private int[][] getPlcEstimationData(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra,
        // departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[hisTds.getRowCount()][NUM_FIELDS + 1];

        int[] ids = hisTds.getColumnAsInt(ID_FIELD);
        int[] departs = hisTds.getColumnAsInt(DEPART_PERIOD_FIELD);
        String[] types = hisTds.getColumnAsString(TYPE_FIELD);
        int[] origs = hisTds.getColumnAsInt(ORIG_FIELD);
        int[] dests = hisTds.getColumnAsInt(DEST_FIELD);

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            odts[r - 1][ID] = ids[r - 1];
            odts[r - 1][DEPART] = departs[r - 1];

            for (int i = 0; i < TYPE_INDICES.length; i++)
            {
                if (types[r - 1].equalsIgnoreCase(TYPE_LABELS[i]))
                {
                    odts[r - 1][TYPE] = TYPE_INDICES[i];
                    break;
                }
            }

            odts[r - 1][ORIG] = origs[r - 1];
            odts[r - 1][DEST] = dests[r - 1];

        }

        return odts;
    }

    protected TableDataSet getEstimationDataTableDataSet(HashMap<String, String> rbMap)
    {

        String estFileName = Util.getStringValueFromPropertyMap(rbMap,
                ESTIMATION_DATA_RECORDS_FILE_KEY);
        if (estFileName == null)
        {
            logger.error("Error getting the filename from the properties file for the Sandag estimation data records file.");
            logger.error("Properties file target: " + ESTIMATION_DATA_RECORDS_FILE_KEY
                    + " not found.");
            logger.error("Please specify a filename value for the "
                    + ESTIMATION_DATA_RECORDS_FILE_KEY + " property.");
            throw new RuntimeException();
        }

        try
        {
            TableDataSet inTds = null;
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            inTds = reader.readFile(new File(estFileName));
            return inTds;
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading Sandag estimation data records file: %s into TableDataSet object.",
                            estFileName));
            throw new RuntimeException(e);
        }

    }

    /**
     * Start the matrix server
     * 
     * @param rb
     *            is a ResourceBundle for the properties file for this
     *            application
     */
    protected void startMatrixServer(ResourceBundle rb)
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

            logger.error("exception caught running ctramp model components -- exiting.", e);
            throw new RuntimeException();

        }

    }

    public static void main(String[] args)
    {

        ResourceBundle rb;
        if (args.length == 0)
        {
            System.out
                    .println("no properties file base name (without .properties extension) was specified as an argument.");
            return;
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
        }

        ParkLocationEstimationAppender appender = new ParkLocationEstimationAppender();

        appender.startMatrixServer(rb);
        appender.runAppender(rb);

    }

}
