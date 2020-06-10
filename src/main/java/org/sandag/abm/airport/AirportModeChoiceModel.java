package org.sandag.abm.airport;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.util.Tracer;

public class AirportModeChoiceModel
{
    private transient Logger                  logger = Logger.getLogger("airportModel");

    private TazDataManager                    tazManager;
    private MgraDataManager                   mgraManager;

    private ChoiceModelApplication[]          driveAloneModel;
    private ChoiceModelApplication[]          shared2Model;
    private ChoiceModelApplication[]          shared3Model;
    private ChoiceModelApplication            transitModel;
    private ChoiceModelApplication            accessModel;
    private ChoiceModelApplication			  mgraModel;
    private ChoiceModelApplication			  rideHailModel;

    private Tracer                            tracer;
    private boolean                           trace;
    private int[]                             traceOtaz;
    private int[]                             traceDtaz;
    private boolean                           seek;
    private HashMap<String, String>           rbMap;

    private BestTransitPathCalculator         bestPathUEC;
    protected WalkTransitWalkSkimsCalculator  wtw;
    protected WalkTransitDriveSkimsCalculator wtd;
    protected DriveTransitWalkSkimsCalculator dtw;
    
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
        int mgraPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
        		"airport.mc.mgra.page"));
        int rideHailPage = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap, 
        		"airport.mc.ridehail.page"));

        logger.info("Creating Airport Model Mode Choice Application UECs");

        // create a DMU
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        
        // get MAX mgra to initialize mgra size array in dmu
        int maxMgra = mgraManager.getMaxMgra();
        dmu.setMaxMgra(maxMgra);
        
        // fake choice model to get airport access MGRA input
        mgraModel = new ChoiceModelApplication(airportModeUecFileName, mgraPage, dataPage,
        		rbMap, (VariableTable) dmu);   
        
        solveModeMgra(dmu);
        
        // create ChoiceModelApplication objects for each airport mgra
        driveAloneModel = new ChoiceModelApplication[dmu.mgra_index_map.size()];
        shared2Model = new ChoiceModelApplication[dmu.mgra_index_map.size()];
        shared3Model = new ChoiceModelApplication[dmu.mgra_index_map.size()];
        
        for (int i = 0; i < dmu.mgra_index_map.size(); i++){
        	// create a ChoiceModelApplication object for drive-alone mode choice
        	driveAloneModel[i] = new ChoiceModelApplication(airportModeUecFileName, daPage, dataPage,
                    rbMap, (VariableTable) dmu);
        	// create a ChoiceModelApplication object for shared 2 mode choice
        	shared2Model[i] = new ChoiceModelApplication(airportModeUecFileName, s2Page, dataPage,
                    rbMap, (VariableTable) dmu);
        	// create a ChoiceModelApplication object for shared 3+ mode choice
        	shared3Model[i] = new ChoiceModelApplication(airportModeUecFileName, s3Page, dataPage,
                    rbMap, (VariableTable) dmu);
        }
        
        rideHailModel = new ChoiceModelApplication(airportModeUecFileName, rideHailPage, dataPage,
                rbMap, (VariableTable) dmu);
        
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
        
        debugChoiceModel = Util.getBooleanValueFromPropertyMap(rbMap, "airport.debug");
        debugPartyID = Integer.parseInt(Util.getStringValueFromPropertyMap(rbMap,
                "airport.debug.party.id"));

    }

    /**
     * Create new transit skim calculators.
     */
    public void initializeBestPathCalculators()
    {

        logger.info("Initializing Airport Model Best Path Calculators");

        bestPathUEC = new BestTransitPathCalculator(rbMap);

        wtw = new WalkTransitWalkSkimsCalculator();
        wtw.setup(rbMap, logger, bestPathUEC);
        wtd = new WalkTransitDriveSkimsCalculator();
        wtd.setup(rbMap, logger, bestPathUEC);
        dtw = new DriveTransitWalkSkimsCalculator();
        dtw.setup(rbMap, logger, bestPathUEC);

        logger.info("Finished Initializing Airport Model Best Path Calculators");

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
    public void chooseMode(AirportParty party, AirportModelDMU dmu)
    {

        int origMgra = party.getOriginMGRA();
        int destMgra = party.getDestinationMGRA();
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
        int origTaz = mgraManager.getTaz(origMgra);
        int destTaz = mgraManager.getTaz(destMgra);   
        int period = party.getDepartTime();
        int skimPeriod = AirportModelStructure.getSkimPeriodIndex(period) + 1; // The
                                                                               // skims
                                                                               // are
                                                                               // stored
                                                                               // 1-based...don't
                                                                               // ask...
        boolean debug = party.getDebugChoiceModels();

        // calculate best tap pairs for this airport party
        int[][] walkTransitTapPairs = wtw.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                logger);
        party.setBestWtwTapPairs(walkTransitTapPairs);

        // drive transit tap pairs depend on direction; departing parties use
        // drive-transit-walk, else walk-transit-drive is used.
        int[][] driveTransitTapPairs;
        if (party.getDirection() == AirportModelStructure.DEPARTURE)
        {
            driveTransitTapPairs = dtw.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                    logger);
            party.setBestDtwTapPairs(driveTransitTapPairs);
        } else
        {
            driveTransitTapPairs = wtd.getBestTapPairs(origMgra, destMgra, skimPeriod, debug,
                    logger);
            party.setBestWtdTapPairs(driveTransitTapPairs);
        }

        // set transit skim values in DMU object
        dmu.setDmuSkimCalculators(wtw, wtd, dtw);
        boolean inbound = false;
        if (party.getDirection() == AirportModelStructure.ARRIVAL) inbound = true;

        dmu.setAirportParty(party);

        dmu.setDmuSkimAttributes(origMgra, destMgra, period, inbound, debug);

        // Solve trip mode level utilities
        
        for (int mode = 1; mode <= AirportModelStructure.ACCESS_MODES; mode++)
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
        				driveAloneModel[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        				double driveAloneLogsum = driveAloneModel[airportMgra_index].getLogsum();
        				dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, los, driveAloneLogsum);
        			}
        			else if (los == AirportModelStructure.SR2){
        				shared2Model[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        				double shared2Logsum = shared2Model[airportMgra_index].getLogsum();
        	            dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, los, shared2Logsum);
        			}
        			else if (los == AirportModelStructure.SR3){
        				shared3Model[airportMgra_index].computeUtilities(dmu, dmu.getDmuIndex());
        	            double shared3Logsum = shared3Model[airportMgra_index].getLogsum();
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
		transitModel.computeUtilities(dmu, dmu.getDmuIndex());
        double transitLogsum = transitModel.getLogsum();
        dmu.setModeTravelTime(nonAirportMgra, airportMgra_index, direction, AirportModelStructure.Transit, transitLogsum);
        
        // calculate access mode utility and choose access mode
        accessModel.computeUtilities(dmu, dmu.getDmuIndex());
        int accessMode = accessModel.getChoiceResult(party.getRandom());
        party.setArrivalMode((byte) accessMode);
        
        int airportAccessMGRA = dmu.mode_mgra_map.get(accessMode);
        party.setAirportAccessMGRA(airportAccessMGRA);

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
            driveAloneModel[2].logUECResults(logger);
            accessModel.logUECResults(logger);
            transitModel.logUECResults(logger);
        }
 
        // choose trip mode
        int tripMode = 0;
        int occupancy = AirportModelStructure.getOccupancy(accessMode, party.getSize());
        double randomNumber = party.getRandom();

        if (accessMode != AirportModelStructure.TRANSIT)
        {
        	int chosenAirportMgra = dmu.mode_mgra_map.get(accessMode);
        	int chosenAirportMgra_index = dmu.mgra_index_map.get(chosenAirportMgra);
            if (occupancy == 1)
            {
                int choice = driveAloneModel[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice;
            } else if (occupancy == 2)
            {
                int choice = shared2Model[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice + 2;
            } else if (occupancy > 2)
            {
                int choice = shared3Model[chosenAirportMgra_index].getChoiceResult(randomNumber);
                tripMode = choice + 3 + 2;
            }
        } else
        {
            int choice = transitModel.getChoiceResult(randomNumber);
            if (choice <= 5) tripMode = choice + 10;
            else tripMode = choice + 15;
        }
        party.setMode((byte) tripMode);
    }

    /**
     * Choose modes for internal trips.
     * 
     * @param airportParties
     *            An array of travel parties, with destinations already chosen.
     * @param dmuFactory
     *            A DMU Factory.
     */
    public void chooseModes(HashMap<String, String> rbMap, AirportParty[] airportParties, AirportDmuFactoryIf dmuFactory)
    {
    	this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);
        
        AirportModelDMU dmu = dmuFactory.getAirportModelDMU();
        
        int maxMgra = mgraManager.getMaxMgra();
        dmu.setMaxMgra(maxMgra);
        
        solveModeMgra(dmu);
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

    /**
     * @param wtw
     *            the wtw to set
     */
    public void setWtw(WalkTransitWalkSkimsCalculator wtw)
    {
        this.wtw = wtw;
    }

    /**
     * @param wtd
     *            the wtd to set
     */
    public void setWtd(WalkTransitDriveSkimsCalculator wtd)
    {
        this.wtd = wtd;
    }

    /**
     * @param dtw
     *            the dtw to set
     */
    public void setDtw(DriveTransitWalkSkimsCalculator dtw)
    {
        this.dtw = dtw;
    }

}
