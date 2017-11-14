package org.sandag.abm.visitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

/**
 * This class is used for both the sample of alternatives and the full
 * destination choice model for visitor tours.
 * 
 * 
 * @author Freedman
 * 
 */
public class VisitorTourDestChoiceModel
{

    private double[][]                  mgraSizeTerms;                            // by
                                                                                   // purpose,
                                                                                   // MGRA
    private double[][]                  tazSizeTerms;                             // by
                                                                                   // purpose,
                                                                                   // TAZ
    private double[][][]                mgraProbabilities;                        // by
                                                                                   // purpose,
                                                                                   // tazNumber,
                                                                                   // mgra
                                                                                   // index
                                                                                   // (sequential,
                                                                                   // 0-based)
    private Matrix[]                    tazProbabilities;                         // by
                                                                                   // purpose,
                                                                                   // origin
                                                                                   // TAZ,
                                                                                   // destination
                                                                                   // TAZ
    private TableDataSet                alternativeData;                          // the
                                                                                   // alternatives,
                                                                                   // with
                                                                                   // a
                                                                                   // dest
                                                                                   // field
                                                                                   // indicating
                                                                                   // tazNumber
    private int[]                       sampleMgras;                              // numbers
                                                                                   // of
                                                                                   // mgra
                                                                                   // for
                                                                                   // the
                                                                                   // sample
    private int[]                       sampleTazs;                               // numbers
                                                                                   // of
                                                                                   // taz
                                                                                   // for
                                                                                   // the
                                                                                   // sample
    private double[]                    sampleCorrectionFactors;                  // correction
                                                                                   // factors
                                                                                   // for
                                                                                   // sample
    private double[][]                  sampleSizeTerms;                          // size
                                                                                   // terms
                                                                                   // for
                                                                                   // sample
    private double[]                    sampleLogsums;                            // tour
                                                                                   // mc
                                                                                   // logsums

    private transient Logger            logger = Logger.getLogger("visitorModel");

    private TazDataManager              tazManager;
    private MgraDataManager             mgraManager;

    private ChoiceModelApplication[]    soaModel;
    private ChoiceModelApplication[]    destModel;
    private UtilityExpressionCalculator sizeTermUEC;
    private HashMap<String, String>     rbMap;

    private VisitorTourDestChoiceDMU    dcDmu;
    private VisitorTourModeChoiceModel  tourModeChoiceModel;

    private HashMap<Integer, Integer>   frequencyChosen;                          // by
                                                                                   // mgra,
                                                                                   // number
                                                                                   // of
                                                                                   // times
                                                                                   // chosen
    private int                         sampleRate;

    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of airport model DMUs
     */
    public VisitorTourDestChoiceModel(HashMap<String, String> rbMap,
            VisitorModelStructure modelStructure, VisitorDmuFactoryIf dmuFactory,
            McLogsumsCalculator logsumsCalculator)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String visitorDCSoaFileName = Util.getStringValueFromPropertyMap(rbMap,
                "visitor.dc.soa.uec.file");
        visitorDCSoaFileName = uecFileDirectory + visitorDCSoaFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.dc.soa.data.page"));
        int sizePage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.dc.soa.size.page"));

        // initiate a DMU
        dcDmu = dmuFactory.getVisitorTourDestChoiceDMU();

        // read the model pages from the property file, create one choice model
        // for each soa model
        soaModel = new ChoiceModelApplication[VisitorModelStructure.VISITOR_PURPOSES.length];
        for (int i = 0; i < soaModel.length; ++i)
        {

            // get page from property file
            String purpose = VisitorModelStructure.VISITOR_PURPOSES[i].toLowerCase();
            String purposeName = "visitor.dc.soa." + purpose + ".page";
            String purposeString = Util.getStringValueFromPropertyMap(rbMap, purposeName);
            purposeString.replaceAll(" ", "");
            int destModelPage = Integer.parseInt(purposeString);

            // create a ChoiceModelApplication object for the filename, model
            // page and data page.
            soaModel[i] = new ChoiceModelApplication(visitorDCSoaFileName, destModelPage, dataPage,
                    rbMap, (VariableTable) dcDmu);
        }

        // get the alternative data from the first segment
        UtilityExpressionCalculator uec = soaModel[0].getUEC();
        alternativeData = uec.getAlternativeData();

        // create a UEC to solve size terms for each MGRA
        sizeTermUEC = new UtilityExpressionCalculator(new File(visitorDCSoaFileName), sizePage,
                dataPage, rbMap, (VariableTable) dcDmu);

        // create the full model UECs
        // read the model pages from the property file, create one choice model
        // for each full model
        String visitorDCFileName = Util.getStringValueFromPropertyMap(rbMap, "visitor.dc.uec.file");
        visitorDCFileName = uecFileDirectory + visitorDCFileName;
        destModel = new ChoiceModelApplication[VisitorModelStructure.VISITOR_PURPOSES.length];
        for (int i = 0; i < destModel.length; ++i)
        {

            // get page from property file
            String purpose = VisitorModelStructure.VISITOR_PURPOSES[i].toLowerCase();
            String purposeName = "visitor.dc." + purpose + ".page";
            String purposeString = Util.getStringValueFromPropertyMap(rbMap, purposeName);
            purposeString.replaceAll(" ", "");
            int destModelPage = Integer.parseInt(purposeString);

            // create a ChoiceModelApplication object for the filename, model
            // page and data page.
            destModel[i] = new ChoiceModelApplication(visitorDCFileName, destModelPage, dataPage,
                    rbMap, (VariableTable) dcDmu);
            if (i == 0) sampleRate = destModel[i].getNumberOfAlternatives();
        }

        frequencyChosen = new HashMap<Integer, Integer>();
        sampleMgras = new int[sampleRate + 1];
        sampleTazs = new int[sampleRate + 1];
        sampleCorrectionFactors = new double[sampleRate + 1];
        sampleSizeTerms = new double[destModel.length][sampleRate + 1];
        sampleLogsums = new double[sampleRate + 1];

        tourModeChoiceModel = new VisitorTourModeChoiceModel(rbMap, modelStructure, dmuFactory);

    }

    /**
     * Calculate size terms
     */
    public void calculateSizeTerms(VisitorDmuFactoryIf dmuFactory)
    {

        logger.info("Calculating Visitor Tour Destination Choice Model MGRA Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();
        int maxTaz = tazManager.getMaxTaz();
        int purposes = sizeTermUEC.getNumberOfAlternatives();

        mgraSizeTerms = new double[purposes][maxMgra + 1];
        tazSizeTerms = new double[purposes][maxTaz + 1];
        IndexValues iv = new IndexValues();
        VisitorTourDestChoiceDMU aDmu = dmuFactory.getVisitorTourDestChoiceDMU();

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
        logger.info("Finished Calculating Visitor Tour Destination Choice Model MGRA Size Terms");
    }

    /**
     * Calculate taz probabilities. This method initializes and calculates the
     * tazProbabilities array.
     */
    public void calculateTazProbabilities(VisitorDmuFactoryIf dmuFactory)
    {

        if (tazSizeTerms == null)
        {
            logger.error("Error:  attemping to execute VisitorTourDestChoiceModel.calculateTazProbabilities() before calling calculateMgraProbabilities()");
            throw new RuntimeException();
        }

        logger.info("Calculating Visitor Model TAZ Probabilities Arrays");

        // initialize taz probabilities array
        int purposes = tazSizeTerms.length;

        // initialize the arrays
        tazProbabilities = new Matrix[purposes];

        // iterate through the alternatives in the alternatives file and set the
        // size term and station logsum for each alternative
        UtilityExpressionCalculator soaModelUEC = soaModel[0].getUEC();
        TableDataSet altData = soaModelUEC.getAlternativeData();

        dcDmu.setSizeTerms(tazSizeTerms);

        // iterate through purposes
        for (int purpose = 0; purpose < soaModel.length; ++purpose)
        {

            tazProbabilities[purpose] = new Matrix("Prob_Matrix", "Probability Matrix",
                    altData.getRowCount() + 1, altData.getRowCount() + 1);
            int[] tazs = altData.getColumnAsInt("dest");
            tazProbabilities[purpose].setExternalNumbersZeroBased(tazs);

            // iterate through origin zones, solve the UEC and store the results
            // in the matrix
            for (int taz = 0; taz < tazs.length; ++taz)
            {

                int originTaz = (int) tazs[taz];

                // set origin taz in dmu (destination set in UEC by alternative)
                dcDmu.setDmuIndexValues(originTaz, originTaz, originTaz, originTaz, false);

                dcDmu.setPurpose(purpose);

                // Calculate utilities & probabilities
                soaModel[purpose].computeUtilities(dcDmu, dcDmu.getDmuIndexValues());

                // Store probabilities (by purpose)
                double[] probabilities = soaModel[purpose].getCumulativeProbabilities();

                for (int i = 0; i < probabilities.length; ++i)
                {

                    double cumProb = probabilities[i];
                    int destTaz = (int) altData.getValueAt(i + 1, "dest");
                    tazProbabilities[purpose].setValueAt(originTaz, destTaz, (float) cumProb);
                }
            }
        }
        logger.info("Finished Calculating Visitor Model TAZ Probabilities Arrays");
    }

    /**
     * Choose a MGRA alternative for sampling
     * 
     * @param tour
     *            VisitorTour with purpose and Random
     */
    private void chooseMgraSample(VisitorTour tour)
    {

        frequencyChosen.clear();

        // choose sample, set station logsums and mgra size terms
        int purpose = tour.getPurpose();
        int originTaz = mgraManager.getTaz(tour.getOriginMGRA());

        for (int sample = 1; sample <= sampleRate; ++sample)
        {

            // first find a TAZ and station
            int alt = 0;
            Matrix tazCumProb = tazProbabilities[purpose];
            double altProb = 0;
            double cumProb = 0;
            double random = tour.getRandom();
            int destinationTaz = -1;
            for (int i = 0; i < tazCumProb.getColumnCount(); ++i)
            {
                destinationTaz = (int) tazCumProb.getExternalColumnNumber(i);
                if (tazCumProb.getValueAt(originTaz, destinationTaz) > random)
                {
                    alt = i;
                    if (i != 0)
                    {
                        cumProb = tazCumProb.getValueAt(originTaz,
                                tazCumProb.getExternalColumnNumber(i - 1));
                        altProb = tazCumProb.getValueAt(originTaz, destinationTaz)
                                - tazCumProb.getValueAt(originTaz,
                                        tazCumProb.getExternalColumnNumber(i - 1));
                    } else
                    {
                        altProb = tazCumProb.getValueAt(originTaz, destinationTaz);
                    }
                    break;
                }
            }

            // get the taz number of the alternative, and an array of mgras in
            // that taz

            int[] mgraArray = tazManager.getMgraArray(destinationTaz);

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
                    sampleMgras[sample] = mgraNumber;
                    sampleTazs[sample] = mgraManager.getTaz(mgraNumber);

                    // for now, store the probability in the correction factors
                    // array
                    sampleCorrectionFactors[sample] = mgraCumProb[i] * altProb;

                    break;
                }
            }
            // store frequency chosen
            if (!frequencyChosen.containsKey(mgraNumber))
            {
                frequencyChosen.put(mgraNumber, 1);
            } else
            {
                int freq = frequencyChosen.get(mgraNumber);
                frequencyChosen.put(mgraNumber, freq + 1);
            }
            // set the size terms for the sample
            sampleSizeTerms[purpose][sample] = mgraSizeTerms[purpose][mgraNumber];
        }
        // calculate correction factors
        for (int sample = 1; sample <= sampleRate; ++sample)
        {
            int mgra = sampleMgras[sample];
            int freq = frequencyChosen.get(mgra);
            sampleCorrectionFactors[sample] = (float) Math.log((double) freq
                    / sampleCorrectionFactors[sample]);

        }

    }

    /**
     * Use the tour mode choice model to calculate the logsum for each sampled
     * mgra and store in the array.
     * 
     * @param tour
     *            The visitor tour.
     */
    private void calculateLogsumsForSample(VisitorTour tour)
    {

        for (int sample = 1; sample <= sampleRate; ++sample)
        {

            if (sampleMgras[sample] > 0)
            {

                int destinationMgra = sampleMgras[sample];
                tour.setDestinationMGRA(destinationMgra);

                double logsum = tourModeChoiceModel.getModeChoiceLogsum(tour, logger,
                        "Sample logsum " + sample, "tour " + tour.getID() + " dest "
                                + destinationMgra);
                sampleLogsums[sample] = logsum;
            } else sampleLogsums[sample] = 0;

        }

    }

    /**
     * Choose a destination MGRA for the tour.
     * 
     * @param tour
     *            A cross border tour with a tour origin, purpose, attributes,
     *            and departure\arrival time and SENTRI availability members.
     */
    public void chooseDestination(VisitorTour tour)
    {

        chooseMgraSample(tour);
        calculateLogsumsForSample(tour);

        double random = tour.getRandom();
        int purpose = tour.getPurpose();
        dcDmu.setPurpose(purpose);

        // set origin taz in dmu (destination set in UEC by alternative)
        int originTaz = mgraManager.getTaz(tour.getOriginMGRA());
        dcDmu.setDmuIndexValues(0, 0, originTaz, 0, false);

        // set size terms for each sampled station-mgra pair corresponding to
        // mgra
        dcDmu.setSizeTerms(sampleSizeTerms);

        // set the correction factors
        dcDmu.setCorrectionFactors(sampleCorrectionFactors);

        // set the tour mode choice logsums
        dcDmu.setTourModeLogsums(sampleLogsums);

        // sampled mgra
        dcDmu.setSampleMgra(sampleMgras);

        // sampled taz
        dcDmu.setSampleTaz(sampleTazs);

        if (tour.getDebugChoiceModels())
        {
            logger.info("***");
            logger.info("Choosing destination alternative from sample");
            tour.logTourObject(logger, 100);

            // log the sample
            destModel[purpose].choiceModelUtilityTraceLoggerHeading(
                    "Visitor tour destination model", "tour " + tour.getID());
        }

        destModel[purpose].computeUtilities(dcDmu, dcDmu.getDmuIndexValues());

        if (tour.getDebugChoiceModels())
        {
            destModel[purpose].logUECResults(logger, "Visitor tour destination model");
        }
        int alt = destModel[purpose].getChoiceResult(random);

        int primaryDestination = sampleMgras[alt];

        if (tour.getDebugChoiceModels())
        {
            logger.info("Chose destination MGRA " + primaryDestination);
        }

        tour.setDestinationMGRA(primaryDestination);
    }

    /**
     * @return the tourModeChoiceModel
     */
    public VisitorTourModeChoiceModel getTourModeChoiceModel()
    {
        return tourModeChoiceModel;
    }

    /**
     * @param tourModeChoiceModel
     *            the tourModeChoiceModel to set
     */
    public void setTourModeChoiceModel(VisitorTourModeChoiceModel tourModeChoiceModel)
    {
        this.tourModeChoiceModel = tourModeChoiceModel;
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
