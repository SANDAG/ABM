package org.sandag.abm.accessibilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.sandag.abm.application.SandagTripModeChoiceDMU;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.newmodel.ChoiceModelApplication;

public final class StopLocationEstimationMcLogsumsAppender
        extends McLogsumsAppender
{

    private static final int DEBUG_EST_RECORD1         = 266;
    private static final int DEBUG_EST_RECORD2         = -1;

    /*
     * for stop location choice estimation file
     */
    private static final int PORTION_FIELD             = 1;
    private static final int SAMPNO_FIELD              = 2;
    private static final int PERNO_FIELD               = 3;
    private static final int TOUR_ID_FIELD             = 4;
    private static final int TRIPNO_FIELD              = 5;
    private static final int STOPID_FIELD              = 6;
    private static final int STOPNO_FIELD              = 7;

    private static final int ORIG_MGRA_FIELD           = 16;
    private static final int DEST_MGRA_FIELD           = 23;
    private static final int CHOSEN_MGRA_FIELD         = 17;
    private static final int MGRA1_FIELD               = 49;

    private static final int TOUR_DEPART_PERIOD_FIELD  = 40;
    private static final int TOUR_ARRIVE_PERIOD_FIELD  = 41;
    private static final int TRIP_START_PERIOD_FIELD   = 29;
    private static final int TOUR_MODE_FIELD           = 30;
    private static final int INCOME_FIELD              = 24;
    private static final int ADULTS_FIELD              = 47;
    private static final int AUTOS_FIELD               = 28;
    private static final int HHSIZE_FIELD              = 27;
    private static final int GENDER_FIELD              = 26;
    private static final int OUT_STOPS_FIELD           = 45;
    private static final int IN_STOPS_FIELD            = 46;
    private static final int FIRST_TRIP_FIELD          = 43;
    private static final int LAST_TRIP_FIELD           = 44;
    private static final int PURPOSE_FIELD             = 12;
    private static final int AGE_FIELD                 = 25;
    private static final int DIR_FIELD                 = 42;
    private static final int J_TOUR_ID_FIELD           = 10;
    private static final int J_TOUR_PARTICIPANTS_FIELD = 11;
    private static final int NUM_MGRA_FIELDS           = 30;

    public StopLocationEstimationMcLogsumsAppender(HashMap<String, String> rbMap)
    {
        super(rbMap);

        debugEstimationFileRecord1 = DEBUG_EST_RECORD1;
        debugEstimationFileRecord2 = DEBUG_EST_RECORD2;

        numMgraFields = NUM_MGRA_FIELDS;
    }

    private void runLogsumAppender(ResourceBundle rb)
    {

        totalTime1 = 0;
        totalTime2 = 0;

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        tazs = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);

        // create modelStructure object
        modelStructure = new SandagModelStructure();

        mgraSetForLogsums = new int[numMgraFields + 1];

        // allocate the logsums array for the chosen destination alternative
        tripModeChoiceLogsums = new double[NUM_MGRA_FIELDS + 1][2];

        departArriveLogsums = new double[NUM_MGRA_FIELDS + 1][departArriveCombinations.length];

        String outputFileName = Util.getStringValueFromPropertyMap(rbMap,
                "slc.est.skims.output.file");
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

        writeDcFile(rbMap, outStream);

        logger.info("total part 1 runtime = " + (totalTime1 / 1000) + " seconds.");
        logger.info("total part 2 runtime = " + (totalTime2 / 1000) + " seconds.");

    }

    private void writeDcFile(HashMap<String, String> rbMap, PrintWriter outStream)
    {

        outStream
                .print("seq,portion,sampn,perno,tour_id,tripno,stopid,stopno,chosenMgra,chosenMgraLogsumIK,chosenMgraLogsumKJ");

        // print each set of sample destMgra and the depart/arrive logsum
        // fieldnames
        // to file 1.
        // print each set of sample destMgra and the chosen depart/arrive logsum
        // fieldname to file 2.
        for (int m = 1; m < tripModeChoiceLogsums.length; m++)
        {
            outStream.print(",sampleMgra_" + m);
            outStream.print(",sampleLogsumIK_" + m);
            outStream.print(",sampleLogsumKJ_" + m);
        }
        outStream.print("\n");

        TableDataSet estTds = getEstimationDataTableDataSet(rbMap);
        int[][] estDataOdts = getDcEstimationDataOrigDestTimes(estTds);

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = rbMap.get(PROPERTIES_UEC_TRIP_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        SandagTripModeChoiceDMU mcDmuObject = new SandagTripModeChoiceDMU(modelStructure);

        ChoiceModelApplication[] mcModel = new ChoiceModelApplication[5 + 1];
        mcModel[WORK_CATEGORY] = new ChoiceModelApplication(mcUecFile, WORK_SHEET, 0, rbMap,
                (VariableTable) mcDmuObject);
        mcModel[UNIVERSITY_CATEGORY] = new ChoiceModelApplication(mcUecFile, UNIVERSITY_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[SCHOOL_CATEGORY] = new ChoiceModelApplication(mcUecFile, SCHOOL_SHEET, 0, rbMap,
                (VariableTable) mcDmuObject);
        mcModel[MAINTENANCE_CATEGORY] = new ChoiceModelApplication(mcUecFile, MAINTENANCE_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[DISCRETIONARY_CATEGORY] = new ChoiceModelApplication(mcUecFile,
                DISCRETIONARY_SHEET, 0, rbMap, (VariableTable) mcDmuObject);
        mcModel[SUBTOUR_CATEGORY] = new ChoiceModelApplication(mcUecFile, SUBTOUR_SHEET, 0, rbMap,
                (VariableTable) mcDmuObject);

        // write skims data for estimation data file records
        int seq = 1;
        for (int i = 0; i < estDataOdts.length; i++)
        {

            int[] odtSet = estDataOdts[i];
            int[] mgraSet = mgras[i];

            odtSet[0] = seq;

            outStream.print(seq + "," + odtSet[PORTION] + "," + odtSet[SAMPNO] + ","
                    + odtSet[PERNO] + "," + odtSet[TOUR_ID] + "," + odtSet[TRIPNO] + ","
                    + odtSet[STOPID] + "," + odtSet[STOPNO]);

            int category = PURPOSE_CATEGORIES[odtSet[TOUR_PURPOSE]];

            try
            {
                calculateTripModeChoiceLogsums(rbMap, mcModel[category], mcDmuObject, odtSet,
                        mgraSet);
            } catch (Exception e)
            {
                logger.error("exception caught processing survey record for i =  " + i);
                throw new RuntimeException();
            }

            outStream.print("," + odtSet[CHOSEN_MGRA]);
            outStream
                    .printf(",%.8f,%.8f", tripModeChoiceLogsums[0][0], tripModeChoiceLogsums[0][1]);

            // write logsum sets for each dest in the sample to file 1
            for (int m = 1; m < tripModeChoiceLogsums.length; m++)
            {
                outStream.print("," + mgraSet[m - 1]);
                outStream.printf(",%.8f,%.8f", tripModeChoiceLogsums[m][0],
                        tripModeChoiceLogsums[m][1]);
            }
            outStream.print("\n");

            if (seq % 1000 == 0) logger.info("wrote DC Estimation file record: " + seq);

            seq++;
        }

        outStream.close();

    }

    private int[][] getDcEstimationDataOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra,
        // departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[hisTds.getRowCount()][NUM_FIELDS];
        mgras = new int[hisTds.getRowCount()][NUM_MGRA_FIELDS];
        int[][] mgraData = new int[NUM_MGRA_FIELDS][];

        int[] tourDeparts = hisTds.getColumnAsInt(TOUR_DEPART_PERIOD_FIELD);
        int[] tourArrives = hisTds.getColumnAsInt(TOUR_ARRIVE_PERIOD_FIELD);
        int[] tripStarts = hisTds.getColumnAsInt(TRIP_START_PERIOD_FIELD);

        int[] direction = hisTds.getColumnAsInt(DIR_FIELD);

        int[] portion = hisTds.getColumnAsInt(PORTION_FIELD);
        int[] sampno = hisTds.getColumnAsInt(SAMPNO_FIELD);
        int[] perno = hisTds.getColumnAsInt(PERNO_FIELD);
        int[] tour_id = hisTds.getColumnAsInt(TOUR_ID_FIELD);
        int[] tripno = hisTds.getColumnAsInt(TRIPNO_FIELD);
        int[] stopid = hisTds.getColumnAsInt(STOPID_FIELD);
        int[] stopno = hisTds.getColumnAsInt(STOPNO_FIELD);
        int[] purpose = hisTds.getColumnAsInt(PURPOSE_FIELD);
        int[] jTourId = hisTds.getColumnAsInt(J_TOUR_ID_FIELD);
        int[] jTourParticipants = hisTds.getColumnAsInt(J_TOUR_PARTICIPANTS_FIELD);
        int[] income = hisTds.getColumnAsInt(INCOME_FIELD);
        int[] mode = hisTds.getColumnAsInt(TOUR_MODE_FIELD);
        int[] origs = hisTds.getColumnAsInt(ORIG_MGRA_FIELD);
        int[] dests = hisTds.getColumnAsInt(DEST_MGRA_FIELD);
        int[] chosen = hisTds.getColumnAsInt(CHOSEN_MGRA_FIELD);
        int[] adults = hisTds.getColumnAsInt(ADULTS_FIELD);
        int[] age = hisTds.getColumnAsInt(AGE_FIELD);
        int[] autos = hisTds.getColumnAsInt(AUTOS_FIELD);
        int[] hhsize = hisTds.getColumnAsInt(HHSIZE_FIELD);
        int[] gender = hisTds.getColumnAsInt(GENDER_FIELD);
        int[] outStops = hisTds.getColumnAsInt(OUT_STOPS_FIELD);
        int[] inStops = hisTds.getColumnAsInt(IN_STOPS_FIELD);
        int[] firstTrip = hisTds.getColumnAsInt(FIRST_TRIP_FIELD);
        int[] lastTrip = hisTds.getColumnAsInt(LAST_TRIP_FIELD);

        for (int i = 0; i < NUM_MGRA_FIELDS; i++)
            mgraData[i] = hisTds.getColumnAsInt(MGRA1_FIELD + i);

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            for (int i = 0; i < NUM_MGRA_FIELDS; i++)
                mgras[r - 1][i] = mgraData[i][r - 1];

            odts[r - 1][PORTION] = portion[r - 1];
            odts[r - 1][SAMPNO] = sampno[r - 1];
            odts[r - 1][PERNO] = perno[r - 1];
            odts[r - 1][TOUR_ID] = tour_id[r - 1];
            odts[r - 1][TRIPNO] = tripno[r - 1];
            odts[r - 1][STOPID] = stopid[r - 1];
            odts[r - 1][STOPNO] = stopno[r - 1];

            odts[r - 1][DEPART_PERIOD] = tourDeparts[r - 1];
            odts[r - 1][ARRIVE_PERIOD] = tourArrives[r - 1];
            odts[r - 1][TRIP_PERIOD] = tripStarts[r - 1];

            odts[r - 1][DIRECTION] = direction[r - 1];

            odts[r - 1][ORIG_MGRA] = origs[r - 1];
            odts[r - 1][DEST_MGRA] = dests[r - 1];
            odts[r - 1][CHOSEN_MGRA] = chosen[r - 1];
            odts[r - 1][TOUR_MODE] = mode[r - 1];
            odts[r - 1][INCOME] = income[r - 1];
            odts[r - 1][ADULTS] = adults[r - 1];
            odts[r - 1][AUTOS] = autos[r - 1];
            odts[r - 1][AGE] = age[r - 1];
            odts[r - 1][HHSIZE] = hhsize[r - 1];
            odts[r - 1][FEMALE] = gender[r - 1] == 2 ? 1 : 0;
            odts[r - 1][FIRST_TRIP] = firstTrip[r - 1];
            odts[r - 1][LAST_TRIP] = lastTrip[r - 1];
            odts[r - 1][OUT_STOPS] = outStops[r - 1];
            odts[r - 1][IN_STOPS] = inStops[r - 1];

            odts[r - 1][TOUR_PURPOSE] = purpose[r - 1];
            odts[r - 1][JOINT] = jTourId[r - 1] > 0 ? 1 : 0;
            odts[r - 1][PARTYSIZE] = jTourParticipants[r - 1];

        }

        return odts;
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

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        StopLocationEstimationMcLogsumsAppender appender = new StopLocationEstimationMcLogsumsAppender(
                rbMap);

        appender.startMatrixServer(rb);
        appender.runLogsumAppender(rb);

    }

}
