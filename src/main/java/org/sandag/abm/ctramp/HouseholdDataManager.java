package org.sandag.abm.ctramp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import umontreal.iro.lecuyer.probdist.LognormalDist;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import com.pb.common.util.ObjectUtil;
import com.pb.common.util.SeededRandom;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from
 *         synthetic population files.
 */
public abstract class HouseholdDataManager
        implements HouseholdDataManagerIf, Serializable
{

    protected transient Logger        logger                                                  = Logger.getLogger(HouseholdDataManager.class);

    public static final String        PROPERTIES_SYNPOP_INPUT_HH                              = "PopulationSynthesizer.InputToCTRAMP.HouseholdFile";
    public static final String        PROPERTIES_SYNPOP_INPUT_PERS                            = "PopulationSynthesizer.InputToCTRAMP.PersonFile";

    public static final String        RANDOM_SEED_NAME                                        = "Model.Random.Seed";

    public static final String        OUTPUT_HH_DATA_FILE_TARGET                              = "outputHouseholdData.file";
    public static final String        OUTPUT_PERSON_DATA_FILE_TARGET                          = "outputPersonData.file";

    public static final String        READ_UWSL_RESULTS_FILE                                  = "read.uwsl.results";
    public static final String        READ_UWSL_RESULTS_FILENAME                              = "read.uwsl.filename";
    public static final String        READ_PRE_AO_RESULTS_FILE                                = "read.pre.ao.results";
    public static final String        READ_PRE_AO_RESULTS_FILENAME                            = "read.pre.ao.filename";
    
    public static final String   PROPERTIES_DISTRIBUTED_TIME = "distributedTimeCoefficients";


    // HHID,household_serial_no,TAZ,MGRA,VEH,PERSONS,HWORKERS,HINCCAT1,HINC,UNITTYPE,HHT,BLDGSZ
    public static final String        HH_ID_FIELD_NAME                                        = "HHID";
    public static final String        HH_HOME_TAZ_FIELD_NAME                                  = "TAZ";
    public static final String        HH_HOME_MGRA_FIELD_NAME                                 = "MGRA";
    public static final String        HH_INCOME_CATEGORY_FIELD_NAME                           = "HINCCAT1";
    public static final String        HH_INCOME_DOLLARS_FIELD_NAME                            = "HINC";
    public static final String        HH_WORKERS_FIELD_NAME                                   = "HWORKERS";
    public static final String        HH_AUTOS_FIELD_NAME                                     = "VEH";
    public static final String        HH_SIZE_FIELD_NAME                                      = "PERSONS";
    public static final String        HH_TYPE_FIELD_NAME                                      = "HHT";
    public static final String        HH_BLDGSZ_FIELD_NAME                                    = "BLDGSZ";
    public static final String        HH_UNITTYPE_FIELD_NAME                                  = "UNITTYPE";

    // HHID,PERID,AGE,SEX,OCCCEN1,INDCEN,PEMPLOY,PSTUDENT,PTYPE,EDUC,GRADE
    public static final String        PERSON_HH_ID_FIELD_NAME                                 = "HHID";
    public static final String        PERSON_PERSON_ID_FIELD_NAME                             = "PERID";
    public static final String        PERSON_AGE_FIELD_NAME                                   = "AGE";
    public static final String        PERSON_GENDER_FIELD_NAME                                = "SEX";
    public static final String        PERSON_MILITARY_FIELD_NAME                              = "MILTARY";
    public static final String        PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME                   = "PEMPLOY";
    public static final String        PERSON_STUDENT_CATEGORY_FIELD_NAME                      = "PSTUDENT";
    public static final String        PERSON_TYPE_CATEGORY_FIELD_NAME                         = "PTYPE";
    public static final String        PERSON_EDUCATION_ATTAINMENT_FIELD_NAME                  = "EDUC";
    public static final String        PERSON_GRADE_ENROLLED_FIELD_NAME                        = "GRADE";
    public static final String        PERSON_OCCCEN1_FIELD_NAME                               = "OCCCEN1";
    public static final String        PERSON_SOC_FIELD_NAME                                   = "OCCSOC5";
    public static final String        PERSON_INDCEN_FIELD_NAME                                = "INDCEN";
    
    public static final String        PERSON_TIMEFACTOR_WORK_FIELD_NAME                       = "timeFactorWork";
    public static final String        PERSON_TIMEFACTOR_NONWORK_FIELD_NAME                    = "timeFactorNonWork";
  
    public static final String        PROPERTIES_HOUSEHOLD_TRACE_LIST                         = "Debug.Trace.HouseholdIdList";
    public static final String        DEBUG_HHS_ONLY_KEY                                      = "Process.Debug.HHs.Only";

    private static final String       PROPERTIES_MIN_VALUE_OF_TIME_KEY                        = "HouseholdManager.MinValueOfTime";
    private static final String       PROPERTIES_MAX_VALUE_OF_TIME_KEY                        = "HouseholdManager.MaxValueOfTime";
    private static final String       PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY                = "HouseholdManager.MeanValueOfTime.Values";
    private static final String       PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY         = "HouseholdManager.MeanValueOfTime.Income.Limits";
    private static final String       PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY = "HouseholdManager.HH.ValueOfTime.Multiplier.Under18";
    private static final String       PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY     = "HouseholdManager.Mean.ValueOfTime.Multiplier.Mu";
    private static final String       PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY            = "HouseholdManager.ValueOfTime.Lognormal.Sigma";

    private HashMap<String, Integer>  schoolSegmentNameIndexMap;
    private HashMap<Integer, Integer> gsDistrictSegmentMap;
    private HashMap<Integer, Integer> hsDistrictSegmentMap;
    private int[]                     mgraGsDistrict;
    private int[]                     mgraHsDistrict;

    // these are not used for sandag; instead sandag uses distributed time coefficients read in the person file
    protected float                   hhValueOfTimeMultiplierForPersonUnder18;
    protected double                  meanValueOfTimeMultiplierBeforeLogForMu;
    protected double                  valueOfTimeLognormalSigma;
    protected float                   minValueOfTime;
    protected float                   maxValueOfTime;
    protected float[]                 meanValueOfTime;
    protected int[]                   incomeDollarLimitsForValueOfTime;
    protected LognormalDist[]         valueOfTimeDistribution;

    protected HashMap<String, String> propertyMap;

    protected String                  projectDirectory;
    protected String                  outputHouseholdFileName;
    protected String                  outputPersonFileName;

    protected ModelStructure          modelStructure;

    protected TableDataSet            hhTable;
    protected TableDataSet            personTable;

    protected HashSet<Integer>        householdTraceSet;

    protected Household[]             hhs;
    protected int[]                   hhIndexArray;

    protected int                     inputRandomSeed;
    protected int                     numPeriods;
    protected int                     firstPeriod;

    protected float                   sampleRate;
    protected int                     sampleSeed;

    protected int                     maximumNumberOfHouseholdsPerFile                        = 0;
    protected int                     numberOfHouseholdDiskObjectFiles                        = 0;

    protected MgraDataManager         mgraManager;
    protected TazDataManager          tazManager;

    protected double[]                percentHhsIncome100Kplus;
    protected double[]                percentHhsMultipleAutos;
    
    protected boolean 				readTimeFactors;
    public HouseholdDataManager()
    {
    }

    /**
     * Associate data in hh and person TableDataSets read from synthetic
     * population files with Household objects and Person objects with
     * Households.
     */
    protected abstract void mapTablesToHouseholdObjects();

    public String testRemote()
    {
        System.out.println("testRemote() called by remote process.");
        return String.format("testRemote() method in %s called.", this.getClass()
                .getCanonicalName());
    }

    public void setDebugHhIdsFromHashmap()
    {

        householdTraceSet = new HashSet<Integer>();

        // get the household ids for which debug info is required
        String householdTraceStringList = propertyMap.get(PROPERTIES_HOUSEHOLD_TRACE_LIST);

        if (householdTraceStringList != null)
        {
            StringTokenizer householdTokenizer = new StringTokenizer(householdTraceStringList, ",");
            while (householdTokenizer.hasMoreTokens())
            {
                String listValue = householdTokenizer.nextToken();
                int idValue = Integer.parseInt(listValue.trim());
                householdTraceSet.add(idValue);
            }
        }

    }

    public void readPopulationFiles(String inputHouseholdFileName, String inputPersonFileName)
    {
    	
        TimeCoefficientDistributions timeDistributions = new TimeCoefficientDistributions();
        timeDistributions.createTimeDistributions(propertyMap);
        timeDistributions.appendTimeDistributionsOnPersonFile(propertyMap);

        // read synthetic population files
        readHouseholdData(inputHouseholdFileName);

        readPersonData(inputPersonFileName);
    }

    public void setModelStructure(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
    }

    public void setupHouseholdDataManager(ModelStructure modelStructure,
            String inputHouseholdFileName, String inputPersonFileName)
    {

        mgraManager = MgraDataManager.getInstance(propertyMap);
        tazManager = TazDataManager.getInstance(propertyMap);

        setModelStructure(modelStructure);
        readPopulationFiles(inputHouseholdFileName, inputPersonFileName);

        // Set the seed for the JVM default SeededRandom object - should only be
        // used
        // to set the order for the
        // HH index array so that hhs can be processed in an arbitrary order as
        // opposed to the order imposed by
        // the synthetic population generator.
        // The seed was set as a command line argument for the model run, or the
        // default if no argument supplied
        SeededRandom.setSeed(sampleSeed);

        // the seed read from the properties file controls seeding the Household
        // object random number generator objects.
        inputRandomSeed = Integer.parseInt(propertyMap.get(HouseholdDataManager.RANDOM_SEED_NAME));

        // map synthetic population table data to objects to be used by CT-RAMP
        mapTablesToHouseholdObjects();
        hhTable = null;
        personTable = null;

        logPersonSummary();

        setTraceHouseholdSet();

        // if read pre-ao results flag is set, read the results file and
        // populate the
        // household object ao result field from these values.
        String readPreAoResultsString = propertyMap.get(READ_PRE_AO_RESULTS_FILE);
        if (readPreAoResultsString != null)
        {
            boolean readResults = Boolean.valueOf(readPreAoResultsString);
            if (readResults) readPreAoResults();
        }

        // if read uwsl results flag is set, read the results file and populate
        // the
        // household object work/school location result fields from these
        // values.
        String readUwslResultsString = propertyMap.get(READ_UWSL_RESULTS_FILE);
        if (readUwslResultsString != null)
        {
            boolean readResults = Boolean.valueOf(readUwslResultsString);
            if (readResults) readWorkSchoolLocationResults();
        }

        //check if we want to read distributed time factors from the person file
        String readTimeFactorsString = propertyMap.get(PROPERTIES_DISTRIBUTED_TIME);
        if (readTimeFactorsString != null)
        {
        	readTimeFactors = Boolean.valueOf(readTimeFactorsString);
        	logger.info("Distributed time coefficients = "+Boolean.toString(readTimeFactors));
        }
     

        
    }

    public void setPropertyFileValues(HashMap<String, String> propertyMap)
    {

        String propertyValue = "";
        this.propertyMap = propertyMap;

        // save the project specific parameters in class attributes
        this.projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);

        outputHouseholdFileName = propertyMap
                .get(CtrampApplication.PROPERTIES_OUTPUT_HOUSEHOLD_FILE);
        outputPersonFileName = propertyMap.get(CtrampApplication.PROPERTIES_OUTPUT_PERSON_FILE);

        setDebugHhIdsFromHashmap();

        propertyValue = propertyMap
                .get(CtrampApplication.PROPERTIES_SCHEDULING_NUMBER_OF_TIME_PERIODS);
        if (propertyValue == null) numPeriods = 0;
        else numPeriods = Integer.parseInt(propertyValue);

        propertyValue = propertyMap.get(CtrampApplication.PROPERTIES_SCHEDULING_FIRST_TIME_PERIOD);
        if (propertyValue == null) firstPeriod = 0;
        else firstPeriod = Integer.parseInt(propertyValue);
        
        //check if we want to read distributed time factors from the person file
        String readTimeFactorsString = propertyMap.get(PROPERTIES_DISTRIBUTED_TIME);
        if (readTimeFactorsString != null)
        {
        	readTimeFactors = Boolean.valueOf(readTimeFactorsString);
        	logger.info("Distributed time coefficients = "+Boolean.toString(readTimeFactors));
        }

    }

    public int[] getRandomOrderHhIndexArray(int numHhs)
    {

        Random myRandom = new Random();
        myRandom.setSeed(numHhs + 1);

        int[] data = new int[numHhs];
        for (int i = 0; i < numHhs; i++)
            data[i] = (int) (10000000 * myRandom.nextDouble());

        int[] index = IndexSort.indexSort(data);

        return index;
    }

    // this is called at the end of UsualWorkSchoolLocation model step.
    public void setUwslRandomCount(int iter)
    {

        for (int r = 0; r < hhs.length; r++)
            hhs[r].setUwslRandomCount(iter, hhs[r].getHhRandomCount());

    }

    private void resetRandom(Household h, int count)
    {
        // get the household's Random
        Random r = h.getHhRandom();

        int seed = inputRandomSeed + h.getHhId();
        r.setSeed(seed);

        // select count Random draws to reset this household's Random to it's
        // state
        // prior to
        // the model run for which model results were stored in
        // HouseholdDataManager.
        for (int i = 0; i < count; i++)
            r.nextDouble();

        // reset the randomCount for the household's Random
        h.setHhRandomCount(count);
    }

    public void resetPreAoRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // The Pre Auto Ownership model is the first model component, so
            // reset
            // counts to 0.
            resetRandom(hhs[i], 0);
        }
    }

    public void resetUwslRandom(int iter)
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current random count for the end of the shadow price
            // iteration
            // passed in.
            // this value was set at the end of UsualWorkSchoolLocation model
            // step
            // for the given iter.
            // if < 0, random count should be set to the count at end of pre
            // auto
            // ownership.
            int uwslCount = hhs[i].getPreAoRandomCount();
            if (iter >= 0)
            {
                uwslCount = hhs[i].getUwslRandomCount(iter);
            }

            // draw uwslCount random numbers from the household's Random
            resetRandom(hhs[i], uwslCount);
        }
    }

    public void resetAoRandom(int iter)
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Auto Ownership model from the
            // Household
            // object.
            // this value was set at the end of UsualWorkSchoolLocation model
            // step.

            int aoCount = hhs[i].getUwslRandomCount(iter);

            // draw aoCount random numbers from the household's Random
            resetRandom(hhs[i], aoCount);
        }
    }

    public void resetTpRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Auto Ownership model from the
            // Household
            // object.
            // this value was set at the end of UsualWorkSchoolLocation model
            // step.
            int tpCount = hhs[i].getAoRandomCount();

            // draw aoCount random numbers from the household's Random
            resetRandom(hhs[i], tpCount);
        }
    }

    public void resetFpRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Auto Ownership model from the
            // Household
            // object.
            // this value was set at the end of UsualWorkSchoolLocation model
            // step.
            int fpCount = hhs[i].getTpRandomCount();

            // draw aoCount random numbers from the household's Random
            resetRandom(hhs[i], fpCount);
        }
    }

    public void resetIeRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Auto Ownership model from the
            // Household
            // object.
            // this value was set at the end of UsualWorkSchoolLocation model
            // step.
            int ieCount = hhs[i].getFpRandomCount();

            // draw aoCount random numbers from the household's Random
            resetRandom(hhs[i], ieCount);
        }
    }

    public void resetCdapRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Coordinated Daily Activity Pattern
            // model from the Household object.
            // this value was set at the end of Auto Ownership model step.
            int cdapCount = hhs[i].getIeRandomCount();

            // draw cdapCount random numbers
            resetRandom(hhs[i], cdapCount);
        }
    }

    public void resetImtfRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Individual Mandatory Tour
            // Frequency
            // model from the Household object.
            // this value was set at the end of Coordinated Daily Activity
            // Pattern
            // model step.
            int imtfCount = hhs[i].getCdapRandomCount();

            // draw imtfCount random numbers
            resetRandom(hhs[i], imtfCount);
        }
    }

    public void resetImtodRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Individual Mandatory Tour
            // Departure and
            // duration model from the Household object.
            // this value was set at the end of Individual Mandatory Tour
            // Frequency
            // model step.
            int imtodCount = hhs[i].getImtfRandomCount();

            // draw imtodCount random numbers
            resetRandom(hhs[i], imtodCount);
        }
    }

    public void resetJtfRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Joint Tour Frequency model from
            // the
            // Household object.
            // this value was set at the end of Individual Mandatory departure
            // time
            // Choice model step.
            int jtfCount = hhs[i].getImtodRandomCount();

            // draw jtfCount random numbers
            resetRandom(hhs[i], jtfCount);
        }
    }

    public void resetJtlRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Joint Tour Location model from the
            // Household object.
            // this value was set at the end of Joint Tour Frequency model step.
            int jtlCount = hhs[i].getJtfRandomCount();

            // draw jtlCount random numbers
            resetRandom(hhs[i], jtlCount);
        }
    }

    public void resetJtodRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Joint Tour Departure and duration
            // model
            // from the Household object.
            // this value was set at the end of Joint Tour Location model step.
            int jtodCount = hhs[i].getJtlRandomCount();

            // draw jtodCount random numbers
            resetRandom(hhs[i], jtodCount);
        }
    }

    public void resetInmtfRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Individual non-mandatory tour
            // frequency
            // model from the Household object.
            // this value was set at the end of Joint Tour Departure and
            // duration
            // model step.
            int inmtfCount = hhs[i].getJtodRandomCount();

            // draw inmtfCount random numbers
            resetRandom(hhs[i], inmtfCount);
        }
    }

    public void resetInmtlRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Individual non-mandatory tour
            // location
            // model from the Household object.
            // this value was set at the end of Individual non-mandatory tour
            // frequency model step.
            int inmtlCount = hhs[i].getInmtfRandomCount();

            // draw inmtlCount random numbers
            resetRandom(hhs[i], inmtlCount);
        }
    }

    public void resetInmtodRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to Individual non-mandatory tour
            // departure
            // and duration model from the Household object.
            // this value was set at the end of Individual non-mandatory tour
            // location model step.
            int inmtodCount = hhs[i].getInmtlRandomCount();

            // draw inmtodCount random numbers
            resetRandom(hhs[i], inmtodCount);
        }
    }

    public void resetAwfRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to At-work Subtour Frequency model
            // from
            // the Household object.
            // this value was set at the end of Individual Non-Mandatory Tour
            // Departure and duration model step.
            int awfCount = hhs[i].getInmtodRandomCount();

            // draw awfCount random numbers
            resetRandom(hhs[i], awfCount);
        }
    }

    public void resetAwlRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to At-work Subtour Location Choice
            // model
            // from the Household object.
            // this value was set at the end of At-work Subtour Frequency model
            // step.
            int awlCount = hhs[i].getAwfRandomCount();

            // draw awlCount random numbers
            resetRandom(hhs[i], awlCount);
        }
    }

    public void resetAwtodRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to At-work Subtour Time-of-day and
            // mode
            // choice model from the Household object.
            // this value was set at the end of At-work Subtour Location Choice
            // model
            // step.
            int awtodCount = hhs[i].getAwlRandomCount();

            // draw awtodCount random numbers
            resetRandom(hhs[i], awtodCount);
        }
    }

    public void resetStfRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to stop frequency model from the
            // Household
            // object.
            // this value was set at the end of At-work Subtour Time-of-day and
            // mode
            // choice model step.
            int stfCount = hhs[i].getAwtodRandomCount();

            // draw stfCount random numbers
            resetRandom(hhs[i], stfCount);
        }
    }

    public void resetStlRandom()
    {
        for (int i = 0; i < hhs.length; i++)
        {
            // get the current count prior to stop location model from the
            // Household
            // object.
            // this value was set at the end of stop frequency model step.
            int stlCount = hhs[i].getStfRandomCount();

            // draw stlCount random numbers
            resetRandom(hhs[i], stlCount);
        }
    }

    /**
     * Sets the HashSet used to trace households for debug purposes and sets the
     * debug switch for each of the listed households. Also sets
     */
    public void setTraceHouseholdSet()
    {

        // loop through the households in the set and set the trace switches
        for (int i = 0; i < hhs.length; i++)
            hhs[i].setDebugChoiceModels(false);

        for (int id : householdTraceSet)
        {
            int index = hhIndexArray[id];
            hhs[index].setDebugChoiceModels(true);
        }

    }

    /**
     * Sets the sample rate used to run the model for a portion of the
     * households.
     * 
     * @param sampleRate
     *            , proportion of total households for which to run the model
     *            [0.0, 1.0].
     */
    public void setHouseholdSampleRate(float sampleRate, int sampleSeed)
    {
        this.sampleRate = sampleRate;
        this.sampleSeed = sampleSeed;
    }

    public void setHhArray(Household[] hhArray)
    {
        hhs = hhArray;
    }

    public void setHhArray(Household[] tempHhs, int startIndex)
    {
        // long startTime = System.currentTimeMillis();
        // logger.info(String.format("start setHhArray for startIndex=%d, startTime=%d.",
        // startIndex,
        // startTime));
        for (int i = 0; i < tempHhs.length; i++)
        {
            hhs[startIndex + i] = tempHhs[i];
        }
        // long endTime = System.currentTimeMillis();
        // logger.info(String.format(
        // "end setHhArray for startIndex=%d, endTime=%d, elapsed=%d millisecs.",
        // startIndex,
        // endTime, (endTime - startTime)));
    }

    /**
     * return the array of Household objects holding the synthetic population
     * and choice model outcomes.
     * 
     * @return hhs
     */
    public Household[] getHhArray()
    {
        return hhs;
    }

    public Household[] getHhArray(int first, int last)
    {
        // long startTime = System.currentTimeMillis();
        // logger.info(String.format("start getHhArray for first=%d, last=%d, startTime=%d.",
        // first, last, startTime));
        Household[] tempHhs = new Household[last - first + 1];
        for (int i = 0; i < tempHhs.length; i++)
        {
            tempHhs[i] = hhs[first + i];
        }
        // long endTime = System.currentTimeMillis();
        // logger.info(String.format(
        // "end getHhArray for first=%d, last=%d, endTime=%d, elapsed=%d millisecs.",
        // first, last, endTime, (endTime - startTime)));
        return tempHhs;
    }

    public int getArrayIndex(int hhId)
    {
        int i = hhIndexArray[hhId];
        return i;
    }

    /**
     * return the number of household objects read from the synthetic
     * population.
     * 
     * @return
     */
    public int getNumHouseholds()
    {
        // hhs is dimesioned to number of households + 1.
        return hhs.length;
    }

    /**
     * set walk segment (0-none, 1-short, 2-long walk to transit access) for the
     * origin for this tour
     */
    public int getInitialOriginWalkSegment(int taz, double randomNumber)
    {
        // double[] proportions = tazDataManager.getZonalWalkPercentagesForTaz(
        // taz
        // );
        // return ChoiceModelApplication.getMonteCarloSelection(proportions,
        // randomNumber);
        return 0;
    }

    private void readHouseholdData(String inputHouseholdFileName)
    {

        // construct input household file name from properties file values
        String fileName = projectDirectory + "/" + inputHouseholdFileName;

        try
        {
            logger.info("reading popsyn household data file.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            hhTable = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading synthetic household data file: %s into TableDataSet object.",
                            fileName));
            throw new RuntimeException(e);
        }

    }

    private void readPersonData(String inputPersonFileName)
    {

        // construct input person file name from properties file values
        String fileName = projectDirectory + "/" + inputPersonFileName;

        try
        {
            logger.info("reading popsyn person data file.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            personTable = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading synthetic person data file: %s into TableDataSet object.",
                            fileName));
            throw new RuntimeException(e);
        }

    }

    public void logPersonSummary()
    {

        HashMap<String, HashMap<String, int[]>> summaryResults;

        summaryResults = new HashMap<String, HashMap<String, int[]>>();

        for (int i = 0; i < hhs.length; ++i)
        {

            Household household = hhs[i];

            Person[] personArray = household.getPersons();
            for (int j = 1; j < personArray.length; ++j)
            {
                Person person = personArray[j];
                String personType = person.getPersonType();

                String employmentStatus = person.getPersonEmploymentCategory();
                String studentStatus = person.getPersonStudentCategory();
                int age = person.getAge();
                int ageCategory;
                if (age <= 5)
                {
                    ageCategory = 0;
                } else if (age <= 15)
                {
                    ageCategory = 1;
                } else if (age <= 18)
                {
                    ageCategory = 2;
                } else if (age <= 24)
                {
                    ageCategory = 3;
                } else if (age <= 44)
                {
                    ageCategory = 4;
                } else if (age <= 64)
                {
                    ageCategory = 5;
                } else
                {
                    ageCategory = 6;
                }

                if (summaryResults.containsKey(personType))
                {
                    // have person type
                    if (summaryResults.get(personType).containsKey(employmentStatus))
                    {
                        // have employment category
                        summaryResults.get(personType).get(employmentStatus)[ageCategory] += 1;
                    } else
                    {
                        // don't have employment category
                        summaryResults.get(personType).put(employmentStatus, new int[7]);
                        summaryResults.get(personType).get(employmentStatus)[ageCategory] += 1;
                    }
                    if (summaryResults.get(personType).containsKey(studentStatus))
                    {
                        // have student category
                        summaryResults.get(personType).get(studentStatus)[ageCategory] += 1;
                    } else
                    {
                        // don't have student category
                        summaryResults.get(personType).put(studentStatus, new int[7]);
                        summaryResults.get(personType).get(studentStatus)[ageCategory] += 1;
                    }
                } else
                {
                    // don't have person type
                    summaryResults.put(personType, new HashMap<String, int[]>());
                    summaryResults.get(personType).put(studentStatus, new int[7]);
                    summaryResults.get(personType).get(studentStatus)[ageCategory] += 1;
                    summaryResults.get(personType).put(employmentStatus, new int[7]);
                    summaryResults.get(personType).get(employmentStatus)[ageCategory] += 1;
                }
            }
        }
        String headerRow = String.format("%5s\t", "Age\t");
        for (String empCategory : Person.EMPLOYMENT_CATEGORY_NAME_ARRAY)
        {
            headerRow += String.format("%16s\t", empCategory);
        }
        for (String stuCategory : Person.STUDENT_CATEGORY_NAME_ARRAY)
        {
            headerRow += String.format("%16s\t", stuCategory);
        }
        String[] ageCategories = {"0-5", "6-15", "16-18", "19-24", "25-44", "45-64", "65+"};

        for (String personType : summaryResults.keySet())
        {

            logger.info("Summary for person type: " + personType);

            logger.info(headerRow);
            String row = "";

            HashMap<String, int[]> personTypeSummary = summaryResults.get(personType);

            for (int j = 0; j < ageCategories.length; ++j)
            {
                row = String.format("%5s\t", ageCategories[j]);
                for (String empCategory : Person.EMPLOYMENT_CATEGORY_NAME_ARRAY)
                {
                    if (personTypeSummary.containsKey(empCategory))
                    {
                        row += String.format("%16d\t", personTypeSummary.get(empCategory)[j]);
                    } else row += String.format("%16d\t", 0);
                }
                for (String stuCategory : Person.STUDENT_CATEGORY_NAME_ARRAY)
                {
                    if (personTypeSummary.containsKey(stuCategory))
                    {
                        row += String.format("%16d\t", personTypeSummary.get(stuCategory)[j]);
                    } else row += String.format("%16d\t", 0);
                }
                logger.info(row);
            }

        }

    }

    public int[][] getTourPurposePersonsByHomeMgra(String[] purposeList)
    {

        int maxMgra = mgraManager.getMaxMgra();
        int[][] personsWithMandatoryPurpose = new int[purposeList.length][maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < hhs.length; r++)
        {

            Person[] persons = hhs[r].getPersons();

            int homeMgra = hhs[r].getHhMgra();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                int purposeIndex = -1;
                try
                {

                    if (person.getPersonIsWorker() == 1)
                    {

                        purposeIndex = person.getWorkLocationSegmentIndex();
                        personsWithMandatoryPurpose[purposeIndex][homeMgra]++;

                    }

                    if (person.getPersonIsPreschoolChild() == 1
                            || person.getPersonIsStudentDriving() == 1
                            || person.getPersonIsStudentNonDriving() == 1
                            || person.getPersonIsUniversityStudent() == 1)
                    {

                        purposeIndex = person.getSchoolLocationSegmentIndex();
                        personsWithMandatoryPurpose[purposeIndex][homeMgra]++;

                    }

                } catch (RuntimeException e)
                {
                    logger.error(String
                            .format("exception caught summing workers/students by origin zone for household table record r=%d.",
                                    r));
                    throw e;
                }

            }

        } // r (households)

        return personsWithMandatoryPurpose;

    }

    public double[] getPercentHhsIncome100Kplus()
    {
        return percentHhsIncome100Kplus;
    }

    public double[] getPercentHhsMultipleAutos()
    {
        return percentHhsMultipleAutos;
    }

    public void computeTransponderChoiceTazPercentArrays()
    {

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new BufferedWriter(new FileWriter(new File(
                    "./transpChoiceArrays.csv"))));
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int maxTaz = tazManager.maxTaz;
        int[] numHhs = new int[maxTaz + 1];

        // for percent of households in TAZ with income > $100K
        percentHhsIncome100Kplus = new double[maxTaz + 1];
        // for percent og households in TAZ with multiple autos
        percentHhsMultipleAutos = new double[maxTaz + 1];

        for (int r = 0; r < getNumHouseholds(); r++)
        {
            int homeMgra = hhs[r].getHhMgra();
            int homeTaz = mgraManager.getTaz(homeMgra);
            numHhs[homeTaz]++;
            if (hhs[r].getIncomeInDollars() > 100000) percentHhsIncome100Kplus[homeTaz]++;
            if (hhs[r].getAutosOwned() > 1) percentHhsMultipleAutos[homeTaz]++;
        }

        out.println("taz,numHhsTaz,numHhsIncome100KplusTaz,numHhsMultipleAutosTaz,proportionHhsIncome100KplusTaz,proportionHhsMultipleAutosTaz");

        for (int i = 0; i <= maxTaz; i++)
        {

            out.print(i + "," + numHhs[i] + "," + percentHhsIncome100Kplus[i] + ","
                    + percentHhsMultipleAutos[i]);
            if (numHhs[i] > 0)
            {
                percentHhsIncome100Kplus[i] /= numHhs[i];
                percentHhsMultipleAutos[i] /= numHhs[i];
                out.println("," + percentHhsIncome100Kplus[i] + "," + percentHhsMultipleAutos[i]);
            } else
            {
                out.println("," + 0.0 + "," + 0.0);
            }
        }

        out.close();
    }

    public int[][] getWorkersByHomeMgra(HashMap<Integer, Integer> segmentValueIndexMap)
    {

        int maxMgra = mgraManager.getMaxMgra();

        int[][] workersByHomeMgra = new int[segmentValueIndexMap.size()][maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < getNumHouseholds(); r++)
        {

            Person[] persons = hhs[r].getPersons();

            int homeMgra = hhs[r].getHhMgra();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                if (person.getPersonIsFullTimeWorker() == 1
                        || person.getPersonIsPartTimeWorker() == 1)
                {

                    int occup = -1;
                    int segmentIndex = -1;
                    try
                    {

                        occup = person.getPersPecasOccup();
                        segmentIndex = segmentValueIndexMap.get(occup);
                        workersByHomeMgra[segmentIndex][homeMgra]++;

                    } catch (Exception e)
                    {
                        logger.error(
                                String.format(
                                        "exception caught summing workers by origin MGRA for household table record r=%d, person=%d, homeMgra=%d, occup=%d, segmentIndex=%d.",
                                        r, person.getPersonNum(), homeMgra, occup, segmentIndex), e);
                        throw new RuntimeException();
                    }

                }

            }

        } // r (households)

        return workersByHomeMgra;

    }

    public int[][] getStudentsByHomeMgra()
    {

        int maxMgra = mgraManager.getMaxMgra();

        // there are 5 school types - preschool, K-8, HS, University with
        // typical
        // students, University with non-typical students.
        int[][] studentsByHomeMgra = new int[schoolSegmentNameIndexMap.size()][maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < getNumHouseholds(); r++)
        {

            Person[] persons = hhs[r].getPersons();

            int homeMgra = hhs[r].getHhMgra();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                if (person.getPersonIsPreschoolChild() == 1
                        || person.getPersonIsStudentNonDriving() == 1
                        || person.getPersonIsStudentDriving() == 1
                        || person.getPersonIsUniversityStudent() == 1)
                {

                    int segmentIndex = -1;
                    try
                    {

                        if (person.getPersonIsPreschoolChild() == 1)
                        {
                            segmentIndex = schoolSegmentNameIndexMap
                                    .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.PRESCHOOL_SEGMENT_GROUP_INDEX]);
                        } else if (person.getPersonIsGradeSchool() == 1)
                        {
                            int gsDistrict = mgraGsDistrict[homeMgra];
                            segmentIndex = gsDistrictSegmentMap.get(gsDistrict);
                        } else if (person.getPersonIsHighSchool() == 1)
                        {
                            int hsDistrict = mgraHsDistrict[homeMgra];
                            segmentIndex = hsDistrictSegmentMap.get(hsDistrict);
                        } else if (person.getPersonIsUniversityStudent() == 1
                                && person.getAge() < 30)
                        {
                            segmentIndex = schoolSegmentNameIndexMap
                                    .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_TYPICAL_SEGMENT_GROUP_INDEX]);
                        } else if (person.getPersonIsUniversityStudent() == 1
                                && person.getAge() >= 30)
                        {
                            segmentIndex = schoolSegmentNameIndexMap
                                    .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX]);
                        }

                        // if person type is a student but segment index is -1,
                        // the person is not enrolled; assume home schooled and
                        // don't add to sum by home mgra
                        if (segmentIndex >= 0) studentsByHomeMgra[segmentIndex][homeMgra]++;

                    } catch (Exception e)
                    {
                        logger.error(
                                String.format(
                                        "exception caught summing students by origin MGRA for household table record r=%d, person=%d, homeMgra=%d, segmentIndex=%d.",
                                        r, person.getPersonNum(), homeMgra, segmentIndex), e);
                        throw new RuntimeException();
                    }

                }

            }

        } // r (households)

        return studentsByHomeMgra;

    }

    public int[] getIndividualNonMandatoryToursByHomeMgra(String purposeString)
    {

        // dimension the array
        int maxMgra = mgraManager.getMaxMgra();
        int[] individualNonMandatoryTours = new int[maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        int count = 0;
        for (int r = 0; r < hhs.length; r++)
        {

            Person[] persons = hhs[r].getPersons();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                ArrayList<Tour> it = person.getListOfIndividualNonMandatoryTours();

                try
                {

                    if (it.size() == 0) continue;

                    for (Tour tour : it)
                    {
                        // increment the segment count if it's the right purpose
                        String tourPurpose = tour.getTourPurpose();
                        if (purposeString.startsWith(tourPurpose))
                        {
                            int homeMgra = hhs[r].getHhMgra();
                            individualNonMandatoryTours[homeMgra]++;
                            count++;
                        }
                    }

                } catch (RuntimeException e)
                {
                    logger.error(String
                            .format("exception caught counting number of individualNonMandatory tours for purpose: %s, for household table record r=%d, personNum=%d.",
                                    purposeString, r, person.getPersonNum()));
                    throw e;
                }

            }

        } // r (households)

        return individualNonMandatoryTours;
    }

    public int[][] getWorkToursByDestMgra(HashMap<Integer, Integer> segmentValueIndexMap)
    {

        // dimension the array
        int maxMgra = mgraManager.getMaxMgra();
        int destMgra = 0;

        int[][] workTours = new int[segmentValueIndexMap.size()][maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < getNumHouseholds(); r++)
        {

            Person[] persons = hhs[r].getPersons();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                int occup = -1;
                int segmentIndex = -1;
                try
                {

                    if (person.getPersonIsWorker() == 1)
                    {

                        destMgra = person.getWorkLocation();

                        if (destMgra != ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                        {
                            occup = person.getPersPecasOccup();
                            segmentIndex = segmentValueIndexMap.get(occup);
                            workTours[segmentIndex][destMgra]++;
                        }

                    }

                } catch (Exception e)
                {
                    logger.error(
                            String.format(
                                    "exception caught summing workers by work location MGRA for household table record r=%d, person=%d, workMgra=%d, occup=%d, segmentIndex=%d.",
                                    r, person.getPersonNum(), destMgra, occup, segmentIndex), e);
                    throw new RuntimeException();
                }

            }

        } // r (households)

        return workTours;

    }

    public int[] getWorksAtHomeBySegment(HashMap<Integer, Integer> segmentValueIndexMap)
    {

        int destMgra = 0;

        int[] workAtHome = new int[segmentValueIndexMap.size()];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < getNumHouseholds(); r++)
        {

            Person[] persons = hhs[r].getPersons();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                int occup = -1;
                int segmentIndex = -1;
                try
                {

                    if (person.getPersonIsWorker() == 1)
                    {

                        destMgra = person.getWorkLocation();

                        if (destMgra == ModelStructure.WORKS_AT_HOME_LOCATION_INDICATOR)
                        {
                            occup = person.getPersPecasOccup();
                            segmentIndex = segmentValueIndexMap.get(occup);
                            workAtHome[segmentIndex]++;
                        }

                    }

                } catch (Exception e)
                {
                    logger.error(
                            String.format(
                                    "exception caught summing workers by work location MGRA for household table record r=%d, person=%d, workMgra=%d, occup=%d, segmentIndex=%d.",
                                    r, person.getPersonNum(), destMgra, occup, segmentIndex), e);
                    throw new RuntimeException();
                }

            }

        } // r (households)

        return workAtHome;

    }

    public void setSchoolDistrictMappings(HashMap<String, Integer> segmentNameIndexMap,
            int[] mgraGsDist, int[] mgraHsDist, HashMap<Integer, Integer> gsDistSegMap,
            HashMap<Integer, Integer> hsDistSegMap)
    {

        schoolSegmentNameIndexMap = segmentNameIndexMap;
        gsDistrictSegmentMap = gsDistSegMap;
        hsDistrictSegmentMap = hsDistSegMap;
        mgraGsDistrict = mgraGsDist;
        mgraHsDistrict = mgraHsDist;
    }

    public int[][] getSchoolToursByDestMgra()
    {

        // dimension the array
        int maxMgra = mgraManager.getMaxMgra();
        int destMgra = 0;

        int[][] schoolTours = new int[schoolSegmentNameIndexMap.size()][maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        for (int r = 0; r < getNumHouseholds(); r++)
        {

            Person[] persons = hhs[r].getPersons();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];
                destMgra = person.getPersonSchoolLocationZone();
                if (destMgra == 0) continue;

                int segmentIndex = -1;
                try
                {

                    if (person.getPersonIsPreschoolChild() == 1)
                    {
                        segmentIndex = schoolSegmentNameIndexMap
                                .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.PRESCHOOL_SEGMENT_GROUP_INDEX]);
                    } else if (person.getPersonIsGradeSchool() == 1)
                    {
                        int gsDistrict = mgraGsDistrict[destMgra];
                        segmentIndex = gsDistrictSegmentMap.get(gsDistrict);
                    } else if (person.getPersonIsHighSchool() == 1)
                    {
                        int hsDistrict = mgraHsDistrict[destMgra];
                        segmentIndex = hsDistrictSegmentMap.get(hsDistrict);
                    } else if (person.getPersonIsUniversityStudent() == 1 && person.getAge() < 30)
                    {
                        segmentIndex = schoolSegmentNameIndexMap
                                .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_TYPICAL_SEGMENT_GROUP_INDEX]);
                    } else if (person.getPersonIsUniversityStudent() == 1 && person.getAge() >= 30)
                    {
                        segmentIndex = schoolSegmentNameIndexMap
                                .get(BuildAccessibilities.SCHOOL_DC_SIZE_SEGMENT_NAME_LIST[BuildAccessibilities.UNIV_NONTYPICAL_SEGMENT_GROUP_INDEX]);
                    }

                    // if person type is a student but segment index is -1, the
                    // person is not enrolled; assume home schooled and don't
                    // add to sum by home mgra
                    if (segmentIndex >= 0) schoolTours[segmentIndex][destMgra]++;

                } catch (Exception e)
                {
                    logger.error(
                            String.format(
                                    "exception caught summing students by origin MGRA for household table record r=%d, person=%d, schoolMgra=%d, segmentIndex=%d.",
                                    r, person.getPersonNum(), destMgra, segmentIndex), e);
                    throw new RuntimeException();
                }

            }

        } // r (households)

        return schoolTours;

    }

    public int[] getJointToursByHomeZoneSubZone(String purposeString)
    {

        // dimension the array
        int maxMgra = mgraManager.getMaxMgra();

        int[] jointTours = new int[maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        int count = 0;
        for (int r = 0; r < hhs.length; r++)
        {

            try
            {

                Tour[] jt = hhs[r].getJointTourArray();

                if (jt == null) continue;

                for (int i = 0; i < jt.length; i++)
                {
                    // increment the segment count if it's the right purpose
                    if (jt[i].getTourPurpose().equalsIgnoreCase(purposeString))
                    {
                        int homeMgra = hhs[r].getHhMgra();
                        jointTours[homeMgra]++;
                        count++;
                    }
                }

            } catch (RuntimeException e)
            {
                logger.error(String
                        .format("exception caught counting number of joint tours for purpose: %s, for household table record r=%d.",
                                purposeString, r));
                throw e;
            }

        } // r (households)

        return jointTours;
    }

    public int[] getAtWorkSubtoursByWorkMgra(String purposeString)
    {

        // dimension the array
        int maxMgra = mgraManager.getMaxMgra();

        int[] subtours = new int[maxMgra + 1];

        // hhs is dimesioned to number of households + 1.
        int count = 0;
        for (int r = 0; r < hhs.length; r++)
        {

            Person[] persons = hhs[r].getPersons();

            for (int p = 1; p < persons.length; p++)
            {

                Person person = persons[p];

                ArrayList<Tour> subtourList = person.getListOfAtWorkSubtours();

                try
                {

                    if (subtourList.size() == 0) continue;

                    for (Tour tour : subtourList)
                    {
                        // increment the segment count if it's the right purpose
                        String tourPurpose = tour.getTourPurpose();
                        if (tourPurpose.startsWith(purposeString))
                        {
                            int workZone = tour.getTourOrigMgra();
                            subtours[workZone]++;
                            count++;
                        }
                    }

                } catch (RuntimeException e)
                {
                    logger.error(String
                            .format("exception caught counting number of at-work subtours for purpose: %s, for household table record r=%d, personNum=%d, count=0%d.",
                                    purposeString, r, person.getPersonNum(), count));
                    throw e;
                }

            }

        } // r (households)

        return subtours;
    }

    public void readWorkSchoolLocationResults()
    {

        String[] headings = {"HHID", "HomeMGRA", "Income", "PersonID", "PersonNum", "PersonType",
                "PersonAge", "EmploymentCategory", "StudentCategory", "WorkSegment",
                "SchoolSegment", "WorkLocation", "WorkLocationDistance", "WorkLocationLogsum",
                "SchoolLocation", "SchoolLocationDistance", "SchoolLocationLogsum"};

        String wsLocResultsFileName = propertyMap.get(READ_UWSL_RESULTS_FILENAME);

        // open the input stream
        String delimSet = ",";
        BufferedReader uwslStream = null;
        String fileName = projectDirectory + wsLocResultsFileName;
        try
        {
            uwslStream = new BufferedReader(new FileReader(new File(fileName)));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // first parse the results file field names from the first record and
        // associate column position with fields used in model
        HashMap<Integer, String> indexHeadingMap = new HashMap<Integer, String>();

        String line = "";
        try
        {
            line = uwslStream.readLine();
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        StringTokenizer st = new StringTokenizer(line, delimSet);
        int col = 0;
        while (st.hasMoreTokens())
        {
            String label = st.nextToken();
            for (String heading : headings)
            {
                if (heading.equalsIgnoreCase(label))
                {
                    indexHeadingMap.put(col, heading);
                    break;
                }
            }
            col++;
        }

        Household hh = null;
        Person person = null;

        try
        {

            while ((line = uwslStream.readLine()) != null)
            {

                // set the line number for the next line in the sample of
                // households
                // int sortedSampleIndex =
                // sortedIndices[sortedSample[sampleCount]];

                // get the household id and personNum first, before parsing
                // other
                // fields. Skip to next record if not in the sample.
                col = 0;
                int id = -1;
                int personNum = -1;
                int workLocation = -1;
                int schoolLocation = -1;
                st = new StringTokenizer(line, delimSet);
                while (st.hasMoreTokens())
                {
                    String fieldValue = st.nextToken();
                    if (indexHeadingMap.containsKey(col))
                    {
                        String fieldName = indexHeadingMap.get(col++);

                        if (fieldName.equalsIgnoreCase("HHID"))
                        {
                            id = Integer.parseInt(fieldValue);

                            int index = getArrayIndex(id);
                            hh = hhs[index];
                        } else if (fieldName.equalsIgnoreCase("PersonNum"))
                        {
                            personNum = Integer.parseInt(fieldValue);
                            person = hh.getPerson(personNum);
                        } else if (fieldName.equalsIgnoreCase("WorkSegment"))
                        {
                            int workSegment = Integer.parseInt(fieldValue);
                            person.setWorkLocationSegmentIndex(workSegment);
                        } else if (fieldName.equalsIgnoreCase("workLocation"))
                        {
                            workLocation = Integer.parseInt(fieldValue);
                            person.setWorkLocation(workLocation);
                        } else if (fieldName.equalsIgnoreCase("WorkLocationDistance"))
                        {
                            float distance = Float.parseFloat(fieldValue);
                            person.setWorkLocDistance(distance);
                        } else if (fieldName.equalsIgnoreCase("WorkLocationLogsum"))
                        {
                            float logsum = Float.parseFloat(fieldValue);
                            person.setWorkLocLogsum(logsum);
                        } else if (fieldName.equalsIgnoreCase("SchoolSegment"))
                        {
                            int schoolSegment = Integer.parseInt(fieldValue);
                            person.setSchoolLocationSegmentIndex(schoolSegment);
                        } else if (fieldName.equalsIgnoreCase("SchoolLocation"))
                        {
                            schoolLocation = Integer.parseInt(fieldValue);
                            person.setSchoolLoc(schoolLocation);
                        } else if (fieldName.equalsIgnoreCase("SchoolLocationDistance"))
                        {
                            float distance = Float.parseFloat(fieldValue);
                            person.setSchoolLocDistance(distance);
                        } else if (fieldName.equalsIgnoreCase("SchoolLocationLogsum"))
                        {
                            float logsum = Float.parseFloat(fieldValue);
                            person.setSchoolLocLogsum(logsum);
                            break;
                        }

                    } else
                    {
                        col++;
                    }

                }

            }

        } catch (NumberFormatException e)
        {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public void readPreAoResults()
    {

        String[] headings = {"HHID", "AO"};

        String preAoResultsFileName = propertyMap.get(READ_PRE_AO_RESULTS_FILENAME);

        // open the input stream
        String delimSet = ",";
        BufferedReader inStream = null;
        String fileName = projectDirectory + preAoResultsFileName;
        try
        {
            inStream = new BufferedReader(new FileReader(new File(fileName)));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // first parse the results file field names from the first record and
        // associate column position with fields used in model
        HashMap<Integer, String> indexHeadingMap = new HashMap<Integer, String>();

        String line = "";
        try
        {
            line = inStream.readLine();
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        StringTokenizer st = new StringTokenizer(line, delimSet);
        int col = 0;
        while (st.hasMoreTokens())
        {
            String label = st.nextToken();
            for (String heading : headings)
            {
                if (heading.equalsIgnoreCase(label))
                {
                    indexHeadingMap.put(col, heading);
                    break;
                }
            }
            col++;
        }

        Household hh = null;

        try
        {

            while ((line = inStream.readLine()) != null)
            {

                // set the line number for the next line in the sample of
                // households
                // int sortedSampleIndex =
                // sortedIndices[sortedSample[sampleCount]];

                // get the household id first, before parsing other fields. Skip
                // to
                // next record if not in the sample.
                col = 0;
                int id = -1;
                int ao = -1;
                st = new StringTokenizer(line, delimSet);
                while (st.hasMoreTokens())
                {
                    String fieldValue = st.nextToken();
                    if (indexHeadingMap.containsKey(col))
                    {
                        String fieldName = indexHeadingMap.get(col++);

                        if (fieldName.equalsIgnoreCase("HHID"))
                        {
                            id = Integer.parseInt(fieldValue);

                            int index = getArrayIndex(id);
                            hh = hhs[index];
                        } else if (fieldName.equalsIgnoreCase("AO"))
                        {
                            ao = Integer.parseInt(fieldValue);
                            // pass in the ao model alternative number to this
                            // method
                            hh.setHhAutos(ao + 1);
                            break;
                        }

                    } else
                    {
                        col++;
                    }

                }

            }

        } catch (NumberFormatException e)
        {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public long getBytesUsedByHouseholdArray()
    {

        long numBytes = 0;
        for (int i = 0; i < hhs.length; i++)
        {
            Household hh = hhs[i];
            long size = ObjectUtil.sizeOf(hh);
            numBytes += size;
        }

        return numBytes;
    }

    /**
     * Assigns each individual person their own value of time, drawing from a
     * lognormal distribution as a function of income.
     */
    protected void setDistributedValuesOfTime()
    {

        // read in values from property file
        setValueOfTimePropertyFileValues();

        // set up the probability distributions
        for (int i = 0; i < valueOfTimeDistribution.length; i++)
        {
            double mu = Math.log(meanValueOfTime[i] * meanValueOfTimeMultiplierBeforeLogForMu);
            valueOfTimeDistribution[i] = new LognormalDist(mu, valueOfTimeLognormalSigma);
        }

        for (int i = 0; i < hhs.length; ++i)
        {
            Household household = hhs[i];

            // each HH gets a VOT for consistency
            double rnum = household.getHhRandom().nextDouble();
            int incomeCategory = getIncomeIndexForValueOfTime(household.getIncomeInDollars());
            double hhValueOfTime = valueOfTimeDistribution[incomeCategory - 1].inverseF(rnum);

            // constrain to logical min and max values
            if (hhValueOfTime < minValueOfTime) hhValueOfTime = minValueOfTime;
            if (hhValueOfTime > maxValueOfTime) hhValueOfTime = maxValueOfTime;

            // adults get the full value, and children 2/3 (1-based)
            Person[] personArray = household.getPersons();
            for (int j = 1; j < personArray.length; ++j)
            {
                Person person = personArray[j];

                int age = person.getAge();
                if (age < 18) person.setValueOfTime((float) hhValueOfTime
                        * hhValueOfTimeMultiplierForPersonUnder18);
                else person.setValueOfTime((float) hhValueOfTime);
            }
        }
    }

    /**
     * Sets additional properties specific to MTC, included distributed
     * value-of-time information
     */
    private void setValueOfTimePropertyFileValues()
    {

        boolean errorFlag = false;
        String propertyValue = "";

        propertyValue = propertyMap.get(PROPERTIES_MIN_VALUE_OF_TIME_KEY);
        if (propertyValue == null)
        {
            logger.error("property file key missing: " + PROPERTIES_MIN_VALUE_OF_TIME_KEY
                    + ", not able to set min value of time value.");
            errorFlag = true;
        } else minValueOfTime = Float.parseFloat(propertyValue);

        propertyValue = propertyMap.get(PROPERTIES_MAX_VALUE_OF_TIME_KEY);
        if (propertyValue == null)
        {
            logger.error("property file key missing: " + PROPERTIES_MAX_VALUE_OF_TIME_KEY
                    + ", not able to set max value of time value.");
            errorFlag = true;
        } else maxValueOfTime = Float.parseFloat(propertyValue);

        // mean values of time by income category are specified as a
        // "comma-sparated" list of float values
        // the number of mean values in the lsit determines the number of income
        // categories for value of time
        // the number of upper limit income dollar values is expected to be
        // number of mean values - 1.
        int numIncomeCategories = -1;
        String meanValueOfTimesPropertyValue = propertyMap
                .get(PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY);
        if (meanValueOfTimesPropertyValue == null)
        {
            logger.error("property file key missing: " + PROPERTIES_MEAN_VALUE_OF_TIME_VALUES_KEY
                    + ", not able to set mean value of time values.");
            errorFlag = true;
        } else
        {

            ArrayList<Float> valueList = new ArrayList<Float>();
            StringTokenizer valueTokenizer = new StringTokenizer(meanValueOfTimesPropertyValue, ",");
            while (valueTokenizer.hasMoreTokens())
            {
                String listValue = valueTokenizer.nextToken();
                float value = Float.parseFloat(listValue.trim());
                valueList.add(value);
            }

            numIncomeCategories = valueList.size();
            meanValueOfTime = new float[numIncomeCategories];
            valueOfTimeDistribution = new LognormalDist[numIncomeCategories];

            for (int i = 0; i < numIncomeCategories; i++)
                meanValueOfTime[i] = valueList.get(i);
        }

        // read the upper limit values for value of time income ranges.
        // there should be exactly 1 less than the number of mean value of time
        // values - any other value is an error.
        String valueOfTimeIncomesPropertyValue = propertyMap
                .get(PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY);
        if (valueOfTimeIncomesPropertyValue == null)
        {
            logger.error("property file key missing: "
                    + PROPERTIES_MEAN_VALUE_OF_TIME_INCOME_LIMITS_KEY
                    + ", not able to set upper limits for value of time income ranges.");
            errorFlag = true;
        } else
        {

            ArrayList<Integer> valueList = new ArrayList<Integer>();
            StringTokenizer valueTokenizer = new StringTokenizer(valueOfTimeIncomesPropertyValue,
                    ",");
            while (valueTokenizer.hasMoreTokens())
            {
                String listValue = valueTokenizer.nextToken();
                int value = Integer.parseInt(listValue.trim());
                valueList.add(value);
            }

            int numIncomeValues = valueList.size();
            if (numIncomeValues != (numIncomeCategories - 1))
            {
                Exception e = new RuntimeException();
                logger.error("an error occurred reading properties file values for distributed value of time calculations.");
                logger.error("the mean value of time values property="
                        + meanValueOfTimesPropertyValue + " specifies " + numIncomeCategories
                        + " mean values, thus " + numIncomeCategories + " income ranges.");
                logger.error("the value of time income range values property="
                        + valueOfTimeIncomesPropertyValue + " specifies " + numIncomeValues
                        + " income range limit values.");
                logger.error("there should be exactly " + (numIncomeCategories - 1)
                        + " income range limit values for " + numIncomeCategories
                        + " mean value of time values.", e);
                System.exit(-1);
            }

            // set the income dollar value upper limits for value of time income
            // ranges
            incomeDollarLimitsForValueOfTime = new int[numIncomeValues + 1];
            for (int i = 0; i < numIncomeValues; i++)
                incomeDollarLimitsForValueOfTime[i] = valueList.get(i);

            incomeDollarLimitsForValueOfTime[numIncomeValues] = Integer.MAX_VALUE;
        }

        propertyValue = propertyMap.get(PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY);
        if (propertyValue == null)
        {
            logger.error("property file key missing: "
                    + PROPERTIES_HH_VALUE_OF_TIME_MULTIPLIER_FOR_UNDER_18_KEY
                    + ", not able to set hh value of time multiplier for kids in hh under age 18.");
            errorFlag = true;
        } else hhValueOfTimeMultiplierForPersonUnder18 = Float.parseFloat(propertyValue);

        propertyValue = propertyMap.get(PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY);
        if (propertyValue == null)
        {
            logger.error("property file key missing: "
                    + PROPERTIES_MEAN_VALUE_OF_TIME_MULTIPLIER_FOR_MU_KEY
                    + ", not able to set lognormal distribution mu parameter multiplier.");
            errorFlag = true;
        } else meanValueOfTimeMultiplierBeforeLogForMu = Float.parseFloat(propertyValue);

        propertyValue = propertyMap.get(PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY);
        if (propertyValue == null)
        {
            logger.error("property file key missing: "
                    + PROPERTIES_VALUE_OF_TIME_LOGNORMAL_SIGMA_KEY
                    + ", not able to set lognormal distribution sigma parameter.");
            errorFlag = true;
        } else valueOfTimeLognormalSigma = Float.parseFloat(propertyValue);

        if (errorFlag)
        {
            Exception e = new RuntimeException();
            logger.error(
                    "errors occurred reading properties file values for distributed value of time calculations.",
                    e);
            System.exit(-1);
        }

    }

    private int getIncomeIndexForValueOfTime(int incomeInDollars)
    {
        int returnValue = -1;
        for (int i = 0; i < incomeDollarLimitsForValueOfTime.length; i++)
        {
            if (incomeInDollars < incomeDollarLimitsForValueOfTime[i])
            {
                // return a 1s based index value
                returnValue = i + 1;
                break;
            }
        }

        return returnValue;
    }

}