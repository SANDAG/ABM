package org.sandag.abm.ctramp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.StoredUtilityData;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.DataFile;
import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.DataWriter;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.ResourceUtil;
// import org.sandag.abm.accessibilities.BuildAccessibilities;
// import
// org.sandag.abm.ctramp.HouseholdDataWriter;
// import org.sandag.abm.ctramp.IndividualMandatoryTourFrequencyModel;

// 1.0.1 - 09/21/09 - starting point for SANDAG AB model implementation - AO
// model

public class CtrampApplication
        implements Serializable
{

    private transient Logger                           logger                                                    = Logger.getLogger(CtrampApplication.class);

    public static final String                         VERSION                                                   = "2.0.0";

    public static final int                            MATRIX_DATA_SERVER_PORT                                   = 1171;
    public static final int                            MATRIX_DATA_SERVER_PORT_OFFSET                            = 0;

    public static final String                         PROPERTIES_BASE_NAME                                      = "ctramp";
    public static final String                         PROPERTIES_PROJECT_DIRECTORY                              = "Project.Directory";

    public static final String                         PROPERTIES_UEC_PATH                                       = "uec.path";
    public static final String                         SQLITE_DATABASE_FILENAME                                  = "Sqlite.DatabaseFileName";

    public static final String                         PROPERTIES_RUN_POPSYN                                     = "RunModel.PopulationSynthesizer";
    public static final String                         PROPERTIES_RUN_PRE_AUTO_OWNERSHIP                         = "RunModel.PreAutoOwnership";
    public static final String                         PROPERTIES_RUN_WORKSCHOOL_CHOICE                          = "RunModel.UsualWorkAndSchoolLocationChoice";
    public static final String                         PROPERTIES_RUN_AUTO_OWNERSHIP                             = "RunModel.AutoOwnership";
    public static final String                         PROPERTIES_RUN_TRANSPONDER_CHOICE                         = "RunModel.TransponderChoice";
    public static final String                         PROPERTIES_RUN_FREE_PARKING_AVAILABLE                     = "RunModel.FreeParking";
    public static final String                         PROPERTIES_RUN_INTERNAL_EXTERNAL_TRIP                     = "RunModel.InternalExternal";
    public static final String                         PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN                     = "RunModel.CoordinatedDailyActivityPattern";
    public static final String                         PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ                  = "RunModel.IndividualMandatoryTourFrequency";
    public static final String                         PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR                 = "RunModel.MandatoryTourDepartureTimeAndDuration";
    public static final String                         PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE                      = "RunModel.MandatoryTourModeChoice";
    public static final String                         PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ                       = "RunModel.AtWorkSubTourFrequency";
    public static final String                         PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE            = "RunModel.AtWorkSubTourLocationChoice";
    public static final String                         PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE                = "RunModel.AtWorkSubTourModeChoice";
    public static final String                         PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR           = "RunModel.AtWorkSubTourDepartureTimeAndDuration";
    public static final String                         PROPERTIES_RUN_JOINT_TOUR_FREQ                            = "RunModel.JointTourFrequency";
    public static final String                         PROPERTIES_RUN_JOINT_LOCATION_CHOICE                      = "RunModel.JointTourLocationChoice";
    public static final String                         PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE                     = "RunModel.JointTourModeChoice";
    public static final String                         PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR                = "RunModel.JointTourDepartureTimeAndDuration";
    public static final String                         PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ              = "RunModel.IndividualNonMandatoryTourFrequency";
    public static final String                         PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE        = "RunModel.IndividualNonMandatoryTourLocationChoice";
    public static final String                         PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE       = "RunModel.IndividualNonMandatoryTourModeChoice";
    public static final String                         PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR  = "RunModel.IndividualNonMandatoryTourDepartureTimeAndDuration";
    public static final String                         PROPERTIES_RUN_STOP_FREQUENCY                             = "RunModel.StopFrequency";
    public static final String                         PROPERTIES_RUN_STOP_LOCATION                              = "RunModel.StopLocation";

    public static final String                         PROPERTIES_RUN_WORK_LOC_CHOICE_KEY                        = "uwsl.run.workLocChoice";
    public static final String                         PROPERTIES_RUN_SCHOOL_LOC_CHOICE_KEY                      = "uwsl.run.schoolLocChoice";
    public static final String                         PROPERTIES_WRITE_WORK_SCHOOL_LOC_RESULTS_KEY              = "uwsl.write.results";

    public static final String                         PROPERTIES_UEC_AUTO_OWNERSHIP                             = "UecFile.AutoOwnership";
    public static final String                         PROPERTIES_UEC_DAILY_ACTIVITY_PATTERN                     = "UecFile.CoordinatedDailyActivityPattern";
    public static final String                         PROPERTIES_UEC_INDIV_MANDATORY_TOUR_FREQ                  = "UecFile.IndividualMandatoryTourFrequency";
    public static final String                         PROPERTIES_UEC_MAND_TOUR_DEP_TIME_AND_DUR                 = "UecFile.TourDepartureTimeAndDuration";
    public static final String                         PROPERTIES_UEC_INDIV_NON_MANDATORY_TOUR_FREQ              = "UecFile.IndividualNonMandatoryTourFrequency";

    public static final String                         PROPERTIES_CLEAR_MATRIX_MANAGER_ON_START                  = "RunModel.Clear.MatrixMgr.At.Start";

    public static final String                         READ_ACCESSIBILITIES                                      = "acc.read.input.file";

    // TODO eventually move to model-specific structure object
    public static final int                            TOUR_MODE_CHOICE_WORK_MODEL_UEC_PAGE                      = 1;
    public static final int                            TOUR_MODE_CHOICE_UNIVERSITY_MODEL_UEC_PAGE                = 2;
    public static final int                            TOUR_MODE_CHOICE_HIGH_SCHOOL_MODEL_UEC_PAGE               = 3;
    public static final int                            TOUR_MODE_CHOICE_GRADE_SCHOOL_MODEL_UEC_PAGE              = 4;

    // TODO eventually move to model-specific model structure object
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_MODEL_UEC_PAGE       = 1;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_DEPARTURE_UEC_PAGE   = 2;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_DURATION_UEC_PAGE    = 3;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_ARRIVAL_UEC_PAGE     = 4;

    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_MODEL_UEC_PAGE     = 5;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_DEPARTURE_UEC_PAGE = 6;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_DURATION_UEC_PAGE  = 7;
    public static final int                            MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_ARRIVAL_UEC_PAGE   = 8;

    public static final String                         PROPERTIES_SCHEDULING_NUMBER_OF_TIME_PERIODS              = "Scheduling.NumberOfTimePeriods";
    public static final String                         PROPERTIES_SCHEDULING_FIRST_TIME_PERIOD                   = "Scheduling.FirstTimePeriod";

    static final String                                PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER                  = "RunModel.RestartWithHhServer";

    static final String                                PROPERTIES_HOUSEHOLD_DISK_OBJECT_FILE_NAME                = "Households.disk.object.base.name";
    static final String                                PROPERTIES_HOUSEHOLD_DISK_OBJECT_KEY                      = "Read.HouseholdDiskObjectFile";

    public static final String                         PROPERTIES_RESULTS_AUTO_OWNERSHIP                         = "Results.AutoOwnership";
    public static final String                         PROPERTIES_RESULTS_CDAP                                   = "Results.CoordinatedDailyActivityPattern";

    public static final String                         PROPERTIES_OUTPUT_WRITE_SWITCH                            = "CTRAMP.Output.WriteToDiskSwitch";
    public static final String                         PROPERTIES_OUTPUT_HOUSEHOLD_FILE                          = "CTRAMP.Output.HouseholdFile";
    public static final String                         PROPERTIES_OUTPUT_PERSON_FILE                             = "CTRAMP.Output.PersonFile";

    public static final String                         PROPERTIES_WRITE_DATA_TO_FILE                             = "Results.WriteDataToFiles";
    public static final String                         PROPERTIES_WRITE_DATA_TO_DATABASE                         = "Results.WriteDataToDatabase";

    public static final String                         PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS                    = "TourModeChoice.Save.UtilsAndProbs";

    public static final String                         PROPERTIES_WORK_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE   = "UsualWorkLocationChoice.ShadowPrice.Input.File";
    public static final String                         PROPERTIES_SCHOOL_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE = "UsualSchoolLocationChoice.ShadowPrice.Input.File";

    public static final String                         PROPERTIES_NUMBER_OF_GLOBAL_ITERATIONS                    = "Global.iterations";

    public static final String                         ALT_FIELD_NAME                                            = "a";
    public static final String                         START_FIELD_NAME                                          = "depart";
    public static final String                         END_FIELD_NAME                                            = "arrive";

    private static final int                           NUM_WRITE_PACKETS                                         = 1000;

    private ResourceBundle                             resourceBundle;
    private HashMap<String, String>                    propertyMap;

    private MatrixIO32BitJvm                           ioVm32Bit;
    private MatrixDataServerRmi                        ms;

    private ModelStructure                             modelStructure;
    protected String                                   projectDirectory;

    private HashMap<Integer, HashMap<String, Integer>> cdapByHhSizeAndPattern;
    private HashMap<String, HashMap<String, Integer>>  cdapByPersonTypeAndActivity;

    private BuildAccessibilities                       aggAcc;
    private boolean                                    calculateLandUseAccessibilities;

    public CtrampApplication(ResourceBundle rb, HashMap<String, String> rbMap,
            boolean calculateLandUseAccessibilities)
    {
        resourceBundle = rb;
        propertyMap = rbMap;
        this.calculateLandUseAccessibilities = calculateLandUseAccessibilities;
    }

    public void setupModels(ModelStructure modelStructure)
    {

        this.modelStructure = modelStructure;

    }

    // public void runPopulationSynthesizer( SANDAGPopSyn populationSynthesizer
    // ){
    //
    // // run population synthesizer
    // boolean runModelPopulationSynthesizer =
    // ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_POPSYN);
    // if(runModelPopulationSynthesizer){
    // populationSynthesizer.run();
    // }
    //
    // }

    public void runModels(HouseholdDataManagerIf householdDataManager,
            CtrampDmuFactoryIf dmuFactory, int globalIterationNumber, float iterationSampleRate)
    {

        logger.info("Running JPPF CtrampApplication.runModels() for "
                + householdDataManager.getNumHouseholds() + " households.");

        String matrixServerAddress = "";
        int serverPort = 0;
        try
        {
            // get matrix server address. if "none" is specified, no server will
            // be
            // started, and matrix io will ocurr within the current process.
            matrixServerAddress = Util.getStringValueFromPropertyMap(propertyMap,
                    "RunModel.MatrixServerAddress");
            try
            {
                // get matrix server port.
                serverPort = Util.getIntegerValueFromPropertyMap(propertyMap,
                        "RunModel.MatrixServerPort");
            } catch (RuntimeException e)
            {
                serverPort = MATRIX_DATA_SERVER_PORT;
            }
        } catch (RuntimeException e)
        {
            matrixServerAddress = "localhost";
            serverPort = MATRIX_DATA_SERVER_PORT;
        }

        String matrixTypeName = Util.getStringValueFromPropertyMap(propertyMap,
                "Results.MatrixType");
        MatrixType mt = MatrixType.lookUpMatrixType(matrixTypeName);

        if (mt != MatrixType.BINARY)
        {

            try
            {
                if (matrixServerAddress.equalsIgnoreCase("localhost"))
                {

                    try
                    {
                        // create the concrete data server object
                        start32BitMatrixIoServer(mt);
                    } catch (RuntimeException e)
                    {
                        logger.error("RuntimeException caught starting 32 bit Matrix IO Process.",
                                e);
                        stop32BitMatrixIoServer();
                    }

                } else
                {

                    ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                            MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    ms.testRemote(Thread.currentThread().getName());
                    ms.start32BitMatrixIoServer(mt);

                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.setMatrixDataServerObject(ms);

                }

            } catch (Exception e)
            {

                if (mt != MatrixType.BINARY)
                {
                    stop32BitMatrixIoServer();
                } else
                {
                    ms.stop32BitMatrixIoServer();
                }

                logger.error(
                        String.format("exception caught starting MatrixDataServer -- exiting."), e);
                throw new RuntimeException();

            }

            // get the property that indicates if the MatrixDataServer should be
            // cleared.
            // default is to clear the manager
            boolean clearMatrixMgr = true;
            try
            {
                clearMatrixMgr = Util.getBooleanValueFromPropertyMap(propertyMap,
                        PROPERTIES_CLEAR_MATRIX_MANAGER_ON_START);
            } catch (RuntimeException e)
            {
                // catch the RuntimeExcption that's thrown if the property key
                // is not found in the properties file.
                // no need to anything - the boolean clearMatrixMgr was
                // initialized to the default action.
            }

            // if the property to clear matrices is true and a remote
            // MatrixDataServer is being used, clear the matrices.
            // if matrices are being read directly into the current process, no
            // need to clear.
            if (clearMatrixMgr && !matrixServerAddress.equalsIgnoreCase("localhost")) ms.clear();

        }

        // run core activity based model for the specified iteration
        runModelSequence(globalIterationNumber, householdDataManager, dmuFactory);

        // if a separate process for running matrix data mnager was started,
        // we're
        // done with it, so close it.
        if (matrixServerAddress.equalsIgnoreCase("localhost"))
        {
            stop32BitMatrixIoServer();
        } else if (ms != null)
        {
            ms.stop32BitMatrixIoServer();
        }

    }

    /**
     * This method maintains the sequencing of the various AB Model choice model
     * components
     * 
     * @param iteration
     *            is the global iteration number in the sequence of AB Model
     *            runs during feedback
     * @param householdDataManager
     *            is the handle to the household object data manager
     * @param dmuFactory
     *            is the factory object for creating DMU objects used in choice
     *            models
     */
    private void runModelSequence(int iteration, HouseholdDataManagerIf householdDataManager,
            CtrampDmuFactoryIf dmuFactory)
    {

        String restartModel = ResourceUtil.getProperty(resourceBundle,
                PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER);
        boolean logResults = Util.getStringValueFromPropertyMap(propertyMap, "RunModel.LogResults")
                .equalsIgnoreCase("true");
        if (restartModel == null) restartModel = "none";
        if (!restartModel.equalsIgnoreCase("none")) restartModels(householdDataManager);

        JPPFClient jppfClient = null;

        boolean runPreAutoOwnershipChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_PRE_AUTO_OWNERSHIP);
        if (runPreAutoOwnershipChoiceModel)
        {

            logger.info("creating Accessibilities Object for Pre-AO.");
            buildNonMandatoryAccessibilities(calculateLandUseAccessibilities);

            logger.info("starting Pre-Auto Ownership Model.");
            HashMap<String, String> propertyMap = ResourceUtil
                    .changeResourceBundleIntoHashMap(resourceBundle);

            householdDataManager.resetPreAoRandom();

            HouseholdAutoOwnershipModel aoModel = new HouseholdAutoOwnershipModel(propertyMap,
                    dmuFactory, aggAcc.getAccessibilitiesTableObject(), null);

            ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                    .getNumHouseholds());

            for (int[] startEndIndices : startEndTaskIndicesList)
            {

                int startIndex = startEndIndices[0];
                int endIndex = startEndIndices[1];

                // get the array of households
                Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);
                for (int i = 0; i < householdArray.length; ++i)
                {

                    try
                    {
                        aoModel.applyModel(householdArray[i], true);
                    } catch (RuntimeException e)
                    {
                        logger.fatal(String
                                .format("exception caught running pre-AO for i=%d, startIndex=%d, endIndex=%d, hhId=%d.",
                                        i, startIndex, endIndex, householdArray[i].getHhId()));
                        logger.fatal("Exception caught:", e);
                        logger.fatal("Throwing new RuntimeException() to terminate.");
                        throw new RuntimeException();
                    }

                }
                householdDataManager.setHhArray(householdArray, startIndex);

            }

            saveAoResults(householdDataManager, projectDirectory, true);

            // clear the zonal data used in the AO UEC so a different zonal data
            // file
            // (MGRA data) can be used later by other UECs.
            // TableDataSetManager tableDataManager =
            // TableDataSetManager.getInstance();
            // tableDataManager.clearData();
        }
        logger.info("flag to run pre-AO was set to: " + runPreAutoOwnershipChoiceModel);
        logAoResults(householdDataManager, true);

        boolean runUsualWorkSchoolChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_WORKSCHOOL_CHOICE);
        if (runUsualWorkSchoolChoiceModel)
        {

            boolean runWorkLocationChoice = false;
            try
            {
                String stringValue = resourceBundle.getString(PROPERTIES_RUN_WORK_LOC_CHOICE_KEY);
                runWorkLocationChoice = stringValue.equalsIgnoreCase("true");
            } catch (MissingResourceException e)
            {
                // default value is true if property was not defined
                runWorkLocationChoice = true;
            }
            logger.info("flag to run work location choice was set to: " + runWorkLocationChoice);

            boolean runSchoolLocationChoice = false;
            try
            {
                String stringValue = resourceBundle.getString(PROPERTIES_RUN_SCHOOL_LOC_CHOICE_KEY);
                runSchoolLocationChoice = stringValue.equalsIgnoreCase("true");
            } catch (MissingResourceException e)
            {
                // default value is true if property was not defined
                runSchoolLocationChoice = true;
            }
            logger.info("flag to run school location choice was set to: " + runSchoolLocationChoice);

            boolean writeLocationChoiceResultsFile = false;
            try
            {
                String stringValue = resourceBundle
                        .getString(PROPERTIES_WRITE_WORK_SCHOOL_LOC_RESULTS_KEY);
                writeLocationChoiceResultsFile = stringValue.equalsIgnoreCase("true");
            } catch (MissingResourceException e)
            {
                // default value is true if property was not defined
                writeLocationChoiceResultsFile = true;
            }
            logger.info("flag to write uwsl result was set to: " + writeLocationChoiceResultsFile);

            if (aggAcc == null)
            {
                logger.info("creating Accessibilities Object for UWSL.");
                buildNonMandatoryAccessibilities(calculateLandUseAccessibilities);
            }

            // new the usual school and location choice model object
            jppfClient = new JPPFClient();
            UsualWorkSchoolLocationChoiceModel usualWorkSchoolLocationChoiceModel = new UsualWorkSchoolLocationChoiceModel(
                    resourceBundle, restartModel, jppfClient, modelStructure, ms, dmuFactory,
                    aggAcc);

            if (runWorkLocationChoice)
            {
                // calculate and get the array of worker size terms table -
                // MGRAs by
                // occupations
                aggAcc.createWorkSegmentNameIndices();
                aggAcc.calculateWorkerSizeTerms();
                double[][] workerSizeTerms = aggAcc.getWorkerSizeTerms();

                // run the model
                logger.info("starting usual work location choice.");
                usualWorkSchoolLocationChoiceModel.runWorkLocationChoiceModel(householdDataManager,
                        workerSizeTerms);
                logger.info("finished with usual work location choice.");
            }

            if (runSchoolLocationChoice)
            {
                aggAcc.createSchoolSegmentNameIndices();
                aggAcc.calculateSchoolSizeTerms();
                double[][] schoolFactors = aggAcc.calculateSchoolSegmentFactors();
                double[][] schoolSizeTerms = aggAcc.getSchoolSizeTerms();

                logger.info("starting usual school location choice.");
                usualWorkSchoolLocationChoiceModel.runSchoolLocationChoiceModel(
                        householdDataManager, schoolSizeTerms, schoolFactors);
                logger.info("finished with usual school location choice.");
            }

            if (writeLocationChoiceResultsFile)
            {
                logger.info("writing work/school location choice results file; may take a few minutes ...");
                usualWorkSchoolLocationChoiceModel.saveResults(householdDataManager,
                        projectDirectory, iteration);
                logger.info(String
                        .format("finished writing work/school location choice results file."));
            }

            usualWorkSchoolLocationChoiceModel = null;

        }
        logger.info("flag to run UWSL was set to: " + runUsualWorkSchoolChoiceModel);

        boolean runAutoOwnershipChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_AUTO_OWNERSHIP);
        boolean runTransponderChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_TRANSPONDER_CHOICE);
        boolean runFreeParkingChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_FREE_PARKING_AVAILABLE);
        boolean runInternalExternalTripChoiceModel = ResourceUtil.getBooleanProperty(
                resourceBundle, PROPERTIES_RUN_INTERNAL_EXTERNAL_TRIP);
        boolean runCoordinatedDailyActivityPatternChoiceModel = ResourceUtil.getBooleanProperty(
                resourceBundle, PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN);
        boolean runMandatoryTourFreqChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ);
        boolean runJointTourFreqChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_JOINT_TOUR_FREQ);
        boolean runIndivNonManTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ);
        boolean runAtWorkSubTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ);
        boolean runStopFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_STOP_FREQUENCY);

        if (runAutoOwnershipChoiceModel || runTransponderChoiceModel || runFreeParkingChoiceModel
                || runInternalExternalTripChoiceModel
                || runCoordinatedDailyActivityPatternChoiceModel || runMandatoryTourFreqChoiceModel
                || runIndivNonManTourFrequencyModel || runAtWorkSubTourFrequencyModel
                || runStopFrequencyModel)
        {

            // We're resetting the random number sequence used by pre-AO for the
            // primary AO
            if (runAutoOwnershipChoiceModel) householdDataManager.resetPreAoRandom();

            if (runTransponderChoiceModel)
                householdDataManager.computeTransponderChoiceTazPercentArrays();

            logger.info("starting HouseholdChoiceModelRunner.");
            HashMap<String, String> propertyMap = ResourceUtil
                    .changeResourceBundleIntoHashMap(resourceBundle);
            HouseholdChoiceModelRunner runner = new HouseholdChoiceModelRunner(propertyMap,
                    jppfClient, restartModel, householdDataManager, ms, modelStructure, dmuFactory);
            runner.runHouseholdChoiceModels();

            if (runAutoOwnershipChoiceModel)
            {
                saveAoResults(householdDataManager, projectDirectory, false);
                if (logResults) logAoResults(householdDataManager, false);
            }

            if (runTransponderChoiceModel)
            {
                if (logResults) logTpResults(householdDataManager);
            }

            if (runFreeParkingChoiceModel)
            {
                if (logResults) logFpResults(householdDataManager);
            }

            if (runInternalExternalTripChoiceModel)
            {
                if (logResults) logIeResults(householdDataManager);
            }

            if (runCoordinatedDailyActivityPatternChoiceModel)
            {
                saveCdapResults(householdDataManager, projectDirectory);
                if (logResults) logCdapResults(householdDataManager);
            }

            if (runMandatoryTourFreqChoiceModel)
            {
                if (logResults) logImtfResults(householdDataManager);
            }

            if (runJointTourFreqChoiceModel)
            {
                if (logResults) logJointModelResults(householdDataManager, dmuFactory);
            }

            if (runAtWorkSubTourFrequencyModel)
            {
                if (logResults) logAtWorkSubtourFreqResults(householdDataManager);
            }

            if (runStopFrequencyModel)
            {
                if (logResults) logIndivStfResults(householdDataManager);
            }

        }

        jppfClient.close();

        boolean writeTextFileFlag = false;
        boolean writeSqliteFlag = false;
        try
        {
            writeTextFileFlag = ResourceUtil.getBooleanProperty(resourceBundle,
                    PROPERTIES_WRITE_DATA_TO_FILE);
        } catch (MissingResourceException e)
        {
            // if exception is caught while getting property file value, then
            // boolean
            // flag remains false
        }
        try
        {
            writeSqliteFlag = ResourceUtil.getBooleanProperty(resourceBundle,
                    PROPERTIES_WRITE_DATA_TO_DATABASE);
        } catch (MissingResourceException e)
        {
            // if exception is caught while getting property file value, then
            // boolean
            // flag remains false
        }

        HouseholdDataWriter dataWriter = null;
        if (writeTextFileFlag || writeSqliteFlag)
        {
            dataWriter = new HouseholdDataWriter(propertyMap, modelStructure, iteration);

            if (writeTextFileFlag) dataWriter.writeDataToFiles(householdDataManager);

            if (writeSqliteFlag)
            {
                String dbFilename = "";
                try
                {
                    String baseDir = resourceBundle.getString(PROPERTIES_PROJECT_DIRECTORY);
                    dbFilename = baseDir + resourceBundle.getString(SQLITE_DATABASE_FILENAME) + "_"
                            + iteration;
                    dataWriter.writeDataToDatabase(householdDataManager, dbFilename);
                } catch (MissingResourceException e)
                {
                    // if exception is caught while getting property file value,
                    // then
                    // boolean flag remains false
                }
            }
        }

    }

    /**
     * Build the mandatory accessibilities object used by the usual work and
     * school location choice models
     * 
     * @return BuildAccessibilities object containing mandatory size term and
     *         logsum information private void buildMandatoryAccessibilities() {
     * 
     *         HashMap<String, String> propertyMap =
     *         ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle);
     * 
     *         if ( aggAcc == null ) aggAcc = new BuildAccessibilities(
     *         propertyMap );
     * 
     *         MatrixDataManager mdm = MatrixDataManager.getInstance();
     *         mdm.setMatrixDataServerObject( ms );
     * 
     *         aggAcc.setupBuildAccessibilities( propertyMap );
     *         aggAcc.calculateConstants();
     * 
     *         // do this in dest choice model
     *         //aggAcc.buildAccessibilityComponents( propertyMap );
     * 
     *         }
     */

    /**
     * Build the non-mandatory accessibilities object used by the auto ownership
     * model
     * 
     * @return BuildAccessibilities object containing non-mandatory size term
     *         and logsum information
     */
    private void buildNonMandatoryAccessibilities(boolean calculateLandUseAccessibilities)
    {

        HashMap<String, String> propertyMap = ResourceUtil
                .changeResourceBundleIntoHashMap(resourceBundle);

        aggAcc = BuildAccessibilities.getInstance();
        aggAcc.setupBuildAccessibilities(propertyMap, calculateLandUseAccessibilities);
        aggAcc.setCalculatedLandUseAccessibilities();

        aggAcc.calculateSizeTerms();
        aggAcc.calculateWorkerSizeTerms();
        aggAcc.createSchoolSegmentNameIndices();
        aggAcc.calculateSchoolSizeTerms();
        aggAcc.calculateConstants();
        // aggAcc.buildAccessibilityComponents(propertyMap);

        boolean readAccessibilities = ResourceUtil.getBooleanProperty(resourceBundle,
                READ_ACCESSIBILITIES);
        if (readAccessibilities)
        {

            // output data
            String accFileName = projectDirectory
                    + Util.getStringValueFromPropertyMap(propertyMap, "acc.output.file");

            aggAcc.readAccessibilityTableFromFile(accFileName);

        } else
        {

            aggAcc.calculateDCUtilitiesDistributed(propertyMap);

            if (isJppfRunningDistributed()) {
	            // release the memory used to store the access-tap, tap-egress, and
	            // tap-tap utilities while calculating accessibilities for the
	            // client program
	            // don't do this if we are running jppf in local mode
	            HashMap<String, String> rbMap = ResourceUtil
	                    .changeResourceBundleIntoHashMap(resourceBundle);
	            StoredUtilityData.getInstance(MgraDataManager.getInstance(rbMap).getMaxMgra(),
	                    MgraDataManager.getInstance(rbMap).getMaxTap(),
	                    TazDataManager.getInstance(rbMap).getMaxTaz(),
	                    BestTransitPathCalculator.NUM_ACC_EGR, BestTransitPathCalculator.NUM_PERIODS)
	                    .deallocateArrays();
	
	            MatrixDataManager.getInstance().clearData();
            }
        }

    }
    
    private boolean isJppfRunningDistributed() {
    	//note: this assumes that the jppf config file is being entered in through a system property, and that it is a property file
    	String jppfConfigFile = System.getProperty("jppf.config");
    	ResourceBundle jppfConfig = ResourceUtil.getResourceBundle(jppfConfigFile.replace(".properties",""));
    	try {
    		return !jppfConfig.getString("jppf.local.execution.enabled").equalsIgnoreCase("true");
    	} catch (MissingResourceException e) {
    		return false;
    	}
    }

    /*
     * method used in original ARC implementation private void runIteration( int
     * iteration, HouseholdDataManagerIf householdDataManager,
     * CtrampDmuFactoryIf dmuFactory ) { String restartModel = ""; if (
     * hhDiskObjectKey != null && ! hhDiskObjectKey.equalsIgnoreCase("none") ) {
     * String doFileName = hhDiskObjectFile + "_" + hhDiskObjectKey;
     * householdDataManager.createHhArrayFromSerializedObjectInFile( doFileName,
     * hhDiskObjectKey ); restartModel = hhDiskObjectKey; restartModels (
     * householdDataManager ); } else { restartModel = ResourceUtil.getProperty(
     * resourceBundle, PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER ); if (
     * restartModel == null ) restartModel = "none"; if ( !
     * restartModel.equalsIgnoreCase("none") ) restartModels (
     * householdDataManager ); } JPPFClient jppfClient = new JPPFClient();
     * boolean runUsualWorkSchoolChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_WORKSCHOOL_CHOICE); if(runUsualWorkSchoolChoiceModel){ //
     * create an object for calculating destination choice attraction size terms
     * and managing shadow price calculations. DestChoiceSize dcSizeObj = new
     * DestChoiceSize( modelStructure, tazDataManager ); // new the usual school
     * and location choice model object UsualWorkSchoolLocationChoiceModel
     * usualWorkSchoolLocationChoiceModel = new
     * UsualWorkSchoolLocationChoiceModel(resourceBundle, restartModel,
     * jppfClient, modelStructure, ms, tazDataManager, dcSizeObj, dmuFactory );
     * // run the model logger.info (
     * "starting usual work and school location choice.");
     * usualWorkSchoolLocationChoiceModel
     * .runSchoolAndLocationChoiceModel(householdDataManager); logger.info (
     * "finished with usual work and school location choice."); logger.info (
     * "writing work/school location choice results file; may take a few minutes ..."
     * ); usualWorkSchoolLocationChoiceModel.saveResults( householdDataManager,
     * projectDirectory, iteration ); logger.info (
     * String.format("finished writing results file.") );
     * usualWorkSchoolLocationChoiceModel = null; dcSizeObj = null; System.gc();
     * // write a disk object fle for the householdDataManager, in case we want
     * to restart from the next step. if ( hhDiskObjectFile != null ) {
     * logger.info (
     * "writing household disk object file after work/school location choice; may take a long time ..."
     * ); String hhFileName = String.format( "%s_%d_ao", hhDiskObjectFile,
     * iteration );
     * householdDataManager.createSerializedHhArrayInFileFromObject( hhFileName,
     * "ao" ); logger.info (String.format(
     * "finished writing household disk object file = %s after uwsl; continuing to household choice models ..."
     * , hhFileName) ); } } boolean runAutoOwnershipChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_AUTO_OWNERSHIP ); boolean runFreeParkingChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_FREE_PARKING_AVAILABLE ); boolean
     * runCoordinatedDailyActivityPatternChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN ); boolean
     * runMandatoryTourFreqChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ ); boolean
     * runMandatoryTourTimeOfDayChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR ); boolean
     * runMandatoryTourModeChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE ); boolean
     * runJointTourFrequencyModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_JOINT_TOUR_FREQ ); boolean runJointTourLocationChoiceModel
     * = ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_JOINT_LOCATION_CHOICE ); boolean
     * runJointTourModeChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE ); boolean
     * runJointTourDepartureTimeAndDurationModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR ); boolean
     * runIndivNonManTourFrequencyModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ ); boolean
     * runIndivNonManTourLocationChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE ); boolean
     * runIndivNonManTourModeChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE ); boolean
     * runIndivNonManTourDepartureTimeAndDurationModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR ); boolean
     * runAtWorkSubTourFrequencyModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ ); boolean
     * runAtWorkSubtourLocationChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE ); boolean
     * runAtWorkSubtourModeChoiceModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE ); boolean
     * runAtWorkSubtourDepartureTimeAndDurationModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR ); boolean
     * runStopFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_STOP_FREQUENCY ); boolean runStopLocationModel =
     * ResourceUtil.getBooleanProperty(resourceBundle,
     * PROPERTIES_RUN_STOP_LOCATION ); boolean runHouseholdModels = false; if (
     * runAutoOwnershipChoiceModel || runFreeParkingChoiceModel ||
     * runCoordinatedDailyActivityPatternChoiceModel ||
     * runMandatoryTourFreqChoiceModel || runMandatoryTourModeChoiceModel ||
     * runMandatoryTourTimeOfDayChoiceModel || runJointTourFrequencyModel ||
     * runJointTourLocationChoiceModel || runJointTourModeChoiceModel ||
     * runJointTourDepartureTimeAndDurationModel ||
     * runIndivNonManTourFrequencyModel || runIndivNonManTourLocationChoiceModel
     * || runIndivNonManTourModeChoiceModel ||
     * runIndivNonManTourDepartureTimeAndDurationModel ||
     * runAtWorkSubTourFrequencyModel || runAtWorkSubtourLocationChoiceModel ||
     * runAtWorkSubtourModeChoiceModel ||
     * runAtWorkSubtourDepartureTimeAndDurationModel || runStopFrequencyModel ||
     * runStopLocationModel ) runHouseholdModels = true; // disk object file is
     * labeled with the next component eligible to be run if model restarted
     * String lastComponent = "uwsl"; String nextComponent = "ao"; if(
     * runHouseholdModels ) { logger.info (
     * "starting HouseholdChoiceModelRunner." ); HashMap<String, String>
     * propertyMap =
     * ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle);
     * HouseholdChoiceModelRunner runner = new HouseholdChoiceModelRunner(
     * propertyMap, jppfClient, restartModel, householdDataManager, ms,
     * modelStructure, tazDataManager, dmuFactory );
     * runner.runHouseholdChoiceModels(); if( runAutoOwnershipChoiceModel ){
     * saveAoResults( householdDataManager, projectDirectory ); logAoResults(
     * householdDataManager ); lastComponent = "ao"; nextComponent = "fp"; } if(
     * runFreeParkingChoiceModel ){ logFpResults( householdDataManager );
     * lastComponent = "fp"; nextComponent = "cdap"; } if(
     * runCoordinatedDailyActivityPatternChoiceModel ){ saveCdapResults(
     * householdDataManager, projectDirectory ); logCdapResults(
     * householdDataManager ); lastComponent = "cdap"; nextComponent = "imtf"; }
     * if( runMandatoryTourFreqChoiceModel ){ logImtfResults(
     * householdDataManager ); lastComponent = "imtf"; nextComponent = "imtod";
     * } if( runMandatoryTourTimeOfDayChoiceModel ||
     * runMandatoryTourModeChoiceModel ){ lastComponent = "imtod"; nextComponent
     * = "jtf"; } if( runJointTourFrequencyModel ){ logJointModelResults(
     * householdDataManager ); lastComponent = "jtf"; nextComponent = "jtl"; }
     * if( runJointTourLocationChoiceModel ){ lastComponent = "jtl";
     * nextComponent = "jtod"; } if( runJointTourDepartureTimeAndDurationModel
     * || runJointTourModeChoiceModel ){ lastComponent = "jtod"; nextComponent =
     * "inmtf"; } if( runIndivNonManTourFrequencyModel ){ lastComponent =
     * "inmtf"; nextComponent = "inmtl"; } if(
     * runIndivNonManTourLocationChoiceModel ){ lastComponent = "inmtl";
     * nextComponent = "inmtod"; } if(
     * runIndivNonManTourDepartureTimeAndDurationModel ||
     * runIndivNonManTourModeChoiceModel ){ lastComponent = "inmtod";
     * nextComponent = "awf"; } if( runAtWorkSubTourFrequencyModel ){
     * logAtWorkSubtourFreqResults( householdDataManager ); lastComponent =
     * "awf"; nextComponent = "awl"; } if( runAtWorkSubtourLocationChoiceModel
     * ){ lastComponent = "awl"; nextComponent = "awtod"; } if(
     * runAtWorkSubtourDepartureTimeAndDurationModel ||
     * runAtWorkSubtourModeChoiceModel ){ lastComponent = "awtod"; nextComponent
     * = "stf"; } if( runStopFrequencyModel ){ lastComponent = "stf";
     * nextComponent = "stl"; } if( runStopLocationModel ){ lastComponent =
     * "stl"; nextComponent = "done"; } // write a disk object fle for the
     * householdDataManager, in case we want to restart from the next step. if (
     * hhDiskObjectFile != null && ! lastComponent.equalsIgnoreCase("uwsl") ) {
     * logger.info (String.format(
     * "writing household disk object file after %s choice model; may take a long time ..."
     * , lastComponent) ); String hhFileName = hhDiskObjectFile + "_" +
     * nextComponent;
     * householdDataManager.createSerializedHhArrayInFileFromObject( hhFileName,
     * nextComponent ); logger.info (
     * String.format("finished writing household disk object file = %s.",
     * hhFileName) ); } logger.info (
     * "finished with HouseholdChoiceModelRunner." ); }
     */

    public String getProjectDirectoryName()
    {
        return projectDirectory;
    }

    /**
     * Start a 32-bit matrix server to write matrices.
     * 
     * @param mType
     *            Matrix type
     */
    private void start32BitMatrixIoServer(MatrixType mType)
    {

        // start the matrix I/O server process
        ioVm32Bit = MatrixIO32BitJvm.getInstance();
        ioVm32Bit.setSizeInMegaBytes(1024);
        ioVm32Bit.startJVM32();

        // establish that matrix reader and writer classes will use the RMI
        // versions
        ioVm32Bit.startMatrixDataServer(mType);
        logger.info("matrix data server 32 bit process started.");

    }

    /**
     * Stop the 32-bit matrix server.
     */
    private void stop32BitMatrixIoServer()
    {

        // stop the matrix I/O server process
        ioVm32Bit.stopMatrixDataServer();

        // close the JVM in which the RMI reader/writer classes were running
        ioVm32Bit.stopJVM32();
        logger.info("matrix data server 32 bit process stopped.");

    }

    public void restartModels(HouseholdDataManagerIf householdDataManager)
    {

        // if no filename was specified for the previous shadow price info,
        // restartIter == -1, and random counts will be reset to 0.
        int restartIter = -1;
        String fileName = ResourceUtil.getProperty(resourceBundle,
                PROPERTIES_WORK_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE);
        if (fileName != null)
        {
            fileName = projectDirectory + fileName;
            int underScoreIndex = fileName.lastIndexOf('_');
            int dotIndex = fileName.lastIndexOf('.');
            restartIter = Integer.parseInt(fileName.substring(underScoreIndex + 1, dotIndex));
        }

        boolean runPreAutoOwnershipModel = ResourceUtil.getBooleanProperty(resourceBundle,
                PROPERTIES_RUN_PRE_AUTO_OWNERSHIP);
        if (runPreAutoOwnershipModel)
        {
            householdDataManager.resetPreAoRandom();
        } else
        {
            boolean runUsualWorkSchoolChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle,
                    PROPERTIES_RUN_WORKSCHOOL_CHOICE);
            if (runUsualWorkSchoolChoiceModel)
            {
                householdDataManager.resetUwslRandom(restartIter + 1);
            } else
            {
                boolean runAutoOwnershipModel = ResourceUtil.getBooleanProperty(resourceBundle,
                        PROPERTIES_RUN_AUTO_OWNERSHIP);
                if (runAutoOwnershipModel)
                {
                    // We're resetting the random number sequence used by pre-AO
                    // for
                    // the primary AO
                    householdDataManager.resetPreAoRandom();
                    // householdDataManager.resetAoRandom( restartIter+1 );
                } else
                {
                    // boolean runFreeParkingAvailableModel =
                    // ResourceUtil.getBooleanProperty(resourceBundle,
                    // PROPERTIES_RUN_FREE_PARKING_AVAILABLE);
                    // if ( runFreeParkingAvailableModel ) {
                    // householdDataManager.resetFpRandom();
                    // }
                    // else {
                    boolean runCoordinatedDailyActivityPatternModel = ResourceUtil
                            .getBooleanProperty(resourceBundle,
                                    PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN);
                    if (runCoordinatedDailyActivityPatternModel)
                    {
                        householdDataManager.resetCdapRandom();
                    } else
                    {
                        boolean runIndividualMandatoryTourFrequencyModel = ResourceUtil
                                .getBooleanProperty(resourceBundle,
                                        PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ);
                        if (runIndividualMandatoryTourFrequencyModel)
                        {
                            householdDataManager.resetImtfRandom();
                        } else
                        {
                            // boolean
                            // runIndividualMandatoryTourDepartureAndDurationModel
                            // =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR);
                            // if (
                            // runIndividualMandatoryTourDepartureAndDurationModel
                            // )
                            // {
                            // householdDataManager.resetImtodRandom();
                            // }
                            // else {
                            // boolean runJointTourFrequencyModel =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_JOINT_TOUR_FREQ);
                            // if ( runJointTourFrequencyModel ) {
                            // householdDataManager.resetJtfRandom();
                            // }
                            // else {
                            // boolean runJointTourLocationModel =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_JOINT_LOCATION_CHOICE);
                            // if ( runJointTourLocationModel ) {
                            // householdDataManager.resetJtlRandom();
                            // }
                            // else {
                            // boolean runJointTourDepartureAndDurationModel =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR);
                            // if ( runJointTourDepartureAndDurationModel ) {
                            // householdDataManager.resetJtodRandom();
                            // }
                            // else {
                            boolean runIndividualNonMandatoryTourFrequencyModel = ResourceUtil
                                    .getBooleanProperty(resourceBundle,
                                            PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ);
                            if (runIndividualNonMandatoryTourFrequencyModel)
                            {
                                householdDataManager.resetInmtfRandom();
                            }
                            // else {
                            // boolean
                            // runIndividualNonMandatoryTourLocationModel =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE);
                            // if ( runIndividualNonMandatoryTourLocationModel )
                            // {
                            // householdDataManager.resetInmtlRandom();
                            // }
                            // else {
                            // boolean
                            // runIndividualNonMandatoryTourDepartureAndDurationModel
                            // = ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR);
                            // if (
                            // runIndividualNonMandatoryTourDepartureAndDurationModel
                            // ) {
                            // householdDataManager.resetInmtodRandom();
                            // }
                            // else {
                            boolean runAtWorkSubTourFrequencyModel = ResourceUtil
                                    .getBooleanProperty(resourceBundle,
                                            PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ);
                            if (runAtWorkSubTourFrequencyModel)
                            {
                                householdDataManager.resetAwfRandom();
                            }
                            // else {
                            // boolean runAtWorkSubtourLocationChoiceModel =
                            // ResourceUtil.getBooleanProperty( resourceBundle,
                            // PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE );
                            // if ( runAtWorkSubtourLocationChoiceModel ) {
                            // householdDataManager.resetAwlRandom();
                            // }
                            // else {
                            // boolean
                            // runAtWorkSubtourDepartureTimeAndDurationModel
                            // = ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR);
                            // if (
                            // runAtWorkSubtourDepartureTimeAndDurationModel ) {
                            // householdDataManager.resetAwtodRandom();
                            // }
                            // else {
                            boolean runStopFrequencyModel = ResourceUtil.getBooleanProperty(
                                    resourceBundle, PROPERTIES_RUN_STOP_FREQUENCY);
                            if (runStopFrequencyModel)
                            {
                                householdDataManager.resetStfRandom();
                            }
                            // else {
                            // boolean runStopLocationModel =
                            // ResourceUtil.getBooleanProperty(resourceBundle,
                            // PROPERTIES_RUN_STOP_LOCATION);
                            // if ( runStopLocationModel ) {
                            // householdDataManager.resetStlRandom();
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                            // }
                        }
                    }
                }
            }
        }
    }

    /**
     * private void createSerializedObjectInFileFromObject( Object
     * objectToSerialize, String serializedObjectFileName, String
     * serializedObjectKey ){ try{ DataFile dataFile = new DataFile(
     * serializedObjectFileName, 1 ); DataWriter dw = new DataWriter(
     * serializedObjectKey ); dw.writeObject( objectToSerialize );
     * dataFile.insertRecord( dw ); dataFile.close(); }
     * catch(NotSerializableException e) { logger.error( String.format(
     * "NotSerializableException for %s.  Trying to create serialized object with key=%s, in filename=%s."
     * , objectToSerialize.getClass().getName(), serializedObjectKey,
     * serializedObjectFileName ), e ); throw new RuntimeException(); }
     * catch(IOException e) { logger.error( String.format(
     * "IOException trying to write disk object file=%s, with key=%s for writing."
     * , serializedObjectFileName, serializedObjectKey ), e ); throw new
     * RuntimeException(); } }
     * 
     * 
     * private Object createObjectFromSerializedObjectInFile( Object newObject,
     * String serializedObjectFileName, String serializedObjectKey ){ try{
     * DataFile dataFile = new DataFile( serializedObjectFileName, "r" );
     * DataReader dr = dataFile.readRecord( serializedObjectKey ); newObject =
     * dr.readObject(); dataFile.close(); return newObject; } catch(IOException
     * e) { logger.error( String.format(
     * "IOException trying to read disk object file=%s, with key=%s.",
     * serializedObjectFileName, serializedObjectKey ), e ); throw new
     * RuntimeException(); } catch(ClassNotFoundException e) { logger.error(
     * String.format
     * ("could not instantiate %s object, with key=%s from filename=%s.",
     * newObject.getClass().getName(), serializedObjectFileName,
     * serializedObjectKey ), e ); throw new RuntimeException(); } }
     **/
    /**
     * Loops through the households in the HouseholdDataManager, gets the auto
     * ownership result for each household, and writes a text file with hhid and
     * auto ownership.
     * 
     * @param householdDataManager
     *            is the object from which the array of household objects can be
     *            retrieved.
     * @param projectDirectory
     *            is the root directory for the output file named
     */
    private void saveAoResults(HouseholdDataManagerIf householdDataManager,
            String projectDirectory, boolean preModel)
    {

        String aoResultsFileName;
        try
        {

            aoResultsFileName = resourceBundle.getString(PROPERTIES_RESULTS_AUTO_OWNERSHIP);

            // change the filename property value to include "_pre" at the end
            // of the
            // name before the extension, if this is a pre-auto ownership run
            if (preModel)
            {
                int dotIndex = aoResultsFileName.indexOf('.');
                if (dotIndex > 0)
                {
                    String beforeDot = aoResultsFileName.substring(0, dotIndex);
                    String afterDot = aoResultsFileName.substring(dotIndex);
                    aoResultsFileName = beforeDot + "_pre" + afterDot;
                } else
                {
                    aoResultsFileName += "_pre";
                }
            }

        } catch (MissingResourceException e)
        {
            // if filename not specified in properties file, don't need to write
            // it.
            return;
        }

        FileWriter writer;
        PrintWriter outStream = null;
        if (aoResultsFileName != null)
        {

            aoResultsFileName = projectDirectory + aoResultsFileName;

            try
            {
                writer = new FileWriter(new File(aoResultsFileName));
                outStream = new PrintWriter(new BufferedWriter(writer));
            } catch (IOException e)
            {
                logger.fatal(String.format("Exception occurred opening AO results file: %s.",
                        aoResultsFileName));
                throw new RuntimeException(e);
            }

            outStream.println("HHID,AO");

            ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                    .getNumHouseholds());

            for (int[] startEndIndices : startEndTaskIndicesList)
            {

                int startIndex = startEndIndices[0];
                int endIndex = startEndIndices[1];

                // get the array of households
                Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

                for (int i = 0; i < householdArray.length; ++i)
                {

                    Household household = householdArray[i];
                    int hhid = household.getHhId();
                    int ao = household.getAutoOwnershipModelResult();

                    outStream.println(String.format("%d,%d", hhid, ao));

                }

            }

            outStream.close();

        }

    }

    private void logAoResults(HouseholdDataManagerIf householdDataManager, boolean preModel)
    {

        String[] aoRowCategoryLabel = {"0 autos", "1 auto", "2 autos", "3 autos", "4 or more autos"};
        String[] aoColCategoryLabel = {"Non-GQ HHs", "GQ HHs",};

        logger.info("");
        logger.info("");
        logger.info((preModel ? "Pre-" : "") + "Auto Ownership Model Results");
        String header = String.format("%-16s", "Category");
        for (String label : aoColCategoryLabel)
            header += String.format("%15s", label);
        header += String.format("%15s", "Total HHs");
        logger.info(header);

        // track the results
        int[][] hhsByAutoOwnership = new int[aoRowCategoryLabel.length][aoColCategoryLabel.length];

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Household household = householdArray[i];
                int ao = household.getAutoOwnershipModelResult();
                if (ao > hhsByAutoOwnership.length - 1) ao = hhsByAutoOwnership.length - 1;

                int gq = household.getIsGroupQuarters();
                hhsByAutoOwnership[ao][gq]++;

            }

        }

        int[] colTotals = new int[aoColCategoryLabel.length];
        for (int i = 0; i < hhsByAutoOwnership.length; i++)
        {

            int rowTotal = 0;
            String logString = String.format("%-16s", aoRowCategoryLabel[i]);
            for (int j = 0; j < hhsByAutoOwnership[i].length; j++)
            {
                int value = hhsByAutoOwnership[i][j];
                logString += String.format("%15d", value);
                rowTotal += value;
                colTotals[j] += value;
            }
            logString += String.format("%15d", rowTotal);
            logger.info(logString);

        }

        int total = 0;
        String colTotalsString = String.format("%-16s", "Total");
        for (int j = 0; j < colTotals.length; j++)
        {
            colTotalsString += String.format("%15d", colTotals[j]);
            total += colTotals[j];
        }
        colTotalsString += String.format("%15d", total);
        logger.info(colTotalsString);

    }

    private void logTpResults(HouseholdDataManagerIf householdDataManager)
    {

        logger.info("");
        logger.info("");
        logger.info("Transponder Choice Model Results");
        logger.info(String.format("%-16s  %20s", "Category", "Num Households"));
        logger.info(String.format("%-16s  %20s", "----------", "------------------"));

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        int numYes = 0;
        int numNo = 0;
        int numOther = 0;
        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Household household = householdArray[i];
                if (household.getTpChoice() + 1 == TransponderChoiceModel.TP_MODEL_NO_ALT) numNo++;
                else if (household.getTpChoice() + 1 == TransponderChoiceModel.TP_MODEL_YES_ALT) numYes++;
                else numOther++;

            }

        }

        logger.info(String.format("%-16s  %20d", "No", numNo));
        logger.info(String.format("%-16s  %20d", "Yes", numYes));
        logger.info(String.format("%-16s  %20d", "Other", numOther));

        logger.info(String.format("%-16s  %20s", "----------", "------------------"));
        logger.info(String.format("%-16s  %20d", "Total", (numNo + numYes + numOther)));

    }

    private void logFpResults(HouseholdDataManagerIf householdDataManager)
    {

        String[] fpCategoryLabel = {"No Choice Made", "Free Available", "Must Pay", "Reimbursed"};

        logger.info("");
        logger.info("");
        logger.info("Free Parking Choice Model Results");
        logger.info(String.format("%-16s  %20s  %20s  %20s  %20s", "Category", "Workers in area 1",
                "Workers in area 2", "Workers in area 3", "Workers in area 4"));
        logger.info(String.format("%-16s  %20s  %20s  %20s  %20s", "----------",
                "------------------", "------------------", "------------------",
                "------------------"));

        // track the results by 4 work areas - only workers in area 1 should
        // have made choices
        int numParkAreas = 4;
        int[][] workLocationsByFreeParking;
        workLocationsByFreeParking = new int[fpCategoryLabel.length][numParkAreas];

        // get the correspndence between mgra and park area to associate work
        // locations with areas
        MgraDataManager mgraManager = MgraDataManager.getInstance(propertyMap);
        int[] parkAreas = mgraManager.getMgraParkAreas();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Household household = householdArray[i];
                Person[] persons = household.getPersons();
                for (int p = 1; p < persons.length; p++)
                {
                    int workLocation = persons[p].getWorkLocation();
                    if (workLocation > 0
                            && workLocation != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                    {
                        int area = parkAreas[workLocation];
                        int areaIndex = area - 1;

                        int fp = persons[p].getFreeParkingAvailableResult();
                        int freqIndex = 0;
                        if (fp > 0) freqIndex = fp;

                        workLocationsByFreeParking[freqIndex][areaIndex]++;
                    }
                }

            }

        }

        int[] total = new int[numParkAreas];
        for (int i = 0; i < workLocationsByFreeParking.length; i++)
        {
            logger.info(String.format("%-16s  %20d  %20d  %20d  %20d", fpCategoryLabel[i],
                    workLocationsByFreeParking[i][0], workLocationsByFreeParking[i][1],
                    workLocationsByFreeParking[i][2], workLocationsByFreeParking[i][3]));
            for (int j = 0; j < numParkAreas; j++)
                total[j] += workLocationsByFreeParking[i][j];
        }
        logger.info(String.format("%-16s  %20s  %20s  %20s  %20s", "----------",
                "------------------", "------------------", "------------------",
                "------------------"));
        logger.info(String.format("%-16s  %20d  %20d  %20d  %20d", "Totals", total[0], total[1],
                total[2], total[3]));

    }

    private void logIeResults(HouseholdDataManagerIf householdDataManager)
    {

        String[] ieCategoryLabel = {"No IE Trip", "Yes IE Trip"};

        logger.info("");
        logger.info("");
        logger.info("Internal-External Trip Choice Model Results");
        logger.info(String.format("%-30s  %20s  %20s  %20s", "Person Type", ieCategoryLabel[0],
                ieCategoryLabel[1], "Total"));
        logger.info(String.format("%-30s  %20s  %20s  %20s", "-------------", "-------------",
                "-------------", "---------"));

        // summarize yes/no choice by person type
        int[][] personTypeByIeChoice;
        personTypeByIeChoice = new int[Person.PERSON_TYPE_NAME_ARRAY.length][2];

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Household household = householdArray[i];
                Person[] persons = household.getPersons();
                for (int p = 1; p < persons.length; p++)
                {

                    int ie = persons[p].getInternalExternalTripChoiceResult();

                    // ie = 1 means no, 2 means yes. Get index by subtracting 1.
                    // person typ indices are 1 based, so subtract 1 for array
                    // index
                    try
                    {
                        personTypeByIeChoice[persons[p].getPersonTypeNumber() - 1][ie - 1]++;
                    } catch (ArrayIndexOutOfBoundsException e)
                    {
                        logger.error("array index error");
                        logger.error("hhid=" + household.getHhId() + ", p=" + p + ", ie=" + ie
                                + ", personType=" + persons[p].getPersonTypeNumber(), e);
                    }
                }

            }

        }

        int[] totals = new int[2];
        for (int i = 0; i < personTypeByIeChoice.length; i++)
        {
            int total = personTypeByIeChoice[i][0] + personTypeByIeChoice[i][1];
            logger.info(String.format("%-30s  %20d  %20d  %20d", Person.PERSON_TYPE_NAME_ARRAY[i],
                    personTypeByIeChoice[i][0], personTypeByIeChoice[i][1], total));
            totals[0] += personTypeByIeChoice[i][0];
            totals[1] += personTypeByIeChoice[i][1];
        }
        logger.info(String.format("%-30s  %20s  %20s  %20s", "-------------", "-------------",
                "-------------", "---------"));
        logger.info(String.format("%-30s  %20d  %20d  %20d", "Totals", totals[0], totals[1],
                (totals[0] + totals[1])));

    }

    /**
     * Records the coordinated daily activity pattern model results to the
     * logger. A household-level summary simply records each pattern type and a
     * person-level summary summarizes the activity choice by person type
     * (full-time worker, university student, etc).
     * 
     */
    public void logCdapResults(HouseholdDataManagerIf householdDataManager)
    {

        String[] activityNameArray = {Definitions.MANDATORY_PATTERN,
                Definitions.NONMANDATORY_PATTERN, Definitions.HOME_PATTERN};

        getLogReportSummaries(householdDataManager);

        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info("Coordinated Daily Activity Pattern Model Results");

        // count of activities by person type
        logger.info(" ");
        logger.info("CDAP Results: Count of activities by person type");
        String firstHeader = "Person type                    ";
        String secondHeader = "-----------------------------  ";
        for (int i = 0; i < activityNameArray.length; ++i)
        {
            firstHeader += "        " + activityNameArray[i] + " ";
            secondHeader += "--------- ";
        }

        firstHeader += "    Total";
        secondHeader += "---------";

        logger.info(firstHeader);
        logger.info(secondHeader);

        int[] columnTotals = new int[activityNameArray.length];

        for (int i = 0; i < Person.PERSON_TYPE_NAME_ARRAY.length; ++i)
        {
            String personType = Person.PERSON_TYPE_NAME_ARRAY[i];
            String stringToLog = String.format("%-30s", personType);
            int lineTotal = 0;

            if (cdapByPersonTypeAndActivity.containsKey(personType))
            {

                for (int j = 0; j < activityNameArray.length; ++j)
                {
                    int count = 0;
                    if (cdapByPersonTypeAndActivity.get(personType).containsKey(
                            activityNameArray[j]))
                    {
                        count = cdapByPersonTypeAndActivity.get(personType).get(
                                activityNameArray[j]);
                    }
                    stringToLog += String.format("%10d", count);

                    lineTotal += count;
                    columnTotals[j] += count;
                } // j

            } // if key

            stringToLog += String.format("%10d", lineTotal);
            logger.info(stringToLog);

        } // i

        logger.info(secondHeader);

        String stringToLog = String.format("%-30s", "Total");
        int lineTotal = 0;
        for (int j = 0; j < activityNameArray.length; ++j)
        {
            stringToLog += String.format("%10d", columnTotals[j]);
            lineTotal += columnTotals[j];
        } // j

        stringToLog += String.format("%10d", lineTotal);
        logger.info(stringToLog);

        // count of patterns
        logger.info(" ");
        logger.info(" ");
        logger.info("CDAP Results: Count of patterns");
        logger.info("Pattern                Count");
        logger.info("------------------ ---------");

        // sort the map by hh size first
        Set<Integer> hhSizeKeySet = cdapByHhSizeAndPattern.keySet();
        Integer[] hhSizeKeyArray = new Integer[hhSizeKeySet.size()];
        hhSizeKeySet.toArray(hhSizeKeyArray);
        Arrays.sort(hhSizeKeyArray);

        int total = 0;
        for (int i = 0; i < hhSizeKeyArray.length; ++i)
        {

            // sort the patterns alphabetically
            HashMap<String, Integer> patternMap = cdapByHhSizeAndPattern.get(hhSizeKeyArray[i]);
            Set<String> patternKeySet = patternMap.keySet();
            String[] patternKeyArray = new String[patternKeySet.size()];
            patternKeySet.toArray(patternKeyArray);
            Arrays.sort(patternKeyArray);
            for (int j = 0; j < patternKeyArray.length; ++j)
            {
                int count = patternMap.get(patternKeyArray[j]);
                total += count;
                logger.info(String.format("%-18s%10d", patternKeyArray[j], count));
            }

        }

        logger.info("------------------ ---------");
        logger.info(String.format("%-18s%10d", "Total", total));
        logger.info(" ");

        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info(" ");
        logger.info(" ");

    }

    /**
     * Logs the results of the individual mandatory tour frequency model.
     * 
     */
    public void logImtfResults(HouseholdDataManagerIf householdDataManager)
    {

        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info("Individual Mandatory Tour Frequency Model Results");

        // count of model results
        logger.info(" ");
        String firstHeader = "Person type                   ";
        String secondHeader = "-----------------------------  ";

        String[] choiceResults = HouseholdIndividualMandatoryTourFrequencyModel.CHOICE_RESULTS;

        // summarize results
        HashMap<String, int[]> countByPersonType = new HashMap<String, int[]>();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Person[] personArray = householdArray[i].getPersons();
                for (int j = 1; j < personArray.length; j++)
                {

                    // only summarize persons with mandatory pattern
                    String personActivity = personArray[j].getCdapActivity();
                    if (personActivity != null
                            && personArray[j].getCdapActivity().equalsIgnoreCase("M"))
                    {

                        String personTypeString = personArray[j].getPersonType();
                        int choice = personArray[j].getImtfChoice();

                        if (choice == 0)
                        {

                            // there are 5 IMTF alts, so it's the offset for the
                            // extra at home categories
                            if (personArray[j].getPersonEmploymentCategoryIndex() < Person.EmployStatus.NOT_EMPLOYED
                                    .ordinal()
                                    && personArray[j].getWorkLocation() == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR) choice = 5 + 1;
                            else if (personArray[j].getPersonEmploymentCategoryIndex() < Person.EmployStatus.NOT_EMPLOYED
                                    .ordinal()
                                    && personArray[j].getPersonSchoolLocationZone() == ModelStructure.NOT_ENROLLED_SEGMENT_INDEX) choice = 5 + 2;
                            else if (personArray[j].getPersonIsStudent() < Person.EmployStatus.NOT_EMPLOYED
                                    .ordinal()
                                    && personArray[j].getWorkLocation() == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR) choice = 5 + 3;
                            else if (personArray[j].getPersonIsStudent() < Person.EmployStatus.NOT_EMPLOYED
                                    .ordinal()
                                    && personArray[j].getPersonSchoolLocationZone() == ModelStructure.NOT_ENROLLED_SEGMENT_INDEX)
                                choice = 5 + 4;

                        }

                        // count the results
                        if (countByPersonType.containsKey(personTypeString))
                        {

                            int[] counterArray = countByPersonType.get(personTypeString);
                            counterArray[choice - 1]++;
                            countByPersonType.put(personTypeString, counterArray);

                        } else
                        {

                            int[] counterArray = new int[choiceResults.length];
                            counterArray[choice - 1]++;
                            countByPersonType.put(personTypeString, counterArray);

                        }
                    }

                }

            }

        }

        for (int i = 0; i < choiceResults.length; ++i)
        {
            firstHeader += String.format("%12s", choiceResults[i]);
            secondHeader += "----------- ";
        }

        firstHeader += String.format("%12s", "Total");
        secondHeader += "-----------";

        logger.info(firstHeader);
        logger.info(secondHeader);

        int[] columnTotals = new int[choiceResults.length];

        int lineTotal = 0;
        for (int i = 0; i < Person.PERSON_TYPE_NAME_ARRAY.length; ++i)
        {
            String personTypeString = Person.PERSON_TYPE_NAME_ARRAY[i];
            String stringToLog = String.format("%-30s", personTypeString);

            if (countByPersonType.containsKey(personTypeString))
            {

                lineTotal = 0;
                int[] countArray = countByPersonType.get(personTypeString);
                for (int j = 0; j < choiceResults.length; ++j)
                {
                    stringToLog += String.format("%12d", countArray[j]);
                    columnTotals[j] += countArray[j];
                    lineTotal += countArray[j];
                } // j
            } else
            {
                // if key
                // log zeros
                lineTotal = 0;
                for (int j = 0; j < choiceResults.length; ++j)
                {
                    stringToLog += String.format("%12d", 0);
                }
            }

            stringToLog += String.format("%12d", lineTotal);

            logger.info(stringToLog);

        } // i

        String stringToLog = String.format("%-30s", "Total");
        lineTotal = 0;
        for (int j = 0; j < choiceResults.length; ++j)
        {
            stringToLog += String.format("%12d", columnTotals[j]);
            lineTotal += columnTotals[j];
        } // j

        logger.info(secondHeader);
        stringToLog += String.format("%12d", lineTotal);
        logger.info(stringToLog);
        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info(" ");
        logger.info(" ");

    }

    private void logJointModelResults(HouseholdDataManagerIf householdDataManager,
            CtrampDmuFactoryIf dmuFactory)
    {

        String uecFileDirectory = ResourceUtil.getProperty(resourceBundle, PROPERTIES_UEC_PATH);
        String uecFileName = ResourceUtil.getProperty(resourceBundle,
                JointTourModels.UEC_FILE_PROPERTIES_TARGET);
        uecFileName = uecFileDirectory + uecFileName;

        int dataSheet = ResourceUtil.getIntegerProperty(resourceBundle,
                JointTourModels.UEC_DATA_PAGE_TARGET);
        int freqCompSheet = ResourceUtil.getIntegerProperty(resourceBundle,
                JointTourModels.UEC_JOINT_TOUR_FREQ_COMP_MODEL_PAGE);

        // get the alternative names
        JointTourModelsDMU dmuObject = dmuFactory.getJointTourModelsDMU();
        ChoiceModelApplication jointTourFrequencyModel = new ChoiceModelApplication(uecFileName,
                freqCompSheet, dataSheet,
                ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle),
                (VariableTable) dmuObject);
        String[] altLabels = jointTourFrequencyModel.getAlternativeNames();

        // this is the first index in the summary array for choices made by
        // eligible households
        int[] jointTourChoiceFreq = new int[altLabels.length + 1];

        TreeMap<String, Integer> partySizeFreq = new TreeMap<String, Integer>();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Tour[] jt = householdArray[i].getJointTourArray();
                int jtfAlt = householdArray[i].getJointTourFreqChosenAlt();

                if (jt == null)
                {

                    if (jtfAlt > 0)
                    {
                        logger.error(String
                                .format("HHID=%d, joint tour array is null, but a valid alternative=%d is recorded for the household.",
                                        householdArray[i].getHhId(), jtfAlt));
                        throw new RuntimeException();
                    }

                    jointTourChoiceFreq[0]++;

                } else
                {

                    if (jtfAlt < 1)
                    {
                        logger.error(String
                                .format("HHID=%d, joint tour array is not null, but an invalid alternative=%d is recorded for the household.",
                                        householdArray[i].getHhId(), jtfAlt));
                        throw new RuntimeException();
                    }

                    jointTourChoiceFreq[jtfAlt]++;

                    // determine party size frequency for joint tours generated
                    Person[] persons = householdArray[i].getPersons();
                    for (int j = 0; j < jt.length; j++)
                    {

                        int compAlt = jt[j].getJointTourComposition();

                        // determine number of children and adults in tour
                        int adults = 0;
                        int children = 0;
                        int[] participants = jt[j].getPersonNumArray();
                        for (int k = 0; k < participants.length; k++)
                        {
                            int index = participants[k];
                            Person person = persons[index];
                            if (person.getPersonIsAdult() == 1) adults++;
                            else children++;
                        }

                        // create a key to use for a frequency map for
                        // "JointTourPurpose_Composition_NumAdults_NumChildren"
                        String key = String.format("%s_%d_%d_%d", jt[j].getTourPurpose(), compAlt,
                                adults, children);

                        int value = 0;
                        if (partySizeFreq.containsKey(key)) value = partySizeFreq.get(key);
                        partySizeFreq.put(key, ++value);

                    }

                }

            }

        }

        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info("Joint Tour Frequency and Joint Tour Composition Model Results");

        logger.info(" ");
        logger.info("Frequency Table of Households by Joint Tour Frequency Choice");
        logger.info(String.format("%-5s   %-26s   %12s", "Alt", "Alt Name", "Households"));

        int rowTotal = jointTourChoiceFreq[0];
        logger.info(String.format("%-5d   %-26s   %12d", 0, "None", jointTourChoiceFreq[0]));
        for (int i = 1; i <= altLabels.length; i++)
        {
            logger.info(String.format("%-5d   %-26s   %12d", i, altLabels[i - 1],
                    jointTourChoiceFreq[i]));
            rowTotal += jointTourChoiceFreq[i];
        }
        logger.info(String.format("%-34s   %12d", "Total Households", rowTotal));

        logger.info(" ");
        logger.info(" ");
        logger.info(" ");

        logger.info("Frequency Table of Joint Tours by All Parties Generated");
        logger.info(String.format("%-5s   %-20s   %-15s   %10s   %10s   %10s", "N", "Purpose",
                "Type", "Adults", "Children", "Freq"));

        int count = 1;
        for (String key : partySizeFreq.keySet())
        {

            int start = 0;
            int end = 0;
            int compIndex = 0;
            int adults = 0;
            int children = 0;
            String indexString = "";
            String purpose = "";

            start = 0;
            end = key.indexOf('_', start);
            purpose = key.substring(start, end);

            start = end + 1;
            end = key.indexOf('_', start);
            indexString = key.substring(start, end);
            compIndex = Integer.parseInt(indexString);

            start = end + 1;
            end = key.indexOf('_', start);
            indexString = key.substring(start, end);
            adults = Integer.parseInt(indexString);

            start = end + 1;
            indexString = key.substring(start);
            children = Integer.parseInt(indexString);

            logger.info(String.format("%-5d   %-20s   %-15s   %10d   %10d   %10d", count++,
                    purpose, JointTourModels.JOINT_TOUR_COMPOSITION_NAMES[compIndex], adults,
                    children, partySizeFreq.get(key)));
        }

        logger.info(" ");
        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info(" ");

    }

    private void getLogReportSummaries(HouseholdDataManagerIf householdDataManager)
    {

        // summary collections
        cdapByHhSizeAndPattern = new HashMap<Integer, HashMap<String, Integer>>();
        cdapByPersonTypeAndActivity = new HashMap<String, HashMap<String, Integer>>();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] partialHhArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (Household hhObject : partialHhArray)
            {

                // get the household's activity pattern choice
                String pattern = hhObject.getCoordinatedDailyActivityPattern();
                if (pattern == null) continue;

                Person[] personArray = hhObject.getPersons();
                for (int j = 1; j < personArray.length; j++)
                {

                    // get person's activity string
                    String activityString = personArray[j].getCdapActivity();

                    // get the person type to simmarize results by
                    String personTypeString = personArray[j].getPersonType();

                    // check if the person type is in the map
                    if (cdapByPersonTypeAndActivity.containsKey(personTypeString))
                    {

                        HashMap<String, Integer> activityCountMap = cdapByPersonTypeAndActivity
                                .get(personTypeString);

                        // check if the activity is in the activity map
                        int currentCount = 1;
                        if (activityCountMap.containsKey(activityString))
                            currentCount = activityCountMap.get(activityString) + 1;

                        activityCountMap.put(activityString, currentCount);
                        cdapByPersonTypeAndActivity.put(personTypeString, activityCountMap);

                    } else
                    {

                        HashMap<String, Integer> activityCountMap = new HashMap<String, Integer>();
                        activityCountMap.put(activityString, 1);
                        cdapByPersonTypeAndActivity.put(personTypeString, activityCountMap);

                    } // is personType in map if

                } // j (person loop)

                // count each type of pattern string by hhSize
                if ((!cdapByHhSizeAndPattern.isEmpty())
                        && cdapByHhSizeAndPattern.containsKey(pattern.length()))
                {

                    HashMap<String, Integer> patternCountMap = cdapByHhSizeAndPattern.get(pattern
                            .length());

                    int currentCount = 1;
                    if (patternCountMap.containsKey(pattern))
                        currentCount = patternCountMap.get(pattern) + 1;
                    patternCountMap.put(pattern, currentCount);
                    cdapByHhSizeAndPattern.put(pattern.length(), patternCountMap);

                } else
                {

                    HashMap<String, Integer> patternCountMap = new HashMap<String, Integer>();
                    patternCountMap.put(pattern, 1);
                    cdapByHhSizeAndPattern.put(pattern.length(), patternCountMap);

                } // is personType in map if

            }

        }

    }

    /**
     * Loops through the households in the HouseholdDataManager, gets the
     * coordinated daily activity pattern for each person in the household, and
     * writes a text file with hhid, personid, persnum, and activity pattern.
     * 
     * @param householdDataManager
     */
    public void saveCdapResults(HouseholdDataManagerIf householdDataManager, String projectDirectory)
    {

        String cdapResultsFileName;
        try
        {
            cdapResultsFileName = resourceBundle.getString(PROPERTIES_RESULTS_CDAP);
        } catch (MissingResourceException e)
        {
            // if filename not specified in properties file, don't need to write
            // it.
            return;
        }

        FileWriter writer;
        PrintWriter outStream = null;
        if (cdapResultsFileName != null)
        {

            cdapResultsFileName = projectDirectory + cdapResultsFileName;

            try
            {
                writer = new FileWriter(new File(cdapResultsFileName));
                outStream = new PrintWriter(new BufferedWriter(writer));
            } catch (IOException e)
            {
                logger.fatal(String.format("Exception occurred opening CDAP results file: %s.",
                        cdapResultsFileName));
                throw new RuntimeException(e);
            }

            outStream.println("HHID,PersonID,PersonNum,PersonType,ActivityString");

            ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                    .getNumHouseholds());

            for (int[] startEndIndices : startEndTaskIndicesList)
            {

                int startIndex = startEndIndices[0];
                int endIndex = startEndIndices[1];

                // get the array of households
                Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

                for (int i = 0; i < householdArray.length; ++i)
                {

                    Household household = householdArray[i];
                    int hhid = household.getHhId();

                    // get the pattern for each person
                    Person[] personArray = household.getPersons();
                    for (int j = 1; j < personArray.length; j++)
                    {

                        Person person = personArray[j];

                        int persId = person.getPersonId();
                        int persNum = person.getPersonNum();
                        int persType = person.getPersonTypeNumber();
                        String activityString = person.getCdapActivity();

                        outStream.println(String.format("%d,%d,%d,%d,%s", hhid, persId, persNum,
                                persType, activityString));

                    } // j (person loop)

                }

            }

            outStream.close();

        }

    }

    /**
     * Logs the results of the model.
     * 
     */
    public void logAtWorkSubtourFreqResults(HouseholdDataManagerIf householdDataManager)
    {

        String[] alternativeNames = modelStructure.getAwfAltLabels();
        HashMap<String, int[]> awfByPersonType = new HashMap<String, int[]>();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);
            for (int i = 0; i < householdArray.length; ++i)
            {

                // get this household's person array
                Person[] personArray = householdArray[i].getPersons();

                // loop through the person array (1-based)
                for (int j = 1; j < personArray.length; ++j)
                {

                    Person person = personArray[j];

                    // loop through the work tours for this person
                    ArrayList<Tour> tourList = person.getListOfWorkTours();
                    if (tourList == null || tourList.size() == 0) continue;

                    // count the results by person type
                    String personTypeString = person.getPersonType();

                    for (Tour workTour : tourList)
                    {

                        int choice = 0;
                        if (person.getListOfAtWorkSubtours().size() == 0) choice = 1;
                        else
                        {
                            choice = workTour.getSubtourFreqChoice();
                            if (choice == 0) choice++;
                        }

                        int dummy = 0;
                        if (person.getPersonTypeNumber() == 7)
                        {
                            dummy = 1;
                        }

                        // count the results by person type
                        if (awfByPersonType.containsKey(personTypeString))
                        {
                            int[] counterArray = awfByPersonType.get(personTypeString);
                            counterArray[choice - 1]++;
                            awfByPersonType.put(personTypeString, counterArray);

                        } else
                        {
                            int[] counterArray = new int[alternativeNames.length];
                            counterArray[choice - 1]++;
                            awfByPersonType.put(personTypeString, counterArray);
                        }

                    }

                }

            }

        }

        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info("At-Work Subtour Frequency Model Results");

        // count of model results
        logger.info(" ");
        String firstHeader = "Person type                 ";
        String secondHeader = "---------------------------     ";

        for (int i = 0; i < alternativeNames.length; ++i)
        {
            firstHeader += String.format("%16s", alternativeNames[i]);
            secondHeader += "------------    ";
        }

        firstHeader += String.format("%16s", "Total");
        secondHeader += "------------";

        logger.info(firstHeader);
        logger.info(secondHeader);

        int[] columnTotals = new int[alternativeNames.length];

        int lineTotal = 0;
        for (int i = 0; i < Person.PERSON_TYPE_NAME_ARRAY.length; ++i)
        {
            String personTypeString = Person.PERSON_TYPE_NAME_ARRAY[i];
            String stringToLog = String.format("%-28s", personTypeString);

            if (awfByPersonType.containsKey(personTypeString))
            {

                lineTotal = 0;
                int[] countArray = awfByPersonType.get(personTypeString);
                for (int j = 0; j < alternativeNames.length; ++j)
                {
                    stringToLog += String.format("%16d", countArray[j]);
                    columnTotals[j] += countArray[j];
                    lineTotal += countArray[j];
                } // j

            } else
            {
                // if key
                // log zeros
                lineTotal = 0;
                for (int j = 0; j < alternativeNames.length; ++j)
                {
                    stringToLog += String.format("%16d", 0);
                }
            }

            stringToLog += String.format("%16d", lineTotal);

            logger.info(stringToLog);

        } // i

        String stringToLog = String.format("%-28s", "Total");
        lineTotal = 0;
        for (int j = 0; j < alternativeNames.length; ++j)
        {
            stringToLog += String.format("%16d", columnTotals[j]);
            lineTotal += columnTotals[j];
        } // j

        logger.info(secondHeader);
        stringToLog += String.format("%16d", lineTotal);
        logger.info(stringToLog);
        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info(" ");
        logger.info(" ");

    }

    /**
     * Logs the results of the individual tour stop frequency model.
     * 
     */
    public void logIndivStfResults(HouseholdDataManagerIf householdDataManager)
    {

        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info("Individual Tour Stop Frequency Model Results");

        // count of model results
        logger.info(" ");
        String firstHeader = "Tour Purpose     ";
        String secondHeader = "---------------   ";

        int[] obStopsAlt = StopFrequencyDMU.NUM_OB_STOPS_FOR_ALT;
        int[] ibStopsAlt = StopFrequencyDMU.NUM_IB_STOPS_FOR_ALT;

        // 10 purposes
        int[][] chosen = new int[obStopsAlt.length][11];
        HashMap<Integer, String> indexPurposeMap = modelStructure.getIndexPrimaryPurposeNameMap();
        HashMap<String, Integer> purposeIndexMap = modelStructure.getPrimaryPurposeNameIndexMap();

        ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                .getNumHouseholds());

        for (int[] startEndIndices : startEndTaskIndicesList)
        {

            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            // get the array of households
            Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

            for (int i = 0; i < householdArray.length; ++i)
            {

                Person[] personArray = householdArray[i].getPersons();
                for (int j = 1; j < personArray.length; j++)
                {

                    List<Tour> tourList = new ArrayList<Tour>();

                    // apply stop frequency for all person tours
                    tourList.addAll(personArray[j].getListOfWorkTours());
                    tourList.addAll(personArray[j].getListOfSchoolTours());
                    tourList.addAll(personArray[j].getListOfIndividualNonMandatoryTours());
                    tourList.addAll(personArray[j].getListOfAtWorkSubtours());

                    for (Tour t : tourList)
                    {

                        int index = t.getTourPrimaryPurposeIndex();
                        int choice = t.getStopFreqChoice();
                        chosen[choice][index]++;

                    }

                }

            }

        }

        for (int i = 1; i < chosen[1].length; ++i)
        {
            firstHeader += String.format("%18s", indexPurposeMap.get(i));
            secondHeader += "  --------------- ";
        }

        firstHeader += String.format("%18s", "Total");
        secondHeader += "  --------------- ";

        logger.info(firstHeader);
        logger.info(secondHeader);

        int[] columnTotals = new int[chosen[1].length];

        int lineTotal = 0;
        for (int i = 1; i < chosen.length; ++i)
        {
            String stringToLog = String.format("%d out, %d in      ", obStopsAlt[i], ibStopsAlt[i]);

            lineTotal = 0;
            int[] countArray = chosen[i];
            for (int j = 1; j < countArray.length; ++j)
            {
                stringToLog += String.format("%18d", countArray[j]);
                columnTotals[j] += countArray[j];
                lineTotal += countArray[j];
            } // j

            stringToLog += String.format("%18d", lineTotal);

            logger.info(stringToLog);

        } // i

        String stringToLog = String.format("%-17s", "Total");
        lineTotal = 0;
        for (int j = 1; j < chosen[1].length; ++j)
        {
            stringToLog += String.format("%18d", columnTotals[j]);
            lineTotal += columnTotals[j];
        } // j

        logger.info(secondHeader);
        stringToLog += String.format("%18d", lineTotal);
        logger.info(stringToLog);
        logger.info(" ");
        logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        logger.info(" ");
        logger.info(" ");

    }

    private void createSerializedObjectInFileFromObject(Object objectToSerialize,
            String serializedObjectFileName, String serializedObjectKey)
    {
        try
        {
            DataFile dataFile = new DataFile(serializedObjectFileName, 1);
            DataWriter dw = new DataWriter(serializedObjectKey);
            dw.writeObject(objectToSerialize);
            dataFile.insertRecord(dw);
            dataFile.close();
        } catch (NotSerializableException e)
        {
            logger.error(
                    String.format(
                            "NotSerializableException for %s.  Trying to create serialized object with key=%s, in filename=%s.",
                            objectToSerialize.getClass().getName(), serializedObjectKey,
                            serializedObjectFileName), e);
            throw new RuntimeException();
        } catch (IOException e)
        {
            logger.error(String.format(
                    "IOException trying to write disk object file=%s, with key=%s for writing.",
                    serializedObjectFileName, serializedObjectKey), e);
            throw new RuntimeException();
        }
    }

    private Object createObjectFromSerializedObjectInFile(Object newObject,
            String serializedObjectFileName, String serializedObjectKey)
    {
        try
        {
            DataFile dataFile = new DataFile(serializedObjectFileName, "r");
            DataReader dr = dataFile.readRecord(serializedObjectKey);
            newObject = dr.readObject();
            dataFile.close();
            return newObject;
        } catch (IOException e)
        {
            logger.error(String.format(
                    "IOException trying to read disk object file=%s, with key=%s.",
                    serializedObjectFileName, serializedObjectKey), e);
            throw new RuntimeException();
        } catch (ClassNotFoundException e)
        {
            logger.error(String.format(
                    "could not instantiate %s object, with key=%s from filename=%s.", newObject
                            .getClass().getName(), serializedObjectFileName, serializedObjectKey),
                    e);
            throw new RuntimeException();
        }
    }

    private ArrayList<int[]> getWriteHouseholdRanges(int numberOfHouseholds)
    {

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        int startIndex = 0;
        int endIndex = 0;

        while (endIndex < numberOfHouseholds - 1)
        {
            endIndex = startIndex + NUM_WRITE_PACKETS - 1;
            if (endIndex + NUM_WRITE_PACKETS > numberOfHouseholds)
                endIndex = numberOfHouseholds - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += NUM_WRITE_PACKETS;
        }

        return startEndIndexList;

    }

}
