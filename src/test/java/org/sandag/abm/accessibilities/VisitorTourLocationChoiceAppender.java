package org.sandag.abm.accessibilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.sandag.abm.application.SandagAppendMcLogsumDMU;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.ResourceUtil;

public final class VisitorTourLocationChoiceAppender
        extends McLogsumsAppender
{

    private static final int    DEBUG_EST_RECORD1          = 1;
    private static final int    DEBUG_EST_RECORD2          = -1;

    /*
     * for DC estimation file
     */
    private static final int    SEQ_FIELD                  = 2;

    // for Atwork subtour DC
    // private static final int ORIG_MGRA_FIELD = 79;
    // private static final int DEST_MGRA_FIELD = 220;
    // private static final int MGRA1_FIELD = 221;
    // private static final int PURPOSE_INDEX_OFFSET = 4;

    // for Escort DC
    private static final int    ORIG_MGRA_FIELD            = 76;
    private static final int    DEST_MGRA_FIELD            = 79;
    private static final int    MGRA1_FIELD                = 217;
    private static final int    PURPOSE_INDEX_OFFSET       = 0;

    // for NonMandatory DC
    // private static final int ORIG_MGRA_FIELD = 76;
    // private static final int DEST_MGRA_FIELD = 79;
    // private static final int MGRA1_FIELD = 221;
    // private static final int PURPOSE_INDEX_OFFSET = 0;

    private static final int    DEPART_PERIOD_FIELD        = 189;
    private static final int    ARRIVE_PERIOD_FIELD        = 190;
    private static final int    INCOME_FIELD               = 20;
    private static final int    ADULTS_FIELD               = 32;
    private static final int    AUTOS_FIELD                = 6;
    private static final int    HHSIZE_FIELD               = 5;
    private static final int    GENDER_FIELD               = 38;
    private static final int    AGE_FIELD                  = 39;
    private static final int    PURPOSE_FIELD              = 80;
    private static final int    JOINT_ID_FIELD             = 125;
    private static final int    JOINT_PURPOSE_FIELD        = 126;
    private static final int    JOINT_P1_FIELD             = 151;
    private static final int    JOINT_P2_FIELD             = 152;
    private static final int    JOINT_P3_FIELD             = 153;
    private static final int    JOINT_P4_FIELD             = 154;
    private static final int    JOINT_P5_FIELD             = 155;
    private static final int    NUM_MGRA_FIELDS            = 30;

    private static final String OUTPUT_SAMPLE_DEST_LOGSUMS = "output.sample.dest.logsums";

    public VisitorTourLocationChoiceAppender(HashMap<String, String> rbMap)
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
        modeChoiceLogsums = new double[NUM_MGRA_FIELDS + 1][];

        departArriveLogsums = new double[NUM_MGRA_FIELDS + 1][departArriveCombinations.length];

        String outputAllKey = Util.getStringValueFromPropertyMap(rbMap, OUTPUT_SAMPLE_DEST_LOGSUMS);

        String outputFileName = Util.getStringValueFromPropertyMap(rbMap,
                "dc.est.skims.output.file");
        if (outputFileName == null)
        {
            logger.info("no output file name was specified in the properties file.  Nothing to do.");
            return;
        }

        int dotIndex = outputFileName.indexOf(".");
        String baseName = outputFileName.substring(0, dotIndex);
        String extension = outputFileName.substring(dotIndex);

        // output1 is only written if "all" was set in propoerties file
        String outputName1 = "";
        if (outputAllKey.equalsIgnoreCase("all")) outputName1 = baseName + "_" + "all" + extension;

        // output1 is written in any case
        String outputName2 = baseName + "_" + "chosen" + extension;

        PrintWriter outStream1 = null;
        PrintWriter outStream2 = null;

        try
        {
            if (outputAllKey.equalsIgnoreCase("all"))
                outStream1 = new PrintWriter(new BufferedWriter(new FileWriter(
                        new File(outputName1))));
            outStream2 = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputName2))));
        } catch (IOException e)
        {
            logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                    outputFileName));
            throw new RuntimeException(e);
        }

        writeDcFile(rbMap, outStream1, outStream2);

        logger.info("total part 1 runtime = " + (totalTime1 / 1000) + " seconds.");
        logger.info("total part 2 runtime = " + (totalTime2 / 1000) + " seconds.");

    }

    private void writeDcFile(HashMap<String, String> rbMap, PrintWriter outStream1,
            PrintWriter outStream2)
    {

        // print the chosen destMgra and the depart/arrive logsum field names to
        // both
        // files
        if (outStream1 != null) outStream1.print("seq,sampno,chosenMgra");

        // attach the OB and IB period labels to the logsum field names for each
        // period
        if (outStream1 != null)
        {
            for (String[] labels : departArriveCombinationLabels)
                outStream1.print(",logsum" + labels[0] + labels[1]);
        }

        outStream2.print("seq,sampno,chosenMgra,chosenTodLogsum");

        // print each set of sample destMgra and the depart/arrive logsum
        // fieldnames
        // to file 1.
        // print each set of sample destMgra and the chosen depart/arrive logsum
        // fieldname to file 2.
        for (int m = 1; m < departArriveLogsums.length; m++)
        {
            if (outStream1 != null)
            {
                outStream1.print(",sampleMgra" + m);
                for (String[] labels : departArriveCombinationLabels)
                    outStream1.print(",logsum" + m + labels[0] + labels[1]);
            }

            outStream2.print(",sampleMgra" + m);
            outStream2.print(",sampleLogsum" + m);
        }
        if (outStream1 != null) outStream1.print("\n");
        outStream2.print("\n");

        TableDataSet estTds = getEstimationDataTableDataSet(rbMap);
        int[][] estDataOdts = getDcEstimationDataOrigDestTimes(estTds);

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = rbMap.get(PROPERTIES_UEC_TOUR_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        SandagAppendMcLogsumDMU mcDmuObject = new SandagAppendMcLogsumDMU(modelStructure, null);

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

            if (outStream1 != null)
            {
                outStream1.print(seq + "," + odtSet[SAMPNO]);
            }
            outStream2.print(seq + "," + odtSet[SAMPNO]);

            int category = PURPOSE_CATEGORIES[odtSet[TOUR_PURPOSE]];

            int[] departAvailable = {-1, 1, 1, 1, 1, 1};
            int[] arriveAvailable = {-1, 1, 1, 1, 1, 1};
            calculateModeChoiceLogsums(rbMap, category == -1 ? null : mcModel[category],
                    mcDmuObject, odtSet, mgraSet, departAvailable, arriveAvailable, false);

            // write chosen dest and logsums to both files
            if (outStream1 != null)
            {
                outStream1.print("," + odtSet[DEST_MGRA]);
                for (double logsum : departArriveLogsums[0])
                    outStream1.printf(",%.8f", logsum);
            }

            outStream2.print("," + odtSet[DEST_MGRA]);
            outStream2.printf(",%.8f", departArriveLogsums[0][chosenLogsumTodIndex]);

            // write logsum sets for each dest in the sample to file 1
            for (int m = 1; m < departArriveLogsums.length; m++)
            {
                if (outStream1 != null)
                {
                    outStream1.print("," + mgraSet[m - 1]);
                    for (double logsum : departArriveLogsums[m])
                        outStream1.printf(",%.8f", logsum);
                }

                outStream2.print("," + mgraSet[m - 1]);
                outStream2.printf(",%.8f", departArriveLogsums[m][chosenLogsumTodIndex]);
            }
            if (outStream1 != null) outStream1.print("\n");
            outStream2.print("\n");

            if (seq % 1000 == 0) logger.info("wrote DC Estimation file record: " + seq);

            seq++;
        }

        if (outStream1 != null) outStream1.close();
        outStream2.close();

    }

    private int[][] getDcEstimationDataOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra,
        // departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[hisTds.getRowCount()][NUM_FIELDS];
        mgras = new int[hisTds.getRowCount()][NUM_MGRA_FIELDS];
        int[][] mgraData = new int[NUM_MGRA_FIELDS][];

        int[] departs = hisTds.getColumnAsInt(DEPART_PERIOD_FIELD);
        int[] arrives = hisTds.getColumnAsInt(ARRIVE_PERIOD_FIELD);

        int[] hisseq = hisTds.getColumnAsInt(SEQ_FIELD);
        int[] purpose = hisTds.getColumnAsInt(PURPOSE_FIELD);
        int[] jtPurpose = hisTds.getColumnAsInt(JOINT_PURPOSE_FIELD);
        int[] income = hisTds.getColumnAsInt(INCOME_FIELD);
        int[] origs = hisTds.getColumnAsInt(ORIG_MGRA_FIELD);
        int[] dests = hisTds.getColumnAsInt(DEST_MGRA_FIELD);
        int[] adults = hisTds.getColumnAsInt(ADULTS_FIELD);
        int[] autos = hisTds.getColumnAsInt(AUTOS_FIELD);
        int[] hhsize = hisTds.getColumnAsInt(HHSIZE_FIELD);
        int[] gender = hisTds.getColumnAsInt(GENDER_FIELD);
        int[] age = hisTds.getColumnAsInt(AGE_FIELD);
        int[] jointId = hisTds.getColumnAsInt(JOINT_ID_FIELD);
        int[] jointPerson1Participates = hisTds.getColumnAsInt(JOINT_P1_FIELD);
        int[] jointPerson2Participates = hisTds.getColumnAsInt(JOINT_P2_FIELD);
        int[] jointPerson3Participates = hisTds.getColumnAsInt(JOINT_P3_FIELD);
        int[] jointPerson4Participates = hisTds.getColumnAsInt(JOINT_P4_FIELD);
        int[] jointPerson5Participates = hisTds.getColumnAsInt(JOINT_P5_FIELD);

        for (int i = 0; i < NUM_MGRA_FIELDS; i++)
            mgraData[i] = hisTds.getColumnAsInt(MGRA1_FIELD + i);

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            for (int i = 0; i < NUM_MGRA_FIELDS; i++)
                mgras[r - 1][i] = mgraData[i][r - 1];

            odts[r - 1][SAMPNO] = hisseq[r - 1];

            odts[r - 1][DEPART_PERIOD] = departs[r - 1];
            odts[r - 1][ARRIVE_PERIOD] = arrives[r - 1];

            odts[r - 1][ORIG_MGRA] = origs[r - 1];
            odts[r - 1][DEST_MGRA] = dests[r - 1];
            odts[r - 1][INCOME] = income[r - 1];
            odts[r - 1][ADULTS] = adults[r - 1];
            odts[r - 1][AUTOS] = autos[r - 1];
            odts[r - 1][HHSIZE] = hhsize[r - 1];
            odts[r - 1][FEMALE] = gender[r - 1] == 2 ? 1 : 0;
            odts[r - 1][AGE] = age[r - 1];
            odts[r - 1][JOINT] = jointId[r - 1] > 0 ? 1 : 0;

            // the offest constant is used because at-work subtours in
            // estimation file are coded as work purpose index (=1),
            // but the model index to use is 5. Nonmandatory and escort files
            // have correct purpose codes, so offset is 0.
            int purposeIndex = purpose[r - 1] + PURPOSE_INDEX_OFFSET;

            odts[r - 1][ESCORT] = purposeIndex == 4 ? 1 : 0;

            odts[r - 1][PARTYSIZE] = jointPerson1Participates[r - 1]
                    + jointPerson2Participates[r - 1] + jointPerson3Participates[r - 1]
                    + jointPerson4Participates[r - 1] + jointPerson5Participates[r - 1];

            odts[r - 1][TOUR_PURPOSE] = odts[r - 1][JOINT] == 1 && purposeIndex > 4 ? jtPurpose[r - 1]
                    : purposeIndex;

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

        VisitorTourLocationChoiceAppender appender = new VisitorTourLocationChoiceAppender(rbMap);

        appender.startMatrixServer(rb);
        appender.runLogsumAppender(rb);

    }

}
