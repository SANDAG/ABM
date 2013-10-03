package org.sandag.abm.ctramp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class DestinationSampleOfAlternativesModel
        implements Serializable
{

    private transient Logger         logger                           = Logger.getLogger(DestinationSampleOfAlternativesModel.class);
    private transient Logger         dcSoaLogger                      = Logger.getLogger("tourDcSoa");

    // set to false to store probabilities in cache for re-use; true to disable
    // probabilities cache.
    private static final boolean     ALWAYS_COMPUTE_PROBABILITIES     = false;
    private static final boolean     ALLOW_DEBUG                      = true;

    private static final int         DC_SOA_DATA_SHEET                = 0;
    private String                   dcSoaUecFileName;
    private int                      sampleSize;

    private MgraDataManager          mgraManager;

    private int                      currentOrigMgra;
    private double[][]               probabilitiesCache;
    private double[][]               cumProbabilitiesCache;
    private int                      currentWorkMgra;
    private double[][]               subtourProbabilitiesCache;
    private double[][]               subtourCumProbabilitiesCache;

    // destsSample[] and destsAvailable[] are indexed by purpose and alternative
    private boolean[]                escortAvailable;
    private int[]                    escortSample;
    private boolean[][]              destsAvailable;
    private int[][]                  destsSample;

    private int[]                    sample;
    private float[]                  corrections;

    private int[]                    dcSoaModelIndices;
    private ChoiceModelApplication[] choiceModel;

    private int                      numberOfSoaChoiceAlternatives;
    private int[]                    numberOfSoaChoiceAlternativesAvailable;

    private int                      soaProbabilitiesCalculationCount = 0;
    private long                     soaRunTime                       = 0;

    public DestinationSampleOfAlternativesModel(String soaUecFile, int sampleSize,
            HashMap<String, String> propertyMap, MgraDataManager mgraManager,
            double[][] dcSizeArray, DcSoaDMU dcSoaDmuObject, int[] soaUecIndices)
    {

        this.sampleSize = sampleSize;
        this.dcSoaUecFileName = soaUecFile;
        this.mgraManager = mgraManager;

        // create an array of sample of alternative ChoiceModelApplication
        // objects
        // for each purpose
        setupSampleOfAlternativesChoiceModelArrays(propertyMap, dcSizeArray, dcSoaDmuObject,
                soaUecIndices);

    }

    private void setupSampleOfAlternativesChoiceModelArrays(HashMap<String, String> propertyMap,
            double[][] dcSizeArray, DcSoaDMU dcSoaDmuObject, int[] soaUecIndices)
    {

        // create a HashMap to map purpose index to model index
        dcSoaModelIndices = new int[soaUecIndices.length];

        // get a set of unique model sheet numbers so that we can create
        // ChoiceModelApplication objects once for each model sheet used
        // also create a HashMap to relate size segment index to SOA Model
        // objects
        HashMap<Integer, Integer> modelIndexMap = new HashMap<Integer, Integer>();
        int soaModelIndex = 0;
        int sizeSegmentIndex = 0;
        for (int uecIndex : soaUecIndices)
        {
            // if the uec sheet for the size segment is not in the map, add it,
            // otherwise, get it from the map
            if (!modelIndexMap.containsKey(uecIndex))
            {
                modelIndexMap.put(uecIndex, soaModelIndex);
                dcSoaModelIndices[sizeSegmentIndex] = soaModelIndex++;
            } else
            {
                dcSoaModelIndices[sizeSegmentIndex] = modelIndexMap.get(uecIndex);
            }

            sizeSegmentIndex++;
        }
        // the value of soaModelIndex is the number of ChoiceModelApplication
        // objects to create
        // the modelIndexMap keys are the uec sheets to use in building
        // ChoiceModelApplication objects

        choiceModel = new ChoiceModelApplication[modelIndexMap.size()];
        probabilitiesCache = new double[sizeSegmentIndex][];
        cumProbabilitiesCache = new double[sizeSegmentIndex][];
        subtourProbabilitiesCache = new double[sizeSegmentIndex][];
        subtourCumProbabilitiesCache = new double[sizeSegmentIndex][];

        int i = 0;
        for (int uecIndex : modelIndexMap.keySet())
        {
            int modelIndex = -1;
            try
            {
                modelIndex = modelIndexMap.get(uecIndex);
                choiceModel[modelIndex] = new ChoiceModelApplication(dcSoaUecFileName, uecIndex,
                        DC_SOA_DATA_SHEET, propertyMap, (VariableTable) dcSoaDmuObject);
                i++;
            } catch (RuntimeException e)
            {
                logger.error(String
                        .format("exception caught setting up DC SOA ChoiceModelApplication[%d] for modelIndex=%d of %d models",
                                i, modelIndex, modelIndexMap.size()));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

        }

        setAvailabilityForSampleOfAlternatives(dcSizeArray);

    }

    /**
     * This method is called initially when the SOA choice models array is
     * created. It would be called subsequently if a shadow pricing methodology
     * is applied to reset the scaled size terms and corresponding
     * availabilities and sample arrays.
     */
    public void setAvailabilityForSampleOfAlternatives(double[][] dcSizeArray)
    {

        int maxMgra = mgraManager.getMaxMgra();

        // declare dimensions for the alternative availability array by purpose
        // and
        // number of alternaives
        escortAvailable = new boolean[maxMgra + 1];
        escortSample = new int[maxMgra + 1];
        destsAvailable = new boolean[dcSizeArray.length][maxMgra + 1];
        destsSample = new int[dcSizeArray.length][maxMgra + 1];

        numberOfSoaChoiceAlternativesAvailable = new int[dcSizeArray.length];

        for (int i = 0; i < dcSizeArray.length; i++)
        {
            for (int k = 1; k <= maxMgra; k++)
            {
                if (dcSizeArray[i][k] > 0.0)
                {
                    destsAvailable[i][k] = true;
                    destsSample[i][k] = 1;
                    numberOfSoaChoiceAlternativesAvailable[i]++;
                }
            } // k
        }

        Arrays.fill(escortAvailable, true);
        Arrays.fill(escortSample, 1);

        numberOfSoaChoiceAlternatives = maxMgra;
    }

    public int getNumberOfAlternatives()
    {
        return numberOfSoaChoiceAlternatives;
    }

    public void computeDestinationSampleOfAlternatives(DcSoaDMU dcSoaDmuObject, Tour tour,
            Person person, String segmentName, int segmentIndex, int origMgra)
    {

        long timeCheck = System.nanoTime();

        // these will be dimensioned with the number of unique alternatives
        // determined for the decision makers
        int[] altList;
        int[] altListFreq;
        HashMap<Integer, Integer> altFreqMap = new HashMap<Integer, Integer>();

        int modelIndex = dcSoaModelIndices[segmentIndex];

        // if the flag is set to compute sample of alternative probabilities for
        // every work/school location choice,
        // or the tour's origin taz is different from the currentOrigTaz, reset
        // the currentOrigTaz and clear the stored probabilities.
        if (tour != null && tour.getTourCategory() == ModelStructure.AT_WORK_CATEGORY)
        {

            if (ALWAYS_COMPUTE_PROBABILITIES || origMgra != currentWorkMgra)
            {

                // clear the probabilities stored for the current origin mgra,
                // for each DC segment
                for (int i = 0; i < subtourProbabilitiesCache.length; i++)
                {
                    subtourProbabilitiesCache[i] = null;
                    subtourCumProbabilitiesCache[i] = null;
                }
                currentWorkMgra = origMgra;

            }

            // If the sample of alternatives choice probabilities have not been
            // computed for the current origin mgra
            // and segment specified, compute them.
            if (subtourProbabilitiesCache[segmentIndex] == null)
            {
                computeSampleOfAlternativesChoiceProbabilities(dcSoaDmuObject, tour, person,
                        segmentName, segmentIndex, origMgra);
                soaProbabilitiesCalculationCount++;
            }

        } else if (tour != null
                && tour.getTourPrimaryPurpose().equalsIgnoreCase(
                        ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME))
        {

            // always update probabilities for Escort tours
            // clear the probabilities stored for the current origin mgra, for
            // the Escort DC segment
            probabilitiesCache[segmentIndex] = null;
            cumProbabilitiesCache[segmentIndex] = null;

            destsAvailable[segmentIndex] = escortAvailable;
            destsSample[segmentIndex] = escortSample;

            currentOrigMgra = origMgra;

            computeSampleOfAlternativesChoiceProbabilities(dcSoaDmuObject, tour, person,
                    segmentName, segmentIndex, origMgra);
            soaProbabilitiesCalculationCount++;

        } else
        {

            if (ALWAYS_COMPUTE_PROBABILITIES || origMgra != currentOrigMgra)
            {

                // clear the probabilities stored for the current origin mgra,
                // for each DC segment
                for (int i = 0; i < probabilitiesCache.length; i++)
                {
                    probabilitiesCache[i] = null;
                    cumProbabilitiesCache[i] = null;
                }
                currentOrigMgra = origMgra;

            }

            // If the sample of alternatives choice probabilities have not been
            // computed for the current origin taz
            // and purpose specified, compute them.
            if (probabilitiesCache[segmentIndex] == null)
            {
                computeSampleOfAlternativesChoiceProbabilities(dcSoaDmuObject, tour, person,
                        segmentName, segmentIndex, origMgra);
                soaProbabilitiesCalculationCount++;
            }

        }

        Household hhObj = person.getHouseholdObject();
        Random hhRandom = hhObj.getHhRandom();
        int rnCount = hhObj.getHhRandomCount();
        // when household.getHhRandom() was applied, the random count was
        // incremented, assuming a random number would be drawn right away.
        // so let's decrement by 1, then increment the count each time a random
        // number is actually drawn in this method.
        rnCount--;

        // select sampleSize alternatives based on probabilitiesList[origTaz],
        // and
        // count frequency of alternatives chosen.
        // final sample may include duplicate alternative selections.
        for (int i = 0; i < sampleSize; i++)
        {

            double rn = hhRandom.nextDouble();
            rnCount++;

            int chosenAlt = -1;
            if (tour != null && tour.getTourCategory() == ModelStructure.AT_WORK_CATEGORY) chosenAlt = Util
                    .binarySearchDouble(subtourCumProbabilitiesCache[segmentIndex], rn) + 1;
            else chosenAlt = Util.binarySearchDouble(cumProbabilitiesCache[segmentIndex], rn) + 1;

            // write choice model alternative info to log file
            if (hhObj.getDebugChoiceModels())
            {
                choiceModel[modelIndex]
                        .logSelectionInfo(
                                String.format(
                                        "Sample Of Alternatives Choice for segmentName=%s, segmentIndex=%d, modelIndex=%d, origMgra=%d",
                                        segmentName, segmentIndex, modelIndex, origMgra), String
                                        .format("HHID=%d, rn=%.8f, rnCount=%d", hhObj.getHhId(),
                                                rn, (rnCount + i)), rn, chosenAlt);
            }

            int freq = 0;
            if (altFreqMap.containsKey(chosenAlt)) freq = altFreqMap.get(chosenAlt);
            altFreqMap.put(chosenAlt, (freq + 1));

        }

        // sampleSize random number draws were made from the Random object for
        // the
        // current household,
        // so update the count in the hh's Random.
        hhObj.setHhRandomCount(rnCount);

        // create arrays of the unique chosen alternatives and the frequency
        // with
        // which those alternatives were chosen.
        int numUniqueAlts = altFreqMap.keySet().size();
        altList = new int[numUniqueAlts];
        altListFreq = new int[numUniqueAlts];
        Iterator<Integer> it = altFreqMap.keySet().iterator();
        int k = 0;
        while (it.hasNext())
        {
            int key = (Integer) it.next();
            int value = (Integer) altFreqMap.get(key);
            altList[k] = key;
            altListFreq[k] = value;
            k++;
        }

        // loop through these arrays, construct final sample[] and
        // corrections[].
        sample = new int[numUniqueAlts + 1];
        corrections = new float[numUniqueAlts + 1];
        for (k = 0; k < numUniqueAlts; k++)
        {
            int alt = altList[k];
            int freq = altListFreq[k];

            double prob = 0;
            if (tour != null && tour.getTourCategory() == ModelStructure.AT_WORK_CATEGORY) prob = subtourProbabilitiesCache[segmentIndex][alt - 1];
            else prob = probabilitiesCache[segmentIndex][alt - 1];

            sample[k + 1] = alt;
            corrections[k + 1] = (float) Math.log((double) freq / prob);
        }

        soaRunTime += (System.nanoTime() - timeCheck);

    }

    /**
     * This method is used if the tour/person decision maker is the first one
     * encountered for the purpose and origin taz. Once the sample of
     * alternatives choice probabilities for a purpose and origin taz are
     * computed, they are stored in an array and used by other decision makers
     * with the same purpose and origin taz.
     * 
     * @param probabilitiesList
     *            is the probabilities array for the given purpose in which
     *            choice probabilities will be saved for the origin of the
     *            tour/place to be selected.
     * @param cumProbabilitiesList
     *            is the probabilities array for the given purpose in which
     *            choice cumulative probabilities will be saved for the origin
     *            of the tour/place to be selected.
     * @param choiceModel
     *            the ChoiceModelApplication object for the purpose
     * @param tour
     *            the tour object for whic destination choice is required, or
     *            null if a usual work/school location is being chosen
     * @param person
     *            the person object for whom the choice is being made
     * @param segmentName
     *            the name of the segment the choice is being made for - for
     *            logging
     * @param segmentindex
     *            the index associated with the segment
     * @param origMgra
     *            the index associated with the segment
     * @param distances
     *            array frpm origin MGRA to all MGRAs
     */
    private void computeSampleOfAlternativesChoiceProbabilities(DcSoaDMU dcSoaDmuObject, Tour tour,
            Person person, String segmentName, int segmentIndex, int origMgra)
    {

        Household hhObj = person.getHouseholdObject();

        // set the hh, person, and tour objects for this DMU object
        dcSoaDmuObject.setHouseholdObject(hhObj);
        dcSoaDmuObject.setPersonObject(person);
        dcSoaDmuObject.setTourObject(tour);

        // set sample of alternatives choice DMU attributes
        dcSoaDmuObject.setDmuIndexValues(hhObj.getHhId(), hhObj.getHhMgra(), origMgra, 0);

        // prepare a trace log header that the choiceModel object will write
        // prior to
        // UEC trace logging
        String choiceModelDescription = "";
        String decisionMakerLabel = "";

        int choiceModelIndex = dcSoaModelIndices[segmentIndex];

        // If the person making the choice is from a household requesting trace
        // information,
        // create a trace logger header and write prior to the choiceModel
        // computing
        // utilities
        if (hhObj.getDebugChoiceModels())
        {

            if (tour == null)
            {
                // null tour means the SOA choice is for a mandatory usual
                // location choice
                choiceModelDescription = String.format(
                        "Usual Location Sample of Alternatives Choice Model for: Segment=%s",
                        segmentName);
                decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", person
                        .getHouseholdObject().getHhId(), person.getPersonNum(), person
                        .getPersonType());
            } else
            {
                choiceModelDescription = String.format(
                        "Destination Choice Model for: Segment=%s, TourId=%d", segmentName,
                        tour.getTourId());
                decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", person
                        .getHouseholdObject().getHhId(), person.getPersonNum(), person
                        .getPersonType());
            }

            // log headers to traceLogger if the person making the choice is
            // from a
            // household requesting trace information
            choiceModel[choiceModelIndex].choiceModelUtilityTraceLoggerHeading(
                    choiceModelDescription, decisionMakerLabel);

        }

        try
        {
            choiceModel[choiceModelIndex].computeUtilities(dcSoaDmuObject,
                    dcSoaDmuObject.getDmuIndexValues(), destsAvailable[segmentIndex],
                    destsSample[segmentIndex]);
        } catch (Exception e)
        {
            logger.error("exception caught in DC SOA model for:");
            choiceModelDescription = String.format(
                    "Destination Choice Model for: Segment=%s, TourId=%d", segmentName,
                    tour.getTourId());
            decisionMakerLabel = String.format("HH=%d, PersonNum=%d, PersonType=%s", person
                    .getHouseholdObject().getHhId(), person.getPersonNum(), person.getPersonType());
            logger.error("choiceModelDescription:" + choiceModelDescription);
            logger.error("decisionMakerLabel:" + decisionMakerLabel);
            throw new RuntimeException(e);
        }

        // TODO: debug
        if (choiceModel[choiceModelIndex].getAvailabilityCount() == 0)
        {

            int j = 0;
            int[] debugAlts = new int[5];
            for (int i = 0; i < destsSample[segmentIndex].length; i++)
            {
                if (destsSample[segmentIndex][i] == 1)
                {
                    debugAlts[j++] = i;
                    if (j == 5) break;
                }
            }

            choiceModel[choiceModelIndex].logUECResultsSpecificAlts(dcSoaLogger, "debugging",
                    debugAlts);
        }

        // the following order of assignment is important in mult-threaded
        // context.
        // probabilitiesCache[][] is a trigger variable - if it is not null for
        // any thread, the cumProbabilitiesCache[][] values
        // are used immediately, so the cumProbabilitiesCache values must be
        // assigned before the probabilitiesCache
        // are assigned, which indicates cumProbabilitiesCache[][] values are
        // ready to be used.
        if (tour != null && tour.getTourCategory() == ModelStructure.AT_WORK_CATEGORY)
        {

            subtourCumProbabilitiesCache[segmentIndex] = Arrays.copyOf(
                    choiceModel[choiceModelIndex].getCumulativeProbabilities(),
                    choiceModel[choiceModelIndex].getNumberOfAlternatives());
            subtourProbabilitiesCache[segmentIndex] = Arrays.copyOf(
                    choiceModel[choiceModelIndex].getProbabilities(),
                    choiceModel[choiceModelIndex].getNumberOfAlternatives());
        } else
        {

            cumProbabilitiesCache[segmentIndex] = Arrays.copyOf(
                    choiceModel[choiceModelIndex].getCumulativeProbabilities(),
                    choiceModel[choiceModelIndex].getNumberOfAlternatives());
            probabilitiesCache[segmentIndex] = Arrays.copyOf(
                    choiceModel[choiceModelIndex].getProbabilities(),
                    choiceModel[choiceModelIndex].getNumberOfAlternatives());

            if (hhObj.getDebugChoiceModels())
            {
                PrintWriter out = null;
                try
                {
                    out = new PrintWriter(new BufferedWriter(new FileWriter(
                            new File("soaProbs.csv"))));

                    out.println("choiceModelDescription:" + choiceModelDescription);
                    out.println("decisionMakerLabel:" + decisionMakerLabel);

                    for (int i = 0; i < probabilitiesCache[segmentIndex].length; i++)
                    {
                        out.println((i + 1) + "," + probabilitiesCache[segmentIndex][i]);
                    }
                } catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                out.close();

            }

        }

        // If the person making the choice is from a household requesting trace
        // information,
        // write choice model alternative info to the debug log file
        if (hhObj.getDebugChoiceModels() && ALLOW_DEBUG)
        {
            // if ( dcSoaLogger.isDebugEnabled() ){
            int[] altsToLog = {0, 77, 78, 79, 80};
            choiceModel[choiceModelIndex].logAlternativesInfo(String.format(
                    "%s Sample Of Alternatives Choice for origTaz=%d", segmentName, origMgra),
                    String.format("HHID=%d", hhObj.getHhId()), dcSoaLogger);
            choiceModel[choiceModelIndex].logUECResultsSpecificAlts(dcSoaLogger,
                    choiceModelDescription + ", " + decisionMakerLabel, altsToLog);

            double[] probs = choiceModel[choiceModelIndex].getProbabilities();
            double[] utils = choiceModel[choiceModelIndex].getUtilities();
            double total = 0;
            for (int i = 0; i < probs.length; i++)
                total += Math.exp(utils[i]);

            dcSoaLogger.info("");
            for (int i = 1; i < altsToLog.length; i++)
                dcSoaLogger.info("alt=" + (altsToLog[i] + 1) + ", util=" + utils[altsToLog[i]]
                        + ", prob=" + probs[altsToLog[i]]);

            dcSoaLogger.info("total exponentiated utility = " + total);
            dcSoaLogger.info("");
            dcSoaLogger.info("");
            // }
        }

    }

    public int getSoaProbabilitiesCalculationCount()
    {
        return soaProbabilitiesCalculationCount;
    }

    public long getSoaRunTime()
    {
        return soaRunTime;
    }

    public void resetSoaRunTime()
    {
        soaRunTime = 0;
    }

    public int getCurrentOrigMgra()
    {
        return currentOrigMgra;
    }

    public int[] getSampleOfAlternatives()
    {
        return sample;
    }

    public float[] getSampleOfAlternativesCorrections()
    {
        return corrections;
    }

}