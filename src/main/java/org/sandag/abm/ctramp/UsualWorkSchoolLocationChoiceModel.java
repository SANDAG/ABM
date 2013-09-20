package org.sandag.abm.ctramp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;

public class UsualWorkSchoolLocationChoiceModel
        implements Serializable
{

    private transient Logger         logger                                             = Logger.getLogger(UsualWorkSchoolLocationChoiceModel.class);

    private static final String      USE_NEW_SOA_METHOD_PROPERTY_KEY                    = "uwsl.use.new.soa";

    private static final String      PROPERTIES_DC_SOA_WORK_SAMPLE_SIZE                 = "uwsl.work.soa.SampleSize";
    private static final String      PROPERTIES_DC_SOA_SCHOOL_SAMPLE_SIZE               = "uwsl.school.soa.SampleSize";
    private static final String      PROPERTIES_UEC_USUAL_LOCATION                      = "uwsl.dc.uec.file";
    private static final String      PROPERTIES_UEC_USUAL_LOCATION_NEW                  = "uwsl.dc2.uec.file";
    private static final String      PROPERTIES_UEC_USUAL_LOCATION_SOA                  = "uwsl.soa.uec.file";

    private static final String      PROPERTIES_RESULTS_WORK_SCHOOL_LOCATION_CHOICE     = "Results.UsualWorkAndSchoolLocationChoice";

    private static final String      PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_PACKET_SIZE = "distributed.task.packet.size";

    private static final String      WORK_SCHOOL_SEGMENTS_FILE_NAME                     = "workSchoolSegments.definitions";

    private static final int[]       WORK_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX            = {1, 1, 1,
            1, 1, 1                                                                     };
    private static final int[]       WORK_LOC_SEGMENT_TO_UEC_SHEET_INDEX                = {2, 2, 2,
            2, 2, 2                                                                     };
    private static int               PACKET_SIZE                                        = 0;

    // TODO: see if we can eliminate the setup synchronization issues - otherwise the
    // number of these small
    // packets can be fine-tuned and set in properties file..

    // The number of initialization packets are the number of "small" packets
    // submited at the beginning of a
    // distributed task to minimize synchronization issues that significantly slow
    // down model object setup.
    // It is assumed that after theses small packets have run, all the model objects
    // will have been setup,
    // and the task objects can process much bigger chuncks of households.
    private static String            PROPERTIES_NUM_INITIALIZATION_PACKETS              = "number.initialization.packets";
    private static String            PROPERTIES_INITIALIZATION_PACKET_SIZE              = "initialization.packet.size";
    private static int               NUM_INITIALIZATION_PACKETS                         = 0;
    private static int               INITIALIZATION_PACKET_SIZE                         = 0;

    private static final int         NUM_WRITE_PACKETS                                  = 1000;

    private String                   wsLocResultsFileName;

    private transient ResourceBundle resourceBundle;

    private MgraDataManager          mgraManager;
    private TazDataManager           tdm;

    private int                      maxTaz;

    private MatrixDataServerIf       ms;
    private ModelStructure           modelStructure;
    private CtrampDmuFactoryIf       dmuFactory;

    private String                   workLocUecFileName;
    private String                   schoolLocUecFileName;
    private String                   soaUecFileName;
    private int                      soaWorkSampleSize;
    private int                      soaSchoolSampleSize;

    private HashSet<Integer>         skipSegmentIndexSet;

    private BuildAccessibilities     aggAcc;
    private DestChoiceSize           workerDcSizeObj;
    private DestChoiceSize           schoolDcSizeObj;

    private String                   restartModelString;

    private JPPFClient               jppfClient                                         = null;

    private boolean                  useNewSoaMethod;

    public UsualWorkSchoolLocationChoiceModel(ResourceBundle resourceBundle,
            String restartModelString, JPPFClient jppfClient, ModelStructure modelStructure,
            MatrixDataServerIf ms, CtrampDmuFactoryIf dmuFactory, BuildAccessibilities aggAcc)
    {

        // set the local variables
        this.resourceBundle = resourceBundle;
        this.modelStructure = modelStructure;
        this.dmuFactory = dmuFactory;
        this.ms = ms;
        this.jppfClient = jppfClient;
        this.restartModelString = restartModelString;
        this.aggAcc = aggAcc;

        try
        {
            PACKET_SIZE = Integer.parseInt(resourceBundle
                    .getString(PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_PACKET_SIZE));
        } catch (MissingResourceException e)
        {
            PACKET_SIZE = 0;
        }

        try
        {
            NUM_INITIALIZATION_PACKETS = Integer.parseInt(resourceBundle
                    .getString(PROPERTIES_NUM_INITIALIZATION_PACKETS));
        } catch (MissingResourceException e)
        {
            NUM_INITIALIZATION_PACKETS = 0;
        }

        try
        {
            INITIALIZATION_PACKET_SIZE = Integer.parseInt(resourceBundle
                    .getString(PROPERTIES_INITIALIZATION_PACKET_SIZE));
        } catch (MissingResourceException e)
        {
            INITIALIZATION_PACKET_SIZE = 0;
        }

        try
        {
            wsLocResultsFileName = resourceBundle
                    .getString(PROPERTIES_RESULTS_WORK_SCHOOL_LOCATION_CHOICE);
        } catch (MissingResourceException e)
        {
            wsLocResultsFileName = null;
        }

        String uecPath = ResourceUtil.getProperty(resourceBundle,
                CtrampApplication.PROPERTIES_UEC_PATH);

        // get the sample-of-alternatives sample size
        soaWorkSampleSize = ResourceUtil.getIntegerProperty(resourceBundle,
                PROPERTIES_DC_SOA_WORK_SAMPLE_SIZE);
        soaSchoolSampleSize = ResourceUtil.getIntegerProperty(resourceBundle,
                PROPERTIES_DC_SOA_SCHOOL_SAMPLE_SIZE);

        useNewSoaMethod = ResourceUtil.getBooleanProperty(resourceBundle,
                USE_NEW_SOA_METHOD_PROPERTY_KEY);

        // locate the UECs for destination choice, sample of alts, and mode choice
        String usualWorkLocationUecFileName;
        String usualSchoolLocationUecFileName;
        if (useNewSoaMethod)
        {
            usualWorkLocationUecFileName = ResourceUtil.getProperty(resourceBundle,
                    PROPERTIES_UEC_USUAL_LOCATION_NEW);
            usualSchoolLocationUecFileName = ResourceUtil.getProperty(resourceBundle,
                    PROPERTIES_UEC_USUAL_LOCATION_NEW);
        } else
        {
            usualWorkLocationUecFileName = ResourceUtil.getProperty(resourceBundle,
                    PROPERTIES_UEC_USUAL_LOCATION);
            usualSchoolLocationUecFileName = ResourceUtil.getProperty(resourceBundle,
                    PROPERTIES_UEC_USUAL_LOCATION);
        }
        workLocUecFileName = uecPath + usualWorkLocationUecFileName;
        schoolLocUecFileName = uecPath + usualSchoolLocationUecFileName;

        String usualLocationSoaUecFileName = ResourceUtil.getProperty(resourceBundle,
                PROPERTIES_UEC_USUAL_LOCATION_SOA);
        soaUecFileName = uecPath + usualLocationSoaUecFileName;

        mgraManager = MgraDataManager.getInstance();

    }

    public void runWorkLocationChoiceModel(HouseholdDataManagerIf householdDataManager,
            double[][] workerSizeTerms)
    {

        HashMap<String, String> propertyMap = ResourceUtil
                .changeResourceBundleIntoHashMap(resourceBundle);

        // get the map of size term segment values to names
        HashMap<Integer, Integer> occupValueIndexMap = aggAcc.getWorkOccupValueIndexMap();

        HashMap<Integer, String> workSegmentIndexNameMap = aggAcc.getWorkSegmentIndexNameMap();
        HashMap<String, Integer> workSegmentNameIndexMap = aggAcc.getWorkSegmentNameIndexMap();

        int maxShadowPriceIterations = Integer.parseInt(propertyMap
                .get(DestChoiceSize.PROPERTIES_WORK_DC_SHADOW_NITER));

        // create an object for calculating destination choice attraction size terms
        // and managing shadow price calculations.
        workerDcSizeObj = new DestChoiceSize(propertyMap, workSegmentIndexNameMap,
                workSegmentNameIndexMap, workerSizeTerms, maxShadowPriceIterations);

        int[][] originLocationsByHomeMgra = householdDataManager
                .getWorkersByHomeMgra(occupValueIndexMap);

        // balance the size variables
        workerDcSizeObj.balanceSizeVariables(originLocationsByHomeMgra);

        if (PACKET_SIZE == 0) PACKET_SIZE = householdDataManager.getNumHouseholds();

        int currentIter = 0;
        String fileName = propertyMap
                .get(CtrampApplication.PROPERTIES_WORK_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE);
        if (fileName != null)
        {
            if (fileName.length() > 2)
            {
                String projectDirectory = ResourceUtil.getProperty(resourceBundle,
                        CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
                workerDcSizeObj.restoreShadowPricingInfo(projectDirectory + fileName);
                int underScoreIndex = fileName.lastIndexOf('_');
                int dotIndex = fileName.lastIndexOf('.');
                currentIter = Integer.parseInt(fileName.substring(underScoreIndex + 1, dotIndex));
                currentIter++;
            }
        }

        // String restartFlag = propertyMap.get(
        // CtrampApplication.PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER );
        // if ( restartFlag == null )
        // restartFlag = "none";
        // if ( restartFlag.equalsIgnoreCase("none") )
        // currentIter = 0;

        long initTime = System.currentTimeMillis();

        // shadow pricing iterations
        for (int iter = 0; iter < workerDcSizeObj.getMaxShadowPriceIterations(); iter++)
        {

            try
            {
                JPPFJob job = new JPPFJob();
                job.setId("Work Location Choice Job");

                ArrayList<int[]> startEndTaskIndicesList = getTaskHouseholdRanges(householdDataManager
                        .getNumHouseholds());

                DataProvider dataProvider = new MemoryMapDataProvider();
                dataProvider.setValue("propertyMap", propertyMap);
                dataProvider.setValue("ms", ms);
                dataProvider.setValue("hhDataManager", householdDataManager);
                dataProvider.setValue("modelStructure", modelStructure);
                dataProvider.setValue("uecIndices", WORK_LOC_SEGMENT_TO_UEC_SHEET_INDEX);
                dataProvider.setValue("soaUecIndices", WORK_LOC_SOA_SEGMENT_TO_UEC_SHEET_INDEX);
                dataProvider.setValue("tourCategory", ModelStructure.MANDATORY_CATEGORY);
                dataProvider.setValue("dcSizeObj", workerDcSizeObj);
                dataProvider.setValue("dcUecFileName", workLocUecFileName);
                dataProvider.setValue("soaUecFileName", soaUecFileName);
                dataProvider.setValue("soaSampleSize", soaWorkSampleSize);
                dataProvider.setValue("dmuFactory", dmuFactory);
                dataProvider.setValue("restartModelString", restartModelString);

                job.setDataProvider(dataProvider);

                int startIndex = 0;
                int endIndex = 0;
                int taskIndex = 1;
                WorkLocationChoiceTaskJppf myTask = null;
                WorkLocationChoiceTaskJppfNew myTaskNew = null;
                for (int[] startEndIndices : startEndTaskIndicesList)
                {
                    startIndex = startEndIndices[0];
                    endIndex = startEndIndices[1];

                    if (useNewSoaMethod)
                    {
                        myTaskNew = new WorkLocationChoiceTaskJppfNew(taskIndex, startIndex,
                                endIndex, iter);
                        job.addTask(myTaskNew);
                    } else
                    {
                        myTask = new WorkLocationChoiceTaskJppf(taskIndex, startIndex, endIndex,
                                iter);
                        job.addTask(myTask);
                    }
                    taskIndex++;
                }

                logger.info("Usual work location choice model submitting tasks to jppf job");
                List<JPPFTask> results = jppfClient.submit(job);
                for (JPPFTask task : results)
                {
                    if (task.getException() != null) throw task.getException();

                    try
                    {
                        String stringResult = (String) task.getResult();
                        logger.info(stringResult);
                        System.out.println(stringResult);
                    } catch (Exception e)
                    {
                        logger.error("", e);
                        throw new RuntimeException();
                    }

                }

            } catch (Exception e)
            {
                e.printStackTrace();
            }

            // sum the chosen destinations by purpose, dest zone and subzone for
            // shadow pricing adjustment
            int[][] finalModeledDestChoiceLocationsByDestMgra = householdDataManager
                    .getWorkToursByDestMgra(occupValueIndexMap);

            int[] numChosenDests = new int[workSegmentNameIndexMap.size()];

            for (int i = 0; i < numChosenDests.length; i++)
            {
                for (int j = 1; j <= mgraManager.getMaxMgra(); j++)
                    numChosenDests[i] += finalModeledDestChoiceLocationsByDestMgra[i][j];
            }

            logger.info(String
                    .format("Usual work location choice tasks completed for shadow price iteration %d in %d seconds.",
                            iter, ((System.currentTimeMillis() - initTime) / 1000)));
            logger.info(String.format("Chosen dests by segment:"));
            double total = 0;
            for (int i = 0; i < numChosenDests.length; i++)
            {
                String segmentString = workSegmentIndexNameMap.get(i);
                logger.info(String.format("\t%-8d%-15s = %10d", i + 1, segmentString,
                        numChosenDests[i]));
                total += numChosenDests[i];
            }
            logger.info(String.format("\t%-8s%-15s = %10.0f", "total", "", total));

            // apply the shadow price adjustments
            workerDcSizeObj.reportMaxDiff(iter, finalModeledDestChoiceLocationsByDestMgra);
            workerDcSizeObj.saveWorkMaxDiffValues(iter, finalModeledDestChoiceLocationsByDestMgra);
            workerDcSizeObj.updateShadowPrices(finalModeledDestChoiceLocationsByDestMgra);
            workerDcSizeObj.updateSizeVariables();
            workerDcSizeObj.updateShadowPricingInfo(currentIter, originLocationsByHomeMgra,
                    finalModeledDestChoiceLocationsByDestMgra, "work");

            householdDataManager.setUwslRandomCount(currentIter);

            currentIter++;

        } // iter

        logger.info("Usual work location choices computed in "
                + ((System.currentTimeMillis() - initTime) / 1000) + " seconds.");
        ;

    }

    public void runSchoolLocationChoiceModel(HouseholdDataManagerIf householdDataManager,
            double[][] schoolSizeTerms, double[][] schoolFactors)
    {

        HashMap<String, String> propertyMap = ResourceUtil
                .changeResourceBundleIntoHashMap(resourceBundle);

        // get the maps of segment names and indices for school location choice size
        HashMap<Integer, String> schoolSegmentIndexNameMap = aggAcc.getSchoolSegmentIndexNameMap();
        HashMap<String, Integer> schoolSegmentNameIndexMap = aggAcc.getSchoolSegmentNameIndexMap();

        int maxShadowPriceIterations = Integer.parseInt(propertyMap
                .get(DestChoiceSize.PROPERTIES_SCHOOL_DC_SHADOW_NITER));

        // create an object for calculating destination choice attraction size terms
        // and managing shadow price calculations.
        schoolDcSizeObj = new DestChoiceSize(propertyMap, schoolSegmentIndexNameMap,
                schoolSegmentNameIndexMap, schoolSizeTerms, maxShadowPriceIterations);

        // get the set of segment indices for which shadow pricing should be skipped.
        skipSegmentIndexSet = aggAcc.getNoShadowPriceSchoolSegmentIndexSet();
        schoolDcSizeObj.setNoShadowPriceSchoolSegmentIndices(skipSegmentIndexSet);

        // set the school segment external factors calculated for university segment in the method that called this one.
        schoolDcSizeObj.setExternalFactors(schoolFactors);

        householdDataManager.setSchoolDistrictMappings(schoolSegmentNameIndexMap,
                aggAcc.getMgraGsDistrict(), aggAcc.getMgraHsDistrict(),
                aggAcc.getGsDistrictIndexMap(), aggAcc.getHsDistrictIndexMap());

        int[][] originLocationsByHomeMgra = householdDataManager.getStudentsByHomeMgra();

        // balance the size variables
        schoolDcSizeObj.balanceSizeVariables(originLocationsByHomeMgra);

        if (PACKET_SIZE == 0) PACKET_SIZE = householdDataManager.getNumHouseholds();

        int currentIter = 0;
        String fileName = propertyMap
                .get(CtrampApplication.PROPERTIES_SCHOOL_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE);
        if (fileName != null)
            if (fileName.length() > 2)
            {
                {
                    String projectDirectory = ResourceUtil.getProperty(resourceBundle,
                            CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
                    schoolDcSizeObj.restoreShadowPricingInfo(projectDirectory + fileName);
                    int underScoreIndex = fileName.lastIndexOf('_');
                    int dotIndex = fileName.lastIndexOf('.');
                    currentIter = Integer.parseInt(fileName
                            .substring(underScoreIndex + 1, dotIndex));
                    currentIter++;
                }
            }
        // String restartFlag = propertyMap.get(
        // CtrampApplication.PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER );
        // if ( restartFlag == null )
        // restartFlag = "none";
        // if ( restartFlag.equalsIgnoreCase("none") )
        // currentIter = 0;

        long initTime = System.currentTimeMillis();

        // shadow pricing iterations
        for (int iter = 0; iter < schoolDcSizeObj.getMaxShadowPriceIterations(); iter++)
        {

            // logger.info( String.format( "Size of Household[] in bytes = %d.",
            // householdDataManager.getBytesUsedByHouseholdArray() ) );

            try
            {
                JPPFJob job = new JPPFJob();
                job.setId("School Location Choice Job");

                ArrayList<int[]> startEndTaskIndicesList = getTaskHouseholdRanges(householdDataManager
                        .getNumHouseholds());

                DataProvider dataProvider = new MemoryMapDataProvider();
                dataProvider.setValue("propertyMap", propertyMap);
                dataProvider.setValue("ms", ms);
                dataProvider.setValue("hhDataManager", householdDataManager);
                dataProvider.setValue("modelStructure", modelStructure);
                dataProvider.setValue("tourCategory", ModelStructure.MANDATORY_CATEGORY);
                dataProvider.setValue("dcSizeObj", schoolDcSizeObj);
                dataProvider.setValue("dcUecFileName", schoolLocUecFileName);
                dataProvider.setValue("soaUecFileName", soaUecFileName);
                dataProvider.setValue("soaSampleSize", soaSchoolSampleSize);
                dataProvider.setValue("dmuFactory", dmuFactory);
                dataProvider.setValue("restartModelString", restartModelString);

                job.setDataProvider(dataProvider);

                int startIndex = 0;
                int endIndex = 0;
                int taskIndex = 1;
                SchoolLocationChoiceTaskJppf myTask = null;
                SchoolLocationChoiceTaskJppfNew myTaskNew = null;
                for (int[] startEndIndices : startEndTaskIndicesList)
                {
                    startIndex = startEndIndices[0];
                    endIndex = startEndIndices[1];

                    if (useNewSoaMethod)
                    {
                        myTaskNew = new SchoolLocationChoiceTaskJppfNew(taskIndex, startIndex,
                                endIndex, iter);
                        job.addTask(myTaskNew);
                    } else
                    {
                        myTask = new SchoolLocationChoiceTaskJppf(taskIndex, startIndex, endIndex,
                                iter);
                        job.addTask(myTask);
                    }
                    taskIndex++;
                }

                List<JPPFTask> results = jppfClient.submit(job);
                for (JPPFTask task : results)
                {
                    if (task.getException() != null) throw task.getException();

                    try
                    {
                        String stringResult = (String) task.getResult();
                        logger.info(stringResult);
                        System.out.println(stringResult);
                    } catch (Exception e)
                    {
                        logger.error("", e);
                        throw new RuntimeException();
                    }

                }

            } catch (Exception e)
            {
                e.printStackTrace();
            }

            // sum the chosen destinations by purpose, dest zone and subzone for
            // shadow pricing adjustment
            int[][] finalModeledDestChoiceLocationsByDestMgra = householdDataManager
                    .getSchoolToursByDestMgra();

            int[] numChosenDests = new int[schoolSegmentIndexNameMap.size()];

            for (int i = 0; i < numChosenDests.length; i++)
            {
                for (int j = 1; j <= mgraManager.getMaxMgra(); j++)
                    numChosenDests[i] += finalModeledDestChoiceLocationsByDestMgra[i][j];
            }

            logger.info(String.format(
                    "Usual school location choice tasks completed for shadow price iteration %d.",
                    iter));
            logger.info(String.format("Chosen dests by segment:"));
            double total = 0;
            for (int i = 0; i < numChosenDests.length; i++)
            {
                String segmentString = schoolSegmentIndexNameMap.get(i);
                logger.info(String.format("\t%-8d%-20s = %10d", i + 1, segmentString,
                        numChosenDests[i]));
                total += numChosenDests[i];
            }
            logger.info(String.format("\t%-8s%-20s = %10.0f", "total", "", total));

            logger.info(String
                    .format("Usual school location choice tasks completed for shadow price iteration %d in %d seconds.",
                            iter, ((System.currentTimeMillis() - initTime) / 1000)));

            // apply the shadow price adjustments
            schoolDcSizeObj.reportMaxDiff(iter, finalModeledDestChoiceLocationsByDestMgra);
            schoolDcSizeObj
                    .saveSchoolMaxDiffValues(iter, finalModeledDestChoiceLocationsByDestMgra);
            schoolDcSizeObj.updateShadowPrices(finalModeledDestChoiceLocationsByDestMgra);
            schoolDcSizeObj.updateSizeVariables();
            schoolDcSizeObj.updateShadowPricingInfo(currentIter, originLocationsByHomeMgra,
                    finalModeledDestChoiceLocationsByDestMgra, "school");

            householdDataManager.setUwslRandomCount(currentIter);

            currentIter++;

        } // iter

        logger.info("Usual school location choices computed in "
                + ((System.currentTimeMillis() - initTime) / 1000) + " seconds.");
        ;

    }

    /**
     * Loops through the households in the HouseholdDataManager, gets the households and persons and writes a row with detail on each of these in a
     * file.
     * 
     * @param householdDataManager
     *            is the object from which the array of household objects can be retrieved.
     * @param projectDirectory
     *            is the root directory for the output file named
     */
    public void saveResults(HouseholdDataManagerIf householdDataManager, String projectDirectory,
            int globalIteration)
    {

        HashMap<String, Integer> workSegmentNameIndexMap = modelStructure
                .getWorkSegmentNameIndexMap();
        HashMap<String, Integer> schoolSegmentNameIndexMap = modelStructure
                .getSchoolSegmentNameIndexMap();

        FileWriter writer;
        PrintWriter outStream = null;

        if (wsLocResultsFileName != null)
        {

            // insert '_' and the global iteration number at end of filename or
            // before '.' if there is a file extension in the name.
            int dotIndex = wsLocResultsFileName.indexOf('.');
            if (dotIndex < 0)
            {
                wsLocResultsFileName = String
                        .format("%s_%d", wsLocResultsFileName, globalIteration);
            } else
            {
                String base = wsLocResultsFileName.substring(0, dotIndex);
                String extension = wsLocResultsFileName.substring(dotIndex);
                wsLocResultsFileName = String.format("%s_%d%s", base, globalIteration, extension);
            }

            wsLocResultsFileName = projectDirectory + wsLocResultsFileName;

            try
            {
                writer = new FileWriter(new File(wsLocResultsFileName));
                outStream = new PrintWriter(new BufferedWriter(writer));
            } catch (IOException e)
            {
                logger.fatal(String.format("Exception occurred opening wsLoc results file: %s.",
                        wsLocResultsFileName));
                throw new RuntimeException(e);
            }

            // write header
            outStream
                    .println("HHID,HomeMGRA,Income,PersonID,PersonNum,PersonType,PersonAge,EmploymentCategory,StudentCategory,WorkSegment,SchoolSegment,WorkLocation,WorkLocationDistance,WorkLocationLogsum,SchoolLocation,SchoolLocationDistance,SchoolLocationLogsum");

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

                    int hhId = household.getHhId();
                    int homeMgra = household.getHhMgra();
                    int income = household.getIncomeInDollars();

                    Person[] personArray = household.getPersons();

                    for (int j = 1; j < personArray.length; ++j)
                    {

                        Person person = personArray[j];

                        int personId = person.getPersonId();
                        int personNum = person.getPersonNum();
                        int personType = person.getPersonTypeNumber();
                        int personAge = person.getAge();
                        int employmentCategory = person.getPersonEmploymentCategoryIndex();
                        int studentCategory = person.getPersonStudentCategoryIndex();

                        int schoolSegmentIndex = person.getSchoolLocationSegmentIndex();
                        int workSegmentIndex = person.getWorkLocationSegmentIndex();

                        int workLocation = person.getUsualWorkLocation();
                        int schoolLocation = person.getUsualSchoolLocation();

                        // write data record
                        outStream.println(String.format(
                                "%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.5e,%.5e,%d,%.5e,%.5e", hhId,
                                homeMgra, income, personId, personNum, personType, personAge,
                                employmentCategory, studentCategory, workSegmentIndex,
                                schoolSegmentIndex, workLocation, person.getWorkLocationDistance(),
                                person.getWorkLocationLogsum(), schoolLocation,
                                person.getSchoolLocationDistance(),
                                person.getSchoolLocationLogsum()));

                    }

                }

            }

            outStream.close();

        }

        // save the mappings between segment index and segment labels to a file for
        // workers and students
        String fileName = projectDirectory + WORK_SCHOOL_SEGMENTS_FILE_NAME;

        try
        {
            writer = new FileWriter(new File(fileName));
            outStream = new PrintWriter(new BufferedWriter(writer));
        } catch (IOException e)
        {
            logger.fatal(String.format(
                    "Exception occurred opening work/school segment definitions file: %s.",
                    fileName));
            throw new RuntimeException(e);
        }

        outStream
                .println("Correspondence table for work location segment indices and work location segment names");
        outStream.println(String.format("%-15s %-20s", "Index", "Segment Name"));

        String[] names = new String[workSegmentNameIndexMap.size() + 1];
        for (String key : workSegmentNameIndexMap.keySet())
        {
            int index = workSegmentNameIndexMap.get(key);
            names[index] = key;
        }

        for (int i = 0; i < names.length; i++)
        {
            if (names[i] != null) outStream.println(String.format("%-15d %-20s", i, names[i]));
        }

        outStream.println("");
        outStream.println("");
        outStream.println("");

        outStream
                .println("Correspondence table for school location segment indices and school location segment names");
        outStream.println(String.format("%-15s %-20s", "Index", "Segment Name"));

        names = new String[schoolSegmentNameIndexMap.size() + 1];
        for (String key : schoolSegmentNameIndexMap.keySet())
        {
            int index = schoolSegmentNameIndexMap.get(key);
            names[index] = key;
        }

        for (int i = 0; i < names.length; i++)
        {
            if (names[i] != null) outStream.println(String.format("%-15d %-20s", i, names[i]));
        }

        outStream.println("");

        outStream.close();

    }

    private ArrayList<int[]> getTaskHouseholdRanges(int numberOfHouseholds)
    {

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        int numInitializationHouseholds = NUM_INITIALIZATION_PACKETS * INITIALIZATION_PACKET_SIZE;

        int startIndex = 0;
        int endIndex = 0;
        if (numInitializationHouseholds < numberOfHouseholds)
        {

            while (endIndex < numInitializationHouseholds)
            {
                endIndex = startIndex + INITIALIZATION_PACKET_SIZE - 1;

                int[] startEndIndices = new int[2];
                startEndIndices[0] = startIndex;
                startEndIndices[1] = endIndex;
                startEndIndexList.add(startEndIndices);

                startIndex += INITIALIZATION_PACKET_SIZE;
            }

        }

        while (endIndex < numberOfHouseholds - 1)
        {
            endIndex = startIndex + PACKET_SIZE - 1;
            if (endIndex + PACKET_SIZE > numberOfHouseholds) endIndex = numberOfHouseholds - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += PACKET_SIZE;
        }

        return startEndIndexList;

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
