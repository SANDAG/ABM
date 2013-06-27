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
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.newmodel.ChoiceModelApplication;

public final class SubtourTodEstimationMcLogsumsAppender
        extends McLogsumsAppender
{

    private static final int DEBUG_EST_RECORD1        = 1;
    private static final int DEBUG_EST_RECORD2        = -1;

    private static final int SUBTOUR_PURPOSE          = 10;
    
    private static final int SEQ_FIELD                = 7;
    private static final int ORIG_MGRA_FIELD          = 81;
    private static final int DEST_MGRA_FIELD          = 89;
    private static final int DEPART_PERIOD_FIELD      = 3;
    private static final int ARRIVE_PERIOD_FIELD      = 4;
    private static final int WORK_TOUR_MODE_FIELD     = 98;
    private static final int AUTOS_FIELD              = 11;
    private static final int AGE_FIELD                = 44;
    private static final int MGRA1_FIELD              = 1;
    private static final int NUM_MGRA_FIELDS          = 0;

    private SubtourTodEstimationMcLogsumsAppender(HashMap<String, String> rbMap)
    {
        super(rbMap);

        debugEstimationFileRecord1 = DEBUG_EST_RECORD1;
        debugEstimationFileRecord2 = DEBUG_EST_RECORD2;

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
        modeChoiceLogsums = new double[1][];

        departArriveLogsums = new double[1][departArriveCombinations.length];

        String outputFileName = Util.getStringValueFromPropertyMap(rbMap, "tod.est.skims.output.file");

        PrintWriter outStream = null;

        if (outputFileName == null)
        {
            logger
                    .info("no output file name was specified in the properties file.  Nothing to do.");
            return;
        }

        try
        {
            outStream = new PrintWriter(
                    new BufferedWriter(new FileWriter(new File(outputFileName))));
        } catch (IOException e)
        {
            logger.fatal(String.format("Exception occurred opening output skims file: %s.",
                    outputFileName));
            throw new RuntimeException(e);
        }

        writeTodFile(rbMap, outStream);

        logger.info("total part 1 runtime = " + (totalTime1 / 1000) + " seconds.");
        logger.info("total part 2 runtime = " + (totalTime2 / 1000) + " seconds.");

    }

    private void writeTodFile(HashMap<String, String> rbMap, PrintWriter outStream2)
    {

        // print the chosen destMgra and the depart/arrive logsum field names to the
        // file
        outStream2.print("seq,hisseq,chosenMgra");
        for (String[] labels : departArriveCombinationLabels)
        {
            outStream2.print(",logsum" + labels[0] + labels[1]);
        }
        outStream2.print("\n");

        TableDataSet estTds = getEstimationDataTableDataSet(rbMap);
        int[][] estDataOdts = getTodEstimationDataOrigDestTimes(estTds);

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = rbMap.get(PROPERTIES_UEC_TOUR_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;


        SandagAppendMcLogsumDMU mcDmuObject = new SandagAppendMcLogsumDMU(modelStructure);

        ChoiceModelApplication[] mcModel = new ChoiceModelApplication[5 + 1];
        mcModel[WORK_CATEGORY] = new ChoiceModelApplication(mcUecFile, WORK_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[UNIVERSITY_CATEGORY] = new ChoiceModelApplication(mcUecFile, UNIVERSITY_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[SCHOOL_CATEGORY] = new ChoiceModelApplication(mcUecFile, SCHOOL_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[MAINTENANCE_CATEGORY] = new ChoiceModelApplication(mcUecFile, MAINTENANCE_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[DISCRETIONARY_CATEGORY] = new ChoiceModelApplication(mcUecFile, DISCRETIONARY_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
        mcModel[SUBTOUR_CATEGORY] = new ChoiceModelApplication(mcUecFile, SUBTOUR_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);

        // write skims data for estimation data file records
        int seq = 1;
        for (int i = 0; i < estDataOdts.length; i++)
        {

            int[] odtSet = estDataOdts[i];
            int[] mgraSet = mgras[i];

            odtSet[0] = seq;

            outStream2.print(seq + "," + odtSet[SAMPNO]);

            int category = PURPOSE_CATEGORIES[odtSet[TOUR_PURPOSE]];

            int[] departAvailable = {-1, 1, 1, 1, 1, 1};
            int[] arriveAvailable = {-1, 1, 1, 1, 1, 1};
            calculateModeChoiceLogsums(rbMap, category == -1 ? null : mcModel[category],
                    mcDmuObject, odtSet, mgraSet, departAvailable, arriveAvailable, false);

            // write chosen dest and logsums to both files
            outStream2.print("," + odtSet[DEST_MGRA]);
            for (double logsum : departArriveLogsums[0])
            {
                outStream2.printf(",%.8f", logsum);
            }
            outStream2.print("\n");

            if (seq % 1000 == 0) logger.info("wrote TOD Estimation file record: " + seq);

            seq++;
        }

        outStream2.close();

    }

    private int[][] getTodEstimationDataOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra, departure
        // period(1-6), and arrival period(1-6).
        int[][] odts = new int[hisTds.getRowCount()][NUM_FIELDS];
        mgras = new int[hisTds.getRowCount()][NUM_MGRA_FIELDS];
        int[][] mgraData = new int[NUM_MGRA_FIELDS][];

        int[] departs = hisTds.getColumnAsInt(DEPART_PERIOD_FIELD);
        int[] arrives = hisTds.getColumnAsInt(ARRIVE_PERIOD_FIELD);

        int[] hisseq = hisTds.getColumnAsInt(SEQ_FIELD);
        int[] origs = hisTds.getColumnAsInt(ORIG_MGRA_FIELD);
        int[] dests = hisTds.getColumnAsInt(DEST_MGRA_FIELD);
        int[] autos = hisTds.getColumnAsInt(AUTOS_FIELD);
        int[] age = hisTds.getColumnAsInt(AGE_FIELD);
        int[] workTourMode = hisTds.getColumnAsInt(WORK_TOUR_MODE_FIELD);

        for (int i = 0; i < NUM_MGRA_FIELDS; i++)
            mgraData[i] = hisTds.getColumnAsInt(MGRA1_FIELD + i);

        for (int r = 1; r <= hisTds.getRowCount(); r++)
        {
            for (int i = 0; i < NUM_MGRA_FIELDS; i++)
                mgras[r - 1][i] = mgraData[i][r - 1];

            odts[r - 1][SAMPNO] = hisseq[r - 1];

            odts[r - 1][TOUR_PURPOSE] = SUBTOUR_PURPOSE;
            
            odts[r - 1][DEPART_PERIOD] = departs[r - 1];
            odts[r - 1][ARRIVE_PERIOD] = arrives[r - 1];

            odts[r - 1][ORIG_MGRA] = origs[r - 1];
            odts[r - 1][DEST_MGRA] = dests[r - 1];
            odts[r - 1][AUTOS] = autos[r - 1];
            odts[r - 1][AGE] = age[r - 1];
            odts[r - 1][WORK_TOUR_MODE] = workTourMode[r - 1];

        }

        return odts;
    }

    public static void main(String[] args)
    {

        long startTime = System.currentTimeMillis();

        ResourceBundle rb;
        if (args.length == 0)
        {
            System.out.println("no properties file base name (without .properties extension) was specified as an argument.");
            return;
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
        }

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        SubtourTodEstimationMcLogsumsAppender appender = new SubtourTodEstimationMcLogsumsAppender(rbMap);

        appender.startMatrixServer(rb);
        appender.runLogsumAppender(rb);


/*
 * used this to read/parse the UEC expressions - debugging the UEC sheet.
 *         
        HashMap<String,String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);
        
        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = rbMap.get(PROPERTIES_UEC_TOUR_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        ModelStructure modelStructure = new SandagModelStructure();
        SandagAppendMcLogsumDMU mcDmuObject = new SandagAppendMcLogsumDMU(modelStructure);
        ChoiceModelApplication mcModel = new ChoiceModelApplication(mcUecFile, SUBTOUR_SHEET, 0,
                rbMap, (VariableTable) mcDmuObject);
*/        
        

        System.out.println("total runtime = " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds.");

    }

}
