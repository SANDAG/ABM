package org.sandag.abm.accessibilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.sandag.abm.application.SandagTripModeChoiceDMU;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public final class StopLocationSampleCalculator
        extends McLogsumsAppender
{

    private static final int    DEBUG_EST_RECORD                      = 5;

    /*
     * for stop location choice estimation file
     */
    private static final int    PORTION_FIELD                         = 1;
    private static final int    SAMPNO_FIELD                          = 2;
    private static final int    PERNO_FIELD                           = 3;
    private static final int    TOUR_ID_FIELD                         = 4;
    private static final int    TRIPNO_FIELD                          = 5;
    private static final int    STOPID_FIELD                          = 6;
    private static final int    STOPNO_FIELD                          = 7;

    private static final int    ORIG_MGRA_FIELD                       = 16;
    private static final int    DEST_MGRA_FIELD                       = 23;
    private static final int    CHOSEN_MGRA_FIELD                     = 17;
    private static final int    MGRA1_FIELD                           = 49;

    private static final int    TOUR_DEPART_PERIOD_FIELD              = 40;
    private static final int    TOUR_ARRIVE_PERIOD_FIELD              = 41;
    private static final int    TRIP_START_PERIOD_FIELD               = 29;
    private static final int    TOUR_MODE_FIELD                       = 30;
    private static final int    INCOME_FIELD                          = 24;
    private static final int    ADULTS_FIELD                          = 47;
    private static final int    AUTOS_FIELD                           = 28;
    private static final int    HHSIZE_FIELD                          = 27;
    private static final int    GENDER_FIELD                          = 26;
    private static final int    OUT_STOPS_FIELD                       = 45;
    private static final int    IN_STOPS_FIELD                        = 46;
    private static final int    FIRST_TRIP_FIELD                      = 43;
    private static final int    LAST_TRIP_FIELD                       = 44;
    private static final int    TOUR_PURPOSE_FIELD                    = 12;
    private static final int    STOP_PURPOSE_FIELD                    = 12;
    private static final int    AGE_FIELD                             = 25;
    private static final int    DIR_FIELD                             = 42;
    private static final int    J_TOUR_ID_FIELD                       = 10;
    private static final int    J_TOUR_PARTICIPANTS_FIELD             = 11;
    private static final int    NUM_MGRA_FIELDS                       = 30;

    private static final String PROPERTIES_UEC_SLC_SOA_CHOICE         = "slc.soa.uec.file";
    private static final String PROPERTIES_UEC_STOP_SOA_SIZE          = "slc.soa.size.uec.file";
    private static final String PROPERTIES_UEC_STOP_SOA_SIZE_DATA     = "slc.soa.size.uec.data.page";
    private static final String PROPERTIES_UEC_STOP_SOA_SIZE_MODEL    = "slc.soa.size.uec.model.page";

    private static final int    WORK_STOP_PURPOSE_INDEX               = 1;
    private static final int    UNIV_STOP_PURPOSE_INDEX               = 2;
    private static final int    ESCORT_STOP_PURPOSE_INDEX             = 4;
    private static final int    SHOP_STOP_PURPOSE_INDEX               = 5;
    private static final int    MAINT_STOP_PURPOSE_INDEX              = 6;
    private static final int    EAT_OUT_STOP_PURPOSE_INDEX            = 7;
    private static final int    VISIT_STOP_PURPOSE_INDEX              = 8;
    private static final int    DISCR_STOP_PURPOSE_INDEX              = 9;
    private static final int    MAX_STOP_PURPOSE_INDEX                = 9;

    private static final int    WORK_STOP_PURPOSE_SOA_SIZE_INDEX      = 0;
    private static final int    UNIV_STOP_PURPOSE_SOA_SIZE_INDEX      = 1;
    private static final int    ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX  = 2;
    private static final int    ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX = 3;
    private static final int    ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX = 4;
    private static final int    ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX = 5;
    private static final int    SHOP_STOP_PURPOSE_SOA_SIZE_INDEX      = 6;
    private static final int    MAINT_STOP_PURPOSE_SOA_SIZE_INDEX     = 7;
    private static final int    EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX   = 8;
    private static final int    VISIT_STOP_PURPOSE_SOA_SIZE_INDEX     = 9;
    private static final int    DISCR_STOP_PURPOSE_SOA_SIZE_INDEX     = 10;

    private static final int    AUTO_DIST_SKIM_INDEX                  = 2;

    private McLogsumsCalculator logsumHelper;

    public StopLocationSampleCalculator(HashMap<String, String> rbMap)
    {
        super(rbMap);
        debugEstimationFileRecord1 = DEBUG_EST_RECORD;

    }

    private void runSampleProbabilitiesCalculator(ResourceBundle rb)
    {

        HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        tazs = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);

        // create modelStructure object
        modelStructure = new SandagModelStructure();

        TableDataSet estTds = getEstimationDataTableDataSet(rbMap);
        int[][] estDataOdts = getDcEstimationDataOrigDestTimes(estTds);

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String mcUecFile = rbMap.get(PROPERTIES_UEC_TRIP_MODE_CHOICE);
        mcUecFile = uecPath + mcUecFile;

        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(rbMap);

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

        /*
         * SandagStopDCSoaDMU slcSoaDmuObject = new SandagStopDCSoaDMU(modelStructure);
         * 
         * String slcSoaUecFile = rbMap.get(PROPERTIES_UEC_SLC_SOA_CHOICE); slcSoaUecFile = uecPath + slcSoaUecFile;
         * 
         * ChoiceModelApplication[] slcSoaModel = new ChoiceModelApplication[MAX_STOP_PURPOSE_INDEX+1]; slcSoaModel[WORK_STOP_PURPOSE_INDEX] = new
         * ChoiceModelApplication(slcSoaUecFile, WORK_STOP_PURPOSE_INDEX, 0, rbMap, (VariableTable) slcSoaDmuObject);
         * slcSoaModel[UNIV_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile, UNIV_STOP_PURPOSE_INDEX, 0, rbMap, (VariableTable)
         * slcSoaDmuObject); slcSoaModel[ESCORT_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile, ESCORT_STOP_PURPOSE_INDEX, 0, rbMap,
         * (VariableTable) slcSoaDmuObject); slcSoaModel[SHOP_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile, SHOP_STOP_PURPOSE_INDEX,
         * 0, rbMap, (VariableTable) slcSoaDmuObject); slcSoaModel[EAT_OUT_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile,
         * EAT_OUT_STOP_PURPOSE_INDEX, 0, rbMap, (VariableTable) slcSoaDmuObject); slcSoaModel[MAINT_STOP_PURPOSE_INDEX] = new
         * ChoiceModelApplication(slcSoaUecFile, MAINT_STOP_PURPOSE_INDEX, 0, rbMap, (VariableTable) slcSoaDmuObject);
         * slcSoaModel[VISIT_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile, VISIT_STOP_PURPOSE_INDEX, 0, rbMap, (VariableTable)
         * slcSoaDmuObject); slcSoaModel[DISCR_STOP_PURPOSE_INDEX] = new ChoiceModelApplication(slcSoaUecFile, DISCR_STOP_PURPOSE_INDEX, 0, rbMap,
         * (VariableTable) slcSoaDmuObject);
         */

        double absoulteDistanceDeviationCoefficient = -0.05;

        int i = DEBUG_EST_RECORD - 1;

        int[] odtSet = estDataOdts[i];

        int mgra = mgras[i][24];

        int category = PURPOSE_CATEGORIES[odtSet[TOUR_PURPOSE]];
        // int stopPurpose = odtSet[STOP_PURPOSE];
        int stopPurpose = 6;

        double[] logsums = null;
        try
        {
            logsums = calculateTripModeChoiceLogsumForEstimationRecord(rbMap, mcModel[category],
                    mcDmuObject, odtSet, mgra);
        } catch (Exception e)
        {
            logger.error("exception caught calculating trip mode choice logsum for survey record i =  "
                    + (i + 1));
            throw new RuntimeException();
        }

        double[] slcSoaSizeTerms = null;
        try
        {
            boolean preSchoolInHh = false;
            boolean gradeSchoolInHh = false;
            boolean highSchoolInHh = false;
            double[][] sizeTerms = calculateSlcSoaSizeTerms(rbMap, mgra);
            slcSoaSizeTerms = getSlcSoaSizeTermsForStopPurpose(stopPurpose, preSchoolInHh,
                    gradeSchoolInHh, highSchoolInHh, sizeTerms);
        } catch (Exception e)
        {
            logger.error("exception caught calculating stop location choice size terms for survey record i =  "
                    + (i + 1));
            throw new RuntimeException();
        }

        try
        {
            int origMgra = odtSet[ORIG_MGRA];
            int destMgra = odtSet[DEST_MGRA];
            int departPeriod = odtSet[TRIP_PERIOD]; // depart period
            int skimPeriodIndex = modelStructure.getSkimPeriodIndex(departPeriod) + 1; // depart skim period
            double odDist = logsumHelper.getAnmSkimCalculator().getAutoSkims(origMgra, destMgra,
                    skimPeriodIndex, false, logger)[AUTO_DIST_SKIM_INDEX];
            double osDist = logsumHelper.getAnmSkimCalculator().getAutoSkims(origMgra, mgra,
                    skimPeriodIndex, false, logger)[AUTO_DIST_SKIM_INDEX];
            double sdDist = logsumHelper.getAnmSkimCalculator().getAutoSkims(mgra, destMgra,
                    skimPeriodIndex, false, logger)[AUTO_DIST_SKIM_INDEX];
            double distance = osDist + sdDist - odDist;
            int availability = 1;

            boolean walkTransitIsAvailable = false;
            if (mgraManager.getMgraWlkTapsDistArray()[mgra][0] != null)
                walkTransitIsAvailable = true;

            double util = -999;
            if (availability == 1)
                util = Math.log(slcSoaSizeTerms[mgra]) + absoulteDistanceDeviationCoefficient
                        * distance;

            logUtilityCalculation(origMgra, mgra, destMgra, departPeriod, skimPeriodIndex, osDist,
                    sdDist, odDist, distance, slcSoaSizeTerms[mgra], util, logsums);
        } catch (Exception e)
        {
            logger.error("exception caught calculating and logging utility calculation for survey record i =  "
                    + (i + 1));
            throw new RuntimeException();
        }

    }

    private void logUtilityCalculation(int origMgra, int sampleMgra, int destMgra,
            int departPeriodIndex, int skimPeriodIndex, double osDist, double sdDist,
            double odDist, double distance, double size, double util, double[] logsums)
    {

        // write UEC calculation results to logsum specific log file if
        // its the chosen dest and its the chosen time combo
        slcSoaLogger.info("Stop Location Sample Probabilities Calculation:");
        slcSoaLogger.info("");
        slcSoaLogger
                .info("--------------------------------------------------------------------------------------------------------");
        slcSoaLogger.info("origin mgra = " + origMgra);
        slcSoaLogger.info("sample destination mgra = " + sampleMgra);
        slcSoaLogger.info("final destination mgra = " + destMgra);
        slcSoaLogger.info("origin taz = " + mgraManager.getTaz(origMgra));
        slcSoaLogger.info("sample destination taz = " + mgraManager.getTaz(sampleMgra));
        slcSoaLogger.info("final destination taz = " + mgraManager.getTaz(destMgra));
        slcSoaLogger.info("depart period interval = " + departPeriodIndex);
        slcSoaLogger.info("skim period index = " + skimPeriodIndex);
        slcSoaLogger.info("orig to stop distance = " + osDist);
        slcSoaLogger.info("stop to dest distance = " + sdDist);
        slcSoaLogger.info("orig to dest distance = " + odDist);
        slcSoaLogger.info("distance = " + distance);
        slcSoaLogger.info("size = " + size);
        slcSoaLogger.info("");
        slcSoaLogger.info("util = size * exp ( -0.05*distance )");
        slcSoaLogger.info("util = " + util);
        slcSoaLogger.info("os logsum = " + logsums[0]);
        slcSoaLogger.info("sd logsum = " + logsums[1]);
        slcSoaLogger
                .info("--------------------------------------------------------------------------------------------------------");
        slcSoaLogger.info("");

    }

    private double[] getSlcSoaSizeTermsForStopPurpose(int stopPurpose, boolean preSchoolInHh,
            boolean gradeSchoolInHh, boolean highSchoolInHh, double[][] sizeTerms)
    {

        double[] slcSoaSizeTerms = null;
        switch (stopPurpose)
        {

            case WORK_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[WORK_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case UNIV_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[UNIV_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case ESCORT_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[ESCORT_0_STOP_PURPOSE_SOA_SIZE_INDEX];

                // add preschool size term if the hh has a preschool child
                if (preSchoolInHh)
                {
                    for (int j = 0; j < sizeTerms[ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX].length; j++)
                        slcSoaSizeTerms[j] += sizeTerms[ESCORT_PS_STOP_PURPOSE_SOA_SIZE_INDEX][j];
                }

                // add grade school size term if the hh has a grade school child
                if (gradeSchoolInHh)
                {
                    for (int j = 0; j < sizeTerms[ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX].length; j++)
                        slcSoaSizeTerms[j] += sizeTerms[ESCORT_GS_STOP_PURPOSE_SOA_SIZE_INDEX][j];
                }

                // add high school size term if the hh has a high school child
                if (highSchoolInHh)
                {
                    for (int j = 0; j < sizeTerms[ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX].length; j++)
                        slcSoaSizeTerms[j] += sizeTerms[ESCORT_HS_STOP_PURPOSE_SOA_SIZE_INDEX][j];
                }
                break;
            case SHOP_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[SHOP_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case MAINT_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[MAINT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case EAT_OUT_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[EAT_OUT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case VISIT_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[VISIT_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
            case DISCR_STOP_PURPOSE_INDEX:
                slcSoaSizeTerms = sizeTerms[DISCR_STOP_PURPOSE_SOA_SIZE_INDEX];
                break;
        }

        return slcSoaSizeTerms;

    }

    private double[][] calculateSlcSoaSizeTerms(HashMap<String, String> rbMap, int sampleMgra)
    {

        logger.info("");
        logger.info("");
        logger.info("Calculating Stop Location SOA Size Terms");

        String uecPath = rbMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String slcSoaSizeUecFile = rbMap.get(PROPERTIES_UEC_STOP_SOA_SIZE);
        slcSoaSizeUecFile = uecPath + slcSoaSizeUecFile;
        int slcSoaSizeUecData = Integer.parseInt(rbMap.get(PROPERTIES_UEC_STOP_SOA_SIZE_DATA));
        int slcSoaSizeUecModel = Integer.parseInt(rbMap.get(PROPERTIES_UEC_STOP_SOA_SIZE_MODEL));

        IndexValues iv = new IndexValues();
        UtilityExpressionCalculator slcSoaSizeUec = new UtilityExpressionCalculator(new File(
                slcSoaSizeUecFile), slcSoaSizeUecModel, slcSoaSizeUecData, rbMap, null);

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int maxMgra = mgraManager.getMaxMgra();
        int alternatives = slcSoaSizeUec.getNumberOfAlternatives();
        double[][] slcSoaSize = new double[alternatives][maxMgra + 1];

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            iv.setZoneIndex(mgra);
            double[] utilities = slcSoaSizeUec.solve(iv, null, null);

            if (mgra == sampleMgra)
                slcSoaSizeUec.logAnswersArray(slcSoaLogger, "Stop Location SOA Size Terms, MGRA = "
                        + mgra);

            // store the size terms
            for (int i = 0; i < alternatives; i++)
                slcSoaSize[i][mgra] = utilities[i];

        }

        return slcSoaSize;

    }

    private int[][] getDcEstimationDataOrigDestTimes(TableDataSet hisTds)
    {

        // odts are an array with elements: origin mgra, destination mgra, departure
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
        int[] tourPurpose = hisTds.getColumnAsInt(TOUR_PURPOSE_FIELD);
        int[] stopPurpose = hisTds.getColumnAsInt(STOP_PURPOSE_FIELD);
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

            odts[r - 1][TOUR_PURPOSE] = tourPurpose[r - 1];
            odts[r - 1][STOP_PURPOSE] = stopPurpose[r - 1];
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
        StopLocationSampleCalculator appender = new StopLocationSampleCalculator(rbMap);

        appender.startMatrixServer(rb);
        appender.runSampleProbabilitiesCalculator(rb);

    }

}
