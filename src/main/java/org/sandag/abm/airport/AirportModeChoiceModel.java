package org.sandag.abm.airport;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.McLogsumsAppender;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.Tracer;

public class AirportModeChoiceModel
{
    private transient Logger                  logger = Logger.getLogger("airportModel");

    private TazDataManager                    tazManager;
    private MgraDataManager                   mgraManager;

    private ChoiceModelApplication            driveAloneModel;
    private ChoiceModelApplication            shared2Model;
    private ChoiceModelApplication            shared3Model;
    private ChoiceModelApplication            transitModel;
    private ChoiceModelApplication            accessModel;

    private Tracer                            tracer;
    private boolean                           trace;
    private int[]                             traceOtaz;
    private int[]                             traceDtaz;
    private boolean                           seek;
    private HashMap<String, String>           rbMap;

    private McLogsumsCalculator      logsumHelper;
    private TripModeChoiceDMU 		 mcDmuObject;
    private AutoTazSkimsCalculator   tazDistanceCalculator;

    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of airport model DMUs
     */
    public AirportModeChoiceModel(HashMap<String, String> rbMap, AirportDmuFactoryIf dmuFactory)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String airportModeUecFileName = Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.uec.file");
        airportModeUecFileName = uecFileDirectory + airportModeUecFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.data.page"));
        int daPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.da.page"));
        int s2Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.s2.page"));
        int s3Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.s3.page"));
        int transitPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.transit.page"));
        int accessPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.mc.accessMode.page"));

        logger.info("Creating Airport Model Mode Choice Application UECs");

        // create a DMU
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();

        // create a ChoiceModelApplication object for drive-alone mode choice
        driveAloneModel = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                rbMap, (VariableTable) dmu);

        // create a ChoiceModelApplication object for shared 2 mode choice
        shared2Model = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage, rbMap,
                (VariableTable) dmu);

        // create a ChoiceModelApplication object for shared 3+ mode choice
        shared3Model = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage, rbMap,
                (VariableTable) dmu);

        // create a ChoiceModelApplication object for transit mode choice
        transitModel = new ChoiceModelApplication(airportModeUecFileName, transitPage, dataPage,
                rbMap, (VariableTable) dmu);

        // create a ChoiceModelApplication object for access mode choice
        accessModel = new ChoiceModelApplication(airportModeUecFileName, accessPage, dataPage,
                rbMap, (VariableTable) dmu);

        logger.info("Finished Creating Airport Model Mode Choice Application UECs");

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
        
        tazDistanceCalculator = new AutoTazSkimsCalculator(rbMap);
        tazDistanceCalculator.computeTazDistanceArrays();
        
        logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(rbMap);
        logsumHelper.setTazDistanceSkimArrays(
                tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims(),
                tazDistanceCalculator.getStoredToTazFromAllTazsDistanceSkims());
        
        SandagModelStructure modelStructure = new SandagModelStructure();
        mcDmuObject = new TripModeChoiceDMU(modelStructure, logger);

    }


    /**
     * Choose airport arrival mode and trip mode for this party. Store results
     * in the party object passed as argument.
     * 
     * @param party
     *            The travel party
     * @param dmu
     *            An airport model DMU
     */
    public void chooseMode(AirportParty party, AirportModelDMU dmu)
    {

        int origMgra = party.getOriginMGRA();
        int destMgra = party.getDestinationMGRA();
        int origTaz = mgraManager.getTaz(origMgra);
        int destTaz = mgraManager.getTaz(destMgra);
        int period = party.getDepartTime();

        boolean inbound = false;
        if (party.getDirection() == AirportModelStructure.ARRIVAL) inbound = true;

        dmu.setAirportParty(party);
        dmu.setDmuIndexValues(party.getID(), origTaz, destTaz);
       
        // set trip mc dmu values for transit logsum (gets replaced below by uec values)
        double c_ivt = -0.03;
        double c_cost = - 0.0003; 

        // Solve trip mode level utilities
        mcDmuObject.setIvtCoeff(c_ivt);
        mcDmuObject.setCostCoeff(c_cost);
        double walkTransitLogsum = -999.0;
        double driveTransitLogsum = -999.0;
        
        // if 1-person party, solve for the drive-alone and 2-person logsums
        if (party.getSize() == 1)
        {
            driveAloneModel.computeUtilities(dmu, dmu.getDmuIndex());
            double driveAloneLogsum = driveAloneModel.getLogsum();
            dmu.setDriveAloneLogsum(driveAloneLogsum);
            
            c_ivt = driveAloneModel.getUEC().lookupVariableIndex("c_ivt");
            c_cost = driveAloneModel.getUEC().lookupVariableIndex("c_cost");
 
            shared2Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared2Logsum = shared2Model.getLogsum();
            dmu.setShared2Logsum(shared2Logsum);

        } else if (party.getSize() == 2)
        { // if 2-person party solve for the
          // shared 2 and shared 3+ logsums
            shared2Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared2Logsum = shared2Model.getLogsum();
            dmu.setShared2Logsum(shared2Logsum);

            shared3Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared3Logsum = shared3Model.getLogsum();
            dmu.setShared3Logsum(shared3Logsum);
           
            c_ivt = shared2Model.getUEC().lookupVariableIndex("c_ivt");
            c_cost = shared2Model.getUEC().lookupVariableIndex("c_cost");
 
        } else
        { // if 3+ person party, solve the shared 3+ logsums
            shared3Model.computeUtilities(dmu, dmu.getDmuIndex());
            double shared3Logsum = shared3Model.getLogsum();
            dmu.setShared3Logsum(shared3Logsum);
 
            c_ivt = shared3Model.getUEC().lookupVariableIndex("c_ivt");
            c_cost = shared3Model.getUEC().lookupVariableIndex("c_cost");
        }
        
        logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, origMgra, destMgra, period, party.getDebugChoiceModels());
        walkTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.WTW);
        if (party.getDirection() == AirportModelStructure.DEPARTURE)
        {
            logsumHelper.setDtwTripMcDmuAttributes( mcDmuObject, origMgra, destMgra, period, party.getDebugChoiceModels());
            driveTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.DTW);
        } else
        {
        	logsumHelper.setWtdTripMcDmuAttributes( mcDmuObject, origMgra, destMgra, period, party.getDebugChoiceModels());
            driveTransitLogsum = mcDmuObject.getTransitLogSum(McLogsumsCalculator.WTD);
        }
      
        dmu.setWalkTransitLogsum(walkTransitLogsum);
        dmu.setDriveTransitLogsum(driveTransitLogsum);
        
        transitModel.computeUtilities(dmu, dmu.getDmuIndex());
        double transitLogsum = transitModel.getLogsum();
        dmu.setTransitLogsum(transitLogsum);

        // calculate access mode utility and choose access mode
        accessModel.computeUtilities(dmu, dmu.getDmuIndex());
        int accessMode = accessModel.getChoiceResult(party.getRandom());
        party.setArrivalMode((byte) accessMode);

        // choose trip mode
        int tripMode = 0;
        int occupancy = AirportModelStructure.getOccupancy(accessMode, party.getSize());
        double randomNumber = party.getRandom();
        
        float valueOfTime = 0;

        if (accessMode != AirportModelStructure.TRANSIT)
        {
            if (occupancy == 1)
            {
                int choice = driveAloneModel.getChoiceResult(randomNumber);
                tripMode = choice;
               
                //following gets vot from UEC
                UtilityExpressionCalculator uec = driveAloneModel.getUEC();
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            } else if (occupancy == 2)
            {
                int choice = shared2Model.getChoiceResult(randomNumber);
                tripMode = choice + 2;

                //following gets vot from UEC
                UtilityExpressionCalculator uec = shared2Model.getUEC();
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            } else if (occupancy > 2)
            {
                int choice = shared3Model.getChoiceResult(randomNumber);
                tripMode = choice + 2 + 2;
                
                //following gets vot from UEC
                UtilityExpressionCalculator uec = shared3Model.getUEC();
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            }
        } else
        {
            int choice = transitModel.getChoiceResult(randomNumber);
            double[][] bestTapPairs;
            if (choice == 1){
            	tripMode = 9; //walk-transit
            	bestTapPairs = logsumHelper.getBestWtwTripTaps();
            }
            else{
            	tripMode = 11; //knr-transit
                if (party.getDirection() == AirportModelStructure.DEPARTURE)
                	bestTapPairs = logsumHelper.getBestDtwTripTaps();
                else
                	bestTapPairs = logsumHelper.getBestWtdTripTaps();
             }
            
           	//pick transit path from N-paths
            float rn = new Double(party.getRandom()).floatValue();
        	int pathIndex = logsumHelper.chooseTripPath(rn, bestTapPairs, party.getDebugChoiceModels(), logger);
        	int boardTap = (int) bestTapPairs[pathIndex][0];
        	int alightTap = (int) bestTapPairs[pathIndex][1];
        	int set = (int) bestTapPairs[pathIndex][2];
        	party.setBoardTap(boardTap);
        	party.setAlightTap(alightTap);
        	party.setSet(set);
         	        			
        	//following gets vot from UEC
            UtilityExpressionCalculator uec = transitModel.getUEC();
            int votIndex = uec.lookupVariableIndex("vot");
            valueOfTime = (float) uec.getValueForIndex(votIndex);

        }
        party.setMode((byte) tripMode);
        party.setValueOfTime(valueOfTime);
    }

    /**
     * Choose modes for internal trips.
     * 
     * @param airportParties
     *            An array of travel parties, with destinations already chosen.
     * @param dmuFactory
     *            A DMU Factory.
     */
    public void chooseModes(AirportParty[] airportParties, AirportDmuFactoryIf dmuFactory)
    {

        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        // iterate through the array, choosing mgras and setting them
        for (AirportParty party : airportParties)
        {

            int ID = party.getID();

            if ((ID <= 5) || (ID % 100) == 0)
                logger.info("Choosing mode for party " + party.getID());

            if (party.getPurpose() < AirportModelStructure.INTERNAL_PURPOSES) chooseMode(party, dmu);
            else
            {
                party.setArrivalMode((byte) -99);
                party.setMode((byte) -99);
            }
        }
    }

  
}
