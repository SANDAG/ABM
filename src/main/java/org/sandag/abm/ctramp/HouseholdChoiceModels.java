package org.sandag.abm.ctramp;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesCalculator;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.util.ObjectUtil;

public class HouseholdChoiceModels
{

    private transient Logger                                         logger                              = Logger.getLogger(HouseholdChoiceModels.class);

    private static final String                                      GLOBAL_MODEL_SEED_PROPERTY          = "Model.Random.Seed";
    private static final int                                         AO_SEED_OFFSET                      = 0;
    private static final int                                         TP_SEED_OFFSET                      = 1;
    private static final int                                         PP_SEED_OFFSET                      = 2;
    private static final int                                         CDAP_SEED_OFFSET                    = 3;
    private static final int                                         IMTF_SEED_OFFSET                    = 4;
    private static final int                                         IMTOD_SEED_OFFSET                   = 5;
    private static final int                                         JTF_SEED_OFFSET                     = 6;
    private static final int                                         JTDC_SEED_OFFSET                    = 7;
    private static final int                                         JTOD_SEED_OFFSET                    = 8;
    private static final int                                         INMTF_SEED_OFFSET                   = 9;
    private static final int                                         INMDC_SEED_OFFSET                   = 10;
    private static final int                                         INMTOD_SEED_OFFSET                  = 11;
    private static final int                                         AWTF_SEED_OFFSET                    = 12;
    private static final int                                         AWDC_SEED_OFFSET                    = 13;
    private static final int                                         AWTOD_SEED_OFFSET                   = 14;
    private static final int                                         STF_SEED_OFFSET                     = 15;
    private static final int                                         SLC_SEED_OFFSET                     = 16;
    private static final int                                         IE_SEED_OFFSET                      = 17;

    private static final String                                      USE_NEW_SLC_SOA_METHOD_PROPERTY_KEY = "slc.use.new.soa";

    private boolean                                                  runAutoOwnershipModel;
    private boolean                                                  runTransponderModel;
    private boolean                                                  runInternalExternalModel;
    private boolean                                                  runParkingProvisionModel;
    private boolean                                                  runCoordinatedDailyActivityPatternModel;
    private boolean                                                  runIndividualMandatoryTourFrequencyModel;
    private boolean                                                  runMandatoryTourModeChoiceModel;
    private boolean                                                  runMandatoryTourDepartureTimeAndDurationModel;
    private boolean												                           runEscortModel;
    private boolean                                                  runAtWorkSubTourFrequencyModel;
    private boolean                                                  runAtWorkSubtourLocationChoiceModel;
    private boolean                                                  runAtWorkSubtourModeChoiceModel;
    private boolean                                                  runAtWorkSubtourDepartureTimeAndDurationModel;
    private boolean                                                  runJointTourFrequencyModel;
    private boolean                                                  runJointTourLocationChoiceModel;
    private boolean                                                  runJointTourDepartureTimeAndDurationModel;
    private boolean                                                  runJointTourModeChoiceModel;
    private boolean                                                  runIndividualNonMandatoryTourFrequencyModel;
    private boolean                                                  runIndividualNonMandatoryTourLocationChoiceModel;
    private boolean                                                  runIndividualNonMandatoryTourModeChoiceModel;
    private boolean                                                  runIndividualNonMandatoryTourDepartureTimeAndDurationModel;
    private boolean                                                  runStopFrequencyModel;
    private boolean                                                  runStopLocationModel;

    private String                                                   restartModelString;

    private HouseholdAutoOwnershipModel                              aoModel;
    private TourVehicleTypeChoiceModel                               tvtcModel;
    private TransponderChoiceModel                                   tcModel;
    private InternalExternalTripChoiceModel                          ieModel;
    private ParkingProvisionModel                                    ppModel;
    private TelecommuteModel                                         teModel;
    private HouseholdCoordinatedDailyActivityPatternModel            cdapModel;
    private HouseholdIndividualMandatoryTourFrequencyModel           imtfModel;
    private HouseholdIndividualNonMandatoryTourFrequencyModel        inmtfModel;
    private SchoolEscortingModel                                     escortModel;
    private HouseholdAtWorkSubtourFrequencyModel                     awfModel;
    private StopFrequencyModel                                       stfModel;
    private TourModeChoiceModel                                      immcModel;
    private HouseholdIndividualMandatoryTourDepartureAndDurationTime imtodModel;
    private JointTourModels                                          jtfModel;
    private TourModeChoiceModel                                      nmmcModel;
    private NonMandatoryDestChoiceModel                              nmlcModel;
    private NonMandatoryTourDepartureAndDurationTime                 nmtodModel;
    private TourModeChoiceModel                                      awmcModel;
    private SubtourDestChoiceModel                                   awlcModel;
    private SubtourDepartureAndDurationTime                          awtodModel;
    private IntermediateStopChoiceModels                             stlmcModel;
    private MicromobilityChoiceModel                                 mmModel;

    private long                                                     aoTime;
    private long                                                     fpTime;
    private long                                                     ieTime;
    private long                                                     cdapTime;
    private long                                                     escortTime;
    private long                                                     imtfTime;
    private long                                                     imtodTime;
    private long                                                     imtmcTime;
    private long                                                     jtfTime;
    private long                                                     jtdcTime;
    private long                                                     jtodTime;
    private long                                                     jtmcTime;
    private long                                                     inmtfTime;
    private long                                                     inmtdcTime;
    private long                                                     inmtdcSoaTime;
    private long                                                     inmtodTime;
    private long                                                     inmtmcTime;
    private long                                                     awtfTime;
    private long                                                     awtdcTime;
    private long                                                     awtdcSoaTime;
    private long                                                     awtodTime;
    private long                                                     awtmcTime;
    private long                                                     stfTime;
    private long                                                     stdtmTime;
    private long[]                                                   returnPartialTimes                  = new long[IntermediateStopChoiceModels.NUM_CPU_TIME_VALUES];

    private int                                                      maxAlts;
    private int                                                      modelIndex;

    private int                                                      globalSeed;

    private boolean                                                  useNewSlcSoaMethod;

    private double[][][]                                             slcSizeProbs;
    private double[][]                                               slcTazSize;
    private double[][]                                               slcTazDistExpUtils;

    private double[]                                                 distanceToCordonsLogsums;

    private MgraDataManager                                          mgraManager;
    private TazDataManager                                           tdm;

    public HouseholdChoiceModels(int modelIndex, String restartModelString,
            HashMap<String, String> propertyMap, ModelStructure modelStructure,
            CtrampDmuFactoryIf dmuFactory, BuildAccessibilities aggAcc,
            McLogsumsCalculator logsumHelper, MandatoryAccessibilitiesCalculator mandAcc,
            double[] pctHighIncome, double[] pctMultipleAutos, double[] avgtts,
            double[] transpDist, double[] pctDetour, double[][][] nonManSoaDistProbs,
            double[][][] nonManSoaSizeProbs, double[][][] subTourSoaDistProbs,
            double[][][] subTourSoaSizeProbs, double[] distanceToCordonsLogsums,AutoTazSkimsCalculator tazDistanceCalculator)
    {

        this.modelIndex = modelIndex;
        this.restartModelString = restartModelString;

        this.distanceToCordonsLogsums = distanceToCordonsLogsums;

        globalSeed = Integer.parseInt(propertyMap.get(GLOBAL_MODEL_SEED_PROPERTY));

        mgraManager = MgraDataManager.getInstance(propertyMap);
        tdm = TazDataManager.getInstance(propertyMap);

        runAutoOwnershipModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_AUTO_OWNERSHIP));
        runTransponderModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_TRANSPONDER_CHOICE));
        runInternalExternalModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_INTERNAL_EXTERNAL_TRIP));
        runParkingProvisionModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_FREE_PARKING_AVAILABLE));
        runCoordinatedDailyActivityPatternModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN));
        runIndividualMandatoryTourFrequencyModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ));
        runMandatoryTourModeChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE));
        runMandatoryTourDepartureTimeAndDurationModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR));
        runEscortModel = Boolean.parseBoolean(propertyMap.get(CtrampApplication.PROPERTIES_RUN_SCHOOL_ESCORT_MODEL));
        runAtWorkSubTourFrequencyModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ));
        runAtWorkSubtourLocationChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE));
        runAtWorkSubtourModeChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE));
        runAtWorkSubtourDepartureTimeAndDurationModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR));
        runJointTourFrequencyModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_JOINT_TOUR_FREQ));
        runJointTourLocationChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_JOINT_LOCATION_CHOICE));
        runJointTourModeChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE));
        runJointTourDepartureTimeAndDurationModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR));
        runIndividualNonMandatoryTourFrequencyModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ));
        runIndividualNonMandatoryTourLocationChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE));
        runIndividualNonMandatoryTourModeChoiceModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE));
        runIndividualNonMandatoryTourDepartureTimeAndDurationModel = Boolean
                .parseBoolean(propertyMap
                        .get(CtrampApplication.PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR));
        runStopFrequencyModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_STOP_FREQUENCY));
        runStopLocationModel = Boolean.parseBoolean(propertyMap
                .get(CtrampApplication.PROPERTIES_RUN_STOP_LOCATION));

        boolean measureObjectSizes = false;

        try
        {
            useNewSlcSoaMethod = Util.getBooleanValueFromPropertyMap(propertyMap,
                    USE_NEW_SLC_SOA_METHOD_PROPERTY_KEY);

            AccessibilitiesTable accTable = aggAcc.getAccessibilitiesTableObject();

            // create the auto ownership choice model application object
            if (runAutoOwnershipModel)
            {
                aoModel = new HouseholdAutoOwnershipModel(propertyMap, dmuFactory, accTable,
                        mandAcc);
                tvtcModel = new TourVehicleTypeChoiceModel(propertyMap);
                if ( measureObjectSizes ) logger.info ( "AO size:         " + ObjectUtil.sizeOf( aoModel ) + ObjectUtil.sizeOf(tvtcModel));
             }

            if (runTransponderModel)
            {
                tcModel = new TransponderChoiceModel(propertyMap, dmuFactory, accTable,
                        pctHighIncome, pctMultipleAutos, avgtts, transpDist, pctDetour);
                if (measureObjectSizes)
                    logger.info("TC size:         " + ObjectUtil.sizeOf(tcModel));
            }

            if (runParkingProvisionModel)
            {
                ppModel = new ParkingProvisionModel(propertyMap, dmuFactory);
                teModel = new TelecommuteModel(propertyMap, dmuFactory);
                if (measureObjectSizes) {
                    logger.info("PP size:       " + ObjectUtil.sizeOf(ppModel));
                    logger.info("TE size:       " + ObjectUtil.sizeOf(teModel));
                    }
           }

            if (runInternalExternalModel)
            {
                ieModel = new InternalExternalTripChoiceModel(propertyMap, modelStructure,dmuFactory);
                if (measureObjectSizes)
                    logger.info("IE size:       " + ObjectUtil.sizeOf(ieModel));
            }

            if (runCoordinatedDailyActivityPatternModel)
            {
                cdapModel = new HouseholdCoordinatedDailyActivityPatternModel(propertyMap,
                        modelStructure, dmuFactory, accTable);
                if (measureObjectSizes)
                    logger.info("CDAP size:       " + ObjectUtil.sizeOf(cdapModel));
            }

            if (runIndividualMandatoryTourFrequencyModel)
            {
                imtfModel = new HouseholdIndividualMandatoryTourFrequencyModel(propertyMap,
                        modelStructure, dmuFactory, accTable, mandAcc);
                if (measureObjectSizes)
                    logger.info("IMTF size:       " + ObjectUtil.sizeOf(imtfModel));
            }

            if (runMandatoryTourDepartureTimeAndDurationModel || runMandatoryTourModeChoiceModel)
            {
                immcModel = new TourModeChoiceModel(propertyMap, modelStructure,
                        TourModeChoiceModel.MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);
                if (measureObjectSizes)
                    logger.info("IMMC size:       " + ObjectUtil.sizeOf(immcModel));
            }

            if (runMandatoryTourDepartureTimeAndDurationModel)
            {
                imtodModel = new HouseholdIndividualMandatoryTourDepartureAndDurationTime(
                        propertyMap, modelStructure, aggAcc.getWorkSegmentNameList(), dmuFactory,
                        immcModel);
                if (measureObjectSizes)
                    logger.info("IMTOD size:      " + ObjectUtil.sizeOf(imtodModel));
            }

            if(runEscortModel){
            	escortModel = new SchoolEscortingModel(propertyMap,mgraManager,tazDistanceCalculator);
                if ( measureObjectSizes ) logger.info ( "SEM size:      " + ObjectUtil.sizeOf( escortModel ) );
            	
            }

            if (runJointTourFrequencyModel)
            {
                jtfModel = new JointTourModels(propertyMap, accTable, modelStructure, dmuFactory);
                if (measureObjectSizes)
                    logger.info("JTF size:        " + ObjectUtil.sizeOf(jtfModel));
            }

            if (runIndividualNonMandatoryTourFrequencyModel)
            {
                inmtfModel = new HouseholdIndividualNonMandatoryTourFrequencyModel(propertyMap,
                        dmuFactory, accTable, mandAcc);
                if (measureObjectSizes)
                    logger.info("INMTF size:      " + ObjectUtil.sizeOf(inmtfModel));
            }

            if (runIndividualNonMandatoryTourLocationChoiceModel || runJointTourLocationChoiceModel
                    || runIndividualNonMandatoryTourDepartureTimeAndDurationModel
                    || runJointTourDepartureTimeAndDurationModel
                    || runIndividualNonMandatoryTourModeChoiceModel || runJointTourModeChoiceModel)
            {
                nmmcModel = new TourModeChoiceModel(propertyMap, modelStructure,
                        TourModeChoiceModel.NON_MANDATORY_MODEL_INDICATOR, dmuFactory, logsumHelper);
                if (measureObjectSizes)
                    logger.info("INMMC size:      " + ObjectUtil.sizeOf(nmmcModel));
            }

            if (runIndividualNonMandatoryTourLocationChoiceModel || runJointTourLocationChoiceModel)
            {
                nmlcModel = new NonMandatoryDestChoiceModel(propertyMap, modelStructure, aggAcc,
                        dmuFactory, nmmcModel);
                nmlcModel.setNonMandatorySoaProbs(nonManSoaDistProbs, nonManSoaSizeProbs);
                if (measureObjectSizes)
                    logger.info("INMLC size:      " + ObjectUtil.sizeOf(nmlcModel));
            }

            if (runIndividualNonMandatoryTourDepartureTimeAndDurationModel
                    || runJointTourDepartureTimeAndDurationModel)
            {
                nmtodModel = new NonMandatoryTourDepartureAndDurationTime(propertyMap,
                        modelStructure, dmuFactory, nmmcModel);
                if (measureObjectSizes)
                    logger.info("INMTOD size:     " + ObjectUtil.sizeOf(nmtodModel));
            }

            if (runAtWorkSubTourFrequencyModel)
            {
                awfModel = new HouseholdAtWorkSubtourFrequencyModel(propertyMap, modelStructure,
                        dmuFactory);
                if (measureObjectSizes)
                    logger.info("AWTF size:       " + ObjectUtil.sizeOf(awfModel));
            }

            if (runAtWorkSubtourLocationChoiceModel
                    || runAtWorkSubtourDepartureTimeAndDurationModel
                    || runAtWorkSubtourModeChoiceModel)
            {
                awmcModel = new TourModeChoiceModel(propertyMap, modelStructure,
                        TourModeChoiceModel.AT_WORK_SUBTOUR_MODEL_INDICATOR, dmuFactory,
                        logsumHelper);
                if (measureObjectSizes)
                    logger.info("AWMC size:       " + ObjectUtil.sizeOf(awmcModel));
            }

            if (runAtWorkSubtourLocationChoiceModel)
            {
                awlcModel = new SubtourDestChoiceModel(propertyMap, modelStructure, aggAcc,
                        dmuFactory, awmcModel);
                awlcModel.setNonMandatorySoaProbs(subTourSoaDistProbs, subTourSoaSizeProbs);
                if (measureObjectSizes)
                    logger.info("AWLC size:       " + ObjectUtil.sizeOf(awlcModel));
            }

            if (runAtWorkSubtourDepartureTimeAndDurationModel)
            {
                awtodModel = new SubtourDepartureAndDurationTime(propertyMap, modelStructure,
                        dmuFactory, awmcModel);
                if (measureObjectSizes)
                    logger.info("AWTOD size:      " + ObjectUtil.sizeOf(awtodModel));
            }

            if (runStopFrequencyModel)
            {
                stfModel = new StopFrequencyModel(propertyMap, dmuFactory, modelStructure, accTable);
                if (measureObjectSizes)
                    logger.info("STF size:        " + ObjectUtil.sizeOf(stfModel));
            }

            if (runStopLocationModel)
            {
                stlmcModel = new IntermediateStopChoiceModels(propertyMap, modelStructure,
                        dmuFactory, logsumHelper);
                
                mmModel = new MicromobilityChoiceModel(propertyMap,modelStructure,dmuFactory);

                // if the slcTazDistProbs are not null, they have been already
                // computed, and it is
                // not necessary for the thread creating this
                // HouseholdChoiceModels object to
                // compute them also. If slcTazDistProbs is null, compute them.
                if (useNewSlcSoaMethod && slcSizeProbs == null)
                {

                    // compute the array of cumulative taz distance based SOA
                    // probabilities for each origin taz.
                    DestChoiceTwoStageSoaTazDistanceUtilityDMU locChoiceDistSoaDmu = dmuFactory
                            .getDestChoiceSoaTwoStageTazDistUtilityDMU();

                    DestChoiceTwoStageSoaProbabilitiesCalculator slcSoaDistProbsObject = new DestChoiceTwoStageSoaProbabilitiesCalculator(
                            propertyMap,
                            dmuFactory,
                            IntermediateStopChoiceModels.PROPERTIES_UEC_SLC_SOA_DISTANCE_UTILITY,
                            IntermediateStopChoiceModels.PROPERTIES_UEC_SLC_SOA_DISTANCE_MODEL_PAGE,
                            IntermediateStopChoiceModels.PROPERTIES_UEC_SLC_SOA_DISTANCE_DATA_PAGE);

                    computeSlcSoaProbabilities(slcSoaDistProbsObject, locChoiceDistSoaDmu,
                            stlmcModel.getSizeSegmentNameIndexMap(),
                            stlmcModel.getSizeSegmentArray());

                    stlmcModel.setupSlcDistanceBaseSoaModel(propertyMap, slcTazDistExpUtils,
                            slcSizeProbs, slcTazSize);
                }

                if (measureObjectSizes)
                    logger.info("SLMT size:       " + ObjectUtil.sizeOf(stlmcModel));
            }

        } catch (RuntimeException e)
        {

            String lastModel = "";
            if (runAutoOwnershipModel && aoModel != null) lastModel += " ao";

            if (runParkingProvisionModel && ppModel != null) lastModel += " fp";

            if (runInternalExternalModel && ieModel != null) lastModel += " ie";

            if (runCoordinatedDailyActivityPatternModel && cdapModel != null) lastModel += " cdap";

            if (runIndividualMandatoryTourFrequencyModel && imtfModel != null)
                lastModel += " imtf";

            if (runMandatoryTourModeChoiceModel && immcModel != null) lastModel += " immc";

            if (runMandatoryTourDepartureTimeAndDurationModel && imtodModel != null)
                lastModel += " imtod";

            if (runJointTourFrequencyModel && jtfModel != null) lastModel += " jtf";

            if (runJointTourModeChoiceModel && nmmcModel != null) lastModel += " jmc";

            if (runJointTourLocationChoiceModel && nmlcModel != null) lastModel += " jlc";

            if (runJointTourDepartureTimeAndDurationModel && nmtodModel != null)
                lastModel += " jtod";

            if (runIndividualNonMandatoryTourFrequencyModel && inmtfModel != null)
                lastModel += " inmtf";

            if (runIndividualNonMandatoryTourModeChoiceModel && nmmcModel != null)
                lastModel += " inmmc";

            if (runIndividualNonMandatoryTourLocationChoiceModel && nmlcModel != null)
                lastModel += " inmlc";

            if (runIndividualNonMandatoryTourDepartureTimeAndDurationModel && nmtodModel != null)
                lastModel += " inmtod";

            if (runAtWorkSubTourFrequencyModel && awfModel != null) lastModel += " awf";

            if (runAtWorkSubtourModeChoiceModel && awmcModel != null) lastModel += " awmc";

            if (runAtWorkSubtourLocationChoiceModel && awlcModel != null) lastModel += " awlc";

            if (runAtWorkSubtourDepartureTimeAndDurationModel && awtodModel != null)
                lastModel += " awtod";

            if (runStopFrequencyModel && stfModel != null) lastModel += " stf";

            if (runStopLocationModel && stlmcModel != null) lastModel += " stlmc";

            logger.error("RuntimeException setting up HouseholdChoiceModels.");
            logger.error("Models setup = " + lastModel);
            logger.error("", e);

            throw new RuntimeException();
        }

    }

    public void runModels(Household hhObject)
    {

        // check to see if restartModel was set and reset random number sequence
        // appropriately if so.
        checkRestartModel(hhObject);

        if (runAutoOwnershipModel) aoModel.applyModel(hhObject, false);

        if (runTransponderModel) tcModel.applyModel(hhObject);

        if (runParkingProvisionModel) {
        	ppModel.applyModel(hhObject);
        	teModel.applyModel(hhObject);
        }

        if (runInternalExternalModel) ieModel.applyModel(hhObject, distanceToCordonsLogsums);

        if (runCoordinatedDailyActivityPatternModel) cdapModel.applyModel(hhObject);

        if (runIndividualMandatoryTourFrequencyModel) {
        	imtfModel.applyModel(hhObject);
            tvtcModel.applyModelToMandatoryTours(hhObject);
        }

        if (runMandatoryTourDepartureTimeAndDurationModel)
            imtodModel.applyModel(hhObject, runMandatoryTourModeChoiceModel);

        if(runEscortModel){
         	try {
				escortModel.applyModel(hhObject);
			} catch (Exception e) {
				logger.fatal("Error Attempting to run escort model for household "+hhObject.getHhId());
				throw new RuntimeException(e);
			}
        }
   		
        if (runJointTourFrequencyModel) {
        	jtfModel.applyModel(hhObject);
            tvtcModel.applyModelToJointTours(hhObject);
        }

        if (runJointTourLocationChoiceModel) nmlcModel.applyJointModel(hhObject);

        if (runJointTourDepartureTimeAndDurationModel)
            nmtodModel.applyJointModel(hhObject, runJointTourModeChoiceModel);

        if (runIndividualNonMandatoryTourFrequencyModel) {
        	inmtfModel.applyModel(hhObject);
            tvtcModel.applyModelToNonMandatoryTours(hhObject);
        }

        if (runIndividualNonMandatoryTourLocationChoiceModel) nmlcModel.applyIndivModel(hhObject);

        if (runIndividualNonMandatoryTourDepartureTimeAndDurationModel)
            nmtodModel.applyIndivModel(hhObject, runIndividualNonMandatoryTourModeChoiceModel);

        if (runAtWorkSubTourFrequencyModel) {
        	awfModel.applyModel(hhObject);
            tvtcModel.applyModelToAtWorkSubTours(hhObject);
        }

        if (runAtWorkSubtourLocationChoiceModel) awlcModel.applyModel(hhObject);

        if (runAtWorkSubtourDepartureTimeAndDurationModel)
            awtodModel.applyModel(hhObject, runAtWorkSubtourModeChoiceModel);

        if (runStopFrequencyModel) stfModel.applyModel(hhObject);

        if (runStopLocationModel) {
        	stlmcModel.applyModel(hhObject, false);
        	mmModel.applyModel(hhObject);
        }

    }

    public void runModelsWithTiming(Household hhObject)
    {

        // check to see if restartModel was set and reset random number sequence
        // appropriately if so.
        checkRestartModel(hhObject);

        if (runAutoOwnershipModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + AO_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            aoModel.applyModel(hhObject, false);
            aoTime += (System.nanoTime() - check);
        }

        if (runTransponderModel)
        {
            // long hhSeed = globalSeed + hhObject.getHhId() + TP_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            tcModel.applyModel(hhObject);
        }

        if (runParkingProvisionModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + PP_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            ppModel.applyModel(hhObject);
            teModel.applyModel(hhObject);
            fpTime += (System.nanoTime() - check);
        }

        if (runInternalExternalModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + PP_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            ieModel.applyModel(hhObject, distanceToCordonsLogsums);
            ieTime += (System.nanoTime() - check);
        }

        if (runCoordinatedDailyActivityPatternModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + CDAP_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            cdapModel.applyModel(hhObject);
            cdapTime += (System.nanoTime() - check);
        }

        if (runIndividualMandatoryTourFrequencyModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + IMTF_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            imtfModel.applyModel(hhObject);
            tvtcModel.applyModelToMandatoryTours(hhObject);
            imtfTime += (System.nanoTime() - check);
        }

        if (runMandatoryTourDepartureTimeAndDurationModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() +
            // IMTOD_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            imtodModel.applyModel(hhObject, runMandatoryTourModeChoiceModel);
            long mcTime = imtodModel.getModeChoiceTime();
            imtodTime += (System.nanoTime() - check - mcTime);
            imtmcTime += mcTime;
        }
        if(runEscortModel){
            long check = System.nanoTime();
        	try {
				escortModel.applyModel(hhObject);
			} catch (Exception e) {
				logger.fatal("Error Attempting to run escort model for household "+hhObject.getHhId());
				e.printStackTrace();
			}
            escortTime += ( System.nanoTime() - check );
        }       


        if (runJointTourFrequencyModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + JTF_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            jtfModel.applyModel(hhObject);
            tvtcModel.applyModelToJointTours(hhObject);
            jtfTime += (System.nanoTime() - check);
        }

        if (runJointTourLocationChoiceModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + JTDC_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            nmlcModel.applyJointModel(hhObject);
            jtdcTime += (System.nanoTime() - check);
        }

        if (runJointTourDepartureTimeAndDurationModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + JTOD_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            nmtodModel.applyJointModel(hhObject, runJointTourModeChoiceModel);
            long mcTime = nmtodModel.getJointModeChoiceTime();
            jtodTime += (System.nanoTime() - check - mcTime);
            jtmcTime += mcTime;
        }

        if (runIndividualNonMandatoryTourFrequencyModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() +
            // INMTF_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            inmtfModel.applyModel(hhObject);
            tvtcModel.applyModelToNonMandatoryTours(hhObject);
            inmtfTime += (System.nanoTime() - check);
        }

        if (runIndividualNonMandatoryTourLocationChoiceModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() +
            // INMDC_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            nmlcModel.resetSoaRunTime();
            nmlcModel.applyIndivModel(hhObject);
            inmtdcSoaTime += nmlcModel.getSoaRunTime();
            inmtdcTime += (System.nanoTime() - check);
        }

        if (runIndividualNonMandatoryTourDepartureTimeAndDurationModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() +
            // INMTOD_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            nmtodModel.applyIndivModel(hhObject, runIndividualNonMandatoryTourModeChoiceModel);
            long mcTime = nmtodModel.getIndivModeChoiceTime();
            inmtodTime += (System.nanoTime() - check - mcTime);
            inmtmcTime += mcTime;
        }

        if (runAtWorkSubTourFrequencyModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + AWTF_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            awfModel.applyModel(hhObject);
            tvtcModel.applyModelToAtWorkSubTours(hhObject);
            awtfTime += (System.nanoTime() - check);
        }

        if (runAtWorkSubtourLocationChoiceModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + AWDC_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            awlcModel.applyModel(hhObject);
            awtdcSoaTime += awlcModel.getSoaRunTime();
            awtdcTime += (System.nanoTime() - check);
        }

        if (runAtWorkSubtourDepartureTimeAndDurationModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() +
            // AWTOD_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            awtodModel.applyModel(hhObject, runAtWorkSubtourModeChoiceModel);
            long mcTime = awtodModel.getModeChoiceTime();
            awtodTime += (System.nanoTime() - check - mcTime);
            awtmcTime += mcTime;
        }

        if (runStopFrequencyModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + STF_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            stfModel.applyModel(hhObject);
            stfTime += (System.nanoTime() - check);
        }

        if (runStopLocationModel)
        {
            long check = System.nanoTime();
            // long hhSeed = globalSeed + hhObject.getHhId() + SLC_SEED_OFFSET;
            // hhObject.getHhRandom().setSeed( hhSeed );
            stlmcModel.applyModel(hhObject, true);
            stdtmTime += (System.nanoTime() - check);

            long[] partials = stlmcModel.getStopTimes();
            for (int i = 0; i < returnPartialTimes.length; i++)
                returnPartialTimes[i] += partials[i];

            if (stlmcModel.getMaxAltsInSample() > maxAlts)
                maxAlts = stlmcModel.getMaxAltsInSample();
            
            mmModel.applyModel(hhObject);
        }

    }

    private void checkRestartModel(Household hhObject)
    {

        // none, ao, cdap, imtf, imtod, awf, awl, awtod, jtf, jtl, jtod, inmtf,
        // inmtl, inmtod, stf, stl
        // version 1.0.8.22 - changed model restart options - possible values
        // for
        // restart are now: none, uwsl, ao, imtf, jtf, inmtf, stf

        // if restartModel was specified, reset the random number sequence
        // based on the cumulative count of random numbers drawn by the
        // component
        // preceding the one specified.
        if (restartModelString.equalsIgnoreCase("") || restartModelString.equalsIgnoreCase("none")) return;
        else if (restartModelString.equalsIgnoreCase("ao"))
        {
            hhObject.initializeForAoRestart();
        } else if (restartModelString.equalsIgnoreCase("imtf"))
        {
            hhObject.initializeForImtfRestart();
        } else if (restartModelString.equalsIgnoreCase("jtf"))
        {
            hhObject.initializeForJtfRestart();
        } else if (restartModelString.equalsIgnoreCase("inmtf"))
        {
            hhObject.initializeForInmtfRestart();
        } else if (restartModelString.equalsIgnoreCase("awf"))
        {
            hhObject.initializeForAwfRestart();
        } else if (restartModelString.equalsIgnoreCase("stf"))
        {
            hhObject.initializeForStfRestart();
        }

    }

    public int getModelIndex()
    {
        return modelIndex;
    }

    public void zeroTimes()
    {
        aoTime = 0;
        fpTime = 0;
        ieTime = 0;
        cdapTime = 0;
        imtfTime = 0;
        imtodTime = 0;
        imtmcTime = 0;
        jtfTime = 0;
        jtdcTime = 0;
        jtodTime = 0;
        jtmcTime = 0;
        inmtfTime = 0;
        inmtdcTime = 0;
        inmtdcSoaTime = 0;
        inmtodTime = 0;
        inmtmcTime = 0;
        awtfTime = 0;
        awtdcTime = 0;
        awtdcSoaTime = 0;
        awtodTime = 0;
        awtmcTime = 0;
        stfTime = 0;
        stdtmTime = 0;

        Arrays.fill(returnPartialTimes, 0);
    }

    public long[] getPartialStopTimes()
    {
        return returnPartialTimes;
    }

    public long[] getTimes()
    {
        long[] returnTimes = new long[23];
        returnTimes[0] = aoTime;
        returnTimes[1] = fpTime;
        returnTimes[2] = ieTime;
        returnTimes[3] = cdapTime;
        returnTimes[4] = imtfTime;
        returnTimes[5] = imtodTime;
        returnTimes[6] = imtmcTime;
        returnTimes[7] = jtfTime;
        returnTimes[8] = jtdcTime;
        returnTimes[9] = jtodTime;
        returnTimes[10] = jtmcTime;
        returnTimes[11] = inmtfTime;
        returnTimes[12] = inmtdcSoaTime;
        returnTimes[13] = inmtdcTime;
        returnTimes[14] = inmtodTime;
        returnTimes[15] = inmtmcTime;
        returnTimes[16] = awtfTime;
        returnTimes[17] = awtdcSoaTime;
        returnTimes[18] = awtdcTime;
        returnTimes[19] = awtodTime;
        returnTimes[20] = awtmcTime;
        returnTimes[21] = stfTime;
        returnTimes[22] = stdtmTime;
        return returnTimes;
    }

    public int getMaxAlts()
    {
        return maxAlts;
    }

    private void computeSlcSoaProbabilities(
            DestChoiceTwoStageSoaProbabilitiesCalculator locChoiceSoaDistProbsObject,
            DestChoiceTwoStageSoaTazDistanceUtilityDMU locChoiceDistSoaDmu,
            HashMap<String, Integer> segmentNameIndexMap, double[][] dcSizeArray)
    {

        // compute the exponentiated distance utilities that all segments of
        // this tour purpose will share
        slcTazDistExpUtils = computeTazDistanceExponentiatedUtilities(locChoiceSoaDistProbsObject,
                locChoiceDistSoaDmu);

        slcTazSize = new double[dcSizeArray.length][];
        slcSizeProbs = new double[dcSizeArray.length][][];

        // compute an array of SOA size probabilities for each segment
        for (String segmentName : segmentNameIndexMap.keySet())
        {

            // compute the TAZ size values from the mgra values and the
            // correspondence between mgras and tazs.
            int segmentIndex = segmentNameIndexMap.get(segmentName);
            slcTazSize[segmentIndex] = computeTazSize(dcSizeArray[segmentIndex]);

            slcSizeProbs[segmentIndex] = computeSizeSegmentProbabilities(dcSizeArray[segmentIndex],
                    slcTazSize[segmentIndex]);

        }

    }

    private double[][] computeSizeSegmentProbabilities(double[] size, double[] totalTazSize)
    {

        int maxTaz = tdm.getMaxTaz();

        // this is a 0-based array of cumulative probabilities
        double[][] sizeProbs = new double[maxTaz][];

        for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
        {

            int[] mgraArray = tdm.getMgraArray(taz);

            if (mgraArray == null)
            {
                sizeProbs[taz - 1] = new double[0];
            } else
            {

                if (totalTazSize[taz] > 0)
                {
                    sizeProbs[taz - 1] = new double[mgraArray.length];
                    for (int i = 0; i < mgraArray.length; i++)
                    {
                        double mgraSize = size[mgraArray[i]];
                        if (mgraSize > 0) mgraSize += 1;
                        sizeProbs[taz - 1][i] = mgraSize / totalTazSize[taz];
                    }
                } else
                {
                    sizeProbs[taz - 1] = new double[0];
                }
            }

        }

        return sizeProbs;

    }

    private double[][] computeTazDistanceExponentiatedUtilities(
            DestChoiceTwoStageSoaProbabilitiesCalculator locChoiceSoaDistProbsObject,
            DestChoiceTwoStageSoaTazDistanceUtilityDMU locChoiceDistSoaDmu)
    {

        // compute the TAZ x TAZ exponentiated utilities array for sample
        // selection utilities.
        double[][] tazDistExpUtils = locChoiceSoaDistProbsObject
                .computeDistanceUtilities(locChoiceDistSoaDmu);
        for (int i = 0; i < tazDistExpUtils.length; i++)
            for (int j = 0; j < tazDistExpUtils[i].length; j++)
            {
                if (tazDistExpUtils[i][j] < -500) tazDistExpUtils[i][j] = 0;
                else tazDistExpUtils[i][j] = Math.exp(tazDistExpUtils[i][j]);
            }

        return tazDistExpUtils;

    }

    private double[] computeTazSize(double[] size)
    {

        int maxTaz = tdm.getMaxTaz();

        double[] tazSize = new double[maxTaz + 1];

        for (int taz = 1; taz <= tdm.getMaxTaz(); taz++)
        {

            int[] mgraArray = tdm.getMgraArray(taz);
            if (mgraArray != null)
            {
                for (int mgra : mgraArray)
                {
                    tazSize[taz] += size[mgra] + (size[mgra] > 0 ? 1 : 0);
                }
            }

        }

        return tazSize;

    }

}
