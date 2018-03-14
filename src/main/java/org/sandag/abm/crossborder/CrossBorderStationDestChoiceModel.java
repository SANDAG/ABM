package org.sandag.abm.crossborder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.Tracer;

/**
 * This class is used for both the sample of alternatives and the full
 * destination choice model for border crossing tours.
 * 
 * The model first calculates a set of station-level logsums which represent the
 * attractiveness of each station based upon the accessibility to Mexico
 * populations (taking into account population size of Colonias and distance
 * between each station and each Colonia. The model then creates a sample of
 * alternatives where each alternative is a pair of border crossing station
 * (entry MGRA) and destination MGRA in San Diego County, by tour purpose. This
 * is sampled from for each tour, and a mode choice logsum is calculated for
 * each station-MGRA pair. The full destination choice model is run on the
 * sample with the mode choice logsums influencing station - destination choice,
 * and a station-MGRA pair is chosen for each tour.
 * 
 * @author Freedman
 * 
 */
public class CrossBorderStationDestChoiceModel
{

    private double[][]   mgraSizeTerms;           // by purpose, MGRA
    private double[][]   tazSizeTerms;            // by purpose, TAZ
    private double[][]   tazStationProbabilities; // by purpose, station-TAZ
                                                   // alternative
    private double[][][] mgraProbabilities;       // by purpose, TAZ, MGRA
    private double[]     stationLogsums;          // by entry station, logsum
                                                   // from colonia to
                                                   // station
    private double[]     soaStationLogsums;       // by station-TAZ
                                                   // alternative, station
                                                   // logsums
    private double[][]   soaSizeTerms;            // by purpose, station-TAZ
                                                   // alternative,
                                                   // size terms for tazs
    private int[]        soaOriginTazs;           // by station-TAZ
                                                   // alternative, origin Taz
    private int[]        soaDestinationTazs;      // by station-TAZ
                                                   // alternative, destination
                                                   // Taz

    private int[]        sampledDestinationMgra;  // destination mgra for each
                                                   // of n
                                                   // samples
    private int[]        sampledEntryMgra;        // entry mgra for each of n
                                                   // samples
    private double[][]   sampledSizeTerms;        // size term for each of n
                                                   // samples (1st
                                                   // dimension is purpose)
    private double[]     sampledStationLogsums;   // station logsum for each of
                                                   // n
                                                   // samples
    private int[]        sampledStations;         // POE for each of n samples
    private int[]        sampledOriginTazs;       // Origin Taz for each of n
                                                   // samples
    private int[]        sampledDestinationTazs;  // Destination Taz for each
                                                   // of n
                                                   // samples

    private double[]     sampledCorrectionFactors; // correction factor for each
                                                   // of
                                                   // n samples
    private double[]     tourModeChoiceLogsums;   // mode choice logsum for
                                                   // each of n
                                                   // samples

    private class KeyClass
    {
        int station;
        int mgra;

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof KeyClass)
            {
                return station == (((KeyClass) obj).station) && mgra == (((KeyClass) obj).mgra);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return station * 10000 + mgra;
        }

    }

    private KeyClass                       key;
    private HashMap<KeyClass, Integer>     frequencyChosen;                              // by
                                                                                          // alternative,
                                                                                          // number
                                                                                          // of
                                                                                          // times
                                                                                          // chosen

    private TableDataSet                   alternativeData;                              // the
                                                                                          // alternatives,
                                                                                          // with
                                                                                          // the
                                                                                          // following
                                                                                          // fields:
                                                                                          // "EntryMGRA"
                                                                                          // -
                                                                                          // indicating
                                                                                          // border
                                                                                          // crossing
                                                                                          // entry
                                                                                          // MGRA
                                                                                          // "dest"
                                                                                          // -
                                                                                          // indicating
                                                                                          // the
                                                                                          // destination
                                                                                          // TAZ
                                                                                          // in
                                                                                          // San
                                                                                          // Diego
                                                                                          // County

    private int                            stations;                                     // number
                                                                                          // of
                                                                                          // stations
    private int                            sampleRate;

    private transient Logger               logger = Logger.getLogger("crossBorderModel");

    private TazDataManager                 tazManager;
    private MgraDataManager                mgraManager;

    private ChoiceModelApplication         soaModel;
    private ChoiceModelApplication         destModel;
    private CrossBorderTourModeChoiceModel tourModeChoiceModel;
    CrossBorderStationDestChoiceDMU        dmu;
    McLogsumsCalculator                    logsumHelper;

    private UtilityExpressionCalculator    sizeTermUEC;
    private Tracer                         tracer;
    private boolean                        trace;
    private int[]                          traceOtaz;
    private int[]                          traceDtaz;
    private boolean                        seek;
    private HashMap<String, String>        rbMap;

    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of cross border model DMUs
     */
    public CrossBorderStationDestChoiceModel(HashMap<String, String> rbMap,
            CrossBorderModelStructure myStructure, CrossBorderDmuFactoryIf dmuFactory,
            AutoTazSkimsCalculator tazDistanceCalculator)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String crossBorderDCSoaFileName = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.soa.uec.file");
        crossBorderDCSoaFileName = uecFileDirectory + crossBorderDCSoaFileName;

        int soaDataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.soa.data.page"));
        int soaSizePage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.soa.size.page"));
        int soaModelPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.soa.model.page"));

        String crossBorderDCFileName = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.uec.file");
        crossBorderDCFileName = uecFileDirectory + crossBorderDCFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.data.page"));
        int modelPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.model.page"));

        // read the model pages from the property file, create one choice model
        // for each
        CrossBorderStationDestChoiceDMU dcDmu = dmuFactory.getCrossBorderStationChoiceDMU();

        // create a ChoiceModelApplication object for the SOA model.
        soaModel = new ChoiceModelApplication(crossBorderDCSoaFileName, soaModelPage, soaDataPage,
                rbMap, (VariableTable) dcDmu);

        // create a ChoiceModelApplication object for the full model.
        destModel = new ChoiceModelApplication(crossBorderDCFileName, modelPage, dataPage, rbMap,
                (VariableTable) dcDmu);
        sampleRate = destModel.getAlternativeNames().length;

        // get the alternative data from the model
        UtilityExpressionCalculator uec = soaModel.getUEC();
        alternativeData = uec.getAlternativeData();

        // create a UEC to solve size terms for each MGRA
        sizeTermUEC = new UtilityExpressionCalculator(new File(crossBorderDCSoaFileName),
                soaSizePage, soaDataPage, rbMap, dmuFactory.getCrossBorderStationChoiceDMU());
        // set up the tracer object
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if (trace)
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String coloniaDistanceFile = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.colonia.file");
        coloniaDistanceFile = directory + coloniaDistanceFile;

        // calculate logsums for each station (based on Super-Colonia population
        // and distance to station)
        float distanceParam = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.dc.colonia.distance.parameter"));
        calculateStationLogsum(coloniaDistanceFile, distanceParam);

        // arrays of sampled station-mgra pairs
        sampledDestinationMgra = new int[sampleRate + 1];
        sampledEntryMgra = new int[sampleRate + 1];
        sampledCorrectionFactors = new double[sampleRate + 1];
        frequencyChosen = new HashMap<KeyClass, Integer>();

        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(rbMap);

        // this sets by thread, so do it outside of initialization
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());

        // set up a tour mode choice model for calculation of tour mode
        // probabilities
        tourModeChoiceModel = new CrossBorderTourModeChoiceModel(rbMap, myStructure, dmuFactory,
        		tazDistanceCalculator);

        tourModeChoiceLogsums = new double[sampleRate + 1];
        sampledSizeTerms = new double[myStructure.CROSSBORDER_PURPOSES.length][sampleRate + 1];
        sampledStationLogsums = new double[sampleRate + 1];
        sampledStations = new int[sampleRate + 1];

        sampledOriginTazs = new int[sampleRate + 1];
        sampledDestinationTazs = new int[sampleRate + 1];
        

    }

    /**
     * Calculate the station logsum. Station logsums are based on distance from
     * supercolonia to station and population of supercolonia, as follows:
     * 
     * stationLogsum_i = LN [ Sum( exp(distanceParam * distance) * population) ]
     * 
     * supercolonia population and distances are stored in @param fileName.
     * Fields in file include:
     * 
     * Population Population of supercolonia Distance_MGRANumber where
     * MGRANumber is the number of the MGRA corresponding to the entry station,
     * with one field for each possible entry station.
     * 
     * @param fileName
     *            Name of file containing supercolonia population and distance
     * @param distanceParameter
     *            Parameter for distance.
     */
    private void calculateStationLogsum(String fileName, float distanceParameter)
    {

        logger.info("Calculating Station Logsum");

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet coloniaTable;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            coloniaTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);

        stations = 0;
        // iterate through columns in table, calculate number of station
        // alternatives (entry stations)
        String[] columnLabels = coloniaTable.getColumnLabels();
        for (int i = 0; i < columnLabels.length; ++i)
        {
            String label = columnLabels[i];
            if (label.contains("Distance_"))
            {
                ++stations;
            }
        }

        // iterate through stations and calculate logsum
        stationLogsums = new double[stations];
        int colonias = coloniaTable.getRowCount();
        for (int i = 0; i < colonias; ++i)
        {

            float population = coloniaTable.getValueAt(i + 1, "Population");

            for (int j = 0; j < stations; ++j)
            {

                float distance = coloniaTable.getValueAt(i + 1, "Distance_poe" + j);
                if (population > 0)
                    stationLogsums[j] += Math.exp(distanceParameter * distance) * population;
            }
        }

        // take natural log
        for (int i = 0; i < stations; ++i)
            stationLogsums[i] = Math.log(stationLogsums[i]);

        logger.info("Finished Calculating Border Crossing Station Logsum");

    }

    /**
     * Calculate size terms
     */
    public void calculateSizeTerms(CrossBorderDmuFactoryIf dmuFactory)
    {

        logger.info("Calculating Cross Border Model MGRA Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();
        int maxTaz = tazManager.getMaxTaz();
        int purposes = sizeTermUEC.getNumberOfAlternatives();

        mgraSizeTerms = new double[purposes][maxMgra + 1];
        tazSizeTerms = new double[purposes][maxTaz + 1];
        IndexValues iv = new IndexValues();
        CrossBorderStationDestChoiceDMU aDmu = dmuFactory.getCrossBorderStationChoiceDMU();

        // loop through mgras and calculate size terms
        for (int mgra : mgras)
        {

            int taz = mgraTaz[mgra];
            iv.setZoneIndex(mgra);
            double[] utilities = sizeTermUEC.solve(iv, aDmu, null);

            // store the size terms
            for (int purpose = 0; purpose < purposes; ++purpose)
            {

                mgraSizeTerms[purpose][mgra] = utilities[purpose];
                tazSizeTerms[purpose][taz] += utilities[purpose];
            }

            // log
            if (tracer.isTraceOn() && tracer.isTraceZone(taz))
            {

                logger.info("Size Term calculations for mgra " + mgra);
                sizeTermUEC.logResultsArray(logger, 0, mgra);

            }
        }

        // now calculate probability of selecting each MGRA within each TAZ for
        // SOA
        mgraProbabilities = new double[purposes][maxTaz + 1][];
        int[] tazs = tazManager.getTazs();

        for (int purpose = 0; purpose < purposes; ++purpose)
        {
            for (int taz = 0; taz < tazs.length; ++taz)
            {
                int tazNumber = tazs[taz];
                int[] mgraArray = tazManager.getMgraArray(tazNumber);

                // initialize the vector of mgras for this purpose-taz
                mgraProbabilities[purpose][tazNumber] = new double[mgraArray.length];

                // now calculate the cumulative probability distribution
                double lastProb = 0.0;
                for (int mgra = 0; mgra < mgraArray.length; ++mgra)
                {

                    int mgraNumber = mgraArray[mgra];
                    if (tazSizeTerms[purpose][tazNumber] > 0.0)
                        mgraProbabilities[purpose][tazNumber][mgra] = lastProb
                                + mgraSizeTerms[purpose][mgraNumber]
                                / tazSizeTerms[purpose][tazNumber];
                    lastProb = mgraProbabilities[purpose][tazNumber][mgra];
                }
                if (tazSizeTerms[purpose][tazNumber] > 0.0 && Math.abs(lastProb - 1.0) > 0.000001)
                    logger.info("Error: purpose " + purpose + " taz " + tazNumber
                            + " cum prob adds up to " + lastProb);
            }

        }

        // calculate logged size terms for mgra and taz vectors to be used in
        // dmu
        for (int purpose = 0; purpose < purposes; ++purpose)
        {
            for (int taz = 0; taz < tazSizeTerms[purpose].length; ++taz)
                if (tazSizeTerms[purpose][taz] > 0.0)
                    tazSizeTerms[purpose][taz] = Math.log(tazSizeTerms[purpose][taz] + 1.0);

            for (int mgra = 0; mgra < mgraSizeTerms[purpose].length; ++mgra)
                if (mgraSizeTerms[purpose][mgra] > 0.0)
                    mgraSizeTerms[purpose][mgra] = Math.log(mgraSizeTerms[purpose][mgra] + 1.0);

        }
        logger.info("Finished Calculating Cross Border Model MGRA Size Terms");
    }

    /**
     * Calculate taz probabilities. This method initializes and calculates the
     * tazProbabilities array.
     */
    public void calculateTazProbabilities(CrossBorderDmuFactoryIf dmuFactory)
    {

        if (tazSizeTerms == null)
        {
            logger.error("Error:  attemping to execute CrossBorderStationDestChoiceModel.calculateTazProbabilities() before calling calculateMgraProbabilities()");
            throw new RuntimeException();
        }

        logger.info("Calculating Cross Border Model TAZ-Station Probabilities Arrays");

        // initialize taz probabilities array
        int purposes = tazSizeTerms.length;

        // initialize the index for station population accessibility and taz
        // size term
        int alternatives = soaModel.getNumberOfAlternatives();
        soaStationLogsums = new double[alternatives + 1]; // by station-TAZ
                                                          // alternative -
                                                          // station logsums
        soaSizeTerms = new double[purposes][alternatives + 1]; // by purpose,
                                                               // station-TAZ
                                                               // alternative -
                                                               // size terms
                                                               // for tazs
        soaOriginTazs = new int[alternatives + 1];
        soaDestinationTazs = new int[alternatives + 1];

        // iterate through the alternatives in the alternatives file and set the
        // size term and station logsum for each alternative
        UtilityExpressionCalculator soaModelUEC = soaModel.getUEC();
        TableDataSet altData = soaModelUEC.getAlternativeData();

        int rowCount = altData.getRowCount();
        for (int row = 1; row <= rowCount; ++row)
        {

            int entryMgra = (int) altData.getValueAt(row, "mgra_entry");
            int poe = (int) altData.getValueAt(row, "poe");
            int destinationTaz = (int) altData.getValueAt(row, "dest");

            soaStationLogsums[row] = stationLogsums[poe];

            for (int purpose = 0; purpose < purposes; ++purpose)
                soaSizeTerms[purpose][row] = tazSizeTerms[purpose][destinationTaz];

            // set the origin taz
            soaOriginTazs[row] = mgraManager.getTaz(entryMgra);

            // set the destination taz
            soaDestinationTazs[row] = destinationTaz;

        }

        dmu = dmuFactory.getCrossBorderStationChoiceDMU();

        // set size terms for each taz
        dmu.setSizeTerms(soaSizeTerms);

        // set population accessibility for each station
        dmu.setStationPopulationAccessibilities(soaStationLogsums);

        // set the stations for each alternative
        int poeField = altData.getColumnPosition("poe");
        int[] poeNumbers = altData.getColumnAsInt(poeField, 1); // return field
                                                                // as 1-based

        dmu.setPoeNumbers(poeNumbers);

        // set origin and destination tazs
        dmu.setOriginTazs(soaOriginTazs);
        dmu.setDestinationTazs(soaDestinationTazs);

        // initialize array to hold taz-station probabilities
        tazStationProbabilities = new double[purposes][alternatives + 1];

        // iterate through purposes, calculate probabilities for each and store
        // in array
        for (int purpose = 0; purpose < purposes; ++purpose)
        {

            dmu.setPurpose(purpose);

            // Calculate utilities & probabilities
            soaModel.computeUtilities(dmu, dmu.getDmuIndexValues());

            // Store probabilities (by purpose)
            tazStationProbabilities[purpose] = Arrays.copyOf(soaModel.getCumulativeProbabilities(),
                    soaModel.getCumulativeProbabilities().length);
        }
        logger.info("Finished Calculating Cross Border Model TAZ-Station Probabilities Arrays");
    }

    /**
     * Choose a Station-MGRA alternative for sampling
     * 
     * @param tour
     *            CrossBorderTour with purpose and Random
     * @return An array of station-mgra pairs
     */
    private void chooseStationMgraSample(CrossBorderTour tour)
    {

        frequencyChosen.clear();

        // choose sample, set station logsums and mgra size terms
        int purpose = tour.getPurpose();
        for (int sample = 1; sample <= sampleRate; ++sample)
        {

            // first find a TAZ and station
            int alt = 0;
            double[] tazCumProb = tazStationProbabilities[purpose];
            double altProb = 0;
            double cumProb = 0;
            double random = tour.getRandom();
            for (int i = 0; i < tazCumProb.length; ++i)
            {
                if (tazCumProb[i] > random)
                {
                    alt = i;
                    if (i != 0)
                    {
                        cumProb = tazCumProb[i - 1];
                        altProb = tazCumProb[i] - tazCumProb[i - 1];
                    } else
                    {
                        altProb = tazCumProb[i];
                    }
                    break;
                }
            }

            // get the taz number of the alternative, and an array of mgras in
            // that taz
            int destinationTaz = (int) alternativeData.getValueAt(alt + 1, "dest");
            int poe = (int) alternativeData.getValueAt(alt + 1, "poe");
            int entryMgra = (int) alternativeData.getValueAt(alt + 1, "mgra_entry");
            sampledEntryMgra[sample] = entryMgra;
            int[] mgraArray = tazManager.getMgraArray(destinationTaz);

            // set the origin taz
            sampledOriginTazs[sample] = (int) alternativeData.getValueAt(alt + 1, "poe_taz");

            // set the destination taz
            sampledDestinationTazs[sample] = destinationTaz;

            // now find an MGRA in the taz corresponding to the random number
            // drawn:
            // note that the indexing needs to be offset by the cumulative
            // probability of the chosen taz and the
            // mgra probabilities need to be scaled by the alternatives
            // probability
            int mgraNumber = 0;
            double[] mgraCumProb = mgraProbabilities[purpose][destinationTaz];
            for (int i = 0; i < mgraCumProb.length; ++i)
            {
                cumProb += mgraCumProb[i] * altProb;
                if (cumProb > random && mgraCumProb[i] > 0)
                {
                    mgraNumber = mgraArray[i];
                    sampledDestinationMgra[sample] = mgraNumber;

                    // for now, store the probability in the correction factors
                    // array
                    sampledCorrectionFactors[sample] = mgraCumProb[i] * altProb;

                    break;
                }
            }

            // store frequency chosen
            key = new KeyClass();
            key.mgra = mgraNumber;
            key.station = entryMgra;
            if (!frequencyChosen.containsKey(key))
            {
                frequencyChosen.put(key, 1);
            } else
            {
                int freq = frequencyChosen.get(key);
                frequencyChosen.put(key, freq + 1);
            }

            // set station logsums
            sampledStationLogsums[sample] = stationLogsums[poe];

            // set the size terms for the sample
            sampledSizeTerms[purpose][sample] = mgraSizeTerms[purpose][mgraNumber];

            // set the sampled station number
            sampledStations[sample] = poe;

        }
        // calculate correction factors
        for (int sample = 1; sample <= sampleRate; ++sample)
        {
            key = new KeyClass();
            key.mgra = sampledDestinationMgra[sample];
            key.station = sampledEntryMgra[sample];
            int freq = frequencyChosen.get(key);
            sampledCorrectionFactors[sample] = (float) Math.log((double) freq
                    / sampledCorrectionFactors[sample]);

        }

    }

    /**
     * Use the tour mode choice model to calculate the logsum for each sampled
     * station-mgra pair and store in the array.
     * 
     * @param tour
     *            The tour attributes used are tour purpose, depart and arrive
     *            periods, and sentri availability.
     */
    private void calculateLogsumsForSample(CrossBorderTour tour)
    {

        for (int sample = 1; sample <= sampleRate; ++sample)
        {

            if (sampledEntryMgra[sample] > 0)
            {

                int originMgra = sampledEntryMgra[sample];
                int destinationMgra = sampledDestinationMgra[sample];

                tour.setOriginMGRA(originMgra);
                tour.setOriginTAZ(sampledOriginTazs[sample]);
                tour.setDestinationMGRA(destinationMgra);
                tour.setDestinationTAZ(mgraManager.getTaz(destinationMgra));
                tour.setPoe(sampledStations[sample]);

                double logsum = tourModeChoiceModel.getLogsum(tour, logger, "Sample logsum "
                        + sample, "tour " + tour.getID());
                tourModeChoiceLogsums[sample] = logsum;
            } else tourModeChoiceLogsums[sample] = 0;

        }

    }

    /**
     * Choose a station and internal destination MGRA for the tour.
     * 
     * @param tour
     *            A cross border tour with a tour purpose and departure\arrival
     *            time and SENTRI availability members.
     */
    public void chooseStationAndDestination(CrossBorderTour tour)
    {

        chooseStationMgraSample(tour);
        calculateLogsumsForSample(tour);

        double random = tour.getRandom();
        dmu.setPurpose(tour.getPurpose());

        // set size terms for each sampled station-mgra pair corresponding to
        // mgra
        dmu.setSizeTerms(sampledSizeTerms);

        // set population accessibility for each station-mgra pair corresponding
        // to station
        dmu.setStationPopulationAccessibilities(sampledStationLogsums);

        // set the sampled stations
        dmu.setPoeNumbers(sampledStations);

        // set the correction factors
        dmu.setCorrectionFactors(sampledCorrectionFactors);

        // set the tour mode choice logsums
        dmu.setTourModeLogsums(tourModeChoiceLogsums);

        // set the origin and destination tazs
        dmu.setOriginTazs(sampledOriginTazs);
        dmu.setDestinationTazs(sampledDestinationTazs);

        if (tour.getDebugChoiceModels())
        {
            logger.info("***");
            logger.info("Choosing station-destination alternative from sample");
            tour.logTourObject(logger, 1000);

            // log the sample
            logSample();
            destModel.choiceModelUtilityTraceLoggerHeading("Station-destination model", "tour "
                    + tour.getID());
        }

        destModel.computeUtilities(dmu, dmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            destModel.logUECResults(logger, "Station-destination model");
        }
        int alt = destModel.getChoiceResult(random);

        int entryMgra = sampledEntryMgra[alt];
        int primaryDestination = sampledDestinationMgra[alt];
        int poe = sampledStations[alt];
        int taz = sampledOriginTazs[alt];

        tour.setOriginMGRA(entryMgra);
        tour.setOriginTAZ(taz);
        tour.setDestinationMGRA(primaryDestination);
        tour.setDestinationTAZ(mgraManager.getTaz(primaryDestination));
        tour.setPoe(poe);

    }

    public CrossBorderTourModeChoiceModel getTourModeChoiceModel()
    {
        return tourModeChoiceModel;
    }

    public void logSample()
    {

        logger.info("Sampled station-destination alternatives");

        logger.info("\nAlt POE EntryMgra DestMgra");
        for (int i = 1; i <= sampleRate; ++i)
        {
            logger.info(i + " " + sampledStations[i] + " " + sampledEntryMgra[i] + " "
                    + sampledDestinationMgra[i]);
        }
        logger.info("");
    }

    /**
     * @return the mgraSizeTerms
     */
    public double[][] getMgraSizeTerms()
    {
        return mgraSizeTerms;
    }

    /**
     * @return the tazSizeTerms
     */
    public double[][] getTazSizeTerms()
    {
        return tazSizeTerms;
    }

    /**
     * @return the mgraProbabilities
     */
    public double[][][] getMgraProbabilities()
    {
        return mgraProbabilities;
    }
}
