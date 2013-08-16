package org.sandag.abm.specialevent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class SpecialEventOriginChoiceModel
{

    private double[][]                  mgraSizeTerms;                                 // by
                                                                                        // purpose,
                                                                                        // MGRA
    private double[][]                  tazSizeTerms;                                  // by
                                                                                        // purpose,
                                                                                        // TAZ
    private double[][][]                mgraProbabilities;                             // by
                                                                                        // purpose,
                                                                                        // tazNumber,
                                                                                        // mgra
                                                                                        // index
                                                                                        // (sequential,
                                                                                        // 0-based)
    private Matrix[]                    tazProbabilities;                              // by
                                                                                        // purpose,
                                                                                        // origin
                                                                                        // TAZ,
                                                                                        // destination
                                                                                        // TAZ

    private TableDataSet                alternativeData;                               // the
                                                                                        // alternatives,
                                                                                        // with
                                                                                        // a
                                                                                        // dest
                                                                                        // field
                                                                                        // indicating
                                                                                        // tazNumber

    private transient Logger            logger = Logger.getLogger("specialEventModel");

    private TazDataManager              tazManager;
    private MgraDataManager             mgraManager;

    private ChoiceModelApplication      destModel;
    private UtilityExpressionCalculator sizeTermUEC;
    private HashMap<String, String>     rbMap;
    private HashMap<String, Integer>    purposeMap;                                    // string
                                                                                        // is
                                                                                        // purpose,
                                                                                        // int
                                                                                        // is
                                                                                        // alternative
                                                                                        // for
                                                                                        // size
                                                                                        // terms

    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of airport model DMUs
     */
    public SpecialEventOriginChoiceModel(HashMap<String, String> rbMap,
            SpecialEventDmuFactoryIf dmuFactory, TableDataSet eventData)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String destUecFileName = Util.getStringValueFromPropertyMap(rbMap,
                "specialEvent.dc.uec.file");
        destUecFileName = uecFileDirectory + destUecFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "specialEvent.dc.data.page"));
        int sizePage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "specialEvent.dc.size.page"));
        int modelPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "specialEvent.dc.model.page"));

        // read the model pages from the property file, create one choice model
        // for each
        // get page from property file

        SpecialEventOriginChoiceDMU dcDmu = dmuFactory.getSpecialEventOriginChoiceDMU();

        // create a ChoiceModelApplication object for the filename, model page
        // and data page.
        destModel = new ChoiceModelApplication(destUecFileName, modelPage, dataPage, rbMap,
                (VariableTable) dcDmu);

        // get the alternative data from the first segment
        UtilityExpressionCalculator uec = destModel.getUEC();
        alternativeData = uec.getAlternativeData();

        // create a UEC to solve size terms for each MGRA
        SpecialEventOriginChoiceDMU dmu = dmuFactory.getSpecialEventOriginChoiceDMU();
        sizeTermUEC = new UtilityExpressionCalculator(new File(destUecFileName), sizePage,
                dataPage, rbMap, dmu);

    }

    /**
     * Calculate size terms
     */
    public void calculateSizeTerms(SpecialEventDmuFactoryIf dmuFactory)
    {

        logger.info("Calculating Special Event Origin Choice Model Size Terms");

        ArrayList<Integer> mgras = mgraManager.getMgras();
        int[] mgraTaz = mgraManager.getMgraTaz();
        int maxMgra = mgraManager.getMaxMgra();
        int maxTaz = tazManager.getMaxTaz();
        int purposes = sizeTermUEC.getNumberOfAlternatives();

        // set up the map of string purpose to integer purpose
        String[] altNames = sizeTermUEC.getAlternativeNames();
        purposeMap = new HashMap<String, Integer>();
        for (int i = 0; i < altNames.length; ++i)
        {
            purposeMap.put(altNames[i], i);
        }

        mgraSizeTerms = new double[purposes][maxMgra + 1];
        tazSizeTerms = new double[purposes][maxTaz + 1];
        IndexValues iv = new IndexValues();
        SpecialEventOriginChoiceDMU aDmu = dmuFactory.getSpecialEventOriginChoiceDMU();

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
        logger.info("Finished Calculating Special Event Tour Origin Choice Model Size Terms");
    }

    /**
     * Calculate taz probabilities. This method initializes and calculates the
     * tazProbabilities array.
     */
    public void calculateTazProbabilities(SpecialEventDmuFactoryIf dmuFactory)
    {

        if (tazSizeTerms == null)
        {
            logger.error("Error:  attemping to execute SpecialEventTourOriginChoiceModel.calculateTazProbabilities() before calling calculateMgraProbabilities()");
            throw new RuntimeException();
        }

        logger.info("Calculating Special Event Model TAZ Probabilities Arrays");

        // initialize taz probabilities array
        int purposes = tazSizeTerms.length;

        // initialize the arrays
        tazProbabilities = new Matrix[purposes];

        // iterate through the alternatives in the alternatives file and set the
        // size term for each alternative
        UtilityExpressionCalculator modelUEC = destModel.getUEC();
        TableDataSet altData = modelUEC.getAlternativeData();

        SpecialEventOriginChoiceDMU dcDmu = dmuFactory.getSpecialEventOriginChoiceDMU();
        dcDmu.setSizeTerms(tazSizeTerms);

        // iterate through purposes
        for (int purpose = 0; purpose < purposes; ++purpose)
        {

            tazProbabilities[purpose] = new Matrix("Prob_Matrix", "Probability Matrix",
                    altData.getRowCount() + 1, altData.getRowCount() + 1);
            int[] tazs = altData.getColumnAsInt("dest");
            tazProbabilities[purpose].setExternalNumbersZeroBased(tazs);

            // iterate through destination zones, solve the UEC for all origins
            // and store the results in the matrix
            for (int taz = 0; taz < tazs.length; ++taz)
            {

                int destinationTaz = (int) tazs[taz];

                // set origin taz in dmu (destination set in UEC by alternative)
                dcDmu.setDmuIndexValues(0, 0, 0, destinationTaz, false);

                dcDmu.setPurpose(purpose);

                // Calculate utilities & probabilities
                destModel.computeUtilities(dcDmu, dcDmu.getDmuIndexValues());

                // Store probabilities (by purpose)
                double[] probabilities = destModel.getCumulativeProbabilities();

                for (int i = 0; i < probabilities.length; ++i)
                {

                    double cumProb = probabilities[i];
                    int originTaz = (int) altData.getValueAt(i + 1, "dest");
                    tazProbabilities[purpose]
                            .setValueAt(originTaz, destinationTaz, (float) cumProb);
                }
            }
        }
        logger.info("Finished Calculating Special Event Model TAZ Probabilities Arrays");
    }

    /**
     * Choose an MGRA
     * 
     * @param eventType
     *            Event type corresponding to size term
     * @param destinationMgra
     *            MGRA of destination
     * @param random
     *            Random number
     * @return The chosen MGRA number
     */
    public int chooseMGRA(String eventType, int destinationMgra, double random, boolean debug)
    {

        int destinationTaz = mgraManager.getTaz(destinationMgra);
        int purpose = purposeMap.get(eventType);

        if (debug)
        {
            logger.info("Random number " + random);
            logger.info("Purpose " + purpose);
            logger.info("Destination TAZ " + destinationTaz);

        }

        // first find a TAZ and station
        Matrix tazCumProb = tazProbabilities[purpose];
        double altProb = 0;
        double cumProb = 0;
        int originTaz = -1;
        for (int i = 0; i < tazCumProb.getColumnCount(); ++i)
        {
            originTaz = (int) tazCumProb.getExternalColumnNumber(i);
            if (tazCumProb.getValueAt(originTaz, destinationTaz) > random)
            { // the probabilities are stored column-wise
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

        // get the taz number of the alternative, and an array of mgras in that
        // taz
        int[] mgraArray = tazManager.getMgraArray(originTaz);

        // now find an MGRA in the taz corresponding to the random number drawn:
        // note that the indexing needs to be offset by the cumulative
        // probability of the chosen taz and the
        // mgra probabilities need to be scaled by the alternatives probability
        int mgraNumber = 0;

        if (debug)
        {
            logger.info("Chosen origin TAZ " + originTaz);
        }

        double[] mgraCumProb = mgraProbabilities[purpose][originTaz];
        for (int i = 0; i < mgraCumProb.length; ++i)
        {
            cumProb += mgraCumProb[i] * altProb;
            if (cumProb > random)
            {
                mgraNumber = mgraArray[i];
            }
        }
        if (debug) logger.info("Chose origin MGRA " + mgraNumber);
        // return the chosen MGRA number
        return mgraNumber;
    }

    /**
     * Choose origin MGRAs for a special event tour.
     * 
     * @param tour
     *            A Special Event tour
     */
    public void chooseOrigin(SpecialEventTour tour)
    {

        String eventType = tour.getEventType();
        double random = tour.getRandom();
        int destinationMgra = tour.getDestinationMGRA();
        int mgra = chooseMGRA(eventType, destinationMgra, random, tour.getDebugChoiceModels());
        tour.setOriginMGRA(mgra);

    }

}
