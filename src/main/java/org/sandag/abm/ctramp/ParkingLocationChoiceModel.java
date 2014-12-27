package org.sandag.abm.ctramp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.newmodel.ChoiceModelApplication;

/**
 * This class is a Parking Location Choice Model. It was originally part of the intermediate stop
 * choice model class but was separated into a separate class to provide a more encapsulated
 * and easily-maintainable design.
 * 
 * @author Freedman
 *
 */
public class ParkingLocationChoiceModel {

    private transient Logger                   parkLocLogger                                       = Logger.getLogger("parkLocLog");
    private static final String                PROPERTIES_UEC_PARKING_LOCATION_CHOICE_ALTERNATIVES = "plc.alts.corresp.file";
    private static final String                PROPERTIES_UEC_PARKING_LOCATION_CHOICE              = "plc.uec.file";
    private static final String                PROPERTIES_UEC_PLC_DATA_PAGE                        = "plc.uec.data.page";
    private static final String                PROPERTIES_UEC_PLC_MODEL_PAGE                       = "plc.uec.model.page";

    private ParkingChoiceDMU                   parkingChoiceDmuObj;
    private TableDataSet                       plcAltsTable;
    private int[]                              parkMgras;
    private int[]                              parkAreas;

    private int[]                              altMgraIndices;
    private double[]                           altOsDistances;
    private double[]                           altSdDistances;
    private boolean[]                          altParkAvail;
    private int[]                              altParkSample;

    private static final String                PARK_MGRA_COLUMN                                    = "mgra";
    private static final String                PARK_AREA_COLUMN                                    = "parkarea";
    private static final int                   MAX_PLC_SAMPLE_SIZE                                 = 620;
    private ChoiceModelApplication             plcModel;
    
    private int[]                              numfreehrs;
    private int[]                              hstallsoth;
    private int[]                              hstallssam;
    private float[]                            hparkcost;
    private int[]                              dstallsoth;
    private int[]                              dstallssam;
    private float[]                            dparkcost;
    private int[]                              mstallsoth;
    private int[]                              mstallssam;
    private float[]                            mparkcost;
    private HashMap<Integer, Integer>          mgraAltLocationIndex;
    private HashMap<Integer, Integer>          mgraAltParkArea;
    private double[]                           altParkingCostsM;
    private double[]                           altParkingCostsD;
    private double[]                           altParkingCostsH;
    private int[]                              altMstallsoth;
    private int[]                              altMstallssam;
    private float[]                            altMparkcost;
    private int[]                              altDstallsoth;
    private int[]                              altDstallssam;
    private float[]                            altDparkcost;
    private int[]                              altHstallsoth;
    private int[]                              altHstallssam;
    private float[]                            altHparkcost;
    private int[]                              altNumfreehrs;
    private double[]                           lsWgtAvgCostM;
    private double[]                           lsWgtAvgCostD;
    private double[]                           lsWgtAvgCostH;
    private int                                numAltsInSample;
    private int                                maxAltsInSample;
    private ModelStructure                     modelStructure;
    private MgraDataManager                    mgraManager;
    
    public ParkingLocationChoiceModel(HashMap<String, String> propertyMap,ModelStructure myModelStructure, CtrampDmuFactoryIf dmuFactory){
    
        mgraManager = MgraDataManager.getInstance(propertyMap);
        modelStructure = myModelStructure;
        
        numfreehrs = mgraManager.getNumFreeHours();
         mstallsoth = mgraManager.getMStallsOth();
        mstallssam = mgraManager.getMStallsSam();
        mparkcost = mgraManager.getMParkCost();
        dstallsoth = mgraManager.getDStallsOth();
        dstallssam = mgraManager.getDStallsSam();
        dparkcost = mgraManager.getDParkCost();
        hstallsoth = mgraManager.getHStallsOth();
        hstallssam = mgraManager.getHStallsSam();
        hparkcost = mgraManager.getHParkCost();
        
        setup(propertyMap, dmuFactory);

    }
    

    private void setup(HashMap<String, String> propertyMap,
            CtrampDmuFactoryIf dmuFactory)
    {

    	parkLocLogger.info("setting up parking location choice models.");

        // locate the UEC
        String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        String plcUecFile = propertyMap.get(PROPERTIES_UEC_PARKING_LOCATION_CHOICE);
        plcUecFile = uecPath + plcUecFile;

        int plcDataPage = Integer.parseInt(propertyMap.get(PROPERTIES_UEC_PLC_DATA_PAGE));
        int plcModelPage = Integer.parseInt(propertyMap.get(PROPERTIES_UEC_PLC_MODEL_PAGE));

        altMgraIndices = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altOsDistances = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altSdDistances = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsM = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsD = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altParkingCostsH = new double[MAX_PLC_SAMPLE_SIZE + 1];
        altMstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altMstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altMparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altDstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altDstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altDparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altHstallsoth = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altHstallssam = new int[MAX_PLC_SAMPLE_SIZE + 1];
        altHparkcost = new float[MAX_PLC_SAMPLE_SIZE + 1];
        altNumfreehrs = new int[MAX_PLC_SAMPLE_SIZE + 1];

        altParkAvail = new boolean[MAX_PLC_SAMPLE_SIZE + 1];
        altParkSample = new int[MAX_PLC_SAMPLE_SIZE + 1];

        parkingChoiceDmuObj = dmuFactory.getParkingChoiceDMU();

        plcModel = new ChoiceModelApplication(plcUecFile, plcModelPage, plcDataPage, propertyMap,
                (VariableTable) parkingChoiceDmuObj);

        // read the parking choice alternatives data file to get alternatives
        // names
        String plcAltsFile = propertyMap.get(PROPERTIES_UEC_PARKING_LOCATION_CHOICE_ALTERNATIVES);
        plcAltsFile = uecPath + plcAltsFile;

        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            plcAltsTable = reader.readFile(new File(plcAltsFile));
        } catch (IOException e)
        {
        	parkLocLogger.error("problem reading table of cbd zones for parking location choice model.", e);
            System.exit(1);
        }

        parkMgras = plcAltsTable.getColumnAsInt(PARK_MGRA_COLUMN);
        parkAreas = plcAltsTable.getColumnAsInt(PARK_AREA_COLUMN);

        parkingChoiceDmuObj.setParkAreaMgraArray(parkMgras);
        parkingChoiceDmuObj.setSampleIndicesArray(altMgraIndices);
        parkingChoiceDmuObj.setDistancesOrigAlt(altOsDistances);
        parkingChoiceDmuObj.setDistancesAltDest(altSdDistances);
        parkingChoiceDmuObj.setParkingCostsM(altParkingCostsM);
        parkingChoiceDmuObj.setMstallsoth(altMstallsoth);
        parkingChoiceDmuObj.setMstallssam(altMstallssam);
        parkingChoiceDmuObj.setMparkCost(altMparkcost);
        parkingChoiceDmuObj.setDstallsoth(altDstallsoth);
        parkingChoiceDmuObj.setDstallssam(altDstallssam);
        parkingChoiceDmuObj.setDparkCost(altDparkcost);
        parkingChoiceDmuObj.setHstallsoth(altHstallsoth);
        parkingChoiceDmuObj.setHstallssam(altHstallssam);
        parkingChoiceDmuObj.setHparkCost(altHparkcost);
        parkingChoiceDmuObj.setNumfreehrs(altNumfreehrs);

        mgraAltLocationIndex = new HashMap<Integer, Integer>();
        mgraAltParkArea = new HashMap<Integer, Integer>();

        for (int i = 0; i < parkMgras.length; i++)
        {
            mgraAltLocationIndex.put(parkMgras[i], i);
            mgraAltParkArea.put(parkMgras[i], parkAreas[i]);
        }

    }
    
    /**
     * This method is called to determine the parking mgra location if the stop
     * location is in parkarea 1 and chosen mode is sov or hov.
     * 
     * @param household
     * @param tour
     * @param stop
     * @return The parking MGRA.
     */
    public int selectParkingLocation(Household household, Tour tour, Stop stop)
    {

        Logger modelLogger = parkLocLogger;

        // if the trip destination mgra is not in parking area 1, it's not
        // necessary to make a parking location choice
        if (mgraAltLocationIndex.containsKey(stop.getDest()) == false
                || mgraAltParkArea.get(stop.getDest()) != 1) return -1;

        // if person worked at home, no reason to make a parking location choice
        if (tour.getPersonObject().getFreeParkingAvailableResult() == ParkingProvisionModel.FP_MODEL_NO_REIMBURSEMENT_CHOICE)
            return -1;

        // if the person has free parking, set the parking location
        if (tour.getPersonObject().getFreeParkingAvailableResult() == 1) return stop.getDest();

        parkingChoiceDmuObj.setDmuIndexValues(household.getHhId(), stop.getOrig(), stop.getDest(),
                household.getDebugChoiceModels());

        parkingChoiceDmuObj.setPersonType(tour.getPersonObject().getPersonTypeNumber());

        Stop[] stops = null;
        if (stop.isInboundStop()) stops = tour.getInboundStops();
        else stops = tour.getOutboundStops();

        // determine activity duration in number od departure time intervals
        // if no stops on halftour, activity duration is tour duration
        int activityIntervals = 0;
        if (stops.length == 1)
        {
            activityIntervals = tour.getTourArrivePeriod() - tour.getTourDepartPeriod();
        } else
        {
            int stopId = stop.getStopId();
            if (stopId == stops.length - 1) activityIntervals = tour.getTourArrivePeriod()
                    - stop.getStopPeriod();
            else activityIntervals = stops[stopId + 1].getStopPeriod() - stop.getStopPeriod();
        }

        parkingChoiceDmuObj.setActivityIntervals(activityIntervals);

        parkingChoiceDmuObj.setDestPurpose(stop.getStopPurposeIndex());

        parkingChoiceDmuObj.setReimbPct(tour.getPersonObject().getParkingReimbursement());

        int[] sampleIndices = setupParkLocationChoiceAlternativeArrays(stop.getOrig(),
                stop.getDest());

        // if no alternatives in the sample, it's not necessary to make a
        // parking location choice
        if (sampleIndices == null) return -1;

        if (household.getDebugChoiceModels())
        {
            household.logHouseholdObject(
                    "Pre Parking Location Choice for trip: HH_" + household.getHhId() + ", Pers_"
                            + tour.getPersonObject().getPersonNum() + ", Tour Purpose_"
                            + tour.getTourPurpose() + ", Tour_" + tour.getTourId()
                            + ", Tour Purpose_" + tour.getTourPurpose() + ", Stop_"
                            + stop.getStopId(), modelLogger);
            household.logPersonObject("Pre Parking Location Choice for person "
                    + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject());
            household.logTourObject("Pre Parking Location Choice for tour " + tour.getTourId(),
                    modelLogger, tour.getPersonObject(), tour);
            household.logStopObject("Pre Parking Location Choice for stop " + stop.getStopId(),
                    modelLogger, stop, modelStructure);
        }

        Person person = tour.getPersonObject();

        String choiceModelDescription = "";
        String separator = "";
        String loggerString = "";
        String decisionMakerLabel = "";

        // log headers to traceLogger if the person making the destination
        // choice is from a household requesting trace information
        if (household.getDebugChoiceModels())
        {

            choiceModelDescription = "Parking Location Choice Model for trip";
            decisionMakerLabel = String
                    .format("HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopPurpose=%s, StopId=%d",
                            household.getHhId(), person.getPersonNum(), person.getPersonType(),
                            tour.getTourPurpose(), tour.getTourId(), tour.getTourPurpose(),
                            stop.getStopId());

            modelLogger.info(" ");
            loggerString = choiceModelDescription + " for " + decisionMakerLabel + ".";
            for (int k = 0; k < loggerString.length(); k++)
                separator += "+";
            modelLogger.info(loggerString);
            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            plcModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription,
                    decisionMakerLabel);

        }

        plcModel.computeUtilities(parkingChoiceDmuObj, parkingChoiceDmuObj.getDmuIndexValues(),
                altParkAvail, altParkSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make
        // choice.
        int chosen = -1;
        int chosenIndex = -1;
        int parkMgra = 0;
        if (plcModel.getAvailabilityCount() > 0)
        {
            // get the mgra number associated with the chosen alternative
            chosen = plcModel.getChoiceResult(rn);
            // sampleIndices is 1-based, but the values returned are 0-based,
            // parkMgras is 0-based
            chosenIndex = sampleIndices[chosen];
            parkMgra = parkMgras[chosenIndex];
        }

        // write choice model alternative info to log file
        if (household.getDebugChoiceModels() || chosen < 0)
        {

            double[] utilities = plcModel.getUtilities();
            double[] probabilities = plcModel.getProbabilities();

            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString);
            modelLogger
                    .info("Alternative                                   Utility       Probability           CumProb");
            modelLogger
                    .info("--------------------                   --------------    --------------    --------------");

            double cumProb = 0.0;

            for (int k = 1; k <= numAltsInSample; k++)
            {
                int index = sampleIndices[k];
                int altMgra = parkMgras[index];
                cumProb += probabilities[k - 1];
                String altString = String.format("k=%d, index=%d, altMgra=%d", k, index, altMgra);
                modelLogger.info(String.format("%-35s%18.6e%18.6e%18.6e", altString,
                        utilities[k - 1], probabilities[k - 1], cumProb));
            }

            modelLogger.info(" ");
            if (chosen < 0)
            {
                modelLogger.info(String.format("No Alternatives Available For Choice !!!"));
            } else
            {
                String altString = String.format("chosen=%d, chosenIndex=%d, chosenMgra=%d",
                        chosen, chosenIndex, parkMgra);
                modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));
            }

            modelLogger.info(separator);
            modelLogger.info("");
            modelLogger.info("");

            plcModel.logAlternativesInfo(choiceModelDescription, decisionMakerLabel);
            plcModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel, rn, chosen);

            // write UEC calculation results to separate model specific log file
            plcModel.logUECResults(modelLogger, loggerString);

        }

        if (chosen > 0) return parkMgra;
        else
        {
        	parkLocLogger.error(String
                    .format("Exception caught for HHID=%d, personNum=%d, no available parking location alternatives in tourId=%d to choose from in plcModelApplication.",
                            household.getHhId(), person.getPersonNum(), tour.getTourId()));
            throw new RuntimeException();
        }

    }

    
    /**
     * This method is called for trips that require a park location choice --
     * trip destination in parkarea 1 and not a work trip with free onsite
     * parking
     * return false if no parking location alternatives are in walk distance of
     * trip destination; true otherwise.
     
     * @param tripOrigMgra
     * @param tripDestMgra
     * @return
     */
    private int[] setupParkLocationChoiceAlternativeArrays(int tripOrigMgra, int tripDestMgra)
    {

        // get the array of mgras within walking distance of the trip
        // destination
        int[] walkMgras = mgraManager.getMgrasWithinWalkDistanceTo(tripDestMgra);

        // set the distance values for the mgras walkable to the destination
        if (walkMgras != null)
        {

            // get distances, in feet, and convert to miles
            // get distances from destMgra since this is the direction of
            // distances read from the data file
            int altCount = 0;
            for (int wMgra : walkMgras)
            {
                // if wMgra is in the set of parkarea==1 MGRAs, add to list of
                // alternatives for this park location choice
                if (mgraAltLocationIndex.containsKey(wMgra))
                {

                    double curWalkDist = mgraManager.getMgraToMgraWalkDistTo(wMgra, tripDestMgra) / 5280.0;

                    if (curWalkDist > MgraDataManager.MAX_PARKING_WALK_DISTANCE) continue;

                    // the hashMap stores a 0-based index
                    int altIndex = mgraAltLocationIndex.get(wMgra);
                    int m = wMgra - 1;

                    altSdDistances[altCount + 1] = curWalkDist;
                    altMgraIndices[altCount + 1] = altIndex;

                    altParkingCostsM[altCount + 1] = lsWgtAvgCostM[m];
                    altParkingCostsD[altCount + 1] = lsWgtAvgCostD[m];
                    altParkingCostsH[altCount + 1] = lsWgtAvgCostH[m];
                    altMstallsoth[altCount + 1] = mstallsoth[m];
                    altMstallssam[altCount + 1] = mstallssam[m];
                    altMparkcost[altCount + 1] = mparkcost[m];
                    altDstallsoth[altCount + 1] = dstallsoth[m];
                    altDstallssam[altCount + 1] = dstallssam[m];
                    altDparkcost[altCount + 1] = dparkcost[m];
                    altHstallsoth[altCount + 1] = hstallsoth[m];
                    altHstallssam[altCount + 1] = hstallssam[m];
                    altHparkcost[altCount + 1] = hparkcost[m];
                    altNumfreehrs[altCount + 1] = numfreehrs[m];

                    altParkAvail[altCount + 1] = true;
                    altParkSample[altCount + 1] = 1;

                    altCount++;
                }
            }

            if (altCount > 0)
            {

                for (int i = altCount; i < MAX_PLC_SAMPLE_SIZE; i++)
                {
                    altOsDistances[i + 1] = Double.NaN;
                    altSdDistances[i + 1] = Double.NaN;
                    altMgraIndices[i + 1] = Integer.MAX_VALUE;

                    altParkingCostsM[i + 1] = Double.NaN;
                    altParkingCostsD[i + 1] = Double.NaN;
                    altParkingCostsH[i + 1] = Double.NaN;
                    altMstallsoth[i + 1] = Integer.MAX_VALUE;
                    altMstallssam[i + 1] = Integer.MAX_VALUE;
                    altMparkcost[i + 1] = Float.MAX_VALUE;
                    altDstallsoth[i + 1] = Integer.MAX_VALUE;
                    altDstallssam[i + 1] = Integer.MAX_VALUE;
                    altDparkcost[i + 1] = Float.MAX_VALUE;
                    altHstallsoth[i + 1] = Integer.MAX_VALUE;
                    altHstallssam[i + 1] = Integer.MAX_VALUE;
                    altHparkcost[i + 1] = Float.MAX_VALUE;
                    altNumfreehrs[i + 1] = Integer.MAX_VALUE;

                    altParkAvail[i + 1] = false;
                    altParkSample[i + 1] = 0;
                }
                numAltsInSample = altCount;
                if (numAltsInSample > maxAltsInSample) maxAltsInSample = numAltsInSample;
            }

            return altMgraIndices;

        }

        return null;

    }

    public int getMaxAltsInSample()
    {
        return maxAltsInSample;
    }

}
