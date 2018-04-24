package org.sandag.abm.visitor;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.ConcreteAlternative;
import com.pb.common.newmodel.UtilityExpressionCalculator;

public class VisitorStopLocationChoiceModel
{

    private transient Logger             logger = Logger.getLogger("visitorModel");

    private McLogsumsCalculator          logsumHelper;
    private VisitorModelStructure        modelStructure;
    private MgraDataManager              mgraManager;
    private TazDataManager               tazManager;
    private VisitorStopLocationChoiceDMU dmu;
    private VisitorTripModeChoiceModel   tripModeChoiceModel;
    double                               logsum = 0;
    private ChoiceModelApplication       soaModel;
    private ChoiceModelApplication       destModel;

    // the following arrays are calculated in the station-destination choice
    // model and passed in the constructor.
    private double[][]                   mgraSizeTerms;                            // by
                                                                                    // purpose,
                                                                                    // MGRA
    private double[][][]                 mgraProbabilities;                        // by
                                                                                    // purpose,
                                                                                    // TAZ,
                                                                                    // MGRA

    private TableDataSet                 alternativeData;                          // the
                                                                                    // alternatives,
                                                                                    // with
                                                                                    // a
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

    // following are used for each taz alternative
    private double[]                     soaTourOrigToStopDistanceAlt;             // by
                                                                                    // TAZ
    private double[]                     soaStopToTourDestDistanceAlt;             // by
                                                                                    // TAZ
    private double[][]                   tazSizeTerms;                             // by
                                                                                    // purpose,
                                                                                    // TAZ
                                                                                    // -
                                                                                    // set
                                                                                    // by
                                                                                    // constructor

    // following are used for sampled mgras
    private int                          sampleRate;
    private double[][]                   sampledSizeTerms;                         // by
                                                                                    // purpose,
                                                                                    // alternative
                                                                                    // (taz
                                                                                    // or
                                                                                    // sampled
                                                                                    // mgra)
    private double[]                     correctionFactors;                        // by
                                                                                    // alternative
                                                                                    // (sampled
                                                                                    // mgra,
                                                                                    // for
                                                                                    // full
                                                                                    // model
                                                                                    // only)
    private int[]                        sampledTazs;                              // by
                                                                                    // alternative
                                                                                    // (sampled
                                                                                    // taz)
    private int[]                        sampledMgras;                             // by
                                                                                    // alternative(sampled
                                                                                    // mgra)
    private double[]                     tourOrigToStopDistanceAlt;
    private double[]                     stopToTourDestDistanceAlt;
    private double[]                     osMcLogsumAlt;
    private double[]                     sdMcLogsumAlt;

    HashMap<Integer, Integer>            frequencyChosen;

    private VisitorTrip                  trip;

    private int                          originMgra;                               // the
                                                                                    // origin
                                                                                    // MGRA
                                                                                    // of
                                                                                    // the
                                                                                    // stop
                                                                                    // (originMgra
                                                                                    // ->
                                                                                    // stopMgra
                                                                                    // ->
                                                                                    // destinationMgra)
    private int                          destinationMgra;                          // the
                                                                                    // destination
                                                                                    // MGRA
                                                                                    // of
                                                                                    // the
                                                                                    // stop
                                                                                    // (originMgra
                                                                                    // ->
                                                                                    // stopMgra
                                                                                    // ->
                                                                                    // destinationMgra)

    /**
     * Constructor.
     * 
     * @param propertyMap
     * @param myModelStructure
     * @param dmuFactory
     * @param myLogsumHelper
     */
    public VisitorStopLocationChoiceModel(HashMap<String, String> propertyMap,
            VisitorModelStructure myModelStructure, VisitorDmuFactoryIf dmuFactory,
            McLogsumsCalculator myLogsumHelper)
    {
        mgraManager = MgraDataManager.getInstance(propertyMap);
        tazManager = TazDataManager.getInstance(propertyMap);

        modelStructure = myModelStructure;
        logsumHelper = myLogsumHelper;

        setupStopLocationChoiceModel(propertyMap, dmuFactory);

        frequencyChosen = new HashMap<Integer, Integer>();

        trip = new VisitorTrip();

    }

    /**
     * Read the UEC file and set up the stop destination choice model.
     * 
     * @param propertyMap
     * @param dmuFactory
     */
    private void setupStopLocationChoiceModel(HashMap<String, String> rbMap,
            VisitorDmuFactoryIf dmuFactory)
    {

        logger.info(String.format("setting up visitor stop location choice model."));

        dmu = dmuFactory.getVisitorStopLocationChoiceDMU();

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String visitorStopLocationSoaFileName = Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.soa.uec.file");
        visitorStopLocationSoaFileName = uecFileDirectory + visitorStopLocationSoaFileName;

        int soaDataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.soa.data.page"));
        int soaModelPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.soa.model.page"));

        String visitorStopLocationFileName = Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.uec.file");
        visitorStopLocationFileName = uecFileDirectory + visitorStopLocationFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.data.page"));
        int modelPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "visitor.slc.model.page"));

        // create a ChoiceModelApplication object for the SOA model.
        soaModel = new ChoiceModelApplication(visitorStopLocationSoaFileName, soaModelPage,
                soaDataPage, rbMap, (VariableTable) dmu);

        // create a ChoiceModelApplication object for the full model.
        destModel = new ChoiceModelApplication(visitorStopLocationFileName, modelPage, dataPage,
                rbMap, (VariableTable) dmu);
        sampleRate = destModel.getAlternativeNames().length;

        // get the alternative data
        UtilityExpressionCalculator uec = soaModel.getUEC();
        alternativeData = uec.getAlternativeData();
        int purposes = modelStructure.VISITOR_PURPOSES.length;

        sampledSizeTerms = new double[purposes][sampleRate + 1]; // by purpose,
                                                                 // alternative
                                                                 // (taz or
                                                                 // sampled
                                                                 // mgra)
        correctionFactors = new double[sampleRate + 1]; // by alternative
                                                        // (sampled mgra, for
                                                        // full model only)
        sampledTazs = new int[sampleRate + 1]; // by alternative (sampled taz)
        sampledMgras = new int[sampleRate + 1]; // by alternative (sampled mgra)
        tourOrigToStopDistanceAlt = new double[sampleRate + 1];
        stopToTourDestDistanceAlt = new double[sampleRate + 1];
        osMcLogsumAlt = new double[sampleRate + 1];
        sdMcLogsumAlt = new double[sampleRate + 1];

    }


    /**
     * Create a sample for the tour and stop.
     * 
     * @param tour
     * @param stop
     */
    private void createSample(VisitorTour tour, VisitorStop stop)
    {

        int purpose = tour.getPurpose();
        int origTaz = 0;
        int destTaz = 0;
        int period = modelStructure.AM;

        dmu.setPurpose(purpose);
        boolean inbound = stop.isInbound();
        if (inbound)
        {
            dmu.setInboundStop(1);
            dmu.setStopsOnHalfTour(tour.getNumberInboundStops());

            // destination for inbound stops is always tour origin
            destinationMgra = tour.getOriginMGRA();
            destTaz = mgraManager.getTaz(destinationMgra);

            // origin for inbound stops is tour destination if first stop, or
            // last chosen stop location
            if (stop.getId() == 0)
            {
                originMgra = tour.getDestinationMGRA();
                origTaz = mgraManager.getTaz(originMgra);
            } else
            {
                VisitorStop[] stops = tour.getInboundStops();
                originMgra = stops[stop.getId() - 1].getMgra();
                origTaz = mgraManager.getTaz(originMgra);
            }

        } else
        {
            dmu.setInboundStop(0);
            dmu.setStopsOnHalfTour(tour.getNumberOutboundStops());

            // destination for outbound stops is always tour destination
            destinationMgra = tour.getDestinationMGRA();
            destTaz = mgraManager.getTaz(destinationMgra);

            // origin for outbound stops is tour origin if first stop, or last
            // chosen stop location
            if (stop.getId() == 0)
            {
                originMgra = tour.getOriginMGRA();
                origTaz = mgraManager.getTaz(originMgra);
            } else
            {
                VisitorStop[] stops = tour.getOutboundStops();
                originMgra = stops[stop.getId() - 1].getMgra();
                origTaz = mgraManager.getTaz(originMgra);
            }
        }
        dmu.setStopNumber(stop.getId() + 1);
        dmu.setDmuIndexValues(origTaz, origTaz, origTaz, 0, false);

        // distances
        soaTourOrigToStopDistanceAlt = logsumHelper.getAnmSkimCalculator().getTazDistanceFromTaz(
                origTaz, period);
        soaStopToTourDestDistanceAlt = logsumHelper.getAnmSkimCalculator().getTazDistanceToTaz(
                destTaz, period);
        dmu.setTourOrigToStopDistanceAlt(soaTourOrigToStopDistanceAlt);
        dmu.setStopToTourDestDistanceAlt(soaStopToTourDestDistanceAlt);

        dmu.setSizeTerms(tazSizeTerms);

        // solve for each sample
        frequencyChosen.clear();
        for (int sample = 1; sample <= sampleRate; ++sample)
        {

            // solve the UEC
            soaModel.computeUtilities(dmu, dmu.getDmuIndexValues());

            // choose a TAZ
            double random = tour.getRandom();
            ConcreteAlternative[] alts = soaModel.getAlternatives();
            double cumProb = 0;
            double altProb = 0;
            int sampledTaz = -1;
            for (int i = 0; i < alts.length; ++i)
            {
                cumProb += alts[i].getProbability();
                if (random < cumProb)
                {
                    sampledTaz = (int) alternativeData.getValueAt(i + 1, "dest");
                    altProb = alts[i].getProbability();
                    break;
                }
            }

            // set the sampled taz in the array
            sampledTazs[sample] = sampledTaz;

            // now find an MGRA in the taz corresponding to the random number
            // drawn:
            // note that the indexing needs to be offset by the cumulative
            // probability of the chosen taz and the
            // mgra probabilities need to be scaled by the alternatives
            // probability
            int[] mgraArray = tazManager.getMgraArray(sampledTaz);
            int mgraNumber = 0;
            double[] mgraCumProb = mgraProbabilities[purpose][sampledTaz];

            if (mgraCumProb == null)
            {
                logger.error("Error: mgraCumProb array is null for purpose " + purpose
                        + " sampledTaz " + sampledTaz + " hhID " + tour.getID());
                throw new RuntimeException();
            }
            for (int i = 0; i < mgraCumProb.length; ++i)
            {
                cumProb += mgraCumProb[i] * altProb;
                if (cumProb > random && mgraCumProb[i] > 0)
                {
                    mgraNumber = mgraArray[i];
                    sampledMgras[sample] = mgraNumber;

                    // for now, store the probability in the correction factors
                    // array
                    correctionFactors[sample] = mgraCumProb[i] * altProb;

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
            sampledSizeTerms[purpose][sample] = mgraSizeTerms[purpose][mgraNumber];

            // set the distances for the sample
            tourOrigToStopDistanceAlt[sample] = soaTourOrigToStopDistanceAlt[sampledTaz];
            stopToTourDestDistanceAlt[sample] = soaStopToTourDestDistanceAlt[sampledTaz];

        }
        // calculate correction factors
        for (int sample = 1; sample <= sampleRate; ++sample)
        {
            int mgra = sampledMgras[sample];
            int freq = frequencyChosen.get(mgra);
            correctionFactors[sample] = (float) Math.log((double) freq / correctionFactors[sample]);

        }

    }

    /**
     * Choose a stop location from the sample.
     * 
     * @param tour
     *            The visitor tour.
     * @param stop
     *            The visitor stop.
     */
    public void chooseStopLocation(VisitorTour tour, VisitorStop stop)
    {

        // create a sample of mgras and set all of the dmu properties
        createSample(tour, stop);
        
        //Wu added, if not mgra samples are found, allow tour origin mgra as a stop alternative
        if (sampledMgras.length==0){
        	logger.error("sample mgra=0");
            logger.error("Tour ID " + tour.getID() + " stop id " + stop.getId()
            + " purpose " + modelStructure.VISITOR_PURPOSES[stop.getPurpose()]);
            for (int sample = 1; sample <= sampleRate; ++sample)
            {
            	sampledMgras[sample]=tour.getOriginMGRA();
            }
        }
        
        dmu.setCorrectionFactors(correctionFactors);
        dmu.setSizeTerms(sampledSizeTerms);
        dmu.setTourOrigToStopDistanceAlt(stopToTourDestDistanceAlt);
        dmu.setStopToTourDestDistanceAlt(stopToTourDestDistanceAlt);
        dmu.setSampleNumber(sampledMgras);

        // calculate trip mode choice logsums to and from stop
        for (int i = 1; i <= sampleRate; ++i)
        {

            // to stop (originMgra -> stopMgra )
            trip.initializeFromStop(tour, stop, true);
            trip.setOriginMgra(trip.getOriginMgra());
            trip.setDestinationMgra(sampledMgras[i]);
            double logsum = tripModeChoiceModel.computeUtilities(tour, trip);
            osMcLogsumAlt[i] = logsum;

            // from stop (stopMgra -> destinationMgra)
            trip.initializeFromStop(tour, stop, false);
            trip.setOriginMgra(sampledMgras[i]);
            trip.setDestinationMgra(trip.getDestinationMgra());
            logsum = tripModeChoiceModel.computeUtilities(tour, trip);
            sdMcLogsumAlt[i] = logsum;

        }
        dmu.setOsMcLogsumAlt(osMcLogsumAlt);
        dmu.setSdMcLogsumAlt(sdMcLogsumAlt);

        // log headers to traceLogger
        if (tour.getDebugChoiceModels())
        {
            String decisionMakerLabel = "Tour ID " + tour.getID() + " stop id " + stop.getId()
                    + " purpose " + modelStructure.VISITOR_PURPOSES[stop.getPurpose()];
            destModel.choiceModelUtilityTraceLoggerHeading(
                    "Intermediate stop location choice model", decisionMakerLabel);
        }

        destModel.computeUtilities(dmu, dmu.getDmuIndexValues());
        double random = tour.getRandom();
        int alt = destModel.getChoiceResult(random);
        int destMgra = sampledMgras[alt];
        stop.setMgra(destMgra);

        // write UEC calculation results and choice
        if (tour.getDebugChoiceModels())
        {
            String decisionMakerLabel = "Tour ID " + tour.getID() + " stop id " + stop.getId()
                    + " purpose " + modelStructure.VISITOR_PURPOSES[stop.getPurpose()];
            String loggingHeader = String.format("%s   %s",
                    "Intermediate stop location choice model", decisionMakerLabel);
            destModel.logUECResults(logger, loggingHeader);
            logger.info("Chose alternative " + alt + " mgra " + destMgra + " with random number "
                    + random);
            logger.info("");
            logger.info("");
        }

    }

    /**
     * @return the mgraSizeTerms
     */
    public double[][] getMgraSizeTerms()
    {
        return mgraSizeTerms;
    }

    /**
     * @return the mgraProbabilities
     */
    public double[][][] getMgraProbabilities()
    {
        return mgraProbabilities;
    }

    /**
     * @return the tazSizeTerms
     */
    public double[][] getTazSizeTerms()
    {
        return tazSizeTerms;
    }

    /**
     * Set mgra size terms: must call before choosing location.
     * 
     * @param mgraSizeTerms
     */
    public void setMgraSizeTerms(double[][] mgraSizeTerms)
    {

        if (mgraSizeTerms == null)
        {
            logger.error("Error attempting to set MGRASizeTerms in VisitorStopLocationChoiceModel:  MGRASizeTerms are null");
            throw new RuntimeException();
        }
        this.mgraSizeTerms = mgraSizeTerms;
    }

    /**
     * Set taz size terms: must call before choosing location.
     * 
     * @param tazSizeTerms
     */
    public void setTazSizeTerms(double[][] tazSizeTerms)
    {
        if (tazSizeTerms == null)
        {
            logger.error("Error attempting to set TazSizeTerms in VisitorStopLocationChoiceModel:  TazSizeTerms are null");
            throw new RuntimeException();
        }
        this.tazSizeTerms = tazSizeTerms;
    }

    /**
     * Set the mgra probabilities. Must call before choosing location.
     * 
     * @param mgraProbabilities
     */
    public void setMgraProbabilities(double[][][] mgraProbabilities)
    {
        if (mgraProbabilities == null)
        {
            logger.error("Error attempting to set mgraProbabilities in VisitorStopLocationChoiceModel:  mgraProbabilities are null");
            throw new RuntimeException();
        }
        this.mgraProbabilities = mgraProbabilities;
    }

    /**
     * Set trip mode choice model. Must call before choosing location.
     * 
     * @param tripModeChoiceModel
     */
    public void setTripModeChoiceModel(VisitorTripModeChoiceModel tripModeChoiceModel)
    {
        this.tripModeChoiceModel = tripModeChoiceModel;
    }

}
