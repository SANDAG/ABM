package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagAppendMcLogsumDMU;
import org.sandag.abm.application.SandagTripModeChoiceDMU;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

public class McLogsumsAppender implements Serializable
{

    protected transient Logger                   logger                           = Logger.getLogger(McLogsumsAppender.class);
    protected transient Logger                   slcSoaLogger                     = Logger.getLogger("slcSoa");
    protected transient Logger                   nonManLogsumsLogger              = Logger.getLogger("nonManLogsums");

    protected int                                debugEstimationFileRecord1;
    protected int                                debugEstimationFileRecord2;

    protected static final String                ESTIMATION_DATA_RECORDS_FILE_KEY = "homeInterview.survey.file";
    public static final String                PROPERTIES_UEC_TOUR_MODE_CHOICE  = "tourModeChoice.uec.file";
    public static final String                PROPERTIES_UEC_TRIP_MODE_CHOICE  = "tripModeChoice.uec.file";

    public static final int                   WORK_SHEET                       = 1;
    public static final int                   UNIVERSITY_SHEET                 = 2;
    public static final int                   SCHOOL_SHEET                     = 3;
    public static final int                   MAINTENANCE_SHEET                = 4;
    public static final int                   DISCRETIONARY_SHEET              = 5;
    public static final int                   SUBTOUR_SHEET                    = 6;
    public static final int[]                 MC_PURPOSE_SHEET_INDICES         = {-1,
            WORK_SHEET, UNIVERSITY_SHEET, SCHOOL_SHEET, MAINTENANCE_SHEET, MAINTENANCE_SHEET,
            MAINTENANCE_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET, DISCRETIONARY_SHEET,
            SUBTOUR_SHEET                                                           };

    public static final int                   WORK_CATEGORY                    = 0;
    public static final int                   UNIVERSITY_CATEGORY              = 1;
    public static final int                   SCHOOL_CATEGORY                  = 2;
    public static final int                   MAINTENANCE_CATEGORY             = 3;
    public static final int                   DISCRETIONARY_CATEGORY           = 4;
    public static final int                   SUBTOUR_CATEGORY                 = 5;
    public static final String[]              PURPOSE_CATEGORY_LABELS          = {"work",
            "university", "school", "maintenance", "discretionary", "subtour"     };
    public static final int[]                 PURPOSE_CATEGORIES               = {-1,
            WORK_CATEGORY, UNIVERSITY_CATEGORY, SCHOOL_CATEGORY, MAINTENANCE_CATEGORY,
            MAINTENANCE_CATEGORY, MAINTENANCE_CATEGORY, DISCRETIONARY_CATEGORY,
            DISCRETIONARY_CATEGORY, DISCRETIONARY_CATEGORY, SUBTOUR_CATEGORY      };

    protected static final int                   ORIG_MGRA                        = 1;
    protected static final int                   DEST_MGRA                        = 2;
    protected static final int                   ADULTS                           = 3;
    protected static final int                   AUTOS                            = 4;
    protected static final int                   HHSIZE                           = 5;
    protected static final int                   FEMALE                           = 6;
    protected static final int                   AGE                              = 7;
    protected static final int                   JOINT                            = 8;
    protected static final int                   PARTYSIZE                        = 9;
    protected static final int                   TOUR_PURPOSE                     = 10;
    protected static final int                   INCOME                           = 11;
    protected static final int                   ESCORT                           = 12;
    protected static final int                   DEPART_PERIOD                    = 13;
    protected static final int                   ARRIVE_PERIOD                    = 14;
    protected static final int                   SAMPNO                           = 15;
    protected static final int                   WORK_TOUR_MODE                   = 16;
    protected static final int                   OUT_STOPS                        = 17;
    protected static final int                   IN_STOPS                         = 18;
    protected static final int                   FIRST_TRIP                       = 19;
    protected static final int                   LAST_TRIP                        = 20;
    protected static final int                   TOUR_MODE                        = 21;
    protected static final int                   TRIP_PERIOD                      = 22;
    protected static final int                   CHOSEN_MGRA                      = 23;
    protected static final int                   DIRECTION                        = 24;
    protected static final int                   PORTION                          = 25;
    protected static final int                   PERNO                            = 26;
    protected static final int                   TOUR_ID                          = 27;
    protected static final int                   TRIPNO                           = 28;
    protected static final int                   STOPID                           = 29;
    protected static final int                   STOPNO                           = 30;
    protected static final int                   STOP_PURPOSE                     = 31;
    protected static final int                   NUM_FIELDS                       = 32;

    private static final int                     INBOUND_DIRCETION_CODE           = 2;
    

    // estimation file defines time periods as:
    // 1 | Early AM: 3:00 AM - 5:59 AM |
    // 2 | AM Peak: 6:00 AM - 8:59 AM |
    // 3 | Early MD: 9:00 AM - 11:59 PM |
    // 4 | Late MD: 12:00 PM - 3:29 PM |
    // 5 | PM Peak: 3:30 PM - 6:59 PM |
    // 6 | Evening: 7:00 PM - 2:59 AM |

    protected static final int                   LAST_EA_INDEX                    = 3;
    protected static final int                   LAST_AM_INDEX                    = 9;
    protected static final int                   LAST_MD_INDEX                    = 22;
    protected static final int                   LAST_PM_INDEX                    = 29;

    protected static final int                   EA                               = 1;
    protected static final int                   AM                               = 2;
    protected static final int                   MD                               = 3;
    protected static final int                   PM                               = 4;
    protected static final int                   EV                               = 5;

    protected static final int                   EA_D                             = 1;                                        // 5am
    protected static final int                   AM_D                             = 5;                                        // 7am
    protected static final int                   MD_D                             = 15;                                       // 12pm
    protected static final int                   PM_D                             = 27;                                       // 6pm
    protected static final int                   EV_D                             = 35;                                       // 10pm
    protected static final int[]                 DEFAULT_DEPART_INDICES           = {-1, EA_D,
            AM_D, MD_D, PM_D, EV_D                                                };

    protected static final int                   EA_A                             = 2;                                        // 5:30am
    protected static final int                   AM_A                             = 6;                                        // 7:30am
    protected static final int                   MD_A                             = 16;                                       // 12:30pm
    protected static final int                   PM_A                             = 28;                                       // 6:30pm
    protected static final int                   EV_A                             = 36;                                       // 10:30pm
    protected static final int[]                 DEFAULT_ARRIVE_INDICES           = {-1, EA_A,
            AM_A, MD_A, PM_A, EV_A                                                };

    protected String[][]                         departArriveCombinationLabels    = {{"EA", "EA"},
            {"EA", "AM"}, {"EA", "MD"}, {"EA", "PM"}, {"EA", "EV"}, {"AM", "AM"}, {"AM", "MD"},
            {"AM", "PM"}, {"AM", "EV"}, {"MD", "MD"}, {"MD", "PM"}, {"MD", "EV"}, {"PM", "PM"},
            {"PM", "EV"}, {"EV", "EV"}                                            };

    protected int[][]                            departArriveCombinations         = {{EA, EA},
            {EA, AM}, {EA, MD}, {EA, PM}, {EA, EV}, {AM, AM}, {AM, MD}, {AM, PM}, {AM, EV},
            {MD, MD}, {MD, PM}, {MD, EV}, {PM, PM}, {PM, EV}, {EV, EV}            };

    private BestTransitPathCalculator bestPathUEC;
    
    // modeChoiceLogsums is an array of logsums for each unique depart/arrive skim
    // period combination, for each sample destination
    protected double[][]                         modeChoiceLogsums;
    protected double[][]                         tripModeChoiceLogsums;
    
    protected double[]                           tripModeChoiceSegmentLogsums = new double[2];

    protected double[]                           tripModeChoiceSegmentStoredProbabilities;


    // departArriveLogsums is the array of values for all 15 depart/arrive
    // combinations
    protected double[][]                         departArriveLogsums;

    protected int                                chosenLogsumTodIndex             = 0;

    protected int[]                              chosenDepartArriveCombination    = new int[2];

    protected int                                numMgraFields;
    protected int[]                              mgraSetForLogsums;

    protected int[][]                            mgras;

    protected TazDataManager                     tazs;
    protected MgraDataManager                    mgraManager;
    protected TapDataManager                     tapManager;
    protected MatrixDataServerIf                 ms;

    protected ModelStructure                     modelStructure;

    protected int[][]                            bestTapPairs;
    protected double[]                           nmSkimsOut;
    protected double[]                           nmSkimsIn;
    protected double[]                           lbSkimsOut;
    protected double[]                           lbSkimsIn;
    protected double[]                           ebSkimsOut;
    protected double[]                           ebSkimsIn;
    protected double[]                           brSkimsOut;
    protected double[]                           brSkimsIn;
    protected double[]                           lrSkimsOut;
    protected double[]                           lrSkimsIn;
    protected double[]                           crSkimsOut;
    protected double[]                           crSkimsIn;

    
    protected double[]                           lsWgtAvgCostM;
    protected double[]                           lsWgtAvgCostD;
    protected double[]                           lsWgtAvgCostH;


    protected int                                totalTime1                       = 0;
    protected int                                totalTime2                       = 0;

    
    private McLogsumsCalculator      logsumHelper;
    

    public McLogsumsAppender( HashMap<String, String> propertyMap )
    {
        logsumHelper = new McLogsumsCalculator();

        logsumHelper.setupSkimCalculators(propertyMap);
        
    }
    
    
    public BestTransitPathCalculator getBestTransitPathCalculator()
    {
        return bestPathUEC;
    }
    
    
    protected TableDataSet getEstimationDataTableDataSet(HashMap<String, String> rbMap)
    {

        String estFileName = Util.getStringValueFromPropertyMap(rbMap,
                ESTIMATION_DATA_RECORDS_FILE_KEY);
        if (estFileName == null)
        {
            logger
                    .error("Error getting the filename from the properties file for the Sandag home interview survey data records file.");
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
            logger.fatal(String.format(
                "Exception occurred reading Sandag home interview survey data records file: %s into TableDataSet object.", estFileName));
            throw new RuntimeException(e);
        }

    }

    protected void calculateModeChoiceLogsums(HashMap<String, String> rbMap,
            ChoiceModelApplication mcModel, SandagAppendMcLogsumDMU mcDmuObject, int[] odt,
            int[] mgraSample, int[] departAvailable, int[] arriveAvailable, boolean chosenTodOnly)
    {

        boolean isChosenDepartArriveCombo = false;

        int origMgra = odt[ORIG_MGRA];
        int chosenMgra = odt[DEST_MGRA];

        int inc = 0;
        if (odt[INCOME] == 1) inc = 7500;
        else if (odt[INCOME] == 2) inc = 22500;
        else if (odt[INCOME] == 3) inc = 37500;
        else if (odt[INCOME] == 4) inc = 52500;
        else if (odt[INCOME] == 5) inc = 67500;
        else if (odt[INCOME] == 6) inc = 87500;
        else if (odt[INCOME] == 7) inc = 112500;
        else if (odt[INCOME] == 8) inc = 137500;
        else if (odt[INCOME] == 9) inc = 175000;
        else if (odt[INCOME] == 10) inc = 250000;
        else if (odt[INCOME] == 99) inc = 37500;

        mcDmuObject.setIncomeInDollars(inc);
        mcDmuObject.setAdults(odt[ADULTS]);
        mcDmuObject.setAutos(odt[AUTOS]);
        mcDmuObject.setHhSize(odt[HHSIZE]);
        mcDmuObject.setPersonIsFemale(odt[FEMALE]);
        mcDmuObject.setAge(odt[AGE]);
        mcDmuObject.setTourCategoryJoint(odt[JOINT]);
        mcDmuObject.setTourCategoryEscort(odt[ESCORT]);
        mcDmuObject.setNumberOfParticipantsInJointTour(odt[PARTYSIZE]);

        mcDmuObject.setWorkTourModeIsSOV( odt[WORK_TOUR_MODE] == 1 || odt[WORK_TOUR_MODE] == 2 ? 1 : 0 );
        mcDmuObject.setWorkTourModeIsBike( odt[WORK_TOUR_MODE] == 10 ? 1 : 0 );
        mcDmuObject.setWorkTourModeIsHOV( odt[WORK_TOUR_MODE] >= 3 || odt[WORK_TOUR_MODE] <= 8 ? 1 : 0 );

        mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(origMgra));
        mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(origMgra));
        mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(origMgra));

        mcDmuObject.setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(origMgra)));
        


        chosenDepartArriveCombination[0] = odt[DEPART_PERIOD];
        chosenDepartArriveCombination[1] = odt[ARRIVE_PERIOD];

        // create an array with the chosen dest and the sample dests, for which to
        // compute the logsums
        mgraSetForLogsums[0] = chosenMgra;
        for (int m = 0; m < mgraSample.length; m++)
            mgraSetForLogsums[m + 1] = mgraSample[m];

        int m = 0;
        for (int destMgra : mgraSetForLogsums)
        {

            if (mcModel != null && destMgra > 0)
            {
                // set the mode choice attributes needed by @variables in the UEC
                // spreadsheets
                mcDmuObject.setDmuIndexValues(odt[0], origMgra, origMgra, destMgra, false);

                mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(destMgra));
                mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(destMgra));
                mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(destMgra));

                mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager.getTaz(destMgra)));

                modeChoiceLogsums[m] = new double[modelStructure.getSkimPeriodCombinationIndices().length];
                Arrays.fill(modeChoiceLogsums[m], -999);
            }

            // compute the logsum for each depart/arrival time combination for the
            // selected destination mgra
            int i = 0;
            for (int[] combo : departArriveCombinations)
            {

                // mcModel might be null in the case where an estimation file record
                // contains multiple purposes and the logsums are not desired for
                // some purposes.
                // destMgra might be null in the case of destination choice where
                // some sample destinations are repeated, so the set of 30 or 40
                // contain 0s to reflect that.
                if (mcModel == null || destMgra == 0)
                {
                    continue;
                }

                if (combo[0] == chosenDepartArriveCombination[0]
                        && combo[1] == chosenDepartArriveCombination[1])
                {
                    isChosenDepartArriveCombo = true;
                    chosenLogsumTodIndex = i;
                }

                if (!chosenTodOnly || isChosenDepartArriveCombo)
                {

                    int departPeriod = DEFAULT_DEPART_INDICES[combo[0]];
                    int arrivePeriod = DEFAULT_ARRIVE_INDICES[combo[1]];

                    // if the depart/arrive combination was flagged as unavailable,
                    // can skip the logsum calculation
                    if (unavailableCombination(departPeriod, arrivePeriod, departAvailable,
                            arriveAvailable))
                    {
                        departArriveLogsums[m][i++] = -999;
                        continue;
                    }

                    int logsumIndex = modelStructure.getSkimPeriodCombinationIndex(departPeriod,
                            arrivePeriod);

                    // if a depart/arrive period combination results in a logsum
                    // index that's already had logsums computed, skip to next
                    // combination.
                    if (modeChoiceLogsums[m][logsumIndex] > -999)
                    {
                        departArriveLogsums[m][i++] = modeChoiceLogsums[m][logsumIndex];
                        continue;
                    }

                    mcDmuObject.setDepartPeriod(departPeriod);
                    mcDmuObject.setArrivePeriod(arrivePeriod);

                    
                    double logsum = logsumHelper.calculateTourMcLogsum(origMgra, destMgra, departPeriod, arrivePeriod, mcModel, mcDmuObject);
                    
                    modeChoiceLogsums[m][logsumIndex] = logsum;
                    departArriveLogsums[m][i] = logsum;

                    // write UEC calculation results to logsum specific log file if
                    // its the chosen dest and its the chosen time combo
                    if ((odt[0] == debugEstimationFileRecord1 || odt[0] == debugEstimationFileRecord2)
                            && (m == 0) /* && isChosenDepartArriveCombo */)
                    {

                        nonManLogsumsLogger.info("Logsum[" + i
                                + "] calculation for estimation file record number " + odt[0]);
                        nonManLogsumsLogger.info("");
                        nonManLogsumsLogger
                                .info("--------------------------------------------------------------------------------------------------------");
                        nonManLogsumsLogger.info("tour purpose = " + odt[TOUR_PURPOSE]);
                        nonManLogsumsLogger.info("mc purpose sheet = "
                                + MC_PURPOSE_SHEET_INDICES[odt[TOUR_PURPOSE]]);
                        nonManLogsumsLogger.info("purpose category = "
                                + PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]] + ": "
                                + PURPOSE_CATEGORY_LABELS[PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]]]);
                        nonManLogsumsLogger.info("origin mgra = " + odt[ORIG_MGRA]);
                        nonManLogsumsLogger.info("destination mgra = " + odt[DEST_MGRA]);
                        nonManLogsumsLogger.info("origin taz = " + mgraManager.getTaz(origMgra));
                        nonManLogsumsLogger.info("destination taz = "
                                + mgraManager.getTaz(destMgra));
                        nonManLogsumsLogger.info("depart interval = "
                                + departArriveCombinationLabels[i][0] + ", @timeOutbound = "
                                + mcDmuObject.getTimeOutbound() + ", chosen depart = "
                                + departPeriod);
                        nonManLogsumsLogger.info("arrive interval = "
                                + departArriveCombinationLabels[i][1] + ", @timeInbound = "
                                + mcDmuObject.getTimeInbound() + ", chosen arrive = "
                                + arrivePeriod);
                        nonManLogsumsLogger.info("income category = " + odt[INCOME]
                                + ", @income = " + mcDmuObject.getIncome());
                        nonManLogsumsLogger.info("adults = " + odt[ADULTS]);
                        nonManLogsumsLogger.info("autos = " + odt[AUTOS]);
                        nonManLogsumsLogger.info("hhsize = " + odt[HHSIZE]);
                        nonManLogsumsLogger.info("gender = " + odt[FEMALE] + ", @female = "
                                + mcDmuObject.getFemale());
                        nonManLogsumsLogger.info("jointTourCategory = " + odt[JOINT]
                                + ", @tourcategoryJoint = " + mcDmuObject.getTourCategoryJoint());
                        nonManLogsumsLogger.info("partySize = " + odt[PARTYSIZE]);
                        nonManLogsumsLogger
                        .info("--------------------------------------------------------------------------------------------------------");
                        nonManLogsumsLogger.info("");

                        mcModel.logUECResults(nonManLogsumsLogger, "Est Record: " + odt[0]);
                        nonManLogsumsLogger.info("Logsum Calculation for index: " + logsumIndex
                                + " , Logsum value: " + modeChoiceLogsums[m][logsumIndex]);
                        nonManLogsumsLogger.info("");
                        nonManLogsumsLogger.info("");

                        isChosenDepartArriveCombo = false;
                    }

                }
                i++;
            }

            m++;
        }

    }

    protected void calculateTripModeChoiceLogsums(HashMap<String, String> rbMap,
            ChoiceModelApplication mcModel, SandagTripModeChoiceDMU mcDmuObject, int[] odt, int[] mgraSample)
        {

            int origMgra = odt[ORIG_MGRA];
            int destMgra = odt[DEST_MGRA];
            int chosenMgra = odt[CHOSEN_MGRA];

            
            for (int m = 0; m < tripModeChoiceLogsums.length; m++){
                tripModeChoiceLogsums[m][0] = -999;
                tripModeChoiceLogsums[m][1] = -999;
            }
            
            if ( origMgra == 0 || destMgra == 0 || odt[TOUR_MODE] == 0 )
                return;
            
            int inc = 0;
            if (odt[INCOME] == 1) inc = 7500;
            else if (odt[INCOME] == 2) inc = 22500;
            else if (odt[INCOME] == 3) inc = 37500;
            else if (odt[INCOME] == 4) inc = 52500;
            else if (odt[INCOME] == 5) inc = 67500;
            else if (odt[INCOME] == 6) inc = 87500;
            else if (odt[INCOME] == 7) inc = 112500;
            else if (odt[INCOME] == 8) inc = 137500;
            else if (odt[INCOME] == 9) inc = 175000;
            else if (odt[INCOME] == 10) inc = 250000;
            else if (odt[INCOME] == 99) inc = 37500;

            mcDmuObject.setOutboundHalfTourDirection(odt[DIRECTION]);

            mcDmuObject.setJointTour( odt[JOINT] );
            mcDmuObject.setEscortTour( odt[TOUR_PURPOSE] == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX ? 1 : 0 );
            
            mcDmuObject.setIncomeInDollars(inc);
            mcDmuObject.setAdults(odt[ADULTS]);
            mcDmuObject.setAutos(odt[AUTOS]);
            mcDmuObject.setAge(odt[AGE]);
            mcDmuObject.setHhSize(odt[HHSIZE]);
            mcDmuObject.setPersonIsFemale(odt[FEMALE] == 2 ? 1 : 0);

            mcDmuObject.setTourModeIsDA( modelStructure.getTourModeIsSov( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsS2( modelStructure.getTourModeIsS2( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsS3( modelStructure.getTourModeIsS3( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsWalk( modelStructure.getTourModeIsWalk( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsBike( modelStructure.getTourModeIsBike( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsWTran( modelStructure.getTourModeIsWalkLocal(odt[TOUR_MODE]) || modelStructure.getTourModeIsWalkPremium(odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsPnr( modelStructure.getTourModeIsPnr( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsKnr( modelStructure.getTourModeIsKnr( odt[TOUR_MODE]) ? 1 : 0 );
            mcDmuObject.setTourModeIsSchBus( modelStructure.getTourModeIsSchoolBus( odt[TOUR_MODE]) ? 1 : 0 );

            mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(origMgra));
            mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(origMgra));
            mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(origMgra));

            mcDmuObject.setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(origMgra)));
            
            mcDmuObject.setDepartPeriod( odt[DEPART_PERIOD] );
            mcDmuObject.setTripPeriod( odt[TRIP_PERIOD] );

            int departPeriod = odt[TRIP_PERIOD];
            
            // create an array with the chosen dest and the sample dests, for which to
            // compute the logsums
            mgraSetForLogsums[0] = chosenMgra;
            for (int m = 0; m < mgraSample.length; m++){
                mgraSetForLogsums[m + 1] = mgraSample[m];
            }

            if (mcModel == null || origMgra == 0)
                return;
            
            int m = 0;
            for (int sampleMgra : mgraSetForLogsums)
            {

                tripModeChoiceLogsums[m][0] = -999;
                tripModeChoiceLogsums[m][1] = -999;

                if (mcModel != null && sampleMgra > 0)
                {
                    // set the mode choice attributes needed by @variables in the UEC
                    // spreadsheets
                    mcDmuObject.setDmuIndexValues(odt[0], origMgra, origMgra, sampleMgra, false);

                    mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(sampleMgra));
                    mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(sampleMgra));
                    mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(sampleMgra));

                    mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager.getTaz(sampleMgra)));

                }


                // mcModel might be null in the case where an estimation file record
                // contains multiple purposes and the logsums are not desired for
                // some purposes.
                // destMgra might be null in the case of destination choice where
                // some sample destinations are repeated, so the set of 30 or 40
                // contain 0s to reflect that.
                if (mcModel == null || sampleMgra == 0)
                {
                    continue;
                }

                            
                // write UEC calculation results to logsum specific log file if
                // its the chosen dest and its the chosen time combo
                if ( (odt[0] == debugEstimationFileRecord1 || odt[0] == debugEstimationFileRecord2) )
                {

                    nonManLogsumsLogger.info("IK Logsum calculation for estimation file record number " + odt[0]);
                    nonManLogsumsLogger.info("");
                    nonManLogsumsLogger.info("--------------------------------------------------------------------------------------------------------");
                    nonManLogsumsLogger.info("tour purpose = " + odt[TOUR_PURPOSE]);
                    nonManLogsumsLogger.info("mc purpose sheet = " + MC_PURPOSE_SHEET_INDICES[odt[TOUR_PURPOSE]]);
                    nonManLogsumsLogger.info("purpose category = " + PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]] + ": " + PURPOSE_CATEGORY_LABELS[PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]]]);
                    nonManLogsumsLogger.info("tour mode = " + odt[TOUR_MODE]);
                    nonManLogsumsLogger.info("origin mgra = " + origMgra);
                    nonManLogsumsLogger.info("sample destination mgra = " + sampleMgra);
                    nonManLogsumsLogger.info("final destination mgra = " + destMgra);
                    nonManLogsumsLogger.info("origin taz = " + mgraManager.getTaz(origMgra));
                    nonManLogsumsLogger.info("sample destination taz = " + mgraManager.getTaz(sampleMgra));
                    nonManLogsumsLogger.info("final destination taz = " + mgraManager.getTaz(destMgra));
                    nonManLogsumsLogger.info("depart interval = " + departPeriod);
                    nonManLogsumsLogger.info("income category = " + odt[INCOME] + ", @income = " + mcDmuObject.getIncome());
                    nonManLogsumsLogger.info("adults = " + odt[ADULTS]);
                    nonManLogsumsLogger.info("autos = " + odt[AUTOS]);
                    nonManLogsumsLogger.info("hhsize = " + odt[HHSIZE]);
                    nonManLogsumsLogger.info("gender = " + odt[FEMALE] + ", @female = " + mcDmuObject.getFemale());
                    nonManLogsumsLogger.info("--------------------------------------------------------------------------------------------------------");
                    nonManLogsumsLogger.info("");

                    mcDmuObject.getDmuIndexValues().setDebug(true);
                }

                
    			if ( (odt[DIRECTION]==INBOUND_DIRCETION_CODE) ) {
            		logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );
    			}
            	else
            		logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );        		
            	
        		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );

        		double logsum = logsumHelper.calculateTripMcLogsum(origMgra, sampleMgra, departPeriod, mcModel, mcDmuObject, nonManLogsumsLogger);
                tripModeChoiceLogsums[m][0] = logsum;

                if ( (odt[0] == debugEstimationFileRecord1 || odt[0] == debugEstimationFileRecord2) ) {
                    nonManLogsumsLogger.info("IK Logsum value: " + tripModeChoiceLogsums[m][0]);
                    nonManLogsumsLogger.info("");
                    nonManLogsumsLogger.info("");
                }
                

                // write UEC calculation results to logsum specific log file if
                // its the chosen dest and its the chosen time combo
                if ( (odt[0] == debugEstimationFileRecord1 || odt[0] == debugEstimationFileRecord2) )
                {

                    nonManLogsumsLogger.info("KJ Logsum calculation for estimation file record number " + odt[0]);
                    nonManLogsumsLogger.info("");
                    nonManLogsumsLogger.info("--------------------------------------------------------------------------------------------------------");
                    nonManLogsumsLogger.info("tour purpose = " + odt[TOUR_PURPOSE]);
                    nonManLogsumsLogger.info("mc purpose sheet = " + MC_PURPOSE_SHEET_INDICES[odt[TOUR_PURPOSE]]);
                    nonManLogsumsLogger.info("purpose category = " + PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]] + ": " + PURPOSE_CATEGORY_LABELS[PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]]]);
                    nonManLogsumsLogger.info("origin mgra = " + sampleMgra);
                    nonManLogsumsLogger.info("sample destination mgra = " + destMgra);
                    nonManLogsumsLogger.info("final destination mgra = " + destMgra);
                    nonManLogsumsLogger.info("origin taz = " + mgraManager.getTaz(sampleMgra));
                    nonManLogsumsLogger.info("sample destination taz = " + mgraManager.getTaz(destMgra));
                    nonManLogsumsLogger.info("final destination taz = " + mgraManager.getTaz(destMgra));
                    nonManLogsumsLogger.info("depart interval = " + departPeriod);
                    nonManLogsumsLogger.info("income category = " + odt[INCOME] + ", @income = " + mcDmuObject.getIncome());
                    nonManLogsumsLogger.info("adults = " + odt[ADULTS]);
                    nonManLogsumsLogger.info("autos = " + odt[AUTOS]);
                    nonManLogsumsLogger.info("hhsize = " + odt[HHSIZE]);
                    nonManLogsumsLogger.info("gender = " + odt[FEMALE] + ", @female = " + mcDmuObject.getFemale());
                    nonManLogsumsLogger.info("--------------------------------------------------------------------------------------------------------");
                    nonManLogsumsLogger.info("");

                    mcDmuObject.getDmuIndexValues().setDebug(true);
                }

    			if ( (odt[DIRECTION]==INBOUND_DIRCETION_CODE) ) {
            		logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );
    			}
            	else
            		logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );        		
            	
        		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );

        		logsum = logsumHelper.calculateTripMcLogsum(origMgra, sampleMgra, departPeriod, mcModel, mcDmuObject, nonManLogsumsLogger);
                tripModeChoiceLogsums[m][1] = logsum;
                
                if ( (odt[0] == debugEstimationFileRecord1 || odt[0] == debugEstimationFileRecord2) ) {
                    nonManLogsumsLogger.info("KJ Logsum value: " + tripModeChoiceLogsums[m][1]);
                    nonManLogsumsLogger.info("");
                    nonManLogsumsLogger.info("");
                }

                m++;
            }

        }

    
    protected double[] calculateTripModeChoiceLogsumForEstimationRecord(HashMap<String, String> rbMap,
            ChoiceModelApplication mcModel, SandagTripModeChoiceDMU mcDmuObject, int[] odt, int sampleMgra )
    {

        int origMgra = odt[ORIG_MGRA];
        int destMgra = odt[DEST_MGRA];

        double[] tripModeChoiceLogsums = new double[2];
        tripModeChoiceLogsums[0] = -999;
        tripModeChoiceLogsums[1] = -999;

        // mcModel would be null if the estimation file record has a stop purpose for which no ChoiceModelApplication has been defined.
        if ( origMgra == 0 || destMgra == 0 || sampleMgra == 0 || odt[TOUR_MODE] == 0 || mcModel == null )
            return tripModeChoiceLogsums;
        
        mcDmuObject.setOutboundHalfTourDirection(odt[DIRECTION]);

        mcDmuObject.setJointTour( odt[JOINT] );
        mcDmuObject.setEscortTour( odt[TOUR_PURPOSE] == ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX ? 1 : 0 );
        
        int inc = 0;
        if (odt[INCOME] == 1) inc = 7500;
        else if (odt[INCOME] == 2) inc = 22500;
        else if (odt[INCOME] == 3) inc = 37500;
        else if (odt[INCOME] == 4) inc = 52500;
        else if (odt[INCOME] == 5) inc = 67500;
        else if (odt[INCOME] == 6) inc = 87500;
        else if (odt[INCOME] == 7) inc = 112500;
        else if (odt[INCOME] == 8) inc = 137500;
        else if (odt[INCOME] == 9) inc = 175000;
        else if (odt[INCOME] == 10) inc = 250000;
        else if (odt[INCOME] == 99) inc = 37500;

        mcDmuObject.setIncomeInDollars(inc);
        mcDmuObject.setAdults(odt[ADULTS]);
        mcDmuObject.setAutos(odt[AUTOS]);
        mcDmuObject.setAge(odt[AGE]);
        mcDmuObject.setHhSize(odt[HHSIZE]);
        mcDmuObject.setPersonIsFemale(odt[FEMALE] == 2 ? 1 : 0);

        mcDmuObject.setTourModeIsDA( modelStructure.getTourModeIsSov( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsS2( modelStructure.getTourModeIsS2( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsS3( modelStructure.getTourModeIsS3( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsWalk( modelStructure.getTourModeIsWalk( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsBike( modelStructure.getTourModeIsBike( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsWTran( modelStructure.getTourModeIsWalkLocal(odt[TOUR_MODE]) || modelStructure.getTourModeIsWalkPremium(odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsPnr( modelStructure.getTourModeIsPnr( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsKnr( modelStructure.getTourModeIsKnr( odt[TOUR_MODE]) ? 1 : 0 );
        mcDmuObject.setTourModeIsSchBus( modelStructure.getTourModeIsSchoolBus( odt[TOUR_MODE]) ? 1 : 0 );

        mcDmuObject.setOrigDuDen(mgraManager.getDuDenValue(origMgra));
        mcDmuObject.setOrigEmpDen(mgraManager.getEmpDenValue(origMgra));
        mcDmuObject.setOrigTotInt(mgraManager.getTotIntValue(origMgra));

        mcDmuObject.setPTazTerminalTime(tazs.getOriginTazTerminalTime(mgraManager.getTaz(origMgra)));
        
        mcDmuObject.setDepartPeriod( odt[DEPART_PERIOD] );
        mcDmuObject.setTripPeriod( odt[TRIP_PERIOD] );

        int departPeriod = odt[TRIP_PERIOD];
        

        // set the mode choice attributes needed by @variables in the UEC spreadsheets
        mcDmuObject.setDmuIndexValues(odt[0], origMgra, origMgra, sampleMgra, false);

        mcDmuObject.setDestDuDen(mgraManager.getDuDenValue(sampleMgra));
        mcDmuObject.setDestEmpDen(mgraManager.getEmpDenValue(sampleMgra));
        mcDmuObject.setDestTotInt(mgraManager.getTotIntValue(sampleMgra));

        mcDmuObject.setATazTerminalTime(tazs.getDestinationTazTerminalTime(mgraManager.getTaz(sampleMgra)));

        if ( mcDmuObject.getDmuIndexValues().getDebug() ) {

            // write UEC calculation results to logsum specific log file if
            // its the chosen dest and its the chosen time combo
            slcSoaLogger.info("IK Logsum calculation for estimation file record number " + odt[0]);
            slcSoaLogger.info("");
            slcSoaLogger.info("--------------------------------------------------------------------------------------------------------");
            slcSoaLogger.info("tour purpose = " + odt[TOUR_PURPOSE]);
            slcSoaLogger.info("mc purpose sheet = " + MC_PURPOSE_SHEET_INDICES[odt[TOUR_PURPOSE]]);
            slcSoaLogger.info("purpose category = " + PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]] + ": " + PURPOSE_CATEGORY_LABELS[PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]]]);
            slcSoaLogger.info("tour mode = " + odt[TOUR_MODE]);
            slcSoaLogger.info("origin mgra = " + origMgra);
            slcSoaLogger.info("sample destination mgra = " + sampleMgra);
            slcSoaLogger.info("final destination mgra = " + destMgra);
            slcSoaLogger.info("origin taz = " + mgraManager.getTaz(origMgra));
            slcSoaLogger.info("sample destination taz = " + mgraManager.getTaz(sampleMgra));
            slcSoaLogger.info("final destination taz = " + mgraManager.getTaz(destMgra));
            slcSoaLogger.info("depart interval = " + departPeriod);
            slcSoaLogger.info("income category = " + odt[INCOME] + ", @income = " + mcDmuObject.getIncome());
            slcSoaLogger.info("adults = " + odt[ADULTS]);
            slcSoaLogger.info("autos = " + odt[AUTOS]);
            slcSoaLogger.info("hhsize = " + odt[HHSIZE]);
            slcSoaLogger.info("gender = " + odt[FEMALE] + ", @female = " + mcDmuObject.getFemale());
            slcSoaLogger.info("--------------------------------------------------------------------------------------------------------");
            slcSoaLogger.info("");

        }
        
		if ( (odt[DIRECTION]==INBOUND_DIRCETION_CODE) ) {
    		logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );
		}
    	else
    		logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );        		
    	
		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, origMgra, sampleMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );

		double logsum = logsumHelper.calculateTripMcLogsum(origMgra, sampleMgra, departPeriod, mcModel, mcDmuObject, nonManLogsumsLogger);
        tripModeChoiceLogsums[0] = logsum;

        if ( mcDmuObject.getDmuIndexValues().getDebug() ) {

            slcSoaLogger.info("IK Mode Choice Logsum value: " + tripModeChoiceLogsums[0]);
            slcSoaLogger.info("");
            slcSoaLogger.info("");

        
            // write UEC calculation results to logsum specific log file if
            // its the chosen dest and its the chosen time combo
            slcSoaLogger.info("KJ Logsum calculation for estimation file record number " + odt[0]);
            slcSoaLogger.info("");
            slcSoaLogger.info("--------------------------------------------------------------------------------------------------------");
            slcSoaLogger.info("tour purpose = " + odt[TOUR_PURPOSE]);
            slcSoaLogger.info("mc purpose sheet = " + MC_PURPOSE_SHEET_INDICES[odt[TOUR_PURPOSE]]);
            slcSoaLogger.info("purpose category = " + PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]] + ": " + PURPOSE_CATEGORY_LABELS[PURPOSE_CATEGORIES[odt[TOUR_PURPOSE]]]);
            slcSoaLogger.info("origin mgra = " + sampleMgra);
            slcSoaLogger.info("sample destination mgra = " + destMgra);
            slcSoaLogger.info("final destination mgra = " + destMgra);
            slcSoaLogger.info("origin taz = " + mgraManager.getTaz(sampleMgra));
            slcSoaLogger.info("sample destination taz = " + mgraManager.getTaz(destMgra));
            slcSoaLogger.info("final destination taz = " + mgraManager.getTaz(destMgra));
            slcSoaLogger.info("depart interval = " + departPeriod);
            slcSoaLogger.info("income category = " + odt[INCOME] + ", @income = " + mcDmuObject.getIncome());
            slcSoaLogger.info("adults = " + odt[ADULTS]);
            slcSoaLogger.info("autos = " + odt[AUTOS]);
            slcSoaLogger.info("hhsize = " + odt[HHSIZE]);
            slcSoaLogger.info("gender = " + odt[FEMALE] + ", @female = " + mcDmuObject.getFemale());
            slcSoaLogger.info("--------------------------------------------------------------------------------------------------------");
            slcSoaLogger.info("");

        }
        
		if ( (odt[DIRECTION]==INBOUND_DIRCETION_CODE) ) {
    		logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, sampleMgra, destMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );
		}
    	else
    		logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, sampleMgra, destMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );        		
    	
		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, sampleMgra, destMgra, departPeriod, mcDmuObject.getDmuIndexValues().getDebug() );

		logsum = logsumHelper.calculateTripMcLogsum(sampleMgra, destMgra, departPeriod, mcModel, mcDmuObject, nonManLogsumsLogger);
        tripModeChoiceLogsums[1] = logsum;
    
        if ( mcDmuObject.getDmuIndexValues().getDebug() ) {
            
            slcSoaLogger.info("KJ Mode Choice Logsum value: " + tripModeChoiceLogsums[1]);
            slcSoaLogger.info("");
            slcSoaLogger.info("");

        }
        
        return tripModeChoiceLogsums;
        
    }


              

    protected int getModelPeriodFromTodIndex(int index)
    {
        int returnValue = -1;
        if (index <= LAST_EA_INDEX)
            returnValue = EA;
        else if (index <= LAST_AM_INDEX)
            returnValue = AM;
        else if (index <= LAST_MD_INDEX)
            returnValue = MD;
        else if (index <= LAST_PM_INDEX)
            returnValue = PM;
        else
            returnValue = EV;
        
        return returnValue;
    }

    /**
     * 
     * @param departPeriod is the departure interval
     * @param arrivePeriod is the arrival interval
     * @param departAvailable is the model time period the departure interval belongs
     *            to (EA, AM, MD, PM, EV)
     * @param arriveAvailable is the model time period the arrival interval belongs
     *            to (EA, AM, MD, PM, EV)
     * @return true if the depart and/or arrival periods are unavailable, false if
     *         both are available.
     */
    protected boolean unavailableCombination(int departPeriod, int arrivePeriod,
            int[] departAvailable, int[] arriveAvailable)
    {

        int departModelPeriod = getModelPeriodFromTodIndex(departPeriod);
        int arriveModelPeriod = getModelPeriodFromTodIndex(arrivePeriod);

        boolean returnValue = true;
        if (departAvailable[departModelPeriod] == 1 && arriveAvailable[arriveModelPeriod] == 1)
            returnValue = false;

        return returnValue;

    }

    
    /**
     * return the array of mode choice model cumulative probabilities determined while
     * computing the mode choice logsum for the trip segmen during stop location choice.
     * These probabilities arrays are stored for each sampled stop location so that when
     * the selected sample stop location is known, the mode choice can be drawn from the
     * already computed probabilities.
     *  
     * @return mode choice cumulative probabilities array
     */
    public double[] getStoredSegmentCumulativeProbabilities() {
        return tripModeChoiceSegmentStoredProbabilities;
    }


    /**
     * Start the matrix server
     * 
     * @param rb is a ResourceBundle for the properties file for this application
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
    
    
    public AutoAndNonMotorizedSkimsCalculator getAnmSkimCalculator() {
        return logsumHelper.getAnmSkimCalculator();
    }
    
    public WalkTransitWalkSkimsCalculator getWtwSkimCalculator() {
        return logsumHelper.getWtwSkimCalculator();
    }
    
    public WalkTransitDriveSkimsCalculator getWtdSkimCalculator() {
        return logsumHelper.getWtdSkimCalculator();
    }
    
    public DriveTransitWalkSkimsCalculator getDtwSkimCalculator() {
        return logsumHelper.getDtwSkimCalculator();
    }
    
    public void setTazDistanceSkimArrays( double[][][] storedFromTazDistanceSkims, double[][][] storedToTazDistanceSkims ) {
        AutoAndNonMotorizedSkimsCalculator anm = logsumHelper.getAnmSkimCalculator();
        anm.setTazDistanceSkimArrays( storedFromTazDistanceSkims, storedToTazDistanceSkims );                                                                
    }                                                                                                                            
                                                                                                                                 
                                                                                                                                 
}
