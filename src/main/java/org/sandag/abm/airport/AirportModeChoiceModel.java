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
    
    private ChoiceModelApplication[]          driveAloneModelArray;
    private ChoiceModelApplication[]          shared2ModelArray;
    private ChoiceModelApplication[]          shared3ModelArray;
    private ChoiceModelApplication			  mgraModel;
    private ChoiceModelApplication			  rideHailModel;

    private Tracer                            tracer;
    private boolean                           trace;
    private int[]                             traceOtaz;
    private int[]                             traceDtaz;
    private boolean                           seek;
    private HashMap<String, String>           rbMap;

    private McLogsumsCalculator      logsumHelper;
    private TripModeChoiceDMU 		 mcDmuObject;
    private AutoTazSkimsCalculator   tazDistanceCalculator;

    private boolean							  debugChoiceModel;
    private int								  debugPartyID;
    
    /**
     * Constructor
     * 
     * @param propertyMap
     *            Resource properties file map.
     * @param dmuFactory
     *            Factory object for creation of airport model DMUs
     */
    public AirportModeChoiceModel(HashMap<String, String> rbMap, AirportDmuFactoryIf dmuFactory, String airportCode)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
                CtrampApplication.PROPERTIES_UEC_PATH);
        String airportModeUecFileName = Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.uec.file");
        airportModeUecFileName = uecFileDirectory + airportModeUecFileName;

        int dataPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.data.page"));
        int daPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.da.page"));
        int s2Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.s2.page"));
        int s3Page = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.s3.page"));
        int transitPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.transit.page"));
        int accessPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".mc.accessMode.page"));
        int mgraPage = 0;
        int rideHailPage = 0;
        if (airportCode.equals("SAN"))
        {
        	mgraPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
            		"airport."+airportCode+".mc.mgra.page"));
            rideHailPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
            		"airport."+airportCode+".mc.ridehail.page"));
        }

        logger.info("Creating Airport Model Mode Choice Application UECs");

        // create a DMU
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();

        if (airportCode.equals("SAN"))
        {
        	// get MAX mgra to initialize mgra size array in dmu
            int maxMgra = mgraManager.getMaxMgra();
            dmu.setMaxMgra(maxMgra);
            
            // fake choice model to get airport access MGRA input
            mgraModel = new ChoiceModelApplication(airportModeUecFileName, mgraPage, dataPage,
            		rbMap, (VariableTable) dmu);   
            
            solveModeMgra(dmu);
            
            // create ChoiceModelApplication objects for each airport mgra
            driveAloneModelArray = new ChoiceModelApplication[dmu.mgra_index_map.size()];
            shared2ModelArray = new ChoiceModelApplication[dmu.mgra_index_map.size()];
            shared3ModelArray = new ChoiceModelApplication[dmu.mgra_index_map.size()];
            
            for (int i = 0; i < dmu.mgra_index_map.size(); i++){
            	// create a ChoiceModelApplication object for drive-alone mode choice
            	driveAloneModelArray[i] = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                        rbMap, (VariableTable) dmu);
            	// create a ChoiceModelApplication object for shared 2 mode choice
            	shared2ModelArray[i] = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage,
                        rbMap, (VariableTable) dmu);
            	// create a ChoiceModelApplication object for shared 3+ mode choice
            	shared3ModelArray[i] = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage,
                        rbMap, (VariableTable) dmu);
            }
            
            rideHailModel = new ChoiceModelApplication(airportModeUecFileName, rideHailPage, dataPage,
                    rbMap, (VariableTable) dmu);
        }
        else
        {
        	// create a ChoiceModelApplication object for drive-alone mode choice
            driveAloneModel = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                    rbMap, (VariableTable) dmu);

            // create a ChoiceModelApplication object for shared 2 mode choice
            shared2Model = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage, rbMap,
                    (VariableTable) dmu);

            // create a ChoiceModelApplication object for shared 3+ mode choice
            shared3Model = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage, rbMap,
                    (VariableTable) dmu);
        }
        
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

        if (Util.getStringValueFromPropertyMap(rbMap,"airport.debug") != "")
        {
        	debugChoiceModel = Util.getBooleanValueFromPropertyMap(rbMap, "airport.debug");
            debugPartyID = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                    "airport.debug.party.id"));

        }
        else
        {
        	debugChoiceModel = false;
        	debugPartyID = -99;
        }
    }

    /**
     * get access mode MGRA from UEC user input
     */
    public void solveModeMgra(AirportModelDMU dmu){ 
    	mgraModel.computeUtilities(dmu, dmu.getDmuIndex());

    	int modeCount = mgraModel.getNumberOfAlternatives();
    	double[] mgraValues = mgraModel.getUtilities();
    	
    	HashMap<Integer, Integer> modeMgraMap = new HashMap<Integer, Integer>();
    	
    	for (int m = 0; m < modeCount; m++){
    		int mgraValue = (int)Math.round(mgraValues[m]);
    		modeMgraMap.put(m+1, mgraValue);
    	}
    	
    	dmu.setModeMgraMap(modeMgraMap);
    	dmu.setMgraIndexMap();
    	dmu.setTravelTimeArraySize();
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
    public void chooseMode(AirportParty party, AirportModelDMU dmu, String airportCode)
    {

        int origMgra = party.getOriginMGRA();
        int destMgra = party.getDestinationMGRA();
        
        //if (airportCode == "SAN")
        //{
        	int direction = party.getDirection();
            int airportMgra = 0;
            int airportMgra_index = 0;
            int nonAirportMgra = 0;
            int accessOrigMgra = 0;
            int accessDestMgra = 0;
            int accessOrigTaz = 0;
            int accessDestTaz = 0;
            if (direction == 0){ //departure
            	nonAirportMgra = origMgra;
            } else { //arrival
            	nonAirportMgra = destMgra;
            }
            dmu.setNonAirportMgra(nonAirportMgra);
            dmu.setDirection(direction);
        //}
        
        int origTaz = mgraManager.getTaz(origMgra);
        int destTaz = mgraManager.getTaz(destMgra);
        int period = party.getDepartTime();

        boolean inbound = false;
        if (party.getDirection() == AirportModelStructure.ARRIVAL) inbound = true;

        dmu.setAirportParty(party);
        dmu.setDmuIndexValues(party.getID(), origTaz, destTaz);
        
        if (party.getPurpose() == AirportModelStructure.EMPLOYEE)
        {
        	logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, origMgra, destMgra, period, party.getDebugChoiceModels());
        	
        	double[][] walkTransitTapPairs_AP2Term = logsumHelper.getBestWtwTripTaps();
            
            //pick transit path from N-paths
            float rn = new Double(party.getRandom()).floatValue();
        	int pathIndex = logsumHelper.chooseTripPath(rn, walkTransitTapPairs_AP2Term, party.getDebugChoiceModels(), logger);
        	if (pathIndex < 0) 
        	{
        		int boardTap = (int) 0;
            	int alightTap = (int) 0;
            	int set = (int) -1;
            	party.setAP2TerminalBoardTap(boardTap);
            	party.setAP2TerminalAlightTap(alightTap);
            	party.setAP2TerminalSet(set);
        	}
        	else
        	{
        		int boardTap = (int) walkTransitTapPairs_AP2Term[pathIndex][0];
            	int alightTap = (int) walkTransitTapPairs_AP2Term[pathIndex][1];
            	int set = (int) walkTransitTapPairs_AP2Term[pathIndex][2];
            	party.setAP2TerminalBoardTap(boardTap);
            	party.setAP2TerminalAlightTap(alightTap);
            	party.setAP2TerminalSet(set);
        	}

        	return;
        }
       
        // set trip mc dmu values for transit logsum (gets replaced below by uec values)
        double c_ivt = -0.03;
        double c_cost = - 0.0003; 

        // Solve trip mode level utilities
        mcDmuObject.setIvtCoeff(c_ivt);
        mcDmuObject.setCostCoeff(c_cost);
        double walkTransitLogsum = -999.0;
        double driveTransitLogsum = -999.0;
        
        
        if (airportCode.equals("SAN"))
        {
        	for (int mode = 1; mode <= AirportModelStructure.ACCESS_MODES_SAN; mode++)
            {
            	airportMgra = dmu.mode_mgra_map.get(mode);
            	
            	airportMgra_index = dmu.mgra_index_map.get(airportMgra);
            	
            	if (airportMgra == -999)
            	{
            		continue;
            	}
            	
            	if (direction == 0){ //departure
                	accessOrigMgra = nonAirportMgra;
                	accessDestMgra = airportMgra;
                } else { //arrival
                	accessOrigMgra = airportMgra;
                	accessDestMgra = nonAirportMgra;
                }
            	
            	accessOrigTaz = mgraManager.getTaz(accessOrigMgra);
            	accessDestTaz = mgraManager.getTaz(accessDestMgra);
            	
            	dmu.setDmuIndexValues(party.getID(), accessOrigTaz, accessDestTaz);  // should this be access point Taz?
            	
            	for (int los = 0; los < AirportModelStructure.LOS_TYPE; los++)
            	{
            		double travelTime = dmu.getModeTravelTime(nonAirportMgra, airportMgra_index, direction, los);
            		if (travelTime == 0)
            		{
            			if (los == AirportModelStructure.DA){
            				driveAloneModelArray[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
            				double driveAloneLogsum = driveAloneModelArray[airportMgra_index].getLogsum();
            				dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, los, driveAloneLogsum);
            			}
            			else if (los == AirportModelStructure.SR2){
            				shared2ModelArray[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
            				double shared2Logsum = shared2ModelArray[airportMgra_index].getLogsum();
            	            dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, los, shared2Logsum);
            			}
            			else if (los == AirportModelStructure.SR3){
            				shared3ModelArray[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
            	            double shared3Logsum = shared3ModelArray[airportMgra_index].getLogsum();
            	            dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, los, shared3Logsum);
            			}
            		}
            	}
            	
            	if (mode == AirportModelStructure.RIDEHAILING_LOC1)
            	{
            		rideHailModel.computeUtilities(dmu, dmu.getDmuIndex());
            		dmu.setRidehailTravelDistanceLocation1(rideHailModel.getUtilities()[1]);
            		dmu.setRidehailTravelTimeLocation1(rideHailModel.getUtilities()[0]);
            	}
            	
            	if (mode == AirportModelStructure.RIDEHAILING_LOC2)
            	{
            		rideHailModel.computeUtilities(dmu, dmu.getDmuIndex());
            		dmu.setRidehailTravelDistanceLocation2(rideHailModel.getUtilities()[1]);
            		dmu.setRidehailTravelTimeLocation2(rideHailModel.getUtilities()[0]);
            	}
            
            }
        	
        	dmu.setDmuIndexValues(party.getID(), origTaz, destTaz);
        }
        else
        {
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
        
        if (airportCode.equals("SAN"))
        {
        	dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, AirportModelStructure.Transit, transitLogsum);
        }

        // calculate access mode utility and choose access mode
        accessModel.computeUtilities(dmu, dmu.getDmuIndex());
        int accessMode = accessModel.getChoiceResult(party.getRandom());
        party.setArrivalMode((byte) accessMode);

        if (airportCode.equals("SAN"))
        {
        	int airportAccessMGRA = dmu.mode_mgra_map.get(accessMode);
            
            if (accessMode == AirportModelStructure.SHUTTLE_VAN_SAN | accessMode == AirportModelStructure.HOTEL_COURTESY)
            {
            	double terminal_logsum = 0;
            	double cmh_logsum = 0;
            	int size = party.getSize();
            	if (size == 1)
            	{
            		terminal_logsum = dmu.getShared2LogsumHotelOrShuttleTerminal();
            		cmh_logsum = dmu.getShared2LogsumHotelOrShuttleCentralMobilityHub();
            	}
            	else
            	{
            		terminal_logsum = dmu.getShared3LogsumHotelOrShuttleTerminal();
            		cmh_logsum = dmu.getShared3LogsumHotelOrShuttleCentralMobilityHub();
            	}
            	
            	if (terminal_logsum >= cmh_logsum)
            	{
            		airportAccessMGRA = dmu.mode_mgra_map.get(AirportModelStructure.MGRAAlt_TERM);
            	}
            	else
            	{
            		airportAccessMGRA = dmu.mode_mgra_map.get(AirportModelStructure.MGRAAlt_CMH);
            	}
            }
            
            party.setAirportAccessMGRA(airportAccessMGRA);
        }
        
        // add debug
        if (debugChoiceModel & party.getID() == debugPartyID)
        {
        	String choiceModelDescription = "";
            String decisionMakerLabel = "";
            String loggingHeader = "";
            String separator = "";
             
        	choiceModelDescription = String.format(
                    "Airport Mode Choice Model for: Purpose=%s, OrigMGRA=%d, DestMGRA=%d",
                    party.getPurpose(), party.getOriginMGRA(), party.getDestinationMGRA());
            decisionMakerLabel = String.format("partyID=%d, partySize=%d, purpose=%s, direction=%d",
                    party.getID(), party.getSize(),
                    party.getPurpose(), party.getDirection());
            loggingHeader = String.format("%s    %s", choiceModelDescription,
                    decisionMakerLabel);
            
            logger.info(loggingHeader);
            accessModel.logUECResults(logger);
            transitModel.logUECResults(logger);
        }
        
        // choose trip mode
        int tripMode = 0;
        int occupancy;
        if (airportCode.equals("SAN"))
        {
        	occupancy = AirportModelStructure.getOccupancy_san(accessMode, party.getSize());
        }
        else
        {
        	occupancy = AirportModelStructure.getOccupancy_cbx(accessMode, party.getSize());
        }
        
        double randomNumber = party.getRandom();
        
        float valueOfTime = 0;

        if (((airportCode.equals("CBX")) && (accessMode != AirportModelStructure.TRANSIT_CBX) && (! AirportModelStructure.taxiTncMode_cbx(accessMode))) ||
        		((airportCode.equals("SAN")) && (accessMode != AirportModelStructure.TRANSIT_SAN) && (! AirportModelStructure.taxiTncMode_san(accessMode))))
        {
        	int chosenAirportMgra;
        	int chosenAirportMgra_index = 0;
        	if (airportCode.equals("SAN"))
        	{
        		chosenAirportMgra = dmu.mode_mgra_map.get(accessMode);
            	chosenAirportMgra_index = dmu.mgra_index_map.get(chosenAirportMgra);
            	
            	if (chosenAirportMgra != dmu.getTerminalMgra())
                {
                	if (direction == 0)
                	{
                		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, chosenAirportMgra, dmu.getTerminalMgra(), period, party.getDebugChoiceModels());
                		
                		double[][] walkTransitTapPairs_AP2Term = logsumHelper.getBestWtwTripTaps();
                		
                		//pick transit path from N-paths
                        float rn = new Double(party.getRandom()).floatValue();
                    	int pathIndex = logsumHelper.chooseTripPath(rn, walkTransitTapPairs_AP2Term, party.getDebugChoiceModels(), logger);
                    	if (pathIndex < 0) 
                    	{
                    		int boardTap = (int) 0;
                        	int alightTap = (int) 0;
                        	int set = (int) -1;
                        	party.setAP2TerminalBoardTap(boardTap);
                        	party.setAP2TerminalAlightTap(alightTap);
                        	party.setAP2TerminalSet(set);
                    	}
                    	else
                    	{
                    		int boardTap = (int) walkTransitTapPairs_AP2Term[pathIndex][0];
                        	int alightTap = (int) walkTransitTapPairs_AP2Term[pathIndex][1];
                        	int set = (int) walkTransitTapPairs_AP2Term[pathIndex][2];
                        	party.setAP2TerminalBoardTap(boardTap);
                        	party.setAP2TerminalAlightTap(alightTap);
                        	party.setAP2TerminalSet(set);
                    	}
                    	
                        //party.setAPtoTermBestWtwTapPairs(walkTransitTapPairs_AP2Term);
                	}
                	else
                	{
                		logsumHelper.setWtwTripMcDmuAttributes( mcDmuObject, dmu.getTerminalMgra(), chosenAirportMgra, period, party.getDebugChoiceModels());
               
                		double[][] walkTransitTapPairs_AP2Term = logsumHelper.getBestWtwTripTaps();
                		
                		//pick transit path from N-paths
                        float rn = new Double(party.getRandom()).floatValue();
                    	int pathIndex = logsumHelper.chooseTripPath(rn, walkTransitTapPairs_AP2Term, party.getDebugChoiceModels(), logger);
                    	
                    	if (pathIndex < 0) 
                    	{
                    		int boardTap = (int) 0;
                        	int alightTap = (int) 0;
                        	int set = (int) -1;
                        	party.setAP2TerminalBoardTap(boardTap);
                        	party.setAP2TerminalAlightTap(alightTap);
                        	party.setAP2TerminalSet(set);
                    	}
                    	else
                    	{
                    		int boardTap = (int) walkTransitTapPairs_AP2Term[pathIndex][0];
                        	int alightTap = (int) walkTransitTapPairs_AP2Term[pathIndex][1];
                        	int set = (int) walkTransitTapPairs_AP2Term[pathIndex][2];
                        	party.setAP2TerminalBoardTap(boardTap);
                        	party.setAP2TerminalAlightTap(alightTap);
                        	party.setAP2TerminalSet(set);
                    	}
                    	
                        //party.setAPtoTermBestWtwTapPairs(walkTransitTapPairs_AP2Term);
                	}
                }
                
        	}
            if (occupancy == 1)
            {
                tripMode = occupancy;
               
                UtilityExpressionCalculator uec;
                //following gets vot from UEC
                if (airportCode.equals("SAN"))
                {
                	uec = driveAloneModelArray[chosenAirportMgra_index].getUEC();
                }
                else
                {
                	uec = driveAloneModel.getUEC();
                }
                
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            } else if (occupancy == 2)
            {
                tripMode = occupancy;

                UtilityExpressionCalculator uec;
                //following gets vot from UEC
                if (airportCode.equals("SAN"))
                {
                	uec = shared2ModelArray[chosenAirportMgra_index].getUEC();
                }
                else
                {
                	uec = shared2Model.getUEC();
                }
              
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            } else if (occupancy > 2)
            {
                tripMode = 3;
                
                UtilityExpressionCalculator uec;
                //following gets vot from UEC
                if (airportCode.equals("SAN"))
                {
                	uec = shared3ModelArray[chosenAirportMgra_index].getUEC();
                }
                else
                {
                	uec = shared3Model.getUEC();
                }
              
                int votIndex = uec.lookupVariableIndex("vot");
                valueOfTime = (float) uec.getValueForIndex(votIndex);

            }
        } else if (((airportCode.equals("CBX")) && (accessMode == AirportModelStructure.TRANSIT_CBX)) || 
        		((airportCode.equals("SAN")) && (accessMode == AirportModelStructure.TRANSIT_SAN)))
        {
            int choice = transitModel.getChoiceResult(randomNumber);
            double[][] bestTapPairs;
            if (choice == 1){
            	tripMode = AirportModelStructure.REALLOCATE_WLKTRN; //walk-transit
            	bestTapPairs = logsumHelper.getBestWtwTripTaps();
            }
            else if (choice == 2){
            	tripMode = AirportModelStructure.REALLOCATE_KNRPERTRN; //knr-personal tNCVehicle
                if (party.getDirection() == AirportModelStructure.DEPARTURE)
                	bestTapPairs = logsumHelper.getBestDtwTripTaps();
                else
                	bestTapPairs = logsumHelper.getBestWtdTripTaps();
            }
            else {
               	tripMode = AirportModelStructure.REALLOCATE_KNRTNCTRN; //knr-TNC
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

        }else if((airportCode.equals("CBX")) && accessMode == AirportModelStructure.TAXI){
        	
        	tripMode=AirportModelStructure.REALLOCATE_TAXI;
        }
    	else if((airportCode.equals("CBX")) && accessMode == AirportModelStructure.TNC_SINGLE){
    	
           	tripMode=AirportModelStructure.REALLOCATE_TNCSINGLE;
  	
    	}
    	else if((airportCode.equals("CBX")) && accessMode == AirportModelStructure.TNC_SHARED){
        	
           	tripMode=AirportModelStructure.REALLOCATE_TNCSHARED;

    	}
    	else if((airportCode.equals("SAN")) && accessMode == AirportModelStructure.RIDEHAILING_LOC1){
        	
           	tripMode=AirportModelStructure.REALLOCATE_TNCSINGLE;

    	}
    	else if((airportCode.equals("SAN")) && accessMode == AirportModelStructure.RIDEHAILING_LOC2){
        	
           	tripMode=AirportModelStructure.REALLOCATE_TNCSINGLE;

    	}
        
        //set the VOT
        if(((airportCode.equals("CBX")) && AirportModelStructure.taxiTncMode_cbx(accessMode)) || ((airportCode.equals("SAN")) && AirportModelStructure.taxiTncMode_san(accessMode)) ) {
        	UtilityExpressionCalculator uec = null;
        	
        	int chosenAirportMgra;
        	int chosenAirportMgra_index = 0;
        	if (airportCode.equals("SAN"))
        	{
        		chosenAirportMgra = dmu.mode_mgra_map.get(accessMode);
            	chosenAirportMgra_index = dmu.mgra_index_map.get(chosenAirportMgra);
                
        	}
        	
        	//following gets vot from UEC
            if(occupancy==1)
            	if (airportCode.equals("SAN"))
            	{
            		uec = driveAloneModelArray[chosenAirportMgra_index].getUEC();
            	}
            	else
            	{
            		uec = driveAloneModel.getUEC();
            	}
            	
            else if (occupancy==2)
            	if (airportCode.equals("SAN"))
            	{
            		uec = shared2ModelArray[chosenAirportMgra_index].getUEC();
            	}
            	else
            	{
            		uec = shared2Model.getUEC();
            	}
            else 
            	if (airportCode.equals("SAN"))
            	{
            		uec = shared3ModelArray[chosenAirportMgra_index].getUEC();
            	}
            	else
            	{
            		uec = shared3Model.getUEC();
            	}
           	
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
    public void chooseModes(HashMap<String, String> rbMap, AirportParty[] airportParties, AirportDmuFactoryIf dmuFactory, String airportCode)
    {
    	this.rbMap = rbMap;
    	
    	tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
    	
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        
        int maxMgra = mgraManager.getMaxMgra();
        dmu.setMaxMgra(maxMgra);
        
        if (airportCode.equals("SAN"))
        {
        	solveModeMgra(dmu);
        	
        	int terminalMgra = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
            		"airport.SAN.airportMgra"));
            
            // set terminal Mgra
            dmu.setTerminalMgra(terminalMgra);
        }
        
        // iterate through the array, choosing mgras and setting them
        for (AirportParty party : airportParties)
        {

            int ID = party.getID();

            if ((ID <= 5) || (ID % 100) == 0)
                logger.info("Choosing mode for party " + party.getID());

            chooseMode(party, dmu, airportCode);
            
            //if (party.getPurpose() == AirportModelStructure.EMPLOYEE) continue;
            //else
            //{
            //	chooseMode(party, dmu, airportCode);
            //}
        }
    }
  
}
